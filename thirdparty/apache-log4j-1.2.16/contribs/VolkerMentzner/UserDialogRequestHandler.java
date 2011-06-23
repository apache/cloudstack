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
/**
 * Title:        PSI Java Framework: UserDialogRequestHandler<p>
 * Copyright:    PSI-BT AG<p>
 * History:
 *   Date        Author        What's new
 *   16.04.2001  VMentzner     Created
 */

package com.psibt.framework.net;
/**
 * This class implements a RequestHandler for the path "/userdialog/" in the PluggableHTTPServer.
 * A simple input form is presented in the browser where you can enter a message. This message will be sent
 * to the PluggableHTTPServer and shown in a JOptionPane MessageDialog.
 *
 * @author <a HREF="mailto:V.Mentzner@psi-bt.de">Volker Mentzner</a>
 */
public class UserDialogRequestHandler extends RootRequestHandler {

  private Component parentComponent;

 /**
   * Creates a new UserDialogRequestHandler object
   */
  public UserDialogRequestHandler() {
    this(null);
  }

 /**
   * Creates a new UserDialogRequestHandler object with a parentComponent reference
   */
  public UserDialogRequestHandler(Component parentComponent) {
    this.setTitle("user dialog");
    this.setDescription("show user dialog");
    this.setHandledPath("/userdialog/");
    this.parentComponent = parentComponent;
  }

 /**
   * Handles the given request and writes the reply to the given out-stream.
   *
   * @param request - client browser request
   * @param out - Out stream for sending data to client browser
   * @return if the request was handled by this handler : true, else : false
   */
  public boolean handleRequest(String request, Writer out) {
    String path = "";
    String query = null;
    try {
      URL url = new URL("http://localhost"+request);
      path = url.getPath();
      query = url.getQuery();
      if (path.startsWith(this.getHandledPath()) == false) {
        return false;
      }

      out.write("HTTP/1.0 200 OK\r\n");
      out.write("Content-type: text/html\r\n\r\n");
      out.write("<HTML><HEAD><TITLE>" + this.getTitle() + "</TITLE></HEAD>\r\n");
      out.write("<BODY><H1>" + this.getDescription() + "</H1>\r\n");
      if ((query != null) && (query.length() >= 0)) {
        int idx = query.indexOf("=");
        String message = query.substring(idx+1, query.length());
        // replace '+' by space
        message = message.replace('+', ' ');
        // replace hex strings starting with '%' by their values
        idx = message.indexOf("%");
        while (idx >= 0) {
          String sl = message.substring(0, idx);
          String sm = message.substring(idx+1, idx+3);
          String sr = message.substring(idx+3, message.length());
          try {
            int i = Integer.parseInt(sm, 16);
            sm = String.valueOf((char)i);
          }
          catch (Exception ex) {
            sm = "";
          }
          message = sl + sm + sr;
          idx = message.indexOf("%");
        }
        // show message in a new thread
        if ((message != null) && (message.length() > 0)) {
          Thread t = new Thread(new DialogThread(parentComponent, message));
          t.start();
        }
      }
      out.write("<form name=\"Formular\" ACTION=\""+this.getHandledPath()+"+\" METHOD=\"PUT\">");
      out.write("<table>\r\n");
      out.write(" <tr><td>Send message to user</td></tr>\r\n");
      out.write(" <tr><td><textarea name=\"message\" rows=10 cols=50></textarea></td></tr>\r\n");
      out.write("</table>\r\n");
      out.write("<input type=submit value=\"Submit\">");
      out.write("</form>");
      out.write("</BODY></HTML>\r\n");
      out.flush();
      return true;
    } catch (Exception ex) {
      return false;
    }
  }

 /**
   * Internal class to start the user dialog in a new thread. This makes the RequestHandler return
   * immediatly
   */
  class DialogThread implements Runnable {
    private Component parentComponent;
    private String message;

    public DialogThread(Component parentComponent, String message) {
      this.parentComponent = parentComponent;
      this.message = message;
    }

    public void run() {
      JOptionPane.showMessageDialog(parentComponent, message);
    }
  }
}