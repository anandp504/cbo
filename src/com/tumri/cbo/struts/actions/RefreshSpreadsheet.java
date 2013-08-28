package com.tumri.cbo.struts.actions;

import java.io.Reader;
import java.io.Writer;

import com.tumri.af.struts.actions.sso.AbstractUserXMLAction;
import com.tumri.cbo.backend.Bidder;

import com.tumri.sso.ssoc.User;

/** Reads the current spreadsheet and runs the optimizer on it
 * producing a new spreadsheet.
 */
public class RefreshSpreadsheet extends AbstractUserXMLAction {

	private static final long serialVersionUID = -1L;
		
	/** Reads the current spreadsheet and runs the optimizer on it
	 * producing a new spreadsheet.
	 * @param u The user.
	 * @param r The reader that contains the contents of the post.
	 * @param w The writer to which to write the output.
	 * @exception Exception if error doing the action.
	 */
	public void execute(User u, Reader r, Writer w) throws Exception {
        			
        Bidder bidder = Bidder.getInstance();
        if(bidder == null) {
        	throw new IllegalStateException("Bidder has not been initialized");
        }
        bidder.processBidInstructions();
        w.write("<result>success</result>");
	}
}

