/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2023
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

package edu.wisc.ssec.mcidasv;


import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.cast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import java.awt.event.ActionEvent;

import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import ucar.unidata.idv.ControlDescriptor;
import ucar.unidata.idv.IdvObjectStore;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.util.FileManager;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.wisc.ssec.mcidasv.startupmanager.StartupManager;
import edu.wisc.ssec.mcidasv.util.SystemState;

/**
 * This class is used to initialize McIDAS-V.
 *
 * <p>The initialization process includes creating an
 * {@literal "object store"} (for preferences), user-modifiable settings,
 * and so on.</p>
 *
 * <p>McIDAS-V uses this class to perform pretty much all version-check
 * operations.</p>
 */
public class StateManager extends ucar.unidata.idv.StateManager
    implements Constants, HyperlinkListener
{
    /** Logging object. */
    private static final Logger logger =
        LoggerFactory.getLogger(StateManager.class);
        
    /** Error message shown when given userpath cannot be used. */
    public static final String USERPATH_IS_BAD_MESSAGE =
        "<html>McIDAS-V is unable to create or write to the local user's " +
        "directory.<br>Please select a directory.</html>";
        
    /** Message shown when asking the user to select a userpath. */
    public static final String USERPATH_PICK =
        "Please select a directory to use as the McIDAS-V user path.";
        
    /**
     * Lazily-loaded VisAD build date. Value may be {@code null}.
     *
     * @see #getVisadDate()
     */
    private String visadDate;
    
    /**
     * Lazily-loaded VisAD revision number. Value may be {@code null}.
     *
     * @see #getVisadVersion()
     */
    private String visadVersion;
    
    /**
     * Lazily-loaded {@code ncIdv.jar} build timestamp. Value may be
     * {@code null}.
     *
     * @see #getNetcdfDate()
     */
    private String netcdfDate;
    
    /**
     * Lazily-loaded {@code ncIdv.jar} version. Value may be {@code null}.
     *
     * @see #getNetcdfVersion()
     */
    private String netcdfVersion;
    
    /**
     * Lazily-loaded {@code mcidasv.jar} version. Value may be {@code null}.
     *
     * @see #getMcIdasVersion()
     */
    private String version;
    
    public StateManager(IntegratedDataViewer idv) {
        super(idv);
    }
    
    /**
     * Override to set the right user directory.
     *
     * @return Newly created object store.
     */
    @Override protected IdvObjectStore doMakeObjectStore() {
        IdvObjectStore store = new IdvObjectStore(getIdv(),
                                   getStoreSystemName(), getStoreName(),
                                   getIdv().getEncoderForRead(),
                                   StartupManager.getInstance().getPlatform().getUserDirectory());
        initObjectStore(store);
        return store;
    }
    
    /**
     * Initialize the given object store. This mostly initializes the user's
     * {@literal "userpath"} directory when it is first created.
     *
     * @param store Object store to initialize. Cannot be {@code null}.
     */
    @Override protected void initObjectStore(IdvObjectStore store) {
        while (!store.userDirectoryOk()) {
            LogUtil.userMessage(USERPATH_IS_BAD_MESSAGE);
            File dir = FileManager.getDirectory(null, USERPATH_PICK);
            if (dir != null) {
                store.setOverrideDirectory(dir);
            } else {
                // TODO(jon): figure out why we aren't using regular exit stuff
                System.exit(0);
            }
        }
        
        if (store.getMadeUserDirectory()) {
            initNewUserDirectory(store.getUserDirectory());
        }
        initUserDirectory(store.getUserDirectory());
    }
    
    /**
     * Initialize the McIDAS-V user directory (if it is not already
     * initalized).
     *
     * <p>Here, initialization means {@literal "the user directory exists,
     * and contains a barebones version of mcidasv.rbi"}.</p>
     *
     * @param directory McIDAS-V user directory. Cannot be {@code null}.
     */
    @Override protected void initUserDirectory(File directory) {
        File idvRbi = new File(IOUtil.joinDir(directory, "idv.rbi"));
        if (idvRbi.exists()) {
            if (!idvRbi.delete()) {
                logger.warn("Could not delete '"+idvRbi+'\'');
            }
        }
        File rbiFile = new File(IOUtil.joinDir(directory, "mcidasv.rbi"));
        if (!rbiFile.exists()) {
            String defaultRbi =
                IOUtil.readContents(
                    "/edu/wisc/ssec/mcidasv/resources/userrbi.rbi",
                    (String)null);
            if (defaultRbi != null) {
                try {
                    IOUtil.writeFile(rbiFile, defaultRbi);
                } catch (Exception exc) {
                    logException("Writing default rbi", exc);
                }
            }
        }
    }
    
    @Override protected void initState(boolean interactiveMode) {
        super.initState(interactiveMode);
        
        // At this point in the startup process, McV has read and initialized
        // the resources specified in mcidasv.rbi. filtering out the disabled
        // controls here allows us to do the work once and have it apply to
        // the rest of the session.
        removeDisabledControlDescriptors();
    }
    
    /**
     * Removes disabled {@link ControlDescriptor ControlDescriptors} from
     * {@link IntegratedDataViewer IntegratedDataViewer's}
     * {@code controlDescriptors} and {@code controlDescriptorMap} fields.
     */
    private void removeDisabledControlDescriptors() {
        McIDASV mcv = (McIDASV)getIdv();
        Map<String, ControlDescriptor> cdMap = mcv.getControlDescriptorMap();
        List<ControlDescriptor> cds = cast(mcv.getAllControlDescriptors());
        // copy is soley for iterating over (avoid concurrent modification probs)
        List<ControlDescriptor> copy = new ArrayList<>(cds);
        for (ControlDescriptor c : copy) {
            Hashtable<String, String> props = cast(c.getProperties());
            String v = props.getOrDefault("disabled", "false");
            if (Objects.equals(v, "true")) {
                cdMap.remove(c.getControlId());
                cds.remove(c);
            }
        }
    }
    
    /**
     * Handle a change to a link.
     *
     * @param e Link event. Cannot be {@code null}.
     */
    @Override public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            if (e.getURL() == null) {
                click(e.getDescription());
            } else {
                click(e.getURL().toString());
            }
        }
    }
    
    /**
     * Handle a click on a link.
     *
     * @param url Link to visit.
     */
    public void click(String url) {
        getIdv().actionPerformed(new ActionEvent(this, 0, url));
    }
    
    /**
     * Get the name of the current operating system (via
     * {@literal "os.name"} system property).
     *
     * <p>Note: all space characters will be replaced with underscores.</p>
     *
     * @return Operating system name.
     */
    public String getOSName() {
        String os = System.getProperty("os.name");
        os = os.replaceAll(" ", "_");
        return os;
    }
    
    public String getMcIdasVersionAbout() {
        if (version == null) {
            getMcIdasVersion();
        }
        
        String aboutText = (String)getProperty(Constants.PROP_ABOUTTEXT);
        String versionAbout = IOUtil.readContents(aboutText, "");
        versionAbout = StringUtil.replace(versionAbout, MACRO_VERSION, version);
        Properties props = Misc.readProperties(
            (String) getProperty(Constants.PROP_VERSIONFILE), 
            null, 
            getClass()
        );
        
        String value = getIdvVersion();
        versionAbout = StringUtil.replace(versionAbout, Constants.MACRO_IDV_VERSION, value);
        value = props.getProperty(PROP_COPYRIGHT_YEAR, "");
        versionAbout = StringUtil.replace(versionAbout, Constants.MACRO_COPYRIGHT_YEAR, value);
        value = props.getProperty(PROP_BUILD_DATE, "Unknown");
        versionAbout = StringUtil.replace(versionAbout, Constants.MACRO_BUILDDATE, value);
        versionAbout = StringUtil.replace(versionAbout, Constants.MACRO_VISAD_VERSION, getVisadVersion());
        
        return versionAbout;
    }
    
    public String getMcIdasVersion() {
        if (version != null) {
            return version;
        }
        
        Properties props = new Properties();
        props = Misc.readProperties((String) getProperty(Constants.PROP_VERSIONFILE), null, getClass());
        String maj = props.getProperty(PROP_VERSION_MAJOR, "0");
        String min = props.getProperty(PROP_VERSION_MINOR, "0");
        String rel = props.getProperty(PROP_VERSION_RELEASE, "");
        
        version = maj.concat(".").concat(min).concat(rel);
        
        return version;
    }
    
    /**
     * Returns the current Jython version.
     * 
     * @return Jython's version information.
     */
    @Override public String getJythonVersion() {
        return org.python.Version.PY_VERSION;
    }
    
    /**
     * Get a property.
     *
     * @param name Name of the property. Cannot be {@code null}.
     *
     * @return Value associated with {@code name} or {@code null}.
     */
    @Override public Object getProperty(final String name) {
        Object value = null;
        if (McIDASV.isMac()) {
            value = getProperties().get("mac."+name);
        }
        if (value == null) {
            value = getProperties().get(name);
        }
        if (value == null) {
            String fixedName = StateManager.fixIds(name);
            if (!name.equals(fixedName)) {
                return getProperties().get(fixedName);
            }
        }
        return value;
    }
    
    /**
     * Find the value associated with the given ID by checking the
     * {@literal "properties"}, and if nothing was found, check the preferences.
     *
     * @param name Property or preference ID. Cannot be {@code null}.
     *
     * @return Either the value associated with {@code name} or {@code null}.
     */
    public Object getPropertyOrPreference(String name) {
        Object o = getProperty(name);
        if (o == null) {
            o = getPreference(name);
        }
        return o;
    }
    
    /**
     * Find the {@link String} value associated with the given ID by checking
     * the {@literal "properties"}, and if nothing was found, check the
     * preferences.
     *
     * @param name Property or preference ID. Cannot be {@code null}.
     * @param dflt Value to return if there is no property or preference
     * associated with {@code name}
     *
     * @return Either the value associated with {@code name} or {@code dflt}.
     */
    public String getPropertyOrPreference(String name, String dflt) {
        String value = dflt;
        Object o = getPropertyOrPreference(name);
        if (o != null) {
            value = o.toString();
        }
        return value;
    }
    
    /**
     * Find the {@link Integer} value associated with the given ID by checking
     * the {@literal "properties"}, and if nothing was found, check the
     * preferences.
     *
     * @param name Property or preference ID. Cannot be {@code null}.
     * @param dflt Value to return if there is no property or preference
     * associated with {@code name}
     *
     * @return Either the value associated with {@code name} or {@code dflt}.
     */
    public int getPropertyOrPreference(String name, int dflt) {
        int value = dflt;
        Object o = getPropertyOrPreference(name);
        if (o != null) {
            value = Integer.valueOf(o.toString());
        }
        return value;
    }
    
    /**
     * Find the {@link Double} value associated with the given ID by checking
     * the {@literal "properties"}, and if nothing was found, check the
     * preferences.
     *
     * @param name Property or preference ID. Cannot be {@code null}.
     * @param dflt Value to return if there is no property or preference
     * associated with {@code name}
     *
     * @return Either the value associated with {@code name} or {@code dflt}.
     */
    public double getPropertyOrPreference(String name, double dflt) {
        double value = dflt;
        Object o = getPropertyOrPreference(name);
        if (o != null) {
            value = Double.valueOf(o.toString());
        }
        return value;
    }
    
    /**
     * Find the {@link Boolean} value associated with the given ID by checking
     * the {@literal "properties"}, and if nothing was found, check the
     * preferences.
     *
     * @param name Property or preference ID. Cannot be {@code null}.
     * @param dflt Value to return if there is no property or preference
     * associated with {@code name}
     *
     * @return Either the value associated with {@code name} or {@code dflt}.
     */
    public boolean getPropertyOrPreference(String name, boolean dflt) {
        boolean value = dflt;
        Object o = getPropertyOrPreference(name);
        if (o != null) {
            value = Boolean.valueOf(o.toString());
        }
        return value;
    }
    
    /**
     * Returns information about the current version of McIDAS-V and the IDV,
     * along with their respective build dates.
     * 
     * @return {@code Hashtable} containing versioning information.
     */
    public Hashtable<String, String> getVersionInfo() {
        String versionFile = (String)getProperty(Constants.PROP_VERSIONFILE);
        Properties props =
            Misc.readProperties(versionFile, null, getClass());
            
        String mcvBuild = props.getProperty(PROP_BUILD_DATE, "Unknown");
        
        Hashtable<String, String> table = new Hashtable<>();
        table.put("mcv.version.general", getMcIdasVersion());
        table.put("mcv.version.build", mcvBuild);
        table.put("idv.version.general", getVersion());
        table.put("idv.version.build", getBuildDate());
        table.put("visad.version.general", getVisadVersion());
        table.put("visad.version.build", getVisadDate());
        table.put("netcdf.version.general", getNetcdfVersion());
        table.put("netcdf.version.build", getNetcdfDate());
        return table;
    }
    
    /**
     * Return the timestamp from when {@code ncIdv.jar} was created.
     *
     * @return {@code String} representation of the creation timestamp.
     */
    public String getNetcdfDate() {
        if (netcdfDate == null) {
            Map<String, String> props = SystemState.queryNcidvBuildProperties();
            netcdfDate = props.get("buildDate");
            netcdfVersion = props.get("version");
        }
        return netcdfDate;
    }
    
    /**
     * Return the version information within {@code ncIdv.jar}.
     *
     * @return Version of {@code ncIdv.jar} shipped by McIDAS-V.
     */
    public String getNetcdfVersion() {
        if (netcdfVersion == null) {
            Map<String, String> props = SystemState.queryNcidvBuildProperties();
            netcdfDate = props.get("buildDate");
            netcdfVersion = props.get("version");
        }
        return netcdfVersion;
    }
    
    /**
     * Return the timestamp from when visad.jar was created.
     * 
     * @return {@code String} representation of the creation timestamp.
     * Likely to change formatting over time.
     */
    public String getVisadDate() {
        if (visadDate == null) {
            Map<String, String> props = SystemState.queryVisadBuildProperties();
            visadDate = props.get(Constants.PROP_VISAD_DATE);
            visadVersion = props.get(Constants.PROP_VISAD_REVISION);
        }
        return visadDate;
    }
    
    /**
     * Return the {@literal "version"} of VisAD.
     * 
     * @return Currently returns whatever the SVN revision number was when
     * visad.jar was built. 
     */
    public String getVisadVersion() {
        if (visadVersion == null) {
            Map<String, String> props = SystemState.queryVisadBuildProperties();
            visadDate = props.get(Constants.PROP_VISAD_DATE);
            visadVersion = props.get(Constants.PROP_VISAD_REVISION);
        }
        return visadVersion;
    }
    
    public String getIdvVersion() {
        return getVersion();
    }
    
    /**
     * Overridden to set default of McIDAS-V
     */
    @Override public String getStoreSystemName() {
        return StartupManager.getInstance().getPlatform().getUserDirectory();
    }
    
    /**
     * Overridden to get dir of the unnecessary second level directory.
     */
    @Override public String getStoreName() {
        return "";
    }
    
    /**
     * Connect to McIDAS-V website and look for latest stable version.
     *
     * @return Latest stable version.
     */
    public String getMcIdasVersionStable() {
        String offscreen = "0";
        if (getIdv().getArgsManager().getIsOffScreen()) {
            offscreen = "1";
        }

        String version = "";
        try {
            version = IOUtil.readContents(Constants.HOMEPAGE_URL+"/"+Constants.VERSION_HANDLER_URL+"?v="+getMcIdasVersion()+"&os="+getOSName()+"&off="+offscreen, "");
        } catch (Exception e) {
            logger.warn("Could not get latest McV stable version", e);
        }
        return version.trim();
    }
    
    /**
     * Connect to McIDAS-V website and look for latest pre-release version.
     *
     * @return Latest pre-release version.
     */
    public String getMcIdasVersionPrerelease() {
        String version = "";
        try {
            String htmlList = IOUtil.readContents(Constants.HOMEPAGE_URL+'/'+Constants.PRERELEASE_URL, "");
            String lines[] = htmlList.split("\n");
            for (int i=0; i<lines.length; i++) {
                String line = lines[i].trim();
                if (line.matches(".*McIDAS-V_\\d+\\.\\d+.*")) {
                    line = line.substring(line.indexOf("McIDAS-V_")+9);
                    String aVersion = line.substring(0, line.indexOf("_"));
                    if (version.isEmpty()) {
                        version = aVersion;
                    } else {
                        int comp = compareVersions(version, aVersion);
                        if (comp > 0) {
                            version = aVersion;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Could not get latest McV pre-release version", e);
        }
        return version.trim();
    }
    
    /**
     * Connect to McIDAS website and look for latest notice.
     *
     * @return Contents of notice. String may be empty.
     */
    public String getNoticeLatest() {
        String notice = "";
        try {
            notice = IOUtil.readContents(Constants.HOMEPAGE_URL+"/"+Constants.NOTICE_URL+"?requesting="+getMcIdasVersion()+"&os="+getOSName(), "");
        } catch (Exception e) {
            logger.warn("Could not get latest notice", e);
        }
        if (!notice.contains("<notice>")) {
            notice = "";
        }
        notice = notice.replaceAll("<[/?]notice>","");
        return notice.trim();
    }
    
    /**
     * Compare version strings.
     *
     * <p>The logic is as follows.
     * <pre>
     *    0: thisVersion and thatVersion are equal.
     *   &lt;0: thisVersion is greater.
     *   &gt;0: thatVersion is greater.
     * </pre>
     *
     * @param thisVersion First version string to compare.
     * @param thatVersion Second version string to compare.
     *
     * @return Value indicating which of {@code thisVersion} and
     * {@code thatVersion} is {@literal "greater"}.
     */
    public static int compareVersions(String thisVersion, String thatVersion) {
        int thisInt = versionToInteger(thisVersion);
        int thatInt = versionToInteger(thatVersion);
        return thatInt - thisInt;
    }
    
    /**
     * Turn version strings of the form {@code #.#(a#)}, where # is one or two
     * digits, a is one of alpha or beta, and () is optional, into an integer
     * value... (empty) &gt; beta &gt; alpha.
     *
     * @param version String representation of version number.
     *
     * @return Integer representation of {@code version}.
     */
    public static int versionToInteger(String version) {
        int value = 0;
        int p;
        String part;
        Character one = null;
        
        try {
            // Major version
            p = version.indexOf('.');
            if (p > 0) {
                part = version.substring(0,p);
                value += Integer.parseInt(part) * 1000000;
                version = version.substring(p+1);
            }
            
            // Minor version
            int minor = 0;
            int i = 0;
            for (i = 0; i < 2 && i < version.length(); i++) {
                one = version.charAt(i);
                if (Character.isDigit(one)) {
                    if (i > 0) {
                        minor *= 10;
                    }
                    minor += Character.digit(one, 10) * 10000;
                } else {
                    break;
                }
            }
            value += minor;
            if (one != null) {
                version = version.substring(i);
            }
            
            // Alpha/beta/update/release status
            if (version.length() == 0) {
                value += 300;
            } else if (version.charAt(0) == 'b') {
                value += 200;
            } else if (version.charAt(0) == 'a') {
                value += 100;
            } else if (version.charAt(0) == 'u') {
                value += 400;
            } else if (version.charAt(0) == 'r') {
                value += 400;
            }
            for (i = 0; i < version.length(); i++) {
                one = version.charAt(i);
                if (Character.isDigit(one)) {
                    break;
                }
            }
            if (one != null) {
                version = version.substring(i);
            }
            
            // Alpha/beta version
            if (version.length() > 0) {
                value += Integer.parseInt(version);
            }
        } catch (Exception e) {
            
        }
        return value;
    }
    
    public boolean getIsPrerelease() {
        boolean isPrerelease = false;
        String version = getMcIdasVersion();
        if (version.contains("a") || version.contains("b")) {
            isPrerelease = true;
        }
        return isPrerelease;
    }
    
    public void checkForNewerVersion(boolean notifyDialog) {
        checkForNewerVersionStable(notifyDialog);
        if (getStore().get(Constants.PREF_PRERELEASE_CHECK, getIsPrerelease())) {
            checkForNewerVersionPrerelease(notifyDialog);
        }
    }
    
    public void checkForNewerVersionStable(boolean notifyDialog) {
        
        // get the stable version from the website (for statistics recording)
        String thatVersion = getMcIdasVersionStable();
        
        // shortcut the rest of the process if we are processing offscreen
        if (getIdv().getArgsManager().getIsOffScreen()) {
            return;
        }
        
        String thisVersion = getMcIdasVersion();
        String titleText = "Version Check";
        
        if (thisVersion.isEmpty() || thatVersion.isEmpty()) {
            if (notifyDialog) {
                JOptionPane.showMessageDialog(null,
                                              "Version check failed",
                                              titleText,
                                              JOptionPane.WARNING_MESSAGE);
            }
        } else if (compareVersions(thisVersion, thatVersion) > 0) {
            String labelText = "<html>Version <b>" + thatVersion + "</b> is available<br><br>";
            labelText += "Visit <a href=\"" + Constants.HOMEPAGE_URL + "\">";
            labelText += Constants.HOMEPAGE_URL + "</a> to download</html>";
            
            JPanel backgroundColorGetterPanel = new JPanel();
            JEditorPane messageText = new JEditorPane("text/html", labelText);
            messageText.setBackground(backgroundColorGetterPanel.getBackground());
            messageText.setEditable(false);
            messageText.addHyperlinkListener(this);
            
            //JLabel message = new JLabel(labelText, JLabel.CENTER);
            JOptionPane.showMessageDialog(null,
                                          messageText,
                                          titleText,
                                          JOptionPane.INFORMATION_MESSAGE);
        } else {
            if (notifyDialog) {
                String labelText = "<html>This version (<b>" + thisVersion + "</b>) is up to date</html>";
                JLabel message = new JLabel(labelText, JLabel.CENTER);
                JOptionPane.showMessageDialog(null,
                                              message,
                                              titleText,
                                              JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }
    
    public void checkForNewerVersionPrerelease(boolean notifyDialog) {
        
        // shortcut the rest of the process if we are processing offscreen
        if (getIdv().getArgsManager().getIsOffScreen()) {
            return;
        }
        
        String thisVersion = getMcIdasVersion();
        String thatVersion = getMcIdasVersionPrerelease();
        String titleText = "Prerelease Check";
        
        if (thisVersion.isEmpty() || thatVersion.isEmpty()) {
            if (notifyDialog) {
                JOptionPane.showMessageDialog(
                    null, "No prerelease version available", titleText,
                    JOptionPane.WARNING_MESSAGE);
            }
        } else if (compareVersions(thisVersion, thatVersion) > 0) {
            String labelText = "<html>Prerelease <b>" + thatVersion + "</b> is available<br><br>";
            labelText += "Visit <a href=\"" + Constants.HOMEPAGE_URL+'/'+Constants.PRERELEASE_URL + "\">";
            labelText += Constants.HOMEPAGE_URL+'/'+Constants.PRERELEASE_URL + "</a> to download</html>";
            
            JPanel backgroundColorGetterPanel = new JPanel();
            JEditorPane messageText = new JEditorPane("text/html", labelText);
            messageText.setBackground(backgroundColorGetterPanel.getBackground());
            messageText.setEditable(false);
            messageText.addHyperlinkListener(this);
            
            // JLabel message = new JLabel(labelText, JLabel.CENTER);
            JOptionPane.showMessageDialog(null,
                                          messageText,
                                          titleText,
                                          JOptionPane.INFORMATION_MESSAGE);
        } else {
            if (notifyDialog) {
                String labelText = "<html>This version (<b>" + thisVersion + "</b>) is up to date</html>";
                JLabel message = new JLabel(labelText, JLabel.CENTER);
                JOptionPane.showMessageDialog(null,
                                              message,
                                              titleText,
                                              JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }
    
    public void checkForNotice(boolean notifyDialog) {
        
        // Shortcut this whole process if we are processing offscreen
        if (getIdv().getArgsManager().getIsOffScreen()) {
            return;
        }
        
        String thisNotice = getNoticeCached().trim();
        String thatNotice = getNoticeLatest().trim();
        String titleText = "New Notice";
        String labelText = thatNotice;
        
        if (thatNotice.isEmpty()) {
            setNoticeCached(thatNotice);
            if (notifyDialog) {
                titleText = "No Notice";
                JLabel message = new JLabel("There is no current notice", JLabel.CENTER);
                JOptionPane.showMessageDialog(null, message, titleText,
                    JOptionPane.INFORMATION_MESSAGE);
            }
        } else if (!thisNotice.equals(thatNotice)) {
            setNoticeCached(thatNotice);
            
            JPanel backgroundColorGetterPanel = new JPanel();
            JEditorPane messageText = new JEditorPane("text/html", labelText);
            messageText.setBackground(backgroundColorGetterPanel.getBackground());
            messageText.setEditable(false);
            messageText.addHyperlinkListener(this);
            JOptionPane.showMessageDialog(null, messageText, titleText,
                JOptionPane.INFORMATION_MESSAGE);
        } else {
            if (notifyDialog) {
                titleText = "Previous Notice";
                JPanel bgPanel = new JPanel();
                JEditorPane messageText =
                    new JEditorPane("text/html", labelText);
                messageText.setBackground(bgPanel.getBackground());
                messageText.setEditable(false);
                messageText.addHyperlinkListener(this);
                JOptionPane.showMessageDialog(null, messageText, titleText,
                    JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }
    
    /**
     * Debug a McIDAS-V {@literal "system notice"} before sending it to all
     * users!
     *
     * @param noticeContents Contents of the notice.
     * @param notifyDialog if {@code true}, show notice even if already seen.
     * @param disableCache Whether or not {@code noticeContents} will be cached.
     */
    public void debugNotice(String noticeContents, boolean notifyDialog,
                            boolean disableCache)
    {
        // Shortcut this whole process if we are processing offscreen
        if (getIdv().getArgsManager().getIsOffScreen()) {
            return;
        }
        
        String thisNotice;
        thisNotice = disableCache ? "" : getNoticeCached().trim();
        String thatNotice = noticeContents.trim();
        String labelText = thatNotice;
        
        if (thatNotice.isEmpty()) {
            if (!disableCache) {
                setNoticeCached(thatNotice);
            }
            if (notifyDialog) {
                String titleText = "No Notice";
                JLabel message =
                    new JLabel("There is no current notice", JLabel.CENTER);
                JOptionPane.showMessageDialog(null, message, titleText,
                    JOptionPane.INFORMATION_MESSAGE);
            }
        } else if (!thisNotice.equals(thatNotice)) {
            if (!disableCache) {
                setNoticeCached(thatNotice);
            }
            String titleText = "New Notice";
            JPanel backgroundColorGetterPanel = new JPanel();
            JEditorPane messageText = new JEditorPane("text/html", labelText);
            messageText.setBackground(backgroundColorGetterPanel.getBackground());
            messageText.setEditable(false);
            messageText.addHyperlinkListener(this);
            JOptionPane.showMessageDialog(null, messageText, titleText,
                JOptionPane.INFORMATION_MESSAGE);
        } else {
            if (notifyDialog) {
                String titleText = "Previous Notice";
                JPanel bgPanel = new JPanel();
                JEditorPane messageText =
                    new JEditorPane("text/html", labelText);
                messageText.setBackground(bgPanel.getBackground());
                messageText.setEditable(false);
                messageText.addHyperlinkListener(this);
                JOptionPane.showMessageDialog(null, messageText, titleText,
                    JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }
    
    private String getNoticePath() {
        return StartupManager.getInstance().getPlatform().getUserFile("notice.txt");
    }
    
    private String getNoticeCached() {
        StringBuilder notice = new StringBuilder(1024);
        try{
            FileReader fstream = new FileReader(getNoticePath());
            BufferedReader in = new BufferedReader(fstream);
            String line;
            while ((line = in.readLine()) != null) {
                notice.append(line).append('\n');
            }
            in.close();
        } catch (Exception e) {
            logger.warn("Could not get cached notice", e);
        }
        return notice.toString();
    }
    
    private void setNoticeCached(String notice) {
        try {
            FileWriter fstream = new FileWriter(getNoticePath());
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(notice);
            out.close();
        } catch (Exception e) {
            logger.warn("Could not cache downloaded notice", e);
        }
    }
}
