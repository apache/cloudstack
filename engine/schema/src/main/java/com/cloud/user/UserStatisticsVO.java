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
package com.cloud.user;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name = "user_statistics")
public class UserStatisticsVO implements InternalIdentity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "data_center_id", updatable = false)
    private long dataCenterId;

    @Column(name = "account_id", updatable = false)
    private long accountId;

    @Column(name = "public_ip_address")
    private String publicIpAddress;

    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "device_type")
    private String deviceType;

    @Column(name = "network_id")
    private Long networkId;

    @Column(name = "net_bytes_received")
    private long netBytesReceived;

    @Column(name = "net_bytes_sent")
    private long netBytesSent;

    @Column(name = "current_bytes_received")
    private long currentBytesReceived;

    @Column(name = "current_bytes_sent")
    private long currentBytesSent;

    @Column(name = "agg_bytes_received")
    private long aggBytesReceived;

    @Column(name = "agg_bytes_sent")
    private long aggBytesSent;

    protected UserStatisticsVO() {
    }

    public UserStatisticsVO(long accountId, long dcId, String publicIpAddress, Long deviceId, String deviceType, Long networkId) {
        this.accountId = accountId;
        this.dataCenterId = dcId;
        this.publicIpAddress = publicIpAddress;
        this.deviceId = deviceId;
        this.deviceType = deviceType;
        this.networkId = networkId;
        this.netBytesReceived = 0;
        this.netBytesSent = 0;
        this.currentBytesReceived = 0;
        this.currentBytesSent = 0;
    }

    public long getAccountId() {
        return accountId;
    }

    @Override
    public long getId() {
        return id;
    }

    public long getDataCenterId() {
        return dataCenterId;
    }

    public String getPublicIpAddress() {
        return publicIpAddress;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public Long getNetworkId() {
        return networkId;
    }

    public long getCurrentBytesReceived() {
        return currentBytesReceived;
    }

    public void setCurrentBytesReceived(long currentBytesReceived) {
        this.currentBytesReceived = currentBytesReceived;
    }

    public long getCurrentBytesSent() {
        return currentBytesSent;
    }

    public void setCurrentBytesSent(long currentBytesSent) {
        this.currentBytesSent = currentBytesSent;
    }

    public long getNetBytesReceived() {
        return netBytesReceived;
    }

    public long getNetBytesSent() {
        return netBytesSent;
    }

    public void setNetBytesReceived(long netBytesReceived) {
        this.netBytesReceived = netBytesReceived;
    }

    public void setNetBytesSent(long netBytesSent) {
        this.netBytesSent = netBytesSent;
    }

    public long getAggBytesReceived() {
        return aggBytesReceived;
    }

    public void setAggBytesReceived(long aggBytesReceived) {
        this.aggBytesReceived = aggBytesReceived;
    }

    public long getAggBytesSent() {
        return aggBytesSent;
    }

    public void setAggBytesSent(long aggBytesSent) {
        this.aggBytesSent = aggBytesSent;
    }

}
