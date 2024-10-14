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

package com.cloud.hypervisor.vmware.util;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import com.cloud.agent.api.to.DiskTO;
import com.cloud.hypervisor.vmware.mo.ClusterMO;
import com.cloud.hypervisor.vmware.mo.DatastoreFile;
import com.cloud.hypervisor.vmware.mo.DistributedVirtualSwitchMO;
import com.cloud.hypervisor.vmware.mo.HypervisorHostHelper;
import com.cloud.serializer.GsonHelper;
import com.cloud.storage.Volume;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.vmware.vim25.DatastoreInfo;
import com.vmware.vim25.DistributedVirtualPort;
import com.vmware.vim25.DistributedVirtualSwitchPortCriteria;
import com.vmware.vim25.GuestInfo;
import com.vmware.vim25.GuestNicInfo;
import com.vmware.vim25.HostPortGroupSpec;
import com.vmware.vim25.NasDatastoreInfo;
import com.vmware.vim25.VMwareDVSPortSetting;
import com.vmware.vim25.VirtualDeviceFileBackingInfo;
import com.vmware.vim25.VirtualIDEController;
import com.vmware.vim25.VirtualMachineConfigSummary;
import com.vmware.vim25.VirtualMachineGuestOsIdentifier;
import com.vmware.vim25.VirtualMachineToolsStatus;
import com.vmware.vim25.VirtualSCSIController;
import com.vmware.vim25.VmwareDistributedVirtualSwitchPvlanSpec;
import com.vmware.vim25.VmwareDistributedVirtualSwitchVlanIdSpec;
import org.apache.cloudstack.utils.volume.VirtualMachineDiskInfo;
import org.apache.cloudstack.vm.UnmanagedInstanceTO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.cloud.hypervisor.vmware.mo.CustomFieldConstants;
import com.cloud.hypervisor.vmware.mo.DatastoreMO;
import com.cloud.hypervisor.vmware.mo.DiskControllerType;
import com.cloud.hypervisor.vmware.mo.HostMO;
import com.cloud.hypervisor.vmware.mo.LicenseAssignmentManagerMO;
import com.cloud.hypervisor.vmware.mo.VirtualEthernetCardType;
import com.cloud.hypervisor.vmware.mo.VirtualMachineMO;
import com.cloud.hypervisor.vmware.mo.VmwareHypervisorHost;
import com.cloud.utils.LogUtils;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.exception.ExceptionUtil;
import com.vmware.vim25.DistributedVirtualSwitchPortConnection;
import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.GuestOsDescriptor;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.MethodFault;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.OptionValue;
import com.vmware.vim25.PerfCounterInfo;
import com.vmware.vim25.PerfMetricId;
import com.vmware.vim25.ResourceAllocationInfo;
import com.vmware.vim25.StorageIOAllocationInfo;
import com.vmware.vim25.VirtualCdrom;
import com.vmware.vim25.VirtualCdromIsoBackingInfo;
import com.vmware.vim25.VirtualCdromRemotePassthroughBackingInfo;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualDeviceBackingInfo;
import com.vmware.vim25.VirtualDeviceConnectInfo;
import com.vmware.vim25.VirtualDisk;
import com.vmware.vim25.VirtualDiskFlatVer1BackingInfo;
import com.vmware.vim25.VirtualDiskFlatVer2BackingInfo;
import com.vmware.vim25.VirtualDiskMode;
import com.vmware.vim25.VirtualDiskRawDiskMappingVer1BackingInfo;
import com.vmware.vim25.VirtualDiskSparseVer1BackingInfo;
import com.vmware.vim25.VirtualDiskSparseVer2BackingInfo;
import com.vmware.vim25.VirtualE1000;
import com.vmware.vim25.VirtualEthernetCard;
import com.vmware.vim25.VirtualEthernetCardDistributedVirtualPortBackingInfo;
import com.vmware.vim25.VirtualEthernetCardNetworkBackingInfo;
import com.vmware.vim25.VirtualEthernetCardOpaqueNetworkBackingInfo;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualMachineSnapshotTree;
import com.vmware.vim25.VirtualPCNet32;
import com.vmware.vim25.VirtualUSBController;
import com.vmware.vim25.VirtualVmxnet2;
import com.vmware.vim25.VirtualVmxnet3;

public class VmwareHelper {
    protected static Logger LOGGER = LogManager.getLogger(VmwareHelper.class);

    public static final int MAX_SCSI_CONTROLLER_COUNT = 4;
    public static final int MAX_IDE_CONTROLLER_COUNT = 2;
    public static final int MAX_ALLOWED_DEVICES_IDE_CONTROLLER = 2;
    public static final int MAX_ALLOWED_DEVICES_SCSI_CONTROLLER = 16;
    public static final int MAX_SUPPORTED_DEVICES_SCSI_CONTROLLER = MAX_ALLOWED_DEVICES_SCSI_CONTROLLER - 1; // One device node is unavailable for hard disks or SCSI devices
    public static final int MAX_USABLE_SCSI_CONTROLLERS = 2;
    public static final String MIN_VERSION_UEFI_LEGACY = "5.5";
    public static final String MIN_VERSION_VMFS6 = "6.5";

    public static boolean isReservedScsiDeviceNumber(int deviceNumber) {
        // The SCSI controller is assigned to virtual device node (z:7), so that device node is unavailable for hard disks or SCSI devices.
        return (deviceNumber % VmwareHelper.MAX_ALLOWED_DEVICES_SCSI_CONTROLLER) == 7;
    }

    @Nonnull
    private static VirtualDeviceConnectInfo getVirtualDeviceConnectInfo(boolean connected, boolean connectOnStart) {
        VirtualDeviceConnectInfo connectInfo = new VirtualDeviceConnectInfo();
        connectInfo.setAllowGuestControl(true);
        connectInfo.setConnected(connected);
        connectInfo.setStartConnected(connectOnStart);
        return connectInfo;
    }

    @Nonnull
    private static VirtualEthernetCard createVirtualEthernetCard(VirtualEthernetCardType deviceType) {
        VirtualEthernetCard nic;
        switch (deviceType) {
            case E1000:
                nic = new VirtualE1000();
                break;

            case PCNet32:
                nic = new VirtualPCNet32();
                break;

            case Vmxnet2:
                nic = new VirtualVmxnet2();
                break;

            case Vmxnet3:
                nic = new VirtualVmxnet3();
                break;

            default:
                assert (false);
                nic = new VirtualE1000();
        }
        return nic;
    }

    public static VirtualDevice prepareNicOpaque(VirtualMachineMO vmMo, VirtualEthernetCardType deviceType, String portGroupName,
            String macAddress, int contextNumber, boolean connected, boolean connectOnStart) throws Exception {
        assert(vmMo.getRunningHost().hasOpaqueNSXNetwork());

        VirtualEthernetCard nic = createVirtualEthernetCard(deviceType);

        VirtualEthernetCardOpaqueNetworkBackingInfo nicBacking = new VirtualEthernetCardOpaqueNetworkBackingInfo();
        nicBacking.setOpaqueNetworkId("br-int");
        nicBacking.setOpaqueNetworkType("nsx.network");

        nic.setBacking(nicBacking);

        nic.setAddressType("Manual");
        nic.setConnectable(getVirtualDeviceConnectInfo(connected, connectOnStart));
        nic.setMacAddress(macAddress);
        nic.setKey(-contextNumber);
        return nic;
    }

