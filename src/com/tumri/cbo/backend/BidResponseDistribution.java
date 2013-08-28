package com.tumri.cbo.backend;

import com.tumri.analytics.distributions.NormalizedDistribution;

/** Represents the distributions of the bid response metrics.
 */
public class BidResponseDistribution {
	
	private NormalizedDistribution m_impressionsDistribution;
	private NormalizedDistribution m_cpmDistribution;
	private NormalizedDistribution m_entropyDistribution;

	/** Constructor that takes the individual distributions.
	 * Does not copy the distributions but references them.
	 * @param impsDist The impressions distribution.
	 * @param cpmDist The CPM distribution.
	 * @param entropyDist The entropy distribution.
	 */
	public BidResponseDistribution(NormalizedDistribution impsDist,
                                   NormalizedDistribution cpmDist,
                                   NormalizedDistribution entropyDist) {
		m_impressionsDistribution = impsDist;
		m_cpmDistribution = cpmDist;
        m_entropyDistribution = entropyDist;
	}

    /** Gets the distribution of the total number of impressions 
     * served over the time period this represents.
     * @return The distribution of the total number of impressions served over the time period this represents.
     */
    public final NormalizedDistribution getImpressionsDistribution() {
    	return m_impressionsDistribution;
    }
    
    /** Gets the distribution of the average CPM paid for serving impressions over the time period this represents.
     * @return The distribution of the average CPM paid for serving impressions over the time period this represents.
     */
    public final NormalizedDistribution getCPMDistribution() {
    	return m_cpmDistribution;
    }
    
    /** Gets the distribution of the site distribution entropies over the time period this represents.
     * @return The distribution of the site distribution entropties over the time period this represents.
     */
    public final NormalizedDistribution getEntropyDistribution() {
    	return m_entropyDistribution;
    }

    /** Gets the expected value of the distribution as a bid response.
     * Returns a new distribution based on the current values in the base distributions
     * each time this is called.
     * @return The expected value of the distribution.
     */
    public BidResponse getExpectedResponse() {
    	
    	double meanImps = 0.0;
    	double meanCPM = 0.0;
    	Double meanEntropy = null;

    	NormalizedDistribution impsDist = getImpressionsDistribution();
    	if(impsDist != null) {
    		meanImps = impsDist.getMean();
    	}
    	NormalizedDistribution cpmDist = getCPMDistribution();
    	if(cpmDist != null) {
    		meanCPM = cpmDist.getMean();
    	}
    	
    	NormalizedDistribution entropyDist = getEntropyDistribution();
    	if(entropyDist != null) {
    		meanEntropy = entropyDist.getMean();
    	}

    	return new TrivialBidResponse
                      (Math.round(meanImps), meanCPM, meanEntropy, null, null);
    }
}
