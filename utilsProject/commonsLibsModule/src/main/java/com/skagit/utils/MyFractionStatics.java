package com.skagit.utils;

import java.util.BitSet;

public class MyFractionStatics {

	/** Allows some use of parentheses. Returns null if it gets confused. */
	public static MyFraction getFraction(String numberString) {
		numberString = numberString.replaceAll("\\s+/", "/");
		numberString = numberString.replaceAll("/\\s+", "/");
		numberString = numberString.replaceAll("\\s+\\(", "(");
		numberString = numberString.replaceAll("\\)\\s+", ")");
		return privateGetFraction(numberString);
	}

	private static MyFraction privateGetFraction(final String numberString) {
		MyFraction frctn = new MyFraction(numberString);
		if (MyFraction.IsValid(frctn)) {
			return frctn;
		}
		final int len = numberString.length();
		final BitSet lefts = new BitSet(len);
		final BitSet rights = new BitSet(len);
		int nExcessLefts = 0;
		for (int k = 0; k < len; ++k) {
			final char c = numberString.charAt(k);
			if (c == '(') {
				++nExcessLefts;
				lefts.set(k);
			} else if (c == ')') {
				--nExcessLefts;
				if (nExcessLefts < 0) {
					return null;
				}
				rights.set(k);
			}
		}
		if (nExcessLefts != 0) {
			return null;
		}
		if (!lefts.isEmpty()) {
			final int lastLeft = lefts.previousSetBit(len);
			final int nextRight = rights.nextSetBit(lastLeft + 1);
			final String s0 = numberString.substring(lastLeft + 1, nextRight);
			frctn = privateGetFraction(s0);
			if (frctn == null) {
				return null;
			}
			/** If there is something to the left of lastLeft, it better be a slash. */
			if (lastLeft > 0) {
				if (numberString.charAt(lastLeft - 1) != '/') {
					return null;
				}
				final MyFraction frctnBefore = privateGetFraction(numberString.substring(0, lastLeft - 1));
				if (frctnBefore == null) {
					return null;
				}
				frctn = frctnBefore.divide(frctn, /* operateOnSelf= */false);
			}
			/** Similar with to the right of nextRight. */
			if (nextRight == len - 1) {
				return frctn;
			}
			if (numberString.charAt(nextRight + 1) != '/') {
				return null;
			}
			final MyFraction frctnAfter = privateGetFraction(numberString.substring(nextRight + 2));
			if (frctnAfter == null) {
				return null;
			}
			frctn.divide(frctnAfter, /* operateOnSelf= */true);
			return frctn;
		}
		final String[] fields = numberString.split("/");
		final int nFields = fields.length;
		if (nFields == 0) {
			return new MyFraction(1L, 1L);
		}
		try {
			final long num = Long.parseLong(fields[0]);
			long den = 1L;
			for (int k = 1; k < nFields; ++k) {
				den *= Long.parseLong(fields[k]);
			}
			return new MyFraction(num, den);
		} catch (final NumberFormatException e) {
		}
		return null;
	}

}