    public static void updateNicDevice(VirtualDevice nic, ManagedObjectReference morNetwork, String portGroupName) throws Exception {
        VirtualEthernetCardNetworkBackingInfo nicBacking = new VirtualEthernetCardNetworkBackingInfo();
        nicBacking.setDeviceName(portGroupName);
        nicBacking.setNetwork(morNetwork);
        nic.setBacking(nicBacking);
    }

    public static void updateDvNicDevice(VirtualDevice nic, ManagedObjectReference morNetwork, String dvSwitchUuid) throws Exception {
        final VirtualEthernetCardDistributedVirtualPortBackingInfo dvPortBacking = new VirtualEthernetCardDistributedVirtualPortBackingInfo();
        final DistributedVirtualSwitchPortConnection dvPortConnection = new DistributedVirtualSwitchPortConnection();

        dvPortConnection.setSwitchUuid(dvSwitchUuid);
        dvPortConnection.setPortgroupKey(morNetwork.getValue());
        dvPortBacking.setPort(dvPortConnection);
        nic.setBacking(dvPortBacking);
    }

    public static VirtualDevice prepareNicDevice(VirtualMachineMO vmMo, ManagedObjectReference morNetwork, VirtualEthernetCardType deviceType, String portGroupName,
            String macAddress, int contextNumber, boolean connected, boolean connectOnStart) throws Exception {

        VirtualEthernetCard nic = createVirtualEthernetCard(deviceType);

        VirtualEthernetCardNetworkBackingInfo nicBacking = new VirtualEthernetCardNetworkBackingInfo();
        nicBacking.setDeviceName(portGroupName);
        nicBacking.setNetwork(morNetwork);
        nic.setBacking(nicBacking);

        nic.setAddressType("Manual");
        nic.setConnectable(getVirtualDeviceConnectInfo(connected, connectOnStart));
        nic.setMacAddress(macAddress);
        nic.setKey(-contextNumber);
        return nic;
    }

    public static VirtualDevice prepareDvNicDevice(VirtualMachineMO vmMo, ManagedObjectReference morNetwork, VirtualEthernetCardType deviceType, String dvPortGroupName,
            String dvSwitchUuid, String macAddress, int contextNumber, boolean connected, boolean connectOnStart) throws Exception {

        VirtualEthernetCard nic = createVirtualEthernetCard(deviceType);

        final VirtualEthernetCardDistributedVirtualPortBackingInfo dvPortBacking = new VirtualEthernetCardDistributedVirtualPortBackingInfo();
        final DistributedVirtualSwitchPortConnection dvPortConnection = new DistributedVirtualSwitchPortConnection();

        dvPortConnection.setSwitchUuid(dvSwitchUuid);
        dvPortConnection.setPortgroupKey(morNetwork.getValue());
        dvPortBacking.setPort(dvPortConnection);
        nic.setBacking(dvPortBacking);

        nic.setAddressType("Manual");
        nic.setConnectable(getVirtualDeviceConnectInfo(connected, connectOnStart));
        nic.setMacAddress(macAddress);
        nic.setKey(-contextNumber);
        return nic;
    }

    // vmdkDatastorePath: [datastore name] vmdkFilePath
    public static VirtualDevice prepareDiskDevice(VirtualMachineMO vmMo, VirtualDisk device, int controllerKey, String vmdkDatastorePathChain[],
                                                  ManagedObjectReference morDs, int deviceNumber, int contextNumber, Long maxIops) throws Exception {
        LOGGER.debug(LogUtils.logGsonWithoutException("Trying to prepare disk device to virtual machine [%s], using the following details: Virtual device [%s], "
                + "ManagedObjectReference [%s], ControllerKey [%s], VMDK path chain [%s], DeviceNumber [%s], ContextNumber [%s] and max IOPS [%s].",
                vmMo, device, morDs, controllerKey, vmdkDatastorePathChain, deviceNumber, contextNumber, maxIops));
        assert (vmdkDatastorePathChain != null);
        assert (vmdkDatastorePathChain.length >= 1);

        VirtualDisk disk;
        VirtualDiskFlatVer2BackingInfo backingInfo;
        if (device != null) {
            disk = device;
            backingInfo = (VirtualDiskFlatVer2BackingInfo)disk.getBacking();
        } else {
            disk = new VirtualDisk();
            backingInfo = new VirtualDiskFlatVer2BackingInfo();
            backingInfo.setDatastore(morDs);
            backingInfo.setDiskMode(VirtualDiskMode.PERSISTENT.value());
            disk.setBacking(backingInfo);

            int ideControllerKey = vmMo.getIDEDeviceControllerKey();
            if (controllerKey < 0)
                controllerKey = ideControllerKey;
            if (deviceNumber < 0) {
                deviceNumber = vmMo.getNextDeviceNumber(controllerKey);
            }

            disk.setControllerKey(controllerKey);
            disk.setKey(-contextNumber);
            disk.setUnitNumber(deviceNumber);

            if (maxIops != null && maxIops > 0) {
                LOGGER.debug(LogUtils.logGsonWithoutException("Defining [%s] as the max IOPS of disk [%s].", maxIops, disk));
                StorageIOAllocationInfo storageIOAllocationInfo = new StorageIOAllocationInfo();
                storageIOAllocationInfo.setLimit(maxIops);
                disk.setStorageIOAllocation(storageIOAllocationInfo);
            }

            VirtualDeviceConnectInfo connectInfo = new VirtualDeviceConnectInfo();
            connectInfo.setConnected(true);
            connectInfo.setStartConnected(true);
            disk.setConnectable(connectInfo);
        }

        backingInfo.setFileName(vmdkDatastorePathChain[0]);
        if (vmdkDatastorePathChain.length > 1) {
            String[] parentDisks = Arrays.copyOfRange(vmdkDatastorePathChain, 1, vmdkDatastorePathChain.length);
            setParentBackingInfo(backingInfo, morDs, parentDisks);
        }

        LOGGER.debug(LogUtils.logGsonWithoutException("Prepared disk device, to attach to virtual machine [%s], has the following details: Virtual device [%s], "
                + "ManagedObjectReference [%s], ControllerKey [%s], VMDK path chain [%s], DeviceNumber [%s], ContextNumber [%s] and max IOPS [%s], is: [%s].",
                vmMo, device, morDs, controllerKey, vmdkDatastorePathChain, deviceNumber, contextNumber, maxIops, disk));
        return disk;
    }

