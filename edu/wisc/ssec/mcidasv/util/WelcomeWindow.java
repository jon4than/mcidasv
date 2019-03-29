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

import static edu.wisc.ssec.mcidasv.util.McVGuiUtils.setButtonImage;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.IOException;

import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.LayoutStyle;
import javax.swing.WindowConstants;
import javax.swing.event.HyperlinkEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@code WelcomeWindow} is really just intended to <i>try</i> to detect known
 * hardware problems and inform the user about any problems.
 *
 * <p>The current implementation does not perform <i>any</i> detection, but
 * expect this to change.
 */
// NOTE TO MCV CODERS:
// **DOCUMENT WHAT CHECKS AND/OR DETECTION ARE BEING PERFORMED**
//public class WelcomeWindow extends JFrame {
public class WelcomeWindow extends JDialog {
    
    private static final Logger logger =
        LoggerFactory.getLogger(WelcomeWindow.class);
    
    /** Path to {@literal "header"} image. */
    private static final String LOGO_PATH = 
        "/edu/wisc/ssec/mcidasv/images/mcidasv_logo.gif";

    /** Path to the HTML to display within {@link #textPane}. */
    private static final String WELCOME_HTML =
        "/edu/wisc/ssec/mcidasv/resources/welcome.html";

    /**
     * Message to display if there was a problem loading
     * {@link #WELCOME_HTML}.
     */
    private static final String ERROR_MESSAGE =
        "McIDAS-V had a problem displaying its welcome message. Please"
        + " contact the McIDAS Help Desk for assistance.";

    /** Dimensions of the welcome window frame. */
    private static final Dimension WINDOW_SIZE = new Dimension(495, 431);

    /** Default auto-quit delay (in milliseconds). */
    public static final long DEFAULT_QUIT_DELAY = 2500;

    /** Java-friendly location of the path to the welcome message. */
    private final java.net.URL contents;

    /** Whether or not the window should automatically close. */
    private final boolean autoQuit;

    /** Delay in milliseconds for auto-quitting. */
    private final long autoQuitDelay;

    /** 
     * Creates new form WelcomeWindow.
     */
    public WelcomeWindow() {
        this(false, Long.MAX_VALUE);
    }

    public WelcomeWindow(boolean autoQuit, long delay) {
        this.autoQuit = autoQuit;
        this.autoQuitDelay = delay;
        this.contents = WelcomeWindow.class.getResource(WELCOME_HTML);
        initComponents();
    }

    /** 
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">
    private void initComponents() {

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Welcome to McIDAS-V");
        setLocationByPlatform(true);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        logoPanel.setLayout(new GridBagLayout());

        logoLabel.setIcon(new ImageIcon(getClass().getResource(LOGO_PATH))); // NOI18N
        logoPanel.add(logoLabel, new GridBagConstraints());

        textPane.setEditable(false);
        try {
            textPane.setPage(contents);
        } catch (IOException e) {
            textPane.setText(ERROR_MESSAGE);
            logger.error("Could not change contents of textPane", e);
        }
        textPane.addHyperlinkListener(this::textPaneHyperlinkUpdate);
        scrollPane.setViewportView(textPane);

        GroupLayout mainPanelLayout = new GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(scrollPane, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 360, Short.MAX_VALUE)
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 273, Short.MAX_VALUE)
        );

        setButtonImage(startButton, McVGuiUtils.ICON_APPLY_SMALL);
        startButton.addActionListener(this::startButtonActionPerformed);

        setButtonImage(quitButton, McVGuiUtils.ICON_CANCEL_SMALL);
        quitButton.addActionListener(this::quitButtonActionPerformed);

        GroupLayout buttonPanelLayout = new GroupLayout(buttonPanel);
        buttonPanel.setLayout(buttonPanelLayout);
        buttonPanelLayout.setHorizontalGroup(
            buttonPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(GroupLayout.Alignment.TRAILING, buttonPanelLayout.createSequentialGroup()
                .addContainerGap(144, Short.MAX_VALUE)
                .addComponent(quitButton)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(startButton))
        );
        buttonPanelLayout.setVerticalGroup(
            buttonPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(buttonPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(startButton)
                .addComponent(quitButton))
        );

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(mainPanel, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(buttonPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(logoPanel, GroupLayout.DEFAULT_SIZE, 360, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(logoPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(mainPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
        setLocationRelativeTo(null);
        setSize(WINDOW_SIZE);
        setModal(true);
    }// </editor-fold>

    /**
     * Show or close the Welcome Window.
     *
     * <p>Overridden in McV to handle auto-quitting. If we're supposed to
     * auto-quit, a thread will be started that calls {@link JButton#doClick()}.
     * </p>
     *
     * @param visible Whether or not the dialog should be opened or closed.
     */
    @Override public void setVisible(boolean visible) {
        if (autoQuit) {
            new Thread() {
                public void run() {
                    try {
                        sleep(autoQuitDelay);
                    } catch (InterruptedException ex) {
                        // not much else to be done...
                        logger.error("Problem changing visibility", ex);
                    } finally {
                        EventQueue.invokeLater(startButton::doClick);
                    }
                }
            }.start();
        }
        super.setVisible(visible);
    }

