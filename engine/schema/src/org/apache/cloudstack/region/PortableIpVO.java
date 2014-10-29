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

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "portable_ip_address")
public class PortableIpVO implements PortableIp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @Column(name = "region_id")
    int regionId;

    @Column(name = "allocated")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date allocatedTime;

    @Column(name = "account_id")
    private Long allocatedToAccountId = null;

    @Column(name = "domain_id")
    private Long allocatedInDomainId = null;

    @Column(name = "state")
    private State state;

    @Column(name = "vlan")
    String vlan;

    @Column(name = "gateway")
    String gateway;

    @Column(name = "netmask")
    String netmask;

    @Column(name = "portable_ip_address")
    String address;

    @Column(name = "portable_ip_range_id")
    private long rangeId;

    @Column(name = "physical_network_id")
    private Long physicalNetworkId;

    @Column(name = "data_center_id")
    private Long dataCenterId;

    @Column(name = "network_id")
    private Long networkId;

    @Column(name = "vpc_id")
    private Long vpcId;

    public PortableIpVO() {

    }

    public PortableIpVO(int regionId, Long rangeId, String vlan, String gateway, String netmask, String address) {
        this.regionId = regionId;
        this.vlan = vlan;
        this.gateway = gateway;
        this.netmask = netmask;
        this.address = address;
        state = State.Free;
        this.rangeId = rangeId;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public Long getAllocatedToAccountId() {
        return allocatedToAccountId;
    }

    public void setAllocatedToAccountId(Long accountId) {
        this.allocatedToAccountId = accountId;
    }

    @Override
    public Long getAllocatedInDomainId() {
        return allocatedInDomainId;
    }

    public void setAllocatedInDomainId(Long domainId) {
        this.allocatedInDomainId = domainId;
    }

    @Override
    public Date getAllocatedTime() {
        return allocatedTime;
    }

    public void setAllocatedTime(Date date) {
        this.allocatedTime = date;
    }

    @Override
    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    @Override
    public int getRegionId() {
        return regionId;
    }

    public void setRegionId(int regionId) {
        this.regionId = regionId;
    }

    @Override
    public Long getAssociatedDataCenterId() {
        return dataCenterId;
    }

    public void setAssociatedDataCenterId(Long datacenterId) {
        this.dataCenterId = datacenterId;
    }

    @Override
    public Long getAssociatedWithNetworkId() {
        return networkId;
    }

    public void setAssociatedWithNetworkId(Long networkId) {
        this.networkId = networkId;
    }

    @Override
    public Long getAssociatedWithVpcId() {
        return vpcId;
    }

    public void setAssociatedWithVpcId(Long vpcId) {
        this.vpcId = vpcId;
    }

    @Override
    public Long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    public void setPhysicalNetworkId(Long physicalNetworkId) {
        this.physicalNetworkId = physicalNetworkId;
    }

    @Override
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String getVlan() {
        return vlan;
    }

    public void setVlan(String vlan) {
        this.vlan = vlan;
    }

    @Override
    public String getNetmask() {
        return netmask;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    @Override
    public String getGateway() {
        return gateway;
    }

    public void setGateay(String gateway) {
        this.gateway = gateway;
    }

    Long getRangeId() {
        return rangeId;
    }

    public void setRangeId(Long rangeId) {
        this.rangeId = rangeId;
    }
}
