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

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.spi.OptionHandler;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableRenderer;
import org.apache.log4j.spi.ThrowableRendererSupport;
import org.apache.log4j.varia.LevelRangeFilter;

/**
 * Test property configurator.
 *
 */
public class PropertyConfiguratorTest extends TestCase {
    public PropertyConfiguratorTest(final String testName) {
        super(testName);
    }

    /**
     * Test for bug 40944.
     * Did not catch IllegalArgumentException on Properties.load
     * and close input stream.
     * @throws IOException if IOException creating properties file.
     */
    public void testBadUnicodeEscape() throws IOException {
        String fileName = "output/badescape.properties";
        FileWriter writer = new FileWriter(fileName);
        writer.write("log4j.rootLogger=\\uXX41");
        writer.close();
        PropertyConfigurator.configure(fileName);
        File file = new File(fileName);
        assertTrue(file.delete()) ;
        assertFalse(file.exists());
    }

    /**
     * Test for bug 40944.
     * configure(URL) never closed opened stream.
     * @throws IOException if IOException creating properties file.
     */
        public void testURL() throws IOException {
        File file = new File("output/unclosed.properties");
        FileWriter writer = new FileWriter(file);
        writer.write("log4j.rootLogger=debug");
        writer.close();
        URL url = file.toURL();
        PropertyConfigurator.configure(url);
        assertTrue(file.delete());
        assertFalse(file.exists());
    }

    /**
     * Test for bug 40944.
     * configure(URL) did not catch IllegalArgumentException and
     * did not close stream.
     * @throws IOException if IOException creating properties file.
     */
        public void testURLBadEscape() throws IOException {
        File file = new File("output/urlbadescape.properties");
        FileWriter writer = new FileWriter(file);
        writer.write("log4j.rootLogger=\\uXX41");
        writer.close();
        URL url = file.toURL();
        PropertyConfigurator.configure(url);
        assertTrue(file.delete());
        assertFalse(file.exists());
    }

    /**
     * Test for bug 47465.
     * configure(URL) did not close opened JarURLConnection.
     * @throws IOException if IOException creating properties jar.
     */
    public void testJarURL() throws IOException {
        File dir = new File("output");
        dir.mkdirs();
        File file = new File("output/properties.jar");
        ZipOutputStream zos =
            new ZipOutputStream(new FileOutputStream(file));
        zos.putNextEntry(new ZipEntry(LogManager.DEFAULT_CONFIGURATION_FILE));
        zos.write("log4j.rootLogger=debug".getBytes());
        zos.closeEntry();
        zos.close();
        URL url = new URL("jar:" + file.toURL() + "!/" +
                LogManager.DEFAULT_CONFIGURATION_FILE);
        PropertyConfigurator.configure(url);
        assertTrue(file.delete());
        assertFalse(file.exists());
    }

    /**
     * Test processing of log4j.reset property, see bug 17531.
     *
     */
    public void testReset() {
        VectorAppender appender = new VectorAppender();
        appender.setName("A1");
        Logger.getRootLogger().addAppender(appender);
        Properties props = new Properties();
        props.put("log4j.reset", "true");
        PropertyConfigurator.configure(props);
        assertNull(Logger.getRootLogger().getAppender("A1"));
        LogManager.resetConfiguration();
    }


    /**
     * Mock definition of org.apache.log4j.rolling.RollingPolicy
     * from extras companion.
     */
    public static class RollingPolicy implements OptionHandler {
        private boolean activated = false;

        public RollingPolicy() {

        }
        public void activateOptions() {
            activated = true;
        }

        public final boolean isActivated() {
            return activated;
        }

    }

    /**
     * Mock definition of FixedWindowRollingPolicy from extras companion.
     */
    public static final class FixedWindowRollingPolicy extends RollingPolicy {
        private String activeFileName;
        private String fileNamePattern;
        private int minIndex;

        public FixedWindowRollingPolicy() {
            minIndex = -1;
        }

        public String getActiveFileName() {
            return activeFileName;
        }
        public void setActiveFileName(final String val) {
            activeFileName = val;
        }

        public String getFileNamePattern() {
            return fileNamePattern;
        }
        public void setFileNamePattern(final String val) {
            fileNamePattern = val;
        }

        public int getMinIndex() {
            return minIndex;
        }

