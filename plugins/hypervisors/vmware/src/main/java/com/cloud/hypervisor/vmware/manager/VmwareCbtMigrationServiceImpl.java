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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.vm.UnmanagedInstanceTO;
import org.apache.cloudstack.vm.VmwareCbtChangedBlockInfo;
import org.apache.cloudstack.vm.VmwareCbtChangedDiskInfo;
import org.apache.cloudstack.vm.VmwareCbtDiskInfo;
import org.apache.cloudstack.vm.VmwareCbtMigrationService;
import org.apache.cloudstack.vm.VmwareCbtPreflightDiskInfo;
import org.apache.cloudstack.vm.VmwareCbtPreflightInfo;
import org.apache.cloudstack.vm.VmwareCbtSnapshotInfo;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
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
import com.vmware.vim25.VirtualDeviceBackingInfo;
import com.vmware.vim25.VirtualDisk;
import com.vmware.vim25.VirtualMachineCapability;
import com.vmware.vim25.VirtualMachineConfigInfo;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualMachineConfigSummary;
import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.VirtualMachineRuntimeInfo;
import com.vmware.vim25.VirtualMachineSnapshotInfo;
import com.vmware.vim25.VirtualMachineSnapshotTree;

public class VmwareCbtMigrationServiceImpl implements VmwareCbtMigrationService {

    private static final Logger LOGGER = LogManager.getLogger(VmwareCbtMigrationServiceImpl.class);

    @Override
    public VmwareCbtPreflightInfo getPreflightInfo(String vcenter, String datacenterName, String username,
                                                   String password, String sourceHost, String sourceVmName) {
        try {
            VmwareContext context = VmwareContextFactory.getContext(vcenter, username, password);
            VmwareVmLookup lookup = lookupVirtualMachine(context, vcenter, datacenterName, sourceHost, sourceVmName);
            VirtualMachineCapability capability = (VirtualMachineCapability)context.getVimClient()
                    .getDynamicProperty(lookup.vmMO.getMor(), "capability");
            VirtualMachineConfigInfo configInfo = lookup.vmMO.getConfigInfo();
            VirtualMachineConfigSummary configSummary = lookup.vmMO.getConfigSummary();
            VirtualMachineRuntimeInfo runtimeInfo = lookup.vmMO.getRuntimeInfo();
            VirtualMachineSnapshotInfo snapshotInfo = lookup.vmMO.getSnapshotInfo();
            UnmanagedInstanceTO unmanagedInstance = VmwareHelper.getUnmanagedInstance(lookup.hyperHost, lookup.vmMO);

            return new VmwareCbtPreflightInfo(sourceVmName, getManagedObjectReferenceValue(lookup.vmMO.getMor()),
                    capability == null ? null : capability.isChangeTrackingSupported(),
                    configInfo == null ? null : configInfo.isChangeTrackingEnabled(),
                    runtimeInfo == null ? null : runtimeInfo.isConsolidationNeeded(),
                    countSnapshots(snapshotInfo),
                    toVmwareCbtPreflightDiskInfo(unmanagedInstance, collectDiskDeviceInfo(lookup.vmMO)),
                    configSummary == null ? null : configSummary.getNumCpu(),
                    configSummary == null ? null : configSummary.getCpuReservation(),
                    configSummary == null ? null : configSummary.getMemorySizeMB(),
                    unmanagedInstance == null ? null : unmanagedInstance.getOperatingSystemId(),
                    unmanagedInstance == null ? null : unmanagedInstance.getOperatingSystem());
        } catch (Exception e) {
            String message = String.format("Unable to check VMware CBT prerequisites for VM %s in vCenter %s: %s",
                    sourceVmName, vcenter, e.getMessage());
            LOGGER.error(message, e);
            throw new CloudRuntimeException(message, e);
        }
    }

    @Override
    public List<VmwareCbtDiskInfo> listSourceDisks(String vcenter, String datacenterName, String username, String password,
                                                   String sourceHost, String sourceVmName) {
        try {
            VmwareContext context = VmwareContextFactory.getContext(vcenter, username, password);
            VmwareVmLookup lookup = lookupVirtualMachine(context, vcenter, datacenterName, sourceHost, sourceVmName);
            UnmanagedInstanceTO unmanagedInstance = VmwareHelper.getUnmanagedInstance(lookup.hyperHost, lookup.vmMO);
            return toVmwareCbtDiskInfo(unmanagedInstance, collectDiskDeviceInfo(lookup.vmMO));
        } catch (Exception e) {
            String message = String.format("Unable to discover VMware CBT source disks for VM %s in vCenter %s: %s",
                    sourceVmName, vcenter, e.getMessage());
            LOGGER.error(message, e);
            throw new CloudRuntimeException(message, e);
        }
    }

