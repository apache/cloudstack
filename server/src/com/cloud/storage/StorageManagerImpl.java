/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.storage;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.BackupSnapshotCommand;
import com.cloud.agent.api.CleanupSnapshotBackupCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.CreateStoragePoolCommand;
import com.cloud.agent.api.CreateVolumeFromSnapshotAnswer;
import com.cloud.agent.api.CreateVolumeFromSnapshotCommand;
import com.cloud.agent.api.DeleteStoragePoolCommand;
import com.cloud.agent.api.ManageSnapshotCommand;
import com.cloud.agent.api.ModifyStoragePoolAnswer;
import com.cloud.agent.api.ModifyStoragePoolCommand;
import com.cloud.agent.api.UpgradeSnapshotCommand;
import com.cloud.agent.api.storage.CopyVolumeAnswer;
import com.cloud.agent.api.storage.CopyVolumeCommand;
import com.cloud.agent.api.storage.CreateAnswer;
import com.cloud.agent.api.storage.CreateCommand;
import com.cloud.agent.api.storage.DeleteTemplateCommand;
import com.cloud.agent.api.storage.DestroyCommand;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.agent.manager.Commands;
import com.cloud.alert.AlertManager;
import com.cloud.api.ApiDBUtils;
import com.cloud.api.commands.CancelPrimaryStorageMaintenanceCmd;
import com.cloud.api.commands.CreateStoragePoolCmd;
import com.cloud.api.commands.CreateVolumeCmd;
import com.cloud.api.commands.DeletePoolCmd;
import com.cloud.api.commands.ListVolumesCmd;
import com.cloud.api.commands.UpdateStoragePoolCmd;
import com.cloud.async.AsyncJobManager;
import com.cloud.capacity.Capacity;
import com.cloud.capacity.CapacityVO;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.cluster.CheckPointManager;
import com.cloud.cluster.ClusterManagerListener;
import com.cloud.cluster.ManagementServerHostVO;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.consoleproxy.ConsoleProxyManager;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.Pod;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.domain.Domain;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventVO;
import com.cloud.event.dao.EventDao;
import com.cloud.event.dao.UsageEventDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.DiscoveryException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientStorageCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceInUseException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.HypervisorGuruManager;
import com.cloud.network.NetworkManager;
import com.cloud.network.router.VirtualNetworkApplianceManager;
import com.cloud.org.Grouping;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceState;
import com.cloud.server.ManagementServer;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.Volume.Type;
import com.cloud.storage.allocator.StoragePoolAllocator;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.SnapshotPolicyDao;
import com.cloud.storage.dao.StoragePoolDao;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.StoragePoolWorkDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateHostDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.dao.VMTemplateSwiftDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.listener.StoragePoolMonitor;
import com.cloud.storage.secondary.SecondaryStorageVmManager;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.storage.snapshot.SnapshotScheduler;
import com.cloud.template.TemplateManager;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.User;
import com.cloud.user.UserContext;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.UriUtils;
import com.cloud.utils.component.Adapters;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.utils.component.Manager;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.JoinBuilder.JoinType;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.exception.ExecutionException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.vm.ConsoleProxyVO;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.SecondaryStorageVmVO;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.VirtualMachineProfileImpl;
import com.cloud.vm.dao.ConsoleProxyDao;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.SecondaryStorageVmDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

@Local(value = { StorageManager.class, StorageService.class })
public class StorageManagerImpl implements StorageManager, Manager, ClusterManagerListener {
    private static final Logger s_logger = Logger.getLogger(StorageManagerImpl.class);

    protected String _name;
    @Inject
    protected UserVmManager _userVmMgr;
    @Inject
    protected AgentManager _agentMgr;
    @Inject
    protected TemplateManager _tmpltMgr;
    @Inject
    protected AsyncJobManager _asyncMgr;
    @Inject
    protected SnapshotManager _snapshotMgr;
    @Inject
    protected SnapshotScheduler _snapshotScheduler;
    @Inject
    protected AccountManager _accountMgr;
    @Inject
    protected ConfigurationManager _configMgr;
    @Inject
    protected ConsoleProxyManager _consoleProxyMgr;
    @Inject
    protected SecondaryStorageVmManager _secStorageMgr;
    @Inject
    protected NetworkManager _networkMgr;
    @Inject
    protected VolumeDao _volsDao;
    @Inject
    protected HostDao _hostDao;
    @Inject
    protected ConsoleProxyDao _consoleProxyDao;
    @Inject
    protected SnapshotDao _snapshotDao;
    @Inject
    protected SnapshotManager _snapMgr;
    @Inject
    protected SnapshotPolicyDao _snapshotPolicyDao;
    @Inject
    protected StoragePoolHostDao _storagePoolHostDao;
    @Inject
    protected AlertManager _alertMgr;
    @Inject
    protected VMTemplateHostDao _vmTemplateHostDao = null;
    @Inject
    protected VMTemplatePoolDao _vmTemplatePoolDao = null;
    @Inject
    protected VMTemplateSwiftDao _vmTemplateSwiftDao = null;
    @Inject
    protected VMTemplateDao _vmTemplateDao = null;
    @Inject
    protected StoragePoolHostDao _poolHostDao = null;
    @Inject
    protected UserVmDao _userVmDao;
    @Inject
    protected VMInstanceDao _vmInstanceDao;
    @Inject
    protected StoragePoolDao _storagePoolDao = null;
    @Inject
    protected CapacityDao _capacityDao;
    @Inject
    protected DiskOfferingDao _diskOfferingDao;
    @Inject
    protected AccountDao _accountDao;
    @Inject
    protected EventDao _eventDao = null;
    @Inject
    protected DataCenterDao _dcDao = null;
    @Inject
    protected HostPodDao _podDao = null;
    @Inject
    protected VMTemplateDao _templateDao;
    @Inject
    protected VMTemplateHostDao _templateHostDao;
    @Inject
    protected ServiceOfferingDao _offeringDao;
    @Inject
    protected DomainDao _domainDao;
    @Inject
    protected UserDao _userDao;
    @Inject
    protected ClusterDao _clusterDao;
    @Inject
    protected VirtualNetworkApplianceManager _routerMgr;
    @Inject
    protected UsageEventDao _usageEventDao;
    @Inject
    protected VirtualMachineManager _vmMgr;
    @Inject
    protected DomainRouterDao _domrDao;
    @Inject
    protected SecondaryStorageVmDao _secStrgDao;
    @Inject
    protected StoragePoolWorkDao _storagePoolWorkDao;
    @Inject
    protected HypervisorGuruManager _hvGuruMgr;
    @Inject
    protected VolumeDao _volumeDao;
    @Inject
    protected OCFS2Manager _ocfs2Mgr;
    @Inject
    protected ResourceLimitService _resourceLimitMgr;
    @Inject
    protected SecondaryStorageVmManager _ssvmMgr;
    @Inject
    protected ResourceManager _resourceMgr;
    @Inject
    protected CheckPointManager _checkPointMgr;

    @Inject(adapter = StoragePoolAllocator.class)
    protected Adapters<StoragePoolAllocator> _storagePoolAllocators;
    @Inject(adapter = StoragePoolDiscoverer.class)
    protected Adapters<StoragePoolDiscoverer> _discoverers;

    protected SearchBuilder<VMTemplateHostVO> HostTemplateStatesSearch;
    protected GenericSearchBuilder<StoragePoolHostVO, Long> UpHostsInPoolSearch;
    protected SearchBuilder<VMInstanceVO> StoragePoolSearch;
    protected SearchBuilder<StoragePoolVO> LocalStorageSearch;

    ScheduledExecutorService _executor = null;
    boolean _storageCleanupEnabled;
    boolean _templateCleanupEnabled = true;
    int _storageCleanupInterval;
    private int _createVolumeFromSnapshotWait;
    private int _copyvolumewait;
    int _storagePoolAcquisitionWaitSeconds = 1800; // 30 minutes
    protected int _retry = 2;
    protected int _pingInterval = 60; // seconds
    protected int _hostRetry;
    protected BigDecimal _overProvisioningFactor = new BigDecimal(1);
    private long _maxVolumeSizeInGb;
    private long _serverId;
    private StateMachine2<Volume.State, Volume.Event, Volume> _volStateMachine;
    private int _customDiskOfferingMinSize = 1;
    private int _customDiskOfferingMaxSize = 1024;

    public boolean share(VMInstanceVO vm, List<VolumeVO> vols, HostVO host, boolean cancelPreviousShare) throws StorageUnavailableException {

        // if pool is in maintenance and it is the ONLY pool available; reject
        List<VolumeVO> rootVolForGivenVm = _volsDao.findByInstanceAndType(vm.getId(), Type.ROOT);
        if (rootVolForGivenVm != null && rootVolForGivenVm.size() > 0) {
            boolean isPoolAvailable = isPoolAvailable(rootVolForGivenVm.get(0).getPoolId());
            if (!isPoolAvailable) {
                throw new StorageUnavailableException("Can not share " + vm, rootVolForGivenVm.get(0).getPoolId());
            }
        }

        // this check is done for maintenance mode for primary storage
        // if any one of the volume is unusable, we return false
        // if we return false, the allocator will try to switch to another PS if available
        for (VolumeVO vol : vols) {
            if (vol.getRemoved() != null) {
                s_logger.warn("Volume id:" + vol.getId() + " is removed, cannot share on this instance");
                // not ok to share
                return false;
            }
        }

        // ok to share
        return true;
    }

    @Override
    public VolumeVO allocateDuplicateVolume(VolumeVO oldVol, Long templateId) {
        VolumeVO newVol = new VolumeVO(oldVol.getVolumeType(), oldVol.getName(), oldVol.getDataCenterId(), oldVol.getDomainId(), oldVol.getAccountId(), oldVol.getDiskOfferingId(), oldVol.getSize());
        if (templateId != null) {
            newVol.setTemplateId(templateId);
        } else {
            newVol.setTemplateId(oldVol.getTemplateId());
        }
        newVol.setDeviceId(oldVol.getDeviceId());
        newVol.setInstanceId(oldVol.getInstanceId());
        newVol.setRecreatable(oldVol.isRecreatable());
        return _volsDao.persist(newVol);
    }

    private boolean isPoolAvailable(Long poolId) {
        // get list of all pools
        List<StoragePoolVO> pools = _storagePoolDao.listAll();

        // if no pools or 1 pool which is in maintenance
        if (pools == null || pools.size() == 0 || (pools.size() == 1 && pools.get(0).getStatus().equals(StoragePoolStatus.Maintenance))) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public List<StoragePoolVO> ListByDataCenterHypervisor(long datacenterId, HypervisorType type) {
        List<StoragePoolVO> pools = _storagePoolDao.listByDataCenterId(datacenterId);
        List<StoragePoolVO> retPools = new ArrayList<StoragePoolVO>();
        for (StoragePoolVO pool : pools) {
            if (pool.getStatus() != StoragePoolStatus.Up) {
                continue;
            }
            ClusterVO cluster = _clusterDao.findById(pool.getClusterId());
            if (type == cluster.getHypervisorType()) {
                retPools.add(pool);
            }
        }
        Collections.shuffle(retPools);
        return retPools;
    }

    @Override
    public boolean isLocalStorageActiveOnHost(Host host) {
        List<StoragePoolHostVO> storagePoolHostRefs = _storagePoolHostDao.listByHostId(host.getId());
        for (StoragePoolHostVO storagePoolHostRef : storagePoolHostRefs) {
            StoragePoolVO storagePool = _storagePoolDao.findById(storagePoolHostRef.getPoolId());
            if (storagePool.getPoolType() == StoragePoolType.LVM || storagePool.getPoolType() == StoragePoolType.EXT) {
                SearchBuilder<VolumeVO> volumeSB = _volsDao.createSearchBuilder();
                volumeSB.and("poolId", volumeSB.entity().getPoolId(), SearchCriteria.Op.EQ);
                volumeSB.and("removed", volumeSB.entity().getRemoved(), SearchCriteria.Op.NULL);

                SearchBuilder<VMInstanceVO> activeVmSB = _vmInstanceDao.createSearchBuilder();
                activeVmSB.and("state", activeVmSB.entity().getState(), SearchCriteria.Op.IN);
                volumeSB.join("activeVmSB", activeVmSB, volumeSB.entity().getInstanceId(), activeVmSB.entity().getId(), JoinBuilder.JoinType.INNER);

                SearchCriteria<VolumeVO> volumeSC = volumeSB.create();
                volumeSC.setParameters("poolId", storagePool.getId());
                volumeSC.setJoinParameters("activeVmSB", "state", State.Starting, State.Running, State.Stopping, State.Migrating);

                List<VolumeVO> volumes = _volsDao.search(volumeSC, null);
                if (volumes.size() > 0) {
                    return true;
                }
            }
        }

        return false;
    }

    protected StoragePoolVO findStoragePool(DiskProfile dskCh, final DataCenterVO dc, HostPodVO pod, Long clusterId, VMInstanceVO vm, final Set<StoragePool> avoid) {

        VirtualMachineProfile<VMInstanceVO> profile = new VirtualMachineProfileImpl<VMInstanceVO>(vm);
        Enumeration<StoragePoolAllocator> en = _storagePoolAllocators.enumeration();
        while (en.hasMoreElements()) {
            final StoragePoolAllocator allocator = en.nextElement();
            final List<StoragePool> poolList = allocator.allocateToPool(dskCh, profile, dc.getId(), pod.getId(), clusterId, avoid, 1);
            if (poolList != null && !poolList.isEmpty()) {
                return (StoragePoolVO) poolList.get(0);
            }
        }
        return null;
    }

    @Override
    public Answer[] sendToPool(StoragePool pool, Commands cmds) throws StorageUnavailableException {
        return sendToPool(pool, null, null, cmds).second();
    }

    @Override
    public Answer sendToPool(StoragePool pool, long[] hostIdsToTryFirst, Command cmd) throws StorageUnavailableException {
        Answer[] answers = sendToPool(pool, hostIdsToTryFirst, null, new Commands(cmd)).second();
        if (answers == null) {
            return null;
        }
        return answers[0];
    }

    @Override
    public Answer sendToPool(StoragePool pool, Command cmd) throws StorageUnavailableException {
        Answer[] answers = sendToPool(pool, new Commands(cmd));
        if (answers == null) {
            return null;
        }
        return answers[0];
    }

    @Override
    public Answer sendToPool(long poolId, Command cmd) throws StorageUnavailableException {
        StoragePool pool = _storagePoolDao.findById(poolId);
        return sendToPool(pool, cmd);
    }

    @Override
    public Answer[] sendToPool(long poolId, Commands cmds) throws StorageUnavailableException {
        StoragePool pool = _storagePoolDao.findById(poolId);
        return sendToPool(pool, cmds);
    }

    protected DiskProfile createDiskCharacteristics(VolumeVO volume, VMTemplateVO template, DataCenterVO dc, DiskOfferingVO diskOffering) {
        if (volume.getVolumeType() == Type.ROOT && Storage.ImageFormat.ISO != template.getFormat()) {
            SearchCriteria<VMTemplateHostVO> sc = HostTemplateStatesSearch.create();
            sc.setParameters("id", template.getId());
            sc.setParameters("state", com.cloud.storage.VMTemplateStorageResourceAssoc.Status.DOWNLOADED);
            sc.setJoinParameters("host", "dcId", dc.getId());

            List<VMTemplateHostVO> sss = _vmTemplateHostDao.search(sc, null);
            if (sss.size() == 0) {
                throw new CloudRuntimeException("Template " + template.getName() + " has not been completely downloaded to zone " + dc.getId());
            }
            VMTemplateHostVO ss = sss.get(0);

            return new DiskProfile(volume.getId(), volume.getVolumeType(), volume.getName(), diskOffering.getId(), ss.getSize(), diskOffering.getTagsArray(), diskOffering.getUseLocalStorage(),
                    diskOffering.isRecreatable(), Storage.ImageFormat.ISO != template.getFormat() ? template.getId() : null);
        } else {
            return new DiskProfile(volume.getId(), volume.getVolumeType(), volume.getName(), diskOffering.getId(), diskOffering.getDiskSize(), diskOffering.getTagsArray(),
                    diskOffering.getUseLocalStorage(), diskOffering.isRecreatable(), null);
        }
    }

    @Override
    public boolean canVmRestartOnAnotherServer(long vmId) {
        List<VolumeVO> vols = _volsDao.findCreatedByInstance(vmId);
        for (VolumeVO vol : vols) {
            if (!vol.isRecreatable() && !vol.getPoolType().isShared()) {
                return false;
            }
        }
        return true;
    }

    @DB
    protected Pair<VolumeVO, String> createVolumeFromSnapshot(VolumeVO volume, SnapshotVO snapshot) {
        VolumeVO createdVolume = null;
        Long volumeId = volume.getId();

        String volumeFolder = null;

        try {
            stateTransitTo(volume, Volume.Event.CreateRequested);
        } catch (NoTransitionException e) {
            s_logger.debug(e.toString());
            return null;
        }
        // Create the Volume object and save it so that we can return it to the user
        Account account = _accountDao.findById(volume.getAccountId());

        final HashSet<StoragePool> poolsToAvoid = new HashSet<StoragePool>();
        StoragePoolVO pool = null;
        boolean success = false;
        Set<Long> podsToAvoid = new HashSet<Long>();
        Pair<HostPodVO, Long> pod = null;
        String volumeUUID = null;
        String details = null;

        DiskOfferingVO diskOffering = _diskOfferingDao.findByIdIncludingRemoved(volume.getDiskOfferingId());
        DataCenterVO dc = _dcDao.findById(volume.getDataCenterId());
        DiskProfile dskCh = new DiskProfile(volume, diskOffering, snapshot.getHypervisorType());

        int retry = 0;
        // Determine what pod to store the volume in
        while ((pod = _resourceMgr.findPod(null, null, dc, account.getId(), podsToAvoid)) != null) {
            podsToAvoid.add(pod.first().getId());
            // Determine what storage pool to store the volume in
            while ((pool = findStoragePool(dskCh, dc, pod.first(), null, null, poolsToAvoid)) != null) {
                poolsToAvoid.add(pool);
                volumeFolder = pool.getPath();
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Attempting to create volume from snapshotId: " + snapshot.getId() + " on storage pool " + pool.getName());
                }

                // Get the newly created VDI from the snapshot.
                // This will return a null volumePath if it could not be created
                Pair<String, String> volumeDetails = createVDIFromSnapshot(UserContext.current().getCallerUserId(), snapshot, pool);

                volumeUUID = volumeDetails.first();
                details = volumeDetails.second();

                if (volumeUUID != null) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Volume with UUID " + volumeUUID + " was created on storage pool " + pool.getName());
                    }
                    success = true;
                    break; // break out of the "find storage pool" loop
                } else {
                    retry++;
                    if (retry >= 3) {
                        _volsDao.expunge(volumeId);
                        String msg = "Unable to create volume from snapshot " + snapshot.getId() + " after retrying 3 times, due to " + details;
                        s_logger.debug(msg);
                        throw new CloudRuntimeException(msg);

                    }
                }
                s_logger.warn("Unable to create volume on pool " + pool.getName() + ", reason: " + details);
            }

