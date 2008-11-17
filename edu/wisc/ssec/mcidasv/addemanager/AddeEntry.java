/*
 * $Id$
 *
 * Copyright 2007-2008
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison,
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 *
 * http://www.ssec.wisc.edu/mcidas
 *
 * This file is part of McIDAS-V.
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
 * along with this program.  If not, see http://www.gnu.org/licenses
 */

package edu.wisc.ssec.mcidasv.addemanager;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import ucar.unidata.util.GuiUtils;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils;
import edu.wisc.ssec.mcidasv.util.McVTextField;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.Width;

/**
 * Keeper of info relevant to a single entry in RESOLV.SRV
 */
public class AddeEntry {
	private String addeGroup;
	private String addeDescriptor;
	private String addeRt;
	private String addeType;
	private String addeFormat;
	private String addeDescription;
	private String addeStart;
	private String addeEnd;
	private String addeFileMask;
	private String addeName;
	
	/**
	 * The full list of possible ADDE servers
	 * 
	 * The fields are:
	 *  4-character server name
	 *  Short name (MUST be unique)
	 *  Long description
	 *  Data type (ie. IMAGE, RADAR, GRID, POINT, etc)
	 */
	/*
	private String[][] addeFormats = {
		{ "AREA", "McIDAS AREA", "McIDAS AREA", "IMAGE" },
		{ "AIRS", "AIRS L1b", "AIRS Level 1b", "IMAGE" },
		{ "AMSR", "AMSR-E L1b", "AMSR-E Level 1b", "IMAGE" },
		{ "AMRR", "AMSR-E Rain Product", "AMSR-E Rain Product", "IMAGE" },
		{ "GINI", "AWIPS GINI", "AWIPS GINI", "IMAGE" },
		{ "AWIP", "AWIPS netCDF", "AWIPS netCDF", "IMAGE" },
		{ "FSDX", "EUMETCast LRIT", "EUMETCast LRIT", "IMAGE" },
		{ "OMTP", "Meteosat OpenMTP", "Meteosat OpenMTP", "IMAGE" },
		{ "LV1B", "Metop AVHRR L1b", "Metop AVHRR Level 1b", "IMAGE" },
		{ "MODS", "MODIS L1b MOD02", "MODIS Level 1b", "IMAGE" },
		{ "MODX", "MODIS L2 MOD06", "MODIS Level 2 (Cloud top properties)", "IMAGE" },
		{ "MODX", "MODIS L2 MOD07", "MODIS Level 2 (Atmospheric profile)", "IMAGE" },
		{ "MODX", "MODIS L2 MOD35", "MODIS Level 2 (Cloud mask)", "IMAGE" },
		{ "MOD4", "MODIS L2 MOD04", "MODIS Level 2 (Aerosol)", "IMAGE" },
		{ "MOD8", "MODIS L2 MOD28", "MODIS Level 2 (Sea surface temperature)", "IMAGE" },
		{ "MODR", "MODIS L2 MODR", "MODIS Level 2 (Corrected reflectance)", "IMAGE" },
		{ "MSGT", "MSG HRIT", "MSG HRIT", "IMAGE" },
		{ "MTST", "MTSAT HRIT", "MTSAT HRIT", "IMAGE" },
		{ "LV1B", "NOAA AVHRR L1b", "NOAA AVHRR Level 1b", "IMAGE" },
		{ "SMIN", "SSMI", "Terrascan netCDF", "IMAGE" },
		{ "TMIN", "TRMM", "Terrascan netCDF", "IMAGE" },
		{ "NEXR", "NEXRAD Radar", "NEXRAD Level 3 Radar", "RADAR" }
	};
	*/
	
