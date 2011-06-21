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

import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Logger;

/**
   Example code for log4j to viewed in conjunction with the {@link
   examples.SortAlgo SortAlgo} class.
   
   <p>This program expects a configuration file name as its first
   argument, and the size of the array to sort as the second and last
   argument. See its <b><a href="doc-files/Sort.java">source
   code</a></b> for more details.

   <p>Play around with different values in the configuration file and
   watch the changing behavior.

   <p>Example configuration files can be found in <a
   href="doc-files/sort1.properties">sort1.properties</a>, <a
   href="doc-files/sort2.properties">sort2.properties</a>, <a
   href="doc-files/sort3.properties">sort3.properties</a> and <a
   href="doc-files/sort4.properties">sort4.properties</a> are supplied with the
   package.
   
   <p>If you are also interested in logging performance, then have
   look at the {@link org.apache.log4j.performance.Logging} class.

   @author Ceki G&uuml;lc&uuml; */

public class Sort {

  static Logger logger = Logger.getLogger(Sort.class.getName());
  
  public static void main(String[] args) {
    if(args.length != 2) {
      usage("Incorrect number of parameters.");
    }
    int arraySize = -1;
    try {
      arraySize = Integer.valueOf(args[1]).intValue();
      if(arraySize <= 0) 
	usage("Negative array size.");
    }
    catch(java.lang.NumberFormatException e) {
      usage("Could not number format ["+args[1]+"].");
    }

    PropertyConfigurator.configure(args[0]);

    int[] intArray = new int[arraySize];

    logger.info("Populating an array of " + arraySize + " elements in" +
	     " reverse order.");
    for(int i = arraySize -1 ; i >= 0; i--) {
      intArray[i] = arraySize - i - 1;
    }

    SortAlgo sa1 = new SortAlgo(intArray);
    sa1.bubbleSort();
    sa1.dump();

    // We intentionally initilize sa2 with null.
    SortAlgo sa2 = new SortAlgo(null);
    logger.info("The next log statement should be an error message.");
    sa2.dump();  
    logger.info("Exiting main method.");    
  }
  
  static
  void usage(String errMsg) {
    System.err.println(errMsg);
    System.err.println("\nUsage: java org.apache.examples.Sort " +
		       "configFile ARRAY_SIZE\n"+
      "where  configFile is a configuration file\n"+
      "      ARRAY_SIZE is a positive integer.\n");
    System.exit(1);
  }
}
