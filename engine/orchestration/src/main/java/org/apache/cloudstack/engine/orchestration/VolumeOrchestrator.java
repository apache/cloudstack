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

import static com.cloud.utils.NumbersUtil.toHumanReadableSize;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import com.cloud.agent.api.to.DatadiskTO;
import com.cloud.utils.StringUtils;
import com.cloud.vm.SecondaryStorageVmVO;
import com.cloud.vm.UserVmDetailVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.SecondaryStorageVmDao;
import com.cloud.vm.dao.UserVmDetailsDao;
import org.apache.cloudstack.api.command.admin.vm.MigrateVMCmd;
import org.apache.cloudstack.api.command.admin.volume.MigrateVolumeCmdByAdmin;
import org.apache.cloudstack.api.command.user.volume.MigrateVolumeCmd;
import org.apache.cloudstack.engine.orchestration.service.VolumeOrchestrationService;
import org.apache.cloudstack.engine.subsystem.api.storage.ChapInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreCapabilities;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotService;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotStrategy;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotStrategy.SnapshotOperation;
import org.apache.cloudstack.engine.subsystem.api.storage.StoragePoolAllocator;
import org.apache.cloudstack.engine.subsystem.api.storage.StorageStrategyFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateService;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService.VolumeApiResult;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.framework.config.ConfigDepot;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.framework.jobs.impl.AsyncJobVO;
import org.apache.cloudstack.resourcedetail.DiskOfferingDetailVO;
import org.apache.cloudstack.resourcedetail.dao.DiskOfferingDetailsDao;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;

import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.manager.allocator.PodAllocator;
import com.cloud.capacity.CapacityManager;
import com.cloud.cluster.ClusterManager;
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
import com.cloud.exception.StorageAccessException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.org.Cluster;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Snapshot;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.Volume;
import com.cloud.storage.Volume.Type;
import com.cloud.storage.VolumeApiService;
import com.cloud.storage.VolumeDetailVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VMTemplateDetailsDao;
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
import com.cloud.vm.UserVmCloneSettingVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.VirtualMachineProfileImpl;
import com.cloud.vm.VmWorkAttachVolume;
import com.cloud.vm.VmWorkMigrateVolume;
import com.cloud.vm.VmWorkSerializer;
import com.cloud.vm.VmWorkTakeVolumeSnapshot;
import com.cloud.vm.dao.UserVmCloneSettingDao;
import com.cloud.vm.dao.UserVmDao;

import static com.cloud.storage.resource.StorageProcessor.REQUEST_TEMPLATE_RELOAD;

public class VolumeOrchestrator extends ManagerBase implements VolumeOrchestrationService, Configurable {

    public enum UserVmCloneType {
        full, linked
    }

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
    protected SnapshotDao _snapshotDao;
    @Inject
    protected SnapshotDataStoreDao _snapshotDataStoreDao;
    @Inject
    protected ResourceLimitService _resourceLimitMgr;
    @Inject
    DiskOfferingDetailsDao _diskOfferingDetailDao;
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
    @Inject
    HostDao _hostDao;
    @Inject
    SnapshotService _snapshotSrv;
    @Inject
    protected UserVmDao _userVmDao;
    @Inject
    protected AsyncJobManager _jobMgr;
    @Inject
    ClusterManager clusterManager;
    @Inject
    StorageManager storageMgr;
    @Inject
    protected UserVmCloneSettingDao _vmCloneSettingDao;
    @Inject
    StorageStrategyFactory _storageStrategyFactory;
    @Inject
    VMTemplateDetailsDao templateDetailsDao;
    @Inject
    TemplateService templateService;
    @Inject
    UserVmDetailsDao userVmDetailsDao;
    @Inject
    private SecondaryStorageVmDao secondaryStorageVmDao;
    @Inject
    VolumeApiService _volumeApiService;

    private final StateMachine2<Volume.State, Volume.Event, Volume> _volStateMachine;
    protected List<StoragePoolAllocator> _storagePoolAllocators;

    public List<StoragePoolAllocator> getStoragePoolAllocators() {
        return _storagePoolAllocators;
    }

    public void setStoragePoolAllocators(List<StoragePoolAllocator> storagePoolAllocators) {
        _storagePoolAllocators = storagePoolAllocators;
    }

    protected List<PodAllocator> _podAllocators;

    public List<PodAllocator> getPodAllocators() {
        return _podAllocators;
    }

    public void setPodAllocators(List<PodAllocator> podAllocators) {
        _podAllocators = podAllocators;
    }

    protected VolumeOrchestrator() {
        _volStateMachine = Volume.State.getStateMachine();
    }

