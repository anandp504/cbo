package com.tumri.analytics.distributions;

/** Represents a binomial distribution of the actual success probability 
 * based on the observed number of trials and successes.
 * For example, if k clicks result from n impressions then this
 * is the distribution of the CTR.
 * <p>
 * The computations in this class can be slow if the n and or k is
 * large and the exact values are required.
 * Therefore, if k and (n - k) are greater than the approximation
 * threshold the results are approximated by a Gaussian distribution.
 * <p>
 * To normalize this when integrated over x the distribution
 * needs to be:
 * p(x) = [(n+1)*n!/(k!*(n-k)!)]*x^k(1-x)^{n-k}
 */
public class BinomialDistribution extends NormalizedDistribution {

	private int m_trialCount;
	private int m_successCount;
	
	private final static int APPROXIMATION_THRESHOLD = 5;
	private final static int MAX_MULTIPLIES = APPROXIMATION_THRESHOLD;				// Max number of multiplies
		
	/** Default constructor that specifies a 
	 */
	public BinomialDistribution() {
		this(0, 0);
	}
	
	/** Constructor that specifies the number of trials
	 * and number of successes.
	 * @param n The number of trials.
	 * @param k The number of successes.
	 * @exception IllegalArgumentException if k > n or n < 0 or k < 0.
	 */
	public BinomialDistribution(int n, int k) {
		setTrialsAndSuccesses(n, k);
	}
	
	/** Gets the density of the distribution at the indicated point.
	 * @param x The independent value.
	 * @return The density of the distribution at x.
	 */
	public double getDensityAt(double x) {
		double y = 1.0;
		int n = getTrialCount();
		int k = getSuccessCount();
		if(useGaussianApproximation(n, k)) {
			y = computeGaussianValue(x);
		} else if(n > 0) {
			double omx = 1.0 - x;
			if(k > 0) {
				y = Math.pow(x, (double)k)*Math.pow(omx, (double)(n - k));
			} else if (k == 0) {
				if(n < MAX_MULTIPLIES) {		// Compute it by multiplication.
					for(int i = 0; i < n; i++) {
						y *= omx;
					}
				} else {
					y = Math.pow(omx, (double)n);		// Use general method.
				}
			}
			y = y*computeNormalization(n, k);
		}
		return y;
	}
	
	/** Sets the number of trials.
	 * @param n The number of trials.
	 * @param k The number of successes.
	 * @exception IllegalArgumentException if k > n or n < 0 or k < 0.
	 */
	public void setTrialsAndSuccesses(int n, int k) {
		if(n < 0) {
			throw new IllegalArgumentException("Binomial distribution cannot have negative trial count: " + n);
		}
		if(k < 0) {
			throw new IllegalArgumentException("Binomial distribution cannot have negative success count: " + k);
		}
		if(k > n) {
			throw new IllegalArgumentException("Binomial distribution cannot have more successes than trials: n = " + n + ", k = " + k);
		}
		m_trialCount = n;
		m_successCount = k;
	}

	/** Gets the number of trials.
	 * @return The number of trials.
	 */
	public final int getTrialCount() {
		return m_trialCount;
	}

	/** Gets the number of successes.
	 * @return The number of successes.
	 */
	public final int getSuccessCount() {
		return m_successCount;
	}
	
	/** Gets the mean of this distribution over the x axis.
	 * If the number of trials is zero the mean is 1/2.
	 * @return The mean of this distribution.
	 */
	public final double getMean() {
		// This is for arbitrary n and k.
		return ((double)(getSuccessCount()+1))/((double)(getTrialCount()+2));
	}

	/** Gets the standard deviation.
	 * This is the square root of the variance.
	 * The Binomial distribution is not symmetric about the mean in general,
	 * but often the distribution can be approximated by a Gaussian distribution
	 * which is symmetric.
	 * @return The standard deviation.
	 */
	public final double getStandardDeviation() {
		return Math.sqrt(getVariance());
	}
	
