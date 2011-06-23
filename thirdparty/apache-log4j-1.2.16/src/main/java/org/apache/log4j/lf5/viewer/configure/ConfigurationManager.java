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
package org.apache.log4j.lf5.viewer.configure;

import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.tree.TreePath;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.lf5.LogLevel;
import org.apache.log4j.lf5.LogLevelFormatException;
import org.apache.log4j.lf5.viewer.LogBrokerMonitor;
import org.apache.log4j.lf5.viewer.LogTable;
import org.apache.log4j.lf5.viewer.LogTableColumn;
import org.apache.log4j.lf5.viewer.LogTableColumnFormatException;
import org.apache.log4j.lf5.viewer.categoryexplorer.CategoryExplorerModel;
import org.apache.log4j.lf5.viewer.categoryexplorer.CategoryExplorerTree;
import org.apache.log4j.lf5.viewer.categoryexplorer.CategoryNode;
import org.apache.log4j.lf5.viewer.categoryexplorer.CategoryPath;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * <p>ConfigurationManager handles the storage and retrival of the state of
 * the CategoryExplorer
 *
 * @author Richard Hurst
 * @author Brad Marlborough
 */

// Contributed by ThoughtWorks Inc.

public class ConfigurationManager extends Object {
  //--------------------------------------------------------------------------
  //   Constants:
  //--------------------------------------------------------------------------
  private static final String CONFIG_FILE_NAME = "lf5_configuration.xml";
  private static final String NAME = "name";
  private static final String PATH = "path";
  private static final String SELECTED = "selected";
  private static final String EXPANDED = "expanded";
  private static final String CATEGORY = "category";
  private static final String FIRST_CATEGORY_NAME = "Categories";
  private static final String LEVEL = "level";
  private static final String COLORLEVEL = "colorlevel";
  private static final String RED = "red";
  private static final String GREEN = "green";
  private static final String BLUE = "blue";
  private static final String COLUMN = "column";
  private static final String NDCTEXTFILTER = "searchtext";
  //--------------------------------------------------------------------------
  //   Protected Variables:
  //--------------------------------------------------------------------------

  //--------------------------------------------------------------------------
  //   Private Variables:
  //--------------------------------------------------------------------------
  private LogBrokerMonitor _monitor = null;
  private LogTable _table = null;

  //--------------------------------------------------------------------------
  //   Constructors:
  //--------------------------------------------------------------------------
  public ConfigurationManager(LogBrokerMonitor monitor, LogTable table) {
    super();
    _monitor = monitor;
    _table = table;
    load();
  }
  //--------------------------------------------------------------------------
  //   Public Methods:
  //--------------------------------------------------------------------------

  public void save() {
    CategoryExplorerModel model = _monitor.getCategoryExplorerTree().getExplorerModel();
    CategoryNode root = model.getRootCategoryNode();

    StringBuffer xml = new StringBuffer(2048);
    openXMLDocument(xml);
    openConfigurationXML(xml);
    processLogRecordFilter(_monitor.getNDCTextFilter(), xml);
    processLogLevels(_monitor.getLogLevelMenuItems(), xml);
    processLogLevelColors(_monitor.getLogLevelMenuItems(),
        LogLevel.getLogLevelColorMap(), xml);
    processLogTableColumns(LogTableColumn.getLogTableColumns(), xml);
    processConfigurationNode(root, xml);
    closeConfigurationXML(xml);
    store(xml.toString());
  }

  public void reset() {
    deleteConfigurationFile();
    collapseTree();
    selectAllNodes();
  }

  public static String treePathToString(TreePath path) {
    // count begins at one so as to not include the 'Categories' - root category
    StringBuffer sb = new StringBuffer();
    CategoryNode n = null;
    Object[] objects = path.getPath();
    for (int i = 1; i < objects.length; i++) {
      n = (CategoryNode) objects[i];
      if (i > 1) {
        sb.append(".");
      }
      sb.append(n.getTitle());
    }
    return sb.toString();
  }

