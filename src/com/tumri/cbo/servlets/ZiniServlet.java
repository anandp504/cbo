package com.tumri.cbo.servlets;

import java.io.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.naming.AuthenticationException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tumri.af.servlet.SSOAuthenticatedServlet;
import com.tumri.af.servlet.SSOUtils;
import com.tumri.af.utils.PropertyNotFoundException;
import com.tumri.cbo.backend.Bidder;
import com.tumri.cbo.utils.CBOConfigurator;
import com.tumri.mediabuying.zini.HTTPHandler;
import com.tumri.mediabuying.zini.HTTPListener;
import com.tumri.mediabuying.zini.Utils;
import com.tumri.sso.ssoc.User;

/** This is a revision to the ZiniDebugServlet that allows
 * the SSO User to be passed in.  The ZiniDebugServlet is
 * still around so we can use the old Flex login for a while.
 * It will be removed at some point.
 * @author jkucera
 */
public class ZiniServlet extends SSOAuthenticatedServlet {

	private static final long serialVersionUID = 5009343182708927340L;
    private static final String BROKEN_PIPE_MSG = "Broken pipe";

	/** An extension of the doGet() method with the SSO user passed in.
	 * Implementations should subclass this method to do whatever the doGet() method would
	 * have done in HttpServlet.
	 * @param u The SSO user.
	 * @param request The servlet request.
	 * @param response The servlet response.
	 * @exception IOException If error reading or writing to the servlet.
	 * @exception ServletException If the servlet has problems.
	 */
	protected void doAuthenticatedGet(User u, HttpServletRequest request, HttpServletResponse response) throws Exception
    {
        HTTPHandler.setRootURL(Bidder.CBO_NAME); 
		String path = request.getPathInfo();
		String context = request.getContextPath();
		String query = request.getQueryString();
		String uri = request.getRequestURI();
		String servletPath = request.getServletPath();
        OutputStream outputStream = response.getOutputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter printWriter = new PrintWriter(baos);
        BufferedReader bufferedReader =
           new BufferedReader(new InputStreamReader(request.getInputStream()));
        
        Map<String, String> params = getParams(u, request);
        if(query != null) {
                HTTPListener.recordArgs(query, params);
        }
        
        try
        {
		    Map<String, String> headers =
                    HTTPListener.processGet
                       (path, path, bufferedReader, printWriter, baos,
                        true, params);
            if(headers != null)
            {
                for(String k:headers.keySet())
                {
                    String v = headers.get(k);
                    response.setHeader(k, v);
                }
            }
            printWriter.flush();
            baos.flush();
            outputStream.write(baos.toByteArray());
            outputStream.flush();
        }
        catch (java.net.SocketException se)
        {
            String msg = se.getMessage();
            // Don't barf over broken pipes.  This is just the user clicking
            // on Stop, or some such.
            if(SSOUtils.isBoringSocketException(se)) {}
            else throw Utils.barf(se, path, context, query, uri, servletPath);
        }
        catch (Error err)
        {
           throw Utils.barf(err, path, context, query, uri, servletPath);
        }
	}
	
	/** An extension of the doPost() method with the SSO user passed in.
	 * Implementations should subclass this method to do whatever the doPost() method would
	 * have done in HttpServlet.
	 * @param u The SSO user.
	 * @param request The servlet request.
	 * @param response The servlet response.
	 * @exception IOException If error reading or writing to the servlet.
	 * @exception ServletException If the servlet has problems.
	 */
	protected void doAuthenticatedPost(User u, HttpServletRequest request, HttpServletResponse response) throws Exception
    {
        String path = request.getPathInfo();
        String context = request.getContextPath();
        String query = request.getQueryString();
        String uri = request.getRequestURI();
        String servletPath = request.getServletPath();
        OutputStream outputStream = response.getOutputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter printWriter = new PrintWriter(baos);
        BufferedReader bufferedReader =
           new BufferedReader(new InputStreamReader(request.getInputStream()));
        
        Map<String, String> params = getParams(u, request);
        if(query != null) {
        	HTTPListener.recordArgs(query, params);
        }
        
        try
        {
            Map<String, String> headers =
                    HTTPListener.processPost
                       (path, path, bufferedReader, printWriter, baos,
                        true, params);
            if(headers != null)
            {
                for(String k:headers.keySet())
                {
                    String v = headers.get(k);
                    response.setHeader(k, v);
                }
            }
            printWriter.flush();
            baos.flush();
            outputStream.write(baos.toByteArray());
            outputStream.flush();
        }
        catch (java.net.SocketException se)
        {
            String msg = se.getMessage();
            // Don't barf over broken pipes.  This is just the user clicking
            // on Stop, or some such.
            if(SSOUtils.isBoringSocketException(se)) {}
            else throw Utils.barf(se, path, context, query, uri, servletPath);
        }
        catch (Error err)
        {
           throw Utils.barf(err, path, context, query, uri, servletPath);
        }
	}
	
	private Map<String, String> getParams(User u, HttpServletRequest request) 
						throws AuthenticationException, PropertyNotFoundException
    {
        Map<String, String> params = new HashMap<String, String>();
        Enumeration<?> e = request.getHeaderNames();
        while(e.hasMoreElements())
        {
            Object n = e.nextElement();
            if(n instanceof String)
                params.put((String) n, request.getHeader((String) n));
        }
        
        if(u != null) {
        	String user = u.getUid();
        	if(user != null) params.put(HTTPHandler.COLLECTIVE_USER_ID_PARAM, user);
        	String email = u.getEmail();
        	if(email != null) params.put(HTTPHandler.COLLECTIVE_EMAIL_PARAM, email);
        	
    		boolean isAdmin = false;
    		boolean isUser = false;
    		
        	Set<String> groups = u.getGroups();
        	if(groups != null) {
        		isAdmin = groups.contains(CBOConfigurator.getCBOAdminGroupName());
        		isUser = groups.contains(CBOConfigurator.getCBOUserGroupName());
        	}
        	// The following should never happen since the CBOLoginAction
        	// checks to make sure the user is authorized to use this application.
        	if(!(isUser || isAdmin)) {
        		throw new AuthenticationException("User '" + user + "' is not authorized to use CBO");
        	}
        	params.put(HTTPHandler.IS_COLLECTIVE_USER, String.valueOf(isUser));
        	params.put(HTTPHandler.IS_COLLECTIVE_ADMIN, String.valueOf(isAdmin));
        }
        return params;
    }
}
