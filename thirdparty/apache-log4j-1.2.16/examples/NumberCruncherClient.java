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

import java.rmi.RemoteException;
import java.rmi.Naming;
import java.io.*;

/**
   NumberCruncherClient is a simple client for factoring integers. A
   remote NumberCruncher is contacted and asked to factor an
   integer. The factors returned by the {@link NumberCruncherServer}
   are displayed on the screen.

   <p>See <a href=doc-files/NumberCruncherClient.java>source</a> code of 
   <code>NumberCruncherClient</code> for more details.

   <pre>
   <b>Usage:</b> java  org.apache.log4j.examples.NumberCruncherClient HOST
    &nbsp;&nbsp;&nbsp;&nbsp;where HOST is the machine where the NumberCruncherServer is running
   </pre>
   
   <p>Note that class files for the example code is not included in
   any of the distributed log4j jar files. You will have to add the
   directory <code>/dir-where-you-unpacked-log4j/classes</code> to
   your classpath before trying out the examples.

   @author Ceki G&uuml;lc&uuml;
   
 */
public class NumberCruncherClient {


  public static void main(String[] args) {
    if(args.length == 1) {
      try {
        String url = "rmi://"+args[0]+ "/Factor";
      	NumberCruncher nc = (NumberCruncher) Naming.lookup(url);
	loop(nc);
      }
      catch(Exception e) {
	e.printStackTrace();
      }      
    }
    else
      usage("Wrong number of arguments.");
  }

  static
  void usage(String msg) {
    System.err.println(msg);
    System.err.println(
     "Usage: java org.apache.log4j.examples.NumberCruncherClient HOST\n" +
     "   where HOST is the machine where the NumberCruncherServer is running.");
    System.exit(1);
  }


  static
  void loop(NumberCruncher nc) {
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    int i = 0;
    while (true) {
      System.out.print("Enter a number to factor, '-1' to quit: ");      
      try {
	i = Integer.parseInt(in.readLine());
      }
      catch(Exception e) {
	e.printStackTrace();
      }
      if(i == -1) {
	System.out.print("Exiting loop.");
	return;
      }
      else {
	try {
	  System.out.println("Will attempt to factor "+i);
	  int[] factors = nc.factor(i);
	  System.out.print("The factors of "+i+" are");
	  for(int k=0; k < factors.length; k++) {
	    System.out.print(" " + factors[k]);
	  }
	  System.out.println(".");
	}
	catch(RemoteException e) {
	  System.err.println("Could not factor "+i);
	  e.printStackTrace();
	}
      }
    }
  }
}
