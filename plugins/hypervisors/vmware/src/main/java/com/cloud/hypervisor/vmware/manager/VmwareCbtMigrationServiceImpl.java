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
package com.cloud.hypervisor.vmware.manager;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.vm.UnmanagedInstanceTO;
import org.apache.cloudstack.vm.VmwareCbtDiskInfo;
import org.apache.cloudstack.vm.VmwareCbtMigrationService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.hypervisor.vmware.mo.DatacenterMO;
import com.cloud.hypervisor.vmware.mo.HostMO;
import com.cloud.hypervisor.vmware.mo.VirtualMachineMO;
import com.cloud.hypervisor.vmware.mo.VmwareHypervisorHost;
import com.cloud.hypervisor.vmware.resource.VmwareContextFactory;
import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.cloud.hypervisor.vmware.util.VmwareHelper;
import com.cloud.utils.exception.CloudRuntimeException;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualDisk;

public class VmwareCbtMigrationServiceImpl implements VmwareCbtMigrationService {

    private static final Logger LOGGER = LogManager.getLogger(VmwareCbtMigrationServiceImpl.class);

    @Override
    public List<VmwareCbtDiskInfo> listSourceDisks(String vcenter, String datacenterName, String username, String password,
                                                   String sourceHost, String sourceVmName) {
        try {
            VmwareContext context = VmwareContextFactory.getContext(vcenter, username, password);
            DatacenterMO datacenterMO = new DatacenterMO(context, datacenterName);
            if (datacenterMO.getMor() == null) {
                throw new CloudRuntimeException(String.format("Unable to find VMware datacenter %s in vCenter %s",
                        datacenterName, vcenter));
            }

            VmwareHypervisorHost hyperHost;
            VirtualMachineMO vmMO;
            if (StringUtils.isNotBlank(sourceHost)) {
                ManagedObjectReference hostMor = datacenterMO.findHost(sourceHost);
                if (hostMor == null) {
                    throw new CloudRuntimeException(String.format("Unable to find VMware host %s in vCenter %s",
                            sourceHost, vcenter));
                }
                HostMO hostMO = new HostMO(context, hostMor);
                vmMO = hostMO.findVmOnHyperHost(sourceVmName);
                hyperHost = hostMO;
            } else {
                vmMO = datacenterMO.findVm(sourceVmName);
                hyperHost = vmMO != null ? vmMO.getRunningHost() : null;
            }

            if (vmMO == null) {
                throw new CloudRuntimeException(String.format("Unable to find VMware VM %s in datacenter %s",
                        sourceVmName, datacenterName));
            }

            UnmanagedInstanceTO unmanagedInstance = VmwareHelper.getUnmanagedInstance(hyperHost, vmMO);
            return toVmwareCbtDiskInfo(unmanagedInstance, collectDiskDeviceInfo(vmMO));
        } catch (Exception e) {
            String message = String.format("Unable to discover VMware CBT source disks for VM %s in vCenter %s: %s",
                    sourceVmName, vcenter, e.getMessage());
            LOGGER.error(message, e);
            throw new CloudRuntimeException(message, e);
        }
    }

    private List<VmwareCbtDiskInfo> toVmwareCbtDiskInfo(UnmanagedInstanceTO unmanagedInstance,
                                                        Map<String, DiskDeviceInfo> diskDeviceInfo) {
        List<VmwareCbtDiskInfo> disks = new ArrayList<>();
        if (unmanagedInstance == null || CollectionUtils.isEmpty(unmanagedInstance.getDisks())) {
            return disks;
        }

        for (UnmanagedInstanceTO.Disk disk : unmanagedInstance.getDisks()) {
            String sourceDiskId = StringUtils.defaultIfBlank(disk.getDiskId(), disk.getLabel());
            String sourceDiskPath = StringUtils.defaultIfBlank(disk.getImagePath(), disk.getFileBaseName());
            DiskDeviceInfo deviceInfo = diskDeviceInfo.get(sourceDiskId);
            if (deviceInfo == null) {
                deviceInfo = diskDeviceInfo.get(sourceDiskPath);
            }
            disks.add(new VmwareCbtDiskInfo(sourceDiskId, deviceInfo != null ? deviceInfo.deviceKey : null,
                    disk.getLabel(), sourceDiskPath, disk.getDatastoreName(), disk.getCapacity(),
                    deviceInfo != null ? deviceInfo.changeId : null));
        }
        return disks;
    }

    private Map<String, DiskDeviceInfo> collectDiskDeviceInfo(VirtualMachineMO vmMO) throws Exception {
        Map<String, DiskDeviceInfo> diskDeviceInfo = new HashMap<>();
        VirtualDisk[] disks = vmMO.getAllDiskDevice();
        if (disks == null) {
            return diskDeviceInfo;
        }

        for (VirtualDevice device : disks) {
            if (!(device instanceof VirtualDisk)) {
                continue;
            }
            VirtualDisk disk = (VirtualDisk) device;
            DiskDeviceInfo deviceInfo = new DiskDeviceInfo(disk.getKey(),
                    getBackingStringValue(disk.getBacking(), "getChangeId"));
            if (StringUtils.isNotBlank(disk.getDiskObjectId())) {
                diskDeviceInfo.put(disk.getDiskObjectId(), deviceInfo);
            }
            String fileName = getBackingStringValue(disk.getBacking(), "getFileName");
            if (StringUtils.isNotBlank(fileName)) {
                diskDeviceInfo.put(fileName, deviceInfo);
            }
        }
        return diskDeviceInfo;
    }

    private String getBackingStringValue(Object backing, String methodName) {
        if (backing == null) {
            return null;
        }

        try {
            Method method = backing.getClass().getMethod(methodName);
            Object value = method.invoke(backing);
            return value != null ? value.toString() : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static class DiskDeviceInfo {
        private final Integer deviceKey;
        private final String changeId;

        private DiskDeviceInfo(Integer deviceKey, String changeId) {
            this.deviceKey = deviceKey;
            this.changeId = changeId;
        }
    }
}
