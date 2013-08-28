package com.tumri.af.servlet.filters;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.naming.AuthenticationException;

import com.tumri.sso.ssoc.InvalidSessionException;

/** This filter allows access only to the web pages that a user is supposed to see.
 * It prevents the user from seeing anything in the protected/ directory unless they
 * are logged in.  If a user is blocked from seeing a page the response is redirected
 * to the login page "login.jsp" under the current web context.
 */
public class SecurityFilter implements Filter
{
	private FilterConfig filterConfig;
			
	// The name of the "secure" directory.
	// Any URL that contains this string will be blocked unless the user is logged in.
	private final static String SECURE_DIRECTORY = "/secure";
	
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
		try
		{
			if((request instanceof HttpServletRequest) && (response instanceof HttpServletResponse)) {
				HttpServletRequest httpRequest = (HttpServletRequest) request;
				HttpServletResponse httpResponse = (HttpServletResponse) response;

				String requestURI = httpRequest.getRequestURI();
				if(isPublicPage(requestURI) || isAuthenticated(httpRequest, httpResponse)) {
					chain.doFilter(request, response);
				} else {
					httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void init(FilterConfig config) throws ServletException {
	    setFilterConfig(config);
	}

	public void setFilterConfig (FilterConfig filterConfig) {
		this.filterConfig = filterConfig;
	}

	public FilterConfig getFilterConfig() {
		return this.filterConfig;
	}
	
	public void destroy() {    
	}
	
	// ------------------------------ Private methods -----------------------
	
	/** Determines if the user is authenticated with the single-sign-on mechanism.
	 * @return True if the user is authenticated or false if not.
	 * @exception Exception If any other error.
	 */
	private boolean isAuthenticated(HttpServletRequest request, HttpServletResponse response) throws Exception {
		boolean authenticated = false;
		try {
			NullSSOAction nullAction = new NullSSOAction();
			nullAction.exec(request, response);
			authenticated = true;
		} catch (InvalidSessionException ise) {
			authenticated = false;	
		} catch(AuthenticationException ae) {
			authenticated = false;
		}
		return authenticated;
	}
	
	/** Determines if the URI can be displayed to the public without a login.
	 * @param requestURI The request URI relative to the servlet context base.
	 * @return True if this is a public page or false if not.
	 */
	private boolean isPublicPage(String requestURI) {
		boolean result = (requestURI != null) && (!requestURI.contains(SECURE_DIRECTORY));
		return result;
	}
}
