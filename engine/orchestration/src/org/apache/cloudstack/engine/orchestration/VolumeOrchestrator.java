/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.engine.orchestration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.apache.cloudstack.engine.orchestration.service.VolumeOrchestrationService;
import org.apache.cloudstack.engine.subsystem.api.storage.ChapInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.StoragePoolAllocator;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService.VolumeApiResult;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.framework.config.ConfigDepot;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;

import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.manager.allocator.PodAllocator;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.dc.DataCenter;
import com.cloud.dc.Pod;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientStorageCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.Host;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.org.Cluster;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Snapshot;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.StoragePool;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.Volume;
import com.cloud.storage.Volume.Type;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.template.TemplateManager;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.ResourceLimitService;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.VirtualMachineProfileImpl;

public class VolumeOrchestrator extends ManagerBase implements VolumeOrchestrationService, Configurable {
    private static final Logger s_logger = Logger.getLogger(VolumeOrchestrator.class);

    @Inject
    EntityManager _entityMgr;
    @Inject
    protected TemplateManager _tmpltMgr;
    @Inject
    protected VolumeDao _volsDao;
    @Inject
    protected PrimaryDataStoreDao _storagePoolDao = null;
    @Inject
    protected TemplateDataStoreDao _vmTemplateStoreDao = null;
    @Inject
    protected VolumeDao _volumeDao;
    @Inject
    protected ResourceLimitService _resourceLimitMgr;
    @Inject
    VolumeDetailsDao _volDetailDao;
    @Inject
    DataStoreManager dataStoreMgr;
    @Inject
    VolumeService volService;
    @Inject
    VolumeDataFactory volFactory;
    @Inject
    TemplateDataFactory tmplFactory;
    @Inject
    SnapshotDataFactory snapshotFactory;
    @Inject
    ConfigDepot _configDepot;

    private final StateMachine2<Volume.State, Volume.Event, Volume> _volStateMachine;
    protected List<StoragePoolAllocator> _storagePoolAllocators;

    public List<StoragePoolAllocator> getStoragePoolAllocators() {
        return _storagePoolAllocators;
    }

    public void setStoragePoolAllocators(List<StoragePoolAllocator> _storagePoolAllocators) {
        this._storagePoolAllocators = _storagePoolAllocators;
    }

    protected List<PodAllocator> _podAllocators;

    public List<PodAllocator> getPodAllocators() {
        return _podAllocators;
    }

    public void setPodAllocators(List<PodAllocator> _podAllocators) {
        this._podAllocators = _podAllocators;
    }

    protected VolumeOrchestrator() {
        _volStateMachine = Volume.State.getStateMachine();
    }

    @Override
    public VolumeInfo moveVolume(VolumeInfo volume, long destPoolDcId, Long destPoolPodId, Long destPoolClusterId, HypervisorType dataDiskHyperType)
            throws ConcurrentOperationException, StorageUnavailableException {

        // Find a destination storage pool with the specified criteria
        DiskOffering diskOffering = _entityMgr.findById(DiskOffering.class, volume.getDiskOfferingId());
;
        DiskProfile dskCh = new DiskProfile(volume.getId(),
            volume.getVolumeType(),
            volume.getName(),
            diskOffering.getId(),
            diskOffering.getDiskSize(),
            diskOffering.getTagsArray(),
            diskOffering.getUseLocalStorage(),
            diskOffering.isRecreatable(),
            null);
        dskCh.setHyperType(dataDiskHyperType);
        DataCenter destPoolDataCenter = _entityMgr.findById(DataCenter.class, destPoolDcId);
        Pod destPoolPod = _entityMgr.findById(Pod.class, destPoolPodId);

        StoragePool destPool = findStoragePool(dskCh, destPoolDataCenter, destPoolPod, destPoolClusterId, null, null, new HashSet<StoragePool>());

        if (destPool == null) {
            throw new CloudRuntimeException("Failed to find a storage pool with enough capacity to move the volume to.");
        }

        Volume newVol = migrateVolume(volume, destPool);
        return volFactory.getVolume(newVol.getId());
    }

    @Override
    public Volume allocateDuplicateVolume(Volume oldVol, Long templateId) {
        return allocateDuplicateVolumeVO(oldVol, templateId);
    }

    public VolumeVO allocateDuplicateVolumeVO(Volume oldVol, Long templateId) {
        VolumeVO newVol = new VolumeVO(oldVol.getVolumeType(),
            oldVol.getName(),
            oldVol.getDataCenterId(),
            oldVol.getDomainId(),
            oldVol.getAccountId(),
            oldVol.getDiskOfferingId(),
            oldVol.getSize(),
            oldVol.getMinIops(),
            oldVol.getMaxIops(),
            oldVol.get_iScsiName());
        if (templateId != null) {
            newVol.setTemplateId(templateId);
        } else {
            newVol.setTemplateId(oldVol.getTemplateId());
        }
        newVol.setDeviceId(oldVol.getDeviceId());
        newVol.setInstanceId(oldVol.getInstanceId());
        newVol.setRecreatable(oldVol.isRecreatable());
        newVol.setFormat(oldVol.getFormat());
        return _volsDao.persist(newVol);
    }

    @Override
    public StoragePool findStoragePool(DiskProfile dskCh, DataCenter dc, Pod pod, Long clusterId, Long hostId, VirtualMachine vm, final Set<StoragePool> avoid) {
        Long podId = null;
        if (pod != null) {
            podId = pod.getId();
        } else if (clusterId != null) {
            Cluster cluster = _entityMgr.findById(Cluster.class, clusterId);
            if (cluster != null) {
                podId = cluster.getPodId();
            }
        }

        VirtualMachineProfile profile = new VirtualMachineProfileImpl(vm);
        for (StoragePoolAllocator allocator : _storagePoolAllocators) {

            ExcludeList avoidList = new ExcludeList();
            for (StoragePool pool : avoid) {
                avoidList.addPool(pool.getId());
            }
            DataCenterDeployment plan = new DataCenterDeployment(dc.getId(), podId, clusterId, hostId, null, null);

            final List<StoragePool> poolList = allocator.allocateToPool(dskCh, profile, plan, avoidList, 1);
            if (poolList != null && !poolList.isEmpty()) {
                return (StoragePool)dataStoreMgr.getDataStore(poolList.get(0).getId(), DataStoreRole.Primary);
            }
        }
        return null;
    }

