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

import java.io.CharArrayWriter;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.text.DateFormat;


/**
 * Unit test for LogMF.
 */
public class TestLogMF extends TestCase {
    /**
     * Trace level.
     */
    private static final Level TRACE = getTraceLevel();

    /**
     * Gets Trace level.
     * Trace level was not defined prior to log4j 1.2.12.
     * @return trace level
     */
    private static Level getTraceLevel() {
        try {
            return (Level) Level.class.getField("TRACE").get(null);
        } catch(Exception ex) {
            return new Level(5000, "TRACE", 7);
        }
    }

    /**
     * Logger.
     */
    private final Logger logger = Logger.getLogger(
            "org.apache.log4j.formatter.TestLogMF");

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public TestLogMF(String testName) {
        super(testName);
    }


    /**
     * Post test clean up.
     */
    public void tearDown() {
        LogManager.resetConfiguration();
    }

    /**
     * Test class name when logging through LogMF.
     */
    public void testClassName() {
        CharArrayWriter writer = new CharArrayWriter();
        PatternLayout layout = new PatternLayout("%C");
        WriterAppender appender = new WriterAppender(layout, writer);
        appender.activateOptions();
        Logger.getRootLogger().addAppender(appender);
        LogMF.debug(logger, null, Math.PI);
        assertEquals(TestLogMF.class.getName(), writer.toString());
    }

    /**
     * Test LogMF.trace with null pattern.
     */
    public void testTraceNullPattern() {
        LogCapture capture = new LogCapture(TRACE);
        logger.setLevel(TRACE);
        LogMF.trace(logger, null, Math.PI);
        assertNull(capture.getMessage());
    }

    /**
     * Test LogMF.trace with no-field pattern.
     */
    public void testTraceNoArg() {
        LogCapture capture = new LogCapture(TRACE);
        logger.setLevel(TRACE);
        LogMF.trace(logger, "Hello, World", Math.PI);
        assertEquals("Hello, World", capture.getMessage());
    }

    /**
     * Test LogMF.trace with malformed pattern.
     */
    public void testTraceBadPattern() {
        LogCapture capture = new LogCapture(TRACE);
        logger.setLevel(TRACE);
        LogMF.trace(logger, "Hello, {.", Math.PI);
        assertEquals("Hello, {.", capture.getMessage());
    }

    /**
     * Test LogMF.trace with missing argument.
     */
    public void testTraceMissingArg() {
        LogCapture capture = new LogCapture(TRACE);
        logger.setLevel(TRACE);
        LogMF.trace(logger, "Hello, {0}World", new Object[0]);
        assertEquals("Hello, {0}World", capture.getMessage());
    }

    /**
     * Test LogMF.trace with single field pattern with string argument.
     */
    public void testTraceString() {
        LogCapture capture = new LogCapture(TRACE);
        logger.setLevel(TRACE);
        LogMF.trace(logger, "Hello, {0}", "World");
        assertEquals("Hello, World", capture.getMessage());
    }

    /**
     * Test LogMF.trace with single field pattern with null argument.
     */
    public void testTraceNull() {
        LogCapture capture = new LogCapture(TRACE);
        logger.setLevel(TRACE);
        LogMF.trace(logger, "Hello, {0}", (Object) null);
        assertEquals("Hello, null", capture.getMessage());
    }

    /**
     * Test LogMF.trace with single field pattern with int argument.
     */
    public void testTraceInt() {
        LogCapture capture = new LogCapture(TRACE);
        logger.setLevel(TRACE);

        int val = 42;
        LogMF.trace(logger, "Iteration {0}", val);
        assertEquals("Iteration 42", capture.getMessage());
    }

    /**
     * Test LogMF.trace with single field pattern with byte argument.
     */
    public void testTraceByte() {
        LogCapture capture = new LogCapture(TRACE);
        logger.setLevel(TRACE);

        byte val = 42;
        LogMF.trace(logger, "Iteration {0}", val);
        assertEquals("Iteration 42", capture.getMessage());
    }

    /**
     * Test LogMF.trace with single field pattern with short argument.
     */
    public void testTraceShort() {
        LogCapture capture = new LogCapture(TRACE);
        logger.setLevel(TRACE);

        short val = 42;
        LogMF.trace(logger, "Iteration {0}", val);
        assertEquals("Iteration 42", capture.getMessage());
    }

    /**
     * Test LogMF.trace with single field pattern with long argument.
     */
    public void testTraceLong() {
        LogCapture capture = new LogCapture(TRACE);
        logger.setLevel(TRACE);

        long val = 42;
        LogMF.trace(logger, "Iteration {0}", val);
        assertEquals("Iteration 42", capture.getMessage());
    }

    /**
     * Test LogMF.trace with single field pattern with char argument.
     */
    public void testTraceChar() {
        LogCapture capture = new LogCapture(TRACE);
        logger.setLevel(TRACE);

        char val = 'C';
        LogMF.trace(logger, "Iteration {0}", val);
        assertEquals("Iteration C", capture.getMessage());
    }

    /**
     * Test LogMF.trace with single field pattern with boolean argument.
     */
    public void testTraceBoolean() {
        LogCapture capture = new LogCapture(TRACE);
        logger.setLevel(TRACE);

        boolean val = true;
        LogMF.trace(logger, "Iteration {0}", val);
        assertEquals("Iteration true", capture.getMessage());
    }

