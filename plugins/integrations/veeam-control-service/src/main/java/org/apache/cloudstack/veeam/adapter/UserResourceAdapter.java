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

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.cloudstack.acl.Role;
import org.apache.cloudstack.acl.RolePermissionEntity;
import org.apache.cloudstack.acl.RoleService;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.acl.Rule;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.command.user.job.QueryAsyncJobResultCmd;
import org.apache.cloudstack.api.command.user.network.ListNetworksCmd;
import org.apache.cloudstack.api.command.user.vm.DeployVMCmd;
import org.apache.cloudstack.api.command.user.vm.DestroyVMCmd;
import org.apache.cloudstack.api.command.user.vm.ListVMsCmd;
import org.apache.cloudstack.api.command.user.vm.StartVMCmd;
import org.apache.cloudstack.api.command.user.vm.StopVMCmd;
import org.apache.cloudstack.api.command.user.volume.AttachVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.CreateVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.DeleteVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.DetachVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.ListVolumesCmd;
import org.apache.cloudstack.api.command.user.volume.ResizeVolumeCmd;
import org.apache.cloudstack.backup.ImageTransfer.Direction;
import org.apache.cloudstack.backup.ImageTransferVO;
import org.apache.cloudstack.backup.IncrementalBackupService;
import org.apache.cloudstack.backup.dao.ImageTransferDao;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.veeam.api.converter.ImageTransferVOToImageTransferConverter;
import org.apache.cloudstack.veeam.api.converter.VolumeJoinVOToDiskConverter;
import org.apache.cloudstack.veeam.api.dto.Disk;
import org.apache.cloudstack.veeam.api.dto.ImageTransfer;
import org.apache.cloudstack.veeam.api.dto.Ref;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.cloud.api.query.dao.HostJoinDao;
import com.cloud.api.query.dao.VolumeJoinDao;
import com.cloud.api.query.vo.HostJoinVO;
import com.cloud.api.query.vo.VolumeJoinVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.org.Grouping;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeApiService;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserAccount;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.EnumUtils;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;

public class UserResourceAdapter extends ManagerBase {
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

    @Inject
    DataCenterDao dataCenterDao;

    @Inject
    RoleService roleService;

    @Inject
    AccountService accountService;

    @Inject
    AccountDao accountDao;

    @Inject
    VolumeJoinDao volumeJoinDao;

    @Inject
    VolumeApiService volumeApiService;

    @Inject
    PrimaryDataStoreDao primaryDataStoreDao;

    @Inject
    ImageTransferDao imageTransferDao;

    @Inject
    HostJoinDao hostJoinDao;

    @Inject
    IncrementalBackupService incrementalBackupService;

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

    protected Account createServiceAccount() {
        CallContext.register(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM);
        try {
            Role role = getServiceAccountRole();
            UserAccount userAccount = accountService.createUserAccount(SERVICE_ACCOUNT_NAME,
                    UUID.randomUUID().toString(), SERVICE_ACCOUNT_FIRST_NAME,
                    SERVICE_ACCOUNT_LAST_NAME, null, null, SERVICE_ACCOUNT_NAME, Account.Type.NORMAL, role.getId(),
                    1L, null, null, null, null, User.Source.NATIVE);
            Account account = accountService.getAccount(userAccount.getAccountId());
            logger.debug("Created Veeam service account: {}", account);
            return account;
        } finally {
            CallContext.unregister();
        }
    }

    protected Account createServiceAccountIfNeeded() {
        List<AccountVO> accounts = accountDao.findAccountsByName(SERVICE_ACCOUNT_NAME);
        for (AccountVO account : accounts) {
            if (Account.State.ENABLED.equals(account.getState())) {
                logger.debug("Veeam service account found: {}", account);
                return account;
            }
        }
        return createServiceAccount();
    }

    @Override
    public boolean start() {
        createServiceAccountIfNeeded();
        //find public custom disk offering
        return true;
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
        if (StringUtils.isBlank(request.provisionedSize)) {
            throw new InvalidParameterValueException("Provisioned size must be specified");
        }
        long sizeInGb;
        try {
            sizeInGb = Long.parseLong(request.provisionedSize);
        } catch (NumberFormatException ex) {
            throw new InvalidParameterValueException("Invalid provisioned size: " + request.provisionedSize);
        }
        if (sizeInGb <= 0) {
            throw new InvalidParameterValueException("Provisioned size must be greater than zero");
        }
        sizeInGb = Math.max(1L, sizeInGb / (1024L * 1024L * 1024L));
        Account serviceAccount = createServiceAccountIfNeeded();
        DataCenterVO zone = dataCenterDao.findById(pool.getDataCenterId());
        if (zone == null || !Grouping.AllocationState.Enabled.equals(zone.getAllocationState())) {
            throw new InvalidParameterValueException("Datacenter for the specified storage domain is not found or not active");
        }
        Long diskOfferingId = volumeApiService.getCustomDiskOfferingIdForVolumeUpload(serviceAccount, zone);
        if (diskOfferingId == null) {
            throw new CloudRuntimeException("Failed to find custom offering for disk" + zone.getName());
        }
        CallContext.register(serviceAccount.getId(), serviceAccount.getId());
        try {
            return createDisk(serviceAccount, pool, name, diskOfferingId, sizeInGb);
        } finally {
            CallContext.unregister();
        }
    }

    @NotNull
    private Disk createDisk(Account serviceAccount, StoragePoolVO pool, String name, Long diskOfferingId, long sizeInGb) {
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

        // Implementation for creating a Disk resource
        return VolumeJoinVOToDiskConverter.toDisk(volumeJoinDao.findById(volume.getId()));
    }

    public List<ImageTransfer> listAllImageTransfers() {
        List<ImageTransferVO> imageTransfers = imageTransferDao.listAll();
        return ImageTransferVOToImageTransferConverter.toImageTransferList(imageTransfers, this::getHostById, this::getVolumeById);
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
        return createImageTransfer(null, volumeVO.getId(), direction);
    }

    private ImageTransfer createImageTransfer(Long backupId, Long volumeId, Direction direction) {
        Account serviceAccount = createServiceAccountIfNeeded();
        CallContext.register(serviceAccount.getId(), serviceAccount.getId());
        try {
            org.apache.cloudstack.backup.ImageTransfer imageTransfer =
                    incrementalBackupService.createImageTransfer(volumeId, null, direction);
            ImageTransferVO imageTransferVO = imageTransferDao.findById(imageTransfer.getId());
            return ImageTransferVOToImageTransferConverter.toImageTransfer(imageTransferVO, this::getHostById, this::getVolumeById);
        } finally {
            CallContext.unregister();
        }
    }
}
