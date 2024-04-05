/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2024
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * https://www.ssec.wisc.edu/mcidas/
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
 * along with this program.  If not, see https://www.gnu.org/licenses/.
 */
package edu.wisc.ssec.mcidasv.util;

import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.arrList;
import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.newLinkedHashMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.TreeSet;

import java.awt.DisplayMode;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.VirtualUniverse;

import com.google.common.base.Splitter;
import org.python.core.Py;
import org.python.core.PySystemState;

import org.python.modules.posix.PosixModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.nc2.grib.GribConverterUtility;
import ucar.unidata.idv.ArgsManager;
import ucar.unidata.idv.IdvResourceManager.IdvResource;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.ResourceCollection;
import ucar.visad.display.DisplayUtil;

import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.McIDASV;
import edu.wisc.ssec.mcidasv.StateManager;

/**
 * Utility methods for querying the state of the user's machine.
 */
public class SystemState {
    
    /** Handy logging object. */
    private static final Logger logger = 
        LoggerFactory.getLogger(SystemState.class);
        
    // Don't allow outside instantiation.
    private SystemState() { }
    
    public static String escapeWhitespaceChars(final CharSequence sequence) {
        StringBuilder sb = new StringBuilder(sequence.length() * 7);
        for (int i = 0; i < sequence.length(); i++) {
            switch (sequence.charAt(i)) {
                case '\t': sb.append("\\t"); break;
                case '\n': sb.append('\\').append('n'); break;
                case '\013': sb.append("\\013"); break;
                case '\f': sb.append("\\f"); break;
                case '\r': sb.append("\\r"); break;
                case '\u0085': sb.append("\\u0085"); break;
                case '\u1680': sb.append("\\u1680"); break;
                case '\u2028': sb.append("\\u2028"); break;
                case '\u2029': sb.append("\\u2029"); break;
                case '\u205f': sb.append("\\u205f"); break;
                case '\u3000': sb.append("\\u3000"); break;
            }
        }
        return sb.toString();
    }
    
    /**
     * Query an {@link OperatingSystemMXBean} attribute and return the result.
     *
     * @param <T> Type of the expected return value and {@code defaultValue}.
     * @param attrName Name of the {@code OperatingSystemMXBean} attribute to 
     *                 query. Cannot be {@code null} or empty.
     * @param defaultValue Value returned if {@code attrName} could not be 
     *                     queried.
     *
     * @return Either the value corresponding to {@code attrName} or 
     *         {@code defaultValue}.
     */
    private static <T> T queryPlatformBean(final String attrName,
                                           final T defaultValue)
    {
        assert attrName != null : "Cannot query a null attribute name";
        assert !attrName.isEmpty() : "Cannot query an empty attribute name";
        T result = defaultValue;
        try {
            final ObjectName objName = 
                new ObjectName("java.lang", "type", "OperatingSystem");
            final MBeanServer beanServer = 
                ManagementFactory.getPlatformMBeanServer();
            final Object attr = beanServer.getAttribute(objName, attrName);
            if (attr != null) {
                // don't suppress warnings because we cannot guarantee that 
                // this cast is correct.
                result = (T)attr;
            }
        } catch (Exception e) {
            logger.error("Couldn't query attribute: " + attrName, e);
        }
        return result;
    }
    
    /**
     * Returns the contents of Jython's registry (basically just Jython-specific
     * properties) as well as some of the information from Python's 
     * {@literal "sys"} module. 
     * 
     * @return Jython's configuration settings. 
     */
    public static Map<Object, Object> queryJythonProps() {
        Map<Object, Object> properties = newLinkedHashMap(PySystemState.registry);
        try (PySystemState systemState = Py.getSystemState()) {
            properties.put("sys.argv", systemState.argv.toString());
            properties.put("sys.path", systemState.path);
            properties.put("sys.platform", systemState.platform.toString());
        }
        properties.put("sys.builtin_module_names", PySystemState.builtin_module_names.toString());
        properties.put("sys.byteorder", PySystemState.byteorder);
        properties.put("sys.isPackageCacheEnabled", PySystemState.isPackageCacheEnabled());
        properties.put("sys.version", PySystemState.version);
        properties.put("sys.version_info", PySystemState.version_info);
        return properties;
    }
    
