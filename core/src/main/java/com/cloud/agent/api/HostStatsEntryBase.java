//
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
//

package com.cloud.agent.api;

import com.cloud.host.HostStats;

/**
 * Serializable host-stats payload persisted as JSON in {@code host_stats.host_stats_data}. Unlike
 * {@link HostStatsEntry} it carries no {@code HostVO}, so serialization does not pull in a host entity.
 */
public class HostStatsEntryBase implements HostStats {

    private long hostId;
    private String entityType;
    private double cpuUtilization;
    private double averageLoad;
    private double networkReadKBs;
    private double networkWriteKBs;
    private double totalMemoryKBs;
    private double freeMemoryKBs;

    public HostStatsEntryBase() {
    }

    public HostStatsEntryBase(long hostId, String entityType, double cpuUtilization, double averageLoad,
            double networkReadKBs, double networkWriteKBs, double totalMemoryKBs, double freeMemoryKBs) {
        this.hostId = hostId;
        this.entityType = entityType;
        this.cpuUtilization = cpuUtilization;
        this.averageLoad = averageLoad;
        this.networkReadKBs = networkReadKBs;
        this.networkWriteKBs = networkWriteKBs;
        this.totalMemoryKBs = totalMemoryKBs;
        this.freeMemoryKBs = freeMemoryKBs;
    }

    public long getHostId() {
        return hostId;
    }

    public void setHostId(long hostId) {
        this.hostId = hostId;
    }

    @Override
    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    @Override
    public double getCpuUtilization() {
        return cpuUtilization;
    }

    public void setCpuUtilization(double cpuUtilization) {
        this.cpuUtilization = cpuUtilization;
    }

    @Override
    public double getLoadAverage() {
        return averageLoad;
    }

    public void setAverageLoad(double averageLoad) {
        this.averageLoad = averageLoad;
    }

    @Override
    public double getNetworkReadKBs() {
        return networkReadKBs;
    }

    public void setNetworkReadKBs(double networkReadKBs) {
        this.networkReadKBs = networkReadKBs;
    }

    @Override
    public double getNetworkWriteKBs() {
        return networkWriteKBs;
    }

    public void setNetworkWriteKBs(double networkWriteKBs) {
        this.networkWriteKBs = networkWriteKBs;
    }

    @Override
    public double getTotalMemoryKBs() {
        return totalMemoryKBs;
    }

    public void setTotalMemoryKBs(double totalMemoryKBs) {
        this.totalMemoryKBs = totalMemoryKBs;
    }

    @Override
    public double getFreeMemoryKBs() {
        return freeMemoryKBs;
    }

    public void setFreeMemoryKBs(double freeMemoryKBs) {
        this.freeMemoryKBs = freeMemoryKBs;
    }

    @Override
    public double getUsedMemory() {
        return (totalMemoryKBs - freeMemoryKBs) * 1024;
    }

    @Override
    public HostStats getHostStats() {
        return this;
    }
}