  //--------------------------------------------------------------------------
  //   Protected Methods:
  //--------------------------------------------------------------------------
  protected void load() {
    File file = new File(getFilename());
    if (file.exists()) {
      try {
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.
            newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(file);
        processRecordFilter(doc);
        processCategories(doc);
        processLogLevels(doc);
        processLogLevelColors(doc);
        processLogTableColumns(doc);
      } catch (Exception e) {
        // ignore all error and just continue as if there was no
        // configuration xml file but do report a message
        System.err.println("Unable process configuration file at " +
            getFilename() + ". Error Message=" + e.getMessage());
      }
    }

  }

  // Added in version 1.2 - reads in the NDC text filter from the
  // xml configuration file.  If the value of the filter is not null
  // or an empty string ("") then the manager will set the LogBrokerMonitor's
  // LogRecordFilter to use the NDC LogRecordFilter.  Otherwise, the
  // LogBrokerMonitor will use the default LogRecordFilter.
  protected void processRecordFilter(Document doc) {
    NodeList nodeList = doc.getElementsByTagName(NDCTEXTFILTER);

    // there is only one value stored
    Node n = nodeList.item(0);
    // add check for backwards compatibility  as this feature was added in
    // version 1.2
    if (n == null) {
      return;
    }

    NamedNodeMap map = n.getAttributes();
    String text = getValue(map, NAME);

    if (text == null || text.equals("")) {
      return;
    }
    _monitor.setNDCLogRecordFilter(text);
  }

  protected void processCategories(Document doc) {
    CategoryExplorerTree tree = _monitor.getCategoryExplorerTree();
    CategoryExplorerModel model = tree.getExplorerModel();
    NodeList nodeList = doc.getElementsByTagName(CATEGORY);

    // determine where the starting node is
    NamedNodeMap map = nodeList.item(0).getAttributes();
    int j = (getValue(map, NAME).equalsIgnoreCase(FIRST_CATEGORY_NAME)) ? 1 : 0;
    // iterate backwards throught the nodeList so that expansion of the
    // list can occur
    for (int i = nodeList.getLength() - 1; i >= j; i--) {
      Node n = nodeList.item(i);
      map = n.getAttributes();
      CategoryNode chnode = model.addCategory(new CategoryPath(getValue(map, PATH)));
      chnode.setSelected((getValue(map, SELECTED).equalsIgnoreCase("true")) ? true : false);
      if (getValue(map, EXPANDED).equalsIgnoreCase("true")) ;
      tree.expandPath(model.getTreePathToRoot(chnode));
    }

  }

  protected void processLogLevels(Document doc) {
    NodeList nodeList = doc.getElementsByTagName(LEVEL);
    Map menuItems = _monitor.getLogLevelMenuItems();

    for (int i = 0; i < nodeList.getLength(); i++) {
      Node n = nodeList.item(i);
      NamedNodeMap map = n.getAttributes();
      String name = getValue(map, NAME);
      try {
        JCheckBoxMenuItem item =
            (JCheckBoxMenuItem) menuItems.get(LogLevel.valueOf(name));
        item.setSelected(getValue(map, SELECTED).equalsIgnoreCase("true"));
      } catch (LogLevelFormatException e) {
        // ignore it will be on by default.
      }
    }
  }

  protected void processLogLevelColors(Document doc) {
    NodeList nodeList = doc.getElementsByTagName(COLORLEVEL);
    LogLevel.getLogLevelColorMap();

    for (int i = 0; i < nodeList.getLength(); i++) {
      Node n = nodeList.item(i);
      // check for backwards compatibility since this feature was added
      // in version 1.3
      if (n == null) {
        return;
      }

      NamedNodeMap map = n.getAttributes();
      String name = getValue(map, NAME);
      try {
        LogLevel level = LogLevel.valueOf(name);
        int red = Integer.parseInt(getValue(map, RED));
        int green = Integer.parseInt(getValue(map, GREEN));
        int blue = Integer.parseInt(getValue(map, BLUE));
        Color c = new Color(red, green, blue);
        if (level != null) {
          level.setLogLevelColorMap(level, c);
        }

      } catch (LogLevelFormatException e) {
        // ignore it will be on by default.
      }
    }
  }

