package com.skagit.utils.randomx;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Random;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.special.Erf;

import com.skagit.utils.Constants0;
import com.skagit.utils.MathX0;

public class Randomx extends Random implements Serializable, Cloneable {
	public final static NormalDistribution _NormalDistribution = new NormalDistribution();

	/**
	 * <pre>
	 * There are 2 considerations for extending Random.
	 * 1. How the basic sequence of random numbers is generated.
	 *      This is encapsulated by overriding Random's next(int nbits).
	 *
	 * 2. How the sequence is translated into
	 *    nextBoolean, nextBytes, nextInt, nextLong, nextFloat, nextDouble,
	 *    and nextGaussian.
	 *      We can either use Random's versions of these translations or
	 *    MersenneTwister's. To use Random's, we don't have to override
	 *    these routines since this is an extension of Random.  To use
	 *    MersenneTwister's, we must explicitly call our embedded
	 *    MersenneTwister, and this is our current approach.  Except
	 *    for nextGaussian, it doesn't seem to matter which translators we use.
	 *      Note that either way, these translators will call next(int nBits).
	 *    If we are using Random's translators, they will call Randomx'
	 *    next(int nBits), which calls MyMersenneTwister's
	 *    exposedNext(int nBits), which calls
	 *    MyMersenneTwister's next(int nBits).
	 *    If we are using MyMersenneTwister's translators, they call
	 *    MyMersenneTwister's next(int nBits), so the sequence being
	 *    used is the same.
	 * </pre>
	 */
	/** A plausible default seed for Random Number Generators. */
	final private static long _DefaultSeed = 1595780067347L;
	final protected static long serialVersionUID = 0;

	/**
	 * Class that creates a random sequence based on MersenneTwister, but modifed by
	 * the Random _gateKeeper.
	 */
	private static class MyMersenneTwister extends MersenneTwister {
		private static final long serialVersionUID = 1L;
		final private Random _gateKeeper;

		private MyMersenneTwister(final long seed) {
			_gateKeeper = new Random();
			setSeed(seed);
		}

		@Override
		public void setSeed(final long seed) {
			if (_gateKeeper != null) {
				super.setSeed(seed);
				_gateKeeper.setSeed(seed);
				/** Warm it up. */
				for (int k = 0; k < 50; ++k) {
					nextLong();
				}
			}
		}

		@Override
		protected int next(final int nBits) {
			for (int k = 0; _gateKeeper.nextBoolean() && k < 5; ++k) {
				super.next(32);
			}
			return super.next(nBits);
		}

		private int exposedNext(final int nBits) {
			return next(nBits);
		}
	}

	final private MyMersenneTwister _mt;

	/** Ctors; main one and convenience ones. */
	public Randomx(final long seed, final int nToAdvance) {
		_mt = new MyMersenneTwister(seed);
		for (int k = 0; k < nToAdvance; ++k) {
			setSeed(_mt.nextLong() ^ _mt._gateKeeper.nextLong());
		}
	}

	public Randomx(final long seed) {
		this(seed, /* nToAdvance= */0);
	}

	public Randomx(final boolean useCurrentTimeMs, final int nToAdvance) {
		this(useCurrentTimeMs ? System.currentTimeMillis() : _DefaultSeed, nToAdvance);
	}

	public Randomx(final boolean useCurrentTimeMs) {
		this(useCurrentTimeMs ? System.currentTimeMillis() : _DefaultSeed, /* nToAdvance= */0);
	}

	@Override
	public void setSeed(final long seed) {
		/**
		 * This is called by Random's ctor, at which point, _mt is null. When Random's
		 * ctor is done, Randomx' ctor takes over, initializes _mt, and calls this
		 * again.
		 */
		if (_mt != null) {
			_mt.setSeed(seed);
		}
	}

	/** Main routine as far as generating a sequence is concerned. */
	@Override
	protected int next(final int nBits) {
		return _mt.exposedNext(nBits);
	}

	/** The basic translations of the sequence. */
	@Override
	public boolean nextBoolean() {
		return _mt.nextBoolean();
	}

	@Override
	public void nextBytes(final byte[] bytes) {
		_mt.nextBytes(bytes);
	}

	@Override
	public int nextInt() {
		return _mt.nextInt();
	}

	@Override
	public int nextInt(final int n) {
		return _mt.nextInt(n);
	}

	@Override
	public long nextLong() {
		return _mt.nextLong();
	}

	@Override
	public float nextFloat() {
		return _mt.nextFloat();
	}

	@Override
	public double nextDouble() {
		return _mt.nextDouble();
	}

	@Override
	public double nextGaussian() {
		return _mt.nextGaussian();
	}

	/** Add more distributions here. */
	final public static double _StandardDeviations = 3d;
	final private static double _TruncateValueForRayleigh = 3d;

	public double getExponentialDraw(final double mu) {
		return -mu * Math.log(nextDouble());
	}

	public double getTruncatedGaussian(final double standardDeviations) {
		for (int k = 0; k < 10; ++k) {
			final double returnValue = nextGaussian();
			if (Math.abs(returnValue) <= standardDeviations)
				return returnValue;
		}
		/** Couldn't find one in 10 tries. Just pin it to the extreme. */
		return (nextBoolean() ? 1d : -1d) * standardDeviations;
	}

