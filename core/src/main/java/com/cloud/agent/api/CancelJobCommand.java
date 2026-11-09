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

/** Between management servers: asks the one running a job's work whether that work can be stopped (checkOnly), or to stop it. */
public class CancelJobCommand extends Command {
    protected long jobId;
    protected String reason;
    protected boolean checkOnly;

    protected CancelJobCommand() {
    }

    public CancelJobCommand(long jobId, String reason, boolean checkOnly) {
        this.jobId = jobId;
        this.reason = reason;
        this.checkOnly = checkOnly;
    }

    public long getJobId() {
        return jobId;
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