    // vmdkDatastorePath: [datastore name] vmdkFilePath, create delta disk based on disk from template
    public static VirtualDevice prepareDiskDevice(VirtualMachineMO vmMo, int controllerKey, String vmdkDatastorePath, int sizeInMb, ManagedObjectReference morDs,
            VirtualDisk templateDisk, int deviceNumber, int contextNumber) throws Exception {

        assert (templateDisk != null);
        VirtualDeviceBackingInfo parentBacking = templateDisk.getBacking();
        assert (parentBacking != null);

        // TODO Not sure if we need to check if the disk in template and the new disk needs to share the
        // same datastore
        VirtualDisk disk = new VirtualDisk();
        if (parentBacking instanceof VirtualDiskFlatVer1BackingInfo) {
            VirtualDiskFlatVer1BackingInfo backingInfo = new VirtualDiskFlatVer1BackingInfo();
            backingInfo.setDiskMode(((VirtualDiskFlatVer1BackingInfo)parentBacking).getDiskMode());
            backingInfo.setDatastore(morDs);
            backingInfo.setFileName(vmdkDatastorePath);
            backingInfo.setParent((VirtualDiskFlatVer1BackingInfo)parentBacking);
            disk.setBacking(backingInfo);
        } else if (parentBacking instanceof VirtualDiskFlatVer2BackingInfo) {
            VirtualDiskFlatVer2BackingInfo backingInfo = new VirtualDiskFlatVer2BackingInfo();
            backingInfo.setDiskMode(((VirtualDiskFlatVer2BackingInfo)parentBacking).getDiskMode());
            backingInfo.setDatastore(morDs);
            backingInfo.setFileName(vmdkDatastorePath);
            backingInfo.setParent((VirtualDiskFlatVer2BackingInfo)parentBacking);
            disk.setBacking(backingInfo);
        } else if (parentBacking instanceof VirtualDiskRawDiskMappingVer1BackingInfo) {
            VirtualDiskRawDiskMappingVer1BackingInfo backingInfo = new VirtualDiskRawDiskMappingVer1BackingInfo();
            backingInfo.setDiskMode(((VirtualDiskRawDiskMappingVer1BackingInfo)parentBacking).getDiskMode());
            backingInfo.setDatastore(morDs);
            backingInfo.setFileName(vmdkDatastorePath);
            backingInfo.setParent((VirtualDiskRawDiskMappingVer1BackingInfo)parentBacking);
            disk.setBacking(backingInfo);
        } else if (parentBacking instanceof VirtualDiskSparseVer1BackingInfo) {
            VirtualDiskSparseVer1BackingInfo backingInfo = new VirtualDiskSparseVer1BackingInfo();
            backingInfo.setDiskMode(((VirtualDiskSparseVer1BackingInfo)parentBacking).getDiskMode());
            backingInfo.setDatastore(morDs);
            backingInfo.setFileName(vmdkDatastorePath);
            backingInfo.setParent((VirtualDiskSparseVer1BackingInfo)parentBacking);
            disk.setBacking(backingInfo);
        } else if (parentBacking instanceof VirtualDiskSparseVer2BackingInfo) {
            VirtualDiskSparseVer2BackingInfo backingInfo = new VirtualDiskSparseVer2BackingInfo();
            backingInfo.setDiskMode(((VirtualDiskSparseVer2BackingInfo)parentBacking).getDiskMode());
            backingInfo.setDatastore(morDs);
            backingInfo.setFileName(vmdkDatastorePath);
            backingInfo.setParent((VirtualDiskSparseVer2BackingInfo)parentBacking);
            disk.setBacking(backingInfo);
        } else {
            throw new Exception("Unsupported disk backing: " + parentBacking.getClass().getCanonicalName());
        }

        int ideControllerKey = vmMo.getIDEDeviceControllerKey();
        if (controllerKey < 0)
            controllerKey = ideControllerKey;
        disk.setControllerKey(controllerKey);
        if (deviceNumber < 0) {
            deviceNumber = vmMo.getNextDeviceNumber(controllerKey);
        }

        disk.setKey(-contextNumber);
        disk.setUnitNumber(deviceNumber);
        disk.setCapacityInKB(sizeInMb * 1024);

        VirtualDeviceConnectInfo connectInfo = new VirtualDeviceConnectInfo();
        connectInfo.setConnected(true);
        connectInfo.setStartConnected(true);
        disk.setConnectable(connectInfo);
        return disk;
    }

    private static void setParentBackingInfo(VirtualDiskFlatVer2BackingInfo backingInfo, ManagedObjectReference morDs, String[] parentDatastorePathList) {

        VirtualDiskFlatVer2BackingInfo parentBacking = new VirtualDiskFlatVer2BackingInfo();
        parentBacking.setDatastore(morDs);
        parentBacking.setDiskMode(VirtualDiskMode.PERSISTENT.value());

        if (parentDatastorePathList.length > 1) {
            String[] nextDatastorePathList = new String[parentDatastorePathList.length - 1];
            for (int i = 0; i < parentDatastorePathList.length - 1; i++)
                nextDatastorePathList[i] = parentDatastorePathList[i + 1];
            setParentBackingInfo(parentBacking, morDs, nextDatastorePathList);
        }
        parentBacking.setFileName(parentDatastorePathList[0]);

        backingInfo.setParent(parentBacking);
    }

    @SuppressWarnings("unchecked")
    private static void setParentBackingInfo(VirtualDiskFlatVer2BackingInfo backingInfo, Pair<String, ManagedObjectReference>[] parentDatastorePathList) {

        VirtualDiskFlatVer2BackingInfo parentBacking = new VirtualDiskFlatVer2BackingInfo();
        parentBacking.setDatastore(parentDatastorePathList[0].second());
        parentBacking.setDiskMode(VirtualDiskMode.PERSISTENT.value());

        if (parentDatastorePathList.length > 1) {
            Pair<String, ManagedObjectReference>[] nextDatastorePathList = new Pair[parentDatastorePathList.length - 1];
            for (int i = 0; i < parentDatastorePathList.length - 1; i++)
                nextDatastorePathList[i] = parentDatastorePathList[i + 1];
            setParentBackingInfo(parentBacking, nextDatastorePathList);
        }
        parentBacking.setFileName(parentDatastorePathList[0].first());

        backingInfo.setParent(parentBacking);
    }

