package com.skagit.utils;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;

public class MiscFormatting {
	/** UTF-8 seems to work well within SAROPS. */
	final public static String _CharsetName = "UTF-8";
	final private static int _NDigitsForStandardDouble = 17;
	final private static int _NDigitsForLatorLng = 7;

	public static String StandardFormat(final boolean b) {
		final String s = String.valueOf(b);
		return s;
	}

	public static String StandardFormat(final double d) {
		return StandardFormat(d, _NDigitsForStandardDouble);
	}

	public static String StandardFormatForLatOrLng(final double latOrLng) {
		if (!(-180d <= latOrLng && latOrLng < 180d)) {
			return String.format("%.2g", latOrLng >= 0d ? 1.0e20 : -1.0e20);
		}
		return StandardFormat(latOrLng, _NDigitsForLatorLng);
	}

	private static String StandardFormat(final double d, final int nDigitsInitial) {
		final double absD = Math.abs(d);
		if (!(absD < Long.MAX_VALUE)) {
			return String.format("%.2g", d >= 0d ? 1.0e20 : -1.0e20);
		}
		if (absD >= 1d) {
			final long intPart = Math.round(Math.floor(absD));
			final String intPartString = String.format("%s%d", d < 0 ? "-" : "", intPart);
			final double fracPart = absD - intPart;
			if (!(0d <= fracPart && fracPart < 1d)) {
				/** Something is gummed up. Must be a huge number. */
				return String.format("%.2g", d >= 0d ? 1.0e20 : -1.0e20);
			}
			if (fracPart <= 0d) {
				return intPartString;
			}
			final String fracPartStringX = StandardFormat(fracPart);
			final int dot = fracPartStringX.indexOf('.');
			if (dot >= fracPartStringX.length() - 1) {
				return intPartString;
			}
			final String fracPartString = fracPartStringX.substring(dot + 1);
			final String s = String.format("%s.%s", intPartString, fracPartString);
			return s;
		}
		for (int nDigits = nDigitsInitial;;) {
			final BigDecimal bigDecimal1 = new BigDecimal(d, new MathContext(nDigits, RoundingMode.HALF_UP));
			final BigDecimal bigDecimal2 = bigDecimal1.stripTrailingZeros();
			final char[] patternChars = new char[2 + nDigits];
			patternChars[0] = '0';
			patternChars[1] = '.';
			for (int k = 0; k < nDigits; ++k) {
				patternChars[2 + k] = '#';
			}
			final String pattern = new String(patternChars);
			final DecimalFormat decimalFormat = new DecimalFormat(pattern);
			final String s1 = decimalFormat.format(bigDecimal2);
			final int dot = s1.indexOf('.');
			if (dot < 0) {
				return s1;
			}
			final int k0 = s1.indexOf("0000000");
			if (k0 > dot) {
				nDigits = k0 - dot;
				continue;
			}
			final int k9 = s1.indexOf("9999999");
			if (k9 > dot) {
				nDigits = k9 - dot + 1;
				continue;
			}
			return s1;
		}
	}

	final private static double _Eps = 1.0e-7;
	final public static String _BadNumberString = "--";
	final private static String _BadPercentString = _BadNumberString + '%';
	final private static String _MagicFormat = "%.0f%%";
	final private static String _ZeroString = String.format(_MagicFormat, 0d);
	final private static String _SmallString = "<0.5%";
	final private static String _BigString = ">99.5%";
	final private static String _OneHunderedString = String.format(_MagicFormat, 100d);

	public static String StandardPercentFormat(final double[] numDen) {
		final double num = numDen[0], den = numDen[1];
		/** Check for bad. */
		if (!(0d <= num && num < Double.MAX_VALUE) || !(0d < den && den < Double.MAX_VALUE)) {
			return _BadPercentString;
		}
		/**
		 * num is at least 0, den is bigger than 0, and both are of reasonable size.
		 */
		final double q = (100d * num) / den;
		if (!(0d <= q && q <= 100d + _Eps)) {
			/** It's bad. */
			return _BadPercentString;
		} else if (num == 0d) {
			/** Exactly 0. */
			return _ZeroString;
		} else if (q >= 100d - _Eps) {
			/** "Exactly 100." */
			return _OneHunderedString;
		} else {
			final String s = String.format(_MagicFormat, q);
			if (s.equals(_ZeroString)) {
				/** Not 0, but looks like 0. */
				return _SmallString;
			} else if (s.equals(_OneHunderedString)) {
				/** Not 100, but looks like 100. */
				return _BigString;
			} else {
				return s;
			}
		}
	}

}
