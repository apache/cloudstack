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
package examples.lf5.UsingLogMonitorAdapter;

import org.apache.log4j.lf5.LogLevel;
import org.apache.log4j.lf5.util.LogMonitorAdapter;

/**
 * This class is a simple example of how use the LogMonitorAdapter to
 * bypass the Log4JAppender and post LogRecords directly to the LogMonitor
 *
 * To make this example work, ensure that the lf5.jar and lf5-license.jar
 * files are in your classpath, and then run the example at the command line.
 *
 * @author Richard Hurst
 */

// Contributed by ThoughtWorks Inc.

public class UsingLogMonitorAdapter {
  //--------------------------------------------------------------------------
  //   Constants:
  //--------------------------------------------------------------------------

  //--------------------------------------------------------------------------
  //   Protected Variables:
  //--------------------------------------------------------------------------

  //--------------------------------------------------------------------------
  //   Private Variables:
  //--------------------------------------------------------------------------
  private static LogMonitorAdapter _adapter;

  static {
    _adapter = LogMonitorAdapter.newInstance(LogMonitorAdapter.LOG4J_LOG_LEVELS);
  }
  //--------------------------------------------------------------------------
  //   Constructors:
  //--------------------------------------------------------------------------

  //--------------------------------------------------------------------------
  //   Public Methods:
  //--------------------------------------------------------------------------

  public static void main(String[] args) {
    UsingLogMonitorAdapter test = new UsingLogMonitorAdapter();
    test.doMyBidding();
  }

  public void doMyBidding() {
    String logger = this.getClass().getName();

    // will default to debug log level
    _adapter.log(logger, "Doh this is a debugging");

    _adapter.log(logger, LogLevel.INFO, "Hmmm fobidden doughnut");
    _adapter.log(logger, LogLevel.WARN, "Danger Danger Will Robinson",
        new RuntimeException("DANGER"), "32");
    _adapter.log(logger, LogLevel.ERROR, "Exit stage right->");
    _adapter.log(logger, LogLevel.FATAL, "What's up Doc?",
        new NullPointerException("Unfortunate exception"));
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





