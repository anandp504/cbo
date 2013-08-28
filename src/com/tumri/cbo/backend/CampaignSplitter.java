package com.tumri.cbo.backend;

import com.tumri.mediabuying.appnexus.*;
import com.tumri.mediabuying.appnexus.services.*;
import com.tumri.mediabuying.zini.Utils;
import org.apache.log4j.Level;
import java.util.*;

public class CampaignSplitter {

    static void info(String s)
    {
        System.out.println(s);
        Utils.logThisPoint(Level.INFO, s);
    }

    static boolean verbose = true;
    static boolean doit = true;

    public static void main(String[] args)
    {
        // AppNexusInterface.enableJSONPrinting();
        AppNexusInterface.disableJSONPrinting();
        int numberOfKids = 0;
        long primaryPercentage = 100;
        long ecpPercentage = 0;
        long totalDailyBudget = 1000000;
        double primaryBid = 2.0;
        double bidOffsetPercentage = 10.0;
        double maxBid = 2.35;
        Identity ident =
                (doit ? AppNexusUtils.identityFromCommandLine(args) : null);
        String advertiserIds =
                AppNexusUtils.commandLineGet("-advertiser", args);
        String[] selectedAdvertisers =
                (advertiserIds == null ? null : advertiserIds.split(","));
        String campaignIds = AppNexusUtils.commandLineGet("-campaign", args);
        Set<String> selectedCampaigns =
                (campaignIds == null
                  ? null
                  : new LinkedHashSet<String>
                            (Arrays.asList(campaignIds.split(","))));
        CampaignService campaign = null;
        if(selectedCampaigns != null && selectedAdvertisers != null &&
           ident != null)
        {
            Long advertiserId = new Long(selectedAdvertisers[0]);
            Long campaignId = new Long(campaignIds.split(",")[0]);
            campaign = AppNexusInterface.simpleFetchCampaign
                            (ident, advertiserId, campaignId,
                             AppNexusInterface.INTERVAL_VALUE_LIFETIME, null);
        }
        Bidder bidder = Bidder.initializeFromCommandLineArgs(false, args);
        Object[] res;
        res = CampaignData.splitCampaign
                (bidder, ident, campaign, numberOfKids, primaryPercentage,
                 ecpPercentage, totalDailyBudget, primaryBid,
                 maxBid, bidOffsetPercentage, verbose);
        if(verbose) info("Res: " + AppNexusUtils.commaSeparate(res));
        ecpPercentage = 10L;
        primaryPercentage = 90;
        res = CampaignData.splitCampaign
                (bidder, ident, campaign, numberOfKids, primaryPercentage,
                 ecpPercentage, totalDailyBudget, primaryBid,
                 maxBid, bidOffsetPercentage, verbose);
        if(verbose) info("Res: " + AppNexusUtils.commaSeparate(res));
        ecpPercentage = 10L;
        primaryPercentage = 80;
        for(int i = 1; i <= 10; i++)
        {
            numberOfKids = i;
            res = CampaignData.splitCampaign
                    (bidder, ident, campaign, numberOfKids, primaryPercentage,
                     ecpPercentage, totalDailyBudget, primaryBid, maxBid,
                     bidOffsetPercentage, verbose);
            if(verbose) info("Res: " + AppNexusUtils.commaSeparate(res));
        }
    }
}
