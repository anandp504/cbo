package com.tumri.cbo.backend;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.tumri.af.exceptions.BusyException;
import com.tumri.cbo.monitor.FetchMessagesHTTPHandler;
import com.tumri.cbo.monitor.ReportingAdminHTTPHandler;
import com.tumri.cbo.monitor.ShowMessagesHTTPHandler;
import com.tumri.cbo.servlets.HealthCheckServlet;
import com.tumri.cbo.servlets.InitServlet;
import com.tumri.mediabuying.appnexus.*;
import com.tumri.mediabuying.appnexus.agent.AppNexusServicePerspective;
import com.tumri.mediabuying.appnexus.agent.AppNexusTheory;
import com.tumri.mediabuying.appnexus.services.*;
import com.tumri.mediabuying.zini.*;
import org.apache.log4j.*;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import au.com.bytecode.opencsv.CSVParser;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import com.mongodb.DB;
import com.tumri.cbo.testing.ServicePersistenceFilter;
import com.tumri.cbo.testing.ServiceRecoveryFilter;
import com.tumri.cbo.testing.TestCore;
import javax.servlet.ServletException;
import java.text.ParseException;

import java.io.*;
import java.sql.Timestamp;
import java.util.*;


public class Bidder extends Atom implements ReportsStatus {

    public static final String CBO_NAME = "cbo";
	// These should have setters and getters if they are going to be used.
    boolean selectExpiredCampaigns = false;
    // Milliseconds before getCurrentTime() for which we're prepared to fetch
    // otherwise expired campaigns..
    long campaignEndDateTolerance = (7 * 24 * 3600 * 1000); // A week.

 // Configuration vars...................
    private Identity m_appNexusIdentity;
    private String name = "Unnamed";
    private boolean debugMode = false;
    private boolean deletePrefetchTempFilesP = false; 
    private boolean updateAppNexus = true;
    private boolean forceBidUpdating = false;
    private boolean fetchHistoricalData = true;  
    private boolean m_effectuateBidsP = false;
    private boolean m_traceSQL = false;
    private boolean m_muffleSQLTrace = false;
    private boolean m_paused = false;
    private Date m_lastRunTime = null; // Was this: new Date();

    private Date m_overrideCurrentTime = null;
    private Long m_maxAdvertisers = null;
    Set<String> m_advertiserIds = null;
    Set<String> m_campaignIds = null;
    
    private String bidderSQLConnectorDriver = null;
    private String bidderSQLConnectorUser = null;
    private String bidderSQLConnectorPassword = null;
    private String bidderSQLConnectorDBName = null;
    private String bidderSQLConnectorUrl = null;
    private String bidderInformationSchemaSQLConnectorUrl = null;
    private static int defaultAppNexusThreadCount = 1;
    private int appNexusThreadCount = defaultAppNexusThreadCount;
 
    private SQLConnector bidderSQLConnector = null;
    private SQLConnector bidderInformationSchemaSQLConnector = null;
    private boolean defeatFetchingAppNexusReports = false;

    private String m_inputSpreadsheetPath = null;
    private String m_outputSpreadsheetPath = null;
    private boolean persistAppNexusP = false;
    private boolean restoreAppNexusP = false;
    private String mongoHost = DEFAULT_MONGO_DB_HOST;
    private int mongoPort = DEFAULT_MONGO_DB_PORT;

    public void setPersistAppNexusP(boolean x)
    {
        persistAppNexusP = x;
    }

    public void setRestoreAppNexusP(boolean x)
    {
        restoreAppNexusP = x;
    }

    public void setMongoHost(String h)
    {
        mongoHost = h;
    }

    public void setMongoPort(int p)
    {
        mongoPort = p;
    }

    // Used to remember the thread we're running in.  Just for debugging, so
    // could conceivably be wrong it we get called from multiple threads.
    // Null generally, and Thread-valued when processing bid instructions.
    private Thread owningThread = null;
    
    // ------------------ State information --------------
    
    private CampaignData currentCampaign = null;
    private AdvertiserData currentAdvertiser = null;
  
    // State stuff.
    private Status status = new Status("Uninitialised");
    private LinkedList<Status> statusHistory = new LinkedList<Status>();
    private boolean m_processingBidInstructions = false;
    
    private BidderState m_bidderState = BidderState.IDLE;
    private HashSet<BidderStateChangeListener> m_bidderStateChangeListeners = new HashSet<BidderStateChangeListener>();

    QueryContext lastQctx = null;
    AppNexusLogPrinter logPrinter = new AppNexusLogPrinter(System.out);
    BidderControlHTTPHandler bidderControlHTTPHandler = null;

    Set<Date> historicalDataKnownDates = null;
    Set<Date> siteDomainReportKnownDates = null;
    AppNexusTheory appNexusTheory = null;
    // Filled in when we've finished processing/
    private Map<Long, AdvertiserData> advertiserMap = null;
    static final String DEFAULT_MONGO_DB_HOST = "localhost";
    static final int DEFAULT_MONGO_DB_PORT = 27017;

    private static final NewCampaignBidImpositionPolicy
            defaultNewCampaignBidImpositionPolicy =
                NewCampaignBidImpositionPolicy.IMMEDIATE;

    private static NewCampaignBidImpositionPolicy bidImpositionPolicy =
            defaultNewCampaignBidImpositionPolicy;

    public static NewCampaignBidImpositionPolicy getNewCampaignBidImpositionPolicy()
    {
        return bidImpositionPolicy;
    }

    public static void setNewCampaignBidImpositionPolicy
            (NewCampaignBidImpositionPolicy policy)
    {
        bidImpositionPolicy = policy;
    }


    // Widget tests - these are used by the (commented-out) entries in
    // BidderControlHTTPHandler.
    /*
    String xxTest1 = "xxTest1";
    Number xxTest2 = 42.3;
    Long xxTest3 = 42L;
    Long xxTest4 = 42L;
    Long xxTest5 = 42L;
    Integer xxTest6 = 42;
    Integer xxTest7 = 42;
    Integer xxTest8 = 42;
    Double xxTest9 = 1001.6D;
    Date xxTest10 = new Date( );
    Date xxTest11 = new Date( );
    Date xxTest12 = new Date( );
    String xxTest13 = "B";
    List<String> xxTest14 = Arrays.asList("B", "D");
    */
    // ----- End of widget tests.
    
    
    
    
    // The one and only instance in this JVM
    private static Bidder s_instance;

    // 50 cents CPM if we can't think of anything better.
    static Double defaultDefaultBid = 0.4321234; // Should be easy to spot!
    static Long defaultDefaultDailyImpressionLimit = null;
    static Double fixedToECPBidFraction = 1.0;
    static Double eCPMToBaseBidTransitionFraction = 0.8;

    // -------------------------------------

    @SuppressWarnings("unused")
    public static Constant BIDDER = Syms.intern("BIDDER");
    
    private final static String PAUSED_SUFFIX = " - Paused";
    private final static String MSG_BIDDER_BUSY = "The bidder is busy processing bid instructions.  " + 
    											  "Please try running the bidder again later.";
    
    public static final IndividualVariable _X =  IndividualVariable.getIndVar("X");
    public static final IndividualVariable _Name = IndividualVariable.getIndVar("NAME");
    static final String EXPLICITLY_FETCHED_DATES = "EXPLICITLY_FETCHED_DATES";
    private final static String HISTORICAL_DATA_TEMP_FILE_PREFIX = "CBO_HistoricalData_";
    public final static String SITE_DOMAIN_TEMP_FILE_PREFIX = "CBO_SiteDomain_";
    public final static String FREQUENCY_TEMP_FILE_PREFIX = "CBO_Frequency_";
    static int DEFAULT_DEBUG_PORT = 10001;

    static final SentenceTemplate getAdvertisersQuery =
            new SentenceTemplate(
                    "(ask-all ?Advertiser\n" +
                            "  (and\n" +
                            "       (oneof ?AdvertiserId @AdvertiserIds)\n" +
                            "	    (QuickStatsInterval Lifetime)\n" +
                            "	    (AppNexus.Advertiser.Id ?Advertiser ?AdvertiserId)" +
                            "	    (AppNexus.Advertiser.State ?Advertiser \"active\")" +
                            "))");

    // Thsi is the version we used until we discovered that some line items
    // have no profiles.  The one below with all the *Total relations seems
    // to do the trick.
    @SuppressWarnings("unused")
    static final SentenceTemplate getCampaignDataQueryHold =
            new SentenceTemplate(
                    "(ask-all (?Advertiser\n" +
                            "		   ?LineItem\n" +
                            "		   ?LineItemProfile\n" +
                            "		   ?Campaign\n" +
                            "		   ?CampaignProfile)\n" +
                            "  (and\n" +
                            "	    (QuickStatsInterval Lifetime)\n" +
                            "	    (AppNexus.Advertiser.Id ?Advertiser ?AdvertiserId)\n" +
                            "	    (AppNexus.LineItem ?Advertiser)\n" +
                            "	    (AppNexus.Profile ?Advertiser)\n" +
                            "	    (AppNexus.Campaign.End_date ?Advertiser ?Campaign ?EndDate)\n" +
                            "	    (>= ?EndDate ?Now)\n" +
                            "	    (AppNexus.Campaign.Id ?Advertiser ?Campaign ?CampaignId)\n" +
                            "	    (AppNexus.Campaign.State ?Advertiser ?Campaign \"active\")\n" +
                            "	    (AppNexus.Campaign.Line_item_id ?Advertiser ?Campaign ?LineItemId)\n" +
                            "	    (AppNexus.LineItem.Id ?Advertiser ?LineItem ?LineItemId)\n" +
                            "	    (AppNexus.LineItem.Profile_Id ?Advertiser ?LineItem ?LineItemProfileId)\n" +
                            "	    (AppNexus.Campaign.Profile_Id ?Advertiser ?Campaign ?CampaignProfileId)\n" +
                            "	    (AppNexus.Profile.Id ?Advertiser ?LineItemProfile ?LineItemProfileId)" +
                            "	    (AppNexus.Profile.Id ?Advertiser ?CampaignProfile ?CampaignProfileId)" +
                            "))");

    static final SentenceTemplate getCampaignDataQuery =
            new SentenceTemplate(
                    "(ask-all (?Advertiser\n" +
                            "		   ?LineItem\n" +
                            "		   ?LineItemProfile\n" +
                            // "		   ?LineItemProfileId\n" +
                            // "		   ?LineItemId\n" +
                            "		   ?Campaign\n" +
                            "		   ?CampaignProfile)\n" +
                            "  (and\n" +
                            "	    (QuickStatsInterval Lifetime)\n" +
                            "	    (AppNexus.Advertiser.Id ?Advertiser ?AdvertiserId)\n" +
                            "	    (AppNexus.LineItem ?Advertiser)\n" +
                            "	    (AppNexus.Profile ?Advertiser)\n" +
                            "	    (AppNexus.Campaign.End_date ?Advertiser ?Campaign ?EndDate)\n" +
                            "	    (>= ?EndDate ?Now)\n" +
                            "	    (AppNexus.Campaign.Id ?Advertiser ?Campaign ?CampaignId)\n" +
                            "	    (AppNexus.Campaign.State ?Advertiser ?Campaign \"active\")\n" +
                            "	    (AppNexus.Campaign.Line_item_id ?Advertiser ?Campaign ?LineItemId)\n" +
                            "	    (AppNexus.LineItem.IdTotal ?Advertiser ?LineItem ?LineItemId)\n" +
                            "	    (AppNexus.LineItem.Profile_IdTotal ?Advertiser ?LineItem ?LineItemProfileId)\n" +
                            "	    (AppNexus.Campaign.Profile_Id ?Advertiser ?Campaign ?CampaignProfileId)\n" +
                            "	    (AppNexus.Profile.IdTotal ?Advertiser ?LineItemProfile ?LineItemProfileId)" +
                            "	    (AppNexus.Profile.Id ?Advertiser ?CampaignProfile ?CampaignProfileId)" +
                            "))");

    static final SentenceTemplate bidderExistenceSentences =
            new SentenceTemplate("(isa ?x bidder)",
                                 "(isa ?x otherobject)",
                                 "(isa ?x thing)",
                                 "(prettyname ?x ?name)",

                                 "(prettyname Bidder \"Bidder\")",
                                 "(subclassof Bidder otherobject)",
                                 "(isa Bidder class)",
                                 "(isa Bidder thing)"
                                );

    PerformanceHistoryDAOImpl performanceHistoryDAOImpl;
    // Widget tests - these are used by the (commented-out) entries in
    // BidderControlHTTPHandler.
    /*
    String xxTest1 = "xxTest1";
    Number xxTest2 = 42.3;
    Long xxTest3 = 42L;
    Long xxTest4 = 42L;
    Long xxTest5 = 42L;
    Integer xxTest6 = 42;
    Integer xxTest7 = 42;
    Integer xxTest8 = 42;
    Double xxTest9 = 1001.6D;
    Date xxTest10 = new Date( );
    Date xxTest11 = new Date( );
    Date xxTest12 = new Date( );
    String xxTest13 = "B";
    List<String> xxTest14 = Arrays.asList("B", "D");
    */
    // ----- End of widget tests.

    static int missingDayHistoricalDataMaxChunkSize = 31;
    static int missingDaySiteDomainReportMaxChunkSize = 1;
    static long milliSecondsInADay = 24 * 3600 * 1000;
    static SynchDateFormat dateParser = new SynchDateFormat("yyyy-MM-dd HH:mm:ss");
    static SynchDateFormat dayParser = new SynchDateFormat("yyyy-MM-dd");
    static SynchDateFormat hourParser = new SynchDateFormat("yyyy-MM-dd HH:mm");
    static SynchDateFormat timeParser = new SynchDateFormat("HH:mm:ss");

    public static final String advertiser_id_ColName = "advertiser_id";
    public static final String line_item_id_ColName  = "line_item_id";
    public static final String line_item_profile_id_ColName = "line_item_profile_id";
    public static final String campaign_id_ColName   = "campaign_id";
    public static final String campaign_profile_id_ColName = "campaign_profile_id";
    public static final String day_Colname = "day";
    public static final String hour_Colname = "hour";

    static final List<String> timeStampTypedColumns =
            Arrays.asList(hour_Colname, day_Colname);

    public static String[] appNexusNetworkReportDimensions
            // WARNING:  This list must align with historicalDataColumns
            //           because we bulk load it!
            = {
            advertiser_id_ColName,
            line_item_id_ColName,
            campaign_id_ColName,
            hour_Colname
    };

    public static String[] appNexusNetworkReportMeasures
            // WARNING:  This list must align with historicalDataColumns
            //           because we bulk load it!
            = {
            "imps",
            "clicks",
            "cost"
    };

    public static String[] appNexusNetworkSiteDomainPerformanceReportDimensions
            = {
            advertiser_id_ColName,
            line_item_id_ColName,
            campaign_id_ColName,
            day_Colname,
            "site_domain"
    };

    public static String[] appNexusNetworkSiteDomainPerformanceReportMeasures
            // WARNING:  This list must align with historicalDataColumns
            //           because we bulk load it!
            = {
            "imps",
            "clicks",
            "media_cost",
            "cpm"
    };

    public static String[] appNexusNetworkAdvertiserFrequencyReportDimensions
            = {
            advertiser_id_ColName,
            line_item_id_ColName,
            campaign_id_ColName,
            hour_Colname,
            "creative_frequency_bucket_id",
            "creative_frequency_bucket"
    };

    public static String[] appNexusNetworkAdvertiserFrequencyReportMeasures
            = {
            "imps",
            "clicks",
            "media_cost",
            "cost_ecpm"
    };

    public static String[] appNexusAdvertiserReportDimensions
            = {
            day_Colname, hour_Colname,
            "line_item_name", line_item_id_ColName,
            "campaign_name", campaign_id_ColName
    };

    public static String[] appNexusAdvertiserReportMeasures
            = {"imps", "clicks", "ctr", "spend", "media_cost"};

    public static int reportDurationInDays = 1;
    public static final CellStyleName DEFAULT_STYLE_NAME =
            CellStyleName.greenBackground;
    
    static int defaultStyleSeverity = 1;
    

    static Object[][] styleSeverities =
            {
                    { CellStyleName.redBackground, 100 },
                    { CellStyleName.orangeBackground, 50},
                    { CellStyleName.greenBackground, 20 },
                    { CellStyleName.boldText, 10 }
            };

    static int styleSeverity(CellStyleName x)
    {
        for(Object[] row: styleSeverities)
        {
            if(row[0].equals(x)) return (Integer) row[1];
        }
        return defaultStyleSeverity;
    }

    @SuppressWarnings("unused")
    static CellStyleName styleMax(CellStyleName a, CellStyleName b)
    {
        int aa = styleSeverity(a);
        int bb = styleSeverity(b);
        if(aa > bb) return a;
        else return b;
    }

    @SuppressWarnings("unused")
    public static int getColumnIndex(String name, ColumnData[] cols)
    {
        int i = 0;
        for(ColumnData col: cols)
        {
            if(col.getName().equals(name)) return i;
            i = i + 1;
        }
        return -1;
    }

    static ColumnData[] historicalDataKnownDatesColumns =
            new ColumnData[]
                    {
                            new ColumnData(hour_Colname, "Timestamp",
                                           Date.class)
                    };

    static IndexData[] historicalDataKnownDatesIndices =
            new IndexData[]
                    {
                            new IndexData("PK", "HistoricalDataKnownDates",
                                    new String[]{hour_Colname}, true, true)
                    };

    static TableData historicalDataKnownDatesTable =
            new TableData("HistoricalDataKnownDates",
                    historicalDataKnownDatesColumns,
                    historicalDataKnownDatesIndices);

    //===================================================

    static ColumnData[] siteDomainReportKnownDatesColumns =
            new ColumnData[]
                    {
                            new ColumnData(day_Colname, "Timestamp",
                                           Date.class)
                    };

    static IndexData[] siteDomainReportKnownDatesIndices =
            new IndexData[]
                    {
                            new IndexData("PK", "Site_Domain_Report_Known_Dates",
                                    new String[]{day_Colname}, true, true)
                    };

    static TableData siteDomainReportKnownDatesTable =
            new TableData("Site_Domain_Report_Known_Dates",
                    siteDomainReportKnownDatesColumns,
                    siteDomainReportKnownDatesIndices);

    //===================================================

    static ColumnData[] historicalDataColumns =
            new ColumnData[]
                    {
                            new ColumnData(advertiser_id_ColName, "int", Long.class),
                            new ColumnData(line_item_id_ColName,  "int", Long.class),
                            new ColumnData(campaign_id_ColName,   "int", Long.class),
                            new ColumnData(hour_Colname,    "Timestamp", Date.class),
                            new ColumnData("imps",             "bigint", Long.class),
                            new ColumnData("clicks",           "bigint", Long.class),
                            new ColumnData("cost",             "double", Double.class)
                    };

    static IndexData[] historicalDataIndices =
            new IndexData[]
                    {
                            new IndexData("PK", "HistoricalData",
                                    new String[]
                                            {
                                                    advertiser_id_ColName,
                                                    campaign_id_ColName,
                                                    hour_Colname,
                                                    line_item_id_ColName
                                            },
                                    true, true),
                            new IndexData("ix1", "HistoricalData",
                                    new String[]
                                            {
                            						hour_Colname,
                                                    advertiser_id_ColName,
                                                    campaign_id_ColName
                                            },
                                    false, false)
                    };

    static TableData historicalDataTable =
            new TableData("HistoricalData",
                    historicalDataColumns,
                    historicalDataIndices);

    //===================================================

    static ColumnData[] changeLogColumns =
            new ColumnData[]
                    {
                            new ColumnData(advertiser_id_ColName, "int",          Long.class),
                            new ColumnData(campaign_id_ColName,   "int",          Long.class),
                            new ColumnData(campaign_profile_id_ColName, "int",    Long.class),
                            new ColumnData("change_type",         "varchar(64)",  String.class),
                            new ColumnData("s0",                  "varchar(256)", String.class),
                            new ColumnData("s1",                  "varchar(256)", String.class),
                            new ColumnData("s2",                  "varchar(256)", String.class),
                            new ColumnData("s3",                  "varchar(256)", String.class),
                            new ColumnData("n0",                  "double",       Double.class),
                            new ColumnData("n1",                  "double",       Double.class),
                            new ColumnData("n2",                  "double",       Double.class),
                            new ColumnData("n3",                  "double",       Double.class),
                            new ColumnData("campaign_json",       "text",         String.class),
                            new ColumnData("campaign_profile_json", "text",         String.class),
                            new ColumnData("event_time",          "Timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP", Date.class)
                    };

    static IndexData[] changeLogIndices =
            new IndexData[]
                    {
                            new IndexData("PK", "ChangeLog",
                                    new String[]
                                            {
                                                    advertiser_id_ColName,
                                                    campaign_id_ColName,
                                                    "event_time"
                                            },
                                    true, true)
                    };

    static TableData changeLogTable =
            new TableData("ChangeLog",
                    changeLogColumns,
                    changeLogIndices);

    //===================================================

