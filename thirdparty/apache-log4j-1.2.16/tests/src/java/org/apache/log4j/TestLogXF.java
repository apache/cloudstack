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
package org.apache.log4j;

import junit.framework.TestCase;


/**
 * Unit test for LogXF.
 */
public class TestLogXF extends TestCase {
    /**
     * Logger.
     */
    private final Logger logger = Logger.getLogger(
            "org.apache.log4j.formatter.TestLogXF");

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public TestLogXF(String testName) {
        super(testName);
    }


    /**
     * Post test clean up.
     */
    public void tearDown() {
        LogManager.resetConfiguration();
    }

    private static class BadStringifier {
        private BadStringifier() {}
        public static BadStringifier INSTANCE = new BadStringifier();
        public String toString() {
            throw new NullPointerException();
        }
    }


    /**
     * Test LogXF.entering with null class and method.
     */
    public void testEnteringNullNull() {
        LogCapture capture = new LogCapture(Level.DEBUG);
        logger.setLevel(Level.DEBUG);
        LogXF.entering(logger, null, null);
        assertEquals("null.null ENTRY", capture.getMessage());
    }


    /**
     * Test LogXF.entering with null class, method and parameter.
     */
    public void testEnteringNullNullNull() {
        LogCapture capture = new LogCapture(Level.DEBUG);
        logger.setLevel(Level.DEBUG);
        LogXF.entering(logger, null, null, (String) null);
        assertEquals("null.null ENTRY null", capture.getMessage());
    }

    /**
     * Test LogXF.entering with null class, method and parameters.
     */
    public void testEnteringNullNullNullArray() {
        LogCapture capture = new LogCapture(Level.DEBUG);
        logger.setLevel(Level.DEBUG);
        LogXF.entering(logger, null, null, (Object[]) null);
        assertEquals("null.null ENTRY {}", capture.getMessage());
    }

    /**
     * Test LogXF.entering with class and method.
     */
    public void testEntering() {
        LogCapture capture = new LogCapture(Level.DEBUG);
        logger.setLevel(Level.DEBUG);
        LogXF.entering(logger, "SomeClass", "someMethod");
        assertEquals("SomeClass.someMethod ENTRY", capture.getMessage());
    }

    /**
     * Test LogXF.entering with class, method and parameter.
     */
    public void testEnteringWithParam() {
        LogCapture capture = new LogCapture(Level.DEBUG);
        logger.setLevel(Level.DEBUG);
        LogXF.entering(logger, "SomeClass", "someMethod", "someParam");
        assertEquals("SomeClass.someMethod ENTRY someParam", capture.getMessage());
    }

    /**
     * Test LogXF.entering with class, method and bad parameter.
     */
    public void testEnteringWithBadParam() {
        LogCapture capture = new LogCapture(Level.DEBUG);
        logger.setLevel(Level.DEBUG);
        LogXF.entering(logger, "SomeClass", "someMethod", BadStringifier.INSTANCE);
        assertEquals("SomeClass.someMethod ENTRY ?", capture.getMessage());
    }

    /**
     * Test LogXF.entering with class, method and bad parameters.
     */
    public void testEnteringWithBadParams() {
        LogCapture capture = new LogCapture(Level.DEBUG);
        logger.setLevel(Level.DEBUG);
        LogXF.entering(logger, "SomeClass", "someMethod", new Object[]{"param1",BadStringifier.INSTANCE});
        assertEquals("SomeClass.someMethod ENTRY {param1,?}", capture.getMessage());
    }


    /**
     * Test LogXF.exiting with null class and method.
     */
    public void testExitingNullNull() {
        LogCapture capture = new LogCapture(Level.DEBUG);
        logger.setLevel(Level.DEBUG);
        LogXF.exiting(logger, null, null);
        assertEquals("null.null RETURN", capture.getMessage());
    }


    /**
     * Test LogXF.exiting with null class, method and parameter.
     */
    public void testExitingNullNullNull() {
        LogCapture capture = new LogCapture(Level.DEBUG);
        logger.setLevel(Level.DEBUG);
        LogXF.exiting(logger, null, null, (String) null);
        assertEquals("null.null RETURN null", capture.getMessage());
    }


    /**
     * Test LogXF.exiting with class and method.
     */
    public void testExiting() {
        LogCapture capture = new LogCapture(Level.DEBUG);
        logger.setLevel(Level.DEBUG);
        LogXF.exiting(logger, "SomeClass", "someMethod");
        assertEquals("SomeClass.someMethod RETURN", capture.getMessage());
    }

    /**
     * Test LogXF.exiting with class, method and return value.
     */
    public void testExitingWithValue() {
        LogCapture capture = new LogCapture(Level.DEBUG);
        logger.setLevel(Level.DEBUG);
        LogXF.exiting(logger, "SomeClass", "someMethod", "someValue");
        assertEquals("SomeClass.someMethod RETURN someValue", capture.getMessage());
    }

    /**
     * Test LogXF.exiting with class, method and bad return value.
     */
    public void testExitingWithBadValue() {
        LogCapture capture = new LogCapture(Level.DEBUG);
        logger.setLevel(Level.DEBUG);
        LogXF.exiting(logger, "SomeClass", "someMethod", BadStringifier.INSTANCE);
        assertEquals("SomeClass.someMethod RETURN ?", capture.getMessage());
    }


    /**
     * Test LogXF.throwing with null class, method and throwable.
     */
    public void testThrowingNullNullNull() {
        LogCapture capture = new LogCapture(Level.DEBUG);
        logger.setLevel(Level.DEBUG);
        LogXF.throwing(logger, null, null, null);
        assertEquals("null.null THROW", capture.getMessage());
    }


    /**
     * Test LogXF.exiting with class and method.
     */
    public void testThrowing() {
        LogCapture capture = new LogCapture(Level.DEBUG);
        logger.setLevel(Level.DEBUG);
        LogXF.throwing(logger, "SomeClass", "someMethod", new IllegalArgumentException());
        assertEquals("SomeClass.someMethod THROW", capture.getMessage());
    }

}
