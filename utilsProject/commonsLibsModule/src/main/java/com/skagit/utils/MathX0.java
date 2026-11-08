package com.skagit.utils;

import org.apache.commons.math3.util.FastMath;

/** Routines in this class do not error check the inputs. */
public class MathX0 {
	public static native double sinY(final double a);

	/** Wrappers. */
	public static double sinX(final double a) {
		return FastMath.sin(a);
	}

	public static double cosX(final double a) {
		return FastMath.cos(a);
	}

	public static double tanX(final double a) {
		return FastMath.tan(a);
	}

	public static double acosX(final double a) {
		return FastMath.acos(a);
	}

	public static double asinX(final double a) {
		return FastMath.asin(a);
	}

	public static double atanX(final double a) {
		return FastMath.atan(a);
	}

	public static double atan2X(final double y, final double x) {
		return FastMath.atan2(y, x);
	}
}
