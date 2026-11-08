package com.skagit.utils.math.cdf;

import java.util.SortedSet;

import org.apache.commons.math3.distribution.ChiSquaredDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;

import com.skagit.utils.Constants0;
import com.skagit.utils.MathX0;
import com.skagit.utils.NumericalRoutines;
import com.skagit.utils.myLogger.MyLogger;
import com.skagit.utils.randomx.Randomx;
import com.skagit.utils.randomx.WeightedPairDataAcc;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;

/**
 * <pre>
 * Good Discussion on Bivariate Normal contours.
 *   In particular, p. 115 says that (x,y)SigInv(x,y) is a chi-squared:
 *   http://www.public.iastate.edu/~maitra/stat501/lectures/MultivariateNormalDistribution-I.pd
 * </pre>
 */

public class BivariateNormalCdf extends Cdf {
	final public static NormalDistribution _NormalDistribution = new NormalDistribution();
	final public static double _ProbableErrorProbability = 0.5;
	final public static double _ChiSqForProbableError = InverseChiSq2CumulativeProbability(_ProbableErrorProbability);
	/**
	 * Defines what we mean by being "inside." Useful since we use this class as a
	 * hazard area.
	 */
	final private static double _ProportionForIn = 0.5;
	final public static double _ChiSqForIn = InverseChiSq2CumulativeProbability(_ProportionForIn);

	final private EigenvalueDecomposition _eigenvalueDecomposition;
	final private Matrix _sigmaSq;
	final private Matrix _sigmaSqInv;
	final private double _sigmaX, _sigmaY, _sigmaXY;

	final private double[] _smiMjrSmiMnrSmeMjrHdg50;

	/**
	 * Takes the two means and variances, as well as E(X'Y'), and forms a bivariate
	 * normal from it. Note X'Y' = (X-E(X))(Y-E(Y)).
	 */
	public BivariateNormalCdf(final MyLogger logger, final double meanX, final double meanY, double sigmaXSq,
			double sigmaYSq, final double sigmaXY, final boolean warnOnDegenerate) {
		setMeans(meanX, meanY);
		_sigmaX = sigmaXSq >= 0d ? Math.sqrt(sigmaXSq) : 0d;
		_sigmaY = sigmaYSq >= 0d ? Math.sqrt(sigmaYSq) : 0d;
		sigmaXSq = _sigmaX * _sigmaX;
		sigmaYSq = _sigmaY * _sigmaY;
		_sigmaXY = Double.isNaN(sigmaXY) ? 0d : sigmaXY;

		final Matrix sigmaSq = new Matrix(
				new double[][] { new double[] { sigmaXSq, sigmaXY }, new double[] { sigmaXY, sigmaYSq } });
		final EigenvalueDecomposition eigenvalueDecomposition0 = new EigenvalueDecomposition(sigmaSq);
		final EigenvalueDecomposition eigenvalueDecomposition = sanityCheck(logger, eigenvalueDecomposition0,
				warnOnDegenerate) ? eigenvalueDecomposition0 : null;
		if (eigenvalueDecomposition == null) {
			_eigenvalueDecomposition = null;
			_smiMjrSmiMnrSmeMjrHdg50 = new double[] { 0d, 0d, 0d };
			_sigmaSq = _sigmaSqInv = null;
			return;
		}

		/** Compute sigmaSqInv. */
		final Matrix D = eigenvalueDecomposition.getD();
		final double eigenvalue0 = D.get(0, 0);
		final double eigenvalue1 = D.get(1, 1);
		final double d0Inv = 1d / eigenvalue0;
		final double d1Inv = 1d / eigenvalue1;
		final Matrix DInv = new Matrix(new double[][] { new double[] { d0Inv, 0d }, new double[] { 0d, d1Inv } });
		final Matrix V = eigenvalueDecomposition.getV();
		final Matrix VTranspose = V.transpose();
		final Matrix sigmaSqInv = (VTranspose.times(DInv)).times(V);
		boolean isBad = false;
		K0_LOOP: for (int k0 = 0; k0 < 2; ++k0) {
			for (int k1 = 0; k1 < 2; ++k1) {
				if (!Double.isFinite(sigmaSqInv.get(k0, k1))) {
					isBad = true;
					break K0_LOOP;
				}
			}
		}
		if (isBad) {
			_eigenvalueDecomposition = null;
			_smiMjrSmiMnrSmeMjrHdg50 = new double[] { 0d, 0d, 0d };
			_sigmaSq = _sigmaSqInv = null;
			return;
		}
		_eigenvalueDecomposition = eigenvalueDecomposition;
		_sigmaSq = sigmaSq;
		_sigmaSqInv = sigmaSqInv;

		/** Compute smiMjrSmiMnrSmiMjrHdg50. */
		double mjrEig = eigenvalue0;
		double mnrEig = eigenvalue1;
		final double[][] vArray = V.getArrayCopy();
		double[] mjrEigVctr = vArray[0];
		double[] mnrEigVctr = vArray[1];
		if (mnrEig > mjrEig) {
			final double[] v = mjrEigVctr;
			mjrEigVctr = mnrEigVctr;
			mnrEigVctr = v;
			final double eig = mjrEig;
			mjrEig = mnrEig;
			mnrEig = eig;
		}
		final double smiMjr = Math.sqrt(mjrEig * _ChiSqForProbableError);
		final double smiMnr = Math.sqrt(mnrEig * _ChiSqForProbableError);
		final double alphaR = MathX0.atan2X(mjrEigVctr[1], mjrEigVctr[0]);
		double hdg = Math.toDegrees(Constants0._PiOver2 - alphaR);
		hdg = NumericalRoutines.generalMod(hdg, 360d);
		if (hdg >= 180d) {
			hdg -= 180d;
		}
		_smiMjrSmiMnrSmeMjrHdg50 = new double[] { smiMjr, smiMnr, hdg };
	}

