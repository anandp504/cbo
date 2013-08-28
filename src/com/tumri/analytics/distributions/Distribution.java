package com.tumri.analytics.distributions;

/** This interface represents a 1-D distribution of real scalar values.
 * Note that it is not necessarily normalized.
 * @see NormalizedDistribution
 * @author jkucera
 *
 */
public interface Distribution {

	/** Gets the density of the distribution at the indicated point.
	 * @param x The independent value.
	 * @return The density of the distribution at x.
	 */
	public double getDensityAt(double x);
	
	/** Gets the mean of this distribution.
	 * @return The mean of this distribution.
	 */
	public double getMean();
	
	/** Gets the variance of this distribution.
	 * @return The variance of this distribution.
	 */
	public double getVariance();
	
	/** Gets the standard deviation of this distribution.
	 * This is just the square root of the variance.
	 * @return The standard deviation.
	 */
	public double getStandardDeviation(); 
	
	/** Gets the norm of this distribution.
	 * @return The norm of this distribution.
	 */
	public double getNorm();
	
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
	public double getUpperErrorBar();		
	
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
	public double getLowerErrorBar();
}
