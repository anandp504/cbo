package com.tumri.cbo.struts.actions;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.tumri.af.struts.actions.sso.BaseAction;
import com.tumri.af.utils.DateUtils;
import com.tumri.af.utils.Utils;
import com.tumri.cbo.utils.CBOConfigurator;
import com.tumri.sso.ssoc.User;

/** Returns the current spreadsheet.
 * Unlike other actions this directly extends the Struts action
 * because the Flex file upload dialog does not send the correct authentication parameters
 * in the HTTP header.  That means there is no SSO User associated with this action.
 * The security filter should prevent this action from being called
 * if there 
 */
public class SetSpreadsheet extends BaseAction {

	private static final long serialVersionUID = -7774683033649571103L;

	private final static Logger log = Logger.getLogger(SetSpreadsheet.class);
	
	private final static String HISTORICAL_SPREADSHEET_FILE_PREFIX = "appnexus_";
	private final static String SPREADSHEET_FILE_NAME_SUFFIX = ".xls";

    private File file;
    private String contentType;
    private String filename;
    private String actionName;
    
	// ---------------------- Struts 2 File upload --------------------
	
    public void setSpreadsheet(File file) {
       this.file = file;
    }

    public File getSpreadsheet() {
        return file;
     }
    
    public void setSpreadsheetContentType(String contentType) {
       this.contentType = contentType;
    }
    
    public String getSpreadsheetContentType() {
        return contentType;
    }

    public void setSpreadsheetFileName(String filename) {
    	this.filename = filename;
    }

    public String getSpreadsheetFileName() {
    	return filename;
    }

    public void setSubmitAction(String actionName) {
    	this.actionName = actionName;
    }

    public String getSubmitAction() {
    	return actionName;
    }
    
	/** Uploads the spreadsheet.
	 * @param u The user.
	 * @param request The servlet request.
	 * @param response The servlet response.
	 * @return The struts result code to return from the execute() method.
	 * @throws Exception Any exception that is not handled.
	 */
	public String authenticatedExecute(User u, HttpServletRequest request,  HttpServletResponse response) throws Exception {
		response.resetBuffer();

		String result = ERROR;
		File newSpreadsheet = getSpreadsheet();
		String fileName = getSpreadsheetFileName();
		
		String errorMessage = validateSpreadsheet(newSpreadsheet, fileName, getSpreadsheetContentType());
		if(errorMessage != null) {
			setErrorMessage(errorMessage);
		} else {
			String msg = "Uploaded file: " + fileName + " (" + newSpreadsheet.length() + " bytes)";
			log.debug(msg);
			System.out.println(msg);
			
			File dir = new File(CBOConfigurator.getDataDirectoryPath());
			File f = new File(CBOConfigurator.getSpreadsheetFilePath());
			if(f.exists()) {                      
				String dateString = DateUtils.toCanonicalDateTimeString(new Date(f.lastModified()));
				dateString = dateString.replace(' ', '_');
				dateString = dateString.replace(':', '-');
				String historicalFileName = HISTORICAL_SPREADSHEET_FILE_PREFIX + dateString + Utils.getSuffix(f.getName());
				try {
					FileUtils.copyFile(f, new File(dir, historicalFileName));
				} catch(IOException ioe) {
					log.warn("Error saving old spreadsheet to " + historicalFileName);
				}
				getSpreadsheet().renameTo(f);
			}
			result = SUCCESS;
		}
		return result;
	}
	
	/** Validates that the file passed in is a valid bid configuration spreadsheet.
	 * @param f The file.
	 * @param name The file name.
	 * @param mimeType The mime type.
	 * @return The error message or null if everything is ok
	 */
	private String validateSpreadsheet(File f, String name, String mimeType) {
		String msg = null;
		if((name == null) || (!name.endsWith(SPREADSHEET_FILE_NAME_SUFFIX))) {
			msg = "Uploaded file (" + name + ") does not end with " + SPREADSHEET_FILE_NAME_SUFFIX;
		} 
		return msg;
	}
}


