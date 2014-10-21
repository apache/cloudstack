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
package com.cloud.usage;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name = "external_public_ip_statistics")
@PrimaryKeyJoinColumn(name = "id")
public class ExternalPublicIpStatisticsVO implements InternalIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "data_center_id", updatable = false)
    private long zoneId;

    @Column(name = "account_id", updatable = false)
    private long accountId;

    @Column(name = "public_ip_address")
    private String publicIpAddress;

    @Column(name = "current_bytes_received")
    private long currentBytesReceived;

    @Column(name = "current_bytes_sent")
    private long currentBytesSent;

    protected ExternalPublicIpStatisticsVO() {
    }

    public ExternalPublicIpStatisticsVO(long zoneId, long accountId, String publicIpAddress) {
        this.zoneId = zoneId;
        this.accountId = accountId;
        this.publicIpAddress = publicIpAddress;
        this.currentBytesReceived = 0;
        this.currentBytesSent = 0;
    }

    @Override
    public long getId() {
        return id;
    }

    public long getZoneId() {
        return zoneId;
    }

    public long getAccountId() {
        return accountId;
    }

    public String getPublicIpAddress() {
        return publicIpAddress;
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

}
