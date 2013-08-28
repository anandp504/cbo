package com.tumri.cbo.backend;

import com.tumri.mediabuying.appnexus.AbstractAppNexusService;
import com.tumri.mediabuying.appnexus.AppNexusInterface;
import com.tumri.mediabuying.appnexus.AppNexusReturnValue;
import com.tumri.mediabuying.appnexus.Identity;
import com.tumri.mediabuying.appnexus.agent.AppNexusTheory;
import com.tumri.mediabuying.appnexus.services.AdvertiserService;
import com.tumri.mediabuying.appnexus.services.CampaignService;
import com.tumri.mediabuying.appnexus.services.LineItemService;
import com.tumri.mediabuying.appnexus.services.ProfileService;
import com.tumri.mediabuying.zini.*;
import org.json.simple.JSONObject;
import java.io.*;
import java.util.Map;

public class CampaignCompareToNowPerspective extends Perspective {

    private Identity ident = null;

    public static CampaignCompareToNowPerspective PERSPECTIVE =
	                    new CampaignCompareToNowPerspective();

    private CampaignCompareToNowPerspective()
    {
        super("Compare to Current AppNexus", -1);
    }

    public boolean applicableTo(Object o, boolean admin)
    {
        if(admin)
        {
            if(ident == null)
            {
                Agent agent =
                        Sexpression.referent
                              (Utils.mgrgetobj(Syms.Isa,
                                               Syms.intern("APPNEXUSTHEORY")));
                if(agent != null && agent != Null.nil)
                {
                    AppNexusTheory th = (AppNexusTheory) agent;
                    ident = th.getIdent();
                }
            }
            return ident != null &&
                    (o instanceof CampaignService ||
                            o instanceof ProfileService ||
                            o instanceof LineItemService ||
                            o instanceof AdvertiserService);
        }
        else return false;
    }

    AbstractAppNexusService getCurrentServiceFor(AbstractAppNexusService x)
    {
        if(x instanceof AdvertiserService)
        {
            AdvertiserService service = (AdvertiserService) x;
            AppNexusReturnValue rv =
                AdvertiserService.viewSpecific
                        (ident, service.getId(),
                         AppNexusInterface.INTERVAL_VALUE_LIFETIME);
            if(rv.isGeneralisedServiceList())
            {
                return (AdvertiserService)rv.coerceToServiceList().get(0);
            }
            else return null;
        }
        else if(x instanceof CampaignService)
        {
            CampaignService service = (CampaignService) x;
            AppNexusReturnValue rv =
                CampaignService.viewSpecific
                        (ident, service.getId(),
                         service.getAdvertiser_id(),
                         AppNexusInterface.INTERVAL_VALUE_LIFETIME);
            if(rv.isGeneralisedServiceList())
            {
                return (CampaignService)rv.coerceToServiceList().get(0);
            }
            else return null;
        }
        else if(x instanceof LineItemService)
        {
            LineItemService service = (LineItemService) x;
            AppNexusReturnValue rv =
                LineItemService.viewSpecific
                        (ident, service.getId(),
                         service.getAdvertiser_id(),
                         AppNexusInterface.INTERVAL_VALUE_LIFETIME);
            if(rv.isGeneralisedServiceList())
            {
                return (LineItemService)rv.coerceToServiceList().get(0);
            }
            else return null;
        }
        else if(x instanceof ProfileService)
        {
            ProfileService service = (ProfileService) x;
            AppNexusReturnValue rv =
                ProfileService.viewSpecific
                        (ident, service.getId(), 0L);
            if(rv.isGeneralisedServiceList())
            {
                return (ProfileService)rv.coerceToServiceList().get(0);
            }
            else return null;
        }
        else throw Utils.barf("Not a handled type: " + x, this, x);
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
        boolean admin = HTTPHandler.isAdministratorUser(httpParams);
        if(applicableTo(x, admin))
        {
            AbstractAppNexusService thenService = (AbstractAppNexusService) x;
            JSONObject then = thenService.serviceToJSONUnwrapped();
            AbstractAppNexusService nowService =
                    getCurrentServiceFor(thenService);
            if(nowService == null)
            {
                stream.append("\n<TR CLASS=\"SlotValueRow\">");
                stream.append("<TD VALIGN=\"TOP\"><I>Comparison not possible."
                           + "  Campaign has probably been deleted.</I></TD>");
                stream.append("</TR>");
            }
            else
            {
                JSONObject now = nowService.serviceToJSONUnwrapped();
                boolean diff = !then.equals(now);
                String diffS = (diff ? CampaignDataHistoryPerspective.diffJson1
                        (then, now, 0,
                               CampaignDataHistoryPerspective.UNIMPORTANT_KEYS)
                        : null);
                stream.append("\n<TR CLASS=\"SlotValueRow\">");
                if(diffS != null)
                {
                    stream.append("<TD VALIGN=\"TOP\"><PRE><NOBR>");
                    stream.append(diffS);
                    stream.append("</NOBR></PRE></TD>");
                }
                else
                {
                    stream.append
                            ("<TD VALIGN=\"TOP\"><i>No differences between ");
                    stream.append(HTTPHandler.htmlify(thenService));
                    stream.append(" and ");
                    stream.append(HTTPHandler.htmlify(nowService));
                    stream.append("</i></TD>");
                }
                stream.append("</TR>");
            }
        }
        else throw Utils.barf("Not a handled type: " + x, this, x, agent,qctx);
    }
}
