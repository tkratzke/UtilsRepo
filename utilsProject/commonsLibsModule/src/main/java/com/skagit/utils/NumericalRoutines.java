package com.skagit.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Stack;
import java.util.TreeSet;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.QRDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

/**
 * This class is a collection of routines that do common floating point
 * operations. These include matrix solving, quadratic formula, formatting
 * floating point numbers, copying single precision arrays to double precision
 * arrays, and others.
 */
public class NumericalRoutines implements Constants0 {
	private static double _Adjustment = Math.sqrt(Double.MIN_VALUE);

	/** Operations. */
	public enum Operation {
		ADD, SUBTRACT, MULTIPLY, DIVIDE, POWER, COPY_TO, SET;
	}

	public static Comparator<double[]> _ByFirstAndSecondOnly = (array0, array1) -> {
		for (int i = 0; i < 2; ++i) {
			if (array0[i] < array1[i]) {
				return -1;
			}
			if (array0[i] > array1[i]) {
				return 1;
			}
		}
		return 0;
	};

	public static Comparator<double[]> _ByFirstOnly = (xy0, xy1) -> {
		if ((xy0 == null) && (xy1 == null)) {
			return 0;
		}
		if ((xy0 == null) || (xy1 == null)) {
			return xy0 == null ? -1 : 1;
		}
		final double compareValue = xy0[0] - xy1[0];
		return compareValue < 0 ? -1 : (compareValue > 0 ? 1 : 0);
	};

	public static Comparator<double[]> _ByAllInOrder1 = (array0, array1) -> {
		if ((array0 == null) != (array1 == null)) {
			return array0 == null ? -1 : 1;
		}
		if (array0 == null) {
			return 0;
		}
		final int n0 = array0.length;
		final int n1 = array1.length;
		if (n0 != n1) {
			return n0 < n1 ? -1 : 1;
		}
		for (int i = 0; i < n0; ++i) {
			if (array0[i] < array1[i]) {
				return -1;
			}
			if (array0[i] > array1[i]) {
				return 1;
			}
		}
		return 0;
	};

	public static boolean deepEquals(final double[][] a, final double[][] b) {
		return Arrays.deepEquals(a, b);
	}

	public static boolean deepEquals(final double[] b0, final double[] b1) {
		return Arrays.equals(b0, b1);
	}

	public static double[] buildCumsArray(final double[] priors) {
		if (priors == null) {
			return null;
		}
		final int n = priors.length;
		final double[] cums = new double[n];
		for (int k = 0; k < n; ++k) {
			cums[k] = (k > 0 ? cums[k - 1] : 0d) + priors[k];
		}
		if (n == 0) {
			return cums;
		}
		final double ttl = cums[n - 1];
		for (int k = 0; k < n - 1; ++k) {
			cums[k] /= ttl;
		}
		cums[n - 1] = 1d;
		return cums;
	}

	public static int compare(final double a, final double b) {
		return compare(a, b, 1.0e-10);
	}

	public static int compare(final double a, final double b, final double maxRelativeError) {
		if (Double.isNaN(a) && Double.isNaN(b)) {
			return 0;
		}
		if (Double.isNaN(a)) {
			return -1;
		}
		if (Double.isNaN(b)) {
			return 1;
		}
		final double relativeError = getRelativeError(a, b);
		if (relativeError < maxRelativeError) {
			return 0;
		}
		return a < b ? -1 : (a > b ? 1 : 0);
	}

	public static double getRelativeError(double approximation, double targetValue) {
		/** Kind of a kluge here to compare two numbers close to 0d; */
		if (targetValue == 0d) {
			targetValue = _Adjustment;
			approximation += _Adjustment;
		}
		return Math.abs((approximation - targetValue) / targetValue);
	}

	/**
	 * Changes the input to something that has unit length and returns the length of
	 * the input.
	 */
	public static double convertToUnitLength(final double[] a) {
		double maxComponent = 0d;
		double sumSq = 0d;
		for (final double d : a) {
			sumSq += d * d;
			final double absD = Math.abs(d);
			if (absD > maxComponent) {
				maxComponent = absD;
			}
		}
		if (maxComponent == 0d) {
			return 0d;
		}
		final double magnitude = Math.sqrt(sumSq);
		if (Math.abs(magnitude / maxComponent - 1d) > 1.0e-15) {
			return magnitude;
		}
		multiply(1d / magnitude, a);
		return magnitude;
	}

