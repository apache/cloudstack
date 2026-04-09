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
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.cloudstack.acl.Role;
import org.apache.cloudstack.acl.RolePermissionEntity;
import org.apache.cloudstack.acl.RoleService;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.acl.Rule;
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.affinity.AffinityGroupVO;
import org.apache.cloudstack.affinity.dao.AffinityGroupDao;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiServerService;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.command.admin.backup.CreateImageTransferCmd;
import org.apache.cloudstack.api.command.admin.backup.DeleteVmCheckpointCmd;
import org.apache.cloudstack.api.command.admin.backup.FinalizeBackupCmd;
import org.apache.cloudstack.api.command.admin.backup.FinalizeImageTransferCmd;
import org.apache.cloudstack.api.command.admin.backup.ListImageTransfersCmd;
import org.apache.cloudstack.api.command.admin.backup.ListVmCheckpointsCmd;
import org.apache.cloudstack.api.command.admin.backup.StartBackupCmd;
import org.apache.cloudstack.api.command.admin.cluster.ListClustersCmd;
import org.apache.cloudstack.api.command.admin.host.ListHostsCmd;
import org.apache.cloudstack.api.command.admin.storage.ListStoragePoolsCmd;
import org.apache.cloudstack.api.command.admin.vm.AssignVMCmd;
import org.apache.cloudstack.api.command.admin.vm.DeployVMCmdByAdmin;
import org.apache.cloudstack.api.command.user.backup.ListBackupsCmd;
import org.apache.cloudstack.api.command.user.job.ListAsyncJobsCmd;
import org.apache.cloudstack.api.command.user.job.QueryAsyncJobResultCmd;
import org.apache.cloudstack.api.command.user.network.ListNetworksCmd;
import org.apache.cloudstack.api.command.user.offering.ListServiceOfferingsCmd;
import org.apache.cloudstack.api.command.user.tag.ListTagsCmd;
import org.apache.cloudstack.api.command.user.vm.AddNicToVMCmd;
import org.apache.cloudstack.api.command.user.vm.DeployVMCmd;
import org.apache.cloudstack.api.command.user.vm.DestroyVMCmd;
import org.apache.cloudstack.api.command.user.vm.ListNicsCmd;
import org.apache.cloudstack.api.command.user.vm.ListVMsCmd;
import org.apache.cloudstack.api.command.user.vm.StartVMCmd;
import org.apache.cloudstack.api.command.user.vm.StopVMCmd;
import org.apache.cloudstack.api.command.user.vm.UpdateVMCmd;
import org.apache.cloudstack.api.command.user.vmsnapshot.CreateVMSnapshotCmd;
import org.apache.cloudstack.api.command.user.vmsnapshot.DeleteVMSnapshotCmd;
import org.apache.cloudstack.api.command.user.vmsnapshot.ListVMSnapshotCmd;
import org.apache.cloudstack.api.command.user.vmsnapshot.RevertToVMSnapshotCmd;
import org.apache.cloudstack.api.command.user.volume.AssignVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.AttachVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.CreateVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.DeleteVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.DestroyVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.DetachVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.ListVolumesCmd;
import org.apache.cloudstack.api.command.user.volume.ResizeVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.UpdateVolumeCmd;
import org.apache.cloudstack.api.command.user.zone.ListZonesCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ServiceOfferingResponse;
import org.apache.cloudstack.backup.BackupVO;
import org.apache.cloudstack.backup.ImageTransfer.Direction;
import org.apache.cloudstack.backup.ImageTransfer.Format;
import org.apache.cloudstack.backup.ImageTransferVO;
import org.apache.cloudstack.backup.KVMBackupExportService;
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
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.cloud.api.query.dao.AsyncJobJoinDao;
import com.cloud.api.query.dao.DataCenterJoinDao;
import com.cloud.api.query.dao.HostJoinDao;
import com.cloud.api.query.dao.StoragePoolJoinDao;
import com.cloud.api.query.dao.UserVmJoinDao;
import com.cloud.api.query.dao.VolumeJoinDao;
import com.cloud.api.query.vo.AsyncJobJoinVO;
import com.cloud.api.query.vo.DataCenterJoinVO;
import com.cloud.api.query.vo.HostJoinVO;
import com.cloud.api.query.vo.StoragePoolJoinVO;
import com.cloud.api.query.vo.UserVmJoinVO;
import com.cloud.api.query.vo.VolumeJoinVO;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.Domain;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.offering.ServiceOffering;
import com.cloud.org.Grouping;
import com.cloud.projects.Project;
import com.cloud.projects.ProjectManager;
import com.cloud.server.ResourceTag;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.Storage;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeApiService;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.tags.ResourceTagVO;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.DomainService;
import com.cloud.user.User;
import com.cloud.user.UserAccount;
import com.cloud.user.UserDataVO;
import com.cloud.user.dao.UserDataDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.EnumUtils;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.Filter;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.NicVO;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDetailsDao;
import com.cloud.vm.snapshot.VMSnapshotVO;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;

