package com.skagit.utils.randomx;

import com.skagit.utils.CombinatoricTools;
import com.skagit.utils.PermutationTools;

public class StratifiedSample {
    final public double[][] _outWeights;
    /** The weights of each strata. */
    final public double[] _stratumWeights;
    final public boolean[] _useAll;
    final public boolean[] _withReplacement;
    final private int[][] _chosen;
    final private double[][] _weightsOfChosen;

    /** Do a stratified sample. */
    public StratifiedSample(final double[][] inWeights, final int targetN, final Randomx r) {
	/**
	 * Normalize the weights into _outWeights and compute _stratumWeights.
	 */
	final int nStrata = inWeights.length;
	_outWeights = new double[nStrata][];
	_stratumWeights = new double[nStrata];
	double totalWeight = 0;
	for (int iPass = 0; iPass < 2; ++iPass) {
	    for (int k1 = 0; k1 < nStrata; ++k1) {
		final double[] inStratum = inWeights[k1];
		final int nInStratum = inStratum.length;
		if (iPass == 0) {
		    _stratumWeights[k1] = 0;
		}
		if (iPass == 1) {
		    _outWeights[k1] = new double[nInStratum];
		}
		for (int k2 = 0; k2 < nInStratum; ++k2) {
		    final double inWeight = inWeights[k1][k2];
		    if (iPass == 0) {
			_stratumWeights[k1] += inWeight;
			totalWeight += inWeight;
		    } else {
			_outWeights[k1][k2] = inWeight / totalWeight;
		    }
		}
		if (iPass == 1) {
		    _stratumWeights[k1] /= totalWeight;
		}
	    }
	}
	/** Compute _sampleSizes, and _useAll. */
	final int[] sampleSizes = new int[nStrata];
	_useAll = new boolean[nStrata];
	CombinatoricTools.constantFill(_useAll, false);
	_withReplacement = new boolean[nStrata];
	CombinatoricTools.constantFill(_withReplacement, false);
	for (int k1 = 0; k1 < nStrata; ++k1) {
	    final double ideal = targetN * _stratumWeights[k1];
	    sampleSizes[k1] = (int) Math.ceil(ideal);
	    final int nInStratum = _outWeights[k1].length;
	    if (nInStratum <= sampleSizes[k1]) {
		sampleSizes[k1] = nInStratum;
		_useAll[k1] = true;
	    }
	}
	/** Sample each stratum. */
	_chosen = new int[nStrata][];
	_weightsOfChosen = new double[nStrata][];
	for (int k1 = 0; k1 < nStrata; ++k1) {
	    final double[] stratum = _outWeights[k1];
	    final int nInStratum = stratum.length;
	    final int sampleSize = sampleSizes[k1];
	    final int[] chosen = _chosen[k1] = new int[sampleSize];
	    final double[] weights = _weightsOfChosen[k1] = new double[sampleSize];
	    if (_useAll[k1]) {
		for (int k2 = 0; k2 < sampleSize; ++k2) {
		    chosen[k2] = k2;
		    weights[k2] = _outWeights[k1][k2];
		}
	    } else {
		/**
		 * Because _useAll[k1] is false, sampleSize < nInStratum. If the weights don't
		 * vary by more than a threshold%, sample without replacement.
		 */
		double minWeight = Double.MAX_VALUE;
		double maxWeight = 0.0;
		for (int k2 = 0; k2 < nInStratum; ++k2) {
		    final double weight = stratum[k2];
		    minWeight = Math.min(minWeight, weight);
		    maxWeight = Math.max(maxWeight, weight);
		    if (maxWeight > minWeight) {
			_withReplacement[k1] = true;
			break;
		    }
		}
		if (_withReplacement[k1]) {
		    final double[] table = new double[nInStratum];
		    table[0] = stratum[0];
		    for (int k2 = 1; k2 < nInStratum; ++k2) {
			table[k2] = table[k2 - 1] + stratum[k2];
		    }
		    for (int k2 = 0; k2 < sampleSize; ++k2) {
			chosen[k2] = r.getDrawFromTable(table);
		    }
		} else {
		    final int[] randomKOfN = PermutationTools.randomKofN(sampleSize, nInStratum, r);
		    for (int k2 = 0; k2 < sampleSize; ++k2) {
			chosen[k2] = randomKOfN[k2];
		    }
		}
		/** With or without replacement; the weights are 1.0. */
		for (int k2 = 0; k2 < sampleSize; ++k2) {
		    weights[k2] = 1.0;
		}
	    }
	    /**
	     * Scale the weights within stratum k1 so that the sum is _stratumWeights[k1].
	     */
	    double auxiliary = Double.NaN;
	    for (int iPass = 0; iPass < 2; ++iPass) {
		if (iPass == 0) {
		    /**
		     * During iPass == 0, auxiliary is used to accumulate the weights.
		     */
		    auxiliary = 0;
		} else {
		    /** In the second pass, auxiliary is the scale factor. */
		    auxiliary = _stratumWeights[k1] / auxiliary;
		}
		for (int k2 = 0; k2 < sampleSize; ++k2) {
		    if (iPass == 0) {
			auxiliary += weights[k2];
		    } else {
			weights[k2] *= auxiliary;
		    }
		}
	    }
	}
	// MyLogger.info(null, getString());
    }

    public int getTotalN() {
	int totalN = 0;
	final int nStrata = _chosen.length;
	for (int k1 = 0; k1 < nStrata; ++k1) {
	    totalN += _chosen[k1].length;
	}
	return totalN;
    }

