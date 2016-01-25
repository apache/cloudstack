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

package org.apache.cloudstack.utils.volume;

public class VirtualMachineVolumeChainInfo {
    String diskDeviceBusName;
    String[] diskChain;

    public String getDiskDeviceBusName() {
        return this.diskDeviceBusName;
    }

    public void setDiskDeviceBusName(String diskDeviceBusName) {
        this.diskDeviceBusName = diskDeviceBusName;
    }

    public String[] getDiskChain() {
        return this.diskChain;
    }

    public void setDiskChain(String[] diskChain) {
        this.diskChain = diskChain;
    }

    public String getControllerFromDeviceBusName() {
        if ((this.diskDeviceBusName == null) || (this.diskDeviceBusName.isEmpty()) || (!this.diskDeviceBusName.contains(":"))) {
            return null;
        }
        return this.diskDeviceBusName.substring(0, this.diskDeviceBusName.indexOf(":") - 1);
    }
}
