package com.skagit.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

public class CombinatoricTools {

	final public static Comparator<boolean[]> _booleanVectorComparator = (bools1, bools2) -> {
		final int n = bools1.length;
		for (int i = 0; i < n; ++i) {
			if (bools1[i] != bools2[i]) {
				return bools1[i] ? 1 : -1;
			}
		}
		return 0;
	};
	final public static Comparator<int[]> _ByFirst = (ints1, ints2) -> ints1[0] - ints2[0];
	final public static Comparator<int[]> _ByAllInOrder = (ints1, ints2) -> {
		final int n = ints1.length;
		for (int i = 0; i < n; ++i) {
			if (ints1[i] < ints2[i]) {
				return -1;
			}
			if (ints1[i] > ints2[i]) {
				return 1;
			}
		}
		return 0;
	};
	final public static Comparator<long[]> _ByAllInOrderL = (longs1, longs2) -> {
		final int n = longs1.length;
		for (int i = 0; i < n; ++i) {
			if (longs1[i] < longs2[i]) {
				return -1;
			}
			if (longs1[i] > longs2[i]) {
				return 1;
			}
		}
		return 0;
	};
	final public static Comparator<boolean[]> _ByAllInOrderB = (booleans1, booleans2) -> {
		final int n1 = booleans1.length;
		final int n2 = booleans2.length;
		if (n1 != n2) {
			return n1 < n2 ? -1 : 1;
		}
		for (int i = 0; i < n1; ++i) {
			if (booleans1[i] != booleans2[i]) {
				return !booleans1[i] ? -1 : 1;
			}
		}
		return 0;
	};
	final public static Comparator<int[]> _ByDictionary = (ints1, ints2) -> {
		final int n1 = ints1 == null ? 0 : ints1.length;
		final int n2 = ints2 == null ? 0 : ints2.length;
		final int n = Math.max(n1, n2);
		for (int i = 0; i < n; ++i) {
			if (i == n1 && i < n2) {
				return -1;
			}
			if (i < n1 && i == n2) {
				return 1;
			}
			final int compareValue = ints1[i] - ints2[i];
			if (compareValue != 0) {
				return compareValue;
			}
		}
		return 0;
	};
	@SuppressWarnings("rawtypes")
	final public static Comparator<Comparable[]> _ByAllInOrderForComparatorArrays = new Comparator<>() {
		@Override
		@SuppressWarnings("unchecked")
		public int compare(final Comparable[] comparatorArray1, final Comparable[] comparatorArray2) {
			int compareValue = 0;
			final int n = comparatorArray1.length;
			for (int i = 0; compareValue == 0 && i < n; ++i) {
				compareValue = comparatorArray1[i].compareTo(comparatorArray2[i]);
			}
			return compareValue;
		}
	};
	final public static Comparator<int[]> _ByFirstAndSecond = (ints1, ints2) -> {
		int compareValue = ints1[0] - ints2[0];
		if (compareValue == 0) {
			compareValue = ints1[1] - ints2[1];
		}
		return compareValue;
	};

	final public static Comparator<short[]> _ShortComparator = (shorts1, shorts2) -> {
		if ((shorts1 == null) != (shorts2 == null)) {
			return shorts1 == null ? -1 : 1;
		}
		if (shorts1 == null) {
			return 0;
		}
		final int n1 = shorts1.length, n2 = shorts2.length, n = Math.max(n1, n2);
		for (int k = 0; k < n; ++k) {
			if (k >= n1) {
				return -1;
			}
			if (k >= n2) {
				return 1;
			}
			if (shorts1[k] < shorts2[k]) {
				return -1;
			}
			if (shorts1[k] > shorts2[k]) {
				return 1;
			}
		}
		return 0;
	};

	final public static Comparator<int[][]> _MatrixComparator = (p1, p2) -> {
		int oompareValue = 0;
		final int n = p1.length;
		for (int i = 0; oompareValue == 0 && i < n; ++i) {
			oompareValue = _ByAllInOrder.compare(p1[i], p2[i]);
		}
		return oompareValue;
	};

	public static int twoToTheNth(final int n) {
		return 1 << n;
	}

	public static Comparator<BitSet> _BitSetComparator = (bitSet0, bitSet1) -> {
		int nextSet0 = bitSet0.nextSetBit(0);
		int nextSet1 = bitSet1.nextSetBit(0);
		while (nextSet0 == nextSet1 && nextSet0 >= 0) {
			nextSet0 = bitSet0.nextSetBit(nextSet0 + 1);
			nextSet1 = bitSet1.nextSetBit(nextSet1 + 1);
		}
		if (nextSet0 < nextSet1) {
			return nextSet0 == -1 ? -1 : 1;
		}
		if (nextSet1 < nextSet0) {
			return nextSet1 == -1 ? 1 : -1;
		}
		return 0;
	};

