package edu.wisc.ssec.mcidasv.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.BevelBorder;

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
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Msg;
import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;

/**
 * Extends the IDV component groups so that we can intercept clicks for Bruce's
 * tab popup menu and handle drag and drop. It also intercepts ViewManager
 * creation in order to wrap components in McIDASVComponentHolders rather than
 * IdvComponentHolders. Doing this allows us to associate ViewManagers back to
 * their ComponentHolders, and this functionality is taken advantage of to form
 * the hierarchical names seen in the McIDASVViewPanel.
 */
public class McvComponentGroup extends IdvComponentGroup {

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

    /** Keep a reference to avoid extraneous calls to <tt>getIdv().</tt> */
    private IntegratedDataViewer idv;

    /** The popup menu for the McV tabbed display interface. */
    private JPopupMenu popup;

    /** Reference to the window associated with this group. */
    private IdvWindow window;

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
    public McvComponentGroup(IntegratedDataViewer idv, String name) {
        super(idv, name);
        this.idv = idv;
    }

    /**
     * This constructor catches the window that will be contain this group.
     * 
     * @param idv The main IDV instance.
     * @param name Presumably the name of this component group?
     * @param window The window holding this component group.
     */
    public McvComponentGroup(IntegratedDataViewer idv, String name,
        IdvWindow window) 
    {
        this(idv, name);
        this.window = window;
        
        container = new JPanel(new BorderLayout());
        
        popup = doMakeTabMenu();
        
        tabbedPane = new DraggableTabbedPane(window, idv, this);
        tabbedPane.addMouseListener(new TabPopupListener());
        
        container.add(tabbedPane);
        GuiUtils.handleHeavyWeightComponentsInTabs(tabbedPane);
    }

    /**
     * Create and return the GUI contents. Overridden so that McV can implement
     * the right click tab menu and draggable tabs.
     * 
     * @return GUI contents
     */
    @Override public JComponent doMakeContents() {
        redoLayout();
        outerContainer = GuiUtils.center(container);
        outerContainer.validate();
        return outerContainer;
    }

