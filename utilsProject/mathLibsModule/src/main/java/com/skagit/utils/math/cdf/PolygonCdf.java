package com.skagit.utils.math.cdf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.skagit.utils.CombinatoricTools;
import com.skagit.utils.NumericalRoutines;
import com.skagit.utils.myLogger.MyLogger;

/**
 * A uniform density 2d Cdf over a polygon that is defined by a sequence of
 * double[]'s.
 */
public class PolygonCdf extends Cdf {
	final private Endpoint[] _endpointsLeftFirst;
	final private Endpoint[] _endpointsInInputOrder;
	/**
	 * Convention: i indexes _segments, and j indexes _endpoints. They must have the
	 * same size, which is always referred to as n.
	 */
	final private Segment[] _segments;
	final private TreeMap<Double, double[]> _verticalStretches;

	/** @see #PolygonCdf(double[][], boolean, int) */
	public PolygonCdf(final MyLogger logger, final double[][] points, final int quietOrLoudAboutIrregularity)
			throws BadCdfException {
		this(logger, points, false, quietOrLoudAboutIrregularity);
	}

	/**
	 * The hardest code to follow. We quickly create Segments and Endpoints. Then we
	 * have to create each Endpoint's Straddlers, and compute the partial sums that
	 * allow us to compute Cdf and CdfInverse.
	 *
	 * @param points          The polygon of points. Each point is a double[]. The
	 *                        last point is not repeated. No check is made for
	 *                        crossing, but the program will crash if there are
	 *                        crossings.
	 * @param transposePoints True if we are to transpose the input points. If we do
	 *                        transpose points, we don't compute a transpose after
	 *                        we compute our own PolygonCdf. If we're not
	 *                        transposing points, we create another PolygonCdf with
	 *                        transposed points, read its meanX, and store that as
	 *                        our meanY.
	 */
	@SuppressWarnings("unchecked")
	private PolygonCdf(final MyLogger logger, final double[][] inputPoints, final boolean transposePoints,
			final int quietOrLoudAboutIrregularity) throws BadCdfException {
		final int nInput = inputPoints == null ? 0 : inputPoints.length;
		/** Put in only distinct ones. */
		final ArrayList<double[]> pointList = new ArrayList<>(nInput);
		final TreeSet<double[]> pointSet = new TreeSet<>(NumericalRoutines._ByFirstAndSecondOnly);
		for (int iPoint = 0; iPoint < nInput; ++iPoint) {
			double[] point = inputPoints[iPoint];
			if (transposePoints) {
				final double x = point[0];
				final double y = point[1];
				point = new double[] { y, x };
			}
			if (pointSet.add(point)) {
				pointList.add(point);
			} else if (iPoint < nInput - 1) {
				/** Unless this is the last one, this is an error. */
				throwBadCdfException(logger, quietOrLoudAboutIrregularity);
			}
		}
		final int n = pointList.size();
		if (n < 3) {
			throwBadCdfException(logger, quietOrLoudAboutIrregularity);
		}
		/** pointArrays is the order in which they were given to us. */
		final double[][] pointArrays = pointList.toArray(new double[n][]);
		_endpointsInInputOrder = new Endpoint[n];
		_segments = new Segment[n];
		for (int j = 0; j < n; ++j) {
			final double x0 = pointArrays[j][0];
			final double y0 = pointArrays[j][1];
			final Endpoint endpointJ;
			if (j == 0) {
				endpointJ = new Endpoint(x0, y0);
				_endpointsInInputOrder[0] = endpointJ;
			} else {
				endpointJ = _endpointsInInputOrder[j];
			}
			Endpoint endpointJPlus1;
			if (j == n - 1) {
				endpointJPlus1 = _endpointsInInputOrder[0];
			} else {
				final int jj = j + 1;
				final double[] xy1 = pointArrays[jj];
				final double x1 = xy1[0];
				final double y1 = xy1[1];
				endpointJPlus1 = new Endpoint(x1, y1);
				_endpointsInInputOrder[jj] = endpointJPlus1;
			}
			_segments[j] = new Segment(endpointJ, endpointJPlus1);
		}
		_endpointsLeftFirst = _endpointsInInputOrder.clone();
		Arrays.sort(_endpointsLeftFirst);
		Arrays.sort(_segments);
		/**
		 * For each segment, find the endpoints that it straddles. Segment segment
		 * straddles x if segment._left._x <= x < segment._right._x.<br>
		 * <br>
		 * Start by finding all of the endpoints' x-coordintates.
		 */
		final ArrayList<Double> xCoordsSet = new ArrayList<>();
		for (final Endpoint endpoint : _endpointsLeftFirst) {
			xCoordsSet.add(endpoint._x);
		}
		/**
		 * Make them into a sorted array, and make a parallel array of sets of
		 * Straddling Edges.
		 */
		final int nDoubles = xCoordsSet.size();
		final double[] xCoords = new double[nDoubles];
		final ArrayList<Segment>[] straddlerLists = new ArrayList[nDoubles];
		int jDouble = 0;
		for (final Double d : xCoordsSet) {
			xCoords[jDouble] = d;
			straddlerLists[jDouble++] = new ArrayList<>();
		}
		Arrays.sort(xCoords);
		/** For each segment, find the xCoords that it straddles. */
		for (final Segment segment : _segments) {
			if (!segment.isVertical()) {
				jDouble = Arrays.binarySearch(xCoords, segment._left._x);
				while (jDouble < nDoubles && xCoords[jDouble] < segment._right._x) {
					straddlerLists[jDouble++].add(segment);
				}
			}
		}
		/**
		 * For each xCoord, change his straddlerList to an array and sort it.
		 */
		final Segment[][] straddlerArrays = new Segment[nDoubles][];
		for (jDouble = 0; jDouble < nDoubles; ++jDouble) {
			final ArrayList<Segment> straddlerList = straddlerLists[jDouble];
			straddlerArrays[jDouble] = straddlerList.toArray(new Segment[straddlerList.size()]);
			final double xCoord = xCoords[jDouble];
			Arrays.sort(straddlerArrays[jDouble], new Comparator<Segment>() {
				@Override
				public int compare(final Segment segment0, final Segment segment1) {
					final double compareValue = segment0.yFor(xCoord) - segment1.yFor(xCoord);
					if (compareValue < 0d) {
						return -1;
					}
					if (compareValue > 0d) {
						return 1;
					}
					return segment0.compareSlope(segment1);
				}
			});
		}
		/** Assign the sorted arrays of straddlers to each Endpoint. */
		for (final Endpoint endpoint : _endpointsLeftFirst) {
			jDouble = Arrays.binarySearch(xCoords, endpoint._x);
			endpoint._straddlers = straddlerArrays[jDouble];
		}
		/**
		 * Compute cumY and cumSlope by considering the straddling segments. The bottom
		 * segment starts an interval and its slope is subtracted from the cumSlope. The
		 * next one stops the interval so that cumY now is incremented by the second's
		 * y-value - the first's y-value. We also accumulate xMean here as well.
		 */
		double xMean = 0d;
		for (int j = 0; j < n; ++j) {
			final Endpoint endpoint = _endpointsLeftFirst[j];
			final double endX = endpoint._x;
			/** Set _areaToLeft and update the xMean calculations. */
			if (j == 0) {
				endpoint._areaToLeft = endpoint._cumY = endpoint._cumSlope = 0d;
			} else {
				final Endpoint oldEndpoint = _endpointsLeftFirst[j - 1];
				final double oldX = oldEndpoint._x;
				final double deltaX = endX - oldX;
				final double oldCumY = oldEndpoint._cumY;
				final double oldCumSlope = oldEndpoint._cumSlope;
				final double deltaY = deltaX * oldCumSlope;
				final double deltaArea1 = deltaX * oldCumY;
				final double deltaArea2 = deltaX * deltaY / 2d;
				final double deltaArea = deltaArea1 + deltaArea2;
				endpoint._areaToLeft = oldEndpoint._areaToLeft + deltaArea;
				/**
				 * <pre>
				 * Adjust the running total for xMean, which is Integral[x*f(x)]. Note that f(x)
				 * = oldCumY + m * (x-oldX), so x*f(x) = x*OldCumY + m * (xSq-x*oldX); The
				 * limits for the definite integral are oldX and endX.
				 */
				final double endXSq = endX * endX;
				final double oldXSq = oldX * oldX;
				final double oldXCb = oldX * oldXSq;
				final double integral1 = oldCumY * (endXSq - oldXSq) / 2d;
				final double mPart1 = endXSq * (endX / 3d - oldX / 2d);
				final double mPart2 = oldXCb / 6d;
				final double integral2 = oldCumSlope * (mPart1 + mPart2);
				xMean += integral1 + integral2;
			}
			final Segment[] straddlers = endpoint._straddlers;
			final int nStraddlers = straddlers.length;
			endpoint._cumSlope = endpoint._cumY = 0d;
			for (int k = 0; k < nStraddlers / 2; ++k) {
				final Segment lowStraddler = straddlers[2 * k];
				final Segment highStraddler = straddlers[2 * k + 1];
				endpoint._cumY += highStraddler.yFor(endX) - lowStraddler.yFor(endX);
				endpoint._cumSlope += highStraddler.getSlope() - lowStraddler.getSlope();
			}
		}
		xMean /= getTotalArea();
		validateSegments(logger, quietOrLoudAboutIrregularity);
		/**
		 * If we transposed points, set the xMean; since that's all we're interested in
		 * when we transpose points, return after the xMean is set.
		 */
		if (transposePoints) {
			setMeans(xMean, Double.NaN);
			_verticalStretches = null;
			return;
		}
		final boolean transposePoints2 = true;
		final double yMean = new PolygonCdf(logger, pointArrays, transposePoints2, quietOrLoudAboutIrregularity)
				.getMeanX();
		setMeans(xMean, yMean);
		/**
		 * Subtract off the means from the endpoints; they'll be in super's _xMean and
		 * _yMean. Endpoints now represent "offsets from mean."
		 */
		for (int j = 0; j < n; ++j) {
			final Endpoint endpoint = _endpointsLeftFirst[j];
			endpoint._x -= xMean;
			endpoint._y -= yMean;
		}
		/**
		 * We need information about vertical stretches for the "isIn" call. Create that
		 * now.
		 */
		_verticalStretches = new TreeMap<>();
		for (final Segment segment : _segments) {
			if (segment.isVertical()) {
				double[] stretch = { segment._left._y, segment._right._y };
				final double[] oldStretch = _verticalStretches.put(segment._left._x, stretch);
				if (oldStretch != null) {
					stretch = new double[oldStretch.length + 2];
					int i = 0;
					for (final double y : oldStretch) {
						stretch[i++] = y;
					}
					stretch[i++] = segment._left._y;
					stretch[i++] = segment._right._y;
					Arrays.sort(stretch);
					_verticalStretches.put(segment._left._x, stretch);
				}
			}
		}
	}