    public static ColumnData[] observedDataColumns =
            new ColumnData[]
                    {
                            new ColumnData(advertiser_id_ColName,         "int",          Long.class),
                            new ColumnData(line_item_id_ColName,          "int",          Long.class),
                            new ColumnData(line_item_profile_id_ColName,  "int",          Long.class),
                            new ColumnData(campaign_id_ColName,           "int",          Long.class),
                            new ColumnData(campaign_profile_id_ColName,   "int",          Long.class),
                            new ColumnData("observation_time",            "Timestamp",    Date.class),
                            new ColumnData("observation_day",             "Timestamp",    Date.class),
                            new ColumnData("base_bid",                    "double",       Double.class),
                            new ColumnData("max_bid",                     "double",       Double.class),
                            new ColumnData("daily_impressions_budget",    "int",          Long.class),
                            new ColumnData("lifetime_impressions_budget", "int",          Long.class),
                            new ColumnData("start_date",                  "Timestamp",    Date.class),
                            new ColumnData("end_date",                    "Timestamp",    Date.class),
                            new ColumnData("first_impression_time",       "Timestamp",    Date.class),
                            new ColumnData("user_group_low",              "int",          Long.class),
                            new ColumnData("user_group_high",             "int",          Long.class),
                            new ColumnData("is_child",                    "int",          Long.class),
                            new ColumnData("day_of_week",                 "int",          Long.class),
                            new ColumnData("day_type",                    "int",          Long.class),
                            new ColumnData("bidding_policy",              "varchar(32)",  String.class),
                            new ColumnData("line_item_json",              "text",         String.class),
                            new ColumnData("line_item_profile_json",      "text",         String.class),
                            new ColumnData("campaign_json",               "text",         String.class),
                            new ColumnData("campaign_profile_json",       "text",         String.class),
                            new ColumnData("combined_json",               "text",         String.class),
                            new ColumnData("combined_profile_json",       "text",         String.class),
                            new ColumnData("sequence_number",             "int null default null", Long.class),
                            new ColumnData("materially_different",        "boolean null default null", Long.class),
                            new ColumnData("has_material_differences",    "boolean not null default false", Long.class),
                            new ColumnData("material_differences",        "text null default null", String.class),
                            new ColumnData("attribute_changed_but_will_not_affect_delivery",    "int null default null", Long.class),
                            new ColumnData("attribute_changed_with_unknown_effect_on_delivery", "int null default null", Long.class),
                            new ColumnData("attribute_changed_increases_delivery",              "int null default null", Long.class),
                            new ColumnData("attribute_changed_decreases_delivery",              "int null default null", Long.class),
                            new ColumnData("attribute_increased_increases_delivery",            "int null default null", Long.class),
                            new ColumnData("attribute_decreased_decreases_delivery",            "int null default null", Long.class),
                            new ColumnData("attribute_increased_decreases_delivery",            "int null default null", Long.class),
                            new ColumnData("attribute_decreased_increases_delivery",            "int null default null", Long.class),
                            new ColumnData("targeting_widened_increases_delivery",              "int null default null", Long.class),
                            new ColumnData("targeting_narrowed_decreases_delivery",              "int null default null", Long.class)
                    };

    static IndexData[] observedDataIndices =
            new IndexData[]
                    {
                            new IndexData("PK", "ObservedData",
                                    new String[]
                                            {
                                                    advertiser_id_ColName,
                                                    campaign_id_ColName,
                                                    "observation_time"
                                            },
                                    true, true),
                            new IndexData("ix1", "ObservedData",
                                    new String[]
                                            {
                                                    "sequence_number",
                                                    campaign_id_ColName,
                                                    advertiser_id_ColName,
                                                    campaign_profile_id_ColName,
                                                    "materially_different",
                                                    "observation_time"
                                            },
                                    true, false),
                            new IndexData("ix2", "ObservedData",
                                    new String[]
                                            {
                                                    campaign_id_ColName,
                                                    advertiser_id_ColName,
                                                    campaign_profile_id_ColName,
                                                    "observation_time"
                                            },
                                    true, false),
                            new IndexData("ix3", "ObservedData",
                                    new String[]
                                            {
                                                    campaign_profile_id_ColName,
                                                    advertiser_id_ColName,
                                                    campaign_id_ColName,
                                                    "observation_time"
                                            },
                                    true, false),
                            new IndexData("ix4", "ObservedData",
                                    new String[]
                                            {
                                                    line_item_id_ColName,
                                                    advertiser_id_ColName,
                                                    line_item_profile_id_ColName,
                                                    "observation_time"
                                            },
                                    false, false),
                            new IndexData("ix5", "ObservedData",
                                    new String[]
                                            {
                            					line_item_profile_id_ColName,
                            					advertiser_id_ColName,
                            					line_item_id_ColName,
                            					"observation_time"
                                            },
                                    false, false),
                            new IndexData("ix6", "ObservedData",
                                    new String[]
                                            {
            									"materially_different",
            									"has_material_differences",
            									advertiser_id_ColName,
            									campaign_id_ColName,
            									"sequence_number",
            									"observation_time"
                                            },
                                    false, false),
                            new IndexData("ix7", "ObservedData",
                                    new String[]
                                            {
                    							"has_material_differences",
                    							advertiser_id_ColName,
                    							campaign_id_ColName,
                    							"observation_time",
                    							line_item_id_ColName
                                            },
                                    false, false),
                            new IndexData("ix8", "ObservedData",
                                    new String[]
                                            {
                            					advertiser_id_ColName,
                            					campaign_id_ColName,
                            					"materially_different"
                                            },
                                    false, false),
                            new IndexData("ix9", "ObservedData",
                                    new String[]
                                            {
                                    			campaign_id_ColName,
            									"sequence_number"
                                            },
                                    false, false),
                            new IndexData("ix10", "ObservedData",
                                    new String[]
                                            {
                                    			advertiser_id_ColName,
                                    			campaign_id_ColName,
                                    			"bidding_policy(4)",
                                    			"observation_time"
                                            },
                                    false, false)
                    };

    static TableData observedDataTable =
            new TableData("ObservedData",
                    observedDataColumns,
                    observedDataIndices);

    //===================================================

    static ColumnData[] advertiserNamesColumns =
            new ColumnData[]
                    {
                            new ColumnData("id",   "int",          Long.class),
                            new ColumnData("name", "varchar(256)",  String.class)
                    };

    static IndexData[] advertiserNamesIndices =
            new IndexData[]
                    {
                            new IndexData("PK", "AdvertiserNames",
                                    new String[]
                                            {
                                                    "id"
                                            },
                                    true, true)
                    };

    static TableData advertiserNamesTable =
            new TableData("AdvertiserNames",
                          advertiserNamesColumns,
                          advertiserNamesIndices);

    //===================================================

    static ColumnData[] lineItemNamesColumns =
            new ColumnData[]
                    {
                            new ColumnData("id",   "int",            Long.class),
                            new ColumnData("name", "varchar(256)", String.class)
                    };

    static IndexData[] lineItemNamesIndices =
            new IndexData[]
                    {
                            new IndexData("PK", "LineItemNames",
                                    new String[]
                                            {
                                                    "id"
                                            },
                                    true, true)
                    };

    static TableData lineItemNamesTable =
            new TableData("LineItemNames",
                          lineItemNamesColumns,
                          lineItemNamesIndices);

    //===================================================

    static ColumnData[] campaignNamesColumns =
            new ColumnData[]
                    {
                            new ColumnData("id",   "int",            Long.class),
                            new ColumnData("name", "varchar(256)", String.class)
                    };

    static IndexData[] campaignNamesIndices =
            new IndexData[]
                    {
                            new IndexData("PK", "CampaignNames",
                                    new String[]
                                            {
                                                    "id"
                                            },
                                    true, true)
                    };

    static TableData campaignNamesTable =
            new TableData("CampaignNames",
                          campaignNamesColumns,
                          campaignNamesIndices);

    //===================================================

    static ColumnData[] bidHistoryColumns =
                  new ColumnData[]
                          {
                                  new ColumnData(advertiser_id_ColName,     "int", Long.class),
                                  new ColumnData(line_item_id_ColName,      "int", Long.class),
                                  new ColumnData(campaign_id_ColName,       "int", Long.class),
                                  new ColumnData("bid_strategy",            "varchar(64)",      String.class),
                                  new ColumnData("bid",                     "double",           Double.class),
                                  new ColumnData("daily_impression_limit",  "bigint NULL DEFAULT NULL", Long.class),
                                  new ColumnData("daily_impression_target", "bigint NULL DEFAULT NULL", Long.class),
                                  new ColumnData("event_time",              "Timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP", Date.class)
                          };

    static IndexData[] bidHistoryIndices =
                  new IndexData[]
                          {
                                  new IndexData("PK", "BidHistory",
                                          new String[]
                                                  {
                                                          advertiser_id_ColName,
                                                          line_item_id_ColName,
                                                          campaign_id_ColName,
                                                          "event_time"
                                                  },
                                          true, true)
                          };

    static TableData bidHistoryTable =
                  new TableData("BidHistory",
                          bidHistoryColumns,
                          bidHistoryIndices);

    //===================================================

    static ColumnData[] dailyImpressionBudgetHistoryColumns =
                  new ColumnData[]
                          {
                                  new ColumnData(advertiser_id_ColName, "int", Long.class),
                                  new ColumnData(line_item_id_ColName,  "int", Long.class),
                                  new ColumnData(campaign_id_ColName,   "int", Long.class),
                                  new ColumnData("daily_budget",        "int", Long.class),
                                  new ColumnData("lifetime_budget",     "int", Long.class),
                                  new ColumnData("event_time",    "Timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP", Date.class)
                          };

    static IndexData[] dailyImpressionBudgetHistoryIndices =
                  new IndexData[]
                          {
                                  new IndexData("PK", "DailyImpressionBudgetHistory",
                                          new String[]
                                                  {
                                                          advertiser_id_ColName,
                                                          line_item_id_ColName,
                                                          campaign_id_ColName,
                                                          "event_time"
                                                  },
                                          true, true)
                          };

    static TableData dailyImpressionBudgetHistoryTable =
                  new TableData("DailyImpressionBudgetHistory",
                          dailyImpressionBudgetHistoryColumns,
                          dailyImpressionBudgetHistoryIndices);

    //===================================================

    static ColumnData[] networkSiteDomainPerformanceColumns =
                  new ColumnData[]
                          {
                                  new ColumnData(advertiser_id_ColName, "int", Long.class),
                                  new ColumnData(line_item_id_ColName,  "int", Long.class),
                                  new ColumnData(campaign_id_ColName,   "int", Long.class),
                                  new ColumnData(day_Colname,     "Timestamp", Date.class),
                                  new ColumnData("site_domain","varchar(256)", String.class),
                                  new ColumnData("imps",             "bigint", Long.class),
                                  new ColumnData("clicks",           "bigint", Long.class),
                                  new ColumnData("media_cost",       "double", Double.class),
                                  new ColumnData("cpm",              "double", Double.class)
                          };

    static IndexData[] networkSiteDomainPerformanceIndices =
                  new IndexData[]
                          {
                                  new IndexData("PK", "Network_Site_Domain_Performance",
                                          new String[]
                                                  {
                                                          advertiser_id_ColName,
                                                          campaign_id_ColName,
                                                          day_Colname,
                                                          "site_domain",
                                                          line_item_id_ColName
                                                  },
                                          true, true)
                          };

    static TableData networkSiteDomainPerformanceTable =
                  new TableData("Network_Site_Domain_Performance",
                          networkSiteDomainPerformanceColumns,
                          networkSiteDomainPerformanceIndices);

    //===================================================

    static ColumnData[] networkAdvertiserFrequencyColumns =
                  new ColumnData[]
                          {
                                  new ColumnData(advertiser_id_ColName, "int", Long.class),
                                  new ColumnData(line_item_id_ColName,  "int", Long.class),
                                  new ColumnData(campaign_id_ColName,   "int", Long.class),
                                  new ColumnData(hour_Colname,    "Timestamp", Date.class),
                                  new ColumnData("creative_frequency_bucket_id", "int", Integer.class),
                                  new ColumnData("creative_frequency_bucket", "varchar(16)", String.class),
                                  new ColumnData("imps",             "bigint", Long.class),
                                  new ColumnData("clicks",           "bigint", Long.class),
                                  new ColumnData("media_cost",       "double", Double.class),
                                  new ColumnData("cost_ecpm",        "double", Double.class)
                          };

    static IndexData[] networkAdvertiserFrequencyIndices =
                  new IndexData[]
                          {
                                  new IndexData("PK", "Network_Advertiser_Frequency",
                                          new String[]
                                                  {
                                                          advertiser_id_ColName,
                                                          campaign_id_ColName,
                                                          hour_Colname,
                                                          "creative_frequency_bucket_id",
                                                          line_item_id_ColName
                                                  },
                                          true, true)
                          };

    static TableData networkAdvertiserFrequencyTable =
                  new TableData("Network_Advertiser_Frequency",
                          networkAdvertiserFrequencyColumns,
                          networkAdvertiserFrequencyIndices);

    //===================================================

    public static ColumnData[] eventsColumns =
                  new ColumnData[]
                          {
                                  new ColumnData(advertiser_id_ColName,   "int",   Long.class),
                                  new ColumnData(campaign_id_ColName,     "int",   Long.class),
                                  new ColumnData("event_type",    "varchar(64)", String.class),
                                  new ColumnData("description",          "text", String.class),
                                  new ColumnData("event_time",    "Timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP", Date.class)
                          };

    static IndexData[] eventsIndices =
                  new IndexData[]
                          {
                                  new IndexData("PK", "Events",
                                          new String[]
                                                  {
                                                          campaign_id_ColName,
                                                          "event_type",
                                                          advertiser_id_ColName,
                                                          "event_time"
                                                  },
                                          true, true)
                          };

    static TableData eventsTable =
                  new TableData("Events", eventsColumns, eventsIndices);

     //===================================================

    static ColumnData[] campaignSettingsColumns =
                  new ColumnData[]
                          {
                                  new ColumnData(advertiser_id_ColName,     "bigint", Long.class),
                                  new ColumnData(line_item_id_ColName,      "bigint", Long.class),
                                  new ColumnData(campaign_id_ColName,       "bigint", Long.class),
                                  new ColumnData("max_bid",                 "double", Double.class),
                                  new ColumnData("daily_budget_imps",       "bigint", Long.class),
                                  new ColumnData("policy", BidderInstruction.policyValuesType(), String.class)
                          };

    static IndexData[] campaignSettingsIndices =
                  new IndexData[]
                          {
                                  new IndexData("PK", "CampaignSettings",
                                          new String[]
                                                  {
                                                          advertiser_id_ColName,
                                                          line_item_id_ColName,
                                                          campaign_id_ColName
                                                  },
                                          true, true)
                          };

    static TableData campaignSettingsTable =
                  new TableData("CampaignSettings",
                          campaignSettingsColumns,
                          campaignSettingsIndices);

     //===================================================

     static TableData[] bidderSchema = new TableData[]
             {
                     historicalDataTable,
                     changeLogTable,
                     observedDataTable,
                     advertiserNamesTable,
                     lineItemNamesTable,
                     campaignNamesTable,
                     historicalDataKnownDatesTable,
                     bidHistoryTable,
                     dailyImpressionBudgetHistoryTable,
                     campaignSettingsTable,
                     eventsTable,
                     networkSiteDomainPerformanceTable,
                     networkAdvertiserFrequencyTable,
                     siteDomainReportKnownDatesTable
             };

    // Just for testing!
    public static int[] intCurve = new int[] { 0, 2, 4, 6, 8, 10, 12 };
    public static int[][] int2DCurve = new int[][]
            {{0, 0}, {1, 2}, {3, 4}, {4, 6}, {5, 8}, {6, 10}, {7, 12}};
    public static int[][] int3DCurve = new int[][]
            {{0,0,0},{0,1,2},{4,3,4},{2,4,6},{7,5,8},{6,6,10},{3,7,12}};
    public static int[][] intSurface = new int[][]
                {{0,0,0,0},
                 {1,2,1,1},
                 {2,3,3,4},
                 {3,4,6,4},
                 {5,8,7,5},
                 {6,8,10,5},
                 {7,12,10,6}};

    @SuppressWarnings("unused")
    public static List<Integer> intListCurve =
            Arrays.asList(0, 2, 4, 6, 8, 10, 12);
    @SuppressWarnings("unused")
    public static List<Number[]> int2DListCurve =
            Arrays.asList(  new Number[] {0, 0},
                            new Number[] {1, 2},
                            new Number[] {3, 4},
                            new Number[] {4, 6},
                            new Number[] {5, 8},
                            new Number[] {6, 10},
                            new Number[] {7, 12});
    @SuppressWarnings("unchecked")
    public static List<List<Integer>> int2DListCurveB =
            Arrays.asList(  Arrays.asList(0, 0),
                            Arrays.asList(1, 2),
                            Arrays.asList(3, 4),
                            Arrays.asList(4, 6),
                            Arrays.asList(5, 8),
                            Arrays.asList(6, 10),
                            Arrays.asList(7, 12));
    @SuppressWarnings("unused")
    public static List<Number[]> int3DListCurve =
            Arrays.asList(  new Number[] {0, 0, 0},
                            new Number[] {0, 1, 2},
                            new Number[] {4, 3, 4},
                            new Number[] {2, 4, 6},
                            new Number[] {7, 5, 8},
                            new Number[] {6, 6, 10},
                            new Number[] {3, 7, 12});
    @SuppressWarnings("unchecked")
    public static List<List<Integer>> int3DListCurveB =
            Arrays.asList(  Arrays.asList(0, 0, 0),
                            Arrays.asList(0, 1, 2),
                            Arrays.asList(4, 3, 4),
                            Arrays.asList(2, 4, 6),
                            Arrays.asList(7, 5, 8),
                            Arrays.asList(6, 6, 10),
                            Arrays.asList(3, 7, 12));

    @SuppressWarnings("unused")
    public static Perspective[] perspectives =
            new Perspective[]
                    {
                            AdvertiserCombinedDataChangesPerspective.PERSPECTIVE,
                            AdvertiserDataChangesPerspective.PERSPECTIVE,
                            AppNexusServicePerspective.PERSPECTIVE,
                            BidderEventsPerspective.PERSPECTIVE,
                            CampaignCompareToNowPerspective.PERSPECTIVE,
                            CampaignDataGraphPerspective.PERSPECTIVE,
                            CampaignDataHistoryPerspective.PERSPECTIVE,
                            CampaignDataHistoryFromLongPerspective.PERSPECTIVE,
                            CombinedDataHistoryPerspective.PERSPECTIVE,
                            CrossAdvertiserTargetingComparisonPerspective.PERSPECTIVE,
                            HistoricalDataGraphPerspective.PERSPECTIVE,
                            ServiceFromLongPerspective.PERSPECTIVE,
                            Simple2DBarPlotPerspective.PERSPECTIVE,
                            Simple2DGraphPerspective.PERSPECTIVE,
                            Simple2DHistogramPlotPerspective.PERSPECTIVE,
                            Simple2DScatterPlotPerspective.PERSPECTIVE,
                            Simple2DStaircasePlotPerspective.PERSPECTIVE,
                            Simple3DBarPlotPerspective.PERSPECTIVE,
                            Simple3DGraphPerspective.PERSPECTIVE,
                            Simple3DScatterPlotPerspective.PERSPECTIVE,
                            Simple3DSurfacePerspective.PERSPECTIVE,
                            TargetingComparisonPerspective.PERSPECTIVE
                    };
    

    private static final String[] classesToForceLoading =
            new String[] { "com.tumri.mediabuying.zini.HTTPListener", null };

    // --------------------------- Constructors ---------------------------

    private Bidder(boolean runningInTomcat, String dbDriver, String dbUser,
                   String dbPassword, String dbName, String dbURL,
                   String dbInformationSchemaUrl, Identity appNexusIdentity,
                   String name, boolean debugP, boolean effectuateBidsP,
                   NewCampaignBidImpositionPolicy bidImpositionPolicy,
                   int appNexusThreadCount)
    {
    	setAppNexusIdentity(appNexusIdentity);
    	this.name = name;
    	this.debugMode = debugP;
    	this.deletePrefetchTempFilesP = !debugP;
    	this.bidderSQLConnectorDriver = dbDriver;
    	this.bidderSQLConnectorUser = dbUser;
    	this.bidderSQLConnectorPassword = dbPassword;
    	this.bidderSQLConnectorDBName = dbName;
    	this.bidderSQLConnectorUrl = dbURL;
    	this.bidderInformationSchemaSQLConnectorUrl = dbInformationSchemaUrl;
        this.appNexusThreadCount = appNexusThreadCount;
    	setEffectuateBids(effectuateBidsP);
        setNewCampaignBidImpositionPolicy(bidImpositionPolicy);
        performanceHistoryDAOImpl = new PerformanceHistoryDAOImpl(this);
    	BindingList bl = BindingList.truth();
    	bl.bind(_X, this);
    	bl.bind(_Name, new StringAtom(this.name));
    	bidderExistenceSentences.instantiate(Manager.MANAGER, bl);
        installHTTPHandlers(null);
        HTTPHandler.setRunningInTomcat(runningInTomcat);
        HTTPHandler.setRootURL(CBO_NAME);
    	if(this.debugMode) startDebugMode(null);
    }

    // --------------------------- Public methods -------------------------

    // ----------------------------- Lifecycle ----------------------------
    
    /** Gets the only instance of the bidder (in this JVM).
     * @return The only instance of the bidder in this JVM.
     * @exception IllegalStateException If the bidder has not been initialized yet.
     */
    public static Bidder getInstance() throws IllegalStateException {
        return getInstance(true);
    }
    
    /** Gets the only instance of the bidder (in this JVM).
     * @param err If true, then err if there's no Bidder known.
     * @return The only instance of the bidder in this JVM.
     * @exception IllegalStateException If the bidder has not been initialized yet.
     */
    public static Bidder getInstance(boolean err) throws IllegalStateException {
    	synchronized(Bidder.class) {
    		if(s_instance == null && err) {
    			throw new IllegalStateException("Bidder has not been initialized.  Call Bidder.initialize() first.");
    		}
            else if(s_instance == null) return null;
    		else return s_instance;
    	}
    }

