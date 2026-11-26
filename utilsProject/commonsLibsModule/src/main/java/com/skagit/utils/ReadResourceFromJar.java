package com.skagit.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ReadResourceFromJar {

	public static void main(final String[] args) {
		final String jarPath = "path/to/your.jar"; // Absolute or relative path to JAR
		final String resourcePath = "com/example/data.txt"; // Path inside JAR (no leading slash)

		try (JarFile jarFile = new JarFile(jarPath)) {
			final JarEntry entry = jarFile.getJarEntry(resourcePath);
			if (entry == null) {
				System.err.println("Resource not found in JAR: " + resourcePath);
				return;
			}

			try (InputStream is = jarFile.getInputStream(entry);
					BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

				String line;
				while ((line = reader.readLine()) != null) {
					System.out.println(line);
				}
			}
		} catch (final IOException e) {
			System.err.println("Error reading resource: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
