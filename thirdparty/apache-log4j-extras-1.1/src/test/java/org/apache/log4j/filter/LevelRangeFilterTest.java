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
 * Unit tests for LevelRangeFilter.
 */
public class LevelRangeFilterTest extends TestCase {

    /**
     * Create new test instance.
     *
     * @param name test name.
     */
    public LevelRangeFilterTest(final String name) {
        super(name);
    }

    /**
     * Check that LevelRangeFilter.decide() returns Filter.DENY
     *     when event level is below min level.
     */
    public void test1() {
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getLogger(LevelRangeFilterTest.class),
                System.currentTimeMillis(), Level.INFO, "Hello, World", null);
        LevelRangeFilter filter = new LevelRangeFilter();
        filter.setLevelMin(Level.WARN);
        filter.activateOptions();
        assertEquals(Filter.DENY, filter.decide(event));
    }

    /**
     * Check that LevelRangeFilter.decide() returns Filter.DENY
     *    when event level is above max level.
     */
    public void test2() {
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getLogger(LevelRangeFilterTest.class),
                System.currentTimeMillis(), Level.INFO, "Hello, World", null);
        LevelRangeFilter filter = new LevelRangeFilter();
        filter.setLevelMax(Level.DEBUG);
        filter.activateOptions();
        assertEquals(Filter.DENY, filter.decide(event));
    }

    /**
     * Check that LevelRangeFilter.decide() returns Filter.ACCEPT
     *    when event level is above min level.
     */
    public void test3() {
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getLogger(LevelRangeFilterTest.class),
                System.currentTimeMillis(), Level.INFO, "Hello, World", null);
        LevelRangeFilter filter = new LevelRangeFilter();
        filter.setLevelMin(Level.DEBUG);
        filter.setAcceptOnMatch(true);
        filter.activateOptions();
        assertEquals(Filter.ACCEPT, filter.decide(event));
    }

    /**
     * Check that LevelRangeFilter.decide() returns Filter.ACCEPT
     *    when event level is below max level.
     */
    public void test4() {
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getLogger(LevelRangeFilterTest.class),
                System.currentTimeMillis(), Level.INFO, "Hello, World", null);
        LevelRangeFilter filter = new LevelRangeFilter();
        filter.setLevelMax(Level.ERROR);
        filter.setAcceptOnMatch(true);
        filter.activateOptions();
        assertEquals(Filter.ACCEPT, filter.decide(event));
    }

    /**
     * Check that LevelRangeFilter.decide() returns Filter.NEUTRAL
     *    when event level is above min level and accept on match is false.
     */
    public void test5() {
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getLogger(LevelRangeFilterTest.class),
                System.currentTimeMillis(), Level.INFO, "Hello, World", null);
        LevelRangeFilter filter = new LevelRangeFilter();
        filter.setLevelMin(Level.DEBUG);
        filter.activateOptions();
        assertEquals(Filter.NEUTRAL, filter.decide(event));
    }

    /**
     * Check that LevelRangeFilter.decide() returns Filter.NEUTRAL
     *    when event level is below max level and accept on match is false.
     */
    public void test6() {
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getLogger(LevelRangeFilterTest.class),
                System.currentTimeMillis(), Level.INFO, "Hello, World", null);
        LevelRangeFilter filter = new LevelRangeFilter();
        filter.setLevelMax(Level.ERROR);
        filter.activateOptions();
        assertEquals(Filter.NEUTRAL, filter.decide(event));
    }
}

