package com.tumri.cbo.backend;

import java.util.Date;
import java.util.List;
import java.io.OutputStream;
import java.util.TimeZone;

import com.tumri.af.context.TimeScale;

public interface PerformanceHistoryDAO {
	
	/** Gets a table consisting of a list of rows that specify the date and/or time,
	 * the bid price and its ranges, and the resulting metrics.
 	 * One row is returned for each time period even if there is no data recorded for that time period.
 	 * @param advertiserId The advertiser id.
 	 * @param campaignId The AppNexus campaign id (line item id).
 	 * @param ts The time scale.
 	 * @param startDate The date-time of the first row to get in UTC.
 	 * @param endDate The date-time of the last row to get in UTC.
     * @param wrtTimeZone The TimeZone with respect to which to report.  Null means the local TimeZone.
     * @throws Exception - coz it may!
 	 * @return A list of the performance history rows within the specified time range.
	 */
	public List<PerformanceHistoryRow> getCampaignPerformanceHistory
            (long advertiserId, long campaignId,
		     TimeScale ts, Date startDate, Date endDate,
             TimeZone wrtTimeZone) throws Exception;

    public void dumpPerformanceHistoryToExcel
            (String savePath, List<PerformanceHistoryRow> curve);

    public void dumpPerformanceHistoryToExcel
            (OutputStream stream, List<PerformanceHistoryRow> curve);

    public void dumpPerformanceHistoryToExcel
            (List<PerformanceCurve> curve, OutputStream stream);

    public void dumpPerformanceHistoryToExcel
            (List<PerformanceCurve> curve, String savePath);
}
