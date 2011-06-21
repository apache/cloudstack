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
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.helpers.MDCKeySetExtractor;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.LocationInfo;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.util.Set;
import java.util.Properties;
import java.util.Arrays;
import java.util.TimeZone;
import java.nio.charset.Charset;
import java.nio.ByteBuffer;
import org.apache.log4j.pattern.CachedDateFormat;
import java.text.SimpleDateFormat;

import org.w3c.dom.Document;

import org.xml.sax.helpers.AttributesImpl;


/**
 * XSLTLayout transforms each event as a document using
 * a specified or default XSLT transform.  The default
 * XSLT transform produces a result similar to XMLLayout.
 *
 * When used with a FileAppender or similar, the transformation of
 * an event will be appended to the results for previous
 * transforms.  If each transform results in an XML element, then
 * resulting file will only be an XML entity
 * since an XML document requires one and only one top-level element.
 * To process the entity, reference it in a XML document like so:
 *
 * <pre>
 *  &lt;!DOCTYPE log4j:eventSet [&lt;!ENTITY data SYSTEM &quot;data.xml&quot;&gt;]&gt;
 *
 *  &lt;log4j:eventSet xmlns:log4j=&quot;http://jakarta.apache.org/log4j/&quot;&gt;
 *    &amp;data
 *  &lt;/log4j:eventSet&gt;
 *
 * </pre>
 *
 * The layout will detect the encoding and media-type specified in
 * the transform.  If no encoding is specified in the transform,
 * an xsl:output element specifying the US-ASCII encoding will be inserted
 * before processing the transform.  If an encoding is specified in the transform,
 * the same encoding should be explicitly specified for the appender.
 *
 * Extracting MDC values can be expensive when used with log4j releases
 * prior to 1.2.15.  Output of MDC values is enabled by default
 * but be suppressed by setting properties to false.
 *
 * Extracting location info can be expensive regardless of log4j version.  
 * Output of location info is disabled by default but can be enabled
 * by setting locationInfo to true.
 *
 * Embedded transforms in XML configuration should not
 * depend on namespace prefixes defined earlier in the document
 * as namespace aware parsing in not generally performed when
 * using DOMConfigurator.  The transform will serialize
 * and reparse to get the namespace aware document needed.
 *
 */
