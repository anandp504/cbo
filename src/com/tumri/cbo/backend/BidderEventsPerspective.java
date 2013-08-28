package com.tumri.cbo.backend;

import com.tumri.mediabuying.appnexus.services.AdvertiserService;
import com.tumri.mediabuying.appnexus.services.CampaignService;
import com.tumri.mediabuying.zini.*;
import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

public class BidderEventsPerspective extends Perspective {

    public static BidderEventsPerspective PERSPECTIVE =
            new BidderEventsPerspective();

    private BidderEventsPerspective()
    {
        super("Events", -5);
    }

    public boolean applicableTo(Object o, boolean admin)
    {
        return admin &&
               (o instanceof AdvertiserData ||
                o instanceof CampaignData ||
                o instanceof AdvertiserService ||
                o instanceof CampaignService ||
                o instanceof Long) &&
               Bidder.getInstance(false) != null;
    }

    Sexpression getEventRows(Bidder bidder, Long advertiserId, Long campaignId,
                             String eventType, QueryContext qctx)
    {   // Maybe rewrite this as a Zini query some day.
        SQLConnector connector = bidder.ensureBidderSQLConnector();
        String query =
        "SELECT *\n" +
        "FROM Events o1\n" +
        "WHERE (1 = 1)" +
        (advertiserId != null ? "\nAND advertiser_id = " + advertiserId : "") +
        (campaignId   != null ? "\nAND   campaign_id = " + campaignId   : "") +
        (eventType    != null ? "\nAND    event_type = '" +
                eventType.replace("''", "'") + "'" : "") +
        ";";
        SexpLoc results = new SexpLoc();
        ResultCollectingThunk thunk =
                new ResultCollectingThunk(connector, results, -1);
        connector.runSQLQuery(query, thunk, qctx);
        return results.getSexp();
    }

    @SuppressWarnings("unchecked")
    public void htmlify
            (Writer stream, Object x, Agent agent, QueryContext qctx,
             boolean ziniStructureToo, boolean javaStructureToo,
             boolean showNulls, boolean showStaticFields, boolean anchorKids,
             boolean useFrameHandles, Integer maxFields, Integer maxLen,
             Integer maxPrintLen, String urlPrefix, Agenda<Anchorable> agenda,
             Perspective p, Map<String, String> httpParams)
            throws IOException
    {
        Bidder bidder = Bidder.getInstance(false);
        if(bidder != null)
        {
            stream.append("\n  <TR><TD>");
            Long advertiserId = null;
            Long campaignId = null;
            String eventType = null;
            if(x instanceof AdvertiserData)
            {
                AdvertiserData a = (AdvertiserData) x;
                if(a.service != null)
                    advertiserId = a.service.getId();
                else if(a.id != null)
                    advertiserId = a.id;
                else {}
            }
            else if(x instanceof CampaignData)
            {
                CampaignData cd = (CampaignData) x;
                if(cd.campaign != null)
                {
                    campaignId = cd.campaign.getId();
                    advertiserId = cd.advertiser.getId();
                }
                else if(cd.campaignId != null)
                {
                    campaignId = cd.campaignId;
                    advertiserId = cd.advertiserId;
                }
                else {}
            }
            else if(x instanceof AdvertiserService)
                advertiserId = ((AdvertiserService) x).getId();
            else if(x instanceof CampaignService)
            {
                campaignId = ((CampaignService) x).getId();
                advertiserId = ((CampaignService) x).getAdvertiser_id();
            }
            else if(x instanceof Long) campaignId = (Long) x;
            else throw Utils.barf("Not a handled type", this, x);
            Sexpression rows = getEventRows(bidder, advertiserId, campaignId,
                                            eventType, qctx);
            if(rows == Null.nil && x instanceof Long)
            {
                // Have a second try, if we didn't find a campaign
                // with this ID.
                advertiserId = (Long) x;
                campaignId = null;
                rows = getEventRows(bidder, advertiserId, campaignId,
                                    eventType, qctx);
            }
            if(rows == Null.nil)
                stream.append("<H3>No events found!</H3>");
            else
            {
                String formString =
                        "<H3><FORM METHOD=\"POST\" ACTION=\"../" +
                         InspectHTTPHandler.urlName + "/" +
                         HTMLifier.getFrameHandleURL(x) + "?perspective=" +
                         escapeHtml(PERSPECTIVE.getName()) + "\">";
                stream.append(formString);
                TimeZoneData tzd =
                        BidHistoryHTTPHandler.outputTimeZoneWidgets
                                (stream, bidder, advertiserId, campaignId,
                                 httpParams, qctx);
                // String selectedTimeZone = tzd.getSelectedTimeZoneName();
                stream.append("</FORM></H3>");
                TimeZone localTimeZone = tzd.getLocalTimeZone();
                TimeZone wrtTimeZone = tzd.getWrtTimeZone();
                stream.append("\n<TABLE BORDER=\"1\">");
                stream.append("\n  <TR>");
                for(ColumnData cd: Bidder.eventsColumns)
                {
                    stream.append("\n    <TH>");
                    stream.append(escapeHtml(cd.getName()));
                    stream.append("\n    </TH>");
                }
                stream.append("\n  </TR>");
                Map<Sexpression, List<Object>> mappedObjTable =
                        SQLHTTPHandler.recordObjectsToMap(rows);
                while(rows != Null.nil)
                {
                    Sexpression row = rows.car();
                    stream.append("\n  <TR>");
                    int colIndex = 0;
                    while(row != Null.nil)
                    {
                        Sexpression cell = row.car();
                        ColumnData cd = Bidder.eventsColumns[colIndex];
                        stream.append("\n    <TD>");
                        Object mapped =
                                SQLHTTPHandler.mappedObjects
                                        (cell, qctx, mappedObjTable);
                        //stream.append
                        //   (SQLHTTPHandler.itemise(mapped, cell.princ()));
                        if(cd.getName().equals(Bidder.advertiser_id_ColName))
                        {
                            stream.append
                                (SQLHTTPHandler.pprintCell
                                   (mapped, cell, localTimeZone, wrtTimeZone,
                                    cell.princ()));
                        }
                        else if(cd.getName().equals(Bidder.campaign_id_ColName))
                        {
                            stream.append
                                (SQLHTTPHandler.pprintCell
                                   (mapped, cell, localTimeZone, wrtTimeZone,
                                    cell.princ()));
                        }
                        else stream.append
                                (SQLHTTPHandler.pprintCell
                                   (mapped, cell, localTimeZone, wrtTimeZone,
                                    cell.princ()));
                        stream.append("\n    </TD>");
                        row = row.cdr();
                        colIndex = colIndex + 1;
                    }
                    stream.append("\n  </TR>");
                    rows = rows.cdr();
                }
                stream.append("\n</TABLE>");
            }
            stream.append("\n  </TD></TR>");
        }
        else throw Utils.barf("No bidder found", this);
    }
}
