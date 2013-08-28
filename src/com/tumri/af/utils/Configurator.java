package com.tumri.af.utils;

import java.text.MessageFormat;
import java.util.Properties;

/**
 * This class is the base class for all configuration classes.
 */
public class Configurator {
	
    protected static Properties properties;

    // Keys used across all applications.
    private static String KEY_APP_MODE = "app.mode";
    private static String DEFAULT_APP_MODE = "EXTERNAL";
    
    // Invalid property messages
    private static String MSG_INVALID_VALUE = "Invalid value '{1}' for property: {0}.";
    private static String MSG_INVALID_INT_PROPERTY = MSG_INVALID_VALUE + "The value must be an integer.";
    private static String MSG_INVALID_LONG_PROPERTY = MSG_INVALID_VALUE + "The value must be a long.";
    private static String MSG_INVALID_BOOLEAN_PROPERTY = MSG_INVALID_VALUE + "The value must be either 'true' or 'false'.";
    private static String MSG_INVALID_DOUBLE_PROPERTY = MSG_INVALID_VALUE + "The value must be a double.";

    /** Initializes this configurator by setting the properties object.
     * This should only be called when the configurator is initialized.
     * @param p The properties object to use.
     */
    public static void init(Properties p) {
    	properties = p;
    }  
    
    public static String getAppMode() {
    	return properties.getProperty(KEY_APP_MODE, DEFAULT_APP_MODE).trim();
    }
    
    public static boolean isDevelopment() {
        return !getAppMode().equalsIgnoreCase(DEFAULT_APP_MODE);
    }
    
    /** Gets a property as a boolean.
     * @param key The key.
     * @return The property as a boolean or false if the property is null or not a boolean value.
     */
    public static boolean getBooleanProperty(String key) {
    	return getBooleanProperty(key, false);
    }
    
    /** Gets a boolean property by the indicated key.
     * Defaults to the default value if the key is not found.
     * @param key The key.
     * @param dflt The default value.
     * @return The boolean value.
     */
    public static boolean getBooleanProperty(String key, boolean dflt) {
    	boolean result = dflt;
    	String val = getProperty(key);
    	if(val != null) {
    		result = Boolean.parseBoolean(val);
    	}
    	return result;
    }
    
    /** Gets a required boolean property by the indicated key.
     * The property must be either "true" or "false"
     * @param key The key.
     * @return The boolean value.
     * @exception PropertyNotFoundException If there is no property found.
     * @exception InvalidPropertyException If the property is not a boolean value.
     */
    public static boolean getRequiredBooleanProperty(String key) throws PropertyNotFoundException, InvalidPropertyException {
    	boolean result = false;
    	String val = getProperty(key);
    	if(Boolean.TRUE.toString().equals(val)) {
    		result = true;
    	} else if(!(Boolean.FALSE.toString().equals(val))) {
    		throw new InvalidPropertyException(MessageFormat.format(MSG_INVALID_BOOLEAN_PROPERTY, key, val), key);
    	}
    	return result;
    }
    
    /** Gets a property as an integer.
     * @param key The key.
     * @return The property as an integer or 0 if the property value is not found or not an integer.
     */
    public static int getIntProperty(String key) {
    	return getIntProperty(key, 0);
    }

    /** Gets a property as an integer.
     * @param key The key.
     * @param dflt The default value.
     * @return The property value or the default value if the property is not an integer.
     */
    public static int getIntProperty(String key, int dflt) {
    	int result = dflt;
    	try {
    		result = Integer.parseInt(getProperty(key));
    	} catch (NumberFormatException nfe) {	
    	}
    	return result;
    }

    /** Gets a required property as an integer.
     * @param key The key.
     * @return The property as an integer.
     * @exception PropertyNotFoundException If the property has not been defined.
     * @exception InvalidPropertyException If the property is not a boolean value.
     */
    public static int getRequiredIntProperty(String key) throws PropertyException {
    	int result = 0;
    	String val = getProperty(key);
    	if(Utils.isEmpty(val)) {
    		throw new PropertyNotFoundException(key);
    	}
    	try {
    		result = Integer.parseInt(val);
    	} catch (NumberFormatException nfe) {
    		throw new InvalidPropertyException(MessageFormat.format(MSG_INVALID_INT_PROPERTY, key, val), key);
    	}
    	return result;
    }
    
