package com.skagit.utils.latLng;

import java.util.Arrays;

import com.skagit.utils.Constants0;
import com.skagit.utils.MathX0;
import com.skagit.utils.navigation.LatLngArrayStatics;

public class GreatCircleCalculator extends NavigationCalculator {

	final private double[] _scLat0;
	final private double _nmiToR;

	public static double getNmiToR(final double[] latLngArray0, final double[] latLngArray1) {
		final double lat0 = latLngArray0[0];
		final double lat1 = latLngArray1[0];
		final double minLat = Math.min(lat0, lat1);
		final double maxLat = Math.max(lat0, lat1);
		final double lng0 = latLngArray0[1];
		final double lng1 = latLngArray1[1];
		final double lngRng = Math.abs(LatLngArrayStatics.degsToEast180_180(lng0, lng1));
		final double[] latAndRadiusR = getLatAndRadiusR(minLat, maxLat, lngRng / 2d);
		final double midLat = latAndRadiusR[0];
		return Constants0.Functions.nmiToR(midLat);
	}

	protected GreatCircleCalculator(final double[] latLngArray0, final long secs0, final double kts,
			final double initialHdg) {
		this(Constants0.Functions.nmiToR(latLngArray0[0]), latLngArray0, secs0, kts, initialHdg);
	}

	public GreatCircleCalculator(final double nmiToR, final double[] latLngArray0, final long secs0, final double kts,
			final double initialHdg) {
		super(latLngArray0, secs0, kts, initialHdg);
		final double lat0 = latLngArray0[0];
		final double lat0R = Math.toRadians(lat0);
		_scLat0 = new double[] { MathX0.sinX(lat0R), MathX0.cosX(lat0R) };
		_nmiToR = nmiToR;
	}

	public GreatCircleCalculator(final double[] latLngArray0, final long secs0, final double kts,
			final double[] latLngArray1) {
		this(getNmiToR(latLngArray0, latLngArray1), latLngArray0, secs0,
				Arrays.equals(latLngArray0, latLngArray1) ? 0d : kts,
				LatLngArrayStatics.initialHdgX(latLngArray0, latLngArray1, false));
	}

	public double getNmiToR(final double[] latLngArray1) {
		if (Double.isFinite(_nmiToR)) {
			return _nmiToR;
		}

	}

	@Override
	public double getNmiFromLatLng0(final double[] latLngArray1) {
		return 0d;
	}

	@Override
	public double[] getPosition(final long secs) {
		if (_kts == 0d) {
			return _latLngArray0;
		}
		final long elapsedSecs = secs - _secs0;
		final double nmi = _kts * elapsedSecs / 3600d;
		final double nmiToR = getNmiToR(/* latLngArray1= */null);
		final double haversine = nmi * nmiToR;
		return LatLngArrayStatics.getLatLngArrayX(_latLngArray0, _scLat0, haversine, _initialHdg);
	}

