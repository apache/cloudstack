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
package com.cloud.network.security;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.api.InternalIdentity;

import com.cloud.utils.db.GenericDao;

/**
 * Records the intent to update a VM's ingress ruleset
 *
 */
@Entity
@Table(name = "op_vm_ruleset_log")
public class VmRulesetLogVO implements InternalIdentity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "instance_id", updatable = false, nullable = false)
    private Long instanceId;    // vm_instance id

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name = "logsequence")
    long logsequence;

    protected VmRulesetLogVO() {

    }

    public VmRulesetLogVO(Long instanceId) {
        super();
        this.instanceId = instanceId;
    }

    @Override
    public long getId() {
        return id;
    }

    public Long getInstanceId() {
        return instanceId;
    }

    public Date getCreated() {
        return created;
    }

    public long getLogsequence() {
        return logsequence;
    }

    public void incrLogsequence() {
        logsequence++;
    }

}
