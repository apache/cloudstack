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
package com.cloud.agent.transport;

import org.apache.log4j.Logger;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.LogLevel;
import com.cloud.agent.api.LogLevel.Log4jLevel;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

public class LoggingExclusionStrategy implements ExclusionStrategy {
    Logger _logger = null;

    @Override
    public boolean shouldSkipClass(Class<?> clazz) {
        if (clazz.isArray() || !Command.class.isAssignableFrom(clazz)) {
            return false;
        }
        Log4jLevel log4jLevel = null;
        LogLevel level = clazz.getAnnotation(LogLevel.class);
        if (level == null) {
            log4jLevel = LogLevel.Log4jLevel.Debug;
        } else {
            log4jLevel = level.value();
        }

        return !log4jLevel.enabled(_logger);
    }

    @Override
    public boolean shouldSkipField(FieldAttributes field) {
        LogLevel level = field.getAnnotation(LogLevel.class);
        return level != null && !level.value().enabled(_logger);
    }

    public LoggingExclusionStrategy(Logger logger) {
        _logger = logger;
    }
}
