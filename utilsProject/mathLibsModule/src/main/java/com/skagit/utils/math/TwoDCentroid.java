package com.skagit.utils.math;

public class TwoDCentroid {

	public static double[] getCentroid(final double[][] points) {
		final int nPoints = points.length;

		double area = 0d;
		double cumX = 0d;
		double cumY = 0d;

		for (int i = 0; i < nPoints; i++) {
			final double[] point0 = points[i];
			final double[] point1 = points[(i + 1) % nPoints];
			final double cross = point0[0] * point1[1] - point1[0] * point0[1];
			area += cross;
			cumX += (point0[0] + point1[0]) * cross;
			cumY += (point0[1] + point1[1]) * cross;
		}
		area *= 0.5;
		if (area == 0d) {
			throw new ArithmeticException("Polygon area is zero — centroid undefined.");
		}
		cumX /= (6d * area);
		cumY /= (6d * area);
		return new double[] { cumX, cumY };
	}

}
