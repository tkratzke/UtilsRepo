package com.skagit.utils.latLng;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.skagit.utils.Constants0;
import com.skagit.utils.MathX0;
import com.skagit.utils.NumericalRoutines;
import com.skagit.utils.math.TwoDCentroid;
import com.skagit.utils.myLogger.MyLogger;

public class TangentCylinder implements Comparator<LatLng> {
	final private static boolean _UseStereographic = true;
	final private static boolean _UseGnomonic = !_UseStereographic;
	final private double _centralLng;
	/**
	 * <pre>
	 * _cLat and _sLat are all we need to store instead of an entire
	 * basis. When we get a regular LatLng to convert, we find the offset of the
	 * longitude. Doing this allows us to assume that the reference point's
	 * longitude is 0.0.
	 * With that assumption, the basis of interest is:
	 *   b0 = [_cLat,0,_sLat]
	 *   b1 = [0, 1, 0], and
	 *   b2 = go north from b0 by 90 degrees.
	 * Hence, the basis is:
	 * [_cLat, 0, _sLat], [0, 1, 0], and [-_sLat, 0, _cLat].
	 * Examples:
	 *   North pole, basis: [0, 0, 1], [0, 1, 0], and [-1, 0, 0]
	 *   South pole, basis: [0, 0, -1], [0, 1, 0], and [1, 0, 0]
	 *   (0,0),      basis: [1, 0, 0], [0, 1, 0], and [0, 0, 1]
	 * </pre>
	 */
	final private double _cLat;
	final private double _sLat;
	final private FlatLatLng _centralFlatLatLng;
	/**
	 * Used to interpret "normal" directions. When we are told to move "north," we
	 * interpret that as "move towards the north pole" or "move away from the south
	 * pole." "East" means 90 degrees to that direction. We store the distance to
	 * the pole of interest, and which one we are interested in.
	 */
	final private FlatLatLng _poleFlatLatLng;
	final private boolean _poleIsNorth;
	final private double _nmiToR;

	public static TangentCylinder getTangentCylinder(final LatLng latLng) {
		if (latLng instanceof final FlatLatLng flatLatLng) {
			if (flatLatLng._eastOffsetR == 0d && flatLatLng._northOffsetR == 0d) {
				return flatLatLng.getOwningTangentCylinder();
			}
		}
		return new TangentCylinder(latLng);
	}

	final private static int _NPassesToTry = 25;
	final private static double _MaxMeanOffsetM = 0.25;
	final private static boolean _AnnounceProblemsWithTangentCylinderConvergence = false;

	/**
	 * The perimeter in the following must be that of a polygon. The center of mass
	 * that we compute in this routine is that of the polygon, not just the members
	 * of perimeter.
	 */
	public static TangentCylinder getTangentCylinder(final MyLogger logger, final LatLng[] perimeter) {
		/**
		 * Our first candidate TangentCylinder is the one that is centered at the
		 * Extent's center.
		 */
		final Extent extent = new Extent(Arrays.asList(perimeter), /* connectLastToFirst= */true);
		final Extent.SmallCircle smallCircle = extent.getSmallCircle();
		final LatLng firstTry = smallCircle._centerLatLngArray;
		TangentCylinder tangentCylinder = getTangentCylinder(firstTry);
		TangentCylinder bestTangentCylinder = null;
		double bestMeanOffsetR = Double.MAX_VALUE;
		double bestMeanOffsetM = Double.MAX_VALUE;
		final int nLatLngs = perimeter.length;
		for (int iTry = 0; iTry < _NPassesToTry; ++iTry) {
			final double[][] points = new double[nLatLngs][2];
			for (int k = 0; k < nLatLngs; ++k) {
				final LatLng latLng = perimeter[k];
				final TangentCylinder.FlatLatLng flatLatLng = tangentCylinder.convertToMyFlatLatLng(latLng);
				final double eastOffset = flatLatLng.getEastOffsetR();
				final double northOffset = flatLatLng.getNorthOffsetR();
				points[k][0] = eastOffset;
				points[k][1] = northOffset;
			}
			final double[] theseMeans = TwoDCentroid.getCentroid(points);
			/**
			 * If the mean of the offsets is small, this is a good choice for a
			 * TangentCylinder.
			 */
			final double meanX_R = theseMeans[0];
			final double meanY_R = theseMeans[1];
			/**
			 * Regardless, the next TangentCylinder to try is the one that is centered at
			 * the mean offset.
			 */
			final FlatLatLng centralFlatLatLng = tangentCylinder.new FlatLatLng(meanX_R, meanY_R);
			tangentCylinder = TangentCylinder.getTangentCylinder(centralFlatLatLng);
			final double meanOffsetR = Math.sqrt(meanX_R * meanX_R + meanY_R * meanY_R);
			final double meanOffsetNmi = meanOffsetR / tangentCylinder.getNmiToR();
			final double meanOffsetM = meanOffsetNmi * Constants0._NmiToM;
			if (meanOffsetR < bestMeanOffsetR) {
				bestMeanOffsetR = meanOffsetR;
				bestMeanOffsetM = meanOffsetM;
				bestTangentCylinder = tangentCylinder;
				if (meanOffsetM < _MaxMeanOffsetM) {
					break;
				}
			}
			if (_AnnounceProblemsWithTangentCylinderConvergence) {
				if (iTry >= 4) {
					final String f = "\nTrouble Converging when setting a TangentCylinder: "
							+ "meanOffsetM[%.9g] bestMeanOffsetM[%.9g] threshold[%.9g]";
					String s = String.format(f, meanOffsetM, bestMeanOffsetM, _MaxMeanOffsetM);
					s += String.format("\n\tNew center:%s", centralFlatLatLng.getString());
					MyLogger.out(logger, s);
				}
			}
		}
		return bestTangentCylinder;
	}

