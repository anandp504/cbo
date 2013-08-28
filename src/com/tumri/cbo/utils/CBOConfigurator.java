/**
 * This class is the base class for all classes which deal with
 * configurators
 */
package com.tumri.cbo.utils;

import java.io.File;

import com.tumri.af.utils.Configurator;
import com.tumri.af.utils.PropertyException;
import com.tumri.af.utils.PropertyNotFoundException;

import com.tumri.cbo.backend.NewCampaignBidImpositionPolicy;
import com.tumri.mediabuying.appnexus.AppNexusUtils;
import com.tumri.mediabuying.appnexus.Identity;

/** This class keeps track of an application-specific properties file
 * and provides methods to get properties from that file.
 * It must be initialized with a properties file and should be disposed of
 * by calling shutdown() on the superclass.
 */
public class CBOConfigurator extends Configurator {
	
	private static final String KEY_LDAP_SERVER_URL = "com.tumri.ldap.server.url";
	    
    private static String KEY_CBO_DB_URL = "cbo_db.url";
    private static String KEY_CBO_DB_NAME = "cbo_db.dbname";
    private static String KEY_CBO_DB_DRIVER = "driver";
    private static String KEY_CBO_DB_USERNAME = "cbo_db.username";
    private static String KEY_CBO_DB_PASSWORD = "cbo_db.password";
    private static String CBO_DATA_CLEANUP_CRON_EXPRESSION = "cbo_datacleanup.schedule.cronexpression";
    private static String DEFAULT_CBO_DATA_CLEANUP_CRON_EXPRESSION = "0 30 3 ? * SUN";
    private static String CBO_DATACLEANUP_FILE = "cbo_datacleanup_file";
    
    private static String KEY_SMTP_HOST = "smtp.smtphost";
    private static String KEY_SMTP_PORT = "smtp.smtpport";
    private static String KEY_SMTP_FROM = "smtp.from";
    private static String KEY_SMTP_TO = "smtp.to";
    // For authenticated SMTP
    private static String KEY_SMTP_USERNAME = "smtp.user";
    private static String KEY_SMTP_PASSWORD = "smtp.password";

    private static String DEFAULT_DATABASE_NAME = "cbo_db";
    
    private static final long DEFAULT_ADVERTISER_ID = -1L;
    private static final int DEFAULT_SMTP_PORT = 25;
    
    
	private static final String KEY_ADVERTISERS = "appnexus.advertisers";
    private static final String KEY_APPNEXUS_ADVERTISER_ID = "appnexus.advertiser.id";
    private static final String KEY_APPNEXUS_USER = "appnexus.username";
    private static final String KEY_APPNEXUS_PASSWORD = "appnexus.password";
    private static final String KEY_APPNEXUS_HOST = "appnexus.host";
    private static final String KEY_APPNEXUS_PORT = "appnexus.port";
    private static final String KEY_APPNEXUS_THREAD_COUNT = "appnexus.thread.count";
    private static final String KEY_APPNEXUS_READ_ONLY = "appnexus.readonly";
    private static final String KEY_NEW_CAMPAIGN_BID_IMPOSITION_POLICY = "bidder.new.campaign.bid.imposition.policy";
    private static final String KEY_BID_FREQUENCY = "bidder.bid.frequency";
	private static final String KEY_CAMPAIGNS = "appnexus.campaigns";
	private static final String KEY_CBO_ADMIN_GROUP_NAME = "com.tumri.ldap.group.admin";
	private static final String KEY_CBO_USER_GROUP_NAME = "com.tumri.ldap.group.user";
    private static final String KEY_DATA_DIRECTORY_PATH = "com.tumri.cbo.data.dir";
    private static final String KEY_EXTERNAL_URL_PREFIX = "bidder.external.url.prefix";
    private static final String KEY_DEBUG_BIDDER = "bidder.debug";
    private static final String KEY_EXECUTE_BIDS = "bidder.execute.bids";
    private static final String KEY_FETCH_HISTORICA_DATA = "appnexus.fetch.history";
    private static final String KEY_FORCE_BID_UPDATE = "bidder.force.update";
    private static final String KEY_LOG_DIRECTORY_PATH = "com.tumri.cbo.log.dir";
    private static final String KEY_USER_MESSAGE_FREQUENCY = "bidder.user.message.frequency";
    private static final String KEY_ADMIN_MESSAGE_FREQUENCY = "bidder.admin.message.frequency";
    private static final String KEY_MUFFLE_SQL_TRACE = "bidder.muffle.sql.trace";
    private static final String KEY_PRINT_APPNEXUS_REQUESTS = "appnexus.debug.requests";
    private static final String KEY_PRINT_APPNEXUS_JSON = "appnexus.debug.json";
    private static final String KEY_TRACE_SQL = "bidder.trace.sql";
    private static final String KEY_UPDATE_APPNEXUS = "appnexus.update";