	/**
	 * This method changes a subset "extension" of {0..m-1} that contains fixed,
	 * into the next subset that also contains fixed.
	 */
	public static boolean nextExtension(final BitSet fixed, final BitSet extension, final int n) {
		for (int i = fixed.nextClearBit(0);; i = fixed.nextClearBit(i + 1)) {
			if (i == n) {
				return false;
			}
			extension.flip(i);
			if (extension.get(i)) {
				return true;
			}
		}
	}

	/** Spreads out k fenceposts among 0, ..., n-1. */
	public static int[] getSimpleFenceposts(final int n, int k) {
		k = Math.min(n, k);
		final int[] returnValue;
		if (k <= 0) {
			returnValue = null;
		} else {
			returnValue = new int[k];
			if (k == 1) {
				returnValue[0] = (n - 1) / 2;
			} else {
				final int n1 = n - 1;
				final int k1 = k - 1;
				for (int i = 0; i < k; ++i) {
					final int in1 = i * n1;
					returnValue[i] = in1 / k1;
					final int remainder = in1 - k1 * returnValue[i];
					if (2 * remainder > k1) {
						++returnValue[i];
					}
				}
			}
		}
		return returnValue;
	}

	public static long[] getFenceposts(long firstFencepost, long lastFencepost, int nFenceposts,
			final boolean distinct) {
		if (nFenceposts == 0) {
			return null;
		}
		boolean flipped = false;
		if (nFenceposts < 0) {
			final long fencepost = firstFencepost;
			firstFencepost = lastFencepost;
			lastFencepost = fencepost;
			nFenceposts = -nFenceposts;
			flipped = !flipped;
		}
		if (firstFencepost > lastFencepost) {
			final long fencepost = firstFencepost;
			firstFencepost = lastFencepost;
			lastFencepost = fencepost;
			flipped = !flipped;
		}
		final long[] fenceposts;
		final int nEntries = (int) (lastFencepost - firstFencepost + 1);
		if (distinct && nFenceposts > nEntries) {
			/**
			 * Too many fenceposts for the range. Every member of the range becomes its own
			 * fencepost.
			 */
			fenceposts = new long[nEntries];
			for (int k = 0; k < nEntries; ++k) {
				fenceposts[k] = firstFencepost + k;
			}
		} else {
			fenceposts = new long[nFenceposts];
			if (nFenceposts == 1) {
				fenceposts[0] = (firstFencepost + lastFencepost) / 2;
			} else {
				final long range = lastFencepost - firstFencepost;
				final int divisor = nFenceposts - 1;
				final int rounder = divisor / 2;
				for (int i = 0; i < nFenceposts; ++i) {
					fenceposts[i] = firstFencepost + (i * range + rounder) / divisor;
				}
			}
		}
		if (flipped) {
			for (int i = 0; i < nFenceposts / 2; ++i) {
				final long fencepostI = fenceposts[i];
				fenceposts[i] = fenceposts[nFenceposts - 1 - i];
				fenceposts[nFenceposts - 1 - i] = fencepostI;
			}
		}
		return fenceposts;
	}

	public static long[] getFenceposts(final long firstFencepost, final long lastFencepost, final int nFenceposts) {
		return getFenceposts(firstFencepost, lastFencepost, nFenceposts, /* distinct= */false);
	}

	public static int getGlbIndex(final long[] array, final long l) {
		final int n = array.length;
		final int j = Arrays.binarySearch(array, l);
		if (j < 0) {
			final int insertionPoint = -(j + 1);
			return insertionPoint - 1;
		}
		/** We have a match. Find the largest one that is still a match. */
		int glbIndex = j;
		for (glbIndex = j + 1; glbIndex < n; ++glbIndex) {
			if (array[glbIndex] > l) {
				return glbIndex - 1;
			}
		}
		return n - 1;
	}

	public static int getLubIndex(final long[] array, final long key) {
		final int j = Arrays.binarySearch(array, key);
		if (j < 0) {
			return -(j + 1);
		}
		/** We have a match. Find the smallest one that is still a match. */
		int lubIndex = j;
		for (lubIndex = j - 1; lubIndex >= 0; --lubIndex) {
			if (array[lubIndex] < key) {
				return lubIndex + 1;
			}
		}
		return 0;
	}

	public static int getGlbIndex(final int[] array, final int i) {
		final int j = Arrays.binarySearch(array, i);
		if (j < 0) {
			return -j - 2;
		}
		final int n = array.length;
		int glbIndex = j;
		for (glbIndex = j + 1; glbIndex < n; ++glbIndex) {
			if (array[glbIndex] > i) {
				return glbIndex - 1;
			}
		}
		return n - 1;
	}

