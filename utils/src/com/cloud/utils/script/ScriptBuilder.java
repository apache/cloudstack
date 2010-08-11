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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * ScriptBuilder builds the script to be executed.
 *
 */
public class ScriptBuilder {
    Logger _logger;
    long _timeout;
    String _command;
    ArrayList<String> _params;
    Executor _executor;
    
    public ScriptBuilder(String command, Executor executor, long timeout, Logger logger) {
        _command = command;
        _timeout = timeout;
        _logger = logger;
        _executor = executor;
        
    }
    
    public ScriptBuilder add(String... params) {
        for (String param : params) {
            _params.add(param);
        }
        
        return this;
    }
    
    public Script script() {
        return new Script(this);
    }
    
    public List<String> getParameterNames() {
        return _params;
    }
    
    public String getCommand() {
        return _command;
    }
    
    public long getTimeout() {
        return _timeout;
    }
    
    public Logger getLogger() {
        return _logger;
    }
    
    public Executor getExecutor() {
        return _executor;
    }
}