// ToDo: check access for list APIs when not ROOT admin

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
    private static final List<Storage.StoragePoolType> SUPPORTED_STORAGE_TYPES = Arrays.asList(
            Storage.StoragePoolType.Filesystem,
            Storage.StoragePoolType.NetworkFilesystem,
            Storage.StoragePoolType.SharedMountPoint
    );
    private static final String VM_TA_KEY = "veeam_tag";
    private static final String WORKER_VM_GUEST_CPU_MODE = "host-passthrough";

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
    VMInstanceDetailsDao vmInstanceDetailsDao;

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
    KVMBackupExportService kvmBackupExportService;

    @Inject
    QueryService queryService;

    @Inject
    ServiceOfferingDao serviceOfferingDao;

    @Inject
    VMTemplateDao templateDao;

    @Inject
    UserVmManager userVmManager;

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
    ProjectManager projectManager;

    @Inject
    AffinityGroupDao affinityGroupDao;

    @Inject
    UserDataDao userDataDao;

    @Inject
    DomainService domainService;

    @Inject
    DomainDao domainDao;

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
        Tag rootTag = ResourceTagVOToTagConverter.getRootTag();
        tags.put(rootTag.getId(), rootTag);
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

    protected Role getServiceAccountRole() {
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

    protected ApiServerService.AsyncCmdResult processAsyncCmdWithContext(BaseAsyncCmd cmd, Map<String, String> params)
            throws Exception {
        final CallContext ctx = CallContext.current();
        final long callerUserId = ctx.getCallingUserId();
        final Account caller = ctx.getCallingAccount();
        return apiServerService.processAsyncCmd(cmd, params, ctx, callerUserId, caller);
    }

    protected Account getOwnerForInstanceCreation(Vm request) {
        if (!VeeamControlService.InstanceRestoreAssignOwner.value()) {
            return null;
        }
        String accountUuid = request.getAccountId();
        if (StringUtils.isBlank(accountUuid)) {
            return null;
        }
        Account account = accountService.getActiveAccountByUuid(accountUuid);
        if (account == null) {
            logger.warn("Account with ID {} not found, unable to determine owner for VM creation request", accountUuid);
            return null;
        }
        return account;
    }

    protected Ternary<Long, String, Long> getOwnerDetailsForInstanceCreation(Account account) {
        if (account == null) {
            return new Ternary<>(null, null, null);
        }
        String accountName = account.getAccountName();
        Long projectId = null;
        if (Account.Type.PROJECT.equals(account.getType())) {
            Project project = projectManager.findByProjectAccountId(account.getId());
            if (project == null) {
                logger.warn("Project for {} not found, unable to determine owner for VM creation request", account);
                return new Ternary<>(null, null, null);
            }
            projectId = project.getId();
            accountName = null;
        }
        return new Ternary<>(account.getDomainId(), accountName, projectId);
    }

    protected Pair<List<Long>, String> getResourceOwnerFilters() {
        final Account caller = CallContext.current().getCallingAccount();
        final Account.Type type = caller.getType();
        if (Account.Type.ADMIN.equals(type)) {
            return new Pair<>(null, null);
        }
        List<Long> permittedAccountIds = null;
        String domainPath = null;
        if (Account.Type.DOMAIN_ADMIN.equals(type) || Account.Type.NORMAL.equals(type)) {
            permittedAccountIds = projectManager.listPermittedProjectAccounts(caller.getId());
            permittedAccountIds.add(caller.getId());
        }
        if (Account.Type.DOMAIN_ADMIN.equals(type)) {
            Domain domain = domainService.getDomain(caller.getDomainId());
            if (domain == null) {
                throw new InvalidParameterValueException("Invalid service account specified");
            }
            domainPath = domain.getPath();
        }
        if (Account.Type.PROJECT.equals(type)) {
            Project project = projectManager.findByProjectAccountId(caller.getId());
            if (project == null) {
                throw new InvalidParameterValueException("Invalid service account specified");
            }
            permittedAccountIds = new ArrayList<>();
            permittedAccountIds.add(caller.getId());
        }
        return new Pair<>(permittedAccountIds, domainPath);
    }

    protected Pair<List<Long>, List<Long>> getResourceOwnerFiltersWithDomainIds() {
        Pair<List<Long>, String> filters = getResourceOwnerFilters();
        if (StringUtils.isNotBlank(filters.second())) {
            return new Pair<>(filters.first(), domainDao.getDomainChildrenIds(filters.second()));
        }
        return new Pair<>(filters.first(), null);
    }

    protected ServiceOfferingVO getServiceOfferingFromRequest(com.cloud.dc.DataCenter zone, Account account,
                      String uuid, int cpu, int memory) {
        if (StringUtils.isBlank(uuid)) {
            return null;
        }
        ServiceOfferingVO offering = serviceOfferingDao.findByUuid(uuid);
        if (offering == null) {
            logger.warn("Service offering with ID {} linked with the VM request not found", uuid);
            return null;
        }
        try {
            accountService.checkAccess(account, offering, zone);
        } catch (PermissionDeniedException e) {
            logger.warn("Service offering with ID {} linked with the VM request is not accessible for the account {}. Offering: {}, zone: {}",
                    uuid, account, offering, zone);
            return null;
        }
        if (!offering.isCustomized() && (offering.getCpu() != cpu || offering.getRamSize() != memory)) {
            logger.warn("Service offering with ID {} linked with the VM request has different CPU or memory than requested. Offering: {}, requested CPU: {}, requested memory: {}",
                    uuid, offering, cpu, memory);
            return null;
        }
        if (offering.isCustomized()) {
            Map<String, String> params = Map.of(
                    VmDetailConstants.CPU_NUMBER, String.valueOf(cpu),
                    VmDetailConstants.MEMORY, String.valueOf(memory)
            );
            try {
                userVmManager.validateCustomParameters(offering, params);
                offering.setCpu(cpu);
                offering.setRamSize(memory);
            } catch (InvalidParameterValueException e) {
                logger.warn("Service offering with ID {} linked with the VM request is customized but does not support requested CPU or memory. Offering: {}, requested CPU: {}, requested memory: {}",
                        uuid, offering, cpu, memory);
                return null;
            }
        }
        return offering;
    }

    protected ServiceOffering getServiceOfferingIdForVmCreation(com.cloud.dc.DataCenter zone, Account account,
                    String serviceOfferingUuid, int cpu, int memory) {
        ServiceOfferingVO offering = getServiceOfferingFromRequest(zone, account, serviceOfferingUuid, cpu, memory);
        if (offering != null) {
            return offering;
        }
        ListServiceOfferingsCmd cmd = new ListServiceOfferingsCmd();
        ComponentContext.inject(cmd);
        cmd.setZoneId(zone.getId());
        cmd.setCpuNumber(cpu);
        cmd.setMemory(memory);
        ListResponse<ServiceOfferingResponse> offerings = queryService.searchForServiceOfferings(cmd);
        if (offerings.getResponses().isEmpty()) {
            return null;
        }
        String uuid = offerings.getResponses().get(0).getId();
        offering = serviceOfferingDao.findByUuid(uuid);
        if (offering.isCustomized()) {
            offering.setCpu(cpu);
            offering.setRamSize(memory);
        }
        return offering;
    }

    protected VMTemplateVO getTemplateForInstanceCreation(String templateUuid) {
        if (StringUtils.isBlank(templateUuid)) {
            return null;
        }
        VMTemplateVO template = templateDao.findByUuid(templateUuid);
        if (template == null) {
            logger.warn("Template with ID {} not found, VM will be created with default template", templateUuid);
            return null;
        }
        return template;
    }

    protected Vm createInstance(com.cloud.dc.DataCenter zone, Long clusterId, Account owner, Long domainId,
                String accountName, Long projectId, String name, String displayName, String serviceOfferingUuid,
                int cpu, int memory, String templateUuid, String userdata, ApiConstants.BootType bootType,
                ApiConstants.BootMode bootMode, String affinityGroupId, String userDataId, Map<String, String> details) {
        Account account = owner != null ? owner : CallContext.current().getCallingAccount();
        ServiceOffering serviceOffering = getServiceOfferingIdForVmCreation(zone, account, serviceOfferingUuid, cpu,
                memory);
        if (serviceOffering == null) {
            throw new CloudRuntimeException("No service offering found for VM creation with specified CPU and memory");
        }
        DeployVMCmdByAdmin cmd = new DeployVMCmdByAdmin();
        cmd.setHttpMethod(BaseCmd.HTTPMethod.POST.name());
        ComponentContext.inject(cmd);
        cmd.setZoneId(zone.getId());
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
        VMTemplateVO template = getTemplateForInstanceCreation(templateUuid);
        if (template != null) {
            cmd.setTemplateId(template.getId());
        }
        if (StringUtils.isNotBlank(affinityGroupId)) {
            AffinityGroupVO group = affinityGroupDao.findByUuid(affinityGroupId);
            if (group == null) {
                logger.warn("Failed to find affinity group with ID {} specified in Instance creation request, " +
                        "skipping affinity group assignment", affinityGroupId);
            } else {
                cmd.setAffinityGroupIds(List.of(group.getId()));
            }
        }
        if (StringUtils.isNotBlank(userDataId)) {
            UserDataVO userData = userDataDao.findByUuid(userDataId);
            if (userData == null) {
                logger.warn("Failed to find userdata with ID {} specified in Instance creation request, " +
                        "skipping userdata assignment", userDataId);
            } else {
                cmd.setUserDataId(userData.getId());
            }
        }
        cmd.setHypervisor(Hypervisor.HypervisorType.KVM.name());
        Map<String, String> instanceDetails = getDetailsForInstanceCreation(userdata, serviceOffering, details);
        if (MapUtils.isNotEmpty(instanceDetails)) {
            Map<Integer, Map<String, String>> map = new HashMap<>();
            map.put(0, instanceDetails);
            cmd.setDetails(map);
        }
        cmd.setBlankInstance(true);
        try {
            UserVm vm = userVmManager.createVirtualMachine(cmd);
            vm = userVmManager.finalizeCreateVirtualMachine(vm.getId());
            UserVmJoinVO vo = userVmJoinDao.findById(vm.getId());
            return UserVmJoinVOToVmConverter.toVm(vo, this::getHostById, this::getDetailsByInstanceId,
                    this::listTagsByInstanceId, this::listDiskAttachmentsByInstanceId, this::listNicsByInstance, false);
        } catch (InsufficientCapacityException | ResourceUnavailableException | ResourceAllocationException | CloudRuntimeException e) {
            throw new CloudRuntimeException("Failed to create VM: " + e.getMessage(), e);
        }
    }

    @NotNull
    protected static Map<String, String> getDetailsForInstanceCreation(String userdata, ServiceOffering serviceOffering,
                           Map<String, String> existingDetails) {
        Map<String, String> details = new HashMap<>();
        List<String> detailsTobeSkipped = List.of(
                ApiConstants.BootType.BIOS.toString(),
                ApiConstants.BootType.UEFI.toString());
        if (MapUtils.isNotEmpty(existingDetails)) {
            for (Map.Entry<String, String> entry : existingDetails.entrySet()) {
                if (detailsTobeSkipped.contains(entry.getKey())) {
                    continue;
                }
                details.put(entry.getKey(), entry.getValue());
            }
        }
        if (StringUtils.isNotEmpty(userdata)) {
            // Assumption: Only worker VM will have userdata and it needs CPU mode
            details.put(VmDetailConstants.GUEST_CPU_MODE, WORKER_VM_GUEST_CPU_MODE);
        }
        if (serviceOffering.isCustomized()) {
            details.put(VmDetailConstants.CPU_NUMBER, String.valueOf(serviceOffering.getCpu()));
            details.put(VmDetailConstants.MEMORY, String.valueOf(serviceOffering.getRamSize()));
            if (serviceOffering.getSpeed() == null && !details.containsKey(VmDetailConstants.CPU_SPEED)) {
                details.put(VmDetailConstants.CPU_SPEED, String.valueOf(1000));
            }
        }
        return details;
    }

    protected static long getProvisionedSizeInGb(String sizeStr) {
        long provisionedSizeInGb;
        try {
            provisionedSizeInGb = Long.parseLong(sizeStr);
        } catch (NumberFormatException ex) {
            throw new InvalidParameterValueException("Invalid provisioned size: " + sizeStr);
        }
        if (provisionedSizeInGb <= 0) {
            throw new InvalidParameterValueException("Provisioned size must be greater than zero");
        }
        // round-up provisionedSizeInGb to the next whole GB
        long GB = 1024L * 1024L * 1024L;
        provisionedSizeInGb = Math.max(1L, (provisionedSizeInGb + GB - 1) / GB);
        return provisionedSizeInGb;
    }

    protected Long getVolumePhysicalSize(VolumeJoinVO vo) {
        return volumeApiService.getVolumePhysicalSize(vo.getFormat(), vo.getPath(), vo.getChainInfo());
    }

    @NotNull
    protected Disk createDisk(Account serviceAccount, StoragePoolVO pool, String name, Long diskOfferingId, long sizeInGb, Long initialSize) {
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

    protected void assignVmToAccount(UserVmVO vmVO, long accountId) {
        Account account = accountService.getActiveAccountById(accountId);
        if (account == null) {
            throw new InvalidParameterValueException("Account with ID " + accountId + " not found");
        }
        try {
            AssignVMCmd cmd = new AssignVMCmd();
            ComponentContext.inject(cmd);
            cmd.setVirtualMachineId(vmVO.getId());
            cmd.setDomainId(account.getDomainId());
            if (Account.Type.PROJECT.equals(account.getType())) {
                Project project = projectManager.findByProjectAccountId(account.getId());
                if (project == null) {
                    throw new InvalidParameterValueException("Project for " + account + " not found");
                }
                cmd.setProjectId(project.getId());
            } else {
                cmd.setAccountName(account.getAccountName());
            }
            cmd.setSkipNetwork(true);
            userVmManager.moveVmToUser(cmd);
        } catch (ResourceAllocationException | CloudRuntimeException | ResourceUnavailableException |
                 InsufficientCapacityException e) {
            logger.error("Failed to assign {} to {}: {}", vmVO, account, e.getMessage(), e);
        }
    }

    protected ImageTransfer createImageTransfer(Long backupId, Long volumeId, Direction direction, Format format) {
        org.apache.cloudstack.backup.ImageTransfer imageTransfer =
                kvmBackupExportService.createImageTransfer(volumeId, backupId, direction, format);
        ImageTransferVO imageTransferVO = imageTransferDao.findById(imageTransfer.getId());
        return ImageTransferVOToImageTransferConverter.toImageTransfer(imageTransferVO, this::getHostById,
                this::getVolumeById);
    }

    protected DataCenterJoinVO getZoneById(Long zoneId) {
        if (zoneId == null) {
            return null;
        }
        return dataCenterJoinDao.findById(zoneId);
    }

    protected HostJoinVO getHostById(Long hostId) {
        if (hostId == null) {
            return null;
        }
        return hostJoinDao.findById(hostId);
    }

    protected VolumeJoinVO getVolumeById(Long volumeId) {
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

    protected Map<String, String> getDetailsByInstanceId(Long instanceId) {
        return vmInstanceDetailsDao.listDetailsKeyPairs(instanceId, true);
    }

    public Pair<User, Account> getServiceAccount() {
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

    @Override
    public boolean start() {
        getServiceAccount();
        return true;
    }

    @ApiAccess(command = ListZonesCmd.class)
    public List<DataCenter> listAllDataCenters(Long offset, Long limit) {
        Filter filter = new Filter(DataCenterJoinVO.class, "id", true, offset, limit);
        final List<DataCenterJoinVO> clusters = dataCenterJoinDao.listAll(filter);
        return DataCenterJoinVOToDataCenterConverter.toDCList(clusters);
    }

    @ApiAccess(command = ListZonesCmd.class)
    public DataCenter getDataCenter(String uuid) {
        final DataCenterJoinVO vo = dataCenterJoinDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("DataCenter with ID " + uuid + " not found");
        }
        return  DataCenterJoinVOToDataCenterConverter.toDataCenter(vo);
    }

    @ApiAccess(command = ListStoragePoolsCmd.class)
    public List<StorageDomain> listStorageDomainsByDcId(final String uuid, final Long offset, final Long limit) {
        final DataCenterVO dataCenterVO = dataCenterDao.findByUuid(uuid);
        if (dataCenterVO == null) {
            throw new InvalidParameterValueException("DataCenter with ID " + uuid + " not found");
        }
        Filter filter = new Filter(StoragePoolJoinVO.class, "id", true, offset, limit);
        List<StoragePoolJoinVO> storagePoolVOS = storagePoolJoinDao.listByZoneAndType(dataCenterVO.getId(),
                SUPPORTED_STORAGE_TYPES, filter);
        return StoreVOToStorageDomainConverter.toStorageDomainListFromPools(storagePoolVOS);
    }

    @ApiAccess(command = ListNetworksCmd.class)
    public List<Network> listNetworksByDcId(final String uuid, final Long offset, final Long limit) {
        final DataCenterJoinVO dataCenterVO = dataCenterJoinDao.findByUuid(uuid);
        if (dataCenterVO == null) {
            throw new InvalidParameterValueException("DataCenter with ID " + uuid + " not found");
        }
        Filter filter = new Filter(NetworkVO.class, "id", true, offset, limit);
        List<NetworkVO> networks = networkDao.listByZoneAndTrafficType(dataCenterVO.getId(), Networks.TrafficType.Guest, filter);
        return NetworkVOToNetworkConverter.toNetworkList(networks, (dcId) -> dataCenterVO);
    }

    @ApiAccess(command = ListClustersCmd.class)
    public List<Cluster> listAllClusters(Long offset, Long limit) {
        Filter filter = new Filter(ClusterVO.class, "id", true, offset, limit);
        final List<ClusterVO> clusters = clusterDao.listByHypervisorType(Hypervisor.HypervisorType.KVM, filter);
        return ClusterVOToClusterConverter.toClusterList(clusters, this::getZoneById);
    }

    @ApiAccess(command = ListClustersCmd.class)
    public Cluster getCluster(String uuid) {
        final ClusterVO vo = clusterDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("Cluster with ID " + uuid + " not found");
        }
        return ClusterVOToClusterConverter.toCluster(vo, this::getZoneById);
    }

    @ApiAccess(command = ListHostsCmd.class)
    public List<Host> listAllHosts(Long offset, Long limit) {
        Filter filter = new Filter(HostJoinVO.class, "id", true, offset, limit);
        final List<HostJoinVO> hosts = hostJoinDao.listRoutingHostsByHypervisor(Hypervisor.HypervisorType.KVM, filter);
        return HostJoinVOToHostConverter.toHostList(hosts);
    }

    @ApiAccess(command = ListHostsCmd.class)
    public Host getHost(String uuid) {
        final HostJoinVO vo = hostJoinDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("Host with ID " + uuid + " not found");
        }
        return HostJoinVOToHostConverter.toHost(vo);
    }

    @ApiAccess(command = ListNetworksCmd.class)
    public List<Network> listAllNetworks(Long offset, Long limit) {
        Filter filter = new Filter(NetworkVO.class, "id", true, offset, limit);
        Pair<List<Long>, List<Long>> ownerDetails = getResourceOwnerFiltersWithDomainIds();
        final List<NetworkVO> networks = networkDao.listByTrafficTypeAndOwners(Networks.TrafficType.Guest,
                ownerDetails.first(), ownerDetails.second(), filter);
        return NetworkVOToNetworkConverter.toNetworkList(networks, this::getZoneById);
    }

    @ApiAccess(command = ListNetworksCmd.class)
    public Network getNetwork(String uuid) {
        final NetworkVO vo = networkDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("Network with ID " + uuid + " not found");
        }
        accountService.checkAccess(CallContext.current().getCallingAccount(), null, false, vo);
        return NetworkVOToNetworkConverter.toNetwork(vo, this::getZoneById);
    }

    @ApiAccess(command = ListNetworksCmd.class)
    public List<VnicProfile> listAllVnicProfiles(Long offset, Long limit) {
        Filter filter = new Filter(NetworkVO.class, "id", true, offset, limit);
        Pair<List<Long>, List<Long>> ownerDetails = getResourceOwnerFiltersWithDomainIds();
        final List<NetworkVO> networks = networkDao.listByTrafficTypeAndOwners(Networks.TrafficType.Guest,
                ownerDetails.first(), ownerDetails.second(), filter);
        return NetworkVOToVnicProfileConverter.toVnicProfileList(networks, this::getZoneById);
    }

    @ApiAccess(command = ListNetworksCmd.class)
    public VnicProfile getVnicProfile(String uuid) {
        final NetworkVO vo = networkDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("Nic profile with ID " + uuid + " not found");
        }
        return NetworkVOToVnicProfileConverter.toVnicProfile(vo, this::getZoneById);
    }

    @ApiAccess(command = ListVMsCmd.class)
    public List<Vm> listAllInstances(boolean includeTags, boolean includeDisks, boolean includeNics,
                 boolean allContent, Long offset, Long limit) {
        Filter filter = new Filter(UserVmJoinVO.class, "id", true, offset, limit);
        Pair<List<Long>, String> ownerDetails = getResourceOwnerFilters();
        List<UserVmJoinVO> vms = userVmJoinDao.listByHypervisorTypeAndOwners(Hypervisor.HypervisorType.KVM,
                ownerDetails.first(), ownerDetails.second(), filter);
        return UserVmJoinVOToVmConverter.toVmList(vms,
                this::getHostById,
                this::getDetailsByInstanceId,
                includeTags ? this::listTagsByInstanceId : null,
                includeDisks ? this::listDiskAttachmentsByInstanceId : null,
                includeNics ? this::listNicsByInstance : null,
                allContent);
    }

    @ApiAccess(command = ListVMsCmd.class)
    public Vm getInstance(String uuid, boolean includeTags, boolean includeDisks, boolean includeNics,
              boolean allContent) {
        UserVmJoinVO vo = userVmJoinDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("VM with ID " + uuid + " not found");
        }
        return UserVmJoinVOToVmConverter.toVm(vo,
                this::getHostById,
                this::getDetailsByInstanceId,
                includeTags ? this::listTagsByInstanceId : null,
                includeDisks ? this::listDiskAttachmentsByInstanceId : null,
                includeNics ? this::listNicsByInstance : null,
                allContent);
    }

    @ApiAccess(command = DeployVMCmd.class)
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
        name = name.replace("_", "-");
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
        DataCenterVO zone = dataCenterDao.findById(zoneId);
        if (zone == null) {
            throw new InvalidParameterValueException("DataCenter could not be determined for the request");
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
        int memoryMB = (int)(memory / (1024L * 1024L));
        String userdata = null;
        if (request.getInitialization() != null) {
            userdata = request.getInitialization().getCustomScript();
        }
        Pair<ApiConstants.BootType, ApiConstants.BootMode> bootOptions = Vm.Bios.retrieveBootOptions(request.getBios());
        Account owner = getOwnerForInstanceCreation(request);
        Ternary<Long, String, Long> ownerDetails = getOwnerDetailsForInstanceCreation(owner);
        String serviceOfferingUuid = null;
        if (request.getCpuProfile() != null && StringUtils.isNotEmpty(request.getCpuProfile().getId())) {
            serviceOfferingUuid = request.getCpuProfile().getId();
        }
        String templateUuid = null;
        if (request.getTemplate() != null && StringUtils.isNotEmpty(request.getTemplate().getId())) {
            templateUuid = request.getTemplate().getId();
        }
        return createInstance(zone, clusterId, owner, ownerDetails.first(), ownerDetails.second(),
                ownerDetails.third(), name, displayName, serviceOfferingUuid, cpu, memoryMB, templateUuid,
                userdata, bootOptions.first(), bootOptions.second(), request.getAffinityGroupId(),
                request.getUserDataId(), request.getDetails());
    }

    @ApiAccess(command = UpdateVMCmd.class)
    public Vm updateInstance(String uuid, Vm request) {
        logger.warn("Received request to update VM with ID {}. No action, returning existing VM data.", uuid);
        return getInstance(uuid, false, false, false, false);
    }

    @ApiAccess(command = DestroyVMCmd.class)
    public VmAction deleteInstance(String uuid, boolean async) {
        UserVmVO vo = userVmDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("VM with ID " + uuid + " not found");
        }
        try {
            DestroyVMCmd cmd = new DestroyVMCmd();
            cmd.setHttpMethod(BaseCmd.HTTPMethod.POST.name());
            ComponentContext.inject(cmd);
            Map<String, String> params = new HashMap<>();
            params.put(ApiConstants.ID, vo.getUuid());
            params.put(ApiConstants.EXPUNGE, Boolean.TRUE.toString());
            ApiServerService.AsyncCmdResult result = processAsyncCmdWithContext(cmd, params);
            AsyncJobJoinVO jobVo = asyncJobJoinDao.findById(result.jobId);
            if (jobVo == null) {
                throw new CloudRuntimeException("Failed to find job for VM deletion");
            }
            if (!async) {
                waitForJobCompletion(jobVo);
            }
            return AsyncJobJoinVOToJobConverter.toVmAction(jobVo, userVmJoinDao.findById(vo.getId()));
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to delete VM: " + e.getMessage(), e);
        }
    }

    @ApiAccess(command = StartVMCmd.class)
    public VmAction startInstance(String uuid, boolean async) {
        UserVmVO vo = userVmDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("VM with ID " + uuid + " not found");
        }
        try {
            StartVMCmd cmd = new StartVMCmd();
            cmd.setHttpMethod(BaseCmd.HTTPMethod.POST.name());
            ComponentContext.inject(cmd);
            Map<String, String> params = new HashMap<>();
            params.put(ApiConstants.ID, vo.getUuid());
            ApiServerService.AsyncCmdResult result = processAsyncCmdWithContext(cmd, params);
            AsyncJobJoinVO jobVo = asyncJobJoinDao.findById(result.jobId);
            if (jobVo == null) {
                throw new CloudRuntimeException("Failed to find job for VM start");
            }
            if (!async) {
                waitForJobCompletion(jobVo);
            }
            return AsyncJobJoinVOToJobConverter.toVmAction(jobVo, userVmJoinDao.findById(vo.getId()));
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to start VM: " + e.getMessage(), e);
        }
    }

    @ApiAccess(command = StopVMCmd.class)
    public VmAction stopInstance(String uuid, boolean async) {
        UserVmVO vo = userVmDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("VM with ID " + uuid + " not found");
        }
        try {
            StopVMCmd cmd = new StopVMCmd();
            cmd.setHttpMethod(BaseCmd.HTTPMethod.POST.name());
            ComponentContext.inject(cmd);
            Map<String, String> params = new HashMap<>();
            params.put(ApiConstants.ID, vo.getUuid());
            params.put(ApiConstants.FORCED, Boolean.TRUE.toString());
            ApiServerService.AsyncCmdResult result = processAsyncCmdWithContext(cmd, params);
            AsyncJobJoinVO jobVo = asyncJobJoinDao.findById(result.jobId);
            if (jobVo == null) {
                throw new CloudRuntimeException("Failed to find job for VM stop");
            }
            if (!async) {
                waitForJobCompletion(jobVo);
            }
            return AsyncJobJoinVOToJobConverter.toVmAction(jobVo, userVmJoinDao.findById(vo.getId()));
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to stop VM: " + e.getMessage(), e);
        }
    }

    @ApiAccess(command = StopVMCmd.class)
    public VmAction shutdownInstance(String uuid, boolean async) {
        UserVmVO vo = userVmDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("VM with ID " + uuid + " not found");
        }
        try {
            StopVMCmd cmd = new StopVMCmd();
            cmd.setHttpMethod(BaseCmd.HTTPMethod.POST.name());
            ComponentContext.inject(cmd);
            Map<String, String> params = new HashMap<>();
            params.put(ApiConstants.ID, vo.getUuid());
            params.put(ApiConstants.FORCED, Boolean.FALSE.toString());
            ApiServerService.AsyncCmdResult result = processAsyncCmdWithContext(cmd, params);
            AsyncJobJoinVO jobVo = asyncJobJoinDao.findById(result.jobId);
            if (jobVo == null) {
                throw new CloudRuntimeException("Failed to find job for VM shutdown");
            }
            if (!async) {
                waitForJobCompletion(jobVo);
            }
            return AsyncJobJoinVOToJobConverter.toVmAction(jobVo, userVmJoinDao.findById(vo.getId()));
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to shutdown VM: " + e.getMessage(), e);
        }
    }

    @ApiAccess(command = ListTagsCmd.class)
    protected List<Tag> listTagsByInstanceId(final long instanceId) {
        ResourceTag vmResourceTag = resourceTagDao.findByKey(instanceId,
                ResourceTag.ResourceObjectType.UserVm, VM_TA_KEY);
        List<ResourceTagVO> tags = new ArrayList<>();
        if (vmResourceTag instanceof ResourceTagVO) {
            tags.add((ResourceTagVO)vmResourceTag);
        } else {
            tags.add(resourceTagDao.findById(vmResourceTag.getId()));
        }
        return ResourceTagVOToTagConverter.toTags(tags);
    }

    @ApiAccess(command = ListVolumesCmd.class)
    protected List<DiskAttachment> listDiskAttachmentsByInstanceId(final long instanceId) {
        List<VolumeJoinVO> kvmVolumes = volumeJoinDao.listByInstanceId(instanceId);
        return VolumeJoinVOToDiskConverter.toDiskAttachmentList(kvmVolumes, this::getVolumePhysicalSize);
    }

    @ApiAccess(command = ListVolumesCmd.class)
    public List<DiskAttachment> listDiskAttachmentsByInstanceUuid(final String uuid) {
        UserVmVO vo = userVmDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("VM with ID " + uuid + " not found");
        }
        accountService.checkAccess(CallContext.current().getCallingAccount(), null, false, vo);
        return listDiskAttachmentsByInstanceId(vo.getId());
    }

    @ApiAccess(command = ListVolumesCmd.class)
    public List<Disk> listAllDisks(Long offset, Long limit) {
        Filter filter = new Filter(VolumeJoinVO.class, "id", true, offset, limit);
        Pair<List<Long>, String> ownerDetails = getResourceOwnerFilters();
        List<VolumeJoinVO> kvmVolumes = volumeJoinDao.listByHypervisorTypeAndOwners(Hypervisor.HypervisorType.KVM,
                ownerDetails.first(), ownerDetails.second(), filter);
        return VolumeJoinVOToDiskConverter.toDiskList(kvmVolumes, this::getVolumePhysicalSize);
    }

    @ApiAccess(command = ListVolumesCmd.class)
    public Disk getDisk(String uuid) {
        VolumeVO vo = volumeDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("Disk with ID " + uuid + " not found");
        }
        accountService.checkAccess(CallContext.current().getCallingAccount(), null, false, vo);
        return VolumeJoinVOToDiskConverter.toDisk(volumeJoinDao.findByUuid(uuid), this::getVolumePhysicalSize);
    }

    protected void assignVolumeToAccount(VolumeVO volumeVO, long accountId) {
        Account account = accountService.getActiveAccountById(accountId);
        if (account == null) {
            throw new InvalidParameterValueException("Account with ID " + accountId + " not found");
        }
        try {
            AssignVolumeCmd cmd = new AssignVolumeCmd();
            ComponentContext.inject(cmd);
            Map<String, String> params = new HashMap<>();
            cmd.setVolumeId(volumeVO.getId());
            params.put(ApiConstants.VOLUME_ID, volumeVO.getUuid());
            if (Account.Type.PROJECT.equals(account.getType())) {
                Project project = projectManager.findByProjectAccountId(account.getId());
                if (project == null) {
                    throw new InvalidParameterValueException("Project for " + account + " not found");
                }
                cmd.setProjectId(project.getId());
                params.put(ApiConstants.PROJECT_ID, project.getUuid());
            } else {
                cmd.setAccountId(account.getId());
                params.put(ApiConstants.ACCOUNT_ID, account.getUuid());
            }
            cmd.setFullUrlParams(params);
            volumeApiService.assignVolumeToAccount(cmd);
        } catch (ResourceAllocationException | CloudRuntimeException e) {
            logger.error("Failed to assign {} to {}: {}", volumeVO, account, e.getMessage(), e);
        }
    }

    @ApiAccess(command = AttachVolumeCmd.class)
    public DiskAttachment attachInstanceDisk(final String vmUuid, final DiskAttachment request) {
        UserVmVO vmVo = userVmDao.findByUuid(vmUuid);
        if (vmVo == null) {
            throw new InvalidParameterValueException("VM with ID " + vmUuid + " not found");
        }
        accountService.checkAccess(CallContext.current().getCallingAccount(), SecurityChecker.AccessType.OperateEntry,
                false, vmVo);
        if (request == null || request.getDisk() == null || StringUtils.isEmpty(request.getDisk().getId())) {
            throw new InvalidParameterValueException("Request disk data is empty");
        }
        VolumeVO volumeVO = volumeDao.findByUuid(request.getDisk().getId());
        if (volumeVO == null) {
            throw new InvalidParameterValueException("Disk with ID " + request.getDisk().getId() + " not found");
        }
        accountService.checkAccess(CallContext.current().getCallingAccount(), SecurityChecker.AccessType.OperateEntry,
                false, vmVo);
        if (vmVo.getAccountId() != volumeVO.getAccountId()) {
            if (VeeamControlService.InstanceRestoreAssignOwner.value()) {
                assignVolumeToAccount(volumeVO, vmVo.getAccountId());
            } else {
                throw new PermissionDeniedException("Disk with ID " + request.getDisk().getId() +
                        " belongs to a different account and cannot be attached to the VM");
            }
        }
        Long deviceId = null;
        List<VolumeVO> volumes = volumeDao.findUsableVolumesForInstance(vmVo.getId());
        if (CollectionUtils.isEmpty(volumes)) {
            deviceId = 0L;
        }
        Volume volume = volumeApiService.attachVolumeToVM(vmVo.getId(), volumeVO.getId(), deviceId, false);
        VolumeJoinVO attachedVolumeVO = volumeJoinDao.findById(volume.getId());
        return VolumeJoinVOToDiskConverter.toDiskAttachment(attachedVolumeVO, this::getVolumePhysicalSize);
    }

    @ApiAccess(command = CreateVolumeCmd.class)
    public Disk createDisk(Disk request) {
        if (request == null) {
            throw new InvalidParameterValueException("Request disk data is empty");
        }
        String name = request.getName();
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
        long provisionedSizeInGb = getProvisionedSizeInGb(sizeStr);
        Long initialSize = null;
        if (StringUtils.isNotBlank(request.getInitialSize())) {
            try {
                initialSize = Long.parseLong(request.getInitialSize());
            } catch (NumberFormatException ignored) {}
        }
        Account caller = CallContext.current().getCallingAccount();
        DataCenterVO zone = dataCenterDao.findById(pool.getDataCenterId());
        if (zone == null || !Grouping.AllocationState.Enabled.equals(zone.getAllocationState())) {
            throw new InvalidParameterValueException("Datacenter for the specified storage domain is not found or not active");
        }
        Long diskOfferingId = volumeApiService.getCustomDiskOfferingIdForVolumeUpload(caller, zone);
        if (diskOfferingId == null) {
            throw new CloudRuntimeException("Failed to find custom offering for disk" + zone.getName());
        }
        return createDisk(caller, pool, name, diskOfferingId, provisionedSizeInGb, initialSize);
    }

    @ApiAccess(command = DestroyVolumeCmd.class)
    public void deleteDisk(String uuid) {
        VolumeVO vo = volumeDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("Disk with ID " + uuid + " not found");
        }
        volumeApiService.deleteVolume(vo.getId(), accountService.getSystemAccount());
    }

    @ApiAccess(command = UpdateVolumeCmd.class)
    public Disk updateDisk(String uuid, Disk request) {
        VolumeVO vo = volumeDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("Disk with ID " + uuid + " not found");
        }
        logger.warn("Update disk is not implemented, returning disk ID: {} as it is", uuid);
        return getDisk(uuid);
    }

    @ApiAccess(command = UpdateVolumeCmd.class)
    public Disk copyDisk(String uuid) {
        throw new InvalidParameterValueException("Copy Disk with ID " + uuid + " not implemented");
    }

    @ApiAccess(command = UpdateVolumeCmd.class)
    public Disk reduceDisk(String uuid) {
        throw new InvalidParameterValueException("Reduce Disk with ID " + uuid + " not implemented");
    }

    @ApiAccess(command = ListNicsCmd.class)
    public List<Nic> listNicsByInstanceUuid(final String uuid) {
        UserVmVO vo = userVmDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("VM with ID " + uuid + " not found");
        }
        accountService.checkAccess(CallContext.current().getCallingAccount(), null, false, vo);
        return listNicsByInstance(vo.getId(), vo.getUuid());
    }

    @ApiAccess(command = AddNicToVMCmd.class)
    public Nic attachInstanceNic(final String vmUuid, final Nic request) {
        UserVmVO vmVo = userVmDao.findByUuid(vmUuid);
        if (vmVo == null) {
            throw new InvalidParameterValueException("VM with ID " + vmUuid + " not found");
        }
        accountService.checkAccess(CallContext.current().getCallingAccount(), SecurityChecker.AccessType.OperateEntry,
                false, vmVo);
        if (request == null || request.getVnicProfile() == null || StringUtils.isEmpty(request.getVnicProfile().getId())) {
            throw new InvalidParameterValueException("Request nic data is empty");
        }
        NetworkVO networkVO = networkDao.findByUuid(request.getVnicProfile().getId());
        if (networkVO == null) {
            throw new InvalidParameterValueException("VNic profile " + request.getVnicProfile().getId() + " not found");
        }
        accountService.checkAccess(CallContext.current().getCallingAccount(), SecurityChecker.AccessType.OperateEntry,
                false, networkVO);
        if (vmVo.getAccountId() != networkVO.getAccountId() &&
                networkVO.getAccountId() != Account.ACCOUNT_ID_SYSTEM &&
                VeeamControlService.InstanceRestoreAssignOwner.value() &&
                accountCannotAccessNetwork(networkVO, vmVo.getAccountId())) {
            assignVmToAccount(vmVo, networkVO.getAccountId());
        }
        AddNicToVMCmd cmd = new AddNicToVMCmd();
        ComponentContext.inject(cmd);
        cmd.setVmId(vmVo.getId());
        cmd.setNetworkId(networkVO.getId());
        if (request.getMac() != null && StringUtils.isNotBlank(request.getMac().getAddress())) {
            cmd.setMacAddress(request.getMac().getAddress());
        }
        userVmManager.addNicToVirtualMachine(cmd);
        NicVO nic = nicDao.findByInstanceIdAndNetworkIdIncludingRemoved(networkVO.getId(), vmVo.getId());
        if (nic == null) {
            throw new CloudRuntimeException("Failed to attach NIC to VM");
        }
        return NicVOToNicConverter.toNic(nic, vmUuid, this::getNetworkById);
    }

    @ApiAccess(command = ListImageTransfersCmd.class)
    public List<ImageTransfer> listAllImageTransfers(Long offset, Long limit) {
        Filter filter = new Filter(ImageTransferVO.class, "id", true, offset, limit);
        Pair<List<Long>, List<Long>> ownerDetails = getResourceOwnerFiltersWithDomainIds();
        List<ImageTransferVO> imageTransfers = imageTransferDao.listByOwners(ownerDetails.first(),
                ownerDetails.second(), filter);
        return ImageTransferVOToImageTransferConverter.toImageTransferList(imageTransfers, this::getHostById, this::getVolumeById);
    }

    @ApiAccess(command = ListImageTransfersCmd.class)
    public ImageTransfer getImageTransfer(String uuid) {
        ImageTransferVO vo = imageTransferDao.findByUuidIncludingRemoved(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("Image transfer with ID " + uuid + " not found");
        }
        accountService.checkAccess(CallContext.current().getCallingAccount(), null, false, vo);
        return ImageTransferVOToImageTransferConverter.toImageTransfer(vo, this::getHostById, this::getVolumeById);
    }

    @ApiAccess(command = CreateImageTransferCmd.class)
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
        accountService.checkAccess(CallContext.current().getCallingAccount(), null, false,
                volumeVO);
        Direction direction = EnumUtils.getEnum(Direction.class, request.getDirection());
        if (direction == null) {
            throw new InvalidParameterValueException("Invalid or missing direction");
        }
        Format format = EnumUtils.getEnum(Format.class, request.getFormat());
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

    @ApiAccess(command = FinalizeImageTransferCmd.class)
    public boolean cancelImageTransfer(String uuid) {
        ImageTransferVO vo = imageTransferDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("Image transfer with ID " + uuid + " not found");
        }
        accountService.checkAccess(CallContext.current().getCallingAccount(), SecurityChecker.AccessType.OperateEntry, false, vo);
        return kvmBackupExportService.cancelImageTransfer(vo.getId());
    }

    @ApiAccess(command = FinalizeImageTransferCmd.class)
    public boolean finalizeImageTransfer(String uuid) {
        ImageTransferVO vo = imageTransferDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("Image transfer with ID " + uuid + " not found");
        }
        accountService.checkAccess(CallContext.current().getCallingAccount(), SecurityChecker.AccessType.OperateEntry, false, vo);
        return kvmBackupExportService.finalizeImageTransfer(vo.getId());
    }

    @ApiAccess(command = ListAsyncJobsCmd.class)
    public List<Job> listPendingJobs() {
        List<Long> jobIds = asyncJobDao.listPendingJobIdsForAccount(CallContext.current().getCallingAccountId());
        List<AsyncJobJoinVO> jobJoinVOs = asyncJobJoinDao.listByIds(jobIds);
        return AsyncJobJoinVOToJobConverter.toJobList(jobJoinVOs);
    }

    @ApiAccess(command = ListAsyncJobsCmd.class)
    public Job getJob(String uuid) {
        final AsyncJobJoinVO vo = asyncJobJoinDao.findByUuidIncludingRemoved(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("Job with ID " + uuid + " not found");
        }
        accountService.checkAccess(CallContext.current().getCallingAccount(), null, false, vo);
        return AsyncJobJoinVOToJobConverter.toJob(vo);
    }

    @ApiAccess(command = ListVMSnapshotCmd.class)
    public List<Snapshot> listSnapshotsByInstanceUuid(final String uuid) {
        UserVmVO vo = userVmDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("VM with ID " + uuid + " not found");
        }
        List<VMSnapshotVO> snapshots = vmSnapshotDao.findByVm(vo.getId());
        return VmSnapshotVOToSnapshotConverter.toSnapshotList(snapshots, vo.getUuid());
    }

    @ApiAccess(command = CreateVMSnapshotCmd.class)
    public Snapshot createInstanceSnapshot(final String vmUuid, final Snapshot request) {
        UserVmVO vmVo = userVmDao.findByUuid(vmUuid);
        if (vmVo == null) {
            throw new InvalidParameterValueException("VM with ID " + vmUuid + " not found");
        }
        accountService.checkAccess(CallContext.current().getCallingAccount(), SecurityChecker.AccessType.OperateEntry,
                false, vmVo);
        try {
            CreateVMSnapshotCmd cmd = new CreateVMSnapshotCmd();
            ComponentContext.inject(cmd);
            Map<String, String> params = new HashMap<>();
            params.put(ApiConstants.VIRTUAL_MACHINE_ID, vmVo.getUuid());
            params.put(ApiConstants.VM_SNAPSHOT_DESCRIPTION, request.getDescription());
            params.put(ApiConstants.VM_SNAPSHOT_MEMORY, String.valueOf(Boolean.parseBoolean(request.getPersistMemorystate())));
            ApiServerService.AsyncCmdResult result = processAsyncCmdWithContext(cmd, params);
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
        }
    }

    @ApiAccess(command = ListVMSnapshotCmd.class)
    public Snapshot getSnapshot(String uuid) {
        VMSnapshotVO vo = vmSnapshotDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("Snapshot with ID " + uuid + " not found");
        }
        accountService.checkAccess(CallContext.current().getCallingAccount(), null, false, vo);
        UserVmVO vm = userVmDao.findById(vo.getVmId());
        return VmSnapshotVOToSnapshotConverter.toSnapshot(vo, vm.getUuid());
    }

    public ResourceAction deleteSnapshot(String uuid, boolean async) {
        ResourceAction action = null;
        VMSnapshotVO vo = vmSnapshotDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("Snapshot with ID " + uuid + " not found");
        }
        accountService.checkAccess(CallContext.current().getCallingAccount(), SecurityChecker.AccessType.OperateEntry,
                false, vo);
        try {
            DeleteVMSnapshotCmd cmd = new DeleteVMSnapshotCmd();
            ComponentContext.inject(cmd);
            Map<String, String> params = new HashMap<>();
            params.put(ApiConstants.VM_SNAPSHOT_ID, vo.getUuid());
            ApiServerService.AsyncCmdResult result = processAsyncCmdWithContext(cmd, params);
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
        }
        return action;
    }

    @ApiAccess(command = RevertToVMSnapshotCmd.class)
    public ResourceAction revertInstanceToSnapshot(String uuid, boolean async) {
        ResourceAction action = null;
        VMSnapshotVO vo = vmSnapshotDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("Snapshot with ID " + uuid + " not found");
        }
        accountService.checkAccess(CallContext.current().getCallingAccount(), SecurityChecker.AccessType.OperateEntry,
                false, vo);
        try {
            RevertToVMSnapshotCmd cmd = new RevertToVMSnapshotCmd();
            ComponentContext.inject(cmd);
            Map<String, String> params = new HashMap<>();
            params.put(ApiConstants.VM_SNAPSHOT_ID, vo.getUuid());
            ApiServerService.AsyncCmdResult result = processAsyncCmdWithContext(cmd, params);
            AsyncJobJoinVO jobVo = asyncJobJoinDao.findById(result.jobId);
            if (jobVo == null) {
                throw new CloudRuntimeException("Failed to find job for snapshot revert");
            }
            if (!async) {
                waitForJobCompletion(jobVo);
            }
            action = AsyncJobJoinVOToJobConverter.toAction(jobVo);
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to revert to snapshot: " + e.getMessage(), e);
        }
        return action;
    }

    @ApiAccess(command = ListBackupsCmd.class)
    public List<Backup> listBackupsByInstanceUuid(final String uuid) {
        UserVmVO vo = userVmDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("VM with ID " + uuid + " not found");
        }
        List<BackupVO> backups = backupDao.searchByVmIds(List.of(vo.getId()));
        return BackupVOToBackupConverter.toBackupList(backups, id -> vo, this::getHostById);
    }

    protected void validateInstanceStorage(UserVmVO vm) {
        List<VolumeVO> volumes = volumeDao.findUsableVolumesForInstance(vm.getId());
        List<Long> storageIds = volumes.stream().map(VolumeVO::getPoolId).distinct().collect(Collectors.toList());
        List<StoragePoolVO> pools = primaryDataStoreDao.listByIds(storageIds);
        pools.stream().filter(p -> !SUPPORTED_STORAGE_TYPES.contains(p.getPoolType()))
                .findAny().ifPresent(p -> {
                    throw new InvalidParameterValueException("VM is using storage pool " + p.getName() +
                            " of type " + p.getPoolType() +
                            " which is not supported for backup operations");
                });
    }

    @ApiAccess(command = StartBackupCmd.class)
    public Backup createInstanceBackup(final String vmUuid, final Backup request) {
        UserVmVO vmVo = userVmDao.findByUuid(vmUuid);
        if (vmVo == null) {
            throw new InvalidParameterValueException("VM with ID " + vmUuid + " not found");
        }
        accountService.checkAccess(CallContext.current().getCallingAccount(), SecurityChecker.AccessType.OperateEntry,
                false, vmVo);
        validateInstanceStorage(vmVo);
        try {
            StartBackupCmd cmd = new StartBackupCmd();
            ComponentContext.inject(cmd);
            Map<String, String> params = new HashMap<>();
            params.put(ApiConstants.VIRTUAL_MACHINE_ID, vmVo.getUuid());
            params.put(ApiConstants.NAME, request.getName());
            params.put(ApiConstants.DESCRIPTION, request.getDescription());
            ApiServerService.AsyncCmdResult result = processAsyncCmdWithContext(cmd, params);
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
        }
    }

    @ApiAccess(command = ListBackupsCmd.class)
    public Backup getBackup(String uuid) {
        BackupVO vo = backupDao.findByUuidIncludingRemoved(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("Backup with ID " + uuid + " not found");
        }
        accountService.checkAccess(CallContext.current().getCallingAccount(), null, false, vo);
        return BackupVOToBackupConverter.toBackup(vo, id -> userVmDao.findById(id), this::getHostById,
                this::getBackupDisks);
    }

    @ApiAccess(command = ListBackupsCmd.class)
    public List<Disk> listDisksByBackupUuid(final String uuid) {
        throw new InvalidParameterValueException("List Backup Disks with ID " + uuid + " not implemented");
        // This won't be feasible with current structure
    }

    @ApiAccess(command = FinalizeBackupCmd.class)
    public Backup finalizeBackup(final String vmUuid, final String backupUuid) {
        UserVmVO vm = userVmDao.findByUuid(vmUuid);
        if (vm == null) {
            throw new InvalidParameterValueException("Instance with ID " + vmUuid + " not found");
        }
        BackupVO backup = backupDao.findByUuid(backupUuid);
        if (backup == null) {
            throw new InvalidParameterValueException("Backup with ID " + backupUuid + " not found");
        }
        accountService.checkAccess(CallContext.current().getCallingAccount(), SecurityChecker.AccessType.OperateEntry,
                false, backup);
        try {
            FinalizeBackupCmd cmd = new FinalizeBackupCmd();
            ComponentContext.inject(cmd);
            Map<String, String> params = new HashMap<>();
            params.put(ApiConstants.VIRTUAL_MACHINE_ID, vm.getUuid());
            params.put(ApiConstants.ID, backup.getUuid());
            ApiServerService.AsyncCmdResult result = processAsyncCmdWithContext(cmd, params);
            if (result == null) {
                throw new CloudRuntimeException("Failed to finalize backup");
            }
            backup = backupDao.findByIdIncludingRemoved(backup.getId());
            return BackupVOToBackupConverter.toBackup(backup, id -> vm, this::getHostById, this::getBackupDisks);
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to finalize backup: " + e.getMessage(), e);
        }
    }

    @ApiAccess(command = ListBackupsCmd.class)
    protected List<Disk> getBackupDisks(final BackupVO backup) {
        List<org.apache.cloudstack.backup.Backup.VolumeInfo> volumeInfos = backup.getBackedUpVolumes();
        if (CollectionUtils.isEmpty(volumeInfos)) {
            return Collections.emptyList();
        }
        return VolumeJoinVOToDiskConverter.toDiskListFromVolumeInfos(volumeInfos);
    }

    @ApiAccess(command = ListVmCheckpointsCmd.class)
    public List<Checkpoint> listCheckpointsByInstanceUuid(final String uuid) {
        UserVmVO vo = userVmDao.findByUuid(uuid);
        if (vo == null) {
            throw new InvalidParameterValueException("VM with ID " + uuid + " not found");
        }
        accountService.checkAccess(CallContext.current().getCallingAccount(), null, false, vo);
        Map<String, String> details = vmInstanceDetailsDao.listDetailsKeyPairs(vo.getId());
        Checkpoint checkpoint = UserVmVOToCheckpointConverter.toCheckpoint(
                details.get(VmDetailConstants.ACTIVE_CHECKPOINT_ID),
                details.get(VmDetailConstants.ACTIVE_CHECKPOINT_CREATE_TIME));
        if (checkpoint == null) {
            return Collections.emptyList();
        }
        return List.of(checkpoint);
    }

    @ApiAccess(command = DeleteVmCheckpointCmd.class)
    public void deleteCheckpoint(String vmUuid, String checkpointId) {
        UserVmVO vo = userVmDao.findByUuid(vmUuid);
        if (vo == null) {
            throw new InvalidParameterValueException("VM with ID " + vmUuid + " not found");
        }
        accountService.checkAccess(CallContext.current().getCallingAccount(), SecurityChecker.AccessType.OperateEntry, false, vo);
        Map<String, String> details = vmInstanceDetailsDao.listDetailsKeyPairs(vo.getId());
        if (!Objects.equals(details.get(VmDetailConstants.ACTIVE_CHECKPOINT_ID), checkpointId)) {
            logger.warn("Checkpoint ID {} does not match active checkpoint for VM {}", checkpointId, vmUuid);
            return;
        }
        try {
            DeleteVmCheckpointCmd cmd = new DeleteVmCheckpointCmd();
            ComponentContext.inject(cmd);
            cmd.setVmId(vo.getId());
            cmd.setCheckpointId(checkpointId);
            kvmBackupExportService.deleteVmCheckpoint(cmd);
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to delete checkpoint: " + e.getMessage(), e);
        }
    }

    @ApiAccess(command = ListTagsCmd.class)
    public List<Tag> listAllTags(final Long offset, final Long limit) {
        List<Tag> tags = new ArrayList<>(getDummyTags().values());
        Filter filter = new Filter(ResourceTagVO.class, "id", true, offset, limit);
        Pair<List<Long>, List<Long>> ownerDetails = getResourceOwnerFiltersWithDomainIds();
        List<ResourceTagVO> vmResourceTags = resourceTagDao.listByResourceTypeKeyAndOwners(
                ResourceTag.ResourceObjectType.UserVm, VM_TA_KEY, ownerDetails.first(), ownerDetails.second(), filter);
        if (CollectionUtils.isNotEmpty(vmResourceTags)) {
            tags.addAll(ResourceTagVOToTagConverter.toTags(vmResourceTags));
        }
        return tags;
    }

    @ApiAccess(command = ListTagsCmd.class)
    public Tag getTag(String uuid) {
        if (BaseDto.ZERO_UUID.equals(uuid)) {
            return ResourceTagVOToTagConverter.getRootTag();
        }
        Tag tag = getDummyTags().get(uuid);
        if (tag == null) {
            ResourceTagVO resourceTagVO = resourceTagDao.findByResourceTypeKeyAndValue(
                    ResourceTag.ResourceObjectType.UserVm, VM_TA_KEY, uuid);
            accountService.checkAccess(CallContext.current().getCallingAccount(), null, false,
                    resourceTagVO);
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