	public static int getLubIndex(final double[] array, final double key) {
		final int j = Arrays.binarySearch(array, key);
		if (j < 0) {
			return -(j + 1);
		}
		/** We have a match. Find the smallest one that is still a match. */
		int lubIndex = j;
		for (lubIndex = j - 1; lubIndex >= 0; --lubIndex) {
			if (array[lubIndex] < key) {
				return lubIndex + 1;
			}
		}
		return 0;
	}

	public static void multiply(final double scalar, final double[] a) {
		final int n = a == null ? 0 : a.length;
		for (int i = 0; i < n; ++i) {
			a[i] *= scalar;
		}
	}

	public static double[][] multiply(final double[][] a, final double[][] b) {
		final int m = a == null ? 0 : a.length;
		if (m == 0) {
			return new double[][] { {} };
		}
		final int n = (b == null || b.length == 0) ? 0 : b[0].length;
		if (n == 0) {
			return new double[][] { {} };
		}
		final int mn = a[0].length;
		if (mn == 0 || mn != b.length) {
			return null;
		}
		final double[][] result = new double[m][];
		for (int i = 0; i < m; ++i) {
			result[i] = new double[n];
			for (int j = 0; j < n; ++j) {
				result[i][j] = 0d;
				for (int k = 0; k < mn; ++k) {
					result[i][j] += a[i][k] * b[k][j];
				}
			}
		}
		return result;
	}

	public static double[] multiply(final double[][] a, final double[] b) {
		final int m = a == null ? 0 : a.length;
		final int n = b == null ? 0 : b.length;
		if (m == 0 || n == 0) {
			return new double[] {};
		}
		final double[] result = new double[m];
		for (int i = 0; i < m; ++i) {
			result[i] = 0d;
			for (int j = 0; j < n; ++j) {
				result[i] += a[i][j] * b[j];
			}
		}
		return result;
	}

	public static double[] multiply(final double[] b, final double[][] a) {
		final int m = a == null ? 0 : a.length;
		if (m == 0) {
			return new double[] {};
		}
		final int n = a[0].length;
		if (n == 0) {
			return new double[] {};
		}
		final double[] result = new double[n];
		for (int j = 0; j < n; ++j) {
			result[j] = 0d;
			for (int i = 0; i < m; ++i) {
				result[j] += b[i] * a[i][j];
			}
		}
		return result;
	}

	public static double[][] multiply(final double[][] a, final double[][] b, final double[][] c) {
		final int m = a == null ? 0 : a.length;
		if (m == 0) {
			return new double[][] { {} };
		}
		final int n = (c == null || c.length == 0) ? 0 : c[0].length;
		if (n == 0) {
			return new double[][] { {} };
		}
		final int middle1 = a[0].length;
		if (middle1 == 0) {
			return new double[][] { {} };
		}
		final int nBRows = b == null ? 0 : b.length;
		if (nBRows != middle1) {
			return null;
		}
		final int middle2 = (b == null || b.length == 0) ? 0 : b[0].length;
		final int nCRows = c == null ? 0 : c.length;
		if (nCRows != middle2) {
			return null;
		}
		final double[][] result = new double[m][n];
		for (int i = 0; i < m; ++i) {
			for (int j = 0; j < n; ++j) {
				result[i][j] = 0;
				for (int k1 = 0; k1 < middle1; ++k1) {
					for (int k2 = 0; k2 < middle2; ++k2) {
						result[i][j] += a[i][k1] * b[k1][k2] * c[k2][j];
					}
				}
			}
		}
		return result;
	}

	public static void pointwiseOperate(final double[][] mtx1, final double[][] mtx2,
			final NumericalRoutines.Operation o) {
		final int m = Math.min(mtx1.length, mtx2.length);
		final int n = Math.min(mtx1[0].length, mtx2[0].length);
		switch (o) {
		case ADD:
			for (int i = 0; i < m; ++i) {
				for (int j = 0; j < n; ++j) {
					mtx1[i][j] += mtx2[i][j];
				}
			}
			break;
		case SUBTRACT:
			for (int i = 0; i < m; ++i) {
				for (int j = 0; j < n; ++j) {
					mtx1[i][j] -= mtx2[i][j];
				}
			}
			break;
		case MULTIPLY:
			for (int i = 0; i < m; ++i) {
				for (int j = 0; j < n; ++j) {
					mtx1[i][j] *= mtx2[i][j];
				}
			}
			break;
		case DIVIDE:
			for (int i = 0; i < m; ++i) {
				for (int j = 0; j < n; ++j) {
					mtx1[i][j] /= mtx2[i][j];
				}
			}
			break;
		case POWER:
			for (int i = 0; i < m; ++i) {
				for (int j = 0; j < n; ++j) {
					mtx1[i][j] = Math.pow(mtx1[i][j], mtx2[i][j]);
				}
			}
			break;
		case COPY_TO:
			for (int i = 0; i < m; ++i) {
				for (int j = 0; j < n; ++j) {
					mtx2[i][j] = mtx1[i][j];
				}
			}
			break;
		case SET:
			break;
		default:
			break;
		}
	}