	private String[][] addeFormats = {
			{ "AREA", "McIDAS AREA", "McIDAS AREA", "IMAGE" },
			{ "AMSR", "AMSR-E L1b", "AMSR-E Level 1b", "IMAGE" },
			{ "AMRR", "AMSR-E Rain Product", "AMSR-E Rain Product", "IMAGE" },
			{ "FSDX", "EUMETCast LRIT", "EUMETCast LRIT", "IMAGE" },
			{ "OMTP", "Meteosat OpenMTP", "Meteosat OpenMTP", "IMAGE" },
			{ "LV1B", "Metop AVHRR L1b", "Metop AVHRR Level 1b", "IMAGE" },
			{ "MODS", "MODIS L1b MOD02", "MODIS Level 1b", "IMAGE" },
			{ "MODX", "MODIS L2 MOD35", "MODIS Level 2 (Cloud mask)", "IMAGE" },
			{ "MOD4", "MODIS L2 MOD04", "MODIS Level 2 (Aerosol)", "IMAGE" },
			{ "MODR", "MODIS L2 MODR", "MODIS Level 2 (Corrected reflectance)", "IMAGE" },
			{ "MSGT", "MSG HRIT - FD", "MSG HRIT - Full Disk", "IMAGE" },
			{ "MSGT", "MSG HRIT - HRV", "MSG HRIT - High Resolution Visible", "IMAGE" },
			{ "LV1B", "NOAA AVHRR L1b", "NOAA AVHRR Level 1b", "IMAGE" },
			{ "SMIN", "SSMI", "Terrascan netCDF", "IMAGE" },
			{ "TMIN", "TRMM", "Terrascan netCDF", "IMAGE" }
		};
	
	private String cygwinPrefix = "/cygdrive/";
	private int cygwinPrefixLength = cygwinPrefix.length();
		
	/**
	 * Empty constructor
	 */
	public AddeEntry() {
		addeGroup = "";
		addeDescriptor = "ERROR";
		addeRt = "N";
		setByDescription(addeFormats[0][1]);
		addeStart = "1";
		addeEnd = "99999";
		addeFileMask = "";
		addeName="";
	}
		
	/**
	 * Initialize with user editable info
	 */
	public AddeEntry(String group, String name, String description, String mask) {
		this();
		addeGroup = group;
		addeDescriptor = "ERROR";
		setByDescription(description);
		addeFileMask = mask;
		addeName = name;
	}
		
	/**
	 * Initialize with a line from RESOLV.SRV
	 * 
	 * @param resolvLine
	 */
	public AddeEntry(String resolvLine) {
		this();
		String[] assignments = resolvLine.trim().split(",");
		String[] varval;
	    for (int i = 0 ; i < assignments.length ; i++) {
	    	if (assignments[i] == null || assignments[i].equals("")) continue;
	    	varval = assignments[i].split("=");
	    	if (varval.length != 2 ||
	    			varval[0].equals("") || varval[1].equals("")) continue;
	    	if (varval[0].equals("N1")) addeGroup = varval[1];
	    	else if (varval[0].equals("N2")) addeDescriptor = varval[1];
	    	else if (varval[0].equals("TYPE")) addeType = varval[1];
	    	else if (varval[0].equals("K")) addeFormat = varval[1];
	    	else if (varval[0].equals("MASK")) {
	    		String tmpFileMask = varval[1];
	    		tmpFileMask = tmpFileMask.replace("/*", "");
	    		/** Look for "cygwinPrefix" at start of string and munge accordingly */
	    		if (tmpFileMask.length() > cygwinPrefixLength+1 &&
	    				tmpFileMask.substring(0,cygwinPrefixLength).equals(cygwinPrefix)) {
	    			String driveLetter = tmpFileMask.substring(cygwinPrefixLength,cygwinPrefixLength+1).toUpperCase();
	    			tmpFileMask = driveLetter + ":" + tmpFileMask.substring(cygwinPrefixLength+1).replace('/', '\\');
	    		}
	    		addeFileMask = tmpFileMask;
	    	}
	    	else if (varval[0].equals("C")) addeName = varval[1];
	    	else if (varval[0].equals("MCV")) addeDescription = varval[1];
	    }
	}
	
