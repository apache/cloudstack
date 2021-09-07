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

import com.cloud.utils.DateUtil;
import com.cloud.utils.db.GenericDao;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "vsphere_storage_policy")
public class VsphereStoragePolicyVO implements VsphereStoragePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "zone_id")
    private long zoneId;

    @Column(name = "policy_id")
    private String policyId;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "update_time", updatable = true)
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date updateTime;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    public VsphereStoragePolicyVO(long zoneId, String policyId, String name, String description) {
        this.uuid = UUID.randomUUID().toString();
        this.zoneId = zoneId;
        this.policyId = policyId;
        this.name = name;
        this.description = description;
        this.updateTime = DateUtil.currentGMTTime();
    }

    public VsphereStoragePolicyVO() {
        uuid = UUID.randomUUID().toString();
    }
    public VsphereStoragePolicyVO(long id) {
        this.id = id;
        uuid = UUID.randomUUID().toString();
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public long getZoneId() {
        return zoneId;
    }

    @Override
    public String getPolicyId() {
        return policyId;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public Date getRemoved() {
        return removed;
    }
}
