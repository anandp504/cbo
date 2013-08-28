package com.tumri.cbo.backend;

import com.tumri.mediabuying.zini.*;

import org.apache.log4j.Level;
import org.json.simple.JSONObject;
import java.io.*;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

public class CampaignChangeClassifier {

	private static long m_imdTime = 0L;
	private static long m_umdQueryTime = 0L;
	private static long m_mfrTime = 0L;
	private static long m_rmduTime = 0L;
	
    // For every pair of consecutive entries in the ObservedData table,
    // Add a row into CampaignChanges that classifies any changes.
    
    public static Set<Object> UNIMPORTANT_KEYS =
            new HashSet<Object>(Arrays.asList
                    ("stats",
                     "name",
                     "id",
                     "base_bid",
                     "profile_id",
                     "advertiser_id",
                     "code",
                     "description",
                     "last_modified",
                     "created_on"));

    static Object[][] attributes =
            {
                    // Campaign attributes follow
                    { "allow_safety_pacing", true },
                    { "allow_unverified_ecp", true },
                    { "bid_margin", true },
                    { "cadence_modifier_enabled", true },
                    { "cadence_type", true },
                    { "cpc_goal", true },
                    { "cpm_bid_type", true },
                    { "daily_budget", true },
                    { "daily_budget_imps", true },
                    { "defer_to_li_prediction", true },
                    { "ecp_learn_divisor", true },
                    { "enable_pacing", true },
                    { "end_date", true },
                    { "inventory_type", true },
                    { "lifetime_budget", true },
                    { "lifetime_budget_imps", true },
                    { "max_bid", true },
                    { "min_bid", true },
                    { "optimization_lever_mode", true },
                    { "state", true },
                    { "timezone", true },
                    // Profile attributes follow
                    { "age_targets", false },
                    { "allow_unaudited", false },
                    { "browser_targets", false },
                    { "carrier_action", false },
                    { "carrier_targets", false },
                    { "city_action", false },
                    { "city_targets", false },
                    { "content_category_targets", false },
                    { "country_action", false },
                    { "country_targets", false },
                    { "daypart_targets", false },
                    { "daypart_timezone", false },
                    { "dma_action", false },
                    { "dma_targets", false },
                    { "domain_action", false },
                    { "domain_list_action", false },
                    { "domain_list_targets", false },
                    { "domain_targets", false },
                    { "exelate_targets", false },
                    { "gender_targets", false },
                    { "handset_make_action", false },
                    { "handset_make_targets", false },
                    { "handset_model_action", false },
                    { "handset_model_targets", false },
                    { "intended_audience_targets", false },
                    { "inv_class_targets", false },
                    { "inventory_action", false },
                    { "inventory_attribute_targets", false },
                    { "inventory_group_targets", false },
                    { "inventory_source_targets", false },
                    { "language_targets", false },
                    { "location_target_latitude", false },
                    { "location_target_longitude", false },
                    { "location_target_radius", false },
                    { "member_targets", false },
                    { "media_buy_targets", false },
                    { "operating_system_targets", false },
                    { "placement_targets", false },
                    { "platform_content_category_targets", false },
                    { "platform_placement_targets", false },
                    { "platform_publisher_targets", false },
                    { "position_targets", false },
                    { "publisher_targets", false },
                    { "querystring_action", false },
                    { "querystring_boolean_operator", false },
                    { "querystring_targets", false },
                    { "region_action", false },
                    { "region_targets", false },
                    { "require_cookie_for_freq_cap", false },
                    { "segment_boolean_operator", false },
                    { "segment_group_targets", false },
                    { "segment_targets", false },
                    { "session_freq_type", false },
                    { "site_targets", false },
                    { "size_targets", false },
                    { "supply_type_action", false },
                    { "supply_type_targets", false },
                    { "trust", false },
                    { "use_inventory_attribute_targets", false },
                    { "user_group_targets", false },
                    { "venue_action", false },
                    { "venue_targets", false },
                    { "zip_targets", false },

                    { "max_day_imps", false },
                    { "max_session_imps", false },
                    { "min_minutes_per_imp", false },
                    { "min_session_imps", false }
            };

    static CampaignChangeCount classifyAttributeChangeTypes
            (JSONObject campaignJo1, JSONObject campaignJo2,
             JSONObject profileJo1,  JSONObject profileJo2,
             Long advertiserId, Long campaignId, BidStrategy bidStrategy)
    {
        CampaignChangeCount res = new CampaignChangeCount();
        for(Object[] attributeSpec: attributes)
        {
            classifyAttributeChangeType
                        (attributeSpec, campaignJo1, campaignJo2,
                         profileJo1,  profileJo2, res, advertiserId,
                         campaignId, bidStrategy);
        }
        return res;
    }

