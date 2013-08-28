package com.tumri.analytics.distributions;

/** A distribution whose norm is 1.0.
 * @author jkucera
 *
 */
public abstract class NormalizedDistribution implements Distribution {

	/** Gets the density of the distribution at the indicated point.
	 * @param x The independent value.
	 * @return The density of the distribution at x.
	 */
	public abstract double getDensityAt(double x);
	
	/** Gets the mean of this distribution.
	 * @return The mean of this distribution.
	 */
	public abstract double getMean();
	
	/** Gets the variance of this distribution.
	 * @return The variance of this distribution.
	 */
	public abstract double getVariance();
	
	/** Gets the standard deviation of this distribution.
	 * This is just the square root of the variance.
	 * @return The standard deviation.
	 */
	public abstract double getStandardDeviation(); 
	
	
	/** Gets the norm of this distribution.
	 * Always returns 1.0 since the distribution is normalized.
	 * @return The norm of this distribution.
	 */
	public final double getNorm() {
		return 1.0;
	}
	
	/** Gets the "1-sigma" upper bound for this distribution.
	 * This is the upper bound of the region that occupies
	 * the same area under the distribution that is occupied
	 * by the region within +/- 1.0 *sigma of the mean
	 * in a Gaussian distribution.
	 * This corresponds to a confidence of about 68.2%.
	 * This is a generalization of the error bar concept to
	 * this non-symmetric distribution.
	 * @return The upper error bar.
	 */
	public abstract double getUpperErrorBar();		
	
	/** Gets the "1-sigma" lower bound for this distribution.
	 * This is the lower bound of the region that occupies
	 * the same area under the distribution that is occupied
	 * by the region within +/- 1.0 *sigma of the mean
	 * in a Gaussian distribution.
	 * This corresponds to a confidence of about 68.2%.
	 * This is a generalization of the error bar concept to
	 * this non-symmetric distribution.
	 * @return The lower error bar.
	 */
	public abstract double getLowerErrorBar();
}
