/*
 * Created on Nov 14, 2003
 */
package com.skagit.utils;

import java.util.Arrays;
import java.util.Comparator;

public interface Constants0 {
	/**
	 * <pre>
	 * For reference:
	 * Byte.MIN_VALUE  -2^7 = -128
	 * Byte.MAX_VALUE  2^7-1 = 127
	 * short.MIN_VALUE  -2^15 = -32768
	 * Short.MAX_VALUE  2^15-1 = 32767
	 * Integer.MIN_VALUE  -2^31 = -2147483648
	 * Integer.MAX_VALUE  2^31-1 = 2147483647
	 * Long.MIN_VALUE  -2^63 = -9223372036854775808
	 * Long.MAX_VALUE  2^63-1 = 9223372036854775807
	 * </pre>
	 */
	/**
	 * .gitignore documentation: https://git-scm.com/docs/gitignore Egit
	 * Documentation: https://www.eclipse.org/egit/documentation/
	 */

	char _FileSeparatorChar = StringUtilities.getSystemProperty("file.separator", /* useSpaceProxy= */false).charAt(0);
	String _NewLine = StringUtilities.getSystemProperty("line.separator", /* useSpaceProxy= */false);
	double _AcresToSqFt = 43560.17;

	/**
	 * <pre>
	 * Note; to type:
	 * a degree symbol into the code (including within Strings),
	 * make sure numLock is on, hold down alt, and type,
	 * on the numeric keypad:
	 * °	0176
	 * →	26
	 * ∞	236
	 * ..., See: https://www.alt-codes.net/
	 * Unicode characters are different.  These are just ASCII characters.
	 * </pre>
	 */
	char _DegreeSymbol = '°';
	char _CheckMark = '\u2713';
	char _UpwardsArrow = '\u2191';
	char _DownwardsArrow = '\u2193';
	char _RightArrow = '\u2192';
	char _LeftArrow = '\u2190';
	char _LeftRightArrow = '\u2194';
	char _UpDownArrow = '\u2195';
	char _Ellipsis = '\u2026';
	char _HeavyCheckMark = '\u2714';
	char _SectionSymbol = '\u00A7';
	char _DaggerSymbol = '\u2020';
	char _MultiplicationX = '\u2715';
	char _InfSymbol = '\u221e';
	char _GreaterThanOrEqual = '\u2265';
	char _LessThanOrEqual = '\u2264';
	char _CapUpsilon = '\u03A5';
	char _CapPhi = '\u03A6';
	char _CapBeta = '\u0392';
	char _PlusOrMinus = '\u00B1';
	char _EmptySet = '\u2205';
	char _IsMemberOf = '∈';
	char _IsNotMemberOf = '\u2209';
	double _PiOver4 = Math.PI / 4d;
	double _PiOver3 = Math.PI / 3d;
	double _PiOver2 = Math.PI / 2d;
	double _5PiOver12 = Math.PI * 5d / 12d;
	double _2PiOver3 = Math.PI * 2d / 3d;
	double _3PiOver4 = Math.PI * 3d / 4d;
	double _5PiOver4 = Math.PI * 5d / 4d;
	double _3PiOver2 = Math.PI * 3d / 2d;
	double _7PiOver4 = Math.PI * 7d / 4d;
	double _2Pi = 2d * Math.PI;
	double _4Pi = 4d * Math.PI;

	/**
	 * The following are from Department of Defense World Geodetic System 1984, NIMA
	 * TR 8350.2.
	 */
	double _Wgs84RToMEq = 6378137d;
	double _OneOverWgs84Flattening = 298.257223563;
	double _Wgs84Flattening = 1d / _OneOverWgs84Flattening;
	double _MeanRToM = 6371008.7714;

	double _Wgs84RToMPo = _Wgs84RToMEq * (1d - _Wgs84Flattening);

	/** l inch is defined as 0.0254 meters. */
	double _MToFeet = 1d / 0.0254 / 12d;
	double _NmiToM = 1852d;
	double _SqKmToSqNmi = 1000000d / (_NmiToM * _NmiToM);
	double _MpsToKts = 3600d / _NmiToM;

	/**
	 * The 3 choices for a constant conversion between Nmi and Radians. MathX uses
	 * _NmiToRAuthalic and everywhere else references MathX.
	 */
	double _NmiToR60 = Math.PI / 10800d;
	double _NmiToRMean = _NmiToM / _MeanRToM;
	double _NmiToRAuthalic = 1852d / 6371007.2;

	/** For reference; how different are the radii? */
	double _MeanToEq = _MeanRToM / _Wgs84RToMEq;
	double _MeanToPo = _MeanRToM / _Wgs84RToMPo;
	double _MeanTo60 = _MeanRToM * (_NmiToR60 / _NmiToM);
	double _EqToPo = _Wgs84RToMEq / _Wgs84RToMPo;
	double _MeanTo45 = _NmiToRMean / Functions.nmiToR(45d);
	double _MeanToAuthalic = _NmiToRMean / _NmiToRAuthalic;

	/** Just for interest. */
	double _MToRAuthalic = _NmiToRAuthalic / _NmiToM;

	int _GoodRadixForIntegerMax = (int) Math.pow(10, Math.floor(Math.log10(Integer.MAX_VALUE)));

	boolean _AssertOn = Functions._FunctionsAssertOn;
	boolean _DebugOn = Functions._FunctionsDebugOn;

