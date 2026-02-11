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

package org.apache.cloudstack.veeam.adapter;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.cloudstack.acl.Role;
import org.apache.cloudstack.acl.RolePermissionEntity;
import org.apache.cloudstack.acl.RoleService;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.acl.Rule;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiServerService;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.command.admin.vm.DeployVMCmdByAdmin;
import org.apache.cloudstack.api.command.user.job.QueryAsyncJobResultCmd;
import org.apache.cloudstack.api.command.user.network.ListNetworksCmd;
import org.apache.cloudstack.api.command.user.offering.ListServiceOfferingsCmd;
import org.apache.cloudstack.api.command.user.vm.AddNicToVMCmd;
import org.apache.cloudstack.api.command.user.vm.DeployVMCmd;
import org.apache.cloudstack.api.command.user.vm.DestroyVMCmd;
import org.apache.cloudstack.api.command.user.vm.ListVMsCmd;
import org.apache.cloudstack.api.command.user.vm.StartVMCmd;
import org.apache.cloudstack.api.command.user.vm.StopVMCmd;
import org.apache.cloudstack.api.command.user.vmsnapshot.CreateVMSnapshotCmd;
import org.apache.cloudstack.api.command.user.vmsnapshot.DeleteVMSnapshotCmd;
import org.apache.cloudstack.api.command.user.volume.AttachVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.CreateVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.DeleteVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.DetachVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.ListVolumesCmd;
import org.apache.cloudstack.api.command.user.volume.ResizeVolumeCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ServiceOfferingResponse;
import org.apache.cloudstack.backup.ImageTransfer.Direction;
import org.apache.cloudstack.backup.ImageTransfer.Format;
import org.apache.cloudstack.backup.ImageTransferVO;
import org.apache.cloudstack.backup.IncrementalBackupService;
import org.apache.cloudstack.backup.dao.ImageTransferDao;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.query.QueryService;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.veeam.api.converter.AsyncJobJoinVOToJobConverter;
import org.apache.cloudstack.veeam.api.converter.ClusterVOToClusterConverter;
import org.apache.cloudstack.veeam.api.converter.DataCenterJoinVOToDataCenterConverter;
import org.apache.cloudstack.veeam.api.converter.HostJoinVOToHostConverter;
import org.apache.cloudstack.veeam.api.converter.ImageTransferVOToImageTransferConverter;
import org.apache.cloudstack.veeam.api.converter.NetworkVOToNetworkConverter;
import org.apache.cloudstack.veeam.api.converter.NetworkVOToVnicProfileConverter;
import org.apache.cloudstack.veeam.api.converter.NicVOToNicConverter;
import org.apache.cloudstack.veeam.api.converter.StoreVOToStorageDomainConverter;
import org.apache.cloudstack.veeam.api.converter.UserVmJoinVOToVmConverter;
import org.apache.cloudstack.veeam.api.converter.VmSnapshotVOToSnapshotConverter;
import org.apache.cloudstack.veeam.api.converter.VolumeJoinVOToDiskConverter;
import org.apache.cloudstack.veeam.api.dto.Cluster;
import org.apache.cloudstack.veeam.api.dto.DataCenter;
import org.apache.cloudstack.veeam.api.dto.Disk;
import org.apache.cloudstack.veeam.api.dto.DiskAttachment;
import org.apache.cloudstack.veeam.api.dto.Host;
import org.apache.cloudstack.veeam.api.dto.ImageTransfer;
import org.apache.cloudstack.veeam.api.dto.Job;
import org.apache.cloudstack.veeam.api.dto.Network;
import org.apache.cloudstack.veeam.api.dto.Nic;
import org.apache.cloudstack.veeam.api.dto.Ref;
import org.apache.cloudstack.veeam.api.dto.ResourceAction;
import org.apache.cloudstack.veeam.api.dto.Snapshot;
import org.apache.cloudstack.veeam.api.dto.StorageDomain;
import org.apache.cloudstack.veeam.api.dto.Vm;
import org.apache.cloudstack.veeam.api.dto.VmAction;
import org.apache.cloudstack.veeam.api.dto.VnicProfile;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.cloud.api.query.dao.AsyncJobJoinDao;
import com.cloud.api.query.dao.DataCenterJoinDao;
import com.cloud.api.query.dao.HostJoinDao;
import com.cloud.api.query.dao.ImageStoreJoinDao;
import com.cloud.api.query.dao.StoragePoolJoinDao;
import com.cloud.api.query.dao.UserVmJoinDao;
import com.cloud.api.query.dao.VolumeJoinDao;
import com.cloud.api.query.vo.AsyncJobJoinVO;
import com.cloud.api.query.vo.DataCenterJoinVO;
import com.cloud.api.query.vo.HostJoinVO;
import com.cloud.api.query.vo.ImageStoreJoinVO;
import com.cloud.api.query.vo.StoragePoolJoinVO;
import com.cloud.api.query.vo.UserVmJoinVO;
import com.cloud.api.query.vo.VolumeJoinVO;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.offering.ServiceOffering;
import com.cloud.org.Grouping;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeApiService;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.User;
import com.cloud.user.UserAccount;
import com.cloud.user.dao.UserAccountDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.EnumUtils;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.NicVO;
import com.cloud.vm.UserVmService;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.snapshot.VMSnapshotService;
import com.cloud.vm.snapshot.VMSnapshotVO;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;

