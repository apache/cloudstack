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

public enum DiskControllerType {
    ide,
    scsi,
    osdefault,
    lsilogic,
    lsisas1068,
    buslogic,
    pvscsi,
    none;
    public static DiskControllerType getType(String diskController) {
        if (diskController == null || diskController.equalsIgnoreCase("osdefault")) {
            return DiskControllerType.osdefault;
        } else if (diskController.equalsIgnoreCase("vim.vm.device.VirtualLsiLogicSASController") || diskController.equalsIgnoreCase("VirtualLsiLogicSASController")
                || diskController.equalsIgnoreCase(ScsiDiskControllerType.LSILOGIC_SAS)) {
            return DiskControllerType.lsisas1068;
        } else if (diskController.equalsIgnoreCase("vim.vm.device.VirtualLsiLogicController") || diskController.equalsIgnoreCase("VirtualLsiLogicController")
                || diskController.equalsIgnoreCase(ScsiDiskControllerType.LSILOGIC_PARALLEL) || diskController.equalsIgnoreCase("scsi")) {
            return DiskControllerType.lsilogic;
        } else if (diskController.equalsIgnoreCase("vim.vm.device.VirtualIDEController") || diskController.equalsIgnoreCase("VirtualIDEController")
                || diskController.equalsIgnoreCase("ide")) {
            return DiskControllerType.ide;
        } else if (diskController.equalsIgnoreCase("vim.vm.device.ParaVirtualSCSIController") || diskController.equalsIgnoreCase("ParaVirtualSCSIController")
                || diskController.equalsIgnoreCase(ScsiDiskControllerType.VMWARE_PARAVIRTUAL)) {
            return DiskControllerType.pvscsi;
        } else if (diskController.equalsIgnoreCase("vim.vm.device.VirtualBusLogicController") || diskController.equalsIgnoreCase("VirtualBusLogicController")
                || diskController.equalsIgnoreCase(ScsiDiskControllerType.BUSLOGIC)) {
            return DiskControllerType.buslogic;
        } else {
            return DiskControllerType.none;
        }
    }
}
