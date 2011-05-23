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
package com.cloud.agent.api;

import java.util.HashMap;
import java.util.Map;

import com.cloud.agent.api.LogLevel.Log4jLevel;

/**
 * All communications between the agent and the management server must be
 * implemented by classes that extends the Command class. Command specifies
 * all of the methods that needs to be implemented by the children classes.
 * 
 */
public abstract class Command {

    // allow command to carry over hypervisor or other environment related context info
    @LogLevel(Log4jLevel.Trace)
    protected Map<String, String> contextMap = new HashMap<String, String>();

    protected Command() {
    }

    @Override
    public final String toString() {
        return this.getClass().getName();
    }

    /**
     * @return Does this command need to be executed in sequence on the agent?
     *         When this is set to true, the commands are executed by a single
     *         thread on the agent.
     */
    public abstract boolean executeInSequence();

    public void setContextParam(String name, String value) {
        contextMap.put(name, value);
    }

    public String getContextParam(String name) {
        return contextMap.get(name);
    }
    
    public boolean allowCaching() {
        return true;
    }
}
