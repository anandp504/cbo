package com.tumri.cbo.struts.actions;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.tumri.af.context.TimeScale;
import com.tumri.af.struts.actions.sso.AbstractUserXMLAction;
import com.tumri.af.utils.DateUtils;
import com.tumri.cbo.backend.Bidder;
import com.tumri.cbo.backend.PerformanceHistoryDAO;
import com.tumri.cbo.backend.PerformanceHistoryRow;

import com.tumri.sso.ssoc.User;


/** Gets a tab-separated values table containing the date/time bid price, bid type,
 * impression count, cost, average cpm, and other performance metrics as a function of
 * time on an hourly or daily basis for a single AppNexus campaign (line item) over a
 * specified time period.
 * <p>
 * The request takes the following parameters:
 * advertiserId:  AppNexus advertiser id.
 * campaignId:  AppNexus campaign id.
 * timeScale:  The time scale ("HOURLY", "DAILY" or another string representation of TimeScale.java
 * starts:  The start date in UTC in the form:  'YYYY-MM-DD' or 'YYYY-MM-DD HH:MM:SS'
 * ends:  The end date in UTC in the form:  'YYYY-MM-DD' or 'YYYY-MM-DD HH:MM:SS'
 * </p>
 */
public class GetCampaignPerformanceHistory extends AbstractUserXMLAction {

	private static final long serialVersionUID = -4974006234145677338L;
	
	private final static Logger log = Logger.getLogger(GetCampaignPerformanceHistory.class);
	
	private final static String MIME_TYPE_TSV = "text/tab-separated-values";
	private final static char SEPARATOR = '\t';
	private final static char END_OF_ROW = '\n';
	
	private final static String[] s_columnTitles = {"time", "bid", "impressions", "cpmPaid", "totalCost"};
	
	private final NumberFormat m_priceFormat;
	
	// Parameters of the request
	private int advertiserId;
	private int campaignId;
	private String m_tz;
    private TimeZone m_timeZone;
	private String m_starts;
	private String m_ends;
	private String m_timeScale;
	
	/** Constructor */
	public GetCampaignPerformanceHistory() {
		m_priceFormat = NumberFormat.getCurrencyInstance();
		m_priceFormat.setMaximumFractionDigits(6);
	}
	
	/** Sets the AppNexus advertiser id of the campaign whose data is to be obtained.
	 * @param advertiserId The AppNexus advertiser id of the campaign whose data is to be obtained.
	 */
	public void setAdvertiserId(int advertiserId) {
		this.advertiserId = advertiserId;
	}
	
	/** Gets the AppNexus advertiser id of the campaign whose data is to be obtained.
	 * @return The AppNexus advertiser id of the campaign whose data is to be obtained.
	 */
	public int getAdvertiserId() {
		return advertiserId;
	}

	/** Sets the AppNexus id of the campaign whose data is to be obtained.
	 * @param campaignId AppNexus id of the campaign whose data is to be obtained.
	 */
	public void setCampaignId(int campaignId) {
		this.campaignId = campaignId;
	}
	
	/** Sets the AppNexus id of the campaign whose data is to be obtained.
	 * @return campaignId The AppNexus id of the campaign whose data is to be obtained.
	 */
	public int getCampaignId() {
		return campaignId;
	}

	/** Sets the time zone string with respect to which to report.
	 * The time zone string must be of the form "GMT -08:00".
	 * The sign is required in the time zone even if the offset is "00:00".
     * @param tz The time zone string.
	 */
	public void setTZ(String tz) {
		m_tz = tz;
		m_timeZone = TimeZone.getTimeZone(tz);
	}

	public String getTZ() {
		return m_tz;
	}
	
    /** Gets the TimeZone with respect to which to report.
     * Returns the default time zone this is running in if not specified.
     * @return The time zone (never null).
	 */
	public TimeZone getTimeZone() {
		TimeZone tz = m_timeZone;
		if(tz == null) {
			tz = TimeZone.getDefault();
		}
		return tz;
	}

	/** Sets the time scale on which to report the data.
	 * The time scale as a string representation of one of the TimeScale enumeration values.
	 * @param timeScale The time scale on which to report the data.
	 */
	public void setTimeScale(String timeScale) {
		m_timeScale = timeScale;
	}
	
	/** Gets the string representation of the time scale on which to report the data.
	 * return The string representation of the time scale on which to report the data.
	 */
	public String getTimeScale() {
		return m_timeScale;
	}
	
	public void setStarts(String starts) {
		m_starts = starts;
	}
	
	public String getStarts() {
		return m_starts;
	}
	
	public void setEnds(String ends) {
		m_ends = ends;
	}
	
	public String getEnds() {
		return m_ends;
	}
	
	/** Overrides the superclass method to write the result in various formats.
	 * @param u The user.
	 * @param request The servlet request.
	 * @param response The servlet response.
	 * @return The struts result code to return from the execute() method.
	 * @throws Exception Any exception that is not handled.
	 */
	public String authenticatedExecute(User u, HttpServletRequest request,  HttpServletResponse response) throws Exception {
		
		String result = ERROR;
		
		Exception ex = null;
		List<PerformanceHistoryRow> rows = null;
		
		try {
			rows = getCampaignPerformanceHistory();
		} catch(Exception e)  {
			ex = e;
		}

		try {
			// Write result as TSV
			response.resetBuffer();
			response.setContentType(MIME_TYPE_TSV);
			
			Writer w = response.getWriter();
			if(ex != null) {
				writeUserError(w, ex);
			} else {
				writePerformanceHistoryRowsAsTSV(w, rows);
			}
			w.flush();
			result = SUCCESS;
		} catch(Exception e) {
			log.error("Exception downloading campaign performance: ", e);
		}
		return result;
	}