public final class XSLTLayout extends Layout
        implements UnrecognizedElementHandler {
    /**
     * Namespace for XSLT.
     */
    private static final String XSLT_NS = "http://www.w3.org/1999/XSL/Transform";
    /**
     * Namespace for log4j events.
     */
    private static final String LOG4J_NS = "http://jakarta.apache.org/log4j/";
    /**
     * Whether location information should be written.
     */
    private boolean locationInfo = false;
    /**
     * media-type (mime type) extracted from XSLT transform.
     */
    private String mediaType = "text/plain";
    /**
     * Encoding extracted from XSLT transform.
     */
    private Charset encoding;
    /**
     * Transformer factory.
     */
    private SAXTransformerFactory transformerFactory;
    /**
     * XSLT templates.
     */
    private Templates templates;
    /**
     * Output stream.
     */
    private final ByteArrayOutputStream outputStream;
    /**
     * Whether throwable information should be ignored.
     */
    private boolean ignoresThrowable = false;
    /**
     * Whether properties should be extracted.
     */
    private boolean properties = true;
    /**
     * Whether activateOptions has been called.
     */
    private boolean activated = false;

    /**
     * DateFormat for UTC time.
     */
    private final CachedDateFormat utcDateFormat;

    /**
     * Default constructor.
     *
     */
    public XSLTLayout() {
        outputStream = new ByteArrayOutputStream();
        transformerFactory = (SAXTransformerFactory)
                TransformerFactory.newInstance();

        SimpleDateFormat zdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        zdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        utcDateFormat = new CachedDateFormat(zdf, 1000);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized String getContentType() {
        return mediaType;
    }

    /**
     * The <b>LocationInfo </b> option takes a boolean value. By default, it is
     * set to false which means there will be no location information output by
     * this layout. If the the option is set to true, then the file name and line
     * number of the statement at the origin of the log statement will be output.
     *
     * <p>
     * If you are embedding this layout within an {@link
     * org.apache.log4j.net.SMTPAppender} then make sure to set the
     * <b>LocationInfo </b> option of that appender as well.
     *
     * @param flag new value.
     */
    public synchronized void setLocationInfo(final boolean flag) {
      locationInfo = flag;
    }

    /**
     * Gets whether location info should be output.
     * @return if location is output.
     */
    public synchronized boolean getLocationInfo() {
      return locationInfo;
    }

    /**
     * Sets whether MDC key-value pairs should be output, default false.
     * @param flag new value.
     */
    public synchronized void setProperties(final boolean flag) {
      properties = flag;
    }

    /**
     * Gets whether MDC key-value pairs should be output.
     * @return true if MDC key-value pairs are output.
     */
    public synchronized boolean getProperties() {
      return properties;
    }


    /** {@inheritDoc} */
    public synchronized void activateOptions() {
        if (templates == null) {
            try {
                InputStream is = XSLTLayout.class.getResourceAsStream("default.xslt");
                StreamSource ss = new StreamSource(is);
                templates = transformerFactory.newTemplates(ss);
                encoding = Charset.forName("US-ASCII");
                mediaType = "text/plain";
            } catch (Exception ex) {
                LogLog.error("Error loading default.xslt", ex);
            }
        }
        activated = true;
    }

    /**
     * Gets whether throwables should not be output.
     * @return true if throwables should not be output.
     */
    public synchronized boolean ignoresThrowable() {
        return ignoresThrowable;
    }

    /**
     * Sets whether throwables should not be output.
     * @param ignoresThrowable if true, throwables should not be output.
    */
    public synchronized void setIgnoresThrowable(boolean ignoresThrowable) {
      this.ignoresThrowable = ignoresThrowable;
    }



    /**
     * {@inheritDoc}
     */
    public synchronized String format(final LoggingEvent event) {
      if (!activated) {
          activateOptions();
      }
      if (templates != null && encoding != null) {
          outputStream.reset();

          try {
            TransformerHandler transformer =
                      transformerFactory.newTransformerHandler(templates);

            transformer.setResult(new StreamResult(outputStream));
            transformer.startDocument();

            //
            //   event element
            //
            AttributesImpl attrs = new AttributesImpl();
            attrs.addAttribute(null, "logger", "logger",
                    "CDATA", event.getLoggerName());
            attrs.addAttribute(null, "timestamp", "timestamp",
                    "CDATA", Long.toString(event.timeStamp));
            attrs.addAttribute(null, "level", "level",
                    "CDATA", event.getLevel().toString());
            attrs.addAttribute(null, "thread", "thread",
                    "CDATA", event.getThreadName());
            StringBuffer buf = new StringBuffer();
            utcDateFormat.format(event.timeStamp, buf);
            attrs.addAttribute(null, "time", "time", "CDATA", buf.toString());


            transformer.startElement(LOG4J_NS, "event", "event", attrs);
            attrs.clear();

            //
            //   message element
            //
            transformer.startElement(LOG4J_NS, "message", "message", attrs);
            String msg = event.getRenderedMessage();
            if (msg != null && msg.length() > 0) {
                transformer.characters(msg.toCharArray(), 0, msg.length());
            }
            transformer.endElement(LOG4J_NS, "message", "message");

            //
            //    NDC element
            //
            String ndc = event.getNDC();
            if (ndc != null) {
                transformer.startElement(LOG4J_NS, "NDC", "NDC", attrs);
                char[] ndcChars = ndc.toCharArray();
                transformer.characters(ndcChars, 0, ndcChars.length);
                transformer.endElement(LOG4J_NS, "NDC", "NDC");
            }

            //
            //    throwable element unless suppressed
            //
              if (!ignoresThrowable) {
                String[] s = event.getThrowableStrRep();
                if (s != null) {
                    transformer.startElement(LOG4J_NS, "throwable",
                            "throwable", attrs);
                    char[] nl = new char[] { '\n' };
                    for (int i = 0; i < s.length; i++) {
                        char[] line = s[i].toCharArray();
                        transformer.characters(line, 0, line.length);
                        transformer.characters(nl, 0, nl.length);
                    }
                    transformer.endElement(LOG4J_NS, "throwable", "throwable");
                }
              }

              //
              //     location info unless suppressed
              //
              //
              if (locationInfo) {
                LocationInfo locationInfo = event.getLocationInformation();
                attrs.addAttribute(null, "class", "class", "CDATA",
                        locationInfo.getClassName());
                attrs.addAttribute(null, "method", "method", "CDATA",
                          locationInfo.getMethodName());
                attrs.addAttribute(null, "file", "file", "CDATA",
                            locationInfo.getFileName());
                attrs.addAttribute(null, "line", "line", "CDATA",
                            locationInfo.getLineNumber());
                transformer.startElement(LOG4J_NS, "locationInfo",
                        "locationInfo", attrs);
                transformer.endElement(LOG4J_NS, "locationInfo",
                        "locationInfo");
              }

              if (properties) {
                //
                //    write MDC contents out as properties element
                //
                Set mdcKeySet = MDCKeySetExtractor.INSTANCE.getPropertyKeySet(event);

                if ((mdcKeySet != null) && (mdcKeySet.size() > 0)) {
                    attrs.clear();
                    transformer.startElement(LOG4J_NS,
                            "properties", "properties", attrs);
                    Object[] keys = mdcKeySet.toArray();
                    Arrays.sort(keys);
                    for (int i = 0; i < keys.length; i++) {
                        String key = keys[i].toString();
                        Object val = event.getMDC(key);
                        attrs.clear();
                        attrs.addAttribute(null, "name", "name", "CDATA", key);
                        attrs.addAttribute(null, "value", "value",
                                "CDATA", val.toString());
                        transformer.startElement(LOG4J_NS,
                                "data", "data", attrs);
                        transformer.endElement(LOG4J_NS, "data", "data");
                    }
                }
              }


            transformer.endElement(LOG4J_NS, "event", "event");
            transformer.endDocument();

            String body = encoding.decode(
                    ByteBuffer.wrap(outputStream.toByteArray())).toString();
            outputStream.reset();
            //
            //   must remove XML declaration since it may
            //      result in erroneous encoding info
            //      if written by FileAppender in a different encoding
            if (body.startsWith("<?xml ")) {
                int endDecl = body.indexOf("?>");
                if (endDecl != -1) {
                    for(endDecl += 2; 
					     endDecl < body.length() &&
						 (body.charAt(endDecl) == '\n' || body.charAt(endDecl) == '\r'); 
						 endDecl++);
                    return body.substring(endDecl);
                }
            }
            return body;
          } catch (Exception ex) {
              LogLog.error("Error during transformation", ex);
              return ex.toString();
          }
      }
      return "No valid transform or encoding specified.";
    }

    /**
     * Sets XSLT transform.
     * @param xsltdoc DOM document containing XSLT transform source,
     * may be modified.
     * @throws TransformerConfigurationException if transformer can not be
     * created.
     */
    public void setTransform(final Document xsltdoc)
            throws TransformerConfigurationException {
        //
        //  scan transform source for xsl:output elements
        //    and extract encoding, media (mime) type and output method
        //
        String encodingName = null;
        mediaType = null;
        String method = null;
        NodeList nodes = xsltdoc.getElementsByTagNameNS(
                XSLT_NS,
                "output");
        for(int i = 0; i < nodes.getLength(); i++) {
            Element outputElement = (Element) nodes.item(i);
            if (method == null || method.length() == 0) {
                method = outputElement.getAttributeNS(null, "method");
            }
            if (encodingName == null || encodingName.length() == 0) {
                encodingName = outputElement.getAttributeNS(null, "encoding");
            }
            if (mediaType == null || mediaType.length() == 0) {
                mediaType = outputElement.getAttributeNS(null, "media-type");
            }
        }

        if (mediaType == null || mediaType.length() == 0) {
            if ("html".equals(method)) {
                mediaType = "text/html";
            } else if ("xml".equals(method)) {
                mediaType = "text/xml";
            } else {
                mediaType = "text/plain";
            }
        }

        //
        //  if encoding was not specified,
        //     add xsl:output encoding=US-ASCII to XSLT source
        //
        if (encodingName == null || encodingName.length() == 0) {
            Element transformElement = xsltdoc.getDocumentElement();
            Element outputElement = xsltdoc.
                    createElementNS(XSLT_NS, "output");
            outputElement.setAttributeNS(null, "encoding", "US-ASCII");
            transformElement.insertBefore(outputElement, transformElement.getFirstChild());
            encoding = Charset.forName("US-ASCII");
        } else {
            encoding = Charset.forName(encodingName);
        }

        DOMSource transformSource = new DOMSource(xsltdoc);
        
        templates = transformerFactory.newTemplates(transformSource);

    }

    /**
     * {@inheritDoc}
     */
    public boolean parseUnrecognizedElement(final Element element,
                                            final Properties props)
            throws Exception {
        if (XSLT_NS.equals(element.getNamespaceURI()) ||
                element.getNodeName().indexOf("transform") != -1 ||
                element.getNodeName().indexOf("stylesheet") != -1) {
            //
            //   DOMConfigurator typically not namespace aware
            //     serialize tree and reparse.
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            DOMSource source = new DOMSource(element);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.transform(source, new StreamResult(os));

            ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
            DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
            domFactory.setNamespaceAware(true);
            Document xsltdoc = domFactory.newDocumentBuilder().parse(is);
            setTransform(xsltdoc);
            return true;
        }
        return false;
    }


}
