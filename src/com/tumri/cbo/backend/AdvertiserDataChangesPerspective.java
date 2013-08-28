package com.tumri.cbo.backend;

import com.tumri.mediabuying.appnexus.AbstractAppNexusService;
import com.tumri.mediabuying.appnexus.services.CampaignService;
import com.tumri.mediabuying.zini.*;
import java.io.*;
import java.util.*;

public class AdvertiserDataChangesPerspective extends Perspective {

    public static AdvertiserDataChangesPerspective PERSPECTIVE =
	                    new AdvertiserDataChangesPerspective();

    boolean returnCombinedP = false;

    private AdvertiserDataChangesPerspective()
    {
        super("Changed campaigns for advertiser", 20);
    }

    AdvertiserDataChangesPerspective(String name, int priority)
    {
        super(name, priority);
    }

    public boolean applicableTo(Object o, boolean admin)
    {
        return o instanceof AdvertiserData;
    }

    static Set<Object> UNIMPORTANT_KEYS =
            new HashSet<Object>
                    (Arrays.asList
                            (
                                    "stats",
                                    "last_modified",
                                    "max_bid",
                                    "base_bid",
                                    "cpm_bid_type",
                                    "cpc_goal",
                                    "created_on"
                            ));

    public static SynchDateFormat dateParser =
            new SynchDateFormat("yyyy-MM-dd HH:mm:ss");

    public static String tzName()
    {
        TimeZone tz = Calendar.getInstance().getTimeZone();
        return tz.getDisplayName(tz.inDaylightTime(new Date()),TimeZone.SHORT);
    }

    static void htmlifyInternal
            (Long campaignId, Sexpression rows, Set<Long> cds,
             Map<Long, List<String[]>> cdData)
            throws IOException
    {
        Sexpression lastDate = Null.nil;
        Sexpression lastCampaign = Null.nil;
        Sexpression lastProfile = Null.nil;
        while(rows != Null.nil)
        {
            Sexpression row = rows.car();
            Sexpression date = row.car();
            Sexpression campaign = row.second();
            Sexpression profile = row.third();
            if(lastDate != Null.nil)
            {
                boolean campDiff = !campaign.equals(lastCampaign);
                boolean profDiff = !profile.equals(lastProfile);
                String dateS = dateParser.format(date.unboxDate());
                String campS =
                        (campDiff ? CampaignDataHistoryPerspective.diffJson
                                (lastCampaign, campaign, "campaign",
                                        UNIMPORTANT_KEYS)
                                : null);
                String profS =
                        (profDiff ? CampaignDataHistoryPerspective.diffJson
                                (lastProfile,  profile, "profile",
                                        UNIMPORTANT_KEYS)
                                : null);
                if(campS != null || profS != null)
                {
                    cds.add(campaignId);
                    List<String[]> entry = cdData.get(campaignId);
                    if(entry == null)
                    {
                        entry = new Vector<String[]>();
                        cdData.put(campaignId, entry);
                    }
                    entry.add(new String[] { dateS, campS, profS });
                }
            }
            lastDate = date;
            lastCampaign = campaign;
            lastProfile = profile;
            rows = rows.cdr();
        }
    }