	public static double[][] transpose(final double[][] a) {
		final int m = a.length, n = a[0].length;
		final double[][] transpose = new double[n][m];
		for (int i = 0; i < m; ++i) {
			for (int j = 0; j < n; ++j) {
				transpose[j][i] = a[i][j];
			}
		}
		return transpose;
	}

	public static void pointwiseOperate(final double[] v1, final double[] v2, final NumericalRoutines.Operation o) {
		final int m = v1.length;
		if (v2.length != m) {
			return;
		}
		switch (o) {
		case ADD:
			for (int i = 0; i < m; ++i) {
				v1[i] += v2[i];
			}
			break;
		case SUBTRACT:
			for (int i = 0; i < m; ++i) {
				v1[i] -= v2[i];
			}
			break;
		case MULTIPLY:
			for (int i = 0; i < m; ++i) {
				v1[i] *= v2[i];
			}
			break;
		case DIVIDE:
			for (int i = 0; i < m; ++i) {
				v1[i] /= v2[i];
			}
			break;
		case POWER:
			for (int i = 0; i < m; ++i) {
				v1[i] = Math.pow(v1[i], v2[i]);
			}
			break;
		case COPY_TO:
			for (int i = 0; i < m; ++i) {
				v2[i] = v1[i];
			}
			break;
		case SET:
			break;
		default:
			break;
		}
	}

	public static void fill(final double[] v, final double d) {
		final int m = v.length;
		for (int k = 0; k < m; ++k) {
			v[k] = d;
		}
	}

	public static double getLength(final double[] vector) {
		return Math.sqrt(dotProduct(vector, vector));
	}

	/**
	 * As one point goes from (a00,a01) to (a10,a11), and another point goes from
	 * (b00,b01) to (b10,b11), this routine answers when (between 0d and 1d) the two
	 * points are closest together.
	 */
	public static double getProportionOfCpa(final double xA0, final double yA0, final double xA1, final double yA1,
			final double xB0, final double yB0, final double xB1, final double yB1) {
		// minimize (over t) ||a0 + t(a1-a0) - (b0 + t(b1-b0))||.
		// A := a0-b0, B := (a1-a0)-(b1-b0), minimize ||A + t*B||.
		final double A0 = xA0 - xB0, A1 = yA0 - yB0;
		final double B0 = xA1 - xA0 - (xB1 - xB0), B1 = yA1 - yA0 - (yB1 - yB0);
		// Minimize the square of the norm by setting the
		// square's norm's derivative to 0:
		// 2(A0 + t*B0)B0 + 2(A1 + t*B1)B1 = 0.
		// A0*B0 + A1*B1 = -t(B0*B0 + B1*B1).
		// t = -(A0*B0 + A1*B1)/(B0*B0 + B1*B1).
		final double r2OfB = B0 * B0 + B1 * B1;
		// If r2OfB = 0d, the a's are moving exactly as the b's are. Hence
		// t = 0d works as well as any t. Otherwise, compute t.
		double t = r2OfB > 0d ? -(A0 * B0 + A1 * B1) / r2OfB : 0d;
		// We're only interested in values of t between 0d and 1d.
		if (t < 0d) {
			t = 0d;
		} else if (t > 1d) {
			t = 1d;
		}
		return t;
	}

	/**
	 * A binary cross product makes sense only if the vectors are 3-tuples.
	 */
	public static double[] crossProduct(final double[] b0, final double[] b1) {
		final double[] result = new double[3];
		result[0] = b0[1] * b1[2] - b0[2] * b1[1];
		result[1] = b0[2] * b1[0] - b0[0] * b1[2];
		result[2] = b0[0] * b1[1] - b0[1] * b1[0];
		return result;
	}

	public static double dotProduct(final double[] a, final double[] b) {
		final int n = a.length;
		double result = 0d;
		for (int i = 0; i < n; ++i) {
			result += a[i] * b[i];
		}
		return result;
	}

	public static double generalMod(final double n, final double m) {
		final double absM = Math.abs(m);
		if (n >= 0d) {
			return n % absM;
		}
		final double nn = m - (-n % m);
		return nn == m ? 0d : nn;
	}

