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

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.UnknownHostException;
import java.net.SocketException;

import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.Category;
import org.apache.log4j.Priority;
import org.apache.log4j.Layout;

import org.apache.log4j.helpers.SingleLineTracerPrintWriter;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.helpers.QuietWriter;


/**
    Use DatagramStringAppender to send log messages to a remote daemon
    which accepts Datagram (UDP) messages.
    <p>
    The benefits of UDP are that the client is guarunteed not to
    slow down if the network or remote log daemon is slow, and that
    no permanent TCP connection between client and server exists.
    <p>
    The disadvantages are that log messages can be lost if the network
    or remote daemon are under excessive load.
    <p>
    This class builts the final message string <b>before</b> sending
    the UDP packet, hence the "string" component in the class name. This
    means that the receiving application can be written in any language.
    The data is transmitted in whatever encoding is specified in the
    configuration file; this may be an 8-bit encoding (eg ISO-8859-1, also
    known as LATIN-1) or a larger encoding, eg UTF-16.
    <p>
    An alternative to building the message string within DatagramStringAppender
    would be to serialize & send the complete logging event object (perhaps
    such a class could be called a DatagramSerialAppender??). The
    receiving end could then be configured with appropriate Layout objects
    to generate the actual logged messages. This would ensure that the
    logging of messages from different sources is done in a consistent
    format, and give a central place to configure that format. It would ensure
    (by transmitting messages as unicode) that the receiving end could control
    the encoding in which the output is generated. It also would possibly allow
    he receiving end to use the full log4j flexibility to pass the event to
    different appenders at the receiving end, as the category information is
    retained, etc. However, this does require that the receiving end is in
    java, and that all clients of the logging daemon are java applications.
    In contrast, this DatagramStringAppender can send mesages to a log daemon
    that accepts messages from a variety of sources.

    @author Simon Kitching
 */
public class DatagramStringAppender extends AppenderSkeleton {

   /**
     A string constant used in naming the option for setting the destination
     server for messages.  Current value of this string constant is
     <b>DatagramHost</b>. */
  public static final String DATAGRAM_HOST_OPTION = "DatagramHost";

   /**
     A string constant used in naming the option for setting the destination
     port for messages. Current value of this string constant is
     <b>DatagramPort</b>. */
  public static final String DATAGRAM_PORT_OPTION = "DatagramPort";

   /**
     A string constant used in naming the option for setting the character
     encoding used when generating the log message. Current value of this
     string constant is <b>DatagramEncoding</b>. */
  public static final String DATAGRAM_ENCODING_OPTION = "DatagramEncoding";

   /**
     The default value for the "host" attribute, ie the machine to which
     messages are sent. Current value of this string constant is
     <b>localhost</b>. */
  public static final String DEFAULT_HOST = "localhost";

   /**
     The default value for the "port" attribute, ie the UDP port to which
     messages are sent. Current value of this integer constant is
     <b>8200</b>. This value was chosen for no particular reason. */
  public static final int DEFAULT_PORT = 8200;

   /**
     The default value for the "encoding" attribute, ie the way in which
     unicode message strings are converted into a stream of bytes before
     their transmission as a UDP packet. The current value of this constant
     is <b>null</b>, which means that the default platform encoding will
     be used. */
  public static final String DEFAULT_ENCODING = null;

  String host = DEFAULT_HOST;
  int port = DEFAULT_PORT;
  String encoding = DEFAULT_ENCODING;

  SingleLineTracerPrintWriter stp;
  QuietWriter qw;

  public
  DatagramStringAppender() {
    this.setDestination(DEFAULT_HOST, DEFAULT_PORT, DEFAULT_ENCODING);
  }

  public
  DatagramStringAppender(Layout layout) {
    this.setLayout(layout);
    this.setDestination(DEFAULT_HOST, DEFAULT_PORT, DEFAULT_ENCODING);
  }

  public
  DatagramStringAppender(Layout layout, String host, int port) {
    this.setLayout(layout);
    this.setDestination(host, port, DEFAULT_ENCODING);
  }

