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

package org.apache.log4j.filter;

import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;

/**
   This is a very simple filter based on string matching.

   <p>The filter admits two options <b>StringToMatch</b> and
   <b>AcceptOnMatch</b>. If there is a match between the value of the
   StringToMatch option and the message of the {@link LoggingEvent},
   then the {@link #decide(LoggingEvent)} method returns 
   {@link org.apache.log4j.spi.Filter#ACCEPT} if
   the <b>AcceptOnMatch</b> option value is true, if it is false then
   {@link org.apache.log4j.spi.Filter#DENY} is returned. If there is no match, {@link
   org.apache.log4j.spi.Filter#NEUTRAL} is returned.

   @author Ceki G&uuml;lc&uuml;

   @since 0.9.0 
*/
public class StringMatchFilter extends Filter {
  
  boolean acceptOnMatch = true;
  String stringToMatch;
  
  public
  void setStringToMatch(String s) {
    stringToMatch = s;
  }
  
  public
  String getStringToMatch() {
    return stringToMatch;
  }
  
  public
  void setAcceptOnMatch(boolean acceptOnMatch) {
    this.acceptOnMatch = acceptOnMatch;
  }
  
  public
  boolean getAcceptOnMatch() {
    return acceptOnMatch;
  }

  /**
     Returns {@link Filter#NEUTRAL} is there is no string match.
   */
  public
  int decide(LoggingEvent event) {
    String msg = event.getRenderedMessage();

    if(msg == null ||  stringToMatch == null)
      return Filter.NEUTRAL;
    

    if( msg.indexOf(stringToMatch) == -1 ) {
      return Filter.NEUTRAL;
    } else { // we've got a match
      if(acceptOnMatch) {
	return Filter.ACCEPT;
      } else {
	return Filter.DENY;
      }
    }
  }
}
