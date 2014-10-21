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
package com.cloud.consoleproxy.util;

// logger facility for dynamic switch between console logger used in Applet and log4j based logger
public class Logger {
    private static LoggerFactory factory = null;

    public static final int LEVEL_TRACE = 1;
    public static final int LEVEL_DEBUG = 2;
    public static final int LEVEL_INFO = 3;
    public static final int LEVEL_WARN = 4;
    public static final int LEVEL_ERROR = 5;

    private Class<?> clazz;
    private Logger logger;

    private static int level = LEVEL_INFO;

    public static Logger getLogger(Class<?> clazz) {
        return new Logger(clazz);
    }

    public static void setFactory(LoggerFactory f) {
        factory = f;
    }

    public static void setLevel(int l) {
        level = l;
    }

    public Logger(Class<?> clazz) {
        this.clazz = clazz;
    }

    protected Logger() {
    }

    public boolean isTraceEnabled() {
        if (factory != null) {
            if (logger == null)
                logger = factory.getLogger(clazz);

            return logger.isTraceEnabled();
        }
        return level <= LEVEL_TRACE;
    }

    public boolean isDebugEnabled() {
        if (factory != null) {
            if (logger == null)
                logger = factory.getLogger(clazz);

            return logger.isDebugEnabled();
        }
        return level <= LEVEL_DEBUG;
    }

    public boolean isInfoEnabled() {
        if (factory != null) {
            if (logger == null)
                logger = factory.getLogger(clazz);

            return logger.isInfoEnabled();
        }
        return level <= LEVEL_INFO;
    }

    public void trace(Object message) {

        if (factory != null) {
            if (logger == null)
                logger = factory.getLogger(clazz);

            logger.trace(message);
        } else {
            if (level <= LEVEL_TRACE)
                System.out.println(message);
        }
    }

    public void trace(Object message, Throwable exception) {
        if (factory != null) {
            if (logger == null)
                logger = factory.getLogger(clazz);

            logger.trace(message, exception);
        } else {
            if (level <= LEVEL_TRACE) {
                System.out.println(message);
                if (exception != null) {
                    exception.printStackTrace(System.out);
                }
            }
        }
    }

    public void info(Object message) {
        if (factory != null) {
            if (logger == null)
                logger = factory.getLogger(clazz);

            logger.info(message);
        } else {
            if (level <= LEVEL_INFO)
                System.out.println(message);
        }
    }

    public void info(Object message, Throwable exception) {
        if (factory != null) {
            if (logger == null)
                logger = factory.getLogger(clazz);

            logger.info(message, exception);
        } else {
            if (level <= LEVEL_INFO) {
                System.out.println(message);
                if (exception != null) {
                    exception.printStackTrace(System.out);
                }
            }
        }
    }

    public void debug(Object message) {
        if (factory != null) {
            if (logger == null)
                logger = factory.getLogger(clazz);

            logger.debug(message);
        } else {
            if (level <= LEVEL_DEBUG)
                System.out.println(message);
        }
    }

    public void debug(Object message, Throwable exception) {
        if (factory != null) {
            if (logger == null)
                logger = factory.getLogger(clazz);

            logger.debug(message, exception);
        } else {
            if (level <= LEVEL_DEBUG) {
                System.out.println(message);
                if (exception != null) {
                    exception.printStackTrace(System.out);
                }
            }
        }
    }

    public void warn(Object message) {
        if (factory != null) {
            if (logger == null)
                logger = factory.getLogger(clazz);

            logger.warn(message);
        } else {
            if (level <= LEVEL_WARN)
                System.out.println(message);
        }
    }

    public void warn(Object message, Throwable exception) {
        if (factory != null) {
            if (logger == null)
                logger = factory.getLogger(clazz);

            logger.warn(message, exception);
        } else {
            if (level <= LEVEL_WARN) {
                System.out.println(message);
                if (exception != null) {
                    exception.printStackTrace(System.out);
                }
            }
        }
    }

    public void error(Object message) {
        if (factory != null) {
            if (logger == null)
                logger = factory.getLogger(clazz);

            logger.error(message);
        } else {
            if (level <= LEVEL_ERROR)
                System.out.println(message);
        }
    }

    public void error(Object message, Throwable exception) {
        if (factory != null) {
            if (logger == null)
                logger = factory.getLogger(clazz);

            logger.error(message, exception);
        } else {
            if (level <= LEVEL_ERROR) {
                System.out.println(message);
                if (exception != null) {
                    exception.printStackTrace(System.out);
                }
            }
        }
    }
}
