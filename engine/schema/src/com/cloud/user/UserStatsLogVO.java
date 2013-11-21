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

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "op_user_stats_log")
public class UserStatsLogVO {
    @Id
    @Column(name = "user_stats_id")
    private long userStatsId;

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

    @Column(name = "updated")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date updatedTime;

    public UserStatsLogVO() {
    }

    public UserStatsLogVO(long userStatsId, long netBytesReceived, long netBytesSent, long currentBytesReceived, long currentBytesSent, long aggBytesReceived,
            long aggBytesSent, Date updatedTime) {
        this.userStatsId = userStatsId;
        this.netBytesReceived = netBytesReceived;
        this.netBytesSent = netBytesSent;
        this.currentBytesReceived = currentBytesReceived;
        this.currentBytesSent = currentBytesSent;
        this.aggBytesReceived = aggBytesReceived;
        this.aggBytesSent = aggBytesSent;
        this.updatedTime = updatedTime;
    }

    public Long getUserStatsId() {
        return userStatsId;
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

    public Date getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(Date updatedTime) {
        this.updatedTime = updatedTime;
    }

}
