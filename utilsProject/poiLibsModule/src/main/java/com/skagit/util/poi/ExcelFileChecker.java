package com.skagit.util.poi;

import java.util.Scanner;

public class ExcelFileChecker {

	/**
	 * <pre>
	 * Checks if the given string ends with ".xls" plus optionally one more
	 * character. Examples that match: "file.xls", "file.xlsm", "file.xls1" and "file.xlsx."
	 * Examples that don't match: "file.xl", "file.xlsmm"
	 * </pre>
	 */
	public static boolean endsWithXlsPlusOne(final String fName) {
		if (fName == null) {
			return false;
		}
		/** Note: (?i) means case-insensitive. */
		return fName.matches("(?i).*\\.xls.?$");
	}

	public static void main(final String[] args) {
		final Scanner scanner = new Scanner(System.in);

		System.out.print("Enter a filename: ");
		final String filename = scanner.nextLine().trim();

		if (endsWithXlsPlusOne(filename)) {
			System.out.println("✅ Matches: ends with .xls plus optionally one more character.");
		} else {
			System.out.println("❌ Does not match the pattern.");
		}
		scanner.close();
	}
}
