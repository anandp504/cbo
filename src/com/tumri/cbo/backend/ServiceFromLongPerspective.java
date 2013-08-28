package com.tumri.cbo.backend;

import com.tumri.mediabuying.appnexus.AbstractAppNexusService;
import com.tumri.mediabuying.appnexus.AppNexusInterface;
import com.tumri.mediabuying.appnexus.AppNexusReturnValue;
import com.tumri.mediabuying.appnexus.services.AdvertiserService;
import com.tumri.mediabuying.zini.*;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

public class ServiceFromLongPerspective extends Perspective {

    public static ServiceFromLongPerspective PERSPECTIVE =
	                    new ServiceFromLongPerspective();

    private ServiceFromLongPerspective()
    {
        super("AppNexus Service", -1);
    }

    public boolean applicableTo(Object o, boolean admin)
    {
        return admin && o instanceof Long;
    }

    public InspectorItem getObjectReplacement(Object x, QueryContext qctx)
    {
        if(x instanceof Long)
        {
            Long l = (Long) x;
            Bidder bidder = Bidder.getInstance(false);
            AbstractAppNexusService s =
                        (bidder != null
                                ? bidder.getServiceFor
                                        (l, qctx, true, false)
                                : null);
            if(s == null && bidder != null)
            {
                boolean advertiserP =
                        CampaignDataHistoryFromLongPerspective.isAnAdvertiser
                                (l, qctx);
                if(advertiserP)
                {
                    AppNexusReturnValue r =
                        AdvertiserService.viewSpecific
                             (bidder.getAppNexusIdentity(),
                              l, AppNexusInterface.INTERVAL_VALUE_UNSPECIFIED);
                    if(r.isGeneralisedServiceList())
                        s = (AbstractAppNexusService)
                                r.coerceToServiceList().get(0);
                }                
            }
            if(s == null) return new InspectorItem
                                (x, null, DefaultPerspective.PERSPECTIVE);
            else return new InspectorItem
                                (s, null, DefaultPerspective.PERSPECTIVE);
        }
        else return null;
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
        throw new Error("Should never get here!");
    }

}
