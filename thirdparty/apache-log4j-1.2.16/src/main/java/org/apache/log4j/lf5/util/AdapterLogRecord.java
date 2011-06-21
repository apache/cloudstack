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

import org.apache.log4j.lf5.LogLevel;
import org.apache.log4j.lf5.LogRecord;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * <p>A LogRecord to be used with the LogMonitorAdapter</p>
 *
 * @author Richard Hurst
 */

// Contributed by ThoughtWorks Inc.

public class AdapterLogRecord extends LogRecord {
  //--------------------------------------------------------------------------
  //   Constants:
  //--------------------------------------------------------------------------

  //--------------------------------------------------------------------------
  //   Protected Variables:
  //--------------------------------------------------------------------------

  //--------------------------------------------------------------------------
  //   Private Variables:
  //--------------------------------------------------------------------------
  private static LogLevel severeLevel = null;

  private static StringWriter sw = new StringWriter();
  private static PrintWriter pw = new PrintWriter(sw);

  //--------------------------------------------------------------------------
  //   Constructors:
  //--------------------------------------------------------------------------
  public AdapterLogRecord() {
    super();
  }

  //--------------------------------------------------------------------------
  //   Public Methods:
  //--------------------------------------------------------------------------
  public void setCategory(String category) {
    super.setCategory(category);
    super.setLocation(getLocationInfo(category));
  }

  public boolean isSevereLevel() {
    if (severeLevel == null) return false;
    return severeLevel.equals(getLevel());
  }

  public static void setSevereLevel(LogLevel level) {
    severeLevel = level;
  }

  public static LogLevel getSevereLevel() {
    return severeLevel;
  }

  //--------------------------------------------------------------------------
  //   Protected Methods:
  //--------------------------------------------------------------------------
  protected String getLocationInfo(String category) {
    String stackTrace = stackTraceToString(new Throwable());
    String line = parseLine(stackTrace, category);
    return line;
  }

  protected String stackTraceToString(Throwable t) {
    String s = null;

    synchronized (sw) {
      t.printStackTrace(pw);
      s = sw.toString();
      sw.getBuffer().setLength(0);
    }

    return s;
  }

  protected String parseLine(String trace, String category) {
    int index = trace.indexOf(category);
    if (index == -1) return null;
    trace = trace.substring(index);
    trace = trace.substring(0, trace.indexOf(")") + 1);
    return trace;
  }
  //--------------------------------------------------------------------------
  //   Private Methods:
  //--------------------------------------------------------------------------

  //--------------------------------------------------------------------------
  //   Nested Top-Level Classes or Interfaces
  //--------------------------------------------------------------------------
}

