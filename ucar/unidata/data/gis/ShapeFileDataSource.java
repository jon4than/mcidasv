/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2024
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * http://www.ssec.wisc.edu/mcidas
 * 
 * All Rights Reserved
 * 
 * McIDAS-V is built on Unidata's IDV and SSEC's VisAD libraries, and
 * some McIDAS-V source code is based on IDV and VisAD source code.  
 * 
 * McIDAS-V is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * McIDAS-V is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */

package ucar.unidata.data.gis;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.data.*;
import ucar.unidata.geoloc.LatLonPointImpl;


import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.gis.shapefile.DbaseData;
import ucar.unidata.gis.shapefile.DbaseFile;
import ucar.unidata.util.CacheManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.JobManager;
import ucar.unidata.util.Misc;

import ucar.unidata.util.TwoFacedObject;

import ucar.visad.MapFamily;

import ucar.visad.ShapefileAdapter;

import ucar.visad.data.MapSet;

import visad.*;

import java.awt.*;


import java.awt.geom.Rectangle2D;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import java.net.URL;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Vector;


import javax.swing.*;


/**
 * DataSource for Shapefiles.
 *
 * @author IDV development team
 * @version $Revision: 1.28 $ $Date: 2007/04/16 20:34:52 $
 */
public class ShapeFileDataSource extends FilesDataSource {
    
    private static final Logger logger =
        LoggerFactory.getLogger(ShapeFileDataSource.class);
    
    /** _more_ */
    private static MapFamily mapFamily = new MapFamily("IDV maps");

    /** Property id for the dbfile */
    public static final String PROP_DBFILE = "PROP_DBFILE";

    /** The db file */
    private DbaseFile dbFile;

    /** The data. We cache this here ourselves */
    private Data shapefileData;

    /** _more_ */
    double coarseness = 0;

    /** _more_ */
    double lastCoarseness = 0;

    /** _more_ */
    private JComboBox coarsenessCbx;

    /**
     * Dummy constructor so this object can get unpersisted.
     */
    public ShapeFileDataSource() {}


    /**
     * Create a ShapeFileDataSource from the specification given.
     *
     * @param descriptor          descriptor for the data source
     * @param source of file      file name (or directory)
     * @param properties          extra properties
     *
     * @throws VisADException     some problem occurred creating data
     */
    public ShapeFileDataSource(DataSourceDescriptor descriptor,
                               String source, Hashtable properties)
            throws VisADException {
        super(descriptor, source, "Shapefile data source", properties);
        initShapeFileDataSource();
    }


