package com.skagit.utils.netCdf;

/** Good article for setting up logging:
 * https://docs.unidata.ucar.edu/netcdf-java/5.9/userguide/using_netcdf_java_artifacts.html
 */
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.logging.LogManager;

import com.google.common.collect.ImmutableList;

import ucar.ma2.Array;
import ucar.ma2.ArrayDouble;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.util.CancelTask;
import ucar.nc2.write.Ncdump;
import ucar.nc2.write.NetcdfFileFormat;
import ucar.nc2.write.NetcdfFormatWriter;

public class Example {

	static {
		final Class<? extends Example> clazz = Example.class;
		final InputStream inputStream = clazz.getResourceAsStream("Logging.properties");
		final LogManager logManager = LogManager.getLogManager();
		try {
			logManager.readConfiguration(inputStream);
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	public static void Read(final String filePath) {
		try (NetcdfFile netCdfFile = NetcdfFiles.open(filePath)) {
			System.out.println("Opened: " + filePath);
			final ImmutableList<Attribute> globalAttributeContainer = netCdfFile.getGlobalAttributes();
			for (final Attribute attribute : globalAttributeContainer) {
				System.out.println("Global Attribute: " + attribute.getName() + "->" + attribute.getStringValue());
			}
			/** List all variables. */
			final ImmutableList<Variable> variables = netCdfFile.getVariables();
			for (final Variable variable : variables) {
				System.out.println("Variable: " + variable.getFullName() + ", Type: " + variable.getDataType());
				final AttributeContainer attributeContainer = variable.attributes();
				for (final Attribute attribute : attributeContainer) {
					System.out.println("Attribute: " + attribute.getName() + "->" + attribute.getStringValue());
				}
			}
			/** Read the temperature variable. */
			final Variable temperatureVariable = netCdfFile.findVariable("temperature");
			if (temperatureVariable != null) {
				final Array data = temperatureVariable.read(); // Read all data
				System.out.println(
						"Temperature data shape: " + java.util.Arrays.toString(temperatureVariable.getShape()));
				System.out.println("First value: " + data.getDouble(0));
			} else {
				System.out.println("Variable 'temperature' not found.");
			}
		} catch (final IOException e) {
			System.err.println("Error reading NetCDF file: " + e.getMessage());
		}
	}

	final private static String _OutputExtension = "txt";

	public static void AsciiDump(final String filePath) {
		final File inputFile = new File(filePath);
		final File inputFileParent = inputFile.getParentFile();
		final String inputFileName = inputFile.getName();
		String fileNameCore = null;
		final int lastDot = inputFileName.lastIndexOf('.');
		if (lastDot == -1) {
			fileNameCore = inputFileName;
		} else {
			String nonSuffix = null;
			String suffix = null;
			try {
				nonSuffix = inputFileName.substring(0, lastDot);
				suffix = inputFileName.substring(lastDot + 1);
				fileNameCore = String.format("%s%s", nonSuffix, suffix.toUpperCase());
			} catch (final Exception e) {
				fileNameCore = lastDot == 0 ? inputFileName.substring(1) : nonSuffix;
			}
		}

		for (int k = -1;; ++k) {
			final String outputFileName;
			if (k == -1) {
				outputFileName = String.format("%s.%s", fileNameCore, _OutputExtension);
			} else {
				outputFileName = String.format("%s@%02d.%s", fileNameCore, k, _OutputExtension);
			}
			final File outputFile = new File(inputFileParent, outputFileName);
			if (!outputFile.exists()) {
				try (final Writer writer = new FileWriter(outputFile)) {
					try (final NetcdfFile netCdfFile = NetcdfFiles.open(inputFile.getCanonicalPath())) {
						Ncdump.ncdump(netCdfFile, "-cdl -vall", writer, new CancelTask() {

							@Override
							public boolean isCancel() {
								// TODO Auto-generated method stub
								return false;
							}

							@Override
							public boolean isDone() {
								// TODO Auto-generated method stub
								return false;
							}

							@Override
							public void setDone(final boolean done) {
								// TODO Auto-generated method stub

							}

							@Override
							public void setError(final String msg) {
								// TODO Auto-generated method stub

							}

							@Override
							public void setProgress(final String msg, final int progress) {
								// TODO Auto-generated method stub

							}
						});
					} catch (final IOException e) {
						e.printStackTrace();
					}
				} catch (final IOException e) {
					e.printStackTrace();
				}
				return;
			}
		}
	}

	public static void Write(final String filePath) {
		final int nTimes = 5, nLats = 3, nLons = 4, nInLastDim = 7;
		try {
			/** Create a NetCDF-4 file. */
			final NetcdfFormatWriter.Builder writerBuilder = NetcdfFormatWriter
					.createNewNetcdf4(NetcdfFileFormat.NETCDF4, filePath, null);
			/** Create dimensions. */
			final Dimension timeDim = writerBuilder.addDimension("time", nTimes);
			final Dimension latDim = writerBuilder.addDimension("lat", nLats);
			final Dimension lonDim = writerBuilder.addDimension("lon", nLons);
			final Dimension lastDim = writerBuilder.addDimension("lastDim", nInLastDim);

			/** Create an example array for the variable temperature. */
			final ArrayDouble.D4 temperatureData = new ArrayDouble.D4(timeDim.getLength(), latDim.getLength(),
					lonDim.getLength(), lastDim.getLength());
			for (int k0 = 0; k0 < timeDim.getLength(); k0++) {
				for (int k1 = 0; k1 < latDim.getLength(); k1++) {
					for (int k2 = 0; k2 < lonDim.getLength(); k2++) {
						for (int k3 = 0; k3 < lonDim.getLength(); k3++) {
							temperatureData.set(k0, k1, k2, k3, k0 * 10d + 20.0 + k1 + k2 + 3d * k3); // Example values
						}
					}
				}
			}

			/** Add a global attribute. */
			writerBuilder.addAttribute(new Attribute("GlblAttribute", "This is a global attribute."));

			/** Create a variable and add an attribute to that variable. */
			final Attribute attribute = new Attribute("units", "Celsius");
			final Variable.Builder<?> variableBuilder = writerBuilder.addVariable("temperature", DataType.DOUBLE,
					"time lat lon lastDim");
			variableBuilder.addAttribute(attribute);
			/**
			 * We are done adding dimensions, variables, and attributes. Build and open
			 * writer. Once it's built, we are done using the writerBuilder except for
			 * closing it. Then populate the variables.
			 */
			try (NetcdfFormatWriter writer = writerBuilder.build()) {
				final Variable temperatureVariable = writer.findVariable("temperature");
				writer.write(temperatureVariable, temperatureData);
			} catch (final InvalidRangeException e) {
				e.printStackTrace();
			}
			System.out.println("NetCDF file created: " + filePath);
		} catch (final IOException e) {
			System.err.println("Error writing NetCDF file: " + e.getMessage());
		}
	}

	public static void main(final String[] args) {
		final String filePath = "Example.nc";
		AsciiDump(filePath);
	}
}
