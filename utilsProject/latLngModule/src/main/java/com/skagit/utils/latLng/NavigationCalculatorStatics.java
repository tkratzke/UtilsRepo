package com.skagit.utils.latLng;

import com.skagit.utils.Constants0;
import com.skagit.utils.MathX0;
import com.skagit.utils.navigation.LatLngArrayStatics;

public class NavigationCalculatorStatics {

	public static NavigationCalculator build(final double[] latLngArray0, final long secs0, final double kts,
			final double initialHdg, final MotionType motionType) {
		if (motionType == MotionType.GREAT_CIRCLE) {
			return new GreatCircleCalculator(latLngArray0, secs0, kts, initialHdg);
		}
		if (motionType == MotionType.RHUMBLINE) {
			return new RhumblineCalculator(latLngArray0, secs0, kts, initialHdg);
		}
		if (motionType == MotionType.TANGENT_CYLINDER) {
			return new TangentCylinderCalculator(latLngArray0, secs0, kts, latLng1);
		}
		if (motionType == MotionType.MID_LAT) {
			return new MidLatCalculator(latLngArray0, secs0, kts, initialHdg);
		}
		return null;
	}

	/**
	 * Use this routine to build a NavigationCalculator if you know where and when
	 * the path started, and where and when it ended.
	 * <p>
	 *
	 * @param latLngArray0 Where to start.
	 * @param secs0        When to start.
	 * @param latLngArray1 Where to go.
	 * @param travelSecs   The time it took to get to latLngArray1. Be careful! This
	 *                     is not the "endtime."
	 * @param motionType   Do we follow a great circle, a rhumbline, etc..
	 * @return A NavigationCalculator that can be used to answer the basic questions
	 *         that a NavigationCaculator answers.
	 */
	public static NavigationCalculator buildWithSeconds(final double[] latLngArray0, final long secs0,
			final long travelSecs, double[] latLngArray1, final MotionType motionType) {
		/**
		 * Find the distance by using the other build routine. Note the 1.0
		 * distinguishes the auxiliary calculator from the one we are trying to build;
		 * the 1.0 is speedInKts.
		 */
		final double speedInKtsIsOne = 1d;
		final NavigationCalculator navCalc = build(latLngArray0, secs0, latLngArray1, /* kts= */1d, motionType);
		final double distanceInNmi = navCalc.getNmiFromLatLng0(latLngArray1);
		double speedInKts;
		if (travelSecs == 0) {
			latLngArray1 = latLngArray0;
			speedInKts = 0d;
		} else {
			speedInKts = distanceInNmi / travelSecs * 3600d;
		}
		return build(latLngArray0, secs0, latLngArray1, speedInKts, motionType);
	}

	/**
	 * If findRangeHdg is false, it returns (nmi,hdg). If findRangeHdg is false, the
	 * inputs are (lat0,lng0, nmi,hdg,...)
	 */
	public static double[] answerBasicQuestion(final double lat0, final double lng0, final double lat1OrNmi,
			final double lng1OrHdg, final boolean findRangeHdg, final String motionTypeId) {
		final MotionType motionType = MotionType.get(motionTypeId);
		final double[] latLngArray0 = { lat0, lng0 };
		final double lat0R = Math.toRadians(lat0);
		final double cLat0 = MathX0.cosX(lat0R);
		final double sLat0 = MathX0.sinX(lat0R);
		final double[] scLat0 = { sLat0, cLat0 };
		final double lat1;
		final double lng1;
		final double nmiIn;
		final double hdgIn;
		final double[] latLngArray1;
		final double lat1R;
		final double cLat1;
		final double sLat1;
		final double[] scLat1;
		if (findRangeHdg) {
			lat1 = lat1OrNmi;
			lng1 = lng1OrHdg;
			nmiIn = hdgIn = Double.NaN;
			latLngArray1 = new double[] { lat1, lng1 };
			lat1R = Math.toRadians(lat1);
			cLat1 = MathX0.cosX(lat1R);
			sLat1 = MathX0.sinX(lat1R);
			scLat1 = new double[] { sLat1, cLat1 };
		} else {
			lat1 = lng1 = Double.NaN;
			nmiIn = lat1OrNmi;
			hdgIn = lng1OrHdg;
			latLngArray1 = null;
			lat1R = Double.NaN;
			cLat1 = Double.NaN;
			sLat1 = Double.NaN;
			scLat1 = null;
		}
		if (motionType == MotionType.GREAT_CIRCLE) {
			if (findRangeHdg) {
				final double haversine = LatLngArrayStatics.haversineX(latLngArray0, latLngArray1, cLat0, cLat1);
				final double hdg = LatLngArrayStatics.initialHdgX(latLngArray0, latLngArray1, scLat0, scLat1,
						/* returnRads= */false);
				final Extent extent = new Extent(latLngArray0, latLngArray1);
				final Extent.SmallCircle smallCircle = new Extent.SmallCircle(extent);
				final double nmiToR = Constants0.Functions.nmiToR(smallCircle._centerLatLngArray[0]);
				final double nmi = haversine / nmiToR;
				return new double[] { nmi, hdg };
			}
			final double nmiToR = Constants0.Functions.nmiToR(latLngArray0[0]);
			final double haversine = nmiIn * nmiToR;
			return LatLngArrayStatics.getLatLngArrayX(latLngArray0, scLat0, haversine, hdgIn);
		}
		/** Not GC. */
		switch (motionType) {
		case MID_LAT:
		case RHUMBLINE:
		case TANGENT_CYLINDER:
			final long secs0 = 0L;
			if (findRangeHdg) {
				final NavigationCalculator navCalc = build(latLngArray0, secs0, /* kts= */1d, latLngArray1, motionType);
				final double nmi = navCalc.getNmiFromLatLng0(latLngArray1);
				final double hdg = navCalc.getInitialHdg();
				return new double[] { nmi, hdg };
			}
			final double hdg = lng1OrHdg;
			final double kts = lat1OrNmi;
			final NavigationCalculator navCalc = build(latLngArray0, secs0, kts, hdg, motionType);
			final LatLng latLng1 = navCalc.getPosition(3600L);
			return latLng1.getLatLngArray();
		case GREAT_CIRCLE:
		default:
			return null;
		}
	}
}
