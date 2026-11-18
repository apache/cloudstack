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
package com.cloud.network.element;

import com.cloud.network.netris.NetrisProvider;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "netris_providers")
public class NetrisProviderVO implements NetrisProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "name")
    private String name;

    @Column(name = "zone_id")
    private long zoneId;

    @Column(name = "host_id")
    private long hostId;

    @Column(name = "url")
    private String url;

    @Column(name = "username")
    private String username;

    @Column(name = "password")
    private String password;

    @Column(name = "site_name")
    private String siteName;

    @Column(name = "tenant_name")
    private String tenantName;

    @Column(name = "netris_tag")
    private String netrisTag;

    @Column(name = "created")
    private Date created;

    @Column(name = "removed")
    private Date removed;

    public NetrisProviderVO() {
        this.uuid = UUID.randomUUID().toString();
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
    public long getZoneId() {
        return zoneId;
    }

    public void setZoneId(long zoneId) {
        this.zoneId = zoneId;
    }

    public long getHostId() {
        return hostId;
    }

    public void setHostId(long hostId) {
        this.hostId = hostId;
    }

    @Override
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public String getNetrisTag() {
        return netrisTag;
    }

    public void setNetrisTag(String netrisTag) {
        this.netrisTag = netrisTag;
    }

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

    public static final class Builder {
        private long zoneId;
        private long hostId;
        private String name;
        private String url;
        private String username;
        private String password;
        private String siteName;
        private String tenantName;
        private String netrisTag;

        public Builder() {
            // Default constructor
        }

        public Builder setZoneId(long zoneId) {
            this.zoneId = zoneId;
            return this;
        }

        public Builder setHostId(long hostId) {
            this.hostId = hostId;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        public Builder setUsername(String username) {
            this.username = username;
            return this;
        }

        public Builder setPassword(String password) {
            this.password = password;
            return this;
        }

        public Builder setSiteName(String siteName) {
            this.siteName = siteName;
            return this;
        }

        public Builder setTenantName(String tenantName) {
            this.tenantName = tenantName;
            return this;
        }

        public Builder setNetrisTag(String netrisTag) {
            this.netrisTag = netrisTag;
            return this;
        }

        public NetrisProviderVO build() {
            NetrisProviderVO provider = new NetrisProviderVO();
            provider.setZoneId(this.zoneId);
            provider.setHostId(this.hostId);
            provider.setUuid(UUID.randomUUID().toString());
            provider.setName(this.name);
            provider.setUrl(this.url);
            provider.setUsername(this.username);
            provider.setPassword(this.password);
            provider.setSiteName(this.siteName);
            provider.setTenantName(this.tenantName);
            provider.setNetrisTag(this.netrisTag);
            provider.setCreated(new Date());
            return provider;
        }
    }
}
