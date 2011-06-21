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

package org.apache.log4j.net;

import junit.framework.TestCase;

import org.apache.log4j.AsyncAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.VectorErrorHandler;
import org.apache.log4j.HTMLLayout;

import java.util.StringTokenizer;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Date;
import java.util.Calendar;


/**
 *    Tests for SyslogAppender
 *
 *
 * */
public class SyslogAppenderTest extends TestCase {
  /**
   * Create new instance of SyslogAppenderTest.
   * @param testName test name
   */
  public SyslogAppenderTest(final String testName) {
    super(testName);
  }

  /**
    * Resets configuration after every test.
  */
  public void tearDown() {
    LogManager.resetConfiguration();
  }

  /**
   * Test default constructor.
   */
  public void testDefaultConstructor() {
    SyslogAppender appender = new SyslogAppender();
    assertEquals("user", appender.getFacility());
    assertEquals(false, appender.getFacilityPrinting());
    assertNull(appender.getLayout());
    assertNull(appender.getSyslogHost());
    assertTrue(appender.requiresLayout());
  }

  /**
   * Test two parameter constructor.
   */
  public void testTwoParamConstructor() {
    Layout layout = new PatternLayout();
    SyslogAppender appender = new SyslogAppender(layout, 24);
    assertEquals("daemon", appender.getFacility());
    assertEquals(false, appender.getFacilityPrinting());
    assertEquals(layout, appender.getLayout());
    assertNull(appender.getSyslogHost());
    assertTrue(appender.requiresLayout());
  }

  /**
   * Test two parameter constructor with unexpected facility.
   */
  public void testTwoParamConstructorBadFacility() {
    Layout layout = new PatternLayout();
    SyslogAppender appender = new SyslogAppender(layout, 25);
    assertEquals("user", appender.getFacility());
    assertEquals(false, appender.getFacilityPrinting());
    assertEquals(layout, appender.getLayout());
    assertNull(appender.getSyslogHost());
    assertTrue(appender.requiresLayout());
  }

  /**
   * Test three parameter constructor.
   */
  public void testThreeParamConstructor() {
    Layout layout = new PatternLayout();
    SyslogAppender appender =
      new SyslogAppender(layout, "syslog.example.org", 24);
    assertEquals("daemon", appender.getFacility());
    assertEquals(false, appender.getFacilityPrinting());
    assertEquals(layout, appender.getLayout());
    assertEquals("syslog.example.org", appender.getSyslogHost());
    assertTrue(appender.requiresLayout());
  }

  /**
   * Test getFacilityString for expected facility codes.
   */
  public void testGetFacilityString() {
    String expected =
      "kern user mail daemon auth syslog lpr news "
      + "uucp cron authpriv ftp local0 local1 local2 local3 "
      + "local4 local5 local6 local7 ";
    StringBuffer actual = new StringBuffer();

    for (int i = 0; i <= 11; i++) {
      actual.append(SyslogAppender.getFacilityString(i << 3));
      actual.append(' ');
    }

    for (int i = 16; i <= 23; i++) {
      actual.append(SyslogAppender.getFacilityString(i << 3));
      actual.append(' ');
    }

    assertEquals(expected, actual.toString());
  }

  /**
   * Test getFacilityString for some unexpected facility codes.
   */
  public void testGetFacilityStringUnexpected() {
    assertNull(SyslogAppender.getFacilityString(1));
    assertNull(SyslogAppender.getFacilityString(12 << 3));
  }

  /**
   * Test getFacility with a bogus facility name.
   */
  public void testGetFacilityBogus() {
    assertEquals(-1, SyslogAppender.getFacility("bogus"));
  }

  /**
   * Test getFacility with a null facility name.
   */
  public void testGetFacilityNull() {
    assertEquals(-1, SyslogAppender.getFacility(null));
  }

  /**
   * Test getFacility for expected system facility names.
   */
  public void testGetFacilitySystemNames() {
    String[] names =
      new String[] {
        "kErn", "usEr", "MaIL", "daemOn", "auTh", "syslOg", "lPr", "newS",
        "Uucp", "croN", "authprIv", "ftP"
      };

    for (int i = 0; i <= 11; i++) {
      assertEquals(i << 3, SyslogAppender.getFacility(names[i]));
    }
  }

  /**
   * Test getFacility for expected system facility names.
   */
  public void testGetFacilityLocalNames() {
    String[] names =
      new String[] {
        "lOcal0", "LOCAL1", "loCal2", "locAl3", "locaL4", "local5", "LOCal6",
        "loCAL7"
      };

    for (int i = 0; i <= 7; i++) {
      assertEquals((16 + i) << 3, SyslogAppender.getFacility(names[i]));
    }
  }

