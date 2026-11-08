package com.skagit.utils;

/** <pre>
 * For large primes, see https://primes.utm.edu/largest.html
 * For prime calculator, see https://www.numberempire.com/primenumbers.php
 * </pre>
 */

import java.io.Serializable;
import java.util.Arrays;

public class BitSetPlus2 implements Serializable {

	private static final long serialVersionUID = 1L;

	private final static int _MaxPerLine = 8;

	private final int _nBitsPerBitSet;
	private BitSetPlus[] _bitSetPlusArray;

	final private static int _DefaultNBitsPerBitSetPlus = 1 << 6;

	public BitSetPlus2(final long size) {
		this(size, _DefaultNBitsPerBitSetPlus);
	}

	public BitSetPlus2(final long size, final int nBitsPerBitSet) {
		_nBitsPerBitSet = nBitsPerBitSet;
		final long nBitsPerBitSetL = getNBitsPerBitSetL();
		final int nBitSetPluses = (int) ((size + nBitsPerBitSetL - 1L) / nBitsPerBitSetL);
		_bitSetPlusArray = new BitSetPlus[nBitSetPluses];
		Arrays.fill(_bitSetPlusArray, null);
	}

	public BitSetPlus2() {
		_nBitsPerBitSet = _DefaultNBitsPerBitSetPlus;
		_bitSetPlusArray = new BitSetPlus[1];
		_bitSetPlusArray[0] = buildLocalBitSetPlus();
	}

	/** Little utility methods. */
	private BitSetPlus buildLocalBitSetPlus() {
		return new BitSetPlus(_nBitsPerBitSet);
	}

	private int[] ellToIj(final long ell) {
		final long nBitsPerBitSetL = getNBitsPerBitSetL();
		final int i = (int) (ell / nBitsPerBitSetL);
		final int j = (int) (ell % nBitsPerBitSetL);
		return new int[] { i, j };
	}

	private long getNBitsPerBitSetL() {
		return _nBitsPerBitSet == Integer.MIN_VALUE ? -(long) Integer.MIN_VALUE : _nBitsPerBitSet;
	}

	/** length, size, and cardinality. */
	public long length() {
		final int nBitSetPluses = _bitSetPlusArray.length;
		for (int j = nBitSetPluses - 1; j >= 0; --j) {
			final BitSetPlus bitSetPlus = _bitSetPlusArray[j];
			if (bitSetPlus != null) {
				final long nBitsPerBitSetL = getNBitsPerBitSetL();
				final long len = j * nBitsPerBitSetL + bitSetPlus.length();
				return len;
			}
		}
		return 0L;
	}

	public long size() {
		final int nBitSets = _bitSetPlusArray.length;
		return nBitSets * getNBitsPerBitSetL();
	}

	public long cardinality() {
		long cardinality = 0L;
		final int nBitSetPluses = _bitSetPlusArray.length;
		for (int j = 0; j < nBitSetPluses; ++j) {
			final BitSetPlus bitSetPlus = _bitSetPlusArray[j];
			if (bitSetPlus != null) {
				cardinality += bitSetPlus.cardinality();
			}
		}
		return cardinality;
	}

	private void setOrClearFromTo(final boolean set, final long from, final long to) {
		if (to < 0 || to < from) {
			throw new IndexOutOfBoundsException();
		}
		if (from == to) {
			return;
		}
		final int[] ijFrom = ellToIj(from);
		final int iFrom = ijFrom[0];
		final int jFrom = ijFrom[1];

		final int[] ijTo = ellToIj(to);
		final int iTo = ijTo[0];
		final int jTo = ijTo[1];

		final int nOldBitSetPluses = _bitSetPlusArray == null ? 0 : _bitSetPlusArray.length;
		if (set && iTo >= nOldBitSetPluses) {
			final BitSetPlus[] bitSetPlusArray = new BitSetPlus[iTo + 1];
			if (_bitSetPlusArray != null) {
				System.arraycopy(_bitSetPlusArray, 0, bitSetPlusArray, 0, nOldBitSetPluses);
			}
			Arrays.fill(bitSetPlusArray, nOldBitSetPluses, iTo + 1, null);
			_bitSetPlusArray = bitSetPlusArray;
		}
		final int nBitSetPluses = _bitSetPlusArray.length;
		final int upperLimitOnITo = Math.min(nBitSetPluses - 1, iTo);

		for (int i = iFrom; i <= upperLimitOnITo; ++i) {
			BitSetPlus bitSetPlus = i >= nBitSetPluses ? null : _bitSetPlusArray[i];
			if (set && bitSetPlus == null) {
				bitSetPlus = _bitSetPlusArray[i] = buildLocalBitSetPlus();
			}
			if (!set && bitSetPlus == null) {
				continue;
			}
			final int jFromLocal = i == iFrom ? jFrom : 0;
			final int jToLocal = i < iTo ? _nBitsPerBitSet : jTo;
			bitSetPlus.set(jFromLocal, jToLocal, set);
		}
	}

