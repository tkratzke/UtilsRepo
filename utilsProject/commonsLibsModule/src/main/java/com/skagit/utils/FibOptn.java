package com.skagit.utils;

public class FibOptn {
	final private static long[] _FibNumbersL;
	static {
		/**
		 * Build Finonacci numbers until they overflow the longs. First find that
		 * overflow point and then compute the Fibonacci numbers.
		 */
		int nL = 2;
		for (long fib1 = 1, fib2 = 1; fib2 <= Long.MAX_VALUE - fib1; ++nL) {
			final long fib = fib1 + fib2;
			fib1 = fib2;
			fib2 = fib;
		}
		_FibNumbersL = new long[nL];
		_FibNumbersL[0] = _FibNumbersL[1] = 1;
		for (int k = 2; k < nL; ++k) {
			_FibNumbersL[k] = _FibNumbersL[k - 1] + _FibNumbersL[k - 2];
		}
	}
	final private Function _f;
	final private double _a, _b;
	final private double _increment;
	final private long[] _fibNumbers;
	final private double[] _maximumPair;

	public FibOptn(final Function f, final double a, final double b) {
		this(f, a, b, Double.NaN);
	}

	public FibOptn(final Function f, final double a, final double b, final double increment) {
		_f = f;
		_a = Math.min(a, b);
		_b = Math.max(a, b);
		if (Double.isNaN(increment)) {
			_fibNumbers = _FibNumbersL;
		} else {
			/**
			 * To get the increment down small enough, we need a set of Fibonacci numbers
			 * that's big enough so that 1 / (biggestFibNumber) < increment.
			 */
			int n = 2;
			final double invIncrement = 1.0 / increment;
			for (long fib1 = 1, fib2 = 1; fib2 < invIncrement && fib2 <= Long.MAX_VALUE - fib1; ++n) {
				final long fib = fib1 + fib2;
				fib1 = fib2;
				fib2 = fib;
			}
			_fibNumbers = new long[n];
			_fibNumbers[0] = _fibNumbers[1] = 1;
			for (int k = 2; k < n; ++k) {
				_fibNumbers[k] = _fibNumbers[k - 1] + _fibNumbers[k - 2];
			}
		}
		final int nFibs = _fibNumbers.length;
		_increment = (_b - _a) / _fibNumbers[nFibs - 1];
		/** Initialization for the induction. */
		final double[][] pairs = new double[4][];
		pairs[0] = new double[] { _a, _f.evaluate(_a) };
		pairs[3] = new double[] { _b, _f.evaluate(_b) };
		final double move = _fibNumbers[nFibs - 2] * _increment;
		final double x1 = _b - move;
		pairs[1] = new double[] { x1, _f.evaluate(x1) };
		final double x2 = _a + move;
		pairs[2] = new double[] { x2, _f.evaluate(x2) };
		_maximumPair = pairs[0].clone();
		for (int k = 1; k < 4; ++k) {
			if (pairs[k][1] > _maximumPair[1]) {
				_maximumPair[0] = pairs[k][0];
				_maximumPair[1] = pairs[k][1];
			}
		}
		/** Do the induction. */
		final int index = nFibs - 1;
		recurse(pairs, index);
	}

	public double[] getMaximumPair() {
		return _maximumPair.clone();
	}

	private void recurse(final double[][] pairs, final int index) {
		if (index < 4)
			return;
		final double move = _fibNumbers[index - 2] * _increment;
		if (pairs[1][1] > pairs[2][1]) {
			final double x = pairs[2][0] - move;
			pairs[3] = pairs[2];
			pairs[2] = pairs[1];
			final double fValue = _f.evaluate(x);
			pairs[1] = new double[] { x, fValue };
			if (fValue > _maximumPair[1]) {
				_maximumPair[0] = x;
				_maximumPair[1] = fValue;
			}
		} else {
			final double x = pairs[1][0] + move;
			pairs[0] = pairs[1];
			pairs[1] = pairs[2];
			final double fValue = _f.evaluate(x);
			pairs[2] = new double[] { x, fValue };
			if (fValue > _maximumPair[1]) {
				_maximumPair[0] = x;
				_maximumPair[1] = fValue;
			}
		}
		recurse(pairs, index - 1);
	}

	public static abstract class Function {
		public abstract double evaluate(double x);
	}

	public static void main(final String[] args) {
		/** Just for fun, check out the integer version. */
		int[] fibNumbersInt;
		int n = 2;
		for (int fib1 = 1, fib2 = 1; fib2 <= Integer.MAX_VALUE - fib1; ++n) {
			final int fib = fib1 + fib2;
			fib1 = fib2;
			fib2 = fib;
		}
		fibNumbersInt = new int[n];
		fibNumbersInt[0] = fibNumbersInt[1] = 1;
		for (int k = 2; k < n; ++k) {
			fibNumbersInt[k] = fibNumbersInt[k - 1] + fibNumbersInt[k - 2];
		}
		System.out.printf("\nn[%d], %s\n", fibNumbersInt.length, CombinatoricTools.getString(fibNumbersInt));
		/** Print out the long version; the one we use. */
		System.out.printf("\nnL[%d], %s\n", _FibNumbersL.length, CombinatoricTools.getString(_FibNumbersL));
		/** Define a function to maximize. */
		final Function f1 = new Function() {
			@Override
			public double evaluate(final double x) {
				return -x;
			}
		};
		/** Define a function to maximize. */
		final FibOptn fibOptn1 = new FibOptn(f1, 0, 1);
		System.out.printf("\n%s\n", NumericalRoutines.getString(fibOptn1.getMaximumPair()));
	}
}
