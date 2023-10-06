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

package org.apache.cloudstack.quota.activationrule.presetvariables;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleVO;
import org.apache.cloudstack.acl.dao.RoleDao;
import org.apache.cloudstack.backup.BackupOfferingVO;
import org.apache.cloudstack.backup.dao.BackupOfferingDao;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.quota.constant.QuotaTypes;
import org.apache.cloudstack.quota.dao.VmTemplateDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreVO;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.usage.UsageTypes;
import org.apache.cloudstack.utils.bytescale.ByteScaleUtils;
import org.apache.cloudstack.utils.jsinterpreter.JsInterpreter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostTagsDao;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.server.ResourceTag;
import com.cloud.server.ResourceTag.ResourceObjectType;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.StoragePoolTagsDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.usage.UsageVO;
import com.cloud.usage.dao.UsageDao;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmDetailVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.constants.VmDetails;
import com.cloud.vm.dao.UserVmDetailsDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.vm.snapshot.VMSnapshotVO;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;

/**
 * Retrieves data from the database to inject into the {@link JsInterpreter} to provide more flexibility when defining quota tariffs' activation rules.<br/></br>
 * As this class is strictly used to retrieve data to inject into the {@link JsInterpreter}, there is no need for logging the retrieved data, because {@link JsInterpreter} already
 * logs the injected data in TRACE level.
 */

@Component
public class PresetVariableHelper {
    protected Logger logger = Logger.getLogger(PresetVariableHelper.class);

    @Inject
    AccountDao accountDao;

    @Inject
    RoleDao roleDao;

    @Inject
    DomainDao domainDao;

    @Inject
    DataCenterDao dataCenterDao;

    @Inject
    UsageDao usageDao;

    @Inject
    VMInstanceDao vmInstanceDao;

    @Inject
    HostDao hostDao;

    @Inject
    HostTagsDao hostTagsDao;

    @Inject
    GuestOSDao guestOsDao;

    @Inject
    ServiceOfferingDao serviceOfferingDao;

    @Inject
    VmTemplateDao vmTemplateDao;

    @Inject
    ResourceTagDao resourceTagDao;

    @Inject
    VolumeDao volumeDao;

    @Inject
    DiskOfferingDao diskOfferingDao;

    @Inject
    PrimaryDataStoreDao primaryStorageDao;

    @Inject
    StoragePoolTagsDao storagePoolTagsDao;

    @Inject
    ImageStoreDao imageStoreDao;

    @Inject
    SnapshotDao snapshotDao;

    @Inject
    SnapshotDataStoreDao snapshotDataStoreDao;

    @Inject
    NetworkOfferingDao networkOfferingDao;

    @Inject
    VMSnapshotDao vmSnapshotDao;

    @Inject
    UserVmDetailsDao userVmDetailsDao;

    @Inject
    BackupOfferingDao backupOfferingDao;

    protected boolean backupSnapshotAfterTakingSnapshot = SnapshotInfo.BackupSnapshotAfterTakingSnapshot.value();

    private List<Integer> runningAndAllocatedVmUsageTypes = Arrays.asList(UsageTypes.RUNNING_VM, UsageTypes.ALLOCATED_VM);
    private List<Integer> templateAndIsoUsageTypes = Arrays.asList(UsageTypes.TEMPLATE, UsageTypes.ISO);
    private String usageRecordToString = null;

    public PresetVariables getPresetVariables(UsageVO usageRecord) {
        this.usageRecordToString = usageRecord.toString();
        PresetVariables presetVariables = new PresetVariables();

        presetVariables.setAccount(getPresetVariableAccount(usageRecord.getAccountId()));
        setPresetVariableProject(presetVariables);

        presetVariables.setDomain(getPresetVariableDomain(usageRecord.getDomainId()));
        presetVariables.setResourceType(usageRecord.getType());
        presetVariables.setValue(getPresetVariableValue(usageRecord));
        presetVariables.setZone(getPresetVariableZone(usageRecord.getZoneId()));

        return presetVariables;
    }

