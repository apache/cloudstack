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

    private double cpuUtilization;
    private double totalMemoryBytes;
    private double freeMemoryBytes;
    private double processMemoryBytes;

    public long getManagementServerHostId() {
        return managementServerHostId;
    }

    public void setManagementServerHostId(long managementServerHostId) {
        this.managementServerHostId = managementServerHostId;
    }

    public ManagementServerHostVO getManagementServerHostVO() {
        return managementServerHostVO;
    }

    public void setManagementServerHostVO(ManagementServerHostVO managementServerHostVO) {
        this.managementServerHostVO = managementServerHostVO;
    }

    public double getCpuUtilization() {
        return cpuUtilization;
    }

    public double getTotalMemoryBytes() {
        return totalMemoryBytes;
    }

    public double getFreeMemoryBytes() {
        return freeMemoryBytes;
    }

    public double getProcessMemoryBytes() {
        return processMemoryBytes;
    }

    public void setCpuUtilization(double cpuUtilization) {
        this.cpuUtilization = cpuUtilization;
    }

    public void setTotalMemoryBytes(double totalMemoryBytes) {
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
}
