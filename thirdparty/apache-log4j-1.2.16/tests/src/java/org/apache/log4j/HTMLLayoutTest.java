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

import org.apache.log4j.spi.LoggingEvent;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.Reader;
import java.io.StringReader;
import java.util.Hashtable;


/**
 * Test for HTMLLayout.
 *
 * @author Curt Arnold
 */
public class HTMLLayoutTest extends LayoutTest {
  /**
   * Construct new instance of XMLLayoutTest.
   *
   * @param testName test name.
   */
  public HTMLLayoutTest(final String testName) {
    super(testName, "text/html", false, null, null);
  }

  /**
   * @{inheritDoc}
   */
  protected Layout createLayout() {
    return new HTMLLayout();
  }

  /**
   * Parses the string as the body of an XML document and returns the document element.
   * @param source source string.
   * @return document element.
   * @throws Exception if parser can not be constructed or source is not a valid XML document.
   */
  private Document parse(final String source) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(false);
    factory.setCoalescing(true);

    DocumentBuilder builder = factory.newDocumentBuilder();
    Reader reader = new StringReader(source);

    return builder.parse(new InputSource(reader));
  }

  /**
   * Tests formatted results.
   * @throws Exception if unable to create parser or output is not valid XML.
   */
  public void testFormat() throws Exception {
    Logger logger = Logger.getLogger("org.apache.log4j.xml.HTMLLayoutTest");
    NDC.push("NDC goes here");

    LoggingEvent event =
      new LoggingEvent(
        "org.apache.log4j.Logger", logger, Level.INFO, "Hello, World", null);
    HTMLLayout layout = (HTMLLayout) createLayout();
    layout.setLocationInfo(true);

    String result = layout.format(event);
    NDC.pop();

    String src =
      "<!DOCTYPE body [ <!ENTITY nbsp ' '>]><body>" + result + "</body>";
    parse(src);
  }

  /**
   * Tests getHeader.
   */
  public void testGetHeader() {
    assertEquals("<!DOCTYPE", createLayout().getHeader().substring(0, 9));
  }

  /**
   * Tests getHeader with locationInfo = true.
   */
  public void testGetHeaderWithLocation() {
    HTMLLayout layout = (HTMLLayout) createLayout();
    layout.setLocationInfo(true);
    assertEquals("<!DOCTYPE", layout.getHeader().substring(0, 9));
  }

  /**
   * Tests getFooter.
   */
  public void testGetFooter() {
    assertEquals("</table>", createLayout().getFooter().substring(0, 8));
  }

  /**
   * Tests getLocationInfo and setLocationInfo.
   */
  public void testGetSetLocationInfo() {
    HTMLLayout layout = new HTMLLayout();
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
    HTMLLayout layout = new HTMLLayout();
    layout.activateOptions();
  }

  /**
   * Tests getTitle and setTitle.
   */
  public void testGetSetTitle() {
    HTMLLayout layout = new HTMLLayout();
    assertEquals("Log4J Log Messages", layout.getTitle());
    layout.setTitle(null);
    assertNull(layout.getTitle());

    String newTitle = "A treatise on messages of log persuasion";
    layout.setTitle(newTitle);
    assertEquals(newTitle, layout.getTitle());
  }

  /**
   * Tests buffer downsizing and DEBUG and WARN colorization code paths.
   */
  public void testFormatResize() {
    Logger logger = Logger.getLogger("org.apache.log4j.xml.HTMLLayoutTest");
    NDC.clear();

    char[] msg = new char[2000];

    for (int i = 0; i < msg.length; i++) {
      msg[i] = 'A';
    }

    LoggingEvent event1 =
      new LoggingEvent(
        "org.apache.log4j.Logger", logger, Level.DEBUG, new String(msg), null);
    HTMLLayout layout = (HTMLLayout) createLayout();
    layout.setLocationInfo(true);

    String result = layout.format(event1);
    Exception ex = new IllegalArgumentException("'foo' is not a valid value.");
    LoggingEvent event2 =
      new LoggingEvent(
        "org.apache.log4j.Logger", logger, Level.WARN, "Hello, World", ex);
    result = layout.format(event2);
    assertEquals(
      Layout.LINE_SEP + "<tr>",
      result.substring(0, Layout.LINE_SEP.length() + 4));
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
     * @throws Exception if parser can not be constructed
     *  or source is not a valid XML document.
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
      HTMLLayout layout = (HTMLLayout) createLayout();
      String result = layout.format(event);
      mdcMap = MDC.getContext();
      if (mdcMap != null) {
        mdcMap.clear();
      }

      Thread.currentThread().setName(threadName);

      //
      //  do a little fixup to make output XHTML
      //
      StringBuffer buf = new StringBuffer(
              "<!DOCTYPE table [<!ENTITY nbsp ' '>]><table>");
      buf.append(result);
      buf.append("</table>");
      String doc = buf.toString();
      for(int i = doc.lastIndexOf("<br>");
          i != -1;
          i = doc.lastIndexOf("<br>", i - 1)) {
          buf.replace(i, i + 4, "<br/>");
      }

      parse(buf.toString());
    }

}