	/** Gets the variance of this distribution.
	 * This is ave(y*y) - ave(y)*ave(y)
	 * @return The variance of this distribution.
	 */
	public final double getVariance() {
		// Based on the moment computation the variance should be:
		// var = (k+2)(k+1)/[(n+3)(n+2)] - [(k+1)/(n+2)]^2
		// var = [(k+1)/(n+2)] * [(k+2)/(n+3) - (k+1)/(n+2)]
		// var = [(k+1)/(n+2)] * [(nk+2n+2k+4) - (nk+3k+n+3)]/[(n+2)(n+3)]
		// var = [(k+1)/(n+2)] * [(n-k+1)/[(n+2)(n+3)]]
		double mean = getMean();
		double variance = computeMoment(getTrialCount(), getSuccessCount(), 2) - mean*mean;
		return variance;
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
	public double getUpperErrorBar() {
		double plusError = 0.0;
		int n = getTrialCount();
		int k = getSuccessCount();
		if(useGaussianApproximation(n, k)) {
			plusError = getMean() + computeGaussianStandardDeviation(n, k);
		} else {
			
			// TODO:
			
			// Calculate the upper error bar.
			// This might be faster to do with a table or some approximations
			// based on the values of k and n-k that do not defer to gaussians.
			
			
			
			
			
		}
		return plusError;
	}
	
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
	public double getLowerErrorBar() {
		double minusError = 0.0;
		int n = getTrialCount();
		int k = getSuccessCount();
		if(useGaussianApproximation(n, k)) {
			minusError = getMean() + computeGaussianStandardDeviation(n, k);
		} else {
			
			// TODO:
			
			
			
			
		}
		return minusError;
	}
	
	// --------------------------- Private methods ----------------

	/** Computes A == [(n+1)*n!/(k!*(n-k)!)] by successively
	 * evaluating terms [(n+1)*(n)*(n-1)*(n-2)...(n-k+1)] / [(k)*(k-1)*(k-2)...(1)].
	 * If k < APPROXIMATION_THRESHOLD there will be only
	 * a few terms in both the numerator and the denominator.
	 * If (n-k) < APPROXIMATION_THRESHOLD then there are only a few terms
	 * in the numerator that do not cancel with the terms in the denominator.
	 * Otherwise, the Gaussian approximation is used so this method should not
	 * be called.
	 * This method assumes k is small or (n-k) is small.
	 * @param n The number of trials.
	 * @param k The number of successes.
	 * @return The computed normalization constant.
	 * @Exception IllegalArgumentExceptoin If we should use the gaussian approximation.
	 */
	private double computeNormalization(int n, int k) {
		double norm = (double)(n + 1);
		if(k < APPROXIMATION_THRESHOLD) {
			for(int i = 0; i < k; i++) {
				norm = norm*((n - ((double)i))/((double)(k-i)));
			}
		} else if((n-k) < APPROXIMATION_THRESHOLD) {
			for(int i = 0; i < n - k; i++) {
				norm = norm*((double)(k+i+1));
			}
		} else {
			throw new IllegalArgumentException("Cannot compute normalization n=" + n + ", k=" + k);
		}
		return norm;
	}
	
	/** Computes the q-th moment of the distribution over x.
	 * Assumes that q is reasonably small.
	 * @param n The number of trials.
	 * @param k The number of successes.
	 * @param q The moment to compute.
	 */
	private double computeMoment(int n, int k, int q) {
		// The qth moment is (n+1) * [(k+q)!/k!] * [n!/(n+q+1)!]
		// This is (n+1) * [(k+q)(k+q-1)...(k+1)] / [(n+q+1)(n+q)(n+q-1)...(n+1)]
		// == [(k+q)(k+q-1)...(k+1)] / [(n+q+1)(n+q)(n+q-1)...(n+1+1)]
		
		double result = 1.0;
		for(int i = 1; i <= q; i++) {
			result = result * (((double)(k+i))/((double)(n+i+1)));
		}
		return result;
	}
	
	/** Computes the value of the distribution at the specified point
	 * using a Gaussian approximation.
	 * @param x The independent value.
	 * @return The distribution value at x.
	 */
	private double computeGaussianValue(double x) {
		return GaussianDistribution.computeGaussianValue(getMean(), getStandardDeviation(), x);
	}
	
	/** Computes the standard deviation of the Gaussian that
	 * most closely approximates this distribution.
	 * @param n The number of trials.
	 * @param k The number of successes.
	 * @return The gaussian standard deviation.
	 */
	private double computeGaussianStandardDeviation(int n, int k) {
		return getStandardDeviation();
	}

	/** Determines if this should be approximated by a Gaussian.
	 * @param n The number of trials.
	 * @param k The number of successes.
	 * @return True if this should be approximated by a Gaussian.
	 */
	private boolean useGaussianApproximation(int n, int k) {
		return (k > APPROXIMATION_THRESHOLD) && ((n - k) > APPROXIMATION_THRESHOLD);
	}
}