	/**
	 * Returns the latitude and the radius in earthRadii of the smallest circle that
	 * contains the LatLngs (minLat, -halfLngRng), (minLat, halfLngRng), (maxLat,
	 * -halfLngRng), and (maxLat, halfLngRng).
	 */
	public static double[] getLatAndRadiusR(final double minLat, final double maxLat, final double halfLngRng) {
		if (halfLngRng >= 90d) {
			/**
			 * We'll be at one pole or the other; pick the one that's closer to its distant
			 * latitude.
			 */
			if (90d - minLat < maxLat + 90d) {
				return new double[] { 90d, Math.toRadians(90d - minLat) };
			}
			return new double[] { -90d, Math.toRadians(maxLat + 90d) };
		}
		/**
		 * There are 2 candidates, both on 0 longitude. The first is where the diagonal
		 * crosses 0 longitude. The second is where the distance to the northern vertex
		 * is the same as the distance to the southern vertex. For each of them, we will
		 * make sure radius is big enough to contain all vertices. This would not be
		 * sufficient except that we already dispensed with the case of a halfLngRng >
		 * 90d.
		 */
		/** Where does the diagonal cross 0 longitude? */
		final double[] loLfArray = { minLat, -halfLngRng };
		final double[] hiRtArray = { maxLat, halfLngRng };
		final double loLatR = Math.toRadians(minLat);
		final double cLoLat = MathX0.cosX(loLatR);
		final double sLoLat = MathX0.sinX(loLatR);
		final double hiLatR = Math.toRadians(maxLat);
		final double cHiLat = MathX0.cosX(hiLatR);
		final double sHiLat = MathX0.sinX(hiLatR);
		final double[] normalXyz = LatLngArrayStatics.crossProductX(loLfArray, hiRtArray, cLoLat, cHiLat);
		final double lat0 = Math.toDegrees(MathX0.atan2X(-normalXyz[0], normalXyz[2]));
		/** We need the y-coordinate to be 0. */
		final double[] latLngArray0 = { lat0, 0d };
		final double cLat0 = MathX0.cosX(Math.toRadians(lat0));
		final double d00R = LatLngArrayStatics.haversineX(latLngArray0, loLfArray, cLat0, cLoLat);
		final double d01R = LatLngArrayStatics.haversineX(latLngArray0, hiRtArray, cLat0, cLoLat);
		final double d0R = Math.max(d00R, d01R);
		/** Second candidate is where on the spine, we're equidistant. */
		final double halfLngRngR = Math.toRadians(halfLngRng);
		final double cHalfLngRng = MathX0.cosX(halfLngRngR);
		final double cCoef = cHalfLngRng * (cLoLat - cHiLat);
		final double sCoef = sLoLat - sHiLat;
		final double lat1A = Math.toDegrees(MathX0.atan2X(cCoef, -sCoef));
		final double lat1;
		if (lat1A < -90d) {
			lat1 = lat1A + 180d;
		} else if (lat1A > 90d) {
			lat1 = lat1A - 180d;
		} else {
			lat1 = lat1A;
		}
		final double[] latLngArray1 = { lat1, 0d };
		final double cLat1 = MathX0.cosX(Math.toRadians(lat1));
		final double d10R = LatLngArrayStatics.haversineX(latLngArray1, loLfArray, cLat1, cLoLat);
		final double d11R = LatLngArrayStatics.haversineX(latLngArray1, hiRtArray, cLat1, cHiLat);
		final double d1R = (d10R + d11R) / 2d;
		final double lat;
		final double dR;
		if (d0R <= d1R) {
			lat = lat0;
			dR = d0R;
		} else {
			lat = lat1;
			dR = d1R;
		}
		return new double[] { lat, dR };
	}

	public static double[] getFirstLatLngWithinTolerance(final LatLng latLng0, final double hdg, final LatLng target,
			final double toleranceNmi) {
		final double[] latLngArray0 = latLng0.getLatLngArray();
		final double[] latLngArrayTgt = target.getLatLngArray();
		final double nmiToR = getNmiToR(latLngArray0, latLngArrayTgt);
		final double nmiAtStart = GreatCircleCalculator.getNmi(latLng0, target);
		if (nmiAtStart <= toleranceNmi) {
			return latLng0;
		}
		final LatLng partner = GreatCircleCalculator.getLatLng(latLng0, hdg, /* nmi= */Constants0._PiOver2 / _NmiToR);
		final double[] startXyz = latLng0.getXyz();
		final double[] partnerXyz = partner.getXyz();
		final double targetTheta = toleranceNmi * _NmiToR;
		final double targetCosine = MathX0.cosX(targetTheta);
		final double[] targetXyz = target.getXyz();
		/**
		 * We need targetXyz dot (c * startXyz + s * partnerXyz) = targetCosine.
		 */
		final double t0 = targetXyz[0];
		final double t1 = targetXyz[1];
		final double t2 = targetXyz[2];
		final double start0 = startXyz[0];
		final double start1 = startXyz[1];
		final double start2 = startXyz[2];
		final double partner0 = partnerXyz[0];
		final double partner1 = partnerXyz[1];
		final double partner2 = partnerXyz[2];
		final double xi = t0 * start0 + t1 * start1 + t2 * start2;
		final double eta = t0 * partner0 + t1 * partner1 + t2 * partner2;
		final double[] thetaValues = LatLng.useWojtowicz(xi, eta, targetCosine);
		if (thetaValues == null || thetaValues.length == 0 || Double.isNaN(thetaValues[0])) {
			return null;
		}
		final double theta = thetaValues[0];
		final double c = MathX0.cosX(theta);
		final double s = MathX0.sinX(theta);
		final double[] cartesian = { c * start0 + s * partner0, //
				c * start1 + s * partner1, //
				c * start2 + s * partner2 };
		return LatLng.getLatLngC3(cartesian);
	}
}