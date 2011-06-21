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

package org.apache.log4j.customLogger;

import org.apache.log4j.xml.DOMConfigurator;
import org.apache.log4j.util.*;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.Test;

/**
   Tests handling of custom loggers.
   
   @author Ceki G&uuml;lc&uuml;
*/
public class XLoggerTestCase extends TestCase {

  static String FILTERED = "output/filtered";
  static XLogger logger = (XLogger) XLogger.getLogger(XLoggerTestCase.class);

  public XLoggerTestCase(String name){
    super(name);
  }

  public void tearDown() {
    logger.getLoggerRepository().resetConfiguration();
  }

  public void test1()  throws Exception  { common(1); }
  public void test2()  throws Exception  { common(2); }

  void common(int number) throws Exception {
    DOMConfigurator.configure("input/xml/customLogger"+number+".xml");

    int i = -1;

    logger.trace("Message " + ++i);
    logger.debug("Message " + ++i);
    logger.warn ("Message " + ++i);
    logger.error("Message " + ++i);
    logger.fatal("Message " + ++i);
    Exception e = new Exception("Just testing");
    logger.debug("Message " + ++i, e);

    Transformer.transform(
      "output/temp", FILTERED,
      new Filter[] {
        new LineNumberFilter(), new SunReflectFilter(),
        new JunitTestRunnerFilter()
      });
    assertTrue(Compare.compare(FILTERED, "witness/customLogger."+number));

  }

  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(new XLoggerTestCase("test1"));
    suite.addTest(new XLoggerTestCase("test2"));
    return suite;
  }
}
