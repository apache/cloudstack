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
		if(factory != null) {
			if(logger == null)
				logger = factory.getLogger(clazz);
			
			return logger.isTraceEnabled();
		}
		return level <= LEVEL_TRACE;
	}
	
	public boolean isDebugEnabled() {
		if(factory != null) {
			if(logger == null)
				logger = factory.getLogger(clazz);
			
			return logger.isDebugEnabled();
		}
		return level <= LEVEL_DEBUG;
	}
	
	public boolean isInfoEnabled() {
		if(factory != null) {
			if(logger == null)
				logger = factory.getLogger(clazz);
			
			return logger.isInfoEnabled();
		}
		return level <= LEVEL_INFO;
	}

	public void trace(Object message) {
		
		if(factory != null) {
			if(logger == null)
				logger = factory.getLogger(clazz);
			
			logger.trace(message);
		} else {
			if(level <= LEVEL_TRACE)
				System.out.println(message);
		}
	}
	
	public void trace(Object message, Throwable exception) {
		if(factory != null) {
			if(logger == null)
				logger = factory.getLogger(clazz);
			
			logger.trace(message, exception);
		} else {
			if(level <= LEVEL_TRACE) {
				System.out.println(message);
				if (exception != null) {
					exception.printStackTrace(System.out);
				}
			}
		}
	}
	
	public void info(Object message) {
		if(factory != null) {
			if(logger == null)
				logger = factory.getLogger(clazz);
			
			logger.info(message);
		} else {
			if(level <= LEVEL_INFO)
				System.out.println(message);
		}
	}
	
	public void info(Object message, Throwable exception) {
		if(factory != null) {
			if(logger == null)
				logger = factory.getLogger(clazz);
			
			logger.info(message, exception);
		} else {		
			if(level <= LEVEL_INFO) {
				System.out.println(message);
				if (exception != null) {
					exception.printStackTrace(System.out);
				}
			}
		}
	}
	
	public void debug(Object message) {
		if(factory != null) {
			if(logger == null)
				logger = factory.getLogger(clazz);
			
			logger.debug(message);
		} else {
			if(level <= LEVEL_DEBUG)
				System.out.println(message);
		}
	}
	
	public void debug(Object message, Throwable exception) {
		if(factory != null) {
			if(logger == null)
				logger = factory.getLogger(clazz);
			
			logger.debug(message, exception);
		} else {
			if(level <= LEVEL_DEBUG) {
				System.out.println(message);
				if (exception != null) {
					exception.printStackTrace(System.out);
				}
			}
		}
	}
	
	public void warn(Object message) {
		if(factory != null) {
			if(logger == null)
				logger = factory.getLogger(clazz);
			
			logger.warn(message);
		} else {
			if(level <= LEVEL_WARN)
				System.out.println(message);
		}
	}
	
	public void warn(Object message, Throwable exception) {
		if(factory != null) {
			if(logger == null)
				logger = factory.getLogger(clazz);
			
			logger.warn(message, exception);
		} else {
			if(level <= LEVEL_WARN) {
				System.out.println(message);
				if (exception != null) {
					exception.printStackTrace(System.out);
				}
			}
		}
	}
	
	public void error(Object message) {
		if(factory != null) {
			if(logger == null)
				logger = factory.getLogger(clazz);
			
			logger.error(message);
		} else {
			if(level <= LEVEL_ERROR)
				System.out.println(message);
		}
	}
	
	public void error(Object message, Throwable exception) {
		if(factory != null) {
			if(logger == null)
				logger = factory.getLogger(clazz);
			
			logger.error(message, exception);
		} else {
			if(level <= LEVEL_ERROR) {
				System.out.println(message);
				if (exception != null) {
					exception.printStackTrace(System.out);
				}
			}
		}
	}
}
