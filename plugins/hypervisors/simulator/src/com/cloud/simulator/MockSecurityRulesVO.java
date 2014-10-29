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
package com.cloud.simulator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name = "mocksecurityrules")
public class MockSecurityRulesVO implements InternalIdentity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "vmid")
    private Long vmId;

    @Column(name = "signature")
    private String signature;

    @Column(name = "seqnum")
    private Long seqNum;

    @Column(name = "ruleset")
    private String ruleSet;

    @Column(name = "hostid")
    private String hostId;

    @Column(name = "vmname")
    public String vmName;

    public String getVmName() {
        return this.vmName;
    }

    public void setVmName(String vmName) {
        this.vmName = vmName;
    }

    public String getHostId() {
        return this.hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    @Override
    public long getId() {
        return this.id;
    }

    public Long getVmId() {
        return this.vmId;
    }

    public void setVmId(Long vmId) {
        this.vmId = vmId;
    }

    public String getSignature() {
        return this.signature;
    }

    public void setSignature(String sig) {
        this.signature = sig;
    }

    public Long getSeqNum() {
        return this.seqNum;
    }

    public void setSeqNum(Long seqNum) {
        this.seqNum = seqNum;
    }

    public String getRuleSet() {
        return this.ruleSet;
    }

    public void setRuleSet(String ruleset) {
        this.ruleSet = ruleset;
    }
}
