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

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;
import javax.persistence.SecondaryTables;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name = "op_dc_storage_network_ip_address")
@SecondaryTables({@SecondaryTable(name = "dc_storage_network_ip_range", pkJoinColumns = {@PrimaryKeyJoinColumn(name = "range_id", referencedColumnName = "id")})})
public class StorageNetworkIpAddressVO implements InternalIdentity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    long id;

    @Column(name = "range_id")
    long rangeId;

    @Column(name = "ip_address", updatable = false, nullable = false)
    String ipAddress;

    @Column(name = "taken")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date takenAt;

    @Column(name = "netmask", table = "dc_storage_network_ip_range", insertable = false, updatable = false)
    private String netmask;

    @Column(name = "mac_address")
    long mac;

    @Column(name = "vlan", table = "dc_storage_network_ip_range", insertable = false, updatable = false)
    Integer vlan;

    @Column(name = "gateway", table = "dc_storage_network_ip_range", insertable = false, updatable = false)
    String gateway;

    protected StorageNetworkIpAddressVO() {
    }

    @Override
    public long getId() {
        return id;
    }

    public void setTakenAt(Date takenDate) {
        this.takenAt = takenDate;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ip) {
        this.ipAddress = ip;
    }

    public Date getTakenAt() {
        return takenAt;
    }

    public long getRangeId() {
        return rangeId;
    }

    public void setRangeId(long id) {
        this.rangeId = id;
    }

    public long getMac() {
        return mac;
    }

    public void setMac(long mac) {
        this.mac = mac;
    }

    public String getNetmask() {
        return netmask;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    public Integer getVlan() {
        return vlan;
    }

    public String getGateway() {
        return gateway;
    }
}
