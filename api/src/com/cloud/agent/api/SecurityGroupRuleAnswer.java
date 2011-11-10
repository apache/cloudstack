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

public class SecurityGroupRuleAnswer extends Answer {
    public static enum FailureReason {
        NONE,
        UNKNOWN,
        PROGRAMMING_FAILED,
        CANNOT_BRIDGE_FIREWALL
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
