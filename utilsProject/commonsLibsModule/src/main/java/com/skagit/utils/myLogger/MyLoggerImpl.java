package com.skagit.utils.myLogger;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.TreeMap;

import com.skagit.utils.DirsTracker;
import com.skagit.utils.StaticUtilities;

public class MyLoggerImpl extends MyLogger {

	public MyLoggerImpl() {
		final Class<?> clazz = StaticUtilities.getMyClass();
		final String className = clazz.getName();
		File logDir = DirsTracker.getLogDir();
		_shortLoggerName = getShortLoggerName(className);
		try {
			logDir = logDir.getCanonicalFile();
		} catch (final IOException e) {
			_dbg = _out = _wrn = _err = null;
			return;
		}
		resetDbgOutWrnAndErr(logDir, "nullDbg", "nullOut", "nullWrn", "nullErr", MyLogger._DefaultSuffix,
				/* append= */false);
	}

	private enum Level {
		ERR, ERR_IF_DEBUG, WRN, OUT, DBG
	}

	final static String _MyClassName = StaticUtilities.getMyClass().getName();
	final private static TreeMap<String, MyLoggerImpl> _allLoggers = new TreeMap<>();

	final private String _shortLoggerName;
	private MyAppender _dbg;
	private MyAppender _out;
	private MyAppender _wrn;
	private MyAppender _err;

	@Override
	protected MyLogger createMyLogger(final String relativeLoggerName, final File logDir, final String dbgCoreName,
			final String outCoreName, final String wrnCoreName, final String errCoreName, final String suffix,
			final boolean append) {
		final String fullLoggerName = _MyClassName + '.' + relativeLoggerName;
		synchronized (_allLoggers) {
			final MyLogger oldLogger = _allLoggers.get(fullLoggerName);
			if (oldLogger != null)
				return oldLogger;
			final MyLoggerImpl logger = new MyLoggerImpl(fullLoggerName, logDir, dbgCoreName, outCoreName, wrnCoreName,
					errCoreName, suffix, append);
			_allLoggers.put(fullLoggerName, logger);
			return logger;
		}
	}

	private MyLoggerImpl(final String fullLoggerName, File logDir, final String dbgCoreName, final String outCoreName,
			final String wrnCoreName, final String errCoreName, final String suffixA, final boolean append) {
		_shortLoggerName = getShortLoggerName(fullLoggerName);
		final String suffix = (suffixA == null || suffixA.length() < 1) ? MyLogger._DefaultSuffix : suffixA;
		try {
			logDir = logDir.getCanonicalFile();
		} catch (final IOException e) {
			e.printStackTrace();
			_dbg = _out = _wrn = _err = null;
			return;
		}
		resetDbgOutWrnAndErr(logDir, dbgCoreName, outCoreName, wrnCoreName, errCoreName, suffix, append);
	}

	@Override
	public synchronized void resetAppenders(File logDir, final String dbgCoreName, final String outCoreName,
			final String wrnCoreName, final String errCoreName, final String suffixA, final boolean append) {
		final String suffix = (suffixA == null || suffixA.length() < 1) ? MyLogger._DefaultSuffix : suffixA;
		try {
			logDir = logDir.getCanonicalFile();
		} catch (final IOException e) {
			e.printStackTrace();
			_dbg = _out = _wrn = _err = null;
			return;
		}
		resetDbgOutWrnAndErr(logDir, dbgCoreName, outCoreName, wrnCoreName, errCoreName, suffix, append);
	}

