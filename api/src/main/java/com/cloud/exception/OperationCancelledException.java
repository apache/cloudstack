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
import com.cloud.utils.exception.CloudRuntimeException;

import java.util.Arrays;

/**
 * Thrown when an operation was stopped because its async job was cancelled.
 *
 * Unchecked on purpose: a cancellation is not an alternative outcome that every caller between the
 * agent layer and the job layer should have to name and handle. Almost all of them would only
 * re-label it as a timeout or an unavailable agent, which is exactly what hides the cancellation
 * from the job result. Callers that genuinely need to distinguish it still can.
 */
public class OperationCancelledException extends CloudRuntimeException {
    private static final long serialVersionUID = SerialVersionUID.OperationCancelledException;
    long _agentId;
    long _seqId;
    int _time;
    boolean _isActive;
    String _reason;

    transient Command[] _cmds;

    public OperationCancelledException(Command[] cmds, long agentId, long seqId, int time, boolean isActive, String reason) {
        super("Commands: " + Arrays.toString(cmds) + " to Host " + agentId + " with seqId " + seqId + " cancelled after " + time + " secs");
        _agentId = agentId;
        _seqId = seqId;
        _time = time;
        _cmds = cmds;
        _isActive = isActive;
        _reason = reason;
    }

    public OperationCancelledException(Command[] cmds, long agentId, long seqId, int time, boolean isActive) {
        this(cmds, agentId, seqId, time, isActive, null);
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

    public boolean isActive() {
        return _isActive;
    }

    public String getReason() {
        return _reason;
    }

    public Command[] getCommands() {
        return _cmds;
    }
}
