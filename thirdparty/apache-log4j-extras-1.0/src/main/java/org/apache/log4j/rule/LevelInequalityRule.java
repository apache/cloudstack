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

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.helpers.UtilLoggingLevel;
import org.apache.log4j.spi.LoggingEvent;

/**
 * A Rule class implementing inequality evaluation for Levels (log4j and
 * util.logging) using the toInt method.
 *
 * @author Scott Deboy (sdeboy@apache.org)
 */
public class LevelInequalityRule {
    /**
     * Level list.
     */
    private static List levelList;
    /**
     * List equivalents of java.util.logging levels.
     */
    private static List utilLoggingLevelList;


    static {
        populateLevels();
    }

    /**
     * Create new instance.
     */
    private LevelInequalityRule() {
        super();
    }

    /**
     * Populate list of levels.
     */
    private static void populateLevels() {
        levelList = new LinkedList();

        levelList.add(Level.FATAL.toString());
        levelList.add(Level.ERROR.toString());
        levelList.add(Level.WARN.toString());
        levelList.add(Level.INFO.toString());
        levelList.add(Level.DEBUG.toString());
		Level trace = Level.toLevel(5000, null);
		if (trace != null) {
			levelList.add(trace.toString());
		}

        utilLoggingLevelList = new LinkedList();

        utilLoggingLevelList.add(UtilLoggingLevel.SEVERE.toString());
        utilLoggingLevelList.add(UtilLoggingLevel.WARNING.toString());
        utilLoggingLevelList.add(UtilLoggingLevel.INFO.toString());
        utilLoggingLevelList.add(UtilLoggingLevel.CONFIG.toString());
        utilLoggingLevelList.add(UtilLoggingLevel.FINE.toString());
        utilLoggingLevelList.add(UtilLoggingLevel.FINER.toString());
        utilLoggingLevelList.add(UtilLoggingLevel.FINEST.toString());

    }

    /**
     * Create new rule.
     * @param inequalitySymbol inequality symbol.
     * @param value Symbolic name of comparison level.
     * @return instance of AbstractRule.
     */
    public static Rule getRule(final String inequalitySymbol,
                               final String value) {

        Level thisLevel;

        //if valid util.logging levels are used against events
        // with log4j levels, the
        //DEBUG level is used and an illegalargumentexception won't be generated

        //an illegalargumentexception is only generated
        // if the user types a level name
        //that doesn't exist as either a log4j or util.logging level name
        if (levelList.contains(value.toUpperCase())) {
            thisLevel = Level.toLevel(value.toUpperCase());
        } else if (utilLoggingLevelList.contains(value.toUpperCase())) {
            thisLevel = UtilLoggingLevel.toLevel(value.toUpperCase());
        } else {
            throw new IllegalArgumentException(
                    "Invalid level inequality rule - " + value
                            + " is not a supported level");
        }

        if ("<".equals(inequalitySymbol)) {
            return new LessThanRule(thisLevel);
        }
        if (">".equals(inequalitySymbol)) {
            return new GreaterThanRule(thisLevel);
        }
        if ("<=".equals(inequalitySymbol)) {
            return new LessThanEqualsRule(thisLevel);
        }
        if (">=".equals(inequalitySymbol)) {
            return new GreaterThanEqualsRule(thisLevel);
        }

        return null;
    }

    /**
     * Rule returning true if event level less than specified level.
     */
    private static final class LessThanRule extends AbstractRule {
        /**
         * Comparison level.
         */
        private final int newLevelInt;

        /**
         * Create new instance.
         * @param level comparison level.
         */
        public LessThanRule(final Level level) {
            super();
            newLevelInt = level.toInt();
        }

        /** {@inheritDoc} */
        public boolean evaluate(final LoggingEvent event) {
            return (event.getLevel().toInt() < newLevelInt);
        }
    }

    /**
     * Rule returning true if event level greater than specified level.
     */
    private static final class GreaterThanRule extends AbstractRule {
        /**
         * Comparison level.
         */
        private final int newLevelInt;

        /**
         * Create new instance.
         * @param level comparison level.
         */
        public GreaterThanRule(final Level level) {
            super();
            newLevelInt = level.toInt();
        }

        /** {@inheritDoc} */
        public boolean evaluate(final LoggingEvent event) {
            return (event.getLevel().toInt() > newLevelInt);
        }
    }

    /**
     * Rule returning true if event level greater than
     * or equal to specified level.
     */
    private static final class GreaterThanEqualsRule extends AbstractRule {
        /**
         * Comparison level.
         */
        private final int newLevelInt;

        /**
         * Create new instance.
         * @param level comparison level.
         */
        public GreaterThanEqualsRule(final Level level) {
            super();
            newLevelInt = level.toInt();
        }

        /** {@inheritDoc} */
        public boolean evaluate(final LoggingEvent event) {
            return event.getLevel().toInt() >= newLevelInt;
        }
    }

    /**
     * Rule returning true if event level less than or
     * equal to specified level.
     */

    private static final class LessThanEqualsRule extends AbstractRule {
        /**
         * Comparison level.
         */
        private final int newLevelInt;

        /**
         * Create new instance.
         * @param level comparison level.
         */
        public LessThanEqualsRule(final Level level) {
            super();
            newLevelInt = level.toInt();
        }

        /** {@inheritDoc} */
        public boolean evaluate(final LoggingEvent event) {
            return (event.getLevel().toInt() <= newLevelInt);
        }
    }
}
