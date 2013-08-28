package com.tumri.af.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Date and calendar manipulation utilities.
 */
public class DateUtils {

	/** The canonical internal date format used to pass data as strings. */
	public static final String CANONICAL_DATE_FORMAT_STRING = "yyyy-MM-dd";
	
	/** The canonical internal date-time format used to pass data as strings. */
	public final static String CANONICAL_DATE_TIME_FORMAT_STRING = "yyyy-MM-dd HH:mm:ss";

	/** One second in milliseconds. */
    public final static long ONE_SECOND_MS = 1000L;
    
	/** One minute in milliseconds. */
    public final static long ONE_MINUTE_MS = 60*ONE_SECOND_MS;

	/** One hour in milliseconds. */
    public final static long ONE_HOUR_MS = 60*ONE_MINUTE_MS;
    
    /** One day (24 hours) in milliseconds. */
    public final static long ONE_DAY_MS = 24*ONE_HOUR_MS;
    
    // All calls to format() on the following instances must be synchronized.
	private final static SimpleDateFormat s_canonicalDateFormat =  
		                                  new SimpleDateFormat(CANONICAL_DATE_FORMAT_STRING);
	private final static SimpleDateFormat s_canonicalDateTimeFormat =  
                                          new SimpleDateFormat(CANONICAL_DATE_TIME_FORMAT_STRING);
	
    /** Sets the calendar passed in to the start of the day that 
     * is represented by the calendar.
     * This method is purely a side effect.  
     * It modifies the calendar passed in and returns it.
     * @param cal The calendar to update (assumed not null).
     * @return The calendar passed in modified to the start of the day.
     */
    public static Calendar setToStartOfDay(Calendar c) {
    	return setToStartOfHour(c, 0);
    }
    
    /** Sets the calendar passed in to the start of a specified hour
     * in the day currently represented by the calendar 
     * by setting the hour number and clearing the minutes and seconds.
     * This method is purely a side effect.
     * It modifies the calendar passed in and returns it.
     * @param cal The calendar to update (assumed not null).
     * @param hr The zero-based hour of the day.
     * @return The calendar passed in modified to the start of the specified hour.
     */
    public static Calendar setToStartOfHour(Calendar c, int hr) {
    	c.set(Calendar.HOUR_OF_DAY, hr);
    	return setToStartOfHour(c);
    }
    
    /** Sets the calendar passed in to the start of the current hour
     * in the day represented by the calendar passed in.
     * This just clears the minutes, seconds and milliseconds.
     * This method is purely a side effect.
     * It modifies the calendar passed in and returns it.
     * @param cal The calendar to update (assumed not null).
     * @param hr The zero-based hour of the day.
     * @return The calendar passed in modified to the start of the day.
     */
    public static Calendar setToStartOfHour(Calendar c) {
    	c.set(Calendar.MINUTE, 0);
    	c.set(Calendar.SECOND, 0);
    	c.set(Calendar.MILLISECOND, 0);
    	return c;
    }
    
    /** Sets the calendar passed in to the end of the day
     * represented by the calendar that is passed in.
     * It sets the hour to the last hour of the day
     * and then sets the minutes, seconds, and milliseconds
     * to the end of the hour.
     * This method is purely a side effect.  
     * It modifies the calendar passed in and returns it.
     * The end of the day is the last mS of the day.
     * @param cal The calendar to update (assumed not null).
     * @return The calendar passed in modified to the end of the day.
     */
    public static Calendar setToEndOfDay(Calendar c) {
    	return setToEndOfHour(c, 23);
    }
    
    /** Sets the calendar passed in to the end of a specified hour
     * in the day currently represented by the calendar 
     * by setting the hour to the hour passed in,
     * and then by setting the minutes, seconds and milliseconds to the
     * last millisecond in the hour.
     * This method is purely a side effect.
     * It modifies the calendar passed in and returns it.
     * @param cal The calendar to update (assumed not null).
     * @param hr The zero-based hour of the day.
     * @return The calendar passed in modified to the end of the specified hour.
     */
    public static Calendar setToEndOfHour(Calendar c, int hr) {
    	c.set(Calendar.HOUR_OF_DAY, hr);
    	return setToEndOfHour(c);
    }
  
