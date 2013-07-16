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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.api.command.admin.storage.AddImageStoreCmd;
import org.apache.cloudstack.api.command.admin.storage.CancelPrimaryStorageMaintenanceCmd;
import org.apache.cloudstack.api.command.admin.storage.CreateCacheStoreCmd;
import org.apache.cloudstack.api.command.admin.storage.CreateStoragePoolCmd;
import org.apache.cloudstack.api.command.admin.storage.DeleteImageStoreCmd;
import org.apache.cloudstack.api.command.admin.storage.DeletePoolCmd;
import org.apache.cloudstack.api.command.admin.storage.UpdateStoragePoolCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.subsystem.api.storage.ClusterScope;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreLifeCycle;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProvider;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProviderManager;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.engine.subsystem.api.storage.HostScope;
import org.apache.cloudstack.engine.subsystem.api.storage.HypervisorHostListener;
import org.apache.cloudstack.engine.subsystem.api.storage.ImageStoreProvider;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.StoragePoolAllocator;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateService;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService.VolumeApiResult;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDetailsDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreVO;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreVO;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.agent.manager.Commands;
import com.cloud.api.ApiDBUtils;
import com.cloud.api.query.dao.TemplateJoinDao;
import com.cloud.api.query.vo.TemplateJoinVO;
import com.cloud.capacity.Capacity;
import com.cloud.capacity.CapacityManager;
import com.cloud.capacity.CapacityState;
import com.cloud.capacity.CapacityVO;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.cluster.ClusterManagerListener;
import com.cloud.cluster.ManagementServerHostVO;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.Pod;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConnectionException;
import com.cloud.exception.DiscoveryException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceInUseException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.HypervisorGuruManager;
import com.cloud.org.Grouping;
import com.cloud.org.Grouping.AllocationState;
import com.cloud.resource.ResourceState;
import com.cloud.server.ConfigurationServer;
import com.cloud.server.ManagementServer;
import com.cloud.server.StatsCollector;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.Volume.Type;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.StoragePoolWorkDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.dao.VMTemplateZoneDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.listener.StoragePoolMonitor;
import com.cloud.storage.listener.VolumeStateListener;
import com.cloud.template.TemplateManager;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.UriUtils;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.JoinBuilder.JoinType;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.VirtualMachineProfileImpl;
import com.cloud.vm.dao.VMInstanceDao;

@Component
@Local(value = { StorageManager.class, StorageService.class })
public class StorageManagerImpl extends ManagerBase implements StorageManager, ClusterManagerListener {
    private static final Logger s_logger = Logger.getLogger(StorageManagerImpl.class);

    protected String _name;
    @Inject
    protected AgentManager _agentMgr;
    @Inject
    protected TemplateManager _tmpltMgr;
    @Inject
    protected AccountManager _accountMgr;
    @Inject
    protected ConfigurationManager _configMgr;
    @Inject
    protected VolumeDao _volsDao;
    @Inject
    protected HostDao _hostDao;
    @Inject
    protected SnapshotDao _snapshotDao;
    @Inject
    protected StoragePoolHostDao _storagePoolHostDao;
    @Inject
    protected VMTemplatePoolDao _vmTemplatePoolDao = null;
    @Inject
    protected VMTemplateZoneDao _vmTemplateZoneDao;
    @Inject
    protected VMTemplateDao _vmTemplateDao = null;
    @Inject
    protected VMInstanceDao _vmInstanceDao;
    @Inject
    protected PrimaryDataStoreDao _storagePoolDao = null;
    @Inject
    protected ImageStoreDao _imageStoreDao = null;
    @Inject
    protected ImageStoreDetailsDao _imageStoreDetailsDao = null;
    @Inject
    protected SnapshotDataStoreDao _snapshotStoreDao = null;
    @Inject
    protected TemplateDataStoreDao _templateStoreDao = null;
    @Inject
    protected TemplateJoinDao _templateViewDao = null;
    @Inject
    protected VolumeDataStoreDao _volumeStoreDao = null;
    @Inject
    protected CapacityDao _capacityDao;
    @Inject
    protected CapacityManager _capacityMgr;
    @Inject
    protected DataCenterDao _dcDao = null;
    @Inject
    protected VMTemplateDao _templateDao;
    @Inject
    protected UserDao _userDao;
    @Inject
    protected ClusterDao _clusterDao;
    @Inject
    protected StoragePoolWorkDao _storagePoolWorkDao;
    @Inject
    protected HypervisorGuruManager _hvGuruMgr;
    @Inject
    protected VolumeDao _volumeDao;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    ManagementServer _msServer;
    @Inject
    DataStoreManager dataStoreMgr;
    @Inject
    DataStoreProviderManager dataStoreProviderMgr;
    @Inject
    VolumeService volService;
    @Inject
    VolumeDataFactory volFactory;
    @Inject
    TemplateDataFactory tmplFactory;
    @Inject
    SnapshotDataFactory snapshotFactory;
    @Inject
    ConfigurationServer _configServer;
    @Inject
    DataStoreManager _dataStoreMgr;
    @Inject
    DataStoreProviderManager _dataStoreProviderMgr;
    @Inject
    private TemplateService _imageSrv;
    @Inject
    EndPointSelector _epSelector;

    protected List<StoragePoolAllocator> _storagePoolAllocators;

    public List<StoragePoolAllocator> getStoragePoolAllocators() {
        return _storagePoolAllocators;
    }

    public void setStoragePoolAllocators(List<StoragePoolAllocator> _storagePoolAllocators) {
        this._storagePoolAllocators = _storagePoolAllocators;
    }

    protected List<StoragePoolDiscoverer> _discoverers;

    public List<StoragePoolDiscoverer> getDiscoverers() {
        return _discoverers;
    }

    public void setDiscoverers(List<StoragePoolDiscoverer> _discoverers) {
        this._discoverers = _discoverers;
    }

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
    // protected BigDecimal _overProvisioningFactor = new BigDecimal(1);
    private long _maxVolumeSizeInGb;
    private long _serverId;

    private int _customDiskOfferingMinSize = 1;
    private int _customDiskOfferingMaxSize = 1024;
    private final Map<String, HypervisorHostListener> hostListeners = new HashMap<String, HypervisorHostListener>();