    /**
     * Test LogMF.trace with single field pattern with float argument.
     */
    public void testTraceFloat() {
        LogCapture capture = new LogCapture(TRACE);
        logger.setLevel(TRACE);

        float val = 3.14f;
        NumberFormat format = NumberFormat.getInstance();
        LogMF.trace(logger, "Iteration {0}", val);
        assertEquals("Iteration "+ format.format(val), capture.getMessage());
    }

    /**
     * Test LogMF.trace with single field pattern with double argument.
     */
    public void testTraceDouble() {
        LogCapture capture = new LogCapture(TRACE);
        logger.setLevel(TRACE);

        double val = 3.14;
        NumberFormat format = NumberFormat.getInstance();
        LogMF.trace(logger, "Iteration {0}", val);
        assertEquals("Iteration "+ format.format(val), capture.getMessage());
    }

    /**
     * Test LogMF.trace with two arguments.
     */
    public void testTraceTwoArg() {
        LogCapture capture = new LogCapture(TRACE);
        logger.setLevel(TRACE);
        LogMF.trace(logger, "{1}, {0}.", "World", "Hello");
        assertEquals("Hello, World.", capture.getMessage());
    }

    /**
     * Test LogMF.trace with three arguments.
     */
    public void testTraceThreeArg() {
        LogCapture capture = new LogCapture(TRACE);
        logger.setLevel(TRACE);
        LogMF.trace(logger, "{1}{2} {0}.", "World", "Hello", ",");
        assertEquals("Hello, World.", capture.getMessage());
    }

    /**
     * Test LogMF.trace with four arguments.
     */
    public void testTraceFourArg() {
        LogCapture capture = new LogCapture(TRACE);
        logger.setLevel(TRACE);
        LogMF.trace(logger, "{1}{2} {0}{3}", "World", "Hello", ",", ".");
        assertEquals("Hello, World.", capture.getMessage());
    }

    /**
     * Test LogMF.trace with Object[] argument.
     */
    public void testTraceArrayArg() {
        LogCapture capture = new LogCapture(TRACE);
        logger.setLevel(TRACE);

        Object[] args = new Object[] { "World", "Hello", ",", "." };
        LogMF.trace(logger, "{1}{2} {0}{3}", args);
        assertEquals("Hello, World.", capture.getMessage());
    }

    /**
     * Test LogMF.trace with null Object[] argument.
     */
    public void testTraceNullArrayArg() {
        LogCapture capture = new LogCapture(TRACE);
        logger.setLevel(TRACE);

        Object[] args = null;
        LogMF.trace(logger, "{1}{2} {0}{3}", args);
        assertEquals("{1}{2} {0}{3}", capture.getMessage());
    }


    /**
     * Test LogMF.debug with null pattern.
     */
    public void testDebugNullPattern() {
        LogCapture capture = new LogCapture(Level.DEBUG);
        LogMF.debug(logger, null, Math.PI);
        assertEquals(null, capture.getMessage());
    }

    /**
     * Test LogMF.debug with no-field pattern.
     */
    public void testDebugNoArg() {
        LogCapture capture = new LogCapture(Level.DEBUG);
        LogMF.debug(logger, "Hello, World", Math.PI);
        assertEquals("Hello, World", capture.getMessage());
    }

    /**
     * Test LogMF.debug with malformed pattern.
     */
    public void testDebugBadPattern() {
        LogCapture capture = new LogCapture(Level.DEBUG);
        LogMF.debug(logger, "Hello, {.", Math.PI);
        assertEquals("Hello, {.", capture.getMessage());
    }

    /**
     * Test LogMF.debug with missing argument.
     */
    public void testDebugMissingArg() {
        LogCapture capture = new LogCapture(Level.DEBUG);
        LogMF.debug(logger, "Hello, {0}World", new Object[0]);
        assertEquals("Hello, {0}World", capture.getMessage());
    }

    /**
     * Test LogMF.debug with single field pattern with string argument.
     */
    public void testDebugString() {
        LogCapture capture = new LogCapture(Level.DEBUG);
        LogMF.debug(logger, "Hello, {0}", "World");
        assertEquals("Hello, World", capture.getMessage());
    }

    /**
     * Test LogMF.debug with single field pattern with null argument.
     */
    public void testDebugNull() {
        LogCapture capture = new LogCapture(Level.DEBUG);
        LogMF.debug(logger, "Hello, {0}", (Object) null);
        assertEquals("Hello, null", capture.getMessage());
    }

    /**
     * Test LogMF.debug with single field pattern with int argument.
     */
    public void testDebugInt() {
        LogCapture capture = new LogCapture(Level.DEBUG);
        int val = 42;
        LogMF.debug(logger, "Iteration {0}", val);
        assertEquals("Iteration 42", capture.getMessage());
    }

    /**
     * Test LogMF.debug with single field pattern with byte argument.
     */
    public void testDebugByte() {
        LogCapture capture = new LogCapture(Level.DEBUG);
        byte val = 42;
        LogMF.debug(logger, "Iteration {0}", val);
        assertEquals("Iteration 42", capture.getMessage());
    }

