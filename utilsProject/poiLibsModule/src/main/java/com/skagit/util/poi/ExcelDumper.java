package com.skagit.util.poi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.skagit.utils.StaticUtilities;

public class ExcelDumper {
	final public static String _ListSuffix = ".xlsx";
	final private static String _UcListSuffix = _ListSuffix.toUpperCase();
	/**
	 * If you just want to use capital letters, you can use the static function
	 * CellReference.convertNumToColString(k) in the poi library.
	 */
	final private static String _Uniquifiers = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	final private static int _Len = _Uniquifiers.length();

	final private static String _LegalStartCharacters = "_ABCDEFGHIJKLMNOPQRSTUVWXYZ";

	final private static String _LegalCharacters = _LegalStartCharacters + "0123456789";

	final private static String _IllegalRegEx = "[a-zA-Z]+[0-9]+";

	final private static Pattern _PatternForSplitting = Pattern.compile("[0-9]+");

	public static void closeAndWriteWorkBook(final XSSFWorkbook workBook, final File fIn) {
		final File f = staticGetFile(fIn);
		try (final FileOutputStream stream = new FileOutputStream(f)) {
			Poi.autoFit(workBook);
			workBook.write(stream);
		} catch (final IOException e) {
		}
		try {
			workBook.close();
		} catch (final IOException e) {
		}
	}

	public static String createExcelName(final String columnLabel) {
		final int nChars = columnLabel == null ? 0 : columnLabel.length();
		if (nChars == 0) {
			return null;
		}
		final char c0 = Character.toUpperCase(columnLabel.charAt(0));
		final StringBuilder sb;
		if (_LegalStartCharacters.indexOf(c0) < 0) {
			sb = new StringBuilder("Z_");
		} else {
			sb = new StringBuilder();
		}
		for (int k = 0; k < nChars; ++k) {
			final char c = Character.toUpperCase(columnLabel.charAt(k));
			if (_LegalCharacters.indexOf(c) >= 0) {
				sb.append(columnLabel.charAt(k));
			}
		}
		for (String returnValue = sb.toString();;) {
			if (!returnValue.matches(_IllegalRegEx)) {
				return returnValue;
			}
			final String[] fields = _PatternForSplitting.split(returnValue);
			final int firstDigit = fields[0].length();
			returnValue = returnValue.substring(0, firstDigit) + "_" + returnValue.substring(firstDigit);
		}
	}

	public static String getCellRef(final int i, final boolean absRow, final int j, final boolean absCol) {
		final String colName = org.apache.poi.ss.util.CellReference.convertNumToColString(j);
		return String.format("%s%s%s%d", absCol ? "$" : "", colName, absRow ? "$" : "", i + 1);
	}

	public static String indexToString(final int index) {
		int nDigits = 1;
		int remaining = index;
		for (int block = _Len; remaining >= block; block *= _Len) {
			++nDigits;
			remaining -= block;
		}
		final StringBuffer sb = new StringBuffer();
		for (int k = 0; k < nDigits; ++k) {
			final int charIndex = remaining % _Len;
			sb.append(_Uniquifiers.charAt(charIndex));
			remaining /= _Len;
		}
		sb.reverse();
		return new String(sb);
	}

	private static File staticGetFile(final File fIn) {
		try {
			final String filePathIn = fIn.getCanonicalPath();
			final boolean ok = filePathIn.toUpperCase().endsWith(_UcListSuffix);
			final String filePath = ok ? filePathIn : (filePathIn + _UcListSuffix);
			final File f = new File(filePath);
			f.delete();
			return f;
		} catch (final IOException e) {
		}
		return null;
	}

	public static int stringToIndex(final String columnName) {
		final int nDigits = columnName.length();
		int part1 = 0;
		int block = _Len;
		for (int k = 0; k < nDigits - 1; ++k) {
			part1 += block;
			block *= _Len;
		}
		int part2 = 0;
		for (int k = 0; k < nDigits; ++k) {
			final char c = Character.toUpperCase(columnName.charAt(k));
			part2 *= _Len;
			part2 += _Uniquifiers.indexOf(c);
		}
		final int returnValue = part1 + part2;
		return returnValue;
	}

	final private File _outputDir;

	protected XSSFWorkbook _defaultWorkBook;

	public ExcelDumper(final File outputDir, final boolean clearOutDir) {
		_outputDir = outputDir;
		StaticUtilities.makeDirectory(_outputDir);
		final String[] fileNames = _outputDir.list(new FilenameFilter() {
			@Override
			public boolean accept(final File f, final String fileName) {
				return fileName.toUpperCase().contains(_UcListSuffix);
			}
		});
		if (clearOutDir) {
			final int nFilesToDelete = fileNames == null ? 0 : fileNames.length;
			for (int i = 0; i < nFilesToDelete; ++i) {
				final File fileToDelete = new File(_outputDir, fileNames[i]);
				if (fileToDelete.isDirectory()) {
					final String suffix = null;
					StaticUtilities.clearDirectoryOfSuffix(fileToDelete, suffix);
				}
				fileToDelete.delete();
			}
		}
		_defaultWorkBook = new XSSFWorkbook();
	}

