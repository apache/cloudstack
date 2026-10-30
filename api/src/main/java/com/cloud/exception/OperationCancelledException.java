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
import java.util.stream.Collectors;

/** Thrown when an operation was stopped because its async job was cancelled. Unchecked: intermediate callers must not relabel it as a timeout. */
public class OperationCancelledException extends CloudRuntimeException {

    private static final long serialVersionUID = SerialVersionUID.OperationCancelledException;

    long _agentId;
    long _seqId;
    long _elapsedSeconds;
    boolean _isActive;
    String _reason;

    transient Command[] _cmds;

    public OperationCancelledException(Command[] cmds, long agentId, long seqId, long elapsedSeconds, boolean isActive, String reason) {
        super(commandNames(cmds) + " (seq " + seqId + ") on host " + agentId + " cancelled after " + elapsedSeconds + " s" + (reason != null ? ": " + reason : ""));
        _agentId = agentId;
        _seqId = seqId;
        _elapsedSeconds = elapsedSeconds;
        _cmds = cmds;
        _isActive = isActive;
        _reason = reason;
    }

    private OperationCancelledException(String message, Command[] cmds, long agentId, String reason) {
        super(message);
        _agentId = agentId;
        _cmds = cmds;
        _reason = reason;
    }

    /** The commands were never sent: its job had already been cancelled. */
    public static OperationCancelledException updateCmdsNotSent(Command[] cmds, long agentId, long jobId) {
        final String reason = "job-" + jobId + " was cancelled";
        return new OperationCancelledException(commandNames(cmds) + " to host " + agentId + " not sent: " + reason, cmds, agentId, reason);
    }

    static String commandNames(Command[] cmds) {
        if (cmds == null || cmds.length == 0) {
            return "Command";
        }
        return Arrays.stream(cmds).map(cmd -> cmd == null ? "null" : cmd.getClass().getSimpleName()).collect(Collectors.joining(", "));
    }

    public long getAgentId() {
        return _agentId;
    }

    public long getSequenceId() {
        return _seqId;
    }

    public long getElapsedSeconds() {
        return _elapsedSeconds;
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
