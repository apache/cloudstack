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
package org.apache.log4j.filter;

import junit.framework.TestCase;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;


/**
 * Unit tests for LevelMatchFilter.
 */
public class LevelMatchFilterTest extends TestCase {

    /**
     * Create new test instance.
     *
     * @param name test name.
     */
    public LevelMatchFilterTest(final String name) {
        super(name);
    }

    /**
     * Check that LevelMatchFilter.decide() returns Filter.ACCEPT when level matches.
     */
    public void test1() {
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getLogger(LevelMatchFilterTest.class),
                System.currentTimeMillis(), Level.INFO, "Hello, World", null);
        LevelMatchFilter filter = new LevelMatchFilter();
        filter.setLevelToMatch("info");
        filter.activateOptions();
        assertEquals(Filter.ACCEPT, filter.decide(event));
    }

    /**
     * Check that LevelMatchFilter.decide() returns Filter.DENY
     *    when level matches and acceptOnMatch = false.
     */
    public void test2() {
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getLogger(LevelMatchFilterTest.class),
                System.currentTimeMillis(), Level.INFO, "Hello, World", null);
        LevelMatchFilter filter = new LevelMatchFilter();
        filter.setLevelToMatch("info");
        filter.setAcceptOnMatch(false);
        filter.activateOptions();
        assertEquals(Filter.DENY, filter.decide(event));
    }

    /**
     * Check that LevelMatchFilter.decide() returns Filter.NEUTRAL
     *    when levelToMatch is unspecified.
     */
    public void test3() {
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getLogger(LevelMatchFilterTest.class),
                System.currentTimeMillis(), Level.INFO, "Hello, World", null);
        LevelMatchFilter filter = new LevelMatchFilter();
        filter.activateOptions();
        assertEquals(Filter.NEUTRAL, filter.decide(event));
    }

    /**
     * Check that LevelMatchFilter.decide() returns Filter.NEUTRAL
     *    when event level is higher than level to match.
     */
    public void test4() {
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getLogger(LevelMatchFilterTest.class),
                System.currentTimeMillis(), Level.INFO, "Hello, World", null);
        LevelMatchFilter filter = new LevelMatchFilter();
        filter.setLevelToMatch("debug");
        filter.activateOptions();
        assertEquals(Filter.NEUTRAL, filter.decide(event));
    }

    /**
     * Check that LevelMatchFilter.decide() returns Filter.NEUTRAL
     *    when event level is lower than level to match.
     */
    public void test5() {
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getLogger(LevelMatchFilterTest.class),
                System.currentTimeMillis(), Level.INFO, "Hello, World", null);
        LevelMatchFilter filter = new LevelMatchFilter();
        filter.setLevelToMatch("warn");
        filter.activateOptions();
        assertEquals(Filter.NEUTRAL, filter.decide(event));
    }
}

