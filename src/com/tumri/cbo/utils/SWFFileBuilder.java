/**
 * This class is the base class for all classes which deal with
 * configurators
 */
package com.tumri.cbo.utils;

import com.tumri.af.utils.Utils;
import com.tumri.af.utils.VersionUtils;


/** This class keeps track of an application-specific properties file
 * and provides methods to get properties from that file.
 */
public class SWFFileBuilder {

	private static final String DEFAULT_APP_NAME = "cbo";
	private static final String KEY_APP_NAME = "app.name";

	private static final String DEFAULT_FILE_NAME_VERSION_SEPERATOR = "_";
	private static final String KEY_FILE_NAME_VERSION_SEPERATOR = "file.name.version.seperator";

	private static final String DEFAULT_FILE_EXTENSION = "swf";
	private static final String KEY_FILE_EXTENSION = "file.extension";

    private final static String DOT_STRING = ".";

    
	private static String FULL_SWF_FILE_NAME = null;
	private static String SWF_FILE_NAME_WITHOUT_EXTENSION = null;

	public static String getSWFFileName() {
		if (FULL_SWF_FILE_NAME == null) {
			prepareSWFFileName();
		}
		return FULL_SWF_FILE_NAME;
	}

	public static String getSWFFileNameWithoutExtension() {
		if (SWF_FILE_NAME_WITHOUT_EXTENSION == null) {
			prepareSWFFileWithoutExtension();
		}
		return SWF_FILE_NAME_WITHOUT_EXTENSION;
	}

	// ------------------------ Private methods ------------------------

	private static void prepareSWFFileWithoutExtension() {
		StringBuilder fileNameBuilder = new StringBuilder();
		String appName = CBOConfigurator.getProperty(KEY_APP_NAME);
		if (Utils.isEmpty(appName)) {
			appName = DEFAULT_APP_NAME;
		}
		String fileNameSeperator = CBOConfigurator.getProperty(KEY_FILE_NAME_VERSION_SEPERATOR);
		if (Utils.isEmpty(fileNameSeperator)) {
			fileNameSeperator = DEFAULT_FILE_NAME_VERSION_SEPERATOR;
		}
		String version = VersionUtils.getAppVersion();
		fileNameBuilder.append(appName);
		fileNameBuilder.append(fileNameSeperator);
		fileNameBuilder.append(version);
		SWF_FILE_NAME_WITHOUT_EXTENSION = fileNameBuilder.toString();
	}

	private static void prepareSWFFileName() {
		StringBuilder appNameBuilder = new StringBuilder();
		prepareSWFFileWithoutExtension();
		String fileExtension = CBOConfigurator.getProperty(KEY_FILE_EXTENSION);
		if (Utils.isEmpty(fileExtension)) {
			fileExtension = DEFAULT_FILE_EXTENSION;
		}
		appNameBuilder.append(SWF_FILE_NAME_WITHOUT_EXTENSION);
		appNameBuilder.append(DOT_STRING);
		appNameBuilder.append(fileExtension);
		FULL_SWF_FILE_NAME = appNameBuilder.toString();
	}

}