        public void setMinIndex(final int val) {
            minIndex = val;
        }
    }

    /**
     * Mock definition of TriggeringPolicy from extras companion.
     */
    public static class TriggeringPolicy implements OptionHandler {
        private boolean activated = false;

        public TriggeringPolicy() {

        }
        public void activateOptions() {
            activated = true;
        }

        public final boolean isActivated() {
            return activated;
        }

    }

    /**
     * Mock definition of FilterBasedTriggeringPolicy from extras companion.
     */
    public static final class FilterBasedTriggeringPolicy extends TriggeringPolicy {
        private Filter filter;
        public FilterBasedTriggeringPolicy() {
        }

        public void setFilter(final Filter val) {
             filter = val;
        }

        public Filter getFilter() {
            return filter;

        }
    }

    /**
     * Mock definition of org.apache.log4j.rolling.RollingFileAppender
     * from extras companion.
     */
    public static final class RollingFileAppender extends AppenderSkeleton {
        private RollingPolicy rollingPolicy;
        private TriggeringPolicy triggeringPolicy;
        private boolean append;

        public RollingFileAppender() {

        }

        public RollingPolicy getRollingPolicy() {
            return rollingPolicy;
        }

        public void setRollingPolicy(final RollingPolicy policy) {
            rollingPolicy = policy;
        }

        public TriggeringPolicy getTriggeringPolicy() {
            return triggeringPolicy;
        }

        public void setTriggeringPolicy(final TriggeringPolicy policy) {
            triggeringPolicy = policy;
        }

        public boolean getAppend() {
            return append;
        }

        public void setAppend(boolean val) {
            append = val;
        }

        public void close() {

        }

        public boolean requiresLayout() {
            return true;
        }

        public void append(final LoggingEvent event) {

        }
    }

    /**
     * Test processing of nested objects, see bug 36384.
     *
     */
    public void testNested() {
        PropertyConfigurator.configure("input/filter1.properties");
        RollingFileAppender rfa = (RollingFileAppender)
                Logger.getLogger("org.apache.log4j.PropertyConfiguratorTest")
                   .getAppender("ROLLING");
        FixedWindowRollingPolicy rollingPolicy = (FixedWindowRollingPolicy) rfa.getRollingPolicy();
        assertEquals("filterBase-test1.log", rollingPolicy.getActiveFileName());
        assertEquals("filterBased-test1.%i", rollingPolicy.getFileNamePattern());
        assertEquals(0, rollingPolicy.getMinIndex());
        assertTrue(rollingPolicy.isActivated());
        FilterBasedTriggeringPolicy triggeringPolicy =
                (FilterBasedTriggeringPolicy) rfa.getTriggeringPolicy();
        LevelRangeFilter filter = (LevelRangeFilter) triggeringPolicy.getFilter();
        assertTrue(Level.INFO.equals(filter.getLevelMin()));
        LogManager.resetConfiguration();
    }


    /**
     * Mock ThrowableRenderer for testThrowableRenderer.  See bug 45721.
     */
    public static class MockThrowableRenderer implements ThrowableRenderer, OptionHandler {
        private boolean activated = false;
        private boolean showVersion = true;

        public MockThrowableRenderer() {
        }

        public void activateOptions() {
            activated = true;
        }

        public boolean isActivated() {
            return activated;
        }

        public String[] doRender(final Throwable t) {
            return new String[0];
        }

        public void setShowVersion(boolean v) {
            showVersion = v;
        }

        public boolean getShowVersion() {
            return showVersion;
        }
    }

    /**
     * Test of log4j.throwableRenderer support.  See bug 45721.
     */
    public void testThrowableRenderer() {
        Properties props = new Properties();
        props.put("log4j.throwableRenderer", "org.apache.log4j.PropertyConfiguratorTest$MockThrowableRenderer");
        props.put("log4j.throwableRenderer.showVersion", "false");
        PropertyConfigurator.configure(props);
        ThrowableRendererSupport repo = (ThrowableRendererSupport) LogManager.getLoggerRepository();
        MockThrowableRenderer renderer = (MockThrowableRenderer) repo.getThrowableRenderer();
        LogManager.resetConfiguration();
        assertNotNull(renderer);
        assertEquals(true, renderer.isActivated());
        assertEquals(false, renderer.getShowVersion());
    }
}
