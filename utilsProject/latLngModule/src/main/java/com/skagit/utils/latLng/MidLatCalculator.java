package com.skagit.utils.latLng;

import com.skagit.utils.Constants0;
import com.skagit.utils.MathX0;
import com.skagit.utils.NumericalRoutines;
import com.skagit.utils.navigation.LatLngArrayStatics;

/**
 * Uses a simple 2-d coordinate system. Conversion between length in the x
 * direction to length in the y direction is based on a latitude. The scaling
 * factor is cos(latitude). If the destination is given, the average of start
 * and destination yields the latitude. If destination is not given, we simply
 * use the start latitude.
 */
public class MidLatCalculator extends NavigationCalculator {
	final private double _deltaXNmiPerSec, _deltaYNmiPerSec;
	final private double _cosMidLat;

	public static double getInitialHdg(final double[] latLngArray0, final double[] latLngArray1) {
		final double lat0 = latLngArray0[0];
		final double lng0 = latLngArray0[1];
		final double lat1 = latLngArray1[0];
		final double lng1 = latLngArray1[1];
		final double deltaLat = (lat1 - lat0);
		final double deltaLng = LatLngArrayStatics.degsToEast180_180(lng0, lng1);
		final double midLat = (lat0 + lat1) / 2d;
		final double cosMidLat = MathX0.cosX(Math.toRadians(midLat));
		return LatLngArrayStatics
				.getInRange0_360(Math.toDegrees(Constants0._PiOver2 - MathX0.atan2X(deltaLat, deltaLng * cosMidLat)));
	}

	public MidLatCalculator(final double[] latLngArray0, final long secs0, final double kts,
			final double[] latLngArray1) {
		super(latLngArray0, secs0, kts, getInitialHdg(latLngArray0, latLngArray1));
		final double lat0 = latLngArray0[0];
		final double lng0 = latLngArray0[1];
		final double lat1 = latLngArray1[0];
		final double lng1 = latLngArray1[1];
		final double deltaLat = (lat1 - lat0);
		final double deltaLng = LatLngArrayStatics.degsToEast180_180(lng0, lng1);
		final double midLat = (lat0 + lat1) / 2d;
		_cosMidLat = MathX0.cosX(Math.toRadians(midLat));
		final double deltaYNmi = deltaLat * 60d;
		final double deltaXNmi = deltaLng * 60d * _cosMidLat;
		final double nmi = Math.sqrt(deltaYNmi * deltaYNmi + deltaXNmi * deltaXNmi);
		if (NumericalRoutines.compare(nmi, 0d) == 0) {
			_deltaXNmiPerSec = _deltaYNmiPerSec = 0d;
		} else {
			final double nmiPerSec = kts / 3600d;
			_deltaXNmiPerSec = nmiPerSec / nmi * deltaXNmi;
			_deltaYNmiPerSec = nmiPerSec / nmi * deltaYNmi;
		}
	}

	public MidLatCalculator(final double[] latLngArray0, final long secs0, final double kts, final double initialHdg) {
		super(latLngArray0, secs0, kts, initialHdg);
		final double lat0 = latLngArray0[0];
		_cosMidLat = MathX0.cosX(Math.toRadians(lat0));
		final double nmiPerSec = kts / 3600d;
		final double courseCcwRadsFromEast = Math.toRadians(90d - initialHdg);
		_deltaXNmiPerSec = nmiPerSec * MathX0.cosX(courseCcwRadsFromEast);
		_deltaYNmiPerSec = nmiPerSec * MathX0.sinX(courseCcwRadsFromEast);
	}

	@Override
	public double[] getPosition(final long secs) {
		final double elapsedSeconds = secs - _secs0;
		final double lat = _latLngArray0[0] + elapsedSeconds * _deltaYNmiPerSec / 60d;
		final double lng = _latLngArray0[1] + elapsedSeconds * _deltaXNmiPerSec / (60d * _cosMidLat);
		return new double[] { lat, lng };
	}

	@Override
	public double getNmiFromLatLng0(final double[] latLngArray1) {
		final double deltaXInNmi = (latLngArray1[1] - _latLngArray0[1]) * 60d * _cosMidLat;
		final double deltaYInNmi = 60d * (latLngArray1[0] - _latLngArray0[1]);
		return Math.sqrt(deltaXInNmi * deltaXInNmi + deltaYInNmi * deltaYInNmi);
	}

}
