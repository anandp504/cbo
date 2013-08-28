package com.tumri.analytics.distributions;

/** A distribution that is +infinity at the mean value and zero everywhere else.
 */
public final class DeltaFunctionDistribution extends NormalizedDistribution {

	private double m_mean;
	
	/** Constructor that takes a mean.
	 * @param mean The mean.
	 */
	public DeltaFunctionDistribution(double mean) {
		setMean(mean);
	}
	
	/** Gets the density of the distribution at the indicated point.
	 * If the point is the mean the density is positive infinity.
	 * @param x The independent value.
	 * @return The density of the distribution at x.
	 */
	public double getDensityAt(double x) {
		return (x == getMean()) ? Double.POSITIVE_INFINITY : 0.0;
	}
	
	/** Gets the mean of this distribution.
	 * The mean is the only value.
	 * @return The mean of this distribution.
	 */
	public double getMean() {
		return m_mean;
	}
	
	/** Gets the variance of this distribution.
	 * @return The variance of this distribution.
	 */
	public double getVariance() {
		return 0.0;
	}
	
	/** Gets the standard deviation of this distribution.
	 * This is just the square root of the variance.
	 * @return The standard deviation.
	 */
	public double getStandardDeviation() {
		return 0.0;
	}
	
	/** Gets the "1-sigma" upper bound for this distribution.
	 * This is just the mean for a delta-function distribution.
	 * @return The upper error bar.
	 */
	public double getUpperErrorBar() {
		return getMean();
	}
	
	/** Gets the "1-sigma" lower bound for this distribution.
	 * This is just the mean for a delta-function distribution.
	 * @return The lower error bar.
	 */
	public double getLowerErrorBar() {
		return getMean();
	}
	
	// --------------------------- Private methods -----------------------
	
	/** Sets the mean.
	 * @param mean The mean.
	 */
	private void setMean(double mean) {
		m_mean = mean;
	}
}