	/**
	 * In this one, latLngs is just a collection of points and we're trying to get
	 * close to this collection's center of mass. It returns the FlatLatLngs.
	 */
	public static FlatLatLng[] getFlatLatLngs(final LatLng[] latLngs, double[] weights) {
		final int nLatLngs = latLngs == null ? 0 : latLngs.length;
		if (nLatLngs == 0) {
			return null;
		}
		if (weights != null) {
			double ttlWt = 0d;
			for (int k = 0; k < nLatLngs; ++k) {
				final double wt = weights[k];
				if (wt < 0d) {
					ttlWt = 0d;
					weights = null;
					break;
				}
				ttlWt += wt;
			}
			if (ttlWt == 0d) {
				weights = null;
			}
		}
		if (weights == null) {
			weights = new double[nLatLngs];
			for (int k = 0; k < nLatLngs; ++k) {
				weights[k] = 1d;
			}
		}
		double ttlWt = 0d;
		for (int k = 0; k < nLatLngs; ++k) {
			ttlWt += weights[k];
		}
		final double[] wts = new double[nLatLngs];
		for (int k = 0; k < nLatLngs; ++k) {
			wts[k] = weights[k] / ttlWt;
		}

		TangentCylinder tangentCylinder = getTangentCylinder(latLngs[0]);
		TangentCylinder bestTangentCylinder = null;
		double bestOffsetR = Double.POSITIVE_INFINITY;
		FlatLatLng[] bestFlatLatLngs = null;
		final FlatLatLng[] flatLatLngs = new FlatLatLng[nLatLngs];
		Arrays.fill(flatLatLngs, null);
		for (int i = 0; i < _NPassesToTry; ++i) {
			double ttlWeightedEastOffsetR = 0d;
			double ttlWeightedNorthOffsetR = 0d;
			for (int k = 0; k < nLatLngs; ++k) {
				final LatLng latLng = flatLatLngs[k] == null ? latLngs[k] : flatLatLngs[k];
				flatLatLngs[k] = tangentCylinder.convertToMyFlatLatLng(latLng);
				final double wt = wts[k];
				ttlWeightedEastOffsetR += flatLatLngs[k].getEastOffsetR() * wt;
				ttlWeightedNorthOffsetR += flatLatLngs[k].getNorthOffsetR() * wt;
			}
			final double ttlOffsetSquaredRR = //
					ttlWeightedEastOffsetR * ttlWeightedEastOffsetR + //
							ttlWeightedNorthOffsetR * ttlWeightedNorthOffsetR;
			final double offsetR = Math.sqrt(ttlOffsetSquaredRR);
			/**
			 * If we improve, record the TangentCylinder that we used as the best one, but
			 * try a new one based on the offsets.
			 */
			if (offsetR < bestOffsetR) {
				bestTangentCylinder = tangentCylinder;
				bestFlatLatLngs = flatLatLngs.clone();
				bestOffsetR = offsetR;
				final double nmiToR = Constants0.Functions.nmiToR(tangentCylinder.getCentralFlatLatLng().getLat());
				final double offsetM = offsetR / nmiToR * Constants0._NmiToM;
				if (offsetM <= _MaxMeanOffsetM) {
					break;
				}
				/** Create a new TangentCylinder centered at the new center of mass. */
				final FlatLatLng centerOfMassLatLng = bestTangentCylinder.new FlatLatLng(ttlWeightedEastOffsetR,
						ttlWeightedNorthOffsetR);
				tangentCylinder = new TangentCylinder(centerOfMassLatLng);
			}
		}
		return bestFlatLatLngs;
	}