    static boolean campaignAllowsBudgetChange
                  (Long advertiserId, Long campaignId, BidStrategy bidStrategy)
    {
        return bidStrategy != null &&
               bidStrategy.isVariableDailyImpressionBudgetStrategy();
    }

    static void classifyAttributeChangeType
            (Object[] attributeSpec,
             JSONObject campaignJo1, JSONObject campaignJo2,
             JSONObject profileJo1,  JSONObject profileJo2,
             CampaignChangeCount res, Long advertiserId, Long campaignId,
             BidStrategy bidStrategy)
    {
        String  attribute = (String) attributeSpec[0];
        boolean campaignP = (Boolean)attributeSpec[1];
        Object v1 =
              (campaignP
                ? (campaignJo1 == null ? null : campaignJo1.get(attribute))
                : (profileJo1  == null ? null : profileJo1.get(attribute)));
        Object v2 =
              (campaignP
                ? (campaignJo2 == null ? null : campaignJo2.get(attribute))
                : (profileJo2  == null ? null : profileJo2.get(attribute)));
        if("allow_safety_pacing".equals(attribute) ||
                "allow_unverified_ecp".equals(attribute) ||
                "cadence_modifier_enabled".equals(attribute) ||
                "cadence_type".equals(attribute) ||
                "cpc_goal".equals(attribute) ||
                "cpm_bid_type".equals(attribute) ||
                "defer_to_li_prediction".equals(attribute) ||
                "ecp_learn_divisor".equals(attribute) ||
                "inventory_type".equals(attribute) ||
                "optimization_lever_mode".equals(attribute) ||
                "timezone".equals(attribute))
        {
            if(v1 == null && v2 == null) res.unchanged++;
            else if(v1 == null || v2 == null)
                res.attribute_Changed_With_Unknown_Effect_On_Delivery++;
            else if(v1.equals(v2))
                res.unchanged++;
            else res.attribute_Changed_With_Unknown_Effect_On_Delivery++;
        }
        else if("enable_pacing".equals(attribute))
        {
            Boolean b1 = (Boolean) v1;
            Boolean b2 = (Boolean) v2;
            if(b1 == null && b2 == null) res.unchanged++;
            else if(b1 == null)
            {
                if(b2) res.attribute_Changed_Decreases_Delivery++;
                else res.attribute_Changed_But_Will_Not_Affect_Delivery++;
            }
            else if(b2 == null)
            {
                if(b1) res.attribute_Changed_Increases_Delivery++;
                else res.attribute_Changed_But_Will_Not_Affect_Delivery++;
            }
            else if(v1.equals(v2))
                res.unchanged++;
            else if(b2) res.attribute_Changed_Decreases_Delivery++;
            else res.attribute_Changed_With_Unknown_Effect_On_Delivery++;
        }
        else if("base_bid".equals(attribute) ||
                "max_bid".equals(attribute) ||
                "min_bid".equals(attribute))
        {
            if(v1 == null && v2 == null) res.unchanged++;
            else if(v1 == null || v2 == null)
                res.attribute_Changed_With_Unknown_Effect_On_Delivery++;
            else if(v1.equals(v2)) res.unchanged++;
            else
            {
                Double d1 = (Double) v1;
                Double d2 = (Double) v2;
                if(d1 > d2) res.attribute_Decreased_Decreases_Delivery++;
                else res.attribute_Increased_Increases_Delivery++;
            }
        }
        else if("bid_margin".equals(attribute) ||
                "daily_budget".equals(attribute) ||
                "lifetime_budget".equals(attribute) ||
                "bid_margin".equals(attribute) ||
                "bid_margin".equals(attribute))
        {
            if(v1 == null && v2 == null) res.unchanged++;
            else if(v1 == null || v2 == null)
                res.attribute_Changed_With_Unknown_Effect_On_Delivery++;
            else if(v1.equals(v2)) res.unchanged++;
            else
            {
                Double d1 = (Double) v1;
                Double d2 = (Double) v2;
                if(d1 > d2) res.attribute_Decreased_Increases_Delivery++;
                else res.attribute_Increased_Decreases_Delivery++;
            }
        }
        else if("max_day_imps".equals(attribute) ||
                "max_session_imps".equals(attribute) ||
                "lifetime_budget_imps".equals(attribute) ||
                "min_minutes_per_imp".equals(attribute) ||
                "min_session_imps".equals(attribute))
        {
            if(v1 == null && v2 == null) res.unchanged++;
            else if(v1 == null) res.attribute_Increased_Decreases_Delivery++;
            else if(v2 == null) res.attribute_Decreased_Increases_Delivery++;
            else if(v1.equals(v2)) res.unchanged++;
            else
            {
                Long l1 = (Long) v1;
                Long l2 = (Long) v2;
                if("min_minutes_per_imp".equals(attribute))
                {
                    if(l1 > l2) res.attribute_Increased_Decreases_Delivery++;
                    else res.attribute_Decreased_Increases_Delivery++;
                }
                else
                {
                    if(l1 > l2) res.attribute_Increased_Increases_Delivery++;
                    else res.attribute_Decreased_Decreases_Delivery++;
                }
            }
        }
        else if("daily_budget_imps".equals(attribute))
        {
            if(v1 == null && v2 == null) res.unchanged++;
            else if(campaignAllowsBudgetChange
                    (advertiserId, campaignId, bidStrategy))
                res.unchanged++;
            else if(v1 == null)
                res.attribute_Increased_Decreases_Delivery++;
            else if(v2 == null)
                res.attribute_Decreased_Increases_Delivery++;
            else if(v1.equals(v2))
                res.unchanged++;
            else
            {
                Long l1 = (Long) v1;
                Long l2 = (Long) v2;
                if(l1 > l2) res.attribute_Increased_Increases_Delivery++;
                else res.attribute_Decreased_Decreases_Delivery++;
            }
        }
        else if("end_date".equals(attribute))
        {
            if(v1 == null && v2 == null) res.unchanged++;
            else if(v1 == null || v2 == null)
                res.attribute_Changed_With_Unknown_Effect_On_Delivery++;
            else if(v1.equals(v2)) res.unchanged++;
            else
            {
                String s1 = (String) v1;
                String s2 = (String) v2;
                int comp = s1.compareTo(s2);
                if("end_date".equals(attribute))
                {
                    if(comp < 0) res.attribute_Decreased_Decreases_Delivery++;
                    else res.attribute_Decreased_Increases_Delivery++;
                }
                else throw Utils.barf("Unhandled attribute", null, attribute);
            }
        }
        else if("state".equals(attribute))
        {
            if(v1 == null && v2 == null) res.unchanged++;
            else if(v1 == null || v2 == null)
                res.attribute_Changed_With_Unknown_Effect_On_Delivery++;
            else if(v1.equals(v2)) res.unchanged++;
            else
            {
                String s1 = (String) v1;
                String s2 = (String) v2;
                if("state".equals(attribute))
                {
                    if("active".equals(s2))
                        res.attribute_Changed_Increases_Delivery++;
                    else if ("active".equals(s1))
                        res.attribute_Changed_Decreases_Delivery++;
                    else
                      res.attribute_Changed_With_Unknown_Effect_On_Delivery++;
                }
                else throw Utils.barf("Unhandled attribute", null, attribute);
            }
        }
        else if("age_targets".equals(attribute) ||
                "allow_unaudited".equals(attribute) ||
                "browser_targets".equals(attribute) ||
                "carrier_action".equals(attribute) ||
                "carrier_targets".equals(attribute) ||
                "city_action".equals(attribute) ||
                "city_targets".equals(attribute) ||
                "content_category_targets".equals(attribute) ||
                "country_action".equals(attribute) ||
                "country_targets".equals(attribute) ||
                "daypart_targets".equals(attribute) ||
                "daypart_timezone".equals(attribute) ||
                "dma_action".equals(attribute) ||
                "dma_targets".equals(attribute) ||
                "domain_action".equals(attribute) ||
                "domain_list_action".equals(attribute) ||
                "domain_list_targets".equals(attribute) ||
                "domain_targets".equals(attribute) ||
                "exelate_targets".equals(attribute) ||
                "gender_targets".equals(attribute) ||
                "handset_make_action".equals(attribute) ||
                "handset_make_targets".equals(attribute) ||
                "handset_model_action".equals(attribute) ||
                "handset_model_targets".equals(attribute) ||
                "intended_audience_targets".equals(attribute) ||
                "inv_class_targets".equals(attribute) ||
                "inventory_action".equals(attribute) ||
                "inventory_attribute_targets".equals(attribute) ||
                "inventory_group_targets".equals(attribute) ||
                "inventory_source_targets".equals(attribute) ||
                "language_targets".equals(attribute) ||
                "location_target_latitude".equals(attribute) ||
                "location_target_longitude".equals(attribute) ||
                "location_target_radius".equals(attribute) ||
                "member_targets".equals(attribute) ||
                "media_buy_targets".equals(attribute) ||
                "operating_system_targets".equals(attribute) ||
                "placement_targets".equals(attribute) ||
                "platform_content_category_targets".equals(attribute) ||
                "platform_placement_targets".equals(attribute) ||
                "platform_publisher_targets".equals(attribute) ||
                "position_targets".equals(attribute) ||
                "publisher_targets".equals(attribute) ||
                "querystring_action".equals(attribute) ||
                "querystring_boolean_operator".equals(attribute) ||
                "querystring_targets".equals(attribute) ||
                "region_action".equals(attribute) ||
                "region_targets".equals(attribute) ||
                "require_cookie_for_freq_cap".equals(attribute) ||
                "segment_boolean_operator".equals(attribute) ||
                "segment_group_targets".equals(attribute) ||
                "segment_targets".equals(attribute) ||
                "session_freq_type".equals(attribute) ||
                "site_targets".equals(attribute) ||
                "size_targets".equals(attribute) ||
                "supply_type_action".equals(attribute) ||
                "supply_type_targets".equals(attribute) ||
                "trust".equals(attribute) ||
                "use_inventory_attribute_targets".equals(attribute) ||
                "user_group_targets".equals(attribute) ||
                "venue_action".equals(attribute) ||
                "venue_targets".equals(attribute) ||
                "zip_targets".equals(attribute))
        {
            if(v1 == null && v2 == null) res.unchanged++;
            else if(v1 == null) res.targeting_Narrowed_Decreases_Delivery++;
            else if(v2 == null) res.targeting_Widened_Increases_Delivery++;
            else if(v1.equals(v2)) res.unchanged++;
            else
            {
                Subsumption comparison =
                        TargetingComparisonPerspective.compare(v1, v2);
                if(comparison == Subsumption.Same) res.unchanged++;
                else if(comparison == Subsumption.Disjoint)
                    res.attribute_Changed_With_Unknown_Effect_On_Delivery++;
                else if(comparison == Subsumption.Subsumes)
                    res.targeting_Narrowed_Decreases_Delivery++;
                else if(comparison == Subsumption.SubsumedBy)
                    res.targeting_Widened_Increases_Delivery++;
                else throw Utils.barf("Unhandled Subsumption", null,
                            attribute, v1, v2, comparison);
            }
        }
        else throw Utils.barf("Unhandled attribute", null, attribute);
    }