	public void set(final long from, final long to) {
		setOrClearFromTo(/* set= */true, from, to);
	}

	public void clear(final long from, final long to) {
		setOrClearFromTo(/* set= */false, from, to);
	}

	/** get, clear, and set singletons. */
	private void setOrClear(final boolean set, final long ell) {
		final int[] ij = ellToIj(ell);
		final int i = ij[0], j = ij[1];
		final int nOldBitSetPluses = _bitSetPlusArray.length;
		final int oldLastBitSetPlus = nOldBitSetPluses - 1;
		if (i > oldLastBitSetPlus) {
			if (!set) {
				return;
			}
			final BitSetPlus[] bitSetPlusArray = new BitSetPlus[i + 1];
			System.arraycopy(_bitSetPlusArray, 0, bitSetPlusArray, 0, nOldBitSetPluses);
			Arrays.fill(bitSetPlusArray, nOldBitSetPluses, i + 1, null);
			_bitSetPlusArray = bitSetPlusArray;
		}
		BitSetPlus bitSetPlus = _bitSetPlusArray[i];
		if (bitSetPlus == null) {
			bitSetPlus = _bitSetPlusArray[i] = buildLocalBitSetPlus();
		}
		if (set) {
			bitSetPlus.set(j);
		} else {
			bitSetPlus.clear(j);
		}
	}

	public void set(final long ell, final boolean set) {
		setOrClear(set, ell);
	}

	public void set(final long ell) {
		setOrClear(/* set= */true, ell);
	}

	public void clear(final long ell) {
		setOrClear(/* set= */false, ell);
	}

	public boolean get(final long ell) {
		final int[] ij = ellToIj(ell);
		final int i = ij[0];
		final int nOldBitSetPluses = _bitSetPlusArray.length;
		final int oldLastBitSetPlus = nOldBitSetPluses - 1;
		if (i > oldLastBitSetPlus) {
			return false;
		}
		final BitSetPlus bitSetPlus = _bitSetPlusArray[i];
		final int j = ij[1];
		return bitSetPlus == null ? false : bitSetPlus.get(j);
	}

	/** cardinality(from, to). Key method for inverting get(ell). */
	public long cardinality(final long from, final long to) {
		if (from < 0 || to < from) {
			throw new IndexOutOfBoundsException();
		}
		if (from == to) {
			return 0;
		}
		final int nBitSetPluses = _bitSetPlusArray == null ? 0 : _bitSetPlusArray.length;
		final int[] ijFrom = ellToIj(from);
		final int iFrom = ijFrom[0];
		if (iFrom > nBitSetPluses - 1) {
			return 0L;
		}
		final int jFrom = ijFrom[1];

		final int[] ijTo = ellToIj(to);
		final int iTo = ijTo[0];
		final int jTo = ijTo[1];

		long card = 0L;
		final int iEnd = Math.min(iTo, nBitSetPluses - 1);
		for (int i = iFrom; i <= iEnd; ++i) {
			final BitSetPlus bitSetPlus = _bitSetPlusArray[i];
			if (bitSetPlus == null) {
				continue;
			}
			final int jLo = i == iFrom ? jFrom : 0;
			final int jHi = i == iTo ? jTo : _nBitsPerBitSet;
			if (jLo == 0 && jHi == _nBitsPerBitSet) {
				card += bitSetPlus.cardinality();
			} else {
				card += bitSetPlus.cardinality(jLo, jHi);
			}
			if (i == iEnd) {
				break;
			}
		}
		return card;
	}