	public boolean hasSpread() {
		return _eigenvalueDecomposition != null;
	}

	private boolean sanityCheck(final MyLogger logger, final EigenvalueDecomposition eigenvalueDecomposition,
			final boolean warnOnDegenerate) {
		final double[] realParts = eigenvalueDecomposition.getRealEigenvalues();
		final double[] imagParts = eigenvalueDecomposition.getImagEigenvalues();
		final double maxEigenvalue = Math.max(realParts[0], realParts[1]);
		final double minEigenvalue = Math.min(realParts[0], realParts[1]);
		for (int k = 0; k < 2; ++k) {
			final double imagPart = imagParts[k];
			/**
			 * Imaginary part should be insignificant compare to the smaller real part,
			 * which itself should be positive.
			 */
			if (!(Math.abs(imagPart) <= 1.0e-7 * minEigenvalue)) {
				if (warnOnDegenerate) {
					MyLogger.wrn(logger, String.format("\nLarge imag part: eig#[%d], real[%.4e] imag[%.4e]", k,
							realParts[k], imagPart));
				}
				return false;
			}
		}
		/**
		 * maxEigenvalue should not dwarf minEigenvalue because we don't want a 0
		 * eigenvalue.
		 */
		if (!(minEigenvalue * 1.0e7 >= maxEigenvalue)) {
			MyLogger.wrn(logger, String.format("\nTiny eigenvalue: max/min[%.3e/%.3e]", maxEigenvalue, minEigenvalue));
			return false;
		}
		final Matrix eigenvectors = eigenvalueDecomposition.getV();
		final Matrix identityMtx = eigenvectors.times(eigenvectors.transpose());
		for (int k0 = 0; k0 < 2; ++k0) {
			for (int k1 = 0; k1 < 2; ++k1) {
				final double target = k0 == k1 ? 1d : 0d;
				final double value = identityMtx.get(k0, k1);
				if (Math.abs(target - value) > 1.0e-7) {
					final String identityMtxString = NumericalRoutines.getString(identityMtx.getArray());
					MyLogger.err(logger, String.format("Should be identity mtx:\n%s", identityMtxString));
					return false;
				}
			}
		}

		final Matrix mtx = (eigenvectors.times(eigenvalueDecomposition.getD())).times(eigenvectors.transpose());
		final double sigmaXSq = mtx.get(0, 0);
		if (!(Math.abs(sigmaXSq - (_sigmaX * _sigmaX)) < 1.0e-7)) {
			MyLogger.wrn(logger, String.format("Decomp mismatched sigmaXSq: %f vs %f.", _sigmaX * _sigmaX, sigmaXSq));
			return false;
		}
		final double sigmaYSq = mtx.get(1, 1);
		if (!(Math.abs(sigmaYSq - _sigmaY * _sigmaY) < 1.0e-7)) {
			MyLogger.wrn(logger, String.format("Decomp mismatched sigmaYSq: %f vs %f.", _sigmaY * _sigmaY, sigmaXSq));
			return false;
		}
		final double sigmaXY = mtx.get(0, 1);
		if (Math.abs(sigmaXY - _sigmaXY) > 1.0e-7) {
			MyLogger.wrn(logger, String.format("Decomp mismatched sigmaXY: %f vs %f.", _sigmaXY, sigmaXY));
			return false;
		}
		return true;
	}