public class ServerAdapter extends ManagerBase {
    private static final String SERVICE_ACCOUNT_NAME = "veemserviceuser";
    private static final String SERVICE_ACCOUNT_ROLE_NAME = "Veeam Service Role";
    private static final String SERVICE_ACCOUNT_FIRST_NAME = "Veeam";
    private static final String SERVICE_ACCOUNT_LAST_NAME = "Service User";
    private static final List<Class<?>> SERVICE_ACCOUNT_ROLE_ALLOWED_APIS = Arrays.asList(
            QueryAsyncJobResultCmd.class,
            ListVMsCmd.class,
            DeployVMCmd.class,
            StartVMCmd.class,
            StopVMCmd.class,
            DestroyVMCmd.class,
            ListVolumesCmd.class,
            CreateVolumeCmd.class,
            DeleteVolumeCmd.class,
            AttachVolumeCmd.class,
            DetachVolumeCmd.class,
            ResizeVolumeCmd.class,
            ListNetworksCmd.class
    );
    public static final String GUEST_CPU_MODE = "host-passthrough";

    @Inject
    RoleService roleService;

    @Inject
    AccountService accountService;

    @Inject
    UserAccountDao userAccountDao;

    @Inject
    DataCenterDao dataCenterDao;

    @Inject
    DataCenterJoinDao dataCenterJoinDao;

    @Inject
    StoragePoolJoinDao storagePoolJoinDao;

    @Inject
    ImageStoreJoinDao imageStoreJoinDao;

    @Inject
    ClusterDao clusterDao;

    @Inject
    HostJoinDao hostJoinDao;

    @Inject
    NetworkDao networkDao;

    @Inject
    UserVmDao userVmDao;

    @Inject
    UserVmJoinDao userVmJoinDao;

    @Inject
    VolumeDao volumeDao;

    @Inject
    VolumeJoinDao volumeJoinDao;

    @Inject
    VolumeDetailsDao volumeDetailsDao;

    @Inject
    VolumeApiService volumeApiService;

    @Inject
    PrimaryDataStoreDao primaryDataStoreDao;

    @Inject
    ImageTransferDao imageTransferDao;

    @Inject
    IncrementalBackupService incrementalBackupService;

    @Inject
    QueryService queryService;

    @Inject
    ServiceOfferingDao serviceOfferingDao;

    @Inject
    UserVmService userVmService;

    @Inject
    NicDao nicDao;

    @Inject
    ApiServerService apiServerService;

    @Inject
    AsyncJobJoinDao asyncJobJoinDao;

    @Inject
    VMSnapshotDao vmSnapshotDao;

    @Inject
    VMSnapshotService vmSnapshotService;

    //ToDo: check access on objects

    protected Role createServiceAccountRole() {
        Role role = roleService.createRole(SERVICE_ACCOUNT_ROLE_NAME, RoleType.User,
                SERVICE_ACCOUNT_ROLE_NAME, false);
        for (Class<?> allowedApi : SERVICE_ACCOUNT_ROLE_ALLOWED_APIS) {
            final String apiName = BaseCmd.getCommandNameByClass(allowedApi);
            roleService.createRolePermission(role, new Rule(apiName), RolePermissionEntity.Permission.ALLOW,
                    String.format("Allow %s", apiName));
        }
        roleService.createRolePermission(role, new Rule("*"), RolePermissionEntity.Permission.DENY,
                "Deny all");
        logger.debug("Created default role for Veeam service account in projects: {}", role);
        return role;
    }

    public Role getServiceAccountRole() {
        List<Role> roles = roleService.findRolesByName(SERVICE_ACCOUNT_ROLE_NAME);
        if (CollectionUtils.isNotEmpty(roles)) {
            Role role = roles.get(0);
            logger.debug("Found default role for Veeam service account in projects: {}", role);
            return role;
        }
        return createServiceAccountRole();
    }

    protected UserAccount createServiceAccount() {
        CallContext.register(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM);
        try {
            Role role = getServiceAccountRole();
            UserAccount userAccount = accountService.createUserAccount(SERVICE_ACCOUNT_NAME,
                    UUID.randomUUID().toString(), SERVICE_ACCOUNT_FIRST_NAME,
                    SERVICE_ACCOUNT_LAST_NAME, null, null, SERVICE_ACCOUNT_NAME, Account.Type.NORMAL, role.getId(),
                    1L, null, null, null, null, User.Source.NATIVE);
            logger.debug("Created Veeam service account: {}", userAccount);
            return userAccount;
        } finally {
            CallContext.unregister();
        }
    }