	/** The (private) ctor. Called by the getTangentCylinder methods. */
	private TangentCylinder(final LatLng latLng) {
		final double lat = latLng.getLat();
		_centralLng = latLng.getLng();
		final double latRads = Math.toRadians(lat);
		_cLat = MathX0.cosX(latRads);
		_sLat = MathX0.sinX(latRads);
		_poleIsNorth = latRads >= 0d;
		final double poleLat = _poleIsNorth ? 90d : -90d;
		_poleFlatLatLng = convertToMyFlatLatLng(LatLng.getLatLng(poleLat, _centralLng));
		_centralFlatLatLng = new FlatLatLng(0d, 0d);
		_nmiToR = Constants0.Functions.nmiToR(_centralFlatLatLng.getLat());
	}

	public FlatLatLng getCentralFlatLatLng() {
		return _centralFlatLatLng;
	}

	@Override
	public int compare(final LatLng latLng0, final LatLng latLng1) {
		if ((latLng0 == null) != (latLng1 == null)) {
			return latLng0 == null ? -1 : 1;
		}
		if (latLng0 == null) {
			return 0;
		}
		final FlatLatLng fl0 = convertToMyFlatLatLng(latLng0);
		final FlatLatLng fl1 = convertToMyFlatLatLng(latLng1);
		final double east0 = fl0.getEastOffsetR(), east1 = fl1.getEastOffsetR();
		if (east0 != east1) {
			return east0 < east1 ? -1 : 1;
		}
		final double north0 = fl0.getNorthOffsetR(), north1 = fl1.getNorthOffsetR();
		return north0 < north1 ? -1 : (north0 > north1 ? 1 : 0);
	}

	/**
	 * Given an arbitrary LatLng3, make one that represents the same point, but is
	 * stored in reference to me.
	 */
	public FlatLatLng convertToMyFlatLatLng(final LatLng latLng) {
		if (latLng instanceof final FlatLatLng flatLatLng) {
			if (flatLatLng.getOwningTangentCylinder() == this) {
				return flatLatLng;
			}
		}
		final double lat = latLng.getLat(), lng = latLng.getLng();
		return new FlatLatLng(lat, lng, /* dummy= */' ');
	}

	public static List<LatLng> makeListOfBasicLatLngs(final List<TangentCylinder.FlatLatLng> listOfFlatLatLngs) {
		final int n = listOfFlatLatLngs.size();
		final ArrayList<LatLng> listOfBasics = new ArrayList<>(n);
		for (int k = 0; k < n; ++k) {
			listOfBasics.add(LatLng.makeBasicLatLng3(listOfFlatLatLngs.get(k)));
		}
		return listOfBasics;
	}

