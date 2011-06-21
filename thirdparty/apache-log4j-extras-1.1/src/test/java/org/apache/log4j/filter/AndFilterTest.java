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
 * Unit tests for AndFilter.
 */
public class AndFilterTest extends TestCase {

    /**
     * Create new test instance.
     *
     * @param name test name.
     */
    public AndFilterTest(final String name) {
        super(name);
    }

    /**
     * Check that AndFilter.decide() returns Filter.ACCEPT if no filters added.
     */
    public void test1() {
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getLogger(AndFilterTest.class),
                System.currentTimeMillis(), Level.INFO, "Hello, World", null);
        Filter filter = new AndFilter();
        filter.activateOptions();
        assertEquals(Filter.ACCEPT, filter.decide(event));
    }

    /**
     * Check that AndFilter.decide() returns Filter.ACCEPT if
     *    only nested filter returns Filter.ACCEPT.
     */
    public void test2() {
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getLogger(AndFilterTest.class),
                System.currentTimeMillis(), Level.INFO, "Hello, World", null);
        AndFilter filter = new AndFilter();
        LevelMatchFilter filter1 = new LevelMatchFilter();
        filter1.setLevelToMatch("info");
        filter1.activateOptions();
        filter.addFilter(filter1);
        filter.activateOptions();
        assertEquals(Filter.ACCEPT, filter.decide(event));
    }

    /**
     * Check that AndFilter.decide() returns Filter.ACCEPT if
     *    two nested filters return Filter.ACCEPT.
     */
    public void test3() {
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getLogger(AndFilterTest.class),
                System.currentTimeMillis(), Level.INFO, "Hello, World", null);
        AndFilter filter = new AndFilter();
        LevelMatchFilter filter1 = new LevelMatchFilter();
        filter1.setLevelToMatch("info");
        filter1.activateOptions();
        filter.addFilter(filter1);
        LevelMatchFilter filter2 = new LevelMatchFilter();
        filter2.setLevelToMatch("info");
        filter2.activateOptions();
        filter.addFilter(filter2);
        filter.activateOptions();
        assertEquals(Filter.ACCEPT, filter.decide(event));
    }

    /**
     * Check that AndFilter.decide() returns Filter.DENY if
     *    only nested filter returns Filter.ACCEPT
     *    and acceptOnMatch is false.
     */
    public void test4() {
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getLogger(AndFilterTest.class),
                System.currentTimeMillis(), Level.INFO, "Hello, World", null);
        AndFilter filter = new AndFilter();
        LevelMatchFilter filter1 = new LevelMatchFilter();
        filter1.setLevelToMatch("info");
        filter1.activateOptions();
        filter.addFilter(filter1);
        filter.setAcceptOnMatch(false);
        filter.activateOptions();
        assertEquals(Filter.DENY, filter.decide(event));
    }

    /**
     * Check that AndFilter.decide() returns Filter.NEUTRAL if
     *    nested filters return Filter.ACCEPT and Filter.DENY.
     */
    public void test5() {
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getLogger(AndFilterTest.class),
                System.currentTimeMillis(), Level.INFO, "Hello, World", null);
        AndFilter filter = new AndFilter();
        LevelMatchFilter filter1 = new LevelMatchFilter();
        filter1.setLevelToMatch("info");
        filter1.activateOptions();
        filter.addFilter(filter1);
        Filter filter2 = new DenyAllFilter();
        filter2.activateOptions();
        filter.addFilter(filter2);
        filter.activateOptions();
        assertEquals(Filter.NEUTRAL, filter.decide(event));
    }

    /**
     * Check that AndFilter.decide() returns Filter.NEUTRAL if
     *    nested filters return Filter.ACCEPT and Filter.NEUTRAL.
     */
    public void test6() {
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getLogger(AndFilterTest.class),
                System.currentTimeMillis(), Level.INFO, "Hello, World", null);
        AndFilter filter = new AndFilter();
        LevelMatchFilter filter1 = new LevelMatchFilter();
        filter1.setLevelToMatch("info");
        filter1.activateOptions();
        filter.addFilter(filter1);
        Filter filter2 = new StringMatchFilter();
        filter2.activateOptions();
        filter.addFilter(filter2);
        filter.activateOptions();
        assertEquals(Filter.NEUTRAL, filter.decide(event));
    }

}

