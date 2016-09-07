/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2016
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

package edu.wisc.ssec.mcidasv.ui;

import static java.awt.Color.GRAY;
import static javax.swing.BorderFactory.createBevelBorder;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static javax.swing.border.BevelBorder.RAISED;
import static ucar.unidata.util.GuiUtils.getImageIcon;
import static ucar.unidata.util.LayoutUtil.inset;
import static ucar.unidata.util.LayoutUtil.topCenter;

import java.awt.Container;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.GroupLayout;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.DefaultCaret;

import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.McIDASV;
import edu.wisc.ssec.mcidasv.StateManager;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils;
import edu.wisc.ssec.mcidasv.util.SystemState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AboutFrame {
    
    /** Logging object. */
    private static final Logger logger =
        LoggerFactory.getLogger(AboutFrame.class);
    
    /**
     * Initial message in text area within {@literal "System Information"} tab.
     */
    private static final String PLEASE_WAIT =
        "Please wait, collecting system information...";
    
    /** Text used as the title for this window. */
    private static final String WINDOW_TITLE = "About McIDAS-V";
    
    /** Name of the first tab. */
    private static final String MCV_TAB_TITLE = "McIDAS-V";
    
    /** Name of the second tab. */
    private static final String SYS_TAB_TITLE = "System Information";
    
    private static final String ERR_INTERRUPTED =
        "Processing was interrupted!\nPlease check logging output.";
    
    private static final String ERR_PROBLEM =
        "Encountered a problem while processing system information.\n" +
        "Please consider contacting the McIDAS Help Desk:\n" +
        "\"Help>Show Support Request Form\"\n" +
        "-OR-\n" +
        "\"Help>Visit Online Forums\" "+
        "(url: http://http://mcidasv.ssec.wisc.edu/forums/)";
    
    
    /** Reference to the main McIDAS-V object. */
    private final McIDASV mcv;
    
    /**
     * Text area within the {@literal "System Information"} tab.
     * Value may be {@code null}.
     */
    private JTextArea sysTextArea;
    
    private JFrame frame;
    
    /** Whether or not the system information has been collected. */
    private final AtomicBoolean hasSysInfo;
    
    /**
     * Creates a new NewAboutFrame window.
     *
     * Can be called from threads other than the Event Dispatch Thread.
     *
     * @param mcv McIDAS-V object. Cannot be {@code null}.
     *
     * @throws NullPointerException if {@code mcv} is {@code null}.
     */
    public AboutFrame(final McIDASV mcv) {
        Objects.requireNonNull("mcv reference cannot be null");
        this.mcv = mcv;
        this.hasSysInfo = new AtomicBoolean(false);
        McVGuiUtils.runOnEDT(this::initComponents);
    }
    
    /**
     * Shows or hides the {@link #frame}.
     *
     * Can be called from threads other than the Event Dispatch Thread.
     *
     * @param visible New visibility for {@link #frame}.
     */
    public void setVisible(boolean visible) {
        // should not need to worry about NPE for frame; it's created by the
        // constructor.
        McVGuiUtils.runOnEDT(() -> frame.setVisible(visible));
    }
    
    /**
     * Called by the constructor to initialize the {@literal "About"} window.
     */
    private void initComponents() {
        frame = new JFrame();
    
        JTabbedPane tabbedPanel = new JTabbedPane();
        JPanel mcvTab = new JPanel();
        JPanel mcvPanel = buildAboutMcv();
        JPanel sysTab = new JPanel();
        JScrollPane sysScrollPane = new JScrollPane();
        sysTextArea = new JTextArea();
    
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setTitle(WINDOW_TITLE);
    
        GroupLayout mcvTabLayout = new GroupLayout(mcvTab);
        mcvTab.setLayout(mcvTabLayout);
        mcvTabLayout.setHorizontalGroup(
            mcvTabLayout.createParallelGroup(LEADING)
                .addComponent(mcvPanel)
        );
        mcvTabLayout.setVerticalGroup(
            mcvTabLayout.createParallelGroup(LEADING)
                .addComponent(mcvPanel)
        );
    
        tabbedPanel.addTab(MCV_TAB_TITLE, mcvTab);
    
        sysTextArea.setText(PLEASE_WAIT);
    
        sysTextArea.setEditable(false);
        sysTextArea.setFont(new Font(Font.MONOSPACED, 0, 12)); // NOI18N
        sysTextArea.setCaretPosition(0);
        sysTextArea.setLineWrap(false);
        sysScrollPane.setViewportView(sysTextArea);
    
        GroupLayout sysTabLayout = new GroupLayout(sysTab);
        sysTab.setLayout(sysTabLayout);
        sysTabLayout.setHorizontalGroup(
            sysTabLayout.createParallelGroup(LEADING)
                .addGroup(sysTabLayout.createSequentialGroup()
                    .addComponent(sysScrollPane))
        );
        sysTabLayout.setVerticalGroup(
            sysTabLayout.createParallelGroup(LEADING)
                .addGroup(sysTabLayout.createSequentialGroup()
                    .addComponent(sysScrollPane))
        );
    
        tabbedPanel.addTab(SYS_TAB_TITLE, sysTab);
    
        Container contentPane = frame.getContentPane();
        GroupLayout layout = new GroupLayout(contentPane);
        contentPane.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(LEADING)
                .addComponent(tabbedPanel)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(LEADING)
                .addComponent(tabbedPanel)
        );
        
        // populating the text field has to happen on the EDT, and the widget
        // just takes awhile to deal with the amount of text we're inserting...
        // this trick allows us to delay things until the user explicitly
        // decides to view the system info tab.
        // end result: GUI seems more lively!
        tabbedPanel.addChangeListener(e -> {
            JTabbedPane tabPane = (JTabbedPane)e.getSource();
            int newIndex = tabPane.getSelectedIndex();
            if (newIndex == 1) {
                populateSystemTab();
            }
        });
        
        frame.pack();
        frame.setSize(450, 375);
        frame.setLocationRelativeTo(mcv.getIdvUIManager().getFrame());
    }
    
    private String getSystemInformation() {
        return SystemState.getStateAsString(mcv, true);
    }
    
    /**
     * Populates the {@literal "System Information"} tab.
     *
     * The system information is collected on a separate thread, and when done,
     * the results are added to the tab (on the Event Dispatch Thread).
     */
    private void populateSystemTab() {
        if (!hasSysInfo.get()) {
            PopulateSystemTabTask task = new PopulateSystemTabTask();
            task.execute();
        }
    }
    
    private JPanel buildAboutMcv() {
        StateManager stateManager = (StateManager)mcv.getStateManager();
        
        JEditorPane editor = new JEditorPane();
        editor.setEditable(false);
        editor.setContentType("text/html");
        String html = stateManager.getMcIdasVersionAbout();
        editor.setText(html);
        editor.setBackground(new JPanel().getBackground());
        editor.addHyperlinkListener(mcv);
        
        String splashIcon = mcv.getProperty(Constants.PROP_SPLASHICON, "");
        final JLabel iconLbl = new JLabel(getImageIcon(splashIcon));
        
        iconLbl.setToolTipText("McIDAS-V Homepage");
        iconLbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        iconLbl.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                String url = mcv.getProperty(Constants.PROP_HOMEPAGE, "");
                try {
                    HyperlinkEvent link = new HyperlinkEvent(
                        iconLbl,
                        HyperlinkEvent.EventType.ACTIVATED,
                        new URL(url)
                    );
                    mcv.hyperlinkUpdate(link);
                } catch (MalformedURLException e) {
                    logger.warn("Malformed URL: '"+url+"'", e);
                }
                
            }
        });
        JPanel contents = topCenter(inset(iconLbl, 5), inset(editor, 5));
        contents.setBorder(createBevelBorder(RAISED, GRAY, GRAY));
        return contents;
    }
    
    private class PopulateSystemTabTask extends SwingWorker<String, Object> {
        @Override public String doInBackground() {
            return getSystemInformation();
        }
        
        @Override protected void done() {
            // the caret manipulation is done so that the "append"
            // call doesn't result in the text area being
            // auto-scrolled to the end. however, it's also nice to
            // have the caret available so that keystroke navigation
            // of the text area still works.
            DefaultCaret caret = (DefaultCaret)sysTextArea.getCaret();
            caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
            try {
                sysTextArea.setText(get());
                hasSysInfo.set(true);
            } catch (InterruptedException ex) {
                logger.warn("Interrupted while populating system tab", ex);
                sysTextArea.setText(ERR_INTERRUPTED);
            } catch (ExecutionException ex) {
                logger.error("Could not populate system tab!", ex);
                sysTextArea.setText(ERR_PROBLEM);
            } finally {
                caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
            }
        }
    }
}
