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
package com.cloud.dc;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name = "op_pod_vlan_alloc")
public class PodVlanVO implements InternalIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @Column(name = "taken", nullable = true)
    @Temporal(value = TemporalType.TIMESTAMP)
    Date takenAt;

    @Column(name = "vlan", updatable = false, nullable = false)
    protected String vlan;

    @Column(name = "data_center_id")
    long dataCenterId;

    @Column(name = "pod_id", updatable = false, nullable = false)
    protected long podId;

    @Column(name = "account_id")
    protected Long accountId;

    public Date getTakenAt() {
        return takenAt;
    }

    public void setTakenAt(Date taken) {
        this.takenAt = taken;
    }

    public PodVlanVO(String vlan, long dataCenterId, long podId) {
        this.vlan = vlan;
        this.dataCenterId = dataCenterId;
        this.podId = podId;
        this.takenAt = null;
    }

    @Override
    public long getId() {
        return id;
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getVlan() {
        return vlan;
    }

    public long getDataCenterId() {
        return dataCenterId;
    }

    public long getPodId() {
        return podId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    protected PodVlanVO() {
    }
}