    /** Initializes this bidder by setting its initial parameters.
     * This method should only be called once.
     * Additional calls to get the instance should should be made from getInstance().
     * @param runningInTomcat A flag that's true if we're rinning in tomcat mode.
     * @param dbDriver The database driver.
     * @param dbUser The user ID used to log in to the DB.
     * @param dbPassword The password used to log in to the DB
     * @param dbName The name of the DB used to store bidder data.
     * @param dbURL The connect info used to connect to the DB.
     * @param dbInformationSchemaURL The connect info used to connect to the information schema on the DB server on which the bidder runs.
     * @param appNexusIdentity The AppNexus Identity object used to mediate AppNexus connectivity
     * @param name The name of this bidder instance
     * @param debugP A flag that's true if we're running in debug mode.
     * @param effectuateBidsP A flag that's true if this bidder instance should really effectuate the bids.
     * @param bidImpositionPolicy The policy of what to do when we do (or don't) impose a bidding policy for newly discovered campaigns.
     * @return The newly created instance.
     * @exception IllegalStateException If the bidder has already been initialized.
     */
    public static Bidder initialize(boolean runningInTomcat, String dbDriver, String dbUser, String dbPassword,
    					   		  String dbName, String dbURL, String dbInformationSchemaURL,
    					   		  Identity appNexusIdentity, String name, boolean debugP,
    					   		  boolean effectuateBidsP, NewCampaignBidImpositionPolicy bidImpositionPolicy,
                                  int appNexusThreadCount)
            throws IllegalStateException {
    	synchronized(Bidder.class) {
    		if(s_instance !=  null) {
    			throw new IllegalStateException("Bidder already initialized.");
    		}
    		s_instance = new Bidder(runningInTomcat, dbDriver, dbUser, dbPassword, dbName, dbURL, dbInformationSchemaURL,
    								appNexusIdentity, name, debugP, effectuateBidsP, bidImpositionPolicy,
                                    appNexusThreadCount);
    	}
    	return s_instance;
    }
    
    /** Destroys the only instance of the bidder and frees up its resources.
     * This method should be called prior to exiting the JVM.
     * Once this has been called the bidder must be initialized again if it is to be started.
     */
    public static void shutdown() {
    	// TODO: Clean up any system resources the bidder might be using...
    	synchronized(Bidder.class) {
    		s_instance = null;
    	}
    }
    
    // ------------------------------ Main and startup stuff ------------------------
    
    /** The main method
     * This creates a new bidder using the command line arguments.
     * @param args The command line arguments.
     */
     public static void main(String[] args) {
        // BasicConfigurator.configure();
        try
        {
            System.setProperty
                   ("catalina.base", "C:\\Tumri\\cbo\\cbo-0.0.0.1\\tomcat5\\");
            InitServlet.earlyInit();
        }
        catch (ServletException e)
        {
            throw Utils.barf(e, null);
        }
        Logger root = Logger.getRootLogger();
        LogManager.resetConfiguration();
        try
        {
            root.addAppender
                    (new FileAppender
                     (new PatternLayout(PatternLayout.TTCC_CONVERSION_PATTERN),
                      "/opt/Tumri/cbo/logs/cbo.log"));
        }
        catch (IOException e)
        {
            throw Utils.barf(e, null);
        }
        startFromCommandLineArgs(false, args);
     }

    // static ThreadLocal<Object> testVar = new ZThreadLocal<Object>();

     static void startFromCommandLineArgs
             (boolean runningInTomcat, String[] args)
     {
         System.out.println
                 ("Starting bidder at " + timeParser.format(new Date()));
         Bidder bidder = initializeFromCommandLineArgs(runningInTomcat, args);
         HTTPHandler.setRunningInTomcat(false);
    	 bidder.initializeFromCommandLineArgsLevel2(args);
         String advertiser = AppNexusUtils.commandLineGet("-advertiser", args);
         String campaign = AppNexusUtils.commandLineGet("-campaign", args);
         String userId = AppNexusUtils.commandLineGet("-overrideuser", args);
         if(userId != null)
             HTTPHandler.setOverrideUserId(userId);
         String email = AppNexusUtils.commandLineGet("-overrideemail", args);
         if(email != null)
             HTTPHandler.setOverrideEmail(email);
         Boolean adminP = AppNexusUtils.commandLineGetBoolean("-overrideadmin", args) ;
         if(adminP != null)
             HTTPHandler.setOverrideIsAdmin(adminP);

         boolean testP = AppNexusUtils.commandLineGetBoolean("-test", args);
         boolean updateP = AppNexusUtils.commandLineGetBoolean("-update", args);

         Set<String> selectedAdvertisers = commaSeparatedStringToSet(advertiser);
         Set<String> selectedCampaigns   = commaSeparatedStringToSet(campaign);
         if(updateP) {
             bidder.runUpdates(selectedAdvertisers, selectedCampaigns);
         }
         else if(testP) {
        	 // bidder.performanceHistoryDAOImpl.runTests();
             // CampaignChangeClassifier.runTests
             // (selectedAdvertisers, selectedCampaigns, bidder);
             // runTests(selectedAdvertisers, selectedCampaigns, bidder);
             // testVar.set("Hello"); try { Thread.sleep(1000000); } catch(InterruptedException e) {}
             System.out.println("No tests to run!");
         } else {
        	 bidder.setAdvertiserMap(null);
        	 try {
                 bidder.setAdvertiserIds(selectedAdvertisers);
                 bidder.setCampaignIds(selectedCampaigns);
        		 bidder.setAdvertiserMap
                        (bidder.processBidInstructions
                                 (bidder.getInputSpreadsheetPath(),
                                  bidder.getOutputSpreadsheetPath(),
                                  selectedAdvertisers,
                                  selectedCampaigns));
        	 } catch(BusyException bbe) {
        		 System.err.println(bbe.getMessage());
        	 }
             //----------------------------------
             AppNexusInterface.debugPrint
                     ("Finished bidder at " + timeParser.format(new Date()) +
                       ": " + bidder.getAdvertiserMap());
         }
     }

    @SuppressWarnings("unused")
    public static JSONObject toJSON(Sexpression s, String key)
            throws org.json.simple.parser.ParseException
    {
        if(s == null || s == Null.nil) return null;
        else if(s instanceof StringAtom) return toJSON(s.unboxString(), key);
        else throw Utils.barf("Not a string as expected.", s);
    }

    public static JSONObject toJSONNullOnError(Sexpression s, String key)
    {
        if(s == null || s == Null.nil) return null;
        else if(s instanceof StringAtom)
            return toJSONNullOnError(s.unboxString(), key);
        else throw Utils.barf("Not a string as expected.", s);
    }

    public static JSONObject toJSONNullOnError(String st, String key)
    {
        try
        {
            return toJSON(st, key);
        }
        catch (org.json.simple.parser.ParseException e)
        {
            System.out.println("JSON parse error in: " + st);
            return null;
        }
    }

    public static JSONObject toJSON(String st, String key)
            throws org.json.simple.parser.ParseException
    {
        if(st == null) return null;
        else
        {
            JSONParser parser = new JSONParser();
            JSONObject res;
            if("".equals(st)) return null;
            else
            {
                res = (JSONObject)parser.parse(st);
                Object res2 = res.get(key);
                if(res2 instanceof JSONObject) res = (JSONObject) res2;
            }
            return res;
        }
    }

    String makeUpdateQuery(Long advertiserId, Long campaignId,
                           Date lastObsTime, SQLConnector connector)
    {
        return "SELECT advertiser_id, observation_time, \n" +
               "       line_item_json, line_item_profile_json, \n" +
               "       campaign_json, campaign_profile_json, observation_time\n" +
               "FROM ObservedData\n" +
               "WHERE observation_time > '" +
                connector.dateToSQL(lastObsTime) + "'\n" +
               "AND   advertiser_id = " + advertiserId + "\n" +
               "AND   campaign_id = "   + campaignId + "\n" +
               "AND   timezone IS NULL\n" +
               "LIMIT 1000;";
    }

    public CampaignData cdFromObservedDataRow
            (Identity ident, QueryContext qctx,
             SQLContext sctx, Sexpression row)
    {
        JSONObject li  = (row.third() == Null.nil
                           ? null
                           : toJSONNullOnError(row.third(), "line-item"));
        JSONObject lip = (row.fourth() == Null.nil
                           ? null
                           : toJSONNullOnError(row.fourth(), "profile"));
        JSONObject ca  = (row.fifth() == Null.nil
                           ? null
                           : toJSONNullOnError(row.fifth(), "campaign"));
        JSONObject cap = (row.sixth() == Null.nil
                           ? null
                           : toJSONNullOnError(row.sixth(), "profile"));
        Sexpression row2 =
            Cons.list
             (row.car(),
              (li  == null ? Null.nil : new LineItemService(li)),
              (lip == null ? Null.nil : new  ProfileService(lip)),
              (ca  == null ? Null.nil : new CampaignService(ca)),
              (cap == null ? Null.nil : new  ProfileService(cap)));
        if(ca != null)
            return new CampaignData(this, ident, null, row2,
                                    null, sctx, qctx, null, null);
        else return null;
    }

    @SuppressWarnings("unused")
    public void runUpdates(Set<String> selectedAdvertisers,
                           Set<String> selectedCampaigns)
    {
        System.out.println("Starting update: " + new Date());
        setDefeatFetchingAppNexusReports(true);
        SQLConnector connector = ensureBidderSQLConnector();
        JSONParser parser = new JSONParser();
        QueryContext qctx = new BasicQueryContext(null, appNexusTheory);
        SQLContext sctx = connector.allocateSQLContext(qctx);
        boolean[] newlineThrownPLoc = new boolean[1];
        int[] countLoc = new int[1];
        class Thunk extends ResultProcessingThunk {

            SQLConnector connector;
            JSONParser parser;
            Bidder bidder;
            Identity ident;
            QueryContext qctx;
            SQLContext sctx;
            Sexpression lastObsTimeLoc;
            boolean[] newlineThrownPLoc;
            int[] countLoc;

            Thunk(Bidder bidder, Identity ident, SQLConnector connector,
                  JSONParser parser, QueryContext qctx, SQLContext sctx,
                  Sexpression lastObsTimeLoc, boolean[] newlineThrownPLoc,
                  int[] countLoc)
            {
                super(connector);
                this.connector = connector;
                this.parser = parser;
                this.bidder = bidder;
                this.ident = ident;
                this.qctx = qctx;
                this.sctx = sctx;
                this.lastObsTimeLoc = lastObsTimeLoc;
                this.newlineThrownPLoc = newlineThrownPLoc;
                this.countLoc = countLoc;
            }

            public void processResultRow(Sexpression row)
            {
                CampaignData cd = bidder.cdFromObservedDataRow
                                                (ident, qctx, sctx, row);
                lastObsTimeLoc.setCar(row.seventh());
                countLoc[0] = countLoc[0] + 1;
                newlineThrownPLoc[0] =
                    ProgressNoter.noteProgress
                          (null, System.out, countLoc[0], newlineThrownPLoc[0],
                           100, false);
                cd.runUpdates(connector, qctx, row.second().unboxDate());
            }
        }
        boolean outerTrace = getTraceSQL();
        setTraceSQL(false);
        String campaignsQuery =
               "SELECT DISTINCT advertiser_id, campaign_id FROM observeddata;";
        connector = ensureBidderSQLConnector();
        Sexpression advertiserCampaignPairs =
                connector.runSQLQuery(campaignsQuery, qctx);
        while(advertiserCampaignPairs != Null.nil)
        {
            int count = 0;
            Sexpression lastObsTimeLoc = Cons.list(new DateAtom(new Date(0)));
            Sexpression pair = advertiserCampaignPairs.car();
            while(true)
            {
                Date startDate = lastObsTimeLoc.car().unboxDate();
                String query = makeUpdateQuery
                        (pair.car().unboxLong(),
                         pair.second().unboxLong(),
                         startDate, connector);
                connector.runSQLQuery
                        (query,
                         new Thunk(this, this.getAppNexusIdentity(),
                                   connector, parser, qctx, sctx,
                                   lastObsTimeLoc, newlineThrownPLoc,
                                   countLoc),
                         qctx);
                if(startDate.getTime() ==
                        lastObsTimeLoc.car().unboxDate().getTime())
                    break;
            }
            advertiserCampaignPairs = advertiserCampaignPairs.cdr();
        }
        bidderSQLConnector.setTraceSQL(outerTrace);
        System.out.println("Finished update: " + new Date());
    }

    @SuppressWarnings("unused")
    public static void runTests(Set<String> selectedAdvertisers,
                                Set<String> selectedCampaigns,
                                Bidder bidder)
    {
        AppNexusTheory appNexusTheory = bidder.ensureAppNexusTheory();
        QueryContext qctx = new BasicQueryContext(null, appNexusTheory);
        Date endsGTETime;

        Long advertiserId = 50926L;
        Agent agent = Integrator.INTEGRATOR;
        boolean selectExpiredCampaigns = false;
        endsGTETime = new Date(1344364083097L);
        Sexpression query =
                bidder.makeQueryForCampaignData
                        (advertiserId, selectExpiredCampaigns,
                         endsGTETime);
        Sexpression findResults =
           Utils.interpretACLWithRetry(agent, query, qctx);
        System.out.println("Results: " + findResults);




        endsGTETime = bidder.getEndsGTETime();
        Sexpression camps =
            bidder.getAdvertiserCampaigns(17191L, Integrator.INTEGRATOR, qctx,
                                          bidder.selectExpiredCampaigns,
                                          endsGTETime);
        System.out.println(camps);
        //Utils.info("Hello", bidder);
        //Utils.barf("This is an error", bidder);
        //Utils.barf(new Error("This is another."), bidder);
        /*
        SynchDateFormat f1 = TimeScaleIterator.getFormatFor(TimeScale.HOURLY, TimeZone.getTimeZone("EST5EDT"));
        SynchDateFormat f2 = TimeScaleIterator.getFormatFor(TimeScale.HOURLY, TimeZone.getTimeZone("EST5EDT"));
        System.out.println("F1: " + f1);
        System.out.println("F2: " + f2);
        System.out.println("F1hc: " + f1.hashCode());
        System.out.println("F2hc: " + f2.hashCode());
        System.out.println("Equals: " + f1.equals(f2));
        */
        /*
        SQLConnector connector = bidder.ensureBidderSQLConnector();
        QueryContext qctx = new BasicQueryContext(null, bidder.appNexusTheory);
        Long advertiserId = 3676L;
        Long campaignId = 250501L;
        Long highWaterMark = AdvertiserData.readHighWaterMark
                (connector, qctx, advertiserId, campaignId);
    */
        /*
        QueryContext qctx =
                new BasicQueryContext(null, bidder.appNexusTheory);
        Date flightStart = bidder.hundredDaysAgo();
        Date flightEnd   = bidder.getCurrentTime();
        bidder.ensureSiteDomainReportPrefetched
               (bidder.getAppNexusIdentity(), flightStart, flightEnd, qctx);
        */
        /*
        File siteCSVFile =
                new File("/tmp/NetworkSiteDomainPerformanceReport.csv");
        File siteTxtFile =
                new File("/tmp/NetworkSiteDomainPerformanceReport.txt");
        Long advertiserId = null;
        Long lineItemId   = null;
        Long campaignId   = null;
        SQLContext sctx;
        SQLConnector connector;
        try
        {
            QueryContext qctx =
                    new BasicQueryContext(null, bidder.appNexusTheory);
            connector = bidder.ensureBidderSQLConnector(false);
            sctx = connector.allocateSQLContext(qctx);
            Date flightStart = dateParser.parse("2012-03-29 00:00:00");
            Date flightEnd   = dateParser.parse("2012-03-30 00:00:00");
            ReportInterval reportInterval = null;
            int columnCount;
            columnCount =
                dumpNetworkSiteDomainPerformanceReportToFile
                    (bidder.getAppNexusIdentity(), flightStart, flightEnd,
                     advertiserId , lineItemId, campaignId, reportInterval,
                     siteCSVFile);
            bidder.saveReportDataToDB
                (siteTxtFile, siteCSVFile,
                 appNexusNetworkSiteDomainPerformanceReportDimensions,
                 columnCount, flightStart, flightEnd,
                 networkSiteDomainPerformanceTable, connector, sctx);
        }
        catch (ParseException e)
        {
            Utils.barf(e, null);
        }
        */
        /*
        File frequencyCSVFile =
                new File("/tmp/NetworkAdvertiserFrequencyReport.csv");
        File frequencyTxtFile =
                new File("/tmp/NetworkAdvertiserFrequencyReport.txt");
        Long advertiserId = 17191L;
        Long lineItemId   = null;
        Long campaignId   = null; // 279499L;
        SQLContext sctx;
        SQLConnector connector;
        QueryContext qctx =
                new BasicQueryContext(null, bidder.appNexusTheory);
        connector = bidder.ensureBidderSQLConnector(false);
        sctx = connector.allocateSQLContext(qctx);
        Date flightStart = null;
        Date flightEnd   = null;
        ReportInterval reportInterval = ReportInterval.last_7_days;
        int columnCount;
        columnCount =
                dumpNetworkAdvertiserFrequencyReportToFile
                        (bidder.getAppNexusIdentity(),
                         advertiserId , lineItemId, campaignId, reportInterval,
                         frequencyCSVFile);
        bidder.saveReportDataToDB
                (frequencyTxtFile, frequencyCSVFile,
                 appNexusNetworkAdvertiserFrequencyReportDimensions,
                 columnCount, flightStart, flightEnd,
                 networkAdvertiserFrequencyTable, connector, sctx);
                 */
    }

    public int fetchAndSaveAdvertiserFrequencyReport
            (Long advertiserId, ReportInterval reportInterval)
    {
        File frequencyTxtFile;
        File frequencyCSVFile;
        SQLContext sctx;
        Date flightStart = null;
        Date flightEnd   = null;
        int rowCount;
        SQLConnector connector = ensureBidderSQLConnector();
        QueryContext qctx = connector.sufficientQueryContextFor();
        try
        {
            frequencyTxtFile = File.createTempFile
                                   (Bidder.FREQUENCY_TEMP_FILE_PREFIX, ".txt");
            frequencyCSVFile = File.createTempFile
                                   (Bidder.FREQUENCY_TEMP_FILE_PREFIX, ".tmp");
        }
        catch(IOException ioe)
        {
            throw Utils.barf("Failed to create temp files for " +
                             "fetching frequency data.", null);
        }
        Long lineItemId   = null;
        Long campaignId   = null;
        sctx = connector.allocateSQLContext(qctx);
        int columnCount;
        columnCount = Bidder.dumpNetworkAdvertiserFrequencyReportToFile
                        (getAppNexusIdentity(),
                         advertiserId , lineItemId, campaignId,
                         reportInterval, frequencyCSVFile);
        rowCount = saveReportDataToDB
                    (frequencyTxtFile, frequencyCSVFile,
                     Bidder.appNexusNetworkAdvertiserFrequencyReportDimensions,
                     columnCount, flightStart, flightEnd,
                     Bidder.networkAdvertiserFrequencyTable, connector, sctx);
        return rowCount;
    }

     public void initializeFromCommandLineArgsLevel2(String[] args) {
         if(args == null)
            throw Utils.barf("Command line args not supplied for Bidder creation.", null);
         String inputPath = AppNexusUtils.commandLineGet("-inputfile", args, true);
         setInputSpreadsheetPath(inputPath);
         String outputPath = AppNexusUtils.commandLineGet("-outputfile", args);
         setOutputSpreadsheetPath(outputPath);
         setOverrideCurrentTime(AppNexusUtils.commandLineGetDate("-now", args));
         setMaxAdvertisers(AppNexusUtils.commandLineGetLong("-maxadvertisers", args));
         setTraceSQL(AppNexusUtils.commandLineGetBoolean("-tracesql", args));
         setMuffleSQLTrace(AppNexusUtils.commandLineGetBoolean("-mufflesqltrace", args));
         setDefeatFetchingAppNexusReports
                 (AppNexusUtils.commandLineGetBoolean
                         ("-defeatfetchingappnexusreports", args));
         setPersistAppNexusP(AppNexusUtils.commandLineGetBoolean
                                ("-persistappnexus", args, false));
         setRestoreAppNexusP(AppNexusUtils.commandLineGetBoolean
                                ("-restoreappnexus", args, false));
         setMongoHost(AppNexusUtils.commandLineGet
                         ("-mongohost", args, DEFAULT_MONGO_DB_HOST));
         setMongoPort(AppNexusUtils.commandLineGetInteger
                         ("-mongoport", args, DEFAULT_MONGO_DB_PORT));
         maybeInitMongo();
     }

    void maybeInitMongo()
    {
        if(persistAppNexusP || restoreAppNexusP)
        {
            String collectionMangulation = "";
            Identity ident = getAppNexusIdentity();
            DB db = TestCore.init(ident, mongoHost, mongoPort);
            ResultFilter filter = null;
            if(persistAppNexusP)
                filter = new ServicePersistenceFilter
                        (db, collectionMangulation);
            else if(restoreAppNexusP)
                filter = new ServiceRecoveryFilter
                        (db, collectionMangulation);
            else {}
            ident.setResultFilter(filter);
        }
    }

