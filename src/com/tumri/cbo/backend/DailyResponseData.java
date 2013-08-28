package com.tumri.cbo.backend;

import java.util.Calendar;

import com.tumri.af.utils.DateUtils;

/** This class represents the response to a certain bid price
 * for a single AppNexus day.
 * It contains the just the relevant metrics observed.
 */
public class DailyResponseData extends DailyData {

	private int firstHourWithImpressions = -1;
	private int lastHourWithImpressions = -1;
	private int numberOfHoursWithImpressions = -1;
	private long totalImpressions = 0L;
	private HistoricalDataRow[] m_historyRows;
	
	// ----------------------------- Constructors -----------------------
	
	/** Constructor that takes a date, and set of history data rows.
	 * The number of history data rows must match the actual number of hours in the date passed in.
	 * @param day The day (assumed not null).
	 * @param history The historical data rows for this day in an array.
	 */
	DailyResponseData(Calendar day, HistoricalDataRow[] history) {
		super(day);
		setHistoryRows(history);
	}
	
	// ------------------------------- Public methods ---------------------

    /** Determines if all data for this day is complete.
     * The data is complete if all hours in the day have data 
	 * or if the first N hours have data and the total impression count
	 * is at least the daily impression cap.
	 * @param dailyImpressionCap The daily impression cap.
     */
	public boolean isComplete(long dailyImpressionCap) {
		int hoursInDay = getNumberOfHoursInDay();
		return (hoursInDay > 0) && 
		       ((getNumberOfHoursWithImpressions() == hoursInDay) || (getTotalImpressions() >= dailyImpressionCap));  
	}
	
	/** Gets the maximum average hourly CPM paid.
	 * @return The maximum average hourly CPM paid.
	 */
	public double getMaxHourlyCPMPaid() {
		double maxAveCPM = 0.0;
		double aveCPM = 0.0;
		int hoursInDay = getNumberOfHoursInDay();
		for(int hr = 0; hr < hoursInDay; hr++) {
			aveCPM = getAverageCPM(hr);
			if(aveCPM > maxAveCPM) {
				maxAveCPM = aveCPM;
			}
		}
		return maxAveCPM;
	}
	
	/** Gets the first hour of the day that has impressions.
	 * @return The zero-based first hour of the day with impressions or < 0 if no impressions.
	 */
	public int getFirstHourWithImpressions() {
		return firstHourWithImpressions;
	}

	/** Gets the first hour of the day that has impressions.
	 * @return The zero-based first hour of the day with impressions or < 0 if no impressions.
	 */
	public int getLastHourWithImpressions() {
		return lastHourWithImpressions;
	}
	
	/** Gets the number of hours in the day that have impressions.
	 * @return The number of hours in the day that have impressions.
	 */
	public int getNumberOfHoursWithImpressions() {
		return numberOfHoursWithImpressions;
	}

	public long getTotalImpressions() {
		return totalImpressions;
	}
	
	/** Gets the number of impressions in the specified hour.
	 * @param hr The zero-based hour index.
	 * @return The number of impressions in the hour.
	 */
	public long getImpressionCount(int hr) {
		long imps = 0L;
		HistoricalDataRow row = getHistoricalDataRow(hr);
		if(row != null) {
			imps = row.getImpressionCount();
		}
		return imps;
	}

	/** Gets the total cost for the impressions in the specified hour.
	 * @param hr The zero-based hour index.
	 * @return The total cost for the impressions in the specified hour.
	 */
	public double getCost(int hr) {
		double cost = 0.0;
		HistoricalDataRow row = getHistoricalDataRow(hr);
		if(row != null) {
			cost = row.getCost();
		}
		return cost;
	}
	
	/** Gets the average CPM for the specified hour.
	 * Returns 0 if there were no impressions for the hour.
	 * @param hr The zero-based hour index.
	 * @return The average CPM for the specified hour.
	 */
	public double getAverageCPM(int hr) {
		double aveCPM = 0.0;
		HistoricalDataRow row = getHistoricalDataRow(hr);
		if(row != null) {
			long imprCount = row.getImpressionCount();
			if(imprCount > 0L) {
				aveCPM = 1000.0*row.getCost()/((double)(imprCount));
			}
		}
		return aveCPM;
	}
	
