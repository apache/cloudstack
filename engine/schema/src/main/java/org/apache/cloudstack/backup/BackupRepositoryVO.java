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

package org.apache.cloudstack.backup;

import com.cloud.utils.db.Encrypt;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "backup_repository")
public class BackupRepositoryVO implements BackupRepository {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "name")
    private String name;

    @Column(name = "zone_id", nullable = false)
    private long zoneId;

    @Column(name = "provider", nullable = false)
    private String provider;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "address", nullable = false)
    private String address;

    @Encrypt
    @Column(name = "mount_opts")
    private String mountOptions;

    @Column(name = "used_bytes",nullable = true)
    private Long usedBytes;

    @Column(name = "capacity_bytes", nullable = true)
    private Long capacityBytes;

    @Column(name = "created")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date created;

    @Column(name = "removed")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date removed;

    public BackupRepositoryVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    public BackupRepositoryVO(final long zoneId, final String provider, final String name, final String type, final String address, final String mountOptions, final Long capacityBytes) {
        this();
        this.zoneId = zoneId;
        this.provider = provider;
        this.name = name;
        this.type = type;
        this.address = address;
        this.mountOptions = mountOptions;
        this.capacityBytes = capacityBytes;
        this.created = new Date();
    }

    public String getUuid() {
        return uuid;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public long getZoneId() {
        return zoneId;
    }

    @Override
    public String getProvider() {
        return provider;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String getAddress() {
        return address;
    }

    @Override
    public String getMountOptions() {
        return mountOptions;
    }

    @Override
    public Long getUsedBytes() {
        return usedBytes;
    }

    @Override
    public Long getCapacityBytes() {
        return capacityBytes;
    }

    public Date getCreated() {
        return created;
    }
}