    /**
     * Test LogMF.debug with single field pattern with short argument.
     */
    public void testDebugShort() {
        LogCapture capture = new LogCapture(Level.DEBUG);
        short val = 42;
        LogMF.debug(logger, "Iteration {0}", val);
        assertEquals("Iteration 42", capture.getMessage());
    }

    /**
     * Test LogMF.debug with single field pattern with long argument.
     */
    public void testDebugLong() {
        LogCapture capture = new LogCapture(Level.DEBUG);
        long val = 42;
        LogMF.debug(logger, "Iteration {0}", val);
        assertEquals("Iteration 42", capture.getMessage());
    }

    /**
     * Test LogMF.debug with single field pattern with char argument.
     */
    public void testDebugChar() {
        LogCapture capture = new LogCapture(Level.DEBUG);
        char val = 'C';
        LogMF.debug(logger, "Iteration {0}", val);
        assertEquals("Iteration C", capture.getMessage());
    }

    /**
     * Test LogMF.debug with single field pattern with boolean argument.
     */
    public void testDebugBoolean() {
        LogCapture capture = new LogCapture(Level.DEBUG);
        boolean val = true;
        LogMF.debug(logger, "Iteration {0}", val);
        assertEquals("Iteration true", capture.getMessage());
    }

    /**
     * Test LogMF.debug with single field pattern with float argument.
     */
    public void testDebugFloat() {
        LogCapture capture = new LogCapture(Level.DEBUG);
        LogMF.debug(logger, "Iteration {0}", (float) Math.PI);

        String expected = MessageFormat.format("Iteration {0}",
                new Object[] { new Float(Math.PI) });
        assertEquals(expected, capture.getMessage());
    }

    /**
     * Test LogMF.debug with single field pattern with double argument.
     */
    public void testDebugDouble() {
        LogCapture capture = new LogCapture(Level.DEBUG);
        LogMF.debug(logger, "Iteration {0}", Math.PI);

        String expected = MessageFormat.format("Iteration {0}",
                new Object[] { new Double(Math.PI) });
        assertEquals(expected, capture.getMessage());
    }

    /**
     * Test LogMF.debug with two arguments.
     */
    public void testDebugTwoArg() {
        LogCapture capture = new LogCapture(Level.DEBUG);
        LogMF.debug(logger, "{1}, {0}.", "World", "Hello");
        assertEquals("Hello, World.", capture.getMessage());
    }

    /**
     * Test LogMF.debug with three arguments.
     */
    public void testDebugThreeArg() {
        LogCapture capture = new LogCapture(Level.DEBUG);
        LogMF.debug(logger, "{1}{2} {0}.", "World", "Hello", ",");
        assertEquals("Hello, World.", capture.getMessage());
    }

    /**
     * Test LogMF.debug with four arguments.
     */
    public void testDebugFourArg() {
        LogCapture capture = new LogCapture(Level.DEBUG);
        LogMF.debug(logger, "{1}{2} {0}{3}", "World", "Hello", ",", ".");
        assertEquals("Hello, World.", capture.getMessage());
    }

    /**
     * Test LogMF.debug with Object[] argument.
     */
    public void testDebugArrayArg() {
        LogCapture capture = new LogCapture(Level.DEBUG);
        Object[] args = new Object[] { "World", "Hello", ",", "." };
        LogMF.debug(logger, "{1}{2} {0}{3}", args);
        assertEquals("Hello, World.", capture.getMessage());
    }

    /**
     * Test LogMF.debug with single field pattern with double argument.
     */
    public void testDebugDate() {
        LogCapture capture = new LogCapture(Level.DEBUG);
        Date epoch = new Date(0);
        LogMF.debug(logger, "Iteration {0}", epoch);

        String expected = MessageFormat.format("Iteration {0}",
                new Object[] { epoch });
        String expected2 = "Iteration " + DateFormat.getDateTimeInstance(
                                DateFormat.SHORT,
                                DateFormat.SHORT).format(epoch);
        String actual = capture.getMessage();
        //
        //  gcj has been observed to output 12/31/69 6:00:00 PM
        //     instead of the expected 12/31/69 6:00 PM
        if (System.getProperty("java.vendor").indexOf("Free") == -1) {
            assertEquals(expected, actual);
        }
        assertEquals(expected2, actual);
    }

    /**
     * Test LogMF.debug with null Object[] argument.
     */
    public void testDebugNullArrayArg() {
        LogCapture capture = new LogCapture(Level.DEBUG);
        Object[] args = null;
        LogMF.debug(logger, "{1}{2} {0}{3}", args);
        assertEquals("{1}{2} {0}{3}", capture.getMessage());
    }

    public void testDebugPercent() {
        LogCapture capture = new LogCapture(Level.DEBUG);
        LogMF.debug(logger, "{0, number, percent}", Math.PI);

        String expected = java.text.MessageFormat.format("{0, number, percent}",
                new Object[] { new Double(Math.PI) });
        assertEquals(expected, capture.getMessage());
    }

    public void testDebugFullPrecisionAndPercent() {
        LogCapture capture = new LogCapture(Level.DEBUG);
        LogMF.debug(logger, "{0}{0, number, percent}", Math.PI);

        String expected = java.text.MessageFormat.format("{0}{0, number, percent}",
                new Object[] { new Double(Math.PI) });
        assertEquals(expected, capture.getMessage());
    }

