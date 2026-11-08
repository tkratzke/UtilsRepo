package com.skagit.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class TestResourcesLister0 {
	public static void main(final String[] args) {
		@SuppressWarnings("rawtypes")
		final Class clazz = StaticUtilities.getMyClass();
		final Package pckg = clazz.getPackage();
		final ArrayList<ResourcesLister0.CpEntryAndRelativePath> cpEntryAndRelativePaths = ResourcesLister0
				.getCpEntryAndRelativePaths(pckg, "Dummy.*\\.txt", true, true);
		for (final ResourcesLister0.CpEntryAndRelativePath cpEntryAndRelativePath : cpEntryAndRelativePaths) {
			System.out.println(cpEntryAndRelativePath);
			final String relativePath0 = cpEntryAndRelativePath._relativePath;
			final String relativePath = "/" + relativePath0.replace('\\', '/');
			try (InputStream in = clazz.getResourceAsStream(relativePath);
					BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
				final StringBuilder content = new StringBuilder();
				String line;
				while ((line = reader.readLine()) != null) {
					content.append(line).append("\n");
				}
				System.out.println(content.toString());
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
	}
}
