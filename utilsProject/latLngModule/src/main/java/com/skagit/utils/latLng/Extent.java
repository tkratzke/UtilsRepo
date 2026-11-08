package com.skagit.utils.latLng;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;

import com.skagit.utils.Constants0;
import com.skagit.utils.MathX0;
import com.skagit.utils.NumericalRoutines;
import com.skagit.utils.navigation.LatLngArrayStatics;

final public class Extent implements Cloneable, Serializable {

	final private static long serialVersionUID = 1L;
	final private static double _2Pi = Constants0._2Pi;
	final private static double _4Pi = Constants0._4Pi;
	final public static double _AllLongitudesLt = -180d;
	final public static double _AllLongitudesRt = 180d;

	public static class SmallCircle {
		final public double[] _centerLatLngArray;
		final double _radiusR;

		public SmallCircle(final Extent extent) {
			final double lt = extent.getLt();
			final double lo = extent.getLo();
			final double rt = extent.getRt();
			final double hi = extent.getHi();
			final double centerLat, centerLng;
			if (extent.isSinglePoint()) {
				centerLat = lo;
				centerLng = lt;
				/** Single point: give it a radius of a mile. */
				_radiusR = 1d * Constants0.Functions.nmiToR(centerLat);
			} else {
				final double lngRng = extent.getLngRng();
				final double[] latAndRadiusR = GreatCircleCalculator.getLatAndRadiusR(lo, hi, lngRng / 2d);
				centerLat = latAndRadiusR[0];
				centerLng = extent.getMidLng();
				_radiusR = latAndRadiusR[1];
			}
			_centerLatLngArray = new double[] { centerLat, centerLng };
		}

		private SmallCircle(final SmallCircle smallCircle) {
			_centerLatLngArray = smallCircle._centerLatLngArray.clone();
			_radiusR = smallCircle._radiusR;
		}
	}

	/** Comparators. */
	/** By ascending lo then ascending hi. */
	final public static Comparator<Extent> _SouthToNorth = new Comparator<>() {
		@Override
		public int compare(final Extent extent0, final Extent extent1) {
			if (extent0.equals(extent1)) {
				return 0;
			}
			final double lo0 = extent0.getLo();
			final double lo1 = extent1.getLo();
			if (lo0 != lo1) {
				return lo0 < lo1 ? -1 : 1;
			}
			final double hi0 = extent0.getHi();
			final double hi1 = extent1.getHi();
			if (hi0 != hi1) {
				return hi0 < hi1 ? -1 : 1;
			}
			final double lt0 = extent0.getLt();
			final double lt1 = extent1.getLt();
			if (lt0 != lt1) {
				return lt0 < lt1 ? -1 : 1;
			}
			final double rt0 = extent0.getRt();
			final double rt1 = extent1.getRt();
			if (rt0 != rt1) {
				return rt0 < rt1 ? -1 : 1;
			}
			return 0;
		}
	};

	/** By ascending leftLng then ascending rightLng. */
	final public static Comparator<Extent> _WestToEast = new Comparator<>() {
		@Override
		public int compare(final Extent extent0, final Extent extent1) {
			if (extent0.equals(extent1)) {
				return 0;
			}
			final double lt0 = extent0.getLt();
			final double lt1 = extent1.getLt();
			if (lt0 != lt1) {
				return lt0 < lt1 ? -1 : 1;
			}
			final double rt0 = extent0.getRt();
			final double rt1 = extent1.getRt();
			if (rt0 != rt1) {
				return rt0 < rt1 ? -1 : 1;
			}
			final double lo0 = extent0.getLo();
			final double lo1 = extent1.getLo();
			if (lo0 != lo1) {
				return lo0 < lo1 ? -1 : 1;
			}
			final double hi0 = extent0.getHi();
			final double hi1 = extent1.getHi();
			if (hi0 != hi1) {
				return hi0 < hi1 ? -1 : 1;
			}
			return 0;
		}
	};

	/** Data. */
	final private double[] _llrh;
	final private SmallCircle _smallCircle;

	/** Simple accessors. */
	public double getMidLat() {
		return (getLo() + getHi()) / 2d;
	}

	public SmallCircle getSmallCircle() {
		return _smallCircle;
	}

	public double getDiameterR() {
		return 2d * _smallCircle._radiusR;
	}

	public static boolean isAllLongitudes(final double lt, final double rt) {
		if (LatLngArrayStatics.getInRange0_360(rt - lt) != 0d) {
			return false;
		}
		return lt != rt;
	}

	public boolean isAllLongitudes() {
		return isAllLongitudes(getLt(), getRt());
	}

	double getLt() {
		return isAllLongitudes() ? -180d : _llrh[0];
	}

	double getLo() {
		return _llrh[1];
	}

	double getRt() {
		return isAllLongitudes() ? 180d : _llrh[2];
	}

	double getHi() {
		return _llrh[3];
	}

	public double getMidLng() {
		if (isAllLongitudes()) {
			return 0d;
		}
		final double lt = getLt(), rt = getRt();
		if (lt <= rt) {
			return (lt + rt) / 2d;
		}
		final double lngRng = rt + 360d - lt;
		return LatLngArrayStatics.getInRange180_180(lt + lngRng / 2d);
	}

	/** equals, hashCode, and clone. */
	@Override
	public boolean equals(final Object o) {
		if (o == null || !(o instanceof final Extent extent)) {
			return false;
		}
		return Arrays.equals(_llrh, extent._llrh);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(_llrh);
	}