    public void testDebugQuoted() {
        LogCapture capture = new LogCapture(Level.DEBUG);
        LogMF.debug(logger, "'{0}'", "World");
        assertEquals("{0}", capture.getMessage());
    }

    /**
     * Test LogMF.info with null pattern.
     */
    public void testInfoNullPattern() {
        LogCapture capture = new LogCapture(Level.INFO);
        LogMF.info(logger, null, Math.PI);
        assertNull(capture.getMessage());
    }

    /**
     * Test LogMF.info with no-field pattern.
     */
    public void testInfoNoArg() {
        LogCapture capture = new LogCapture(Level.INFO);
        LogMF.info(logger, "Hello, World", Math.PI);
        assertEquals("Hello, World", capture.getMessage());
    }

    /**
     * Test LogMF.info with malformed pattern.
     */
    public void testInfoBadPattern() {
        LogCapture capture = new LogCapture(Level.INFO);
        LogMF.info(logger, "Hello, {.", Math.PI);
        assertEquals("Hello, {.", capture.getMessage());
    }

    /**
     * Test LogMF.info with missing argument.
     */
    public void testInfoMissingArg() {
        LogCapture capture = new LogCapture(Level.INFO);
        LogMF.info(logger, "Hello, {0}World", new Object[0]);
        assertEquals("Hello, {0}World", capture.getMessage());
    }

    /**
     * Test LogMF.info with single field pattern with string argument.
     */
    public void testInfoString() {
        LogCapture capture = new LogCapture(Level.INFO);
        LogMF.info(logger, "Hello, {0}", "World");
        assertEquals("Hello, World", capture.getMessage());
    }

    /**
     * Test LogMF.info with single field pattern with null argument.
     */
    public void testInfoNull() {
        LogCapture capture = new LogCapture(Level.INFO);
        LogMF.info(logger, "Hello, {0}", (Object) null);
        assertEquals("Hello, null", capture.getMessage());
    }

    /**
     * Test LogMF.info with single field pattern with int argument.
     */
    public void testInfoInt() {
        LogCapture capture = new LogCapture(Level.INFO);
        int val = 42;
        LogMF.info(logger, "Iteration {0}", val);
        assertEquals("Iteration 42", capture.getMessage());
    }

    /**
     * Test LogMF.info with single field pattern with byte argument.
     */
    public void testInfoByte() {
        LogCapture capture = new LogCapture(Level.INFO);
        byte val = 42;
        LogMF.info(logger, "Iteration {0}", val);
        assertEquals("Iteration 42", capture.getMessage());
    }

    /**
     * Test LogMF.info with single field pattern with short argument.
     */
    public void testInfoShort() {
        LogCapture capture = new LogCapture(Level.INFO);
        short val = 42;
        LogMF.info(logger, "Iteration {0}", val);
        assertEquals("Iteration 42", capture.getMessage());
    }

    /**
     * Test LogMF.info with single field pattern with long argument.
     */
    public void testInfoLong() {
        LogCapture capture = new LogCapture(Level.INFO);
        long val = 42;
        LogMF.info(logger, "Iteration {0}", val);
        assertEquals("Iteration 42", capture.getMessage());
    }

    /**
     * Test LogMF.info with single field pattern with char argument.
     */
    public void testInfoChar() {
        LogCapture capture = new LogCapture(Level.INFO);
        char val = 'C';
        LogMF.info(logger, "Iteration {0}", val);
        assertEquals("Iteration C", capture.getMessage());
    }

    /**
     * Test LogMF.info with single field pattern with boolean argument.
     */
    public void testInfoBoolean() {
        LogCapture capture = new LogCapture(Level.INFO);
        boolean val = true;
        LogMF.info(logger, "Iteration {0}", val);
        assertEquals("Iteration true", capture.getMessage());
    }

    /**
     * Test LogMF.info with single field pattern with float argument.
     */
    public void testInfoFloat() {
        LogCapture capture = new LogCapture(Level.INFO);
        LogMF.info(logger, "Iteration {0}", (float) Math.PI);

        String expected = MessageFormat.format("Iteration {0}",
                new Object[] { new Float(Math.PI) });
        assertEquals(expected, capture.getMessage());
    }

    /**
     * Test LogMF.info with single field pattern with double argument.
     */
    public void testInfoDouble() {
        LogCapture capture = new LogCapture(Level.INFO);
        LogMF.info(logger, "Iteration {0}", Math.PI);

        String expected = MessageFormat.format("Iteration {0}",
                new Object[] { new Double(Math.PI) });
        assertEquals(expected, capture.getMessage());
    }

    /**
     * Test LogMF.info with two arguments.
     */
    public void testInfoTwoArg() {
        LogCapture capture = new LogCapture(Level.INFO);
        LogMF.info(logger, "{1}, {0}.", "World", "Hello");
        assertEquals("Hello, World.", capture.getMessage());
    }

    /**
     * Test LogMF.info with three arguments.
     */
    public void testInfoThreeArg() {
        LogCapture capture = new LogCapture(Level.INFO);
        LogMF.info(logger, "{1}{2} {0}.", "World", "Hello", ",");
        assertEquals("Hello, World.", capture.getMessage());
    }

