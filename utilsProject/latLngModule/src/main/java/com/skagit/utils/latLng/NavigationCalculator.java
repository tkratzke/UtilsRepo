package com.skagit.utils.latLng;

import com.skagit.utils.navigation.LatLngArrayStatics;

/**
 * Base class for asking the questions "Where am I if I started at x and went z
 * nmi in direction p?" and "How far are x and y from each other?".
 * <p>
 * The static "build" routines take a <code>MotionType</code> as an argument,
 * and construct an appropriate type of NavigationCalculator.
 * <p>
 * In addition, there are static utilities are in this file. Utilities such as
 * finding the great circle distance between two points, converting cartesian
 * coordinates to polar and vice versa, etc.
 */
public abstract class NavigationCalculator {
	protected final double[] _latLngArray0;
	protected final long _secs0;
	protected final double _kts;
	protected final double _initialHdg;

	/**
	 * One of two questions that a NavigationCalculator can answer. Every
	 * NavigationCaclulator has a starting time, an initial direction, and a speed.
	 * Different NavigationCalculators will compute getPosition differently. For
	 * example, a "RhumblineCalculator" will compute the resulting LatLng by
	 * following a rhumbline, whereas a GreatCircleCalculator will compute the
	 * resulting LatLng by following a great circle.
	 *
	 */
	public abstract double[] getPosition(long seconds);

	public abstract double getNmiFromLatLng0(double[] latLngArray1);

	final public double getInitialHdg() {
		return _initialHdg;
	}

	/**
	 * All extensions must use this base ctor, and it is in this base class that the
	 * four fields are stored.
	 */
	protected NavigationCalculator(final double[] latLngArray0, final long secs0, final double kts,
			final double initialHdg) {
		_latLngArray0 = latLngArray0;
		_secs0 = secs0;
		_kts = kts;
		_initialHdg = LatLngArrayStatics.getInRange0_360(initialHdg);
	}
}
