package com.skagit.utils.navigation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Scanner;
import java.util.regex.Pattern;

import com.skagit.utils.Constants0;
import com.skagit.utils.MathX0;
import com.skagit.utils.NumericalRoutines;

public class LatLngArrayStatics {
	final public static int _NDigitsForReadWrite = 7;
	final public static String _StandardFormat;
	static {
		_StandardFormat = String.format("%%.%df", _NDigitsForReadWrite);
	}

	public static String getString(final int nDigits, final double[] latLngArray) {
		final String format = "[%." + nDigits + "f,%." + nDigits + "f]";
		return String.format(format, latLngArray[0], latLngArray[1]);
	}

	public static Comparator<double[]> _ByLatThenLng = (latLngArray0A, latLngArray1A) -> {
		if (latLngArray0A == latLngArray1A) {
			return 0;
		}
		if (latLngArray0A == null) {
			return -1;
		}
		if (latLngArray1A == null) {
			return 1;
		}
		/** They're not the same and neither is null. */
		final double[] latLngArray0B = normalizeLatLngArray(latLngArray0A);
		final double[] latLngArray1B = normalizeLatLngArray(latLngArray1A);
		final double lat0 = latLngArray0B[0];
		final double lat1 = latLngArray1B[0];
		if (lat0 != lat1) {
			return lat0 < lat1 ? -1 : 1;
		}
		final double lng0 = latLngArray0B[1];
		final double lng1 = latLngArray1B[1];
		return lng0 < lng1 ? -1 : (lng0 > lng1 ? 1 : 0);
	};

	public static double degsToEast180_180(final double baseDegs, final double degs) {
		return getInRange180_180(degs - baseDegs);
	}

	/** Radians routines. */
	public static double getInRange0_2Pi(final double dIn) {
		return NumericalRoutines.generalMod(dIn, Constants0._2Pi);
	}

	public static double getInRangePi_Pi(final double dIn) {
		if (-Math.PI <= dIn && dIn < Math.PI) {
			return dIn;
		}
		return NumericalRoutines.generalMod(dIn + Math.PI, Constants0._2Pi) - Math.PI;
	}

	/** Degree routines. */
	public static double getInRange180_180(final double dIn) {
		if (-180d <= dIn && dIn < 180d) {
			return dIn;
		}
		return NumericalRoutines.generalMod(dIn + 180d, 360d) - 180d;
	}

	public static double getInRange0_360(final double dIn) {
		return NumericalRoutines.generalMod(dIn, 360d);
	}

	/** We return the input if there are no changes. */
	public static double[] normalizeLatLngArray(final double[] latLngArray) {
		double lat = latLngArray[0];
		double lng = latLngArray[1];
		if (Double.isNaN(lat) || Double.isNaN(lng)) {
			return new double[] { Double.NaN, Double.NaN };
		}
		lat = getInRange180_180(lat);
		lng = getInRange180_180(lng);
		/**
		 * lat is now between -180 and 180. If it's between -90 and 90, there's no
		 * problem. Handle the problem.
		 */
		if (lat < -90d || lat > 90d) {
			if (lng >= 0d) {
				lng -= 180d;
			} else {
				lng += 180d;
			}
			lat = (lat < -90d ? -180d : 180d) - lat;
		}
		if (lat == 90d || lat == -90d) {
			lng = 0d;
		}
		if (latLngArray[0] == lat && latLngArray[1] == lng) {
			return latLngArray;
		}
		return new double[] { lat, lng };

	}

	/** Text routines. */
	public static String getDmString(final double degs, final String plusMinusString) {
		if ("ns".equalsIgnoreCase(plusMinusString)) {
			final char ns = degs > 0d ? 'N' : 'S';
			final double absLat = Math.abs(degs);
			final int nsDeg = (int) Math.floor(absLat);
			final double nsMinutes = (absLat - nsDeg) * 60d;
			return String.format("%02d-%.3f%c", nsDeg, nsMinutes, ns);
		}
		final char ew = degs > 0d ? 'E' : 'W';
		final double absLng = Math.abs(degs);
		final int ewDeg = (int) Math.floor(absLng);
		final double ewMinutes = (absLng - ewDeg) * 60d;
		return String.format("%03d-%.3f%c", ewDeg, ewMinutes, ew);
	}

	public static String toDmString(final boolean encloseInBrackets, final double[] latLngArray) {
		final double latx = latLngArray[0];
		final char ns = latx > 0d ? 'N' : 'S';
		final double absLat = Math.abs(latx);
		final int nsDeg = (int) Math.floor(absLat);
		final double nsMinutes = (absLat - nsDeg) * 60d;
		final double lngx = latLngArray[1];
		final char ew = lngx > 0d ? 'E' : 'W';
		final double absLng = Math.abs(lngx);
		final int ewDeg = (int) Math.floor(absLng);
		final double ewMinutes = (absLng - ewDeg) * 60d;
		final String bracket1String = encloseInBrackets ? "[" : "";
		final String bracket2String = encloseInBrackets ? "]" : "";
		return String.format("%s%02d-%.5f%c %03d-%.5f%c%s", bracket1String, nsDeg, nsMinutes, ns, ewDeg, ewMinutes, ew,
				bracket2String);
	}

