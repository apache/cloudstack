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
import java.util.Stack;

/**
 * Test for ExistsRule.
 */
public class ExistsRuleTest extends TestCase {

    /**
     * Create new test.
     *
     * @param testName test name.
     */
    public ExistsRuleTest(final String testName) {
        super(testName);
    }

    /**
     * getRule() with no entry on stack should throw IllegalArgumentException.
     */
    public void test1() {
        Stack stack = new Stack();
        try {
            ExistsRule.getRule(stack);
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
        try {
            ExistsRule.getRule(stack);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
        }
    }

    /**
     * getRule with "msg".
     */
    public void test3() {
        Stack stack = new Stack();
        stack.push("msg");
        Rule rule = ExistsRule.getRule(stack);
        assertEquals(0, stack.size());
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getRootLogger(), System.currentTimeMillis(), Level.INFO,
                "Hello, World", null);
        assertTrue(rule.evaluate(event, null));
    }

    /**
     * getRule with "msg".
     */
    public void test4() {
        Stack stack = new Stack();
        stack.push("msg");
        Rule rule = ExistsRule.getRule(stack);
        assertEquals(0, stack.size());
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getRootLogger(), System.currentTimeMillis(), Level.INFO,
                "", null);
        assertFalse(rule.evaluate(event, null));
    }

    /**
     * getRule with "msg".
     */
    public void test5() throws IOException, ClassNotFoundException {
        Stack stack = new Stack();
        stack.push("msg");
        Rule rule = (Rule) SerializationTestHelper.serializeClone(ExistsRule.getRule(stack));
        assertEquals(0, stack.size());
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getRootLogger(), System.currentTimeMillis(), Level.INFO,
                "Hello, World", null);
        assertTrue(rule.evaluate(event, null));
    }

}
