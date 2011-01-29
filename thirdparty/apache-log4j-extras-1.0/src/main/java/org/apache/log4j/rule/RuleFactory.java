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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Stack;
import org.apache.log4j.LogManager;

/**
 * A Factory class which, given a string representation of the rule,
 * and a context stack, will
 * return a Rule ready for evaluation against events.
 * If an operator is requested that isn't supported, 
 * an IllegalArgumentException is thrown.
 *
 * @author Scott Deboy (sdeboy@apache.org)
 */
public final class RuleFactory {
    /**
     * Singleton instance.
     */
  private static final RuleFactory FACTORY = new RuleFactory();
    /**
     * Rules.
     */
  private static final Collection RULES = new LinkedList();
    /**
     * AND operator literal.
     */
  private static final String AND_RULE = "&&";
    /**
     * OR operator literal.
     */
  private static final String OR_RULE = "||";
    /**
     * NOT operator literal.
     */
  private static final String NOT_RULE = "!";
    /**
     * Inequality operator literal.
     */
  private static final String NOT_EQUALS_RULE = "!=";
    /**
     * Equality operator literal.
     */
  private static final String EQUALS_RULE = "==";
    /**
     * Partial match operator literal.
     */
  private static final String PARTIAL_TEXT_MATCH_RULE = "~=";
    /**
     * Like operator literal.
     */
  private static final String LIKE_RULE = "like";
    /**
     * Exists operator literal.
     */
  private static final String EXISTS_RULE = "exists";
    /**
     * Less than operator literal.
     */
  private static final String LESS_THAN_RULE = "<";
    /**
     * Greater than operator literal.
     */
  private static final String GREATER_THAN_RULE = ">";
    /**
     * Less than or equal operator literal.
     */
  private static final String LESS_THAN_EQUALS_RULE = "<=";
    /**
     * Greater than or equal operator literal.
     */
  private static final String GREATER_THAN_EQUALS_RULE = ">=";

  static {
    RULES.add(AND_RULE);
    RULES.add(OR_RULE);
    RULES.add(NOT_RULE);
    RULES.add(NOT_EQUALS_RULE);
    RULES.add(EQUALS_RULE);
    RULES.add(PARTIAL_TEXT_MATCH_RULE);
    RULES.add(LIKE_RULE);
    RULES.add(EXISTS_RULE);
    RULES.add(LESS_THAN_RULE);
    RULES.add(GREATER_THAN_RULE);
    RULES.add(LESS_THAN_EQUALS_RULE);
    RULES.add(GREATER_THAN_EQUALS_RULE);
  }

    /**
     * Create instance.
     */
  private RuleFactory() {
        super();
    }

    /**
     * Get instance.
     * @return rule factory instance.
     */
  public static RuleFactory getInstance() {
      return FACTORY;
  }

    /**
     * Determine if specified string is a known operator.
     * @param symbol string
     * @return true if string is a known operator
     */
  public boolean isRule(final String symbol) {
    return ((symbol != null) && (RULES.contains(symbol.toLowerCase())));
  }

    /**
     * Create rule from applying operator to stack.
     * @param symbol symbol
     * @param stack stack
     * @return new instance
     */
  public Rule getRule(final String symbol, final Stack stack) {
    if (AND_RULE.equals(symbol)) {
      return AndRule.getRule(stack);
    }

    if (OR_RULE.equals(symbol)) {
      return OrRule.getRule(stack);
    }

    if (NOT_RULE.equals(symbol)) {
      return NotRule.getRule(stack);
    }

    if (NOT_EQUALS_RULE.equals(symbol)) {
      return NotEqualsRule.getRule(stack);
    }

    if (EQUALS_RULE.equals(symbol)) {
      return EqualsRule.getRule(stack);
    }

    if (PARTIAL_TEXT_MATCH_RULE.equals(symbol)) {
      return PartialTextMatchRule.getRule(stack);
    }

    if (RULES.contains(LIKE_RULE) && LIKE_RULE.equalsIgnoreCase(symbol)) {
      return LikeRule.getRule(stack);
    }

    if (EXISTS_RULE.equalsIgnoreCase(symbol)) {
      return ExistsRule.getRule(stack);
    }

    if (LESS_THAN_RULE.equals(symbol)) {
      return InequalityRule.getRule(LESS_THAN_RULE, stack);
    }

    if (GREATER_THAN_RULE.equals(symbol)) {
      return InequalityRule.getRule(GREATER_THAN_RULE, stack);
    }

    if (LESS_THAN_EQUALS_RULE.equals(symbol)) {
      return InequalityRule.getRule(LESS_THAN_EQUALS_RULE, stack);
    }

    if (GREATER_THAN_EQUALS_RULE.equals(symbol)) {
      return InequalityRule.getRule(GREATER_THAN_EQUALS_RULE, stack);
    }
    throw new IllegalArgumentException("Invalid rule: " + symbol);
  }
}
