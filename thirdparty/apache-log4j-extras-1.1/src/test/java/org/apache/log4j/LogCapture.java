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
package org.apache.log4j;

import junit.framework.Assert;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.VectorAppender;
import org.apache.log4j.spi.LoggingEvent;

import java.util.Vector;


/**
 * Helper class to set up and capture log messages.
 */
public class LogCapture {
    /**
     * Appender.
     */
    private final VectorAppender appender;

    /**
     * Expected level.
     */
    private final Level level;

    /**
     * Creates new instance of LogCapture.
     *
     */
    public LogCapture(final Level level) {
        this.level = level;

        Logger root = Logger.getRootLogger();
        appender = new VectorAppender();
        root.addAppender(appender);
    }

    /**
     * Get message.
     * @return rendered message, null if no logging event captured.
     */
    public String getMessage() {
        Vector vector = appender.getVector();
        String msg = null;

        switch (vector.size()) {
        case 0:
            break;

        case 1:

            LoggingEvent event = (LoggingEvent) vector.elementAt(0);
            Assert.assertNotNull(event);
            Assert.assertEquals(level, event.getLevel());
            msg = event.getRenderedMessage();

            break;

        default:
            Assert.fail("More than one request captured");
        }

        return msg;
    }
}