    /** Sets the calendar passed in to the end of the hour
     * in the day currently represented by the calendar 
     * by setting the minutes, seconds and milliseconds to the
     * last millisecond in the hour.
     * This method is purely a side effect.
     * It modifies the calendar passed in and returns it.
     * @param cal The calendar to update (assumed not null).
     * @param hr The zero-based hour of the day.
     * @return The calendar passed in modified to the end of the specified hour.
     */
    public static Calendar setToEndOfHour(Calendar c) {
    	c.set(Calendar.MINUTE, 59);
    	c.set(Calendar.SECOND, 59);
    	c.set(Calendar.MILLISECOND, 999);
    	return c;
    }
    
    /** Gets a copy of the calendar passed in.
     * @param c The calendar.
     * @return A copy of the calendar or null if the calendar passed in is null.
     */
    public static Calendar copyCalendar(Calendar c) {
    	if(c != null) {
    		c = (Calendar)(c.clone());
    	}
    	return c;
    }
    
    /** Gets a string representation of the date in a calendar.
     * Includes the time zone at the end.
     * @param c The calendar.
     * @return The string representation of the date in the calendar.
     */
    public static String toDateString(Calendar c) {
    	return toString(c, DateFormat.getDateInstance());
    }
    
    /** Gets a string representation of the date and time in a calendar.
     * Includes the time zone at the end.
     * @param c The calendar.
     * @return The string representation of the date in the calendar.
     */
    public static String toDateTimeString(Calendar c) {
    	return toString(c, DateFormat.getDateTimeInstance());
    }
    
    /** Gets a string representation of the date in canonical format
     * using the time zone this code is running in.
     * @param d The date.
     * @return The canonical string representation of the date or a blank string if the date is null.
     */
    public static String toCanonicalDateString(Date d) {
    	String s = Utils.EMPTY_STRING;
    	if(d != null) {
    		s = doSynchronizedFormat(d, s_canonicalDateFormat);
    	}
    	return s;
    }
    
    /** Gets a string representation of the date and time in canonical format
     * using the time zone this code is running in.
     * @param d The date.
     * @return The canonical string representation of the date-time or a blank string if the date is null.
     */
    public static String toCanonicalDateTimeString(Date d) {
    	String s = Utils.EMPTY_STRING;
    	if(d != null) {
    		s = doSynchronizedFormat(d, s_canonicalDateTimeFormat);
    	}
    	return s;
    }
    
	/** Converts a string that represents a date/time in the format
	 * YYYY-MM-DD HH:MM:SS or just YYYY-MM-DD into an actual date.
	 * @param s The canonical date string.
	 * @return d The date.
	 * @throws ParseException If error parsing the string.
	 */
	public static Date parseCanonicalDateString(String s) throws ParseException {
		Date d = null;
		if (s != null) {
			int len = s.length();
			SimpleDateFormat df = null;
			if (len <= 10) {
				df = new SimpleDateFormat(CANONICAL_DATE_FORMAT_STRING);
			} else {
				df = new SimpleDateFormat(CANONICAL_DATE_TIME_FORMAT_STRING);
			}
			d = df.parse(s);
		}
		return d;
	}
    
    // ------------------------------- Private methods -----------------------
    
	/** Converts a date in a calendar to a string including the time zone
	 * using the date format passed in.
	 * Returns a blank string if the calendar is null.
	 * @param c The calendar.
	 * @param df The date format (assumed not null).
	 * @return a string representation of the date with the time zone on the end.
	 */
    private static String toString(Calendar c, DateFormat df) {
    	String s = Utils.EMPTY_STRING;
    	if(c != null) {
    		df.setCalendar(c);
    		TimeZone tz = c.getTimeZone();
    		boolean dst = tz.inDaylightTime(c.getTime());
    		s = df.format(c.getTime()) + " " + tz.getDisplayName(dst, TimeZone.SHORT);
    	}
    	return s;
    }
    
    /** Formats the specified date using the specified date format.
     * Synchronizes on the date format before calling format().
     * @param d The date (assumed not null).
     * @param df The date format (assumed not null).
     * @return The date formatted as a string with the formatter.
     */
    private static String doSynchronizedFormat(Date d, DateFormat df) {
    	String s = null;
    	synchronized(df) {
    		s= df.format(d);
    	}
    	return s;
    }
}