    protected void setPresetVariableProject(PresetVariables presetVariables) {
        Account account = presetVariables.getAccount();

        if (account.getRole() != null) {
            return;
        }

        GenericPresetVariable project = new GenericPresetVariable();
        project.setId(account.getId());
        project.setName(account.getName());

        presetVariables.setProject(project);
    }

    protected Account getPresetVariableAccount(Long accountId) {
        AccountVO accountVo = accountDao.findByIdIncludingRemoved(accountId);
        validateIfObjectIsNull(accountVo, accountId, "account");

        Account account = new Account();
        account.setId(accountVo.getUuid());
        account.setName(accountVo.getName());

        setPresetVariableRoleInAccountIfAccountIsNotAProject(accountVo.getType(), accountVo.getRoleId(), account);

        return account;
    }

    protected void setPresetVariableRoleInAccountIfAccountIsNotAProject(com.cloud.user.Account.Type accountType, Long roleId, Account account) {
        if (accountType != com.cloud.user.Account.Type.PROJECT) {
            account.setRole(getPresetVariableRole(roleId));
        }
    }

    protected Role getPresetVariableRole(Long roleId) {
        RoleVO roleVo = roleDao.findByIdIncludingRemoved(roleId);
        validateIfObjectIsNull(roleVo, roleId, "role");

        Role role = new Role();
        role.setId(roleVo.getUuid());
        role.setName(roleVo.getName());
        role.setType(roleVo.getRoleType());

        return role;
    }

    protected Domain getPresetVariableDomain(Long domainId) {
        DomainVO domainVo = domainDao.findByIdIncludingRemoved(domainId);
        validateIfObjectIsNull(domainVo, domainId, "domain");

        Domain domain = new Domain();
        domain.setId(domainVo.getUuid());
        domain.setName(domainVo.getName());
        domain.setPath(domainVo.getPath());

        return domain;
    }

    protected GenericPresetVariable getPresetVariableZone(Long zoneId) {
        DataCenterVO dataCenterVo = dataCenterDao.findByIdIncludingRemoved(zoneId);
        validateIfObjectIsNull(dataCenterVo, zoneId, "zone");

        GenericPresetVariable zone = new GenericPresetVariable();
        zone.setId(dataCenterVo.getUuid());
        zone.setName(dataCenterVo.getName());

        return zone;
    }

    protected Value getPresetVariableValue(UsageVO usageRecord) {
        Long accountId = usageRecord.getAccountId();
        int usageType = usageRecord.getUsageType();

        Value value = new Value();

        value.setAccountResources(getPresetVariableAccountResources(usageRecord, accountId, usageType));
        loadPresetVariableValueForRunningAndAllocatedVm(usageRecord, value);
        loadPresetVariableValueForVolume(usageRecord, value);
        loadPresetVariableValueForTemplateAndIso(usageRecord, value);
        loadPresetVariableValueForSnapshot(usageRecord, value);
        loadPresetVariableValueForNetworkOffering(usageRecord, value);
        loadPresetVariableValueForVmSnapshot(usageRecord, value);
        loadPresetVariableValueForBackup(usageRecord, value);

        return value;
    }

    /**
     * Retrieves a list of zone and domain IDs of the records allocated in the same period and same usage type of the usage record.
     */
    protected List<Resource> getPresetVariableAccountResources(UsageVO usageRecord, Long accountId, int usageType) {
        Date startDate = usageRecord.getStartDate();
        Date endDate = usageRecord.getEndDate();

        List<Pair<String, String>> pairResources = usageDao.listAccountResourcesInThePeriod(accountId, usageType, startDate, endDate);

        List<Resource> resourcesInThePeriod = new ArrayList<>();

        for (Pair<String, String> pairResource : pairResources) {
            Resource resource = new Resource();
            resource.setZoneId(pairResource.first());
            resource.setDomainId(pairResource.second());

            resourcesInThePeriod.add(resource);
        }

        return resourcesInThePeriod;
    }

