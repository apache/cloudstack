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

import org.apache.log4j.lf5.LogRecord;
import org.apache.log4j.lf5.LogRecordFilter;
import org.apache.log4j.lf5.PassingLogRecordFilter;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;


/**
 * A TableModel for LogRecords which includes filtering support.
 *
 * @author Richard Wan
 * @author Brent Sprecher
 */

// Contributed by ThoughtWorks Inc.

public class FilteredLogTableModel
    extends AbstractTableModel {
  //--------------------------------------------------------------------------
  //   Constants:
  //--------------------------------------------------------------------------

  //--------------------------------------------------------------------------
  //   Protected Variables:
  //--------------------------------------------------------------------------

  protected LogRecordFilter _filter = new PassingLogRecordFilter();
  protected List _allRecords = new ArrayList();
  protected List _filteredRecords;
  protected int _maxNumberOfLogRecords = 5000;
  protected String[] _colNames = {"Date",
                                  "Thread",
                                  "Message #",
                                  "Level",
                                  "NDC",
                                  "Category",
                                  "Message",
                                  "Location",
                                  "Thrown"};

  //--------------------------------------------------------------------------
  //   Private Variables:
  //--------------------------------------------------------------------------

  //--------------------------------------------------------------------------
  //   Constructors:
  //--------------------------------------------------------------------------

  public FilteredLogTableModel() {
    super();
  }

  //--------------------------------------------------------------------------
  //   Public Methods:
  //--------------------------------------------------------------------------

  public void setLogRecordFilter(LogRecordFilter filter) {
    _filter = filter;
  }

  public LogRecordFilter getLogRecordFilter() {
    return _filter;
  }

  public String getColumnName(int i) {
    return _colNames[i];
  }

  public int getColumnCount() {
    return _colNames.length;
  }

  public int getRowCount() {
    return getFilteredRecords().size();
  }

  public int getTotalRowCount() {
    return _allRecords.size();
  }

  public Object getValueAt(int row, int col) {
    LogRecord record = getFilteredRecord(row);
    return getColumn(col, record);
  }

  public void setMaxNumberOfLogRecords(int maxNumRecords) {
    if (maxNumRecords > 0) {
      _maxNumberOfLogRecords = maxNumRecords;
    }

  }

  public synchronized boolean addLogRecord(LogRecord record) {

    _allRecords.add(record);

    if (_filter.passes(record) == false) {
      return false;
    }
    getFilteredRecords().add(record);
    fireTableRowsInserted(getRowCount(), getRowCount());
    trimRecords();
    return true;
  }

  /**
   * Forces the LogTableModel to requery its filters to determine
   * which records to display.
   */
  public synchronized void refresh() {
    _filteredRecords = createFilteredRecordsList();
    fireTableDataChanged();
  }

  public synchronized void fastRefresh() {
    _filteredRecords.remove(0);
    fireTableRowsDeleted(0, 0);
  }


  /**
   * Clears all records from the LogTableModel
   */
  public synchronized void clear() {
    _allRecords.clear();
    _filteredRecords.clear();
    fireTableDataChanged();
  }

  //--------------------------------------------------------------------------
  //   Protected Methods:
  //--------------------------------------------------------------------------

  protected List getFilteredRecords() {
    if (_filteredRecords == null) {
      refresh();
    }
    return _filteredRecords;
  }

  protected List createFilteredRecordsList() {
    List result = new ArrayList();
    Iterator records = _allRecords.iterator();
    LogRecord current;
    while (records.hasNext()) {
      current = (LogRecord) records.next();
      if (_filter.passes(current)) {
        result.add(current);
      }
    }
    return result;
  }

  protected LogRecord getFilteredRecord(int row) {
    List records = getFilteredRecords();
    int size = records.size();
    if (row < size) {
      return (LogRecord) records.get(row);
    }
    // a minor problem has happened. JTable has asked for
    // a row outside the bounds, because the size of
    // _filteredRecords has changed while it was looping.
    // return the last row.
    return (LogRecord) records.get(size - 1);

  }

  protected Object getColumn(int col, LogRecord lr) {
    if (lr == null) {
      return "NULL Column";
    }
    String date = new Date(lr.getMillis()).toString();
    switch (col) {
      case 0:
        return date + " (" + lr.getMillis() + ")";
      case 1:
        return lr.getThreadDescription();
      case 2:
        return new Long(lr.getSequenceNumber());
      case 3:
        return lr.getLevel();
      case 4:
        return lr.getNDC();
      case 5:
        return lr.getCategory();
      case 6:
        return lr.getMessage();
      case 7:
        return lr.getLocation();
      case 8:
        return lr.getThrownStackTrace();
      default:
        String message = "The column number " + col + "must be between 0 and 8";
        throw new IllegalArgumentException(message);
    }
  }

  // We don't want the amount of rows to grow without bound,
  // leading to a out-of-memory-exception.  Especially not good
  // in a production environment :)

  // This method & clearLogRecords() are synchronized so we don't
  // delete rows that don't exist.
  protected void trimRecords() {
    if (needsTrimming()) {
      trimOldestRecords();
    }
  }

  protected boolean needsTrimming() {
    return (_allRecords.size() > _maxNumberOfLogRecords);
  }

  protected void trimOldestRecords() {
    synchronized (_allRecords) {
      int trim = numberOfRecordsToTrim();
      if (trim > 1) {
        List oldRecords =
            _allRecords.subList(0, trim);
        oldRecords.clear();
        refresh();
      } else {
        _allRecords.remove(0);
        fastRefresh();
      }
    }

  }

  //--------------------------------------------------------------------------
  //   Private Methods:
  //--------------------------------------------------------------------------
  private int numberOfRecordsToTrim() {
    return _allRecords.size() - _maxNumberOfLogRecords;
  }

  //--------------------------------------------------------------------------
  //   Nested Top-Level Classes or Interfaces
  //--------------------------------------------------------------------------
}

