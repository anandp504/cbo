package com.tumri.cbo.backend;

import com.tumri.mediabuying.appnexus.AbstractAppNexusService;
import com.tumri.mediabuying.appnexus.BidSpec;
import com.tumri.mediabuying.appnexus.services.AdvertiserService;
import com.tumri.mediabuying.appnexus.services.CampaignService;
import com.tumri.mediabuying.appnexus.services.CreativeService;
import com.tumri.mediabuying.appnexus.services.ProfileService;
import com.tumri.mediabuying.zini.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.*;
import java.util.*;

public class TargetingComparisonPerspective extends Perspective {

    public static TargetingComparisonPerspective PERSPECTIVE =
	                    new TargetingComparisonPerspective();

    private TargetingComparisonPerspective()
    {
        super("Targeting comparison", -10);
    }

    TargetingComparisonPerspective(String name, int priority)
    {
        super(name, priority);
    }

    public boolean applicableTo(Object o, boolean admin)
    {
        if(o instanceof AdvertiserService) return true;
        else if(o instanceof AdvertiserData)
        {
            AdvertiserData a = (AdvertiserData) o;
            int count = 0;
            for(CampaignData cd: a.campaignData)
            {
                if(cd.getCampaignProfile() != null)
                {
                    count = count + 1;
                    if(count >= 2) return true;
                }
            }
            return false;
        }
        else if(o instanceof Bidder)
        {
            Bidder b = (Bidder) o;
            Map<Long, AdvertiserData> advMap = b.getAdvertiserMap();
            if(advMap == null) return false;
            {
                for(AdvertiserData a: b.getAdvertiserMap().values())
                {
                    if(applicableTo(a, admin)) return true;
                }
                return false;
            }
        }
        else return false;
    }

    static Set<Object> UNIMPORTANT_KEYS =
            new HashSet<Object>
                    (Arrays.asList
                            (
                                    "id",
                                    "name",
                                    "description",
                                    "max_day_imps",
                                    "max_session_imps",
                                    "min_minutes_per_imp",
                                    "min_session_imps",
                                    "require_cookie_for_freq_cap",
                                    "session_freq_type",
                                    "stats",
                                    "last_modified",
                                    "created_on"
                            ));

    static Subsumption compare1(JSONObject a, JSONObject b)
    {
        // Some day, try to switch polarity of subsumption for values with
        // an associated *action which is set to exclude.
        int subsumesCount = 0;
        int subsumedByCount = 0;
        int disjointCount = 0;
        int sameCount = 0;
        for(Object aKey: a.keySet())
        {
            Object aValue = a.get(aKey);
            Object bValue = b.get(aKey);
            Subsumption s = compare(aValue, bValue);
            if(UNIMPORTANT_KEYS.contains(aKey)) {} // Do nothing
            else if(s == Subsumption.Same) sameCount = sameCount + 1;
            else if(s == Subsumption.Subsumes)
                subsumesCount = subsumesCount + 1;
            else if(s == Subsumption.SubsumedBy)
                subsumedByCount = subsumedByCount + 1;
            else if(s == Subsumption.Disjoint)
                disjointCount = disjointCount + 1;
            else throw Utils.barf("Unhandled subsumption value", null, s);
        }
        for(Object bKey: b.keySet())
        {
            Object aValue = a.get(bKey);
            Object bValue = b.get(bKey);
            Subsumption s = compare(aValue, bValue);
            if(UNIMPORTANT_KEYS.contains(bKey)) {} // Do nothing
            else if(s == Subsumption.Same) sameCount = sameCount + 1;
            else if(s == Subsumption.Subsumes)
                subsumesCount = subsumesCount + 1;
            else if(s == Subsumption.SubsumedBy)
                subsumedByCount = subsumedByCount + 1;
            else if(s == Subsumption.Disjoint)
                disjointCount = disjointCount + 1;
            else throw Utils.barf("Unhandled subsumption value", null, s);
        }
        if(subsumesCount > 0)
        {
            if(subsumedByCount > 0) return Subsumption.Disjoint;
            else return (disjointCount > 0
                            ? Subsumption.Disjoint
                            : Subsumption.Subsumes);
        }
        else if(subsumedByCount > 0)
        {
            if(disjointCount > 0) return Subsumption.Disjoint;
            else return Subsumption.SubsumedBy;
        }
        else if(disjointCount > 0) return Subsumption.Disjoint;
        else return Subsumption.Same;
    }