    public static void htmlifyInternal
            (Writer stream, Long advertiserId, QueryContext qctx, 
             boolean returnCombinedP)
	throws IOException
    {
        Set<Long> cds = new LinkedHashSet<Long>();
        Map<Long, List<String[]>> cdData =
                new HashMap<Long, List<String[]>>();
        Sexpression campaigns =
                CampaignDataHistoryPerspective.getObservedCampaigns
                        (qctx, advertiserId);
        while(campaigns != Null.nil)
        {
            Sexpression campaignId = campaigns.car();
            Long campId = campaignId.unboxLong();
            Sexpression rows =
                    Cons.sort(CampaignDataHistoryPerspective.getObservedData
                            (qctx, advertiserId, campId,
                                    returnCombinedP),
                            Cons.Lessp, Cons.carKey);
            htmlifyInternal(campId, rows, cds, cdData);
            campaigns = campaigns.cdr();
        }
        if(cds.size() == 0)
            stream.append("<H3>No significant differences found</H3>");
        else
        {
            stream.append("\n<TR CLASS=\"SlotValueRow\"><TH>Campaign</TH>");
            stream.append("<TH>Change date (");
            stream.append(tzName());
            stream.append(")</TH><TH>Campaign</TH><TH>Profile</TH>");
            for(Long campaignId: cds)
            {
                List<String[]> changes = cdData.get(campaignId);
                stream.append("\n<TR CLASS=\"SlotValueRow\">");
                stream.append("<TD VALIGN=\"TOP\"><NOBR>");
                Bidder bidder = Bidder.getInstance(false);
                AbstractAppNexusService cs =
                        (bidder != null
                                ?bidder.getServiceFor
                                    (campaignId, qctx, true, false)
                                : null);
                if(!(cs instanceof CampaignService))
                    stream.append(campaignId.toString());
                else stream.append
                        (HTMLifier.anchorIfReasonable
                                (cs, ((CampaignService)cs).getName() + " ("
                                        + cs.getId() + ")",
                                        "../" + InspectHTTPHandler.urlName + "/",
                                        Manager.MANAGER, null, true,
                                        advertiserId + "/" + campaignId));
                stream.append("</NOBR></TD>");
                stream.append("<TD VALIGN=\"TOP\"><TABLE BORDER=\"1\">");
                for(String[] data: changes)
                {
                    String dateS = data[0];
                    String campS = data[1];
                    String profS = data[2];
                    stream.append("<TR>");
                    stream.append("<TD VALIGN=\"TOP\"><NOBR>");
                    stream.append(dateS);
                    stream.append("</NOBR></TD>");
                    if(campS == null) stream.append("<TD>&nbsp;</TD>");
                    else
                    {
                        stream.append("<TD VALIGN=\"TOP\"><PRE><NOBR>");
                        stream.append(campS);
                        stream.append("</NOBR></PRE></TD>");
                    }
                    if(profS == null) stream.append("<TD>&nbsp;</TD>");
                    else
                    {
                        stream.append("<TD VALIGN=\"TOP\"><PRE><NOBR>");
                        stream.append(profS);
                        stream.append("</NOBR></PRE></TD>");
                    }
                    stream.append("</TR>");
                }
                stream.append("</TABLE></TD>");
            }
        }
    }

    static String campaignPrintName
            (Long campaignId, CampaignData cd, Sexpression bidder,
             QueryContext qctx)
    {
        String name;
        if(cd != null)
            name = cd.campaign.getName() + " (" +
                   cd.campaign.getId() + ")";
        else
        {
            AbstractAppNexusService cs =
                    (bidder instanceof Bidder
                            ?((Bidder)bidder).getServiceFor
                            (campaignId, qctx, true, false)
                            : null);
            if(!(cs instanceof CampaignService))
                name = campaignId.toString();
            else name = ((CampaignService)cs).getName() + " ("
                         + cs.getId() + ")";
        }
        return name;
    }

