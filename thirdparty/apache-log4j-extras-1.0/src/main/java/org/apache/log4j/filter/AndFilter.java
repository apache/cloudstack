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
import org.apache.log4j.spi.OptionHandler;
import org.apache.log4j.xml.UnrecognizedElementHandler;
import org.w3c.dom.Element;

import java.util.Properties;


/**
 * A filter that 'and's the results of any number of contained filters together.
 * 
 * For the filter to process events, all contained filters must return Filter.ACCEPT.
 * 
 * If the contained filters do not return Filter.ACCEPT, Filter.NEUTRAL is returned.
 * 
 * If acceptOnMatch is set to true, Filter.ACCEPT is returned.
 * If acceptOnMatch is set to false, Filter.DENY is returned.
 * 
 * Here is an example config that will accept only events that contain BOTH
 * a DEBUG level AND 'test' in the message:
 * 
 *<appender name="STDOUT" class="org.apache.log4j.ConsoleAppender">
 * <filter class="org.apache.log4j.filter.AndFilter">
 *  <filter class="org.apache.log4j.filter.LevelMatchFilter">
 *        <param name="levelToMatch" value="DEBUG" />
 *        <param name="acceptOnMatch" value="true" />
 *  </filter>
 *  <filter class="org.apache.log4j.filter.StringMatchFilter">
 *        <param name="stringToMatch" value="test" />
 *        <param name="acceptOnMatch" value="true" />
 *  </filter>
 *  <param name="acceptOnMatch" value="false"/>
 * </filter>
 * <filter class="org.apache.log4j.filter.DenyAllFilter"/>
 *<layout class="org.apache.log4j.SimpleLayout"/>
 *</appender>
 * 
 * To accept all events EXCEPT those events that contain a 
 * DEBUG level and 'test' in the message: 
 * change the AndFilter's acceptOnMatch param to false and remove the DenyAllFilter
 * 
 * NOTE: If you are defining a filter that is only relying on logging event content 
 * (no external or filter-managed state), you could opt instead
 * to use an ExpressionFilter with one of the following expressions:
 * 
 * LEVEL == DEBUG && MSG ~= 'test'
 * or
 * ! ( LEVEL == DEBUG && MSG ~= 'test' )
 *
 * XML configuration of this filter requires use of either log4j 1.2.15 or later or
 * org.apache.log4j.rolling.DOMConfigurator.
 *
 * @author Scott Deboy sdeboy@apache.org
 */
public class AndFilter extends Filter implements UnrecognizedElementHandler {
  Filter headFilter = null;
  Filter tailFilter = null;
  boolean acceptOnMatch = true;
  
  public void activateOptions() {
  }

  public void addFilter(final Filter filter) {
    System.out.println("add"+filter);
    if (headFilter == null) {
      headFilter = filter;
      tailFilter = filter;
    } else {
      tailFilter.next = filter;
    }
  }
  
  public void setAcceptOnMatch(final boolean acceptOnMatch) {
    this.acceptOnMatch = acceptOnMatch;
  }
  /**
   * If this event does not already contain location information, 
   * evaluate the event against the expression.
   * 
   * If the expression evaluates to true, generate a LocationInfo instance 
   * by creating an exception and set this LocationInfo on the event.
   * 
   * Returns {@link Filter#NEUTRAL}
   */
  public int decide(final LoggingEvent event) {
    boolean accepted = true;
    Filter f = headFilter;
    while (f != null) {
      accepted = accepted && (Filter.ACCEPT == f.decide(event));
      f = f.next;
    }
    if (accepted) {
      if(acceptOnMatch) {
        return Filter.ACCEPT;
      }
       return Filter.DENY;
    }
    return Filter.NEUTRAL;
  }

    /**
     * {@inheritDoc}
     */
  public boolean parseUnrecognizedElement(final Element element,
                                          final Properties props) throws Exception {
      final String nodeName = element.getNodeName();
      if ("filter".equals(nodeName)) {
          OptionHandler filter =
                  org.apache.log4j.extras.DOMConfigurator.parseElement(
                          element, props, Filter.class);
          if (filter instanceof Filter) {
              filter.activateOptions();
              this.addFilter((Filter) filter);
          }
          return true;
      }
      return false;
  }

}
