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

package org.apache.log4j.pattern;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import junit.framework.TestCase;

import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;


/**
   Test case for PatternParser.java. Tests the various
   conversion patterns supported by PatternParser. This test
   class tests PatternParser via the EnhancedPatternLayout class which
   uses it.
 */
public class PatternParserTest extends TestCase {
  private final Logger logger = Logger.getLogger("org.foobar");
  private final LoggingEvent event;

  public PatternParserTest(String name) {
    super(name);
    event = new LoggingEvent("org.apache.log4j.Logger",
            logger, Level.INFO, "msg 1", null);
  }

  private static String convert(
                 String pattern,
                 Map registry,
                 LoggingEvent event) {
    List converters = new ArrayList();
    List fields = new ArrayList();
    PatternParser.parse(pattern, converters, fields,
            registry,
            PatternParser.getPatternLayoutRules());
    assertEquals(converters.size(), fields.size());

    StringBuffer buf = new StringBuffer();
    Iterator converterIter = converters.iterator();
    Iterator fieldIter = fields.iterator();
    while(converterIter.hasNext()) {
        int fieldStart = buf.length();
        ((PatternConverter) converterIter.next()).format(event, buf);
        ((FormattingInfo) fieldIter.next()).format(fieldStart, buf);
    }
    return buf.toString();
  }

  public void testNewWord() throws Exception {
    HashMap ruleRegistry = new HashMap(5);
    ruleRegistry.put("z343", Num343PatternConverter.class.getName());
    String result = convert("%z343", ruleRegistry, event);
    assertEquals("343", result);
  }

  /* Test whether words starting with the letter 'n' are treated differently,
   * which was previously the case by mistake.
   */
  public void testNewWord2() throws Exception {
    HashMap ruleRegistry = new HashMap(5);
    ruleRegistry.put("n343", Num343PatternConverter.class.getName());
    String result = convert("%n343", ruleRegistry, event);
    assertEquals("343", result);
  }

  public void testBogusWord1() throws Exception {
    String result = convert("%, foobar", null, event);
    assertEquals("%, foobar", result);
  }

  public void testBogusWord2() throws Exception {
    String result = convert("xyz %, foobar", null, event);
    assertEquals("xyz %, foobar", result);
  }

  public void testBasic1() throws Exception {
    String result = convert("hello %-5level - %m%n", null, event);
    assertEquals("hello INFO  - msg 1" + Layout.LINE_SEP, result);
  }

  public void testBasic2() throws Exception {
    String result = convert("%relative %-5level [%thread] %logger - %m%n", null, event);

    long expectedRelativeTime = event.timeStamp - LoggingEvent.getStartTime();
    assertEquals(expectedRelativeTime + " INFO  [main] "+logger.getName()+" - msg 1" + Layout.LINE_SEP, result);
  }

  public void testMultiOption() throws Exception {
    String result = convert("%d{HH:mm:ss}{GMT} %d{HH:mm:ss} %c  - %m", null, event);

    SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
    String localTime = dateFormat.format(new Date(event.timeStamp));
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    String utcTime = dateFormat.format(new Date(event.timeStamp));
    StringBuffer buf = new StringBuffer(utcTime);
    buf.append(' ');
    buf.append(localTime);
    buf.append(" org.foobar  - msg 1");
    assertEquals(buf.toString(), result);
  }

  public void testBogus() throws Exception {
      String result = convert("%bogus", null, event);
      assertEquals("%bogus", result);
    }

  public void testMore() throws Exception {
        String result = convert("%more", null, event);
        assertEquals("msg 1ore", result);
  }

    /**
     * Options with missing close braces will be treated as a literal.
     * Endless looped with earlier code.
     *
     */
    public void testMalformedOption() {
        String result = convert("foo%m{yyyy.MM.dd", null, event);
        assertEquals("foomsg 1{yyyy.MM.dd", result);
    }


  private void assertFactories(Map rules) throws Exception {
      assertTrue(rules.size() > 0);
      Iterator iter = rules.values().iterator();
      Class[] factorySig = new Class[] { Class.forName("[Ljava.lang.String;") };
      Object[] factoryArg = new Object[] { null };
      while(iter.hasNext()) {
          Class ruleClass = (Class) iter.next();
          Method factory =  ruleClass.getMethod("newInstance", factorySig);
          Object converter = factory.invoke(null, factoryArg);
          assertTrue(converter != null);
      }
  }

  public void testPatternLayoutFactories() throws Exception {
      assertFactories(PatternParser.getPatternLayoutRules());
  }

  public void testFileNamePatternFactories() throws Exception {
        assertFactories(PatternParser.getFileNamePatternRules());
  }

}