    @Override
    public List<VmwareCbtDiskInfo> listSnapshotDisks(String vcenter, String datacenterName, String username, String password,
                                                     String sourceHost, String sourceVmName, String snapshotMor) {
        try {
            VmwareContext context = VmwareContextFactory.getContext(vcenter, username, password);
            VmwareVmLookup lookup = lookupVirtualMachine(context, vcenter, datacenterName, sourceHost, sourceVmName);
            UnmanagedInstanceTO unmanagedInstance = VmwareHelper.getUnmanagedInstance(lookup.hyperHost, lookup.vmMO);
            ManagedObjectReference snapshot = toManagedObjectReference("VirtualMachineSnapshot", snapshotMor);
            List<VirtualDevice> snapshotDevices = context.getVimClient().getDynamicProperty(snapshot,
                    "config.hardware.device");
            return toVmwareCbtDiskInfo(unmanagedInstance, collectDiskDeviceInfo(snapshotDevices));
        } catch (Exception e) {
            String message = String.format("Unable to discover VMware CBT snapshot disks for VM %s snapshot %s in vCenter %s: %s",
                    sourceVmName, snapshotMor, vcenter, e.getMessage());
            LOGGER.error(message, e);
            throw new CloudRuntimeException(message, e);
        }
    }

    @Override
    public void ensureChangeTrackingEnabled(String vcenter, String datacenterName, String username, String password,
                                            String sourceHost, String sourceVmName) {
        try {
            VmwareContext context = VmwareContextFactory.getContext(vcenter, username, password);
            VmwareVmLookup lookup = lookupVirtualMachine(context, vcenter, datacenterName, sourceHost, sourceVmName);
            ensureChangeTrackingEnabled(context, lookup.vmMO, sourceVmName);
        } catch (Exception e) {
            String message = String.format("Unable to enable VMware CBT for VM %s in vCenter %s: %s",
                    sourceVmName, vcenter, e.getMessage());
            LOGGER.error(message, e);
            throw new CloudRuntimeException(message, e);
        }
    }

    @Override
    public VmwareCbtSnapshotInfo createSnapshot(String vcenter, String datacenterName, String username, String password,
                                                String sourceHost, String sourceVmName, String snapshotName,
                                                String snapshotDescription, boolean quiesce) {
        try {
            VmwareContext context = VmwareContextFactory.getContext(vcenter, username, password);
            VmwareVmLookup lookup = lookupVirtualMachine(context, vcenter, datacenterName, sourceHost, sourceVmName);
            ensureChangeTrackingEnabled(context, lookup.vmMO, sourceVmName);
            ManagedObjectReference snapshot = lookup.vmMO.createSnapshotGetReference(snapshotName,
                    snapshotDescription, false, quiesce);
            if (snapshot == null) {
                throw new CloudRuntimeException(String.format("Unable to create VMware snapshot %s for VM %s",
                        snapshotName, sourceVmName));
            }
            return new VmwareCbtSnapshotInfo(snapshotName, formatManagedObjectReference(snapshot),
                    getManagedObjectReferenceValue(lookup.vmMO.getMor()));
        } catch (Exception e) {
            String message = String.format("Unable to create VMware CBT snapshot for VM %s in vCenter %s: %s",
                    sourceVmName, vcenter, e.getMessage());
            LOGGER.error(message, e);
            throw new CloudRuntimeException(message, e);
        }
    }

    @Override
    public List<VmwareCbtChangedDiskInfo> queryChangedDiskAreas(String vcenter, String datacenterName, String username,
                                                                String password, String sourceHost, String sourceVmName,
                                                                List<VmwareCbtDiskInfo> disks, String snapshotMor) {
        try {
            VmwareContext context = VmwareContextFactory.getContext(vcenter, username, password);
            VmwareVmLookup lookup = lookupVirtualMachine(context, vcenter, datacenterName, sourceHost, sourceVmName);
            ManagedObjectReference snapshot = toManagedObjectReference("VirtualMachineSnapshot", snapshotMor);
            List<VmwareCbtChangedDiskInfo> changedDisks = new ArrayList<>();
            if (CollectionUtils.isEmpty(disks)) {
                return changedDisks;
            }
            for (VmwareCbtDiskInfo disk : disks) {
                changedDisks.add(queryChangedDiskAreas(context, lookup.vmMO, snapshot, disk));
            }
            return changedDisks;
        } catch (Exception e) {
            String message = String.format("Unable to query VMware CBT changed areas for VM %s in vCenter %s: %s",
                    sourceVmName, vcenter, e.getMessage());
            LOGGER.error(message, e);
            throw new CloudRuntimeException(message, e);
        }
    }