	@Override
	public Extent clone() {
		return new Extent(_llrh.clone(), new SmallCircle(_smallCircle));
	}

	/** Normalizing routines. */
	/**
	 * This one normalizes the individual longitudes, and makes sure that lo <= hi.
	 * Every ctor, except for the null one, should call this.
	 */
	private static void correctLlrh(final double[] llrh) {
		final double ltIn = llrh[0], rtIn = llrh[2];
		final double ltOut = LatLngArrayStatics.getInRange180_180(ltIn);
		final double rtOut = LatLngArrayStatics.getInRange180_180(rtIn);
		if (ltOut == rtOut && ltIn != rtIn) {
			/** Caller wants allLongitudes. This just normalizes that directive. */
			llrh[0] = _AllLongitudesLt;
			llrh[2] = _AllLongitudesRt;
		} else {
			llrh[0] = ltOut;
			llrh[2] = rtOut;
		}
		final double lo = llrh[1], hi = llrh[3];
		if (lo > hi) {
			llrh[1] = hi;
			llrh[3] = lo;
		}
		llrh[1] = Math.min(90d, llrh[1]);
		llrh[3] = Math.max(-90d, llrh[3]);
	}

	/**
	 * Adjusts so that ltRt[0] is the left side of the smallest window. Assumes both
	 * are [-180,180).
	 */
	private static void correctLtRt(final double[] ltRt) {
		final double lt = ltRt[0], rt = ltRt[1];
		if (lt <= rt) {
			if (lt + 180d >= rt) {
				/** Nothing to do. */
			} else {
				/** rt is actually left. */
				ltRt[0] = rt;
				ltRt[1] = lt;
			}
			return;
		}
		/** rt < lt. */
		if (rt + 180d <= lt) {
			/** -179 + 180 < 179. Leave it alone. */
		} else {
			/** rt is actually left. */
			ltRt[0] = rt;
			ltRt[1] = lt;
		}
	}

	/** Ctors. */
	/** Null ctor. */
	public Extent() {
		_llrh = null;
		_smallCircle = null;
	}

	/** Assumes that the caller really intends left to be left! */
	public Extent(final double[] llrh) {
		_llrh = llrh;
		correctLlrh(_llrh);
		_smallCircle = new SmallCircle(this);
	}

	/** For when the caller simply wants the smallest lng window. */
	public Extent(final double[] latLngArray0, final double[] latLngArray1) {
		final double lat0 = latLngArray0[0];
		final double lng0 = latLngArray0[1];
		final double lat1 = latLngArray1[0];
		final double lng1 = latLngArray1[1];
		_llrh = new double[] { lng0, lat0, lng1, lat1 };
		correctLlrh(_llrh);
		final double lo = Math.min(lat0, lat1);
		final double hi = Math.max(lat0, lat1);
		final double[] ltRt = { _llrh[0], _llrh[2] };
		correctLtRt(ltRt);
		_llrh[0] = ltRt[0];
		_llrh[1] = lo;
		_llrh[2] = ltRt[1];
		_llrh[3] = hi;
		_smallCircle = new SmallCircle(this);
	}

	/** Just a wrapper for the hard work. */
	public Extent(final List<double[]> path, final boolean connectLastToFirst) {
		final int nLatLngArrays = path.size();
		final int nGcas = nLatLngArrays - (connectLastToFirst ? 0 : 1);
		final ArrayList<Extent> extents = new ArrayList<>(nGcas);
		for (int k = 0; k < nGcas; ++k) {
			final double[] latLngArray0 = path.get(k);
			final double[] latLngArray1 = path.get((k + 1) % nLatLngArrays);
			final Extent gcaExtent = new Extent(latLngArray0, latLngArray1);
			extents.add(gcaExtent);
		}
		final boolean[] isLngGapFree = new boolean[1];
		final Extent extent = new Extent(extents, isLngGapFree);
		_llrh = extent._llrh;
		_smallCircle = extent._smallCircle;
	}

	/** Hard one. */
	public Extent(final List<Extent> extents, final boolean[] isLngGapFree) {
		final int n = extents.size();
		if (n == 0) {
			_llrh = null;
			_smallCircle = null;
			return;
		}

		double lo = 90d;
		double hi = -90d;
		for (int k = 0; k < n; ++k) {
			final Extent extent = extents.get(k);
			lo = Math.min(extent.getLo(), lo);
			hi = Math.max(extent.getHi(), hi);
		}
		final double[][] lngWindows = new double[n][];
		boolean allLongitudes = false;
		for (int k = 0; k < n; ++k) {
			final Extent extent = extents.get(k);
			final double[] ltRt = { extent.getLt(), extent.getRt() };
			if (isAllLongitudes(ltRt[0], ltRt[1])) {
				allLongitudes = true;
				break;
			}
			lngWindows[k] = ltRt;
		}
		final double lt, rt;
		if (allLongitudes) {
			lt = _AllLongitudesLt;
			rt = _AllLongitudesRt;
		} else {
			final double[] lngWindow = getLngWindow(lngWindows, isLngGapFree);
			if (lngWindow == null) {
				_leftLng = _UnsetExtent._leftLng;
				_minLat = _UnsetExtent._minLat;
				_rightLng = _UnsetExtent._rightLng;
				_maxLat = _UnsetExtent._maxLat;
				_smallCircle = new SmallCircle(this);
				return;
			}
			lt = lngWindow[0];
			rt = lngWindow[1];
		}
		final double[] llrh = correctLlrh(lt, lo, rt, hi);
		_leftLng = llrh[0];
		_minLat = llrh[1];
		_rightLng = llrh[2];
		_maxLat = llrh[3];
		_smallCircle = new SmallCircle(this);
	}

