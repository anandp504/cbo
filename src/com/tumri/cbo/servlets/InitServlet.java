package com.tumri.cbo.servlets;

import com.tumri.af.utils.PropertiesFileReader;
import com.tumri.af.utils.PropertyException;
import com.tumri.af.utils.Utils;
import com.tumri.af.utils.VersionUtils;

import com.tumri.cbo.backend.NewCampaignBidImpositionPolicy;
import com.tumri.cbo.backend.Bidder;
import com.tumri.cbo.backend.BidderSchedulerJob;
import com.tumri.cbo.backend.ScheduleFrequency;
import com.tumri.cbo.monitor.MonitorSchedulerJob;
import com.tumri.cbo.scheduler.DataCleanUpScheduler;
import com.tumri.cbo.utils.CBOConfigurator;
import com.tumri.cbo.utils.CBODatabaseConfig;

import com.tumri.mediabuying.appnexus.AppNexusUtils;
import com.tumri.mediabuying.appnexus.Identity;
import com.tumri.mediabuying.zini.HTTPHandler;
import com.tumri.sso.ssoc.SSOClient;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.IOException;
import java.util.Properties;

/** Initializes the SSO client and any other required servlet services.
 */
public final class InitServlet extends HttpServlet {

	private static final long serialVersionUID = -8451953370126357005L;

	private final static Logger log = Logger.getLogger(InitServlet.class);
	
	private final static String APP_PROPERTY_FILE_NAME = "cbo.properties";
	private final static String LOG4J_PROPERTIES_FILE_NAME = "cboLog4j.properties";
	private final static String CBO_VERSION_PROPERTIES_FILE = "cbo_version.properties";
	
	private final static String KEY_LOG4J_PROPS_FILE = "log4j.properties";
	
	private final static String DEFAULT_BIDDER_NAME_PREFIX = "Bidder_";
	
    public static void earlyInit() throws ServletException
    {
        try
        {
            // Read the application-specific properties file.
            HTTPHandler.setRootURL(Bidder.CBO_NAME);
            Properties appProps = readPropertiesFile(APP_PROPERTY_FILE_NAME);
            CBOConfigurator.init(appProps);

            // Set the properties file for log4J.
            String log4jFileName = CBOConfigurator.getProperty(KEY_LOG4J_PROPS_FILE);
            if(Utils.isEmpty(log4jFileName)) {
                log4jFileName = LOG4J_PROPERTIES_FILE_NAME;
            }
            LogManager.resetConfiguration();   // Need to clear the old configuration first.
            PropertyConfigurator.configure(readPropertiesFile(log4jFileName));

            // Initialize version properties.  Must be called before initializeBidder().
            VersionUtils.init(readPropertiesFile(CBO_VERSION_PROPERTIES_FILE));
        }
        catch (Throwable t)
        {
			log.error("Exception caught during CBO startup (early)", t);
		}
    }
	/** Initializes the SSO client and any other things required.
	 * @throws ServletException
	 */
	public void init() throws ServletException {
		try {
			// Read the application-specific properties file.
            HTTPHandler.setRootURL(Bidder.CBO_NAME); 
			Properties appProps = readPropertiesFile(APP_PROPERTY_FILE_NAME);
			CBOConfigurator.init(appProps);
			
			// Set the properties file for log4J.
		    String log4jFileName = CBOConfigurator.getProperty(KEY_LOG4J_PROPS_FILE);
		    if(Utils.isEmpty(log4jFileName)) {
		        log4jFileName = LOG4J_PROPERTIES_FILE_NAME;
		    }
		    LogManager.resetConfiguration();   // Need to clear the old configuration first.
			PropertyConfigurator.configure(readPropertiesFile(log4jFileName));
			
			// Initialize version properties.  Must be called before initializeBidder().
			VersionUtils.init(readPropertiesFile(CBO_VERSION_PROPERTIES_FILE));
			
			// Initialize SSO
            SSOClient.init(appProps);
            
            // Initialize the bidder using the properties in the properties file.
            initializeBidder();
            
            // Initialize Quartz to run bid optimizer regularly.
            log.info("Scheduling bidder job");
            BidderSchedulerJob.scheduleBidderJob
                    (ScheduleFrequency.get(CBOConfigurator.getBidFrequency()));
            
            // Initialize Quartz to emit messages to users and admins regularly.
            log.info("Scheduling user message job");
            MonitorSchedulerJob.scheduleUserMessageJob
                    (ScheduleFrequency.get
                            (CBOConfigurator.getUserMessageFrequency()));
            log.info("Scheduling admin message job");
            MonitorSchedulerJob.scheduleAdminMessageJob
                    (ScheduleFrequency.get
                            (CBOConfigurator.getAdminMessageFrequency()));
            
            log.info("Scheduling data clean up job");
			DataCleanUpScheduler.scheduleDataCleanUp(CBOConfigurator
					.getDataCleanUpCronExpression());

			log.info("CBO Server started successfully.");
		} catch (Throwable t) {
			log.error("Exception caught during CBO startup", t);
		}
	}
	
