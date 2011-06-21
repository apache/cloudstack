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
package org.apache.log4j.lf5.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.SwingUtilities;

import org.apache.log4j.lf5.Log4JLogRecord;
import org.apache.log4j.lf5.LogLevel;
import org.apache.log4j.lf5.LogLevelFormatException;
import org.apache.log4j.lf5.LogRecord;
import org.apache.log4j.lf5.viewer.LogBrokerMonitor;
import org.apache.log4j.lf5.viewer.LogFactor5ErrorDialog;
import org.apache.log4j.lf5.viewer.LogFactor5LoadingDialog;

/**
 * Provides utility methods for input and output streams.
 *
 * @author Brad Marlborough
 * @author Richard Hurst
 */

// Contributed by ThoughtWorks Inc.

public class LogFileParser implements Runnable {
  //--------------------------------------------------------------------------
  //   Constants:
  //--------------------------------------------------------------------------
  public static final String RECORD_DELIMITER = "[slf5s.start]";
  public static final String ATTRIBUTE_DELIMITER = "[slf5s.";
  public static final String DATE_DELIMITER = ATTRIBUTE_DELIMITER + "DATE]";
  public static final String THREAD_DELIMITER = ATTRIBUTE_DELIMITER + "THREAD]";
  public static final String CATEGORY_DELIMITER = ATTRIBUTE_DELIMITER + "CATEGORY]";
  public static final String LOCATION_DELIMITER = ATTRIBUTE_DELIMITER + "LOCATION]";
  public static final String MESSAGE_DELIMITER = ATTRIBUTE_DELIMITER + "MESSAGE]";
  public static final String PRIORITY_DELIMITER = ATTRIBUTE_DELIMITER + "PRIORITY]";
  public static final String NDC_DELIMITER = ATTRIBUTE_DELIMITER + "NDC]";

  //--------------------------------------------------------------------------
  //   Protected Variables:
  //--------------------------------------------------------------------------

  //--------------------------------------------------------------------------
  //   Private Variables:
  //--------------------------------------------------------------------------
  private static SimpleDateFormat _sdf = new SimpleDateFormat("dd MMM yyyy HH:mm:ss,S");
  private LogBrokerMonitor _monitor;
  LogFactor5LoadingDialog _loadDialog;
  private InputStream _in = null;

  //--------------------------------------------------------------------------
  //   Constructors:
  //--------------------------------------------------------------------------
  public LogFileParser(File file) throws IOException,
      FileNotFoundException {
    this(new FileInputStream(file));
  }

  public LogFileParser(InputStream stream) throws IOException {
    _in = stream;
  }
  //--------------------------------------------------------------------------
  //   Public Methods:
  //--------------------------------------------------------------------------

  /**
   * Starts a new thread to parse the log file and create a LogRecord.
   * See run().
   * @param monitor LogBrokerMonitor
   */
  public void parse(LogBrokerMonitor monitor) throws RuntimeException {
    _monitor = monitor;
    Thread t = new Thread(this);
    t.start();
  }

  /**
   * Parses the file and creates new log records and adds the record
   * to the monitor.
   */
  public void run() {

    int index = 0;
    int counter = 0;
    LogRecord temp;
    boolean isLogFile = false;

    _loadDialog = new LogFactor5LoadingDialog(
        _monitor.getBaseFrame(), "Loading file...");


    try {
      String logRecords = loadLogFile(_in);

      while ((counter = logRecords.indexOf(RECORD_DELIMITER, index)) != -1) {
        temp = createLogRecord(logRecords.substring(index, counter));
        isLogFile = true;

        if (temp != null) {
          _monitor.addMessage(temp);
        }

        index = counter + RECORD_DELIMITER.length();
      }

      if (index < logRecords.length() && isLogFile) {
        temp = createLogRecord(logRecords.substring(index));

        if (temp != null) {
          _monitor.addMessage(temp);
        }
      }

      if (isLogFile == false) {
        throw new RuntimeException("Invalid log file format");
      }
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          destroyDialog();
        }
      });

    } catch (RuntimeException e) {
      destroyDialog();
      displayError("Error - Invalid log file format.\nPlease see documentation"
          + " on how to load log files.");
    } catch (IOException e) {
      destroyDialog();
      displayError("Error - Unable to load log file!");
    }

    _in = null;
  }

  //--------------------------------------------------------------------------
  //   Protected Methods:
  //--------------------------------------------------------------------------
  protected void displayError(String message) {
    LogFactor5ErrorDialog error = new LogFactor5ErrorDialog(
        _monitor.getBaseFrame(), message);

  }

  //--------------------------------------------------------------------------
  //   Private Methods:
  //--------------------------------------------------------------------------
  private void destroyDialog() {
    _loadDialog.hide();
    _loadDialog.dispose();
  }

  /**
   * Loads a log file from a web server into the LogFactor5 GUI.
   */
  private String loadLogFile(InputStream stream) throws IOException {
    BufferedInputStream br = new BufferedInputStream(stream);

    int count = 0;
    int size = br.available();

    StringBuffer sb = null;
    if (size > 0) {
      sb = new StringBuffer(size);
    } else {
      sb = new StringBuffer(1024);
    }

    while ((count = br.read()) != -1) {
      sb.append((char) count);
    }

    br.close();
    br = null;
    return sb.toString();

  }

  private String parseAttribute(String name, String record) {

    int index = record.indexOf(name);

    if (index == -1) {
      return null;
    }

    return getAttribute(index, record);
  }

  private long parseDate(String record) {
    try {
      String s = parseAttribute(DATE_DELIMITER, record);

      if (s == null) {
        return 0;
      }

      Date d = _sdf.parse(s);

      return d.getTime();
    } catch (ParseException e) {
      return 0;
    }
  }

  private LogLevel parsePriority(String record) {
    String temp = parseAttribute(PRIORITY_DELIMITER, record);

    if (temp != null) {
      try {
        return LogLevel.valueOf(temp);
      } catch (LogLevelFormatException e) {
        return LogLevel.DEBUG;
      }

    }

    return LogLevel.DEBUG;
  }

  private String parseThread(String record) {
    return parseAttribute(THREAD_DELIMITER, record);
  }

  private String parseCategory(String record) {
    return parseAttribute(CATEGORY_DELIMITER, record);
  }

  private String parseLocation(String record) {
    return parseAttribute(LOCATION_DELIMITER, record);
  }

  private String parseMessage(String record) {
    return parseAttribute(MESSAGE_DELIMITER, record);
  }

  private String parseNDC(String record) {
    return parseAttribute(NDC_DELIMITER, record);
  }

  private String parseThrowable(String record) {
    return getAttribute(record.length(), record);
  }

  private LogRecord createLogRecord(String record) {
    if (record == null || record.trim().length() == 0) {
      return null;
    }

    LogRecord lr = new Log4JLogRecord();
    lr.setMillis(parseDate(record));
    lr.setLevel(parsePriority(record));
    lr.setCategory(parseCategory(record));
    lr.setLocation(parseLocation(record));
    lr.setThreadDescription(parseThread(record));
    lr.setNDC(parseNDC(record));
    lr.setMessage(parseMessage(record));
    lr.setThrownStackTrace(parseThrowable(record));

    return lr;
  }


  private String getAttribute(int index, String record) {
    int start = record.lastIndexOf(ATTRIBUTE_DELIMITER, index - 1);

    if (start == -1) {
      return record.substring(0, index);
    }

    start = record.indexOf("]", start);

    return record.substring(start + 1, index).trim();
  }
  //--------------------------------------------------------------------------
  //   Nested Top-Level Classes or Interfaces
  //--------------------------------------------------------------------------

}
