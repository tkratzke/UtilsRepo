package com.skagit.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

public class Win10FileComparator implements Comparator<File> {

	/**
	 * <pre>
	 * OFFSET_T string is decomposed into a sequence of digit-strings
	 * and non-digit strings. When both Strings are so decomposed, we compare
	 * these sequences of Strings pairwise.  For each pair:
	 * 1. Digit-strings are "smaller" than non-digit Strings.
	 * 2. Non-digit Strings are compared with ignore-case.
	 * 3. Digit-strings are compared by parsing into integers,
	 *    and if the resulting integers are equal, the longer String
	 *    is considered smaller because it has more 0's padding
	 *    the left side of it.
	 * Note that there are no negative numbers or decimals in
	 * digit-strings, since neither '-' nor '.' is a digit.
	 *
	 * </pre>
	 */
	private static String getSection(final String s) {
		final int n = s == null ? 0 : s.length();
		if (n == 0) {
			return null;
		}
		/** The returned String will have at least 1 character. */
		final char c0 = s.charAt(0);
		final boolean isDigits = Character.isDigit(c0);
		final boolean isWhite = Character.isWhitespace(c0);
		for (int k = 1;; ++k) {
			final boolean done;
			if (k == n) {
				done = true;
			} else {
				final char c = s.charAt(k);
				done = (Character.isDigit(c) != isDigits) || (Character.isWhitespace(c) != isWhite);
			}
			if (done) {
				return s.substring(0, k);
			}
		}
	}

	final private static int compareSections(final String section0, final String section1) {
		final int len0 = section0 == null ? 0 : section0.length();
		final int len1 = section1 == null ? 0 : section1.length();
		if (len0 == 0) {
			return len1 == 0 ? 0 : -1;
		}
		if (len1 == 0) {
			return 1;
		}
		final char c00 = section0.charAt(0);
		final char c10 = section1.charAt(0);
		final boolean isWhite0 = Character.isWhitespace(c00);
		final boolean isWhite1 = Character.isWhitespace(c10);
		if (isWhite0) {
			if (!isWhite1) {
				return -1;
			}
			/** Both are white. */
			return len0 < len1 ? 1 : (len0 > len1 ? -1 : 0);
		}
		if (isWhite1) {
			return 1;
		}
		/** Neither is white. */
		final boolean isDigits0 = Character.isDigit(c00);
		final boolean isDigits1 = Character.isDigit(c10);
		if (isDigits0) {
			if (!isDigits1) {
				return -1;
			}
			final int intValue0 = Integer.parseInt(section0);
			final int intValue1 = Integer.parseInt(section1);
			if (intValue0 != intValue1) {
				return intValue0 < intValue1 ? -1 : 1;
			}
			return len0 < len1 ? 1 : (len0 > len1 ? -1 : 0);
		}
		if (isDigits1) {
			return 1;
		}
		return section0.compareToIgnoreCase(section1);
	}

	private static int compareFileNames(final String fileName0, final String fileName1) {
		for (int iPass = 0; iPass < 2; ++iPass) {
			String s0 = iPass == 0 ? FilenameUtils.getBaseName(fileName0) : FilenameUtils.getExtension(fileName0);
			String s1 = iPass == 0 ? FilenameUtils.getBaseName(fileName1) : FilenameUtils.getExtension(fileName1);
			for (;;) {
				final String section0 = getSection(s0);
				final String section1 = getSection(s1);
				if (section0 == null) {
					if (section1 == null) {
						/** Done with this pass. */
						break;
					}
					return -1;
				}
				if (section1 == null) {
					return 1;
				}
				/** Neither section is null, and both have length at least 1. */
				final int compareValue = compareSections(section0, section1);
				if (compareValue != 0) {
					return compareValue;
				}
				s0 = s0.substring(section0.length());
				s1 = s1.substring(section1.length());
			}
		}
		return 0;
	}

	@Override
	public int compare(final File f0, final File f1) {
		return compareFileNames(f0.getName(), f1.getName());
	}