    public void htmlify
            (Writer stream, Object x, Agent agent, QueryContext qctx,
             boolean ziniStructureToo, boolean javaStructureToo,
             boolean showNulls, boolean showStaticFields, boolean anchorKids,
             boolean useFrameHandles, Integer maxFields, Integer maxLen,
             Integer maxPrintLen, String urlPrefix, Agenda<Anchorable> agenda,
             Perspective p, Map<String, String> httpParams)
	throws IOException
    {
        if(x instanceof AdvertiserData)
        {
            Bidder bidder = Bidder.getInstance(false);
            AdvertiserData a = (AdvertiserData) x;
            Long advertiserId = a.id;
            Set<Long> cds = new LinkedHashSet<Long>();
            Map<Long, List<String[]>> cdData =
                    new HashMap<Long, List<String[]>>();
            Sexpression campaigns =
                    CampaignDataHistoryPerspective.getObservedCampaigns
                            (qctx, advertiserId);
            while(campaigns != Null.nil)
            {
                Sexpression campaignId = campaigns.car();
                Long campId = campaignId.unboxLong();
                Sexpression rows =
                       Cons.sort(CampaignDataHistoryPerspective.getObservedData
                                    (qctx, advertiserId, campId,
                                     returnCombinedP),
                                 Cons.Lessp, Cons.carKey);
                htmlifyInternal(campId, rows, cds, cdData);
                campaigns = campaigns.cdr();
            }
            stream.append
                 ("<style type='text/css'> pre {display: inline;} </style>\n");
            if(cds.size() == 0)
                stream.append("<H3>No significant differences found</H3>");
            else
            {
                stream.append("\n<UL>");
                for(Long campaignId: cds)
                {
                    CampaignData cd = a.getCampaignData(campaignId);
                    String name =
                            campaignPrintName(campaignId, cd, bidder, qctx);
                    stream.append("\n  <LI><A HREF=\"#");
                    stream.append(advertiserId.toString());
                    stream.append("/");
                    stream.append(campaignId.toString());
                    stream.append("\">");
                    stream.append(name);
                    stream.append("</A>");
                }
                stream.append("\n</UL>");
                for(Long campaignId: cds)
                {
                    List<String[]> changes = cdData.get(campaignId);
                    stream.append("\n<TR CLASS=\"SlotValueRow\">");
                    stream.append("<TD VALIGN=\"TOP\"><NOBR>");
                    CampaignData cd = a.getCampaignData(campaignId);
                    String name = campaignPrintName(campaignId, cd, bidder,
                                                    qctx);
                    if(cd != null)
                        stream.append
                                (HTMLifier.anchorIfReasonable
                                 (cd, name,
                                  "../" + InspectHTTPHandler.urlName + "/",
                                  Manager.MANAGER,
                                  CampaignDataHistoryPerspective.PERSPECTIVE,
                                  true, a.getId() + "/" + cd.campaign.getId()));
                    else
                    {
                        AbstractAppNexusService cs =
                                (bidder != null
                                    ? bidder.getServiceFor
                                        (campaignId, qctx, true, false)
                                    : null);
                        if(!(cs instanceof CampaignService))
                            stream.append(name);
                        else stream.append
                                 (HTMLifier.anchorIfReasonable
                                    (cs, name,
                                    "../" + InspectHTTPHandler.urlName + "/",
                                    Manager.MANAGER, null, true,
                                    a.getId() + "/" + campaignId));
                    }
                    stream.append("</NOBR></TD></TR>");
                    stream.append("\n<TR><TD>");
                    stream.append("\n<TABLE BORDER=\"1\"><TR>");
                    stream.append("<TH>Change date (");
                    stream.append(tzName());
                    stream.append(")</TH><TH>Campaign def</TH><TH>Targeting</TH>");
                    stream.append("</TR>");
                    for(String[] data: changes)
                    {
                        String dateS = data[0];
                        String campS = data[1];
                        String profS = data[2];
                        stream.append("<TR>");
                        stream.append("<TD VALIGN=\"TOP\"><NOBR>");
                        stream.append(dateS);
                        stream.append("</NOBR></TD>");
                        if(campS == null) stream.append("<TD>&nbsp;</TD>");
                        else
                        {
                            stream.append("<TD VALIGN=\"TOP\"><PRE>");
                            stream.append(campS);
                            stream.append("</PRE></TD>");
                        }
                        if(profS == null) stream.append("<TD>&nbsp;</TD>");
                        else
                        {
                            stream.append("<TD VALIGN=\"TOP\"><PRE>");
                            stream.append(profS);
                            stream.append("</PRE></TD>");
                        }
                        stream.append("</TR>");
                    }
                    stream.append("</TABLE>");
                    stream.append("\n</TD></TR>");
                }
            }
        }
        else throw Utils.barf
                ("Not an AdvertiserData: " + x, this, x, agent,qctx);
    }
}