	/** This is the key routine that goes from the plane back to lat/lng. */
	private double[] getLatAndLng(final double eastOffset, final double northOffset) {
		final double[] bX;
		if (_UseStereographic) {
			/**
			 * <pre>
			 * It's relatively straightforward to prove the following:
			 * Let dSquared = eSquared + nSquared. Then, define
			 * x, y, and z as follows:
			 *   x = (4-dSquared)/(4+dSquared)
			 *   y = (x+1)e/2
			 *   z = (x+1)n/2.
			 * Then ||(x,y,z|| = 1. This is how we convert back.
			 * </pre>
			 */
			final double dSquared = eastOffset * eastOffset + northOffset * northOffset;
			final double x = (4d - dSquared) / (4d + dSquared);
			final double scale = (x + 1d) / 2d;
			final double y = scale * eastOffset;
			final double z = scale * northOffset;
			bX = new double[] { x, y, z };
		} else if (_UseGnomonic) {
			bX = new double[] { 1d, eastOffset, northOffset };
			NumericalRoutines.convertToUnitLength(bX);
		} else {
			return null;
		}
		/**
		 * Convert bX back to normal coordinates so we can then get lat and lng.
		 */
		final double[] b = { //
				bX[0] * _cLat - _sLat * bX[2], //
				bX[1], //
				_sLat * bX[0] + _cLat * bX[2]//
		};
		/** b gets normalized. */
		final double[] latLngArray = MathX0.toLatLngArrayX(b);
		/**
		 * The first thing we do when we convert to our coordinate system is to subtract
		 * off the reference longitude. Hence the last thing we do when we convert from
		 * our coordinate system is to add this reference longitude back.
		 */
		latLngArray[1] += _centralLng;
		latLngArray[1] = ModFunctions.getInRange180_180(latLngArray[1]);
		return latLngArray;
	}

	public class FlatLatLng extends LatLng implements Cloneable {
		final private double _northOffsetR, _eastOffsetR;

		final private int _tcLatUnits;
		final private int _tcLngUnits;
		final private double _tcSLat;
		final private double _tcCLat;
		final private double _tcSLng;
		final private double _tcCLng;

		@Override
		final public int getLatUnits() {
			return _tcLatUnits;
		}

		@Override
		final public int getLngUnits() {
			return _tcLngUnits;
		}

		@Override
		final public double getSLat() {
			return _tcSLat;
		}

		@Override
		final public double getCLat() {
			return _tcCLat;
		}

		@Override
		final public double getSLng() {
			return _tcSLng;
		}

		@Override
		final public double getCLng() {
			return _tcCLng;
		}

		public FlatLatLng(final double eastOffset, final double northOffset) {
			_eastOffsetR = eastOffset;
			_northOffsetR = northOffset;
			final double[] latAndLng = getLatAndLng(eastOffset, northOffset);
			final LatLng latLng = LatLng.getLatLngC2(latAndLng);
			_tcLatUnits = latLng.getLatUnits();
			_tcLngUnits = latLng.getLngUnits();
			_tcSLat = latLng.getSLat();
			_tcCLat = latLng.getCLat();
			_tcSLng = latLng.getSLng();
			_tcCLng = latLng.getCLng();
		}

		/** This is the key routine that goes from lat/lng to the plane. */
		private FlatLatLng(final double lat, final double lng, final char dummy) {
			/** Handy handles. */
			final double c = TangentCylinder.this._cLat;
			final double s = TangentCylinder.this._sLat;
			final double lngX = lng - _centralLng;
			/** Get the normal Cartesian coordinates. */
			final double[] b = MathX0.toCartesianX(lat, lngX);
			/**
			 * Convert to strange Cartesian coordinates. The basis, and its use, are
			 * discussed in the comments for the fields _cLat and _sLat.
			 */
			final double[] twB = { //
					b[0] * c + b[2] * s, //
					b[1], //
					-b[0] * s + b[2] * c //
			};
			final double alpha;
			if (_UseStereographic) {
				alpha = 2d / (twB[0] + 1d);
			} else {
				alpha = 1d / twB[0];
			}
			final double eastOffset = alpha * twB[1];
			final double northOffset = alpha * twB[2];
			final FlatLatLng flatLatLng = new FlatLatLng(eastOffset, northOffset);
			_eastOffsetR = flatLatLng._eastOffsetR;
			_northOffsetR = flatLatLng._northOffsetR;
			_tcLatUnits = flatLatLng._tcLatUnits;
			_tcLngUnits = flatLatLng._tcLngUnits;
			_tcSLat = flatLatLng._tcSLat;
			_tcCLat = flatLatLng._tcCLat;
			_tcSLng = flatLatLng._tcSLng;
			_tcCLng = flatLatLng._tcCLng;
		}

