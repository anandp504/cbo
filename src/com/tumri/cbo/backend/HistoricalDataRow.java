package com.tumri.cbo.backend;
import com.tumri.mediabuying.appnexus.AppNexusUtils;
import com.tumri.mediabuying.zini.Utils;

import java.util.*;


/** Represents 1 time interval (hour) of impressions vs cost starting at the
 * specified date and time.  The date and time is in UTC.
 */
public class HistoricalDataRow implements BidResponse{
    Date dateTime;
    long impressions;
    double cost;
    double entropy;

    public String toString()
    {
        return "["+ AppNexusUtils.afterDot(this.getClass().getName()) + ": "
                  + dateTime + ", " + impressions + ", $" + cost +"]";
    }

    HistoricalDataRow (Date dateTime, long impressions, double cost, double entropy)
    {
        this.dateTime = dateTime;
        this.impressions = impressions;
        this.cost = cost;
        this.entropy = entropy;
    }
    
    public BidResponse cloneSelf()
    {
        return new HistoricalDataRow(dateTime, impressions, cost, entropy);
    }

    public Date getDate() {
    	return dateTime;
    }
    
    /** Gets the total number of impressions in the hour this represents.
     * @return The total number of impressions in the hour this represents.
     */
    public long getImpressionCount() {
    	return impressions;
    }
    
    public long getImpressionsServed() {
    	return impressions;
    }

    public Double getEntropy() {
        return entropy;
    }

    public void setEntropy(Double entropy) {
        this.entropy = entropy;
    }

    /** This is the total cost for all of the impressions.
     * @return The total cost for all of the impressions.
     */
    public double getCost() {
    	return cost;
    }
    
    public double getTotalCost() {
    	return cost;
    }

    public double getAverageCPM() {
    	return cost / (1000 * impressions);
    }

    /** Gets the cost per thousand impressions for this hour.
     * @return The cost per thousand impressions for this hour.
     */
    public double getCPM() {
    	return cost/1000.0;
    }
    
    public Long getImpressionTarget() {
        throw Utils.barf("Not meaningful for this class.", this);
    }

    public void setImpressionTarget(Long impressionTarget) {
        throw Utils.barf("Not meaningful for this class.", this);
    }

    public Long getImpressionBudget() {
        throw Utils.barf("Not meaningful for this class.", this);
    }

    public void setImpressionBudget(Long impressionBudget) {
        throw Utils.barf("Not meaningful for this class.", this);
    }
}