    @Override
    public VolumeInfo moveVolume(VolumeInfo volume, long destPoolDcId, Long destPoolPodId, Long destPoolClusterId, HypervisorType dataDiskHyperType)
            throws ConcurrentOperationException, StorageUnavailableException {

        // Find a destination storage pool with the specified criteria
        DiskOffering diskOffering = _entityMgr.findById(DiskOffering.class, volume.getDiskOfferingId());
        DiskProfile dskCh = new DiskProfile(volume.getId(), volume.getVolumeType(), volume.getName(), diskOffering.getId(), diskOffering.getDiskSize(), diskOffering.getTagsArray(),
                diskOffering.isUseLocalStorage(), diskOffering.isRecreatable(), null);
        dskCh.setHyperType(dataDiskHyperType);
        storageMgr.setDiskProfileThrottling(dskCh, null, diskOffering);

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
        VolumeVO newVol = new VolumeVO(oldVol.getVolumeType(), oldVol.getName(), oldVol.getDataCenterId(), oldVol.getDomainId(), oldVol.getAccountId(), oldVol.getDiskOfferingId(),
                oldVol.getProvisioningType(), oldVol.getSize(), oldVol.getMinIops(), oldVol.getMaxIops(), oldVol.get_iScsiName());
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

    @Override
    public StoragePool findChildDataStoreInDataStoreCluster(DataCenter dc, Pod pod, Long clusterId, Long hostId, VirtualMachine vm, Long datastoreClusterId) {
        Long podId = null;
        if (pod != null) {
            podId = pod.getId();
        } else if (clusterId != null) {
            Cluster cluster = _entityMgr.findById(Cluster.class, clusterId);
            if (cluster != null) {
                podId = cluster.getPodId();
            }
        }
        List<StoragePoolVO> childDatastores = _storagePoolDao.listChildStoragePoolsInDatastoreCluster(datastoreClusterId);
        List<StoragePool> suitablePools = new ArrayList<StoragePool>();

        for (StoragePoolVO childDatastore: childDatastores)
            suitablePools.add((StoragePool)dataStoreMgr.getDataStore(childDatastore.getId(), DataStoreRole.Primary));

        VirtualMachineProfile profile = new VirtualMachineProfileImpl(vm);
        for (StoragePoolAllocator allocator : _storagePoolAllocators) {
            DataCenterDeployment plan = new DataCenterDeployment(dc.getId(), podId, clusterId, hostId, null, null);
            final List<StoragePool> poolList = allocator.reorderPools(suitablePools, profile, plan);

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
    public VolumeInfo createVolumeFromSnapshot(Volume volume, Snapshot snapshot, UserVm vm) throws StorageUnavailableException {
        Account account = _entityMgr.findById(Account.class, volume.getAccountId());

        final HashSet<StoragePool> poolsToAvoid = new HashSet<StoragePool>();
        StoragePool pool = null;

        Set<Long> podsToAvoid = new HashSet<Long>();
        Pair<Pod, Long> pod = null;

        DiskOffering diskOffering = _entityMgr.findById(DiskOffering.class, volume.getDiskOfferingId());
        DataCenter dc = _entityMgr.findById(DataCenter.class, volume.getDataCenterId());
        DiskProfile dskCh = new DiskProfile(volume, diskOffering, snapshot.getHypervisorType());

        String msg = "There are no available storage pools to store the volume in";

        if (vm != null) {
            Pod podofVM = _entityMgr.findById(Pod.class, vm.getPodIdToDeployIn());
            if (podofVM != null) {
                pod = new Pair<Pod, Long>(podofVM, podofVM.getId());
            }
        }

        if (vm != null && pod != null) {
            //if VM is running use the hostId to find the clusterID. If it is stopped, refer the cluster where the ROOT volume of the VM exists.
            Long hostId = null;
            Long clusterId = null;
            if (vm.getState() == State.Running) {
                hostId = vm.getHostId();
                if (hostId != null) {
                    Host vmHost = _entityMgr.findById(Host.class, hostId);
                    clusterId = vmHost.getClusterId();
                }
            } else {
                List<VolumeVO> rootVolumesOfVm = _volsDao.findByInstanceAndType(vm.getId(), Volume.Type.ROOT);
                if (rootVolumesOfVm.size() != 1) {
                    throw new CloudRuntimeException("The VM " + vm.getHostName() + " has more than one ROOT volume and is in an invalid state. Please contact Cloud Support.");
                } else {
                    VolumeVO rootVolumeOfVm = rootVolumesOfVm.get(0);
                    StoragePoolVO rootDiskPool = _storagePoolDao.findById(rootVolumeOfVm.getPoolId());
                    clusterId = (rootDiskPool == null ? null : rootDiskPool.getClusterId());
                }
            }
            // Determine what storage pool to store the volume in
            while ((pool = findStoragePool(dskCh, dc, pod.first(), clusterId, hostId, vm, poolsToAvoid)) != null) {
                break;
            }

            if (pool == null) {
                //pool could not be found in the VM's pod/cluster.
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Could not find any storage pool to create Volume in the pod/cluster of the provided VM " + vm.getUuid());
                }
                StringBuilder addDetails = new StringBuilder(msg);
                addDetails.append(", Could not find any storage pool to create Volume in the pod/cluster of the VM ");
                addDetails.append(vm.getUuid());
                msg = addDetails.toString();
            }
        } else {
            // Determine what pod to store the volume in
            while ((pod = findPod(null, null, dc, account.getId(), podsToAvoid)) != null) {
                podsToAvoid.add(pod.first().getId());
                // Determine what storage pool to store the volume in
                while ((pool = findStoragePool(dskCh, dc, pod.first(), null, null, null, poolsToAvoid)) != null) {
                    break;
                }

                if (pool != null) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Found a suitable pool for create volume: " + pool.getId());
                    }
                    break;
                }
            }
        }

        if (pool == null) {
            s_logger.info(msg);
            throw new StorageUnavailableException(msg, -1);
        }

        VolumeInfo vol = volFactory.getVolume(volume.getId());
        DataStore store = dataStoreMgr.getDataStore(pool.getId(), DataStoreRole.Primary);
        DataStoreRole dataStoreRole = getDataStoreRole(snapshot);
        SnapshotInfo snapInfo = snapshotFactory.getSnapshot(snapshot.getId(), dataStoreRole);

        if (snapInfo == null && dataStoreRole == DataStoreRole.Image) {
            // snapshot is not backed up to secondary, let's do that now.
            snapInfo = snapshotFactory.getSnapshot(snapshot.getId(), DataStoreRole.Primary);

            if (snapInfo == null) {
                throw new CloudRuntimeException("Cannot find snapshot " + snapshot.getId());
            }
            // We need to copy the snapshot onto secondary.
            SnapshotStrategy snapshotStrategy = _storageStrategyFactory.getSnapshotStrategy(snapshot, SnapshotOperation.BACKUP);
            snapshotStrategy.backupSnapshot(snapInfo);

            // Attempt to grab it again.
            snapInfo = snapshotFactory.getSnapshot(snapshot.getId(), dataStoreRole);
            if (snapInfo == null) {
                throw new CloudRuntimeException("Cannot find snapshot " + snapshot.getId() + " on secondary and could not create backup");
            }
        }
        // don't try to perform a sync if the DataStoreRole of the snapshot is equal to DataStoreRole.Primary
        if (!DataStoreRole.Primary.equals(dataStoreRole)) {
            try {
                // sync snapshot to region store if necessary
                DataStore snapStore = snapInfo.getDataStore();
                long snapVolId = snapInfo.getVolumeId();

                _snapshotSrv.syncVolumeSnapshotsToRegionStore(snapVolId, snapStore);
            } catch (Exception ex) {
                // log but ignore the sync error to avoid any potential S3 down issue, it should be sync next time
                s_logger.warn(ex.getMessage(), ex);
            }
        }

        // create volume on primary from snapshot
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

    public DataStoreRole getDataStoreRole(Snapshot snapshot) {
        SnapshotDataStoreVO snapshotStore = _snapshotDataStoreDao.findBySnapshot(snapshot.getId(), DataStoreRole.Primary);

        if (snapshotStore == null) {
            return DataStoreRole.Image;
        }

        long storagePoolId = snapshotStore.getDataStoreId();
        DataStore dataStore = dataStoreMgr.getDataStore(storagePoolId, DataStoreRole.Primary);

        Map<String, String> mapCapabilities = dataStore.getDriver().getCapabilities();

        if (mapCapabilities != null) {
            String value = mapCapabilities.get(DataStoreCapabilities.STORAGE_SYSTEM_SNAPSHOT.toString());
            Boolean supportsStorageSystemSnapshots = new Boolean(value);

            if (supportsStorageSystemSnapshots) {
                return DataStoreRole.Primary;
            }
        }

        return DataStoreRole.Image;
    }

    protected DiskProfile createDiskCharacteristics(VolumeInfo volume, VirtualMachineTemplate template, DataCenter dc, DiskOffering diskOffering) {
        if (volume.getVolumeType() == Type.ROOT && Storage.ImageFormat.ISO != template.getFormat()) {
            TemplateDataStoreVO ss = _vmTemplateStoreDao.findByTemplateZoneDownloadStatus(template.getId(), dc.getId(), VMTemplateStorageResourceAssoc.Status.DOWNLOADED);
            if (ss == null) {
                throw new CloudRuntimeException("Template " + template.getName() + " has not been completely downloaded to zone " + dc.getId());
            }

            return new DiskProfile(volume.getId(), volume.getVolumeType(), volume.getName(), diskOffering.getId(), ss.getSize(), diskOffering.getTagsArray(), diskOffering.isUseLocalStorage(),
                    diskOffering.isRecreatable(), Storage.ImageFormat.ISO != template.getFormat() ? template.getId() : null);
        } else {
            return new DiskProfile(volume.getId(), volume.getVolumeType(), volume.getName(), diskOffering.getId(), diskOffering.getDiskSize(), diskOffering.getTagsArray(),
                    diskOffering.isUseLocalStorage(), diskOffering.isRecreatable(), null);
        }
    }

    @DB
    public VolumeInfo copyVolumeFromSecToPrimary(VolumeInfo volume, VirtualMachine vm, VirtualMachineTemplate template, DataCenter dc, Pod pod, Long clusterId, ServiceOffering offering,
                                                 DiskOffering diskOffering, List<StoragePool> avoids, long size, HypervisorType hyperType) throws NoTransitionException {

        final HashSet<StoragePool> avoidPools = new HashSet<StoragePool>(avoids);
        DiskProfile dskCh = createDiskCharacteristics(volume, template, dc, diskOffering);
        dskCh.setHyperType(vm.getHypervisorType());
        storageMgr.setDiskProfileThrottling(dskCh, null, diskOffering);

        // Find a suitable storage to create volume on
        StoragePool destPool = findStoragePool(dskCh, dc, pod, clusterId, null, vm, avoidPools);
        if (destPool == null) {
            throw new CloudRuntimeException("Failed to find a suitable storage pool to create Volume in the pod/cluster of the provided VM "+ vm.getUuid());
        }
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
    public VolumeInfo createVolume(VolumeInfo volume, VirtualMachine vm, VirtualMachineTemplate template, DataCenter dc, Pod pod, Long clusterId, ServiceOffering offering, DiskOffering diskOffering,
                                   List<StoragePool> avoids, long size, HypervisorType hyperType) {
        // update the volume's hv_ss_reserve (hypervisor snapshot reserve) from a disk offering (used for managed storage)
        volume = volService.updateHypervisorSnapshotReserveForVolume(diskOffering, volume.getId(), hyperType);

        StoragePool pool = null;

        DiskProfile dskCh = null;
        if (volume.getVolumeType() == Type.ROOT && Storage.ImageFormat.ISO != template.getFormat()) {
            dskCh = createDiskCharacteristics(volume, template, dc, diskOffering);
            storageMgr.setDiskProfileThrottling(dskCh, offering, diskOffering);
        } else {
            dskCh = createDiskCharacteristics(volume, template, dc, diskOffering);
            storageMgr.setDiskProfileThrottling(dskCh, null, diskOffering);
        }

        if (diskOffering != null && diskOffering.isCustomized()) {
            dskCh.setSize(size);
        }

        dskCh.setHyperType(hyperType);

        final HashSet<StoragePool> avoidPools = new HashSet<StoragePool>(avoids);

        pool = findStoragePool(dskCh, dc, pod, clusterId, vm.getHostId(), vm, avoidPools);
        if (pool == null) {
            s_logger.warn("Unable to find suitable primary storage when creating volume " + volume.getName());
            throw new CloudRuntimeException("Unable to find suitable primary storage when creating volume " + volume.getName());
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
                    if (result.getResult().contains(REQUEST_TEMPLATE_RELOAD) && (i == 0)) {
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

        if (vmId == null) {
            return true;
        }

        UserVm vm = _entityMgr.findById(UserVm.class, vmId);

        if (vm == null) {
            return true;
        }

        State state = vm.getState();

        if (state.equals(State.Stopped) || state.equals(State.Destroyed)) {
            return true;
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
            throw new InvalidParameterValueException("Please specify a size of at least 1 GB.");
        } else if (size > (MaxVolumeSize.value() * 1024 * 1024 * 1024)) {
            throw new InvalidParameterValueException("volume size " + size + ", but the maximum size allowed is " + MaxVolumeSize.value() + " GB.");
        }

        return true;
    }

    protected DiskProfile toDiskProfile(Volume vol, DiskOffering offering) {
        return new DiskProfile(vol.getId(), vol.getVolumeType(), vol.getName(), offering.getId(), vol.getSize(), offering.getTagsArray(), offering.isUseLocalStorage(), offering.isRecreatable(),
                vol.getTemplateId());
    }

    @Override
    public DiskProfile allocateRawVolume(Type type, String name, DiskOffering offering, Long size, Long minIops, Long maxIops, VirtualMachine vm, VirtualMachineTemplate template, Account owner,
                                         Long deviceId) {
        if (size == null) {
            size = offering.getDiskSize();
        } else {
            size = (size * 1024 * 1024 * 1024);
        }

        minIops = minIops != null ? minIops : offering.getMinIops();
        maxIops = maxIops != null ? maxIops : offering.getMaxIops();

        VolumeVO vol = new VolumeVO(type, name, vm.getDataCenterId(), owner.getDomainId(), owner.getId(), offering.getId(), offering.getProvisioningType(), size, minIops, maxIops, null);
        if (vm != null) {
            vol.setInstanceId(vm.getId());
        }

        if (deviceId != null) {
            vol.setDeviceId(deviceId);
        } else if (type.equals(Type.ROOT)) {
            vol.setDeviceId(0l);
        } else {
            vol.setDeviceId(1l);
        }
        if (template.getFormat() == ImageFormat.ISO) {
            vol.setIsoId(template.getId());
        } else if (template.getTemplateType().equals(Storage.TemplateType.DATADISK)) {
            vol.setTemplateId(template.getId());
        }
        // display flag matters only for the User vms
        if (vm.getType() == VirtualMachine.Type.User) {
            UserVmVO userVm = _userVmDao.findById(vm.getId());
            vol.setDisplayVolume(userVm.isDisplayVm());
        }

        vol.setFormat(getSupportedImageFormatForCluster(vm.getHypervisorType()));
        vol = _volsDao.persist(vol);

        List<VolumeDetailVO> volumeDetailsVO = new ArrayList<VolumeDetailVO>();
        DiskOfferingDetailVO bandwidthLimitDetail = _diskOfferingDetailDao.findDetail(offering.getId(), Volume.BANDWIDTH_LIMIT_IN_MBPS);
        if (bandwidthLimitDetail != null) {
            volumeDetailsVO.add(new VolumeDetailVO(vol.getId(), Volume.BANDWIDTH_LIMIT_IN_MBPS, bandwidthLimitDetail.getValue(), false));
        }
        DiskOfferingDetailVO iopsLimitDetail = _diskOfferingDetailDao.findDetail(offering.getId(), Volume.IOPS_LIMIT);
        if (iopsLimitDetail != null) {
            volumeDetailsVO.add(new VolumeDetailVO(vol.getId(), Volume.IOPS_LIMIT, iopsLimitDetail.getValue(), false));
        }
        if (!volumeDetailsVO.isEmpty()) {
            _volDetailDao.saveDetails(volumeDetailsVO);
        }

        // Save usage event and update resource count for user vm volumes
        if (vm.getType() == VirtualMachine.Type.User) {
            UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VOLUME_CREATE, vol.getAccountId(), vol.getDataCenterId(), vol.getId(), vol.getName(), offering.getId(), null, size,
                    Volume.class.getName(), vol.getUuid(), vol.isDisplayVolume());

            _resourceLimitMgr.incrementResourceCount(vm.getAccountId(), ResourceType.volume, vol.isDisplayVolume());
            _resourceLimitMgr.incrementResourceCount(vm.getAccountId(), ResourceType.primary_storage, vol.isDisplayVolume(), new Long(vol.getSize()));
        }
        return toDiskProfile(vol, offering);
    }

    private DiskProfile allocateTemplatedVolume(Type type, String name, DiskOffering offering, Long rootDisksize, Long minIops, Long maxIops, VirtualMachineTemplate template, VirtualMachine vm,
                                                Account owner, long deviceId, String configurationId) {
        assert (template.getFormat() != ImageFormat.ISO) : "ISO is not a template really....";

        Long size = _tmpltMgr.getTemplateSize(template.getId(), vm.getDataCenterId());
        if (rootDisksize != null) {
            if (template.isDeployAsIs()) {
                // Volume size specified from template deploy-as-is
                size = rootDisksize;
            } else {
                rootDisksize = rootDisksize * 1024 * 1024 * 1024;
                if (rootDisksize > size) {
                    s_logger.debug("Using root disk size of " + toHumanReadableSize(rootDisksize) + " Bytes for volume " + name);
                    size = rootDisksize;
                } else {
                    s_logger.debug("Using root disk size of " + toHumanReadableSize(rootDisksize) + " Bytes for volume " + name + "since specified root disk size of " + rootDisksize + " Bytes is smaller than template");
                }
            }
        }

        minIops = minIops != null ? minIops : offering.getMinIops();
        maxIops = maxIops != null ? maxIops : offering.getMaxIops();

        VolumeVO vol = new VolumeVO(type, name, vm.getDataCenterId(), owner.getDomainId(), owner.getId(), offering.getId(), offering.getProvisioningType(), size, minIops, maxIops, null);
        vol.setFormat(getSupportedImageFormatForCluster(template.getHypervisorType()));
        if (vm != null) {
            vol.setInstanceId(vm.getId());
        }
        vol.setTemplateId(template.getId());

        vol.setDeviceId(deviceId);
        if (type.equals(Type.ROOT) && !vm.getType().equals(VirtualMachine.Type.User)) {
            vol.setRecreatable(true);
        }

        if (vm.getType() == VirtualMachine.Type.User) {
            UserVmVO userVm = _userVmDao.findById(vm.getId());
            vol.setDisplayVolume(userVm.isDisplayVm());
        }

        vol = _volsDao.persist(vol);

        List<VolumeDetailVO> volumeDetailsVO = new ArrayList<VolumeDetailVO>();
        DiskOfferingDetailVO bandwidthLimitDetail = _diskOfferingDetailDao.findDetail(offering.getId(), Volume.BANDWIDTH_LIMIT_IN_MBPS);
        if (bandwidthLimitDetail != null) {
            volumeDetailsVO.add(new VolumeDetailVO(vol.getId(), Volume.BANDWIDTH_LIMIT_IN_MBPS, bandwidthLimitDetail.getValue(), false));
        }
        DiskOfferingDetailVO iopsLimitDetail = _diskOfferingDetailDao.findDetail(offering.getId(), Volume.IOPS_LIMIT);
        if (iopsLimitDetail != null) {
            volumeDetailsVO.add(new VolumeDetailVO(vol.getId(), Volume.IOPS_LIMIT, iopsLimitDetail.getValue(), false));
        }
        if (!volumeDetailsVO.isEmpty()) {
            _volDetailDao.saveDetails(volumeDetailsVO);
        }

        if (StringUtils.isNotBlank(configurationId)) {
            VolumeDetailVO deployConfigurationDetail = new VolumeDetailVO(vol.getId(), VmDetailConstants.DEPLOY_AS_IS_CONFIGURATION, configurationId, false);
            _volDetailDao.persist(deployConfigurationDetail);
        }

        // Create event and update resource count for volumes if vm is a user vm
        if (vm.getType() == VirtualMachine.Type.User) {

            Long offeringId = null;

            if (!offering.isComputeOnly()) {
                offeringId = offering.getId();
            }

            UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VOLUME_CREATE, vol.getAccountId(), vol.getDataCenterId(), vol.getId(), vol.getName(), offeringId, vol.getTemplateId(), size,
                    Volume.class.getName(), vol.getUuid(), vol.isDisplayVolume());

            _resourceLimitMgr.incrementResourceCount(vm.getAccountId(), ResourceType.volume, vol.isDisplayVolume());
            _resourceLimitMgr.incrementResourceCount(vm.getAccountId(), ResourceType.primary_storage, vol.isDisplayVolume(), new Long(vol.getSize()));
        }
        return toDiskProfile(vol, offering);
    }

    @Override
    public List<DiskProfile> allocateTemplatedVolumes(Type type, String name, DiskOffering offering, Long rootDisksize, Long minIops, Long maxIops, VirtualMachineTemplate template, VirtualMachine vm,
                                                      Account owner) {
        int volumesNumber = 1;
        List<DatadiskTO> templateAsIsDisks = null;
        String configurationId = null;
        boolean deployVmAsIs = false;
        if (template.isDeployAsIs() && vm.getType() != VirtualMachine.Type.SecondaryStorageVm) {
            List<SecondaryStorageVmVO> runningSSVMs = secondaryStorageVmDao.getSecStorageVmListInStates(null, vm.getDataCenterId(), State.Running);
            if (CollectionUtils.isEmpty(runningSSVMs)) {
                s_logger.info("Could not find a running SSVM in datacenter " + vm.getDataCenterId() + " for deploying VM as is, " +
                        "not deploying VM " + vm.getInstanceName() + " as-is");
            } else {
                UserVmDetailVO configurationDetail = userVmDetailsDao.findDetail(vm.getId(), VmDetailConstants.DEPLOY_AS_IS_CONFIGURATION);
                if (configurationDetail != null) {
                    configurationId = configurationDetail.getValue();
                }
                templateAsIsDisks = _tmpltMgr.getTemplateDisksOnImageStore(template.getId(), DataStoreRole.Image, configurationId);
                if (CollectionUtils.isNotEmpty(templateAsIsDisks)) {
                    templateAsIsDisks = templateAsIsDisks.stream()
                            .filter(x -> !x.isIso())
                            .sorted(Comparator.comparing(DatadiskTO::getDiskNumber))
                            .collect(Collectors.toList());
                }
                volumesNumber = templateAsIsDisks.size();
                deployVmAsIs = true;
            }
        }

        if (volumesNumber < 1) {
            throw new CloudRuntimeException("Unable to create any volume from template " + template.getName());
        }

        List<DiskProfile> profiles = new ArrayList<>();

        for (int number = 0; number < volumesNumber; number++) {
            String volumeName = name;
            Long volumeSize = rootDisksize;
            long deviceId = type.equals(Type.ROOT) ? 0L : 1L;
            if (deployVmAsIs) {
                int volumeNameSuffix = templateAsIsDisks.get(number).getDiskNumber();
                volumeName = String.format("%s-%d", volumeName, volumeNameSuffix);
                volumeSize = templateAsIsDisks.get(number).getVirtualSize();
                deviceId = templateAsIsDisks.get(number).getDiskNumber();
            }
            s_logger.info(String.format("adding disk object %s to %s", volumeName, vm.getInstanceName()));
            DiskProfile diskProfile = allocateTemplatedVolume(type, volumeName, offering, volumeSize, minIops, maxIops,
                    template, vm, owner, deviceId, configurationId);
            profiles.add(diskProfile);
        }

        handleRootDiskControllerTpeForDeployAsIs(templateAsIsDisks, vm);
        return profiles;
    }

    private void handleRootDiskControllerTpeForDeployAsIs(List<DatadiskTO> disksAsIs, VirtualMachine vm) {
        if (CollectionUtils.isNotEmpty(disksAsIs)) {
            String diskControllerSubType = disksAsIs.get(0).getDiskControllerSubType();
            if (StringUtils.isNotBlank(diskControllerSubType)) {
                long vmId = vm.getId();
                UserVmDetailVO detail = userVmDetailsDao.findDetail(vmId, VmDetailConstants.ROOT_DISK_CONTROLLER);
                if (detail != null) {
                    detail.setValue(diskControllerSubType);
                    userVmDetailsDao.update(detail.getId(), detail);
                } else {
                    detail = new UserVmDetailVO(vmId, VmDetailConstants.ROOT_DISK_CONTROLLER, diskControllerSubType, false);
                    userVmDetailsDao.persist(detail);
                }
            }
        }
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
        } else if (hyperType == HypervisorType.Hyperv) {
            return ImageFormat.VHDX;
        } else {
            return null;
        }
    }

    private boolean isSupportedImageFormatForCluster(VolumeInfo volume, HypervisorType rootDiskHyperType) {
        ImageFormat volumeFormat = volume.getFormat();
        if (rootDiskHyperType == HypervisorType.Hyperv) {
            if (volumeFormat.equals(ImageFormat.VHDX) || volumeFormat.equals(ImageFormat.VHD)) {
                return true;
            } else {
                return false;
            }
        } else {
            return volume.getFormat().equals(getSupportedImageFormatForCluster(rootDiskHyperType));
        }
    }

    private VolumeInfo copyVolume(StoragePool rootDiskPool, VolumeInfo volume, VirtualMachine vm, VirtualMachineTemplate rootDiskTmplt, DataCenter dcVO, Pod pod, DiskOffering diskVO,
                                  ServiceOffering svo, HypervisorType rootDiskHyperType) throws NoTransitionException {

        if (!isSupportedImageFormatForCluster(volume, rootDiskHyperType)) {
            throw new InvalidParameterValueException("Failed to attach volume to VM since volumes format " + volume.getFormat().getFileExtension() + " is not compatible with the vm hypervisor type");
        }

        VolumeInfo volumeOnPrimary = copyVolumeFromSecToPrimary(volume, vm, rootDiskTmplt, dcVO, pod, rootDiskPool.getClusterId(), svo, diskVO, new ArrayList<StoragePool>(), volume.getSize(),
                rootDiskHyperType);

        return volumeOnPrimary;
    }

    @Override
    public VolumeInfo createVolumeOnPrimaryStorage(VirtualMachine vm, VolumeInfo volume, HypervisorType rootDiskHyperType, StoragePool storagePool) throws NoTransitionException {
        VirtualMachineTemplate rootDiskTmplt = _entityMgr.findById(VirtualMachineTemplate.class, vm.getTemplateId());
        DataCenter dcVO = _entityMgr.findById(DataCenter.class, vm.getDataCenterId());
        Pod pod = _entityMgr.findById(Pod.class, storagePool.getPodId());

        ServiceOffering svo = _entityMgr.findById(ServiceOffering.class, vm.getServiceOfferingId());
        DiskOffering diskVO = _entityMgr.findById(DiskOffering.class, volume.getDiskOfferingId());
        Long clusterId = storagePool.getClusterId();

        VolumeInfo vol = null;
        if (volume.getState() == Volume.State.Allocated) {
            vol = createVolume(volume, vm, rootDiskTmplt, dcVO, pod, clusterId, svo, diskVO, new ArrayList<StoragePool>(), volume.getSize(), rootDiskHyperType);
        } else if (volume.getState() == Volume.State.Uploaded) {
            vol = copyVolume(storagePool, volume, vm, rootDiskTmplt, dcVO, pod, diskVO, svo, rootDiskHyperType);
            if (vol != null) {
                // Moving of Volume is successful, decrement the volume resource count from secondary for an account and increment it into primary storage under same account.
                _resourceLimitMgr.decrementResourceCount(volume.getAccountId(), ResourceType.secondary_storage, volume.getSize());
                _resourceLimitMgr.incrementResourceCount(volume.getAccountId(), ResourceType.primary_storage, volume.getSize());
            }
        }

        if (vol == null) {
            throw new CloudRuntimeException("Volume shouldn't be null " + volume.getId());
        }
        VolumeVO volVO = _volsDao.findById(vol.getId());
        if (volVO.getFormat() == null) {
            volVO.setFormat(getSupportedImageFormatForCluster(rootDiskHyperType));
        }
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
                s_logger.debug("switchVolume: Old Volume's templateId: " + volTemplateId + " does not match the VM's templateId: " + vmTemplateId + ", updating templateId in the new Volume");
            }
            templateIdToUse = vmTemplateId;
        }

