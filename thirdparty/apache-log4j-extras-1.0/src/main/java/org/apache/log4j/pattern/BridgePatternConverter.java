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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * The class implements the pre log4j 1.3 org.apache.log4j.helpers.PatternConverter
 * contract by delegating to the log4j 1.3 pattern implementation.
 *
 *
 * @author Curt Arnold
 *
 */
public final class BridgePatternConverter
  extends org.apache.log4j.helpers.PatternConverter {
  /**
   * Pattern converters.
   */
  private LoggingEventPatternConverter[] patternConverters;

  /**
   * Field widths and alignment corresponding to pattern converters.
   */
  private FormattingInfo[] patternFields;

  /**
   * Does pattern process exceptions.
   */
  private boolean handlesExceptions;

  /**
   * Create a new instance.
   * @param pattern pattern, may not be null.
   */
  public BridgePatternConverter(
    final String pattern) {
    next = null;
    handlesExceptions = false;

    List converters = new ArrayList();
    List fields = new ArrayList();
    Map converterRegistry = null;

    PatternParser.parse(
      pattern, converters, fields, converterRegistry,
      PatternParser.getPatternLayoutRules());

    patternConverters = new LoggingEventPatternConverter[converters.size()];
    patternFields = new FormattingInfo[converters.size()];

    int i = 0;
    Iterator converterIter = converters.iterator();
    Iterator fieldIter = fields.iterator();

    while (converterIter.hasNext()) {
      Object converter = converterIter.next();

      if (converter instanceof LoggingEventPatternConverter) {
        patternConverters[i] = (LoggingEventPatternConverter) converter;
        handlesExceptions |= patternConverters[i].handlesThrowable();
      } else {
        patternConverters[i] =
          new org.apache.log4j.pattern.LiteralPatternConverter("");
      }

      if (fieldIter.hasNext()) {
        patternFields[i] = (FormattingInfo) fieldIter.next();
      } else {
        patternFields[i] = FormattingInfo.getDefault();
      }

      i++;
    }
  }

  /**
   * {@inheritDoc}
   */
  protected String convert(final LoggingEvent event) {
    //
    //  code should be unreachable.
    //
    StringBuffer sbuf = new StringBuffer();
    format(sbuf, event);

    return sbuf.toString();
  }

  /**
     Format event to string buffer.
     @param sbuf string buffer to receive formatted event, may not be null.
     @param e event to format, may not be null.
   */
  public void format(final StringBuffer sbuf, final LoggingEvent e) {
    for (int i = 0; i < patternConverters.length; i++) {
      int startField = sbuf.length();
      patternConverters[i].format(e, sbuf);
      patternFields[i].format(startField, sbuf);
    }
  }

  /**
   * Will return false if any of the conversion specifiers in the pattern
   * handles {@link Exception Exceptions}.
   * @return true if the pattern formats any information from exceptions.
   */
  public boolean ignoresThrowable() {
    return !handlesExceptions;
  }
}
