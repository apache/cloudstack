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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LogTableColumn
 *
 * @author Michael J. Sikorsky
 * @author Brad Marlborough
 */

// Contributed by ThoughtWorks Inc.

public class LogTableColumn implements java.io.Serializable {
  private static final long serialVersionUID = -4275827753626456547L;

  // log4j table columns.
  public final static LogTableColumn DATE = new LogTableColumn("Date");
  public final static LogTableColumn THREAD = new LogTableColumn("Thread");
  public final static LogTableColumn MESSAGE_NUM = new LogTableColumn("Message #");
  public final static LogTableColumn LEVEL = new LogTableColumn("Level");
  public final static LogTableColumn NDC = new LogTableColumn("NDC");
  public final static LogTableColumn CATEGORY = new LogTableColumn("Category");
  public final static LogTableColumn MESSAGE = new LogTableColumn("Message");
  public final static LogTableColumn LOCATION = new LogTableColumn("Location");
  public final static LogTableColumn THROWN = new LogTableColumn("Thrown");


  //--------------------------------------------------------------------------
  //   Protected Variables:
  //--------------------------------------------------------------------------
  protected String _label;

  //--------------------------------------------------------------------------
  //   Private Variables:
  //--------------------------------------------------------------------------
  private static LogTableColumn[] _log4JColumns;
  private static Map _logTableColumnMap;

  //--------------------------------------------------------------------------
  //   Constructors:
  //--------------------------------------------------------------------------
  static {
    _log4JColumns = new LogTableColumn[]{DATE, THREAD, MESSAGE_NUM, LEVEL, NDC, CATEGORY,
                                         MESSAGE, LOCATION, THROWN};

    _logTableColumnMap = new HashMap();

    for (int i = 0; i < _log4JColumns.length; i++) {
      _logTableColumnMap.put(_log4JColumns[i].getLabel(), _log4JColumns[i]);
    }
  }


  public LogTableColumn(String label) {
    _label = label;
  }

  //--------------------------------------------------------------------------
  //   Public Methods:
  //--------------------------------------------------------------------------

  /**
   * Return the Label of the LogLevel.
   */
  public String getLabel() {
    return _label;
  }

  /**
   * Convert a column label into a LogTableColumn object.
   *
   * @param column The label of a level to be converted into a LogTableColumn.
   * @return LogTableColumn The LogTableColumn with a label equal to column.
   * @throws LogTableColumnFormatException Is thrown when the column can not be
   *         converted into a LogTableColumn.
   */
  public static LogTableColumn valueOf(String column)
      throws LogTableColumnFormatException {
    LogTableColumn tableColumn = null;
    if (column != null) {
      column = column.trim();
      tableColumn = (LogTableColumn) _logTableColumnMap.get(column);
    }

    if (tableColumn == null) {
      StringBuffer buf = new StringBuffer();
      buf.append("Error while trying to parse (" + column + ") into");
      buf.append(" a LogTableColumn.");
      throw new LogTableColumnFormatException(buf.toString());
    }
    return tableColumn;
  }


  public boolean equals(Object o) {
    boolean equals = false;

    if (o instanceof LogTableColumn) {
      if (this.getLabel() ==
          ((LogTableColumn) o).getLabel()) {
        equals = true;
      }
    }

    return equals;
  }

  public int hashCode() {
    return _label.hashCode();
  }

  public String toString() {
    return _label;
  }

  /**
   * @return A <code>List</code> of <code>LogTableColumn/code> objects that map
   * to log4j <code>Column</code> objects.
   */
  public static List getLogTableColumns() {
    return Arrays.asList(_log4JColumns);
  }

  public static LogTableColumn[] getLogTableColumnArray() {
    return _log4JColumns;
  }

  //--------------------------------------------------------------------------
  //   Protected Methods:
  //--------------------------------------------------------------------------

  //--------------------------------------------------------------------------
  //   Private Methods:
  //--------------------------------------------------------------------------

  //--------------------------------------------------------------------------
  //   Nested Top-Level Classes or Interfaces:
  //--------------------------------------------------------------------------

}