	/**
	 * <pre>
	 * Parses:
	 * 44-40.170S 090-36.420E
	 * 44.435,-125.23 (assumed lat lng)
	 * 44.435 -125.23 (assumed lat lng)
	 * -158 16.7521  21 34.454916 Degrees Decimal Minutes (assumed lng lat)
	 * -158.151405  21.558459 Decimal Degrees  (assumed lng lat)
	 * N45 05 53.99 E155 03 59.99
	 * Also, strips any °, ', and "  symbols from the end of a field.
	 * </pre>
	 */
	final private static Pattern _PatternForSplitting1 = Pattern.compile("[\\t ,]+");
	final private static Pattern _PatternForSplitting2 = Pattern.compile("-");

	public static double[] getLatLngArray(final String latLngText) {
		final String[] fields0 = _PatternForSplitting1.split(latLngText.trim());
		final int nFields0 = fields0.length;
		/**
		 * Strip the degrees, ', and " symbols from each field, and retain only fields
		 * with positive length.
		 */
		final ArrayList<String> fields1 = new ArrayList<>(nFields0);
		NEXT_FIELDA: for (int k = 0; k < nFields0; ++k) {
			for (String field = fields0[k].trim();;) {
				final int fieldLength = field.length();
				if (fieldLength == 0) {
					continue NEXT_FIELDA;
				}
				final char lastChar = field.charAt(fieldLength - 1);
				if ('\'' != lastChar && '"' != lastChar && '�' != lastChar) {
					fields1.add(field);
					break;
				}
				field = field.substring(0, fieldLength - 1);
			}
		}
		final int nFields1 = fields1.size();
		if (nFields1 == 4) {
			final String lngString = fields1.get(0);
			final String latString = fields1.get(1);
			if (!"Decimal".equals(fields1.get(2)) || !"Degrees".equals(fields1.get(3))) {
				return null;
			}
			try {
				final double lat = Double.parseDouble(latString);
				final double lng = Double.parseDouble(lngString);
				return new double[] { lat, lng };
			} catch (final NumberFormatException e) {
				return null;
			}
		}
		if (nFields1 == 7) {
			final String lngDegString = fields1.get(0);
			final String lngMinString = fields1.get(1);
			final String latDegString = fields1.get(2);
			final String latMinString = fields1.get(3);
			if (!("Degrees".equals(fields1.get(4))) || !("Decimal".equals(fields1.get(5)))
					|| !("Minutes".equals(fields1.get(6)))) {
				return null;
			}
			try {
				final int latDeg = Integer.parseInt(latDegString);
				final double latMin = Double.parseDouble(latMinString);
				final int lngDeg = Integer.parseInt(lngDegString);
				final double lngMin = Double.parseDouble(lngMinString);
				final double lat = latDeg + Math.signum(latDeg) * latMin / 60d;
				final double lng = lngDeg + Math.signum(lngDeg) * lngMin / 60d;
				return new double[] { lat, lng };
			} catch (final NumberFormatException e) {
				return null;
			}
		}
		if (nFields1 == 2) {
			try {
				final String latString = fields1.get(0);
				final String lngString = fields1.get(1);
				final double lat = Double.parseDouble(latString);
				final double lng = Double.parseDouble(lngString);
				return new double[] { lat, lng };
			} catch (final NumberFormatException e) {
				e.printStackTrace();
			}
			/** They must be like "44-40.170S 090-36.420E." */
			double lat = Double.NaN;
			double lng = Double.NaN;
			for (int iPass = 0; iPass < 2; ++iPass) {
				final String[] fields2 = _PatternForSplitting2.split(fields1.get(iPass).trim());
				final int nFields2 = fields2.length;
				if (nFields2 == 2) {
					try {
						final int degrees = Integer.parseInt(fields2[0]);
						final String minutesString = fields2[1];
						final int len = minutesString.length();
						final char signChar = Character.toLowerCase(minutesString.charAt(len - 1));
						final boolean positive = signChar == 'n' || signChar == 'e';
						final boolean negative = signChar == 's' || signChar == 'w';
						if (!positive && !negative) {
							return null;
						}
						final double sign = positive ? 1d : -1d;
						final String minutesString2 = minutesString.substring(0, len - 1);
						final double minutes = Double.parseDouble(minutesString2);
						final double value = sign * (degrees + minutes / 60d);
						if (signChar == 'n' || signChar == 's') {
							lat = value;
						} else {
							lng = value;
						}
					} catch (final NumberFormatException e) {
						return null;
					}
				}
			}
			if (Double.isNaN(lat) || Double.isNaN(lng)) {
				return null;
			}
			return new double[] { lat, lng };
		}
		if (nFields1 == 6) {
			double lat = Double.NaN;
			double lng = Double.NaN;
			for (int iPass = 0; iPass < 2; ++iPass) {
				final String field0 = fields1.get(3 * iPass + 0);
				final String field1 = fields1.get(3 * iPass + 1);
				final String field2 = fields1.get(3 * iPass + 2);
				final String s = field0 + " " + field1 + " " + field2;
				double latOrLng = Double.NaN;
				try {
					latOrLng = Double.parseDouble(s);
				} catch (final NumberFormatException e) {
					e.printStackTrace();
				}
				final char signChar = Character.toLowerCase(s.charAt(0));
				final boolean positive = signChar == 'n' || signChar == 'e';
				final boolean negative = signChar == 's' || signChar == 'w';
				if (!positive && !negative) {
					return null;
				}
				final double sign = positive ? 1d : -1d;
				double d = 0d;
				try (final Scanner scanner = new Scanner(s)) {
					for (int k = 0; scanner.hasNext() && k < 3; ++k) {
						final String field = scanner.next();
						final String sToParse = k == 0 ? field.substring(1) : field;
						try {
							double thisD = Double.parseDouble(sToParse);
							thisD /= k == 0 ? 1d : (k == 1 ? 60d : 3600d);
							d += thisD;
						} catch (final NumberFormatException e) {
							return null;
						}
					}
				}
				latOrLng = d * sign;
				if (signChar == 'n' || signChar == 's') {
					lat = latOrLng;
				} else {
					lng = latOrLng;
				}
			}
			if (Double.isNaN(lat) || Double.isNaN(lng)) {
				return null;
			}
			return new double[] { lat, lng };
		}
		return null;
	}

