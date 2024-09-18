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

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckConvertInstanceAnswer;
import com.cloud.agent.api.CheckConvertInstanceCommand;
import com.cloud.agent.api.CheckVolumeAnswer;
import com.cloud.agent.api.CheckVolumeCommand;
import com.cloud.agent.api.ConvertInstanceAnswer;
import com.cloud.agent.api.ConvertInstanceCommand;
import com.cloud.agent.api.CopyRemoteVolumeAnswer;
import com.cloud.agent.api.CopyRemoteVolumeCommand;
import com.cloud.agent.api.GetRemoteVmsAnswer;
import com.cloud.agent.api.GetRemoteVmsCommand;
import com.cloud.agent.api.GetUnmanagedInstancesAnswer;
import com.cloud.agent.api.GetUnmanagedInstancesCommand;
import com.cloud.agent.api.PrepareUnmanageVMInstanceAnswer;
import com.cloud.agent.api.PrepareUnmanageVMInstanceCommand;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.RemoteInstanceTO;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.configuration.Config;
import com.cloud.configuration.Resource;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.VmwareDatacenterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VmwareDatacenterDao;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlanner;
import com.cloud.deploy.DeploymentPlanningManager;
import com.cloud.event.ActionEvent;
import com.cloud.event.ActionEventUtils;
import com.cloud.event.EventTypes;
import com.cloud.event.EventVO;
import com.cloud.event.UsageEventUtils;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.UnsupportedServiceException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.HypervisorGuru;
import com.cloud.hypervisor.HypervisorGuruManager;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.org.Cluster;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceState;
import com.cloud.serializer.GsonHelper;
import com.cloud.server.ManagementService;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.GuestOS;
import com.cloud.storage.GuestOSHypervisor;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage;
import com.cloud.storage.StoragePool;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeApiService;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.GuestOSHypervisorDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.StoragePoolHostDao;
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
import com.cloud.utils.LogUtils;
import com.cloud.utils.Pair;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineName;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.VirtualMachineProfileImpl;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.UserVmDetailsDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.google.gson.Gson;
import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.admin.vm.ImportUnmanagedInstanceCmd;
import org.apache.cloudstack.api.command.admin.vm.ImportVmCmd;
import org.apache.cloudstack.api.command.admin.vm.ListUnmanagedInstancesCmd;
import org.apache.cloudstack.api.command.admin.vm.ListVmsForImportCmd;
import org.apache.cloudstack.api.command.admin.vm.UnmanageVMInstanceCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.NicResponse;
import org.apache.cloudstack.api.response.UnmanagedInstanceDiskResponse;
import org.apache.cloudstack.api.response.UnmanagedInstanceResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.engine.orchestration.service.VolumeOrchestrationService;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreVO;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.utils.volume.VirtualMachineDiskInfo;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.cloudstack.api.ApiConstants.MAX_IOPS;
import static org.apache.cloudstack.api.ApiConstants.MIN_IOPS;

public class UnmanagedVMsManagerImpl implements UnmanagedVMsManager {
    public static final String VM_IMPORT_DEFAULT_TEMPLATE_NAME = "system-default-vm-import-dummy-template.iso";
    public static final String KVM_VM_IMPORT_DEFAULT_TEMPLATE_NAME = "kvm-default-vm-import-dummy-template";
    protected Logger logger = LogManager.getLogger(UnmanagedVMsManagerImpl.class);
    private static final List<Hypervisor.HypervisorType> importUnmanagedInstancesSupportedHypervisors =
            Arrays.asList(Hypervisor.HypervisorType.VMware, Hypervisor.HypervisorType.KVM);

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
    private UserVmDetailsDao userVmDetailsDao;
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
    private SnapshotDao snapshotDao;
    @Inject
    private UserVmDao userVmDao;
    @Inject
    private NetworkOfferingDao networkOfferingDao;
    @Inject
    EntityManager entityMgr;
    @Inject
    private NetworkOrchestrationService networkMgr;
    @Inject
    private PhysicalNetworkDao physicalNetworkDao;
    @Inject
    private IpAddressManager ipAddressManager;
    @Inject
    private StoragePoolHostDao storagePoolHostDao;
    @Inject
    private HypervisorGuruManager hypervisorGuruManager;
    @Inject
    private VmwareDatacenterDao vmwareDatacenterDao;
    @Inject
    private ImageStoreDao imageStoreDao;
    @Inject
    private DataStoreManager dataStoreManager;

    protected Gson gson;

    public UnmanagedVMsManagerImpl() {
        gson = GsonHelper.getGsonLogger();
    }

    private VMTemplateVO createDefaultDummyVmImportTemplate(boolean isKVM) {
        String templateName = (isKVM) ? KVM_VM_IMPORT_DEFAULT_TEMPLATE_NAME : VM_IMPORT_DEFAULT_TEMPLATE_NAME;
        VMTemplateVO template = null;
        try {
            template = VMTemplateVO.createSystemIso(templateDao.getNextInSequence(Long.class, "id"), templateName, templateName, true,
                    "", true, 64, Account.ACCOUNT_ID_SYSTEM, "",
                    "VM Import Default Template", false, 1);
            template.setState(VirtualMachineTemplate.State.Inactive);
            template = templateDao.persist(template);
            if (template == null) {
                return null;
            }
            templateDao.remove(template.getId());
            template = templateDao.findByName(templateName);
        } catch (Exception e) {
            logger.error("Unable to create default dummy template for VM import", e);
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
            response.setHostName(host.getName());
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
                if (StringUtils.isNotEmpty(disk.getLabel())) {
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
                if (StringUtils.isNotEmpty(nic.getAdapterType())) {
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
                if (StringUtils.isEmpty(volumeVO.getChainInfo())) {
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
                        path = split[split.length - 1];
                        split = path.split("\\.");
                        path = split[0];
                        if (StringUtils.isNotEmpty(path)) {
                            if (!additionalNameFilter.contains(path)) {
                                volumeFileNames.add(path);
                            }
                            if (path.contains("-")) {
                                split = path.split("-");
                                path = split[0];
                                if (StringUtils.isNotEmpty(path) && !path.equals("ROOT") && !additionalNameFilter.contains(path)) {
                                    volumeFileNames.add(path);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn(String.format("Unable to find volume file name for volume ID: %s while adding filters unmanaged VMs", volumeVO.getUuid()), e);
                }
                if (!volumeFileNames.isEmpty()) {
                    additionalNameFilter.addAll(volumeFileNames);
                }
            }
        }
        return additionalNameFilter;
    }

    private List<String> getHostsManagedVms(List<HostVO> hosts) {
        if (CollectionUtils.isEmpty(hosts)) {
            return new ArrayList<>();
        }
        List<VMInstanceVO> instances = vmDao.listByHostOrLastHostOrHostPod(hosts.stream().map(HostVO::getId).collect(Collectors.toList()), hosts.get(0).getPodId());
        List<String> managedVms = instances.stream().map(VMInstanceVO::getInstanceName).collect(Collectors.toList());
        return managedVms;
    }

    private boolean hostSupportsServiceOfferingAndTemplate(HostVO host, ServiceOffering serviceOffering, VirtualMachineTemplate template) {
        if (StringUtils.isAllEmpty(serviceOffering.getHostTag(), template.getTemplateTag())) {
            return true;
        }
        hostDao.loadHostTags(host);
        return host.checkHostServiceOfferingAndTemplateTags(serviceOffering, template, UserVmManager.getStrictHostTags());
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

    private ServiceOfferingVO getUnmanagedInstanceServiceOffering(final UnmanagedInstanceTO instance, ServiceOfferingVO serviceOffering, final Account owner, final DataCenter zone, final Map<String, String> details, Hypervisor.HypervisorType hypervisorType)
            throws ServerApiException, PermissionDeniedException, ResourceAllocationException {
        if (instance == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Cannot find VM to import.");
        }
        if (serviceOffering == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Cannot find service offering used to import VM [%s].", instance.getName()));
        }
        accountService.checkAccess(owner, serviceOffering, zone);
        final Integer cpu = instance.getCpuCores();
        final Integer memory = instance.getMemory();
        Integer cpuSpeed = instance.getCpuSpeed() == null ? 0 : instance.getCpuSpeed();

        if (cpu == null || cpu == 0) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("CPU cores [%s] is not valid for importing VM [%s].", cpu, instance.getName()));
        }
        if (memory == null || memory == 0) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Memory [%s] is not valid for importing VM [%s].", memory, instance.getName()));
        }

        if (serviceOffering.isDynamic()) {
            if (details.containsKey(VmDetailConstants.CPU_SPEED)) {
                try {
                    cpuSpeed = Integer.parseInt(details.get(VmDetailConstants.CPU_SPEED));
                } catch (Exception e) {
                    logger.error(String.format("Failed to get CPU speed for importing VM [%s] due to [%s].", instance.getName(), e.getMessage()), e);
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
            if (hypervisorType == Hypervisor.HypervisorType.VMware && cpuSpeed != null && cpuSpeed > 0 && !cpuSpeed.equals(serviceOffering.getSpeed()) && !instance.getPowerState().equals(UnmanagedInstanceTO.PowerState.PowerOff)) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Service offering (%s) %dMHz CPU speed does not match VM CPU speed %dMHz and VM is not in powered off state (Power state: %s)", serviceOffering.getUuid(), serviceOffering.getSpeed(), cpuSpeed, instance.getPowerState()));
            }
        }
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
                String searchPoolParam = StringUtils.isNotBlank(dsPath) ? dsPath : dsName;
                if (StringUtils.contains(pool.getPath(), searchPoolParam)) {
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

    private Pair<UnmanagedInstanceTO.Disk, List<UnmanagedInstanceTO.Disk>> getRootAndDataDisks(
            List<UnmanagedInstanceTO.Disk> disks,
            final Map<String, Long> dataDiskOfferingMap) {
        UnmanagedInstanceTO.Disk rootDisk = null;
        List<UnmanagedInstanceTO.Disk> dataDisks = new ArrayList<>();

        Set<String> callerDiskIds = dataDiskOfferingMap.keySet();
        if (callerDiskIds.size() != disks.size() - 1) {
            String msg = String.format("VM has total %d disks for which %d disk offering mappings provided. %d disks need a disk offering for import", disks.size(), callerDiskIds.size(), disks.size() - 1);
            logger.error(String.format("%s. %s parameter can be used to provide disk offerings for the disks", msg, ApiConstants.DATADISK_OFFERING_LIST));
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
                DiskOffering diskOffering = diskOfferingDao.findById(dataDiskOfferingMap.getOrDefault(disk.getDiskId(), null));
                if ((disk.getCapacity() == null || disk.getCapacity() <= 0) && diskOffering != null) {
                    disk.setCapacity(diskOffering.getDiskSize());
                }
            }
        }
        if (diskIdsWithoutOffering.size() > 1 || rootDisk == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("VM has total %d disks, disk offering mapping not provided for %d disks. Disk IDs that may need a disk offering - %s", disks.size(), diskIdsWithoutOffering.size() - 1, String.join(", ", diskIdsWithoutOffering)));
        }

        return new Pair<>(rootDisk, dataDisks);
    }

    private void checkUnmanagedDiskAndOfferingForImport(String instanceName, UnmanagedInstanceTO.Disk disk, DiskOffering diskOffering, ServiceOffering serviceOffering, final Account owner, final DataCenter zone, final Cluster cluster, final boolean migrateAllowed)
            throws ServerApiException, PermissionDeniedException, ResourceAllocationException {
        if (serviceOffering == null && diskOffering == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Disk offering for disk ID [%s] not found during VM [%s] import.", disk.getDiskId(), instanceName));
        }
        if (diskOffering != null) {
            accountService.checkAccess(owner, diskOffering, zone);
        }
        resourceLimitService.checkVolumeResourceLimit(owner, true, null, diskOffering);
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
    }

    private void checkUnmanagedDiskAndOfferingForImport(String intanceName, List<UnmanagedInstanceTO.Disk> disks, final Map<String, Long> diskOfferingMap, final Account owner, final DataCenter zone, final Cluster cluster, final boolean migrateAllowed)
            throws ServerApiException, PermissionDeniedException, ResourceAllocationException {
        String diskController = null;
        for (UnmanagedInstanceTO.Disk disk : disks) {
            if (disk == null) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Unable to retrieve disk details for VM [%s].", intanceName));
            }
            if (!diskOfferingMap.containsKey(disk.getDiskId())) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Disk offering for disk ID [%s] not found during VM import.", disk.getDiskId()));
            }
            if (StringUtils.isEmpty(diskController)) {
                diskController = disk.getController();
            } else {
                if (!diskController.equals(disk.getController())) {
                    throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Multiple data disk controllers of different type (%s, %s) are not supported for import. Please make sure that all data disk controllers are of the same type", diskController, disk.getController()));
                }
            }
            checkUnmanagedDiskAndOfferingForImport(intanceName, disk, diskOfferingDao.findById(diskOfferingMap.get(disk.getDiskId())), null, owner, zone, cluster, migrateAllowed);
        }
    }

    private void checkUnmanagedNicAndNetworkForImport(String instanceName, UnmanagedInstanceTO.Nic nic, Network network, final DataCenter zone, final Account owner, final boolean autoAssign, Hypervisor.HypervisorType hypervisorType) throws ServerApiException {
        basicNetworkChecks(instanceName, nic, network);
        if (network.getDataCenterId() != zone.getId()) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Network(ID: %s) for nic(ID: %s) belongs to a different zone than VM to be imported", network.getUuid(), nic.getNicId()));
        }
        networkModel.checkNetworkPermissions(owner, network);
        if (!autoAssign && network.getGuestType().equals(Network.GuestType.Isolated)) {
            return;
        }
        checksOnlyNeededForVmware(nic, network, hypervisorType);
    }

