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

package org.apache.cloudstack.vm.bootgroup;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.cloudstack.vm.bootgroup.readiness.InstanceBootGroupReadinessRule;

/**
 * Last cached evaluation result for a readiness rule, upserted in place — no history, by design.
 * {@code vmId} is 0 for the rule's "own" row (a VM-scoped rule's single target, or a group-scoped
 * rule's all-members aggregate); a group-scoped rule inherited by its members additionally gets one
 * row per (ruleId, vmId) for that member's own individual result.
 */
@Entity
@Table(name = "instance_boot_group_readiness_check_result")
public class InstanceBootGroupReadinessCheckResultVO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "rule_id")
    private long ruleId;

    @Column(name = "vm_id")
    private long vmId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private InstanceBootGroupReadinessRule.Status status;

    @Column(name = "message")
    private String message;

    @Column(name = "checked_on")
    @Temporal(TemporalType.TIMESTAMP)
    private Date checkedOn;

    protected InstanceBootGroupReadinessCheckResultVO() {
    }

    public InstanceBootGroupReadinessCheckResultVO(long ruleId, long vmId, InstanceBootGroupReadinessRule.Status status, String message, Date checkedOn) {
        this.ruleId = ruleId;
        this.vmId = vmId;
        this.status = status;
        this.message = message;
        this.checkedOn = checkedOn;
    }

    public long getId() {
        return id;
    }

    public long getRuleId() {
        return ruleId;
    }

    public long getVmId() {
        return vmId;
    }

    public InstanceBootGroupReadinessRule.Status getStatus() {
        return status;
    }

    public void setStatus(InstanceBootGroupReadinessRule.Status status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getCheckedOn() {
        return checkedOn;
    }

    public void setCheckedOn(Date checkedOn) {
        this.checkedOn = checkedOn;
    }
}