	/**
	 * Right now this gets the correct left and right endpoints, but it only returns
	 * one region, specified by the min and max y values between the left and right
	 * cums. In theory, there could be several regions between leftCum and rightCum.
	 * We are using this (as of Jan 14, 2019) for convex PolygonCdfs, so this is
	 * acceptable.
	 */
	public double[][][] getRegions(final MyLogger logger, final double leftCum, final double rightCum) {
		double[] chunkLeftLow = null;
		double[] chunkRightLow = null;
		double[] chunkRightHigh = null;
		double[] chunkLeftHigh = null;
		try {
			final double[] leftLow = cdfsToXy(logger, leftCum, 0d, null);
			final double[] leftHigh = cdfsToXy(logger, leftCum, 1d, null);
			final double[] rightLow = cdfsToXy(logger, rightCum, 0d, null);
			final double[] rightHigh = cdfsToXy(logger, rightCum, 1d, null);
			final double left = Math.min(leftLow[0], leftHigh[0]);
			final double right = Math.max(rightLow[0], rightHigh[0]);
			double yMax = Math.max(leftHigh[1], rightHigh[1]);
			double yMin = Math.max(leftLow[1], rightLow[1]);
			final double meanY = getMeanY();
			final double totalArea = getTotalArea();
			for (final Endpoint endpoint : _endpointsLeftFirst) {
				final double cumToLeft = endpoint._areaToLeft / totalArea;
				if (cumToLeft < leftCum) {
					continue;
				}
				if (cumToLeft <= rightCum) {
					final double y = endpoint._y + meanY;
					yMax = Math.max(yMax, y);
					yMin = Math.min(yMin, y);
					continue;
				}
				break;
			}
			chunkLeftLow = new double[] { left, yMin };
			chunkRightLow = new double[] { right, yMin };
			chunkRightHigh = new double[] { right, yMax };
			chunkLeftHigh = new double[] { left, yMax };
		} catch (final BadCdfException e) {
			e.printStackTrace();
		}
		return new double[][][] { new double[][] { chunkLeftLow, chunkRightLow, chunkRightHigh, chunkLeftHigh } };
	}