	public static double[] linearCombination(final double alpha, final double beta, final double[] b0,
			final double[] b1) {
		final int n0 = b0 == null ? 0 : b0.length;
		final int n1 = b1 == null ? 0 : b1.length;
		if (n0 != n1 || n0 == 0) {
			return null;
		}
		final int n = n0;
		final double[] b = new double[n];
		for (int i = 0; i < n; ++i) {
			b[i] = alpha * b0[i] + beta * b1[i];
		}
		return b;
	}

	public static double[] linearCombination(final double alpha, final double beta, final double gamma,
			final double[] b0, final double[] b1, final double[] b2) {
		final int n0 = b0 == null ? 0 : b0.length;
		final int n1 = b1 == null ? 0 : b1.length;
		final int n2 = b2 == null ? 0 : b2.length;
		if (n1 != n0 || n2 != n0 || n0 == 0) {
			return null;
		}
		final int n = n0;
		final double[] b = new double[n];
		for (int k = 0; k < n; ++k) {
			b[k] = alpha * b0[k] + beta * b1[k] + gamma * b2[k];
		}
		return b;
	}

	public static double[] rotateXy(final double c, final double s, final double x, final double y) {
		return new double[] { c * x - s * y, //
				s * x + c * y };
	}

	public static void rotateXy(final double c, final double s, final double x, final double y,
			final double[] putHere) {
		putHere[0] = c * x - s * y;
		putHere[1] = s * x + c * y;
	}

	public static float[][] deepClone(final float[][] a) {
		if (a == null) {
			return null;
		}
		final int n = a.length;
		final float[][] clone = new float[n][];
		for (int i = 0; i < n; ++i) {
			clone[i] = a[i].clone();
		}
		return clone;
	}

	public static double distance(final double[] array0, final double[] array1) {
		final int n = array0 == null ? 0 : array0.length;
		final int n1 = array1 == null ? 0 : array1.length;
		if (n == 0 || n != n1) {
			return Double.POSITIVE_INFINITY;
		}
		if (n == 2) {
			return Math.hypot(array0[0] - array1[0], array0[1] - array1[1]);
		}
		return Math.sqrt(dotProduct(array0, array1));
	}

	public static int getGlbIndex(final double[] array, final double d) {
		final int j = Arrays.binarySearch(array, d);
		if (j < 0) {
			return -j - 2;
		}
		final int n = array.length;
		int glbIndex = j;
		for (glbIndex = j + 1; glbIndex < n; ++glbIndex) {
			if (array[glbIndex] > d) {
				return glbIndex - 1;
			}
		}
		return n - 1;
	}

	/**
	 * Solves the quadratic equation ax^2 + bx + c, and leaves the roots in result.
	 *
	 * @param result If not null, the roots are put there.
	 * @return Always returns an array of length 2. return[0] = a root if there is
	 *         at least one root, return[1] = a root if there are 2 roots. If there
	 *         are no roots, both return[0] and return[1] are Double.NaN. If a is
	 *         0d, so there is only one root, return[1] is Double.NaN.
	 */
	public static double[] quadratic(final double a, final double b, final double c, final double[] rootsA) {
		final double[] roots = (rootsA != null && rootsA.length == 2) ? rootsA : new double[2];
		if (a != 0d) {
			if (c == 0d) {
				if (b != 0d) {
					/** b is not 0, c is 0; we have 2 roots; 0 and -b/a. */
					roots[0] = 0d;
					roots[1] = -b / a;
				} else {
					/** b and c are both 0; we have two roots; both 0. */
					roots[0] = roots[1] = 0d;
				}
			} else {
				final double discriminant = b * b - 4d * a * c;
				if (discriminant < 0d) {
					roots[0] = roots[1] = Double.NaN;
				} else {
					final double twoC = 2d * c;
					final double squareRootOfDiscriminant = Math.sqrt(discriminant);
					final double den1 = -b - squareRootOfDiscriminant;
					/**
					 * Neither den1 nor den2 can be 0 from numerical analysis.
					 */
					roots[0] = twoC / den1;
					final double den2 = -b + squareRootOfDiscriminant;
					roots[1] = twoC / den2;
				}
			}
		} else /** a is 0d. */
		if (b != 0d) {
			roots[0] = -c / b;
			roots[1] = Double.NaN;
		} else if (c != 0d) {
			roots[0] = roots[1] = Double.NaN;
		} else {
			roots[0] = 0d;
			roots[1] = Double.NaN;
		}
		return roots;
	}