    static Subsumption compare1(JSONArray a, JSONArray b)
    {
        int subsumesCount = 0;
        int subsumedByCount = 0;
        int disjointCount = 0;
        int sameCount = 0;
        for(Object aValue: a)
        {
            if(b.contains(aValue)) sameCount = sameCount + 1;
            else subsumedByCount = subsumedByCount + 1;
        }
        for(Object bValue: b)
        {
            if(a.contains(bValue)) sameCount = sameCount + 1;
            else subsumesCount = subsumesCount + 1;
        }
        if(subsumesCount > 0)
        {
            if(subsumedByCount > 0) return Subsumption.Disjoint;
            else return (disjointCount > 0
                            ? Subsumption.Disjoint
                            : Subsumption.Subsumes);
        }
        else if(subsumedByCount > 0)
        {
            if(disjointCount > 0) return Subsumption.Disjoint;
            else return Subsumption.SubsumedBy;
        }
        else if(disjointCount > 0) return Subsumption.Disjoint;
        else return Subsumption.Same;

    }

    static Subsumption compare(Object a, Object b)
    {
        if(a == b) return Subsumption.Same;
        else if(a == null) return Subsumption.Subsumes;
        else if(b == null) return Subsumption.SubsumedBy;
        else if(a.equals(b)) return Subsumption.Same;
        else if(a.getClass() == b.getClass())
        {
            if(a instanceof JSONObject && b instanceof JSONObject)
                return compare1((JSONObject) a, (JSONObject) b);
            else if(a instanceof JSONArray && b instanceof JSONArray)
                return compare1((JSONArray) a, (JSONArray) b);
            else return Subsumption.Disjoint;
        }
        else return Subsumption.Disjoint;
    }

    static String itemize(Object o, String printedRepresentation)
    {
        return HTMLifier.anchorIfReasonable
                    (o, printedRepresentation,
                     "../" + InspectHTTPHandler.urlName + "/",
                     Manager.MANAGER, null, true, null);
    }

    static boolean emitAdvertiserHeading
            (Writer stream, AdvertiserService a, boolean heading)
	        throws IOException
    {
        if(heading)
        {
            stream.append("\n  <TR CLASS=\"SlotValueRow\">");
            stream.append("\n    <TH ALIGN=\"LEFT\" COLSPAN=\"2\">");
            stream.append("<H3>Advertiser: ");
            stream.append(itemize(a, a.getName() + " (" +
                                  a.getId() + ")"));
            stream.append("</H3></TH>");
        }
        stream.append("\n  <TR CLASS=\"SlotValueRow\">");
        stream.append("\n    <TH ALIGN=\"LEFT\">Campaign</TH>");
        stream.append("\n    <TH ALIGN=\"LEFT\">Targeting Differences</TH>");
        return true;
    }

    static boolean emitCampaignHeading(Writer stream, CampaignService campaign,
                                       CampaignProfile cp)
	        throws IOException
    {
        stream.append("\n  <TR CLASS=\"SlotValueRow\">");
        stream.append("\n    <TD VALIGN=\"TOP\"><NOBR>");
        stream.append
                (HTMLifier.anchorIfReasonable
                        ((cp == null ? campaign : cp),
                         campaign.getName() + " (" + campaign.getId()
                         +")", "../" + InspectHTTPHandler.urlName + "/",
                         Manager.MANAGER, null, true, null));
        stream.append("</NOBR>\n    </TD>");
        return true;
    }

    @SuppressWarnings("unchecked")
    public List<String> getCreativeGeometries(CampaignService c)
    {
        List<CreativeService> creatives = c.getCreatives();
        List<String> geometries = new Vector<String>();
        for(CreativeService cs: creatives)
        {
            geometries.add("" + cs.getWidth() + "x" + cs.getHeight());
        }
        return geometries;
    }

    public Double getBid(CampaignService c)
    {
        return c.getCpm_bid_type().equals(BidSpec.BASE_BID_MODE)
                ? c.getBase_bid()
                : c.getMax_bid();
    }

    @SuppressWarnings({"unused", "SimplifiableIfStatement"})
    public boolean bidExceeds(CampaignService a, CampaignService b)
    {
        Double aBid = getBid(a);
        Double bBid = getBid(b);
        if(aBid == null)
            return false;
        else if(bBid == null) return false;
        else return aBid > bBid;
    }

