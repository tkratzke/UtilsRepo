package com.skagit.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Stack;

public class PermutationTools {

	private static int[] passThroughGetInitial(final int n) {
		return CombinatoricTools.getInitial(n);
	}

	private static int findPriorToTail(final int[] letters) {
		int preTail = letters.length - 2;
		for (; preTail >= 0 && letters[preTail] > letters[preTail + 1]; --preTail) {
		}
		return preTail;
	}

	/**
	 * This method changes a subset of {0..m-1} with at least nLow elements and at
	 * most subset.length elements to the next one. It loops back to the first one.
	 * Upon return, if subset[k] = m, the subset ends before the kth element.
	 */
	public static void next(final int nLow, final int m, final int[] subset) {
		final int nHigh = subset.length;
		for (int i = nHigh - 1; i >= 0; --i) {
			if (++subset[i] <= (i < nLow ? m + i - nLow : m)) {
				for (int j = i + 1; j < subset.length; ++j) {
					subset[j] = subset[j - 1] == m ? m : subset[j - 1] + 1;
				}
				return;
			}
		}
		/** Need the first subset. */
		for (int i = 0; i < nHigh; ++i) {
			subset[i] = i;
		}
	}

	/**
	 * Changes subset to the next lexicographically ordered set and returns the
	 * number in that set. To use this with 3 elements, set subset to {3, 3, 3} and
	 * iterate until the return value is 0.
	 */
	public static int lexicographicalNextSet(final int[] subset) {
		final int m = subset.length;
		if (m == 0)
			return 0;
		if (subset[0] == m) {
			/** This is an indication that we want the first set. */
			subset[0] = 0;
			for (int k = 1; k < m; ++k) {
				subset[k] = m;
			}
			return 1;
		}
		for (int k = m - 1;; --k) {
			if (subset[k] == m) {
				continue;
			}
			if (subset[k] == m - 1) {
				/**
				 * Bump the previous one if you can; it cannot be m-1 since subset[k] == m-1.
				 */
				subset[k] = m;
				if (k > 0) {
					++subset[k - 1];
				}
				return k;
			} /**
				 * subset[k] < m - 1. The only value that will ever be in the (m-1)-st element
				 * is m-1. Since subset[k] < m-1, we are not on the (m-1)-st element and we can
				 * safely refer to subset[k+1].
				 */
			subset[k + 1] = subset[k] + 1;
			return k + 2;
		}
	}

	/**
	 * This method changes a subset of {0..m-1} that contains fixed, into the next
	 * subset that also contains fixed.
	 */
	public static void nextExtension(final boolean[] fixed, final boolean[] subset) {
		final int m = subset.length;
		for (int i = m - 1; i >= 0; --i) {
			if (fixed != null && fixed[i]) {
				continue;
			}
			subset[i] = !subset[i];
			if (subset[i])
				return;
		}
	}

	public static boolean nextExtension(final BitSet fixed, final BitSet extension, final int n) {
		for (int i = fixed.nextClearBit(0);; i = fixed.nextClearBit(i + 1)) {
			if (i == n)
				return false;
			extension.flip(i);
			if (extension.get(i))
				return true;
		}
	}

	public static int[] randomPermutation(final int n, final Random r) {
		final int[] perm = passThroughGetInitial(n);
		permute(perm, r);
		return perm;
	}

	public static void permute(final int[] letters, final Random r) {
		final int n = letters.length;
		for (int i = 0; i < n; ++i) {
			final int j = r.nextInt(n - i);
			final int letterI = letters[i];
			letters[i] = letters[i + j];
			letters[i + j] = letterI;
		}
	}

	public static <T extends Object> void permute(final T[] tArray, final Random r) {
		final int n = tArray.length;
		for (int i = 0; i < n; ++i) {
			final int j = r.nextInt(n - i);
			final T t = tArray[i];
			tArray[i] = tArray[i + j];
			tArray[i + j] = t;
		}
	}

	/** Randomly select k of {0,...,n-1}. */
	public static int[] randomKofN(final int k, final int n, final Random r) {
		final int[] perm = passThroughGetInitial(n);
		if (k >= n)
			return perm;
		final int[] answer = new int[k];
		final int nDraws = Math.min(k, n - k);
		/** Put random ones into permutation. */
		for (int i = 0; i < nDraws; ++i) {
			/** Randomly pick one that hasn't been chosen. */
			final int j = i + r.nextInt(n - i);
			final int permI = perm[i];
			perm[i] = perm[j];
			perm[j] = permI;
		}
		/**
		 * Load up the answer from permutation, either with the front (if we made k
		 * draws) or the back (if we made n-k draws).
		 */
		final int offset = (k == nDraws) ? 0 : nDraws;
		for (int i = 0; i < k; ++i) {
			answer[i] = perm[offset + i];
		}
		return answer;
	}

