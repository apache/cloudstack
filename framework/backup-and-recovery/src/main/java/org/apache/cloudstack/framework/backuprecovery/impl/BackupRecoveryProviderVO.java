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
package org.apache.cloudstack.framework.backuprecovery.impl;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Column;
import javax.persistence.GenerationType;
import java.util.UUID;

@Entity
@Table(name = "backup_and_recovery_providers")
public class BackupRecoveryProviderVO implements InternalIdentity, Identity {

    public BackupRecoveryProviderVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    public BackupRecoveryProviderVO(final long hostId, final String name, final long zoneId, final String url, final String provider) {
        this();
        this.name = name;
        this.zoneId = zoneId;
        this.hostId = hostId;
        this.provider = provider;
        this.url = url;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "host_id")
    private long hostId;

    @Column(name = "name")
    private String name;

    @Column(name = "url", length = 2048)
    private String url;

    @Column(name = "zone_id")
    private long zoneId;

    @Column(name = "provider")
    private String provider;

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

    public long getHostId() {
        return hostId;
    }

    public void setHostId(long hostId) {
        this.hostId = hostId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getZoneId() {
        return zoneId;
    }

    public void setZoneId(long zoneId) {
        this.zoneId = zoneId;
    }

    public String getProviderName() {
        return provider;
    }

    public void setProviderName(String provider) {
        this.provider = provider;
    }
}