    public static CampaignChangeCount describeMaterialDifferences
            (Long advertiserId, Long campaignId, Date observationTime,
             Long sequenceNumber, Bidder bidder, SQLContext sctx,
             QueryContext qctx)
    {
        Sexpression campaignSettings =
                CampaignData.getCampaignSettings
                        (sctx, qctx, advertiserId, campaignId);
        Sexpression bidStrategySexp = campaignSettings.third();
        String bidStrategyName =
              (bidStrategySexp == Null.nil
                      ? null
                      : bidStrategySexp.unboxString());
        BidStrategy bidStrategy =
                (bidStrategySexp == null
                        ? null
                        : BidderInstruction.getStrategy(bidStrategyName));
        Observation thisTimeJSON =
               (sequenceNumber != null
                       ? bidder.getJSONFor
                            (campaignId, qctx, null, sequenceNumber,
                             false, true, false)
                       : bidder.getJSONFor
                            (campaignId, qctx, observationTime, null,
                             false, true, false));
        if(thisTimeJSON == null)
            throw Utils.barf("No observed campaign data found for " +
                             advertiserId + "/" + campaignId, null);
        Observation previousJSON =
                (sequenceNumber != null
                       ? bidder.getJSONFor
                            (campaignId, qctx, null, sequenceNumber - 1,
                             false, true, false)
                       : bidder.getJSONFor
                            (campaignId, qctx, observationTime, null,
                             true, true, false));
        if(previousJSON == null) return null; // no previous observation;
        else
        {
            JSONObject campaignJo1 = previousJSON.campaignObject();
            JSONObject campaignJo2 = thisTimeJSON.campaignObject();
            JSONObject profileJo1  = previousJSON.profileObject();
            JSONObject profileJo2  = thisTimeJSON.profileObject();
            try
            {
                String campS = CampaignDataHistoryPerspective.diffJson
                      (campaignJo1, campaignJo2, "campaign", UNIMPORTANT_KEYS);
                String profS = CampaignDataHistoryPerspective.diffJson
                      ( profileJo1,  profileJo2,  "profile", UNIMPORTANT_KEYS);
                if(campS != null || profS != null)
                {
                    CampaignChangeCount changeTypes =
                            classifyAttributeChangeTypes
                                    (campaignJo1, campaignJo2,
                                     profileJo1, profileJo2,
                                     advertiserId, campaignId, bidStrategy);
                    return (changeTypes.isNonTrivial()
                            ? changeTypes
                            : null);
                }
                else return null;
            }
            catch (IOException e)
            {
                throw Utils.barf(e, null);
            }
        }
    }

