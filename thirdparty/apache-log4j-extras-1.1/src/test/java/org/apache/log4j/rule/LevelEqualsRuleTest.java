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
 * Test for LevelEqualsRule.
 */
public class LevelEqualsRuleTest extends TestCase {

    /**
     * Create new test.
     *
     * @param testName test name.
     */
    public LevelEqualsRuleTest(final String testName) {
        super(testName);
    }

    /**
     * Tests evaluate when levels are equal.
     */
    public void test1() {
        LevelEqualsRule rule = (LevelEqualsRule) LevelEqualsRule.getRule("info");
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getRootLogger(), System.currentTimeMillis(), Level.INFO,
                "Hello, World", null);
        assertTrue(rule.evaluate(event, null));
    }

    /**
     * Tests evaluate when levels are not equal.
     */
    public void test2() {
        LevelEqualsRule rule = (LevelEqualsRule) LevelEqualsRule.getRule("info");
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getRootLogger(), System.currentTimeMillis(), Level.WARN,
                "Hello, World", null);
        assertFalse(rule.evaluate(event, null));
    }

    /**
     * Tests evaluate of a deserialized clone when levels are equal.
     */
    public void test3() throws IOException, ClassNotFoundException {
        LevelEqualsRule rule = (LevelEqualsRule)
                SerializationTestHelper.serializeClone(LevelEqualsRule.getRule("info"));
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getRootLogger(), System.currentTimeMillis(), Level.INFO,
                "Hello, World", null);
        assertTrue(rule.evaluate(event, null));
    }

    /**
     * Tests evaluate of a deserialized clone when levels are not equal.
     */
    public void test4() throws IOException, ClassNotFoundException {
        LevelEqualsRule rule = (LevelEqualsRule)
                SerializationTestHelper.serializeClone(LevelEqualsRule.getRule("info"));
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getRootLogger(), System.currentTimeMillis(), Level.WARN,
                "Hello, World", null);
        assertFalse(rule.evaluate(event, null));
    }

    /**
     * Tests evaluate when levels are JDK 1.4 levels and equal.
     */
    public void test5() {
        LevelEqualsRule rule = (LevelEqualsRule) LevelEqualsRule.getRule("finer");
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getRootLogger(), System.currentTimeMillis(), UtilLoggingLevel.FINER,
                "Hello, World", null);
        assertTrue(rule.evaluate(event, null));
    }

    /**
     * Tests evaluate when levels are JDK 1.4 levels and not equal.
     */
    public void test6() {
        LevelEqualsRule rule = (LevelEqualsRule) LevelEqualsRule.getRule("finer");
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getRootLogger(), System.currentTimeMillis(), UtilLoggingLevel.FINE,
                "Hello, World", null);
        assertFalse(rule.evaluate(event, null));
    }

    /**
     * Tests evaluate of a deserialized clone when levels are JDK 1.4 levels and equal.
     */
    public void test7() throws IOException, ClassNotFoundException {
        LevelEqualsRule rule = (LevelEqualsRule)
                SerializationTestHelper.serializeClone(LevelEqualsRule.getRule("finer"));
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getRootLogger(), System.currentTimeMillis(), UtilLoggingLevel.FINER,
                "Hello, World", null);
        assertTrue(rule.evaluate(event, null));
    }

    /**
     * Tests evaluate of a deserialized clone when levels are JDK 1.4 levels and not equal.
     */
    public void test8() throws IOException, ClassNotFoundException {
        LevelEqualsRule rule = (LevelEqualsRule)
                SerializationTestHelper.serializeClone(LevelEqualsRule.getRule("finer"));
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getRootLogger(), System.currentTimeMillis(), UtilLoggingLevel.FINE,
                "Hello, World", null);
        assertFalse(rule.evaluate(event, null));
    }

}
