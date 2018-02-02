/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2018
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

package ucar.unidata.ui;

import edu.wisc.ssec.mcidasv.ui.ColorSwatchComponent;
import ucar.unidata.gis.maps.LatLonData;
import ucar.unidata.idv.control.MapDisplayControl;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.xml.XmlObjectStore;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * A panel to hold the gui for one lat lon line
 */
public class LatLonPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	/** flag for ignoring events */
    private boolean ignoreEvents = false;

    /** This holds the data that describes the latlon lines */
    private LatLonData latLonData;

    /** The visibility cbx */
    JCheckBox onOffCbx;

    /** The spacing input box */
    JTextField spacingField;

    /** The base input box */
    JTextField baseField;

    /** Shows the color */
    ColorSwatchComponent colorButton;

    /** The line width box */
    JComboBox widthBox;

    /** The line style box */
    JComboBox styleBox;

    /** The line style box */
    JCheckBox fastRenderCbx;

    /**
     * Create a LatLonPanel
     *
     * @param lld Holds the lat lon data
     *
     */
    
    public LatLonPanel(XmlObjectStore store, LatLonData lld) {
        this.latLonData = lld;
        ignoreEvents    = true;
        onOffCbx        = new JCheckBox("", latLonData.getVisible());
        onOffCbx.setToolTipText("Turn on/off lines");
        onOffCbx.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if ( !ignoreEvents) {
                    latLonData.setVisible(onOffCbx.isSelected());
                }
            }
        });
        spacingField =
            new JTextField(String.valueOf(latLonData.getSpacing()), 6);
        spacingField.setToolTipText(
            "Set the interval (degrees) between lines");
        spacingField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (ignoreEvents) {
                    return;
                }
                validateTextFields();
                latLonData.setSpacing(
                    new Float(spacingField.getText()).floatValue());
                // this ensures the formatting is consistent for lat/lon text fields
                spacingField.setText("" + latLonData.getSpacing());
            }
        });
        baseField = new JTextField(String.valueOf(latLonData.getBase()), 6);
        baseField.setToolTipText("Set the base value for the interval");
        baseField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (ignoreEvents) {
                    return;
                }
                validateTextFields();
                if (latLonData.getIsLatitude()) {
                	latLonData.setBase(
                			new Float(baseField.getText()).floatValue());
                } else {
                	latLonData.setBase(
                			new Float(baseField.getText()).floatValue());
                }
                baseField.setText("" + latLonData.getBase());
                // this ensures the formatting is consistent for lat/lon text fields
            }
        });

        widthBox = new JComboBox(new String[] { "1.0", "1.5", "2.0", "2.5",
                "3.0" });
        widthBox.setMaximumSize(new Dimension(30, 16));
        widthBox.setEditable(true);
        widthBox.setSelectedItem(String.valueOf(latLonData.getLineWidth()));
        widthBox.setToolTipText("Set the width of the lines");
        widthBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (ignoreEvents) {
                    return;
                }
                latLonData
                    .setLineWidth(Float
                        .parseFloat((String) ((JComboBox) e.getSource())
                            .getSelectedItem()));
            }
        });
        styleBox = new JComboBox(new String[] { "_____", "_ _ _", ".....",
                "_._._" });
        styleBox.setMaximumSize(new Dimension(30, 16));
        styleBox.setSelectedIndex(latLonData.getLineStyle());
        styleBox.setToolTipText("Set the line style");
        Font f = Font.decode("monospaced-BOLD");
        if (f != null) {
            styleBox.setFont(f);
        }
        styleBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (ignoreEvents) {
                    return;
                }
                latLonData.setLineStyle(
                    ((JComboBox) e.getSource()).getSelectedIndex());
            }
        });
        colorButton = new ColorSwatchComponent(store, latLonData.getColor(),
                "Set " + (latLonData.getIsLatitude()
                          ? "Latitude"
                          : "Longitude") + " Color");
        colorButton.setToolTipText("Set the line color");
        colorButton.addPropertyChangeListener("background",
                new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (ignoreEvents) {
                    return;
                }
                Color c = ((JPanel) evt.getSource()).getBackground();

                if (c != null) {
                    latLonData.setColor(c);
                }
            }
        });
        fastRenderCbx = new JCheckBox("", latLonData.getFastRendering());
        fastRenderCbx.setToolTipText("Set if lines don't render correctly");
        fastRenderCbx.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if ( !ignoreEvents) {
                    latLonData.setFastRendering(fastRenderCbx.isSelected());
                }
            }
        });
        ignoreEvents = false;
    }


    /**
     * Set the information that configures this.
     *
     * @param lld   the latlon data
     */
    public void setLatLonData(LatLonData lld) {
        this.latLonData = lld;
        if (onOffCbx != null) {
            ignoreEvents = true;
            onOffCbx.setSelected(lld.getVisible());
            spacingField.setText("" + lld.getSpacing());
            baseField.setText("" + lld.getBase());
            widthBox.setSelectedItem("" + lld.getLineWidth());
            styleBox.setSelectedIndex(lld.getLineStyle());
            colorButton.setBackground(lld.getColor());
            fastRenderCbx.setSelected(lld.getFastRendering());
            ignoreEvents = false;
        }

    }


    /**
     * Layout the panels
     *
     * @param latPanel  the lat panel
     * @param lonPanel  the lon panel
     *
     * @return The layed out panels
     */
    public static JPanel layoutPanels(LatLonPanel latPanel,
                                      LatLonPanel lonPanel) {
        Component[] comps = {
            GuiUtils.lLabel("<html><b>Lines</b></html"), GuiUtils.filler(),
            GuiUtils.cLabel("Interval"), GuiUtils.cLabel("Relative to"),
            GuiUtils.cLabel("Width"), GuiUtils.cLabel("Style"),
            GuiUtils.cLabel("Color"), GuiUtils.cLabel("Fast Render"),
            latPanel.onOffCbx, GuiUtils.rLabel("Latitude:"),
            latPanel.spacingField, latPanel.baseField, latPanel.widthBox,
            latPanel.styleBox, latPanel.colorButton, latPanel.fastRenderCbx,
            lonPanel.onOffCbx, GuiUtils.rLabel("Longitude:"),
            lonPanel.spacingField, lonPanel.baseField, lonPanel.widthBox,
            lonPanel.styleBox, lonPanel.colorButton, lonPanel.fastRenderCbx
        };
        GuiUtils.tmpInsets = new Insets(2, 4, 2, 4);
        return GuiUtils.doLayout(comps, 8, GuiUtils.WT_N, GuiUtils.WT_N);
    }

    /**
     * Apply any of the state in the gui (e.g., spacing) to the  latLonData
     */
    
    public void applyStateToData() {
        // need to get the TextField values because people could type in a new value
        // without hitting return.  Other widgets should trigger a change
        latLonData.setSpacing(new Float(spacingField.getText()).floatValue());
        latLonData.setBase(new Float(baseField.getText()).floatValue());
    }


	/**
	 * @throws HeadlessException
	 */
	private void validateTextFields() throws HeadlessException {
        
		float curVal = latLonData.getSpacing();
		//System.err.println("");
        //System.err.println("Is this Lat?: " + latLonData.getIsLatitude());
        boolean isLat = latLonData.getIsLatitude();
        boolean llLinked = ((MapDisplayControl.LatLonState) latLonData).getMapDisplayControl().getApplyChangesToAllLatLon();
        //System.err.println("Lat/Lon panels are linked: " + llLinked);
        // TJJ May 2014, validate input, bad data was causing NPEs
        try {
        	
        	// First check the interval fields
        	float val = Float.parseFloat(spacingField.getText());
        	if (val <= 0.0f) {
        		JOptionPane.showMessageDialog(null, 
        				"Interval must be greater than zero",
        				"Invalid Latitude or Longitude Interval",
        				JOptionPane.ERROR_MESSAGE);
        		// put text field back to old value
        		spacingField.setText("" + curVal);
        		return;
        	} else {
        		if (isLat) {
        			if (val > 180) {
        				JOptionPane.showMessageDialog(null, 
        						"Value exceeds valid bounds",
        						"Invalid Lat/Lon interval",
        						JOptionPane.ERROR_MESSAGE);
        				// put text field back to old value
        				spacingField.setText("" + curVal);
        				return;
        			}
        		} else {
        			if (llLinked) {
        				if (val > 180) {
        					JOptionPane.showMessageDialog(null, 
        							"Unlink to set Longitude interval > 180",
        							"Invalid Longitude interval while linked",
        							JOptionPane.ERROR_MESSAGE);
        					// put text field back to old value
        					spacingField.setText("" + curVal);
        					return;
        				}
        			} else {
        				if (val > 360) {
        					JOptionPane.showMessageDialog(null, 
        							"Value exceeds valid bounds",
        							"Invalid Lat/Lon interval",
        							JOptionPane.ERROR_MESSAGE);
        					// put text field back to old value
        					spacingField.setText("" + curVal);
        					return;
        				}
        			}
        		}
        	}
        } catch (NumberFormatException nfe) {
        	//System.err.println("NFE: " + spacingField.getText());
        	// Also an error, different message
    		JOptionPane.showMessageDialog(null, 
    				"Value entered is not a number",
    				"Invalid Latitude or Longitude Interval",
    				JOptionPane.ERROR_MESSAGE);
    		// put text field back to old value
    		spacingField.setText("" + curVal);
        }
        
        // Now the base (offset) fields
		float curBase = latLonData.getBase();
        try {
        	float val = Float.parseFloat(baseField.getText());
        	if (latLonData.getIsLatitude()) {
	        	if ((val < -90.0f) || (val > 90.0f)) {
	        		JOptionPane.showMessageDialog(null, 
	        				"Value must be a valid Latitude (-90 to 90)",
	        				"Invalid Relative Latitude",
	        				JOptionPane.ERROR_MESSAGE);
	        		// put text field back to old value
	        		baseField.setText("" + curBase);
	        	}
        	} else {
        		// valid range changes depending on convention checkbox
	        	if ((val < -180.0f + LatLonLabelPanel.LON_OFFSET) || 
	        		(val > 180.0f + LatLonLabelPanel.LON_OFFSET)) {
	        		JOptionPane.showMessageDialog(null, 
	        				"Value must be a valid Longitude " + LatLonLabelPanel.LON_RANGE,
	        				"Invalid Relative Longitude",
	        				JOptionPane.ERROR_MESSAGE);
	        		// put text field back to old value
	        		baseField.setText("" + curBase);
	        	}
        	}
        } catch (NumberFormatException nfe) {
        	//System.err.println("NFE: " + baseField.getText());
        	// Also an error, different message
    		JOptionPane.showMessageDialog(null, 
    				"Value entered is not a number",
    				"Invalid Relative Latitude or Longitude",
    				JOptionPane.ERROR_MESSAGE);
    		// put text field back to old value
    		baseField.setText("" + curBase);
        }
        
	}

	/**
	 * Get the latlondata object
	 *
	 * @return The latlondata object
	 */

	public LatLonData getLatLonData() {
		return latLonData;
	}

}
