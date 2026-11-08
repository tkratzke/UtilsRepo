package com.skagit.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.SimpleTimeZone;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;

import com.skagit.utils.lsFormatter.LsFormatter;

public class TimeUtilities {
    public static long _UnsetTime = Long.MIN_VALUE;
    final private static long _RefMillis;
    final private static TimeZone _ReferenceTimeZone;
    final private static SortedMap<String, Integer> _monthMap = new TreeMap<>();
    final private static SortedMap<Integer, String> _ReverseMonthMap = new TreeMap<>();

    static {
	_monthMap.put("JAN", Calendar.JANUARY);
	_monthMap.put("FEB", Calendar.FEBRUARY);
	_monthMap.put("MAR", Calendar.MARCH);
	_monthMap.put("APR", Calendar.APRIL);
	_monthMap.put("MAY", Calendar.MAY);
	_monthMap.put("JUN", Calendar.JUNE);
	_monthMap.put("JUL", Calendar.JULY);
	_monthMap.put("AUG", Calendar.AUGUST);
	_monthMap.put("SEP", Calendar.SEPTEMBER);
	_monthMap.put("OCT", Calendar.OCTOBER);
	_monthMap.put("NOV", Calendar.NOVEMBER);
	_monthMap.put("DEC", Calendar.DECEMBER);
	_ReverseMonthMap.put(Calendar.JANUARY, "JAN");
	_ReverseMonthMap.put(Calendar.FEBRUARY, "FEB");
	_ReverseMonthMap.put(Calendar.MARCH, "MAR");
	_ReverseMonthMap.put(Calendar.APRIL, "APR");
	_ReverseMonthMap.put(Calendar.MAY, "MAY");
	_ReverseMonthMap.put(Calendar.JUNE, "JUN");
	_ReverseMonthMap.put(Calendar.JULY, "JUL");
	_ReverseMonthMap.put(Calendar.AUGUST, "AUG");
	_ReverseMonthMap.put(Calendar.SEPTEMBER, "SEP");
	_ReverseMonthMap.put(Calendar.OCTOBER, "OCT");
	_ReverseMonthMap.put(Calendar.NOVEMBER, "NOV");
	_ReverseMonthMap.put(Calendar.DECEMBER, "DEC");
	_ReferenceTimeZone = TimeZone.getTimeZone("Etc/UTC");
	final Calendar referenceCalendar = Calendar.getInstance(_ReferenceTimeZone);
	referenceCalendar.set(Calendar.MILLISECOND, 0);
	referenceCalendar.set(Calendar.SECOND, 0);
	referenceCalendar.set(Calendar.MINUTE, 0);
	referenceCalendar.set(Calendar.HOUR_OF_DAY, 0);
	referenceCalendar.set(Calendar.MONTH, Calendar.JANUARY);
	referenceCalendar.set(Calendar.DATE, 1);
	referenceCalendar.set(Calendar.YEAR, 2000);
	_RefMillis = referenceCalendar.getTimeInMillis();
    }

    public static int[] _OrdinalToMonth = new int[] { -1, Calendar.JANUARY, Calendar.FEBRUARY, Calendar.MARCH,
	    Calendar.APRIL, Calendar.MAY, Calendar.JUNE, Calendar.JULY, Calendar.AUGUST, Calendar.SEPTEMBER,
	    Calendar.OCTOBER, Calendar.NOVEMBER, Calendar.DECEMBER };

    /** refSecs is from 1/1/2000. */
    public static String formatTime(final long refSecs, final boolean includeSecs) {
	return formatTime(refSecs, includeSecs, false);
    }

