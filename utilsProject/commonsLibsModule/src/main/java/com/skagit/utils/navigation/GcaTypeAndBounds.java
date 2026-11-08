package com.skagit.utils.navigation;

import java.util.Arrays;

import com.skagit.utils.MathX0;

public class GcaTypeAndBounds {

	enum GcaType {
		DEGENERATE, POLAR, CROSSES_IDL, REGULAR;
	}

	final public static double _AllLongitudesLeft = -180d;
	final public static double _AllLongitudesRight = 180d;
	final public static double _UnsetExtentValue = Double.NaN;

	final public GcaType _gcaType;
	final private double _normalX, _normalY, _normalZ;
	final private boolean _startsRight;
	final private boolean _startsLeft;
	final private boolean _startsUp;
	final private boolean _startsDown;
	final private double[] _llrh;
	final private double[] _interiorExtremeLatPointArray;

	public GcaTypeAndBounds(final double[] latLngArray0, final double[] latLngArray1, final double cLat0,
			final double cLat1) {
		final double lat0 = latLngArray0[0];
		final double lat1 = latLngArray1[0];
		final double simpleMinLat = Math.min(lat0, lat1);
		final double simpleMaxLat = Math.max(lat0, lat1);
		final double simpleLatDiff = simpleMaxLat - simpleMinLat;
		final double lng0 = latLngArray0[1];
		final double lng1 = latLngArray1[1];
		final double maxLng = Math.max(lng0, lng1);
		final double minLng = Math.min(lng0, lng1);
		final double leftLng, rightLng;
		if (minLng + 180d < maxLng) {
			leftLng = maxLng;
			rightLng = minLng;
		} else {
			leftLng = minLng;
			rightLng = maxLng;
		}
		final double lngDiff = maxLng - minLng;
		final boolean isDegenerate;
		if (Arrays.equals(latLngArray0, latLngArray1)) {
			isDegenerate = true;
		} else if (simpleLatDiff == 180d) {
			/** Degenerate; starts and ends at the poles. */
			isDegenerate = true;
		} else if (lngDiff == 180d) {
			isDegenerate = lat0 == -lat1;
		} else {
			isDegenerate = false;
		}
		if (isDegenerate) {
			_normalX = _normalY = _normalZ = Double.NaN;
			_gcaType = GcaType.DEGENERATE;
			_interiorExtremeLatPointArray = null;
			if (Arrays.equals(latLngArray0, latLngArray1)) {
				/** Equal points type of degenerate. */
				_llrh = new double[] { lng0, lat0, lng0, lat0 };
				_startsRight = _startsLeft = false;
				_startsUp = _startsDown = false;
				return;
			}
			/** 180 type of degenerate. */
			_startsRight = true;
			_startsLeft = false;
			if (simpleMaxLat == 90d) {
				_startsUp = false;
				_startsDown = true;
			} else if (simpleMinLat == -90d) {
				_startsUp = true;
				_startsDown = false;
			} else {
				_startsUp = _startsDown = false;
			}
			_llrh = new double[] { _AllLongitudesLeft, simpleMinLat, _AllLongitudesRight, simpleMaxLat };
			return;
		}

		/**
		 * It's polar if it goes through a pole; detected by a lngDiff of 180. Note that
		 * that means that a GCA that starts or ends at a pole is not polar.
		 */
		if (lngDiff == 180d) {
			_gcaType = GcaType.POLAR;
			final double sumOfLats = lat0 + lat1;
			final boolean goesThroughNorthPole = sumOfLats > 0d;
			final double minLat, maxLat;
			if (goesThroughNorthPole) {
				minLat = simpleMinLat;
				maxLat = 90d;
				_interiorExtremeLatPointArray = LatLngArrayStatics.normalizeLatLngArray(new double[] { 90d, 0d });
			} else {
				minLat = -90d;
				maxLat = simpleMaxLat;
				_interiorExtremeLatPointArray = LatLngArrayStatics.normalizeLatLngArray(new double[] { -90d, 0d });
			}
			_llrh = new double[] { _AllLongitudesLeft, minLat, _AllLongitudesRight, maxLat };
			_startsRight = _startsLeft = false;
			_startsUp = goesThroughNorthPole;
			_startsDown = !_startsUp;
			final double lngOfNormal;
			if (_startsUp) {
				lngOfNormal = lng0 - 90d;
			} else {
				lngOfNormal = lng0 + 90d;
			}
			final double lngOfNormalR = Math.toRadians(lngOfNormal);
			_normalX = MathX0.cosX(lngOfNormalR);
			_normalY = MathX0.sinX(lngOfNormalR);
			_normalZ = 0d;
			return;
		}

		if (lng0 == lng1) {
			/** Routine meridional including those that start or end at a pole. */
			_gcaType = GcaType.REGULAR;
			_llrh = new double[] { lng0, simpleMinLat, lng0, simpleMaxLat };
			_startsRight = _startsLeft = false;
			_startsUp = lat1 > lat0;
			_startsDown = !_startsUp;
			final double lngOfNormal;
			if (_startsUp) {
				lngOfNormal = lng0 - 90d;
			} else {
				lngOfNormal = lng0 + 90d;
			}
			final double lngOfNormalR = Math.toRadians(lngOfNormal);
			_normalX = MathX0.cosX(lngOfNormalR);
			_normalY = MathX0.sinX(lngOfNormalR);
			_normalZ = 0d;
			_interiorExtremeLatPointArray = null;
			return;
		}

		final boolean crossesIdl = lngDiff > 180d;

		/** Set startsRight and startsLeft. */
		_startsRight = LatLngArrayStatics.getInRange180_180(lng1 - lng0) > 0;
		_startsLeft = !_startsRight;

		/** Equatorial Gcas. */
		if (lat0 == lat1 && lat0 == 0d) {
			_llrh = new double[] { leftLng, 0d, rightLng, 0d };
			_startsUp = _startsDown = false;
			_gcaType = crossesIdl ? GcaType.CROSSES_IDL : GcaType.REGULAR;
			_normalX = _normalY = 0d;
			_normalZ = _startsRight ? 1d : -1d;
			_interiorExtremeLatPointArray = null;
			return;
		}

		/** Real work. */
		final double[] normal = LatLngArrayStatics.crossProduct(latLngArray0, latLngArray1, cLat0, cLat1);
		/** normal gets normalized in the following. */
		final double[] latLngNormal = LatLngArrayStatics.toLatLngArrayX(normal);
		_normalX = normal[0];
		_normalY = normal[1];
		_normalZ = normal[2];

		final double latOfNormal = latLngNormal[0];
		final double lngOfNormal = latLngNormal[1];
		/**
		 * FWIW, the normal will be in the northern hemisphere if the gca goes west to
		 * east, the southern hemisphere if the gca goes east to west, and the equator
		 * if the gca is on a meridian.
		 */
		final double maxLatGcZ;
		final double minLatGcZ;
		final double lngMaxLatGc, lngMinLatGc;
		if (latOfNormal > 0d) {
			maxLatGcZ = 90d - latOfNormal;
			minLatGcZ = -maxLatGcZ;
			lngMaxLatGc = lngOfNormal + 180d;
			lngMinLatGc = lngOfNormal;
		} else {
			minLatGcZ = -(latOfNormal + 90d);
			maxLatGcZ = -minLatGcZ;
			lngMinLatGc = lngOfNormal + 180d;
			lngMaxLatGc = lngOfNormal;
		}
		final double minLatGc = LatLngArrayStatics.getInRange180_180(minLatGcZ);
		final double maxLatGc = LatLngArrayStatics.getInRange180_180(maxLatGcZ);

		final double lngFromZeroToOne = LatLngArrayStatics.getInRange180_180(lng1 - lng0);
		final double lngW, lngE;
		if (lngFromZeroToOne > 0d) {
			lngW = lng0;
			lngE = lng1;
		} else {
			lngW = lng1;
			lngE = lng0;
		}

		final double lngFromWToE = LatLngArrayStatics.getInRange0_360(lngE - lngW);
		final double lngFromWToMaxLatGc = LatLngArrayStatics.getInRange0_360(lngMaxLatGc - lngW);
		if (lngFromWToMaxLatGc < lngFromWToE) {
			_interiorExtremeLatPointArray = LatLngArrayStatics
					.normalizeLatLngArray(new double[] { maxLatGc, lngMaxLatGc });
			final double[] llrh = correctLlrh(lngW, simpleMinLat, lngE, _interiorExtremeLatPointArray[0]);
			final double minLat = llrh[1];
			final double maxLat = llrh[3];
			/**
			 * Both ends have to go up to maxLatOfGc. Hence, we start up (and, btw, end
			 * going down).
			 */
			_startsUp = true;
			_startsDown = false;
			_gcaType = crossesIdl ? GcaType.CROSSES_IDL : GcaType.REGULAR;
			_llrh = new double[] { leftLng, minLat, rightLng, maxLat };
			return;
		}
		final double lngFromWToMinLowGc = LatLngArrayStatics.getInRange0_360(lngMinLatGc - lngW);
		if (lngFromWToMinLowGc < lngFromWToE) {
			_interiorExtremeLatPointArray = LatLngArrayStatics
					.normalizeLatLngArray(new double[] { minLatGc, lngMinLatGc });
			final double[] llrh = correctLlrh(lngW, _interiorExtremeLatPointArray[0], lngE, simpleMaxLat);
			final double minLat = llrh[1];
			final double maxLat = llrh[3];
			/**
			 * Both sides have to go down to minLatOfGc. Hence, we start down (and, btw, end
			 * going up).
			 */
			_startsUp = false;
			_startsDown = true;
			_gcaType = crossesIdl ? GcaType.CROSSES_IDL : GcaType.REGULAR;
			_llrh = new double[] { leftLng, minLat, rightLng, maxLat };
			return;
		}
		_interiorExtremeLatPointArray = null;
		_startsUp = lat0 == simpleMinLat;
		_startsDown = !_startsUp;
		_llrh = new double[] { leftLng, simpleMinLat, rightLng, simpleMaxLat };
		_gcaType = crossesIdl ? GcaType.CROSSES_IDL : GcaType.REGULAR;
	}

