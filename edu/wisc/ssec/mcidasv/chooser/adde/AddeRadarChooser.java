/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2020
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

package edu.wisc.ssec.mcidasv.chooser.adde;

import static javax.swing.GroupLayout.DEFAULT_SIZE;
import static javax.swing.GroupLayout.PREFERRED_SIZE;
import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static javax.swing.LayoutStyle.ComponentPlacement.RELATED;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.GroupLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.w3c.dom.Element;

import edu.wisc.ssec.mcidas.AreaDirectory;
import edu.wisc.ssec.mcidas.AreaDirectoryList;
import edu.wisc.ssec.mcidas.AreaFileException;
import edu.wisc.ssec.mcidas.McIDASUtil;

import ucar.unidata.data.imagery.AddeImageInfo;
import ucar.unidata.data.imagery.ImageDataSource;
import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.idv.chooser.adde.AddeServer;
import ucar.unidata.metdata.NamedStationTable;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;


import edu.wisc.ssec.mcidasv.util.McVGuiUtils;

/**
 * Widget to select NEXRAD radar images from a remote ADDE server
 * Displays a list of the descriptors (names) of the radar datasets
 * available for a particular ADDE group on the remote server.
 *
 * @author Don Murray
 */
public class AddeRadarChooser extends AddeImageChooser {

    /** Use to list the stations */
    protected static final String VALUE_LIST = "list";

    /** This is the list of properties that are used in the advanced gui */
    private static final String[] RADAR_PROPS = { PROP_UNIT };

    /** This is the list of labels used for the advanced gui */
    private static final String[] RADAR_LABELS = { "Data Type:" };

    /** Am I currently reading the stations */
    private boolean readingStations = false;

    /** handle on the station update task */
    private Object readStationTask;

    /** station table */
    private List nexradStations;