    /**
     * Test LogMF.info with four arguments.
     */
    public void testInfoFourArg() {
        LogCapture capture = new LogCapture(Level.INFO);
        LogMF.info(logger, "{1}{2} {0}{3}", "World", "Hello", ",", ".");
        assertEquals("Hello, World.", capture.getMessage());
    }

    /**
     * Test LogMF.info with Object[] argument.
     */
    public void testInfoArrayArg() {
        LogCapture capture = new LogCapture(Level.INFO);
        Object[] args = new Object[] { "World", "Hello", ",", "." };
        LogMF.info(logger, "{1}{2} {0}{3}", args);
        assertEquals("Hello, World.", capture.getMessage());
    }

    /**
     * Test LogMF.warn with null pattern.
     */
    public void testWarnNullPattern() {
        LogCapture capture = new LogCapture(Level.WARN);
        LogMF.warn(logger, null, Math.PI);
        assertNull(capture.getMessage());
    }

    /**
     * Test LogMF.warn with no-field pattern.
     */
    public void testWarnNoArg() {
        LogCapture capture = new LogCapture(Level.WARN);
        LogMF.warn(logger, "Hello, World", Math.PI);
        assertEquals("Hello, World", capture.getMessage());
    }

    /**
     * Test LogMF.warn with malformed pattern.
     */
    public void testWarnBadPattern() {
        LogCapture capture = new LogCapture(Level.WARN);
        LogMF.warn(logger, "Hello, {.", Math.PI);
        assertEquals("Hello, {.", capture.getMessage());
    }

    /**
     * Test LogMF.warn with missing argument.
     */
    public void testWarnMissingArg() {
        LogCapture capture = new LogCapture(Level.WARN);
        LogMF.warn(logger, "Hello, {0}World", new Object[0]);
        assertEquals("Hello, {0}World", capture.getMessage());
    }

    /**
     * Test LogMF.warn with single field pattern with string argument.
     */
    public void testWarnString() {
        LogCapture capture = new LogCapture(Level.WARN);
        LogMF.warn(logger, "Hello, {0}", "World");
        assertEquals("Hello, World", capture.getMessage());
    }

    /**
     * Test LogMF.warn with single field pattern with null argument.
     */
    public void testWarnNull() {
        LogCapture capture = new LogCapture(Level.WARN);
        LogMF.warn(logger, "Hello, {0}", (Object) null);
        assertEquals("Hello, null", capture.getMessage());
    }

    /**
     * Test LogMF.warn with single field pattern with int argument.
     */
    public void testWarnInt() {
        LogCapture capture = new LogCapture(Level.WARN);
        int val = 42;
        LogMF.warn(logger, "Iteration {0}", val);
        assertEquals("Iteration 42", capture.getMessage());
    }

    /**
     * Test LogMF.warn with single field pattern with byte argument.
     */
    public void testWarnByte() {
        LogCapture capture = new LogCapture(Level.WARN);
        byte val = 42;
        LogMF.warn(logger, "Iteration {0}", val);
        assertEquals("Iteration 42", capture.getMessage());
    }

    /**
     * Test LogMF.warn with single field pattern with short argument.
     */
    public void testWarnShort() {
        LogCapture capture = new LogCapture(Level.WARN);
        short val = 42;
        LogMF.warn(logger, "Iteration {0}", val);
        assertEquals("Iteration 42", capture.getMessage());
    }

    /**
     * Test LogMF.warn with single field pattern with long argument.
     */
    public void testWarnLong() {
        LogCapture capture = new LogCapture(Level.WARN);
        long val = 42;
        LogMF.warn(logger, "Iteration {0}", val);
        assertEquals("Iteration 42", capture.getMessage());
    }

    /**
     * Test LogMF.warn with single field pattern with char argument.
     */
    public void testWarnChar() {
        LogCapture capture = new LogCapture(Level.WARN);
        char val = 'C';
        LogMF.warn(logger, "Iteration {0}", val);
        assertEquals("Iteration C", capture.getMessage());
    }

    /**
     * Test LogMF.warn with single field pattern with boolean argument.
     */
    public void testWarnBoolean() {
        LogCapture capture = new LogCapture(Level.WARN);
        boolean val = true;
        LogMF.warn(logger, "Iteration {0}", val);
        assertEquals("Iteration true", capture.getMessage());
    }

    /**
     * Test LogMF.warn with single field pattern with float argument.
     */
    public void testWarnFloat() {
        LogCapture capture = new LogCapture(Level.WARN);
        LogMF.warn(logger, "Iteration {0}", (float) Math.PI);

        String expected = MessageFormat.format("Iteration {0}",
                new Object[] { new Float(Math.PI) });
        assertEquals(expected, capture.getMessage());
    }

    /**
     * Test LogMF.debug with single field pattern with double argument.
     */
    public void testWarnDouble() {
        LogCapture capture = new LogCapture(Level.WARN);
        LogMF.warn(logger, "Iteration {0}", Math.PI);

        String expected = MessageFormat.format("Iteration {0}",
                new Object[] { new Double(Math.PI) });
        assertEquals(expected, capture.getMessage());
    }

    /**
     * Test LogMF.warn with two arguments.
     */
    public void testWarnTwoArg() {
        LogCapture capture = new LogCapture(Level.WARN);
        LogMF.warn(logger, "{1}, {0}.", "World", "Hello");
        assertEquals("Hello, World.", capture.getMessage());
    }

