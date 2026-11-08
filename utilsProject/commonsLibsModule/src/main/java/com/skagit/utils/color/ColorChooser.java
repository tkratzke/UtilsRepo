package com.skagit.utils.color;

import com.skagit.utils.NumericalRoutines;

public class ColorChooser {
	final public static int _NColors;
	final private double _minimum, _maximum, _base;
	final public static int[] _ZeroRgb;
	final public static int[] _LowRgb;
	final public static int[] _MiddleRgb;
	final public static int[] _HighRgb;
	final private int[][] _rgbs;
	static {
		_NColors = 24;
		_ZeroRgb = ColorUtil.normalizeRgb(ColorUtil._Navy);
		_LowRgb = ColorUtil.normalizeRgb(ColorUtil._Blue);
		_MiddleRgb = ColorUtil.normalizeRgb(ColorUtil._Yellow);
		_HighRgb = ColorUtil.normalizeRgb(ColorUtil._Red);
	}

	public ColorChooser(final double otherDoubleInput, final double maximum, final double brightValue,
			final boolean useExponentialScale) {
		if (useExponentialScale) {
			_maximum = maximum;
			_minimum = Double.NaN;
			/**
			 * We want b **((nC - 1)/2) = average/maximum, which is the number of steps back
			 * from nC-1 that we want to take when we plug in average. The general formula
			 * will be k = nC - 1 - Math.floor(log(base b)(p/maximum)).
			 */
			final double average = otherDoubleInput;
			_base = Math.pow(average / maximum, 2.0 / (_NColors - 1));
		} else {
			final double min = Math.min(maximum, otherDoubleInput);
			final double max = Math.max(maximum, otherDoubleInput);
			if (max > min) {
				_minimum = min;
				_maximum = max;
			} else if (max == min) {
				if (max == 0d) {
					_minimum = 0d;
					_maximum = 1d;
				} else if (max > 0d) {
					_minimum = max / 2d;
					_maximum = max * 2d;
				} else {
					_minimum = max * 2d;
					_maximum = max / 2d;
				}
			} else {
				/** For Nan's. */
				_minimum = 0d;
				_maximum = 1d;
			}
			_base = Double.NaN;
		}
		_rgbs = computeRgbs();
	}

	static private int[][] computeRgbs() {
		final int[][] rgbs = new int[_NColors][];
		if (_NColors < 5) {
			switch (_NColors) {
			case 1:
				rgbs[0] = _MiddleRgb;
				break;
			case 2:
				rgbs[0] = _ZeroRgb;
				rgbs[1] = _HighRgb;
				break;
			case 3:
				rgbs[0] = _ZeroRgb;
				rgbs[1] = _LowRgb;
				rgbs[2] = _HighRgb;
			case 4:
				rgbs[0] = _ZeroRgb;
				rgbs[1] = _LowRgb;
				rgbs[2] = _MiddleRgb;
				rgbs[3] = _HighRgb;
				break;
			default:
			}
			return rgbs;
		}
		rgbs[0] = _ZeroRgb;
		rgbs[1] = _LowRgb;
		rgbs[_NColors - 1] = _HighRgb;
		final int nLtM = (_NColors - 4) / 2;
		final int nMtH = _NColors - 4 - nLtM;
		final int middleK = 2 + nLtM;
		rgbs[middleK] = _MiddleRgb;
		final float[] lowHsb = ColorUtil.rgbToHsb(_LowRgb);
		final float[] middleHsb = ColorUtil.rgbToHsb(_MiddleRgb);
		final float lowH = lowHsb[0];
		final float middleH = middleHsb[0];
		final float lowS = lowHsb[1];
		final float middleS = middleHsb[1];
		final float lowB = lowHsb[2];
		final float middleB = middleHsb[2];
		final double lowToMiddle = NumericalRoutines.generalMod(middleH - lowH, 1d);
		final float[] highHsb = ColorUtil.rgbToHsb(_HighRgb);
		final float highH = highHsb[0];
		final double lowToHigh = NumericalRoutines.generalMod(highH - lowH, 1d);
		final boolean goForward = lowToMiddle <= lowToHigh;
		final double hGapLtM = lowToMiddle - (goForward ? 0d : 1d);
		final double sGapLtM = middleS - lowS;
		final double bGapLtM = middleB - lowB;
		final double denLtM = nMtH + 1d;
		for (int k = 1; k <= nLtM; ++k) {
			final float h = (float) NumericalRoutines.generalMod(lowH + (k * hGapLtM) / denLtM, 1d);
			final float s = (float) (lowS + (k * sGapLtM) / denLtM);
			final float b = (float) (lowB + (k * bGapLtM) / denLtM);
			rgbs[k + 1] = ColorUtil.hsbToRgb(new float[] { h, s, b });
		}
		final double middleToHigh = NumericalRoutines.generalMod(highH - middleH, 1d);
		final double hGapMtH = middleToHigh - (goForward ? 0d : 1d);
		final float highS = highHsb[1];
		final float highB = highHsb[2];
		final double sGapMtH = highS - middleS;
		final double bGapMtH = highB - middleB;
		final double denMtH = nMtH + 1d;
		for (int k = 1; k <= nMtH; ++k) {
			final float h = (float) NumericalRoutines.generalMod(middleH + (k * hGapMtH) / denMtH, 1d);
			final float s = (float) (middleS + (k * sGapMtH) / denMtH);
			final float b = (float) (middleB + (k * bGapMtH) / denMtH);
			rgbs[middleK + k] = ColorUtil.hsbToRgb(new float[] { h, s, b });
		}
		return rgbs;
	}

	public int[] getRgb(final double p, final int minIndex, final int maxIndex) {
		int index0;
		if (p == 0d) {
			index0 = 0;
		} else if (!Double.isNaN(_base)) {
			final int nStepsBack = (int) Math.floor(Math.log(p / _maximum) / Math.log(_base));
			final int k = _NColors - 1 - nStepsBack;
			index0 = Math.max(1, Math.min(k, _NColors - 1));
		} else {
			/** Use linear. */
			if (p >= _maximum) {
				index0 = _NColors - 1;
			} else if (p < _minimum) {
				index0 = 0;
			} else {
				final double r = (p - _minimum) / (_maximum - _minimum);
				final int preIndex = (int) (r * (_NColors - 2));
				/**
				 * if _NColors is 5, preIndex should be 0, 1, ... up to 2 since r < 1. We want
				 * it to go from 1 to 3.
				 */
				index0 = Math.min(preIndex + 1, _NColors - 2);
			}
		}
		final int index = Math.max(minIndex, Math.min(index0, maxIndex));
		return _rgbs[index];
	}

	public int[] getColorIntArrayFromIndex(final int i) {
		return _rgbs[i];
	}

	public double getUpperBound(final int i) {
		if (!Double.isNaN(_base))
			return _maximum * Math.pow(_base, _NColors - 1 - i);
		final double delta = (_maximum - _minimum) / (_NColors - 2);
		if (i == _NColors - 1)
			return _maximum + delta;
		else if (i == 0)
			return _minimum;
		else
			return i * delta;
	}
}