	public boolean isDegenerate() {
		return _sigmaSq == null;
	}

	public double getSigmaX() {
		return _sigmaX;
	}

	public double getSigmaY() {
		return _sigmaY;
	}

	public double getSigmaXY() {
		return _sigmaXY;
	}

	/**
	 * Returns the probability of being as close or closer as (x,y). Low numbers
	 * indicate that (x,y) is close to the mean.
	 */
	public double getContainmentProportion(final double x, final double y) {
		if (_eigenvalueDecomposition == null) {
			return 1d;
		}
		final Matrix X = new Matrix(new double[][] { new double[] { x - getMeanX(), y - getMeanY() } });
		final Matrix chiSqMtx = (X.times(_sigmaSqInv)).times(X.transpose());
		final double chiSq = chiSqMtx.get(0, 0);
		final double containment = ChiSq2CumulativeProbability(chiSq);
		return containment;
	}

	/**
	 * Creates a BivariateNormalCdf by computing the inputs for a call to the ctor,
	 * and returning what the ctor gives us.
	 */
	public static BivariateNormalCdf getBivariateNormalCdf(final MyLogger logger, final double[] x, final double[] y,
			final double[] weights) {
		final WeightedPairDataAcc acc = new WeightedPairDataAcc();
		final int n = Math.min(x.length, y.length);
		for (int k = 0; k < n; ++k) {
			acc.add(x[k], y[k], weights == null ? 1d : weights[k]);
		}
		//
		/** Following line sets _mean and _var of acc. */
		final double[][] var = acc.getVar();
		final double[] means = acc.getMean();
		final double xBar = means[0];
		final double yBar = means[1];
		final double sigmaXSq = var[0][0];
		final double sigmaYSq = var[1][1];
		final double sigmaXY = var[0][1];
		return new BivariateNormalCdf(logger, xBar, yBar, sigmaXSq, sigmaYSq, sigmaXY, /* warnOnDegenerate= */true);
	}

	public double[] get50SmiMjrSmiMnrSmiMjrHdg() {
		return _smiMjrSmiMnrSmeMjrHdg50;
	}

	@Override
	protected double[] cdfsToOffsets(final MyLogger logger, final double cdfX, final double cdfYGivenX,
			double[] result) {
		if (result == null) {
			result = new double[2];
		}
		if (_eigenvalueDecomposition == null) {
			result[0] = result[1] = 0d;
			return result;
		}
		final double z1 = Randomx._NormalDistribution.inverseCumulativeProbability(cdfX);
		result[0] = z1 * _sigmaX;
		final double z2 = Randomx._NormalDistribution.inverseCumulativeProbability(cdfYGivenX);
		final double rho = _sigmaXY / (_sigmaX * _sigmaY);
		final double rhoPrime = Math.sqrt(1d - rho * rho);
		result[1] = _sigmaY * (rho * z1 + rhoPrime * z2);
		return result;
	}

	@Override
	protected double[] offsetsToCdfs(final double offsetX, final double offsetY, double[] result) {
		if (result == null) {
			result = new double[2];
		}
		if (_eigenvalueDecomposition == null) {
			result[0] = result[1] = 0.5;
			return result;
		}
		final double z1 = offsetX / _sigmaX;
		result[0] = Randomx._NormalDistribution.cumulativeProbability(z1);
		final double rho = _sigmaXY / (_sigmaX * _sigmaY);
		final double rhoPrime = Math.sqrt(1d - rho * rho);
		final double z2 = (offsetY / _sigmaY - rho * z1) / rhoPrime;
		result[1] = Randomx._NormalDistribution.cumulativeProbability(z2);
		return result;
	}

