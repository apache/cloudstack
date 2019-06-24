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
package com.cloud.consoleproxy;

import com.cloud.consoleproxy.util.Logger;
import com.cloud.consoleproxy.util.LoggerFactory;

public class ConsoleProxyLoggerFactory implements LoggerFactory {
    public ConsoleProxyLoggerFactory() {
    }

    @Override
    public Logger getLogger(Class<?> clazz) {
        return new Log4jLogger(org.apache.log4j.Logger.getLogger(clazz));
    }

    public static class Log4jLogger extends Logger {
        private org.apache.log4j.Logger logger;

        public Log4jLogger(org.apache.log4j.Logger logger) {
            this.logger = logger;
        }

        @Override
        public boolean isTraceEnabled() {
            return logger.isTraceEnabled();
        }

        @Override
        public boolean isDebugEnabled() {
            return logger.isDebugEnabled();
        }

        @Override
        public boolean isInfoEnabled() {
            return logger.isInfoEnabled();
        }

        @Override
        public void trace(Object message) {
            logger.trace(message);
        }

        @Override
        public void trace(Object message, Throwable exception) {
            logger.trace(message, exception);
        }

        @Override
        public void info(Object message) {
            logger.info(message);
        }

        @Override
        public void info(Object message, Throwable exception) {
            logger.info(message, exception);
        }

        @Override
        public void debug(Object message) {
            logger.debug(message);
        }

        @Override
        public void debug(Object message, Throwable exception) {
            logger.debug(message, exception);
        }

        @Override
        public void warn(Object message) {
            logger.warn(message);
        }

        @Override
        public void warn(Object message, Throwable exception) {
            logger.warn(message, exception);
        }

        @Override
        public void error(Object message) {
            logger.error(message);
        }

        @Override
        public void error(Object message, Throwable exception) {
            logger.error(message, exception);
        }
    }
}