    /** refSecs is from 1/1/2000. */
    public static String formatTime(final long refSecs, final boolean includeSecs, final boolean trimAtZ) {
	try {
	    final Calendar calendar = Calendar.getInstance(_ReferenceTimeZone);
	    long timeInMillis = _RefMillis + (refSecs * 1000L);
	    calendar.setTimeInMillis(timeInMillis);
	    /** If we do not include seconds, we have to round. */
	    final int second = calendar.get(Calendar.SECOND);
	    if (!includeSecs && second >= 30) {
		timeInMillis += 1000L * (60 - second);
		calendar.setTimeInMillis(timeInMillis);
	    }
	    final int day = calendar.get(Calendar.DAY_OF_MONTH);
	    final int hour = calendar.get(Calendar.HOUR_OF_DAY);
	    final int minute = calendar.get(Calendar.MINUTE);
	    final int year = calendar.get(Calendar.YEAR);
	    String s = String.format("%02d%02d%02d", day, hour, minute);
	    if (includeSecs) {
		s += String.format("%02d", second);
	    }
	    s += "Z";
	    if (!trimAtZ) {
		final String monthString = _ReverseMonthMap.get(calendar.get(Calendar.MONTH));
		s += String.format(" %s %04d", monthString, year);
	    }
	    return s;
	} catch (final Exception e) {
	    return "Bad RefSecs";
	}
    }

    public static String shortFormatTime(final long refSecs) {
	final Calendar calendar = Calendar.getInstance(_ReferenceTimeZone);
	final long timeInMillis = _RefMillis + (refSecs * 1000L);
	calendar.setTimeInMillis(timeInMillis);
	final int second = calendar.get(Calendar.SECOND);
	final int day = calendar.get(Calendar.DAY_OF_MONTH);
	final int hour = calendar.get(Calendar.HOUR_OF_DAY);
	final int minute = calendar.get(Calendar.MINUTE);
	String s = String.format("%02d%02d%02d", day, hour, minute);
	s += String.format("%02d", second);
	return s;
    }

    /**
     * refMills is from 1/1/2000. Example of format: 02-FEB-03 03.15.00.000 AM
     * 0123456789 123456789 1234
     *
     * Note: Oracle appears to use 12 instead of 0 for noon and midnight
     */
    public static String formatToOracleTime(final long refMillis, final boolean inMillis) {
	final Calendar calendar = Calendar.getInstance(_ReferenceTimeZone);
	final long timeInMillis;
	if (inMillis) {
	    timeInMillis = _RefMillis + refMillis;
	} else {
	    timeInMillis = _RefMillis + (1000L * refMillis);
	}
	calendar.setTimeInMillis(timeInMillis);
	final int year = calendar.get(Calendar.YEAR);
	final int month = calendar.get(Calendar.MONTH);
	final int day = calendar.get(Calendar.DAY_OF_MONTH);
	int hour = calendar.get(Calendar.HOUR);
	final int am_pm = calendar.get(Calendar.AM_PM);
	final int minute = calendar.get(Calendar.MINUTE);
	final int second = calendar.get(Calendar.SECOND);
	final String yearStr = LsFormatter.StandardFormat(year).substring(2, 4);
	final String monthStr = _ReverseMonthMap.get(month);
	final String suffix = am_pm == Calendar.AM ? "AM" : "PM";
	if (hour == 0) {
	    hour = 12;
	}
	return String.format("%02d-%s-%s %02d.%02d.%02d.000 %s", day, monthStr, yearStr, hour, minute, second, suffix);
    }

    public static String unixSecsToString(final long unixSecs, final boolean includeSecs) {
	final long refSecs = convertToRefSecs(unixSecs);
	return formatTime(refSecs, includeSecs);
    }

    /** refSecs is from 1/1/2000. */
    public static int[] getTimeArray(final long refSecs) {
	final Calendar calendar = Calendar.getInstance(_ReferenceTimeZone);
	final long timeInMillis = _RefMillis + (refSecs * 1000L);
	calendar.setTimeInMillis(timeInMillis);
	final int year = calendar.get(Calendar.YEAR);
	final int month = calendar.get(Calendar.MONTH) + 1; // January is zero!
	final int day = calendar.get(Calendar.DAY_OF_MONTH);
	final int hour = calendar.get(Calendar.HOUR_OF_DAY);
	final int minute = calendar.get(Calendar.MINUTE);
	final int second = calendar.get(Calendar.SECOND);
	return new int[] { year, month, day, hour, minute, second };
    }