     public static Bidder initializeFromCommandLineArgs
             (boolean runningInTomcat, String[] args) {
         if(args == null)
            throw Utils.barf("Command line args not supplied for Bidder creation.", null);

         AppNexusInterface.pointAtProduction();
         
         if(AppNexusUtils.commandLineGetBoolean("-printappnexusdebugs", args, true))
            AppNexusInterface.enableDebugPrinting();
         else 
        	 AppNexusInterface.disableDebugPrinting();
         
         if(AppNexusUtils.commandLineGetBoolean("-printappnexusjson", args, false))
            AppNexusInterface.enableJSONPrinting();
         else 
        	 AppNexusInterface.disableJSONPrinting();
            
         boolean updateAppNexus = AppNexusUtils.commandLineGetBoolean("-updateappnexus", args);
         boolean forceBidUpdating = AppNexusUtils.commandLineGetBoolean("-forcebidupdating", args);
         boolean fetchHistoricalData = AppNexusUtils.commandLineGetBoolean("-fetchhistoricaldata", args, true);
         
         String dbDriver = AppNexusUtils.commandLineGet("-dbdriver", args, "com.mysql.jdbc.Driver");
         String dbUser = AppNexusUtils.commandLineGet("-dbuser", args, "root");
         String dbPassword = AppNexusUtils.commandLineGet("-dbpassword", args, "root");
         String dbName = AppNexusUtils.commandLineGet("-dbname", args, "CBO_DB");
         String dbURL = AppNexusUtils.commandLineGet("-dburl", args, "jdbc:mysql://localhost:3306/" + dbName);
         String dbInformationSchemaUrl = AppNexusUtils.commandLineGet("-informationschemadburl", args,
        	 								 "jdbc:mysql://localhost:3306/" + "information_schema");
         Identity appNexusIdentity = AppNexusUtils.identityFromCommandLine(args);
         String name = AppNexusUtils.commandLineGet("-name", args, "The one and only bidder");
         boolean effectuateBidsP = AppNexusUtils.commandLineGetBoolean("-executebids", args);
         boolean debugP = AppNexusUtils.commandLineGetBoolean("-debug", args);
         int appNexusThreadCount = AppNexusUtils.commandLineGetInteger
                 ("-appnexusthreadcount", args, defaultAppNexusThreadCount);
         String bidImpositionPolicyName =
                 AppNexusUtils.commandLineGet("-bidimpositionpolicy", args);
         NewCampaignBidImpositionPolicy bidImpositionPolicy =
                 (bidImpositionPolicyName == null
                     ? defaultNewCampaignBidImpositionPolicy
                     : NewCampaignBidImpositionPolicy.valueOf
                         (NewCampaignBidImpositionPolicy.class,
                          bidImpositionPolicyName));

         // CampaignData.init(); // enable for testing, if unsure.
         // BidderInstruction.init();
         //----------------------------------
         
         initialize(runningInTomcat, dbDriver, dbUser, dbPassword, dbName,
                    dbURL, dbInformationSchemaUrl, appNexusIdentity, name,
                    debugP, effectuateBidsP, bidImpositionPolicy,
                    appNexusThreadCount);
         
         Bidder bidder = getInstance();
         
         bidder.setUpdateAppNexus(updateAppNexus);
         bidder.setForceBidUpdating(forceBidUpdating);
         bidder.setFetchHistoricalData(fetchHistoricalData);
         //----------------------------------
         
         return bidder;
     }

     /** Parses a comma separated string into a set of unique non-blank strings
      * by breaking the string at the commas and trimming all spaces.
      * @param s The comma-separated string.
      * @return The set of non-blank strings in the string or null if none.
      */
     public static Set<String> commaSeparatedStringToSet(String s) {
    	 Set<String> result = null;
    	 if(s != null) {
    		 result = toSet(s.split(","));
    	 }
    	 return result;
     }
     
     /** Utility to convert a string array to a set, while trimming whitespaces from each string.
      * If a string is blank it is not added to the set.
      * Returns NULL if there are no non-blank strings in the array.
      * @param array The string array.
      * @return A set of the trimmed, non-blank strings in the array or null if no values in the string.
      */
     public static Set<String> toSet(String[] array) {
    	 Set<String> result = null;
    	 if(array != null) {
    		 for(String s: array) {
    			 if(s != null) {
    				 s = s.trim();
    				 if(s.length() > 0) {
    					 if(result == null) {
    						 result = new HashSet<String>();
    					 }
    					 result.add(s);
    				 }
    			 }
    		 }
    	 }
    	 return result;
     }
     
    private void installHTTPHandlers(String[] commandLineArgs)
    {
        forceClassLoading(classesToForceLoading);
        HTTPListener.registerHandler
                ("cbo", HTTPListener.getHandler(DocHTTPHandler.urlName));
        InspectHTTPHandler.noteInterestingObject("Interesting value", this);

        PeriodicPerformanceComparisonHTTPHandler ppch =
                new PeriodicPerformanceComparisonHTTPHandler
                        (null, commandLineArgs, this);
        ppch.registerHandler(null);

        ReportingAdminHTTPHandler rah =
                new ReportingAdminHTTPHandler(null, commandLineArgs);
        rah.registerHandler(null);

        ShowMessagesHTTPHandler smh =
                new ShowMessagesHTTPHandler(null, commandLineArgs);
        smh.registerHandler(null);

        FetchMessagesHTTPHandler fmh =
                new FetchMessagesHTTPHandler(null, commandLineArgs);
        fmh.registerHandler(null);

        RunBidderHTTPHandler rbh =
                new RunBidderHTTPHandler(null, commandLineArgs, this);
        rbh.registerHandler(null);

        BidderGrapherHTTPHandler bh =
                new BidderGrapherHTTPHandler(null, commandLineArgs, this);
        bh.registerHandler(null);

        CampaignTabulationHTTPHandler cth =
                new CampaignTabulationHTTPHandler(null, commandLineArgs, this);
        cth.registerHandler(null);

        BidHistoryHTTPHandler bhh =
                new BidHistoryHTTPHandler(null, commandLineArgs, this);
        bhh.registerHandler(null);

        CampaignJSONCompareHTTPHandler cjch =
                new CampaignJSONCompareHTTPHandler(null, commandLineArgs, this);
        cjch.registerHandler(null);

        StatusHTTPHandler sh =
                new StatusHTTPHandler(null, commandLineArgs, this);
        sh.registerHandler(null);

        ThreadsHTTPHandler th =
                new ThreadsHTTPHandler(null, commandLineArgs);
        th.registerHandler(null);

        BidderStatusHTTPHandler bsh =
                new BidderStatusHTTPHandler(null, commandLineArgs, this);
        bsh.registerHandler(null);

        LogFileListHTTPHandler lflh =
                new LogFileListHTTPHandler(null, commandLineArgs);
        lflh.registerHandler(null);

        LogFileFilterHTTPHandler lffh =
                new LogFileFilterHTTPHandler(null, commandLineArgs);
        lffh.registerHandler(null);

        GetLogFileHTTPHandler glfh =
                new GetLogFileHTTPHandler(null, commandLineArgs);
        glfh.registerHandler(null);

        ListAdvertisersHTTPHandler lah =
                new ListAdvertisersHTTPHandler(null, commandLineArgs, this);
        lah.registerHandler(null);

        BidderDashboardHTTPHandler bdh =
                new BidderDashboardHTTPHandler(null, commandLineArgs, this);
        bdh.registerHandler(null);

        GetFrequencyDataHTTPHandler gfdh =
                new GetFrequencyDataHTTPHandler(null, commandLineArgs, this);
        gfdh.registerHandler(null);

        bidderControlHTTPHandler =
                new BidderControlHTTPHandler(null, commandLineArgs, this);
        bidderControlHTTPHandler.registerHandler(null);
    }

    private void startDebugMode(String[] commandLineArgs)
    {
        this.debugMode = true;
        bidderControlHTTPHandler =
                new BidderControlHTTPHandler(null, commandLineArgs, this);
        bidderControlHTTPHandler.registerHandler(null);
        int port = AppNexusUtils.commandLineGetInteger
                ("-debugport", commandLineArgs, DEFAULT_DEBUG_PORT);
        SQLHTTPHandler.registerIdDecoder
                (new BidderIdDecoder(this, "Bidder ID Decoder"));
        HTTPListener.listen(port, this, commandLineArgs, true);
        HTTPHandler shellHandler =
                HTTPListener.getHandler(ShellHTTPHandler.urlName);
        if(shellHandler != null)
            shellHandler.enable();
        HTTPHandler viewDirHandler =
                HTTPListener.getHandler(ViewDirectoryHTTPHandler.urlName);
        if(viewDirHandler != null)
            viewDirHandler.enable();
    }

    // -------------------------- Status ---------------

    @SuppressWarnings("SynchronizeOnNonFinalField")
    void setStatus(Status theStatus)
    {
        synchronized(statusHistory)
        {
            status = theStatus;
            statusHistory.push(status);
        }
    }

    public Status getStatus()
    {
        return status;
    }


    /** Sets the paused flag.
     * If the bidder is paused it will not do anything when process bid instructions is run.
     * @param paused True to pause the bidder or false to let it run.
     */
    @SuppressWarnings("unused")
    public void setPaused(boolean paused) {
    	m_paused = paused;
    	
    	// Update the display status.
    	StringBuilder buf = new StringBuilder();
    	BidderState bs = getBidderState();
    	if(bs != null) {
    		buf.append(bs.getDisplayName());
    	}
    	if(paused) {
    		buf.append(PAUSED_SUFFIX);
    	}
    	setStatus(new Status(buf.toString()));
    }
    
    /** Determines if this is paused. 
     * If the bidder is paused it will not do anything when process bid instructions is run.
     * @return True if the bidder if paused or false if not.
     */
    public boolean isPaused() {
        return m_paused;
    }
    
    /** Gets the last time the bidder started processing bids.
     * @return The last time the bidder started processing bids.
     */
    public Date getLastRunTime() {
    	return m_lastRunTime;
    }
    
    /** Sets the last time the bidder started processing bids.
     * @param lastRunTime The last time the bidder started processing bids.
     */
    private void setLastRunTime(Date lastRunTime) {
    	m_lastRunTime = lastRunTime;
    }
    
    /** Sets a flag indicating if this is processing bid instructions.
     * This most only be called while synchronized on this instance.
     * @param processing True if this is processing bid instructions.
     */
    private void setProcessingBidInstructions(boolean processing) {
    	m_processingBidInstructions = processing;
    }
    
    /** Determines if the bidder is processing bid instructions.
     * @return True if the bidder is processing bid instructions.
     */
    public boolean isProcessingBidInstructions() {
    	return m_processingBidInstructions;
    }
    
    /** Gets the current state of the bidder.
     * @return The bidder state.
     */
    public BidderState getBidderState() {
    	return m_bidderState;
    }
    
    /** Adds a listener that is informed after the
     * bidder state changes.
     * Does nothing if the listener is null
     * or if the listener has already been added.
     * @param l The listener to add.
     * @return True if the listener was added or false if not.
     */
    @SuppressWarnings({"unused", "SynchronizeOnNonFinalField"})
    public boolean addBidderStateChangeListener(BidderStateChangeListener l) {
    	boolean added = false;
    	if(l != null) {
    		synchronized(m_bidderStateChangeListeners) {
    			added = m_bidderStateChangeListeners.add(l);
    		}
    	}
    	return added;
    }
    
    /** Removes a bidder state change listener.
     * Does nothing if the listener to remove is null
     * or if the listener was not registered.
     * @param l The listener to remove.
     * @return True if the listener was removed or false if not.
     */
    @SuppressWarnings({"unused", "SynchronizeOnNonFinalField"})
    public boolean removeBidderStateChangeListener(BidderStateChangeListener l) {
    	boolean removed = false;
    	if(l != null) {
    		synchronized(m_bidderStateChangeListeners) {
    			removed = m_bidderStateChangeListeners.remove(l);
    		}
    	}
    	return removed;
    }

    /** Fires a bidder state changed event 
     * to all bidder state change listeners
     * registered on this class.
     * @param e The bidder state changed event.
     */
    protected void fireBidderStateChangedEvent(BidderStateChangedEvent e) {
    	// Fire events to all listeners.
    	for(BidderStateChangeListener l: getBidderStateChangeListeners()) {
    		l.bidderStateChanged(e);
    	}
    }
    
    @SuppressWarnings("unchecked")
    /** Gets the bidder state change listeners that are registered on this class.
     * Returns a collection of the listeners that were registered at the call
     * that will not change while firing events.
     * @return The bidder state change listeners that are registered.
     */
    protected Set<BidderStateChangeListener> getBidderStateChangeListeners() {
    	return (Set<BidderStateChangeListener>)(m_bidderStateChangeListeners.clone());
    }
    
    /** Sets the bidder state.
     * Fires a bidder state change event 
     * to all bidder state change listeners if the state changes.
     * @param bs The bidder state
     */
    void setBidderState(BidderState bs) {
    	BidderStateChangedEvent e = null;
    	synchronized(this) {
    		if(setBidderStateInternal(bs)) {
    			e = new BidderStateChangedEvent(this, bs);
    		}
    	}
    	if(e != null) {
    		setStatus(new Status(bs.getDisplayName(), 0));
    		fireBidderStateChangedEvent(e); 
    	}
    }
    
    static
    {
        Utils.addLogElideMethod(Bidder.class, "setBidderState");
    }

    /** Sets the bidder state.
     * Does not fire a bidder state change event.
     * All users should call setBidderState() to 
     * ensure the state change event is fired.
     * This method must be called while synchronized on the monitor
     * that enforces consistent bidder state.
     * @param bs The bidder state
     * @return True if the bidder state changed or false if not
     */
    private boolean setBidderStateInternal(BidderState bs) {
    	boolean stateChanged = false;
    	if(bs != getBidderState()) {
    		m_bidderState = bs;
    		stateChanged = true;
    	}
    	return stateChanged;
    }
    
    // -------------------------- Debug configuration ------------------
    
    /** Sets the debug mode flag.
     * @param debugMode The debug mode flag.
     */
    @SuppressWarnings("unused")
    public void setDebugMode(boolean debugMode)
    {
        this.debugMode = debugMode;
    }
    
    /** Gets the debug mode flag.
     * @return The debug mode flag.
     */
    public boolean getDebugMode() {
        return debugMode;
    }

    /** Sets a flag indicating if SQL statements should be "traced" for debugging.
     * @param traceSQL True if SQL statements should be traced or false if not.
     */
    public void setTraceSQL(boolean traceSQL) {
    	m_traceSQL = traceSQL;
        // This was removed because it is a side effect.
    	// ensureBidderSQLConnector().setTraceSQL(traceSQL);
    }
    
    /** Gets a flag indicating if SQL statements should be "traced" for debugging.
     * @return True if SQL statements should be traced or false if not.
     */
    public boolean getTraceSQL() {
    	return m_traceSQL;
        // This was removed because it is a side effect.
    	// return ensureBidderSQLConnector().getTraceSQL();
    }

    /** Sets a flag indicating if SQL results should be muffed when tracing is enabled.
     * @param muffleSQLTrace True if SQL results should be muffled or false if not.
     */
    public void setMuffleSQLTrace(boolean muffleSQLTrace) {
    	m_muffleSQLTrace = muffleSQLTrace;
    }

    /** Gets a flag indicating if SQL results should be muffled when SQL tracing has been set.
     * @return True if SQL results should be muffled or false if not.
     */
    public boolean getMuffleSQLTrace() {
    	return m_muffleSQLTrace;
    }

    @SuppressWarnings("unused")
    public void setTenureErrors(boolean to) {
        Utils.setTenureErrors(to);
    }
    
    @SuppressWarnings("unused")
    public boolean getTenureErrors() {
        return Utils.getTenureErrors();
    }

    public void setPrintAppNexusDebugs(boolean to) {
        if(to) AppNexusInterface.enableDebugPrinting();
        else AppNexusInterface.disableDebugPrinting();
    }

    @SuppressWarnings("unused")
    public boolean getPrintAppNexusDebugs() {
        return AppNexusInterface.getPrintDebugs();
    }

    public void setPrintAppNexusJSON(boolean to) {
        if(to) AppNexusInterface.enableJSONPrinting();
        else AppNexusInterface.disableJSONPrinting();
    }
    
    @SuppressWarnings("unused")
    public boolean getPrintAppNexusJSON() {
        return AppNexusInterface.getPrintJSON();
    }
    
    /** Sets a flag indicating if bids should be passed to AppNexus.
     * This is one of the protection mechanisms against accidentally performing a bid while debugging.
     * @param b True to send the bids or false not to.
     */
    public void setEffectuateBids(boolean b) {
        m_effectuateBidsP = b;
    }
    
    /** Gets a flag indicating if bids should be passed to AppNexus.
     * This is one of the protection mechanisms against accidentally performing a bid while debugging.
     * @return True to if the bids are sent to AppNexus or false if not.
     */
    public boolean getEffectuateBids() {
        return m_effectuateBidsP;
    }

    /** Sets a flag indicating if only read interactions should be made with AppNexus.
     * This is one of the protection mechanisms against accidentally performing a bid while debugging.
     * @param b True to ensure that all AppNexus methods are read only.
     */
    public void setAppNexusReadOnly(boolean b) {
        getAppNexusIdentity().setReadOnly(b);
    }
    
    /** Gets a flag indicating if only read interactions should be made with AppNexus.
     * This is one of the protection mechanisms against accidentally performing a bid while debugging.
     * @return True to ensure that all AppNexus methods are read only.
     */
    @SuppressWarnings("unused")
    public boolean getAppNexusReadOnly() {
        return getAppNexusIdentity().isReadOnly();
    }

    public void setUpdateAppNexus(boolean updateAppNexus) {
        this.updateAppNexus = updateAppNexus;
    }
    
    public boolean getUpdateAppNexus() {
        return updateAppNexus;
    }

    public void setForceBidUpdating(boolean forceBidUpdating) {
        this.forceBidUpdating = forceBidUpdating;
    }

    public boolean getForceBidUpdating() {
        return forceBidUpdating;
    }

    public void setFetchHistoricalData(boolean fetchHistoricalData) {
        this.fetchHistoricalData = fetchHistoricalData;
    }

    @SuppressWarnings("unused")
    public boolean getFetchHistoricalData() {
        return fetchHistoricalData;
    }

    /** Sets the time to use to override the current time for the purposes
     * of reproducing system behaviour.  When null, the value of new Date( ) is
     * used.
     * @param overrideCurrentTime The time to use for processing instead
     * of new Date( ).
     */
    public void setOverrideCurrentTime(Date overrideCurrentTime) {
    	m_overrideCurrentTime = overrideCurrentTime;
    }
    
    /** Gets the time to use to override the true current time when processing.
     * @return The override time.
     */
    public Date getOverrideCurrentTime() {
    	return m_overrideCurrentTime;
    }

    /** Gets the time to use when processing.
     * @return The time to use.
     */
    public Date getCurrentTime()
    {
        if(getOverrideCurrentTime() == null) return new Date();
        else return getOverrideCurrentTime();
    }
    
    /** Sets the maximum number of advertisers to consider.
     * If set to null all advertisers will be considered.
     * This is used mainly for debugging.
     * @param maxAdvertisers The maximum number of advertisers to consider or null if none.
     */
    public void setMaxAdvertisers(Long maxAdvertisers) {
    	m_maxAdvertisers = maxAdvertisers;
    }

    /** Gets the maximum number of advertisers to consider.
     * If this is set the maximum number of advertisers to consider is limited to this value.
     * @return The maximum number of advertisers to consider.
     */
    public Long getMaxAdvertisers() {
    	return m_maxAdvertisers;
    }

    /** Sets the set of advertiser ids to which to restrict the processing when the bidder is
     * run unless some other set has been specified in the call to processBidRequests().
     * This is mainly used to reduce the time spent fetching data each time the 
     * bidder is run.
     * If the set is NULL all advertisers will be considered.
     * @param advertiserIds The advertisers to be considered or null to consider all advertisers.
     */
    public void setAdvertiserIds(Set<String> advertiserIds) {
    	m_advertiserIds = advertiserIds;
    }

    /** Gets the set of advertiser ids to which to restrict the processing when the bidder is
     * run unless some other set has been specified in the call to processBidRequests().
     * This is mainly used to reduce the time spent fetching data each time the 
     * bidder is run.
     * If the set is NULL all advertisers will be considered.
     * @return The advertisers to be considered or null to consider all advertisers.
     */
    public Set<String> getAdvertiserIds() {
    	return m_advertiserIds;
    }
    
    /** Sets the set of campaign ids to which to restrict the processing when the bidder is
     * run unless some other set has been specified in the call to processBidRequests().
     * This is mainly used to reduce the time spent fetching data each time the 
     * bidder is run.
     * If the set is NULL all campaigns will be considered.
     * @param campaignIds The campaigns to be considered or null to consider all campaigns.
     */
    public void setCampaignIds(Set<String> campaignIds) {
    	m_campaignIds = campaignIds;
    }

    /** Gets the set of campaign ids to which to restrict the processing when the bidder is
     * run unless some other set has been specified in the call to processBidRequests().
     * This is mainly used to reduce the time spent fetching data each time the 
     * bidder is run.
     * If the set is NULL all campaigns will be considered.
     * @return The campaigns to be considered or null to consider all campaigna.
     */
    public Set<String> getCampaignIds() {
    	return m_campaignIds;
    }
    
    /** Sets the path to the input spreadsheet.
     * @param path The path to the input spreadsheet.
     */
    public void setInputSpreadsheetPath(String path) {
    	m_inputSpreadsheetPath = path;
    }
    
    /** Gets the path to the input spreadsheet.
     * @return The path to the input spreadsheet.
     */
    public String getInputSpreadsheetPath() {
    	return m_inputSpreadsheetPath;
    }
 
    /** Sets the path to the output spreadsheet.
     * @param path The path to the output spreadsheet.
     */
    public void setOutputSpreadsheetPath(String path) {
    	m_outputSpreadsheetPath = path;
    }
    
    /** Gets the path to the output spreadsheet.
     * @return The path to the output spreadsheet.
     */
    public String getOutputSpreadsheetPath() {
    	return m_outputSpreadsheetPath;
    }

    @SuppressWarnings("unused")
    public void backupSpreadsheet()
    {
        String path = getOutputSpreadsheetPath();
        backupSpreadsheet(path);
    }

    public void backupSpreadsheet(String path)
    {
        Integer backupNum = Utils.backupFile(path);
        if(backupNum != null)
            Utils.logThisPoint(Level.INFO,
                               "Backed up spreadsheet " + path +
                               " to version " + backupNum);
    }

    // -------------------------- Control parameters ---------------

    /** Sets the AppNexus identity.
     * This can only be called during construction.
     * @param appNexusIdentity The AppNexus identity.
     */
    private void setAppNexusIdentity(Identity appNexusIdentity) {
        m_appNexusIdentity = appNexusIdentity;
    }