    protected void loadPresetVariableValueForRunningAndAllocatedVm(UsageVO usageRecord, Value value) {
        int usageType = usageRecord.getUsageType();

        if (!runningAndAllocatedVmUsageTypes.contains(usageType)) {
            logNotLoadingMessageInTrace("running/allocated VM", usageType);
            return;
        }

        Long vmId = usageRecord.getUsageId();

        VMInstanceVO vmVo = vmInstanceDao.findByIdIncludingRemoved(vmId);
        validateIfObjectIsNull(vmVo, vmId, "VM");

        setPresetVariableHostInValueIfUsageTypeIsRunningVm(value, usageType, vmVo);

        value.setId(vmVo.getUuid());
        value.setName(vmVo.getHostName());
        value.setOsName(getPresetVariableValueOsName(vmVo.getGuestOSId()));

        setPresetVariableValueServiceOfferingAndComputingResources(value, usageType, vmVo);

        value.setTags(getPresetVariableValueResourceTags(vmId, ResourceObjectType.UserVm));
        value.setTemplate(getPresetVariableValueTemplate(vmVo.getTemplateId()));
    }

    protected void logNotLoadingMessageInTrace(String resource, int usageType) {
        logger.trace(String.format("Not loading %s preset variables because the usage record [%s] is of type [%s].", resource, usageRecordToString,
                QuotaTypes.listQuotaTypes().get(usageType).getQuotaName()));
    }

    protected void setPresetVariableHostInValueIfUsageTypeIsRunningVm(Value value, int quotaType, VMInstanceVO vmVo) {
        if (quotaType != UsageTypes.RUNNING_VM) {
            return;
        }

        Long hostId = vmVo.getHostId();
        if (hostId == null) {
            hostId = vmVo.getLastHostId();
        }

        value.setHost(getPresetVariableValueHost(hostId));
    }

    protected Host getPresetVariableValueHost(Long hostId) {
        HostVO hostVo = hostDao.findByIdIncludingRemoved(hostId);
        validateIfObjectIsNull(hostVo, hostId, "host");

        Host host = new Host();
        host.setId(hostVo.getUuid());
        host.setName(hostVo.getName());
        host.setTags(hostTagsDao.getHostTags(hostId));

        return host;
    }

    protected String getPresetVariableValueOsName(Long guestOsId) {
        GuestOSVO guestOsVo = guestOsDao.findByIdIncludingRemoved(guestOsId);
        validateIfObjectIsNull(guestOsVo, guestOsId, "guest OS");

        return guestOsVo.getDisplayName();
    }

    protected ComputeOffering getPresetVariableValueComputeOffering(ServiceOfferingVO serviceOfferingVo) {
        ComputeOffering computeOffering = new ComputeOffering();
        computeOffering.setId(serviceOfferingVo.getUuid());
        computeOffering.setName(serviceOfferingVo.getName());
        computeOffering.setCustomized(serviceOfferingVo.isDynamic());

        return computeOffering;
    }


    protected void setPresetVariableValueServiceOfferingAndComputingResources(Value value, int usageType, VMInstanceVO vmVo) {
        long computeOfferingId = vmVo.getServiceOfferingId();
        ServiceOfferingVO serviceOfferingVo = serviceOfferingDao.findByIdIncludingRemoved(computeOfferingId);
        validateIfObjectIsNull(serviceOfferingVo, computeOfferingId, "compute offering");
        value.setComputeOffering(getPresetVariableValueComputeOffering(serviceOfferingVo));

        if (usageType == UsageTypes.RUNNING_VM) {
            value.setComputingResources(getPresetVariableValueComputingResource(vmVo, serviceOfferingVo));
        }
    }

    protected ComputingResources getPresetVariableValueComputingResource(VMInstanceVO vmVo, ServiceOfferingVO serviceOfferingVo) {
        ComputingResources computingResources = new ComputingResources();
        computingResources.setMemory(serviceOfferingVo.getRamSize());
        computingResources.setCpuNumber(serviceOfferingVo.getCpu());
        computingResources.setCpuSpeed(serviceOfferingVo.getSpeed());

        if (serviceOfferingVo.isDynamic()) {
            List<UserVmDetailVO> details = userVmDetailsDao.listDetails(vmVo.getId());

            computingResources.setMemory(getDetailByName(details, VmDetails.MEMORY.getName(), computingResources.getMemory()));
            computingResources.setCpuNumber(getDetailByName(details, VmDetails.CPU_NUMBER.getName(), computingResources.getCpuNumber()));
            computingResources.setCpuSpeed(getDetailByName(details, VmDetails.CPU_SPEED.getName(), computingResources.getCpuSpeed()));
        }

        warnIfComputingResourceIsNull(VmDetails.MEMORY.getName(), computingResources.getMemory(), vmVo);
        warnIfComputingResourceIsNull(VmDetails.CPU_NUMBER.getName(), computingResources.getCpuNumber(), vmVo);
        warnIfComputingResourceIsNull(VmDetails.CPU_SPEED.getName(), computingResources.getCpuSpeed(), vmVo);

        return computingResources;
    }

