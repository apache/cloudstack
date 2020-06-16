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
package com.cloud.hypervisor.vmware.resource;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.StartAnswer;
import com.cloud.agent.api.StartCommand;
import com.cloud.agent.api.storage.OVFPropertyTO;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.configuration.Resource;
import com.cloud.hypervisor.vmware.manager.VmwareManager;
import com.cloud.hypervisor.vmware.mo.CustomFieldConstants;
import com.cloud.hypervisor.vmware.mo.DatacenterMO;
import com.cloud.hypervisor.vmware.mo.DatastoreFile;
import com.cloud.hypervisor.vmware.mo.DatastoreMO;
import com.cloud.hypervisor.vmware.mo.DiskControllerType;
import com.cloud.hypervisor.vmware.mo.HostMO;
import com.cloud.hypervisor.vmware.mo.HypervisorHostHelper;
import com.cloud.hypervisor.vmware.mo.TaskMO;
import com.cloud.hypervisor.vmware.mo.VirtualEthernetCardType;
import com.cloud.hypervisor.vmware.mo.VirtualMachineDiskInfoBuilder;
import com.cloud.hypervisor.vmware.mo.VirtualMachineMO;
import com.cloud.hypervisor.vmware.mo.VmwareHypervisorHost;
import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.cloud.hypervisor.vmware.util.VmwareHelper;
import com.cloud.network.Networks;
import com.cloud.storage.Storage;
import com.cloud.storage.Volume;
import com.cloud.storage.resource.VmwareStorageLayoutHelper;
import com.cloud.storage.resource.VmwareStorageProcessor;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.nicira.nvp.plugin.NiciraNvpApiVersion;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VmDetailConstants;
import com.vmware.vim25.BoolPolicy;
import com.vmware.vim25.DVPortConfigInfo;
import com.vmware.vim25.DVPortConfigSpec;
import com.vmware.vim25.DasVmPriority;
import com.vmware.vim25.DistributedVirtualPort;
import com.vmware.vim25.DistributedVirtualSwitchPortConnection;
import com.vmware.vim25.DistributedVirtualSwitchPortCriteria;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.OptionValue;
import com.vmware.vim25.VMwareDVSPortSetting;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualDeviceBackingInfo;
import com.vmware.vim25.VirtualDeviceConfigSpec;
import com.vmware.vim25.VirtualDeviceConfigSpecOperation;
import com.vmware.vim25.VirtualDisk;
import com.vmware.vim25.VirtualEthernetCard;
import com.vmware.vim25.VirtualEthernetCardDistributedVirtualPortBackingInfo;
import com.vmware.vim25.VirtualEthernetCardNetworkBackingInfo;
import com.vmware.vim25.VirtualEthernetCardOpaqueNetworkBackingInfo;
import com.vmware.vim25.VirtualMachineBootOptions;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualMachineFileInfo;
import com.vmware.vim25.VirtualMachineFileLayoutEx;
import com.vmware.vim25.VirtualMachineGuestOsIdentifier;
import com.vmware.vim25.VirtualUSBController;
import com.vmware.vim25.VmConfigInfo;
import com.vmware.vim25.VmwareDistributedVirtualSwitchVlanIdSpec;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.storage.configdrive.ConfigDrive;
import org.apache.cloudstack.storage.to.TemplateObjectTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.utils.volume.VirtualMachineDiskInfo;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class StartCommandExecutor {
    private static final Logger LOGGER = Logger.getLogger(StartCommandExecutor.class);

    private final VmwareResource vmwareResource;

    public StartCommandExecutor(VmwareResource vmwareResource) {
        this.vmwareResource = vmwareResource;
    }

    // FR37 the monster blob god method:
    protected StartAnswer execute(StartCommand cmd) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Executing resource StartCommand: " + vmwareResource.getGson().toJson(cmd));
        }

        VirtualMachineTO vmSpec = cmd.getVirtualMachine();

        boolean installAsIs = StringUtils.isNotEmpty(vmSpec.getTemplateLocation());
        // FR37 if startcommand contains a template url deploy OVA as is
        if (installAsIs) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(String.format("deploying OVA as is from %s", vmSpec.getTemplateLocation()));
            }
        }

        VirtualMachineData existingVm = null;

        Pair<String, String> names = composeVmNames(vmSpec);
        String vmInternalCSName = names.first();
        String vmNameOnVcenter = names.second();

        DiskTO rootDiskTO = null;

        Pair<String, String> controllerInfo = getDiskControllerInfo(vmSpec);

        Boolean systemVm = vmSpec.getType().isUsedBySystem();

        // Thus, vmInternalCSName always holds i-x-y, the cloudstack generated internal VM name. FR37 this is an out of place comment
        VmwareContext context = vmwareResource.getServiceContext();
        DatacenterMO dcMo = null;
        try {
            VmwareManager mgr = context.getStockObject(VmwareManager.CONTEXT_STOCK_NAME);

            VmwareHypervisorHost hyperHost = vmwareResource.getHyperHost(context);
            dcMo = new DatacenterMO(hyperHost.getContext(), hyperHost.getHyperHostDatacenter());

            checkIfVmExistsInVcenter(vmInternalCSName, vmNameOnVcenter, dcMo);

            // FR37 disks should not yet be our concern
            String guestOsId = translateGuestOsIdentifier(vmSpec.getArch(), vmSpec.getOs(), vmSpec.getPlatformEmulator()).value();
            DiskTO[] disks = validateDisks(vmSpec.getDisks());
            // FR37 this assert is not usefull if disks may be reconsiled later
            assert (disks.length > 0);

            NicTO[] nics = vmSpec.getNics();

            HashMap<String, Pair<ManagedObjectReference, DatastoreMO>> dataStoresDetails = inferDatastoreDetailsFromDiskInfo(hyperHost, context, disks, cmd);
            if ((dataStoresDetails == null) || (dataStoresDetails.isEmpty())) {
                String msg = "Unable to locate datastore details of the volumes to be attached";
                LOGGER.error(msg);
                // throw a more specific Exception
                throw new Exception(msg);
            }

            DatastoreMO dsRootVolumeIsOn = getDatastoreThatRootDiskIsOn(dataStoresDetails, disks);
            if (dsRootVolumeIsOn == null) {
                String msg = "Unable to locate datastore details of root volume";
                LOGGER.error(msg);
                // throw a more specific Exception
                throw new Exception(msg);
            }

            VirtualMachineDiskInfoBuilder diskInfoBuilder = null;
            VirtualMachineMO vmMo = hyperHost.findVmOnHyperHost(vmInternalCSName);
            DiskControllerType systemVmScsiControllerType = DiskControllerType.lsilogic;
            int firstScsiControllerBusNum = 0;
            int numScsiControllerForSystemVm = 1;
            boolean hasSnapshot = false;
            if (vmMo != null) {
                LOGGER.info("VM " + vmInternalCSName + " already exists, tear down devices for reconfiguration");
                if (VmwareResource.getVmPowerState(vmMo) != VirtualMachine.PowerState.PowerOff)
                    vmMo.safePowerOff(vmwareResource.getShutdownWaitMs());

                // retrieve disk information before we tear down
                diskInfoBuilder = vmMo.getDiskInfoBuilder();
                hasSnapshot = vmMo.hasSnapshot();
                if (!hasSnapshot)
                    vmMo.tearDownDevices(new Class<?>[] {VirtualDisk.class, VirtualEthernetCard.class});
                else
                    vmMo.tearDownDevices(new Class<?>[] {VirtualEthernetCard.class});
                if (systemVm) {
                    ensureScsiDiskControllers(vmMo, systemVmScsiControllerType.toString(), numScsiControllerForSystemVm, firstScsiControllerBusNum);
                } else {
                    ensureDiskControllers(vmMo, controllerInfo);
                }
            } else {
                ManagedObjectReference morDc = hyperHost.getHyperHostDatacenter();
                assert (morDc != null);

                vmMo = hyperHost.findVmOnPeerHyperHost(vmInternalCSName);
                if (vmMo != null) {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("Found vm " + vmInternalCSName + " at other host, relocate to " + hyperHost.getHyperHostName());
                    }

                    takeVmFromOtherHyperHost(hyperHost, vmInternalCSName);

                    if (VmwareResource.getVmPowerState(vmMo) != VirtualMachine.PowerState.PowerOff)
                        vmMo.safePowerOff(vmwareResource.getShutdownWaitMs());

                    diskInfoBuilder = vmMo.getDiskInfoBuilder();
                    hasSnapshot = vmMo.hasSnapshot();
                    if (!hasSnapshot)
                        vmMo.tearDownDevices(new Class<?>[] {VirtualDisk.class, VirtualEthernetCard.class});
                    else
                        vmMo.tearDownDevices(new Class<?>[] {VirtualEthernetCard.class});

                    if (systemVm) {
                        // System volumes doesn't require more than 1 SCSI controller as there is no requirement for data volumes.
                        ensureScsiDiskControllers(vmMo, systemVmScsiControllerType.toString(), numScsiControllerForSystemVm, firstScsiControllerBusNum);
                    } else {
                        ensureDiskControllers(vmMo, controllerInfo);
                    }
                } else {
                    existingVm = unregisterButHoldOnToOldVmData(vmInternalCSName, dcMo);

                    Pair<ManagedObjectReference, DatastoreMO> rootDiskDataStoreDetails = getRootDiskDataStoreDetails(disks, dataStoresDetails);

                    assert (vmSpec.getMinSpeed() != null) && (rootDiskDataStoreDetails != null);

                    boolean vmFolderExists = rootDiskDataStoreDetails.second().folderExists(String.format("[%s]", rootDiskDataStoreDetails.second().getName()), vmNameOnVcenter);
                    String vmxFileFullPath = dsRootVolumeIsOn.searchFileInSubFolders(vmNameOnVcenter + ".vmx", false, VmwareManager.s_vmwareSearchExcludeFolder.value());
                    if (vmFolderExists && vmxFileFullPath != null) { // VM can be registered only if .vmx is present.
                        registerVm(vmNameOnVcenter, dsRootVolumeIsOn, vmwareResource);
                        vmMo = hyperHost.findVmOnHyperHost(vmInternalCSName);
                        if (vmMo != null) {
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug("Found registered vm " + vmInternalCSName + " at host " + hyperHost.getHyperHostName());
                            }
                        }
                        tearDownVm(vmMo);
                    } else if (installAsIs) {
                        // FR37 create blank or install as is ???? needs to be replaced with the proceudre at
                        // https://code.vmware.com/docs/5540/vsphere-automation-sdks-programming-guide/doc/GUID-82084C78-49FC-4B7F-BD89-F90D5AA22631.html
                        hyperHost.importVmFromOVF(vmSpec.getTemplateLocation(), vmNameOnVcenter, rootDiskDataStoreDetails.second(), "thin", false);
                        // FR37 importUnmanaged code must be called
                        // FR37 this must be called before starting
                        // FR37 existing serviceOffering with the right (minimum) dimensions must exist
                    } else {
                        if (!hyperHost
                                .createBlankVm(vmNameOnVcenter, vmInternalCSName, vmSpec.getCpus(), vmSpec.getMaxSpeed(), vmwareResource.getReservedCpuMHZ(vmSpec), vmSpec.getLimitCpuUse(), (int)(vmSpec.getMaxRam() / Resource.ResourceType.bytesToMiB), vmwareResource.getReservedMemoryMb(vmSpec), guestOsId,
                                        rootDiskDataStoreDetails.first(), false, controllerInfo, systemVm)) {
                            throw new Exception("Failed to create VM. vmName: " + vmInternalCSName);
                        }
                    }
                }

                vmMo = hyperHost.findVmOnHyperHost(vmInternalCSName);
                if (vmMo == null) {
                    throw new Exception("Failed to find the newly create or relocated VM. vmName: " + vmInternalCSName);
                }
            }

            int totalChangeDevices = disks.length + nics.length;
            // vApp cdrom device
            // HACK ALERT: ovf properties might not be the only or defining feature of vApps; needs checking
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("adding divice tie device count for vApp config ISO");
            }
            if (vmSpec.getOvfProperties() != null) {
                totalChangeDevices++;
            }

            DiskTO volIso = null;
            if (vmSpec.getType() != VirtualMachine.Type.User) {
                // system VM needs a patch ISO
                totalChangeDevices++;
            } else {
                volIso = getIsoDiskTO(disks);
                if (volIso == null)
                    totalChangeDevices++;
            }

            VirtualMachineConfigSpec vmConfigSpec = new VirtualMachineConfigSpec();

            VmwareHelper.setBasicVmConfig(vmConfigSpec, vmSpec.getCpus(), vmSpec.getMaxSpeed(), vmwareResource.getReservedCpuMHZ(vmSpec), (int)(vmSpec.getMaxRam() / (1024 * 1024)),
                    vmwareResource.getReservedMemoryMb(vmSpec), guestOsId, vmSpec.getLimitCpuUse());

            // Check for multi-cores per socket settings
            int numCoresPerSocket = 1;
            String coresPerSocket = vmSpec.getDetails().get(VmDetailConstants.CPU_CORE_PER_SOCKET);
            if (coresPerSocket != null) {
                String apiVersion = HypervisorHostHelper.getVcenterApiVersion(vmMo.getContext());
                // Property 'numCoresPerSocket' is supported since vSphere API 5.0
                if (apiVersion.compareTo("5.0") >= 0) {
                    numCoresPerSocket = NumbersUtil.parseInt(coresPerSocket, 1);
                    vmConfigSpec.setNumCoresPerSocket(numCoresPerSocket);
                }
            }

            // Check for hotadd settings
            vmConfigSpec.setMemoryHotAddEnabled(vmMo.isMemoryHotAddSupported(guestOsId));

            String hostApiVersion = ((HostMO)hyperHost).getHostAboutInfo().getApiVersion();
            if (numCoresPerSocket > 1 && hostApiVersion.compareTo("5.0") < 0) {
                LOGGER.warn("Dynamic scaling of CPU is not supported for Virtual Machines with multi-core vCPUs in case of ESXi hosts 4.1 and prior. Hence CpuHotAdd will not be"
                        + " enabled for Virtual Machine: " + vmInternalCSName);
                vmConfigSpec.setCpuHotAddEnabled(false);
            } else {
                vmConfigSpec.setCpuHotAddEnabled(vmMo.isCpuHotAddSupported(guestOsId));
            }

            vmwareResource.configNestedHVSupport(vmMo, vmSpec, vmConfigSpec);

            VirtualDeviceConfigSpec[] deviceConfigSpecArray = new VirtualDeviceConfigSpec[totalChangeDevices];
            int i = 0;
            int ideUnitNumber = 0;
            int scsiUnitNumber = 0;
            int ideControllerKey = vmMo.getIDEDeviceControllerKey();
            int scsiControllerKey = 0;//vmMo.getGenericScsiDeviceControllerKeyNoException();
            int controllerKey;

            //
            // Setup ISO device
            //

            // vAPP ISO
            // FR37 the native deploy mechs should create this for us
            if (vmSpec.getOvfProperties() != null) {
                if (LOGGER.isTraceEnabled()) {
                    // FR37 TODO add more usefull info (if we keep this bit
                    LOGGER.trace("adding iso for properties for 'xxx'");
                }
                deviceConfigSpecArray[i] = new VirtualDeviceConfigSpec();
                Pair<VirtualDevice, Boolean> isoInfo = VmwareHelper.prepareIsoDevice(vmMo, null, null, true, true, ideUnitNumber++, i + 1);
                deviceConfigSpecArray[i].setDevice(isoInfo.first());
                if (isoInfo.second()) {
                    if (LOGGER.isDebugEnabled())
                        LOGGER.debug("Prepare vApp ISO volume at existing device " + vmwareResource.getGson().toJson(isoInfo.first()));

                    deviceConfigSpecArray[i].setOperation(VirtualDeviceConfigSpecOperation.ADD);
                } else {
                    if (LOGGER.isDebugEnabled())
                        LOGGER.debug("Prepare vApp ISO volume at existing device " + vmwareResource.getGson().toJson(isoInfo.first()));

                    deviceConfigSpecArray[i].setOperation(VirtualDeviceConfigSpecOperation.EDIT);
                }
                i++;
            }

            // prepare systemvm patch ISO
            if (vmSpec.getType() != VirtualMachine.Type.User) {
                // attach ISO (for patching of system VM)
                Pair<String, Long> secStoreUrlAndId = mgr.getSecondaryStorageStoreUrlAndId(Long.parseLong(vmwareResource.getDcId()));
                String secStoreUrl = secStoreUrlAndId.first();
                Long secStoreId = secStoreUrlAndId.second();
                if (secStoreUrl == null) {
                    String msg = "secondary storage for dc " + vmwareResource.getDcId() + " is not ready yet?";
                    throw new Exception(msg);
                }
                mgr.prepareSecondaryStorageStore(secStoreUrl, secStoreId);

                ManagedObjectReference morSecDs = vmwareResource.prepareSecondaryDatastoreOnHost(secStoreUrl);
                if (morSecDs == null) {
                    String msg = "Failed to prepare secondary storage on host, secondary store url: " + secStoreUrl;
                    throw new Exception(msg);
                }
                DatastoreMO secDsMo = new DatastoreMO(hyperHost.getContext(), morSecDs);

                deviceConfigSpecArray[i] = new VirtualDeviceConfigSpec();
                Pair<VirtualDevice, Boolean> isoInfo = VmwareHelper
                        .prepareIsoDevice(vmMo, String.format("[%s] systemvm/%s", secDsMo.getName(), mgr.getSystemVMIsoFileNameOnDatastore()), secDsMo.getMor(), true, true,
                                ideUnitNumber++, i + 1);
                deviceConfigSpecArray[i].setDevice(isoInfo.first());
                if (isoInfo.second()) {
                    if (LOGGER.isDebugEnabled())
                        LOGGER.debug("Prepare ISO volume at new device " + vmwareResource.getGson().toJson(isoInfo.first()));
                    deviceConfigSpecArray[i].setOperation(VirtualDeviceConfigSpecOperation.ADD);
                } else {
                    if (LOGGER.isDebugEnabled())
                        LOGGER.debug("Prepare ISO volume at existing device " + vmwareResource.getGson().toJson(isoInfo.first()));
                    deviceConfigSpecArray[i].setOperation(VirtualDeviceConfigSpecOperation.EDIT);
                }
                i++;
            } else {
                // Note: we will always plug a CDROM device
                if (volIso != null) {
                    for (DiskTO vol : disks) {
                        if (vol.getType() == Volume.Type.ISO) {

                            TemplateObjectTO iso = (TemplateObjectTO)vol.getData();

                            if (iso.getPath() != null && !iso.getPath().isEmpty()) {
                                DataStoreTO imageStore = iso.getDataStore();
                                if (!(imageStore instanceof NfsTO)) {
                                    LOGGER.debug("unsupported protocol");
                                    throw new Exception("unsupported protocol");
                                }
                                NfsTO nfsImageStore = (NfsTO)imageStore;
                                String isoPath = nfsImageStore.getUrl() + File.separator + iso.getPath();
                                Pair<String, ManagedObjectReference> isoDatastoreInfo = getIsoDatastoreInfo(hyperHost, isoPath);
                                assert (isoDatastoreInfo != null);
                                assert (isoDatastoreInfo.second() != null);

                                deviceConfigSpecArray[i] = new VirtualDeviceConfigSpec();
                                Pair<VirtualDevice, Boolean> isoInfo = VmwareHelper
                                        .prepareIsoDevice(vmMo, isoDatastoreInfo.first(), isoDatastoreInfo.second(), true, true, ideUnitNumber++, i + 1);
                                deviceConfigSpecArray[i].setDevice(isoInfo.first());
                                if (isoInfo.second()) {
                                    if (LOGGER.isDebugEnabled())
                                        LOGGER.debug("Prepare ISO volume at new device " + vmwareResource.getGson().toJson(isoInfo.first()));
                                    deviceConfigSpecArray[i].setOperation(VirtualDeviceConfigSpecOperation.ADD);
                                } else {
                                    if (LOGGER.isDebugEnabled())
                                        LOGGER.debug("Prepare ISO volume at existing device " + vmwareResource.getGson().toJson(isoInfo.first()));
                                    deviceConfigSpecArray[i].setOperation(VirtualDeviceConfigSpecOperation.EDIT);
                                }
                            }
                            i++;
                        }
                    }
                } else {
                    deviceConfigSpecArray[i] = new VirtualDeviceConfigSpec();
                    Pair<VirtualDevice, Boolean> isoInfo = VmwareHelper.prepareIsoDevice(vmMo, null, null, true, true, ideUnitNumber++, i + 1);
                    deviceConfigSpecArray[i].setDevice(isoInfo.first());
                    if (isoInfo.second()) {
                        if (LOGGER.isDebugEnabled())
                            LOGGER.debug("Prepare ISO volume at existing device " + vmwareResource.getGson().toJson(isoInfo.first()));

                        deviceConfigSpecArray[i].setOperation(VirtualDeviceConfigSpecOperation.ADD);
                    } else {
                        if (LOGGER.isDebugEnabled())
                            LOGGER.debug("Prepare ISO volume at existing device " + vmwareResource.getGson().toJson(isoInfo.first()));

                        deviceConfigSpecArray[i].setOperation(VirtualDeviceConfigSpecOperation.EDIT);
                    }
                    i++;
                }
            }

            //
            // Setup ROOT/DATA disk devices
            //
            DiskTO[] sortedDisks = sortVolumesByDeviceId(disks);
            for (DiskTO vol : sortedDisks) {
                if (vol.getType() == Volume.Type.ISO)
                    continue;

                VirtualMachineDiskInfo matchingExistingDisk = getMatchingExistingDisk(diskInfoBuilder, vol, hyperHost, context);
                controllerKey = getDiskController(matchingExistingDisk, vol, vmSpec, ideControllerKey, scsiControllerKey);
                String diskController = getDiskController(vmMo, matchingExistingDisk, vol, controllerInfo);

                if (DiskControllerType.getType(diskController) == DiskControllerType.osdefault) {
                    diskController = vmMo.getRecommendedDiskController(null);
                }
                if (DiskControllerType.getType(diskController) == DiskControllerType.ide) {
                    controllerKey = vmMo.getIDEControllerKey(ideUnitNumber);
                    if (vol.getType() == Volume.Type.DATADISK) {
                        // Could be result of flip due to user configured setting or "osdefault" for data disks
                        // Ensure maximum of 2 data volumes over IDE controller, 3 includeing root volume
                        if (vmMo.getNumberOfVirtualDisks() > 3) {
                            throw new CloudRuntimeException(
                                    "Found more than 3 virtual disks attached to this VM [" + vmMo.getVmName() + "]. Unable to implement the disks over " + diskController + " controller, as maximum number of devices supported over IDE controller is 4 includeing CDROM device.");
                        }
                    }
                } else {
                    controllerKey = vmMo.getScsiDiskControllerKeyNoException(diskController, 0);
                    if (controllerKey == -1) {
                        // This may happen for ROOT legacy VMs which doesn't have recommended disk controller when global configuration parameter 'vmware.root.disk.controller' is set to "osdefault"
                        // Retrieve existing controller and use.
                        Ternary<Integer, Integer, DiskControllerType> vmScsiControllerInfo = vmMo.getScsiControllerInfo();
                        DiskControllerType existingControllerType = vmScsiControllerInfo.third();
                        controllerKey = vmMo.getScsiDiskControllerKeyNoException(existingControllerType.toString(), 0);
                    }
                }
                if (!hasSnapshot) {
                    deviceConfigSpecArray[i] = new VirtualDeviceConfigSpec();

                    VolumeObjectTO volumeTO = (VolumeObjectTO)vol.getData();
                    DataStoreTO primaryStore = volumeTO.getDataStore();
                    Map<String, String> details = vol.getDetails();
                    boolean managed = false;
                    String iScsiName = null;

                    if (details != null) {
                        managed = Boolean.parseBoolean(details.get(DiskTO.MANAGED));
                        iScsiName = details.get(DiskTO.IQN);
                    }

                    // if the storage is managed, iScsiName should not be null
                    String datastoreName = managed ? VmwareResource.getDatastoreName(iScsiName) : primaryStore.getUuid();
                    Pair<ManagedObjectReference, DatastoreMO> volumeDsDetails = dataStoresDetails.get(datastoreName);

                    assert (volumeDsDetails != null);

                    String[] diskChain = syncDiskChain(dcMo, vmMo, vmSpec, vol, matchingExistingDisk, dataStoresDetails);
                    if (controllerKey == scsiControllerKey && VmwareHelper.isReservedScsiDeviceNumber(scsiUnitNumber))
                        scsiUnitNumber++;
                    VirtualDevice device = VmwareHelper.prepareDiskDevice(vmMo, null, controllerKey, diskChain, volumeDsDetails.first(),
                            (controllerKey == vmMo.getIDEControllerKey(ideUnitNumber)) ? ((ideUnitNumber++) % VmwareHelper.MAX_IDE_CONTROLLER_COUNT) : scsiUnitNumber++, i + 1);

                    if (vol.getType() == Volume.Type.ROOT)
                        rootDiskTO = vol;
                    deviceConfigSpecArray[i].setDevice(device);
                    deviceConfigSpecArray[i].setOperation(VirtualDeviceConfigSpecOperation.ADD);

                    if (LOGGER.isDebugEnabled())
                        LOGGER.debug("Prepare volume at new device " + vmwareResource.getGson().toJson(device));

                    i++;
                } else {
                    if (controllerKey == scsiControllerKey && VmwareHelper.isReservedScsiDeviceNumber(scsiUnitNumber))
                        scsiUnitNumber++;
                    if (controllerKey == vmMo.getIDEControllerKey(ideUnitNumber))
                        ideUnitNumber++;
                    else
                        scsiUnitNumber++;
                }
            }

            //
            // Setup USB devices
            //
            if (guestOsId.startsWith("darwin")) { //Mac OS
                VirtualDevice[] devices = vmMo.getMatchedDevices(new Class<?>[] {VirtualUSBController.class});
                if (devices.length == 0) {
                    LOGGER.debug("No USB Controller device on VM Start. Add USB Controller device for Mac OS VM " + vmInternalCSName);

                    //For Mac OS X systems, the EHCI+UHCI controller is enabled by default and is required for USB mouse and keyboard access.
                    VirtualDevice usbControllerDevice = VmwareHelper.prepareUSBControllerDevice();
                    deviceConfigSpecArray[i] = new VirtualDeviceConfigSpec();
                    deviceConfigSpecArray[i].setDevice(usbControllerDevice);
                    deviceConfigSpecArray[i].setOperation(VirtualDeviceConfigSpecOperation.ADD);

                    if (LOGGER.isDebugEnabled())
                        LOGGER.debug("Prepare USB controller at new device " + vmwareResource.getGson().toJson(deviceConfigSpecArray[i]));

                    i++;
                } else {
                    LOGGER.debug("USB Controller device exists on VM Start for Mac OS VM " + vmInternalCSName);
                }
            }

            //
            // Setup NIC devices
            //
            VirtualDevice nic;
            int nicMask = 0;
            int nicCount = 0;

            if (vmSpec.getType() == VirtualMachine.Type.DomainRouter) {
                int extraPublicNics = mgr.getRouterExtraPublicNics();
                if (extraPublicNics > 0 && vmSpec.getDetails().containsKey("PeerRouterInstanceName")) {
                    //Set identical MAC address for RvR on extra public interfaces
                    String peerRouterInstanceName = vmSpec.getDetails().get("PeerRouterInstanceName");

                    VirtualMachineMO peerVmMo = hyperHost.findVmOnHyperHost(peerRouterInstanceName);
                    if (peerVmMo == null) {
                        peerVmMo = hyperHost.findVmOnPeerHyperHost(peerRouterInstanceName);
                    }

                    if (peerVmMo != null) {
                        String oldMacSequence = generateMacSequence(nics);

                        for (int nicIndex = nics.length - extraPublicNics; nicIndex < nics.length; nicIndex++) {
                            VirtualDevice nicDevice = peerVmMo.getNicDeviceByIndex(nics[nicIndex].getDeviceId());
                            if (nicDevice != null) {
                                String mac = ((VirtualEthernetCard)nicDevice).getMacAddress();
                                if (mac != null) {
                                    LOGGER.info("Use same MAC as previous RvR, the MAC is " + mac + " for extra NIC with device id: " + nics[nicIndex].getDeviceId());
                                    nics[nicIndex].setMac(mac);
                                }
                            }
                        }

                        if (!StringUtils.isBlank(vmSpec.getBootArgs())) {
                            String newMacSequence = generateMacSequence(nics);
                            vmSpec.setBootArgs(vmwareResource.replaceNicsMacSequenceInBootArgs(oldMacSequence, newMacSequence, vmSpec));
                        }
                    }
                }
            }

            VirtualEthernetCardType nicDeviceType = VirtualEthernetCardType.valueOf(vmSpec.getDetails().get(VmDetailConstants.NIC_ADAPTER));
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("VM " + vmInternalCSName + " will be started with NIC device type: " + nicDeviceType);

            NiciraNvpApiVersion.logNiciraApiVersion();

            Map<String, String> nicUuidToDvSwitchUuid = new HashMap<>();
            for (NicTO nicTo : sortNicsByDeviceId(nics)) {
                LOGGER.info("Prepare NIC device based on NicTO: " + vmwareResource.getGson().toJson(nicTo));

                boolean configureVServiceInNexus = (nicTo.getType() == Networks.TrafficType.Guest) && (vmSpec.getDetails().containsKey("ConfigureVServiceInNexus"));
                VirtualMachine.Type vmType = cmd.getVirtualMachine().getType();
                Pair<ManagedObjectReference, String> networkInfo = vmwareResource.prepareNetworkFromNicInfo(vmMo.getRunningHost(), nicTo, configureVServiceInNexus, vmType);
                if ((nicTo.getBroadcastType() != Networks.BroadcastDomainType.Lswitch) || (nicTo.getBroadcastType() == Networks.BroadcastDomainType.Lswitch && NiciraNvpApiVersion.isApiVersionLowerThan("4.2"))) {
                    if (VmwareHelper.isDvPortGroup(networkInfo.first())) {
                        String dvSwitchUuid;
                        ManagedObjectReference dcMor = hyperHost.getHyperHostDatacenter();
                        DatacenterMO dataCenterMo = new DatacenterMO(context, dcMor);
                        ManagedObjectReference dvsMor = dataCenterMo.getDvSwitchMor(networkInfo.first());
                        dvSwitchUuid = dataCenterMo.getDvSwitchUuid(dvsMor);
                        LOGGER.info("Preparing NIC device on dvSwitch : " + dvSwitchUuid);
                        nic = VmwareHelper.prepareDvNicDevice(vmMo, networkInfo.first(), nicDeviceType, networkInfo.second(), dvSwitchUuid, nicTo.getMac(), i + 1, true, true);
                        if (nicTo.getUuid() != null) {
                            nicUuidToDvSwitchUuid.put(nicTo.getUuid(), dvSwitchUuid);
                        }
                    } else {
                        LOGGER.info("Preparing NIC device on network " + networkInfo.second());
                        nic = VmwareHelper.prepareNicDevice(vmMo, networkInfo.first(), nicDeviceType, networkInfo.second(), nicTo.getMac(), i + 1, true, true);
                    }
                } else {
                    //if NSX API VERSION >= 4.2, connect to br-int (nsx.network), do not create portgroup else previous behaviour
                    nic = VmwareHelper.prepareNicOpaque(vmMo, nicDeviceType, networkInfo.second(), nicTo.getMac(), i + 1, true, true);
                }

                deviceConfigSpecArray[i] = new VirtualDeviceConfigSpec();
                deviceConfigSpecArray[i].setDevice(nic);
                deviceConfigSpecArray[i].setOperation(VirtualDeviceConfigSpecOperation.ADD);

                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("Prepare NIC at new device " + vmwareResource.getGson().toJson(deviceConfigSpecArray[i]));

                // this is really a hacking for DomR, upon DomR startup, we will reset all the NIC allocation after eth3
                if (nicCount < 3)
                    nicMask |= (1 << nicCount);

                i++;
                nicCount++;
            }

            for (int j = 0; j < i; j++)
                vmConfigSpec.getDeviceChange().add(deviceConfigSpecArray[j]);

            //
            // Setup VM options
            //

            // pass boot arguments through machine.id & perform customized options to VMX
            ArrayList<OptionValue> extraOptions = new ArrayList<>();
            configBasicExtraOption(extraOptions, vmSpec);
            configNvpExtraOption(extraOptions, vmSpec, nicUuidToDvSwitchUuid);
            configCustomExtraOption(extraOptions, vmSpec);

            // config for NCC
            VirtualMachine.Type vmType = cmd.getVirtualMachine().getType();
            if (vmType.equals(VirtualMachine.Type.NetScalerVm)) {
                NicTO mgmtNic = vmSpec.getNics()[0];
                OptionValue option = new OptionValue();
                option.setKey("machine.id");
                option.setValue("ip=" + mgmtNic.getIp() + "&netmask=" + mgmtNic.getNetmask() + "&gateway=" + mgmtNic.getGateway());
                extraOptions.add(option);
            }

            // config VNC
            String keyboardLayout = null;
            if (vmSpec.getDetails() != null)
                keyboardLayout = vmSpec.getDetails().get(VmDetailConstants.KEYBOARD);
            vmConfigSpec.getExtraConfig().addAll(
                    Arrays.asList(vmwareResource.configureVnc(extraOptions.toArray(new OptionValue[0]), hyperHost, vmInternalCSName, vmSpec.getVncPassword(), keyboardLayout)));

            // config video card
            vmwareResource.configureVideoCard(vmMo, vmSpec, vmConfigSpec);

            // Set OVF properties (if available)
            Pair<String, List<OVFPropertyTO>> ovfPropsMap = vmSpec.getOvfProperties();
            VmConfigInfo templateVappConfig;
            List<OVFPropertyTO> ovfProperties;
            if (ovfPropsMap != null) {
                String vmTemplate = ovfPropsMap.first();
                LOGGER.info("Find VM template " + vmTemplate);
                VirtualMachineMO vmTemplateMO = dcMo.findVm(vmTemplate);
                templateVappConfig = vmTemplateMO.getConfigInfo().getVAppConfig();
                ovfProperties = ovfPropsMap.second();
                // Set OVF properties (if available)
                if (CollectionUtils.isNotEmpty(ovfProperties)) {
                    LOGGER.info("Copying OVF properties from template and setting them to the values the user provided");
                    vmwareResource.copyVAppConfigsFromTemplate(templateVappConfig, ovfProperties, vmConfigSpec);
                }
            }

            checkBootOptions(vmSpec, vmConfigSpec);

            //
            // Configure VM
            //
            if (!vmMo.configureVm(vmConfigSpec)) {
                throw new Exception("Failed to configure VM before start. vmName: " + vmInternalCSName);
            }

            if (vmSpec.getType() == VirtualMachine.Type.DomainRouter) {
                hyperHost.setRestartPriorityForVM(vmMo, DasVmPriority.HIGH.value());
            }

            // Resizing root disk only when explicit requested by user
            final Map<String, String> vmDetails = cmd.getVirtualMachine().getDetails();
            if (rootDiskTO != null && !hasSnapshot && (vmDetails != null && vmDetails.containsKey(ApiConstants.ROOT_DISK_SIZE))) {
                resizeRootDiskOnVMStart(vmMo, rootDiskTO, hyperHost, context);
            }

            //
            // Post Configuration
            //

            vmMo.setCustomFieldValue(CustomFieldConstants.CLOUD_NIC_MASK, String.valueOf(nicMask));
            postNvpConfigBeforeStart(vmMo, vmSpec);

            Map<String, Map<String, String>> iqnToData = new HashMap<>();

            postDiskConfigBeforeStart(vmMo, vmSpec, sortedDisks, ideControllerKey, scsiControllerKey, iqnToData, hyperHost, context);

            //
            // Power-on VM
            //
            if (!vmMo.powerOn()) {
                throw new Exception("Failed to start VM. vmName: " + vmInternalCSName + " with hostname " + vmNameOnVcenter);
            }

            StartAnswer startAnswer = new StartAnswer(cmd);

            startAnswer.setIqnToData(iqnToData);

            // Since VM was successfully powered-on, if there was an existing VM in a different cluster that was unregistered, delete all the files associated with it.
            if (existingVm != null && existingVm.vmName != null && existingVm.vmFileLayout != null) {
                List<String> vmDatastoreNames = new ArrayList<>();
                for (DatastoreMO vmDatastore : vmMo.getAllDatastores()) {
                    vmDatastoreNames.add(vmDatastore.getName());
                }
                // Don't delete files that are in a datastore that is being used by the new VM as well (zone-wide datastore).
                List<String> skipDatastores = new ArrayList<>();
                for (DatastoreMO existingDatastore : existingVm.datastores) {
                    if (vmDatastoreNames.contains(existingDatastore.getName())) {
                        skipDatastores.add(existingDatastore.getName());
                    }
                }
                vmwareResource.deleteUnregisteredVmFiles(existingVm.vmFileLayout, dcMo, true, skipDatastores);
            }

            return startAnswer;
        } catch (Throwable e) {
            if (e instanceof RemoteException) {
                LOGGER.warn("Encounter remote exception to vCenter, invalidate VMware session context");
                vmwareResource.invalidateServiceContext();
            }

            String msg = "StartCommand failed due to " + VmwareHelper.getExceptionMessage(e);
            LOGGER.warn(msg, e);
            StartAnswer startAnswer = new StartAnswer(cmd, msg);
            if ( e instanceof VmAlreadyExistsInVcenter) {
                startAnswer.setContextParam("stopRetry", "true");
            }
            reRegisterExistingVm(existingVm, dcMo);

            return startAnswer;
        } finally {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(String.format("finally done with %s",  vmwareResource.getGson().toJson(cmd)));
            }
        }
    }

    /**
     * If a VM with the same name is found in a different cluster in the DC, unregister the old VM and configure a new VM (cold-migration).
     */
    private VirtualMachineData unregisterButHoldOnToOldVmData(String vmInternalCSName, DatacenterMO dcMo) throws Exception {
        VirtualMachineMO existingVmInDc = dcMo.findVm(vmInternalCSName);
        VirtualMachineData existingVm = null;
        if (existingVmInDc != null) {
            existingVm = new VirtualMachineData();
            existingVm.vmName = existingVmInDc.getName();
            existingVm.vmFileInfo = existingVmInDc.getFileInfo();
            existingVm.vmFileLayout = existingVmInDc.getFileLayout();
            existingVm.datastores = existingVmInDc.getAllDatastores();
            LOGGER.info("Found VM: " + vmInternalCSName + " on a host in a different cluster. Unregistering the exisitng VM.");
            existingVmInDc.unregisterVm();
        }
        return existingVm;
    }

    private Pair<ManagedObjectReference, DatastoreMO> getRootDiskDataStoreDetails(DiskTO[] disks, HashMap<String, Pair<ManagedObjectReference, DatastoreMO>> dataStoresDetails) {
        Pair<ManagedObjectReference, DatastoreMO> rootDiskDataStoreDetails = null;
        for (DiskTO vol : disks) {
            if (vol.getType() == Volume.Type.ROOT) {
                Map<String, String> details = vol.getDetails();
                boolean managed = false;

                if (details != null) {
                    managed = Boolean.parseBoolean(details.get(DiskTO.MANAGED));
                }

                if (managed) {
                    String datastoreName = VmwareResource.getDatastoreName(details.get(DiskTO.IQN));

                    rootDiskDataStoreDetails = dataStoresDetails.get(datastoreName);
                } else {
                    DataStoreTO primaryStore = vol.getData().getDataStore();

                    rootDiskDataStoreDetails = dataStoresDetails.get(primaryStore.getUuid());
                }
            }
        }
        return rootDiskDataStoreDetails;
    }

    /**
     * Since VM start failed, if there was an existing VM in a different cluster that was unregistered, register it back.
     *
     * @param dcMo is guaranteed to be not null since we have noticed there is an existing VM in the dc (using that mo)
     */
    private void reRegisterExistingVm(VirtualMachineData existingVm, DatacenterMO dcMo) {
        if (existingVm != null && existingVm.vmName != null && existingVm.vmFileInfo != null) {
            LOGGER.debug("Since VM start failed, registering back an existing VM: " + existingVm.vmName + " that was unregistered");
            try {
                DatastoreFile fileInDatastore = new DatastoreFile(existingVm.vmFileInfo.getVmPathName());
                DatastoreMO existingVmDsMo = new DatastoreMO(dcMo.getContext(), dcMo.findDatastore(fileInDatastore.getDatastoreName()));
                registerVm(existingVm.vmName, existingVmDsMo, vmwareResource);
            } catch (Exception ex) {
                String message = "Failed to register an existing VM: " + existingVm.vmName + " due to " + VmwareHelper.getExceptionMessage(ex);
                LOGGER.warn(message, ex);
            }
        }
    }

    private void checkIfVmExistsInVcenter(String vmInternalCSName, String vmNameOnVcenter, DatacenterMO dcMo) throws VmAlreadyExistsInVcenter, Exception {
        // Validate VM name is unique in Datacenter
        VirtualMachineMO vmInVcenter = dcMo.checkIfVmAlreadyExistsInVcenter(vmNameOnVcenter, vmInternalCSName);
        if (vmInVcenter != null) {
            String msg = "VM with name: " + vmNameOnVcenter + " already exists in vCenter.";
            LOGGER.error(msg);
            throw new VmAlreadyExistsInVcenter(msg);
        }
    }

    private Pair<String, String> getDiskControllerInfo(VirtualMachineTO vmSpec) {
        String dataDiskController = vmSpec.getDetails().get(VmDetailConstants.DATA_DISK_CONTROLLER);
        String rootDiskController = vmSpec.getDetails().get(VmDetailConstants.ROOT_DISK_CONTROLLER);
        // If root disk controller is scsi, then data disk controller would also be scsi instead of using 'osdefault'
        // This helps avoid mix of different scsi subtype controllers in instance.
        if (DiskControllerType.osdefault == DiskControllerType.getType(dataDiskController) && DiskControllerType.lsilogic == DiskControllerType.getType(rootDiskController)) {
            dataDiskController = DiskControllerType.scsi.toString();
        }

        // Validate the controller types
        dataDiskController = DiskControllerType.getType(dataDiskController).toString();
        rootDiskController = DiskControllerType.getType(rootDiskController).toString();

        if (DiskControllerType.getType(rootDiskController) == DiskControllerType.none) {
            throw new CloudRuntimeException("Invalid root disk controller detected : " + rootDiskController);
        }
        if (DiskControllerType.getType(dataDiskController) == DiskControllerType.none) {
            throw new CloudRuntimeException("Invalid data disk controller detected : " + dataDiskController);
        }

        return new Pair<>(rootDiskController, dataDiskController);
    }

    /**
     * Registers the vm to the inventory given the vmx file.
     * @param vmName
     * @param dsMo
     * @param vmwareResource
     */
    private void registerVm(String vmName, DatastoreMO dsMo, VmwareResource vmwareResource) throws Exception {

        //1st param
        VmwareHypervisorHost hyperHost = vmwareResource.getHyperHost(vmwareResource.getServiceContext());
        ManagedObjectReference dcMor = hyperHost.getHyperHostDatacenter();
        DatacenterMO dataCenterMo = new DatacenterMO(vmwareResource.getServiceContext(), dcMor);
        ManagedObjectReference vmFolderMor = dataCenterMo.getVmFolder();

        //2nd param
        String vmxFilePath = dsMo.searchFileInSubFolders(vmName + ".vmx", false, VmwareManager.s_vmwareSearchExcludeFolder.value());

        // 5th param
        ManagedObjectReference morPool = hyperHost.getHyperHostOwnerResourcePool();

        ManagedObjectReference morTask = vmwareResource.getServiceContext().getService().registerVMTask(vmFolderMor, vmxFilePath, vmName, false, morPool, hyperHost.getMor());
        boolean result = vmwareResource.getServiceContext().getVimClient().waitForTask(morTask);
        if (!result) {
            throw new Exception("Unable to register vm due to " + TaskMO.getTaskFailureInfo(vmwareResource.getServiceContext(), morTask));
        } else {
            vmwareResource.getServiceContext().waitForTaskProgressDone(morTask);
        }

    }

    // Pair<internal CS name, vCenter display name>
    private Pair<String, String> composeVmNames(VirtualMachineTO vmSpec) {
        String vmInternalCSName = vmSpec.getName();
        String vmNameOnVcenter = vmSpec.getName();
        if (VmwareResource.instanceNameFlag && vmSpec.getHostName() != null) {
            vmNameOnVcenter = vmSpec.getHostName();
        }
        return new Pair<String, String>(vmInternalCSName, vmNameOnVcenter);
    }

    private VirtualMachineGuestOsIdentifier translateGuestOsIdentifier(String cpuArchitecture, String guestOs, String cloudGuestOs) {
        if (cpuArchitecture == null) {
            LOGGER.warn("CPU arch is not set, default to i386. guest os: " + guestOs);
            cpuArchitecture = "i386";
        }

        if (cloudGuestOs == null) {
            LOGGER.warn("Guest OS mapping name is not set for guest os: " + guestOs);
        }

        VirtualMachineGuestOsIdentifier identifier = null;
        try {
            if (cloudGuestOs != null) {
                identifier = VirtualMachineGuestOsIdentifier.fromValue(cloudGuestOs);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Using mapping name : " + identifier.toString());
                }
            }
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Unable to find Guest OS Identifier in VMware for mapping name: " + cloudGuestOs + ". Continuing with defaults.");
        }
        if (identifier != null) {
            return identifier;
        }

        if (cpuArchitecture.equalsIgnoreCase("x86_64")) {
            return VirtualMachineGuestOsIdentifier.OTHER_GUEST_64;
        }
        return VirtualMachineGuestOsIdentifier.OTHER_GUEST;
    }
    private DiskTO[] validateDisks(DiskTO[] disks) {
        List<DiskTO> validatedDisks = new ArrayList<DiskTO>();

        for (DiskTO vol : disks) {
            if (vol.getType() != Volume.Type.ISO) {
                VolumeObjectTO volumeTO = (VolumeObjectTO) vol.getData();
                DataStoreTO primaryStore = volumeTO.getDataStore();
                if (primaryStore.getUuid() != null && !primaryStore.getUuid().isEmpty()) {
                    validatedDisks.add(vol);
                }
            } else if (vol.getType() == Volume.Type.ISO) {
                TemplateObjectTO templateTO = (TemplateObjectTO) vol.getData();
                if (templateTO.getPath() != null && !templateTO.getPath().isEmpty()) {
                    validatedDisks.add(vol);
                }
            } else {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Drop invalid disk option, volumeTO: " + vmwareResource.getGson().toJson(vol));
                }
            }
        }
        Collections.sort(validatedDisks, (d1, d2) -> d1.getDiskSeq().compareTo(d2.getDiskSeq()));
        return validatedDisks.toArray(new DiskTO[0]);
    }
    private HashMap<String, Pair<ManagedObjectReference, DatastoreMO>> inferDatastoreDetailsFromDiskInfo(VmwareHypervisorHost hyperHost, VmwareContext context,
            DiskTO[] disks, Command cmd) throws Exception {
        HashMap<String, Pair<ManagedObjectReference, DatastoreMO>> mapIdToMors = new HashMap<>();

        assert (hyperHost != null) && (context != null);

        for (DiskTO vol : disks) {
            if (vol.getType() != Volume.Type.ISO) {
                VolumeObjectTO volumeTO = (VolumeObjectTO) vol.getData();
                DataStoreTO primaryStore = volumeTO.getDataStore();
                String poolUuid = primaryStore.getUuid();

                if (mapIdToMors.get(poolUuid) == null) {
                    boolean isManaged = false;
                    Map<String, String> details = vol.getDetails();

                    if (details != null) {
                        isManaged = Boolean.parseBoolean(details.get(DiskTO.MANAGED));
                    }

                    if (isManaged) {
                        String iScsiName = details.get(DiskTO.IQN); // details should not be null for managed storage (it may or may not be null for non-managed storage)
                        String datastoreName = VmwareResource.getDatastoreName(iScsiName);
                        ManagedObjectReference morDatastore = HypervisorHostHelper.findDatastoreWithBackwardsCompatibility(hyperHost, datastoreName);

                        // if the datastore is not present, we need to discover the iSCSI device that will support it,
                        // create the datastore, and create a VMDK file in the datastore
                        if (morDatastore == null) {
                            final String vmdkPath = vmwareResource.getVmdkPath(volumeTO.getPath());

                            morDatastore = getStorageProcessor().prepareManagedStorage(context, hyperHost, null, iScsiName,
                                    details.get(DiskTO.STORAGE_HOST), Integer.parseInt(details.get(DiskTO.STORAGE_PORT)),
                                    vmdkPath,
                                    details.get(DiskTO.CHAP_INITIATOR_USERNAME), details.get(DiskTO.CHAP_INITIATOR_SECRET),
                                    details.get(DiskTO.CHAP_TARGET_USERNAME), details.get(DiskTO.CHAP_TARGET_SECRET),
                                    Long.parseLong(details.get(DiskTO.VOLUME_SIZE)), cmd);

                            DatastoreMO dsMo = new DatastoreMO(vmwareResource.getServiceContext(), morDatastore);

                            final String datastoreVolumePath;

                            if (vmdkPath != null) {
                                datastoreVolumePath = dsMo.getDatastorePath(vmdkPath + VmwareResource.VMDK_EXTENSION);
                            } else {
                                datastoreVolumePath = dsMo.getDatastorePath(dsMo.getName() + VmwareResource.VMDK_EXTENSION);
                            }

                            volumeTO.setPath(datastoreVolumePath);
                            vol.setPath(datastoreVolumePath);
                        }

                        mapIdToMors.put(datastoreName, new Pair<>(morDatastore, new DatastoreMO(context, morDatastore)));
                    } else {
                        ManagedObjectReference morDatastore = HypervisorHostHelper.findDatastoreWithBackwardsCompatibility(hyperHost, poolUuid);

                        if (morDatastore == null) {
                            String msg = "Failed to get the mounted datastore for the volume's pool " + poolUuid;

                            LOGGER.error(msg);

                            throw new Exception(msg);
                        }

                        mapIdToMors.put(poolUuid, new Pair<>(morDatastore, new DatastoreMO(context, morDatastore)));
                    }
                }
            }
        }

        return mapIdToMors;
    }

    private VmwareStorageProcessor getStorageProcessor() {
        return vmwareResource.getStorageProcessor();
    }

    private DatastoreMO getDatastoreThatRootDiskIsOn(HashMap<String, Pair<ManagedObjectReference, DatastoreMO>> dataStoresDetails, DiskTO disks[]) {
        Pair<ManagedObjectReference, DatastoreMO> rootDiskDataStoreDetails = null;

        for (DiskTO vol : disks) {
            if (vol.getType() == Volume.Type.ROOT) {
                Map<String, String> details = vol.getDetails();
                boolean managed = false;

                if (details != null) {
                    managed = Boolean.parseBoolean(details.get(DiskTO.MANAGED));
                }

                if (managed) {
                    String datastoreName = VmwareResource.getDatastoreName(details.get(DiskTO.IQN));

                    rootDiskDataStoreDetails = dataStoresDetails.get(datastoreName);

                    break;
                } else {
                    DataStoreTO primaryStore = vol.getData().getDataStore();

                    rootDiskDataStoreDetails = dataStoresDetails.get(primaryStore.getUuid());

                    break;
                }
            }
        }

        if (rootDiskDataStoreDetails != null) {
            return rootDiskDataStoreDetails.second();
        }

        return null;
    }

    private void ensureDiskControllers(VirtualMachineMO vmMo, Pair<String, String> controllerInfo) throws Exception {
        if (vmMo == null) {
            return;
        }

        String msg;
        String rootDiskController = controllerInfo.first();
        String dataDiskController = controllerInfo.second();
        String scsiDiskController;
        String recommendedDiskController = null;

        if (VmwareHelper.isControllerOsRecommended(dataDiskController) || VmwareHelper.isControllerOsRecommended(rootDiskController)) {
            recommendedDiskController = vmMo.getRecommendedDiskController(null);
        }
        scsiDiskController = HypervisorHostHelper.getScsiController(new Pair<String, String>(rootDiskController, dataDiskController), recommendedDiskController);
        if (scsiDiskController == null) {
            return;
        }

        vmMo.getScsiDeviceControllerKeyNoException();
        // This VM needs SCSI controllers.
        // Get count of existing scsi controllers. Helps not to attempt to create more than the maximum allowed 4
        // Get maximum among the bus numbers in use by scsi controllers. Safe to pick maximum, because we always go sequential allocating bus numbers.
        Ternary<Integer, Integer, DiskControllerType> scsiControllerInfo = vmMo.getScsiControllerInfo();
        int requiredNumScsiControllers = VmwareHelper.MAX_SCSI_CONTROLLER_COUNT - scsiControllerInfo.first();
        int availableBusNum = scsiControllerInfo.second() + 1; // method returned current max. bus number

        if (requiredNumScsiControllers == 0) {
            return;
        }
        if (scsiControllerInfo.first() > 0) {
            // For VMs which already have a SCSI controller, do NOT attempt to add any more SCSI controllers & return the sub type.
            // For Legacy VMs would have only 1 LsiLogic Parallel SCSI controller, and doesn't require more.
            // For VMs created post device ordering support, 4 SCSI subtype controllers are ensured during deployment itself. No need to add more.
            // For fresh VM deployment only, all required controllers should be ensured.
            return;
        }
        ensureScsiDiskControllers(vmMo, scsiDiskController, requiredNumScsiControllers, availableBusNum);
    }

    private void ensureScsiDiskControllers(VirtualMachineMO vmMo, String scsiDiskController, int requiredNumScsiControllers, int availableBusNum) throws Exception {
        // Pick the sub type of scsi
        if (DiskControllerType.getType(scsiDiskController) == DiskControllerType.pvscsi) {
            if (!vmMo.isPvScsiSupported()) {
                String msg = "This VM doesn't support Vmware Paravirtual SCSI controller for virtual disks, because the virtual hardware version is less than 7.";
                throw new Exception(msg);
            }
            vmMo.ensurePvScsiDeviceController(requiredNumScsiControllers, availableBusNum);
        } else if (DiskControllerType.getType(scsiDiskController) == DiskControllerType.lsisas1068) {
            vmMo.ensureLsiLogicSasDeviceControllers(requiredNumScsiControllers, availableBusNum);
        } else if (DiskControllerType.getType(scsiDiskController) == DiskControllerType.buslogic) {
            vmMo.ensureBusLogicDeviceControllers(requiredNumScsiControllers, availableBusNum);
        } else if (DiskControllerType.getType(scsiDiskController) == DiskControllerType.lsilogic) {
            vmMo.ensureLsiLogicDeviceControllers(requiredNumScsiControllers, availableBusNum);
        }
    }

    private VirtualMachineMO takeVmFromOtherHyperHost(VmwareHypervisorHost hyperHost, String vmName) throws Exception {

        VirtualMachineMO vmMo = hyperHost.findVmOnPeerHyperHost(vmName);
        if (vmMo != null) {
            ManagedObjectReference morTargetPhysicalHost = hyperHost.findMigrationTarget(vmMo);
            if (morTargetPhysicalHost == null) {
                String msg = "VM " + vmName + " is on other host and we have no resource available to migrate and start it here";
                LOGGER.error(msg);
                throw new Exception(msg);
            }

            if (!vmMo.relocate(morTargetPhysicalHost)) {
                String msg = "VM " + vmName + " is on other host and we failed to relocate it here";
                LOGGER.error(msg);
                throw new Exception(msg);
            }

            return vmMo;
        }
        return null;
    }

    private void tearDownVm(VirtualMachineMO vmMo) throws Exception {

        if (vmMo == null)
            return;

        boolean hasSnapshot = false;
        hasSnapshot = vmMo.hasSnapshot();
        if (!hasSnapshot)
            vmMo.tearDownDevices(new Class<?>[]{VirtualDisk.class, VirtualEthernetCard.class});
        else
            vmMo.tearDownDevices(new Class<?>[]{VirtualEthernetCard.class});
        vmMo.ensureScsiDeviceController();
    }

    private static DiskTO getIsoDiskTO(DiskTO[] disks) {
        for (DiskTO vol : disks) {
            if (vol.getType() == Volume.Type.ISO) {
                return vol;
            }
        }
        return null;
    }

    // isoUrl sample content :
    // nfs://192.168.10.231/export/home/kelven/vmware-test/secondary/template/tmpl/2/200//200-2-80f7ee58-6eff-3a2d-bcb0-59663edf6d26.iso
    private Pair<String, ManagedObjectReference> getIsoDatastoreInfo(VmwareHypervisorHost hyperHost, String isoUrl) throws Exception {

        assert (isoUrl != null);
        int isoFileNameStartPos = isoUrl.lastIndexOf("/");
        if (isoFileNameStartPos < 0) {
            throw new Exception("Invalid ISO path info");
        }

        String isoFileName = isoUrl.substring(isoFileNameStartPos);

        int templateRootPos = isoUrl.indexOf("template/tmpl");
        templateRootPos = (templateRootPos < 0 ? isoUrl.indexOf(ConfigDrive.CONFIGDRIVEDIR) : templateRootPos);
        if (templateRootPos < 0) {
            throw new Exception("Invalid ISO path info");
        }

        String storeUrl = isoUrl.substring(0, templateRootPos - 1);
        String isoPath = isoUrl.substring(templateRootPos, isoFileNameStartPos);

        ManagedObjectReference morDs = vmwareResource.prepareSecondaryDatastoreOnHost(storeUrl);
        DatastoreMO dsMo = new DatastoreMO(vmwareResource.getServiceContext(), morDs);

        return new Pair<String, ManagedObjectReference>(String.format("[%s] %s%s", dsMo.getName(), isoPath, isoFileName), morDs);
    }

    private static DiskTO[] sortVolumesByDeviceId(DiskTO[] volumes) {

        List<DiskTO> listForSort = new ArrayList<DiskTO>();
        for (DiskTO vol : volumes) {
            listForSort.add(vol);
        }
        Collections.sort(listForSort, new Comparator<DiskTO>() {

            @Override
            public int compare(DiskTO arg0, DiskTO arg1) {
                if (arg0.getDiskSeq() < arg1.getDiskSeq()) {
                    return -1;
                } else if (arg0.getDiskSeq().equals(arg1.getDiskSeq())) {
                    return 0;
                }

                return 1;
            }
        });

        return listForSort.toArray(new DiskTO[0]);
    }

    private int getDiskController(VirtualMachineDiskInfo matchingExistingDisk, DiskTO vol, VirtualMachineTO vmSpec, int ideControllerKey, int scsiControllerKey) {

        int controllerKey;
        if (matchingExistingDisk != null) {
            LOGGER.info("Chose disk controller based on existing information: " + matchingExistingDisk.getDiskDeviceBusName());
            if (matchingExistingDisk.getDiskDeviceBusName().startsWith("ide"))
                return ideControllerKey;
            else
                return scsiControllerKey;
        }

        if (vol.getType() == Volume.Type.ROOT) {
            Map<String, String> vmDetails = vmSpec.getDetails();
            if (vmDetails != null && vmDetails.get(VmDetailConstants.ROOT_DISK_CONTROLLER) != null) {
                if (vmDetails.get(VmDetailConstants.ROOT_DISK_CONTROLLER).equalsIgnoreCase("scsi")) {
                    LOGGER.info("Chose disk controller for vol " + vol.getType() + " -> scsi, based on root disk controller settings: "
                            + vmDetails.get(VmDetailConstants.ROOT_DISK_CONTROLLER));
                    controllerKey = scsiControllerKey;
                } else {
                    LOGGER.info("Chose disk controller for vol " + vol.getType() + " -> ide, based on root disk controller settings: "
                            + vmDetails.get(VmDetailConstants.ROOT_DISK_CONTROLLER));
                    controllerKey = ideControllerKey;
                }
            } else {
                LOGGER.info("Chose disk controller for vol " + vol.getType() + " -> scsi. due to null root disk controller setting");
                controllerKey = scsiControllerKey;
            }

        } else {
            // DATA volume always use SCSI device
            LOGGER.info("Chose disk controller for vol " + vol.getType() + " -> scsi");
            controllerKey = scsiControllerKey;
        }

        return controllerKey;
    }

    private String getDiskController(VirtualMachineMO vmMo, VirtualMachineDiskInfo matchingExistingDisk, DiskTO vol, Pair<String, String> controllerInfo) throws Exception {
        int controllerKey;
        DiskControllerType controllerType = DiskControllerType.none;
        if (matchingExistingDisk != null) {
            String currentBusName = matchingExistingDisk.getDiskDeviceBusName();
            if (currentBusName != null) {
                LOGGER.info("Chose disk controller based on existing information: " + currentBusName);
                if (currentBusName.startsWith("ide")) {
                    controllerType = DiskControllerType.ide;
                } else if (currentBusName.startsWith("scsi")) {
                    controllerType = DiskControllerType.scsi;
                }
            }
            if (controllerType == DiskControllerType.scsi || controllerType == DiskControllerType.none) {
                Ternary<Integer, Integer, DiskControllerType> vmScsiControllerInfo = vmMo.getScsiControllerInfo();
                controllerType = vmScsiControllerInfo.third();
            }
            return controllerType.toString();
        }

        if (vol.getType() == Volume.Type.ROOT) {
            LOGGER.info("Chose disk controller for vol " + vol.getType() + " -> " + controllerInfo.first()
                    + ", based on root disk controller settings at global configuration setting.");
            return controllerInfo.first();
        } else {
            LOGGER.info("Chose disk controller for vol " + vol.getType() + " -> " + controllerInfo.second()
                    + ", based on default data disk controller setting i.e. Operating system recommended."); // Need to bring in global configuration setting & template level setting.
            return controllerInfo.second();
        }
    }

    private void postDiskConfigBeforeStart(VirtualMachineMO vmMo, VirtualMachineTO vmSpec, DiskTO[] sortedDisks, int ideControllerKey,
            int scsiControllerKey, Map<String, Map<String, String>> iqnToData, VmwareHypervisorHost hyperHost, VmwareContext context) throws Exception {
        VirtualMachineDiskInfoBuilder diskInfoBuilder = vmMo.getDiskInfoBuilder();

        for (DiskTO vol : sortedDisks) {
            if (vol.getType() == Volume.Type.ISO)
                continue;

            VolumeObjectTO volumeTO = (VolumeObjectTO) vol.getData();

            VirtualMachineDiskInfo diskInfo = getMatchingExistingDisk(diskInfoBuilder, vol, hyperHost, context);
            assert (diskInfo != null);

            String[] diskChain = diskInfo.getDiskChain();
            assert (diskChain.length > 0);

            Map<String, String> details = vol.getDetails();
            boolean managed = false;

            if (details != null) {
                managed = Boolean.parseBoolean(details.get(DiskTO.MANAGED));
            }

            DatastoreFile file = new DatastoreFile(diskChain[0]);

            if (managed) {
                DatastoreFile originalFile = new DatastoreFile(volumeTO.getPath());

                if (!file.getFileBaseName().equalsIgnoreCase(originalFile.getFileBaseName())) {
                    if (LOGGER.isInfoEnabled())
                        LOGGER.info("Detected disk-chain top file change on volume: " + volumeTO.getId() + " " + volumeTO.getPath() + " -> " + diskChain[0]);
                }
            } else {
                if (!file.getFileBaseName().equalsIgnoreCase(volumeTO.getPath())) {
                    if (LOGGER.isInfoEnabled())
                        LOGGER.info("Detected disk-chain top file change on volume: " + volumeTO.getId() + " " + volumeTO.getPath() + " -> " + file.getFileBaseName());
                }
            }

            VolumeObjectTO volInSpec = getVolumeInSpec(vmSpec, volumeTO);

            if (volInSpec != null) {
                if (managed) {
                    Map<String, String> data = new HashMap<>();

                    String datastoreVolumePath = diskChain[0];

                    data.put(StartAnswer.PATH, datastoreVolumePath);
                    data.put(StartAnswer.IMAGE_FORMAT, Storage.ImageFormat.OVA.toString());

                    iqnToData.put(details.get(DiskTO.IQN), data);

                    vol.setPath(datastoreVolumePath);
                    volumeTO.setPath(datastoreVolumePath);
                    volInSpec.setPath(datastoreVolumePath);
                } else {
                    volInSpec.setPath(file.getFileBaseName());
                }
                volInSpec.setChainInfo(vmwareResource.getGson().toJson(diskInfo));
            }
        }
    }

    private void checkBootOptions(VirtualMachineTO vmSpec, VirtualMachineConfigSpec vmConfigSpec) {
        String bootMode = null;
        if (vmSpec.getDetails().containsKey(VmDetailConstants.BOOT_MODE)) {
            bootMode = vmSpec.getDetails().get(VmDetailConstants.BOOT_MODE);
        }
        if (null == bootMode) {
            bootMode = ApiConstants.BootType.BIOS.toString();
        }

        setBootOptions(vmSpec, bootMode, vmConfigSpec);
    }

    private void setBootOptions(VirtualMachineTO vmSpec, String bootMode, VirtualMachineConfigSpec vmConfigSpec) {
        VirtualMachineBootOptions bootOptions = null;
        if (StringUtils.isNotBlank(bootMode) && !bootMode.equalsIgnoreCase("bios")) {
            vmConfigSpec.setFirmware("efi");
            if (vmSpec.getDetails().containsKey(ApiConstants.BootType.UEFI.toString()) && "secure".equalsIgnoreCase(vmSpec.getDetails().get(ApiConstants.BootType.UEFI.toString()))) {
                if (bootOptions == null) {
                    bootOptions = new VirtualMachineBootOptions();
                }
                bootOptions.setEfiSecureBootEnabled(true);
            }
        }
        if (vmSpec.isEnterHardwareSetup()) {
            if (bootOptions == null) {
                bootOptions = new VirtualMachineBootOptions();
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("configuring VM '%s' to enter hardware setup",vmSpec.getName()));
            }
            bootOptions.setEnterBIOSSetup(vmSpec.isEnterHardwareSetup());
        }
        if (bootOptions != null) {
            vmConfigSpec.setBootOptions(bootOptions);
        }
    }

    // return the finalized disk chain for startup, from top to bottom
    private String[] syncDiskChain(DatacenterMO dcMo, VirtualMachineMO vmMo, VirtualMachineTO vmSpec, DiskTO vol, VirtualMachineDiskInfo diskInfo,
            HashMap<String, Pair<ManagedObjectReference, DatastoreMO>> dataStoresDetails) throws Exception {

        VolumeObjectTO volumeTO = (VolumeObjectTO) vol.getData();
        DataStoreTO primaryStore = volumeTO.getDataStore();
        Map<String, String> details = vol.getDetails();
        boolean isManaged = false;
        String iScsiName = null;

        if (details != null) {
            isManaged = Boolean.parseBoolean(details.get(DiskTO.MANAGED));
            iScsiName = details.get(DiskTO.IQN);
        }

        // if the storage is managed, iScsiName should not be null
        String datastoreName = isManaged ? VmwareResource.getDatastoreName(iScsiName) : primaryStore.getUuid();
        Pair<ManagedObjectReference, DatastoreMO> volumeDsDetails = dataStoresDetails.get(datastoreName);

        if (volumeDsDetails == null) {
            throw new Exception("Primary datastore " + primaryStore.getUuid() + " is not mounted on host.");
        }

        DatastoreMO dsMo = volumeDsDetails.second();

        // we will honor vCenter's meta if it exists
        if (diskInfo != null) {
            // to deal with run-time upgrade to maintain the new datastore folder structure
            String disks[] = diskInfo.getDiskChain();
            for (int i = 0; i < disks.length; i++) {
                DatastoreFile file = new DatastoreFile(disks[i]);
                if (!isManaged && file.getDir() != null && file.getDir().isEmpty()) {
                    LOGGER.info("Perform run-time datastore folder upgrade. sync " + disks[i] + " to VM folder");
                    disks[i] = VmwareStorageLayoutHelper.syncVolumeToVmDefaultFolder(dcMo, vmMo.getName(), dsMo, file.getFileBaseName(), VmwareManager.s_vmwareSearchExcludeFolder.value());
                }
            }
            return disks;
        }

        final String datastoreDiskPath;

        if (isManaged) {
            String vmdkPath = new DatastoreFile(volumeTO.getPath()).getFileBaseName();

            if (volumeTO.getVolumeType() == Volume.Type.ROOT) {
                if (vmdkPath == null) {
                    vmdkPath = volumeTO.getName();
                }

                datastoreDiskPath = VmwareStorageLayoutHelper.syncVolumeToVmDefaultFolder(dcMo, vmMo.getName(), dsMo, vmdkPath);
            } else {
                if (vmdkPath == null) {
                    vmdkPath = dsMo.getName();
                }

                datastoreDiskPath = dsMo.getDatastorePath(vmdkPath + VmwareResource.VMDK_EXTENSION);
            }
        } else {
            datastoreDiskPath = VmwareStorageLayoutHelper.syncVolumeToVmDefaultFolder(dcMo, vmMo.getName(), dsMo, volumeTO.getPath(), VmwareManager.s_vmwareSearchExcludeFolder.value());
        }

        if (!dsMo.fileExists(datastoreDiskPath)) {
            LOGGER.warn("Volume " + volumeTO.getId() + " does not seem to exist on datastore, out of sync? path: " + datastoreDiskPath);
        }

        return new String[]{datastoreDiskPath};
    }

    private void resizeRootDiskOnVMStart(VirtualMachineMO vmMo, DiskTO rootDiskTO, VmwareHypervisorHost hyperHost, VmwareContext context) throws Exception {
        final Pair<VirtualDisk, String> vdisk = vmwareResource.getVirtualDiskInfo(vmMo, VmwareResource.appendFileType(rootDiskTO.getPath(), VmwareResource.VMDK_EXTENSION));
        assert (vdisk != null);

        Long reqSize = 0L;
        final VolumeObjectTO volumeTO = ((VolumeObjectTO) rootDiskTO.getData());
        if (volumeTO != null) {
            reqSize = volumeTO.getSize() / 1024;
        }
        final VirtualDisk disk = vdisk.first();
        if (reqSize > disk.getCapacityInKB()) {
            final VirtualMachineDiskInfo diskInfo = getMatchingExistingDisk(vmMo.getDiskInfoBuilder(), rootDiskTO, hyperHost, context);
            assert (diskInfo != null);
            final String[] diskChain = diskInfo.getDiskChain();

            if (diskChain != null && diskChain.length > 1) {
                LOGGER.warn("Disk chain length for the VM is greater than one, this is not supported");
                throw new CloudRuntimeException("Unsupported VM disk chain length: " + diskChain.length);
            }

            boolean resizingSupported = false;
            String deviceBusName = diskInfo.getDiskDeviceBusName();
            if (deviceBusName != null && (deviceBusName.toLowerCase().contains("scsi") || deviceBusName.toLowerCase().contains("lsi"))) {
                resizingSupported = true;
            }
            if (!resizingSupported) {
                LOGGER.warn("Resizing of root disk is only support for scsi device/bus, the provide VM's disk device bus name is " + diskInfo.getDiskDeviceBusName());
                throw new CloudRuntimeException("Unsupported VM root disk device bus: " + diskInfo.getDiskDeviceBusName());
            }

            disk.setCapacityInKB(reqSize);
            VirtualMachineConfigSpec vmConfigSpec = new VirtualMachineConfigSpec();
            VirtualDeviceConfigSpec deviceConfigSpec = new VirtualDeviceConfigSpec();
            deviceConfigSpec.setDevice(disk);
            deviceConfigSpec.setOperation(VirtualDeviceConfigSpecOperation.EDIT);
            vmConfigSpec.getDeviceChange().add(deviceConfigSpec);
            if (!vmMo.configureVm(vmConfigSpec)) {
                throw new Exception("Failed to configure VM for given root disk size. vmName: " + vmMo.getName());
            }
        }
    }

    private static void postNvpConfigBeforeStart(VirtualMachineMO vmMo, VirtualMachineTO vmSpec) throws Exception {
        /**
         * We need to configure the port on the DV switch after the host is
         * connected. So make this happen between the configure and start of
         * the VM
         */
        int nicIndex = 0;
        for (NicTO nicTo : sortNicsByDeviceId(vmSpec.getNics())) {
            if (nicTo.getBroadcastType() == Networks.BroadcastDomainType.Lswitch) {
                // We need to create a port with a unique vlan and pass the key to the nic device
                LOGGER.trace("Nic " + nicTo.toString() + " is connected to an NVP logicalswitch");
                VirtualDevice nicVirtualDevice = vmMo.getNicDeviceByIndex(nicIndex);
                if (nicVirtualDevice == null) {
                    throw new Exception("Failed to find a VirtualDevice for nic " + nicIndex); //FIXME Generic exceptions are bad
                }
                VirtualDeviceBackingInfo backing = nicVirtualDevice.getBacking();
                if (backing instanceof VirtualEthernetCardDistributedVirtualPortBackingInfo) {
                    // This NIC is connected to a Distributed Virtual Switch
                    VirtualEthernetCardDistributedVirtualPortBackingInfo portInfo = (VirtualEthernetCardDistributedVirtualPortBackingInfo) backing;
                    DistributedVirtualSwitchPortConnection port = portInfo.getPort();
                    String portKey = port.getPortKey();
                    String portGroupKey = port.getPortgroupKey();
                    String dvSwitchUuid = port.getSwitchUuid();

                    LOGGER.debug("NIC " + nicTo.toString() + " is connected to dvSwitch " + dvSwitchUuid + " pg " + portGroupKey + " port " + portKey);

                    ManagedObjectReference dvSwitchManager = vmMo.getContext().getVimClient().getServiceContent().getDvSwitchManager();
                    ManagedObjectReference dvSwitch = vmMo.getContext().getVimClient().getService().queryDvsByUuid(dvSwitchManager, dvSwitchUuid);

                    // Get all ports
                    DistributedVirtualSwitchPortCriteria criteria = new DistributedVirtualSwitchPortCriteria();
                    criteria.setInside(true);
                    criteria.getPortgroupKey().add(portGroupKey);
                    List<DistributedVirtualPort> dvPorts = vmMo.getContext().getVimClient().getService().fetchDVPorts(dvSwitch, criteria);

                    DistributedVirtualPort vmDvPort = null;
                    List<Integer> usedVlans = new ArrayList<Integer>();
                    for (DistributedVirtualPort dvPort : dvPorts) {
                        // Find the port for this NIC by portkey
                        if (portKey.equals(dvPort.getKey())) {
                            vmDvPort = dvPort;
                        }
                        VMwareDVSPortSetting settings = (VMwareDVSPortSetting) dvPort.getConfig().getSetting();
                        VmwareDistributedVirtualSwitchVlanIdSpec vlanId = (VmwareDistributedVirtualSwitchVlanIdSpec) settings.getVlan();
                        LOGGER.trace("Found port " + dvPort.getKey() + " with vlan " + vlanId.getVlanId());
                        if (vlanId.getVlanId() > 0 && vlanId.getVlanId() < 4095) {
                            usedVlans.add(vlanId.getVlanId());
                        }
                    }

                    if (vmDvPort == null) {
                        throw new Exception("Empty port list from dvSwitch for nic " + nicTo.toString());
                    }

                    DVPortConfigInfo dvPortConfigInfo = vmDvPort.getConfig();
                    VMwareDVSPortSetting settings = (VMwareDVSPortSetting) dvPortConfigInfo.getSetting();

                    VmwareDistributedVirtualSwitchVlanIdSpec vlanId = (VmwareDistributedVirtualSwitchVlanIdSpec) settings.getVlan();
                    BoolPolicy blocked = settings.getBlocked();
                    if (blocked.isValue() == Boolean.TRUE) {
                        LOGGER.trace("Port is blocked, set a vlanid and unblock");
                        DVPortConfigSpec dvPortConfigSpec = new DVPortConfigSpec();
                        VMwareDVSPortSetting edittedSettings = new VMwareDVSPortSetting();
                        // Unblock
                        blocked.setValue(Boolean.FALSE);
                        blocked.setInherited(Boolean.FALSE);
                        edittedSettings.setBlocked(blocked);
                        // Set vlan
                        int i;
                        for (i = 1; i < 4095; i++) {
                            if (!usedVlans.contains(i))
                                break;
                        }
                        vlanId.setVlanId(i); // FIXME should be a determined
                        // based on usage
                        vlanId.setInherited(false);
                        edittedSettings.setVlan(vlanId);

                        dvPortConfigSpec.setSetting(edittedSettings);
                        dvPortConfigSpec.setOperation("edit");
                        dvPortConfigSpec.setKey(portKey);
                        List<DVPortConfigSpec> dvPortConfigSpecs = new ArrayList<DVPortConfigSpec>();
                        dvPortConfigSpecs.add(dvPortConfigSpec);
                        ManagedObjectReference task = vmMo.getContext().getVimClient().getService().reconfigureDVPortTask(dvSwitch, dvPortConfigSpecs);
                        if (!vmMo.getContext().getVimClient().waitForTask(task)) {
                            throw new Exception("Failed to configure the dvSwitch port for nic " + nicTo.toString());
                        }
                        LOGGER.debug("NIC " + nicTo.toString() + " connected to vlan " + i);
                    } else {
                        LOGGER.trace("Port already configured and set to vlan " + vlanId.getVlanId());
                    }
                } else if (backing instanceof VirtualEthernetCardNetworkBackingInfo) {
                    // This NIC is connected to a Virtual Switch
                    // Nothing to do
                } else if (backing instanceof VirtualEthernetCardOpaqueNetworkBackingInfo) {
                    //if NSX API VERSION >= 4.2, connect to br-int (nsx.network), do not create portgroup else previous behaviour
                    //OK, connected to OpaqueNetwork
                } else {
                    LOGGER.error("nic device backing is of type " + backing.getClass().getName());
                    throw new Exception("Incompatible backing for a VirtualDevice for nic " + nicIndex); //FIXME Generic exceptions are bad
                }
            }
            nicIndex++;
        }
    }

    private VirtualMachineDiskInfo getMatchingExistingDisk(VirtualMachineDiskInfoBuilder diskInfoBuilder, DiskTO vol, VmwareHypervisorHost hyperHost, VmwareContext context)
            throws Exception {
        if (diskInfoBuilder != null) {
            VolumeObjectTO volume = (VolumeObjectTO) vol.getData();

            String dsName = null;
            String diskBackingFileBaseName = null;

            Map<String, String> details = vol.getDetails();
            boolean isManaged = details != null && Boolean.parseBoolean(details.get(DiskTO.MANAGED));

            if (isManaged) {
                String iScsiName = details.get(DiskTO.IQN);

                // if the storage is managed, iScsiName should not be null
                dsName = VmwareResource.getDatastoreName(iScsiName);

                diskBackingFileBaseName = new DatastoreFile(volume.getPath()).getFileBaseName();
            } else {
                ManagedObjectReference morDs = HypervisorHostHelper.findDatastoreWithBackwardsCompatibility(hyperHost, volume.getDataStore().getUuid());
                DatastoreMO dsMo = new DatastoreMO(context, morDs);

                dsName = dsMo.getName();

                diskBackingFileBaseName = volume.getPath();
            }

            VirtualMachineDiskInfo diskInfo = diskInfoBuilder.getDiskInfoByBackingFileBaseName(diskBackingFileBaseName, dsName);
            if (diskInfo != null) {
                LOGGER.info("Found existing disk info from volume path: " + volume.getPath());
                return diskInfo;
            } else {
                String chainInfo = volume.getChainInfo();
                if (chainInfo != null) {
                    VirtualMachineDiskInfo infoInChain = vmwareResource.getGson().fromJson(chainInfo, VirtualMachineDiskInfo.class);
                    if (infoInChain != null) {
                        String[] disks = infoInChain.getDiskChain();
                        if (disks.length > 0) {
                            for (String diskPath : disks) {
                                DatastoreFile file = new DatastoreFile(diskPath);
                                diskInfo = diskInfoBuilder.getDiskInfoByBackingFileBaseName(file.getFileBaseName(), dsName);
                                if (diskInfo != null) {
                                    LOGGER.info("Found existing disk from chain info: " + diskPath);
                                    return diskInfo;
                                }
                            }
                        }

                        if (diskInfo == null) {
                            diskInfo = diskInfoBuilder.getDiskInfoByDeviceBusName(infoInChain.getDiskDeviceBusName());
                            if (diskInfo != null) {
                                LOGGER.info("Found existing disk from from chain device bus information: " + infoInChain.getDiskDeviceBusName());
                                return diskInfo;
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    private static VolumeObjectTO getVolumeInSpec(VirtualMachineTO vmSpec, VolumeObjectTO srcVol) {
        for (DiskTO disk : vmSpec.getDisks()) {
            if (disk.getData() instanceof VolumeObjectTO) {
                VolumeObjectTO vol = (VolumeObjectTO) disk.getData();
                if (vol.getId() == srcVol.getId())
                    return vol;
            }
        }

        return null;
    }

    /**
     * Generate the mac sequence from the nics.
     */
    protected String generateMacSequence(NicTO[] nics) {
        if (nics.length == 0) {
            return "";
        }

        StringBuffer sbMacSequence = new StringBuffer();
        for (NicTO nicTo : sortNicsByDeviceId(nics)) {
            sbMacSequence.append(nicTo.getMac()).append("|");
        }
        if (!sbMacSequence.toString().isEmpty()) {
            sbMacSequence.deleteCharAt(sbMacSequence.length() - 1); //Remove extra '|' char appended at the end
        }

        return sbMacSequence.toString();
    }

    static NicTO[] sortNicsByDeviceId(NicTO[] nics) {

        List<NicTO> listForSort = new ArrayList<NicTO>();
        for (NicTO nic : nics) {
            listForSort.add(nic);
        }
        Collections.sort(listForSort, new Comparator<NicTO>() {

            @Override
            public int compare(NicTO arg0, NicTO arg1) {
                if (arg0.getDeviceId() < arg1.getDeviceId()) {
                    return -1;
                } else if (arg0.getDeviceId() == arg1.getDeviceId()) {
                    return 0;
                }

                return 1;
            }
        });

        return listForSort.toArray(new NicTO[0]);
    }

    private static void configBasicExtraOption(List<OptionValue> extraOptions, VirtualMachineTO vmSpec) {
        OptionValue newVal = new OptionValue();
        newVal.setKey("machine.id");
        newVal.setValue(vmSpec.getBootArgs());
        extraOptions.add(newVal);

        newVal = new OptionValue();
        newVal.setKey("devices.hotplug");
        newVal.setValue("true");
        extraOptions.add(newVal);
    }

    private static void configNvpExtraOption(List<OptionValue> extraOptions, VirtualMachineTO vmSpec, Map<String, String> nicUuidToDvSwitchUuid) {
        /**
         * Extra Config : nvp.vm-uuid = uuid
         *  - Required for Nicira NVP integration
         */
        OptionValue newVal = new OptionValue();
        newVal.setKey("nvp.vm-uuid");
        newVal.setValue(vmSpec.getUuid());
        extraOptions.add(newVal);

        /**
         * Extra Config : nvp.iface-id.<num> = uuid
         *  - Required for Nicira NVP integration
         */
        int nicNum = 0;
        for (NicTO nicTo : sortNicsByDeviceId(vmSpec.getNics())) {
            if (nicTo.getUuid() != null) {
                newVal = new OptionValue();
                newVal.setKey("nvp.iface-id." + nicNum);
                newVal.setValue(nicTo.getUuid());
                extraOptions.add(newVal);
            }
            nicNum++;
        }
    }

    private static void configCustomExtraOption(List<OptionValue> extraOptions, VirtualMachineTO vmSpec) {
        // we no longer to validation anymore
        for (Map.Entry<String, String> entry : vmSpec.getDetails().entrySet()) {
            if (entry.getKey().equalsIgnoreCase(VmDetailConstants.BOOT_MODE)) {
                continue;
            }
            OptionValue newVal = new OptionValue();
            newVal.setKey(entry.getKey());
            newVal.setValue(entry.getValue());
            extraOptions.add(newVal);
        }
    }

    private class VmAlreadyExistsInVcenter extends Exception {
        public VmAlreadyExistsInVcenter(String msg) {
        }
    }

    private class VirtualMachineData {
        String vmName = null;
        VirtualMachineFileInfo vmFileInfo = null;
        VirtualMachineFileLayoutEx vmFileLayout = null;
        List<DatastoreMO> datastores = new ArrayList<>();
    }
}