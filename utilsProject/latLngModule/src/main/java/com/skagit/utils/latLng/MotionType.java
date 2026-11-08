package com.skagit.utils.latLng;

public enum MotionType {
	GREAT_CIRCLE("GC"), RHUMBLINE("RL"), TANGENT_CYLINDER("TC"), MID_LAT("ML");

	final private String _id;

	MotionType(final String id) {
		_id = id;
	}

	public String getId() {
		return _id;
	}

	public static MotionType get(final String id) {
		if (id == null || id.length() == 0) {
			return GREAT_CIRCLE;
		}
		final char idChar = Character.toUpperCase(id.charAt(0));
		switch (idChar) {
		case 'G':
			return GREAT_CIRCLE;
		case 'R':
			return RHUMBLINE;
		case 'T':
			return TANGENT_CYLINDER;
		case 'M':
			return MID_LAT;
		default:
			return GREAT_CIRCLE;
		}
	}

	final public static MotionType[] _MotionTypeValues = MotionType.values();
}