	final private static String[] _DirectionNames = { "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW",
			"SW", "WSW", "W", "WNW", "NW", "NNW" };

	final public static String getDirectionName(double d) {
		final int n = _DirectionNames.length;
		d = getInRange0_360(d);
		final int k = ((int) Math.round(n * (d / 360d))) % n;
		return _DirectionNames[k];
	}

	/** Returns a number in [-180,180), which is the turn to the left. */
	public static double getTurnToLeftD(final double[] latLngArray0, final double[] scLat0, final double[] latLngArray1,
			final double[] scLat1, final double[] latLngArray2, final double[] scLat2) {
		final double hdg10 = initialHdg(latLngArray1, scLat1, latLngArray0, scLat0, /* returnRads= */false);
		final double hdg12 = initialHdg(latLngArray1, scLat1, latLngArray2, scLat2, /* returnRads= */false);
		final double turnToLeft = 180d + hdg10 - hdg12;
		return getInRange180_180(turnToLeft);
	}

	/**
	 * Returns a number in [0,2Pi), which is the interior angle, assuming this is
	 * part of a ccw tracing of a polygon.
	 */
	public static double getInteriorAngleForCcwR(final double[] latLngArray0, final double[] scLat0,
			final double[] pivot, final double[] scPivotLat, final double[] latLngArray1, final double[] scLat1) {
		final double hdg10R = initialHdg(pivot, scPivotLat, latLngArray0, scLat0, /* returnRads= */true);
		final double hdg12R = initialHdg(pivot, scPivotLat, latLngArray1, scLat1, /* returnRads= */true);
		final double interiorAngleForCcwRX = hdg12R - hdg10R;
		return getInRange0_2Pi(interiorAngleForCcwRX);
	}

	/** xyz will get normalized. */
	public static double[] toLatLngArrayX(final double[] xyz) {
		NumericalRoutines.convertToUnitLength(xyz);
		final double latR = MathX0.asinX(xyz[2]);
		final double lngR = MathX0.atan2X(xyz[1], xyz[0]);
		return new double[] { //
				Math.toDegrees(latR), //
				Math.toDegrees(lngR) //
		};
	}

	/**
	 * <pre>
	 * Ralph Wojtowicz' formula for finding the values of theta for which: xi *
	 * cos(theta) + eta * sin(theta) = r. After normalizing (xi,eta), we can think
	 * of xi and eta as the cos and sin of some angle psi. Then the defining
	 * equation is the cos of the difference of angles psi and theta, and the rest
	 * is easy.
	 */
	final private static double _ThresholdD = 1.0e-10;

	public static double[] useWojtowicz(final double xi, final double eta, final double r) {
		final double kk = xi * xi + eta * eta;
		if (kk <= r * r) {
			return null;
		}
		final double k = Math.sqrt(kk);
		final double psiR = MathX0.atan2X(eta, xi);
		final double deltaR = MathX0.acosX(r / k);
		/** psi - theta = plus-or-minus delta. */
		final double absDeltaD = Math.toDegrees(Math.abs(deltaR));
		if (absDeltaD < _ThresholdD) {
			return new double[] { psiR + (psiR < 0d ? Constants0._2Pi : 0) };
		}
		/** theta = psi - delta or psi + delta. */
		double theta0R = getInRange0_2Pi(psiR + deltaR);
		double theta1R = getInRange0_2Pi(psiR - deltaR);
		if (theta1R < theta0R) {
			final double theta = theta0R;
			theta0R = theta1R;
			theta1R = theta;
		}
		return new double[] { theta0R, theta1R };
	}