    protected void warnIfComputingResourceIsNull(String name, Integer value, VMInstanceVO vmVo) {
        if (value == null) {
            logger.warn(String.format("Could not get %s of %s. Injecting \"value.computingResources.[%s]\" as null.", name, vmVo, name));
        }
    }

    protected Integer getDetailByName(List<UserVmDetailVO> details, String name, Integer defaultValue) {
        List<UserVmDetailVO> detailFiltered = details.stream().filter(det -> name.equals(det.getName())).collect(Collectors.toList());

        if (CollectionUtils.isEmpty(detailFiltered)) {
            return defaultValue;
        }

        UserVmDetailVO detail = detailFiltered.get(0);

        if (detail.getValue() != null) {
            return Integer.valueOf(detail.getValue());
        }

        return defaultValue;
    }
    protected GenericPresetVariable getPresetVariableValueTemplate(Long templateId) {
        VMTemplateVO vmTemplateVo = vmTemplateDao.findByIdIncludingRemoved(templateId);
        validateIfObjectIsNull(vmTemplateVo, templateId, "template");

        GenericPresetVariable template = new GenericPresetVariable();
        template.setId(vmTemplateVo.getUuid());
        template.setName(vmTemplateVo.getName());

        return template;
    }

    protected Map<String, String> getPresetVariableValueResourceTags(Long resourceId, ResourceObjectType resourceType) {
        List<? extends ResourceTag> listResourceTags = resourceTagDao.listBy(resourceId, resourceType);

        Map<String, String> mapResourceTags = new HashMap<>();
        for (ResourceTag resourceTag : listResourceTags) {
            mapResourceTags.put(resourceTag.getKey(), resourceTag.getValue());
        }

        return mapResourceTags;
    }

    protected void loadPresetVariableValueForVolume(UsageVO usageRecord, Value value) {
        int usageType = usageRecord.getUsageType();

        if (usageType != UsageTypes.VOLUME) {
            logNotLoadingMessageInTrace("volume", usageType);
            return;
        }

        Long volumeId = usageRecord.getUsageId();

        VolumeVO volumeVo = volumeDao.findByIdIncludingRemoved(volumeId);
        validateIfObjectIsNull(volumeVo, volumeId, "volume");

        value.setDiskOffering(getPresetVariableValueDiskOffering(volumeVo.getDiskOfferingId()));
        value.setId(volumeVo.getUuid());
        value.setName(volumeVo.getName());
        value.setProvisioningType(volumeVo.getProvisioningType());

        Long poolId = volumeVo.getPoolId();
        if (poolId == null) {
            logger.debug(String.format("Volume [%s] from usage record [%s] has a NULL pool ID; therefore, the preset variable \"storage\" will not be loaded for this record.",
                    volumeId, usageRecordToString));
        } else {
            value.setStorage(getPresetVariableValueStorage(poolId, usageType));
        }

        value.setTags(getPresetVariableValueResourceTags(volumeId, ResourceObjectType.Volume));
        value.setSize(ByteScaleUtils.bytesToMebibytes(volumeVo.getSize()));
    }

    protected GenericPresetVariable getPresetVariableValueDiskOffering(Long diskOfferingId) {
        DiskOfferingVO diskOfferingVo = diskOfferingDao.findByIdIncludingRemoved(diskOfferingId);
        validateIfObjectIsNull(diskOfferingVo, diskOfferingId, "disk offering");

        GenericPresetVariable diskOffering = new GenericPresetVariable();
        diskOffering.setId(diskOfferingVo.getUuid());
        diskOffering.setName(diskOfferingVo.getName());

        return diskOffering;
    }