  public
  DatagramStringAppender(Layout layout, String host, int port, String encoding) {
    this.setLayout(layout);
    this.setDestination(host, port, encoding);
  }

  /**
     Release any resources held by this Appender
   */
  public
  void close() {
    closed = true;
    // A DatagramWriter is UDP based and needs no opening. Hence, it
    // can't be closed. We just unset the variables here.
    qw = null;
    stp = null;
  }

  public
  void append(LoggingEvent event) {
    if(!isAsSevereAsThreshold(event.priority))
      return;

    // We must not attempt to append if qw is null.
    if(qw == null) {
      errorHandler.error(
        "No host is set for DatagramStringAppender named \""
        +	this.name + "\".");
      return;
    }

    String buffer = layout.format(event);
    qw.write(buffer);

    if(event.throwable != null)
      event.throwable.printStackTrace(stp);
    else if (event.throwableInformation != null) {
      // we must be the receiver of a serialized/deserialized LoggingEvent;
      // the event's throwable member is transient, ie becomes null when
      // deserialized, but that's ok because throwableInformation should
      // have the string equivalent of the same info (ie stack trace)
      qw.write(event.throwableInformation);
    }
  }

  /**
     Activate the options set via the setOption method.

     @see #setOption
  */
  public
  void activateOptions() {
    this.setDestination(this.host, this.port, this.encoding);
  }

  /**
     Returns the option names for this component, namely the string
     array consisting of {{@link #DATAGRAM_HOST_OPTION}, {@link
     #DATAGRAM_PORT_OPTION}, {@link #DATAGRAM_ENCODING_OPTION}  */
  public
  String[] getOptionStrings() {
    return OptionConverter.concatanateArrays(super.getOptionStrings(),
		      new String[] {
            DATAGRAM_HOST_OPTION,
            DATAGRAM_PORT_OPTION,
            DATAGRAM_ENCODING_OPTION});
  }

  /**
     The DatagramStringAppender requires a layout. Hence, this method return
     <code>true</code>.

     @since 0.8.4 */
  public
  boolean requiresLayout() {
    return true;
  }

  /**
    Set DatagramStringAppender specific parameters.
    <p>
    The recognized options are <b>DatagramHost</b>, <b>DatagramPort</b> and
    <b>DatagramEncoding</b>, i.e. the values of the string constants
    {@link #DATAGRAM_HOST_OPTION}, {@link #DATAGRAM_PORT_OPTION} and
    {@link #DATAGRAM_ENCODING_OPTION} respectively.
    <p>
    <dl>
    <p>
    <dt><b>DatagramHost</b>
    <dd>
    The name (or ip address) of the host machine where log output should go.
    If the DatagramHost is not set, then this appender will default to
    {@link #DEFAULT_HOST}.
    <p>
    <dt><b>DatagramPort</b>
    <dd>
    The UDP port number where log output should go. See {@link #DEFAULT_PORT}
    <p>
    <dt><b>DatagramEncoding</b>
    <dd>
    The ISO character encoding to be used when converting the Unicode
    message to a sequence of bytes within a UDP packet. If not defined, then
    the encoding defaults to the default platform encoding.
    </dl>
    */
  public
  void setOption(String option, String value) {
    if(value == null) return;

    super.setOption(option, value);

    if(option.equals(DATAGRAM_HOST_OPTION))
    {
      this.host = value;
    }
    else if(option.equals(DATAGRAM_PORT_OPTION))
    {
      this.port = OptionConverter.toInt(value, DEFAULT_PORT);
    }
    else if(option.equals(DATAGRAM_ENCODING_OPTION))
    {
      this.encoding = value;
    }
  }

  public
  void setDestination(String host, int port, String encoding) {
    if (host==null) {
      LogLog.error("setDestination: host is null");
      host = DEFAULT_HOST;
    }
    
    this.host = host;
    this.port = port;
    this.encoding = encoding;

    this.qw = new QuietWriter(
        new DatagramStringWriter(host, port, encoding),
        errorHandler);
    this.stp = new SingleLineTracerPrintWriter(qw);
  }

  public
  void setLayout(Layout layout) {
    this.layout = layout;
  }
}
