package com.skagit.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ResourcesLister0 {
	public static class CpEntryAndRelativePath {

		final boolean _fromDirectory;
		final File _cpEntry;
		final public String _relativePath;

		public CpEntryAndRelativePath(final boolean fromDirectory, final File cpEntry, final String relativeString) {
			_fromDirectory = fromDirectory;
			_cpEntry = cpEntry;
			_relativePath = relativeString;
		}

		public String getString() {
			return String.format("From cpEntry %s(%s):  relativeString=%s.", StringUtilities.getCanonicalPath(_cpEntry),
					_fromDirectory ? "dir" : "jar", _relativePath);
		}

		@Override
		public String toString() {
			return getString();
		}
	}

	final private static String _FileSeparatorString = System.getProperties().getProperty("file.separator");
	final private static String[] _ClassPathEntries;

	static {
		final String pathSeparatorString = System.getProperties().getProperty("path.separator");
		final String classPath = StringUtilities.getSystemProperty("java.class.path", /* useSpaceProxy= */false);
		_ClassPathEntries = classPath.split(pathSeparatorString);
	}

	private static void addFromDirectory(final ArrayList<CpEntryAndRelativePath> cpEntryAndRelativePaths,
			final File cpDir, final int cpEntryDirLen, final File dirWithPckg, final Pattern pattern) {
		final String cpDirPath = StringUtilities.getCanonicalPath(cpDir);
		final int cpDirPathLen = cpDirPath.length();
		final File[] files = dirWithPckg.listFiles();
		if (files != null) {
			for (final File f : files) {
				if (f.isDirectory()) {
					addFromDirectory(cpEntryAndRelativePaths, cpDir, cpEntryDirLen, f, pattern);
				} else if (f.isFile()) {
					final String fName = f.getName();
					if (pattern.matcher(fName).matches()) {
						final String relativeString = StringUtilities.getCanonicalPath(f).substring(cpDirPathLen + 1);
						cpEntryAndRelativePaths
								.add(new CpEntryAndRelativePath(/* fromDirectory= */true, cpDir, relativeString));
					}
				}
			}
		}
	}

	private static void addFromJarFile(final ArrayList<CpEntryAndRelativePath> cpEntryAndRelativePaths,
			final File cpJarFile, final String packageStringLc, final Pattern pattern) {
		try (final ZipFile zipFile = new ZipFile(cpJarFile)) {
			Enumeration<? extends ZipEntry> enumeration;
			try {
				enumeration = zipFile.entries();
			} catch (final Exception e2) {
				return;
			}
			while (enumeration.hasMoreElements()) {
				final ZipEntry zipEntry = enumeration.nextElement();
				final String zipName = zipEntry.getName();
				if (pattern.matcher(zipName).matches()) {
					cpEntryAndRelativePaths
							.add(new CpEntryAndRelativePath(/* fromDirectory= */false, cpJarFile, zipName));
				}
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	public static ArrayList<CpEntryAndRelativePath> getCpEntryAndRelativePaths(final Package rootPckg,
			final String regExp, final boolean lookInDirectories, final boolean lookInJarFiles) {
		final ArrayList<CpEntryAndRelativePath> cpEntryAndRelativePaths = new ArrayList<>();
		final String rootPckgName = rootPckg.getName();
		final String relativeRootPckgPath = StringUtilities.cleanUpString(rootPckgName, ".",
				_FileSeparatorString.charAt(0));
		if (lookInDirectories) {
			final Pattern pattern = Pattern.compile(regExp, Pattern.CASE_INSENSITIVE);
			for (final String cpEntry : _ClassPathEntries) {
				final File cpEntryFile = new File(cpEntry);
				final int cpEntryDirLen = StringUtilities.getCanonicalPath(cpEntryFile).length();
				if (cpEntryFile.isDirectory()) {
					final File dirWithPckg = new File(cpEntryFile, relativeRootPckgPath);
					if (dirWithPckg.isDirectory()) {
						addFromDirectory(cpEntryAndRelativePaths, cpEntryFile, cpEntryDirLen, dirWithPckg, pattern);
					}
				}
			}
		}
		if (lookInJarFiles) {
			final String packageStringForJar = StringUtilities.cleanUpString(rootPckgName, ".", '/').toLowerCase();
			final String regExpForJar = packageStringForJar + "/" + regExp;
			final Pattern pattern = Pattern.compile(regExpForJar);
			for (final String cpEntry : _ClassPathEntries) {
				final File cpEntryFile = new File(cpEntry);
				if (cpEntryFile.isFile() && cpEntryFile.getName().toLowerCase().endsWith(".jar")) {
					addFromJarFile(cpEntryAndRelativePaths, cpEntryFile, packageStringForJar, pattern);
				}
			}
		}
		return cpEntryAndRelativePaths;
	}

	public static void main(final String[] args) {
		@SuppressWarnings("rawtypes")
		final Class clazz = StaticUtilities.getMyClass();
		final Package pckg = clazz.getPackage();
		getCpEntryAndRelativePaths(pckg, "Dummy.*\\.txt", true, true);
	}
}
