package com.skagit.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintStream;

import org.apache.commons.io.FilenameUtils;

public class DirsTracker {

	final private static String _LogDirName = "LogFiles";
	final protected static File _UserDir;
	final protected static File _RunDir;
	final protected static File _LogDir;

	static {
		final String userDirPath = StringUtilities.getSystemProperty("user.dir", /* useSpaceProxy= */true);
		_UserDir = new File(userDirPath);
		_RunDir = computeRunDir();
		_LogDir = new File(_RunDir, _LogDirName);
		if (!_LogDir.isDirectory()) {
			_LogDir.mkdirs();
		}
		final boolean clearLogDir = StringUtilities.getSystemProperty("ClearLogDir", /* useSpaceProxy= */true) == null;
		if (clearLogDir) {
			final FileFilter filesToDeleteFilter = file -> {
				if (file.isDirectory())
					return false;
				final String fileName = file.getName().toLowerCase();
				return fileName.toLowerCase().endsWith(".log");
			};
			final File[] filesToDelete = _LogDir.listFiles(filesToDeleteFilter);
			if (filesToDelete != null) {
				for (final File f : filesToDelete) {
					if (f.exists() && !f.delete()) {
						final String fPath = StringUtilities.getCanonicalPath(f);
						logStream(System.err, "Could not delete " + fPath);
					}
				}
			}
		}
	}

	public static void logStream(final PrintStream ps, final String s) {
		final String timeString = TimeUtilities.formatNow();
		ps.println(String.format("[%s]: %s", timeString, s));
	}

	public static File getUserDir() {
		return _UserDir;
	}

	public static File getLogDir() {
		return _LogDir;
	}

	public static File getLogFile(final String fileNameX) {
		final String baseName = FilenameUtils.getBaseName(fileNameX);

		String fileName;
		for (int k = 0;; ++k) {
			fileName = null;
			if (k == 0) {
				fileName = baseName + ".log";
			} else {
				fileName = String.format("%s(%02d).log", baseName, k);
			}
			final File f = new File(_LogDir, fileName);
			if (f.isDirectory() || f.exists()) {
				continue;
			}
			try {
				if (!f.createNewFile()) {
					continue;
				}
				final boolean success = f.canWrite();
				f.delete();
				if (!success) {
					continue;
				}
			} catch (final IOException e) {
				continue;
			}
			return f;
		}
	}

	public static File getRunDir() {
		if (_RunDir != null)
			return _RunDir;
		return computeRunDir();
	}

	private static File computeRunDir() {
		/**
		 * <pre>
		 * We compute _RunDir here, and it might be called within the static initializer.
		 * Unless overridden as a VM argument, _RunDir is set to _UserDir.
		 * </pre>
		 */
		final String userDirPath = StringUtilities.getSystemProperty("user.dir", /* useSpaceProxy= */true);
		final File userDir = new File(userDirPath);
		/** Unless overridden as a VM argument, _RunDir is set to _UserDir. */
		final String runDirPath = StringUtilities.getSystemProperty("run.dir", /* useSpaceProxy= */true);
		if (runDirPath == null)
			return userDir;
		final File runDir = new File(runDirPath);
		if (runDir.exists() && runDir.isDirectory())
			return runDir;
		return null;
	}

}
