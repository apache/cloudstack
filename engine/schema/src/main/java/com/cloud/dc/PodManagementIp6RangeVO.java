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
@Table(name = "pod_ip6_range")
public class PodManagementIp6RangeVO implements PodManagementIp6Range {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    String uuid;

    @Column(name = "data_center_id")
    private long dataCenterId;

    @Column(name = "pod_id")
    private long podId;

    @Column(name = "gateway")
    private String gateway;

    @Column(name = "cidr")
    private String cidr;

    @Column(name = "vlan")
    private Integer vlan;

    @Column(name = "start_ip")
    private String startIp;

    @Column(name = "end_ip")
    private String endIp;

    @Column(name = "created")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date created = null;

    public PodManagementIp6RangeVO(long dcId, long podId, String gateway, String cidr, Integer vlan, String startIp, String endIp) {
        this();
        this.dataCenterId = dcId;
        this.podId = podId;
        this.gateway = gateway;
        this.cidr = cidr;
        this.vlan = vlan;
        this.startIp = startIp;
        this.endIp = endIp;
        this.created = new Date();
    }

    protected PodManagementIp6RangeVO() {
        this.uuid = UUID.randomUUID().toString();
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
    public Long getDataCenterId() {
        return dataCenterId;
    }

    public void setDataCenterId(long dcId) {
        this.dataCenterId = dcId;
    }

    @Override
    public Long getPodId() {
        return podId;
    }

    public void setPodId(long podId) {
        this.podId = podId;
    }

    @Override
    public String getGateway() {
        return this.gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    @Override
    public String getCidr() {
        return cidr;
    }

    public void setCidr(String cidr) {
        this.cidr = cidr;
    }

    @Override
    public Integer getVlan() {
        return vlan;
    }

    public void setVlan(int vlan) {
        this.vlan = vlan;
    }

    @Override
    public String getStartIp() {
        return startIp;
    }

    public void setStartIp(String start) {
        this.startIp = start;
    }

    @Override
    public String getEndIp() {
        return endIp;
    }

    public void setEndIp(String end) {
        this.endIp = end;
    }

    public Date getCreated() {
        return created;
    }
}