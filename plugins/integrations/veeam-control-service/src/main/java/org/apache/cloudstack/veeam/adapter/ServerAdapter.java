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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.cloudstack.acl.Role;
import org.apache.cloudstack.acl.RolePermissionEntity;
import org.apache.cloudstack.acl.RoleService;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.acl.Rule;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiServerService;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.command.admin.backup.DeleteVmCheckpointCmd;
import org.apache.cloudstack.api.command.admin.backup.FinalizeBackupCmd;
import org.apache.cloudstack.api.command.admin.backup.StartBackupCmd;
import org.apache.cloudstack.api.command.admin.vm.AssignVMCmd;
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
import org.apache.cloudstack.api.command.user.vmsnapshot.RevertToVMSnapshotCmd;
import org.apache.cloudstack.api.command.user.volume.AssignVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.AttachVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.CreateVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.DeleteVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.DetachVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.ListVolumesCmd;
import org.apache.cloudstack.api.command.user.volume.ResizeVolumeCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ServiceOfferingResponse;
import org.apache.cloudstack.backup.BackupVO;
import org.apache.cloudstack.backup.ImageTransfer.Direction;
import org.apache.cloudstack.backup.ImageTransfer.Format;
import org.apache.cloudstack.backup.ImageTransferVO;
import org.apache.cloudstack.backup.IncrementalBackupService;
import org.apache.cloudstack.backup.dao.BackupDao;
import org.apache.cloudstack.backup.dao.ImageTransferDao;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.jobs.dao.AsyncJobDao;
import org.apache.cloudstack.framework.jobs.impl.AsyncJobVO;
import org.apache.cloudstack.query.QueryService;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.veeam.VeeamControlService;
import org.apache.cloudstack.veeam.api.TagsRouteHandler;
import org.apache.cloudstack.veeam.api.converter.AsyncJobJoinVOToJobConverter;
import org.apache.cloudstack.veeam.api.converter.BackupVOToBackupConverter;
import org.apache.cloudstack.veeam.api.converter.ClusterVOToClusterConverter;
import org.apache.cloudstack.veeam.api.converter.DataCenterJoinVOToDataCenterConverter;
import org.apache.cloudstack.veeam.api.converter.HostJoinVOToHostConverter;
import org.apache.cloudstack.veeam.api.converter.ImageTransferVOToImageTransferConverter;
import org.apache.cloudstack.veeam.api.converter.NetworkVOToNetworkConverter;
import org.apache.cloudstack.veeam.api.converter.NetworkVOToVnicProfileConverter;
import org.apache.cloudstack.veeam.api.converter.NicVOToNicConverter;
import org.apache.cloudstack.veeam.api.converter.ResourceTagVOToTagConverter;
import org.apache.cloudstack.veeam.api.converter.StoreVOToStorageDomainConverter;
import org.apache.cloudstack.veeam.api.converter.UserVmJoinVOToVmConverter;
import org.apache.cloudstack.veeam.api.converter.UserVmVOToCheckpointConverter;
import org.apache.cloudstack.veeam.api.converter.VmSnapshotVOToSnapshotConverter;
import org.apache.cloudstack.veeam.api.converter.VolumeJoinVOToDiskConverter;
import org.apache.cloudstack.veeam.api.dto.Backup;
import org.apache.cloudstack.veeam.api.dto.BaseDto;
import org.apache.cloudstack.veeam.api.dto.Checkpoint;
import org.apache.cloudstack.veeam.api.dto.Cluster;
import org.apache.cloudstack.veeam.api.dto.DataCenter;
import org.apache.cloudstack.veeam.api.dto.Disk;
import org.apache.cloudstack.veeam.api.dto.DiskAttachment;
import org.apache.cloudstack.veeam.api.dto.Host;
import org.apache.cloudstack.veeam.api.dto.ImageTransfer;
import org.apache.cloudstack.veeam.api.dto.Job;
import org.apache.cloudstack.veeam.api.dto.Network;
import org.apache.cloudstack.veeam.api.dto.Nic;
import org.apache.cloudstack.veeam.api.dto.OvfXmlUtil;
import org.apache.cloudstack.veeam.api.dto.ResourceAction;
import org.apache.cloudstack.veeam.api.dto.Snapshot;
import org.apache.cloudstack.veeam.api.dto.StorageDomain;
import org.apache.cloudstack.veeam.api.dto.Tag;
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
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.offering.ServiceOffering;
import com.cloud.org.Grouping;
import com.cloud.projects.Project;
import com.cloud.projects.ProjectService;
import com.cloud.server.ResourceTag;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeApiService;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.tags.ResourceTagVO;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.User;
import com.cloud.user.UserAccount;
import com.cloud.uservm.UserVm;
import com.cloud.utils.EnumUtils;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.NicVO;
import com.cloud.vm.UserVmService;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.snapshot.VMSnapshotVO;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;

