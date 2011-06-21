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

import org.apache.log4j.helpers.DateLayoutTest;
import org.apache.log4j.spi.LoggingEvent;


/**
 * Test for TTCCLayout.
 *
 * @author Curt Arnold
 */
public class TTCCLayoutTest extends DateLayoutTest {
  /**
   * Construct new instance of TTCCLayoutTest.
   *
   * @param testName test name.
   */
  public TTCCLayoutTest(final String testName) {
    super(testName, "text/plain", true, null, null);
  }

  /**
   * @{inheritDoc}
   */
  protected Layout createLayout() {
    return new TTCCLayout();
  }

  /**
   * Tests format.
   */
  public void testFormat() {
    NDC.clear();
    NDC.push("NDC goes here");

    Logger logger = Logger.getLogger("org.apache.log4j.LayoutTest");
    LoggingEvent event =
      new LoggingEvent(
        "org.apache.log4j.Logger", logger, Level.INFO, "Hello, World", null);
    TTCCLayout layout = (TTCCLayout) createLayout();
    String result = layout.format(event);
    NDC.pop();

    StringBuffer buf = new StringBuffer(100);
    layout.dateFormat(buf, event);
    buf.append('[');
    buf.append(event.getThreadName());
    buf.append("] ");
    buf.append(event.getLevel().toString());
    buf.append(' ');
    buf.append(event.getLoggerName());
    buf.append(' ');
    buf.append("NDC goes here");
    buf.append(" - ");
    buf.append(event.getMessage());
    buf.append(System.getProperty("line.separator"));
    assertEquals(buf.toString(), result);
  }

  /**
   * Tests getThreadPrinting and setThreadPrinting.
   */
  public void testGetSetThreadPrinting() {
    TTCCLayout layout = new TTCCLayout();
    assertEquals(true, layout.getThreadPrinting());
    layout.setThreadPrinting(false);
    assertEquals(false, layout.getThreadPrinting());
    layout.setThreadPrinting(true);
    assertEquals(true, layout.getThreadPrinting());
  }

  /**
   * Tests getCategoryPrefixing and setCategoryPrefixing.
   */
  public void testGetSetCategoryPrefixing() {
    TTCCLayout layout = new TTCCLayout();
    assertEquals(true, layout.getCategoryPrefixing());
    layout.setCategoryPrefixing(false);
    assertEquals(false, layout.getCategoryPrefixing());
    layout.setCategoryPrefixing(true);
    assertEquals(true, layout.getCategoryPrefixing());
  }

  /**
   * Tests getContextPrinting and setContextPrinting.
   */
  public void testGetSetContextPrinting() {
    TTCCLayout layout = new TTCCLayout();
    assertEquals(true, layout.getContextPrinting());
    layout.setContextPrinting(false);
    assertEquals(false, layout.getContextPrinting());
    layout.setContextPrinting(true);
    assertEquals(true, layout.getContextPrinting());
  }
}
