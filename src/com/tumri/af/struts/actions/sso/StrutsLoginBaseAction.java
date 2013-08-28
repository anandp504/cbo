package com.tumri.af.struts.actions.sso;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/** This is the base action that is to be extended to create a login struts action.
 * It assumes the application passes attributes called "username" and "password" to the
 * action to authenticate the user.
 */
abstract class StrutsLoginBaseAction extends StrutsBaseAction {

	private static final long serialVersionUID = 1L;
	
    private String m_username;
    private String m_password;
	
	
    public void setUsername(String username) {
        m_username = username;
    }

     public String getUsername() {
        return m_username;
    }
     
     public void setPassword(String password) {
         m_password = password;
     }

    public String getPassword() {
        return m_password;
    }
	
	/** This method overrides the superclass method to first log in the user before
	 * performing the action.
	 * It authenticates the user by using the username and password parameters
	 * in this class.
	 * If the user name or password is not right it throws an authentication exception.
	 * @return The result string from the result of calling authenticatedExecute().
	 */
	String doAuthenticatedExecute() throws Exception {
		SSOStrutsAuthenticator a = new SSOStrutsAuthenticator(this);
		HttpServletRequest request = getServletRequest();
		HttpServletResponse response = getServletResponse();
		a.authenticate(request, response, getUsername(), getPassword());
		a.exec(request, response);     // Calls back to authenticatedExecute() on success.
		return a.getResultString();
	}
}
