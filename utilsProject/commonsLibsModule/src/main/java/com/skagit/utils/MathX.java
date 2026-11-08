package com.skagit.utils;

import org.apache.commons.math3.util.FastMath;

import com.skagit.utils.greatCircleArc.GreatCircleArc;
import com.skagit.utils.navigation.Extent;
import com.skagit.utils.navigation.LatLng3;

public class MathX {
	final public static double _NmiToR = Constants._NmiToRAuthalic;

	public static double acosX(final double a) {
		return FastMath.acos(a);
	}

	public static double asinX(final double a) {
		return FastMath.asin(a);
	}

	public static double atan2X(final double y, final double x) {
		return FastMath.atan2(y, x);
	}

	public static double atanX(final double a) {
		return FastMath.atan(a);
	}

	public static double cosX(final double a) {
		return FastMath.cos(a);
	}

	public static double[] crossProductX(final LatLng3 latLng0, final double hdg) {
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
		final double sHdg = sinX(hdgR);
		final double cHdg = cosX(hdgR);
		final double sLat0 = latLng0.getSLat();
		final double cLat0 = latLng0.getCLat();
		final double sLng0 = latLng0.getSLng();
		final double cLng0 = latLng0.getCLng();
		final double x = sLng0 * cHdg - sLat0 * cLng0 * sHdg;
		final double y = -cLng0 * cHdg - sLat0 * sLng0 * sHdg;
		final double z = cLat0 * sHdg;
		final double[] xyz = { x, y, z };
		return xyz;
	}

	/** Cross product. */
	public static double[] crossProductX(final LatLng3 latLng0, final LatLng3 latLng1) {
		final double lat0R = Math.toRadians(latLng0.getLat());
		final double lng0R = Math.toRadians(latLng0.getLng());
		final double lat1R = Math.toRadians(latLng1.getLat());
		final double lng1R = Math.toRadians(latLng1.getLng());
		final double cLat0 = latLng0.getCLat();
		final double cLat1 = latLng1.getCLat();
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

		final double sLatSum = sinX(latSumR);
		final double sLatDiff = sinX(latDiffR);
		final double cLngSumOver2 = cosX(lngSumROver2);
		final double cLngDiffOver2 = cosX(lngDiffROver2);
		/** Williams corrections. */
		final double sLngSumOver2 = -sinX(lngSumROver2);
		final double sLngDiffOver2 = -sinX(lngDiffROver2);
		final double sLngDiff = -sinX(lngDiffR);

		final double x = sLatDiff * sLngSumOver2 * cLngDiffOver2 - sLatSum * cLngSumOver2 * sLngDiffOver2;
		final double y = sLatDiff * cLngSumOver2 * cLngDiffOver2 + sLatSum * sLngSumOver2 * sLngDiffOver2;
		final double z = cLat0 * cLat1 * sLngDiff;
		final double[] xyz = { x, y, z };
		return xyz;
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
		final double sin = sinX(absHaversine) / cosX(Math.toRadians(absLat));
		final double deltaLngR = asinX(sin);
		return deltaLngR;
	}

	final public static double[] getLatLngArrayX(final double lat0, final double lng0, final double hdg,
			final double haversine) {
		final LatLng3 latLng0 = LatLng3.getLatLngB(lat0, lng0);
		final LatLng3 latLng1 = getLatLngX(latLng0, hdg, haversine);
		return latLng1.toArray();
	}

	/** LatLng + (hdg,haversine). */
	public static LatLng3 getLatLngX(final LatLng3 latLng0, final double hdg, final double haversine) {
		final double lat0 = latLng0.getLat();
		final double lng0 = latLng0.getLng();
		if (haversine == Math.PI) {
			return LatLng3.getLatLngB(-lat0, lng0 + 180d);
		}
		/** Take care of starting point is a pole. */
		if (lat0 == 90d) {
			final double lat1 = Math.toDegrees(Constants._PiOver2 - haversine);
			return LatLng3.getLatLngB(lat1, lng0);
		}
		if (lat0 == -90d) {
			final double lat1 = Math.toDegrees(-Constants._PiOver2 + haversine);
			return LatLng3.getLatLngB(lat1, lng0);
		}

		/**
		 * Real work; we use Williams' formula that works for all haversines, not just
		 * those that cross less than 90 degrees of longitude. Also, he computes "dlon,"
		 * which a function of starting lat, and (hdg,haversine); it is how much he is
		 * planning to move to the east, and is independent of longitudinal input.
		 * Hence, when he subtracts that from his lng, I add it to mine.
		 */
		final double cLat0 = latLng0.getCLat();
		final double hdgR = Math.toRadians(hdg);
		final double sHdg = sinX(hdgR);
		final double sHav = sinX(haversine);
		/**
		 * <pre>
		 * http://williams.best.vwh.net/avform.htm#LL
			 * lat =asin(sin(lat1)*cos(d)+cos(lat1)*sin(d)*cos(tc))
			 * dlon=atan2(sin(tc)*sin(d)*cos(lat1),cos(d)-sin(lat1)*sin(lat))
			 * lon=mod( lon1-dlon +pi,2*pi )-pi
		 * </pre>
		 */
		final double cHdg = cosX(hdgR);
		final double sLat0 = latLng0.getSLat();
		final double cHav = cosX(haversine);
		final double latR = asinX(sLat0 * cHav + cLat0 * sHav * cHdg);
		final double lat = Math.toDegrees(latR);
		final double sLat = sinX(Math.toRadians(lat));
		final double y = sHdg * sHav * cLat0;
		final double x = cHav - sLat0 * sLat;
		final double dLngR = atan2X(y, x);
		/** Here's the Williams correction for adjusting the longitude. */
		final double lng = lng0 + Math.toDegrees(dLngR);
		return LatLng3.getLatLngB(lat, lng);
	}

