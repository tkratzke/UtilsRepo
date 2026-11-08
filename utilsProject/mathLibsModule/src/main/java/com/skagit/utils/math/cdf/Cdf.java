package com.skagit.utils.math.cdf;

import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

import com.skagit.utils.MathX0;
import com.skagit.utils.NumericalRoutines;
import com.skagit.utils.TimeUtilities;
import com.skagit.utils.myLogger.MyLogger;

/**
 * This abstract class is for 2-dimensional random variables. "Cdf" is an
 * acronym for "Cumulative Distribution Function," and this class is based on
 * two Cdfs; the Cdf of X, and the Cdf of Y, given X. This base class handles
 * the means, and the extensions do their computations on the (zero-mean)
 * distribution of the offsets. The extensions have no access to the means
 * except for the setMeans functions, and they cannot override that or the
 * means' accessors. Hence, an implementation class' ctor should compute the
 * means from its input parameters, call setMeans, and then deal exclusively
 * with offsets.
 */
public abstract class Cdf {
	final public static int _QuietAboutIrregularity = 0;
	final public static int _LoudAboutIrregularity = 1;

	public static class BadCdfException extends Exception {
		protected static final long serialVersionUID = 0;

		public BadCdfException() {
		}
	}

	private double _meanX = Double.NaN, _meanY = Double.NaN;

	/**
	 * The way from a uniform random number generator to a random draw of (X,Y).
	 * Note that this relies heavily on the implementations and that they deal
	 * exclusively with offsets.
	 *
	 * @param cdfX       The CDF of X, that will be translated to a draw for X.
	 * @param cdfYGivenX The CDF of Y, given the draw for X above.
	 * @param result     If not null, the (X,Y) draw will be put there.
	 * @return The (X,Y) draw.
	 * @throws BadCdfException
	 * @see Cdf#xyToCdfs(double, double, double[])
	 */
	final public double[] cdfsToXy(final MyLogger logger, final double cdfX, final double cdfYGivenX, double[] result)
			throws Cdf.BadCdfException {
		result = cdfsToOffsets(logger, cdfX, cdfYGivenX, result);
		result[0] += _meanX;
		result[1] += _meanY;
		return result;
	}

	/**
	 * The way from an instance of this 2-d random variable to their Cdfs. Note that
	 * this relies heavily on the extensions' implementations and that they deal
	 * exclusively with offsets.
	 *
	 * @param x      The X part of an instance of the 2-d random variable.
	 * @param y      The Y part of an instance of the 2-d random variable.
	 * @param result If not null, the cdfs will be put there.
	 * @return [Cdf(x), Cdf(y|x)].
	 * @see Cdf#cdfsToXy(double, double, double[])
	 */
	final public double[] xyToCdfs(final double x, final double y, final double[] result) {
		final double xOffset = x - _meanX;
		final double yOffset = y - _meanY;
		return offsetsToCdfs(xOffset, yOffset, result);
	}

	final public double getMeanX() {
		return _meanX;
	}

	final public double getMeanY() {
		return _meanY;
	}

	protected final void setMeans(final double xMean, final double yMean) {
		_meanX = xMean;
		_meanY = yMean;
	}

	/**
	 * Defines a "central" portion of the distribution. For example, our
	 * implementation of a bivariate normal defines this as being within the 1-sigma
	 * ellipse.
	 *
	 * @param x Test value for x.
	 * @param y Test value for y.
	 * @return true iff (x,y) is within the central portion.
	 * @see #offsetsAreIn(double, double)
	 */
	final public boolean isIn(final double x, final double y) {
		return offsetsAreIn(x - _meanX, y - _meanY);
	}

	/**
	 * Given a path that starts at time 0.0 and ends at time 1.0, find the sequence
	 * of intervals within [0,1], that the path goes through the central portion.
	 *
	 * @param a0 [x,y] of the start of the path.
	 * @param a1 [x,y] of the end of the path.
	 * @return array of double[2]'s, where each double[2] is an interval.
	 * @see #isIn(double, double)
	 */
	final public double[][] getIntervalsOfIntersection(final double[] a0, final double[] a1) {
		final double[] times = getIntersectionTimes(a0, a1);
		final int nIntervals = times.length / 2;
		final double[][] intervals = new double[nIntervals][2];
		for (int i = 0; i < nIntervals; ++i) {
			intervals[i][0] = times[2 * i];
			intervals[i][1] = times[2 * i + 1];
		}
		return intervals;
	}

	final public double[] getIntersectionTimes(final double[] a0, final double[] a1) {
		final TreeSet<Double> times = new TreeSet<>();
		if (isIn(a0[0], a0[1])) {
			times.add(0d);
		}
		final double[] offsets0 = { a0[0] - _meanX, a0[1] - _meanY };
		final double[] offsets1 = { a1[0] - _meanX, a1[1] - _meanY };
		addInternalIntersectingTimesForOffsets(offsets0, offsets1, times);
		if (isIn(a1[0], a1[1])) {
			times.add(1d);
		}
		final double[] timesArray = new double[times.size()];
		int i = 0;
		for (final Double time : times) {
			timesArray[i++] = time;
		}
		return timesArray;
	}

