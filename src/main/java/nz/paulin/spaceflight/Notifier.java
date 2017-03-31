package nz.paulin.spaceflight;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Properties;

/**
 * Created by thomas on 31/03/17.
 */
public class Notifier {
    private static final java.lang.String SMTP_USER = "system@biomatters.com";  //todo change to inspectgadget's
    private static final String SMTP_PASSWORD = ""; //todo change
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final int SMTP_PORT = 587;

    static void sendEmail(String from, String to, String subject, Exception e, List<File> attachments) {
        e.printStackTrace();
        try {
//            Address sender = new InternetAddress(from);
//
//            Address[] recipients = new Address[]{new InternetAddress(from)};
//
//            Properties props = new Properties();
//            props.put("mail.smtp.auth", "true");
//            props.put("mail.smtp.starttls.enable", "true");
//            props.put("mail.smtp.host", SMTP_HOST);
//            props.put("mail.smtp.port", "" + SMTP_PORT);
//
//            Session session = Session.getDefaultInstance(props, new javax.mail.Authenticator() {
//                protected PasswordAuthentication getPasswordAuthentication() {
//                    return new PasswordAuthentication(SMTP_USER, SMTP_PASSWORD);
//                }
//            });
//            Transport transport = session.getTransport("smtp");
//
//            SMTPMessage smtpMessage = new SMTPMessage(session);
//            smtpMessage.addFrom(new Address[]{sender});
//            smtpMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
//
//            InetAddress lksHost = null;
//            try {
//                lksHost = InetAddress.getLocalHost();
//            } catch (UnknownHostException ignored) {}
//            String host = " on host " + (lksHost != null ? lksHost : "Unknown");
//            smtpMessage.setSubject(subject + host);
//            smtpMessage.setReplyTo(new InternetAddress[] {new InternetAddress(replyTo)});
//
//            Multipart multipleContent = new MimeMultipart();
//            addMessageText(multipleContent, message);
//            addAttachments(multipleContent, attachments);
//            smtpMessage.setContent(multipleContent);
//
//            String body = "Hi there!\n\nSorry to bother you, but I encountered an error when trying to parse some launches I saw. Here is a description of the exception:\n\n" + getExceptionDescription(e) + "\n\nI've attached some log files too.\n\nThank you!";
//            smtpMessage.setContent(body, "text/plain");
//            smtpMessage.saveChanges();
//
//            transport.connect(SMTP_HOST, SMTP_PORT, SMTP_USER, SMTP_PASSWORD);
//            transport.sendMessage(smtpMessage, recipients);
//            transport.close();
//        }
        } finally {
            //so it doesn't fail compilation while code is commented out
        }
    }

    private static String getExceptionDescription(Throwable exception) {
        StringWriter writer = new StringWriter();
        PrintWriter pwriter = new PrintWriter(writer);
        exception.printStackTrace(pwriter);
        pwriter.flush();
        return writer.toString();
    }
}