	public static boolean relativeErrorIsSmall(final double d0, final double d1, final double relativeErrorThreshold) {
		final double d0Abs = Math.abs(d0);
		final double d1Abs = Math.abs(d1);
		/** If either is very close to 0, both have to be. */
		if (Math.min(d0Abs, d1Abs) < relativeErrorThreshold) {
			return Math.max(d0Abs, d1Abs) < relativeErrorThreshold;
		}
		if (d0 > 0d != d1 > 0d) {
			return false;
		}
		final double relativeError = Math.abs(d0Abs - d1Abs) / Math.max(d0Abs, d1Abs);
		return relativeError <= relativeErrorThreshold;
	}

	public static boolean isZeroAngle(final double a) {
		return Math.abs(a) < _2Pi * 1.0e-10;
	}

	/**
	 * Returns a number between -pi (a very hard right) and pi (a very hard left).
	 * This is the same range as atan2. The vectors are 2D.
	 */
	public static double getTurn(final double[] xy0, final double[] xy1, final double[] xy2) {
		final double previousX = xy0[0] - xy1[0];
		final double previousY = xy0[1] - xy1[1];
		/** Set b to the direction from 0 to 1. */
		final double[] b = { -previousX, -previousY };
		convertToUnitLength(b);
		final double[] bPerp = { -b[1], b[0] };
		/** Find the coordinates wrt the basis [b,bPerp]. */
		final double nextX = xy2[0] - xy1[0];
		final double nextY = xy2[1] - xy1[1];
		final double[] c = { nextX, nextY };
		final double xCoord = dotProduct(b, c);
		final double yCoord = dotProduct(bPerp, c);
		return MathX0.atan2X(yCoord, xCoord);
	}

	public static double round(final double d, final double inc) {
		final int nIncs = (int) Math.round(d / inc);
		return nIncs * inc;
	}

	public static double symDiff(final double horizontal0, final double vertical0, final double horizontal1,
			final double vertical1) {
		final double deltaHorizontal = Math.abs(horizontal0 - horizontal1);
		final double deltaVertical = Math.abs(vertical0 - vertical1);
		final double smallerHorizontal = Math.min(horizontal0, horizontal1);
		if (horizontal0 >= horizontal1 == vertical0 >= vertical1) {
			/**
			 * <pre>
			 * deltaHorizontal * largerVertical to pick up the entire outside columns.
			 * smallerHorizontal * deltaVertical to pick up the two truncated rows.
			 * </pre>
			 */
			final double largerVertical = Math.max(vertical0, vertical1);
			return deltaHorizontal * largerVertical + smallerHorizontal * deltaVertical;
		}
		final double smallerVertical = Math.min(vertical0, vertical1);
		return smallerVertical * deltaHorizontal + smallerHorizontal * deltaVertical;
	}

	/** getStrings for doubles. */
	public static String getString(final double a, final int width, final int nSignificantDigits) {
		final double threshold = getThreshold(nSignificantDigits);
		return getStringWithThreshold(a, width, nSignificantDigits, threshold, /* doNotCheck= */false);
	}

	public static String getString(final double[] a, final int width, final int nSignificantDigits) {
		final double threshold = getThreshold(nSignificantDigits);
		return getStringWithThreshold(a, width, nSignificantDigits, threshold, /* doNotCheck= */false);
	}

	public static String getString(final double[][] a, final int width, final int nSignificantDigits) {
		final double threshold = getThreshold(nSignificantDigits);
		return getStringWithThreshold(a, width, nSignificantDigits, threshold, /* doNotCheck= */false);
	}

	public static String getString(final double[][][] a, final int width, final int nSignificantDigits) {
		final double threshold = getThreshold(nSignificantDigits);
		return getStringWithThreshold(a, width, nSignificantDigits, threshold, /* doNotCheck= */false);
	}

	public static String getString(final double a) {
		return getString(a, 8, 3);
	}

	public static String getString(final double[] a) {
		return getString(a, 8, 3);
	}

	public static String getString(final double[][] a) {
		return getString(a, 8, 3);
	}

	public static String getString(final double[][][] a) {
		return getString(a, 8, 3);
	}