    protected Pair<User, Account> createServiceAccountIfNeeded() {
        UserAccount userAccount = accountService.getActiveUserAccount(SERVICE_ACCOUNT_NAME, 1L);
        if (userAccount == null) {
            userAccount =  createServiceAccount();
        } else {
            logger.debug("Veeam service user account found: {}", userAccount);
        }
        return new Pair<>(accountService.getActiveUser(userAccount.getId()),
                accountService.getActiveAccountById(userAccount.getAccountId()));
    }

    @Override
    public boolean start() {
        createServiceAccountIfNeeded();
        //find public custom disk offering
        return true;
    }

    public List<DataCenter> listAllDataCenters() {
        final List<DataCenterJoinVO> clusters = dataCenterJoinDao.listAll();
        return DataCenterJoinVOToDataCenterConverter.toDCList(clusters);
    }

    public DataCenter getDataCenter(String uuid) {
        final DataCenterJoinVO vo = dataCenterJoinDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("DataCenter with ID " + uuid + " not found");
        }
        return  DataCenterJoinVOToDataCenterConverter.toDataCenter(vo);
    }

    public List<StorageDomain> listStorageDomainsByDcId(final String uuid) {
        final DataCenterJoinVO dataCenterVO = dataCenterJoinDao.findByUuid(uuid);
        if (dataCenterVO == null) {
            throw new InvalidParameterValueException("DataCenter with ID " + uuid + " not found");
        }
        List<StoragePoolJoinVO> storagePoolVOS = storagePoolJoinDao.listAll();
        List<StorageDomain> storageDomains = StoreVOToStorageDomainConverter.toStorageDomainListFromPools(storagePoolVOS);
        List<ImageStoreJoinVO> imageStoreJoinVOS = imageStoreJoinDao.listAll();
        storageDomains.addAll(StoreVOToStorageDomainConverter.toStorageDomainListFromStores(imageStoreJoinVOS));
        return storageDomains;
    }

    public List<Network> listNetworksByDcId(final String uuid) {
        final DataCenterJoinVO dataCenterVO = dataCenterJoinDao.findByUuid(uuid);
        if (dataCenterVO == null) {
            throw new InvalidParameterValueException("DataCenter with ID " + uuid + " not found");
        }
        List<NetworkVO> networks = networkDao.listAll();
        return NetworkVOToNetworkConverter.toNetworkList(networks, (dcId) -> dataCenterVO);
    }

    public List<Cluster> listAllClusters() {
        final List<ClusterVO> clusters = clusterDao.listAll();
        return ClusterVOToClusterConverter.toClusterList(clusters, this::getZoneById);
    }

    public Cluster getCluster(String uuid) {
        final ClusterVO vo = clusterDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("Cluster with ID " + uuid + " not found");
        }
        return  ClusterVOToClusterConverter.toCluster(vo, this::getZoneById);
    }

    public List<Host> listAllHosts() {
        final List<HostJoinVO> hosts = hostJoinDao.listAll();
        return HostJoinVOToHostConverter.toHostList(hosts);
    }

    public Host getHost(String uuid) {
        final HostJoinVO vo = hostJoinDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("Host with ID " + uuid + " not found");
        }
        return  HostJoinVOToHostConverter.toHost(vo);
    }

    public List<Network> listAllNetworks() {
        final List<NetworkVO> networks = networkDao.listAll();
        return NetworkVOToNetworkConverter.toNetworkList(networks, this::getZoneById);
    }

    public Network getNetwork(String uuid) {
        final NetworkVO vo = networkDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("Host with ID " + uuid + " not found");
        }
        return NetworkVOToNetworkConverter.toNetwork(vo, this::getZoneById);
    }

    public List<VnicProfile> listAllVnicProfiles() {
        final List<NetworkVO> networks = networkDao.listAll();
        return NetworkVOToVnicProfileConverter.toVnicProfileList(networks, this::getZoneById);
    }

    public VnicProfile getVnicProfile(String uuid) {
        final NetworkVO vo = networkDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("Host with ID " + uuid + " not found");
        }
        return NetworkVOToVnicProfileConverter.toVnicProfile(vo, this::getZoneById);
    }

    public List<Vm> listAllInstances() {
        // Todo: add filtering, pagination
        List<UserVmJoinVO> vms = userVmJoinDao.listAll();
        return UserVmJoinVOToVmConverter.toVmList(vms, this::getHostById);
    }

    public Vm getInstance(String uuid) {
        UserVmJoinVO vo = userVmJoinDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("VM with ID " + uuid + " not found");
        }
        return UserVmJoinVOToVmConverter.toVm(vo, this::getHostById, this::listDiskAttachmentsByInstanceId,
                this::listNicsByInstance);
    }

    public Vm createInstance(Vm request) {
        if (request == null) {
            throw new InvalidParameterValueException("Request disk data is empty");
        }
        String name = request.name;
        Long zoneId = null;
        Long clusterId = null;
        if (request.cluster != null && StringUtils.isNotEmpty(request.cluster.id)) {
            ClusterVO clusterVO = clusterDao.findByUuid(request.cluster.id);
            if (clusterVO != null) {
                zoneId = clusterVO.getDataCenterId();
                clusterId = clusterVO.getId();
            }
        }
        if (zoneId == null) {
            throw new InvalidParameterValueException("Failed to determine datacenter for VM creation request");
        }
        Integer cpu = null;
        try {
            cpu = request.cpu.topology.sockets;
        } catch (Exception ignored) {}
        if (cpu == null) {
            throw new InvalidParameterValueException("CPU topology sockets must be specified");
        }
        Long memory = null;
        try {
            memory = Long.valueOf(request.memory);
        } catch (Exception ignored) {}
        if (memory == null) {
            throw new InvalidParameterValueException("Memory must be specified");
        }
        String userdata = null;
        if (request.getInitialization() != null) {
            userdata = request.getInitialization().getCustomScript();
        }
        ApiConstants.BootType bootType = ApiConstants.BootType.BIOS;
        ApiConstants.BootMode bootMode = ApiConstants.BootMode.LEGACY;
        if (request.bios != null && StringUtils.isNotEmpty(request.bios.type) && request.bios.type.contains("secure")) {
            bootType = ApiConstants.BootType.UEFI;
            bootMode = ApiConstants.BootMode.SECURE;
        }
        Pair<User, Account> serviceUserAccount = createServiceAccountIfNeeded();
        CallContext ctx = CallContext.register(serviceUserAccount.first(), serviceUserAccount.second());
        try {
            return createInstance(zoneId, clusterId, name, cpu, memory, userdata, bootType, bootMode);
        } finally {
            CallContext.unregister();
        }
    }

    protected ServiceOffering getServiceOfferingIdForVmCreation(long zoneId, int cpu, long memory) {
        ListServiceOfferingsCmd cmd = new ListServiceOfferingsCmd();
        ComponentContext.inject(cmd);
        cmd.setZoneId(zoneId);
        cmd.setCpuNumber(cpu);
        Integer memoryMB = (int)(memory / (1024L * 1024L));
        cmd.setMemory(memoryMB);
        ListResponse<ServiceOfferingResponse> offerings = queryService.searchForServiceOfferings(cmd);
        if (offerings.getResponses().isEmpty()) {
            return null;
        }
        String uuid = offerings.getResponses().get(0).getId();
        return serviceOfferingDao.findByUuid(uuid);
    }

    protected Vm createInstance(Long zoneId, Long clusterId, String name, int cpu, long memory, String userdata,
                                ApiConstants.BootType bootType, ApiConstants.BootMode bootMode) {
        ServiceOffering serviceOffering = getServiceOfferingIdForVmCreation(zoneId, cpu, memory);
        if (serviceOffering == null) {
            throw new CloudRuntimeException("No service offering found for VM creation with specified CPU and memory");
        }
        DeployVMCmdByAdmin cmd = new DeployVMCmdByAdmin();
        cmd.setHttpMethod(BaseCmd.HTTPMethod.POST.name());
        ComponentContext.inject(cmd);
        cmd.setZoneId(zoneId);
        cmd.setClusterId(clusterId);
        cmd.setName(name);
        cmd.setServiceOfferingId(serviceOffering.getId());
        if (StringUtils.isNotEmpty(userdata)) {
            cmd.setUserData(Base64.getEncoder().encodeToString(userdata.getBytes(StandardCharsets.UTF_8)));
        }
        if (bootType != null) {
            cmd.setBootType(bootType.toString());
        }
        if (bootMode != null) {
            cmd.setBootMode(bootMode.toString());
        }
        // ToDo: handle other.
        cmd.setHypervisor(Hypervisor.HypervisorType.KVM.name());
        cmd.setBlankInstance(true);
        Map<String, String> details = new HashMap<>();
        details.put(VmDetailConstants.GUEST_CPU_MODE, GUEST_CPU_MODE);
        Map<Integer, Map<String, String>> map = new HashMap<>();
        map.put(0, details);
        cmd.setDetails(map);
        try {
            UserVm vm = userVmService.createVirtualMachine(cmd);
            vm = userVmService.finalizeCreateVirtualMachine(vm.getId());
            UserVmJoinVO vo = userVmJoinDao.findById(vm.getId());
            return UserVmJoinVOToVmConverter.toVm(vo, this::getHostById, this::listDiskAttachmentsByInstanceId,
                    this::listNicsByInstance);
        } catch (InsufficientCapacityException | ResourceUnavailableException | ResourceAllocationException | CloudRuntimeException e) {
            throw new CloudRuntimeException("Failed to create VM: " + e.getMessage(), e);
        }
    }

    public Vm updateInstance(String uuid, Vm request) {
        return getInstance(uuid);
    }

    public void deleteInstance(String uuid) {
        UserVmVO vo = userVmDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("VM with ID " + uuid + " not found");
        }
        try {
            userVmService.destroyVm(vo.getId(), true);
        } catch (ResourceUnavailableException e) {
            throw new CloudRuntimeException("Failed to delete VM: " + e.getMessage(), e);
        }
    }

    public VmAction startInstance(String uuid) {
        UserVmVO vo = userVmDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("VM with ID " + uuid + " not found");
        }
        Pair<User, Account> serviceUserAccount = createServiceAccountIfNeeded();
        CallContext ctx = CallContext.register(serviceUserAccount.first(), serviceUserAccount.second());
        try {
            StartVMCmd cmd = new StartVMCmd();
            cmd.setHttpMethod(BaseCmd.HTTPMethod.POST.name());
            ComponentContext.inject(cmd);
            Map<String, String> params = new HashMap<>();
            params.put(ApiConstants.ID, vo.getUuid());
            ApiServerService.AsyncCmdResult result =
                    apiServerService.processAsyncCmd(cmd, params, ctx, serviceUserAccount.first().getId(),
                            serviceUserAccount.second());
            AsyncJobJoinVO asyncJobJoinVO = asyncJobJoinDao.findById(result.jobId);
            return AsyncJobJoinVOToJobConverter.toVmAction(asyncJobJoinVO, userVmJoinDao.findById(vo.getId()));
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to start VM: " + e.getMessage(), e);
        } finally {
            CallContext.unregister();
        }
    }

    public VmAction stopInstance(String uuid) {
        UserVmVO vo = userVmDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("VM with ID " + uuid + " not found");
        }
        Pair<User, Account> serviceUserAccount = createServiceAccountIfNeeded();
        CallContext ctx = CallContext.register(serviceUserAccount.first(), serviceUserAccount.second());
        try {
            StopVMCmd cmd = new StopVMCmd();
            cmd.setHttpMethod(BaseCmd.HTTPMethod.POST.name());
            ComponentContext.inject(cmd);
            Map<String, String> params = new HashMap<>();
            params.put(ApiConstants.ID, vo.getUuid());
            params.put(ApiConstants.FORCED, Boolean.TRUE.toString());
            ApiServerService.AsyncCmdResult result =
                    apiServerService.processAsyncCmd(cmd, params, ctx, serviceUserAccount.first().getId(),
                            serviceUserAccount.second());
            AsyncJobJoinVO asyncJobJoinVO = asyncJobJoinDao.findById(result.jobId);
            return AsyncJobJoinVOToJobConverter.toVmAction(asyncJobJoinVO, userVmJoinDao.findById(vo.getId()));
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to stop VM: " + e.getMessage(), e);
        } finally {
            CallContext.unregister();
        }
    }

    public VmAction shutdownInstance(String uuid) {
        UserVmVO vo = userVmDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("VM with ID " + uuid + " not found");
        }
        Pair<User, Account> serviceUserAccount = createServiceAccountIfNeeded();
        CallContext ctx = CallContext.register(serviceUserAccount.first(), serviceUserAccount.second());
        try {
            StopVMCmd cmd = new StopVMCmd();
            cmd.setHttpMethod(BaseCmd.HTTPMethod.POST.name());
            ComponentContext.inject(cmd);
            Map<String, String> params = new HashMap<>();
            params.put(ApiConstants.ID, vo.getUuid());
            params.put(ApiConstants.FORCED, Boolean.FALSE.toString());
            ApiServerService.AsyncCmdResult result =
                    apiServerService.processAsyncCmd(cmd, params, ctx, serviceUserAccount.first().getId(),
                            serviceUserAccount.second());
            AsyncJobJoinVO asyncJobJoinVO = asyncJobJoinDao.findById(result.jobId);
            return AsyncJobJoinVOToJobConverter.toVmAction(asyncJobJoinVO, userVmJoinDao.findById(vo.getId()));
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to shutdown VM: " + e.getMessage(), e);
        } finally {
            CallContext.unregister();
        }
    }

    public List<Disk> listAllDisks() {
        List<VolumeJoinVO> kvmVolumes = volumeJoinDao.listByHypervisor(Hypervisor.HypervisorType.KVM);
        return VolumeJoinVOToDiskConverter.toDiskList(kvmVolumes);
    }

    public Disk getDisk(String uuid) {
        VolumeJoinVO vo = volumeJoinDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("Disk with ID " + uuid + " not found");
        }
        return VolumeJoinVOToDiskConverter.toDisk(vo);
    }

    protected List<DiskAttachment> listDiskAttachmentsByInstanceId(final long instanceId) {
        List<VolumeJoinVO> kvmVolumes = volumeJoinDao.listByInstanceId(instanceId);
        return VolumeJoinVOToDiskConverter.toDiskAttachmentList(kvmVolumes);
    }

    public List<DiskAttachment> listDiskAttachmentsByInstanceUuid(final String uuid) {
        UserVmVO vo = userVmDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("VM with ID " + uuid + " not found");
        }
        return listDiskAttachmentsByInstanceId(vo.getId());
    }

    public DiskAttachment handleInstanceAttachDisk(final String vmUuid, final DiskAttachment request) {
        UserVmVO vmVo = userVmDao.findByUuid(vmUuid);
        if (vmVo == null) {
            throw new InvalidParameterValueException("VM with ID " + vmUuid + " not found");
        }
        if (request == null || request.disk == null || StringUtils.isEmpty(request.disk.id)) {
            throw new InvalidParameterValueException("Request disk data is empty");
        }
        VolumeVO volumeVO = volumeDao.findByUuid(request.disk.id);
        if (volumeVO == null) {
            throw new InvalidParameterValueException("Disk with ID " + request.disk.id + " not found");
        }
        Pair<User, Account> serviceUserAccount = createServiceAccountIfNeeded();
        CallContext.register(serviceUserAccount.first(), serviceUserAccount.second());
        try {
            Volume volume = volumeApiService.attachVolumeToVM(vmVo.getId(), volumeVO.getId(), 0L, false);
            VolumeJoinVO attachedVolumeVO = volumeJoinDao.findById(volume.getId());
            return VolumeJoinVOToDiskConverter.toDiskAttachment(attachedVolumeVO);
        } finally {
            CallContext.unregister();
        }
    }

    public void deleteDisk(String uuid) {
        VolumeVO vo = volumeDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("Disk with ID " + uuid + " not found");
        }
        volumeApiService.deleteVolume(vo.getId(), accountService.getSystemAccount());
    }

    public Disk handleCreateDisk(Disk request) {
        if (request == null) {
            throw new InvalidParameterValueException("Request disk data is empty");
        }
        String name = request.name;
        if (StringUtils.isBlank(name) && !name.startsWith("Veeam_KvmBackupDisk_")) {
            throw new InvalidParameterValueException("Only worker VM disk creation is supported");
        }
        if (request.storageDomains == null || CollectionUtils.isEmpty(request.storageDomains.storageDomain) ||
                request.storageDomains.storageDomain.size() > 1) {
            throw new InvalidParameterValueException("Exactly one storage domain must be specified");
        }
        Ref domain = request.storageDomains.storageDomain.get(0);
        if (domain == null || domain.id == null) {
            throw new InvalidParameterValueException("Storage domain ID must be specified");
        }
        StoragePoolVO pool = primaryDataStoreDao.findByUuid(domain.id);
        if (pool == null) {
            throw new InvalidParameterValueException("Storage domain with ID " + domain.id + " not found");
        }
        String sizeStr = request.provisionedSize;
        if (StringUtils.isBlank(sizeStr)) {
            throw new InvalidParameterValueException("Provisioned size must be specified");
        }
        long provisionedSizeInGb;
        try {
            provisionedSizeInGb = Long.parseLong(sizeStr);
        } catch (NumberFormatException ex) {
            throw new InvalidParameterValueException("Invalid provisioned size: " + sizeStr);
        }
        if (provisionedSizeInGb <= 0) {
            throw new InvalidParameterValueException("Provisioned size must be greater than zero");
        }
        provisionedSizeInGb = Math.max(1L, provisionedSizeInGb / (1024L * 1024L * 1024L));
        Long initialSize = null;
        if (StringUtils.isNotBlank(request.initialSize)) {
            try {
                initialSize = Long.parseLong(request.initialSize);
            } catch (NumberFormatException ignored) {}
        }
        Pair<User, Account> serviceUserAccount = createServiceAccountIfNeeded();
        Account serviceAccount = serviceUserAccount.second();
        DataCenterVO zone = dataCenterDao.findById(pool.getDataCenterId());
        if (zone == null || !Grouping.AllocationState.Enabled.equals(zone.getAllocationState())) {
            throw new InvalidParameterValueException("Datacenter for the specified storage domain is not found or not active");
        }
        Long diskOfferingId = volumeApiService.getCustomDiskOfferingIdForVolumeUpload(serviceAccount, zone);
        if (diskOfferingId == null) {
            throw new CloudRuntimeException("Failed to find custom offering for disk" + zone.getName());
        }
        CallContext ctx = CallContext.register(serviceUserAccount.first(), serviceUserAccount.second());
        try {
            return createDisk(serviceAccount, pool, name, diskOfferingId, provisionedSizeInGb, initialSize);
        } finally {
            CallContext.unregister();
        }
    }

    @NotNull
    private Disk createDisk(Account serviceAccount, StoragePoolVO pool, String name, Long diskOfferingId, long sizeInGb, Long initialSize) {
        Volume volume;
        try {
            volume = volumeApiService.allocVolume(serviceAccount.getId(), pool.getDataCenterId(), diskOfferingId, null,
                    null, name, sizeInGb, null, null, null, null);
        } catch (ResourceAllocationException e) {
            throw new CloudRuntimeException(e.getMessage(), e);
        }
        if (volume == null) {
            throw new CloudRuntimeException("Failed to create volume");
        }
        volume = volumeApiService.createVolume(volume.getId(), null, null, pool.getId(), true);
        if (initialSize != null) {
            volumeDetailsDao.addDetail(volume.getId(), ApiConstants.VIRTUAL_SIZE, String.valueOf(initialSize), true);
        }

        // Implementation for creating a Disk resource
        return VolumeJoinVOToDiskConverter.toDisk(volumeJoinDao.findById(volume.getId()));
    }

    protected List<Nic> listNicsByInstance(final long instanceId, final String instanceUuid) {
        List<NicVO> nics = nicDao.listByVmId(instanceId);
        return NicVOToNicConverter.toNicList(nics, instanceUuid, this::getNetworkById);
    }

    protected List<Nic> listNicsByInstance(final UserVmJoinVO vo) {
        return listNicsByInstance(vo.getId(), vo.getUuid());
    }

    public List<Nic> listNicsByInstanceUuid(final String uuid) {
        UserVmVO vo = userVmDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("VM with ID " + uuid + " not found");
        }
        return listNicsByInstance(vo.getId(), vo.getUuid());
    }

    public Nic handleAttachInstanceNic(final String vmUuid, final Nic request) {
        UserVmVO vmVo = userVmDao.findByUuid(vmUuid);
        if (vmVo == null) {
            throw new InvalidParameterValueException("VM with ID " + vmUuid + " not found");
        }
        if (request == null || request.getVnicProfile() == null || StringUtils.isEmpty(request.getVnicProfile().id)) {
            throw new InvalidParameterValueException("Request nic data is empty");
        }
        NetworkVO networkVO = networkDao.findByUuid(request.getVnicProfile().id);
        if (networkVO == null) {
            throw new InvalidParameterValueException("VNic profile " + request.getVnicProfile().id+ " not found");
        }
        Pair<User, Account> serviceUserAccount = createServiceAccountIfNeeded();
        CallContext.register(serviceUserAccount.first(), serviceUserAccount.second());
        try {
            AddNicToVMCmd cmd = new AddNicToVMCmd();
            ComponentContext.inject(cmd);
            cmd.setVmId(vmVo.getId());
            cmd.setNetworkId(networkVO.getId());
            if (request.getMac() != null && StringUtils.isNotBlank(request.getMac().getAddress())) {
                cmd.setMacAddress(request.getMac().getAddress());
            }
            userVmService.addNicToVirtualMachine(cmd);
            NicVO nic = nicDao.findByInstanceIdAndNetworkIdIncludingRemoved(networkVO.getId(), vmVo.getId());
            if (nic == null) {
                throw new CloudRuntimeException("Failed to attach NIC to VM");
            }
            return NicVOToNicConverter.toNic(nic, vmUuid, this::getNetworkById);
        } finally {
            CallContext.unregister();
        }
    }

    public List<ImageTransfer> listAllImageTransfers() {
        List<ImageTransferVO> imageTransfers = imageTransferDao.listAll();
        return ImageTransferVOToImageTransferConverter.toImageTransferList(imageTransfers, this::getHostById, this::getVolumeById);
    }

    public ImageTransfer getImageTransfer(String uuid) {
        ImageTransferVO vo = imageTransferDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("Image transfer with ID " + uuid + " not found");
        }
        return ImageTransferVOToImageTransferConverter.toImageTransfer(vo, this::getHostById, this::getVolumeById);
    }

    public ImageTransfer handleCreateImageTransfer(ImageTransfer request) {
        if (request == null) {
            throw new InvalidParameterValueException("Request image transfer data is empty");
        }
        if (request.getDisk() == null || StringUtils.isBlank(request.getDisk().id)) {
            throw new InvalidParameterValueException("Disk ID must be specified");
        }
        VolumeJoinVO volumeVO = volumeJoinDao.findByUuid(request.getDisk().id);
        if (volumeVO == null) {
            throw new InvalidParameterValueException("Disk with ID " + request.getDisk().id + " not found");
        }
        Direction direction = EnumUtils.fromString(Direction.class, request.getDirection());
        if (direction == null) {
            throw new InvalidParameterValueException("Invalid or missing direction");
        }
        Format format = EnumUtils.fromString(Format.class, request.getFormat());
        return createImageTransfer(null, volumeVO.getId(), direction, format);
    }

    public boolean handleCancelImageTransfer(String uuid) {
        ImageTransferVO vo = imageTransferDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("Image transfer with ID " + uuid + " not found");
        }
        return incrementalBackupService.cancelImageTransfer(vo.getId());
    }

    public boolean handleFinalizeImageTransfer(String uuid) {
        ImageTransferVO vo = imageTransferDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("Image transfer with ID " + uuid + " not found");
        }
        return incrementalBackupService.finalizeImageTransfer(vo.getId());
    }

    private ImageTransfer createImageTransfer(Long backupId, Long volumeId, Direction direction, Format format) {
        Pair<User, Account> serviceUserAccount = createServiceAccountIfNeeded();
        CallContext.register(serviceUserAccount.first(), serviceUserAccount.second());
        try {
            org.apache.cloudstack.backup.ImageTransfer imageTransfer =
                    incrementalBackupService.createImageTransfer(volumeId, null, direction, format);
            ImageTransferVO imageTransferVO = imageTransferDao.findById(imageTransfer.getId());
            return ImageTransferVOToImageTransferConverter.toImageTransfer(imageTransferVO, this::getHostById, this::getVolumeById);
        } finally {
            CallContext.unregister();
        }
    }

    protected DataCenterJoinVO getZoneById(Long zoneId) {
        if (zoneId == null) {
            return null;
        }
        return dataCenterJoinDao.findById(zoneId);
    }

    private HostJoinVO getHostById(Long hostId) {
        if (hostId == null) {
            return null;
        }
        return hostJoinDao.findById(hostId);
    }

    private VolumeJoinVO getVolumeById(Long volumeId) {
        if (volumeId == null) {
            return null;
        }
        return volumeJoinDao.findById(volumeId);
    }

    protected NetworkVO getNetworkById(Long networkId) {
        if (networkId == null) {
            return null;
        }
        return networkDao.findById(networkId);
    }

    public List<Job> listAllJobs() {
        // ToDo: find active jobs for service account
        return Collections.emptyList();
    }

    public Job getJob(String uuid) {
        final AsyncJobJoinVO vo = asyncJobJoinDao.findByUuidIncludingRemoved(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("Job with ID " + uuid + " not found");
        }
        return AsyncJobJoinVOToJobConverter.toJob(vo);
    }

    public List<Snapshot> listSnapshotsByInstanceUuid(final String uuid) {
        UserVmVO vo = userVmDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("VM with ID " + uuid + " not found");
        }
        List<VMSnapshotVO> snapshots = vmSnapshotDao.findByVm(vo.getId());
        return VmSnapshotVOToSnapshotConverter.toSnapshotList(snapshots, vo.getUuid());
    }

    public Snapshot handleCreateInstanceSnapshot(final String vmUuid, final Snapshot request) {
        UserVmVO vmVo = userVmDao.findByUuid(vmUuid);
        if (vmVo == null) {
            throw new InvalidParameterValueException("VM with ID " + vmUuid + " not found");
        }
        Pair<User, Account> serviceUserAccount = createServiceAccountIfNeeded();
        CallContext ctx = CallContext.register(serviceUserAccount.first(), serviceUserAccount.second());
        try {
            CreateVMSnapshotCmd cmd = new CreateVMSnapshotCmd();
            ComponentContext.inject(cmd);
            Map<String, String> params = new HashMap<>();
            params.put(ApiConstants.VIRTUAL_MACHINE_ID, vmVo.getUuid());
            params.put(ApiConstants.VM_SNAPSHOT_DESCRIPTION, request.getDescription());
            params.put(ApiConstants.VM_SNAPSHOT_MEMORY, String.valueOf(Boolean.parseBoolean(request.getPersistMemorystate())));
            ApiServerService.AsyncCmdResult result =
                    apiServerService.processAsyncCmd(cmd, params, ctx, serviceUserAccount.first().getId(),
                            serviceUserAccount.second());
            if (result.objectId == null) {
                throw new CloudRuntimeException("No snapshot ID returned");
            }
            VMSnapshotVO vo = vmSnapshotDao.findById(result.objectId);
            if (vo == null) {
                throw new CloudRuntimeException("Snapshot not found");
            }
            return VmSnapshotVOToSnapshotConverter.toSnapshot(vo, vmVo.getUuid());
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to create snapshot: " + e.getMessage(), e);
        } finally {
            CallContext.unregister();
        }
    }

    public Snapshot getSnapshot(String uuid) {
        VMSnapshotVO vo = vmSnapshotDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("Snapshot with ID " + uuid + " not found");
        }
        UserVmVO vm = userVmDao.findById(vo.getVmId());
        return VmSnapshotVOToSnapshotConverter.toSnapshot(vo, vm.getUuid());
    }

    public ResourceAction deleteSnapshot(String uuid, boolean async) {
        ResourceAction action = null;
        VMSnapshotVO vo = vmSnapshotDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("Snapshot with ID " + uuid + " not found");
        }
        Pair<User, Account> serviceUserAccount = createServiceAccountIfNeeded();
        CallContext ctx = CallContext.register(serviceUserAccount.first(), serviceUserAccount.second());
        try {
            if (async) {
                DeleteVMSnapshotCmd cmd = new DeleteVMSnapshotCmd();
                ComponentContext.inject(cmd);
                Map<String, String> params = new HashMap<>();
                params.put(ApiConstants.VM_SNAPSHOT_ID, vo.getUuid());
                ApiServerService.AsyncCmdResult result =
                        apiServerService.processAsyncCmd(cmd, params, ctx, serviceUserAccount.first().getId(),
                                serviceUserAccount.second());
                AsyncJobJoinVO jobVo = asyncJobJoinDao.findById(result.jobId);
                if (jobVo == null) {
                    throw new CloudRuntimeException("Failed to find job for snapshot deletion");
                }
                action = AsyncJobJoinVOToJobConverter.toAction(jobVo);
            } else {
                vmSnapshotService.deleteVMSnapshot(vo.getId());
            }
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to delete snapshot: " + e.getMessage(), e);
        } finally {
            CallContext.unregister();
        }
        return action;
    }
}
