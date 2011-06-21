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
package examples.lf5.InitUsingPropertiesFile;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.IOException;
import java.net.URL;

/**
 * This is another simple example of how to use the LogFactor5
 * logging console.
 *
 * The LF5Appender is the primary class that enables logging to the
 * LogFactor5 logging window. If the following line is added to a properties
 * file, the LF5Appender will be appended to the root category when
 * the properties file is loaded:
 *
 *    log4j.appender.A1=org.apache.log4j.lf5.LF5Appender
 *
 * To make this example work, you must ensure that the example.properties file
 * is in your classpath.You can then run the example at the command line.
 *
 * @author Brent Sprecher
 */

// Contributed by ThoughtWorks Inc.

public class InitUsingPropertiesFile {
    //--------------------------------------------------------------------------
    //   Constants:
    //--------------------------------------------------------------------------

    //--------------------------------------------------------------------------
    //   Protected Variables:
    //--------------------------------------------------------------------------

    //--------------------------------------------------------------------------
    //   Private Variables:
    //--------------------------------------------------------------------------

    private static Logger logger =
            Logger.getLogger(InitUsingPropertiesFile.class);

    //--------------------------------------------------------------------------
    //   Constructors:
    //--------------------------------------------------------------------------

    //--------------------------------------------------------------------------
    //   Public Methods:
    //--------------------------------------------------------------------------

    public static void main(String argv[]) {
        // Use a PropertyConfigurator to initialize from a property file.
        String resource =
                "/examples/lf5/InitUsingPropertiesFile/example.properties";
        URL configFileResource =
                InitUsingPropertiesFile.class.getResource(resource);
        PropertyConfigurator.configure(configFileResource);

        // Add a bunch of logging statements ...
        logger.debug("Hello, my name is Homer Simpson.");
        logger.debug("Hello, my name is Lisa Simpson.");
        logger.debug("Hello, my name is Marge Simpson.");
        logger.debug("Hello, my name is Bart Simpson.");
        logger.debug("Hello, my name is Maggie Simpson.");

        logger.info("We are the Simpsons!");
        logger.info("Mmmmmm .... Chocolate.");
        logger.info("Homer likes chocolate");
        logger.info("Doh!");
        logger.info("We are the Simpsons!");

        logger.warn("Bart: I am through with working! Working is for chumps!" +
                "Homer: Son, I'm proud of you. I was twice your age before " +
                "I figured that out.");
        logger.warn("Mmm...forbidden donut.");
        logger.warn("D'oh! A deer! A female deer!");
        logger.warn("Truly, yours is a butt that won't quit." +
                "- Bart, writing as Woodrow to Ms. Krabappel.");

        logger.error("Dear Baby, Welcome to Dumpsville. Population: you.");
        logger.error("Dear Baby, Welcome to Dumpsville. Population: you.",
                new IOException("Dumpsville, USA"));
        logger.error("Mr. Hutz, are you aware you're not wearing pants?");
        logger.error("Mr. Hutz, are you aware you're not wearing pants?",
                new IllegalStateException("Error !!"));


        logger.fatal("Eep.");
        logger.fatal("Mmm...forbidden donut.",
                new SecurityException("Fatal Exception"));
        logger.fatal("D'oh! A deer! A female deer!");
        logger.fatal("Mmmmmm .... Chocolate.",
                new SecurityException("Fatal Exception"));
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
