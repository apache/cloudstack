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
 * Unit tests for StringMatchFilter.
 */
public class StringMatchFilterTest extends TestCase {

    /**
     * Create new test instance.
     *
     * @param name test name.
     */
    public StringMatchFilterTest(final String name) {
        super(name);
    }

    /**
     * Check that StringMatchFilter.decide() returns Filter.NEUTRAL
     *   when string to match is unspecified.
     */
    public void test1() {
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getLogger(StringMatchFilterTest.class),
                System.currentTimeMillis(), Level.INFO, "Hello, World", null);
        Filter filter = new StringMatchFilter();
        filter.activateOptions();
        assertEquals(Filter.NEUTRAL, filter.decide(event));
    }

    /**
     * Check that StringMatchFilter.decide() returns Filter.NEUTRAL
     *   when string to match does not appear in message.
     */
    public void test2() {
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getLogger(StringMatchFilterTest.class),
                System.currentTimeMillis(), Level.INFO, "Hello, World", null);
        StringMatchFilter filter = new StringMatchFilter();
        filter.setStringToMatch("Monde");
        filter.activateOptions();
        assertEquals(Filter.NEUTRAL, filter.decide(event));
    }

    /**
     * Check that StringMatchFilter.decide() returns Filter.ACCEPT
     *   when string to match does appear in message.
     */
    public void test3() {
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getLogger(StringMatchFilterTest.class),
                System.currentTimeMillis(), Level.INFO, "Hello, World", null);
        StringMatchFilter filter = new StringMatchFilter();
        filter.setStringToMatch("World");
        filter.activateOptions();
        assertEquals(Filter.ACCEPT, filter.decide(event));
    }

    /**
     * Check that StringMatchFilter.decide() returns Filter.DENY
     *   when string to match does appear in message and
     *   accept on match is false.
     */
    public void test4() {
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getLogger(StringMatchFilterTest.class),
                System.currentTimeMillis(), Level.INFO, "Hello, World", null);
        StringMatchFilter filter = new StringMatchFilter();
        filter.setStringToMatch("World");
        filter.setAcceptOnMatch(false);
        filter.activateOptions();
        assertEquals(Filter.DENY, filter.decide(event));
    }

    /**
     * Check that StringMatchFilter.decide() returns Filter.NEUTRAL
     *   when string to match does appear in message but differs in case.
     */
    public void test5() {
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getLogger(StringMatchFilterTest.class),
                System.currentTimeMillis(), Level.INFO, "Hello, World", null);
        StringMatchFilter filter = new StringMatchFilter();
        filter.setStringToMatch("world");
        filter.activateOptions();
        assertEquals(Filter.NEUTRAL, filter.decide(event));
    }

}

