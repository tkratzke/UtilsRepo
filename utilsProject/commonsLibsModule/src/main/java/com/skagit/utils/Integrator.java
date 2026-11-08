package com.skagit.utils;

public class Integrator {

    public interface IntegrableFunction {
	double valueAt(double x);
    }

    public static double lowerRiemann(final IntegrableFunction f, final double a, final double b, final int N) {
	double x = a;
	final double h = (b - a) / N;
	double sum = 0, fx1, fx2, fx;
	for (int i = 0; i < N; i++) {
	    fx1 = f.valueAt(x);
	    fx2 = f.valueAt(x + h);
	    fx = fx1 < fx2 ? fx1 : fx2;
	    sum += fx;
	    x += h;
	}
	sum *= h;
	return sum;
    }

    public static double upperRiemann(final IntegrableFunction f, final double a, final double b, final int N) {
	double x = a;
	final double h = (b - a) / N;
	double sum = 0, fx1, fx2, fx;
	for (int i = 0; i < N; i++) {
	    fx1 = f.valueAt(x);
	    fx2 = f.valueAt(x + h);
	    fx = fx1 < fx2 ? fx2 : fx1;
	    sum += fx;
	    x += h;
	}
	sum *= h;
	return sum;
    }

    public static double trapez(final IntegrableFunction f, final double a, final double b, final int N) {
	double x = a;
	final double h = (b - a) / N;
	double sum = f.valueAt(x);
	for (int i = 0; i < N - 1; i++) {
	    x += h;
	    sum += 2 * f.valueAt(x);
	}
	sum += f.valueAt(b);
	sum *= (h / 2);
	return sum;
    }

    public static double midpoint(final IntegrableFunction f, final double a, final double b, final int N) {
	final double h = (b - a) / N;
	double x = a + 0.5 * h, sum = 0;
	for (int i = 0; i < N; i++) {
	    sum += f.valueAt(x);
	    x += h;
	}
	sum *= h;
	return sum;
    }

    public static double[] trapezPowerOfTwo(final IntegrableFunction f, final double a, final double b, final int N) {
	final double[] values = new double[N];
	double h = (b - a), sum = 0, x = 0;
	values[0] = 0.5 * (f.valueAt(a) + f.valueAt(b)) * h;
	for (int i = 1; i < N; i++) {
	    values[i] = 0.5 * values[i - 1];
	    sum = 0;
	    x = a + 0.5 * h;
	    sum = 0;
	    do {
		sum += f.valueAt(x);
		x += h;
	    } while (x < b);
	    sum *= h / 2;
	    values[i] += sum;
	    h *= 0.5;
	}
	return values;
    }

    public static double[][] romberg(final IntegrableFunction f, final Object util, final double a, final double b,
	    final int N) {
	final double[][] values = new double[N][];
	for (int i = 0; i < N; i++) {
	    values[i] = new double[N];
	}
	values[0] = trapezPowerOfTwo(f, a, b, N);
	int p = 4;
	for (int i = 1; i < N; i++) {
	    for (int j = 0; j < N; j++) {
		if (j < i) {
		    values[i][j] = 0;
		} else {
		    values[i][j] = values[i - 1][j] + (values[i - 1][j] - values[i - 1][j - 1]) / (p - 1);
		}
	    }
	    p *= 4;
	}
	return values;
    }

    public static double simpsonApproximation(final IntegrableFunction f, final double a, final double b,
	    final int nIntervals0) {
	/** nIntervals must be even. */
	final int nIntervals = nIntervals0 + (nIntervals0 % 2 == 0 ? 0 : 1);
	final double h = (b - a) / nIntervals;
	/**
	 * <pre>
	 *  Endpoints are a+kh, for k = 0, ..., nIntervals.
	 * For k = 0 or nIntervals, multiply by 1.
	 * Otherwise:
	 *   if k is odd, multiply by 4 and
	 *   if k is even, multiply by 2.
	 * </pre>
	 */
	double toMultiplyBy1 = 0d;
	double toMultiplyBy2 = 0d;
	double toMultiplyBy4 = 0d;
	for (int k = 0; k <= nIntervals; ++k) {
	    /**
	     * We assume the endpoints, as given, are not singular. To re-compute the
	     * endpoints via k * h, is asking for trouble.
	     */
	    final double x = k == 0 ? a : (k == nIntervals ? b : (a + k * h));
	    final double value = f.valueAt(x);
	    if (k == 0 || k == nIntervals) {
		toMultiplyBy1 += value;
	    } else if (k % 2 == 1) {
		toMultiplyBy4 += value;
	    } else {
		toMultiplyBy2 += value;
	    }
	}
	final double sum = toMultiplyBy1 + 2d * toMultiplyBy2 + 4d * toMultiplyBy4;
	final double returnValue = sum * h / 3d;
	return returnValue;
    }

