package com.skagit.utils.math;

import java.util.Arrays;

import com.skagit.utils.NumericalRoutines;
import com.skagit.utils.myLogger.MyLogger;
import com.skagit.utils.randomx.Randomx;

import jsc.distributions.Normal;
import jsc.distributions.Uniform;
import jsc.goodnessfit.KolmogorovTest;

public class KsStatistics {
	final public boolean _uniform;
	final public int _n;
	final public double _d;
	final public double _tailProb;
	final public double _positiveD;
	final public double _negativeD;
	final public double _testStatistic;
	final public double _sp;
	final public double _xOfD;
	final public double _xOfPositiveD;
	final public double _xOfNegativeD;

	public KsStatistics(final double[] data, final boolean uniform) {
		_uniform = uniform;
		final double[] dataClone;
		if (uniform) {
			dataClone = data;
		} else {
			dataClone = Randomx.statisticallyStandardize(data);
		}
		final KolmogorovTest kolmogorovTest = new KolmogorovTest(dataClone, uniform ? new Uniform() : new Normal());
		_n = kolmogorovTest.getN();
		_d = kolmogorovTest.getD();
		_tailProb = KolmogorovTest.exactUpperTailProb(_n, _d);
		_positiveD = kolmogorovTest.getPositiveD();
		_negativeD = kolmogorovTest.getNegativeD();
		_testStatistic = kolmogorovTest.getTestStatistic();
		_sp = kolmogorovTest.getSP();
		_xOfD = kolmogorovTest.xOfD();
		_xOfPositiveD = kolmogorovTest.xOfPositiveD();
		_xOfNegativeD = kolmogorovTest.xOfNegativeD();
	}

	/**
	 * The following does just a manual computation and hence returns a simple
	 * double array.
	 */
	private static double[] getKsTypeStatistics1(final double[] data, final boolean uniform) {
		final int n = data.length;
		final double[] dataClone;
		if (uniform) {
			dataClone = data.clone();
		} else {
			dataClone = Randomx.statisticallyStandardize(data);
		}
		Arrays.sort(dataClone);
		if (!uniform) {
			for (int k = 0; k < n; ++k) {
				dataClone[k] = Randomx._NormalDistribution.cumulativeProbability(dataClone[k]);
			}
		}
		double ks = 0;
		double xOfD = -1;
		for (int k = 0; k < n; ++k) {
			final double emp = ((double) k) / n;
			final double theo = dataClone[k];
			final double thisKs = Math.abs(emp - theo);
			if (thisKs > ks) {
				ks = thisKs;
				xOfD = k;
			}
		}
		final double upperTail = KolmogorovTest.exactUpperTailProb(n, ks);
		return new double[] { ks, upperTail, xOfD };
	}

	public String getString() {
		return String.format(
				"TWO_BY_THREE[%d] D[%.3f] +D[%.3f] -D[%.3f], TestStat[%.3f] "
						+ "SP[%.3f] XOfD[%.3f] XOf+D[%.3f] XOf-D[%.3f]", //
				_n, _d, _positiveD, _negativeD, _testStatistic, _sp, _xOfD, _xOfPositiveD, _xOfNegativeD);
	}

	@Override
	public String toString() {
		return getString();
	}

	public static void main(final String[] args) {
		for (int n = 5; n < 30; n += 5) {
			final double[] data = new double[n];
			for (int k = 0; k < n; ++k) {
				data[k] = ((double) k) / n;
			}
			final boolean uniform = false;
			final double[] d1 = getKsTypeStatistics1(data, uniform);
			final KsStatistics ksStatistics = new KsStatistics(data, uniform);
			System.out.println(NumericalRoutines.getString(data) + "\n" + NumericalRoutines.getString(d1) + "\n"
					+ ksStatistics.getString());
			System.exit(41);
		}
		final Randomx r = new Randomx(/* useCurrentTimeMs= */false);
		r.nextBytes(null);
		for (int k = 0; k < 100; ++k) {
			final long longDraw = r.nextLong();
			MyLogger.out(/* logger= */null, String.format("k[%d] draw[%d]", k, longDraw));
		}
		System.exit(34);
		for (int k = 0; k < 100; ++k) {
			final double w = r.getWeibullDraw(1.0, 1.5);
			MyLogger.out(/* logger= */null, String.format("%f", w));
		}
		System.exit(35);
		final int nCells = 816, nTrialsPerRun = 10000, nRuns = 100;
		final int high = 18;
		double total = 0.0, totalSquared = 0.0;
		for (int iRun = 0; iRun < nRuns; ++iRun) {
			final int[] cellCounts = new int[nCells];
			for (int trial = 0; trial < nTrialsPerRun; ++trial) {
				++cellCounts[r.nextInt(nCells)];
			}
			int nHigh = 0;
			for (final int cellCount : cellCounts) {
				if (cellCount >= high) {
					++nHigh;
				}
			}
			total += nHigh;
			totalSquared += nHigh * nHigh;
		}
		final double nHighBar = total / nRuns;
		final double nHighVariance = (totalSquared - nRuns * nHighBar * nHighBar) / nRuns;
		Math.sqrt(nHighVariance);
		Math.sqrt(nHighVariance);
	}
}
/**
 * <pre>
 * http://www.eridlc.com/onlinetextbook/appendix/table7.htm SAMPLE SIZE
 * (TWO_BY_THREE) LEVEL OF SIGNIFICANCE FOR D = MAXIMUM [ F0(X) - Sn(X) ] 0.20,
 * 0.15, 0.10, 0.05, 0.01 1 0.900, 0.925, 0.950, 0.975, 0.995 2 0.684, 0.726,
 * 0.776, 0.842, 0.929 3 0.565, 0.597, 0.642, 0.708, 0.828 4 0.494, 0.525,
 * 0.564, 0.624, 0.733 5 0.446, 0.474, 0.510, 0.565, 0.669 6 0.410, 0.436,
 * 0.470, 0.521, 0.618 7 0.381, 0.405, 0.438, 0.486, 0.577 8 0.358, 0.381,
 * 0.411, 0.457, 0.543 9 0.339, 0.360, 0.388, 0.432, 0.514 10 0.322, 0.342,
 * 0.368, 0.410, 0.490 11 0.307, 0.326, 0.352, 0.391, 0.468 12 0.295, 0.313,
 * 0.338, 0.375, 0.450 13 0.284, 0.302, 0.325, 0.361, 0.433 14 0.274, 0.292,
 * 0.314, 0.349, 0.418 15 0.266, 0.283, 0.304, 0.338, 0.404 16 0.258, 0.274,
 * 0.295, 0.328, 0.392 17 0.250, 0.266, 0.286, 0.318, 0.381 18 0.244, 0.259,
 * 0.278, 0.309, 0.371 19 0.237, 0.252, 0.272, 0.301, 0.363 20 0.231, 0.246,
 * 0.264, 0.294, 0.356 25 0.210, 0.220, 0.240, 0.270, 0.320 30 0.190, 0.200,
 * 0.220, 0.240, 0.290 35 0.180, 0.190, 0.210, 0.230, 0.270 OVER 35
 * 1.07/sqrt(TWO_BY_THREE), 1.14/sqrt(TWO_BY_THREE), 1.22/sqrt(TWO_BY_THREE),
 * 1.36/sqrt(TWO_BY_THREE), 1.63/sqrt(TWO_BY_THREE)
 */