	public void close() {
		close("CatchAll");
	}

	/** Close (write) the default workbook. */
	public void close(final String fileName) {
		if (_defaultWorkBook != null && _defaultWorkBook.getNumberOfSheets() > 0) {
			close(_defaultWorkBook, fileName);
			_defaultWorkBook = null;
		}
	}

	/** Close (write) a specified workbook. */
	public void close(final XSSFWorkbook workBookX, final String fileName) {
		final File f = getFile(fileName);
		try (final FileOutputStream stream = new FileOutputStream(f)) {
			final XSSFWorkbook workBook = workBookX == null ? _defaultWorkBook : workBookX;
			Poi.autoFit(workBook);
			workBook.write(stream);
			workBook.close();
		} catch (final IOException e) {
		}
	}

	/** Float @@@ */
	public void createSheetFl(final XSSFWorkbook workBookX, final String coreSheetName, final float[] a) {
		final int em = a.length;
		final XSSFWorkbook workBook = workBookX == null ? _defaultWorkBook : workBookX;
		final Sheet sheet = workBook.createSheet(coreSheetName);
		int iRow = 0;
		final Row row = sheet.createRow(iRow++);
		for (int i = 0; i < em; ++i) {
			final Cell cell = row.createCell(i);
			cell.setCellValue(a[i]);
		}
	}

	public void createSheetFl(final XSSFWorkbook workBookX, final String coreSheetName, final float[][] a) {
		final int em = a.length;
		final int en = a[0].length;
		int iRow = 0;
		final XSSFWorkbook workBook = workBookX == null ? _defaultWorkBook : workBookX;
		final Sheet sheet = workBook.createSheet(coreSheetName);
		for (int i = 0; i < em; ++i) {
			final Row row = sheet.createRow(iRow++);
			for (int j = 0; j < en; ++j) {
				final Cell cell = row.createCell(j);
				cell.setCellValue(a[i][j]);
			}
		}
	}

	public void createSheetFl(final XSSFWorkbook workBookX, final String coreSheetName, final float[][][] a) {
		final int em = a.length;
		final int en1 = a[0].length;
		final int en2 = a[0][0].length;
		int iRow = 0;
		final XSSFWorkbook workBook = workBookX == null ? _defaultWorkBook : workBookX;
		final Sheet sheet = workBook.createSheet(coreSheetName);
		for (int i = 0; i < em; ++i) {
			if (i > 0) {
				++iRow;
			}
			for (int j1 = 0; j1 < en1; ++j1) {
				final Row row = sheet.createRow(iRow++);
				for (int j2 = 0; j2 < en2; ++j2) {
					final Cell cell = row.createCell(j2);
					cell.setCellValue(a[i][j1][j2]);
				}
			}
		}
	}

	public void createSheetFl(final XSSFWorkbook workBookX, final String coreSheetName, final float[][][][] a) {
		final int em = a.length;
		final boolean buildSeparateWorkbook = em >= 4;
		final XSSFWorkbook workBook;
		if (buildSeparateWorkbook) {
			workBook = new XSSFWorkbook();
		} else {
			workBook = workBookX == null ? _defaultWorkBook : workBookX;
		}
		for (int i = 0; i < em; ++i) {
			final String name = String.format("%s-%03d", coreSheetName, i);
			createSheetFl(workBook, name, a[i]);
		}
		if (buildSeparateWorkbook) {
			close(workBook, coreSheetName);
		}
	}

	/** Integer @@@ */
	public void createSheetIn(final XSSFWorkbook workBookX, final String coreSheetName, final int[] a) {
		final int em = a.length;
		final XSSFWorkbook workBook = workBookX == null ? _defaultWorkBook : workBookX;
		final Sheet sheet = workBook.createSheet(coreSheetName);
		int iRow = 0;
		final Row row = sheet.createRow(iRow++);
		for (int i = 0; i < em; ++i) {
			final Cell cell = row.createCell(i);
			cell.setCellValue(a[i]);
		}
	}

	public void createSheetIn(final XSSFWorkbook workBookX, final String coreSheetName, final int[][] a) {
		final int em = a.length;
		final int en = a[0].length;
		int iRow = 0;
		final XSSFWorkbook workBook = workBookX == null ? _defaultWorkBook : workBookX;
		final Sheet sheet = workBook.createSheet(coreSheetName);
		for (int i = 0; i < em; ++i) {
			final Row row = sheet.createRow(iRow++);
			for (int j = 0; j < en; ++j) {
				final Cell cell = row.createCell(j);
				cell.setCellValue(a[i][j]);
			}
		}
	}

