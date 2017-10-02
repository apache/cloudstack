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

import com.cloud.vm.VmNetworkStats;

public class VmNetworkStatsEntry implements VmNetworkStats {
    String vmName;
    String macAddress;
    long bytesSent;
    long bytesReceived;

    public VmNetworkStatsEntry() {
    }

    public VmNetworkStatsEntry(String vmName, String macAddress, long bytesSent, long bytesReceived) {
        this.bytesSent = bytesSent;
        this.bytesReceived = bytesReceived;
        this.vmName = vmName;
        this.macAddress = macAddress;
    }

    public void setVmName(String vmName) {
        this.vmName = vmName;
    }

    public String getVmName() {
        return vmName;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setBytesSent(long bytesSent) {
        this.bytesSent = bytesSent;
    }

    @Override
    public long getBytesSent() {
        return bytesSent;
    }

    public void setBytesReceived(long bytesReceived) {
        this.bytesReceived = bytesReceived;
    }

    @Override
    public long getBytesReceived() {
        return bytesReceived;
    }

}
