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
import java.util.Locale;


/**
 * This class provides parameterized logging services
 * using the pattern syntax of java.text.MessageFormat.
 * Message formatting is only performed when the 
 * request exceeds the threshold level of the logger.
 * When the pattern only contains literal text and
 * default conversion patterns (that is "{0}" and similar)
 * a simple fast compatible formatter is used.  
 * If the pattern contains more complex conversion patterns,
 * formatting will be delegated to java.text.MessageFormatter
 * which can be substantially slower.
 *
 * @see org.apache.log4j.LogSF
 * @since 1.2.16
 *
 */
public final class LogMF extends LogXF {
    /**
     * private constructor.
     *
     */
    private LogMF() {
    }

    /**
     * Number format.
     */
    private static NumberFormat numberFormat = null;
    /**
     * Locale at time of last number format request.
     */
    private static Locale numberLocale = null;
    /**
     * Date format.
     */
    private static DateFormat dateFormat = null;
    /**
     * Locale at time of last date format request.
     */
    private static Locale dateLocale = null;

    /**
     * Format number.
     * @param n number to format, may not be null.
     * @return formatted value.
     */
    private static synchronized String formatNumber(final Object n) {
        Locale currentLocale = Locale.getDefault();
        if (currentLocale != numberLocale || numberFormat == null) {
            numberLocale = currentLocale;
            numberFormat = NumberFormat.getInstance(currentLocale);
        }
        return numberFormat.format(n);
    }


    /**
     * Format date.
     * @param d date, may not be null.
     * @return formatted value.
     */
    private static synchronized String formatDate(final Object d) {
        Locale currentLocale = Locale.getDefault();
        if (currentLocale != dateLocale || dateFormat == null) {
            dateLocale = currentLocale;
            dateFormat = DateFormat.getDateTimeInstance(
                                DateFormat.SHORT,
                                DateFormat.SHORT,
                                currentLocale);
        }
        return dateFormat.format(d);
    }

    /**
     * Format a single parameter like a "{0}" formatting specifier.
     *
     * @param arg0 parameter, may be null.
     * @return string representation of arg0.
     */
    private static String formatObject(final Object arg0) {
        if (arg0 instanceof String) {
            return arg0.toString();
        } else if (arg0 instanceof Double ||
                   arg0 instanceof Float) {
           return formatNumber(arg0);
        } else if (arg0 instanceof Date) {
            return formatDate(arg0);
        }
        return String.valueOf(arg0);
    }


    /**
     * Determines if pattern contains only {n} format elements
     * and not apostrophes.
     *
     * @param pattern pattern, may not be null.
     * @return true if pattern only contains {n} format elements.
     */
    private static boolean isSimple(final String pattern) {
        if (pattern.indexOf('\'') != -1) {
            return false;
        }
        for(int pos = pattern.indexOf('{');
            pos != -1;
            pos = pattern.indexOf('{', pos + 1)) {
            if (pos + 2 >= pattern.length() ||
                    pattern.charAt(pos+2) != '}' ||
                    pattern.charAt(pos+1) < '0' ||
                    pattern.charAt(pos+1) > '9') {
                return false;
            }
        }
        return true;

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
        } else if(isSimple(pattern)) {
            String formatted[] = new String[10];
            int prev = 0;
            String retval = "";
            int pos = pattern.indexOf('{');
            while(pos >= 0) {
                if(pos + 2 < pattern.length() && 
                      pattern.charAt(pos+2) == '}' &&
                      pattern.charAt(pos+1) >= '0' &&
                      pattern.charAt(pos+1) <= '9') {
                    int index = pattern.charAt(pos+1) - '0';
                    retval += pattern.substring(prev, pos);
                    if (formatted[index] == null) {
                         if (arguments == null || index >= arguments.length) {
                            formatted[index] = pattern.substring(pos, pos+3);
                         } else {
                            formatted[index] = formatObject(arguments[index]);
                         }
                    }
                    retval += formatted[index];
                    prev = pos + 3;
                    pos = pattern.indexOf('{', prev);
                } else {
                    pos = pattern.indexOf('{', pos + 1);
                }
            }
            retval += pattern.substring(prev);
            return retval;
        }
        try {
            return MessageFormat.format(pattern, arguments);
        } catch (IllegalArgumentException ex) {
            return pattern;
        }
    }

    /**
     * Formats a single argument using MessageFormat.
     * @param pattern pattern, may be malformed or null.
     * @param arguments arguments, may be null or mismatched.
     * @return Message string or null
     */
    private static String format(final String pattern,
                                 final Object arg0) {
        if (pattern == null) {
            return null;
        } else if(isSimple(pattern)) {
            String formatted = null;
            int prev = 0;
            String retval = "";
            int pos = pattern.indexOf('{');
            while(pos >= 0) {
                if(pos + 2 < pattern.length() &&
                      pattern.charAt(pos+2) == '}' &&
                      pattern.charAt(pos+1) >= '0' &&
                      pattern.charAt(pos+1) <= '9') {
                    int index = pattern.charAt(pos+1) - '0';
                    retval += pattern.substring(prev, pos);
                    if (index != 0) {
                        retval += pattern.substring(pos, pos+3);
                    } else {
                        if (formatted == null) {
                            formatted = formatObject(arg0);
                        }
                        retval += formatted;
                    }
                    prev = pos + 3;
                    pos = pattern.indexOf('{', prev);
                } else {
                    pos = pattern.indexOf('{', pos + 1);
                }
            }
            retval += pattern.substring(prev);
            return retval;
        }
        try {
            return MessageFormat.format(pattern, new Object[] { arg0 });
        } catch (IllegalArgumentException ex) {
            return pattern;
        }
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
