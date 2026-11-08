package com.skagit.utils.randomx;

import com.skagit.utils.Constants0;

public class WeightedPairDataAcc {
	public static int _NInRawData = 6;

	/** "0-th" order. */
	private double _totalWeight;

	/** First Order. */
	private double _sumU;
	private double _sumV;

	/** Second order: */
	private double _sumUu;
	private double _sumUv;
	private double _sumVv;

	/** Computed; wiped out whenever we add more data. */
	private double[][] _var;
	private double[] _mean;
	private double _sigmaU, _sigmaV, _rho, _k0, _k1;

	public WeightedPairDataAcc() {
		_totalWeight = 0d;
		_sumU = _sumV = _sumUu = _sumUv = _sumVv = 0d;
		_var = null;
		_mean = null;
	}

	public double[] exportRawData() {
		return new double[] { _totalWeight, _sumU, _sumV, _sumUu, _sumUv, _sumVv };
	}

	public float[] exportRawDataF() {
		final double[] rawData = exportRawData();
		final float[] rawDataF = new float[rawData.length];
		for (int k = 0; k < rawData.length; ++k) {
			rawDataF[k] = (float) rawData[k];
		}
		return rawDataF;
	}

	public WeightedPairDataAcc(final double[] rawData) {
		_totalWeight = rawData[0];
		_sumU = rawData[1];
		_sumV = rawData[2];
		_sumUu = rawData[3];
		_sumUv = rawData[4];
		_sumVv = rawData[5];
		_var = null;
		_mean = null;
	}

	public static WeightedPairDataAcc getWeightedPairDataAccFromFloats(final float[] rawDataF) {
		final double[] rawData = new double[rawDataF.length];
		for (int k = 0; k < rawDataF.length; ++k) {
			rawData[k] = rawDataF[k];
		}
		return new WeightedPairDataAcc(rawData);
	}

	public void add(final double u, final double v, final double weight) {
		_sumU += weight * u;
		_sumV += weight * v;
		_sumUu += weight * u * u;
		_sumUv += weight * u * v;
		_sumVv += weight * v * v;
		_totalWeight += weight;
		_var = null;
		_mean = null;
	}

	public double getTotalWeight() {
		return _totalWeight;
	}

	public double[][] getVar() {
		if (_var != null && _mean != null) {
			return _var;
		}
		synchronized (this) {
			if (_var != null && _mean != null) {
				return _var;
			}
			if (_totalWeight == 0d) {
				_mean = new double[] { Double.NaN, Double.NaN };
				_var = new double[][] { new double[] { Double.NaN, Double.NaN },
						new double[] { Double.NaN, Double.NaN } };
			} else {
				_mean = new double[] { _sumU / _totalWeight, _sumV / _totalWeight };
				final double uBar = _mean[0];
				final double vBar = _mean[1];
				final double uu = (_sumUu - _totalWeight * uBar * uBar) / _totalWeight;
				final double vv = (_sumVv - _totalWeight * vBar * vBar) / _totalWeight;
				final double uv = (_sumUv - _totalWeight * uBar * vBar) / _totalWeight;
				_var = new double[][] { new double[] { uu, uv }, new double[] { uv, vv } };
				_sigmaU = Math.sqrt(uu);
				_sigmaV = Math.sqrt(vv);
				final double sigmaUv = _sigmaU * _sigmaV;
				_rho = uv / sigmaUv;
				final double rhoPrime = 1d - _rho * _rho;
				_k0 = 1d / (Constants0._2Pi * sigmaUv * Math.sqrt(rhoPrime));
				_k1 = -1d / (2d * rhoPrime);
			}
		}
		return _var;
	}

	public double[] getMean() {
		/** Following line sets _mean and _var. */
		getVar();
		return _mean;
	}

	/**
	 * <pre>
	 * https://www.probabilitycourse.com/chapter5/5_3_2_bivariate_normal_dist.php
	 * </pre>
	 */
	public double getDensity(final double u, final double v) {
		getVar();
		final double uPrime = (u - _mean[0]) / _sigmaU;
		final double vPrime = (v - _mean[1]) / _sigmaV;
		return _k0 * Math.exp( //
				_k1 * (uPrime * uPrime + vPrime * vPrime - 2d * _rho * uPrime * vPrime));
	}

}