  /**
   * Test setFacilityPrinting.
   */
  public void testSetFacilityPrinting() {
    SyslogAppender appender = new SyslogAppender();
    assertFalse(appender.getFacilityPrinting());
    appender.setFacilityPrinting(true);
    assertTrue(appender.getFacilityPrinting());
    appender.setFacilityPrinting(false);
    assertFalse(appender.getFacilityPrinting());
  }

  /**
   * Test of SyslogAppender constants.
   */
  public void testConstants() {
    assertEquals(0 << 3, SyslogAppender.LOG_KERN);
    assertEquals(1 << 3, SyslogAppender.LOG_USER);
    assertEquals(2 << 3, SyslogAppender.LOG_MAIL);
    assertEquals(3 << 3, SyslogAppender.LOG_DAEMON);
    assertEquals(4 << 3, SyslogAppender.LOG_AUTH);
    assertEquals(5 << 3, SyslogAppender.LOG_SYSLOG);
    assertEquals(6 << 3, SyslogAppender.LOG_LPR);
    assertEquals(7 << 3, SyslogAppender.LOG_NEWS);
    assertEquals(8 << 3, SyslogAppender.LOG_UUCP);
    assertEquals(9 << 3, SyslogAppender.LOG_CRON);
    assertEquals(10 << 3, SyslogAppender.LOG_AUTHPRIV);
    assertEquals(11 << 3, SyslogAppender.LOG_FTP);
    assertEquals(16 << 3, SyslogAppender.LOG_LOCAL0);
    assertEquals(17 << 3, SyslogAppender.LOG_LOCAL1);
    assertEquals(18 << 3, SyslogAppender.LOG_LOCAL2);
    assertEquals(19 << 3, SyslogAppender.LOG_LOCAL3);
    assertEquals(20 << 3, SyslogAppender.LOG_LOCAL4);
    assertEquals(21 << 3, SyslogAppender.LOG_LOCAL5);
    assertEquals(22 << 3, SyslogAppender.LOG_LOCAL6);
    assertEquals(23 << 3, SyslogAppender.LOG_LOCAL7);
  }

  /**
   * Test setFacility with null.
   * Should have no effect.
   */
  public void testSetFacilityKern() {
    SyslogAppender appender = new SyslogAppender();
    appender.setFacility("kern");
    appender.setFacility(null);
    assertEquals("kern", appender.getFacility());
  }

  /**
   * Test setFacility with null.
   * Should have no effect.
   */
  public void testSetFacilityNull() {
    SyslogAppender appender = new SyslogAppender();
    appender.setFacility("kern");
    appender.setFacility(null);
    assertEquals("kern", appender.getFacility());
  }

  /**
   * Test setFacility with bogus value.
   * Should reset to user.
   */
  public void testSetFacilityBogus() {
    SyslogAppender appender = new SyslogAppender();
    appender.setFacility("kern");
    appender.setFacility("bogus");
    assertEquals("user", appender.getFacility());
  }

  /**
   * Tests calling setFacility after appender has been activated.
   */
  public void testSetFacilityAfterActivation() {
    SyslogAppender appender = new SyslogAppender();
    appender.setName("foo");
    appender.setThreshold(Level.INFO);
    appender.setSyslogHost("localhost");
    appender.setFacility("user");
    appender.setLayout(new PatternLayout("%m%n"));

    VectorErrorHandler errorHandler = new VectorErrorHandler();
    appender.setErrorHandler(errorHandler);
    appender.activateOptions();
    appender.setFacility("kern");
    assertEquals("kern", appender.getFacility());
  }

  /**
   * Tests that append method drops messages below threshold.
   * Can't reach isSevereAsThreshold call in SyslogAppender.append
   * since it is checked in AppenderSkeleton.doAppend.
   */
  public void testAppendBelowThreshold() {
    SyslogAppender appender = new SyslogAppender();
    appender.setThreshold(Level.ERROR);
    appender.activateOptions();

    Logger logger = Logger.getRootLogger();
    logger.addAppender(appender);
    logger.info(
      "Should not be logged by SyslogAppenderTest.testAppendBelowThreshold.");
  }

  /**
   * Tests that append method drops messages below threshold.
   */
  public void testAppendNoHost() {
    SyslogAppender appender = new SyslogAppender();
    appender.setName("foo");
    appender.setThreshold(Level.INFO);

    VectorErrorHandler errorHandler = new VectorErrorHandler();
    appender.setErrorHandler(errorHandler);
    appender.setLayout(new PatternLayout("%m%n"));
    appender.activateOptions();

    Logger logger = Logger.getRootLogger();
    logger.addAppender(appender);
    logger.info(
      "Should not be logged by SyslogAppenderTest.testAppendNoHost.");
    assertEquals(1, errorHandler.size());

    //
    //  Appender is misspelled in implementation
    //
    assertEquals(
      "No syslog host is set for SyslogAppedender named \"foo\".",
      errorHandler.getMessage(0));
  }