    //=========================================================================

    static Map<String, Long> findMaxSeqNumResults
            (SQLConnector connector, QueryContext qctx, Long advertiserId,
             Long campaignId)
    {
        String maxSeqNumQuery =
            "SELECT advertiser_id, campaign_id, MAX(sequence_number)\n" +
            "FROM observeddata\n" +
            (advertiserId == null
                    ? ""
                    : "WHERE advertiser_id = " + advertiserId + "\n" +
                      "AND   campaign_id = " + campaignId + "\n") +
            "GROUP BY advertiser_id, campaign_id;";

        Map<String, Long> maxSeqNumResults = new HashMap<String, Long>();
        class MaxSeqNumThunk extends SQLThunk {

            Map<String, Long> maxSeqnumResults;

            MaxSeqNumThunk(SQLConnector connector,
                           Map<String, Long> maxSeqnumResults)
            {
                super(connector);
                this.maxSeqnumResults = maxSeqnumResults;
            }

            public void doit(ResultSet rs) throws SQLException
            {
                String key = "" + rs.getLong(1) + "-" +  rs.getLong(2);
                maxSeqnumResults.put(key, rs.getLong(3));
                traceOrNoteProgress(Syms.T, false);
            }
        }
        SQLThunk thunk = new MaxSeqNumThunk(connector, maxSeqNumResults);
        connector.runSQLQuery(maxSeqNumQuery, thunk, qctx);
        thunk.finishProgress();
        return maxSeqNumResults;
    }

