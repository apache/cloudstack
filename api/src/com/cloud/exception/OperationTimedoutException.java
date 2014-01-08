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
package com.cloud.exception;

import com.cloud.agent.api.Command;
import com.cloud.utils.SerialVersionUID;

/**
 * wait timeout.
 */
public class OperationTimedoutException extends CloudException {
    private static final long serialVersionUID = SerialVersionUID.OperationTimedoutException;

    long _agentId;
    long _seqId;
    int _time;

    // TODO
    // I did a reference search on usage of getCommands() and found none
    //
    // to prevent serialization problems across boundaries, I'm disabling serialization of _cmds here
    // getCommands() will still be available within the same serialization boundary, but it will be lost
    // when exception is propagated across job boundaries.
    //
    transient Command[] _cmds;
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
