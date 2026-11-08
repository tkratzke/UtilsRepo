package com.skagit.utils.myLogger;

import java.io.File;
import java.text.SimpleDateFormat;

import com.skagit.utils.StringUtilities;

abstract public class MyLogger {

	final public static boolean _LogToConsole;
	final public static String _DefaultSuffix;
	final protected static SimpleDateFormat _MsgDateFormat = new SimpleDateFormat("MMMdd:HHmmsszzz");
	static {
		_LogToConsole = StringUtilities.getSystemProperty("Log.To.Console", /* useSpaceProxy= */false) != null;
		_DefaultSuffix = "txt";
	}

	final private static MyLogger _Singleton = new MyLoggerImpl();

	public static MyLogger CreateMyLogger(final String relativeLoggerName, final File logDir, final String dbgCoreName,
			final String outCoreName, final String wrnCoreName, final String errCoreName, final String suffix,
			final boolean append) {
		return _Singleton.createMyLogger(relativeLoggerName, logDir, dbgCoreName, outCoreName, wrnCoreName, errCoreName,
				suffix, append);
	}

	abstract protected MyLogger createMyLogger(final String relativeLoggerName, final File logDir,
			final String dgbCoreName, final String outCoreName, final String wrnErrName, final String errCoreName,
			final String suffix, final boolean append);

	abstract public void dbg(final String msg);

	public static void dbg(final MyLogger logger, final String msg) {
		(logger == null ? _Singleton : logger).dbg(msg);
	}

	abstract public void out(final String msg);

	public static void out(final MyLogger logger, final String msg) {
		(logger == null ? _Singleton : logger).out(msg);
	}

	abstract public void wrn(final String msg);

	public static void wrn(final MyLogger logger, final String msg) {
		(logger == null ? _Singleton : logger).wrn(msg);
	}

	abstract public void err(final String msg);

	public static void err(final MyLogger logger, final String msg) {
		(logger == null ? _Singleton : logger).err(msg);
	}

	public abstract void resetAppenders(final File logDir, final String dbgCoreName, final String outCoreName,
			final String wrnCoreName, final String errCoreName, final String suffix, final boolean append);
}
