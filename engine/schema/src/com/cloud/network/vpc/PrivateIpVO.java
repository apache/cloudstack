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
package com.cloud.network.vpc;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name = "private_ip_address")
public class PrivateIpVO implements InternalIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    long id;

    @Column(name = "ip_address", updatable = false, nullable = false)
    String ipAddress;

    @Column(name = "mac_address")
    private long macAddress;

    @Column(name = "taken")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date takenAt;

    @Column(name = "network_id", updatable = false, nullable = false)
    private long networkId;

    @Column(name = "vpc_id")
    private Long vpcId;

    @Column(name = "source_nat")
    private boolean sourceNat;

    public PrivateIpVO() {
    }

    public PrivateIpVO(String ipAddress, long networkId, long macAddress, long vpcId, boolean sourceNat) {
        this.ipAddress = ipAddress;
        this.networkId = networkId;
        this.macAddress = macAddress;
        this.vpcId = vpcId;
        this.sourceNat = sourceNat;
    }

    public void setTakenAt(Date takenDate) {
        this.takenAt = takenDate;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public long getNetworkId() {
        return networkId;
    }

    public Date getTakenAt() {
        return takenAt;
    }

    @Override
    public long getId() {
        return id;
    }

    public long getMacAddress() {
        return macAddress;
    }

    public Long getVpcId() {
        return vpcId;
    }

    public boolean getSourceNat() {
        return sourceNat;
    }

}
