package com.skagit.utils;

public class MyFraction {
	public static long _InvalidFlag = Long.MIN_VALUE;

	public static boolean IsValid(final MyFraction frctn) {
		return frctn != null && frctn.isValid();
	}

	/**
	 * _den will always be positive. If _intPart != 0, the sign of _intPart and _num
	 * will be the same. The number represented is (_den*_intPart + _num)/_den.
	 */
	public long _num, _den;

	public MyFraction(String numberString) {
		numberString = numberString.replaceAll("\\s+/", "/");
		numberString = numberString.replaceAll("/\\s+", "/");
		final String[] fields = numberString.split("\\s");
		final int nFields = fields.length;
		long num = 0L, den = 1L;
		if (nFields == 0 || nFields > 2) {
			_num = _den = _InvalidFlag;
			return;
		}
		final String s0 = fields[0];
		final int slashIdx0 = s0.indexOf('/');
		if (nFields == 1) {
			if (slashIdx0 == 0 || slashIdx0 == s0.length() - 1) {
				_num = _den = _InvalidFlag;
				return;
			}
			if (slashIdx0 < 0) {
				/** One field, no slash; pure integer. */
				try {
					num = Long.parseLong(s0);
				} catch (final NumberFormatException e) {
					num = den = _InvalidFlag;
				}
				_num = num;
				_den = 1L;
				return;
			}
			/** One field, there is a slash; pure fraction. */
			try {
				num = Long.parseLong(s0.substring(0, slashIdx0));
				den = Long.parseLong(s0.substring(slashIdx0 + 1));
			} catch (final NumberFormatException e) {
				_num = _den = _InvalidFlag;
				return;
			}
			if (den == 0L) {
				_num = _den = _InvalidFlag;
				return;
			}
			if (den < 0) {
				num = -num;
				den = -den;
			}
			final long[] normalized = normalize(num, den);
			_num = normalized[0];
			_den = normalized[1];
			return;
		}
		/** Two fields. */
		final String s1 = fields[1];
		final int dashIdx1 = s1.indexOf('-');
		final int slashIdx1 = s1.indexOf('/');
		/**
		 * If the first field has a slash or the second field has a dash or the second
		 * field does not have a slash or that slash is not internal, we have an error.
		 */
		if (slashIdx0 >= 0 || dashIdx1 >= 0 || slashIdx1 <= 0 || slashIdx1 == s1.length() - 1) {
			_num = _den = _InvalidFlag;
			return;
		}
		long intPart = 0L;
		try {
			intPart = Long.parseLong(s0);
			num = Long.parseLong(s1.substring(0, slashIdx1));
			den = Long.parseLong(s1.substring(slashIdx1 + 1));
			if (intPart < 0) {
				num = intPart * den - num;
			} else {
				num += intPart * den;
			}
		} catch (final NumberFormatException e) {
			_num = _den = _InvalidFlag;
			return;
		}
		final long[] normalized = normalize(num, den);
		_num = normalized[0];
		_den = normalized[1];
	}

	public MyFraction(final double d0, final double threshold) {
		final double d = Math.abs(d0);
		long num0 = 1L;
		long den0 = 0L;
		long num1 = (long) Math.floor(d);
		long den1 = 1;
		double r = d - num1;

		for (;;) {
			if (r < threshold) {
				break;
			}
			r = 1d / r;
			final long floor = (long) Math.floor(r);
			if (floor > (Long.MAX_VALUE - num0) / num1) {
				break;
			}
			if (floor > (Long.MAX_VALUE - den0) / den1) {
				break;
			}
			final long newNum = floor * num1 + num0;
			final long newDen = floor * den1 + den0;
			num0 = num1;
			den0 = den1;
			num1 = newNum;
			den1 = newDen;
			r -= floor;
		}
		_num = d0 < 0 ? -num1 : num1;
		_den = den1;
	}

	public MyFraction(final long intPart) {
		_num = intPart;
		_den = 1L;
	}

	public final boolean isValid() {
		return _num != _InvalidFlag;
	}

	public MyFraction(final long num0, final long den0) {
		if (den0 == 0L) {
			_num = _den = _InvalidFlag;
			return;
		}
		final long num = den0 < 0 ? -num0 : num0;
		final long den = den0 < 0 ? -den0 : den0;
		final long[] normalized = normalize(num, den);
		_num = normalized[0];
		_den = normalized[1];
	}

	@Override
	public MyFraction clone() {
		return new MyFraction(_num, _den);
	}

