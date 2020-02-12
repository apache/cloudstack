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

package com.cloud.kubernetes.version;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "kubernetes_supported_version")
public class KubernetesSupportedVersionVO implements KubernetesSupportedVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "name")
    private String name;

    @Column(name = "semantic_version")
    private String semanticVersion;

    @Column(name = "iso_id")
    private long isoId;

    @Column(name = "zone_id")
    private Long zoneId;

    @Column(name = "state")
    @Enumerated(value = EnumType.STRING)
    State state = State.Enabled;

    @Column(name = "min_cpu")
    private int minimumCpu;

    @Column(name = "min_ram_size")
    private int minimumRamSize;

    @Column(name = GenericDao.CREATED_COLUMN)
    Date created;

    @Column(name = GenericDao.REMOVED_COLUMN)
    Date removed;

    public KubernetesSupportedVersionVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    public KubernetesSupportedVersionVO(String name, String semanticVersion, long isoId, Long zoneId,
                                        int minimumCpu, int minimumRamSize) {
        this.uuid = UUID.randomUUID().toString();
        this.name = name;
        this.semanticVersion = semanticVersion;
        this.isoId = isoId;
        this.zoneId = zoneId;
        this.minimumCpu = minimumCpu;
        this.minimumRamSize = minimumRamSize;
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
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getSemanticVersion() {
        return semanticVersion;
    }

    public void setSemanticVersion(String semanticVersion) {
        this.semanticVersion = semanticVersion;
    }

    @Override
    public long getIsoId() {
        return isoId;
    }

    public void setIsoId(long isoId) {
        this.isoId = isoId;
    }

    @Override
    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }

    @Override
    public State getState() {
        return this.state;
    }

    public void setState(State state) {
        this.state = state;
    }

    @Override
    public int getMinimumCpu() {
        return minimumCpu;
    }

    public void setMinimumCpu(int minimumCpu) {
        this.minimumCpu = minimumCpu;
    }

    @Override
    public int getMinimumRamSize() {
        return minimumRamSize;
    }

    public void setMinimumRamSize(int minimumRamSize) {
        this.minimumRamSize = minimumRamSize;
    }

    public Date getCreated() {
        return created;
    }

    public Date getRemoved() {
        return removed;
    }
}