	public static double[] toCartesian(final double[] latLngArray) {
		final double latR = Math.toRadians(latLngArray[0]);
		final double lngR = Math.toRadians(latLngArray[1]);
		final double sLat = MathX0.sinX(latR);
		final double cLat = MathX0.cosX(latR);
		final double sLng = MathX0.sinX(lngR);
		final double cLng = MathX0.cosX(lngR);
		return new double[] { cLat * cLng, cLat * sLng, sLat };
	}

	/** Cross product. */
	public static double[] crossProduct(final double[] scLat, final double[] scLng, final double hdg) {
		final double hdgR = Math.toRadians(hdg);
		/**
		 * <pre>
		 * http: // www.movable-type.co.uk/scripts/latlong-vectors.html
		 * var x =  sin(λ) * cos(θ) - sin(φ) * cos(λ) * sin(θ);
		 * var y = -cos(λ) * cos(θ) - sin(φ) * sin(λ) * sin(θ);
		 * var z =  cos(φ) * sin(θ);
		 *
		 * X = sin(lng0R) * cos(hdgR) - sin(lat0R) * cos(lng0R) * sin(hdgR);
		 * Y = -cos(lng0R) * cos(hdgR) - sin(lat0R) * sin(lng0R) * sin(hdR);
		 * Z = cos(lat0R) * sin(hdgR);
		 * </pre>
		 */
		final double sHdg = MathX0.sinX(hdgR);
		final double cHdg = MathX0.cosX(hdgR);
		final double sLat = scLat[0], cLat = scLat[1];
		final double sLng = scLng[0], cLng = scLng[1];
		final double x = sLng * cHdg - sLat * cLng * sHdg;
		final double y = -cLng * cHdg - sLat * sLng * sHdg;
		final double z = cLat * sHdg;
		return new double[] { x, y, z };
	}

	public static double[] crossProduct(final double[] latLngArray0, final double[] latLngArray1, final double cLat0,
			final double cLat1) {
		final double lat0R = Math.toRadians(latLngArray0[0]);
		final double lng0R = Math.toRadians(latLngArray0[1]);
		final double lat1R = Math.toRadians(latLngArray1[0]);
		final double lng1R = Math.toRadians(latLngArray1[1]);
		/**
		 * <pre>
		 * from: http://williams.best.vwh.net/intersect.htm
		 * e1 X e2 = {
		 * 	 sin(lat1-lat2) *sin((lon1+lon2)/2) *cos((lon1-lon2)/2) - sin(lat1+lat2) *cos((lon1+lon2)/2) *sin((lon1-lon2)/2),
		 *   sin(lat1-lat2) *cos((lon1+lon2)/2) *cos((lon1-lon2)/2) + sin(lat1+lat2) *sin((lon1+lon2)/2) *sin((lon1-lon2)/2),
		 *   cos(lat1)*cos(lat2)*sin(lon1-lon2)
		 * } (5)
		 * </pre>
		 */
		final double latDiffR = lat0R - lat1R;
		final double lngDiffR = lng0R - lng1R;
		final double lngSumROver2 = (lng0R + lng1R) / 2d;
		final double lngDiffROver2 = lngDiffR / 2d;
		final double latSumR = lat0R + lat1R;

		final double sLatSum = MathX0.sinX(latSumR);
		final double sLatDiff = MathX0.sinX(latDiffR);
		final double cLngSumOver2 = MathX0.cosX(lngSumROver2);
		final double cLngDiffOver2 = MathX0.cosX(lngDiffROver2);
		/** Williams corrections. */
		final double sLngSumOver2 = -MathX0.sinX(lngSumROver2);
		final double sLngDiffOver2 = -MathX0.sinX(lngDiffROver2);
		final double sLngDiff = -MathX0.sinX(lngDiffR);

		final double x = sLatDiff * sLngSumOver2 * cLngDiffOver2 - sLatSum * cLngSumOver2 * sLngDiffOver2;
		final double y = sLatDiff * cLngSumOver2 * cLngDiffOver2 + sLatSum * sLngSumOver2 * sLngDiffOver2;
		final double z = cLat0 * cLat1 * sLngDiff;
		return new double[] { x, y, z };
	}

	/**
	 * What is the largest longitude that a circle centered at (lat,0) with radius
	 * haversine, will reach?
	 */
	public static double getDeltaLngHaversine(final double lat, final double haversine) {
		/** If the circle contains a pole, we need all longitudes. */
		final double absLat = Math.abs(lat);
		final double absHaversine = Math.abs(haversine);
		if (absLat + Math.toDegrees(absHaversine) >= 90d) {
			return Math.PI;
		}
		final double sin = MathX0.sinX(absHaversine) / MathX0.cosX(Math.toRadians(absLat));
		return MathX0.asinX(sin);
	}