    /** Gets a property as a long.
     * @param key The key.
     * @return The property as a long or 0 if the property value is not found or not a long.
     */
    public static long getLongProperty(String key) {
    	return getLongProperty(key, 0L);
    }

    /** Gets a property as a long.
     * @param key The key.
     * @param dflt The default value.
     * @return The property value as a long or the default value if the property is not a long.
     */
    public static long getLongProperty(String key, long dflt) {
    	long result = dflt;
    	try {
    		result = Long.parseLong(getProperty(key));
    	} catch (NumberFormatException nfe) {	
    	}
    	return result;
    }
    
    /** Gets a required property as a long.
     * @param key The key.
     * @return The property as a long
     * @exception PropertyNotFoundException If the property has not been defined.
     * @exception InvalidPropertyException If the property is not a boolean value.
     */
    public static long getRequiredLongProperty(String key) throws PropertyException {
    	long result = 0L;
    	String val = getProperty(key);
    	if(Utils.isEmpty(val)) {
    		throw new PropertyNotFoundException(key);
    	}
    	try {
    		result = Long.parseLong(val);
    	} catch (NumberFormatException nfe) {
    		throw new InvalidPropertyException(MessageFormat.format(MSG_INVALID_LONG_PROPERTY, key, val), key);
    	}
    	return result;
    }

    /**
     * Gets the property value as a double
     * @param key The property key
     * @param defaultValue Default value
     * @return The property value as a double or the default value if the value is not a double
     */
    public static double getDoubleProperty(String key, double defaultValue) {
        double result = defaultValue;
        try {
            result = Double.parseDouble(key);
        } catch (NumberFormatException ex) {

        }
        return result;
    }


    /**
     * Gets the property as a double value
     * @param key The property key
     * @return The property as a double
     * @exception PropertyNotFoundException If the property has not been defined
     * @exception NumberFormatException If the value is not a double
     */
    public static double getDoubleProperty(String key) throws PropertyException {
        double result = 0d;
        String propertyValue = getProperty(key);
        if (Utils.isEmpty(propertyValue)) {
            throw new PropertyNotFoundException((key));
        }
        try {
            result = Double.parseDouble(propertyValue);
        } catch (NumberFormatException ex) {
            throw new InvalidPropertyException(MessageFormat.format(MSG_INVALID_DOUBLE_PROPERTY, key, propertyValue), key);
        }
        return result;
    }
    
    /** Gets the property by the indicated key.
     * The property must be defined in the configuration file 
     * and it must not be blank or a PropertyNotFoundException is thrown.
     * @param key The property name
     * @return The property value as a string.
     * @exception PropertyNotFoundException If not found.
     */
    public static String getRequiredProperty(String key) throws PropertyNotFoundException {
        String val = getProperty(key);
        if(Utils.isEmpty(val)) {
        	throw new PropertyNotFoundException(key);
        }
        return val;
    }
    
	/** Gets a string array property that is specified by a comma-separated
	 * list of strings.
	 * Spaces are trimmed on either end.
	 * Returns null if the property has no strings or does not exist.
	 * @param key The property key.
	 * @return An array of strings or null if none.
	 */
	public static String[] getStringArrayProperty(String key) {
		String[] result = null;
		String s = getProperty(key);
		if(s != null) {
			s.trim();
			if(s.length() > 0) {
				result = s.split(",");
			}
		}
		return result;
	}
	
    /** Gets the property by the indicated key.
     * @param key The property name
     * @return The property value as a string or null if none found.
     */
    public static String getProperty(String key) {
        return getProperty(key, null);
    }

    /** Gets the property by the indicated key.
     * Uses the default value if no property by the specified key is found.
     * @param key The property name
     * @param dflt The default property value.
     * @return The property value as a string or the default value if none found.
     */
    public static String getProperty(String key, String dflt) {
    	String val = dflt;
    	if(properties != null) {
    		String propVal = properties.getProperty(key);
    		if(propVal != null) { 
    			val = propVal.trim();
    		}
    	}
        return val;
    }
    
    /** This method must be called to free resources 
     * used by this static class.
     */
    public static void shutdown() {
    	properties = null;
    }
    

}