	static long[] normalize(final long num, final long den) {
		if (num == 0) {
			return new long[] { 0, 1 };
		}
		if (num == _InvalidFlag || den == _InvalidFlag) {
			return new long[] { _InvalidFlag, _InvalidFlag };
		}
		final long gcd = CombinatoricTools.getGcd(num, den);
		return new long[] { num / gcd, den / gcd };
	}

	/**
	 * Operators operate on themselves if asked to, but return themselves for
	 * convenience in calling.
	 */
	public MyFraction add(final MyFraction frctn, final boolean operateOnSelf) {
		return addOrSubtract(/* add= */true, frctn, operateOnSelf);
	}

	public MyFraction subtract(final MyFraction frctn, final boolean operateOnSelf) {
		return addOrSubtract(/* add= */false, frctn, operateOnSelf);
	}

	private MyFraction addOrSubtract(final boolean add, final MyFraction frctn1, final boolean operateOnSelf) {
		final long num0 = _num;
		final long den0 = _den;
		final long num1 = frctn1._num;
		final long den1 = frctn1._den;

		final long num, den;
		if (den0 == 0 || den1 == 0) {
			num = den = Long.MAX_VALUE;
		} else {
			den = CombinatoricTools.getLcm(den0, den1);
			if (add) {
				num = num0 * (den / den0) + num1 * (den / den1);
			} else {
				num = num0 * (den / den0) - num1 * (den / den1);
			}
		}
		if (operateOnSelf) {
			final long[] normalized = normalize(num, den);
			_num = normalized[0];
			_den = normalized[1];
			return this;
		}
		return new MyFraction(num, den);
	}

	public MyFraction multiply(final MyFraction frctn, final boolean operateOnSelf) {
		return multiplyOrDivide(/* multiply= */true, frctn, operateOnSelf);
	}

	public MyFraction divide(final MyFraction frctn, final boolean operateOnSelf) {
		return multiplyOrDivide(/* multiply= */false, frctn, operateOnSelf);
	}

	private MyFraction multiplyOrDivide(final boolean multiply, final MyFraction frctn1, final boolean operateOnSelf) {
		long num0 = _num;
		long den0 = _den;
		long num1 = multiply ? frctn1._num : frctn1._den;
		long den1 = multiply ? frctn1._den : frctn1._num;
		final long num, den;
		if (den0 == _InvalidFlag || den1 == _InvalidFlag) {
			num = den = _InvalidFlag;
		} else {
			final long num0Den1Gcd = CombinatoricTools.getGcd(num0, den1);
			num0 /= num0Den1Gcd;
			den1 /= num0Den1Gcd;
			final long den0Num1Gcd = CombinatoricTools.getGcd(den0, num1);
			den0 /= den0Num1Gcd;
			num1 /= den0Num1Gcd;
			num = num0 * num1;
			den = den0 * den1;
		}
		if (operateOnSelf) {
			final long[] normalized = normalize(num, den);
			_num = normalized[0];
			_den = normalized[1];
			return this;
		}
		return new MyFraction(num, den);
	}

	public MyFraction abs() {
		return new MyFraction(Math.abs(_num), _den);
	}

	public double toDouble() {
		return ((double) (_num)) / _den;
	}

	public String getString(final boolean printFloat) {
		if (!IsValid(this)) {
			return "INVALID FRACTION";
		}
		final String s1 = String.format("%d/%d", _num, _den);
		final long intPart = _num / _den;
		final long num = _num % _den;
		final String s2;
		if (num == 0) {
			s2 = String.format("%d", intPart);
		} else if (Math.abs(_num) < _den) {
			s2 = s1;
		} else {
			s2 = String.format("%d %d/%d", intPart, num, _den);
		}
		String s = s1.length() < s2.length() ? s1 : s2;
		if (printFloat && _den > 1) {
			s += String.format(" (%f)", toDouble());
		}
		return s;
	}

	public String getNumDenString() {
		if (!IsValid(this)) {
			return "INVALID FRACTION";
		}
		return String.format("%d/%d", _num, _den);
	}

	@Override
	public String toString() {
		return getString(/* printFloat= */true);
	}

	public static void main(final String[] args) {
		final double d = 43d / 17d;
		final MyFraction myFraction = new MyFraction(d, /* threshold= */1.0e-5);
		System.out.println(String.format("%f: %s", d, myFraction.getString(/* printFloat= */true)));
	}

}