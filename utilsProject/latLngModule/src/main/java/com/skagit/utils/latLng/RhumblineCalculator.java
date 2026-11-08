package com.skagit.utils.latLng;

import com.skagit.utils.Constants0;
import com.skagit.utils.MathX0;
import com.skagit.utils.navigation.LatLngArrayStatics;

/**
 * Key formulae, as per page 107... of Elements, Modules, and Algorithms.
 *
 * <pre>
 * tan(heading) = deltaLng/deltaMeridionalParts.
 */
public class RhumblineCalculator extends NavigationCalculator {
	/** The value compensates for the non-spherical nature of the earth. */
	static final private double _E = 0.081819191;
	/** Other static fields are functions of _E. */
	static final private double _E2 = _E * _E;
	static final private double _E4OverThree = _E2 * _E2 / 3;
	static final private double _E6OverFive = _E2 * _E2 * _E2 / 5;
	/** Defines "close to" for going east or west. */
	static final private double _EpsilonR = Math.toRadians(0.0001);
	/** Meridional parts of starting latitude. */
	final private double _meridionalParts0;
	final private double _nmiToR;

	private static double getMeridionalParts(final double latR) {
		/** latR is in [-pi/2,pi/2). */
		final double sLat = MathX0.sinX(latR);
		final double sLat2 = sLat * sLat;
		final double sLat3 = sLat * sLat2;
		final double sLat5 = sLat3 * sLat2;
		return Math.log(MathX0.tanX((Constants0._PiOver2 + latR) / 2))
				- (_E2 * sLat + _E4OverThree * sLat3 + _E6OverFive * sLat5);
	}

	public static double getInitialHdg(final double[] latLngArray0, final double[] latLngArray1) {
		final double lat0 = latLngArray0[0];
		final double lat1 = latLngArray1[0];
		final double lat0R = Math.toRadians(lat0);
		final double lat1R = Math.toRadians(lat1);
		final double deltaLatRads = lat1R - lat0R;
		final double meridionalParts0 = getMeridionalParts(lat0R);
		final double lng0 = latLngArray0[1];
		final double lng1 = latLngArray1[1];
		final double deltaLng = LatLngArrayStatics.degsToEast180_180(lng0, lng1);
		final double deltaLngR = Math.toRadians(deltaLng);
		final double initialHdg;
		if (deltaLatRads > _EpsilonR || deltaLatRads < -_EpsilonR) {
			final double destMeridionalParts = getMeridionalParts(lat1R);
			final double deltaMeridionalParts = destMeridionalParts - meridionalParts0;
			initialHdg = LatLngArrayStatics
					.getInRange0_360(Math.toDegrees(MathX0.atan2X(deltaLngR, deltaMeridionalParts)));
		} else {
			/** Same parallel; either east or west. */
			initialHdg = deltaLng > 0d ? 90d : 270d;
		}
		return initialHdg;
	}

	protected RhumblineCalculator(final double[] latLngArray0, final long secs0, final double kts,
			final double[] latLngArray1) {
		super(latLngArray0, secs0, kts, getInitialHdg(latLngArray0, latLngArray1));
		final double lat0 = latLngArray0[0];
		final double lat1 = latLngArray1[0];
		_nmiToR = Constants0.Functions.nmiToR((lat0 + lat1) / 2d);
		_meridionalParts0 = getMeridionalParts(Math.toRadians(lat0));
	}

	protected RhumblineCalculator(final double[] latLngArray0, final long secs0, final double kts,
			final double initialHdg) {
		super(latLngArray0, secs0, kts, initialHdg);
		final double lat = latLngArray0[0];
		_meridionalParts0 = getMeridionalParts(Math.toRadians(lat));
		_nmiToR = Constants0.Functions.nmiToR(lat);
	}

	@Override
	public double[] getPosition(final long secs) {
		final long elapsedSecs = secs - _secs0;
		final double lat0 = _latLngArray0[0];
		final double lat0R = Math.toRadians(lat0);
		final double d = _kts / 3600d * elapsedSecs * _nmiToR;
		final double radsCwFromNorth = Math.toRadians(_initialHdg);
		final double p = d * MathX0.sinX(radsCwFromNorth);
		final double deltaLatR = d * MathX0.cosX(radsCwFromNorth);
		final double newLatR = lat0R + deltaLatR;
		if (newLatR >= Constants0._PiOver2) {
			return new double[] { 90d, 0d };
		}
		if (newLatR <= -Constants0._PiOver2) {
			return new double[] { -90d, 0d };
		}
		final double deltaLngR;
		if (deltaLatR > _EpsilonR || deltaLatR < -_EpsilonR) {
			final double destMeridionalParts = getMeridionalParts(newLatR);
			final double deltaMeridionalParts = destMeridionalParts - _meridionalParts0;
			deltaLngR = deltaMeridionalParts * p / deltaLatR;
		} else /** We're running along a parallel for length d. */
		if (_initialHdg < 180d) {
			/** We're going east. */
			deltaLngR = d / MathX0.cosX(lat0R);
		} else {
			/** We're going west. */
			deltaLngR = -d / MathX0.cosX(lat0R);
		}
		final double startLng = _latLngArray0[1];
		final double deltaLng = Math.toDegrees(deltaLngR);
		final double newLng = startLng + deltaLng;
		final double newLat = Math.toDegrees(newLatR);
		return new double[] { newLat, newLng };
	}

	@Override
	public double getNmiFromLatLng0(final double[] latLngArray1) {
		final double lat0R = Math.toRadians(_latLngArray0[0]);
		final double lat1R = Math.toRadians(latLngArray1[0]);
		final double deltaLatR = lat1R - lat0R;
		final double lng0 = _latLngArray0[1];
		final double lng1 = latLngArray1[1];
		final double deltaLng = LatLngArrayStatics.degsToEast180_180(lng0, lng1);
		final double deltaLngR = Math.toRadians(deltaLng);
		if (deltaLatR > _EpsilonR || deltaLatR < -_EpsilonR) {
			final double meridionalParts1 = getMeridionalParts(lat1R);
			final double deltaMeridionalParts = meridionalParts1 - _meridionalParts0;
			final double p = deltaLatR * deltaLngR / deltaMeridionalParts;
			return Math.sqrt(deltaLatR * deltaLatR + p * p) / _nmiToR;
		}
		return Math.abs(deltaLngR) * MathX0.cosX(lat0R) / _nmiToR;
	}

}
