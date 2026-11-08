package com.skagit.utils;

import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import com.skagit.utils.myLogger.MyLogger;

public class Ellipse2 extends Ellipse {
    /** (x/a)^2 + (y/b)^2 = 1. */
    public Ellipse2(final double a, final double b, final int nPointsPerQuadrant) {
	super(a, b, nPointsPerQuadrant);
    }

    @Override
    protected double[][] getQuadrantOfPoints(final int nPoints) {
	if (_workingB == 0) {
	    return new double[][] { new double[] { 1, 0 } };
	}
	final TreeSet<double[]> tuples = new TreeSet<>(new Comparator<double[]>() {
	    @Override
	    public int compare(final double[] tuple0, final double[] tuple1) {
		final double compareValue = tuple0[0] - tuple1[0];
		/** Sort by increasing angle (ccw). */
		return compareValue > 0 ? 1 : (compareValue < 0 ? -1 : 0);
	    }
	});
	final double[] zeroTuple = getTuple(0);
	final double[] piOver2Tuple = getTuple(Math.PI / 2);
	tuples.add(getTuple(-Math.PI / 2));
	tuples.add(zeroTuple);
	tuples.add(piOver2Tuple);
	tuples.add(getTuple(Math.PI));
	while (true) {
	    double[] losingTuple = null;
	    double sharpestAngle = 0;
	    double[] oneAgo = null;
	    double[] oneAgoXy = null;
	    double[] twoAgoXy = null;
	    for (final double[] tuple : tuples) {
		final double[] xy = new double[] { tuple[1], tuple[2] };
		if (twoAgoXy != null) {
		    final double angle = NumericalRoutines.getTurn(twoAgoXy, oneAgoXy, xy);
		    if (angle < 0) {
			System.exit(312);
		    }
		    if (losingTuple == null || angle > sharpestAngle) {
			losingTuple = oneAgo;
			sharpestAngle = angle;
		    }
		}
		oneAgo = tuple;
		twoAgoXy = oneAgoXy;
		oneAgoXy = xy;
	    }
	    tuples.remove(losingTuple);
	    final double[] nextTuple = tuples.higher(losingTuple);
	    final double[] previousTuple = tuples.lower(losingTuple);
	    tuples.add(getTuple((losingTuple[0] + nextTuple[0]) / 2));
	    tuples.add(getTuple((losingTuple[0] + previousTuple[0]) / 2));
	    /** Count how many are between pi/2 and 0 inclusive. */
	    final SortedSet<double[]> span = tuples.subSet(zeroTuple, piOver2Tuple);
	    if ((span.size() + (span.contains(zeroTuple) ? 0 : 1)) == nPoints) {
		break;
	    }
	}
	tuples.add(zeroTuple);
	final double[][] returnValue = new double[nPoints][];
	final Iterator<double[]> it = tuples.iterator();
	for (int k = 0; k < nPoints;) {
	    final double[] tuple = it.next();
	    if (0 <= tuple[0] && tuple[0] < Math.PI / 2) {
		returnValue[k++] = new double[] { tuple[1], tuple[2] };
	    }
	}
	return returnValue;
    }

    public static void main(final String[] args) {
	final MyLogger logger = null;
	final Ellipse2 ellipse = new Ellipse2(3, 1, 5);
	MyLogger.out(logger, NumericalRoutines.getString(ellipse.getFullCycle()));
    }
}
