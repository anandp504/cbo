package com.tumri.cbo.backend;

import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.tumri.af.context.TimeScale;
import com.tumri.af.utils.DateUtils;
import com.tumri.mediabuying.appnexus.AppNexusUtils;
import com.tumri.mediabuying.appnexus.BidSpec;
import com.tumri.mediabuying.appnexus.Identity;
import com.tumri.mediabuying.appnexus.agent.AppNexusTheory;
import com.tumri.mediabuying.zini.*;
import org.json.simple.JSONObject;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class PerformanceHistoryDAOImpl implements PerformanceHistoryDAO {

    //=========================================================================
    Bidder bidder;
    AppNexusTheory appNexusTheory;
    Identity ident;
    static final SynchDateFormat sqlDateParser =
            new SynchDateFormat("yyyy-MM-dd HH:mm:ss");
    static final long HOURLY_REPORTING_INTERVAL =
            100L * 24L * 3600000L; //100 days
    SQLConnector connector;

    PerformanceHistoryDAOImpl(Bidder theBidder)
    {
        bidder = theBidder;
        appNexusTheory = bidder.appNexusTheory;
        ident = bidder.getAppNexusIdentity();
        connector = bidder.ensureBidderSQLConnector();
    }

    String inUTC(Date date)
    {
        synchronized(sqlDateParser)
        {
            return sqlDateParser.format(connector.reverseCorrectSQLDate(date));
        }
    }

    static String makeTimeScaleReturnVal
        (TimeScale ts, String colName, int hourOffset, int localOffset,
         SQLConnector connector)
    {
        String col =
                (connector.getTimeZone() == null
                        ? // colName
                          "date_add(" + colName + ", INTERVAL " +
                                      (hourOffset - localOffset) +
                                    " HOUR)"
                        : // This version is necessary if the SQLConnector has
                          // a defined TimeZone.
                          "date_add(" + colName + ", INTERVAL " + hourOffset +
                                    " HOUR)");
        return
        (ts == TimeScale.HOURLY
            ? "DATE_FORMAT(" + col + ", '%Y-%m-%d %H') AS TimeScale" : "") +
        (ts == TimeScale.DAILY
            ? "DATE_FORMAT(" + col + ", '%Y-%m-%d') AS TimeScale" : "") +
        (ts == TimeScale.WEEKLY
            ? "DATE_FORMAT(" + col + ", '%Y-%U') AS TimeScale" : "") +
        (ts == TimeScale.MONTHLY
            ? "DATE_FORMAT(" + col + ", '%Y-%m') AS TimeScale" : "") +
        (ts == TimeScale.QUARTERLY
            ? "CONCAT(DATE_FORMAT(" + col + ", '%Y-Q'), QUARTER(" + col + ")) AS TimeScale" : "") +
        (ts == TimeScale.YEARLY
            ? "DATE_FORMAT(" + col + ", '%Y') AS TimeScale" : "");
    }

    static String rollUpDate(long utcTimestamp, TimeZone tz, TimeScale ts)
    {
        Date thisDate = new Date(utcTimestamp * 1000);
        String res;
        res = TimeScaleIterator.rollUpDate(thisDate, tz, ts);
        // System.out.println(res + " <- " + thisDate + ", UTC: " + utcTimestamp + ", TZ: " + tz.getID() + ", TS: " + ts);
        return res;
    }

    String aggregatedHistDataQuery
            (Long advertiserId, Long campaignId, TimeScale ts,
             Date startDate, Date endDate, int hourOffset,
             int localOffset, SQLConnector connector)
    {
        return
    "SELECT " + makeTimeScaleReturnVal
                        (ts, "hour", hourOffset, localOffset, connector) +
    ",\n" +
    "       SUM(imps) AS Impressions, SUM(cost) AS Cost\n" +
    "FROM historicaldata\n" +
    "WHERE 1 = 1\n" +
    (advertiserId == null ? "":"AND   advertiser_id = " + advertiserId + "\n")+
    (campaignId == null ? "" : "AND   campaign_id = " + campaignId + "\n") +
    "AND   hour BETWEEN '" + inUTC(startDate) +
    "' AND '" + inUTC(endDate) + "'\n" +
    "GROUP BY advertiser_id, campaign_id, TimeScale\n" +
    "ORDER BY advertiser_id, campaign_id, TimeScale";
    }

    // This has been modified but is untested.
    // It is only called by queryForCampaignPerformanceSQLSide
    // and that method never appears to be used.
    String aggregatedEntropyQuery
            (Long advertiserId, Long campaignId, TimeScale ts,
             Date startDate, Date endDate, int hourOffset,
             int localOffset, SQLConnector connector)
    {
        String query = 
        "SELECT " + makeTimeScaleReturnVal(ts, "day", hourOffset, localOffset, connector) + ",\n" +
                           CampaignData.ENTROPY_QUERY_EXPRESSION + "\n" +
        "  FROM network_site_domain_performance\n" +
        " WHERE 1 = 1\n" +
        (advertiserId == null ? "":"AND   advertiser_id = " + advertiserId + "\n")+
        (campaignId == null ? "" : "AND   campaign_id = " + campaignId + "\n") +
        "  AND day BETWEEN '" + inUTC(startDate) +
        "' AND '" + inUTC(endDate) + "'\n" +
        "GROUP BY advertiser_id, campaign_id, TimeScale\n" +
        "ORDER BY advertiser_id, campaign_id, TimeScale";
        
        System.err.println("Entropy Query 3: " + query);
        return query;
    }

    String aggregatedBidHistoryQuery
            (Long advertiserId, Long campaignId, TimeScale ts,
             Date startDate, Date endDate, int hourOffset,
             int localOffset, SQLConnector connector)
    {
        return
    "SELECT " + makeTimeScaleReturnVal
                      (ts, "event_time", hourOffset, localOffset, connector) +
    ",\n" +
    "       AVG(daily_impression_budget) AS Budget,\n" +
    "       AVG(daily_impression_target) AS Target\n" +
    "FROM bidhistory\n" +
    "WHERE 1 = 1\n" +
    (advertiserId == null ? "":"AND   advertiser_id = " + advertiserId + "\n")+
    (campaignId == null ? "" : "AND   campaign_id = " + campaignId + "\n") +
    "AND   event_time BETWEEN '" + inUTC(startDate) +
    "' AND '" + inUTC(endDate) + "'\n" +
    "GROUP BY advertiser_id, campaign_id, TimeScale\n" +
    "ORDER BY advertiser_id, campaign_id, TimeScale";
    }

    String utcHistDataQuery
            (Long advertiserId, Long campaignId, Date startDate, Date endDate)
    {
        return
    "SELECT unix_timestamp(hour),\n" +
    "       imps AS Impressions, cost AS Cost\n" +
    "FROM historicaldata\n" +
    "WHERE 1 = 1\n" +
    (advertiserId == null ? "":"AND   advertiser_id = " + advertiserId + "\n")+
    (campaignId == null ? "" : "AND   campaign_id = " + campaignId + "\n") +
    "AND   hour BETWEEN '" + inUTC(startDate) +
    "' AND '" + inUTC(endDate) + "'\n" +
    "ORDER BY hour ASC;";
    }

    String utcEntropyQuery
            (Long advertiserId, Long campaignId, Date startDate, Date endDate)
    {
    	String query = 
    	    "SELECT unix_timestamp(day),\n" +
    	    "       " + CampaignData.ENTROPY_QUERY_EXPRESSION + "\n" +
    	    "  FROM network_site_domain_performance\n" +
    	    " WHERE 1 = 1\n" +
    	    (advertiserId == null ? "":"   AND advertiser_id = " + advertiserId + "\n")+
    	    (campaignId == null ? "" : "   AND campaign_id = " + campaignId + "\n") +
    	    "   AND day BETWEEN '" + inUTC(startDate) +
    	    "' AND '" + inUTC(endDate) + "'\n" +
    	    "GROUP BY day\n" +
    	    "ORDER BY day ASC";
        return query;

    }

    String utcBidHistoryQuery
            (Long advertiserId, Long campaignId, Date startDate, Date endDate)
    {
        return
    "SELECT unix_timestamp(event_time),\n" +
    "       daily_impression_budget AS Budget, daily_impression_target AS Target\n" +
    "FROM bidhistory\n" +
    "WHERE 1 = 1\n" +
    (advertiserId == null ? "":"AND   advertiser_id = " + advertiserId + "\n")+
    (campaignId == null ? "" : "AND   campaign_id = " + campaignId + "\n") +
    "AND   event_time BETWEEN '" + inUTC(startDate) +
    "' AND '" + inUTC(endDate) + "'\n" +
    "ORDER BY event_time ASC;";
    }

    static final int combinedJSONIndex = 6;
    static final int campaignChangeCountStartIndex = combinedJSONIndex + 1;

    String aggregatedObservedDataQuery
            (Long advertiserId, Long campaignId, TimeScale ts,
             Date startDate, Date endDate, int hourOffset,
             int localOffset, SQLConnector connector)
    {
        return
    "SELECT " + makeTimeScaleReturnVal
                 (ts, "observation_time", hourOffset, localOffset, connector) +
    ",\n" +
    "       AVG(base_bid) AS BaseBid, AVG(max_bid) AS MaxBid,\n" +
    "       AVG(daily_impressions_budget) AS DailyImpressionTarget,\n" +
    "       AVG(lifetime_impressions_budget) AS LifetimeImpressionTarget,\n" +
    "       MAX(combined_json) AS CombinedJSON,\n" +
    "       SUM(attribute_changed_but_will_not_affect_delivery) AS attribute_changed_but_will_not_affect_delivery,\n" +
    "       SUM(attribute_changed_with_unknown_effect_on_delivery) AS attribute_changed_with_unknown_effect_on_delivery,\n" +
    "       SUM(attribute_changed_increases_delivery) AS attribute_changed_increases_delivery,\n" +
    "       SUM(attribute_changed_decreases_delivery) AS attribute_changed_decreases_delivery,\n" +
    "       SUM(attribute_increased_increases_delivery) AS attribute_increased_increases_delivery,\n" +
    "       SUM(attribute_decreased_decreases_delivery) AS attribute_decreased_decreases_delivery,\n" +
    "       SUM(attribute_increased_decreases_delivery) AS attribute_increased_decreases_delivery,\n" +
    "       SUM(attribute_decreased_increases_delivery) AS attribute_decreased_increases_delivery,\n" +
    "       SUM(targeting_widened_increases_delivery) AS targeting_widened_increases_delivery,\n" +
    "       SUM(targeting_narrowed_decreases_delivery) AS targeting_narrowed_decreases_delivery\n" +
    "FROM observeddata\n" +
    "WHERE 1 = 1\n" +
    (advertiserId == null ? "":"AND   advertiser_id = " + advertiserId + "\n")+
    (campaignId == null ? "" : "AND   campaign_id = " + campaignId + "\n") +
    "AND   observation_time BETWEEN '" + inUTC(startDate) +
    "' AND '" + inUTC(endDate) + "'\n" +
    "GROUP BY advertiser_id, campaign_id, TimeScale\n" +
    "ORDER BY advertiser_id, campaign_id, TimeScale";
    }

    String utcObservedDataQuery
            (Long advertiserId, Long campaignId, Date startDate, Date endDate)
    {
        return
    "SELECT unix_timestamp(observation_time),\n" +
    "       base_bid AS BaseBid,\n" +
    "       max_bid AS MaxBid,\n" +
    "       daily_impressions_budget AS DailyImpressionTarget,\n" +
    "       lifetime_impressions_budget AS LifetimeImpressionTarget,\n" +
    "       combined_json AS CombinedJSON,\n" +
    "       attribute_changed_but_will_not_affect_delivery AS attribute_changed_but_will_not_affect_delivery,\n" +
    "       attribute_changed_with_unknown_effect_on_delivery AS attribute_changed_with_unknown_effect_on_delivery,\n" +
    "       attribute_changed_increases_delivery AS attribute_changed_increases_delivery,\n" +
    "       attribute_changed_decreases_delivery AS attribute_changed_decreases_delivery,\n" +
    "       attribute_increased_increases_delivery AS attribute_increased_increases_delivery,\n" +
    "       attribute_decreased_decreases_delivery AS attribute_decreased_decreases_delivery,\n" +
    "       attribute_increased_decreases_delivery AS attribute_increased_decreases_delivery,\n" +
    "       attribute_decreased_increases_delivery AS attribute_decreased_increases_delivery,\n" +
    "       targeting_widened_increases_delivery AS targeting_widened_increases_delivery,\n" +
    "       targeting_narrowed_decreases_delivery AS targeting_narrowed_decreases_delivery\n" +
    "FROM observeddata\n" +
    "WHERE 1 = 1\n" +
    (advertiserId == null ? "":"AND   advertiser_id = " + advertiserId + "\n")+
    (campaignId == null ? "" : "AND   campaign_id = " + campaignId + "\n") +
    "AND   observation_time BETWEEN '" + inUTC(startDate) +
    "' AND '" + inUTC(endDate) + "'\n" +
    "ORDER BY observation_time ASC;";
    }

    String aggregatedEventQuery
            (Long advertiserId, Long campaignId, TimeScale ts,
             Date startDate, Date endDate, int hourOffset, int localOffset,
             SQLConnector connector)
    {
        return
    "SELECT " + makeTimeScaleReturnVal
                        (ts, "event_time", hourOffset, localOffset, connector)
              + ",\n" +
    "       event_type, description, event_time\n" +
    "FROM events\n" +
    "WHERE 1 = 1\n" +
    (advertiserId == null ? "":"AND   advertiser_id = " + advertiserId + "\n")+
    (campaignId == null ? "" : "AND   campaign_id = " + campaignId + "\n") +
    "AND   event_time BETWEEN '" + inUTC(startDate) +
    "' AND '" + inUTC(endDate) + "'\n" +
    "ORDER BY advertiser_id, campaign_id, TimeScale";
    }

    String utcEventQuery
            (Long advertiserId, Long campaignId, Date startDate, Date endDate)
    {
        return
    "SELECT unix_timestamp(event_time),\n" +
    "       event_type, description, event_time\n" +
    "FROM events\n" +
    "WHERE 1 = 1\n" +
    (advertiserId == null ? "":"AND   advertiser_id = " + advertiserId + "\n")+
    (campaignId == null ? "" : "AND   campaign_id = " + campaignId + "\n") +
    "AND   event_time BETWEEN '" + inUTC(startDate) +
    "' AND '" + inUTC(endDate) + "'" +
    "ORDER BY event_time ASC;";
    }

    List<PerformanceHistoryRow> totaliseResults
            (Map<String, PerformanceHistoryRow> histResults,
             Map<String, BidParameters> bidParameterResults,
             TimeScale ts, TimeZone wrtTimeZone, Date startDate, Date endDate)
    {
        List<PerformanceHistoryRow> res = new Vector<PerformanceHistoryRow>();
        Iterator<String> it =
                new TimeScaleIterator(ts, startDate, endDate, wrtTimeZone,
                                      bidder.getCurrentTime());
        PerformanceHistoryRow lastHistMatch = null;
        BidParameters lastBidParamsMatch = null;
        while(it.hasNext())
        {
            String key = it.next();
            PerformanceHistoryRow histMatch = histResults.get(key);
            BidParameters paramsMatch = bidParameterResults.get(key);
            if(histMatch != null)
            {
                histMatch.setBidParameters
                        ((paramsMatch == null
                                ? lastBidParamsMatch
                                : paramsMatch));
                BidResponse br = histMatch.getBidResponse();
                if(br == null && lastHistMatch != null)
                    histMatch.setBidResponse(lastHistMatch.getBidResponse());
                else if(br != null && lastHistMatch != null)
                {
                    if(br.getEntropy() == null)
                        br.setEntropy
                                (lastHistMatch.getBidResponse().getEntropy());
                    if(br.getImpressionTarget() == null)
                        br.setImpressionTarget
                            (lastHistMatch.getBidResponse().
                                    getImpressionTarget());
                    if(br.getImpressionBudget() == null)
                        br.setImpressionBudget
                            (lastHistMatch.getBidResponse().
                                    getImpressionBudget());
                }
                res.add(histMatch);
                lastHistMatch = histMatch;
            }
            else res.add(new PerformanceHistoryRow
                                (lastHistMatch,
                                 (paramsMatch == null
                                    ? lastBidParamsMatch
                                    : paramsMatch),
                                 key,
                                 dateFromTimeString(key, wrtTimeZone, ts)));
            if(paramsMatch != null) lastBidParamsMatch = paramsMatch;
        }
        return res;
    }

    BidderEvent.EventType mapEventType(String s)
    {
        if("Bid".equals(s)) return BidderEvent.EventType.BID_PLACED; // todo
        else return BidderEvent.EventType.BID_PLACED; 
    }

    static final SynchDateFormat hourlyFormat =
            new SynchDateFormat("yyyy-MM-dd HH z");
    static final SynchDateFormat dailyFormat =
            new SynchDateFormat("yyyy-MM-dd z");
    static final SynchDateFormat weeklyFormat =
            new SynchDateFormat("yyyy-w z");
    static final SynchDateFormat monthlyFormat =
            new SynchDateFormat("yyyy-MM z");
    static final SynchDateFormat yearlyFormat =
            new SynchDateFormat("yyyy z");

    static Date dateFromTimeString
            (String timeString, TimeZone tz, TimeScale ts)
    {
        Date d;
        try
        {
            String s = timeString + " "+ tz.getDisplayName();
            if(ts == TimeScale.HOURLY)  d =  hourlyFormat.parse(s);
            else if(ts == TimeScale.DAILY)   d =   dailyFormat.parse(s);
            else if(ts == TimeScale.WEEKLY)  d =  weeklyFormat.parse(s);
            else if(ts == TimeScale.MONTHLY) d = monthlyFormat.parse(s);
            else if(ts == TimeScale.QUARTERLY)
            {
                Pattern p = Pattern.compile("(\\d{4})-Q(\\d)");
                Matcher m = p.matcher(timeString);
                if(m.find())
                {
                    String yearString = m.group(1);
                    String quarterString = m.group(2);
                    if(yearString != null && quarterString != null)
                    {
                        int year = Integer.parseInt(yearString);
                        int quarter = Integer.parseInt(quarterString);
                        int month = ((quarter - 1) * 3) + 1;
                        s = year + "-" + month + " "+ tz.getDisplayName();
                        d = monthlyFormat.parse(s);
                    }
                    else throw Utils.barf("Mal-formatted QUARTERLY value",
                                          null, timeString, tz, ts);
                }
                else throw Utils.barf("Mal-formatted QUARTERLY value",
                                      null, timeString, tz, ts);
            }
            else if(ts == TimeScale.YEARLY) d = yearlyFormat.parse(s);
            else throw Utils.barf("Unknown timescale", null, ts);
        }
        catch (ParseException e)
        {
            throw Utils.barf("Illegal time syntax", null, timeString, ts, tz);
        }
        return d;
    }

    List<PerformanceHistoryRow> getPerformanceHistoryRows
            (String eventQuery, String histQuery, String entropyQuery,
             String bidHistoryQuery, String observedQuery, long advertiserId,
             long campaignId, TimeScale ts, TimeZone wrtTimeZone,
             Date startDate, Date endDate, QueryContext qctx)
    {
        SQLConnector connector = bidder.ensureBidderSQLConnector();
        Map<String, Collection<BidderEvent>> eventMap =
                new HashMap<String, Collection<BidderEvent>>();
        Map<String, PerformanceHistoryRow> intermediateResults =
                new HashMap<String, PerformanceHistoryRow>();
        Map<String, BidParameters> bidParameterResults =
                new HashMap<String, BidParameters>();
        //-----------------------------------------------
        class GetEventsThunk extends SQLThunk {

            Map<String, Collection<BidderEvent>> results;
            long advertiserId;
            long campaignId;
            TimeScale ts;
            Date startDate;
            Date endDate;
            SQLConnector connector;

            GetEventsThunk(SQLConnector connector,
                           Map<String, Collection<BidderEvent>> results,
                           long advertiserId, long campaignId,
                           TimeScale ts, Date startDate, Date endDate)
            {
                super(connector);
                this.results = results;
                this.advertiserId = advertiserId;
                this.campaignId = campaignId;
                this.ts = ts;
                this.startDate = startDate;
                this.endDate = endDate;
                this.connector = connector;
            }

            public void doit(ResultSet rs) throws SQLException
            {
                String time = rs.getString(1);
                Collection<BidderEvent> timeEntry = results.get(time);
                if(timeEntry == null)
                {
                    timeEntry = new Vector<BidderEvent>();
                    results.put(time, timeEntry);
                }
                BidderEvent e = new BidderEvent();
                e.setAdvertiserId(advertiserId);
                e.setCampaignId(campaignId);
                e.setEventType(mapEventType(rs.getString(2)));
                e.setDescription(rs.getString(3));
                e.setEventTime(connector.correctSQLDate(rs.getTimestamp(4)));
                timeEntry.add(e);
            }
        }
        //-----------------------------------------------
        class GetHistoricalDataThunk extends SQLThunk {

            Map<String, PerformanceHistoryRow> results;
            Map<String, Collection<BidderEvent>> eventMap;
            long advertiserId;
            long campaignId;
            TimeScale ts;
            TimeZone tz;
            Date startDate;
            Date endDate;

            GetHistoricalDataThunk
                    (SQLConnector connector,
                     Map<String, PerformanceHistoryRow> results,
                     Map<String, Collection<BidderEvent>> eventMap,
                     long advertiserId, long campaignId, TimeScale ts,
                     TimeZone tz, Date startDate, Date endDate)
            {
                super(connector);
                this.results = results;
                this.eventMap = eventMap;
                this.advertiserId = advertiserId;
                this.campaignId = campaignId;
                this.ts = ts;
                this.tz = tz;
                this.startDate = startDate;
                this.endDate = endDate;
            }

            public void doit(ResultSet rs) throws SQLException
            {
                String time = rs.getString(1);
                Collection<BidderEvent> eventMatch = eventMap.get(time);
                if(eventMatch == null)
                    eventMatch = new Vector<BidderEvent>();
                PerformanceHistoryRow row =
                        new PerformanceHistoryRow
                                (null,
                                 new TrivialBidResponse
                                        (rs.getLong(2), rs.getDouble(3), null,
                                         null, null),
                                 eventMatch, time,
                                 dateFromTimeString(time, tz, ts));
                results.put(time, row);
            }
        }
        //-----------------------------------------------
        class GetEntropyThunk extends SQLThunk {

            Map<String, PerformanceHistoryRow> results;
            Map<String, Collection<BidderEvent>> eventMap;
            long advertiserId;
            long campaignId;
            TimeScale ts;
            TimeZone tz;
            Date startDate;
            Date endDate;

            GetEntropyThunk
                    (SQLConnector connector,
                     Map<String, PerformanceHistoryRow> results,
                     Map<String, Collection<BidderEvent>> eventMap,
                     long advertiserId, long campaignId, TimeScale ts,
                     TimeZone tz, Date startDate, Date endDate)
            {
                super(connector);
                this.results = results;
                this.eventMap = eventMap;
                this.advertiserId = advertiserId;
                this.campaignId = campaignId;
                this.ts = ts;
                this.tz = tz;
                this.startDate = startDate;
                this.endDate = endDate;
            }

            public void doit(ResultSet rs) throws SQLException
            {
                String time = rs.getString(1);
                Double entropy = rs.getDouble(2);
                Collection<BidderEvent> eventMatch = eventMap.get(time);
                if(eventMatch == null)
                    eventMatch = new Vector<BidderEvent>();
                PerformanceHistoryRow row = results.get(time);
                if(row == null)
                {
                    row = new PerformanceHistoryRow
                                (null,
                                 new TrivialBidResponse(0, 0, null, null, null),
                                 eventMatch, time,
                                 dateFromTimeString(time, tz, ts));
                    results.put(time, row);
                }
                TrivialBidResponse resp =
                        (TrivialBidResponse)row.getBidResponse();
                resp.setEntropy(entropy);
            }
        }
        //-----------------------------------------------
        class GetBidHistoryThunk extends SQLThunk {

            Map<String, PerformanceHistoryRow> results;
            Map<String, Collection<BidderEvent>> eventMap;
            long advertiserId;
            long campaignId;
            TimeScale ts;
            TimeZone tz;
            Date startDate;
            Date endDate;

            GetBidHistoryThunk
                    (SQLConnector connector,
                     Map<String, PerformanceHistoryRow> results,
                     Map<String, Collection<BidderEvent>> eventMap,
                     long advertiserId, long campaignId, TimeScale ts,
                     TimeZone tz, Date startDate, Date endDate)
            {
                super(connector);
                this.results = results;
                this.eventMap = eventMap;
                this.advertiserId = advertiserId;
                this.campaignId = campaignId;
                this.ts = ts;
                this.tz = tz;
                this.startDate = startDate;
                this.endDate = endDate;
            }

            public void doit(ResultSet rs) throws SQLException
            {
                String eventTime = rs.getString(1);
                long budget = rs.getLong(2);
                long target = rs.getLong(3);
                Collection<BidderEvent> eventMatch = eventMap.get(eventTime);
                if(eventMatch == null)
                    eventMatch = new Vector<BidderEvent>();
                PerformanceHistoryRow row =
                        new PerformanceHistoryRow
                                (null,
                                 new TrivialBidResponse
                                        (rs.getLong(2), rs.getDouble(3), null,
                                         budget, target),
                                 eventMatch, eventTime,
                                 dateFromTimeString(eventTime, tz, ts));
                results.put(eventTime, row);
            }
        }
        //-------------------------------------
        class GetObservedThunk extends SQLThunk {

            Map<String, BidParameters> results;
            long advertiserId;
            long campaignId;
            TimeScale ts;
            Date startDate;
            Date endDate;

            GetObservedThunk(SQLConnector connector,
                                   Map<String, BidParameters> results,
                                   long advertiserId, long campaignId,
                                   TimeScale ts, Date startDate, Date endDate)
            {
                super(connector);
                this.results = results;
                this.advertiserId = advertiserId;
                this.campaignId = campaignId;
                this.ts = ts;
                this.startDate = startDate;
                this.endDate = endDate;
            }

            public void doit(ResultSet rs) throws SQLException
            {
                String time = rs.getString(1);
                Double baseBid = rs.getDouble(2);
                Double maxBid = rs.getDouble(3);
                Long dailyImpressionLimit = rs.getLong(4);
                //long lifetimeImpressionTarget = rs.getLong(5);
                
                boolean fixedBidMode = false;
                Boolean enablePacing = null;
                Double minBid = null;
                
                JSONObject json =
                        (JSONObject)SQLHTTPHandler.toJSON
                                (rs.getString(combinedJSONIndex));
                if(json != null) {
                    json = (JSONObject)json.get("campaign");
                }
                if(json != null) {
                    Object bidType = json.get("cpm_bid_type");
                    fixedBidMode =
                            BidSpec.BASE_BID_MODE.equals(bidType);
                    enablePacing = (Boolean)json.get("enable_pacing");
                    minBid = toDouble(json.get("min_bid"));
                    maxBid = toDouble(json.get("max_bid"));
                } 
                
                CampaignChangeCount changes = // SQL is 1-based.
                        new CampaignChangeCount
                                (rs, campaignChangeCountStartIndex);
                BidParameters b;
                if(fixedBidMode) {
                	b = new BidParameters(baseBid, enablePacing, dailyImpressionLimit, changes);
                } else {
                	b = new BidParameters(minBid, maxBid, enablePacing, dailyImpressionLimit, changes);
                }

                results.put(time, b);
            }
            
            private Double toDouble(Object obj) {
            	if(obj instanceof Double) {
            		return (Double)obj;
            	}
            	return null;
            }
        }
        //------------------------------------
        connector.runSQLQuery
                (eventQuery, new GetEventsThunk
                        (connector, eventMap, advertiserId,
                         campaignId, ts, startDate, endDate),
                 qctx);
        connector.runSQLQuery
                (histQuery, new GetHistoricalDataThunk
                        (connector, intermediateResults, eventMap,
                         advertiserId, campaignId, ts, wrtTimeZone,
                         startDate, endDate),
                 qctx);
        connector.runSQLQuery
                (entropyQuery, new GetEntropyThunk
                        (connector, intermediateResults, eventMap,
                         advertiserId, campaignId, ts, wrtTimeZone,
                         startDate, endDate),
                 qctx);
        connector.runSQLQuery
                (bidHistoryQuery, new GetBidHistoryThunk
                        (connector, intermediateResults, eventMap,
                         advertiserId, campaignId, ts, wrtTimeZone,
                         startDate, endDate),
                 qctx);
        connector.runSQLQuery
                (observedQuery, new GetObservedThunk
                        (connector, bidParameterResults, advertiserId,
                         campaignId, ts, startDate, endDate),
                 qctx);
        //------------------------------------
        return totaliseResults(intermediateResults, bidParameterResults,
                               ts, wrtTimeZone, startDate, endDate);
    }

    List<PerformanceHistoryRow> getPerformanceHistoryRowsUTC
            (String eventQuery, String histQuery, String entropyQuery,
             String bidHistoryQuery, String observedQuery, long advertiserId,
             long campaignId, TimeScale ts, TimeZone wrtTimeZone,
             Date startDate, Date endDate, QueryContext qctx)
    {
        SQLConnector connector = bidder.ensureBidderSQLConnector();
        Map<String, Collection<BidderEvent>> eventMap =
                new HashMap<String, Collection<BidderEvent>>();
        Map<String, PerformanceHistoryRow> intermediateResults =
                new HashMap<String, PerformanceHistoryRow>();
        Map<String, BidParameters> bidParameterResults =
                new HashMap<String, BidParameters>();
        //-----------------------------------------------
        class GetEventsThunk extends SQLThunk {

            Map<String, Collection<BidderEvent>> results;
            long advertiserId;
            long campaignId;
            TimeScale ts;
            TimeZone tz;
            Date startDate;
            Date endDate;
            SQLConnector connector;

            GetEventsThunk(SQLConnector connector,
                           Map<String, Collection<BidderEvent>> results,
                           long advertiserId, long campaignId,
                           TimeScale ts, TimeZone tz, Date startDate,
                           Date endDate)
            {
                super(connector);
                this.results = results;
                this.advertiserId = advertiserId;
                this.campaignId = campaignId;
                this.ts = ts;
                this.tz = tz;
                this.startDate = startDate;
                this.endDate = endDate;
                this.connector = connector;
            }

            public void doit(ResultSet rs) throws SQLException
            {
                long observationTime = rs.getLong(1);
                String time = rollUpDate(observationTime, tz, ts);
                Collection<BidderEvent> timeEntry = results.get(time);
                if(timeEntry == null)
                {
                    timeEntry = new Vector<BidderEvent>();
                    results.put(time, timeEntry);
                }
                BidderEvent e = new BidderEvent();
                e.setAdvertiserId(advertiserId);
                e.setCampaignId(campaignId);
                e.setEventType(mapEventType(rs.getString(2)));
                e.setDescription(rs.getString(3));
                e.setEventTime(connector.correctSQLDate(rs.getTimestamp(4)));
                timeEntry.add(e);
            }
        }
        //-----------------------------------------------
        class GetHistoricalDataThunk extends SQLThunk {

            Map<String, PerformanceHistoryRow> results;
            Map<String, Collection<BidderEvent>> eventMap;
            long advertiserId;
            long campaignId;
            TimeScale ts;
            TimeZone tz;
            Date startDate;
            Date endDate;

            GetHistoricalDataThunk
                    (SQLConnector connector,
                     Map<String, PerformanceHistoryRow> results,
                     Map<String, Collection<BidderEvent>> eventMap,
                     long advertiserId, long campaignId, TimeScale ts,
                     TimeZone tz, Date startDate, Date endDate)
            {
                super(connector);
                this.results = results;
                this.eventMap = eventMap;
                this.advertiserId = advertiserId;
                this.campaignId = campaignId;
                this.ts = ts;
                this.tz = tz;
                this.startDate = startDate;
                this.endDate = endDate;
            }

            public void doit(ResultSet rs) throws SQLException
            {
                long observationTime = rs.getLong(1);
                long imps = rs.getLong(2);
                double cost = rs.getDouble(3);
                String time = rollUpDate(observationTime, tz, ts);
                Collection<BidderEvent> eventMatch = eventMap.get(time);
                if(eventMatch == null)  eventMatch = new Vector<BidderEvent>();
                PerformanceHistoryRow row = results.get(time);
                if(row == null)
                   row = new PerformanceHistoryRow
                                (null,
                                 new TrivialBidResponse(0, 0, null, null, null),
                                 eventMatch, time,
                                 dateFromTimeString(time, tz, ts));
                TrivialBidResponse br =
                        (TrivialBidResponse)row.getBidResponse();
                br.addImpressionsServed(imps);
                br.addTotalCost(cost);
                results.put(time, row);
            }
        }
        //-----------------------------------------------
        class GetEntropyThunk extends SQLThunk {

            Map<String, PerformanceHistoryRow> results;
            Map<String, Collection<BidderEvent>> eventMap;
            long advertiserId;
            long campaignId;
            TimeScale ts;
            TimeZone tz;
            Date startDate;
            Date endDate;

            GetEntropyThunk
                    (SQLConnector connector,
                     Map<String, PerformanceHistoryRow> results,
                     Map<String, Collection<BidderEvent>> eventMap,
                     long advertiserId, long campaignId, TimeScale ts,
                     TimeZone tz, Date startDate, Date endDate)
            {
                super(connector);
                this.results = results;
                this.eventMap = eventMap;
                this.advertiserId = advertiserId;
                this.campaignId = campaignId;
                this.ts = ts;
                this.tz = tz;
                this.startDate = startDate;
                this.endDate = endDate;
            }

            public void doit(ResultSet rs) throws SQLException
            {
                long observationTime = rs.getLong(1);
                double entropy = rs.getDouble(2);
                String time = rollUpDate(observationTime, tz, ts);
                Collection<BidderEvent> eventMatch = eventMap.get(time);
                if(eventMatch == null)  eventMatch = new Vector<BidderEvent>();
                PerformanceHistoryRow row = results.get(time);
                if(row == null)
                {
                   row = new PerformanceHistoryRow
                                (null, new TrivialBidResponse
                                        (0, 0, entropy, null, null),
                                 eventMatch, time,
                                 dateFromTimeString(time, tz, ts));
                    results.put(time, row);
                }
                else
                {
                    TrivialBidResponse br =
                            (TrivialBidResponse)row.getBidResponse();
                    br.setEntropy(entropy);
                }
            }
        }
        //-----------------------------------------------
        class GetBidHistoryThunk extends SQLThunk {

            Map<String, PerformanceHistoryRow> results;
            Map<String, Collection<BidderEvent>> eventMap;
            long advertiserId;
            long campaignId;
            TimeScale ts;
            TimeZone tz;
            Date startDate;
            Date endDate;

            GetBidHistoryThunk
                    (SQLConnector connector,
                     Map<String, PerformanceHistoryRow> results,
                     Map<String, Collection<BidderEvent>> eventMap,
                     long advertiserId, long campaignId, TimeScale ts,
                     TimeZone tz, Date startDate, Date endDate)
            {
                super(connector);
                this.results = results;
                this.eventMap = eventMap;
                this.advertiserId = advertiserId;
                this.campaignId = campaignId;
                this.ts = ts;
                this.tz = tz;
                this.startDate = startDate;
                this.endDate = endDate;
            }

            public void doit(ResultSet rs) throws SQLException
            {
                long eventTime = rs.getLong(1);
                long budget = rs.getLong(2);
                long target = rs.getLong(3);
                String time = rollUpDate(eventTime, tz, ts);
                Collection<BidderEvent> eventMatch = eventMap.get(time);
                if(eventMatch == null)  eventMatch = new Vector<BidderEvent>();
                PerformanceHistoryRow row = results.get(time);
                if(row == null)
                   row = new PerformanceHistoryRow
                                (null,
                                 new TrivialBidResponse(0, 0, null, null, null),
                                 eventMatch, time,
                                 dateFromTimeString(time, tz, ts));
                TrivialBidResponse br =
                        (TrivialBidResponse)row.getBidResponse();
                br.setImpressionBudget(budget);
                br.setImpressionTarget(target);
                results.put(time, row);
            }
        }
        //-------------------------------------
        class GetObservedThunk extends SQLThunk {

            Map<String, BidParameters> results;
            long advertiserId;
            long campaignId;
            TimeScale ts;
            TimeZone tz;
            Date startDate;
            Date endDate;

            GetObservedThunk(SQLConnector connector,
                                   Map<String, BidParameters> results,
                                   long advertiserId, long campaignId,
                                   TimeScale ts, TimeZone tz,
                                   Date startDate, Date endDate)
            {
                super(connector);
                this.results = results;
                this.advertiserId = advertiserId;
                this.campaignId = campaignId;
                this.ts = ts;
                this.tz = tz;
                this.startDate = startDate;
                this.endDate = endDate;
            }

            public void doit(ResultSet rs) throws SQLException
            {
                long observationTime = rs.getLong(1);
                Double baseBid = rs.getDouble(2);
                Double maxBid = rs.getDouble(3);
                Long dailyImpressionLimit = rs.getLong(4);
                //long lifetimeImpressionTarget = rs.getLong(5);
                JSONObject combinedJson =
                        (JSONObject)SQLHTTPHandler.toJSON
                                (rs.getString(combinedJSONIndex));
                CampaignChangeCount changes = // SQL is 1-based.
                        new CampaignChangeCount
                                (rs, campaignChangeCountStartIndex);
                String time = rollUpDate(observationTime, tz, ts);

                boolean fixedBidMode = false;
                Boolean enablePacing = null;
                Double minBid = null;

                if(combinedJson != null) {
                    combinedJson = (JSONObject)combinedJson.get("campaign");
                }
                if(combinedJson != null) {
                    Object bidType = combinedJson.get("cpm_bid_type");
                    fixedBidMode =
                            BidSpec.BASE_BID_MODE.equals(bidType);
                    enablePacing = (Boolean)combinedJson.get("enable_pacing");
                    minBid = toDouble(combinedJson.get("min_bid"));
                    maxBid = toDouble(combinedJson.get("max_bid"));
                }

                BidParameters b = results.get(time);
                if(b == null)
                    b = new BidParameters(enablePacing);
                if(fixedBidMode)
                {
                    b.addBid(observationTime, baseBid);
                    b.addDailyImpressionBudget
                            (observationTime, dailyImpressionLimit);
                    b.accumulateChanges(changes);
                }
                else
                {
                    b.addMinBid(observationTime, minBid);
                    b.addMaxBid(observationTime, maxBid);
                    b.addDailyImpressionBudget
                            (observationTime, dailyImpressionLimit);
                    b.accumulateChanges(changes);
                }
                results.put(time, b);
            }

            private Double toDouble(Object obj) {
            	if(obj instanceof Double) {
            		return (Double)obj;
            	}
            	return null;
            }
        }
        //------------------------------------
        connector.runSQLQuery
                (eventQuery, new GetEventsThunk
                        (connector, eventMap, advertiserId,
                         campaignId, ts, wrtTimeZone, startDate, endDate),
                 qctx);
        connector.runSQLQuery
                (histQuery, new GetHistoricalDataThunk
                        (connector, intermediateResults, eventMap,
                         advertiserId, campaignId, ts, wrtTimeZone,
                         startDate, endDate),
                 qctx);
        connector.runSQLQuery
                (entropyQuery, new GetEntropyThunk
                        (connector, intermediateResults, eventMap,
                         advertiserId, campaignId, ts, wrtTimeZone,
                         startDate, endDate),
                 qctx);
        connector.runSQLQuery
                (bidHistoryQuery, new GetBidHistoryThunk
                        (connector, intermediateResults, eventMap,
                         advertiserId, campaignId, ts, wrtTimeZone,
                         startDate, endDate),
                 qctx);
        connector.runSQLQuery
                (observedQuery, new GetObservedThunk
                        (connector, bidParameterResults, advertiserId,
                         campaignId, ts, wrtTimeZone, startDate, endDate),
                 qctx);
        //------------------------------------
        return totaliseResults(intermediateResults, bidParameterResults,
                               ts, wrtTimeZone, startDate, endDate);
    }

    @SuppressWarnings("unused")
    List<PerformanceHistoryRow> queryForCampaignPerformanceHistorySQLSide
                    (long advertiserId, long campaignId, TimeScale ts,
                     TimeZone wrtTimeZone, Date startDate, Date endDate,
                     QueryContext qctx, int hourOffset, int localOffset,
                     SQLConnector connector)
    {
        String eventQuery = aggregatedEventQuery
                            (advertiserId, campaignId, ts, startDate, endDate,
                             hourOffset, localOffset, connector);
        String histQuery = aggregatedHistDataQuery
                            (advertiserId, campaignId, ts, startDate, endDate,
                             hourOffset, localOffset, connector);
        String entropyQuery = aggregatedEntropyQuery
                            (advertiserId, campaignId, ts, startDate, endDate,
                             hourOffset, localOffset, connector);
        String bidHistoryQuery = aggregatedBidHistoryQuery
                            (advertiserId, campaignId, ts, startDate, endDate,
                             hourOffset, localOffset, connector);
        String observedQuery = aggregatedObservedDataQuery
                            (advertiserId, campaignId, ts, startDate, endDate,
                             hourOffset, localOffset, connector);
        return getPerformanceHistoryRows
                (eventQuery, histQuery, entropyQuery, bidHistoryQuery,
                 observedQuery, advertiserId, campaignId, ts, wrtTimeZone,
                 startDate, endDate, qctx);
    }

    List<PerformanceHistoryRow> queryForCampaignPerformanceHistory
                    (long advertiserId, long campaignId, TimeScale ts,
                     TimeZone wrtTimeZone, Date startDate, Date endDate,
                     QueryContext qctx)
    {
        String eventQuery = utcEventQuery
                            (advertiserId, campaignId, startDate, endDate);
        String histQuery = utcHistDataQuery
                            (advertiserId, campaignId, startDate, endDate);
        String entropyQuery = utcEntropyQuery
                            (advertiserId, campaignId, startDate, endDate);
        String bidHistoryQuery = utcBidHistoryQuery
                            (advertiserId, campaignId, startDate, endDate);
        String observedQuery = utcObservedDataQuery
                            (advertiserId, campaignId, startDate, endDate);
        return getPerformanceHistoryRowsUTC
                (eventQuery, histQuery, entropyQuery, bidHistoryQuery,
                 observedQuery, advertiserId, campaignId, ts, wrtTimeZone,
                 startDate, endDate, qctx);
    }

    public List<PerformanceHistoryRow> getCampaignPerformanceHistory
            (long advertiserId, long campaignId, TimeScale ts,
             Date startDate, Date endDate, TimeZone wrtTimeZone)
            throws Exception
    {
        return getCampaignPerformanceHistory
            (advertiserId, campaignId, ts, startDate, endDate, false, false,
             wrtTimeZone);
    }

    Map<Integer, Map<TimeScale, Map<Long, Map<Long, List<PerformanceHistoryRow>>>>>
            performanceCache =
                new HashMap<Integer, Map<TimeScale, Map<Long, Map<Long, List<PerformanceHistoryRow>>>>>();

    public List<PerformanceHistoryRow> getCampaignPerformanceHistory
            (long advertiserId, long campaignId, TimeScale ts,
             Date startDate, Date endDate, boolean fetchAppNexusData,
             boolean cacheResults, TimeZone wrtTimeZone)
            throws Exception
    {
        // Ideally move these four lines outside of this method.
        // Should we cache this stuff c.f. performanceCache?
        // All depends on the granularity of calling.
        TimeZone localTimeZone =
                Calendar.getInstance().getTimeZone();
        if(wrtTimeZone == null) wrtTimeZone = localTimeZone;
        QueryContext qctx = new BasicQueryContext(null, appNexusTheory);
        Date now = bidder.getCurrentTime();
        Integer hourOffset = wrtTimeZone.getOffset(now.getTime()) / 3600000;
        if(fetchAppNexusData)
        {
            Date then = new Date(now.getTime() - HOURLY_REPORTING_INTERVAL);
            bidder.ensureHistoricalDataPrefetched(ident, then, now, qctx);
        }
        //----------------------
        if(cacheResults)
        {
            Map<TimeScale, Map<Long, Map<Long, List<PerformanceHistoryRow>>>>
                    offsetEntry = performanceCache.get(hourOffset);
            if(offsetEntry == null)
            {
                offsetEntry =
                    new HashMap<TimeScale,
                                Map<Long,
                                Map<Long, List<PerformanceHistoryRow>>>>();
                performanceCache.put(hourOffset, offsetEntry);
            }
            Map<Long, Map<Long, List<PerformanceHistoryRow>>>
                    timeScaleEntry = offsetEntry.get(ts);
            if(timeScaleEntry == null)
            {
                timeScaleEntry =
                    new HashMap<Long,
                                Map<Long, List<PerformanceHistoryRow>>>();
                offsetEntry.put(ts, timeScaleEntry);
            }
            Map<Long, List<PerformanceHistoryRow>> advertiserEntry =
                    timeScaleEntry.get(advertiserId);
            if(advertiserEntry == null)
            {
                advertiserEntry =
                        new HashMap<Long, List<PerformanceHistoryRow>>();
                timeScaleEntry.put(advertiserId, advertiserEntry);
            }
            List<PerformanceHistoryRow> campaignEntry =
                    advertiserEntry.get(campaignId);
            if(campaignEntry == null)
            {
                campaignEntry = queryForCampaignPerformanceHistory
                      (advertiserId, campaignId, ts, wrtTimeZone,
                       startDate, endDate, qctx
                              // , hourOffset, localOffset, connector
                      );
                advertiserEntry.put(campaignId, campaignEntry);
            }
            return campaignEntry;
        }
        else return queryForCampaignPerformanceHistory
                      (advertiserId, campaignId, ts, wrtTimeZone,
                       startDate, endDate, qctx
                              // , hourOffset, localOffset, connector
                      );
    }

    @SuppressWarnings("unchecked")
    public void dumpPerformanceHistoryToExcel
            (String savePath, List<PerformanceHistoryRow> curve)
    {
        ExcelColSchema.dumpToExcel(savePath, (List) curve);
    }

    @SuppressWarnings("unchecked")
    public void dumpPerformanceHistoryToExcel
            (OutputStream stream, List<PerformanceHistoryRow> curve)
    {
        ExcelColSchema.dumpToExcel(stream, (List) curve);
    }

    @SuppressWarnings("unchecked")
    public void dumpPerformanceHistoryToExcel
            (List<PerformanceCurve> curves, OutputStream stream)
    {
        List<List<PerformanceHistoryRow>> pointsList =
                new Vector<List<PerformanceHistoryRow>>();
        List<String> names = new Vector<String>();
        for(PerformanceCurve pc: curves)
        {
            pointsList.add(pc.points);
            names.add(pc.campaignName);
        }
        ExcelColSchema.dumpToExcel(stream, (List) pointsList, names);
    }


    @SuppressWarnings("unchecked")
    public void dumpPerformanceHistoryToExcel
            (List<PerformanceCurve> curves, String savePath)
    {
        List<List<PerformanceHistoryRow>> pointsList =
                new Vector<List<PerformanceHistoryRow>>();
        List<String> names = new Vector<String>();
        for(PerformanceCurve pc: curves)
        {
            pointsList.add(pc.points);
            names.add(pc.campaignName);
        }
        ExcelColSchema.dumpToExcel(savePath, (List) pointsList, names);
    }

    @SuppressWarnings("unused")
    void runTests()
    {
        bidder.setTraceSQL(true);
        int advertiserId = 8040;
        int campaignId = 226113;
        Date endDate = bidder.getCurrentTime();
        long millisInTwoMonths = 60L * 24L * 3600L * 1000L;
        Date startDate =
                new Date(endDate.getTime() - millisInTwoMonths);
        try
        {
            List<PerformanceHistoryRow> hourlyCurve  =
                getCampaignPerformanceHistory
                        (advertiserId, campaignId, TimeScale.HOURLY,
                         startDate, endDate, null);
            List<PerformanceHistoryRow> dailyCurve  =
                getCampaignPerformanceHistory
                        (advertiserId, campaignId, TimeScale.DAILY,
                         startDate, endDate, null);
            List<PerformanceHistoryRow> weeklyCurve  =
                getCampaignPerformanceHistory
                        (advertiserId, campaignId, TimeScale.WEEKLY,
                         startDate, endDate, null);
            List<PerformanceHistoryRow> monthlyCurve  =
                getCampaignPerformanceHistory
                        (advertiserId, campaignId, TimeScale.MONTHLY,
                         startDate, endDate, null);
            List<PerformanceHistoryRow> quarterlyCurve  =
                getCampaignPerformanceHistory
                        (advertiserId, campaignId, TimeScale.QUARTERLY,
                         startDate, endDate, null);
            List<PerformanceHistoryRow> yearlyCurve  =
                getCampaignPerformanceHistory
                        (advertiserId, campaignId, TimeScale.YEARLY,
                         startDate, endDate, null);
            System.out.println("Hourly: " + hourlyCurve);
            System.out.println("Daily: " + dailyCurve);
            System.out.println("Weekly: " + weeklyCurve);
            System.out.println("Monthly: " + monthlyCurve);
            System.out.println("Quarterly: " + quarterlyCurve);
            System.out.println("Yearly: " + yearlyCurve);
            // --------------------
            String path = "/tmp/temp.xls";
            dumpPerformanceHistoryToExcel(path, dailyCurve);
            List<ExcelDumpable> tableReadIn =
                  ExcelColSchema.readExcelFile
                      (path, new TestRow().getSchema(),
                              TestRow.class, null, true, true);
            System.out.println
                ("Table read in: " + AppNexusUtils.commaSeparate(tableReadIn));
        }
        catch (Exception e)
        {
            throw Utils.barf(e, this);
        }
    }

    public static void tabulateDates
            (SynchDateFormat format, TimeZone tz, Date start, int forDays)
    {
        for(long hour = 0; hour < (24 * forDays); hour++)
        {
            Date d = new Date(start.getTime() + (hour * 3600 * 1000));
            String s = "" + d.getTime()/1000 + "; " + d.toString() + "; " +
                       format.format(d) + " ";
            for(TimeScale ts: TimeScale.values())
            {
                s = s + ", " + ts + "=" +
                        rollUpDate(d.getTime() / 1000, tz, ts);
            }
            System.out.println(s);
        }
    }

    public static void main(String[] args)
    {
        SynchDateFormat format = new SynchDateFormat("yyyy-MM-dd HH");
        TimeZone tz = TimeZone.getTimeZone("est5edt"); 
        try
        {
            System.out.println("-------------------------------");
            Date start1 = format.parse("2011-11-05 00");
            tabulateDates(format, tz, start1, 3);
            System.out.println("-------------------------------");
            Date start2 = format.parse("2012-03-10 00");
            tabulateDates(format, tz, start2, 3);
            System.out.println("-------------------------------");
            Calendar c2 = Calendar.getInstance();
            c2.setTime(start2);
            c2.setTimeZone(tz);
            System.out.println(start2);
            System.out.println(DateUtils.toDateTimeString(c2)); 
            System.out.println("-------------------------------");
        }
        catch (ParseException e)
        {
            throw new Error(e);
        }
    }

}

class TestRow implements ExcelDumpable{
    String time;
    Double bidPrice;

    public TestRow() {}

    private String[][] excelSchemaDef =
      {
        { "Time",      "getTime",     null,              "setTime" },
        { "Bid price", "getBidPrice", "dollarCurrency" , "setBidPrice"}
      };

    public List<ExcelColSchema> getSchema()
    {
        return ExcelColSchema.getSchema(TestRow.class, excelSchemaDef);
    }

    public Object getValue(ExcelColSchema colSchema)
    {
        throw Utils.barf("Not implemented", this);
    }

    public String getTime() { return time; }
    @SuppressWarnings("unused")
    public Double getBidPrice() { return bidPrice; }
    @SuppressWarnings("unused")
    public void setTime(String time) { this.time = time; }
    @SuppressWarnings("unused")
    public void setBidPrice(Double bp) { this.bidPrice = bp; }
}