	public double computeSqDegs() {
		return Math.toDegrees(Math.toDegrees(computeSqR()));
	}

	public double computeSqR() {
		final double sinMax = Math.abs(MathX0.sinX(Math.toRadians(_maxLat)));
		final double sinMin = Math.abs(MathX0.sinX(Math.toRadians(_minLat)));
		final double areaMax = _2Pi * (1d - sinMax);
		final double areaMin = _2Pi * (1d - sinMin);
		final double areaA;
		if (_minLat >= 0d || _maxLat <= 0d) {
			areaA = Math.max(areaMax, areaMin) - Math.min(areaMax, areaMin);
		} else {
			areaA = _4Pi - areaMax - areaMin;
		}
		final double lngRng = getLngRng();
		return lngRng / 360d * areaA;
	}

	/** For cloning purposes. */
	private Extent(final double[] llrh, final SmallCircle smallCircle) {
		_llrh = llrh;
		_smallCircle = smallCircle;
	}

	/** Find the smallest extent that contains the circle. */
	public Extent(final double[] centerLatLngArray, final double radiusD) {
		final double centerLat = centerLatLngArray[0];
		/** Don't go over the poles. */
		double leftLngB, minLatB, rightLngB, maxLatB;
		if (centerLat + radiusD >= 90d || centerLat - radiusD <= -90d) {
			leftLngB = -180d;
			minLatB = Math.max(-90d, centerLat - radiusD);
			rightLngB = 180d;
			maxLatB = Math.min(90d, centerLat + radiusD);
		} else {
			final double centerLng = centerLatLngArray[1];
			final double absLatR = Math.abs(Math.toRadians(centerLat));
			final double radiusR = Math.toRadians(radiusD);
			final double c1 = MathX0.cosX(absLatR);
			final double s1 = MathX0.sinX(absLatR);
			final double c4 = MathX0.cosX(radiusR);
			/**
			 * <pre>
			 * Suppose (wLat,wLng) is the winning point. Its coords are:
			 * (cos(wLat)*cos(wLng), cos(wLat)*sin(wLng), sin(wLat)).
			 * Define: (c,s) = (cos(wLat), sin(wLat)) (c3,s3) = (cos(wLng), sin(wLng))
			 *
			 * If we convert to cartesian, we know:
			 * dot(center,(wLat,wLng)) = c4:
			 *   (c1 * (c3 * sqrt(1-s^2))) + (s1*s) = c4.
			 * Let Q = (c1*c3)^2. Q * (1 - s^2) = c4^2 - 2(c4 * s1*s) + (s1*s)^2.
			 * Gathering constants, s terms, and s^2 terms:
			 *   s^2 * (s1^2 + Q) + s * (-2 * c4 * s1) + (c4^2 - Q) = 0.
			 * We must pick Q so that there is only one solution for s.
			 * So B*B - 4AC must be 0:
			 *   A = s1^2 + Q B = -2 * c4 * s1 C = c4^2 - Q.
			 *   0 = B*B - 4AC = 4(c4*s1)^2 - 4(s1^2+Q)(c4^2-Q).
			 * Discarding the common factor 4, we have a quadratic in Q:
			 *   C' = (c4*s1)^2 - (s1^2 *c4^2) = 0.
			 * B' = -(-s1^2 + c4^2) = s1^2 - c4^2. A' = 1
			 *
			 * Because C' is 0, we have: Q(Q + s1^2 - c4^2) = 0 and Q = c4^2 - s1^2.
			 * </pre>
			 */
			final double Q = c4 * c4 - s1 * s1;
			final double c3 = Math.sqrt(Q) / c1;
			final double deltaLng = Math.toDegrees(MathX0.acosX(c3));
			leftLngB = LatLngArrayStatics.getInRange180_180(centerLng - deltaLng);
			rightLngB = LatLngArrayStatics.getInRange180_180(centerLng + deltaLng);
			minLatB = LatLngArrayStatics.getInRange180_180(centerLat - radiusD);
			maxLatB = LatLngArrayStatics.getInRange180_180(centerLat + radiusD);
		}
		final double[] llrh = correctLlrh(leftLngB, minLatB, rightLngB, maxLatB);
		_leftLng = llrh[0];
		_minLat = llrh[1];
		_rightLng = llrh[2];
		_maxLat = llrh[3];
		_smallCircle = new SmallCircle(this);
	}

	public double getLngRng() {
		return getLngRng(_leftLng, _rightLng);
	}

	public static double getLngRng(final double lt, final double rt) {
		if (isAllLongitudes(lt, rt)) {
			return 360d;
		}
		return LatLngArrayStatics.getInRange0_360(rt - lt);
	}

	public double getLatRng() {
		return _maxLat - _minLat;
	}

