package com.tumri.cbo.struts.actions;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.tumri.af.struts.actions.sso.AbstractUserXMLAction;
import com.tumri.af.utils.FileUtils;
import com.tumri.cbo.utils.CBOConfigurator;
import com.tumri.sso.ssoc.User;

/** Returns the current spreadsheet.
 */
public class GetSpreadsheet extends AbstractUserXMLAction {

	private static final long serialVersionUID = -391621779060421267L;

	private final static Logger log = Logger.getLogger(GetSpreadsheet.class);

	private final static String MIME_TYPE_XLS = "application/vnd.ms-excel";
	private final static String CONTENT_DISPOSITION = "Content-Disposition";
	private final static String ATTACHMENT_FILENAME = "attachment; filename=campaignSummary.xls";
	
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
		
		response.resetBuffer();
		response.setContentType(MIME_TYPE_XLS);
		response.addHeader(CONTENT_DISPOSITION, ATTACHMENT_FILENAME);


		File f = new File(CBOConfigurator.getSpreadsheetFilePath());
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
			log.error("Exception downloading spreadsheet: ", e);
		} finally {
			FileUtils.close(in);
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
		// Not called by the authenticatedExecute method.
	}
}


