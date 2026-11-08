package com.skagit.utils;

public class BasicDirsTracker extends DirsTracker {

	public static void dumpBasicDirs() {
		/** Print out critical directories. */
		final StringBuilder s = new StringBuilder();
		s.append("\nCurrent Working Dir: ").append(StringUtilities.getCanonicalPath(getUserDir()));
		s.append("\nRunDir: ").append(StringUtilities.getCanonicalPath(getRunDir()));
		s.append("\nLogDir: ").append(StringUtilities.getCanonicalPath(getLogDir()));
		System.out.printf("\n%s", s.toString());
	}
}
