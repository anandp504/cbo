package com.tumri.af.test.email;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.internet.InternetAddress;

import com.tumri.af.email.MailSender;
import com.tumri.af.utils.FileUtils;

import junit.framework.TestCase;

public class MailSenderTest extends TestCase {

	private File m_tempAttachmentFile;
	
	private final static String DEFAULT_TEST_NAME = "MailSenderTest";
	private final static String TEMP_FILE_PREFIX = "testAttachment";
	private final static String TEMP_FILE_SUFFIX = ".csv";
	
	private final static String TEST_DATA = "Hour, Imps, Clicks\n12:00,12345,12\n13:00,6789,10\n";
	
	private final static String TEST_MAIL_HOST = "smtp.dc1.tumri.net";
	private final static int TEST_MAIL_PORT = 25;
	private final static String TEST_MAIL_FROM = "cboadmins@collective.com";
	private final static String[] TEST_MAIL_TO = {"jkucera@collective.com"};
	private final static String TEST_MAIL_SUBJECT = "Test of mail sender";
	private final static String TEST_MAIL_TEXT = "This is a test of the mail sender.";
	private final static String TEST_MAIL_HTML = null; //"<html><head></head><body>This is <b>HTML</b> text.<p>Wow!</body></html>";
	
	// --------------------- Constructors -------------------------
	
	public MailSenderTest() {
		super(DEFAULT_TEST_NAME);
	}
	
	// -------------------- Public methods ------------------------
	
	/** The main method.
	 * @param args
	 */
	public static void main(String[] args) {
		MailSenderTest t = new MailSenderTest();
		t.runMain(args);
	}
	
	/** Tests plain text mail. */
	public void testTextMail() throws Exception {
		System.out.println("Testing text mail");
		sendMailMessage(TEST_MAIL_HOST, TEST_MAIL_PORT, TEST_MAIL_FROM, TEST_MAIL_TO, 
						TEST_MAIL_SUBJECT, TEST_MAIL_TEXT, null, null);
	}

	/** Tests HTML + plain text mail. */
	public void testHTMLMail() throws Exception {
		System.out.println("Testing html mail");
		sendMailMessage(TEST_MAIL_HOST, TEST_MAIL_PORT, TEST_MAIL_FROM, TEST_MAIL_TO, 
						TEST_MAIL_SUBJECT, TEST_MAIL_TEXT, TEST_MAIL_HTML, null);
	}
	
	/** Tests HTML + plain text + attachments mail. */
	public void testAttachmentMail() throws Exception {
		System.out.println("Testing attachment mail");
		sendMailMessage(TEST_MAIL_HOST, TEST_MAIL_PORT, TEST_MAIL_FROM, TEST_MAIL_TO, 
						TEST_MAIL_SUBJECT, TEST_MAIL_TEXT, TEST_MAIL_HTML, getTempAttachmentFile());
	}
	
	// ------------------------- Protected methods ---------------
	
	/** Creates a temporary attachment file.
	 */
	protected void setUp() throws Exception {
		if(getTempAttachmentFile() == null) {
			 File f = createTempAttachmentFile();
			 log("setUp() created temp attachment file: " + f);
			 m_tempAttachmentFile = f;
		}
	}
	
	/** Deletes the temporary attachment file.
	 */
	protected void tearDown() throws Exception {
		File f = getTempAttachmentFile();
		if((f != null) && f.exists()) {
			f.delete();
			log("Deleted temp attachment file: " + f);
		}
		m_tempAttachmentFile = null;
	}
	
	// -------------------------- Private methods -------------------
	
	/** The main method calls this.
	 * @param args The arguments.
	 */
	private void runMain(String[] args) {
		
		String host = TEST_MAIL_HOST;
		int port = TEST_MAIL_PORT;
		String from = TEST_MAIL_FROM;
		String[] to = TEST_MAIL_TO;
		String subject = TEST_MAIL_SUBJECT;
		String text = TEST_MAIL_TEXT;
		String html = TEST_MAIL_HTML;
		File attachmentFile = null;
		
		try {
			if(args != null) {
				int count = args.length;
				for(int i = 0; i < count; i++) {
					if(args[i].equals("-host")) {
						host = args[++i];
					} else if(args[i].equals("-port")) {
						port = Integer.parseInt(args[++i]);
					} else if(args[i].equals("-from")) {
						from = args[++i];
					} else if(args[i].equals("-subject")) {
						subject = args[++i];
					} else if (args[i].equals("-text")) {
						text = args[++i];
					} else if (args[i].equals("-html")) {
						html = args[++i];
					} else if(args[i].equals("-to")) {
						to = new String[1];
						to[0] = args[++i];
					} else if (args[i].equals("-attachment")) {
						attachmentFile = new File(args[++i]);
					} 
				}
			}
		} catch(Exception e) {
			System.err.println("Use: java " + getClass().getName() + "[" +
			           "-host <mailHost> -port <port> -from <fromAddress>" +
			           "-subject <subject> -text <text> -html <html>" +
			           "-to <toAddress> -attachment <attachmentFilePath>");
			System.exit(1);
		}
		
		System.out.println("Sending mail:\nhost=" + host + "\nport=" + port + 
				           "\nfrom=" + from + "\nto=" + to[0] + "\nsubject=" + subject +
				           "\ntext=" + text + "\nhtml=" + html + 
				           "\nattachmentFile=" + attachmentFile);
		
		try {
			MailSenderTest test = new MailSenderTest();
			test.sendMailMessage(host, port, from, to, subject, text, html, attachmentFile);
		} catch(Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		System.exit(0);
	}
	
	/** Sends a message.
	 * @param host
	 * @param port
	 * @param from
	 * @param toList
	 * @param subject
	 * @param text
	 * @param html
	 * @param attachment
	 * @throws Exception
	 */
	private void sendMailMessage(String host, int port, String from, String[] toList, 
			                     String subject, String text, String html, File attachment) throws Exception {
		MailSender sender = new MailSender();
		List<InternetAddress> recipients = new ArrayList<InternetAddress>();
		for(String recipient : toList) {
			recipients.add(new InternetAddress(recipient));
		}
		List<DataSource> attachments = new ArrayList<DataSource>();
		if(attachment != null) {
			attachments.add(new FileDataSource(attachment));
		}
		sender.sendMail(host, port, new InternetAddress(from), recipients, subject, text, html, attachments);

	}
	
	/** Creates a temp file to be used as an attachment.
	 * @return The temp file.
	 */
	private File createTempAttachmentFile() throws IOException {
		Writer w = null;
		File f = File.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX);
		log("Created temp file: " + f);
		try {
			w = new BufferedWriter(new FileWriter(f));
			w.write(TEST_DATA);
			log("Wrote test data");
		} finally {
			FileUtils.close(w);
			log("Closed temp file writer");
		}
		return f;
	}
	
	/** Gets the temporary attachment file.
	 * @return The temporary attachment file.
	 */
	private File getTempAttachmentFile() {
		return m_tempAttachmentFile;
	}
	
	private void log(String msg) {
		System.out.println(msg);
	}
}