    /**
     * Construct an Adde image selection widget displaying information
     * for the specified dataset located on the specified server.
     *
     *
     *
     * @param mgr The chooser manager
     * @param root The chooser.xml node
     */
    public AddeRadarChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
        this.nexradStations =
            getIdv().getResourceManager().findLocationsByType("radar");
    }

    /**
     * get the adde server grup type to use
     *
     * @return group type
     */
    protected String getGroupType() {
        return AddeServer.TYPE_RADAR;
    }

    /**
     * Overwrite base class method to return the correct name
     * (used for labeling, etc.)
     *
     * @return  data name specific to this selector
     */
    public String getDataName() {
        return "Radar Data";
    }

    @Override public String getDataType() {
        return "RADAR";
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getDescriptorLabel() {
        return "Product";
    }

    /**
     * Get the size of the image list
     *
     * @return the image list size
     */
    protected int getImageListSize() {
        return 6;
    }
    
    /**
     * Get a description of the currently selected dataset
     *
     * @return the data set description.
     */
    public String getDatasetName() {
        return getSelectedStation() + " (" + super.getDatasetName() + ")";
    }

    /**
     * Method to call if the server changed.
     */
    protected void connectToServer() {
        clearStations();
        super.connectToServer();
        setAvailableStations();
    }

    /**
     * Check if we are ready to read times
     *
     * @return  true if times can be read
     */
    protected boolean canReadTimes() {
        return super.canReadTimes() && (getSelectedStation() != null);
    }

    /**
     * Get the advanced property names
     *
     * @return array of advanced properties
     */
    protected String[] getAdvancedProps() {
        return RADAR_PROPS;
    }

    /**
     * Get the labels for the advanced properties
     *
     * @return array of labels
     */
    protected String[] getAdvancedLabels() {
        return RADAR_LABELS;
    }

    /**
     * Update labels, etc.
     */
    protected void updateStatus() {
        super.updateStatus();
        if (getState() != STATE_CONNECTED) {
            clearStations();
        }
        if (readStationTask!=null) {
            if(taskOk(readStationTask)) {
                setStatus("Reading available stations from server");
            } else {
                readStationTask  = null;
                setState(STATE_UNCONNECTED);
            }
        }
    }

    /**
     * A new station was selected. Update the gui.
     *
     * @param stations List of selected stations
     */
    protected void newSelectedStations(List stations) {
        super.newSelectedStations(stations);
        descriptorChanged();
    }

    /**
     *  Generate a list of radar ids for the id list.
     */
    private void setAvailableStations() {
        readStationTask = startTask();
        clearSelectedStations();
        updateStatus();
        List stations = readStations();
        if(stopTaskAndIsOk(readStationTask)) {
            readStationTask = null;
            if (stations != null) {
                getStationMap().setStations(stations);
            } else {
                clearStations();
            }
            updateStatus();
            revalidate();
        } else {
            //User pressed cancel
            setState(STATE_UNCONNECTED);
            return;
        }
    }

    /**
     * Generate a list of radar ids for the id list.
     *
     * @return  list of station IDs
     */
    private List readStations() {
        ArrayList stations = new ArrayList();
        try {
            if ((descriptorNames == null) || (descriptorNames.length == 0)) {
                return stations;
            }
            StringBuffer buff        = getGroupUrl(REQ_IMAGEDIR, getGroup());
            String       descrForIds = descriptorNames[0];
            // try to use base reflectivity if it's available.
            for (int i = 0; i < descriptorNames.length; i++) {
                if ((descriptorNames[i] != null)
                        && descriptorNames[i].toLowerCase().startsWith(
                            "base")) {
                    descrForIds = descriptorNames[i];
                    break;
                }
            }
            appendKeyValue(buff, PROP_DESCR,
                           getDescriptorFromSelection(descrForIds));
            appendKeyValue(buff, PROP_ID, VALUE_LIST);
            Hashtable         seen    = new Hashtable();
            AreaDirectoryList dirList =
                new AreaDirectoryList(buff.toString());
            for (Iterator it = dirList.getDirs().iterator(); it.hasNext(); ) {
                AreaDirectory ad = (AreaDirectory) it.next();
                String stationId =
                    McIDASUtil.intBitsToString(ad.getValue(20)).trim();
                //Check for uniqueness
                if (seen.get(stationId) != null) {
                    continue;
                }
                seen.put(stationId, stationId);
                //System.err.println ("id:" + stationId);
                Object station = findStation(stationId);
                if (station != null) {
                    stations.add(station);
                }
            }
        } catch (AreaFileException e) {
            String msg = e.getMessage();
            if (msg.toLowerCase().indexOf(
                    "no images meet the selection criteria") >= 0) {
                LogUtil.userErrorMessage(
                    "No stations could be found on the server");
            } else {
                handleConnectionError(e);
            }
            stations = new ArrayList();
            setState(STATE_UNCONNECTED);
        }
        return stations;
    }

    /**
     * Find the station for the given ID
     *
     * @param stationId  the station ID
     *
     * @return  the station or null if not found
     */
    private Object findStation(String stationId) {
        for (int i = 0; i < nexradStations.size(); i++) {
            NamedStationTable table =
                (NamedStationTable) nexradStations.get(i);
            Object station = table.get(stationId);
            if (station != null) {
                return station;
            }
        }
        return null;
    }

    public void doCancel() {
        readStationTask = null;
        super.doCancel();
    }

    /**
     * Get the list of properties for the base URL
     * @return list of properties
     */
    protected String[] getBaseUrlProps() {
        return new String[] { PROP_DESCR, PROP_ID, PROP_UNIT, PROP_SPAC,
                              PROP_BAND, PROP_USER, PROP_PROJ, };
    }

    /**
     * Overwrite the base class method to return the default property value
     * for PROP_ID.
     *
     * @param prop The property
     * @param ad The area directory
     * @param forDisplay Is this to show the end user in the gui.
     *
     * @return The value of the property
     */
    protected String getDefaultPropValue(String prop, AreaDirectory ad,
                                         boolean forDisplay) {
        if (prop.equals(PROP_ID)) {
            return getSelectedStation();
        }
        if (prop.equals(PROP_SPAC)) {
            // Don't want this to default to "1" or it will break
            // Hydrometeor Classification product...see inquiry 1518
            return "4";
        }
        return super.getDefaultPropValue(prop, ad, forDisplay);
    }

    /**
     * Get a description of the properties
     *
     * @return  a description
     */
    protected String getPropertiesDescription() {
        StringBuilder buf = new StringBuilder();
        if (unitComboBox != null) {
            buf.append(getAdvancedLabels()[0]);
            buf.append(' ');
            buf.append(unitComboBox.getSelectedItem());
        }
        return buf.toString();
    }

    /**
     * get properties
     *
     * @param ht properties
     */
    protected void getDataSourceProperties(Hashtable ht) {
        unitComboBox.setSelectedItem(ALLUNITS);
        super.getDataSourceProperties(ht);
        ht.put(ImageDataSource.PROP_IMAGETYPE, ImageDataSource.TYPE_RADAR);
    }
    
    /**
     * Get the time popup widget
     *
     * @return  a widget for selecing the day
     */
    protected JComponent getExtraTimeComponent() {
        JPanel filler = new JPanel();
        McVGuiUtils.setComponentHeight(filler, new JComboBox());
        return filler;
    }
    
    /**
     * Make the UI for this selector.
     *
     * @return The gui
     */
    public JComponent doMakeContents() {      
        JPanel myPanel = new JPanel();
                
        JLabel stationLabel = McVGuiUtils.makeLabelRight("Station:");
        addServerComp(stationLabel);

        JComponent stationPanel = getStationMap();
        registerStatusComp("stations", stationPanel);
        addServerComp(stationPanel);
        
        JLabel timesLabel = McVGuiUtils.makeLabelRight("Times:");
        addDescComp(timesLabel);
        
        JPanel timesPanel = makeTimesPanel();
        timesPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        addDescComp(timesPanel);
        
        // We need to create this but never show it... AddeImageChooser requires it to be instantiated
        unitComboBox = new JComboBox();
        
        enableWidgets();

        GroupLayout layout = new GroupLayout(myPanel);
        myPanel.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(descriptorLabel)
                        .addGap(GAP_RELATED)
                        .addComponent(descriptorComboBox))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(stationLabel)
                        .addGap(GAP_RELATED)
                        .addComponent(stationPanel, PREFERRED_SIZE, DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(timesLabel)
                        .addGap(GAP_RELATED)
                        .addComponent(timesPanel, PREFERRED_SIZE, DEFAULT_SIZE, Short.MAX_VALUE))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(descriptorLabel)
                    .addComponent(descriptorComboBox))
                .addPreferredGap(RELATED)
                .addGroup(layout.createParallelGroup(LEADING)
                    .addComponent(stationLabel)
                    .addComponent(stationPanel, PREFERRED_SIZE, DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(RELATED)
                .addGroup(layout.createParallelGroup(LEADING)
                    .addComponent(timesLabel)
                    .addComponent(timesPanel, PREFERRED_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        
        setInnerPanel(myPanel);
        return super.doMakeContents(true);
    }
    
    /**
     * Get the default value for a key
     * 
     * @return null for SIZE, else super
     */
    protected String getDefault(String property, String dflt) {
        if (PROP_SIZE.equals(property)) {
            return dflt;
        }
        return super.getDefault(property, dflt);
    }
    
    /**
     * Make an AddeImageInfo from a URL and an AreaDirectory
     * 
     * @param dir
     *            AreaDirectory
     * @param isRelative
     *            true if is relative
     * @param num
     *            number (for relative images)
     * 
     * @return corresponding AddeImageInfo
     */
    protected AddeImageInfo makeImageInfo(AreaDirectory dir,
            boolean isRelative, int num) {
        AddeImageInfo info = new AddeImageInfo(getAddeServer().getName(),
                AddeImageInfo.REQ_IMAGEDATA, getGroup(), getDescriptor());
        if (isRelative) {
            info.setDatasetPosition((num == 0) ? 0 : -num);
        } else {
            info.setStartDate(dir.getNominalTime());
        }
        setImageInfoProps(info, getMiscKeyProps(), dir);
        setImageInfoProps(info, getBaseUrlProps(), dir);

        info.setLocateKey(PROP_LINELE);
        info.setLocateValue("0 0 F");
        info.setPlaceValue("ULEFT");
        
        String magKey = getPropValue(PROP_MAG, dir);
        int lmag = 1;
        int emag = 1;
        StringTokenizer tok = new StringTokenizer(magKey);
        lmag = (int) Misc.parseNumber((String) tok.nextElement());
        if (tok.hasMoreTokens()) {
            emag = (int) Misc.parseNumber((String) tok.nextElement());
        } else {
            emag = lmag;
        }
        info.setLineMag(lmag);
        info.setElementMag(emag);

        int lines = dir.getLines();
        int elems = dir.getElements();
        String sizeKey = getPropValue(PROP_SIZE, dir);
        tok = new StringTokenizer(sizeKey);
        String size = (String) tok.nextElement();
        if (!size.equalsIgnoreCase("all")) {
            lines = (int) Misc.parseNumber(size);
            if (tok.hasMoreTokens()) {
                elems = (int) Misc.parseNumber((String) tok.nextElement());
            } else {
                elems = lines;
            }
        }
        info.setLines(lines);
        info.setElements(elems);
        /*
         * System.out.println("url = " + info.getURLString().toLowerCase() +
         * "\n");
         */
        return info;
    }

    /**
     * Set the relative and absolute extra components.
     */
    @Override protected JPanel makeTimesPanel() {
        // show the time driver if the rest of the choosers are doing so.
        JPanel timesPanel =
            super.makeTimesPanel(false, true, getIdv().getUseTimeDriver());

        // Make a new timesPanel that has extra components tacked on the
        // bottom, inside the tabs
        Component[] comps = timesPanel.getComponents();

        if ((comps.length == 1) && (comps[0] instanceof JTabbedPane)) {
            timesCardPanelExtra = new GuiUtils.CardLayoutPanel();
            timesCardPanelExtra.add(new JPanel(), "relative");
            timesCardPanelExtra.add(getExtraTimeComponent(), "absolute");
            timesPanel = GuiUtils.centerBottom(comps[0], timesCardPanelExtra);
        }
        return timesPanel;
    }
}
