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
package org.apache.cloudstack.vm;

public class VmwareCbtPreflightDiskInfo extends VmwareCbtDiskInfo {

    private final String backingType;
    private final String diskMode;
    private final String rdmCompatibilityMode;
    private final boolean independentDisk;
    private final boolean physicalRdm;

    public VmwareCbtPreflightDiskInfo(String sourceDiskId, Integer sourceDiskDeviceKey, String label,
                                      String sourceDiskPath, String datastoreName, Long capacityBytes,
                                      String changeId, String backingType, String diskMode,
                                      String rdmCompatibilityMode, boolean independentDisk,
                                      boolean physicalRdm) {
        super(sourceDiskId, sourceDiskDeviceKey, label, sourceDiskPath, datastoreName, capacityBytes, changeId);
        this.backingType = backingType;
        this.diskMode = diskMode;
        this.rdmCompatibilityMode = rdmCompatibilityMode;
        this.independentDisk = independentDisk;
        this.physicalRdm = physicalRdm;
    }

    public String getBackingType() {
        return backingType;
    }

    public String getDiskMode() {
        return diskMode;
    }

    public String getRdmCompatibilityMode() {
        return rdmCompatibilityMode;
    }

    public boolean isIndependentDisk() {
        return independentDisk;
    }

    public boolean isPhysicalRdm() {
        return physicalRdm;
    }
}
