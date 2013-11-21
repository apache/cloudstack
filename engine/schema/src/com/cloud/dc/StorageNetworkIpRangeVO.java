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

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;
import javax.persistence.SecondaryTables;
import javax.persistence.Table;

@Entity
@Table(name = "dc_storage_network_ip_range")
@SecondaryTables({@SecondaryTable(name = "networks", pkJoinColumns = {@PrimaryKeyJoinColumn(name = "network_id", referencedColumnName = "id")}),
    @SecondaryTable(name = "host_pod_ref", pkJoinColumns = {@PrimaryKeyJoinColumn(name = "pod_id", referencedColumnName = "id")}),
    @SecondaryTable(name = "data_center", pkJoinColumns = {@PrimaryKeyJoinColumn(name = "data_center_id", referencedColumnName = "id")})})
public class StorageNetworkIpRangeVO implements StorageNetworkIpRange {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    String uuid;

    @Column(name = "vlan")
    private Integer vlan;

    @Column(name = "data_center_id")
    private long dataCenterId;

    @Column(name = "pod_id")
    private long podId;

    @Column(name = "start_ip")
    private String startIp;

    @Column(name = "end_ip")
    private String endIp;

    @Column(name = "gateway")
    private String gateway;

    @Column(name = "network_id")
    private long networkId;

    @Column(name = "netmask")
    private String netmask;

    @Column(name = "uuid", table = "networks", insertable = false, updatable = false)
    String networkUuid;

    @Column(name = "uuid", table = "host_pod_ref", insertable = false, updatable = false)
    String podUuid;

    @Column(name = "uuid", table = "data_center", insertable = false, updatable = false)
    String zoneUuid;

    public StorageNetworkIpRangeVO(long dcId, long podId, long networkId, String startIp, String endIp, Integer vlan, String netmask, String gateway) {
        this();
        this.dataCenterId = dcId;
        this.podId = podId;
        this.networkId = networkId;
        this.startIp = startIp;
        this.endIp = endIp;
        this.vlan = vlan;
        this.netmask = netmask;
        this.gateway = gateway;
    }

    protected StorageNetworkIpRangeVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    @Override
    public long getId() {
        return id;
    }

    public long getDataCenterId() {
        return dataCenterId;
    }

    public void setDataCenterId(long dcId) {
        this.dataCenterId = dcId;
    }

    public long getPodId() {
        return podId;
    }

    public void setPodId(long podId) {
        this.podId = podId;
    }

    public long getNetworkId() {
        return networkId;
    }

    public void setNetworkId(long nwId) {
        this.networkId = nwId;
    }

    @Override
    public Integer getVlan() {
        return vlan;
    }

    public void setVlan(int vlan) {
        this.vlan = vlan;
    }

    public void setStartIp(String start) {
        this.startIp = start;
    }

    @Override
    public String getStartIp() {
        return startIp;
    }

    public void setEndIp(String end) {
        this.endIp = end;
    }

    @Override
    public String getEndIp() {
        return endIp;
    }

    @Override
    public String getNetmask() {
        return netmask;
    }

    @Override
    public String getGateway() {
        return this.gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public String getPodUuid() {
        return podUuid;
    }

    @Override
    public String getNetworkUuid() {
        return networkUuid;
    }

    @Override
    public String getZoneUuid() {
        return zoneUuid;
    }
}