    public Pair<Pod, Long> findPod(VirtualMachineTemplate template, ServiceOffering offering, DataCenter dc, long accountId, Set<Long> avoids) {
        for (PodAllocator allocator : _podAllocators) {
            final Pair<Pod, Long> pod = allocator.allocateTo(template, offering, dc, accountId, avoids);
            if (pod != null) {
                return pod;
            }
        }
        return null;
    }

    @DB
    @Override
    public VolumeInfo createVolumeFromSnapshot(Volume volume, Snapshot snapshot) throws StorageUnavailableException {
        Account account = _entityMgr.findById(Account.class, volume.getAccountId());

        final HashSet<StoragePool> poolsToAvoid = new HashSet<StoragePool>();
        StoragePool pool = null;

        Set<Long> podsToAvoid = new HashSet<Long>();
        Pair<Pod, Long> pod = null;

        DiskOffering diskOffering = _entityMgr.findById(DiskOffering.class, volume.getDiskOfferingId());
        DataCenter dc = _entityMgr.findById(DataCenter.class, volume.getDataCenterId());
        DiskProfile dskCh = new DiskProfile(volume, diskOffering, snapshot.getHypervisorType());

        // Determine what pod to store the volume in
        while ((pod = findPod(null, null, dc, account.getId(), podsToAvoid)) != null) {
            podsToAvoid.add(pod.first().getId());
            // Determine what storage pool to store the volume in
            while ((pool = findStoragePool(dskCh, dc, pod.first(), null, null, null, poolsToAvoid)) != null) {
                break;
            }
        }

        if (pool == null) {
            String msg = "There are no available storage pools to store the volume in";
            s_logger.info(msg);
            throw new StorageUnavailableException(msg, -1);
        }

        VolumeInfo vol = volFactory.getVolume(volume.getId());
        DataStore store = dataStoreMgr.getDataStore(pool.getId(), DataStoreRole.Primary);
        SnapshotInfo snapInfo = snapshotFactory.getSnapshot(snapshot.getId(), DataStoreRole.Image);
        AsyncCallFuture<VolumeApiResult> future = volService.createVolumeFromSnapshot(vol, store, snapInfo);
        try {
            VolumeApiResult result = future.get();
            if (result.isFailed()) {
                s_logger.debug("Failed to create volume from snapshot:" + result.getResult());
                throw new CloudRuntimeException("Failed to create volume from snapshot:" + result.getResult());
            }
            return result.getVolume();
        } catch (InterruptedException e) {
            s_logger.debug("Failed to create volume from snapshot", e);
            throw new CloudRuntimeException("Failed to create volume from snapshot", e);
        } catch (ExecutionException e) {
            s_logger.debug("Failed to create volume from snapshot", e);
            throw new CloudRuntimeException("Failed to create volume from snapshot", e);
        }

    }

    protected DiskProfile createDiskCharacteristics(VolumeInfo volume, VirtualMachineTemplate template, DataCenter dc, DiskOffering diskOffering) {
        if (volume.getVolumeType() == Type.ROOT && Storage.ImageFormat.ISO != template.getFormat()) {
            TemplateDataStoreVO ss = _vmTemplateStoreDao.findByTemplateZoneDownloadStatus(template.getId(), dc.getId(), VMTemplateStorageResourceAssoc.Status.DOWNLOADED);
            if (ss == null) {
                throw new CloudRuntimeException("Template " + template.getName() + " has not been completely downloaded to zone " + dc.getId());
            }

            return new DiskProfile(volume.getId(),
                volume.getVolumeType(),
                volume.getName(),
                diskOffering.getId(),
                ss.getSize(),
                diskOffering.getTagsArray(),
                diskOffering.getUseLocalStorage(),
                diskOffering.isRecreatable(),
                Storage.ImageFormat.ISO != template.getFormat() ? template.getId() : null);
        } else {
            return new DiskProfile(volume.getId(),
                volume.getVolumeType(),
                volume.getName(),
                diskOffering.getId(),
                diskOffering.getDiskSize(),
                diskOffering.getTagsArray(),
                diskOffering.getUseLocalStorage(),
                diskOffering.isRecreatable(),
                null);
        }
    }

    @DB
    public VolumeInfo copyVolumeFromSecToPrimary(VolumeInfo volume, VirtualMachine vm, VirtualMachineTemplate template, DataCenter dc, Pod pod, Long clusterId,
            ServiceOffering offering, DiskOffering diskOffering, List<StoragePool> avoids, long size, HypervisorType hyperType) throws NoTransitionException {

        final HashSet<StoragePool> avoidPools = new HashSet<StoragePool>(avoids);
        DiskProfile dskCh = createDiskCharacteristics(volume, template, dc, diskOffering);
        dskCh.setHyperType(vm.getHypervisorType());
        // Find a suitable storage to create volume on
        StoragePool destPool = findStoragePool(dskCh, dc, pod, clusterId, null, vm, avoidPools);
        DataStore destStore = dataStoreMgr.getDataStore(destPool.getId(), DataStoreRole.Primary);
        AsyncCallFuture<VolumeApiResult> future = volService.copyVolume(volume, destStore);

        try {
            VolumeApiResult result = future.get();
            if (result.isFailed()) {
                s_logger.debug("copy volume failed: " + result.getResult());
                throw new CloudRuntimeException("copy volume failed: " + result.getResult());
            }
            return result.getVolume();
        } catch (InterruptedException e) {
            s_logger.debug("Failed to copy volume: " + volume.getId(), e);
            throw new CloudRuntimeException("Failed to copy volume", e);
        } catch (ExecutionException e) {
            s_logger.debug("Failed to copy volume: " + volume.getId(), e);
            throw new CloudRuntimeException("Failed to copy volume", e);
        }
    }