    public static Pair<VirtualDevice, Boolean> prepareIsoDevice(VirtualMachineMO vmMo, String isoDatastorePath, ManagedObjectReference morDs, boolean connect,
            boolean connectAtBoot, int deviceNumber, int contextNumber) throws Exception {

        boolean newCdRom = false;
        VirtualCdrom cdRom = (VirtualCdrom)vmMo.getIsoDevice();
        if (cdRom == null) {
            newCdRom = true;
            cdRom = new VirtualCdrom();

            assert (vmMo.getIDEDeviceControllerKey() >= 0);
            cdRom.setControllerKey(vmMo.getIDEDeviceControllerKey());
            if (deviceNumber < 0)
                deviceNumber = vmMo.getNextIDEDeviceNumber();

            cdRom.setUnitNumber(deviceNumber);
            cdRom.setKey(-contextNumber);
        }

        VirtualDeviceConnectInfo cInfo = new VirtualDeviceConnectInfo();
        cInfo.setConnected(connect);
        cInfo.setStartConnected(connectAtBoot);
        cdRom.setConnectable(cInfo);

        if (isoDatastorePath != null) {
            VirtualCdromIsoBackingInfo backingInfo = new VirtualCdromIsoBackingInfo();
            backingInfo.setFileName(isoDatastorePath);
            backingInfo.setDatastore(morDs);
            cdRom.setBacking(backingInfo);
        } else {
            VirtualCdromRemotePassthroughBackingInfo backingInfo = new VirtualCdromRemotePassthroughBackingInfo();
            backingInfo.setDeviceName("");
            cdRom.setBacking(backingInfo);
        }

        return new Pair<VirtualDevice, Boolean>(cdRom, newCdRom);
    }

    public static VirtualDisk getRootDisk(VirtualDisk[] disks) {
        if (disks.length == 1)
            return disks[0];

        // TODO : for now, always return the first disk as root disk
        return disks[0];
    }

    public static ManagedObjectReference findSnapshotInTree(List<VirtualMachineSnapshotTree> snapTree, String findName) {
        assert (findName != null);

        ManagedObjectReference snapMor = null;
        if (snapTree == null)
            return snapMor;

        for (int i = 0; i < snapTree.size() && snapMor == null; i++) {
            VirtualMachineSnapshotTree node = snapTree.get(i);

            if (node.getName().equals(findName)) {
                snapMor = node.getSnapshot();
            } else {
                List<VirtualMachineSnapshotTree> childTree = node.getChildSnapshotList();
                snapMor = findSnapshotInTree(childTree, findName);
            }
        }
        return snapMor;
    }

    public static byte[] composeDiskInfo(List<Ternary<String, String, String>> diskInfo, int disksInChain, boolean includeBase) throws IOException {

        BufferedWriter out = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try {
            out = new BufferedWriter(new OutputStreamWriter(bos,"UTF-8"));

            out.write("disksInChain=" + disksInChain);
            out.newLine();

            out.write("disksInBackup=" + diskInfo.size());
            out.newLine();

            out.write("baseDiskIncluded=" + includeBase);
            out.newLine();

            int seq = disksInChain - 1;
            for (Ternary<String, String, String> item : diskInfo) {
                out.write(String.format("disk%d.fileName=%s", seq, item.first()));
                out.newLine();

                out.write(String.format("disk%d.baseFileName=%s", seq, item.second()));
                out.newLine();

                if (item.third() != null) {
                    out.write(String.format("disk%d.parentFileName=%s", seq, item.third()));
                    out.newLine();
                }
                seq--;
            }

            out.newLine();
        } finally {
            if (out != null)
                out.close();
        }

        return bos.toByteArray();
    }

    public static OptionValue[] composeVncOptions(OptionValue[] optionsToMerge, boolean enableVnc, String vncPassword, int vncPort, String keyboardLayout) {

        int numOptions = 3;
        boolean needKeyboardSetup = false;
        if (keyboardLayout != null && !keyboardLayout.isEmpty()) {
            numOptions++;
            needKeyboardSetup = true;
        }

        if (optionsToMerge != null)
            numOptions += optionsToMerge.length;

        OptionValue[] options = new OptionValue[numOptions];
        int i = 0;
        if (optionsToMerge != null) {
            for (int j = 0; j < optionsToMerge.length; j++)
                options[i++] = optionsToMerge[j];
        }

        options[i] = new OptionValue();
        options[i].setKey("RemoteDisplay.vnc.enabled");
        options[i++].setValue(enableVnc ? "true" : "false");

        options[i] = new OptionValue();
        options[i].setKey("RemoteDisplay.vnc.password");
        options[i++].setValue(vncPassword);

        options[i] = new OptionValue();
        options[i].setKey("RemoteDisplay.vnc.port");
        options[i++].setValue("" + vncPort);

        if (needKeyboardSetup) {
            options[i] = new OptionValue();
            options[i].setKey("RemoteDisplay.vnc.keymap");
            options[i++].setValue(keyboardLayout);
        }

        return options;
    }

    public static void setVmScaleUpConfig(VirtualMachineConfigSpec vmConfig, int cpuCount, int cpuSpeedMHz, int cpuReservedMhz, int memoryMB, int memoryReserveMB,
            boolean limitCpuUse) {

        // VM config for scaling up
        vmConfig.setMemoryMB((long)memoryMB);
        vmConfig.setNumCPUs(cpuCount);

        ResourceAllocationInfo cpuInfo = new ResourceAllocationInfo();
        if (limitCpuUse) {
            cpuInfo.setLimit((long)(cpuSpeedMHz * cpuCount));
        } else {
            cpuInfo.setLimit(-1L);
        }

        cpuInfo.setReservation((long)cpuReservedMhz);
        vmConfig.setCpuAllocation(cpuInfo);

        ResourceAllocationInfo memInfo = new ResourceAllocationInfo();
        memInfo.setLimit((long)memoryMB);
        memInfo.setReservation((long)memoryReserveMB);
        vmConfig.setMemoryAllocation(memInfo);

    }

    public static void setBasicVmConfig(VirtualMachineConfigSpec vmConfig, int cpuCount, int cpuSpeedMHz, int cpuReservedMhz, int memoryMB, int memoryReserveMB,
                                        String guestOsIdentifier, boolean limitCpuUse, boolean deployAsIs) {

        // VM config basics
        vmConfig.setMemoryMB((long)memoryMB);
        vmConfig.setNumCPUs(cpuCount);

        ResourceAllocationInfo cpuInfo = new ResourceAllocationInfo();
        if (limitCpuUse) {
            cpuInfo.setLimit(((long)cpuSpeedMHz * cpuCount));
        } else {
            cpuInfo.setLimit(-1L);
        }

        cpuInfo.setReservation((long)cpuReservedMhz);
        vmConfig.setCpuAllocation(cpuInfo);
        ResourceAllocationInfo memInfo = new ResourceAllocationInfo();
        memInfo.setLimit((long)memoryMB);
        memInfo.setReservation((long)memoryReserveMB);
        vmConfig.setMemoryAllocation(memInfo);

        if (!deployAsIs) {
            // Deploy as-is uses the cloned VM guest OS
            vmConfig.setGuestId(guestOsIdentifier);
        }

    }

    public static VirtualDevice prepareUSBControllerDevice() {
        LOGGER.debug("Preparing USB controller(EHCI+UHCI) device");
        VirtualUSBController usbController = new VirtualUSBController(); //EHCI+UHCI
        usbController.setEhciEnabled(true);
        usbController.setAutoConnectDevices(true);

        return usbController;
    }