            if (success) {
                break; // break out of the "find pod" loop
            }
        }

        if (!success) {
            _volsDao.expunge(volumeId);
            String msg = "Unable to create volume from snapshot " + snapshot.getId() + " due to " + details;
            s_logger.debug(msg);
            throw new CloudRuntimeException(msg);

        }

        createdVolume = _volsDao.findById(volumeId);

        try {
            if (success) {
                createdVolume.setPodId(pod.first().getId());
                createdVolume.setPoolId(pool.getId());
                createdVolume.setPoolType(pool.getPoolType());
                createdVolume.setFolder(volumeFolder);
                createdVolume.setPath(volumeUUID);
                createdVolume.setDomainId(account.getDomainId());
                stateTransitTo(createdVolume, Volume.Event.OperationSucceeded);
            }
        } catch (NoTransitionException e) {
            s_logger.debug("Failed to update volume state: " + e.toString());
            return null;
        }

        return new Pair<VolumeVO, String>(createdVolume, details);
    }

    @Override
    public boolean stateTransitTo(Volume vol, Volume.Event event) throws NoTransitionException {
        return _volStateMachine.transitTo(vol, event, null, _volsDao);
    }

    protected VolumeVO createVolumeFromSnapshot(VolumeVO volume, long snapshotId) {

        // By default, assume failure.
        VolumeVO createdVolume = null;
        SnapshotVO snapshot = _snapshotDao.findById(snapshotId); // Precondition: snapshot is not null and not removed.

        Pair<VolumeVO, String> volumeDetails = createVolumeFromSnapshot(volume, snapshot);
        if (volumeDetails != null) {
            createdVolume = volumeDetails.first();
        }
        return createdVolume;
    }

    protected Pair<String, String> createVDIFromSnapshot(long userId, SnapshotVO snapshot, StoragePoolVO pool) {
        String vdiUUID = null;
        Long snapshotId = snapshot.getId();
        Long volumeId = snapshot.getVolumeId();
        String primaryStoragePoolNameLabel = pool.getUuid(); // pool's uuid is actually the namelabel.
        Long dcId = snapshot.getDataCenterId();
        String secondaryStoragePoolUrl = _snapMgr.getSecondaryStorageURL(snapshot);
        long accountId = snapshot.getAccountId();

        String backedUpSnapshotUuid = snapshot.getBackupSnapshotId();
        snapshot = _snapshotDao.findById(snapshotId);
        if (snapshot.getVersion().trim().equals("2.1")) {
            VolumeVO volume = _volsDao.findByIdIncludingRemoved(volumeId);
            if (volume == null) {
                throw new CloudRuntimeException("failed to upgrade snapshot " + snapshotId + " due to unable to find orignal volume:" + volumeId + ", try it later ");
            }
            if (volume.getTemplateId() == null) {
                _snapshotDao.updateSnapshotVersion(volumeId, "2.1", "2.2");
            } else {
                VMTemplateVO template = _templateDao.findByIdIncludingRemoved(volume.getTemplateId());
                if (template == null) {
                    throw new CloudRuntimeException("failed to upgrade snapshot " + snapshotId + " due to unalbe to find orignal template :" + volume.getTemplateId() + ", try it later ");
                }
                Long templateId = template.getId();
                Long tmpltAccountId = template.getAccountId();
                if (!_snapshotDao.lockInLockTable(snapshotId.toString(), 10)) {
                    throw new CloudRuntimeException("failed to upgrade snapshot " + snapshotId + " due to this snapshot is being used, try it later ");
                }
                UpgradeSnapshotCommand cmd = new UpgradeSnapshotCommand(null, secondaryStoragePoolUrl, dcId, accountId, volumeId, templateId, tmpltAccountId, null, snapshot.getBackupSnapshotId(),
                        snapshot.getName(), "2.1");
                Answer answer = null;
                try {
                    answer = sendToPool(pool, cmd);
                } catch (StorageUnavailableException e) {
                } finally {
                    _snapshotDao.unlockFromLockTable(snapshotId.toString());
                }
                if ((answer != null) && answer.getResult()) {
                    _snapshotDao.updateSnapshotVersion(volumeId, "2.1", "2.2");
                } else {
                    return new Pair<String, String>(null, "Unable to upgrade snapshot from 2.1 to 2.2 for " + snapshot.getId());
                }
            }
        }
        String basicErrMsg = "Failed to create volume from " + snapshot.getName() + " on pool " + pool;
        try {
            if (snapshot.getSwiftId() != null) {
                _snapshotMgr.downloadSnapshotsFromSwift(snapshot);
            }
            CreateVolumeFromSnapshotCommand createVolumeFromSnapshotCommand = new CreateVolumeFromSnapshotCommand(primaryStoragePoolNameLabel, secondaryStoragePoolUrl, dcId, accountId, volumeId,
                    backedUpSnapshotUuid, snapshot.getName(), _createVolumeFromSnapshotWait);
            CreateVolumeFromSnapshotAnswer answer;
            if (!_snapshotDao.lockInLockTable(snapshotId.toString(), 10)) {
                throw new CloudRuntimeException("failed to create volume from " + snapshotId + " due to this snapshot is being used, try it later ");
            }
            answer = (CreateVolumeFromSnapshotAnswer) sendToPool(pool, createVolumeFromSnapshotCommand);
            if (answer != null && answer.getResult()) {
                vdiUUID = answer.getVdi();
            } else {
                s_logger.error(basicErrMsg + " due to " + ((answer == null) ? "null" : answer.getDetails()));
                throw new CloudRuntimeException(basicErrMsg);
            }
        } catch (StorageUnavailableException e) {
            s_logger.error(basicErrMsg);
        } finally {
            if (snapshot.getSwiftId() != null) {
                _snapshotMgr.deleteSnapshotsDirForVolume(secondaryStoragePoolUrl, dcId, accountId, volumeId);
            }
            _snapshotDao.unlockFromLockTable(snapshotId.toString());
        }
        return new Pair<String, String>(vdiUUID, basicErrMsg);
    }

    @Override
    @DB
    public VolumeVO createVolume(VolumeVO volume, VMInstanceVO vm, VMTemplateVO template, DataCenterVO dc, HostPodVO pod, Long clusterId, ServiceOfferingVO offering, DiskOfferingVO diskOffering,
            List<StoragePoolVO> avoids, long size, HypervisorType hyperType) {
        StoragePoolVO pool = null;
        final HashSet<StoragePool> avoidPools = new HashSet<StoragePool>(avoids);

        try {
            stateTransitTo(volume, Volume.Event.CreateRequested);
        } catch (NoTransitionException e) {
            s_logger.debug("Unable to update volume state: " + e.toString());
            return null;
        }

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

        VolumeTO created = null;
        int retry = _retry;
        while (--retry >= 0) {
            created = null;

            long podId = pod.getId();
            pod = _podDao.findById(podId);
            if (pod == null) {
                s_logger.warn("Unable to find pod " + podId + " when create volume " + volume.getName());
                break;
            }

            pool = findStoragePool(dskCh, dc, pod, clusterId, vm, avoidPools);
            if (pool == null) {
                s_logger.warn("Unable to find storage poll when create volume " + volume.getName());
                break;
            }

            avoidPools.add(pool);
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Trying to create " + volume + " on " + pool);
            }

            CreateCommand cmd = null;
            VMTemplateStoragePoolVO tmpltStoredOn = null;

            for (int i = 0; i < 2; i++) {
                if (volume.getVolumeType() == Type.ROOT && Storage.ImageFormat.ISO != template.getFormat()) {
                    tmpltStoredOn = _tmpltMgr.prepareTemplateForCreate(template, pool);
                    if (tmpltStoredOn == null) {
                        continue;
                    }
                    cmd = new CreateCommand(dskCh, tmpltStoredOn.getLocalDownloadPath(), new StorageFilerTO(pool));
                } else {
                    if (volume.getVolumeType() == Type.ROOT && Storage.ImageFormat.ISO == template.getFormat()) {
                        VMTemplateHostVO tmpltHostOn = _tmpltMgr.prepareISOForCreate(template, pool);
                        if (tmpltHostOn == null) {
                            throw new CloudRuntimeException("Did not find ISO in secondry storage in zone " + pool.getDataCenterId());
                        }
                    }
                    cmd = new CreateCommand(dskCh, new StorageFilerTO(pool));
                }

                try {
                    Answer answer = sendToPool(pool, cmd);
                    if (answer != null && answer.getResult()) {
                        created = ((CreateAnswer) answer).getVolume();
                        break;
                    }

                    if (tmpltStoredOn != null && answer != null && (answer instanceof CreateAnswer) && ((CreateAnswer) answer).templateReloadRequested()) {
                        if (!_tmpltMgr.resetTemplateDownloadStateOnPool(tmpltStoredOn.getId())) {
                            break; // break out of template-redeploy retry loop
                        }
                    } else {
                        break;
                    }
                } catch (StorageUnavailableException e) {
                    s_logger.debug("Storage unavailable for " + pool.getId());
                    break; // break out of template-redeploy retry loop
                }
            }

            if (created != null) {
                break;
            }

            s_logger.debug("Retrying the create because it failed on pool " + pool);
        }

        if (created == null) {
            return null;
        } else {
            volume.setFolder(pool.getPath());
            volume.setPath(created.getPath());
            volume.setSize(created.getSize());
            volume.setPoolType(pool.getPoolType());
            volume.setPoolId(pool.getId());
            volume.setPodId(pod.getId());
            try {
                stateTransitTo(volume, Volume.Event.OperationSucceeded);
            } catch (NoTransitionException e) {
                s_logger.debug("Unable to update volume state: " + e.toString());
                return null;
            }
            return volume;
        }
    }

    public Long chooseHostForStoragePool(StoragePoolVO poolVO, List<Long> avoidHosts, boolean sendToVmResidesOn, Long vmId) {
        if (sendToVmResidesOn) {
            if (vmId != null) {
                VMInstanceVO vmInstance = _vmInstanceDao.findById(vmId);
                if (vmInstance != null) {
                    Long hostId = vmInstance.getHostId();
                    if (hostId != null && !avoidHosts.contains(vmInstance.getHostId())) {
                        return hostId;
                    }
                }
            }
            /*
             * Can't find the vm where host resides on(vm is destroyed? or volume is detached from vm), randomly choose
             * a host
             * to send the cmd
             */
        }
        List<StoragePoolHostVO> poolHosts = _poolHostDao.listByHostStatus(poolVO.getId(), Status.Up);
        Collections.shuffle(poolHosts);
        if (poolHosts != null && poolHosts.size() > 0) {
            for (StoragePoolHostVO sphvo : poolHosts) {
                if (!avoidHosts.contains(sphvo.getHostId())) {
                    return sphvo.getHostId();
                }
            }
        }
        return null;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _name = name;

        ComponentLocator locator = ComponentLocator.getCurrentLocator();

        ConfigurationDao configDao = locator.getDao(ConfigurationDao.class);
        if (configDao == null) {
            s_logger.error("Unable to get the configuration dao.");
            return false;
        }

        Map<String, String> configs = configDao.getConfiguration("management-server", params);

        String overProvisioningFactorStr = configs.get("storage.overprovisioning.factor");
        if (overProvisioningFactorStr != null) {
            _overProvisioningFactor = new BigDecimal(overProvisioningFactorStr);
        }

        _retry = NumbersUtil.parseInt(configs.get(Config.StartRetry.key()), 10);
        _pingInterval = NumbersUtil.parseInt(configs.get("ping.interval"), 60);
        _hostRetry = NumbersUtil.parseInt(configs.get("host.retry"), 2);
        _storagePoolAcquisitionWaitSeconds = NumbersUtil.parseInt(configs.get("pool.acquisition.wait.seconds"), 1800);
        s_logger.info("pool.acquisition.wait.seconds is configured as " + _storagePoolAcquisitionWaitSeconds + " seconds");

        _agentMgr.registerForHostEvents(new StoragePoolMonitor(this, _storagePoolDao), true, false, true);

        String storageCleanupEnabled = configs.get("storage.cleanup.enabled");
        _storageCleanupEnabled = (storageCleanupEnabled == null) ? true : Boolean.parseBoolean(storageCleanupEnabled);

        String value = configDao.getValue(Config.CreateVolumeFromSnapshotWait.toString());
        _createVolumeFromSnapshotWait = NumbersUtil.parseInt(value, Integer.parseInt(Config.CreateVolumeFromSnapshotWait.getDefaultValue()));

        value = configDao.getValue(Config.CopyVolumeWait.toString());
        _copyvolumewait = NumbersUtil.parseInt(value, Integer.parseInt(Config.CopyVolumeWait.getDefaultValue()));

        value = configDao.getValue(Config.StorageTemplateCleanupEnabled.key());
        _templateCleanupEnabled = (value == null ? true : Boolean.parseBoolean(value));

        String time = configs.get("storage.cleanup.interval");
        _storageCleanupInterval = NumbersUtil.parseInt(time, 86400);

        s_logger.info("Storage cleanup enabled: " + _storageCleanupEnabled + ", interval: " + _storageCleanupInterval + ", template cleanup enabled: " + _templateCleanupEnabled);

        String workers = configs.get("expunge.workers");
        int wrks = NumbersUtil.parseInt(workers, 10);
        _executor = Executors.newScheduledThreadPool(wrks, new NamedThreadFactory("StorageManager-Scavenger"));

        boolean localStorage = Boolean.parseBoolean(configs.get(Config.UseLocalStorage.key()));
        if (localStorage) {
            _agentMgr.registerForHostEvents(ComponentLocator.inject(LocalStoragePoolListener.class), true, false, false);
        }

        String maxVolumeSizeInGbString = configDao.getValue("storage.max.volume.size");
        _maxVolumeSizeInGb = NumbersUtil.parseLong(maxVolumeSizeInGbString, 2000);

        String _customDiskOfferingMinSizeStr = configDao.getValue(Config.CustomDiskOfferingMinSize.toString());
        _customDiskOfferingMinSize = NumbersUtil.parseInt(_customDiskOfferingMinSizeStr, Integer.parseInt(Config.CustomDiskOfferingMinSize.getDefaultValue()));

        String _customDiskOfferingMaxSizeStr = configDao.getValue(Config.CustomDiskOfferingMaxSize.toString());
        _customDiskOfferingMaxSize = NumbersUtil.parseInt(_customDiskOfferingMaxSizeStr, Integer.parseInt(Config.CustomDiskOfferingMaxSize.getDefaultValue()));

        HostTemplateStatesSearch = _vmTemplateHostDao.createSearchBuilder();
        HostTemplateStatesSearch.and("id", HostTemplateStatesSearch.entity().getTemplateId(), SearchCriteria.Op.EQ);
        HostTemplateStatesSearch.and("state", HostTemplateStatesSearch.entity().getDownloadState(), SearchCriteria.Op.EQ);

        SearchBuilder<HostVO> HostSearch = _hostDao.createSearchBuilder();
        HostSearch.and("dcId", HostSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);

        HostTemplateStatesSearch.join("host", HostSearch, HostSearch.entity().getId(), HostTemplateStatesSearch.entity().getHostId(), JoinBuilder.JoinType.INNER);
        HostSearch.done();
        HostTemplateStatesSearch.done();

        _serverId = ((ManagementServer) ComponentLocator.getComponent(ManagementServer.Name)).getId();

        UpHostsInPoolSearch = _storagePoolHostDao.createSearchBuilder(Long.class);
        UpHostsInPoolSearch.selectField(UpHostsInPoolSearch.entity().getHostId());
        SearchBuilder<HostVO> hostSearch = _hostDao.createSearchBuilder();
        hostSearch.and("status", hostSearch.entity().getStatus(), Op.EQ);
        hostSearch.and("resourceState", hostSearch.entity().getResourceState(), Op.EQ);
        UpHostsInPoolSearch.join("hosts", hostSearch, hostSearch.entity().getId(), UpHostsInPoolSearch.entity().getHostId(), JoinType.INNER);
        UpHostsInPoolSearch.and("pool", UpHostsInPoolSearch.entity().getPoolId(), Op.EQ);
        UpHostsInPoolSearch.done();

        StoragePoolSearch = _vmInstanceDao.createSearchBuilder();

        SearchBuilder<VolumeVO> volumeSearch = _volumeDao.createSearchBuilder();
        volumeSearch.and("volumeType", volumeSearch.entity().getVolumeType(), SearchCriteria.Op.EQ);
        volumeSearch.and("poolId", volumeSearch.entity().getPoolId(), SearchCriteria.Op.EQ);
        StoragePoolSearch.join("vmVolume", volumeSearch, volumeSearch.entity().getInstanceId(), StoragePoolSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        StoragePoolSearch.done();

        LocalStorageSearch = _storagePoolDao.createSearchBuilder();
        SearchBuilder<StoragePoolHostVO> storageHostSearch = _storagePoolHostDao.createSearchBuilder();
        storageHostSearch.and("hostId", storageHostSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        LocalStorageSearch.join("poolHost", storageHostSearch, storageHostSearch.entity().getPoolId(), LocalStorageSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        LocalStorageSearch.and("type", LocalStorageSearch.entity().getPoolType(), SearchCriteria.Op.IN);
        LocalStorageSearch.done();
        return true;
    }

    public String getRandomVolumeName() {
        return UUID.randomUUID().toString();
    }

    @Override
    public boolean volumeOnSharedStoragePool(VolumeVO volume) {
        Long poolId = volume.getPoolId();
        if (poolId == null) {
            return false;
        } else {
            StoragePoolVO pool = _storagePoolDao.findById(poolId);

            if (pool == null) {
                return false;
            } else {
                return pool.isShared();
            }
        }
    }

    @Override
    public boolean volumeInactive(VolumeVO volume) {
        Long vmId = volume.getInstanceId();
        if (vmId != null) {
            UserVm vm = _userVmDao.findById(vmId);
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
    public String getVmNameOnVolume(VolumeVO volume) {
        Long vmId = volume.getInstanceId();
        if (vmId != null) {
            VMInstanceVO vm = _vmInstanceDao.findById(vmId);

            if (vm == null) {
                return null;
            }
            return vm.getInstanceName();
        }
        return null;
    }

    @Override
    public Pair<String, String> getAbsoluteIsoPath(long templateId, long dataCenterId) {
        String isoPath = null;

        List<HostVO> storageHosts = _resourceMgr.listAllHostsInOneZoneByType(Host.Type.SecondaryStorage, dataCenterId);
        if (storageHosts != null) {
            for (HostVO storageHost : storageHosts) {
                VMTemplateHostVO templateHostVO = _vmTemplateHostDao.findByHostTemplate(storageHost.getId(), templateId);
                if (templateHostVO != null) {
                    isoPath = storageHost.getStorageUrl() + "/" + templateHostVO.getInstallPath();
                    return new Pair<String, String>(isoPath, storageHost.getStorageUrl());
                }
            }
        }
        s_logger.warn("Unable to find secondary storage in zone id=" + dataCenterId);
        return null;
    }

    @Override
    public String getSecondaryStorageURL(long zoneId) {
        // Determine the secondary storage URL
        HostVO secondaryStorageHost = getSecondaryStorageHost(zoneId);

        if (secondaryStorageHost == null) {
            return null;
        }

        return secondaryStorageHost.getStorageUrl();
    }

    @Override
    public HostVO getSecondaryStorageHost(long zoneId, long tmpltId) {
        List<HostVO> hosts = _ssvmMgr.listSecondaryStorageHostsInOneZone(zoneId);
        if (hosts == null || hosts.size() == 0) {
            return null;
        }
        for (HostVO host : hosts) {
            VMTemplateHostVO tmpltHost = _vmTemplateHostDao.findByHostTemplate(host.getId(), tmpltId);
            if (tmpltHost != null && !tmpltHost.getDestroyed() && tmpltHost.getDownloadState() == VMTemplateStorageResourceAssoc.Status.DOWNLOADED) {
                return host;
            }
        }
        return null;
    }

    @Override
    public VMTemplateHostVO getTemplateHostRef(long zoneId, long tmpltId, boolean readyOnly) {
        List<HostVO> hosts = _ssvmMgr.listSecondaryStorageHostsInOneZone(zoneId);
        if (hosts == null || hosts.size() == 0) {
            return null;
        }
        VMTemplateHostVO inProgress = null;
        VMTemplateHostVO other = null;
        for (HostVO host : hosts) {
            VMTemplateHostVO tmpltHost = _vmTemplateHostDao.findByHostTemplate(host.getId(), tmpltId);
            if (tmpltHost != null && !tmpltHost.getDestroyed()) {
                if (tmpltHost.getDownloadState() == VMTemplateStorageResourceAssoc.Status.DOWNLOADED) {
                    return tmpltHost;
                } else if (tmpltHost.getDownloadState() == VMTemplateStorageResourceAssoc.Status.DOWNLOAD_IN_PROGRESS) {
                    inProgress = tmpltHost;
                } else {
                    other = tmpltHost;
                }
            }
        }
        if (inProgress != null) {
            return inProgress;
        }
        return other;
    }

    @Override
    public HostVO getSecondaryStorageHost(long zoneId) {
        List<HostVO> hosts = _ssvmMgr.listSecondaryStorageHostsInOneZone(zoneId);
        if (hosts == null || hosts.size() == 0) {
            hosts = _ssvmMgr.listLocalSecondaryStorageHostsInOneZone(zoneId);
            if (hosts.isEmpty()) {
                return null;
            }
        }

        int size = hosts.size();
        Random rn = new Random();
        int index = rn.nextInt(size);
        return hosts.get(index);
    }

    @Override
    public List<HostVO> getSecondaryStorageHosts(long zoneId) {
        List<HostVO> hosts = _ssvmMgr.listSecondaryStorageHostsInOneZone(zoneId);
        if (hosts == null || hosts.size() == 0) {
            hosts = _ssvmMgr.listLocalSecondaryStorageHostsInOneZone(zoneId);
            if (hosts.isEmpty()) {
                return new ArrayList<HostVO>();
            }
        }
        return hosts;
    }

    @Override
    public String getStoragePoolTags(long poolId) {
        return _configMgr.listToCsvTags(_storagePoolDao.searchForStoragePoolDetails(poolId, "true"));
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public boolean start() {
        if (_storageCleanupEnabled) {
            Random generator = new Random();
            int initialDelay = generator.nextInt(_storageCleanupInterval);
            _executor.scheduleWithFixedDelay(new StorageGarbageCollector(), initialDelay, _storageCleanupInterval, TimeUnit.SECONDS);
        } else {
            s_logger.debug("Storage cleanup is not enabled, so the storage cleanup thread is not being scheduled.");
        }

        return true;
    }

    @Override
    public boolean stop() {
        if (_storageCleanupEnabled) {
            _executor.shutdown();
        }

        return true;
    }

    protected StorageManagerImpl() {
        _volStateMachine = Volume.State.getStateMachine();
    }

    @Override
    @SuppressWarnings("rawtypes")
    public StoragePoolVO createPool(CreateStoragePoolCmd cmd) throws ResourceInUseException, IllegalArgumentException, UnknownHostException, ResourceUnavailableException {
        Long clusterId = cmd.getClusterId();
        Long podId = cmd.getPodId();
        Map ds = cmd.getDetails();

        if (clusterId != null && podId == null) {
            throw new InvalidParameterValueException("Cluster id requires pod id");
        }

        Map<String, String> details = new HashMap<String, String>();
        if (ds != null) {
            Collection detailsCollection = ds.values();
            Iterator it = detailsCollection.iterator();
            while (it.hasNext()) {
                HashMap d = (HashMap) it.next();
                Iterator it2 = d.entrySet().iterator();
                while (it2.hasNext()) {
                    Map.Entry entry = (Map.Entry) it2.next();
                    details.put((String) entry.getKey(), (String) entry.getValue());
                }
            }
        }

        // verify input parameters
        Long zoneId = cmd.getZoneId();
        DataCenterVO zone = _dcDao.findById(cmd.getZoneId());
        if (zone == null) {
            throw new InvalidParameterValueException("unable to find zone by id " + zoneId);
        }
        // Check if zone is disabled
        Account account = UserContext.current().getCaller();
        if (Grouping.AllocationState.Disabled == zone.getAllocationState() && !_accountMgr.isRootAdmin(account.getType())) {
            throw new PermissionDeniedException("Cannot perform this operation, Zone is currently disabled: " + zoneId);
        }

        // Check if there is host up in this cluster
        List<HostVO> allHosts = _resourceMgr.listAllUpAndEnabledHosts(Host.Type.Routing, clusterId, podId, zoneId);
        if (allHosts.isEmpty()) {
            throw new ResourceUnavailableException("No host up to associate a storage pool with in cluster " + clusterId, Pod.class, podId);
        }
        URI uri = null;
        try {
            uri = new URI(cmd.getUrl());
            if (uri.getScheme() == null) {
                throw new InvalidParameterValueException("scheme is null " + cmd.getUrl() + ", add nfs:// as a prefix");
            } else if (uri.getScheme().equalsIgnoreCase("nfs")) {
                String uriHost = uri.getHost();
                String uriPath = uri.getPath();
                if (uriHost == null || uriPath == null || uriHost.trim().isEmpty() || uriPath.trim().isEmpty()) {
                    throw new InvalidParameterValueException("host or path is null, should be nfs://hostname/path");
                }
            } else if (uri.getScheme().equalsIgnoreCase("sharedMountPoint")) {
                String uriPath = uri.getPath();
                if (uriPath == null) {
                    throw new InvalidParameterValueException("host or path is null, should be sharedmountpoint://localhost/path");
                }
            } else if (uri.getScheme().equalsIgnoreCase("clvm")) {
                String uriPath = uri.getPath();
                if (uriPath == null) {
                    throw new InvalidParameterValueException("host or path is null, should be clvm://localhost/path");
                }
            }
        } catch (URISyntaxException e) {
            throw new InvalidParameterValueException(cmd.getUrl() + " is not a valid uri");
        }

        String tags = cmd.getTags();
        if (tags != null) {
            String[] tokens = tags.split(",");

            for (String tag : tokens) {
                tag = tag.trim();
                if (tag.length() == 0) {
                    continue;
                }
                details.put(tag, "true");
            }
        }

        String scheme = uri.getScheme();
        String storageHost = uri.getHost();
        String hostPath = uri.getPath();
        int port = uri.getPort();
        StoragePoolVO pool = null;
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("createPool Params @ scheme - " + scheme + " storageHost - " + storageHost + " hostPath - " + hostPath + " port - " + port);
        }
        if (scheme.equalsIgnoreCase("nfs")) {
            if (port == -1) {
                port = 2049;
            }
            pool = new StoragePoolVO(StoragePoolType.NetworkFilesystem, storageHost, port, hostPath);
            if (clusterId == null) {
                throw new IllegalArgumentException("NFS need to have clusters specified for XenServers");
            }
        } else if (scheme.equalsIgnoreCase("file")) {
            if (port == -1) {
                port = 0;
            }
            pool = new StoragePoolVO(StoragePoolType.Filesystem, "localhost", 0, hostPath);
        } else if (scheme.equalsIgnoreCase("sharedMountPoint")) {
            pool = new StoragePoolVO(StoragePoolType.SharedMountPoint, storageHost, 0, hostPath);
        } else if (scheme.equalsIgnoreCase("clvm")) {
            pool = new StoragePoolVO(StoragePoolType.CLVM, storageHost, 0, hostPath.replaceFirst("/", ""));
        } else if (scheme.equalsIgnoreCase("PreSetup")) {
            pool = new StoragePoolVO(StoragePoolType.PreSetup, storageHost, 0, hostPath);
        } else if (scheme.equalsIgnoreCase("iscsi")) {
            String[] tokens = hostPath.split("/");
            int lun = NumbersUtil.parseInt(tokens[tokens.length - 1], -1);
            if (port == -1) {
                port = 3260;
            }
            if (lun != -1) {
                if (clusterId == null) {
                    throw new IllegalArgumentException("IscsiLUN need to have clusters specified");
                }
                hostPath.replaceFirst("/", "");
                pool = new StoragePoolVO(StoragePoolType.IscsiLUN, storageHost, port, hostPath);
            } else {
                Enumeration<StoragePoolDiscoverer> en = _discoverers.enumeration();
                while (en.hasMoreElements()) {
                    Map<StoragePoolVO, Map<String, String>> pools;
                    try {
                        pools = en.nextElement().find(cmd.getZoneId(), podId, uri, details);
                    } catch (DiscoveryException e) {
                        throw new IllegalArgumentException("Not enough information for discovery " + uri, e);
                    }
                    if (pools != null) {
                        Map.Entry<StoragePoolVO, Map<String, String>> entry = pools.entrySet().iterator().next();
                        pool = entry.getKey();
                        details = entry.getValue();
                        break;
                    }
                }
            }
        } else if (scheme.equalsIgnoreCase("iso")) {
            if (port == -1) {
                port = 2049;
            }
            pool = new StoragePoolVO(StoragePoolType.ISO, storageHost, port, hostPath);
        } else if (scheme.equalsIgnoreCase("vmfs")) {
            pool = new StoragePoolVO(StoragePoolType.VMFS, "VMFS datastore: " + hostPath, 0, hostPath);
        } else if (scheme.equalsIgnoreCase("ocfs2")) {
            port = 7777;
            pool = new StoragePoolVO(StoragePoolType.OCFS2, "clustered", port, hostPath);
        } else {
            s_logger.warn("Unable to figure out the scheme for URI: " + uri);
            throw new IllegalArgumentException("Unable to figure out the scheme for URI: " + uri);
        }

        if (pool == null) {
            s_logger.warn("Unable to figure out the scheme for URI: " + uri);
            throw new IllegalArgumentException("Unable to figure out the scheme for URI: " + uri);
        }

        List<StoragePoolVO> pools = _storagePoolDao.listPoolByHostPath(storageHost, hostPath);
        if (!pools.isEmpty() && !scheme.equalsIgnoreCase("sharedmountpoint")) {
            Long oldPodId = pools.get(0).getPodId();
            throw new ResourceInUseException("Storage pool " + uri + " already in use by another pod (id=" + oldPodId + ")", "StoragePool", uri.toASCIIString());
        }

        long poolId = _storagePoolDao.getNextInSequence(Long.class, "id");
        String uuid = null;
        if (scheme.equalsIgnoreCase("sharedmountpoint") || scheme.equalsIgnoreCase("clvm")) {
            uuid = UUID.randomUUID().toString();
        } else if (scheme.equalsIgnoreCase("PreSetup")) {
            uuid = hostPath.replace("/", "");
        } else {
            uuid = UUID.nameUUIDFromBytes(new String(storageHost + hostPath).getBytes()).toString();
        }

        List<StoragePoolVO> spHandles = _storagePoolDao.findIfDuplicatePoolsExistByUUID(uuid);
        if ((spHandles != null) && (spHandles.size() > 0)) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Another active pool with the same uuid already exists");
            }
            throw new ResourceInUseException("Another active pool with the same uuid already exists");
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("In createPool Setting poolId - " + poolId + " uuid - " + uuid + " zoneId - " + zoneId + " podId - " + podId + " poolName - " + cmd.getStoragePoolName());
        }

        pool.setId(poolId);
        pool.setUuid(uuid);
        pool.setDataCenterId(cmd.getZoneId());
        pool.setPodId(podId);
        pool.setName(cmd.getStoragePoolName());
        pool.setClusterId(clusterId);
        pool.setStatus(StoragePoolStatus.Up);
        pool = _storagePoolDao.persist(pool, details);

        if (pool.getPoolType() == StoragePoolType.OCFS2 && !_ocfs2Mgr.prepareNodes(allHosts, pool)) {
            s_logger.warn("Can not create storage pool " + pool + " on cluster " + clusterId);
            _storagePoolDao.expunge(pool.getId());
            return null;
        }

        boolean success = false;
        for (HostVO h : allHosts) {
            success = createStoragePool(h.getId(), pool);
            if (success) {
                break;
            }
        }
        if (!success) {
            s_logger.warn("Can not create storage pool " + pool + " on cluster " + clusterId);
            _storagePoolDao.expunge(pool.getId());
            return null;
        }
        s_logger.debug("In createPool Adding the pool to each of the hosts");
        List<HostVO> poolHosts = new ArrayList<HostVO>();
        for (HostVO h : allHosts) {
            try {
                connectHostToSharedPool(h.getId(), pool);
                poolHosts.add(h);
            } catch (Exception e) {
                s_logger.warn("Unable to establish a connection between " + h + " and " + pool, e);
            }
        }

        if (poolHosts.isEmpty()) {
            s_logger.warn("No host can access storage pool " + pool + " on cluster " + clusterId);
            _storagePoolDao.expunge(pool.getId());
            return null;
        } else {
            createCapacityEntry(pool);
        }
        return pool;
    }

    @Override
    public StoragePoolVO updateStoragePool(UpdateStoragePoolCmd cmd) throws IllegalArgumentException {
        // Input validation
        Long id = cmd.getId();
        List<String> tags = cmd.getTags();

        StoragePoolVO pool = _storagePoolDao.findById(id);
        if (pool == null) {
            throw new IllegalArgumentException("Unable to find storage pool with ID: " + id);
        }

        if (tags != null) {
            Map<String, String> details = new HashMap<String, String>();
            for (String tag : tags) {
                tag = tag.trim();
                if (tag.length() > 0 && !details.containsKey(tag)) {
                    details.put(tag, "true");
                }
            }

            _storagePoolDao.updateDetails(id, details);
        }

        return pool;
    }

    @Override
    @DB
    public boolean deletePool(DeletePoolCmd command) {
        Long id = command.getId();
        boolean deleteFlag = false;

        // verify parameters
        StoragePoolVO sPool = _storagePoolDao.findById(id);
        if (sPool == null) {
            s_logger.warn("Unable to find pool:" + id);
            throw new InvalidParameterValueException("Unable to find pool by id " + id);
        }

        if (sPool.getPoolType().equals(StoragePoolType.LVM) || sPool.getPoolType().equals(StoragePoolType.EXT)) {
            s_logger.warn("Unable to delete local storage id:" + id);
            throw new InvalidParameterValueException("Unable to delete local storage id: " + id);
        }

        // Check if the pool has associated volumes in the volumes table
        // If it does, then you cannot delete the pool
        Pair<Long, Long> volumeRecords = _volsDao.getCountAndTotalByPool(id);

        if (volumeRecords.first() > 0) {
            s_logger.warn("Cannot delete pool " + sPool.getName() + " as there are associated vols for this pool");
            return false; // cannot delete as there are associated vols
        }

        // First get the host_id from storage_pool_host_ref for given pool id
        StoragePoolVO lock = _storagePoolDao.acquireInLockTable(sPool.getId());

        if (lock == null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Failed to acquire lock when deleting StoragePool with ID: " + sPool.getId());
            }
            return false;
        }

        // mark storage pool as removed (so it can't be used for new volumes creation), release the lock
        boolean isLockReleased = false;
        sPool.setStatus(StoragePoolStatus.Removed);
        _storagePoolDao.update(id, sPool);
        isLockReleased = _storagePoolDao.releaseFromLockTable(lock.getId());
        s_logger.trace("Released lock for storage pool " + id);

        // for the given pool id, find all records in the storage_pool_host_ref
        List<StoragePoolHostVO> hostPoolRecords = _storagePoolHostDao.listByPoolId(id);
        Transaction txn = Transaction.currentTxn();
        try {
            // if not records exist, delete the given pool (base case)
            if (hostPoolRecords.size() == 0) {

                txn.start();
                sPool.setUuid(null);
                _storagePoolDao.update(id, sPool);
                _storagePoolDao.remove(id);
                deletePoolStats(id);
                txn.commit();

                deleteFlag = true;
                return true;
            } else {
                // Remove the SR associated with the Xenserver
                for (StoragePoolHostVO host : hostPoolRecords) {
                    DeleteStoragePoolCommand cmd = new DeleteStoragePoolCommand(sPool);
                    final Answer answer = _agentMgr.easySend(host.getHostId(), cmd);

                    if (answer != null && answer.getResult()) {
                        deleteFlag = true;
                        break;
                    }
                }
            }
        } finally {
            if (deleteFlag) {
                // now delete the storage_pool_host_ref and storage_pool records
                txn.start();
                for (StoragePoolHostVO host : hostPoolRecords) {
                    _storagePoolHostDao.deleteStoragePoolHostDetails(host.getHostId(), host.getPoolId());
                }
                sPool.setUuid(null);
                _storagePoolDao.update(id, sPool);
                _storagePoolDao.remove(id);
                deletePoolStats(id);
                // Delete op_host_capacity entries
                _capacityDao.removeBy(Capacity.CAPACITY_TYPE_STORAGE_ALLOCATED, null, null, null, id);
                txn.commit();

                s_logger.debug("Storage pool id=" + id + " is removed successfully");
                return true;
            } else {
                // alert that the storage cleanup is required
                s_logger.warn("Failed to Delete storage pool id: " + id);
                _alertMgr.sendAlert(AlertManager.ALERT_TYPE_STORAGE_DELETE, sPool.getDataCenterId(), sPool.getPodId(), "Unable to delete storage pool id= " + id,
                        "Delete storage pool command failed.  Please check logs.");
            }

            if (lock != null && !isLockReleased) {
                _storagePoolDao.releaseFromLockTable(lock.getId());
            }
        }

        return false;

    }

    @DB
    private boolean deletePoolStats(Long poolId) {
        CapacityVO capacity1 = _capacityDao.findByHostIdType(poolId, CapacityVO.CAPACITY_TYPE_STORAGE);
        CapacityVO capacity2 = _capacityDao.findByHostIdType(poolId, CapacityVO.CAPACITY_TYPE_STORAGE_ALLOCATED);
        Transaction txn = Transaction.currentTxn();
        txn.start();
        if (capacity1 != null) {
            _capacityDao.remove(capacity1.getId());
        }

        if (capacity2 != null) {
            _capacityDao.remove(capacity2.getId());
        }

        txn.commit();
        return true;

    }

    @Override
    public boolean createStoragePool(long hostId, StoragePoolVO pool) {
        s_logger.debug("creating pool " + pool.getName() + " on  host " + hostId);
        if (pool.getPoolType() != StoragePoolType.NetworkFilesystem && pool.getPoolType() != StoragePoolType.Filesystem && pool.getPoolType() != StoragePoolType.IscsiLUN
                && pool.getPoolType() != StoragePoolType.Iscsi && pool.getPoolType() != StoragePoolType.VMFS && pool.getPoolType() != StoragePoolType.SharedMountPoint
                && pool.getPoolType() != StoragePoolType.PreSetup && pool.getPoolType() != StoragePoolType.OCFS2 && pool.getPoolType() != StoragePoolType.CLVM) {
            s_logger.warn(" Doesn't support storage pool type " + pool.getPoolType());
            return false;
        }
        CreateStoragePoolCommand cmd = new CreateStoragePoolCommand(true, pool);
        final Answer answer = _agentMgr.easySend(hostId, cmd);
        if (answer != null && answer.getResult()) {
            return true;
        } else {
            _storagePoolDao.expunge(pool.getId());
            String msg = "";
            if (answer != null) {
                msg = "Can not create storage pool through host " + hostId + " due to " + answer.getDetails();
                s_logger.warn(msg);
            } else {
                msg = "Can not create storage pool through host " + hostId + " due to CreateStoragePoolCommand returns null";
                s_logger.warn(msg);
            }
            throw new CloudRuntimeException(msg);
        }
    }

    @Override
    public boolean delPoolFromHost(long hostId) {
        List<StoragePoolHostVO> poolHosts = _poolHostDao.listByHostIdIncludingRemoved(hostId);
        for (StoragePoolHostVO poolHost : poolHosts) {
            s_logger.debug("Deleting pool " + poolHost.getPoolId() + " from  host " + hostId);
            _poolHostDao.remove(poolHost.getId());
        }
        return true;
    }

    public void connectHostToSharedPool(long hostId, StoragePoolVO pool) throws StorageUnavailableException {
        assert (pool.getPoolType().isShared()) : "Now, did you actually read the name of this method?";
        s_logger.debug("Adding pool " + pool.getName() + " to  host " + hostId);

        ModifyStoragePoolCommand cmd = new ModifyStoragePoolCommand(true, pool);
        final Answer answer = _agentMgr.easySend(hostId, cmd);

        if (answer == null) {
            throw new StorageUnavailableException("Unable to get an answer to the modify storage pool command", pool.getId());
        }

        if (!answer.getResult()) {
            String msg = "Add host failed due to ModifyStoragePoolCommand failed" + answer.getDetails();
            _alertMgr.sendAlert(AlertManager.ALERT_TYPE_HOST, pool.getDataCenterId(), pool.getPodId(), msg, msg);
            throw new StorageUnavailableException("Unable establish connection from storage head to storage pool " + pool.getId() + " due to " + answer.getDetails(), pool.getId());
        }

        assert (answer instanceof ModifyStoragePoolAnswer) : "Well, now why won't you actually return the ModifyStoragePoolAnswer when it's ModifyStoragePoolCommand? Pool=" + pool.getId() + "Host=" + hostId;
        ModifyStoragePoolAnswer mspAnswer = (ModifyStoragePoolAnswer) answer;

        StoragePoolHostVO poolHost = _poolHostDao.findByPoolHost(pool.getId(), hostId);
        if (poolHost == null) {
            poolHost = new StoragePoolHostVO(pool.getId(), hostId, mspAnswer.getPoolInfo().getLocalPath().replaceAll("//", "/"));
            _poolHostDao.persist(poolHost);
        } else {
            poolHost.setLocalPath(mspAnswer.getPoolInfo().getLocalPath().replaceAll("//", "/"));
        }
        pool.setAvailableBytes(mspAnswer.getPoolInfo().getAvailableBytes());
        pool.setCapacityBytes(mspAnswer.getPoolInfo().getCapacityBytes());
        _storagePoolDao.update(pool.getId(), pool);

        s_logger.info("Connection established between " + pool + " host + " + hostId);
    }

    @Override
    public VolumeVO moveVolume(VolumeVO volume, long destPoolDcId, Long destPoolPodId, Long destPoolClusterId, HypervisorType dataDiskHyperType) throws ConcurrentOperationException {

        // Find a destination storage pool with the specified criteria
        DiskOfferingVO diskOffering = _diskOfferingDao.findById(volume.getDiskOfferingId());
        DiskProfile dskCh = new DiskProfile(volume.getId(), volume.getVolumeType(), volume.getName(), diskOffering.getId(), diskOffering.getDiskSize(), diskOffering.getTagsArray(),
                diskOffering.getUseLocalStorage(), diskOffering.isRecreatable(), null);
        dskCh.setHyperType(dataDiskHyperType);
        DataCenterVO destPoolDataCenter = _dcDao.findById(destPoolDcId);
        HostPodVO destPoolPod = _podDao.findById(destPoolPodId);
        StoragePoolVO destPool = findStoragePool(dskCh, destPoolDataCenter, destPoolPod, destPoolClusterId, null, new HashSet<StoragePool>());
        String secondaryStorageURL = getSecondaryStorageURL(volume.getDataCenterId());

        if (destPool == null) {
            throw new CloudRuntimeException("Failed to find a storage pool with enough capacity to move the volume to.");
        }
        if (secondaryStorageURL == null) {
            throw new CloudRuntimeException("Failed to find secondary storage.");
        }

        List<Volume> vols = new ArrayList<Volume>();
        vols.add(volume);
        migrateVolumes(vols, destPool);
        return _volsDao.findById(volume.getId());
    }

    /*
     * Just allocate a volume in the database, don't send the createvolume cmd to hypervisor. The volume will be finally
     * created
     * only when it's attached to a VM.
     */
    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_VOLUME_CREATE, eventDescription = "creating volume", create = true)
    public VolumeVO allocVolume(CreateVolumeCmd cmd) throws ResourceAllocationException {
        // FIXME: some of the scheduled event stuff might be missing here...
        Account caller = UserContext.current().getCaller();

        long ownerId = cmd.getEntityOwnerId();

        // permission check
        _accountMgr.checkAccess(caller, null, true, _accountMgr.getActiveAccountById(ownerId));

        // Check that the resource limit for volumes won't be exceeded
        _resourceLimitMgr.checkResourceLimit(_accountMgr.getAccount(ownerId), ResourceType.volume);

        Long zoneId = cmd.getZoneId();
        Long diskOfferingId = null;
        Long size = null;

        // validate input parameters before creating the volume
        if ((cmd.getSnapshotId() == null && cmd.getDiskOfferingId() == null) || (cmd.getSnapshotId() != null && cmd.getDiskOfferingId() != null)) {
            throw new InvalidParameterValueException("Either disk Offering Id or snapshot Id must be passed whilst creating volume");
        }

        if (cmd.getSnapshotId() == null) {// create a new volume

            diskOfferingId = cmd.getDiskOfferingId();
            size = cmd.getSize();
            Long sizeInGB = size;
            if (size != null) {
                if (size > 0) {
                    size = size * 1024 * 1024 * 1024; // user specify size in GB
                } else {
                    throw new InvalidParameterValueException("Disk size must be larger than 0");
                }
            }
            if (diskOfferingId == null) {
                throw new InvalidParameterValueException("Missing parameter(s),either a positive volume size or a valid disk offering id must be specified.");
            }
            // Check that the the disk offering is specified
            DiskOfferingVO diskOffering = _diskOfferingDao.findById(diskOfferingId);
            if ((diskOffering == null) || diskOffering.getRemoved() != null || !DiskOfferingVO.Type.Disk.equals(diskOffering.getType())) {
                throw new InvalidParameterValueException("Please specify a valid disk offering.");
            }

            if (diskOffering.isCustomized()) {
                if (size == null) {
                    throw new InvalidParameterValueException("This disk offering requires a custom size specified");
                }
                if ((sizeInGB < _customDiskOfferingMinSize) || (sizeInGB > _customDiskOfferingMaxSize)) {
                    throw new InvalidParameterValueException("Volume size: " + sizeInGB + "GB is out of allowed range. Max: " + _customDiskOfferingMaxSize + " Min:" + _customDiskOfferingMinSize);
                }
            }

            if (!diskOffering.isCustomized() && size != null) {
                throw new InvalidParameterValueException("This disk offering does not allow custom size");
            }

            if (diskOffering.getDomainId() == null) {
                // do nothing as offering is public
            } else {
                _configMgr.checkDiskOfferingAccess(caller, diskOffering);
            }

            if (diskOffering.getDiskSize() > 0) {
                size = diskOffering.getDiskSize();
            }

            if (!validateVolumeSizeRange(size)) {// convert size from mb to gb for validation
                throw new InvalidParameterValueException("Invalid size for custom volume creation: " + size + " ,max volume size is:" + _maxVolumeSizeInGb);
            }
        } else { // create volume from snapshot
            Long snapshotId = cmd.getSnapshotId();
            SnapshotVO snapshotCheck = _snapshotDao.findById(snapshotId);
            if (snapshotCheck == null) {
                throw new InvalidParameterValueException("unable to find a snapshot with id " + snapshotId);
            }

            if (snapshotCheck.getStatus() != Snapshot.Status.BackedUp) {
                throw new InvalidParameterValueException("Snapshot id=" + snapshotId + " is not in " + Snapshot.Status.BackedUp + " state yet and can't be used for volume creation");
            }

            diskOfferingId = (cmd.getDiskOfferingId() != null) ? cmd.getDiskOfferingId() : snapshotCheck.getDiskOfferingId();
            zoneId = snapshotCheck.getDataCenterId();
            size = snapshotCheck.getSize(); // ; disk offering is used for tags purposes

            // check snapshot permissions
            _accountMgr.checkAccess(caller, null, true, snapshotCheck);

            /*
             * // bug #11428. Operation not supported if vmware and snapshots parent volume = ROOT
             * if(snapshotCheck.getHypervisorType() == HypervisorType.VMware
             * && _volumeDao.findByIdIncludingRemoved(snapshotCheck.getVolumeId()).getVolumeType() == Type.ROOT){
             * throw new UnsupportedServiceException("operation not supported, snapshot with id " + snapshotId +
             * " is created from ROOT volume");
             * }
             */
        }

        // Verify that zone exists
        DataCenterVO zone = _dcDao.findById(zoneId);
        if (zone == null) {
            throw new InvalidParameterValueException("Unable to find zone by id " + zoneId);
        }

        // Check if zone is disabled
        if (Grouping.AllocationState.Disabled == zone.getAllocationState() && !_accountMgr.isRootAdmin(caller.getType())) {
            throw new PermissionDeniedException("Cannot perform this operation, Zone is currently disabled: " + zoneId);
        }

        // Check that there is a shared primary storage pool in the specified zone
        List<StoragePoolVO> storagePools = _storagePoolDao.listByDataCenterId(zoneId);
        boolean sharedPoolExists = false;
        for (StoragePoolVO storagePool : storagePools) {
            if (storagePool.isShared()) {
                sharedPoolExists = true;
            }
        }

        // Check that there is at least one host in the specified zone
        List<HostVO> hosts = _resourceMgr.listAllUpAndEnabledHostsInOneZoneByType(Host.Type.Routing, zoneId);
        if (hosts.isEmpty()) {
            throw new InvalidParameterValueException("There is no workable host in data center id " + zoneId + ", please check hosts' agent status and see if they are disabled");
        }

        if (!sharedPoolExists) {
            throw new InvalidParameterValueException("Please specify a zone that has at least one shared primary storage pool.");
        }

        String userSpecifiedName = cmd.getVolumeName();
        if (userSpecifiedName == null) {
            userSpecifiedName = getRandomVolumeName();
        }

        Transaction txn = Transaction.currentTxn();
        txn.start();

        VolumeVO volume = new VolumeVO(userSpecifiedName, -1, -1, -1, -1, new Long(-1), null, null, 0, Volume.Type.DATADISK);
        volume.setPoolId(null);
        volume.setDataCenterId(zoneId);
        volume.setPodId(null);
        volume.setAccountId(ownerId);
        volume.setDomainId(((caller == null) ? Domain.ROOT_DOMAIN : caller.getDomainId()));
        volume.setDiskOfferingId(diskOfferingId);
        volume.setSize(size);
        volume.setInstanceId(null);
        volume.setUpdated(new Date());
        volume.setDomainId((caller == null) ? Domain.ROOT_DOMAIN : caller.getDomainId());

        volume = _volsDao.persist(volume);
        UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_VOLUME_CREATE, volume.getAccountId(), volume.getDataCenterId(), volume.getId(), volume.getName(), diskOfferingId, null, size);
        _usageEventDao.persist(usageEvent);

        UserContext.current().setEventDetails("Volume Id: " + volume.getId());

        // Increment resource count during allocation; if actual creation fails, decrement it
        _resourceLimitMgr.incrementResourceCount(volume.getAccountId(), ResourceType.volume);

        txn.commit();

        return volume;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_VOLUME_CREATE, eventDescription = "creating volume", async = true)
    public VolumeVO createVolume(CreateVolumeCmd cmd) {
        VolumeVO volume = _volsDao.findById(cmd.getEntityId());
        boolean created = false;

        try {
            if (cmd.getSnapshotId() != null) {
                volume = createVolumeFromSnapshot(volume, cmd.getSnapshotId());
                if (volume.getState() == Volume.State.Ready) {
                    created = true;
                }
                return volume;
            } else {
                _volsDao.update(volume.getId(), volume);
                created = true;
            }

            return _volsDao.findById(volume.getId());
        } finally {
            if (!created) {
                s_logger.trace("Decrementing volume resource count for account id=" + volume.getAccountId() + " as volume failed to create on the backend");
                _resourceLimitMgr.decrementResourceCount(volume.getAccountId(), ResourceType.volume);
            }
        }
    }

    @Override
    @DB
    public boolean destroyVolume(VolumeVO volume) throws ConcurrentOperationException {
        try {
            if (!stateTransitTo(volume, Volume.Event.DestroyRequested)) {
                throw new ConcurrentOperationException("Failed to transit to destroyed state");
            }
        } catch (NoTransitionException e) {
            s_logger.debug("Unable to destoy the volume: " + e.toString());
            return false;
        }

        long volumeId = volume.getId();

        // Delete the recurring snapshot policies for this volume.
        _snapshotMgr.deletePoliciesForVolume(volumeId);

        Long instanceId = volume.getInstanceId();
        VMInstanceVO vmInstance = null;
        if (instanceId != null) {
            vmInstance = _vmInstanceDao.findById(instanceId);
        }

        if (instanceId == null || (vmInstance.getType().equals(VirtualMachine.Type.User))) {
            // Decrement the resource count for volumes belonging user VM's only
            _resourceLimitMgr.decrementResourceCount(volume.getAccountId(), ResourceType.volume);
            // Log usage event for volumes belonging user VM's only
            UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_VOLUME_DELETE, volume.getAccountId(), volume.getDataCenterId(), volume.getId(), volume.getName());
            _usageEventDao.persist(usageEvent);
        }

        try {
            if (!stateTransitTo(volume, Volume.Event.OperationSucceeded)) {
                throw new ConcurrentOperationException("Failed to transit state");

            }
        } catch (NoTransitionException e) {
            s_logger.debug("Unable to change volume state: " + e.toString());
            return false;
        }

        return true;

    }

    @Override
    public void createCapacityEntry(StoragePoolVO storagePool) {
        createCapacityEntry(storagePool, Capacity.CAPACITY_TYPE_STORAGE_ALLOCATED, 0);
    }

    @Override
    public void createCapacityEntry(StoragePoolVO storagePool, short capacityType, long allocated) {
        SearchCriteria<CapacityVO> capacitySC = _capacityDao.createSearchCriteria();

        List<CapacityVO> capacities = _capacityDao.search(capacitySC, null);
        capacitySC = _capacityDao.createSearchCriteria();
        capacitySC.addAnd("hostOrPoolId", SearchCriteria.Op.EQ, storagePool.getId());
        capacitySC.addAnd("dataCenterId", SearchCriteria.Op.EQ, storagePool.getDataCenterId());
        capacitySC.addAnd("capacityType", SearchCriteria.Op.EQ, capacityType);

        capacities = _capacityDao.search(capacitySC, null);

        long totalOverProvCapacity;
        if (storagePool.getPoolType() == StoragePoolType.NetworkFilesystem) {
            totalOverProvCapacity = _overProvisioningFactor.multiply(new BigDecimal(storagePool.getCapacityBytes())).longValue();// All
// this for the inaccuracy of floats for big number multiplication.
        } else {
            totalOverProvCapacity = storagePool.getCapacityBytes();
        }

        if (capacities.size() == 0) {
            CapacityVO capacity = new CapacityVO(storagePool.getId(), storagePool.getDataCenterId(), storagePool.getPodId(), storagePool.getClusterId(), allocated, totalOverProvCapacity, capacityType);
            _capacityDao.persist(capacity);
        } else {
            CapacityVO capacity = capacities.get(0);
            boolean update = false;
            if (capacity.getTotalCapacity() != totalOverProvCapacity) {
                capacity.setTotalCapacity(totalOverProvCapacity);
                update = true;
            }
            if (allocated != 0) {
                capacity.setUsedCapacity(allocated);
                update = true;
            }
            if (update) {
                _capacityDao.update(capacity.getId(), capacity);
            }
        }
        s_logger.debug("Successfully set Capacity - " + totalOverProvCapacity + " for capacity type - " + capacityType + " , DataCenterId - "
                + storagePool.getDataCenterId() + ", HostOrPoolId - " + storagePool.getId() + ", PodId " + storagePool.getPodId());
    }

    @Override
    public List<Long> getUpHostsInPool(long poolId) {
        SearchCriteria<Long> sc = UpHostsInPoolSearch.create();
        sc.setParameters("pool", poolId);
        sc.setJoinParameters("hosts", "status", Status.Up);
        sc.setJoinParameters("hosts", "resourceState", ResourceState.Enabled);
        return _storagePoolHostDao.customSearch(sc, null);
    }

    @Override
    public Pair<Long, Answer[]> sendToPool(StoragePool pool, long[] hostIdsToTryFirst, List<Long> hostIdsToAvoid, Commands cmds) throws StorageUnavailableException {
        List<Long> hostIds = getUpHostsInPool(pool.getId());
        Collections.shuffle(hostIds);
        if (hostIdsToTryFirst != null) {
            for (int i = hostIdsToTryFirst.length - 1; i >= 0; i--) {
                if (hostIds.remove(hostIdsToTryFirst[i])) {
                    hostIds.add(0, hostIdsToTryFirst[i]);
                }
            }
        }

        if (hostIdsToAvoid != null) {
            hostIds.removeAll(hostIdsToAvoid);
        }
        if (hostIds == null || hostIds.isEmpty()) {
            throw new StorageUnavailableException("Unable to send command to the pool " + pool.getId() + " due to there is no enabled hosts up in this cluster", pool.getId());
        }
        for (Long hostId : hostIds) {
            try {
                List<Answer> answers = new ArrayList<Answer>();
                Command[] cmdArray = cmds.toCommands();
                for (Command cmd : cmdArray) {
                    long targetHostId = _hvGuruMgr.getGuruProcessedCommandTargetHost(hostId, cmd);

                    answers.add(_agentMgr.send(targetHostId, cmd));
                }
                return new Pair<Long, Answer[]>(hostId, answers.toArray(new Answer[answers.size()]));
            } catch (AgentUnavailableException e) {
                s_logger.debug("Unable to send storage pool command to " + pool + " via " + hostId, e);
            } catch (OperationTimedoutException e) {
                s_logger.debug("Unable to send storage pool command to " + pool + " via " + hostId, e);
            }
        }

        throw new StorageUnavailableException("Unable to send command to the pool ", pool.getId());
    }

    @Override
    public Pair<Long, Answer> sendToPool(StoragePool pool, long[] hostIdsToTryFirst, List<Long> hostIdsToAvoid, Command cmd) throws StorageUnavailableException {
        Commands cmds = new Commands(cmd);
        Pair<Long, Answer[]> result = sendToPool(pool, hostIdsToTryFirst, hostIdsToAvoid, cmds);
        return new Pair<Long, Answer>(result.first(), result.second()[0]);
    }

    @Override
    public void cleanupStorage(boolean recurring) {
        GlobalLock scanLock = GlobalLock.getInternLock("storagemgr.cleanup");

        try {
            if (scanLock.lock(3)) {
                try {
                    // Cleanup primary storage pools
                    if (_templateCleanupEnabled) {
                        List<StoragePoolVO> storagePools = _storagePoolDao.listAll();
                        for (StoragePoolVO pool : storagePools) {
                            try {

                                List<VMTemplateStoragePoolVO> unusedTemplatesInPool = _tmpltMgr.getUnusedTemplatesInPool(pool);
                                s_logger.debug("Storage pool garbage collector found " + unusedTemplatesInPool.size() + " templates to clean up in storage pool: " + pool.getName());
                                for (VMTemplateStoragePoolVO templatePoolVO : unusedTemplatesInPool) {
                                    if (templatePoolVO.getDownloadState() != VMTemplateStorageResourceAssoc.Status.DOWNLOADED) {
                                        s_logger.debug("Storage pool garbage collector is skipping templatePoolVO with ID: " + templatePoolVO.getId() + " because it is not completely downloaded.");
                                        continue;
                                    }

                                    if (!templatePoolVO.getMarkedForGC()) {
                                        templatePoolVO.setMarkedForGC(true);
                                        _vmTemplatePoolDao.update(templatePoolVO.getId(), templatePoolVO);
                                        s_logger.debug("Storage pool garbage collector has marked templatePoolVO with ID: " + templatePoolVO.getId() + " for garbage collection.");
                                        continue;
                                    }

                                    _tmpltMgr.evictTemplateFromStoragePool(templatePoolVO);
                                }
                            } catch (Exception e) {
                                s_logger.warn("Problem cleaning up primary storage pool " + pool, e);
                            }
                        }
                    }

                    cleanupSecondaryStorage(recurring);

                    List<VolumeVO> vols = _volsDao.listVolumesToBeDestroyed();
                    for (VolumeVO vol : vols) {
                        try {
                            expungeVolume(vol);
                        } catch (Exception e) {
                            s_logger.warn("Unable to destroy " + vol.getId(), e);
                        }
                    }
                } finally {
                    scanLock.unlock();
                }
            }
        } finally {
            scanLock.releaseRef();
        }
    }

    @DB
    List<Long> findAllVolumeIdInSnapshotTable(Long hostId) {
        String sql = "SELECT volume_id from snapshots WHERE sechost_id=? GROUP BY volume_id";
        List<Long> list = new ArrayList<Long>();
        try {
            Transaction txn = Transaction.currentTxn();
            ResultSet rs = null;
            PreparedStatement pstmt = null;
            pstmt = txn.prepareAutoCloseStatement(sql);
            pstmt.setLong(1, hostId);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(rs.getLong(1));
            }
            return list;
        } catch (Exception e) {
            s_logger.debug("failed to get all volumes who has snapshots in secondary storage " + hostId + " due to " + e.getMessage());
            return null;
        }

    }

    List<String> findAllSnapshotForVolume(Long volumeId) {
        String sql = "SELECT backup_snap_id FROM snapshots WHERE volume_id=? and backup_snap_id is not NULL";
        try {
            Transaction txn = Transaction.currentTxn();
            ResultSet rs = null;
            PreparedStatement pstmt = null;
            pstmt = txn.prepareAutoCloseStatement(sql);
            pstmt.setLong(1, volumeId);
            rs = pstmt.executeQuery();
            List<String> list = new ArrayList<String>();
            while (rs.next()) {
                list.add(rs.getString(1));
            }
            return list;
        } catch (Exception e) {
            s_logger.debug("failed to get all snapshots for a volume " + volumeId + " due to " + e.getMessage());
            return null;
        }
    }

    @Override
    @DB
    public void cleanupSecondaryStorage(boolean recurring) {
        try {
            // Cleanup templates in secondary storage hosts
            List<HostVO> secondaryStorageHosts = _ssvmMgr.listSecondaryStorageHostsInAllZones();
            for (HostVO secondaryStorageHost : secondaryStorageHosts) {
                try {
                    long hostId = secondaryStorageHost.getId();
                    List<VMTemplateHostVO> destroyedTemplateHostVOs = _vmTemplateHostDao.listDestroyed(hostId);
                    s_logger.debug("Secondary storage garbage collector found " + destroyedTemplateHostVOs.size() + " templates to cleanup on secondary storage host: "
                            + secondaryStorageHost.getName());
                    for (VMTemplateHostVO destroyedTemplateHostVO : destroyedTemplateHostVOs) {
                        if (!_tmpltMgr.templateIsDeleteable(destroyedTemplateHostVO)) {
                            if (s_logger.isDebugEnabled()) {
                                s_logger.debug("Not deleting template at: " + destroyedTemplateHostVO);
                            }
                            continue;
                        }

                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Deleting template host: " + destroyedTemplateHostVO);
                        }

                        String installPath = destroyedTemplateHostVO.getInstallPath();

                        if (installPath != null) {
                            Answer answer = _agentMgr.sendToSecStorage(secondaryStorageHost, new DeleteTemplateCommand(secondaryStorageHost.getStorageUrl(), destroyedTemplateHostVO.getInstallPath()));

                            if (answer == null || !answer.getResult()) {
                                s_logger.debug("Failed to delete " + destroyedTemplateHostVO + " due to " + ((answer == null) ? "answer is null" : answer.getDetails()));
                            } else {
                                _vmTemplateHostDao.remove(destroyedTemplateHostVO.getId());
                                s_logger.debug("Deleted template at: " + destroyedTemplateHostVO.getInstallPath());
                            }
                        } else {
                            _vmTemplateHostDao.remove(destroyedTemplateHostVO.getId());
                        }
                    }
                } catch (Exception e) {
                    s_logger.warn("problem cleaning up templates in secondary storage " + secondaryStorageHost, e);
                }
            }

            // Cleanup snapshot in secondary storage hosts
            for (HostVO secondaryStorageHost : secondaryStorageHosts) {
                try {
                    long hostId = secondaryStorageHost.getId();
                    List<Long> vIDs = findAllVolumeIdInSnapshotTable(hostId);
                    if (vIDs == null) {
                        continue;
                    }
                    for (Long volumeId : vIDs) {
                        boolean lock = false;
                        try {
                            VolumeVO volume = _volsDao.findByIdIncludingRemoved(volumeId);
                            if (volume.getRemoved() == null) {
                                volume = _volsDao.acquireInLockTable(volumeId, 10);
                                if (volume == null) {
                                    continue;
                                }
                                lock = true;
                            }
                            List<String> snapshots = findAllSnapshotForVolume(volumeId);
                            if (snapshots == null) {
                                continue;
                            }
                            CleanupSnapshotBackupCommand cmd = new CleanupSnapshotBackupCommand(secondaryStorageHost.getStorageUrl(), secondaryStorageHost.getDataCenterId(), volume.getAccountId(),
                                    volumeId, snapshots);

                            Answer answer = _agentMgr.sendToSecStorage(secondaryStorageHost, cmd);
                            if ((answer == null) || !answer.getResult()) {
                                String details = "Failed to cleanup snapshots for volume " + volumeId + " due to " + (answer == null ? "null" : answer.getDetails());
                                s_logger.warn(details);
                            }
                        } catch (Exception e1) {
                            s_logger.warn("problem cleaning up snapshots in secondary storage " + secondaryStorageHost, e1);
                        } finally {
                            if (lock) {
                                _volsDao.releaseFromLockTable(volumeId);
                            }
                        }
                    }
                } catch (Exception e2) {
                    s_logger.warn("problem cleaning up snapshots in secondary storage " + secondaryStorageHost, e2);
                }
            }

        } catch (Exception e3) {
            s_logger.warn("problem cleaning up secondary storage ", e3);
        }
    }

    @Override
    public String getPrimaryStorageNameLabel(VolumeVO volume) {
        Long poolId = volume.getPoolId();

        // poolId is null only if volume is destroyed, which has been checked before.
        assert poolId != null;
        StoragePoolVO storagePoolVO = _storagePoolDao.findById(poolId);
        assert storagePoolVO != null;
        return storagePoolVO.getUuid();
    }

    @Override
    @DB
    public StoragePoolVO preparePrimaryStorageForMaintenance(Long primaryStorageId) throws ResourceUnavailableException, InsufficientCapacityException {
        Long userId = UserContext.current().getCallerUserId();
        User user = _userDao.findById(userId);
        Account account = UserContext.current().getCaller();
        boolean restart = true;
        StoragePoolVO primaryStorage = null;
        try {
            // 1. Get the primary storage record and perform validation check
            primaryStorage = _storagePoolDao.lockRow(primaryStorageId, true);

            if (primaryStorage == null) {
                String msg = "Unable to obtain lock on the storage pool record in preparePrimaryStorageForMaintenance()";
                s_logger.error(msg);
                throw new ExecutionException(msg);
            }

            List<StoragePoolVO> spes = _storagePoolDao.listBy(primaryStorage.getDataCenterId(), primaryStorage.getPodId(), primaryStorage.getClusterId());
            for (StoragePoolVO sp : spes) {
                if (sp.getStatus() == StoragePoolStatus.PrepareForMaintenance) {
                    throw new CloudRuntimeException("Only one storage pool in a cluster can be in PrepareForMaintenance mode, " + sp.getId() + " is already in  PrepareForMaintenance mode ");
                }
            }

            if (!primaryStorage.getStatus().equals(StoragePoolStatus.Up) && !primaryStorage.getStatus().equals(StoragePoolStatus.ErrorInMaintenance)) {
                throw new InvalidParameterValueException("Primary storage with id " + primaryStorageId + " is not ready for migration, as the status is:" + primaryStorage.getStatus().toString());
            }

            List<HostVO> hosts = _resourceMgr.listHostsInClusterByStatus(primaryStorage.getClusterId(), Status.Up);
            if (hosts == null || hosts.size() == 0) {
                primaryStorage.setStatus(StoragePoolStatus.Maintenance);
                _storagePoolDao.update(primaryStorageId, primaryStorage);
                return _storagePoolDao.findById(primaryStorageId);
            } else {
                // set the pool state to prepare for maintenance
                primaryStorage.setStatus(StoragePoolStatus.PrepareForMaintenance);
                _storagePoolDao.update(primaryStorageId, primaryStorage);
            }
            // remove heartbeat
            for (HostVO host : hosts) {
                ModifyStoragePoolCommand cmd = new ModifyStoragePoolCommand(false, primaryStorage);
                final Answer answer = _agentMgr.easySend(host.getId(), cmd);
                if (answer == null || !answer.getResult()) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("ModifyStoragePool false failed due to " + ((answer == null) ? "answer null" : answer.getDetails()));
                    }
                } else {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("ModifyStoragePool false secceeded");
                    }
                }
            }
            // check to see if other ps exist
            // if they do, then we can migrate over the system vms to them
            // if they dont, then just stop all vms on this one
            List<StoragePoolVO> upPools = _storagePoolDao.listByStatusInZone(primaryStorage.getDataCenterId(), StoragePoolStatus.Up);

            if (upPools == null || upPools.size() == 0) {
                restart = false;
            }

            // 2. Get a list of all the ROOT volumes within this storage pool
            List<VolumeVO> allVolumes = _volsDao.findByPoolId(primaryStorageId);

            // 3. Enqueue to the work queue
            for (VolumeVO volume : allVolumes) {
                VMInstanceVO vmInstance = _vmInstanceDao.findById(volume.getInstanceId());

                if (vmInstance == null) {
                    continue;
                }

                // enqueue sp work
                if (vmInstance.getState().equals(State.Running) || vmInstance.getState().equals(State.Starting) || vmInstance.getState().equals(State.Stopping)) {

                    try {
                        StoragePoolWorkVO work = new StoragePoolWorkVO(vmInstance.getId(), primaryStorageId, false, false, _serverId);
                        _storagePoolWorkDao.persist(work);
                    } catch (Exception e) {
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Work record already exists, re-using by re-setting values");
                        }
                        StoragePoolWorkVO work = _storagePoolWorkDao.findByPoolIdAndVmId(primaryStorageId, vmInstance.getId());
                        work.setStartedAfterMaintenance(false);
                        work.setStoppedForMaintenance(false);
                        work.setManagementServerId(_serverId);
                        _storagePoolWorkDao.update(work.getId(), work);
                    }
                }
            }

            // 4. Process the queue
            List<StoragePoolWorkVO> pendingWork = _storagePoolWorkDao.listPendingWorkForPrepareForMaintenanceByPoolId(primaryStorageId);

            for (StoragePoolWorkVO work : pendingWork) {
                // shut down the running vms
                VMInstanceVO vmInstance = _vmInstanceDao.findById(work.getVmId());

                if (vmInstance == null) {
                    continue;
                }

                // if the instance is of type consoleproxy, call the console proxy
                if (vmInstance.getType().equals(VirtualMachine.Type.ConsoleProxy)) {
                    // call the consoleproxymanager
                    ConsoleProxyVO consoleProxy = _consoleProxyDao.findById(vmInstance.getId());
                    if (!_vmMgr.advanceStop(consoleProxy, true, user, account)) {
                        String errorMsg = "There was an error stopping the console proxy id: " + vmInstance.getId() + " ,cannot enable storage maintenance";
                        s_logger.warn(errorMsg);
                        throw new CloudRuntimeException(errorMsg);
                    } else {
                        // update work status
                        work.setStoppedForMaintenance(true);
                        _storagePoolWorkDao.update(work.getId(), work);
                    }

                    if (restart) {

                        if (_vmMgr.advanceStart(consoleProxy, null, user, account) == null) {
                            String errorMsg = "There was an error starting the console proxy id: " + vmInstance.getId() + " on another storage pool, cannot enable primary storage maintenance";
                            s_logger.warn(errorMsg);
                        } else {
                            // update work status
                            work.setStartedAfterMaintenance(true);
                            _storagePoolWorkDao.update(work.getId(), work);
                        }
                    }
                }

                // if the instance is of type uservm, call the user vm manager
                if (vmInstance.getType().equals(VirtualMachine.Type.User)) {
                    UserVmVO userVm = _userVmDao.findById(vmInstance.getId());
                    if (!_vmMgr.advanceStop(userVm, true, user, account)) {
                        String errorMsg = "There was an error stopping the user vm id: " + vmInstance.getId() + " ,cannot enable storage maintenance";
                        s_logger.warn(errorMsg);
                        throw new CloudRuntimeException(errorMsg);
                    } else {
                        // update work status
                        work.setStoppedForMaintenance(true);
                        _storagePoolWorkDao.update(work.getId(), work);
                    }
                }

                // if the instance is of type secondary storage vm, call the secondary storage vm manager
                if (vmInstance.getType().equals(VirtualMachine.Type.SecondaryStorageVm)) {
                    SecondaryStorageVmVO secStrgVm = _secStrgDao.findById(vmInstance.getId());
                    if (!_vmMgr.advanceStop(secStrgVm, true, user, account)) {
                        String errorMsg = "There was an error stopping the ssvm id: " + vmInstance.getId() + " ,cannot enable storage maintenance";
                        s_logger.warn(errorMsg);
                        throw new CloudRuntimeException(errorMsg);
                    } else {
                        // update work status
                        work.setStoppedForMaintenance(true);
                        _storagePoolWorkDao.update(work.getId(), work);
                    }

                    if (restart) {
                        if (_vmMgr.advanceStart(secStrgVm, null, user, account) == null) {
                            String errorMsg = "There was an error starting the ssvm id: " + vmInstance.getId() + " on another storage pool, cannot enable primary storage maintenance";
                            s_logger.warn(errorMsg);
                        } else {
                            // update work status
                            work.setStartedAfterMaintenance(true);
                            _storagePoolWorkDao.update(work.getId(), work);
                        }
                    }
                }

                // if the instance is of type domain router vm, call the network manager
                if (vmInstance.getType().equals(VirtualMachine.Type.DomainRouter)) {
                    DomainRouterVO domR = _domrDao.findById(vmInstance.getId());
                    if (!_vmMgr.advanceStop(domR, true, user, account)) {
                        String errorMsg = "There was an error stopping the domain router id: " + vmInstance.getId() + " ,cannot enable primary storage maintenance";
                        s_logger.warn(errorMsg);
                        throw new CloudRuntimeException(errorMsg);
                    } else {
                        // update work status
                        work.setStoppedForMaintenance(true);
                        _storagePoolWorkDao.update(work.getId(), work);
                    }

                    if (restart) {
                        if (_vmMgr.advanceStart(domR, null, user, account) == null) {
                            String errorMsg = "There was an error starting the domain router id: " + vmInstance.getId() + " on another storage pool, cannot enable primary storage maintenance";
                            s_logger.warn(errorMsg);
                        } else {
                            // update work status
                            work.setStartedAfterMaintenance(true);
                            _storagePoolWorkDao.update(work.getId(), work);
                        }
                    }
                }
            }

            // 5. Update the status
            primaryStorage.setStatus(StoragePoolStatus.Maintenance);
            _storagePoolDao.update(primaryStorageId, primaryStorage);

            return _storagePoolDao.findById(primaryStorageId);
        } catch (Exception e) {
            if (e instanceof ExecutionException || e instanceof ResourceUnavailableException) {
                s_logger.error("Exception in enabling primary storage maintenance:", e);
                setPoolStateToError(primaryStorage);
                throw (ResourceUnavailableException) e;
            }
            if (e instanceof InvalidParameterValueException) {
                s_logger.error("Exception in enabling primary storage maintenance:", e);
                setPoolStateToError(primaryStorage);
                throw (InvalidParameterValueException) e;
            }
            if (e instanceof InsufficientCapacityException) {
                s_logger.error("Exception in enabling primary storage maintenance:", e);
                setPoolStateToError(primaryStorage);
                throw (InsufficientCapacityException) e;
            }
            // for everything else
            s_logger.error("Exception in enabling primary storage maintenance:", e);
            setPoolStateToError(primaryStorage);
            throw new CloudRuntimeException(e.getMessage());

        }
    }

    private void setPoolStateToError(StoragePoolVO primaryStorage) {
        primaryStorage.setStatus(StoragePoolStatus.ErrorInMaintenance);
        _storagePoolDao.update(primaryStorage.getId(), primaryStorage);
    }

    @Override
    @DB
    public StoragePoolVO cancelPrimaryStorageForMaintenance(CancelPrimaryStorageMaintenanceCmd cmd) throws ResourceUnavailableException {
        Long primaryStorageId = cmd.getId();
        Long userId = UserContext.current().getCallerUserId();
        User user = _userDao.findById(userId);
        Account account = UserContext.current().getCaller();
        StoragePoolVO primaryStorage = null;
        try {
            Transaction txn = Transaction.currentTxn();
            txn.start();
            // 1. Get the primary storage record and perform validation check
            primaryStorage = _storagePoolDao.lockRow(primaryStorageId, true);

            if (primaryStorage == null) {
                String msg = "Unable to obtain lock on the storage pool in cancelPrimaryStorageForMaintenance()";
                s_logger.error(msg);
                throw new ExecutionException(msg);
            }

            if (primaryStorage.getStatus().equals(StoragePoolStatus.Up) || primaryStorage.getStatus().equals(StoragePoolStatus.PrepareForMaintenance)) {
                throw new StorageUnavailableException("Primary storage with id " + primaryStorageId + " is not ready to complete migration, as the status is:" + primaryStorage.getStatus().toString(),
                        primaryStorageId);
            }

            // Change the storage state back to up
            primaryStorage.setStatus(StoragePoolStatus.Up);
            _storagePoolDao.update(primaryStorageId, primaryStorage);
            txn.commit();
            List<HostVO> hosts = _resourceMgr.listHostsInClusterByStatus(primaryStorage.getClusterId(), Status.Up);
            if (hosts == null || hosts.size() == 0) {
                return _storagePoolDao.findById(primaryStorageId);
            }
            // add heartbeat
            for (HostVO host : hosts) {
                ModifyStoragePoolCommand msPoolCmd = new ModifyStoragePoolCommand(true, primaryStorage);
                final Answer answer = _agentMgr.easySend(host.getId(), msPoolCmd);
                if (answer == null || !answer.getResult()) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("ModifyStoragePool add failed due to " + ((answer == null) ? "answer null" : answer.getDetails()));
                    }
                } else {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("ModifyStoragePool add secceeded");
                    }
                }
            }

            // 2. Get a list of pending work for this queue
            List<StoragePoolWorkVO> pendingWork = _storagePoolWorkDao.listPendingWorkForCancelMaintenanceByPoolId(primaryStorageId);

            // 3. work through the queue
            for (StoragePoolWorkVO work : pendingWork) {

                VMInstanceVO vmInstance = _vmInstanceDao.findById(work.getVmId());

                if (vmInstance == null) {
                    continue;
                }

                // if the instance is of type consoleproxy, call the console proxy
                if (vmInstance.getType().equals(VirtualMachine.Type.ConsoleProxy)) {

                    ConsoleProxyVO consoleProxy = _consoleProxyDao.findById(vmInstance.getId());
                    if (_vmMgr.advanceStart(consoleProxy, null, user, account) == null) {
                        String msg = "There was an error starting the console proxy id: " + vmInstance.getId() + " on storage pool, cannot complete primary storage maintenance";
                        s_logger.warn(msg);
                        throw new ExecutionException(msg);
                    } else {
                        // update work queue
                        work.setStartedAfterMaintenance(true);
                        _storagePoolWorkDao.update(work.getId(), work);
                    }
                }

                // if the instance is of type ssvm, call the ssvm manager
                if (vmInstance.getType().equals(VirtualMachine.Type.SecondaryStorageVm)) {
                    SecondaryStorageVmVO ssVm = _secStrgDao.findById(vmInstance.getId());
                    if (_vmMgr.advanceStart(ssVm, null, user, account) == null) {
                        String msg = "There was an error starting the ssvm id: " + vmInstance.getId() + " on storage pool, cannot complete primary storage maintenance";
                        s_logger.warn(msg);
                        throw new ExecutionException(msg);
                    } else {
                        // update work queue
                        work.setStartedAfterMaintenance(true);
                        _storagePoolWorkDao.update(work.getId(), work);
                    }
                }

                // if the instance is of type ssvm, call the ssvm manager
                if (vmInstance.getType().equals(VirtualMachine.Type.DomainRouter)) {
                    DomainRouterVO domR = _domrDao.findById(vmInstance.getId());
                    if (_vmMgr.advanceStart(domR, null, user, account) == null) {
                        String msg = "There was an error starting the domR id: " + vmInstance.getId() + " on storage pool, cannot complete primary storage maintenance";
                        s_logger.warn(msg);
                        throw new ExecutionException(msg);
                    } else {
                        // update work queue
                        work.setStartedAfterMaintenance(true);
                        _storagePoolWorkDao.update(work.getId(), work);
                    }
                }

                // if the instance is of type user vm, call the user vm manager
                if (vmInstance.getType().equals(VirtualMachine.Type.User)) {
                    UserVmVO userVm = _userVmDao.findById(vmInstance.getId());
                    try {
                        if (_vmMgr.advanceStart(userVm, null, user, account) == null) {

                            String msg = "There was an error starting the user vm id: " + vmInstance.getId() + " on storage pool, cannot complete primary storage maintenance";
                            s_logger.warn(msg);
                            throw new ExecutionException(msg);
                        } else {
                            // update work queue
                            work.setStartedAfterMaintenance(true);
                            _storagePoolWorkDao.update(work.getId(), work);
                        }
                    } catch (StorageUnavailableException e) {
                        String msg = "There was an error starting the user vm id: " + vmInstance.getId() + " on storage pool, cannot complete primary storage maintenance";
                        s_logger.warn(msg, e);
                        throw new ExecutionException(msg);
                    } catch (InsufficientCapacityException e) {
                        String msg = "There was an error starting the user vm id: " + vmInstance.getId() + " on storage pool, cannot complete primary storage maintenance";
                        s_logger.warn(msg, e);
                        throw new ExecutionException(msg);
                    } catch (ConcurrentOperationException e) {
                        String msg = "There was an error starting the user vm id: " + vmInstance.getId() + " on storage pool, cannot complete primary storage maintenance";
                        s_logger.warn(msg, e);
                        throw new ExecutionException(msg);
                    } catch (ExecutionException e) {
                        String msg = "There was an error starting the user vm id: " + vmInstance.getId() + " on storage pool, cannot complete primary storage maintenance";
                        s_logger.warn(msg, e);
                        throw new ExecutionException(msg);
                    }
                }
            }
            return primaryStorage;
        } catch (Exception e) {
            setPoolStateToError(primaryStorage);
            if (e instanceof ExecutionException) {
                throw (ResourceUnavailableException) e;
            } else if (e instanceof InvalidParameterValueException) {
                throw (InvalidParameterValueException) e;
            } else {// all other exceptions
                throw new CloudRuntimeException(e.getMessage());
            }
        }
    }

    private boolean sendToVmResidesOn(StoragePoolVO storagePool, Command cmd) {
        ClusterVO cluster = _clusterDao.findById(storagePool.getClusterId());
        if ((cluster.getHypervisorType() == HypervisorType.KVM || cluster.getHypervisorType() == HypervisorType.VMware)
                && ((cmd instanceof ManageSnapshotCommand) || (cmd instanceof BackupSnapshotCommand))) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isAdmin(short accountType) {
        return ((accountType == Account.ACCOUNT_TYPE_ADMIN) || (accountType == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) || (accountType == Account.ACCOUNT_TYPE_READ_ONLY_ADMIN));
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_VOLUME_DELETE, eventDescription = "deleting volume")
    public boolean deleteVolume(long volumeId) throws ConcurrentOperationException {
        Account caller = UserContext.current().getCaller();

        // Check that the volume ID is valid
        VolumeVO volume = _volsDao.findById(volumeId);
        if (volume == null) {
            throw new InvalidParameterValueException("Unable to aquire volume with ID: " + volumeId);
        }

        if (!_snapshotMgr.canOperateOnVolume(volume)) {
            throw new InvalidParameterValueException("There are snapshot creating on it, Unable to delete the volume");
        }

        // permission check
        _accountMgr.checkAccess(caller, null, true, volume);

        // Check that the volume is not currently attached to any VM
        if (volume.getInstanceId() != null) {
            throw new InvalidParameterValueException("Please specify a volume that is not attached to any VM.");
        }

        // Check that the volume is not already destroyed
        if (volume.getState() != Volume.State.Destroy) {
            if (!destroyVolume(volume)) {
                return false;
            }
        }

        try {
            expungeVolume(volume);
        } catch (Exception e) {
            s_logger.warn("Failed to expunge volume:", e);
            return false;
        }

        return true;
    }

    private boolean validateVolumeSizeRange(long size) {
        if (size < 0 || (size > 0 && size < (1024 * 1024 * 1024))) {
            throw new InvalidParameterValueException("Please specify a size of at least 1 Gb.");
        } else if (size > (_maxVolumeSizeInGb * 1024 * 1024 * 1024)) {
            throw new InvalidParameterValueException("volume size " + size + ", but the maximum size allowed is " + _maxVolumeSizeInGb + " Gb.");
        }

        return true;
    }

    protected DiskProfile toDiskProfile(VolumeVO vol, DiskOfferingVO offering) {
        return new DiskProfile(vol.getId(), vol.getVolumeType(), vol.getName(), offering.getId(), vol.getSize(), offering.getTagsArray(), offering.getUseLocalStorage(), offering.isRecreatable(),
                vol.getTemplateId());
    }

    @Override
    public <T extends VMInstanceVO> DiskProfile allocateRawVolume(Type type, String name, DiskOfferingVO offering, Long size, T vm, Account owner) {
        if (size == null) {
            size = offering.getDiskSize();
        } else {
            size = (size * 1024 * 1024 * 1024);
        }
        VolumeVO vol = new VolumeVO(type, name, vm.getDataCenterIdToDeployIn(), owner.getDomainId(), owner.getId(), offering.getId(), size);
        if (vm != null) {
            vol.setInstanceId(vm.getId());
        }

        if (type.equals(Type.ROOT)) {
            vol.setDeviceId(0l);
        } else {
            vol.setDeviceId(1l);
        }

        vol = _volsDao.persist(vol);

        // Save usage event and update resource count for user vm volumes
        if (vm instanceof UserVm) {

            UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_VOLUME_CREATE, vol.getAccountId(), vol.getDataCenterId(), vol.getId(), vol.getName(), offering.getId(), null, size);
            _usageEventDao.persist(usageEvent);

            _resourceLimitMgr.incrementResourceCount(vm.getAccountId(), ResourceType.volume);
        }
        return toDiskProfile(vol, offering);
    }

    @Override
    public <T extends VMInstanceVO> DiskProfile allocateTemplatedVolume(Type type, String name, DiskOfferingVO offering, VMTemplateVO template, T vm, Account owner) {
        assert (template.getFormat() != ImageFormat.ISO) : "ISO is not a template really....";

        SearchCriteria<VMTemplateHostVO> sc = HostTemplateStatesSearch.create();
        sc.setParameters("id", template.getId());
        sc.setParameters("state", com.cloud.storage.VMTemplateStorageResourceAssoc.Status.DOWNLOADED);
        sc.setJoinParameters("host", "dcId", vm.getDataCenterIdToDeployIn());
        List<VMTemplateSwiftVO> tsvs = _vmTemplateSwiftDao.listByTemplateId(template.getId());
        Long size = null;
        if (tsvs != null && tsvs.size() > 0) {
            size = tsvs.get(0).getSize();
        }
        if (size == null) {
            List<VMTemplateHostVO> sss = _vmTemplateHostDao.search(sc, null);
            if (sss == null || sss.size() == 0) {
                throw new CloudRuntimeException("Template " + template.getName() + " has not been completely downloaded to zone " + vm.getDataCenterIdToDeployIn());
            }
            size = sss.get(0).getSize();
        }

        VolumeVO vol = new VolumeVO(type, name, vm.getDataCenterIdToDeployIn(), owner.getDomainId(), owner.getId(), offering.getId(), size);
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

            if (offering.getType() == DiskOfferingVO.Type.Disk) {
                offeringId = offering.getId();
            }

            UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_VOLUME_CREATE, vol.getAccountId(), vol.getDataCenterId(), vol.getId(), vol.getName(), offeringId, template.getId(),
                    vol.getSize());
            _usageEventDao.persist(usageEvent);

            _resourceLimitMgr.incrementResourceCount(vm.getAccountId(), ResourceType.volume);
        }
        return toDiskProfile(vol, offering);
    }

    @Override
    public void prepareForMigration(VirtualMachineProfile<? extends VirtualMachine> vm, DeployDestination dest) {
        List<VolumeVO> vols = _volsDao.findUsableVolumesForInstance(vm.getId());
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Preparing " + vols.size() + " volumes for " + vm);
        }

        for (VolumeVO vol : vols) {
            StoragePool pool = _storagePoolDao.findById(vol.getPoolId());
            vm.addDisk(new VolumeTO(vol, pool));
        }

        if (vm.getType() == VirtualMachine.Type.User) {
            UserVmVO userVM = (UserVmVO) vm.getVirtualMachine();
            if (userVM.getIsoId() != null) {
                Pair<String, String> isoPathPair = getAbsoluteIsoPath(userVM.getIsoId(), userVM.getDataCenterIdToDeployIn());
                if (isoPathPair != null) {
                    String isoPath = isoPathPair.first();
                    VolumeTO iso = new VolumeTO(vm.getId(), Volume.Type.ISO, StoragePoolType.ISO, null, null, null, isoPath, 0, null, null);
                    vm.addDisk(iso);
                }
            }
        }
    }

    @DB
    @Override
    public Volume migrateVolume(Long volumeId, Long storagePoolId) throws ConcurrentOperationException {
        VolumeVO vol = _volsDao.findById(volumeId);
        if (vol == null) {
            throw new InvalidParameterValueException("Failed to find the volume id: " + volumeId);
        }

        if (vol.getState() != Volume.State.Ready) {
            throw new InvalidParameterValueException("Volume must be in ready state");
        }

        if (vol.getInstanceId() != null) {
            throw new InvalidParameterValueException("Volume needs to be dettached from VM");
        }

        StoragePool destPool = _storagePoolDao.findById(storagePoolId);
        if (destPool == null) {
            throw new InvalidParameterValueException("Faild to find the destination storage pool: " + storagePoolId);
        }

        List<Volume> vols = new ArrayList<Volume>();
        vols.add(vol);

        migrateVolumes(vols, destPool);
        return vol;
    }

    @DB
    public boolean migrateVolumes(List<Volume> volumes, StoragePool destPool) throws ConcurrentOperationException {
        Transaction txn = Transaction.currentTxn();
        txn.start();

        boolean transitResult = false;
        long checkPointTaskId = -1;
        try {
            List<Long> volIds = new ArrayList<Long>();
            for (Volume volume : volumes) {
                if (!_snapshotMgr.canOperateOnVolume((VolumeVO) volume)) {
                    throw new CloudRuntimeException("There are snapshots creating on this volume, can not move this volume");
                }

                try {
                    if (!stateTransitTo(volume, Volume.Event.MigrationRequested)) {
                        throw new ConcurrentOperationException("Failed to transit volume state");
                    }
                } catch (NoTransitionException e) {
                    s_logger.debug("Failed to set state into migrate: " + e.toString());
                    throw new CloudRuntimeException("Failed to set state into migrate: " + e.toString());
                }
                volIds.add(volume.getId());
            }

            checkPointTaskId = _checkPointMgr.pushCheckPoint(new StorageMigrationCleanupMaid(StorageMigrationCleanupMaid.StorageMigrationState.MIGRATING, volIds));
            transitResult = true;
        } finally {
            if (!transitResult) {
                txn.rollback();
            } else {
                txn.commit();
            }
        }

        // At this stage, nobody can modify volumes. Send the copyvolume command
        List<Pair<StoragePoolVO, DestroyCommand>> destroyCmds = new ArrayList<Pair<StoragePoolVO, DestroyCommand>>();
        List<CopyVolumeAnswer> answers = new ArrayList<CopyVolumeAnswer>();
        try {
            for (Volume volume : volumes) {
                String secondaryStorageURL = getSecondaryStorageURL(volume.getDataCenterId());
                StoragePoolVO srcPool = _storagePoolDao.findById(volume.getPoolId());
                CopyVolumeCommand cvCmd = new CopyVolumeCommand(volume.getId(), volume.getPath(), srcPool, secondaryStorageURL, true, _copyvolumewait);
                CopyVolumeAnswer cvAnswer;
                try {
                    cvAnswer = (CopyVolumeAnswer) sendToPool(srcPool, cvCmd);
                } catch (StorageUnavailableException e1) {
                    throw new CloudRuntimeException("Failed to copy the volume from the source primary storage pool to secondary storage.", e1);
                }

                if (cvAnswer == null || !cvAnswer.getResult()) {
                    throw new CloudRuntimeException("Failed to copy the volume from the source primary storage pool to secondary storage.");
                }

                String secondaryStorageVolumePath = cvAnswer.getVolumePath();

                // Copy the volume from secondary storage to the destination storage
                // pool
                cvCmd = new CopyVolumeCommand(volume.getId(), secondaryStorageVolumePath, destPool, secondaryStorageURL, false, _copyvolumewait);
                try {
                    cvAnswer = (CopyVolumeAnswer) sendToPool(destPool, cvCmd);
                } catch (StorageUnavailableException e1) {
                    throw new CloudRuntimeException("Failed to copy the volume from secondary storage to the destination primary storage pool.");
                }

                if (cvAnswer == null || !cvAnswer.getResult()) {
                    throw new CloudRuntimeException("Failed to copy the volume from secondary storage to the destination primary storage pool.");
                }

                answers.add(cvAnswer);
                destroyCmds.add(new Pair<StoragePoolVO, DestroyCommand>(srcPool, new DestroyCommand(srcPool, volume, null)));
            }
        } finally {
            if (answers.size() != volumes.size()) {
                // this means one of copying volume failed
                for (Volume volume : volumes) {
                    try {
                        stateTransitTo(volume, Volume.Event.OperationFailed);
                    } catch (NoTransitionException e) {
                        s_logger.debug("Failed to change volume state: " + e.toString());
                    }
                }
                _checkPointMgr.popCheckPoint(checkPointTaskId);
            } else {
                // Need a transaction, make sure all the volumes get migrated to new storage pool
                txn = Transaction.currentTxn();
                txn.start();

                transitResult = false;
                try {
                    for (int i = 0; i < volumes.size(); i++) {
                        CopyVolumeAnswer answer = answers.get(i);
                        VolumeVO volume = (VolumeVO) volumes.get(i);
                        Long oldPoolId = volume.getPoolId();
                        volume.setPath(answer.getVolumePath());
                        volume.setFolder(destPool.getPath());
                        volume.setPodId(destPool.getPodId());
                        volume.setPoolId(destPool.getId());
                        volume.setLastPoolId(oldPoolId);
                        volume.setPodId(destPool.getPodId());
                        try {
                            stateTransitTo(volume, Volume.Event.OperationSucceeded);
                        } catch (NoTransitionException e) {
                            s_logger.debug("Failed to change volume state: " + e.toString());
                            throw new CloudRuntimeException("Failed to change volume state: " + e.toString());
                        }
                    }
                    transitResult = true;
                    try {
                        _checkPointMgr.popCheckPoint(checkPointTaskId);
                    } catch (Exception e) {

                    }
                } finally {
                    if (!transitResult) {
                        txn.rollback();
                    } else {
                        txn.commit();
                    }
                }

            }
        }

        // all the volumes get migrated to new storage pool, need to delete the copy on old storage pool
        for (Pair<StoragePoolVO, DestroyCommand> cmd : destroyCmds) {
            try {
                Answer cvAnswer = sendToPool(cmd.first(), cmd.second());
            } catch (StorageUnavailableException e) {
                s_logger.debug("Unable to delete the old copy on storage pool: " + e.toString());
            }
        }
        return true;
    }

    @Override
    public boolean StorageMigration(VirtualMachineProfile<? extends VirtualMachine> vm, StoragePool destPool) throws ConcurrentOperationException {
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

        return migrateVolumes(volumesNeedToMigrate, destPool);
    }

    @Override
    public void prepare(VirtualMachineProfile<? extends VirtualMachine> vm, DeployDestination dest) throws StorageUnavailableException, InsufficientStorageCapacityException {

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

        List<VolumeVO> recreateVols = new ArrayList<VolumeVO>(vols.size());

        for (VolumeVO vol : vols) {
            StoragePool assignedPool = null;
            if (dest.getStorageForDisks() != null) {
                assignedPool = dest.getStorageForDisks().get(vol);
            }
            if (assignedPool != null) {
                Volume.State state = vol.getState();
                if (state == Volume.State.Allocated || state == Volume.State.Creating) {
                    recreateVols.add(vol);
                } else {
                    if (vol.isRecreatable()) {
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Volume " + vol + " will be recreated on storage pool " + assignedPool + " assigned by deploymentPlanner");
                        }
                        recreateVols.add(vol);
                    } else {
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Volume " + vol + " is not recreatable! Cannot recreate on storagepool: " + assignedPool);
                        }
                        throw new StorageUnavailableException("Volume is not recreatable, Unable to create " + vol, Volume.class, vol.getId());
                        // copy volume usecase - not yet developed.
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
                vm.addDisk(new VolumeTO(vol, pool));
            }
        }

        for (VolumeVO vol : recreateVols) {
            VolumeVO newVol;
            if (vol.getState() == Volume.State.Allocated || vol.getState() == Volume.State.Creating) {
                newVol = vol;
            } else {
                newVol = switchVolume(vol, vm);
                // update the volume->storagePool map since volumeId has changed
                if (dest.getStorageForDisks() != null && dest.getStorageForDisks().containsKey(vol)) {
                    StoragePool poolWithOldVol = dest.getStorageForDisks().get(vol);
                    dest.getStorageForDisks().put(newVol, poolWithOldVol);
                    dest.getStorageForDisks().remove(vol);
                }
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Created new volume " + newVol + " for old volume " + vol);
                }
            }

            try {
                stateTransitTo(newVol, Volume.Event.CreateRequested);
            } catch (NoTransitionException e) {
                throw new CloudRuntimeException("Unable to create " + e.toString());
            }
            Pair<VolumeTO, StoragePool> created = createVolume(newVol, _diskOfferingDao.findById(newVol.getDiskOfferingId()), vm, vols, dest);
            if (created == null) {
                Long poolId = newVol.getPoolId();
                newVol.setPoolId(null);
                try {
                    stateTransitTo(newVol, Volume.Event.OperationFailed);
                } catch (NoTransitionException e) {
                    throw new CloudRuntimeException("Unable to update the failure on a volume: " + newVol, e);
                }
                throw new StorageUnavailableException("Unable to create " + newVol, poolId == null ? -1L : poolId);
            }
            created.first().setDeviceId(newVol.getDeviceId().intValue());
            newVol.setFolder(created.second().getPath());
            newVol.setPath(created.first().getPath());
            newVol.setSize(created.first().getSize());
            newVol.setPoolType(created.second().getPoolType());
            newVol.setPodId(created.second().getPodId());
            try {
                stateTransitTo(newVol, Volume.Event.OperationSucceeded);
            } catch (NoTransitionException e) {
                throw new CloudRuntimeException("Unable to update an CREATE operation succeeded on volume " + newVol, e);
            }
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Volume " + newVol + " is created on " + created.second());
            }

            vm.addDisk(created.first());
        }
    }

    @DB
    protected VolumeVO switchVolume(VolumeVO existingVolume, VirtualMachineProfile<? extends VirtualMachine> vm) throws StorageUnavailableException {
        Transaction txn = Transaction.currentTxn();
        txn.start();
        try {
            stateTransitTo(existingVolume, Volume.Event.DestroyRequested);
        } catch (NoTransitionException e) {
            s_logger.debug("Unable to destroy existing volume: " + e.toString());
        }

        Long templateIdToUse = null;
        Long volTemplateId = existingVolume.getTemplateId();
        long vmTemplateId = vm.getTemplateId();
        if (volTemplateId != null && volTemplateId.longValue() != vmTemplateId) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("switchVolume: Old Volume's templateId: " + volTemplateId + " does not match the VM's templateId: " + vmTemplateId + ", updating templateId in the new Volume");
            }
            templateIdToUse = vmTemplateId;
        }
        VolumeVO newVolume = allocateDuplicateVolume(existingVolume, templateIdToUse);
        txn.commit();
        return newVolume;

    }

    public Pair<VolumeTO, StoragePool> createVolume(VolumeVO toBeCreated, DiskOfferingVO offering, VirtualMachineProfile<? extends VirtualMachine> vm, List<? extends Volume> alreadyCreated,
            DeployDestination dest) throws StorageUnavailableException {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Creating volume: " + toBeCreated);
        }
        DiskProfile diskProfile = new DiskProfile(toBeCreated, offering, vm.getHypervisorType());

        VMTemplateVO template = null;
        if (toBeCreated.getTemplateId() != null) {
            template = _templateDao.findById(toBeCreated.getTemplateId());
        }

        if (dest.getStorageForDisks() != null) {
            StoragePool pool = dest.getStorageForDisks().get(toBeCreated);
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Trying to create in " + pool);
            }
            toBeCreated.setPoolId(pool.getId());
            try {
                stateTransitTo(toBeCreated, Volume.Event.OperationRetry);
            } catch (NoTransitionException e) {
                throw new CloudRuntimeException("Unable to retry a create operation on volume " + toBeCreated);
            }

            CreateCommand cmd = null;
            VMTemplateStoragePoolVO tmpltStoredOn = null;

            for (int i = 0; i < 2; i++) {
                if (template != null && template.getFormat() != Storage.ImageFormat.ISO) {
                    tmpltStoredOn = _tmpltMgr.prepareTemplateForCreate(template, pool);
                    if (tmpltStoredOn == null) {
                        s_logger.debug("Cannot use this pool " + pool + " because we can't propagate template " + template);
                        return null;
                    }
                    cmd = new CreateCommand(diskProfile, tmpltStoredOn.getLocalDownloadPath(), new StorageFilerTO(pool));
                } else {
                    if (template != null && Storage.ImageFormat.ISO == template.getFormat()) {
                        VMTemplateHostVO tmpltHostOn = _tmpltMgr.prepareISOForCreate(template, pool);
                        if (tmpltHostOn == null) {
                            throw new CloudRuntimeException("Did not find ISO in secondry storage in zone " + pool.getDataCenterId());
                        }
                    }
                    cmd = new CreateCommand(diskProfile, new StorageFilerTO(pool));
                }
                long[] hostIdsToTryFirst = { dest.getHost().getId() };
                Answer answer = sendToPool(pool, hostIdsToTryFirst, cmd);
                if (answer.getResult()) {
                    CreateAnswer createAnswer = (CreateAnswer) answer;
                    return new Pair<VolumeTO, StoragePool>(createAnswer.getVolume(), pool);
                } else {
                    if (tmpltStoredOn != null && (answer instanceof CreateAnswer) && ((CreateAnswer) answer).templateReloadRequested()) {
                        if (!_tmpltMgr.resetTemplateDownloadStateOnPool(tmpltStoredOn.getId())) {
                            break; // break out of template-redeploy retry loop
                        }
                    } else {
                        break;
                    }
                }
            }
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Unable to create volume " + toBeCreated);
        }
        return null;
    }

    @Override
    public void release(VirtualMachineProfile<? extends VMInstanceVO> profile) {
        // add code here
    }

    public void expungeVolume(VolumeVO vol) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Expunging " + vol);
        }
        String vmName = null;
        if (vol.getVolumeType() == Type.ROOT && vol.getInstanceId() != null) {
            VirtualMachine vm = _vmInstanceDao.findByIdIncludingRemoved(vol.getInstanceId());
            if (vm != null) {
                vmName = vm.getInstanceName();
            }
        }

        String volumePath = vol.getPath();
        Long poolId = vol.getPoolId();
        if (poolId == null || volumePath == null || volumePath.trim().isEmpty()) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Marking volume that was never created as destroyed: " + vol);
            }
            _volsDao.remove(vol.getId());
            return;
        }

        StoragePoolVO pool = _storagePoolDao.findById(poolId);
        if (pool == null) {
            s_logger.debug("Removing volume as storage pool is gone: " + poolId);
            _volsDao.remove(vol.getId());
            return;
        }

        DestroyCommand cmd = new DestroyCommand(pool, vol, vmName);
        try {
            Answer answer = sendToPool(pool, cmd);
            if (answer != null && answer.getResult()) {
                _volsDao.remove(vol.getId());
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Volume successfully expunged from " + poolId);
                }
            } else {
                s_logger.info("Will retry delete of " + vol + " from " + poolId);
            }
        } catch (StorageUnavailableException e) {
            s_logger.info("Storage is unavailable currently.  Will retry delete of " + vol + " from " + poolId);
        }

    }

    @Override
    @DB
    public void cleanupVolumes(long vmId) throws ConcurrentOperationException {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Cleaning storage for vm: " + vmId);
        }
        List<VolumeVO> volumesForVm = _volsDao.findByInstance(vmId);
        List<VolumeVO> toBeExpunged = new ArrayList<VolumeVO>();
        Transaction txn = Transaction.currentTxn();
        txn.start();
        for (VolumeVO vol : volumesForVm) {
            if (vol.getVolumeType().equals(Type.ROOT)) {
                // This check is for VM in Error state (volume is already destroyed)
                if (!vol.getState().equals(Volume.State.Destroy)) {
                    destroyVolume(vol);
                }
                toBeExpunged.add(vol);
            } else {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Detaching " + vol);
                }
                _volsDao.detachVolume(vol.getId());
            }
        }
        txn.commit();

        for (VolumeVO expunge : toBeExpunged) {
            expungeVolume(expunge);
        }
    }

    protected class StorageGarbageCollector implements Runnable {

        public StorageGarbageCollector() {
        }

        @Override
        public void run() {
            try {
                s_logger.trace("Storage Garbage Collection Thread is running.");

                cleanupStorage(true);

            } catch (Exception e) {
                s_logger.error("Caught the following Exception", e);
            }
        }
    }

    @Override
    public void onManagementNodeJoined(List<ManagementServerHostVO> nodeList, long selfNodeId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onManagementNodeLeft(List<ManagementServerHostVO> nodeList, long selfNodeId) {
        for (ManagementServerHostVO vo : nodeList) {
            if (vo.getMsid() == _serverId) {
                s_logger.info("Cleaning up storage maintenance jobs associated with Management server" + vo.getMsid());
                List<Long> poolIds = _storagePoolWorkDao.searchForPoolIdsForPendingWorkJobs(vo.getMsid());
                if (poolIds.size() > 0) {
                    for (Long poolId : poolIds) {
                        StoragePoolVO pool = _storagePoolDao.findById(poolId);
                        // check if pool is in an inconsistent state
                        if (pool != null
                                && (pool.getStatus().equals(StoragePoolStatus.ErrorInMaintenance) || pool.getStatus().equals(StoragePoolStatus.PrepareForMaintenance) || pool.getStatus().equals(
                                        StoragePoolStatus.CancelMaintenance))) {
                            _storagePoolWorkDao.removePendingJobsOnMsRestart(vo.getMsid(), poolId);
                            pool.setStatus(StoragePoolStatus.ErrorInMaintenance);
                            _storagePoolDao.update(poolId, pool);
                        }

                    }
                }
            }
        }
    }

    @Override
    public void onManagementNodeIsolated() {
    }

    @Override
    public CapacityVO getSecondaryStorageUsedStats(Long hostId, Long zoneId) {
        SearchCriteria<HostVO> sc = _hostDao.createSearchCriteria();
        if (zoneId != null) {
            sc.addAnd("dataCenterId", SearchCriteria.Op.EQ, zoneId);
        }

        List<HostVO> hosts = new ArrayList<HostVO>();
        if (hostId != null) {
            hosts.add(ApiDBUtils.findHostById(hostId));
        } else {
            hosts = _ssvmMgr.listSecondaryStorageHostsInOneZone(zoneId);
        }

        CapacityVO capacity = new CapacityVO(hostId, zoneId, null, null, 0, 0, CapacityVO.CAPACITY_TYPE_SECONDARY_STORAGE);
        for (HostVO host : hosts) {
            StorageStats stats = ApiDBUtils.getSecondaryStorageStatistics(host.getId());
            if (stats == null) {
                continue;
            }
            capacity.setUsedCapacity(stats.getByteUsed() + capacity.getUsedCapacity());
            capacity.setTotalCapacity(stats.getCapacityBytes() + capacity.getTotalCapacity());
        }

        return capacity;
    }

    @Override
    public CapacityVO getStoragePoolUsedStats(Long poolId, Long clusterId, Long podId, Long zoneId) {
        SearchCriteria<StoragePoolVO> sc = _storagePoolDao.createSearchCriteria();
        List<StoragePoolVO> pools = new ArrayList<StoragePoolVO>();

        if (zoneId != null) {
            sc.addAnd("dataCenterId", SearchCriteria.Op.EQ, zoneId);
        }

        if (podId != null) {
            sc.addAnd("podId", SearchCriteria.Op.EQ, podId);
        }

        if (clusterId != null) {
            sc.addAnd("clusterId", SearchCriteria.Op.EQ, clusterId);
        }

        if (poolId != null) {
            sc.addAnd("hostOrPoolId", SearchCriteria.Op.EQ, poolId);
        }
        if (poolId != null) {
            pools.add(_storagePoolDao.findById(poolId));
        } else {
            pools = _storagePoolDao.search(sc, null);
        }

        CapacityVO capacity = new CapacityVO(poolId, zoneId, podId, clusterId, 0, 0, CapacityVO.CAPACITY_TYPE_STORAGE);
        for (StoragePoolVO storagePool : pools) {
            StorageStats stats = ApiDBUtils.getStoragePoolStatistics(storagePool.getId());
            if (stats == null) {
                continue;
            }
            capacity.setUsedCapacity(stats.getByteUsed() + capacity.getUsedCapacity());
            capacity.setTotalCapacity(stats.getCapacityBytes() + capacity.getTotalCapacity());
        }
        return capacity;
    }

    @Override
    public StoragePool getStoragePool(long id) {
        return _storagePoolDao.findById(id);
    }

    @Override
    public VMTemplateHostVO findVmTemplateHost(long templateId, StoragePool pool) {
        long dcId = pool.getDataCenterId();
        Long podId = pool.getPodId();

        List<HostVO> secHosts = _ssvmMgr.listSecondaryStorageHostsInOneZone(dcId);

        // FIXME, for cloudzone, the local secondary storoge
        if (pool.isLocal() && pool.getPoolType() == StoragePoolType.Filesystem && secHosts.isEmpty()) {
            List<StoragePoolHostVO> sphs = _storagePoolHostDao.listByPoolId(pool.getId());
            if (!sphs.isEmpty()) {
                StoragePoolHostVO localStoragePoolHost = sphs.get(0);
                return _templateHostDao.findLocalSecondaryStorageByHostTemplate(localStoragePoolHost.getHostId(), templateId);
            } else {
                return null;
            }
        }

        if (secHosts.size() == 1) {
            VMTemplateHostVO templateHostVO = _templateHostDao.findByHostTemplate(secHosts.get(0).getId(), templateId);
            return templateHostVO;
        }
        if (podId != null) {
            List<VMTemplateHostVO> templHosts = _templateHostDao.listByTemplateStatus(templateId, dcId, podId, VMTemplateStorageResourceAssoc.Status.DOWNLOADED);
            if (templHosts != null && !templHosts.isEmpty()) {
                Collections.shuffle(templHosts);
                return templHosts.get(0);
            }
        }
        List<VMTemplateHostVO> templHosts = _templateHostDao.listByTemplateStatus(templateId, dcId, VMTemplateStorageResourceAssoc.Status.DOWNLOADED);
        if (templHosts != null && !templHosts.isEmpty()) {
            Collections.shuffle(templHosts);
            return templHosts.get(0);
        }
        return null;
    }

    @Override
    @DB
    public List<VMInstanceVO> listByStoragePool(long storagePoolId) {
        SearchCriteria<VMInstanceVO> sc = StoragePoolSearch.create();
        sc.setJoinParameters("vmVolume", "volumeType", Volume.Type.ROOT);
        sc.setJoinParameters("vmVolume", "poolId", storagePoolId);
        return _vmInstanceDao.search(sc, null);
    }

    @Override
    @DB
    public StoragePoolVO findLocalStorageOnHost(long hostId) {
        SearchCriteria<StoragePoolVO> sc = LocalStorageSearch.create();
        sc.setParameters("type", new Object[] { StoragePoolType.Filesystem, StoragePoolType.LVM });
        sc.setJoinParameters("poolHost", "hostId", hostId);
        List<StoragePoolVO> storagePools = _storagePoolDao.search(sc, null);
        if (!storagePools.isEmpty()) {
            return storagePools.get(0);
        } else {
            return null;
        }
    }

    @Override
    public Host updateSecondaryStorage(long secStorageId, String newUrl) {
        HostVO secHost = _hostDao.findById(secStorageId);
        if (secHost == null) {
            throw new InvalidParameterValueException("Can not find out the secondary storage id: " + secStorageId);
        }

        if (secHost.getType() != Host.Type.SecondaryStorage) {
            throw new InvalidParameterValueException("host: " + secStorageId + " is not a secondary storage");
        }

        URI uri = null;
        try {
            uri = new URI(UriUtils.encodeURIComponent(newUrl));
            if (uri.getScheme() == null) {
                throw new InvalidParameterValueException("uri.scheme is null " + newUrl + ", add nfs:// as a prefix");
            } else if (uri.getScheme().equalsIgnoreCase("nfs")) {
                if (uri.getHost() == null || uri.getHost().equalsIgnoreCase("") || uri.getPath() == null || uri.getPath().equalsIgnoreCase("")) {
                    throw new InvalidParameterValueException("Your host and/or path is wrong.  Make sure it's of the format nfs://hostname/path");
                }
            }
        } catch (URISyntaxException e) {
            throw new InvalidParameterValueException(newUrl + " is not a valid uri");
        }

        String oldUrl = secHost.getStorageUrl();

        URI oldUri = null;
        try {
            oldUri = new URI(UriUtils.encodeURIComponent(oldUrl));
            if (!oldUri.getScheme().equalsIgnoreCase(uri.getScheme())) {
                throw new InvalidParameterValueException("can not change old scheme:" + oldUri.getScheme() + " to " + uri.getScheme());
            }
        } catch (URISyntaxException e) {
            s_logger.debug("Failed to get uri from " + oldUrl);
        }

        secHost.setStorageUrl(newUrl);
        secHost.setGuid(newUrl);
        secHost.setName(newUrl);
        _hostDao.update(secHost.getId(), secHost);
        return secHost;
    }

    @Override
    public List<VolumeVO> searchForVolumes(ListVolumesCmd cmd) {
        Account caller = UserContext.current().getCaller();
        List<Long> permittedAccounts = new ArrayList<Long>();

        Long id = cmd.getId();
        Long vmInstanceId = cmd.getVirtualMachineId();
        String name = cmd.getVolumeName();
        String keyword = cmd.getKeyword();
        String type = cmd.getType();

        Long zoneId = cmd.getZoneId();
        Long podId = null;
        // Object host = null; TODO
        if (_accountMgr.isAdmin(caller.getType())) {
            podId = cmd.getPodId();
            // host = cmd.getHostId(); TODO
        }

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(cmd.getDomainId(), cmd.isRecursive(), null);
        _accountMgr.buildACLSearchParameters(caller, id, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts, domainIdRecursiveListProject, cmd.listAll(), false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();
        Filter searchFilter = new Filter(VolumeVO.class, "created", false, cmd.getStartIndex(), cmd.getPageSizeVal());

        // hack for now, this should be done better but due to needing a join I opted to
        // do this quickly and worry about making it pretty later
        SearchBuilder<VolumeVO> sb = _volumeDao.createSearchBuilder();
        _accountMgr.buildACLSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        sb.and("name", sb.entity().getName(), SearchCriteria.Op.LIKE);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("volumeType", sb.entity().getVolumeType(), SearchCriteria.Op.LIKE);
        sb.and("instanceId", sb.entity().getInstanceId(), SearchCriteria.Op.EQ);
        sb.and("dataCenterId", sb.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        sb.and("podId", sb.entity().getPodId(), SearchCriteria.Op.EQ);
        // Only return volumes that are not destroyed
        sb.and("state", sb.entity().getState(), SearchCriteria.Op.NEQ);

        SearchBuilder<DiskOfferingVO> diskOfferingSearch = _diskOfferingDao.createSearchBuilder();
        diskOfferingSearch.and("systemUse", diskOfferingSearch.entity().getSystemUse(), SearchCriteria.Op.NEQ);
        sb.join("diskOfferingSearch", diskOfferingSearch, sb.entity().getDiskOfferingId(), diskOfferingSearch.entity().getId(), JoinBuilder.JoinType.LEFTOUTER);

        // display UserVM volumes only
        SearchBuilder<VMInstanceVO> vmSearch = _vmInstanceDao.createSearchBuilder();
        vmSearch.and("type", vmSearch.entity().getType(), SearchCriteria.Op.NIN);
        vmSearch.or("nulltype", vmSearch.entity().getType(), SearchCriteria.Op.NULL);
        sb.join("vmSearch", vmSearch, sb.entity().getInstanceId(), vmSearch.entity().getId(), JoinBuilder.JoinType.LEFTOUTER);

        // now set the SC criteria...
        SearchCriteria<VolumeVO> sc = sb.create();
        _accountMgr.buildACLSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        if (keyword != null) {
            SearchCriteria<VolumeVO> ssc = _volumeDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("volumeType", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (name != null) {
            sc.setParameters("name", "%" + name + "%");
        }

        sc.setJoinParameters("diskOfferingSearch", "systemUse", 1);

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (type != null) {
            sc.setParameters("volumeType", "%" + type + "%");
        }
        if (vmInstanceId != null) {
            sc.setParameters("instanceId", vmInstanceId);
        }
        if (zoneId != null) {
            sc.setParameters("dataCenterId", zoneId);
        }
        if (podId != null) {
            sc.setParameters("podId", podId);
        }

        // Don't return DomR and ConsoleProxy volumes
        sc.setJoinParameters("vmSearch", "type", VirtualMachine.Type.ConsoleProxy, VirtualMachine.Type.SecondaryStorageVm, VirtualMachine.Type.DomainRouter);

        // Only return volumes that are not destroyed
        sc.setParameters("state", Volume.State.Destroy);

        return _volumeDao.search(sc, searchFilter);
    }

}