	/**
	 * Convenience method to set attributes defined by unique description
	 */
	private void setByDescription(String description) {
		addeDescription = description;
		addeFormat = "ERROR";
		addeType = "ERROR";
		int i;
		for (i=0; i<addeFormats.length; i++) {
			if (addeFormats[i][1].equals(addeDescription)) {
				addeFormat = addeFormats[i][0];
				addeType = addeFormats[i][3];
			}
		}
	}
	
	/**
	 * Return descriptions from addeFormats
	 * @return
	 */
	private String[] getFormatDescriptions() {
		String[] descriptions = new String[addeFormats.length];
		int i;
		for (i=0; i<addeFormats.length; i++) {
			descriptions[i] = addeFormats[i][1];
		}
		return descriptions;
	}
	
	/**
	 * Return tooltip from addeFormats (by index)
	 */
	private String getTooltip(int index) {
		return addeFormats[index][2];
	}
	
	/**
	 * Return a JPanel with editing elements
	 */
	public JPanel doMakePanel() {
		JPanel entryPanel = new JPanel();
		
		final McVTextField inputGroup = McVGuiUtils.makeTextFieldDeny(addeGroup, 8, true, McVTextField.mcidasDeny);
        inputGroup.addFocusListener(new FocusListener(){
			public void focusGained(FocusEvent e){}
			public void focusLost(FocusEvent e){
				addeGroup = inputGroup.getText();
			}
		});
				
		final McVTextField inputName = (McVTextField)McVGuiUtils.makeTextField(addeName);
		McVGuiUtils.setComponentWidth(inputName, Width.DOUBLE);
		inputName.addFocusListener(new FocusListener(){
			public void focusGained(FocusEvent e){}
			public void focusLost(FocusEvent e){
				addeName = inputName.getText();
				if (addeName.trim().equals("")) addeName = addeDescriptor;
			}
		});
		
		final JComboBox inputFormat = new JComboBox(getFormatDescriptions());
		McVGuiUtils.setComponentWidth(inputFormat, Width.DOUBLE);
	    inputFormat.setRenderer(new TooltipComboBoxRenderer());
		inputFormat.setSelectedItem(addeDescription);
		setByDescription((String)inputFormat.getSelectedItem());
		inputFormat.addItemListener(new ItemListener(){
	        public void itemStateChanged(ItemEvent e){
	    		setByDescription((String)inputFormat.getSelectedItem());
	        }
	    });
		
		String buttonLabel = addeFileMask.equals("") ? "<SELECT>" : getShortString(addeFileMask);
		final JButton inputFileButton = new JButton(buttonLabel);
		McVGuiUtils.setComponentWidth(inputFileButton, Width.DOUBLE);
		inputFileButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				addeFileMask = getDataDirectory(addeFileMask);
				inputFileButton.setText(getShortString(addeFileMask));
			}
		});
		
        entryPanel = GuiUtils.doLayout(new Component[] {
                GuiUtils.rLabel("Dataset (e.g. MYDATA): "), GuiUtils.left(inputGroup),
                GuiUtils.rLabel("Image Type (e.g. JAN 07 GOES): "), GuiUtils.left(inputName),
                GuiUtils.rLabel("Format: "), GuiUtils.left(inputFormat),
                GuiUtils.rLabel("Directory: "), GuiUtils.left(inputFileButton)
            }, 2, GuiUtils.WT_N, GuiUtils.WT_NNNY);
		
		return entryPanel;
	}
	
	/**
	 * Get a short directory name representation, suitable for a button label
	 */
	private String getShortString(String longString) {
		String shortString = longString;
		if (longString.length() > 19) {
			shortString = longString.subSequence(0, 16) + "...";
		}
		return shortString;
	}

	/**
	 * Ask the user for a data directory from which to create a MASK=
	 */
	private String getDataDirectory(String startDir) {
        JFileChooser fileChooser = new JFileChooser(startDir);
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int status = fileChooser.showOpenDialog(null);
        if (status == JFileChooser.APPROVE_OPTION) {
        	File file = fileChooser.getSelectedFile();
        	return file.getAbsolutePath();
        }
        return(startDir);
	}
	
	/**
	 * Return a valid RESOLV.SRV line
	 */
	public String getResolvEntry() {
		String entry = "N1=" + addeGroup.toUpperCase() + ",";
		entry += "N2=" + addeDescriptor.toUpperCase() + ",";
		entry += "TYPE=" + addeType.toUpperCase() + ",";
		entry += "RT=" + addeRt.toUpperCase() + ",";
		entry += "K=" + addeFormat.toUpperCase() + ",";
		entry += "R1=" + addeStart.toUpperCase() + ",";
		entry += "R2=" + addeEnd.toUpperCase() + ",";
		/** Look for "C:" at start of string and munge accordingly */
		if (addeFileMask.length() > 3 && addeFileMask.substring(1,2).equals(":")) {
			String newFileMask = addeFileMask;
			String driveLetter = newFileMask.substring(0,1).toLowerCase();
			newFileMask = newFileMask.substring(3);
			newFileMask = newFileMask.replace('\\', '/');
			entry += "MASK=" + cygwinPrefix + driveLetter + "/" + newFileMask + "/*,";
		}
		else {
			entry += "MASK=" + addeFileMask + "/*,";
		}
		if (addeFormat.toUpperCase().equals("LV1B"))
			entry += "Q=LALO,";
		entry += "C=" + addeName + ",";
		entry += "MCV=" + addeDescription + ",";
		return(entry);
	}
	
	/**
	 * Set just the descriptor--AddeManager uses this to fake descriptor names
	 */
	public void setDescriptor(String newDescriptor) {
		if (newDescriptor==null) newDescriptor = "ENTRY" + Math.random() % 9999;
		//Special rules for MSG HRIT
		if (addeDescription.equals("MSG HRIT - FD")) {
			addeDescriptor = "FD";
		}
		else if (addeDescription.equals("MSG HRIT - HRV")) {
			addeDescriptor = "HRV";
		}
		else {
			addeDescriptor = newDescriptor;
		}
	}
	
	/**
	 * Return just the group
	 */
	public String getGroup() {
		return addeGroup;
	}
	
	/**
	 * Return just the descriptor
	 */
	public String getDescriptor() {
		return addeDescriptor;
	}
	
	/**
	 * Return just the format
	 */
	public String getFormat() {
		return addeFormat;
	}
	
	/**
	 * Return just the description
	 */
	public String getDescription() {
		return addeDescription;
	}
	
	/**
	 * Return just the mask
	 */
	public String getMask() {
		return addeFileMask;
	}
	
	/**
	 * Return just the type
	 */
	public String getType() {
		return addeType;
	}
	
	/**
	 * Return just the name
	 */
	public String getName() {
		return addeName;
	}
	
	/**
	 * Set just the file mask
	 */
	public void setMask(String mask) {
		addeFileMask = mask;
	}
	
	/**
	 * See if this is a valid entry
	 */
	public boolean isValid() {
		if (addeGroup.equals("") || addeDescriptor.equals("") || addeName.equals("")) return false;
		else return true;
	}
	
	private class TooltipComboBoxRenderer extends BasicComboBoxRenderer {
		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean cellHasFocus) {
			if (isSelected) {
				setBackground(list.getSelectionBackground());
				setForeground(list.getSelectionForeground());
				if (-1 < index) {
					list.setToolTipText(getTooltip(index));
				}
			}
			else {
				setBackground(list.getBackground());
				setForeground(list.getForeground());
			}
			setFont(list.getFont());
			setText((value == null) ? "" : value.toString());
			return this;
		}
	}
	
}
