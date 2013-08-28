package com.tumri.af.struts.actions.sso;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

class SSOStrutsAuthenticator extends com.tumri.sso.ssoc.BaseAction {

	private static final long serialVersionUID = 1L;
	
	private StrutsBaseAction m_callback;
	private String m_resultString;
	
	/** Package private constructor. 
	 * @param sba The struts base action (never null).
	 */
	SSOStrutsAuthenticator(StrutsBaseAction sba) {
		m_callback = sba;
	}

	/** Implementation of SSO base action run method.
	 * @param request The servlet request.
	 * @param response The servlet response.
	 * @exception Any exception that is thrown,
	 */
	public void run(HttpServletRequest request,  HttpServletResponse response) throws Exception {
		m_resultString = m_callback.authenticatedExecute(getSession().getUser(), request, response);
	}
	
	/** Package private method to get the result of the execution.
	 * @return The result string.
	 */
	String getResultString() {
		return m_resultString;
	}
}
