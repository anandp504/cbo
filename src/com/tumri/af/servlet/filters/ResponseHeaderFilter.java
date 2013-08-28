package com.tumri.af.servlet.filters;

import javax.servlet.Filter;
import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ResponseHeaderFilter implements Filter {

	FilterConfig fc;

	@SuppressWarnings("unchecked")
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		
		HttpServletResponse response = (HttpServletResponse) res;
		response.setHeader("Cache-Control", "");
		response.setHeader("Pragma", "");
		// response.setHeader("Expires","Thu, 15 Apr 2010 20:00:00 GMT");
		response.setHeader("Expires", "");
		// set the provided HTTP response parameters

		for (Enumeration<String> e = fc.getInitParameterNames(); e.hasMoreElements();) {
			String headerName = e.nextElement();
			if (headerName.equals("Cache-Control")) {
				long currentTimeMillis = System.currentTimeMillis();
				String configCacheTime = fc.getInitParameter(headerName);
				long time = 1000 * Long.parseLong(configCacheTime);
				Date expiresDate = new Date(time + currentTimeMillis);
				SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
				String expiresHeader = format.format(expiresDate);
				response.addHeader("Expires", expiresHeader);
				response.addHeader(headerName, "max-age=" + configCacheTime + ",private");
			} else {
				response.addHeader(headerName, fc.getInitParameter(headerName));
			}

		}
		// pass the request/response on
		chain.doFilter(req, response);
	}

	public void init(FilterConfig filterConfig) {
		this.fc = filterConfig;
	}

	public void destroy() {
		this.fc = null;
	}
}
