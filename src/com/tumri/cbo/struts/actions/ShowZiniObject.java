package com.tumri.cbo.struts.actions;

import java.io.Reader;
import java.io.Writer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tumri.af.struts.actions.sso.AbstractUserXMLAction;

import com.tumri.mediabuying.zini.HTMLifier;
import com.tumri.sso.ssoc.User;

/** Reads the current spreadsheet and runs the optimizer on it
 * producing a new spreadsheet.
 */
public class ShowZiniObject extends AbstractUserXMLAction {

	private static final long serialVersionUID = 2687714566470053551L;
	
	private final static String INITIAL_ZINI_OBJECT_NAME = "MANAGER";
	private final static String PATH_PREFIX="showZiniObject.action?name=";
	
	private String m_name;
	
	/** Sets the name of the ZINI object whose page is to be shown.
	 * @param name The name of the ZINI object whose page is to be shown.
	 */
	public void setName(String name) {
		m_name = name;
	}
	
	/** Gets the name of the ZINI object whose page is to be shown.
	 * @return The name of the ZINI object whose page is to be shown.
	 */
	public String getName() {
		return m_name;
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
		response.resetBuffer();
		response.setCharacterEncoding("UTF-8");
		response.setContentType("text/html");

		String name = getName();
		if(name == null) {
			name = INITIAL_ZINI_OBJECT_NAME;
		}
		String stylesheetUrl = null;
		Writer w = response.getWriter();
		HTMLifier.htmlifyObjectGivenName
                (w, name, stylesheetUrl, PATH_PREFIX, null, null, false);
		w.flush();
		
		return SUCCESS;
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
