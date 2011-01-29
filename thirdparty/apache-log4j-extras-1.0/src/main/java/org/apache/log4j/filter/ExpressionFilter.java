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

package org.apache.log4j.filter;

import org.apache.log4j.rule.ExpressionRule;
import org.apache.log4j.rule.Rule;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;


/**
 * A filter supporting complex expressions - supports both infix and postfix
 * expressions (infix expressions must first be converted to postfix prior
 * to processing).
 * <p/>
 * <p>See <code>org.apache.log4j.chainsaw.LoggingEventFieldResolver.java</code>
 * for the correct names for logging event fields
 * used when building expressions.
 * <p/>
 * <p>See <code>org.apache.log4j.chainsaw.rule</code> package
 * for a list of available
 * rules which can be applied using the expression syntax.
 * <p/>
 * <p>See <code>org.apache.log4j.chainsaw.RuleFactory</code> for the symbols
 * used to activate the corresponding rules.
 * <p/>
 * NOTE:  Grouping using parentheses is supported -
 * all tokens must be separated by spaces, and
 * operands which contain spaces are not yet supported.
 * <p/>
 * Example:
 * <p/>
 * In order to build a filter that displays all messages with
 * infomsg-45 or infomsg-44 in the message,
 * as well as all messages with a level of WARN or higher,
 * build an expression using
 * the LikeRule (supports java.util.regex based regular expressions) and the InequalityRule.
 * <b> ( MSG LIKE infomsg-4[4,5] ) && ( LEVEL >= WARN ) </b>
 * <p/>
 * Three options are required:
 * <b>Expression</b> - the expression to match
 * <b>ConvertInFixToPostFix</b> - convert from infix to posfix (default true)
 * <b>AcceptOnMatch</b> - true or false (default true)
 * <p/>
 * Meaning of <b>AcceptToMatch</b>:
 * If there is a match between the value of the
 * Expression option and the {@link LoggingEvent} and AcceptOnMatch is true,
 * the {@link #decide} method returns {@link Filter#ACCEPT}.
 * <p/>
 * If there is a match between the value of the
 * Expression option and the {@link LoggingEvent} and AcceptOnMatch is false,
 * {@link Filter#DENY} is returned.
 * <p/>
 * If there is no match, {@link Filter#NEUTRAL} is returned.
 *
 * @author Scott Deboy sdeboy@apache.org
 */
public class ExpressionFilter extends Filter {
    /**
     * accept on match.
     */
    boolean acceptOnMatch = true;
    /**
     * Convert in-fix to post-fix.
     */
    boolean convertInFixToPostFix = true;
    /**
     * Expression.
     */
    String expression;
    /**
     * Evaluated rule.
     */
    Rule expressionRule;

    /**
     * {@inheritDoc}
     */
    public void activateOptions() {
        expressionRule =
                ExpressionRule.getRule(expression, !convertInFixToPostFix);
    }

    /**
     * Set exp.
     * @param exp exp.
     */
    public void setExpression(final String exp) {
        this.expression = exp;
    }

    /**
     * Get expression.
     * @return expression.
     */
    public String getExpression() {
        return expression;
    }

    /**
     * Set convert in-fix to post-fix.
     * @param newValue new value.
     */
    public void setConvertInFixToPostFix(final boolean newValue) {
        this.convertInFixToPostFix = newValue;
    }

    /**
     * Get in-fix to post-fix conversion setting.
     * @return true if in-fix expressions are converted to post-fix.
     */
    public boolean getConvertInFixToPostFix() {
        return convertInFixToPostFix;
    }

    /**
     * Set whether filter should accept events if they match the expression.
     * @param newValue if true, accept on match.
     */
    public void setAcceptOnMatch(final boolean newValue) {
        this.acceptOnMatch = newValue;
    }

    /**
     * Gets whether filter accepts matching or non-matching events.
     * @return if true, accept matching events.
     */
    public boolean getAcceptOnMatch() {
        return acceptOnMatch;
    }

    /**
     * Determines if event matches the filter.
     * @param event logging event;
     * @return {@link Filter#NEUTRAL} is there is no string match.
     */
    public int decide(final LoggingEvent event) {
        if (expressionRule.evaluate(event)) {
            if (acceptOnMatch) {
                return Filter.ACCEPT;
            } else {
                return Filter.DENY;
            }
        }
        return Filter.NEUTRAL;
    }
}
