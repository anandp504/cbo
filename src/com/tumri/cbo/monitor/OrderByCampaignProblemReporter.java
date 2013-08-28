package com.tumri.cbo.monitor;

import com.tumri.cbo.backend.BidHistoryHTTPHandler;
import com.tumri.cbo.backend.CampaignData;
import com.tumri.cbo.backend.CampaignDataHistoryFromLongPerspective;
import com.tumri.mediabuying.zini.Comparators;
import com.tumri.mediabuying.zini.InspectHTTPHandler;
import com.tumri.mediabuying.zini.Utils;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

public class OrderByCampaignProblemReporter
        extends AbstractReporter
        implements ProblemReporter {

    static final String GLOBAL_KEY = "** Global";

    public void summariseProblems
            (Writer writer,
             Map<AbstractProblem,
                 Map<AbstractMonitor, List<AbstractProblem>>> recordedProblems,
             Map<Object, String> outputKey,
             boolean htmlify, boolean admin)
            throws IOException
    {
        List<AbstractProblem> types = new Vector<AbstractProblem>();
        types.addAll(recordedProblems.keySet());
        Collections.sort(types, AbstractProblem.SEVERITY_COMPARATOR);
        int i = 0;
        for(AbstractProblem prototype: types)
        {
            Map<AbstractMonitor, List<AbstractProblem>> monitorMap =
                recordedProblems.get(prototype);
            if(monitorMap != null && monitorMap.size() > 0)
            {
                if(i > 0)
                {
                    nl(writer, htmlify);
                    nl(writer, htmlify);
                }
                // e.g. Warnings or Notifications
                writer.append(prototype.summaryHeading(htmlify));
                Map<String, Map<String, List<AbstractProblem>>>
                    advertiserMap =
                        new HashMap<String,
                                    Map<String, List<AbstractProblem>>>();
                Map<String, Long> advertiserIdMap =
                        new HashMap<String, Long>();
                for(AbstractMonitor key: monitorMap.keySet())
                {
                    List<AbstractProblem> problems = monitorMap.get(key);
                    for(AbstractProblem p: problems)
                    {
                        Long advertiserId = p.getAdvertiserId();
                        String advertiserName =
                                (advertiserId == null
                                        ? GLOBAL_KEY
                                        : p.getAdvertiserName() + " (" +
                                            advertiserId + ")");
                        advertiserIdMap.put(advertiserName, advertiserId);
                        String campaignName =
                                (advertiserId == null
                                    ? GLOBAL_KEY
                                    : p.getCampaignName());
                        Map<String, List<AbstractProblem>> campaignMap
                                = advertiserMap.get(advertiserName);
                        if(campaignMap == null)
                        {
                            campaignMap =
                                new HashMap<String, List<AbstractProblem>>();
                            advertiserMap.put(advertiserName, campaignMap);
                        }
                        List<AbstractProblem> problemList
                                = campaignMap.get(campaignName);
                        if(problemList == null)
                        {
                            problemList = new Vector<AbstractProblem>();
                            campaignMap.put(campaignName, problemList);
                        }
                        problemList.add(p);
                    }
                }
                List<String> advertiserNames =
                        new Vector<String>(advertiserMap.keySet());
                Collections.sort(advertiserNames, Comparators.STRING_EQUAL);
                for(String advertiserName: advertiserNames)
                {
                    Long advertiserId = advertiserIdMap.get(advertiserName);
                    summariseAdvertiserProblems
                           (writer, advertiserName, advertiserId,
                            advertiserMap.get(advertiserName), outputKey,
                            htmlify, admin);
                }
                i = i + 1;
            }
        }
    }

    @SuppressWarnings("unused")
    void summariseAdvertiserProblems
            (Writer writer, String advertiserName, Long advertiserId,
             Map<String, List<AbstractProblem>> campaignMap,
             Map<Object, String> outputKey, boolean htmlify,
             boolean admin)
            throws IOException
    {
        List<String> campaignNames =
                new Vector<String>(campaignMap.keySet());
        Collections.sort(campaignNames, Comparators.STRING_EQUAL);
        if(htmlify && advertiserId != null)
        {
            String hash = advertiserId.toString();
            outputKey.put(advertiserId, hash);
            String url =
                "<U><A NAME=\"" + hash +
                 "\" HREF=\"" + URL_PRELUDE + InspectHTTPHandler.urlName + "/" +
                InspectHTTPHandler.urlName + "?&perspective=" +
                Utils.urlEscape
                    (CampaignDataHistoryFromLongPerspective.PERSPECTIVE.getName()) +
                "&INSPECT=" + advertiserId + "\">";
            writer.append(url);
            writer.append(advertiserName);
            writer.append("</A></U>");
        }
        else underline(writer, advertiserName, htmlify);
        String shortName = AbstractMonitor.removeTrailingId(advertiserName);
        for(String campaignName: campaignNames)
        {
            summariseCampaignProblems
                    (writer, advertiserId, shortName, campaignName,
                     campaignMap.get(campaignName), outputKey, htmlify);
        }
        nl(writer, htmlify);
        nl(writer, htmlify);
    }

    void summariseCampaignProblems(Writer writer, Long advertiserId,
                                   String advertiserShortName,
                                   String campaignName,
                                   List<AbstractProblem> problemList,
                                   Map<Object, String> outputKey,
                                   boolean htmlify)
            throws IOException
    {
        nl(writer, 1, htmlify);
        writer.append("- ");
        String heading =
                AbstractMonitor.simplifyCampaignName
                        (advertiserShortName, campaignName);
        if(htmlify)
        {
            CampaignData cd = null;
            for(AbstractProblem p: problemList)
            {
                cd = p.getCampaignData();
            }
            if(cd == null) writer.append(_html(heading, htmlify));
            else
            {
                Long campaignId = cd.getCampaignId();
                String hash = advertiserId.toString() + "/" +
                              campaignId.toString();
                outputKey.put(campaignId, hash);
                writer.append("<A NAME=\"");
                writer.append(hash);
                writer.append("\" HREF=\"");
                writer.append(BidHistoryHTTPHandler.makeURL(cd));
                writer.append("\">");
                writer.append(_html(heading, htmlify));
                writer.append("</A>");
            }
        }
        else writer.append(heading);
        for(AbstractProblem p: problemList)
        {
            summariseProblem(writer, p, htmlify);
        }
    }

    void summariseProblem(Writer writer, AbstractProblem p, boolean htmlify)
            throws IOException
    {
        nl(writer, 3, htmlify);
        String heading = "- ";
        writer.append(heading);
        writer.append(p.summarise(htmlify, true));
    }
}

