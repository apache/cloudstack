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

import org.apache.log4j.Layout;
import org.apache.log4j.LayoutTest;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.NDC;
import org.apache.log4j.MDC;
import org.apache.log4j.spi.LoggingEvent;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.InputSource;

import java.io.Reader;
import java.io.StringReader;
import java.util.Hashtable;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;


/**
 * Test for XMLLayout.
 *
 * @author Curt Arnold
 */
public class XMLLayoutTest extends LayoutTest {
  /**
   * Construct new instance of XMLLayoutTest.
   *
   * @param testName test name.
   */
  public XMLLayoutTest(final String testName) {
    super(testName, "text/plain", false, null, null);
  }

    /**
     * Clear MDC and NDC before test.
     */
  public void setUp() {
      NDC.clear();
      if (MDC.getContext() != null) {
        MDC.getContext().clear();
      }
  }

    /**
     * Clear MDC and NDC after test.
     */
  public void tearDown() {
      setUp();
  }

  /**
   * @{inheritDoc}
   */
  protected Layout createLayout() {
    return new XMLLayout();
  }

  /**
   * Parses the string as the body of an XML document and returns the document element.
   * @param source source string.
   * @return document element.
   * @throws Exception if parser can not be constructed or source is not a valid XML document.
   */
  private Element parse(final String source) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(false);
    factory.setCoalescing(true);

    DocumentBuilder builder = factory.newDocumentBuilder();
    Reader reader = new StringReader(source);
    Document doc = builder.parse(new InputSource(reader));