    /** Gets the AppNexus identity.
     * @return appNexusIdentity The AppNexus identity.
     */
    public Identity getAppNexusIdentity() {
        return m_appNexusIdentity;
    }
    
    // ------------------------ End of control parameters -------------
    
    public PerformanceHistoryDAO getPerformanceHistoryDAO() {
    	return performanceHistoryDAOImpl;
    }
    
    public boolean outputConsoleWidgets(Writer stream, OutputStream os,
    		                            Map<String, String> httpParams) throws IOException
    {
    	// Nothing to do right now!
    	return false;
    }

    public Object objectForId(Long id)
    {
    	if(advertiserMap == null) return null;
    	else
    	{
    		AdvertiserData a = advertiserMap.get(id);
    		if(a != null) return a;
    		else
    		{
    			for(AdvertiserData ad: advertiserMap.values())
    			{
    				Object obj = ad.objectForId(id);
    				if(obj != null) return obj;
    			}
    			return null;
    		}
    	}
    }
    
    public Map<Long, AdvertiserData> getAdvertiserMap()
    {
        return advertiserMap;
    }

    public void setAdvertiserMap(Map<Long, AdvertiserData> map)
    {
        advertiserMap = map;
    }

    public void br(PrintWriter stream) throws IOException
    {
        stream.append("<BR>\n");
    }

    public static final String IDLE_STATE = "Idle";
    public static final String UNINITIALISED_STATE = "Uninitialised";
    public static final String ACTIVE_STATE = "Active";
    public static int STATUS_FRAME_REFRESH_SECONDS = 4;
    public static final String BIDDER_FRAME = "BidderFrame";


    @SuppressWarnings("SynchronizeOnNonFinalField")
    public boolean reportStatus(Writer stream, OutputStream os,
                                Map<String, String> httpParams)
            throws IOException
    {
        boolean admin = HTTPHandler.isAdministratorUser(httpParams);
        if(admin)
        {
            stream.append("\n<H2>Bidder: ");
            stream.append(HTMLifier.anchorIfReasonable
                           (this, name, "../INSPECT/", Manager.MANAGER, null,
                            admin, null));
            stream.append("</H2>");
        }
        stream.append("\n<SCRIPT LANGUAGE=\"JavaScript\">");
        stream.append("\n  function checkButtonEnDisabled() {");
        stream.append("\n    var fr = document.getElementById('" +
                                      BIDDER_FRAME + "');");
        stream.append("\n    var frm = fr.contentDocument.getElementById('" +
                              BidderStatusHTTPHandler.STATE_FORM_NAME + "');");
        stream.append("\n    if(frm != null && frm." +
                                 BidderStatusHTTPHandler.STATE_ELEMENT_NAME +
                                 ".value == '" + IDLE_STATE + "' ||" +
                                "frm." +
                                 BidderStatusHTTPHandler.STATE_ELEMENT_NAME +
                                 ".value == '" + UNINITIALISED_STATE + "')");
        stream.append("\n        BidderForm." +
                                 RunBidderHTTPHandler.RUN_BIDDER_PARAM +
                                 ".disabled=false;");
        stream.append("\n    else BidderForm." +
                                 RunBidderHTTPHandler.RUN_BIDDER_PARAM +
                                 ".disabled=true;");
        stream.append("\n  }");
        stream.append("\n  setInterval(checkButtonEnDisabled, ");
        stream.append(Integer.toString(STATUS_FRAME_REFRESH_SECONDS * 1000));
        stream.append(");");
        stream.append("\n</SCRIPT>");
        stream.append("\n<TABLE>\n  <TR><TD>Status:</TD>" +
                      "<TD VALIGN=\"MIDDLE\" NOWRAP=NOWRAP>");
        stream.append("\n<FORM NAME=\"BidderForm\" METHOD=\"POST\" " +
                              "ACTION=\"../" + RunBidderHTTPHandler.URL_NAME +
                              "/" + RunBidderHTTPHandler.URL_NAME + "?\">");
        if(admin)
            stream.append("\n  <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                                RunBidderHTTPHandler.RUN_BIDDER_PARAM + "\" " +
                                "VALUE=\"Run Bidder Now\" disabled>&nbsp;");
        stream.append("<IFRAME id=\"" + BIDDER_FRAME
                      + "\" NAME=\"" + BIDDER_FRAME + "\""
                      + " HEIGHT=\"20\" WIDTH=\"1000\" FRAMEBORDER=\"0\""
                      + " SCROLLING=\"no\" MARGINHEIGHT=\"0\""
                      + " MARGINWIDTH=\"0\" SRC=\"../"
                      + BidderStatusHTTPHandler.URL_NAME + "\"></IFRAME>");
        stream.append("</FORM>");
        stream.append("</TD></TR>");
        stream.append("\n<TABLE>\n  <TR><TD>Last run:</TD>" +
                      "<TD NOWRAP=NOWRAP>");
        Date lastRun = getLastRunTime();
        if(lastRun != null)
            stream.append(dateParser.format(lastRun));
        stream.append("</TD></TR>");
        stream.append("\n  <TR><TD>Server health:</TD>" +
                      "<TD NOWRAP=NOWRAP>");
        if(HTTPHandler.getRunningInTomcat())
        {
            boolean isHealthy = HealthCheckServlet.isHealthy();
            if(isHealthy) stream.append("Healthy");
            else stream.append("<B>SICK!</B>");
        }
        else stream.append("Not running in Tomcat");
        stream.append("</TD></TR>");
        //----------------------------
        if(admin)
        {
            stream.append("<TR><TD VALIGN=\"TOP\">Control:</TD><TD>");
            if(bidderControlHTTPHandler != null)
                bidderControlHTTPHandler.outputForm(stream);
            stream.append("</TD></TR>");
        }
        stream.append
           ("\n  <TR><TD VALIGN=\"TOP\">Status history:</TD><TD>");
        stream.append
                ("<TEXTAREA READONLY WRAP=\"off\" ROWS=\"10\" COLS=\"80\">");
        synchronized(statusHistory)
        {
            for(Status s: statusHistory)
            {
                stream.append(HTTPHandler.htmlify
                                    (s.printedRepresentation(true)));
                stream.append("\n");
            }
        }
        stream.append("</TEXTAREA></TD></TR></TABLE>");
        return true;
    }

    public static String ensureLength(Object o, Integer length)
    {
        String s = (o == null ? "" : o.toString());
        s = (length != null && s.length() > length
                ? s.substring(0, length)
                : s);
        return s;
    }

    public static String serviceToJson(Object s)
    {
        if(s == null) return "";
        else if(s instanceof String) return (String) s;
        else if(s instanceof AbstractAppNexusService)
            return ((AbstractAppNexusService)s).serviceToJSON().toString();
        else return s.toString();
    }

    public static void logChange(Long advertiserId, Long campaignId,
                                 Long campaignProfileId, String changeType, Object p0,
                                 Object p1, Object p2, Object p3, Object campaign,
                                 Object profile, QueryContext qctx)
    {
        String campaignJson = serviceToJson(campaign);
        String profileJson  = serviceToJson(profile);
        changeType = ensureLength(changeType, 64);
        String s0 = "";
        String s1 = "";
        String s2 = "";
        String s3 = "";
        Number n0 = 0.0;
        Number n1 = 0.0;
        Number n2 = 0.0;
        Number n3 = 0.0;
        if(p0 instanceof Number) n0 = (Number) p0;
        else s0 = ensureLength(p0, 256);
        if(p1 instanceof Number) n1 = (Number) p1;
        else s1 = ensureLength(p1, 256);
        if(p2 instanceof Number) n2 = (Number) p2;
        else s2 = ensureLength(p2, 256);
        if(p3 instanceof Number) n3 = (Number) p3;
        else s3 = ensureLength(p3, 256);
        campaignJson = ensureLength(campaignJson, null);
        profileJson = ensureLength(profileJson, null);
        Sexpression updateQuery =
                Cons.list(Syms.Tell,
                          Sexpression.boxList
                            (Syms.intern("CBO_DB.CHANGELOG"),
                             advertiserId, campaignId, campaignProfileId,
                             changeType,
                             s0, s1, s2, s3,
                             n0, n1, n2, n3,
                             campaignJson, profileJson, Syms.NULL));
        Utils.interpretACL(Integrator.INTEGRATOR,
                           Cons.list(Syms.Request, updateQuery, Null.nil),
                           qctx);
    }

    public boolean lessp(Sexpression x)
    {
        return x instanceof Bidder &&
               this.toString().compareTo(x.toString()) < 0;
    }

    public PPrintMotion pprint(boolean pprintP, int indent, boolean quote,
                               int level, int length, PPrintMotion motion)
    {
        return motion.anotherItem(this, toString());
    }

    /** Gets a string representation of this class for debugging.
     * @return A string representation of this class for debugging.
     */
    public String toString() {
        return "#<" + AppNexusUtils.afterDot(this.getClass().getName()) + " " +
                (name == null ? "Unnamed" : name) + " " +
                Integer.toHexString(hashCode()) + ">";
    }

    /** Gets the bidder SQL connector.
     * Creates on if necessary.
     * Note that the traceSQL flag is set to whatever it is set to on this bidder instance.
     * @return The bidder SQL connector.
     */
    SQLConnector getAgent() {
        return ensureBidderSQLConnector();
    }

    /** Gets the bidder SQL connector.
     * Creates on if necessary.
     * Note that the traceSQL flag is set to whatever it is set to on this bidder instance.
     * @return The bidder SQL connector.
     */
    public synchronized SQLConnector ensureBidderSQLConnector() {
        return ensureBidderSQLConnector(false);
    }

    private static String bidderSQLConnectorTimeZone = null; // Utils.UTC_TIMEZONE

    /** Gets the bidder SQL connector.
     * Creates on if necessary.
     * Note that the traceSQL flag is set to whatever it is set to on this bidder instance.
     * @param forceSchemaP ????
     * @return The bidder SQL connector.
     */
    private synchronized SQLConnector ensureBidderSQLConnector(boolean forceSchemaP)
    {
        if(bidderSQLConnector == null)
        {
            Constant agentName =
                    Syms.intern(bidderSQLConnectorDBName.toUpperCase());
            Agent existing = Sexpression.referent(agentName);
            if(existing == null)
            {
                bidderSQLConnector =
                    new MySQLConnector
                          (agentName, bidderSQLConnectorDriver,
                           bidderSQLConnectorUser, bidderSQLConnectorPassword,
                           bidderSQLConnectorUrl, null,
                           bidderSQLConnectorTimeZone);
                // We've just created one, so this is our first time around.
                // We should do any start-time initialisations here.
                QueryContext qctx = new BasicQueryContext(null, appNexusTheory);
                bidderSQLConnector.purgeTempTables(qctx);
            }
            else bidderSQLConnector = (SQLConnector)existing;
            SQLSchemaManager schema =
                    new SQLSchemaManager(bidderSQLConnector, bidderSchema);
            bidderSQLConnector.setSchemaManager(schema, forceSchemaP);
        }
        // Reset the trace setting to our current desires.
        bidderSQLConnector.setTraceSQL(getTraceSQL());
        ResultCollectingThunk.setDefaultMuffleSQLTrace(getMuffleSQLTrace());
        return bidderSQLConnector;
    }

    synchronized SQLConnector ensureBidderInformationSchemaSQLConnector()
    {
        if(bidderInformationSchemaSQLConnector == null &&
                bidderInformationSchemaSQLConnectorUrl != null)
        {
            Constant agentName = Syms.intern("INFORMATION_SCHEMA");
            Agent existing = Sexpression.referent(agentName);
            if(existing == null)
                bidderInformationSchemaSQLConnector =
                    new MySQLConnector
                           (agentName, bidderSQLConnectorDriver,
                            bidderSQLConnectorUser, bidderSQLConnectorPassword,
                            bidderInformationSchemaSQLConnectorUrl,
                            null, bidderSQLConnectorTimeZone);
            else bidderInformationSchemaSQLConnector = (SQLConnector)existing;
        }
        return bidderInformationSchemaSQLConnector;
    }

    static Sexpression toIdList(String[] ids)
    {
        SexpLoc loc = new SexpLoc();
        for(String s: ids)
        {
            Long l = Long.parseLong(s);
            loc.collect(new NumberAtom(l));
        }
        return loc.getSexp();
    }

    static Sexpression makeQueryForAdvertisers(String[] selectedAdvertiserIds)
    {
        BindingList bl = BindingList.truth();
        if(selectedAdvertiserIds != null && selectedAdvertiserIds.length > 0)
            bl.bind("@AdvertiserIds", toIdList(selectedAdvertiserIds));
        else bl.bind("@AdvertiserIds",
                     Cons.list(SequenceVariable.anonymousVariable));
        List<Sexpression> instantiated = getAdvertisersQuery.instantiate(bl);
        return instantiated.get(0);
    }

    Sexpression makeQueryForCampaignData
            (Long advertiserId, boolean selectExpiredCampaigns,
             Date endsGTETime)
    {
        BindingList bl = BindingList.truth();
        bl.bind("?AdvertiserId", new NumberAtom(advertiserId));
        // End time constraint can be either Now, or unconstrained.
        bl.bind("?Now", (selectExpiredCampaigns
                            ? IndividualVariable.anonymousVariable
                            : new DateAtom(endsGTETime)));
        List<Sexpression> instantiated = getCampaignDataQuery.instantiate(bl);
        return instantiated.get(0);
    }

    static Map<String, BidderInstruction> readInstructionsFile
            (String inputPath, String sheetName, ExcelFileSchema schema)
    {
        HSSFWorkbook workbook = ExcelColSchema.readInWorkbook(inputPath);
        Map<String, BidderInstruction> res =
                new HashMap<String, BidderInstruction>();
        HSSFSheet worksheet =
                (sheetName == null
                        ? workbook.getSheetAt(0)
                        : workbook.getSheet(sheetName));
        Integer columnIndex = 0;
        Iterator<Row> rowIterator = worksheet.rowIterator();
        Map<String, Integer> columnMap = new HashMap<String, Integer>();
        if(!rowIterator.hasNext())
            throw Utils.barf("Spreadsheet " + inputPath + " is empty", null);
        Row headers = rowIterator.next();
        Iterator<Cell> cellIterator = headers.cellIterator();
        while(cellIterator.hasNext())
        {
            Cell c = cellIterator.next();
            columnMap.put(c.getStringCellValue(), columnIndex);
            columnMap.put(c.getStringCellValue().toUpperCase(), columnIndex);
            columnIndex = columnIndex + 1;
        }
        while(rowIterator.hasNext())
        {
            Row row = rowIterator.next();
            BidderInstruction bi =
                    new BidderInstruction(columnMap, row, schema);
            res.put(bi.getKey(), bi);
        }
        return res;
    }

    void rememberServices(Sexpression x, QueryContext qctx)
    {
        if(x instanceof Cons)
        {
            rememberServices(x.car(), qctx);
            rememberServices(x.cdr(), qctx);
        }
        else if(x == Null.nil) {}
        else if(x instanceof AbstractAppNexusService)
            qctx.cacheService((AbstractAppNexusService) x);
        else {}
    }

    public  String currentTimeString()
    {
        return timeParser.format(getCurrentTime());
    }

    Sexpression getAdvertiserCampaigns
            (Long advertiserId, Agent agent, QueryContext qctx,
             boolean selectExpiredCampaigns, Date endsGTETime)
    {
        setStatus(new Status("Fetching advertiser data for " + advertiserId));
        Sexpression query =
                makeQueryForCampaignData(advertiserId, selectExpiredCampaigns,
                                         endsGTETime);
        AppNexusInterface.debugPrint
                ("Fetching advertiser data for " + advertiserId + ".  (" +
                 currentTimeString() + ")");
        Sexpression findResults =
                Utils.interpretACLWithRetry(agent, query, qctx);
            Sexpression temp = Null.nil;
            while(findResults != Null.nil)
            {
                Sexpression row = findResults.car();
                Sexpression tempRow = row;
                while(tempRow != Null.nil)
                {
                    Sexpression col = tempRow.car();
                    if(col instanceof CampaignService)
                    {
                        temp = new Cons(row, temp);
                        break;
                    }
                    tempRow = tempRow.cdr();
                }
                findResults = findResults.cdr();
            }
            findResults = Cons.reverse(temp);
        return findResults;
    }

    AdvertiserData getCampaignDataForAdvertiser
            (IndexCounter index, Identity appNexusIdentity,
             AdvertiserService advertiser, Set<String> selectedCampaigns,
             Agent agent, QueryContext qctx,
             ReportNetworkAnalyticsService[] networkReportData,
             Map<String, BidderInstruction> instructions, SQLContext sctx,
             boolean selectExpiredCampaigns, Date endsGTETime,
             Map<Long, AdvertiserData> currentAdvertiserDataMap,
             Date instructionsDate)
    {
        Long advertiserId = advertiser.getId();
        setStatus(new Status("Fetching advertiser data for " + advertiserId,
                             false));
        Sexpression query =
                makeQueryForCampaignData(advertiserId, selectExpiredCampaigns,
                                         endsGTETime);
        AppNexusInterface.debugPrint
                (AppNexusUtils.intToString(index.getIndex(), 3) +
                 " Fetching advertiser data for " + advertiserId + ".  (" +
                 currentTimeString() + ")");
        Sexpression findResults =
                Utils.interpretACLWithRetry(agent, query, qctx);
        if(selectedCampaigns != null)
        {
            Sexpression temp = Null.nil;
            while(findResults != Null.nil)
            {
                Sexpression row = findResults.car();
                Sexpression tempRow = row;
                while(tempRow != Null.nil)
                {
                    Sexpression col = tempRow.car();
                    if(col instanceof CampaignService &&
                            selectedCampaigns.contains
                                  (((CampaignService)col).getId().toString()))
                    {
                        temp = new Cons(row, temp);
                        break;
                    }
                    tempRow = tempRow.cdr();
                }
                findResults = findResults.cdr();
            }
            findResults = Cons.reverse(temp);
        }
        rememberServices(findResults, qctx);
        return new AdvertiserData
                        (appNexusIdentity, this, advertiserId, findResults,
                         instructions, networkReportData, sctx, qctx,
                         currentAdvertiserDataMap, instructionsDate);
    }

    public static int dumpReportToFile
            (Class reportClass, Identity appNexusIdentity, Date flightStart,
             Date flightEnd, Long advertiserId, Long lineItemId,
             Long campaignId, ReportInterval reportInterval, File outfile,
             String[] dimensions, String[] measures)
    {
        boolean waitForResult = true;
        boolean returnCsvString = true;
        JSONArray filters = null;
        JSONArray orders = null;
        String timezone = "UTC";
        Object appNexusReportRet;
        Boolean oldOverrideMuffleJSON =
                AppNexusInterface.overrideMuffleJSON;
        try
        {
            AppNexusInterface.overrideMuffleJSON = true;
            appNexusReportRet =
                    AppNexusInterface.getGenericReportData
                            (reportClass, appNexusIdentity, advertiserId,
                             lineItemId, campaignId, reportInterval,
                             flightStart, flightEnd, dimensions, measures,
                             filters, orders, timezone, waitForResult,
                             returnCsvString);
        }
        catch (AppNexusUnAuthError e)
        {
            Utils.logThisPoint
                    (Level.WARN,
                     "Not authorised to get report data for advertiser "
                     + advertiserId);
            return 0;
        }
        finally
        {
            AppNexusInterface.overrideMuffleJSON =
                    oldOverrideMuffleJSON;
        }
        if(appNexusReportRet == null ||
           (appNexusReportRet instanceof AppNexusReturnValue &&
            ((AppNexusReturnValue)appNexusReportRet).isNull()))
        {
            return 0;
        }
        else if(appNexusReportRet instanceof String)
        {
            FileWriter fw = null;
            try
            {
                fw = new FileWriter(outfile);
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write((String) appNexusReportRet);
                bw.flush();
                fw.flush();
                bw.close();
                fw.close();
                fw = null;
            }
            catch (IOException e)
            {
                if(fw != null)
                {
                    try
                    {
                        fw.close();
                    }
                    catch (IOException ex)
                    {
                        throw Utils.barf("Failed to close file " + outfile +
                                         " properly.", null);
                    }
                }
            }
            return dimensions.length + measures.length;
        }
        else throw Utils.barf("Failed to get the requested report: " +
                              appNexusReportRet, null);
    }

    public static int dumpNetworkReportToFile
            (Identity appNexusIdentity, Date flightStart, Date flightEnd,
             Long advertiserId, Long lineItemId, Long campaignId, File outfile)
    {
        ReportInterval reportInterval = null;
        return dumpReportToFile
                (ReportNetworkAnalyticsService.class, appNexusIdentity,
                 flightStart, flightEnd, advertiserId, lineItemId, campaignId,
                 reportInterval, outfile, appNexusNetworkReportDimensions,
                 appNexusNetworkReportMeasures);
    }

    public static int dumpNetworkSiteDomainPerformanceReportToFile
            (Identity appNexusIdentity, Date flightStart, Date flightEnd,
             Long advertiserId, Long lineItemId, Long campaignId,
             ReportInterval reportInterval, File outfile)
    {
        return dumpReportToFile
                (ReportNetworkSiteDomainPerformanceService.class,
                 appNexusIdentity, flightStart, flightEnd, advertiserId,
                 lineItemId, campaignId, reportInterval, outfile,
                 appNexusNetworkSiteDomainPerformanceReportDimensions,
                 appNexusNetworkSiteDomainPerformanceReportMeasures);
    }

    public static int dumpNetworkAdvertiserFrequencyReportToFile
            (Identity appNexusIdentity, Long advertiserId, Long lineItemId,
             Long campaignId, ReportInterval reportInterval, File outfile)
    {
        return dumpReportToFile
                (ReportNetworkAdvertiserFrequencyRecencyService.class,
                 appNexusIdentity, null, null, advertiserId,
                 lineItemId, campaignId, reportInterval, outfile,
                 appNexusNetworkAdvertiserFrequencyReportDimensions,
                 appNexusNetworkAdvertiserFrequencyReportMeasures);
    }