    private static final int DEFAULT_BID_FREQUENCY = 60;  // 60 mins.
    private static final String DEFAULT_USER_MESSAGE_FREQUENCY = "FREQUENCY_DAILY_LATE";
    private static final String DEFAULT_ADMIN_MESSAGE_FREQUENCY = "FREQUENCY_HOURLY_LATE";
    private static final String MIN_BID_FOR_ADJUSTABLE_DAILY_IMP = "bidder.min_bid";
    private static final double DEFAULT_MIN_BID = 0.1d;

	private static final String SPREADSHEET_FILE_NAME = "appnexus.xls";
	
	/** Gets the LDAP URL.
	 * @return The LDAP URL string.
	 */
	public static String getLDAPServerUrl() {
		return getProperty(KEY_LDAP_SERVER_URL);
	}
    
    /** Gets the directory where persistent data is to be stored.
     * @return The path to the data directory.
     */
    public static String getDataDirectoryPath() {
    	return getProperty(KEY_DATA_DIRECTORY_PATH);
    }
    
    /** Gets the directory where log files stored.
     * @return The path to the logs directory.
     */
    public static String getLogDirectoryPath() {
    	return getProperty(KEY_LOG_DIRECTORY_PATH);
    }
    
    /** Gets the database configuration from the properties file.
     * @return The database configuration.
     * @exception PropertyException If any of the required properties are not found.
     */
    public static CBODatabaseConfig getDatabaseConfig() throws PropertyException {
    	CBODatabaseConfig dbConf = new CBODatabaseConfig();
    	dbConf.setDatabaseName(getProperty(KEY_CBO_DB_NAME, DEFAULT_DATABASE_NAME));
    	dbConf.setUrl(getProperty(KEY_CBO_DB_URL));
    	dbConf.setDriver(getProperty(KEY_CBO_DB_DRIVER));
    	dbConf.setUsername(getProperty(KEY_CBO_DB_USERNAME));
    	dbConf.setPassword(getProperty(KEY_CBO_DB_PASSWORD));
    	return dbConf;
    }
    
    /** Gets the mail configuration from the properties file.
     * @return The mail configuration.
     * @exception PropertyException If any of the required properties are not found.
     */
    public static CBOMailConfig getMailConfig() throws PropertyException {
    	CBOMailConfig mailConf = new CBOMailConfig();
    	mailConf.setHost(getRequiredProperty(KEY_SMTP_HOST));
    	mailConf.setPort(getIntProperty(KEY_SMTP_PORT, DEFAULT_SMTP_PORT));
    	mailConf.setFrom(getRequiredProperty(KEY_SMTP_FROM));
    	mailConf.setTo(getProperty(KEY_SMTP_TO));
    	mailConf.setUsername(getProperty(KEY_SMTP_USERNAME));
    	mailConf.setPassword(getProperty(KEY_SMTP_PASSWORD));
    	return mailConf;
    }

    /** Gets the AppNexus identity from the properties file.
     * @return The AppNexus identity from the properties file.
     * @exception PropertyException If any of the required properties are not found.
     */
    @SuppressWarnings("unused")
    public static Identity getAppNexusIdentity() throws PropertyException  {
    	Long advertiserId = getAppNexusAdvertiserId();
    	String user = getAppNexusUserName();
    	String password = getAppNexusPassword();
    	String host = getAppNexusHost();  	
    	long port = getAppNexusPort();
    	boolean readOnly = getAppNexusReadOnly();
    	return AppNexusUtils.identityFromExplicitArgs(advertiserId, user, password, host, port, readOnly);
    }
    
    /** Gets the AppNexus advertiser id.
     * @return The AppNexus advertiser id.
     * @exception PropertyException If any of the required properties are not found.
     */
    public static long getAppNexusAdvertiserId() throws PropertyException {
    	return getLongProperty(KEY_APPNEXUS_ADVERTISER_ID, DEFAULT_ADVERTISER_ID);
    }
    
