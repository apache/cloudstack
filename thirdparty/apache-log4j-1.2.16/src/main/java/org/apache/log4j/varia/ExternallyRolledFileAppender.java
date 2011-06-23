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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.helpers.LogLog;

/**
   This appender listens on a socket on the port specified by the
   <b>Port</b> property for a "RollOver" message. When such a message
   is received, the underlying log file is rolled over and an
   acknowledgment message is sent back to the process initiating the
   roll over.

   <p>This method of triggering roll over has the advantage of being
   operating system independent, fast and reliable.

   <p>A simple application {@link Roller} is provided to initiate the
   roll over.

   <p>Note that the initiator is not authenticated. Anyone can trigger
   a rollover. In production environments, it is recommended that you
   add some form of protection to prevent undesired rollovers.


   @author Ceki G&uuml;lc&uuml;
   @since version 0.9.0 */
public class ExternallyRolledFileAppender extends RollingFileAppender {

  /**
     The string constant sent to initiate a roll over.   Current value of
     this string constant is <b>RollOver</b>.
  */
  static final public String ROLL_OVER = "RollOver";

  /**
     The string constant sent to acknowledge a roll over.   Current value of
      this string constant is <b>OK</b>.
  */
  static final public String OK = "OK";

  int port = 0;
  HUP hup;

  /**
     The default constructor does nothing but calls its super-class
     constructor.  */
  public
  ExternallyRolledFileAppender() {
  }

  /**
     The <b>Port</b> [roperty is used for setting the port for
     listening to external roll over messages.
  */
  public
  void setPort(int port) {
    this.port = port;
  }

  /**
     Returns value of the <b>Port</b> option.
   */
  public
  int getPort() {
    return port;
  }

  /**
     Start listening on the port specified by a preceding call to
     {@link #setPort}.  */
  public
  void activateOptions() {
    super.activateOptions();
    if(port != 0) {
      if(hup != null) {
	hup.interrupt();
      }
      hup = new HUP(this, port);
      hup.setDaemon(true);
      hup.start();
    }
  }
}


class HUP extends Thread {

  int port;
  ExternallyRolledFileAppender er;

  HUP(ExternallyRolledFileAppender er, int port) {
    this.er = er;
    this.port = port;
  }

  public
  void run() {
    while(!isInterrupted()) {
      try {
	ServerSocket serverSocket = new ServerSocket(port);
	while(true) {
	  Socket socket = serverSocket.accept();
	  LogLog.debug("Connected to client at " + socket.getInetAddress());
	  new Thread(new HUPNode(socket, er), "ExternallyRolledFileAppender-HUP").start();
	}
      } catch(InterruptedIOException e) {
        Thread.currentThread().interrupt();
	    e.printStackTrace();
      } catch(IOException e) {
	    e.printStackTrace();
      } catch(RuntimeException e) {
	    e.printStackTrace();
      }
    }
  }
}

class HUPNode implements Runnable {

  Socket socket;
  DataInputStream dis;
  DataOutputStream dos;
  ExternallyRolledFileAppender er;

  public
  HUPNode(Socket socket, ExternallyRolledFileAppender er) {
    this.socket = socket;
    this.er = er;
    try {
      dis = new DataInputStream(socket.getInputStream());
      dos = new DataOutputStream(socket.getOutputStream());
    } catch(InterruptedIOException e) {
      Thread.currentThread().interrupt();
      e.printStackTrace();
    } catch(IOException e) {
      e.printStackTrace();
    } catch(RuntimeException e) {
      e.printStackTrace();
    }
  }

  public void run() {
    try {
      String line = dis.readUTF();
      LogLog.debug("Got external roll over signal.");
      if(ExternallyRolledFileAppender.ROLL_OVER.equals(line)) {
	synchronized(er) {
	  er.rollOver();
	}
	dos.writeUTF(ExternallyRolledFileAppender.OK);
      }
      else {
	dos.writeUTF("Expecting [RollOver] string.");
      }
      dos.close();
    } catch(InterruptedIOException e) {
      Thread.currentThread().interrupt();
      LogLog.error("Unexpected exception. Exiting HUPNode.", e);
    } catch(IOException e) {
      LogLog.error("Unexpected exception. Exiting HUPNode.", e);
    } catch(RuntimeException e) {
      LogLog.error("Unexpected exception. Exiting HUPNode.", e);
    }
  }
}

