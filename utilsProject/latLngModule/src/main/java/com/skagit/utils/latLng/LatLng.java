package com.skagit.utils.latLng;

import java.util.Arrays;
import java.util.Comparator;

import com.skagit.utils.NumericalRoutines;
import com.skagit.utils.navigation.LatLngArrayStatics;
import com.skagit.utils.randomx.Randomx;

public class LatLng implements Cloneable {

	final private double[] _latLngArray;
	final private double[] _xyz;
	final private double[] _scLat, _scLng;

	@Override
	public LatLng clone() {
		return new LatLng(_latLngArray, /* isXyz= */false);
	}

	public LatLng(final double lat, final double lng) {
		this(new double[] { lat, lng }, /* isXyz= */false);
	}

	public LatLng(final double[] array, final boolean isXyz) {
		if (isXyz) {
			_xyz = array;
			_latLngArray = LatLngArrayStatics.toLatLngArrayX(_xyz);
		} else {
			_latLngArray = array;
			_xyz = LatLngArrayStatics.toCartesian(array);
		}
		LatLngArrayStatics.normalizeLatLngArray(_latLngArray);
		final double sLat = _xyz[2];
		final double cLat = Math.sqrt(1 - sLat * sLat);
		_scLat = new double[] { sLat, cLat };
		_scLng = new double[] { _xyz[1], _xyz[0] };
		NumericalRoutines.convertToUnitLength(_scLng);
	}

	public LatLng(final String latLngText) {
		this(LatLngArrayStatics.getLatLngArray(latLngText), /* isXyz= */false);
	}

	public LatLng(final Randomx r) {
		this(new double[] { 180d * r.nextDouble(), 360d * r.nextDouble() }, /* isXyz= */false);
	}

	/** Critical accessors. */
	final public double[] getLatLngArray() {
		return _latLngArray;
	}

	final public double getLat() {
		return _latLngArray[0];
	}

	final public double getLng() {
		return _latLngArray[1];
	}

	final public double[] getXyz() {
		return _xyz;
	}

	final public double[] getScLat() {
		return _scLat;
	}

	final public double[] getScLng() {
		return _scLng;
	}

	/** Accessors. */
	public boolean latLngAreValid() {
		return !Double.isNaN(getLat()) && !Double.isNaN(getLng());
	}

	/** Comparison routines. */
	public static Comparator<? super LatLng> _ByLatThenLng = new Comparator<>() {
		@Override
		public int compare(final LatLng latLng0, final LatLng latLng1) {
			if (latLng0 == latLng1) {
				return 0;
			}
			if ((latLng0 == null) != (latLng1 == null)) {
				return latLng0 == null ? -1 : 1;
			}
			if (latLng0 == null) {
				return 0;
			}
			/** They're not the same and neither is null. */
			final double lat0 = latLng0.getLat(), lat1 = latLng1.getLat();
			if (lat0 != lat1) {
				return lat0 < lat1 ? -1 : 1;
			}
			final double lng0 = latLng0.getLng(), lng1 = latLng1.getLng();
			if (lng0 != lng1) {
				return lng0 < lng1 ? -1 : 1;
			}
			return 0;
		}
	};

	public static Comparator<LatLng[]> _ArrayOfLatLngsComparator = new Comparator<>() {
		@Override
		public int compare(final LatLng[] arrayOfLatLngs0, final LatLng[] arrayOfLatLngs1) {
			if (arrayOfLatLngs0 == arrayOfLatLngs1) {
				return 0;
			}
			if ((arrayOfLatLngs0 == null) != (arrayOfLatLngs1 == null)) {
				return arrayOfLatLngs0 == null ? -1 : 1;
			}
			final int n0 = arrayOfLatLngs0.length, n1 = arrayOfLatLngs1.length;
			if (n0 != n1) {
				return n0 < n1 ? -1 : 1;
			}
			for (int k = 0; k < n0; ++k) {
				final LatLng latLng0 = arrayOfLatLngs0[k];
				final LatLng latLng1 = arrayOfLatLngs1[k];
				final int compareValue = _ByLatThenLng.compare(latLng0, latLng1);
				if (compareValue != 0) {
					return compareValue;
				}
			}
			return 0;
		}
	};

