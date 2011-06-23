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

package org.apache.log4j.lf5;

import java.awt.Toolkit;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.lf5.viewer.LogBrokerMonitor;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;

/**
 * <code>LF5Appender</code> logs events to a swing based logging
 * console. The swing console supports turning categories on and off,
 * multiple detail level views, as well as full text searching and many
 * other capabilties.
 *
 * @author Brent Sprecher
 */

// Contributed by ThoughtWorks Inc.

public class LF5Appender extends AppenderSkeleton {
  //--------------------------------------------------------------------------
  // Constants:
  //--------------------------------------------------------------------------

  //--------------------------------------------------------------------------
  // Protected Variables:
  //--------------------------------------------------------------------------

  protected LogBrokerMonitor _logMonitor;
  protected static LogBrokerMonitor _defaultLogMonitor;
  protected static AppenderFinalizer _finalizer;

  //--------------------------------------------------------------------------
  // Private Variables:
  //--------------------------------------------------------------------------

  //--------------------------------------------------------------------------
  // Constructors:
  //--------------------------------------------------------------------------

  /**
   * Constructs a <code>LF5Appender</code> using the default instance of
   * the <code>LogBrokerMonitor</code>. This constructor should <bold>always
   * </bold> be  preferred over the
   * <code>LF5Appender(LogBrokerMonitor monitor)</code>
   * constructor, unless you need to spawn additional log monitoring
   * windows.
   */
  public LF5Appender() {
    this(getDefaultInstance());
  }

  /**
   * Constructs a <code>LF5Appender<code> using an instance of
   * a <code>LogBrokerMonitor<code> supplied by the user. This
   * constructor should only be used when you need to spawn
   * additional log monitoring windows.
   *
   * @param monitor An instance of a <code>LogBrokerMonitor<code>
   * created by the user.
   */
  public LF5Appender(LogBrokerMonitor monitor) {

    if (monitor != null) {
      _logMonitor = monitor;
    }
  }

  //--------------------------------------------------------------------------
  // Public Methods:
  //--------------------------------------------------------------------------

  /**
   * Appends a <code>LoggingEvent</code> record to the
   * <code>LF5Appender</code>.
   * @param event The <code>LoggingEvent</code>
   * to be appended.
   */
  public void append(LoggingEvent event) {
    // Retrieve the information from the log4j LoggingEvent.
    String category = event.getLoggerName();
    String logMessage = event.getRenderedMessage();
    String nestedDiagnosticContext = event.getNDC();
    String threadDescription = event.getThreadName();
    String level = event.getLevel().toString();
    long time = event.timeStamp;
    LocationInfo locationInfo = event.getLocationInformation();

    // Add the logging event information to a LogRecord
    Log4JLogRecord record = new Log4JLogRecord();

    record.setCategory(category);
    record.setMessage(logMessage);
    record.setLocation(locationInfo.fullInfo);
    record.setMillis(time);
    record.setThreadDescription(threadDescription);

    if (nestedDiagnosticContext != null) {
      record.setNDC(nestedDiagnosticContext);
    } else {
      record.setNDC("");
    }

    if (event.getThrowableInformation() != null) {
      record.setThrownStackTrace(event.getThrowableInformation());
    }

    try {
      record.setLevel(LogLevel.valueOf(level));
    } catch (LogLevelFormatException e) {
      // If the priority level doesn't match one of the predefined
      // log levels, then set the level to warning.
      record.setLevel(LogLevel.WARN);
    }

    if (_logMonitor != null) {
      _logMonitor.addMessage(record);
    }
  }

  /**
   * This method is an empty implementation of the close() method inherited
   * from the <code>org.apache.log4j.Appender</code> interface.
   */
  public void close() {
  }

  /**
   * Returns a value that indicates whether this appender requires a
   * <code>Layout</code>. This method always returns false.
   * No layout is required for the <code>LF5Appender</code>.
   */
  public boolean requiresLayout() {
    return false;
  }

  /**
   * This method is used to set the property that controls whether
   * the <code>LogBrokerMonitor</code> is hidden or closed when a user
   * exits
   * the monitor. By default, the <code>LogBrokerMonitor</code> will hide
   * itself when the log window is exited, and the swing thread will
   * continue to run in the background. If this property is
   * set to true, the <code>LogBrokerMonitor</code> will call System.exit(0)
   * and will shut down swing thread and the virtual machine.
   *
   * @param callSystemExitOnClose A boolean value indicating whether
   * to call System.exit(0) when closing the log window.
   */
  public void setCallSystemExitOnClose(boolean callSystemExitOnClose) {
    _logMonitor.setCallSystemExitOnClose(callSystemExitOnClose);
  }

  /**
   * The equals method compares two LF5Appenders and determines whether
   * they are equal. Two <code>Appenders</code> will be considered equal
   * if, and only if, they both contain references to the same <code>
   * LogBrokerMonitor</code>.
   *
   * @param compareTo A boolean value indicating whether
   * the two LF5Appenders are equal.
   */
  public boolean equals(LF5Appender compareTo) {
    // If both reference the same LogBrokerMonitor, they are equal.
    return _logMonitor == compareTo.getLogBrokerMonitor();
  }

  public LogBrokerMonitor getLogBrokerMonitor() {
    return _logMonitor;
  }

  public static void main(String[] args) {
    new LF5Appender();
  }

  public void setMaxNumberOfRecords(int maxNumberOfRecords) {
    _defaultLogMonitor.setMaxNumberOfLogRecords(maxNumberOfRecords);
  }
  //--------------------------------------------------------------------------
  // Protected Methods:
  //--------------------------------------------------------------------------

  /**
   * @return The default instance of the <code>LogBrokerMonitor</code>.
   */
  protected static synchronized LogBrokerMonitor getDefaultInstance() {
    if (_defaultLogMonitor == null) {
      try {
        _defaultLogMonitor =
            new LogBrokerMonitor(LogLevel.getLog4JLevels());
        _finalizer = new AppenderFinalizer(_defaultLogMonitor);

        _defaultLogMonitor.setFrameSize(getDefaultMonitorWidth(),
            getDefaultMonitorHeight());
        _defaultLogMonitor.setFontSize(12);
        _defaultLogMonitor.show();

      } catch (SecurityException e) {
        _defaultLogMonitor = null;
      }
    }

    return _defaultLogMonitor;
  }

  /**
   * @return the screen width from Toolkit.getScreenSize()
   * if possible, otherwise returns 800
   * @see java.awt.Toolkit
   */
  protected static int getScreenWidth() {
    try {
      return Toolkit.getDefaultToolkit().getScreenSize().width;
    } catch (Throwable t) {
      return 800;
    }
  }

  /**
   * @return the screen height from Toolkit.getScreenSize()
   * if possible, otherwise returns 600
   * @see java.awt.Toolkit
   */
  protected static int getScreenHeight() {
    try {
      return Toolkit.getDefaultToolkit().getScreenSize().height;
    } catch (Throwable t) {
      return 600;
    }
  }

  protected static int getDefaultMonitorWidth() {
    return (3 * getScreenWidth()) / 4;
  }

  protected static int getDefaultMonitorHeight() {
    return (3 * getScreenHeight()) / 4;
  }
  //--------------------------------------------------------------------------
  // Private Methods:
  //--------------------------------------------------------------------------


  //--------------------------------------------------------------------------
  // Nested Top-Level Classes or Interfaces:
  //--------------------------------------------------------------------------

}
