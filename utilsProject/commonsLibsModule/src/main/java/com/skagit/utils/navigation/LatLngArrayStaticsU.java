package com.skagit.utils.navigation;

public class LatLngArrayStaticsU extends LatLngArrayStatics {
	public final static int _NaNUnits = Integer.MIN_VALUE;
	final public static int _DegsToUnits = (int) Math.round(Math.pow(10, _NDigitsForReadWrite));
	final public static int _QuarterCircleOfUnits = _DegsToUnits * 90;
	final public static int _HalfCircleOfUnits = _DegsToUnits * 180;
	/** Note: _FullCircleOfUnits > Integer.MAX_VALUE. */
	final public static long _FullCircleOfUnits = _DegsToUnits * 360L;

	public static int getNDigitsForUnits() {
		return (int) Math.ceil(Math.log10(_DegsToUnits));
	}

	public static int normalizeUnits180_180I(final long uIn) {
		if (-_HalfCircleOfUnits <= uIn && uIn < _HalfCircleOfUnits) {
			return (int) uIn;
		}
		if (uIn < -_HalfCircleOfUnits) {
			final long u = -(-uIn % _FullCircleOfUnits);
			if (u < -_HalfCircleOfUnits) {
				return (int) (u + _FullCircleOfUnits);
			}
			return (int) u;
		}
		final long u = uIn % _FullCircleOfUnits;
		if (u >= _HalfCircleOfUnits) {
			return (int) (u - _FullCircleOfUnits);
		}
		return (int) u;
	}

	public static double simpleUToD180_180I(final int u) {
		return ((double) u) / _DegsToUnits;
	}

	public static int simpleDToU180_180I(final double d) {
		return (int) Math.round(d * _DegsToUnits);
	}

	public static double unitsToDegs180_180I(int u) {
		if (u == _NaNUnits) {
			return Double.NaN;
		}
		u = normalizeUnits180_180I(u);
		return simpleUToD180_180I(u);
	}

	public static int degsToUnits180_180I(final double dIn) {
		if (Double.isNaN(dIn)) {
			return _NaNUnits;
		}
		final double d = getInRange180_180(dIn);
		return simpleDToU180_180I(d);
	}

	public static int unitsToEast180_180I(final int baseUnits, final int units) {
		final long diffUnits = (long) units - baseUnits;
		return normalizeUnits180_180I(diffUnits);
	}

	public static double roundToLattice180_180I(final double dIn) {
		final double d = getInRange180_180(dIn);
		final int u = simpleDToU180_180I(d);
		return simpleUToD180_180I(u);
	}

	public static long normalizeUnits0_360L(final long uIn) {
		if (0L <= uIn && uIn < _FullCircleOfUnits) {
			return uIn;
		}
		if (uIn < 0) {
			final long u = -(-uIn % _FullCircleOfUnits);
			if (u == 0) {
				return 0;
			}
			return _FullCircleOfUnits - u;
		}
		return uIn % _FullCircleOfUnits;
	}

	private static double simpleUToD0_360L(final long u) {
		return ((double) u) / _DegsToUnits;
	}

	private static long simpleDToU0_360L(final double d) {
		return Math.round(d * _DegsToUnits);
	}

	public static double unitsToDegs0_360L(long u) {
		if (u == _NaNUnits) {
			return Double.NaN;
		}
		u = normalizeUnits0_360L(u);
		return simpleUToD0_360L(u);
	}

	public static long degsToUnits0_360L(final double dIn) {
		if (Double.isNaN(dIn)) {
			return _NaNUnits;
		}
		final double d = getInRange0_360(dIn);
		return simpleDToU0_360L(d);
	}

	public static double roundToLattice0_360L(final double dIn) {
		final double d = getInRange0_360(dIn);
		final long u = simpleDToU0_360L(d);
		return simpleUToD0_360L(u);
	}

	public static long unitsToEast0_360L(final long baseUnits, final long units) {
		final long diffUnits = units - baseUnits;
		return normalizeUnits0_360L(diffUnits);
	}

	public static double degsToEast0_360L(final double baseDegs, final double degs) {
		final double diff = getInRange0_360(degs - baseDegs);
		final double d = roundToLattice0_360L(diff);
		if (degs != baseDegs && d == 0d) {
			return 360d;
		}
		return d;
	}

	public static int[] getNormalizedUnits(final int latUIn, final int lngUIn) {
		int latU = normalizeUnits180_180I(latUIn);
		int lngU = lngUIn;
		/** Latitude's between -180 and 180. We want it between -90 and 90. */
		if (latU < -_QuarterCircleOfUnits || latU > _QuarterCircleOfUnits) {
			if (lngU < 0) {
				lngU += _HalfCircleOfUnits;
			} else {
				lngU -= _HalfCircleOfUnits;
			}
			if (latU < -_QuarterCircleOfUnits) {
				latU = -_HalfCircleOfUnits - latU;
			} else {
				latU = _HalfCircleOfUnits - latU;
			}
		}
		if (latU == _QuarterCircleOfUnits || latU == -_QuarterCircleOfUnits) {
			lngU = 0;
		} else {
			lngU = normalizeUnits180_180I(lngU);
		}
		return new int[] { latU, lngU };
	}

	/**
	 * Assumes units have already been normalized. Returns the delta-to-the-right.
	 * For examples, (179,-179) return 2 and (-179,179) returns -2.
	 */
	public static int getDeltaLngUnits(final int fromLngUnits, final int toLngUnits) {
		final int min = Math.min(fromLngUnits, toLngUnits);
		final int max = Math.max(fromLngUnits, toLngUnits);
		final int absDelta;
		final boolean crossesIdl;
		if (min >= 0 || max <= 0) {
			absDelta = max - min;
			crossesIdl = false;
		} else {
			/** Because min < 0, I can safely add 180 to it. */
			final int a = min + _HalfCircleOfUnits;
			if (a >= max) {
				absDelta = max - min;
				crossesIdl = false;
			} else {
				/**
				 * <pre>
				 * We want min+360-max or a+180-max or a-(max-180).
				 * Because max > 0, I can safely subtract 180 from it.
				 * </pre>
				 */
				final int b = max - _HalfCircleOfUnits;
				absDelta = a - b;
				crossesIdl = true;
			}
		}
		if (fromLngUnits == min && crossesIdl) {
			return -absDelta;
		}
		if ((fromLngUnits == min && !crossesIdl) || crossesIdl) {
			return absDelta;
		}
		return -absDelta;
	}

}