    /**
     * Attempts to call methods belonging to 
     * {@code com.sun.management.OperatingSystemMXBean}. If successful, we'll
     * have the following information:
     * <ul>
     *   <li>opsys.memory.virtual.committed: virtual memory that is guaranteed to be available</li>
     *   <li>opsys.memory.swap.total: total amount of swap space in bytes</li>
     *   <li>opsys.memory.swap.free: free swap space in bytes</li>
     *   <li>opsys.cpu.time: CPU time used by the process (nanoseconds)</li>
     *   <li>opsys.memory.physical.free: free physical memory in bytes</li>
     *   <li>opsys.memory.physical.total: physical memory in bytes</li>
     *   <li>opsys.load: system load average for the last minute</li>
     * </ul>
     * 
     * @return Map of properties that contains interesting information about
     * the hardware McIDAS-V is using.
     */
    public static Map<String, String> queryOpSysProps() {
        Map<String, String> properties = newLinkedHashMap(10);
        long committed = queryPlatformBean("CommittedVirtualMemorySize", Long.MIN_VALUE);
        long freeMemory = queryPlatformBean("FreePhysicalMemorySize", Long.MIN_VALUE);
        long freeSwap = queryPlatformBean("FreeSwapSpaceSize", Long.MIN_VALUE);
        long cpuTime = queryPlatformBean("ProcessCpuTime", Long.MIN_VALUE);
        long totalMemory = queryPlatformBean("TotalPhysicalMemorySize", Long.MIN_VALUE);
        long totalSwap = queryPlatformBean("TotalSwapSpaceSize", Long.MIN_VALUE);
        double loadAvg = queryPlatformBean("SystemLoadAverage", Double.NaN);
        
        Runtime rt = Runtime.getRuntime();
        long currentMem = rt.totalMemory() - rt.freeMemory();
        
        properties.put("opsys.cpu.time", Long.toString(cpuTime));
        properties.put("opsys.load", Double.toString(loadAvg));
        properties.put("opsys.memory.jvm.current", Long.toString(currentMem));
        properties.put("opsys.memory.jvm.max", Long.toString(rt.maxMemory()));
        properties.put("opsys.memory.virtual.committed", Long.toString(committed));
        properties.put("opsys.memory.physical.free", Long.toString(freeMemory));
        properties.put("opsys.memory.physical.total", Long.toString(totalMemory));
        properties.put("opsys.memory.swap.free", Long.toString(freeSwap));
        properties.put("opsys.memory.swap.total", Long.toString(totalSwap));
        
        return properties;
    }
    
    /**
     * Polls Java for information about the user's machine. We're specifically
     * after memory statistics, number of processors, and display information.
     * 
     * @return {@link Map} of properties that describes the user's machine.
     */
    public static Map<String, String> queryMachine() {
        Map<String, String> props = newLinkedHashMap();
        
        // cpu count and whatnot
        int processors = Runtime.getRuntime().availableProcessors();
        props.put("opsys.cpu.count", Integer.toString(processors));
        
        // memory: available, used, etc
        props.putAll(queryOpSysProps());
        
        // screen: count, resolution(s)
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        int displayCount = ge.getScreenDevices().length;
        
        for (int i = 0; i < displayCount; i++) {
            String baseId = "opsys.display."+i+'.';
            GraphicsDevice dev = ge.getScreenDevices()[i];
            DisplayMode mode = dev.getDisplayMode();
            props.put(baseId+"name", dev.getIDstring());
            props.put(baseId+"depth", Integer.toString(mode.getBitDepth()));
            props.put(baseId+"width", Integer.toString(mode.getWidth()));
            props.put(baseId+"height", Integer.toString(mode.getHeight()));
            props.put(baseId+"refresh", Integer.toString(mode.getRefreshRate()));
        }
        return props;
    }
    
