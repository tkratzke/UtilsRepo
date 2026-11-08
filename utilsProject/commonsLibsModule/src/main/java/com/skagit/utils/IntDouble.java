package com.skagit.utils;

public class IntDouble implements Comparable<IntDouble> {
    public final int _i;
    public final double _d;

    public IntDouble(final int i, final double d) {
	_i = i;
	_d = d;
    }

    public IntDouble(final int i) {
	_i = i;
	_d = Double.NaN;
    }

    @Override
    public boolean equals(final Object intDouble) {
	if (intDouble == null || !(intDouble instanceof IntDouble)) {
	    return false;
	}
	return _i == ((IntDouble) intDouble)._i;
    }

    @Override
    public int hashCode() {
	return Integer.hashCode(_i);
    }

    @Override
    public int compareTo(final IntDouble intDouble) {
	return _i < intDouble._i ? -1 : (_i > intDouble._i ? 1 : 0);
    }
}
