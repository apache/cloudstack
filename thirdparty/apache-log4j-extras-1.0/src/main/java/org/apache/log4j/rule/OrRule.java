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

import java.util.Stack;

/**
 * A Rule class implementing logical or.
 *
 * @author Scott Deboy (sdeboy@apache.org)
 */
public class OrRule extends AbstractRule {
    /**
     * Serialization ID.
     */
  static final long serialVersionUID = 2088765995061413165L;
    /**
     * rule 1.
     */
  private final Rule rule1;
    /**
     * Rule 2.
     */
  private final Rule rule2;

    /**
     * Create new instance.
     * @param firstParam first rule
     * @param secondParam second rule
     */
  private OrRule(final Rule firstParam, final Rule secondParam) {
    super();
    this.rule1 = firstParam;
    this.rule2 = secondParam;
  }

    /**
     * Create new instance.
     * @param firstParam first rule
     * @param secondParam second rule
     * @return new instance
     */
  public static Rule getRule(final Rule firstParam, final Rule secondParam) {
      return new OrRule(firstParam, secondParam);
  }

    /**
     * Create new instance from top two elements of stack.
     * @param stack stack
     * @return new instance
     */
  public static Rule getRule(final Stack stack) {
      if (stack.size() < 2) {
          throw new IllegalArgumentException(
                  "Invalid OR rule - expected two rules but received "
                          + stack.size());
      }
      Object o2 = stack.pop();
      Object o1 = stack.pop();
      if ((o2 instanceof Rule) && (o1 instanceof Rule)) {
          Rule p2 = (Rule) o2;
          Rule p1 = (Rule) o1;
          return new OrRule(p1, p2);
      }
      throw new IllegalArgumentException("Invalid OR rule: " + o2 + "..." + o1);
  }

  /** {@inheritDoc} */
  public boolean evaluate(final LoggingEvent event) {
    return (rule1.evaluate(event) || rule2.evaluate(event));
  }
}
