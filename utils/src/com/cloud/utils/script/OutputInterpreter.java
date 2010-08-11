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
package com.cloud.utils.script;

import java.io.BufferedReader;
import java.io.IOException;

import org.apache.log4j.Logger;

/**
 * OutputInterpreter is used to parse the output of the script call.
 * It should return null if the parsing is successful and an error
 * message if it was not.  Do not catch the InterruptedException as
 * that is used by Script to implement the timeout.
 */
public abstract class OutputInterpreter {
    public boolean drain() {
        return false;
    }
    
    public String processError(BufferedReader reader) throws IOException {
        StringBuilder buff = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            buff.append(line);
        }
        return buff.toString();
    }
    
    public abstract String interpret(BufferedReader reader) throws IOException;
    
    public static final OutputInterpreter NoOutputParser = new OutputInterpreter() {
        @Override
		public String interpret(BufferedReader reader) throws IOException {
            return null;
        }
    };
    
    public static class TimedOutLogger extends OutputInterpreter {
    	Process _process;
    	public TimedOutLogger(Process process) {
    		_process = process;
    	}
    	
    	@Override
    	public boolean drain() {
    		return true;
    	}
    	
    	@Override
		public String interpret(BufferedReader reader) throws IOException {
        	StringBuilder buff = new StringBuilder();
    		
    		while (reader.ready()) {
    			buff.append(reader.readLine());
    		}
    		
    		_process.destroy();
    		
    		try {
	    		while (reader.ready()) {
	    			buff.append(reader.readLine());
	    		}
    		} catch (IOException e) {
    		}
    		
    		return buff.toString();
    	}
    }
    
    public static class OutputLogger extends OutputInterpreter {
        Logger _logger;
        public OutputLogger(Logger logger) {
            _logger = logger;
        }
        @Override
		public String interpret(BufferedReader reader) throws IOException {
            StringBuilder builder = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
            if (builder.length() > 0) {
                _logger.debug(builder.toString());
            }
            return null;
        }
    }
    
    public static class OneLineParser extends OutputInterpreter {
        String line = null;
        
        @Override
		public String interpret(BufferedReader reader) throws IOException {
            line = reader.readLine();
            return null;
        }
        
        public String getLine() {
            return line;
        }
    };
    
    public static class AllLinesParser extends OutputInterpreter {
    	String allLines = null;
    	
    	@Override
		public String interpret(BufferedReader reader) throws IOException {
            StringBuilder builder = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
            allLines = builder.toString();
            return null;
        }
    	
    	public String getLines() {
    		return allLines;
    	}
    }
    
}