    public static void fixUpSequenceNumbersEtc
            (Bidder bidder, SQLContext sctx, QueryContext qctx)
    {
        SQLConnector connector = bidder.ensureBidderSQLConnector();
        BidderState bs = BidderState.IDENTIFYING_CAMPAIGN_DIFFERENCES;
        bidder.setBidderState(bs);
        String stat = bs.getDisplayName();
        Status status = new Status(stat, false);
        bidder.setStatus(status);
        
        Recordings recordings = null;
        List<List<Long>> rows = getHighWaterMarks(connector, qctx);
        Long currentAdv = null;
        ProgressNoter noter = new ProgressNoter
                ("Identifying material campaign differences", 1, false);
        m_umdQueryTime = 0L;
        m_imdTime = 0L;
        m_mfrTime = 0L;
    	m_rmduTime = 0L;
    	
        for(List<Long> row: rows)
        {
            Long advertiserId = row.get(0);
            Long campaignId = row.get(1);
            Long hwm = row.get(2);
            if(currentAdv == null || !currentAdv.equals(advertiserId))
            {
                currentAdv = advertiserId;
                // status.change(stat + " in adv " + advertiserId);
                status.change(stat + " in adv/camp " + advertiserId + "/" +
                              campaignId, false);
            }
            noter.event();
            recordings = identifyMaterialDifferences
                  (connector, qctx, recordings, advertiserId, campaignId, hwm,
                          // Todo: Set to true when everything works?
                   false);
        }
        noter.finish();
        // Now we've recorded the differences, effectuate them!
        long t0 = System.currentTimeMillis();
        Recordings.maybeFlushRecordings(connector, qctx, recordings, true,
                                        "materially_different");
        m_mfrTime += (System.currentTimeMillis() - t0);
        
        String timings = "Timings: identifyMaterialDifferences(): " + m_imdTime +
        "mS, updateMaterialDifferencesQueryTime = " + m_umdQueryTime +
        "mS, maybeFlushRecordingsTime = " + m_mfrTime +
        "mS, recordMaterialDiffUpdate() took " + m_rmduTime + "mS";

        Utils.logThisPoint
                (Level.INFO, "Finished identifying material differences\n" + timings);
        
        bidder.setBidderState(BidderState.RECORDING_CAMPAIGN_DIFFERENCES);
        
        recordMissingMaterialDifferences(bidder, connector, sctx, qctx);
    }


    //=========================================================================

    static List<List<Long>> getHighWaterMarks
            (SQLConnector connector, QueryContext qctx)
    {
        String missingZeroQuery =
            "SELECT advertiser_id, campaign_id, hwm\n" +
            "FROM High_Water_Mark\n" +
            "ORDER BY advertiser_id, campaign_id;";

        List<List<Long>> res = new Vector<List<Long>>();
        class Thunk extends SQLThunk {

            List<List<Long>> res;
            SQLConnector connector;

            Thunk(SQLConnector connector, List<List<Long>> res)
            {
                super(connector);
                this.res = res;
                this.connector = connector;
            }

            public void doit(ResultSet rs) throws SQLException
            {
                List<Long> row =  new Vector<Long>();
                row.add(rs.getLong(1));
                row.add(rs.getLong(2));
                row.add(rs.getLong(3));
                res.add(row);
            }
        }
        connector.runSQLQuery
                (missingZeroQuery, new Thunk(connector, res), qctx);
        return res;
    }

