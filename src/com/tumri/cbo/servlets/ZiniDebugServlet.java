package com.tumri.cbo.servlets;

import java.io.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tumri.mediabuying.zini.HTTPHandler;
import com.tumri.mediabuying.zini.HTTPListener;
import com.tumri.mediabuying.zini.Utils;

/** This servlet is used when the user clicks on the "Debug"
 * link on the Flex UI of CBO.  The Flex UI is being deprecated
 * in favor of just using the zini stuff as the entire application.
 * This class is still around to keep the flex UI working while
 * we transition to the HTML UI.
 * @author jkucera
 * @deprecated
 */
@Deprecated
public class ZiniDebugServlet extends HttpServlet {

	private static final long serialVersionUID = 1350020117210231162L;


	protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws IOException
    {
		String path = request.getPathInfo();
		String context = request.getContextPath();
		String query = request.getQueryString();
		String uri = request.getRequestURI();
		String servletPath = request.getServletPath();
        OutputStream outputStream = response.getOutputStream();
        PrintWriter printWriter = new PrintWriter(outputStream);
        BufferedReader bufferedReader =
           new BufferedReader(new InputStreamReader(request.getInputStream()));
        Map<String, String> params = getParams(request);
        if(query != null)
                HTTPListener.recordArgs(query, params);
		// System.out.println("path = " + path + ", context = " + context + ", query = " + query + ", uri = " + uri + ", servletPath = " + servletPath);
        try
        {
		    Map<String, String> headers =
                    HTTPListener.processGet
                       (path, path, bufferedReader, printWriter, outputStream,
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
            outputStream.flush();
        }
        catch (Error err)
        {
            throw Utils.barf(err, path, context, query, uri, servletPath);
        }
	}
	
	protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws IOException
    {
        String path = request.getPathInfo();
        String context = request.getContextPath();
        String query = request.getQueryString();
        String uri = request.getRequestURI();
        String servletPath = request.getServletPath();
        OutputStream outputStream = response.getOutputStream();
        PrintWriter printWriter = new PrintWriter(outputStream);
        BufferedReader bufferedReader =
           new BufferedReader(new InputStreamReader(request.getInputStream()));
        Map<String, String> params = getParams(request);
        if(query != null)
                HTTPListener.recordArgs(query, params);
        // System.out.println("path = " + path + ", context = " + context + ", query = " + query + ", uri = " + uri + ", servletPath = " + servletPath);
        try
        {
            Map<String, String> headers =
                    HTTPListener.processPost
                       (path, path, bufferedReader, printWriter, outputStream,
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
            outputStream.flush();
        }
        catch (Error e)
        {
            throw Utils.barf(e, path, context, query, uri, servletPath);
        }
	}
	
	private Map<String, String> getParams(HttpServletRequest request)
    {
        Map<String, String> params = new HashMap<String, String>();
        Enumeration<?> e = request.getHeaderNames();
        while(e.hasMoreElements())
        {
            Object n = e.nextElement();
            if(n instanceof String)
                params.put((String) n, request.getHeader((String) n));
        }
        String remoteUser = request.getRemoteUser();
        if(remoteUser != null)
            params.put(HTTPHandler.COLLECTIVE_USER_ID_PARAM, remoteUser);
        return params;
    }
}