    public static PerfMetricId createPerfMetricId(PerfCounterInfo counterInfo, String instance) {
        PerfMetricId metricId = new PerfMetricId();
        metricId.setCounterId(counterInfo.getKey());
        metricId.setInstance(instance);
        return metricId;
    }

    public static String getDiskDeviceFileName(VirtualDisk diskDevice) {
        VirtualDeviceBackingInfo backingInfo = diskDevice.getBacking();
        if (backingInfo instanceof VirtualDiskFlatVer2BackingInfo) {
            final String vmdkName = ((VirtualDiskFlatVer2BackingInfo)backingInfo).getFileName().replace(".vmdk", "");
            if (vmdkName.contains("/")) {
                return vmdkName.split("/", 2)[1];
            }
            return vmdkName;
        }
        return null;
    }

    public static ManagedObjectReference getDiskDeviceDatastore(VirtualDisk diskDevice) throws Exception {
        VirtualDeviceBackingInfo backingInfo = diskDevice.getBacking();
        assert (backingInfo instanceof VirtualDiskFlatVer2BackingInfo);
        return ((VirtualDiskFlatVer2BackingInfo)backingInfo).getDatastore();
    }

    public static Object getPropValue(ObjectContent oc, String name) {
        List<DynamicProperty> props = oc.getPropSet();

        for (DynamicProperty prop : props) {
            if (prop.getName().equalsIgnoreCase(name))
                return prop.getVal();
        }

        return null;
    }

    public static String getFileExtension(String fileName, String defaultExtension) {
        int pos = fileName.lastIndexOf('.');
        if (pos < 0)
            return defaultExtension;

        return fileName.substring(pos);
    }

    public static boolean isSameHost(String ipAddress, String destName) {
        // TODO : may need to do DNS lookup to compare IP address exactly
        return ipAddress.equals(destName);
    }

    public static String getExceptionMessage(Throwable e) {
        return getExceptionMessage(e, false);
    }

    public static String getExceptionMessage(Throwable e, boolean printStack) {
        //TODO: in vim 5.1, exceptions do not have a base exception class, MethodFault becomes a FaultInfo that we can only get
        // from individual exception through getFaultInfo, so we have to use reflection here to get MethodFault information.
        try {
            Class<? extends Throwable> cls = e.getClass();
            Method mth = cls.getDeclaredMethod("getFaultInfo", (Class<?>)null);
            if (mth != null) {
                Object fault = mth.invoke(e, (Object[])null);
                if (fault instanceof MethodFault) {
                    final StringWriter writer = new StringWriter();
                    writer.append("Exception: " + fault.getClass().getName() + "\n");
                    writer.append("message: " + ((MethodFault)fault).getFaultMessage() + "\n");

                    if (printStack) {
                        writer.append("stack: ");
                        e.printStackTrace(new PrintWriter(writer));
                    }
                    return writer.toString();
                }
            }
        } catch (Exception ex) {
            LOGGER.info("[ignored]"
                    + "failed to get message for exception: " + e.getLocalizedMessage());
        }

        return ExceptionUtil.toString(e, printStack);
    }

    public static VirtualMachineMO pickOneVmOnRunningHost(List<VirtualMachineMO> vmList, boolean bFirstFit) throws Exception {
        List<VirtualMachineMO> candidates = new ArrayList<VirtualMachineMO>();

        for (VirtualMachineMO vmMo : vmList) {
            HostMO hostMo = vmMo.getRunningHost();
            if (hostMo.isHyperHostConnected())
                candidates.add(vmMo);
        }

        if (candidates.size() == 0)
            return null;

        if (bFirstFit)
            return candidates.get(0);

        Random random = new Random();
        return candidates.get(random.nextInt(candidates.size()));
    }

    public static boolean isDvPortGroup(ManagedObjectReference networkMor) {
        return "DistributedVirtualPortgroup".equalsIgnoreCase(networkMor.getType());
    }

    public static boolean isFeatureLicensed(VmwareHypervisorHost hyperHost, String featureKey) throws Exception {
        boolean hotplugSupportedByLicense = false;
        String licenseName;
        LicenseAssignmentManagerMO licenseAssignmentMgrMo;

        licenseAssignmentMgrMo = hyperHost.getLicenseAssignmentManager();
        // Check if license supports the feature
        hotplugSupportedByLicense = licenseAssignmentMgrMo.isFeatureSupported(featureKey, hyperHost.getMor());
        // Fetch license name
        licenseName = licenseAssignmentMgrMo.getHostLicenseName(hyperHost.getMor());

        if (!hotplugSupportedByLicense) {
            throw new Exception("hotplug feature is not supported by license : [" + licenseName + "] assigned to host : " + hyperHost.getHyperHostName());
        }

        return hotplugSupportedByLicense;
    }

    public static String getVCenterSafeUuid(DatastoreMO dsMo) throws Exception{
        // Object name that is greater than 32 is not safe in vCenter
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        if (dsMo.getDatastoreType().equalsIgnoreCase("VVOL")) {
            return CustomFieldConstants.CLOUD_UUID + "-" + uuid;
        }
        return uuid;
    }

    public static String getRecommendedDiskControllerFromDescriptor(GuestOsDescriptor guestOsDescriptor) throws Exception {
        String recommendedController;

        recommendedController = guestOsDescriptor.getRecommendedDiskController();

        // By-pass auto detected PVSCSI controller to use LsiLogic Parallel instead
        if (DiskControllerType.getType(recommendedController) == DiskControllerType.pvscsi) {
            recommendedController = DiskControllerType.lsilogic.toString();
        }

        return recommendedController;
    }

    public static String trimSnapshotDeltaPostfix(String name) {
        String[] tokens = name.split("-");
        if (tokens.length > 1 && tokens[tokens.length - 1].matches("[0-9]{6,}")) {
            List<String> trimmedTokens = new ArrayList<String>();
            for (int i = 0; i < tokens.length - 1; i++)
                trimmedTokens.add(tokens[i]);
            return StringUtils.join(trimmedTokens, "-");
        }
        return name;
    }

    public static boolean isControllerOsRecommended(String dataDiskController) {
        return DiskControllerType.getType(dataDiskController) == DiskControllerType.osdefault;
    }

