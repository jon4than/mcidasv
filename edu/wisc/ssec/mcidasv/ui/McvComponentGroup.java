/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2025
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

package edu.wisc.ssec.mcidasv.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;

import com.formdev.flatlaf.FlatClientProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ucar.unidata.idv.IdvResourceManager;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.MapViewManager;
import ucar.unidata.idv.TransectViewManager;
import ucar.unidata.idv.ViewDescriptor;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.idv.ui.IdvComponentGroup;
import ucar.unidata.idv.ui.IdvComponentHolder;
import ucar.unidata.idv.ui.IdvUIManager;
import ucar.unidata.idv.ui.IdvWindow;
import ucar.unidata.ui.ComponentHolder;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LayoutUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Msg;
import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;

import edu.wisc.ssec.mcidasv.McIDASV;
import edu.wisc.ssec.mcidasv.PersistenceManager;

/**
 * Extends the IDV component groups so that we can intercept clicks for Bruce's
 * tab popup menu and handle drag and drop. It also intercepts ViewManager
 * creation in order to wrap components in McIDASVComponentHolders rather than
 * IdvComponentHolders. Doing this allows us to associate ViewManagers back to
 * their ComponentHolders, and this functionality is taken advantage of to form
 * the hierarchical names seen in the McIDASVViewPanel.
 */

public class McvComponentGroup extends IdvComponentGroup {
    
    private static final Logger logger =
        LoggerFactory.getLogger(McvComponentGroup.class);
    
    /** Path to the "close tab" icon in the popup menu. */
    protected static final String ICO_CLOSE =
        "/edu/wisc/ssec/mcidasv/resources/icons/tabmenu/stop-loads16.png";

    /** Path to the "rename" icon in the popup menu. */
    protected static final String ICO_RENAME =
        "/edu/wisc/ssec/mcidasv/resources/icons/tabmenu/accessories-text-editor16.png";

    /** Path to the eject icon in the popup menu. */
    protected static final String ICO_UNDOCK =
        "/edu/wisc/ssec/mcidasv/resources/icons/tabmenu/media-eject16.png";

    /** Action command for destroying a display. */
    private static final String CMD_DISPLAY_DESTROY = "DESTROY_DISPLAY_TAB";

    /** Action command for ejecting a display from a tab. */
    private static final String CMD_DISPLAY_EJECT = "EJECT_TAB";

    /** Action command for renaming a display. */
    private static final String CMD_DISPLAY_RENAME = "RENAME_DISPLAY";

    /** The popup menu for the McV tabbed display interface. */
    private final JPopupMenu popup = doMakeTabMenu();

    /** Number of tabs that have been stored in this group. */
    @SuppressWarnings("unused")
    private int tabCount = 0;

    /** Whether or not {@code init} has been called. */
    private boolean initDone = false;

    /**
     * Holders that McV knows are held by this component group. Used to avoid
     * any needless work in {@code redoLayout}.
     */
    private List<ComponentHolder> knownHolders = new ArrayList<>();

    /** Keep a reference to avoid extraneous calls to {@code getIdv()}. */
    private IntegratedDataViewer idv;

    /** Reference to the window associated with this group. */
    private IdvWindow window = IdvWindow.getActiveWindow();

    /** 
     * Whether or not {@link #redoLayout()} needs to worry about a renamed 
     * tab. 
     */
    private boolean tabRenamed = false;

    /**
     * Whether or not the {@literal "tab area"} should be visible if there is
     * only a single tab (defaults to {@code false}).
     */
    private boolean hideTabArea;

    /** Whether or not the title bar is hidden (defaults to {@code false}). */
    private boolean hideTitleBar;

    /**
     * Default constructor for serialization.
     */
    
    public McvComponentGroup() {}

    /**
     * A pretty typical constructor.
     * 
     * @param idv The main IDV instance.
     * @param name Presumably the name of this component group?
     */
    
    public McvComponentGroup(final IntegratedDataViewer idv, 
        final String name) 
    {
        super(idv, name);
        this.idv = idv;
        hideTabArea = false;
        hideTitleBar = false;
        init();
    }

    /**
     * This constructor catches the window that will be contained in this group.
     * 
     * @param idv The main IDV instance.
     * @param name Presumably the name of this component group?
     * @param window The window holding this component group.
     */
    