        final Long templateIdToUseFinal = templateIdToUse;
        return Transaction.execute(new TransactionCallback<VolumeVO>() {
            @Override
            public VolumeVO doInTransaction(TransactionStatus status) {
                VolumeVO newVolume = allocateDuplicateVolumeVO(existingVolume, templateIdToUseFinal);
                try {
                    stateTransitTo(existingVolume, Volume.Event.DestroyRequested);
                } catch (NoTransitionException e) {
                    s_logger.debug("Unable to destroy existing volume: " + e.toString());
                }
                // In case of VMware VM will continue to use the old root disk until expunged, so force expunge old root disk
                if (vm.getHypervisorType() == HypervisorType.VMware) {
                    s_logger.info("Expunging volume " + existingVolume.getId() + " from primary data store");
                    AsyncCallFuture<VolumeApiResult> future = volService.expungeVolumeAsync(volFactory.getVolume(existingVolume.getId()));
                    try {
                        future.get();
                    } catch (Exception e) {
                        s_logger.debug("Failed to expunge volume:" + existingVolume.getId(), e);
                    }
                }

                return newVolume;
            }
        });
    }

    @Override
    public void release(VirtualMachineProfile vmProfile) {
        Long hostId = vmProfile.getVirtualMachine().getHostId();
        if (hostId != null) {
            revokeAccess(vmProfile.getId(), hostId);
        }
    }

    @Override
    public void release(long vmId, long hostId) {
        List<VolumeVO> volumesForVm = _volsDao.findUsableVolumesForInstance(vmId);
        if (volumesForVm == null || volumesForVm.isEmpty()) {
            return;
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Releasing " + volumesForVm.size() + " volumes for VM: " + vmId + " from host: " + hostId);
        }

        for (VolumeVO volumeForVm : volumesForVm) {
            VolumeInfo volumeInfo = volFactory.getVolume(volumeForVm.getId());

            // pool id can be null for the VM's volumes in Allocated state
            if (volumeForVm.getPoolId() != null) {
                DataStore dataStore = dataStoreMgr.getDataStore(volumeForVm.getPoolId(), DataStoreRole.Primary);
                PrimaryDataStore primaryDataStore = (PrimaryDataStore)dataStore;
                HostVO host = _hostDao.findById(hostId);

                // This might impact other managed storages, grant access for PowerFlex storage pool only
                if (primaryDataStore.isManaged() && primaryDataStore.getPoolType() == Storage.StoragePoolType.PowerFlex) {
                    volService.revokeAccess(volumeInfo, host, dataStore);
                }
            }
        }
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
                        VMInstanceVO vm = _userVmDao.findById(vmId);
                        if (vm.getHypervisorType().equals(HypervisorType.VMware)) {
                            _volumeApiService.detachVolumeViaDestroyVM(vmId, vol.getId());
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
    public void revokeAccess(DataObject dataObject, Host host, DataStore dataStore) {
        DataStoreDriver dataStoreDriver = dataStore != null ? dataStore.getDriver() : null;

        if (dataStoreDriver instanceof PrimaryDataStoreDriver) {
            ((PrimaryDataStoreDriver)dataStoreDriver).revokeAccess(dataObject, host, dataStore);
        }
    }

    @Override
    public void revokeAccess(long vmId, long hostId) {
        HostVO host = _hostDao.findById(hostId);

        List<VolumeVO> volumesForVm = _volsDao.findByInstance(vmId);

        if (volumesForVm != null) {
            for (VolumeVO volumeForVm : volumesForVm) {
                VolumeInfo volumeInfo = volFactory.getVolume(volumeForVm.getId());

                // pool id can be null for the VM's volumes in Allocated state
                if (volumeForVm.getPoolId() != null) {
                    DataStore dataStore = dataStoreMgr.getDataStore(volumeForVm.getPoolId(), DataStoreRole.Primary);

                    volService.revokeAccess(volumeInfo, host, dataStore);
                }
            }
        }
    }

    private void checkConcurrentJobsPerDatastoreThreshhold(final StoragePool destPool) {
        final Long threshold = VolumeApiService.ConcurrentMigrationsThresholdPerDatastore.value();
        if (threshold != null && threshold > 0) {
            long count = _jobMgr.countPendingJobs("\"storageid\":\"" + destPool.getUuid() + "\"", MigrateVMCmd.class.getName(), MigrateVolumeCmd.class.getName(), MigrateVolumeCmdByAdmin.class.getName());
            if (count > threshold) {
                throw new CloudRuntimeException("Number of concurrent migration jobs per datastore exceeded the threshold: " + threshold.toString() + ". Please try again after some time.");
            }
        }
    }


    @Override
    @DB
    public Volume migrateVolume(Volume volume, StoragePool destPool) throws StorageUnavailableException {
        VolumeInfo vol = volFactory.getVolume(volume.getId());
        if (vol == null){
            throw new CloudRuntimeException("Migrate volume failed because volume object of volume " + volume.getName()+ "is null");
        }
        if (destPool == null) {
            throw new CloudRuntimeException("Migrate volume failed because destination storage pool is not available!!");
        }

        checkConcurrentJobsPerDatastoreThreshhold(destPool);

        DataStore dataStoreTarget = dataStoreMgr.getDataStore(destPool.getId(), DataStoreRole.Primary);
        AsyncCallFuture<VolumeApiResult> future = volService.copyVolume(vol, dataStoreTarget);
        try {
            VolumeApiResult result = future.get();
            if (result.isFailed()) {
                s_logger.error("Migrate volume failed:" + result.getResult());
                if (result.getResult() != null && result.getResult().contains("[UNSUPPORTED]")) {
                    throw new CloudRuntimeException("Migrate volume failed: " + result.getResult());
                }
                throw new StorageUnavailableException("Migrate volume failed: " + result.getResult(), destPool.getId());
            } else {
                // update the volumeId for snapshots on secondary
                if (!_snapshotDao.listByVolumeId(vol.getId()).isEmpty()) {
                    _snapshotDao.updateVolumeIds(vol.getId(), result.getVolume().getId());
                    _snapshotDataStoreDao.updateVolumeIds(vol.getId(), result.getVolume().getId());
                }
            }
            return result.getVolume();
        } catch (InterruptedException e) {
            s_logger.debug("migrate volume failed", e);
            throw new CloudRuntimeException(e.getMessage());
        } catch (ExecutionException e) {
            s_logger.debug("migrate volume failed", e);
            throw new CloudRuntimeException(e.getMessage());
        }
    }

    @Override
    @DB
    public Volume liveMigrateVolume(Volume volume, StoragePool destPool) {
        VolumeInfo vol = volFactory.getVolume(volume.getId());
        DataStore dataStoreTarget = dataStoreMgr.getDataStore(destPool.getId(), DataStoreRole.Primary);
        AsyncCallFuture<VolumeApiResult> future = volService.migrateVolume(vol, dataStoreTarget);
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
                throw new CloudRuntimeException("Failed to migrated vm " + vm + " along with its volumes. ");
            }
        } catch (InterruptedException e) {
            s_logger.debug("Failed to migrated vm " + vm + " along with its volumes.", e);
        } catch (ExecutionException e) {
            s_logger.debug("Failed to migrated vm " + vm + " along with its volumes.", e);
        }
    }

    @Override
    public boolean storageMigration(VirtualMachineProfile vm, Map<Volume, StoragePool> volumeToPool) throws StorageUnavailableException {
        Map<Volume, StoragePool> volumeStoragePoolMap = new HashMap<>();
        for (Map.Entry<Volume, StoragePool> entry : volumeToPool.entrySet()) {
            Volume volume = entry.getKey();
            StoragePool pool = entry.getValue();
            if (volume.getState() != Volume.State.Ready) {
                s_logger.debug("volume: " + volume.getId() + " is in " + volume.getState() + " state");
                throw new CloudRuntimeException("volume: " + volume.getId() + " is in " + volume.getState() + " state");
            }

            if (volume.getPoolId() == pool.getId()) {
                s_logger.debug("volume: " + volume.getId() + " is on the same storage pool: " + pool.getId());
                continue;
            }
            volumeStoragePoolMap.put(volume, volumeToPool.get(volume));
        }

        if (MapUtils.isEmpty(volumeStoragePoolMap)) {
            s_logger.debug("No volume need to be migrated");
            return true;
        }
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Offline vm migration was not done up the stack in VirtualMachineManager so trying here.");
        }
        for (Map.Entry<Volume, StoragePool> entry : volumeStoragePoolMap.entrySet()) {
            Volume result = migrateVolume(entry.getKey(), entry.getValue());
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
            VolumeInfo volumeInfo = volFactory.getVolume(vol.getId());
            DataTO volTO = volumeInfo.getTO();
            DiskTO disk = storageMgr.getDiskWithThrottling(volTO, vol.getVolumeType(), vol.getDeviceId(), vol.getPath(), vm.getServiceOfferingId(), vol.getDiskOfferingId());
            DataStore dataStore = dataStoreMgr.getDataStore(vol.getPoolId(), DataStoreRole.Primary);

            disk.setDetails(getDetails(volumeInfo, dataStore));

            PrimaryDataStore primaryDataStore = (PrimaryDataStore)dataStore;
            // This might impact other managed storages, grant access for PowerFlex storage pool only
            if (primaryDataStore.isManaged() && primaryDataStore.getPoolType() == Storage.StoragePoolType.PowerFlex) {
                volService.grantAccess(volFactory.getVolume(vol.getId()), dest.getHost(), dataStore);
            }

            vm.addDisk(disk);
        }

        //if (vm.getType() == VirtualMachine.Type.User && vm.getTemplate().getFormat() == ImageFormat.ISO) {
        if (vm.getType() == VirtualMachine.Type.User) {
            _tmpltMgr.prepareIsoForVmProfile(vm, dest);
            //DataTO dataTO = tmplFactory.getTemplate(vm.getTemplate().getId(), DataStoreRole.Image, vm.getVirtualMachine().getDataCenterId()).getTO();
            //DiskTO iso = new DiskTO(dataTO, 3L, null, Volume.Type.ISO);
            //vm.addDisk(iso);
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
        details.put(DiskTO.MOUNT_POINT, volumeInfo.get_iScsiName());

        VolumeVO volume = _volumeDao.findById(volumeInfo.getId());
        details.put(DiskTO.PROTOCOL_TYPE, (volume.getPoolType() != null) ? volume.getPoolType().toString() : null);
        details.put(StorageManager.STORAGE_POOL_DISK_WAIT.toString(), String.valueOf(StorageManager.STORAGE_POOL_DISK_WAIT.valueIn(storagePool.getId())));

         if (volume.getPoolId() != null) {
            StoragePoolVO poolVO = _storagePoolDao.findById(volume.getPoolId());
            if (poolVO.getParent() != 0L) {
                details.put(DiskTO.PROTOCOL_TYPE, Storage.StoragePoolType.DatastoreCluster.toString());
            }
        }

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

    private List<VolumeTask> getTasks(List<VolumeVO> vols, Map<Volume, StoragePool> destVols, VirtualMachineProfile vm) throws StorageUnavailableException {
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
            if (assignedPool != null) {
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
                            if (diskOffering.isUseLocalStorage()) {
                                // Currently migration of local volume is not supported so bail out
                                if (s_logger.isDebugEnabled()) {
                                    s_logger.debug("Local volume " + vol + " cannot be recreated on storagepool " + assignedPool + " assigned by deploymentPlanner");
                                }
                                throw new CloudRuntimeException("Local volume " + vol + " cannot be recreated on storagepool " + assignedPool + " assigned by deploymentPlanner");
                            } else {
                                //Check if storage migration is enabled in config
                                Boolean isHAOperation = (Boolean)vm.getParameter(VirtualMachineProfile.Param.HaOperation);
                                Boolean storageMigrationEnabled = true;
                                if (isHAOperation != null && isHAOperation) {
                                    storageMigrationEnabled = StorageHAMigrationEnabled.value();
                                } else {
                                    storageMigrationEnabled = StorageMigrationEnabled.value();
                                }
                                if (storageMigrationEnabled) {
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
                    throw new StorageUnavailableException("Volume has no pool associate and also no storage pool assigned in DeployDestination, Unable to create " + vol, Volume.class, vol.getId());
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

    private Pair<VolumeVO, DataStore> recreateVolume(VolumeVO vol, VirtualMachineProfile vm, DeployDestination dest) throws StorageUnavailableException, StorageAccessException {
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
            // retry one more time in case of template reload is required for VMware case
            AsyncCallFuture<VolumeApiResult> future;

            if (templateId == null) {
                DiskOffering diskOffering = _entityMgr.findById(DiskOffering.class, volume.getDiskOfferingId());
                HypervisorType hyperType = vm.getVirtualMachine().getHypervisorType();

                // update the volume's hv_ss_reserve (hypervisor snapshot reserve) from a disk offering (used for managed storage)
                volService.updateHypervisorSnapshotReserveForVolume(diskOffering, volume.getId(), hyperType);

                volume = volFactory.getVolume(newVol.getId(), destPool);

                future = volService.createVolumeAsync(volume, destPool);
            } else {
                TemplateInfo templ = tmplFactory.getReadyTemplateOnImageStore(templateId, dest.getDataCenter().getId());
                PrimaryDataStore primaryDataStore = (PrimaryDataStore)destPool;

                if (templ == null) {
                    if (tmplFactory.isTemplateMarkedForDirectDownload(templateId)) {
                        // Template is marked for direct download bypassing Secondary Storage
                        if (!primaryDataStore.isManaged()) {
                            templ = tmplFactory.getReadyBypassedTemplateOnPrimaryStore(templateId, destPool.getId(), dest.getHost().getId());
                        } else {
                            s_logger.debug("Direct download template: " + templateId + " on host: " + dest.getHost().getId() + " and copy to the managed storage pool: " + destPool.getId());
                            templ = volService.createManagedStorageTemplate(templateId, destPool.getId(), dest.getHost().getId());
                        }

                        if (templ == null) {
                            s_logger.debug("Failed to spool direct download template: " + templateId + " for data center " + dest.getDataCenter().getId());
                            throw new CloudRuntimeException("Failed to spool direct download template: " + templateId + " for data center " + dest.getDataCenter().getId());
                        }
                    } else {
                        s_logger.debug("can't find ready template: " + templateId + " for data center " + dest.getDataCenter().getId());
                        throw new CloudRuntimeException("can't find ready template: " + templateId + " for data center " + dest.getDataCenter().getId());
                    }
                }

                if (primaryDataStore.isManaged()) {
                    DiskOffering diskOffering = _entityMgr.findById(DiskOffering.class, volume.getDiskOfferingId());
                    HypervisorType hyperType = vm.getVirtualMachine().getHypervisorType();

                    // update the volume's hv_ss_reserve (hypervisor snapshot reserve) from a disk offering (used for managed storage)
                    volService.updateHypervisorSnapshotReserveForVolume(diskOffering, volume.getId(), hyperType);

                    long hostId = vm.getVirtualMachine().getHostId();

                    future = volService.createManagedStorageVolumeFromTemplateAsync(volume, destPool.getId(), templ, hostId);
                } else {
                    future = volService.createVolumeFromTemplateAsync(volume, destPool.getId(), templ);
                }
            }
            VolumeApiResult result;
            try {
                result = future.get();
                if (result.isFailed()) {
                    if (result.getResult().contains(REQUEST_TEMPLATE_RELOAD) && (i == 0)) {
                        s_logger.debug("Retry template re-deploy for vmware");
                        continue;
                    } else {
                        s_logger.debug("Unable to create " + newVol + ":" + result.getResult());
                        throw new StorageUnavailableException("Unable to create " + newVol + ":" + result.getResult(), destPool.getId());
                    }
                }

                StoragePoolVO storagePool = _storagePoolDao.findById(destPool.getId());

                if (storagePool.isManaged()) {
                    long hostId = vm.getVirtualMachine().getHostId();
                    Host host = _hostDao.findById(hostId);

                    try {
                        volService.grantAccess(volFactory.getVolume(newVol.getId()), host, destPool);
                    } catch (Exception e) {
                        throw new StorageAccessException("Unable to grant access to volume: " + newVol.getId() + " on host: " + host.getId());
                    }
                }

                newVol = _volsDao.findById(newVol.getId());
                break; //break out of template-redeploy retry loop
            } catch (StorageAccessException e) {
                throw e;
            } catch (InterruptedException | ExecutionException e) {
                s_logger.error("Unable to create " + newVol, e);
                throw new StorageUnavailableException("Unable to create " + newVol + ":" + e.toString(), destPool.getId());
            }
        }

        return new Pair<VolumeVO, DataStore>(newVol, destPool);
    }

    @Override
    public void prepare(VirtualMachineProfile vm, DeployDestination dest) throws StorageUnavailableException, InsufficientStorageCapacityException, ConcurrentOperationException, StorageAccessException {

        if (dest == null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("DeployDestination cannot be null, cannot prepare Volumes for the vm: " + vm);
            }
            throw new CloudRuntimeException("Unable to prepare Volume for vm because DeployDestination is null, vm:" + vm);
        }

        // don't allow to start vm that doesn't have a root volume
        if (_volsDao.findByInstanceAndType(vm.getId(), Volume.Type.ROOT).isEmpty()) {
            throw new CloudRuntimeException("Unable to prepare volumes for vm as ROOT volume is missing");
        }

        List<VolumeVO> vols = _volsDao.findUsableVolumesForInstance(vm.getId());

        List<VolumeTask> tasks = getTasks(vols, dest.getStorageForDisks(), vm);
        Volume vol = null;
        StoragePool pool;
        for (VolumeTask task : tasks) {
            if (task.type == VolumeTaskType.NOP) {
                vol = task.volume;

                pool = (StoragePool)dataStoreMgr.getDataStore(task.pool.getId(), DataStoreRole.Primary);

                // For zone-wide managed storage, it is possible that the VM can be started in another
                // cluster. In that case, make sure that the volume is in the right access group.
                if (pool.isManaged()) {
                    Host lastHost = _hostDao.findById(vm.getVirtualMachine().getLastHostId());
                    Host host = _hostDao.findById(vm.getVirtualMachine().getHostId());

                    long lastClusterId = lastHost == null || lastHost.getClusterId() == null ? -1 : lastHost.getClusterId();
                    long clusterId = host == null || host.getClusterId() == null ? -1 : host.getClusterId();

                    if (lastClusterId != clusterId) {
                        if (lastHost != null) {
                            storageMgr.removeStoragePoolFromCluster(lastHost.getId(), vol.get_iScsiName(), pool);

                            DataStore storagePool = dataStoreMgr.getDataStore(pool.getId(), DataStoreRole.Primary);

                            volService.revokeAccess(volFactory.getVolume(vol.getId()), lastHost, storagePool);
                        }

                        try {
                            volService.grantAccess(volFactory.getVolume(vol.getId()), host, (DataStore)pool);
                        } catch (Exception e) {
                            throw new StorageAccessException("Unable to grant access to volume: " + vol.getId() + " on host: " + host.getId());
                        }
                    } else {
                        // This might impact other managed storages, grant access for PowerFlex storage pool only
                        if (pool.getPoolType() == Storage.StoragePoolType.PowerFlex) {
                            try {
                                volService.grantAccess(volFactory.getVolume(vol.getId()), host, (DataStore)pool);
                            } catch (Exception e) {
                                throw new StorageAccessException("Unable to grant access to volume: " + vol.getId() + " on host: " + host.getId());
                            }
                        }
                    }
                }
            } else if (task.type == VolumeTaskType.MIGRATE) {
                pool = (StoragePool)dataStoreMgr.getDataStore(task.pool.getId(), DataStoreRole.Primary);
                vol = migrateVolume(task.volume, pool);
            } else if (task.type == VolumeTaskType.RECREATE) {
                Pair<VolumeVO, DataStore> result = recreateVolume(task.volume, vm, dest);
                pool = (StoragePool)dataStoreMgr.getDataStore(result.second().getId(), DataStoreRole.Primary);
                vol = result.first();
            }

            VolumeInfo volumeInfo = volFactory.getVolume(vol.getId());
            DataTO volTO = volumeInfo.getTO();
            DiskTO disk = storageMgr.getDiskWithThrottling(volTO, vol.getVolumeType(), vol.getDeviceId(), vol.getPath(), vm.getServiceOfferingId(), vol.getDiskOfferingId());
            DataStore dataStore = dataStoreMgr.getDataStore(vol.getPoolId(), DataStoreRole.Primary);

            disk.setDetails(getDetails(volumeInfo, dataStore));

            vm.addDisk(disk);

            // If hypervisor is vSphere, check for clone type setting.
            if (vm.getHypervisorType().equals(HypervisorType.VMware)) {
                // retrieve clone flag.
                UserVmCloneType cloneType = UserVmCloneType.linked;
                Boolean value = CapacityManager.VmwareCreateCloneFull.valueIn(vol.getPoolId());
                if (value != null && value) {
                    cloneType = UserVmCloneType.full;
                }
                UserVmCloneSettingVO cloneSettingVO = _vmCloneSettingDao.findByVmId(vm.getId());
                if (cloneSettingVO != null) {
                    if (!cloneSettingVO.getCloneType().equals(cloneType.toString())) {
                        cloneSettingVO.setCloneType(cloneType.toString());
                        _vmCloneSettingDao.update(cloneSettingVO.getId(), cloneSettingVO);
                    }
                } else {
                    UserVmCloneSettingVO vmCloneSettingVO = new UserVmCloneSettingVO(vm.getId(), cloneType.toString());
                    _vmCloneSettingDao.persist(vmCloneSettingVO);
                }
            }

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

    public static final ConfigKey<Long> MaxVolumeSize = new ConfigKey<Long>(Long.class, "storage.max.volume.size", "Storage", "2000", "The maximum size for a volume (in GB).", true);

    public static final ConfigKey<Boolean> RecreatableSystemVmEnabled = new ConfigKey<Boolean>(Boolean.class, "recreate.systemvm.enabled", "Advanced", "false",
            "If true, will recreate system vm root disk whenever starting system vm", true);

    public static final ConfigKey<Boolean> StorageHAMigrationEnabled = new ConfigKey<Boolean>(Boolean.class, "enable.ha.storage.migration", "Storage", "true",
            "Enable/disable storage migration across primary storage during HA", true);

    public static final ConfigKey<Boolean> StorageMigrationEnabled = new ConfigKey<Boolean>(Boolean.class, "enable.storage.migration", "Storage", "true",
            "Enable/disable storage migration across primary storage", true);

    static final ConfigKey<Boolean> VolumeUrlCheck = new ConfigKey<Boolean>("Advanced", Boolean.class, "volume.url.check", "true",
            "Check the url for a volume before downloading it from the management server. Set to flase when you managment has no internet access.", true);

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {RecreatableSystemVmEnabled, MaxVolumeSize, StorageHAMigrationEnabled, StorageMigrationEnabled, CustomDiskOfferingMaxSize, CustomDiskOfferingMinSize, VolumeUrlCheck};
    }

    @Override
    public String getConfigComponentName() {
        return VolumeOrchestrationService.class.getSimpleName();
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        return true;
    }

    private void cleanupVolumeDuringAttachFailure(Long volumeId, Long vmId) {
        VolumeVO volume = _volsDao.findById(volumeId);
        if (volume == null) {
            return;
        }

        if (volume.getState().equals(Volume.State.Creating)) {
            s_logger.debug("Remove volume: " + volume.getId() + ", as it's leftover from last mgt server stop");
            _volsDao.remove(volume.getId());
        }

        if (volume.getState().equals(Volume.State.Attaching)) {
            s_logger.warn("Vol: " + volume.getName() + " failed to attach to VM: " + _userVmDao.findById(vmId).getHostName() + " on last mgt server stop, changing state back to Ready");
            volume.setState(Volume.State.Ready);
            _volsDao.update(volumeId, volume);
        }
    }

    private void cleanupVolumeDuringMigrationFailure(Long volumeId, Long destPoolId) {
        StoragePool destPool = (StoragePool)dataStoreMgr.getDataStore(destPoolId, DataStoreRole.Primary);
        if (destPool == null) {
            return;
        }

        VolumeVO volume = _volsDao.findById(volumeId);
        if (volume.getState() == Volume.State.Migrating) {
            VolumeVO duplicateVol = _volsDao.findByPoolIdName(destPoolId, volume.getName());
            if (duplicateVol != null) {
                s_logger.debug("Remove volume " + duplicateVol.getId() + " on storage pool " + destPoolId);
                _volsDao.remove(duplicateVol.getId());
            }

            s_logger.debug("change volume state to ready from migrating in case migration failure for vol: " + volumeId);
            volume.setState(Volume.State.Ready);
            _volsDao.update(volumeId, volume);
        }

    }

    private void cleanupVolumeDuringSnapshotFailure(Long volumeId, Long snapshotId) {
        _snapshotSrv.cleanupVolumeDuringSnapshotFailure(volumeId, snapshotId);
        VolumeVO volume = _volsDao.findById(volumeId);
        if (volume.getState() == Volume.State.Snapshotting) {
            s_logger.debug("change volume state back to Ready: " + volume.getId());
            volume.setState(Volume.State.Ready);
            _volsDao.update(volume.getId(), volume);
        }
    }

    @Override
    public void cleanupStorageJobs() {
        //clean up failure jobs related to volume
        List<AsyncJobVO> jobs = _jobMgr.findFailureAsyncJobs(VmWorkAttachVolume.class.getName(), VmWorkMigrateVolume.class.getName(), VmWorkTakeVolumeSnapshot.class.getName());

        for (AsyncJobVO job : jobs) {
            try {
                if (job.getCmd().equalsIgnoreCase(VmWorkAttachVolume.class.getName())) {
                    VmWorkAttachVolume work = VmWorkSerializer.deserialize(VmWorkAttachVolume.class, job.getCmdInfo());
                    cleanupVolumeDuringAttachFailure(work.getVolumeId(), work.getVmId());
                } else if (job.getCmd().equalsIgnoreCase(VmWorkMigrateVolume.class.getName())) {
                    VmWorkMigrateVolume work = VmWorkSerializer.deserialize(VmWorkMigrateVolume.class, job.getCmdInfo());
                    cleanupVolumeDuringMigrationFailure(work.getVolumeId(), work.getDestPoolId());
                } else if (job.getCmd().equalsIgnoreCase(VmWorkTakeVolumeSnapshot.class.getName())) {
                    VmWorkTakeVolumeSnapshot work = VmWorkSerializer.deserialize(VmWorkTakeVolumeSnapshot.class, job.getCmdInfo());
                    cleanupVolumeDuringSnapshotFailure(work.getVolumeId(), work.getSnapshotId());
                }
            } catch (Exception e) {
                s_logger.debug("clean up job failure, will continue", e);
            }
        }
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
                _resourceLimitMgr.decrementResourceCount(volume.getAccountId(), ResourceType.volume, volume.isDisplay());
                _resourceLimitMgr.decrementResourceCount(volume.getAccountId(), ResourceType.primary_storage, volume.isDisplay(), new Long(volume.getSize()));
            } else {
                volService.destroyVolume(volume.getId());
            }
            // FIXME - All this is boiler plate code and should be done as part of state transition. This shouldn't be part of orchestrator.
            // publish usage event for the volume
            UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VOLUME_DELETE, volume.getAccountId(), volume.getDataCenterId(), volume.getId(), volume.getName(), Volume.class.getName(),
                    volume.getUuid(), volume.isDisplayVolume());
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
    public void updateVolumeDiskChain(long volumeId, String path, String chainInfo, String updatedDataStoreUUID) {
        VolumeVO vol = _volsDao.findById(volumeId);
        boolean needUpdate = false;
        // Volume path is not getting updated in the DB, need to find reason and fix the issue.
        if (vol.getPath() == null) {
            return;
        }
        if (!vol.getPath().equalsIgnoreCase(path)) {
            needUpdate = true;
        }

        if (chainInfo != null && (vol.getChainInfo() == null || !chainInfo.equalsIgnoreCase(vol.getChainInfo()))) {
            needUpdate = true;
        }

        if (updatedDataStoreUUID != null) {
            needUpdate = true;
        }

        if (needUpdate) {
            s_logger.info("Update volume disk chain info. vol: " + vol.getId() + ", " + vol.getPath() + " -> " + path + ", " + vol.getChainInfo() + " -> " + chainInfo);
            vol.setPath(path);
            vol.setChainInfo(chainInfo);
            if (updatedDataStoreUUID != null) {
                StoragePoolVO pool = _storagePoolDao.findByUuid(updatedDataStoreUUID);
                if (pool != null) {
                    vol.setPoolId(pool.getId());
                }
            }
            _volsDao.update(volumeId, vol);
        }
    }

    @Override
    public DiskProfile importVolume(Type type, String name, DiskOffering offering, Long size, Long minIops, Long maxIops,
                                    VirtualMachine vm, VirtualMachineTemplate template, Account owner,
                                    Long deviceId, Long poolId, String path, String chainInfo) {
        if (size == null) {
            size = offering.getDiskSize();
        } else {
            size = (size * 1024 * 1024 * 1024);
        }

        minIops = minIops != null ? minIops : offering.getMinIops();
        maxIops = maxIops != null ? maxIops : offering.getMaxIops();

        VolumeVO vol = new VolumeVO(type, name, vm.getDataCenterId(), owner.getDomainId(), owner.getId(), offering.getId(), offering.getProvisioningType(), size, minIops, maxIops, null);
        if (vm != null) {
            vol.setInstanceId(vm.getId());
        }

        if (deviceId != null) {
            vol.setDeviceId(deviceId);
        } else if (type.equals(Type.ROOT)) {
            vol.setDeviceId(0l);
        } else {
            vol.setDeviceId(1l);
        }

        if (template != null) {
            if (ImageFormat.ISO.equals(template.getFormat())) {
                vol.setIsoId(template.getId());
            } else if (Storage.TemplateType.DATADISK.equals(template.getTemplateType())) {
                vol.setTemplateId(template.getId());
            }
            if (type == Type.ROOT) {
                vol.setTemplateId(template.getId());
            }
        }

        // display flag matters only for the User vms
        if (VirtualMachine.Type.User.equals(vm.getType())) {
            UserVmVO userVm = _userVmDao.findById(vm.getId());
            vol.setDisplayVolume(userVm.isDisplayVm());
        }

        vol.setFormat(getSupportedImageFormatForCluster(vm.getHypervisorType()));
        vol.setPoolId(poolId);
        vol.setPath(path);
        vol.setChainInfo(chainInfo);
        vol.setState(Volume.State.Ready);
        vol = _volsDao.persist(vol);
        return toDiskProfile(vol, offering);
    }

    @Override
    public void unmanageVolumes(long vmId) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Unmanaging storage for vm: " + vmId);
        }
        final List<VolumeVO> volumesForVm = _volsDao.findByInstance(vmId);

        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                for (VolumeVO vol : volumesForVm) {
                    boolean volumeAlreadyDestroyed = (vol.getState() == Volume.State.Destroy || vol.getState() == Volume.State.Expunged
                            || vol.getState() == Volume.State.Expunging);
                    if (volumeAlreadyDestroyed) {
                        s_logger.debug("Skipping destroy for the volume " + vol + " as its in state " + vol.getState().toString());
                    } else {
                        volService.unmanageVolume(vol.getId());
                    }
                }
            }
        });
    }
}