	/** Gets a string representation of this for debugging.
	 * @return A string representation of this for debugging.
	 */
    public String toString() {
        return "DailyResponseData[day=" + DateUtils.toDateTimeString(getStartOfDay()) +
               ",hrsInDay=" + getNumberOfHoursInDay() + 
               ",firstHourWithImpr=" + getFirstHourWithImpressions() +
               ",lastHourWithImpr=" + getLastHourWithImpressions() +
               ",numberOfHrsWithImpr=" + getNumberOfHoursWithImpressions() + 
               ",totalImpr=" + getTotalImpressions() + "]";
    }
    
    // ---------------------- Private methods --------------
	
    /** Gets the history row for the specified hour of the day.
     * @param hr The zero-based hour index.
     * @return The history row at that hour or null if none.
     */
    private HistoricalDataRow getHistoricalDataRow(int hr) {
    	return m_historyRows[hr];
    }

    /** Sets the historical data rows by copying them.
     * Also computes the first, last and number of hours with impressions.
     * This must be called after the number of hours in the day is known by setting the day.
     * @param rows The historical data in an array by zero-based hour.
     */
    private void setHistoryRows(HistoricalDataRow[] rows) {
    	int hoursInDay = getNumberOfHoursInDay();
		HistoricalDataRow[] storedRows = new HistoricalDataRow[hoursInDay];
		if(rows != null) {
			int length = rows.length;
			int rowsToCopy = Math.min(length, hoursInDay);
			if(rowsToCopy > 0) {
				System.arraycopy(rows, 0, storedRows, 0, rowsToCopy);
			}
		}
		m_historyRows = storedRows;
		updateCachedValues(storedRows);
    }
    
    /** Computes the various cached metrics from the historical data rows.
     */
    private void updateCachedValues(HistoricalDataRow[] rows) {
    	int len = rows.length;
    	int firstImpressions = -1;
    	int lastImpressions = -1;
    	int hoursWithImpressions = 0;
    	long totalImpressions = 0L;
    	long rowImpressions = 0L;
    	HistoricalDataRow row;
    	for(int i = 0; i < len; i++) {
    		row = rows[i];
    		if(row != null) {
    			rowImpressions = row.getImpressionCount();
    			if(rowImpressions > 0L) {
    				++hoursWithImpressions;
    				totalImpressions += rowImpressions;
    				if(firstImpressions < 0) {
    					firstImpressions = i;
    				}
    				if(i > lastImpressions) {
    					lastImpressions = i;
    				}
    			}
    		}
    	}
    	setFirstHourWithImpressions(firstImpressions);
    	setLastHourWithImpressions(lastImpressions);
    	setNumberOfHoursWithImpressions(hoursWithImpressions);
    	setTotalImpressions(totalImpressions);
    }
    
	/** Sets the hour of the day that has the first impressions.
	 * @param hr The zero-based hour that first has impressions.
	 */
	private void setFirstHourWithImpressions(int hr) {
		this.firstHourWithImpressions = hr;
	}
	
	/** Sets the hour that has the last impressions.
	 * @param hr The last zero-based hour that has impressions.
	 */
	private void setLastHourWithImpressions(int hr) {
		this.lastHourWithImpressions = hr;
	}

	/** Sets the number of hours in the day that have impressions.
	 * @param n The number of hours in the day that have impressions.
	 */
	private void setNumberOfHoursWithImpressions(int n) {
		this.numberOfHoursWithImpressions = n;
	}

	/** Sets the total number of impressions in the day.
	 * @param totalImpressions The total number of impressions in the day.
	 */
	private void setTotalImpressions(long totalImpressions) {
		this.totalImpressions = totalImpressions;
	}
	
}
