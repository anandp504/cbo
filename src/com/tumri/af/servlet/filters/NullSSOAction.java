package com.tumri.af.servlet.filters;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tumri.sso.ssoc.BaseAction;

/** A single-sign on action that does nothing.
 * This can be used to determine if the user is authenticated by calling
 * <code>exec(request, response)</code>.
 * If exec throws javax.naming.AuthenticationException
 * or com.tumri.sso.ssoc.InvalidSessionException
 * then the session has not been authenticated.
 * @author jkucera
 *
 */
class NullSSOAction extends BaseAction {
	
	/** Implementation of SSO base action run method that does nothing.
	 * @param request The servlet request.
	 * @param response The servlet response.
	 * @exception Exception If anything else went wrong.
	 */
	public final void run(HttpServletRequest request,  HttpServletResponse response) throws Exception {
		// Does nothing.
	}
}