	@Override
	protected boolean offsetsAreIn(final double offsetX, final double offsetY) {
		if (_eigenvalueDecomposition == null) {
			return false;
		}
		final double containment = getContainmentProportion(offsetX + getMeanX(), offsetY + getMeanY());
		return containment <= _ProportionForIn;
	}

	@Override
	protected void addInternalIntersectingTimesForOffsets(final double[] offset0, final double[] offset1,
			final SortedSet<Double> times) {
		if (_eigenvalueDecomposition == null) {
			return;
		}
		/**
		 * <pre>
		 * Solve for t in:
		 *      "offset0 + t(offset1-offset0)
		 *      is on the threshold ellipse."
		 * </pre>
		 */
		final double x = offset0[0], y = offset0[1];
		final double d0 = offset1[0] - x, d1 = offset1[1] - y;
		final double D = _sigmaSqInv.get(0, 0);
		final double E = _sigmaSqInv.get(0, 1);
		final double F = _sigmaSqInv.get(1, 1);
		final double a = (D * d0 * d0) + (F * d1 * d1) + (2 * E * d0 * d1);
		final double b = (2 * x * d0 * D) + (2 * y * d1 * F) + (2 * (x * d1 + y * d0) * E);
		final double c = -_ChiSqForIn + (x * x * D) + (y * y * F) + (2 * x * y * E);
		final double[] roots = NumericalRoutines.quadratic(a, b, c, null);
		if (roots == null) {
			return;
		}
		if (roots.length > 0 && roots[0] > 0 && roots[0] < 1) {
			times.add(roots[0]);
		}
		if (roots.length > 1 && roots[1] > 0 && roots[1] < 1) {
			times.add(roots[1]);
		}
	}

	@Override
	protected Cdf rotateOffsetDistribution(final MyLogger logger, final double c, final double s) {
		if (_eigenvalueDecomposition == null) {
			return new BivariateNormalCdf(logger, 0d, 0d, 0d, 0d, 0d, /* warnOnDegenerate= */false);
		}
		final double[][] leftArray = { new double[] { c, -s }, new double[] { s, c } };
		final Matrix left = new Matrix(leftArray);
		final Matrix rotatedSigmaSq = (left.times(_sigmaSq)).times(left.transpose());
		final double rotatedSigmaXSq = rotatedSigmaSq.get(0, 0);
		final double rotatedSigmaYSq = rotatedSigmaSq.get(1, 1);
		final double rotatedSigmaXY = rotatedSigmaSq.get(0, 1);
		return new BivariateNormalCdf(logger, 0d, 0d, rotatedSigmaXSq, rotatedSigmaYSq, rotatedSigmaXY,
				/* warnOnDegenerate= */true);
	}

	@Override
	protected String bodyString() {
		final StringBuffer b = new StringBuffer();
		final String sigmaString = NumericalRoutines.getString(_sigmaSq.getArray());
		b.append(Constants0._NewLine).append("SigmaSq =").append(Constants0._NewLine).append(sigmaString);
		return new String(b);
	}

	/**
	 * Here, they want a single containment radius, even with unequal sigmas. We
	 * invent something for different sigmas.
	 */
	public static double standardDeviationsToContainmentRadius(final double containment, final double sigma0,
			final double sigma1) {
		final double chiSq = InverseChiSq2CumulativeProbability(containment);
		final double containmentRadius = Math.sqrt(chiSq * sigma0 * sigma1);
		return containmentRadius;
	}

	public static double[] probableErrorRadiiToContainmentRadii(final double probableErrorSmiMjrIn,
			final double probableErrorSmiMnrIn, final double containment) {
		final double chiSqScale = _ChiSqForProbableError / InverseChiSq2CumulativeProbability(containment);
		return new double[] { Math.sqrt(chiSqScale * probableErrorSmiMjrIn),
				Math.sqrt(chiSqScale * probableErrorSmiMnrIn), };
	}

