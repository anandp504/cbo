package com.tumri.cbo.backend;

import java.util.Calendar;

import com.tumri.af.utils.DateUtils;

/** The superclass for classes that keep track of something over the period of
 * a single AppNexus day.
 */
public class DailyData {

	private Calendar m_startOfDay;
	private int m_numberOfHoursInDay = -1;
	
	// A constant used to round the number of mS to ensure that any
	// leap seconds will have no effect on the computation of the 
	// number of hours in a day.
	private final static double ROUNDING = DateUtils.ONE_MINUTE_MS; 
	
	/** Constructor that specifies the day.
	 * @param day The day (assumed not null).
	 */
	protected DailyData(Calendar day) {
		setDay(day);
	}
	
	/** Gets a new calendar set to the start of the day this data is for in the AppNexus time zone.
	 * @return A new calendar set to the start of the day in the AppNexus time zone.
	 */
	public Calendar getStartOfDay() {
		return DateUtils.copyCalendar(m_startOfDay);
	}
	
	/** Gets a new calendar set to the end of the day this data is for in the AppNexus time zone.
	 * @return A new calendar set to the end of the day in the AppNexus time zone.
	 */
	public Calendar getEndOfDay() {
		return DateUtils.setToEndOfDay(DateUtils.copyCalendar(m_startOfDay));
	}
	
	/** Gets the total number of hours in the AppNexus day.
	 * This is usually 24, but is 23 on the day that daylight savings time starts
	 * and 25 on the day that daylight savings time ends.
	 * @return The actual number of hours between the start and end of the day.
	 */
	public int getNumberOfHoursInDay() {
		return m_numberOfHoursInDay;
	}

    // ---------------------- Private methods --------------
    
	/** Sets the day this data is for.
	 * This method also updates the number of hours in the day.
	 * @param day a calendar set to the start of the day in AppNexus time.
	 */
	private void setDay(Calendar day) {
		Calendar start = null;
		int hrs = 0;
		if(day != null) {
			start = DateUtils.setToStartOfDay(DateUtils.copyCalendar(day));
			Calendar end = DateUtils.setToEndOfDay(DateUtils.copyCalendar(day));
			double numHours = (ROUNDING + end.getTimeInMillis() - start.getTimeInMillis()) / ((double)DateUtils.ONE_HOUR_MS);
			hrs = (int)(Math.floor(numHours));
		}
		m_startOfDay = start;
		m_numberOfHoursInDay = hrs;
	}
}
