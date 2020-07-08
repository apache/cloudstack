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
import com.cloud.host.HostVO;

public class HostStatsEntry implements HostStats {

    long hostId;
    HostVO hostVo;
    String entityType;
    double cpuUtilization;
    double averageLoad;
    double networkReadKBs;
    double networkWriteKBs;
    double totalMemoryKBs;
    double freeMemoryKBs;

    public HostStatsEntry() {
    }

    public HostStatsEntry(long hostId, double cpuUtilization, double networkReadKBs, double networkWriteKBs, String entityType, double totalMemoryKBs,
            double freeMemoryKBs, double xapiMemoryUsageKBs, double averageLoad) {
        this.hostId = hostId;
        this.entityType = entityType;
        this.cpuUtilization = cpuUtilization;
        this.networkReadKBs = networkReadKBs;
        this.networkWriteKBs = networkWriteKBs;
        this.totalMemoryKBs = totalMemoryKBs;
        this.freeMemoryKBs = freeMemoryKBs;
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
    public String getEntityType() {
        return this.entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    @Override
    public double getTotalMemoryKBs() {
        return this.totalMemoryKBs;
    }

    public void setTotalMemoryKBs(double totalMemoryKBs) {
        this.totalMemoryKBs = totalMemoryKBs;
    }

    @Override
    public double getFreeMemoryKBs() {
        return this.freeMemoryKBs;
    }

    public void setFreeMemoryKBs(double freeMemoryKBs) {
        this.freeMemoryKBs = freeMemoryKBs;
    }

    @Override
    public double getCpuUtilization() {
        return this.cpuUtilization;
    }

    public void setCpuUtilization(double cpuUtilization) {
        this.cpuUtilization = cpuUtilization;
    }

    @Override
    public double getLoadAverage() {
        return this.averageLoad;
    }

    public void setAverageLoad(double cpuAvgLoad) {
        this.averageLoad = cpuAvgLoad;
    }

    @Override
    public double getUsedMemory() {
        return (totalMemoryKBs - freeMemoryKBs) * 1024;
    }

    @Override
    public HostStats getHostStats() {
        return this;
    }

    public void setHostId(long hostId) {
        this.hostId = hostId;
    }

    public long getHostId() {
        return hostId;
    }

    public HostVO getHostVo() {
        return hostVo;
    }

    public void setHostVo(HostVO hostVo) {
        this.hostVo = hostVo;
    }
}
