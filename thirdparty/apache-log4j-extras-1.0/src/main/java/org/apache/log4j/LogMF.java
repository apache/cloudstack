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

import org.apache.log4j.spi.LoggingEvent;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.ResourceBundle;


/**
 * This class provides static methods to
 * format log messages using java.text.MessageFormat.
 *
 */
public final class LogMF {
    /**
     * Trace level.
     */
    private static final Level TRACE = new Level(5000, "TRACE", 7);
    /**
     * private constructor.
     *
     */
    private LogMF() {
    }


    /**
     * Returns a Boolean instance representing the specified boolean.
     * Boolean.valueOf was added in JDK 1.4.
     * @param b a boolean value.
     * @return a Boolean instance representing b.
     */
    private static Boolean valueOf(final boolean b) {
        if (b) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    /**
     * Returns a Character instance representing the specified char.
     * Character.valueOf was added in JDK 1.5.
     * @param c a character value.
     * @return a Character instance representing c.
     */
    private static Character valueOf(final char c) {
        return new Character(c);
    }

    /**
     * Returns a Byte instance representing the specified byte.
     * Byte.valueOf was added in JDK 1.5.
     * @param b a byte value.
     * @return a Byte instance representing b.
     */
    private static Byte valueOf(final byte b) {
        return new Byte(b);
    }

    /**
     * Returns a Short instance representing the specified short.
     * Short.valueOf was added in JDK 1.5.
     * @param b a short value.
     * @return a Byte instance representing b.
     */
    private static Short valueOf(final short b) {
        return new Short(b);
    }

    /**
     * Returns an Integer instance representing the specified int.
     * Integer.valueOf was added in JDK 1.5.
     * @param b an int value.
     * @return an Integer instance representing b.
     */
    private static Integer valueOf(final int b) {
        return new Integer(b);
    }

    /**
     * Returns a Long instance representing the specified long.
     * Long.valueOf was added in JDK 1.5.
     * @param b a long value.
     * @return a Long instance representing b.
     */
    private static Long valueOf(final long b) {
        return new Long(b);
    }

    /**
     * Returns a Float instance representing the specified float.
     * Float.valueOf was added in JDK 1.5.
     * @param b a float value.
     * @return a Float instance representing b.
     */
    private static Float valueOf(final float b) {
        return new Float(b);
    }

    /**
     * Returns a Double instance representing the specified double.
     * Double.valueOf was added in JDK 1.5.
     * @param b a double value.
     * @return a Byte instance representing b.
     */
    private static Double valueOf(final double b) {
        return new Double(b);
    }



    /**
     * Create new array.
     * @param param1 parameter 1.
     * @return  new array.
     */
    private static Object[] toArray(final Object param1) {
        return new Object[] {
                param1
        };
    }

    /**
     * Create new array.
     * @param param1 parameter 1.
     * @param param2 parameter 2.
     * @return  new array.
     */
    private static Object[] toArray(final Object param1,
                             final Object param2) {
        return new Object[] {
                param1, param2
        };
    }

    /**
     * Create new array.
     * @param param1 parameter 1.
     * @param param2 parameter 2.
     * @param param3 parameter 3.
     * @return  new array.
     */
    private static Object[] toArray(final Object param1,
                             final Object param2,
                             final Object param3) {
        return new Object[] {
                param1, param2, param3
        };
    }

    /**
     * Create new array.
     * @param param1 parameter 1.
     * @param param2 parameter 2.
     * @param param3 parameter 3.
     * @param param4 parameter 4.
     * @return  new array.
     */
    private static Object[] toArray(final Object param1,
                             final Object param2,
                             final Object param3,
                             final Object param4) {
        return new Object[] {
                param1, param2, param3, param4
        };
    }

    /**
     * Formats arguments using a minimal subset
     * of MessageFormat syntax.
     * @param pattern pattern, may not be null.
     * @param arg0 argument, may be null.
     * @return Message string or null if pattern
     * is not supported.
     */
    private static String subsetFormat(final String pattern,
                                       final Object arg0) {
        if (pattern != null) {
            //
            //  find position of first brace
            //    if none then format is a literal
            int bracePos = pattern.indexOf("{");

            //
            //  if the first format is {0}
            //    and there are no other format specifiers
            //    and no quotes then substitute for {0}
            if (bracePos != -1) {
                if ((pattern.indexOf("{0}", bracePos) == bracePos)
                        && (pattern.indexOf("{", bracePos + 1) == -1)
                        && (pattern.indexOf("'") == -1)) {
                    String replacement;

                    if (arg0 instanceof String) {
                        replacement = arg0.toString();
                    } else if (arg0 instanceof Number) {
                        replacement = NumberFormat.getInstance().format(arg0);
                    } else if (arg0 instanceof Date) {
                        replacement = DateFormat.getDateTimeInstance(
                                DateFormat.SHORT,
                                DateFormat.SHORT).format(arg0);
                    } else {
                        replacement = String.valueOf(arg0);
                    }

                    final StringBuffer buf = new StringBuffer(pattern);
                    buf.replace(bracePos,
                            bracePos + "{0}".length(), replacement);

                    return buf.toString();
                }
            } else {
                //
                //   pattern is a literal with no braces
                //    and not quotes, return pattern.
                if (pattern.indexOf("'") == -1) {
                    return pattern;
                }
            }
        }

        return null;
    }

    /**
     * Formats arguments using MessageFormat.
     * @param pattern pattern, may be malformed or null.
     * @param arguments arguments, may be null or mismatched.
     * @return Message string or null
     */
    private static String format(final String pattern,
                                 final Object[] arguments) {
        if (pattern == null) {
            return null;
        }
        try {
            return MessageFormat.format(pattern, arguments);
        } catch (IllegalArgumentException ex) {
            return pattern;
        }
    }

    /**
     * Formats arguments using MessageFormat.
     * @param pattern pattern, may be malformed.
     * @param arg0 argument, may be null or mismatched.
     * @return Message string
     */
    private static String format(final String pattern, final Object arg0) {
        String msg = subsetFormat(pattern, arg0);

        if (msg == null) {
            msg = format(pattern, toArray(arg0));
        }

        return msg;
    }

    /**
     * Formats arguments using MessageFormat using a pattern from
     * a resource bundle.
     * @param resourceBundleName name of resource bundle, may be null.
     * @param key key for pattern in resource bundle, may be null.
     * @param arguments arguments, may be null or mismatched.
     * @return Message string or null
     */
    private static String format(
            final String resourceBundleName,
            final String key,
            final Object[] arguments) {
        String pattern;
        if (resourceBundleName != null) {
            try {
                ResourceBundle bundle =
                        ResourceBundle.getBundle(resourceBundleName);
                pattern = bundle.getString(key);
            } catch (Exception ex) {
                pattern = key;
            }
        } else {
            pattern = key;
        }
        return format(pattern, arguments);
    }


    /**
     * Fully Qualified Class Name of this class.
     */
    private static final String FQCN = LogMF.class.getName();

    /**
     * Equivalent of Logger.forcedLog.
     *
     * @param logger logger, may not be null.
     * @param level level, may not be null.
     * @param msg message, may be null.
     */
    private static void forcedLog(final Logger logger,
                                  final Level level,
                                  final String msg) {
        logger.callAppenders(new LoggingEvent(FQCN, logger, level, msg, null));
    }

    /**
     * Equivalent of Logger.forcedLog.
     *
     * @param logger logger, may not be null.
     * @param level level, may not be null.
     * @param msg message, may be null.
     * @param t throwable.
     */
    private static void forcedLog(final Logger logger,
                                  final Level level,
                                  final String msg,
                                  final Throwable t) {
        logger.callAppenders(new LoggingEvent(FQCN, logger, level, msg, t));
    }
    /**
         * Log a parameterized message at trace level.
         * @param logger logger, may not be null.
         * @param pattern pattern, may be null.
         * @param arguments an array of arguments to be
         *          formatted and substituted.
         */
    public static void trace(final Logger logger, final String pattern,
        final Object[] arguments) {
        if (logger.isEnabledFor(TRACE)) {
            forcedLog(logger, TRACE, format(pattern, arguments));
        }
    }

    /**
     * Log a parameterized message at debug level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param arguments an array of arguments to be formatted and substituted.
     */
    public static void debug(final Logger logger, final String pattern,
        final Object[] arguments) {
        if (logger.isDebugEnabled()) {
            forcedLog(logger, Level.DEBUG, format(pattern, arguments));
        }
    }

    /**
     * Log a parameterized message at info level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param arguments an array of arguments to be formatted and substituted.
     */
    public static void info(final Logger logger, final String pattern,
        final Object[] arguments) {
        if (logger.isInfoEnabled()) {
            forcedLog(logger, Level.INFO, format(pattern, arguments));
        }
    }

    /**
     * Log a parameterized message at warn level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param arguments an array of arguments to be formatted and substituted.
     */
    public static void warn(final Logger logger, final String pattern,
        final Object[] arguments) {
        if (logger.isEnabledFor(Level.WARN)) {
            forcedLog(logger, Level.WARN, format(pattern, arguments));
        }
    }

    /**
     * Log a parameterized message at error level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param arguments an array of arguments to be formatted and substituted.
     */
    public static void error(final Logger logger, final String pattern,
        final Object[] arguments) {
        if (logger.isEnabledFor(Level.ERROR)) {
            forcedLog(logger, Level.ERROR, format(pattern, arguments));
        }
    }

    /**
     * Log a parameterized message at fatal level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param arguments an array of arguments to be formatted and substituted.
     */
    public static void fatal(final Logger logger, final String pattern,
        final Object[] arguments) {
        if (logger.isEnabledFor(Level.FATAL)) {
            forcedLog(logger, Level.FATAL, format(pattern, arguments));
        }
    }

    /**
         * Log a parameterized message at trace level.
         * @param logger logger, may not be null.
         * @param t throwable, may be null.
         * @param pattern pattern, may be null.
         * @param arguments an array of arguments to be
         *          formatted and substituted.
         */
    public static void trace(final Logger logger,
                             final Throwable t,
                             final String pattern,
        final Object[] arguments) {
        if (logger.isEnabledFor(TRACE)) {
            forcedLog(logger, TRACE, format(pattern, arguments), t);
        }
    }

    /**
     * Log a parameterized message at debug level.
     * @param logger logger, may not be null.
     * @param t throwable, may be null.
     * @param pattern pattern, may be null.
     * @param arguments an array of arguments to be formatted and substituted.
     */
    public static void debug(final Logger logger,
                             final Throwable t,
                             final String pattern,
        final Object[] arguments) {
        if (logger.isDebugEnabled()) {
            forcedLog(logger, Level.DEBUG, format(pattern, arguments), t);
        }
    }

    /**
     * Log a parameterized message at info level.
     * @param logger logger, may not be null.
     * @param t throwable, may be null.
     * @param pattern pattern, may be null.
     * @param arguments an array of arguments to be formatted and substituted.
     */
    public static void info(final Logger logger,
                            final Throwable t,
                            final String pattern,
        final Object[] arguments) {
        if (logger.isInfoEnabled()) {
            forcedLog(logger, Level.INFO, format(pattern, arguments), t);
        }
    }

    /**
     * Log a parameterized message at warn level.
     * @param logger logger, may not be null.
     * @param t throwable, may be null.
     * @param pattern pattern, may be null.
     * @param arguments an array of arguments to be formatted and substituted.
     */
    public static void warn(final Logger logger,
                            final Throwable t,
                            final String pattern,
        final Object[] arguments) {
        if (logger.isEnabledFor(Level.WARN)) {
            forcedLog(logger, Level.WARN, format(pattern, arguments), t);
        }
    }

    /**
     * Log a parameterized message at error level.
     * @param logger logger, may not be null.
     * @param t throwable, may be null.
     * @param pattern pattern, may be null.
     * @param arguments an array of arguments to be formatted and substituted.
     */
    public static void error(final Logger logger,
                             final Throwable t,
                             final String pattern,
        final Object[] arguments) {
        if (logger.isEnabledFor(Level.ERROR)) {
            forcedLog(logger, Level.ERROR, format(pattern, arguments), t);
        }
    }

    /**
     * Log a parameterized message at fatal level.
     * @param logger logger, may not be null.
     * @param t throwable, may be null.
     * @param pattern pattern, may be null.
     * @param arguments an array of arguments to be formatted and substituted.
     */
    public static void fatal(final Logger logger,
                             final Throwable t,
                             final String pattern,
        final Object[] arguments) {
        if (logger.isEnabledFor(Level.FATAL)) {
            forcedLog(logger, Level.FATAL, format(pattern, arguments), t);
        }
    }



    /**
     * Log a parameterized message at trace level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param argument a value to be formatted and substituted.
     */
    public static void trace(final Logger logger, final String pattern,
        final boolean argument) {
        if (logger.isEnabledFor(TRACE)) {
            forcedLog(logger, TRACE, format(pattern, valueOf(argument)));
        }
    }

    /**
     * Log a parameterized message at trace level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param argument a value to be formatted and substituted.
     */
    public static void trace(final Logger logger, final String pattern,
        final char argument) {
        if (logger.isEnabledFor(TRACE)) {
            forcedLog(logger, TRACE, format(pattern, valueOf(argument)));
        }
    }

    /**
     * Log a parameterized message at trace level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param argument a value to be formatted and substituted.
     */
    public static void trace(final Logger logger, final String pattern,
        final byte argument) {
        if (logger.isEnabledFor(TRACE)) {
            forcedLog(logger, TRACE, format(pattern, valueOf(argument)));
        }
    }

    /**
     * Log a parameterized message at trace level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param argument a value to be formatted and substituted.
     */
    public static void trace(final Logger logger, final String pattern,
        final short argument) {
        if (logger.isEnabledFor(TRACE)) {
            forcedLog(logger, TRACE, format(pattern, valueOf(argument)));
        }
    }

    /**
     * Log a parameterized message at trace level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param argument a value to be formatted and substituted.
     */
    public static void trace(final Logger logger, final String pattern,
        final int argument) {
        if (logger.isEnabledFor(TRACE)) {
            forcedLog(logger, TRACE, format(pattern, valueOf(argument)));
        }
    }

    /**
     * Log a parameterized message at trace level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param argument a value to be formatted and substituted.
     */
    public static void trace(final Logger logger, final String pattern,
        final long argument) {
        if (logger.isEnabledFor(TRACE)) {
            forcedLog(logger, TRACE, format(pattern, valueOf(argument)));
        }
    }

    /**
     * Log a parameterized message at trace level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param argument a value to be formatted and substituted.
     */
    public static void trace(final Logger logger, final String pattern,
        final float argument) {
        if (logger.isEnabledFor(TRACE)) {
            forcedLog(logger, TRACE, format(pattern, valueOf(argument)));
        }
    }

    /**
     * Log a parameterized message at trace level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param argument a value to be formatted and substituted.
     */
    public static void trace(final Logger logger, final String pattern,
        final double argument) {
        if (logger.isEnabledFor(TRACE)) {
            forcedLog(logger, TRACE, format(pattern, valueOf(argument)));
        }
    }

    /**
     * Log a parameterized message at trace level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param argument a value to be formatted and substituted.
     */
    public static void trace(final Logger logger, final String pattern,
        final Object argument) {
        if (logger.isEnabledFor(TRACE)) {
            forcedLog(logger, TRACE, format(pattern, argument));
        }
    }

    /**
     * Log a parameterized message at trace level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param arg0 a value to be formatted and substituted.
     * @param arg1 a value to be formatted and substituted.
     */
    public static void trace(final Logger logger, final String pattern,
        final Object arg0, final Object arg1) {
        if (logger.isEnabledFor(TRACE)) {
            forcedLog(logger, TRACE,
                    format(pattern, toArray(arg0, arg1)));
        }
    }

    /**
     * Log a parameterized message at trace level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param arg0 a value to be formatted and substituted.
     * @param arg1 a value to be formatted and substituted.
     * @param arg2 a value to be formatted and substituted.
     */
    public static void trace(final Logger logger, final String pattern,
        final Object arg0, final Object arg1, final Object arg2) {
        if (logger.isEnabledFor(TRACE)) {
            forcedLog(logger, TRACE,
                    format(pattern, toArray(arg0, arg1, arg2)));
        }
    }

    /**
     * Log a parameterized message at trace level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param arg0 a value to be formatted and substituted.
     * @param arg1 a value to be formatted and substituted.
     * @param arg2 a value to be formatted and substituted.
     * @param arg3 a value to be formatted and substituted.
     */
    public static void trace(final Logger logger, final String pattern,
        final Object arg0, final Object arg1, final Object arg2,
        final Object arg3) {
        if (logger.isEnabledFor(TRACE)) {
            forcedLog(logger, TRACE,
                    format(pattern, toArray(arg0, arg1, arg2, arg3)));
        }
    }

    /**
     * Log a parameterized message at debug level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param argument a value to be formatted and substituted.
     */
    public static void debug(final Logger logger, final String pattern,
        final boolean argument) {
        if (logger.isDebugEnabled()) {
            forcedLog(logger, Level.DEBUG, format(pattern, valueOf(argument)));
        }
    }

    /**
     * Log a parameterized message at debug level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param argument a value to be formatted and substituted.
     */
    public static void debug(final Logger logger, final String pattern,
        final char argument) {
        if (logger.isDebugEnabled()) {
            forcedLog(logger, Level.DEBUG, format(pattern, valueOf(argument)));
        }
    }

    /**
     * Log a parameterized message at debug level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param argument a value to be formatted and substituted.
     */
    public static void debug(final Logger logger, final String pattern,
        final byte argument) {
        if (logger.isDebugEnabled()) {
            forcedLog(logger, Level.DEBUG, format(pattern, valueOf(argument)));
        }
    }

    /**
     * Log a parameterized message at debug level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param argument a value to be formatted and substituted.
     */
    public static void debug(final Logger logger, final String pattern,
        final short argument) {
        if (logger.isDebugEnabled()) {
            forcedLog(logger, Level.DEBUG, format(pattern, valueOf(argument)));
        }
    }

    /**
     * Log a parameterized message at debug level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param argument a value to be formatted and substituted.
     */
    public static void debug(final Logger logger, final String pattern,
        final int argument) {
        if (logger.isDebugEnabled()) {
            forcedLog(logger, Level.DEBUG, format(pattern, valueOf(argument)));
        }
    }

    /**
     * Log a parameterized message at debug level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param argument a value to be formatted and substituted.
     */
    public static void debug(final Logger logger, final String pattern,
        final long argument) {
        if (logger.isDebugEnabled()) {
            forcedLog(logger, Level.DEBUG, format(pattern, valueOf(argument)));
        }
    }

    /**
     * Log a parameterized message at debug level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param argument a value to be formatted and substituted.
     */
    public static void debug(final Logger logger, final String pattern,
        final float argument) {
        if (logger.isDebugEnabled()) {
            forcedLog(logger, Level.DEBUG, format(pattern, valueOf(argument)));
        }
    }

    /**
     * Log a parameterized message at debug level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param argument a value to be formatted and substituted.
     */
    public static void debug(final Logger logger, final String pattern,
        final double argument) {
        if (logger.isDebugEnabled()) {
            forcedLog(logger, Level.DEBUG, format(pattern, valueOf(argument)));
        }
    }

    /**
     * Log a parameterized message at debug level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param argument a value to be formatted and substituted.
     */
    public static void debug(final Logger logger, final String pattern,
        final Object argument) {
        if (logger.isDebugEnabled()) {
            forcedLog(logger, Level.DEBUG, format(pattern, argument));
        }
    }

    /**
     * Log a parameterized message at debug level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param arg0 a value to be formatted and substituted.
     * @param arg1 a value to be formatted and substituted.
     */
    public static void debug(final Logger logger, final String pattern,
        final Object arg0, final Object arg1) {
        if (logger.isDebugEnabled()) {
            forcedLog(logger, Level.DEBUG,
                    format(pattern, toArray(arg0, arg1)));
        }
    }

    /**
     * Log a parameterized message at debug level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param arg0 a value to be formatted and substituted.
     * @param arg1 a value to be formatted and substituted.
     * @param arg2 a value to be formatted and substituted.
     */
    public static void debug(final Logger logger, final String pattern,
        final Object arg0, final Object arg1, final Object arg2) {
        if (logger.isDebugEnabled()) {
            forcedLog(logger, Level.DEBUG,
                    format(pattern, toArray(arg0, arg1, arg2)));
        }
    }

    /**
     * Log a parameterized message at debug level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param arg0 a value to be formatted and substituted.
     * @param arg1 a value to be formatted and substituted.
     * @param arg2 a value to be formatted and substituted.
     * @param arg3 a value to be formatted and substituted.
     */
    public static void debug(final Logger logger, final String pattern,
        final Object arg0, final Object arg1, final Object arg2,
        final Object arg3) {
        if (logger.isDebugEnabled()) {
            forcedLog(logger, Level.DEBUG,
                    format(pattern, toArray(arg0, arg1, arg2, arg3)));
        }
    }

    /**
     * Log a parameterized message at info level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param argument a value to be formatted and substituted.
     */
    public static void info(final Logger logger, final String pattern,
        final boolean argument) {
        if (logger.isInfoEnabled()) {
            forcedLog(logger, Level.INFO, format(pattern, valueOf(argument)));
        }
    }

    /**
     * Log a parameterized message at info level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param argument a value to be formatted and substituted.
     */
    public static void info(final Logger logger, final String pattern,
        final char argument) {
        if (logger.isInfoEnabled()) {
            forcedLog(logger, Level.INFO, format(pattern, valueOf(argument)));
        }
    }

    /**
     * Log a parameterized message at info level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param argument a value to be formatted and substituted.
     */
    public static void info(final Logger logger, final String pattern,
        final byte argument) {
        if (logger.isInfoEnabled()) {
            forcedLog(logger, Level.INFO, format(pattern, valueOf(argument)));
        }
    }

    /**
     * Log a parameterized message at info level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param argument a value to be formatted and substituted.
     */
    public static void info(final Logger logger, final String pattern,
        final short argument) {
        if (logger.isInfoEnabled()) {
            forcedLog(logger, Level.INFO, format(pattern, valueOf(argument)));
        }
    }

    /**
     * Log a parameterized message at info level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param argument a value to be formatted and substituted.
     */
    public static void info(final Logger logger, final String pattern,
        final int argument) {
        if (logger.isInfoEnabled()) {
            forcedLog(logger, Level.INFO, format(pattern, valueOf(argument)));
        }
    }

    /**
     * Log a parameterized message at info level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param argument a value to be formatted and substituted.
     */
    public static void info(final Logger logger, final String pattern,
        final long argument) {
        if (logger.isInfoEnabled()) {
            forcedLog(logger, Level.INFO, format(pattern, valueOf(argument)));
        }
    }

    /**
     * Log a parameterized message at info level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param argument a value to be formatted and substituted.
     */
    public static void info(final Logger logger, final String pattern,
        final float argument) {
        if (logger.isInfoEnabled()) {
            forcedLog(logger, Level.INFO, format(pattern, valueOf(argument)));
        }
    }

    /**
     * Log a parameterized message at info level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param argument a value to be formatted and substituted.
     */
    public static void info(final Logger logger, final String pattern,
        final double argument) {
        if (logger.isInfoEnabled()) {
            forcedLog(logger, Level.INFO, format(pattern, valueOf(argument)));
        }
    }

    /**
     * Log a parameterized message at info level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param argument a value to be formatted and substituted.
     */
    public static void info(final Logger logger, final String pattern,
        final Object argument) {
        if (logger.isInfoEnabled()) {
            forcedLog(logger, Level.INFO, format(pattern, argument));
        }
    }

    /**
     * Log a parameterized message at info level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param arg0 a value to be formatted and substituted.
     * @param arg1 a value to be formatted and substituted.
     */
    public static void info(final Logger logger, final String pattern,
        final Object arg0, final Object arg1) {
        if (logger.isInfoEnabled()) {
            forcedLog(logger, Level.INFO, format(pattern, toArray(arg0, arg1)));
        }
    }

    /**
     * Log a parameterized message at info level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param arg0 a value to be formatted and substituted.
     * @param arg1 a value to be formatted and substituted.
     * @param arg2 a value to be formatted and substituted.
     */
    public static void info(final Logger logger, final String pattern,
        final Object arg0, final Object arg1, final Object arg2) {
        if (logger.isInfoEnabled()) {
            forcedLog(logger, Level.INFO, format(pattern,
                    toArray(arg0, arg1, arg2)));
        }
    }

    /**
     * Log a parameterized message at info level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param arg0 a value to be formatted and substituted.
     * @param arg1 a value to be formatted and substituted.
     * @param arg2 a value to be formatted and substituted.
     * @param arg3 a value to be formatted and substituted.
     */
    public static void info(final Logger logger, final String pattern,
        final Object arg0, final Object arg1, final Object arg2,
        final Object arg3) {
        if (logger.isInfoEnabled()) {
            forcedLog(logger, Level.INFO, format(pattern,
                    toArray(arg0, arg1, arg2, arg3)));
        }
    }

    /**
     * Log a parameterized message at warn level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param argument a value to be formatted and substituted.
     */
    public static void warn(final Logger logger, final String pattern,
        final boolean argument) {
        if (logger.isEnabledFor(Level.WARN)) {
            forcedLog(logger, Level.WARN, format(pattern, valueOf(argument)));
        }
    }

    /**
     * Log a parameterized message at warn level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param argument a value to be formatted and substituted.
     */
    public static void warn(final Logger logger, final String pattern,
        final char argument) {
        if (logger.isEnabledFor(Level.WARN)) {
            forcedLog(logger, Level.WARN, format(pattern, valueOf(argument)));
        }
    }

    /**
     * Log a parameterized message at warn level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param argument a value to be formatted and substituted.
     */
    public static void warn(final Logger logger, final String pattern,
        final byte argument) {
        if (logger.isEnabledFor(Level.WARN)) {
            forcedLog(logger, Level.WARN, format(pattern, valueOf(argument)));
        }
    }

    /**
     * Log a parameterized message at warn level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param argument a value to be formatted and substituted.
     */
    public static void warn(final Logger logger, final String pattern,
        final short argument) {
        if (logger.isEnabledFor(Level.WARN)) {
            forcedLog(logger, Level.WARN, format(pattern, valueOf(argument)));
        }
    }

    /**
     * Log a parameterized message at warn level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param argument a value to be formatted and substituted.
     */
    public static void warn(final Logger logger, final String pattern,
        final int argument) {
        if (logger.isEnabledFor(Level.WARN)) {
            forcedLog(logger, Level.WARN, format(pattern, valueOf(argument)));
        }
    }

    /**
     * Log a parameterized message at warn level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param argument a value to be formatted and substituted.
     */
    public static void warn(final Logger logger, final String pattern,
        final long argument) {
        if (logger.isEnabledFor(Level.WARN)) {
            forcedLog(logger, Level.WARN, format(pattern, valueOf(argument)));
        }
    }

    /**
     * Log a parameterized message at warn level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param argument a value to be formatted and substituted.
     */
    public static void warn(final Logger logger, final String pattern,
        final float argument) {
        if (logger.isEnabledFor(Level.WARN)) {
            forcedLog(logger, Level.WARN, format(pattern, valueOf(argument)));
        }
    }

    /**
     * Log a parameterized message at warn level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param argument a value to be formatted and substituted.
     */
    public static void warn(final Logger logger, final String pattern,
        final double argument) {
        if (logger.isEnabledFor(Level.WARN)) {
            forcedLog(logger, Level.WARN, format(pattern, valueOf(argument)));
        }
    }

    /**
     * Log a parameterized message at warn level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param argument a value to be formatted and substituted.
     */
    public static void warn(final Logger logger, final String pattern,
        final Object argument) {
        if (logger.isEnabledFor(Level.WARN)) {
            forcedLog(logger, Level.WARN, format(pattern, argument));
        }
    }

    /**
     * Log a parameterized message at warn level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param arg0 a value to be formatted and substituted.
     * @param arg1 a value to be formatted and substituted.
     */
    public static void warn(final Logger logger, final String pattern,
        final Object arg0, final Object arg1) {
        if (logger.isEnabledFor(Level.WARN)) {
            forcedLog(logger, Level.WARN,
                    format(pattern, toArray(arg0, arg1)));
        }
    }

    /**
     * Log a parameterized message at warn level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param arg0 a value to be formatted and substituted.
     * @param arg1 a value to be formatted and substituted.
     * @param arg2 a value to be formatted and substituted.
     */
    public static void warn(final Logger logger, final String pattern,
        final Object arg0, final Object arg1, final Object arg2) {
        if (logger.isEnabledFor(Level.WARN)) {
            forcedLog(logger, Level.WARN,
                    format(pattern, toArray(arg0, arg1, arg2)));
        }
    }

    /**
     * Log a parameterized message at warn level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param arg0 a value to be formatted and substituted.
     * @param arg1 a value to be formatted and substituted.
     * @param arg2 a value to be formatted and substituted.
     * @param arg3 a value to be formatted and substituted.
     */
    public static void warn(final Logger logger, final String pattern,
        final Object arg0, final Object arg1, final Object arg2,
        final Object arg3) {
        if (logger.isEnabledFor(Level.WARN)) {
            forcedLog(logger, Level.WARN, format(pattern,
                    toArray(arg0, arg1, arg2, arg3)));
        }
    }

    /**
      * Log a parameterized message at specified level.
      * @param logger logger, may not be null.
      * @param level level, may not be null.
      * @param pattern pattern, may be null.
     * @param parameters parameters to the log message.
      */
    public static void log(final Logger logger,
                             final Level level,
                             final String pattern,
                             final Object[] parameters) {
        if (logger.isEnabledFor(level)) {
            forcedLog(logger, level,
                    format(pattern, parameters));
        }
    }

    /**
      * Log a parameterized message at specified level.
      * @param logger logger, may not be null.
      * @param level level, may not be null.
     * @param t throwable, may be null.
      * @param pattern pattern, may be null.
     * @param parameters parameters to the log message.
      */
    public static void log(final Logger logger,
                             final Level level,
                             final Throwable t,
                             final String pattern,
                             final Object[] parameters) {
        if (logger.isEnabledFor(level)) {
            forcedLog(logger, level,
                    format(pattern, parameters), t);
        }
    }

    /**
      * Log a parameterized message at specified level.
      * @param logger logger, may not be null.
      * @param level level, may not be null.
      * @param pattern pattern, may be null.
     * @param param1 parameter to the log message.
      */
    public static void log(final Logger logger,
                             final Level level,
                             final String pattern,
                             final Object param1) {
        if (logger.isEnabledFor(level)) {
            forcedLog(logger, level,
                    format(pattern, toArray(param1)));
        }
    }

    /**
      * Log a parameterized message at specified level.
      * @param logger logger, may not be null.
      * @param level level, may not be null.
      * @param pattern pattern, may be null.
     * @param param1 parameter to the log message.
      */
    public static void log(final Logger logger,
                             final Level level,
                             final String pattern,
                             final boolean param1) {
        if (logger.isEnabledFor(level)) {
            forcedLog(logger, level,
                    format(pattern, toArray(valueOf(param1))));
        }
    }


    /**
      * Log a parameterized message at specified level.
      * @param logger logger, may not be null.
      * @param level level, may not be null.
      * @param pattern pattern, may be null.
     * @param param1 parameter to the log message.
      */
    public static void log(final Logger logger,
                             final Level level,
                             final String pattern,
                             final byte param1) {
        if (logger.isEnabledFor(level)) {
            forcedLog(logger, level,
                    format(pattern, toArray(valueOf(param1))));
        }
    }


    /**
      * Log a parameterized message at specified level.
      * @param logger logger, may not be null.
      * @param level level, may not be null.
      * @param pattern pattern, may be null.
     * @param param1 parameter to the log message.
      */
    public static void log(final Logger logger,
                             final Level level,
                             final String pattern,
                             final char param1) {
        if (logger.isEnabledFor(level)) {
            forcedLog(logger, level,
                    format(pattern, toArray(valueOf(param1))));
        }
    }

    /**
      * Log a parameterized message at specified level.
      * @param logger logger, may not be null.
      * @param level level, may not be null.
      * @param pattern pattern, may be null.
     * @param param1 parameter to the log message.
      */
    public static void log(final Logger logger,
                             final Level level,
                             final String pattern,
                             final short param1) {
        if (logger.isEnabledFor(level)) {
            forcedLog(logger, level,
                    format(pattern, toArray(valueOf(param1))));
        }
    }

    /**
      * Log a parameterized message at specified level.
      * @param logger logger, may not be null.
      * @param level level, may not be null.
      * @param pattern pattern, may be null.
     * @param param1 parameter to the log message.
      */
    public static void log(final Logger logger,
                             final Level level,
                             final String pattern,
                             final int param1) {
        if (logger.isEnabledFor(level)) {
            forcedLog(logger, level,
                    format(pattern, toArray(valueOf(param1))));
        }
    }


    /**
      * Log a parameterized message at specified level.
      * @param logger logger, may not be null.
      * @param level level, may not be null.
      * @param pattern pattern, may be null.
     * @param param1 parameter to the log message.
      */
    public static void log(final Logger logger,
                             final Level level,
                             final String pattern,
                             final long param1) {
        if (logger.isEnabledFor(level)) {
            forcedLog(logger, level,
                    format(pattern, toArray(valueOf(param1))));
        }
    }


    /**
      * Log a parameterized message at specified level.
      * @param logger logger, may not be null.
      * @param level level, may not be null.
      * @param pattern pattern, may be null.
     * @param param1 parameter to the log message.
      */
    public static void log(final Logger logger,
                             final Level level,
                             final String pattern,
                             final float param1) {
        if (logger.isEnabledFor(level)) {
            forcedLog(logger, level,
                    format(pattern, toArray(valueOf(param1))));
        }
    }


    /**
      * Log a parameterized message at specified level.
      * @param logger logger, may not be null.
      * @param level level, may not be null.
      * @param pattern pattern, may be null.
     * @param param1 parameter to the log message.
      */
    public static void log(final Logger logger,
                             final Level level,
                             final String pattern,
                             final double param1) {
        if (logger.isEnabledFor(level)) {
            forcedLog(logger, level,
                    format(pattern, toArray(valueOf(param1))));
        }
    }


    /**
     * Log a parameterized message at specified level.
     * @param logger logger, may not be null.
     * @param level level, may not be null.
     * @param pattern pattern, may be null.
     * @param arg0 a value to be formatted and substituted.
     * @param arg1 a value to be formatted and substituted.
     */
    public static void log(final Logger logger,
                            final Level level,
                            final String pattern,
        final Object arg0, final Object arg1) {
        if (logger.isEnabledFor(level)) {
            forcedLog(logger, level,
                    format(pattern, toArray(arg0, arg1)));
        }
    }

    /**
     * Log a parameterized message at specifed level.
     * @param logger logger, may not be null.
     * @param level level, may not be null.
     * @param pattern pattern, may be null.
     * @param arg0 a value to be formatted and substituted.
     * @param arg1 a value to be formatted and substituted.
     * @param arg2 a value to be formatted and substituted.
     */
    public static void log(final Logger logger,
                           final Level level,
                           final String pattern,
        final Object arg0, final Object arg1, final Object arg2) {
        if (logger.isEnabledFor(level)) {
            forcedLog(logger, level,
                    format(pattern, toArray(arg0, arg1, arg2)));
        }
    }

    /**
     * Log a parameterized message at specified level.
     * @param logger logger, may not be null.
     * @param pattern pattern, may be null.
     * @param level level, may not be null.
     * @param arg0 a value to be formatted and substituted.
     * @param arg1 a value to be formatted and substituted.
     * @param arg2 a value to be formatted and substituted.
     * @param arg3 a value to be formatted and substituted.
     */
    public static void log(final Logger logger,
                           final Level level,
                           final String pattern,
        final Object arg0, final Object arg1, final Object arg2,
        final Object arg3) {
        if (logger.isEnabledFor(level)) {
            forcedLog(logger, level, format(pattern,
                    toArray(arg0, arg1, arg2, arg3)));
        }
    }


    /**
      * Log a parameterized message using a pattern from a resource bundle.
      * @param logger logger, may not be null.
      * @param level level, may not be null.
      * @param bundleName resource bundle name, may be null.
     * @param key key, may be null.
     * @param parameters parameters to the log message.
      */
    public static void logrb(final Logger logger,
                             final Level level,
                             final String bundleName,
                             final String key,
                             final Object[] parameters) {
        if (logger.isEnabledFor(level)) {
            forcedLog(logger, level,
                    format(bundleName, key, parameters));
        }
    }

    /**
      * Log a parameterized message using a pattern from a resource bundle.
      * @param logger logger, may not be null.
      * @param level level, may not be null.
     * @param t throwable, may be null.
      * @param bundleName resource bundle name, may be null.
     * @param key key, may be null.
     * @param parameters parameters to the log message.
      */
    public static void logrb(final Logger logger,
                             final Level level,
                             final Throwable t,
                             final String bundleName,
                             final String key,
                             final Object[] parameters) {
        if (logger.isEnabledFor(level)) {
            forcedLog(logger, level,
                    format(bundleName, key, parameters), t);
        }
    }

    /**
      * Log a parameterized message using a pattern from a resource bundle.
      * @param logger logger, may not be null.
      * @param level level, may not be null.
      * @param bundleName resource bundle name, may be null.
     * @param key key, may be null.
     * @param param1 Parameter to the log message.
      */
    public static void logrb(final Logger logger,
                             final Level level,
                             final String bundleName,
                             final String key,
                             final Object param1) {
        if (logger.isEnabledFor(level)) {
            forcedLog(logger, level,
                    format(bundleName, key, toArray(param1)));
        }
    }

    /**
      * Log a parameterized message using a pattern from a resource bundle.
      * @param logger logger, may not be null.
      * @param level level, may not be null.
      * @param bundleName resource bundle name, may be null.
     * @param key key, may be null.
     * @param param1 Parameter to the log message.
      */
    public static void logrb(final Logger logger,
                             final Level level,
                             final String bundleName,
                             final String key,
                             final boolean param1) {
        if (logger.isEnabledFor(level)) {
            forcedLog(logger, level,
                    format(bundleName, key, toArray(valueOf(param1))));
        }
    }

    /**
      * Log a parameterized message using a pattern from a resource bundle.
      * @param logger logger, may not be null.
      * @param level level, may not be null.
      * @param bundleName resource bundle name, may be null.
     * @param key key, may be null.
     * @param param1 Parameter to the log message.
      */
    public static void logrb(final Logger logger,
                             final Level level,
                             final String bundleName,
                             final String key,
                             final char param1) {
        if (logger.isEnabledFor(level)) {
            forcedLog(logger, level,
                    format(bundleName, key, toArray(valueOf(param1))));
        }
    }

    /**
      * Log a parameterized message using a pattern from a resource bundle.
      * @param logger logger, may not be null.
      * @param level level, may not be null.
      * @param bundleName resource bundle name, may be null.
     * @param key key, may be null.
     * @param param1 Parameter to the log message.
      */
    public static void logrb(final Logger logger,
                             final Level level,
                             final String bundleName,
                             final String key,
                             final byte param1) {
        if (logger.isEnabledFor(level)) {
            forcedLog(logger, level,
                    format(bundleName, key, toArray(valueOf(param1))));
        }
    }

    /**
      * Log a parameterized message using a pattern from a resource bundle.
      * @param logger logger, may not be null.
      * @param level level, may not be null.
      * @param bundleName resource bundle name, may be null.
     * @param key key, may be null.
     * @param param1 Parameter to the log message.
      */
    public static void logrb(final Logger logger,
                             final Level level,
                             final String bundleName,
                             final String key,
                             final short param1) {
        if (logger.isEnabledFor(level)) {
            forcedLog(logger, level,
                    format(bundleName, key, toArray(valueOf(param1))));
        }
    }

    /**
      * Log a parameterized message using a pattern from a resource bundle.
      * @param logger logger, may not be null.
      * @param level level, may not be null.
      * @param bundleName resource bundle name, may be null.
     * @param key key, may be null.
     * @param param1 Parameter to the log message.
      */
    public static void logrb(final Logger logger,
                             final Level level,
                             final String bundleName,
                             final String key,
                             final int param1) {
        if (logger.isEnabledFor(level)) {
            forcedLog(logger, level,
                    format(bundleName, key, toArray(valueOf(param1))));
        }
    }

    /**
      * Log a parameterized message using a pattern from a resource bundle.
      * @param logger logger, may not be null.
      * @param level level, may not be null.
      * @param bundleName resource bundle name, may be null.
     * @param key key, may be null.
     * @param param1 Parameter to the log message.
      */
    public static void logrb(final Logger logger,
                             final Level level,
                             final String bundleName,
                             final String key,
                             final long param1) {
        if (logger.isEnabledFor(level)) {
            forcedLog(logger, level,
                    format(bundleName, key, toArray(valueOf(param1))));
        }
    }
    /**
      * Log a parameterized message using a pattern from a resource bundle.
      * @param logger logger, may not be null.
      * @param level level, may not be null.
      * @param bundleName resource bundle name, may be null.
     * @param key key, may be null.
     * @param param1 Parameter to the log message.
      */
    public static void logrb(final Logger logger,
                             final Level level,
                             final String bundleName,
                             final String key,
                             final float param1) {
        if (logger.isEnabledFor(level)) {
            forcedLog(logger, level,
                    format(bundleName, key, toArray(valueOf(param1))));
        }
    }


    /**
      * Log a parameterized message using a pattern from a resource bundle.
      * @param logger logger, may not be null.
      * @param level level, may not be null.
      * @param bundleName resource bundle name, may be null.
     * @param key key, may be null.
     * @param param1 Parameter to the log message.
      */
    public static void logrb(final Logger logger,
                             final Level level,
                             final String bundleName,
                             final String key,
                             final double param1) {
        if (logger.isEnabledFor(level)) {
            forcedLog(logger, level,
                    format(bundleName, key, toArray(valueOf(param1))));
        }
    }

    /**
      * Log a parameterized message using a pattern from a resource bundle.
      * @param logger logger, may not be null.
      * @param level level, may not be null.
      * @param bundleName resource bundle name, may be null.
     * @param key key, may be null.
     * @param param0 Parameter to the log message.
     * @param param1 Parameter to the log message.
      */
    public static void logrb(final Logger logger,
                             final Level level,
                             final String bundleName,
                             final String key,
                             final Object param0,
                             final Object param1) {
        if (logger.isEnabledFor(level)) {
            forcedLog(logger, level,
                    format(bundleName, key, toArray(param0, param1)));
        }
    }


    /**
      * Log a parameterized message using a pattern from a resource bundle.
      * @param logger logger, may not be null.
      * @param level level, may not be null.
      * @param bundleName resource bundle name, may be null.
     * @param key key, may be null.
     * @param param0 Parameter to the log message.
     * @param param1 Parameter to the log message.
     * @param param2 Parameter to the log message.
      */
    public static void logrb(final Logger logger,
                             final Level level,
                             final String bundleName,
                             final String key,
                             final Object param0,
                             final Object param1,
                             final Object param2) {
        if (logger.isEnabledFor(level)) {
            forcedLog(logger, level,
                    format(bundleName, key, toArray(param0, param1, param2)));
        }
    }


    /**
      * Log a parameterized message using a pattern from a resource bundle.
      * @param logger logger, may not be null.
      * @param level level, may not be null.
      * @param bundleName resource bundle name, may be null.
     * @param key key, may be null.
     * @param param0 Parameter to the log message.
     * @param param1 Parameter to the log message.
     * @param param2 Parameter to the log message.
     * @param param3 Parameter to the log message.
      */
    public static void logrb(final Logger logger,
                             final Level level,
                             final String bundleName,
                             final String key,
                             final Object param0,
                             final Object param1,
                             final Object param2,
                             final Object param3) {
        if (logger.isEnabledFor(level)) {
            forcedLog(logger, level,
                    format(bundleName, key,
                            toArray(param0, param1, param2, param3)));
        }
    }
}
