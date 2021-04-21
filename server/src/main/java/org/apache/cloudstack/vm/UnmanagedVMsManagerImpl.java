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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import com.cloud.agent.api.PrepareUnmanageVMInstanceAnswer;
import com.cloud.agent.api.PrepareUnmanageVMInstanceCommand;
import com.cloud.event.ActionEvent;
import com.cloud.exception.UnsupportedServiceException;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.vm.NicVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.admin.vm.ImportUnmanagedInstanceCmd;
import org.apache.cloudstack.api.command.admin.vm.ListUnmanagedInstancesCmd;
import org.apache.cloudstack.api.command.admin.vm.UnmanageVMInstanceCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.NicResponse;
import org.apache.cloudstack.api.response.UnmanagedInstanceDiskResponse;
import org.apache.cloudstack.api.response.UnmanagedInstanceResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.engine.orchestration.service.VolumeOrchestrationService;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.utils.volume.VirtualMachineDiskInfo;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.GetUnmanagedInstancesAnswer;
import com.cloud.agent.api.GetUnmanagedInstancesCommand;
import com.cloud.capacity.CapacityManager;
import com.cloud.configuration.Config;
import com.cloud.configuration.Resource;
import com.cloud.dc.DataCenter;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlanner;
import com.cloud.deploy.DeploymentPlanningManager;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.org.Cluster;
import com.cloud.resource.ResourceManager;
import com.cloud.serializer.GsonHelper;
import com.cloud.server.ManagementService;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.GuestOS;
import com.cloud.storage.GuestOSHypervisor;
import com.cloud.storage.StoragePool;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeApiService;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.GuestOSHypervisorDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.UserVO;
import com.cloud.user.dao.UserDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.NicProfile;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.VirtualMachineProfileImpl;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.google.common.base.Strings;
import com.google.gson.Gson;

public class UnmanagedVMsManagerImpl implements UnmanagedVMsManager {
    public static final String VM_IMPORT_DEFAULT_TEMPLATE_NAME = "system-default-vm-import-dummy-template.iso";
    private static final Logger LOGGER = Logger.getLogger(UnmanagedVMsManagerImpl.class);

    @Inject
    private AgentManager agentManager;
    @Inject
    private DataCenterDao dataCenterDao;
    @Inject
    private ClusterDao clusterDao;
    @Inject
    private HostDao hostDao;
    @Inject
    private AccountService accountService;
    @Inject
    private UserDao userDao;
    @Inject
    private VMTemplateDao templateDao;
    @Inject
    private VMTemplatePoolDao templatePoolDao;
    @Inject
    private ServiceOfferingDao serviceOfferingDao;
    @Inject
    private DiskOfferingDao diskOfferingDao;
    @Inject
    private ResourceManager resourceManager;
    @Inject
    private ResourceLimitService resourceLimitService;
    @Inject
    private UserVmManager userVmManager;
    @Inject
    private ResponseGenerator responseGenerator;
    @Inject
    private VolumeOrchestrationService volumeManager;
    @Inject
    private VolumeDao volumeDao;
    @Inject
    private PrimaryDataStoreDao primaryDataStoreDao;
    @Inject
    private NetworkDao networkDao;
    @Inject
    private NetworkOrchestrationService networkOrchestrationService;
    @Inject
    private VMInstanceDao vmDao;
    @Inject
    private CapacityManager capacityManager;
    @Inject
    private VolumeApiService volumeApiService;
    @Inject
    private DeploymentPlanningManager deploymentPlanningManager;
    @Inject
    private VirtualMachineManager virtualMachineManager;
    @Inject
    private ManagementService managementService;
    @Inject
    private NicDao nicDao;
    @Inject
    private NetworkModel networkModel;
    @Inject
    private ConfigurationDao configurationDao;
    @Inject
    private GuestOSDao guestOSDao;
    @Inject
    private GuestOSHypervisorDao guestOSHypervisorDao;
    @Inject
    private VMSnapshotDao vmSnapshotDao;
    @Inject
    private SnapshotDao snapshotDao;
    @Inject
    private UserVmDao userVmDao;

    protected Gson gson;

    public UnmanagedVMsManagerImpl() {
        gson = GsonHelper.getGsonLogger();
    }

    private VMTemplateVO createDefaultDummyVmImportTemplate() {
        VMTemplateVO template = null;
        try {
            template = VMTemplateVO.createSystemIso(templateDao.getNextInSequence(Long.class, "id"), VM_IMPORT_DEFAULT_TEMPLATE_NAME, VM_IMPORT_DEFAULT_TEMPLATE_NAME, true,
                    "", true, 64, Account.ACCOUNT_ID_SYSTEM, "",
                    "VM Import Default Template", false, 1);
            template.setState(VirtualMachineTemplate.State.Inactive);
            template = templateDao.persist(template);
            if (template == null) {
                return null;
            }
            templateDao.remove(template.getId());
            template = templateDao.findByName(VM_IMPORT_DEFAULT_TEMPLATE_NAME);
        } catch (Exception e) {
            LOGGER.error("Unable to create default dummy template for VM import", e);
        }
        return template;
    }

    private UnmanagedInstanceResponse createUnmanagedInstanceResponse(UnmanagedInstanceTO instance, Cluster cluster, Host host) {
        UnmanagedInstanceResponse response = new UnmanagedInstanceResponse();
        response.setName(instance.getName());
        if (cluster != null) {
            response.setClusterId(cluster.getUuid());
        }
        if (host != null) {
            response.setHostId(host.getUuid());
        }
        response.setPowerState(instance.getPowerState().toString());
        response.setCpuCores(instance.getCpuCores());
        response.setCpuSpeed(instance.getCpuSpeed());
        response.setCpuCoresPerSocket(instance.getCpuCoresPerSocket());
        response.setMemory(instance.getMemory());
        response.setOperatingSystemId(instance.getOperatingSystemId());
        response.setOperatingSystem(instance.getOperatingSystem());
        response.setObjectName("unmanagedinstance");

        if (instance.getDisks() != null) {
            for (UnmanagedInstanceTO.Disk disk : instance.getDisks()) {
                UnmanagedInstanceDiskResponse diskResponse = new UnmanagedInstanceDiskResponse();
                diskResponse.setDiskId(disk.getDiskId());
                if (!Strings.isNullOrEmpty(disk.getLabel())) {
                    diskResponse.setLabel(disk.getLabel());
                }
                diskResponse.setCapacity(disk.getCapacity());
                diskResponse.setController(disk.getController());
                diskResponse.setControllerUnit(disk.getControllerUnit());
                diskResponse.setPosition(disk.getPosition());
                diskResponse.setImagePath(disk.getImagePath());
                diskResponse.setDatastoreName(disk.getDatastoreName());
                diskResponse.setDatastoreHost(disk.getDatastoreHost());
                diskResponse.setDatastorePath(disk.getDatastorePath());
                diskResponse.setDatastoreType(disk.getDatastoreType());
                response.addDisk(diskResponse);
            }
        }

        if (instance.getNics() != null) {
            for (UnmanagedInstanceTO.Nic nic : instance.getNics()) {
                NicResponse nicResponse = new NicResponse();
                nicResponse.setId(nic.getNicId());
                nicResponse.setNetworkName(nic.getNetwork());
                nicResponse.setMacAddress(nic.getMacAddress());
                if (!Strings.isNullOrEmpty(nic.getAdapterType())) {
                    nicResponse.setAdapterType(nic.getAdapterType());
                }
                if (!CollectionUtils.isEmpty(nic.getIpAddress())) {
                    nicResponse.setIpAddresses(nic.getIpAddress());
                }
                nicResponse.setVlanId(nic.getVlan());
                nicResponse.setIsolatedPvlanId(nic.getPvlan());
                nicResponse.setIsolatedPvlanType(nic.getPvlanType());
                response.addNic(nicResponse);
            }
        }
        return response;
    }

