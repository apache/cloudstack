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

import org.apache.log4j.lf5.viewer.LogBrokerMonitor;

/**
 * Starts an instance of the LogFactor5 console for off-line viewing.
 *
 * @author Brad Marlborough
 * @author Richard Hurst
 */

// Contributed by ThoughtWorks Inc.

public class StartLogFactor5 {
  //--------------------------------------------------------------------------
  //   Constants:
  //--------------------------------------------------------------------------

  //--------------------------------------------------------------------------
  //   Protected Variables:
  //--------------------------------------------------------------------------

  //--------------------------------------------------------------------------
  //   Private Variables:
  //--------------------------------------------------------------------------

  //--------------------------------------------------------------------------
  //   Constructors:
  //--------------------------------------------------------------------------

  //--------------------------------------------------------------------------
  //   Public Methods:
  //--------------------------------------------------------------------------

  /**
   * Main - starts a an instance of the LogFactor5 console and configures
   * the console settings.
   */
  public final static void main(String[] args) {

    LogBrokerMonitor monitor = new LogBrokerMonitor(
        LogLevel.getLog4JLevels());

    monitor.setFrameSize(LF5Appender.getDefaultMonitorWidth(),
        LF5Appender.getDefaultMonitorHeight());
    monitor.setFontSize(12);
    monitor.show();

  }

  //--------------------------------------------------------------------------
  //   Protected Methods:
  //--------------------------------------------------------------------------

  //--------------------------------------------------------------------------
  //   Private Methods:
  //--------------------------------------------------------------------------

  //--------------------------------------------------------------------------
  //   Nested Top-Level Classes or Interfaces
  //--------------------------------------------------------------------------

}


