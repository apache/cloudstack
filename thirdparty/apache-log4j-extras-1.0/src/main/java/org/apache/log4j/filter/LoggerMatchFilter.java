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
   This is a very simple filter based on logger name matching.

   <p>The filter admits two options <b>LoggerToMatch</b> and
   <b>AcceptOnMatch</b>. If there is an exact match between the value
   of the <b>LoggerToMatch</b> option and the logger of the {@link
   org.apache.log4j.spi.LoggingEvent}, then the {@link #decide} method returns {@link
   org.apache.log4j.spi.Filter#ACCEPT} in case the <b>AcceptOnMatch</b> option value is set
   to <code>true</code>, if it is <code>false</code> then {@link
   org.apache.log4j.spi.Filter#DENY} is returned. If there is no match, {@link
   org.apache.log4j.spi.Filter#NEUTRAL} is returned.  A loggerToMatch of "root"
   matches both the root logger and a logger named "root".

   */
public class LoggerMatchFilter extends Filter {
  /**
     Do we return ACCEPT when a match occurs. Default is
     <code>true</code>.  */
  private boolean acceptOnMatch = true;

  /**
   * Logger name, may be null or empty in which case it matches root.
   */
  private String loggerToMatch = "root";

    /**
     * Sets logger name.
     * @param logger logger name.
     */
  public void setLoggerToMatch(final String logger) {
    if (logger == null) {
        loggerToMatch = "root";
    } else {
        loggerToMatch = logger;
    }
  }

    /**
     * Gets logger name.
     * @return logger name.
     */
  public String getLoggerToMatch() {
    return loggerToMatch;
  }

    /**
     * Sets whether a match should result in acceptance.
     * @param acceptOnMatch if true, accept if logger name matches, otherwise reject.
     */
  public void setAcceptOnMatch(final boolean acceptOnMatch) {
    this.acceptOnMatch = acceptOnMatch;
  }

    /**
     * Gets whether a match should result in acceptance.
     * @return true if event is accepted if logger name matches.
     */
  public boolean getAcceptOnMatch() {
    return acceptOnMatch;
  }


  /**
   * {@inheritDoc}
  */
  public int decide(final LoggingEvent event) {
    boolean matchOccured = loggerToMatch.equals(event.getLoggerName());
    if (matchOccured) {
      if (this.acceptOnMatch) {
        return Filter.ACCEPT;
      } else {
        return Filter.DENY;
      }
    } else {
      return Filter.NEUTRAL;
    }
  }
}