    /**
     * _more_
     */
    public void reloadData() {
        shapefileData = null;
        dbFile        = null;
        super.reloadData();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean canDoGeoSelection() {
        return true;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected boolean canDoGeoSelectionStride() {
        return false;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected JComponent doMakeGeoSubsetPropertiesComponent() {
        JComponent comp = super.doMakeGeoSubsetPropertiesComponent();
        if (coarsenessCbx == null) {
            Object   selected = null;
            Vector   items    = new Vector();
            double[] values   = new double[] {
                0, 1, 2, 3, 4, 5, 6, 7, 8, 9
            };
            String[] names    = new String[] {
                "Full resolution", "........", ".......", "......", ".....",
                "....", "...", "..", ".", "Really coarse"
            };
            for (int i = 0; i < values.length; i++) {
                TwoFacedObject tfo = new TwoFacedObject(names[i],
                                         new Double(values[i]));
                if (values[i] == coarseness) {
                    selected = tfo;
                }
                items.add(tfo);
            }
            coarsenessCbx = new JComboBox(items);
            if (selected != null) {
                coarsenessCbx.setSelectedItem(selected);
            }
        }
        return GuiUtils.topCenter(
            GuiUtils.inset(
                GuiUtils.left(GuiUtils.label("Resolution: ", coarsenessCbx)),
                5), comp);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean applyProperties() {
        if (coarsenessCbx != null) {
            TwoFacedObject tfo =
                (TwoFacedObject) coarsenessCbx.getSelectedItem();
            coarseness = ((Double) tfo.getId()).doubleValue();
        }
        if ( !super.applyProperties()) {
            return false;
        }
        return true;
    }

    /**
     * Is this data source capable of saving its data to local disk
     *
     * @return Can save to local disk
     */
    public boolean canSaveDataToLocalDisk() {
        return !isFileBased();
    }


    /**
     * Initialize if being unpersisted.
     */
    public void initAfterUnpersistence() {
        
        
        //From a legacy bundle
        if (sources == null) {
            sources = Misc.newList(getName());
        }
        super.initAfterUnpersistence();
        initShapeFileDataSource();
    }


    /**
     * Initialization method
     */
    private void initShapeFileDataSource() {}

    /** categories */
    private List categories = DataCategory.parseCategories("GIS-SHAPEFILE",
                                  false);

    /**
     * Create the data choices associated with this source.
     */
    protected void doMakeDataChoices() {
        File file = new File(getSource());
        if (file.isDirectory()) {
            CompositeDataChoice composite = new CompositeDataChoice(this, "",
                                                getSource(), getSource(),
                                                null);
            walkTree(file, composite);
            addDataChoice(composite);
        } else {
            String name = getProperty(PROP_TITLE, (String) null);
            if (name == null) {
                name = getProperty(PROP_NAME, (String) null);
            }
            if (name == null) {
                name = IOUtil.getFileTail(getSource());
            }
            Hashtable props = Misc.newHashtable(DataChoice.PROP_ICON,
                                  "/auxdata/ui/icons/Map16.gif");
            addDataChoice(new DirectDataChoice(this, getSource(), name, name,
                    categories, props));
//            DirectDataChoice ddc = new DirectDataChoice(this, name, name, name,
//                categories, props);
//            addDataChoice(ddc);
        }
    }

    /**
     * Walk a directory tree and create children DataChoices
     *
     * @param directory      directory to walk
     * @param parent         parent DataChoice
     */
    private void walkTree(File directory, CompositeDataChoice parent) {
        File[] subfiles = directory.listFiles();
        for (int i = 0; i < subfiles.length; i++) {
            if (subfiles[i].isDirectory()) {
                String dirName = IOUtil.getFileTail(subfiles[i].toString());
                CompositeDataChoice composite = new CompositeDataChoice(this,
                                                    "", dirName, dirName,
                                                    null);
                parent.addDataChoice(composite);
                walkTree(subfiles[i], composite);
            } else if (subfiles[i].toString().toLowerCase().endsWith(
                    ".shp")) {
                String name = subfiles[i].toString();
                String shortName =
                    IOUtil.stripExtension(IOUtil.getFileTail(name));
                parent.addDataChoice(new DirectDataChoice(this, name,
                        shortName, shortName, categories));
            }
        }
    }
    
    /**
     * Fix for loading shape files from zipped bundles.
     *
     * <p>The problem is that the {@link DataChoice} associated with a
     * {@link ucar.unidata.idv.control.DisplayControlImpl} may have an
     * {@link DataChoice#id} that points to the original file, rather than
     * the one from the zipped bundle.</p>
     *
     * <p>The solution is to check {@link Path#getFileName()} of {@code choice}
     * against all of the paths in {@link #getSources()}. If there was a
     * matching file name, the ID of {@code choice} will be changed to the
     * match from {@code getSources()}.</p>
     *
     * <p>Put another way:
     * <code>
     *   Result of choice.getId():
     *     /home/user/data/ne_10m_lakes/ne_10m_lakes.shp
     *
     *   Result from getSources():
     *     /home/user/McIDAS-V/tmp/1478190157171_0_08101960429484834_13/ne_10m_lakes.shp
     *
     *   Because both strings contain "ne_10m_lakes.shp", we consider them the
     *   same.
     * </code></p>
     *
     * @param choice {@code DataChoice} to examine. Cannot be {@code null}.
     */
    private void fixBundledId(DataChoice choice) {
        String id = choice.getStringId();
        Path fromChoice = Paths.get(id).getFileName();
        for (String source : (List<String>)getSources()) {
            Path tmp = Paths.get(source).getFileName();
            if (Objects.equals(fromChoice, tmp)) {
                choice.setId(source);
            }
        }
    }
    
    /**
     * Actually get the data identified by the given DataChoice. The default is
     * to call the getDataInner that does not take the requestProperties. This
     * allows other, non unidata.data DataSource-s (that follow the old API)
     * to work.
     *
     * @param dataChoice        The data choice that identifies the requested
     *                          data.
     * @param category          The data category of the request.
     * @param dataSelection     Identifies any subsetting of the data.
     * @param requestProperties Hashtable that holds any detailed request
     *                          properties.
     *
     * @return The visad.Data object
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection,
                                Hashtable requestProperties)
            throws VisADException, RemoteException {

        GeoSelection    geoSelection = ((dataSelection != null)
                                        ? dataSelection.getGeoSelection()
                                        : null);
        GeoLocationInfo bbox         = ((geoSelection == null)
                                        ? null
                                        : geoSelection.getBoundingBox());


        LatLonRect      llr          = ((bbox != null)
                                        ? bbox.getLatLonRect()
                                        : null);


        boolean         useDbFile    = true;
        boolean         amSubsetting = false;

        Rectangle2D     box          = null;
        if (llr != null) {
            LatLonPointImpl ul = llr.getUpperLeftPoint();
            LatLonPointImpl lr = llr.getLowerRightPoint();
            box = new Rectangle2D.Double(ul.getLongitude(),
                                         ul.getLatitude() - llr.getHeight(),
                                         llr.getWidth(), llr.getHeight());
            shapefileData = null;
            amSubsetting  = true;
            useDbFile     = false;
        }

        if (coarseness != lastCoarseness) {
            shapefileData = null;
            amSubsetting  = true;
            useDbFile     = false;
        }
        lastCoarseness = coarseness;
        
        if (haveBeenUnPersisted && haveSources()) {
            fixBundledId(dataChoice);
        }
        
        String filename = (String) dataChoice.getId();
        byte[] bytes    = null;
        try {
            if (shapefileData == null) {
                //If its not a shp or zip file then try it with the mapFamily
                if ( !IOUtil.hasSuffix(filename, ".shp")
                        && !IOUtil.hasSuffix(filename, ".zip")
                        && !IOUtil.hasSuffix(filename, ".tcl")) {
                    try {
                        URL url = IOUtil.getURL(filename, getClass());
                        shapefileData = (url == null)
                                        ? (SampledSet) mapFamily.open(
                                            filename)
                                        : (SampledSet) mapFamily.open(url);

                        return shapefileData;
                    } catch (Exception exc) {
                        String msg = String.format("Problem opening '%s' as URL", filename);
                        logger.error(msg, exc);
                    }
                }


                if (getProperty(PROP_CACHEABLE, false)) {
                    bytes = CacheManager.getCachedFile("ShapeFileDataSource",
                            filename);
                }

                if (bytes == null) {
                    Object loadId =
                        JobManager.getManager().startLoad("Map File");
                    bytes = IOUtil.readBytes(IOUtil.getInputStream(filename,
                            getClass()), loadId);
                    JobManager.getManager().stopLoad(loadId);
                }

                if (bytes == null) {
                    return null;
                }

                InputStream inputStream = new ByteArrayInputStream(bytes, 0,
                                              bytes.length);
                ShapefileAdapter sfa = new ShapefileAdapter(inputStream,
                                           filename, box, coarseness);


                dbFile = sfa.getDbFile();
                //If this is a .shp file then try to read in the dbf file
                if (useDbFile && (dbFile == null)
                        && IOUtil.hasSuffix(filename, ".shp")) {
                    InputStream dbfInputStream = null;
                    try {
                        String dbFilename = IOUtil.stripExtension(filename)
                                            + ".dbf";
                        dbfInputStream = IOUtil.getInputStream(dbFilename,
                                getClass());
                    } catch (Exception exc) {}
                    if (dbfInputStream != null) {
                        dbFile = new DbaseFile(dbfInputStream);
                        dbFile.loadHeader();
                        dbFile.loadData();
                    }
                }
                shapefileData = sfa.getData();
                if (useDbFile) {
                    setProperties(shapefileData, dbFile);
                }
            }

            if (useDbFile && (requestProperties != null)
                    && (dbFile != null)) {
                requestProperties.put(PROP_DBFILE, dbFile);
            }
            Data result = shapefileData;

            if (amSubsetting) {
                shapefileData = null;
            }

            return result;
        } catch (Exception exc) {
            logException("Reading shapefile: " + filename, exc, bytes);
        }
        return null;
    }



    /**
     * _more_
     *
     * @param filename _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Data readMap(String filename) throws Exception {
        if ( !IOUtil.hasSuffix(filename, ".shp")
                && !IOUtil.hasSuffix(filename, ".zip")) {
            try {
                URL  url  = IOUtil.getURL(filename,
                                          ShapeFileDataSource.class);
                Data data = (url == null)
                            ? (SampledSet) mapFamily.open(filename)
                            : (SampledSet) mapFamily.open(url);
                return data;
            } catch (Exception ignore) {}
        }


        byte[] bytes = IOUtil.readBytes(IOUtil.getInputStream(filename,
                           ShapeFileDataSource.class));
        if (bytes == null) {
            return null;
        }
        ShapefileAdapter sfa =
            new ShapefileAdapter(new ByteArrayInputStream(bytes, 0,
                bytes.length), filename);

        Data data = sfa.getData();
        return data;
    }


    /**
     * _more_
     *
     * @param shapefileData _more_
     * @param dbFile _more_
     */
    private void setProperties(Data shapefileData, DbaseFile dbFile) {
        if ((dbFile == null) || (shapefileData == null)
                || !(shapefileData instanceof UnionSet)) {
            return;
        }
        SampledSet[] sets = ((UnionSet) shapefileData).getSets();
        //if ((sets.length == 0) || !(sets[0] instanceof MapSet)) {
        if (sets.length == 0) {
            return;
        }
        boolean noMapSets = true;
        for (int idx = 0; idx < sets.length; idx++) {
        	if (sets[idx] instanceof MapSet) {
        		noMapSets = false;
        		break;
        	}
        }
        
        if (noMapSets) return;
        
        int  numFields     = dbFile.getNumFields();
        List propertyNames = new ArrayList();

        for (int fieldIdx = 0; fieldIdx < numFields; fieldIdx++) {
            String    fieldName = dbFile.getFieldName(fieldIdx);
            DbaseData dbData    = dbFile.getField(fieldIdx);
            List      values    = dbData.asList();
            if (values.size() != sets.length) {
                /*throw new IllegalArgumentException("DBfile size:"
                        + values.size() + " != number of map lines:"
                        + sets.length);  */

                System.err.println(" WARNING:" + "DBfile size:"
                        + values.size() + " != number of map lines:"
                        + sets.length);
                return;
            }
            for (int i = 0; i < sets.length; i++) {
                if (sets[i] instanceof MapSet) {
                    MapSet mapSet = (MapSet) sets[i];
                    mapSet.setProperty(fieldName, values.get(i));
                }
            }
        }
        for (int fieldIdx = 0; fieldIdx < numFields; fieldIdx++) {
            String fieldName = dbFile.getFieldName(fieldIdx);
            propertyNames.add(fieldName);
        }

        //Some of these might be union sets
        for (int i = 0; i < sets.length; i++) {
            if (sets[i] instanceof MapSet) {
                MapSet mapSet = (MapSet) sets[i];
                mapSet.setPropertyNames(propertyNames);
            }
        }




    }


    /**
     * See if this DataSource should cache or not
     *
     * @param data   Data to cache
     * @return  false
     */
    protected boolean shouldCache(Data data) {
        return false;
    }



    /**
     * Create a list of times for this data source.  Since shapefiles
     * don't have any times, return an empty List.
     *
     * @return  an empty List
     */
    protected List doMakeDateTimes() {
        return new ArrayList();
    }

    /**
     *  Set the Coarseness property.
     *
     *  @param value The new value for Coarseness
     */
    public void setCoarseness(double value) {
        this.coarseness = value;
    }

    /**
     *  Get the Coarseness property.
     *
     *  @return The Coarseness
     */
    public double getCoarseness() {
        return this.coarseness;
    }



}
