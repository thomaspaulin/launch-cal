package nz.paulin.spaceflight;

import com.sun.mail.smtp.SMTPMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.event.TransportAdapter;
import javax.mail.event.TransportEvent;
import javax.mail.event.TransportListener;
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
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final int SMTP_PORT = 587;

    static void sendEmail(String from, String to, String subject, Exception e, List<File> attachments) {
        try {
            final String SMTP_USER = from;
            final String SMTP_PASSWORD = System.getenv("NOTIFIER_SENDER_PASSWORD");

            Properties props = new Properties();
            props.put("mail.smtp.host",                     SMTP_HOST);
            props.put("mail.smtp.port",                     "" + SMTP_PORT);
            props.put("mail.smtp.auth",                     "true");
            props.put("mail.smtp.starttls.enable",          "true");
            props.put("mail.smtp.socketFactory.port",       SMTP_PORT);
            props.put("mail.smtp.socketFactory.class",      "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.debug",                    "true");

            Session session = Session.getDefaultInstance(props, new javax.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(SMTP_USER, SMTP_PASSWORD);
                }
            });

            SMTPMessage smtpMessage = new SMTPMessage(session);
            // sender
            InternetAddress sender = new InternetAddress(from);
            sender.setPersonal("Inspector Gadget");
            // recipients
            Address[] recipients = new Address[]{new InternetAddress(to)};

            // set up sender and recipients
            smtpMessage.addFrom(new InternetAddress[]{sender});
            smtpMessage.setRecipients(Message.RecipientType.TO, recipients);

            // set subject
            smtpMessage.setSubject(subject);

            // set text and attachments
            Multipart multipart = new MimeMultipart();

            String message = "Hi there!\n\nSorry to bother you, but I encountered an error when trying to parse some " +
                    "launches. Here is a description of the exception:\n\n" + getExceptionDescription(e) + "\n\nThank you!";
            BodyPart body = new MimeBodyPart();
            body.setText(message);
            multipart.addBodyPart(body);
//            addAttachments(multipart, attachments);
            smtpMessage.setContent(multipart);
            smtpMessage.saveChanges();

            // transport
            Transport transport = session.getTransport("smtp");
            transport.connect(SMTP_HOST, SMTP_PORT, SMTP_USER, SMTP_PASSWORD);
            transport.sendMessage(smtpMessage, recipients);
            transport.close();
        } catch (IOException | MessagingException e1) {
            e1.printStackTrace();
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