    @SuppressWarnings("SimplifiableIfStatement")
    public boolean bidIsLess(CampaignService a, CampaignService b)
    {
        Double aBid = getBid(a);
        Double bBid = getBid(b);
        if(aBid == null)
            return false;
        else if(bBid == null) return false;
        else return aBid < bBid;
    }

    public boolean creativeSizesDisjoint(CampaignService a, CampaignService b)
    {
        if(a == null || b == null) return true;
        else
        {
            List<String> aGeometries = getCreativeGeometries(a);
            List<String> bGeometries = getCreativeGeometries(b);
            for(String ag: aGeometries)
            {
                if(!bGeometries.contains(ag)) return true;
                else {}
            }
            for(String bg: aGeometries)
            {
                if(!aGeometries.contains(bg)) return true;
                else {}
            }
            return false;
        }
    }

    Map<Long, List<CampaignProfile>> getAdvertiserDataFor
            (AdvertiserService a, Bidder bidder, QueryContext qctx)
    {   // Maybe rewrite this as a Zini query some day.
        SQLConnector connector = bidder.ensureBidderSQLConnector();
        Map<Long, List<CampaignProfile>> res =
                new HashMap<Long, List<CampaignProfile>>();
        String query =
        "SELECT advertiser_id, combined_json, combined_profile_json\n" +
        "FROM observeddata o1\n" +
        "WHERE NOT EXISTS(SELECT * FROM observeddata o2\n" +
        "                 WHERE o1.advertiser_id = o2.advertiser_id\n" +
        "                 AND   o1.campaign_id = o2.campaign_id\n" +
        "                 AND   o1.observation_time < o2.observation_time)\n"+
        (a == null ? "" : "AND   advertiser_id = " + a.getId() + "\n") +
        "ORDER BY advertiser_id, campaign_id;";
        SexpLoc results = new SexpLoc();
        ResultCollectingThunk thunk =
                new ResultCollectingThunk(connector, results, -1);
        connector.runSQLQuery(query, thunk, qctx);
        Sexpression l = results.getSexp();
        while(l != Null.nil)
        {
            Sexpression row = l.car();
            Long advertiserId = row.car().unboxLong();
            List<CampaignProfile> entry = res.get(advertiserId);
            if(entry == null)
            {
                entry = new Vector<CampaignProfile>();
                res.put(advertiserId, entry);
            }
            entry.add(new CampaignProfile
                    ((JSONObject)SQLHTTPHandler.toJSON(row.second()),
                     (JSONObject)SQLHTTPHandler.toJSON(row.third())));
            l = l.cdr();
        }
        return res;
    }

