package com.skagit.utils;

import java.util.Iterator;
import java.util.TreeSet;

/**
 * This class takes the parameters of an ellipse aligned with the axes and
 * produces a ccw perimeter polygon that approximates it.
 */
public class Ellipse {
	final private double _inputA, _inputB;
	final private boolean _flip;
	protected final double _workingB;
	final private double[][] _fullCycle;

	/** (x/a)^2 + (y/b)^2 = 1. */
	public Ellipse(final double a, final double b, final int nPointsPerQuadrant) {
		/**
		 * We normalize by dividing by a, and then a disappears, as it is replaced by 1.
		 */
		_inputA = Math.abs(a);
		_inputB = Math.abs(b);
		_flip = _inputB > _inputA;
		final double workingB = _flip ? _inputA / _inputB : _inputB / _inputA;
		_workingB = Double.isNaN(workingB) ? 0 : workingB;
		_fullCycle = computeFullCycle(nPointsPerQuadrant);
	}

	private double[][] computeFullCycle(final double[][] quandrantOfPoints) {
		final int nPointsPerQuadrant = quandrantOfPoints.length;
		final int nPoints = 4 * nPointsPerQuadrant;
		final double[][] points = new double[nPoints][];
		for (int k = 0; k < nPointsPerQuadrant; ++k) {
			/** Quadrants I & III. */
			final double[] point1 = points[k] = quandrantOfPoints[k].clone();
			points[2 * nPointsPerQuadrant + k] = new double[] { -point1[0], -point1[1] };
			/** Quadrants II & IV. */
			if (k == 0) {
				points[nPointsPerQuadrant] = new double[] { 0, _workingB };
				points[3 * nPointsPerQuadrant] = new double[] { 0, -_workingB };
			} else {
				points[2 * nPointsPerQuadrant - k] = new double[] { -point1[0], point1[1] };
				points[4 * nPointsPerQuadrant - k] = new double[] { point1[0], -point1[1] };
			}
		}
		return points;
	}

	/** A tuple is the angle, the x, and the y coordinates. */
	protected double[] getTuple(final double t) {
		return new double[] { t, MathX0.cosX(t), _workingB * MathX0.sinX(t) };
	}

	/**
	 * Uses Fibonacci Optimization to determine where is the worst point (farthest
	 * away from the chord).
	 */
	protected double[][] getQuadrantOfPoints(final int nPoints) {
		if (_workingB == 0)
			return new double[][] { new double[] { 1, 0 } };
		final TreeSet<double[]> tuples = new TreeSet<>((tuple0, tuple1) -> {
			final double compareValue = tuple0[0] - tuple1[0];
			return compareValue > 0 ? 1 : (compareValue < 0 ? -1 : 0);
		});
		tuples.add(getTuple(0));
		tuples.add(getTuple(Math.PI / 2.0));
		while (tuples.size() - 1 < nPoints) {
			double[] worstPair = null;
			double[] lastTuple = null;
			for (final double[] tuple : tuples) {
				if (lastTuple != null) {
					final double[] thisPair = getWorstAngle(lastTuple[0], tuple[0]);
					if (worstPair == null || thisPair[1] > worstPair[1]) {
						worstPair = thisPair;
					}
				}
				lastTuple = tuple;
			}
			tuples.add(getTuple(worstPair[0]));
		}
		final double[][] returnValue = new double[nPoints][];
		final Iterator<double[]> it = tuples.iterator();
		for (int k = 0; k < nPoints; ++k) {
			final double[] tuple = it.next();
			returnValue[k] = new double[] { tuple[1], tuple[2] };
		}
		return returnValue;
	}

	private double[] getWorstAngle(final double t1, final double t2) {
		final double[] x1 = getTuple(t1);
		final double[] x2 = getTuple(t2);
		final double[] d = { x2[1] - x1[1], x2[2] - x1[2] };
		NumericalRoutines.convertToUnitLength(d);
		final double oldY = d[1];
		d[1] = -d[0];
		d[0] = oldY;
		final FibOptn.Function f = new FibOptn.Function() {
			@Override
			public double evaluate(final double t) {
				if (t == t1 || t == t2)
					return 0;
				final double[] tuple = getTuple(t);
				final double[] dotter = { tuple[1] - x1[1], tuple[2] - x1[2] };
				final double dotProduct = NumericalRoutines.dotProduct(d, dotter);
				return Math.max(0, dotProduct);
			}
		};
		final FibOptn fibOptn = new FibOptn(f, t1, t2);
		return fibOptn.getMaximumPair();
	}

	/** The reason for the class. */
	private double[][] computeFullCycle(final int nPointsPerQuadrant) {
		final double[][] quadrantOfPoints;
		if (_workingB == 1) {
			quadrantOfPoints = new double[nPointsPerQuadrant][];
			for (int k = 0; k < nPointsPerQuadrant; ++k) {
				final double angle = Math.PI / 2 * k / nPointsPerQuadrant;
				quadrantOfPoints[k] = new double[] { MathX0.cosX(angle), MathX0.sinX(angle) };
			}
		} else {
			quadrantOfPoints = getQuadrantOfPoints(nPointsPerQuadrant);
		}
		final double[][] perimeterPoints = computeFullCycle(quadrantOfPoints);
		final double scaleFactor = _flip ? _inputB : _inputA;
		for (final double[] point : perimeterPoints) {
			point[0] *= scaleFactor;
			point[1] *= scaleFactor;
		}
		if (_flip) {
			final int nPoints = perimeterPoints.length;
			for (int k = 0; k < nPoints; ++k) {
				final double[] point = perimeterPoints[k];
				final double temp = point[0];
				point[0] = point[1];
				point[1] = -temp;
			}
		}
		return perimeterPoints;
	}

	public double[][] getFullCycle() {
		return _fullCycle;
	}
}