    /**
     * Returns a mapping of display number to a {@link java.awt.Rectangle} 
     * that represents the {@literal "bounds"} of the display.
     *
     * @return Rectangles representing the {@literal "bounds"} of the current
     * display devices.
     */
    public static Map<Integer, Rectangle> getDisplayBounds() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        int idx = 0;
        Map<Integer, Rectangle> map = newLinkedHashMap(ge.getScreenDevices().length * 2);
        for (GraphicsDevice dev : ge.getScreenDevices()) {
            for (GraphicsConfiguration config : dev.getConfigurations()) {
                map.put(idx++, config.getBounds());
            }
        }
        return map;
    }
    
    // TODO(jon): this should really be a polygon
    public static Rectangle getVirtualDisplayBounds() {
        Rectangle virtualBounds = new Rectangle();
        for (Rectangle bounds : getDisplayBounds().values()) {
            virtualBounds = virtualBounds.union(bounds);
        }
        return virtualBounds;
    }
    
    /**
     * Polls Java 3D for information about its environment. Specifically, we 
     * call {@link VirtualUniverse#getProperties()} and 
     * {@link Canvas3D#queryProperties()}.
     * 
     * @return As much information as Java 3D can provide.
     */
    @SuppressWarnings("unchecked") // casting to Object, so this should be fine.
    public static Map<String, Object> queryJava3d() {
        
        Map<String, Object> universeProps = 
            (Map<String, Object>)VirtualUniverse.getProperties();
            
        GraphicsConfiguration config =
            DisplayUtil.getPreferredConfig(null, true, false);
        Map<String, Object> c3dMap = new Canvas3D(config).queryProperties();
        
        Map<String, Object> props =
                newLinkedHashMap(universeProps.size() + c3dMap.size());
        props.putAll(universeProps);
        props.putAll(c3dMap);
        return props;
    }
    
    /**
     * Gets a human-friendly representation of the information embedded within
     * IDV's {@code build.properties}.
     *
     * @return {@code String} that looks like {@literal "IDV version major.minor<b>revision</b> built <b>date</b>"}.
     * For example: {@code IDV version 2.9u4 built 2011-04-13 14:01 UTC}.
     */
    public static String getIdvVersionString() {
        Map<String, String> info = queryIdvBuildProperties();
        return "IDV version " + info.get("idv.version.major") + '.' +
               info.get("idv.version.minor") + info.get("idv.version.revision") +
               " built " + info.get("idv.build.date");
    }
    
    /**
     * Gets a human-friendly representation of the information embedded within
     * McIDAS-V's {@code build.properties}.
     * 
     * @return {@code String} that looks like {@literal "McIDAS-V version major.minor<b>release</b> built <b>date</b>"}.
     * For example: {@code McIDAS-V version 1.02beta1 built 2011-04-14 17:36}.
     */
    public static String getMcvVersionString() {
        Map<String, String> info = queryMcvBuildProperties();
        return "McIDAS-V version " + info.get(Constants.PROP_VERSION_MAJOR) + '.' +
               info.get(Constants.PROP_VERSION_MINOR) + info.get(Constants.PROP_VERSION_RELEASE) +
               " built " + info.get(Constants.PROP_BUILD_DATE);
    }
    
    /**
     * Gets a human-friendly representation of the version information embedded 
     * within VisAD's {@literal "DATE"} file.
     * 
     * @return {@code String} that looks
     * {@literal "VisAD version <b>revision</b> built <b>date</b>"}.
     * For example: {@code VisAD version 5952 built Thu Mar 22 13:01:31 CDT 2012}.
     */
    public static String getVisadVersionString() {
        Map<String, String> props = queryVisadBuildProperties();
        return "VisAD version " + props.get(Constants.PROP_VISAD_REVISION) + " built " + props.get(Constants.PROP_VISAD_DATE);
    }
    
    /**
     * Gets a human-friendly representation of the ncIdv.jar version
     * information.
     *
     * @return {@code String} that looks like
     * {@literal "netCDF-Java version <b>revision</b> <b>built</b>"}.
     */
    public static String getNcidvVersionString() {
        Map<String, String> props = queryNcidvBuildProperties();
        return "netCDF-Java version " + props.get("version") + " built " + props.get("buildDate");
    }
    
    /**
     * Open a file for reading.
     *
     * @param name File to open.
     *
     * @return {@code InputStream} used to read {@code name}, or {@code null}
     * if {@code name} could not be found.
     */
    private static InputStream resourceAsStream(final String name) {
        return ClassLoader.getSystemResourceAsStream(name);
    }
    
    /**
     * Returns a {@link Map} containing any relevant version information. 
     * 
     * <p>Currently this information consists of the date visad.jar was built, 
     * as well as the (then-current) Subversion revision number.
     * 
     * @return {@code Map} of the contents of VisAD's DATE file.
     */
    public static Map<String, String> queryVisadBuildProperties() {
        Map<String, String> props = newLinkedHashMap(4);
        
        try (BufferedReader input = new BufferedReader(
                                        new InputStreamReader(
                                            resourceAsStream("DATE")))) 
        {
            String contents = input.readLine();
            // string should look like: Thu Mar 22 13:01:31 CDT 2012  Rev:5952
            String splitAt = "  Rev:";
            int index = contents.indexOf(splitAt);
            String buildDate = "ERROR";
            String revision = "ERROR";
            String parseFail = "true";
            if (index > 0) {
                buildDate = contents.substring(0, index);
                revision = contents.substring(index + splitAt.length());
                parseFail = "false";
            }
            props.put(Constants.PROP_VISAD_ORIGINAL, contents);
            props.put(Constants.PROP_VISAD_PARSE_FAIL, parseFail);
            props.put(Constants.PROP_VISAD_DATE, buildDate);
            props.put(Constants.PROP_VISAD_REVISION, revision);
        } catch (IOException e) {
            logger.error("could not read from VisAD DATE file", e);
        }
        return props;
    }
    
    /**
     * Determine the actual name of the McIDAS-V JAR file.
     *
     * <p>This is needed because we're now following the Maven naming
     * convention, and this means that {@literal "mcidasv.jar"} no longer
     * exists.</p>
     *
     * @param jarDir Directory containing all of the McIDAS-V JARs.
     *               Cannot be {@code null}.
     *
     * @return Name (note: not path) of the McIDAS-V JAR file.
     *
     * @throws IOException if there was a problem locating the McIDAS-V JAR.
     */
    private static String getMcvJarname(String jarDir)
        throws IOException
    {
        File dir = new File(jarDir);
        File[] files = dir.listFiles();
        if (files == null) {
            throw new IOException("Could not get list of files within " +
                "'"+jarDir+"'");
        }
        for (File f : files) {
            String name = f.getName();
            if (name.startsWith("mcidasv-") && name.endsWith(".jar")) {
                return name;
            }
        }
        throw new FileNotFoundException("Could not find McIDAS-V JAR file");
    }
    
    /**
     * Return McIDAS-V's classpath.
     *
     * <p>This may differ from what is reported by 
     * {@code System.getProperty("java.class.path")}. This is because McIDAS-V
     * specifies its classpath using {@code META-INF/MANIFEST.MF} in 
     * {@code mcidasv.jar}.
     * </p>
     *
     * <p>This is used by console_init.py to figure out where the VisAD,
     * IDV, and McIDAS-V JAR files are located.</p>
     *
     * @return Either a list of strings containing the path to each JAR file
     * in the classpath, or an empty list.
     */
    public static List<String> getMcvJarClasspath() {
        String jarDir = System.getProperty("user.dir");
        if (jarDir == null) {
            jarDir = PosixModule.getcwd().toString();
        }

        // 64 chosen because we're currently at 38 JARs.
        List<String> jars = arrList(64);
        try {
            String mcvJar = getMcvJarname(jarDir);
            Path p = Paths.get(jarDir, mcvJar);
            String path = "jar:file:"+p.toString()+"!/META-INF/MANIFEST.MF";
            InputStream stream = IOUtil.getInputStream(path);
            if (stream != null) {
                Manifest manifest = new Manifest(stream);
                Attributes attrs = manifest.getMainAttributes();
                if (attrs != null) {
                    String classpath = attrs.getValue("Class-Path");
                    Splitter split = Splitter.on(' ')
                        .trimResults()
                        .omitEmptyStrings();
                    for (String jar : split.split(classpath)) {
                        jars.add(Paths.get(jarDir, jar).toFile().toString());
                    }
                }
            }
        } catch (IOException | NullPointerException e) {
            logger.warn("Exception occurred:", e);
        }
        return jars;
    }
    
    /**
     * Returns a {@link Map} containing {@code version} and {@code buildDate}
     * keys.
     *
     * @return {@code Map} containing netCDF-Java version and build date.
     */
    public static Map<String, String> queryNcidvBuildProperties() {
        // largely taken from LibVersionUtil's getBuildInfo method.
        GribConverterUtility util = new GribConverterUtility();
        Map<String, String> buildInfo = new HashMap<>(4);
        // format from ncidv.jar
        SimpleDateFormat formatIn =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        // format of output timestamp
        SimpleDateFormat formatOut =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
        formatOut.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            Enumeration<URL> resources = 
                util.getClass()
                    .getClassLoader()
                    .getResources("META-INF/MANIFEST.MF");
                    
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if (!resource.toString().contains("ncIdv")) {
                    // skip over all of the JARs in the classpath except for ncIdv.
                    continue;
                }
                Manifest manifest =
                    new Manifest(resource.openStream());
                Attributes attrs = manifest.getMainAttributes();
                if (attrs != null) {
                    buildInfo.put(
                        "version",
                        attrs.getValue("Implementation-Version"));
                    String strDate = attrs.getValue("Built-On");
                    Date date = formatIn.parse(strDate);
                    buildInfo.put("buildDate", formatOut.format(date));
                    break;
                }
            }
        } catch (IOException e) {
            logger.warn("exception occurred:", e);
        } catch (ParseException pe) {
            logger.warn("failed to parse build date from ncIdv.jar", pe);
        }
        return buildInfo;
    }
    
    /**
     * Returns a {@link Map} of the (currently) most useful contents of
     * {@code ucar/unidata/idv/resources/build.properties}.
     *
     * <p>Consider the output of {@link #getIdvVersionString()}; it's built
     * with the the following:
     * <ul>
     *   <li><b>{@code idv.version.major}</b>: currently {@literal "3"}</li>
     *   <li><b>{@code idv.version.minor}</b>: currently {@literal "0"}</li>
     *   <li><b>{@code idv.version.revision}</b>: currently {@literal "u2"}}</li>
     *   <li><b>{@code idv.build.date}</b>: varies pretty frequently,
     *   as it's the build timestamp for idv.jar</li>
     * </ul>
     *
     * @return A {@code Map} of at least the useful parts of build.properties.
     */
    public static Map<String, String> queryIdvBuildProperties() {
        Map<String, String> versions = newLinkedHashMap(4);
        InputStream input = null;
        try {
            input = resourceAsStream("ucar/unidata/idv/resources/build.properties");
            Properties props = new Properties();
            props.load(input);
            String major = props.getProperty("idv.version.major", "no_major");
            String minor = props.getProperty("idv.version.minor", "no_minor");
            String revision = props.getProperty("idv.version.revision", "no_revision");
            String date = props.getProperty("idv.build.date", "");
            versions.put("idv.version.major", major);
            versions.put("idv.version.minor", minor);
            versions.put("idv.version.revision", revision);
            versions.put("idv.build.date", date);
        } catch (Exception e) {
            logger.error("could not read from IDV build.properties", e);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (Exception ex) {
                    logger.error("could not close IDV build.properties", ex);
                }
            }
        }
        return versions;
    }
    
    /**
     * Returns a {@link Map} of the (currently) most useful contents of
     * {@code edu/wisc/ssec/mcidasv/resources/build.properties}.
     *
     * <p>Consider the output of {@link #getMcvVersionString()}; it's built
     * with the the following:
     * <ul>
     *   <li><b>{@code mcidasv.version.major}</b>:
     *   currently {@literal "1"}</li>
     *   <li><b>{@code mcidasv.version.minor}</b>:
     *   currently {@literal "02"}</li>
     *   <li><b>{@code mcidasv.version.release}</b>: currently
     *   {@literal "beta1"}</li>
     *   <li><b>{@code mcidasv.build.date}</b>: varies pretty frequently, as
     *   it's the build timestamp for mcidasv.jar.</li>
     * </ul>
     *
     * @return A {@code Map} of at least the useful parts of build.properties.
     */
    public static Map<String, String> queryMcvBuildProperties() {
        Map<String, String> versions = newLinkedHashMap(4);
        InputStream input = null;
        try {
            input = resourceAsStream("edu/wisc/ssec/mcidasv/resources/build.properties");
            Properties props = new Properties();
            props.load(input);
            String major = props.getProperty(Constants.PROP_VERSION_MAJOR, "0");
            String minor = props.getProperty(Constants.PROP_VERSION_MINOR, "0");
            String release = props.getProperty(Constants.PROP_VERSION_RELEASE, "");
            String date = props.getProperty(Constants.PROP_BUILD_DATE, "Unknown");
            versions.put(Constants.PROP_VERSION_MAJOR, major);
            versions.put(Constants.PROP_VERSION_MINOR, minor);
            versions.put(Constants.PROP_VERSION_RELEASE, release);
            versions.put(Constants.PROP_BUILD_DATE, date);
        } catch (Exception e) {
            logger.error("could not read from McIDAS-V build.properties!", e);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (Exception ex) {
                    logger.error("could not close McIDAS-V build.properties!", ex);
                }
            }
        }
        return versions;
    }
    
    /**
     * Queries McIDAS-V for information about its state. There's not a good way
     * to characterize what we're interested in, so let's leave it at 
     * {@literal "whatever seems useful"}.
     * 
     * @param mcv The McIDASV {@literal "god"} object.
     * 
     * @return Information about the state of McIDAS-V.
     */
    // need: argsmanager, resource manager
    public static Map<String, Object> queryMcvState(final McIDASV mcv) {
        // through some simple verification, props generally has under 250 elements
        Map<String, Object> props = newLinkedHashMap(250);
        
        ArgsManager args = mcv.getArgsManager();
        props.put("mcv.state.islinteractive", args.getIslInteractive());
        props.put("mcv.state.offscreen", args.getIsOffScreen());
        props.put("mcv.state.initcatalogs", args.getInitCatalogs());
        props.put("mcv.state.actions", mcv.getActionHistory());
        props.put("mcv.plugins.installed", args.installPlugins);
        props.put("mcv.state.commandline", mcv.getCommandLineArgs());
        
        // loop through resources
        List<IdvResource> resources =
                (List<IdvResource>)mcv.getResourceManager().getResources();
        for (IdvResource resource : resources) {
            String id = resource.getId();
            props.put(id+".description", resource.getDescription());
            if (resource.getPattern() == null) {
                props.put(id+".pattern", "null");
            } else {
                props.put(id+".pattern", resource.getPattern());
            }
            
            ResourceCollection rc = 
                mcv.getResourceManager().getResources(resource);
            int rcSize = rc.size();
            List<String> specified = arrList(rcSize);
            List<String> valid = arrList(rcSize);
            for (int i = 0; i < rcSize; i++) {
                String tmpResource = (String)rc.get(i);
                specified.add(tmpResource);
                if (rc.isValid(i)) {
                    valid.add(tmpResource);
                }
            }
            
            props.put(id+".specified", specified);
            props.put(id+".existing", valid);
        }
        return props;
    }
    
    /**
     * Builds a (filtered) subset of the McIDAS-V system properties and returns
     * the results as a {@code String}.
     * 
     * @param mcv The McIDASV {@literal "god"} object.
     * 
     * @return The McIDAS-V system properties in the following format: 
     * {@code KEY=VALUE\n}. This is so we kinda-sorta conform to the standard
     * {@link Properties} file format.
     * 
     * @see #getStateAsString(edu.wisc.ssec.mcidasv.McIDASV, boolean)
     */
    public static String getStateAsString(final McIDASV mcv) {
        return getStateAsString(mcv, false);
    }
    
    /**
     * Builds the McIDAS-V system properties and returns the results as a 
     * {@code String}.
     * 
     * @param mcv The McIDASV {@literal "god"} object.
     * @param firehose If {@code true}, enables {@literal "unfiltered"} output.
     * 
     * @return The McIDAS-V system properties in the following format: 
     * {@code KEY=VALUE\n}. This is so we kinda-sorta conform to the standard
     * {@link Properties} file format.
     */
    public static String getStateAsString(final McIDASV mcv, final boolean firehose) {
        int builderSize = firehose ? 45000 : 1000;
        StringBuilder buf = new StringBuilder(builderSize);
        
        Map<String, String> versions = ((StateManager)mcv.getStateManager()).getVersionInfo();
        Properties sysProps = System.getProperties();
        Map<String, Object> j3dProps = queryJava3d();
        Map<String, String> machineProps = queryMachine();
        Map<Object, Object> jythonProps = queryJythonProps();
        Map<String, Object> mcvProps = queryMcvState(mcv);
        
        if (sysProps.contains("line.separator")) {
            sysProps.put("line.separator", escapeWhitespaceChars((String)sysProps.get("line.separator")));
        }
        
        String maxMem = Long.toString(Long.valueOf(machineProps.get("opsys.memory.jvm.max")) / 1048576L);
        String curMem = Long.toString(Long.valueOf(machineProps.get("opsys.memory.jvm.current")) / 1048576L);
        
        buf.append("# Software Versions:")
            .append("\n# McIDAS-V:    ").append(versions.get("mcv.version.general")).append(" (").append(versions.get("mcv.version.build")).append(')')
            .append("\n# VisAD:       ").append(versions.get("visad.version.general")).append(" (").append(versions.get("visad.version.build")).append(')')
            .append("\n# IDV:         ").append(versions.get("idv.version.general")).append(" (").append(versions.get("idv.version.build")).append(')')
            .append("\n# netcdf-Java: ").append(versions.get("netcdf.version.general")).append(" (").append(versions.get("netcdf.version.build")).append(')')
            .append("\n\n# Operating System:")
            .append("\n# Name:         ").append(sysProps.getProperty("os.name"))
            .append("\n# Version:      ").append(sysProps.getProperty("os.version"))
            .append("\n# Architecture: ").append(sysProps.getProperty("os.arch"))
            .append("\n\n# Java:")
            .append("\n# Version: ").append(sysProps.getProperty("java.version"))
            .append("\n# Vendor:  ").append(sysProps.getProperty("java.vendor"))
            .append("\n# Home:    ").append(sysProps.getProperty("java.home"))
            .append("\n\n# JVM Memory")
            .append("\n# Current: ").append(curMem).append(" MB")
            .append("\n# Maximum: ").append(maxMem).append(" MB")
            .append("\n\n# Java 3D:")
            .append("\n# Renderer: ").append(j3dProps.get("j3d.renderer"))
            .append("\n# Pipeline: ").append(j3dProps.get("j3d.pipeline"))
            .append("\n# Vendor:   ").append(j3dProps.get("native.vendor"))
            .append("\n# Version:  ").append(j3dProps.get("j3d.version"))
            .append("\n\n# Jython:")
            .append("\n# Version:     ").append(jythonProps.get("sys.version_info"))
            .append("\n# python.home: ").append(jythonProps.get("python.home"));
            
        if (firehose) {
            buf.append("\n\n\n#Firehose:\n\n# SOFTWARE VERSIONS\n");
            for (String key : new TreeSet<>(versions.keySet())) {
                buf.append(key).append('=').append(versions.get(key)).append('\n');
            }
            
            buf.append("\n# MACHINE PROPERTIES\n");
            for (String key : new TreeSet<>(machineProps.keySet())) {
                buf.append(key).append('=').append(machineProps.get(key)).append('\n');
            }
            
            buf.append("\n# JAVA SYSTEM PROPERTIES\n");
            for (Object key : new TreeSet<>(sysProps.keySet())) {
                buf.append(key).append('=').append(sysProps.get(key)).append('\n');
            }
            
            buf.append("\n# JAVA3D/JOGL PROPERTIES\n");
            for (String key : new TreeSet<>(j3dProps.keySet())) {
                buf.append(key).append('=').append(j3dProps.get(key)).append('\n');
            }
            
            buf.append("\n# JYTHON PROPERTIES\n");
            for (Object key : new TreeSet<>(jythonProps.keySet())) {
                buf.append(key).append('=').append(jythonProps.get(key)).append('\n');
            }
            
            // get idv/mcv properties
            buf.append("\n# IDV AND MCIDAS-V PROPERTIES\n");
            for (String key : new TreeSet<>(mcvProps.keySet())) {
                buf.append(key).append('=').append(mcvProps.get(key)).append('\n');
            }
        }
        return buf.toString();
    }
}