// ToDo: fix list APIs to support pagination, etc
// ToDo: check access on objects

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
    AsyncJobDao asyncJobDao;

    @Inject
    AsyncJobJoinDao asyncJobJoinDao;

    @Inject
    VMSnapshotDao vmSnapshotDao;

    @Inject
    BackupDao backupDao;

    @Inject
    ResourceTagDao resourceTagDao;

    @Inject
    NetworkModel networkModel;

    @Inject
    ProjectService projectService;

    protected static Tag getDummyTagByName(String name) {
        Tag tag = new Tag();
        String id = UUID.nameUUIDFromBytes(String.format("veeam:%s", name.toLowerCase()).getBytes()).toString();
        tag.setId(id);
        tag.setName(name);
        tag.setDescription(String.format("Default %s tag", name.toLowerCase()));
        tag.setHref(VeeamControlService.ContextPath.value() + TagsRouteHandler.BASE_ROUTE + "/" + id);
        tag.setParent(ResourceTagVOToTagConverter.getRootTagRef());
        return tag;
    }

    protected static Map<String, Tag> getDummyTags() {
        Map<String, Tag> tags = new HashMap<>();
        Tag tag1 = getDummyTagByName("Automatic");
        tags.put(tag1.getId(), tag1);
        Tag tag2 = getDummyTagByName("Manual");
        tags.put(tag2.getId(), tag2);
        return tags;
    }

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

    protected Pair<User, Account> getDefaultServiceAccount() {
        UserAccount userAccount = accountService.getActiveUserAccount(SERVICE_ACCOUNT_NAME, 1L);
        if (userAccount == null) {
            userAccount =  createServiceAccount();
        } else {
            logger.debug("Veeam service user account found: {}", userAccount);
        }
        return new Pair<>(accountService.getActiveUser(userAccount.getId()),
                accountService.getActiveAccountById(userAccount.getAccountId()));
    }

    protected Pair<User, Account> getServiceAccount() {
        String serviceAccountUuid = VeeamControlService.ServiceAccountId.value();
        if (StringUtils.isEmpty(serviceAccountUuid)) {
            throw new CloudRuntimeException("Service account is not configured, unable to proceed");
        }
        Account account = accountService.getActiveAccountByUuid(serviceAccountUuid);
        if (account == null) {
            throw new CloudRuntimeException("Service account with ID " + serviceAccountUuid + " not found, unable to proceed");
        }
        User user = accountService.getOneActiveUserForAccount(account);
        if (user == null) {
            throw new CloudRuntimeException("No active user found for service account with ID " + serviceAccountUuid);
        }
        return new Pair<>(user, account);
    }

    protected void waitForJobCompletion(long jobId) {
        long timeoutNanos = TimeUnit.MINUTES.toNanos(5);
        final long deadline = System.nanoTime() + timeoutNanos;
        long sleepMillis = 500;
        while (true) {
            AsyncJobVO job = asyncJobDao.findById(jobId);
            if (job == null) {
                logger.warn("Async job with ID {} not found", jobId);
                return;
            }
            if (job.getStatus() == AsyncJobVO.Status.SUCCEEDED || job.getStatus() == AsyncJobVO.Status.FAILED) {
                return;
            }
            if (System.nanoTime() > deadline) {
                logger.warn("Timed out waiting for {} completion", job);
            }
            try {
                Thread.sleep(sleepMillis);
                // back off gradually to reduce DB pressure
                sleepMillis = Math.min(5000, sleepMillis + 500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Interrupted while waiting for async job completion");
            }
        }
    }

    protected void waitForJobCompletion(AsyncJobJoinVO job) {
        if (job == null) {
            logger.warn("Async job not found");
            return;
        }
        if (job.getStatus() == AsyncJobVO.Status.SUCCEEDED.ordinal() ||
                job.getStatus() == AsyncJobVO.Status.FAILED.ordinal()) {
            logger.warn("Async job with ID {} already completed with status {}", job.getId(), job.getStatus());
        }
        waitForJobCompletion(job.getId());
    }

    @Override
    public boolean start() {
        getServiceAccount();
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
        final List<ClusterVO> clusters = clusterDao.listByHypervisorType(Hypervisor.HypervisorType.KVM);
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
        final List<HostJoinVO> hosts = hostJoinDao.listRoutingHostsByHypervisor(Hypervisor.HypervisorType.KVM);
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
        List<UserVmJoinVO> vms = userVmJoinDao.listAll();
        return UserVmJoinVOToVmConverter.toVmList(vms, this::getHostById);
    }

    public Vm getInstance(String uuid, boolean includeDisks, boolean includeNics, boolean allContent) {
        UserVmJoinVO vo = userVmJoinDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("VM with ID " + uuid + " not found");
        }
        return UserVmJoinVOToVmConverter.toVm(vo, this::getHostById,
                includeDisks ? this::listDiskAttachmentsByInstanceId : null,
                includeNics ? this::listNicsByInstance : null,
                allContent);
    }

    Ternary<Long, String, Long> getVmOwner(Vm request) {
        String accountUuid = request.getAccountId();
        if (StringUtils.isBlank(accountUuid)) {
            return new Ternary<>(null, null, null);
        }
        Account account = accountService.getActiveAccountByUuid(accountUuid);
        if (account == null) {
            logger.warn("Account with ID {} not found, unable to determine owner for VM creation request", accountUuid);
            return new Ternary<>(null, null, null);
        }
        Long projectId = null;
        if (Account.Type.PROJECT.equals(account.getType())) {
            Project project = projectService.findByProjectAccountId(account.getId());
            if (project == null) {
                logger.warn("Project for {} not found, unable to determine owner for VM creation request", account);
                return new Ternary<>(null, null, null);
            }
            projectId = project.getId();
        }
        return new Ternary<>(account.getDomainId(), account.getAccountName(), projectId);
    }

    public Vm createInstance(Vm request) {
        if (request == null) {
            throw new InvalidParameterValueException("Request disk data is empty");
        }
        OvfXmlUtil.updateFromConfiguration(request);
        String name = request.getName();
        if (StringUtils.isBlank(name)) {
            throw new InvalidParameterValueException("Invalid name specified for the VM");
        }
        String displayName = name;
        if (name.endsWith("_restored")) {
            name = name.replace("_restored", "-restored");
        }
        Long zoneId = null;
        Long clusterId = null;
        if (request.getCluster() != null && StringUtils.isNotEmpty(request.getCluster().getId())) {
            ClusterVO clusterVO = clusterDao.findByUuid(request.getCluster().getId());
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
            cpu = Integer.valueOf(request.getCpu().getTopology().getSockets());
        } catch (Exception ignored) {
        }
        if (cpu == null) {
            throw new InvalidParameterValueException("CPU topology sockets must be specified");
        }
        Long memory = null;
        try {
            memory = Long.valueOf(request.getMemory());
        } catch (Exception ignored) {
        }
        if (memory == null) {
            throw new InvalidParameterValueException("Memory must be specified");
        }
        String userdata = null;
        if (request.getInitialization() != null) {
            userdata = request.getInitialization().getCustomScript();
        }
        ApiConstants.BootType bootType = ApiConstants.BootType.BIOS;
        ApiConstants.BootMode bootMode = ApiConstants.BootMode.LEGACY;
        if (request.getBios() != null && StringUtils.isNotEmpty(request.getBios().getType()) && request.getBios().getType().contains("secure")) {
            bootType = ApiConstants.BootType.UEFI;
            bootMode = ApiConstants.BootMode.SECURE;
        }
        Ternary<Long, String, Long> owner = getVmOwner(request);
        String serviceOfferingUuid = null;
        if (request.getCpuProfile() != null && StringUtils.isNotEmpty(request.getCpuProfile().getId())) {
            serviceOfferingUuid = request.getCpuProfile().getId();
        }
        Pair<User, Account> serviceUserAccount = getServiceAccount();
        CallContext ctx = CallContext.register(serviceUserAccount.first(), serviceUserAccount.second());
        try {
            return createInstance(zoneId, clusterId, owner.first(), owner.second(), owner.third(), name, displayName,
                    serviceOfferingUuid, cpu, memory, userdata, bootType, bootMode);
        } finally {
            CallContext.unregister();
        }
    }

    protected ServiceOffering getServiceOfferingIdForVmCreation(String serviceOfferingUuid, long zoneId, int cpu, long memory) {
        if (StringUtils.isNotBlank(serviceOfferingUuid)) {
            ServiceOffering offering = serviceOfferingDao.findByUuid(serviceOfferingUuid);
            if (offering != null && !offering.isCustomized()) {
                // ToDo: check offering is available in the specified zone and matches the requested cpu/memory if it's not a custom offering
                return offering;
            }
        }
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

    protected Vm createInstance(Long zoneId, Long clusterId, Long domainId, String accountName, Long projectId,
                String name, String displayName, String serviceOfferingUuid, int cpu, long memory, String userdata,
                ApiConstants.BootType bootType, ApiConstants.BootMode bootMode) {
        ServiceOffering serviceOffering = getServiceOfferingIdForVmCreation(serviceOfferingUuid, zoneId, cpu, memory);
        if (serviceOffering == null) {
            throw new CloudRuntimeException("No service offering found for VM creation with specified CPU and memory");
        }
        DeployVMCmdByAdmin cmd = new DeployVMCmdByAdmin();
        cmd.setHttpMethod(BaseCmd.HTTPMethod.POST.name());
        ComponentContext.inject(cmd);
        cmd.setZoneId(zoneId);
        cmd.setClusterId(clusterId);
        if (domainId != null && StringUtils.isNotEmpty(accountName)) {
            cmd.setDomainId(domainId);
            cmd.setAccountName(accountName);
        }
        if (projectId != null) {
            cmd.setProjectId(projectId);
        }
        cmd.setName(name);
        if (displayName != null) {
            cmd.setDisplayName(displayName);
        }
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
        // ToDo: handle any other field?
        // Handle custom offerings
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
                    this::listNicsByInstance, false);
        } catch (InsufficientCapacityException | ResourceUnavailableException | ResourceAllocationException | CloudRuntimeException e) {
            throw new CloudRuntimeException("Failed to create VM: " + e.getMessage(), e);
        }
    }

    public Vm updateInstance(String uuid, Vm request) {
        logger.warn("Received request to update VM with ID {}. No action, returning existing VM data.", uuid);
        return getInstance(uuid, false, false, false);
    }

    public VmAction deleteInstance(String uuid) {
        UserVmVO vo = userVmDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("VM with ID " + uuid + " not found");
        }
        Pair<User, Account> serviceUserAccount = getServiceAccount();
        CallContext ctx = CallContext.register(serviceUserAccount.first(), serviceUserAccount.second());
        try {
            DestroyVMCmd cmd = new DestroyVMCmd();
            cmd.setHttpMethod(BaseCmd.HTTPMethod.POST.name());
            ComponentContext.inject(cmd);
            Map<String, String> params = new HashMap<>();
            params.put(ApiConstants.ID, vo.getUuid());
            params.put(ApiConstants.EXPUNGE, Boolean.TRUE.toString());
            ApiServerService.AsyncCmdResult result =
                    apiServerService.processAsyncCmd(cmd, params, ctx, serviceUserAccount.first().getId(),
                            serviceUserAccount.second());
            AsyncJobJoinVO asyncJobJoinVO = asyncJobJoinDao.findById(result.jobId);
            return AsyncJobJoinVOToJobConverter.toVmAction(asyncJobJoinVO, userVmJoinDao.findById(vo.getId()));
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to delete VM: " + e.getMessage(), e);
        } finally {
            CallContext.unregister();
        }
    }

    public VmAction startInstance(String uuid) {
        UserVmVO vo = userVmDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("VM with ID " + uuid + " not found");
        }
        Pair<User, Account> serviceUserAccount = getServiceAccount();
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
        Pair<User, Account> serviceUserAccount = getServiceAccount();
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
        Pair<User, Account> serviceUserAccount = getServiceAccount();
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

    protected Long getVolumePhysicalSize(VolumeJoinVO vo) {
        return volumeApiService.getVolumePhysicalSize(vo.getFormat(), vo.getPath(), vo.getChainInfo());
    }

    public List<Disk> listAllDisks() {
        List<VolumeJoinVO> kvmVolumes = volumeJoinDao.listByHypervisor(Hypervisor.HypervisorType.KVM);
        return VolumeJoinVOToDiskConverter.toDiskList(kvmVolumes, this::getVolumePhysicalSize);
    }

    public Disk getDisk(String uuid) {
        VolumeJoinVO vo = volumeJoinDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("Disk with ID " + uuid + " not found");
        }
        return VolumeJoinVOToDiskConverter.toDisk(vo, this::getVolumePhysicalSize);
    }

    public Disk copyDisk(String uuid) {
        throw new InvalidParameterValueException("Copy Disk with ID " + uuid + " not implemented");
//        VolumeVO vo = volumeDao.findByUuid(uuid);
//        if (vo == null) {
//            throw new InvalidParameterValueException("Disk with ID " + uuid + " not found");
//        }
//        Pair<User, Account> serviceUserAccount = createServiceAccountIfNeeded();
//        CallContext.register(serviceUserAccount.first(), serviceUserAccount.second());
//        try {
//            Volume volume = volumeApiService.copyVolume(vo.getId(), vo.getName() + "_copy", null, null);
//            VolumeJoinVO copiedVolumeVO = volumeJoinDao.findById(volume.getId());
//            return VolumeJoinVOToDiskConverter.toDisk(copiedVolumeVO);
//        } finally {
//            CallContext.unregister();
//        }
    }

    public Disk reduceDisk(String uuid) {
        throw new InvalidParameterValueException("Reduce Disk with ID " + uuid + " not implemented");
//        VolumeVO vo = volumeDao.findByUuid(uuid);
//        if (vo == null) {
//            throw new InvalidParameterValueException("Disk with ID " + uuid + " not found");
//        }
//        Pair<User, Account> serviceUserAccount = createServiceAccountIfNeeded();
//        CallContext.register(serviceUserAccount.first(), serviceUserAccount.second());
//        try {
//            Volume volume = volumeApiService.reduceDisk(vo.getId(), vo.getName() + "_copy", null, null);
//            VolumeJoinVO copiedVolumeVO = volumeJoinDao.findById(volume.getId());
//            return VolumeJoinVOToDiskConverter.toDisk(copiedVolumeVO);
//        } finally {
//            CallContext.unregister();
//        }
    }

    protected List<DiskAttachment> listDiskAttachmentsByInstanceId(final long instanceId) {
        List<VolumeJoinVO> kvmVolumes = volumeJoinDao.listByInstanceId(instanceId);
        return VolumeJoinVOToDiskConverter.toDiskAttachmentList(kvmVolumes, this::getVolumePhysicalSize);
    }

    public List<DiskAttachment> listDiskAttachmentsByInstanceUuid(final String uuid) {
        UserVmVO vo = userVmDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("VM with ID " + uuid + " not found");
        }
        return listDiskAttachmentsByInstanceId(vo.getId());
    }

    protected void assignVolumeToAccount(VolumeVO volumeVO, long accountId, Pair<User, Account> serviceUserAccount) {
        Account account = accountService.getActiveAccountById(accountId);
        if (account == null) {
            throw new InvalidParameterValueException("Account with ID " + accountId + " not found");
        }
        CallContext.register(serviceUserAccount.first(), serviceUserAccount.second());
        try {
            AssignVolumeCmd cmd = new AssignVolumeCmd();
            ComponentContext.inject(cmd);
            Map<String, String> params = new HashMap<>();
            cmd.setVolumeId(volumeVO.getId());
            params.put(ApiConstants.VOLUME_ID, volumeVO.getUuid());
            if (Account.Type.PROJECT.equals(account.getType())) {
                cmd.setProjectId(account.getId());
                params.put(ApiConstants.PROJECT_ID, account.getUuid());
            } else {
                cmd.setAccountId(account.getId());
                params.put(ApiConstants.ACCOUNT_ID, account.getUuid());
            }
            cmd.setFullUrlParams(params);
            volumeApiService.assignVolumeToAccount(cmd);
        } catch (ResourceAllocationException | CloudRuntimeException e) {
            logger.error("Failed to assign {} to {}: {}", volumeVO, account, e.getMessage(), e);
        } finally {
            CallContext.unregister();
        }
    }

    public DiskAttachment attachInstanceDisk(final String vmUuid, final DiskAttachment request) {
        UserVmVO vmVo = userVmDao.findByUuid(vmUuid);
        if (vmVo == null) {
            throw new InvalidParameterValueException("VM with ID " + vmUuid + " not found");
        }
        if (request == null || request.getDisk() == null || StringUtils.isEmpty(request.getDisk().getId())) {
            throw new InvalidParameterValueException("Request disk data is empty");
        }
        VolumeVO volumeVO = volumeDao.findByUuid(request.getDisk().getId());
        if (volumeVO == null) {
            throw new InvalidParameterValueException("Disk with ID " + request.getDisk().getId() + " not found");
        }
        Pair<User, Account> serviceUserAccount = getServiceAccount();
        if (vmVo.getAccountId() != volumeVO.getAccountId()) {
            if (VeeamControlService.InstanceRestoreAssignOwner.value()) {
                assignVolumeToAccount(volumeVO, vmVo.getAccountId(), serviceUserAccount);
            } else {
                throw new PermissionDeniedException("Disk with ID " + request.getDisk().getId() +
                        " belongs to a different account and cannot be attached to the VM");
            }
        }
        CallContext.register(serviceUserAccount.first(), serviceUserAccount.second());
        try {
            Long deviceId = null;
            List<VolumeVO> volumes = volumeDao.findUsableVolumesForInstance(vmVo.getId());
            if (CollectionUtils.isEmpty(volumes)) {
                deviceId = 0L;
            }
            Volume volume = volumeApiService.attachVolumeToVM(vmVo.getId(), volumeVO.getId(), deviceId, false);
            VolumeJoinVO attachedVolumeVO = volumeJoinDao.findById(volume.getId());
            return VolumeJoinVOToDiskConverter.toDiskAttachment(attachedVolumeVO, this::getVolumePhysicalSize);
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

    public Disk createDisk(Disk request) {
        if (request == null) {
            throw new InvalidParameterValueException("Request disk data is empty");
        }
        String name = request.getName();
        if (StringUtils.isBlank(name) && !name.startsWith("Veeam_KvmBackupDisk_")) {
            throw new InvalidParameterValueException("Only worker VM disk creation is supported");
        }
        if (request.getStorageDomains() == null || CollectionUtils.isEmpty(request.getStorageDomains().getItems()) ||
                request.getStorageDomains().getItems().size() > 1) {
            throw new InvalidParameterValueException("Exactly one storage domain must be specified");
        }
        StorageDomain domain = request.getStorageDomains().getItems().get(0);
        if (domain == null || domain.getId() == null) {
            throw new InvalidParameterValueException("Storage domain ID must be specified");
        }
        StoragePoolVO pool = primaryDataStoreDao.findByUuid(domain.getId());
        if (pool == null) {
            throw new InvalidParameterValueException("Storage domain with ID " + domain.getId() + " not found");
        }
        String sizeStr = request.getProvisionedSize();
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
        if (StringUtils.isNotBlank(request.getInitialSize())) {
            try {
                initialSize = Long.parseLong(request.getInitialSize());
            } catch (NumberFormatException ignored) {}
        }
        Pair<User, Account> serviceUserAccount = getServiceAccount();
        Account serviceAccount = serviceUserAccount.second();
        DataCenterVO zone = dataCenterDao.findById(pool.getDataCenterId());
        if (zone == null || !Grouping.AllocationState.Enabled.equals(zone.getAllocationState())) {
            throw new InvalidParameterValueException("Datacenter for the specified storage domain is not found or not active");
        }
        Long diskOfferingId = volumeApiService.getCustomDiskOfferingIdForVolumeUpload(serviceAccount, zone);
        if (diskOfferingId == null) {
            throw new CloudRuntimeException("Failed to find custom offering for disk" + zone.getName());
        }
        CallContext.register(serviceUserAccount.first(), serviceUserAccount.second());
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
        return VolumeJoinVOToDiskConverter.toDisk(volumeJoinDao.findById(volume.getId()), this::getVolumePhysicalSize);
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

    protected boolean accountCannotAccessNetwork(NetworkVO networkVO, long accountId) {
        Account account = accountService.getActiveAccountById(accountId);
        try {
            networkModel.checkNetworkPermissions(account, networkVO);
            return false;
        } catch (CloudRuntimeException e) {
            logger.debug("{} cannot access {}: {}", account, networkVO, e.getMessage());
        }
        return true;
    }

    protected void assignVmToAccount(UserVmVO vmVO, long accountId, Pair<User, Account> serviceUserAccount) {
        Account account = accountService.getActiveAccountById(accountId);
        if (account == null) {
            throw new InvalidParameterValueException("Account with ID " + accountId + " not found");
        }
        CallContext.register(serviceUserAccount.first(), serviceUserAccount.second());
        try {
            AssignVMCmd cmd = new AssignVMCmd();
            ComponentContext.inject(cmd);
            cmd.setVirtualMachineId(vmVO.getId());
            cmd.setAccountName(account.getAccountName());
            cmd.setDomainId(account.getDomainId());
            if (Account.Type.PROJECT.equals(account.getType())) {
                cmd.setProjectId(account.getId());
            }
            cmd.setSkipNetwork(true);
            userVmService.moveVmToUser(cmd);
        } catch (ResourceAllocationException | CloudRuntimeException | ResourceUnavailableException |
                 InsufficientCapacityException e) {
            logger.error("Failed to assign {} to {}: {}", vmVO, account, e.getMessage(), e);
        } finally {
            CallContext.unregister();
        }
    }

    public Nic attachInstanceNic(final String vmUuid, final Nic request) {
        UserVmVO vmVo = userVmDao.findByUuid(vmUuid);
        if (vmVo == null) {
            throw new InvalidParameterValueException("VM with ID " + vmUuid + " not found");
        }
        if (request == null || request.getVnicProfile() == null || StringUtils.isEmpty(request.getVnicProfile().getId())) {
            throw new InvalidParameterValueException("Request nic data is empty");
        }
        NetworkVO networkVO = networkDao.findByUuid(request.getVnicProfile().getId());
        if (networkVO == null) {
            throw new InvalidParameterValueException("VNic profile " + request.getVnicProfile().getId() + " not found");
        }
        Pair<User, Account> serviceUserAccount = getServiceAccount();
        if (vmVo.getAccountId() != networkVO.getAccountId() &&
                networkVO.getAccountId() != Account.ACCOUNT_ID_SYSTEM &&
                VeeamControlService.InstanceRestoreAssignOwner.value() &&
                accountCannotAccessNetwork(networkVO, vmVo.getAccountId())) {
            assignVmToAccount(vmVo, networkVO.getAccountId(), serviceUserAccount);
        }
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
        ImageTransferVO vo = imageTransferDao.findByUuidIncludingRemoved(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("Image transfer with ID " + uuid + " not found");
        }
        return ImageTransferVOToImageTransferConverter.toImageTransfer(vo, this::getHostById, this::getVolumeById);
    }

    public ImageTransfer createImageTransfer(ImageTransfer request) {
        if (request == null) {
            throw new InvalidParameterValueException("Request image transfer data is empty");
        }
        if (request.getDisk() == null || StringUtils.isBlank(request.getDisk().getId())) {
            throw new InvalidParameterValueException("Disk ID must be specified");
        }
        VolumeJoinVO volumeVO = volumeJoinDao.findByUuid(request.getDisk().getId());
        if (volumeVO == null) {
            throw new InvalidParameterValueException("Disk with ID " + request.getDisk().getId() + " not found");
        }
        Direction direction = EnumUtils.fromString(Direction.class, request.getDirection());
        if (direction == null) {
            throw new InvalidParameterValueException("Invalid or missing direction");
        }
        Format format = EnumUtils.fromString(Format.class, request.getFormat());
        Long backupId = null;
        if (request.getBackup() != null && StringUtils.isNotBlank(request.getBackup().getId())) {
            BackupVO backupVO = backupDao.findByUuid(request.getBackup().getId());
            if (backupVO == null) {
                throw new InvalidParameterValueException("Backup with ID " + request.getBackup().getId() + " not found");
            }
            backupId = backupVO.getId();
        }
        return createImageTransfer(backupId, volumeVO.getId(), direction, format);
    }

    public boolean cancelImageTransfer(String uuid) {
        ImageTransferVO vo = imageTransferDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("Image transfer with ID " + uuid + " not found");
        }
        return incrementalBackupService.cancelImageTransfer(vo.getId());
    }

    public boolean finalizeImageTransfer(String uuid) {
        ImageTransferVO vo = imageTransferDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("Image transfer with ID " + uuid + " not found");
        }
        return incrementalBackupService.finalizeImageTransfer(vo.getId());
    }

    private ImageTransfer createImageTransfer(Long backupId, Long volumeId, Direction direction, Format format) {
        Pair<User, Account> serviceUserAccount = getServiceAccount();
        CallContext.register(serviceUserAccount.first(), serviceUserAccount.second());
        try {
            org.apache.cloudstack.backup.ImageTransfer imageTransfer =
                    incrementalBackupService.createImageTransfer(volumeId, backupId, direction, format);
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
        Pair<User, Account> serviceUserAccount = getServiceAccount();
        List<Long> jobIds = asyncJobDao.listPendingJobIdsForAccount(serviceUserAccount.second().getId());
        List<AsyncJobJoinVO> jobJoinVOs = asyncJobJoinDao.listByIds(jobIds);
        return AsyncJobJoinVOToJobConverter.toJobList(jobJoinVOs);
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

    public Snapshot createInstanceSnapshot(final String vmUuid, final Snapshot request) {
        UserVmVO vmVo = userVmDao.findByUuid(vmUuid);
        if (vmVo == null) {
            throw new InvalidParameterValueException("VM with ID " + vmUuid + " not found");
        }
        Pair<User, Account> serviceUserAccount = getServiceAccount();
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
        Pair<User, Account> serviceUserAccount = getServiceAccount();
        CallContext ctx = CallContext.register(serviceUserAccount.first(), serviceUserAccount.second());
        try {
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
            if (!async) {
                waitForJobCompletion(jobVo);
            }
            action = AsyncJobJoinVOToJobConverter.toAction(jobVo);
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to delete snapshot: " + e.getMessage(), e);
        } finally {
            CallContext.unregister();
        }
        return action;
    }

    public ResourceAction revertInstanceToSnapshot(String uuid) {
        ResourceAction action = null;
        VMSnapshotVO vo = vmSnapshotDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("Snapshot with ID " + uuid + " not found");
        }
        Pair<User, Account> serviceUserAccount = getServiceAccount();
        CallContext ctx = CallContext.register(serviceUserAccount.first(), serviceUserAccount.second());
        try {
            RevertToVMSnapshotCmd cmd = new RevertToVMSnapshotCmd();
            ComponentContext.inject(cmd);
            Map<String, String> params = new HashMap<>();
            params.put(ApiConstants.VM_SNAPSHOT_ID, vo.getUuid());
            ApiServerService.AsyncCmdResult result =
                    apiServerService.processAsyncCmd(cmd, params, ctx, serviceUserAccount.first().getId(),
                            serviceUserAccount.second());
            AsyncJobJoinVO jobVo = asyncJobJoinDao.findById(result.jobId);
            if (jobVo == null) {
                throw new CloudRuntimeException("Failed to find job for snapshot revert");
            }
            action = AsyncJobJoinVOToJobConverter.toAction(jobVo);
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to revert to snapshot: " + e.getMessage(), e);
        } finally {
            CallContext.unregister();
        }
        return action;
    }

    public List<Backup> listBackupsByInstanceUuid(final String uuid) {
        UserVmVO vo = userVmDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("VM with ID " + uuid + " not found");
        }
        List<BackupVO> backups = backupDao.searchByVmIds(List.of(vo.getId()));
        return BackupVOToBackupConverter.toBackupList(backups, id -> vo, this::getHostById);
    }

    public Backup createInstanceBackup(final String vmUuid, final Backup request) {
        UserVmVO vmVo = userVmDao.findByUuid(vmUuid);
        if (vmVo == null) {
            throw new InvalidParameterValueException("VM with ID " + vmUuid + " not found");
        }
        Pair<User, Account> serviceUserAccount = getServiceAccount();
        CallContext ctx = CallContext.register(serviceUserAccount.first(), serviceUserAccount.second());
        try {
            StartBackupCmd cmd = new StartBackupCmd();
            ComponentContext.inject(cmd);
            Map<String, String> params = new HashMap<>();
            params.put(ApiConstants.VIRTUAL_MACHINE_ID, vmVo.getUuid());
            params.put(ApiConstants.NAME, request.getName());
            params.put(ApiConstants.DESCRIPTION, request.getDescription());
            ApiServerService.AsyncCmdResult result =
                    apiServerService.processAsyncCmd(cmd, params, ctx, vmVo.getUserId(), serviceUserAccount.second());
            if (result == null || result.objectId == null) {
                throw new CloudRuntimeException("Unexpected backup ID returned");
            }
            BackupVO vo = backupDao.findById(result.objectId);
            if (vo == null) {
                throw new CloudRuntimeException("Backup not found");
            }
            return BackupVOToBackupConverter.toBackup(vo, id -> vmVo, this::getHostById, this::getBackupDisks);
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to create backup: " + e.getMessage(), e);
        } finally {
            CallContext.unregister();
        }
    }

    public Backup getBackup(String uuid) {
        BackupVO vo = backupDao.findByUuidIncludingRemoved(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("Backup with ID " + uuid + " not found");
        }
        return BackupVOToBackupConverter.toBackup(vo, id -> userVmDao.findById(id), this::getHostById,
                this::getBackupDisks);
    }

    public List<Disk> listDisksByBackupUuid(final String uuid) {
        throw new InvalidParameterValueException("List Backup Disks with ID " + uuid + " not implemented");
        // This won't be feasible with current structure
    }

    public Backup finalizeBackup(final String vmUuid, final String backupUuid) {
        UserVmVO vm = userVmDao.findByUuid(vmUuid);
        if (vm == null) {
            throw new InvalidParameterValueException("Instance with ID " + vmUuid + " not found");
        }
        BackupVO backup = backupDao.findByUuid(backupUuid);
        if (backup == null) {
            throw new InvalidParameterValueException("Backup with ID " + backupUuid + " not found");
        }
        Pair<User, Account> serviceUserAccount = getServiceAccount();
        CallContext ctx = CallContext.register(serviceUserAccount.first(), serviceUserAccount.second());
        try {
            FinalizeBackupCmd cmd = new FinalizeBackupCmd();
            ComponentContext.inject(cmd);
            Map<String, String> params = new HashMap<>();
            params.put(ApiConstants.VIRTUAL_MACHINE_ID, vm.getUuid());
            params.put(ApiConstants.ID, backup.getUuid());
            ApiServerService.AsyncCmdResult result =
                    apiServerService.processAsyncCmd(cmd, params, ctx, vm.getUserId(), serviceUserAccount.second());
            if (result == null) {
                throw new CloudRuntimeException("Failed to finalize backup");
            }
            backup = backupDao.findByIdIncludingRemoved(backup.getId());
            return BackupVOToBackupConverter.toBackup(backup, id -> vm, this::getHostById, this::getBackupDisks);
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to finalize backup: " + e.getMessage(), e);
        } finally {
            CallContext.unregister();
        }
    }

    protected List<Disk> getBackupDisks(final BackupVO backup) {
        List<org.apache.cloudstack.backup.Backup.VolumeInfo> volumeInfos = backup.getBackedUpVolumes();
        if (CollectionUtils.isEmpty(volumeInfos)) {
            return Collections.emptyList();
        }
        return VolumeJoinVOToDiskConverter.toDiskListFromVolumeInfos(volumeInfos);
    }

    public List<Checkpoint> listCheckpointsByInstanceUuid(final String uuid) {
        UserVmVO vo = userVmDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("VM with ID " + uuid + " not found");
        }
        Checkpoint checkpoint = UserVmVOToCheckpointConverter.toCheckpoint(vo);
        if (checkpoint == null) {
            return Collections.emptyList();
        }
        return List.of(checkpoint);
    }

    public void deleteCheckpoint(String vmUuid, String checkpointId) {
        UserVmVO vo = userVmDao.findByUuid(vmUuid);
        if (vo == null) {
            throw new InvalidParameterValueException("VM with ID " + vmUuid + " not found");
        }
        if (!Objects.equals(vo.getActiveCheckpointId(), checkpointId)) {
            logger.warn("Checkpoint ID {} does not match active checkpoint for VM {}", checkpointId, vmUuid);
            return;
        }
        Pair<User, Account> serviceUserAccount = getServiceAccount();
        CallContext.register(serviceUserAccount.first(), serviceUserAccount.second());
        try {
            DeleteVmCheckpointCmd cmd = new DeleteVmCheckpointCmd();
            ComponentContext.inject(cmd);
            cmd.setVmId(vo.getId());
            incrementalBackupService.deleteVmCheckpoint(cmd);
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to delete checkpoint: " + e.getMessage(), e);
        } finally {
            CallContext.unregister();
        }
    }

    public List<Tag> listAllTags() {
        List<Tag> tags = new ArrayList<>(getDummyTags().values());
        List<ResourceTagVO> vmResourceTags = resourceTagDao.listByResourceType(ResourceTag.ResourceObjectType.UserVm);
        if (CollectionUtils.isNotEmpty(vmResourceTags)) {
            tags.addAll(ResourceTagVOToTagConverter.toTags(vmResourceTags));
        }
        return tags;
    }

    public Tag getTag(String uuid) {
        if (BaseDto.ZERO_UUID.equals(uuid)) {
            return ResourceTagVOToTagConverter.getRootTag();
        }
        Tag tag = getDummyTags().get(uuid);
        if (tag == null) {
            ResourceTagVO resourceTagVO = resourceTagDao.findByUuid(uuid);
            if (resourceTagVO != null) {
                tag = ResourceTagVOToTagConverter.toTag(resourceTagVO);
            }
        }
        if (tag == null) {
            throw new InvalidParameterValueException("Tag with ID " + uuid + " not found");
        }
        return tag;
    }
}