	/** LatLng + (hdg,haversine). */
	final public static double[] getLatLngArray(final double[] latLngArray0, final double[] scLat0,
			final double haversine, final double hdg) {
		if (haversine == Math.PI) {
			return LatLngArrayStatics.normalizeLatLngArray(new double[] { -latLngArray0[0], latLngArray0[1] + 180d });
		}
		/** Take care of starting point is a pole. */
		if (latLngArray0[0] == 90d) {
			return LatLngArrayStatics.normalizeLatLngArray(
					new double[] { Math.toDegrees(Constants0._PiOver2 - haversine), latLngArray0[1] });
		}
		if (latLngArray0[0] == -90d) {
			return LatLngArrayStatics.normalizeLatLngArray(
					new double[] { Math.toDegrees(-Constants0._PiOver2 + haversine), latLngArray0[1] });
		}

		/**
		 * We use Williams' formula that works for all haversines, not just those that
		 * cross less than 90 degrees of longitude. Also, he computes "dlon," which a
		 * function of starting lat, and (hdg,haversine); it is how much he is planning
		 * to move to the east, and is independent of longitudinal input. Hence, when he
		 * subtracts that from his lng, I add it to mine.
		 */
		final double hdgR = Math.toRadians(hdg);
		final double sHdg = MathX0.sinX(hdgR);
		final double sHav = MathX0.sinX(haversine);
		/**
		 * <pre>
		 * http://williams.best.vwh.net/avform.htm#LL
			 * lat =asin(sin(lat1)*cos(d)+cos(lat1)*sin(d)*cos(tc))
			 * dlon=atan2(sin(tc)*sin(d)*cos(lat1),cos(d)-sin(lat1)*sin(lat))
			 * lon=mod( lon1-dlon +pi,2*pi )-pi
		 * </pre>
		 */
		final double sLat0 = scLat0[0], cLat0 = scLat0[1];
		final double cHdg = MathX0.cosX(hdgR);
		final double cHav = MathX0.cosX(haversine);
		final double latR = MathX0.asinX(sLat0 * cHav + cLat0 * sHav * cHdg);
		final double lat = Math.toDegrees(latR);
		final double sLat = MathX0.sinX(Math.toRadians(lat));
		final double y = sHdg * sHav * cLat0;
		final double x = cHav - sLat0 * sLat;
		final double dLngR = MathX0.atan2X(y, x);
		/** Here's the Williams correction for adjusting the longitude. */
		final double lng = latLngArray0[1] + Math.toDegrees(dLngR);
		return LatLngArrayStatics.normalizeLatLngArray(new double[] { lat, lng });
	}

	/** How far is (lat,lng) from the 0 meridion? */
	public static double haversineToLng(final double lat, final double lng) {
		return Math.abs(MathX0.asinX(MathX0.cosX(Math.toRadians(lat)) * MathX0.sinX(Math.toRadians(lng))));
	}

	/** Haversine. */
	public static double haversine(final double[] latLngArray0, final double[] latLngArray1, final double cLat0,
			final double cLat1) {
		LatLngArrayStatics.normalizeLatLngArray(latLngArray0);
		LatLngArrayStatics.normalizeLatLngArray(latLngArray1);
		final int compareValue = LatLngArrayStatics._ByLatThenLng.compare(latLngArray0, latLngArray1);
		if (compareValue == 0) {
			return 0d;
		}
		final double lat0 = latLngArray0[0];
		final double lng0 = latLngArray0[1];
		final double lat1 = latLngArray1[0];
		final double lng1 = latLngArray1[1];
		if (lat1 == -lat0 && Math.abs(lng0 - lng1) == 180d) {
			return Math.PI;
		}
		/**
		 * <pre>
		 * http://williams.best.vwh.net/avform.htm#Dist
		 * d=2*asin(sqrt((sin((lat1-lat2)/2))^2 + cos(lat1)*cos(lat2)*(sin((lon1-lon2)/2))^2))
		 * </pre>
		 */
		final double a = MathX0.sinX(Math.toRadians((lat0 - lat1) / 2d));
		final double aSq = a * a;
		final double b = MathX0.sinX(Math.toRadians((lng0 - lng1) / 2d));
		final double bPlusSq = cLat0 * cLat1 * b * b;
		final double sqrt = Math.sqrt(aSq + bPlusSq);
		return 2d * MathX0.asinX(sqrt);
	}

