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
 
package org.apache.log4j.varia;

import  org.apache.log4j.spi.ErrorHandler;
import  org.apache.log4j.spi.LoggingEvent;
import  org.apache.log4j.Appender;
import  org.apache.log4j.Logger;
import  org.apache.log4j.helpers.LogLog;
import java.util.Vector;
import java.io.InterruptedIOException;

/**
  *
  * The <code>FallbackErrorHandler</code> implements the ErrorHandler
  * interface such that a secondary appender may be specified.  This
  * secondary appender takes over if the primary appender fails for
  * whatever reason.
  *
  * <p>The error message is printed on <code>System.err</code>, and
  * logged in the new secondary appender.
  *
  * @author Ceki G&uuml;c&uuml;
  * */
public class FallbackErrorHandler implements ErrorHandler {


  Appender backup;
  Appender primary;
  Vector loggers;

  public FallbackErrorHandler() {
  }
  

  /**
     <em>Adds</em> the logger passed as parameter to the list of
     loggers that we need to search for in case of appender failure.
  */
  public 
  void setLogger(Logger logger) {
    LogLog.debug("FB: Adding logger [" + logger.getName() + "].");
    if(loggers == null) {
      loggers = new Vector();
    }
    loggers.addElement(logger);
  }


  /**
     No options to activate.
  */
  public 
  void activateOptions() {
  }


  /**
     Prints the message and the stack trace of the exception on
     <code>System.err</code>.  */
  public
  void error(String message, Exception e, int errorCode) { 
    error(message, e, errorCode, null);
  }

  /**
     Prints the message and the stack trace of the exception on
     <code>System.err</code>.
   */
  public
  void error(String message, Exception e, int errorCode, LoggingEvent event) {
    if (e instanceof InterruptedIOException) {
        Thread.currentThread().interrupt();
    }
    LogLog.debug("FB: The following error reported: " + message, e);
    LogLog.debug("FB: INITIATING FALLBACK PROCEDURE.");
    if (loggers != null) {
    	for(int i = 0; i < loggers.size(); i++) {
      		Logger l = (Logger) loggers.elementAt(i);
      		LogLog.debug("FB: Searching for ["+primary.getName()+"] in logger ["
		   		+l.getName() + "].");
      		LogLog.debug("FB: Replacing ["+primary.getName()+"] by ["
		   		+ backup.getName() + "] in logger ["+ l.getName() +"].");
      		l.removeAppender(primary);
      		LogLog.debug("FB: Adding appender ["+backup.getName()+"] to logger "
		   		+  l.getName());
      		l.addAppender(backup);
        }
    }    
  }


  /**
     Print a the error message passed as parameter on
     <code>System.err</code>.  
  */
  public 
  void error(String message) {
    //if(firstTime) {
    //LogLog.error(message);
    //firstTime = false;
    //}
  }
  
  /**
     The appender to which this error handler is attached.
   */
  public
  void setAppender(Appender primary) {
    LogLog.debug("FB: Setting primary appender to [" + primary.getName() + "].");
    this.primary = primary;
  }

  /**
     Set the backup appender.
   */
  public
  void setBackupAppender(Appender backup) {
    LogLog.debug("FB: Setting backup appender to [" + backup.getName() + "].");
    this.backup = backup;
  }
  
}
