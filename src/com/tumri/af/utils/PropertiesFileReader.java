package com.tumri.af.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


/** This class finds a properties file in the Tumri-specific configuration of
 * application directories and reads it.  It has a single method to load the properties file.
 * This is a singleton.  Use getInstance() to get the only instance.
 * @author jkucera
 */
public class PropertiesFileReader {
	
    private final static String SYSTEM_PROP_CATALINA_BASE = "catalina.base";
    private final static String CONFIG_DIRECTORY_NAME = "conf";
    
    private final static PropertiesFileReader s_instance = new PropertiesFileReader();
    
    /** Gets the only instance of this class.
     * @return The only instance of this class.
     */
    public static PropertiesFileReader getInstance() {
    	return s_instance;
    }
    
	/** Looks for the properties file in the "usual" places,
	 * reads it and returns a new properties object.
	 * First, if CATALINA_BASE is defined, it looks for CATALINA_BASE/conf/&lt;fileName&gt;
	 * If that is not found, or if CATALINA_BASE is not defined looks for the the file "/&lt;fileName&gt;" in the classpath.
	 * @param fileName The file name of the properties file (without path, but with .properties)
	 * @return A properties object or an empty properties object if no file found.
	 * @exception IOException If error reading.
	 */
	public Properties readPropertiesFile(String fileName) throws IOException {
		Properties p = new Properties();
    	InputStream stream = null;
    	try {
    		String tomcatConfFolder = (String)System.getProperties().get(SYSTEM_PROP_CATALINA_BASE);
    		
    		if(tomcatConfFolder != null) {
    			String fileSep = File.separator;
    			String propertiesFilePath = tomcatConfFolder + fileSep+ CONFIG_DIRECTORY_NAME + fileSep + fileName;
    			File f = new File(propertiesFilePath);
    			if(f.exists()) {
    				stream = new FileInputStream(f);
    			}
    		}
    		
    		//there is no catalina.base or the file was not found in the tomcat/conf directory. - init from class-path
    		if(stream == null) {
    			stream = getClass().getResourceAsStream("/" + fileName);
    		}
    		
    		p.load(stream);
        } finally {
        	FileUtils.close(stream);
        }
        return p;
	}
}
