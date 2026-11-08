package com.skagit.utils.randomx;

import org.w3c.dom.Element;

import com.skagit.utils.TimeUtilities;
import com.skagit.utils.lsFormatter.LsFormatter;

public class TimeDistribution {
	final private int _meanRefMins;
	final private int _plusMinusMins;

	public TimeDistribution(int meanRefMins, int plusMinusMins) {
		meanRefMins = Math.abs(meanRefMins);
		plusMinusMins = Math.abs(plusMinusMins);
		if (plusMinusMins > meanRefMins) {
			plusMinusMins = meanRefMins = (plusMinusMins + meanRefMins + 1) / 2;
		}
		_meanRefMins = meanRefMins;
		_plusMinusMins = plusMinusMins;
	}

	public boolean deepEquals(final TimeDistribution compared) {
		return _meanRefMins == compared._meanRefMins && _plusMinusMins == compared._plusMinusMins;
	}

	public int getMeanRefMins() {
		/**
		 * If this is a duration distribution, this is the mean for that duration. If it
		 * is a point-in-time distribution, this is the mean of that point in time in
		 * refMinutes.
		 */
		return _meanRefMins;
	}

	public int getPlusMinusMins() {
		return _plusMinusMins;
	}

	public static float toHours(final int minutes) {
		float hours = minutes;
		hours /= 60f;
		return hours;
	}

	public void write(final LsFormatter formatter, final Element root, final boolean formatAsADuration,
			final String tagName) {
		Element element;
		if (tagName == null) {
			element = root;
		} else {
			element = formatter.newChild(root, tagName);
		}
		if (_plusMinusMins > 0) {
			element.setAttribute("plus_minus", toHours(_plusMinusMins) + " hrs");
		}
		if (formatAsADuration) {
			element.setAttribute("duration", toHours(_meanRefMins) + " hrs");
		} else {
			element.setAttribute("dtg", TimeUtilities.formatTime(_meanRefMins * 60, false));
			element.setAttribute("dtgs", TimeUtilities.formatTime(_meanRefMins * 60, true));
		}
	}
}
