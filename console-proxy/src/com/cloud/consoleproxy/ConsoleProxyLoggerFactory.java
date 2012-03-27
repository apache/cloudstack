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

package com.cloud.consoleproxy;

import com.cloud.consoleproxy.util.Logger;
import com.cloud.consoleproxy.util.LoggerFactory;

public class ConsoleProxyLoggerFactory implements LoggerFactory {
	public ConsoleProxyLoggerFactory() {
	}
	
	public Logger getLogger(Class<?> clazz) {
		return new Log4jLogger(org.apache.log4j.Logger.getLogger(clazz));
	}
	
	public static class Log4jLogger extends Logger {
		private org.apache.log4j.Logger logger;
		
		public Log4jLogger(org.apache.log4j.Logger logger) {
			this.logger = logger;
		}
		
		public boolean isTraceEnabled() {
			return logger.isTraceEnabled();
		}
		
		public boolean isDebugEnabled() {
			return logger.isDebugEnabled();
		}
		
		public boolean isInfoEnabled() {
			return logger.isInfoEnabled();
		}

		public void trace(Object message) {
			logger.trace(message);
		}
		
		public void trace(Object message, Throwable exception) {
			logger.trace(message, exception);
		}
		
		public void info(Object message) {
			logger.info(message);
		}
		
		public void info(Object message, Throwable exception) {
			logger.info(message, exception);
		}
		
		public void debug(Object message) {
			logger.debug(message);
		}
		
		public void debug(Object message, Throwable exception) {
			logger.debug(message, exception);
		}
		
		public void warn(Object message) {
			logger.warn(message);
		}
		
		public void warn(Object message, Throwable exception) {
			logger.warn(message, exception);
		}
		
		public void error(Object message) {
			logger.error(message);
		}
		
		public void error(Object message, Throwable exception) {
			logger.error(message, exception);
		}
	}
}
