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

import com.cloud.network.nsx.NsxProvider;
import com.cloud.utils.db.Encrypt;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "nsx_providers")
public class NsxProviderVO implements NsxProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    long id;

    @Column(name = "zone_id")
    private long zoneId;

    @Column(name = "host_id")
    private long hostId;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "provider_name")
    private String providerName;

    @Column(name = "hostname")
    private String hostname;

    @Column(name = "port")
    private String port = "443";

    @Column(name = "username")
    private String username;

    @Encrypt
    @Column(name = "password")
    private String password;

    @Column(name = "tier0_gateway")
    private String tier0Gateway;

    @Column(name = "edge_cluster")
    private String edgeCluster;

    @Column(name = "transport_zone")
    private String transportZone;

    @Column(name = "created")
    private Date created;

    @Column(name = "removed")
    private Date removed;
    public NsxProviderVO() {
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
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    @Override
    public String getHostname() {
        return hostname;
    }

    public void setPort(String port) {
        this.port = port;
    }

    @Override
    public String getPort() {
        return port;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
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

    public String getTier0Gateway() {
        return tier0Gateway;
    }

    public void setTier0Gateway(String tier0Gateway) {
        this.tier0Gateway = tier0Gateway;
    }

    public String getEdgeCluster() {
        return edgeCluster;
    }

    public void setEdgeCluster(String edgeCluster) {
        this.edgeCluster = edgeCluster;
    }

    public String getTransportZone() {
        return transportZone;
    }

    public void setTransportZone(String transportZone) {
        this.transportZone = transportZone;
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
        private String providerName;
        private String hostname;
        private String port;
        private String username;
        private String password;
        private String tier0Gateway;
        private String edgeCluster;
        private String transportZone;


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

        public Builder setProviderName(String providerName) {
            this.providerName = providerName;
            return this;
        }

        public Builder setHostname(String hostname) {
            this.hostname = hostname;
            return this;
        }

        public Builder setPort(String port) {
            this.port = port;
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

        public Builder setTier0Gateway(String tier0Gateway) {
            this.tier0Gateway = tier0Gateway;
            return this;
        }

        public Builder setEdgeCluster(String edgeCluster) {
            this.edgeCluster = edgeCluster;
            return this;
        }

        public Builder setTransportZone(String transportZone) {
            this.transportZone = transportZone;
            return this;
        }
        public NsxProviderVO build() {
            NsxProviderVO provider = new NsxProviderVO();
            provider.setZoneId(this.zoneId);
            provider.setHostId(this.hostId);
            provider.setUuid(UUID.randomUUID().toString());
            provider.setProviderName(this.providerName);
            provider.setHostname(this.hostname);
            provider.setPort(this.port);
            provider.setUsername(this.username);
            provider.setPassword(this.password);
            provider.setTier0Gateway(this.tier0Gateway);
            provider.setEdgeCluster(this.edgeCluster);
            provider.setTransportZone(this.transportZone);
            provider.setCreated(new Date());
            return provider;
        }
    }
}
