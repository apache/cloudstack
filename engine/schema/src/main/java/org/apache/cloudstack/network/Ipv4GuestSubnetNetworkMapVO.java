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

package org.apache.cloudstack.network;

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

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "ip4_guest_subnet_network_map")
public class Ipv4GuestSubnetNetworkMapVO implements Ipv4GuestSubnetNetworkMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "subnet")
    private String subnet;

    @Column(name = "vpc_id")
    private Long vpcId;

    @Column(name = "network_id")
    private Long networkId;

    @Column(name = "state")
    private State state;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "allocated")
    Date allocated;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name= GenericDao.REMOVED_COLUMN)
    private Date removed;

    protected Ipv4GuestSubnetNetworkMapVO() {
        uuid = UUID.randomUUID().toString();
    }

    protected Ipv4GuestSubnetNetworkMapVO(Long parentId, String subnet, Long networkId, Ipv4GuestSubnetNetworkMap.State state) {
        this.parentId = parentId;
        this.subnet = subnet;
        this.networkId = networkId;
        this.state = state;
        uuid = UUID.randomUUID().toString();
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
    public Long getParentId() {
        return parentId;
    }

    @Override
    public String getSubnet() {
        return subnet;
    }

    @Override
    public Long getVpcId() {
        return vpcId;
    }

    public void setVpcId(Long vpcId) {
        this.vpcId = vpcId;
    }

    @Override
    public Long getNetworkId() {
        return networkId;
    }

    public void setNetworkId(Long networkId) {
        this.networkId = networkId;
    }

    @Override
    public State getState() {
        return state;
    }

    public void setState(Ipv4GuestSubnetNetworkMap.State state) {
        this.state = state;
    }

    public void setAllocated(Date allocated) {
        this.allocated = allocated;
    }

    @Override
    public Date getAllocated() {
        return allocated;
    }

    @Override
    public Date getCreated() {
        return created;
    }
}