    /** Gets the AppNexus user name.
     * @return The AppNexus user name.
     * @exception PropertyException If any of the required properties are not found.
     */
    public static String getAppNexusUserName() throws PropertyException {
    	return getRequiredProperty(KEY_APPNEXUS_USER);
    }
 
    /** Gets the AppNexus password.
     * @return The AppNexus password.
     * @exception PropertyException If any of the required properties are not found.
     */
    public static String getAppNexusPassword() throws PropertyException {
    	return getRequiredProperty(KEY_APPNEXUS_PASSWORD);
    }
    
    /** Gets the NewCampaignBidImpositionPolicy.
     * @return The NewCampaignBidImpositionPolicy.
     * @exception PropertyException If any of the required properties are not found.
     */
    public static NewCampaignBidImpositionPolicy getNewCampaignBidImpositionPolicy() throws PropertyException {
        String name = getRequiredProperty(KEY_NEW_CAMPAIGN_BID_IMPOSITION_POLICY);
        return NewCampaignBidImpositionPolicy.valueOf
                (NewCampaignBidImpositionPolicy.class, name);
    }

    /** Gets the AppNexus host.
     * @return The AppNexus host.
     * @exception PropertyException If any of the required properties are not found.
     */
    public static String getAppNexusHost() throws PropertyException {
    	return getRequiredProperty(KEY_APPNEXUS_HOST);
    }

    /** Gets the AppNexus port.
     * @return The AppNexus port.
     * @exception PropertyException If any of the required properties are not found.
     */
    public static int getAppNexusPort() throws PropertyException {
    	return getRequiredIntProperty(KEY_APPNEXUS_PORT);
    }
    
    /** Gets the AppNexus thread count.
     * @return The AppNexus thread count.
     * @exception PropertyException If any of the required properties are not found.
     */
    public static int getAppNexusThreadCount() throws PropertyException {
    	return getRequiredIntProperty(KEY_APPNEXUS_THREAD_COUNT);
    }

    /** Gets the AppNexus read-only flag.
     * @return The AppNexus read-only flag.
     * @exception PropertyException If any of the required properties are not found.
     */
    public static boolean getAppNexusReadOnly() throws PropertyException {
    	return getRequiredBooleanProperty(KEY_APPNEXUS_READ_ONLY);
    }
        
    /** Gets a flag that indicates whether the AppNexus requests are printed
     * to the log.
     * @return True if AppNexus requests should be logged or false if not.
     */
    public static boolean getPrintAppNexusRequestCalls() {
    	return getBooleanProperty(KEY_PRINT_APPNEXUS_REQUESTS, false);
    }
    
    /** Gets a flag that indicates whether the JSON details of the AppNexus 
     * requests are printed to the log.
     * @return True if the details of the JSON AppNexus requests should be logged.
     */
    public static boolean getPrintAppNexusJSONDetails() {
    	return getBooleanProperty(KEY_PRINT_APPNEXUS_JSON, false);
    }
	
	/** Gets the set of advertiser ids to be considered.
	 * @return The set of advertiser ids to be considered.
	 */
	public static String[] getAdvertisers() {
		return getStringArrayProperty(KEY_ADVERTISERS); 
	}
	
	/** Gets the set of campaign ids to be considered.
	 * @return The set of campaign ids to be considered.
	 */
	public static String[] getCampaigns() {
		return getStringArrayProperty(KEY_CAMPAIGNS);
	}
	
	/** Gets the path to the spreadsheet that is used as the current input
	 * and output for the bidder spreadsheet.
	 * @return The path to the bidder spreadsheet.
	 */
	public static String getSpreadsheetFilePath() {
        return getDataDirectoryPath() + File.separator + SPREADSHEET_FILE_NAME;
	}
	
	/** Gets a flag indicating if the updateAppNexus flag should be set on the bidder.
	 * @return True if the updateAppNexus flag should be set on the bidder.
	 */
	public static boolean getUpdateAppNexus() {
		return getBooleanProperty(KEY_UPDATE_APPNEXUS, true);
	}
    
    
    /** Gets a flag for the forceBidUpdate flag on the bidder.
     * @return True if the forceBidUpdate flag should be used.
     */
    public static boolean getForceBidUpdate() {
    	return getBooleanProperty(KEY_FORCE_BID_UPDATE, false);
    }
    
