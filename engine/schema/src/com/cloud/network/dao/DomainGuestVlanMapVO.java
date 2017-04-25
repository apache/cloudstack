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
package com.cloud.network.dao;

import com.cloud.network.GuestVlanDomain;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;


import java.util.UUID;

@Entity
@Table(name = "domain_vnet_map")
public class DomainGuestVlanMapVO implements GuestVlanDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "domain_id")
    private long domainId;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "vnet_range")
    private String guestVlanRange;

    @Column(name = "physical_network_id")
    private long physicalNetworkId;


    public DomainGuestVlanMapVO(long domainId, long physicalNetworkId) {
        this.domainId = domainId;
        this.physicalNetworkId = physicalNetworkId;
        this.guestVlanRange = null;
        this.uuid = UUID.randomUUID().toString();
    }

    public DomainGuestVlanMapVO() {

    }

    @Override
    public long getId() {
        return id;
    }

    public long getDomainId() {
        return domainId;
    }

    public Long getAccountId() {return null;}

    public String getGuestVlanRange() {
        return guestVlanRange;
    }

    public void setGuestVlanRange(String guestVlanRange) {
        this.guestVlanRange = guestVlanRange;
    }

    public String getUuid() {
        return this.uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public long getPhysicalNetworkId() {
        return this.physicalNetworkId;
    }

    public void setPhysicalNetworkId(long physicalNetworkId) {
        this.physicalNetworkId = physicalNetworkId;
    }

}
