package com.tumri.cbo.monitor;

import com.tumri.cbo.backend.AdvertiserData;
import com.tumri.cbo.backend.Bidder;
import com.tumri.cbo.backend.CampaignData;
import com.tumri.mediabuying.appnexus.AppNexusUtils;
import com.tumri.mediabuying.appnexus.Identity;
import com.tumri.mediabuying.zini.*;
import java.io.IOException;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.util.*;

public abstract class AbstractMonitor {

    String name;
    static boolean verbose = false;
    static final Comparator<AbstractMonitor> COMPARATOR =
            new MonitorComparator();
    public static final int TEXT_INDEX = 0;
    public static final int HTML_INDEX = 1;
    public static final int MESSAGES_SIZE =
            Math.max(TEXT_INDEX, HTML_INDEX) + 1;
    public String getName()
    {
        return name;
    }

    @SuppressWarnings("unused")
    private AbstractMonitor() {}

    List<ReportApplicabilityColumn> includedColumns;
    List<MessageReport> applicableReports;

    public AbstractMonitor(List<ReportApplicabilityColumn> includedColumns)
    {
        this.name = AppNexusUtils.afterDot(this.getClass().getName());
        this.includedColumns = includedColumns;
    }

    public AbstractMonitor(MessageReport[] applicableReports,
                           ReportApplicabilityColumn... includedColumns)
    {
        this.name = AppNexusUtils.afterDot(this.getClass().getName());
        this.includedColumns = Arrays.asList(includedColumns);
        this.applicableReports =
                (applicableReports == null
                        ? null
                        :Arrays.asList(applicableReports));
    }

    static final DecimalFormat percentageFormat = new DecimalFormat("#.0%");

    static String asPercent(double d)
    {
        return percentageFormat.format(d);
    }

    static final DecimalFormat oneDecimalFormat = new DecimalFormat("#.0");

    static String asOneDecimal(double d)
    {
        return oneDecimalFormat.format(d);
    }

    static final DecimalFormat twoDecimalFormat = new DecimalFormat("#.00");

    @SuppressWarnings("unused")
    static String asTwoDecimals(double d)
    {
        return twoDecimalFormat.format(d);
    }

    private static Map<String, AbstractMonitor> knownMonitors =
            new HashMap<String, AbstractMonitor>();

    public void check(Bidder bidder, SQLConnector connector, QueryContext qctx,
                      SQLContext sctx, Map<Long, List<CampaignData>> map,
                      Date now, TimeZone localTz)
    {
        if(bidder != null && connector != null && qctx != null && map != null)
        {
            int count = 0;
            boolean newLineThrownP = true;
            int modulus = 5;
            AbstractProblem globalProb =
                checkGlobal(connector, qctx, sctx, now, localTz);
            if(globalProb == null && verbose)
            {
                System.out.println("Global check OK.");
            }
            for(Long advertiserId: map.keySet())
            {
                List<CampaignData> campaigns = map.get(advertiserId);
                AbstractProblem advProb =
                    checkAdvertiser(advertiserId, connector, qctx, sctx, now,
                                    localTz);
                if(advProb == null && verbose)
                {
                    System.out.println("Advertiser " + advertiserId + " OK.");
                }
                for(CampaignData cd: campaigns)
                {
                    AbstractProblem campProb =
                        checkCampaign(advertiserId, cd, connector, qctx,
                                      sctx, now, localTz);
                    if(campProb == null && verbose)
                    {
                        System.out.println
                                ("Campaign " + briefName(cd) + " OK.");
                    }
                }
                count = count + 1;
                newLineThrownP = ProgressNoter.noteProgress
                     (null, System.out, count, newLineThrownP, modulus, false);
            }
        }
    }

    public String briefName(CampaignData cd)
    {
        return cd.getCampaignName() + " (" + cd.getAdvertiserId() + "/" +
               cd.getCampaignId() + ")";
    }

    @SuppressWarnings("unused")
    public String briefName(AdvertiserData ad)
    {
        return ad.getName() + " (" + ad.getId() + ")";
    }

    abstract public AbstractProblem result
            (String message, CampaignData cd, Object... arguments);

    static Map<AbstractProblem, Map<AbstractMonitor, List<AbstractProblem>>>
            recordedProblems = null;

