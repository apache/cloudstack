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

import java.lang.reflect.Method;


/**
 *
 * FileAppender tests.
 *
 * @author Curt Arnold
 */
public class FileAppenderTest extends TestCase {
  /**
   * Tests that any necessary directories are attempted to
   * be created if they don't exist.  See bug 9150.
   *
   */
  public void testDirectoryCreation() {
    //
    //   known to fail on JDK 1.1
    if (!System.getProperty("java.version").startsWith("1.1.")) {
      File newFile = new File("output/newdir/temp.log");
      newFile.delete();

      File newDir = new File("output/newdir");
      newDir.delete();

      org.apache.log4j.FileAppender wa = new org.apache.log4j.FileAppender();
      wa.setFile("output/newdir/temp.log");
      wa.setLayout(new PatternLayout("%m%n"));
      wa.activateOptions();

      assertTrue(new File("output/newdir/temp.log").exists());
    }
  }

  /**
   * Tests that the return type of getThreshold is Priority.
   * @throws Exception
   */
  public void testGetThresholdReturnType() throws Exception {
    Method method = FileAppender.class.getMethod("getThreshold", (Class[]) null);
    assertTrue(method.getReturnType() == Priority.class);
  }

  /**
   * Tests getThreshold and setThreshold.
   */
  public void testgetSetThreshold() {
    FileAppender appender = new FileAppender();
    Priority debug = Level.DEBUG;
    assertNull(appender.getThreshold());
    appender.setThreshold(debug);
    assertTrue(appender.getThreshold() == debug);
  }

  /**
   * Tests isAsSevereAsThreshold.
   */
  public void testIsAsSevereAsThreshold() {
    FileAppender appender = new FileAppender();
    Priority debug = Level.DEBUG;
    assertTrue(appender.isAsSevereAsThreshold(debug));
  }
}
