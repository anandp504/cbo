package com.tumri.cbo.backend;

import com.tumri.mediabuying.zini.*;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

public class CampaignDataGraphPerspective extends Perspective {

    public static CampaignDataGraphPerspective PERSPECTIVE =
	                    new CampaignDataGraphPerspective();

    private CampaignDataGraphPerspective()
    {
        super("Graph campaign", 3);
    }

    public boolean applicableTo(Object o, boolean admin)
    {
        return admin && o instanceof CampaignData;
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
            Sexpression currentAdvertiser =
                    Cons.list(new NumberAtom(cd.advertiserId),
                             new StringAtom(cd.advertiser.getName()));
            Sexpression currentCampaign =
                    Cons.list(new NumberAtom(cd.campaignId),
                             new StringAtom(cd.campaign.getName()));
            Sexpression currentStartHour = new DateAtom(cd.getStartDate());
            Sexpression currentEndHour = new DateAtom(cd.getEndDate());
            List<Sexpression> kidsV = new Vector<Sexpression>();
            kidsV.add(currentCampaign);
            BidderGrapherHTTPHandler.outputImpressionsGraph
                    (stream, currentAdvertiser, currentCampaign,
                     currentStartHour, currentEndHour, kidsV, qctx);
            stream.append("\n<HR>\n");
            BidderGrapherHTTPHandler.outputCostGraph
                    (stream, currentAdvertiser, currentCampaign,
                     currentStartHour, currentEndHour, kidsV, qctx);
            stream.append("\n<HR>\n");
            BidderGrapherHTTPHandler.outputBidHistoryGraph
                    (stream, currentAdvertiser, currentCampaign,
                     currentStartHour, currentEndHour, kidsV, qctx);
        }
        else throw Utils.barf("Not a CampaignData: " + x, this, x, agent,qctx);
    }
}
