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
 * Formats a string literal.
 *
 * @author Curt Arnold
 *
 */
public final class LiteralPatternConverter extends LoggingEventPatternConverter {
  /**
   * String literal.
   */
  private final String literal;

  /**
   * Create a new instance.
   * @param literal string literal.
   */
  public LiteralPatternConverter(final String literal) {
    super("Literal", "literal");
    this.literal = literal;
  }

  /**
   * {@inheritDoc}
   */
  public void format(final LoggingEvent event, final StringBuffer toAppendTo) {
    toAppendTo.append(literal);
  }

  /**
   * {@inheritDoc}
   */
  public void format(final Object obj, final StringBuffer toAppendTo) {
    toAppendTo.append(literal);
  }
}
