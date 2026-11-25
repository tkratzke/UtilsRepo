package com.skagit.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Java 8 program to collect all .txt files from directories in the classpath.
 */
public class ResourcesListerX {

	public static void main(final String[] args) {
		try {
			final List<Path> txtFiles = collectTxtFilesFromClasspath();
			if (txtFiles.isEmpty()) {
				System.out.println("No .txt files found in classpath directories.");
			} else {
				System.out.println("Found .txt files:");
				txtFiles.forEach(System.out::println);
			}
		} catch (final IOException e) {
			System.err.println("Error scanning classpath: " + e.getMessage());
		}
	}

	public static List<Path> collectTxtFilesFromClasspath() throws IOException {
		final String classpath = System.getProperty("java.class.path");
		final String pathSeparator = File.pathSeparator;

		final List<Path> txtFiles = new ArrayList<>();

		for (final String entry : classpath.split(Pattern.quote(pathSeparator))) {
			final Path path = Paths.get(entry);
			if (Files.isDirectory(path)) {
				try (Stream<Path> walk = Files.walk(path)) {
					final List<Path> found = walk.filter(Files::isRegularFile)
							.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".txt"))
							.collect(Collectors.toList());
					txtFiles.addAll(found);
				}
			}
		}
		return txtFiles;
	}
}