  /**
   * Tests append method under normal conditions.
   */
  public void testAppend() {
    SyslogAppender appender = new SyslogAppender();
    appender.setName("foo");
    appender.setThreshold(Level.INFO);
    appender.setSyslogHost("localhost");
    appender.setFacility("user");
    appender.setLayout(new PatternLayout("%m%n"));

    VectorErrorHandler errorHandler = new VectorErrorHandler();
    appender.setErrorHandler(errorHandler);
    appender.activateOptions();

    //
    //  wrap SyslogAppender with an Async since appender may
    //    hang if syslogd is not accepting network messages
    //
    AsyncAppender asyncAppender = new AsyncAppender();
    asyncAppender.addAppender(appender);
    asyncAppender.activateOptions();

    Logger logger = Logger.getRootLogger();
    logger.addAppender(asyncAppender);

    Exception e =
      new Exception("Expected exception from SyslogAppenderTest.testAppend");
    logger.info(
      "Expected message from log4j unit test SyslogAppenderTest.testAppend.", e);
    assertEquals(0, errorHandler.size());
  }

  /**
    *  Tests SyslogAppender with IPv6 address.
    */
  public void testIPv6() {
      SyslogAppender appender = new SyslogAppender();
      appender.setSyslogHost("::1");
  }

  /**
    *  Tests SyslogAppender with IPv6 address enclosed in square brackets.
    */
  public void testIPv6InBrackets() {
      SyslogAppender appender = new SyslogAppender();
      appender.setSyslogHost("[::1]");
  }

  /**
    *  Tests SyslogAppender with IPv6 address enclosed in square brackets
    *     followed by port specification.
    */
  public void testIPv6AndPort() {
      SyslogAppender appender = new SyslogAppender();
      appender.setSyslogHost("[::1]:1514");
  }

  /**
    *  Tests SyslogAppender with host name enclosed in square brackets
    *     followed by port specification.
    */
  public void testHostNameAndPort() {
      SyslogAppender appender = new SyslogAppender();
      appender.setSyslogHost("localhost:1514");
  }


  /**
    *  Tests SyslogAppender with IPv4 address followed by port specification.
    */
  public void testIPv4AndPort() {
      SyslogAppender appender = new SyslogAppender();
      appender.setSyslogHost("127.0.0.1:1514");
  }

    private static String[] log(final boolean header,
                                final String msg,
                                final Exception ex,
                                final int packets) throws Exception {
        DatagramSocket ds = new DatagramSocket();
        ds.setSoTimeout(2000);

      SyslogAppender appender = new SyslogAppender();
      appender.setSyslogHost("localhost:" + ds.getLocalPort());
      appender.setName("name");
      appender.setHeader(header);
      PatternLayout pl = new PatternLayout("%m");
      appender.setLayout(pl);
      appender.activateOptions();

      Logger l = Logger.getRootLogger();
      l.addAppender(appender);
      if (ex == null) {
        l.info(msg);
      } else {
        l.error(msg, ex);
      }
      appender.close();
      String[] retval = new String[packets];
      byte[] buf = new byte[1000];
      for(int i = 0; i < packets; i++) {
          DatagramPacket p = new DatagramPacket(buf, 0, buf.length);
          ds.receive(p);
          retval[i] = new String(p.getData(), 0, p.getLength());
      }
      ds.close();
      return retval;
    }

    public void testActualLogging() throws Exception {
      String s = log(false, "greetings", null, 1)[0];
      StringTokenizer st = new StringTokenizer(s, "<>() ");
      assertEquals("14", st.nextToken());
      assertEquals("greetings", st.nextToken());
    }

    /**
     * Exception with printStackTrace that breaks earlier SyslogAppender.
     */
    private static class MishandledException extends Exception {
        private static final long serialVersionUID = 1L;
        /*
         *   Create new instance.
         */
        public MishandledException() {
        }

        /**
         * Print stack trace.
         * @param w print writer, may not be null.
         */
        public void printStackTrace(final java.io.PrintWriter w) {
             w.println("Mishandled stack trace follows:");
             w.println("");
             w.println("No tab here");
             w.println("\ttab here");
             w.println("\t");
        }
    }

    /**
     * Tests fix for bug 40502.
     * @throws Exception on IOException.
     */
    public void testBadTabbing() throws Exception {
        String[] s = log(false, "greetings", new MishandledException(), 6);
        StringTokenizer st = new StringTokenizer(s[0], "<>() ");
        assertEquals("11", st.nextToken());
        assertEquals("greetings", st.nextToken());
        assertEquals("<11>Mishandled stack trace follows:", s[1]);
        assertEquals("<11>", s[2]);
        assertEquals("<11>No tab here", s[3]);
        assertEquals("<11>" + SyslogAppender.TAB + "tab here", s[4]);
        assertEquals("<11>" + SyslogAppender.TAB, s[5]);
    }

