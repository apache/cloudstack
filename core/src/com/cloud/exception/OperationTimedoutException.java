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
package com.cloud.exception;

import com.cloud.agent.api.Command;
import com.cloud.utils.SerialVersionUID;

/**
 * This exception is thrown when the operation couldn't complete due to a
 * wait timeout.
 */
public class OperationTimedoutException extends CloudException {
    private static final long serialVersionUID = SerialVersionUID.OperationTimedoutException;
    
    long _agentId;
    long _seqId;
    int _time;
    Command[] _cmds;
    boolean _isActive;
    
    public OperationTimedoutException(Command[] cmds, long agentId, long seqId, int time, boolean isActive) {
        super("Commands " + seqId + " to Host " + agentId + " timed out after " + time);
        _agentId = agentId;
        _seqId = seqId;
        _time = time;
        _cmds = cmds;
        _isActive = isActive;
    }
    
    public long getAgentId() {
        return _agentId;
    }
    
    public long getSequenceId() {
        return _seqId;
    }
    
    public int getWaitTime() {
        return _time;
    }
    
    public Command[] getCommands() {
        return _cmds;
    }
    
    public boolean isActive() {
    	return _isActive;
    }
}