    public boolean htmlifyAdvertiser
            (Writer stream, AdvertiserService a, List<CampaignProfile> pairs,
             boolean heading, List<CampaignProfile> pairsToCompare)
	        throws IOException
    {
        if(pairsToCompare == null) pairsToCompare= pairs;
        boolean headingEmitted = false;
        int j = 0;
        for(CampaignProfile cp: pairs)
        {
            List<ProfileService> otherProfiles = new Vector<ProfileService>();
            List<CampaignProfile> otherPairs = new Vector<CampaignProfile>();
            ProfileService prof1 = cp.profile;
            int i = 0;
            for(CampaignProfile cp2: pairsToCompare)
            {
                if(cp2 != cp && cp2.profile != null)
                {
                    otherPairs.add(cp2);
                    otherProfiles.add(cp2.profile);
                }
                i = i + 1;
            }
            if(otherProfiles.size() > 0)
            {
                //------------
                boolean campaignHeaderEmitted = false;
                i = 0;
                for(ProfileService prof2: otherProfiles)
                {
                    CampaignProfile otherPair = otherPairs.get(i);
                    JSONObject prof1JSON = prof1.serviceToJSONUnwrapped();
                    JSONObject prof2JSON = prof2.serviceToJSONUnwrapped();
                    Subsumption comparison = compare(prof1JSON, prof2JSON);
                    String profS = CampaignDataHistoryPerspective.diffJson1
                            (prof1JSON, prof2JSON, 0, UNIMPORTANT_KEYS);
                    if(creativeSizesDisjoint
                            (cp.campaign, otherPair.campaign))
                        {} // Do nothing
                    else if(comparison == Subsumption.Disjoint) {}// Do nothing
                    else if(comparison == Subsumption.Same)
                    {
                        if(cp.campaign.getId() < otherPair.campaign.getId())
                        {
                            headingEmitted = headingEmitted ||
                                    emitAdvertiserHeading(stream, a, heading);
                            if(campaignHeaderEmitted)
                                stream.append("\n  <TR><TD>&nbsp;</TD>");
                            else campaignHeaderEmitted =
                                  emitCampaignHeading(stream, cp.campaign, cp);
                            stream.append("\n    <TD VALIGN=\"TOP\">");
                            stream.append("<H4><i>Equivalent to </i>");
                            stream.append
                                    (itemize
                                     (otherPair,
                                      otherPair.campaign.getName() + " (" +
                                      otherPair.campaign.getId() + ")"));
                            stream.append("</H4>");
                            stream.append("\n    </TD>\n  </TR>");
                        }
                    }
                    else if(comparison == Subsumption.SubsumedBy) {}
                    else if(comparison == Subsumption.Subsumes
                            // Only show one polarity
                            // || comparison == Subsumption.SubsumedBy
                            )
                    {
                        headingEmitted = headingEmitted ||
                                emitAdvertiserHeading(stream, a, heading);
                        if(campaignHeaderEmitted)
                            stream.append("\n  <TR><TD>&nbsp;</TD>");
                        else campaignHeaderEmitted =
                                  emitCampaignHeading(stream, cp.campaign, cp);
                        stream.append("\n    <TD VALIGN=\"TOP\">");
                        stream.append("\n<TABLE BORDER=1>");
                        stream.append("\n  <TR CLASS=\"SlotValueRow\">");
                        stream.append("\n    <TD VALIGN=\"TOP\">");
                        if(comparison == Subsumption.Subsumes)
                            stream.append("Subsumes: ");
                        else stream.append("Subsumed By: ");
                        stream.append(itemize
                                (otherPair,
                                 otherPair.campaign.getName() + " (" +
                                 otherPair.campaign.getId() + ")"));
                        if(bidIsLess(cp.campaign, otherPair.campaign))
                        {
                            // A subsumes B, but bids less, and this should
                            // be OK, but if the difference in bids is not
                            // substantial, this is probably still wrong, so
                            // we show the deltas.
                            stream.append(", bids are OK (");
                            stream.append(getBid(cp.campaign).toString());
                            stream.append(" vs. ");
                            stream.append
                                    (getBid(otherPair.campaign).toString());
                            stream.append(").");
                        }
                        stream.append("\n    </TD>\n  </TR>");
                        stream.append("\n  <TR CLASS=\"SlotValueRow\">");
                        stream.append("\n    <TD VALIGN=\"TOP\"><PRE>");
                        stream.append(profS);
                        stream.append("</PRE>\n    </TD>");
                        stream.append("\n  </TR>");
                        stream.append("\n</TABLE>");
                        stream.append("\n    </TD>\n  </TR>");
                    }
                    else throw Utils.barf
                        ("Unhandled subsumption", null, comparison);
                    i = i + 1;
                }
            }
            j = j + 1;
        }
        return headingEmitted;
    }

    List<CampaignProfile> cpsFromAdvertiserData(AdvertiserData a)
    {
        List<CampaignProfile> res = new Vector<CampaignProfile>();
        if(a.campaignData != null)
        {
            for(CampaignData cd: a.campaignData)
            {
                res.add(new CampaignProfile(cd.campaign, cd.campaignProfile));
            }
        }
        return res;
    }