    private List<String> getAdditionalNameFilters(Cluster cluster) {
        List<String> additionalNameFilter = new ArrayList<>();
        if (cluster == null) {
            return additionalNameFilter;
        }
        if (cluster.getHypervisorType() == Hypervisor.HypervisorType.VMware) {
            // VMWare considers some templates as VM and they are not filtered by VirtualMachineMO.isTemplate()
            List<VMTemplateStoragePoolVO> templates = templatePoolDao.listAll();
            for (VMTemplateStoragePoolVO template : templates) {
                additionalNameFilter.add(template.getInstallPath());
            }

            // VMWare considers some removed volumes as VM
            List<VolumeVO> volumes = volumeDao.findIncludingRemovedByZone(cluster.getDataCenterId());
            for (VolumeVO volumeVO : volumes) {
                if (volumeVO.getRemoved() == null) {
                    continue;
                }
                if (Strings.isNullOrEmpty(volumeVO.getChainInfo())) {
                    continue;
                }
                List<String> volumeFileNames = new ArrayList<>();
                try {
                    VirtualMachineDiskInfo diskInfo = gson.fromJson(volumeVO.getChainInfo(), VirtualMachineDiskInfo.class);
                    String[] files = diskInfo.getDiskChain();
                    if (files.length == 1) {
                        continue;
                    }
                    boolean firstFile = true;
                    for (final String file : files) {
                        if (firstFile) {
                            firstFile = false;
                            continue;
                        }
                        String path = file;
                        String[] split = path.split(" ");
                        path = split[split.length - 1];
                        split = path.split("/");
                        ;
                        path = split[split.length - 1];
                        split = path.split("\\.");
                        path = split[0];
                        if (!Strings.isNullOrEmpty(path)) {
                            if (!additionalNameFilter.contains(path)) {
                                volumeFileNames.add(path);
                            }
                            if (path.contains("-")) {
                                split = path.split("-");
                                path = split[0];
                                if (!Strings.isNullOrEmpty(path) && !path.equals("ROOT") && !additionalNameFilter.contains(path)) {
                                    volumeFileNames.add(path);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    LOGGER.warn(String.format("Unable to find volume file name for volume ID: %s while adding filters unmanaged VMs", volumeVO.getUuid()), e);
                }
                if (!volumeFileNames.isEmpty()) {
                    additionalNameFilter.addAll(volumeFileNames);
                }
            }
        }
        return additionalNameFilter;
    }

    private List<String> getHostManagedVms(Host host) {
        List<String> managedVms = new ArrayList<>();
        List<VMInstanceVO> instances = vmDao.listByHostId(host.getId());
        for (VMInstanceVO instance : instances) {
            managedVms.add(instance.getInstanceName());
        }
        instances = vmDao.listByLastHostIdAndStates(host.getId(),
                VirtualMachine.State.Stopped, VirtualMachine.State.Destroyed,
                VirtualMachine.State.Expunging, VirtualMachine.State.Error,
                VirtualMachine.State.Unknown, VirtualMachine.State.Shutdown);
        for (VMInstanceVO instance : instances) {
            managedVms.add(instance.getInstanceName());
        }
        return managedVms;
    }

    private boolean hostSupportsServiceOffering(HostVO host, ServiceOffering serviceOffering) {
        if (host == null) {
            return false;
        }
        if (serviceOffering == null) {
            return false;
        }
        if (Strings.isNullOrEmpty(serviceOffering.getHostTag())) {
            return true;
        }
        hostDao.loadHostTags(host);
        return host.getHostTags() != null && host.getHostTags().contains(serviceOffering.getHostTag());
    }

    private boolean storagePoolSupportsDiskOffering(StoragePool pool, DiskOffering diskOffering) {
        if (pool == null) {
            return false;
        }
        if (diskOffering == null) {
            return false;
        }
        return volumeApiService.doesTargetStorageSupportDiskOffering(pool, diskOffering.getTags());
    }

    private boolean storagePoolSupportsServiceOffering(StoragePool pool, ServiceOffering serviceOffering) {
        if (pool == null) {
            return false;
        }
        if (serviceOffering == null) {
            return false;
        }
        return volumeApiService.doesTargetStorageSupportDiskOffering(pool, serviceOffering.getTags());
    }

    private ServiceOfferingVO getUnmanagedInstanceServiceOffering(final UnmanagedInstanceTO instance, ServiceOfferingVO serviceOffering, final Account owner, final DataCenter zone, final Map<String, String> details)
            throws ServerApiException, PermissionDeniedException, ResourceAllocationException {
        if (instance == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("VM is not valid"));
        }
        if (serviceOffering == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Service offering is not valid"));
        }
        accountService.checkAccess(owner, serviceOffering, zone);
        final Integer cpu = instance.getCpuCores();
        final Integer memory = instance.getMemory();
        Integer cpuSpeed = instance.getCpuSpeed() == null ? 0 : instance.getCpuSpeed();
        if (cpu == null || cpu == 0) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("CPU cores for VM (%s) not valid", instance.getName()));
        }
        if (memory == null || memory == 0) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Memory for VM (%s) not valid", instance.getName()));
        }
        if (serviceOffering.isDynamic()) {
            if (details.containsKey(VmDetailConstants.CPU_SPEED)) {
                try {
                    cpuSpeed = Integer.parseInt(details.get(VmDetailConstants.CPU_SPEED));
                } catch (Exception e) {
                }
            }
            Map<String, String> parameters = new HashMap<>();
            parameters.put(VmDetailConstants.CPU_NUMBER, String.valueOf(cpu));
            parameters.put(VmDetailConstants.MEMORY, String.valueOf(memory));
            if (serviceOffering.getSpeed() == null && cpuSpeed > 0) {
                parameters.put(VmDetailConstants.CPU_SPEED, String.valueOf(cpuSpeed));
            }
            serviceOffering.setDynamicFlag(true);
            userVmManager.validateCustomParameters(serviceOffering, parameters);
            serviceOffering = serviceOfferingDao.getComputeOffering(serviceOffering, parameters);
        } else {
            if (!cpu.equals(serviceOffering.getCpu()) && !instance.getPowerState().equals(UnmanagedInstanceTO.PowerState.PowerOff)) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Service offering (%s) %d CPU cores do not match VM CPU cores %d and VM is not in powered off state (Power state: %s)", serviceOffering.getUuid(), serviceOffering.getCpu(), cpu, instance.getPowerState()));
            }
            if (!memory.equals(serviceOffering.getRamSize()) && !instance.getPowerState().equals(UnmanagedInstanceTO.PowerState.PowerOff)) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Service offering (%s) %dMB memory does not match VM memory %dMB and VM is not in powered off state (Power state: %s)", serviceOffering.getUuid(), serviceOffering.getRamSize(), memory, instance.getPowerState()));
            }
            if (cpuSpeed != null && cpuSpeed > 0 && !cpuSpeed.equals(serviceOffering.getSpeed()) && !instance.getPowerState().equals(UnmanagedInstanceTO.PowerState.PowerOff)) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Service offering (%s) %dMHz CPU speed does not match VM CPU speed %dMHz and VM is not in powered off state (Power state: %s)", serviceOffering.getUuid(), serviceOffering.getSpeed(), cpuSpeed, instance.getPowerState()));
            }
        }
        resourceLimitService.checkResourceLimit(owner, Resource.ResourceType.cpu, new Long(serviceOffering.getCpu()));
        resourceLimitService.checkResourceLimit(owner, Resource.ResourceType.memory, new Long(serviceOffering.getRamSize()));
        return serviceOffering;
    }

    private Map<String, Network.IpAddresses> getNicIpAddresses(final List<UnmanagedInstanceTO.Nic> nics, final Map<String, Network.IpAddresses> callerNicIpAddressMap) {
        Map<String, Network.IpAddresses> nicIpAddresses = new HashMap<>();
        for (UnmanagedInstanceTO.Nic nic : nics) {
            Network.IpAddresses ipAddresses = null;
            if (MapUtils.isNotEmpty(callerNicIpAddressMap) && callerNicIpAddressMap.containsKey(nic.getNicId())) {
                ipAddresses = callerNicIpAddressMap.get(nic.getNicId());
            }
            // If IP is set to auto-assign, check NIC doesn't have more that one IP from SDK
            if (ipAddresses != null && ipAddresses.getIp4Address() != null && ipAddresses.getIp4Address().equals("auto") && !CollectionUtils.isEmpty(nic.getIpAddress())) {
                if (nic.getIpAddress().size() > 1) {
                    throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Multiple IP addresses (%s, %s) present for nic ID: %s. IP address cannot be assigned automatically, only single IP address auto-assigning supported", nic.getIpAddress().get(0), nic.getIpAddress().get(1), nic.getNicId()));
                }
                String address = nic.getIpAddress().get(0);
                if (NetUtils.isValidIp4(address)) {
                    ipAddresses.setIp4Address(address);
                }
            }
            if (ipAddresses != null) {
                nicIpAddresses.put(nic.getNicId(), ipAddresses);
            }
        }
        return nicIpAddresses;
    }

    private StoragePool getStoragePool(final UnmanagedInstanceTO.Disk disk, final DataCenter zone, final Cluster cluster) {
        StoragePool storagePool = null;
        final String dsHost = disk.getDatastoreHost();
        final String dsPath = disk.getDatastorePath();
        final String dsType = disk.getDatastoreType();
        final String dsName = disk.getDatastoreName();
        if (dsType != null) {
            List<StoragePoolVO> pools = primaryDataStoreDao.listPoolByHostPath(dsHost, dsPath);
            for (StoragePool pool : pools) {
                if (pool.getDataCenterId() == zone.getId() &&
                        (pool.getClusterId() == null || pool.getClusterId().equals(cluster.getId()))) {
                    storagePool = pool;
                    break;
                }
            }
        }

        if (storagePool == null) {
            List<StoragePoolVO> pools = primaryDataStoreDao.listPoolsByCluster(cluster.getId());
            pools.addAll(primaryDataStoreDao.listByDataCenterId(zone.getId()));
            for (StoragePool pool : pools) {
                if (pool.getPath().endsWith(dsName)) {
                    storagePool = pool;
                    break;
                }
            }
        }
        if (storagePool == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Storage pool for disk %s(%s) with datastore: %s not found in zone ID: %s", disk.getLabel(), disk.getDiskId(), disk.getDatastoreName(), zone.getUuid()));
        }
        return storagePool;
    }

    private Pair<UnmanagedInstanceTO.Disk, List<UnmanagedInstanceTO.Disk>> getRootAndDataDisks(List<UnmanagedInstanceTO.Disk> disks, final Map<String, Long> dataDiskOfferingMap) {
        UnmanagedInstanceTO.Disk rootDisk = null;
        List<UnmanagedInstanceTO.Disk> dataDisks = new ArrayList<>();
        if (disks.size() == 1) {
            rootDisk = disks.get(0);
            return new Pair<>(rootDisk, dataDisks);
        }
        Set<String> callerDiskIds = dataDiskOfferingMap.keySet();
        if (callerDiskIds.size() != disks.size() - 1) {
            String msg = String.format("VM has total %d disks for which %d disk offering mappings provided. %d disks need a disk offering for import", disks.size(), callerDiskIds.size(), disks.size()-1);
            LOGGER.error(String.format("%s. %s parameter can be used to provide disk offerings for the disks", msg, ApiConstants.DATADISK_OFFERING_LIST));
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, msg);
        }
        List<String> diskIdsWithoutOffering = new ArrayList<>();
        for (UnmanagedInstanceTO.Disk disk : disks) {
            String diskId = disk.getDiskId();
            if (!callerDiskIds.contains(diskId)) {
                diskIdsWithoutOffering.add(diskId);
                rootDisk = disk;
            } else {
                dataDisks.add(disk);
            }
        }
        if (diskIdsWithoutOffering.size() > 1) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("VM has total %d disks, disk offering mapping not provided for %d disks. Disk IDs that may need a disk offering - %s", disks.size(), diskIdsWithoutOffering.size()-1, String.join(", ", diskIdsWithoutOffering)));
        }
        return new Pair<>(rootDisk, dataDisks);
    }

    private void checkUnmanagedDiskAndOfferingForImport(UnmanagedInstanceTO.Disk disk, DiskOffering diskOffering, ServiceOffering serviceOffering, final Account owner, final DataCenter zone, final Cluster cluster, final boolean migrateAllowed)
            throws ServerApiException, PermissionDeniedException, ResourceAllocationException {
        if (serviceOffering == null && diskOffering == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Disk offering for disk ID: %s not found during VM import", disk.getDiskId()));
        }
        if (diskOffering != null) {
            accountService.checkAccess(owner, diskOffering, zone);
        }
        resourceLimitService.checkResourceLimit(owner, Resource.ResourceType.volume);
        if (disk.getCapacity() == null || disk.getCapacity() == 0) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Size of disk(ID: %s) is found invalid during VM import", disk.getDiskId()));
        }
        if (diskOffering != null && !diskOffering.isCustomized() && diskOffering.getDiskSize() == 0) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Size of fixed disk offering(ID: %s) is found invalid during VM import", diskOffering.getUuid()));
        }
        if (diskOffering != null && !diskOffering.isCustomized() && diskOffering.getDiskSize() < disk.getCapacity()) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Size of disk offering(ID: %s) %dGB is found less than the size of disk(ID: %s) %dGB during VM import", diskOffering.getUuid(), (diskOffering.getDiskSize() / Resource.ResourceType.bytesToGiB), disk.getDiskId(), (disk.getCapacity() / (Resource.ResourceType.bytesToGiB))));
        }
        StoragePool storagePool = getStoragePool(disk, zone, cluster);
        if (diskOffering != null && !migrateAllowed && !storagePoolSupportsDiskOffering(storagePool, diskOffering)) {
            throw new InvalidParameterValueException(String.format("Disk offering: %s is not compatible with storage pool: %s of unmanaged disk: %s", diskOffering.getUuid(), storagePool.getUuid(), disk.getDiskId()));
        }
        if (serviceOffering != null && !migrateAllowed && !storagePoolSupportsServiceOffering(storagePool, serviceOffering)) {
            throw new InvalidParameterValueException(String.format("Service offering: %s is not compatible with storage pool: %s of unmanaged disk: %s", serviceOffering.getUuid(), storagePool.getUuid(), disk.getDiskId()));
        }
    }

    private void checkUnmanagedDiskAndOfferingForImport(List<UnmanagedInstanceTO.Disk> disks, final Map<String, Long> diskOfferingMap, final Account owner, final DataCenter zone, final Cluster cluster, final boolean migrateAllowed)
            throws ServerApiException, PermissionDeniedException, ResourceAllocationException {
        String diskController = null;
        for (UnmanagedInstanceTO.Disk disk : disks) {
            if (disk == null) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Unable to retrieve disk details for VM"));
            }
            if (!diskOfferingMap.containsKey(disk.getDiskId())) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Disk offering for disk ID: %s not found during VM import", disk.getDiskId()));
            }
            if (Strings.isNullOrEmpty(diskController)) {
                diskController = disk.getController();
            } else {
                if (!diskController.equals(disk.getController())) {
                    throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Multiple data disk controllers of different type (%s, %s) are not supported for import. Please make sure that all data disk controllers are of the same type", diskController, disk.getController()));
                }
            }
            checkUnmanagedDiskAndOfferingForImport(disk, diskOfferingDao.findById(diskOfferingMap.get(disk.getDiskId())), null, owner, zone, cluster, migrateAllowed);
        }
    }

    private void checkUnmanagedNicAndNetworkForImport(UnmanagedInstanceTO.Nic nic, Network network, final DataCenter zone, final Account owner, final boolean autoAssign) throws ServerApiException {
        if (nic == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Unable to retrieve NIC details during VM import"));
        }
        if (network == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Network for nic ID: %s not found during VM import", nic.getNicId()));
        }
        if (network.getDataCenterId() != zone.getId()) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Network(ID: %s) for nic(ID: %s) belongs to a different zone than VM to be imported", network.getUuid(), nic.getNicId()));
        }
        networkModel.checkNetworkPermissions(owner, network);
        if (!autoAssign && network.getGuestType().equals(Network.GuestType.Isolated)) {
            return;
        }

        String networkBroadcastUri = network.getBroadcastUri() == null ? null : network.getBroadcastUri().toString();
        if (nic.getVlan() != null && nic.getVlan() != 0 && nic.getPvlan() == null &&
                (Strings.isNullOrEmpty(networkBroadcastUri) ||
                        !networkBroadcastUri.equals(String.format("vlan://%d", nic.getVlan())))) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("VLAN of network(ID: %s) %s is found different from the VLAN of nic(ID: %s) vlan://%d during VM import", network.getUuid(), networkBroadcastUri, nic.getNicId(), nic.getVlan()));
        }
        if (nic.getVlan() != null && nic.getVlan() != 0 && nic.getPvlan() != null && nic.getPvlan() != 0 &&
                (Strings.isNullOrEmpty(network.getBroadcastUri().toString()) ||
                        !networkBroadcastUri.equals(String.format("pvlan://%d-i%d", nic.getVlan(), nic.getPvlan())))) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("PVLAN of network(ID: %s) %s is found different from the VLAN of nic(ID: %s) pvlan://%d-i%d during VM import", network.getUuid(), networkBroadcastUri, nic.getNicId(), nic.getVlan(), nic.getPvlan()));
        }
    }

    private void checkUnmanagedNicAndNetworkHostnameForImport(UnmanagedInstanceTO.Nic nic, Network network, final String hostName) throws ServerApiException {
        if (nic == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Unable to retrieve NIC details during VM import"));
        }
        if (network == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Network for nic ID: %s not found during VM import", nic.getNicId()));
        }
        // Check for duplicate hostname in network, get all vms hostNames in the network
        List<String> hostNames = vmDao.listDistinctHostNames(network.getId());
        if (CollectionUtils.isNotEmpty(hostNames) && hostNames.contains(hostName)) {
            throw new InvalidParameterValueException("The vm with hostName " + hostName + " already exists in the network domain: " + network.getNetworkDomain() + "; network="
                    + network);
        }
    }

    private void checkUnmanagedNicIpAndNetworkForImport(UnmanagedInstanceTO.Nic nic, Network network, final Network.IpAddresses ipAddresses) throws ServerApiException {
        if (nic == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Unable to retrieve NIC details during VM import"));
        }
        if (network == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Network for nic ID: %s not found during VM import", nic.getNicId()));
        }
        // Check IP is assigned for non L2 networks
        if (!network.getGuestType().equals(Network.GuestType.L2) && (ipAddresses == null || Strings.isNullOrEmpty(ipAddresses.getIp4Address()))) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("NIC(ID: %s) needs a valid IP address for it to be associated with network(ID: %s). %s parameter of API can be used for this", nic.getNicId(), network.getUuid(), ApiConstants.NIC_IP_ADDRESS_LIST));
        }
        // If network is non L2, IP v4 is assigned and not set to auto-assign, check it is available for network
        if (!network.getGuestType().equals(Network.GuestType.L2) && ipAddresses != null && !Strings.isNullOrEmpty(ipAddresses.getIp4Address()) && !ipAddresses.getIp4Address().equals("auto")) {
            Set<Long> ips = networkModel.getAvailableIps(network, ipAddresses.getIp4Address());
            if (CollectionUtils.isEmpty(ips) || !ips.contains(NetUtils.ip2Long(ipAddresses.getIp4Address()))) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("IP address %s for NIC(ID: %s) is not available in network(ID: %s)", ipAddresses.getIp4Address(), nic.getNicId(), network.getUuid()));
            }
        }
    }

    private Map<String, Long> getUnmanagedNicNetworkMap(List<UnmanagedInstanceTO.Nic> nics, final Map<String, Long> callerNicNetworkMap, final Map<String, Network.IpAddresses> callerNicIpAddressMap, final DataCenter zone, final String hostName, final Account owner) throws ServerApiException {
        Map<String, Long> nicNetworkMap = new HashMap<>();
        String nicAdapter = null;
        for (UnmanagedInstanceTO.Nic nic : nics) {
            if (Strings.isNullOrEmpty(nicAdapter)) {
                nicAdapter = nic.getAdapterType();
            } else {
                if (!nicAdapter.equals(nic.getAdapterType())) {
                    throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Multiple network adapter of different type (%s, %s) are not supported for import. Please make sure that all network adapters are of the same type", nicAdapter, nic.getAdapterType()));
                }
            }
            Network network = null;
            Network.IpAddresses ipAddresses = null;
            if (MapUtils.isNotEmpty(callerNicIpAddressMap) && callerNicIpAddressMap.containsKey(nic.getNicId())) {
                ipAddresses = callerNicIpAddressMap.get(nic.getNicId());
            }
            if (!callerNicNetworkMap.containsKey(nic.getNicId())) {
                if (nic.getVlan() != null && nic.getVlan() != 0) {
                    // Find a suitable network
                    List<NetworkVO> networks = networkDao.listByZone(zone.getId());
                    for (NetworkVO networkVO : networks) {
                        if (networkVO.getTrafficType() == Networks.TrafficType.None || Networks.TrafficType.isSystemNetwork(networkVO.getTrafficType())) {
                            continue;
                        }
                        try {
                            checkUnmanagedNicAndNetworkForImport(nic, networkVO, zone, owner, true);
                            network = networkVO;
                        } catch (Exception e) {
                        }
                        if (network != null) {
                            checkUnmanagedNicAndNetworkHostnameForImport(nic, network, hostName);
                            checkUnmanagedNicIpAndNetworkForImport(nic, network, ipAddresses);
                            break;
                        }
                    }
                }
            } else {
                network = networkDao.findById(callerNicNetworkMap.get(nic.getNicId()));
                checkUnmanagedNicAndNetworkForImport(nic, network, zone, owner, false);
                checkUnmanagedNicAndNetworkHostnameForImport(nic, network, hostName);
                checkUnmanagedNicIpAndNetworkForImport(nic, network, ipAddresses);
            }
            if (network == null) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Suitable network for nic(ID: %s) not found during VM import", nic.getNicId()));
            }
            nicNetworkMap.put(nic.getNicId(), network.getId());
        }
        return nicNetworkMap;
    }

    private Pair<DiskProfile, StoragePool> importDisk(UnmanagedInstanceTO.Disk disk, VirtualMachine vm, Cluster cluster, DiskOffering diskOffering,
                                                      Volume.Type type, String name, Long diskSize, Long minIops, Long maxIops, VirtualMachineTemplate template,
                                                      Account owner, Long deviceId) {
        final DataCenter zone = dataCenterDao.findById(vm.getDataCenterId());
        final String path = Strings.isNullOrEmpty(disk.getFileBaseName()) ? disk.getImagePath() : disk.getFileBaseName();
        String chainInfo = disk.getChainInfo();
        if (Strings.isNullOrEmpty(chainInfo)) {
            VirtualMachineDiskInfo diskInfo = new VirtualMachineDiskInfo();
            diskInfo.setDiskDeviceBusName(String.format("%s%d:%d", disk.getController(), disk.getControllerUnit(), disk.getPosition()));
            diskInfo.setDiskChain(new String[]{disk.getImagePath()});
            chainInfo = gson.toJson(diskInfo);
        }
        StoragePool storagePool = getStoragePool(disk, zone, cluster);
        DiskProfile profile = volumeManager.importVolume(type, name, diskOffering, diskSize,
                minIops, maxIops, vm, template, owner, deviceId, storagePool.getId(), path, chainInfo);

        return new Pair<DiskProfile, StoragePool>(profile, storagePool);
    }

    private NicProfile importNic(UnmanagedInstanceTO.Nic nic, VirtualMachine vm, Network network, Network.IpAddresses ipAddresses, boolean isDefaultNic, boolean forced) throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException {
        Pair<NicProfile, Integer> result = networkOrchestrationService.importNic(nic.getMacAddress(), 0, network, isDefaultNic, vm, ipAddresses, forced);
        if (result == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("NIC ID: %s import failed", nic.getNicId()));
        }
        return result.first();
    }

    private void cleanupFailedImportVM(final UserVm userVm) {
        if (userVm == null) {
            return;
        }
        VirtualMachineProfile profile = new VirtualMachineProfileImpl(userVm);
        // Remove all volumes
        volumeDao.deleteVolumesByInstance(userVm.getId());
        // Remove all nics
        try {
            networkOrchestrationService.release(profile, true);
        } catch (Exception e) {
            LOGGER.error(String.format("Unable to release NICs for unsuccessful import unmanaged VM: %s", userVm.getInstanceName()), e);
            nicDao.removeNicsForInstance(userVm.getId());
        }
        // Remove vm
        vmDao.remove(userVm.getId());
    }

    private UserVm migrateImportedVM(HostVO sourceHost, VirtualMachineTemplate template, ServiceOfferingVO serviceOffering, UserVm userVm, final Account owner, List<Pair<DiskProfile, StoragePool>> diskProfileStoragePoolList) {
        UserVm vm = userVm;
        if (vm == null) {
            LOGGER.error(String.format("Failed to check migrations need during VM import"));
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Failed to check migrations need during VM import"));
        }
        if (sourceHost == null || serviceOffering == null || diskProfileStoragePoolList == null) {
            LOGGER.error(String.format("Failed to check migrations need during import, VM: %s", userVm.getInstanceName()));
            cleanupFailedImportVM(vm);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Failed to check migrations need during import, VM: %s", userVm.getInstanceName()));
        }
        if (!hostSupportsServiceOffering(sourceHost, serviceOffering)) {
            LOGGER.debug(String.format("VM %s needs to be migrated", vm.getUuid()));
            final VirtualMachineProfile profile = new VirtualMachineProfileImpl(vm, template, serviceOffering, owner, null);
            DeploymentPlanner.ExcludeList excludeList = new DeploymentPlanner.ExcludeList();
            excludeList.addHost(sourceHost.getId());
            final DataCenterDeployment plan = new DataCenterDeployment(sourceHost.getDataCenterId(), sourceHost.getPodId(), sourceHost.getClusterId(), null, null, null);
            DeployDestination dest = null;
            try {
                dest = deploymentPlanningManager.planDeployment(profile, plan, excludeList, null);
            } catch (Exception e) {
                LOGGER.warn(String.format("VM import failed for unmanaged vm: %s during vm migration, finding deployment destination", vm.getInstanceName()), e);
                cleanupFailedImportVM(vm);
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("VM import failed for unmanaged vm: %s during vm migration, finding deployment destination", vm.getInstanceName()));
            }
            if (dest != null) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(" Found " + dest + " for migrating the vm to");
                }
            }
            if (dest == null) {
                cleanupFailedImportVM(vm);
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("VM import failed for unmanaged vm: %s during vm migration, no deployment destination found", vm.getInstanceName()));
            }
            try {
                if (vm.getState().equals(VirtualMachine.State.Stopped)) {
                    VMInstanceVO vmInstanceVO = vmDao.findById(userVm.getId());
                    vmInstanceVO.setHostId(dest.getHost().getId());
                    vmInstanceVO.setLastHostId(dest.getHost().getId());
                    vmDao.update(vmInstanceVO.getId(), vmInstanceVO);
                } else {
                    virtualMachineManager.migrate(vm.getUuid(), sourceHost.getId(), dest);
                }
                vm = userVmManager.getUserVm(vm.getId());
            } catch (Exception e) {
                LOGGER.error(String.format("VM import failed for unmanaged vm: %s during vm migration", vm.getInstanceName()), e);
                cleanupFailedImportVM(vm);
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("VM import failed for unmanaged vm: %s during vm migration. %s", userVm.getInstanceName(), e.getMessage()));
            }
        }
        for (Pair<DiskProfile, StoragePool> diskProfileStoragePool : diskProfileStoragePoolList) {
            if (diskProfileStoragePool == null ||
                    diskProfileStoragePool.first() == null ||
                    diskProfileStoragePool.second() == null) {
                continue;
            }
            DiskProfile profile = diskProfileStoragePool.first();
            DiskOffering dOffering = diskOfferingDao.findById(profile.getDiskOfferingId());
            if (dOffering == null) {
                continue;
            }
            VolumeVO volumeVO = volumeDao.findById(profile.getVolumeId());
            if (volumeVO == null) {
                continue;
            }
            boolean poolSupportsOfferings = storagePoolSupportsDiskOffering(diskProfileStoragePool.second(), dOffering);
            if (poolSupportsOfferings && profile.getType() == Volume.Type.ROOT) {
                poolSupportsOfferings = storagePoolSupportsServiceOffering(diskProfileStoragePool.second(), serviceOffering);
            }
            if (poolSupportsOfferings) {
                continue;
            }
            LOGGER.debug(String.format("Volume %s needs to be migrated", volumeVO.getUuid()));
            Pair<List<? extends StoragePool>, List<? extends StoragePool>> poolsPair = managementService.listStoragePoolsForMigrationOfVolume(profile.getVolumeId());
            if (CollectionUtils.isEmpty(poolsPair.first()) && CollectionUtils.isEmpty(poolsPair.second())) {
                cleanupFailedImportVM(vm);
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("VM import failed for unmanaged vm: %s during volume ID: %s migration as no suitable pool(s) found", userVm.getInstanceName(), volumeVO.getUuid()));
            }
            List<? extends StoragePool> storagePools = poolsPair.second();
            StoragePool storagePool = null;
            if (CollectionUtils.isNotEmpty(storagePools)) {
                for (StoragePool pool : storagePools) {
                    if (diskProfileStoragePool.second().getId() != pool.getId() &&
                            storagePoolSupportsDiskOffering(pool, dOffering) &&
                            (!profile.getType().equals(Volume.Type.ROOT) ||
                                    profile.getType().equals(Volume.Type.ROOT) && storagePoolSupportsServiceOffering(pool, serviceOffering))) {
                        storagePool = pool;
                        break;
                    }
                }
            }
            // For zone-wide pools, at times, suitable storage pools are not returned therefore consider all pools.
            if (storagePool == null && CollectionUtils.isNotEmpty(poolsPair.first())) {
                storagePools = poolsPair.first();
                for (StoragePool pool : storagePools) {
                    if (diskProfileStoragePool.second().getId() != pool.getId() &&
                            storagePoolSupportsDiskOffering(pool, dOffering) &&
                            (!profile.getType().equals(Volume.Type.ROOT) ||
                                    profile.getType().equals(Volume.Type.ROOT) && storagePoolSupportsServiceOffering(pool, serviceOffering))) {
                        storagePool = pool;
                        break;
                    }
                }
            }
            if (storagePool == null) {
                cleanupFailedImportVM(vm);
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("VM import failed for unmanaged vm: %s during volume ID: %s migration as no suitable pool found", userVm.getInstanceName(), volumeVO.getUuid()));
            } else {
                LOGGER.debug(String.format("Found storage pool %s(%s) for migrating the volume %s to", storagePool.getName(), storagePool.getUuid(), volumeVO.getUuid()));
            }
            try {
                Volume volume = null;
                if (vm.getState().equals(VirtualMachine.State.Running)) {
                    volume = volumeManager.liveMigrateVolume(volumeVO, storagePool);
                } else {
                    volume = volumeManager.migrateVolume(volumeVO, storagePool);
                }
                if (volume == null) {
                    String msg = "";
                    if (vm.getState().equals(VirtualMachine.State.Running)) {
                        msg = String.format("Live migration for volume ID: %s to destination pool ID: %s failed", volumeVO.getUuid(), storagePool.getUuid());
                    } else {
                        msg = String.format("Migration for volume ID: %s to destination pool ID: %s failed", volumeVO.getUuid(), storagePool.getUuid());
                    }
                    LOGGER.error(msg);
                    throw new CloudRuntimeException(msg);
                }
            } catch (Exception e) {
                LOGGER.error(String.format("VM import failed for unmanaged vm: %s during volume migration", vm.getInstanceName()), e);
                cleanupFailedImportVM(vm);
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("VM import failed for unmanaged vm: %s during volume migration. %s", userVm.getInstanceName(), Strings.nullToEmpty(e.getMessage())));
            }
        }
        return userVm;
    }

    private void publishVMUsageUpdateResourceCount(final UserVm userVm, ServiceOfferingVO serviceOfferingVO) {
        if (userVm == null || serviceOfferingVO == null) {
            LOGGER.error("Failed to publish usage records during VM import");
            cleanupFailedImportVM(userVm);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("VM import failed for unmanaged vm during publishing usage records"));
        }
        try {
            if (!serviceOfferingVO.isDynamic()) {
                UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VM_CREATE, userVm.getAccountId(), userVm.getDataCenterId(), userVm.getId(), userVm.getHostName(), serviceOfferingVO.getId(), userVm.getTemplateId(),
                        userVm.getHypervisorType().toString(), VirtualMachine.class.getName(), userVm.getUuid(), userVm.isDisplayVm());
            } else {
                UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VM_CREATE, userVm.getAccountId(), userVm.getAccountId(), userVm.getDataCenterId(), userVm.getHostName(), serviceOfferingVO.getId(), userVm.getTemplateId(),
                        userVm.getHypervisorType().toString(), VirtualMachine.class.getName(), userVm.getUuid(), userVm.getDetails(), userVm.isDisplayVm());
            }
            if (userVm.getState() == VirtualMachine.State.Running) {
                UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VM_START, userVm.getAccountId(), userVm.getDataCenterId(), userVm.getId(), userVm.getHostName(), serviceOfferingVO.getId(), userVm.getTemplateId(),
                        userVm.getHypervisorType().toString(), VirtualMachine.class.getName(), userVm.getUuid(), userVm.isDisplayVm());
            }
        } catch (Exception e) {
            LOGGER.error(String.format("Failed to publish usage records during VM import for unmanaged vm %s", userVm.getInstanceName()), e);
            cleanupFailedImportVM(userVm);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("VM import failed for unmanaged vm %s during publishing usage records", userVm.getInstanceName()));
        }
        resourceLimitService.incrementResourceCount(userVm.getAccountId(), Resource.ResourceType.user_vm, userVm.isDisplayVm());
        resourceLimitService.incrementResourceCount(userVm.getAccountId(), Resource.ResourceType.cpu, userVm.isDisplayVm(), new Long(serviceOfferingVO.getCpu()));
        resourceLimitService.incrementResourceCount(userVm.getAccountId(), Resource.ResourceType.memory, userVm.isDisplayVm(), new Long(serviceOfferingVO.getRamSize()));
        // Save usage event and update resource count for user vm volumes
        List<VolumeVO> volumes = volumeDao.findByInstance(userVm.getId());
        for (VolumeVO volume : volumes) {
            try {
                UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VOLUME_CREATE, volume.getAccountId(), volume.getDataCenterId(), volume.getId(), volume.getName(), volume.getDiskOfferingId(), null, volume.getSize(),
                        Volume.class.getName(), volume.getUuid(), volume.isDisplayVolume());
            } catch (Exception e) {
                LOGGER.error(String.format("Failed to publish volume ID: %s usage records during VM import", volume.getUuid()), e);
            }
            resourceLimitService.incrementResourceCount(userVm.getAccountId(), Resource.ResourceType.volume, volume.isDisplayVolume());
            resourceLimitService.incrementResourceCount(userVm.getAccountId(), Resource.ResourceType.primary_storage, volume.isDisplayVolume(), volume.getSize());
        }

        List<NicVO> nics = nicDao.listByVmId(userVm.getId());
        for (NicVO nic : nics) {
            try {
                NetworkVO network = networkDao.findById(nic.getNetworkId());
                UsageEventUtils.publishUsageEvent(EventTypes.EVENT_NETWORK_OFFERING_ASSIGN, userVm.getAccountId(), userVm.getDataCenterId(), userVm.getId(),
                        Long.toString(nic.getId()), network.getNetworkOfferingId(), null, 1L, VirtualMachine.class.getName(), userVm.getUuid(), userVm.isDisplay());
            } catch (Exception e) {
                LOGGER.error(String.format("Failed to publish network usage records during VM import. %s", Strings.nullToEmpty(e.getMessage())));
            }
        }
    }

    private UserVm importVirtualMachineInternal(final UnmanagedInstanceTO unmanagedInstance, final String instanceName, final DataCenter zone, final Cluster cluster, final HostVO host,
                                                final VirtualMachineTemplate template, final String displayName, final String hostName, final Account caller, final Account owner, final Long userId,
                                                final ServiceOfferingVO serviceOffering, final Map<String, Long> dataDiskOfferingMap,
                                                final Map<String, Long> nicNetworkMap, final Map<String, Network.IpAddresses> callerNicIpAddressMap,
                                                final Map<String, String> details, final boolean migrateAllowed, final boolean forced) {
        UserVm userVm = null;

        ServiceOfferingVO validatedServiceOffering = null;
        try {
            validatedServiceOffering = getUnmanagedInstanceServiceOffering(unmanagedInstance, serviceOffering, owner, zone, details);
        } catch (Exception e) {
            LOGGER.error("Service offering for VM import not compatible", e);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Failed to import VM: %s. %s", unmanagedInstance.getName(), Strings.nullToEmpty(e.getMessage())));
        }

        Map<String, String> allDetails = new HashMap<>(details);
        if (validatedServiceOffering.isDynamic()) {
            allDetails.put(VmDetailConstants.CPU_NUMBER, String.valueOf(validatedServiceOffering.getCpu()));
            allDetails.put(VmDetailConstants.MEMORY, String.valueOf(validatedServiceOffering.getRamSize()));
            if (serviceOffering.getSpeed() == null) {
                allDetails.put(VmDetailConstants.CPU_SPEED, String.valueOf(validatedServiceOffering.getSpeed()));
            }
        }

        if (!migrateAllowed && !hostSupportsServiceOffering(host, validatedServiceOffering)) {
            throw new InvalidParameterValueException(String.format("Service offering: %s is not compatible with host: %s of unmanaged VM: %s", serviceOffering.getUuid(), host.getUuid(), instanceName));
        }
        // Check disks and supplied disk offerings
        List<UnmanagedInstanceTO.Disk> unmanagedInstanceDisks = unmanagedInstance.getDisks();
        if (CollectionUtils.isEmpty(unmanagedInstanceDisks)) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("No attached disks found for the unmanaged VM: %s", instanceName));
        }
        Pair<UnmanagedInstanceTO.Disk, List<UnmanagedInstanceTO.Disk>> rootAndDataDisksPair = getRootAndDataDisks(unmanagedInstanceDisks, dataDiskOfferingMap);
        final UnmanagedInstanceTO.Disk rootDisk = rootAndDataDisksPair.first();
        final List<UnmanagedInstanceTO.Disk> dataDisks = rootAndDataDisksPair.second();
        if (rootDisk == null || Strings.isNullOrEmpty(rootDisk.getController())) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("VM import failed. Unable to retrieve root disk details for VM: %s ", instanceName));
        }
        allDetails.put(VmDetailConstants.ROOT_DISK_CONTROLLER, rootDisk.getController());
        try {
            checkUnmanagedDiskAndOfferingForImport(rootDisk, null, validatedServiceOffering, owner, zone, cluster, migrateAllowed);
            if (CollectionUtils.isNotEmpty(dataDisks)) { // Data disk(s) present
                checkUnmanagedDiskAndOfferingForImport(dataDisks, dataDiskOfferingMap, owner, zone, cluster, migrateAllowed);
                allDetails.put(VmDetailConstants.DATA_DISK_CONTROLLER, dataDisks.get(0).getController());
            }
            resourceLimitService.checkResourceLimit(owner, Resource.ResourceType.volume, unmanagedInstanceDisks.size());
        } catch (ResourceAllocationException e) {
            LOGGER.error(String.format("Volume resource allocation error for owner: %s", owner.getUuid()), e);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Volume resource allocation error for owner: %s. %s", owner.getUuid(), Strings.nullToEmpty(e.getMessage())));
        }
        // Check NICs and supplied networks
        Map<String, Network.IpAddresses> nicIpAddressMap = getNicIpAddresses(unmanagedInstance.getNics(), callerNicIpAddressMap);
        Map<String, Long> allNicNetworkMap = getUnmanagedNicNetworkMap(unmanagedInstance.getNics(), nicNetworkMap, nicIpAddressMap, zone, hostName, owner);
        if (!CollectionUtils.isEmpty(unmanagedInstance.getNics())) {
            allDetails.put(VmDetailConstants.NIC_ADAPTER, unmanagedInstance.getNics().get(0).getAdapterType());
        }
        VirtualMachine.PowerState powerState = VirtualMachine.PowerState.PowerOff;
        if (unmanagedInstance.getPowerState().equals(UnmanagedInstanceTO.PowerState.PowerOn)) {
            powerState = VirtualMachine.PowerState.PowerOn;
        }
        try {
            userVm = userVmManager.importVM(zone, host, template, instanceName, displayName, owner,
                    null, caller, true, null, owner.getAccountId(), userId,
                    validatedServiceOffering, null, hostName,
                    cluster.getHypervisorType(), allDetails, powerState);
        } catch (InsufficientCapacityException ice) {
            LOGGER.error(String.format("Failed to import vm name: %s", instanceName), ice);
            throw new ServerApiException(ApiErrorCode.INSUFFICIENT_CAPACITY_ERROR, ice.getMessage());
        }
        if (userVm == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Failed to import vm name: %s", instanceName));
        }
        List<Pair<DiskProfile, StoragePool>> diskProfileStoragePoolList = new ArrayList<>();
        try {
            if (rootDisk.getCapacity() == null || rootDisk.getCapacity() == 0) {
                throw new InvalidParameterValueException(String.format("Root disk ID: %s size is invalid", rootDisk.getDiskId()));
            }
            Long minIops = null;
            if (details.containsKey("minIops")) {
                minIops = Long.parseLong(details.get("minIops"));
            }
            Long maxIops = null;
            if (details.containsKey("maxIops")) {
                maxIops = Long.parseLong(details.get("maxIops"));
            }
            diskProfileStoragePoolList.add(importDisk(rootDisk, userVm, cluster, serviceOffering, Volume.Type.ROOT, String.format("ROOT-%d", userVm.getId()),
                    (rootDisk.getCapacity() / Resource.ResourceType.bytesToGiB), minIops, maxIops,
                    template, owner, null));
            for (UnmanagedInstanceTO.Disk disk : dataDisks) {
                if (disk.getCapacity() == null || disk.getCapacity() == 0) {
                    throw new InvalidParameterValueException(String.format("Disk ID: %s size is invalid", rootDisk.getDiskId()));
                }
                DiskOffering offering = diskOfferingDao.findById(dataDiskOfferingMap.get(disk.getDiskId()));
                diskProfileStoragePoolList.add(importDisk(disk, userVm, cluster, offering, Volume.Type.DATADISK, String.format("DATA-%d-%s", userVm.getId(), disk.getDiskId()),
                        (disk.getCapacity() / Resource.ResourceType.bytesToGiB), offering.getMinIops(), offering.getMaxIops(),
                        template, owner, null));
            }
        } catch (Exception e) {
            LOGGER.error(String.format("Failed to import volumes while importing vm: %s", instanceName), e);
            cleanupFailedImportVM(userVm);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Failed to import volumes while importing vm: %s. %s", instanceName, Strings.nullToEmpty(e.getMessage())));
        }
        try {
            boolean firstNic = true;
            for (UnmanagedInstanceTO.Nic nic : unmanagedInstance.getNics()) {
                Network network = networkDao.findById(allNicNetworkMap.get(nic.getNicId()));
                Network.IpAddresses ipAddresses = nicIpAddressMap.get(nic.getNicId());
                importNic(nic, userVm, network, ipAddresses, firstNic, forced);
                firstNic = false;
            }
        } catch (Exception e) {
            LOGGER.error(String.format("Failed to import NICs while importing vm: %s", instanceName), e);
            cleanupFailedImportVM(userVm);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Failed to import NICs while importing vm: %s. %s", instanceName, Strings.nullToEmpty(e.getMessage())));
        }
        if (migrateAllowed) {
            userVm = migrateImportedVM(host, template, validatedServiceOffering, userVm, owner, diskProfileStoragePoolList);
        }
        publishVMUsageUpdateResourceCount(userVm, validatedServiceOffering);
        return userVm;
    }

    @Override
    public ListResponse<UnmanagedInstanceResponse> listUnmanagedInstances(ListUnmanagedInstancesCmd cmd) {
        final Account caller = CallContext.current().getCallingAccount();
        if (caller.getType() != Account.ACCOUNT_TYPE_ADMIN) {
            throw new PermissionDeniedException(String.format("Cannot perform this operation, Calling account is not root admin: %s", caller.getUuid()));
        }
        final Long clusterId = cmd.getClusterId();
        if (clusterId == null) {
            throw new InvalidParameterValueException(String.format("Cluster ID cannot be null"));
        }
        final Cluster cluster = clusterDao.findById(clusterId);
        if (cluster == null) {
            throw new InvalidParameterValueException(String.format("Cluster ID: %d cannot be found", clusterId));
        }
        if (cluster.getHypervisorType() != Hypervisor.HypervisorType.VMware) {
            throw new InvalidParameterValueException(String.format("VM ingestion is currently not supported for hypervisor: %s", cluster.getHypervisorType().toString()));
        }
        List<HostVO> hosts = resourceManager.listHostsInClusterByStatus(clusterId, Status.Up);
        List<String> additionalNameFilters = getAdditionalNameFilters(cluster);
        List<UnmanagedInstanceResponse> responses = new ArrayList<>();
        for (HostVO host : hosts) {
            if (host.isInMaintenanceStates()) {
                continue;
            }
            List<String> managedVms = new ArrayList<>();
            managedVms.addAll(additionalNameFilters);
            managedVms.addAll(getHostManagedVms(host));

            GetUnmanagedInstancesCommand command = new GetUnmanagedInstancesCommand();
            command.setInstanceName(cmd.getName());
            command.setManagedInstancesNames(managedVms);
            Answer answer = agentManager.easySend(host.getId(), command);
            if (!(answer instanceof GetUnmanagedInstancesAnswer)) {
                continue;
            }
            GetUnmanagedInstancesAnswer unmanagedInstancesAnswer = (GetUnmanagedInstancesAnswer) answer;
            HashMap<String, UnmanagedInstanceTO> unmanagedInstances = new HashMap<>();
            unmanagedInstances.putAll(unmanagedInstancesAnswer.getUnmanagedInstances());
            Set<String> keys = unmanagedInstances.keySet();
            for (String key : keys) {
                responses.add(createUnmanagedInstanceResponse(unmanagedInstances.get(key), cluster, host));
            }
        }
        ListResponse<UnmanagedInstanceResponse> listResponses = new ListResponse<>();
        listResponses.setResponses(responses, responses.size());
        return listResponses;
    }

    @Override
    public UserVmResponse importUnmanagedInstance(ImportUnmanagedInstanceCmd cmd) {
        final Account caller = CallContext.current().getCallingAccount();
        if (caller.getType() != Account.ACCOUNT_TYPE_ADMIN) {
            throw new PermissionDeniedException(String.format("Cannot perform this operation, Calling account is not root admin: %s", caller.getUuid()));
        }
        final Long clusterId = cmd.getClusterId();
        if (clusterId == null) {
            throw new InvalidParameterValueException(String.format("Cluster ID cannot be null"));
        }
        final Cluster cluster = clusterDao.findById(clusterId);
        if (cluster == null) {
            throw new InvalidParameterValueException(String.format("Cluster ID: %d cannot be found", clusterId));
        }
        if (cluster.getHypervisorType() != Hypervisor.HypervisorType.VMware) {
            throw new InvalidParameterValueException(String.format("VM import is currently not supported for hypervisor: %s", cluster.getHypervisorType().toString()));
        }
        final DataCenter zone = dataCenterDao.findById(cluster.getDataCenterId());
        final String instanceName = cmd.getName();
        if (Strings.isNullOrEmpty(instanceName)) {
            throw new InvalidParameterValueException(String.format("Instance name cannot be empty"));
        }
        if (cmd.getDomainId() != null && Strings.isNullOrEmpty(cmd.getAccountName())) {
            throw new InvalidParameterValueException("domainid parameter must be specified with account parameter");
        }
        final Account owner = accountService.getActiveAccountById(cmd.getEntityOwnerId());
        long userId = CallContext.current().getCallingUserId();
        List<UserVO> userVOs = userDao.listByAccount(owner.getAccountId());
        if (CollectionUtils.isNotEmpty(userVOs)) {
            userId = userVOs.get(0).getId();
        }
        VMTemplateVO template = null;
        final Long templateId = cmd.getTemplateId();
        if (templateId == null) {
            template = templateDao.findByName(VM_IMPORT_DEFAULT_TEMPLATE_NAME);
            if (template == null) {
                template = createDefaultDummyVmImportTemplate();
                if (template == null) {
                    throw new InvalidParameterValueException(String.format("Default VM import template with unique name: %s for hypervisor: %s cannot be created. Please use templateid paramter for import", VM_IMPORT_DEFAULT_TEMPLATE_NAME, cluster.getHypervisorType().toString()));
                }
            }
        } else {
            template = templateDao.findById(templateId);
        }
        if (template == null) {
            throw new InvalidParameterValueException(String.format("Template ID: %d cannot be found", templateId));
        }
        final Long serviceOfferingId = cmd.getServiceOfferingId();
        if (serviceOfferingId == null) {
            throw new InvalidParameterValueException(String.format("Service offering ID cannot be null"));
        }
        final ServiceOfferingVO serviceOffering = serviceOfferingDao.findById(serviceOfferingId);
        if (serviceOffering == null) {
            throw new InvalidParameterValueException(String.format("Service offering ID: %d cannot be found", serviceOfferingId));
        }
        accountService.checkAccess(owner, serviceOffering, zone);
        try {
            resourceLimitService.checkResourceLimit(owner, Resource.ResourceType.user_vm, 1);
        } catch (ResourceAllocationException e) {
            LOGGER.error(String.format("VM resource allocation error for account: %s", owner.getUuid()), e);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("VM resource allocation error for account: %s. %s", owner.getUuid(), Strings.nullToEmpty(e.getMessage())));
        }
        String displayName = cmd.getDisplayName();
        if (Strings.isNullOrEmpty(displayName)) {
            displayName = instanceName;
        }
        String hostName = cmd.getHostName();
        if (Strings.isNullOrEmpty(hostName)) {
            if (!NetUtils.verifyDomainNameLabel(instanceName, true)) {
                throw new InvalidParameterValueException(String.format("Please provide hostname for the VM. VM name contains unsupported characters for it to be used as hostname"));
            }
            hostName = instanceName;
        }
        if (!NetUtils.verifyDomainNameLabel(hostName, true)) {
            throw new InvalidParameterValueException("Invalid VM hostname. VM hostname can contain ASCII letters 'a' through 'z', the digits '0' through '9', "
                    + "and the hyphen ('-'), must be between 1 and 63 characters long, and can't start or end with \"-\" and can't start with digit");
        }
        if (cluster.getHypervisorType().equals(Hypervisor.HypervisorType.VMware) &&
                Boolean.parseBoolean(configurationDao.getValue(Config.SetVmInternalNameUsingDisplayName.key()))) {
            // If global config vm.instancename.flag is set to true, then CS will set guest VM's name as it appears on the hypervisor, to its hostname.
            // In case of VMware since VM name must be unique within a DC, check if VM with the same hostname already exists in the zone.
            VMInstanceVO vmByHostName = vmDao.findVMByHostNameInZone(hostName, zone.getId());
            if (vmByHostName != null && vmByHostName.getState() != VirtualMachine.State.Expunging) {
                throw new InvalidParameterValueException(String.format("Failed to import VM: %s. There already exists a VM by the hostname: %s in zone: %s", instanceName, hostName, zone.getUuid()));
            }
        }
        final Map<String, Long> nicNetworkMap = cmd.getNicNetworkList();
        final Map<String, Network.IpAddresses> nicIpAddressMap = cmd.getNicIpAddressList();
        final Map<String, Long> dataDiskOfferingMap = cmd.getDataDiskToDiskOfferingList();
        final Map<String, String> details = cmd.getDetails();
        final boolean forced = cmd.isForced();
        List<HostVO> hosts = resourceManager.listHostsInClusterByStatus(clusterId, Status.Up);
        UserVm userVm = null;
        List<String> additionalNameFilters = getAdditionalNameFilters(cluster);
        for (HostVO host : hosts) {
            if (host.isInMaintenanceStates()) {
                continue;
            }
            List<String> managedVms = new ArrayList<>();
            managedVms.addAll(additionalNameFilters);
            managedVms.addAll(getHostManagedVms(host));
            GetUnmanagedInstancesCommand command = new GetUnmanagedInstancesCommand(instanceName);
            command.setManagedInstancesNames(managedVms);
            Answer answer = agentManager.easySend(host.getId(), command);
            if (!(answer instanceof GetUnmanagedInstancesAnswer)) {
                continue;
            }
            GetUnmanagedInstancesAnswer unmanagedInstancesAnswer = (GetUnmanagedInstancesAnswer) answer;
            HashMap<String, UnmanagedInstanceTO> unmanagedInstances = unmanagedInstancesAnswer.getUnmanagedInstances();
            if (MapUtils.isEmpty(unmanagedInstances)) {
                continue;
            }
            Set<String> names = unmanagedInstances.keySet();
            for (String name : names) {
                if (instanceName.equals(name)) {
                    UnmanagedInstanceTO unmanagedInstance = unmanagedInstances.get(name);
                    if (unmanagedInstance == null) {
                        throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Unable to retrieve details for unmanaged VM: %s", name));
                    }
                    if (template.getName().equals(VM_IMPORT_DEFAULT_TEMPLATE_NAME)) {
                        String osName = unmanagedInstance.getOperatingSystem();
                        GuestOS guestOS = null;
                        if (!Strings.isNullOrEmpty(osName)) {
                            guestOS = guestOSDao.listByDisplayName(osName);
                        }
                        GuestOSHypervisor guestOSHypervisor = null;
                        if (guestOS != null) {
                            guestOSHypervisor = guestOSHypervisorDao.findByOsIdAndHypervisor(guestOS.getId(), host.getHypervisorType().toString(), host.getHypervisorVersion());
                        }
                        if (guestOSHypervisor == null && !Strings.isNullOrEmpty(unmanagedInstance.getOperatingSystemId())) {
                            guestOSHypervisor = guestOSHypervisorDao.findByOsNameAndHypervisor(unmanagedInstance.getOperatingSystemId(), host.getHypervisorType().toString(), host.getHypervisorVersion());
                        }
                        if (guestOSHypervisor == null) {
                            if (guestOS != null) {
                                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Unable to find hypervisor guest OS ID: %s details for unmanaged VM: %s for hypervisor: %s version: %s. templateid parameter can be used to assign template for VM", guestOS.getUuid(), name, host.getHypervisorType().toString(), host.getHypervisorVersion()));
                            }
                            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Unable to retrieve guest OS details for unmanaged VM: %s with OS name: %s, OS ID: %s for hypervisor: %s version: %s. templateid parameter can be used to assign template for VM", name, osName, unmanagedInstance.getOperatingSystemId(), host.getHypervisorType().toString(), host.getHypervisorVersion()));
                        }
                        template.setGuestOSId(guestOSHypervisor.getGuestOsId());
                    }
                    userVm = importVirtualMachineInternal(unmanagedInstance, instanceName, zone, cluster, host,
                            template, displayName, hostName, caller, owner, userId,
                            serviceOffering, dataDiskOfferingMap,
                            nicNetworkMap, nicIpAddressMap,
                            details, cmd.getMigrateAllowed(), forced);
                    break;
                }
            }
            if (userVm != null) {
                break;
            }
        }
        if (userVm == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Failed to find unmanaged vm with name: %s in cluster: %s", instanceName, cluster.getUuid()));
        }
        return responseGenerator.createUserVmResponse(ResponseObject.ResponseView.Full, "virtualmachine", userVm).get(0);
    }

    @Override
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(ListUnmanagedInstancesCmd.class);
        cmdList.add(ImportUnmanagedInstanceCmd.class);
        cmdList.add(UnmanageVMInstanceCmd.class);
        return cmdList;
    }

    /**
     * Perform validations before attempting to unmanage a VM from CloudStack:
     * - VM must not have any associated volume snapshot
     * - VM must not have an attached ISO
     */
    private void performUnmanageVMInstancePrechecks(VMInstanceVO vmVO) {
        if (hasVolumeSnapshotsPriorToUnmanageVM(vmVO)) {
            throw new UnsupportedServiceException("Cannot unmanage VM with id = " + vmVO.getUuid() +
                    " as there are volume snapshots for its volume(s). Please remove snapshots before unmanaging.");
        }

        if (hasISOAttached(vmVO)) {
            throw new UnsupportedServiceException("Cannot unmanage VM with id = " + vmVO.getUuid() +
                    " as there is an ISO attached. Please detach ISO before unmanaging.");
        }
    }

    private boolean hasVolumeSnapshotsPriorToUnmanageVM(VMInstanceVO vmVO) {
        List<VolumeVO> volumes = volumeDao.findByInstance(vmVO.getId());
        for (VolumeVO volume : volumes) {
            List<SnapshotVO> snaps = snapshotDao.listByVolumeId(volume.getId());
            if (CollectionUtils.isNotEmpty(snaps)) {
                for (SnapshotVO snap : snaps) {
                    if (snap.getState() != Snapshot.State.Destroyed && snap.getRemoved() == null) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean hasISOAttached(VMInstanceVO vmVO) {
        UserVmVO userVM = userVmDao.findById(vmVO.getId());
        if (userVM == null) {
            throw new InvalidParameterValueException("Could not find user VM with ID = " + vmVO.getUuid());
        }
        return userVM.getIsoId() != null;
    }

    /**
     * Find a suitable host within the scope of the VM to unmanage to verify the VM exists
     */
    private Long findSuitableHostId(VMInstanceVO vmVO) {
        Long hostId = vmVO.getHostId();
        if (hostId == null) {
            long zoneId = vmVO.getDataCenterId();
            List<HostVO> hosts = hostDao.listAllHostsUpByZoneAndHypervisor(zoneId, vmVO.getHypervisorType());
            for (HostVO host : hosts) {
                if (host.isInMaintenanceStates() || host.getState() != Status.Up || host.getStatus() != Status.Up) {
                    continue;
                }
                hostId = host.getId();
                break;
            }
        }

        if (hostId == null) {
            throw new CloudRuntimeException("Cannot find a host to verify if the VM to unmanage " +
                    "with id = " + vmVO.getUuid() + " exists.");
        }
        return hostId;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_UNMANAGE, eventDescription = "unmanaging VM", async = true)
    public boolean unmanageVMInstance(long vmId) {
        VMInstanceVO vmVO = vmDao.findById(vmId);
        if (vmVO == null || vmVO.getRemoved() != null) {
            throw new InvalidParameterValueException("Could not find VM to unmanage, it is either removed or not existing VM");
        } else if (vmVO.getState() != VirtualMachine.State.Running && vmVO.getState() != VirtualMachine.State.Stopped) {
            throw new InvalidParameterValueException("VM with id = " + vmVO.getUuid() + " must be running or stopped to be unmanaged");
        } else if (vmVO.getHypervisorType() != Hypervisor.HypervisorType.VMware) {
            throw new UnsupportedServiceException("Unmanage VM is currently allowed for VMware VMs only");
        } else if (vmVO.getType() != VirtualMachine.Type.User) {
            throw new UnsupportedServiceException("Unmanage VM is currently allowed for guest VMs only");
        }

        performUnmanageVMInstancePrechecks(vmVO);

        Long hostId = findSuitableHostId(vmVO);
        String instanceName = vmVO.getInstanceName();

        if (!existsVMToUnmanage(instanceName, hostId)) {
            throw new CloudRuntimeException("VM with id = " + vmVO.getUuid() + " is not found in the hypervisor");
        }

        return userVmManager.unmanageUserVM(vmId);
    }

    /**
     * Verify the VM to unmanage exists on the hypervisor
     */
    private boolean existsVMToUnmanage(String instanceName, Long hostId) {
        PrepareUnmanageVMInstanceCommand command = new PrepareUnmanageVMInstanceCommand();
        command.setInstanceName(instanceName);
        Answer ans = agentManager.easySend(hostId, command);
        if (!(ans instanceof PrepareUnmanageVMInstanceAnswer)) {
            throw new CloudRuntimeException("Error communicating with host " + hostId);
        }
        PrepareUnmanageVMInstanceAnswer answer = (PrepareUnmanageVMInstanceAnswer) ans;
        if (!answer.getResult()) {
            LOGGER.error("Error verifying VM " + instanceName + " exists on host with ID = " + hostId + ": " + answer.getDetails());
        }
        return answer.getResult();
    }

    @Override
    public String getConfigComponentName() {
        return UnmanagedVMsManagerImpl.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] { UnmanageVMPreserveNic };
    }
}
