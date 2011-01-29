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

import java.awt.Color;
import java.io.Serializable;

import org.apache.log4j.spi.LoggingEvent;


/**
 * A Rule class which also holds a color.
 *
 * @author Scott Deboy (sdeboy@apache.org)
 */
public class ColorRule extends AbstractRule implements Serializable {
    /**
     * Serialization id.
     */
  static final long serialVersionUID = -794434783372847773L;

    /**
     * Wrapped rule.
     */
  private final Rule rule;
    /**
     * Foreground color.
     */
  private final Color foregroundColor;
    /**
     * Background color.
     */
  private final Color backgroundColor;
    /**
     * Expression.
     */
  private final String expression;

    /**
     * Create new instance.
     * @param expression expression.
     * @param rule rule.
     * @param backgroundColor background color.
     * @param foregroundColor foreground color.
     */
  public ColorRule(final String expression,
                   final Rule rule,
                   final Color backgroundColor,
                   final Color foregroundColor) {
    super();
    this.expression = expression;
    this.rule = rule;
    this.backgroundColor = backgroundColor;
    this.foregroundColor = foregroundColor;
  }

    /**
     * Get rule.
     * @return underlying rule.
     */
  public Rule getRule() {
      return rule;
  }

    /**
     * Get foreground color.
     * @return foreground color.
     */
  public Color getForegroundColor() {
    return foregroundColor;
  }

    /**
     * Get background color.
     * @return background color.
     */
  public Color getBackgroundColor() {
    return backgroundColor;
  }

    /**
     * Get expression.
     * @return expression.
     */
  public String getExpression() {
      return expression;
  }

    /**
     * {@inheritDoc}
     */
  public boolean evaluate(final LoggingEvent event) {
    return (rule != null && rule.evaluate(event));
  }

    /**
     * {@inheritDoc}
     */
  public String toString() {
      StringBuffer buf = new StringBuffer("color rule - expression: ");
      buf.append(expression);
      buf.append(", rule: ");
      buf.append(rule);
      buf.append(" bg: ");
      buf.append(backgroundColor);
      buf.append(" fg: ");
      buf.append(foregroundColor);
      return buf.toString();
  }
}
