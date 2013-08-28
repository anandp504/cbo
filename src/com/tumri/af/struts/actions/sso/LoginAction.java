package com.tumri.af.struts.actions.sso;

import java.text.MessageFormat;

import javax.naming.AuthenticationException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.tumri.sso.ssoc.User;
import com.tumri.sso.ssoc.InvalidSessionException;

public abstract class LoginAction extends StrutsLoginBaseAction {

	private static final long serialVersionUID = 1090175312864855693L;

	private static final Logger log = Logger.getLogger(LoginAction.class);
    
    private final static String MSG_NO_ERROR = "";
    private final static String MSG_INVALID_USER_OR_PASSWORD = "Invalid username or password";
    private final static String MSG_SESSION_EXPIRED = "Error - Session expired";
    private final static String MSG_SUCCESSFUL_LOGIN = "Login successful for user: {0}";
    private final static String MSG_EXCEPTION = "Exception occurred while logging in for user: {0}\n";
    private final static String MSG_UNAUTHORIZED = "You are not authorized to use this application.";
    
    private final static String HAS_FAILED_TRUE = "yes";
    private final static String HAS_FAILED_FALSE = "no";
    
    // Struts code for unauthorized.
    private final static String UNAUTHORIZED = "unauthorized";
    
    private String hasFailed="no";
    private String errorMessage = MSG_NO_ERROR;
    
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
        try {
            log.info(MessageFormat.format(MSG_SUCCESSFUL_LOGIN, getUsername()));
            if(isAuthorized(u)) {
            	setHasFailed(HAS_FAILED_FALSE);
            	setErrorMessage(MSG_NO_ERROR);
            	return onLogin(u, request, response);
            } else {
            	setHasFailed(HAS_FAILED_TRUE);
            	setErrorMessage(MSG_UNAUTHORIZED);
            	return UNAUTHORIZED;
            }
        } catch (Throwable t) {
            String msg = MessageFormat.format(MSG_EXCEPTION, getUsername());
            log.error(msg, t);
            setErrorMessage(msg);
            setHasFailed(HAS_FAILED_TRUE);
            return ERROR;
        }
    }
    
    /** Performs any extra actions to be done upon login
     * after authorization.  
     * Returns the struts return value associated with the result.  
     * This implementation just returns <code>SUCCESS</code>.
     * Subclasses can override it to implement login-specific actions if needed.
     * @param u The user.
     * @param request The servlet request.
     * @param response The servlet response.
     * @exception Exception If error.
	 * @return SUCCESS on success or any other Struts action code.
	 */
    public String onLogin(User u, HttpServletRequest request,  HttpServletResponse response) throws Exception {
    	return SUCCESS;
    }

    /** This method is called when authentication fails.
     * The subclass may override this method to handle authentication failure in any way it wants.
     * @param e The authentication exception.
     * @return The result code to return to struts from the execute() method.
     */

    public String handleAuthenticationException(AuthenticationException e) {
    	String msg = MSG_INVALID_USER_OR_PASSWORD;
        log.warn(msg); // Was log.error, but this resulted in mail to admins
        // for failed logins, which is silly.
        setErrorMessage(msg);
        setHasFailed("yes");
        return INPUT;
    }

    /** This method is called when the session is invalid.
     * The subclass may override this method to handle session expiration in any way it wants.
     * @param e The invalid session exception.
     * @return The result code to return to struts from the execute() method.
     */
    public String handleInvalidSessionException(InvalidSessionException e) {
    	String msg = MSG_SESSION_EXPIRED;
        log.error(msg);
        setErrorMessage(msg);
        setHasFailed("yes");
        return INPUT;
    }

    /** This method is called when the an unhandled exception is generated from the
     * doAuthenticatedExecute() method.
     * The subclass may override this method to handle session expiration in any way it wants.
     * @param t The throwable that was caught.
     * @return The result code to return to struts from the execute() method.
     */
    public String handleOtherException(Throwable t) {
    	String msg = MessageFormat.format(MSG_EXCEPTION, getUsername());
        log.error(msg);
        setErrorMessage(msg);
        setHasFailed("yes");
        return INPUT;
    }

    public String getHasFailed() {
        return hasFailed;
    }

    public void setHasFailed(String hasFailed) {
        this.hasFailed = hasFailed;
    }

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	/** Checks to see if the user has privileges to run
	 * the application.  
	 * This implementation just returns true.
	 * @param u The SSO User.
	 * @return True if the user is authorized or false if not.
	 * @throws Exception If error determining if the user is authorized.
	 */
	protected abstract boolean isAuthorized(User u) throws Exception;
}