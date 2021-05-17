/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2021
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

package edu.wisc.ssec.mcidasv.data;

import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.list;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.data.CompositeDataChoice;
import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.DirectDataChoice;
import ucar.unidata.data.FilesDataSource;
import ucar.unidata.util.Misc;
import visad.Data;
import visad.VisADException;

/**
 * This is an implementation that will read in a generic data file and return
 * a single Data choice that is a VisAD Data object.
 */
public class FlatFileDataSource extends FilesDataSource {

    private static final Logger logger =
        LoggerFactory.getLogger(FlatFileDataSource.class);

    /**
     *  Parameterless ctor
     */
    public FlatFileDataSource() {}

    /**
     * Just pass through to the base class the ctor arguments.
     * @param descriptor    Describes this data source, has a label etc.
     * @param filename      This is the filename (or url) that
     *                      points to the actual data source.
     * @param properties General properties used in the base class
     *
     * @throws VisADException   problem getting the data
     */
    public FlatFileDataSource(DataSourceDescriptor descriptor,
                              String filename, Hashtable properties)
            throws VisADException {
        super(descriptor, filename, "Image flat file data source", properties);
        Path p = Paths.get(filename);
        setName(p.getName(p.getNameCount() - 1).toString());
        logger.trace("descriptor: '{}', filename: '{}', properties: '{}'", descriptor, filename, properties);
    }

    /**
     * This method is called at initialization time and  should create
     * a set of {@link ucar.unidata.data.DirectDataChoice}-s  and add them
     * into the base class managed list of DataChoice-s with the method
     * addDataChoice.
     */
    protected void doMakeDataChoices() {
//        String xmlFile = getFilePath();
//        List bandsDefault = new ArrayList();
//        bandsDefault.add("Band 1");
//        String name = getProperty("FLAT.NAME", "Unknown name");
//        List bandNames = (List)getProperty("FLAT.BANDNAMES", bandsDefault);
//        List bandFiles = (List)getProperty("FLAT.BANDFILES", bandsDefault);
//
//        int lines = getProperty("FLAT.LINES", (int)0);
//        int elements = getProperty("FLAT.ELEMENTS", (int)0);
//        String unit = getProperty("FLAT.UNIT", "");
//        int stride = getProperty("FLAT.STRIDE", (int)1);
//
////        if (bandNames.size() == bandFiles.size()) {
////            for (int i=0; i<bandNames.size(); i++) {
////                System.out.println(bandNames.get(i) + ": " + bandFiles.get(i));
////            }
////        } else {
////            System.err.println("bandNames: " + bandNames.toString());
////            System.err.println("bandFiles: " + bandFiles.toString());
////            System.err.println("Huh... bandNames (" + bandNames.size() + ") and bandFiles (" + bandFiles.size() + ") should be the same size");
////        }
//
//        Hashtable imageProps = Misc.newHashtable(DataChoice.PROP_ICON, "/auxdata/ui/icons/Earth16.gif");

        // Navigation
        String navType = getProperty("NAV.TYPE", "UNKNOWN");
        double ulLat = 0;
        double ulLon = 0;
        double lrLat = 0;
        double lrLon = 0;
        String latFile = null;
        String lonFile = null;
        switch (navType) {
            case "FILES":
                latFile = getProperty("FILE.LAT", "");
                lonFile = getProperty("FILE.LON", "");
                break;
            case "BOUNDS":
                ulLat = getProperty("BOUNDS.ULLAT", (double)0);
                ulLon = getProperty("BOUNDS.ULLON", (double)0);
                lrLat = getProperty("BOUNDS.LRLAT", (double)0);
                lrLon = getProperty("BOUNDS.LRLON", (double)0);
                break;
            default:
                logger.warn("unknown navType '{}'", navType);
        }

        int scale = getProperty("NAV.SCALE", 1);
        boolean eastPositive = getProperty("NAV.EASTPOS", false);

        // Format
        String formatType = getProperty("FORMAT.TYPE", "UNKNOWN");
        switch (formatType) {
            case "BINARY":
                handleBinaryFormat(ulLat, ulLon, lrLat, lrLon, latFile, lonFile, scale, eastPositive);
                break;
            case "ASCII":
                handleAsciiFormat(ulLat, ulLon, lrLat, lrLon, latFile, lonFile, scale, eastPositive);
                break;
            case "IMAGE":
                handleImageFormat(ulLat, ulLon, lrLat, lrLon, latFile, lonFile, scale, eastPositive);
                break;
            default:
                logger.warn("unknown formatType '{}'", formatType);
                break;
        }
    }

    private void handleImageFormat(double ulLat, double ulLon, double lrLat, double lrLon, String latFile, String lonFile, int scale, boolean eastPositive) {
        List<String> bandsDefault = list("Band 1");
        List<String> bandNames = (List<String>)getProperty("FLAT.BANDNAMES", bandsDefault);
        List<String> bandFiles = (List<String>)getProperty("FLAT.BANDFILES", bandsDefault);

        int lines = getProperty("FLAT.LINES", 0);
        int elements = getProperty("FLAT.ELEMENTS", 0);
        String unit = getProperty("FLAT.UNIT", "");
        int stride = getProperty("FLAT.STRIDE", 1);

        Hashtable imageProps = Misc.newHashtable(DataChoice.PROP_ICON, "/auxdata/ui/icons/Earth16.gif");

        List categories = DataCategory.parseCategories("RGBIMAGE", false);
        FlatFileReader dataChoiceData = new FlatFileReader(bandFiles.get(0), lines, elements);
        dataChoiceData.setImageInfo();
        dataChoiceData.setUnit(unit);
        dataChoiceData.setEastPositive(eastPositive);
        dataChoiceData.setStride(stride);
        if ((latFile != null) && (lonFile != null)) {
            dataChoiceData.setNavFiles(latFile, lonFile, scale);
        } else {
            dataChoiceData.setNavBounds(ulLat, ulLon, lrLat, lrLon);
        }
        String bandName = bandNames.get(0);
        DirectDataChoice ddc = new DirectDataChoice(this, dataChoiceData, bandName, bandName, categories, imageProps);
        addDataChoice(ddc);
    }

