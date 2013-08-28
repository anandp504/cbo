package com.tumri.af.struts.actions.sso;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.tumri.sso.ssoc.User;

/** The action to be called to log the user out of the session.
 * It just invalidates the HTTP session.
 * @author jkucera
 *
 */
public class LogoutAction extends BaseAction {
    
	private static final long serialVersionUID = -5164505072567524980L;

	/** This method is called back from the authenticator after successful authentication.
	 * It should be implemented by subclasses to do the action within
	 * the authentication context.
	 * @param u The user.
	 * @param request The servlet request.
	 * @param response The servlet response.
	 * @return The struts result code to return from the execute() method.
	 * @throws Exception Any exception that is not handled.
	 */
	public String authenticatedExecute(User u, HttpServletRequest request,  HttpServletResponse response) throws Exception {
		// This should clear the cookies.
		response.reset();
		
		// The following sets the SSO cookie values to blank
		// and removes the user from the user cache.
		// Construct a null action to use to logout.
		NullSSOAction nsa = new NullSSOAction();
		nsa.logout(request, response);
		
		// Invalidating the session must be done
		// after the logout.
		HttpSession session = getHttpSession();
		session.invalidate();
		
		setErrorMessage("");
		return SUCCESS;
	}
}
