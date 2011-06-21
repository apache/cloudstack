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
 * using customized LogLevels
 *
 * To make this example work, ensure that the lf5.jar and lf5-license.jar
 * files are in your classpath, and then run the example at the command line.
 *
 * @author Richard Hurst
 */

// Contributed by ThoughtWorks Inc.

public class CustomizedLogLevels {
    //--------------------------------------------------------------------------
    //   Constants:
    //--------------------------------------------------------------------------
    public final static LogLevel LEVEL_ONE = new LogLevel("LEVEL 1", 1);
    public final static LogLevel LEVEL_TWO = new LogLevel("LEVEL 2", 2);
    public final static LogLevel LEVEL_THREE = new LogLevel("LEVEL 3", 3);
    public final static LogLevel LEVEL_FOUR = new LogLevel("LEVEL 4", 4);
    public final static LogLevel DEFAULT = new LogLevel("DEFAULT", 0);

    //--------------------------------------------------------------------------
    //   Protected Variables:
    //--------------------------------------------------------------------------

    //--------------------------------------------------------------------------
    //   Private Variables:
    //--------------------------------------------------------------------------
    private static LogMonitorAdapter _adapter;

    static {
        // The first LogLevel in the Array will be used as the default LogLevel.
        _adapter = LogMonitorAdapter.newInstance(new LogLevel[]{DEFAULT, LEVEL_ONE,
                                                                LEVEL_TWO, LEVEL_THREE, LEVEL_FOUR, LogLevel.FATAL});
        // if a different log level is to be used it can be specified as such
        // _adapter.setDefaultLevel(LEVEL_THREE);
    }
    //--------------------------------------------------------------------------
    //   Constructors:
    //--------------------------------------------------------------------------

    //--------------------------------------------------------------------------
    //   Public Methods:
    //--------------------------------------------------------------------------

    public static void main(String[] args) {
        CustomizedLogLevels test = new CustomizedLogLevels();
        test.doMyBidding();
    }

    public void doMyBidding() {
        // tell the LogMonitorAdapter which LogLevel is the severe Level if necessary
        _adapter.setSevereLevel(LEVEL_ONE);

        String levels = this.getClass().getName();

        // will used the default Level
        _adapter.log(levels, "Using the customized LogLevels");

        _adapter.log(levels, LEVEL_FOUR, "This is a test");
        _adapter.log(levels, LEVEL_THREE, "Hmmm fobidden doughnut");
        _adapter.log(levels, LEVEL_ONE, "Danger Danger Will Robinson",
                new RuntimeException("DANGER"), "32");
        _adapter.log(levels, LEVEL_TWO, "Exit stage right->");
        _adapter.log(levels, LEVEL_FOUR, "What's up Doc?",
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





