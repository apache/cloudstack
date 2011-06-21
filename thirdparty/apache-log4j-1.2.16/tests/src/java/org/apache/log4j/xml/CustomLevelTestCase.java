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

package org.apache.log4j.xml;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.log4j.Logger;
import org.apache.log4j.util.Compare;

public class CustomLevelTestCase extends TestCase {

  static String TEMP = "output/temp";

  Logger root; 
  Logger logger;

  public CustomLevelTestCase(String name) {
    super(name);
  }

  public void setUp() {
    root = Logger.getRootLogger();
    logger = Logger.getLogger(CustomLevelTestCase.class);
  }

  public void tearDown() {  
    root.getLoggerRepository().resetConfiguration();
  }

  public void test1() throws Exception {
    DOMConfigurator.configure("input/xml/customLevel1.xml");
    common();
    assertTrue(Compare.compare(TEMP, "witness/customLevel.1"));
  }

  public void test2() throws Exception {
    DOMConfigurator.configure("input/xml/customLevel2.xml");
    common();
    assertTrue(Compare.compare(TEMP, "witness/customLevel.2"));
  }

  public void test3() throws Exception {
    DOMConfigurator.configure("input/xml/customLevel3.xml");
    common();
    assertTrue(Compare.compare(TEMP, "witness/customLevel.3"));
  }

  public void test4() throws Exception {
    DOMConfigurator.configure("input/xml/customLevel4.xml");
    common();
    assertTrue(Compare.compare(TEMP, "witness/customLevel.4"));
  }


  void common() {
    int i = 0;
    logger.debug("Message " + ++i);
    logger.info ("Message " + ++i);
    logger.warn ("Message " + ++i);
    logger.error("Message " + ++i);
    logger.log(XLevel.TRACE, "Message " + ++i);
  }

  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(new CustomLevelTestCase("test1"));
    suite.addTest(new CustomLevelTestCase("test2"));
    suite.addTest(new CustomLevelTestCase("test3"));
    suite.addTest(new CustomLevelTestCase("test4"));
    return suite;
  }

}
