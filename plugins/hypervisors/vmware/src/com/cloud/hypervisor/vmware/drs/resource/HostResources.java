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
package com.cloud.hypervisor.vmware.drs.resource;

import java.util.ArrayList;
import java.util.List;

public class HostResources {

    private Long id;
    private int cpus; // Number of cores
    private double speedMhz; // Each core speed in Mhz
    private double cpuUsedMhz; // Mhz used
    private double cpuTotalMhz; // Total Mhz
    private double normalizedCpuUsage;
    private long memoryUsedMb;
    private long memoryTotalMb;
    private List<VmResources> vms;

    public HostResources(Long id, double cpuUsed, int cpus, double cpuSpeed, double normalizedCpuUsage,
            long memoryUsedMb, long memoryTotalMb, List<VmResources> hostVmResources) {
        this.id = id;
        this.cpuUsedMhz = cpuUsed;
        this.cpus = cpus;
        this.speedMhz = cpuSpeed;
        this.cpuTotalMhz = cpus * speedMhz;
        this.normalizedCpuUsage = normalizedCpuUsage;
        this.memoryUsedMb = memoryUsedMb;
        this.memoryTotalMb = memoryTotalMb;
        this.vms = new ArrayList<VmResources>(hostVmResources);
    }

    public Long getId() {
        return id;
    }
    public double getCpuUsedMhz() {
        return cpuUsedMhz;
    }
    public int getCpus() {
        return cpus;
    }
    public double getCpuSpeedMhz() {
        return speedMhz;
    }
    public double getTotalCpuSpeedMhz() {
        return cpuTotalMhz;
    }
    public double getNormalizedCpuUsage() {
        return normalizedCpuUsage;
    }
    public List<VmResources> getVms() {
        return vms;
    }
    public void setCpuUsedMhz(double cpuUsedMhz) {
        this.cpuUsedMhz = cpuUsedMhz;
    }
    public void setNormalizedCpuUsage(double normalizedCpuUsage) {
        this.normalizedCpuUsage = normalizedCpuUsage;
    }
    public long getMemoryUsedMb() {
        return memoryUsedMb;
    }
    public long getMemoryTotalMb() {
        return memoryTotalMb;
    }
    public void setMemoryUsedMb(long memoryUsedMb) {
        this.memoryUsedMb = memoryUsedMb;
    }

}
