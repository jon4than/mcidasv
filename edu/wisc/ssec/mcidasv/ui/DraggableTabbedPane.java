/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2009
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * http://www.ssec.wisc.edu/mcidas
 * 
 * All Rights Reserved
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

package edu.wisc.ssec.mcidasv.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.plaf.metal.MetalTabbedPaneUI;

import org.w3c.dom.Element;

import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.ui.IdvWindow;
import ucar.unidata.ui.ComponentGroup;
import ucar.unidata.ui.ComponentHolder;
import ucar.unidata.xml.XmlUtil;

import edu.wisc.ssec.mcidasv.Constants;

/**
 * This is a rather simplistic drag and drop enabled JTabbedPane. It allows
 * users to use drag and drop to move tabs between windows and reorder tabs.
 */
public class DraggableTabbedPane extends JTabbedPane 
implements DragGestureListener, DragSourceListener, DropTargetListener {

    private static final long serialVersionUID = -5710302260509445686L;

    /** Local shorthand for the actions we're accepting. */
    private static final int VALID_ACTION = DnDConstants.ACTION_COPY_OR_MOVE;

    /** Path to the icon we'll use as an index indicator. */
    private static final String IDX_ICON = 
        "/edu/wisc/ssec/mcidasv/resources/icons/tabmenu/go-down.png";

    /** 
     * Used to signal across all DraggableTabbedPanes that the component 
     * currently being dragged originated in another window. This'll let McV
     * determine if it has to do a quiet ComponentHolder transfer.
     */
    protected static boolean outsideDrag = false;

    /** The actual image that we'll use to display the index indications. */
    private final Image INDICATOR = 
        (new ImageIcon(getClass().getResource(IDX_ICON))).getImage();

    /** The tab index where the drag started. */
    private int sourceIndex = -1;

    /** The tab index that the user is currently over. */
    private int overIndex = -1;

    /** Used for starting the dragging process. */
    private DragSource dragSource;

    /** Used for signaling that we'll accept drops (registers listeners). */
    private DropTarget dropTarget;

    /** The component group holding our components. */
    private McvComponentGroup group;

    /** The IDV window that contains this tabbed pane. */
    private IdvWindow window;

    /** Keep around this reference so that we can access the UI Manager. */
    private IntegratedDataViewer idv;

    /**
     * Mostly just registers that this component should listen for drag and
     * drop operations.
     * 
     * @param win The IDV window containing this tabbed pane.
     * @param idv The main IDV instance.
     * @param group The <tt>ComponentGroup</tt> that holds this component's tabs.
     */
    public DraggableTabbedPane(IdvWindow win, IntegratedDataViewer idv, McvComponentGroup group) {
        dropTarget = new DropTarget(this, this);
        dragSource = new DragSource();
        dragSource.createDefaultDragGestureRecognizer(this, VALID_ACTION, this);

        this.group = group;
        this.idv = idv;
        window = win;

//        addMouseListener(this);
//        addMouseMotionListener(this);

//        if (getUI() instanceof MetalTabbedPaneUI)
//          setUI(new CloseableMetalTabbedPaneUI(SwingUtilities.LEFT));
//        else
//          setUI(new CloseableTabbedPaneUI(SwingUtilities.LEFT));
    }

    /**
     * Triggered when the user does a (platform-dependent) drag initiating 
     * gesture. Used to populate the things that the user is attempting to 
     * drag. 
     */
    public void dragGestureRecognized(DragGestureEvent e) {
        sourceIndex = getSelectedIndex();

        // transferable allows us to store the current DraggableTabbedPane and
        // the source index of the drag inside the various drag and drop event
        // listeners.
        Transferable transferable = new TransferableIndex(this, sourceIndex);

        Cursor cursor = DragSource.DefaultMoveDrop;
        if (e.getDragAction() != DnDConstants.ACTION_MOVE)
            cursor = DragSource.DefaultCopyDrop;

        dragSource.startDrag(e, cursor, transferable, this);
    }

    /** 
     * Triggered when the user drags into <tt>dropTarget</tt>.
     */
    public void dragEnter(DropTargetDragEvent e) {
        DataFlavor[] flave = e.getCurrentDataFlavors();
        if ((flave.length == 0) || !(flave[0] instanceof DraggableTabFlavor))
            return;

        //System.out.print("entered window outsideDrag=" + outsideDrag + " sourceIndex=" + sourceIndex);

        // if the DraggableTabbedPane associated with this drag isn't the 
        // "current" DraggableTabbedPane we're dealing with a drag from another
        // window and we need to make this DraggableTabbedPane aware of that.
        if (((DraggableTabFlavor)flave[0]).getDragTab() != this) {
            //System.out.println(" coming from outside!");
            outsideDrag = true;
        } else {
            //System.out.println(" re-entered parent window");
            outsideDrag = false;
        }
    }

    /**
     * Triggered when the user drags out of <tt>dropTarget</tt>.
     */
    public void dragExit(DropTargetEvent e) {
        //		System.out.println("drag left a window outsideDrag=" + outsideDrag + " sourceIndex=" + sourceIndex);
        overIndex = -1;

        //outsideDrag = true;
        repaint();
    }

    /**
     * Triggered continually while the user is dragging over 
     * <tt>dropTarget</tt>. McIDAS-V uses this to draw the index indicator.
     * 
     * @param e Information about the current state of the drag.
     */
    public void dragOver(DropTargetDragEvent e) {
        //		System.out.println("dragOver outsideDrag=" + outsideDrag + " sourceIndex=" + sourceIndex);
        if ((!outsideDrag) && (sourceIndex == -1))
            return;

        Point dropPoint = e.getLocation();
        overIndex = indexAtLocation(dropPoint.x, dropPoint.y);

        repaint();
    }

    /**
     * Triggered when a drop has happened over <tt>dropTarget</tt>.
     * 
     * @param e State that we'll need in order to handle the drop.
     */
    public void drop(DropTargetDropEvent e) {
        // if the dragged ComponentHolder was dragged from another window we
        // must do a behind-the-scenes transfer from its old ComponentGroup to 
        // the end of the new ComponentGroup.
        if (outsideDrag) {
            DataFlavor[] flave = e.getCurrentDataFlavors();
            DraggableTabbedPane other = ((DraggableTabFlavor)flave[0]).getDragTab();

            ComponentHolder target = other.removeDragged();
            sourceIndex = group.quietAddComponent(target);
            outsideDrag = false;
        }

        // check to see if we've actually dropped something McV understands.
        if (sourceIndex >= 0) {
            e.acceptDrop(VALID_ACTION);
            Point dropPoint = e.getLocation();
            int dropIndex = indexAtLocation(dropPoint.x, dropPoint.y);

            // make sure the user chose to drop over a valid area/thing first
            // then do the actual drop.
            if ((dropIndex != -1) && (getComponentAt(dropIndex) != null))
                doDrop(sourceIndex, dropIndex);

            // clean up anything associated with the current drag and drop
            e.getDropTargetContext().dropComplete(true);
            sourceIndex = -1;
            overIndex = -1;

            repaint();
        }
    }

    /**
     * &quot;Quietly&quot; removes the dragged component from its group. If the
     * last component in a group has been dragged out of the group, the 
     * associated window will be killed.
     * 
     * @return The removed component.
     */
    private ComponentHolder removeDragged() {
        ComponentHolder removed = group.quietRemoveComponentAt(sourceIndex);

        // no point in keeping an empty window around.
        List<ComponentHolder> comps = group.getDisplayComponents();
        //		if ((window != null) && (comps == null || comps.isEmpty()))
        if (comps == null || comps.isEmpty())
            window.dispose();

        return removed;
    }

    /**
     * Moves a component to its new index within the component group.
     * 
     * @param srcIdx The old index of the component.
     * @param dstIdx The new index of the component.
     */
    public void doDrop(int srcIdx, int dstIdx) {
        List<ComponentHolder> comps = group.getDisplayComponents();
        ComponentHolder src = comps.get(srcIdx);

        group.removeComponent(src);
        group.addComponent(src, dstIdx);
    }

    /**
     * Overridden so that McV can draw an indicator of a dragged tab's possible
     * new position.
     */
    @Override
    public void paint(Graphics g) {
        super.paint(g);

        if (overIndex == -1)
            return;

        Rectangle bounds = getBoundsAt(overIndex);

        if (bounds != null)
            g.drawImage(INDICATOR, bounds.x-7, bounds.y, null);
    }

    /**
     * Used to simply provide a reference to the originating 
     * DraggableTabbedPane while we're dragging and dropping.
     */
    private static class TransferableIndex implements Transferable {
        private DraggableTabbedPane tabbedPane;

        private int index;

        public TransferableIndex(DraggableTabbedPane dt, int i) {
            tabbedPane = dt;
            index = i;
        }

        // whatever is returned here needs to be serializable. so we can't just
        // return the tabbedPane. :(
        public Object getTransferData(DataFlavor flavor) {
            return index;
        }

        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[] { new DraggableTabFlavor(tabbedPane) };
        }

        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return true;
        }
    }

    /**
     * To be perfectly honest I'm still a bit fuzzy about DataFlavors. As far 
     * as I can tell they're used like so: if a user dragged an image file on
     * to a toolbar, the toolbar might be smart enough to add the image. If the
     * user dragged the same image file into a text document, the text editor
     * might be smart enough to insert the path to the image or something.
     * 
     * I'm thinking that would require two data flavors: some sort of toolbar
     * flavor and then some sort of text flavor?
     */
    private static class DraggableTabFlavor extends DataFlavor {
        private DraggableTabbedPane tabbedPane;

        public DraggableTabFlavor(DraggableTabbedPane dt) {
            super(DraggableTabbedPane.class, "DraggableTabbedPane");
            tabbedPane = dt;
        }

        public DraggableTabbedPane getDragTab() {
            return tabbedPane;
        }
    }

    /**
     * Handle the user dropping a tab outside of a McV window. This will create
     * a new window and add the dragged tab to the ComponentGroup within the
     * newly created window. The new window is the same size as the origin 
     * window, with the top centered over the location where the user released
     * the mouse.
     * 
     * @param dragged The ComponentHolder that's being dragged around.
     * @param drop The x- and y-coordinates where the user dropped the tab.
     */
    private void newWindowDrag(ComponentHolder dragged, Point drop) {
        //		if ((dragged == null) || (window == null))
        if (dragged == null)
            return;

        UIManager ui = (UIManager)idv.getIdvUIManager();

        try {
            Element skinRoot = XmlUtil.getRoot(Constants.BLANK_COMP_GROUP, getClass());

            // create the new window with visibility off, so we can position 
            // the window in a sensible way before the user has to see it.
            IdvWindow w = ui.createNewWindow(null, false, "McIDAS-V", 
                    Constants.BLANK_COMP_GROUP, 
                    skinRoot, false, null);

            // make the new window the same size as the old and center the 
            // *top* of the window over the drop point.
            int height = window.getBounds().height;
            int width = window.getBounds().width;
            int startX = drop.x - (width / 2);

            w.setBounds(new Rectangle(startX, drop.y, width, height));

            // be sure to add the dragged component holder to the new window.
            ComponentGroup newGroup = 
                (ComponentGroup)w.getComponentGroups().get(0);

            newGroup.addComponent(dragged);

            // let there be a window
            w.setVisible(true);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles what happens at the very end of a drag and drop. Since I could
     * not find a better method for it, tabs that are dropped outside of a McV
     * window are handled with this method.
     */
    public void dragDropEnd(DragSourceDropEvent e) {
        //System.out.println("other dragDropEnd outsideDrag=" + outsideDrag + " sourceIndex=" + sourceIndex + " success=" + e.getDropSuccess() + " action=" + e.getDropAction());
        if (!e.getDropSuccess() && e.getDropAction() == 0) {
            newWindowDrag(removeDragged(), e.getLocation());
        }
    }

    // required methods that we don't need to implement yet.
    public void dragEnter(DragSourceDragEvent e) {
        //System.out.println("other dragEnter outsideDrag=" + outsideDrag + " sourceIndex=" + sourceIndex);
    }
    public void dragExit(DragSourceEvent e) {
        //System.out.println("other dragExit outsideDrag=" + outsideDrag + " sourceIndex=" + sourceIndex);
    }
    public void dragOver(DragSourceDragEvent e) {
        //System.out.println("other dragOver outsideDrag=" + outsideDrag + " sourceIndex=" + sourceIndex);
    }
    public void dropActionChanged(DragSourceDragEvent e) {
        //System.out.println("other dropActionChanged outsideDrag=" + outsideDrag + " sourceIndex=" + sourceIndex);
    }
    public void dropActionChanged(DropTargetDragEvent e) {
        //System.out.println("other dropActionChanged outsideDrag=" + outsideDrag + " sourceIndex=" + sourceIndex);
    }

    private JViewport headerViewport = null;
    private Icon closeIconNormal = null;
    private Icon closeIconHover = null;
    private Icon closeIconPressed = null;

    public void mouseClicked(MouseEvent e) {
        processMouseEvents(e);
    }

    public void mouseExited(MouseEvent e) {
        for (int i = 0; i < getTabCount(); i++) {
            CloseTabIcon icon = (CloseTabIcon) getIconAt(i);
            if (icon != null)
                icon.mouseover = false;
        }
        repaint();
    }

    public void mousePressed(MouseEvent e) {
        processMouseEvents(e);
    }

    private void processMouseEvents(MouseEvent e) {
        int tabNumber = getUI().tabForCoordinate(this, e.getX(), e.getY());
        if (tabNumber < 0) return;
        CloseTabIcon icon = (CloseTabIcon) getIconAt(tabNumber);
        if (icon != null) {
            Rectangle rect= icon.getBounds();
            Point pos = headerViewport == null ? new Point() : headerViewport.getViewPosition();
            Rectangle drawRect = new Rectangle(
                    rect.x - pos.x, rect.y - pos.y, rect.width, rect.height);

            if (e.getID() == e.MOUSE_PRESSED) {
                icon.mousepressed = e.getModifiers() == e.BUTTON1_MASK;
                repaint(drawRect);
            } else if (e.getID() == e.MOUSE_MOVED || e.getID() == e.MOUSE_DRAGGED ||
                    e.getID() == e.MOUSE_CLICKED) {
                pos.x += e.getX();
                pos.y += e.getY();
                if (rect.contains(pos)) {
                    if (e.getID() == e.MOUSE_CLICKED) {
//                        int selIndex = getSelectedIndex();
//                        if (fireCloseTab(selIndex)) {
//                            if (selIndex > 0) {
//                                // to prevent uncatchable null-pointers
//                                Rectangle rec = getUI().getTabBounds(this, selIndex - 1);
//
//                                MouseEvent event = new MouseEvent((Component) e.getSource(),
//                                        e.getID() + 1,
//                                        System.currentTimeMillis(),
//                                        e.getModifiers(),
//                                        rec.x,
//                                        rec.y,
//                                        e.getClickCount(),
//                                        e.isPopupTrigger(),
//                                        e.getButton());
//                                dispatchEvent(event);
//                            }
//                            //the tab is being closed
//                            //removeTabAt(tabNumber);
//                            remove(selIndex);
//                        } else {
                            icon.mouseover = false;
                            icon.mousepressed = false;
                            repaint(drawRect);
//                        }
                    } else {
                        icon.mouseover = true;
                        icon.mousepressed = e.getModifiers() == e.BUTTON1_MASK;
                    }
                } else {
                    icon.mouseover = false;
                }
                repaint(drawRect);
            }
        }
    }

    public void addTab(String title, Component component) {
        addTab(title, component, null);
    }

    public void addTab(String title, Component component, Icon extraIcon) {
        boolean doPaintCloseIcon = true;
        try {
            Object prop = null;
            if ((prop = ((JComponent)component).getClientProperty("isClosable")) != null)
                doPaintCloseIcon = (Boolean)prop;

        } catch (Exception ignored) {
            
        }

        super.addTab(title,
            doPaintCloseIcon ? new CloseTabIcon(extraIcon) : null, component);

        if (headerViewport == null)
            for (Component c : getComponents())
                if ("DraggableTabbedPane.scrollableViewport".equals(c.getName()))
                    headerViewport = (JViewport)c;
    }

    private class CloseTabIcon implements Icon {
        private int posX;
        private int posY;
        private int width = 16;
        private int height = 16;
        private Icon fileIcon;
        private boolean mouseover = false;
        private boolean mousepressed = false;

        public CloseTabIcon(Icon fileIcon) {
            this.fileIcon = fileIcon;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            boolean doPaintCloseIcon = true;
            try {
                // JComponent.putClientProperty("isClosable", new Boolean(false));
                JTabbedPane tabbedpane = (JTabbedPane)c;
                int tabNumber = 
                    tabbedpane.getUI().tabForCoordinate(tabbedpane, x, y);
                JComponent curPanel = (JComponent) tabbedpane.getComponentAt(tabNumber);
                Object prop = null;
                if ((prop = curPanel.getClientProperty("isClosable")) != null) {
                    doPaintCloseIcon = (Boolean) prop;
                }
            } catch (Exception e) {
                // nothing for now.
            }

            if (doPaintCloseIcon) {
                posX = x;
                posY = y;
                int y_p = y + 1;

                if (closeIconNormal != null && !mouseover) {
                    closeIconNormal.paintIcon(c, g, x, y_p);
                } else if (closeIconHover != null && mouseover && !mousepressed) {
                    closeIconHover.paintIcon(c, g, x, y_p);
                } else if (closeIconPressed != null && mousepressed) {
                    closeIconPressed.paintIcon(c, g, x, y_p);
                } else {
                    y_p++;

                    Color col = g.getColor();

                    if (mousepressed && mouseover) {
                        g.setColor(Color.WHITE);
                        g.fillRect(x+1, y_p, 12, 13);
                    }

                    g.setColor(Color.black);
                    g.drawLine(x+1, y_p, x+12, y_p);
                    g.drawLine(x+1, y_p+13, x+12, y_p+13);
                    g.drawLine(x, y_p+1, x, y_p+12);
                    g.drawLine(x+13, y_p+1, x+13, y_p+12);
                    g.drawLine(x+3, y_p+3, x+10, y_p+10);

                    if (mouseover)
                        g.setColor(Color.GRAY);

                    g.drawLine(x+3, y_p+4, x+9, y_p+10);
                    g.drawLine(x+4, y_p+3, x+10, y_p+9);
                    g.drawLine(x+10, y_p+3, x+3, y_p+10);
                    g.drawLine(x+10, y_p+4, x+4, y_p+10);
                    g.drawLine(x+9, y_p+3, x+3, y_p+9);
                    g.setColor(col);
                    if (fileIcon != null) {
                        fileIcon.paintIcon(c, g, x+width, y_p);
                    }
                }
            }
        }

        public int getIconWidth() {
            return width + (fileIcon != null ? fileIcon.getIconWidth() : 0);
        }

        public int getIconHeight() {
            return height;
        }

        public Rectangle getBounds() {
            return new Rectangle(posX, posY, width, height);
        }
    }

    class CloseableTabbedPaneUI extends BasicTabbedPaneUI {
        private int horizontalTextPosition = SwingUtilities.LEFT;

        public CloseableTabbedPaneUI() {
        }

        public CloseableTabbedPaneUI(int horizontalTextPosition) {
            this.horizontalTextPosition = horizontalTextPosition;
        }

        protected void layoutLabel(int tabPlacement, FontMetrics metrics,
                int tabIndex, String title, Icon icon,
                Rectangle tabRect, Rectangle iconRect,
                Rectangle textRect, boolean isSelected) {

            textRect.x = textRect.y = iconRect.x = iconRect.y = 0;

            javax.swing.text.View v = getTextViewForTab(tabIndex);
            if (v != null)
                tabPane.putClientProperty("html", v);

            SwingUtilities.layoutCompoundLabel((JComponent) tabPane,
                    metrics, title, icon,
                    SwingUtilities.CENTER,
                    SwingUtilities.CENTER,
                    SwingUtilities.CENTER,
                    //SwingUtilities.TRAILING,
                    horizontalTextPosition,
                    tabRect,
                    iconRect,
                    textRect,
                    textIconGap + 2);

            tabPane.putClientProperty("html", null);

            int xNudge = getTabLabelShiftX(tabPlacement, tabIndex, isSelected);
            int yNudge = getTabLabelShiftY(tabPlacement, tabIndex, isSelected);
            iconRect.x += xNudge;
            iconRect.y += yNudge;
            textRect.x += xNudge;
            textRect.y += yNudge;
        }
    }

    class CloseableMetalTabbedPaneUI extends MetalTabbedPaneUI {

        private int horizontalTextPosition = SwingUtilities.LEFT;

        public CloseableMetalTabbedPaneUI() {
        }

        public CloseableMetalTabbedPaneUI(int horizontalTextPosition) {
            this.horizontalTextPosition = horizontalTextPosition;
        }

        protected void layoutLabel(int tabPlacement, FontMetrics metrics,
                int tabIndex, String title, Icon icon,
                Rectangle tabRect, Rectangle iconRect,
                Rectangle textRect, boolean isSelected) {

            textRect.x = textRect.y = iconRect.x = iconRect.y = 0;

            javax.swing.text.View v = getTextViewForTab(tabIndex);
            if (v != null)
                tabPane.putClientProperty("html", v);

            SwingUtilities.layoutCompoundLabel((JComponent) tabPane,
                    metrics, title, icon,
                    SwingUtilities.CENTER,
                    SwingUtilities.CENTER,
                    SwingUtilities.CENTER,
                    //SwingUtilities.TRAILING,
                    horizontalTextPosition,
                    tabRect,
                    iconRect,
                    textRect,
                    textIconGap + 2);

            tabPane.putClientProperty("html", null);

            int xNudge = getTabLabelShiftX(tabPlacement, tabIndex, isSelected);
            int yNudge = getTabLabelShiftY(tabPlacement, tabIndex, isSelected);
            iconRect.x += xNudge;
            iconRect.y += yNudge;
            textRect.x += xNudge;
            textRect.y += yNudge;
        }
    }
}