	public void createSheetIn(final XSSFWorkbook workBookX, final String coreSheetName, final int[][][] a) {
		final int em = a.length;
		final int en1 = a[0].length;
		final int en2 = a[0][0].length;
		int iRow = 0;
		final XSSFWorkbook workBook = workBookX == null ? _defaultWorkBook : workBookX;
		final Sheet sheet = workBook.createSheet(coreSheetName);
		for (int i = 0; i < em; ++i) {
			if (i > 0) {
				++iRow;
			}
			for (int j1 = 0; j1 < en1; ++j1) {
				final Row row = sheet.createRow(iRow++);
				for (int j2 = 0; j2 < en2; ++j2) {
					final Cell cell = row.createCell(j2);
					cell.setCellValue(a[i][j1][j2]);
				}
			}
		}
	}

	public void createSheetIn(final XSSFWorkbook workBookX, final String coreSheetName, final int[][][][] a) {
		final int em = a.length;
		final boolean buildSeparateWorkbook = em >= 4;
		final XSSFWorkbook workBook;
		if (buildSeparateWorkbook) {
			workBook = new XSSFWorkbook();
		} else {
			workBook = workBookX == null ? _defaultWorkBook : workBookX;
		}
		if (em < 4) {
			for (int i = 0; i < em; ++i) {
				final String name = String.format("%s-%03d", coreSheetName, i);
				createSheetIn(workBook, name, a[i]);
			}
		}
		if (buildSeparateWorkbook) {
			close(workBook, coreSheetName);
		}
	}

	/** LabeledData. */
	public void createSheetLabeledData(final XSSFWorkbook workBookX, final String sheetName,
			final Poi.LabeledData labeledData) {
		final XSSFWorkbook workBook = workBookX == null ? _defaultWorkBook : workBookX;
		Poi.createLabeledDataSheet(workBook, sheetName, labeledData);
	}

	/** Real @@@ */
	public void createSheetRe(final XSSFWorkbook workBookX, final String coreSheetName, final double[] a) {
		final int em = a.length;
		final XSSFWorkbook workBook = workBookX == null ? _defaultWorkBook : workBookX;
		final Sheet sheet = workBook.createSheet(coreSheetName);
		int iRow = 0;
		final Row row = sheet.createRow(iRow++);
		for (int i = 0; i < em; ++i) {
			final Cell cell = row.createCell(i);
			cell.setCellValue(a[i]);
		}
	}

	public void createSheetRe(final XSSFWorkbook workBookX, final String coreSheetName, final double[][] a) {
		final int em = a.length;
		final int en = a[0].length;
		int iRow = 0;
		final XSSFWorkbook workBook = workBookX == null ? _defaultWorkBook : workBookX;
		try {
			final Sheet sheet = workBook.createSheet(coreSheetName);
			for (int i = 0; i < em; ++i) {
				final Row row = sheet.createRow(iRow++);
				for (int j = 0; j < en; ++j) {
					final Cell cell = row.createCell(j);
					cell.setCellValue(a[i][j]);
				}
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	public void createSheetRe(final XSSFWorkbook workBookX, final String coreSheetName, final double[][][] a) {
		final int em = a.length;
		final int en1 = a[0].length;
		final int en2 = a[0][0].length;
		int iRow = 0;
		final XSSFWorkbook workBook = workBookX == null ? _defaultWorkBook : workBookX;
		final Sheet sheet = workBook.createSheet(coreSheetName);
		for (int i = 0; i < em; ++i) {
			if (i > 0) {
				++iRow;
			}
			for (int j1 = 0; j1 < en1; ++j1) {
				final Row row = sheet.createRow(iRow++);
				for (int j2 = 0; j2 < en2; ++j2) {
					final Cell cell = row.createCell(j2);
					cell.setCellValue(a[i][j1][j2]);
				}
			}
		}
	}

	public void createSheetRe(final XSSFWorkbook workBookX, final String coreSheetName, final double[][][][] a) {
		final int em = a.length;
		final boolean buildSeparateWorkbook = em >= 4;
		final XSSFWorkbook workBook;
		if (buildSeparateWorkbook) {
			workBook = new XSSFWorkbook();
		} else {
			workBook = workBookX == null ? _defaultWorkBook : workBookX;
		}
		for (int i = 0; i < em; ++i) {
			final String name = String.format("%s-%03d", coreSheetName, i);
			createSheetRe(workBook, name, a[i]);
		}
		if (buildSeparateWorkbook) {
			close(workBook, coreSheetName);
		}
	}

	public XSSFWorkbook getDefaultWorkBook() {
		return _defaultWorkBook;
	}

	private File getFile(final String fileName) {
		final boolean ok = fileName.toUpperCase().endsWith(_UcListSuffix);
		final String name = ok ? fileName : (fileName + _UcListSuffix);
		try {
			final File f = new File(_outputDir, name);
			f.delete();
			return f;
		} catch (final Exception e1) {
		}
		return null;
	}

}
