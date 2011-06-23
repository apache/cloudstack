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

package org.apache.log4j.spi;

import junit.framework.TestCase;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.apache.log4j.NDC;
import org.apache.log4j.util.SerializationTestHelper;
import org.apache.log4j.Priority;
import org.apache.log4j.Category;


/**
 * Tests LoggingEvent.
 *
 * @author Curt Arnold
 */
public class LoggingEventTest extends TestCase {
  /**
   * Create LoggingEventTest.
   *
   * @param name test name.
   */
  public LoggingEventTest(final String name) {
    super(name);
  }

  /**
   * Serialize a simple logging event and check it against
   * a witness.
   * @throws Exception if exception during test.
   */
  public void testSerializationSimple() throws Exception {
    Logger root = Logger.getRootLogger();
    LoggingEvent event =
      new LoggingEvent(
        root.getClass().getName(), root, Level.INFO, "Hello, world.", null);
//    event.prepareForDeferredProcessing();

    int[] skip = new int[] { 352, 353, 354, 355, 356 };
    SerializationTestHelper.assertSerializationEquals(
      "witness/serialization/simple.bin", event, skip, 237);
  }

  /**
   * Serialize a logging event with an exception and check it against
   * a witness.
   * @throws Exception if exception during test.
   *
   */
  public void testSerializationWithException() throws Exception {
    Logger root = Logger.getRootLogger();
    Exception ex = new Exception("Don't panic");
    LoggingEvent event =
      new LoggingEvent(
        root.getClass().getName(), root, Level.INFO, "Hello, world.", ex);
//    event.prepareForDeferredProcessing();

    int[] skip = new int[] { 352, 353, 354, 355, 356 };
    SerializationTestHelper.assertSerializationEquals(
      "witness/serialization/exception.bin", event, skip, 237);
  }

  /**
   * Serialize a logging event with an exception and check it against
   * a witness.
   * @throws Exception if exception during test.
   *
   */
  public void testSerializationWithLocation() throws Exception {
    Logger root = Logger.getRootLogger();
    LoggingEvent event =
      new LoggingEvent(
        root.getClass().getName(), root, Level.INFO, "Hello, world.", null);
    event.getLocationInformation();
//    event.prepareForDeferredProcessing();

    int[] skip = new int[] { 352, 353, 354, 355, 356 };
    SerializationTestHelper.assertSerializationEquals(
      "witness/serialization/location.bin", event, skip, 237);
  }

  /**
   * Serialize a logging event with ndc.
   * @throws Exception if exception during test.
   *
   */
  public void testSerializationNDC() throws Exception {
    Logger root = Logger.getRootLogger();
    NDC.push("ndc test");

    LoggingEvent event =
      new LoggingEvent(
        root.getClass().getName(), root, Level.INFO, "Hello, world.", null);
//    event.prepareForDeferredProcessing();

    int[] skip = new int[] { 352, 353, 354, 355, 356 };
    SerializationTestHelper.assertSerializationEquals(
      "witness/serialization/ndc.bin", event, skip, 237);
    }

  /**
   * Serialize a logging event with mdc.
   * @throws Exception if exception during test.
   *
   */
  public void testSerializationMDC() throws Exception {
    Logger root = Logger.getRootLogger();
    MDC.put("mdckey", "mdcvalue");

    LoggingEvent event =
      new LoggingEvent(
        root.getClass().getName(), root, Level.INFO, "Hello, world.", null);
//    event.prepareForDeferredProcessing();

    int[] skip = new int[] { 352, 353, 354, 355, 356 };
    SerializationTestHelper.assertSerializationEquals(
      "witness/serialization/mdc.bin", event, skip, 237);
  }

  /**
   * Deserialize a simple logging event.
   * @throws Exception if exception during test.
   *
   */
  public void testDeserializationSimple() throws Exception {
    Object obj =
      SerializationTestHelper.deserializeStream(
        "witness/serialization/simple.bin");
    assertTrue(obj instanceof LoggingEvent);

    LoggingEvent event = (LoggingEvent) obj;
    assertEquals("Hello, world.", event.getMessage());
    assertEquals(Level.INFO, event.getLevel());
  }

  /**
   * Deserialize a logging event with an exception.
   * @throws Exception if exception during test.
   *
   */
  public void testDeserializationWithException() throws Exception {
    Object obj =
      SerializationTestHelper.deserializeStream(
        "witness/serialization/exception.bin");
    assertTrue(obj instanceof LoggingEvent);

    LoggingEvent event = (LoggingEvent) obj;
    assertEquals("Hello, world.", event.getMessage());
    assertEquals(Level.INFO, event.getLevel());
  }

  /**
   * Deserialize a logging event with an exception.
   * @throws Exception if exception during test.
   *
   */
  public void testDeserializationWithLocation() throws Exception {
    Object obj =
      SerializationTestHelper.deserializeStream(
        "witness/serialization/location.bin");
    assertTrue(obj instanceof LoggingEvent);

    LoggingEvent event = (LoggingEvent) obj;
    assertEquals("Hello, world.", event.getMessage());
    assertEquals(Level.INFO, event.getLevel());
  }

    /**
     * Tests LoggingEvent.fqnOfCategoryClass.
     */
  public void testFQNOfCategoryClass() {
      Category root = Logger.getRootLogger();
      Priority info = Level.INFO;
      String catName = Logger.class.toString();
      LoggingEvent event =
        new LoggingEvent(
          catName, root, info, "Hello, world.", null);
      assertEquals(catName, event.fqnOfCategoryClass);
  }

    /**
     * Tests LoggingEvent.level.
     * @deprecated
     */
  public void testLevel() {
      Category root = Logger.getRootLogger();
      Priority info = Level.INFO;
      String catName = Logger.class.toString();
      LoggingEvent event =
        new LoggingEvent(
          catName, root, 0L,  info, "Hello, world.", null);
      Priority error = Level.ERROR;
      event.level = error;
      assertEquals(Level.ERROR, event.level);
  }

    /**
     * Tests LoggingEvent.getLocationInfo() when no FQCN is specified.
     * See bug 41186.
     */
  public void testLocationInfoNoFQCN() {
      Category root = Logger.getRootLogger();
	  Priority level = Level.INFO;
      LoggingEvent event =
        new LoggingEvent(
          null, root, 0L,  level, "Hello, world.", null);
      LocationInfo info = event.getLocationInformation();
	  //
	  //  log4j 1.2 returns an object, its layout doesn't check for nulls.
	  //  log4j 1.3 returns a null.
	  //
	  assertNotNull(info);
	  if (info != null) {
	     assertEquals("?", info.getLineNumber());
		 assertEquals("?", info.getClassName());
		 assertEquals("?", info.getFileName());
		 assertEquals("?", info.getMethodName());
	  }
  }

    /**
     * Message object that throws a RuntimeException on toString().
     * See bug 37182.
     */
    private static class BadMessage {
        public BadMessage() {
        }

        public String toString() {
            throw new RuntimeException();
        }
    }

    /**
     * Tests that an runtime exception or error during toString
     * on the message parameter does not propagate to caller.
     * See bug 37182.
     */
    public void testBadMessage() {
        Category root = Logger.getRootLogger();
        Priority info = Level.INFO;
        String catName = Logger.class.toString();
        BadMessage msg = new BadMessage();
        LoggingEvent event =
          new LoggingEvent(
            catName, root, 0L,  info, msg, null);
        //  would result in exception in earlier versions
        event.getRenderedMessage();
    }


}
