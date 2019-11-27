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

package com.cloud.kubernetesversion;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
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

    @Column(name = "kubernetes_version")
    private String kubernetesVersion;

    @Column(name = "iso_id")
    private long isoId;

    @Column(name = "zone_id")
    private Long zoneId;

    @Column(name = GenericDao.CREATED_COLUMN)
    Date created;

    @Column(name = GenericDao.REMOVED_COLUMN)
    Date removed;

    public KubernetesSupportedVersionVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    public KubernetesSupportedVersionVO(String name, String kubernetesVersion, long isoId, Long zoneId) {
        this.uuid = UUID.randomUUID().toString();
        this.name = name;
        this.kubernetesVersion = kubernetesVersion;
        this.isoId = isoId;
        this.zoneId = zoneId;
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
    public String getKubernetesVersion() {
        return kubernetesVersion;
    }

    public void setKubernetesVersion(String kubernetesVersion) {
        this.kubernetesVersion = kubernetesVersion;
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

    public Date getCreated() {
        return created;
    }

    public Date getRemoved() {
        return removed;
    }
}
