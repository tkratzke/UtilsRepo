package com.skagit.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.skagit.utils.myLogger.MyLogger;

public class ResourcesLister {
	final private static String _FileSeparatorString = System.getProperties().getProperty("file.separator");
	final private static String[] _ClassPathEntries;
	static {
		final String pathSeparatorString = System.getProperties().getProperty("path.separator");
		final String classPath = StringUtilities.getSystemProperty("java.class.path", /* useSpaceProxy= */false);
		_ClassPathEntries = classPath.split(pathSeparatorString);
	}

	private static void addDirectories(final ArrayList<File> listOfPossibleDirectories, final String mustMatch,
			final File directory) {
		final File[] files = directory.listFiles();
		for (final File file : files) {
			if (file.isDirectory()) {
				final String fPath = StringUtilities.getCanonicalPath(file);
				if (fPath.startsWith(mustMatch)) {
					listOfPossibleDirectories.add(file);
					addDirectories(listOfPossibleDirectories, mustMatch, file);
				} else if (mustMatch.startsWith(fPath)) {
					addDirectories(listOfPossibleDirectories, mustMatch, file);
				}
			}
		}
	}

	public static File getDirOfResource(final Package callingPackage, final String inputResourceName) {
		final String fullPackageName = callingPackage.getName();
		/** Only look at the directories of the path. */
		final String packageStringForDirectory = StringUtilities.cleanUpString(fullPackageName, ".",
				_FileSeparatorString.charAt(0));
		final TreeMap<String, ArrayList<File>> allDirectories = new TreeMap<>();
		for (final String classPathEntry : _ClassPathEntries) {
			final File file = new File(classPathEntry);
			if (!file.exists()) {
				continue;
			}
			if (file.isDirectory()) {
				final String mustMatch = classPathEntry + _FileSeparatorString + packageStringForDirectory
						+ _FileSeparatorString;
				final ArrayList<File> dirFiles = new ArrayList<>();
				addDirectories(dirFiles, mustMatch, file);
				allDirectories.put(mustMatch, dirFiles);
			}
		}
		final TreeSet<String> triedAbsPaths = new TreeSet<>();
		for (final Map.Entry<String, ArrayList<File>> entry : allDirectories.entrySet()) {
			final int startingIndexOfKeyPart = entry.getKey().length();
			for (final File directory : entry.getValue()) {
				for (final File f : directory.listFiles()) {
					if (f.isFile()) {
						final String fPath = StringUtilities.getCanonicalPath(f);
						if (!fPath.endsWith(".class")) {
							if (triedAbsPaths.add(fPath)) {
								/**
								 * Strip off directory's abs path and check the rest for a match.
								 */
								final String resourceName = fPath.substring(startingIndexOfKeyPart);
								if (resourceName.equalsIgnoreCase(inputResourceName)) {
									return directory;
								}
							}
						}
					}
				}
			}
		}
		return null;
	}

	public static class ResourceNamePlus {
		final public String _resourceName;
		final public long _size;

		private ResourceNamePlus(final String resourceName, final long size) {
			_resourceName = resourceName;
			_size = size;
		}

		public String getString() {
			return _resourceName + " size[" + _size + "]";
		}
	}

	public static ArrayList<ResourceNamePlus> getRelativeResourceNamePluses(final MyLogger logger,
			final Package callingPackage, final Pattern pattern, final boolean lookInDirectories,
			final boolean lookInJarFiles) {
		final ArrayList<ResourceNamePlus> relativeResourceNameReturns = new ArrayList<>();
		final String fullPackageName = callingPackage.getName();
		/** Start with the directories. */
		if (lookInDirectories) {
			final String packageStringForDirectory = StringUtilities.cleanUpString(fullPackageName, ".",
					_FileSeparatorString.charAt(0));
			final TreeMap<String, ArrayList<File>> allDirectories = new TreeMap<>();
			for (final String classPathEntry : _ClassPathEntries) {
				final File file = new File(classPathEntry);
				if (!file.exists()) {
					continue;
				}
				if (file.isDirectory()) {
					final String mustMatch = classPathEntry + _FileSeparatorString + packageStringForDirectory
							+ _FileSeparatorString;
					final ArrayList<File> dirFiles = new ArrayList<>();
					addDirectories(dirFiles, mustMatch, file);
					allDirectories.put(mustMatch, dirFiles);
				}
			}
			final TreeSet<String> triedAbsPaths = new TreeSet<>();
			for (final Map.Entry<String, ArrayList<File>> entry : allDirectories.entrySet()) {
				final int startingIndexOfKeyPart = entry.getKey().length();
				for (final File directory : entry.getValue()) {
					final File[] files = directory.listFiles();
					for (final File f : files) {
						if (f.isFile()) {
							final String fPath = StringUtilities.getCanonicalPath(f);
							if (!fPath.endsWith(".class")) {
								if (triedAbsPaths.add(fPath)) {
									/**
									 * Strip off directory's abs path and check the rest for a match.
									 */
									final String resourceName = fPath.substring(startingIndexOfKeyPart);
									if (pattern.matcher(resourceName).matches()) {
										relativeResourceNameReturns.add(new ResourceNamePlus(resourceName, f.length()));
									}
								}
							}
						}
					}
				}
			}
		}
		if (lookInJarFiles) {
			/** Now do the jar files. */
			final String packageStringForJar = StringUtilities.cleanUpString(fullPackageName, ".", '/');
			for (final String classPathEntry : _ClassPathEntries) {
				final File file = new File(classPathEntry);
				if (!file.exists()) {
					continue;
				}
				if (!file.isDirectory()) {
					relativeResourceNameReturns
							.addAll(getResourcesFromJarFile(logger, file, packageStringForJar, pattern));
				}
			}
		}
		return relativeResourceNameReturns;
	}

