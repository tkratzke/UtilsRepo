package com.skagit.utils;

import java.util.BitSet;

public class BitSetPlus extends BitSet {
	private static final long serialVersionUID = 1L;

	/** For getString(): */
	final private static int _MaxToPrintOut = 10;
	final private static int _TopOfLowSide = (_MaxToPrintOut + 1) / 2;
	final private static int _MaxPerLine = 8;

	public BitSetPlus(final int size) {
		super(size == Integer.MIN_VALUE ? Integer.MAX_VALUE : size);
		if (size == Integer.MIN_VALUE) {
			set(Integer.MAX_VALUE);
			clear(Integer.MAX_VALUE);
		}
	}

	/** Key routine for kToBit. */
	public int cardinality(final int from, final int to) {
		final boolean askingForMaxValue = to == Integer.MIN_VALUE;
		if ((!askingForMaxValue && to < 0) || from < 0) {
			throw new IndexOutOfBoundsException();
		}
		if (!askingForMaxValue && from > to) {
			throw new IndexOutOfBoundsException();
		}
		if (from == to) {
			return 0;
		}
		final int toToUse = askingForMaxValue ? Integer.MAX_VALUE : to;
		final BitSet bitSet = new BitSet(toToUse);
		bitSet.set(from, toToUse);
		if (askingForMaxValue && get(Integer.MAX_VALUE)) {
			bitSet.set(Integer.MAX_VALUE);
		}
		bitSet.and(this);
		return bitSet.cardinality();
	}

	protected int kToBitEstimate(final int bitLow, final int kLow, final int bitHigh, final int kHigh, final int k) {
		final double kDiff = kHigh - kLow;
		final double bitDiff = bitHigh - bitLow;
		final long deltaBit = Math.round(bitDiff * (((double) k - kLow) / kDiff));
		final int bitEstimate = (int) (bitLow + deltaBit);
		if (bitLow < bitEstimate && bitEstimate < bitHigh) {
			return bitEstimate;
		}
		return (bitLow + bitHigh) / 2;
	}

	public int kToBit(final int k) {
		/** WKT = "We know that.". */
		if (Integer.MIN_VALUE < k && k <= 0) {
			throw new IndexOutOfBoundsException();
		}
		final int cardinality = cardinality();
		if (k == Integer.MIN_VALUE) {
			if (cardinality == Integer.MIN_VALUE) {
				return Integer.MAX_VALUE;
			}
			throw new IndexOutOfBoundsException();
		}
		/** WKT k is positive. */
		if (cardinality == Integer.MIN_VALUE) {
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
		int kLow = 1, bitLow = nextSetBit(0);
		int kHigh = cardinality, bitHigh = length() - 1;
		while (kLow < k && k < kHigh) {
			/**
			 * <pre>
			 * WKT the kth one is at bit, and bitLow < bit < bitHigh,
			 * since the kth one does not occur at bitLow or bitHigh,
			 * and those are the bits for kLow and kHigh.
			 * </pre>
			 */
			final int bitNew0 = kToBitEstimate(bitLow, kLow, bitHigh, kHigh, k);
			final int bitNew1 = previousSetBit(bitNew0);
			final int bitNew = bitNew1 > bitLow ? bitNew1 : nextSetBit(bitLow + 1);
			final int bitNewMinusLow = bitNew - bitLow;
			final int bitHighMinusNew = bitHigh - bitNew;
			final int kNew;
			if (bitNewMinusLow <= bitHighMinusNew) {
				/** WKT bitLow < newBit and newBit is a member. */
				final int card = cardinality(bitLow, bitNew);
				/**
				 * <pre>
				 * To find newK when we go from bitLow to newBit,
				 * we start with kLow and:
				 * 1. add all of bitSet,
				 * 2. subtract 1 for double-counting bitLow, and
				 * 3. add 1 for newBit.
				 * </pre>
				 */
				kNew = kLow + card;
			} else {
				/** WKT newBit is a member and newBit < bitHigh. */
				final int card = cardinality(bitNew, bitHigh);
				/**
				 * <pre>
				 * To find newK when we go from bitHigh to newBit,
				 * we start with kHigh and:
				 * 1. subtract 1 for losing bitHigh,
				 * 2. subtract all of bitSet, and
				 * 3. add 1 to get back newBit.
				 * </pre>
				 */
				kNew = kHigh - card;
			}
			if (kNew <= k) {
				kLow = kNew;
				bitLow = bitNew;
			} else {
				kHigh = kNew;
				bitHigh = bitNew;
			}
		}
		if (kLow == k) {
			return bitLow;
		}
		return bitHigh;
	}

	@Override
	public void set(final int from, final int to, final boolean set) {
		if (from == to) {
			return;
		}
		if (Integer.MIN_VALUE < to && to < 0) {
			throw new IndexOutOfBoundsException();
		}
		if (to == Integer.MIN_VALUE) {
			super.set(from, Integer.MAX_VALUE, set);
			set(Integer.MAX_VALUE, set);
			return;
		}
		super.set(from, to, set);
	}

	/** Debugging. */
	public String getString() {
		final int len = length(), sz = size(), card = cardinality();
		final StringBuilder s = new StringBuilder().append(String.format("<length[%d] size[%d] card[%d]>", //
				len, sz, card));
		final int[] elements;
		if (card == Integer.MIN_VALUE || card >= _MaxToPrintOut) {
			elements = new int[_MaxToPrintOut];
			for (int bit = nextSetBit(0), k = 0; //
					k <= _TopOfLowSide; //
					bit = nextSetBit(bit + 1), ++k) {
				elements[k] = bit;
			}
			for (int bit = previousSetBit(len - 1), k = _MaxToPrintOut - 1; //
					k > _TopOfLowSide; //
					bit = previousSetBit(bit - 1), --k) {
				elements[k] = bit;
			}
		} else {
			elements = new int[card];
			int k = 0;
			for (int bit = nextSetBit(0); bit >= 0; bit = nextSetBit(bit + 1)) {
				elements[k++] = bit;
				if (bit == Integer.MAX_VALUE) {
					break;
				}
			}
		}
		int nNumbersPrinted = 0;
		final int nElements = elements.length;
		for (int k = 0; k < nElements; ++k) {
			final long bit = elements[k];
			final String s0;
			if (nNumbersPrinted == 0) {
				s0 = nElements >= _MaxPerLine ? " [\n " : " [";
			} else {
				s0 = nNumbersPrinted % _MaxPerLine == 0 ? "\n " : ", ";
			}
			s.append(String.format("%s%d", s0, bit));
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