    protected Storage getPresetVariableValueStorage(Long storageId, int usageType) {
        Storage storage = getSecondaryStorageForSnapshot(storageId, usageType);

        if (storage != null) {
            return storage;
        }

        StoragePoolVO storagePoolVo = primaryStorageDao.findByIdIncludingRemoved(storageId);
        validateIfObjectIsNull(storagePoolVo, storageId, "primary storage");

        storage = new Storage();
        storage.setId(storagePoolVo.getUuid());
        storage.setName(storagePoolVo.getName());
        storage.setScope(storagePoolVo.getScope());
        storage.setTags(storagePoolTagsDao.getStoragePoolTags(storageId));

        return storage;
    }

    /**
     * If the usage type is {@link UsageTypes#SNAPSHOT} and {@link SnapshotInfo#BackupSnapshotAfterTakingSnapshot} is enabled, returns the data from the secondary storage.
     *  Otherwise, returns null.
     */
    protected Storage getSecondaryStorageForSnapshot(Long storageId, int usageType) {
        if (usageType != UsageTypes.SNAPSHOT || !backupSnapshotAfterTakingSnapshot) {
            return null;
        }

        ImageStoreVO imageStoreVo = imageStoreDao.findByIdIncludingRemoved(storageId);
        validateIfObjectIsNull(imageStoreVo, storageId, "secondary storage");

        Storage storage = new Storage();
        storage.setId(imageStoreVo.getUuid());
        storage.setName(imageStoreVo.getName());
        return storage;
    }

    protected void loadPresetVariableValueForTemplateAndIso(UsageVO usageRecord, Value value) {
        int usageType = usageRecord.getUsageType();
        if (!templateAndIsoUsageTypes.contains(usageType)) {
            logNotLoadingMessageInTrace("template/ISO", usageType);
            return;
        }

        Long templateOrIsoId = usageRecord.getUsageId();

        VMTemplateVO vmTemplateVo = vmTemplateDao.findByIdIncludingRemoved(templateOrIsoId);
        validateIfObjectIsNull(vmTemplateVo, templateOrIsoId, "template/ISO");

        value.setId(vmTemplateVo.getUuid());
        value.setName(vmTemplateVo.getName());
        value.setOsName(getPresetVariableValueOsName(vmTemplateVo.getGuestOSId()));
        value.setTags(getPresetVariableValueResourceTags(templateOrIsoId, usageType == UsageTypes.ISO ? ResourceObjectType.ISO : ResourceObjectType.Template));
        value.setSize(ByteScaleUtils.bytesToMebibytes(vmTemplateVo.getSize()));
    }

    protected void loadPresetVariableValueForSnapshot(UsageVO usageRecord, Value value) {
        int usageType = usageRecord.getUsageType();

        if (usageType != UsageTypes.SNAPSHOT) {
            logNotLoadingMessageInTrace("snapshot", usageType);
            return;
        }

        Long snapshotId = usageRecord.getUsageId();

        SnapshotVO snapshotVo = snapshotDao.findByIdIncludingRemoved(snapshotId);
        validateIfObjectIsNull(snapshotVo, snapshotId, "snapshot");

        value.setId(snapshotVo.getUuid());
        value.setName(snapshotVo.getName());
        value.setSize(ByteScaleUtils.bytesToMebibytes(snapshotVo.getSize()));
        value.setSnapshotType(Snapshot.Type.values()[snapshotVo.getSnapshotType()]);
        value.setStorage(getPresetVariableValueStorage(getSnapshotDataStoreId(snapshotId, usageRecord.getZoneId()), usageType));
        value.setTags(getPresetVariableValueResourceTags(snapshotId, ResourceObjectType.Snapshot));
    }

    protected SnapshotDataStoreVO getSnapshotImageStoreRef(long snapshotId, long zoneId) {
        List<SnapshotDataStoreVO> snaps = snapshotDataStoreDao.listReadyBySnapshot(snapshotId, DataStoreRole.Image);
        for (SnapshotDataStoreVO ref : snaps) {
            ImageStoreVO store = imageStoreDao.findById(ref.getDataStoreId());
            if (store != null && zoneId == store.getDataCenterId()) {
                return ref;
            }
        }
        return null;
    }