	public static BitSet randomKofNBitSet(final int k, final int n, final Random r) {
		final int[] intArray = PermutationTools.randomKofN(k, n, r);
		final BitSet bitSet = new BitSet();
		for (final int element : intArray) {
			bitSet.set(element);
		}
		return bitSet;
	}

	/** Cyclically steps through the different orderings of letters. */
	public static void next(final int[] letters) {
		final int length = letters.length;
		/**
		 * Everything after i is called the "tail." Exchange i with the last letter in
		 * the tail that is as big as i. Then "pancake" the tail.
		 */
		final int i = findPriorToTail(letters);
		/**
		 * Unless the tail is the entire array, set ii to the position within the tail
		 * that i will swap with, and swap i and ii. If the tail is the entire array,
		 * then i is -1, and we don't want to swap i with anything.
		 */
		if (i >= 0) {
			int ii = length - 1;
			for (final int p = letters[i]; letters[ii] < p; --ii) {
			}
			final int letterI = letters[i];
			letters[i] = letters[ii];
			letters[ii] = letterI;
		}
		/** Pancake the tail. */
		final int nSwaps = (length - i - 1) / 2;
		for (int k = 0; k < nSwaps; ++k) {
			final int a = i + 1 + k, b = length - 1 - k;
			final int letterA = letters[a];
			letters[a] = letters[b];
			letters[b] = letterA;
		}
	}

	public static boolean next(final int[] set, final int n) {
		/**
		 * <pre>
		 * Returns the next subset of size k, from 0,1,...,n-1, where k =
		 * set.length.
		 * Example: n = 4 (here k = 2). Then:
		 * set = [0,1] goes to set = [0,2], return true.
		 * set = [0,3] goes to set = [1,2], return true.
		 * set = [2,3] goes to set = [2,3], return false.
		 * </pre>
		 */
		final int k = set.length;
		for (int i1 = k - 1; i1 >= 0; --i1) {
			final int value = set[i1];
			final int nBigBrothers = k - 1 - i1;
			if (value + 1 + nBigBrothers >= n) {
				continue;
			}
			++set[i1];
			for (int i2 = i1 + 1; i2 < k; ++i2) {
				set[i2] = set[i2 - 1] + 1;
			}
			return true;
		}
		return false;
	}

	/** Returns up to nBins "high dividing lines" and the count in each one. */
	public static double[][] getBins(final double[] data, int nBins) {
		final ArrayList<double[]> candidates = new ArrayList<>();
		final double[] dataClone = data.clone();
		Arrays.sort(dataClone);
		final int dataLength = data.length;
		for (int j = 1; j < dataLength; ++j) {
			if (dataClone[j] > dataClone[j - 1]) {
				candidates.add(new double[] { (dataClone[j] + dataClone[j - 1]) / 2.0, j });
			}
		}
		final int nCandidates = candidates.size();
		final double[][] candidates2 = candidates.toArray(new double[nCandidates][]);
		nBins = Math.min(nBins, nCandidates + 1);
		final double fairShare = ((double) dataLength) / nBins;
		/** Must pick nBins-1 candidates from the candidates. */
		final int[] set = passThroughGetInitial(nBins - 1);
		int[] winner = null;
		double score = Double.MAX_VALUE;
		double maxSum = Double.MAX_VALUE;
		do {
			int nUsed = 0;
			double maxDeviation = 0;
			double sum = 0;
			for (final int element : set) {
				final double[] candidate = candidates2[element];
				final int nMine = (int) (candidate[1] - nUsed);
				final double deviation = Math.abs(nMine - fairShare);
				if (deviation > maxDeviation) {
					maxDeviation = deviation;
				}
				sum += deviation;
				nUsed += nMine;
			}
			final int nMine = (dataLength - nUsed);
			final double deviation = Math.abs(nMine - fairShare);
			if (deviation > maxDeviation) {
				maxDeviation = deviation;
			}
			sum += deviation;
			nUsed += nMine;
			if (maxDeviation < score || (maxDeviation == score && sum < maxSum)) {
				score = maxDeviation;
				maxSum = sum;
				winner = set.clone();
			}
		} while (next(set, nCandidates));
		final double[][] returnValue = new double[nBins][2];
		int nUsed = 0;
		for (int k = 0; k < nBins - 1; ++k) {
			returnValue[k] = candidates2[winner[k]].clone();
			final int nMine = (int) (returnValue[k][1] - nUsed);
			returnValue[k][1] = nMine;
			nUsed += nMine;
		}
		final double highPoint = Integer.MAX_VALUE;
		final int nMine = dataLength - nUsed;
		returnValue[nBins - 1] = new double[] { highPoint, nMine };
		return returnValue;
	}

