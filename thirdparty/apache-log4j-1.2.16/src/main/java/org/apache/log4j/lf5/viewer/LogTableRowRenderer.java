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
package org.apache.log4j.lf5.viewer;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.apache.log4j.lf5.LogLevel;
import org.apache.log4j.lf5.LogRecord;

/**
 * LogTableRowRenderer
 *
 * @author Michael J. Sikorsky
 * @author Robert Shaw
 * @author Brad Marlborough
 */

// Contributed by ThoughtWorks Inc.

public class LogTableRowRenderer extends DefaultTableCellRenderer {
  private static final long serialVersionUID = -3951639953706443213L;
  //--------------------------------------------------------------------------
  //   Constants:
  //--------------------------------------------------------------------------

  //--------------------------------------------------------------------------
  //   Protected Variables:
  //--------------------------------------------------------------------------
  protected boolean _highlightFatal = true;
  protected Color _color = new Color(230, 230, 230);

  //--------------------------------------------------------------------------
  //   Private Variables:
  //--------------------------------------------------------------------------

  //--------------------------------------------------------------------------
  //   Constructors:
  //--------------------------------------------------------------------------

  //--------------------------------------------------------------------------
  //   Public Methods:
  //--------------------------------------------------------------------------

  public Component getTableCellRendererComponent(JTable table,
      Object value,
      boolean isSelected,
      boolean hasFocus,
      int row,
      int col) {

    if ((row % 2) == 0) {
      setBackground(_color);
    } else {
      setBackground(Color.white);
    }

    FilteredLogTableModel model = (FilteredLogTableModel) table.getModel();
    LogRecord record = model.getFilteredRecord(row);

    setForeground(getLogLevelColor(record.getLevel()));

    return (super.getTableCellRendererComponent(table,
        value,
        isSelected,
        hasFocus,
        row, col));
  }


  //--------------------------------------------------------------------------
  //   Protected Methods:
  //--------------------------------------------------------------------------
  protected Color getLogLevelColor(LogLevel level) {
    return (Color) LogLevel.getLogLevelColorMap().get(level);
  }

  //--------------------------------------------------------------------------
  //   Private Methods:
  //--------------------------------------------------------------------------

  //--------------------------------------------------------------------------
  //   Nested Top-Level Classes or Interfaces:
  //--------------------------------------------------------------------------

}






