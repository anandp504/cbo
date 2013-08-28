package com.tumri.cbo.backend;

@SuppressWarnings("unused")
public class DashboardInstruction extends BidderInstruction {
    public Long lifetimeProjectedImpressions;
    public Double lifetimePacingPercentage;
    public Long lifetimePacingLookback;
    public Double yesterdayEntropy;
    public Double currentOrMaxBid;

    public DashboardInstruction() // So that we can call newInstance.
    {
        super();
    }
    
}
