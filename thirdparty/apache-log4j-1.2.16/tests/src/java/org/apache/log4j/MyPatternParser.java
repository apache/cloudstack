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

import org.apache.log4j.helpers.FormattingInfo;
import org.apache.log4j.helpers.PatternConverter;
import org.apache.log4j.helpers.PatternParser;
import org.apache.log4j.spi.LoggingEvent;

/**
  Example showing how to extend PatternParser to recognize additional
  conversion characters.  The examples shows that minimum and maximum
  width and alignment settings apply for "extension" conversion
  characters just as they do for PatternLayout recognized characters.
  
  <p>In this case MyPatternParser recognizes %# and outputs the value
  of an internal counter which is also incremented at each call.

  See <a href=doc-files/MyPatternParser.java><b>source</b></a> code
   for more details.
  
  @see org.apache.log4j.examples.MyPatternLayout
  @see org.apache.log4j.helpers.PatternParser
  @see org.apache.log4j.PatternLayout

  @author Anders Kristensen 
*/
public class MyPatternParser extends PatternParser {

  int counter = 0;

  public
  MyPatternParser(String pattern) {
    super(pattern);
  }
    
  public
  void finalizeConverter(char c) {
    if (c == '#') {
      addConverter(new UserDirPatternConverter(formattingInfo));
      currentLiteral.setLength(0);
    } else {
      super.finalizeConverter(c);
    }
  }
  
  private class UserDirPatternConverter extends PatternConverter {
    UserDirPatternConverter(FormattingInfo formattingInfo) {
      super(formattingInfo);
    }

    public
    String convert(LoggingEvent event) {
      return String.valueOf(++counter);
    }
  }  
}
