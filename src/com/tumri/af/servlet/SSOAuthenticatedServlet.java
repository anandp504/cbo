package com.tumri.af.servlet;

import java.io.IOException;
import java.net.SocketException;

import javax.naming.AuthenticationException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.tumri.sso.ssoc.InvalidSessionException;
import com.tumri.sso.ssoc.User;

/** A subclass of HttpServlet that allows users to do get and post
 * with requests authenticated by the SSO code.  
 * The user should override the protected methods:
 * <code>doAuthenticatedGet()</code> and
 * <code>doAuthenticatedPost()</code>
 * instead of 
 * <code>doGet()</code> and 
 * <code>doPost()</code>.
 * @author jkucera
 */
public abstract class SSOAuthenticatedServlet extends HttpServlet {

	private static final long serialVersionUID = 7444250558089332042L;

	private final static Logger log = Logger.getLogger(SSOAuthenticatedServlet.class);
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException  {
		try {
			SSOGetAction getAction = new SSOGetAction(this);
			getAction.exec(request, response);
		} catch(SocketException se) {
            if(SSOUtils.isBoringSocketException(se)) {}
			else handleOtherException(se, request, response);
		} catch(AuthenticationException ae) {
			handleAuthenticationException(ae, request, response);
		} catch(InvalidSessionException ise) {
			handleInvalidSessionException(ise, request, response);
		} catch(Exception e) {
			handleOtherException(e, request, response);
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException  {
		try {
			SSOPostAction postAction = new SSOPostAction(this);
			postAction.exec(request, response);
        } catch(SocketException se) {
            if(SSOUtils.isBoringSocketException(se)) {}
            else handleOtherException(se, request, response);
		} catch(AuthenticationException ae) {
			handleAuthenticationException(ae, request, response);
		} catch(InvalidSessionException ise) {
			handleInvalidSessionException(ise, request, response);
		} catch(Exception e) {
			handleOtherException(e, request, response);
		}
	}
	
	/** An extension of the doGet() method with the SSO user passed in.
	 * Implementations should subclass this method to do whatever the doGet() method would
	 * have done in HttpServlet.
	 * @param u The SSO user.
	 * @param request The servlet request.
	 * @param response The servlet response.
	 * @exception IOException If error reading or writing to the servlet.
	 * @exception ServletException If the servlet has problems.
	 */
	protected abstract void doAuthenticatedGet(User u, HttpServletRequest request, HttpServletResponse response) 
	                                                                           throws Exception;

	/** An extension of the doPost() method with the SSO user passed in.
	 * Implementations should subclass this method to do whatever the doGet() method would
	 * have done in HttpServlet.
	 * @param u The SSO user.
	 * @param request The servlet request.
	 * @param response The servlet response.
	 * @exception IOException If error reading or writing to the servlet.
	 * @exception ServletException If the servlet has problems.
	 */
	protected abstract void doAuthenticatedPost(User u, HttpServletRequest request, HttpServletResponse response) 
	                                                                         throws Exception;
	
	// --------------------------------- Exception handling --------------------------
	
	/** This method is called when authentication fails.
	 * The subclass may override this method to handle authentication failure in any way it wants.
	 * @param e The authentication exception.
	 * @return The result code to return to struts from the execute() method.
	 */
	protected void handleAuthenticationException(AuthenticationException e, 
				   HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.sendError(HttpServletResponse.SC_UNAUTHORIZED);	
	}

	/** This method is called when the session is invalid.
	 * The subclass may override this method to handle session expiration in any way it wants.
	 * @param e The invalid session exception.
	 * @return The result code to return to struts from the execute() method.
	 */
	protected void handleInvalidSessionException(InvalidSessionException e,
				  HttpServletRequest request, HttpServletResponse response) throws IOException {
	    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
	}

	/** This method is called when the an unhandled exception is generated from the
	 * doAuthenticatedExecute() method.
	 * The subclass may override this method to handle session expiration in any way it wants.
	 * @param t The throwable that was caught.
	 * @return The result code to return to struts from the execute() method.
	 */
	protected void handleOtherException(Throwable t, HttpServletRequest request, HttpServletResponse response) throws IOException {
		log.error("Other exception caught by authenticated servlet: ", t);
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	}


}
