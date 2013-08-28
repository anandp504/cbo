package com.tumri.af.struts.actions.sso;


import javax.naming.AuthenticationException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.tumri.sso.ssoc.InvalidSessionException;

import com.tumri.af.utils.Utils;

public abstract class BaseAction extends StrutsBaseAction {

	private static final long serialVersionUID = 1L;

	private final static int MAX_STACK_TRACE_CHARS = 600;
	private final static String MSG_UNAUTHROIZED = "You are not authorized to view the result.  Please login in again.";
	private final static String MSG_SESSION_EXPIRED = "Your session has expired.  Please log in again.";
	
	private final static String SERVER_ERROR_MESSAGE = "Please relay the following information to support:\n";

	// This probably could just return success.
	private static String INVALID_USER = "invalidUser";

	private final static Logger log = Logger.getLogger(BaseAction.class);

    private String dt = ""; //dummy variable to beat browser cache
    private int msgId = -1;
    private String errorMessage = "Server encountered an error while processing the request.";
	  
    public HttpSession getHttpSession() {
    	return getServletRequest().getSession();
    }
    
    /** Sets the dt parameter.
     * This is only implemented to support the cache buster dt string.
     * It is never used in the application.
     * @param dt the dt to set
     */
    public void setDt(String dt) {
        this.dt = dt;
    }

    /** Gets the dt parameter.
     * This is only implemented to support the cache buster dt string.
     * It is never used in the application.
     * @return the dt
     */
    public String getDt() {
        return dt;
    }

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	
	public int getMsgId() {
		return msgId;
	}

	public void setMsgId(int msgId) {
		this.msgId = msgId;
	}
	
	// --------------------------------- Exception handling --------------------------
	
	/** This method is called when authentication fails.
	 * The subclass may override this method to handle authentication failure in any way it wants.
	 * @param e The authentication exception.
	 * @return The result code to return to struts from the execute() method.
	 */
	public String handleAuthenticationException(AuthenticationException e) {
	    log.warn("Authentication failed");
		setMsgId(HttpServletResponse.SC_UNAUTHORIZED);
		setErrorMessage(MSG_UNAUTHROIZED);
		return INVALID_USER;		
	}

	/** This method is called when the session is invalid.
	 * The subclass may override this method to handle session expiration in any way it wants.
	 * @param e The invalid session exception.
	 * @return The result code to return to struts from the execute() method.
	 */
	public String handleInvalidSessionException(InvalidSessionException e) {
	    log.warn("Session expired");	
		setMsgId(HttpServletResponse.SC_UNAUTHORIZED);
		setErrorMessage(MSG_SESSION_EXPIRED);
		return INVALID_USER;		
	}

	/** This method is called when the an unhandled exception is generated from the
	 * doAuthenticatedExecute() method.
	 * The subclass may override this method to handle session expiration in any way it wants.
	 * @param t The throwable that was caught.
	 * @return The result code to return to struts from the execute() method.
	 */
	public String handleOtherException(Throwable t) {
		String msg = "Exception occurred while fetching data: ";
		log.error(msg, t);
        String stackTrace = Utils.getStackTrace(t);
        log.error(stackTrace);
		setMsgId(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		setErrorMessage(SERVER_ERROR_MESSAGE + stackTrace.substring(0, MAX_STACK_TRACE_CHARS));
        return ERROR;
	}
}
