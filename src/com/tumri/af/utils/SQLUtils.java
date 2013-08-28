package com.tumri.af.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

public class SQLUtils {
    
    private static Logger log = Logger.getLogger(SQLUtils.class);
    
	private final static String SQL_TEST_DB = "Select 1 from DUAL";
    
    /** Closes the prepared statement passed in if it is not null, logging any exceptions.
     * @param ps The result set to close.
     */
    public static void close(PreparedStatement ps) {
    	if(ps != null) {
    		try {
    			ps.close();
    		} catch(SQLException sqe) {
    			log.error("Error closing database connection", sqe);
    		}
    	}
    }
    
    /** Closes the statement passed in if it is not null, logging any exceptions.
     * @param s The result set to close.
     */
    public static void close(Statement s) {
    	if(s != null) {
    		try {
    			s.close();
    		} catch(SQLException sqe) {
    			log.error("Error closing database SQL statement", sqe);
    		}
    	}
    }
    /** Closes the result set passed in if it is not null, logging any exceptions.
     * @param rs The result set to close.
     */
    public static void close(ResultSet rs) {
    	if(rs != null) {
    		try {
    			rs.close();
    		} catch(SQLException sqe) {
    			log.error("Error closing database connection", sqe);
    		}
    	}
    }

    /** Closes the connection passed in if it is not null, logging any exceptions.
     * @param conn The connection to close.
     */
    public static void close(Connection conn) {
    	if(conn != null) {
    		try {
    			conn.close();
    		} catch(SQLException sqe) {
    			log.error("Error closing database connection", sqe);
    		}
    	}
    }
    
	/** Checks the health of a database JDBC connection.
	 * @param driver The name of the driver class.
	 * @param url The database URL.
	 * @param username The user name.
	 * @param password The password.
	 * @return True on success.
	 * @throws Exception If error.
	 */
	public static boolean checkDatabaseHealth(String driver, String url, String username, String password) throws Exception {
		// Test DB connection
		boolean result = false;
		Connection conn = null;
		Statement st = null;
		try {
			log.info("Testing database connectivity.  Driver = " + driver + ", url = " + url
                     // Enable this if we're really desperate.
                     // Otherwise it's a bit of a security hole.
					// + ", user = " + username + ", password = " + password
                    );
			Class.forName(driver).newInstance();
			conn = DriverManager.getConnection(url, username, password);
			st = conn.createStatement();
			st.executeQuery(SQL_TEST_DB);
			result = true;
			log.info("Successfully connected to the database");
		} catch(Exception e) {
			log.error("Exception checking database health", e);
			throw e;
		} finally {
			SQLUtils.close(st);
			SQLUtils.close(conn);
		}
		return result;
	}

}

