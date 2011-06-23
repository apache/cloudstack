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
package examples.lf5.InitUsingDefaultConfigurator;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;
import org.apache.log4j.lf5.DefaultLF5Configurator;

import java.io.IOException;

/**
 * This class is a simple example of how to configure the LogFactor5
 * logging window using the DefaultLF5Configurator.
 *
 * The DefaultLF5Configurator uses a default configuration file stored
 * in the log4j.jar in order to provide a default configuration for
 * the LF5Appender.
 *
 * @author Brent Sprecher
 */

// Contributed by ThoughtWorks Inc.

public class InitUsingDefaultConfigurator {
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
            Logger.getLogger(InitUsingDefaultConfigurator.class);

    //--------------------------------------------------------------------------
    //   Constructors:
    //--------------------------------------------------------------------------

    //--------------------------------------------------------------------------
    //   Public Methods:
    //--------------------------------------------------------------------------

    public static void main(String[] args) throws IOException {
        // Configure the LF5Appender using the DefaultLF5Configurator.  This
        // will add the LF5Appender to the root of the Category tree.
        DefaultLF5Configurator.configure();

        // Add an NDC to demonstrate how NDC information is output.
        NDC.push("#23856");
        // Log some information.
        for (int i = 0; i < 10; i++) {
            logger.debug("Hello, my name is Homer Simpson.");
            logger.info("Mmmmmm .... Chocolate.");
            logger.warn("Mmm...forbidden donut.");
        }
        // Clean up NDC
        NDC.pop();
        NDC.remove();

        NDC.push("Another NDC");
        // Log some information.
        logger.fatal("Hello, my name is Bart Simpson.");
        logger.error("Hi diddly ho good neighbour.");
        // Clean up NDC
        NDC.pop();
        NDC.remove();

        // Call methods on both classes.
        InitUsingDefaultConfigurator.foo();
        InnerInitUsingDefaultConfigurator.foo();

        logger.info("Exiting InitUsingDefaultConfigurator.");

    }

    public static void foo() {
        logger.debug("Entered foo in InitUsingDefaultConfigurator class");

        NDC.push("#123456");
        logger.debug("Hello, my name is Marge Simpson.");
        logger.info("D'oh!! A deer! A female deer.");
        // Clean up NDC
        NDC.pop();
        NDC.remove();
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

    public static class InnerInitUsingDefaultConfigurator {
        static Logger logger =
                Logger.getLogger(InnerInitUsingDefaultConfigurator.class.getName());

        static void foo() throws IOException {
            // Configure the LF5Appender again. You can call
            // DefaultLF5Configurator.configure() as often as you want
            // without unexpected behavior.
            DefaultLF5Configurator.configure();

            logger.info("Entered foo in InnerInitUsingDefaultConfigurator class.");
        }
    }
}