    /**
     * Test LogMF.warn with three arguments.
     */
    public void testWarnThreeArg() {
        LogCapture capture = new LogCapture(Level.WARN);
        LogMF.warn(logger, "{1}{2} {0}.", "World", "Hello", ",");
        assertEquals("Hello, World.", capture.getMessage());
    }

    /**
     * Test LogMF.debug with four arguments.
     */
    public void testWarnFourArg() {
        LogCapture capture = new LogCapture(Level.WARN);
        LogMF.warn(logger, "{1}{2} {0}{3}", "World", "Hello", ",", ".");
        assertEquals("Hello, World.", capture.getMessage());
    }

    /**
     * Test LogMF.warn with Object[] argument.
     */
    public void testWarnArrayArg() {
        LogCapture capture = new LogCapture(Level.WARN);
        Object[] args = new Object[] { "World", "Hello", ",", "." };
        LogMF.warn(logger, "{1}{2} {0}{3}", args);
        assertEquals("Hello, World.", capture.getMessage());
    }

    /**
     * Test LogMF.log with null pattern.
     */
    public void testLogNullPattern() {
        LogCapture capture = new LogCapture(Level.ERROR);
        LogMF.log(logger, Level.ERROR, null, Math.PI);
        assertNull(capture.getMessage());
    }

    /**
     * Test LogMF.log with no-field pattern.
     */
    public void testLogNoArg() {
        LogCapture capture = new LogCapture(Level.ERROR);
        LogMF.log(logger, Level.ERROR, "Hello, World", Math.PI);
        assertEquals("Hello, World", capture.getMessage());
    }

    /**
     * Test LogMF.log with malformed pattern.
     */
    public void testLogBadPattern() {
        LogCapture capture = new LogCapture(Level.ERROR);
        LogMF.log(logger, Level.ERROR, "Hello, {.", Math.PI);
        assertEquals("Hello, {.", capture.getMessage());
    }

    /**
     * Test LogMF.log with missing argument.
     */
    public void testLogMissingArg() {
        LogCapture capture = new LogCapture(Level.ERROR);
        LogMF.log(logger, Level.ERROR, "Hello, {0}World", new Object[0]);
        assertEquals("Hello, {0}World", capture.getMessage());
    }

    /**
     * Test LogMF.log with single field pattern with string argument.
     */
    public void testLogString() {
        LogCapture capture = new LogCapture(Level.ERROR);
        LogMF.log(logger, Level.ERROR, "Hello, {0}", "World");
        assertEquals("Hello, World", capture.getMessage());
    }

    /**
     * Test LogMF.log with single field pattern with null argument.
     */
    public void testLogNull() {
        LogCapture capture = new LogCapture(Level.ERROR);
        LogMF.log(logger, Level.ERROR, "Hello, {0}", (Object) null);
        assertEquals("Hello, null", capture.getMessage());
    }

    /**
     * Test LogMF.log with single field pattern with int argument.
     */
    public void testLogInt() {
        LogCapture capture = new LogCapture(Level.ERROR);
        int val = 42;
        LogMF.log(logger, Level.ERROR, "Iteration {0}", val);
        assertEquals("Iteration 42", capture.getMessage());
    }

    /**
     * Test LogMF.log with single field pattern with byte argument.
     */
    public void testLogByte() {
        LogCapture capture = new LogCapture(Level.ERROR);
        byte val = 42;
        LogMF.log(logger, Level.ERROR, "Iteration {0}", val);
        assertEquals("Iteration 42", capture.getMessage());
    }

    /**
     * Test LogMF.log with single field pattern with short argument.
     */
    public void testLogShort() {
        LogCapture capture = new LogCapture(Level.ERROR);
        short val = 42;
        LogMF.log(logger, Level.ERROR, "Iteration {0}", val);
        assertEquals("Iteration 42", capture.getMessage());
    }

    /**
     * Test LogMF.log with single field pattern with long argument.
     */
    public void testLogLong() {
        LogCapture capture = new LogCapture(Level.ERROR);
        long val = 42;
        LogMF.log(logger, Level.ERROR, "Iteration {0}", val);
        assertEquals("Iteration 42", capture.getMessage());
    }

    /**
     * Test LogMF.log with single field pattern with char argument.
     */
    public void testLogChar() {
        LogCapture capture = new LogCapture(Level.ERROR);
        char val = 'C';
        LogMF.log(logger, Level.ERROR, "Iteration {0}", val);
        assertEquals("Iteration C", capture.getMessage());
    }

    /**
     * Test LogMF.log with single field pattern with boolean argument.
     */
    public void testLogBoolean() {
        LogCapture capture = new LogCapture(Level.ERROR);
        boolean val = true;
        LogMF.log(logger, Level.ERROR, "Iteration {0}", val);
        assertEquals("Iteration true", capture.getMessage());
    }

    /**
     * Test LogMF.log with single field pattern with float argument.
     */
    public void testLogFloat() {
        LogCapture capture = new LogCapture(Level.ERROR);
        LogMF.log(logger, Level.ERROR, "Iteration {0}", (float) Math.PI);

        String expected = MessageFormat.format("Iteration {0}",
                new Object[] { new Float(Math.PI) });
        assertEquals(expected, capture.getMessage());
    }

