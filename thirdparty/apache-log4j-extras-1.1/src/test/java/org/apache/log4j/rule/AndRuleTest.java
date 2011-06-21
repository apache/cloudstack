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
 * Test for AndRule.
 */
public class AndRuleTest extends TestCase {

    /**
     * Create new test.
     *
     * @param testName test name.
     */
    public AndRuleTest(final String testName) {
        super(testName);
    }

    /**
     * AndRule.getRule(Stack) throws exception if only one rule provided.
     */
    public void test1() {
        Stack stack = new Stack();
        stack.push(LevelEqualsRule.getRule("INFO"));
        try {
            AndRule.getRule(stack);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
        }
    }

    /**
     * AndRule.getRule(Stack) throws exception if non-rules are provided.
     */
    public void test2() {
        Stack stack = new Stack();
        stack.push("Hello");
        stack.push("World");
        try {
            AndRule.getRule(stack);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
        }
    }

    /**
     * Test And of Level and Time.
     */
    public void test3() {
        Stack stack = new Stack();
        stack.push(LevelEqualsRule.getRule("INFO"));
        stack.push(TimestampInequalityRule.getRule(">=", "2008/05/21 00:44:45"));
        Rule rule = AndRule.getRule(stack);
        assertEquals(0, stack.size());
        Calendar cal = new GregorianCalendar(2008, 04, 21, 00, 45, 44);
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getRootLogger(), cal.getTimeInMillis(), Level.INFO,
                "Hello, World", null);
        assertTrue(rule.evaluate(event, null));
    }

    /**
     * Test And of Level and Time when Level does not match.
     */
    public void test4() {
        Stack stack = new Stack();
        stack.push(LevelEqualsRule.getRule("INFO"));
        stack.push(TimestampInequalityRule.getRule(">=", "2008/05/21 00:44:45"));
        Rule rule = AndRule.getRule(stack);
        assertEquals(0, stack.size());
        Calendar cal = new GregorianCalendar(2008, 04, 21, 00, 45, 44);
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getRootLogger(), cal.getTimeInMillis(), Level.WARN,
                "Hello, World", null);
        assertFalse(rule.evaluate(event, null));
    }

    /**
     * Test And of Level and Time when Time does not match.
     */
    public void test5() {
        Stack stack = new Stack();
        stack.push(LevelEqualsRule.getRule("INFO"));
        stack.push(TimestampInequalityRule.getRule(">=", "2009/05/21 00:44:45"));
        Rule rule = AndRule.getRule(stack);
        assertEquals(0, stack.size());
        Calendar cal = new GregorianCalendar(2008, 04, 21, 00, 45, 44);
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getRootLogger(), cal.getTimeInMillis(), Level.INFO,
                "Hello, World", null);
        assertFalse(rule.evaluate(event, null));
    }


    /**
     * Test deserialized And.
     */
    public void test6() throws IOException, ClassNotFoundException {
        Stack stack = new Stack();
        stack.push(LevelEqualsRule.getRule("INFO"));
        stack.push(TimestampInequalityRule.getRule(">=", "2008/05/21 00:44:45"));
        Rule rule = (Rule) SerializationTestHelper.serializeClone(AndRule.getRule(stack));
        assertEquals(0, stack.size());
        Calendar cal = new GregorianCalendar(2008, 04, 21, 00, 45, 44);
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getRootLogger(), cal.getTimeInMillis(), Level.INFO,
                "Hello, World", null);
        assertTrue(rule.evaluate(event, null));
    }


    /**
     * Test deserialized And when Level doesn't match.
     */
    public void test7() throws IOException, ClassNotFoundException {
        Stack stack = new Stack();
        stack.push(LevelEqualsRule.getRule("INFO"));
        stack.push(TimestampInequalityRule.getRule(">=", "2008/05/21 00:44:45"));
        Rule rule = (Rule) SerializationTestHelper.serializeClone(AndRule.getRule(stack));
        assertEquals(0, stack.size());
        Calendar cal = new GregorianCalendar(2008, 04, 21, 00, 45, 44);
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getRootLogger(), cal.getTimeInMillis(), Level.WARN,
                "Hello, World", null);
        assertFalse(rule.evaluate(event, null));
    }

}