    public int[][] getChosen() {
	return _chosen;
    }

    public double[][] getWeightsOfChosen() {
	return _weightsOfChosen;
    }

    /**
     * Retrieves which stratum, which entry within the stratum, and the resulting
     * weight of this selection.
     */
    public double[] getStratumEntryWeightOfChosen(final int k) {
	int n = 0;
	for (int k1 = 0; k1 < _chosen.length; ++k1) {
	    final int nInStratum = _chosen[k1].length;
	    n += nInStratum;
	    if (n > k) {
		n -= nInStratum;
		final int kStratum = k1;
		final int kEntry = k - n;
		/**
		 * kEntry is the index of the output sample. entry is the index from the input
		 * list of weights.
		 */
		final int entry = _chosen[kStratum][kEntry];
		/** weight is the weight attached to this sample point. */
		final double weight = _weightsOfChosen[kStratum][kEntry];
		return new double[] { kStratum, entry, weight };
	    }
	}
	return null;
    }

    private String getString() {
	String s = null;
	final int nStrata = _stratumWeights.length;
	final int[] maxLengths = new int[nStrata];
	CombinatoricTools.constantFill(maxLengths, 0);
	for (int iPass = 0; iPass < 2; ++iPass) {
	    s = iPass == 0 ? null : "";
	    for (int k1 = 0; k1 < nStrata; ++k1) {
		final double stratumWeight = _stratumWeights[k1];
		final int n = _chosen[k1].length;
		final String s1 = String.format("Wt/n[%.5f/%d]", stratumWeight, n);
		final int s1Length = s1.length();
		if (iPass == 0) {
		    maxLengths[k1] = Math.max(maxLengths[k1], s1Length);
		} else {
		    final int fieldLength = (k1 == 0 ? 0 : 1) + maxLengths[k1];
		    s += centerPad(fieldLength, s1);
		}
	    }
	    if (iPass == 1) {
		s += "\n";
	    }
	    for (int k1 = 0; k1 < nStrata; ++k1) {
		final boolean withReplacement = _withReplacement[k1];
		final boolean useAll = _useAll[k1];
		final String s1 = String.format("Repl/UseAll[%b/%b]", withReplacement, useAll);
		final int s1Length = s1.length();
		if (iPass == 0) {
		    maxLengths[k1] = Math.max(maxLengths[k1], s1Length);
		} else {
		    s += centerPad(1 + maxLengths[k1], s1);
		}
	    }
	    if (iPass == 1) {
		s += "\n";
	    }
	    for (int k2 = 0;; ++k2) {
		boolean haveOne = false;
		for (int k1 = 0; k1 < nStrata; ++k1) {
		    final int[] chosen = _chosen[k1];
		    final double[] theseWeights = _weightsOfChosen[k1];
		    final int nInThisStratum = chosen.length;
		    final String s2;
		    if (k2 > nInThisStratum - 1) {
			s2 = "";
		    } else {
			s2 = String.format(" %d,%.5f", chosen[k2], theseWeights[k2]);
			haveOne = true;
		    }
		    final int s2Length = s2.length();
		    if (iPass == 0) {
			maxLengths[k1] = Math.max(maxLengths[k1], s2Length);
		    } else {
			final int fieldLength = (k1 == 0 ? 0 : 1) + maxLengths[k1];
			s += centerPad(fieldLength, s2);
		    }
		}
		if (!haveOne) {
		    break;
		}
		if (iPass == 1) {
		    s += "\n";
		}
	    }
	}
	return s + "\n";
    }

    private static String centerPad(final int fieldLength, final String s) {
	final int sLen = s.length();
	final int nInPad = fieldLength - sLen;
	if (nInPad <= 0) {
	    return s;
	}
	final int nInLeftPad = (nInPad + 1) / 2;
	final int nInRightPad = nInPad - nInLeftPad;
	final char[] leftPadChars = new char[nInLeftPad];
	final char[] rightPadChars = new char[nInRightPad];
	CombinatoricTools.constantFill(leftPadChars, ' ');
	CombinatoricTools.constantFill(rightPadChars, ' ');
	final String leftPad = new String(leftPadChars);
	final String rightPad = new String(rightPadChars);
	final String returnValue = leftPad + s + rightPad;
	return returnValue;
    }

    public static void main(final String[] args) {
	final int targetN = 30;
	final int[] n = new int[] { 10, 20, 30, 30 };
	final int nStrata = n.length;
	final double[][] inWeights = new double[nStrata][];
	final Randomx r = new Randomx(/* useCurrentTimeMs= */false);
	for (int k1 = 0; k1 < nStrata; ++k1) {
	    final int thisN = n[k1];
	    final double[] array = inWeights[k1] = new double[thisN];
	    for (int k2 = 0; k2 < thisN; ++k2) {
		array[k2] = r.nextDouble();
		/** Make the last stratum constant. */
		if (k1 == nStrata - 1) {
		    array[k2] = 0.5;
		}
	    }
	}
	final StratifiedSample stratifiedSample = new StratifiedSample(inWeights, targetN, r);
	System.out.println(stratifiedSample.getString());
	final int totalN = stratifiedSample.getTotalN();
	for (int k = 0; k < totalN; ++k) {
	    final double[] trio = stratifiedSample.getStratumEntryWeightOfChosen(k);
	    final double stratum = trio[0];
	    final double entry = trio[1];
	    final double weight = trio[2];
	    System.out.printf("\n%d %f %f %f", k, stratum, entry, weight);
	}
    }
}
