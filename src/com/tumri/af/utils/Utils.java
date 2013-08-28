package com.tumri.af.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Utils {
           
    public static final String EMPTY_STRING = "";
    
    public static final String FILE_FORMAT_XLS = ".xls";   
    public static final String FILE_FORMAT_ZIP = ".zip";
    
    public static final String HYPHEN_SYMBOl = "-";  
    public static final String FORWARD_SLASH = "/"; 
    public static final String SPACE = " ";
   
    /** 
     * Is the given string non-empty?
     * @param str
     * @return True if the string is not empty.
     */
    public static boolean isNonEmpty( String str ) {
        return !isEmpty(str);
    }

    /** Determines if the string is null or blank.
     * Trims spaces.
     * @param str The string.
     * @return True if the string is blank or false if not.
     */
    public static boolean isEmpty(String str) {
    	return (str == null) || str.trim().equals(EMPTY_STRING);
    }
    
	/** Returns the string unless it is null in which case a blank is returned.
	 * @param s The string.
	 * @return The string passed in or "" if the string is null.
	 */
	public static String nullToBlank(String s) {
		if(s == null) {
			return EMPTY_STRING;
		}
		return s;
	}

	/** Gets the portion of the string that starts with the last dot.
	 * @param s The string.
	 * @return The suffix of the string or a blank string if s is null or if the suffix is not found.
	 */
	public static String getSuffix(String s) {
		return getSuffix(s, ".");
	}
	
	/** Gets the portion of the string that starts with the last occurrence of the specified string.
	 * @param s The string.
	 * @param suffixStart The string that starts the suffix.
	 * @return The suffix of the string or a blank string if s is null or if the suffix start is not found.
	 */
	public static String getSuffix(String s, String suffixStart) {
		String result = EMPTY_STRING;
		if((s != null) && (suffixStart != null)) {
			int lastIndex = s.lastIndexOf(suffixStart);
			if(lastIndex >= 0) {
				result = s.substring(lastIndex);
			}
		}
		return result;
	}
	
    /**
     * This method takes the exception thrown and returns the stack trace as a String
     * @param e The exception thrown
     * @return exception as String
     */
    public static String getStackTrace(Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter writer = new PrintWriter(sw);
        e.printStackTrace(writer);
        String expTrace = sw.toString();
        return expTrace;
    }
    
    /** Removes the last instance of the separator string and everything after it.
     * Does nothing if the separator string is not in the string or if the string is null.
     * @param str The string from which to remove the end.
     * @param seperator The separator.
     * @return The string minus everything after the last occurrance of the separator string.
     */
    public static String removeTrailingSeperator(String str, String seperator) {
        if(isEmpty(str))
            return str;
        int idx = str.lastIndexOf(seperator);
        if(idx != -1) {
            String tmpStr = str.substring(0, idx);
            return tmpStr;
        }
        return str;     
    }

    /** Determines if two objects are equal.
     * Considers null objects to be equal.
     * @param o1 The first object.
     * @param o2 The second object.
     * @return True if the objects are equal or false if not.
     */
    public static boolean equals(Object o1, Object o2) {
    	return (o1 == o2) || ((o1 != null) && (o1.equals(o2)));
    }
    
    /** Hashes the hash code of an object with the hash code passed in.
     * @param h The hash code passed in.
     * @param obj The object whose hash code is to be added.
     * @return The hash of the two hash codes or the original hash code if the object is null.
     */
    public static int addToHash(int h, Object obj) {
    	if(obj != null) {
    		h = addHashCodes(h, obj.hashCode());
    	}
    	return h;
    }
    
    /** "Adds" two hash codes in a non-symmetric manner to
     * produce a new hash code.
     * @param h1 The first hash code.
     * @param h2 The second hash code.
     * @return The new hash code
     */
    public static int addHashCodes(int h1, int h2) {
    	return ((h1 >> 13) | (h1 << 18)) ^ (h2 + 37);
    }
}

