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
package com.psibt.framework.net;

import java.net.*;
import java.io.*;
import java.util.*;
import org.apache.log4j.*;

/**
 * This class implements a HTTP-server frame. All HTTP-requests are handled by HTTPRequestHandler
 * classes which implement the <code>HTTPRequestHandler</code> interface. Every RequestHandler has
 * to be registered in the PluggableHTTPServer with the <code>addRequestHandler</code> method.
 * A new thread is created for each connection to handle the request. If all reply data are sent
 * to the client the connection is closed and the thread ends.
 * An example how to use the PluggableHTTPServer class can be found in the <code>main</code> method
 * at the end of the source file.
 *
 * @author <a HREF="mailto:V.Mentzner@psi-bt.de">Volker Mentzner</a>
 */
public class PluggableHTTPServer implements Runnable {

  public static final int DEFAULT_PORT = 80;
  static Category cat = Category.getInstance("PluggableHTTPServer");
  private int port;
  private Vector handler;
  private ServerSocket server;

  /**
   * Creates a new server object on the given TCP port.
   * If the port is occupied by another process a IOException (java.net.BindException) is thrown.
   *
   * @param port - TCP port number to listen on for requests
   */
  public PluggableHTTPServer(int port) throws IOException {
    this.port = port;
    this.handler = new Vector();
    cat.setPriority(Priority.ERROR);
    server = new ServerSocket(this.port);
  }

  /**
   * Creates a new server object on the default TCP port 80
   * If the port is occupied by another process a IOException (java.net.BindException) is thrown.
   */
  public PluggableHTTPServer() throws IOException {
    this(DEFAULT_PORT);
  }

  /**
   * Registers the given HTTPRequestHandler
   *
   * @param h - the HTTPRequestHandler to register
   */
  public void addRequestHandler(HTTPRequestHandler h) {
    handler.add(h);
  }

  /**
   * Unregisters the given HTTPRequestHandler
   *
   * @param h - the HTTPRequestHandler to unregister
   */
  public void removeRequestHandler(HTTPRequestHandler h) {
    handler.remove(h);
  }

  /**
   * Sends the HTTP message 404 - File Not Found
   * see RFC2616 for details
   *
   * @param out - Out stream for sending data to client browser
   */
  public static void replyNotFound(Writer out) {
    try {
      out.write("HTTP/1.0 404 Not Found\r\n");
      out.write("<HTML><HEAD><TITLE>Not Found</TITLE></HEAD>\r\n");
      out.write("<BODY><H1>Not Found</H1>\r\n");
      out.write("</BODY></HTML>\r\n");
      out.flush();
    }  // end try
    catch (IOException e) {
    }
  }

  /**
   * Sends the HTTP message 405 - Method Not Allowed
   * see RFC2616 for details
   *
   * @param out - Out stream for sending data to client browser
   */
  public static void replyMethodNotAllowed(Writer out) {
    try {
      out.write("HTTP/1.1 405 Method Not Allowed\r\n");
      out.write("Allow: GET, PUT\r\n");
      out.write("<HTML><HEAD><TITLE>Method Not Allowed</TITLE></HEAD>\r\n");
      out.write("<BODY><H1>Method Not Allowed</H1>\r\n");
      out.write("</BODY></HTML>\r\n");
      out.flush();
    }  // end try
    catch (IOException e) {
    }
  }

  /**
   * Creates the ReplyHTML data for the root page
   *
   * @param index - index of the RootRequestHandler
   */
  public void autoCreateRootPage(int index) {
    if (handler.get(index) instanceof RootRequestHandler) {
      RootRequestHandler r = (RootRequestHandler)handler.get(index);
      String html = "<HTML><HEAD><TITLE>"+r.getTitle()+"</TITLE></HEAD>\r\n";
      html = html + "<BODY><H1>"+r.getDescription()+"</H1>\r\n";
      for (int i = 0; i < handler.size(); i++) {
        html = html + "<a href=\"" + ((HTTPRequestHandler)handler.get(i)).getHandledPath();
        html = html + "\">" + ((HTTPRequestHandler)handler.get(i)).getDescription() + "</a><br>";
      }
      html = html + "</BODY></HTML>\r\n";
      r.setReplyHTML(html);
    }
  }

  /**
   * Main loop of the PluggableHTTPServer
   */
  public void run() {
    while (true) {
      try {
        Socket s = server.accept();
        Thread t = new ServerThread(s);
        t.start();
      }
      catch (IOException e) {
      }
    }
  }

  /**
   * This class handles the incomming connection for one request.
   */
  class ServerThread extends Thread {

    private Socket connection;

    ServerThread(Socket s) {
      this.connection = s;
    }

    /**
     * Serves the HTTP request.
     */
    public void run() {
      try {
        Writer out = new BufferedWriter(
                      new OutputStreamWriter(
                       connection.getOutputStream(), "ASCII"
                      )
                     );
        Reader in = new InputStreamReader(
                     new BufferedInputStream(
                      connection.getInputStream()
                     )
                    );

        // read the first line only; that's all we need
        StringBuffer req = new StringBuffer(80);
        while (true) {
          int c = in.read();
          if (c == '\r' || c == '\n' || c == -1) break;
          req.append((char) c);
        }
        String get = req.toString();
        cat.debug(get);
        StringTokenizer st = new StringTokenizer(get);
        String method = st.nextToken();
        String request = st.nextToken();
        String version = st.nextToken();

        if (method.equalsIgnoreCase("GET")) {
          boolean served = false;
          for (int i = 0; i < handler.size(); i++) {
            if (handler.get(i) instanceof HTTPRequestHandler) {
              if (((HTTPRequestHandler)handler.get(i)).handleRequest(request, out)) {
                served = true;
                break;
              }
            }
          }
          if (!served)
            PluggableHTTPServer.replyNotFound(out);
        }
        else {
          PluggableHTTPServer.replyMethodNotAllowed(out);
        }
      } // end try
      catch (IOException e) {
      }
      finally {
        try {
          if (connection != null) connection.close();
        }
        catch (IOException e) {}
      }
    }  // end run
  }  // end class ServerThread

  /**
   * Demo how to use the PluggableHTTPServer.
   */
  public static void main(String[] args) {

    int thePort;

    // create some logging stuff
    BasicConfigurator.configure();
    Category cat1 = Category.getInstance("cat1");
    cat1.addAppender(new org.apache.log4j.ConsoleAppender(new PatternLayout("%m%n")));
    Category cat2 = Category.getInstance("cat2");
    cat2.setPriority(Priority.INFO);
    cat2.addAppender(new org.apache.log4j.ConsoleAppender(new PatternLayout("%c - %m%n")));

    // set TCP port number
    try {
      thePort = Integer.parseInt(args[1]);
    }
    catch (Exception e) {
      thePort = PluggableHTTPServer.DEFAULT_PORT;
    }

    PluggableHTTPServer server = null;
    while (server == null) {
      try {
        server = new PluggableHTTPServer(thePort);
        server.addRequestHandler(new RootRequestHandler());
        server.addRequestHandler(new Log4jRequestHandler());
        server.addRequestHandler(new UserDialogRequestHandler());
        server.autoCreateRootPage(0);
        Thread t = new Thread(server);
        t.start();
      } catch (IOException e) {
        server = null;
        thePort++;
      }
    }

  }  // end main
}
