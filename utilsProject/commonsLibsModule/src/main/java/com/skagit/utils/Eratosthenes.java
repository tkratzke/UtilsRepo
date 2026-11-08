package com.skagit.utils;

/** <pre>
 * For large primes, see https://primes.utm.edu/largest.html
 * For prime calculator, see https://www.numberempire.com/primenumbers.php
 * </pre>
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

public class Eratosthenes {

	final private static String _PrimesFileName = "Primes.bitSetPlus";
	final private static boolean _TryToReadPrimesIn = true;

	final private static BitSetPlus _Primes;

	/**
	 * This is an interesting idea, but it actually slows things down. So we dumb it
	 * down in the first line.
	 */
	final private static int _PntKToBitEstimate(final int bitLow0, final int kLow0, final int bitHigh, final int kHigh,
			final int k) {
		if (k < Integer.MAX_VALUE) {
			return new BitSetPlus(1).kToBitEstimate(bitLow0, kLow0, bitHigh, kHigh, k);
		}
		/** Nuisances that spoil the log and denominators in ensuing calculations: */
		if (k <= 2) {
			return k == 1 ? 2 : 3;
		}
		/**
		 * a and b are measures of how "hot" the true bits are running compared to our
		 * estimates, at kLow and kHigh. We have a problem if kLow or kHigh is 1 so we
		 * work around it. We do know that here, we are working with primes.
		 */
		final int kLow = kLow0 == 1 ? 2 : kLow0;
		final int bitLow = kLow0 == 1 ? 3 : bitLow0;
		final double a = bitLow / (kLow * Math.log(kLow));
		final double b = bitHigh / (kHigh * Math.log(kHigh));
		/*
		 * p is an estimate of how hot the true bit is running compared to our estimate,
		 * at k.
		 */
		final double p = a + (b - a) / (kHigh - kLow) * (k - kLow);
		final long bitEstimate = (int) Math.round(p * k * Math.log(k));
		if (bitLow < bitEstimate && bitEstimate < bitHigh) {
			return (int) bitEstimate;
		}
		return new BitSetPlus(1).kToBitEstimate(bitLow0, kLow0, bitHigh, kHigh, k);
	}

	static {
		BitSetPlus primes = null;
		final File f = new File(_PrimesFileName);
		if (_TryToReadPrimesIn) {
			try (final FileInputStream fis = new FileInputStream(f)) {
				try (final ObjectInputStream ois = new ObjectInputStream(fis)) {
					try {
						final Object o = ois.readObject();
						primes = (BitSetPlus) o;
					} catch (final ClassNotFoundException e) {
						/** Let it crash. */
					}
				} catch (final IOException e) {
					/** Let it crash. */
				}
			} catch (final IOException e1) {
				/** Let it crash. */
			}
		}
		if (primes == null) {
			final long currentTimeMillis0 = System.currentTimeMillis();
			primes = new BitSetPlus(Integer.MIN_VALUE) {
				private static final long serialVersionUID = 1L;

				@Override
				protected int kToBitEstimate(final int bitLow, final int kLow, final int bitHigh, final int kHigh,
						final int k) {
					return _PntKToBitEstimate(bitLow, kLow, bitHigh, kHigh, k);
				}
			};
			primes.set(2, Integer.MAX_VALUE);
			primes.set(Integer.MAX_VALUE);
			for (int bit = 4;;) {
				primes.clear(bit);
				if (bit > Integer.MAX_VALUE - 2) {
					break;
				}
				bit += 2;
			}
			final int maxValueOver3 = Integer.MAX_VALUE / 3;
			for (int p = 3; 0 <= p && p <= maxValueOver3; p = primes.nextSetBit(p + 1)) {
				final int twoP = 2 * p;
				final int topValue = Integer.MAX_VALUE - twoP;
				for (int bit = 3 * p;;) {
					primes.clear(bit);
					if (bit > topValue) {
						break;
					}
					bit += twoP;
				}
			}
			final long currentTimeMillis1 = System.currentTimeMillis();
			final String durationString = TimeUtilities.durationInSecondsToMinsSecsString(
					(int) ((currentTimeMillis1 - currentTimeMillis0) / 1000L), /* nDigitsForMinutes= */2);
			System.out.printf("Took %s to compute primes.\n", durationString);
			try (final FileOutputStream fos = new FileOutputStream(f)) {
				try (final ObjectOutputStream oos = new ObjectOutputStream(fos)) {
					oos.writeObject(primes);
				} catch (final IOException e) {
					/** Let it crash. */
				}
			} catch (final IOException e1) {
				/** Let it crash. */
			}
		}
		_Primes = primes;
	}

	public static boolean isPrime(final long n) {
		if (n <= Integer.MAX_VALUE) {
			return _Primes.get((int) n);
		}
		final int sqrt = (int) Math.round(Math.floor(Math.sqrt(n)));
		int p = 2;
		for (; 0 < p && p <= sqrt; p = _Primes.nextSetBit(p + 1)) {
			if (n % p == 0) {
				return false;
			}
			if (p == Integer.MAX_VALUE) {
				break;
			}
		}
		if (p >= sqrt) {
			return true;
		}
		final long startAt = Integer.MAX_VALUE + 2L;
		for (long m = startAt; m <= sqrt; m += 2) {
			if (n % m == 0L) {
				return false;
			}
		}
		return true;
	}

	public static long getPrimeDivisor(final long n) {
		final int sqrt = (int) Math.round(Math.floor(Math.sqrt(n)));
		int p = 2;
		for (; 0 < p && p <= sqrt; p = _Primes.nextSetBit(p + 1)) {
			if (n % p == 0) {
				return p;
			}
			if (p == Integer.MAX_VALUE) {
				break;
			}
		}
		if (p >= sqrt) {
			return n;
		}
		for (int m = Integer.MAX_VALUE + 2; m <= sqrt; m += 2) {
			if (n % m == 0) {
				return getPrimeDivisor(m);
			}
		}
		return n;
	}

	public static long[][] getPrimeDivisorsWithCounts(long n) {
		final ArrayList<long[]> primeDivisorsWithCounts = new ArrayList<>();
		while (n > 1L) {
			final long primeDivisor = getPrimeDivisor(n);
			int count = 1;
			while (true) {
				n /= primeDivisor;
				if (n % primeDivisor != 0) {
					break;
				}
				++count;
			}
			primeDivisorsWithCounts.add(new long[] { primeDivisor, count });
		}
		return primeDivisorsWithCounts.toArray(new long[primeDivisorsWithCounts.size()][]);
	}

	public static long[] getAllDivisors(final long n) {
		final long[][] primeDivisorsWithCounts = getPrimeDivisorsWithCounts(n);
		if (primeDivisorsWithCounts == null) {
			return null;
		}
		final int nPrimeDivisors = primeDivisorsWithCounts.length;
		int nDivisors = 1;
		final long[][] primePowers = new long[nPrimeDivisors][];
		for (int k0 = 0; k0 < nPrimeDivisors; ++k0) {
			final long prime = primeDivisorsWithCounts[k0][0];
			final int nPowers = (int) (primeDivisorsWithCounts[k0][1] + 1);
			primePowers[k0] = new long[nPowers];
			primePowers[k0][0] = 1;
			for (int k1 = 1; k1 < nPowers; ++k1) {
				primePowers[k0][k1] = primePowers[k0][k1 - 1] * prime;
			}
			nDivisors *= nPowers;
		}
		final long[] divisors = new long[nDivisors];
		for (int k1 = 0; k1 < nDivisors; ++k1) {
			int divisor = 1;
			int k11 = k1;
			for (int k0 = 0; k0 < nPrimeDivisors; ++k0) {
				final int nPowers = primePowers[k0].length;
				final int power = k11 % nPowers;
				k11 /= nPowers;
				divisor *= primePowers[k0][power];
			}
			divisors[k1] = divisor;
		}
		Arrays.sort(divisors);
		return divisors;
	}

	public static int getKthPrime(final int k) {
		return _Primes.kToBit(k);
	}

	public static int nextPrimeAfter(final int k) {
		return _Primes.nextSetBit(k + 1);
	}

	public static int getNPrimes() {
		return _Primes.cardinality();
	}

	public static String getString(final long[] longs) {
		final String[] stringArray = Arrays.stream(longs).mapToObj(String::valueOf).toArray(String[]::new);
		return String.join(", ", stringArray);
	}

	public static void testGetKthPrime(final String[] args) {
		boolean printedALine = false;
		final int nPrimes = _Primes.cardinality();
		System.out.printf("PrimesCardinality = %d.", nPrimes);
		final int nToTry = 25;
		printedALine = true;
		if (true) {
			int oldPrime = -1;
			for (int k = 990, count = 0; count < nToTry; ++count, ++k) {
				final int kthPrime = getKthPrime(k);
				System.out.printf("%s%dth prime is %d, estimate[%d]", printedALine ? "\n" : "", k, kthPrime,
						_Primes.kToBitEstimate(2, 1, Integer.MAX_VALUE, _Primes.cardinality(), k));
				if (oldPrime > 0) {
					final int diffInPrimes = kthPrime - oldPrime;
					System.out.printf("\tDiff with above = %d", diffInPrimes);
				}
				oldPrime = kthPrime;
				printedALine = true;
			}
		}
		if (true) {
			int oldPrime = -1;
			for (int k = nPrimes, count = 0; count < nToTry; ++count, --k) {
				final int kthPrime = getKthPrime(k);
				System.out.printf("%s%dth prime is %d", printedALine ? "\n" : "", k, kthPrime);
				if (oldPrime > 0) {
					final int diffInPrimes = oldPrime - kthPrime;
					System.out.printf("\tDiff with above = %d", diffInPrimes);
				}
				oldPrime = kthPrime;
				printedALine = true;
			}
		}
		if (true) {
			int oldPrime = -1;
			final int startK = (nPrimes + nToTry) / 2;
			for (int k = startK, count = 0; count < nToTry; ++count, --k) {
				final int kthPrime = getKthPrime(k);
				System.out.printf("%s%dth prime is %d", printedALine ? "\n" : "", k, kthPrime);
				if (oldPrime > 0) {
					final int diffInPrimes = oldPrime - kthPrime;
					System.out.printf("\tDiff with above = %d", diffInPrimes);
				}
				oldPrime = kthPrime;
				printedALine = true;
			}
		}
	}

	public static void main(final String[] args) {
		final long[][] divisors = getPrimeDivisorsWithCounts(72L);
		System.out.print(CombinatoricTools.getString(divisors));
	}
}
