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

import com.cloud.vm.UserVmVO;

public class VmStatsEntry extends VmStatsEntryBase {

    private UserVmVO userVmVO;

    public VmStatsEntry() {

    }

    /**
     * Creates an instance of {@code VmStatsEntry} with all the stats attributes filled in.
     *
     * @param vmId the VM ID.
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
    public VmStatsEntry(long vmId, double memoryKBs, double intFreeMemoryKBs, double targetMemoryKBs, double cpuUtilization, double networkReadKBs, double networkWriteKBs, int numCPUs,
            double diskReadKBs, double diskWriteKBs, double diskReadIOs, double diskWriteIOs, String entityType) {
        super(vmId, memoryKBs, intFreeMemoryKBs, targetMemoryKBs, cpuUtilization, networkReadKBs, networkWriteKBs, numCPUs, diskReadKBs, diskWriteKBs, diskReadIOs, diskWriteIOs,
                entityType);
    }

    public UserVmVO getUserVmVO() {
        return userVmVO;
    }

    public void setUserVmVO(UserVmVO userVmVO) {
        this.userVmVO = userVmVO;
    }

}
