package com.tumri.af.email;

import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.log4j.Logger;

public class MailSender {

	private final static Logger log = Logger.getLogger(MailSender.class);
	
    private static final String SMTP = "smtp";
    private static final String MAIL_SMTP_PORT = "mail." + SMTP + ".port";
    private static final String SMTP_HOST = SMTP + ".smtphost";
    private static final String SMTP_PORT = SMTP + ".smtpport";
    private static final String SMTP_FROM = SMTP + ".from";
    private static final String TEXT_PLAIN = "text/plain";
    private static final String TEXT_HTML = "text/html";
	
    /** Sends an email from the default transport with full typed arguments.
     * Note that the HTML text is sent added to the set of attachments if it is not null.
     * @param host The mail sender host name.
     * @param port The mail sender port number.
     * @param userName If non-null, the user name to use for authenticated SMTP.
     * @param password If non-null, the password to use for authenticated SMTP.
     * @param from The email address of the sender.
     * @param toList A list of email addresses of the recipients.
     * @param subject The subject of the mail.
     * @param text The plain text email message.
     * @param htmlText The email message in HTML (optional).
     * @param attachments Any attachments or null if none.
     * @throws MessagingException If error sending mail.
     */
    public static void sendMail(String host, int port, String userName, String password, InternetAddress from, List<InternetAddress> toList,
    		                    String subject, String text, String htmlText, List<DataSource> attachments) 
    						throws MessagingException {
		Transport tr;
        Properties props = new Properties(System.getProperties());

		props.put(MAIL_SMTP_PORT, port);
        props.put(SMTP_HOST, host);
        props.put(SMTP_PORT, port);
        props.put(SMTP_FROM, from);

		// Get a Session object
        // See here for an example of authentication:
        // http://www.mkyong.com/java/javamail-api-sending-email-via-gmail-smtp-example/
        Session mailSession;
        if(userName != null)
            mailSession = Session.getInstance
                    (props, new PasswordAuthenticator(userName, password));
        else mailSession = Session.getDefaultInstance(props, null);
        // construct the message
        Message msg = new MimeMessage(mailSession);
        msg.setFrom(from);
		for(InternetAddress to:toList){
			msg.addRecipient(Message.RecipientType.TO, to);
		}
		msg.setSubject(subject);

		boolean hasHTMLText = (htmlText != null) && (htmlText.trim().length() > 0);
		boolean hasAttachments = (attachments != null);
		
		if(hasHTMLText || hasAttachments) {
			
			Multipart multipart =
                    (hasAttachments
                            ? new MimeMultipart()
                            : new MimeMultipart("alternative"));
			MimeBodyPart messageBodyPart = new MimeBodyPart();		// Set the plain text
			messageBodyPart.setText(text);
			multipart.addBodyPart(messageBodyPart);
			
			if(hasHTMLText) {
				MimeBodyPart part = new MimeBodyPart();
				part.setContent(htmlText, TEXT_HTML);
				multipart.addBodyPart(part);
			}
			
			if(attachments != null) {
				for(DataSource ds : attachments) {
					MimeBodyPart part = new MimeBodyPart();
					part.setDataHandler(new DataHandler(ds));
					part.setFileName(ds.getName());
					multipart.addBodyPart(part);
				}
			}
			msg.setContent(multipart);
		} else {  // No HTML or attachments
			msg.setContent(text, TEXT_PLAIN);
		}
		msg.setSentDate(new Date());
		msg.saveChanges();
		tr = mailSession.getTransport(SMTP);
		tr.connect(host, from.getAddress(), null);
		tr.sendMessage(msg, msg.getAllRecipients());
		log.info("Mail sent to " + toList);
    }
}

class PasswordAuthenticator extends Authenticator
{
    String userName;
    String password;

    public PasswordAuthenticator(String userName, String password)
    {
        this.userName = userName;
        this.password = password;
    }

    protected PasswordAuthentication getPasswordAuthentication()
    {
        return new PasswordAuthentication(userName, password);
    }
}