	/** Private, real work versions of getString's. */
	private static String getStringWithThreshold(final double a, final int widthIn, final int nSignificantDigitsIn,
			final double threshold, final boolean doNotCheck) {
		final int width, nSignificantDigits;
		if (!Double.isFinite(a)) {
			width = widthIn;
			nSignificantDigits = nSignificantDigitsIn;
		} else if (doNotCheck) {
			width = widthIn;
			nSignificantDigits = nSignificantDigitsIn;
		} else {
			final int maxWidth = getMaxWidthForInt(a, threshold);
			if (maxWidth >= 0) {
				width = maxWidth;
				nSignificantDigits = 0;
			} else {
				width = widthIn;
				nSignificantDigits = nSignificantDigitsIn;
			}
		}
		if (nSignificantDigits > 0) {
			/** Print it out as a double. */
			final String format = (width > 0 ? "%" + width + "." : "%.") + nSignificantDigits + "f";
			return String.format(format, a);
		}
		/** Print it out as an integer. */
		final String format = (width > 0 ? "%" + width : "%") + "d";
		return String.format(format, (int) Math.round(a));
	}

	private static String getStringWithThreshold(final double[] a, final int widthIn, final int nSignificantDigitsIn,
			final double threshold, final boolean doNotCheck) {
		final int n = a == null ? 0 : a.length;
		if (n == 0) {
			return "[]";
		}
		final int width, nSignificantDigits;
		if (doNotCheck) {
			width = widthIn;
			nSignificantDigits = nSignificantDigitsIn;
		} else {
			final int maxWidth = getMaxWidthForInt(a, threshold);
			if (maxWidth >= 0) {
				width = maxWidth;
				nSignificantDigits = 0;
			} else {
				width = widthIn;
				nSignificantDigits = nSignificantDigitsIn;
			}
		}
		final StringBuffer b = new StringBuffer("[");
		for (int i = 0; i < n; ++i) {
			if (i > 0) {
				if (nSignificantDigits > 0) {
					b.append(", ");
				} else {
					b.append(" ");
				}
			}
			b.append(getStringWithThreshold(a[i], width, nSignificantDigits, threshold, /* doNotCheck= */true));
		}
		b.append("]");
		return new String(b);
	}

	public static double bigDecimalRound(final double d, final int places) {
		BigDecimal bigDecimal = BigDecimal.valueOf(d);
		bigDecimal = bigDecimal.setScale(places, RoundingMode.HALF_UP); // HALF_UP is standard rounding
		return bigDecimal.doubleValue();
	}

	public static double round(final double d, final int nSignificantDigits) {
		/** E.g.: DecimalFormat formatObject = new DecimalFormat("#.##E0"); */
		if (nSignificantDigits <= 0) {
			return d;
		}
		final char[] format = new char[2 + nSignificantDigits - 1 + 2];
		Arrays.fill(format, '#');
		format[1] = '.';
		format[nSignificantDigits + 1] = 'E';
		format[nSignificantDigits + 2] = '0';
		final DecimalFormat formatObject = new DecimalFormat(new String(format));
		return Double.parseDouble(formatObject.format(d));
	}

	private static String getStringWithThreshold(final double[][] a, final int widthIn, final int nSignificantDigitsIn,
			final double threshold, final boolean doNotCheck) {
		final int m = a == null ? 0 : a.length;
		if (m == 0) {
			return "[[]]";
		}
		final int width, nSignificantDigits;
		if (doNotCheck) {
			width = widthIn;
			nSignificantDigits = nSignificantDigitsIn;
		} else {
			final int maxWidth = getMaxWidthForInt(a, threshold);
			if (maxWidth >= 0) {
				width = maxWidth;
				nSignificantDigits = 0;
			} else {
				width = widthIn;
				nSignificantDigits = nSignificantDigitsIn;
			}
		}
		final StringBuffer b = new StringBuffer("\n[");
		for (int k = 0; k < m; ++k) {
			b.append("\n " + getStringWithThreshold(a[k], width, nSignificantDigits, threshold, /* doNotCheck= */true));
		}
		b.append("\n]");
		return new String(b);
	}

	private static String getStringWithThreshold(final double[][][] a, final int widthIn,
			final int nSignificantDigitsIn, final double threshold, final boolean doNotCheck) {
		final int m = a == null ? 0 : a.length;
		if (m == 0) {
			return "[[[]]]";
		}
		final int width, nSignificantDigits;
		if (doNotCheck) {
			width = widthIn;
			nSignificantDigits = nSignificantDigitsIn;
		} else {
			final int maxWidth = getMaxWidthForInt(a, threshold);
			if (maxWidth >= 0) {
				width = maxWidth;
				nSignificantDigits = 0;
			} else {
				width = widthIn;
				nSignificantDigits = nSignificantDigitsIn;
			}
		}
		final StringBuffer b = new StringBuffer("\n[");
		for (int k = 0; k < m; ++k) {
			b.append("\n " + getStringWithThreshold(a[k], width, nSignificantDigits, threshold, /* doNotCheck= */true));
		}
		b.append("\n]");
		return new String(b);
	}

