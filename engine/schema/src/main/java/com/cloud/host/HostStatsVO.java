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
package com.cloud.host;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;

/** One persisted historical host-stats sample. */
@Entity
@Table(name = "host_stats")
public class HostStatsVO {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    protected long id;

    @Column(name = "host_id", updatable = false, nullable = false)
    protected Long hostId;

    @Column(name = "mgmt_server_id", updatable = false, nullable = false)
    protected Long mgmtServerId;

    @Column(name = "timestamp", updatable = false)
    @Temporal(value = TemporalType.TIMESTAMP)
    protected Date timestamp;

    @Column(name = "host_stats_data", updatable = false, nullable = false, length = 65535)
    protected String hostStatsData;

    public HostStatsVO(Long hostId, Long mgmtServerId, Date timestamp, String hostStatsData) {
        this.hostId = hostId;
        this.mgmtServerId = mgmtServerId;
        this.timestamp = timestamp;
        this.hostStatsData = hostStatsData;
    }

    public HostStatsVO() {

    }

    public long getId() {
        return id;
    }

    public Long getHostId() {
        return hostId;
    }

    public Long getMgmtServerId() {
        return mgmtServerId;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getHostStatsData() {
        return hostStatsData;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilderUtils.reflectOnlySelectedFields(this, "hostId", "mgmtServerId", "timestamp", "hostStatsData");
    }

}