	public static class BinMaker {
		final public static int _MinInBinPosition = 0;
		final public static int _MaxNBinsPosition = 1;
		final public static int _NIntParams = 2;
		final public static int _MaxForNoGreed = 25;
		final private double[] _data;
		final private double _minLegalGap;
		final private int[] _intParams;
		/** Outputs: */
		public double[] _highEndpoints;
		public double[] _gaps;
		public int[] _binCounts;
		/** Convenient output for printing everything. */
		final public double[][] _binsInfo;

		public BinMaker(final double[] data, final int[] intParams) {
			_data = data.clone();
			_intParams = intParams.clone();
			Arrays.sort(_data);
			final int nData = data.length;
			int nDistinctData = 0;
			double oldD = Double.NEGATIVE_INFINITY;
			for (int k = 0; k < nData; ++k) {
				final double d = _data[k];
				if (d != oldD) {
					++nDistinctData;
					oldD = d;
				}
			}
			/**
			 * minInBin must be at least 1 and no more than nDistinctData. "1" means that we
			 * allow all-singleton-bins and nDistinctData means we force a single bin.
			 */
			final int minInBin = Math.max(1, Math.min(nData, intParams[_MinInBinPosition]));
			/**
			 * maxNBins must be at least 1 and no more than nDistinctData. "1" means that we
			 * force a single bin and nDistinctData means we allow all-singleton-bins.
			 */
			final int maxNBins = Math.max(1, Math.min(nData, intParams[_MaxNBinsPosition]));
			_intParams[_MaxNBinsPosition] = maxNBins;
			/**
			 * Compute minLegalGap based only on those numbers that are within 2s of the
			 * mean.
			 */
			double sum = 0;
			double sum2 = 0;
			for (int k = 0; k < nData; ++k) {
				final double d = _data[k];
				sum += d;
				sum2 += d * d;
			}
			final double xBar = sum / nData;
			final double s = Math.sqrt((sum2 - nData * xBar * xBar) / (nData - 1));
			final double lowThreshold = xBar - 2 * s;
			final double highThreshold = xBar + 2 * s;
			double lowNormal = Double.MAX_VALUE;
			double highNormal = Double.NEGATIVE_INFINITY;
			int nNormal = 0;
			for (final double d : _data) {
				if (lowThreshold <= d && d <= highThreshold) {
					++nNormal;
					lowNormal = Math.min(lowNormal, d);
					highNormal = Math.max(highNormal, d);
				}
			}
			final double minLegalGap2 = (highNormal - lowNormal) / (nNormal - 1);
			final double minLegalGap = (_data[nData - 1] - _data[0]) / (nDistinctData - 1);
			_minLegalGap = Math.min(minLegalGap2, minLegalGap);
			final boolean useGreed = data.length > _MaxForNoGreed;
			if (useGreed) {
				/**
				 * Bin at every distinct point and then combine bins until surviving bins are
				 * all large enough.
				 */
				oldD = Double.NEGATIVE_INFINITY;
				final ArrayList<int[]> indexPairs = new ArrayList<>();
				int[] indexPair = null;
				for (int k = 0; k < nData; ++k) {
					final double d = _data[k];
					if (d == oldD) {
						indexPair[1] = k;
					} else {
						indexPair = new int[] { k, k };
						indexPairs.add(indexPair);
						oldD = d;
					}
				}
				/** Look for the smallest gap*binWidth combination. */
				/**
				 * The first time through, restrict yourself to considering gaps that have a
				 * too-small bin. The second time through, let any bin compete for
				 * consolidation.
				 */
				for (int iPass = 0; iPass < 2; ++iPass) {
					for (;;) {
						final int nBins = indexPairs.size();
						/** If every bin has enough, we can quit. */
						final int[] binCounts = getBinCounts(indexPairs);
						int thisMinInBin = Integer.MAX_VALUE;
						for (final int binCount : binCounts) {
							thisMinInBin = Math.min(thisMinInBin, binCount);
						}
						if ((iPass == 0 && thisMinInBin >= minInBin) || (iPass == 1 && nBins <= maxNBins)) {
							break;
						}
						double minScore = Double.MAX_VALUE;
						int jWinner = -1;
						for (int j = 0; j < nBins - 1; ++j) {
							indexPair = indexPairs.get(j);
							final int[] nextIndexPair = indexPairs.get(j + 1);
							final int minCard = 1
									+ Math.min(nextIndexPair[1] - nextIndexPair[0], indexPair[1] - indexPair[0]);
							if (iPass == 1 || minCard < minInBin) {
								final double gap = _data[nextIndexPair[0]] - _data[indexPair[1]];
								final double score = gap * minCard;
								if (score < minScore) {
									minScore = score;
									jWinner = j;
								}
							}
						}
						if (jWinner >= 0) {
							indexPair = indexPairs.get(jWinner);
							final int[] nextIndexPair = indexPairs.remove(jWinner + 1);
							indexPair[1] = nextIndexPair[1];
						} else {
							break;
						}
					}
				}
				final int nBins = indexPairs.size();
				_highEndpoints = new double[nBins - 1];
				_gaps = new double[nBins - 1];
				for (int iBin = 0; iBin < nBins; ++iBin) {
					indexPair = indexPairs.get(iBin);
					if (iBin < nBins - 1) {
						final int[] nextIndexPair = indexPairs.get(iBin + 1);
						_gaps[iBin] = _data[nextIndexPair[0]] - _data[indexPair[1]];
						_highEndpoints[iBin] = (_data[indexPair[1]] + _data[nextIndexPair[0]]) / 2.0;
					}
				}
				_binCounts = getBinCounts(indexPairs);
			} else {
				/**
				 * Not using greed. No gap will be less than the minLegalGap as computed above.
				 * No bin will have fewer than 1/4 of its fair share (as per number of bins). We
				 * take the partition with the most bins and among those, the one with the
				 * biggest minInBin. This won't work for sets of size 100. The algorithm simply
				 * starts with a "no dividers" solution and puts in every possible combination
				 * of dividers.
				 */
				_highEndpoints = new double[] {};
				_gaps = new double[] {};
				_binCounts = new int[] { nData };
				/** Compute. */
				final Stack<Double> dividersStack = new Stack<>();
				recurseForBinMaker(dividersStack);
			}
			/** Record in _binsInfo. */
			final int nBins = _binCounts.length;
			_binsInfo = new double[nBins][];
			for (int iBin = 0; iBin < nBins; ++iBin) {
				_binsInfo[iBin] = new double[3];
				if (iBin < nBins - 1) {
					_binsInfo[iBin][0] = _highEndpoints[iBin];
					_binsInfo[iBin][2] = _gaps[iBin];
				} else {
					_binsInfo[iBin][0] = _binsInfo[iBin][2] = Double.NaN;
				}
				_binsInfo[iBin][1] = _binCounts[iBin];
			}
		}