    public McvComponentGroup(final IntegratedDataViewer idv,
        final String name, final IdvWindow window) 
    {
        super(idv, name);
        this.window = window;
        this.idv = idv;
        hideTabArea = false;
        hideTitleBar = false;
        init();
    }

    public boolean getHideTabArea() {
//        logger.trace("val: {}", hideTabArea);
        return hideTabArea;
    }

    public void setHideTabArea(boolean hide) {
        hideTabArea = hide;
    }

    public boolean getHideTitleBar() {
        return hideTitleBar;
    }

    public void setHideTitleBar(boolean hide) {
        // note: you want to set this before "pack" is called!!
        hideTitleBar = hide;
    }

    /**
     * Initializes the various UI components.
     */
    
    private void init() {
        if (initDone) {
            return;
        }

        tabbedPane = new DraggableTabbedPane(window, idv, this);
        tabbedPane.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_CLOSABLE, true);
        tabbedPane.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_CLOSE_CALLBACK,
                (BiConsumer<JTabbedPane, Integer>) (tabPane, tabIndex) -> {
                    destroyDisplay(tabIndex);
                });

        // dark mode results in the previous MouseListener in DraggableTabbed not being able to
        // listen for mouse clicks. being unable to detect mouse clicks means that we lose the
        // ability to rename tabs via double-clicking on the tab.
        if (McIDASV.isDarkMode()) {
            tabbedPane.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                        int eventX = e.getX();
                        int eventY = e.getY();
                        int tabIndex = tabbedPane.getUI().tabForCoordinate(tabbedPane, eventX, eventY);
                        if (tabIndex >= 0) {
                            renameDisplay(tabIndex);
                        }
                    }
                    super.mouseClicked(e);
                }
            });
        }

        container = new JPanel(new BorderLayout());
        container.add(tabbedPane);
        GuiUtils.handleHeavyWeightComponentsInTabs(tabbedPane);
        initDone = true;
    }

    @Override public void initWith(Element node) {
        boolean myhideTabArea = XmlUtil.getAttribute(node, "hideTabArea", false);
        boolean myhideTitleBar = XmlUtil.getAttribute(node, "hideTitleBar", false);
//        logger.trace("node tabVal: {} tabField: {}", myhideTabArea, hideTabArea);
//        logger.trace("node titleVal: {} titleField: {}", myhideTitleBar, hideTitleBar);
        hideTabArea = myhideTabArea;
        hideTitleBar = myhideTitleBar;
        window.setUndecorated(hideTitleBar);
        super.initWith(node);
    }

    /**
     * Create and return the GUI contents. Overridden so that McV can implement
     * the right click tab menu and draggable tabs.
     * 
     * @return GUI contents
     */
    
    @Override public JComponent doMakeContents() {
        redoLayout();
        outerContainer = LayoutUtil.center(container);
        outerContainer.validate();
        return outerContainer;
    }

    /**
     * Importing a display control entails adding the control to the component
     * group and informing the UI that the control is no longer in its own
     * window.
     * 
     * <p>
     * Overridden in McV so that the display control is wrapped in a
     * McIDASVComponentHolder rather than a IdvComponentHolder.
     * </p>
     * 
     * @param dc The display control to import.
     */
    
    @Override public void importDisplayControl(final DisplayControlImpl dc) {
        if (dc.getComponentHolder() != null) {
            dc.getComponentHolder().removeDisplayControl(dc);
        }
        idv.getIdvUIManager().getViewPanel().removeDisplayControl(dc);
        dc.guiImported();
        addComponent(new McvComponentHolder(idv, dc));
    }

    /**
     * Basically just creates a McVCompHolder for holding a dynamic skin and
     * sets the name of the component holder.
     * 
     * @param root The XML skin that we'll use.
     */
    
    public void makeDynamicSkin(final Element root) {
        IdvComponentHolder comp =
            new McvComponentHolder(idv, XmlUtil.toString(root));

        comp.setType(McvComponentHolder.TYPE_DYNAMIC_SKIN);
        comp.setName("Dynamic Skin Test");
        addComponent(comp);
        comp.doMakeContents();
    }

    /**
     * Doesn't do anything for the time being...
     * 
     * @param doc
     * 
     * @return XML representation of the contents of this component group.
     */
    
    @Override public Element createXmlNode(final Document doc) {
        // System.err.println("caught createXmlNode");
        Element e = super.createXmlNode(doc);
        // System.err.println(XmlUtil.toString(e));
        // System.err.println("exit createXmlNode");
        return e;
    }

    /**
     * Handles creation of the component represented by the XML skin at the
     * given index.
     * 
     * <p>
     * Overridden so that McV can wrap the component in a
     * McIDASVComponentHolder.
     * </p>
     * 
     * @param index The index of the skin within the skin resource.
     */
    
    @Override public void makeSkin(final int index) {
//        final XmlResourceCollection skins = idv.getResourceManager().getXmlResources(
//            IdvResourceManager.RSC_SKIN);
//
////        String id = skins.getProperty("skinid", index);
////        if (id == null)
////            id = skins.get(index).toString();
//
////        SwingUtilities.invokeLater(new Runnable() {
////            public void run() {
//                String id = skins.getProperty("skinid", index);
//                if (id == null)
//                    id = skins.get(index).toString();
//                IdvComponentHolder comp = new McvComponentHolder(idv, id);
//                comp.setType(IdvComponentHolder.TYPE_SKIN);
//                comp.setName("untitled");
//
//                addComponent(comp);
////            }
////        });
        makeSkinAtIndex(index);
    }
    
    public IdvComponentHolder makeSkinAtIndex(final int index) {
        final XmlResourceCollection skins = idv.getResourceManager().getXmlResources(
                        IdvResourceManager.RSC_SKIN);
        String id = skins.getProperty("skinid", index);
        if (id == null) {
            id = skins.get(index).toString();
        }
        IdvComponentHolder comp = new McvComponentHolder(idv, id);
        comp.setType(IdvComponentHolder.TYPE_SKIN);
        comp.setName("untitled");

        addComponent(comp);
        return comp;
    }

    /**
     * Create a new component whose type will be determined by the contents of
     * {@code what}.
     * 
     * <p>
     * Overridden so that McV can wrap up the components in
     * McVComponentHolders, which allow McV to map ViewManagers to
     * ComponentHolders.
     * </p>
     * 
     * @param what String that determines what sort of component we create.
     */
    
    @Override public void makeNew(final String what) {
        try {
            ViewManager vm = null;
            ComponentHolder comp = null;
            String property = "showControlLegend=false";
            ViewDescriptor desc = new ViewDescriptor();

            // we're only really interested in map, globe, or transect views.
            if (what.equals(IdvUIManager.COMP_MAPVIEW)) {
                vm = new MapViewManager(idv, desc, property);
            } else if (what.equals(IdvUIManager.COMP_TRANSECTVIEW)) {
                vm = new TransectViewManager(idv, desc, property);
            } else if (what.equals(IdvUIManager.COMP_GLOBEVIEW)) {
                vm = new MapViewManager(idv, desc, property);
                ((MapViewManager)vm).setUseGlobeDisplay(true);
            } else {
                // hand off uninteresting things to the IDV
                super.makeNew(what);
                return;
            }

            // make sure we get the component into a mcv component holder,
            // otherwise we won't be able to easily map ViewManagers to
            // ComponentHolders for the hierarchical names in the ViewPanel.
            idv.getVMManager().addViewManager(vm);
            comp = new McvComponentHolder(idv, vm);

            if (comp != null) {
                addComponent(comp);
//                GuiUtils.showComponentInTabs(comp.getContents());
            }

        } catch (Exception exc) {
            LogUtil.logException("Error making new " + what, exc);
        }
    }

    /**
     * Forces this group to layout its components. Extended because the IDV was
     * doing extra work that McIDAS-V doesn't need, such as dealing with
     * layouts other than LAYOUT_TABS and needlessly reinitializing the group's
     * container.
     * 
     * @see ucar.unidata.ui.ComponentGroup#redoLayout()
     */
    
    @SuppressWarnings("unchecked")
    @Override public void redoLayout() {
        final List<ComponentHolder> currentHolders = getDisplayComponents();
        if (!tabRenamed && knownHolders.equals(currentHolders)) {
            return;
        }

        if (tabbedPane == null) {
            return;
        }

        Runnable updateGui = () -> {
            int selectedIndex = tabbedPane.getSelectedIndex();

            tabbedPane.setVisible(false);
            tabbedPane.removeAll();

            knownHolders = new ArrayList<>(currentHolders);
            for (ComponentHolder holder : knownHolders) {
                tabbedPane.addTab(holder.getName(), holder.getContents());
            }

            if (tabRenamed) {
                tabbedPane.setSelectedIndex(selectedIndex);
            }

            tabbedPane.setVisible(true);
            tabRenamed = false;
        };
        
        if (SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(updateGui);
        } else {
            try {
                SwingUtilities.invokeAndWait(updateGui);
            } catch (InvocationTargetException | InterruptedException e) {
                logger.error("Problem updating GUI", e);
            }
        }
    }

    // TODO(jon): remove this method if Unidata implements your fix.
    @Override public void getViewManagers(@SuppressWarnings("rawtypes") final List viewManagers) {
        if ((viewManagers == null) || (getDisplayComponents() == null)) {
//            logger.debug("McvComponentGroup.getViewManagers(): bailing out early!");
            return;
        }

        super.getViewManagers(viewManagers);
    }

    /**
     * Adds a component holder to this group. Extended so that the added holder
     * becomes the active tab, and the component is explicitly set to visible
     * in an effort to fix that heavyweight/lightweight component problem.
     * 
     * @param holder
     * @param index
     * 
     * @see ucar.unidata.ui.ComponentGroup#addComponent(ComponentHolder, int)
     */
    
    @Override public void addComponent(final ComponentHolder holder,
        final int index) 
    {
        if (shouldGenerateName(holder, index)) {
            holder.setName("untitled");
        }

        if (holder.getName().trim().isEmpty()) {
            holder.setName("untitled");
        }

        super.addComponent(holder, index);
        setActiveComponentHolder(holder);
        holder.getContents().setVisible(true);

        if (window != null) {
            window.setTitle(makeWindowTitle(holder.getName()));
        }
    }

    /*
     * (non-Javadoc)
     * TBD - not sure how used yet.
     * @param h
     * @param i
     * @return boolean
     */
    
    private boolean shouldGenerateName(final ComponentHolder h, final int i) {
        if ((h.getName() != null) && !h.getName().startsWith("untitled")) {
            return false;
        }

        boolean invalidIndex = i >= 0;
        boolean withoutName = ((h.getName() == null) || (h.getName().length() == 0));
        boolean loadingBundle = ((PersistenceManager)getIdv().getPersistenceManager()).isBundleLoading();

        return invalidIndex || withoutName || !loadingBundle;
    }

    /**
     * Used to set the tab associated with {@code holder} as the active tab 
     * in our {@link javax.swing.JTabbedPane JTabbedPane}.
     * 
     * @param holder The active component holder.
     */
    
    public void setActiveComponentHolder(final ComponentHolder holder) {
        if (getDisplayComponentCount() > 1) {
            final int newIdx = getDisplayComponents().indexOf(holder);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    setActiveIndex(newIdx);
                }
            });
            
        }

        // TODO: this doesn't work quite right...
        if (window == null) {
            window = IdvWindow.getActiveWindow();
        }
        if (window != null) {
//            SwingUtilities.invokeLater(new Runnable() {
//                public void run() {
                    window.toFront();
//                  window.setTitle(holder.getName());
                    window.setTitle(makeWindowTitle(holder.getName()));
//                }
//            });
        }
    }

    /**
     * Get the index of the active tab in a group.
     * 
     * @return The index of the active component holder within this group.
     */
    
    public int getActiveIndex() {
        if (tabbedPane == null) {
            return -1;
        } else {
            return tabbedPane.getSelectedIndex();
        }
    }

    /**
     * Make the component holder at {@code index} active.
     * 
     * @param index The index of the desired component holder.
     * 
     * @return True if the active component holder was set, false otherwise.
     */
    
    public boolean setActiveIndex(final int index) {
        int size = getDisplayComponentCount();
        if ((index < 0) || (index >= size)) {
            return false;
        }

//        SwingUtilities.invokeLater(new Runnable() {
//            public void run() {
                tabbedPane.setSelectedIndex(index);
                if (window != null) {
                    ComponentHolder h = (ComponentHolder)getDisplayComponents().get(index);
                    if (h != null) {
                        window.setTitle(makeWindowTitle(h.getName()));
                    }
                }
//            }
//        });
        return true;
    }

    /**
     * Returns the index of {@code holder} within this component group.
     * 
     * @return Either the index of {@code holder}, or {@code -1} 
     * if {@link #getDisplayComponents()} returns a {@code null} {@link List}.
     * 
     * @see List#indexOf(Object)
     */
    
    @Override public int indexOf(final ComponentHolder holder) {
        @SuppressWarnings("rawtypes")
        List dispComps = getDisplayComponents();
        if (dispComps == null) {
            return -1;
        } else {
            return getDisplayComponents().indexOf(holder);
        }
    }

    /**
     * Returns the {@link ComponentHolder} at the given position within this
     * component group. 
     * 
     * @param index Index of the {@code ComponentHolder} to return.
     * 
     * @return {@code ComponentHolder} at {@code index}.
     * 
     * @see List#get(int)
     */
    
    protected ComponentHolder getHolderAt(final int index) {
        @SuppressWarnings("unchecked")
        List<ComponentHolder> dispComps = getDisplayComponents();
        return dispComps.get(index);
    }

    /**
     * @return Component holder that corresponds to the selected tab.
     */
    
    public ComponentHolder getActiveComponentHolder() {
        int idx = 0;

        if (getDisplayComponentCount() > 1) {
//            idx = tabbedPane.getSelectedIndex();
            idx = getActiveIndex();
        }

//        return (ComponentHolder)getDisplayComponents().get(idx);
        return getHolderAt(idx);
    }

    /**
     * Overridden so that McV can also update its copy of the IDV reference.
     */
    
    @Override public void setIdv(final IntegratedDataViewer newIdv) {
        super.setIdv(newIdv);
        idv = newIdv;
    }

    /**
     * Create a window title suitable for an application window.
     * 
     * @param title Window title
     * 
     * @return Application title plus the window title.
     */
    
    private String makeWindowTitle(final String title) {
        String defaultApplicationName = "McIDAS-V";
        if (idv != null) {
            defaultApplicationName = idv.getStateManager().getTitle();
        }
        return UIManager.makeTitle(defaultApplicationName, title);
    }

    /**
     * Returns the number of display components {@literal "in"} this group.
     * 
     * @return Either the {@code size()} of the {@link List} returned by 
     * {@link #getDisplayComponents()} or {@code -1} if 
     * {@code getDisplayComponents()} returns a {@code null} {@code List}.
     */
    
    protected int getDisplayComponentCount() {
        @SuppressWarnings("rawtypes")
        List dispComps = getDisplayComponents();
        if (dispComps == null) {
            return -1;
        } else {
            return dispComps.size();
        }
    }
    
    /**
     * Create the {@code JPopupMenu} that will be displayed for a tab.
     * 
     * @return Menu initialized with tab options
     */
    
    protected JPopupMenu doMakeTabMenu() {
        ActionListener menuListener = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                final String cmd = evt.getActionCommand();
                if (CMD_DISPLAY_EJECT.equals(cmd)) {
                    ejectDisplay(tabbedPane.getSelectedIndex());
                } else if (CMD_DISPLAY_RENAME.equals(cmd)) {
                    renameDisplay(tabbedPane.getSelectedIndex());
                } else if (CMD_DISPLAY_DESTROY.equals(cmd)) {
                    destroyDisplay(tabbedPane.getSelectedIndex());
                }
            }
        };

        final JPopupMenu popup = new JPopupMenu();
        JMenuItem item;

        // URL img = getClass().getResource(ICO_UNDOCK);
        // item = new JMenuItem("Undock", new ImageIcon(img));
        // item.setActionCommand(CMD_DISPLAY_EJECT);
        // item.addActionListener(menuListener);
        // popup.add(item);

        URL img = getClass().getResource(ICO_RENAME);
        item = new JMenuItem("Rename", new ImageIcon(img));
        item.setActionCommand(CMD_DISPLAY_RENAME);
        item.addActionListener(menuListener);
        popup.add(item);

        // popup.addSeparator();

        img = getClass().getResource(ICO_CLOSE);
        item = new JMenuItem("Close", new ImageIcon(img));
        item.setActionCommand(CMD_DISPLAY_DESTROY);
        item.addActionListener(menuListener);
        popup.add(item);

        popup.setBorder(new BevelBorder(BevelBorder.RAISED));

        Msg.translateTree(popup);
        return popup;
    }

    /**
     * Remove the component holder at index {@code idx}. This method does
     * not destroy the component holder.
     * 
     * @param idx Index of the ejected component holder.
     * 
     * @return Component holder that was ejected.
     */
    
    private ComponentHolder ejectDisplay(final int idx) {
        return null;
    }

    /**
     * Prompt the user to change the name of the component holder at index
     * {@code idx}. Nothing happens if the user doesn't enter anything.
     * 
     * @param idx Index of the component holder.
     */
    
    protected void renameDisplay(final int idx) {
        
        // TJJ Aug 2017 - Making JOptionPane resizable here for long names
        
        JLabel tabNameLabel = new JLabel("Enter new name:");
        JTextField jtf = new JTextField();
        // Initialize dialog with current tab name
        jtf.setText(getHolderAt(idx).getName());
        Object[] array = { tabNameLabel, jtf };
        JOptionPane pane = new JOptionPane(array, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        JDialog dialog = pane.createDialog(null, "Rename Tab");
        dialog.setResizable(true);
        dialog.setVisible(true);
        String title = jtf.getText();

        if (title == null) {
            return;
        }
        
        // Check return value of dialog Ok/Cancel
        Object selectedValue = pane.getValue();
        if (selectedValue == null) {
            // Dialog was closed (x'd out)
            return;
        }
        // Bizarre way of checking for Cancel, but it's in the doc and works
        if (selectedValue instanceof Integer) {
            // User clicked the Cancel button
            if (((Integer) selectedValue).intValue() == JOptionPane.CANCEL_OPTION) {
                return;
            }
        }

        // Go ahead and update with new name user provided
        getHolderAt(idx).setName(title);
        tabRenamed = true;
        if (window != null) {
            window.setTitle(makeWindowTitle(title));
        }
        redoLayout();
    }

    /**
     * Prompts the user to confirm removal of the component holder at index
     * {@code idx}. Nothing happens if the user declines.
     * 
     * @param idx Index of the component holder.
     * 
     * @return Either {@code true} if the user elected to remove, 
     * {@code false} otherwise.
     */
    
    protected boolean destroyDisplay(final int idx) {
//        final List<IdvComponentHolder> comps = getDisplayComponents();
//        IdvComponentHolder comp = comps.get(idx);
        return ((IdvComponentHolder)getHolderAt(idx)).removeDisplayComponent();
//        return comp.removeDisplayComponent();
    }

    /**
     * Remove the component at {@code index} without forcing the IDV-land
     * component group to redraw.
     * 
     * @param index The index of the component to be removed.
     * 
     * @return The removed component.
     */
    
    @SuppressWarnings("unchecked")
    public ComponentHolder quietRemoveComponentAt(final int index) {
        List<ComponentHolder> comps = getDisplayComponents();
        if (comps == null || comps.size() == 0) {
            return null;
        }
        ComponentHolder removed = comps.remove(index);
        removed.setParent(null);
        return removed;
    }

    /**
     * Adds a component to the end of the list of display components without
     * forcing the IDV-land code to redraw.
     * 
     * @param component The component to add.
     * 
     * @return The index of the newly added component, or {@code -1} if 
     * {@link #getDisplayComponents()} returned a null {@code List}.
     */
    
    @SuppressWarnings("unchecked")
    public int quietAddComponent(final ComponentHolder component) {
        List<ComponentHolder> comps = getDisplayComponents();
        if (comps == null) {
            return -1;
        }
        if (comps.contains(component)) {
            comps.remove(component);
        }
        comps.add(component);
        component.setParent(this);
        return comps.indexOf(component);
    }

    /**
     * Handle pop-up events for tabs.
     */
    
    @SuppressWarnings("unused")
    private class TabPopupListener extends MouseAdapter {

        @Override public void mouseClicked(final MouseEvent evt) {
            checkPopup(evt);
        }

        @Override public void mousePressed(final MouseEvent evt) {
            checkPopup(evt);
        }

        @Override public void mouseReleased(final MouseEvent evt) {
            checkPopup(evt);
        }

        /**
         * <p>
         * Determines whether or not the tab popup menu should be shown, and
         * if so, which parts of it should be enabled or disabled.
         * </p>
         * 
         * @param evt Allows us to determine the type of event.
         */
        
        private void checkPopup(final MouseEvent evt) {
            if (evt.isPopupTrigger()) {
                // can't close or eject last tab
                // TODO: re-evaluate this
                Component[] comps = popup.getComponents();
                for (Component comp : comps) {
                    if (comp instanceof JMenuItem) {
                        String cmd = ((JMenuItem)comp).getActionCommand();
                        if ((CMD_DISPLAY_DESTROY.equals(cmd) || CMD_DISPLAY_EJECT.equals(cmd))
                            && tabbedPane.getTabCount() == 1) {
                            comp.setEnabled(false);
                        } else {
                            comp.setEnabled(true);
                        }
                    }
                }
                popup.show(tabbedPane, evt.getX(), evt.getY());
            }
        }
    }
}