    public static int[] getUnixTimeArray(final long unixSecs) {
	final Calendar calendar = Calendar.getInstance(_ReferenceTimeZone);
	final long timeInMillis = unixSecs * 1000L;
	calendar.setTimeInMillis(timeInMillis);
	final int year = calendar.get(Calendar.YEAR);
	final int month = calendar.get(Calendar.MONTH) + 1; // January is zero!
	final int day = calendar.get(Calendar.DAY_OF_MONTH);
	final int hour = calendar.get(Calendar.HOUR_OF_DAY);
	final int minute = calendar.get(Calendar.MINUTE);
	final int second = calendar.get(Calendar.SECOND);
	return new int[] { year, month, day, hour, minute, second };
    }

    public static String[] decomposeToStrings(final long unixSecs) {
	final Calendar calendar = Calendar.getInstance(_ReferenceTimeZone);
	final long timeInMillis = unixSecs * 1000L;
	calendar.setTimeInMillis(timeInMillis);
	final String yearString = "" + calendar.get(Calendar.YEAR);
	final String monthString = _ReverseMonthMap.get(calendar.get(Calendar.MONTH));
	final String dayString = String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH));
	final String hourString = String.format("%02d", calendar.get(Calendar.HOUR_OF_DAY));
	final String minuteString = String.format("%02d", calendar.get(Calendar.MINUTE));
	final String secsString = String.format("%02d", calendar.get(Calendar.SECOND));
	return new String[] { yearString, monthString, dayString, hourString, minuteString, secsString };
    }

    public static long getUnixSecs(final int year, final int month, final int day, final int hours, final int minutes,
	    final int secs) {
	final Calendar calendar = Calendar.getInstance(_ReferenceTimeZone);
	calendar.set(Calendar.MILLISECOND, 0);
	calendar.set(Calendar.SECOND, secs);
	calendar.set(Calendar.MINUTE, minutes);
	calendar.set(Calendar.HOUR_OF_DAY, hours);
	calendar.set(Calendar.MONTH, month);
	calendar.set(Calendar.DATE, day);
	calendar.set(Calendar.YEAR, year);
	return calendar.getTimeInMillis() / 1000L;
    }

    /** refSecs is from 1/1/2000. */
    public static int getYear(final long refSecs) {
	final Calendar calendar = Calendar.getInstance(_ReferenceTimeZone);
	final long timeInMillis = _RefMillis + (refSecs * 1000L);
	calendar.setTimeInMillis(timeInMillis);
	return calendar.get(Calendar.YEAR);
    }

    public static int getMonth(final String monthString) {
	final Integer month = _monthMap.get(monthString);
	return month;
    }

    /** returns _UnsetTime if something is wrong with the string. */
    public static long dtgToRefSecs(final String dtg) {
	final int dtgLength = dtg == null ? 0 : dtg.length();
	if (dtgLength == 0) {
	    return _UnsetTime;
	}
	final int firstSpace = dtg.indexOf(' ');
	try {
	    int secs = -1;
	    if (firstSpace == 7) {
		if (dtgLength != 14 && dtgLength != 16) {
		    return _UnsetTime;
		}
		secs = 0;
	    } else if (firstSpace == 9) {
		if (dtgLength != 16 && dtgLength != 18) {
		    return _UnsetTime;
		}
		secs = Integer.parseInt(dtg.substring(6, 8));
	    } else {
		return _UnsetTime;
	    }
	    final int day = Integer.parseInt(dtg.substring(0, 2));
	    final int hours = Integer.parseInt(dtg.substring(2, 4));
	    final int minutes = Integer.parseInt(dtg.substring(4, 6));
	    final String monthString = dtg.substring(firstSpace + 1, firstSpace + 4).toUpperCase();
	    int month = 0;
	    final int yearStartsAt = firstSpace + 5;
	    int year = Integer.parseInt(dtg.substring(yearStartsAt, dtgLength));
	    if (dtgLength - yearStartsAt == 2) {
		/**
		 * If it were a 2-digit year, we assume that the century is 2000.
		 */
		year += 2000;
	    }
	    try {
		month = (_monthMap.get(monthString));
	    } catch (final Exception ignored) {
		return _UnsetTime;
	    }
	    return getRefSecs(year, month, day, hours, minutes, secs);
	} catch (final NumberFormatException ignored) {
	    return _UnsetTime;
	}
    }

    /** returns # of seconds since 1/1/2000. */
    /** NOTE: for 'month' remember that January == 0 (zero) !! */
    public static long getRefSecs(final int year, final int month, final int day, final int hours, final int minutes,
	    final int secs) {
	final Calendar calendar = Calendar.getInstance(_ReferenceTimeZone);
	calendar.set(Calendar.MILLISECOND, 0);
	calendar.set(Calendar.SECOND, secs);
	calendar.set(Calendar.MINUTE, minutes);
	calendar.set(Calendar.HOUR_OF_DAY, hours);
	calendar.set(Calendar.MONTH, month);
	calendar.set(Calendar.DATE, day);
	calendar.set(Calendar.YEAR, year);
	return getRefSecs(calendar);
    }

    /** returns # of seconds since 1/1/2000. */
    public static long getRefSecs(final int year, final int dayInYear, final int hours, final int minutes,
	    final int secs) {
	final Calendar calendar = Calendar.getInstance(_ReferenceTimeZone);
	calendar.set(Calendar.YEAR, year);
	calendar.set(Calendar.MILLISECOND, 0);
	calendar.set(Calendar.SECOND, secs);
	calendar.set(Calendar.MINUTE, minutes);
	calendar.set(Calendar.HOUR_OF_DAY, hours);
	calendar.set(Calendar.DAY_OF_YEAR, dayInYear);
	return getRefSecs(calendar);
    }

    /** Returns # of seconds since 1/1/2000. */
    public static long getRefSecs(final Calendar calendar) {
	return (calendar.getTimeInMillis() - _RefMillis) / 1000L;
    }

    public static TimeZone getReferenceTimeZone() {
	return _ReferenceTimeZone;
    }

    public static long convertToRefSecs(final long unixSecs) {
	final Calendar calendar = Calendar.getInstance();
	calendar.setTimeInMillis(1000L * unixSecs);
	return (calendar.getTimeInMillis() - _RefMillis) / 1000L;
    }

    public static long convertToUnixSecs(final long refSecs) {
	final long unixMilliSecs = refSecs * 1000L + _RefMillis;
	final Calendar calendar = Calendar.getInstance();
	calendar.setTimeInMillis(unixMilliSecs);
	return (calendar.getTimeInMillis()) / 1000L;
    }

    /**
     * refTimeInMillis is # of millis since 1/1/2000. Returns # of millis since
     * 1/1/70.
     */
    public static long getUnixTimeInMillis(final long refMillis) {
	return _RefMillis + refMillis;
    }

    public static long[] convertToUnixSecs(final long[] refSecsS) {
	final int n = refSecsS == null ? 0 : refSecsS.length;
	final long[] unixSecsS = new long[n];
	final int offsetSecs = (int) (_RefMillis / 1000);
	for (int k = 0; k < n; ++k) {
	    unixSecsS[k] = offsetSecs + refSecsS[k];
	}
	return unixSecsS;
    }

    /** No reference times here; just manipulate dates. */
    final private static int[] _daysIn = new int[] { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };
    final private static int[] _daysBefore;

    static {
	_daysBefore = new int[12];
	_daysBefore[0] = 0;
	for (int i = 1; i < 12; ++i) {
	    _daysBefore[i] = _daysBefore[i - 1] + _daysIn[i - 1];
	}
    }

    private static boolean isLeapYear(final int y) {
	return y % 4 == 0 && (y % 100 != 0 || y % 400 == 0);
    }

    private static int leapDaysBefore(final int y, final int m) {
	final int n = y / 4 - y / 100 + y / 400;
	/**
	 * In January and February of a leap year, we may have counted too many leap
	 * days. Note that there is no leap day BEFORE 2/29 in a leap year, / but there
	 * is one BEFORE 3/1.
	 */
	if (!isLeapYear(y)) {
	    return n;
	}
	return n - (m <= 2 ? 1 : 0);
    }

    public static int minutesFromTo(final int y1, final int m1, final int d1, final int h1, final int mm1, final int y2,
	    final int m2, final int d2, final int h2, final int mm2) {
	final int nDaysFromTo = daysFromTo(y1, m1, d1, y2, m2, d2);
	return (nDaysFromTo * 24 + h2 - h1) * 60 + mm2 - mm1;
    }

    public static Date addDhms(final long unixSecs, final int d, final int h, final int mm, final int s) {
	final Calendar calendar = Calendar.getInstance(new SimpleTimeZone(0, "UTC"));
	calendar.setTimeInMillis(unixSecs * 1000);
	calendar.add(Calendar.DATE, d);
	calendar.add(Calendar.HOUR, h);
	calendar.add(Calendar.MINUTE, mm);
	calendar.add(Calendar.SECOND, s);
	return new Date(calendar.getTimeInMillis());
    }

    public static int daysFromTo(final int y1, final int m1, final int d1, final int y2, final int m2, final int d2) {
	return (y2 - y1) * 365 + //
		(leapDaysBefore(y2, m2) - leapDaysBefore(y1, m1)) + //
		(_daysBefore[m2] - _daysBefore[m1]) + //
		(d2 - d1);
    }

    public static int daysFromTo(final Date date1, final Date date2) {
	final Calendar calendar1 = Calendar.getInstance(new SimpleTimeZone(0, "UTC"));
	calendar1.setTime(date1);
	final int y1 = calendar1.get(Calendar.YEAR);
	final int m1 = calendar1.get(Calendar.MONTH);
	final int d1 = calendar1.get(Calendar.DAY_OF_MONTH);
	final Calendar calendar2 = Calendar.getInstance(new SimpleTimeZone(0, "UTC"));
	calendar2.setTime(date2);
	final int y2 = calendar2.get(Calendar.YEAR);
	final int m2 = calendar2.get(Calendar.MONTH);
	final int d2 = calendar2.get(Calendar.DAY_OF_MONTH);
	return daysFromTo(y1, m1, d1, y2, m2, d2);
    }

    public static long roundDownToDayBoundary(final long refSecs) {
	final int[] timeArray = getTimeArray(refSecs);
	/**
	 * getTimeArray returns new int[] { year, month, day, hour, minute, second };
	 */
	final int year = timeArray[0];
	/**
	 * We are getting ready to call getRefSecs. That one expects January == 0. The
	 * return from getTimeArray has January == 1.
	 */
	final int month = timeArray[1] - 1;
	final int day = timeArray[2];
	return getRefSecs(year, month, day, 0, 0, 0);
    }

    public static long unixRoundDownToDayBoundary(final long unixSecs) {
	final int[] timeArray = getUnixTimeArray(unixSecs);
	final int year = timeArray[0];
	/**
	 * We are getting ready to call getRefSecs. That one expects January == 0. The
	 * return from getTimeArray has January == 1.
	 */
	final int month = timeArray[1] - 1;
	final int day = timeArray[2];
	return getUnixSecs(year, month, day, 0, 0, 0);
    }

    public static String formatNow() {
	final String nowString = new SimpleDateFormat("M/d/yy h:mm:ss a").format(new Date(System.currentTimeMillis()));
	return nowString;
    }

    /** 2016Aug12-0013Z */
    public static String formatNowForFileName() {
	final Calendar c = Calendar.getInstance(TimeZone.getTimeZone("Etc/UTC"));
	final int year = c.get(Calendar.YEAR);
	final String monthString = _ReverseMonthMap.get(c.get(Calendar.MONTH));
	final int day = c.get(Calendar.DAY_OF_MONTH);
	final int hour = c.get(Calendar.HOUR_OF_DAY);
	final int minute = c.get(Calendar.MINUTE);
	final String nowString = String.format("%04d%s%02d-%02d%02dZ", year, monthString, day, hour, minute);
	return nowString;
    }

    public static long getNowRefSecs() {
	return convertToRefSecs(System.currentTimeMillis() / 1000);
    }

    public static String durationInSecondsToMinsSecsString(final int durationInSeconds, final int nDigitsForMinutes) {
	final int minutes = durationInSeconds / 60;
	final int seconds = durationInSeconds - (minutes * 60);
	final String format = "%0" + nDigitsForMinutes + "d:%02d";
	return String.format(format, minutes, seconds);
    }

    public static void main(final String[] args) {
	final long refSecs = getNowRefSecs();
	final long unixSecs = convertToUnixSecs(refSecs);
	final long refSecs2 = convertToRefSecs(unixSecs);
	System.out.printf("\nref0[%d]\nunix[%d]\nref1[%d]", refSecs, unixSecs, refSecs2);
    }

}
