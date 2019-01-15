/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2019
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

package edu.wisc.ssec.mcidasv.util;

import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.arrList;
import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.newHashSet;

import static javax.swing.GroupLayout.DEFAULT_SIZE;
import static javax.swing.GroupLayout.PREFERRED_SIZE;
import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static javax.swing.LayoutStyle.ComponentPlacement.RELATED;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.HierarchyEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.regex.Pattern;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.text.JTextComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.unidata.idv.MapViewManager;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.ui.IdvComponentGroup;
import ucar.unidata.idv.ui.IdvComponentHolder;
import ucar.unidata.idv.ui.IdvWindow;
import ucar.unidata.idv.ui.WindowInfo;
import ucar.unidata.ui.ComponentHolder;
import ucar.unidata.ui.MultiFrame;
import ucar.unidata.util.GuiUtils;

import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.McIDASV;
import edu.wisc.ssec.mcidasv.ViewManagerManager;
import edu.wisc.ssec.mcidasv.ui.McvComponentGroup;
import edu.wisc.ssec.mcidasv.ui.McvComponentHolder;
import edu.wisc.ssec.mcidasv.ui.UIManager;

/**
 * McIDAS-V's collection of GUI utility methods
 */
public class McVGuiUtils implements Constants {
    
    /** Logging object. */
    // TODO(jon): consider removing when testing is done
    private static final Logger logger =
        LoggerFactory.getLogger(McVGuiUtils.class);

    /** 
     * Estimated number of {@link ucar.unidata.idv.ViewManager ViewManagers}.
     * This value is only used as a last resort ({@link McIDASV#getStaticMcv()} failing). 
     */
    private static final int ESTIMATED_VM_COUNT = 32;

    public enum Width { HALF, SINGLE, ONEHALF, DOUBLE, TRIPLE, QUADRUPLE, DOUBLEDOUBLE }

    public enum Position { LEFT, RIGHT, CENTER }

    public enum Prefer { TOP, BOTTOM, NEITHER }

    public enum TextColor { NORMAL, STATUS }

    private McVGuiUtils() {}

    /**
     * Use this class to create a panel with a background image
     * @author davep
     *
     */
    public static class IconPanel extends JPanel {
        private Image img;

        public IconPanel(String img) {
            this(GuiUtils.getImageIcon(img).getImage());
        }

