package com.tumri.cbo.backend;

import com.tumri.mediabuying.appnexus.*;
import com.tumri.mediabuying.zini.*;
import org.json.simple.JSONObject;

import java.io.*;
import java.util.*;
//import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;

public class CampaignJSONCompareHTTPHandler extends BidderGrapherHTTPHandler {

    public void registerHandler(HTTPListener l)
    {
        setListener(l);
        HTTPListener.registerHandler(getUrlName(), this);
    }

    static String urlName = "CAMPAIGNJSONCOMPARE";
    static String prettyName = "Campaign Compare";

    @SuppressWarnings("unused")
    public CampaignJSONCompareHTTPHandler
            (HTTPListener listener, String[] commandLineArgs)
    {
        super(listener, commandLineArgs, urlName, prettyName);
    }

    @SuppressWarnings("unused")
    public CampaignJSONCompareHTTPHandler
            (HTTPListener listener, String[] commandLineArgs, Bidder bidder)
    {
        super(listener, commandLineArgs, urlName, prettyName);
        setBidder(bidder);
    }

    static String advertiserParam1 = "advertiserId1";
    static String advertiserParam2 = "advertiserId2";
    static String campaignParam1   = "campaignId1";
    static String campaignParam2   = "campaignId2";

    MapAndCampaigns buildKidsMap(Sexpression l)
    {
        Sexpression parentCampaigns = Null.nil;
        Map<String, List<Sexpression>> kidsMap
                = new HashMap<String, List<Sexpression>>();
        while(l != Null.nil)
        {
            Sexpression pair = l.car();
            Sexpression name = pair.second();
            String nameS = name.unboxString();
            String canonicalName =
                    AppNexusCampaignSplitter.getCanonicalName(nameS);
            if(!AppNexusCampaignSplitter.isChildCampaign(nameS))
                parentCampaigns = new Cons(pair, parentCampaigns);
            List<Sexpression> list = kidsMap.get(canonicalName);
            if(list == null)
            {
                list = new Vector<Sexpression>();
                kidsMap.put(canonicalName, list);
            }
            list.add(pair);
            l = l.cdr();
        }
        parentCampaigns = Cons.reverse(parentCampaigns);
        return new MapAndCampaigns(kidsMap, parentCampaigns);
    }

    public static Set<Object> UNIMPORTANT_KEYS =
            new HashSet<Object>(Arrays.asList
                    ("stats",
                     "name",
                     "id",
                     "profile_id",
                     "advertiser_id",
                     "code",
                     "description",
                     "last_modified",
                     "created_on"));