    @Override
    public UnmanagedInstanceTO.PowerState getPowerState(String vcenter, String datacenterName, String username,
                                                        String password, String sourceHost, String sourceVmName) {
        try {
            VmwareContext context = VmwareContextFactory.getContext(vcenter, username, password);
            VmwareVmLookup lookup = lookupVirtualMachine(context, vcenter, datacenterName, sourceHost, sourceVmName);
            return toUnmanagedPowerState(lookup.vmMO.getPowerState());
        } catch (Exception e) {
            String message = String.format("Unable to check VMware power state for VM %s in vCenter %s: %s",
                    sourceVmName, vcenter, e.getMessage());
            LOGGER.error(message, e);
            throw new CloudRuntimeException(message, e);
        }
    }

    @Override
    public void removeSnapshot(String vcenter, String datacenterName, String username, String password,
                               String sourceHost, String sourceVmName, String snapshotMor) {
        if (StringUtils.isBlank(snapshotMor)) {
            return;
        }

        try {
            VmwareContext context = VmwareContextFactory.getContext(vcenter, username, password);
            lookupVirtualMachine(context, vcenter, datacenterName, sourceHost, sourceVmName);
            ManagedObjectReference snapshot = toManagedObjectReference("VirtualMachineSnapshot", snapshotMor);
            ManagedObjectReference task = context.getService().removeSnapshotTask(snapshot, false, true);
            if (!context.getVimClient().waitForTask(task)) {
                throw new CloudRuntimeException(String.format("Unable to remove VMware snapshot %s for VM %s",
                        snapshotMor, sourceVmName));
            }
            context.waitForTaskProgressDone(task);
        } catch (Exception e) {
            String message = String.format("Unable to remove VMware CBT snapshot %s for VM %s in vCenter %s: %s",
                    snapshotMor, sourceVmName, vcenter, e.getMessage());
            LOGGER.error(message, e);
            throw new CloudRuntimeException(message, e);
        }
    }