    @DB
    public VolumeInfo createVolume(VolumeInfo volume, VirtualMachine vm, VirtualMachineTemplate template, DataCenter dc, Pod pod, Long clusterId, ServiceOffering offering,
            DiskOffering diskOffering, List<StoragePool> avoids, long size, HypervisorType hyperType) {
        StoragePool pool = null;

        if (diskOffering != null && diskOffering.isCustomized()) {
            diskOffering.setDiskSize(size);
        }

        DiskProfile dskCh = null;
        if (volume.getVolumeType() == Type.ROOT && Storage.ImageFormat.ISO != template.getFormat()) {
            dskCh = createDiskCharacteristics(volume, template, dc, offering);
        } else {
            dskCh = createDiskCharacteristics(volume, template, dc, diskOffering);
        }

        dskCh.setHyperType(hyperType);

        final HashSet<StoragePool> avoidPools = new HashSet<StoragePool>(avoids);

        pool = findStoragePool(dskCh, dc, pod, clusterId, vm.getHostId(), vm, avoidPools);
        if (pool == null) {
            s_logger.warn("Unable to find storage pool when create volume " + volume.getName());
            throw new CloudRuntimeException("Unable to find storage pool when create volume" + volume.getName());
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Trying to create " + volume + " on " + pool);
        }
        DataStore store = dataStoreMgr.getDataStore(pool.getId(), DataStoreRole.Primary);
        for (int i = 0; i < 2; i++) {
            // retry one more time in case of template reload is required for Vmware case
            AsyncCallFuture<VolumeApiResult> future = null;
            boolean isNotCreatedFromTemplate = volume.getTemplateId() == null ? true : false;
            if (isNotCreatedFromTemplate) {
                future = volService.createVolumeAsync(volume, store);
            } else {
                TemplateInfo templ = tmplFactory.getTemplate(template.getId(), DataStoreRole.Image);
                future = volService.createVolumeFromTemplateAsync(volume, store.getId(), templ);
            }
            try {
                VolumeApiResult result = future.get();
                if (result.isFailed()) {
                    if (result.getResult().contains("request template reload") && (i == 0)) {
                        s_logger.debug("Retry template re-deploy for vmware");
                        continue;
                    } else {
                        s_logger.debug("create volume failed: " + result.getResult());
                        throw new CloudRuntimeException("create volume failed:" + result.getResult());
                    }
                }

                return result.getVolume();
            } catch (InterruptedException e) {
                s_logger.error("create volume failed", e);
                throw new CloudRuntimeException("create volume failed", e);
            } catch (ExecutionException e) {
                s_logger.error("create volume failed", e);
                throw new CloudRuntimeException("create volume failed", e);
            }
        }
        throw new CloudRuntimeException("create volume failed even after template re-deploy");
    }

    public String getRandomVolumeName() {
        return UUID.randomUUID().toString();
    }

    @Override
    public boolean volumeOnSharedStoragePool(Volume volume) {
        Long poolId = volume.getPoolId();
        if (poolId == null) {
            return false;
        } else {
            StoragePoolVO pool = _storagePoolDao.findById(poolId);

            if (pool == null) {
                return false;
            } else {
                return (pool.getScope() == ScopeType.HOST) ? false : true;
            }
        }
    }

