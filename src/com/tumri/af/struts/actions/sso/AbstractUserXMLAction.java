package com.tumri.af.struts.actions.sso;

import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tumri.sso.ssoc.User;

/** An action that authenticates the user and passes it and the input reader and output writer
 * to an abstract execute() method that processes the action and writes an XML response back
 * to the writer.
 */
public abstract class AbstractUserXMLAction extends BaseAction {

	private static final long serialVersionUID = -6903539466818616783L;

	/** This method is called back from the authenticator after successful authentication.
	 * It resets the servlet response buffer,
	 * sets the character encoding of the response to UTF-8,
	 * and sets the content type to "application/xml".
	 * It then creates a temporary string buffer for the response and calls execute().
	 * Finally it writes the response from the execute() method to the servlet output stream in UTF-8.
	 * @param u The user.
	 * @param request The servlet request.
	 * @param response The servlet response.
	 * @return The struts result code to return from the execute() method.
	 * @throws Exception Any exception that is not handled.
	 */
	public String authenticatedExecute(User u, HttpServletRequest request,  HttpServletResponse response) throws Exception {
		response.resetBuffer();
		response.setCharacterEncoding("UTF-8");
		response.setContentType("application/xml");

		// Ideally the output stream writer should be passed in here, 
		// but if there are exceptions we might end up with partially truncated XML.
		// What does struts do if the return code is an error but some data has been written?
		StringWriter w = new StringWriter(65536);    // Buffer the result in case of exceptions.
		execute(u, request.getReader(), w);
		
		// Write the result after execution so exceptions can be handled above.
		// Convert all output to UTF-8.
	    OutputStreamWriter ow = new OutputStreamWriter(response.getOutputStream(), "UTF-8");
	    ow.write(w.toString());
	    ow.flush();
		return SUCCESS;
	}

	/** This method is called with a non-null user, a reader and a writer.
	 * It does the actual method processing.
	 * Most error handling is done here.
	 * @param u The current user (never null).
	 * @param r The reader (never null).
	 * @param w The writer (never null).
	 * @throws Exception
	 */
	public abstract void execute(User u, Reader r, Writer w) throws Exception;
}