	@Override
	protected double[] cdfsToOffsets(final MyLogger logger, double cdfX, final double cdfYGivenX, double[] result)
			throws Cdf.BadCdfException {
		try {
			if (result == null) {
				result = new double[2];
			}
			/**
			 * Binary search for the biggest endpoint with areaToLeft not too big.
			 */
			if (cdfX <= 0d) {
				cdfX = 0d;
			}
			if (cdfX >= 1d) {
				cdfX = 1d;
			}
			final double targetArea = cdfX * getTotalArea();
			/**
			 * To find the correctX, note that it is between two xCoords; the two whose
			 * areaToLeft sandwiches targetArea.
			 */
			final Endpoint lookUp = new Endpoint(0d, 0d, targetArea, Double.NaN, Double.NaN);
			final int glbIndex = CombinatoricTools.getGlbIndex(_endpointsLeftFirst, lookUp, new Comparator<Endpoint>() {
				@Override
				public int compare(final Endpoint endpoint0, final Endpoint endpoint1) {
					if (endpoint0._areaToLeft < endpoint1._areaToLeft) {
						return -1;
					}
					if (endpoint0._areaToLeft > endpoint1._areaToLeft) {
						return 1;
					}
					return 0;
				}
			});
			final Endpoint glbEndpoint = _endpointsLeftFirst[glbIndex];
			final double glbEndpointX = glbEndpoint._x;
			final double offsetX;
			if (glbIndex == _endpointsLeftFirst.length - 1) {
				offsetX = glbEndpointX;
			} else {
				final double maxOffset = _endpointsLeftFirst[glbIndex + 1]._x - glbEndpointX;
				/**
				 * The following quadratic's solution between 0 and maxOffset is where to find
				 * the correct xCoord.
				 */
				final double[] roots = NumericalRoutines.quadratic(glbEndpoint._cumSlope / 2d, glbEndpoint._cumY,
						glbEndpoint._areaToLeft - targetArea, null);
				if (roots[0] >= 0d && roots[0] <= maxOffset) {
					offsetX = glbEndpointX + roots[0];
				} else if (roots[1] >= 0d && roots[1] <= maxOffset) {
					offsetX = glbEndpointX + roots[1];
				} else {
					offsetX = Double.NaN;
					throwBadCdfException(logger, _LoudAboutIrregularity);
				}
			}
			result[0] = offsetX;
			/**
			 * We have offsetX. We need offsetY. Consider glbEndpoint's straddlers; they are
			 * exactly the endpoints that cover x.
			 */
			final Segment[] straddlers = glbEndpoint._straddlers;
			final int nStraddlers = straddlers.length;
			final int nIntervals = nStraddlers / 2;
			if (nIntervals == 0) {
				result[1] = glbEndpoint._y;
			} else {
				final double[][] intervals = new double[nIntervals][2];
				double totalLength = 0d;
				for (int intervalNumber = 0; intervalNumber < nIntervals; ++intervalNumber) {
					final Segment straddlerLow = straddlers[2 * intervalNumber];
					final Segment straddlerHigh = straddlers[(2 * intervalNumber) + 1];
					final double[] thisInterval = intervals[intervalNumber];
					thisInterval[0] = straddlerLow.yFor(offsetX);
					thisInterval[1] = straddlerHigh.yFor(offsetX);
					totalLength += (thisInterval[1] - thisInterval[0]);
				}
				final double targetLength = cdfYGivenX * totalLength;
				double runningLength = 0d;
				double offsetY = Double.NaN;
				for (int intervalNumber = 0; intervalNumber < nIntervals; ++intervalNumber) {
					final double[] thisInterval = intervals[intervalNumber];
					final double thisLength = thisInterval[1] - thisInterval[0];
					final double oldRunningLength = runningLength;
					runningLength += thisLength;
					if (runningLength >= targetLength) {
						offsetY = thisInterval[0] + (targetLength - oldRunningLength);
						result[1] = offsetY;
						break;
					}
				}
			}
			return result;
		} catch (final ArrayIndexOutOfBoundsException e) {
			throwBadCdfException(logger, Cdf._LoudAboutIrregularity);
		}
		return null;
	}

