package com.skagit.util.poi;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.openxml4j.exceptions.InvalidOperationException;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.skagit.utils.lsFormatter.LsFormatter;

public class Poi {
	public static class LabeledData {
		public String[] _labels;
		public Object[] _data;

		public LabeledData(final String[] labels, final Object[] data) {
			_labels = labels;
			_data = data;
		}
	}

	private static void autoFit(final Sheet sheet) {
		final TreeSet<Integer> colIndexes = new TreeSet<>();
		final TreeMap<Integer, int[]> minWidths = new TreeMap<>();
		final TreeSet<Integer> needMinWidths = new TreeSet<>();
		final Iterator<Row> rowIterator = sheet.rowIterator();
		while (rowIterator.hasNext()) {
			final Row row = rowIterator.next();
			final Iterator<Cell> cellIterator = row.cellIterator();
			while (cellIterator.hasNext()) {
				final Cell cell = cellIterator.next();
				final int colIndex = cell.getColumnIndex();
				colIndexes.add(colIndex);
				int[] minWidth = minWidths.get(colIndex);
				if (minWidth == null) {
					minWidth = new int[] { 0 };
					minWidths.put(colIndex, minWidth);
				}
				final int thisWidth;
				final CellType cellType = cell.getCellType();
				if (cellType == CellType.NUMERIC) {
					final double d = cell.getNumericCellValue();
					final String s = LsFormatter.StandardFormat(d);
					thisWidth = s.length();
				} else if (cellType == CellType.STRING) {
					final String s = cell.getStringCellValue();
					thisWidth = s.length();
				} else {
					thisWidth = 4;
				}
				minWidth[0] = Math.max(minWidth[0], thisWidth);
			}
		}
		for (final int colIndex : colIndexes) {
			if (needMinWidths.contains(colIndex)) {
				final int[] minWidth = minWidths.get(colIndex);
				sheet.setColumnWidth(colIndex, minWidth[0] * 256);
			} else {
				sheet.autoSizeColumn(colIndex);
			}
		}
	}

	public static void autoFit(final Workbook workBook) {
		final int nSheets = workBook.getNumberOfSheets();
		for (int k = 0; k < nSheets; ++k) {
			final Sheet sheet = workBook.getSheetAt(k);
			autoFit(sheet);
		}
	}

	public static void createLabeledDataSheet(final Workbook workBook, final String sheetName,
			final LabeledData labeledData) {
		final Sheet sheet = workBook.createSheet(sheetName);
		final int n = labeledData._labels.length;
		int iRow = 0;
		for (int k = 0; k < n; ++k) {
			final Row row = sheet.createRow(iRow++);
			int iCol = 0;
			row.createCell(iCol++).setCellValue(labeledData._labels[k]);
			final Object o = labeledData._data[k];
			final Cell cell = row.createCell(iCol++);
			if (o instanceof Integer) {
				cell.setCellValue(((Integer) o).intValue());
			} else if (o instanceof Boolean) {
				cell.setCellValue(((Boolean) o).booleanValue());
			} else if (o instanceof Float) {
				cell.setCellValue(((Float) o).floatValue());
			} else if (o instanceof Double) {
				cell.setCellValue(((Double) o).doubleValue());
			} else {
				cell.setCellValue(o.toString());
			}
		}
	}

	public static Row getOrCreateRow(final Sheet sheet, final int iRow) {
		final Row row = sheet.getRow(iRow);
		return row != null ? row : sheet.createRow(iRow);
	}

	public static Sheet getSheet(final String filePath, final String sheetName) {
		/** Try with no suffix, "xls," "xlsx," and "csv." */
		for (final String suffix : new String[] { "", ".xls", ".xlsx", "csv" }) {
			final Sheet sheet = getSheet2(filePath + suffix, sheetName);
			if (sheet != null) {
				return sheet;
			}
		}
		return null;
	}

	private static Sheet getSheet2(final String filePath, final String sheetName) {
		/** Try with no suffix, "xls," and "xlsx." */
		final File inputFile = new File(filePath);
		if (!inputFile.exists() || inputFile.isDirectory()) {
			return null;
		}
		try (FileInputStream fis = new FileInputStream(inputFile)) {
			try (final XSSFWorkbook xssfWorkBook = new XSSFWorkbook(fis)) {
				return xssfWorkBook.getSheet(sheetName);
			} catch (final IOException e) {
			}
		} catch (final InvalidOperationException | IOException e) {
		}
		/**
		 * We tried the XSSF style and we either succeeded and returned, or we failed
		 * and cleaned up. Start over with HSSF.
		 */
		try (FileInputStream fis = new FileInputStream(inputFile)) {
			try (final POIFSFileSystem poiFileStream = new POIFSFileSystem(fis)) {
				try (final HSSFWorkbook hssfWorkBook = new HSSFWorkbook(poiFileStream)) {
					return hssfWorkBook.getSheet(sheetName);
				} catch (final IOException e) {
				}
			} catch (final IOException e) {
			}
		} catch (final InvalidOperationException | IOException e) {
		}
		return null;
	}

	public static String[] getSheetNames(final String fileNameCore) {
		/** Try with no suffix, "xls," and "xlsx." */
		for (final String suffix : new String[] { "", ".xls", ".xlsx" }) {
			final String fileName = fileNameCore + suffix;
			final File inputFile = new File(fileName);
			try (final FileInputStream fis = new FileInputStream(inputFile)) {
				try (final XSSFWorkbook xssfWorkBook = new XSSFWorkbook(fis)) {
					try {
						final int nSheets = xssfWorkBook.getNumberOfSheets();
						final String[] sheetNames = new String[nSheets];
						for (int k = 0; k < nSheets; ++k) {
							sheetNames[k] = xssfWorkBook.getSheetAt(k).getSheetName();
						}
						return sheetNames;
					} catch (final InvalidOperationException | POIXMLException e) {
						/** xssfWorkbook just isn't working out. */
					}
				} catch (final IOException e) {
					/** xssfWorkbook just isn't working out. */
				}
				/*
				 * We tried the XSSF style and we either succeeded and returned, or we failed
				 * and cleaned up. Start over with HSSF.
				 */
				try (final POIFSFileSystem poiFs = new POIFSFileSystem(fis)) {
					try (HSSFWorkbook hssfWorkBook = new HSSFWorkbook(poiFs)) {
						try {
							final int nSheets = hssfWorkBook.getNumberOfSheets();
							final String[] sheetNames = new String[nSheets];
							for (int k = 0; k < nSheets; ++k) {
								sheetNames[k] = hssfWorkBook.getSheetAt(k).getSheetName();
							}
							return sheetNames;
						} catch (final InvalidOperationException | POIXMLException e) {
							/** hssfWorkbook just isn't working out. */
						}
					} catch (final IOException e) {
						/** hssfWorkbook just isn't working out. */
					}
				} catch (final IOException e) {
					/** poiFs just isn't working out. */
				}
			} catch (final IOException e) {
				/** fis just isn't working out. */
			}
		}
		return null;
	}
	
	public static void main(String[] args) {
		
	}
}
