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

package org.apache.log4j.pattern;

import org.apache.log4j.spi.LoggingEvent;


/**
 * LoggingEventPatternConverter is a base class for pattern converters
 * that can format information from instances of LoggingEvent.
 *
 * @author Curt Arnold
 *
 */
public abstract class LoggingEventPatternConverter extends PatternConverter {
  /**
   * Constructs an instance of LoggingEventPatternConverter.
   * @param name name of converter.
   * @param style CSS style for output.
   */
  protected LoggingEventPatternConverter(
    final String name, final String style) {
    super(name, style);
  }

  /**
   * Formats an event into a string buffer.
   * @param event event to format, may not be null.
   * @param toAppendTo string buffer to which the formatted event will be appended.  May not be null.
   */
  public abstract void format(
    final LoggingEvent event, final StringBuffer toAppendTo);

  /**
   * {@inheritDoc}
   */
  public void format(final Object obj, final StringBuffer output) {
    if (obj instanceof LoggingEvent) {
      format((LoggingEvent) obj, output);
    }
  }

  /**
   * Normally pattern converters are not meant to handle Exceptions although
   * few pattern converters might.
   *
   * By examining the return values for this method, the containing layout will
   * determine whether it handles throwables or not.

   * @return true if this PatternConverter handles throwables
   */
  public boolean handlesThrowable() {
    return false;
  }
}