	public static double[] correctLlrh(double leftLng, double minLat, double rightLng, double maxLat) {
		if (!Double.isFinite(leftLng) || !Double.isFinite(minLat) || !Double.isFinite(rightLng)
				|| !Double.isFinite(maxLat)) {
			return new double[] { _UnsetExtentValue, _UnsetExtentValue, _UnsetExtentValue, _UnsetExtentValue };
		}
		if (minLat > maxLat) {
			final double lat = minLat;
			minLat = maxLat;
			maxLat = lat;
		}
		minLat = LatLngArrayStatics.getInRange180_180(minLat);
		maxLat = LatLngArrayStatics.getInRange180_180(maxLat);
		if (maxLat >= 90d || minLat <= -90d || isAllLongitudes(leftLng, rightLng)) {
			leftLng = _AllLongitudesLeft;
			rightLng = _AllLongitudesRight;
		} else {
			leftLng = LatLngArrayStatics.getInRange180_180(leftLng);
			rightLng = LatLngArrayStatics.getInRange180_180(rightLng);
		}
		return new double[] { leftLng, minLat, rightLng, maxLat };
	}

	public boolean latWindowContains(final double lat0) {
		return _llrh[1] <= lat0 && lat0 <= _llrh[3];
	}

	private static boolean ltRtContains(final double lt, final double rt, final double lng) {
		if (isAllLongitudes(lt, rt)) {
			return true;
		}
		if (lt <= rt) {
			return lt <= lng && lng <= rt;
		}
		return lng >= lt || lng <= rt;
	}

