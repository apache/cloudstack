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

package examples;

import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.rmi.Naming;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;
import org.apache.log4j.PropertyConfigurator;

/**
   A simple {@link NumberCruncher} implementation that logs its
   progress when factoring numbers. The purpose of the whole exercise
   is to show the use of nested diagnostic contexts in order to
   distinguish the log output from different client requests.

   <pre>
   <b>Usage:</b> java org.apache.log4j.examples.NumberCruncherServer <em>configFile</em>
    &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;where <em>configFile</em> is a log4j configuration file.
   </pre>

   We supply a simple config file <a href=doc-files/factor.lcf>factor.lcf</a>
   for directing log output to the file <code>factor.log</code>.

   <p>Try it yourself by starting a <code>NumberCruncherServer</code>
   and make queries from multiple {@link NumberCruncherClient
   NumberCruncherClients} to factor numbers.

   
   <p><b><a href="doc-files/factor.html">Sample output</a></b> shows the log
   output when two clients connect to the server near simultaneously.
      
   <p>See <a href=doc-files/NumberCruncherServer.java>source</a> code
   of <code>NumberCruncherServer</code> for more details.

   <p>Note that class files for the example code is not included in
   any of the distributed log4j jar files. You will have to add the
   directory <code>/dir-where-you-unpacked-log4j/classes</code> to
   your classpath before trying out the examples.

   
 */
public class NumberCruncherServer extends UnicastRemoteObject
                                  implements  NumberCruncher {
  private static final long serialVersionUID = 2626753561969426769L;


  static Logger logger = Logger.getLogger(NumberCruncherServer.class);

  public
  NumberCruncherServer() throws RemoteException {
  }
  
  public
  int[] factor(int number) throws RemoteException {

    // The client's host is an important source of information.
    try {
      NDC.push(getClientHost());
    }
    catch(java.rmi.server.ServerNotActiveException e) {
      // we are being called from same VM
      NDC.push("localhost");
    }

    // The information contained within the request is another source of
    // distinctive information. It might reveal the users name, date of request,
    // request ID etc. In servlet type environments, much information is
    // contained in cookies.
    NDC.push(String.valueOf(number));    

    logger.info("Beginning to factor.");
    if(number <= 0) {
      throw new IllegalArgumentException(number+" is not a positive integer.");
    }
    else if(number == 1)
       return new int[] {1};
    
    Vector factors = new Vector();
    int n = number;

    for(int i = 2; (i <= n) && (i*i <= number); i++) {
      // It is bad practice to place log requests within tight loops.
      // It is done here to show interleaved log output from
      // different requests. 
      logger.debug("Trying to see if " + i + " is a factor.");

      if((n % i) == 0) {
	logger.info("Found factor "+i);
	factors.addElement(new Integer(i));
	do {
	  n /= i;
	} while((n % i) == 0);
      }
      // Placing artificial delays in tight-loops will also lead to sub-optimal
      // resuts. :-)
      delay(100);
    }

    if(n != 1) {
      logger.info("Found factor "+n);
      factors.addElement(new Integer(n));
    }
    
    int len = factors.size();
    
    int[] result = new int[len];
    for(int i = 0; i < len; i++) {
      result[i] = ((Integer) factors.elementAt(i)).intValue();
    }

    // Before leaving a thread we call NDC.remove. This deletes the reference
    // to the thread in the internal hash table. Version 0.8.5 introduces a
    // a lazy removal mechanism in case you forget to call remove when
    // exiting a thread. See the java documentation in NDC.remove for further
    // details.
    NDC.remove();
    
    return result;
  }

  static
  void usage(String msg) {
    System.err.println(msg);
    System.err.println(
     "Usage: java org.apache.log4j.examples.NumberCruncherServer configFile\n" +
     "   where configFile is a log4j configuration file.");
    System.exit(1);
  }

  public static
  void delay(int millis) {
    try{Thread.sleep(millis);}
    catch(InterruptedException e) {}
  }
  
  public static void main(String[] args) {
    if(args.length != 1) 
      usage("Wrong number of arguments.");
    
    NumberCruncherServer ncs;
    PropertyConfigurator.configure(args[0]);
    try {
      ncs = new NumberCruncherServer();
      Naming.rebind("Factor", ncs);
      logger.info("NumberCruncherServer bound and ready to serve.");
    }
    catch(Exception e) {
      logger.error("Could not bind NumberCruncherServer.", e);
      return;
    }
    NumberCruncherClient.loop(ncs);          
  }
}
