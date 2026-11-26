package com.skagit.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

public class ResourcesLister {
	public static class Resource {
		final public Path _cpEntry;
		final public String _rltvPath;
		final public long _size;

		private Resource(final Path cpEntry, final String rltvPath, final long size) {
			_cpEntry = cpEntry;
			_rltvPath = rltvPath;
			_size = size;
		}

		public boolean isJarFile() {
			return _cpEntry.toFile().isFile();
		}

		public Path getFilePath() {
			return isJarFile() ? null : _cpEntry.resolve(_rltvPath);
		}

		public String getString() {
			return String.format("%s::%s(%s), size[%d]", _cpEntry, _rltvPath, isJarFile() ? "JAR" : "DIR", _size);
		}

		@Override
		public String toString() {
			return getString();
		}
	}

	final private static String[] _CpEntries = System.getProperty("java.class.path").split(File.pathSeparator);
	final private static String _SeparatorString = String.valueOf(File.separatorChar); // (eg / or \)

	public static Resource[] getRsrcs(final Package pckg, final String regExForFileName0) {
		final ArrayList<Resource> allRsrcs = new ArrayList<>();

		final String regEx0x;
		if ("\\".equals(_SeparatorString)) {
			/**
			 * Since _SeparatorString is a backslash, we need to send two of them, wherever
			 * we want a backslash, to Pattern.compile.
			 */
			final String doubledSeparator = _SeparatorString + _SeparatorString;
			final String regExForFileName = regExForFileName0.replace("/", doubledSeparator);
			regEx0x = pckg.getName().replace(".", doubledSeparator) + doubledSeparator + regExForFileName;
		} else {
			final String regExForFileName = regExForFileName0.replace("\\", _SeparatorString);
			regEx0x = pckg.getName().replace(".", _SeparatorString) + _SeparatorString + regExForFileName;
		}
		final String regEx0 = regEx0x + (regEx0x.endsWith("$") ? "" : "$");
		final Pattern pattern0 = Pattern.compile(regEx0, Pattern.CASE_INSENSITIVE);
		final Predicate<Path> predForFilter0 = new Predicate<>() {

			@Override
			public boolean test(final Path path) {
				final Matcher matcher = pattern0.matcher(path.toString());
				return matcher.find();
			}
		};
		for (final String cpEntry : _CpEntries) {
			final Path cpEntryPath = Paths.get(cpEntry);
			final File cpEntryFile = cpEntryPath.toFile();
			if (cpEntryFile.isDirectory()) {
				final Consumer<Path> addToAllRsrcs0 = new Consumer<>() {

					@Override
					public void accept(final Path path) {
						allRsrcs.add(new Resource(cpEntryPath, cpEntryPath.relativize(path).toString(),
								path.toFile().length()));
					}
				};
				try {
					Files.walk(cpEntryPath).filter(predForFilter0).forEach(addToAllRsrcs0);
				} catch (final IOException e) {
				}
			}
		}

		/** jarFiles use forward slashes. */
		final String regExForFileName = regExForFileName0.replace("\\", _SeparatorString);
		final String regEx1x = pckg.getName().replace('.', '/') + '/' + regExForFileName;
		final String regEx1 = regEx1x + (regEx1x.endsWith("$") ? "" : "$");
		final Pattern pattern1 = Pattern.compile(regEx1, Pattern.CASE_INSENSITIVE);
		final Predicate<ZipEntry> predForFilter1 = new Predicate<>() {

			@Override
			public boolean test(final ZipEntry zipEntry) {
				final Matcher matcher = pattern1.matcher(zipEntry.toString());
				return matcher.find();
			}
		};
		for (final String cpEntry : _CpEntries) {
			final Path cpEntryPath = Paths.get(cpEntry);
			final File cpEntryFile = cpEntryPath.toFile();
			if (cpEntryFile.isFile()) {
				try (JarFile jarFile = new JarFile(cpEntryFile)) {
					final Consumer<ZipEntry> addToAllRsrcs1 = new Consumer<>() {

						@Override
						public void accept(final ZipEntry zipEntry) {
							allRsrcs.add(new Resource(cpEntryPath, zipEntry.getName(), zipEntry.getSize()));
						}
					};
					Collections.list(jarFile.entries()).stream().filter(predForFilter1).forEach(addToAllRsrcs1);
				} catch (final IOException e) {
				}
			}
		}
		return allRsrcs.toArray(new Resource[allRsrcs.size()]);
	}

	public static void main(final String[] args) {
		final Class<?> clazz = StaticUtilities.getMyClass();
		final Package pckg = clazz.getPackage();
		final String regExForFileName = "(a|b)\\.(txt|properties)";
		Stream.of(getRsrcs(pckg, regExForFileName)).forEach(System.out::println);
	}

}
