//
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
//

package com.cloud.agent.transport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.LogLevel;
import com.cloud.agent.api.LogLevel.Log4jLevel;

public class LoggingExclusionStrategy implements ExclusionStrategy {
    protected Logger exclusionLogger = null;

    protected Logger logger = LogManager.getLogger(getClass());

    @Override
    public boolean shouldSkipClass(Class<?> clazz) {
        if (clazz.isArray() || !Command.class.isAssignableFrom(clazz)) {
            return false;
        }
        Log4jLevel log4jLevel = null;
        LogLevel level = clazz.getAnnotation(LogLevel.class);
        if (level == null) {
            log4jLevel = LogLevel.Log4jLevel.Debug;
            logger.trace("Class {} does not have any log level annotation, considering level as debug.", clazz);
        } else {
            log4jLevel = level.value();
        }

        return !log4jLevel.enabled(exclusionLogger);
    }

    @Override
    public boolean shouldSkipField(FieldAttributes field) {
        LogLevel level = field.getAnnotation(LogLevel.class);
        return level != null && !level.value().enabled(exclusionLogger);
    }

    public LoggingExclusionStrategy(Logger logger) {
        exclusionLogger = logger;
    }

    public LoggingExclusionStrategy() {
    }
}
