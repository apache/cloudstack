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

package org.apache.log4j.helpers;


import java.io.Writer;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.UnknownHostException;
import java.net.SocketException;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;

/**
   SyslogWriter is a wrapper around the java.net.DatagramSocket class
   so that it behaves like a java.io.Writer.

   @since 0.7.3
*/
public class SyslogWriter extends Writer {

  final int SYSLOG_PORT = 514;
  /**
   *  Host string from last constructed SyslogWriter.
   *  @deprecated
   */
  static String syslogHost;
  
  private InetAddress address;
  private final int port;
  private DatagramSocket ds;

  /**
   *  Constructs a new instance of SyslogWriter.
   *  @param syslogHost host name, may not be null.  A port
   *  may be specified by following the name or IPv4 literal address with
   *  a colon and a decimal port number.  To specify a port with an IPv6
   *  address, enclose the IPv6 address in square brackets before appending
   *  the colon and decimal port number.
   */
  public
  SyslogWriter(final String syslogHost) {
    SyslogWriter.syslogHost = syslogHost;
    if (syslogHost == null) {
        throw new NullPointerException("syslogHost");
    }
    
    String host = syslogHost;
    int urlPort = -1;
    
    //
    //  If not an unbracketed IPv6 address then
    //      parse as a URL
    //
    if (host.indexOf("[") != -1 || host.indexOf(':') == host.lastIndexOf(':')) {
        try {
            URL url = new URL("http://" + host);
            if (url.getHost() != null) {
                host = url.getHost();
                //   if host is a IPv6 literal, strip off the brackets
                if(host.startsWith("[") && host.charAt(host.length() - 1) == ']') {
                    host = host.substring(1, host.length() - 1);
                }
                urlPort = url.getPort();
            }
        } catch(MalformedURLException e) {
      		LogLog.error("Malformed URL: will attempt to interpret as InetAddress.", e);
        }
    }
    
    if (urlPort == -1) {
        urlPort = SYSLOG_PORT;
    }
    port = urlPort;

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
  void write(final String string) throws IOException {

    if(this.ds != null && this.address != null) {
        byte[] bytes = string.getBytes();
        //
        //  syslog packets must be less than 1024 bytes
        //
        int bytesLength = bytes.length;
        if (bytesLength >= 1024) {
            bytesLength = 1024;
        }
        DatagramPacket packet = new DatagramPacket(bytes, bytesLength,
                               address, port);
        ds.send(packet);
    }
    
  }

  public
  void flush() {}

  public void close() {
      if (ds != null) {
          ds.close();
      }
  }
}
