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

import java.io.Writer;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.UnknownHostException;
import java.net.SocketException;
import java.io.IOException;

import org.apache.log4j.helpers.LogLog;

/**
 * DatagramStringWriter is a wrapper around the java.net.DatagramSocket class
 * so that it behaves like a java.io.Writer.
 */
public class DatagramStringWriter extends Writer {

  static final int SYSLOG_PORT = 514;

  private int port;
  private String host;
  private String encoding;
  private String prefix;

  private InetAddress address;
  private DatagramSocket ds;

  /**
   * This constructor assumes that it is sending to a remote syslog daemon
   * on the normal syslog port (514), and uses the default platform character
   * encoding when converting the message string to a byte sequence.
   */
  public
  DatagramStringWriter(String host) {
    this(host, SYSLOG_PORT, null, null);
  }

  /**
   * This constructor sends messages to the specified host and port, and
   * uses the default platform character encoding when converting the message
   * string to a byte sequence.
   */
  public
  DatagramStringWriter(String host, int port) {
    this(host, port, null, null);
  }

  /**
   * This constructor sends messages to the specified host and port, and
   * uses the specified character encoding when converting the message
   * string to a byte sequence.
   */
  public
  DatagramStringWriter(String host, int port, String encoding) {
    this(host, port, null, null);
  }
  /**
   * This constructor sends messages to the specified host and port, and
   * uses the specified character encoding when converting the message
   * string to a byte sequence; the specified prefix (which may be null)
   * is prepended to each message.
   */
  public
  DatagramStringWriter(String host, int port, String encoding, String prefix) {
    this.host = host;
    this.port = port;
    this.encoding = encoding;
    this.prefix = prefix;

    try {
      this.address = InetAddress.getByName(host);
    }
    catch (UnknownHostException e) {
      LogLog.error("Could not find " + host +
			 ". All logging will FAIL.", e);
    }

    try {
      this.ds = new DatagramSocket();
    }
    catch (SocketException e) {
      e.printStackTrace();
      LogLog.error("Could not instantiate DatagramSocket to " + host +
			 ". All logging will FAIL.", e);
    }
  }


  public
  void write(char[] buf, int off, int len) throws IOException {
    this.write(new String(buf, off, len));
  }

  public
  void write(String string) throws IOException {
    if (prefix != null) {
      string = prefix + string;
    }
    
    byte[] rawData;
    if (this.encoding == null)
    {
      // convert to byte sequence using platform's default encoding
      rawData = string.getBytes();
    }
    else
    {
      // convert to specified encoding - which may be sequence of
      // 8-bit chars, or multi-byte encodings like UTF-8 or UTF-16.
      // The receiving end had better be expecting whatever encoding
      // is used here on the sending end!
      rawData = string.getBytes(encoding);
    }

    DatagramPacket packet =
      new DatagramPacket(
                 rawData,
					       rawData.length,
					       address,
                 port);

    if(this.ds != null)
    {
      ds.send(packet);
    }
    else
    {
      LogLog.error(
        "write: failed to create DatagramPacket");
    }
  }

  public
  void flush() {}

  public
  void close() {}

  /**
   * Set a string to be prefixed to every message sent by this Writer.
   * For example, this method could be used to prepend a syslog
   * facility/priority code on the front of each message.
   * <p>
   * Note that this method is not synchronised, so should not be called in
   * a situation where other threads may be logging messages at the same
   * moment.
   * <p>
   * @param prefix may be a prefix string, or null which indicates no
   *  prefix should be added.
   */
  public
  void setPrefix(String prefix){
    this.prefix = prefix;
  }
}
