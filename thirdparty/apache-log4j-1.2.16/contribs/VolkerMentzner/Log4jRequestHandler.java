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

import java.io.*;
import java.net.*;
import java.util.*;
import org.apache.log4j.*;

/**
 * This class implements a RequestHandler for log4j configuration. It serves the "/log4j/" path
 * in the PluggableHTTPServer. If this path is requested a list of all current log4j categories
 * with their current priorities is created. All priority settings can be changed by the user
 * and can be submitted and taken over.
 *
 * @author <a HREF="mailto:V.Mentzner@psi-bt.de">Volker Mentzner</a>
 */
public class Log4jRequestHandler extends RootRequestHandler {

  private Priority[] prios = Priority.getAllPossiblePriorities();

 /**
   * Creates a new Log4jRequestHandler object
   */
  public Log4jRequestHandler() {
    this.setTitle("log4j");
    this.setDescription("log4j configuration");
    this.setHandledPath("/log4j/");
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
    String name;
    try {
      // check request url
      URL url = new URL("http://localhost"+request);
      path = url.getPath();
      query = url.getQuery();
      if (path.startsWith(this.getHandledPath()) == false) {
        return false;
      }

      out.write("HTTP/1.0 200 OK\r\n");
      out.write("Content-type: text/html\r\n\r\n");
      out.write("<HTML><HEAD><TITLE>" + this.getTitle() + "</TITLE></HEAD>\r\n");
      out.write("<BODY><H1>log4j</H1>\r\n");
      out.write(this.getDescription() + "<br><br>\r\n");

      // handle a request with query
      if ((query != null) && (query.length() >= 0)) {
        StringTokenizer st = new StringTokenizer(query, "&");
        String cmd;
        String catname;
        String catval;
        int idx;
        while (st.hasMoreTokens()) {
          cmd = st.nextToken();
          idx = cmd.indexOf("=");
          catname = cmd.substring(0, idx);
          catval = cmd.substring(idx+1, cmd.length());
          if (catname.equalsIgnoreCase("root"))
            Category.getRoot().setPriority(Priority.toPriority(catval));
          else
            Category.getInstance(catname).setPriority(Priority.toPriority(catval));
        }
      }

      // output category information in a form with a simple table
      out.write("<form name=\"Formular\" ACTION=\""+this.getHandledPath()+"\" METHOD=\"PUT\">");
      out.write("<table cellpadding=4>\r\n");
      out.write(" <tr>\r\n");
      out.write("  <td><b>Category</b></td>\r\n");
      out.write("  <td><b>Priority</b></td>\r\n");
      out.write("  <td><b>Appender</b></td>\r\n");
      out.write(" </tr>\r\n");

      // output for root category
      Category cat = Category.getRoot();
      out.write(" <tr><td>root</td>\r\n");
      out.write("  <td>\r\n");
      out.write("   <select size=1 name=\""+ cat.getName() +"\">");
      for (int i = 0; i < prios.length; i++) {
        if (cat.getChainedPriority().toString().equals(prios[i].toString()))
          out.write("<option selected>"+prios[i].toString());
        else
          out.write("<option>"+prios[i].toString());
      }
      out.write("</select>\r\n");
      out.write("  </td>\r\n");
      out.write("  <td>\r\n");
      for (Enumeration apds = cat.getAllAppenders(); apds.hasMoreElements();) {
        Appender apd = (Appender)apds.nextElement();
        name = apd.getName();
        if (name == null)
          name = "<i>(no name)</i>";
        out.write(name);
        if (apd instanceof AppenderSkeleton) {
          try {
            AppenderSkeleton apskel = (AppenderSkeleton)apd;
            out.write(" [" + apskel.getThreshold().toString() + "]");
          } catch (Exception ex) {
          }
        }
        if (apds.hasMoreElements())
          out.write(",  ");
      }
      out.write("  </td>\r\n");
      out.write(" </tr>\r\n");

      // output for all other categories
      for (Enumeration en = Category.getCurrentCategories(); en.hasMoreElements();) {
        cat = (Category)en.nextElement();
        out.write(" <tr>\r\n");
        out.write("  <td>" + cat.getName() + "</td>\r\n");
        out.write("  <td>\r\n");
        out.write("   <select size=1 name=\""+ cat.getName() +"\">");
        for (int i = 0; i < prios.length; i++) {
          if (cat.getChainedPriority().toString().equals(prios[i].toString()))
            out.write("<option selected>"+prios[i].toString());
          else
            out.write("<option>"+prios[i].toString());
        }
        out.write("</select>\r\n");
        out.write("  </td>\r\n");
        out.write("  <td>\r\n");
        for (Enumeration apds = cat.getAllAppenders(); apds.hasMoreElements();) {
          Appender apd = (Appender)apds.nextElement();
          name = apd.getName();
          if (name == null)
            name = "<i>(no name)</i>";
          out.write(name);
          if (apd instanceof AppenderSkeleton) {
            try {
              AppenderSkeleton apskel = (AppenderSkeleton)apd;
              out.write(" [" + apskel.getThreshold().toString() + "]");
            } catch (Exception ex) {
            }
          }
          if (apds.hasMoreElements())
            out.write(",  ");
        }
        out.write("  </td>\r\n");
        out.write(" </tr>\r\n");
      }
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
}