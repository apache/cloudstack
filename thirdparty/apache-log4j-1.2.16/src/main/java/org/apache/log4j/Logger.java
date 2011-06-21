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

package org.apache.log4j;

import org.apache.log4j.spi.LoggerFactory;


/**
  This is the central class in the log4j package. Most logging
  operations, except configuration, are done through this class.

  @since log4j 1.2

  @author Ceki G&uuml;lc&uuml; */
public class Logger extends Category {

  /**
     The fully qualified name of the Logger class. See also the
     getFQCN method. */
  private static final String FQCN = Logger.class.getName();


  protected
  Logger(String name) {
    super(name);
  }

  /**
    Log a message object with the {@link Level#FINE FINE} level which
    is just an alias for the {@link Level#DEBUG DEBUG} level.

    <p>This method first checks if this category is <code>DEBUG</code>
    enabled by comparing the level of this category with the {@link
    Level#DEBUG DEBUG} level. If this category is
    <code>DEBUG</code> enabled, then it converts the message object
    (passed as parameter) to a string by invoking the appropriate
    {@link org.apache.log4j.or.ObjectRenderer}. It then proceeds to call all the
    registered appenders in this category and also higher in the
    hierarchy depending on the value of the additivity flag.

    <p><b>WARNING</b> Note that passing a {@link Throwable} to this
    method will print the name of the <code>Throwable</code> but no
    stack trace. To print a stack trace use the {@link #debug(Object,
    Throwable)} form instead.

    @param message the message object to log. */
  //public
  //void fine(Object message) {
  //  if(repository.isDisabled(Level.DEBUG_INT))
  //	return;
  //  if(Level.DEBUG.isGreaterOrEqual(this.getChainedLevel())) {
  //	forcedLog(FQCN, Level.DEBUG, message, null);
  //  }
  //}


  /**
   Log a message object with the <code>FINE</code> level including
   the stack trace of the {@link Throwable} <code>t</code> passed as
   parameter.

   <p>See {@link #fine(Object)} form for more detailed information.

   @param message the message object to log.
   @param t the exception to log, including its stack trace.  */
  //public
  //void fine(Object message, Throwable t) {
  //  if(repository.isDisabled(Level.DEBUG_INT))
  //	return;
  //  if(Level.DEBUG.isGreaterOrEqual(this.getChainedLevel()))
  //	forcedLog(FQCN, Level.FINE, message, t);
  //}

  /**
   * Retrieve a logger named according to the value of the
   * <code>name</code> parameter. If the named logger already exists,
   * then the existing instance will be returned. Otherwise, a new
   * instance is created.  
   *
   * <p>By default, loggers do not have a set level but inherit it
   * from their neareast ancestor with a set level. This is one of the
   * central features of log4j.
   *
   * @param name The name of the logger to retrieve.  
  */
  static
  public
  Logger getLogger(String name) {
    return LogManager.getLogger(name);
  }

  /**
   * Shorthand for <code>getLogger(clazz.getName())</code>.
   *
   * @param clazz The name of <code>clazz</code> will be used as the
   * name of the logger to retrieve.  See {@link #getLogger(String)}
   * for more detailed information.
   */
  static
  public
  Logger getLogger(Class clazz) {
    return LogManager.getLogger(clazz.getName());
  }


  /**
   * Return the root logger for the current logger repository.
   * <p>
   * The {@link #getName Logger.getName()} method for the root logger always returns
   * stirng value: "root". However, calling
   * <code>Logger.getLogger("root")</code> does not retrieve the root
   * logger but a logger just under root named "root".
   * <p>
   * In other words, calling this method is the only way to retrieve the 
   * root logger.
   */
  public
  static
  Logger getRootLogger() {
    return LogManager.getRootLogger();
  }

  /**
     Like {@link #getLogger(String)} except that the type of logger
     instantiated depends on the type returned by the {@link
     LoggerFactory#makeNewLoggerInstance} method of the
     <code>factory</code> parameter.

     <p>This method is intended to be used by sub-classes.

     @param name The name of the logger to retrieve.

     @param factory A {@link LoggerFactory} implementation that will
     actually create a new Instance.

     @since 0.8.5 */
  public
  static
  Logger getLogger(String name, LoggerFactory factory) {
    return LogManager.getLogger(name, factory);
  }

    /**
     * Log a message object with the {@link org.apache.log4j.Level#TRACE TRACE} level.
     *
     * @param message the message object to log.
     * @see #debug(Object) for an explanation of the logic applied.
     * @since 1.2.12
     */
    public void trace(Object message) {
      if (repository.isDisabled(Level.TRACE_INT)) {
        return;
      }

      if (Level.TRACE.isGreaterOrEqual(this.getEffectiveLevel())) {
        forcedLog(FQCN, Level.TRACE, message, null);
      }
    }

    /**
     * Log a message object with the <code>TRACE</code> level including the
     * stack trace of the {@link Throwable}<code>t</code> passed as parameter.
     *
     * <p>
     * See {@link #debug(Object)} form for more detailed information.
     * </p>
     *
     * @param message the message object to log.
     * @param t the exception to log, including its stack trace.
     * @since 1.2.12
     */
    public void trace(Object message, Throwable t) {
      if (repository.isDisabled(Level.TRACE_INT)) {
        return;
      }

      if (Level.TRACE.isGreaterOrEqual(this.getEffectiveLevel())) {
        forcedLog(FQCN, Level.TRACE, message, t);
      }
    }

    /**
     * Check whether this category is enabled for the TRACE  Level.
     * @since 1.2.12
     *
     * @return boolean - <code>true</code> if this category is enabled for level
     *         TRACE, <code>false</code> otherwise.
     */
    public boolean isTraceEnabled() {
        if (repository.isDisabled(Level.TRACE_INT)) {
            return false;
          }

          return Level.TRACE.isGreaterOrEqual(this.getEffectiveLevel());
    }

}
