package com.tumri.af.utils;

import java.util.Properties;


import com.tumri.af.utils.Utils;

/** Class that parses the version properties object to get the current application version.
 * This must be initialized with a properties object by the initialization servlet.
 */
public class VersionUtils {
	
	private final static String BUILD_VERSION_PROPERTY = "build_version";
	
	private static Properties m_versionProperties = null;
	
	/** Initializes this with the version properties.
	 * @param p The version properties.
	 */
	public static void init(Properties p) {
		m_versionProperties = p;
	}

	/** Releases resources before exiting the JVM.
	 */
	public static void shutdown() {
	}
	
	/** Gets the application version as a string.
	 * @return The application version as a string.
	 */
	public static String getAppVersion() {
		String version = null;
		if(m_versionProperties != null) {
			version = m_versionProperties.getProperty(BUILD_VERSION_PROPERTY);
		}
		if(version == null) {
			version = Utils.EMPTY_STRING;
		}
		return version;
	}
}
