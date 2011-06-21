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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.ObjectInputStream;
import java.net.Socket;

import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.LoggingEvent;

// Contributors:  Moses Hohman <mmhohman@rainbow.uchicago.edu>

/**
   Read {@link LoggingEvent} objects sent from a remote client using
   Sockets (TCP). These logging events are logged according to local
   policy, as if they were generated locally.

   <p>For example, the socket node might decide to log events to a
   local file and also resent them to a second socket node.

    @author  Ceki G&uuml;lc&uuml;

    @since 0.8.4
*/
public class SocketNode implements Runnable {

  Socket socket;
  LoggerRepository hierarchy;
  ObjectInputStream ois;

  static Logger logger = Logger.getLogger(SocketNode.class);

  public SocketNode(Socket socket, LoggerRepository hierarchy) {
    this.socket = socket;
    this.hierarchy = hierarchy;
    try {
      ois = new ObjectInputStream(
                         new BufferedInputStream(socket.getInputStream()));
    } catch(InterruptedIOException e) {
      Thread.currentThread().interrupt();
      logger.error("Could not open ObjectInputStream to "+socket, e);
    } catch(IOException e) {
      logger.error("Could not open ObjectInputStream to "+socket, e);
    } catch(RuntimeException e) {
      logger.error("Could not open ObjectInputStream to "+socket, e);
    }
  }

  //public
  //void finalize() {
  //System.err.println("-------------------------Finalize called");
  // System.err.flush();
  //}

  public void run() {
    LoggingEvent event;
    Logger remoteLogger;

    try {
      if (ois != null) {
          while(true) {
	        // read an event from the wire
	        event = (LoggingEvent) ois.readObject();
	        // get a logger from the hierarchy. The name of the logger is taken to be the name contained in the event.
	        remoteLogger = hierarchy.getLogger(event.getLoggerName());
	        //event.logger = remoteLogger;
	        // apply the logger-level filter
	        if(event.getLevel().isGreaterOrEqual(remoteLogger.getEffectiveLevel())) {
	        // finally log the event as if was generated locally
	        remoteLogger.callAppenders(event);
	      }
        }
      }
    } catch(java.io.EOFException e) {
      logger.info("Caught java.io.EOFException closing conneciton.");
    } catch(java.net.SocketException e) {
      logger.info("Caught java.net.SocketException closing conneciton.");
    } catch(InterruptedIOException e) {
      Thread.currentThread().interrupt();
      logger.info("Caught java.io.InterruptedIOException: "+e);
      logger.info("Closing connection.");
    } catch(IOException e) {
      logger.info("Caught java.io.IOException: "+e);
      logger.info("Closing connection.");
    } catch(Exception e) {
      logger.error("Unexpected exception. Closing conneciton.", e);
    } finally {
      if (ois != null) {
         try {
            ois.close();
         } catch(Exception e) {
            logger.info("Could not close connection.", e);
         }
      }
      if (socket != null) {
        try {
          socket.close();
        } catch(InterruptedIOException e) {
            Thread.currentThread().interrupt();
        } catch(IOException ex) {
        }
      }
    }
  }
}