	public Extent buildExtension(final double[] latLngArray) {
		final double lat = latLngArray[0];
		final double lng = latLngArray[1];
		final double minLat;
		final double maxLat;
		final double leftLng;
		final double rightLng;
		if (!valuesAreSet()) {
			minLat = maxLat = lat;
			leftLng = rightLng = lng;
		} else {
			minLat = Math.min(_minLat, lat);
			maxLat = Math.max(_maxLat, lat);
			final double[] mine = { _leftLng, _rightLng };
			final double[] his = { lng, lng };
			final double[] lngWindow = getLngWindow(new double[][] { mine, his }, /* isLngGapFree= */null);
			leftLng = lngWindow[0];
			rightLng = lngWindow[1];
		}
		final double[] llrh = correctLlrh(leftLng, minLat, rightLng, maxLat);
		return new Extent(llrh);
	}

	public Extent buildExtension(final double[] latLngArray0, final double[] latLngArray1) {
		if (latLngArray0 == null) {
			return latLngArray1 == null ? this : buildExtension(latLngArray1);
		}
		if (latLngArray1 == null) {
			return buildExtension(latLngArray0);
		}
		/** Compute the window of the new edge. */
		final double lng0 = latLngArray0[1];
		final double lng1 = latLngArray1[1];
		final double minLng = Math.min(lng0, lng1);
		final double maxLng = Math.max(lng0, lng1);
		final double ltIn, rtIn;
		if (minLng + 180d >= maxLng) {
			ltIn = minLng;
			rtIn = maxLng;
		} else {
			ltIn = maxLng;
			rtIn = minLng;
		}
		final double lat0 = latLngArray0[0];
		final double lat1 = latLngArray1[0];
		final double loIn = Math.min(lat0, lat1);
		final double hiIn = Math.max(lat0, lat1);

		/** If I am unset, we have our new Extent. */
		if (!valuesAreSet()) {
			final double[] llrh = correctLlrh(ltIn, loIn, rtIn, hiIn);
			return new Extent(llrh);
		}

		final double[] mine = { _leftLng, _rightLng };
		final double[] his = { ltIn, rtIn };
		final double[] lngWindow = getLngWindow(new double[][] { mine, his }, /* isLngGapFree= */null);
		final double lt = lngWindow[0];
		final double rt = lngWindow[1];
		final double lo = Math.min(_minLat, loIn);
		final double hi = Math.max(_maxLat, hiIn);
		final double[] llrh = correctLlrh(lt, lo, rt, hi);
		return new Extent(llrh);
	}

	public Extent createContainsPole(final boolean north) {
		final double minLat, maxLat;
		if (north) {
			minLat = _minLat;
			maxLat = 90d;
		} else {
			minLat = -90d;
			maxLat = _maxLat;
		}
		final double[] llrh = correctLlrh(-180d, minLat, 180d, maxLat);
		return new Extent(llrh);
	}

	public boolean latWindowContains(final double lat0) {
		final double minLat = LatLngArrayStatics.roundToLattice180_180I(_minLat);
		final double maxLat = LatLngArrayStatics.roundToLattice180_180I(_maxLat);
		final double lat = LatLngArrayStatics.roundToLattice180_180I(lat0);
		return minLat <= lat && lat <= maxLat;
	}

	public boolean lngWindowContains(final double lng) {
		return ltRtContains(_leftLng, _rightLng, lng);
	}

	public double degsToLatWindow(final double lat) {
		if (latWindowContains(lat)) {
			return 0d;
		}
		final double minLat = _minLat;
		if (lat < minLat) {
			return minLat - lat;
		}
		final double maxLat = _maxLat;
		return lat - maxLat;
	}

	public double degsToLngWindow(final double lng) {
		if (lngWindowContains(lng)) {
			return 0d;
		}
		final double leftLng = _leftLng;
		final double lngToLeft = LatLngArrayStatics.getInRange0_360(leftLng - lng);
		final double rightLng = _rightLng;
		final double rightToLng = LatLngArrayStatics.getInRange0_360(lng - rightLng);
		return Math.min(lngToLeft, rightToLng);
	}

	private static boolean ltRtContains(final double lt0, final double rt0, final double lng0) {
		if (isAllLongitudes(lt0, rt0)) {
			return true;
		}
		final double lt = LatLngArrayStatics.roundToLattice180_180I(lt0);
		final double rt = LatLngArrayStatics.roundToLattice180_180I(rt0);
		final double lng = LatLngArrayStatics.roundToLattice180_180I(lng0);
		if (lt <= rt) {
			return lt <= lng && lng <= rt;
		}
		return lng >= lt || lng <= rt;
	}

	public static boolean lngWindowContains(final double lng0, final double lng1, final double lng) {
		final double[] ltRt = getLtRt(lng0, lng1);
		return ltRtContains(ltRt[0], ltRt[1], lng);
	}

	private double getSomeOverlappingLng(final Extent extent) {
		if (!valuesAreSet() || !extent.valuesAreSet()) {
			return Double.NaN;
		}
		final double myLeftLng = _leftLng;
		final double myRightLng = _rightLng;
		final double hisLeftLng = extent._leftLng;
		final double hisRightLng = extent._rightLng;
		if (ltRtContains(myLeftLng, myRightLng, hisLeftLng)) {
			return hisLeftLng;
		}
		if (ltRtContains(hisLeftLng, hisRightLng, myLeftLng)) {
			return myLeftLng;
		}
		return Double.NaN;
	}

	public boolean contains(final double[] latLngArray) {
		final double lat = latLngArray[0];
		if (!latWindowContains(lat)) {
			return false;
		}
		return lngWindowContains(latLngArray[1]);
	}

