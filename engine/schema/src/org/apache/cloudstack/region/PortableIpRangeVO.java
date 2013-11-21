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
package org.apache.cloudstack.region;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "portable_ip_range")
public class PortableIpRangeVO implements PortableIpRange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @Column(name = "uuid")
    String uuid;

    @Column(name = "region_id")
    int regionId;

    @Column(name = "vlan_id")
    String vlan;

    @Column(name = "gateway")
    String gateway;

    @Column(name = "netmask")
    String netmask;

    @Column(name = "start_ip")
    String startIp;

    @Column(name = "end_ip")
    String endIp;

    public PortableIpRangeVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    public PortableIpRangeVO(int regionId, String vlan, String gateway, String netmask, String startIp, String endIp) {
        this.uuid = UUID.randomUUID().toString();
        this.regionId = regionId;
        this.vlan = vlan;
        this.gateway = gateway;
        this.netmask = netmask;
        this.startIp = startIp;
        this.endIp = endIp;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return this.uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getVlanTag() {
        return vlan;
    }

    public void setVlanTag(String vlan) {
        this.vlan = vlan;
    }

    @Override
    public String getGateway() {
        return gateway;
    }

    @Override
    public String getNetmask() {
        return netmask;
    }

    @Override
    public int getRegionId() {
        return regionId;
    }

    @Override
    public String getIpRange() {
        return startIp + "-" + endIp;
    }
}
