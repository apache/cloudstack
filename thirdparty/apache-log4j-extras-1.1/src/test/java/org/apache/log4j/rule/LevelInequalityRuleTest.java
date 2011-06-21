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
import org.apache.log4j.helpers.UtilLoggingLevel;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.util.SerializationTestHelper;

import java.io.IOException;

/**
 * Test for LevelInequalityRule.
 */
public class LevelInequalityRuleTest extends TestCase {

    /**
     * Create new test.
     *
     * @param testName test name.
     */
    public LevelInequalityRuleTest(final String testName) {
        super(testName);
    }

    /**
     * Test construction when level is unrecognized.
     */
    public void test1() {
        try {
            LevelInequalityRule.getRule(">", "emergency");
            fail("Expected IllegalArgumentException");
        } catch(IllegalArgumentException ex) {
        }
    }

    /**
     * Tests construction when operator is unrecognized.
     */
    public void test2() {
        Rule rule = LevelInequalityRule.getRule("~", "iNfO");
        assertNull(rule);
    }

    /**
     * Tests evaluate of a deserialized clone when level satisfies rule.
     */
    public void test3() throws IOException, ClassNotFoundException {
        Rule rule = (Rule)
                SerializationTestHelper.serializeClone(LevelInequalityRule.getRule(">=", "info"));
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getRootLogger(), System.currentTimeMillis(), Level.INFO,
                "Hello, World", null);
        assertTrue(rule.evaluate(event, null));
    }

    /**
     * Tests evaluate of a deserialized clone when level does not satisfy rule.
     */
    public void test4() throws IOException, ClassNotFoundException {
        Rule rule = (Rule)
                SerializationTestHelper.serializeClone(LevelInequalityRule.getRule("<", "info"));
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getRootLogger(), System.currentTimeMillis(), Level.INFO,
                "Hello, World", null);
        assertFalse(rule.evaluate(event, null));
    }

    /**
     * Tests evaluate when levels are JDK 1.4 levels and satisified.
     */
    public void test5() {
        Rule rule = (Rule) LevelInequalityRule.getRule(">=", "finer");
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getRootLogger(), System.currentTimeMillis(), UtilLoggingLevel.FINER,
                "Hello, World", null);
        assertTrue(rule.evaluate(event, null));
    }

    /**
     * Tests evaluate when levels are JDK 1.4 levels and not equal.
     */
    public void test6() {
        Rule rule = (Rule) LevelInequalityRule.getRule("<", "finer");
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getRootLogger(), System.currentTimeMillis(), UtilLoggingLevel.FINER,
                "Hello, World", null);
        assertFalse(rule.evaluate(event, null));
    }

    /**
     * Tests evaluate of a deserialized clone when levels are JDK 1.4 levels and satisified.
     */
    public void test7() throws IOException, ClassNotFoundException {
        Rule rule = (Rule)
                SerializationTestHelper.serializeClone(LevelInequalityRule.getRule(">=", "finer"));
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getRootLogger(), System.currentTimeMillis(), UtilLoggingLevel.FINER,
                "Hello, World", null);
        assertTrue(rule.evaluate(event, null));
    }

    /**
     * Tests evaluate of a deserialized clone when levels are JDK 1.4 levels and not satified.
     */
    public void test8() throws IOException, ClassNotFoundException {
        Rule rule = (Rule)
                SerializationTestHelper.serializeClone(LevelInequalityRule.getRule("<", "finer"));
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getRootLogger(), System.currentTimeMillis(), UtilLoggingLevel.FINER,
                "Hello, World", null);
        assertFalse(rule.evaluate(event, null));
    }

}
