package com.tumri.cbo.backend;

import com.tumri.mediabuying.zini.*;
import java.io.*;
import java.util.*;

public class CrossAdvertiserTargetingComparisonPerspective 
    extends TargetingComparisonPerspective {

    public static CrossAdvertiserTargetingComparisonPerspective PERSPECTIVE =
	                  new CrossAdvertiserTargetingComparisonPerspective();

    private CrossAdvertiserTargetingComparisonPerspective()
    {
        super("Cross-Advertiser Targeting comparison", -20);
    }

    public boolean applicableTo(Object o, boolean admin)
    {
        return admin && o instanceof AdvertiserData;
    }

    List<CampaignProfile> exhaustiveCps()
    {
        List<CampaignProfile> res = new Vector<CampaignProfile>();
        Bidder bidder = Bidder.getInstance(false);
        if(bidder != null)
        {
            for(AdvertiserData a: bidder.getAdvertiserMap().values())
            {
                if(a.campaignData != null)
                {
                    for(CampaignData cd: a.campaignData)
                    {
                        res.add(new CampaignProfile
                                (cd.campaign, cd.campaignProfile));
                    }
                }
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
        if(x instanceof AdvertiserData)
        {
            AdvertiserData a = (AdvertiserData) x;
            List<CampaignProfile> pairs = cpsFromAdvertiserData(a);
            List<CampaignProfile> otherPairs = exhaustiveCps();
            if(a.service == null || a.campaignData == null ||
               !htmlifyAdvertiser(stream, a.service, pairs, false, otherPairs))
            {
                stream.append("\n  <TR CLASS=\"SlotValueRow\">");
                stream.append("\n    <TH ALIGN=\"LEFT\" COLSPAN=\"2\">");
                stream.append("<H3>Nothing to report!</H3></TH>\n  </TR>");
            }
        }
        else throw Utils.barf
              ("Not an AdvertiserData: " + x, this, x, agent,qctx);
    }
}

