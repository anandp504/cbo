package com.tumri.af.servlet.filters;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class DebugFilter implements Filter
{
	private FilterConfig filterConfig;

	public void doFilter(ServletRequest request,
			ServletResponse response,
			FilterChain chain)
	{
		try
		{
		    HttpServletRequest httpRequest = (HttpServletRequest) request;
		    HttpSession session = httpRequest.getSession();
		    String debugFlag = request.getParameter("debug");
		    if(debugFlag != null) {
		        //set it session wide
		        System.out.println ("setting session wide debug filter to : " + debugFlag); 
		        session.setAttribute("debug", debugFlag);
		    } else {
		        //if you started with login remove the debug flag from session
		        String requestUrl = httpRequest.getServletPath();
		        if(requestUrl.contains("jsp/admin/login.action")) {
		            session.setAttribute("debug", null);
		        }
		        
		    }
			chain.doFilter (request, response);

		} catch (IOException io) {
			System.out.println ("IOException raised in DebugFilter");
			io.printStackTrace();
		} catch (ServletException se) {
			System.out.println ("ServletException raised in DebugFilter");
			se.printStackTrace();
		}
	}
	public FilterConfig getFilterConfig()
	{
		return this.filterConfig;
	}
	public void setFilterConfig (FilterConfig filterConfig)
	{
		this.filterConfig = filterConfig;
	}
	public void destroy() {
	    
	}
	public void init(FilterConfig config) throws ServletException {
	    filterConfig = config;
	}
}
