package com.tumri.cbo.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.directory.DirContext;
import javax.naming.ldap.InitialLdapContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tumri.cbo.monitor.MonitorSchedulerJob;
import org.apache.log4j.Logger;

import com.tumri.af.utils.SQLUtils;
import com.tumri.af.utils.Utils;
import com.tumri.cbo.backend.Bidder;
import com.tumri.cbo.backend.BidderSchedulerJob;
import com.tumri.cbo.scheduler.DataCleanUpScheduler;
import com.tumri.cbo.utils.CBOConfigurator;
import com.tumri.cbo.utils.CBODatabaseConfig;

/** Checks the health of the application.
 */
public class HealthCheckServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static Logger log = Logger.getLogger(HealthCheckServlet.class);

	// LDAP related
	private static String INITIAL_CONTEXT_FACTORY = "com.sun.jndi.ldap.LdapCtxFactory";
	private static String SIMPLE_AUTHENTICATION = "simple";
	private static Hashtable<String, String> LDAP_ENV = null;
	
	// Static variable to store response on success
	private static final String RESPONSE_SUCCESS = "success";
	
	// Static variable to store response on failure
	private static final String RESPONSE_FAILURE = "failed";

	private static final long TOO_LONG_SINCE_BIDDER_RAN_MS = 3*3600*1000L;  // 3 hours.
		
	// --------------------------- Public methods --------------------------

	public void init() throws ServletException {
	}

	public void destroy() {
	}

	// --------------------------- Protected methods --------------------------

    /**
     * Checks the health of all connections required by this application.
     * @return Null if the health check is OK
     */
    public static boolean isHealthy()
    {
        boolean result = true;
        try {
            // Need to get the LDAP URL from the configurator.
            checkSSOHealth(CBOConfigurator.getLDAPServerUrl());

            // Test Quartz health.
            if(!BidderSchedulerJob.isSchedulerOK()) {
                throw new Exception("Quartz bidder scheduler is not running");
            }
            if(!MonitorSchedulerJob.isSchedulerOK()) {
                throw new Exception("Monitor scheduler is not running");
            }
            if(!DataCleanUpScheduler.isSchedulerOK()){
            	throw new Exception("Quartz data cleanup scheduler is not running");
            }

            // test DB Connectivity
            checkDatabaseHealth();

            // Test that the bidder has run recently
            checkLastTimeBidderRan();

            log.debug("health check passed");
        } catch (Throwable ex) {
            result = false;
            String expTrace = Utils.getStackTrace(ex);
            log.fatal(ex.getMessage(), ex);
            log.fatal(expTrace);
            log.fatal("health check failed");
        }
        return result;
    }

	/**
	 * Checks the health of all connections required by this application.
	 * @param request  The servlet request object
	 * @param response The servlet response object
	 */
	protected void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		setResponseHeaders(response);
		response.setContentType("text/plain");
        String responseText = RESPONSE_SUCCESS;
        boolean isHealthy = isHealthy();
        if(!isHealthy) responseText = RESPONSE_FAILURE;
		//now write the response
		PrintWriter writer = response.getWriter();
		writer.write(responseText);
		response.flushBuffer();
	}
	
	/**
	 * Checks the health of any servers needed by SSO.
	 * This should be done in SSO.
	 * @param ldapURL The LDAP URL.
	 * @return Returns a true if succeeds else, returns a false.
	 */
	protected static boolean checkSSOHealth(String ldapURL) {
		boolean result = false;
		try {
			synchronized(HealthCheckServlet.class) {
				if(LDAP_ENV == null) {
					LDAP_ENV = new Hashtable<String, String>();
					LDAP_ENV.put(Context.INITIAL_CONTEXT_FACTORY, INITIAL_CONTEXT_FACTORY);
					LDAP_ENV.put(Context.PROVIDER_URL, ldapURL);
					LDAP_ENV.put(Context.SECURITY_AUTHENTICATION, SIMPLE_AUTHENTICATION);		
				}
			}
			
			DirContext ctx = new InitialLdapContext(LDAP_ENV, null);
			if (ctx == null) {
				log.error("Error connecting to ldap...");
				result = false;
			} else {
				log.info("Success...");
				result = true;
			}
			
		} catch (Exception e) {
    		String msg = "Exception encountered while connecting to LDAP server: " + LDAP_ENV.get(Context.PROVIDER_URL);
    		log.error(msg);
            log.error(Utils.getStackTrace(e));
            result = false;
		}
		return result;
	}
	
	// --------------------------- Private methods --------------------------

	/** Checks that the last time the bidder ran recently.
	 * If not this throws an exception.
	 * @Exception Exception If bidder has not run recently.
	 */
	private static void checkLastTimeBidderRan() throws Exception {
		long now = System.currentTimeMillis();
		Date d = Bidder.getInstance().getLastRunTime();
		if(d != null) {
			long lastRunTime = d.getTime();
			if((now - lastRunTime) > TOO_LONG_SINCE_BIDDER_RAN_MS) {
				throw new Exception("The bidder has not run for " + String.valueOf(((now - lastRunTime)/60000L)) + " minutes.");
			}
		}
	}
	
	/**
	 * Checks the health of the Database connection.
	 * @return True if the database is ok.
	 * @throws Exception If error.
	 */
	private static boolean checkDatabaseHealth() throws Exception {
		CBODatabaseConfig dbc = CBOConfigurator.getDatabaseConfig();
		return SQLUtils.checkDatabaseHealth(dbc.getDriver(), dbc.getUrl(), dbc.getUsername(), dbc.getPassword());
	}
	
	/**
	 * Set the appropriate headers in the Http response so that whoever asked
	 * for this Health check will not cache it.  This ensures that subsequent
	 * health checks really do come do this server.
	 *
	 * @param response
	 */
	private void setResponseHeaders(HttpServletResponse response) {
		response.setHeader("Cache-Control", "no-cache, no-store");
	}
}