    /**
     * If {@link SnapshotInfo#BackupSnapshotAfterTakingSnapshot} is enabled, returns the secondary storage's ID where the snapshot is. Otherwise, returns the primary storage's ID
     *  where the snapshot is.
     */
    protected long getSnapshotDataStoreId(Long snapshotId, long zoneId) {
        if (backupSnapshotAfterTakingSnapshot) {
            SnapshotDataStoreVO snapshotStore = getSnapshotImageStoreRef(snapshotId, zoneId);
            validateIfObjectIsNull(snapshotStore, snapshotId, "data store for snapshot");
            return snapshotStore.getDataStoreId();
        }

        SnapshotDataStoreVO snapshotStore = snapshotDataStoreDao.findOneBySnapshotAndDatastoreRole(snapshotId, DataStoreRole.Primary);
        validateIfObjectIsNull(snapshotStore, snapshotId, "data store for snapshot");
        return snapshotStore.getDataStoreId();
    }

    protected void loadPresetVariableValueForNetworkOffering(UsageVO usageRecord, Value value) {
        int usageType = usageRecord.getUsageType();

        if (usageType != UsageTypes.NETWORK_OFFERING) {
            logNotLoadingMessageInTrace("network offering", usageType);
            return;
        }

        Long networkOfferingId = usageRecord.getOfferingId();

        NetworkOfferingVO networkOfferingVo = networkOfferingDao.findByIdIncludingRemoved(networkOfferingId);
        validateIfObjectIsNull(networkOfferingVo, networkOfferingId, "network offering");

        value.setId(networkOfferingVo.getUuid());
        value.setName(networkOfferingVo.getName());
        value.setTag(networkOfferingVo.getTags());
    }

    protected void loadPresetVariableValueForVmSnapshot(UsageVO usageRecord, Value value) {
        int usageType = usageRecord.getUsageType();
        if (usageType != UsageTypes.VM_SNAPSHOT) {
            logNotLoadingMessageInTrace("VM snapshot", usageType);
            return;
        }

        Long vmSnapshotId = usageRecord.getUsageId();

        VMSnapshotVO vmSnapshotVo = vmSnapshotDao.findByIdIncludingRemoved(vmSnapshotId);
        validateIfObjectIsNull(vmSnapshotVo, vmSnapshotId, "VM snapshot");

        value.setId(vmSnapshotVo.getUuid());
        value.setName(vmSnapshotVo.getName());
        value.setTags(getPresetVariableValueResourceTags(vmSnapshotId, ResourceObjectType.VMSnapshot));
        value.setVmSnapshotType(vmSnapshotVo.getType());
    }

    protected void loadPresetVariableValueForBackup(UsageVO usageRecord, Value value) {
        int usageType = usageRecord.getUsageType();
        if (usageType != UsageTypes.BACKUP) {
            logNotLoadingMessageInTrace("Backup", usageType);
            return;
        }

        value.setSize(usageRecord.getSize());
        value.setVirtualSize(usageRecord.getVirtualSize());
        value.setBackupOffering(getPresetVariableValueBackupOffering(usageRecord.getOfferingId()));
    }

    protected BackupOffering getPresetVariableValueBackupOffering(Long offeringId) {
        BackupOfferingVO backupOfferingVo = backupOfferingDao.findByIdIncludingRemoved(offeringId);
        validateIfObjectIsNull(backupOfferingVo, offeringId, "backup offering");

        BackupOffering backupOffering = new BackupOffering();
        backupOffering.setId(backupOfferingVo.getUuid());
        backupOffering.setName(backupOfferingVo.getName());
        backupOffering.setExternalId(backupOfferingVo.getExternalId());

        return backupOffering;
    }

    /**
     * Throws a {@link CloudRuntimeException} if the object is null;
     */
    protected void validateIfObjectIsNull(Object object, Long id, String resource) {
        if (object != null) {
            return;
        }

        String message = String.format("Unable to load preset variable [%s] for usage record [%s] due to: [%s] with ID [%s] does not exist.", resource, usageRecordToString,
                resource, id);

        logger.error(message);
        throw new CloudRuntimeException(message);
    }
}
