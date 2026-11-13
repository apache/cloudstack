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
package com.cloud.hypervisor.vmware.mo;

import com.vmware.vim25.ParaVirtualSCSIController;
import com.vmware.vim25.VirtualAHCIController;
import com.vmware.vim25.VirtualBusLogicController;
import com.vmware.vim25.VirtualIDEController;
import com.vmware.vim25.VirtualLsiLogicController;
import com.vmware.vim25.VirtualLsiLogicSASController;
import com.vmware.vim25.VirtualNVMEController;

import java.util.List;

public enum DiskControllerType {
    osdefault(null, null, null, null, null, null),
    ide(VirtualIDEController.class.getName(), "ide", 2, 2, VmdkAdapterType.ide, null),
    // The canonical controller type for VirtualLsiLogicController is "lsilogic"; "scsi" is kept as an alias.
    lsilogic(VirtualLsiLogicController.class.getName(), "scsi", 16, 4, VmdkAdapterType.lsilogic, null),
    scsi(VirtualLsiLogicController.class.getName(), "scsi", 16, 4, VmdkAdapterType.lsilogic, null),
    buslogic(VirtualBusLogicController.class.getName(), "scsi", 16, 4, VmdkAdapterType.buslogic, null),
    lsisas1068(VirtualLsiLogicSASController.class.getName(), "scsi", 16, 4, VmdkAdapterType.lsilogic, null),
    pvscsi(ParaVirtualSCSIController.class.getName(), "scsi", 16, 4, VmdkAdapterType.lsilogic, "7"),
    // The canonical controller type for VirtualAHCIController is "ahci"; "sata" is kept as an alias.
    ahci(VirtualAHCIController.class.getName(), "sata", 30, 4, VmdkAdapterType.ide, "10"),
    sata(VirtualAHCIController.class.getName(), "sata", 30, 4, VmdkAdapterType.ide, "10"),
    nvme(VirtualNVMEController.class.getName(), "nvme", 15, 4, VmdkAdapterType.ide, "13"),
    none(null, null, null, null, null, null);

    private static final List<DiskControllerType> SUPPORTED_DISK_CONTROLLERS = List.of(
            osdefault, ide, lsilogic, scsi, buslogic, lsisas1068, pvscsi, ahci, sata, nvme);

    private final String controllerReference;
    private final String busName;
    private final Integer maxDeviceCount;
    private final Integer maxControllerCount;
    private final VmdkAdapterType vmdkAdapterType;
    private final String minHardwareVersion;

    DiskControllerType(String controllerReference, String busName, Integer maxDeviceCount, Integer maxControllerCount,
                       VmdkAdapterType vmdkAdapterType, String minHardwareVersion) {
        this.controllerReference = controllerReference;
        this.busName = busName;
        this.maxDeviceCount = maxDeviceCount;
        this.maxControllerCount = maxControllerCount;
        this.vmdkAdapterType = vmdkAdapterType;
        this.minHardwareVersion = minHardwareVersion;
    }

    public static List<DiskControllerType> getSupportedDiskControllerTypes() {
        return SUPPORTED_DISK_CONTROLLERS;
    }

    public String getName() {
        return name();
    }

    public String getControllerReference() {
        return controllerReference;
    }

    public String getBusName() {
        return busName;
    }

    public Integer getMaxDeviceCount() {
        return maxDeviceCount;
    }

    public Integer getMaxControllerCount() {
        return maxControllerCount;
    }

    public VmdkAdapterType getVmdkAdapterType() {
        return vmdkAdapterType;
    }

    public String getMinHardwareVersion() {
        return minHardwareVersion;
    }
}
