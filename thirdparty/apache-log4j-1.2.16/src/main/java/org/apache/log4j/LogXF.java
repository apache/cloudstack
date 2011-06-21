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

/**
 * This is a base class for LogMF and LogSF parameterized logging classes.
 *
 *
 * @see org.apache.log4j.LogMF
 * @see org.apache.log4j.LogSF
 * @since 1.2.16
 */
public abstract class LogXF {
    /**
     * Trace level.
     */
    protected static final Level TRACE = new Level(5000, "TRACE", 7);
    /**
     * Fully Qualified Class Name of this class.
     */
    private static final String FQCN = LogXF.class.getName();

    protected LogXF() {
    }

    /**
     * Returns a Boolean instance representing the specified boolean.
     * Boolean.valueOf was added in JDK 1.4.
     *
     * @param b a boolean value.
     * @return a Boolean instance representing b.
     */
    protected static Boolean valueOf(final boolean b) {
        if (b) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    /**
     * Returns a Character instance representing the specified char.
     * Character.valueOf was added in JDK 1.5.
     *
     * @param c a character value.
     * @return a Character instance representing c.
     */
    protected static Character valueOf(final char c) {
        return new Character(c);
    }

    /**
     * Returns a Byte instance representing the specified byte.
     * Byte.valueOf was added in JDK 1.5.
     *
     * @param b a byte value.
     * @return a Byte instance representing b.
     */
    protected static Byte valueOf(final byte b) {
        return new Byte(b);
    }

    /**
     * Returns a Short instance representing the specified short.
     * Short.valueOf was added in JDK 1.5.
     *
     * @param b a short value.
     * @return a Byte instance representing b.
     */
    protected static Short valueOf(final short b) {
        return new Short(b);
    }

    /**
     * Returns an Integer instance representing the specified int.
     * Integer.valueOf was added in JDK 1.5.
     *
     * @param b an int value.
     * @return an Integer instance representing b.
     */
    protected static Integer valueOf(final int b) {
        return new Integer(b);
    }

    /**
     * Returns a Long instance representing the specified long.
     * Long.valueOf was added in JDK 1.5.
     *
     * @param b a long value.
     * @return a Long instance representing b.
     */
    protected static Long valueOf(final long b) {
        return new Long(b);
    }

    /**
     * Returns a Float instance representing the specified float.
     * Float.valueOf was added in JDK 1.5.
     *
     * @param b a float value.
     * @return a Float instance representing b.
     */
    protected static Float valueOf(final float b) {
        return new Float(b);
    }

    /**
     * Returns a Double instance representing the specified double.
     * Double.valueOf was added in JDK 1.5.
     *
     * @param b a double value.
     * @return a Byte instance representing b.
     */
    protected static Double valueOf(final double b) {
        return new Double(b);
    }

    /**
     * Create new array.
     *
     * @param param1 parameter 1.
     * @return new array.
     */
    protected static Object[] toArray(final Object param1) {
        return new Object[]{
                param1
        };
    }

    /**
     * Create new array.
     *
     * @param param1 parameter 1.
     * @param param2 parameter 2.
     * @return new array.
     */
    protected static Object[] toArray(final Object param1,
                                      final Object param2) {
        return new Object[]{
                param1, param2
        };
    }

    /**
     * Create new array.
     *
     * @param param1 parameter 1.
     * @param param2 parameter 2.
     * @param param3 parameter 3.
     * @return new array.
     */
    protected static Object[] toArray(final Object param1,
                                      final Object param2,
                                      final Object param3) {
        return new Object[]{
                param1, param2, param3
        };
    }

    /**
     * Create new array.
     *
     * @param param1 parameter 1.
     * @param param2 parameter 2.
     * @param param3 parameter 3.
     * @param param4 parameter 4.
     * @return new array.
     */
    protected static Object[] toArray(final Object param1,
                                      final Object param2,
                                      final Object param3,
                                      final Object param4) {
        return new Object[]{
                param1, param2, param3, param4
        };
    }

    /**
     * Log an entering message at DEBUG level.
     *
     * @param logger       logger, may not be null.
     * @param sourceClass  source class, may be null.
     * @param sourceMethod method, may be null.
     */
    public static void entering(final Logger logger,
                                final String sourceClass,
                                final String sourceMethod) {
        if (logger.isDebugEnabled()) {
            logger.callAppenders(new LoggingEvent(FQCN, logger, Level.DEBUG,
                    sourceClass + "." + sourceMethod + " ENTRY", null));
        }
    }

    /**
     * Log an entering message with a parameter at DEBUG level.
     *
     * @param logger       logger, may not be null.
     * @param sourceClass  source class, may be null.
     * @param sourceMethod method, may be null.
     * @param param        parameter, may be null.
     */
    public static void entering(final Logger logger,
                                final String sourceClass,
                                final String sourceMethod,
                                final String param) {
        if (logger.isDebugEnabled()) {
            String msg = sourceClass + "." + sourceMethod + " ENTRY " + param;
            logger.callAppenders(new LoggingEvent(FQCN, logger, Level.DEBUG,
                    msg, null));
        }
    }

    /**
     * Log an entering message with a parameter at DEBUG level.
     *
     * @param logger       logger, may not be null.
     * @param sourceClass  source class, may be null.
     * @param sourceMethod method, may be null.
     * @param param        parameter, may be null.
     */
    public static void entering(final Logger logger,
                                final String sourceClass,
                                final String sourceMethod,
                                final Object param) {
        if (logger.isDebugEnabled()) {
            String msg = sourceClass + "." + sourceMethod + " ENTRY ";
            if (param == null) {
                msg += "null";
            } else {
                try {
                    msg += param;
                } catch(Throwable ex) {
                    msg += "?";
                }
            }
            logger.callAppenders(new LoggingEvent(FQCN, logger, Level.DEBUG,
                    msg, null));
        }
    }

    /**
     * Log an entering message with an array of parameters at DEBUG level.
     *
     * @param logger       logger, may not be null.
     * @param sourceClass  source class, may be null.
     * @param sourceMethod method, may be null.
     * @param params       parameters, may be null.
     */
    public static void entering(final Logger logger,
                                final String sourceClass,
                                final String sourceMethod,
                                final Object[] params) {
        if (logger.isDebugEnabled()) {
            String msg = sourceClass + "." + sourceMethod + " ENTRY ";
            if (params != null && params.length > 0) {
                String delim = "{";
                for (int i = 0; i < params.length; i++) {
                    try {
                        msg += delim + params[i];
                    } catch(Throwable ex) {
                        msg += delim + "?";
                    }
                    delim = ",";
                }
                msg += "}";
            } else {
                msg += "{}";
            }
            logger.callAppenders(new LoggingEvent(FQCN, logger, Level.DEBUG,
                    msg, null));
        }
    }

    /**
     * Log an exiting message at DEBUG level.
     *
     * @param logger       logger, may not be null.
     * @param sourceClass  source class, may be null.
     * @param sourceMethod method, may be null.
     */
    public static void exiting(final Logger logger,
                               final String sourceClass,
                               final String sourceMethod) {
        if (logger.isDebugEnabled()) {
            logger.callAppenders(new LoggingEvent(FQCN, logger, Level.DEBUG,
                    sourceClass + "." + sourceMethod + " RETURN", null));
        }
    }

    /**
     * Log an exiting message with result at DEBUG level.
     *
     * @param logger       logger, may not be null.
     * @param sourceClass  source class, may be null.
     * @param sourceMethod method, may be null.
     * @param result       result, may be null.
     */
    public static void exiting(
            final Logger logger,
            final String sourceClass,
            final String sourceMethod,
            final String result) {
        if (logger.isDebugEnabled()) {
            logger.callAppenders(new LoggingEvent(FQCN, logger, Level.DEBUG,
                    sourceClass + "." + sourceMethod + " RETURN " + result, null));
        }
    }

    /**
     * Log an exiting message with result at DEBUG level.
     *
     * @param logger       logger, may not be null.
     * @param sourceClass  source class, may be null.
     * @param sourceMethod method, may be null.
     * @param result       result, may be null.
     */
    public static void exiting(
            final Logger logger,
            final String sourceClass,
            final String sourceMethod,
            final Object result) {
        if (logger.isDebugEnabled()) {
            String msg = sourceClass + "." + sourceMethod + " RETURN ";
            if (result == null) {
                msg += "null";
            } else {
                try {
                    msg += result;
                } catch(Throwable ex) {
                    msg += "?";
                }
            }
            logger.callAppenders(new LoggingEvent(FQCN, logger, Level.DEBUG,
                    msg, null));
        }
    }

    /**
     * Logs a throwing message at DEBUG level.
     *
     * @param logger       logger, may not be null.
     * @param sourceClass  source class, may be null.
     * @param sourceMethod method, may be null.
     * @param thrown      throwable, may be null.
     */
    public static void throwing(
            final Logger logger,
            final String sourceClass,
            final String sourceMethod,
            final Throwable thrown) {
        if (logger.isDebugEnabled()) {
            logger.callAppenders(new LoggingEvent(FQCN, logger, Level.DEBUG,
                    sourceClass + "." + sourceMethod + " THROW", thrown));
        }
    }
}