	private static ArrayList<ResourceNamePlus> getResourcesFromJarFile(final MyLogger logger, final File jarFile,
			final String packageStringForJar, final Pattern pattern) {
		final ArrayList<ResourceNamePlus> returnValue = new ArrayList<>();
		try (ZipFile zipFile = new ZipFile(jarFile)) {
			final int keyPartStartingIndex = packageStringForJar.length() + 1;
			Enumeration<? extends ZipEntry> enumeration;
			try {
				enumeration = zipFile.entries();
			} catch (final Exception e2) {
				MyLogger.err(logger, "Bad ZipFile " + zipFile.getName());
				return returnValue;
			}
			while (enumeration.hasMoreElements()) {
				final ZipEntry zipEntry = enumeration.nextElement();
				final String fileName = zipEntry.getName();
				final String lcFileName = fileName.toLowerCase();
				if (lcFileName.endsWith(".class") || lcFileName.toLowerCase().endsWith(".java")) {
					continue;
				}
				/**
				 * Convert all backward slashes to forward slashes; packageStringForJar has
				 * forward slashes.
				 */
				final String forwardSlashFileName = StringUtilities.cleanUpString(fileName, "\\", '/');
				if (forwardSlashFileName.startsWith(packageStringForJar)) {
					final String keyPart = forwardSlashFileName.substring(keyPartStartingIndex);
					if (pattern.matcher(keyPart).matches()) {
						returnValue.add(new ResourceNamePlus(keyPart, zipEntry.getSize()));
					}
				}
			}
		} catch (final IOException e) {
		}
		return returnValue;
	}

	/** The old ResourceLister code follows. */
	/** List non-class resources available from the classpath. */
	final public static String _PathSeparator = StringUtilities.getSystemProperty("path.separator",
			/* useSpaceProxy= */false);
	final public static String _FileSeparator = StringUtilities.getSystemProperty("file.separator",
			/* useSpaceProxy= */false);

	public static ArrayList<String> getResourceFileNames(final String regExp) {
		final Pattern pattern = Pattern.compile(regExp, Pattern.CASE_INSENSITIVE);
		final ArrayList<String> returnValue = new ArrayList<>();
		final String classPath = System.getProperty("java.class.path", ".");
		final String[] classPathElements = classPath.split(_PathSeparator);
		for (final String element : classPathElements) {
			returnValue.addAll(getResources(element, pattern));
		}
		return returnValue;
	}

	public static String getFileSeparator() {
		return _FileSeparator;
	}

	private static ArrayList<String> getResources(final String element, final Pattern pattern) {
		final ArrayList<String> resources = new ArrayList<>();
		final File file = new File(element);
		if (file.isDirectory()) {
			final ArrayList<String> fullFileNames = getResourcesFromDirectory(file, pattern);
			/**
			 * Strip the file's full name from the file names here; we're only after the
			 * resources.
			 */
			for (final String fullFileName : fullFileNames) {
				final String resourceName = fullFileName.substring(element.length() + 1);
				resources.add(resourceName);
			}
		} else {
			resources.addAll(getResourcesFromJarFile(file, pattern));
		}
		return resources;
	}

	private static Collection<String> getResourcesFromJarFile(final File file, final Pattern pattern) {
		final ArrayList<String> returnValue = new ArrayList<>();
		try (ZipFile zipFile = new ZipFile(file)) {
			final Enumeration<? extends ZipEntry> e = zipFile.entries();
			while (e.hasMoreElements()) {
				final ZipEntry zipEntry = e.nextElement();
				final String fileName = zipEntry.getName();
				if (pattern.matcher(fileName).matches()) {
					if (!fileName.toLowerCase().endsWith(".class")) {
						returnValue.add(fileName);
					}
				}
			}
		} catch (final IOException e) {
		}
		return returnValue;
	}

	private static ArrayList<String> getResourcesFromDirectory(final File directory, final Pattern pattern) {
		final ArrayList<String> returnValue = new ArrayList<>();
		final File[] fileList = directory.listFiles();
		for (final File file : fileList) {
			if (file.isDirectory()) {
				returnValue.addAll(getResourcesFromDirectory(file, pattern));
			} else {
				final String filePath = StringUtilities.getCanonicalPath(file);
				if (filePath == null) {
					continue;
				}
				final boolean accept = pattern.matcher(filePath).matches();
				if (accept) {
					returnValue.add(filePath);
				}
			}
		}
		return returnValue;
	}

	/**
	 * list the resources that match args[0]
	 *
	 * @param args args[0] is the pattern to match, or list all resources if there
	 *             are no args
	 */
	public static void main1(final String[] args) {
		final Properties properties = System.getProperties();
		final Set<Map.Entry<Object, Object>> entries = properties.entrySet();
		for (final Map.Entry<Object, Object> entry : entries) {
			MyLogger.out(/* MyLogger= */null, entry.getKey() + "::" + entry.getValue());
		}
		final String regExp = ".*gshhs.extraIslands.islandFiles.*.xml";
		final Collection<String> resourceFileNames = getResourceFileNames(regExp);
		for (final String string : resourceFileNames) {
			MyLogger.out(/* MyLogger= */null, string);
		}
	}

	public static void main2(final String[] args) {
		final MyLogger logger = null;
		final Package thisPackage = new ResourcesLister().getClass().getPackage();
		final String regex = args[1];
		final Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		final Collection<ResourceNamePlus> list = getRelativeResourceNamePluses(logger, thisPackage, pattern, true,
				true);
		for (final ResourceNamePlus relativeResourceNameReturn : list) {
			MyLogger.out(logger, relativeResourceNameReturn.getString());
		}
	}
}