    return doc.getDocumentElement();
  }

  /**
   * Checks a log4j:event element against expectations.
   * @param element element, may not be null.
   * @param event event, may not be null.
   */
  private void checkEventElement(
    final Element element, final LoggingEvent event) {
    assertEquals("log4j:event", element.getTagName());
    assertEquals(
      event.getLoggerName(), element.getAttribute("logger"));
    assertEquals(
      Long.toString(event.timeStamp), element.getAttribute("timestamp"));
    assertEquals(event.getLevel().toString(), element.getAttribute("level"));
    assertEquals(event.getThreadName(), element.getAttribute("thread"));
  }

  /**
   * Checks a log4j:message element against expectations.
   * @param element element, may not be null.
   * @param message expected message.
   */
  private void checkMessageElement(
    final Element element, final String message) {
    assertEquals("log4j:message", element.getTagName());

    Node messageNode = element.getFirstChild();
    assertNotNull(messageNode);
    assertEquals(Node.TEXT_NODE, messageNode.getNodeType());
    assertEquals(message, messageNode.getNodeValue());
    assertNull(messageNode.getNextSibling());
  }

  /**
   * Checks a log4j:message element against expectations.
   * @param element element, may not be null.
   * @param message expected message.
   */
  private void checkNDCElement(final Element element, final String message) {
    assertEquals("log4j:NDC", element.getTagName());

    Node messageNode = element.getFirstChild();
    assertNotNull(messageNode);
    assertEquals(Node.TEXT_NODE, messageNode.getNodeType());
    assertEquals(message, messageNode.getNodeValue());
    assertNull(messageNode.getNextSibling());
  }

  /**
   * Checks a log4j:throwable element against expectations.
   * @param element element, may not be null.
   * @param ex exception, may not be null.
   */
  private void checkThrowableElement(
    final Element element, final Exception ex) {
    assertEquals("log4j:throwable", element.getTagName());

    Node messageNode = element.getFirstChild();
    assertNotNull(messageNode);
    assertEquals(Node.TEXT_NODE, messageNode.getNodeType());

    String msg = ex.toString();
    assertEquals(msg, messageNode.getNodeValue().substring(0, msg.length()));
    assertNull(messageNode.getNextSibling());
  }

    /**
     * Checks a log4j:properties element against expectations.
     * @param element element, may not be null.
     * @param key key.
     * @param value value.
     */
    private void checkPropertiesElement(
      final Element element, final String key, final String value) {
      assertEquals("log4j:properties", element.getTagName());

      int childNodeCount = 0;
      for(Node child = element.getFirstChild();
               child != null;
               child = child.getNextSibling()) {
          if (child.getNodeType() == Node.ELEMENT_NODE) {
              assertEquals("log4j:data", child.getNodeName());
              Element childElement = (Element) child;
              assertEquals(key, childElement.getAttribute("name"));
              assertEquals(value, childElement.getAttribute("value"));
              childNodeCount++;
          }
      }
      assertEquals(1, childNodeCount);  
    }

  /**
   * Tests formatted results.
   * @throws Exception if parser can not be constructed or source is not a valid XML document.
   */
  public void testFormat() throws Exception {
    Logger logger = Logger.getLogger("org.apache.log4j.xml.XMLLayoutTest");
    LoggingEvent event =
      new LoggingEvent(
        "org.apache.log4j.Logger", logger, Level.INFO, "Hello, World", null);
    XMLLayout layout = (XMLLayout) createLayout();
    String result = layout.format(event);
    Element parsedResult = parse(result);
    checkEventElement(parsedResult, event);

    int childElementCount = 0;

    for (
      Node node = parsedResult.getFirstChild(); node != null;
        node = node.getNextSibling()) {
      switch (node.getNodeType()) {
      case Node.ELEMENT_NODE:
        childElementCount++;
        checkMessageElement((Element) node, "Hello, World");

        break;

      case Node.COMMENT_NODE:
        break;

      case Node.TEXT_NODE:

        //  should only be whitespace
        break;

      default:
        fail("Unexpected node type");

        break;
      }
    }

    assertEquals(1, childElementCount);
  }

  /**
   * Tests formatted results with an exception.
   * @throws Exception if parser can not be constructed or source is not a valid XML document.
   */
  public void testFormatWithException() throws Exception {
    Logger logger = Logger.getLogger("org.apache.log4j.xml.XMLLayoutTest");
    Exception ex = new IllegalArgumentException("'foo' is not a valid name");
    LoggingEvent event =
      new LoggingEvent(
        "org.apache.log4j.Logger", logger, Level.INFO, "Hello, World", ex);
    XMLLayout layout = (XMLLayout) createLayout();
    String result = layout.format(event);
    Element parsedResult = parse(result);
    checkEventElement(parsedResult, event);

    int childElementCount = 0;

    for (
      Node node = parsedResult.getFirstChild(); node != null;
        node = node.getNextSibling()) {
      switch (node.getNodeType()) {
      case Node.ELEMENT_NODE:
        childElementCount++;

        if (childElementCount == 1) {
          checkMessageElement((Element) node, "Hello, World");
        } else {
          checkThrowableElement((Element) node, ex);
        }

        break;

      case Node.COMMENT_NODE:
        break;

      case Node.TEXT_NODE:

        //  should only be whitespace
        break;

      default:
        fail("Unexpected node type");

        break;
      }
    }

    assertEquals(2, childElementCount);
  }

  /**
   * Tests formatted results with an exception.
   * @throws Exception if parser can not be constructed or source is not a valid XML document.
   */
  public void testFormatWithNDC() throws Exception {
    Logger logger = Logger.getLogger("org.apache.log4j.xml.XMLLayoutTest");
    NDC.push("NDC goes here");

    LoggingEvent event =
      new LoggingEvent(
        "org.apache.log4j.Logger", logger, Level.INFO, "Hello, World", null);
    XMLLayout layout = (XMLLayout) createLayout();
    String result = layout.format(event);
    NDC.pop();

    Element parsedResult = parse(result);
    checkEventElement(parsedResult, event);

    int childElementCount = 0;

    for (
      Node node = parsedResult.getFirstChild(); node != null;
        node = node.getNextSibling()) {
      switch (node.getNodeType()) {
      case Node.ELEMENT_NODE:
        childElementCount++;

        if (childElementCount == 1) {
          checkMessageElement((Element) node, "Hello, World");
        } else {
          checkNDCElement((Element) node, "NDC goes here");
        }

        break;

      case Node.COMMENT_NODE:
        break;

      case Node.TEXT_NODE:

        //  should only be whitespace
        break;

      default:
        fail("Unexpected node type");

        break;
      }
    }

    assertEquals(2, childElementCount);
  }

  /**
   * Tests getLocationInfo and setLocationInfo.
   */
  public void testGetSetLocationInfo() {
    XMLLayout layout = new XMLLayout();
    assertEquals(false, layout.getLocationInfo());
    layout.setLocationInfo(true);
    assertEquals(true, layout.getLocationInfo());
    layout.setLocationInfo(false);
    assertEquals(false, layout.getLocationInfo());
  }

  /**
   * Tests activateOptions().
   */
  public void testActivateOptions() {
    XMLLayout layout = new XMLLayout();
    layout.activateOptions();
  }

    /**
     * Level with arbitrary toString value.
     */
    private static final class ProblemLevel extends Level {
        private static final long serialVersionUID = 1L;
        /**
         * Construct new instance.
         * @param levelName level name, may not be null.
         */
        public ProblemLevel(final String levelName) {
            super(6000, levelName, 6);
        }
    }

    /**
     * Tests problematic characters in multiple fields.
     * @throws Exception if parser can not be constructed or source is not a valid XML document.
     */
    public void testProblemCharacters() throws Exception {
      String problemName = "com.example.bar<>&\"'";
      Logger logger = Logger.getLogger(problemName);
      Level level = new ProblemLevel(problemName);
      Exception ex = new IllegalArgumentException(problemName);
      String threadName = Thread.currentThread().getName();
      Thread.currentThread().setName(problemName);
      NDC.push(problemName);
      Hashtable mdcMap = MDC.getContext();
      if (mdcMap != null) {
          mdcMap.clear();
      }
      MDC.put(problemName, problemName);
      LoggingEvent event =
        new LoggingEvent(
          problemName, logger, level, problemName, ex);
      XMLLayout layout = (XMLLayout) createLayout();
      layout.setProperties(true);
      String result = layout.format(event);
      mdcMap = MDC.getContext();
      if (mdcMap != null) {
          mdcMap.clear();
      }
      Thread.currentThread().setName(threadName);

      Element parsedResult = parse(result);
      checkEventElement(parsedResult, event);

      int childElementCount = 0;

      for (
        Node node = parsedResult.getFirstChild(); node != null;
          node = node.getNextSibling()) {
        switch (node.getNodeType()) {
        case Node.ELEMENT_NODE:
          childElementCount++;
          switch(childElementCount) {
              case 1:
              checkMessageElement((Element) node, problemName);
              break;

              case 2:
              checkNDCElement((Element) node, problemName);
              break;

              case 3:
              checkThrowableElement((Element) node, ex);
              break;

              case 4:
              checkPropertiesElement((Element) node, problemName, problemName);
              break;

              default:
              fail("Unexpected element");
              break;
          }

          break;

        case Node.COMMENT_NODE:
          break;

        case Node.TEXT_NODE:

          //  should only be whitespace
          break;

        default:
          fail("Unexpected node type");

          break;
        }
      }
    }

    /**
      * Tests CDATA element within NDC content.  See bug 37560.
      */
    public void testNDCWithCDATA() throws Exception {
        Logger logger = Logger.getLogger("com.example.bar");
        Level level = Level.INFO;
        String ndcMessage ="<envelope><faultstring><![CDATA[The EffectiveDate]]></faultstring><envelope>";
        NDC.push(ndcMessage);
        LoggingEvent event =
          new LoggingEvent(
            "com.example.bar", logger, level, "Hello, World", null);
        Layout layout = createLayout();
        String result = layout.format(event);
        NDC.clear();
        Element parsedResult = parse(result);
        NodeList ndcs = parsedResult.getElementsByTagName("log4j:NDC");
        assertEquals(1, ndcs.getLength());
        StringBuffer buf = new StringBuffer();
        for(Node child = ndcs.item(0).getFirstChild();
                child != null;
                child = child.getNextSibling()) {
            buf.append(child.getNodeValue());
        }
        assertEquals(ndcMessage, buf.toString());
   }

    /**
      * Tests CDATA element within exception.  See bug 37560.
      */
    public void testExceptionWithCDATA() throws Exception {
        Logger logger = Logger.getLogger("com.example.bar");
        Level level = Level.INFO;
        String exceptionMessage ="<envelope><faultstring><![CDATA[The EffectiveDate]]></faultstring><envelope>";
        LoggingEvent event =
          new LoggingEvent(
            "com.example.bar", logger, level, "Hello, World", new Exception(exceptionMessage));
        Layout layout = createLayout();
        String result = layout.format(event);
        Element parsedResult = parse(result);
        NodeList throwables = parsedResult.getElementsByTagName("log4j:throwable");
        assertEquals(1, throwables.getLength());
        StringBuffer buf = new StringBuffer();
        for(Node child = throwables.item(0).getFirstChild();
                child != null;
                child = child.getNextSibling()) {
            buf.append(child.getNodeValue());
        }
        assertTrue(buf.toString().indexOf(exceptionMessage) != -1);
   }

}
