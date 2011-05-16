/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.agent.api;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Specifies the log level we should log the command in.
 */
@Target({ TYPE, FIELD })
@Retention(RUNTIME)
public @interface LogLevel {
    public enum Log4jLevel {
        Off(Level.OFF),
        Trace(Level.TRACE),
        Debug(Level.DEBUG),
        Info(Level.INFO),
        Warn(Level.WARN),
        Error(Level.ERROR),
        Fatal(Level.FATAL);

        Level _level;

        private Log4jLevel(Level level) {
            _level = level;
        }

        public void log(Logger logger, String msg) {
            logger.log(_level, msg);
        }

        public boolean enabled(Logger logger) {
            return logger.isEnabledFor(_level);
        }
    }

    Log4jLevel level() default Log4jLevel.Debug;
}