	private void resetDbgOutWrnAndErr(final File logDir, final String dbgCoreName, final String outCoreName,
			final String wrnCoreName, final String errCoreName, final String suffixA, final boolean append) {
		final String suffix = (suffixA == null || suffixA.length() < 1) ? MyLogger._DefaultSuffix : suffixA;
		final StringBuilder msg = new StringBuilder();
		synchronized (this) {
			if (_dbg != null) {
				_dbg.close();
			}
			if (_out != null) {
				_out.close();
			}
			if (_wrn != null) {
				_err.close();
			}
			if (_err != null) {
				_err.close();
			}
			if (dbgCoreName != null) {
				final File dgbFile = new File(logDir, dbgCoreName + '.' + suffix);
				_dbg = new MyAppender(dgbFile, append);
			} else {
				_dbg = null;
			}
			if (outCoreName != null) {
				final File outFile = new File(logDir, outCoreName + '.' + suffix);
				_out = new MyAppender(outFile, append);
			} else {
				_out = null;
			}
			if (wrnCoreName != null) {
				final File wrnFile = new File(logDir, wrnCoreName + '.' + suffix);
				_wrn = new MyAppender(wrnFile, append);
			} else {
				_err = null;
			}
			if (errCoreName != null) {
				final File errFile = new File(logDir, errCoreName + '.' + suffix);
				_err = new MyAppender(errFile, append);
			} else {
				_err = null;
			}
			try {
				msg.append("\n\t").append(
						_dbg == null ? "NoDbg" : String.format("Dbg:%s", _dbg.getFullPathFile().getCanonicalPath()));
				msg.append("\n\t").append(
						_out == null ? "NoOut" : String.format("Out:%s", _out.getFullPathFile().getCanonicalPath()));
				msg.append("\n\t").append(
						_out == null ? "NoWrn" : String.format("Wrn:%s", _wrn.getFullPathFile().getCanonicalPath()));
				msg.append("\n\t").append(
						_err == null ? "NoErr" : String.format("Err:%s", _err.getFullPathFile().getCanonicalPath()));
			} catch (final IOException e) {
			}
		}
		out(msg.toString());
	}

	private static String getShortLoggerName(final String fullLoggerName) {
		String remainingLoggerName = fullLoggerName;
		final StringBuilder s = new StringBuilder();
		for (;;) {
			final int dot0 = remainingLoggerName.indexOf('.');
			if (dot0 < 0) {
				s.append(remainingLoggerName);
				break;
			}
			s.append(String.format("%c.", remainingLoggerName.charAt(0)));
			remainingLoggerName = remainingLoggerName.substring(dot0 + 1);
		}
		return s.toString();
	}

	private String expandMsg(final Level level, final String msg) {
		/** Don't add anything if msg is pure white space. */
		final String stripped = msg.strip();
		if (stripped.length() == 0)
			return msg;
		final String tmString = _MsgDateFormat.format(new Date());
		return String.format("%s:%s:%s %s\n", level.name(), tmString, _shortLoggerName, msg);
	}

	@Override
	public void dbg(String msg) {
		msg = expandMsg(Level.DBG, msg);
		if (_dbg != null) {
			_dbg.log(msg);
			if (_LogToConsole) {
				System.out.printf("%s", msg);
			}
		}
	}

	@Override
	public void out(String msg) {
		msg = expandMsg(Level.OUT, msg);
		if (_out != null) {
			_out.log(msg);
			if (_dbg != null) {
				_dbg.log(msg);
			}
			if (_LogToConsole) {
				System.out.printf("%s", msg);
			}
		}
	}

	@Override
	public void wrn(String msg) {
		msg = expandMsg(Level.WRN, msg);
		if (_wrn != null) {
			_wrn.log(msg);
			if (_out != null) {
				_out.log(msg);
			}
			if (_dbg != null) {
				_dbg.log(msg);
			}
			if (_LogToConsole) {
				System.err.printf("%s", msg);
			}
		}
	}

	@Override
	public void err(final String msg) {
		err(Level.ERR, msg);
	}

	public void err(final Level level, String msg) {
		msg = expandMsg(level, msg);
		if (_err != null) {
			_err.log(msg);
			if (_wrn != null) {
				_wrn.log(msg);
			}
			if (_out != null) {
				_out.log(msg);
			}
			if (_dbg != null) {
				_dbg.log(msg);
			}
			if (_LogToConsole) {
				System.err.printf("%s", msg);
			}
		}
	}

	public static void main(final String[] args) {
		final File logDir = DirsTracker.getLogDir();
		final MyLogger logger0 = CreateMyLogger("Glbl0", logDir, "dbg0.dbg", "out0.out", "wrn0.wrn", "err0.err",
				MyLogger._DefaultSuffix, /* append= */true);
		final MyLogger logger1 = CreateMyLogger("Glbl1", logDir, "dbg1.dbg", "out1.out", "wrn1.wrn", "err1.err",
				MyLogger._DefaultSuffix, /* append= */true);
		logger0.out("aaa");
		logger1.out("aaa");
	}
}