    @SuppressWarnings("unchecked")
    public Map<String, String> handle
            (List<String> parsed, String command, String inputLine,
             BufferedReader in, Writer stream,
             OutputStream os, Map<String, String> httpParams,
             boolean returnHeaders)
            throws IOException
    {
        Bidder bidder = ensureBidder();
        SQLConnector connector = bidder.ensureBidderSQLConnector();
        QueryContext qctx = connector.sufficientQueryContextFor();
        String stylesheetUrl = null;
        Map<String, String> headers =
                outputHeaderStuff(stream, stylesheetUrl, returnHeaders,
                                  httpParams);
        Sexpression advertisers = getAdvertisers(connector, qctx);
        Sexpression currentAdvertiser1 =
                outputAdvertiserMenu(stream, advertisers, httpParams,
                                     "Advertiser 1: ", advertiserParam1, true);
        Sexpression currentAdvertiser2 =
                outputAdvertiserMenu(stream, advertisers, httpParams,
                                     "Advertiser 2: ", advertiserParam2, true);
        //=================
        if(currentAdvertiser1 != Null.nil && currentAdvertiser2 != Null.nil)
        {
            Sexpression campaigns1 =
                    getCampaigns(currentAdvertiser1.car(), qctx);
            Sexpression campaigns2 =
                    getCampaigns(currentAdvertiser2.car(), qctx);
            if(campaigns1 != Null.nil && campaigns2 != Null.nil)
            {
                campaigns1 = Cons.sort(campaigns1, Cons.Lessp, Cons.cadrKey);
                campaigns2 = Cons.sort(campaigns2, Cons.Lessp, Cons.cadrKey);
                MapAndCampaigns mapAndCamp1 = buildKidsMap(campaigns1);
                MapAndCampaigns mapAndCamp2 = buildKidsMap(campaigns2);
                Sexpression parentCampaigns1 = mapAndCamp1.parentCampaigns;
                Sexpression parentCampaigns2 = mapAndCamp2.parentCampaigns;
                //Map<String, List<Sexpression>> kidsMap1 = mapAndCamp1.kidsMap;
                //Map<String, List<Sexpression>> kidsMap2 = mapAndCamp2.kidsMap;
                Sexpression currentCampaign1 =
                      outputCampaignMenu(stream, parentCampaigns1, httpParams,
                                         "Campaign 1: ", campaignParam1, true,
                                         false);
                Sexpression currentCampaign2 =
                      outputCampaignMenu(stream, parentCampaigns2, httpParams,
                                         "Campaign 2: ", campaignParam2, true,
                                         false);
                if(currentCampaign1 != Null.nil &&
                   currentCampaign2 != Null.nil)
                {
                    Long campaignId1 = currentCampaign1.first().unboxLong();
                    Long campaignId2 = currentCampaign2.first().unboxLong();
                    //Sexpression campaignName1 = currentCampaign1.second();
                    //Sexpression campaignName2 = currentCampaign2.second();
                    //String campaignName1S = campaignName1.unboxString();
                    //String campaignName2S = campaignName2.unboxString();
                    //List<Sexpression> kidsV1 = kidsMap1.get(campaignName1S);
                    //List<Sexpression> kidsV2 = kidsMap2.get(campaignName2S);
                    //stream.append("\n<HR>\n");
                    //stream.append(escapeHtml("  1:" + currentCampaign1));
                    //stream.append(escapeHtml(", 2:" + currentCampaign2));
                    Observation campProfJo1 = bidder.getJSONFor
                                                    (campaignId1, qctx, true);
                    Observation campProfJo2 = bidder.getJSONFor
                                                    (campaignId2, qctx, true);
                    JSONObject campaignJo1 = campProfJo1.campaignJSON;
                    JSONObject campaignJo2 = campProfJo2.campaignJSON;
                    JSONObject profileJo1 = campProfJo1.profileJSON;
                    JSONObject profileJo2 = campProfJo2.profileJSON;
                    String campS = CampaignDataHistoryPerspective.diffJson
                      (campaignJo1, campaignJo2, "campaign", UNIMPORTANT_KEYS);
                    String profS = CampaignDataHistoryPerspective.diffJson
                      ( profileJo1,  profileJo2,  "profile", UNIMPORTANT_KEYS);
                    if(campS != null || profS != null)
                    {
                        stream.append("\n<TABLE BORDER=\"1\">");
                        stream.append("\n  <TR>");
                        stream.append("\n    <TH>Campaign</TH>");
                        stream.append("<TH>Profile</TH>");
                        stream.append("\n  <TR>");
                        if(campS != null)
                        {
                            stream.append("<TD VALIGN=\"TOP\" NOWRAP=\"nowrap\"><PRE>");
                            stream.append(campS);
                            stream.append("</PRE></TD>");
                        }
                        else stream.append("<TD VALIGN=\"TOP\">&nbsp;</TD>");
                        if(profS != null)
                        {
                            stream.append("<TD VALIGN=\"TOP\" NOWRAP=\"nowrap\"><PRE>");
                            stream.append(profS);
                            stream.append("</PRE></TD>");
                        }
                        else stream.append("<TD VALIGN=\"TOP\">&nbsp;</TD>");
                        stream.append("\n  </TR>");
                        stream.append("\n</TABLE>");
                    }
                    else stream.append("<H3>No significant differences!</H3>");
                }
                else warn(stream, "No campaigns found for these advertisers!");
            }
            else warn(stream, "No campaigns for these advertisers!");
        }
        else warn(stream, "No advertisers found!");
        HTMLifier.finishHTMLPage(stream);
        return headers;
    }

    Map<String, String> outputHeaderStuff
            (Writer stream, String stylesheetUrl, boolean returnHeaders,
             Map<String, String> httpParams)
            throws IOException
    {
        String title = prettyName;
        return handlerPageSetup
                (stream, title, urlName, stylesheetUrl, returnHeaders,
                 httpParams);
    }

    public static void register()
    {
        HTTPListener.registerHandlerClass(CampaignJSONCompareHTTPHandler.class);
    }
    static
    { register(); }
}

class MapAndCampaigns {
    Map<String, List<Sexpression>> kidsMap;
    Sexpression parentCampaigns;

    MapAndCampaigns(Map<String, List<Sexpression>> kidsMap,
                    Sexpression parentCampaigns)
    {
        this.kidsMap = kidsMap;
        this.parentCampaigns = parentCampaigns;
    }
}