	final private static int _DigitsForDblFormat = 4;
	final private static String _DblFormat = String.format("%%.%df", _DigitsForDblFormat);
	final public static String _StandardFormat = String.format("LLRH[%s %s %s %s]", _DblFormat, _DblFormat, _DblFormat,
			_DblFormat);

	public String getString() {
		final double myLeft = _leftLng;
		final double myLow = _minLat;
		final double myRight = _rightLng;
		final double myHigh = _maxLat;
		return String.format(_StandardFormat, myLeft, myLow, myRight, myHigh);
	}

	public String getCenterAndRadiusString() {
		final double[] centerLatLngArray = _smallCircle._centerLatLngArray;
		final double nmi = _smallCircle._radiusR / Constants0.Functions.nmiToR(centerLatLngArray[0]);
		final LatLng centerLatLng = LatLng.getLatLng(centerLatLngArray[0], centerLatLngArray[1]);
		return String.format("%s %.3fNM", centerLatLng.getString(3), nmi);
	}

	public String getString(final int nDigits) {
		final double myLeft = _leftLng;
		final double myLow = _minLat;
		final double myRight = _rightLng;
		final double myHigh = _maxLat;
		final String thisFormat = String.format("LLRH[%%.%df,%%.%df,%%.%df,%%.%df]", nDigits, nDigits, nDigits,
				nDigits);
		return String.format(thisFormat, myLeft, myLow, myRight, myHigh);
	}

	@Override
	public String toString() {
		return getString();
	}

	private double getSomeOverlappingLat(final Extent extent) {
		if (!valuesAreSet() || !extent.valuesAreSet()) {
			return Double.NaN;
		}
		final double myMinLat = _minLat;
		final double myMaxLat = _maxLat;
		final double hisMinLat = extent._minLat;
		final double hisMaxLat = extent._maxLat;
		if (hisMinLat <= myMaxLat && myMaxLat <= hisMaxLat) {
			return myMaxLat;
		}
		if (myMinLat <= hisMaxLat && hisMaxLat <= myMaxLat) {
			return hisMaxLat;
		}
		return Double.NaN;
	}

	/** Even if just on the border, it is an overlap. */
	public boolean overlaps(final Extent extent) {
		if (extent == null || !extent.valuesAreSet()) {
			return false;
		}
		final double overlappingLat = getSomeOverlappingLat(extent);
		if (Double.isNaN(overlappingLat)) {
			return false;
		}
		final double overlappingLng = getSomeOverlappingLng(extent);
		if (Double.isNaN(overlappingLng)) {
			return false;
		}
		return true;
	}

	/** Do I surround extent? */
	public boolean surrounds(final Extent extent, final boolean mustBeClean) {
		final double myMinLat = _minLat;
		final double myMaxLat = _maxLat;
		final double hisMinLat = extent._minLat;
		final double hisMaxLat = extent._maxLat;
		if (hisMinLat < myMinLat || (mustBeClean && hisMinLat == myMinLat)) {
			return false;
		}
		if (hisMaxLat > myMaxLat || (mustBeClean && hisMaxLat == myMaxLat)) {
			return false;
		}
		/** latitude is good. Check longitude. */
		final double myLeftLng = _leftLng;
		final double myRightLng = _rightLng;
		final double hisLeftLng = extent._leftLng;
		final double hisRightLng = extent._rightLng;
		if (isAllLongitudes(myLeftLng, myRightLng)) {
			if (mustBeClean) {
				return !isAllLongitudes(hisLeftLng, hisRightLng);
			}
			return true;
		}
		if (isAllLongitudes(hisLeftLng, hisRightLng)) {
			return false;
		}

		/** Neither of us is the whole longitudinal world. */
		if (myLeftLng <= myRightLng) {
			/** I do not cross the dateline. To surround him, he cannot either. */
			if (hisLeftLng > hisRightLng) {
				return false;
			}
			/** He does not cross the dateline. */
			if (mustBeClean) {
				return myLeftLng < hisLeftLng && hisRightLng < myRightLng;
			}
			return myLeftLng <= hisLeftLng && hisRightLng <= myRightLng;
		}
		/** I cross the dateline. */
		if (hisLeftLng <= hisRightLng) {
			/** He does not cross the dateline. */
			if (myLeftLng < hisLeftLng || !mustBeClean && myLeftLng == hisLeftLng) {
				return true;
			}
			if (hisRightLng < myRightLng || !mustBeClean && hisRightLng == myRightLng) {
				return true;
			}
			return false;
		}
		/** We both cross the dateline. */
		if (mustBeClean) {
			return myLeftLng < hisLeftLng && hisRightLng < myRightLng;
		}
		return myLeftLng <= hisLeftLng && hisRightLng <= myRightLng;
	}

