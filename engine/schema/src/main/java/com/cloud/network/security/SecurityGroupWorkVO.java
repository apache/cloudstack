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
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.cloudstack.api.InternalIdentity;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "op_nwgrp_work")
public class SecurityGroupWorkVO implements SecurityGroupWork, InternalIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "instance_id", updatable = false, nullable = false)
    private Long instanceId;    // vm_instance id

    @Column(name = "mgmt_server_id", nullable = true)
    private Long serverId;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name = "step", nullable = false)
    @Enumerated(value = EnumType.STRING)
    private Step step;

    @Column(name = "taken", nullable = true)
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date dateTaken;

    @Column(name = "seq_no", nullable = true)
    private Long logsequenceNumber = null;

    protected SecurityGroupWorkVO() {
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public Long getInstanceId() {
        return instanceId;
    }

    public Long getServerId() {
        return serverId;
    }

    public void setServerId(final Long serverId) {
        this.serverId = serverId;
    }

    public Date getCreated() {
        return created;
    }

    public SecurityGroupWorkVO(Long instanceId, Long serverId, Date created, Step step, Date dateTaken) {
        super();
        this.instanceId = instanceId;
        this.serverId = serverId;
        this.created = created;
        this.step = step;
        this.dateTaken = dateTaken;
    }

    @Override
    public String toString() {
        return new StringBuilder("[NWGrp-Work:id=").append(id).append(":vm=").append(instanceId).append("]").toString();
    }

    public Date getDateTaken() {
        return dateTaken;
    }

    @Override
    public void setStep(Step step) {
        this.step = step;
    }

    @Override
    public Step getStep() {
        return step;
    }

    public void setDateTaken(Date date) {
        dateTaken = date;
    }

    @Override
    public Long getLogsequenceNumber() {
        return logsequenceNumber;
    }

    @Override
    public void setLogsequenceNumber(Long logsequenceNumber) {
        this.logsequenceNumber = logsequenceNumber;
    }

}