  protected void processLogTableColumns(Document doc) {
    NodeList nodeList = doc.getElementsByTagName(COLUMN);
    Map menuItems = _monitor.getLogTableColumnMenuItems();
    List selectedColumns = new ArrayList();
    for (int i = 0; i < nodeList.getLength(); i++) {
      Node n = nodeList.item(i);
      // check for backwards compatibility since this feature was added
      // in version 1.3
      if (n == null) {
        return;
      }
      NamedNodeMap map = n.getAttributes();
      String name = getValue(map, NAME);
      try {
        LogTableColumn column = LogTableColumn.valueOf(name);
        JCheckBoxMenuItem item =
            (JCheckBoxMenuItem) menuItems.get(column);
        item.setSelected(getValue(map, SELECTED).equalsIgnoreCase("true"));

        if (item.isSelected()) {
          selectedColumns.add(column);
        }
      } catch (LogTableColumnFormatException e) {
        // ignore it will be on by default.
      }

      if (selectedColumns.isEmpty()) {
        _table.setDetailedView();
      } else {
        _table.setView(selectedColumns);
      }

    }
  }

  protected String getValue(NamedNodeMap map, String attr) {
    Node n = map.getNamedItem(attr);
    return n.getNodeValue();
  }

  protected void collapseTree() {
    // collapse everything except the first category
    CategoryExplorerTree tree = _monitor.getCategoryExplorerTree();
    for (int i = tree.getRowCount() - 1; i > 0; i--) {
      tree.collapseRow(i);
    }
  }

  protected void selectAllNodes() {
    CategoryExplorerModel model = _monitor.getCategoryExplorerTree().getExplorerModel();
    CategoryNode root = model.getRootCategoryNode();
    Enumeration all = root.breadthFirstEnumeration();
    CategoryNode n = null;
    while (all.hasMoreElements()) {
      n = (CategoryNode) all.nextElement();
      n.setSelected(true);
    }
  }

  protected void store(String s) {

    try {
      PrintWriter writer = new PrintWriter(new FileWriter(getFilename()));
      writer.print(s);
      writer.close();
    } catch (IOException e) {
      // do something with this error.
      e.printStackTrace();
    }

  }

  protected void deleteConfigurationFile() {
    try {
      File f = new File(getFilename());
      if (f.exists()) {
        f.delete();
      }
    } catch (SecurityException e) {
      System.err.println("Cannot delete " + getFilename() +
          " because a security violation occured.");
    }
  }

  protected String getFilename() {
    String home = System.getProperty("user.home");
    String sep = System.getProperty("file.separator");

    return home + sep + "lf5" + sep + CONFIG_FILE_NAME;
  }

  //--------------------------------------------------------------------------
  //   Private Methods:
  //--------------------------------------------------------------------------
  private void processConfigurationNode(CategoryNode node, StringBuffer xml) {
    CategoryExplorerModel model = _monitor.getCategoryExplorerTree().getExplorerModel();

    Enumeration all = node.breadthFirstEnumeration();
    CategoryNode n = null;
    while (all.hasMoreElements()) {
      n = (CategoryNode) all.nextElement();
      exportXMLElement(n, model.getTreePathToRoot(n), xml);
    }

  }

  private void processLogLevels(Map logLevelMenuItems, StringBuffer xml) {
    xml.append("\t<loglevels>\r\n");
    Iterator it = logLevelMenuItems.keySet().iterator();
    while (it.hasNext()) {
      LogLevel level = (LogLevel) it.next();
      JCheckBoxMenuItem item = (JCheckBoxMenuItem) logLevelMenuItems.get(level);
      exportLogLevelXMLElement(level.getLabel(), item.isSelected(), xml);
    }

    xml.append("\t</loglevels>\r\n");
  }

