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


import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.NDC;

/**
   View the <a href="doc-files/Trivial.java">source code</a> of this a
   trivial usage example. Running <code>java examples.Trivial</code>
   should output something similar to:

   <pre>
      0    INFO  [main] examples.Trivial (Client #45890) - Awake awake. Put on thy strength.
      15   DEBUG [main] examples.Trivial (Client #45890 DB) - Now king David was old.
      278  INFO  [main] examples.Trivial$InnerTrivial (Client #45890) - Entered foo.
      293  INFO  [main] examples.Trivial (Client #45890) - Exiting Trivial.   
   </pre>
   
   <p> The increasing numbers at the beginning of each line are the
   times elapsed since the start of the program. The string between
   the parentheses is the nested diagnostic context.

   <p>See {@link Sort} and {@link SortAlgo} for sligtly more elaborate
   examples.

   <p>Note thent class files for the example code is not included in
   any of the distributed log4j jar files. You will have to add the
   directory <code>/dir-where-you-unpacked-log4j/classes</code> to
   your classpath before trying out the examples.

 */
public class Trivial {

  static Logger logger = Logger.getLogger(Trivial.class);

  public static void main(String[] args) {
    BasicConfigurator.configure();
    NDC.push("Client #45890"); 

    logger.info("Awake awake. Put on thy strength.");
    Trivial.foo();
    InnerTrivial.foo();
    logger.info("Exiting Trivial.");    
  }

  static
  void foo() {
    NDC.push("DB"); 
    logger.debug("Now king David was old.");    
    NDC.pop(); 
  }

  static class InnerTrivial {
    static  Logger logger = Logger.getLogger(InnerTrivial.class);

    static    
    void foo() {
      logger.info("Entered foo."); 
    }
  }
}
