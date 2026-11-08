package com.skagit.utils.myLogger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

class MyAppender {
	final private long _SizeLimit = 1024 * 1024 * 100;

	final private File _fullPathFile;
	final private boolean _append;

	private int _nRolled;
	private long _currentSize;
	private long _nextRollMs;
	private PrintStream _ps;

	private int _nLogs;

	MyAppender(File fullPathFile, final boolean append) {
		_append = append;
		String fullPath = null;
		try {
			fullPath = fullPathFile.getCanonicalPath();
		} catch (final IOException e) {
			e.printStackTrace();
			_fullPathFile = null;
			return;
		}
		String fullPathCore = null;
		int lastDot0 = fullPath.lastIndexOf('.');
		if (lastDot0 <= 0) {
			fullPathCore = fullPath;
			fullPath = fullPathCore + "." + MyLogger._DefaultSuffix;
			lastDot0 = fullPath.lastIndexOf('.');
			fullPathFile = new File(fullPath);
		} else {
			fullPathCore = fullPath.substring(0, lastDot0);
		}
		final String fullPathName = fullPathFile.getName();
		final int lastDot1 = fullPathName.lastIndexOf('.');
		final String coreName = fullPathName.substring(0, lastDot1);
		final String suffix = fullPathName.substring(lastDot1 + 1);
		/**
		 * If fullPathFile has no parent, we will create a directory where fullPathFile,
		 * minus any suffix, is.
		 *
		 */
		File parent = fullPathFile.getParentFile();
		if (parent == null) {
			parent = new File(fullPathCore, "logs");
			fullPathFile = new File(parent, fullPathCore);
		}
		if (parent.exists()) {
			if (!parent.isDirectory()) {
				_fullPathFile = null;
				return;
			}
		} else {
			if (!parent.mkdirs()) {
				_fullPathFile = null;
				return;
			}
		}
		_fullPathFile = new File(parent, coreName + '.' + suffix);
		_ps = null;
		_nRolled = 0;
		_currentSize = 0;
		_nextRollMs = Long.MAX_VALUE / 2;
		_nLogs = 0;
	}

	final private static int _FileFieldToRound = Calendar.AM_PM;

	void log(final String msg) {
		final long currentMs = System.currentTimeMillis();
		if (_ps == null) {
			try {
				_ps = new PrintStream(new FileOutputStream(_fullPathFile, _append));
				_currentSize = 0;
				_nextRollMs = round(currentMs, _FileFieldToRound);
			} catch (final FileNotFoundException e) {
				e.printStackTrace();
				return;
			}
		}
		if (_currentSize > _SizeLimit || currentMs > _nextRollMs) {
			_ps.close();
			String fullPath = null;
			try {
				fullPath = _fullPathFile.getCanonicalPath();
			} catch (final IOException e) {
				e.printStackTrace();
				return;
			}
			final int lastDot = fullPath.lastIndexOf('.');
			final String suffix = fullPath.substring(lastDot + 1);
			final String newPath = String.format("%s+%02d.%s", fullPath.substring(0, lastDot), ++_nRolled, suffix);
			try {
				_fullPathFile.renameTo(new File(newPath));
				_ps = new PrintStream(_fullPathFile);
			} catch (final FileNotFoundException e) {
				e.printStackTrace();
				return;
			}
			_currentSize = 0;
			_nextRollMs = round(currentMs, _FileFieldToRound);
		}
		_currentSize += msg.length();
		_ps.print(msg);
		if (++_nLogs % 5 == 0) {
			_ps.flush();
		}
	}

	final private static TimeZone _ReferenceTimeZone = TimeZone.getTimeZone("Etc/UTC");

	private static long round(final long unixMs, final int field) {
		final Calendar calendar = Calendar.getInstance(_ReferenceTimeZone);
		calendar.setTimeInMillis(unixMs);
		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH);
		int day = calendar.get(Calendar.DAY_OF_MONTH);
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		int minute = calendar.get(Calendar.MINUTE);
		int second = calendar.get(Calendar.SECOND);
		switch (field) {
		case Calendar.YEAR:
			++year;
			month = day = hour = minute = second = 0;
			break;
		case Calendar.MONTH:
			++month;
			day = hour = minute = second = 0;
			break;
		case Calendar.DAY_OF_MONTH:
			++day;
			hour = minute = second = 0;
			break;
		case Calendar.AM_PM:
			if (hour < 12) {
				hour = 12;
			} else {
				hour = 0;
				++day;
			}
			minute = second = 0;
			break;
		case Calendar.HOUR:
			++hour;
			minute = second = 0;
			break;
		case Calendar.MINUTE:
			++minute;
			second = 0;
			break;
		default:
			++second;
			break;
		}
		final GregorianCalendar roundedGregorianCalendar = new GregorianCalendar(year, month, day, hour, minute,
				second);
		final Date roundedDate = roundedGregorianCalendar.getTime();
		return roundedDate.getTime();
	}

	void close() {
		if (_ps != null) {
			_ps.close();
			_ps = null;
		}
	}

	public static void main(final String[] args) {
		final long currentMs = System.currentTimeMillis();
		final long rounded = round(currentMs, Calendar.MONTH);
		System.out.printf("\n%s", new Date(rounded).toString());
	}

	File getFullPathFile() {
		return _fullPathFile;
	}

}