		/**
		 * Finds the largest integer k for which k * multiple + offset <= w.
		 * <p>
		 * k <= (w - offset)/multiple, since multiple > 0.
		 */
		private static int getBinNumber(final double w, final double offset, final double multiple) {
			return (int) Math.floor((w - offset) / multiple);
		}

		public BinMaker(final double[] data, final double offset, final double multiple) {
			_data = data.clone();
			_intParams = new int[] { 0, Integer.MAX_VALUE };
			Arrays.sort(_data);
			_minLegalGap = Double.NaN;
			final int nData = data.length;
			/** Find the minimum value. */
			final double minD = _data[0];
			final double maxD = _data[nData - 1];
			final int bottomK = getBinNumber(minD, offset, multiple);
			final int topK = getBinNumber(maxD, offset, multiple);
			final int nBins = topK - bottomK + 1;
			_highEndpoints = new double[nBins - 1];
			_gaps = new double[nBins - 1];
			for (int iBin = 0; iBin < nBins; ++iBin) {
				if (iBin < nBins - 1) {
					_highEndpoints[iBin] = (bottomK + iBin + 1) * multiple + offset;
					_gaps[iBin] = 0;
				}
			}
			_binCounts = new int[nBins];
			for (final double d : _data) {
				final int glbIndex = NumericalRoutines.getGlbIndex(_highEndpoints, d) + 1;
				++_binCounts[Math.max(0, Math.min(glbIndex, nBins - 1))];
			}
			/** Record in _binsInfo. */
			_binsInfo = new double[nBins][];
			for (int iBin = 0; iBin < nBins; ++iBin) {
				_binsInfo[iBin] = new double[3];
				if (iBin < nBins - 1) {
					_binsInfo[iBin][0] = _highEndpoints[iBin];
					_binsInfo[iBin][2] = _gaps[iBin];
				} else {
					_binsInfo[iBin][0] = _binsInfo[iBin][2] = Double.NaN;
				}
				_binsInfo[iBin][1] = _binCounts[iBin];
			}
		}