    /**
     * Test LogMF.log with single field pattern with double argument.
     */
    public void testLogDouble() {
        LogCapture capture = new LogCapture(Level.ERROR);
        LogMF.log(logger, Level.ERROR, "Iteration {0}", Math.PI);

        String expected = MessageFormat.format("Iteration {0}",
                new Object[] { new Double(Math.PI) });
        assertEquals(expected, capture.getMessage());
    }

    /**
     * Test LogMF.log with two arguments.
     */
    public void testLogTwoArg() {
        LogCapture capture = new LogCapture(Level.ERROR);
        LogMF.log(logger, Level.ERROR, "{1}, {0}.", "World", "Hello");
        assertEquals("Hello, World.", capture.getMessage());
    }

    /**
     * Test LogMF.log with three arguments.
     */
    public void testLogThreeArg() {
        LogCapture capture = new LogCapture(Level.ERROR);
        LogMF.log(logger, Level.ERROR, "{1}{2} {0}.", "World", "Hello", ",");
        assertEquals("Hello, World.", capture.getMessage());
    }

    /**
     * Test LogMF.log with four arguments.
     */
    public void testLogFourArg() {
        LogCapture capture = new LogCapture(Level.ERROR);
        LogMF.log(logger, Level.ERROR, "{1}{2} {0}{3}", "World", "Hello", ",", ".");
        assertEquals("Hello, World.", capture.getMessage());
    }

    /**
     * Test LogMF.log with Object[] argument.
     */
    public void testLogArrayArg() {
        LogCapture capture = new LogCapture(Level.ERROR);
        Object[] args = new Object[] { "World", "Hello", ",", "." };
        LogMF.log(logger, Level.ERROR, "{1}{2} {0}{3}", args);
        assertEquals("Hello, World.", capture.getMessage());
    }

    /**
     * Bundle name for resource bundle tests.
     */
    private static final String BUNDLE_NAME =
            "org.apache.log4j.TestLogMFPatterns";

    /**
     * Test LogMF.logrb with null bundle name.
     */
    public void testLogrbNullBundle() {
        LogCapture capture = new LogCapture(Level.ERROR);
        LogMF.logrb(logger, Level.ERROR, null, "Iteration0", Math.PI);
        assertEquals("Iteration0", capture.getMessage());
    }

    /**
     * Test LogMF.logrb with null key.
     */
    public void testLogrbNullKey() {
        LogCapture capture = new LogCapture(Level.ERROR);
        LogMF.logrb(logger, Level.ERROR, BUNDLE_NAME, null, Math.PI);
        assertNull(capture.getMessage());
    }

    /**
     * Test LogMF.logrb with no-field pattern.
     */
    public void testLogrbNoArg() {
        LogCapture capture = new LogCapture(Level.ERROR);
        LogMF.logrb(logger, Level.ERROR, BUNDLE_NAME, "Hello1", Math.PI);
        assertEquals("Hello, World", capture.getMessage());
    }

    /**
     * Test LogMF.logrb with malformed pattern.
     */
    public void testLogrbBadPattern() {
        LogCapture capture = new LogCapture(Level.ERROR);
        LogMF.logrb(logger, Level.ERROR, BUNDLE_NAME, "Malformed", Math.PI);
        assertEquals("Hello, {.", capture.getMessage());
    }

    /**
     * Test LogMF.logrb with missing argument.
     */
    public void testLogrbMissingArg() {
        LogCapture capture = new LogCapture(Level.ERROR);
        LogMF.logrb(logger, Level.ERROR, BUNDLE_NAME, "Hello2", new Object[0]);
        assertEquals("Hello, {0}World", capture.getMessage());
    }

    /**
     * Test LogMF.logrb with single field pattern with string argument.
     */
    public void testLogrbString() {
        LogCapture capture = new LogCapture(Level.ERROR);
        LogMF.logrb(logger, Level.ERROR, BUNDLE_NAME, "Hello3", "World");
        assertEquals("Hello, World", capture.getMessage());
    }

    /**
     * Test LogMF.logrb with single field pattern with null argument.
     */
    public void testLogrbNull() {
        LogCapture capture = new LogCapture(Level.ERROR);
        LogMF.logrb(logger, Level.ERROR, BUNDLE_NAME, "Hello3", (Object) null);
        assertEquals("Hello, null", capture.getMessage());
    }

    /**
     * Test LogMF.logrb with single field pattern with int argument.
     */
    public void testLogrbInt() {
        LogCapture capture = new LogCapture(Level.ERROR);
        int val = 42;
        LogMF.logrb(logger, Level.ERROR, BUNDLE_NAME, "Iteration0", val);
        assertEquals("Iteration 42", capture.getMessage());
    }

    /**
     * Test LogMF.logrb with single field pattern with byte argument.
     */
    public void testLogrbByte() {
        LogCapture capture = new LogCapture(Level.ERROR);
        byte val = 42;
        LogMF.logrb(logger, Level.ERROR, BUNDLE_NAME, "Iteration0", val);
        assertEquals("Iteration 42", capture.getMessage());
    }

