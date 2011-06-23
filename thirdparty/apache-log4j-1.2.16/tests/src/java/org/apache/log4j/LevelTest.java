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

import org.apache.log4j.util.SerializationTestHelper;
import java.util.Locale;


/**
 * Tests of Level.
 *
 * @author Curt Arnold
 * @since 1.2.12
 */
public class LevelTest extends TestCase {
  /**
   * Constructs new instance of test.
   * @param name test name.
   */
  public LevelTest(final String name) {
    super(name);
  }

  /**
   * Serialize Level.INFO and check against witness.
   * @throws Exception if exception during test.
   *
   */
  public void testSerializeINFO() throws Exception {
    int[] skip = new int[] {  };
    SerializationTestHelper.assertSerializationEquals(
      "witness/serialization/info.bin", Level.INFO, skip, Integer.MAX_VALUE);
  }

  /**
   * Deserialize witness and see if resolved to Level.INFO.
   * @throws Exception if exception during test.
   */
  public void testDeserializeINFO() throws Exception {
    Object obj =
      SerializationTestHelper.deserializeStream(
        "witness/serialization/info.bin");
    assertTrue(obj instanceof Level);
    Level info = (Level) obj;
    assertEquals("INFO", info.toString());
    //
    //  JDK 1.1 doesn't support readResolve necessary for the assertion
    if (!System.getProperty("java.version").startsWith("1.1.")) {
       assertTrue(obj == Level.INFO);
    }
  }

  /**
   * Tests that a custom level can be serialized and deserialized
   * and is not resolved to a stock level.
   *
   * @throws Exception if exception during test.
   */
  public void testCustomLevelSerialization() throws Exception {
    CustomLevel custom = new CustomLevel();
    Object obj = SerializationTestHelper.serializeClone(custom);
    assertTrue(obj instanceof CustomLevel);

    CustomLevel clone = (CustomLevel) obj;
    assertEquals(Level.INFO.level, clone.level);
    assertEquals(Level.INFO.levelStr, clone.levelStr);
    assertEquals(Level.INFO.syslogEquivalent, clone.syslogEquivalent);
  }

  /**
   * Custom level to check that custom levels are
   * serializable, but not resolved to a plain Level.
   */
  private static class CustomLevel extends Level {
    private static final long serialVersionUID = 1L;
      /**
       * Create an instance of CustomLevel.
       */
    public CustomLevel() {
      super(
        Level.INFO.level, Level.INFO.levelStr, Level.INFO.syslogEquivalent);
    }
  }

    /**
     * Tests Level.TRACE_INT.
     */
  public void testTraceInt() {
      assertEquals(5000, Level.TRACE_INT);
  }

    /**
     * Tests Level.TRACE.
     */
  public void testTrace() {
      assertEquals("TRACE", Level.TRACE.toString());
      assertEquals(5000, Level.TRACE.toInt());
      assertEquals(7, Level.TRACE.getSyslogEquivalent());
  }

    /**
     * Tests Level.toLevel(Level.TRACE_INT).
     */
  public void testIntToTrace() {
      Level trace = Level.toLevel(5000);
      assertEquals("TRACE", trace.toString());
  }

    /**
     * Tests Level.toLevel("TRACE");
     */
  public void testStringToTrace() {
        Level trace = Level.toLevel("TRACE");
        assertEquals("TRACE", trace.toString());
  }

    /**
     * Tests that Level extends Priority.
     */
  public void testLevelExtendsPriority() {
      assertTrue(Priority.class.isAssignableFrom(Level.class));
  }

    /**
     * Tests Level.OFF.
     */
  public void testOFF() {
    assertTrue(Level.OFF instanceof Level);
  }

    /**
     * Tests Level.FATAL.
     */
    public void testFATAL() {
      assertTrue(Level.FATAL instanceof Level);
    }

    /**
     * Tests Level.ERROR.
     */
    public void testERROR() {
      assertTrue(Level.ERROR instanceof Level);
    }

    /**
     * Tests Level.WARN.
     */
    public void testWARN() {
      assertTrue(Level.WARN instanceof Level);
    }

    /**
     * Tests Level.INFO.
     */
    public void testINFO() {
      assertTrue(Level.INFO instanceof Level);
    }

    /**
     * Tests Level.DEBUG.
     */
    public void testDEBUG() {
      assertTrue(Level.DEBUG instanceof Level);
    }

    /**
     * Tests Level.TRACE.
     */
    public void testTRACE() {
      assertTrue(Level.TRACE instanceof Level);
    }

    /**
     * Tests Level.ALL.
     */
    public void testALL() {
      assertTrue(Level.ALL instanceof Level);
    }

    /**
     * Tests Level.serialVersionUID.
     */
    public void testSerialVersionUID() {
      assertEquals(3491141966387921974L, Level.serialVersionUID);
    }

    /**
     * Tests Level.toLevel(Level.All_INT).
     */
  public void testIntToAll() {
      Level level = Level.toLevel(Level.ALL_INT);
      assertEquals("ALL", level.toString());
  }

    /**
     * Tests Level.toLevel(Level.FATAL_INT).
     */
  public void testIntToFatal() {
      Level level = Level.toLevel(Level.FATAL_INT);
      assertEquals("FATAL", level.toString());
  }


    /**
     * Tests Level.toLevel(Level.OFF_INT).
     */
  public void testIntToOff() {
      Level level = Level.toLevel(Level.OFF_INT);
      assertEquals("OFF", level.toString());
  }

    /**
     * Tests Level.toLevel(17, Level.FATAL).
     */
  public void testToLevelUnrecognizedInt() {
      Level level = Level.toLevel(17, Level.FATAL);
      assertEquals("FATAL", level.toString());
  }

    /**
     * Tests Level.toLevel(null, Level.FATAL).
     */
  public void testToLevelNull() {
      Level level = Level.toLevel(null, Level.FATAL);
      assertEquals("FATAL", level.toString());
  }

    /**
     * Test that dotless lower I + "nfo" is recognized as INFO.
     */
  public void testDotlessLowerI() {
      Level level = Level.toLevel("\u0131nfo");
      assertEquals("INFO", level.toString());
  }

    /**
     * Test that dotted lower I + "nfo" is recognized as INFO
     * even in Turkish locale.
     */
  public void testDottedLowerI() {
      Locale defaultLocale = Locale.getDefault();
      Locale turkey = new Locale("tr", "TR");
      Locale.setDefault(turkey);
      Level level = Level.toLevel("info");
      Locale.setDefault(defaultLocale);
      assertEquals("INFO", level.toString());
  }


}
