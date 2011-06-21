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
import examples.customLevel.XLevel;

/**
   A simple example showing logger subclassing. 

   <p>See <b><a href="doc-files/MyLogger.java">source code</a></b>
   for more details.

   <p>See {@link MyLoggerTest} for a usage example.
   
 */
public class MyLogger extends Logger {

  // It's usually a good idea to add a dot suffix to the fully
  // qualified class name. This makes caller localization to work
  // properly even from classes that have almost the same fully
  // qualified class name as MyLogger, e.g. MyLoggerTest.
  static String FQCN = MyLogger.class.getName() + ".";

  // It's enough to instantiate a factory once and for all.
  private static MyLoggerFactory myFactory = new MyLoggerFactory();

  /**
     Just calls the parent constuctor.
   */
  public MyLogger(String name) {
    super(name);
  }

  /**
     Overrides the standard debug method by appending " world" at the
     end of each message.  */
  public 
  void debug(Object message) {
    super.log(FQCN, Level.DEBUG, message + " world.", null);    
  }

  /**
     This method overrides {@link Logger#getLogger} by supplying
     its own factory type as a parameter.
  */
  public 
  static
  Logger getLogger(String name) {
    return Logger.getLogger(name, myFactory); 
  }

  public
  void trace(Object message) {
    super.log(FQCN, XLevel.TRACE, message, null); 
  }
}


