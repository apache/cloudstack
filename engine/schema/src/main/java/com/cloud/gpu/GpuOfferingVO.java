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
package com.cloud.gpu;

import com.cloud.utils.db.GenericDao;
import org.apache.cloudstack.gpu.GpuOffering;
import org.apache.cloudstack.gpu.VgpuProfile;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * GPU offering Value Object
 */
@Entity
@Table(name = "gpu_offering")
public class GpuOfferingVO implements GpuOffering {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid", updatable = false, nullable = false, length = 40)
    private String uuid;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", nullable = true, length = 1024)
    private String description;


    @Column(name = "sort_key")
    int sortKey;

    @Column(name = "created", updatable = false, nullable = false)
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date created;

    @Column(name = GenericDao.REMOVED_COLUMN)
    @Temporal(TemporalType.TIMESTAMP)
    private Date removed;

    @Column(name = "state", nullable = false)
    private State state = State.Active;

    @Transient
    private List<VgpuProfile> vgpuProfiles = null;

    public GpuOfferingVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    /**
     * Constructor for creating a new GPU offering
     */
    public GpuOfferingVO(String name, String description) {
        this.uuid = UUID.randomUUID().toString();
        this.name = name;
        this.description = description;
        this.created = new Date();
    }

    @Override
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

    public int getSortKey() {
        return sortKey;
    }

    public void setSortKey(int sortKey) {
        this.sortKey = sortKey;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getRemoved() {
        return removed;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    @Override
    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    @Override
    public List<VgpuProfile> getVgpuProfiles() {
        return vgpuProfiles;
    }

    public void setVgpuProfiles(List<VgpuProfile> vgpuProfiles) {
        this.vgpuProfiles = vgpuProfiles;
    }

    /**
     * Add a vGPU profile to this offering
     */
    public void addVgpuProfile(VgpuProfile vgpuProfile) {
        if (vgpuProfiles == null) {
            vgpuProfiles = new ArrayList<>();
        }
        if (!vgpuProfiles.contains(vgpuProfile)) {
            vgpuProfiles.add(vgpuProfile);
        }
    }

    /**
     * Remove a vGPU profile from this offering
     */
    public void removeVgpuProfile(VgpuProfile vgpuProfile) {
        if (vgpuProfile != null) {
            vgpuProfiles.remove(vgpuProfile);
        }
    }
}