	public static double[] standardDeviationsToContainmentRadii(final double containment, final double sigma0,
			final double sigma1) {
		final double sqrtChiSq = Math.sqrt(InverseChiSq2CumulativeProbability(containment));
		return new double[] { sqrtChiSq * sigma0, sqrtChiSq * sigma1 };
	}

	public static double[] containmentRadiiToStandardDeviations(final double containment, final double semiMajor,
			final double semiMinor) {
		final double sqrtChiSq = Math.sqrt(InverseChiSq2CumulativeProbability(containment));
		return new double[] { semiMajor / sqrtChiSq, semiMinor / sqrtChiSq };
	}

	/**
	 * <pre>
	 * Chi-squared with 2 df, the only Chi-square of interest, has simple cdf and
	 * inverse cdf functions.  Simple form is found in Wiki:
	 * https://en.wikipedia.org/wiki/Chi-squared_distribution#Cumulative_distribution_function
	 * </pre>
	 */
	public static double ChiSq2CumulativeProbability(final double chiSq) {
		final double prob = 1d - Math.exp(-chiSq / 2d);
		return prob;
	}

	/** Usually, we have to take the sqrt of the return value. */
	public static double InverseChiSq2CumulativeProbability(final double prob) {
		return -2d * Math.log(1d - prob);
	}

	@SuppressWarnings("unused")
	public static void main(final String[] args) {
		final double[] containmentRadii = standardDeviationsToContainmentRadii(0.7, 3d, 3d);
		final double semiMajor = containmentRadii[0];
		final double semiMinor = containmentRadii[1];
		final double[] standardDeviations = containmentRadiiToStandardDeviations(0.7, semiMajor, semiMinor);
		final double semiMajorA = standardDeviationsToContainmentRadius(0.7, 3d, 3d);
		final double[] standardDeviation = containmentRadiiToStandardDeviations(0.7, semiMajorA, semiMajorA);
		final int x = 0;
	}

	/** Testing containment calculations. */
	@SuppressWarnings("unused")
	public static void main3(final String[] args) {
		final double c = Math.sqrt(3d) / 2d;
		final double s = 0.5;
		final Matrix sigmaSq = new Matrix(new double[][] { { 9, 0 }, { 0, 4 } });
		final Matrix sigmaSqInv = sigmaSq.inverse();
		final double[] basis0 = { c, s };
		final double[] basis1 = { -s, c };
		final Matrix rotationBasis = new Matrix(new double[][] { basis0, basis1 });
		final Matrix rotatedInv = (rotationBasis.transpose().times(sigmaSqInv)).times(rotationBasis);
		final Matrix rotated = (rotationBasis.transpose().times(sigmaSq)).times(rotationBasis);
		final double rotatedSigmaXSq = rotated.get(0, 0);
		final double rotatedSigmaYSq = rotated.get(1, 1);
		final double rotatedSigmaXY = rotated.get(1, 0);
		final BivariateNormalCdf cdf = new BivariateNormalCdf(/* logger= */null, 0d, 0d, rotatedSigmaXSq,
				rotatedSigmaYSq, rotatedSigmaXY, /* warnOnDegenerate= */true);
		final int x = 0;
	}

	/** Checks the formula for chiSq cdf and inv cdf. */
	public static void main1(final String[] args) {
		final MyLogger logger = null;
		final ChiSquaredDistribution chiSquared = new ChiSquaredDistribution(2d);
		final double p = chiSquared.cumulativeProbability(5.99146454710798);
		MyLogger.out(logger, "\ncdf of 5.9914... = " + p);
		/** -5.991464... is gotten from the AOU code given me. */
		final double containmentValue = 1d - Math.exp(-5.99146454710798 / 2d);
		MyLogger.out(logger, "\nConfidence = " + containmentValue);
		MyLogger.out(logger, "\nContainment = 0.95, sigma = " + containmentRadiiToStandardDeviations(0.95, 1d, 1d));
		MyLogger.out(logger, "\nContainment = 0.5, sigma = " + containmentRadiiToStandardDeviations(0.5, 1d, 1d));
	}

}