    List<CampaignProfile> cpsFromAdvertiserData
            (AdvertiserService a, Agent agent, QueryContext qctx)
    {
        List<CampaignProfile> res = new Vector<CampaignProfile>();
        Bidder bidder = Bidder.getInstance(false);
        if(bidder != null)
        {
            boolean selectExpiredCampaigns = bidder.selectExpiredCampaigns;
            Date endsGTETime = bidder.getEndsGTETime();
            Sexpression campaigns =
                    bidder.getAdvertiserCampaigns
                        (a.getId(), agent, qctx,
                         selectExpiredCampaigns, endsGTETime);
            while(campaigns != Null.nil)
            {
                Sexpression spec = campaigns.car();
                res.add(new CampaignProfile((CampaignService)spec.fourth(),
                                             (ProfileService)spec.fifth()));
                campaigns = campaigns.cdr();
            }
        }
        return res;
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
        Sexpression advertisers;
        List<CampaignProfile> pairs;
        if(x instanceof AdvertiserData)
        {
            AdvertiserData a = (AdvertiserData) x;
            pairs = cpsFromAdvertiserData(a);
            if(a.service == null || a.campaignData == null ||
               !htmlifyAdvertiser(stream, a.service, pairs, false, null))
            {
                stream.append("\n  <TR CLASS=\"SlotValueRow\">");
                stream.append("\n    <TH ALIGN=\"LEFT\" COLSPAN=\"2\">");
                stream.append("<H3>Nothing to report!</H3></TH>\n  </TR>");
            }
        }
        else if(x instanceof AdvertiserService)
        {
            AdvertiserService a = (AdvertiserService) x;
            if((pairs = cpsFromAdvertiserData
                            (a, Integrator.INTEGRATOR, qctx)).size() > 0 &&
               !htmlifyAdvertiser(stream, a, pairs, false, null))
            {
                stream.append("\n  <TR CLASS=\"SlotValueRow\">");
                stream.append("\n    <TH ALIGN=\"LEFT\" COLSPAN=\"2\">");
                stream.append("<H3>Nothing to report!</H3></TH>\n  </TR>");
            }
        }
        else if(x instanceof Bidder)
        {
            Bidder b = (Bidder) x;
            boolean emittedP = false;
            advertisers = Bidder.fetchSelectedAdvertisers(qctx, null);
            advertisers =
                    Cons.sort(advertisers, Sexpression.Lessp, new NameKey());
            Sexpression l = advertisers;
            while(l != Null.nil)
            {
                AdvertiserService a = (AdvertiserService)l.car();
                Map<Long, List<CampaignProfile>> lookup =
                        getAdvertiserDataFor(a, b, qctx);
                pairs = lookup.get(a.getId());
                if(pairs != null && pairs.size() > 0)
                    emittedP = htmlifyAdvertiser
                            (stream, a, pairs, true, null) || emittedP;
                l = l.cdr();
            }
            if(!emittedP)
            {
                stream.append("\n  <TR CLASS=\"SlotValueRow\">");
                stream.append("\n    <TH ALIGN=\"LEFT\" COLSPAN=\"2\">");
                stream.append("<H3>Nothing to report!</H3></TH>\n  </TR>");
            }
        }
        else throw Utils.barf
              ("Not an AdvertiserData or a Bidder: " + x, this, x, agent,qctx);
    }
}

@SuppressWarnings("unused")
class IdKey implements Key
{
    public Sexpression extract(Sexpression x)
    {
        if(x instanceof AbstractAppNexusService)
        {
            AbstractAppNexusService aans = (AbstractAppNexusService) x;
            return new NumberAtom(aans.getId());
        }
        else throw Utils.barf("Not an AbstractAppNexusService", this, x);
    }
}

class NameKey implements Key
{
    public Sexpression extract(Sexpression x)
    {
        if(x instanceof AbstractAppNexusService)
        {
            AbstractAppNexusService aans = (AbstractAppNexusService) x;
            return new StringAtom((String)aans.getName());
        }
        else throw Utils.barf("Not an AbstractAppNexusService", this, x);
    }
}

enum Subsumption { Subsumes, SubsumedBy, Same, Disjoint }

class CampaignProfile {
    CampaignService campaign;
    ProfileService profile;
    JSONObject campaignJSON;
    JSONObject profileJSON;

    public CampaignProfile(JSONObject campaignJSON, JSONObject profileJSON)
    {
        this.campaignJSON = campaignJSON;
        if(this.campaignJSON != null &&
           this.campaignJSON.get("campaign") != null)
            this.campaignJSON = (JSONObject) this.campaignJSON.get("campaign");
        this.profileJSON = profileJSON;
        if(this.profileJSON != null &&
           this.profileJSON.get("profile") != null)
            this.profileJSON = (JSONObject) this.profileJSON.get("profile");
        this.campaign =
              (campaignJSON == null
                      ? null
                      : new CampaignService(this.campaignJSON));
        this.profile =
              (profileJSON == null
                      ? null
                      : new ProfileService(this.profileJSON));
    }

    public CampaignProfile(CampaignService campaign, ProfileService profile)
    {
        this.campaign = campaign;
        this.profile = profile;
        this.campaignJSON =
                (campaign == null ? null : campaign.serviceToJSONUnwrapped());
        this.profileJSON =
                (profile == null ? null : profile.serviceToJSONUnwrapped());
    }

    public String toString()
    {
        return "#<CampaignProfile " +
                (campaign == null ? "???" : campaign.getId()) + "/" +
                (profile == null ? "???" : profile.getId()) + ">";
    }
}