		private static int[] getBinCounts(final ArrayList<int[]> indexPairs) {
			final int nBins = indexPairs.size();
			final int[] binCounts = new int[nBins];
			for (int iBin = 0; iBin < nBins; ++iBin) {
				final int[] indexPair = indexPairs.get(iBin);
				binCounts[iBin] = indexPair[1] - indexPair[0] + 1;
			}
			return binCounts;
		}

		private void recurseForBinMaker(final Stack<Double> dividersStack) {
			final int nDividers = dividersStack.size();
			final boolean cannotExtend = evaluate(dividersStack);
			if (cannotExtend)
				/** No point in extending this one. */
				return;
			final int nData = _data.length;
			final int minInBin = _intParams[_MinInBinPosition];
			/** Try adding a dividing line and recursing. */
			final int indexToStartAt;
			final double lastD;
			if (nDividers == 0) {
				indexToStartAt = minInBin - 1;
				lastD = Double.NEGATIVE_INFINITY;
			} else {
				final double lastDivider = dividersStack.get(nDividers - 1);
				final int lastIndex = NumericalRoutines.getGlbIndex(_data, lastDivider);
				lastD = _data[lastIndex];
				indexToStartAt = lastIndex + minInBin;
			}
			for (int index = indexToStartAt; index < nData - minInBin; ++index) {
				final double d = _data[index];
				if (d == lastD) {
					continue;
				}
				/**
				 * The potential gap is between data[index] and data[index+1]. It guarantees
				 * enough below and leaves enough above.
				 */
				if (d < _data[index + 1]) {
					/**
					 * There's a positive gap between index and index+1. Consider it.
					 */
					final double newDivider = (_data[index] + _data[index + 1]) / 2.0;
					dividersStack.push(newDivider);
					recurseForBinMaker(dividersStack);
					dividersStack.pop();
				}
			}
		}

		/**
		 * Updates the current best, and evaluates if this has an illegal bin. If it
		 * does have an illegal bin, there's no point in trying to add to this set of
		 * bins.
		 */
		private boolean evaluate(final Stack<Double> highEndpointStack) {
			final int nHighEndpoints = highEndpointStack.size();
			final double[] highEndpoints = new double[nHighEndpoints];
			final Iterator<Double> highEndpointIt = highEndpointStack.iterator();
			for (int k = 0; k < nHighEndpoints; ++k) {
				highEndpoints[k] = highEndpointIt.next();
			}
			final int nData = _data.length;
			final int nBins = nHighEndpoints + 1;
			/** Compute the bin counts and gaps for this set of dividers. */
			final double[] gaps = new double[nHighEndpoints];
			final int[] binCounts = new int[nBins];
			final double highValue = _data[nData - 1] + 1;
			int binCount = 0;
			int iBin = 0;
			final int _minInBin = _intParams[_MinInBinPosition];
			double highEndpoint = (iBin >= nHighEndpoints) ? highValue : highEndpoints[iBin];
			int thisMinInBin = Integer.MAX_VALUE;
			/** Every bin must have at least the following: */
			final int requiredBinSize = Math.max(_minInBin, (int) Math.ceil(nData / (4.0 * nBins)));
			for (int i = 0; i < nData; ++i) {
				if (_data[i] < highEndpoint) {
					++binCount;
				} else {
					/** Wrap up the old bin. */
					thisMinInBin = Math.min(thisMinInBin, binCount);
					binCounts[iBin] = binCount;
					if (binCount < requiredBinSize) {
						/**
						 * This bin has too few so we know it will lose. We cannot say "cannotExtend =
						 * true" because more bins might make this bin legal. However, we do know that
						 * this set of bins will not win.
						 */
					}
					final double gap = (_data[i] - _data[i - 1]);
					if (gap < _minLegalGap) {
						/** This gap is too small. */
						final boolean cannotExtend = true;
						return cannotExtend;
					}
					gaps[iBin] = gap;
					binCount = 0;
					/** Re-do i with the new bin. */
					--i;
					++iBin;
					highEndpoint = (iBin >= nHighEndpoints) ? highValue : highEndpoints[iBin];
				}
			}
			/** Wrap up the old (and last) bin. */
			if (binCount < requiredBinSize) {
				/**
				 * This bin has too few so we know it will lose. We cannot say "cannotExtend =
				 * true" because more bins might make this bin legal. However, we do know that
				 * this set of bins will not win.
				 */
			}
			binCounts[iBin] = binCount;
			thisMinInBin = Math.min(thisMinInBin, binCount);
			/** We're legal and pass all preliminary tests. */
			if (thisMinInBin < requiredBinSize) {
				final boolean cannotExtend = false;
				return cannotExtend;
			}
			final int oldNHighpoints = _highEndpoints.length;
			if ((nHighEndpoints < oldNHighpoints) || (nHighEndpoints == oldNHighpoints && thisMinInBin <= _minInBin)) {
				final boolean cannotExtend = false;
				return cannotExtend;
			}
			/** We win. */
			_highEndpoints = highEndpoints;
			_gaps = gaps;
			_binCounts = binCounts;
			final boolean cannotExtend = false;
			return cannotExtend;
		}