	/** How far is (lat,lng) from the 0 meridion? */
	public static double haversineToLngX(final double lat, final double lng) {
		final double haversine = Math.abs(asinX(cosX(Math.toRadians(lat)) * sinX(Math.toRadians(lng))));
		return haversine;
	}

	/** How far is latLng from the 0 meridion? */
	public static double haversineToLngX(final LatLng3 latLng) {
		return haversineToLngX(latLng.getLat(), latLng.getLng());
	}

	public static double haversineX(final double lat0, final double lng0, final double lat1, final double lng1) {
		return haversineX(LatLng3.getLatLngB(lat0, lng0), LatLng3.getLatLngB(lat1, lng1));
	}

	/** Haversine. */
	public static double haversineX(final LatLng3 latLng0, final LatLng3 latLng1) {
		if (latLng0.equals(latLng1)) {
			return 0d;
		}
		final double lat0 = latLng0.getLat();
		final double lng0 = latLng0.getLng();
		final double lat1 = latLng1.getLat();
		final double lng1 = latLng1.getLng();
		if (lat1 == -lat0 && Math.abs(lng0 - lng1) == 180d) {
			return Math.PI;
		}
		/**
		 * <pre>
		 * http://williams.best.vwh.net/avform.htm#Dist
		 * d=2*asin(sqrt((sin((lat1-lat2)/2))^2 + cos(lat1)*cos(lat2)*(sin((lon1-lon2)/2))^2))
		 * </pre>
		 */
		final double a = sinX(Math.toRadians((lat0 - lat1) / 2d));
		final double aSq = a * a;
		final double b = sinX(Math.toRadians((lng0 - lng1) / 2d));
		final double cLat0 = latLng0.getCLat();
		final double cLat1 = latLng1.getCLat();
		final double bPlusSq = cLat0 * cLat1 * b * b;
		final double sqrt = Math.sqrt(aSq + bPlusSq);
		final double haversine = 2d * asinX(sqrt);
		return haversine;
	}

	public static double initialHdgX(final double lat0, final double lng0, final double lat1, final double lng1) {
		final LatLng3 latLng0 = LatLng3.getLatLngB(lat0, lng0);
		final LatLng3 latLng1 = LatLng3.getLatLngB(lat1, lng1);
		return initialHdgX(latLng0, latLng1);
	}

	/** InitialHdg. */
	public static double initialHdgX(final LatLng3 latLng0, final LatLng3 latLng1) {
		return initialHdgX(latLng0, latLng1, /* returnRadians= */false);
	}

