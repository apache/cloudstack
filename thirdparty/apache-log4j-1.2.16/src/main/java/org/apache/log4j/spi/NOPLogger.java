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
package org.apache.log4j.spi;

import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

import java.util.Enumeration;
import java.util.ResourceBundle;
import java.util.Vector;

/**
 * No-operation implementation of Logger used by NOPLoggerRepository.
 * @since 1.2.15
 */
public final class NOPLogger extends Logger {
    /**
     * Create instance of Logger.
     * @param repo repository, may not be null.
     * @param name name, may not be null, use "root" for root logger.
     */
    public NOPLogger(NOPLoggerRepository repo, final String name) {
        super(name);
        this.repository = repo;
        this.level = Level.OFF;
        this.parent = this;
    }

    /** {@inheritDoc} */
    public void addAppender(final Appender newAppender) {
    }

    /** {@inheritDoc} */
    public void assertLog(final boolean assertion, final String msg) {
    }


    /** {@inheritDoc} */
    public void callAppenders(final LoggingEvent event) {
    }

    /** {@inheritDoc} */
    void closeNestedAppenders() {
    }

    /** {@inheritDoc} */
    public void debug(final Object message) {
    }


    /** {@inheritDoc} */
    public void debug(final Object message, final Throwable t) {
    }

    /** {@inheritDoc} */
    public void error(final Object message) {
    }

    /** {@inheritDoc} */
    public void error(final Object message, final Throwable t) {
    }


    /** {@inheritDoc} */
    public void fatal(final Object message) {
    }

    /** {@inheritDoc} */
    public void fatal(final Object message, final Throwable t) {
    }


    /** {@inheritDoc} */
    public Enumeration getAllAppenders() {
      return new Vector().elements();
    }

    /** {@inheritDoc} */
    public Appender getAppender(final String name) {
       return null;
    }

    /** {@inheritDoc} */
    public Level getEffectiveLevel() {
      return Level.OFF;
    }

    /** {@inheritDoc} */
    public Priority getChainedPriority() {
      return getEffectiveLevel();
    }

    /** {@inheritDoc} */
    public ResourceBundle getResourceBundle() {
      return null;
    }


    /** {@inheritDoc} */
    public void info(final Object message) {
    }

    /** {@inheritDoc} */
    public void info(final Object message, final Throwable t) {
    }

    /** {@inheritDoc} */
    public boolean isAttached(Appender appender) {
      return false;
    }

    /** {@inheritDoc} */
    public boolean isDebugEnabled() {
      return false;
    }

    /** {@inheritDoc} */
    public boolean isEnabledFor(final Priority level) {
      return false;
    }

    /** {@inheritDoc} */
    public boolean isInfoEnabled() {
      return false;
    }


    /** {@inheritDoc} */
    public void l7dlog(final Priority priority, final String key, final Throwable t) {
    }

    /** {@inheritDoc} */
    public void l7dlog(final Priority priority, final String key,  final Object[] params, final Throwable t) {
    }

    /** {@inheritDoc} */
    public void log(final Priority priority, final Object message, final Throwable t) {
    }

    /** {@inheritDoc} */
    public void log(final Priority priority, final Object message) {
    }

    /** {@inheritDoc} */
    public void log(final String callerFQCN, final Priority level, final Object message, final Throwable t) {
    }

    /** {@inheritDoc} */
    public void removeAllAppenders() {
    }


    /** {@inheritDoc} */
    public void removeAppender(Appender appender) {
    }

    /** {@inheritDoc} */
    public void removeAppender(final String name) {
    }

    /** {@inheritDoc} */
    public void setLevel(final Level level) {
    }


    /** {@inheritDoc} */
    public void setPriority(final Priority priority) {
    }

    /** {@inheritDoc} */
    public void setResourceBundle(final ResourceBundle bundle) {
    }

    /** {@inheritDoc} */
    public void warn(final Object message) {
    }

    /** {@inheritDoc} */
    public void warn(final Object message, final Throwable t) {
    }

    /** {@inheritDoc} */
    public void trace(Object message) {
    }

    /** {@inheritDoc} */
    public void trace(Object message, Throwable t) {
    }

    /** {@inheritDoc} */
    public boolean isTraceEnabled() {
        return false;
    }


}