        public IconPanel(Image img) {
            this.img = img;
            Dimension size = new Dimension(img.getWidth(null), img.getHeight(null));
            setPreferredSize(size);
            setMinimumSize(size);
            setMaximumSize(size);
            setSize(size);
            setLayout(null);
        }

        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(img, 0, 0, null);
        }

    }

    /**
     * Create a standard sized, right-justified label
     *
     * @param title Label text. Should not be {@code null}.
     *
     * @return A new label.
     */
    public static JLabel makeLabelRight(String title) {
        return makeLabelRight(title, null);
    }

    public static JLabel makeLabelRight(String title, Width width) {
        if (width == null) {
            width = Width.SINGLE;
        }
        JLabel newLabel = new JLabel(title);
        setComponentWidth(newLabel, width);
        setLabelPosition(newLabel, Position.RIGHT);
        return newLabel;
    }

    /**
     * Create a standard sized, left-justified label.
     *
     * @param title Label text. Should not be {@code null}.
     *
     * @return A new label.
     */
    public static JLabel makeLabelLeft(String title) {
        return makeLabelLeft(title, null);
    }

    public static JLabel makeLabelLeft(String title, Width width) {
        if (width == null) {
            width = Width.SINGLE;
        }
        JLabel newLabel = new JLabel(title);
        setComponentWidth(newLabel, width);
        setLabelPosition(newLabel, Position.LEFT);
        return newLabel;
    }

    /**
     * Create a sized, labeled component.
     *
     * @param label Label for {@code thing}. Should not be {@code null}.
     * @param thing Component to label. Should not be {@code null}.
     *
     * @return A component with its label to the right.
     */
    public static JPanel makeLabeledComponent(String label, JComponent thing) {
        return makeLabeledComponent(makeLabelRight(label), thing);
    }

    public static JPanel makeLabeledComponent(JLabel label, JComponent thing) {
        return makeLabeledComponent(label, thing, Position.RIGHT);
    }

    public static JPanel makeLabeledComponent(String label, JComponent thing, Position position) {
        return makeLabeledComponent(new JLabel(label), thing, position);
    }

    public static JPanel makeLabeledComponent(JLabel label, JComponent thing, Position position) {
        JPanel newPanel = new JPanel();

        if (position == Position.RIGHT) {
            setComponentWidth(label);
            setLabelPosition(label, Position.RIGHT);
        }

        GroupLayout layout = new GroupLayout(newPanel);
        newPanel.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(label)
                    .addGap(GAP_RELATED)
                    .addComponent(thing))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(LEADING)
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(label)
                    .addComponent(thing))
        );
        return newPanel;
    }

    /**
     * Create a sized, labeled component.
     *
     * @param thing Component to label. Should not be {@code null}.
     * @param label Label for {@code thing}. Should not be {@code null}.
     *
     * @return A labeled component.
     */
    public static JPanel makeComponentLabeled(JComponent thing, String label) {
        return makeComponentLabeled(thing, new JLabel(label));
    }

    public static JPanel makeComponentLabeled(JComponent thing, String label, Position position) {
        return makeComponentLabeled(thing, new JLabel(label), position);
    }

    public static JPanel makeComponentLabeled(JComponent thing, JLabel label) {
        return makeComponentLabeled(thing, label, Position.LEFT);
    }

    public static JPanel makeComponentLabeled(JComponent thing, JLabel label, Position position) {
        JPanel newPanel = new JPanel();

        if (position == Position.RIGHT) {
            setComponentWidth(label);
            setLabelPosition(label, Position.RIGHT);
        }

        GroupLayout layout = new GroupLayout(newPanel);
        newPanel.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(thing)
                    .addGap(GAP_RELATED)
                    .addComponent(label))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(LEADING)
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(thing)
                    .addComponent(label))
        );
        return newPanel;
    }

    /**
     * Set the width of an existing component.
     *
     * @param existingComponent Component that will have its width set.
     */
    public static void setComponentWidth(JComponent existingComponent) {
        setComponentWidth(existingComponent, Width.SINGLE);
    }

    /**
     * Set the width of an existing component using standard McIDAS-V component
     * widths.
     *
     * @param existingComponent Component that will have its width set.
     * @param width Width to use for {@code existingComponent}.
     */
    public static void setComponentWidth(JComponent existingComponent, Width width) {
        if (width == null) {
            width = Width.SINGLE;
        }

        int componentWidth;
        switch (width) {
            case HALF:
                componentWidth = ELEMENT_HALF_WIDTH;
                break;
            case SINGLE:
                componentWidth = ELEMENT_WIDTH;
                break;
            case ONEHALF:
                componentWidth = ELEMENT_ONEHALF_WIDTH;
                break;
            case DOUBLE:
                componentWidth = ELEMENT_DOUBLE_WIDTH;
                break;
            case TRIPLE:
                componentWidth = ELEMENT_DOUBLE_WIDTH + ELEMENT_WIDTH;
                break;
            case QUADRUPLE:
                componentWidth = ELEMENT_DOUBLE_WIDTH + ELEMENT_DOUBLE_WIDTH;
                break;
            case DOUBLEDOUBLE:
                componentWidth = ELEMENT_DOUBLEDOUBLE_WIDTH;
                break;
            default:
                componentWidth = ELEMENT_WIDTH;
                break;
        }
        setComponentWidth(existingComponent, componentWidth);
    }

    /**
     * Set the width of an existing component to a given integer width.
     *
     * @param existingComponent Component that will have its width set.
     * @param width Width to use for {@code existingComponent}.
     */
    public static void setComponentWidth(JComponent existingComponent, int width) {
        existingComponent.setMinimumSize(new Dimension(width, 24));
        existingComponent.setMaximumSize(new Dimension(width, 24));
        existingComponent.setPreferredSize(new Dimension(width, 24));
    }

    /**
     * Set the component width to that of another component.
     */
    public static void setComponentWidth(JComponent setme, JComponent getme) {
        setComponentWidth(setme, getme, 0);
    }

    public static void setComponentWidth(JComponent setme, JComponent getme, int padding) {
        setme.setPreferredSize(new Dimension(getme.getPreferredSize().width + padding, getme.getPreferredSize().height));
    }

    /**
     * Set the component height to that of another component.
     */
    public static void setComponentHeight(JComponent setme, JComponent getme) {
        setComponentHeight(setme, getme, 0);
    }

    public static void setComponentHeight(JComponent setme, JComponent getme, int padding) {
        setme.setPreferredSize(new Dimension(getme.getPreferredSize().width, getme.getPreferredSize().height + padding));
    }

    /**
     * Set the label position of an existing label
     * @param existingLabel
     */
    public static void setLabelPosition(JLabel existingLabel) {
        setLabelPosition(existingLabel, Position.LEFT);
    }

    public static void setLabelPosition(JLabel existingLabel, Position position) {
        switch (position) {
            case LEFT:
                existingLabel.setHorizontalTextPosition(SwingConstants.LEFT);
                existingLabel.setHorizontalAlignment(SwingConstants.LEFT);
                break;

            case RIGHT:
                existingLabel.setHorizontalTextPosition(SwingConstants.RIGHT);
                existingLabel.setHorizontalAlignment(SwingConstants.RIGHT);
                break;

            case CENTER:
                existingLabel.setHorizontalTextPosition(SwingConstants.CENTER);
                existingLabel.setHorizontalAlignment(SwingConstants.CENTER);
                break;

            default:
                existingLabel.setHorizontalTextPosition(SwingConstants.LEFT);
                existingLabel.setHorizontalAlignment(SwingConstants.LEFT);
                break;
        }
    }

    /**
     * Set the bold attribute of an existing label
     * @param existingLabel
     * @param bold
     */
    public static void setLabelBold(JLabel existingLabel, boolean bold) {
        Font f = existingLabel.getFont();
        if (bold) {
            existingLabel.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD));
        } else {
            existingLabel.setFont(f.deriveFont(f.getStyle() | Font.BOLD));
        }
    }

    /**
     * Set the foreground color of an existing component
     * @param existingComponent
     */
    public static void setComponentColor(JComponent existingComponent) {
        setComponentColor(existingComponent, TextColor.NORMAL);
    }

    public static void setComponentColor(JComponent existingComponent, TextColor color) {
        switch (color) {
            case NORMAL:
                existingComponent.setForeground(new Color(0, 0, 0));
                break;

            case STATUS:
                existingComponent.setForeground(MCV_BLUE_DARK);
                break;

            default:
                existingComponent.setForeground(new Color(0, 0, 0));
                break;
        }
    }

    /**
     * Custom makeImageButton to ensure proper sizing and mouseborder are set
     */
    public static JButton makeImageButton(String iconName, 
            final Object object,
            final String methodName,
            final Object arg,
            final String tooltip
    ) {
        final JButton btn = makeImageButton(iconName, tooltip);
        return (JButton)GuiUtils.addActionListener(btn, object, methodName, arg);
    }

    /**
     * Custom makeImageButton to ensure proper sizing and mouseborder are set
     */
    public static JButton makeImageButton(String iconName, String tooltip) {
//        boolean addMouseOverBorder = true;

        ImageIcon imageIcon = GuiUtils.getImageIcon(iconName);
        if (imageIcon.getIconWidth() > 22 || imageIcon.getIconHeight() > 22) {
            Image scaledImage  = imageIcon.getImage().getScaledInstance(22, 22, Image.SCALE_SMOOTH);
            imageIcon = new ImageIcon(scaledImage);
        }

        final JButton btn = GuiUtils.getImageButton(imageIcon);
        btn.setBackground(null);
        btn.setContentAreaFilled(false);
        btn.setSize(new Dimension(24, 24));
        btn.setPreferredSize(new Dimension(24, 24));
        btn.setMinimumSize(new Dimension(24, 24));
//        if (addMouseOverBorder) {
            GuiUtils.makeMouseOverBorder(btn);
//        }
        btn.setToolTipText(tooltip);
        return btn;
    }

    /**
     * Create a button with text and an icon
     */
    public static JButton makeImageTextButton(String iconName, String label) {
        JButton newButton = new JButton(label);
        setButtonImage(newButton, iconName);
        return newButton;
    }

    /**
     * Add an icon to a button... but only if the LookAndFeel supports it
     */
    public static void setButtonImage(JButton existingButton, String iconName) {
        // TODO: see if this is fixed in some future Apple Java release?
        // When using Aqua look and feel don't use icons in the buttons
        // Messes with the button vertical sizing
        if (existingButton.getBorder().toString().indexOf("Aqua") > 0) {
            return;
        }
        ImageIcon imageIcon = GuiUtils.getImageIcon(iconName);
        existingButton.setIcon(imageIcon);
    }

    /**
     * Add an icon to a menu item
     */
    public static void setMenuImage(JMenuItem existingMenuItem, String iconName) {
        ImageIcon imageIcon = GuiUtils.getImageIcon(iconName);
        existingMenuItem.setIcon(imageIcon);
    }

    public static <E> JComboBox<E> makeComboBox(final E[] items, final E selected) {
        return makeComboBox(CollectionHelpers.list(items), selected);
    }

    public static <E> JComboBox<E> makeComboBox(final E[] items, final E selected, final Width width) {
        return makeComboBox(CollectionHelpers.list(items), selected, width);
    }

    public static <E> JComboBox<E> makeComboBox(final Collection<E> items, final E selected) {
        return makeComboBox(items, selected, null);
    }
    
    public static <E> JComboBox<E> makeComboBox(final Collection<E> items, final E selected, final Width width) {
        JComboBox<E> newComboBox = getEditableBox(items, selected);
        setComponentWidth(newComboBox, width);
        return newComboBox;
    }
    
    public static <E> void setListData(final JComboBox<E> box, final Collection<E> items, final E selected) {
        box.removeAllItems();
        if (items != null) {
            for (E o : items) {
                box.addItem(o);
            }
            if (selected != null && !items.contains(selected)) {
                box.addItem(selected);
            }
        }
    }
    
    public static <E> JComboBox<E> getEditableBox(final Collection<E> items, final E selected) {
        JComboBox<E> fld = new JComboBox<>();
        fld.setEditable(true);
        setListData(fld, items, selected);
        if (selected != null) {
            fld.setSelectedItem(selected);
        }
        return fld;
    }

    /**
     * Create a standard sized text field.
     *
     * @param value Text to place within the text field. Should not be {@code null}.
     *
     * @return {@link JTextField} with initial text taken from {@code value}.
     */
    public static JTextField makeTextField(String value) {
        return makeTextField(value, null);
    }

    public static JTextField makeTextField(String value, Width width) {
        JTextField newTextField = new McVTextField(value);
        setComponentWidth(newTextField, width);
        return newTextField;
    }

    /**
     * Create some custom text entry widgets
     */
    public static McVTextField makeTextFieldLimit(String defaultString, int limit) {
        return new McVTextField(defaultString, limit);
    }

    public static McVTextField makeTextFieldUpper(String defaultString, int limit) {
        return new McVTextField(defaultString, limit, true);
    }

    public static McVTextField makeTextFieldAllow(String defaultString, int limit, boolean upper, String allow) {
        McVTextField newField = new McVTextField(defaultString, limit, upper);
        newField.setAllow(allow);
        return newField;
    }

    public static McVTextField makeTextFieldDeny(String defaultString, int limit, boolean upper, String deny) {
        McVTextField newField = new McVTextField(defaultString, limit, upper);
        newField.setDeny(deny);
        return newField;
    }

    public static McVTextField makeTextFieldAllow(String defaultString, int limit, boolean upper, char... allow) {
        McVTextField newField = new McVTextField(defaultString, limit, upper);
        newField.setAllow(allow);
        return newField;
    }

    public static McVTextField makeTextFieldDeny(String defaultString, int limit, boolean upper, char... deny) {
        McVTextField newField = new McVTextField(defaultString, limit, upper);
        newField.setDeny(deny);
        return newField;
    }

    public static McVTextField makeTextFieldAllow(String defaultString, int limit, boolean upper, Pattern allow) {
        McVTextField newField = new McVTextField(defaultString, limit, upper);
        newField.setAllow(allow);
        return newField;
    }

    public static McVTextField makeTextFieldDeny(String defaultString, int limit, boolean upper, Pattern deny) {
        McVTextField newField = new McVTextField(defaultString, limit, upper);
        newField.setDeny(deny);
        return newField;
    }

    /**
     * Use GroupLayout for stacking components vertically.
     * Set center to resize vertically.
     *
     * @param top Component to place at the top of the newly created panel. Should not be {@code null}.
     * @param center Component to place in the center of the newly created panel. Should not be {@code null}.
     * @param bottom Component to place at the bottom of the newly created panel. Should not be {@code null}.
     *
     * @return New {@link JPanel} with the given components in the top, center, and bottom positions.
     */
    public static JPanel topCenterBottom(JComponent top, JComponent center, JComponent bottom) {
        JPanel newPanel = new JPanel();

        GroupLayout layout = new GroupLayout(newPanel);
        newPanel.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(LEADING)
                .addComponent(top, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(center, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(bottom, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(top, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
                    .addPreferredGap(RELATED)
                    .addComponent(center, PREFERRED_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
                    .addPreferredGap(RELATED)
                    .addComponent(bottom, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE))
        );
        return newPanel;
    }

    /**
     * Use GroupLayout for stacking components vertically.
     *
     * @param top Component to place at the top of the newly created panel. Should not be {@code null}.
     * @param bottom Component to place at the bottom of the newly created panel. Should not be {@code null}.
     * @param which Which component's size to prefer. Should not be {@code null}.
     *
     * @return New {@link JPanel} with the given components.
     */
    public static JPanel topBottom(JComponent top, JComponent bottom, Prefer which) {
        JPanel newPanel = new JPanel();

        int topSize = PREFERRED_SIZE;

        if (which == Prefer.TOP) {
            topSize = Short.MAX_VALUE;
        } else if (which == Prefer.BOTTOM) {
            topSize = Short.MAX_VALUE;
        }

        GroupLayout layout = new GroupLayout(newPanel);
        newPanel.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(LEADING)
                .addComponent(top, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(bottom, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(top, PREFERRED_SIZE, DEFAULT_SIZE, topSize)
                    .addPreferredGap(RELATED)
                    .addComponent(bottom, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE))
        );
        return newPanel;
    }

    /**
     * Use GroupLayout for wrapping components to stop vertical resizing.
     *
     * @param left Left component. Should not be {@code null}.
     * @param right Right component. Should not be {@code null}.
     *
     * @return New {@link JPanel} with the given components side-by-side.
     */
    public static JPanel sideBySide(JComponent left, JComponent right) {
        return sideBySide(left, right, GAP_RELATED);
    }

    /**
     * Use GroupLayout for wrapping components to stop vertical resizing.
     *
     * @param left Left component. Should not be {@code null}.
     * @param right Right component. Should not be {@code null}.
     * @param gap Gap between {@code left} and {@code right}.
     *
     * @return New {@link JPanel} with the given components side-by-side,
     * separated by value from {@code gap}.
     */
    public static JPanel sideBySide(JComponent left, JComponent right, int gap) {
        JPanel newPanel = new JPanel();

        GroupLayout layout = new GroupLayout(newPanel);
        newPanel.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(left, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGap(gap)
                    .addComponent(right, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGroup(layout.createParallelGroup(LEADING)
                        .addComponent(left, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
                        .addComponent(right, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)))
        );

        return newPanel;
    }

    /**
     * Use GroupLayout for wrapping a list of components horizontally.
     *
     * @param components Components to stack horizontally. Should not be {@code null}.
     *
     * @return {@link JPanel} with the given components.
     */
    public static JPanel horizontal(Component... components) {
        JPanel newPanel = new JPanel();

        GroupLayout layout = new GroupLayout(newPanel);
        newPanel.setLayout(layout);

        SequentialGroup hGroup = layout.createSequentialGroup();
        for (int i = 0; i < components.length; i++) {
            if (i > 0) {
                hGroup.addGap(GAP_RELATED);
            }
            hGroup.addComponent(components[i], DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE);
        }

        SequentialGroup vGroup = layout.createSequentialGroup();
        ParallelGroup vInner = layout.createParallelGroup(LEADING);
        for (int i = 0; i < components.length; i++) {
            vInner.addComponent(components[i], PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE);
        }
        vGroup.addGroup(vInner);

        layout.setHorizontalGroup(layout.createParallelGroup(LEADING).addGroup(hGroup));
        layout.setVerticalGroup(layout.createParallelGroup(LEADING).addGroup(vGroup));

        return newPanel;
    }

    /**
     * Use GroupLayout for wrapping a list of components vertically.
     *
     * @param components Components to stack vertically. Should not be {@code null}.
     *
     * @return {@link JPanel} with the given components.
     */
    public static JPanel vertical(Component... components) {
        JPanel newPanel = new JPanel();

        GroupLayout layout = new GroupLayout(newPanel);
        newPanel.setLayout(layout);

        ParallelGroup hGroup = layout.createParallelGroup(LEADING);
        for (int i = 0; i < components.length; i++) {
            hGroup.addComponent(components[i], DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE);
        }

        int vSize = PREFERRED_SIZE;

        ParallelGroup vGroup = layout.createParallelGroup(LEADING);
        SequentialGroup vInner = layout.createSequentialGroup();
        for (int i = 0; i < components.length; i++) {
            if (i > 0) {
                vInner.addGap(GAP_RELATED);
            }
            if (i == components.length-1) {
                vSize = Short.MAX_VALUE;
            }
            vInner.addComponent(components[i], PREFERRED_SIZE, DEFAULT_SIZE, vSize);
        }
        vGroup.addGroup(vInner);
        layout.setHorizontalGroup(layout.createParallelGroup(LEADING).addGroup(hGroup));
        layout.setVerticalGroup(layout.createParallelGroup(LEADING).addGroup(vGroup));
        return newPanel;
    }

    /**
     * Hack apart an IDV button panel and do a few things:
     * - Reorder the buttons based on OS preference
     *   Windows: OK on left
     *   Mac: OK on right
     * - Add icons when we understand the button name
     *
     * @param idvButtonPanel {@link JPanel} to scan for understood button names. Should not be {@code null}.
     *
     * @return The given {@code JPanel} with pretty buttons (where possible).
     */
    // TODO: Revisit this?  Could hamper GUI performance.  But it is niiice...
    public static JPanel makePrettyButtons(JPanel idvButtonPanel) {
        // These are the buttons we know about
        JButton buttonOK = null;
        JButton buttonApply = null;
        JButton buttonCancel = null;
        JButton buttonHelp = null;
        JButton buttonNew = null;
        JButton buttonReset = null;
        JButton buttonYes = null;
        JButton buttonNo = null;

        // First pull apart the panel and see if it looks like we expect
        Component[] comps = idvButtonPanel.getComponents();

        // These are the buttons we don't know about
        List<JButton> buttonList = new ArrayList<JButton>(comps.length);

        for (int i = 0; i < comps.length; i++) {
            if (!(comps[i] instanceof JButton)) {
                continue;
            }
            JButton button = (JButton)comps[i];
            if ("OK".equals(button.getText())) {
                buttonOK = makePrettyButton(button);
            } else if ("Apply".equals(button.getText())) {
                buttonApply = makePrettyButton(button);
            } else if ("Cancel".equals(button.getText())) {
                buttonCancel = makePrettyButton(button);
            } else if ("Help".equals(button.getText())) {
                buttonHelp = makePrettyButton(button);
            } else if ("New".equals(button.getText())) {
                buttonNew = makePrettyButton(button);
            } else if ("Reset".equals(button.getText())) {
                buttonReset = makePrettyButton(button);
            } else if ("Yes".equals(button.getText())) {
                buttonYes = makePrettyButton(button);
            } else if ("No".equals(button.getText())) {
                buttonNo = makePrettyButton(button);
            } else {
                buttonList.add(button);
            }
        }

        // If we are on a Mac, this is the order (right aligned)
        // Help, New, Reset, No, Yes, Cancel, Apply, OK
        if (System.getProperty("os.name").contains("Mac OS X")) {
            JPanel newButtonPanel = new JPanel();
            if (buttonHelp != null) {
                newButtonPanel.add(buttonHelp);
            }
            if (buttonNew != null) {
                newButtonPanel.add(buttonNew);
            }
            if (buttonReset != null) {
                newButtonPanel.add(buttonReset);
            }
            if (buttonNo != null) {
                newButtonPanel.add(buttonNo);
            }
            if (buttonYes != null) {
                newButtonPanel.add(buttonYes);
            }
            if (buttonCancel != null) {
                newButtonPanel.add(buttonCancel);
            }
            if (buttonApply != null) {
                newButtonPanel.add(buttonApply);
            }
            if (buttonOK != null) {
                newButtonPanel.add(buttonOK);
            }
            if (!buttonList.isEmpty()) {
                return GuiUtils.right(GuiUtils.hbox(GuiUtils.hbox(buttonList), newButtonPanel));
            } else {
                return GuiUtils.right(newButtonPanel);
            }
        }

        // If we are not on a Mac, this is the order (center aligned)
        // OK, Apply, Cancel, Yes, No, Reset, New, Help
        if (!System.getProperty("os.name").contains("Mac OS X")) {
            JPanel newButtonPanel = new JPanel();
            if (buttonOK != null) {
                newButtonPanel.add(buttonOK);
            }
            if (buttonApply != null) {
                newButtonPanel.add(buttonApply);
            }
            if (buttonCancel != null) {
                newButtonPanel.add(buttonCancel);
            }
            if (buttonYes != null) {
                newButtonPanel.add(buttonYes);
            }
            if (buttonNo != null) {
                newButtonPanel.add(buttonNo);
            }
            if (buttonReset != null) {
                newButtonPanel.add(buttonReset);
            }
            if (buttonNew != null) {
                newButtonPanel.add(buttonNew);
            }
            if (buttonHelp != null) {
                newButtonPanel.add(buttonHelp);
            }
            if (!buttonList.isEmpty()) {
                return GuiUtils.center(GuiUtils.hbox(GuiUtils.hbox(buttonList), newButtonPanel));
            } else {
                return GuiUtils.center(newButtonPanel);
            }
        }

        return idvButtonPanel;
    }

    /**
     * Take a list of buttons and make them pretty.
     * 
     * @param buttonList List of buttons. Should not be {@code null}.
     *
     * @return {@link List} of pretty buttons.
     */
    public static List makePrettyButtons(List buttonList) {
        int size = buttonList.size();
        List newButtons = arrList(size);
        for (int i = 0; i < size; i++) {
            if (buttonList.get(i) instanceof JButton) {
                newButtons.add(makePrettyButton((JButton)(buttonList.get(i))));
            } else {
                newButtons.add(buttonList.get(i));
            }
        }
        return newButtons;
    }

    /**
     * Convenience method to make a button based solely on its name.
     *
     * @param name Button text. Should not be {@code null}.
     *
     * @return A {@literal "pretty"} button.
     */
    public static JButton makePrettyButton(String name) {
        return makePrettyButton(new JButton(name));
    }

    /**
     * Add icons when we understand the button name.
     * 
     * @param button Button to make pretty. Should not be {@code null}.
     *
     * @return button Either the given {@code button} with an icon, or just the given
     * {@code button} (if the name was not understood).
     */
    public static JButton makePrettyButton(JButton button) {
        McVGuiUtils.setComponentWidth(button, Width.ONEHALF);
        if ("OK".equals(button.getText())) {
            McVGuiUtils.setButtonImage(button, ICON_ACCEPT_SMALL);
        } else if ("Apply".equals(button.getText())) {
            McVGuiUtils.setButtonImage(button, ICON_APPLY_SMALL);
        } else if ("Cancel".equals(button.getText())) {
            McVGuiUtils.setButtonImage(button, ICON_CANCEL_SMALL);
        } else if ("Help".equals(button.getText())) {
            McVGuiUtils.setButtonImage(button, ICON_HELP_SMALL);
        } else if ("New".equals(button.getText())) {
            McVGuiUtils.setButtonImage(button, ICON_ADD_SMALL);
        } else if ("Reset".equals(button.getText())) {
            McVGuiUtils.setButtonImage(button, ICON_UNDO_SMALL);
        } else if ("Yes".equals(button.getText())) {
            McVGuiUtils.setButtonImage(button, ICON_ACCEPT_SMALL);
        } else if ("No".equals(button.getText())) {
            McVGuiUtils.setButtonImage(button, ICON_CANCEL_SMALL);
        } else if ("Close".equals(button.getText())) {
            McVGuiUtils.setButtonImage(button, ICON_CANCEL_SMALL);
        } else if ("Previous".equals(button.getText())) {
            McVGuiUtils.setButtonImage(button, ICON_PREVIOUS_SMALL);
        } else if ("Next".equals(button.getText())) {
            McVGuiUtils.setButtonImage(button, ICON_NEXT_SMALL);
        } else if ("Random".equals(button.getText())) {
            McVGuiUtils.setButtonImage(button, ICON_RANDOM_SMALL);
        } else if ("Support Form".equals(button.getText())) {
            McVGuiUtils.setButtonImage(button, ICON_SUPPORT_SMALL);
        }
        return button;
    }

    /**
     * Print the hierarchy of components.
     */
    public static void printUIComponents(JComponent parent) {
        printUIComponents(parent, 0, 0);
    }

    public static void printUIComponents(JComponent parent, int index, int depth) {
        if (parent == null) {
            System.err.println("McVGuiUtils.printUIComponents: null parent");
            return;
        }
        Component[] children = parent.getComponents();
        int childcount = children.length;

        String indent = "";
        for (int d=0; d<depth; d++) {
            indent += "  ";
        }
        System.out.println(indent + index + ": " + parent);

        if (childcount > 0) {
            for (int c=0; c<childcount; c++) {
                if (children[c] instanceof JComponent) {
                    printUIComponents((JComponent)children[c], c, depth+1);
                }
            }
        }
    }

    /**
     * Calls {@link SwingUtilities#invokeLater(Runnable)} if the current thread
     * is not the event dispatch thread. If this thread <b>is</b> the EDT,
     * then call {@link Runnable#run()} for {@code r}.
     * 
     * <p>Remember, you <i>do not</i> want to execute long-running tasks in the
     * event dispatch thread--it'll lock up the GUI.
     * 
     * @param r Code to run in the event dispatch thread. Cannot be {@code null}.
     */
    public static void runOnEDT(final Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }
    }
    
    /**
     * Executes the specified {@link Callable} on the EDT thread. 
     * 
     * <p>If the calling thread is already the EDT thread, this invocation 
     * simply delegates to call(), otherwise the callable is placed in Swing's 
     * event dispatch queue and the method waits for the result.</p>
     * 
     * @param <T> Result type of the {@code callable}.
     * @param callable Callable task. Cannot be {@code null}.
     * 
     * @return Computed result
     */
    public static <T> T getFromEDT(final Callable<T> callable) {
        // credit for this belongs to:
        // http://stackoverflow.com/a/31156045
        final RunnableFuture<T> f = new FutureTask<>(callable);
        
        if (SwingUtilities.isEventDispatchThread()) {
            f.run();
        } else {
            SwingUtilities.invokeLater(f);
        }
        
        T result = null;
        try {
            result = f.get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Thread execution problem", e);
        }
        return result;
    }
    
    //    private static <E> List<E> sizedList() {
    //        McIDASV mcv = McIDASV.getStaticMcv();
    //        int viewManagerCount = ESTIMATED_VM_COUNT;
    //        if (mcv != null) {
    //            ViewManagerManager vmm = cast(mcv.getVMManager());
    //            viewManagerCount = vmm.getViewManagers().size();
    //        }
    //        return arrList(viewManagerCount);
    //    }


    private static int getVMCount() {
        McIDASV mcv = McIDASV.getStaticMcv();
        int viewManagerCount = ESTIMATED_VM_COUNT;
        if (mcv != null) {
            ViewManagerManager vmm = (ViewManagerManager)mcv.getVMManager();
            viewManagerCount = vmm.getViewManagerCount();
        }
        return viewManagerCount;
    }

    private static int getHolderCount() {
        McIDASV mcv = McIDASV.getStaticMcv();
        int holderCount = ESTIMATED_VM_COUNT;
        if (mcv != null) {
            UIManager uiManager = (UIManager)mcv.getIdvUIManager();
            holderCount = uiManager.getComponentHolderCount();
        }
        return holderCount;
    }

    private static int getGroupCount() {
        McIDASV mcv = McIDASV.getStaticMcv();
        int groupCount = ESTIMATED_VM_COUNT;
        if (mcv != null) {
            UIManager uiManager = (UIManager)mcv.getIdvUIManager();
            groupCount = uiManager.getComponentGroupCount();
        }
        return groupCount;
    }

    public static List<ViewManager> getActiveViewManagers() {
        IdvWindow activeWindow = IdvWindow.getActiveWindow();
        List<ViewManager> vms;
        if (activeWindow != null) {
            vms = getViewManagers(activeWindow);
        } else {
            vms = Collections.emptyList();
        }
        return vms;
    }

    public static List<ViewManager> getAllViewManagers() {
        McIDASV mcv = McIDASV.getStaticMcv();
        List<ViewManager> vms = Collections.emptyList();
        if (mcv != null) {
            ViewManagerManager vmm = (ViewManagerManager)mcv.getVMManager();
            vms = arrList(vmm.getViewManagers());
        }
        return vms;
    }

    /**
     * Attempt to find the {@link IdvWindow} that contains the given
     * {@link ViewManager}.
     *
     * @param vm {@code ViewManager} whose {@code IdvWindow} is needed.
     * Cannot be {@code null}.
     *
     * @return Either the {@code IdvWindow} containing {@code vm}, or
     * {@code null}.
     */
    public static IdvWindow getWindowForViewManager(final ViewManager vm) {
        IdvWindow result = null;
        for (IdvWindow w : getAllDisplayWindows()) {
            List<ViewManager> viewManagers = getViewManagers(w);
            if (viewManagers.contains(vm)) {
                result = w;
                break;
            }
        }
        return result;
    }

    public static List<Object> getShareGroupsInWindow(final IdvWindow window) {
        List<ViewManager> vms = arrList(getVMCount());
        vms.addAll(window.getViewManagers());
        for (IdvComponentHolder holder : getComponentHolders(window)) {
            List<ViewManager> holderVms = holder.getViewManagers();
            if (holderVms != null) {
                vms.addAll(holderVms);
            }
        }
        Set<Object> groupIds = newHashSet(vms.size());
        for (ViewManager vm : vms) {
            groupIds.add(vm.getShareGroup());
        }
        return arrList(groupIds);
    }

    public static List<Object> getAllShareGroups() {
        List<ViewManager> vms = getAllViewManagers();
        Set<Object> groupIds = newHashSet(vms.size());
        for (ViewManager vm : vms) {
            groupIds.add(vm.getShareGroup());
        }
        return arrList(groupIds);
    }

    public static List<ViewManager> getViewManagersInGroup(final Object sharedGroup) {
        List<ViewManager> allVMs = getAllViewManagers();
        List<ViewManager> filtered = arrList(allVMs.size());
        for (ViewManager vm : allVMs) {
            if (vm.getShareGroup().equals(sharedGroup)) {
                filtered.add(vm);
            }
        }
        return filtered;
    }

    public static List<ViewManager> getViewManagers(final WindowInfo info) {
        List<ViewManager> vms = arrList(getVMCount());
        for (IdvComponentHolder holder : getComponentHolders(info)) {
            List<ViewManager> holderVms = holder.getViewManagers();
            if (holderVms != null) {
                vms.addAll(holderVms);
            }
        }
        return vms;
    }

    public static List<ViewManager> getViewManagers(final IdvWindow window) {
        List<ViewManager> vms = arrList(getVMCount());
        vms.addAll(window.getViewManagers());
        for (IdvComponentHolder holder : getComponentHolders(window)) {
            List<ViewManager> holderVms = holder.getViewManagers();
            if (holderVms != null) {
                vms.addAll(holderVms);
            }
        }
        return vms;
    }

    /**
     * @return Whether or not {@code h} contains some UI component like
     * the dashboard of field selector. Yes, it can happen!
     */
    public static boolean isUIHolder(final IdvComponentHolder h) {
        if (McvComponentHolder.TYPE_DYNAMIC_SKIN.equals(h.getType())) {
            return false;
        }
        return h.getViewManagers().isEmpty();
    }

    /**
     * @return Whether or not {@code h} is a dynamic skin.
     */
    public static boolean isDynamicSkin(final IdvComponentHolder h) {
        return McvComponentHolder.TYPE_DYNAMIC_SKIN.equals(h.getType());
    }

    /**
     * @return Whether or not {@code windows} has at least one dynamic
     * skin.
     */
    public static boolean hasDynamicSkins(final List<WindowInfo> windows) {
        for (WindowInfo window : windows) {
            for (IdvComponentHolder holder : getComponentHolders(window)) {
                if (isDynamicSkin(holder)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @return The component holders within {@code windowInfo}.
     * @see #getComponentHolders(IdvComponentGroup)
     */
    public static List<IdvComponentHolder> getComponentHolders(final WindowInfo windowInfo) {
        Collection<Object> comps =
            (Collection<Object>)windowInfo.getPersistentComponents().values();
        List<IdvComponentHolder> holders = arrList(getHolderCount());
        for (Object comp : comps) {
            if (!(comp instanceof IdvComponentGroup)) {
                continue;
            }
            holders.addAll(getComponentHolders((IdvComponentGroup)comp));
        }
        return holders;
    }

    /**
     * @return The component holders within {@code idvWindow}.
     * @see #getComponentHolders(IdvComponentGroup)
     */
    public static List<IdvComponentHolder> getComponentHolders(final IdvWindow idvWindow) {
        List<IdvComponentHolder> holders = arrList(getHolderCount());
        for (IdvComponentGroup group : (List<IdvComponentGroup>)idvWindow.getComponentGroups()) {
            holders.addAll(getComponentHolders(group));
        }
        return holders;
    }

    /**
     * @return <b>Recursively</b> searches {@code group} to find any 
     * component holders.
     */
    public static List<IdvComponentHolder> getComponentHolders(final IdvComponentGroup group) {
        List<IdvComponentHolder> holders = arrList(getHolderCount());
        List<ComponentHolder> comps = (List<ComponentHolder>)group.getDisplayComponents();
        if (comps.isEmpty()) {
            return holders;
        }
        for (ComponentHolder comp : comps) {
            if (comp instanceof IdvComponentGroup) {
                holders.addAll(getComponentHolders((IdvComponentGroup) comp));
            } else if (comp instanceof IdvComponentHolder) {
                holders.add((IdvComponentHolder)comp);
            }
        }
        return holders;
    }

    /**
     * @return <b>Recursively</b> searches {@code group} for any nested
     * component groups.
     */
    public static List<IdvComponentGroup> getComponentGroups(final IdvComponentGroup group) {
        List<IdvComponentGroup> groups = arrList(getGroupCount());
        groups.add(group);

        List<ComponentHolder> comps = (List<ComponentHolder>)group.getDisplayComponents();
        if (comps.isEmpty()) {
            return groups;
        }
        for (ComponentHolder comp : comps) {
            if (comp instanceof IdvComponentGroup) {
                groups.addAll(getComponentGroups((IdvComponentGroup)comp));
            }
        }
        return groups;
    }

    /**
     * @return Component groups contained in {@code window}.
     * @see #getComponentGroups(IdvComponentGroup)
     */
    public static List<IdvComponentGroup> getComponentGroups(final WindowInfo window) {
        Collection<Object> comps = (Collection<Object>)window.getPersistentComponents().values();
        for (Object comp : comps) {
            if (comp instanceof IdvComponentGroup) {
                return getComponentGroups((IdvComponentGroup)comp);
            }
        }
        return Collections.emptyList();
    }

    /**
     * @return Component groups contained in {@code windows}.
     * @see #getComponentGroups(IdvComponentGroup)
     */
    public static List<IdvComponentGroup> getComponentGroups(final List<WindowInfo> windows) {
        List<IdvComponentGroup> groups = arrList(getGroupCount());
        for (WindowInfo window : windows) {
            groups.addAll(getComponentGroups(window));
        }
        return groups;
    }

    /**
     * @return The component group within {@code window}.
     */
    public static IdvComponentGroup getComponentGroup(final IdvWindow window) {
        List<IdvComponentGroup> groups = window.getComponentGroups();
        if (!groups.isEmpty()) {
            return groups.get(0);
        }
        return null;
    }

    /**
     * @return Whether or not {@code group} contains any component
     *         groups.
     */
    public static boolean hasNestedGroups(final IdvComponentGroup group) {
        List<ComponentHolder> comps = (List<ComponentHolder>)group.getDisplayComponents();
        for (ComponentHolder comp : comps) {
            if (comp instanceof IdvComponentGroup) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return All active component holders in McIDAS-V.
     */
    // TODO: needs update for nested groups
    public static List<IdvComponentHolder> getAllComponentHolders() {
        List<IdvComponentHolder> holders = arrList(getHolderCount());
        for (IdvComponentGroup g : getAllComponentGroups()) {
            holders.addAll(g.getDisplayComponents());
        }
        return holders;
    }

    /**
     * @return All active component groups in McIDAS-V.
     */
    // TODO: needs update for nested groups
    public static List<IdvComponentGroup> getAllComponentGroups() {
        List<IdvComponentGroup> groups = arrList(getGroupCount());
        for (IdvWindow w : getAllDisplayWindows()) {
            groups.addAll(w.getComponentGroups());
        }
        return groups;
    }

    /**
     * @return All windows that contain at least one component group.
     */
    public static List<IdvWindow> getAllDisplayWindows() {
        List<IdvWindow> allWindows = (List<IdvWindow>)IdvWindow.getWindows();
        List<IdvWindow> windows = arrList(allWindows.size());
        for (IdvWindow w : allWindows) {
            if (!w.getComponentGroups().isEmpty()) {
                windows.add(w);
            }
        }
        return windows;
    }

    /**
     * @return The component holder positioned after the active component holder.
     */
    public static IdvComponentHolder getAfterActiveHolder() {
        return getAfterHolder(getActiveComponentHolder());
    }

    /**
     * @return The component holder positioned before the active component holder.
     */
    public static IdvComponentHolder getBeforeActiveHolder() {
        return getBeforeHolder(getActiveComponentHolder());
    }

    /**
     * @return The active component holder in the active window.
     */
    public static IdvComponentHolder getActiveComponentHolder() {
        IdvWindow window = IdvWindow.getActiveWindow();
        McvComponentGroup group = (McvComponentGroup)getComponentGroup(window);
        return (IdvComponentHolder)group.getActiveComponentHolder();
    }

    /**
     * @return The component holder positioned after {@code current}.
     */
    public static IdvComponentHolder getAfterHolder(final IdvComponentHolder current) {
        List<IdvComponentHolder> holders = getAllComponentHolders();
        int currentIndex = holders.indexOf(current);
        return holders.get((currentIndex + 1) % holders.size());
    }

    /**
     * @return The component holder positioned before {@code current}.
     */
    public static IdvComponentHolder getBeforeHolder(final IdvComponentHolder current) {
        List<IdvComponentHolder> holders = getAllComponentHolders();
        int currentIndex = holders.indexOf(current);
        int newidx = (currentIndex - 1) % holders.size();
        if (newidx == -1) {
            newidx = holders.size() - 1;
        }
        return holders.get(newidx);
    }

    /**
     * @param w {@link IdvWindow} whose component groups you want (as 
     * {@link McvComponentGroup}s).
     * 
     * @return A {@link List} of {@code McvComponentGroup}s or an empty list. 
     * If there were no {@code McvComponentGroup}s in {@code w}, 
     * <b>or</b> if {@code w} is {@code null}, an empty {@code List} is returned.
     */
    public static List<McvComponentGroup> idvGroupsToMcv(final IdvWindow w) {
        if (w == null) {
            return Collections.emptyList();
        }
        final List<IdvComponentGroup> idvLandGroups = w.getComponentGroups();
        final List<McvComponentGroup> groups = arrList(idvLandGroups.size());
        for (IdvComponentGroup group : idvLandGroups) {
            groups.add((McvComponentGroup)group);
        }
        return groups;
    }

    public static void compGroup(final IdvComponentGroup g) {
        compGroup(g, 0);
    }

    public static void compGroup(final IdvComponentGroup g, final int level) {
        p("Comp Group", level);
        p("  name=" + g.getName(), level);
        p("  id=" + g.getUniqueId(), level);
        p("  layout=" + g.getLayout(), level);
        p("  comp count=" + g.getDisplayComponents().size() + ": ", level);
        for (Object comp : g.getDisplayComponents()) {
            if (comp instanceof IdvComponentHolder) {
                compHolder((IdvComponentHolder)comp, level+1);
            } else if (comp instanceof IdvComponentGroup) {
                compGroup((IdvComponentGroup)comp, level+1);
            } else {
                p("    umm=" + comp.getClass().getName(), level);
            }
        }
    }

    public static void compHolder(final IdvComponentHolder h, final int level) {
        p("Comp Holder", level);
        p("  cat=" + h.getCategory(), level);
        p("  name=" + h.getName(), level);
        p("  id=" + h.getUniqueId(), level);
        if (h.getViewManagers() == null) {
            System.err.println("  null vms!");
            return;
        }
        p("  vm count=" + h.getViewManagers().size() + ": ", level);
        for (ViewManager vm : (List<ViewManager>)h.getViewManagers()) {
            p("    " + vmType(vm) + "=" + vm.getViewDescriptor().getName(), level);
        }
    }

    public static List<ViewManager> findvms(final List<WindowInfo> windows) {
        List<ViewManager> vms = new ArrayList<ViewManager>();
        for (WindowInfo window : windows) {
            for (IdvComponentHolder h : getComponentHolders(window)) {
                if (h.getViewManagers() != null) {
                    vms.addAll((List<ViewManager>)h.getViewManagers());
                } else {
                    System.err.println(h.getUniqueId() + " has no vms!");
                }
            }
        }
        for (ViewManager vm : vms) {
            System.err.println("vm=" + vm.getViewDescriptor().getName());
        }
        return vms;
    }

    private static String vmType(final ViewManager vm) {
        if (vm instanceof MapViewManager) {
            if (((MapViewManager)vm).getUseGlobeDisplay()) {
                return "Globe";
            } else {
                return "Map";
            }
        }
        return "Other";
    }

    private static String pad(final String str, final int pad) {
        char[] padding = new char[pad*2];
        for (int i = 0; i < pad*2; i++) {
            padding[i] = ' ';
        }
        return new String(padding).concat(str);
    }

    private static void p(final String str, final int padding) {
        System.err.println(pad(str, padding));
    }

    /**
     * Find the {@literal "bounds"} for the physical display at {@code index}.
     * 
     * @param index Zero-based index of the desired physical display.
     * 
     * @return Either a {@link java.awt.Rectangle} representing the display's
     * bounds, or {@code null} if {@code index} is invalid.
     */
    public static Rectangle getDisplayBoundsFor(final int index) {
        return SystemState.getDisplayBounds().get(index);
    }

    /**
     * Tries to determine the physical display that contains the given
     * {@link java.awt.Rectangle}. <b>This method (currently) fails for 
     * {@code Rectangle}s that span multiple displays!</b>
     * 
     * @param rect {@code Rectangle} to test. Should not be {@code null}.
     * 
     * @return Either the (zero-based) index of the physical display, or 
     * {@code -1} if there was no match.
     */
    public static int findDisplayNumberForRectangle(final Rectangle rect) {
        Map<Integer, Rectangle> bounds = SystemState.getDisplayBounds();
        int index = -1;
        for (Entry<Integer, Rectangle> entry : bounds.entrySet()) {
            if (entry.getValue().contains(rect)) {
                index = entry.getKey();
                break;
            }
        }
        return index;
    }

    /**
     * Tries to determine the physical display that contains the given
     * {@link java.awt.Component}. <b>This method (currently) fails for 
     * {@code Component}s that span multiple displays!</b>
     * 
     * @param comp {@code Component} to test. Should not be {@code null}.
     * 
     * @return Either the (zero-based) index of the physical display, or 
     * {@code -1} if there was no match.
     */
    public static int findDisplayNumberForComponent(final Component comp) {
        return findDisplayNumberForRectangle(
                new Rectangle(comp.getLocation(), comp.getSize()));
    }

    /**
     * Tries to determine the physical display that contains the given
     * {@link ucar.unidata.ui.MultiFrame}. <b>This method (currently) fails 
     * for {@code MultiFrame}s that span multiple displays!</b>
     * 
     * @param mf {@code MultiFrame} to test. Should not be {@code null}.
     * 
     * @return Either the (zero-based) index of the physical display, or 
     * {@code -1} if there was no match.
     */
    public static int findDisplayNumberForMultiFrame(final MultiFrame mf) {
        return findDisplayNumberForRectangle(
                new Rectangle(mf.getLocation(), mf.getSize()));
    }

    /**
     * Tries to determine the physical display that contains the rectangle 
     * defined by the specified coordinates. <b>This method (currently) fails 
     * for coordinates that span multiple displays!</b>
     * 
     * @param x X coordinate of the upper-left corner.
     * @param y Y coordinate of the upper-left corner.
     * @param width Width of the rectangle.
     * @param height Height of the rectangle.
     * 
     * @return Either the (zero-based) index of the physical display, or 
     * {@code -1} if there was no match.
     * 
     * @see java.awt.Rectangle#Rectangle(int, int, int, int)
     */
    public static int findDisplayNumberForCoords(final int x, final int y, 
            final int width, final int height) 
    {
        return findDisplayNumberForRectangle(
                new Rectangle(x, y, width, height));
    }

    /**
     * Tries to determine which physical display contains the 
     * {@link java.awt.Component} or {@link ucar.unidata.ui.MultiFrame} that 
     * fired the given event. <b>This method (currently) fails for coordinates 
     * that span multiple displays!</b>
     * 
     * @param event {@code EventObject} to test. Should not be {@code null}.
     * 
     * @return Either the (zero-based) index of the physical display, or 
     * {@code -1} if there was no match.
     */
    public static int findDisplayNumberForEvent(final EventObject event) {
        int idx = -1;
        Object src = event.getSource();
        if (event instanceof HierarchyEvent) {
            src = ((HierarchyEvent)event).getChanged();
        }
        if (src != null) {
            if (src instanceof Component) {
                idx = findDisplayNumberForComponent((Component)src);
            } else if (src instanceof MultiFrame) {
                idx = findDisplayNumberForMultiFrame((MultiFrame)src);
            }
        }
        return idx;
    }

    // thing below here are courtesy http://tips4java.wordpress.com/2008/11/13/swing-utils/

    /**
     * Convenience method for searching below {@code container} in the
     * component hierarchy and return nested components that are instances of
     * class {@code clazz} it finds. Returns an empty list if no such
     * components exist in the container.
     * <P>
     * Invoking this method with a class parameter of JComponent.class
     * will return all nested components.
     * <P>
     * This method invokes
     * {@link #getDescendantsOfType(Class, java.awt.Container, boolean)}
     *
     * @param clazz the class of components whose instances are to be found.
     * @param container the container at which to begin the search
     * @return the List of components
     */
    public static <T extends JComponent> List<T> getDescendantsOfType(
        Class<T> clazz, Container container) {
        return getDescendantsOfType(clazz, container, true);
    }

    /**
     * Convenience method for searching below {@code container} in the
     * component hierarchy and return nested components that are instances of
     * class {@code clazz} it finds. Returns an empty list if no such
     * components exist in the container.
     * <P>
     * Invoking this method with a class parameter of JComponent.class
     * will return all nested components.
     *
     * @param clazz the class of components whose instances are to be found.
     * @param container the container at which to begin the search
     * @param nested true to list components nested within another listed
     * component, false otherwise
     * @return the List of components
     */
    public static <T extends JComponent> List<T> getDescendantsOfType(
        Class<T> clazz, Container container, boolean nested) {
        List<T> tList = new ArrayList<>();
        for (Component component : container.getComponents()) {
            if (clazz.isAssignableFrom(component.getClass())) {
                tList.add(clazz.cast(component));
            }
            if (nested || !clazz.isAssignableFrom(component.getClass())) {
                tList.addAll(McVGuiUtils.getDescendantsOfType(clazz,
                    (Container)component, nested));
            }
        }
        return tList;
    }

    /**
     * Convenience method that searches below {@code container} in the
     * component hierarchy and returns the first found component that is an
     * instance of class {@code clazz} having the bound property value.
     * Returns {@code null} if such component cannot be found.
     * <P>
     * This method invokes
     * {@link #getDescendantOfType(Class, java.awt.Container, String, Object, boolean)}
     *
     * @param clazz the class of component whose instance is to be found.
     * @param container the container at which to begin the search
     * @param property the className of the bound property, exactly as expressed in
     * the accessor e.g. {@literal "Text"} for getText(), {@literal "Value"}
     * for getValue().
     * @param value the value of the bound property
     * @return the component, or null if no such component exists in the
     * container
     * @throws java.lang.IllegalArgumentException if the bound property does
     * not exist for the class or cannot be accessed
     */
    public static <T extends JComponent> T getDescendantOfType(
        Class<T> clazz, Container container, String property, Object value)
        throws IllegalArgumentException
    {
        return getDescendantOfType(clazz, container, property, value, true);
    }

    /**
     * Convenience method that searches below {@code container} in the
     * component hierarchy and returns the first found component that is an
     * instance of class {@code clazz} and has the bound property value.
     * Returns {@code null} if such component cannot be found.
     *
     * @param clazz the class of component whose instance to be found.
     * @param container the container at which to begin the search
     * @param property the className of the bound property, exactly as expressed in
     * the accessor e.g. {@literal "Text"} for getText(), {@literal "Value"}
     * for getValue().
     * @param value the value of the bound property
     * @param nested true to list components nested within another component
     * which is also an instance of {@code clazz}, false otherwise.
     *
     * @return the component, or null if no such component exists in the
     * container.
     *
     * @throws java.lang.IllegalArgumentException if the bound property does
     * not exist for the class or cannot be accessed.
     */
    public static <T extends JComponent> T getDescendantOfType(Class<T> clazz,
                                                               Container container,
                                                               String property,
                                                               Object value,
                                                               boolean nested)
        throws IllegalArgumentException
    {
        List<T> list = getDescendantsOfType(clazz, container, nested);
        return getComponentFromList(clazz, list, property, value);
    }

    /**
     * Convenience method for searching below {@code container} in the
     * component hierarchy and return nested components of class
     * {@code clazz} it finds.  Returns an empty list if no such
     * components exist in the container.
     * <P>
     * This method invokes
     * {@link #getDescendantsOfClass(Class, java.awt.Container, boolean)}.
     *
     * @param clazz the class of components to be found.
     * @param container the container at which to begin the search
     * @return the List of components
     */
    public static <T extends JComponent> List<T> getDescendantsOfClass(
        Class<T> clazz, Container container)
    {
        return getDescendantsOfClass(clazz, container, true);
    }

    /**
     * Convenience method for searching below {@code container} in the
     * component hierarchy and return nested components of class
     * {@code clazz} it finds.  Returns an empty list if no such
     * components exist in the container.
     *
     * @param clazz the class of components to be found.
     * @param container the container at which to begin the search
     * @param nested true to list components nested within another listed
     * component, false otherwise
     *
     * @return the List of components
     */
    public static <T extends JComponent> List<T> getDescendantsOfClass(
        Class<T> clazz, Container container, boolean nested)
    {
        List<T> tList = new ArrayList<>();
        for (Component component : container.getComponents()) {
            if (clazz.equals(component.getClass())) {
                tList.add(clazz.cast(component));
            }
            if (nested || !clazz.equals(component.getClass())) {
                tList.addAll(McVGuiUtils.getDescendantsOfClass(clazz,
                    (Container)component, nested));
            }
        }
        return tList;
    }

    /**
     * Convenience method that searches below {@code container} in the
     * component hierarchy in a depth first manner and returns the first
     * found component of class {@code clazz} having the bound property
     * value.
     * <P>
     * Returns {@code null} if such component cannot be found.
     * <P>
     * This method invokes
     * {@link #getDescendantOfClass(Class, java.awt.Container, String, Object, boolean)}
     *
     * @param clazz the class of component to be found.
     * @param container the container at which to begin the search
     * @param property the className of the bound property, exactly as expressed in
     * the accessor e.g. {@literal "Text"} for getText(), {@literal "Value"}
     * for getValue(). This parameter is case sensitive.
     * @param value the value of the bound property
     *
     * @return the component, or null if no such component exists in the
     * container's hierarchy.
     *
     * @throws java.lang.IllegalArgumentException if the bound property does
     * not exist for the class or cannot be accessed
     */
    public static <T extends JComponent> T getDescendantOfClass(Class<T> clazz,
                                                                Container container,
                                                                String property,
                                                                Object value)
        throws IllegalArgumentException
    {
        return getDescendantOfClass(clazz, container, property, value, true);
    }

    /**
     * Convenience method that searches below {@code container} in the
     * component hierarchy in a depth first manner and returns the first
     * found component of class {@code clazz} having the bound property
     * value.
     * <P>
     * Returns {@code null} if such component cannot be found.
     *
     * @param clazz the class of component to be found.
     * @param container the container at which to begin the search
     * @param property the className of the bound property, exactly as expressed
     * in the accessor e.g. {@literal "Text"} for getText(), {@literal "Value"}
     * for getValue(). This parameter is case sensitive.
     * @param value the value of the bound property
     * @param nested true to include components nested within another listed
     * component, false otherwise.
     *
     * @return the component, or null if no such component exists in the
     * container's hierarchy.
     *
     * @throws java.lang.IllegalArgumentException if the bound property does
     * not exist for the class or cannot be accessed
     */
    public static <T extends JComponent> T getDescendantOfClass(Class<T> clazz,
                                                                Container container,
                                                                String property,
                                                                Object value,
                                                                boolean nested)
        throws IllegalArgumentException
    {
        List<T> list = getDescendantsOfClass(clazz, container, nested);
        return getComponentFromList(clazz, list, property, value);
    }

    private static <T extends JComponent> T getComponentFromList(Class<T> clazz,
                                                                 List<T> list,
                                                                 String property,
                                                                 Object value)
        throws IllegalArgumentException
    {
        T retVal = null;
        Method method = null;
        try {
            method = clazz.getMethod("get" + property);
        } catch (NoSuchMethodException ex) {
            try {
                method = clazz.getMethod("is" + property);
            } catch (NoSuchMethodException ex1) {
                throw new IllegalArgumentException("Property " + property +
                    " not found in class " + clazz.getName(), ex1);
            }
        }
        try {
            for (T t : list) {
                Object testVal = method.invoke(t);
                if (equals(value, testVal)) {
                    return t;
                }
            }
        } catch (InvocationTargetException ex) {
            throw new IllegalArgumentException(
                "Error accessing property " + property +
                    " in class " + clazz.getName(), ex);
        } catch (IllegalAccessException ex) {
            throw new IllegalArgumentException(
                "Property " + property +
                    " cannot be accessed in class " + clazz.getName(), ex);
        } catch (SecurityException ex) {
            throw new IllegalArgumentException(
                "Property " + property +
                    " cannot be accessed in class " + clazz.getName(), ex);
        }
        return retVal;
    }

    /**
     * Convenience method for determining whether two objects are either
     * equal or both null.
     *
     * @param obj1 the first reference object to compare.
     * @param obj2 the second reference object to compare.
     * @return true if obj1 and obj2 are equal or if both are null,
     * false otherwise
     */
    public static boolean equals(Object obj1, Object obj2) {
        return (obj1 == null) ? (obj2 == null) : obj1.equals(obj2);
    }

    /**
     * Convenience method for mapping a container in the hierarchy to its
     * contained components.  The keys are the containers, and the values
     * are lists of contained components.
     * <P>
     * Implementation note:  The returned value is a HashMap and the values
     * are of type ArrayList.  This is subject to change, so callers should
     * code against the interfaces Map and List.
     *
     * @param container The JComponent to be mapped
     * @param nested true to drill down to nested containers, false otherwise
     * @return the Map of the UI
     */
    public static Map<JComponent, List<JComponent>> getComponentMap(
        JComponent container, boolean nested)
    {
        List<JComponent> descendants =
            getDescendantsOfType(JComponent.class, container, false);
        Map<JComponent, List<JComponent>> retVal =
            new HashMap<>(descendants.size());
        for (JComponent component : descendants) {
            if (!retVal.containsKey(container)) {
                retVal.put(container,
                    new ArrayList<JComponent>());
            }
            retVal.get(container).add(component);
            if (nested) {
                retVal.putAll(getComponentMap(component, nested));
            }
        }
        return retVal;
    }

    /**
     * Convenience method for retrieving a subset of the UIDefaults pertaining
     * to a particular class.
     *
     * @param clazz the class of interest
     * @return the UIDefaults of the class
     */
    public static UIDefaults getUIDefaultsOfClass(Class<?> clazz) {
        String name = clazz.getName();
        name = name.substring(name.lastIndexOf(".") + 2);
        return getUIDefaultsOfClass(name);
    }

    /**
     * Convenience method for retrieving a subset of the UIDefaults pertaining
     * to a particular class.
     *
     * @param className fully qualified name of the class of interest
     * @return the UIDefaults of the class named
     */
    public static UIDefaults getUIDefaultsOfClass(String className) {
        UIDefaults retVal = new UIDefaults();
        UIDefaults defaults = javax.swing.UIManager.getLookAndFeelDefaults();
        List<?> listKeys = Collections.list(defaults.keys());
        for (Object key : listKeys) {
            if ((key instanceof String) && ((String) key).startsWith(className)) {
                String stringKey = (String) key;
                String property = stringKey;
                if (stringKey.contains(".")) {
                    property = stringKey.substring(stringKey.indexOf(".") + 1);
                }
                retVal.put(property, defaults.get(key));
            }
        }
        return retVal;
    }

    /**
     * Convenience method for retrieving the UIDefault for a single property
     * of a particular class.
     *
     * @param clazz the class of interest
     * @param property the property to query
     * @return the UIDefault property, or null if not found
     */
    public static Object getUIDefaultOfClass(Class<?> clazz, String property) {
        Object retVal = null;
        UIDefaults defaults = getUIDefaultsOfClass(clazz);
        List<Object> listKeys = Collections.list(defaults.keys());
        for (Object key : listKeys) {
            if (key.equals(property)) {
                return defaults.get(key);
            }
            if (key.toString().equalsIgnoreCase(property)) {
                retVal = defaults.get(key);
            }
        }
        return retVal;
    }

    /**
     * Exclude methods that return values that are meaningless to the user
     */
    static Set<String> setExclude = new HashSet<>(10);
    static {
        setExclude.add("getFocusCycleRootAncestor");
        setExclude.add("getAccessibleContext");
        setExclude.add("getColorModel");
        setExclude.add("getGraphics");
        setExclude.add("getGraphicsConfiguration");
    }

    /**
     * Convenience method for obtaining most non-null human readable properties
     * of a JComponent.  Array properties are not included.
     * <P>
     * Implementation note:  The returned value is a HashMap.  This is subject
     * to change, so callers should code against the interface Map.
     *
     * @param component the component whose proerties are to be determined
     * @return the class and value of the properties
     */
    public static Map<Object, Object> getProperties(JComponent component) {
        Class<?> clazz = component.getClass();
        Method[] methods = clazz.getMethods();
        Map<Object, Object> retVal = new HashMap<>(methods.length);
        Object value = null;
        for (Method method : methods) {
            if (method.getName().matches("^(is|get).*") &&
                (method.getParameterTypes().length == 0)) {
                try {
                    Class<?> returnType = method.getReturnType();
                    if ((returnType != void.class) &&
                        !returnType.getName().startsWith("[") &&
                        !setExclude.contains(method.getName())) {
                        String key = method.getName();
                        value = method.invoke(component);
                        if ((value != null) && !(value instanceof Component)) {
                            retVal.put(key, value);
                        }
                    }
                    // ignore exceptions that arise if the property could not be accessed
                } catch (IllegalAccessException ex) {
                } catch (IllegalArgumentException ex) {
                } catch (InvocationTargetException ex) {
                }
            }
        }
        return retVal;
    }

    /**
     * Convenience method to obtain the Swing class from which this
     * component was directly or indirectly derived.
     *
     * @param component The component whose Swing superclass is to be
     * determined
     * @return The nearest Swing class in the inheritance tree
     */
    public static <T extends JComponent> Class<?> getJClass(T component) {
        Class<?> clazz = component.getClass();
        while (!clazz.getName().matches("javax.swing.J[^.]*$")) {
            clazz = clazz.getSuperclass();
        }
        return clazz;
    }

    /**
     * Gets the {@literal "text"} contents of a {@link JTextComponent} without
     * the possibility of a {@code NullPointerException}.
     *
     * @param textComponent {@code JTextComponent} whose contents should be
     * extracted. {@code null} is allowed.
     *
     * @return Either the results of {@link JTextComponent#getText()} or an
     * empty {@code String} if {@code textComponent} or {@code getText()} are
     * {@code null}.
     */
    public static String safeGetText(JTextComponent textComponent) {
        String value = "";
        if ((textComponent != null) && (textComponent.getText() != null)) {
            value = textComponent.getText();
        }
        return value;
    }
}
