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

import java.util.Locale;


/**
 * Tests of Priority.
 *
 * @author Curt Arnold
 * @since 1.2.14
 */
public class PriorityTest extends TestCase {
  /**
   * Constructs new instance of test.
   * @param name test name.
   */
  public PriorityTest(final String name) {
    super(name);
  }

  /**
   * Tests Priority.OFF_INT.
   */
  public void testOffInt() {
    assertEquals(Integer.MAX_VALUE, Priority.OFF_INT);
  }

  /**
   * Tests Priority.FATAL_INT.
   */
  public void testFatalInt() {
    assertEquals(50000, Priority.FATAL_INT);
  }

  /**
   * Tests Priority.ERROR_INT.
   */
  public void testErrorInt() {
    assertEquals(40000, Priority.ERROR_INT);
  }

  /**
   * Tests Priority.WARN_INT.
   */
  public void testWarnInt() {
    assertEquals(30000, Priority.WARN_INT);
  }

  /**
   * Tests Priority.INFO_INT.
   */
  public void testInfoInt() {
    assertEquals(20000, Priority.INFO_INT);
  }

  /**
   * Tests Priority.DEBUG_INT.
   */
  public void testDebugInt() {
    assertEquals(10000, Priority.DEBUG_INT);
  }

  /**
   * Tests Priority.ALL_INT.
   */
  public void testAllInt() {
    assertEquals(Integer.MIN_VALUE, Priority.ALL_INT);
  }

  /**
   * Tests Priority.FATAL.
   * @deprecated
   */
  public void testFatal() {
    assertTrue(Priority.FATAL instanceof Level);
  }

  /**
   * Tests Priority.ERROR.
   * @deprecated
   */
  public void testERROR() {
    assertTrue(Priority.ERROR instanceof Level);
  }

  /**
   * Tests Priority.WARN.
   * @deprecated
   */
  public void testWARN() {
    assertTrue(Priority.WARN instanceof Level);
  }

  /**
   * Tests Priority.INFO.
   * @deprecated
   */
  public void testINFO() {
    assertTrue(Priority.INFO instanceof Level);
  }

  /**
   * Tests Priority.DEBUG.
   * @deprecated
   */
  public void testDEBUG() {
    assertTrue(Priority.DEBUG instanceof Level);
  }

  /**
   * Tests Priority.equals(null).
   * @deprecated
   */
  public void testEqualsNull() {
    assertFalse(Priority.DEBUG.equals(null));
  }

  /**
   * Tests Priority.equals(Level.DEBUG).
   * @deprecated
   */
  public void testEqualsLevel() {
    //
    //   this behavior violates the equals contract.
    //
    assertTrue(Priority.DEBUG.equals(Level.DEBUG));
  }

  /**
   * Tests getAllPossiblePriorities().
   * @deprecated
   */
  public void testGetAllPossiblePriorities() {
    Priority[] priorities = Priority.getAllPossiblePriorities();
    assertEquals(5, priorities.length);
  }

  /**
   * Tests toPriority(String).
   * @deprecated
   */
  public void testToPriorityString() {
    assertTrue(Priority.toPriority("DEBUG") == Level.DEBUG);
  }

  /**
   * Tests toPriority(int).
   * @deprecated
   */
  public void testToPriorityInt() {
    assertTrue(Priority.toPriority(Priority.DEBUG_INT) == Level.DEBUG);
  }

  /**
   * Tests toPriority(String, Priority).
   * @deprecated
   */
  public void testToPriorityStringPriority() {
    assertTrue(Priority.toPriority("foo", Priority.DEBUG) == Priority.DEBUG);
  }

  /**
   * Tests toPriority(int, Priority).
   * @deprecated
   */
  public void testToPriorityIntPriority() {
    assertTrue(Priority.toPriority(17, Priority.DEBUG) == Priority.DEBUG);
  }

    /**
     * Test that dotless lower I + "nfo" is recognized as INFO.
     * @deprecated
     */
  public void testDotlessLowerI() {
      Priority level = Priority.toPriority("\u0131nfo");
      assertEquals("INFO", level.toString());
  }

    /**
     * Test that dotted lower I + "nfo" is recognized as INFO
     * even in Turkish locale.
     * @deprecated
     */
  public void testDottedLowerI() {
      Locale defaultLocale = Locale.getDefault();
      Locale turkey = new Locale("tr", "TR");
      Locale.setDefault(turkey);
      Priority level = Priority.toPriority("info");
      Locale.setDefault(defaultLocale);
      assertEquals("INFO", level.toString());
  }

}
