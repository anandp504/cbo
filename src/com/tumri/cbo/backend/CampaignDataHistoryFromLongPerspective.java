package com.tumri.cbo.backend;

import com.tumri.mediabuying.zini.*;
import java.io.*;
import java.util.Map;

public class CampaignDataHistoryFromLongPerspective 
    extends CombinedDataHistoryPerspective {

    public static CampaignDataHistoryFromLongPerspective PERSPECTIVE =
	                    new CampaignDataHistoryFromLongPerspective();

    private CampaignDataHistoryFromLongPerspective()
    {
        super("Combined change history", -1);
    }

    public boolean applicableTo(Object o, boolean admin)
    {
        return admin && o instanceof Long;
    }

    static SentenceTemplate advertiserQuery =
            new SentenceTemplate(
                    "(ask-one (?AdvertiserId ?AdvertiserName)\n" +
                    "         (CBO_DB.AdvertiserNames ?AdvertiserId ?AdvertiserName))");

    public static boolean isAnAdvertiser(Long advertiserId, QueryContext qctx)
    {
        if(advertiserId == null) return false;
        else
        {
            BindingList bl = BindingList.truth();
            bl.bind("?AdvertiserId", advertiserId);
            Sexpression instantiated = advertiserQuery.instantiate(bl).get(0);
            Sexpression res =
                    Utils.interpretACL(Integrator.INTEGRATOR,
                        Cons.list(Syms.Request, instantiated, Null.nil), qctx);
            return res != Null.nil;
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
        if(x instanceof Long)
        {
            Long l = (Long) x;
            boolean advertiserP = isAnAdvertiser(l, qctx);
            if(advertiserP)
                AdvertiserCombinedDataChangesPerspective.htmlifyInternal
                        (stream, l, qctx, true);
            // Guess it's a campaign.
            else htmlifyInternal(stream, l, qctx);
        }
        else throw Utils.barf("Not a Long: " + x, this, x, agent,qctx);
    }

}