		public int getLevel(final double d) {
			return NumericalRoutines.getGlbIndex(_highEndpoints, d) + 1;
		}
	}

	public static int lexicographicalNextSet(final int low, final int high, final int[] subset) {
		while (true) {
			final int size = lexicographicalNextSet(subset);
			if (size == 0)
				return 0;
			if (low <= size && size < high)
				return size;
		}
	}

	public static int[] kPermToPerm(final int nPairs, final int kPerm) {
		/** Gets the kPerm-th permutation of 0, ..., nPairs-1. */
		final int[] perm = new int[nPairs];
		final BitSet used = new BitSet(nPairs);
		int fact = factorial(nPairs - 1);
		for (int ii = kPerm, k0 = 0; k0 < nPairs; ++k0) {
			final int block;
			if (ii == 0) {
				block = 0;
			} else {
				block = ii / fact;
			}
			int k2 = used.nextClearBit(0);
			for (int k1 = 0; k1 < block; ++k1, k2 = used.nextClearBit(k2 + 1)) {
			}
			perm[k0] = k2;
			if (k0 == nPairs - 1) {
				break;
			}
			used.set(k2);
			ii = ii - block * fact;
			fact /= nPairs - k0 - 1;
		}
		return perm;
	}

	public static int permToKPerm(final int[] perm) {
		final int nPairs = perm.length;
		int fact = factorial(nPairs - 1);
		final BitSet used = new BitSet(nPairs);
		int kPerm = 0;
		for (int k0 = 0; k0 < nPairs; ++k0) {
			int block = 0;
			for (int k1 = used.nextClearBit(0); k1 != perm[k0]; ++block, k1 = used.nextClearBit(k1 + 1)) {
			}
			kPerm += block * fact;
			used.set(perm[k0]);
			if (k0 == nPairs - 1) {
				break;
			}
			fact /= nPairs - k0 - 1;
		}
		return kPerm;
	}

	public static int kPermAndKToEntry(final int nPairs, final int kPerm, final int k) {
		/** Gets the kth entry of the permutation (nPairs, kPerm). */
		final BitSet used = new BitSet(nPairs);
		int factorial = factorial(nPairs - 1);
		for (int ii = kPerm, k0 = 0; k0 < nPairs; ++k0) {
			final int nMK0M1 = nPairs - k0 - 1;
			final int block = nMK0M1 == 0 ? 0 : ii / factorial;
			int k2 = used.nextClearBit(0);
			for (int k1 = 0; k1 < block; ++k1, k2 = used.nextClearBit(k2 + 1)) {
			}
			if (k0 == k)
				return k2;
			used.set(k2);
			ii = ii - block * factorial;
			if (nMK0M1 > 0) {
				factorial /= nMK0M1;
			}
		}
		return 0;
	}

	public static int factorial(final int n) {
		int f = 1;
		for (int k = 2; k <= n; ++k) {
			f *= k;
		}
		return f;
	}

}