    /**
     * Tests presence of timestamp if header = true.
     *
     * @throws Exception if IOException.
     */
    public void testHeaderLogging() throws Exception {
      Date preDate = new Date();
      String s = log(true, "greetings", null, 1)[0];
      Date postDate = new Date();
      assertEquals("<14>", s.substring(0, 4));

      String syslogDateStr = s.substring(4, 20);
      SimpleDateFormat fmt = new SimpleDateFormat("MMM dd HH:mm:ss ", Locale.ENGLISH);
      Date syslogDate = fmt.parse(syslogDateStr);
      Calendar cal = Calendar.getInstance(Locale.ENGLISH);
      cal.setTime(syslogDate);
      int syslogMonth = cal.get(Calendar.MONTH);
      int syslogDay = cal.get(Calendar.DATE);
      if (syslogDay < 10) {
          assertEquals(' ', syslogDateStr.charAt(4));
      }
      cal.setTime(preDate);
      int preMonth = cal.get(Calendar.MONTH);
      cal.set(Calendar.MILLISECOND, 0);
      preDate = cal.getTime();
      int syslogYear;
      if (preMonth == syslogMonth) {
          syslogYear = cal.get(Calendar.YEAR);
      } else {
          cal.setTime(postDate);
          syslogYear = cal.get(Calendar.YEAR);
      }
      cal.setTime(syslogDate);
      cal.set(Calendar.YEAR, syslogYear);
      syslogDate = cal.getTime();
      assertTrue(syslogDate.compareTo(preDate) >= 0);
      assertTrue(syslogDate.compareTo(postDate) <= 0);
    }


    /**
     * Tests that any header or footer in layout is sent.
     * @throws Exception if exception during test.
     */
    public void testLayoutHeader() throws Exception {
        DatagramSocket ds = new DatagramSocket();
        ds.setSoTimeout(2000);

      SyslogAppender appender = new SyslogAppender();
      appender.setSyslogHost("localhost:" + ds.getLocalPort());
      appender.setName("name");
      appender.setHeader(false);
      HTMLLayout pl = new HTMLLayout();
      appender.setLayout(pl);
      appender.activateOptions();

      Logger l = Logger.getRootLogger();
      l.addAppender(appender);
      l.info("Hello, World");
      appender.close();
      String[] s = new String[3];
      byte[] buf = new byte[1000];
      for(int i = 0; i < 3; i++) {
          DatagramPacket p = new DatagramPacket(buf, 0, buf.length);
          ds.receive(p);
          s[i] = new String(p.getData(), 0, p.getLength());
      }
      ds.close();
      assertEquals("<14><!DOCTYPE", s[0].substring(0,13));
      assertEquals("<14></table>", s[2].substring(0,12));
    }

    /**
     * Tests that syslog packets do not exceed 1024 bytes.
     * See bug 42087.
     * @throws Exception if exception during test.
     */
    public void testBigPackets() throws Exception {
        DatagramSocket ds = new DatagramSocket();
        ds.setSoTimeout(2000);

      SyslogAppender appender = new SyslogAppender();
      appender.setSyslogHost("localhost:" + ds.getLocalPort());
      appender.setName("name");
      appender.setHeader(false);
      PatternLayout pl = new PatternLayout("%m");
      appender.setLayout(pl);
      appender.activateOptions();

      Logger l = Logger.getRootLogger();
      l.addAppender(appender);
      StringBuffer msgbuf = new StringBuffer();
      while(msgbuf.length() < 8000) {
          msgbuf.append("0123456789");
      }
      String msg = msgbuf.toString();
      l.info(msg);
      appender.close();
      String[] s = new String[8];
      byte[] buf = new byte[1200];
      for(int i = 0; i < 8; i++) {
          DatagramPacket p = new DatagramPacket(buf, 0, buf.length);
          ds.receive(p);
          assertTrue(p.getLength() <= 1024);
          s[i] = new String(p.getData(), 0, p.getLength());
      }
      ds.close();
      StringBuffer rcvbuf = new StringBuffer(s[0]);
      rcvbuf.delete(0, 4);
      for(int i = 1; i < 8; i++) {
          rcvbuf.setLength(rcvbuf.length() - 3);
          rcvbuf.append(s[i].substring(s[i].indexOf("...") + 3));
      }
      assertEquals(msg.length(), rcvbuf.length());
      assertEquals(msg, rcvbuf.toString());
    }

}
