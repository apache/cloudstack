/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.utils.log;

/**
 * wrapper class for the logging functionality of the log library in use
 */
public class Logger {
    private org.apache.log4j.Logger logger;
    public Logger(org.apache.log4j.Logger logger) {
        this.logger = logger;
    }

    // log simple strings
    public void log(Level p, String entry) {
        logger.log(p, entry);
    }

    public void error(String s) {
        logger.log(Level.ERROR, s);
    }

    public void warn(String s) {
        logger.log(Level.WARN, s);
    }

    public void info(String s) {
        logger.log(Level.INFO, s);
    }

    public void debug(String s) {
        logger.log(Level.DEBUG, s);
    }

    public void trace(String s) {
        logger.log(Level.TRACE, s);
    }

    // log string with exceptions
    public void error(String s, Throwable e) {
        logger.error(s, e);
    }

    public void warn(String s, Throwable e) {
        logger.warn(s, e);
    }

    public void info(String s, Throwable e) {
        logger.info(s, e);
    }

    public void debug(String s, Throwable e) {
        logger.debug(s, e);
    }

    // level checks
    public boolean isEnabledFor(Level p) {
        return logger.isEnabledFor(p);
    }

    public boolean isDebugEnabled() {
        return logger.isEnabledFor(Level.DEBUG);
    }

    public boolean isTraceEnabled() {
        return logger.isEnabledFor(Level.TRACE);
    }
}
