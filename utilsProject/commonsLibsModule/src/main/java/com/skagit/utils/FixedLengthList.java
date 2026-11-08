package com.skagit.utils;

public class FixedLengthList<T extends Object> {
    final private T[] _data;
    private int _k, _size;

    @SuppressWarnings("unchecked")
    public FixedLengthList(final int length) {
	_data = (T[]) new Object[length];
	clear();
    }

    /** Not only adds it, but resets _k so that the new one is the active one. */
    public void add(final T t) {
	final int length = _data.length;
	if (_data[length - 1] == null) {
	    /**
	     * We have room. Shift from (_k + 1) to (_size - 1) up, and put it in (_k + 1).
	     */
	    for (int k = _size - 1; k >= _k + 1; --k) {
		_data[k + 1] = _data[k];
	    }
	    ++_size;
	    _data[++_k] = t;
	} else {
	    if (_k - 1 > length - 1 - _k) {
		/**
		 * Discard Low. Shift 2 up to _k down 1, and put the new one at _k.
		 */
		for (int k = 2; k <= _k; ++k) {
		    _data[k - 1] = _data[k];
		}
		_data[_k] = t;
	    } else {
		/**
		 * Discard high. Shift (_k + 1) up to (length - 2) up 1, and put the new one at
		 * (_k + 1).
		 */
		for (int k = length - 2; k >= _k + 1; --k) {
		    _data[k + 1] = _data[k];
		}
		_data[++_k] = t;
	    }
	}
    }

    public boolean canDoPrevious() {
	return _k > 0;
    }

    public boolean firstIsLessThanPrevious() {
	return _k > 1;
    }

    public T doPrevious() {
	return canDoPrevious() ? _data[--_k] : null;
    }

    public boolean canDoNext() {
	return _k + 1 < _size;
    }

    public boolean lastIsBiggerThanNext() {
	return _k + 2 < _size;
    }

    public T doNext() {
	return canDoNext() ? _data[++_k] : null;
    }

    public T getActive() {
	return _k >= 0 ? _data[_k] : null;
    }

    public T doFirst() {
	_k = Math.min(_k, 0);
	if (0 <= _k && _k < _data.length) {
	    return _data[_k];
	}
	return null;
    }

    public T doLast() {
	_k = _size - 1;
	if (0 <= _k && _k < _data.length) {
	    return _data[_k];
	}
	return null;
    }

    public void clear() {
	final int length = _data.length;
	for (int k = 0; k < length; ++k) {
	    _data[k] = null;
	}
	_k = -1;
	_size = 0;
    }
}