    /**
     * Handles the user clicking on {@link #startButton}. 
     *
     * @param evt Event to handle. Currently ignored.
     */
    private void startButtonActionPerformed(ActionEvent evt) {
        this.setVisible(false);
    }

    /**
     * Handles the user clicking on {@link #quitButton}. Doesn't do anything
     * aside from handing off things to
     * {@link #formWindowClosing(WindowEvent)}
     *
     * @param evt Event to handle. Currently ignored.
     *
     * @see #formWindowClosing(WindowEvent)
     */
    private void quitButtonActionPerformed(ActionEvent evt) {
        formWindowClosing(null);
    }

    /**
     * Handles the user opting to close the welcome window
     * {@link JFrame}. Executes {@code System.exit(1)} in an
     * effort to signal to the startup scripts that window terminated 
     * {@literal "abnormally"}.
     * 
     * <p>An abnormal termination will result in the startup script 
     * terminating the launch of McIDAS-V.
     * 
     * @param evt Event to handle. Currently ignored.
     */
    private void formWindowClosing(WindowEvent evt) {
        System.exit(1);
    }

    /**
     * Listens to {@link #textPane} in order to handle the user clicking on
     * HTML links.
     *
     * @param evt Event to handle. Anything other than
     * an {@literal "ACTIVATED"}
     * {@link javax.swing.event.HyperlinkEvent.EventType HyperlinkEvent.EventType}
     * is ignored.
     *
     * @see WebBrowser#browse(String)
     */
    private void textPaneHyperlinkUpdate(HyperlinkEvent evt) {
        if (evt.getEventType() != HyperlinkEvent.EventType.ACTIVATED) {
            return;
        }

        String url = null;
        if (evt.getURL() == null) {
            url = evt.getDescription();
        } else {
            url = evt.getURL().toString();
        }

        WebBrowser.browse(url);
    }

    /**
     * Kick the tires.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            WelcomeWindow ww;
            if (args.length == 2) {
                boolean autoQuit = "-autoquit".equals(args[0]);
                long delay = Long.parseLong(args[1]);
                ww = new WelcomeWindow(autoQuit, delay);
            } else if (args.length == 1) {
                boolean autoQuit = "-autoquit".equals(args[0]);
                ww = new WelcomeWindow(autoQuit, DEFAULT_QUIT_DELAY);
            }
            else {
                ww = new WelcomeWindow();
            }
            ww.setVisible(true);
        });
    }

    // boring gui components
    private final JPanel buttonPanel = new JPanel();
    private final JLabel logoLabel = new JLabel();
    private final JPanel logoPanel = new JPanel();
    private final JPanel mainPanel = new JPanel();
    private final JButton quitButton = new JButton("Quit");
    private final JScrollPane scrollPane = new JScrollPane();
    private final JButton startButton = new JButton("Start McIDAS-V");
    private final JTextPane textPane = new JTextPane();
}