    private void handleAsciiFormat(double ulLat, double ulLon, double lrLat, double lrLon, String latFile, String lonFile, int scale, boolean eastPositive) {
        String name = getProperty("FLAT.NAME", "Unknown name");
        List<String> bandsDefault = list("Band 1");
        List<String> bandNames = (List<String>)getProperty("FLAT.BANDNAMES", bandsDefault);
        List<String> bandFiles = (List<String>)getProperty("FLAT.BANDFILES", bandsDefault);

        int lines = getProperty("FLAT.LINES", 0);
        int elements = getProperty("FLAT.ELEMENTS", 0);
        String unit = getProperty("FLAT.UNIT", "");
        int stride = getProperty("FLAT.STRIDE", 1);

        Hashtable imageProps = Misc.newHashtable(DataChoice.PROP_ICON, "/auxdata/ui/icons/Earth16.gif");
        String delimiter = getProperty("ASCII.DELIMITER", "");

        List categories = DataCategory.parseCategories("IMAGE", false);
        CompositeDataChoice cdc = new CompositeDataChoice(this, "", name, name, null);
        for (int i=0; i<bandFiles.size(); i++) {
            FlatFileReader dataChoiceData = new FlatFileReader(bandFiles.get(i), lines, elements);
            dataChoiceData.setAsciiInfo(delimiter, 1);
            dataChoiceData.setUnit(unit);
            dataChoiceData.setEastPositive(eastPositive);
            dataChoiceData.setStride(stride);
            if ((latFile != null) && (lonFile != null)) {
                dataChoiceData.setNavFiles(latFile, lonFile, scale);
            } else {
                dataChoiceData.setNavBounds(ulLat, ulLon, lrLat, lrLon);
            }
            String bandName = bandNames.get(i);
            DirectDataChoice ddc = new DirectDataChoice(this, dataChoiceData, bandName, bandName, categories, imageProps);
            cdc.addDataChoice(ddc);
        }
        addDataChoice(cdc);
//            System.err.println("Still working on ascii data...");
    }

    private void handleBinaryFormat(double ulLat, double ulLon, double lrLat, double lrLon, String latFile, String lonFile, int scale, boolean eastPositive) {
        String name = getProperty("FLAT.NAME", "Unknown name");
        List<String> bandsDefault = list("Band 1");
        List<String> bandNames = (List<String>)getProperty("FLAT.BANDNAMES", bandsDefault);
        List<String> bandFiles = (List<String>)getProperty("FLAT.BANDFILES", bandsDefault);

        int lines = getProperty("FLAT.LINES", 0);
        int elements = getProperty("FLAT.ELEMENTS", 0);
        String unit = getProperty("FLAT.UNIT", "");
        int stride = getProperty("FLAT.STRIDE", 1);

        Hashtable imageProps = Misc.newHashtable(DataChoice.PROP_ICON, "/auxdata/ui/icons/Earth16.gif");
        int format = getProperty("BINARY.FORMAT", HeaderInfo.kFormat1ByteUInt);
        String interleave = getProperty("BINARY.INTERLEAVE", HeaderInfo.kInterleaveSequential);
        boolean bigEndian = getProperty("BINARY.BIGENDIAN", false);
        int offset = getProperty("BINARY.OFFSET", 0);

        List categories = DataCategory.parseCategories("IMAGE", false);
        CompositeDataChoice cdc = new CompositeDataChoice(this, "", name, name, null);
        for (int i=0; i<bandFiles.size(); i++) {
            FlatFileReader dataChoiceData = new FlatFileReader(bandFiles.get(i), lines, elements);
            dataChoiceData.setBinaryInfo(format, interleave, bigEndian, offset, i+1, bandFiles.size());
            dataChoiceData.setUnit(unit);
            dataChoiceData.setEastPositive(eastPositive);
            dataChoiceData.setStride(stride);
            if ((latFile != null) && (lonFile != null)) {
                dataChoiceData.setNavFiles(latFile, lonFile, scale);
            } else {
                dataChoiceData.setNavBounds(ulLat, ulLon, lrLat, lrLon);
            }
            String bandName = bandNames.get(i);
            DirectDataChoice ddc = new DirectDataChoice(this, dataChoiceData, bandName, bandName, categories, imageProps);
            cdc.addDataChoice(ddc);
        }
        addDataChoice(cdc);
        //            System.err.println("Still working on binary data...");
    }

    /**
     * This method should create and return the visad.Data that is
     * identified by the given {@link DataChoice}.
     *
     * @param dataChoice     This is one of the DataChoice-s that was created
     *                       in the doMakeDataChoices call above.
     * @param category       The specific {@link DataCategory}
     *                       which the {@link ucar.unidata.idv.DisplayControl}
     *                       was instantiated with. Usually can be ignored.
     * @param dataSelection  This may contain a list of times which
     *                       subsets the request.
     * @param requestProperties  extra request properties
     *
     * @return {@link Data} object represented by the given dataChoice
     *
     * @throws RemoteException Java RMI problem.
     * @throws VisADException VisAD problem.
     */
    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection,
                                Hashtable requestProperties)
        throws VisADException, RemoteException
    {
        FlatFileReader stuff = (FlatFileReader) dataChoice.getId();
        return stuff.getData();
    }

}

