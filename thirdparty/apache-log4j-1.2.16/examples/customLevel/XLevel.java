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

package examples.customLevel;

import org.apache.log4j.Level;


/**
   This class introduces a new level level called TRACE. TRACE has
   lower level than DEBUG.

 */
public class XLevel extends Level {
  private static final long serialVersionUID = 2626753561969426769L;

  static public final int  TRACE_INT   = Level.DEBUG_INT - 1;
  static public final int  LETHAL_INT  = Level.FATAL_INT + 1;


  private static String TRACE_STR  = "TRACE";
  private static String LETHAL_STR  = "LETHAL";


  public static final XLevel TRACE = new XLevel(TRACE_INT, TRACE_STR, 7);
  public static final XLevel LETHAL = new XLevel(LETHAL_INT, LETHAL_STR, 
						       0);


  protected
  XLevel(int level, String strLevel, int syslogEquiv) {
    super(level, strLevel, syslogEquiv);
  }

  /**
     Convert the string passed as argument to a level. If the
     conversion fails, then this method returns {@link #TRACE}. 
  */
  public
  static
  Level toLevel(String sArg) {
    return (Level) toLevel(sArg, XLevel.TRACE);
  }


  public
  static
  Level toLevel(String sArg, Level defaultValue) {

    if(sArg == null) {
      return defaultValue;
    }
    String stringVal = sArg.toUpperCase();
    
    if(stringVal.equals(TRACE_STR)) {
      return XLevel.TRACE;
    } else if(stringVal.equals(LETHAL_STR)) {
      return XLevel.LETHAL;
    }
      
    return Level.toLevel(sArg, (Level) defaultValue);    
  }


  public
  static
  Level toLevel(int i) throws  IllegalArgumentException {
    switch(i) {
    case TRACE_INT: return XLevel.TRACE;
    case LETHAL_INT: return XLevel.LETHAL;
    }
    return Level.toLevel(i);
  }

}
  
