/*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements. See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership. The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package com.cloud.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.springframework.util.Assert;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static org.apache.logging.log4j.Level.ALL;
import static org.apache.logging.log4j.Level.DEBUG;
import static org.apache.logging.log4j.Level.ERROR;
import static org.apache.logging.log4j.Level.FATAL;
import static org.apache.logging.log4j.Level.INFO;
import static org.apache.logging.log4j.Level.OFF;

/**
*
* Tracks one or more patterns to determine whether or not they have been
* logged. It uses a streaming approach to determine whether or not a message
* has a occurred to prevent unnecessary memory consumption. Instances of this
* of this class are created using the {@link TestAppenderBuilder}.
*
* To use this class, register a one or more expected patterns by level as part
* of the test setup and retain an reference to the appender instance. After the
* expected logging events have occurred in the test case, call
* {@link TestAppender#assertMessagesLogged()} which will fail the test if any of the
* expected patterns were not logged.
*
*/
public final class TestAppender extends AbstractAppender {
    private final static String APPENDER_NAME = "test_appender";
    private final ImmutableMap<Level, Set<PatternResult>> expectedPatternResults;

    private TestAppender(final Map<Level, Set<PatternResult>> expectedPatterns) {
        // TODO evaluate these parameters
        super("TestAppender", null, null);
        expectedPatternResults = ImmutableMap.copyOf(expectedPatterns);
    }

    @Override
    public void append(LogEvent loggingEvent) {
        checkArgument(loggingEvent != null, "append requires a non-null loggingEvent");
        final Level level = loggingEvent.getLevel();
        checkState(expectedPatternResults.containsKey(level), "level " + level + " not supported by append");
        for (final PatternResult patternResult : expectedPatternResults.get(level)) {
            if (patternResult.getPattern().matcher(loggingEvent.getMessage().getFormattedMessage()).matches()) {
                patternResult.markFound();
            }
        }
    }

    public void close() {
// Do nothing ...
    }
    public boolean requiresLayout() {
        return false;
    }
    public void assertMessagesLogged() {
        final List<String> unloggedPatterns = new ArrayList<>();
        for (final Map.Entry<Level, Set<PatternResult>> expectedPatternResult : expectedPatternResults.entrySet()) {
            for (final PatternResult patternResults : expectedPatternResult.getValue()) {
                if (!patternResults.isFound()) {
                    unloggedPatterns.add(format("%1$s was not logged for level %2$s",
                            patternResults.getPattern().toString(), expectedPatternResult.getKey()));
                }
            }
        }
        if (!unloggedPatterns.isEmpty()) {
            //Raise an assert
            Assert.isTrue(false, Joiner.on(",").join(unloggedPatterns));
        }
    }

    private static final class PatternResult {
        private final Pattern pattern;
        private boolean foundFlag = false;
        private PatternResult(Pattern pattern) {
            super();
            this.pattern = pattern;
        }
        public Pattern getPattern() {
            return pattern;
        }
        public void markFound() {
        // This operation is thread-safe because the value will only ever be switched from false to true. Therefore,
        // multiple threads mutating the value for a pattern will not corrupt the value ...
            foundFlag = true;
        }
        public boolean isFound() {
            return foundFlag;
        }
        @Override
        public boolean equals(Object thatObject) {
            if (this == thatObject) {
                return true;
            }
            if (thatObject == null || getClass() != thatObject.getClass()) {
                return false;
            }
            PatternResult thatPatternResult = (PatternResult) thatObject;
            return foundFlag == thatPatternResult.foundFlag &&
                    Objects.equal(pattern, thatPatternResult.pattern);
        }
        @Override
        public int hashCode() {
            return Objects.hashCode(pattern, foundFlag);
        }
        @Override
        public String toString() {
            return format("Pattern Result [ pattern: %1$s, markFound: %2$s ]", pattern.toString(), foundFlag);
        }
    }

    public static final class TestAppenderBuilder {
        private final Map<Level, Set<PatternResult>> expectedPatterns;
        public TestAppenderBuilder() {
            super();
            expectedPatterns = new HashMap<>();
            expectedPatterns.put(ALL, new HashSet<PatternResult>());
            expectedPatterns.put(DEBUG, new HashSet<PatternResult>());
            expectedPatterns.put(ERROR, new HashSet<PatternResult>());
            expectedPatterns.put(FATAL, new HashSet<PatternResult>());
            expectedPatterns.put(INFO, new HashSet<PatternResult>());
            expectedPatterns.put(OFF, new HashSet<PatternResult>());
        }
        public TestAppenderBuilder addExpectedPattern(final Level level, final String pattern) {
            checkArgument(level != null, "addExpectedPattern requires a non-null level");
            checkArgument(!isNullOrEmpty(pattern), "addExpectedPattern requires a non-blank pattern");
            checkState(expectedPatterns.containsKey(level), "level " + level + " is not supported by " + getClass().getName());
            expectedPatterns.get(level).add(new PatternResult(Pattern.compile(pattern)));
            return this;
        }
        public TestAppender build() {
            return new TestAppender(expectedPatterns);
        }
    }
    /**
     *
     * Attaches a {@link TestAppender} to a {@link Logger} and ensures that it is the only
     * test appender attached to the logger.
     *
     * @param logger The logger which will be monitored by the test
     * @param testAppender The test appender to attach to {@code logger}
     */
    public static void safeAddAppender(Logger logger, TestAppender testAppender) {
        org.apache.logging.log4j.core.Logger coreLogger
            = (org.apache.logging.log4j.core.Logger)logger;
        org.apache.logging.log4j.core.LoggerContext context
            = (org.apache.logging.log4j.core.LoggerContext)coreLogger.getContext();
        org.apache.logging.log4j.core.config.Configuration configuration
            = (org.apache.logging.log4j.core.config.Configuration)context.getConfiguration();

        Appender appender = configuration.getAppender(APPENDER_NAME);

        coreLogger.removeAppender(appender);
        coreLogger.addAppender(testAppender);
    }
}