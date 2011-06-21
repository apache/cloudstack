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

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.LoggingEvent;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

/**
  <p>The TelnetAppender is a log4j appender that specializes in
  writing to a read-only socket.  The output is provided in a
  telnet-friendly way so that a log can be monitored over TCP/IP.
  Clients using telnet connect to the socket and receive log data.
  This is handy for remote monitoring, especially when monitoring a
  servlet.

  <p>Here is a list of the available configuration options:

  <table border=1>
   <tr>
   <th>Name</th>
   <th>Requirement</th>
   <th>Description</th>
   <th>Sample Value</th>
   </tr>

   <tr>
   <td>Port</td>
   <td>optional</td>
   <td>This parameter determines the port to use for announcing log events.  The default port is 23 (telnet).</td>
   <td>5875</td>
   </table>

   @author <a HREF="mailto:jay@v-wave.com">Jay Funnell</a>
*/

public class TelnetAppender extends AppenderSkeleton {

  private SocketHandler sh;
  private int port = 23;

  /** 
      This appender requires a layout to format the text to the
      attached client(s). */
  public boolean requiresLayout() {
    return true;
  }

  /** all of the options have been set, create the socket handler and
      wait for connections. */
  public void activateOptions() {
    try {
      sh = new SocketHandler(port);
      sh.start();
    }
    catch(InterruptedIOException e) {
      Thread.currentThread().interrupt();
      e.printStackTrace();
    } catch(IOException e) {
      e.printStackTrace();
    } catch(RuntimeException e) {
      e.printStackTrace();
    }
    super.activateOptions();
  }

  public
  int getPort() {
    return port;
  }

  public
  void setPort(int port) {
    this.port = port;
  }


  /** shuts down the appender. */
  public void close() {
    if (sh != null) {
        sh.close();
        try {
            sh.join();
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
  }

  /** Handles a log event.  For this appender, that means writing the
    message to each connected client.  */
  protected void append(LoggingEvent event) {
      if(sh != null) {
        sh.send(layout.format(event));
        if(layout.ignoresThrowable()) {
            String[] s = event.getThrowableStrRep();
            if (s != null) {
                StringBuffer buf = new StringBuffer();
                for(int i = 0; i < s.length; i++) {
                    buf.append(s[i]);
                    buf.append("\r\n");
                }
                sh.send(buf.toString());
            }
        }
      }
  }

  //---------------------------------------------------------- SocketHandler:

  /** The SocketHandler class is used to accept connections from
      clients.  It is threaded so that clients can connect/disconnect
      asynchronously. */
  protected class SocketHandler extends Thread {

    private Vector writers = new Vector();
    private Vector connections = new Vector();
    private ServerSocket serverSocket;
    private int MAX_CONNECTIONS = 20;

    public void finalize() {
        close();
    }
      
    /** 
    * make sure we close all network connections when this handler is destroyed.
    * @since 1.2.15 
    */
    public void close() {
      synchronized(this) {
        for(Enumeration e = connections.elements();e.hasMoreElements();) {
            try {
                ((Socket)e.nextElement()).close();
            } catch(InterruptedIOException ex) {
                Thread.currentThread().interrupt();
            } catch(IOException ex) {
            } catch(RuntimeException ex) {
            }
        }
      }

      try {
        serverSocket.close();
      } catch(InterruptedIOException ex) {
          Thread.currentThread().interrupt();
      } catch(IOException ex) {
      } catch(RuntimeException ex) {
      }
    }

    /** sends a message to each of the clients in telnet-friendly output. */
    public synchronized void send(final String message) {
      Iterator ce = connections.iterator();
      for(Iterator e = writers.iterator();e.hasNext();) {
        ce.next();
        PrintWriter writer = (PrintWriter)e.next();
        writer.print(message);
        if(writer.checkError()) {
          ce.remove();
          e.remove();
        }
      }
    }

    /** 
	Continually accepts client connections.  Client connections
        are refused when MAX_CONNECTIONS is reached. 
    */
    public void run() {
      while(!serverSocket.isClosed()) {
        try {
          Socket newClient = serverSocket.accept();
          PrintWriter pw = new PrintWriter(newClient.getOutputStream());
          if(connections.size() < MAX_CONNECTIONS) {
            synchronized(this) {
                connections.addElement(newClient);
                writers.addElement(pw);
                pw.print("TelnetAppender v1.0 (" + connections.size()
		            + " active connections)\r\n\r\n");
                pw.flush();
            }
          } else {
            pw.print("Too many connections.\r\n");
            pw.flush();
            newClient.close();
          }
        } catch(Exception e) {
          if (e instanceof InterruptedIOException || e instanceof InterruptedException) {
              Thread.currentThread().interrupt();
          }
          if (!serverSocket.isClosed()) {
            LogLog.error("Encountered error while in SocketHandler loop.", e);
          }
          break;
        }
      }

      try {
          serverSocket.close();
      } catch(InterruptedIOException ex) {
          Thread.currentThread().interrupt();
      } catch(IOException ex) {
      }
    }

    public SocketHandler(int port) throws IOException {
      serverSocket = new ServerSocket(port);
      setName("TelnetAppender-" + getName() + "-" + port);
    }

  }
}