	public static boolean lngWindowsOverlap(final double left0In, final double right0In, final double left1In,
			final double right1In) {
		final double left0 = LatLngArrayStatics.roundToLattice180_180I(left0In);
		final double right0 = LatLngArrayStatics.roundToLattice180_180I(left0In);
		final double left1 = LatLngArrayStatics.roundToLattice180_180I(left0In);
		final double right1 = LatLngArrayStatics.roundToLattice180_180I(left0In);
		/** If either is the entire globe, return true. */
		if ((right0 >= left0 + 360d) || (right1 >= left1 + 360d)) {
			return true;
		}
		/** Is either of 1's endpoints in 0's window? */
		final double left0ToRight0 = NumericalRoutines.generalMod(right0 - left0, 360d);
		final double left0ToLeft1 = NumericalRoutines.generalMod(left1 - left0, 360d);
		if (left0ToLeft1 <= left0ToRight0) {
			return true;
		}
		final double left0ToRight1 = NumericalRoutines.generalMod(right1 - left0, 360d);
		if (left0ToRight1 <= left0ToRight0) {
			return true;
		}
		/** Is either of 0's endpoints in 1's window? */
		final double left1ToRight1 = NumericalRoutines.generalMod(right1 - left1, 360d);
		final double left1ToLeft0 = NumericalRoutines.generalMod(left0 - left1, 360d);
		if (left1ToLeft0 <= left1ToRight1) {
			return true;
		}
		final double left1ToRight0 = NumericalRoutines.generalMod(right0 - left1, 360d);
		if (left1ToRight0 <= left1ToRight1) {
			return true;
		}
		return false;
	}

	public static boolean latWindowsOverlap(final double low0In, final double high0In, final double low1In,
			final double high1In) {
		final double low0 = LatLngArrayStatics.roundToLattice180_180I(low0In);
		final double high0 = LatLngArrayStatics.roundToLattice180_180I(low0In);
		final double low1 = LatLngArrayStatics.roundToLattice180_180I(low0In);
		final double high1 = LatLngArrayStatics.roundToLattice180_180I(low0In);
		if (high1 < low0 || low1 > high0) {
			return false;
		}
		return true;
	}

	public Extent buildExtension(final Extent extent) {
		if (!extent.valuesAreSet()) {
			/** It's up to me. */
			return this;
		}
		if (!valuesAreSet()) {
			/** It's up to him. */
			return extent;
		}
		if (surrounds(extent, /* mustBeClean= */false)) {
			return this;
		}
		if (extent.surrounds(this, /* mustBeClean= */false)) {
			return extent;
		}
		final double myMinLat = _minLat;
		final double myMaxLat = _maxLat;
		final double hisMinLat = extent._minLat;
		final double hisMaxLat = extent._maxLat;
		final double minLat = Math.min(myMinLat, hisMinLat);
		final double maxLat = Math.max(myMaxLat, hisMaxLat);
		final double myLeftLng = _leftLng;
		final double myRightLng = _rightLng;
		final double hisLeftLng = extent._leftLng;
		final double hisRightLng = extent._rightLng;
		final double[][] lngWindows = { new double[] { myLeftLng, myRightLng },
				new double[] { hisLeftLng, hisRightLng } };
		final double[] lngWindow = getLngWindow(lngWindows, /* isLngGapFree= */null);
		final double[] llrh = correctLlrh(lngWindow[0], minLat, lngWindow[1], maxLat);
		return new Extent(llrh);
	}

	public boolean crossesGreenwich() {
		final double rt = getRt();
		if (rt <= 0d) {
			return false;
		}
		final double lt = getLt();
		return crossesIdl() ? true : lt <= 0d;
	}

	public boolean crossesIdl() {
		if (isAllLongitudes()) {
			return true;
		}
		return getRt() < getLt();
	}

	public boolean isWholeWorld() {
		return isAllLongitudes() && getHi() >= 90d && getLo() <= -90d;
	}

	public Extent buildExtensionWithBuffers(double latBuffer, double lngBuffer) {
		latBuffer = Math.abs(latBuffer);
		lngBuffer = Math.abs(lngBuffer);
		final double minLat = Math.max(-90d, _minLat - latBuffer);
		final double maxLat = Math.min(90d, _maxLat + latBuffer);
		final double leftLng = _leftLng - lngBuffer;
		final double rightLng = _rightLng + lngBuffer;
		final double[] llrh = correctLlrh(leftLng, minLat, rightLng, maxLat);
		return new Extent(llrh);
	}

	public Extent buildExtensionWithDegBuffer(final double degBuffer) {
		final double highLat = _maxLat + degBuffer;
		final double lowLat = _maxLat + degBuffer;
		if (highLat >= 90d) {
			if (lowLat <= -90d) {
				return createWholeWorldExtent();
			}
			return new Extent(-180d, lowLat, 180d, 90d);
		}
		if (lowLat <= -90d) {
			return new Extent(-180d, -90d, 180d, highLat);
		}
		final double lngBuff = LatLngArrayStatics
				.haversineToLngX(Math.abs(highLat) >= Math.abs(lowLat) ? highLat : lowLat, degBuffer);
		return new Extent(_leftLng - lngBuff, lowLat, _rightLng + lngBuff, highLat);
	}

	/** Returns the whole world. */
	public static Extent createWholeWorldExtent() {
		return new Extent(-180d, -90d, 180d, 90d);
	}

	public static Extent getUnsetExtent() {
		return _UnsetExtent;
	}

	public double[] getCentralLatLngArray() {
		final double midLat = getMidLat();
		final double midLng = getMidLng();
		return new double[] { midLat, midLng };
	}

	public double[][] createLatLngArray() {
		final double[] lwLf = { _minLat, _leftLng };
		final double[] lwRt = { _minLat, _rightLng };
		final double[] hgRt = { _maxLat, _rightLng };
		final double[] hgLf = { _maxLat, _leftLng };
		return new double[][] { lwLf, lwRt, hgRt, hgLf };
	}