    static Recordings recordMaterialDiffUpdate
            (Sexpression row, Recordings recordings)
    {
    	Recordings result = null;
    	long t0 = System.currentTimeMillis();
        try
        {
            String campS =
                    CampaignDataHistoryPerspective.diffJson
                            (row.fourth(), row.sixth(), "campaign",
                             CampaignDataHistoryPerspective.UNIMPORTANT_KEYS);
            String profS =
                    CampaignDataHistoryPerspective.diffJson
                            (row.fifth(), row.seventh(), "profile",
                             CampaignDataHistoryPerspective.UNIMPORTANT_KEYS);
            boolean differentP = false;
            // Write it this perverse way to make it easier to break in.
            if(campS != null || profS != null)
                differentP = true;
            result = Recordings.recordMaterialDiffUpdate
                    (differentP, row.first().unboxLong(),
                     row.second().unboxLong(), row.third().unboxDate(),
                     recordings);
        }
        catch (IOException e)
        {
            throw Utils.barf(e, row);
        }
        m_rmduTime += (System.currentTimeMillis() - t0);
        return result;
    }

    static Recordings identifyMaterialDifferences
            (SQLConnector connector, QueryContext qctx, Recordings recordings,
             Long advertiserId, Long campaignId, Long hwm,
             boolean onlyRecent)
    {
    	long start = System.currentTimeMillis();
        if(recordings == null) recordings = new Recordings();
        String query =
                "SELECT o1.advertiser_id, o1.campaign_id, o1.observation_time,\n" +
                "       o1.campaign_json, o1.campaign_profile_json,\n" +
                "       o2.campaign_json, o2.campaign_profile_json\n" +
                "FROM  observeddata o1 use index (ix8), observeddata o2\n" +
                "WHERE o1.advertiser_id = " + advertiserId + "\n" +
                "AND   o1.campaign_id = " + campaignId + "\n" +
                "AND   o1.materially_different IS NULL\n" + 
                (onlyRecent ? "" : "-- ") +
                "AND   o1.sequence_number >  " + hwm + "\n" +
                "AND   o2.advertiser_id = o1.advertiser_id\n" +
                "AND   o2.campaign_id = o1.campaign_id\n" +
                "AND   o2.sequence_number = o1.sequence_number - 1\n" +
                "AND   o2.observation_time < o1.observation_time\n" +
                (onlyRecent ? "" : "-- ") +
                "AND   o2.sequence_number >= " + hwm + "\n" +
                "ORDER BY o1.advertiser_id, o1.campaign_id,\n" +
                "         o1.observation_time";
        class Thunk extends SQLThunk
        {
            SQLConnector connector;
            QueryContext qctx;
            Recordings recordings;

            public void doit(ResultSet rs) throws SQLException
            {
                ResultSetMetaData md = rs.getMetaData();
                int cols = md.getColumnCount();
                Sexpression res = Null.nil;
                Sexpression tail = Null.nil;
                for(int i = 1; i <= cols; i++)
                {
                    int type = md.getColumnType(i);
                    Sexpression newTail =
                            Cons.list(connector.sqlToSexpression(i, type, rs));
                    if(res == Null.nil)
                    {
                        res = newTail;
                        tail = newTail;
                    }
                    else
                    {
                        tail.setCdr(newTail);
                        tail = newTail;
                    }
                }
                // updateForMaterialDifferences(connector, qctx, res);
                recordings = recordMaterialDiffUpdate(res, recordings);
                
                long t0 = System.currentTimeMillis();
                Recordings.maybeFlushRecordings
                        (connector, qctx, recordings, false,
                         "materially_different");
                m_mfrTime += (System.currentTimeMillis() - t0);
                traceOrNoteProgress(res);
            }

            Thunk(SQLConnector connector, QueryContext qctx,
                  Recordings recordings)
            {
                super(connector);
                this.connector = connector;
                this.qctx = qctx;
                this.recordings = recordings;
            }
        }
        SQLThunk thunk = new Thunk(connector, qctx, recordings);
        long t0 = System.currentTimeMillis();
        connector.runSQLQuery(query, thunk, qctx);
        m_umdQueryTime += (System.currentTimeMillis() - t0);
        thunk.finishProgress();
        m_imdTime += (System.currentTimeMillis() - start);
        return recordings;
    }