	/** nextSetBit and nextClearBit. */
	private long nextSetOrClear(final boolean set, final long ell) {
		final int[] ij = ellToIj(ell);
		final int i = ij[0], j = ij[1];
		final int nBitSetPluses = _bitSetPlusArray == null ? 0 : _bitSetPlusArray.length;
		final boolean longEnough = nBitSetPluses - 1 >= i;
		if (!set && !longEnough) {
			return ell;
		}

		for (int i0 = i; i0 < nBitSetPluses; ++i0) {
			final BitSetPlus bitSetPlus = _bitSetPlusArray[i0];
			if (set && bitSetPlus == null) {
				continue;
			}
			final int j0 = i0 == i ? j : 0;
			if (!set && bitSetPlus == null) {
				return i0 * _nBitsPerBitSet + j0;
			}
			final int j1;
			if (set) {
				j1 = bitSetPlus.nextSetBit(j0);
			} else {
				j1 = bitSetPlus.nextClearBit(j0);
			}
			if (j1 < 0) {
				continue;
			}
			return i0 * _nBitsPerBitSet + j1;
		}
		return set ? -1L : (nBitSetPluses * _nBitsPerBitSet);
	}

	public long nextSetBit(final long ell) {
		return nextSetOrClear(/* set= */true, ell);
	}

	public long nextClearBit(final long ell) {
		return nextSetOrClear(/* set= */false, ell);
	}

	/** previousSetBit and previousClearBit. */
	private long previousSetOrClear(final boolean set, final long ell) {
		final int[] ij = ellToIj(ell);
		final int i = ij[0], j = ij[1];
		final int nBitSetPluses = _bitSetPlusArray == null ? 0 : _bitSetPlusArray.length;
		final boolean longEnough = nBitSetPluses - 1 >= i;
		if (!set && !longEnough) {
			return ell;
		}

		for (int i0 = Math.min(nBitSetPluses - 1, i); i0 >= 0; --i0) {
			final BitSetPlus bitSetPlus = _bitSetPlusArray[i0];
			final int j0 = i0 == i ? j : (_nBitsPerBitSet - 1);
			if (bitSetPlus == null) {
				if (set) {
					continue;
				}
				return i0 * _nBitsPerBitSet + j0;
			}
			final int j1;
			if (set) {
				j1 = bitSetPlus.previousSetBit(j0);
			} else {
				j1 = bitSetPlus.previousClearBit(j0);
			}
			if (0 <= j1 && j1 < _nBitsPerBitSet) {
				return i0 * _nBitsPerBitSet + j1;
			}
		}
		return -1L;
	}

	public long previousSetBit(final long ell) {
		return previousSetOrClear(/* set= */true, ell);
	}

	public long previousClearBit(final long ell) {
		return previousSetOrClear(/* set= */false, ell);
	}

