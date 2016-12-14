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

import java.util.Arrays;

/**
 * job can be cancelled using async job cancel api
 */
public class OperationCancelledException extends CloudException {


    long _agentId;
    long _seqId;
    int _time;


    transient Command[] _cmds;
    boolean _isActive;
    boolean _isCancelled = false;

    public OperationCancelledException(Command[] cmds, long agentId, long seqId, int time, boolean isActive) {
        super("Commands: " + Arrays.toString(cmds) + " to Host " + agentId + " with seqId " + seqId + " cancelled " +
                "after " + time);
        _agentId = agentId;
        _seqId = seqId;
        _time = time;
        _cmds = cmds;
        _isActive = isActive;
        _isCancelled = true;
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

    public boolean isCancelled() {
        return _isCancelled;
    }

}
