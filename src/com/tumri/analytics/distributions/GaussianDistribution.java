package com.tumri.analytics.distributions;

/** Represents a gaussian distribution with a specified mean and 
 * standard deviation.  If the standard deviation is set to zero
 * this represents a "delta" function centered at the mean and the
 * value will be Double.POSITIVE_INFINITY at the mean and zero everywhere else.
 * @author jkucera
 */
public class GaussianDistribution extends NormalizedDistribution {

	private double m_mean;
	private double m_stdDev;
	
	private final static double ONE_OVER_SQRT_2PI = 1.0/Math.sqrt(2.0*Math.PI);
	
	/** Default constructor that specifies a delta function centered at 0.0.
	 */
	public GaussianDistribution() {
		this(0.0, 0.0);
	}
	
	/** Constructor that specifies a delta function at the mean
	 * with a standard deviation of 0.0
	 * @param mean The mean.
	 */
	public GaussianDistribution(double mean) {
		this(mean, 0.0);
	}
	
	/** Constructor that specifies a mean and standard deviation.
	 * @param mean The mean.
	 * @param stdDev The standard deviation.
	 */
	public GaussianDistribution(double mean, double stdDev) {
		setMean(mean);
		setStandardDeviation(stdDev);
	}
	
	/** Gets the density of the distribution at the indicated point.
	 * @param x The independent value.
	 * @return The density of the distribution at x.
	 */
	public double getDensityAt(double x) {
		return computeGaussianValue(getMean(), getStandardDeviation(), x);
	}
	
	/** Sets the mean of this distribution.
	 * @param mean The mean of this distribution.
	 */
	public void setMean(double mean) {
		m_mean = mean;
	}

	/** Gets the mean of this distribution.
	 * @return The mean of this distribution.
	 */
	public final double getMean() {
		return m_mean;
	}
	
	/** Sets the standard deviation of this distribution.
	 * The standard deviation must always be >= 0.0
	 * @param stdDev The standard deviation of this distribution.
	 * @exception IllegalArgumentException If the standard deviation is < 0.0
	 */
	public void setStandardDeviation(double stdDev) {
		if(stdDev < 0.0) {
			throw new IllegalArgumentException("Cannot have negative standard deviation: " + stdDev);
		}
		m_stdDev = stdDev;
	}

	/** Gets the standard deviation of this distribution.
	 * @return The standard deviation of this distribution.
	 */
	public final double getStandardDeviation() {
		return m_stdDev;
	}
	
	/** Gets the variance of this distribution.
	 * For a Gaussian this is the square of the standard deviation.
	 * @return The variance of this distribution.
	 */
	public final double getVariance() {
		double sigma = getStandardDeviation();
		return sigma*sigma;
	}
	
	/** Gets the "1-sigma" upper bound for this distribution.
	 * This corresponds to a confidence of about 68.2%.
	 * This is a generalization of the error bar concept to
	 * handle non-symmetric distributions.
	 * @return The upper error bar.
	 */
	public double getUpperErrorBar() {
		return getMean() + getStandardDeviation();
	}
	
	/** Gets the "1-sigma" lower bound for this distribution.
	 * This corresponds to a confidence of about 68.2%.
	 * This is a generalization of the error bar concept to
	 * handle non-symmetric distributions.
	 * @return The lower error bar.
	 */
	public double getLowerErrorBar() {
		return getMean() - getStandardDeviation();
	}
	
	/** This returns true if this is a "delta" function where 
	 * the standard deviation is 0.0.
	 * @return True if this is a delta function or false if not.
	 */
	public final boolean isDeltaFunction() {
		return getStandardDeviation() == 0.0;
	}
	
	/*
	// Confidence intervals == percent that the value is within n sigma:
	// from Wikipedia
	
	1	0.682689492137	0.317310507863	1.0/3.15148718753
	2	0.954499736104	0.045500263896	1.0/21.9778945080
	3	0.997300203937	0.002699796063	10./370.398347345
	4	0.999936657516	0.000063342484	1.0/15,787.1927673
	5	0.999999426697	0.000000573303	1.0/1,744,277.89362
	6	0.999999998027	0.000000001973	1.0/506,797,345.897
	*/
	
	/** Sigma required for specific confidence interval.
	0.80	1.281551565545		0.999	3.290526731492
	0.90	1.644853626951		0.9999	3.890591886413
	0.95	1.959963984540		0.99999	4.417173413469
	0.98	2.326347874041		0.999999	4.891638475699
	0.99	2.575829303549		0.9999999	5.326723886384
	0.995	2.807033768344		0.99999999	5.730728868236
	0.998	3.090232306168		0.999999999	6.109410204869
	*/
	
	// ------------------- Package private methods -----------
	
	/** Computes the value of the Gaussian distribution with the indicated
	 * mean and standard deviation at the indicated point.
	 * @param mean The mean.
	 * @param stdDev The standard deviation.
	 * @param x The independent value.
	 * @return The distribution value at x.
	 */
	static double computeGaussianValue(double mean, double stdDev, double x) {
		double y = 0.0;
		if(stdDev > 0.0) {
			y = ONE_OVER_SQRT_2PI*Math.exp(-((x-mean)*(x-mean))/(2.0*stdDev*stdDev))/stdDev;
		} else if(stdDev == 0.0) {
			if(x == mean) {
				y = Double.POSITIVE_INFINITY;
			}
		}
		return y;
	}
}
