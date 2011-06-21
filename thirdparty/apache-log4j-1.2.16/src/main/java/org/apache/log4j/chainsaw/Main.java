/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.log4j.chainsaw;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.Properties;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * The main application.
 *
 * @author <a href="mailto:oliver@puppycrawl.com">Oliver Burn</a>
 */
public class Main
    extends JFrame
{
    /** the default port number to listen on **/
    private static final int DEFAULT_PORT = 4445;

    /** name of property for port name **/
    public static final String PORT_PROP_NAME = "chainsaw.port";

    /** use to log messages **/
    private static final Logger LOG = Logger.getLogger(Main.class);


    /**
     * Creates a new <code>Main</code> instance.
     */
    private Main() {
        super("CHAINSAW - Log4J Log Viewer");
        // create the all important model
        final MyTableModel model = new MyTableModel();

        //Create the menu bar.
        final JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);
        final JMenu menu = new JMenu("File");
        menuBar.add(menu);

        try {
            final LoadXMLAction lxa = new LoadXMLAction(this, model);
            final JMenuItem loadMenuItem = new JMenuItem("Load file...");
            menu.add(loadMenuItem);
            loadMenuItem.addActionListener(lxa);
        } catch (NoClassDefFoundError e) {
            LOG.info("Missing classes for XML parser", e);
            JOptionPane.showMessageDialog(
                this,
                "XML parser not in classpath - unable to load XML events.",
                "CHAINSAW",
                JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            LOG.info("Unable to create the action to load XML files", e);
            JOptionPane.showMessageDialog(
                this,
                "Unable to create a XML parser - unable to load XML events.",
                "CHAINSAW",
                JOptionPane.ERROR_MESSAGE);
        }

        final JMenuItem exitMenuItem = new JMenuItem("Exit");
        menu.add(exitMenuItem);
        exitMenuItem.addActionListener(ExitAction.INSTANCE);

        // Add control panel
        final ControlPanel cp = new ControlPanel(model);
        getContentPane().add(cp, BorderLayout.NORTH);

        // Create the table
        final JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        final JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Events: "));
        scrollPane.setPreferredSize(new Dimension(900, 300));

        // Create the details
        final JPanel details = new DetailPanel(table, model);
        details.setPreferredSize(new Dimension(900, 300));

        // Add the table and stack trace into a splitter
        final JSplitPane jsp =
            new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollPane, details);
        getContentPane().add(jsp, BorderLayout.CENTER);

        addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent aEvent) {
                    ExitAction.INSTANCE.actionPerformed(null);
                }
            });

        pack();
        setVisible(true);

        setupReceiver(model);
    }

    /**
     * Setup recieving messages.
     *
     * @param aModel a <code>MyTableModel</code> value
     */
    private void setupReceiver(MyTableModel aModel) {
        int port = DEFAULT_PORT;
        final String strRep = System.getProperty(PORT_PROP_NAME);
        if (strRep != null) {
            try {
                port = Integer.parseInt(strRep);
            } catch (NumberFormatException nfe) {
                LOG.fatal("Unable to parse " + PORT_PROP_NAME +
                          " property with value " + strRep + ".");
                JOptionPane.showMessageDialog(
                    this,
                    "Unable to parse port number from '" + strRep +
                    "', quitting.",
                    "CHAINSAW",
                    JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        }

        try {
            final LoggingReceiver lr = new LoggingReceiver(aModel, port);
            lr.start();
        } catch (IOException e) {
            LOG.fatal("Unable to connect to socket server, quiting", e);
            JOptionPane.showMessageDialog(
                this,
                "Unable to create socket on port " + port + ", quitting.",
                "CHAINSAW",
                JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }


    ////////////////////////////////////////////////////////////////////////////
    // static methods
    ////////////////////////////////////////////////////////////////////////////


    /** initialise log4j **/
    private static void initLog4J() {
        final Properties props = new Properties();
        props.setProperty("log4j.rootLogger", "DEBUG, A1");
        props.setProperty("log4j.appender.A1",
                          "org.apache.log4j.ConsoleAppender");
        props.setProperty("log4j.appender.A1.layout",
                          "org.apache.log4j.TTCCLayout");
        PropertyConfigurator.configure(props);
    }

    /**
     * The main method.
     *
     * @param aArgs ignored
     */
    public static void main(String[] aArgs) {
        initLog4J();
        new Main();
    }
}
