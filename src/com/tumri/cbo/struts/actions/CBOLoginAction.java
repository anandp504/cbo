package com.tumri.cbo.struts.actions;

import java.util.Set;

import com.tumri.af.struts.actions.sso.LoginAction;
import com.tumri.af.utils.PropertyNotFoundException;
import com.tumri.cbo.utils.CBOConfigurator;
import com.tumri.sso.ssoc.User;

/** Overrides the generic SSO LoginAction to authorize the user.
 * @author jkucera
 */
public class CBOLoginAction extends LoginAction {

	private static final long serialVersionUID = -3246823863054606109L;

	/** Determines if the user is authorized to use CBO.
     * @param u The user.
     * @return True if the user is authorized or false if not.
     * @throws PropertyNotFoundException If the admin or user group name is not set.
     */
    protected boolean isAuthorized(User u) throws PropertyNotFoundException {
    	boolean authorized = false;
    	if(u != null) {
        	Set<String> groups = u.getGroups();
        	if(groups != null) { 
        		boolean isAdmin = groups.contains(CBOConfigurator.getCBOAdminGroupName());
        		boolean isUser = groups.contains(CBOConfigurator.getCBOUserGroupName());
        		authorized = isUser || isAdmin;
        	}
    	}
    	return authorized;
    }
}
