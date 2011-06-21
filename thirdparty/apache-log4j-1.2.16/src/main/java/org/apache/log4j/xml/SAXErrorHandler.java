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

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;
import org.apache.log4j.helpers.LogLog;

public class SAXErrorHandler implements ErrorHandler {

  public
  void error(final SAXParseException ex) {
    emitMessage("Continuable parsing error ", ex);
  }
  
  public
  void fatalError(final SAXParseException ex) {
    emitMessage("Fatal parsing error ", ex);
  }
   
  public
  void warning(final SAXParseException ex) {
    emitMessage("Parsing warning ", ex);
  }
  
  private static void emitMessage(final String msg, final SAXParseException ex) {
    LogLog.warn(msg +ex.getLineNumber()+" and column "
		 +ex.getColumnNumber());
    LogLog.warn(ex.getMessage(), ex.getException());
  }
}