	public long kToEll(final long k) {
		/** WKT = "We know that.". */
		if (Integer.MIN_VALUE < k && k <= 0) {
			throw new IndexOutOfBoundsException();
		}
		final long cardinality = cardinality();
		if (k < 0L) {
			throw new IndexOutOfBoundsException();
		}
		/** WKT k is positive. */
		if (cardinality == size()) {
			/** WKT everything is set. The (e.g.) 5th one is at bit 4. */
			return k - 1;
		}
		/** WKT both k and cardinality are positive. */
		if (cardinality < k) {
			throw new IndexOutOfBoundsException();
		}

		/**
		 * <pre>
		 * AllDifferent kLow, bitLow, kHigh, and bitHigh such that:
		 * 1. There are kLow from 0 up to and including bitLow, which is always a member.
		 * 2. There are kHigh from 0 up to and including bitHigh, which is always a member.
		 * 3. 0 < kLow <= k <= kHigh.
		 * These 3 facts remain true for all iterations.
		 * Note that if Integer.MAX_VALUE is set, then length() returns Integer.MIN_VALUE,
		 * and length() - 1 will be Integer.MAX_VALUE.  So length() - 1 always works
		 * for finding the highest set bit.
		 * </pre>
		 */
		long kLow = 1, bitLow = nextSetBit(0);
		long kHigh = cardinality, bitHigh = length() - 1;
		while (kLow < k && k < kHigh) {
			/**
			 * <pre>
			 * WKT the kth one is at bit, and bitLow < bit < bitHigh,
			 * since the kth one does not occur at bitLow or bitHigh,
			 * and those are the bits for kLow and kHigh.
			 * </pre>
			 */
			final long kDiff = kHigh - kLow;
			final long bitHiMinusLo = bitHigh - bitLow;
			final long bitNewMinusLo0 = Math.round(((double) k - kLow) / kDiff * bitHiMinusLo);
			/**
			 * WKT bitLow <= bitLow + bitNewMinusLo0 <= bitHigh. Find a bit between bitLow
			 * and bitHigh that is a member and call it newBit.
			 */
			final long newBit0 = previousSetBit(bitLow + bitNewMinusLo0);
			final long newBit = newBit0 > bitLow ? newBit0 : nextSetBit(bitLow + 1);
			final long bitNewMinusLow = newBit - bitLow;
			final long newK;
			if (bitNewMinusLow <= bitHiMinusLo / 2) {
				/** WKT bitLow < newBit and newBit is a member. */
				final long card = cardinality(bitLow, newBit);
				/**
				 * <pre>
				 * To find newK when we go from bitLow to newBit,
				 * we start with kLow and:
				 * 1. add all of bitSet,
				 * 2. subtract 1 for double-counting bitLow, and
				 * 3. add 1 for newBit.
				 * </pre>
				 */
				newK = kLow + card;
			} else {
				/** WKT newBit is a member and newBit < bitHigh. */
				final long card = cardinality(newBit, bitHigh);
				/**
				 * <pre>
				 * To find newK when we go from bitHigh to newBit,
				 * we start with kHigh and:
				 * 1. subtract 1 for losing bitHigh,
				 * 2. subtract all of bitSet, and
				 * 3. add 1 to get back newBit.
				 * </pre>
				 */
				newK = kHigh - card;
			}
			if (newK <= k) {
				kLow = newK;
				bitLow = newBit;
			} else {
				kHigh = newK;
				bitHigh = newBit;
			}
		}
		if (kLow == k) {
			return bitLow;
		}
		return bitHigh;
	}

	/** Debugging. */
	public String getString() {
		final long len = length(), sz = size(), card = cardinality();
		final StringBuilder s = new StringBuilder()
				.append(String.format("<length[%d] size[%d] card[%d]>", len, sz, card));
		final long[] elements;
		if (card > 10) {
			elements = new long[10];
			int k = 0;
			for (long ell = nextSetBit(0L); k < 5; ell = nextSetBit(ell + 1L)) {
				elements[k++] = ell;
			}
			for (long ell = previousSetBit(length() - 1); k < 10; ell = previousSetBit(ell - 1L)) {
				elements[k++] = ell;
			}
		} else {
			elements = new long[(int) cardinality()];
			int k = 0;
			for (long ell = nextSetBit(0L); ell >= 0; ell = nextSetBit(ell + 1L)) {
				elements[k++] = ell;
			}
		}
		int nNumbersPrinted = 0;
		final int nElements = elements.length;
		for (int k = 0; k < nElements; ++k) {
			final long ell = elements[k];
			final String s0;
			if (nNumbersPrinted == 0) {
				s0 = nElements >= _MaxPerLine ? " [\n " : " [";
			} else {
				s0 = nNumbersPrinted % _MaxPerLine == 0 ? "\n " : ", ";
			}
			s.append(String.format("%s%d", s0, ell));
			++nNumbersPrinted;
		}
		s.append(nElements >= _MaxPerLine ? "\n]" : (nElements > 0 ? "]" : ""));
		return s.toString();
	}

	@Override
	public String toString() {
		return getString();
	}

}