	public static class Functions {
		/** To fake out the compiler. */
		public static boolean returnTrue() {
			return true;
		}

		/** Make sure that MathX gets loaded. */
		@SuppressWarnings("unused")
		final private static double _Dummy = 90d - Math.toDegrees(MathX0.atan2X(8d, 3d));
		final private static boolean _FunctionsAssertOn;
		final private static boolean _FunctionsDebugOn;
		static {
			/**
			 * The following is a fairly clever way of identifying whether or not the
			 * "assert" is turned on. Do NOT change the 2nd statement below even though the
			 * compiler warns that it might be an error.
			 */
			boolean assertOn = false;
			assert assertOn = true;
			_FunctionsAssertOn = assertOn;
			_FunctionsDebugOn = java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments().toString()
					.indexOf("-agentlib:jdwp") > 0;
		}

		final public static double nmiToR(final double lat) {
			final double latRads = Math.toRadians(lat);
			final double cos = MathX0.cosX(latRads);
			final double sin = MathX0.sinX(latRads);
			final double num1 = _Wgs84RToMEq * _Wgs84RToMEq * cos;
			final double num2 = _Wgs84RToMPo * _Wgs84RToMPo * sin;
			final double num = num1 * num1 + num2 * num2;
			final double den1 = _Wgs84RToMEq * cos;
			final double den2 = _Wgs84RToMPo * sin;
			final double den = den1 * den1 + den2 * den2;
			final double ratio = num / den;
			final double earthRadiiToM = Math.sqrt(ratio);
			return _NmiToM / earthRadiiToM;
		}

		public static double log(final double arg, final double base) {
			return Math.log(arg) / Math.log(base);
		}

		private static class Triplet {
			final private String _caption;
			final private long _ell;
			final private double _d;

			private Triplet(final String caption, final long ell) {
				_caption = caption;
				_ell = ell;
				_d = Double.longBitsToDouble(ell);
			}

			private Triplet(final String caption, final double d) {
				_caption = caption;
				_d = d;
				_ell = Double.doubleToLongBits(d);
			}
		}

		private static double doNothing(final double d) {
			return d;
		}

		private static void dump(final Triplet[] triplets) {
			final int nOfInterest = triplets.length;
			int maxLen = 0;
			for (int k = 0; k < nOfInterest; ++k) {
				maxLen = Math.max(maxLen, triplets[k]._caption.length());
			}
			System.out.printf("\n\n%2d. %" + maxLen + "s: %16.9g, %16X", 0, triplets[0]._caption, triplets[0]._d,
					triplets[0]._ell);
			for (int k = 1; k < nOfInterest; ++k) {
				final String caption = triplets[k]._caption;
				final long ell = triplets[k]._ell;
				final double d = triplets[k]._d;
				final long diff = ell - triplets[k - 1]._ell;
				System.out.printf("\n%2d. %" + maxLen + "s: %16.9g, %16X, diff[%X]", k, caption, d, ell, diff);
			}
		}

		public static void oldMain(final String[] args) {
			System.out.printf("\nMeanToPolar:%f\nMeanToEquatorial:%f\nMeanTo21600:%f\nMeanTo45:%f", _MeanToPo,
					_MeanToEq, _MeanTo60, _MeanTo45);
			final double maxDbl = Double.MAX_VALUE;
			final double minPos = Double.MIN_VALUE;
			final double posInf = Double.POSITIVE_INFINITY;
			final double negInf = Double.NEGATIVE_INFINITY;
			final double a = doNothing(0d);
			final double b = doNothing(-0d);
			System.out.printf("\n0[%f]", a);
			System.out.printf("\n-0[%f]", b);
			if (a == b) {
				System.out.println("\n0 == -0");
			} else {
				System.out.println("\n0 != -0");
			}
			final Triplet[] triplets = { new Triplet("LongMIN", Long.MIN_VALUE), //
					new Triplet("-0", -0d), //
					new Triplet("0", 0d), //
					new Triplet("NegInf", negInf), //
					new Triplet("PosInf", posInf), //
					new Triplet("1d", 1d), //
					new Triplet("-1d", -1d), //
					new Triplet("-MinPos", -minPos), //
					new Triplet("MinPos", minPos), //
					new Triplet("MaxDbl", maxDbl), //
					new Triplet("MaxDbl+1", Double.doubleToLongBits(maxDbl) + 1), new Triplet("-MaxDbl", -maxDbl),
					new Triplet("LongMAX", Long.MAX_VALUE) };
			final Comparator<Triplet> comparator1 = (o1, o2) -> {
				if (o1._ell != o2._ell)
					return o1._ell < o2._ell ? -1 : 1;
				return (o1._caption.compareTo(o2._caption));
			};
			Arrays.sort(triplets, comparator1);
			dump(triplets);
			final Comparator<Triplet> comparator2 = (o1, o2) -> {
				final double d1 = o1._d;
				final double d2 = o2._d;
				if (Double.isNaN(d1) != Double.isNaN(d2))
					return Double.isNaN(d1) ? 1 : -1;
				if (Double.isNaN(d1) || d1 == d2)
					return comparator1.compare(o1, o2);
				return d1 < d2 ? -1 : 1;
			};
			Arrays.sort(triplets, comparator2);
			dump(triplets);
		}
	}
}
