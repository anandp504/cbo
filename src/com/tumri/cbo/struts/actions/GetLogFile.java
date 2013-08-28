package com.tumri.cbo.struts.actions;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.text.ParseException;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.tumri.af.struts.actions.sso.AbstractUserXMLAction;
import com.tumri.af.utils.DateUtils;
import com.tumri.af.utils.FileUtils;
import com.tumri.cbo.utils.CBOConfigurator;
import com.tumri.sso.ssoc.User;

/** Downloads a log file.
 */
public class GetLogFile extends AbstractUserXMLAction {

	private static final long serialVersionUID = 3258516446307184094L;

	private final static Logger log = Logger.getLogger(GetLogFile.class);
	
	private final static String LOG_FILE_NAME_PREFIX = "cbo.log";
	private final static int LOG_FILE_NAME_PREFIX_LENGTH = LOG_FILE_NAME_PREFIX.length();

	private final static String MIME_TYPE_TEXT = "text/plain";
	private final static String CONTENT_DISPOSITION = "Content-Disposition";
	private final static String ATTACHMENT_FILENAME_PREFIX = "attachment; filename=";

	private String m_fileName;

	/** Sets the name of the log file to download.
	 * @param fileName The file name without the path.
	 */
	public void setName(String fileName) {
		m_fileName = fileName;
	}

	/** Gets the name of the log file to download.
	 * @return The file name without the path.
	 */
	public String getName() {
		return m_fileName;
	}

	/** Overrides the superclass method to write the HTML result of inspecting an object
	 * directly to the 
	 * @param u The user.
	 * @param request The servlet request.
	 * @param response The servlet response.
	 * @return The struts result code to return from the execute() method.
	 * @throws Exception Any exception that is not handled.
	 */
	public String authenticatedExecute(User u, HttpServletRequest request,  HttpServletResponse response) throws Exception {
		
		String result = ERROR;
		String fileName = getName();
		
		/* tests
		String[] tests = { "cbo.log", "", ".", "2011-02-03", ".1999-07-30", "cbo.log..\\Foo", "cbo.log.2012-03-14"};
		for(String name :tests) {
			System.out.println("isValidLogFileName(" + name +") =" + isValidLogFileName(name));
		}
		*/
		
		if(!isValidLogFileName(fileName)) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			log.error("Invalid log file name: " + fileName);
		} else {

			response.resetBuffer();
			response.setContentType(MIME_TYPE_TEXT);
			response.addHeader(CONTENT_DISPOSITION, ATTACHMENT_FILENAME_PREFIX + fileName);

			File f = new File(new File(CBOConfigurator.getLogDirectoryPath()), fileName);
			if(!f.exists()) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
			}

			InputStream in =  null;
			OutputStream out = response.getOutputStream();
			try {
				in = new FileInputStream(f);
				FileUtils.copyStream(in, out);
				out.flush();
				result = SUCCESS;
			} catch (Exception e) {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				log.error("Exception downloading file: ", e);
			} finally {
				FileUtils.close(in);
			}
		}
		return result;
	}
	
	/** Dummy implementation that does nothing.
	 * It is not called but needs to be implemented.
	 * @param u The user.
	 * @param r The reader that contains the contents of the post.
	 * @param w The writer to which to write the output.
	 * @exception Exception if error doing the action.
	 */
	public void execute(User u, Reader r, Writer w) throws Exception {
		// Does nothing
	}
	
	// ----------------- Private methods ------------------
	
	private boolean isValidLogFileName(String s) {
		boolean result = false;
		if((s != null) && s.startsWith(LOG_FILE_NAME_PREFIX)) {
			int len = s.length();
			if(len == LOG_FILE_NAME_PREFIX_LENGTH) {
				result = true;
			} else if(s.charAt(LOG_FILE_NAME_PREFIX_LENGTH) == '.') {
				try {
					Date d = DateUtils.parseCanonicalDateString(s.substring(LOG_FILE_NAME_PREFIX_LENGTH + 1));
					result = (d != null);
				} catch(ParseException pe) {
					result = false;
				}
			}
		}
		return result;
	}
}


