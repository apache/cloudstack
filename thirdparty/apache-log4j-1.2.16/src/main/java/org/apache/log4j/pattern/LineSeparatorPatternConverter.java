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

import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;


/**
 * Formats a line separator.
 *
 * @author Ceki G&uuml;lc&uuml;
 */
public final class LineSeparatorPatternConverter
  extends LoggingEventPatternConverter {
  /**
   * Singleton.
   */
  private static final LineSeparatorPatternConverter INSTANCE =
    new LineSeparatorPatternConverter();

  /**
   * Line separator.
   */
  private final String lineSep;

  /**
   * Private constructor.
   */
  private LineSeparatorPatternConverter() {
    super("Line Sep", "lineSep");
    lineSep = Layout.LINE_SEP;
  }

  /**
   * Obtains an instance of pattern converter.
   * @param options options, may be null.
   * @return instance of pattern converter.
   */
  public static LineSeparatorPatternConverter newInstance(
    final String[] options) {
    return INSTANCE;
  }

  /**
   * {@inheritDoc}
   */
  public void format(LoggingEvent event, final StringBuffer toAppendTo) {
    toAppendTo.append(lineSep);
  }

  /**
   * {@inheritDoc}
   */
  public void format(final Object obj, final StringBuffer toAppendTo) {
    toAppendTo.append(lineSep);
  }
}
