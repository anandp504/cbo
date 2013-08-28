package com.tumri.cbo.monitor;

import com.tumri.cbo.backend.CampaignData;
import com.tumri.mediabuying.appnexus.AppNexusUtils;
import com.tumri.mediabuying.zini.*;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;
import java.util.TimeZone;

public class BadSiteDistributionWarning extends NonUrgentWarningMonitor {

    static double entropyThreshold = 2.5;
    static int TOP_N_SITES_TO_GET = 5;

    public BadSiteDistributionWarning
            (ReportApplicabilityColumn... includedColumns)
    {
        super(null, includedColumns);
    }

    public AbstractProblem checkCampaign
            (Long advertiserId, CampaignData cd, SQLConnector connector,
             QueryContext qctx, SQLContext sctx, Date now, TimeZone localTz)
    {

        Double entropy = cd.getYesterdayEntropy(qctx);
        if(entropy == null)
        {
            return null; // Can't say!
        }
        else
        {
            if(entropy <= entropyThreshold)
            {
                Sexpression topN = cd.getYesterdayTopNSites
                        (qctx, TOP_N_SITES_TO_GET);
                // Only issue a warning if there are registered topN domains.
                // We might get back a zero entropy for an inactive campaign,
                // but then there would be no topN domains for yesterday.
                if(topN != Null.nil)
                {
                    Messages messages;
                    String text = null;
                    String html = null;
                    for(int i = 0; i < AbstractMonitor.MESSAGES_SIZE; i++)
                    {
                        boolean htmlify = i > AbstractMonitor.TEXT_INDEX;
                        try
                        {
                            StringWriter sb = new StringWriter();
                            sb.append("Entropy = ");
                            sb.append(asOneDecimal(entropy));
                            Sexpression l = topN;
                            while(l != Null.nil)
                            {
                                Sexpression row = l.car();
                                AbstractProblem.nl(sb, 7, htmlify);
                                String site = row.car().unboxString();
                                /*
                                sb.append(site.endsWith(".com")
                                    ? site.substring(0, site.length() - 4)
                                    : site);
                                */
                                if(htmlify) sb.append("<CODE>");
                                sb.append
                                    (AppNexusUtils.intToThousandsString
                                            (row.second().unboxLong(), 9,
                                             (htmlify ? "&nbsp;" : " ")));
                                if(htmlify) sb.append("</CODE>");
                                sb.append(": ");
                                sb.append
                                    (AbstractProblem._html(site, htmlify));
                                l = l.cdr();
                            }
                            if(htmlify) html = sb.toString();
                            else text = sb.toString();
                        }
                        catch(IOException e)
                        {
                            throw Utils.barf(e, this, advertiserId, cd);
                        }
                    }
                    messages = new Messages(text, html);
                    return result(messages, cd, advertiserId);
                }
                else return null;
            }
            else
            {
                return null; // OK!
            }
        }
    }

    public String shortHeading()
    {
        return "Poor site distribution.";
    }

    public String heading()
    {
        return "The following have poor site distribution";
    }
}