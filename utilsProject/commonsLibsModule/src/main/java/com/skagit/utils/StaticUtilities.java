package com.skagit.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import com.skagit.utils.lsFormatter.LsFormatter;
import com.skagit.utils.myLogger.MyLogger;

public class StaticUtilities {
	public static String getDateString(final long timeInMillis) {
		final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return dateFormat.format(timeInMillis);
	}

	public static void changePropertiesFile(final MyLogger logger, final String from, final String to,
			final String charactersToKill, final char replacedBy) {
		/**
		 * <pre>
		 * Example of args "K:\Kelp\evmi\Epromise.properties"
		 * "K:\Kelp\evmi\Epromise2.properties, "_", ' '"
		 */
		final Properties oldProperties = new Properties();
		final Properties newProperties = new Properties();
		try (FileInputStream fis = new FileInputStream(new File(from))) {
			try {
				oldProperties.load(fis);
			} catch (final IOException e) {
				e.printStackTrace();
			}
			final Set<Map.Entry<Object, Object>> entries = oldProperties.entrySet();
			final Iterator<Map.Entry<Object, Object>> it = entries.iterator();
			while (it.hasNext()) {
				final Map.Entry<Object, Object> entry = it.next();
				String key = (String) entry.getKey();
				String value = (String) entry.getValue();
				try {
					key = URLEncoder.encode(StringUtilities.cleanUpString(key, charactersToKill, replacedBy),
							LsFormatter._CharsetName);
					value = URLEncoder.encode(StringUtilities.cleanUpString(value, "_", ' '), LsFormatter._CharsetName);
				} catch (final UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				MyLogger.err(logger, key + " >> " + value);
				newProperties.put(key, value);
			}
			try (FileOutputStream fos = new FileOutputStream(new File(to))) {
				newProperties.store(fos, "Converted from Properties with " + charactersToKill);
			} catch (final IOException e) {
				e.printStackTrace();
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	public static String convertToFileSeparator(final String path0) {
		/** Turn all slashes to File.separator. */
		final String path1 = path0.replace("/", File.separator);
		return path1.replace("\\", File.separator);
	}

	public static boolean copyNonDirectoryFile(final MyLogger logger, final String sourceFilePath,
			final String destinationFilePath) {
		final File sourceFile = new File(sourceFilePath);
		final File destinationFile = new File(destinationFilePath);
		if (destinationFile.exists() && !destinationFile.delete()) {
			return false;
		}
		try (final InputStream in = new FileInputStream(sourceFile)) {
			try (final OutputStream out = new FileOutputStream(destinationFile)) {
				final byte[] buf = new byte[1024];
				int len;
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
			} catch (final IOException e) {
				MyLogger.out(logger, e.getMessage());
				return false;
			}
		} catch (final IOException e) {
			MyLogger.out(logger, e.getMessage());
			return false;
		}
		return true;
	}

	public static void copyStreamToStream(final MyLogger logger, final InputStream in, final OutputStream out) {
		final byte[] buf = new byte[1024];
		int len;
		try {
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
		} catch (final IOException e) {
			MyLogger.out(logger, e.getMessage());
		}
	}

	/**
	 * Returns 0 if both null, 2 if neither is null, 1 if o0 is not null, and -1 if
	 * o1 is not null.
	 */
	public static int NullCompare(final Object o0, final Object o1) {
		if ((o0 == null) != (o1 == null)) {
			return o0 == null ? -1 : 1;
		}
		if (o0 == null) {
			return 0;
		}
		return 2;
	}

	public static void moveDirectory(final MyLogger logger, final String from, final String to,
			final String charactersToKill, final char replacedBy) {
		/**
		 * <pre>
		 * Example of args "K:\Kelp\evmi\copy of data" "K:\Kelp\evmi\data" "_" ' '
		 */
		final String newPath = StringUtilities.cleanUpString(to, charactersToKill, replacedBy);
		final File oldDirectory = new File(from);
		final File[] files = oldDirectory.listFiles();
		for (int passNumber = 0; passNumber < 2; ++passNumber) {
			for (final File file : files) {
				final String oldFilePath = StringUtilities.getCanonicalPath(file);
				final String newFilePathX = newPath + StringUtilities
						.cleanUpString(oldFilePath.substring(from.length()), charactersToKill, replacedBy);
				final String newFilePath = StringUtilities.cleanUpFilePath(newFilePathX);
				if (file.isDirectory()) {
					if ((passNumber == 0) || "cvs".equalsIgnoreCase(file.getName())) {
						continue;
					}
					/**
					 * Print an extra line if this is a directory that contains a real file.
					 */
					final File[] filesx = file.listFiles();
					for (final File element : filesx) {
						if (!element.isDirectory()) {
							MyLogger.out(logger, "");
							break;
						}
					}
					MyLogger.out(logger, oldFilePath + ">>" + newFilePath);
					moveDirectory(logger, oldFilePath, newFilePath, charactersToKill, replacedBy);
				} else {
					if (passNumber == 1) {
						continue;
					}
					final File targetFile = new File(newFilePath);
					new File(targetFile.getParent()).mkdirs();
					MyLogger.out(logger, "\t" + oldFilePath + ">>" + newFilePath);
					file.renameTo(targetFile);
				}
			}
		}
	}

	public static void copyDirectory(final MyLogger logger, final String fromPath, final String toPath,
			final boolean trace) {
		final File oldDirectory = new File(fromPath);
		final File[] files = oldDirectory.listFiles();
		for (int passNumber = 0; passNumber < 2; ++passNumber) {
			for (final File f : files) {
				final String oldFilePath = StringUtilities.getCanonicalPath(f);
				final String newFilePath = toPath + oldFilePath.substring(fromPath.length());
				if (f.isDirectory()) {
					if (passNumber == 0) {
						continue;
					}
					/**
					 * Print an extra line if f is a directory that contains a real file.
					 */
					final File[] filesx = f.listFiles();
					for (final File element : filesx) {
						if (!element.isDirectory()) {
							MyLogger.out(logger, "");
							break;
						}
					}
					MyLogger.out(logger, oldFilePath + '→' + newFilePath);
					copyDirectory(logger, oldFilePath, newFilePath, trace);
				} else {
					if (passNumber == 1) {
						continue;
					}
					final File targetFile = new File(newFilePath);
					new File(targetFile.getParent()).mkdirs();
					if (trace) {
						MyLogger.out(logger, "\t" + oldFilePath + ">>" + newFilePath);
					}
					copyNonDirectoryFile(logger, oldFilePath, newFilePath);
				}
			}
		}
	}

	public static void copyAndRenameFilesInDirectory(final MyLogger logger, final String fromPath, final String newPath,
			final String oldExtension, final String newExtension, final boolean trace) {
		final File oldDirectory = new File(fromPath);
		final File[] files = oldDirectory.listFiles();
		for (int passNumber = 0; passNumber < 2; ++passNumber) {
			for (final File file : files) {
				final String oldFilePath = StringUtilities.getCanonicalPath(file);
				final String newFilePath = newPath + oldFilePath.substring(fromPath.length());
				if (file.isDirectory()) {
					if (passNumber == 0) {
						continue;
					}
					/**
					 * Print an extra line if this is a directory that contains a real file.
					 */
					final File[] filesx = file.listFiles();
					for (final File element : filesx) {
						if (!element.isDirectory()) {
							MyLogger.out(logger, "");
							break;
						}
					}
					MyLogger.out(logger, oldFilePath + ">>" + newFilePath);
					copyAndRenameFilesInDirectory(logger, oldFilePath, newFilePath, oldExtension, newExtension, trace);
				} else {
					if (passNumber == 1) {
						continue;
					}
					final File targetFile = new File(newFilePath);
					new File(targetFile.getParent()).mkdirs();
					if (trace) {
						MyLogger.out(logger, "\t" + oldFilePath + ">>" + newFilePath);
					}
					final String targetFilePath = StringUtilities.getCanonicalPath(targetFile);
					copyNonDirectoryFile(logger, oldFilePath, targetFilePath);
					if (targetFilePath.endsWith(oldExtension)) {
						final String targetFilePath2 = targetFilePath.substring(0,
								targetFilePath.length() - oldExtension.length()) + newExtension;
						targetFile.renameTo(new File(targetFilePath2));
					}
				}
			}
		}
	}

	public static String getRelativePath(final File f) {
		final String homePath = StringUtilities.getCanonicalPath(new File("."));
		final String fPath = StringUtilities.getCanonicalPath(f);
		if (homePath == null || fPath == null) {
			return null;
		}
		return getRelativePath(homePath, fPath);
	}

	public static String getRelativePath(final String homePath, final String fPath) {
		String regex = File.separator;
		if ("\\".equals(regex)) {
			regex = "\\\\";
		}
		final String[] homeFields = homePath.split(regex);
		final String[] fFields = fPath.split(regex);
		int nMatched;
		for (nMatched = 0; nMatched < homeFields.length; ++nMatched) {
			if (!homeFields[nMatched].equals(fFields[nMatched])) {
				break;
			}
		}
		if (nMatched == 0) {
			return fPath;
		}
		String returnValue = "";
		for (int k = nMatched; k < homeFields.length; ++k) {
			returnValue += ".." + File.separator;
		}
		for (int k = nMatched; k < fFields.length; ++k) {
			if (k > nMatched) {
				returnValue += File.separator;
			}
			returnValue += fFields[k];
		}
		if (returnValue.length() == 0) {
			returnValue = ".";
		}
		return returnValue;
	}

	public static String getFileContent(final String filePath) {
		final StringBuilder buffer = new StringBuilder();
		try (final FileReader fileReader = new FileReader(filePath);
				final BufferedReader bufferedReader = new BufferedReader(fileReader)) {
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				buffer.append(line).append('\n');
			}
		} catch (final IOException e) {
			return null;
		}
		return buffer.toString();
	}

	public static String getFileContent(final File file) {
		final StringBuilder buffer = new StringBuilder();
		try (final FileReader fileReader = new FileReader(file);
				final BufferedReader bufferedReader = new BufferedReader(fileReader)) {
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				buffer.append(line).append('\n');
			}
		} catch (final IOException e) {
			return null;
		}
		return buffer.toString();
	}

	public static Class<?> getMyClass() {
		/**
		 * <pre>
		 * The following line would work if it were called from the calling routine.
		 * final Class myClass = new Object() {}.getClass().getEnclosingClass();
		 *
		 * Since this is a subroutine in StaticUtilities, we need to appeal to the
		 * stack trace.
		 * </pre>
		 */
		final Thread t = Thread.currentThread();
		final StackTraceElement[] stackTrace = t.getStackTrace();
		if (stackTrace.length > 2 && stackTrace[2] != null) {
			final String myClassName = stackTrace[2].getClassName();
			try {
				final Class<?> clazz0 = Class.forName(myClassName);
				/** Check other way. */
				try {
					throw new Exception();
				} catch (final Exception e) {
					final StackTraceElement[] stackTrace2 = e.getStackTrace();
					final Class<?> clazz1 = Class.forName(stackTrace2[1].getClassName());
					if (clazz0 != clazz1) {
						e.printStackTrace();
						System.exit(33);
					}
					return clazz0;
				}
			} catch (final ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public static Class<?> getMyClass2() {
		try {
			throw new Exception();
		} catch (final Exception e) {
			/** 0 would be StaticUtilities. 1 is the calling routine. */
			return e.getStackTrace()[1].getClass();
		}
	}

	public static void moveDirectory2(final MyLogger logger, final String fromDir, final String toDir,
			final String[] suffixes) {
		final File oldDirectory = new File(fromDir);
		final File[] files = oldDirectory.listFiles();
		for (int iPass = 0; iPass < 2; ++iPass) {
			for (final File file : files) {
				final String oldFilePath = StringUtilities.getCanonicalPath(file);
				final String newFilePath = toDir + oldFilePath.substring(fromDir.length());
				if (file.isDirectory()) {
					if ((iPass == 0) || "cvs".equalsIgnoreCase(file.getName())) {
						continue;
					}
					/**
					 * Print an extra line if this is a directory that contains a real file.
					 */
					final File[] filesx = file.listFiles();
					for (final File element : filesx) {
						if (!element.isDirectory()) {
							MyLogger.out(logger, "");
							break;
						}
					}
					MyLogger.out(logger, oldFilePath + ">>" + newFilePath);
					moveDirectory2(logger, oldFilePath, newFilePath, suffixes);
				} else {
					if (iPass == 1) {
						continue;
					}
					final File targetFile = new File(newFilePath);
					new File(targetFile.getParent()).mkdirs();
					MyLogger.out(logger, "\t" + oldFilePath + ">>" + newFilePath);
					final int dotIndex = newFilePath.lastIndexOf('.');
					final String mySuffix = newFilePath.substring(dotIndex + 1).toLowerCase();
					for (final String suffix : suffixes) {
						if (mySuffix.equals(suffix.toLowerCase())) {
							BufferedReader bufferedReader = null;
							BufferedWriter bufferedWriter = null;
							try {
								final FileReader fileReader = new FileReader(new File(oldFilePath));
								bufferedReader = new BufferedReader(fileReader);
								final FileWriter fileWriter = new FileWriter(new File(newFilePath));
								bufferedWriter = new BufferedWriter(fileWriter);
							} catch (final IOException e) {
								e.printStackTrace();
							}
							while (true) {
								try {
									final String line = bufferedReader.readLine();
									if (line == null) {
										bufferedReader.close();
										bufferedWriter.close();
										break;
									}
									bufferedWriter.write(line);
									bufferedWriter.newLine();
								} catch (final IOException e) {
									e.printStackTrace();
								}
							}
							break;
						}
					}
				}
			}
		}
	}

	public static String[] getStemAndExtension(final String fName) {
		final int lastDot = fName.lastIndexOf('.');
		final int fNameLen = fName.length();
		if (lastDot < 0) {
			/** We have no dot. */
			return new String[] { fName, "" };
		}
		if (lastDot < fNameLen - 1) {
			/** We have a dot and it's not the last character. */
			return new String[] { //
					fName.substring(0, lastDot), //
					fName.substring(lastDot + 1) //
			};
		}
		/** Because of OS problems with files that end with a period, let this crash. */
		return null;
	}

	/**
	 * <pre>
	 * 1. a/b/c.txt -> a/b/c.txt
	 * 2. a.txt     -> a.txt
	 * 3. a/b/c     -> a/b/c.txt
	 * 4. a/b/c/    -> .txt
	 * 5. a/b.c/d   -> a/b.c/d.txt
	 * 6. abcd      -> abcd.txt
	 * </pre>
	 */
	public static String forceExtension(final String path0, final String extension) {
		final String withDot = "." + extension.toLowerCase();
		if (path0.toLowerCase().endsWith(withDot)) {
			/** We're fine the way we are. */
			return path0;
		}
		final int len = path0 == null ? 0 : path0.length();
		if (len == 0) {
			return null;
		}
		final int lastDot = path0.lastIndexOf('.');
		if (lastDot == len - 1) {
			/** Ends with a dot. Just add the extension. */
			return path0 + extension;
		}
		/** Turn all slashes to File.separator. */
		final String path1 = path0.replace('/', File.separatorChar);
		final String path = path1.replace('\\', File.separatorChar);
		final int lastSep = path.lastIndexOf(File.separatorChar);
		if (lastDot < lastSep) {
			/** The last dot precedes the last slash. Do nothing. */
			return path0;
		}
		return path0.substring(0, lastDot) + withDot;
	}

	/**
	 * <pre>
	 * 1. a/b/c.txt -> a/b/c
	 * 2. a.txt     -> a
	 * 3. a/b/c/    -> a/b/c/
	 * 4. a/b.c/d   -> a/b.c/d
	 * 5. abcd      -> abcd
	 * </pre>
	 */
	public static String stripExtension(final String path0) {
		final int lastDot = path0.lastIndexOf('.');
		final int len = path0.length();
		if (lastDot == -1 || lastDot == len - 1) {
			return path0;
		}
		final String path1 = path0.replace("/", File.separator);
		final String path = path1.replace("\\", File.separator);
		final int lastSep = path.lastIndexOf(File.separatorChar);
		if (lastDot < lastSep) {
			return path0;
		}
		return path0.substring(0, lastDot);
	}

	/**
	 * Returns the the input if dirFile exists and is a directory, or we were able
	 * to create a directory, which is what we would then return.
	 */
	public static File makeDirectory(final File dirFile) {
		if (dirFile == null) {
			return null;
		}
		if (dirFile.exists()) {
			return dirFile.isDirectory() ? dirFile : null;
		}
		return dirFile.mkdirs() ? dirFile : null;
	}

	public static boolean deleteAnyFile(final File file) {
		return deleteAnyFileWithErrReturn(file) == 0;
	}

	public static int deleteAnyFileWithErrReturn(final File file) {
		if (file == null || !file.exists()) {
			return 0;
		}
		if (!file.isDirectory()) {
			return file.delete() ? 0 : 1;
		}
		if (!clearDirectory(file)) {
			return 2;
		}
		return file.delete() ? 0 : 3;
	}

	public static boolean clearDirectory(final File directoryFile) {
		return clearDirectoryOfSuffix(directoryFile, /* suffix= */null);
	}

	public static boolean clearDirectoryOfSuffix(final File directoryFile, final String suffix) {
		if (directoryFile != null && directoryFile.isDirectory()) {
			String[] fileNames = null;
			fileNames = directoryFile.list(
					(_, fileName) -> suffix == null || fileName.toLowerCase().endsWith("." + suffix.toLowerCase()));
			final int nI = fileNames.length;
			for (int i = 0; i < nI; ++i) {
				final File fileToDie = new File(directoryFile, fileNames[i]);
				if ((fileToDie.isDirectory() && !clearDirectoryOfSuffix(fileToDie, /* suffix= */null))
						|| !fileToDie.delete()) {
					return false;
				}
			}
		}
		return true;
	}

	public static boolean clearDirectoryWithFilter(final File dirFile, final FilenameFilter filter) {
		if (dirFile != null && dirFile.isDirectory()) {
			String[] fileNames = null;
			if (filter != null) {
				fileNames = dirFile.list(filter);
			} else {
				fileNames = dirFile.list();
			}
			final int nI = fileNames.length;
			for (int i = 0; i < nI; ++i) {
				final File fileToDie = new File(dirFile, fileNames[i]);
				if ((fileToDie.isDirectory() && !clearDirectoryWithFilter(fileToDie, /* subFilter= */null))
						|| !fileToDie.delete()) {
					return false;
				}
			}
		}
		return true;
	}

	public static void clearSubDirsWithName(final File dirFile, final String fileNameToDelete) {
		final File[] subDirs = dirFile.listFiles((FileFilter) f -> f.isDirectory() && f.canWrite() && f.canRead());
		final int nSubDirs = subDirs.length;
		for (int k = 0; k < nSubDirs; ++k) {
			final File subDir = subDirs[k];
			if (subDir.getName().equals(fileNameToDelete) && clearDirectory(subDir)) {
				subDir.delete();
			} else {
				clearSubDirsWithName(subDir, fileNameToDelete);
			}
		}
	}

	private enum Operation {
		READ, WRITE, EXECUTE;
	}

	public static boolean giveEveryoneFullPermission1(final MyLogger logger, final File f, final boolean logToErrFile) {
		if (!f.exists()) {
			return true;
		}
		final boolean permission = true;
		final boolean ownerOnly = false;
		boolean globalSuccess = true;
		for (final Operation operation : Operation.values()) {
			boolean couldDo = false;
			boolean success = false;
			SecurityException securityException = null;
			try {
				switch (operation) {
				case READ:
					couldDo = f.canRead();
					success = f.setReadable(permission, ownerOnly);
					break;
				case WRITE:
					couldDo = f.canWrite();
					success = f.setWritable(permission, ownerOnly);
					break;
				default:
					couldDo = f.canExecute();
					success = f.setExecutable(permission, ownerOnly);
					break;
				}
			} catch (final SecurityException e) {
				success = false;
				securityException = e;
			}
			String message = String.format("Could %s %s %s", (couldDo ? "" : "not"), operation.name(),
					StringUtilities.getCanonicalPath(f));
			if (!success) {
				globalSuccess = false;
				final StringBuilder message1 = new StringBuilder().append(String.format(
						"Could not set %s permission for %s", operation.name(), StringUtilities.getCanonicalPath(f)));
				if (securityException != null) {
					message1.append(String.format(":\n\tSecurityException[%s].", securityException.getMessage()));
				} else {
					message1.append(".\nNot a SecurityException.");
				}
				message += "\n" + message1.toString();
			} else {
				final String message1 = String.format("Set %s permission for %s.\n", operation.name(),
						StringUtilities.getCanonicalPath(f));
				message += "\n" + message1;
			}
			if (couldDo || !logToErrFile) {
				MyLogger.out(logger, message);
			} else {
				MyLogger.err(logger, message);
			}
		}
		/** If a directory, give everyone below it full permission. */
		if (f.isDirectory()) {
			final File[] contents = f.listFiles();
			if (contents != null) {
				for (final File content : contents) {
					final boolean success = giveEveryoneFullPermission1(logger, content, logToErrFile);
					if (!success) {
						globalSuccess = false;
					}
				}
			}
		}
		return globalSuccess;
	}

	public static long getTimeOfModificationMs(final String filePath) {
		try {
			return new File(filePath).lastModified();
		} catch (final NullPointerException e) {
			return -1L;
		}
	}

	public static String[] trimEmptyStrings(final String[] stringArray) {
		if (stringArray == null) {
			return null;
		}
		final ArrayList<String> stringList = new ArrayList<>();
		for (final String s : stringArray) {
			if (s != null && s.length() > 0) {
				stringList.add(s);
			}
		}
		return stringList.toArray(new String[stringList.size()]);
	}

	@SuppressWarnings("rawtypes")
	public static void clearList(final List list) {
		if (list != null) {
			list.clear();
			if (list instanceof ArrayList) {
				((ArrayList) list).trimToSize();
			}
		}
	}

	public static String legalizeWindowsFilename(final String fileName) {
		return fileName.replaceAll("[:\\\\/*?|<>]", "_");
	}

	public static void main2(final String[] args) {
		final File dirFile = new File(args[0]);
		final String fileNameToDelete = args[1];
		clearSubDirsWithName(dirFile, fileNameToDelete);
	}

	public static void main3(final String[] args) {
		final Properties systemProperties = System.getProperties();
		final Set<Object> keys = systemProperties.keySet();
		final int nKeys = keys.size();
		final Iterator<Object> it = keys.iterator();
		for (int k = 0; k < nKeys; ++k) {
			final String key = (String) it.next();
			final String value = systemProperties.getProperty(key);
			System.out.printf("\nkey[%s] value[%s]", key, value);
		}
	}

	/**
	 * The following was used to convert one text file to ColorConstants.java. Its
	 * usefulness at this point, since I now have ColorConstants.java, is as an
	 * example of reading in a text file of lines and parsing it with
	 * StringTokenizer, BufferedReader and all that.
	 */
	public static void main4() {
		final MyLogger logger = null;
		final String fileName = "C:/javaDevelopment/Downloads/Webpages/Colors.txt";
		try (BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName))) {
			final TreeSet<int[]> colors = new TreeSet<>(CombinatoricTools._ByAllInOrder);
			while (true) {
				try {
					final String line = bufferedReader.readLine();
					if (line == null || line.length() == 0) {
						break;
					}
					final StringTokenizer stringTokenizer = new StringTokenizer(line, "\n\t ");
					int red = -1, green = -1, blue = -1;
					String colorName = "";
					while (true) {
						try {
							String token = stringTokenizer.nextToken();
							if (red == -1) {
								red = Integer.parseInt(token);
							} else if (green == -1) {
								green = Integer.parseInt(token);
							} else if (blue == -1) {
								blue = Integer.parseInt(token);
							} else {
								final char[] b = token.toCharArray();
								if (Character.isLetter(b[0])) {
									b[0] = Character.toUpperCase(b[0]);
								}
								token = new String(b);
								colorName += token;
							}
						} catch (final RuntimeException e) {
							if (colors.add(new int[] { red, green, blue })) {
								MyLogger.out(logger, "final public static int[] _" + colorName + " = new int[]{" + red
										+ ',' + green + ',' + blue + "};");
							}
							break;
						}
					}
				} catch (final IOException e) {
					e.printStackTrace();
					break;
				}
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}
}
