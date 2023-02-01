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

import com.cloud.network.TungstenProvider;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "tungsten_providers")
public class TungstenProviderVO implements TungstenProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    long id;

    @Column(name = "zone_id")
    private long zoneId;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "host_id")
    private long hostId;

    @Column(name = "provider_name")
    private String providerName;

    @Column(name = "port")
    private String port;

    @Column(name = "hostname")
    private String hostname;

    @Column(name = "gateway")
    private String gateway;

    @Column(name = "vrouter_port")
    private String vrouterPort;

    @Column(name = "introspect_port")
    private String introspectPort;

    public TungstenProviderVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    public TungstenProviderVO(long zoneId, String providerName, long hostId, String port, String hostname, String gateway, String vrouterPort, String introspectPort) {
        this.zoneId = zoneId;
        this.uuid = UUID.randomUUID().toString();
        this.providerName = providerName;
        this.port = port;
        this.hostname = hostname;
        this.gateway = gateway;
        this.vrouterPort = vrouterPort;
        this.introspectPort = introspectPort;
        this.hostId = hostId;
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
    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    @Override
    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    @Override
    public long getZoneId() {
        return zoneId;
    }

    public void setZoneId(final long zoneId) {
        this.zoneId = zoneId;
    }

    @Override
    public String getGateway() {
        return gateway;
    }

    public void setGateway(final String gateway) {
        this.gateway = gateway;
    }

    @Override
    public String getIntrospectPort() {
        return introspectPort;
    }

    public void setIntrospectPort(final String introspectPort) {
        this.introspectPort = introspectPort;
    }

    @Override
    public String getVrouterPort() {
        return vrouterPort;
    }

    public void setVrouterPort(final String vrouterPort) {
        this.vrouterPort = vrouterPort;
    }

    @Override
    public long getHostId() {
        return hostId;
    }

    public void setHostId(long hostId) {
        this.hostId = hostId;
    }
}
