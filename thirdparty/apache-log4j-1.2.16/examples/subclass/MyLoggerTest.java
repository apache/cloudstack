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

package examples.subclass;

import org.apache.log4j.*;
import org.apache.log4j.xml.DOMConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.helpers.LogLog;

/**
   A simple example showing logger subclassing. 

   <p>The example should make it clear that subclasses follow the
   hierarchy. You should also try running this example with a <a
   href="doc-files/mycat.bad">bad</a> and <a
   href="doc-files/mycat.good">good</a> configuration file samples.

   <p>See <b><a
   href="doc-files/MyLogger.java">source code</a></b> for more details.
*/
public class MyLoggerTest {

  /**
     When called wihtout arguments, this program will just print 
     <pre>
       DEBUG [main] some.cat - Hello world.
     </pre>
     and exit.
     
     <b>However, it can be called with a configuration file in XML or
     properties format.

   */
  static public void main(String[] args) {
    
    if(args.length == 0) {
      // Note that the appender is added to root but that the log
      // request is made to an instance of MyLogger. The output still
      // goes to System.out.
      Logger root = Logger.getRootLogger();
      Layout layout = new PatternLayout("%p [%t] %c (%F:%L) - %m%n");
      root.addAppender(new ConsoleAppender(layout, ConsoleAppender.SYSTEM_OUT));
    }
    else if(args.length == 1) {
      if(args[0].endsWith("xml")) {
	DOMConfigurator.configure(args[0]);
      } else {
	PropertyConfigurator.configure(args[0]);
      }
    } else {
      usage("Incorrect number of parameters.");
    }
    try {
      MyLogger c = (MyLogger) MyLogger.getLogger("some.cat");    
      c.trace("Hello");
      c.debug("Hello");
    } catch(ClassCastException e) {
      LogLog.error("Did you forget to set the factory in the config file?", e);
    }
  }

  static
  void usage(String errMsg) {
    System.err.println(errMsg);
    System.err.println("\nUsage: "+MyLogger.class.getName() + "[configFile]\n"
                + " where *configFile* is an optional configuration file, "+
		       "either in properties or XML format.");
    System.exit(1);
  }
}
