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

package org.apache.log4j.rule;

import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.LoggingEventFieldResolver;

import java.util.Stack;


/**
 * A Rule class implementing not equals against two strings.
 *
 * @author Scott Deboy (sdeboy@apache.org)
 */
public class NotEqualsRule extends AbstractRule {
    /**
     * Serialization ID.
     */
  static final long serialVersionUID = -1135478467213793211L;
    /**
     * Resolver.
     */
  private static final LoggingEventFieldResolver RESOLVER =
    LoggingEventFieldResolver.getInstance();
    /**
     * Field.
     */
  private final String field;
    /**
     * Value.
     */
  private final String value;

    /**
     * Create new instance.
     * @param field field
     * @param value value
     */
  private NotEqualsRule(final String field, final String value) {
    super();
    if (!RESOLVER.isField(field)) {
      throw new IllegalArgumentException(
        "Invalid NOT EQUALS rule - " + field + " is not a supported field");
    }

    this.field = field;
    this.value = value;
  }

    /**
     * Get new instance.
     * @param field field
     * @param value value
     * @return new instance.
     */
  public static Rule getRule(final String field, final String value) {
    return new NotEqualsRule(field, value);
  }

    /**
     * Get new instance from top two elements of stack.
     * @param stack stack.
     * @return new instance.
     */
  public static Rule getRule(final Stack stack) {
    if (stack.size() < 2) {
      throw new IllegalArgumentException(
        "Invalid NOT EQUALS rule - expected two parameters but received "
        + stack.size());
    }

    String p2 = stack.pop().toString();
    String p1 = stack.pop().toString();

    return new NotEqualsRule(p1, p2);
  }

    /** {@inheritDoc} */
  public boolean evaluate(final LoggingEvent event) {
    Object p2 = RESOLVER.getValue(field, event);

    return ((p2 != null) && !(p2.toString().equals(value)));
  }
}
