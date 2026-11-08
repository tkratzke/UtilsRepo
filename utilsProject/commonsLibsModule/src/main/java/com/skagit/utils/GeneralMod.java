package com.skagit.utils;

public class GeneralMod {

	public static void main0(final String[] args) {
		/** The % operator always treats the second argument as if it's positive. */
		final double[][] cases = { { 8d, 3d }, { 8d, -3d }, { -8d, 3d }, { -8d, -3d } };
		final int nCases = cases.length;
		for (int k = 0; k < nCases; ++k) {
			final double[] thisCase = cases[k];
			final double d0 = thisCase[0], d1 = thisCase[1];
			System.out.printf("\n[%f,%f]: %f", d0, d1, d0 % d1);
		}
	}

	public static void main(final String[] args) {
		final int bigMultiplier = 10000000;
		final int[] cases = { 170, -180, -360, -190, 180, 190, 350, 360, 370, 730, 0, -720, -730 };
		final int nCases = cases.length;
		for (int k = 0; k < nCases; ++k) {
			final int loUnits = -180 * bigMultiplier, hiUnits = 180 * bigMultiplier;
			final int theCase = cases[k];
			final long kUnits = cases[k] * (long) bigMultiplier;
			System.out.printf("\n[%d,%d]: %d:: %d", -180, 180, theCase,
					generalMod(loUnits, hiUnits, (int) kUnits) / bigMultiplier);
		}
	}

	public static double generalMod(final double lo, final double hi, final double d0) {
		final double remainder = (d0 - lo) % (hi - lo);
		return remainder + (remainder < 0d ? hi : lo);
	}

	public static int generalMod(final int lo, final int hi, final long ell0) {
		final long remainder = (ell0 - lo) % ((long) hi - lo);
		return (int) (remainder) + (remainder < 0 ? hi : lo);
	}

	public static long generalMod(final long loEll, final long hiEll, final long ell0) {
		final long remainder = (ell0 - loEll) % (hiEll - loEll);
		return remainder + (remainder < 0 ? hiEll : loEll);
	}

}