    private void checksOnlyNeededForVmware(UnmanagedInstanceTO.Nic nic, Network network, final Hypervisor.HypervisorType hypervisorType) {
        if (hypervisorType == Hypervisor.HypervisorType.VMware) {
            String networkBroadcastUri = network.getBroadcastUri() == null ? null : network.getBroadcastUri().toString();
            if (nic.getVlan() != null && nic.getVlan() != 0 && nic.getPvlan() == null &&
                    (StringUtils.isEmpty(networkBroadcastUri) ||
                            !networkBroadcastUri.equals(String.format("vlan://%d", nic.getVlan())))) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("VLAN of network(ID: %s) %s is found different from the VLAN of nic(ID: %s) vlan://%d during VM import", network.getUuid(), networkBroadcastUri, nic.getNicId(), nic.getVlan()));
            }
            String pvLanType = nic.getPvlanType() == null ? "" : nic.getPvlanType().toLowerCase().substring(0, 1);
            if (nic.getVlan() != null && nic.getVlan() != 0 && nic.getPvlan() != null && nic.getPvlan() != 0 &&
                    (StringUtils.isEmpty(networkBroadcastUri) || !String.format("pvlan://%d-%s%d", nic.getVlan(), pvLanType, nic.getPvlan()).equals(networkBroadcastUri))) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("PVLAN of network(ID: %s) %s is found different from the VLAN of nic(ID: %s) pvlan://%d-%s%d during VM import", network.getUuid(), networkBroadcastUri, nic.getNicId(), nic.getVlan(), pvLanType, nic.getPvlan()));
            }
        }
    }

    private void basicNetworkChecks(String instanceName, UnmanagedInstanceTO.Nic nic, Network network) {
        if (nic == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Unable to retrieve the NIC details used by VM [%s] from VMware. Please check if this VM have NICs in VMWare.", instanceName));
        }
        if (network == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Network for nic ID: %s not found during VM import.", nic.getNicId()));
        }
    }

    private void checkUnmanagedNicAndNetworkHostnameForImport(String instanceName, UnmanagedInstanceTO.Nic nic, Network network, final String hostName) throws ServerApiException {
        basicNetworkChecks(instanceName, nic, network);
        // Check for duplicate hostname in network, get all vms hostNames in the network
        List<String> hostNames = vmDao.listDistinctHostNames(network.getId());
        if (CollectionUtils.isNotEmpty(hostNames) && hostNames.contains(hostName)) {
            throw new InvalidParameterValueException(String.format("VM with Name [%s] already exists in the network [%s] domain [%s]. Cannot import another VM with the same name. Please try again with a different name.", hostName, network, network.getNetworkDomain()));
        }
    }

    private void checkUnmanagedNicIpAndNetworkForImport(String instanceName, UnmanagedInstanceTO.Nic nic, Network network, final Network.IpAddresses ipAddresses) throws ServerApiException {
        basicNetworkChecks(instanceName, nic, network);
        // Check IP is assigned for non L2 networks
        if (!network.getGuestType().equals(Network.GuestType.L2) && (ipAddresses == null || StringUtils.isEmpty(ipAddresses.getIp4Address()))) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("NIC(ID: %s) needs a valid IP address for it to be associated with network(ID: %s). %s parameter of API can be used for this", nic.getNicId(), network.getUuid(), ApiConstants.NIC_IP_ADDRESS_LIST));
        }
        // If network is non L2, IP v4 is assigned and not set to auto-assign, check it is available for network
        if (!network.getGuestType().equals(Network.GuestType.L2) && ipAddresses != null && StringUtils.isNotEmpty(ipAddresses.getIp4Address()) && !ipAddresses.getIp4Address().equals("auto")) {
            Set<Long> ips = networkModel.getAvailableIps(network, ipAddresses.getIp4Address());
            if (CollectionUtils.isEmpty(ips) || !ips.contains(NetUtils.ip2Long(ipAddresses.getIp4Address()))) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("IP address %s for NIC(ID: %s) is not available in network(ID: %s)", ipAddresses.getIp4Address(), nic.getNicId(), network.getUuid()));
            }
        }
    }

    private Map<String, Long> getUnmanagedNicNetworkMap(String instanceName, List<UnmanagedInstanceTO.Nic> nics, final Map<String, Long> callerNicNetworkMap,
                                                        final Map<String, Network.IpAddresses> callerNicIpAddressMap, final DataCenter zone, final String hostName,
                                                        final Account owner, Hypervisor.HypervisorType hypervisorType) throws ServerApiException {
        Map<String, Long> nicNetworkMap = new HashMap<>();
        String nicAdapter = null;
        for (int i = 0; i < nics.size(); i++) {
            UnmanagedInstanceTO.Nic nic = nics.get(i);
            if (StringUtils.isEmpty(nicAdapter)) {
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
                            checkUnmanagedNicAndNetworkForImport(instanceName, nic, networkVO, zone, owner, true, hypervisorType);
                            network = networkVO;
                        } catch (Exception e) {
                            logger.error(String.format("Error when checking NIC [%s] of unmanaged instance to import due to [%s].", nic.getNicId(), e.getMessage()), e);
                        }
                        if (network != null) {
                            checkUnmanagedNicAndNetworkHostnameForImport(instanceName, nic, network, hostName);
                            checkUnmanagedNicIpAndNetworkForImport(instanceName, nic, network, ipAddresses);
                            break;
                        }
                    }
                }
            } else {
                network = networkDao.findById(callerNicNetworkMap.get(nic.getNicId()));
                boolean autoImport = false;
                if (hypervisorType == Hypervisor.HypervisorType.KVM) {
                    autoImport = ipAddresses != null && ipAddresses.getIp4Address() != null && ipAddresses.getIp4Address().equalsIgnoreCase("auto");
                }
                checkUnmanagedNicAndNetworkForImport(instanceName, nic, network, zone, owner, autoImport, hypervisorType);
                checkUnmanagedNicAndNetworkHostnameForImport(instanceName, nic, network, hostName);
                checkUnmanagedNicIpAndNetworkForImport(instanceName, nic, network, ipAddresses);
            }
            if (network == null) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Suitable network for nic(ID: %s) not found during VM import", nic.getNicId()));
            }
            nicNetworkMap.put(nic.getNicId(), network.getId());
        }
        return nicNetworkMap;
    }

    private Pair<DiskProfile, StoragePool> importExternalDisk(UnmanagedInstanceTO.Disk disk, VirtualMachine vm, DeployDestination dest, DiskOffering diskOffering,
                                                      Volume.Type type, VirtualMachineTemplate template,Long deviceId, String remoteUrl, String username, String password,
                                                      String tmpPath, DiskProfile diskProfile) {
        final String path = StringUtils.isEmpty(disk.getDatastorePath()) ? disk.getImagePath() : disk.getDatastorePath();
        String chainInfo = disk.getChainInfo();
        if (StringUtils.isEmpty(chainInfo)) {
            VirtualMachineDiskInfo diskInfo = new VirtualMachineDiskInfo();
            diskInfo.setDiskDeviceBusName(String.format("%s%d:%d", disk.getController(), disk.getControllerUnit(), disk.getPosition()));
            diskInfo.setDiskChain(new String[]{disk.getImagePath()});
            chainInfo = gson.toJson(diskInfo);
        }
        Map<Volume, StoragePool> storage = dest.getStorageForDisks();
        Volume volume = volumeDao.findById(diskProfile.getVolumeId());
        StoragePool storagePool = storage.get(volume);
        CopyRemoteVolumeCommand copyRemoteVolumeCommand = new CopyRemoteVolumeCommand();
        copyRemoteVolumeCommand.setRemoteIp(remoteUrl);
        copyRemoteVolumeCommand.setUsername(username);
        copyRemoteVolumeCommand.setPassword(password);
        copyRemoteVolumeCommand.setSrcFile(path);
        StorageFilerTO storageTO = new StorageFilerTO(storagePool);
        copyRemoteVolumeCommand.setStorageFilerTO(storageTO);
        if(tmpPath == null || tmpPath.length() < 1) {
            tmpPath = "/tmp/";
        } else {
            // Add / if path doesn't end with /
            if(tmpPath.charAt(tmpPath.length() - 1) != '/') {
                tmpPath += "/";
            }
        }
        copyRemoteVolumeCommand.setTempPath(tmpPath);
        int copyTimeout = UnmanagedVMsManager.RemoteKvmInstanceDisksCopyTimeout.value();
        if (copyTimeout <= 0) {
            copyTimeout = Integer.valueOf(UnmanagedVMsManager.RemoteKvmInstanceDisksCopyTimeout.defaultValue());
        }
        int copyTimeoutInSecs = copyTimeout * 60;
        copyRemoteVolumeCommand.setWait(copyTimeoutInSecs);
        logger.error(String.format("Initiating copy remote volume %s from %s, timeout %d secs", path, remoteUrl, copyTimeoutInSecs));
        Answer answer = agentManager.easySend(dest.getHost().getId(), copyRemoteVolumeCommand);
        if (!(answer instanceof CopyRemoteVolumeAnswer)) {
            throw new CloudRuntimeException("Error while copying volume of remote instance: " + answer.getDetails());
        }
        CopyRemoteVolumeAnswer copyRemoteVolumeAnswer = (CopyRemoteVolumeAnswer) answer;
        if(!copyRemoteVolumeAnswer.getResult()) {
            throw new CloudRuntimeException("Unable to copy volume of remote instance");
        }
        diskProfile.setSize(copyRemoteVolumeAnswer.getSize());
        DiskProfile profile = volumeManager.updateImportedVolume(type, diskOffering, vm, template, deviceId,
                storagePool.getId(), copyRemoteVolumeAnswer.getFilename(), chainInfo, diskProfile);

        return new Pair<>(profile, storagePool);
    }

    private Pair<DiskProfile, StoragePool> importKVMLocalDisk(VirtualMachine vm, DiskOffering diskOffering,
                                                              Volume.Type type, VirtualMachineTemplate template,
                                                              Long deviceId, Long hostId, String diskPath, DiskProfile diskProfile) {
        List<StoragePoolVO> storagePools = primaryDataStoreDao.findLocalStoragePoolsByHostAndTags(hostId, null);
        if(storagePools.size() < 1) {
            throw new CloudRuntimeException("Local Storage not found for host");
        }

        StoragePool storagePool = storagePools.get(0);

        DiskProfile profile = volumeManager.updateImportedVolume(type, diskOffering, vm, template, deviceId,
                storagePool.getId(), diskPath, null, diskProfile);

        return new Pair<>(profile, storagePool);
    }

    private Pair<DiskProfile, StoragePool> importKVMSharedDisk(VirtualMachine vm, DiskOffering diskOffering,
                                                              Volume.Type type, VirtualMachineTemplate template,
                                                              Long deviceId, Long poolId, String diskPath, DiskProfile diskProfile) {
        StoragePool storagePool = primaryDataStoreDao.findById(poolId);

        DiskProfile profile = volumeManager.updateImportedVolume(type, diskOffering, vm, template, deviceId,
                poolId, diskPath, null, diskProfile);

        return new Pair<>(profile, storagePool);
    }

    private Pair<DiskProfile, StoragePool> importDisk(UnmanagedInstanceTO.Disk disk, VirtualMachine vm, Cluster cluster, DiskOffering diskOffering,
                                                      Volume.Type type, String name, Long diskSize, Long minIops, Long maxIops, VirtualMachineTemplate template,
                                                      Account owner, Long deviceId) {
        final DataCenter zone = dataCenterDao.findById(vm.getDataCenterId());
        final String path = StringUtils.isEmpty(disk.getFileBaseName()) ? disk.getImagePath() : disk.getFileBaseName();
        String chainInfo = disk.getChainInfo();
        if (vm.getHypervisorType() == Hypervisor.HypervisorType.VMware && StringUtils.isEmpty(chainInfo)) {
            VirtualMachineDiskInfo diskInfo = new VirtualMachineDiskInfo();
            diskInfo.setDiskDeviceBusName(String.format("%s%d:%d", disk.getController(), disk.getControllerUnit(), disk.getPosition()));
            diskInfo.setDiskChain(new String[]{disk.getImagePath()});
            chainInfo = gson.toJson(diskInfo);
        }
        StoragePool storagePool = getStoragePool(disk, zone, cluster);
        DiskProfile profile = volumeManager.importVolume(type, name, diskOffering, diskSize,
                minIops, maxIops, vm.getDataCenterId(), vm.getHypervisorType(), vm, template, owner, deviceId, storagePool.getId(), path, chainInfo);

        return new Pair<DiskProfile, StoragePool>(profile, storagePool);
    }

    private NicProfile importNic(UnmanagedInstanceTO.Nic nic, VirtualMachine vm, Network network, Network.IpAddresses ipAddresses, int deviceId, boolean isDefaultNic, boolean forced) throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException {
        DataCenterVO dataCenterVO = dataCenterDao.findById(network.getDataCenterId());
        Pair<NicProfile, Integer> result = networkOrchestrationService.importNic(nic.getMacAddress(), deviceId, network, isDefaultNic, vm, ipAddresses, dataCenterVO, forced);
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
            logger.error(String.format("Unable to release NICs for unsuccessful import unmanaged VM: %s", userVm.getInstanceName()), e);
            nicDao.removeNicsForInstance(userVm.getId());
        }
        // Remove vm
        vmDao.remove(userVm.getId());
    }

    private UserVm migrateImportedVM(HostVO sourceHost, VirtualMachineTemplate template, ServiceOfferingVO serviceOffering, UserVm userVm, final Account owner, List<Pair<DiskProfile, StoragePool>> diskProfileStoragePoolList) {
        UserVm vm = userVm;
        if (vm == null) {
            logger.error(String.format("Failed to check migrations need during VM import"));
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Failed to check migrations need during VM import"));
        }
        if (sourceHost == null || serviceOffering == null || diskProfileStoragePoolList == null) {
            logger.error(String.format("Failed to check migrations need during import, VM: %s", userVm.getInstanceName()));
            cleanupFailedImportVM(vm);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Failed to check migrations need during import, VM: %s", userVm.getInstanceName()));
        }
        if (!hostSupportsServiceOfferingAndTemplate(sourceHost, serviceOffering, template)) {
            logger.debug(String.format("VM %s needs to be migrated", vm.getUuid()));
            final VirtualMachineProfile profile = new VirtualMachineProfileImpl(vm, template, serviceOffering, owner, null);
            profile.setServiceOffering(serviceOfferingDao.findById(vm.getId(), serviceOffering.getId()));
            DeploymentPlanner.ExcludeList excludeList = new DeploymentPlanner.ExcludeList();
            excludeList.addHost(sourceHost.getId());
            final DataCenterDeployment plan = new DataCenterDeployment(sourceHost.getDataCenterId(), sourceHost.getPodId(), sourceHost.getClusterId(), null, null, null);
            DeployDestination dest = null;
            try {
                dest = deploymentPlanningManager.planDeployment(profile, plan, excludeList, null);
            } catch (Exception e) {
                String errorMsg = String.format("VM import failed for Unmanaged VM [%s] during VM migration, cannot find deployment destination due to [%s].", vm.getInstanceName(), e.getMessage());
                logger.warn(errorMsg, e);
                cleanupFailedImportVM(vm);
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, errorMsg);
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
                String errorMsg = String.format("VM import failed for Unmanaged VM [%s] during VM migration due to [%s].", vm.getInstanceName(), e.getMessage());
                logger.error(errorMsg, e);
                cleanupFailedImportVM(vm);
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, errorMsg);
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
            if (poolSupportsOfferings) {
                continue;
            }
            logger.debug(String.format("Volume %s needs to be migrated", volumeVO.getUuid()));
            Pair<List<? extends StoragePool>, List<? extends StoragePool>> poolsPair = managementService.listStoragePoolsForSystemMigrationOfVolume(profile.getVolumeId(), null, null, null, null, false, true);
            if (CollectionUtils.isEmpty(poolsPair.first()) && CollectionUtils.isEmpty(poolsPair.second())) {
                cleanupFailedImportVM(vm);
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("VM import failed for unmanaged vm: %s during volume ID: %s migration as no suitable pool(s) found", userVm.getInstanceName(), volumeVO.getUuid()));
            }
            List<? extends StoragePool> storagePools = poolsPair.second();
            StoragePool storagePool = null;
            if (CollectionUtils.isNotEmpty(storagePools)) {
                for (StoragePool pool : storagePools) {
                    if (diskProfileStoragePool.second().getId() != pool.getId() &&
                            storagePoolSupportsDiskOffering(pool, dOffering)
                    ) {
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
                            storagePoolSupportsDiskOffering(pool, dOffering)
                    ) {
                        storagePool = pool;
                        break;
                    }
                }
            }
            if (storagePool == null) {
                cleanupFailedImportVM(vm);
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("VM import failed for unmanaged vm: %s during volume ID: %s migration as no suitable pool found", userVm.getInstanceName(), volumeVO.getUuid()));
            } else {
                logger.debug(String.format("Found storage pool %s(%s) for migrating the volume %s to", storagePool.getName(), storagePool.getUuid(), volumeVO.getUuid()));
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
                    logger.error(msg);
                    throw new CloudRuntimeException(msg);
                }
            } catch (Exception e) {
                logger.error(String.format("VM import failed for unmanaged vm: %s during volume migration", vm.getInstanceName()), e);
                cleanupFailedImportVM(vm);
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("VM import failed for unmanaged vm: %s during volume migration. %s", userVm.getInstanceName(), StringUtils.defaultString(e.getMessage())));
            }
        }
        return userVm;
    }

    private void publishVMUsageUpdateResourceCount(final UserVm userVm, ServiceOfferingVO serviceOfferingVO, VirtualMachineTemplate templateVO) {
        if (userVm == null || serviceOfferingVO == null) {
            logger.error(String.format("Failed to publish usage records during VM import because VM [%s] or ServiceOffering [%s] is null.", userVm, serviceOfferingVO));
            cleanupFailedImportVM(userVm);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "VM import failed for Unmanaged VM during publishing Usage Records.");
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
            logger.error(String.format("Failed to publish usage records during VM import for unmanaged VM [%s] due to [%s].", userVm.getInstanceName(), e.getMessage()), e);
            cleanupFailedImportVM(userVm);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("VM import failed for unmanaged vm %s during publishing usage records", userVm.getInstanceName()));
        }
        resourceLimitService.incrementVmResourceCount(userVm.getAccountId(), userVm.isDisplayVm(), serviceOfferingVO, templateVO);
        // Save usage event and update resource count for user vm volumes
        List<VolumeVO> volumes = volumeDao.findByInstance(userVm.getId());
        for (VolumeVO volume : volumes) {
            try {
                UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VOLUME_CREATE, volume.getAccountId(), volume.getDataCenterId(), volume.getId(), volume.getName(), volume.getDiskOfferingId(), null, volume.getSize(),
                        Volume.class.getName(), volume.getUuid(), volume.isDisplayVolume());
            } catch (Exception e) {
                logger.error(String.format("Failed to publish volume ID: %s usage records during VM import", volume.getUuid()), e);
            }
            resourceLimitService.incrementVolumeResourceCount(userVm.getAccountId(), volume.isDisplayVolume(),
                    volume.getSize(), diskOfferingDao.findById(volume.getDiskOfferingId()));
        }

        List<NicVO> nics = nicDao.listByVmId(userVm.getId());
        for (NicVO nic : nics) {
            try {
                NetworkVO network = networkDao.findById(nic.getNetworkId());
                UsageEventUtils.publishUsageEvent(EventTypes.EVENT_NETWORK_OFFERING_ASSIGN, userVm.getAccountId(), userVm.getDataCenterId(), userVm.getId(),
                        Long.toString(nic.getId()), network.getNetworkOfferingId(), null, 1L, VirtualMachine.class.getName(), userVm.getUuid(), userVm.isDisplay());
            } catch (Exception e) {
                logger.error(String.format("Failed to publish network usage records during VM import. %s", StringUtils.defaultString(e.getMessage())));
            }
        }
    }

    protected void checkUnmanagedDiskLimits(Account account, UnmanagedInstanceTO.Disk rootDisk, ServiceOffering serviceOffering,
            List<UnmanagedInstanceTO.Disk> dataDisks, Map<String, Long> dataDiskOfferingMap) throws ResourceAllocationException {
        Long totalVolumes = 0L;
        Long totalVolumesSize = 0L;
        List<UnmanagedInstanceTO.Disk> disks = new ArrayList<>();
        disks.add(rootDisk);
        disks.addAll(dataDisks);
        Map<String, Long> diskOfferingMap = new HashMap<>(dataDiskOfferingMap);
        diskOfferingMap.put(rootDisk.getDiskId(), serviceOffering.getDiskOfferingId());
        Map<Long, Long> diskOfferingVolumeCountMap = new HashMap<>();
        Map<Long, Long> diskOfferingSizeMap = new HashMap<>();
        for (UnmanagedInstanceTO.Disk disk : disks) {
            totalVolumes++;
            totalVolumesSize += disk.getCapacity();
            Long diskOfferingId = diskOfferingMap.get(disk.getDiskId());
            if (diskOfferingVolumeCountMap.containsKey(diskOfferingId)) {
                diskOfferingVolumeCountMap.put(diskOfferingId, diskOfferingVolumeCountMap.get(diskOfferingId) + 1);
                diskOfferingSizeMap.put(diskOfferingId, diskOfferingSizeMap.get(diskOfferingId) + disk.getCapacity());
            } else {
                diskOfferingVolumeCountMap.put(diskOfferingId, 1L);
                diskOfferingSizeMap.put(diskOfferingId, disk.getCapacity());
            }
        }
        resourceLimitService.checkResourceLimit(account, Resource.ResourceType.volume, totalVolumes);
        resourceLimitService.checkResourceLimit(account, Resource.ResourceType.primary_storage, totalVolumesSize);
        for (Long diskOfferingId : diskOfferingVolumeCountMap.keySet()) {
            List<String> tags = resourceLimitService.getResourceLimitStorageTags(diskOfferingDao.findById(diskOfferingId));
            for (String tag : tags) {
                resourceLimitService.checkResourceLimitWithTag(account, Resource.ResourceType.volume, tag, diskOfferingVolumeCountMap.get(diskOfferingId));
                resourceLimitService.checkResourceLimitWithTag(account, Resource.ResourceType.primary_storage, tag, diskOfferingSizeMap.get(diskOfferingId));
            }
        }
    }

    private UserVm importVirtualMachineInternal(final UnmanagedInstanceTO unmanagedInstance, final String instanceName, final DataCenter zone, final Cluster cluster, final HostVO host,
                                                final VirtualMachineTemplate template, final String displayName, final String hostName, final Account caller, final Account owner, final Long userId,
                                                final ServiceOfferingVO serviceOffering, final Map<String, Long> dataDiskOfferingMap,
                                                final Map<String, Long> nicNetworkMap, final Map<String, Network.IpAddresses> callerNicIpAddressMap,
                                                final Map<String, String> details, final boolean migrateAllowed, final boolean forced, final boolean isImportUnmanagedFromSameHypervisor) {
        logger.debug(LogUtils.logGsonWithoutException("Trying to import VM [%s] with name [%s], in zone [%s], cluster [%s], and host [%s], using template [%s], service offering [%s], disks map [%s], NICs map [%s] and details [%s].",
                unmanagedInstance, instanceName, zone, cluster, host, template, serviceOffering, dataDiskOfferingMap, nicNetworkMap, details));
        UserVm userVm = null;
        ServiceOfferingVO validatedServiceOffering = null;
        try {
            validatedServiceOffering = getUnmanagedInstanceServiceOffering(unmanagedInstance, serviceOffering, owner, zone, details, cluster.getHypervisorType());
        } catch (Exception e) {
            String errorMsg = String.format("Failed to import Unmanaged VM [%s] because the service offering [%s] is not compatible due to [%s].", unmanagedInstance.getName(), serviceOffering.getUuid(), StringUtils.defaultIfEmpty(e.getMessage(), ""));
            logger.error(errorMsg, e);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, errorMsg);
        }

        String internalCSName = unmanagedInstance.getInternalCSName();
        if (StringUtils.isEmpty(internalCSName)) {
            internalCSName = instanceName;
        }
        Map<String, String> allDetails = new HashMap<>(details);
        if (validatedServiceOffering.isDynamic()) {
            allDetails.put(VmDetailConstants.CPU_NUMBER, String.valueOf(validatedServiceOffering.getCpu()));
            allDetails.put(VmDetailConstants.MEMORY, String.valueOf(validatedServiceOffering.getRamSize()));
            if (serviceOffering.getSpeed() == null) {
                allDetails.put(VmDetailConstants.CPU_SPEED, String.valueOf(validatedServiceOffering.getSpeed()));
            }
        }

        if (!migrateAllowed && host != null && !hostSupportsServiceOfferingAndTemplate(host, validatedServiceOffering, template)) {
            throw new InvalidParameterValueException(String.format("Service offering: %s or template: %s is not compatible with host: %s of unmanaged VM: %s", serviceOffering.getUuid(), template.getUuid(), host.getUuid(), instanceName));
        }
        // Check disks and supplied disk offerings
        List<UnmanagedInstanceTO.Disk> unmanagedInstanceDisks = unmanagedInstance.getDisks();
        if (CollectionUtils.isEmpty(unmanagedInstanceDisks)) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("No attached disks found for the unmanaged VM: %s", instanceName));
        }
        Pair<UnmanagedInstanceTO.Disk, List<UnmanagedInstanceTO.Disk>> rootAndDataDisksPair = getRootAndDataDisks(unmanagedInstanceDisks, dataDiskOfferingMap);
        final UnmanagedInstanceTO.Disk rootDisk = rootAndDataDisksPair.first();
        final List<UnmanagedInstanceTO.Disk> dataDisks = rootAndDataDisksPair.second();
        if (rootDisk == null || StringUtils.isEmpty(rootDisk.getController())) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("VM import failed. Unable to retrieve root disk details for VM: %s ", instanceName));
        }
        if (cluster.getHypervisorType() == Hypervisor.HypervisorType.KVM) {
            Long rootDiskOfferingId = validatedServiceOffering.getDiskOfferingId();
            DiskOffering rootDiskOffering = diskOfferingDao.findById(rootDiskOfferingId);
            if ((rootDisk.getCapacity() == null || rootDisk.getCapacity() <= 0) && rootDiskOffering != null) {
                rootDisk.setCapacity(rootDiskOffering.getDiskSize());
            }
        }
        allDetails.put(VmDetailConstants.ROOT_DISK_CONTROLLER, rootDisk.getController());
        if (cluster.getHypervisorType() == Hypervisor.HypervisorType.KVM && isImportUnmanagedFromSameHypervisor) {
            long size = Double.valueOf(Math.ceil((double)rootDisk.getCapacity() / Resource.ResourceType.bytesToGiB)).longValue();
            allDetails.put(VmDetailConstants.ROOT_DISK_SIZE, String.valueOf(size));
        }

        try {
            checkUnmanagedDiskAndOfferingForImport(unmanagedInstance.getName(), rootDisk, null, validatedServiceOffering, owner, zone, cluster, migrateAllowed);
            if (CollectionUtils.isNotEmpty(dataDisks)) { // Data disk(s) present
                checkUnmanagedDiskAndOfferingForImport(unmanagedInstance.getName(), dataDisks, dataDiskOfferingMap, owner, zone, cluster, migrateAllowed);
                allDetails.put(VmDetailConstants.DATA_DISK_CONTROLLER, dataDisks.get(0).getController());
            }
            checkUnmanagedDiskLimits(owner, rootDisk, serviceOffering, dataDisks, dataDiskOfferingMap);
        } catch (ResourceAllocationException e) {
            logger.error(String.format("Volume resource allocation error for owner: %s", owner.getUuid()), e);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Volume resource allocation error for owner: %s. %s", owner.getUuid(), StringUtils.defaultString(e.getMessage())));
        }
        // Check NICs and supplied networks
        Map<String, Network.IpAddresses> nicIpAddressMap = getNicIpAddresses(unmanagedInstance.getNics(), callerNicIpAddressMap);
        Map<String, Long> allNicNetworkMap = getUnmanagedNicNetworkMap(unmanagedInstance.getName(), unmanagedInstance.getNics(), nicNetworkMap, nicIpAddressMap, zone, hostName, owner, cluster.getHypervisorType());
        if (!CollectionUtils.isEmpty(unmanagedInstance.getNics())) {
            allDetails.put(VmDetailConstants.NIC_ADAPTER, unmanagedInstance.getNics().get(0).getAdapterType());
        }

        if (StringUtils.isNotEmpty(unmanagedInstance.getVncPassword())) {
            allDetails.put(VmDetailConstants.KVM_VNC_PASSWORD, unmanagedInstance.getVncPassword());
        }

        VirtualMachine.PowerState powerState = VirtualMachine.PowerState.PowerOff;
        if (unmanagedInstance.getPowerState().equals(UnmanagedInstanceTO.PowerState.PowerOn)) {
            powerState = VirtualMachine.PowerState.PowerOn;
        }

        try {
            userVm = userVmManager.importVM(zone, host, template, internalCSName, displayName, owner,
                    null, caller, true, null, owner.getAccountId(), userId,
                    validatedServiceOffering, null, hostName,
                    cluster.getHypervisorType(), allDetails, powerState, null);
        } catch (InsufficientCapacityException ice) {
            String errorMsg = String.format("Failed to import VM [%s] due to [%s].", instanceName, ice.getMessage());
            logger.error(errorMsg, ice);
            throw new ServerApiException(ApiErrorCode.INSUFFICIENT_CAPACITY_ERROR, errorMsg);
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
            if (details.containsKey(MIN_IOPS)) {
                minIops = Long.parseLong(details.get(MIN_IOPS));
            }
            Long maxIops = null;
            if (details.containsKey(MAX_IOPS)) {
                maxIops = Long.parseLong(details.get(MAX_IOPS));
            }
            DiskOfferingVO diskOffering = diskOfferingDao.findById(serviceOffering.getDiskOfferingId());
            diskProfileStoragePoolList.add(importDisk(rootDisk, userVm, cluster, diskOffering, Volume.Type.ROOT, String.format("ROOT-%d", userVm.getId()),
                    rootDisk.getCapacity(), minIops, maxIops, template, owner, null));
            long deviceId = 1L;
            for (UnmanagedInstanceTO.Disk disk : dataDisks) {
                if (disk.getCapacity() == null || disk.getCapacity() == 0) {
                    throw new InvalidParameterValueException(String.format("Disk ID: %s size is invalid", rootDisk.getDiskId()));
                }
                DiskOffering offering = diskOfferingDao.findById(dataDiskOfferingMap.get(disk.getDiskId()));
                diskProfileStoragePoolList.add(importDisk(disk, userVm, cluster, offering, Volume.Type.DATADISK, String.format("DATA-%d-%s", userVm.getId(), disk.getDiskId()),
                        disk.getCapacity(), offering.getMinIops(), offering.getMaxIops(),
                        template, owner, deviceId));
                deviceId++;
            }
        } catch (Exception e) {
            logger.error(String.format("Failed to import volumes while importing vm: %s", instanceName), e);
            cleanupFailedImportVM(userVm);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Failed to import volumes while importing vm: %s. %s", instanceName, StringUtils.defaultString(e.getMessage())));
        }
        try {
            int nicIndex = 0;
            for (UnmanagedInstanceTO.Nic nic : unmanagedInstance.getNics()) {
                Network network = networkDao.findById(allNicNetworkMap.get(nic.getNicId()));
                Network.IpAddresses ipAddresses = nicIpAddressMap.get(nic.getNicId());
                importNic(nic, userVm, network, ipAddresses, nicIndex, nicIndex == 0, forced);
                nicIndex++;
            }
        } catch (Exception e) {
            logger.error(String.format("Failed to import NICs while importing vm: %s", instanceName), e);
            cleanupFailedImportVM(userVm);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Failed to import NICs while importing vm: %s. %s", instanceName, StringUtils.defaultString(e.getMessage())));
        }
        if (migrateAllowed) {
            userVm = migrateImportedVM(host, template, validatedServiceOffering, userVm, owner, diskProfileStoragePoolList);
        }
        publishVMUsageUpdateResourceCount(userVm, validatedServiceOffering, template);
        return userVm;
    }

    private HashMap<String, UnmanagedInstanceTO> getUnmanagedInstancesForHost(HostVO host, String instanceName, List<String> managedVms) {
        HashMap<String, UnmanagedInstanceTO> unmanagedInstances = new HashMap<>();
        if (host.isInMaintenanceStates()) {
            return unmanagedInstances;
        }

        GetUnmanagedInstancesCommand command = new GetUnmanagedInstancesCommand();
        command.setInstanceName(instanceName);
        command.setManagedInstancesNames(managedVms);
        Answer answer = agentManager.easySend(host.getId(), command);
        if (!(answer instanceof GetUnmanagedInstancesAnswer) || !answer.getResult()) {
            return unmanagedInstances;
        }
        GetUnmanagedInstancesAnswer unmanagedInstancesAnswer = (GetUnmanagedInstancesAnswer) answer;
        unmanagedInstances = unmanagedInstancesAnswer.getUnmanagedInstances();
        return unmanagedInstances;
    }

    protected Cluster basicAccessChecks(Long clusterId) {
        final Account caller = CallContext.current().getCallingAccount();
        if (caller.getType() != Account.Type.ADMIN) {
            throw new PermissionDeniedException(String.format("Cannot perform this operation, caller account [%s] is not ROOT Admin.", caller.getUuid()));
        }
        if (clusterId == null) {
            throw new InvalidParameterValueException("Cluster ID cannot be null.");
        }
        final Cluster cluster = clusterDao.findById(clusterId);
        if (cluster == null) {
            throw new InvalidParameterValueException(String.format("Cluster with ID [%d] cannot be found.", clusterId));
        }

        if (!importUnmanagedInstancesSupportedHypervisors.contains(cluster.getHypervisorType())) {
            throw new InvalidParameterValueException(String.format("VM import is currently not supported for hypervisor [%s].", cluster.getHypervisorType().toString()));
        }
        return cluster;
    }

    @Override
    public ListResponse<UnmanagedInstanceResponse> listUnmanagedInstances(ListUnmanagedInstancesCmd cmd) {
        Long clusterId = cmd.getClusterId();
        Cluster cluster = basicAccessChecks(clusterId);
        String keyword = cmd.getKeyword();
        if (StringUtils.isNotEmpty(keyword)) {
            keyword = keyword.toLowerCase();
        }
        List<HostVO> hosts = resourceManager.listHostsInClusterByStatus(clusterId, Status.Up);
        List<String> additionalNameFilters = getAdditionalNameFilters(cluster);
        List<String> managedVms = new ArrayList<>(additionalNameFilters);
        managedVms.addAll(getHostsManagedVms(hosts));
        List<UnmanagedInstanceResponse> responses = new ArrayList<>();
        for (HostVO host : hosts) {
            HashMap<String, UnmanagedInstanceTO> unmanagedInstances = getUnmanagedInstancesForHost(host, cmd.getName(), managedVms);
            Set<String> keys = unmanagedInstances.keySet();
            for (String key : keys) {
                UnmanagedInstanceTO instance = unmanagedInstances.get(key);
                if (StringUtils.isNotEmpty(keyword) &&
                        !instance.getName().toLowerCase().contains(keyword)) {
                    continue;
                }
                responses.add(responseGenerator.createUnmanagedInstanceResponse(instance, cluster, host));
            }
        }
        ListResponse<UnmanagedInstanceResponse> listResponses = new ListResponse<>();
        listResponses.setResponses(responses, responses.size());
        return listResponses;
    }

    @Override
    public UserVmResponse importUnmanagedInstance(ImportUnmanagedInstanceCmd cmd) {
        return baseImportInstance(cmd);
    }

    /**
     * Base logic for import virtual machines (unmanaged, external) into CloudStack
     * @param cmd importVM or importUnmanagedInstance command
     * @return imported user vm
     */
    private UserVmResponse baseImportInstance(ImportUnmanagedInstanceCmd cmd) {
        basicParametersCheckForImportInstance(cmd.getName(), cmd.getDomainId(), cmd.getAccountName());

        final String instanceName = cmd.getName();
        Long clusterId = cmd.getClusterId();
        Cluster cluster = basicAccessChecks(clusterId);

        final Account caller = CallContext.current().getCallingAccount();
        final DataCenter zone = dataCenterDao.findById(cluster.getDataCenterId());
        final Account owner = accountService.getActiveAccountById(cmd.getEntityOwnerId());
        long userId = getUserIdForImportInstance(owner);

        VMTemplateVO template = getTemplateForImportInstance(cmd.getTemplateId(), cluster.getHypervisorType());
        ServiceOfferingVO serviceOffering = getServiceOfferingForImportInstance(cmd.getServiceOfferingId(), owner, zone);

        checkResourceLimitForImportInstance(owner);

        String displayName = getDisplayNameForImportInstance(cmd.getDisplayName(), instanceName);
        String hostName = getHostNameForImportInstance(cmd.getHostName(), cluster.getHypervisorType(), instanceName, displayName);

        checkVmwareInstanceNameForImportInstance(cluster.getHypervisorType(), instanceName, hostName, zone);
        final Map<String, Long> nicNetworkMap = cmd.getNicNetworkList();
        final Map<String, Network.IpAddresses> nicIpAddressMap = cmd.getNicIpAddressList();
        final Map<String, Long> dataDiskOfferingMap = cmd.getDataDiskToDiskOfferingList();
        final Map<String, String> details = cmd.getDetails();
        final boolean forced = cmd.isForced();
        List<HostVO> hosts = resourceManager.listHostsInClusterByStatus(clusterId, Status.Up);
        UserVm userVm = null;
        List<String> additionalNameFilters = getAdditionalNameFilters(cluster);
        List<String> managedVms = new ArrayList<>(additionalNameFilters);
        managedVms.addAll(getHostsManagedVms(hosts));

        ActionEventUtils.onStartedActionEvent(userId, owner.getId(), EventTypes.EVENT_VM_IMPORT,
                cmd.getEventDescription(), null, null, true, 0);

        if (cmd instanceof ImportVmCmd) {
            ImportVmCmd importVmCmd = (ImportVmCmd) cmd;
            if (StringUtils.isBlank(importVmCmd.getImportSource())) {
                throw new CloudRuntimeException("Please provide an import source for importing the VM");
            }
            String source = importVmCmd.getImportSource().toUpperCase();
            ImportSource importSource = Enum.valueOf(ImportSource.class, source);
            if (ImportSource.VMWARE == importSource) {
                userVm = importUnmanagedInstanceFromVmwareToKvm(zone, cluster,
                        template, instanceName, displayName, hostName, caller, owner, userId,
                        serviceOffering, dataDiskOfferingMap,
                        nicNetworkMap, nicIpAddressMap,
                        details, importVmCmd, forced);
            }
        } else {
            if (List.of(Hypervisor.HypervisorType.VMware, Hypervisor.HypervisorType.KVM).contains(cluster.getHypervisorType())) {
                userVm = importUnmanagedInstanceFromHypervisor(zone, cluster, hosts, additionalNameFilters,
                        template, instanceName, displayName, hostName, caller, owner, userId,
                        serviceOffering, dataDiskOfferingMap,
                        nicNetworkMap, nicIpAddressMap,
                        details, cmd.getMigrateAllowed(), managedVms, forced);
            }
        }

        if (userVm == null) {
            ActionEventUtils.onCompletedActionEvent(userId, owner.getId(), EventVO.LEVEL_ERROR, EventTypes.EVENT_VM_IMPORT,
                    cmd.getEventDescription(), null, null, 0);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Failed to find unmanaged vm with name: %s in cluster: %s", instanceName, cluster.getUuid()));
        }
        ActionEventUtils.onCompletedActionEvent(userId, owner.getId(), EventVO.LEVEL_INFO, EventTypes.EVENT_VM_IMPORT,
                cmd.getEventDescription(), userVm.getId(), ApiCommandResourceType.VirtualMachine.toString(), 0);
        return responseGenerator.createUserVmResponse(ResponseObject.ResponseView.Full, "virtualmachine", userVm).get(0);
    }

    private long getUserIdForImportInstance(Account owner) {
        long userId = CallContext.current().getCallingUserId();
        List<UserVO> userVOs = userDao.listByAccount(owner.getAccountId());
        if (CollectionUtils.isNotEmpty(userVOs)) {
            userId = userVOs.get(0).getId();
        }
        return userId;
    }

    protected void basicParametersCheckForImportInstance(String name, Long domainId, String accountName) {
        if (StringUtils.isEmpty(name)) {
            throw new InvalidParameterValueException("Instance name cannot be empty");
        }
        if (domainId != null && StringUtils.isEmpty(accountName)) {
            throw new InvalidParameterValueException(String.format("%s parameter must be specified with %s parameter", ApiConstants.DOMAIN_ID, ApiConstants.ACCOUNT));
        }
    }

    private void checkVmwareInstanceNameForImportInstance(Hypervisor.HypervisorType hypervisorType, String instanceName, String hostName, DataCenter zone) {
        if (hypervisorType.equals(Hypervisor.HypervisorType.VMware) &&
                Boolean.parseBoolean(configurationDao.getValue(Config.SetVmInternalNameUsingDisplayName.key()))) {
            // If global config vm.instancename.flag is set to true, then CS will set guest VM's name as it appears on the hypervisor, to its hostname.
            // In case of VMware since VM name must be unique within a DC, check if VM with the same hostname already exists in the zone.
            VMInstanceVO vmByHostName = vmDao.findVMByHostNameInZone(hostName, zone.getId());
            if (vmByHostName != null && vmByHostName.getState() != VirtualMachine.State.Expunging) {
                throw new InvalidParameterValueException(String.format("Failed to import VM: %s. There already exists a VM by the hostname: %s in zone: %s", instanceName, hostName, zone.getUuid()));
            }
        }
    }

    private String getHostNameForImportInstance(String hostName, Hypervisor.HypervisorType hypervisorType,
                                                String instanceName, String displayName) {
        if (StringUtils.isEmpty(hostName)) {
            hostName = hypervisorType == Hypervisor.HypervisorType.VMware ? instanceName : displayName;
            if (!NetUtils.verifyDomainNameLabel(hostName, true)) {
                throw new InvalidParameterValueException("Please provide a valid hostname for the VM. VM name contains unsupported characters that cannot be used as hostname.");
            }
        }
        if (!NetUtils.verifyDomainNameLabel(hostName, true)) {
            throw new InvalidParameterValueException("Invalid VM hostname. VM hostname can contain ASCII letters 'a' through 'z', the digits '0' through '9', "
                    + "and the hyphen ('-'), must be between 1 and 63 characters long, and can't start or end with \"-\" and can't start with digit");
        }
        return hostName;
    }

    private String getDisplayNameForImportInstance(String displayName, String instanceName) {
        return StringUtils.isEmpty(displayName) ? instanceName : displayName;
    }

    private void checkResourceLimitForImportInstance(Account owner) {
        try {
            resourceLimitService.checkResourceLimit(owner, Resource.ResourceType.user_vm, 1);
        } catch (ResourceAllocationException e) {
            logger.error(String.format("VM resource allocation error for account: %s", owner.getUuid()), e);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("VM resource allocation error for account: %s. %s", owner.getUuid(), StringUtils.defaultString(e.getMessage())));
        }
    }

    private ServiceOfferingVO getServiceOfferingForImportInstance(Long serviceOfferingId, Account owner, DataCenter zone) {
        if (serviceOfferingId == null) {
            throw new InvalidParameterValueException("Service offering ID cannot be null");
        }
        final ServiceOfferingVO serviceOffering = serviceOfferingDao.findById(serviceOfferingId);
        if (serviceOffering == null) {
            throw new InvalidParameterValueException(String.format("Service offering ID: %d cannot be found", serviceOfferingId));
        }
        accountService.checkAccess(owner, serviceOffering, zone);
        return serviceOffering;
    }

    protected VMTemplateVO getTemplateForImportInstance(Long templateId, Hypervisor.HypervisorType hypervisorType) {
        VMTemplateVO template;
        if (templateId == null) {
            template = templateDao.findByName(VM_IMPORT_DEFAULT_TEMPLATE_NAME);
            if (template == null) {
                template = createDefaultDummyVmImportTemplate(false);
                if (template == null) {
                    throw new InvalidParameterValueException(String.format("Default VM import template with unique name: %s for hypervisor: %s cannot be created. Please use templateid parameter for import", VM_IMPORT_DEFAULT_TEMPLATE_NAME, hypervisorType.toString()));
                }
            }
        } else {
            template = templateDao.findById(templateId);
        }
        if (template == null) {
            throw new InvalidParameterValueException(String.format("Template ID: %d cannot be found", templateId));
        }
        return template;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_IMPORT, eventDescription = "importing VM", async = true)
    public UserVmResponse importVm(ImportVmCmd cmd) {
        String source = cmd.getImportSource().toUpperCase();
        ImportSource importSource = Enum.valueOf(ImportSource.class, source);
        if (ImportSource.VMWARE == importSource  || ImportSource.UNMANAGED == importSource) {
            return baseImportInstance(cmd);
        } else {
            return importKvmInstance(cmd);
        }
    }

    private UserVm importUnmanagedInstanceFromHypervisor(DataCenter zone, Cluster cluster,
                                                         List<HostVO> hosts, List<String> additionalNameFilters,
                                                         VMTemplateVO template, String instanceName, String displayName,
                                                         String hostName, Account caller, Account owner, long userId,
                                                         ServiceOfferingVO serviceOffering, Map<String, Long> dataDiskOfferingMap,
                                                         Map<String, Long> nicNetworkMap, Map<String, Network.IpAddresses> nicIpAddressMap,
                                                         Map<String, String> details, Boolean migrateAllowed, List<String> managedVms, boolean forced) {
        UserVm userVm = null;
        for (HostVO host : hosts) {
            HashMap<String, UnmanagedInstanceTO> unmanagedInstances = getUnmanagedInstancesForHost(host, instanceName, managedVms);
            if (MapUtils.isEmpty(unmanagedInstances)) {
                continue;
            }
            Set<String> names = unmanagedInstances.keySet();
            for (String name : names) {
                if (!instanceName.equals(name)) {
                    continue;
                }
                UnmanagedInstanceTO unmanagedInstance = unmanagedInstances.get(name);
                if (unmanagedInstance == null) {
                    throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Unable to retrieve details for unmanaged VM: %s", name));
                }

                if (template.getName().equals(VM_IMPORT_DEFAULT_TEMPLATE_NAME) && cluster.getHypervisorType().equals(Hypervisor.HypervisorType.KVM)) {
                    throw new InvalidParameterValueException("Template is needed and unable to use default template for hypervisor " + host.getHypervisorType().toString());
                }

                if (template.getName().equals(VM_IMPORT_DEFAULT_TEMPLATE_NAME)) {
                    String osName = unmanagedInstance.getOperatingSystem();
                    GuestOS guestOS = null;
                    if (StringUtils.isNotEmpty(osName)) {
                        guestOS = guestOSDao.findOneByDisplayName(osName);
                    }

                    GuestOSHypervisor guestOSHypervisor = null;
                    if (guestOS != null) {
                        guestOSHypervisor = guestOSHypervisorDao.findByOsIdAndHypervisor(guestOS.getId(), host.getHypervisorType().toString(), host.getHypervisorVersion());
                    }
                    if (guestOSHypervisor == null && StringUtils.isNotEmpty(unmanagedInstance.getOperatingSystemId())) {
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
                        template, displayName, hostName, CallContext.current().getCallingAccount(), owner, userId,
                        serviceOffering, dataDiskOfferingMap,
                        nicNetworkMap, nicIpAddressMap,
                        details, migrateAllowed, forced, true);
                break;
            }
            if (userVm != null) {
                break;
            }
        }
        return userVm;
    }

    private Pair<UnmanagedInstanceTO, Boolean> getSourceVmwareUnmanagedInstance(String vcenter, String datacenterName, String username,
                                                                 String password, String clusterName, String sourceHostName,
                                                                 String sourceVM) {
        HypervisorGuru vmwareGuru = hypervisorGuruManager.getGuru(Hypervisor.HypervisorType.VMware);

        Map<String, String> params = createParamsForTemplateFromVmwareVmMigration(vcenter, datacenterName,
                username, password, clusterName, sourceHostName, sourceVM);

        return vmwareGuru.getHypervisorVMOutOfBandAndCloneIfRequired(sourceHostName, sourceVM, params);
    }

    private String createOvfTemplateOfSourceVmwareUnmanagedInstance(String vcenter, String datacenterName, String username,
                                                                    String password, String clusterName, String sourceHostName,
                                                                    String sourceVMwareInstanceName, DataStoreTO convertLocation, int threadsCountToExportOvf) {
        HypervisorGuru vmwareGuru = hypervisorGuruManager.getGuru(Hypervisor.HypervisorType.VMware);

        Map<String, String> params = createParamsForTemplateFromVmwareVmMigration(vcenter, datacenterName,
                username, password, clusterName, sourceHostName, sourceVMwareInstanceName);

        return vmwareGuru.createVMTemplateOutOfBand(sourceHostName, sourceVMwareInstanceName, params, convertLocation, threadsCountToExportOvf);
    }

    protected UserVm importUnmanagedInstanceFromVmwareToKvm(DataCenter zone, Cluster destinationCluster, VMTemplateVO template,
                                                          String sourceVMName, String displayName, String hostName,
                                                          Account caller, Account owner, long userId,
                                                          ServiceOfferingVO serviceOffering, Map<String, Long> dataDiskOfferingMap,
                                                          Map<String, Long> nicNetworkMap, Map<String, Network.IpAddresses> nicIpAddressMap,
                                                          Map<String, String> details, ImportVmCmd cmd, boolean forced) {
        Long existingVcenterId = cmd.getExistingVcenterId();
        String vcenter = cmd.getVcenter();
        String datacenterName = cmd.getDatacenterName();
        String username = cmd.getUsername();
        String password = cmd.getPassword();
        String clusterName = cmd.getClusterName();
        String sourceHostName = cmd.getHostIp();
        Long convertInstanceHostId = cmd.getConvertInstanceHostId();
        Long convertStoragePoolId = cmd.getConvertStoragePoolId();

        if ((existingVcenterId == null && vcenter == null) || (existingVcenterId != null && vcenter != null)) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                    "Please provide an existing vCenter ID or a vCenter IP/Name, parameters are mutually exclusive");
        }
        if (existingVcenterId == null && StringUtils.isAnyBlank(vcenter, datacenterName, username, password)) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                    "Please set all the information for a vCenter IP/Name, datacenter, username and password");
        }

        if (existingVcenterId != null) {
            VmwareDatacenterVO existingDC = vmwareDatacenterDao.findById(existingVcenterId);
            if (existingDC == null) {
                String err = String.format("Cannot find any existing VMware DC with ID %s", existingVcenterId);
                logger.error(err);
                throw new CloudRuntimeException(err);
            }
            vcenter = existingDC.getVcenterHost();
            datacenterName = existingDC.getVmwareDatacenterName();
            username = existingDC.getUser();
            password = existingDC.getPassword();
        }

        boolean isClonedInstance = false;
        UnmanagedInstanceTO sourceVMwareInstance = null;
        DataStoreTO temporaryConvertLocation = null;
        String ovfTemplateOnConvertLocation = null;
        try {
            HostVO convertHost = selectInstanceConversionKVMHostInCluster(destinationCluster, convertInstanceHostId);
            CheckConvertInstanceAnswer conversionSupportAnswer = checkConversionSupportOnHost(convertHost, sourceVMName, false);
            logger.debug(String.format("The host %s (%s) is selected to execute the conversion of the instance %s" +
                    " from VMware to KVM ", convertHost.getId(), convertHost.getName(), sourceVMName));

            temporaryConvertLocation = selectInstanceConversionTemporaryLocation(destinationCluster, convertStoragePoolId);
            List<StoragePoolVO> convertStoragePools = findInstanceConversionStoragePoolsInCluster(destinationCluster);
            long importStartTime = System.currentTimeMillis();
            Pair<UnmanagedInstanceTO, Boolean> sourceInstanceDetails = getSourceVmwareUnmanagedInstance(vcenter, datacenterName, username, password, clusterName, sourceHostName, sourceVMName);
            sourceVMwareInstance = sourceInstanceDetails.first();
            isClonedInstance = sourceInstanceDetails.second();
            boolean isWindowsVm = sourceVMwareInstance.getOperatingSystem().toLowerCase().contains("windows");
            if (isWindowsVm) {
                checkConversionSupportOnHost(convertHost, sourceVMName, true);
            }

            String instanceName = getGeneratedInstanceName(owner);
            checkNetworkingBeforeConvertingVmwareInstance(zone, owner, instanceName, hostName, sourceVMwareInstance, nicNetworkMap, nicIpAddressMap, forced);
            UnmanagedInstanceTO convertedInstance;
            if (cmd.getForceMsToImportVmFiles() || !conversionSupportAnswer.isOvfExportSupported()) {
                // Uses MS for OVF export to temporary conversion location
                int noOfThreads = UnmanagedVMsManager.ThreadsOnMSToImportVMwareVMFiles.value();
                ovfTemplateOnConvertLocation = createOvfTemplateOfSourceVmwareUnmanagedInstance(vcenter, datacenterName, username, password,
                        clusterName, sourceHostName, sourceVMwareInstance.getName(), temporaryConvertLocation, noOfThreads);
                convertedInstance = convertVmwareInstanceToKVMWithOVFOnConvertLocation(sourceVMName, sourceVMwareInstance, convertHost, convertStoragePools,
                        temporaryConvertLocation, ovfTemplateOnConvertLocation);
            } else {
                // Uses KVM Host for OVF export to temporary conversion location, through ovftool
                convertedInstance = convertVmwareInstanceToKVMAfterExportingOVFToConvertLocation(sourceVMName, sourceVMwareInstance, convertHost, convertStoragePools,
                        temporaryConvertLocation, vcenter, username, password, datacenterName);
            }

            sanitizeConvertedInstance(convertedInstance, sourceVMwareInstance);
            UserVm userVm = importVirtualMachineInternal(convertedInstance, instanceName, zone, destinationCluster, null,
                    template, displayName, hostName, caller, owner, userId,
                    serviceOffering, dataDiskOfferingMap,
                    nicNetworkMap, nicIpAddressMap,
                    details, false, forced, false);
            long timeElapsedInSecs = (System.currentTimeMillis() - importStartTime) / 1000;
            logger.debug(String.format("VMware VM %s imported successfully to CloudStack instance %s (%s), Time taken: %d secs, OVF files imported from %s, Source VMware VM details - OS: %s, PowerState: %s, Disks: %s, NICs: %s",
                    sourceVMName, instanceName, displayName, timeElapsedInSecs, (ovfTemplateOnConvertLocation != null)? "MS" : "KVM Host", sourceVMwareInstance.getOperatingSystem(), sourceVMwareInstance.getPowerState(), sourceVMwareInstance.getDisks(), sourceVMwareInstance.getNics()));
            return userVm;
        } catch (CloudRuntimeException e) {
            logger.error(String.format("Error importing VM: %s", e.getMessage()), e);
            ActionEventUtils.onCompletedActionEvent(userId, owner.getId(), EventVO.LEVEL_ERROR, EventTypes.EVENT_VM_IMPORT,
                    cmd.getEventDescription(), null, null, 0);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        } finally {
            if (isClonedInstance && sourceVMwareInstance != null) {
                removeClonedInstance(vcenter, datacenterName, username, password, sourceHostName, sourceVMwareInstance.getName(), sourceVMName);
            }
            if (temporaryConvertLocation != null  && StringUtils.isNotBlank(ovfTemplateOnConvertLocation)) {
                removeTemplate(temporaryConvertLocation, ovfTemplateOnConvertLocation);
            }
        }
    }

    private void checkNetworkingBeforeConvertingVmwareInstance(DataCenter zone, Account owner, String instanceName,
                                                               String hostName, UnmanagedInstanceTO sourceVMwareInstance,
                                                               Map<String, Long> nicNetworkMap,
                                                               Map<String, Network.IpAddresses> nicIpAddressMap,
                                                               boolean forced) {
        List<UnmanagedInstanceTO.Nic> nics = sourceVMwareInstance.getNics();
        List<Long> networkIds = new ArrayList<>(nicNetworkMap.values());
        if (nics.size() != networkIds.size()) {
            String msg = String.format("Different number of nics found on instance %s: %s vs %s nics provided",
                    sourceVMwareInstance.getName(), nics.size(), networkIds.size());
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }

        for (UnmanagedInstanceTO.Nic nic : nics) {
            Long networkId = nicNetworkMap.get(nic.getNicId());
            NetworkVO network = networkDao.findById(networkId);
            if (network == null) {
                String err = String.format("Cannot find a network with id = %s", networkId);
                logger.error(err);
                throw new CloudRuntimeException(err);
            }
            Network.IpAddresses ipAddresses = null;
            if (MapUtils.isNotEmpty(nicIpAddressMap) && nicIpAddressMap.containsKey(nic.getNicId())) {
                ipAddresses = nicIpAddressMap.get(nic.getNicId());
            }
            boolean autoImport = ipAddresses != null && ipAddresses.getIp4Address() != null && ipAddresses.getIp4Address().equalsIgnoreCase("auto");
            checkUnmanagedNicAndNetworkMacAddressForImport(network, nic, forced);
            checkUnmanagedNicAndNetworkForImport(instanceName, nic, network, zone, owner, autoImport, Hypervisor.HypervisorType.KVM);
            checkUnmanagedNicAndNetworkHostnameForImport(instanceName, nic, network, hostName);
            checkUnmanagedNicIpAndNetworkForImport(instanceName, nic, network, ipAddresses);
        }
    }

    private void checkUnmanagedNicAndNetworkMacAddressForImport(NetworkVO network, UnmanagedInstanceTO.Nic nic, boolean forced) {
        NicVO existingNic = nicDao.findByNetworkIdAndMacAddress(network.getId(), nic.getMacAddress());
        if (existingNic != null && !forced) {
            String err = String.format("NIC with MAC address %s already exists on network with ID %s and forced flag is disabled. " +
                    "Retry with forced flag enabled if a new MAC address to be generated.", nic.getMacAddress(), network.getUuid());
            logger.error(err);
            throw new CloudRuntimeException(err);
        }
    }

    private String getGeneratedInstanceName(Account owner) {
        long id = vmDao.getNextInSequence(Long.class, "id");
        String instanceSuffix = configurationDao.getValue(Config.InstanceName.key());
        if (instanceSuffix == null) {
            instanceSuffix = "DEFAULT";
        }
        return VirtualMachineName.getVmName(id, owner.getId(), instanceSuffix);
    }

    private void sanitizeConvertedInstance(UnmanagedInstanceTO convertedInstance, UnmanagedInstanceTO sourceVMwareInstance) {
        convertedInstance.setCpuCores(sourceVMwareInstance.getCpuCores());
        convertedInstance.setCpuSpeed(sourceVMwareInstance.getCpuSpeed());
        convertedInstance.setCpuCoresPerSocket(sourceVMwareInstance.getCpuCoresPerSocket());
        convertedInstance.setMemory(sourceVMwareInstance.getMemory());
        convertedInstance.setPowerState(UnmanagedInstanceTO.PowerState.PowerOff);
        List<UnmanagedInstanceTO.Disk> convertedInstanceDisks = convertedInstance.getDisks();
        List<UnmanagedInstanceTO.Disk> sourceVMwareInstanceDisks = sourceVMwareInstance.getDisks();
        for (int i = 0; i < convertedInstanceDisks.size(); i++) {
            UnmanagedInstanceTO.Disk disk = convertedInstanceDisks.get(i);
            disk.setDiskId(sourceVMwareInstanceDisks.get(i).getDiskId());
        }
        List<UnmanagedInstanceTO.Nic> convertedInstanceNics = convertedInstance.getNics();
        List<UnmanagedInstanceTO.Nic> sourceVMwareInstanceNics = sourceVMwareInstance.getNics();
        if (CollectionUtils.isEmpty(convertedInstanceNics) && CollectionUtils.isNotEmpty(sourceVMwareInstanceNics)) {
            for (UnmanagedInstanceTO.Nic nic : sourceVMwareInstanceNics) {
                // In case the NICs information is not parsed from the converted XML domain, use the cloned instance NICs with virtio adapter
                nic.setAdapterType("virtio");
            }
            convertedInstance.setNics(sourceVMwareInstanceNics);
            for (int i = 0; i < convertedInstanceNics.size(); i++) {
                UnmanagedInstanceTO.Nic nic = convertedInstanceNics.get(i);
                nic.setNicId(sourceVMwareInstanceNics.get(i).getNicId());
            }
        } else if (CollectionUtils.isNotEmpty(convertedInstanceNics) && CollectionUtils.isNotEmpty(sourceVMwareInstanceNics)
                && convertedInstanceNics.size() == sourceVMwareInstanceNics.size()) {
            for (int i = 0; i < convertedInstanceNics.size(); i++) {
                UnmanagedInstanceTO.Nic nic = convertedInstanceNics.get(i);
                nic.setNicId(sourceVMwareInstanceNics.get(i).getNicId());
                if (nic.getMacAddress() == null) {
                    nic.setMacAddress(sourceVMwareInstanceNics.get(i).getMacAddress());
                }
            }
        }
    }

    private void removeClonedInstance(String vcenter, String datacenterName, String username, String password,
                                      String sourceHostName, String clonedInstanceName, String sourceVM) {
        HypervisorGuru vmwareGuru = hypervisorGuruManager.getGuru(Hypervisor.HypervisorType.VMware);
        Map<String, String> params = createParamsForRemoveClonedInstance(vcenter, datacenterName, username, password, sourceVM);
        boolean result = vmwareGuru.removeClonedHypervisorVMOutOfBand(sourceHostName, clonedInstanceName, params);
        if (!result) {
            String msg = String.format("Could not properly remove the cloned instance %s from VMware datacenter %s:%s",
                    clonedInstanceName, vcenter, datacenterName);
            logger.warn(msg);
            return;
        }
        logger.debug(String.format("Removed the cloned instance %s from VMWare datacenter %s/%s",
                clonedInstanceName, vcenter, datacenterName));
    }

    private void removeTemplate(DataStoreTO convertLocation, String ovfTemplateOnConvertLocation) {
        HypervisorGuru vmwareGuru = hypervisorGuruManager.getGuru(Hypervisor.HypervisorType.VMware);
        boolean result = vmwareGuru.removeVMTemplateOutOfBand(convertLocation, ovfTemplateOnConvertLocation);
        if (!result) {
            String msg = String.format("Could not remove the template file %s on datastore %s",
                    ovfTemplateOnConvertLocation, convertLocation.getUrl());
            logger.warn(msg);
            return;
        }
        logger.debug(String.format("Removed the template file %s on datastore %s",
                ovfTemplateOnConvertLocation, convertLocation.getUrl()));
    }

    private Map<String, String> createParamsForRemoveClonedInstance(String vcenter, String datacenterName, String username,
                                                                    String password, String sourceVM) {
        Map<String, String> params = new HashMap<>();
        params.put(VmDetailConstants.VMWARE_VCENTER_HOST, vcenter);
        params.put(VmDetailConstants.VMWARE_DATACENTER_NAME, datacenterName);
        params.put(VmDetailConstants.VMWARE_VCENTER_USERNAME, username);
        params.put(VmDetailConstants.VMWARE_VCENTER_PASSWORD, password);
        return params;
    }

    private HostVO selectInstanceConversionKVMHostInCluster(Cluster destinationCluster, Long convertInstanceHostId) {
        if (convertInstanceHostId != null) {
            HostVO selectedHost = hostDao.findById(convertInstanceHostId);
            if (selectedHost == null) {
                String msg = String.format("Cannot find host with ID %s", convertInstanceHostId);
                logger.error(msg);
                throw new CloudRuntimeException(msg);
            }
            if (selectedHost.getResourceState() != ResourceState.Enabled ||
                    selectedHost.getStatus() != Status.Up || selectedHost.getType() != Host.Type.Routing ||
                    selectedHost.getClusterId() != destinationCluster.getId()) {
                String msg = String.format("Cannot perform the conversion on the host %s as it is not a running and Enabled host", selectedHost.getName());
                logger.error(msg);
                throw new CloudRuntimeException(msg);
            }
            return selectedHost;
        }
        // Auto select host with conversion capability
        List<HostVO> hosts = hostDao.listByClusterHypervisorTypeAndHostCapability(destinationCluster.getId(), destinationCluster.getHypervisorType(), Host.HOST_INSTANCE_CONVERSION);
        if (CollectionUtils.isNotEmpty(hosts)) {
            return hosts.get(new Random().nextInt(hosts.size()));
        }

        // Try without host capability check
        hosts = hostDao.listByClusterAndHypervisorType(destinationCluster.getId(), destinationCluster.getHypervisorType());
        if (CollectionUtils.isNotEmpty(hosts)) {
            return hosts.get(new Random().nextInt(hosts.size()));
        }

        String err = String.format("Could not find any suitable %s host in cluster %s to perform the instance conversion",
                destinationCluster.getHypervisorType(), destinationCluster.getName());
        logger.error(err);
        throw new CloudRuntimeException(err);
    }

    private CheckConvertInstanceAnswer checkConversionSupportOnHost(HostVO convertHost, String sourceVM, boolean checkWindowsGuestConversionSupport) {
        logger.debug(String.format("Checking the %s conversion support on the host %s (%s)", checkWindowsGuestConversionSupport? "windows guest" : "", convertHost.getId(), convertHost.getName()));
        CheckConvertInstanceCommand cmd = new CheckConvertInstanceCommand(checkWindowsGuestConversionSupport);
        int timeoutSeconds = 60;
        cmd.setWait(timeoutSeconds);

        CheckConvertInstanceAnswer checkConvertInstanceAnswer;
        try {
            checkConvertInstanceAnswer = (CheckConvertInstanceAnswer) agentManager.send(convertHost.getId(), cmd);
        } catch (AgentUnavailableException | OperationTimedoutException e) {
            String err = String.format("Failed to check %s conversion support on the host %s for converting instance %s from VMware to KVM due to: %s",
                    checkWindowsGuestConversionSupport? "windows guest" : "", convertHost.getName(), sourceVM, e.getMessage());
            logger.error(err);
            throw new CloudRuntimeException(err);
        }

        if (!checkConvertInstanceAnswer.getResult()) {
            String err = String.format("The host %s doesn't support conversion of instance %s from VMware to KVM due to: %s",
                    convertHost.getName(), sourceVM, checkConvertInstanceAnswer.getDetails());
            logger.error(err);
            throw new CloudRuntimeException(err);
        }

        return checkConvertInstanceAnswer;
    }

    private UnmanagedInstanceTO convertVmwareInstanceToKVMWithOVFOnConvertLocation(String sourceVM, UnmanagedInstanceTO sourceVMwareInstance, HostVO convertHost,
                                                                                   List<StoragePoolVO> convertStoragePools, DataStoreTO temporaryConvertLocation,
                                                                                   String ovfTemplateDirConvertLocation) {
        logger.debug(String.format("Delegating the conversion of instance %s from VMware to KVM to the host %s (%s) using OVF %s on conversion datastore",
                sourceVM, convertHost.getId(), convertHost.getName(), ovfTemplateDirConvertLocation));

        RemoteInstanceTO remoteInstanceTO = new RemoteInstanceTO(sourceVM);
        List<String> destinationStoragePools = selectInstanceConversionStoragePools(convertStoragePools, sourceVMwareInstance.getDisks());
        ConvertInstanceCommand cmd = new ConvertInstanceCommand(remoteInstanceTO,
                Hypervisor.HypervisorType.KVM, destinationStoragePools, temporaryConvertLocation, ovfTemplateDirConvertLocation, false, false);
        int timeoutSeconds = UnmanagedVMsManager.ConvertVmwareInstanceToKvmTimeout.value() * 60 * 60;
        cmd.setWait(timeoutSeconds);

        Answer convertAnswer;
        try {
             convertAnswer = agentManager.send(convertHost.getId(), cmd);
        } catch (AgentUnavailableException | OperationTimedoutException e) {
            String err = String.format("Could not send the convert instance command to host %s (%s) due to: %s",
                    convertHost.getId(), convertHost.getName(), e.getMessage());
            logger.error(err, e);
            throw new CloudRuntimeException(err);
        }

        if (!convertAnswer.getResult()) {
            String err = String.format("The convert process failed for instance %s from VMware to KVM on host %s: %s",
                    sourceVM, convertHost.getName(), convertAnswer.getDetails());
            logger.error(err);
            throw new CloudRuntimeException(err);
        }
        return ((ConvertInstanceAnswer) convertAnswer).getConvertedInstance();
    }

    private UnmanagedInstanceTO convertVmwareInstanceToKVMAfterExportingOVFToConvertLocation(String sourceVM, UnmanagedInstanceTO sourceVMwareInstance, HostVO convertHost,
                                                                                             List<StoragePoolVO> convertStoragePools, DataStoreTO temporaryConvertLocation,
                                                                                             String vcenterHost, String vcenterUsername, String vcenterPassword, String datacenterName) {
        logger.debug(String.format("Delegating the conversion of instance %s from VMware to KVM to the host %s (%s) after OVF export through ovftool",
                sourceVM, convertHost.getId(), convertHost.getName()));

        RemoteInstanceTO remoteInstanceTO = new RemoteInstanceTO(sourceVMwareInstance.getName(), vcenterHost, vcenterUsername, vcenterPassword, datacenterName);
        List<String> destinationStoragePools = selectInstanceConversionStoragePools(convertStoragePools, sourceVMwareInstance.getDisks());
        ConvertInstanceCommand cmd = new ConvertInstanceCommand(remoteInstanceTO,
                Hypervisor.HypervisorType.KVM, destinationStoragePools, temporaryConvertLocation, null, false, true);
        int timeoutSeconds = UnmanagedVMsManager.ConvertVmwareInstanceToKvmTimeout.value() * 60 * 60;
        cmd.setWait(timeoutSeconds);
        int noOfThreads = UnmanagedVMsManager.ThreadsOnKVMHostToImportVMwareVMFiles.value();
        if (noOfThreads == 0) {
            // Use no. of threads as the disks count
            noOfThreads = sourceVMwareInstance.getDisks().size();
        }
        cmd.setThreadsCountToExportOvf(noOfThreads);

        Answer convertAnswer;
        try {
            convertAnswer = agentManager.send(convertHost.getId(), cmd);
        } catch (AgentUnavailableException | OperationTimedoutException e) {
            String err = String.format("Could not send the convert instance command to host %s (%s) due to: %s",
                    convertHost.getId(), convertHost.getName(), e.getMessage());
            logger.error(err, e);
            throw new CloudRuntimeException(err);
        }

        if (!convertAnswer.getResult()) {
            String err = String.format("The convert process failed for instance %s from VMware to KVM on host %s: %s",
                    sourceVM, convertHost.getName(), convertAnswer.getDetails());
            logger.error(err);
            throw new CloudRuntimeException(err);
        }
        return ((ConvertInstanceAnswer) convertAnswer).getConvertedInstance();
    }

    private List<StoragePoolVO> findInstanceConversionStoragePoolsInCluster(Cluster destinationCluster) {
        List<StoragePoolVO> pools = new ArrayList<>();
        List<StoragePoolVO> clusterPools = primaryDataStoreDao.findClusterWideStoragePoolsByHypervisorAndPoolType(destinationCluster.getId(), Hypervisor.HypervisorType.KVM, Storage.StoragePoolType.NetworkFilesystem);
        pools.addAll(clusterPools);
        List<StoragePoolVO> zonePools = primaryDataStoreDao.findZoneWideStoragePoolsByHypervisorAndPoolType(destinationCluster.getDataCenterId(), Hypervisor.HypervisorType.KVM, Storage.StoragePoolType.NetworkFilesystem);
        pools.addAll(zonePools);
        if (pools.isEmpty()) {
            String msg = String.format("Cannot find suitable storage pools in cluster %s for the conversion", destinationCluster.getName());
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
        return pools;
    }

    private List<String> selectInstanceConversionStoragePools(List<StoragePoolVO> pools, List<UnmanagedInstanceTO.Disk> disks) {
        List<String> storagePools = new ArrayList<>(disks.size());
        //TODO: Choose pools by capacity
        for (UnmanagedInstanceTO.Disk disk : disks) {
            Long capacity = disk.getCapacity();
            storagePools.add(pools.get(0).getUuid());
        }
        return storagePools;
    }

    private void logFailureAndThrowException(String msg) {
        logger.error(msg);
        throw new CloudRuntimeException(msg);
    }

    protected DataStoreTO selectInstanceConversionTemporaryLocation(Cluster destinationCluster, Long convertStoragePoolId) {
        if (convertStoragePoolId != null) {
            StoragePoolVO selectedStoragePool = primaryDataStoreDao.findById(convertStoragePoolId);
            if (selectedStoragePool == null) {
                logFailureAndThrowException(String.format("Cannot find a storage pool with ID %s", convertStoragePoolId));
            }
            if ((selectedStoragePool.getScope() == ScopeType.CLUSTER && selectedStoragePool.getClusterId() != destinationCluster.getId()) ||
                    (selectedStoragePool.getScope() == ScopeType.ZONE && selectedStoragePool.getDataCenterId() != destinationCluster.getDataCenterId())) {
                logFailureAndThrowException(String.format("Cannot use the storage pool %s for the instance conversion as " +
                        "it is not in the scope of the cluster %s", selectedStoragePool.getName(), destinationCluster.getName()));
            }
            if (selectedStoragePool.getScope() == ScopeType.HOST) {
                logFailureAndThrowException(String.format("The storage pool %s is a local storage pool and not supported for temporary conversion location, cluster and zone wide NFS storage pools are supported", selectedStoragePool.getName()));
            } else if (selectedStoragePool.getPoolType() != Storage.StoragePoolType.NetworkFilesystem) {
                logFailureAndThrowException(String.format("The storage pool %s is not supported for temporary conversion location, only NFS storage pools are supported", selectedStoragePool.getName()));
            }
            return dataStoreManager.getPrimaryDataStore(convertStoragePoolId).getTO();
        } else {
            long zoneId = destinationCluster.getDataCenterId();
            ImageStoreVO imageStore = imageStoreDao.findOneByZoneAndProtocol(zoneId, "nfs");
            if (imageStore == null) {
                logFailureAndThrowException(String.format("Could not find an NFS secondary storage pool on zone %s to use as a temporary location " +
                        "for instance conversion", zoneId));
            }
            DataStore dataStore = dataStoreManager.getDataStore(imageStore.getId(), DataStoreRole.Image);
            return dataStore.getTO();
        }
    }

    protected Map<String, String> createParamsForTemplateFromVmwareVmMigration(String vcenterHost, String datacenterName,
                                                                               String username, String password,
                                                                               String clusterName, String sourceHostName,
                                                                               String sourceVMName) {
        Map<String, String> params = new HashMap<>();
        params.put(VmDetailConstants.VMWARE_VCENTER_HOST, vcenterHost);
        params.put(VmDetailConstants.VMWARE_DATACENTER_NAME, datacenterName);
        params.put(VmDetailConstants.VMWARE_VCENTER_USERNAME, username);
        params.put(VmDetailConstants.VMWARE_VCENTER_PASSWORD, password);
        params.put(VmDetailConstants.VMWARE_CLUSTER_NAME, clusterName);
        params.put(VmDetailConstants.VMWARE_HOST_NAME, sourceHostName);
        params.put(VmDetailConstants.VMWARE_VM_NAME, sourceVMName);
        return params;
    }

    @Override
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(ListUnmanagedInstancesCmd.class);
        cmdList.add(ImportUnmanagedInstanceCmd.class);
        cmdList.add(UnmanageVMInstanceCmd.class);
        cmdList.add(ListVmsForImportCmd.class);
        cmdList.add(ImportVmCmd.class);
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
            throw new CloudRuntimeException(String.format("Cannot find a host to verify if the VM [%s] exists. Thus we are unable to unmanage it.", vmVO.getUuid()));
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
        } else if (!UnmanagedVMsManager.isSupported(vmVO.getHypervisorType())) {
            throw new UnsupportedServiceException("Unmanage VM is currently not allowed for hypervisor " +
                    vmVO.getHypervisorType().toString());
        } else if (vmVO.getType() != VirtualMachine.Type.User) {
            throw new UnsupportedServiceException("Unmanage VM is currently allowed for guest VMs only");
        }

        if (vmVO.getType().equals(VirtualMachine.Type.User)) {
            UserVmVO userVm = userVmDao.findById(vmId);
            if (UserVmManager.SHAREDFSVM.equals(userVm.getUserVmType())) {
                throw new InvalidParameterValueException("Operation not supported on Shared FileSystem Instance");
            }
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
            logger.error("Error verifying VM " + instanceName + " exists on host with ID = " + hostId + ": " + answer.getDetails());
        }
        return answer.getResult();
    }

    private UserVmResponse importKvmInstance(ImportVmCmd cmd) {
        final Account caller = CallContext.current().getCallingAccount();
        if (caller.getType() != Account.Type.ADMIN) {
            throw new PermissionDeniedException(String.format("Cannot perform this operation, Calling account is not root admin: %s", caller.getUuid()));
        }
        final Long zoneId = cmd.getZoneId();
        final DataCenterVO zone = dataCenterDao.findById(zoneId);
        if (zone == null) {
            throw new InvalidParameterValueException("Please specify a valid zone.");
        }
        final String hypervisorType = cmd.getHypervisor();
        if (!Hypervisor.HypervisorType.KVM.toString().equalsIgnoreCase(hypervisorType)) {
            throw new InvalidParameterValueException(String.format("VM import is currently not supported for hypervisor: %s", hypervisorType));
        }

        final String instanceName = cmd.getName();
        if (StringUtils.isEmpty(instanceName)) {
            throw new InvalidParameterValueException(String.format("Instance name cannot be empty"));
        }
        if (cmd.getDomainId() != null && StringUtils.isEmpty(cmd.getAccountName())) {
            throw new InvalidParameterValueException("domainid parameter must be specified with account parameter");
        }
        final Account owner = accountService.getActiveAccountById(cmd.getEntityOwnerId());
        long userId = CallContext.current().getCallingUserId();
        List<UserVO> userVOs = userDao.listByAccount(owner.getAccountId());
        if (CollectionUtils.isNotEmpty(userVOs)) {
            userId = userVOs.get(0).getId();
        }
        VMTemplateVO template = templateDao.findByName(KVM_VM_IMPORT_DEFAULT_TEMPLATE_NAME);
        if (template == null) {
            template = createDefaultDummyVmImportTemplate(true);
            if (template == null) {
                throw new InvalidParameterValueException("Error while creating default Import Vm Template");
            }
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
            logger.error(String.format("VM resource allocation error for account: %s", owner.getUuid()), e);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("VM resource allocation error for account: %s. %s", owner.getUuid(), StringUtils.defaultString(e.getMessage())));
        }
        String displayName = cmd.getDisplayName();
        if (StringUtils.isEmpty(displayName)) {
            displayName = instanceName;
        }
        String hostName = cmd.getHostName();
        if (StringUtils.isEmpty(hostName)) {
            if (!NetUtils.verifyDomainNameLabel(instanceName, true)) {
                throw new InvalidParameterValueException(String.format("Please provide hostname for the VM. VM name contains unsupported characters for it to be used as hostname"));
            }
            hostName = instanceName;
        }
        if (!NetUtils.verifyDomainNameLabel(hostName, true)) {
            throw new InvalidParameterValueException("Invalid VM hostname. VM hostname can contain ASCII letters 'a' through 'z', the digits '0' through '9', "
                    + "and the hyphen ('-'), must be between 1 and 63 characters long, and can't start or end with \"-\" and can't start with digit");
        }

        final Map<String, Long> nicNetworkMap = cmd.getNicNetworkList();
        final Map<String, Network.IpAddresses> nicIpAddressMap = cmd.getNicIpAddressList();
        final Map<String, Long> dataDiskOfferingMap = cmd.getDataDiskToDiskOfferingList();
        final Map<String, String> details = cmd.getDetails();

        String remoteUrl = cmd.getHost();
        String source = cmd.getImportSource().toUpperCase();
        String diskPath = cmd.getDiskPath();
        ImportSource importSource = Enum.valueOf(ImportSource.class, source);
        Long hostId = cmd.getHostId();
        Long poolId = cmd.getStoragePoolId();
        Long networkId = cmd.getNetworkId();

        UnmanagedInstanceTO unmanagedInstanceTO = null;
        if (ImportSource.EXTERNAL == importSource) {
            if (StringUtils.isBlank(cmd.getUsername())) {
                throw new InvalidParameterValueException("Username need to be provided.");
            }

            HashMap<String, UnmanagedInstanceTO> instancesMap = getRemoteVmsOnKVMHost(zoneId, remoteUrl, cmd.getUsername(), cmd.getPassword());
            unmanagedInstanceTO = instancesMap.get(cmd.getName());
            if (unmanagedInstanceTO == null) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("VM with name: %s not found on remote host %s", instanceName, remoteUrl));
            }
        }

        if (ImportSource.SHARED == importSource || ImportSource.LOCAL == importSource) {
            if (diskPath == null) {
                throw new InvalidParameterValueException("Disk Path is required for Import from shared/local storage");
            }

            if (networkId == null) {
                throw new InvalidParameterValueException("Network is required for Import from shared/local storage");
            }

            if (poolId == null) {
                throw new InvalidParameterValueException("Storage Pool is required for Import from shared/local storage");
            }

            StoragePool storagePool = primaryDataStoreDao.findById(poolId);
            if (storagePool == null) {
                throw new InvalidParameterValueException("Storage Pool not found");
            }

            if (volumeDao.findByPoolIdAndPath(poolId, diskPath) != null) {
                throw new InvalidParameterValueException("Disk image is already in use");
            }

            DiskOffering diskOffering = diskOfferingDao.findById(serviceOffering.getDiskOfferingId());

            if (diskOffering != null && !storagePoolSupportsDiskOffering(storagePool, diskOffering)) {
                throw new InvalidParameterValueException(String.format("Service offering: %s storage tags are not compatible with selected storage pool: %s", serviceOffering.getUuid(), storagePool.getUuid()));
            }
        }

        if (ImportSource.LOCAL == importSource) {
            if (hostId == null) {
                throw new InvalidParameterValueException("Host is required for Import from local storage");
            }

            if (hostDao.findById(hostId) == null) {
                throw new InvalidParameterValueException("Host not found");
            }

            if(storagePoolHostDao.findByPoolHost(poolId, hostId) == null) {
                throw new InvalidParameterValueException("Specified Local Storage Pool not found on Host");
            }
        }

        UserVm userVm = null;

        if (ImportSource.EXTERNAL == importSource) {
            String username = cmd.getUsername();
            String password = cmd.getPassword();
            String tmpPath = cmd.getTmpPath();
            userVm = importExternalKvmVirtualMachine(unmanagedInstanceTO, instanceName, zone,
                    template, displayName, hostName, caller, owner, userId,
                    serviceOffering, dataDiskOfferingMap,
                    nicNetworkMap, nicIpAddressMap, remoteUrl, username, password, tmpPath, details);
        } else if (ImportSource.SHARED == importSource || ImportSource.LOCAL == importSource) {
            try {
                userVm = importKvmVirtualMachineFromDisk(importSource, instanceName, zone,
                        template, displayName, hostName, caller, owner, userId,
                        serviceOffering, dataDiskOfferingMap, networkId, hostId, poolId, diskPath,
                        details);
            } catch (InsufficientCapacityException e) {
                throw new RuntimeException(e);
            } catch (ResourceAllocationException e) {
                throw new RuntimeException(e);
            }
        }
        if (userVm == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Failed to import Vm with name: %s ", instanceName));
        }

        CallContext.current().setEventResourceId(userVm.getId());
        CallContext.current().setEventResourceType(ApiCommandResourceType.VirtualMachine);
        return responseGenerator.createUserVmResponse(ResponseObject.ResponseView.Full, "virtualmachine", userVm).get(0);
    }

    private UserVm importExternalKvmVirtualMachine(final UnmanagedInstanceTO unmanagedInstance, final String instanceName, final DataCenter zone,
                                                final VirtualMachineTemplate template, final String displayName, final String hostName, final Account caller, final Account owner, final Long userId,
                                                final ServiceOfferingVO serviceOffering, final Map<String, Long> dataDiskOfferingMap,
                                                final Map<String, Long> nicNetworkMap, final Map<String, Network.IpAddresses> callerNicIpAddressMap,
                                                final String remoteUrl, String username, String password, String tmpPath, final Map<String, String> details) {
        UserVm userVm = null;

        Map<String, String> allDetails = new HashMap<>(details);
        // Check disks and supplied disk offerings
        List<UnmanagedInstanceTO.Disk> unmanagedInstanceDisks = unmanagedInstance.getDisks();

        if (CollectionUtils.isEmpty(unmanagedInstanceDisks)) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("No attached disks found for the unmanaged VM: %s", instanceName));
        }

        Pair<UnmanagedInstanceTO.Disk, List<UnmanagedInstanceTO.Disk>> rootAndDataDisksPair = getRootAndDataDisks(unmanagedInstanceDisks, dataDiskOfferingMap);
        final UnmanagedInstanceTO.Disk rootDisk = rootAndDataDisksPair.first();
        final List<UnmanagedInstanceTO.Disk> dataDisks = rootAndDataDisksPair.second();
        if (rootDisk == null || StringUtils.isEmpty(rootDisk.getController())) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("VM import failed. Unable to retrieve root disk details for VM: %s ", instanceName));
        }
        allDetails.put(VmDetailConstants.ROOT_DISK_CONTROLLER, rootDisk.getController());

        // Check NICs and supplied networks
        Map<String, Network.IpAddresses> nicIpAddressMap = getNicIpAddresses(unmanagedInstance.getNics(), callerNicIpAddressMap);
        Map<String, Long> allNicNetworkMap = getUnmanagedNicNetworkMap(unmanagedInstance.getName(), unmanagedInstance.getNics(), nicNetworkMap, nicIpAddressMap, zone, hostName, owner, Hypervisor.HypervisorType.KVM);
        if (!CollectionUtils.isEmpty(unmanagedInstance.getNics())) {
            allDetails.put(VmDetailConstants.NIC_ADAPTER, unmanagedInstance.getNics().get(0).getAdapterType());
        }
        VirtualMachine.PowerState powerState = VirtualMachine.PowerState.PowerOff;

        String internalName = getInternalName(owner.getAccountId());

        try {
            userVm = userVmManager.importVM(zone, null, template, internalName, displayName, owner,
                    null, caller, true, null, owner.getAccountId(), userId,
                    serviceOffering, null, hostName,
                    Hypervisor.HypervisorType.KVM, allDetails, powerState, null);
        } catch (InsufficientCapacityException ice) {
            logger.error(String.format("Failed to import vm name: %s", instanceName), ice);
            throw new ServerApiException(ApiErrorCode.INSUFFICIENT_CAPACITY_ERROR, ice.getMessage());
        }
        if (userVm == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Failed to import vm name: %s", instanceName));
        }
        DiskOfferingVO diskOffering = diskOfferingDao.findById(serviceOffering.getDiskOfferingId());
        String rootVolumeName = String.format("ROOT-%s", userVm.getId());
        DiskProfile diskProfile = volumeManager.allocateRawVolume(Volume.Type.ROOT, rootVolumeName, diskOffering, null, null, null, userVm, template, owner, null);

        DiskProfile[] dataDiskProfiles = new DiskProfile[dataDisks.size()];
        int diskSeq = 0;
        for (UnmanagedInstanceTO.Disk disk : dataDisks) {
            if (disk.getCapacity() == null || disk.getCapacity() == 0) {
                throw new InvalidParameterValueException(String.format("Disk ID: %s size is invalid", disk.getDiskId()));
            }
            DiskOffering offering = diskOfferingDao.findById(dataDiskOfferingMap.get(disk.getDiskId()));
            DiskProfile dataDiskProfile = volumeManager.allocateRawVolume(Volume.Type.DATADISK, String.format("DATA-%d-%s", userVm.getId(), disk.getDiskId()), offering, null, null, null, userVm, template, owner, null);
            dataDiskProfiles[diskSeq++] = dataDiskProfile;
        }

        final VirtualMachineProfile profile = new VirtualMachineProfileImpl(userVm, template, serviceOffering, owner, null);
        ServiceOfferingVO dummyOffering = serviceOfferingDao.findById(userVm.getId(), serviceOffering.getId());
        profile.setServiceOffering(dummyOffering);
        DeploymentPlanner.ExcludeList excludeList = new DeploymentPlanner.ExcludeList();
        final DataCenterDeployment plan = new DataCenterDeployment(zone.getId(), null, null, null, null, null);
        DeployDestination dest = null;
        try {
            dest = deploymentPlanningManager.planDeployment(profile, plan, excludeList, null);
        } catch (Exception e) {
            logger.warn(String.format("Import failed for Vm: %s while finding deployment destination", userVm.getInstanceName()), e);
            cleanupFailedImportVM(userVm);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Import failed for Vm: %s while finding deployment destination", userVm.getInstanceName()));
        }
        if(dest == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Import failed for Vm: %s. Suitable deployment destination not found", userVm.getInstanceName()));
        }

        List<Pair<DiskProfile, StoragePool>> diskProfileStoragePoolList = new ArrayList<>();
        try {
            if (rootDisk.getCapacity() == null || rootDisk.getCapacity() == 0) {
                throw new InvalidParameterValueException(String.format("Root disk ID: %s size is invalid", rootDisk.getDiskId()));
            }

            diskProfileStoragePoolList.add(importExternalDisk(rootDisk, userVm, dest, diskOffering, Volume.Type.ROOT,
                    template, null, remoteUrl, username, password, tmpPath, diskProfile));

            long deviceId = 1L;
            diskSeq = 0;
            for (UnmanagedInstanceTO.Disk disk : dataDisks) {
                DiskProfile dataDiskProfile = dataDiskProfiles[diskSeq++];
                DiskOffering offering = diskOfferingDao.findById(dataDiskOfferingMap.get(disk.getDiskId()));

                diskProfileStoragePoolList.add(importExternalDisk(disk, userVm, dest, offering, Volume.Type.DATADISK,
                        template, deviceId, remoteUrl, username, password, tmpPath, dataDiskProfile));
                deviceId++;
            }
        } catch (Exception e) {
            logger.error(String.format("Failed to import volumes while importing vm: %s", instanceName), e);
            cleanupFailedImportVM(userVm);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Failed to import volumes while importing vm: %s. %s", instanceName, StringUtils.defaultString(e.getMessage())));
        }
        try {
            int nicIndex = 0;
            for (UnmanagedInstanceTO.Nic nic : unmanagedInstance.getNics()) {
                Network network = networkDao.findById(allNicNetworkMap.get(nic.getNicId()));
                Network.IpAddresses ipAddresses = nicIpAddressMap.get(nic.getNicId());
                importNic(nic, userVm, network, ipAddresses, nicIndex, nicIndex==0, true);
                nicIndex++;
            }
        } catch (Exception e) {
            logger.error(String.format("Failed to import NICs while importing vm: %s", instanceName), e);
            cleanupFailedImportVM(userVm);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Failed to import NICs while importing vm: %s. %s", instanceName, StringUtils.defaultString(e.getMessage())));
        }
        publishVMUsageUpdateResourceCount(userVm, dummyOffering, template);
        return userVm;
    }

    private UserVm importKvmVirtualMachineFromDisk(final ImportSource importSource, final String instanceName, final DataCenter zone,
                                                   final VirtualMachineTemplate template, final String displayName, final String hostName, final Account caller, final Account owner, final Long userId,
                                                   final ServiceOfferingVO serviceOffering, final Map<String, Long> dataDiskOfferingMap, final Long networkId,
                                                   final Long hostId, final Long poolId, final String diskPath, final Map<String, String> details) throws InsufficientCapacityException, ResourceAllocationException {

        UserVm userVm = null;

        Map<String, String> allDetails = new HashMap<>(details);

        VirtualMachine.PowerState powerState = VirtualMachine.PowerState.PowerOff;

        NetworkVO network = networkDao.findById(networkId);
        if (network == null) {
            throw new InvalidParameterValueException("Unable to find network by id " + networkId);
        }

        networkModel.checkNetworkPermissions(owner, network);

        // don't allow to use system networks
        NetworkOffering networkOffering = entityMgr.findById(NetworkOffering.class, network.getNetworkOfferingId());
        if (networkOffering.isSystemOnly()) {
            throw new InvalidParameterValueException("Network id=" + networkId + " is system only and can't be used for vm deployment");
        }

        LinkedHashMap<String, List<NicProfile>> networkNicMap = new LinkedHashMap<>();

        if ((network.getDataCenterId() != zone.getId())) {
            if (!network.isStrechedL2Network()) {
                throw new InvalidParameterValueException("Network id=" + network.getId() +
                        " doesn't belong to zone " + zone.getId());
            }
        }

        String macAddress = networkModel.getNextAvailableMacAddressInNetwork(networkId);
        String ipAddress = null;
        if (network.getGuestType() != Network.GuestType.L2) {
            ipAddress = ipAddressManager.acquireGuestIpAddress(network, null);
        }

        Network.IpAddresses requestedIpPair = new Network.IpAddresses(ipAddress, null, macAddress);

        NicProfile nicProfile = new NicProfile(requestedIpPair.getIp4Address(), requestedIpPair.getIp6Address(), requestedIpPair.getMacAddress());
        nicProfile.setOrderIndex(0);

        boolean securityGroupEnabled = false;
        if (networkModel.isSecurityGroupSupportedInNetwork(network)) {
            securityGroupEnabled = true;
        }
        List<NicProfile> profiles = networkNicMap.get(network.getUuid());
        if (CollectionUtils.isEmpty(profiles)) {
            profiles = new ArrayList<>();
        }
        profiles.add(nicProfile);
        networkNicMap.put(network.getUuid(), profiles);

        String internalName = getInternalName(owner.getAccountId());

        try {
            userVm = userVmManager.importVM(zone, null, template, internalName, displayName, owner,
                    null, caller, true, null, owner.getAccountId(), userId,
                    serviceOffering, null, hostName,
                    Hypervisor.HypervisorType.KVM, allDetails, powerState, networkNicMap);
        } catch (InsufficientCapacityException ice) {
            logger.error(String.format("Failed to import vm name: %s", instanceName), ice);
            throw new ServerApiException(ApiErrorCode.INSUFFICIENT_CAPACITY_ERROR, ice.getMessage());
        }
        if (userVm == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Failed to import vm name: %s", instanceName));
        }
        DiskOfferingVO diskOffering = diskOfferingDao.findById(serviceOffering.getDiskOfferingId());
        String rootVolumeName = String.format("ROOT-%s", userVm.getId());
        DiskProfile diskProfile = volumeManager.allocateRawVolume(Volume.Type.ROOT, rootVolumeName, diskOffering, null, null, null, userVm, template, owner, null);

        final VirtualMachineProfile profile = new VirtualMachineProfileImpl(userVm, template, serviceOffering, owner, null);
        ServiceOfferingVO dummyOffering = serviceOfferingDao.findById(userVm.getId(), serviceOffering.getId());
        profile.setServiceOffering(dummyOffering);
        DeploymentPlanner.ExcludeList excludeList = new DeploymentPlanner.ExcludeList();
        final DataCenterDeployment plan = new DataCenterDeployment(zone.getId(), null, null, hostId, poolId, null);
        DeployDestination dest = null;
        try {
            dest = deploymentPlanningManager.planDeployment(profile, plan, excludeList, null);
        } catch (Exception e) {
            logger.warn(String.format("Import failed for Vm: %s while finding deployment destination", userVm.getInstanceName()), e);
            cleanupFailedImportVM(userVm);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Import failed for Vm: %s while finding deployment destination", userVm.getInstanceName()));
        }
        if(dest == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Import failed for Vm: %s. Suitable deployment destination not found", userVm.getInstanceName()));
        }

        Map<Volume, StoragePool> storage = dest.getStorageForDisks();
        Volume volume = volumeDao.findById(diskProfile.getVolumeId());
        StoragePool storagePool = storage.get(volume);
        CheckVolumeCommand checkVolumeCommand = new CheckVolumeCommand();
        checkVolumeCommand.setSrcFile(diskPath);
        StorageFilerTO storageTO = new StorageFilerTO(storagePool);
        checkVolumeCommand.setStorageFilerTO(storageTO);
        Answer answer = agentManager.easySend(dest.getHost().getId(), checkVolumeCommand);
        if (!(answer instanceof CheckVolumeAnswer)) {
            cleanupFailedImportVM(userVm);
            throw new CloudRuntimeException("Disk not found or is invalid");
        }
        CheckVolumeAnswer checkVolumeAnswer = (CheckVolumeAnswer) answer;
        if(!checkVolumeAnswer.getResult()) {
            cleanupFailedImportVM(userVm);
            throw new CloudRuntimeException("Disk not found or is invalid");
        }
        diskProfile.setSize(checkVolumeAnswer.getSize());

        List<Pair<DiskProfile, StoragePool>> diskProfileStoragePoolList = new ArrayList<>();
        try {
            long deviceId = 1L;
            if(ImportSource.SHARED == importSource) {
                diskProfileStoragePoolList.add(importKVMSharedDisk(userVm, diskOffering, Volume.Type.ROOT,
                        template, deviceId, poolId, diskPath, diskProfile));
            } else if(ImportSource.LOCAL == importSource) {
                diskProfileStoragePoolList.add(importKVMLocalDisk(userVm, diskOffering, Volume.Type.ROOT,
                        template, deviceId, hostId, diskPath, diskProfile));
            }
        } catch (Exception e) {
            logger.error(String.format("Failed to import volumes while importing vm: %s", instanceName), e);
            cleanupFailedImportVM(userVm);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Failed to import volumes while importing vm: %s. %s", instanceName, StringUtils.defaultString(e.getMessage())));
        }
        networkOrchestrationService.importNic(macAddress, 0, network, true, userVm, requestedIpPair, zone, true);
        publishVMUsageUpdateResourceCount(userVm, dummyOffering, template);
        return userVm;
    }

    private NetworkVO getDefaultNetwork(DataCenter zone, Account owner, boolean selectAny) throws InsufficientCapacityException, ResourceAllocationException {
        NetworkVO defaultNetwork = null;

        // if no network is passed in
        // Check if default virtual network offering has
        // Availability=Required. If it's true, search for corresponding
        // network
        // * if network is found, use it. If more than 1 virtual network is
        // found, throw an error
        // * if network is not found, create a new one and use it

        List<NetworkOfferingVO> requiredOfferings = networkOfferingDao.listByAvailability(NetworkOffering.Availability.Required, false);
        if (requiredOfferings.size() < 1) {
            throw new InvalidParameterValueException("Unable to find network offering with availability=" + NetworkOffering.Availability.Required
                    + " to automatically create the network as a part of vm creation");
        }

        if (requiredOfferings.get(0).getState() == NetworkOffering.State.Enabled) {
            // get Virtual networks
            List<? extends Network> virtualNetworks = networkModel.listNetworksForAccount(owner.getId(), zone.getId(), Network.GuestType.Isolated);
            if (virtualNetworks == null) {
                throw new InvalidParameterValueException("No (virtual) networks are found for account " + owner);
            }
            if (virtualNetworks.isEmpty()) {
                defaultNetwork = createDefaultNetworkForAccount(zone, owner, requiredOfferings);
            } else if (virtualNetworks.size() > 1 && !selectAny) {
                throw new InvalidParameterValueException("More than 1 default Isolated networks are found for account " + owner + "; please specify networkIds");
            } else {
                defaultNetwork = networkDao.findById(virtualNetworks.get(0).getId());
            }
        } else {
            throw new InvalidParameterValueException("Required network offering id=" + requiredOfferings.get(0).getId() + " is not in " + NetworkOffering.State.Enabled);
        }

        return defaultNetwork;
    }

    private NetworkVO createDefaultNetworkForAccount(DataCenter zone, Account owner, List<NetworkOfferingVO> requiredOfferings)
            throws InsufficientCapacityException, ResourceAllocationException {
        NetworkVO defaultNetwork = null;
        long physicalNetworkId = networkModel.findPhysicalNetworkId(zone.getId(), requiredOfferings.get(0).getTags(), requiredOfferings.get(0).getTrafficType());
        // Validate physical network
        PhysicalNetwork physicalNetwork = physicalNetworkDao.findById(physicalNetworkId);
        if (physicalNetwork == null) {
            throw new InvalidParameterValueException("Unable to find physical network with id: " + physicalNetworkId + " and tag: "
                    + requiredOfferings.get(0).getTags());
        }
        logger.debug("Creating network for account " + owner + " from the network offering id=" + requiredOfferings.get(0).getId() + " as a part of deployVM process");
        Network newNetwork = networkMgr.createGuestNetwork(requiredOfferings.get(0).getId(), owner.getAccountName() + "-network", owner.getAccountName() + "-network",
                null, null, null, false, null, owner, null, physicalNetwork, zone.getId(), ControlledEntity.ACLType.Account, null, null, null, null, true, null, null,
                null, null, null, null, null, null, null, null, null);
        if (newNetwork != null) {
            defaultNetwork = networkDao.findById(newNetwork.getId());
        }
        return defaultNetwork;
    }

    public ListResponse<UnmanagedInstanceResponse> listVmsForImport(ListVmsForImportCmd cmd) {
        final Account caller = CallContext.current().getCallingAccount();
        if (caller.getType() != Account.Type.ADMIN) {
            throw new PermissionDeniedException(String.format("Cannot perform this operation, Calling account is not root admin: %s", caller.getUuid()));
        }
        final Long zoneId = cmd.getZoneId();
        final DataCenterVO zone = dataCenterDao.findById(zoneId);
        if (zone == null) {
            throw new InvalidParameterValueException("Please specify a valid zone.");
        }
        final String hypervisorType = cmd.getHypervisor();
        if (!Hypervisor.HypervisorType.KVM.toString().equalsIgnoreCase(hypervisorType)) {
            throw new InvalidParameterValueException(String.format("VM Import is currently not supported for hypervisor: %s", hypervisorType));
        }

        String keyword = cmd.getKeyword();
        if (StringUtils.isNotEmpty(keyword)) {
            keyword = keyword.toLowerCase();
        }

        List<UnmanagedInstanceResponse> responses = new ArrayList<>();
        HashMap<String, UnmanagedInstanceTO> vmMap = getRemoteVmsOnKVMHost(zoneId, cmd.getHost(), cmd.getUsername(), cmd.getPassword());
        for (String key : vmMap.keySet()) {
            UnmanagedInstanceTO instance = vmMap.get(key);
            if (StringUtils.isNotEmpty(keyword) &&
                    !instance.getName().toLowerCase().contains(keyword)) {
                continue;
            }
            responses.add(createUnmanagedInstanceResponse(instance, null, null));
        }

        ListResponse<UnmanagedInstanceResponse> listResponses = new ListResponse<>();
        listResponses.setResponses(responses, responses.size());
        return listResponses;
    }

    private HashMap<String, UnmanagedInstanceTO> getRemoteVmsOnKVMHost(long zoneId, String remoteHostUrl, String username, String password) {
        //ToDo: add option to list one Vm by name
        List<HostVO> hosts = resourceManager.listAllUpAndEnabledHostsInOneZoneByHypervisor(Hypervisor.HypervisorType.KVM, zoneId);
        if (hosts.size() < 1) {
            throw new CloudRuntimeException("No hosts available to list VMs on remote host " + remoteHostUrl);
        }
        HostVO host = hosts.get(0);
        GetRemoteVmsCommand getRemoteVmsCommand = new GetRemoteVmsCommand(remoteHostUrl, username, password);
        Answer answer = agentManager.easySend(host.getId(), getRemoteVmsCommand);
        if (!(answer instanceof GetRemoteVmsAnswer)) {
            throw new CloudRuntimeException("Failed to list VMs, due to: " + answer.getDetails());
        }
        GetRemoteVmsAnswer getRemoteVmsAnswer = (GetRemoteVmsAnswer) answer;
        return getRemoteVmsAnswer.getUnmanagedInstances();
    }

    private String getInternalName(long accounId) {
        String instanceSuffix = configurationDao.getValue(Config.InstanceName.key());
        if (instanceSuffix == null) {
            instanceSuffix = "DEFAULT";
        }
        long vmId = userVmDao.getNextInSequence(Long.class, "id");
        return VirtualMachineName.getVmName(vmId, accounId, instanceSuffix);
    }

    @Override
    public String getConfigComponentName() {
        return UnmanagedVMsManagerImpl.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[]{
                UnmanageVMPreserveNic,
                RemoteKvmInstanceDisksCopyTimeout,
                ConvertVmwareInstanceToKvmTimeout,
                ThreadsOnMSToImportVMwareVMFiles,
                ThreadsOnKVMHostToImportVMwareVMFiles
        };
    }
}
