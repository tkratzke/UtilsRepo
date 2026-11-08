package com.skagit.utils.latLng;

import com.skagit.utils.MathX0;

class TangentCylinderCalculator extends NavigationCalculator {
	final private TangentCylinder _tc;
	final private double _eastOffsetPerSecond, _northOffsetPerSecond;

	TangentCylinderCalculator(final LatLng latLng0, final long secsStart, final double hdg, final double kts) {
		super(secsStart, latLng0, kts);
		_tc = TangentCylinder.getTangentCylinder(latLng0);
		final TangentCylinder.FlatLatLng flatStartLatLng = _tc.getCentralFlatLatLng();
		final double rPerSec = _kts / 3600d * _tc.getNmiToR();
		final double[] xyNorth = flatStartLatLng.getUnitVectorTowardsNorthPole();
		final double[] xyEast = { xyNorth[1], -xyNorth[0] };
		final double courseCcwRadsFromEast = Math.toRadians(90d - hdg);
		final double c = MathX0.cosX(courseCcwRadsFromEast);
		final double s = MathX0.sinX(courseCcwRadsFromEast);
		final double ux = c * xyEast[0] + s * xyNorth[0];
		final double uy = c * xyEast[1] + s * xyNorth[1];
		_eastOffsetPerSecond = ux * rPerSec;
		_northOffsetPerSecond = uy * rPerSec;
		_initialHdg = hdg;
	}

	public static double getInitialHeading(final LatLng startLatLng, final LatLng destinationLatLng) {
		final TangentCylinder tangentCylinder = TangentCylinder.getTangentCylinder(startLatLng);
		final TangentCylinder.FlatLatLng _flatDestinationLatLng = tangentCylinder
				.convertToMyFlatLatLng(destinationLatLng);
		return ModFunctions.roundToLattice0_360L(90d - Math.toDegrees(
				MathX0.atan2X(_flatDestinationLatLng.getNorthOffsetR(), _flatDestinationLatLng.getEastOffsetR())));
	}

	TangentCylinderCalculator(final LatLng latLng0, final long startSecs, final LatLng latLng1, final double kts) {
		super(startSecs, latLng0, kts);
		_tc = TangentCylinder.getTangentCylinder(latLng0);
		final TangentCylinder.FlatLatLng flatStartLatLng = _tc.getCentralFlatLatLng();
		final double speedR_PerSec = _kts / 3600d * _tc.getNmiToR();
		final TangentCylinder.FlatLatLng flatLatLng1 = _tc.convertToMyFlatLatLng(latLng1);
		final double eastDelta = flatLatLng1.getEastOffsetR() - flatStartLatLng.getEastOffsetR();
		final double northDelta = flatLatLng1.getNorthOffsetR() - flatStartLatLng.getNorthOffsetR();
		final double courseCcwRadsFromEast = MathX0.atan2X(northDelta, eastDelta);
		_eastOffsetPerSecond = MathX0.cosX(courseCcwRadsFromEast) * speedR_PerSec;
		_northOffsetPerSecond = MathX0.sinX(courseCcwRadsFromEast) * speedR_PerSec;
		_initialHdg = ModFunctions.getInRange0_360(90d - Math.toDegrees(courseCcwRadsFromEast));
	}

	@Override
	public LatLng getPosition(final long timeSecs) {
		final long elapsedTimeSecs = timeSecs - _secs0;
		final double eastOffset = _eastOffsetPerSecond * elapsedTimeSecs;
		final double northOffset = _northOffsetPerSecond * elapsedTimeSecs;
		return _tc.new FlatLatLng(eastOffset, northOffset);
	}

	@Override
	public double getNmiFromLatLng0(final LatLng latLng1) {
		final TangentCylinder.FlatLatLng flatDestinationLatLng = _tc.convertToMyFlatLatLng(latLng1);
		final double eastDelta = flatDestinationLatLng.getEastOffsetR();
		final double northDelta = flatDestinationLatLng.getNorthOffsetR();
		return Math.sqrt(eastDelta * eastDelta + northDelta * northDelta) / _tc.getNmiToR();
	}

	@Override
	public double getInitialHdg() {
		return _initialHdg;
	}
}