    static void recordMissingMaterialDifferences
            (Bidder bidder, SQLConnector connector, SQLContext sctx,
             QueryContext qctx)
    {
        String query =
        "SELECT advertiser_id, campaign_id, observation_time,\n" +
        "       sequence_number\n" +
        "FROM observeddata o1\n" +
        "WHERE materially_different = true\n" +
        "AND   has_material_differences = false\n" +
        "ORDER BY advertiser_id, campaign_id,\n" +
        "         sequence_number, observation_time;";
        SexpLoc results = new SexpLoc();
        ResultCollectingThunk thunk =
                new ResultCollectingThunk(connector, results, -1);
        thunk.setMuffleSQLTrace(true);
        thunk.setUniquify(false);
        connector.runSQLQuery(query, thunk, qctx);
        thunk.finishProgress();
        Sexpression l = results.getSexp();
        while(l != Null.nil)
        {
            Sexpression row = l.car();
            Long advertiserId = row.first().unboxLong();
            Long campaignId = row.second().unboxLong();
            Date observationTime = row.third().unboxDate();
            Long sequenceNumber = row.fourth().unboxLong();
            CampaignChangeCount res = describeMaterialDifferences
                        (advertiserId, campaignId, observationTime,
                         sequenceNumber, bidder, sctx, qctx);
            String materialDiffs = (res == null ? "" : res.describe().replace("'", "''"));
            boolean hasDiffs = (materialDiffs != null) && (!materialDiffs.isEmpty());
            String updateQuery =
                    "UPDATE observeddata\n" +
                    "  SET has_material_differences = " + hasDiffs + ",\n" +
                    "      material_differences = '" + materialDiffs + "',\n" +
                    "      attribute_Changed_But_Will_Not_Affect_Delivery = " + (res == null ? "null" : res.attribute_Changed_But_Will_Not_Affect_Delivery) + ",\n" +
                    "      attribute_Changed_With_Unknown_Effect_On_Delivery = " + (res == null ? "null" : res.attribute_Changed_With_Unknown_Effect_On_Delivery) + ",\n" +
                    "      attribute_Changed_Increases_Delivery = " + (res == null ? "null" : res.attribute_Changed_Increases_Delivery) + ",\n" +
                    "      attribute_Changed_Decreases_Delivery = " + (res == null ? "null" : res.attribute_Changed_Decreases_Delivery) + ",\n" +
                    "      attribute_Increased_Increases_Delivery = " + (res == null ? "null" : res.attribute_Increased_Increases_Delivery) + ",\n" +
                    "      attribute_Decreased_Decreases_Delivery = " + (res == null ? "null" : res.attribute_Decreased_Decreases_Delivery) + ",\n" +
                    "      attribute_Increased_Decreases_Delivery = " + (res == null ? "null" : res.attribute_Increased_Decreases_Delivery) + ",\n" +
                    "      attribute_Decreased_Increases_Delivery = " + (res == null ? "null" : res.attribute_Decreased_Increases_Delivery) + ",\n" +
                    "      targeting_Widened_Increases_Delivery = " + (res == null ? "null" : res.targeting_Widened_Increases_Delivery) + ",\n" +
                    "      targeting_Narrowed_Decreases_Delivery = " + (res == null ? "null" : res.targeting_Narrowed_Decreases_Delivery) + "\n" +
                    "WHERE campaign_id = " + row.second().unboxLong()  + "\n" +
                    "AND   advertiser_id = " + row.first().unboxLong() + "\n" +
                    "AND   sequence_number = " + sequenceNumber +";";
            connector.runSQLUpdate(updateQuery, qctx);
            l = l.cdr();
        }
    }

    //=========================================================================

    public static void run(Long advertiserId, Long campaignId, Bidder bidder)
    {
        bidder.ensureBidderSQLConnector();
        SentenceTemplate getDifferingTimesQuery =
                new SentenceTemplate(
                        "(ask-all (?ObsTime ?SequenceNumber)\n" +
                        "  (CBO_DB.ObservedData :Advertiser_id ?AdvertiserId :Campaign_id ?CampaignId :observation_time ?ObsTime :Sequence_number ?SequenceNumber :Materially_different 1))");
        QueryContext qctx = new BasicQueryContext(null, null);
        SQLConnector connector = bidder.ensureBidderSQLConnector();
        SQLContext sctx = connector.allocateSQLContext(qctx);
        BindingList bl = BindingList.truth();
        bl.bind("?AdvertiserId", new NumberAtom(advertiserId));
        bl.bind(  "?CampaignId", new NumberAtom(campaignId));
        Sexpression query = getDifferingTimesQuery.instantiate(bl).get(0);
        Sexpression rows =
                Utils.interpretACL
                        (Integrator.INTEGRATOR,
                         Cons.list(Syms.Request, query, Null.nil), qctx);
        while(rows != Null.nil)
        {
            Sexpression row = rows.car();
            Sexpression observationTime = row.car();
            Sexpression sequenceNumber  = row.second();
            CampaignChangeCount res = describeMaterialDifferences
                        (advertiserId, campaignId, observationTime.unboxDate(),
                         sequenceNumber.unboxLong(), bidder, sctx, qctx);
            System.out.println
                    ("-------------- Time: " + observationTime +
                     " Advertiser: " + advertiserId +
                     " Campaign: " + campaignId +
                     " ObservationTime: " + observationTime +
                     " SequenceNumber: "  + sequenceNumber +
                     " Res: " + res);
            rows = rows.cdr();
        }
    }