	private static double getThreshold(final int nSignificantDigits) {
		return 5d * Math.pow(10d, -nSignificantDigits);
	}

	private final static int getMaxWidthForInt(final double a, final double threshold) {
		/** If it's not close to an integer, return "not an integer." */
		if (!Double.isFinite(a) || Math.abs(Math.round(a) - a) > threshold) {
			return -1;
		}
		/** It's close to an integer. If it's close to 0, give it width 2. */
		final int core = (int) Math.abs(Math.round(a));
		if (core == 0) {
			return 2;
		}
		/** If it's close to 10, give it 2 spaces plus the padding space. */
		final int unsigned = (int) Math.floor(Math.log10(core));
		return unsigned + (a < 0d ? 2 : 1);
	}

	private static int getMaxWidthForInt(final double[] a, final double threshold) {
		final int m = a == null ? 0 : a.length;
		int maxWidth = 0;
		for (int k = 0; k < m; ++k) {
			final double v = a[k];
			final int thisWidth = getMaxWidthForInt(v, threshold);
			if (thisWidth < 0) {
				return -1;
			}
			maxWidth = Math.max(maxWidth, thisWidth);
		}
		return maxWidth;
	}

	private static int getMaxWidthForInt(final double[][] a, final double threshold) {
		final int m = a == null ? 0 : a.length;
		int maxWidth = 0;
		for (int k = 0; k < m; ++k) {
			final double[] v = a[k];
			final int thisWidth = getMaxWidthForInt(v, threshold);
			if (thisWidth == -1) {
				return -1;
			}
			maxWidth = Math.max(maxWidth, thisWidth);
		}
		return maxWidth;
	}

	private static int getMaxWidthForInt(final double[][][] a, final double threshold) {
		final int m = a == null ? 0 : a.length;
		int maxWidth = 0;
		for (int k = 0; k < m; ++k) {
			final double[][] v = a[k];
			final int thisWidth = getMaxWidthForInt(v, threshold);
			if (thisWidth == -1) {
				return -1;
			}
			maxWidth = Math.max(maxWidth, thisWidth);
		}
		return maxWidth;
	}

	public static String getString(final ArrayList<ArrayList<double[]>> pathSet) {
		final StringBuffer b = new StringBuffer();
		if (pathSet == null) {
			b.append("NULL");
		} else {
			for (final ArrayList<double[]> path : pathSet) {
				final double[][] pathArray = path.toArray(new double[path.size()][]);
				b.append(getString(pathArray));
			}
		}
		return new String(b);
	}

	/** Only does very simple solves; a must be square and invertible. */
	public static double[] solve(final double[][] a, final double[] rhs) {
		final RealMatrix aMatrix = new Array2DRowRealMatrix(a, /* makeACopy= */true);
		final DecompositionSolver qrSolver = new QRDecomposition(aMatrix).getSolver();
		final RealVector rhsVector = new ArrayRealVector(rhs, /* makeACopy= */true);
		final RealVector xVector = qrSolver.solve(rhsVector);
		return xVector.toArray();
	}

	public static double[][] deepClone(final double[][] a) {
		if (a == null) {
			return null;
		}
		final int n = a.length;
		final double[][] clone = new double[n][];
		for (int i = 0; i < n; ++i) {
			clone[i] = Arrays.copyOf(a[i], a[i].length);
		}
		return clone;
	}

	public static double getAngle(final double[] xy0, final double[] xy1, final double[] xy2) {
		final double[] offset0 = linearCombination(1, -1, xy0, xy1);
		final double[] offset2 = linearCombination(1, -1, xy2, xy1);
		final double magnitude0 = convertToUnitLength(offset0);
		final double magnitude2 = convertToUnitLength(offset2);
		if (magnitude0 <= 0d || magnitude2 <= 0d) {
			return 0d;
		}
		return MathX0.acosX(dotProduct(offset0, offset2));
	}

	public static double[] toDouble(final float[] a) {
		final int n = a == null ? 0 : a.length;
		final double[] doubles = new double[n];
		for (int k = 0; k < n; ++k) {
			doubles[k] = a[k];
		}
		return doubles;
	}

	public static double[][] toDouble(final float[][] a) {
		final int m = a == null ? 0 : a.length;
		final double[][] doubles = new double[m][];
		for (int k = 0; k < m; ++k) {
			doubles[k] = toDouble(a[k]);
		}
		return doubles;
	}