	public static double initialHdg(double[] latLngArray0, double[] scLat0, double[] latLngArray1,
			final double[] scLat1, final boolean returnRads) {
		/** Check trivial cases. */
		if (Arrays.equals(latLngArray0, latLngArray1)) {
			return 0d;
		}
		latLngArray0 = normalizeLatLngArray(latLngArray0);
		final double lat0 = latLngArray0[0];
		if (lat0 == 90d) {
			return returnRads ? Math.PI : 180d;
		}
		if (lat0 == -90d) {
			return 0d;
		}
		latLngArray1 = normalizeLatLngArray(latLngArray1);
		final double lat1 = latLngArray1[0];
		if (lat1 == 90d) {
			return 0d;
		}
		if (lat1 == -90d) {
			return returnRads ? Math.PI : 180d;
		}
		final double lng0 = latLngArray0[1];
		final double lng1 = latLngArray1[1];
		if (lng0 == lng1) {
			return lat1 < lat0 ? (returnRads ? Math.PI : 180d) : 0d;
		}
		if (Math.abs(lng0 - lng1) == 180d) {
			if (lat0 + lat1 > 0d) {
				return 0d;
			}
			if (lat0 + lat1 == 0d) {
				return returnRads ? Constants0._PiOver2 : 90d;
			}
			return returnRads ? Math.PI : 180d;
		}
		/**
		 * <pre>
		 * http://williams.best.vwh.net/avform.htm#Dist
		 * tc1=mod(atan2(sin(lon1-lon2)*cos(lat2),
		 *   cos(lat1)*sin(lat2)-sin(lat1)*cos(lat2)*cos(lon1-lon2)), 2*pi)
		 * </pre>
		 */
		if (scLat0 == null) {
			final double lat0R = Math.toRadians(latLngArray0[0]);
			scLat0 = new double[] { MathX0.sinX(lat0R), MathX0.cosX(lat0R) };
		}
		final double sLat0 = scLat0[0];
		final double cLat0 = scLat0[1];
		if (scLat1 == null) {
			final double lat1R = Math.toRadians(latLngArray1[0]);
			scLat0 = new double[] { MathX0.sinX(lat1R), MathX0.cosX(lat1R) };
		}
		final double sLat1 = scLat1[0];
		final double cLat1 = scLat1[1];
		final double lngDiffR = Math.toRadians(lng0 - lng1);
		final double y = -MathX0.sinX(lngDiffR) * cLat1;
		final double x = cLat0 * sLat1 - sLat0 * cLat1 * MathX0.cosX(lngDiffR);
		final double hdgRadsA = MathX0.atan2X(y, x);
		if (returnRads) {
			return LatLngArrayStatics.getInRange0_2Pi(hdgRadsA);
		}
		final double hdgA = Math.toDegrees(hdgRadsA);
		return LatLngArrayStatics.getInRange0_360(hdgA);
	}

	/**
	 * <pre>
	 * Finds the first lat where gca crosses lng.  Except for meridionals and polars,
	 * If polar, returns startLat if startLng = lng, and 90 or -90 if endLng == lng.
	 *   there is at most 1.
	 *   Otherwise it returns Nan.
	 * If meridional, returns startLat or Nan.
	 * If lng is not in gca's range, it returns Nan.
	 * </pre>
	 */
	public static double latForLng(final double[] latLngArray0, final double[] latLngArray1, final double lng) {
		final double lng0 = latLngArray0[1];
		final double lng1 = latLngArray1[1];
		/** Check the lng window. */
		final double minLng = Math.min(lng0, lng1);
		final double maxLng = Math.max(lng0, lng1);
		if (minLng + 180d < maxLng) {
			/** Wraps the Idl; the middle is the dead spot. */
			if (minLng < lng && lng < maxLng) {
				return Double.NaN;
			}
		} else /** Does not wrap the Idl; the outsides are the dead spots. */
		if (lng < minLng || lng > maxLng) {
			return Double.NaN;
		}

		/** Dispose of trivial cases. */
		final double lat0 = latLngArray0[0];
		final double lat1 = latLngArray1[1];
		if (lng0 == lng) {
			return lat0;
		}
		if (lng1 == lng) {
			/**
			 * We don't start at the correct lng, but we end there. If we go across a pole
			 * to get there, the answer is the pole.
			 */
			if ((lng1 <= 0d && lng0 == lng1 + 180d) || (lng1 > 0d && lng0 == lng1 - 180d)) {
				return (lat0 + lat1 >= 0d) ? 90d : -90d;
			}
			/**
			 * We don't start at the correct lng, we end there, and we don't go across a
			 * pole to get there; the answer is lat1.
			 */
			return lat1;
		}
		return williamsLatForLng(lat0, lng0, lat1, lng1, lng);
	}

