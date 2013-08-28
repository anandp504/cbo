package com.tumri.cbo.test;

import org.apache.commons.dbcp.BasicDataSource;

import com.tumri.af.utils.SQLUtils;

import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;


import javax.sql.DataSource;

import junit.framework.TestCase;

/** Tests the time zone behavior of time stamp columns.
 */
public class JDBCTimeZoneTest extends TestCase {
	
	private DataSource m_dataSource;
	
	private final static String DEFAULT_DRIVER_CLASS_NAME = "com.mysql.jdbc.Driver";
	/*
	private final static String DEFAULT_URL = "jdbc:mysql://localhost:3306/cbo_db";
	private final static String DEFAULT_USERNAME = "root";
	private final static String DEFAULT_PASSWORD = "root";
	*/

	private final static String DEFAULT_URL = "jdbc:mysql://eit-server01.dev.tumri.net:3306/cbo_db";
	private final static String DEFAULT_USERNAME = "CBOUSER";
	private final static String DEFAULT_PASSWORD = "w3lc0m31";
	
	private final static String SQL_SELECT_HOURLY_IMPRESSIONS = 
		"select hour, imps, 1000*cost/imps from historicaldata where campaign_id = ? and hour >= ? and hour < ? order by 1";

	private final static String SQL_SELECT_DAILY_IMPRESSIONS = 
		"select date(convert_tz(hour, 'SYSTEM', 'America/New_York')), sum(imps), 1000*sum(cost)/sum(imps) from historicaldata where campaign_id = ? and hour >= ? and hour < ? group by 1 order by 1";
	
	public JDBCTimeZoneTest(String name) {
		super(name);
	}
	
	// JUnit
	public void setUp() {
		initialize(DEFAULT_DRIVER_CLASS_NAME, DEFAULT_URL, DEFAULT_USERNAME, DEFAULT_PASSWORD);
	}
	
	public void initialize(String driverClassName, String dbURL, String dbUsername, String dbPassword) {
		BasicDataSource ds = new BasicDataSource();
	    ds.setUrl(dbURL);
	    ds.setDriverClassName(driverClassName);
	    ds.setUsername(dbUsername);
	    ds.setPassword(dbPassword);   
	    m_dataSource = ds;
	}
	
	public static void main(String[] args) {
		String driverClassName = DEFAULT_DRIVER_CLASS_NAME;
		String dbURL = DEFAULT_URL;
		String user = DEFAULT_USERNAME;
		String pwd = DEFAULT_PASSWORD;
		
		// TODO:  Read db args from command line if needed.
		
		JDBCTimeZoneTest t = new JDBCTimeZoneTest("Command line JDBCTimeZoneTest");
		t.initialize(driverClassName, dbURL, user, pwd);
	}
	
	public void testDSTChange() throws SQLException {
		Connection conn = null;
		
		try {
			conn = getConnection();
			System.out.println("Testing DST change using connection " + conn);

			int campaignId = 279499;
			long startTime = 1331355600000L;     // 00:00 EST March 10th, 2012
			long endTime = 1331697600000L;       // 00:00 EDT March 14th, 2012
			
			System.out.println("Start = " + new Date(startTime));
			System.out.println("End = " + new Date(endTime));
			System.out.println("");
			
			List<ImpressionsVsTime> hourly = getHourlyImpressions(conn, campaignId, startTime, endTime);
			printImpressionsVsTime(hourly);
			System.out.println();
			List<ImpressionsVsTime> daily = getDailyImpressions(conn, campaignId, startTime, endTime);
			printImpressionsVsTime(daily);
			
		} finally {
			SQLUtils.close(conn);
		}
	}
	
	private void printImpressionsVsTime(List<ImpressionsVsTime> values) {
		for(ImpressionsVsTime ivt : values) {
			System.out.println(ivt);
		}
	}
	
	private List<ImpressionsVsTime> getHourlyImpressions(Connection conn, int campaignId, long startTime, long endTime) throws SQLException {
		
		List<ImpressionsVsTime> result = new ArrayList<ImpressionsVsTime>();
		
		PreparedStatement ps = null;
		ResultSet rs = null;
		Statement stmt = null;
		
		// Must pass these in as time stamp objects to get the right date.
		Timestamp start = new Timestamp(startTime);
		Timestamp end = new Timestamp(endTime);
		System.out.println("Start = " + start + ", end = " + end);
		
		try {
			ps = conn.prepareStatement(SQL_SELECT_HOURLY_IMPRESSIONS);
			ps.setInt(1, campaignId);
			ps.setTimestamp(2, start);
			ps.setTimestamp(3, end);
			
			rs = ps.executeQuery();
			while(rs.next()) {
				Timestamp ts = rs.getTimestamp(1);
				long imps = rs.getLong(2);	
				result.add(new ImpressionsVsTime(ts.getTime(), imps));
			}
		} finally {
			SQLUtils.close(rs);
			SQLUtils.close(ps);
			SQLUtils.close(stmt);
		}
		return result;
	}

	private List<ImpressionsVsTime> getDailyImpressions(Connection conn, int campaignId, long startTime, long endTime) throws SQLException {
		
		List<ImpressionsVsTime> result = new ArrayList<ImpressionsVsTime>();
		
		PreparedStatement ps = null;
		ResultSet rs = null;
		Statement stmt = null;
		
		// Must pass these in as time stamp objects to get the right date.
		Timestamp start = new Timestamp(startTime);
		Timestamp end = new Timestamp(endTime);
		System.out.println("Start = " + start + ", end = " + end);
		
		try {
			ps = conn.prepareStatement(SQL_SELECT_DAILY_IMPRESSIONS);
			ps.setInt(1, campaignId);
			ps.setTimestamp(2, start);
			ps.setTimestamp(3, end);
			
			rs = ps.executeQuery();
			while(rs.next()) {
				
				// NOTE: Group by date(convert_tz(hour)) internally to MySQL			
				String dateString = rs.getString(1);
				System.out.println("Date string = " + dateString);
				Date d = rs.getDate(1, Calendar.getInstance(TimeZone.getTimeZone("America/New_York")));
				long imps = rs.getLong(2);
				result.add(new ImpressionsVsTime(d.getTime(), imps));
			}
		} finally {
			SQLUtils.close(rs);
			SQLUtils.close(ps);
			SQLUtils.close(stmt);
		}
		return result;
	} 
	private Connection getConnection() throws SQLException {
		return m_dataSource.getConnection();
	}
    
	private class ImpressionsVsTime {
		long t;
		long imps;
		
		public ImpressionsVsTime(long t, long imps) {
			this.t = t;
			this.imps = imps;
		}
		
		public String toString() {
			return new Date(t) + "\t" + t + "\t" + imps;
		}
	}
}