	public static double[][][] toDouble(final float[][][] a) {
		final int m = a == null ? 0 : a.length;
		final double[][][] doubles = new double[m][][];
		for (int k = 0; k < m; ++k) {
			doubles[k] = toDouble(a[k]);
		}
		return doubles;
	}

	/** Returns a ccw hull. */
	public static double[][] getConvexHull(final double[][] input) {
		/** Compute the distinct set. */
		TreeSet<double[]> distinctsSet = new TreeSet<>(NumericalRoutines._ByAllInOrder1);
		Collections.addAll(distinctsSet, input);
		final double[][] distincts = distinctsSet.toArray(new double[0][]);
		distinctsSet = null;
		/**
		 * Find the pivot among distincts. Also, build a copy of the points we're
		 * working with, leave room for the normalized x-value, the magnitude, and set
		 * the original index. pivot is the smallest y and, among those, the smallest x.
		 */
		final int nPoints = distincts.length;
		double[] pivot = null;
		for (int k = 0; k < nPoints; ++k) {
			final double[] p = distincts[k];
			if (pivot == null || p[1] < pivot[1] || (p[1] == pivot[1] && p[0] < pivot[0])) {
				pivot = p;
			}
		}
		final double pivotX = pivot[0];
		final double pivotY = pivot[1];
		/**
		 * Build a working copy of the offsets, the keyValue for sorting, the magnitude,
		 * and the original index.
		 */
		final double[][] workingCopy = new double[nPoints][];
		for (int k = 0; k < nPoints; ++k) {
			final double[] p = distincts[k];
			if (p == pivot) {
				/**
				 * The pivot element will be sorted into the 0th position if we give it a
				 * keyValue greater than 1.
				 */
				workingCopy[k] = new double[] { 0d, 0d, 2d, 0d, k };
			} else {
				final double offsetX = p[0] - pivotX;
				final double offsetY = p[1] - pivotY;
				final double magnitude = Math.sqrt(offsetX * offsetX + offsetY * offsetY);
				final double keyValue = offsetX / magnitude;
				workingCopy[k] = new double[] { offsetX, offsetY, keyValue, magnitude, k };
			}
		}
		/**
		 * Sort workingCopy by angle to origin; all have y values that are nonnegative,
		 * and we sort by the normalized version's x value.
		 */
		Arrays.sort(workingCopy, (o0, o1) -> {
			/** Sort by descending keyValue. */
			if (o0[2] > o1[2]) {
				return -1;
			}
			if (o0[2] < o1[2]) {
				return 1;
			}
			/** Break ties by ascending magnitudes. */
			return o0[3] < o1[3] ? -1 : 1;
		});
		/** They're sorted. We can compute the hull now. */
		final Stack<double[]> hullStack = new Stack<>();
		int hullSize = 0;
		for (final double[] copy : workingCopy) {
			while (hullSize > 1) {
				final double[] recent = hullStack.pop();
				final double[] beforeThat = hullStack.peek();
				final double[] b0 = { recent[0] - beforeThat[0], recent[1] - beforeThat[1], 0 };
				final double[] b1 = { copy[0] - recent[0], copy[1] - recent[1], 0 };
				final double criticalValue = NumericalRoutines.crossProduct(b0, b1)[2];
				final boolean makeLeftTurn = criticalValue > 0;
				if (!makeLeftTurn) {
					/** Leave recent off and go check the previous one. */
					--hullSize;
					continue;
				}
				/** Put recent back on and we're ready to put newPoint on. */
				hullStack.push(recent);
				break;
			}
			hullStack.push(copy);
			++hullSize;
		}
		/** Use the indexes to return an array of the original points. */
		final double[][] returnValue = new double[hullSize][];
		int k = 0;
		for (final double[] copy : hullStack) {
			returnValue[k++] = distincts[(int) (copy[4])];
		}
		return returnValue;
	}

	/**
	 * <pre>
	 * For algorithm and definition of Ulps, see:
	 * https://randomascii.wordpress.com/2012/02/25/comparing-floating-point-numbers-2012-edition/.
	 * </pre>
	 */
	public static boolean areEqualUpToUlps(final double d0, final double d1, final int maxUlps) {
		/** If either is very close to 0, both have to be. */
		if (Math.min(Math.abs(d0), Math.abs(d1)) < Double.MIN_NORMAL) {
			return Math.max(Math.abs(d0), Math.abs(d1)) < Double.MIN_NORMAL;
		}
		final long ell1 = Double.doubleToLongBits(d0);
		final long ell2 = Double.doubleToLongBits(d1);
		return ell1 <= ell2 + maxUlps && ell2 <= ell1 + maxUlps;
	}

}