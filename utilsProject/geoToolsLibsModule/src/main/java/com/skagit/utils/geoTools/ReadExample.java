/*
 *    GeoTools Sample code and Tutorials by Open Source Geospatial Foundation, and others
 *    https://docs.geotools.org
 *
 *    To the extent possible under law, the author(s) have dedicated all copyright
 *    and related and neighboring rights to this software to the public domain worldwide.
 *    This software is distributed without any warranty.
 *
 *    You should have received a copy of the CC0 Public Domain Dedication along with this
 *    software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package com.skagit.utils.geoTools;

import java.io.File;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.geotools.api.data.FileDataStore;
import org.geotools.api.data.FileDataStoreFinder;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.style.Style;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.styling.SLD;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.data.JFileDataStoreChooser;

public class ReadExample {

	private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(ReadExample.class);

	public static void main(final String[] args) throws Exception {
		/** Display a data store file chooser dialog for shapefiles. */
		LOGGER.info("Quickstart");
		LOGGER.config("Welcome Developers");
		LOGGER.info("java.util.logging.config.file=" + System.getProperty("java.util.logging.config.file"));

		final JFileDataStoreChooser dialog = new JFileDataStoreChooser("shp");

		final FileFilter shpFilter = new FileFilter() {
			@Override
			public boolean accept(final File f) {
				if (f.isDirectory()) {
					return true;
				}
				return f.getName().toLowerCase().endsWith(".shp");
			}

			@Override
			public String getDescription() {
				return "ESRI(tm) Shapefiles (*.shp)";
			}
		};
		dialog.addChoosableFileFilter(shpFilter);
		dialog.setAcceptAllFileFilterUsed(false);
		dialog.setFileFilter(shpFilter);

		dialog.setCurrentDirectory(new File(
				"C:\\Users\\tkrat\\JavaDevelopment\\git\\UtilsProjectRepo\\utilsProject\\geoToolsLibsModule\\50m_cultural"));
		File file = null;
		if (dialog.showOpenDialog(/* parent= */null) == JFileChooser.APPROVE_OPTION) {
			file = dialog.getSelectedFile();
		}

		if (file == null) {
			return;
		}
		LOGGER.config("File selected " + file);

		final FileDataStore store = FileDataStoreFinder.getDataStore(file);
		final SimpleFeatureSource featureSource = store.getFeatureSource();

		/** Create a map content and add our shapefile to it. */
		final MapContent map = new MapContent();
		map.setTitle("GeoTools.ReadExample");

		final Style style = SLD.createSimpleStyle(featureSource.getSchema());
		final Layer layer = new FeatureLayer(featureSource, style);
		map.addLayer(layer);

		/** Display the map. */
		JMapFrame.showMap(map);
	}
}