	public static int getLubIndex(final int[] array, final int key) {
		final int j = Arrays.binarySearch(array, key);
		if (j < 0) {
			return -(j + 1);
		}
		/** We have a match. Find the smallest one that is still a match. */
		int lubIndex = j;
		for (lubIndex = j - 1; lubIndex >= 0; --lubIndex) {
			if (array[lubIndex] < key) {
				return lubIndex + 1;
			}
		}
		return 0;
	}

	public static int getGlbIndex(final short[] array, final short i) {
		final int j = Arrays.binarySearch(array, i);
		if (j < 0) {
			return -j - 2;
		}
		final int n = array.length;
		int glbIndex = j;
		for (glbIndex = j + 1; glbIndex < n; ++glbIndex) {
			if (array[glbIndex] > i) {
				return glbIndex - 1;
			}
		}
		return n - 1;
	}

	/**
	 * Examples, and comparison between binarySearch and glbIndex:
	 *
	 * <pre>
	 * int[] array = new int[] { 1, 1, 3, 3, 5, 5 };
	 * 0: binarySearch[-1], glbIndex[-1].
	 * 1: binarySearch[1], glbIndex[1].
	 * 2: binarySearch[-3], glbIndex[1].
	 * 3: binarySearch[2], glbIndex[3].
	 * 4: binarySearch[-5], glbIndex[3].
	 * 5: binarySearch[4], glbIndex[5].
	 * 6: binarySearch[-7], glbIndex[5].
	 * </pre>
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Object> int getGlbIndex(final T[] tArray, final T t, final Comparator<T> comparator) {
		final int j = Arrays.binarySearch(tArray, t, comparator);
		if (j < 0) {
			return -j - 2;
		}
		/**
		 * We now know that tArray[j] <= t. Increase j until array[j] > t, and return j
		 * - 1.
		 */
		final int n = tArray.length;
		int idx = j;
		for (idx = j + 1; idx < n; ++idx) {
			int compareValue;
			if (comparator != null) {
				compareValue = comparator.compare(tArray[idx], t);
			} else {
				compareValue = ((Comparable<T>) tArray[idx]).compareTo(t);
			}
			if (compareValue > 0) {
				return idx - 1;
			}
		}
		return n - 1;
	}

	@SuppressWarnings("unchecked")
	public static <T extends Object> T[] consolidate(final T[] array) {
		if (array == null) {
			return array;
		}
		int n = 0;
		for (final T t : array) {
			if (t != null) {
				++n;
			}
		}
		if (n == array.length) {
			return array;
		}
		final T[] array2 = (T[]) (new Object[n]);
		int i = 0;
		for (final T t : array) {
			if (t != null) {
				array2[i++] = t;
			}
		}
		return array2;
	}

	public static void pancake(final int[] a) {
		final int len = a == null ? 0 : a.length;
		final int nFlips = len / 2;
		for (int k = 0; k < nFlips; ++k) {
			final int t = a[k];
			a[k] = a[len - 1 - k];
			a[len - 1 - k] = t;
		}
	}

	public static <T extends Object> void pancake(final T[] a) {
		final int len = a == null ? 0 : a.length;
		final int nFlips = len / 2;
		for (int k = 0; k < nFlips; ++k) {
			final T t = a[k];
			a[k] = a[len - 1 - k];
			a[len - 1 - k] = t;
		}
	}

	public static <T extends Object> void reverse(final List<T> tList) {
		final int n = tList.size();
		final int nPairs = n / 2;
		for (int k = 0; k < nPairs; ++k) {
			final int kEnd = n - 1 - k;
			final T t = tList.get(k);
			tList.set(k, tList.get(kEnd));
			tList.set(kEnd, t);
		}
	}

	public static String getString(@SuppressWarnings("rawtypes") final Comparable[] query) {
		final int length = query == null ? 0 : query.length;
		final StringBuffer b = new StringBuffer();
		b.append('[');
		for (int i = 0; i < length; ++i) {
			b.append(i == 0 ? "" : ",");
			if (query[i] == null) {
				b.append('*');
			} else {
				b.append(query[i]);
			}
		}
		b.append(']');
		return new String(b);
	}

	public static String toString(final String[] names) {
		final int length = names == null ? 0 : names.length;
		final StringBuffer b = new StringBuffer();
		b.append('[');
		for (int i = 0; i < length; ++i) {
			b.append(i == 0 ? "" : ",");
			b.append(names[i]);
		}
		b.append(']');
		return new String(b);
	}

	public static int[] getInitial(final int n) {
		final int[] perm = new int[n];
		for (int i = 0; i < n; ++i) {
			perm[i] = i;
		}
		return perm;
	}

	/**
	 * Spaces them over the entire range. Gaps are not necessarily the same size.
	 */
	public static int[] getSpacedIndices(final int arrayLength, final int nIndices) {
		if (arrayLength <= nIndices) {
			return getInitial(arrayLength);
		}
		final int[] indices = new int[nIndices];
		for (int k = 0; k < nIndices; ++k) {
			indices[k] = (int) Math.round((double) k / nIndices * arrayLength);
		}
		return indices;
	}

	/**
	 * Spaces them as well as possible over the entire range. Gaps are the same
	 * size.
	 */
	public static int[] getSpacedIndices2(final int arrayLength, final int nIndices) {
		if (arrayLength <= nIndices) {
			return getInitial(arrayLength);
		}
		final int gap = (arrayLength - 1) / (nIndices - 1) - 1;
		final int tails = arrayLength - (1 + (gap + 1) * (nIndices - 1));
		final int lowTail = tails / 2;
		final int[] indices = new int[nIndices];
		for (int k = 0; k < nIndices; ++k) {
			indices[k] = lowTail + k * (gap + 1);
		}
		return indices;
	}

	public static void constantFill(final boolean[] booleans, final boolean b) {
		final int n = booleans == null ? 0 : booleans.length;
		for (int i = 0; i < n; ++i) {
			booleans[i] = b;
		}
	}

	public static void constantFill(final char[] chars, final char c) {
		final int n = chars == null ? 0 : chars.length;
		for (int i = 0; i < n; ++i) {
			chars[i] = c;
		}
	}

	public static void constantFill(final short[] shorts, final short value) {
		final int n = shorts == null ? 0 : shorts.length;
		for (int i = 0; i < n; ++i) {
			shorts[i] = value;
		}
	}

	public static void constantFill(final int[] ints, final int value) {
		Arrays.fill(ints, value);
	}

	public static void constantFill(final long[] longs, final long value) {
		Arrays.fill(longs, value);
	}

	/**
	 * magical formula for an expanding "square." 0>>(0,0), 1>>(1,0), 2>>(1,1),
	 * 3>>(0,1), etc.
	 */
	public static int[] intToSpiralCoords(final int m) {
		/** (m+1)/2 <= n(n+1)/2 => we want n-1. */
		final int n = (int) Math.round(Math.ceil((-1 + Math.sqrt(5 + 4.0 * m)) / 2.0) - 1);
		final int iStep = m - n * (n + 1);
		final int start;
		final int j, i;
		if (n % 2 == 0) {
			start = -n / 2;
			if (iStep < n + 1) {
				j = start + iStep;
				i = start;
			} else {
				j = start + n + 1;
				i = start + iStep - (n + 1);
			}
		} else {
			start = (n + 1) / 2;
			if (iStep < n + 1) {
				j = start - iStep;
				i = start;
			} else {
				j = start - (n + 1);
				i = start - (iStep - (n + 1));
			}
		}
		return new int[] { j, i };
	}

	private static long recursive(final TreeMap<int[], Long> map, final int[] nk) {
		nk[1] = Math.min(nk[1], nk[0] - nk[1]);
		if (nk[1] < 2) {
			return nk[1] == 0 ? 1 : nk[0];
		}
		final Long returnValue = map.get(nk);
		if (returnValue != null) {
			return returnValue;
		}
		final int[] nk1 = { nk[0] - 1, nk[1] - 1 };
		final long v1 = recursive(map, nk1);
		if (v1 == Long.MIN_VALUE) {
			return Long.MIN_VALUE;
		}
		final int[] nk2 = { nk[0] - 1, nk[1] };
		final long v2 = recursive(map, nk2);
		if (v2 == Long.MIN_VALUE || v2 > Long.MAX_VALUE - v1) {
			return Long.MIN_VALUE;
		}
		final long v = v1 + v2;
		map.put(nk, v);
		return v;
	}

	/**
	 * Returns Long.MIN_VALUE if the result would be bigger than Long.MAX_VALUE.
	 */
	public static long nChooseK(final int n, final int k) {
		if (k < 0 || k > n) {
			return 0;
		}
		final TreeMap<int[], Long> map = new TreeMap<>(_ByFirstAndSecond);
		return recursive(map, new int[] { n, k });
	}

	public static int compareForNulls(final Object o1, final Object o2) {
		return ((o1 == null) == (o2 == null)) ? 0 : (o1 == null ? -1 : 1);
	}

	/**
	 * Finds gcd of the absolute values of the inputs. If both are 0, Long.MAX_VALUE
	 * is returned.
	 */
	public static long getGcd(final long aIn, final long bIn) {
		long a = Math.abs(aIn), b = Math.abs(bIn);
		if (a == 0 || b == 0) {
			if (a == 0 && b == 0) {
				return Long.MAX_VALUE;
			}
			if (a == 0) {
				return b;
			}
			return a;
		}
		if (a < b) {
			final long c = a;
			a = b;
			b = c;
		}
		for (;;) {
			/** a >= b. */
			final long c = a % b;
			if (c == 0) {
				return b;
			}
			a = b;
			b = c;
		}
	}

	public static long getLcm(final long a, final long b) {
		final long gcd = getGcd(a, b);
		if (gcd == Long.MAX_VALUE) {
			return 0L;
		}
		return Math.abs(a / gcd) * Math.abs(b);
	}

	/** This is not sophisticated. */
	public static long[] getDivisors(final long n) {
		if (n <= 0) {
			return null;
		}
		if (n == 1) {
			return new long[] { 1 };
		}
		final ArrayList<Long> divisorList = new ArrayList<>();
		divisorList.add(1L);
		divisorList.add(n);
		long k;
		for (k = 2; k * k < n; ++k) {
			if (n % k == 0) {
				divisorList.add(k);
				divisorList.add(n / k);
			}
		}
		/** At this point, k*k >= n. If k*k == n, then k is also a divisor. */
		if (k * k == n) {
			divisorList.add(k);
		}
		final int nDivisors = divisorList.size();
		final long[] divisors = new long[nDivisors];
		final Iterator<Long> it = divisorList.iterator();
		for (int j = 0; j < nDivisors; ++j) {
			divisors[j] = it.next();
		}
		Arrays.sort(divisors);
		return divisors;
	}

	/**
	 * <pre>
	 * Returns a number 0, ..., |m|-1.
	 *   (5,3) = (5,-3) = 2.
	 *   (-5,3) = 1
	 *   (-5,-3) = 1.
	 * </pre>
	 */
	public static int generalMod(final int n, final int m) {
		final int absM = Math.abs(m);
		if (absM == 1) {
			return 0;
		}
		if (n >= 0) {
			return n % absM;
		}
		final int nn = m - (-n % m);
		return nn == m ? 0 : nn;
	}

	public static int getClosest(final long[] longs, final long ell) {
		/** Assume sorted ascending. */
		final int length = longs.length;
		final int glb = getGlbIndex(longs, ell);
		if (glb < 0) {
			return 0;
		}
		if (glb == length - 1) {
			return length - 1;
		}
		final long ell1 = ell - longs[glb];
		final long ell2 = longs[glb + 1] - ell;
		return ell1 < ell2 ? glb : (glb + 1);
	}

	public static int getClosest(final int[] ints, final int i) {
		/** Assume sorted ascending. */
		final int length = ints.length;
		final int glb = getGlbIndex(ints, i);
		if (glb < 0) {
			return 0;
		}
		if (glb == length - 1) {
			return length - 1;
		}
		final long i1 = i - ints[glb];
		final long i2 = ints[glb + 1] - i;
		return i1 < i2 ? glb : (glb + 1);
	}

	public static int[] getLowIndexes(int nToUse, final int n) {
		nToUse = Math.min(nToUse, n);
		final int[] returnValue = new int[nToUse];
		for (int k = 0; k < nToUse; ++k) {
			returnValue[k] = k;
		}
		return returnValue;
	}

	public static int[] getHighIndexes(int nToUse, final int n) {
		nToUse = Math.min(nToUse, n);
		final int[] returnValue = new int[nToUse];
		final int offset = n - nToUse;
		for (int k = 0; k < nToUse; ++k) {
			returnValue[k] = k + offset;
		}
		return returnValue;
	}

	public static int[] getMiddleIndexes(int nToUse, final int n) {
		nToUse = Math.min(nToUse, n);
		final int[] returnValue = new int[nToUse];
		final int offset = (n - nToUse) / 2;
		for (int k = 0; k < nToUse; ++k) {
			returnValue[k] = k + offset;
		}
		return returnValue;
	}

	public static int[] getSpreadOutIndexes(int nToUse, final int n) {
		nToUse = Math.min(nToUse, n);
		if (nToUse <= 1) {
			return new int[] { 0 };
		}
		final int gap = (n - 1) / (nToUse - 1);
		final int middle = (n - 1) / 2;
		final TreeSet<Integer> winners = new TreeSet<>();
		winners.add(middle);
		while (winners.size() < nToUse) {
			final int nGaps = (winners.size() + 1) / 2;
			if (winners.size() % 2 == 1) {
				winners.add(middle + nGaps * gap);
			} else {
				winners.add(middle - nGaps * gap);
			}
		}
		final int[] returnValue = new int[nToUse];
		int k = 0;
		for (final int winner : winners) {
			returnValue[k++] = winner;
		}
		return returnValue;
	}

	public static int compareBooleans(final boolean b1, final boolean b2) {
		return b1 == b2 ? 0 : (b1 ? 1 : -1);
	}

	public static int[] listToArray(final ArrayList<Integer> intList) {
		final int n = intList == null ? 0 : intList.size();
		final int[] ints = new int[n];
		for (int k = 0; k < n; ++k) {
			ints[k] = intList.get(k);
		}
		return ints;
	}

	/**
	 * <pre>
	 * k = 0: {0,0,0} k = 1: {1,1,1} k = 2: {1,1,0} k = 3: {1,1,-1}. Etc. Loops
	 * around each "ring from the top right." Returns {ring#,x-coord,y-coord}.
	 *
	 * <pre>
	 */
	public static class SpiralCoords {
		final public int _k;
		final public int _ring;
		final public int _x;
		final public int _y;

		public SpiralCoords(final int k, final int ring, final int x, final int y) {
			_k = k;
			_ring = ring;
			_x = x;
			_y = y;
		}

		public String getString() {
			return String.format("k[%d] ring/x/y[%d/%d/%d]", _k, _ring, _x, _y);
		}

		@Override
		public String toString() {
			return getString();
		}

	}

	public static SpiralCoords getSpiralCoords(final int k) {
		if (k == 0) {
			return new SpiralCoords(0, 0, 0, 0);
		}
		for (int ring = 1;; ++ring) {
			final int nInRing = 8 * ring;
			final int nInCore = 4 * ring * ring - 4 * ring + 1;
			int kInRing = k - nInCore;
			if (0 <= kInRing && kInRing < nInRing) {
				final int nInCol = 2 * ring + 1;
				if (kInRing < nInCol) {
					return new SpiralCoords(k, ring, ring, ring - kInRing);
				}
				kInRing -= nInCol;
				final int nInRow = nInCol - 2;
				if (kInRing < nInRow) {
					return new SpiralCoords(k, ring, ring - kInRing - 1, -ring);
				}
				kInRing -= nInRow;
				if (kInRing < nInCol) {
					return new SpiralCoords(k, ring, -ring, -ring + kInRing);
				}
				kInRing -= nInCol;
				return new SpiralCoords(k, ring, -ring + kInRing + 1, ring);
			}
		}
	}

	private static class Endpoint {
		int _startCount;
		int _endCount;

		private Endpoint(final boolean forStart) {
			_startCount = forStart ? 1 : 0;
			_endCount = forStart ? 0 : 1;
		}
	}

	public static int[][] union(final int[][] stretches) {
		final int nOrig = stretches.length;
		final TreeMap<Integer, Endpoint> endpoints = new TreeMap<>();
		for (int k = 0; k < nOrig; ++k) {
			final int start = Math.min(stretches[k][0], stretches[k][1]);
			final int end = Math.max(stretches[k][0], stretches[k][1]);
			boolean forStart = true;
			final Endpoint oldStartEndpoint = endpoints.put(start, new Endpoint(forStart));
			if (oldStartEndpoint != null) {
				++oldStartEndpoint._startCount;
				endpoints.put(start, oldStartEndpoint);
			}
			forStart = false;
			final Endpoint oldEndEndpoint = endpoints.put(end, new Endpoint(forStart));
			if (oldEndEndpoint != null) {
				++oldEndEndpoint._endCount;
				endpoints.put(end, oldEndEndpoint);
			}
		}
		final Iterator<Map.Entry<Integer, Endpoint>> it = endpoints.entrySet().iterator();
		final int nEntries = endpoints.size();
		int nActive = 0;
		int startValue = -1;
		final ArrayList<int[]> unionList = new ArrayList<>();
		for (int k = 0; k < nEntries; ++k) {
			final Map.Entry<Integer, Endpoint> entry = it.next();
			final int value = entry.getKey();
			final Endpoint endpoint = entry.getValue();
			final int oldNActive = nActive;
			nActive += endpoint._startCount - endpoint._endCount;
			if (oldNActive == 0 && nActive > 0) {
				startValue = value;
			} else if (oldNActive > 0 && nActive == 0) {
				unionList.add(new int[] { startValue, value });
				startValue = -1;
			}
		}
		return unionList.toArray(new int[unionList.size()][]);
	}

	public static class FibSeq {
		private int _fib0;
		private int _fib1;

		public FibSeq() {
			this(1, 1);
		}

		public FibSeq(final int fib0, final int fib1) {
			_fib0 = fib0;
			_fib1 = fib1;
		}

		public int nextFib() {
			if (_fib0 < 0) {
				return Integer.MIN_VALUE;
			}
			final int nextFib = _fib0;
			if (_fib1 > Integer.MAX_VALUE - _fib0) {
				_fib0 = _fib1;
				_fib1 = Integer.MIN_VALUE;
			} else {
				_fib1 += _fib0;
				_fib0 = _fib1 - _fib0;
			}
			return nextFib;
		}
	}

	// [969 520 976 514 978 519]
	public static int[] stringToIntArray(final String s) {
		final String[] fields0 = s.trim().split("[,\\s]+");
		final ArrayList<Integer> intList = new ArrayList<>();
		final int nFields = fields0.length;
		for (int k0 = 0; k0 < nFields; ++k0) {
			final String field0 = fields0[k0];
			String field = field0;
			if (field.startsWith("[")) {
				if (field.length() == 1) {
					continue;
				}
				field = field.substring(1);
			}
			if (field.endsWith("]")) {
				if (field.length() == 1) {
					continue;
				}
				field = field.substring(0, field.length() - 1);
			}
			try {
				intList.add(Integer.parseInt(field));
			} catch (final NumberFormatException e) {
				final String[] fields1 = field.trim().split("-");
				if (fields1.length == 2) {
					try {
						final int first0 = Integer.parseInt(fields1[0]);
						final int last0 = Integer.parseInt(fields1[1]);
						final int first = first0 <= last0 ? first0 : last0;
						final int last = first0 <= last0 ? last0 : first0;
						for (int k1 = first; k1 <= last; ++k1) {
							intList.add(k1);
						}
					} catch (final NumberFormatException e1) {
						/** Not worried. */
					}
				}
			}
		}
		final int nInts = intList.size();
		final int[] intArray = new int[nInts];
		for (int k = 0; k < nInts; ++k) {
			intArray[k] = intList.get(k);
		}
		return intArray;
	}

	public static BitSet intArrayToBitSet(final int[] intArray) {
		final BitSet bitSet = new BitSet();
		final int n = intArray == null ? 0 : intArray.length;
		if (n == 0) {
			return bitSet;
		}
		for (int k = 0; k < n; ++k) {
			bitSet.set(intArray[k]);
		}
		return bitSet;
	}

	public static int[] bitSetToIntArray(final BitSet bitSet) {
		final int n = bitSet == null ? 0 : bitSet.cardinality();
		final int[] intArray = new int[n];
		if (n == 0) {
			return intArray;
		}
		for (int k = 0, setBit = bitSet.nextSetBit(0); setBit >= 0; setBit = bitSet.nextSetBit(setBit + 1), ++k) {
			intArray[k] = setBit;
		}
		return intArray;
	}

	public static String bitSetToString(final BitSet bitSet) {
		return bitSetToString(bitSet, ',');
	}

	public static String bitSetToString(final BitSet bitSet, final char separator) {
		int firstInStreak = Integer.MIN_VALUE;
		int lastInStreak = Integer.MIN_VALUE;
		String s = "";
		for (int k = bitSet.nextSetBit(0); k != -1; k = bitSet.nextSetBit(k + 1)) {
			if (k > lastInStreak + 1) {
				if (lastInStreak >= 0) {
					/** We have an old streak to print out. */
					if (s.length() > 0) {
						s += separator;
					}
					s += firstInStreak;
					if (lastInStreak > firstInStreak) {
						s += String.format("-%d", lastInStreak);
					}
				}
				firstInStreak = lastInStreak = k;
			} else {
				lastInStreak = k;
			}
		}
		/** Print out the last streak. */
		if (s.length() > 0) {
			s += separator;
		}
		if (lastInStreak >= 0) {
			s += firstInStreak;
			if (lastInStreak > firstInStreak) {
				s += String.format("-%d", lastInStreak);
			}
		}
		return s;
	}

	public static BitSet stringToBitset(final String s) {
		return stringToBitset(s, 0, Integer.MAX_VALUE);
	}

	public static BitSet[] deepCopy(final BitSet[] bitSets) {
		final int nBitSets = bitSets.length;
		final BitSet[] newBitSets = new BitSet[nBitSets];
		for (int kBitSet = 0; kBitSet < nBitSets; ++kBitSet) {
			newBitSets[kBitSet] = (BitSet) bitSets[kBitSet].clone();
		}
		return newBitSets;
	}

	public static BitSet stringToBitset(final String s, int low, int high) {
		low = Math.max(low, 0);
		high = Math.max(high, low);
		final String s0;
		final String s1;
		int nextX = s.lastIndexOf('x');
		if (nextX < 0) {
			nextX = s.lastIndexOf('X');
		}
		if (0 < nextX && nextX < s.length()) {
			s0 = s.substring(0, nextX);
			s1 = s.substring(nextX + 1);
		} else {
			s0 = s;
			s1 = null;
		}
		final String[] fields0 = s0.trim().split("[,\\s]+");
		final BitSet bitSet = new BitSet();
		final int nFields = fields0.length;
		for (int k = 0; k < nFields; ++k) {
			final String field = fields0[k];
			try {
				final int singleton = Integer.parseInt(field);
				if (low <= singleton && singleton <= high) {
					bitSet.set(singleton);
				}
			} catch (final NumberFormatException e) {
				final String[] fields1 = field.trim().split("-");
				if (fields1.length == 2) {
					try {
						final int firstInStreak = Math.max(low, Integer.parseInt(fields1[0]));
						final int lastInStreak = Math.min(Integer.parseInt(fields1[1]), high);
						if (firstInStreak <= lastInStreak) {
							bitSet.set(firstInStreak, lastInStreak + 1);
						}
					} catch (final NumberFormatException e1) {
						/** Not worried. */
					}
				}
			}
		}
		if (s1 != null) {
			try {
				final int increment = Integer.parseInt(s1.trim());
				if (increment < 0) {
					return bitSet;
				}
				final BitSet bitSet2 = new BitSet();
				for (int base = 0;; base += increment) {
					for (int bit0 = bitSet.nextSetBit(0); bit0 >= 0; bit0 = bitSet.nextSetBit(bit0 + 1)) {
						final int bit = base + bit0;
						if (bit > high) {
							return bitSet2;
						}
						bitSet2.set(bit);
					}
				}
			} catch (final NumberFormatException e) {
				/** Not worried. */
			}
		}
		return bitSet;
	}

	private static String getAddOn(int k, final String uniquifiers) {
		int nDigits = 0;
		int upper = 0;
		final int nUniqs = uniquifiers.length();
		int power = nUniqs;
		while (k > upper) {
			++nDigits;
			upper += power;
			power *= nUniqs;
		}
		if (nDigits == 0) {
			return "";
		}
		String ss = "";
		power = 1;
		for (int k0 = 0; k0 < nDigits; ++k0) {
			k -= power;
			power *= nUniqs;
		}
		for (int k0 = 0; k0 < nDigits; ++k0) {
			final int digit = k % nUniqs;
			ss = uniquifiers.substring(digit, digit + 1) + ss;
			k /= nUniqs;
		}
		return ss;
	}

	public static String alterString(final String s, final int maxLen, final String uniquifiers, final int kTry) {
		final String addOn = getAddOn(kTry, uniquifiers);
		final int addOnLen = addOn.length();
		if (addOnLen == 0) {
			return s;
		}
		final int sLen = s.length();
		String ss;
		if (sLen > maxLen) {
			ss = s.substring(0, maxLen);
		} else {
			ss = s;
		}
		final int ssLen = ss.length();
		return ss.substring(0, Math.min(ssLen, maxLen - addOnLen)) + '+' + addOn;
	}

	/**
	 * Without the case, _SeedMod would be the negative number Integer.MIN_VALUE. We
	 * want a positive number.
	 */
	private static final long _ModForLongToInt = -(long) Integer.MIN_VALUE;

	public static int longToInt(final long aLong) {
		if (aLong >= 0) {
			return (int) (aLong % _ModForLongToInt);
		}
		return (int) ((-aLong % _ModForLongToInt) - 1L);
	}

	/** For most of these, we simply appeal to the NumericalRoutines version. */
	private static double[] intsToDoubles(final int[] ints) {
		final int m = ints == null ? 0 : ints.length;
		final double[] doubles = new double[m];
		for (int i = 0; i < m; ++i) {
			doubles[i] = ints[i];
		}
		return doubles;
	}

	private static double[][] intsToDoubles(final int[][] ints) {
		final int m = ints == null ? 0 : ints.length;
		final double[][] doubles = new double[m][];
		for (int i = 0; i < m; ++i) {
			doubles[i] = intsToDoubles(ints[i]);
		}
		return doubles;
	}

	public static String getString(final int[][] matrix) {
		return NumericalRoutines.getString(intsToDoubles(matrix));
	}

	public static String getString(final int[] vector) {
		return NumericalRoutines.getString(intsToDoubles(vector));
	}

	private static double[] longsToDoubles(final long[] longs) {
		final int m = longs == null ? 0 : longs.length;
		final double[] doubles = new double[m];
		for (int i = 0; i < m; ++i) {
			doubles[i] = longs[i];
		}
		return doubles;
	}

	private static double[][] longsToDoubles(final long[][] longs) {
		final int m = longs == null ? 0 : longs.length;
		final double[][] doubles = new double[m][];
		for (int i = 0; i < m; ++i) {
			doubles[i] = longsToDoubles(longs[i]);
		}
		return doubles;
	}

	public static String getString(final long[][] matrix) {
		return NumericalRoutines.getString(longsToDoubles(matrix));
	}

	public static String getString(final long[] vector) {
		return NumericalRoutines.getString(longsToDoubles(vector));
	}

	public static void main(final String[] args) {
		final int a = 72, b = 36;
		final long gcd = getGcd(a, b);
		System.out.println(gcd);
	}

}