	/** Cleans up any resources gracefully before shutting down
	 */
	public void destroy() {
		try {
			SSOClient.shutdown();
			LogManager.shutdown();
			CBOConfigurator.shutdown();
			Bidder.shutdown();
			log.info("Server shutdown successfully.");
		} catch (Exception e) {
			log.error("Exception caught during shutdown", e);
		}
	}
	
	// -------------------------- Protected methods ---------------------
	
	/** Finds a properties file by the indicated name and reads it.
	 * Looks in the standard deployment locations within the application.
	 * @param fileName The file name not including any directory information (e.g. / or \)
     * @return The properties.
	 * @exception IOException If error reading the file.
	 */
	protected static Properties readPropertiesFile(String fileName) throws IOException {
		return PropertiesFileReader.getInstance().readPropertiesFile(fileName);
	}

	// -------------------------- Private methods ---------------------
	
	/** Initializes the bidder from the configuration properties.
	 * @exception PropertyException If a required property is not found.
	 */
	private void initializeBidder() throws PropertyException {
		
		String name = DEFAULT_BIDDER_NAME_PREFIX + VersionUtils.getAppVersion();   // A name for this bidder class instance.
		CBODatabaseConfig dbConfig = CBOConfigurator.getDatabaseConfig();
        // CBOMailConfig mailConfig =
                CBOConfigurator.getMailConfig();
		boolean debug = CBOConfigurator.getDebugBidder();
        boolean executeBids = CBOConfigurator.getExecuteBids();
        String infoSchemaDB = null;
        Long maxAdvertisers = null;
        boolean readOnly = CBOConfigurator.getAppNexusReadOnly();
        String inputPath = CBOConfigurator.getSpreadsheetFilePath();
        String outputPath;
        outputPath = inputPath;

        String[] advertisers = CBOConfigurator.getAdvertisers();
        String[] campaigns = CBOConfigurator.getCampaigns();
        
        // AppNexus identity arguments.
        long advertiserId = CBOConfigurator.getAppNexusAdvertiserId();
        String appNexusUser =  CBOConfigurator.getAppNexusUserName();
        String appNexusPassword = CBOConfigurator.getAppNexusPassword();
        String host = CBOConfigurator.getAppNexusHost();  
        Long port = (long) CBOConfigurator.getAppNexusPort();
        int threadCount = CBOConfigurator.getAppNexusThreadCount();
        NewCampaignBidImpositionPolicy bidImpositionPolicy =
                CBOConfigurator.getNewCampaignBidImpositionPolicy();
        
        Identity appNexusIdentity = 
        	AppNexusUtils.identityFromExplicitArgs(advertiserId, appNexusUser, appNexusPassword, 
        										   host, port, readOnly);
        
        Bidder bidder = Bidder.initialize(true, dbConfig.getDriver(), dbConfig.getUsername(), dbConfig.getPassword(), 
        								  dbConfig.getDatabaseName(), dbConfig.getUrl(), infoSchemaDB, 
        				  			      appNexusIdentity, name, debug, executeBids,
                                          bidImpositionPolicy, threadCount);
        
        // Spreadsheet paths.
        bidder.setInputSpreadsheetPath(inputPath);
        bidder.setOutputSpreadsheetPath(outputPath);
        
        // Limit to the specified advertisers and campaigns.
        bidder.setAdvertiserIds(Bidder.toSet(advertisers));
        bidder.setCampaignIds(Bidder.toSet(campaigns));
        
        
        // More debug options.
        bidder.setPrintAppNexusDebugs(CBOConfigurator.getPrintAppNexusRequestCalls());
        bidder.setPrintAppNexusJSON(CBOConfigurator.getPrintAppNexusJSONDetails());
        bidder.setUpdateAppNexus(CBOConfigurator.getUpdateAppNexus());
        bidder.setForceBidUpdating(CBOConfigurator.getForceBidUpdate());
        bidder.setFetchHistoricalData(CBOConfigurator.getFetchHistoricalData());
        bidder.setTraceSQL(CBOConfigurator.getTraceSQL());
        bidder.setMuffleSQLTrace(CBOConfigurator.getMuffleSQLTrace());
        bidder.setAppNexusReadOnly(readOnly);
        bidder.setMaxAdvertisers(maxAdvertisers);
	}
}