	public double getSplitGaussianDraw(double min, double mode, double max) {
		if (min > mode || min > max || mode > max) {
			final double[] d = { min, mode, max };
			Arrays.sort(d);
			min = d[0];
			mode = d[1];
			max = d[2];
		}
		final double z = getTruncatedGaussian();
		final boolean useLower = z < 0d;
		final double standardDeviation = (useLower ? mode - min : max - mode) / _StandardDeviations;
		return mode + z * standardDeviation;
	}

	public double getTruncatedGaussian() {
		return getTruncatedGaussian(_StandardDeviations);
	}

	public double getNormalizedTruncatedGaussian() {
		return getTruncatedGaussian(_StandardDeviations) / _StandardDeviations;
	}

	public double getAnnular(final double minRadius, final double maxRadius) {
		final double d = nextDouble();
		final double r1Squared = minRadius * minRadius;
		final double r2Squared = maxRadius * maxRadius;
		return Math.sqrt(d * (r2Squared - r1Squared) + r1Squared);
	}

	public double getStandardRayleighDraw() {
		return Math.sqrt(-2d * Math.log(1d - nextDouble()));
	}

	public double getStandardTruncatedRayleighDraw(final double truncateValue) {
		if (truncateValue <= 0d)
			return 0d;
		double x = truncateValue + 1d;
		while (x > truncateValue) {
			x = getStandardRayleighDraw();
		}
		return x;
	}

	public double getStandardTruncatedRayleighDraw() {
		return getStandardTruncatedRayleighDraw(_TruncateValueForRayleigh);
	}

	public double getWeibullDraw(final double scale, final double shape) {
		/**
		 * F(x) = 1 - exp(-[(scale * x)**shape)]. (Barlow, Proschan, 1975, p. 73)
		 */
		final double cdf = nextDouble();
		final double FBar = 1d - cdf;
		return Math.exp(Math.log(-Math.log(FBar)) / shape) / scale;
	}

	/**
	 * <pre>
	 * cums must be an ascending set of positive doubles.
	 * Returns a value draw, where 0 <= draw < drawTable.length.
	 * </pre>
	 */
	public int getDrawFromTable(final double[] cums) {
		final int nCums = cums.length;
		final double maxEntry = cums[nCums - 1];
		final double u = nextDouble() * maxEntry;
		/** Find the smallest index i for which u <= drawTable[i]. */
		int insertIdx = Arrays.binarySearch(cums, u);
		if (insertIdx < 0) {
			insertIdx = -insertIdx - 1;
		}
		insertIdx = Math.min(nCums - 1, insertIdx);
		return insertIdx;
	}

	public double[] nextPolarOnUnitSphere() {
		final double lng = nextDouble() * 360d - 180d;
		final double lat = Math.toDegrees(MathX0.acosX(2d * nextDouble() - 1d) - Constants0._PiOver2);
		return new double[] { lat, lng };
	}

	public double getCorrelatedUniform(final double u, final double rho, final boolean highPrecision) {
		final double z1 = _NormalDistribution.inverseCumulativeProbability(u);
		final double z2 = nextGaussian();
		final double z = rho * z1 + Math.sqrt(1 - rho * rho) * z2;
		return _NormalDistribution.cumulativeProbability(z);
	}

	public double getCorrelatedGaussian(final double z, final double rho) {
		return rho * z + Math.sqrt(1 - rho * rho) * nextGaussian();
	}

	@Override
	public Randomx clone() {
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
			out.writeObject(this);
			final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
			final ObjectInputStream in = new ObjectInputStream(bis);
			try {
				return (Randomx) in.readObject();
			} catch (final ClassNotFoundException e) {
			}
		} catch (final IOException e) {
		}
		return null;
	}

	/** Useful static utility. */
	public static double[] statisticallyStandardize(final double[] data) {
		final double[] dataClone = data.clone();
		final int n = dataClone.length;
		double sum = 0, sumSq = 0;
		for (int k = 0; k < n; ++k) {
			sum += dataClone[k];
			sumSq += dataClone[k] * dataClone[k];
		}
		final double bar = sum / n;
		final double sd = Math.sqrt((sumSq - n * bar * bar) / n);
		for (int k = 0; k < n; ++k) {
			dataClone[k] = (dataClone[k] - bar) / sd;
		}
		return dataClone;
	}

	/** Tests erf, cdf, and cdfInv. */
	public static void main(final String[] args) {
		final Randomx r1 = new Randomx(35, 0);
		final Randomx r2 = new Randomx(350L, 5);
		r2.setSeed(35);
		for (int k = 0; k < 10; ++k) {
			final double k1 = r1.nextFloat();
			final double k2 = r2.nextFloat();
			System.out.printf("\n%f %f", k1, k2);
		}
		System.exit(33);
		final NormalDistribution nn = new NormalDistribution();
		System.out.printf("Erf Values:");
		for (double d = 0d; d < 50d; d += 0.5) {
			final double xxx = Erf.erf(d);
			System.out.printf("\n%.1f: %.10f", d, xxx);
		}
		System.out.printf("Z->Cdf:");
		for (double d = -4d; d < 4d; d += 0.25) {
			final double xxx = nn.cumulativeProbability(d);
			System.out.printf("\n%.2f: %.10f", d, xxx);
		}
		System.out.printf("Cdf->Z:");
		for (double d = 0d; d <= 1d; d += 0.05) {
			final double xxx = nn.inverseCumulativeProbability(d);
			System.out.printf("\n%.2f: %.10f", d, xxx);
		}
	}

}
