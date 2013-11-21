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
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "usage_network")
public class UsageNetworkVO {
    @Id
    @Column(name = "account_id")
    private long accountId;

    @Column(name = "zone_id")
    private long zoneId;

    @Column(name = "host_id")
    private long hostId;

    @Column(name = "host_type")
    private String hostType;

    @Column(name = "network_id")
    private Long networkId;

    @Column(name = "bytes_sent")
    private long bytesSent;

    @Column(name = "bytes_received")
    private long bytesReceived;

    @Column(name = "agg_bytes_received")
    private long aggBytesReceived;

    @Column(name = "agg_bytes_sent")
    private long aggBytesSent;

    @Column(name = "event_time_millis")
    private long eventTimeMillis = 0;

    protected UsageNetworkVO() {
    }

    public UsageNetworkVO(Long accountId, long zoneId, long hostId, String hostType, Long networkId, long bytesSent, long bytesReceived, long aggBytesReceived,
            long aggBytesSent, long eventTimeMillis) {
        this.accountId = accountId;
        this.zoneId = zoneId;
        this.hostId = hostId;
        this.hostType = hostType;
        this.networkId = networkId;
        this.bytesSent = bytesSent;
        this.bytesReceived = bytesReceived;
        this.aggBytesReceived = aggBytesReceived;
        this.aggBytesSent = aggBytesSent;
        this.eventTimeMillis = eventTimeMillis;
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public long getZoneId() {
        return zoneId;
    }

    public void setZoneId(long zoneId) {
        this.zoneId = zoneId;
    }

    public Long getBytesSent() {
        return bytesSent;
    }

    public void setBytesSent(Long bytesSent) {
        this.bytesSent = bytesSent;
    }

    public Long getBytesReceived() {
        return bytesReceived;
    }

    public void setBytes(Long bytesReceived) {
        this.bytesReceived = bytesReceived;
    }

    public long getEventTimeMillis() {
        return eventTimeMillis;
    }

    public void setEventTimeMillis(long eventTimeMillis) {
        this.eventTimeMillis = eventTimeMillis;
    }

    public void setHostId(long hostId) {
        this.hostId = hostId;
    }

    public long getHostId() {
        return hostId;
    }

    public String getHostType() {
        return hostType;
    }

    public Long getNetworkId() {
        return networkId;
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
