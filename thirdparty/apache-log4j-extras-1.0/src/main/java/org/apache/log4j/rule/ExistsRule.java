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
 * A Rule class implementing a not null (and not empty string) check.
 *
 * @author Scott Deboy (sdeboy@apache.org)
 */
public class ExistsRule extends AbstractRule {
    /**
     * Serialization id.
     */
  static final long serialVersionUID = -5386265224649967464L;
    /**
     * field resolver.
     */
  private static final LoggingEventFieldResolver RESOLVER =
    LoggingEventFieldResolver.getInstance();
    /**
     * field name.
     */
  private final String field;

    /**
     * Create new instance.
     * @param fld field name.
     */
  private ExistsRule(final String fld) {
    super();
    if (!RESOLVER.isField(fld)) {
      throw new IllegalArgumentException(
        "Invalid EXISTS rule - " + fld + " is not a supported field");
    }

    this.field = fld;
  }

    /**
     * Get an instance of ExistsRule.
     * @param field field.
     * @return instance of ExistsRule.
     */
  public static Rule getRule(final String field) {
    return new ExistsRule(field);
  }

    /**
     * Create an instance of ExistsRule using the
     * top name on the stack.
     * @param stack stack
     * @return instance of ExistsRule.
     */
  public static Rule getRule(final Stack stack) {
    if (stack.size() < 1) {
      throw new IllegalArgumentException(
        "Invalid EXISTS rule - expected one parameter but received "
        + stack.size());
    }

    return new ExistsRule(stack.pop().toString());
  }

    /**
     * {@inheritDoc}
     */
  public boolean evaluate(final LoggingEvent event) {
    Object p2 = RESOLVER.getValue(field, event);

    return (!((p2 == null) || ((p2 != null) && p2.toString().equals(""))));
  }
}