    static void resetProblemRecording()
    {
        recordedProblems =
                new HashMap<AbstractProblem,
                            Map<AbstractMonitor, List<AbstractProblem>>>();
    }

    AbstractProblem recordProblem(AbstractProblem problem)
    {
        AbstractProblem problemPrototype = problem.prototype();
        Map<AbstractMonitor, List<AbstractProblem>> monitorMap =
                recordedProblems.get(problemPrototype);
        if(monitorMap == null)
        {
            monitorMap = new HashMap<AbstractMonitor, List<AbstractProblem>>();
            recordedProblems.put(problemPrototype, monitorMap);
        }
        AbstractMonitor key = problem.getMonitor();
        List<AbstractProblem> list = monitorMap.get(key);
        if(list == null)
        {
            list = new Vector<AbstractProblem>();
            monitorMap.put(key, list);
        }
        list.add(problem);
        return problem;
    }

    public abstract String heading();

    public abstract String shortHeading();

    static final long MILLISECONDS_PER_DAY = 24 * 3600 * 1000;

    // static long daysCountingAsRecent = 2;
    
    static boolean recentEnough(long now, Date observationTime,
                                long daysCountingAsRecent)
    {
        // An observation is recent enough if it happened in the last day.
        // These are always the max time value, so anything else indicates that
        // the campaign is inactive or isn't serving.
        return (now - observationTime.getTime()) <=
                (daysCountingAsRecent * MILLISECONDS_PER_DAY);
    }

    boolean applicableToColumns(MessageReport report)
    {
        // Example: report is for_users and this.includedColumns.contains
        //    (ReportApplicabilityColumn.FOR_USERS)
        // All includedColumns in this monitor MUST be present in the report.
        for(ReportApplicabilityColumn col: includedColumns)
        {
            if(!report.getIncludeColumns().contains(col)) return false;
        }
        return applicableReports == null || applicableReports.contains(report);
    }

    boolean applicableTo(MessageReport report)
    {
        return applicableToColumns(report);
    }

    public static Messages checkAllInternal
            (Bidder bidder, Date now, TimeZone localTz, boolean admin,
             MessageReport report, ProblemReporter reporter,
             long daysCountingAsRecent)
            // Return the txt and html formats of the messages.
    {
        Messages res = null;
        if(bidder != null)
        {
            resetProblemRecording();
            SQLConnector connector = bidder.ensureBidderSQLConnector();
            QueryContext qctx = new BasicQueryContext
                                    (null, bidder.getAppNexusTheory());
            SQLContext sctx = null;
            Identity ident = bidder.getAppNexusIdentity();
            long nowTime = now.getTime();
            try
            {
                sctx = connector.allocateSQLContext(qctx);
                String query =
                "SELECT t1.advertiser_id,  t1.observation_time,\n" +
                "       t1.line_item_json, t1.line_item_profile_json,\n" +
                "       t1.campaign_json,  t1.campaign_profile_json\n" +
                "FROM (SELECT advertiser_id, campaign_id,\n" +
                "             MAX(observation_time) AS observation_time\n" +
                "      FROM observeddata o\n" +
                "      GROUP BY advertiser_id, campaign_id\n" +
                "      ORDER BY advertiser_id, campaign_id) t2,\n" +
                "     observeddata t1\n" +
                "WHERE t1.advertiser_id = t2.advertiser_id\n" +
                "AND   t1.campaign_id = t2.campaign_id\n" +
                "AND   t1.observation_time = t2.observation_time\n" +
                "ORDER BY t1.advertiser_id, t1.campaign_id";
                Sexpression rows = connector.runSQLQuery(query, qctx);
                Map<Long, List<CampaignData>> map =
                        new HashMap<Long, List<CampaignData>>();
                while(rows != Null.nil)
                {
                    Sexpression row = rows.car();
                    CampaignData cd =
                         bidder.cdFromObservedDataRow(ident, qctx, sctx, row);
                    Date observationTime = row.second().unboxDate();
                    Long advertiserId = row.car().unboxLong();
                    List<CampaignData> campaigns = map.get(advertiserId);
                    if(cd != null && recentEnough(nowTime, observationTime,
                                                  daysCountingAsRecent))
                    {
                        if(campaigns == null)
                        {
                            campaigns = new Vector<CampaignData>();
                            map.put(advertiserId, campaigns);
                        }
                        campaigns.add(cd);
                    }
                    // else System.out.println("No campaign.");
                    rows = rows.cdr();
                }
                List<AbstractMonitor> mons1 =
                        new Vector<AbstractMonitor>(knownMonitors.values());
                List<AbstractMonitor> mons =
                        new Vector<AbstractMonitor>();
                for(AbstractMonitor mon: mons1)
                {
                    if(mon.applicableTo(report))
                    {
                        mons.add(mon);
                    }
                }
                Collections.sort(mons, COMPARATOR);
                for(AbstractMonitor mon: mons)
                {
                    if(mon.applicableTo(report))
                    {
                        long startTime = new Date().getTime();
                        System.out.print(mon.getName() + " ");
                        System.out.flush();
                        mon.check(bidder, connector, qctx, sctx, map,
                                now, localTz);
                        long time = new Date().getTime() - startTime;
                        System.out.println(" - " + time / 1000 + "s");
                    }
                }
                try
                {
                    Map<Object, String> key = new HashMap<Object, String>();
                    // Do it in HTML mode.
                    // Wrap in <HTML> tags and such.
                    StringWriter sw = new StringWriter();
                    reporter.summariseProblems
                            (sw, recordedProblems, key, true, admin);
                    String html = sw.toString();
                    //-----------------------------
                    // Do it in text mode.
                    sw = new StringWriter();
                    reporter = new OrderByCampaignProblemReporter();
                    reporter.summariseProblems
                            (sw, recordedProblems, key, false, admin);
                    sw.append("\n");
                    String text = sw.toString();
                    if(text == null || "".equals(text) || "\n".equals(text))
                        res = new Messages(null, null, report);
                    else res = new Messages(text, html, key, report);
                }
                catch (IOException e)
                {
                    throw Utils.barf(e, null);
                }
                return res;
            }
            finally
            {
                if(sctx != null) connector.deallocateSQLContext(sctx);
            }
        }
        else return res;
    }

