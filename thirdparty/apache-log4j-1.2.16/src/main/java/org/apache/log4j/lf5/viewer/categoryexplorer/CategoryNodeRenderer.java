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
package org.apache.log4j.lf5.viewer.categoryexplorer;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.net.URL;

/**
 * CategoryNodeRenderer
 *
 * @author Michael J. Sikorsky
 * @author Robert Shaw
 */

// Contributed by ThoughtWorks Inc.

public class CategoryNodeRenderer extends DefaultTreeCellRenderer {
  private static final long serialVersionUID = -6046702673278595048L;

  //--------------------------------------------------------------------------
  //   Constants:
  //--------------------------------------------------------------------------

  public static final Color FATAL_CHILDREN = new Color(189, 113, 0);

  //--------------------------------------------------------------------------
  //   Protected Variables:
  //--------------------------------------------------------------------------
  protected JCheckBox _checkBox = new JCheckBox();
  protected JPanel _panel = new JPanel();
  protected static ImageIcon _sat = null;
//   protected JLabel              _label  = new JLabel();

  //--------------------------------------------------------------------------
  //   Private Variables:
  //--------------------------------------------------------------------------

  //--------------------------------------------------------------------------
  //   Constructors:
  //--------------------------------------------------------------------------
  public CategoryNodeRenderer() {
    _panel.setBackground(UIManager.getColor("Tree.textBackground"));

    if (_sat == null) {
      // Load the satellite image.
      String resource =
          "/org/apache/log4j/lf5/viewer/images/channelexplorer_satellite.gif";
      URL satURL = getClass().getResource(resource);

      _sat = new ImageIcon(satURL);
    }

    setOpaque(false);
    _checkBox.setOpaque(false);
    _panel.setOpaque(false);

    // The flowlayout set to LEFT is very important so that the editor
    // doesn't jump around.
    _panel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
    _panel.add(_checkBox);
    _panel.add(this);

    setOpenIcon(_sat);
    setClosedIcon(_sat);
    setLeafIcon(_sat);
  }

  //--------------------------------------------------------------------------
  //   Public Methods:
  //--------------------------------------------------------------------------
  public Component getTreeCellRendererComponent(
      JTree tree, Object value,
      boolean selected, boolean expanded,
      boolean leaf, int row,
      boolean hasFocus) {

    CategoryNode node = (CategoryNode) value;
    //FileNode node = (FileNode)value;
    //String s = tree.convertValueToText(value, selected,
    //						   expanded, leaf, row, hasFocus);

    super.getTreeCellRendererComponent(
        tree, value, selected, expanded,
        leaf, row, hasFocus);

    if (row == 0) {
      // Root row -- no check box
      _checkBox.setVisible(false);
    } else {
      _checkBox.setVisible(true);
      _checkBox.setSelected(node.isSelected());
    }
    String toolTip = buildToolTip(node);
    _panel.setToolTipText(toolTip);
    if (node.hasFatalChildren()) {
      this.setForeground(FATAL_CHILDREN);
    }
    if (node.hasFatalRecords()) {
      this.setForeground(Color.red);
    }

    return _panel;
  }

  public Dimension getCheckBoxOffset() {
    return new Dimension(0, 0);
  }

  //--------------------------------------------------------------------------
  //   Protected Methods:
  //--------------------------------------------------------------------------

  protected String buildToolTip(CategoryNode node) {
    StringBuffer result = new StringBuffer();
    result.append(node.getTitle()).append(" contains a total of ");
    result.append(node.getTotalNumberOfRecords());
    result.append(" LogRecords.");
    result.append(" Right-click for more info.");
    return result.toString();
  }
  //--------------------------------------------------------------------------
  //   Private Methods:
  //--------------------------------------------------------------------------

  //--------------------------------------------------------------------------
  //   Nested Top-Level Classes or Interfaces:
  //--------------------------------------------------------------------------

}






