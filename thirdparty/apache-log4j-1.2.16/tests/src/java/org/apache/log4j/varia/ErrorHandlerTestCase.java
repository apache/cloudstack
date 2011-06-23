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

package org.apache.log4j.varia;

import junit.framework.TestCase;
import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.util.Filter;
import org.apache.log4j.util.Transformer;
import org.apache.log4j.util.Compare;
import org.apache.log4j.util.JunitTestRunnerFilter;
import org.apache.log4j.util.SunReflectFilter;
import org.apache.log4j.util.LineNumberFilter;
import org.apache.log4j.util.ControlFilter;
import org.apache.log4j.xml.DOMConfigurator;
import org.apache.log4j.PropertyConfigurator;

public class ErrorHandlerTestCase extends TestCase {

  static String TEMP = "output/temp";
  static String FILTERED = "output/filtered";


  static String EXCEPTION1 = "java.lang.Exception: Just testing";
  static String EXCEPTION2 = "\\s*at .*\\(.*\\)";
  static String EXCEPTION3 = "\\s*at .*\\(Native Method\\)";

  static String TEST1_PAT =
                       "FALLBACK - (root|test) - Message \\d";


  Logger root; 
  Logger logger;

  public ErrorHandlerTestCase(String name) {
    super(name);
  }

  public void setUp() {
    root = Logger.getRootLogger();
    logger = Logger.getLogger("test");
  }

  public void tearDown() {  
    root.getLoggerRepository().resetConfiguration();
  }

  public void test1() throws Exception {
    DOMConfigurator.configure("input/xml/fallback1.xml");
    Appender primary = root.getAppender("PRIMARY");
    ErrorHandler eh = primary.getErrorHandler();
    assertNotNull(eh);

    common();

    ControlFilter cf = new ControlFilter(new String[]{TEST1_PAT,
					       EXCEPTION1, EXCEPTION2, EXCEPTION3});

    Transformer.transform(TEMP, FILTERED, new Filter[] {cf,
                            new LineNumberFilter(),
                            new JunitTestRunnerFilter(),
                            new SunReflectFilter()});


    assertTrue(Compare.compare(FILTERED, "witness/fallback1"));
  }
  
  public void test2() throws Exception {
    PropertyConfigurator.configure("input/fallback1.properties");
    Appender primary = root.getAppender("PRIMARY");
    ErrorHandler eh = primary.getErrorHandler();
    assertNotNull(eh);

    common();

    ControlFilter cf = new ControlFilter(new String[]{TEST1_PAT,
					       EXCEPTION1, EXCEPTION2, EXCEPTION3});

    Transformer.transform(TEMP, FILTERED, new Filter[] {cf,
                            new LineNumberFilter(),
                            new JunitTestRunnerFilter(),
                            new SunReflectFilter()});


    assertTrue(Compare.compare(FILTERED, "witness/fallback1"));
  }

  void common() {
    int i = -1;
 
    logger.debug("Message " + ++i);
    root.debug("Message " + i);        

    logger.info ("Message " + ++i);
    root.info("Message " + i);        

    logger.warn ("Message " + ++i);
    root.warn("Message " + i);        

    logger.error("Message " + ++i);
    root.error("Message " + i);
    
    logger.log(Level.FATAL, "Message " + ++i);
    root.log(Level.FATAL, "Message " + i);    
    
    Exception e = new Exception("Just testing");
    logger.debug("Message " + ++i, e);
    root.debug("Message " + i, e);
    
    logger.error("Message " + ++i, e);
    root.error("Message " + i, e);    

  }

}
