package com.tumri.cbo.backend;

public class TrivialBidResponse implements BidResponse {

	long impressions;
	double cost;
    Double entropy;
    Long impressionBudget;
    Long impressionTarget;

	public TrivialBidResponse(long impressions, double cost, Double entropy,
                              Long impressionBudget, Long impressionTarget)
	{
		this.impressions = impressions;
		this.cost = cost;
        this.entropy = entropy;
        this.impressionBudget = impressionBudget;
        this.impressionTarget = impressionTarget;
	}

    public BidResponse cloneSelf()
    {
        return new TrivialBidResponse(impressions, cost, entropy,
                                      impressionBudget, impressionTarget);
    }

	public long getImpressionsServed() {
		return impressions;
	}

	public double getTotalCost() {
		return cost;
	}

	public Double getEntropy() {
		return entropy;
	}

	public void setEntropy(Double entropy) {
		this.entropy = entropy;
	}

	public void addImpressionsServed(long i) {
		impressions = impressions + i;
	}

	public void addTotalCost(double c) {
		cost = cost + c;
	}

	public double getAverageCPM() {
		if(cost < 0 || impressions <= 0) return 0.0d;
		else return (cost * 1000.0d) / impressions;
	}

    public Long getImpressionTarget() {
        return impressionTarget;
    }

    public void setImpressionTarget(Long impressionTarget) {
        this.impressionTarget = impressionTarget;
    }

    public Long getImpressionBudget() {
        return impressionBudget;
    }

    public void setImpressionBudget(Long impressionBudget) {
        this.impressionBudget = impressionBudget;
    }

}

