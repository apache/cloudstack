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
package com.cloud.server;

import com.cloud.cluster.ManagementServerHostVO;

public class ManagementServerHostStatsEntry implements ManagementServerHostStats {

    private ManagementServerHostVO managementServerHostVO;
    private long managementServerHostId;

    private long sessions;
    private double cpuUtilization;
    private long totalMemoryBytes;
    private double freeMemoryBytes;
    private double processMemoryBytes;
    private long uptime;
    private long startTime;
    private int availableProcessors;
    private double loadAverage;
    long totalInit;
    long totalUsed;
    long totalMax;
    long totalCommitted;

    @Override
    public long getManagementServerHostId() {
        return managementServerHostId;
    }

    public void setManagementServerHostId(long managementServerHostId) {
        this.managementServerHostId = managementServerHostId;
    }

    public ManagementServerHostVO getManagementServerHostVO() {
        return managementServerHostVO;
    }

    @Override
    public long getSessions() {
        return sessions;
    }

    @Override
    public double getCpuUtilization() {
        return cpuUtilization;
    }

    @Override
    public long getTotalMemoryBytes() {
        return totalMemoryBytes;
    }

    @Override
    public double getFreeMemoryBytes() {
        return freeMemoryBytes;
    }

    @Override
    public double getProcessMemoryBytes() {
        return processMemoryBytes;
    }

    @Override
    public long getUptime() {
        return uptime;
    }

    @Override
    public long getStartTime() {
        return startTime;
    }

    @Override
    public int getAvailableProcessors() {
        return availableProcessors;
    }

    @Override
    public double getLoadAverage() {
        return loadAverage;
    }

    @Override
    public long getTotalInit() {
        return totalInit;
    }

    @Override
    public long getTotalUsed() {
        return totalUsed;
    }

    @Override
    public long getTotalMax() {
        return totalMax;
    }

    @Override
    public long getTotalCommitted() {
        return totalCommitted;
    }

    public void setManagementServerHostVO(ManagementServerHostVO managementServerHostVO) {
        this.managementServerHostVO = managementServerHostVO;
    }

    public void setSessions(long sessions) {
        this.sessions = sessions;
    }

    public void setCpuUtilization(double cpuUtilization) {
        this.cpuUtilization = cpuUtilization;
    }

    public void setTotalMemoryBytes(long totalMemoryBytes) {
        this.totalMemoryBytes = totalMemoryBytes;
    }

    public void setFreeMemoryBytes(double freeMemoryBytes) {
        this.freeMemoryBytes = freeMemoryBytes;
    }

    public void setProcessMemoryBytes(double processMemoryBytes) {
        this.processMemoryBytes = processMemoryBytes;
    }

    protected void validateSome() {
        assert totalMemoryBytes - processMemoryBytes > freeMemoryBytes;
    }

    public void setUptime(long uptime) {
        this.uptime = uptime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void setAvailableProcessors(int availableProcessors) {
        this.availableProcessors = availableProcessors;
    }

    public void setLoadAverage(double loadAverage) {
        this.loadAverage = loadAverage;
    }

    public void setTotalInit(long totalInit) {
        this.totalInit = totalInit;
    }

    public void setTotalUsed(long totalUsed) {
        this.totalUsed = totalUsed;
    }

    public void setTotalMax(long totalMax) {
        this.totalMax = totalMax;
    }

    public void setTotalCommitted(long totalCommitted) {
        this.totalCommitted = totalCommitted;
    }
}