	final private static Comparator<Endpoint> _ComparatorForX = new Comparator<>() {
		@Override
		public int compare(final Endpoint endpoint0, final Endpoint endpoint1) {
			if (endpoint0._x < endpoint1._x) {
				return -1;
			}
			if (endpoint0._x > endpoint1._x) {
				return 1;
			}
			return 0;
		}
	};

	private Endpoint getGlbWrtX(final double offsetX) {
		if (_endpointsLeftFirst == null || _endpointsLeftFirst.length == 0 || offsetX < _endpointsLeftFirst[0]._x) {
			return null;
		}
		/** Binary search for the biggest endpoint with x at most offsetX. */
		final Endpoint lookUp = new Endpoint(offsetX, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
		final int glbIndex = CombinatoricTools.getGlbIndex(_endpointsLeftFirst, lookUp, _ComparatorForX);
		return _endpointsLeftFirst[glbIndex];
	}

	@Override
	protected double[] offsetsToCdfs(final double inputOffsetX, final double offsetY, double[] result) {
		if (result == null) {
			result = new double[2];
		}
		final int n = _endpointsLeftFirst.length;
		final double firstX = _endpointsLeftFirst[0]._x;
		final double lastX = _endpointsLeftFirst[n - 1]._x;
		final double offsetX;
		if (inputOffsetX < firstX) {
			offsetX = firstX;
		} else if (inputOffsetX > lastX) {
			offsetX = lastX;
		} else {
			offsetX = inputOffsetX;
		}
		final Endpoint glbEndpoint = getGlbWrtX(offsetX);
		final double glbEndpointX = glbEndpoint._x;
		final double deltaX = offsetX - glbEndpointX;
		final double cumSlope = glbEndpoint._cumSlope;
		final double cumY = glbEndpoint._cumY;
		final double totalArea = getTotalArea();
		double areaToLeft = glbEndpoint._areaToLeft;
		areaToLeft += (deltaX * deltaX * cumSlope) / 2d;
		areaToLeft += deltaX * cumY;
		final double cdfX = areaToLeft / totalArea;
		result[0] = cdfX;
		/**
		 * We have cdfX. We need cdfYGivenX. Consider glbEndpoint's straddlers; they are
		 * exactly the endpoints that cover x.
		 */
		final Segment[] straddlers = glbEndpoint._straddlers;
		final int nStraddlers = straddlers.length;
		final int nIntervals = nStraddlers / 2;
		final double[][] intervals = new double[nIntervals][2];
		double totalLength = 0d;
		for (int intervalNumber = 0; intervalNumber < nIntervals; ++intervalNumber) {
			final double[] interval = intervals[intervalNumber];
			final Segment straddlerLow = straddlers[2 * intervalNumber];
			final Segment straddlerHigh = straddlers[(2 * intervalNumber) + 1];
			interval[0] = straddlerLow.yFor(offsetX);
			interval[1] = straddlerHigh.yFor(offsetX);
			totalLength += interval[1] - interval[0];
		}
		if (totalLength <= 0d) {
			/**
			 * If we are at a point that has no y extent, simply take glbEndpoint's y value.
			 */
			result[1] = glbEndpoint._y;
		} else {
			/** Normal situation. */
			result[1] = Double.NaN;
			double runningLength = 0d;
			for (int kInterval = 0; kInterval < nIntervals; ++kInterval) {
				final double[] interval = intervals[kInterval];
				if (interval[0] > offsetY) {
					break;
				}
				if (interval[1] >= offsetY) {
					runningLength += offsetY - interval[0];
					break;
				}
				runningLength += interval[1] - interval[0];
			}
			result[1] = runningLength / totalLength;
		}
		return result;
	}

	@Override
	protected boolean offsetsAreIn(final double offsetX, final double offsetY) {
		final Endpoint glbEndpoint = getGlbWrtX(offsetX);
		if (glbEndpoint == null) {
			return false;
		}
		/**
		 * Vertical edges do not straddle anybody, so we have to check vertical edges
		 * explicitly.
		 */
		final double[] stretch = _verticalStretches.get(offsetX);
		if (stretch != null) {
			final int nIntervals = stretch.length / 2;
			for (int i = 0; i < nIntervals; ++i) {
				final double lowY = stretch[2 * i];
				final double highY = stretch[2 * i + 1];
				if (offsetY >= lowY && offsetY <= highY) {
					return false;
				}
			}
		}
		/**
		 * Compute the number of straddlers if we go straight up, by finding how many
		 * straddlers are above me.
		 */
		final Segment[] straddlers = glbEndpoint._straddlers;
		final int nStraddlers = straddlers.length;
		final Segment lookUpSegment = new Segment(offsetY);
		final int straddlerIndex = CombinatoricTools.getGlbIndex(straddlers, lookUpSegment, new Comparator<Segment>() {
			@Override
			public int compare(final Segment segment0, final Segment segment1) {
				final double compareValue = segment0.yFor(offsetX) - segment1.yFor(offsetX);
				if (compareValue < 0d) {
					return -1;
				}
				if (compareValue > 0d) {
					return 1;
				}
				return 0;
			}
		});
		if (straddlerIndex < 0 || straddlerIndex >= nStraddlers) {
			return false;
		}
		final Segment lowStraddler = straddlers[straddlerIndex];
		if (lowStraddler.yFor(offsetX) >= offsetY) {
			return false;
		}
		final int nCrossings = nStraddlers - 1 - straddlerIndex;
		return nCrossings % 2 == 1;
	}

	@Override
	protected void addInternalIntersectingTimesForOffsets(final double[] offsets0, final double[] offsets1,
			final SortedSet<Double> times) {
		/**
		 * Form a 2 by 2 matrix a with a right hand side rhs. t is column 0 and
		 * represents the proportion between offsets0 and offsets1. s is column 1 and
		 * represents the proportion between segmentLeft and segmentRight.
		 */
		final double a00 = offsets1[0] - offsets0[0];
		final double a10 = offsets1[1] - offsets0[1];
		final double[][] a = { new double[] { a00, 0d }, new double[] { a10, 0d } };
		final int n = _endpointsLeftFirst.length;
		for (int i = 0; i < n; ++i) {
			final Segment segment = _segments[i];
			a[0][0] = a00;
			a[1][0] = a10;
			a[0][1] = segment._left._x - segment._right._x;
			a[1][1] = segment._left._y - segment._right._y;
			final double[] rhs = { segment._left._x - offsets0[0], segment._left._y - offsets0[1] };
			final double[] ts = NumericalRoutines.solve(a, rhs);
			if (ts != null && ts[1] >= 0d && ts[1] <= 1d) {
				if (ts[0] >= 0d && ts[0] <= 1d) {
					times.add(ts[0]);
				}
			}
		}
	}

	/**
	 * The "clone with a twist" routine. This is how we change variables relatively
	 * easily. Note that the resulting Cdf should have 0 means; the Cdf routine that
	 * calls this one will rotate the means.
	 */
	@Override
	protected Cdf rotateOffsetDistribution(final MyLogger logger, final double c, final double s) {
		final int n = _endpointsLeftFirst.length;
		final double[][] rotatedPoints = new double[n][];
		for (int j = 0; j < n; ++j) {
			final Endpoint endpoint = _endpointsInInputOrder[j];
			final double endpointX = endpoint._x;
			final double endpointY = endpoint._y;
			rotatedPoints[j] = NumericalRoutines.rotateXy(c, s, endpointX, endpointY);
		}
		try {
			return new PolygonCdf(logger, rotatedPoints, _LoudAboutIrregularity);
		} catch (final BadCdfException e) {
			return null;
		}
	}

	private void validateSegments(final MyLogger logger, final int quietOrLoudAboutIrregularity)
			throws BadCdfException {
		final int n = _endpointsLeftFirst.length;
		final double oldAreaToLeft = 0d;
		for (int j = 0; j < n; ++j) {
			final Endpoint endpoint = _endpointsLeftFirst[j];
			final double thisAreaToLeft = endpoint._areaToLeft;
			if (thisAreaToLeft < oldAreaToLeft) {
				throwBadCdfException(logger, quietOrLoudAboutIrregularity);
			}
		}
		if (getTotalArea() <= 0d) {
			throwBadCdfException(logger, quietOrLoudAboutIrregularity);
		}
	}

	private double[] getStandInMeans() {
		double standInMeanX = getMeanX();
		if (Double.isNaN(standInMeanX)) {
			standInMeanX = 0d;
		}
		double standInMeanY = getMeanY();
		if (Double.isNaN(standInMeanY)) {
			standInMeanY = 0d;
		}
		return new double[] { standInMeanX, standInMeanY };
	}

	final private static boolean _DumpPoints = false;
	final private static boolean _DumpSegments = false;
	final private static boolean _DumpMeans = false;
	final private static boolean _DumpEndpoints = false;

	@Override
	protected String bodyString() {
		final StringBuffer b = new StringBuffer("");
		if (_DumpPoints) {
			b.append("Points:");
			final int n = _segments.length;
			final int rowLength = 2;
			final int nRows = (rowLength - 1 + n) / rowLength;
			for (int row = 0; row < nRows; ++row) {
				for (int clmn = 0; clmn < rowLength; ++clmn) {
					final int j = row + (clmn * nRows);
					if (j >= n) {
						break;
					}
					final Endpoint endpoint = _endpointsInInputOrder[j];
					final double x = endpoint._x;
					final double y = endpoint._y;
					b.append((clmn == 0 ? "\n" : "\t") + x + "\t" + y);
				}
			}
			return new String(b);
		}
		if (_segments == null) {
			return "null";
		}
		final int n = _segments.length;
		if (_DumpSegments) {
			b.append("Segments:");
			for (int i = 0; i < n; ++i) {
				final Segment segment = _segments[i];
				b.append("\n");
				b.append(segment.getString());
			}
		}
		if (_DumpMeans) {
			final double[] standInMeans = getStandInMeans();
			if (_verticalStretches != null && _verticalStretches.size() > 0) {
				b.append("\nVertical Stretches:");
				for (final Map.Entry<Double, double[]> entry : _verticalStretches.entrySet()) {
					b.append("\n").append(NumericalRoutines.getString(entry.getKey() + standInMeans[0]));
					final double[] adjustedInterval = entry.getValue().clone();
					adjustedInterval[0] += standInMeans[1];
					adjustedInterval[1] += standInMeans[1];
					b.append(",\t").append(NumericalRoutines.getString(adjustedInterval));
				}
			}
		}
		if (_DumpEndpoints) {
			b.append("\n\nEndpoints:");
			for (int j = 0; j < n; ++j) {
				final Endpoint endpoint = _endpointsLeftFirst[j];
				b.append('\n');
				b.append(endpoint);
			}
		}
		b.append("\nTotalArea[" + getTotalArea() + ']');
		return new String(b);
	}

	/** Needed to convert area to probability. */
	public double getTotalArea() {
		return _endpointsLeftFirst[_endpointsLeftFirst.length - 1]._areaToLeft;
	}

	/**
	 * One of two main classes. Segment is the other. Arrays of Segments and
	 * Endpoints form the heart of this Cdf. The two are knit together by not only
	 * the incidence relationship, but the notion of "Straddler." An Endpoint knows
	 * which Segments start before him and end at or after him. These are called
	 * Straddlers and are the essence of computing the Cdf.
	 */
	protected class Endpoint implements Comparable<Endpoint> {
		private Segment[] _straddlers;
		private double _areaToLeft;
		private double _cumSlope;
		private double _cumY;
		private double _x, _y;

		/** This one is real. */
		private Endpoint(final double x, final double y) {
			_x = x;
			_y = y;
			_cumY = _cumSlope = _areaToLeft = Double.NaN;
			_straddlers = null;
		}

		/** Use this one for binary searches. */
		private Endpoint(final double x, final double y, final double areaToLeft, final double cumSlope,
				final double cumY) {
			_x = x;
			_y = y;
			_areaToLeft = areaToLeft;
			_cumSlope = cumSlope;
			_cumY = cumY;
			_straddlers = null;
		}

		@Override
		public int compareTo(final Endpoint endpoint) {
			if (_x < endpoint._x) {
				return -1;
			}
			if (_x > endpoint._x) {
				return 1;
			}
			if (_y < endpoint._y) {
				return -1;
			}
			if (_y > endpoint._y) {
				return 1;
			}
			return 0;
		}

		public String getString() {
			final double totalArea = PolygonCdf.this.getTotalArea();
			final StringBuffer b = new StringBuffer();
			final double[] standInMeans = getStandInMeans();
			final String arrayString = NumericalRoutines
					.getString(new double[] { _x + standInMeans[0], _y + standInMeans[1] });
			b.append(arrayString);
			b.append(",[cumSlope,cumY,AreaLeft,ProbLeft] = ");
			final Endpoint[] points = _endpointsLeftFirst;
			final int nPoints = points == null ? 0 : points.length;
			final double probToLeft = //
					(points == null || points[nPoints - 1] == null) ? //
							Double.NaN : //
							_areaToLeft / totalArea;
			b.append(NumericalRoutines.getString(new double[] { _cumSlope, _cumY, _areaToLeft, probToLeft }));
			final int nStraddlingSegments = (_straddlers == null) ? 0 : _straddlers.length;
			for (int i = 0; i < nStraddlingSegments; ++i) {
				final Segment straddler = _straddlers[i];
				b.append("\n\t").append(straddler.toString());
				b.append(", y[").append(NumericalRoutines.getString(straddler.yFor(_x) + standInMeans[1])).append(']');
				b.append('.');
			}
			return new String(b);
		}

		@Override
		public String toString() {
			return getString();
		}
	}

	/**
	 * One of two main classes. Endpoint is the other. Arrays of Segments and
	 * Endpoints form the heart of this Cdf.
	 */
	private class Segment implements Comparable<Segment> {
		final private Endpoint _left;
		final private Endpoint _right;
		/**
		 * This is a "fake" value that is used with a Segment comparator. If
		 * !Double.isNaN(_yValue), then the segment is "fake."
		 */
		final private double _yValue;

		private Segment(final Endpoint left, final Endpoint right) {
			if (left.compareTo(right) < 0) {
				_left = left;
				_right = right;
			} else {
				_left = right;
				_right = left;
			}
			_yValue = Double.NaN;
		}

		private Segment(final double yValue) {
			_left = _right = null;
			_yValue = yValue;
		}

		@Override
		public int compareTo(final Segment segment) {
			if (segment == null) {
				return 1;
			}
			if (_left._x < segment._left._x) {
				return -1;
			}
			if (_left._x > segment._left._x) {
				return 1;
			}
			if (_right._x < segment._right._x) {
				return -1;
			}
			if (_right._x > segment._right._x) {
				return 1;
			}
			if (_left._y < segment._left._y) {
				return -1;
			}
			if (_left._y > segment._left._y) {
				return 1;
			}
			if (_right._y < segment._right._y) {
				return -1;
			}
			if (_right._y > segment._right._y) {
				return 1;
			}
			return 0;
		}

		private int compareSlope(final Segment segment) {
			final double hisXDiff = segment.getXDiff(), hisYDiff = segment.getYDiff();
			return compareSlope(hisXDiff, hisYDiff);
		}

		public int compareSlope(final double hisXDiff, final double hisYDiff) {
			final double myXDiff = getXDiff(), myYDiff = getYDiff();
			final double compareValue = myYDiff * hisXDiff - hisYDiff * myXDiff;
			int returnValue = compareValue > 0d ? 1 : (compareValue < 0d ? -1 : 0);
			if (myXDiff < 0d) {
				returnValue = -returnValue;
			}
			if (hisXDiff < 0d) {
				returnValue = -returnValue;
			}
			return returnValue;
		}

		public String getString() {
			final double[] standInMeans = getStandInMeans();
			final StringBuffer b = new StringBuffer();
			final double x1 = _left._x + standInMeans[0];
			final double y1 = _left._y + standInMeans[1];
			final String arrayString1 = NumericalRoutines.getString(new double[] { x1, y1 });
			b.append('<').append(arrayString1);
			b.append(',');
			final double x2 = _right._x + standInMeans[0];
			final double y2 = _right._y + standInMeans[1];
			final String arrayString2 = NumericalRoutines.getString(new double[] { x2, y2 });
			b.append(arrayString2);
			b.append('>');
			b.append(", m[").append(NumericalRoutines.getString(getSlope())).append(']');
			return new String(b);
		}

		private boolean isVertical() {
			return _left._x >= _right._x;
		}

		private double getXDiff() {
			return _right._x - _left._x;
		}

		private double getYDiff() {
			return _right._y - _left._y;
		}

		private double getSlope() {
			final double xDiff = getXDiff();
			final double yDiff = getYDiff();
			if (xDiff != 0d) {
				return yDiff / xDiff;
			}
			return Double.POSITIVE_INFINITY;
		}

		private double yFor(final double x) {
			if (_left == null) {
				return _yValue;
			}
			final double slope = getSlope();
			if (Double.isInfinite(slope)) {
				if (x == _left._x) {
					return (_left._y + _right._y) / 2d;
				}
				return Double.NaN;
			}
			return _left._y + (slope * (x - _left._x));
		}
	}

	public static void main(final String[] args) {

		final MyLogger logger = null;
		final double[][] pts = { //
				new double[] { 1d, 2d }, //
				new double[] { 6d, 1d }, //
				new double[] { 11d, 2d }, //
				new double[] { 11d, 3d }, //
				new double[] { 6d, 4d }, //
				new double[] { 1d, 3d } //
		};
		try {
			final PolygonCdf polygonCdf = new PolygonCdf(logger, pts, Cdf._LoudAboutIrregularity);
			final double[][][] regionsBetweenLeftAndRightCum = polygonCdf.getRegions(logger, 0.2, 0.8);
			for (final double[][] region : regionsBetweenLeftAndRightCum) {
				System.out.printf("\n%s", NumericalRoutines.getString(region));
			}
		} catch (final BadCdfException e) {
			e.printStackTrace();
		}

	}
}