	/**
	 * The lngs come back sorted; we hit the first one first and the second one
	 * second, as we traverse the arc.
	 */
	public static double[] lngsForLat(final double[] latLngArray0, final double[] latLngArray1, double[] scLat0,
			double[] scLat1, final double lat) {
		final double lat0 = latLngArray0[0];
		final double lng0 = latLngArray0[1];
		final double lat1 = latLngArray1[0];
		final double lng1 = latLngArray1[1];
		final double lat0R = Math.toRadians(lat0);
		final double lng0R = Math.toRadians(lng0);
		final double lat1R = Math.toRadians(lat1);
		final double lng1R = Math.toRadians(lng1);
		final double latR = Math.toRadians(lat);
		final double sLat0, cLat0;
		if (scLat0 == null) {
			sLat0 = MathX0.sinX(lat0R);
			cLat0 = MathX0.cosX(lat0R);
			scLat0 = new double[] { sLat0, cLat0 };
		} else {
			sLat0 = scLat0[0];
			cLat0 = scLat0[1];
		}
		final double sLat1, cLat1;
		if (scLat1 == null) {
			sLat1 = MathX0.sinX(lat1R);
			cLat1 = MathX0.cosX(lat1R);
			scLat1 = new double[] { sLat1, cLat1 };
		} else {
			sLat1 = scLat1[0];
			cLat1 = scLat1[1];
		}
		final GcaTypeAndBounds gcaTypeAndBounds = new GcaTypeAndBounds(latLngArray0, latLngArray1, cLat0, cLat1);
		if (!gcaTypeAndBounds.latWindowContains(lat)) {
			return new double[] {};
		}
		/**
		 * <pre>
		 * We use the formulary to find both longitudes of the Great Circle
		 * (not just the Gca) that have this lat.
		 *   l12 = lon1-lon2
		 *   A = sin(lat1)*cos(lat2)*cos(lat3)*sin(l12)
		 *   B = sin(lat1)*cos(lat2)*cos(lat3)*cos(l12) - cos(lat1)*sin(lat2)*cos(lat3)
		 *   C = cos(lat1)*cos(lat2)*sin(lat3)*sin(l12)
		 *   lon = atan2(B,A) (atan2(y,x) convention)
		 *   IF (abs(C) > sqrt(A^2 + B^2))
		 *     "no crossing"
		 *   ELSE
		 *     dlon = acos(C/sqrt(A^2+B^2))
		 *     lon3_1 = mod(lon1+dlon+lon+pi, 2*pi)-pi
		 *     lon3_2 = mod(lon1-dlon+lon+pi, 2*pi)-pi
		 *   ENDIF
		 * </pre>
		 */
		final double lngDiffR = lng0R - lng1R;

		/**
		 * Dispose of trivial cases; meridional Gcas and those that go through a pole,
		 * gca starts and ends at the same lat, and lat is one of gca's endpoints' lats.
		 */
		if (lng0 == lng1) {
			return new double[] { lng0 };
		}
		if (Math.abs(lng0 - lng1) == 180d) {
			if (lat0 + lat1 >= 0d) {
				/** Through the north pole. */
				if (lat0 <= lat) {
					/** We'll have lng0. */
					if (lat1 <= lat) {
						/** We'll have lng1. */
						return new double[] { lng0, lng1 };
					}
					return new double[] { lng0 };
				}
				if (lat1 <= lat) {
					return new double[] { lng1 };
				}
			} else /** Through the south pole */
			if (lat0 >= lat) {
				if (lat1 >= lat) {
					return new double[] { lng0, lng1 };
				}
				return new double[] { lng0 };
			} else if (lat1 >= lat) {
				return new double[] { lng1 };
			}
		}
		if (lat0 == lat1 && lat == lat0) {
			return new double[] { lng0, lng1 };
		}

		if (lat == lat0) {
			/** We start at the input lat and do not end there. */
			final boolean startsUp = gcaTypeAndBounds.startsUp();
			final boolean startsDown = gcaTypeAndBounds.startsDown();
			if ((!startsUp && !startsDown) || (startsUp && lat1 > lat0)) {
				return new double[] { lng0 };
			}
			if (startsDown && lat1 < lat0) {
				return new double[] { lng0 };
			}
		}
		if (lat == lat1) {
			/**
			 * We don't start at the input lat, but we end there. Go in the reverse
			 * direction and reverse the longitudes.
			 */
			final double[] lngsForReverse = lngsForLat(latLngArray1, latLngArray0, scLat1, scLat0, lat);
			if (lngsForReverse.length == 2) {
				final double lng = lngsForReverse[0];
				lngsForReverse[0] = lngsForReverse[1];
				lngsForReverse[1] = lng;
			}
			return lngsForReverse;
		}

		/** Finally, real work. */
		/**
		 * <pre>
		 * We use the formulary to find both longitudes of the Great Circle
		 * (not just the Gca) that have this lat.
		 *   l12 = lon1-lon2
		 *   A = sin(lat1)*cos(lat2)*cos(lat3)*sin(l12)
		 *   B = sin(lat1)*cos(lat2)*cos(lat3)*cos(l12) - cos(lat1)*sin(lat2)*cos(lat3)
		 *   C = cos(lat1)*cos(lat2)*sin(lat3)*sin(l12)
		 *   lon = atan2(B,A) (atan2(y,x) convention)
		 *   IF (abs(C) > sqrt(A^2 + B^2))
		 *     "no crossing"
		 *   ELSE
		 *     dlon = acos(C/sqrt(A^2+B^2))
		 *     lon3_1 = mod(lon1+dlon+lon+pi, 2*pi)-pi
		 *     lon3_2 = mod(lon1-dlon+lon+pi, 2*pi)-pi
		 *   ENDIF
		 * </pre>
		 */
		/** Williams correction. */
		final double sLngDiff = -MathX0.sinX(lngDiffR);
		final double cLngDiff = MathX0.cosX(lngDiffR);
		final double sLat = MathX0.sinX(latR);
		final double cLat = MathX0.cosX(latR);
		final double A = sLat0 * cLat1 * cLat * sLngDiff;
		final double B = sLat0 * cLat1 * cLat * cLngDiff - cLat0 * sLat1 * cLat;
		final double C = cLat0 * cLat1 * sLat * sLngDiff;
		final double sqrtAaBb = Math.sqrt(A * A + B * B);
		if (C * C > A * A + B * B) {
			return new double[] {};
		}
		final double atan2Value = MathX0.atan2X(B, A);
		final double acosValue = MathX0.acosX(C / sqrtAaBb);
		/** 2 Williams corrections in each of the following 2 lines. */
		final double lng0OutRa = -(-lng0R + acosValue + atan2Value);
		final double lng1OutRa = -(-lng0R - acosValue + atan2Value);
		final double lng0Out = LatLngArrayStatics.getInRange180_180(Math.toDegrees(lng0OutRa));
		final double lng1Out = LatLngArrayStatics.getInRange180_180(Math.toDegrees(lng1OutRa));

		final double lngDiff = LatLngArrayStatics.getInRange180_180(lng1 - lng0);
		final double lngDiff0Out = LatLngArrayStatics.getInRange180_180(lng0Out - lng0);
		final double lngDiff1Out = LatLngArrayStatics.getInRange180_180(lng1Out - lng0);
		final boolean zeroIsIn, oneIsIn;
		if (lngDiff > 0d) {
			zeroIsIn = 0 <= lngDiff0Out && lngDiff0Out <= lngDiff;
			oneIsIn = 0 <= lngDiff1Out && lngDiff1Out <= lngDiff;
		} else {
			zeroIsIn = lngDiff <= lngDiff0Out && lngDiff0Out <= 0d;
			oneIsIn = lngDiff <= lngDiff1Out && lngDiff1Out <= 0d;
		}
		if (zeroIsIn && !oneIsIn) {
			return new double[] { lng0Out };
		}
		if (!zeroIsIn && oneIsIn) {
			return new double[] { lng1Out };
		}
		if (!zeroIsIn && !oneIsIn) {
			return new double[] {};
		}
		if (Math.abs(lngDiff0Out) <= Math.abs(lngDiff1Out)) {
			return new double[] { lng0Out, lng1Out };
		}
		return new double[] { lng1Out, lng0Out };
	}

