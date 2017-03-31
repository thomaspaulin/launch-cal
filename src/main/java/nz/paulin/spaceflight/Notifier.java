package nz.paulin.spaceflight;

import com.sun.mail.smtp.SMTPMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Properties;


@SuppressWarnings("SameParameterValue")
class Notifier {
    private static final Logger logger = LogManager.getLogger(Notifier.class);
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final int SMTP_PORT = 587;

    static void sendEmail(String from, String to, String subject, Exception e, List<File> attachments) {
        try {
            if(System.getProperty("notifier.sender") == null) {
                LaunchCal.setPropertiesFromFile("properties.txt");
            }
            if(System.getProperty("notifier.sender") == null) {
                throw new RuntimeException("I couldn't set the properties so I don't know how to email this exception :(");
            }
            final String SMTP_USER = from;
            final String SMTP_PASSWORD = System.getProperty("notifier.sender.password");

            Address sender = new InternetAddress(from);

            Address[] recipients = new Address[]{new InternetAddress(from)};

            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", SMTP_HOST);
            props.put("mail.smtp.port", "" + SMTP_PORT);
            props.put("mail.smtp.debug", "true");

            Session session = Session.getDefaultInstance(props, new javax.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    logger.debug("Trying to authenticate as " + SMTP_USER);
                    return new PasswordAuthentication(SMTP_USER, SMTP_PASSWORD);
                }
            });
            Transport transport = session.getTransport("smtp");

            SMTPMessage smtpMessage = new SMTPMessage(session);
            smtpMessage.addFrom(new Address[]{sender});
            smtpMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(to));

            smtpMessage.setSubject(subject);

            Multipart multipleContent = new MimeMultipart();
            String message = "Hi there!\n\nSorry to bother you, but I encountered an error when trying to parse some launches. Here is a description of the exception:\n\n" + getExceptionDescription(e) + "\nI've attached some log files too.\n\nThank you!";
            BodyPart body = new MimeBodyPart();
            body.setText(message);
            multipleContent.addBodyPart(body);

            addAttachments(multipleContent, attachments);

            smtpMessage.setContent(multipleContent);

            smtpMessage.saveChanges();

            transport.connect(SMTP_HOST, SMTP_PORT, SMTP_USER, SMTP_PASSWORD);
            transport.sendMessage(smtpMessage, recipients);
            transport.close();
        } catch (IOException | URISyntaxException | MessagingException e1) {
            logger.fatal("I really hope someone is watching because this happened: ");
            logger.fatal(e1);
        }
    }

    private static void addAttachments(Multipart contents, List<File> files) throws MessagingException {
        for(File file : files) {
            if(file.exists()) {
                addAttachment(contents, file);
            } else {
                throw new IllegalArgumentException("Attachment file at " + file.getAbsolutePath() + " does not exist");
            }
        }
    }

    private static void addAttachment(Multipart contents, File file) throws MessagingException {
        MimeBodyPart filePart = new MimeBodyPart();

        FileDataSource fileDataSource = new FileDataSource(file);
        filePart.setDataHandler(new DataHandler(fileDataSource));
        filePart.setFileName(file.getName());

        contents.addBodyPart(filePart);
    }

    static String getExceptionDescription(Throwable exception) {
        StringWriter writer = new StringWriter();
        PrintWriter pwriter = new PrintWriter(writer);
        exception.printStackTrace(pwriter);
        pwriter.flush();
        return writer.toString();
    }
}