    @Override
    public boolean volumeInactive(Volume volume) {
        Long vmId = volume.getInstanceId();
        if (vmId != null) {
            UserVm vm = _entityMgr.findById(UserVm.class, vmId);
            if (vm == null) {
                return true;
            }
            State state = vm.getState();
            if (state.equals(State.Stopped) || state.equals(State.Destroyed)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getVmNameOnVolume(Volume volume) {
        Long vmId = volume.getInstanceId();
        if (vmId != null) {
            VirtualMachine vm = _entityMgr.findById(VirtualMachine.class, vmId);

            if (vm == null) {
                return null;
            }
            return vm.getInstanceName();
        }
        return null;
    }

    @Override
    public boolean validateVolumeSizeRange(long size) {
        if (size < 0 || (size > 0 && size < (1024 * 1024 * 1024))) {
            throw new InvalidParameterValueException("Please specify a size of at least 1 Gb.");
        } else if (size > (MaxVolumeSize.value() * 1024 * 1024 * 1024)) {
            throw new InvalidParameterValueException("volume size " + size + ", but the maximum size allowed is " + MaxVolumeSize + " Gb.");
        }

        return true;
    }

    protected DiskProfile toDiskProfile(Volume vol, DiskOffering offering) {
        return new DiskProfile(vol.getId(),
            vol.getVolumeType(),
            vol.getName(),
            offering.getId(),
            vol.getSize(),
            offering.getTagsArray(),
            offering.getUseLocalStorage(),
            offering.isRecreatable(),
            vol.getTemplateId());
    }

    @Override
    public DiskProfile allocateRawVolume(Type type, String name, DiskOffering offering, Long size, VirtualMachine vm, VirtualMachineTemplate template, Account owner) {
        if (size == null) {
            size = offering.getDiskSize();
        } else {
            size = (size * 1024 * 1024 * 1024);
        }
        VolumeVO vol = new VolumeVO(type,
            name,
            vm.getDataCenterId(),
            owner.getDomainId(),
            owner.getId(),
            offering.getId(),
            size,
            offering.getMinIops(),
            offering.getMaxIops(),
            null);
        if (vm != null) {
            vol.setInstanceId(vm.getId());
        }

        if (type.equals(Type.ROOT)) {
            vol.setDeviceId(0l);
        } else {
            vol.setDeviceId(1l);
        }
        if (template.getFormat() == ImageFormat.ISO) {
            vol.setIsoId(template.getId());
        }

        vol.setFormat(getSupportedImageFormatForCluster(vm.getHypervisorType()));
        vol = _volsDao.persist(vol);

        // Save usage event and update resource count for user vm volumes
        if (vm instanceof UserVm) {

            UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VOLUME_CREATE,
                vol.getAccountId(),
                vol.getDataCenterId(),
                vol.getId(),
                vol.getName(),
                offering.getId(),
                null,
                size,
                Volume.class.getName(),
                vol.getUuid());

            _resourceLimitMgr.incrementResourceCount(vm.getAccountId(), ResourceType.volume);
            _resourceLimitMgr.incrementResourceCount(vm.getAccountId(), ResourceType.primary_storage, new Long(vol.getSize()));
        }
        return toDiskProfile(vol, offering);
    }

    @Override
    public DiskProfile allocateTemplatedVolume(Type type, String name, DiskOffering offering, VirtualMachineTemplate template, VirtualMachine vm, Account owner) {
        assert (template.getFormat() != ImageFormat.ISO) : "ISO is not a template really....";

        Long size = _tmpltMgr.getTemplateSize(template.getId(), vm.getDataCenterId());

        VolumeVO vol = new VolumeVO(type,
            name,
            vm.getDataCenterId(),
            owner.getDomainId(),
            owner.getId(),
            offering.getId(),
            size,
            offering.getMinIops(),
            offering.getMaxIops(),
            null);
        vol.setFormat(getSupportedImageFormatForCluster(template.getHypervisorType()));
        if (vm != null) {
            vol.setInstanceId(vm.getId());
        }
        vol.setTemplateId(template.getId());

        if (type.equals(Type.ROOT)) {
            vol.setDeviceId(0l);
            if (!vm.getType().equals(VirtualMachine.Type.User)) {
                vol.setRecreatable(true);
            }
        } else {
            vol.setDeviceId(1l);
        }

        vol = _volsDao.persist(vol);

        // Create event and update resource count for volumes if vm is a user vm
        if (vm instanceof UserVm) {

            Long offeringId = null;

            offeringId = offering.getId();

            UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VOLUME_CREATE,
                vol.getAccountId(),
                vol.getDataCenterId(),
                vol.getId(),
                vol.getName(),
                offeringId,
                null,
                size,
                Volume.class.getName(),
                vol.getUuid());

            _resourceLimitMgr.incrementResourceCount(vm.getAccountId(), ResourceType.volume);
            _resourceLimitMgr.incrementResourceCount(vm.getAccountId(), ResourceType.primary_storage, new Long(vol.getSize()));
        }
        return toDiskProfile(vol, offering);
    }

    private ImageFormat getSupportedImageFormatForCluster(HypervisorType hyperType) {
        if (hyperType == HypervisorType.XenServer) {
            return ImageFormat.VHD;
        } else if (hyperType == HypervisorType.KVM) {
            return ImageFormat.QCOW2;
        } else if (hyperType == HypervisorType.VMware) {
            return ImageFormat.OVA;
        } else if (hyperType == HypervisorType.Ovm) {
            return ImageFormat.RAW;
        } else {
            return null;
        }
    }

    private VolumeInfo copyVolume(StoragePool rootDiskPool, VolumeInfo volume, VirtualMachine vm, VirtualMachineTemplate rootDiskTmplt, DataCenter dcVO, Pod pod,
            DiskOffering diskVO, ServiceOffering svo, HypervisorType rootDiskHyperType) throws NoTransitionException {

        if (!volume.getFormat().equals(getSupportedImageFormatForCluster(rootDiskHyperType))) {
            throw new InvalidParameterValueException("Failed to attach volume to VM since volumes format " + volume.getFormat().getFileExtension() +
                                                     " is not compatible with the vm hypervisor type");
        }

        VolumeInfo volumeOnPrimary = copyVolumeFromSecToPrimary(volume,
            vm,
            rootDiskTmplt,
            dcVO,
            pod,
            rootDiskPool.getClusterId(),
            svo,
            diskVO,
            new ArrayList<StoragePool>(),
            volume.getSize(),
            rootDiskHyperType);

        return volumeOnPrimary;
    }

    @Override
    public VolumeInfo createVolumeOnPrimaryStorage(VirtualMachine vm, Volume rootVolumeOfVm, VolumeInfo volume, HypervisorType rootDiskHyperType) throws NoTransitionException {
        VirtualMachineTemplate rootDiskTmplt = _entityMgr.findById(VirtualMachineTemplate.class, vm.getTemplateId());
        DataCenter dcVO = _entityMgr.findById(DataCenter.class, vm.getDataCenterId());
        Pod pod = _entityMgr.findById(Pod.class, vm.getPodIdToDeployIn());
        StoragePoolVO rootDiskPool = _storagePoolDao.findById(rootVolumeOfVm.getPoolId());
        ServiceOffering svo = _entityMgr.findById(ServiceOffering.class, vm.getServiceOfferingId());
        DiskOffering diskVO = _entityMgr.findById(DiskOffering.class, volume.getDiskOfferingId());
        Long clusterId = (rootDiskPool == null ? null : rootDiskPool.getClusterId());

        VolumeInfo vol = null;
        if (volume.getState() == Volume.State.Allocated) {
            vol = createVolume(volume, vm, rootDiskTmplt, dcVO, pod, clusterId, svo, diskVO, new ArrayList<StoragePool>(), volume.getSize(), rootDiskHyperType);
        } else if (volume.getState() == Volume.State.Uploaded) {
            vol = copyVolume(rootDiskPool, volume, vm, rootDiskTmplt, dcVO, pod, diskVO, svo, rootDiskHyperType);
            if (vol != null) {
                // Moving of Volume is successful, decrement the volume resource count from secondary for an account and increment it into primary storage under same account.
                _resourceLimitMgr.decrementResourceCount(volume.getAccountId(), ResourceType.secondary_storage, new Long(volume.getSize()));
                _resourceLimitMgr.incrementResourceCount(volume.getAccountId(), ResourceType.primary_storage, new Long(volume.getSize()));
            }
        }

        VolumeVO volVO = _volsDao.findById(vol.getId());
        volVO.setFormat(getSupportedImageFormatForCluster(rootDiskHyperType));
        _volsDao.update(volVO.getId(), volVO);
        return volFactory.getVolume(volVO.getId());
    }

    @DB
    protected VolumeVO switchVolume(final VolumeVO existingVolume, final VirtualMachineProfile vm) throws StorageUnavailableException {
        Long templateIdToUse = null;
        Long volTemplateId = existingVolume.getTemplateId();
        long vmTemplateId = vm.getTemplateId();
        if (volTemplateId != null && volTemplateId.longValue() != vmTemplateId) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("switchVolume: Old Volume's templateId: " + volTemplateId + " does not match the VM's templateId: " + vmTemplateId +
                               ", updating templateId in the new Volume");
            }
            templateIdToUse = vmTemplateId;
        }

