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

package org.apache.log4j;


import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import java.util.Random;

/*
  Stress test the Logger class.

*/

class StressCategory {

  static Level[] level = new Level[] {Level.DEBUG, 
				      Level.INFO, 
				      Level.WARN,
				      Level.ERROR,
				      Level.FATAL};

  static Level defaultLevel = Logger.getRootLogger().getLevel();
  
  static int LENGTH;
  static String[] names;
  static Logger[] cat;
  static CT[] ct;

  static Random random = new Random(10);

  public static void main(String[] args) {
    
    LENGTH = args.length;

    if(LENGTH == 0) {
      System.err.println( "Usage: java " + StressCategory.class.getName() +
			  " name1 ... nameN\n.");      
      System.exit(1);
    }
    if(LENGTH >= 7) {
      System.err.println(
        "This stress test suffers from combinatorial explosion.\n"+
        "Invoking with seven arguments takes about 90 minutes even on fast machines");
    }

    names = new String[LENGTH];
    for(int i=0; i < LENGTH; i++) {
      names[i] = args[i];
    }    
    cat = new Logger[LENGTH];
    ct = new CT[LENGTH]; 


    permute(0); 

    // If did not exit, then passed all tests.
  }

  // Loop through all permutations of names[].
  // On each possible permutation call createLoop
  static
  void permute(int n) {
    if(n == LENGTH)
      createLoop(0);
    else
      for(int i = n; i < LENGTH; i++) {
	swap(names, n, i);
	permute(n+1);
	swap(names, n, i);	
      }
  }

  static
  void swap(String[] names, int i, int j) {
    String t = names[i];
    names[i] = names[j];
    names[j] = t;
  }
  
  public
  static
  void permutationDump() {
    System.out.print("Current permutation is - ");
    for(int i = 0; i < LENGTH; i++) {
      System.out.print(names[i] + " ");
    }
    System.out.println();
  }


  // Loop through all possible 3^n combinations of not instantiating, 
  // instantiating and setting/not setting a level.

  static
  void createLoop(int n) {
    if(n == LENGTH) {  
      //System.out.println("..............Creating cat[]...........");
      for(int i = 0; i < LENGTH; i++) {
	if(ct[i] == null)
	  cat[i] = null;
	else {
	  cat[i] = Logger.getLogger(ct[i].catstr);
	  cat[i].setLevel(ct[i].level);
	}
      }
      test();
      // Clear hash table for next round
      Hierarchy h = (Hierarchy) LogManager.getLoggerRepository();
      h.clear();
    }
    else {      
      ct[n]  = null;
      createLoop(n+1);  

      ct[n]  = new CT(names[n], null);
      createLoop(n+1);  
      
      int r = random.nextInt(); if(r < 0) r = -r;
      ct[n]  = new CT(names[n], level[r%5]);
      createLoop(n+1);
    }
  }


  static
  void test() {    
    //System.out.println("++++++++++++TEST called+++++++++++++");
    //permutationDump();
    //catDump();

    for(int i = 0; i < LENGTH; i++) {
      if(!checkCorrectness(i)) {
	System.out.println("Failed stress test.");
	permutationDump();
	
	//Hierarchy._default.fullDump();
	ctDump();
	catDump();
	System.exit(1);
      }
    }
  }
  
  static
  void ctDump() {
    for(int j = 0; j < LENGTH; j++) {
       if(ct[j] != null) 
	    System.out.println("ct [" + j + "] = ("+ct[j].catstr+"," + 
			       ct[j].level + ")");
       else 
	 System.out.println("ct [" + j + "] = undefined");
    }
  }
  
  static
  void catDump() {
    for(int j = 0; j < LENGTH; j++) {
      if(cat[j] != null)
	System.out.println("cat[" + j + "] = (" + cat[j].name + "," +
			   cat[j].getLevel() + ")");
      else
	System.out.println("cat[" + j + "] = undefined"); 
    }
  }

  //  static
  //void provisionNodesDump() {
  //for (Enumeration e = CategoryFactory.ht.keys(); e.hasMoreElements() ;) {
  //  CategoryKey key = (CategoryKey) e.nextElement();
  //  Object c = CategoryFactory.ht.get(key);
  //  if(c instanceof  ProvisionNode) 
  //((ProvisionNode) c).dump(key.name);
  //}
  //}
  
  static
  boolean checkCorrectness(int i) {
    CT localCT = ct[i];

    // Can't perform test if logger is not instantiated
    if(localCT == null) 
      return true;
    
    // find expected level
    Level expected = getExpectedPrioriy(localCT);

			    
    Level purported = cat[i].getEffectiveLevel();

    if(expected != purported) {
      System.out.println("Expected level for " + localCT.catstr + " is " +
		       expected);
      System.out.println("Purported level for "+ cat[i].name + " is "+purported);
      return false;
    }
    return true;
      
  }

  static
  Level getExpectedPrioriy(CT ctParam) {
    Level level = ctParam.level;
    if(level != null) 
      return level;

    
    String catstr = ctParam.catstr;    
    
    for(int i = catstr.lastIndexOf('.', catstr.length()-1); i >= 0; 
	                              i = catstr.lastIndexOf('.', i-1))  {
      String substr = catstr.substring(0, i);

      // find the level of ct corresponding to substr
      for(int j = 0; j < LENGTH; j++) {	
	if(ct[j] != null && substr.equals(ct[j].catstr)) {
	  Level p = ct[j].level;
	  if(p != null) 
	    return p;	  
	}
      }
    }
    return defaultLevel;
  }

  

  static class CT {
    public String   catstr;
    public Level level;

    CT(String catstr,  Level level) {
      this.catstr = catstr;
      this.level = level;
    }
  }
}
