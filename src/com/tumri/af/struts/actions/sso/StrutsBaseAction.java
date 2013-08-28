package com.tumri.af.struts.actions.sso;

import javax.naming.AuthenticationException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts2.interceptor.ServletRequestAware;
import org.apache.struts2.interceptor.ServletResponseAware;

import com.opensymphony.xwork2.ActionSupport;

import com.tumri.sso.ssoc.InvalidSessionException;
import com.tumri.sso.ssoc.User;

/** This is the base action that is to be extended to create authenticated struts actions.
 * It should be called only after a user has been logged in using the StrutsLoginBaseAction.
 */
public abstract class StrutsBaseAction extends ActionSupport implements ServletRequestAware, ServletResponseAware {

	private static final long serialVersionUID = 1L;
	
	private HttpServletRequest request = null;
	private HttpServletResponse response = null;
	
	/** Struts calls this method to execute the action.
	 * This calls the authenticator to make sure the user is authenticated
	 * which then calls back to the authenticatedExecute method.
	 * Different actions must implement the authenticatedExecute method
	 * and put the logic that used to be in execute() into that method.
	 * The authenticatedExecute() method returns the struts result code string
	 * that would ordinarily have been returned from execute().
	 */
	public final String execute() {
		try {
			return doAuthenticatedExecute();
		} catch(AuthenticationException ae) {
			return handleAuthenticationException(ae);
		} catch(InvalidSessionException ise) {
			return handleInvalidSessionException(ise);
		} catch(Throwable t) {
			Logger.getRootLogger().error("Exception caught during action ", t);
			return handleOtherException(t);
		}
	}
	
	public void setServletRequest(HttpServletRequest request) {
		this.request = request;
	}
	
	public HttpServletRequest getServletRequest() {
		return this.request;
	}

	public void setServletResponse(HttpServletResponse response) {
		this.response = response;
	}
	
	public HttpServletResponse getServletResponse() {
		return this.response;
	}

	/** This method is called back from the authenticator after successful authentication.
	 * It should be implemented by subclasses to do the action within
	 * the authentication context.
	 * @param u The user.
	 * @param request The servlet request.
	 * @param response The servlet response.
	 * @return The struts result code to return from the execute() method.
	 * @throws Exception Any exception that is not handled.
	 */
	public abstract String authenticatedExecute(User u, HttpServletRequest request,  HttpServletResponse response) throws Exception;	

	/** This method is called when authentication fails.
	 * The subclass may override this method to handle authentication failure in any way it wants.
	 * @param e The authentication exception.
	 * @return The result code to return to struts from the execute() method.
	 */
	public abstract String handleAuthenticationException(AuthenticationException e);

	/** This method is called when the session is invalid.
	 * The subclass may override this method to handle session expiration in any way it wants.
	 * @param e The invalid session exception.
	 * @return The result code to return to struts from the execute() method.
	 */
	public abstract String handleInvalidSessionException(InvalidSessionException e);

	/** This method is called when the an unhandled exception is generated from the
	 * doAuthenticatedExecute() method.
	 * The subclass may override this method to handle session expiration in any way it wants.
	 * @param t The throwable that was caught.
	 * @return The result code to return to struts from the execute() method.
	 */
	public abstract String handleOtherException(Throwable t);

	/** This method overrides the superclass method to first log in the user before
	 * performing the action.
	 * It authenticates the user by using the username and password parameters
	 * in this class.
	 * If the user name or password is not right it throws an authentication exception.
	 * @return The result string from the result of calling authenticatedExecute().
	 * @exception AuthenticationException If the user cannot be authenticated.
	 * @exception InvalidSessionException If the session is no longer valid.
	 * @exception Exception If another unhandled exception is encountered.
	 */
	String doAuthenticatedExecute() throws AuthenticationException, InvalidSessionException, Exception {
		SSOStrutsAuthenticator a = new SSOStrutsAuthenticator(this);	
		a.exec(getServletRequest(), getServletResponse());  // Calls back to authenticatedExecute() on success.
		return a.getResultString();
	}
}