    private boolean _recreateSystemVmEnabled;

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
        // if we return false, the allocator will try to switch to another PS if
        // available
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
            if (pool.getScope() == ScopeType.ZONE) {
                if (pool.getHypervisor() != null && pool.getHypervisor() == type) {
                    retPools.add(pool);
                }
            } else {
                ClusterVO cluster = _clusterDao.findById(pool.getClusterId());
                if (type == cluster.getHypervisorType()) {
                    retPools.add(pool);
                }
            }
        }
        Collections.shuffle(retPools);
        return retPools;
    }

    @Override
    public boolean isLocalStorageActiveOnHost(Long hostId) {
        List<StoragePoolHostVO> storagePoolHostRefs = _storagePoolHostDao.listByHostId(hostId);
        for (StoragePoolHostVO storagePoolHostRef : storagePoolHostRefs) {
            StoragePoolVO PrimaryDataStoreVO = _storagePoolDao.findById(storagePoolHostRef.getPoolId());
            if (PrimaryDataStoreVO.getPoolType() == StoragePoolType.LVM || PrimaryDataStoreVO.getPoolType() == StoragePoolType.EXT) {
                SearchBuilder<VolumeVO> volumeSB = _volsDao.createSearchBuilder();
                volumeSB.and("poolId", volumeSB.entity().getPoolId(), SearchCriteria.Op.EQ);
                volumeSB.and("removed", volumeSB.entity().getRemoved(), SearchCriteria.Op.NULL);
                volumeSB.and("state", volumeSB.entity().getState(), SearchCriteria.Op.NIN);

                SearchBuilder<VMInstanceVO> activeVmSB = _vmInstanceDao.createSearchBuilder();
                activeVmSB.and("state", activeVmSB.entity().getState(), SearchCriteria.Op.IN);
                volumeSB.join("activeVmSB", activeVmSB, volumeSB.entity().getInstanceId(), activeVmSB.entity().getId(), JoinBuilder.JoinType.INNER);

                SearchCriteria<VolumeVO> volumeSC = volumeSB.create();
                volumeSC.setParameters("poolId", PrimaryDataStoreVO.getId());
                volumeSC.setParameters("state", Volume.State.Expunging, Volume.State.Destroy);
                volumeSC.setJoinParameters("activeVmSB", "state", State.Starting, State.Running, State.Stopping, State.Migrating);

                List<VolumeVO> volumes = _volsDao.search(volumeSC, null);
                if (volumes.size() > 0) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public StoragePool findStoragePool(DiskProfile dskCh, final DataCenterVO dc, Pod pod, Long clusterId, Long hostId, VMInstanceVO vm,
                                       final Set<StoragePool> avoid) {

        VirtualMachineProfile<VMInstanceVO> profile = new VirtualMachineProfileImpl<VMInstanceVO>(vm);
        for (StoragePoolAllocator allocator : _storagePoolAllocators) {

            ExcludeList avoidList = new ExcludeList();
            for (StoragePool pool : avoid) {
                avoidList.addPool(pool.getId());
            }
            DataCenterDeployment plan = new DataCenterDeployment(dc.getId(), pod.getId(), clusterId, hostId, null, null);

            final List<StoragePool> poolList = allocator.allocateToPool(dskCh, profile, plan, avoidList, 1);
            if (poolList != null && !poolList.isEmpty()) {
                return (StoragePool) dataStoreMgr.getDataStore(poolList.get(0).getId(), DataStoreRole.Primary);
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
             * Can't find the vm where host resides on(vm is destroyed? or
             * volume is detached from vm), randomly choose a host to send the
             * cmd
             */
        }
        List<StoragePoolHostVO> poolHosts = _storagePoolHostDao.listByHostStatus(poolVO.getId(), Status.Up);
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

        Map<String, String> configs = _configDao.getConfiguration("management-server", params);

        _retry = NumbersUtil.parseInt(configs.get(Config.StartRetry.key()), 10);
        _pingInterval = NumbersUtil.parseInt(configs.get("ping.interval"), 60);
        _hostRetry = NumbersUtil.parseInt(configs.get("host.retry"), 2);
        _storagePoolAcquisitionWaitSeconds = NumbersUtil.parseInt(configs.get("pool.acquisition.wait.seconds"), 1800);
        s_logger.info("pool.acquisition.wait.seconds is configured as " + _storagePoolAcquisitionWaitSeconds + " seconds");

        _agentMgr.registerForHostEvents(new StoragePoolMonitor(this, _storagePoolDao), true, false, true);

        String storageCleanupEnabled = configs.get("storage.cleanup.enabled");
        _storageCleanupEnabled = (storageCleanupEnabled == null) ? true : Boolean.parseBoolean(storageCleanupEnabled);

        String value = _configDao.getValue(Config.CreateVolumeFromSnapshotWait.toString());
        _createVolumeFromSnapshotWait = NumbersUtil.parseInt(value, Integer.parseInt(Config.CreateVolumeFromSnapshotWait.getDefaultValue()));

        value = _configDao.getValue(Config.CopyVolumeWait.toString());
        _copyvolumewait = NumbersUtil.parseInt(value, Integer.parseInt(Config.CopyVolumeWait.getDefaultValue()));

        value = _configDao.getValue(Config.RecreateSystemVmEnabled.key());
        _recreateSystemVmEnabled = Boolean.parseBoolean(value);

        value = _configDao.getValue(Config.StorageTemplateCleanupEnabled.key());
        _templateCleanupEnabled = (value == null ? true : Boolean.parseBoolean(value));

        String time = configs.get("storage.cleanup.interval");
        _storageCleanupInterval = NumbersUtil.parseInt(time, 86400);

        s_logger.info("Storage cleanup enabled: " + _storageCleanupEnabled + ", interval: " + _storageCleanupInterval
                + ", template cleanup enabled: " + _templateCleanupEnabled);

        String workers = configs.get("expunge.workers");
        int wrks = NumbersUtil.parseInt(workers, 10);
        _executor = Executors.newScheduledThreadPool(wrks, new NamedThreadFactory("StorageManager-Scavenger"));

        _agentMgr.registerForHostEvents(ComponentContext.inject(LocalStoragePoolListener.class), true, false, false);

        String maxVolumeSizeInGbString = _configDao.getValue("storage.max.volume.size");
        _maxVolumeSizeInGb = NumbersUtil.parseLong(maxVolumeSizeInGbString, 2000);

        String _customDiskOfferingMinSizeStr = _configDao.getValue(Config.CustomDiskOfferingMinSize.toString());
        _customDiskOfferingMinSize = NumbersUtil.parseInt(_customDiskOfferingMinSizeStr,
                Integer.parseInt(Config.CustomDiskOfferingMinSize.getDefaultValue()));

        String _customDiskOfferingMaxSizeStr = _configDao.getValue(Config.CustomDiskOfferingMaxSize.toString());
        _customDiskOfferingMaxSize = NumbersUtil.parseInt(_customDiskOfferingMaxSizeStr,
                Integer.parseInt(Config.CustomDiskOfferingMaxSize.getDefaultValue()));

        _serverId = _msServer.getId();

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
        volumeSearch.and("state", volumeSearch.entity().getState(), SearchCriteria.Op.EQ);
        StoragePoolSearch.join("vmVolume", volumeSearch, volumeSearch.entity().getInstanceId(), StoragePoolSearch.entity().getId(),
                JoinBuilder.JoinType.INNER);
        StoragePoolSearch.done();

        LocalStorageSearch = _storagePoolDao.createSearchBuilder();
        SearchBuilder<StoragePoolHostVO> storageHostSearch = _storagePoolHostDao.createSearchBuilder();
        storageHostSearch.and("hostId", storageHostSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        LocalStorageSearch.join("poolHost", storageHostSearch, storageHostSearch.entity().getPoolId(), LocalStorageSearch.entity().getId(),
                JoinBuilder.JoinType.INNER);
        LocalStorageSearch.and("type", LocalStorageSearch.entity().getPoolType(), SearchCriteria.Op.IN);
        LocalStorageSearch.done();

        Volume.State.getStateMachine().registerListener(new VolumeStateListener());

        return true;
    }

    @Override
    public String getStoragePoolTags(long poolId) {
        return _configMgr.listToCsvTags(_storagePoolDao.searchForStoragePoolDetails(poolId, "true"));
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

    @DB
    @Override
    public DataStore createLocalStorage(Host host, StoragePoolInfo pInfo) throws ConnectionException {

        DataCenterVO dc = _dcDao.findById(host.getDataCenterId());
        if (dc == null || !dc.isLocalStorageEnabled()) {
            return null;
        }
        DataStore store;
        try {
            StoragePoolVO pool = _storagePoolDao.findPoolByHostPath(host.getDataCenterId(), host.getPodId(), pInfo.getHost(), pInfo.getHostPath(),
                    pInfo.getUuid());
            if (pool == null && host.getHypervisorType() == HypervisorType.VMware) {
                // perform run-time upgrade. In versions prior to 2.2.12, there
                // is a bug that we don't save local datastore info (host path
                // is empty), this will cause us
                // not able to distinguish multiple local datastores that may be
                // available on the host, to support smooth migration, we
                // need to perform runtime upgrade here
                if (pInfo.getHostPath().length() > 0) {
                    pool = _storagePoolDao.findPoolByHostPath(host.getDataCenterId(), host.getPodId(), pInfo.getHost(), "", pInfo.getUuid());
                }
            }
            DataStoreProvider provider = dataStoreProviderMgr.getDefaultPrimaryDataStoreProvider();
            DataStoreLifeCycle lifeCycle = provider.getDataStoreLifeCycle();
            if (pool == null) {
                Map<String, Object> params = new HashMap<String, Object>();
                String name = (host.getName() + " Local Storage");
                params.put("zoneId", host.getDataCenterId());
                params.put("clusterId", host.getClusterId());
                params.put("podId", host.getPodId());
                params.put("url", pInfo.getPoolType().toString() + "://" + pInfo.getHost() + "/" + pInfo.getHostPath());
                params.put("name", name);
                params.put("localStorage", true);
                params.put("details", pInfo.getDetails());
                params.put("uuid", pInfo.getUuid());
                params.put("providerName", provider.getName());

                store = lifeCycle.initialize(params);
            } else {
                store = dataStoreMgr.getDataStore(pool.getId(), DataStoreRole.Primary);
            }

            HostScope scope = new HostScope(host.getId(), host.getDataCenterId());
            lifeCycle.attachHost(store, scope, pInfo);
        } catch (Exception e) {
            s_logger.warn("Unable to setup the local storage pool for " + host, e);
            throw new ConnectionException(true, "Unable to setup the local storage pool for " + host, e);
        }

        return dataStoreMgr.getDataStore(store.getId(), DataStoreRole.Primary);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public PrimaryDataStoreInfo createPool(CreateStoragePoolCmd cmd) throws ResourceInUseException, IllegalArgumentException, UnknownHostException,
            ResourceUnavailableException {
        String providerName = cmd.getStorageProviderName();
        DataStoreProvider storeProvider = dataStoreProviderMgr.getDataStoreProvider(providerName);

        if (storeProvider == null) {
            storeProvider = dataStoreProviderMgr.getDefaultPrimaryDataStoreProvider();
            if (storeProvider == null) {
                throw new InvalidParameterValueException("can't find storage provider: " + providerName);
            }
        }

        Long clusterId = cmd.getClusterId();
        Long podId = cmd.getPodId();
        Long zoneId = cmd.getZoneId();

        ScopeType scopeType = ScopeType.CLUSTER;
        String scope = cmd.getScope();
        if (scope != null) {
            try {
                scopeType = Enum.valueOf(ScopeType.class, scope.toUpperCase());
            } catch (Exception e) {
                throw new InvalidParameterValueException("invalid scope for pool " + scope);
            }
        }

        if (scopeType == ScopeType.CLUSTER && clusterId == null) {
            throw new InvalidParameterValueException("cluster id can't be null, if scope is cluster");
        } else if (scopeType == ScopeType.ZONE && zoneId == null) {
            throw new InvalidParameterValueException("zone id can't be null, if scope is zone");
        }

        HypervisorType hypervisorType = HypervisorType.KVM;
        if (scopeType == ScopeType.ZONE) {
            // ignore passed clusterId and podId
            clusterId = null;
            podId = null;
            String hypervisor = cmd.getHypervisor();
            if (hypervisor != null) {
                try {
                    hypervisorType = HypervisorType.getType(hypervisor);
                } catch (Exception e) {
                    throw new InvalidParameterValueException("invalid hypervisor type " + hypervisor);
                }
            } else {
                throw new InvalidParameterValueException(
                        "Missing parameter hypervisor. Hypervisor type is required to create zone wide primary storage.");
            }
            if (hypervisorType != HypervisorType.KVM && hypervisorType != HypervisorType.VMware &&
                    hypervisorType != HypervisorType.Any) {
                throw new InvalidParameterValueException(
                        "zone wide storage pool is not supported for hypervisor type " + hypervisor);
            }
        }

        Map<String, String> details = extractApiParamAsMap(cmd.getDetails());
        DataCenterVO zone = _dcDao.findById(cmd.getZoneId());
        if (zone == null) {
            throw new InvalidParameterValueException("unable to find zone by id " + zoneId);
        }
        // Check if zone is disabled
        Account account = CallContext.current().getCallingAccount();
        if (Grouping.AllocationState.Disabled == zone.getAllocationState() && !_accountMgr.isRootAdmin(account.getType())) {
            throw new PermissionDeniedException("Cannot perform this operation, Zone is currently disabled: " + zoneId);
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("zoneId", zone.getId());
        params.put("clusterId", clusterId);
        params.put("podId", podId);
        params.put("url", cmd.getUrl());
        params.put("tags", cmd.getTags());
        params.put("name", cmd.getStoragePoolName());
        params.put("details", details);
        params.put("providerName", storeProvider.getName());
        params.put("managed", cmd.isManaged());
        params.put("capacityBytes", cmd.getCapacityBytes());
        params.put("capacityIops", cmd.getCapacityIops());

        DataStoreLifeCycle lifeCycle = storeProvider.getDataStoreLifeCycle();
        DataStore store = null;
        try {
            store = lifeCycle.initialize(params);
            if (scopeType == ScopeType.CLUSTER) {
                ClusterScope clusterScope = new ClusterScope(clusterId, podId, zoneId);
                lifeCycle.attachCluster(store, clusterScope);
            } else if (scopeType == ScopeType.ZONE) {
                ZoneScope zoneScope = new ZoneScope(zoneId);
                lifeCycle.attachZone(store, zoneScope, hypervisorType);
            }
        } catch (Exception e) {
            s_logger.debug("Failed to add data store", e);
            // clean up the db
            if (store != null) {
                lifeCycle.deleteDataStore(store);
            }
            throw new CloudRuntimeException("Failed to add data store", e);
        }

        return (PrimaryDataStoreInfo) dataStoreMgr.getDataStore(store.getId(), DataStoreRole.Primary);
    }

    private Map<String, String> extractApiParamAsMap(Map ds) {
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
        return details;
    }

    @Override
    public PrimaryDataStoreInfo updateStoragePool(UpdateStoragePoolCmd cmd) throws IllegalArgumentException {
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

        return (PrimaryDataStoreInfo) dataStoreMgr.getDataStore(pool.getId(), DataStoreRole.Primary);
    }

    @Override
    @DB
    public boolean deletePool(DeletePoolCmd cmd) {
        Long id = cmd.getId();
        boolean forced = cmd.isForced();

        StoragePoolVO sPool = _storagePoolDao.findById(id);
        if (sPool == null) {
            s_logger.warn("Unable to find pool:" + id);
            throw new InvalidParameterValueException("Unable to find pool by id " + id);
        }
        if (sPool.getStatus() != StoragePoolStatus.Maintenance) {
            s_logger.warn("Unable to delete storage id: " + id + " due to it is not in Maintenance state");
            throw new InvalidParameterValueException("Unable to delete storage due to it is not in Maintenance state, id: " + id);
        }
        if (sPool.isLocal()) {
            s_logger.warn("Unable to delete local storage id:" + id);
            throw new InvalidParameterValueException("Unable to delete local storage id: " + id);
        }

        Pair<Long, Long> vlms = _volsDao.getCountAndTotalByPool(id);
        if (forced) {
            if (vlms.first() > 0) {
                Pair<Long, Long> nonDstrdVlms = _volsDao.getNonDestroyedCountAndTotalByPool(id);
                if (nonDstrdVlms.first() > 0) {
                    throw new CloudRuntimeException("Cannot delete pool " + sPool.getName() + " as there are associated "
                            + "non-destroyed vols for this pool");
                }
                // force expunge non-destroyed volumes
                List<VolumeVO> vols = _volsDao.listVolumesToBeDestroyed();
                for (VolumeVO vol : vols) {
                    AsyncCallFuture<VolumeApiResult> future = volService.expungeVolumeAsync(volFactory.getVolume(vol.getId()));
                    try {
                        future.get();
                    } catch (InterruptedException e) {
                        s_logger.debug("expunge volume failed:" + vol.getId(), e);
                    } catch (ExecutionException e) {
                        s_logger.debug("expunge volume failed:" + vol.getId(), e);
                    }
                }
            }
        } else {
            // Check if the pool has associated volumes in the volumes table
            // If it does , then you cannot delete the pool
            if (vlms.first() > 0) {
                throw new CloudRuntimeException("Cannot delete pool " + sPool.getName() + " as there are associated volumes for this pool");
            }
        }

        // First get the host_id from storage_pool_host_ref for given pool id
        StoragePoolVO lock = _storagePoolDao.acquireInLockTable(sPool.getId());

        if (lock == null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Failed to acquire lock when deleting PrimaryDataStoreVO with ID: " + sPool.getId());
            }
            return false;
        }

        _storagePoolDao.releaseFromLockTable(lock.getId());
        s_logger.trace("Released lock for storage pool " + id);

        DataStoreProvider storeProvider = dataStoreProviderMgr.getDataStoreProvider(sPool.getStorageProviderName());
        DataStoreLifeCycle lifeCycle = storeProvider.getDataStoreLifeCycle();
        DataStore store = dataStoreMgr.getDataStore(sPool.getId(), DataStoreRole.Primary);
        return lifeCycle.deleteDataStore(store);
    }

    @Override
    public void connectHostToSharedPool(long hostId, long poolId) throws StorageUnavailableException {
        StoragePool pool = (StoragePool) dataStoreMgr.getDataStore(poolId, DataStoreRole.Primary);
        assert (pool.isShared()) : "Now, did you actually read the name of this method?";
        s_logger.debug("Adding pool " + pool.getName() + " to  host " + hostId);

        DataStoreProvider provider = dataStoreProviderMgr.getDataStoreProvider(pool.getStorageProviderName());
        HypervisorHostListener listener = hostListeners.get(provider.getName());
        listener.hostConnect(hostId, pool.getId());
    }

    @Override
    public BigDecimal getStorageOverProvisioningFactor(Long dcId) {
        return new BigDecimal(_configServer.getConfigValue(Config.StorageOverprovisioningFactor.key(),
                Config.ConfigurationParameterScope.zone.toString(), dcId));
    }

    @Override
    public void createCapacityEntry(StoragePoolVO storagePool, short capacityType, long allocated) {
        SearchCriteria<CapacityVO> capacitySC = _capacityDao.createSearchCriteria();
        capacitySC.addAnd("hostOrPoolId", SearchCriteria.Op.EQ, storagePool.getId());
        capacitySC.addAnd("dataCenterId", SearchCriteria.Op.EQ, storagePool.getDataCenterId());
        capacitySC.addAnd("capacityType", SearchCriteria.Op.EQ, capacityType);

        List<CapacityVO> capacities = _capacityDao.search(capacitySC, null);

        long totalOverProvCapacity;
        if (storagePool.getPoolType() == StoragePoolType.NetworkFilesystem) {
            BigDecimal overProvFactor = getStorageOverProvisioningFactor(storagePool.getDataCenterId());
            totalOverProvCapacity = overProvFactor.multiply(new BigDecimal(storagePool.getCapacityBytes())).longValue();// All
            // this
            // for
            // the
            // inaccuracy
            // of
            // floats
            // for
            // big
            // number
            // multiplication.
        } else {
            totalOverProvCapacity = storagePool.getCapacityBytes();
        }

        if (capacities.size() == 0) {
            CapacityVO capacity = new CapacityVO(storagePool.getId(), storagePool.getDataCenterId(), storagePool.getPodId(),
                    storagePool.getClusterId(), allocated, totalOverProvCapacity, capacityType);

            if (storagePool.getScope() == ScopeType.ZONE) {
                DataCenterVO dc = ApiDBUtils.findZoneById(storagePool.getDataCenterId());
                AllocationState allocationState = dc.getAllocationState();
                CapacityState capacityState = (allocationState == AllocationState.Disabled) ? CapacityState.Disabled
                        : CapacityState.Enabled;
                capacity.setCapacityState(capacityState);
            } else {
                if (storagePool.getClusterId() != null) {
                    ClusterVO cluster = ApiDBUtils.findClusterById(storagePool.getClusterId());
                    if (cluster != null) {
                        AllocationState allocationState = _configMgr.findClusterAllocationState(cluster);
                        CapacityState capacityState = (allocationState == AllocationState.Disabled) ? CapacityState.Disabled
                                : CapacityState.Enabled;
                        capacity.setCapacityState(capacityState);
                    }
                }
            }
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
    public Pair<Long, Answer[]> sendToPool(StoragePool pool, long[] hostIdsToTryFirst, List<Long> hostIdsToAvoid, Commands cmds)
            throws StorageUnavailableException {
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
            throw new StorageUnavailableException("Unable to send command to the pool " + pool.getId()
                    + " due to there is no enabled hosts up in this cluster", pool.getId());
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
    public Pair<Long, Answer> sendToPool(StoragePool pool, long[] hostIdsToTryFirst, List<Long> hostIdsToAvoid, Command cmd)
            throws StorageUnavailableException {
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
                                s_logger.debug("Storage pool garbage collector found " + unusedTemplatesInPool.size()
                                        + " templates to clean up in storage pool: " + pool.getName());
                                for (VMTemplateStoragePoolVO templatePoolVO : unusedTemplatesInPool) {
                                    if (templatePoolVO.getDownloadState() != VMTemplateStorageResourceAssoc.Status.DOWNLOADED) {
                                        s_logger.debug("Storage pool garbage collector is skipping templatePoolVO with ID: " + templatePoolVO.getId()
                                                + " because it is not completely downloaded.");
                                        continue;
                                    }

                                    if (!templatePoolVO.getMarkedForGC()) {
                                        templatePoolVO.setMarkedForGC(true);
                                        _vmTemplatePoolDao.update(templatePoolVO.getId(), templatePoolVO);
                                        s_logger.debug("Storage pool garbage collector has marked templatePoolVO with ID: " + templatePoolVO.getId()
                                                + " for garbage collection.");
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

                            volService.expungeVolumeAsync(volFactory.getVolume(vol.getId()));

                        } catch (Exception e) {
                            s_logger.warn("Unable to destroy " + vol.getId(), e);
                        }
                    }

                    // remove snapshots in Error state
                    List<SnapshotVO> snapshots = _snapshotDao.listAllByStatus(Snapshot.State.Error);
                    for (SnapshotVO snapshotVO : snapshots) {
                        try {
                            _snapshotDao.expunge(snapshotVO.getId());
                        } catch (Exception e) {
                            s_logger.warn("Unable to destroy " + snapshotVO.getId(), e);
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
    List<Long> findAllVolumeIdInSnapshotTable(Long storeId) {
        String sql = "SELECT volume_id from snapshots, snapshot_store_ref WHERE snapshots.id = snapshot_store_ref.snapshot_id and store_id=? GROUP BY volume_id";
        List<Long> list = new ArrayList<Long>();
        try {
            Transaction txn = Transaction.currentTxn();
            ResultSet rs = null;
            PreparedStatement pstmt = null;
            pstmt = txn.prepareAutoCloseStatement(sql);
            pstmt.setLong(1, storeId);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(rs.getLong(1));
            }
            return list;
        } catch (Exception e) {
            s_logger.debug("failed to get all volumes who has snapshots in secondary storage " + storeId + " due to " + e.getMessage());
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
        // NOTE that object_store refactor will immediately delete the object from secondary storage when deleteTemplate etc api is issued.
        // so here we don't need to issue DeleteCommand to resource anymore, only need to remove db entry.
        try {
            // Cleanup templates in template_store_ref
            List<DataStore> imageStores = dataStoreMgr.getImageStoresByScope(new ZoneScope(null));
            for (DataStore store : imageStores) {
                try {
                    long storeId = store.getId();
                    List<TemplateDataStoreVO> destroyedTemplateStoreVOs = _templateStoreDao.listDestroyed(storeId);
                    s_logger.debug("Secondary storage garbage collector found " + destroyedTemplateStoreVOs.size()
                            + " templates to cleanup on template_store_ref for store: " + store.getName());
                    for (TemplateDataStoreVO destroyedTemplateStoreVO : destroyedTemplateStoreVOs) {
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Deleting template store DB entry: " + destroyedTemplateStoreVO);
                        }
                        _templateStoreDao.remove(destroyedTemplateStoreVO.getId());
                    }
                } catch (Exception e) {
                    s_logger.warn("problem cleaning up templates in template_store_ref for store: " + store.getName(), e);
                }
            }

            // CleanUp snapshots on snapshot_store_ref
            for (DataStore store : imageStores) {
                try {
                    List<SnapshotDataStoreVO> destroyedSnapshotStoreVOs = _snapshotStoreDao.listDestroyed(store.getId());
                    s_logger.debug("Secondary storage garbage collector found " + destroyedSnapshotStoreVOs.size()
                            + " snapshots to cleanup on snapshot_store_ref for store: " + store.getName());
                    for (SnapshotDataStoreVO destroyedSnapshotStoreVO : destroyedSnapshotStoreVOs) {
                        // check if this snapshot has child
                        SnapshotInfo snap = snapshotFactory.getSnapshot(destroyedSnapshotStoreVO.getSnapshotId(), store);
                        if ( snap.getChild() != null ){
                            s_logger.debug("Skip snapshot on store: " + destroyedSnapshotStoreVO + " , because it has child");
                            continue;
                        }

                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Deleting snapshot store DB entry: " + destroyedSnapshotStoreVO);
                        }

                        _snapshotStoreDao.remove(destroyedSnapshotStoreVO.getId());
                    }

                } catch (Exception e2) {
                    s_logger.warn("problem cleaning up snapshots in snapshot_store_ref for store: " + store.getName(), e2);
                }

            }

            // CleanUp volumes on volume_store_ref
            for (DataStore store : imageStores) {
                try {
                    List<VolumeDataStoreVO> destroyedStoreVOs = _volumeStoreDao.listDestroyed(store.getId());
                    s_logger.debug("Secondary storage garbage collector found " + destroyedStoreVOs.size()
                            + " volumes to cleanup on volume_store_ref for store: " + store.getName());
                    for (VolumeDataStoreVO destroyedStoreVO : destroyedStoreVOs) {
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Deleting volume store DB entry: " + destroyedStoreVO);
                        }
                        _volumeStoreDao.remove(destroyedStoreVO.getId());
                    }

                } catch (Exception e2) {
                    s_logger.warn("problem cleaning up volumes in volume_store_ref for store: " + store.getName(), e2);
                }
            }
        } catch (Exception e3) {
            s_logger.warn("problem cleaning up secondary storage DB entries. ", e3);
        }
    }

    @Override
    public String getPrimaryStorageNameLabel(VolumeVO volume) {
        Long poolId = volume.getPoolId();

        // poolId is null only if volume is destroyed, which has been checked
        // before.
        assert poolId != null;
        StoragePoolVO PrimaryDataStoreVO = _storagePoolDao.findById(poolId);
        assert PrimaryDataStoreVO != null;
        return PrimaryDataStoreVO.getUuid();
    }

    @Override
    @DB
    public PrimaryDataStoreInfo preparePrimaryStorageForMaintenance(Long primaryStorageId) throws ResourceUnavailableException,
            InsufficientCapacityException {
        Long userId = CallContext.current().getCallingUserId();
        User user = _userDao.findById(userId);
        Account account = CallContext.current().getCallingAccount();

        boolean restart = true;
        StoragePoolVO primaryStorage = null;
        primaryStorage = _storagePoolDao.findById(primaryStorageId);

        if (primaryStorage == null) {
            String msg = "Unable to obtain lock on the storage pool record in preparePrimaryStorageForMaintenance()";
            s_logger.error(msg);
            throw new InvalidParameterValueException(msg);
        }

        if (!primaryStorage.getStatus().equals(StoragePoolStatus.Up) && !primaryStorage.getStatus().equals(StoragePoolStatus.ErrorInMaintenance)) {
            throw new InvalidParameterValueException("Primary storage with id " + primaryStorageId + " is not ready for migration, as the status is:"
                    + primaryStorage.getStatus().toString());
        }

        DataStoreProvider provider = dataStoreProviderMgr.getDataStoreProvider(primaryStorage.getStorageProviderName());
        DataStoreLifeCycle lifeCycle = provider.getDataStoreLifeCycle();
        DataStore store = dataStoreMgr.getDataStore(primaryStorage.getId(), DataStoreRole.Primary);
        lifeCycle.maintain(store);

        return (PrimaryDataStoreInfo) dataStoreMgr.getDataStore(primaryStorage.getId(), DataStoreRole.Primary);
    }

    @Override
    @DB
    public PrimaryDataStoreInfo cancelPrimaryStorageForMaintenance(CancelPrimaryStorageMaintenanceCmd cmd) throws ResourceUnavailableException {
        Long primaryStorageId = cmd.getId();
        Long userId = CallContext.current().getCallingUserId();
        User user = _userDao.findById(userId);
        Account account = CallContext.current().getCallingAccount();
        StoragePoolVO primaryStorage = null;

        primaryStorage = _storagePoolDao.findById(primaryStorageId);

        if (primaryStorage == null) {
            String msg = "Unable to obtain lock on the storage pool in cancelPrimaryStorageForMaintenance()";
            s_logger.error(msg);
            throw new InvalidParameterValueException(msg);
        }

        if (primaryStorage.getStatus().equals(StoragePoolStatus.Up) || primaryStorage.getStatus().equals(StoragePoolStatus.PrepareForMaintenance)) {
            throw new StorageUnavailableException("Primary storage with id " + primaryStorageId
                    + " is not ready to complete migration, as the status is:" + primaryStorage.getStatus().toString(), primaryStorageId);
        }

        DataStoreProvider provider = dataStoreProviderMgr.getDataStoreProvider(primaryStorage.getStorageProviderName());
        DataStoreLifeCycle lifeCycle = provider.getDataStoreLifeCycle();
        DataStore store = dataStoreMgr.getDataStore(primaryStorage.getId(), DataStoreRole.Primary);
        lifeCycle.cancelMaintain(store);

        return (PrimaryDataStoreInfo) dataStoreMgr.getDataStore(primaryStorage.getId(), DataStoreRole.Primary);
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
                s_logger.info("Cleaning up storage maintenance jobs associated with Management server: " + vo.getMsid());
                List<Long> poolIds = _storagePoolWorkDao.searchForPoolIdsForPendingWorkJobs(vo.getMsid());
                if (poolIds.size() > 0) {
                    for (Long poolId : poolIds) {
                        StoragePoolVO pool = _storagePoolDao.findById(poolId);
                        // check if pool is in an inconsistent state
                        if (pool != null
                                && (pool.getStatus().equals(StoragePoolStatus.ErrorInMaintenance)
                                || pool.getStatus().equals(StoragePoolStatus.PrepareForMaintenance) || pool.getStatus().equals(
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

        List<Long> hosts = new ArrayList<Long>();
        if (hostId != null) {
            hosts.add(hostId);
        } else {
            List<DataStore> stores = _dataStoreMgr.getImageStoresByScope(new ZoneScope(zoneId));
            if (stores != null) {
                for (DataStore store : stores) {
                    hosts.add(store.getId());
                }
            }
        }

        CapacityVO capacity = new CapacityVO(hostId, zoneId, null, null, 0, 0, CapacityVO.CAPACITY_TYPE_SECONDARY_STORAGE);
        for (Long id : hosts) {
            StorageStats stats = ApiDBUtils.getSecondaryStorageStatistics(id);
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
        for (StoragePoolVO PrimaryDataStoreVO : pools) {
            StorageStats stats = ApiDBUtils.getStoragePoolStatistics(PrimaryDataStoreVO.getId());
            if (stats == null) {
                continue;
            }
            capacity.setUsedCapacity(stats.getByteUsed() + capacity.getUsedCapacity());
            capacity.setTotalCapacity(stats.getCapacityBytes() + capacity.getTotalCapacity());
        }
        return capacity;
    }

    @Override
    public PrimaryDataStoreInfo getStoragePool(long id) {
        return (PrimaryDataStoreInfo) dataStoreMgr.getDataStore(id, DataStoreRole.Primary);
    }

    @Override
    @DB
    public List<VMInstanceVO> listByStoragePool(long storagePoolId) {
        SearchCriteria<VMInstanceVO> sc = StoragePoolSearch.create();
        sc.setJoinParameters("vmVolume", "volumeType", Volume.Type.ROOT);
        sc.setJoinParameters("vmVolume", "poolId", storagePoolId);
        sc.setJoinParameters("vmVolume", "state", Volume.State.Ready);
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
    public HypervisorType getHypervisorTypeFromFormat(ImageFormat format) {

        if (format == null) {
            return HypervisorType.None;
        }

        if (format == ImageFormat.VHD) {
            return HypervisorType.XenServer;
        } else if (format == ImageFormat.OVA) {
            return HypervisorType.VMware;
        } else if (format == ImageFormat.QCOW2) {
            return HypervisorType.KVM;
        } else if (format == ImageFormat.RAW) {
            return HypervisorType.Ovm;
        } else {
            return HypervisorType.None;
        }
    }

    private boolean checkUsagedSpace(StoragePool pool) {
        StatsCollector sc = StatsCollector.getInstance();
        double storageUsedThreshold = Double.parseDouble(_configServer.getConfigValue(Config.StorageCapacityDisableThreshold.key(),
                Config.ConfigurationParameterScope.zone.toString(), pool.getDataCenterId()));
        if (sc != null) {
            long totalSize = pool.getCapacityBytes();
            StorageStats stats = sc.getStoragePoolStats(pool.getId());
            if (stats == null) {
                stats = sc.getStorageStats(pool.getId());
            }
            if (stats != null) {
                double usedPercentage = ((double) stats.getByteUsed() / (double) totalSize);
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Checking pool " + pool.getId() + " for storage, totalSize: " + pool.getCapacityBytes() + ", usedBytes: "
                            + stats.getByteUsed() + ", usedPct: " + usedPercentage + ", disable threshold: " + storageUsedThreshold);
                }
                if (usedPercentage >= storageUsedThreshold) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Insufficient space on pool: " + pool.getId() + " since its usage percentage: " + usedPercentage
                                + " has crossed the pool.storage.capacity.disablethreshold: " + storageUsedThreshold);
                    }
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean storagePoolHasEnoughIops(List<Volume> requestedVolumes,
                                            StoragePool pool) {
        if (requestedVolumes == null || requestedVolumes.isEmpty() || pool == null) {
            return false;
        }
        // Only Solidfire type primary storage is using/setting Iops.
        // This check will fix to return the storage has enough Iops when capacityIops is set to NULL for any PS Storage provider
        if (pool.getCapacityIops() == null ) {
            return true;
        }
        long currentIops = 0;
        List<VolumeVO> volumesInPool = _volumeDao.findByPoolId(pool.getId(), null);


        for (VolumeVO volumeInPool : volumesInPool) {
            Long minIops = volumeInPool.getMinIops();

            if (minIops != null && minIops > 0) {
                currentIops += minIops;
            }
        }

        long requestedIops = 0;

        for (Volume requestedVolume : requestedVolumes) {
            Long minIops = requestedVolume.getMinIops();

            if (minIops != null && minIops > 0) {
                requestedIops += minIops;
            }
        }

        long futureIops = currentIops + requestedIops;
        
        // getCapacityIops returns a Long so we need to check for null
        if (pool.getCapacityIops() == null) {
            s_logger.warn("Storage pool " + pool.getName() + " (" + pool.getId() + ") does not supply Iops capacity, assuming enough capacity");
            return true;
        }
        
        return futureIops <= pool.getCapacityIops();
    }

    @Override
    public boolean storagePoolHasEnoughSpace(List<Volume> volumes,
                                             StoragePool pool) {
        if (volumes == null || volumes.isEmpty()){
            return false;
        }

        if (!checkUsagedSpace(pool)) {
            return false;
        }

        // allocated space includes template of specified volume
        StoragePoolVO poolVO = _storagePoolDao.findById(pool.getId());
        long allocatedSizeWithtemplate = _capacityMgr.getAllocatedPoolCapacity(poolVO, null);
        long totalAskingSize = 0;
        for (Volume volume : volumes) {
            if (volume.getTemplateId() != null) {
                VMTemplateVO tmpl = _templateDao.findById(volume.getTemplateId());
                if (tmpl.getFormat() != ImageFormat.ISO) {
                    allocatedSizeWithtemplate = _capacityMgr.getAllocatedPoolCapacity(poolVO, tmpl);
                }
            }
            if (volume.getState() != Volume.State.Ready) {
                totalAskingSize = totalAskingSize + volume.getSize();
            }
        }

        long totalOverProvCapacity;
        if (pool.getPoolType() == StoragePoolType.NetworkFilesystem) {
            totalOverProvCapacity = getStorageOverProvisioningFactor(pool.getDataCenterId()).multiply(new BigDecimal(pool.getCapacityBytes()))
                    .longValue();
        } else {
            totalOverProvCapacity = pool.getCapacityBytes();
        }

        double storageAllocatedThreshold = Double.parseDouble(_configServer.getConfigValue(Config.StorageAllocatedCapacityDisableThreshold.key(),
                Config.ConfigurationParameterScope.zone.toString(), pool.getDataCenterId()));
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Checking pool: " + pool.getId() + " for volume allocation " + volumes.toString() + ", maxSize : " + totalOverProvCapacity
                    + ", totalAllocatedSize : " + allocatedSizeWithtemplate + ", askingSize : " + totalAskingSize + ", allocated disable threshold: "
                    + storageAllocatedThreshold);
        }

        double usedPercentage = (allocatedSizeWithtemplate + totalAskingSize) / (double) (totalOverProvCapacity);
        if (usedPercentage > storageAllocatedThreshold) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Insufficient un-allocated capacity on: " + pool.getId() + " for volume allocation: " + volumes.toString()
                        + " since its allocated percentage: " + usedPercentage
                        + " has crossed the allocated pool.storage.allocated.capacity.disablethreshold: " + storageAllocatedThreshold
                        + ", skipping this pool");
            }
            return false;
        }

        if (totalOverProvCapacity < (allocatedSizeWithtemplate + totalAskingSize)) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Insufficient un-allocated capacity on: " + pool.getId() + " for volume allocation: " + volumes.toString()
                        + ", not enough storage, maxSize : " + totalOverProvCapacity + ", totalAllocatedSize : " + allocatedSizeWithtemplate
                        + ", askingSize : " + totalAskingSize);
            }
            return false;
        }
        return true;
    }

    @Override
    public void createCapacityEntry(long poolId) {
        StoragePoolVO storage = _storagePoolDao.findById(poolId);
        createCapacityEntry(storage, Capacity.CAPACITY_TYPE_STORAGE_ALLOCATED, 0);
    }

    @Override
    public synchronized boolean registerHostListener(String providerName, HypervisorHostListener listener) {
        hostListeners.put(providerName, listener);
        return true;
    }

    @Override
    public Answer sendToPool(long poolId, Command cmd) throws StorageUnavailableException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Answer[] sendToPool(long poolId, Commands cmd) throws StorageUnavailableException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ImageStore discoverImageStore(AddImageStoreCmd cmd) throws IllegalArgumentException, DiscoveryException, InvalidParameterValueException {
        String providerName = cmd.getProviderName();
        DataStoreProvider storeProvider = _dataStoreProviderMgr.getDataStoreProvider(providerName);

        if (storeProvider == null) {
            storeProvider = _dataStoreProviderMgr.getDefaultImageDataStoreProvider();
            if (storeProvider == null) {
                throw new InvalidParameterValueException("can't find image store provider: " + providerName);
            }
            providerName = storeProvider.getName(); // ignored passed provider name and use default image store provider name
        }

        Long dcId = cmd.getZoneId();
        Map details = cmd.getDetails();
        ScopeType scopeType = ScopeType.ZONE;
        if (dcId == null) {
            scopeType = ScopeType.REGION;
        }

        // check if scope is supported by store provider
        if (!((ImageStoreProvider) storeProvider).isScopeSupported(scopeType)) {
            throw new InvalidParameterValueException("Image store provider " + providerName + " does not support scope " + scopeType);
        }

        // check if we have already image stores from other different providers,
        // we currently are not supporting image stores from different
        // providers co-existing
        List<ImageStoreVO> imageStores = _imageStoreDao.listImageStores();
        for (ImageStoreVO store : imageStores) {
            if (!store.getProviderName().equalsIgnoreCase(providerName)) {
                throw new InvalidParameterValueException("You can only add new image stores from the same provider " + store.getProviderName()
                        + " already added");
            }
        }

        if (dcId != null) {
            // Check if the zone exists in the system
            DataCenterVO zone = _dcDao.findById(dcId);
            if (zone == null) {
                throw new InvalidParameterValueException("Can't find zone by id " + dcId);
            }

            Account account = CallContext.current().getCallingAccount();
            if (Grouping.AllocationState.Disabled == zone.getAllocationState() && !_accountMgr.isRootAdmin(account.getType())) {
                PermissionDeniedException ex = new PermissionDeniedException(
                        "Cannot perform this operation, Zone with specified id is currently disabled");
                ex.addProxyObject(zone.getUuid(), "dcId");
                throw ex;
            }
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("zoneId", dcId);
        params.put("url", cmd.getUrl());
        params.put("name", cmd.getName());
        params.put("details", details);
        params.put("scope", scopeType);
        params.put("providerName", storeProvider.getName());
        params.put("role", DataStoreRole.Image);

        DataStoreLifeCycle lifeCycle = storeProvider.getDataStoreLifeCycle();
        DataStore store;
        try {
            store = lifeCycle.initialize(params);
        } catch (Exception e) {
            s_logger.debug("Failed to add data store", e);
            throw new CloudRuntimeException("Failed to add data store", e);
        }

        if (((ImageStoreProvider) storeProvider).needDownloadSysTemplate()) {
            // trigger system vm template download
            _imageSrv.downloadBootstrapSysTemplate(store);
        }
        else {
            // populate template_store_ref table
            _imageSrv.addSystemVMTemplatesToSecondary(store);
        }

        // associate builtin template with zones associated with this image
        // store
        associateCrosszoneTemplatesToZone(dcId);

        return (ImageStore) _dataStoreMgr.getDataStore(store.getId(), DataStoreRole.Image);
    }

    private void associateCrosszoneTemplatesToZone(Long zoneId) {
        VMTemplateZoneVO tmpltZone;

        List<VMTemplateVO> allTemplates = _vmTemplateDao.listAll();
        List<Long> dcIds = new ArrayList<Long>();
        if (zoneId != null) {
            dcIds.add(zoneId);
        } else {
            List<DataCenterVO> dcs = _dcDao.listAll();
            if (dcs != null) {
                for (DataCenterVO dc : dcs) {
                    dcIds.add(dc.getId());
                }
            }
        }

        for (VMTemplateVO vt : allTemplates) {
            if (vt.isCrossZones()) {
                for (Long dcId : dcIds) {
                    tmpltZone = _vmTemplateZoneDao.findByZoneTemplate(dcId, vt.getId());
                    if (tmpltZone == null) {
                        VMTemplateZoneVO vmTemplateZone = new VMTemplateZoneVO(dcId, vt.getId(), new Date());
                        _vmTemplateZoneDao.persist(vmTemplateZone);
                    }
                }
            }
        }
    }

    @Override
    public boolean deleteImageStore(DeleteImageStoreCmd cmd) {
        long storeId = cmd.getId();
        User caller = _accountMgr.getActiveUser(CallContext.current().getCallingUserId());
        // Verify that image store exists
        ImageStoreVO store = _imageStoreDao.findById(storeId);
        if (store == null) {
            throw new InvalidParameterValueException("Image store with id " + storeId + " doesn't exist");
        }
        _accountMgr.checkAccessAndSpecifyAuthority(CallContext.current().getCallingAccount(), store.getDataCenterId());

        // Verify that there are no live snapshot, template, volume on the image
        // store to be deleted
        List<SnapshotDataStoreVO> snapshots = _snapshotStoreDao.listByStoreId(storeId, DataStoreRole.Image);
        if (snapshots != null && snapshots.size() > 0) {
            throw new InvalidParameterValueException("Cannot delete image store with active snapshots backup!");
        }
        List<VolumeDataStoreVO> volumes = _volumeStoreDao.listByStoreId(storeId);
        if (volumes != null && volumes.size() > 0) {
            throw new InvalidParameterValueException("Cannot delete image store with active volumes backup!");
        }

        // search if there are user templates stored on this image store, excluding system, builtin templates
        List<TemplateJoinVO> templates = _templateViewDao.listActiveTemplates(storeId);
        if (templates != null && templates.size() > 0) {
            throw new InvalidParameterValueException("Cannot delete image store with active templates backup!");
        }

        // ready to delete
        Transaction txn = Transaction.currentTxn();
        txn.start();
        // first delete from image_store_details table, we need to do that since
        // we are not actually deleting record from main
        // image_data_store table, so delete cascade will not work
        _imageStoreDetailsDao.deleteDetails(storeId);
        _snapshotStoreDao.deletePrimaryRecordsForStore(storeId);
        _volumeStoreDao.deletePrimaryRecordsForStore(storeId);
        _templateStoreDao.deletePrimaryRecordsForStore(storeId);
        _imageStoreDao.remove(storeId);
        txn.commit();
        return true;
    }

    @Override
    public ImageStore createCacheStore(CreateCacheStoreCmd cmd) {
        String providerName = cmd.getProviderName();
        DataStoreProvider storeProvider = _dataStoreProviderMgr.getDataStoreProvider(providerName);

        if (storeProvider == null) {
            storeProvider = _dataStoreProviderMgr.getDefaultCacheDataStoreProvider();
            if (storeProvider == null) {
                throw new InvalidParameterValueException("can't find cache store provider: " + providerName);
            }
        }

        Long dcId = cmd.getZoneId();

        ScopeType scopeType = null;
        String scope = cmd.getScope();
        if (scope != null) {
            try {
                scopeType = Enum.valueOf(ScopeType.class, scope.toUpperCase());

            } catch (Exception e) {
                throw new InvalidParameterValueException("invalid scope for cache store " + scope);
            }

            if (scopeType != ScopeType.ZONE) {
                throw new InvalidParameterValueException("Only zone wide cache storage is supported");
            }
        }

        if (scopeType == ScopeType.ZONE && dcId == null) {
            throw new InvalidParameterValueException("zone id can't be null, if scope is zone");
        }

        // Check if the zone exists in the system
        DataCenterVO zone = _dcDao.findById(dcId);
        if (zone == null) {
            throw new InvalidParameterValueException("Can't find zone by id " + dcId);
        }

        Account account = CallContext.current().getCallingAccount();
        if (Grouping.AllocationState.Disabled == zone.getAllocationState() && !_accountMgr.isRootAdmin(account.getType())) {
            PermissionDeniedException ex = new PermissionDeniedException(
                    "Cannot perform this operation, Zone with specified id is currently disabled");
            ex.addProxyObject(zone.getUuid(), "dcId");
            throw ex;
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("zoneId", dcId);
        params.put("url", cmd.getUrl());
        params.put("name", cmd.getUrl());
        params.put("details", cmd.getDetails());
        params.put("scope", scopeType);
        params.put("providerName", storeProvider.getName());
        params.put("role", DataStoreRole.ImageCache);

        DataStoreLifeCycle lifeCycle = storeProvider.getDataStoreLifeCycle();
        DataStore store = null;
        try {
            store = lifeCycle.initialize(params);
        } catch (Exception e) {
            s_logger.debug("Failed to add data store", e);
            throw new CloudRuntimeException("Failed to add data store", e);
        }

        return (ImageStore) _dataStoreMgr.getDataStore(store.getId(), DataStoreRole.ImageCache);
    }

    // get bytesReadRate from service_offering, disk_offering and vm.disk.throttling.bytes_read_rate
    @Override
    public Long getDiskBytesReadRate(ServiceOfferingVO offering, DiskOfferingVO diskOffering) {
        if ((offering != null) && (offering.getBytesReadRate() != null) && (offering.getBytesReadRate() > 0)) {
            return offering.getBytesReadRate();
        } else if ((diskOffering != null) && (diskOffering.getBytesReadRate() != null) && (diskOffering.getBytesReadRate() > 0)) {
            return diskOffering.getBytesReadRate();
        } else {
            Long bytesReadRate = Long.parseLong(_configDao.getValue(Config.VmDiskThrottlingBytesReadRate.key()));
            if ((bytesReadRate > 0) && ((offering == null) || (! offering.getSystemUse()))) {
                return bytesReadRate;
            }
        }
        return 0L;
    }

    // get bytesWriteRate from service_offering, disk_offering and vm.disk.throttling.bytes_write_rate
    @Override
    public Long getDiskBytesWriteRate(ServiceOfferingVO offering, DiskOfferingVO diskOffering) {
        if ((offering != null) && (offering.getBytesWriteRate() != null) && (offering.getBytesWriteRate() > 0)) {
            return offering.getBytesWriteRate();
        } else if ((diskOffering != null) && (diskOffering.getBytesWriteRate() != null) && (diskOffering.getBytesWriteRate() > 0)) {
            return diskOffering.getBytesWriteRate();
        } else {
            Long bytesWriteRate = Long.parseLong(_configDao.getValue(Config.VmDiskThrottlingBytesWriteRate.key()));
            if ((bytesWriteRate > 0) && ((offering == null) || (! offering.getSystemUse())))  {
                return bytesWriteRate;
            }
        }
        return 0L;
    }

    // get iopsReadRate from service_offering, disk_offering and vm.disk.throttling.iops_read_rate
    @Override
    public Long getDiskIopsReadRate(ServiceOfferingVO offering, DiskOfferingVO diskOffering) {
        if ((offering != null) && (offering.getIopsReadRate() != null) && (offering.getIopsReadRate() > 0)) {
            return offering.getIopsReadRate();
        } else if ((diskOffering != null) && (diskOffering.getIopsReadRate() != null) && (diskOffering.getIopsReadRate() > 0)) {
            return diskOffering.getIopsReadRate();
        } else {
            Long iopsReadRate = Long.parseLong(_configDao.getValue(Config.VmDiskThrottlingIopsReadRate.key()));
            if ((iopsReadRate > 0) && ((offering == null) || (! offering.getSystemUse())))  {
                return iopsReadRate;
            }
        }
        return 0L;
    }

    // get iopsWriteRate from service_offering, disk_offering and vm.disk.throttling.iops_write_rate
    @Override
    public Long getDiskIopsWriteRate(ServiceOfferingVO offering, DiskOfferingVO diskOffering) {
        if ((offering != null) && (offering.getIopsWriteRate() != null) && (offering.getIopsWriteRate() > 0)) {
            return offering.getIopsWriteRate();
        } else if ((diskOffering != null) && (diskOffering.getIopsWriteRate() != null) && (diskOffering.getIopsWriteRate() > 0)) {
            return diskOffering.getIopsWriteRate();
        } else {
            Long iopsWriteRate = Long.parseLong(_configDao.getValue(Config.VmDiskThrottlingIopsWriteRate.key()));
            if ((iopsWriteRate > 0) && ((offering == null) || (! offering.getSystemUse())))  {
                return iopsWriteRate;
            }
        }
        return 0L;
    }
}