	public int compareLatLng(final LatLng latLng) {
		return _ByLatThenLng.compare(this, latLng);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(new double[] { getLat(), getLng() });
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || !(o instanceof LatLng)) {
			return false;
		}
		return _ByLatThenLng.compare(this, (LatLng) o) == 0;
	}

	/** Printing routines. */
	public String getString() {
		return getString(LatLngArrayStatics._NDigitsForReadWrite);
	}

	public String getBracketFreeString() {
		final int nDigits = LatLngArrayStatics._NDigitsForReadWrite;
		final String format = "%." + nDigits + "f,%." + nDigits + "f";
		return String.format(format, getLat(), getLng());
	}

	public String getString(final int nDigits) {
		return LatLngArrayStatics.getString(nDigits, _latLngArray);
	}

	public String toCleanLatLngString(final int nDigits) {
		final String format = "%." + nDigits + "f %." + nDigits + "f";
		return String.format(format, getLat(), getLng());
	}

	public String toNavyString() {
		return toNavyString('-', false);
	}

	@Override
	public String toString() {
		return getString();
	}

	public String toNavyString(final char degreeSeparator, final boolean useMinutesMark) {
		final double lat = getLat();
		final double lng = getLng();
		final char ns = lat > 0d ? 'N' : 'S';
		final char ew = lng > 0d ? 'E' : 'W';
		final double absLat = Math.abs(lat);
		final double absLng = Math.abs(lng);
		final int nsDeg = (int) Math.floor(absLat);
		final double nsMinutes = (absLat - nsDeg) * 60d;
		final int ewDeg = (int) Math.floor(absLng);
		final double ewMinutes = (absLng - ewDeg) * 60d;
		if (!useMinutesMark) {
			return String.format("%02d%c%.3f%c %03d%c%.3f%c", nsDeg, degreeSeparator, nsMinutes, ns, ewDeg,
					degreeSeparator, ewMinutes, ew);
		}
		return String.format("%02d%c%.3f\'%c %03d%c%.3f\'%c", nsDeg, degreeSeparator, nsMinutes, ns, ewDeg,
				degreeSeparator, ewMinutes, ew);
	}

	public String toMinutesString() {
		final double lat = getLat();
		final double lng = getLng();
		final char ns = lat > 0d ? 'N' : 'S';
		final char ew = lng > 0d ? 'E' : 'W';
		final double absLat = Math.abs(lat);
		final double absLng = Math.abs(lng);
		final int nsDeg = (int) Math.floor(absLat);
		final double nsMinutes = (absLat - nsDeg) * 60d;
		final int ewDeg = (int) Math.floor(absLng);
		final double ewMinutes = (absLng - ewDeg) * 60d;
		return String.format("%02d:%.1f%c/%03d:%.1f%c", nsDeg, nsMinutes, ns, ewDeg, ewMinutes, ew);
	}

	public String toDmsString(final char degreeMarker, final char minutesMarker, final char secondsMarker) {
		final double lat = getLat(), lng = getLng();
		final char ns = lat > 0d ? 'N' : 'S';
		final char ew = lng > 0d ? 'E' : 'W';
		final int[] nsHms = toDms(lat);
		final int[] ewHms = toDms(lng);
		final String latString = String.format("%02d%c%02d%c%02d%c%c", nsHms[0], degreeMarker, nsHms[1], minutesMarker,
				nsHms[2], secondsMarker, ns);
		final String lngString = String.format("%03d%c%02d%c%02d%c%c", ewHms[0], degreeMarker, ewHms[1], minutesMarker,
				ewHms[2], secondsMarker, ew);
		return latString + '\t' + lngString;
	}

	private static int[] toDms(final double d) {
		final double absD = Math.abs(d);
		int deg = (int) Math.floor(absD);
		final double dMinutes = (absD - deg) * 60d;
		int minutes = (int) dMinutes;
		final double dSeconds = (dMinutes - minutes) * 60d;
		int seconds = (int) Math.round(dSeconds);
		if (seconds == 60) {
			seconds = 0;
			++minutes;
		}
		if (minutes == 60) {
			++deg;
		}
		return new int[] { deg, minutes, seconds };
	}

	public static String degreesToNavyString(final double degrees, final char pos, final char neg, final int nDigits) {
		final char indicator = degrees > 0d ? pos : neg;
		final double absDegs = Math.abs(degrees);
		int iDegrees = (int) Math.floor(absDegs);
		final double minutes = (absDegs - iDegrees) * 60d;
		int iMinutes = (int) Math.floor(minutes);
		final double seconds = (minutes - iMinutes) * 60d;
		int iSeconds = (int) Math.round(seconds);
		if (iSeconds == 60) {
			iSeconds = 0;
			++iMinutes;
		}
		if (iMinutes == 60) {
			++iDegrees;
			iMinutes = 0;
		}
		if (iSeconds == 0 && iMinutes == 0) {
			return String.format("%0" + nDigits + "d%c", iDegrees, indicator);
		}
		if (iSeconds == 0) {
			return String.format("%02d:%02d'%c", iDegrees, iMinutes, indicator);
		}
		if (iMinutes == 0) {
			return String.format("%02d:%02d\"%c", iDegrees, iSeconds, indicator);
		}
		return String.format("%0" + nDigits + "d:%02d'%02d\"%c", //
				iDegrees, iMinutes, iSeconds, indicator);
	}

}