	final public static String[] _TestNames = { //
			"filename", //
			"filename 00", //
			"filename 0", //
			"filename 01", //
			"filename.jpg", //
			"filename.txt", //
			"filename00.jpg", //
			"filename00a.jpg", //
			"filename00a.txt", //
			"filename0", //
			"filename0.jpg", //
			"filename0a.txt", //
			"filename0b.jpg", //
			"filename0b1.jpg", //
			"filename0b02.jpg", //
			"filename0c.jpg", //
			"filename01.0hjh45-test.txt", //
			"filename01.0hjh46", //
			"filename01.1hjh45.txt", //
			"filename01.hjh45.txt", //
			"Filename01.jpg", //
			"Filename1.jpg", //
			"filename2.hjh45.txt", //
			"filename2.jpg", //
			"filename03.jpg", //
			"filename3.jpg", //
	};

	public static void main0(final String[] args) {
		final File testDir = new File("C:\\Users\\tkrat\\Documents\\TestFolder");
		final int nTestNames = _TestNames.length;
		final File testFile = new File(testDir, "testFile");
		for (int k = 0; k < nTestNames; ++k) {
			try {
				FileUtils.copyFile(testFile, new File(testDir, _TestNames[k]));
			} catch (final IOException e) {
			}
		}
	}

	public static void main(final String[] args) {

		/** Shuffle. */
		final int nTestNames = _TestNames.length;
		final String[] testNames0 = _TestNames.clone();
		PermutationTools.permute(testNames0, new Random(2081957L));
		final String[] testNames = testNames0.clone();
		/** Sort. */
		Arrays.parallelSort(testNames, new Comparator<String>() {

			@Override
			public int compare(final String s0, final String s1) {
				return compareFileNames(s0, s1);
			}
		});
		for (int k = 0; k < nTestNames; ++k) {
			System.out.printf("\n%-28s%-28s", _TestNames[k], testNames[k]);
		}

		final File origDir = new File(
				"C:\\Users\\tkrat\\Google Drive\\AWK Documents\\ArchivedSlides\\2017Project\\TWO_BY_THREE\\N06");
		final File copiesDir = new File("C:\\Users\\tkrat\\Desktop\\ScanCafeSlides\\N06");
		final File mergeDir = new File(origDir, "MergedWithCopies");
		final FileFilter jpgFilter = new FileFilter() {

			@Override
			public boolean accept(final File f) {
				return "jpg".equalsIgnoreCase(FilenameUtils.getExtension(f.getAbsolutePath()));
			}
		};
		final File[] origFiles = origDir.listFiles(jpgFilter);
		Arrays.parallelSort(origFiles, new Win10FileComparator());
		final File[] copyFiles = copiesDir.listFiles(jpgFilter);
		Arrays.parallelSort(copyFiles, new Win10FileComparator());
		final int nOrigFiles = origFiles == null ? 0 : origFiles.length;
		final int nCopyFiles = copyFiles == null ? 0 : copyFiles.length;
		if (nOrigFiles != nCopyFiles) {
			System.exit(30);
		}

		StaticUtilities.makeDirectory(mergeDir);
		StaticUtilities.clearDirectory(mergeDir);
		for (int k = 0; k < nOrigFiles; ++k) {
			final File origFile = origFiles[k];
			final File origFileA = new File(mergeDir, origFile.getName());
			final String origPathA = origFileA.getAbsolutePath();
			final File copyFile = copyFiles[k];
			final String copyBaseName = FilenameUtils.getBaseName(origPathA) + "_sc";
			final String extension = FilenameUtils.getExtension(origPathA);
			final File copyFileA = new File(mergeDir, copyBaseName + FilenameUtils.EXTENSION_SEPARATOR + extension);
			try {
				FileUtils.copyFile(origFile, origFileA, /* preserveFileDate= */true);
				FileUtils.copyFile(copyFile, copyFileA, /* preserveFileDate= */true);
			} catch (final IOException e) {
			}
		}
	}

}
