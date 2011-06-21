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
package org.apache.log4j.rule;


import junit.framework.TestCase;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.util.SerializationTestHelper;

import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Test for TimestampEqualsRule.
 */
public class TimestampEqualsRuleTest extends TestCase {

    /**
     * Create new test.
     *
     * @param testName test name.
     */
    public TimestampEqualsRuleTest(final String testName) {
        super(testName);
    }

    /**
     * Tests evaluate when timestamps are equal.
     */
    public void test1() {
        TimestampEqualsRule rule = (TimestampEqualsRule) TimestampEqualsRule.getRule("2008/05/21 00:45:44");
        Calendar cal = new GregorianCalendar(2008,04,21,00,45,44);
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getRootLogger(), cal.getTimeInMillis(), Level.INFO,
                "Hello, World", null);
        assertTrue(rule.evaluate(event, null));
    }

    /**
     * Tests evaluate when levels are not equal.
     */
    public void test2() {
        TimestampEqualsRule rule = (TimestampEqualsRule) TimestampEqualsRule.getRule("2008/05/21 00:45:44");
        Calendar cal = new GregorianCalendar(2008,04,21,00,46,44);
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getRootLogger(), cal.getTimeInMillis(), Level.WARN,
                "Hello, World", null);
        assertFalse(rule.evaluate(event, null));
    }

    /**
     * Tests evaluate of a deserialized clone when timestamps are equal.
     */
    public void test3() throws IOException, ClassNotFoundException {
        TimestampEqualsRule rule = (TimestampEqualsRule)
                SerializationTestHelper.serializeClone(TimestampEqualsRule.getRule("2008/05/21 00:45:44"));
        Calendar cal = new GregorianCalendar(2008,04,21,00,45,44);
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getRootLogger(), cal.getTimeInMillis(), Level.INFO,
                "Hello, World", null);
        assertTrue(rule.evaluate(event, null));
    }

    /**
     * Tests evaluate of a deserialized clone when timestamps are not equal.
     */
    public void test4() throws IOException, ClassNotFoundException {
        TimestampEqualsRule rule = (TimestampEqualsRule)
                SerializationTestHelper.serializeClone(TimestampEqualsRule.getRule("2008/05/21 00:45:44"));
        Calendar cal = new GregorianCalendar(2008,04,21,00,46,44);
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getRootLogger(), cal.getTimeInMillis(), Level.INFO,
                "Hello, World", null);
        assertFalse(rule.evaluate(event, null));
    }

    /**
     * Tests constructor will badly formed time specification.
     */
    public void test5() {
        try {
            TimestampEqualsRule.getRule("2008/May/21");
            fail("IllegalArgumentException expected to be thrown");
        } catch(IllegalArgumentException ex) {
        }
    }

}
