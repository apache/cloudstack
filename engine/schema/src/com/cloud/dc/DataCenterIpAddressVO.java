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
@Table(name = "op_dc_ip_address_alloc")
public class DataCenterIpAddressVO implements InternalIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    long id;

    @Column(name = "ip_address", updatable = false, nullable = false)
    String ipAddress;

    @Column(name = "taken")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date takenAt;

    @Column(name = "data_center_id", updatable = false, nullable = false)
    private long dataCenterId;

    @Column(name = "pod_id", updatable = false, nullable = false)
    private long podId;

    @Column(name = "reservation_id")
    String reservationId;

    @Column(name = "nic_id")
    private Long instanceId;

    @Column(name = "mac_address")
    long macAddress;

    protected DataCenterIpAddressVO() {
    }

    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public DataCenterIpAddressVO(String ipAddress, long dataCenterId, long podId) {
        this.ipAddress = ipAddress;
        this.dataCenterId = dataCenterId;
        this.podId = podId;
    }

    @Override
    public long getId() {
        return id;
    }

    public Long getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(Long instanceId) {
        this.instanceId = instanceId;
    }

    public long getPodId() {
        return podId;
    }

    public void setTakenAt(Date takenDate) {
        this.takenAt = takenDate;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public long getDataCenterId() {
        return dataCenterId;
    }

    public Date getTakenAt() {
        return takenAt;
    }

    public long getMacAddress() {
        return macAddress;
    }
}