    static final String CM_PREFIX = "cm.appnexus/";

    public static String removeTrailingId(String s)
    {
        int pos = s.lastIndexOf(" (");
        if(pos > 0)
            return s.substring(0, pos);
        else return s;
    }

    public static String simplifyCampaignName
            (String advertiserName, String campaignName)
    {
        campaignName = campaignName.trim();
        if(campaignName.startsWith(advertiserName))
            return simplifyCampaignName
                        (advertiserName,
                         campaignName.substring(advertiserName.length()));
        else if(campaignName.startsWith(CM_PREFIX))
            return simplifyCampaignName
                        (advertiserName,
                         campaignName.substring(CM_PREFIX.length()));
        else return campaignName;
    }

    public static void addMonitor(AbstractMonitor monitor)
    {
        knownMonitors.put(monitor.getName(), monitor);
    }

    // Nothing to do by default.
    @SuppressWarnings("unused")
    public AbstractProblem checkGlobal
            (SQLConnector connector, QueryContext qctx, SQLContext sctx,
             Date now, TimeZone localTz)
    {
        return null;
    }

    // Nothing to do by default.
    @SuppressWarnings("unused")
    public AbstractProblem checkAdvertiser
            (Long advertiserId, SQLConnector connector,
             QueryContext qctx, SQLContext sctx, Date now, TimeZone localTz)
    {
        return null;
    }

    @SuppressWarnings("unused")
    public AbstractProblem checkCampaign
            (Long advertiserId, CampaignData campaign, SQLConnector connector,
             QueryContext qctx, SQLContext sctx, Date now, TimeZone localTz)
    {
        return null;
    }

    public long uSecsOffsetFromLocal(TimeZone localTz, TimeZone tz)
    {
        long localOffset = localTz.getRawOffset();
        long tzOffset = tz.getRawOffset();
        return (localOffset - tzOffset) * 1000;
    }
}

class MonitorComparator implements Comparator<AbstractMonitor> {

    public int compare(AbstractMonitor x, AbstractMonitor y)
    {
        return x.getName().compareTo(y.getName());
    }
}