  private void processLogLevelColors(Map logLevelMenuItems, Map logLevelColors, StringBuffer xml) {
    xml.append("\t<loglevelcolors>\r\n");
    // iterate through the list of log levels being used (log4j, jdk1.4, custom levels)
    Iterator it = logLevelMenuItems.keySet().iterator();
    while (it.hasNext()) {
      LogLevel level = (LogLevel) it.next();
      // for each level, get the associated color from the log level color map
      Color color = (Color) logLevelColors.get(level);
      exportLogLevelColorXMLElement(level.getLabel(), color, xml);
    }

    xml.append("\t</loglevelcolors>\r\n");
  }


  private void processLogTableColumns(List logTableColumnMenuItems, StringBuffer xml) {
    xml.append("\t<logtablecolumns>\r\n");
    Iterator it = logTableColumnMenuItems.iterator();
    while (it.hasNext()) {
      LogTableColumn column = (LogTableColumn) it.next();
      JCheckBoxMenuItem item = _monitor.getTableColumnMenuItem(column);
      exportLogTableColumnXMLElement(column.getLabel(), item.isSelected(), xml);
    }

    xml.append("\t</logtablecolumns>\r\n");
  }

  // Added in version 1.2 - stores the NDC text filter in the xml file
  // for future use.
  private void processLogRecordFilter(String text, StringBuffer xml) {
    xml.append("\t<").append(NDCTEXTFILTER).append(" ");
    xml.append(NAME).append("=\"").append(text).append("\"");
    xml.append("/>\r\n");
  }

  private void openXMLDocument(StringBuffer xml) {
    xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n");
  }

  private void openConfigurationXML(StringBuffer xml) {
    xml.append("<configuration>\r\n");
  }

  private void closeConfigurationXML(StringBuffer xml) {
    xml.append("</configuration>\r\n");
  }

  private void exportXMLElement(CategoryNode node, TreePath path, StringBuffer xml) {
    CategoryExplorerTree tree = _monitor.getCategoryExplorerTree();

    xml.append("\t<").append(CATEGORY).append(" ");
    xml.append(NAME).append("=\"").append(node.getTitle()).append("\" ");
    xml.append(PATH).append("=\"").append(treePathToString(path)).append("\" ");
    xml.append(EXPANDED).append("=\"").append(tree.isExpanded(path)).append("\" ");
    xml.append(SELECTED).append("=\"").append(node.isSelected()).append("\"/>\r\n");
  }

  private void exportLogLevelXMLElement(String label, boolean selected, StringBuffer xml) {
    xml.append("\t\t<").append(LEVEL).append(" ").append(NAME);
    xml.append("=\"").append(label).append("\" ");
    xml.append(SELECTED).append("=\"").append(selected);
    xml.append("\"/>\r\n");
  }

  private void exportLogLevelColorXMLElement(String label, Color color, StringBuffer xml) {
    xml.append("\t\t<").append(COLORLEVEL).append(" ").append(NAME);
    xml.append("=\"").append(label).append("\" ");
    xml.append(RED).append("=\"").append(color.getRed()).append("\" ");
    xml.append(GREEN).append("=\"").append(color.getGreen()).append("\" ");
    xml.append(BLUE).append("=\"").append(color.getBlue());
    xml.append("\"/>\r\n");
  }

  private void exportLogTableColumnXMLElement(String label, boolean selected, StringBuffer xml) {
    xml.append("\t\t<").append(COLUMN).append(" ").append(NAME);
    xml.append("=\"").append(label).append("\" ");
    xml.append(SELECTED).append("=\"").append(selected);
    xml.append("\"/>\r\n");
  }
  //--------------------------------------------------------------------------
  //   Nested Top-Level Classes or Interfaces:
  //--------------------------------------------------------------------------

}






