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
package com.cloud.hypervisor.vmware.drs;

import com.cloud.hypervisor.vmware.drs.resource.VmResources;

public class VmwareDrsInternalBestMove {

    private int hostFromIndex;
    private int hostToIndex;
    private int vmIndex;
    private VmResources vm;
    private boolean updated = false;
    private long vmId;
    private long hostId;
    private double stdDev;

    public VmwareDrsInternalBestMove(double stdDev) {
       this.stdDev = stdDev;
    }

    public int getHostFromIndex() {
        return hostFromIndex;
    }
    public void setHostFromIndex(int hostFromIndex) {
        this.hostFromIndex = hostFromIndex;
    }
    public int getHostToIndex() {
        return hostToIndex;
    }
    public void setHostToIndex(int hostToIndex) {
        this.hostToIndex = hostToIndex;
    }
    public int getVmIndex() {
        return vmIndex;
    }
    public void setVmIndex(int vmIndex) {
        this.vmIndex = vmIndex;
    }
    public VmResources getVm() {
        return vm;
    }
    public void setVm(VmResources vm) {
        this.vm = vm;
    }
    public boolean isUpdated() {
        return updated;
    }
    public void setUpdated(boolean updated) {
        this.updated = updated;
    }
    public long getVmId() {
        return vmId;
    }
    public void setVmId(long vmId) {
        this.vmId = vmId;
    }
    public long getHostId() {
        return hostId;
    }
    public void setHostId(long hostId) {
        this.hostId = hostId;
    }
    public double getStdDev() {
        return stdDev;
    }
    public void setStdDev(double stdDev) {
        this.stdDev = stdDev;
    }
}