	public static double initialHdgX(final LatLng3 latLng0, final LatLng3 latLng1, final boolean returnRads) {
		/** Check trivial cases. */
		final double lat0 = latLng0.getLat();
		if (latLng0.equals(latLng1)) {
			return 0d;
		}
		if (lat0 == 90d) {
			return returnRads ? Math.PI : 180d;
		}
		if (lat0 == -90d) {
			return 0d;
		}
		final double lat1 = latLng1.getLat();
		if (lat1 == 90d) {
			return 0d;
		}
		if (lat1 == -90d) {
			return returnRads ? Math.PI : 180d;
		}
		final double lng0 = latLng0.getLng();
		final double lng1 = latLng1.getLng();
		if (lng0 == lng1) {
			return lat1 < lat0 ? (returnRads ? Math.PI : 180d) : 0d;
		}
		if (Math.abs(lng0 - lng1) == 180d) {
			if (lat0 + lat1 > 0d) {
				return 0d;
			}
			if (lat0 + lat1 == 0d) {
				return returnRads ? Constants._PiOver2 : 90d;
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
		final double sLat0 = latLng0.getSLat();
		final double cLat0 = latLng0.getCLat();
		final double sLat1 = latLng1.getSLat();
		final double cLat1 = latLng1.getCLat();
		final double lngDiffR = Math.toRadians(lng0 - lng1);
		final double y = -sinX(lngDiffR) * cLat1;
		final double x = cLat0 * sLat1 - sLat0 * cLat1 * cosX(lngDiffR);
		final double hdgRadsA = atan2X(y, x);
		if (returnRads) {
			final double hdgRads = LatLng3.getInRange0_2Pi(hdgRadsA);
			return hdgRads;
		}
		final double hdgA = Math.toDegrees(hdgRadsA);
		final double hdg = LatLng3.getInRange0_360(hdgA);
		return hdg;
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
	public static double latForLngX(final GreatCircleArc gca, final double lng) {
		final LatLng3 latLng0 = gca.getLatLng0();
		final LatLng3 latLng1 = gca.getLatLng1();
		final double lng0 = latLng0.getLng();
		final double lng1 = latLng1.getLng();
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
		final double lat0 = latLng0.getLat();
		final double lat1 = latLng1.getLat();
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
	public static double[] lngsForLatX(final GreatCircleArc gca, final double lat) {
		final Extent gcaExtent = gca.createExtent();
		if (!gcaExtent.latWindowContains(lat)) {
			return new double[] {};
		}
		final LatLng3 latLng0 = gca.getLatLng0();
		final LatLng3 latLng1 = gca.getLatLng1();
		final double lat0 = latLng0.getLat();
		final double lng0 = latLng0.getLng();
		final double lat1 = latLng1.getLat();
		final double lng1 = latLng1.getLng();
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
			final boolean startsUp = gca.startsUp();
			final boolean startsDown = gca.startsDown();
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
			final GreatCircleArc gcaReverse = GreatCircleArc.CreateGca(latLng1, latLng0);
			final double[] lngsForReverse = lngsForLatX(gcaReverse, lat);
			if (lngsForReverse.length == 2) {
				final double lng = lngsForReverse[0];
				lngsForReverse[0] = lngsForReverse[1];
				lngsForReverse[1] = lng;
			}
			return lngsForReverse;
		}

		/** Finally, real work. */
		final double lat0R = Math.toRadians(lat0);
		final double lng0R = Math.toRadians(lng0);
		final double lat1R = Math.toRadians(lat1);
		final double lng1R = Math.toRadians(lng1);
		final double latR = Math.toRadians(lat);
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
		final double sLat0 = sinX(lat0R);
		final double cLat0 = cosX(lat0R);
		final double sLat1 = sinX(lat1R);
		final double cLat1 = cosX(lat1R);
		/** Williams correction. */
		final double sLngDiff = -sinX(lngDiffR);
		final double cLngDiff = cosX(lngDiffR);
		final double sLat = sinX(latR);
		final double cLat = cosX(latR);
		final double A = sLat0 * cLat1 * cLat * sLngDiff;
		final double B = sLat0 * cLat1 * cLat * cLngDiff - cLat0 * sLat1 * cLat;
		final double C = cLat0 * cLat1 * sLat * sLngDiff;
		final double sqrtAaBb = Math.sqrt(A * A + B * B);
		if (C * C > A * A + B * B) {
			return new double[] {};
		}
		final double atan2Value = atan2X(B, A);
		final double acosValue = acosX(C / sqrtAaBb);
		/** 2 Williams corrections in each of the following 2 lines. */
		final double lng0OutRa = -(-lng0R + acosValue + atan2Value);
		final double lng1OutRa = -(-lng0R - acosValue + atan2Value);
		final double lng0Out = LatLng3.getInRange180_180(Math.toDegrees(lng0OutRa));
		final double lng1Out = LatLng3.getInRange180_180(Math.toDegrees(lng1OutRa));

		final double lngDiff = LatLng3.getInRange180_180(lng1 - lng0);
		final double lngDiff0Out = LatLng3.getInRange180_180(lng0Out - lng0);
		final double lngDiff1Out = LatLng3.getInRange180_180(lng1Out - lng0);
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

	/** Wrappers. */
	public static double sinX(final double a) {
		return FastMath.sin(a);
	}

	public static native double sinY(final double a);

	public static double tanX(final double a) {
		return FastMath.tan(a);
	}

	public static double[] toCartesianX(final double lat, final double lng) {
		final double latR = Math.toRadians(lat);
		final double lngR = Math.toRadians(lng);
		final double sLat = sinX(latR);
		final double cLat = cosX(latR);
		final double sLng = sinX(lngR);
		final double cLng = cosX(lngR);
		final double[] xyz = { cLat * cLng, cLat * sLng, sLat };
		return xyz;
	}

	public static double[] toCartesianX(final LatLng3 latLng) {
		final double[] xyz = toCartesianX(latLng.getLat(), latLng.getLng());
		return xyz;
	}

	/** xyz will get normalized. */
	public static double[] toLatLngArrayX(final double[] xyz) {
		NumericalRoutines.convertToUnitLength(xyz);
		final double latR = asinX(xyz[2]);
		final double lngR = atan2X(xyz[1], xyz[0]);
		final double[] polar = { //
				Math.toDegrees(latR), //
				Math.toDegrees(lngR) //
		};
		return polar;
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
		final double A = sinX(lat0R);
		final double B = cosX(lat1R);
		/** Williams correction. */
		final double C = -sinX(lngDiff1R);
		final double D = sinX(lat1R);
		final double E = cosX(lat0R);
		/** Williams correction. */
		final double F = -sinX(lngDiff0R);
		final double G = cosX(lat0R);
		final double H = cosX(lat1R);
		/** Williams correction. */
		final double J = -sinX(lngDiff01R);
		final double num = (A * B * C) - (D * E * F);
		final double den = G * H * J;
		final double latR = atanX(num / den);
		final double lat = Math.toDegrees(latR);
		return lat;
	}

}
