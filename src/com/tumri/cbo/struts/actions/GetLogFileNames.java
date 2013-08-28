package com.tumri.cbo.struts.actions;

import java.io.File;
import java.io.FilenameFilter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.thoughtworks.xstream.XStream;
import com.tumri.af.struts.actions.sso.AbstractUserXMLAction;
import com.tumri.af.utils.XMLUtils;
import com.tumri.cbo.utils.CBOConfigurator;
import com.tumri.sso.ssoc.User;


/** Gets a list of the files in the log file directory whose name contains ".log"
 * The result is returned in an XML response.
 */
public class GetLogFileNames extends AbstractUserXMLAction implements FilenameFilter {

	private static final long serialVersionUID = -7772809689670278766L;

	private final static String LOG_FILE_SUFFIX = ".log";
	
	private final static String FILENAMES_TAG = "fileNames";
	private final static String FILENAME_TAG = "fileName";
	
	
	/** Writes a list of the names of the log files in the log file directory
	 * to the writer in XML in the format:  
	 * <fileNames>
	 *   <fileName>File 1</fileName>
	 *   <fileName>File 2</fileName>
	 *   ...
	 * </fileNames>
	 * @param u The user.
	 * @param r The reader that contains the contents of the post.
	 * @param w The writer to which to write the output.
	 * @exception Exception if error doing the action.
	 */
	public void execute(User u, Reader r, Writer w) throws Exception {
		File logDir = new File(CBOConfigurator.getLogDirectoryPath());
		List<String> names = new ArrayList<String>();
		String[] fileNames = logDir.list(this);
		if(fileNames != null) {
			for(String fileName: fileNames) {
				names.add(fileName);
			}
		}
		Collections.sort(names);
		XStream xstream = new XStream();
		xstream.alias(FILENAMES_TAG, List.class);
		xstream.alias(FILENAME_TAG, String.class);
		w.write(XMLUtils.XML_START_UTF8);
		w.write(xstream.toXML(names));
	}
	
	/** This is exposed as a side effect.
	 * @param dir The directory.
	 * @param name The file name.
	 * @return True if the name is a log file name.
	 */
	public boolean accept(File dir, String name) {
		return (name != null) && name.contains(LOG_FILE_SUFFIX);
	}
}


