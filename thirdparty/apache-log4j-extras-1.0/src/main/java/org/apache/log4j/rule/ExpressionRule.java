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
import java.util.StringTokenizer;


/**
 * A Rule class supporting both infix and postfix expressions,
 * accepting any rule which
 * is supported by the <code>RuleFactory</code>.
 *
 * NOTE: parsing is supported through the use of
 * <code>StringTokenizer</code>, which
 * implies two limitations:
 * 1: all tokens in the expression must be separated by spaces,
 * including parenthesis
 * 2: operands which contain spaces MUST be wrapped in single quotes.
 *    For example, the expression:
 *      msg == 'some msg'
 *    is a valid expression.
 * 3: To group expressions, use parentheses.
 *    For example, the expression:
 *      level >= INFO || ( msg == 'some msg' || logger == 'test' )
 *    is a valid expression.
 * See org.apache.log4j.rule.InFixToPostFix for a
 * description of supported operators.
 * See org.apache.log4j.spi.LoggingEventFieldResolver for field keywords.
 *
 * @author Scott Deboy (sdeboy@apache.org)
 */
public class ExpressionRule extends AbstractRule {
    /**
     * Serialization ID.
     */
  static final long serialVersionUID = 5809121703146893729L;
    /**
     * Converter.
     */
  private static final InFixToPostFix CONVERTER = new InFixToPostFix();
    /**
     * Compiler.
     */
  private static final PostFixExpressionCompiler COMPILER =
          new PostFixExpressionCompiler();
    /**
     * Rule.
     */
  private final Rule rule;

    /**
     * Create new instance.
     * @param r rule
     */
  private ExpressionRule(final Rule r) {
    super();
    this.rule = r;
  }

    /**
     * Get rule.
     * @param expression expression.
     * @return rule.
     */
  public static Rule getRule(final String expression) {
      return getRule(expression, false);
  }

    /**
     * Get rule.
     * @param expression expression.
     * @param isPostFix If post-fix.
     * @return rule
     */
  public static Rule getRule(final String expression,
                             final boolean isPostFix) {
    String postFix = expression;
    if (!isPostFix) {
      postFix = CONVERTER.convert(expression);
    }

    return new ExpressionRule(COMPILER.compileExpression(postFix));
  }

    /**
     * {@inheritDoc}
     */
  public boolean evaluate(final LoggingEvent event) {
    return rule.evaluate(event);
  }

    /**
     * {@inheritDoc}
     */
  public String toString() {
      return rule.toString();
  }

  /**
   * Evaluate a boolean postfix expression.
   *
   */
  static final class PostFixExpressionCompiler {
      /**
       * Compile expression.
       * @param expression expression.
       * @return rule.
       */
    public Rule compileExpression(final String expression) {
      RuleFactory factory = RuleFactory.getInstance();

      Stack stack = new Stack();
      StringTokenizer tokenizer = new StringTokenizer(expression);

      while (tokenizer.hasMoreTokens()) {
        //examine each token
        String token = tokenizer.nextToken();
        if ((token.startsWith("'"))
                && (token.endsWith("'")
                && (token.length() > 2))) {
            token = token.substring(1, token.length() - 1);
        }
        if ((token.startsWith("'"))
                && (token.endsWith("'")
                && (token.length() == 2))) {
            token = "";
        }

        boolean inText = token.startsWith("'");
        if (inText) {
            token = token.substring(1);
            while (inText && tokenizer.hasMoreTokens()) {
              token = token + " " + tokenizer.nextToken();
              inText = !(token.endsWith("'"));
          }
          if (token.length() > 0) {
              token = token.substring(0, token.length() - 1);
          }
        }

        //if a symbol is found, pop 2 off the stack,
          // evaluate and push the result
        if (factory.isRule(token)) {
          Rule r = factory.getRule(token, stack);
          stack.push(r);
        } else {
          //variables or constants are pushed onto the stack
          if (token.length() > 0) {
              stack.push(token);
          }
        }
      }

      if ((stack.size() == 1) && (!(stack.peek() instanceof Rule))) {
        //while this may be an attempt at creating an expression,
        //for ease of use, convert this single entry to a partial-text
        //match on the MSG field
        Object o = stack.pop();
        stack.push("MSG");
        stack.push(o);
        return factory.getRule("~=", stack);
      }

      //stack should contain a single rule if the expression is valid
      if ((stack.size() != 1) || (!(stack.peek() instanceof Rule))) {
        throw new IllegalArgumentException("invalid expression: " + expression);
      } else {
        return (Rule) stack.pop();
      }
    }
  }
}


