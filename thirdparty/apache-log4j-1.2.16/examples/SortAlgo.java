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
import org.apache.log4j.NDC;

/**
   Example code for log4j to viewed in conjunction with the {@link
   examples.Sort Sort} class.
      
   <p>SortAlgo uses the bubble sort algorithm to sort an integer
   array. See also its <b><a href="doc-files/SortAlgo.java">source
   code</a></b>.

   @author Ceki G&uuml;lc&uuml; */
public class SortAlgo {

  final static String className = SortAlgo.class.getName();
  final static Logger LOG = Logger.getLogger(className);
  final static Logger OUTER = Logger.getLogger(className + ".OUTER");
  final static Logger INNER = Logger.getLogger(className + ".INNER");
  final static Logger DUMP = Logger.getLogger(className + ".DUMP");
  final static Logger SWAP = Logger.getLogger(className + ".SWAP");

  int[] intArray;

  SortAlgo(int[] intArray) {
    this.intArray = intArray;
  }
    
  void bubbleSort() {
    LOG.info( "Entered the sort method.");

    for(int i = intArray.length -1; i >= 0  ; i--) {
      NDC.push("i=" + i);
      OUTER.debug("in outer loop.");
      for(int j = 0; j < i; j++) {
	NDC.push("j=" + j);
	// It is poor practice to ship code with log staments in tight loops.
	// We do it anyway in this example.
	INNER.debug( "in inner loop.");
         if(intArray[j] > intArray[j+1])
	   swap(j, j+1);
	NDC.pop();
      }
      NDC.pop();
    }
  }  

  void dump() {    
    if(! (this.intArray instanceof int[])) {
      DUMP.error("Tried to dump an uninitialized array.");
      return;
    }
    DUMP.info("Dump of integer array:");
    for(int i = 0; i < this.intArray.length; i++) {
      DUMP.info("Element [" + i + "]=" + this.intArray[i]);
    }    
  }

  void swap(int l, int r) {
    // It is poor practice to ship code with log staments in tight
    // loops or code called potentially millions of times.
    SWAP.debug( "Swapping intArray["+l+"]=" + intArray[l] +
	                     " and intArray["+r+"]=" + intArray[r]);
    int temp = this.intArray[l];
    this.intArray[l] = this.intArray[r];
    this.intArray[r] = temp;
  }
}