    @SuppressWarnings("unused")
    public ReportAdvertiserAnalyticsService[] getAdvertiserReportData
            (Identity appNexusIdentity, Long advertiserId)
    {
        Date flightEnd = getCurrentTime();
        Date flightStart =
                new Date(flightEnd.getTime() -
                        (reportDurationInDays * 24 * 3600 * 1000));
        boolean waitForResult = true;
        boolean returnCsvString = false;
        Object appNexusReportRet;
        try
        {
            appNexusReportRet =
                    AppNexusInterface.getStandardReportForIntegration
                            (appNexusIdentity, advertiserId, null,
                                    flightStart, flightEnd,
                                    appNexusAdvertiserReportDimensions,
                                    appNexusAdvertiserReportMeasures,
                                    waitForResult, returnCsvString);
        }
        catch (AppNexusUnAuthError e)
        {
            Utils.logThisPoint
                    (Level.WARN,
                     "Not authorised to get report data for advertiser "
                            + advertiserId);
            return null;
        }
        if(appNexusReportRet instanceof ReportAdvertiserAnalyticsService[])
            return (ReportAdvertiserAnalyticsService[]) appNexusReportRet;
        else throw Utils.barf("Failed to get the requested report: " +
                              appNexusReportRet, null);
    }

    static boolean advertiserIsSelected(Long advertiserId, String[] selectedAdvertisers)
    {
        if(selectedAdvertisers == null) return true;
        else
        {
            String s1 = advertiserId.toString();
            for(String s: selectedAdvertisers)
            {
                if(s1.equals(s)) return true;
            }
            return false;
        }
    }

    public static Sexpression fetchSelectedAdvertisers(QueryContext qctx, String[] selectedAdvertisers)
    {
        Sexpression advertiserQuery =
                makeQueryForAdvertisers(selectedAdvertisers);
        Sexpression allAdvertisers =
                Utils.interpretACLWithRetry
                        (Integrator.INTEGRATOR, advertiserQuery, qctx);
        AppNexusInterface.debugPrint
                (allAdvertisers.length() + " advertisers found.");
        // Fetch the campaign data for the advertisers.
        Map<Long, AdvertiserService> advertiserMap =
                qctx.contextMap(AdvertiserService.class);
        Sexpression l = allAdvertisers;
        while(l != Null.nil)
        {
            Sexpression advertiserO = l.car();
            if(advertiserO instanceof AdvertiserService)
            {
                AdvertiserService advertiser =
                        (AdvertiserService)advertiserO;
                advertiserMap.put(advertiser.getId(), advertiser);
            }
            else throw Utils.barf("Not an advertiser: " + advertiserO, null,l);
            l = l.cdr();
        }
        return allAdvertisers;
    }

    public Date ensureLastReportTime(QueryContext qctx)
    {
        Date lastReportTime =
                CampaignData.getNetworkAnalyticsReportTime(qctx, true);
        if(lastReportTime == null)
            lastReportTime = CampaignData.recordNetworkAnalyticsReportTime
                                    (qctx, getAppNexusIdentity());
        return lastReportTime;
    }

    public IndexCounter fetchCampaignDataForAdvertiser
            (QueryContext qctx, SQLContext sctx, String[] selectedAdvertisers,
             Set<String> selectedCampaigns, IndexCounter advertiserIndex,
             Map<String, BidderInstruction> instructions,
             AdvertiserService advertiser,
             Map<Long, AdvertiserData> advertiserDataMap,
             Map<Long, AdvertiserData> currentAdvertiserDataMap,
             boolean selectExpiredCampaigns, Date endsGTETime,
             Date instructionsDate, Map<Long, Set<Long>> readyToForceStrategy)
    {
        Map<Long, AdvertiserService> advertiserMap =
                qctx.contextMap(AdvertiserService.class);
        synchronized(advertiserMap)
        {
            advertiserMap.put(advertiser.getId(), advertiser);
        }
        boolean selectedp =
                advertiserIsSelected
                        (advertiser.getId(), selectedAdvertisers);
        Long maxAdvertisers = getMaxAdvertisers();
        Date lastReportTime = ensureLastReportTime(qctx);
        if(selectedp && (maxAdvertisers == null ||
                         advertiserIndex.getIndex() < maxAdvertisers))
        {
            advertiserIndex.increment();
            setBidderState(BidderState.FETCHING_ADVERTISER_DATA);
            AdvertiserData ad = getCampaignDataForAdvertiser
                    (advertiserIndex, getAppNexusIdentity(), advertiser,
                     selectedCampaigns, Integrator.INTEGRATOR, qctx,
                     null, instructions, sctx, selectExpiredCampaigns,
                     endsGTETime, currentAdvertiserDataMap, instructionsDate);
            setCurrentAdvertiser(ad); // Might get overwritten in ||.
            synchronized(advertiserDataMap)
            {
                advertiserDataMap.put(advertiser.getId(), ad);
            }
            setBidderState(BidderState.EFFECTUATING_BIDS);
            if(getEffectuateBids())
                ad.effectuateBids(sctx, qctx, lastReportTime,
                                  readyToForceStrategy);
        }
        return advertiserIndex;
    }

    private Map<Long, AdvertiserData> fetchCampaignDataForAdvertisers
            (QueryContext qctx, SQLContext sctx, String[] selectedAdvertisers,
             Set<String> selectedCampaigns,
             Map<String, BidderInstruction> instructions,
             Sexpression allAdvertisers, boolean selectExpiredCampaigns,
             Date endsGTETime, Date instructionsDate,
             Map<Long, Set<Long>> readyToForceStrategy)
    {
        // Fetch the campaign data for the advertisers.
        Map<Long, AdvertiserData> advertiserDataMap =
                new HashMap<Long, AdvertiserData>();
        Map<Long, AdvertiserData> currentAdvertiserDataMap =
                getAdvertiserMap();
        IndexCounter advertiserIndex = new IndexCounter();
        ParallelExecuter exec = new ParallelExecuter(appNexusThreadCount);
        CatchThunk catchThunk = new BidderCatchThunk(exec);
        exec.setCatchThunk(catchThunk);
        int thunkIndex = 0;
        for(AdvertiserService advertiser:
                allAdvertisers.iterator(AdvertiserService.class))
        {
            ParallelJobThunk thunk =
                    new FetchCampaignDataThunk
                        (this, qctx, sctx, selectedAdvertisers,
                         selectedCampaigns, advertiserIndex, instructions,
                         advertiser, advertiserDataMap,
                         currentAdvertiserDataMap, selectExpiredCampaigns,
                         endsGTETime, instructionsDate, readyToForceStrategy,
                         thunkIndex);
            exec.addJob(thunk);
            thunkIndex = thunkIndex + 1;
        }
        List<Throwable> thrown = exec.doit();
        if(thrown.size() > 0)
        {
            StringBuffer sb = new StringBuffer();
            sb.append("Parallel processing of advertisers " +
                      "finished with these abnormal event(s): ");
            for(Throwable t: thrown)
            {
                sb.append("\n    ");
                sb.append(t);
            }
            Utils.logIt(Level.WARN, sb.toString());
        }
        return advertiserDataMap;
    }

    public AppNexusTheory getAppNexusTheory() {
        return appNexusTheory;
    }

    static AbstractBidStrategy[] defaultBidStrategies =
            { NoOptimizationBidStrategy.STRATEGY,
              NotSelectedBidStrategy.STRATEGY };

    static Set<BidStrategy> defaultBidStrategiesSet =
         new HashSet<BidStrategy>(Arrays.asList(defaultBidStrategies));

    static String campaignsWithUnchangedDefaultBidStrategyQuery()
    {
        StringBuffer sb = new StringBuffer();
        boolean first = true;
        for(AbstractBidStrategy bs: defaultBidStrategies)
        {
            String[] names = bs.getNames();
            for(String name: names)
            {
                if(first) first = false;
                else sb.append(", ");
                sb.append("'");
                sb.append(name);
                sb.append("'");
            }
        }
        String res;
        res =
        "SELECT t2.advertiser_id, t2.line_item_id, t2.campaign_id,\n" +
        "       t2.bid_strategy, Mn, Mx, Cnt\n" +
        "FROM (SELECT advertiser_id, line_item_id, campaign_id,\n" +
        "             MIN(event_time) AS Mn, MAX(event_time) AS Mx,\n" +
        "             COUNT(*) AS Cnt\n" +
        "      FROM bidhistory b1\n" +
        "      GROUP BY advertiser_id, line_item_id, campaign_id\n" +
        "      HAVING Cnt = 1\n" +
        "      AND    Mn < date_sub(date(now()), interval 1 day)) t1,\n" +
        "     bidhistory t2\n" +
        "WHERE t1.advertiser_id = t2.advertiser_id\n" +
        "AND   t1.line_item_id  = t2.line_item_id\n" +
        "AND   t1.campaign_id   = t2.campaign_id\n" +
        "AND   t2.bid_strategy IN\n" +
        "        (" + sb.toString() + ")\n" +
        "ORDER BY t2.advertiser_id, t2.line_item_id, t2.campaign_id;";
        return res;
    }

    Map<Long, Set<Long>> getGampaignsWithUnchangedDefaultBidStrategy
            (QueryContext qctx)
    {
        Sexpression rows;
        SQLConnector connector = ensureBidderSQLConnector();
        String query = campaignsWithUnchangedDefaultBidStrategyQuery();
        rows = connector.runSQLQuery(query, qctx);
        Map<Long, Set<Long>> map = new HashMap<Long, Set<Long>>();
        for(Sexpression row: rows)
        {
            long advertiserId = row.car().unboxLong();
            long campaignId   = row.third().unboxLong();
            Set<Long> camps = map.get(advertiserId);
            if(camps == null)
            {
                camps = new HashSet<Long>();
                map.put(advertiserId, camps);
            }
            camps.add(campaignId);
        }
        return map;
    }

    /** Processes the bid instructions from the input file specified in this class
     * writing the results to the output file specified in this class.
     * Limits the set of advertisers and/or campaigns to those specified in this class.
     * @return A map of the advertiser data by advertiser id.
     * @exception BusyException If this is already processing bid instructions.
     */
    public Map<Long, AdvertiserData> processBidInstructions() throws BusyException {
    	return processBidInstructions(getInputSpreadsheetPath(), getOutputSpreadsheetPath(), getAdvertiserIds(), getCampaignIds());
    }

    public AppNexusTheory ensureAppNexusTheory()
    {
        Identity appNexusIdentity = getAppNexusIdentity();
        if(appNexusIdentity == null)
            throw Utils.barf("AppNexus identity not set.", this);
        // Initialise AppNexus
        Constant theoryName = Syms.intern("APPNEXUS");
        appNexusTheory = (AppNexusTheory) Theory.referentTheory(theoryName);
        if(appNexusTheory == null)
            appNexusTheory = new AppNexusTheory(theoryName, appNexusIdentity);
        return appNexusTheory;
    }

    public Date getEndsGTETime()
    {
        // Typically a week before now.
        return new Date(this.getCurrentTime().getTime() -
                        this.campaignEndDateTolerance);
    }
    														
    /** Processes the bid instructions.
     * @param inputPath The path of the input file.
     * @param outputPath The path of the output file.
     * @param selectedAdvertisers The advertisers to which this should be restricted or null to use all advertisers.
     * @param selectedCampaigns The campaigns to which this should be restricted or null to use all campaigns.
     * @return A map of the advertiser data by advertiser id.
     * @exception BusyException If this is already processing bid instructions.
     */
    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public Map<Long, AdvertiserData> processBidInstructions
            (String inputPath, String outputPath,
             Set<String> selectedAdvertisers, Set<String> selectedCampaigns)
            throws BusyException
    {
        ExcelFileSchema schema = BidderInstruction.bidderInstructionSchema;
        Map<Long, AdvertiserData> advertiserDataMap = null;
        SQLContext sctx = null;
        SQLConnector connector = null;
        boolean selectExpiredCampaigns = this.selectExpiredCampaigns;
        Date endsGTETime = getEndsGTETime();
        QueryContext qctx = new BasicQueryContext(null, appNexusTheory);
        try {
            owningThread = Thread.currentThread(); // Just for debugging.
            synchronized(this) {
                if(isProcessingBidInstructions()) {
                    throw new BusyException(MSG_BIDDER_BUSY);
                }
                setProcessingBidInstructions(true);
                setLastRunTime(new Date());
            }
        	if(isPaused()) {
        		advertiserDataMap = new HashMap<Long, AdvertiserData>();
        	} else {
        		setBidderState(BidderState.STARTING);
        		setCurrentCampaign(null);
        		setCurrentAdvertiser(null);
                Map<Long, Set<Long>> readyToForceStrategy =
                        getGampaignsWithUnchangedDefaultBidStrategy(qctx);

        		Identity appNexusIdentity = getAppNexusIdentity();
        		if(appNexusIdentity == null)
        			throw Utils.barf("AppNexus identity not set.", this);

        		// Ensure the bidder database exists.
        		SQLConnector isAgent =
                        ensureBidderInformationSchemaSQLConnector();
        		if(isAgent != null)
        			isAgent.ensureDBExists(bidderSQLConnectorDBName);

        		// Load the instruction spreadsheet
        		File inputFile =
                        (inputPath == null ? null : new File(inputPath));
        		if(inputFile != null && !inputFile.exists()) {
                    Utils.logThisPoint
                            (Level.WARN, "Input file does not exist.");
        		}
        		setBidderState(BidderState.READING_SPREADSHEET);
        		Map<String, BidderInstruction> instructions =
        			(inputFile == null || !inputFile.exists()
        					? new HashMap<String, BidderInstruction>()
        			        : readInstructionsFile(inputPath, null, schema));
                Date instructionsDate = new Date();
                ensureAppNexusTheory();
        		// Initialise connection to bidder SQL DB.
        		connector = ensureBidderSQLConnector(false);

        		// Enable logging.
        		AppNexusInterface.setDebugPrinter(logPrinter);
        		connector.setTracePrinter(logPrinter);
        		lastQctx = qctx;

        		sctx = connector.allocateSQLContext(qctx);
                // Flush the historical data known dates cache
                historicalDataKnownDates = null;
                siteDomainReportKnownDates = null;
                // Force resetting of the TimeZone offset.  This should mean
                // that all processing for this bid instruction processing
                // gets converted with a consistent TimeZone.
                // The actual value of the offset should only change when DST
                // changes, i.e. twice a year.
                connector.resetTimeZoneOffset();

                // The site domain report has a maximum lookback of 30 days!
                Date reportFetchStart = hundredDaysAgo();
                Date reportFetchEnd = getCurrentTime();
                ensureSiteDomainReportPrefetched
                    (getAppNexusIdentity(), reportFetchStart,
                     reportFetchEnd, qctx);

                
        		setBidderState(BidderState.FETCHING_ADVERTISERS);
        		
        		String[] advertisers = null;
        		if((selectedAdvertisers != null) &&
                    !selectedAdvertisers.isEmpty())
                {
        			advertisers = new String[selectedAdvertisers.size()];
        			advertisers = selectedAdvertisers.toArray(advertisers);
        		}
        		Sexpression allAdvertisers =
                        fetchSelectedAdvertisers(qctx, advertisers);

        		advertiserDataMap = fetchCampaignDataForAdvertisers
                        (qctx, sctx, advertisers, selectedCampaigns,
        		         instructions, allAdvertisers, selectExpiredCampaigns,
                         endsGTETime, instructionsDate, readyToForceStrategy);

        		recordObservedAppNexusData(advertiserDataMap, sctx, qctx);

        		setBidderState(BidderState.WRITING_SPREADSHEET);
        		if(outputPath != null)
        			dumpBidData(outputPath, advertiserDataMap, qctx);
        		setAdvertiserMap(advertiserDataMap);
                // We process with an override date only once, so reset here!
                setOverrideCurrentTime(null);
        		setBidderState(BidderState.IDLE);
                if(sctx != null) {
        		    connector.deallocateSQLContext(sctx);
                    sctx = null;
        	    }
        	}
        }
        catch (BusyException e)
        {
            Utils.logThisPoint(Level.ERROR, "Bidder was busy");
        }
        catch (Throwable t)
        {
            Utils.barf(t, this, inputPath,
                       outputPath, selectedAdvertisers, selectedCampaigns,
                       advertiserDataMap, sctx, connector);
        }
        finally {
        	if(sctx != null) {
        		connector.deallocateSQLContext(sctx);
        	}
            owningThread = null;
            synchronized(this) {
                setProcessingBidInstructions(false);
            }
        }
        return advertiserDataMap;
    }

    @SuppressWarnings("unused")
    public Thread getOwningThread()
    {
        return owningThread;
    }

    void recordObservedAppNexusData
            (Map<Long, AdvertiserData> advertiserDataMap, SQLContext sctx,
             QueryContext qctx)
    {
        Date now = getCurrentTime();
        HashMap<Long, String> advertiserNameMap = new HashMap<Long, String>();
        HashMap<Long, String> lineItemNameMap = new HashMap<Long, String>();
        HashMap<Long, String> campaignNameMap = new HashMap<Long, String>();
        // Record names for dimension tables.
        for(Long id: advertiserDataMap.keySet())
        {
            AdvertiserService service = advertiserDataMap.get(id).service;
            if(service != null)
                advertiserNameMap.put(id, service.getName());
            advertiserDataMap.get(id).recordObservedData
                    (ensureBidderSQLConnector(), sctx, qctx, now,
                     lineItemNameMap, campaignNameMap);
        }
        fillInDimensionTables(advertiserNameMap, lineItemNameMap,
                              campaignNameMap, qctx);
        if(!defeatFetchingAppNexusReports)
            CampaignChangeClassifier.fixUpSequenceNumbersEtc(this, sctx, qctx);
    }

    void fillInDimensionTables(HashMap<Long, String> advertiserNameMap,
                               HashMap<Long, String> lineItemNameMap,
                               HashMap<Long, String> campaignNameMap,
                               QueryContext qctx)
    {
        Constant[] relations =
                new Constant[]
                        {
                                Syms.intern("CBO_DB.ADVERTISERNAMES"),
                                Syms.intern("CBO_DB.LINEITEMNAMES"),
                                Syms.intern("CBO_DB.CAMPAIGNNAMES")
                        };
        List<Map<Long, String>> maps = new Vector<Map<Long, String>> ();
        maps.add(advertiserNameMap);
        maps.add(lineItemNameMap);
        maps.add(campaignNameMap);
        int i = 0;
        for(Map<Long, String> map: maps)
        {
            Constant relation = relations[i];
            Sexpression updateQuery = Null.nil;
            for(Long id: map.keySet())
            {
                String name = map.get(id);
                updateQuery = new Cons(Sexpression.boxList(relation, id, name),
                                       updateQuery);
            }
            if(updateQuery != Null.nil)
            {
                updateQuery =
                        Cons.list(Syms.Tell, new Cons(Syms.And, updateQuery));
                Utils.interpretACL(Integrator.INTEGRATOR,
                                   Cons.list(Syms.Request, updateQuery,
                                             Null.nil), qctx);
            }
            i = i + 1;
        }
    }

    synchronized Set<Date> ensureHistoricalDataKnownDates(QueryContext qctx)
    {
        return ensureHistoricalDataKnownDates(false, qctx);
    }

    @SuppressWarnings("unchecked")
    synchronized Set<Date> ensureHistoricalDataKnownDates
            (boolean force, QueryContext qctx)
    {
        if(historicalDataKnownDates == null || force)
        {
            SQLConnector connector = ensureBidderSQLConnector();
            Sexpression query =
                    Sexpression.readFromString
                   ("(ask-all ?hour (CBO_DB.HistoricalDataKnownDates ?hour))");
            Sexpression res = connector.request(query, Null.nil, qctx);
            historicalDataKnownDates = Sexpression.unboxSet(res, Date.class);
            Set<Date> explicit = (Set<Date>)qctx.simpleCacheGet
                                        (EXPLICITLY_FETCHED_DATES);
            if(explicit != null)
                historicalDataKnownDates.addAll(explicit);
        }
        return historicalDataKnownDates;
    }

    synchronized Set<Date> ensureSiteDomainReportKnownDates(QueryContext qctx)
    {
        return ensureSiteDomainReportKnownDates(false, qctx);
    }

    @SuppressWarnings("unchecked")
    synchronized Set<Date> ensureSiteDomainReportKnownDates
            (boolean force, QueryContext qctx)
    {
        if(siteDomainReportKnownDates == null || force)
        {
            SQLConnector connector = ensureBidderSQLConnector();
            Sexpression query =
                    Sexpression.readFromString
             ("(ask-all ?hour (CBO_DB.Site_Domain_Report_Known_Dates ?hour))");
            Sexpression res = connector.request(query, Null.nil, qctx);
            siteDomainReportKnownDates = Sexpression.unboxSet(res, Date.class);
            Set<Date> explicit = (Set<Date>)qctx.simpleCacheGet
                                        (EXPLICITLY_FETCHED_DATES);
            if(explicit != null)
                siteDomainReportKnownDates.addAll(explicit);
        }
        return siteDomainReportKnownDates;
    }

    public void recordEvent
            (SQLContext sctx, QueryContext qctx, Long advertiserId,
             Long campaignId, String eventType, String description)
    {
        Sexpression updateQuery =
                Sexpression.readFromString
                        ("(tell (CBO_DB.Events",
                                advertiserId, campaignId, qt, eventType, qt,
                                qt, description, qt, "null))");
        sctx.requestQuiet(updateQuery, qctx);
    }

