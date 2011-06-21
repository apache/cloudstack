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
package org.apache.log4j.filter;

import junit.framework.TestCase;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.util.Compare;
import org.apache.log4j.util.ControlFilter;
import org.apache.log4j.util.Filter;
import org.apache.log4j.util.JunitTestRunnerFilter;
import org.apache.log4j.util.LineNumberFilter;
import org.apache.log4j.util.SunReflectFilter;
import org.apache.log4j.util.Transformer;
import org.apache.log4j.extras.DOMConfigurator;
import org.apache.log4j.xml.Log4jEntityResolver;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.FileNotFoundException;
import java.io.InputStream;


/**
 * Various tests verifying that filters work properly and that 
 * JoranConfigurator can effectively parse config files containing them.
 * 
 * @author Ceki Gulcu
 *
 */
public class SimpleFilterTest extends TestCase {
  Logger root; 
  Logger logger;

  public final static String FILTERED = "filtered";
  public final static String TEMP = "temp";
  
  static String TEST1_PAT = "(DEBUG|INFO|WARN|ERROR|FATAL) - Message \\d";
  static String TEST8_PAT = "WARN org.apache.log4j.filter.SimpleFilterTest - Message \\d";
  static String EXCEPTION1 = "java.lang.Exception: Just testing";
  static String EXCEPTION2 = "\\s*at .*\\(.*:\\d{1,4}\\)";
  static String EXCEPTION3 = "\\s*at .*\\(Native Method\\)";
  
  public SimpleFilterTest(String name) {
    super(name);
  }

  public void setUp() {
    root = Logger.getRootLogger();
    logger = Logger.getLogger(SimpleFilterTest.class);
  }
 
  public void tearDown() {  
    root.getLoggerRepository().resetConfiguration();
  }

  private final void configure(final String resourceName) throws Exception {
    InputStream is = getClass().getResourceAsStream(resourceName);
    if (is == null) {
        throw new FileNotFoundException(
                "Could not find resource " + resourceName);
    }
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
	builder.setEntityResolver(new Log4jEntityResolver());
    Document doc = builder.parse(is);
    DOMConfigurator.configure(doc.getDocumentElement());
  }
  
  public void test1() throws Exception {
    configure("simpleFilter1.xml");

    common();
    
    ControlFilter cf = new ControlFilter(new String[]{TEST1_PAT, EXCEPTION1, EXCEPTION2, EXCEPTION3});
    

    Transformer.transform(TEMP, FILTERED, new Filter[] {cf,
        new LineNumberFilter(), 
        new SunReflectFilter(), 
        new JunitTestRunnerFilter()});

     assertTrue(Compare.compare(SimpleFilterTest.class,
             FILTERED,
             "witness/filter/simpleFilter.1"));
  }

    public void test6() throws Exception {
      configure("simpleFilter6.xml");
      common();

      ControlFilter cf = new ControlFilter(new String[]{TEST1_PAT, EXCEPTION1, EXCEPTION2, EXCEPTION3});


      Transformer.transform(TEMP, FILTERED, new Filter[] {cf,
          new LineNumberFilter(),
          new SunReflectFilter(),
          new JunitTestRunnerFilter()});

       assertTrue(Compare.compare(SimpleFilterTest.class, FILTERED, "witness/filter/simpleFilter.6"));
    }

    public void test7() throws Exception {
      configure("simpleFilter7.xml");
      common();

      ControlFilter cf = new ControlFilter(new String[]{TEST1_PAT, EXCEPTION1, EXCEPTION2, EXCEPTION3});


      Transformer.transform(TEMP, FILTERED, new Filter[] {cf,
          new LineNumberFilter(),
          new SunReflectFilter(),
          new JunitTestRunnerFilter()});

       assertTrue(Compare.compare(SimpleFilterTest.class, FILTERED, "witness/filter/simpleFilter.7"));
    }

    public void test8() throws Exception {
      configure("simpleFilter8.xml");
      common();

      ControlFilter cf = new ControlFilter(new String[]{TEST8_PAT, EXCEPTION1, EXCEPTION2, EXCEPTION3});


      Transformer.transform(TEMP, FILTERED, new Filter[] {cf,
          new LineNumberFilter(),
          new SunReflectFilter(),
          new JunitTestRunnerFilter()});

       assertTrue(Compare.compare(SimpleFilterTest.class, FILTERED, "witness/filter/simpleFilter.8"));
    }

    public void test9() throws Exception {
      configure("simpleFilter9.xml");
      common();

      ControlFilter cf = new ControlFilter(new String[]{TEST1_PAT, EXCEPTION1, EXCEPTION2, EXCEPTION3});


      Transformer.transform(TEMP, FILTERED, new Filter[] {cf,
          new LineNumberFilter(),
          new SunReflectFilter(),
          new JunitTestRunnerFilter()});

       assertTrue(Compare.compare(SimpleFilterTest.class, FILTERED, "witness/filter/simpleFilter.1"));
    }

    public void test10() throws Exception {
      configure("simpleFilter10.xml");
      common();

      ControlFilter cf = new ControlFilter(new String[]{TEST1_PAT, EXCEPTION1, EXCEPTION2, EXCEPTION3});


      Transformer.transform(TEMP, FILTERED, new Filter[] {cf,
          new LineNumberFilter(),
          new SunReflectFilter(),
          new JunitTestRunnerFilter()});

       assertTrue(Compare.compare(SimpleFilterTest.class, FILTERED, "witness/filter/simpleFilter.6"));
    }

    public void test11() throws Exception {
      configure("simpleFilter11.xml");
      common();

      ControlFilter cf = new ControlFilter(new String[]{TEST1_PAT, EXCEPTION1, EXCEPTION2, EXCEPTION3});


      Transformer.transform(TEMP, FILTERED, new Filter[] {cf,
          new LineNumberFilter(),
          new SunReflectFilter(),
          new JunitTestRunnerFilter()});

       assertTrue(Compare.compare(SimpleFilterTest.class, FILTERED, "witness/filter/simpleFilter.11"));
    }

    public void test12() throws Exception {
      configure("simpleFilter12.xml");
      common();

      ControlFilter cf = new ControlFilter(new String[]{TEST8_PAT, EXCEPTION1, EXCEPTION2, EXCEPTION3});


      Transformer.transform(TEMP, FILTERED, new Filter[] {cf,
          new LineNumberFilter(),
          new SunReflectFilter(),
          new JunitTestRunnerFilter()});

       assertTrue(Compare.compare(SimpleFilterTest.class, FILTERED, "witness/filter/simpleFilter.8"));
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
