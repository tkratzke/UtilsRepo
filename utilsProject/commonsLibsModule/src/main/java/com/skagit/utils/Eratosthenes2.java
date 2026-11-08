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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

public class Eratosthenes2 implements Serializable {
	private static final long serialVersionUID = 1L;

	// final private static long _LastEllToSet = (1 << 20) - 1L;
	// final private static int _NBitsPerBitSetPlus = 1 << 16;
	final private static long _LastEllToSet = Math.round(Math.floor(Math.sqrt(Long.MAX_VALUE)));
	final private static int _NBitsPerBitSetPlus = Integer.MIN_VALUE;

	final private static String _BigBitSetFileName = "Primes.bitSetPlus2";

	final private static BitSetPlus2 _Primes2;
	final private static long _NPrimes2;

	/** We build _Primes2 if we cannot read it in. */
	static {
		BitSetPlus2 primes2 = null;
		final File f = new File(_BigBitSetFileName);
		try (final FileInputStream fis = new FileInputStream(f)) {
			try (final ObjectInputStream ois = new ObjectInputStream(fis)) {
				try {
					final Object o = ois.readObject();
					primes2 = (BitSetPlus2) o;
				} catch (final ClassNotFoundException e) {
					/** Let it crash. */
				}
			} catch (final IOException e) {
				/** Let it crash. */
			}
		} catch (final IOException e1) {
			/** Let it crash. */
		}
		if (primes2 == null) {
			final long currentTimeMillis0 = System.currentTimeMillis();
			final long primes2Size = _LastEllToSet + 1L;
			primes2 = new BitSetPlus2(primes2Size, /* nBitsPerBitSetPlus = */_NBitsPerBitSetPlus);
			primes2.set(2L, primes2Size);
			for (long p = 4L; p <= _LastEllToSet; p += 2L) {
				primes2.clear(p);
				if (p > _LastEllToSet - 2) {
					break;
				}
			}
			final long lastBitToSetOver3 = _LastEllToSet / 3L;
			for (long p = 3L; 0L <= p && p <= lastBitToSetOver3; p = primes2.nextSetBit(p + 1L)) {
				final long twoP = 2L * p;
				final long topValue = _LastEllToSet - twoP;
				for (long k = 3L * p;; k += twoP) {
					primes2.clear(k);
					if (k > topValue) {
						break;
					}
				}
			}
			final long currentTimeMillis1 = System.currentTimeMillis();
			final String durationString = TimeUtilities.durationInSecondsToMinsSecsString(
					(int) ((currentTimeMillis1 - currentTimeMillis0) / 1000L), /* nDigitsForMinutes= */2);
			System.out.printf("Took %s to compute primes.\n", durationString);
			try (final FileOutputStream fos = new FileOutputStream(f)) {
				try (final ObjectOutputStream oos = new ObjectOutputStream(fos)) {
					oos.writeObject(primes2);
				} catch (final IOException e) {
					/** Let it crash. */
				}
			} catch (final IOException e1) {
				/** Let it crash. */
			}
		}
		_Primes2 = primes2;
		_NPrimes2 = _Primes2.cardinality();
	}

	public static boolean isPrime(final long n) {
		if (n < _Primes2.size()) {
			return _Primes2.get(n);
		}
		final long sqrt = Math.round(Math.floor(Math.sqrt(n)));
		long p = 2L;
		for (; 0L < p && p <= sqrt; p = _Primes2.nextSetBit(p + 1L)) {
			if (n % p == 0L) {
				return false;
			}
		}
		return true;
	}

	public static long getPrimeDivisor(final long n) {
		final long sqrt = Math.round(Math.floor(Math.sqrt(n)));
		long p = 2L;
		for (; 0L < p && p <= sqrt; p = (_Primes2.nextSetBit(p + 1L))) {
			if (n % p == 0L) {
				return p;
			}
		}
		return n;
	}

	public static long getKthPrime(final long k) {
		return _Primes2.kToEll(k);
	}

	public static long getNStoredPrimes() {
		return _NPrimes2;
	}

	public static long[][] getPrimeDivisorsWithCounts(long n) {
		final ArrayList<long[]> primeDivisorsWithCounts = new ArrayList<>();
		while (n > 1) {
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

	// public static void testGetKthPrime(final String[] args) {
	public static void main(final String[] args) {
		boolean printedALine = false;
		final long nPrimes = _Primes2.cardinality();
		System.out.printf("PrimesCardinality = %d.", nPrimes);
		final int nToTry = 25;
		printedALine = true;
		if (true) {
			long oldPrime = -1;
			for (int k = 1, count = 0; count < nToTry; ++count, ++k) {
				final long kthPrime = getKthPrime(k);
				System.out.printf("%s%dth prime is %d", printedALine ? "\n" : "", k, kthPrime);
				if (oldPrime > 0) {
					final long diffInPrimes = kthPrime - oldPrime;
					System.out.printf("\tDiff with above = %d", diffInPrimes);
				}
				oldPrime = kthPrime;
				printedALine = true;
			}
		}
		if (true) {
			long oldPrime = -1;
			for (long k = nPrimes, count = 0; count < nToTry; ++count, --k) {
				final long kthPrime = getKthPrime(k);
				System.out.printf("%s%dth prime is %d", printedALine ? "\n" : "", k, kthPrime);
				if (oldPrime > 0) {
					final long diffInPrimes = oldPrime - kthPrime;
					System.out.printf("\tDiff with above = %d", diffInPrimes);
				}
				oldPrime = kthPrime;
				printedALine = true;
			}
		}
		if (true) {
			long oldPrime = -1;
			final long startK = (nPrimes + nToTry) / 2;
			for (long k = startK, count = 0; count < nToTry; ++count, --k) {
				final long kthPrime = getKthPrime(k);
				System.out.printf("%s%dth prime is %d", printedALine ? "\n" : "", k, kthPrime);
				if (oldPrime > 0) {
					final long diffInPrimes = oldPrime - kthPrime;
					System.out.printf("\tDiff with above = %d", diffInPrimes);
				}
				oldPrime = kthPrime;
				printedALine = true;
			}
		}
	}

}