	public static boolean lngWindowContains(final double lng0, final double lng1, final double lng) {
		final double[] ltRt = getLtRt(lng0, lng1);
		return ltRtContains(ltRt[0], ltRt[1], lng);
	}

	private static double[] getLtRt(final double lng0, final double lng1) {
		if (lng0 <= lng1) {
			if (lng0 + 180d > lng1) {
				return new double[] { lng0, lng1 };
			}
			return new double[] { lng1, lng0 };
		}
		if (lng1 + 180d > lng0) {
			return new double[] { lng1, lng0 };
		}
		return new double[] { lng0, lng1 };
	}

	public boolean lngWindowContains(final double lng) {
		return ltRtContains(_llrh[0], _llrh[2], lng);
	}

	public double degsToLatWindow(final double lat) {
		if (latWindowContains(lat)) {
			return 0d;
		}
		final double minLat = _llrh[1];
		if (lat < minLat) {
			return minLat - lat;
		}
		final double maxLat = _llrh[3];
		return lat - maxLat;
	}

	public static boolean isAllLongitudes(final double left, final double right) {
		if (LatLngArrayStatics.getInRange0_360(right - left) != 0d) {
			return false;
		}
		return left != right;
	}

	public boolean startsRight() {
		return _startsRight;
	}

	public boolean startsLeft() {
		return _startsLeft;
	}

	public boolean startsUp() {
		return _startsUp;
	}

	public boolean startsDown() {
		return _startsDown;
	}

	public double[] getLlrh() {
		return _llrh;
	}

	public boolean goesUpAndDown() {
		return _interiorExtremeLatPointArray != null;
	}

	public double[] getNormal() {
		return new double[] { _normalX, _normalY, _normalZ };
	}

	public double[] getInteriorExtremeLatPoint() {
		return _interiorExtremeLatPointArray;
	}

}