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

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;

import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

/**
   A simple application to send roll over messages to a potentially
   remote {@link ExternallyRolledFileAppender}. 

   <p>It takes two arguments, the <code>host_name</code> and
   <code>port_number</code> where the
   <code>ExternallyRolledFileAppender</code> is listening.
   

   @author Ceki G&uuml;lc&uuml;
   @since version 0.9.0 */
public class Roller {

  static Logger cat = Logger.getLogger(Roller.class);
  

  static String host;
  static int port;

  // Static class.
  Roller() {
  }

  /**
     Send a "RollOver" message to
     <code>ExternallyRolledFileAppender</code> on <code>host</code>
     and <code>port</code>.

   */
  public 
  static 
  void main(String argv[]) {

    BasicConfigurator.configure();

    if(argv.length == 2) 
      init(argv[0], argv[1]);
    else 
      usage("Wrong number of arguments.");
    
    roll();
  }

  static
  void usage(String msg) {
    System.err.println(msg);
    System.err.println( "Usage: java " + Roller.class.getName() +
			"host_name port_number");
    System.exit(1);
  }

  static 
  void init(String hostArg, String portArg) {
    host = hostArg;
    try {
      port =  Integer.parseInt(portArg);
    }
    catch(java.lang.NumberFormatException e) {
      usage("Second argument "+portArg+" is not a valid integer.");
    }
  }

  static
  void roll() {
    try {
      Socket socket = new Socket(host, port);
      DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
      DataInputStream dis = new DataInputStream(socket.getInputStream());
      dos.writeUTF(ExternallyRolledFileAppender.ROLL_OVER);
      String rc = dis.readUTF();
      if(ExternallyRolledFileAppender.OK.equals(rc)) {
	cat.info("Roll over signal acknowledged by remote appender.");
      } else {
	cat.warn("Unexpected return code "+rc+" from remote entity.");
	System.exit(2);
      }
    } catch(IOException e) {
      cat.error("Could not send roll signal on host "+host+" port "+port+" .",
		e);
      System.exit(2);
    }
    System.exit(0);
  }
}