	/** Finds the smallest lngWindow that contains lngWindows. */
	private static double[] getLngWindow(final double[][] lngWindows, final boolean[] isLngGapFree) {
		final double[][] lngWindowsOut = getDisjointLngWindows(lngWindows);
		final int nLngWindowsOut = lngWindowsOut.length;
		if (isLngGapFree != null) {
			isLngGapFree[0] = nLngWindowsOut < 2;
		}
		if (nLngWindowsOut < 2) {
			return nLngWindowsOut == 0 ? null : lngWindowsOut[0];
		}
		/** Find the biggest gap. */
		double biggestGap = 0d;
		double winningRight = Double.NaN;
		double winningLeft = Double.NaN;
		for (int k = 0; k < nLngWindowsOut; ++k) {
			final double[] interval = lngWindowsOut[k];
			final double myRight = interval[1];
			final double[] nextInterval = lngWindowsOut[(k + 1) % nLngWindowsOut];
			final double nextLeft = nextInterval[0];
			assert nextLeft != myRight : "Should be disjoint(3)";
			final double gap;
			if (myRight < nextLeft) {
				gap = nextLeft - myRight;
			} else {
				gap = nextLeft + 360d - myRight;
			}
			if (gap > biggestGap) {
				biggestGap = gap;
				winningLeft = nextLeft;
				winningRight = myRight;
			}
		}
		return new double[] { winningLeft, winningRight };
	}

	private static double[][] getDisjointLngWindows(final double[][] lngWindows) {
		final int nIn = lngWindows == null ? 0 : lngWindows.length;
		if (nIn == 0) {
			return new double[0][];
		}
		/** Separate into idl and normal. */
		double[] idl = null;
		final ArrayList<double[]> normalsIn = new ArrayList<>(nIn);
		for (int k = 0; k < nIn; ++k) {
			final double[] lngWindow = lngWindows[k];
			if (lngWindow[0] == _AllLongitudesLt && lngWindow[1] == _AllLongitudesLt) {
				return new double[][] { { _AllLongitudesLt, _AllLongitudesRt } };
			}
			if (lngWindow[0] > lngWindow[1]) {
				if (idl == null) {
					idl = lngWindow.clone();
				} else {
					idl[0] = Math.min(idl[0], lngWindow[0]);
					idl[1] = Math.max(idl[1], lngWindow[1]);
					if (idl[0] <= idl[1]) {
						return new double[][] { { _AllLongitudesLt, _AllLongitudesRt } };
					}
				}
			} else {
				normalsIn.add(lngWindow);
			}
		}
		/**
		 * Sort the normals by left then right, then combine them into disjoint
		 * intervals.
		 */
		normalsIn.sort(NumericalRoutines._ByFirstAndSecondOnly);
		final ArrayList<double[]> normalsOutList = new ArrayList<>();
		final int nNormalsIn = normalsIn.size();
		double[] current = null;
		for (int k = 0; k < nNormalsIn; ++k) {
			final double[] normal = normalsIn.get(k);
			if (current == null) {
				current = normal.clone();
			} else if (normal[0] <= current[1]) {
				current[1] = Math.max(current[1], normal[1]);
			} else {
				normalsOutList.add(current);
				current = normal.clone();
			}
		}
		if (current != null) {
			normalsOutList.add(current);
		}
		assert disjointTest(normalsOutList) : "Should be disjoint at this point(1).";
		final int nNormalsOut = normalsOutList.size();
		if (nNormalsOut == 0) {
			return idl == null ? new double[][] {} : new double[][] { idl };
		}
		if (idl == null) {
			return normalsOutList.toArray(new double[nNormalsOut][]);
		}
		/**
		 * We have normalsOut and we have an Idl. Find the first normal east of Idl that
		 * is not hooked by idlRight.
		 */
		int firstKeeper = -1;
		for (int k = 0; k < nNormalsOut; ++k) {
			final double[] normal = normalsOutList.get(k);
			if (idl[1] >= normal[0]) {
				idl[1] = Math.max(idl[1], normal[1]);
				continue;
			}
			/** This is the first one that is kept. */
			firstKeeper = k;
			break;
		}
		if (firstKeeper == -1) {
			/**
			 * Never found a keeper; all of the normals get swallowed. This now giant Idl
			 * might wrap around on itself.
			 */
			if (idl[1] >= idl[0]) {
				return new double[][] { { _AllLongitudesLt, _AllLongitudesRt } };
			}
			return new double[][] { idl };
		}
		/** Find the last one west of Idl that is not hooked by idlLeft. */
		int lastKeeper = -1;
		for (int k = nNormalsOut - 1; k >= firstKeeper; --k) {
			final double[] normal = normalsOutList.get(k);
			if (idl[0] <= normal[1]) {
				idl[0] = Math.min(idl[0], normal[0]);
				continue;
			}
			lastKeeper = k;
			break;
		}
		if (lastKeeper == -1) {
			/**
			 * Never found a keeper; all of the normals get swallowed. This now giant Idl
			 * might wrap around on itself.
			 */
			if (idl[1] >= idl[0]) {
				return new double[][] { { _AllLongitudesLt, _AllLongitudesRt } };
			}
			return new double[][] { idl };
		}
		final int nNormals = lastKeeper - firstKeeper + 1;
		final double[][] returnValue = new double[nNormals + 1][];
		for (int k = firstKeeper; k <= lastKeeper; ++k) {
			returnValue[k - firstKeeper] = normalsOutList.get(k);
		}
		returnValue[nNormals] = idl;
		assert disjointTest(Arrays.asList(returnValue)) : "Should be disjoint(2).";
		return returnValue;
	}