    public static double simpson(final IntegrableFunction f, double a, double b, final double relativeErrorThreshold) {
	/**
	 * Make sure that a and b are real numbers or infinite, and that a < b.
	 */
	if (Double.isNaN(a) || Double.isNaN(b)) {
	    return Double.NaN;
	}
	if (b == a) {
	    return 0d;
	}
	if (a > b) {
	    final double x = a;
	    a = b;
	    b = x;
	}
	/**
	 * The straightforward case is when they are both finite. n is the number of
	 * intervals, which must be even.
	 */
	if (Double.isFinite(a) && Double.isFinite(b)) {
	    double try1 = Double.NaN, try2 = try1;
	    for (long n = 2;; n = Math.min(2L * n, Integer.MAX_VALUE)) {
		try2 = simpsonApproximation(f, a, b, (int) n);
		if (!Double.isNaN(try1)) {
		    if (NumericalRoutines.relativeErrorIsSmall(try1, try2, relativeErrorThreshold)) {
			final double returnValue = (try1 + try2) / 2d;
			return returnValue;
		    }
		}
		try1 = try2;
	    }
	}

	/**
	 * <pre>
	 * At least one of the limits is infinite. We use the following:
	 * If ab > 0, then:
	 *   the integral from a to b of f(x) is equal to:
	 *   the integral from 1/b to 1/a of (1/(tt) * f(1/t)).
	 * We start by forming the function:
	 *   fX(t) = 1/(tt) * f(1/t),
	 * Then we decompose the infinite domain and integrate
	 * f and/or fX, but always with finite limits.
	 *
	 * If a or b is 0, then we have trouble with 1/tt and f(1/t),
	 * but we assume that any trouble indicates that 1/tt * f(1/t)
	 * really is 0 (or the limit is 0).
	 *
	 * With this assumption, we can handle a or b is 0.
	 *
	 * </pre>
	 */

	final IntegrableFunction fX = new IntegrableFunction() {

	    @Override
	    public double valueAt(final double t) {
		/** We assume that any NaN really should be 0. */
		if (t == 0d) {
		    return 0d;
		}
		final double tInv = 1d / t;
		if (Double.isNaN(tInv)) {
		    return 0d;
		}
		final double tInvSq = 1d / (t * t);
		if (Double.isNaN(tInvSq)) {
		    return 0d;
		}
		final double fOfTInv = f.valueAt(tInv);
		if (Double.isNaN(fOfTInv)) {
		    return 0d;
		}
		final double value = tInvSq * fOfTInv;
		if (Double.isNaN(value)) {
		    return 0d;
		}
		return value;
	    }
	};
	final double relativeErrorThreshold2 = relativeErrorThreshold / 2d;
	final double relativeErrorThreshold3 = relativeErrorThreshold / 3d;
	if (a < 0d) {
	    if (b < 0d) {
		/** a = -inf, b is finite and less than 0. */
		return simpson(fX, 1d / b, 0d, relativeErrorThreshold);
	    } else if (b == 0d) {
		/** a = -inf, b = 0. */
		final double integral0 = simpson(fX, -1d, 0d, relativeErrorThreshold2);
		final double integral1 = simpson(f, -1d, 0d, relativeErrorThreshold2);
		return integral0 + integral1;
	    } else {
		/** a < 0, b > 0, at least one is infinite. */
		if (Double.isFinite(a)) {
		    /** b = +inf. */
		    final double integral0 = simpson(f, a, 1d, relativeErrorThreshold2);
		    final double integral1 = simpson(fX, 0d, 1d, relativeErrorThreshold2);
		    return integral0 + integral1;
		}
		/** a = -inf, and b > 0. */
		if (Double.isFinite(b)) {
		    final double integral0 = simpson(fX, -1d, 0d, relativeErrorThreshold2);
		    final double integral1 = simpson(f, -1d, b, relativeErrorThreshold2);
		    return integral0 + integral1;
		}
		/** a = -inf, b = +inf. */
		final double integral0 = simpson(fX, -1d, 0d, relativeErrorThreshold3);
		final double integral1 = simpson(f, -1d, 1d, relativeErrorThreshold3);
		final double integral2 = simpson(fX, 0d, 1d, relativeErrorThreshold3);
		return integral0 + integral1 + integral2;
	    }
	} else if (a == 0d) {
	    /** b must be +inf. */
	    final double integral0 = simpson(f, 0d, 1d, relativeErrorThreshold2);
	    final double integral1 = simpson(fX, 0d, 1d, relativeErrorThreshold2);
	    return integral0 + integral1;
	} else {
	    /** a > 0. a cannot be infinite since 0 < a < b. Hence, b = +inf. */
	    return simpson(fX, 0d, 1d / a, relativeErrorThreshold);
	}
    }

    /**
     * A second degree polynomial that passes through the points (x0,y0), (x0+h,y1),
     * and (x0+2h,y2):
     */
    private static IntegrableFunction Par(final double x0, final double h, final double y0, final double y1,
	    final double y2) {
	return new IntegrableFunction() {
	    @Override
	    public double valueAt(final double x) {
		return y0 + (x - x0) * ((y1 - y0) / h + (x - x0 - h) * (y0 - 2 * y1 + y2) / (2 * h * h));
	    }
	};
    }

    /**
     * A second degree polynomial that passes through the points (x0,f(x0)),
     * (x0+h,f(x0+h)) and (x0+2h,f(x0+2h)):
     */
    @SuppressWarnings("unused")
    private static IntegrableFunction Pa(final double x0, final double h, final IntegrableFunction f) {
	return new IntegrableFunction() {
	    @Override
	    public double valueAt(final double x) {
		return Par(x0, h, f.valueAt(x0), f.valueAt(x0 + h), f.valueAt(x0 + 2d * h)).valueAt(x);
	    }
	};
    }

    public static void main(final String[] args) {
	final IntegrableFunction f = new IntegrableFunction() {
	    @Override
	    public double valueAt(final double x) {
		return Math.cos(x);
	    }
	};
	final double d = simpson(f, 0d, Math.PI / 2d, /* relativeErrorThreshold= */1.0e10);
	System.out.printf("\n%f", d);
    }
}
