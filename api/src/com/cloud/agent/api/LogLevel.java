// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.agent.api;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 */
@Target({TYPE, FIELD})
@Retention(RUNTIME)
public @interface LogLevel {
    public enum Log4jLevel { // Had to do this because Level is not primitive.
        Off(Level.OFF), Trace(Level.TRACE), Debug(Level.DEBUG);

        Level _level;

        private Log4jLevel(Level level) {
            _level = level;
        }

        public boolean enabled(Logger logger) {
            return _level != Level.OFF && logger.isEnabledFor(_level);
        }
    }

    Log4jLevel value() default Log4jLevel.Debug;
}