    /**
     * Test LogMF.logrb with single field pattern with short argument.
     */
    public void testLogrbShort() {
        LogCapture capture = new LogCapture(Level.ERROR);
        short val = 42;
        LogMF.logrb(logger, Level.ERROR, BUNDLE_NAME, "Iteration0", val);
        assertEquals("Iteration 42", capture.getMessage());
    }

    /**
     * Test LogMF.logrb with single field pattern with long argument.
     */
    public void testLogrbLong() {
        LogCapture capture = new LogCapture(Level.ERROR);
        long val = 42;
        LogMF.logrb(logger, Level.ERROR, BUNDLE_NAME, "Iteration0", val);
        assertEquals("Iteration 42", capture.getMessage());
    }

    /**
     * Test LogMF.logrb with single field pattern with char argument.
     */
    public void testLogrbChar() {
        LogCapture capture = new LogCapture(Level.ERROR);
        char val = 'C';
        LogMF.logrb(logger, Level.ERROR, BUNDLE_NAME, "Iteration0", val);
        assertEquals("Iteration C", capture.getMessage());
    }

    /**
     * Test LogMF.logrb with single field pattern with boolean argument.
     */
    public void testLogrbBoolean() {
        LogCapture capture = new LogCapture(Level.ERROR);
        boolean val = true;
        LogMF.logrb(logger, Level.ERROR, BUNDLE_NAME, "Iteration0", val);
        assertEquals("Iteration true", capture.getMessage());
    }

    /**
     * Test LogMF.logrb with single field pattern with float argument.
     */
    public void testLogrbFloat() {
        LogCapture capture = new LogCapture(Level.ERROR);
        LogMF.logrb(logger, Level.ERROR, BUNDLE_NAME, "Iteration0", (float) Math.PI);

        String expected = MessageFormat.format("Iteration {0}",
                new Object[] { new Float(Math.PI) });
        assertEquals(expected, capture.getMessage());
    }

    /**
     * Test LogMF.logrb with single field pattern with double argument.
     */
    public void testLogrbDouble() {
        LogCapture capture = new LogCapture(Level.ERROR);
        LogMF.logrb(logger, Level.ERROR, BUNDLE_NAME, "Iteration0", Math.PI);

        String expected = MessageFormat.format("Iteration {0}",
                new Object[] { new Double(Math.PI) });
        assertEquals(expected, capture.getMessage());
    }

    /**
     * Test LogMF.logrb with two arguments.
     */
    public void testLogrbTwoArg() {
        LogCapture capture = new LogCapture(Level.ERROR);
        LogMF.logrb(logger, Level.ERROR,
                BUNDLE_NAME, "Hello4", "World", "Hello");
        assertEquals("Hello, World.", capture.getMessage());
    }

    /**
     * Test LogMF.logrb with three arguments.
     */
    public void testLogrbThreeArg() {
        LogCapture capture = new LogCapture(Level.ERROR);
        LogMF.logrb(logger, Level.ERROR,
                BUNDLE_NAME, "Hello5", "World", "Hello", ",");
        assertEquals("Hello, World.", capture.getMessage());
    }

    /**
     * Test LogMF.logrb with four arguments.
     */
    public void testLogrbFourArg() {
        LogCapture capture = new LogCapture(Level.ERROR);
        LogMF.logrb(logger, Level.ERROR,
                BUNDLE_NAME, "Hello6", "World", "Hello", ",", ".");
        assertEquals("Hello, World.", capture.getMessage());
    }

    /**
     * Test LogMF.logrb with Object[] argument.
     */
    public void testLogrbArrayArg() {
        LogCapture capture = new LogCapture(Level.ERROR);
        Object[] args = new Object[] { "World", "Hello", ",", "." };
        LogMF.logrb(logger, Level.ERROR,
                BUNDLE_NAME, "Hello6", args);
        assertEquals("Hello, World.", capture.getMessage());
    }


    /**
     * Test LogMF.info with a pattern containing {9} and one argument.
     */
    public void testInfo1ParamBrace9() {
        LogCapture capture = new LogCapture(Level.INFO);
        LogMF.info(logger, "Hello, {9}{0}", "World");
        assertEquals("Hello, {9}World", capture.getMessage());
    }

    /**
     * Test LogMF.info with a pattern containing {9} and two arguments.
     */
    public void testInfo2ParamBrace9() {
        LogCapture capture = new LogCapture(Level.INFO);
        LogMF.info(logger, "{1}, {9}{0}", "World", "Hello");
        assertEquals("Hello, {9}World", capture.getMessage());
    }

    /**
     * Test LogMF.info with a pattern containing {9} and two arguments.
     */
    public void testInfo10ParamBrace9() {
        LogCapture capture = new LogCapture(Level.INFO);
        LogMF.info(logger, "{1}, {9}{0}",
                new Object[] { "World", "Hello", null, null, null,
                                null, null, null, null, "New " });
        assertEquals("Hello, New World", capture.getMessage());
    }

    /**
     * Test LogMF.info with indexes just outside of 0 to 9.
     */
    public void testInfo1ParamBraceSlashColon() {
        LogCapture capture = new LogCapture(Level.INFO);
        String pattern = "Hello, {/}{0}{:}";
        LogMF.info(logger, pattern, "World");
        assertEquals(pattern, capture.getMessage());
    }


}