	/**
	 * Produces a new Cdf that is a rotated version of itself. Note that if rads is
	 * 0.0, or 2Pi, this is an expensive clone. The new distribution is the rotated
	 * offset distribution (which has zero means) and the new mean is the rotated
	 * mean vector.
	 *
	 * @param rads rotation in radians, counterclockwise from the positive x
	 *             direction.
	 * @return The rotated Cdf.
	 * @see #rotateOffsetDistribution(double, double)
	 */
	final public Cdf rotate(final MyLogger logger, final double rads) {
		final double c = MathX0.cosX(rads);
		final double s = MathX0.sinX(rads);
		final Cdf rotatedCdf = rotateOffsetDistribution(logger, c, s);
		final double[] rotatedMeans = NumericalRoutines.rotateXy(c, s, _meanX, _meanY);
		rotatedCdf.setMeans(rotatedMeans[0], rotatedMeans[1]);
		return rotatedCdf;
	}

	/**
	 * Given a uniform random number generator, this produces a random draw from the
	 * 2-d Cdf.
	 *
	 * @param random A uniform random number generator.
	 * @param result if not null, the answer is put there.
	 * @return the random generates.
	 * @throws BadCdfException
	 */
	public double[] generateRandomPair(final MyLogger logger, final Random random, double[] result)
			throws BadCdfException {
		if (result == null) {
			result = new double[2];
		}
		final double cdfX = random.nextDouble(), cdfYGivenX = random.nextDouble();
		cdfsToOffsets(logger, cdfX, cdfYGivenX, result);
		result[0] += _meanX;
		result[1] += _meanY;
		return result;
	}

	protected void throwBadCdfException(final MyLogger logger, final int quietOrLoudAboutIrregularity)
			throws BadCdfException {
		final BadCdfException badCdfException = new BadCdfException();
		if (quietOrLoudAboutIrregularity == _LoudAboutIrregularity) {
			final long nowRefSecs = TimeUtilities.convertToRefSecs(System.currentTimeMillis() / 1000L);
			MyLogger.err(logger, String.format("\nProblem at %s with %s:\n", TimeUtilities.formatTime(nowRefSecs, true),
					bodyString()));
			badCdfException.printStackTrace();
		}
		throw badCdfException;
	}

	final public String getString() {
		final StringBuffer b = new StringBuffer("Means = ");
		b.append(NumericalRoutines.getString(new double[] { _meanX, _meanY }));
		final String bodyString = bodyString();
		b.append(bodyString);
		return new String(b);
	}

	/**
	 * Extensions must implement.
	 *
	 * @param cdfX       A Cdf of X.
	 * @param cdfYGivenX A Cdf of Y, given X.
	 * @param result     If not null, the answer is put there.
	 * @return The offsets. return[0] is the X-value corresponding to the input
	 *         CdfX, and return[1] is the Y value corresponding to Cdf(Y|return[0]).
	 * @throws BadCdfException
	 * @see #offsetsToCdfs(double, double, double[])
	 */
	abstract protected double[] cdfsToOffsets(MyLogger logger, double cdfX, double cdfYGivenX, double[] result)
			throws Cdf.BadCdfException;

	/**
	 * Extensions must implement.
	 *
	 * @param offsetX The offset of X from the mean of X.
	 * @param offsetY The offset of Y from the mean of Y
	 * @param result  If not null, the answer is put there.
	 * @return [Cdf(X), Cdf(offsetY|offsetX).
	 * @see #cdfsToOffsets(double, double, double[])
	 */
	abstract protected double[] offsetsToCdfs(double offsetX, double offsetY, double[] result);

	/**
	 * Extensions must implement.
	 *
	 * @param offsetX The offset of X from the mean of X.
	 * @param offsetY The offset of Y from the mean of Y
	 * @return true iff (offsetX,offsetY) is in the central portion of the Cdf of
	 *         the offsets.
	 * @see #isIn(double, double)
	 */
	abstract protected boolean offsetsAreIn(double offsetX, double offsetY);

	abstract protected void addInternalIntersectingTimesForOffsets(double[] offset0, double[] offset1,
			SortedSet<Double> times);

	/**
	 * Extensions must implement.
	 *
	 * @param cos ... of the angle to rotate the distribution by.
	 * @param sin ... of the angle to rotate the distribution by.
	 * @return The new Cdf that has been rotated. Still has zero means.
	 * @see #rotate(double)
	 */
	abstract protected Cdf rotateOffsetDistribution(MyLogger logger, double cos, double sin);

	abstract protected String bodyString();
}
