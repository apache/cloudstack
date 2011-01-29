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
 * A Rule class implementing a logical 'and'.
 *
 * @author Scott Deboy (sdeboy@apache.org)
 */
public class AndRule extends AbstractRule {
    /**
     * First rule.
     */
  private final Rule firstRule;
    /**
     * Second rule.
     */
  private final Rule secondRule;
    /**
     * Serialization id.
     */
  static final long serialVersionUID = -8233444426923854651L;

    /**
     * Create new instance.
     * @param first first rule.
     * @param second second rule.
     */
  private AndRule(final Rule first, final Rule second) {
    super();
    this.firstRule = first;
    this.secondRule = second;
  }

    /**
     * Create rule from top two elements of stack.
     * @param stack stack of rules.
     * @return Rule that evaluates true only if both rules are true.
     */
  public static Rule getRule(final Stack stack) {
    if (stack.size() < 2) {
        throw new IllegalArgumentException(
                "Invalid AND rule - expected two rules but received "
                        + stack.size());
    }
    Object o2 = stack.pop();
    Object o1 = stack.pop();
    if ((o2 instanceof Rule) && (o1 instanceof Rule)) {
        Rule p2 = (Rule) o2;
        Rule p1 = (Rule) o1;
        return new AndRule(p1, p2);
    }
    throw new IllegalArgumentException("Invalid AND rule: " + o2 + "..." + o1);
  }

    /**
     * Get rule.
     * @param firstParam first rule.
     * @param secondParam second rule.
     * @return Rule that evaluates true only if both rules are true.
     */
  public static Rule getRule(final Rule firstParam, final Rule secondParam) {
    return new AndRule(firstParam, secondParam);
  }

    /**
     * {@inheritDoc}
     */
  public boolean evaluate(final LoggingEvent event) {
    return (firstRule.evaluate(event) && secondRule.evaluate(event));
  }
}