    Set<Date> asHourRange (Date start, Date end)
    {
        Set<Date> res = new LinkedHashSet<Date>();
        if(start == null || end == null) return res; // empty
        {
            start = AppNexusUtils.hourFloor(start);
            end = AppNexusUtils.hourCeiling(end);
            Calendar cal = Calendar.getInstance();
            cal.setTime(start);
            while(start.getTime() < end.getTime())
            {
                res.add(start);
                cal.add(Calendar.HOUR_OF_DAY, 1);
                start = cal.getTime();
            }
            return res;
        }
    }

    Set<Date> asDayRange (Date startDate, Date end)
    {
        Set<Date> res = new LinkedHashSet<Date>();
        Date start = startDate;
        if(start == null || end == null) return res; // empty
        {
            start = AppNexusUtils.dayFloor(start);
            end = AppNexusUtils.dayCeiling(end);
            Calendar cal = Calendar.getInstance();
            cal.setTime(start);
            while(start.getTime() < end.getTime())
            {
                res.add(start);
                cal.add(Calendar.DATE, 1);
                start = cal.getTime();
            }
            return res;
        }
    }

    @SuppressWarnings("unchecked")
    void recordExplicitlyFetchedDays(Date d, TableData knownDatesTable,
                                     SQLConnector connector, SQLContext sctx,
                                     QueryContext qctx, int maxHour)
    {

        // Today (or the future) doesn't count for permanent recording!
        // However, we non-persistently cache all hours that have been fetched.
        for(int i = 0; i < maxHour; i++)
        {
            StringBuffer b = new StringBuffer();
            // Note:  We make a new time here measured by
            // incrementing explicit milliseconds, rather than
            // by using a calendar and setting the hour_of_day.
            // This is because we want to handle the DST
            // transition issue correctly.
            Date thisHour =
                    new Date(d.getTime() + 3600 * i * 1000);
            Set<Date> explicit = (Set<Date>)qctx.simpleCacheGet
                                        (EXPLICITLY_FETCHED_DATES);
            if(explicit == null)
            {
                explicit = new LinkedHashSet<Date>();
                qctx.simpleCachePut(EXPLICITLY_FETCHED_DATES, explicit);
            }
            explicit.add(thisHour);
            if(d.getTime() < AppNexusUtils.dayFloor(getCurrentTime()).getTime())
            {
                b.append("INSERT IGNORE INTO ");
                b.append(knownDatesTable.getName());
                b.append(" VALUES('");
                b.append(dateParser.format(thisHour));
                b.append("');");
                String thisCommand = b.toString();
                connector.runSQLUpdate(sctx, thisCommand);
            }
        }
    }

    static boolean dstHackDateP(Date d)
    {
        long t = d.getTime();
        return t == 1320566400000L ||
               t == 1289116800000L;
    }

    int saveReportDataToDB
            (File txtOutFile, File csvOutFile, String[] dimensions,
             int columnCount, Date start, Date end, TableData dataTable,
             SQLConnector connector, SQLContext sctx)
    {
        Boolean oldOverrideMuffleJSON =
                AppNexusInterface.overrideMuffleJSON;
        int rowCount = 0;
        try
        {
            AppNexusInterface.overrideMuffleJSON = true;
            String msg =
                    (start != null && end != null
                        ? "Processing report data for " +
                                dateParser.format(start) + " to " +
                                dateParser.format(end)
                        : "Processing report data");
            AppNexusInterface.debugPrint(msg);
            setStatus(new Status(msg));
            rowCount =
                sanitiseCsvFile // Produces a TDT file.
                    (csvOutFile, txtOutFile, dimensions, columnCount, '\t',
                     dimensions.length, bidderSQLConnectorTimeZone == null);
            String nlConvention  =
                    toSQLCrLF(System.getProperty("line.separator"));
            // Now, try and bulk load the file.  Ideally, the file should
            // contain no data that we have, but that's not going to be the
            // case for today, since the fetch granularity is a whole day.
            // We therefore load with replace mode, and use the primary
            // key in the table to take care of the duplicates, preferring
            // the new data in case the latest hour we had got updated.
            // Ignore 1 line because of the column headers.
            String command =
                    "LOAD DATA LOCAL INFILE '" + toUnixPath(txtOutFile) + "'" +
                            "\nREPLACE" +
                            "\nINTO TABLE " + dataTable.getName() +
                            "\nFIELDS TERMINATED BY '\\t'" +
                            "\nLINES TERMINATED BY '"+nlConvention + "'" +
                            "\nIGNORE 1 LINES;";
            connector.runSQLUpdate(sctx, command);
        }
        finally
        {
            AppNexusInterface.overrideMuffleJSON =
                    oldOverrideMuffleJSON;
            if(csvOutFile.exists())
            {
                if(deletePrefetchTempFilesP && !csvOutFile.delete())
                    Utils.logThisPoint
                            (Level.WARN,
                             "Failed to delete temp file: " + csvOutFile);
            }
            if(txtOutFile.exists())
            {
                if(deletePrefetchTempFilesP && !txtOutFile.delete())
                    Utils.logThisPoint
                            (Level.WARN,
                             "Failed to delete temp file: " + txtOutFile);
            }
        }
        return rowCount;
    }

    static Date[] emptyDates = new Date[0];

    synchronized int prefetchMissingHistoricalData
            (SQLContext sctx, Set<Date> missingDates, TableData dataTable,
             TableData knownDatesTable, Identity appNexusIdentity, Long advertiserId,
             Long lineItemId, Long campaignId, QueryContext qctx)
    {
        // Group the missing dates into Days, since that is the granularity
        // of fetching from AppNexus, and then fetch in chunks of
        // n=missingDayHistoricalDataMaxChunkSize days, where n is an arbitrary number
        // that's small enough that it shouldn't load things too heavily,
        // and shouldn't result in too huge a chunk of data to
        // be transferred.
        Set<Date> missingDays = new LinkedHashSet<Date>();
        File outFile;
        File outFileTmp;
        int rowCount = 0;
        try {
        	outFile = File.createTempFile(HISTORICAL_DATA_TEMP_FILE_PREFIX, ".txt");
        	outFileTmp = File.createTempFile(HISTORICAL_DATA_TEMP_FILE_PREFIX, ".tmp");
        } catch(IOException ioe) {
        	throw Utils.barf("Failed to create temp files for fetching historical data.", null);
        }

        SQLConnector connector = ensureBidderSQLConnector();
        // Date startOfToday = AppNexusUtils.dayFloor(getCurrentTime());
        Date endOfToday = AppNexusUtils.dayCeiling(getCurrentTime());
        for(Date m: missingDates)
        {
            Date dayStart = AppNexusUtils.dayFloor(m);
            if(m.getTime() < endOfToday.getTime() &&
               !dstHackDateP(m)) // todo - Shouldn't need this if we are using a stable absolute timezone - needs a fix from AppNexus.
                missingDays.add(dayStart);
        }
        Date[] currentMissingDays = missingDays.toArray(emptyDates);
        Arrays.sort(currentMissingDays);
        int chunkSize;
        boolean fetchPerformed = false;
        while(currentMissingDays.length > 0)
        {
            Date start = currentMissingDays[0];
            Date end = currentMissingDays[0];
            chunkSize = 1;
            for(int i = 1; i < currentMissingDays.length; i++)
            {
                if(chunkSize >= missingDayHistoricalDataMaxChunkSize) break;
                Date thisDay = currentMissingDays[i];
                // Must pick contiguous days!
                if(thisDay.getTime() == (end.getTime() + milliSecondsInADay))
                {
                    end = thisDay;
                    chunkSize = chunkSize + 1;
                }
            }
            // Start and End are now a contiguous date range that we can fetch
            Boolean oldOverrideMuffleJSON =
                    AppNexusInterface.overrideMuffleJSON;
            try
            {
                AppNexusInterface.overrideMuffleJSON = true;
                String msg = "Fetching report data for " +
                             dateParser.format(start) + " to " +
                             dateParser.format(end);
                AppNexusInterface.debugPrint(msg);
                setStatus(new Status(msg));
                int columnCount =
                        dumpNetworkReportToFile
                                (appNexusIdentity, start, end, advertiserId,
                                        lineItemId, campaignId, outFileTmp);
                // This step may not be necessary.  Keep it for now, just
                // in case.
                if(columnCount > 0)
                {
                    rowCount =
                            sanitiseCsvFile // Produces a TDT file.
                                    (outFileTmp, outFile,
                                     appNexusNetworkReportDimensions,
                                     columnCount, '\t',
                                     appNexusNetworkReportDimensions.length,
                                     bidderSQLConnectorTimeZone == null);
                    String nlConvention  =
                            toSQLCrLF(System.getProperty("line.separator"));
                    fetchPerformed = true;
                    // Now, try and bulk load the file.  Ideally, the file
                    // should contain no data that we have, but that's
                    // not going to be the case for today, since the fetch
                    // granularity is a whole day.  We therefore load with
                    // replace mode, and use the primary key in the table
                    // to take care of the duplicates, preferring the new
                    // data in case the latest hour we had got updated.
                    // Ignore 1 line because of the column headers.
                    String command =
                            "LOAD DATA LOCAL INFILE '" + toUnixPath(outFile) + "'" +
                                    "\nREPLACE" +
                                    "\nINTO TABLE " + dataTable.getName() +
                                    "\nFIELDS TERMINATED BY '\\t'" +
                                    "\nLINES TERMINATED BY '" +
                                    nlConvention + "'" +
                                    "\nIGNORE 1 LINES;";
                    connector.runSQLUpdate(sctx, command);
                    // Record the days explicitly fetched.
                    // As long as a day isn't today, we can assume
                    // we've fetched every hour successfully.
                    for(int i = 0; i < chunkSize; i++)
                    {
                        Date d = currentMissingDays[i];
                        recordExplicitlyFetchedDays
                                (d, knownDatesTable, connector, sctx, qctx, 24);
                    }
                }
            }
            finally
            {
                AppNexusInterface.overrideMuffleJSON =
                        oldOverrideMuffleJSON;
                if(outFileTmp.exists())
                {
                    if(deletePrefetchTempFilesP && !outFileTmp.delete())
                        Utils.logThisPoint
                                (Level.WARN,
                                 "Failed to delete temp file: " + outFileTmp);
                }
                if(outFile.exists())
                {
                    if(deletePrefetchTempFilesP && !outFile.delete())
                        Utils.logThisPoint
                                (Level.WARN,
                                 "Failed to delete temp file: " + outFile);
                }
            }
            // Now, look for another chunk to work on.
            Date[] newCurrentMissingDays =
                    new Date[currentMissingDays.length - chunkSize];
            System.arraycopy(currentMissingDays, chunkSize,
                             newCurrentMissingDays, 0,
                             newCurrentMissingDays.length);
            currentMissingDays = newCurrentMissingDays;
        }
        if(fetchPerformed)
        // Then we must recompute the hours we have cached.
        {
            String command = "INSERT IGNORE INTO " +
                    knownDatesTable.getName() +
                    "\n    SELECT DISTINCT hour FROM " + dataTable.getName() +
                    ";";
            connector.runSQLUpdate(sctx, command);
            ensureHistoricalDataKnownDates(true, qctx);
        }
        return rowCount;
    }

    static String rewriteTimeStamp
            (String v, long timeZoneOffset, String colName)
    {
        // Incoming date is yyyy-mm-dd hh:mm:ss in UTC.
        // Rewrite value into the local timeZone.
        SynchDateFormat parser;
        if(v == null) return null;
        else
        {
            try
            {
                if(hour_Colname.equals(colName)) parser = hourParser;
                else if(day_Colname.equals(colName)) parser = dayParser;
                else throw Utils.barf
                        ("Unhandled Timesatmp column", null, colName);
                Date d = parser.parse(v);
                Date corrected = new Date(d.getTime() + timeZoneOffset);
                String rewritten;
                rewritten = dateParser.format(corrected);
                return rewritten;
            }
            catch (ParseException e)
            {
                throw Utils.barf(e, null);
            }
        }
    }

    static int sanitiseCsvFile(File infile, File outfile, String[] dimensions,
                               int colCount, char outputSeparator,
                               int measureColsStart, boolean rewriteDates)
    {
        // We observe that sometimes AppNexus CSV results come back
        // with missing columns, usually on the last row.
        // If so, we need to remove them.
        FileWriter fw = null;
        FileReader fr = null;
        CSVParser parser = new CSVParser();
        TimeZone tz = Calendar.getInstance().getTimeZone();
        long dateForTimeZoneOffset = new Date().getTime();
        long timeZoneOffset = tz.getOffset(dateForTimeZoneOffset);
        boolean[] idMask = new boolean[colCount];
        for(int i = 0; i < colCount; i++)
        {
            idMask[i] = false;
        }
        for(int i = 0; i < dimensions.length; i++)
        {
            if(advertiser_id_ColName.equals(dimensions[i]) ||
                line_item_id_ColName.equals(dimensions[i]) ||
                 campaign_id_ColName.equals(dimensions[i]))
                idMask[i] = true;
        }
        int rowCount = 0;
        int bogusRows = 0;
        try
        {
            fw = new FileWriter(outfile);
            fr = new FileReader(infile);
            BufferedWriter bw = new BufferedWriter(fw);
            BufferedReader br = new BufferedReader(fr);
            String line;
            String lastLine = null;
            while(true)
            {
                line = br.readLine();
                if(line == null) break;
                String[] vals = parser.parseLineMulti(line);
                boolean zeroIdFound = false;
                if(vals.length == colCount)
                {
                    for(int i = 0; i < colCount; i++)
                    {
                        if(idMask[i] && "0".equals(vals[i]))
                        {
                            zeroIdFound = true;
                            break;
                        }
                    }
                    if(zeroIdFound) bogusRows = bogusRows + 1;
                    else
                    {
                        if(rowCount > 0) bw.newLine();
                        for(int i = 0; i < vals.length; i++)
                        {
                            String colName =
                                    (i < dimensions.length
                                            ? dimensions[i]
                                            : null);
                            boolean rewriteTimeStampCol =
                                    rowCount > 0 && // because of col headers.
                                            rewriteDates &&
                                            colName != null &&
                                            timeStampTypedColumns.contains
                                                    (colName);
                            if(i > 0) bw.write(outputSeparator);
                            String v = vals[i];
                            if(rewriteTimeStampCol)
                                v = rewriteTimeStamp
                                        (v, timeZoneOffset, colName);
                            if(i >= measureColsStart && ".".equals(v))
                                bw.write("0.0"); // Replace "." with "0.0"
                            else bw.write(v);
                        }
                        rowCount = rowCount + 1;
                    }
                }
                else bogusRows = bogusRows + 1;
                lastLine = line;
            }
            AppNexusInterface.debugPrint("Sanitised " + rowCount +
                    " rows.  Bogus rows: " + bogusRows +
                    ".  Last row: " + lastLine);
            bw.flush();
            fw.flush();
            bw.close();
            fw.close();
            fw = null;
            fr.close();
            fr = null;
        }
        catch (IOException e)
        {
            if(fw != null)
            {
                try
                {
                    fw.close();
                }
                catch (IOException ex)
                {
                    throw Utils.barf("Failed to close file " + outfile +
                                     " properly.", null);
                }
            }
            if(fr != null)
            {
                try
                {
                    fr.close();
                }
                catch (IOException ex)
                {
                    throw Utils.barf("Failed to close file " + infile +
                                     " properly.", null);
                }
            }
        }
        return rowCount;
    }

    static String toUnixPath(File f)
    {
        return f.getAbsolutePath().replace('\\', '/');
    }

    static String toSQLCrLF(String s)
    {
        String res = "";
        for(int i=0; i < s.length(); i++)
        {
            Character c = s.charAt(i);
            if(c == '\n') res = res + "\\n";
            else if(c == '\r') res = res + "\\r";
            else res = res + c;
        }
        return res;
    }

    Date daysAgo(int days)
    {
        Date now = getCurrentTime();
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        cal.add(Calendar.DATE, - days);
        return cal.getTime();
    }

    Date thirtyDaysAgo()
    {
        return daysAgo(30);
    }

