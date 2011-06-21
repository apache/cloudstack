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

/**
 * This interface defines all methods that have to be implemented for a HTTPRequestHandler for the
 * PluggableHTTPServer.
 *
 * @author <a HREF="mailto:V.Mentzner@psi-bt.de">Volker Mentzner</a>
 */
public interface HTTPRequestHandler {

 /**
   * Gets the title for html page
   */
  public String getTitle();

 /**
   * Sets the title for html page
   */
  public void setTitle(String title);

 /**
   * Gets the description for html page
   */
  public String getDescription();

 /**
   * Sets the description for html page
   */
  public void setDescription(String description);

 /**
   * Gets the virtual path in the HTTP server that ist handled in this HTTPRequestHandler.
   * So the root path handler will return "/" (without brackets) because it handles the path
   * "http://servername/" or a handler for "http://servername/somepath/" will return "/somepath/"
   * It is important to include the trailing "/" because all HTTPRequestHandler have to serve a path!
   */
  public String getHandledPath();

 /**
   * Sets the virtual path in the HTTP server that ist handled in this HTTPRequestHandler.
   * So set the path to "/" for the root path handler because it handles the path
   * "http://servername/" or set it to "/somepath/" for a handler for "http://servername/somepath/".
   * It is important to include the trailing "/" because all HTTPRequestHandler have to serve a path!
   */
  public void setHandledPath(String path);

 /**
   * Handles the given request and writes the reply to the given out-stream. Every handler has to check
   * the request for the right path info.
   *
   * @param request - client browser request
   * @param out - Out stream for sending data to client browser
   * @return if the request was handled by this handler : true, else : false
   */
  public boolean handleRequest(String request, Writer out);
}