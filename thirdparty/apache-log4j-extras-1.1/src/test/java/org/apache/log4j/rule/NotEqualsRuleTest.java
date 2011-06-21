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
import java.util.Stack;

/**
 * Test for NotEqualsRule.
 */
public class NotEqualsRuleTest extends TestCase {

    /**
     * Create new test.
     *
     * @param testName test name.
     */
    public NotEqualsRuleTest(final String testName) {
        super(testName);
    }

    /**
     * getRule() with only one entry on stack should throw IllegalArgumentException.
     */
    public void test1() {
        Stack stack = new Stack();
        stack.push("Hello");
        try {
            NotEqualsRule.getRule(stack);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
        }
    }

    /**
     * getRule() with bad field name should throw IllegalArgumentException.
     */
    public void test2() {
        Stack stack = new Stack();
        stack.push("Hello");
        stack.push("World");
        try {
            NotEqualsRule.getRule(stack);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
        }
    }

    /**
     * getRule with "level" and "info".
     */
    public void test3() {
        Stack stack = new Stack();
        stack.push("level");
        stack.push("info");
        Rule rule = NotEqualsRule.getRule(stack);
        assertEquals(0, stack.size());
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getRootLogger(), System.currentTimeMillis(), Level.WARN,
                "Hello, World", null);
        assertTrue(rule.evaluate(event, null));
    }

    /**
     * getRule with "timestamp" and time.
     */
    public void test4() {
        Stack stack = new Stack();
        stack.push("timestamp");
        stack.push("2008/05/21 00:45:44");
        Rule rule = NotEqualsRule.getRule(stack);
        assertEquals(0, stack.size());
        Calendar cal = new GregorianCalendar(2009, 04, 21, 00, 45, 44);
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getRootLogger(), cal.getTimeInMillis(), Level.INFO,
                "Hello, World", null);
        assertTrue(rule.evaluate(event, null));
    }

    /**
     * getRule with "msg".
     */
    public void test5() {
        Stack stack = new Stack();
        stack.push("msg");
        stack.push("Hello, World");
        Rule rule = NotEqualsRule.getRule(stack);
        assertEquals(0, stack.size());
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getRootLogger(), System.currentTimeMillis(), Level.INFO,
                "Hello, World", null);
        assertFalse(rule.evaluate(event, null));
    }

    /**
     * getRule with "msg".
     */
    public void test6() {
        Stack stack = new Stack();
        stack.push("msg");
        stack.push("Bonjour, Monde");
        Rule rule = NotEqualsRule.getRule(stack);
        assertEquals(0, stack.size());
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getRootLogger(), System.currentTimeMillis(), Level.INFO,
                "Hello, World", null);
        assertTrue(rule.evaluate(event, null));
    }

    /**
     * Check NotEqualsRule serialization.
     */
    public void test7() throws IOException, ClassNotFoundException {
        Stack stack = new Stack();
        stack.push("msg");
        stack.push("Hello, World");
        Rule rule = (Rule) SerializationTestHelper.serializeClone(NotEqualsRule.getRule(stack));
        assertEquals(0, stack.size());
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getRootLogger(), System.currentTimeMillis(), Level.INFO,
                "Hello, World", null);
        assertFalse(rule.evaluate(event, null));
    }

}
