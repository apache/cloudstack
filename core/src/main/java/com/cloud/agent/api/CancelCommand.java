//
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
//

package com.cloud.agent.api;

/**
 * Asks whoever is executing a request sequence to stop it.
 *
 * Between management servers this travels as a control request and drops the peer's bookkeeping for
 * the sequence. Sent to an agent as an ordinary command, it asks the agent to stop the backend work
 * the sequence has in flight; with checkOnly set it only asks whether that would be possible, which is
 * what lets the job layer refuse a cancellation it cannot honour instead of recording it.
 */
public class CancelCommand extends Command {
    protected long sequence;
    protected String reason;
    protected boolean checkOnly;

    protected CancelCommand() {
    }

    public CancelCommand(long sequence, String reason) {
        this(sequence, reason, false);
    }

    public CancelCommand(long sequence, String reason, boolean checkOnly) {
        this.sequence = sequence;
        this.reason = reason;
        this.checkOnly = checkOnly;
    }

    public long getSequence() {
        return sequence;
    }

    public String getReason() {
        return reason;
    }

    public boolean isCheckOnly() {
        return checkOnly;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
