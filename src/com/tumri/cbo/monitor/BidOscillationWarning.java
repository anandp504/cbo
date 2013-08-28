package com.tumri.cbo.monitor;

import com.tumri.cbo.backend.*;
import com.tumri.mediabuying.appnexus.AppNexusUtils;
import com.tumri.mediabuying.zini.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.log4j.Level;

public class BidOscillationWarning extends ImpressionsOscillationWarning {

    public BidOscillationWarning
            (ReportApplicabilityColumn... includedColumns)
    {
        super(includedColumns);
    }

    public AbstractProblem checkCampaign(Long advertiserId, CampaignData cd,
                                 SQLConnector connector, QueryContext qctx,
                                 SQLContext sctx, Date now, TimeZone localTz)
    {
        AbstractProblem res = null;
        TimeZone wrtTimeZone = cd.getTimeZone();
        Date bod = AppNexusUtils.dayFloor(now);
        Date earlierBod =
                AppNexusUtils.dayFloor(now, null, windowWidthInDays + 1);
        long uSecsOffset = uSecsOffsetFromLocal(localTz, wrtTimeZone);
        String obsT = "date_add(observation_time, INTERVAL " + uSecsOffset +
                      " MICROSECOND)";
        String query =
                "SELECT advertiser_id, campaign_id,\n" +
                "       date_format(" + obsT + ", '%Y-%m-%d'),\n" +
                "       average_base_bid\n" +
                "  FROM (SELECT advertiser_id, campaign_id, " +
                "               observation_time, avg(base_bid) as average_base_bid\n" +
                "          FROM observeddata t1\n" +
                "         WHERE t1.advertiser_id = " + advertiserId + "\n" +
                "           AND t1.campaign_id = "  + cd.getCampaignId() + "\n" +
                "           AND t1.base_bid > 0\n" +
                "           AND t1.observation_time <= '" +
                                connector.dateToSQL(bod) + "'\n" +
                "           AND t1.observation_time >= '" +
                                connector.dateToSQL(earlierBod) + "'\n" +
                "         GROUP BY advertiser_id, campaign_id, observation_time\n" +
                "         ORDER BY advertiser_id, campaign_id, observation_time\n" +
                "        ) as x";
        Sexpression rows = connector.runSQLQuery(query, qctx);
        int len = rows.length();
        if(len >= 3)
        {
        	try {
        		List<Double> bidList = new ArrayList<Double>(len);
        		for(Sexpression row : rows) {
        			bidList.add(row.fourth().unboxDouble());
        		}
        		int count = bidList.size();
        		double[] bids = new double[count];
        		for(int i = 0; i < count; i++) {
        			bids[i] = bidList.get(i);
        		}
        		for(int i = 0; i < bids.length - 2; i++)
        		{
        			if(res == null &&
        					exceedsGreatly(bids[i], bids[i + 1]) &&
        					exceedsGreatly(bids[i + 2], bids[i + 1]))
        				res = result("", cd, advertiserId);
        			else if(res == null &&
        					exceedsGreatly(bids[i + 1], bids[i]) &&
        					exceedsGreatly(bids[i + 1], bids[i + 2]))
        				res = result("", cd, advertiserId);
        			else {}
        		}
        	} catch(Exception e) {
        		// Just log the error if any.  Don't stop!
        		Utils.logThisPoint(Level.ERROR, e);
        	}
        }
        return res;
    }

    public String heading()
    {
        return "The following are showing day-by-day bid oscillations";
    }

    public String shortHeading()
    {
        return "Day-by-day bid oscillations.";
    }
}