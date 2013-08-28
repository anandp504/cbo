package com.tumri.cbo.backend;

/** Represents the set of campaign metrics measured over some time period.
 */
public interface BidResponse {

    /** Gets the total number of impressions served over the time period this represents.
     * @return The total number of impressions served over the time period this represents.
     */
    public long getImpressionsServed();
    
    /** Gets the site distribution entropy over the time period this represents.
     * @return The site distribution entropy over the time period this represents.
     */
    public Double getEntropy();
    
    /** Sets the site distribution entropy over the time period this represents.
     * @param entropy The site distribution entropy over the time period this represents.
     */
    public void setEntropy(Double entropy);

    /** Gets the daily impression target over the time period this represents.
     * @return The daily impressions target over the time period this represents.
     */
    public Long getImpressionTarget();

    /** Sets the daily impression target over the time period this represents.
     * @param imp The daily impression target over the time period this represents.
     */
    public void setImpressionTarget(Long imp);

    /** Gets the daily impression budget over the time period this represents.
     * @return The daily impressions budget over the time period this represents.
     */
    public Long getImpressionBudget();

    /** Sets the daily impression budget over the time period this represents.
     * @param imp The daily impression budget over the time period this represents.
     */
    public void setImpressionBudget(Long imp);

    /** Gets the total price paid for serving impressions over the time period this represents.
     * @return The total price paid for serving impressions over the time period this represents.
     */
    public double getTotalCost();

    /** Gets the average price paid per thousand impressions for the time period this represents.
     * @return The average price paid per thousand impressions for the time period this represents.
     */
    public double getAverageCPM();

    /** Copies the response
     * @return The new copy.
     */
    public BidResponse cloneSelf();
}
