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

import org.apache.log4j.spi.LoggingEvent;


/**
 * Test for PatternLayout.
 *
 * @author Curt Arnold
 */
public class PatternLayoutTest extends LayoutTest {
  /**
   * Construct new instance of PatternLayoutTest.
   *
   * @param testName test name.
   */
  public PatternLayoutTest(final String testName) {
    super(testName, "text/plain", true, null, null);
  }

  /**
   * @{inheritDoc}
   */
  protected Layout createLayout() {
    return new PatternLayout("[%t] %p %c - %m%n");
  }

  /**
   * Tests format.
   */
  public void testFormat() {
    Logger logger = Logger.getLogger("org.apache.log4j.LayoutTest");
    LoggingEvent event =
      new LoggingEvent(
        "org.apache.log4j.Logger", logger, Level.INFO, "Hello, World", null);
    PatternLayout layout = (PatternLayout) createLayout();
    String result = layout.format(event);
    StringBuffer buf = new StringBuffer(100);
    buf.append('[');
    buf.append(event.getThreadName());
    buf.append("] ");
    buf.append(event.getLevel().toString());
    buf.append(' ');
    buf.append(event.getLoggerName());
    buf.append(" - ");
    buf.append(event.getMessage());
    buf.append(System.getProperty("line.separator"));
    assertEquals(buf.toString(), result);
  }

  /**
   * Tests getPatternFormat().
   */
  public void testGetPatternFormat() {
    PatternLayout layout = (PatternLayout) createLayout();
    assertEquals("[%t] %p %c - %m%n", layout.getConversionPattern());
  }

  /**
   * Tests DEFAULT_CONVERSION_PATTERN constant.
   */
  public void testDefaultConversionPattern() {
    assertEquals("%m%n", PatternLayout.DEFAULT_CONVERSION_PATTERN);
  }

  /**
   * Tests DEFAULT_CONVERSION_PATTERN constant.
   */
  public void testTTCCConversionPattern() {
    assertEquals(
      "%r [%t] %p %c %x - %m%n", PatternLayout.TTCC_CONVERSION_PATTERN);
  }

  /**
   * Tests buffer downsizing code path.
   */
  public void testFormatResize() {
    Logger logger = Logger.getLogger("org.apache.log4j.xml.PatternLayoutTest");
    NDC.clear();

    char[] msg = new char[2000];

    for (int i = 0; i < msg.length; i++) {
      msg[i] = 'A';
    }

    LoggingEvent event1 =
      new LoggingEvent(
        "org.apache.log4j.Logger", logger, Level.DEBUG, new String(msg), null);
    PatternLayout layout = (PatternLayout) createLayout();
    String result = layout.format(event1);
    LoggingEvent event2 =
      new LoggingEvent(
        "org.apache.log4j.Logger", logger, Level.WARN, "Hello, World", null);
    result = layout.format(event2);
    assertEquals("[", result.substring(0, 1));
  }

  /**
   * Class to ensure that protected members are still available.
   */
  public static final class DerivedPatternLayout extends PatternLayout {
    /**
     * Constructs a new instance of DerivedPatternLayout.
     */
    public DerivedPatternLayout() {
    }

    /**
     * Get BUF_SIZE.
     * @return return initial buffer size in characters.
     */
    public int getBufSize() {
      return BUF_SIZE;
    }

    /**
     * Get MAX_CAPACITY.
     * @return maximum capacity in characters.
     */
    public int getMaxCapacity() {
      return MAX_CAPACITY;
    }
  }
}
