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

public class SecurityGroupRuleAnswer extends Answer {
    public static enum FailureReason {
        NONE, UNKNOWN, PROGRAMMING_FAILED, CANNOT_BRIDGE_FIREWALL
    }

    Long logSequenceNumber = null;
    Long vmId = null;
    FailureReason reason = FailureReason.NONE;

    protected SecurityGroupRuleAnswer() {
    }

    public SecurityGroupRuleAnswer(SecurityGroupRulesCmd cmd) {
        super(cmd);
        this.logSequenceNumber = cmd.getSeqNum();
        this.vmId = cmd.getVmId();
    }

    public SecurityGroupRuleAnswer(SecurityGroupRulesCmd cmd, boolean result, String detail) {
        super(cmd, result, detail);
        this.logSequenceNumber = cmd.getSeqNum();
        this.vmId = cmd.getVmId();
        reason = FailureReason.PROGRAMMING_FAILED;
    }

    public SecurityGroupRuleAnswer(SecurityGroupRulesCmd cmd, boolean result, String detail, FailureReason r) {
        super(cmd, result, detail);
        this.logSequenceNumber = cmd.getSeqNum();
        this.vmId = cmd.getVmId();
        reason = r;
    }

    public Long getLogSequenceNumber() {
        return logSequenceNumber;
    }

    public Long getVmId() {
        return vmId;
    }

    public FailureReason getReason() {
        return reason;
    }

    public void setReason(FailureReason reason) {
        this.reason = reason;
    }

}
