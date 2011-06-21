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
package org.apache.log4j.net;

import junit.framework.TestCase;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.xml.DOMConfigurator;

public class SocketAppenderTest extends TestCase {

    /**
	 *  Create new instance.
	 */
    public SocketAppenderTest(final String testName) {
	    super(testName);
    }

    /* JUnit's setUp and tearDown */

    protected void setUp() {
        DOMConfigurator.configure("input/xml/SocketAppenderTestConfig.xml");

        logger = Logger.getLogger(SocketAppenderTest.class);
        secondary = (LastOnlyAppender) Logger.getLogger(
                "org.apache.log4j.net.SocketAppenderTestDummy").getAppender("lastOnly");
    }

    protected void tearDown() {
    }

    /* Tests */

    public void testFallbackErrorHandlerWhenStarting() {
        String msg = "testFallbackErrorHandlerWhenStarting";
        logger.debug(msg);

        // above debug log will fail and shoul be redirected to secondary appender
        assertEquals("SocketAppender with FallbackErrorHandler", msg, secondary.getLastMessage());
    }

    /* Fields */

    private static Logger logger;
    private static LastOnlyAppender secondary;

    /* Inner classes */

    /**
     * Inner-class For debugging purposes only Saves last LoggerEvent
     */
    static public class LastOnlyAppender extends AppenderSkeleton {
        protected void append(LoggingEvent event) {
            this.lastEvent = event;
        }

        public boolean requiresLayout() {
            return false;
        }

        public void close() {
            this.closed = true;
        }

        /**
         * @return last appended LoggingEvent's message
         */
        public String getLastMessage() {
            if (this.lastEvent != null)
                return this.lastEvent.getMessage().toString();
            else
                return "";
        }

        private LoggingEvent lastEvent;
    };

}