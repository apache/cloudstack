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

import com.cloud.vm.VmStats;

public class VmStatsEntryBase implements VmStats {

    private long vmId;
    private double cpuUtilization;
    private double networkReadKBs;
    private double networkWriteKBs;
    private double diskReadIOs;
    private double diskWriteIOs;
    private double diskReadKBs;
    private double diskWriteKBs;
    private double memoryKBs;
    private double intFreeMemoryKBs;
    private double targetMemoryKBs;
    private int numCPUs;
    private String entityType;

    public VmStatsEntryBase() {

    }

    /**
     * Creates an instance of {@code VmStatsEntryBase} with all the stats attributes filled in.
     *
     * @param memoryKBs the memory total (in KBs).
     * @param intFreeMemoryKBs the internal free memory (in KBs).
     * @param targetMemoryKBs the target memory (in KBs).
     * @param cpuUtilization the CPU utilization.
     * @param networkReadKBs the network read (in KiBs).
     * @param networkWriteKBs the network write (in KiBs).
     * @param numCPUs the number of CPUs.
     * @param diskReadKBs the disk read (in KiBs).
     * @param diskWriteKBs the disk write (in KiBs).
     * @param diskReadIOs the disk read I/O.
     * @param diskWriteIOs the disk write I/O.
     * @param entityType the entity type.
     */
    public VmStatsEntryBase(long vmId, double memoryKBs, double intFreeMemoryKBs, double targetMemoryKBs, double cpuUtilization, double networkReadKBs, double networkWriteKBs, int numCPUs,
            double diskReadKBs, double diskWriteKBs, double diskReadIOs, double diskWriteIOs, String entityType) {
        this.vmId = vmId;
        this.memoryKBs = memoryKBs;
        this.intFreeMemoryKBs = intFreeMemoryKBs;
        this.targetMemoryKBs = targetMemoryKBs;
        this.cpuUtilization = cpuUtilization;
        this.networkReadKBs = networkReadKBs;
        this.networkWriteKBs = networkWriteKBs;
        this.numCPUs = numCPUs;
        this.diskReadKBs = diskReadKBs;
        this.diskWriteKBs = diskWriteKBs;
        this.diskReadIOs = diskReadIOs;
        this.diskWriteIOs = diskWriteIOs;
        this.entityType = entityType;
    }


    public long getVmId() {
        return vmId;
    }

    public void setVmId(long vmId) {
        this.vmId = vmId;
    }

    @Override
    public double getCPUUtilization() {
        return cpuUtilization;
    }

    public void setCPUUtilization(double cpuUtilization) {
        this.cpuUtilization = cpuUtilization;
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
    public double getDiskReadIOs() {
        return diskReadIOs;
    }

    public void setDiskReadIOs(double diskReadIOs) {
        this.diskReadIOs = diskReadIOs;
    }

    @Override
    public double getDiskWriteIOs() {
        return diskWriteIOs;
    }

    public void setDiskWriteIOs(double diskWriteIOs) {
        this.diskWriteIOs = diskWriteIOs;
    }

    @Override
    public double getDiskReadKBs() {
        return diskReadKBs;
    }

    public void setDiskReadKBs(double diskReadKBs) {
        this.diskReadKBs = diskReadKBs;
    }

    @Override
    public double getDiskWriteKBs() {
        return diskWriteKBs;
    }

    public void setDiskWriteKBs(double diskWriteKBs) {
        this.diskWriteKBs = diskWriteKBs;
    }

    @Override
    public double getMemoryKBs() {
        return memoryKBs;
    }

    public void setMemoryKBs(double memoryKBs) {
        this.memoryKBs = memoryKBs;
    }

    @Override
    public double getIntFreeMemoryKBs() {
        return intFreeMemoryKBs;
    }

    public void setIntFreeMemoryKBs(double intFreeMemoryKBs) {
        this.intFreeMemoryKBs = intFreeMemoryKBs;
    }

    @Override
    public double getTargetMemoryKBs() {
        return targetMemoryKBs;
    }

    public void setTargetMemoryKBs(double targetMemoryKBs) {
        this.targetMemoryKBs = targetMemoryKBs;
    }

    public int getNumCPUs() {
        return numCPUs;
    }

    public void setNumCPUs(int numCPUs) {
        this.numCPUs = numCPUs;
    }

    public String getEntityType() {
        return this.entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

}