    /** Gets the "fetchHistoricalData" flag for the bidder.
     * @return True to fetch historical data or false not to.
     */
    public static boolean getFetchHistoricalData() {
    	return getBooleanProperty(KEY_FETCH_HISTORICA_DATA, true);
    }
    
    /** Gets the execute bids flag.
     * @return True if bids should be executed or false if not.
     */
    public static boolean getExecuteBids() {
    	return getBooleanProperty(KEY_EXECUTE_BIDS, false);
    }
    
    /** Gets the bid frequency in minutes.
     * @return The bid frequency in minutes.
     */
    public static int getBidFrequency() {
    	return getIntProperty(KEY_BID_FREQUENCY, DEFAULT_BID_FREQUENCY);
    }
    
    /** Gets the frequency for computing user messages in minutes.
     * @return The bid frequency in minutes.
     */
    public static String getUserMessageFrequency() {
    	return getProperty(KEY_USER_MESSAGE_FREQUENCY, DEFAULT_USER_MESSAGE_FREQUENCY);
    }

    /** Gets the frequency for computing admin messages in minutes.
     * @return The bid frequency in minutes.
     */
    public static String getAdminMessageFrequency() {
    	return getProperty(KEY_ADMIN_MESSAGE_FREQUENCY, DEFAULT_ADMIN_MESSAGE_FREQUENCY);
    }
    
    /**
     * Gets the cron expression to execute the data cleanup script
     * @return
     */
	public static String getDataCleanUpCronExpression() {
		return getProperty(CBO_DATA_CLEANUP_CRON_EXPRESSION,
				DEFAULT_CBO_DATA_CLEANUP_CRON_EXPRESSION);
	}
	
	/**
	 * Gets the data clean up script file location with the filename
	 * @return
	 */
	public static String getDataCleanUpScriptFile() {
		return getProperty(CBO_DATACLEANUP_FILE, null);
	}

    /** Gets the URL prefix used to contact this CBO server.  The value
     * returned is such that Zini HTTPHandler service names can be appended
     * to the prefix thereby producing fully-functioning absolute URLs.
     * This is useful when generating email with links that should teleport
     * the user into specific places in the CBO universe.
     * @return A String of the form https://cbo.ensemble-digital.com/cbo/secure/zini/
     * @throws PropertyNotFoundException If the property can't be found.
     *
     */
    public static String getExternalURLPrefix()
            throws PropertyNotFoundException{
    	return getRequiredProperty(KEY_EXTERNAL_URL_PREFIX);
    }
    
    /** Gets a flag to indicate if the debug flag should be passed into the bidder.
     * @return True if the bidder should have its debug flag set or false if not.
     */
    public static boolean getDebugBidder() {
    	return getBooleanProperty(KEY_DEBUG_BIDDER);
    }

    /** Gets a flag to indicate if trace sql flag should be passed into the bidder.
     * @return True if the bidder should have its trace sql flag set or false if not.
     */
    public static boolean getTraceSQL() {
    	return getBooleanProperty(KEY_TRACE_SQL);
    }

    /** Gets a flag to indicate if the muffle sql trace flag should be passed into the bidder.
     * @return True if the bidder should have its muffle sql trace flag set or false if not.
     */
    public static boolean getMuffleSQLTrace() {
    	return getBooleanProperty(KEY_MUFFLE_SQL_TRACE);
    }
    
    /** Gets the name of the administrator group.
     * Any LDAP users who are in this group have 
     * administrator and user permissions on the bid optimizer.
     * @return The name of the CBO administrator group.
     * @throws PropertyNotFoundException If the property is not found.
     */
	public static Object getCBOAdminGroupName() throws PropertyNotFoundException {
		return getRequiredProperty(KEY_CBO_ADMIN_GROUP_NAME);
	}
    /** Gets the name of the CBO Users group.
     * Any LDAP users who are in this group have 
     * user permissions on the bid optimizer.
     * @return The name of the CBO user group.
     * @throws PropertyNotFoundException If the property is not found.
     */
	public static Object getCBOUserGroupName() throws PropertyNotFoundException {
		return getRequiredProperty(KEY_CBO_USER_GROUP_NAME);
	}

    /**
     * Gets the minimum bid amount for Adjustable daily impressions policy
     * @return
     */
    public static double getMinBidForAdjDailyImpressionsPolicy() {
        return getDoubleProperty(MIN_BID_FOR_ADJUSTABLE_DAILY_IMP, DEFAULT_MIN_BID);
    }
}
