package com.tumri.af.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tumri.sso.ssoc.BaseAction;

class SSOGetAction extends BaseAction {
	
	SSOAuthenticatedServlet m_authenticatedServlet;
		
	SSOGetAction(SSOAuthenticatedServlet as) {
		m_authenticatedServlet = as;
	}
	
	/** This method is called after the user has been authenticated.
	 * It passes the user to the extended "get" method of the SSOAuthenticatedServlet.
	 * @param request The servlet request.
	 * @param response The servlet response.
	 */
	protected void run(HttpServletRequest request, HttpServletResponse response) throws Exception {
		m_authenticatedServlet.doAuthenticatedGet(getSession().getUser(), request, response);
	}
}
