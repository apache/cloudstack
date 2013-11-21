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
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name = "op_dc_vnet_alloc")
public class DataCenterVnetVO implements InternalIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    long id;

    @Column(name = "taken", nullable = true)
    @Temporal(value = TemporalType.TIMESTAMP)
    Date takenAt;

    @Column(name = "vnet", updatable = false, nullable = false)
    protected String vnet;

    @Column(name = "physical_network_id", updatable = false, nullable = false)
    protected long physicalNetworkId;

    @Column(name = "data_center_id", updatable = false, nullable = false)
    protected long dataCenterId;

    @Column(name = "account_id")
    protected Long accountId;

    @Column(name = "reservation_id")
    protected String reservationId;

    @Column(name = "account_vnet_map_id")
    protected Long accountGuestVlanMapId;

    public Date getTakenAt() {
        return takenAt;
    }

    public void setTakenAt(Date taken) {
        this.takenAt = taken;
    }

    public DataCenterVnetVO(String vnet, long dcId, long physicalNetworkId) {
        this.vnet = vnet;
        this.dataCenterId = dcId;
        this.physicalNetworkId = physicalNetworkId;
        this.takenAt = null;
    }

    @Override
    public long getId() {
        return id;
    }

    public String getVnet() {
        return vnet;
    }

    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public long getDataCenterId() {
        return dataCenterId;
    }

    public long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    public void setAccountGuestVlanMapId(Long accountGuestVlanMapId) {
        this.accountGuestVlanMapId = accountGuestVlanMapId;
    }

    public Long getAccountGuestVlanMapId() {
        return accountGuestVlanMapId;
    }

    protected DataCenterVnetVO() {
    }
}