		public FlatLatLng(final double[] eastNorthOffsets) {
			final FlatLatLng flatLatLng = new FlatLatLng(eastNorthOffsets[0], eastNorthOffsets[1]);
			_eastOffsetR = flatLatLng._eastOffsetR;
			_northOffsetR = flatLatLng._northOffsetR;
			_tcLatUnits = flatLatLng._tcLatUnits;
			_tcLngUnits = flatLatLng._tcLngUnits;
			_tcSLat = flatLatLng._tcSLat;
			_tcCLat = flatLatLng._tcCLat;
			_tcSLng = flatLatLng._tcSLng;
			_tcCLng = flatLatLng._tcCLng;
		}

		/**
		 * @param eastOffsetR  in earthRadii of the owning TangentCylinder, 90 degrees
		 *                     from north.
		 * @param northOffsetR in earthRadii of the owning TangentCylinder, towards
		 *                     north. interprets the addition of offsets relative to the
		 *                     direction to the pole.
		 */
		public FlatLatLng addOffsets(final double eastOffsetR, final double northOffsetR) {
			final double[] xy = getUnitVectorTowardsNorthPole();
			final double x = xy[0], y = xy[1];
			/**
			 * (x,y) is now a (unitless) unit vector pointed from this towards the pole.
			 * We'll multiply (x,y) by northOffset and that will be the move corresponding
			 * to northOffset. A hard right from that direction, multiplied by eastOffset
			 * will be the move corresponding to eastOffset.
			 */
			final double xPerp = y;
			final double yPerp = -x;
			final double totalEastOffsetR = _eastOffsetR + (northOffsetR * x + eastOffsetR * xPerp);
			final double totalNorthOffsetR = _northOffsetR + (northOffsetR * y + eastOffsetR * yPerp);
			return TangentCylinder.this.new FlatLatLng(totalEastOffsetR, totalNorthOffsetR);
		}

		public double[] getUnitVectorTowardsNorthPole() {
			/**
			 * "towards the north pole" means just that if the reference point of the
			 * tangent cylinder is in the north, and away from the south pole if the
			 * reference point is in the south.
			 */
			double x = _poleFlatLatLng.getEastOffsetR() - _eastOffsetR;
			double y = _poleFlatLatLng.getNorthOffsetR() - _northOffsetR;
			if (!_poleIsNorth) {
				/**
				 * If we're pointing at the south pole, reverse it to point north.
				 */
				x = -x;
				y = -y;
			}
			final double r = Math.sqrt(x * x + y * y);
			/** If we're at a pole, just add offsets, moving away from the pole. */
			return r <= 0d ? new double[] { 0d, 1d } : new double[] { x / r, y / r };
		}

		public TangentCylinder getOwningTangentCylinder() {
			return TangentCylinder.this;
		}

		public double getEastOffsetR() {
			return _eastOffsetR;
		}

		public double getNorthOffsetR() {
			return _northOffsetR;
		}

		public double getEastOffsetNmi() {
			final double nmiToR = TangentCylinder.this.getNmiToR();
			return _eastOffsetR / nmiToR;
		}

		public double getNorthOffsetNmi() {
			final double nmiToR = TangentCylinder.this.getNmiToR();
			return _northOffsetR / nmiToR;
		}

		public double[] createOffsetPairR() {
			return new double[] { _eastOffsetR, _northOffsetR };
		}
	}

	public static TangentCylinder.FlatLatLng convertToCentered(final LatLng latLng) {
		TangentCylinder.FlatLatLng flatLatLng = null;
		if (latLng instanceof TangentCylinder.FlatLatLng) {
			flatLatLng = (TangentCylinder.FlatLatLng) latLng;
			final TangentCylinder.FlatLatLng flatCenter = flatLatLng.getOwningTangentCylinder().getCentralFlatLatLng();
			if (flatCenter.equals(latLng)) {
				return flatLatLng;
			}
		}
		final TangentCylinder tangentCylinder = getTangentCylinder(latLng);
		return tangentCylinder.getCentralFlatLatLng();
	}

	public double getNmiToR() {
		return _nmiToR;
	}
}
