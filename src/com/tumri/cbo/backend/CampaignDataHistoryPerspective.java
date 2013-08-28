package com.tumri.cbo.backend;

import com.tumri.mediabuying.appnexus.AppNexusInterface;
import com.tumri.mediabuying.appnexus.AppNexusUtils;
import com.tumri.mediabuying.appnexus.NullItemizer;
import com.tumri.mediabuying.appnexus.services.ProfileService;
import com.tumri.mediabuying.zini.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class CampaignDataHistoryPerspective extends Perspective {

    public static CampaignDataHistoryPerspective PERSPECTIVE =
	                    new CampaignDataHistoryPerspective();

    boolean returnCombinedP = false;

    private CampaignDataHistoryPerspective()
    {
        super("Campaign change history", 20);
    }

    CampaignDataHistoryPerspective(String name, int priority)
    {
        super(name, priority);
    }

    public boolean applicableTo(Object o, boolean admin)
    {
        return o instanceof CampaignData;
    }

    @SuppressWarnings("unused")
    static Sexpression getObservedData(QueryContext qctx, CampaignData cd,
                                       boolean returnCombined)
    {
        IndividualVariable v = IndividualVariable.anonymousVariable;
        IndividualVariable _CampaignJson =
                IndividualVariable.getIndVar("CAMPAIGNJSON");
        IndividualVariable _CampaignProfileJson =
                IndividualVariable.getIndVar("CAMPAIGNPROFILEJSON");
        IndividualVariable _CombinedJson =
                IndividualVariable.getIndVar("COMBINEDJSON");
        IndividualVariable _CombinedProfileJson =
                IndividualVariable.getIndVar("COMBINEDPROFILEJSON");
        IndividualVariable _ObservationTime =
                IndividualVariable.getIndVar("OBSERVATIONTIME");
        Long advertiserId = cd.getAdvertiserId();
        Long lineItemId = cd.getLineItemId();
        Long campaignId = cd.getCampaignId();
        ProfileService profile = cd.getCampaignProfile();
        Sexpression campaignProfileId = v;
        if(profile != null)
            campaignProfileId = new NumberAtom(profile.getId());
        Sexpression query =
                Cons.list(Syms.AskAll,
                          (returnCombined
                                  ? Cons.list(_ObservationTime, _CombinedJson,
                                              _CombinedProfileJson)
                                  : Cons.list(_ObservationTime, _CampaignJson,
                                              _CampaignProfileJson)),
                          Sexpression.boxList
                            (Syms.intern("CBO_DB.OBSERVEDDATA"),
                             advertiserId, lineItemId, v, campaignId,
                             campaignProfileId,
                             _ObservationTime, v, v, v, v, v, v, v, v, v,
                             v, v, v, v, v, v, v,
                             _CampaignJson, _CampaignProfileJson,
                             _CombinedJson, _CombinedProfileJson));
        return Utils.interpretACL(Integrator.INTEGRATOR,
                                  Cons.list(Syms.Request, query, Null.nil),
                                  qctx);
    }

    static Sexpression getObservedData
            (QueryContext qctx, Object advertiserId, Object campaignId,
             boolean returnCombined)
    {
        IndividualVariable v = IndividualVariable.anonymousVariable;
        IndividualVariable _CampaignJson =
                IndividualVariable.getIndVar("CAMPAIGNJSON");
        IndividualVariable _CampaignProfileJson =
                IndividualVariable.getIndVar("CAMPAIGNPROFILEJSON");
        IndividualVariable _CombinedJson =
                IndividualVariable.getIndVar("COMBINEDJSON");
        IndividualVariable _CombinedProfileJson =
                IndividualVariable.getIndVar("COMBINEDPROFILEJSON");
        IndividualVariable _ObservationTime =
                IndividualVariable.getIndVar("OBSERVATIONTIME");
        if(advertiserId == null) advertiserId = v;
        if(campaignId   == null)   campaignId = v;
        Sexpression query =
                Cons.list(Syms.AskAll,
                          (returnCombined
                                ? Cons.list(_ObservationTime, _CombinedJson,
                                            _CombinedProfileJson)
                                : Cons.list(_ObservationTime, _CampaignJson,
                                            _CampaignProfileJson)),
                          Sexpression.boxList
                            (Syms.intern("CBO_DB.OBSERVEDDATA"),
                             advertiserId, v, v, campaignId, v,
                             _ObservationTime, v, v, v, v, v, v, v, v, v,
                             v, v, v, v, v, v, v, _CampaignJson,
                             _CampaignProfileJson, _CombinedJson,
                             _CombinedProfileJson));
        return Utils.interpretACL(Integrator.INTEGRATOR,
                                  Cons.list(Syms.Request, query, Null.nil),
                                  qctx);
    }

    static Sexpression getObservedCampaigns
            (QueryContext qctx, Object advertiserId)
    {
        IndividualVariable v = IndividualVariable.anonymousVariable;
        IndividualVariable _CampaignId =
                IndividualVariable.getIndVar("CAMPAIGNID");
        if(advertiserId == null) advertiserId = v;
        Sexpression query =
                Cons.list(Syms.AskAll,
                          _CampaignId,
                          Sexpression.boxList
                            (Syms.intern("CBO_DB.OBSERVEDDATA"),
                             advertiserId, v, v, _CampaignId, v,
                             v, v, v, v, v, v, v, v, v, v,
                             v, v, v, v, v, v, v, v, v));
        return Utils.interpretACL(Integrator.INTEGRATOR,
                                  Cons.list(Syms.Request, query, Null.nil),
                                  qctx);
    }

    static String showAdd(Object o, int indent)
    {
        StringWriter out = new StringWriter();
        try
        {
            AppNexusInterface.pprintJSON
                    (o, indent, out, true, NullItemizer.ITEMIZER);
        }
        catch(IOException e) { throw Utils.barf(e, null, o); }
        return "<FONT COLOR='#00A000'>" + HTTPHandler.htmlify(out) +"</FONT>";
    }

    static String showDel(Object o, int indent)
    {
        StringWriter out = new StringWriter();
        try
        {
            AppNexusInterface.pprintJSON
                    (o, indent, out, true, NullItemizer.ITEMIZER);
        }
        catch(IOException e) { throw Utils.barf(e, null, o); }
        return "<STRIKE><FONT COLOR='#A00000'>" + HTTPHandler.htmlify(out) +
               "</FONT></STRIKE>";
    }

    static boolean same(Object o0, Object o1)
    {
        if(o0 == null) return o1 == null;
        else return o0.equals(o1);
    }

    public static Set<Object> UNIMPORTANT_KEYS =
            new HashSet<Object>(Arrays.asList("stats",
                                              "last_modified",
                                              "created_on"));

    static String diffJsonObj(JSONObject j0, JSONObject j1, int indent,
                              Set<Object> unimportantKeys, boolean grayOutSame)
            throws IOException
    {
        boolean emitted = false;
        StringBuffer res = new StringBuffer();
        for(Object k0: j0.keySet())
        {
            Object v0 = j0.get(k0);
            Object v1 = j1.get(k0);
            if(unimportantKeys.contains(k0)) {}
            else if(same(v0, v1))
            {
                if(v0 != null && grayOutSame)
                {
                    String k0String = k0.toString();
                    if(!emitted)
                    {
                        res.append("{");
                        emitted = true;
                    }
                    else
                    {
                        res.append(",\n");
                        for(int i  = 0; i < indent + 1; i++)
                        {
                            res.append(" ");
                        }
                    }
                    res.append("<FONT COLOR='#C0C0C0'>");
                    res.append(HTTPHandler.htmlify(k0String));
                    res.append(": ");
                    res.append(HTTPHandler.htmlify(v0.toString()));
                    res.append("</FONT>");
                }
            }
            else
            {
                String k0String = k0.toString();
                int newIndent = indent + 3 + k0String.length();
                String s1 = diffJson1(v0, v1, newIndent, unimportantKeys);
                if(s1 != null)
                {
                    if(!emitted)
                    {
                        res.append("{");
                        emitted = true;
                    }
                    else
                    {
                        res.append(",\n");
                        for(int i  = 0; i < indent + 1; i++)
                        {
                            res.append(" ");
                        }
                    }
                    res.append(HTTPHandler.htmlify(k0String));
                    res.append(": ");
                    res.append(s1);
                }
            }
        }
        for(Object k1: j1.keySet())
        {
            Object v0 = j0.get(k1);
            Object v1 = j1.get(k1);
            if(unimportantKeys.contains(k1)) {}
            else if(same(v0, v1) || v0 != null) {}
            else
            {
                String k1String = k1.toString();
                int newIndent = indent + 3 + k1String.length();
                String s1 = diffJson1(v0, v1, newIndent, unimportantKeys);
                if(s1 != null)
                {
                    if(!emitted)
                    {
                        res.append("{");
                        emitted = true;
                    }
                    else
                    {
                        res.append(",\n");
                        for(int i  = 0; i < indent + 1; i++)
                        {
                            res.append(" ");
                        }
                    }
                    res.append(HTTPHandler.htmlify(k1String));
                    res.append(": ");
                    res.append(s1);
                }
            }
        }
        if(emitted) res.append("}");
        return (emitted ? res.toString() : null);
    }

    static JSONObject shouldMatch(Object o, JSONArray j1) // But doesn't!
    {
        if(o instanceof JSONObject)
        {
            JSONObject jo = (JSONObject) o;
            Object id = jo.get("id");
            if(id != null)
            {
                for(Object j1o: j1)
                {
                    if(j1o instanceof JSONObject)
                    {
                        JSONObject jo2 = (JSONObject) j1o;
                        Object id2 = jo2.get("id");
                        if(id.equals(id2)) return jo2;
                    }
                }
                return null;
            }
            else return null;
        }
        else return null;
    }

    static boolean containsIgnoringUnimportantKeys
            (Object o0, JSONArray a, Set<Object> unimportantKeys)
            throws IOException
    {
        if(o0 instanceof JSONObject)
        {
            for(Object o1: a)
            {
                String compResult = diffJson1(o0, o1, 0, unimportantKeys);
                if(compResult == null) return true;
            }
            return false;
        }
        else return a.contains(o0);
    }

    static String diffJsonArray(JSONArray j0, JSONArray j1, int indent,
                                Set<Object> unimportantKeys)
            throws IOException
    {
        boolean emitted = false;
        Set<Object> handled = new HashSet<Object>();
        StringBuffer res = new StringBuffer();
        JSONObject shouldMatch;
        for(Object k0: j0)
        {
            if(containsIgnoringUnimportantKeys(k0, j1, unimportantKeys)) {}
            // if(j1.contains(k0)) {}
            else if(k0 instanceof JSONObject &&
                    (shouldMatch = shouldMatch(k0, j1)) != null)
            {
                handled.add(shouldMatch);
                handled.add(k0);
                int newIndent = indent + 1;
                String s1 = diffJsonObj((JSONObject) k0, shouldMatch,
                                        newIndent, unimportantKeys, true);
                if(s1 != null)
                {
                    if(emitted)
                    {
                        res.append(",\n");
                        for(int i  = 0; i < newIndent; i++)
                        {
                            res.append(" ");
                        }
                    }
                    else
                    {
                        emitted = true;
                        res.append("[");
                    }
                    res.append(s1);
                }
            }
            else
            {
                int newIndent = indent + 1;
                String s1 = diffJson1(k0, null, newIndent, unimportantKeys);
                if(s1 != null)
                {
                    if(emitted)
                    {
                        res.append(",\n");
                        for(int i  = 0; i < newIndent; i++)
                        {
                            res.append(" ");
                        }
                    }
                    else
                    {
                        emitted = true;
                        res.append("[");
                    }
                    res.append(s1);
                }
            }
        }
        for(Object k1: j1)
        {
            if(handled.contains(k1)) {}
            else if(containsIgnoringUnimportantKeys(k1, j0, unimportantKeys))
                {}
            else
            {
                int newIndent = indent + 1;
                String s1 = diffJson1(null, k1, newIndent, unimportantKeys);
                if(s1 != null)
                {
                    if(emitted)
                    {
                        res.append(",\n");
                        for(int i  = 0; i < newIndent; i++)
                        {
                            res.append(" ");
                        }
                    }
                    else
                    {
                        emitted = true;
                        res.append("[");
                    }
                    res.append(s1);
                }
            }
        }
        if(emitted) res.append("]");
        return (emitted ? res.toString() : null);
    }

    // Return printed representation in HTML if different, else null.
    static String diffJson1(Object o0, Object o1, int indent,
                            Set<Object> unimportantKeys)
            throws IOException
    {
        if(o0 == o1) return null;
        else if(o0 == null) return showAdd(o1, indent);
        else if(o1 == null) return showDel(o0, indent);
        else if(o0.equals(o1)) return null;
        else if(o0.getClass() == o1.getClass())
        {
            if(o0 instanceof JSONObject && o1 instanceof JSONObject)
            {
                JSONObject j0 = (JSONObject) o0;
                JSONObject j1 = (JSONObject) o1;
                return diffJsonObj(j0, j1, indent, unimportantKeys, false);
            }
            else if(o0 instanceof JSONArray && o1 instanceof JSONArray)
            {
                JSONArray j0 = (JSONArray) o0;
                JSONArray j1 = (JSONArray) o1;
                return diffJsonArray(j0, j1, indent, unimportantKeys);
            }
            else return showDel(o0, indent) + showAdd(o1, indent);
        }
        else return showDel(o0, indent) + showAdd(o1, indent);
    }

    static String diffJson(JSONObject j0, JSONObject j1, String key,
                           Set<Object> unimportantKeys)
            throws IOException
    {
        Object o0 = elideTop(j0, key);
        Object o1 = elideTop(j1, key);
        return diffJson1(o0, o1, 0, unimportantKeys);
    }

    static String diffJson(Sexpression j0, Sexpression j1, String key,
                           Set<Object> unimportantKeys)
            throws IOException
    {
        Object o0 = SQLHTTPHandler.toJSON(j0);
        Object o1 = SQLHTTPHandler.toJSON(j1);
        if(o0 instanceof JSONObject && o1 instanceof JSONObject)
            return diffJson((JSONObject) o0, (JSONObject) o1, key,
                            unimportantKeys);
        else return null;
    }

    static Object elideTop(Object o, String key)
    {
        if(o instanceof JSONObject)
        {
            JSONObject j = (JSONObject) o;
            Object val = j.get(key);
            if(val == null) return o;
            else return val;
        }
        else return o;
    }

    static String dateToString(Sexpression date)
    {
        return HTTPHandler.htmlify(AdvertiserDataChangesPerspective.dateParser.format
                                         (date.unboxDate()));
    }

    static final SimpleDateFormat transferFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

    // Low-rent escape to save us including another jar.
    static String escape(String s)
    {
        return s.replace(" ", "+").replace(":", "%3A");
    }

    List<String> flooredDates(Date d)
    {
        synchronized(transferFormat)
        {
            List<String> res = new Vector<String>();
            res.add(transferFormat.format(AppNexusUtils.hourFloor(d)));
            res.add(transferFormat.format(AppNexusUtils.dayFloor(d)));
            res.add(transferFormat.format(AppNexusUtils.weekFloor(d)));
            res.add(transferFormat.format(AppNexusUtils.monthFloor(d)));
            res.add(transferFormat.format(AppNexusUtils.yearFloor(d)));
            return res;
        }
    }

    public void htmlifyInternal
            (Writer stream, Long campaignId, QueryContext qctx)
	throws IOException
    {
        Sexpression rows =
                Cons.sort(getObservedData(qctx, null, campaignId,
                                          returnCombinedP),
                          Cons.Lessp, Cons.carKey);
        Sexpression lastDate = Null.nil;
        Sexpression lastCampaign = Null.nil;
        Sexpression lastProfile = Null.nil;
        stream.append
             ("<style type='text/css'> pre {display: inline;} </style>\n");
        stream.append("<TH>Date (");
        stream.append(AdvertiserDataChangesPerspective.tzName());
        stream.append(")</TH><TH>Campaign</TH>");
        stream.append("<TH>Profile</TH></TR>");
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
                String campS = (campDiff ? diffJson(lastCampaign, campaign,
                        "campaign",
                        UNIMPORTANT_KEYS)
                        : null);
                String profS = (profDiff ? diffJson(lastProfile, profile,
                        "profile",
                        UNIMPORTANT_KEYS)
                        : null);
                if(campS != null || profS != null)
                {
                    stream.append("\n<TR CLASS=\"SlotValueRow\">");
                    stream.append("<TD VALIGN=\"TOP\"><NOBR>");
                    /*
                    stream.append("<A NAME=\"");
                    stream.append
                            (escape(transferFormat.format(date.unboxDate())));
                    stream.append("\">");
                    stream.append(dateToString(date));
                    stream.append("</A>");
                    */
                    int i = 0;
                    for(String name: flooredDates(date.unboxDate()))
                    {
                        stream.append("<A NAME=\"");
                        stream.append(escape(name));
                        stream.append("\">");
                        if(i == 0) stream.append(dateToString(date));
                        else stream.append(" ");
                        stream.append("</A>");
                        i = i + 1;
                    }
                    stream.append("</NOBR></TD>");
                    if(campS != null)
                    {
                        stream.append("<TD VALIGN=\"TOP\"><PRE>");
                        stream.append(campS);
                        stream.append("</PRE></TD>");
                    }
                    else stream.append
                            ("<TD VALIGN=\"TOP\"><i>Match</i></TD>");
                    if(profS != null)
                    {
                        stream.append("<TD VALIGN=\"TOP\"><PRE>");
                        stream.append(profS);
                        stream.append("</PRE></TD>");
                    }
                    else stream.append
                            ("<TD VALIGN=\"TOP\"><i>Match</i></TD>");
                    stream.append("</TR>");
                }
            }
            lastDate = date;
            lastCampaign = campaign;
            lastProfile = profile;
            rows = rows.cdr();
        }
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
        if(x instanceof CampaignData)
        {
            CampaignData cd = (CampaignData) x;
            htmlifyInternal(stream, cd.getCampaignId(), qctx);
        }
        else throw Utils.barf("Not a CampaignData: " + x, this, x, agent,qctx);
    }
}
