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

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.EventObject;

import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.TreePath;

/**
 * CategoryImmediateEditor
 *
 * @author Michael J. Sikorsky
 * @author Robert Shaw
 */

// Contributed by ThoughtWorks Inc.

public class CategoryImmediateEditor extends DefaultTreeCellEditor {
  //--------------------------------------------------------------------------
  //   Constants:
  //--------------------------------------------------------------------------

  //--------------------------------------------------------------------------
  //   Protected Variables:
  //--------------------------------------------------------------------------
  private CategoryNodeRenderer renderer;
  protected Icon editingIcon = null;

  //--------------------------------------------------------------------------
  //   Private Variables:
  //--------------------------------------------------------------------------

  //--------------------------------------------------------------------------
  //   Constructors:
  //--------------------------------------------------------------------------
  public CategoryImmediateEditor(JTree tree,
      CategoryNodeRenderer renderer,
      CategoryNodeEditor editor) {
    super(tree, renderer, editor);
    this.renderer = renderer;
    renderer.setIcon(null);
    renderer.setLeafIcon(null);
    renderer.setOpenIcon(null);
    renderer.setClosedIcon(null);

    super.editingIcon = null;
  }

  //--------------------------------------------------------------------------
  //   Public Methods:
  //--------------------------------------------------------------------------
  public boolean shouldSelectCell(EventObject e) {
    boolean rv = false;  // only mouse events

    if (e instanceof MouseEvent) {
      MouseEvent me = (MouseEvent) e;
      TreePath path = tree.getPathForLocation(me.getX(),
          me.getY());
      CategoryNode node = (CategoryNode)
          path.getLastPathComponent();

      rv = node.isLeaf() /*|| !inCheckBoxHitRegion(me)*/;
    }
    return rv;
  }

  public boolean inCheckBoxHitRegion(MouseEvent e) {
    TreePath path = tree.getPathForLocation(e.getX(),
        e.getY());
    if (path == null) {
      return false;
    }
    CategoryNode node = (CategoryNode) path.getLastPathComponent();
    boolean rv = false;

    if (true) {
      // offset and lastRow DefaultTreeCellEditor
      // protected members

      Rectangle bounds = tree.getRowBounds(lastRow);
      Dimension checkBoxOffset =
          renderer.getCheckBoxOffset();

      bounds.translate(offset + checkBoxOffset.width,
          checkBoxOffset.height);

      rv = bounds.contains(e.getPoint());
    }
    return true;
  }

  //--------------------------------------------------------------------------
  //   Protected Methods:
  //--------------------------------------------------------------------------

  protected boolean canEditImmediately(EventObject e) {
    boolean rv = false;

    if (e instanceof MouseEvent) {
      MouseEvent me = (MouseEvent) e;
      rv = inCheckBoxHitRegion(me);
    }

    return rv;
  }

  protected void determineOffset(JTree tree, Object value,
      boolean isSelected, boolean expanded,
      boolean leaf, int row) {
    // Very important: means that the tree won't jump around.
    offset = 0;
  }

  //--------------------------------------------------------------------------
  //   Private Methods:
  //--------------------------------------------------------------------------

  //--------------------------------------------------------------------------
  //   Nested Top-Level Classes or Interfaces:
  //--------------------------------------------------------------------------

}






