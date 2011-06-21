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

import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The LogLevel class defines a set of standard logging levels.
 *
 * The logging Level objects are ordered and are specified by ordered
 * integers. Enabling logging at a given level also enables logging at all
 * higher levels.
 *
 * @author Michael J. Sikorsky
 * @author Robert Shaw
 * @author Brent Sprecher
 * @author Richard Hurst
 * @author Brad Marlborough
 */

// Contributed by ThoughtWorks Inc.

public class LogLevel implements java.io.Serializable {
  //--------------------------------------------------------------------------
  //   Constants:
  //--------------------------------------------------------------------------

  // log4j log levels.
  public final static LogLevel FATAL = new LogLevel("FATAL", 0);
  public final static LogLevel ERROR = new LogLevel("ERROR", 1);
  public final static LogLevel WARN = new LogLevel("WARN", 2);
  public final static LogLevel INFO = new LogLevel("INFO", 3);
  public final static LogLevel DEBUG = new LogLevel("DEBUG", 4);

  // jdk1.4 log levels NOTE: also includes INFO
  public final static LogLevel SEVERE = new LogLevel("SEVERE", 1);
  public final static LogLevel WARNING = new LogLevel("WARNING", 2);
  public final static LogLevel CONFIG = new LogLevel("CONFIG", 4);
  public final static LogLevel FINE = new LogLevel("FINE", 5);
  public final static LogLevel FINER = new LogLevel("FINER", 6);
  public final static LogLevel FINEST = new LogLevel("FINEST", 7);

  //--------------------------------------------------------------------------
  //   Protected Variables:
  //--------------------------------------------------------------------------
  protected String _label;
  protected int _precedence;
  //--------------------------------------------------------------------------
  //   Private Variables:
  //--------------------------------------------------------------------------
  private static LogLevel[] _log4JLevels;
  private static LogLevel[] _jdk14Levels;
  private static LogLevel[] _allDefaultLevels;
  private static Map _logLevelMap;
  private static Map _logLevelColorMap;
  private static Map _registeredLogLevelMap = new HashMap();

  //--------------------------------------------------------------------------
  //   Constructors:
  //--------------------------------------------------------------------------
  static {
    _log4JLevels = new LogLevel[]{FATAL, ERROR, WARN, INFO, DEBUG};
    _jdk14Levels = new LogLevel[]{SEVERE, WARNING, INFO,
                                  CONFIG, FINE, FINER, FINEST};
    _allDefaultLevels = new LogLevel[]{FATAL, ERROR, WARN, INFO, DEBUG,
                                       SEVERE, WARNING, CONFIG, FINE, FINER, FINEST};

    _logLevelMap = new HashMap();
    for (int i = 0; i < _allDefaultLevels.length; i++) {
      _logLevelMap.put(_allDefaultLevels[i].getLabel(), _allDefaultLevels[i]);
    }

    // prepopulate map with levels and text color of black
    _logLevelColorMap = new HashMap();
    for (int i = 0; i < _allDefaultLevels.length; i++) {
      _logLevelColorMap.put(_allDefaultLevels[i], Color.black);
    }
  }

  public LogLevel(String label, int precedence) {
    _label = label;
    _precedence = precedence;
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
   * Returns true if the level supplied is encompassed by this level.
   * For example, LogLevel.SEVERE encompasses no other LogLevels and
   * LogLevel.FINE encompasses all other LogLevels.  By definition,
   * a LogLevel encompasses itself.
   */
  public boolean encompasses(LogLevel level) {
    if (level.getPrecedence() <= getPrecedence()) {
      return true;
    }

    return false;
  }

  /**
   * Convert a log level label into a LogLevel object.
   *
   * @param level The label of a level to be converted into a LogLevel.
   * @return LogLevel The LogLevel with a label equal to level.
   * @throws LogLevelFormatException Is thrown when the level can not be
   *         converted into a LogLevel.
   */
  public static LogLevel valueOf(String level)
      throws LogLevelFormatException {
    LogLevel logLevel = null;
    if (level != null) {
      level = level.trim().toUpperCase();
      logLevel = (LogLevel) _logLevelMap.get(level);
    }

    // Didn't match, Check for registered LogLevels
    if (logLevel == null && _registeredLogLevelMap.size() > 0) {
      logLevel = (LogLevel) _registeredLogLevelMap.get(level);
    }

    if (logLevel == null) {
      StringBuffer buf = new StringBuffer();
      buf.append("Error while trying to parse (" + level + ") into");
      buf.append(" a LogLevel.");
      throw new LogLevelFormatException(buf.toString());
    }
    return logLevel;
  }

  /**
   * Registers a used defined LogLevel.
   *
   * @param logLevel The log level to be registered. Cannot be a default LogLevel
   * @return LogLevel The replaced log level.
   */
  public static LogLevel register(LogLevel logLevel) {
    if (logLevel == null) return null;

    // ensure that this is not a default log level
    if (_logLevelMap.get(logLevel.getLabel()) == null) {
      return (LogLevel) _registeredLogLevelMap.put(logLevel.getLabel(), logLevel);
    }

    return null;
  }

  public static void register(LogLevel[] logLevels) {
    if (logLevels != null) {
      for (int i = 0; i < logLevels.length; i++) {
        register(logLevels[i]);
      }
    }
  }

  public static void register(List logLevels) {
    if (logLevels != null) {
      Iterator it = logLevels.iterator();
      while (it.hasNext()) {
        register((LogLevel) it.next());
      }
    }
  }

  public boolean equals(Object o) {
    boolean equals = false;

    if (o instanceof LogLevel) {
      if (this.getPrecedence() ==
          ((LogLevel) o).getPrecedence()) {
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

  // set a text color for a specific log level
  public void setLogLevelColorMap(LogLevel level, Color color) {
    // remove the old entry
    _logLevelColorMap.remove(level);
    // add the new color entry
    if (color == null) {
      color = Color.black;
    }
    _logLevelColorMap.put(level, color);
  }

  public static void resetLogLevelColorMap() {
    // empty the map
    _logLevelColorMap.clear();

    // repopulate map and reset text color black
    for (int i = 0; i < _allDefaultLevels.length; i++) {
      _logLevelColorMap.put(_allDefaultLevels[i], Color.black);
    }
  }

  /**
   * @return A <code>List</code> of <code>LogLevel</code> objects that map
   * to log4j <code>Priority</code> objects.
   */
  public static List getLog4JLevels() {
    return Arrays.asList(_log4JLevels);
  }

  public static List getJdk14Levels() {
    return Arrays.asList(_jdk14Levels);
  }

  public static List getAllDefaultLevels() {
    return Arrays.asList(_allDefaultLevels);
  }

  public static Map getLogLevelColorMap() {
    return _logLevelColorMap;
  }

  //--------------------------------------------------------------------------
  //   Protected Methods:
  //--------------------------------------------------------------------------

  protected int getPrecedence() {
    return _precedence;
  }

  //--------------------------------------------------------------------------
  //   Private Methods:
  //--------------------------------------------------------------------------

  //--------------------------------------------------------------------------
  //   Nested Top-Level Classes or Interfaces:
  //--------------------------------------------------------------------------

}