    public static void runTests(Set<String> selectedAdvertisers,
                                Set<String> selectedCampaigns,
                                Bidder bidder)
    {
        QueryContext qctx =
                    new BasicQueryContext(null, bidder.appNexusTheory);
        SQLConnector connector = bidder.ensureBidderSQLConnector();
        SQLContext sctx = connector.allocateSQLContext(qctx);
        recordMissingMaterialDifferences(bidder, connector, sctx, qctx);
        /*
        for(String advertiserId: selectedAdvertisers)
        {
            for(String campaignId: selectedCampaigns)
            {
                run(Long.parseLong(advertiserId), Long.parseLong(campaignId),
                    bidder);
            }
        }
        */
    }
}

class Recordings {
    static int flushThreshold = 500;
    Map<Boolean, Map<Long, Map<Long, List<Date>>>> recordings =
            new HashMap<Boolean, Map<Long, Map<Long, List<Date>>>>();

    static Recordings recordMaterialDiffUpdate
            (boolean differentP, Long advertiserId, Long campaignId,
             Date observationTime, Recordings recordings)
    {
        if(recordings == null) recordings = new Recordings();

        Map<Long, Map<Long, List<Date>>> diffEntry =
                recordings.recordings.get(differentP);
        if(diffEntry == null)
        {
            diffEntry = new HashMap<Long, Map<Long, List<Date>>>();
            recordings.recordings.put(differentP, diffEntry);
        }
        Map<Long, List<Date>> advertiserEntry = diffEntry.get(advertiserId);
        if(advertiserEntry == null)
        {
            advertiserEntry = new HashMap<Long, List<Date>>();
            diffEntry.put(advertiserId, advertiserEntry);
        }
        List<Date> campaignEntry = advertiserEntry.get(campaignId);
        if(campaignEntry == null)
        {
            campaignEntry = new Vector<Date>();
            advertiserEntry.put(campaignId, campaignEntry);
        }
        campaignEntry.add(observationTime);
        return recordings;
    }

    static void maybeFlushRecordings
            (SQLConnector connector, QueryContext qctx,
             Recordings recordings, boolean forceP, String column)
    {
      if(recordings != null)
      {
        for(Boolean differentP: recordings.recordings.keySet())
        {
            Map<Long, Map<Long, List<Date>>> differentEntry =
                    recordings.recordings.get(differentP);
            if(differentEntry != null)
            {
                for(Long advertiserId: differentEntry.keySet())
                {
                    Map<Long, List<Date>> advertiserEntry =
                            differentEntry.get(advertiserId);
                    if(advertiserEntry != null)
                    {
                        for(Long campaignId: advertiserEntry.keySet())
                        {
                            List<Date> times =
                                    advertiserEntry.get(campaignId);
                            if(times != null && times.size() > 0 &&
                                    (forceP || times.size() >= flushThreshold))
                            {
                                StringBuffer updateQuery =
                                        new StringBuffer
                                        ("UPDATE observeddata\n" +
                                         "  SET " + column + " = " +
                                         (differentP ? 1 : 0) + "\n" +
                                         "WHERE advertiser_id = " + advertiserId + "\n" +
                                         "AND   campaign_id = " + campaignId + "\n" +
                                         "AND   observation_time IN\n      (");
                                boolean firstP = true;
                                for(Date date: times)
                                {
                                    if(firstP) firstP = false;
                                    else updateQuery.append(",\n       ");
                                    updateQuery.append("'");
                                    updateQuery.append(connector.dateToSQL(date));
                                    updateQuery.append("'");
                                }
                                updateQuery.append(");");
                                connector.runSQLUpdate(updateQuery.toString(), qctx);
                                times.clear();
                            }
                        }
                    }
                }
            }
        }
      }
    }
}