    public static XMLGregorianCalendar getXMLGregorianCalendar(final Date date, final int offsetSeconds) throws DatatypeConfigurationException {
        if (offsetSeconds > 0) {
            date.setTime(date.getTime() - offsetSeconds * 1000);
        }
        final GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTime(date);
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar);
    }

    public static HostMO getHostMOFromHostName(final VmwareContext context, final String hostName) {
        HostMO host = null;
        if (StringUtils.isNotBlank(hostName) && hostName.contains("@")) {
            String hostMorInfo = hostName.split("@")[0];
            if (hostMorInfo.contains(":")) {
                ManagedObjectReference morHost = new ManagedObjectReference();
                morHost.setType(hostMorInfo.split(":")[0]);
                morHost.setValue(hostMorInfo.split(":")[1]);
                host = new HostMO(context, morHost);
            }
        }
        return host;
    }

    public static UnmanagedInstanceTO getUnmanagedInstance(VmwareHypervisorHost hyperHost, VirtualMachineMO vmMo) {
        UnmanagedInstanceTO instance = null;
        try {
            instance = new UnmanagedInstanceTO();
            instance.setName(vmMo.getVmName());
            instance.setInternalCSName(vmMo.getInternalCSName());
            instance.setCpuCoresPerSocket(vmMo.getCoresPerSocket());
            instance.setOperatingSystemId(vmMo.getVmGuestInfo().getGuestId());
            VirtualMachineConfigSummary configSummary = vmMo.getConfigSummary();
            if (configSummary != null) {
                instance.setCpuCores(configSummary.getNumCpu());
                instance.setCpuSpeed(configSummary.getCpuReservation());
                instance.setMemory(configSummary.getMemorySizeMB());
            }

            try {
                ClusterMO clusterMo = new ClusterMO(hyperHost.getContext(), hyperHost.getHyperHostCluster());
                instance.setClusterName(clusterMo.getName());
            } catch (Exception e) {
                LOGGER.warn("Unable to get unmanaged instance cluster info, due to: " + e.getMessage());
            }

            instance.setHostName(hyperHost.getHyperHostName());

            if (StringUtils.isEmpty(instance.getOperatingSystemId()) && configSummary != null) {
                instance.setOperatingSystemId(configSummary.getGuestId());
            }
            VirtualMachineGuestOsIdentifier osIdentifier = VirtualMachineGuestOsIdentifier.OTHER_GUEST;
            try {
                osIdentifier = VirtualMachineGuestOsIdentifier.fromValue(instance.getOperatingSystemId());
            } catch (IllegalArgumentException iae) {
                if (StringUtils.isNotEmpty(instance.getOperatingSystemId()) && instance.getOperatingSystemId().contains("64")) {
                    osIdentifier = VirtualMachineGuestOsIdentifier.OTHER_GUEST_64;
                }
            }
            instance.setOperatingSystem(vmMo.getGuestInfo().getGuestFullName());
            if (StringUtils.isEmpty(instance.getOperatingSystem()) && configSummary != null) {
                instance.setOperatingSystem(configSummary.getGuestFullName());
            }
            UnmanagedInstanceTO.PowerState powerState = UnmanagedInstanceTO.PowerState.PowerUnknown;
            if (vmMo.getPowerState().toString().equalsIgnoreCase("POWERED_ON")) {
                powerState = UnmanagedInstanceTO.PowerState.PowerOn;
                instance.setCpuSpeed(vmMo.getRuntimeInfo().getMaxCpuUsage() / instance.getCpuCores());
            }
            if (vmMo.getPowerState().toString().equalsIgnoreCase("POWERED_OFF")) {
                powerState = UnmanagedInstanceTO.PowerState.PowerOff;
            }
            instance.setPowerState(powerState);
            instance.setDisks(getUnmanageInstanceDisks(vmMo));
            instance.setNics(getUnmanageInstanceNics(hyperHost, vmMo));
        } catch (Exception e) {
            LOGGER.error("Unable to retrieve unmanaged instance info, due to: " + e.getMessage());
        }
        return instance;
    }

    protected static List<UnmanagedInstanceTO.Disk> getUnmanageInstanceDisks(VirtualMachineMO vmMo) {
        List<UnmanagedInstanceTO.Disk> instanceDisks = new ArrayList<>();
        VirtualDisk[] disks = null;
        try {
            disks = vmMo.getAllDiskDevice();
        } catch (Exception e) {
            LOGGER.info("Unable to retrieve unmanaged instance disks. " + e.getMessage());
        }
        if (disks != null) {
            for (VirtualDevice diskDevice : disks) {
                try {
                    if (diskDevice instanceof VirtualDisk) {
                        UnmanagedInstanceTO.Disk instanceDisk = new UnmanagedInstanceTO.Disk();
                        VirtualDisk disk = (VirtualDisk) diskDevice;
                        instanceDisk.setDiskId(disk.getDiskObjectId());
                        instanceDisk.setLabel(disk.getDeviceInfo() != null ? disk.getDeviceInfo().getLabel() : "");
                        instanceDisk.setFileBaseName(vmMo.getVmdkFileBaseName(disk));
                        instanceDisk.setImagePath(getAbsoluteVmdkFile(disk));
                        instanceDisk.setCapacity(disk.getCapacityInBytes());
                        instanceDisk.setPosition(diskDevice.getUnitNumber());
                        DatastoreFile file = new DatastoreFile(getAbsoluteVmdkFile(disk));
                        if (StringUtils.isNoneEmpty(file.getFileBaseName(), file.getDatastoreName())) {
                            VirtualMachineDiskInfo diskInfo = vmMo.getDiskInfoBuilder().getDiskInfoByBackingFileBaseName(file.getFileBaseName(), file.getDatastoreName());
                            instanceDisk.setChainInfo(GsonHelper.getGsonLogger().toJson(diskInfo));
                        }
                        for (VirtualDevice device : vmMo.getAllDeviceList()) {
                            if (diskDevice.getControllerKey() == device.getKey()) {
                                if (device instanceof VirtualIDEController) {
                                    instanceDisk.setController(DiskControllerType.getType(device.getClass().getSimpleName()).toString());
                                    instanceDisk.setControllerUnit(((VirtualIDEController) device).getBusNumber());
                                } else if (device instanceof VirtualSCSIController) {
                                    instanceDisk.setController(DiskControllerType.getType(device.getClass().getSimpleName()).toString());
                                    instanceDisk.setControllerUnit(((VirtualSCSIController) device).getBusNumber());
                                } else {
                                    instanceDisk.setController(DiskControllerType.none.toString());
                                }
                                break;
                            }
                        }
                        if (disk.getBacking() instanceof VirtualDeviceFileBackingInfo) {
                            VirtualDeviceFileBackingInfo diskBacking = (VirtualDeviceFileBackingInfo) disk.getBacking();
                            ManagedObjectReference morDs = diskBacking.getDatastore();
                            DatastoreInfo info = (DatastoreInfo)vmMo.getContext().getVimClient().getDynamicProperty(diskBacking.getDatastore(), "info");
                            if (info instanceof NasDatastoreInfo) {
                                NasDatastoreInfo dsInfo = (NasDatastoreInfo) info;
                                instanceDisk.setDatastoreName(dsInfo.getName());
                                if (dsInfo.getNas() != null) {
                                    instanceDisk.setDatastoreHost(dsInfo.getNas().getRemoteHost());
                                    instanceDisk.setDatastorePath(dsInfo.getNas().getRemotePath());
                                    instanceDisk.setDatastoreType(dsInfo.getNas().getType());
                                }
                            } else {
                                instanceDisk.setDatastoreName(info.getName());
                            }
                        }
                        LOGGER.info(vmMo.getName() + " " + disk.getDeviceInfo().getLabel() + " " + disk.getDeviceInfo().getSummary() + " " + disk.getDiskObjectId() + " " + disk.getCapacityInKB() + " " + instanceDisk.getController());
                        instanceDisks.add(instanceDisk);
                    }
                } catch (Exception e) {
                    LOGGER.info("Unable to retrieve unmanaged instance disk info. " + e.getMessage());
                }
            }
            Collections.sort(instanceDisks, new Comparator<UnmanagedInstanceTO.Disk>() {
                @Override
                public int compare(final UnmanagedInstanceTO.Disk disk1, final UnmanagedInstanceTO.Disk disk2) {
                    return extractInt(disk1) - extractInt(disk2);
                }

                int extractInt(UnmanagedInstanceTO.Disk disk) {
                    String num = disk.getLabel().replaceAll("\\D", "");
                    // return 0 if no digits found
                    return num.isEmpty() ? 0 : Integer.parseInt(num);
                }
            });
        }
        return instanceDisks;
    }

    private static List<UnmanagedInstanceTO.Nic> getUnmanageInstanceNics(VmwareHypervisorHost hyperHost, VirtualMachineMO vmMo) {
        List<UnmanagedInstanceTO.Nic> instanceNics = new ArrayList<>();

        HashMap<String, List<String>> guestNicMacIPAddressMap = new HashMap<>();
        try {
            GuestInfo guestInfo = vmMo.getGuestInfo();
            if (guestInfo.getToolsStatus() == VirtualMachineToolsStatus.TOOLS_OK) {
                for (GuestNicInfo nicInfo: guestInfo.getNet()) {
                    if (CollectionUtils.isNotEmpty(nicInfo.getIpAddress())) {
                        List<String> ipAddresses = new ArrayList<>();
                        for (String ipAddress : nicInfo.getIpAddress()) {
                            if (NetUtils.isValidIp4(ipAddress)) {
                                ipAddresses.add(ipAddress);
                            }
                        }
                        guestNicMacIPAddressMap.put(nicInfo.getMacAddress(), ipAddresses);
                    }
                }
            } else {
                LOGGER.info(String.format("Unable to retrieve guest nics for instance: %s from VMware tools as tools status: %s", vmMo.getName(), guestInfo.getToolsStatus().toString()));
            }
        } catch (Exception e) {
            LOGGER.info("Unable to retrieve guest nics for instance from VMware tools. " + e.getMessage());
        }
        VirtualDevice[] nics = null;
        try {
            nics = vmMo.getNicDevices();
        } catch (Exception e) {
            LOGGER.info("Unable to retrieve unmanaged instance nics. " + e.getMessage());
        }
        if (nics != null) {
            for (VirtualDevice nic : nics) {
                try {
                    VirtualEthernetCard ethCardDevice = (VirtualEthernetCard) nic;
                    LOGGER.error(nic.getClass().getCanonicalName() + " " + nic.getBacking().getClass().getCanonicalName() + " " + ethCardDevice.getMacAddress());
                    UnmanagedInstanceTO.Nic instanceNic = new UnmanagedInstanceTO.Nic();
                    instanceNic.setNicId(ethCardDevice.getDeviceInfo().getLabel());
                    if (ethCardDevice instanceof VirtualPCNet32) {
                        instanceNic.setAdapterType(VirtualEthernetCardType.PCNet32.toString());
                    } else if (ethCardDevice instanceof VirtualVmxnet2) {
                        instanceNic.setAdapterType(VirtualEthernetCardType.Vmxnet2.toString());
                    } else if (ethCardDevice instanceof VirtualVmxnet3) {
                        instanceNic.setAdapterType(VirtualEthernetCardType.Vmxnet3.toString());
                    } else {
                        instanceNic.setAdapterType(VirtualEthernetCardType.E1000.toString());
                    }
                    instanceNic.setMacAddress(ethCardDevice.getMacAddress());
                    if (guestNicMacIPAddressMap.containsKey(instanceNic.getMacAddress())) {
                        instanceNic.setIpAddress(guestNicMacIPAddressMap.get(instanceNic.getMacAddress()));
                    }
                    if (ethCardDevice.getSlotInfo() != null) {
                        instanceNic.setPciSlot(ethCardDevice.getSlotInfo().toString());
                    }
                    VirtualDeviceBackingInfo backing = ethCardDevice.getBacking();
                    if (backing instanceof VirtualEthernetCardDistributedVirtualPortBackingInfo) {
                        VirtualEthernetCardDistributedVirtualPortBackingInfo backingInfo = (VirtualEthernetCardDistributedVirtualPortBackingInfo) backing;
                        DistributedVirtualSwitchPortConnection port = backingInfo.getPort();
                        String portKey = port.getPortKey();
                        String portGroupKey = port.getPortgroupKey();
                        String dvSwitchUuid = port.getSwitchUuid();

                        LOGGER.debug("NIC " + nic.toString() + " is connected to dvSwitch " + dvSwitchUuid + " pg " + portGroupKey + " port " + portKey);

                        ManagedObjectReference dvSwitchManager = vmMo.getContext().getVimClient().getServiceContent().getDvSwitchManager();
                        ManagedObjectReference dvSwitch = vmMo.getContext().getVimClient().getService().queryDvsByUuid(dvSwitchManager, dvSwitchUuid);

                        // Get all ports
                        DistributedVirtualSwitchPortCriteria criteria = new DistributedVirtualSwitchPortCriteria();
                        criteria.setInside(true);
                        criteria.getPortgroupKey().add(portGroupKey);
                        List<DistributedVirtualPort> dvPorts = vmMo.getContext().getVimClient().getService().fetchDVPorts(dvSwitch, criteria);

                        for (DistributedVirtualPort dvPort : dvPorts) {
                            // Find the port for this NIC by portkey
                            if (portKey.equals(dvPort.getKey())) {
                                VMwareDVSPortSetting settings = (VMwareDVSPortSetting) dvPort.getConfig().getSetting();
                                if (settings.getVlan() instanceof VmwareDistributedVirtualSwitchVlanIdSpec) {
                                    VmwareDistributedVirtualSwitchVlanIdSpec vlanId = (VmwareDistributedVirtualSwitchVlanIdSpec) settings.getVlan();
                                    LOGGER.trace("Found port " + dvPort.getKey() + " with vlan " + vlanId.getVlanId());
                                    if (vlanId.getVlanId() > 0 && vlanId.getVlanId() < 4095) {
                                        instanceNic.setVlan(vlanId.getVlanId());
                                    }
                                } else if (settings.getVlan() instanceof VmwareDistributedVirtualSwitchPvlanSpec) {
                                    VmwareDistributedVirtualSwitchPvlanSpec pvlanSpec = (VmwareDistributedVirtualSwitchPvlanSpec) settings.getVlan();
                                    LOGGER.trace("Found port " + dvPort.getKey() + " with pvlan " + pvlanSpec.getPvlanId());
                                    if (pvlanSpec.getPvlanId() > 0 && pvlanSpec.getPvlanId() < 4095) {
                                        DistributedVirtualSwitchMO dvSwitchMo = new DistributedVirtualSwitchMO(vmMo.getContext(), dvSwitch);
                                        Pair<Integer, HypervisorHostHelper.PvlanType> vlanDetails = dvSwitchMo.retrieveVlanFromPvlan(pvlanSpec.getPvlanId(), dvSwitch);
                                        if (vlanDetails != null && vlanDetails.first() != null && vlanDetails.second() != null) {
                                            instanceNic.setVlan(vlanDetails.first());
                                            instanceNic.setPvlan(pvlanSpec.getPvlanId());
                                            instanceNic.setPvlanType(vlanDetails.second().toString());
                                        }
                                    }
                                }
                                break;
                            }
                        }
                    } else if (backing instanceof VirtualEthernetCardNetworkBackingInfo) {
                        VirtualEthernetCardNetworkBackingInfo backingInfo = (VirtualEthernetCardNetworkBackingInfo) backing;
                        instanceNic.setNetwork(backingInfo.getDeviceName());
                        if (hyperHost instanceof HostMO) {
                            HostMO hostMo = (HostMO) hyperHost;
                            HostPortGroupSpec portGroupSpec = hostMo.getHostPortGroupSpec(backingInfo.getDeviceName());
                            instanceNic.setVlan(portGroupSpec.getVlanId());
                        }
                    }
                    instanceNics.add(instanceNic);
                } catch (Exception e) {
                    LOGGER.info("Unable to retrieve unmanaged instance nic info. " + e.getMessage());
                }
            }
            Collections.sort(instanceNics, new Comparator<UnmanagedInstanceTO.Nic>() {
                @Override
                public int compare(final UnmanagedInstanceTO.Nic nic1, final UnmanagedInstanceTO.Nic nic2) {
                    return extractInt(nic1) - extractInt(nic2);
                }

                int extractInt(UnmanagedInstanceTO.Nic nic) {
                    String num = nic.getNicId().replaceAll("\\D", "");
                    // return 0 if no digits found
                    return num.isEmpty() ? 0 : Integer.parseInt(num);
                }
            });
        }
        return  instanceNics;
    }

    public static String getAbsoluteVmdkFile(VirtualDisk disk) {
        String vmdkAbsFile = null;
        VirtualDeviceBackingInfo backingInfo = disk.getBacking();
        if (backingInfo instanceof VirtualDiskFlatVer2BackingInfo) {
            VirtualDiskFlatVer2BackingInfo diskBackingInfo = (VirtualDiskFlatVer2BackingInfo) backingInfo;
            vmdkAbsFile = diskBackingInfo.getFileName();
        }
        return vmdkAbsFile;
    }

    /**
     * Validates an instance's <code>rootDiskController</code> and <code>dataDiskController</code> details. Throws a
     * <code>CloudRuntimeException</code> if they are invalid.
     */
    public static void validateDiskControllerDetails(String rootDiskControllerDetail, String dataDiskControllerDetail) {
        rootDiskControllerDetail = DiskControllerType.getType(rootDiskControllerDetail).toString();
        if (DiskControllerType.getType(rootDiskControllerDetail) == DiskControllerType.none) {
            throw new CloudRuntimeException(String.format("[%s] is not a valid root disk controller", rootDiskControllerDetail));
        }
        dataDiskControllerDetail = DiskControllerType.getType(dataDiskControllerDetail).toString();
        if (DiskControllerType.getType(dataDiskControllerDetail) == DiskControllerType.none) {
            throw new CloudRuntimeException(String.format("[%s] is not a valid data disk controller", dataDiskControllerDetail));
        }
    }

    /**
     * Based on an instance's <code>rootDiskController</code> and <code>dataDiskController</code> details, returns a pair
     * containing the disk controllers that should be used for root disk and the data disks, respectively.
     *
     * @param controllerInfo    pair containing the root disk and data disk controllers, respectively.
     * @param vmMo              virtual machine to derive the recommended disk controllers from. If not null, <code>host</code> and <code>guestOsIdentifier</code> will be ignored.
     * @param host              host to derive the recommended disk controllers from. Must be provided with <code>guestOsIdentifier</code>.
     * @param guestOsIdentifier used to derive the recommended disk controllers from the host.
     */
    public static Pair<String, String> chooseRequiredDiskControllers(Pair<String, String> controllerInfo, VirtualMachineMO vmMo,
                                                                     VmwareHypervisorHost host, String guestOsIdentifier) throws Exception {
        String recommendedDiskControllerClassName = vmMo != null ? vmMo.getRecommendedDiskController(null) : host.getRecommendedDiskController(guestOsIdentifier);
        String recommendedDiskController = DiskControllerType.getType(recommendedDiskControllerClassName).toString();

        String convertedRootDiskController = controllerInfo.first();
        if (isControllerOsRecommended(convertedRootDiskController)) {
            convertedRootDiskController = recommendedDiskController;
        }

        String convertedDataDiskController = controllerInfo.second();
        if (isControllerOsRecommended(convertedDataDiskController)) {
            convertedDataDiskController = recommendedDiskController;
        }

        if (diskControllersShareTheSameBusType(convertedRootDiskController, convertedDataDiskController)) {
            LOGGER.debug("Root and data disk controllers share the same bus type; therefore, we will only use the controllers specified for the root disk.");
            return new Pair<>(convertedRootDiskController, convertedRootDiskController);
        }

        return new Pair<>(convertedRootDiskController, convertedDataDiskController);
    }

    protected static boolean diskControllersShareTheSameBusType(String rootDiskController, String dataDiskController) {
        DiskControllerType rootDiskControllerType = DiskControllerType.getType(rootDiskController);
        DiskControllerType dataDiskControllerType = DiskControllerType.getType(dataDiskController);
        if (rootDiskControllerType.equals(dataDiskControllerType)) {
            return true;
        }
        List<DiskControllerType> scsiDiskControllers = List.of(DiskControllerType.scsi, DiskControllerType.lsilogic, DiskControllerType.lsisas1068,
                DiskControllerType.buslogic ,DiskControllerType.pvscsi);
        return scsiDiskControllers.contains(rootDiskControllerType) && scsiDiskControllers.contains(dataDiskControllerType);
    }

    /**
     * Identifies whether the disk is a root or data disk, and returns the controller from the provided pair that should
     * be used for the disk.
     * @param controllerInfo pair containing the root disk and data disk controllers, respectively.
     */
    public static String getControllerBasedOnDiskType(Pair<String, String> controllerInfo, DiskTO disk) {
        if (disk.getType() == Volume.Type.ROOT || disk.getDiskSeq() == 0) {
            LOGGER.debug(String.format("Choosing disk controller [%s] for the root disk.", controllerInfo.first()));
            return controllerInfo.first();
        }
        LOGGER.debug(String.format("Choosing disk controller [%s] for the data disks.", controllerInfo.second()));
        return controllerInfo.second();
    }
}