	/** Dummy implementation that does nothing.
	 * It is not called but needs to be implemented.
	 * @param u The user.
	 * @param r The reader that contains the contents of the post.
	 * @param w The writer to which to write the output.
	 * @exception Exception if error doing the action.
	 */
	public void execute(User u, Reader r, Writer w) throws Exception {
		// Not called by the authenticatedExecute method.
	}
	
	// ------------------------- Private methods ---------------------
	
	/** Gets the result as a list of performance history rows.
	 * Parses the input arguments.
	 * @exception IllegalArgumentException If the arguments cannot be parsed.
	 * @exception IllegalStateException If the bidder has not been initialized.
	 */
	private List<PerformanceHistoryRow> getCampaignPerformanceHistory() throws Exception { 

		TimeZone tz = getTimeZone();
		Date startDate = adjustForTimeZone(DateUtils.parseCanonicalDateString(getStarts()), tz);
		Date endDate = adjustForTimeZone(DateUtils.parseCanonicalDateString(getEnds()), tz);
		
		TimeScale ts = TimeScale.valueOf(getTimeScale());

		if((startDate == null) || (endDate == null)) {
			throw new IllegalArgumentException("The parameters 'starts' and 'ends' must be specified in the form: YYYY-MM-DD [HH:MM]");
		}

		Bidder b = Bidder.getInstance();
		if(b == null) {
			throw new IllegalStateException("Bidder has not been initialized.");
		}

		PerformanceHistoryDAO dao = b.getPerformanceHistoryDAO();
		return dao.getCampaignPerformanceHistory(getAdvertiserId(), getCampaignId(), 
											     ts, startDate, endDate, tz);
	}
	
	/** Writes all performance history as a TSV table with header row(s).
	 * @param w The writer (assumed not null).
	 * @param rows The performance history rows to write.
	 */
	private void writePerformanceHistoryRowsAsTSV(Writer w, List<PerformanceHistoryRow> rows) throws IOException {
		writePerformanceHistoryHeaderTSV(w);
		if(rows != null) {
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm z");
			df.setTimeZone(getTimeZone());
			for(PerformanceHistoryRow row : rows) {
				writePerformanceHistoryRowTSV(w, row, df);
			}
		}
	}
	
	/** Writes the header row(s) for the performance history report in TSV format
	 * @param w The writer (assumed not null).
	 * @exception IOException If error writing the header row.
	 */
	private void writePerformanceHistoryHeaderTSV(Writer w) throws IOException {
		boolean firstCol = true;
		for(String colTitle : s_columnTitles) {
			if(!firstCol) {
				writeSeparator(w);
			}
			w.write(colTitle);
			firstCol = false;
		}
		writeEndOfRow(w);
	}
	
	/** Writes a performance history row as a tab-separated values row.
	 * Does nothing if the row is null.
	 * @param w The writer (assumed not null).
	 * @param row The row to write.
	 * @param df The date format to use.
	 * @exception IOException If error writing the row.
	 */
	private void writePerformanceHistoryRowTSV(Writer w, PerformanceHistoryRow row, DateFormat df) throws IOException {
		if(row != null) {
			w.write(df.format(row.getTime()));
			writeSeparator(w);
			Double bid = null;
			if(row.isFixedBidPrice()) {
				bid = row.getBidPrice();
			}
			if(bid != null) {
				writePrice(w, bid.doubleValue());
			}
			writeSeparator(w);
			w.write(String.valueOf(row.getImpressionsServed()));
			writeSeparator(w);
			writePrice(w, row.getAverageCPMPaid());
			writeSeparator(w);
			writePrice(w, row.getTotalCost());
			writeEndOfRow(w);
		}
	}
	
	private void writePrice(Writer w, double d) throws IOException {
		w.write(getPriceFormat().format(d));
	}

	private void writeSeparator(Writer w) throws IOException {
		w.write(SEPARATOR);
	}
	
	private void writeEndOfRow(Writer w) throws IOException {
		w.write(END_OF_ROW);
	}
	
	
	/** Writes a user error message to the writer.
	 * @param w The writer.
	 * @param e The exception whose error is to be written.
	 */
	private void writeUserError(Writer w, Exception e) throws IOException {
		w.write(e.getMessage());
	}
	
	private NumberFormat getPriceFormat() {
		return m_priceFormat;
	}
	
	/** Adjusts GMT dates for the specified time zone.
	 * @param d The date.
	 * @param tz The time zone.
	 * @return The date adjusted for the time zone.
	 */
	private Date adjustForTimeZone(Date d, TimeZone tz) {
		long t = d.getTime();
		t += TimeZone.getDefault().getOffset(t);  // Converts to start of the day GMT.
		t -= tz.getOffset(t);  // To start of the day in the desired time zone.
		return new Date(t);
	}
}