	/**
	 * These must be sorted by ascending lefts, including the (at most one) Idl
	 * crosser.
	 */
	private static boolean disjointTest(final List<double[]> intervals) {
		final int nIntervals = intervals.size();
		if (nIntervals <= 1) {
			return true;
		}
		for (int k = 0; k < nIntervals; ++k) {
			final double[] interval = intervals.get(k);
			final double left = interval[0];
			final double right = interval[1];
			if (k > 0) {
				final double[] oldInterval = intervals.get(k - 1);
				final double oldLeft = oldInterval[0];
				final double oldRight = oldInterval[1];
				final double oldLeftToOldRight = LatLngArrayStatics.getInRange0_360(oldRight - oldLeft);
				final double oldLeftToLeft = LatLngArrayStatics.getInRange0_360(left - oldLeft);
				final double oldLeftToRight = LatLngArrayStatics.getInRange0_360(right - oldLeft);
				final boolean ok = oldLeftToOldRight < oldLeftToLeft && oldLeftToLeft <= oldLeftToRight;
				if (!ok) {
					return false;
				}
			}
		}
		return true;
	}

	public static void main(final String[] args) {
		final double[][] lngWindows = { /* { 177, -175 }, */{ -178, -2 }, { 1, 179 }, { -3, 1 } };
		final boolean[] isLngGapFree = new boolean[1];
		final double[] lngWindow = getLngWindow(lngWindows, isLngGapFree);
		System.out.printf("\n%s lngGapFree[%b]", NumericalRoutines.getString(lngWindow), isLngGapFree[0]);
		/** The rest is for a different test. */
		final Integer[] members = { 0, 2, 4, 6 };
		final TreeSet<Integer> memberSet = new TreeSet<>(Arrays.asList(members));
		final Integer floor = memberSet.floor(1);
		final NavigableSet<Integer> frontEnd = memberSet.headSet(floor, true).descendingSet();
		for (final int k : frontEnd) {
			System.out.printf("\n%d", k);
		}
	}

	public boolean isSinglePoint() {
		return _leftLng == _rightLng && _minLat == _maxLat;
	}

	public double getSmall(final boolean isRow) {
		if (!valuesAreSet()) {
			return isRow ? -180d : -90d;
		}
		return isRow ? _leftLng : _minLat;
	}

	public double getBig(final boolean isRow) {
		if (!valuesAreSet()) {
			return isRow ? 180d : 90d;
		}
		return isRow ? _rightLng : _maxLat;
	}

	public Extent[] getIntersections(final Extent extent) {
		if (extent == null || !extent.valuesAreSet() || !valuesAreSet()) {
			return new Extent[] {};
		}
		final double minLat = Math.max(_minLat, extent._minLat);
		final double maxLat = Math.min(_maxLat, extent._maxLat);
		if (minLat > maxLat) {
			return new Extent[] {};
		}
		final double myLt = _leftLng;
		final double hisLt = extent._leftLng;
		final double myRt = _rightLng;
		final double hisRt = extent._rightLng;
		if (isAllLongitudes()) {
			return new Extent[] { new Extent(hisLt, minLat, hisRt, maxLat) };
		}
		if (extent.isAllLongitudes()) {
			return new Extent[] { new Extent(myLt, minLat, myRt, maxLat) };
		}
		final double myLtToMyRt = getLngRng();
		final double myLtToHisLt = LatLngArrayStatics.getInRange0_360(hisLt - myLt);
		final double myLtToHisRt = LatLngArrayStatics.getInRange0_360(hisRt - myLt);
		if (myLtToHisLt <= myLtToMyRt) {
			if (myLtToHisRt <= myLtToMyRt) {
				/** Both of his are inside of me. */
				if (myLtToHisLt <= myLtToHisRt) {
					/** Correct order. */
					return new Extent[] { new Extent(hisLt, minLat, hisRt, maxLat) };
				}
				/** Wrong order. */
				final Extent leftPart = new Extent(myLt, minLat, hisRt, maxLat);
				final Extent rightPart = new Extent(hisLt, minLat, myRt, maxLat);
				return new Extent[] { leftPart, rightPart };
			}
			return new Extent[] { new Extent(hisLt, minLat, myRt, maxLat) };
		}

		if (myLtToHisRt <= myLtToMyRt) {
			/** His right is in me, his left is not. */
			return new Extent[] { new Extent(myLt, minLat, hisRt, maxLat) };
		}

		/**
		 * Neither of his endpoints is in me. If one of mine is, both are, and they are
		 * in the wrong order.
		 */
		final double hisLtToHisRt = extent.getLngRng();
		final double hisLtToMyLt = LatLngArrayStatics.getInRange0_360(myLt - hisLt);
		final double hisLtToMyRt = LatLngArrayStatics.getInRange0_360(myRt - hisLt);
		if (hisLtToMyLt <= hisLtToHisRt || hisLtToMyRt <= hisLtToHisRt) {
			assert hisLtToMyLt <= hisLtToHisRt && hisLtToMyRt <= hisLtToHisRt
					: "Zero or one case here.  Something's wrong.";
			final Extent leftPart = new Extent(hisLt, minLat, myRt, maxLat);
			final Extent rightPart = new Extent(myLt, minLat, hisRt, maxLat);
			return new Extent[] { leftPart, rightPart };
		}
		return new Extent[] {};
	}

}