    /**
     * <p>
     * Importing a display control entails adding the control to the component
     * group and informing the UI that the control is no longer in its own
     * window.
     * </p>
     * 
     * <p>
     * Overridden in McV so that the display control is wrapped in a
     * McIDASVComponentHolder rather than a IdvComponentHolder.
     * </p>
     * 
     * @param dc The display control to import.
     */
    @Override public void importDisplayControl(final DisplayControlImpl dc) {
        if (dc.getComponentHolder() != null)
            dc.getComponentHolder().removeDisplayControl(dc);

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
     * <p>
     * Handles creation of the component represented by the XML skin at the
     * given index.
     * </p>
     * 
     * <p>
     * Overridden so that McV can wrap the component in a
     * McIDASVComponentHolder.
     * </p>
     * 
     * @param index The index of the skin within the skin resource.
     */
    @Override public void makeSkin(final int index) {
        XmlResourceCollection skins =
            idv.getResourceManager().getXmlResources(
                IdvResourceManager.RSC_SKIN);
        String id = skins.getProperty("skinid", index);
        if (id == null)
            id = skins.get(index).toString();

        IdvComponentHolder comp = new McvComponentHolder(idv, id);
        comp.setType(IdvComponentHolder.TYPE_SKIN);
        comp.setName(skins.getLabel(index));

        addComponent(comp);

        redoLayout();
    }

    /**
     * <p>
     * Create a new component whose type will be determined by the contents of
     * <tt>what</tt>.
     * </p>
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
                GuiUtils.showComponentInTabs(comp.getContents());
            }

        } catch (Exception exc) {
            LogUtil.logException("Error making new " + what, exc);
        }
    }

    private List<ComponentHolder> compList = new ArrayList<ComponentHolder>();
    @Override public void redoLayout() {
        List<ComponentHolder> comps = getDisplayComponents();
        if (compList.equals(comps)) {
            return;
        }
        
        tabbedPane.setVisible(false);
        tabbedPane.removeAll();
        for (ComponentHolder holder : comps) {
            JComponent comp = holder.getContents();
            tabbedPane.addTab(holder.getName(), holder.getIcon(), comp);
        }
        tabbedPane.setVisible(true);
        compList = new ArrayList<ComponentHolder>(comps);
    }

    @Override public void addComponent(final ComponentHolder comp, final int idx) {
        super.addComponent(comp, idx);
        setActiveComponentHolder(comp);
        comp.getContents().setVisible(true);
    }

    /**
     * Used to set the tab associated with <tt>holder</tt> as the active tab
     * in our JTabbedPane.
     * 
     * @param The active component holder.
     */
    public void setActiveComponentHolder(final ComponentHolder holder) {
        if (getDisplayComponents().size() > 1) {
            int newIdx = getDisplayComponents().indexOf(holder);
            setActiveIndex(newIdx);
        }

        window.toFront();
    }

    /**
     * @return The index of the active component holder within this group.
     */
    public int getActiveIndex() {
        return tabbedPane.getSelectedIndex();
    }

    /**
     * <p>Make the component holder at <code>index</code> active.</p>
     * 
     * @return True if the active component holder was set, false otherwise.
     */
    public boolean setActiveIndex(final int index) {
        int size = getDisplayComponents().size();
        if ((index < 0) || (index >= size))
            return false;

        tabbedPane.setSelectedIndex(index);
        return true;
    }

    /**
     * @return The index of <code>holder</code> within this group.
     */
    public int indexOf(final ComponentHolder holder) {
        return getDisplayComponents().indexOf(holder);
    }

    /**
     * @return The component holder that corresponds to the selected tab.
     */
    public ComponentHolder getActiveComponentHolder() {
        int idx = 0;

        if (getDisplayComponents().size() > 1)
            idx = tabbedPane.getSelectedIndex();

        return (ComponentHolder)getDisplayComponents().get(idx);
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
     * @param title window title
     * 
     * @return Application title plus the window title.
     */
    private String makeWindowTitle(final String title) {
        return UIManager.makeTitle(idv.getStateManager().getTitle(), title);
    }

    /**
     * Create the <tt>JPopupMenu</tt> that will be displayed for a tab.
     * 
     * @return Menu initialized with tab options
     */
    protected JPopupMenu doMakeTabMenu() {
        ActionListener menuListener = new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                final String cmd = evt.getActionCommand();

                if (cmd.equals(CMD_DISPLAY_EJECT))
                    ejectDisplay(tabbedPane.getSelectedIndex());

                else if (cmd.equals(CMD_DISPLAY_RENAME))
                    renameDisplay(tabbedPane.getSelectedIndex());

                else if (cmd.equals(CMD_DISPLAY_DESTROY))
                    destroyDisplay(tabbedPane.getSelectedIndex());
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
     * Remove the component holder at index <tt>idx</tt>. This method does
     * not destroy the component holder.
     * 
     * @param idx The index of the ejected component holder.
     * 
     * @return The component holder that was ejected.
     */
    private ComponentHolder ejectDisplay(final int idx) {
        return null;
    }

    /**
     * Prompt the user to change the name of the component holder at index
     * <tt>idx</tt>. Nothing happens if the user doesn't enter anything.
     * 
     * @param idx Index of the component holder.
     */
    private void renameDisplay(final int idx) {
        final String title =
            JOptionPane.showInputDialog(
                IdvWindow.getActiveWindow().getFrame(), "Enter new name",
                makeWindowTitle("Rename Tab"), JOptionPane.PLAIN_MESSAGE);

        if (title == null)
            return;

        final List<ComponentHolder> comps = getDisplayComponents();
        comps.get(idx).setName(title);
        redoLayout();
    }

    /**
     * Prompts the user to confirm removal of the component holder at index
     * <tt>idx</tt>. Nothing happens if the user declines.
     * 
     * @param idx Index of the component holder.
     */
    private void destroyDisplay(final int idx) {
        final List<IdvComponentHolder> comps = getDisplayComponents();
        IdvComponentHolder comp = comps.get(idx);
        comp.removeDisplayComponent();
    }

    /**
     * Remove the component at <tt>index</tt> without forcing the IDV-land
     * component group to redraw.
     * 
     * @param index The index of the component to be removed.
     * 
     * @return The removed component.
     */
    public ComponentHolder quietRemoveComponentAt(final int index) {
        System.err.println("quietRemove: idx=" + index);
        List<ComponentHolder> comps = getDisplayComponents();
        if (comps == null || comps.size() == 0)
            return null;

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
     * @return The index of the newly added component.
     */
    public int quietAddComponent(final ComponentHolder component) {
        System.err.println("quietAdd: name=" + component.getName());
        List<ComponentHolder> comps = getDisplayComponents();
        if (comps.contains(component))
            comps.remove(component);

        comps.add(component);
        component.setParent(this);
        return comps.indexOf(component);
    }

    /**
     * Handle pop-up events for tabs.
     */
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

        private void checkPopup(final MouseEvent evt) {
            if (evt.isPopupTrigger()) {
                // can't close or eject last tab
                // TODO: re-evaluate this
                Component[] comps = popup.getComponents();
                for (Component comp : comps) {
                    if (comp instanceof JMenuItem) {
                        String cmd = ((JMenuItem)comp).getActionCommand();
                        if ( (CMD_DISPLAY_DESTROY.equals(cmd) || CMD_DISPLAY_EJECT
                                                                                  .equals(cmd))
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