    Date hundredDaysAgo()
    {
        return daysAgo(100);
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    void ensureSiteDomainReportPrefetched
          (Identity appNexusIdentity, Date flightStart, Date flightEnd,
           QueryContext qctx)
    {
        // The site domain report has a maximum lookback of 30 days!
        Date thirtyDaysAgo = thirtyDaysAgo();
        Set<Date> knownDates = ensureSiteDomainReportKnownDates(qctx);
        flightStart = AppNexusUtils.dateMax(flightStart, thirtyDaysAgo);
        Set<Date> neededDates = asDayRange(flightStart, flightEnd);
        Set<Date> missingDates =
                AppNexusUtils.setDifference(neededDates, knownDates);
        SQLConnector connector = ensureBidderSQLConnector();
        if(!missingDates.isEmpty() && fetchHistoricalData)
        {
            SQLContext sctx = null;
            try
            {
                sctx = connector.allocateSQLContext(qctx);
                try
                {
                    prefetchMissingSiteDomainReport
                           (sctx, missingDates,
                            networkSiteDomainPerformanceTable,
                            siteDomainReportKnownDatesTable, appNexusIdentity,
                            null, null, null, qctx);
                }
                catch (AppNexusSystemError e)
                {
                    Utils.barf("Error fetching a report: " + e.getMessage(),
                               this, sctx, missingDates,
                               networkSiteDomainPerformanceTable,
                               siteDomainReportKnownDatesTable,
                               appNexusIdentity, qctx);
                }
            }
            finally
            {
                if(sctx != null) connector.deallocateSQLContext(sctx);
            }
        }
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    synchronized void prefetchMissingSiteDomainReport
            (SQLContext sctx, Set<Date> missingDates, TableData dataTable,
             TableData knownDatesTable, Identity appNexusIdentity,
             Long advertiserId, Long lineItemId, Long campaignId,
             QueryContext qctx)
    {
        // Group the missing dates into Days, since that is the granularity
        // of fetching from AppNexus, and then fetch in chunks of
        // n=missingDaySiteDomainReportMaxChunkSize days, where n is an arbitrary number
        // that's small enough that it shouldn't load things too heavily,
        // and shouldn't result in too huge a chunk of data to
        // be transferred.
        Set<Date> missingDays = new LinkedHashSet<Date>();
        File outFile;
        File outFileTmp;

        try
        {
        	outFile =
                    File.createTempFile(SITE_DOMAIN_TEMP_FILE_PREFIX, ".txt");
        	outFileTmp =
                    File.createTempFile(SITE_DOMAIN_TEMP_FILE_PREFIX, ".tmp");
        }
        catch(IOException ioe)
        {
        	throw Utils.barf
          ("Failed to create temp files for fetching site domain data.", null);
        }

        SQLConnector connector = ensureBidderSQLConnector();
        // Date startOfToday = AppNexusUtils.dayFloor(getCurrentTime());
        Date endOfToday = AppNexusUtils.dayCeiling(getCurrentTime());
        for(Date m: missingDates)
        {
            Date dayStart = AppNexusUtils.dayFloor(m);
            if(m.getTime() < endOfToday.getTime() &&
               !dstHackDateP(m)) // todo - Shouldn't need this if we are using a stable absolute timezone - needs a fix from AppNexus.
                missingDays.add(dayStart);
        }
        Date[] currentMissingDays = missingDays.toArray(emptyDates);
        Arrays.sort(currentMissingDays);
        int chunkSize;
        while(currentMissingDays.length > 0)
        {
            Date start = currentMissingDays[0];
            Date end = currentMissingDays[0];
            chunkSize = 1;
            for(int i = 1; i < currentMissingDays.length; i++)
            {
                if(chunkSize >= missingDaySiteDomainReportMaxChunkSize) break;
                Date thisDay = currentMissingDays[i];
                // Must pick contiguous days!
                if(thisDay.getTime() == (end.getTime() + milliSecondsInADay))
                {
                    end = thisDay;
                    chunkSize = chunkSize + 1;
                }
            }
            // Start and End are now a contiguous date range that we can fetch
            // Note: There's an asymmetry in the way AppNexus handles the start
            //       and end dates for this service, compared to (say) the
            //       Analytics Service.  We have to set the end_date to one day
            //       after the intended date.  Go figguh!
            end = new Date(end.getTime() + (24 * 3600 * 1000));
            Boolean oldOverrideMuffleJSON =
                    AppNexusInterface.overrideMuffleJSON;
            try
            {
                AppNexusInterface.overrideMuffleJSON = true;
                String msg = "Fetching report data for " +
                             dateParser.format(start) + " to " +
                             dateParser.format(end);
                AppNexusInterface.debugPrint(msg);
                setStatus(new Status(msg));
                ReportInterval reportInterval = null; // ReportInterval.last_7_days;
                int columnCount =
                        dumpNetworkSiteDomainPerformanceReportToFile
                            (appNexusIdentity, start, end,
                             advertiserId , lineItemId, campaignId,
                             reportInterval,
                             outFileTmp);
                if(columnCount > 0)
                {
                    saveReportDataToDB
                         (outFile, outFileTmp,
                          appNexusNetworkSiteDomainPerformanceReportDimensions,
                          columnCount, start, end,
                          dataTable, connector, sctx);
                    // Record the days explicitly fetched.
                    for(int i = 0; i < chunkSize; i++)
                    {
                        Date d = currentMissingDays[i];
                        recordExplicitlyFetchedDays
                                (d, knownDatesTable, connector, sctx, qctx, 1);
                    }
                }
            }
            finally
            {
                AppNexusInterface.overrideMuffleJSON =
                        oldOverrideMuffleJSON;
                if(outFileTmp.exists())
                {
                    if(deletePrefetchTempFilesP && !outFileTmp.delete())
                        Utils.logThisPoint
                                (Level.WARN,
                                 "Failed to delete temp file: " + outFileTmp);
                }
                if(outFile.exists())
                {
                    if(deletePrefetchTempFilesP && !outFile.delete())
                        Utils.logThisPoint
                                (Level.WARN,
                                 "Failed to delete temp file: " + outFile);
                }
            }
            // Now, look for another chunk to work on.
            Date[] newCurrentMissingDays =
                    new Date[currentMissingDays.length - chunkSize];
            System.arraycopy(currentMissingDays, chunkSize,
                             newCurrentMissingDays, 0,
                             newCurrentMissingDays.length);
            currentMissingDays = newCurrentMissingDays;
        }
        ensureSiteDomainReportKnownDates(true, qctx);
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    void ensureHistoricalDataPrefetched
          (Identity appNexusIdentity, Date flightStart, Date flightEnd, QueryContext qctx)
    {
        Set<Date> knownDates = ensureHistoricalDataKnownDates(qctx);
        Set<Date> neededDates = asHourRange(flightStart, flightEnd);
        Set<Date> missingDates =
                AppNexusUtils.setDifference(neededDates, knownDates);
        SQLConnector connector = ensureBidderSQLConnector();
        if(!missingDates.isEmpty() && fetchHistoricalData)
        {
            SQLContext sctx = null;
            try
            {
                sctx = connector.allocateSQLContext(qctx);
                try
                {
                    prefetchMissingHistoricalData
                               (sctx, missingDates, historicalDataTable,
                                historicalDataKnownDatesTable, appNexusIdentity,
                                null, null, null, qctx);
                }
                catch (AppNexusSystemError e)
                {
                    Utils.barf("Error fetching a report: " + e.getMessage(),
                               this, sctx, missingDates, historicalDataTable,
                               historicalDataKnownDatesTable, appNexusIdentity, qctx);
                }
            }
            finally
            {
                if(sctx != null) connector.deallocateSQLContext(sctx);
            }
        }
    }

    public void setDefeatFetchingAppNexusReports(boolean to)
    {
        defeatFetchingAppNexusReports = to;
    }

    public boolean getDefeatFetchingAppNexusReports()
    {
        return defeatFetchingAppNexusReports;
    }

    Map<Date, HistoricalDataRow> getHistoricalDataFor
            (Identity appNexusIdentity, Date flightStart, Date flightEnd,
             Long advertiserId, Long lineItemId, Long campaignId,
             QueryContext qctx)
    {
        if(!defeatFetchingAppNexusReports)
            ensureHistoricalDataPrefetched
                    (appNexusIdentity, flightStart, flightEnd, qctx);
        SQLConnector connector = ensureBidderSQLConnector();
        Map<Date, HistoricalDataRow> results =
                new HashMap<Date, HistoricalDataRow>();
        class GetHistoricalDataThunk extends SQLThunk {

            Map<Date, HistoricalDataRow> results;
            SQLConnector connector;

            GetHistoricalDataThunk(SQLConnector connector,
                                   Map<Date, HistoricalDataRow> results)
            {
                super(connector);
                this.results = results;
                this.connector = connector;
            }

            public void doit(ResultSet rs) throws SQLException
            {
                Timestamp timestamp = rs.getTimestamp(1);
                HistoricalDataRow row =
                        new HistoricalDataRow
                                (connector.correctSQLDate(timestamp),
                                 rs.getLong(2), rs.getDouble(3), 0d);
                results.put(row.dateTime, row);
                traceOrNoteProgress(Syms.T, false);
            }
        }
        String query =
                "SELECT hour, imps, cost\n" +
                "FROM " + historicalDataTable.getName() + "\n" +
                "WHERE advertiser_id = " + advertiserId + "\n" +
                "AND   line_item_id = " + lineItemId + "\n" +
                "AND   campaign_id = " + campaignId + "\n" +
                "ORDER BY hour ASC";
        SQLThunk thunk = new GetHistoricalDataThunk(connector, results);
        connector.runSQLQuery(query, thunk, qctx);
        thunk.finishProgress();
        return results;
    }

    int dumpAdvertiserBidData
            (AdvertiserData advertiser, HSSFSheet worksheet,
             Map<CellStyleName, CellStyle> styleMap, int rowIndex,
             QueryContext qctx, ExcelFileSchema schema)
    {
        MethodMapper methodMapper = schema.getMethodMapper();
    	setCurrentAdvertiser(advertiser);
        for(CampaignData cd: advertiser.campaignData)
        {
            if(cd != null)
            {
                CampaignData.dumpCampaignData
                        (worksheet, rowIndex, styleMap, qctx, schema,
                         methodMapper, cd);
                rowIndex = rowIndex + 1;
            }
        }
        return rowIndex;
    }

    public synchronized void dumpBidData
            (String outputPath, Map<Long, AdvertiserData> advertiserDataMap,
             QueryContext qctx)
    {
        ExcelFileSchema schema = BidderInstruction.bidderInstructionSchema;
        dumpBidData(outputPath, advertiserDataMap, qctx, schema);
    }

    String quotify(String s)
    {
        return s.replace("'", "''");
    }

    public void recordPerpetrator(String perpetrator, String eventName,
                                  Date eventTime, String param,
                                  QueryContext qctx)
    {
        SQLConnector connector = ensureBidderSQLConnector();
        String updateQuery =
                "INSERT INTO perpetrators VALUES ('" +
                        quotify(perpetrator) + "','" +
                        quotify(eventName) + "','" +
                        connector.dateToSQL(eventTime) + "','" +
                        quotify(param) + "');";
        connector.runSQLUpdate(updateQuery, qctx);
    }

    public void dumpBidData
            (String outputPath, Map<Long, AdvertiserData> advertiserDataMap,
             QueryContext qctx, ExcelFileSchema schema)
    {
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet worksheet = workbook.createSheet();
        Map<CellStyleName, CellStyle> styleMap =
                ExcelColSchema.setupStyleMap(workbook);

        // Always write the header row.
        int rowIndex = CampaignData.emitColumnHeaders
                                (worksheet, 0, styleMap, schema);
        List<AdvertiserData> values = new Vector<AdvertiserData>(advertiserDataMap.values());
        Collections.sort(values, AdvertiserData.nameComparator);
        for(AdvertiserData ad: values)
        {
            rowIndex = dumpAdvertiserBidData(ad, worksheet, styleMap, rowIndex,
                                             qctx, schema);
        }
        BidderInstruction.assertConstraints(worksheet, schema);
        synchronized(Utils.internFile(outputPath))
        {
            ExcelColSchema.dumpSpreadsheet(workbook, outputPath);
        }
    }

    public void downloadSpreadsheetForSchema
            (OutputStream os, Map<Long, AdvertiserData> advertiserDataMap,
             QueryContext qctx, ExcelFileSchema schema)
    {
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet worksheet = workbook.createSheet();
        Map<CellStyleName, CellStyle> styleMap =
                ExcelColSchema.setupStyleMap(workbook);
        // Always write the header row.
        int rowIndex = CampaignData.emitColumnHeaders
                                (worksheet, 0, styleMap, schema);
        for(Long id: advertiserDataMap.keySet())
        {
            rowIndex = dumpAdvertiserBidData(advertiserDataMap.get(id),
                                             worksheet, styleMap, rowIndex,
                                             qctx, schema);
        }
        BidderInstruction.assertConstraints(worksheet, schema);
        ExcelColSchema.dumpSpreadsheet(workbook, os);
    }

    Observation getJSONFor(Long id, QueryContext qctx)
    {
        return getJSONFor(id, qctx, null, null, false, true, true);
    }

    Observation getJSONFor(Long id, QueryContext qctx, boolean justForCampaign)
    {
        return getJSONFor(id, qctx, null, null, false, true, !justForCampaign);
    }

    Observation getJSONFor
            (Long id, QueryContext qctx, Date observationTime,
             Long sequenceNumber, boolean before, boolean getCampaign,
             boolean getProfile)
    {   // Maybe rewrite this as a Zini query some day.
        SQLConnector connector = ensureBidderSQLConnector();
        if(!getCampaign && !getProfile)
            throw Utils.barf("Must have getCampaign ior getProfile", this);
        String query =
        "SELECT campaign_id, campaign_profile_id, campaign_json,\n" +
        "       campaign_profile_json, observation_time\n" +
        "FROM observeddata o1\n" +
        (getCampaign
          ? (getProfile
              ? "WHERE (campaign_id = " + id + " or campaign_profile_id = " +
                                          id + ")\n"
              : "WHERE campaign_id = " + id + "\n")
          : "WHERE campaign_profile_id = " + id + "\n")+
        (observationTime != null
                ? "AND   observation_time " + (before ? "< '" : "= '") +
                  SQLConnector.grindFormat.format
                  (connector.reverseCorrectSQLDate(observationTime)) + "'\n"
                : "") +
        (sequenceNumber != null
                ? "AND   sequence_number " + (before ? "< " : "= ") +
                         sequenceNumber + "\n"
                : "") +
        "ORDER BY observation_time DESC\n" +
        "LIMIT 1;";
        SexpLoc results = new SexpLoc();
        ResultCollectingThunk thunk =
                new ResultCollectingThunk(connector, results, -1);
        connector.runSQLQuery(query, thunk, qctx);
        thunk.finishProgress();
        Sexpression l = results.getSexp();
        if(l != Null.nil)
        {
            Sexpression row = l.car();
            Observation res = new Observation();
            res.observationTime = row.fifth().unboxDate();
            if(id.equals(row.car().unboxLong()) ||
               id.equals(row.second().unboxLong()))
            {
                res.campaignJSON =
                        (JSONObject)SQLHTTPHandler.toJSON(row.third());
                res.profileJSON  =
                        (JSONObject)SQLHTTPHandler.toJSON(row.fourth());
            }
            return res;
        }
        return null;
    }

    public static String commaSeparatePrinc(Collection<Sexpression> ss)
    {
        StringBuffer sb = new StringBuffer();
        boolean first = true;
        for(Sexpression s: ss)
        {
            if(first) first = false;
            else sb.append(",");
            sb.append(s == null ? "null" : s.princ());
        }
        return sb.toString();
    }

    Map<Sexpression, List<Object>> getServicesFor
            (List<Sexpression> ids, Map<Sexpression,
             List<Object>> mappedObjTable, QueryContext qctx)
    {   // Maybe rewrite this as a Zini query some day.
        if(ids == null || ids.size() == 0) {}
        else
        {
            SQLConnector connector = ensureBidderSQLConnector();
            Collections.sort(ids, Sexpression.comparator);
            String separated = commaSeparatePrinc(ids);
            String query =
              "SELECT campaign_id, campaign_profile_id, campaign_json,\n" +
              "       campaign_profile_json\n" +
              "FROM observeddata o1\n" +
              "WHERE (   campaign_id IN (" + separated + ")\n" +
              "       OR  campaign_profile_id IN (" + separated + "))\n" +
              "AND   NOT EXISTS (SELECT * FROM observeddata o2\n" +
              "                  WHERE o1.campaign_id = o2.campaign_id\n" +
              "                  AND   o1.advertiser_id = o2.advertiser_id\n" +
              "                  AND   o1.campaign_profile_id = o2.campaign_profile_id\n" +
              "                  AND   o1.observation_time < o2.observation_time)\n" +
              "ORDER BY campaign_id, campaign_profile_id;";
            SexpLoc results = new SexpLoc();
            ResultCollectingThunk thunk =
                    new ResultCollectingThunk(connector, results, -1);
            connector.runSQLQuery(query, thunk, qctx);
            thunk.finishProgress();
            Sexpression l = results.getSexp();
            while(l != Null.nil)
            {
                Sexpression row = l.car();
                SQLHTTPHandler.ensureMapEntry(row.car(), mappedObjTable).add
                        (serviceForJSON
                           ((JSONObject)SQLHTTPHandler.toJSON(row.third()),
                                   true, false));
                SQLHTTPHandler.ensureMapEntry(row.second(), mappedObjTable).add
                        (serviceForJSON
                           ((JSONObject)SQLHTTPHandler.toJSON(row.fourth()),
                                   false, true));
                l = l.cdr();
            }
        }
        return mappedObjTable;
    }

    AbstractAppNexusService getServiceFor
            (Long id, QueryContext qctx, boolean campaignP, boolean profileP)
    {
        Observation obj = getJSONFor(id, qctx);
        if(obj == null) return null;
        else
        {
            JSONObject obj2 =
                    (campaignP && obj.campaignJSON != null
                            ? (JSONObject)obj.campaignJSON.get("campaign")
                            : null);
            if(obj2 != null) return new CampaignService(obj2);
            else
            {
                obj2 = (profileP && obj.profileJSON != null
                            ? (JSONObject)obj.profileJSON.get("profile")
                            : null);
                if(obj2 != null) return new ProfileService(obj2);
                else return null;
            }
        }
    }
    
    AbstractAppNexusService serviceForJSON
            (JSONObject obj, boolean campaignP, boolean profileP)
    {
        if(obj == null) return null;
        else
        {
            JSONObject obj2 =
                    (campaignP
                            ? (JSONObject)obj.get("campaign")
                            : null);
            if(obj2 != null) return new CampaignService(obj2);
            else
            {
                obj2 = (profileP
                            ? (JSONObject)obj.get("profile")
                            : null);
                if(obj2 != null) return new ProfileService(obj2);
                else return null;
            }
        }
    }

    /** Sets the current campaign being considered for debugging purposes.
     * @param c The current campaign.
     */
    private void setCurrentCampaign(CampaignData c) {
    	this.currentCampaign = c;
    }
    
    /** Gets the current campaign being worked on for debugging purposes.
     * @return The current campaign that is being worked on.
     */
    @SuppressWarnings("unused")
    CampaignData getCurrentCampaign() {
    	return currentCampaign;
    }
    
    /** Sets the current advertiser being considered for debugging purposes.
     * @param a The current advertiser.
     */
    private void setCurrentAdvertiser(AdvertiserData a) {
    	currentAdvertiser = a;
    }

    /** Gets the current advertiser being worked on for debugging purposes.
     * @return The current advertiser that is being worked on.
     */
    @SuppressWarnings("unused")
    AdvertiserData getCurrentAdvertiser() {
    	return currentAdvertiser;
    }
    
 }

class BidderIdDecoder extends IdDecoder {

    Bidder bidder;

    BidderIdDecoder(Bidder bidder, String name)
    {
        super(name);
        this.bidder = bidder;
    }

    public List<Sexpression> findIdsYetToProcess
            (Map<Sexpression, List<Object>> mappedObjTable)
    {
        List<Sexpression> yetToProcess = new Vector<Sexpression>();
        for(Sexpression key: mappedObjTable.keySet())
        {
            List<Object> obj = mappedObjTable.get(key);
            // Object found = qctx.simpleCacheGet(key);
            // if(found != null)
            //     bidder.ensureEntry(key, mappedObjTable).add(found);
            // else
            if((obj == null || obj.size() == 0)
                    && key instanceof NumberAtom)
            {
                Long l = ((NumberAtom)key).longValueOfSafe();
                if(l != null && l > 0l) yetToProcess.add(key);
            }
            else {}
        }
        return yetToProcess;
    }

    public void mapIds(Map<Sexpression, List<Object>> mappedObjTable,
                       QueryContext qctx)
    {
        List<Sexpression> yetToProcess =
                findIdsYetToProcess(mappedObjTable);
        if(yetToProcess .size() == 0) {}
        else if(bidder.getAdvertiserMap() != null)
        {
            // First try
            for(Sexpression s: yetToProcess)
            {
                Long l = s.unboxLong();
                Object o = bidder.objectForId(l);
                if(o != null)
                    SQLHTTPHandler.ensureMapEntry(s, mappedObjTable).add(o);
            }
            // Second try
            yetToProcess = findIdsYetToProcess(mappedObjTable);
            // Object match = qctx.simpleCacheGet(l);
            bidder.getServicesFor(yetToProcess, mappedObjTable, qctx);
        }
        else
        {
            SQLConnector connector = bidder.ensureBidderSQLConnector();
            Sexpression advertisers =
                    BidderGrapherHTTPHandler.getAdvertisers(connector, qctx);
            Set<Sexpression> advertiserMap = new HashSet<Sexpression>();
            Sexpression campaigns =
                    BidderGrapherHTTPHandler.getAdvertisersAndCampaigns(qctx);
            Map<Sexpression, Long> campaignsMap =
                    new HashMap<Sexpression, Long>();
            while(advertisers != Null.nil)
            {
                advertiserMap.add(advertisers.car().car());
                advertisers = advertisers.cdr();
            }
            while(campaigns != Null.nil)
            {
                campaignsMap.put(campaigns.car().second(),
                                 campaigns.car().car().unboxLong());
                campaigns = campaigns.cdr();
            }
            for(Sexpression s: yetToProcess)
            {
                Long match = campaignsMap.get(s);
                if(match != null)
                {
                    Long l = s.unboxLong();
                    InspectorItem i =
                            new InspectorItem
                                    (l, l.toString(),
                                     "../" + BidHistoryHTTPHandler.urlName +
                                     "/" + BidHistoryHTTPHandler.urlName +
                                     "?advertiserId=" + match +
                                     "&campaignId=" + l);
                    SQLHTTPHandler.ensureMapEntry(s, mappedObjTable).add(i);
                }
                else if(advertiserMap.contains(s))
                {
                    Long l = s.unboxLong();
                    InspectorItem i =
                            new InspectorItem
                                    (l, l.toString(),
                                     "../" + BidHistoryHTTPHandler.urlName +
                                     "/" + BidHistoryHTTPHandler.urlName +
                                     "?advertiserId=" + l);
                    SQLHTTPHandler.ensureMapEntry(s, mappedObjTable).add(i);
                }
                else {}
            }
        }
    }
}

@SuppressWarnings("unused")
class MissingZeroResRow {
    Long advertiserId;
    Long campaignId;
    Date observationTime;

    MissingZeroResRow(Long advertiserId, Long campaignId, Date observationTime)
    {
        this.advertiserId = advertiserId;
        this.campaignId = campaignId;
        this.observationTime = observationTime;
    }
}

@SuppressWarnings("unused")
class MissingSeqNumResRow {
    Long advertiserId;
    Long campaignId;
    Date observationTime;

    MissingSeqNumResRow(Long advertiserId, Long campaignId,
                        Date observationTime)
    {
        this.advertiserId = advertiserId;
        this.campaignId = campaignId;
        this.observationTime = observationTime;
    }
}

class IndexCounter {
    int index = 0;

    public synchronized int getIndex()
    {
        return index;
    }

    public synchronized int increment()
    {
        index = index + 1;
        return index;
    }
}



class FetchCampaignDataThunk extends ParallelJobThunk {

    Bidder bidder;
    QueryContext qctx;
    SQLContext sctx;
    String[] selectedAdvertisers;
    Set<String> selectedCampaigns;
    IndexCounter advertiserIndex;
    Map<String, BidderInstruction> instructions;
    AdvertiserService advertiser;
    Map<Long, AdvertiserData> advertiserDataMap;
    Map<Long, AdvertiserData> currentAdvertiserDataMap;
    boolean selectExpiredCampaigns;
    Date endsGTETime;
    Date instructionsDate;
    Map<Long, Set<Long>> readyToForceStrategy;
    int thunkIndex;

    public FetchCampaignDataThunk
            (Bidder bidder, QueryContext qctx, SQLContext sctx,
             String[] selectedAdvertisers, Set<String> selectedCampaigns,
             IndexCounter advertiserIndex,
             Map<String, BidderInstruction> instructions,
             AdvertiserService advertiser,
             Map<Long, AdvertiserData> advertiserDataMap,
             Map<Long, AdvertiserData> currentAdvertiserDataMap,
             boolean selectExpiredCampaigns,
             Date endsGTETime, Date instructionsDate,
             Map<Long, Set<Long>> readyToForceStrategy, int thunkIndex)
    {
        this.bidder = bidder;
        this.qctx = qctx;
        this.sctx = sctx;
        this.selectedAdvertisers = selectedAdvertisers;
        this.selectedCampaigns = selectedCampaigns;
        this.advertiserIndex = advertiserIndex;
        this.instructions = instructions;
        this.advertiser = advertiser;
        this.advertiserDataMap = advertiserDataMap;
        this.currentAdvertiserDataMap = currentAdvertiserDataMap;
        this.selectExpiredCampaigns = selectExpiredCampaigns;
        this.endsGTETime = endsGTETime;
        this.instructionsDate = instructionsDate;
        this.readyToForceStrategy = readyToForceStrategy;
        this.thunkIndex = thunkIndex;
    }

    public Object[] getBarfArgs()
    {
        Object[] res;
        res = new Object[]
                {
                        bidder,
                        qctx,
                        sctx,
                        selectedAdvertisers,
                        selectedCampaigns,
                        advertiserIndex,
                        instructions,
                        advertiser,
                        advertiserDataMap,
                        currentAdvertiserDataMap,
                        selectExpiredCampaigns,
                        endsGTETime,
                        instructionsDate,
                        readyToForceStrategy,
                        thunkIndex
                };
        return res;
    }

    public void doit()
    {
        bidder.fetchCampaignDataForAdvertiser
                (qctx, sctx, selectedAdvertisers,
                 selectedCampaigns, advertiserIndex, instructions,
                 advertiser, advertiserDataMap,
                 currentAdvertiserDataMap, selectExpiredCampaigns,
                 endsGTETime, instructionsDate, readyToForceStrategy);
    }

    public String toString()
    {
        return "#<FetchCampaignDataThunk (" + thunkIndex + ") " +
               advertiser.getName() + ">";
    }
}

class BidderCatchThunk extends CatchThunk {
    ParallelExecuter parent;
    Bidder bidder;

    public BidderCatchThunk(ParallelExecuter parent)
    {
        super(parent);
        this.parent = parent;
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public void doit(Runner runner, Throwable throwable)
    {
        parent.addThrown(throwable);
        if(throwable instanceof BusyException)
        {
            Utils.logThisPoint(Level.ERROR, "Bidder was busy");
        }
        else
        {
            Utils.barf(throwable, this, bidder, runner.getBarfArgs());
        }
    }
}