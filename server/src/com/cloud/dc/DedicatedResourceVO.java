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

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;


@Entity
@Table(name="dedicated_resources")
public class DedicatedResourceVO implements DedicatedResources{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    long id;

    @Column(name="data_center_id")
    Long dataCenterId;

    @Column(name="pod_id")
    Long podId;

    @Column(name="cluster_id")
    Long clusterId;

    @Column(name="host_id")
    Long hostId;

    @Column(name="uuid")
    String uuid;

    @Column(name = "domain_id")
    private Long domainId;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "affinity_group_id")
    private long affinityGroupId;

    public DedicatedResourceVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    public DedicatedResourceVO(Long dataCenterId, Long podId, Long clusterId, Long hostId, Long domainId,
            Long accountId, long affinityGroupId) {
        this.dataCenterId = dataCenterId;
        this.podId = podId;
        this.clusterId = clusterId;
        this.hostId = hostId;
        this.domainId = domainId;
        this.accountId = accountId;
        this.uuid = UUID.randomUUID().toString();
        this.affinityGroupId = affinityGroupId;
    }

    public long getId() {
        return id;
    }

    public Long getDataCenterId() {
        return dataCenterId;
    }

    public void setDataCenterId(long dataCenterId) {
        this.dataCenterId = dataCenterId;
    }

    public Long getPodId() {
        return podId;
    }

    public void setPodId(long podId) {
        this.podId = podId;
    }

    public Long getClusterId() {
        return clusterId;
    }

    public void setClusterId(long clusterId) {
        this.clusterId = clusterId;
    }

    public Long getHostId() {
        return hostId;
    }

    public void setHostId(long hostId) {
        this.hostId = hostId;
    }

    public DedicatedResourceVO(long dedicatedResourceId) {
        this.id = dedicatedResourceId;
    }

    public Long getDomainId() {
        return domainId;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getUuid() {
        return this.uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public long getAffinityGroupId() {
        return affinityGroupId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DedicatedResourceVO) {
            return ((DedicatedResourceVO) obj).getId() == this.getId();
        } else {
            return false;
        }
    }
}