    private VmwareVmLookup lookupVirtualMachine(VmwareContext context, String vcenter, String datacenterName,
                                                String sourceHost, String sourceVmName) throws Exception {
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
        return new VmwareVmLookup(hyperHost, vmMO);
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
            if (deviceInfo == null) {
                deviceInfo = diskDeviceInfo.get(disk.getLabel());
            }
            disks.add(new VmwareCbtDiskInfo(sourceDiskId, deviceInfo != null ? deviceInfo.deviceKey : null,
                    disk.getLabel(), sourceDiskPath, disk.getDatastoreName(), disk.getCapacity(),
                    deviceInfo != null ? deviceInfo.changeId : null));
        }
        return disks;
    }

    private List<VmwareCbtPreflightDiskInfo> toVmwareCbtPreflightDiskInfo(UnmanagedInstanceTO unmanagedInstance,
                                                                          Map<String, DiskDeviceInfo> diskDeviceInfo) {
        List<VmwareCbtPreflightDiskInfo> disks = new ArrayList<>();
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
            if (deviceInfo == null) {
                deviceInfo = diskDeviceInfo.get(disk.getLabel());
            }
            disks.add(new VmwareCbtPreflightDiskInfo(sourceDiskId, deviceInfo != null ? deviceInfo.deviceKey : null,
                    disk.getLabel(), sourceDiskPath, disk.getDatastoreName(), disk.getCapacity(),
                    deviceInfo != null ? deviceInfo.changeId : null,
                    deviceInfo != null ? deviceInfo.backingType : null,
                    deviceInfo != null ? deviceInfo.diskMode : null,
                    deviceInfo != null ? deviceInfo.rdmCompatibilityMode : null,
                    deviceInfo != null && deviceInfo.independentDisk,
                    deviceInfo != null && deviceInfo.physicalRdm));
        }
        return disks;
    }

    private Map<String, DiskDeviceInfo> collectDiskDeviceInfo(VirtualMachineMO vmMO) throws Exception {
        VirtualDisk[] disks = vmMO.getAllDiskDevice();
        return collectDiskDeviceInfo(disks == null ? null : Arrays.asList(disks));
    }

    private Map<String, DiskDeviceInfo> collectDiskDeviceInfo(List<? extends VirtualDevice> devices) {
        Map<String, DiskDeviceInfo> diskDeviceInfo = new HashMap<>();
        if (devices == null) {
            return diskDeviceInfo;
        }

        for (VirtualDevice device : devices) {
            if (!(device instanceof VirtualDisk)) {
                continue;
            }
            VirtualDisk disk = (VirtualDisk) device;
            VirtualDeviceBackingInfo backing = disk.getBacking();
            String diskMode = getBackingStringValue(backing, "getDiskMode");
            String rdmCompatibilityMode = getBackingStringValue(backing, "getCompatibilityMode");
            DiskDeviceInfo deviceInfo = new DiskDeviceInfo(disk.getKey(), getBackingStringValue(backing, "getChangeId"),
                    backing == null ? null : backing.getClass().getSimpleName(), diskMode, rdmCompatibilityMode,
                    StringUtils.containsIgnoreCase(diskMode, "independent"),
                    StringUtils.containsIgnoreCase(rdmCompatibilityMode, "physical"));
            if (StringUtils.isNotBlank(disk.getDiskObjectId())) {
                diskDeviceInfo.put(disk.getDiskObjectId(), deviceInfo);
            }
            diskDeviceInfo.put(String.valueOf(disk.getKey()), deviceInfo);
            if (disk.getDeviceInfo() != null && StringUtils.isNotBlank(disk.getDeviceInfo().getLabel())) {
                diskDeviceInfo.put(disk.getDeviceInfo().getLabel(), deviceInfo);
            }
            String fileName = getBackingStringValue(backing, "getFileName");
            if (StringUtils.isNotBlank(fileName)) {
                diskDeviceInfo.put(fileName, deviceInfo);
            }
        }
        return diskDeviceInfo;
    }

    private int countSnapshots(VirtualMachineSnapshotInfo snapshotInfo) {
        if (snapshotInfo == null || CollectionUtils.isEmpty(snapshotInfo.getRootSnapshotList())) {
            return 0;
        }
        return countSnapshotTrees(snapshotInfo.getRootSnapshotList());
    }

    private int countSnapshotTrees(List<VirtualMachineSnapshotTree> snapshotTrees) {
        int count = 0;
        for (VirtualMachineSnapshotTree snapshotTree : snapshotTrees) {
            count++;
            if (CollectionUtils.isNotEmpty(snapshotTree.getChildSnapshotList())) {
                count += countSnapshotTrees(snapshotTree.getChildSnapshotList());
            }
        }
        return count;
    }

    private UnmanagedInstanceTO.PowerState toUnmanagedPowerState(VirtualMachinePowerState powerState) {
        if (powerState == VirtualMachinePowerState.POWERED_OFF) {
            return UnmanagedInstanceTO.PowerState.PowerOff;
        }
        if (powerState == VirtualMachinePowerState.POWERED_ON) {
            return UnmanagedInstanceTO.PowerState.PowerOn;
        }
        return UnmanagedInstanceTO.PowerState.PowerUnknown;
    }

    private VmwareCbtChangedDiskInfo queryChangedDiskAreas(VmwareContext context, VirtualMachineMO vmMO,
                                                           ManagedObjectReference snapshot, VmwareCbtDiskInfo disk)
            throws ReflectiveOperationException {
        if (disk.getSourceDiskDeviceKey() == null) {
            throw new CloudRuntimeException(String.format("VMware disk device key is missing for source disk %s",
                    disk.getSourceDiskId()));
        }
        if (StringUtils.isBlank(disk.getChangeId())) {
            throw new CloudRuntimeException(String.format("VMware CBT change ID is missing for source disk %s",
                    disk.getSourceDiskId()));
        }

        List<VmwareCbtChangedBlockInfo> changedBlocks = new ArrayList<>();
        long startOffset = 0L;
        long capacityBytes = disk.getCapacityBytes() == null ? 0L : disk.getCapacityBytes();
        String nextChangeId = null;

        do {
            Object diskChangeInfo = invokeQueryChangedDiskAreas(context, vmMO, snapshot, disk, startOffset);
            nextChangeId = StringUtils.defaultIfBlank(getObjectStringValue(diskChangeInfo, "getChangeId"),
                    nextChangeId);
            for (Object changedArea : getObjectListValue(diskChangeInfo, "getChangedArea")) {
                Long areaStart = getObjectLongValue(changedArea, "getStart");
                Long areaLength = getObjectLongValue(changedArea, "getLength");
                if (areaStart != null && areaLength != null && areaLength > 0L) {
                    changedBlocks.add(new VmwareCbtChangedBlockInfo(areaStart, areaLength));
                }
            }

            Long responseStart = getObjectLongValue(diskChangeInfo, "getStartOffset");
            Long responseLength = getObjectLongValue(diskChangeInfo, "getLength");
            if (responseStart == null || responseLength == null || responseLength <= 0L) {
                break;
            }
            startOffset = responseStart + responseLength;
        } while (capacityBytes > 0L && startOffset < capacityBytes);

        return new VmwareCbtChangedDiskInfo(disk.getSourceDiskId(), nextChangeId, changedBlocks);
    }

    private Object invokeQueryChangedDiskAreas(VmwareContext context, VirtualMachineMO vmMO,
                                               ManagedObjectReference snapshot, VmwareCbtDiskInfo disk,
                                               long startOffset) throws ReflectiveOperationException {
        Method method = context.getService().getClass().getMethod("queryChangedDiskAreas",
                ManagedObjectReference.class, ManagedObjectReference.class, int.class, long.class, String.class);
        return method.invoke(context.getService(), vmMO.getMor(), snapshot, disk.getSourceDiskDeviceKey(),
                startOffset, disk.getChangeId());
    }

    private void ensureChangeTrackingEnabled(VmwareContext context, VirtualMachineMO vmMO, String sourceVmName)
            throws Exception {
        VirtualMachineCapability capability = (VirtualMachineCapability)context.getVimClient()
                .getDynamicProperty(vmMO.getMor(), "capability");
        if (capability != null && BooleanUtils.isFalse(capability.isChangeTrackingSupported())) {
            throw new CloudRuntimeException(String.format("VMware CBT is not supported for VM %s", sourceVmName));
        }

        VirtualMachineConfigInfo configInfo = vmMO.getConfigInfo();
        if (configInfo != null && BooleanUtils.isTrue(configInfo.isChangeTrackingEnabled())) {
            return;
        }

        VirtualMachineConfigSpec configSpec = new VirtualMachineConfigSpec();
        configSpec.setChangeTrackingEnabled(true);
        if (!vmMO.configureVm(configSpec)) {
            throw new CloudRuntimeException(String.format("Unable to enable VMware CBT for VM %s", sourceVmName));
        }
    }

    private ManagedObjectReference toManagedObjectReference(String defaultType, String mor) {
        if (StringUtils.isBlank(mor)) {
            return null;
        }

        ManagedObjectReference reference = new ManagedObjectReference();
        if (mor.contains(":")) {
            String[] parts = mor.split(":", 2);
            reference.setType(parts[0]);
            reference.setValue(parts[1]);
        } else {
            reference.setType(defaultType);
            reference.setValue(mor);
        }
        return reference;
    }

    private String formatManagedObjectReference(ManagedObjectReference mor) {
        if (mor == null) {
            return null;
        }
        return String.format("%s:%s", mor.getType(), mor.getValue());
    }

    private String getManagedObjectReferenceValue(ManagedObjectReference mor) {
        return mor == null ? null : mor.getValue();
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

    private String getObjectStringValue(Object object, String methodName) {
        Object value = invokeGetter(object, methodName);
        return value != null ? value.toString() : null;
    }

    private Long getObjectLongValue(Object object, String methodName) {
        Object value = invokeGetter(object, methodName);
        if (value instanceof Number) {
            return ((Number)value).longValue();
        }
        return null;
    }

    private List<?> getObjectListValue(Object object, String methodName) {
        Object value = invokeGetter(object, methodName);
        if (value instanceof List) {
            return (List<?>)value;
        }
        return new ArrayList<>();
    }

    private Object invokeGetter(Object object, String methodName) {
        if (object == null) {
            return null;
        }

        try {
            Method method = object.getClass().getMethod(methodName);
            return method.invoke(object);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static class DiskDeviceInfo {
        private final Integer deviceKey;
        private final String changeId;
        private final String backingType;
        private final String diskMode;
        private final String rdmCompatibilityMode;
        private final boolean independentDisk;
        private final boolean physicalRdm;

        private DiskDeviceInfo(Integer deviceKey, String changeId, String backingType, String diskMode,
                               String rdmCompatibilityMode, boolean independentDisk, boolean physicalRdm) {
            this.deviceKey = deviceKey;
            this.changeId = changeId;
            this.backingType = backingType;
            this.diskMode = diskMode;
            this.rdmCompatibilityMode = rdmCompatibilityMode;
            this.independentDisk = independentDisk;
            this.physicalRdm = physicalRdm;
        }
    }

    private static class VmwareVmLookup {
        private final VmwareHypervisorHost hyperHost;
        private final VirtualMachineMO vmMO;

        private VmwareVmLookup(VmwareHypervisorHost hyperHost, VirtualMachineMO vmMO) {
            this.hyperHost = hyperHost;
            this.vmMO = vmMO;
        }
    }
}
