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

package org.apache.log4j.xml.examples;

import org.apache.log4j.xml.DOMConfigurator;
import org.apache.log4j.Logger;

/**

   This <a href="XMLSample.java">example code</a> shows how to
   read an XML based configuration file using a DOM parser.

   <p>Sample XML files <a href="sample1.xml">sample1.xml</a>
   and <a href="sample2.xml">sample2.xml</a> are provided.


   <p>Note that the log4j.dtd is not in the local directory.
   It is found by the class loader.

   @author Ceki G&uuml;lc&uuml;

*/
public class XMLSample {

  static Logger cat = Logger.getLogger(XMLSample.class);

  public
  static
  void main(String argv[]) {

    if(argv.length == 1)
      init(argv[0]);
    else
      Usage("Wrong number of arguments.");
    sample();
  }

  static
  void Usage(String msg) {
    System.err.println(msg);
    System.err.println( "Usage: java " + XMLSample.class.getName() +
			"configFile");
    System.exit(1);
  }

  static
  void init(String configFile) {
    DOMConfigurator.configure(configFile);
  }

  static
  void sample() {
    int i = -1;
    cat.debug("Message " + ++i);
    cat.warn ("Message " + ++i);
    cat.error("Message " + ++i);
    Exception e = new Exception("Just testing");
    cat.debug("Message " + ++i, e);
  }
}