        final Long templateIdToUseFinal = templateIdToUse; 
        return Transaction.execute(new TransactionCallback<VolumeVO>() {
            @Override
            public VolumeVO doInTransaction(TransactionStatus status) {
                VolumeVO newVolume = allocateDuplicateVolumeVO(existingVolume, templateIdToUseFinal);
                // In case of Vmware if vm reference is not removed then during root
                // disk cleanup
                // the vm also gets deleted, so remove the reference
                if (vm.getHypervisorType() == HypervisorType.VMware) {
                    _volsDao.detachVolume(existingVolume.getId());
                }
                try {
                    stateTransitTo(existingVolume, Volume.Event.DestroyRequested);
                } catch (NoTransitionException e) {
                    s_logger.debug("Unable to destroy existing volume: " + e.toString());
                }
                
                return newVolume;
            }
        });
    }

    @Override
    public void release(VirtualMachineProfile profile) {
        // add code here
    }

    @Override
    @DB
    public void cleanupVolumes(long vmId) throws ConcurrentOperationException {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Cleaning storage for vm: " + vmId);
        }
        final List<VolumeVO> volumesForVm = _volsDao.findByInstance(vmId);
        final List<VolumeVO> toBeExpunged = new ArrayList<VolumeVO>();

        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                for (VolumeVO vol : volumesForVm) {
                    if (vol.getVolumeType().equals(Type.ROOT)) {
                        // Destroy volume if not already destroyed
                        boolean volumeAlreadyDestroyed = (vol.getState() == Volume.State.Destroy || vol.getState() == Volume.State.Expunged || vol.getState() == Volume.State.Expunging);
                        if (!volumeAlreadyDestroyed) {
                            volService.destroyVolume(vol.getId());
                        } else {
                            s_logger.debug("Skipping destroy for the volume " + vol + " as its in state " + vol.getState().toString());
                        }
                        toBeExpunged.add(vol);
                    } else {
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Detaching " + vol);
                        }
                        _volsDao.detachVolume(vol.getId());
                    }
                }
            }
        });

        AsyncCallFuture<VolumeApiResult> future = null;
        for (VolumeVO expunge : toBeExpunged) {
            future = volService.expungeVolumeAsync(volFactory.getVolume(expunge.getId()));
            try {
                future.get();
            } catch (InterruptedException e) {
                s_logger.debug("failed expunge volume" + expunge.getId(), e);
            } catch (ExecutionException e) {
                s_logger.debug("failed expunge volume" + expunge.getId(), e);
            }
        }
    }

    @Override
    @DB
    public Volume migrateVolume(Volume volume, StoragePool destPool) throws StorageUnavailableException {
        VolumeInfo vol = volFactory.getVolume(volume.getId());
        AsyncCallFuture<VolumeApiResult> future = volService.copyVolume(vol, (DataStore)destPool);
        try {
            VolumeApiResult result = future.get();
            if (result.isFailed()) {
                s_logger.error("migrate volume failed:" + result.getResult());
                throw new StorageUnavailableException("migrate volume failed: " + result.getResult(), destPool.getId());
            }
            return result.getVolume();
        } catch (InterruptedException e) {
            s_logger.debug("migrate volume failed", e);
            return null;
        } catch (ExecutionException e) {
            s_logger.debug("migrate volume failed", e);
            return null;
        }
    }

    @DB
    protected Volume liveMigrateVolume(Volume volume, StoragePool destPool) {
        VolumeInfo vol = volFactory.getVolume(volume.getId());
        AsyncCallFuture<VolumeApiResult> future = volService.migrateVolume(vol, (DataStore)destPool);
        try {
            VolumeApiResult result = future.get();
            if (result.isFailed()) {
                s_logger.debug("migrate volume failed:" + result.getResult());
                return null;
            }
            return result.getVolume();
        } catch (InterruptedException e) {
            s_logger.debug("migrate volume failed", e);
            return null;
        } catch (ExecutionException e) {
            s_logger.debug("migrate volume failed", e);
            return null;
        }
    }

    @Override
    public void migrateVolumes(VirtualMachine vm, VirtualMachineTO vmTo, Host srcHost, Host destHost, Map<Volume, StoragePool> volumeToPool) {
        // Check if all the vms being migrated belong to the vm.
        // Check if the storage pool is of the right type.
        // Create a VolumeInfo to DataStore map too.
        Map<VolumeInfo, DataStore> volumeMap = new HashMap<VolumeInfo, DataStore>();
        for (Map.Entry<Volume, StoragePool> entry : volumeToPool.entrySet()) {
            Volume volume = entry.getKey();
            StoragePool storagePool = entry.getValue();
            StoragePool destPool = (StoragePool)dataStoreMgr.getDataStore(storagePool.getId(), DataStoreRole.Primary);

            if (volume.getInstanceId() != vm.getId()) {
                throw new CloudRuntimeException("Volume " + volume + " that has to be migrated doesn't belong to the" + " instance " + vm);
            }

            if (destPool == null) {
                throw new CloudRuntimeException("Failed to find the destination storage pool " + storagePool.getId());
            }

            volumeMap.put(volFactory.getVolume(volume.getId()), (DataStore)destPool);
        }

        AsyncCallFuture<CommandResult> future = volService.migrateVolumes(volumeMap, vmTo, srcHost, destHost);
        try {
            CommandResult result = future.get();
            if (result.isFailed()) {
                s_logger.debug("Failed to migrated vm " + vm + " along with its volumes. " + result.getResult());
                throw new CloudRuntimeException("Failed to migrated vm " + vm + " along with its volumes. " + result.getResult());
            }
        } catch (InterruptedException e) {
            s_logger.debug("Failed to migrated vm " + vm + " along with its volumes.", e);
        } catch (ExecutionException e) {
            s_logger.debug("Failed to migrated vm " + vm + " along with its volumes.", e);
        }
    }

    @Override
    public boolean storageMigration(VirtualMachineProfile vm, StoragePool destPool) throws StorageUnavailableException {
        List<VolumeVO> vols = _volsDao.findUsableVolumesForInstance(vm.getId());
        List<Volume> volumesNeedToMigrate = new ArrayList<Volume>();

        for (VolumeVO volume : vols) {
            if (volume.getState() != Volume.State.Ready) {
                s_logger.debug("volume: " + volume.getId() + " is in " + volume.getState() + " state");
                throw new CloudRuntimeException("volume: " + volume.getId() + " is in " + volume.getState() + " state");
            }

            if (volume.getPoolId() == destPool.getId()) {
                s_logger.debug("volume: " + volume.getId() + " is on the same storage pool: " + destPool.getId());
                continue;
            }

            volumesNeedToMigrate.add(volume);
        }

        if (volumesNeedToMigrate.isEmpty()) {
            s_logger.debug("No volume need to be migrated");
            return true;
        }

        for (Volume vol : volumesNeedToMigrate) {
            Volume result = migrateVolume(vol, destPool);
            if (result == null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void prepareForMigration(VirtualMachineProfile vm, DeployDestination dest) {
        List<VolumeVO> vols = _volsDao.findUsableVolumesForInstance(vm.getId());
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Preparing " + vols.size() + " volumes for " + vm);
        }

        for (VolumeVO vol : vols) {
            DataTO volTO = volFactory.getVolume(vol.getId()).getTO();
            DiskTO disk = new DiskTO(volTO, vol.getDeviceId(), vol.getPath(), vol.getVolumeType());
            VolumeInfo volumeInfo = volFactory.getVolume(vol.getId());
            DataStore dataStore = dataStoreMgr.getDataStore(vol.getPoolId(), DataStoreRole.Primary);

            disk.setDetails(getDetails(volumeInfo, dataStore));

            vm.addDisk(disk);
        }

        if (vm.getType() == VirtualMachine.Type.User && vm.getTemplate().getFormat() == ImageFormat.ISO) {
            DataTO dataTO = tmplFactory.getTemplate(vm.getTemplate().getId(), DataStoreRole.Image, vm.getVirtualMachine().getDataCenterId()).getTO();
            DiskTO iso = new DiskTO(dataTO, 3L, null, Volume.Type.ISO);
            vm.addDisk(iso);
        }
    }

    private Map<String, String> getDetails(VolumeInfo volumeInfo, DataStore dataStore) {
        Map<String, String> details = new HashMap<String, String>();

        StoragePoolVO storagePool = _storagePoolDao.findById(dataStore.getId());

        details.put(DiskTO.MANAGED, String.valueOf(storagePool.isManaged()));
        details.put(DiskTO.STORAGE_HOST, storagePool.getHostAddress());
        details.put(DiskTO.STORAGE_PORT, String.valueOf(storagePool.getPort()));
        details.put(DiskTO.VOLUME_SIZE, String.valueOf(volumeInfo.getSize()));
        details.put(DiskTO.IQN, volumeInfo.get_iScsiName());

        ChapInfo chapInfo = volService.getChapInfo(volumeInfo, dataStore);

        if (chapInfo != null) {
            details.put(DiskTO.CHAP_INITIATOR_USERNAME, chapInfo.getInitiatorUsername());
            details.put(DiskTO.CHAP_INITIATOR_SECRET, chapInfo.getInitiatorSecret());
            details.put(DiskTO.CHAP_TARGET_USERNAME, chapInfo.getTargetUsername());
            details.put(DiskTO.CHAP_TARGET_SECRET, chapInfo.getTargetSecret());
        }

        return details;
    }

    private static enum VolumeTaskType {
        RECREATE, NOP, MIGRATE
    }

    private static class VolumeTask {
        final VolumeTaskType type;
        final StoragePoolVO pool;
        final VolumeVO volume;

        VolumeTask(VolumeTaskType type, VolumeVO volume, StoragePoolVO pool) {
            this.type = type;
            this.pool = pool;
            this.volume = volume;
        }
    }

    private List<VolumeTask> getTasks(List<VolumeVO> vols, Map<Volume, StoragePool> destVols) throws StorageUnavailableException {
        boolean recreate = RecreatableSystemVmEnabled.value();
        List<VolumeTask> tasks = new ArrayList<VolumeTask>();
        for (VolumeVO vol : vols) {
            StoragePoolVO assignedPool = null;
            if (destVols != null) {
                StoragePool pool = destVols.get(vol);
                if (pool != null) {
                    assignedPool = _storagePoolDao.findById(pool.getId());
                }
            }
            if (assignedPool == null && recreate) {
                assignedPool = _storagePoolDao.findById(vol.getPoolId());
            }
            if (assignedPool != null || recreate) {
                Volume.State state = vol.getState();
                if (state == Volume.State.Allocated || state == Volume.State.Creating) {
                    VolumeTask task = new VolumeTask(VolumeTaskType.RECREATE, vol, null);
                    tasks.add(task);
                } else {
                    if (vol.isRecreatable()) {
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Volume " + vol + " will be recreated on storage pool " + assignedPool + " assigned by deploymentPlanner");
                        }
                        VolumeTask task = new VolumeTask(VolumeTaskType.RECREATE, vol, null);
                        tasks.add(task);
                    } else {
                        if (assignedPool.getId() != vol.getPoolId()) {
                            if (s_logger.isDebugEnabled()) {
                                s_logger.debug("Mismatch in storage pool " + assignedPool + " assigned by deploymentPlanner and the one associated with volume " + vol);
                            }
                            DiskOffering diskOffering = _entityMgr.findById(DiskOffering.class, vol.getDiskOfferingId());
                            if (diskOffering.getUseLocalStorage()) {
                                // Currently migration of local volume is not supported so bail out
                                if (s_logger.isDebugEnabled()) {
                                    s_logger.debug("Local volume " + vol + " cannot be recreated on storagepool " + assignedPool + " assigned by deploymentPlanner");
                                }
                                throw new CloudRuntimeException("Local volume " + vol + " cannot be recreated on storagepool " + assignedPool + " assigned by deploymentPlanner");
                            } else {
                                //Check if storage migration is enabled in config
                                if (StorageHAMigrationEnabled.value()) {
                                    if (s_logger.isDebugEnabled()) {
                                        s_logger.debug("Shared volume " + vol + " will be migrated on storage pool " + assignedPool + " assigned by deploymentPlanner");
                                    }
                                    VolumeTask task = new VolumeTask(VolumeTaskType.MIGRATE, vol, assignedPool);
                                    tasks.add(task);
                                } else {
                                    throw new CloudRuntimeException("Cannot migrate volumes. Volume Migration is disabled");
                                }
                            }
                        } else {
                            StoragePoolVO pool = _storagePoolDao.findById(vol.getPoolId());
                            VolumeTask task = new VolumeTask(VolumeTaskType.NOP, vol, pool);
                            tasks.add(task);
                        }

                    }
                }
            } else {
                if (vol.getPoolId() == null) {
                    throw new StorageUnavailableException("Volume has no pool associate and also no storage pool assigned in DeployDestination, Unable to create " + vol,
                        Volume.class,
                        vol.getId());
                }
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("No need to recreate the volume: " + vol + ", since it already has a pool assigned: " + vol.getPoolId() + ", adding disk to VM");
                }
                StoragePoolVO pool = _storagePoolDao.findById(vol.getPoolId());
                VolumeTask task = new VolumeTask(VolumeTaskType.NOP, vol, pool);
                tasks.add(task);
            }
        }

        return tasks;
    }

    private Pair<VolumeVO, DataStore> recreateVolume(VolumeVO vol, VirtualMachineProfile vm, DeployDestination dest) throws StorageUnavailableException {
        VolumeVO newVol;
        boolean recreate = RecreatableSystemVmEnabled.value();
        DataStore destPool = null;
        if (recreate && (dest.getStorageForDisks() == null || dest.getStorageForDisks().get(vol) == null)) {
            destPool = dataStoreMgr.getDataStore(vol.getPoolId(), DataStoreRole.Primary);
            s_logger.debug("existing pool: " + destPool.getId());
        } else {
            StoragePool pool = dest.getStorageForDisks().get(vol);
            destPool = dataStoreMgr.getDataStore(pool.getId(), DataStoreRole.Primary);
        }

        if (vol.getState() == Volume.State.Allocated || vol.getState() == Volume.State.Creating) {
            newVol = vol;
        } else {
            newVol = switchVolume(vol, vm);
            // update the volume->PrimaryDataStoreVO map since volumeId has
            // changed
            if (dest.getStorageForDisks() != null && dest.getStorageForDisks().containsKey(vol)) {
                StoragePool poolWithOldVol = dest.getStorageForDisks().get(vol);
                dest.getStorageForDisks().put(newVol, poolWithOldVol);
                dest.getStorageForDisks().remove(vol);
            }
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Created new volume " + newVol + " for old volume " + vol);
            }
        }
        VolumeInfo volume = volFactory.getVolume(newVol.getId(), destPool);
        Long templateId = newVol.getTemplateId();
        for (int i = 0; i < 2; i++) {
            // retry one more time in case of template reload is required for Vmware case
            AsyncCallFuture<VolumeApiResult> future = null;
            if (templateId == null) {
                future = volService.createVolumeAsync(volume, destPool);
            } else {
                TemplateInfo templ = tmplFactory.getTemplate(templateId, DataStoreRole.Image);
                future = volService.createVolumeFromTemplateAsync(volume, destPool.getId(), templ);
            }
            VolumeApiResult result = null;
            try {
                result = future.get();
                if (result.isFailed()) {
                    if (result.getResult().contains("request template reload") && (i == 0)) {
                        s_logger.debug("Retry template re-deploy for vmware");
                        continue;
                    }
                    else {
                        s_logger.debug("Unable to create " + newVol + ":" + result.getResult());
                        throw new StorageUnavailableException("Unable to create " + newVol + ":" + result.getResult(), destPool.getId());
                    }
                }
                newVol = _volsDao.findById(newVol.getId());
                break; //break out of template-redeploy retry loop
            } catch (InterruptedException e) {
                s_logger.error("Unable to create " + newVol, e);
                throw new StorageUnavailableException("Unable to create " + newVol + ":" + e.toString(), destPool.getId());
            } catch (ExecutionException e) {
                s_logger.error("Unable to create " + newVol, e);
                throw new StorageUnavailableException("Unable to create " + newVol + ":" + e.toString(), destPool.getId());
            }
        }

        return new Pair<VolumeVO, DataStore>(newVol, destPool);
    }

    @Override
    public void prepare(VirtualMachineProfile vm, DeployDestination dest) throws StorageUnavailableException, InsufficientStorageCapacityException, ConcurrentOperationException {

        if (dest == null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("DeployDestination cannot be null, cannot prepare Volumes for the vm: " + vm);
            }
            throw new CloudRuntimeException("Unable to prepare Volume for vm because DeployDestination is null, vm:" + vm);
        }
        List<VolumeVO> vols = _volsDao.findUsableVolumesForInstance(vm.getId());
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Checking if we need to prepare " + vols.size() + " volumes for " + vm);
        }

        List<VolumeTask> tasks = getTasks(vols, dest.getStorageForDisks());
        Volume vol = null;
        StoragePool pool = null;
        for (VolumeTask task : tasks) {
            if (task.type == VolumeTaskType.NOP) {
                pool = (StoragePool)dataStoreMgr.getDataStore(task.pool.getId(), DataStoreRole.Primary);
                vol = task.volume;
            } else if (task.type == VolumeTaskType.MIGRATE) {
                pool = (StoragePool)dataStoreMgr.getDataStore(task.pool.getId(), DataStoreRole.Primary);
                vol = migrateVolume(task.volume, pool);
            } else if (task.type == VolumeTaskType.RECREATE) {
                Pair<VolumeVO, DataStore> result = recreateVolume(task.volume, vm, dest);
                pool = (StoragePool)dataStoreMgr.getDataStore(result.second().getId(), DataStoreRole.Primary);
                vol = result.first();
            }
            DataTO volumeTO = volFactory.getVolume(vol.getId()).getTO();
            DiskTO disk = new DiskTO(volumeTO, vol.getDeviceId(), vol.getPath(), vol.getVolumeType());
            VolumeInfo volumeInfo = volFactory.getVolume(vol.getId());
            DataStore dataStore = dataStoreMgr.getDataStore(vol.getPoolId(), DataStoreRole.Primary);

            disk.setDetails(getDetails(volumeInfo, dataStore));

            vm.addDisk(disk);
        }
    }

    private boolean stateTransitTo(Volume vol, Volume.Event event) throws NoTransitionException {
        return _volStateMachine.transitTo(vol, event, null, _volsDao);
    }

    @Override
    public boolean canVmRestartOnAnotherServer(long vmId) {
        List<VolumeVO> vols = _volsDao.findCreatedByInstance(vmId);
        for (VolumeVO vol : vols) {
            StoragePoolVO storagePoolVO = _storagePoolDao.findById(vol.getPoolId());
            if (!vol.isRecreatable() && storagePoolVO != null && storagePoolVO.getPoolType() != null && !(storagePoolVO.getPoolType().isShared())) {
                return false;
            }
        }
        return true;
    }
    
    public static final ConfigKey<Long> MaxVolumeSize = new ConfigKey<Long>(Long.class,
        "storage.max.volume.size",
        "Storage",
        "2000",
        "The maximum size for a volume (in GB).",
        true);

    public static final ConfigKey<Boolean> RecreatableSystemVmEnabled = new ConfigKey<Boolean>(Boolean.class,
        "recreate.systemvm.enabled",
        "Advanced",
        "false",
        "If true, will recreate system vm root disk whenever starting system vm",
        true);

    public static final ConfigKey<Boolean> StorageHAMigrationEnabled = new ConfigKey<Boolean>(Boolean.class,
            "enable.ha.storage.migration",
            "Storage",
            "true",
            "Enable/disable storage migration across primary storage during HA",
            true);

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {RecreatableSystemVmEnabled, MaxVolumeSize, StorageHAMigrationEnabled};
    }

    @Override
    public String getConfigComponentName() {
        return VolumeOrchestrationService.class.getSimpleName();
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        return true;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public String getName() {
        return "Volume Manager";
    }

    @Override
    public void destroyVolume(Volume volume) {
        try {
            // Mark volume as removed if volume has not been created on primary
            if (volume.getState() == Volume.State.Allocated) {
                _volsDao.remove(volume.getId());
                stateTransitTo(volume, Volume.Event.DestroyRequested);
            } else {
                volService.destroyVolume(volume.getId());
            }
        } catch (Exception e) {
            s_logger.debug("Failed to destroy volume" + volume.getId(), e);
            throw new CloudRuntimeException("Failed to destroy volume" + volume.getId(), e);
        }
    }

    @Override
    public String getVmNameFromVolumeId(long volumeId) {
        VolumeVO volume = _volsDao.findById(volumeId);
        return getVmNameOnVolume(volume);
    }

    @Override
    public String getStoragePoolOfVolume(long volumeId) {
        VolumeVO vol = _volsDao.findById(volumeId);
        return dataStoreMgr.getPrimaryDataStore(vol.getPoolId()).getUuid();
    }
    
    @Override
    public void updateVolumeDiskChain(long volumeId, String path, String chainInfo) {
        VolumeVO vol = _volsDao.findById(volumeId);
        boolean needUpdate = false;
        if(!vol.getPath().equalsIgnoreCase(path))
        	needUpdate = true;
        
        if(chainInfo != null && (vol.getChainInfo() == null || !chainInfo.equalsIgnoreCase(vol.getChainInfo())))
        	needUpdate = true;
        
        if(needUpdate) {
        	s_logger.info("Update volume disk chain info. vol: " + vol.getId() + ", " + vol.getPath() + " -> " + path
        		+ ", " + vol.getChainInfo() + " -> " + chainInfo);
	        vol.setPath(path);
	        vol.setChainInfo(chainInfo);
	        _volsDao.update(volumeId, vol);
        }
    }
}