	private static double williamsLatForLng(final double lat0, final double lng0, final double lat1, final double lng1,
			final double lng) {
		final double lat0R = Math.toRadians(lat0);
		final double lng0R = Math.toRadians(lng0);
		final double lat1R = Math.toRadians(lat1);
		final double lng1R = Math.toRadians(lng1);
		final double lngR = Math.toRadians(lng);
		/**
		 * <pre>
		 *  From: http://williams.best.vwh.net/avform.htm#Intermediate
		 * lat = atan(
		 *   (sin(lat1)*cos(lat2)*sin(lon-lon2) - sin(lat2)*cos(lat1)*sin(lon-lon1))/
		 *   (cos(lat1)*cos(lat2)*sin(lon1-lon2))
		 * )
		 * </pre>
		 */
		final double lngDiff0R = lngR - lng0R;
		final double lngDiff1R = lngR - lng1R;
		final double lngDiff01R = lng0R - lng1R;
		final double A = MathX0.sinX(lat0R);
		final double B = MathX0.cosX(lat1R);
		/** Williams correction. */
		final double C = -MathX0.sinX(lngDiff1R);
		final double D = MathX0.sinX(lat1R);
		final double E = MathX0.cosX(lat0R);
		/** Williams correction. */
		final double F = -MathX0.sinX(lngDiff0R);
		final double G = MathX0.cosX(lat0R);
		final double H = MathX0.cosX(lat1R);
		/** Williams correction. */
		final double J = -MathX0.sinX(lngDiff01R);
		final double num = (A * B * C) - (D * E * F);
		final double den = G * H * J;
		final double latR = MathX0.atanX(num / den);
		return Math.toDegrees(latR);
	}

	public static double[] toCartesianX(final double lat, final double lng) {
		final double latR = Math.toRadians(lat);
		final double lngR = Math.toRadians(lng);
		final double sLat = MathX0.sinX(latR);
		final double cLat = MathX0.cosX(latR);
		final double sLng = MathX0.sinX(lngR);
		final double cLng = MathX0.cosX(lngR);
		return new double[] { cLat * cLng, cLat * sLng, sLat };
	}

}
