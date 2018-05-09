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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.cloudstack.api.command.admin.storage.CancelPrimaryStorageMaintenanceCmd;
import org.apache.cloudstack.api.command.admin.storage.CreateSecondaryStagingStoreCmd;
import org.apache.cloudstack.api.command.admin.storage.CreateStoragePoolCmd;
import org.apache.cloudstack.api.command.admin.storage.DeleteImageStoreCmd;
import org.apache.cloudstack.api.command.admin.storage.DeletePoolCmd;
import org.apache.cloudstack.api.command.admin.storage.DeleteSecondaryStagingStoreCmd;
import org.apache.cloudstack.api.command.admin.storage.UpdateStoragePoolCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.subsystem.api.storage.ClusterScope;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreLifeCycle;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProvider;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProviderManager;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.engine.subsystem.api.storage.HostScope;
import org.apache.cloudstack.engine.subsystem.api.storage.HypervisorHostListener;
import org.apache.cloudstack.engine.subsystem.api.storage.ImageStoreProvider;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreLifeCycle;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotService;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateService;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateService.TemplateApiResult;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService.VolumeApiResult;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.storage.command.DettachCommand;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDetailsDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreVO;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreVO;
import org.apache.cloudstack.storage.image.datastore.ImageStoreEntity;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.DeleteStoragePoolCommand;
import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.DiskTO;
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
import com.cloud.cluster.ManagementServerHost;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.ConfigurationManagerImpl;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConnectionException;
import com.cloud.exception.DiscoveryException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceInUseException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.StorageConflictException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.HypervisorGuruManager;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.org.Grouping;
import com.cloud.org.Grouping.AllocationState;
import com.cloud.resource.ResourceState;
import com.cloud.server.ConfigurationServer;
import com.cloud.server.ManagementServer;
import com.cloud.server.StatsCollector;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.Volume.Type;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.StoragePoolTagsDao;
import com.cloud.storage.dao.StoragePoolWorkDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.dao.VMTemplateZoneDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.listener.StoragePoolMonitor;
import com.cloud.storage.listener.VolumeStateListener;
import com.cloud.template.TemplateManager;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.DateUtil;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.UriUtils;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.JoinBuilder.JoinType;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.VMInstanceDao;

@Component
public class StorageManagerImpl extends ManagerBase implements StorageManager, ClusterManagerListener, Configurable {
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
    private VolumeDataStoreDao _volumeDataStoreDao;
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
    protected StoragePoolDetailsDao _storagePoolDetailsDao;
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
    @Inject
    private DiskOfferingDao _diskOfferingDao;
    @Inject
    ResourceLimitService _resourceLimitMgr;
    @Inject
    EntityManager _entityMgr;
    @Inject
    SnapshotService _snapshotService;
    @Inject
    StoragePoolTagsDao _storagePoolTagsDao;

    protected List<StoragePoolDiscoverer> _discoverers;

    public List<StoragePoolDiscoverer> getDiscoverers() {
        return _discoverers;
    }

    public void setDiscoverers(List<StoragePoolDiscoverer> discoverers) {
        _discoverers = discoverers;
    }

    protected SearchBuilder<VMTemplateHostVO> HostTemplateStatesSearch;
    protected GenericSearchBuilder<StoragePoolHostVO, Long> UpHostsInPoolSearch;
    protected SearchBuilder<VMInstanceVO> StoragePoolSearch;
    protected SearchBuilder<StoragePoolVO> LocalStorageSearch;

    ScheduledExecutorService _executor = null;
    int _storagePoolAcquisitionWaitSeconds = 1800; // 30 minutes
    int _downloadUrlCleanupInterval;
    int _downloadUrlExpirationInterval;
    // protected BigDecimal _overProvisioningFactor = new BigDecimal(1);
    private long _serverId;

    private final Map<String, HypervisorHostListener> hostListeners = new HashMap<String, HypervisorHostListener>();

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
    public boolean configure(String name, Map<String, Object> params) {

        Map<String, String> configs = _configDao.getConfiguration("management-server", params);

        _storagePoolAcquisitionWaitSeconds = NumbersUtil.parseInt(configs.get("pool.acquisition.wait.seconds"), 1800);
        s_logger.info("pool.acquisition.wait.seconds is configured as " + _storagePoolAcquisitionWaitSeconds + " seconds");

        _agentMgr.registerForHostEvents(new StoragePoolMonitor(this, _storagePoolDao, _dataStoreProviderMgr), true, false, true);

        s_logger.info("Storage cleanup enabled: " + StorageCleanupEnabled.value() + ", interval: " + StorageCleanupInterval.value() + ", delay: " + StorageCleanupDelay.value()
        + ", template cleanup enabled: " + TemplateCleanupEnabled.value());

        String cleanupInterval = configs.get("extract.url.cleanup.interval");
        _downloadUrlCleanupInterval = NumbersUtil.parseInt(cleanupInterval, 7200);

        String urlExpirationInterval = configs.get("extract.url.expiration.interval");
        _downloadUrlExpirationInterval = NumbersUtil.parseInt(urlExpirationInterval, 14400);

        String workers = configs.get("expunge.workers");
        int wrks = NumbersUtil.parseInt(workers, 10);
        _executor = Executors.newScheduledThreadPool(wrks, new NamedThreadFactory("StorageManager-Scavenger"));

        _agentMgr.registerForHostEvents(ComponentContext.inject(LocalStoragePoolListener.class), true, false, false);

        _serverId = _msServer.getId();

        UpHostsInPoolSearch = _storagePoolHostDao.createSearchBuilder(Long.class);
        UpHostsInPoolSearch.selectFields(UpHostsInPoolSearch.entity().getHostId());
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
        StoragePoolSearch.join("vmVolume", volumeSearch, volumeSearch.entity().getInstanceId(), StoragePoolSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        StoragePoolSearch.done();

        LocalStorageSearch = _storagePoolDao.createSearchBuilder();
        SearchBuilder<StoragePoolHostVO> storageHostSearch = _storagePoolHostDao.createSearchBuilder();
        storageHostSearch.and("hostId", storageHostSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        LocalStorageSearch.join("poolHost", storageHostSearch, storageHostSearch.entity().getPoolId(), LocalStorageSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        LocalStorageSearch.and("type", LocalStorageSearch.entity().getPoolType(), SearchCriteria.Op.IN);
        LocalStorageSearch.done();

        Volume.State.getStateMachine().registerListener(new VolumeStateListener(_configDao, _vmInstanceDao));

        return true;
    }

    @Override
    public String getStoragePoolTags(long poolId) {
        return com.cloud.utils.StringUtils.listToCsvTags(_storagePoolDao.searchForStoragePoolTags(poolId));
    }

    @Override
    public boolean start() {
        if (StorageCleanupEnabled.value()) {
            Random generator = new Random();
            int initialDelay = generator.nextInt(StorageCleanupInterval.value());
            _executor.scheduleWithFixedDelay(new StorageGarbageCollector(), initialDelay, StorageCleanupInterval.value(), TimeUnit.SECONDS);
        } else {
            s_logger.debug("Storage cleanup is not enabled, so the storage cleanup thread is not being scheduled.");
        }

        _executor.scheduleWithFixedDelay(new DownloadURLGarbageCollector(), _downloadUrlCleanupInterval, _downloadUrlCleanupInterval, TimeUnit.SECONDS);

        return true;
    }

    @Override
    public boolean stop() {
        if (StorageCleanupEnabled.value()) {
            _executor.shutdown();
        }
        return true;
    }

    @DB
    @Override
    public DataStore createLocalStorage(Host host, StoragePoolInfo pInfo) throws ConnectionException {
        DataCenterVO dc = _dcDao.findById(host.getDataCenterId());
        if (dc == null) {
            return null;
        }
        boolean useLocalStorageForSystemVM = false;
        Boolean isLocal = ConfigurationManagerImpl.SystemVMUseLocalStorage.valueIn(dc.getId());
        if (isLocal != null) {
            useLocalStorageForSystemVM = isLocal.booleanValue();
        }
        if (!(dc.isLocalStorageEnabled() || useLocalStorageForSystemVM)) {
            return null;
        }
        DataStore store;
        try {
            String hostAddress = pInfo.getHost();
            if (host.getHypervisorType() == Hypervisor.HypervisorType.VMware) {
                hostAddress = "VMFS datastore: " + pInfo.getHostPath();
            }
            StoragePoolVO pool = _storagePoolDao.findPoolByHostPath(host.getDataCenterId(), host.getPodId(), hostAddress, pInfo.getHostPath(), pInfo.getUuid());
            if (pool == null && host.getHypervisorType() == HypervisorType.VMware) {
                // perform run-time upgrade. In versions prior to 2.2.12, there
                // is a bug that we don't save local datastore info (host path
                // is empty), this will cause us
                // not able to distinguish multiple local datastores that may be
                // available on the host, to support smooth migration, we
                // need to perform runtime upgrade here
                if (pInfo.getHostPath().length() > 0) {
                    pool = _storagePoolDao.findPoolByHostPath(host.getDataCenterId(), host.getPodId(), hostAddress, "", pInfo.getUuid());
                }
            }
            if (pool == null) {
                //the path can be different, but if they have the same uuid, assume they are the same storage
                pool = _storagePoolDao.findPoolByHostPath(host.getDataCenterId(), host.getPodId(), hostAddress, null, pInfo.getUuid());
                if (pool != null) {
                    s_logger.debug("Found a storage pool: " + pInfo.getUuid() + ", but with different hostpath " + pInfo.getHostPath() + ", still treat it as the same pool");
                }
            }

            DataStoreProvider provider = _dataStoreProviderMgr.getDefaultPrimaryDataStoreProvider();
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
                store = _dataStoreMgr.getDataStore(pool.getId(), DataStoreRole.Primary);
            }

            pool = _storagePoolDao.findById(store.getId());
            if (pool.getStatus() != StoragePoolStatus.Maintenance && pool.getStatus() != StoragePoolStatus.Removed) {
                HostScope scope = new HostScope(host.getId(), host.getClusterId(), host.getDataCenterId());
                lifeCycle.attachHost(store, scope, pInfo);
            }

        } catch (Exception e) {
            s_logger.warn("Unable to setup the local storage pool for " + host, e);
            throw new ConnectionException(true, "Unable to setup the local storage pool for " + host, e);
        }

        return _dataStoreMgr.getDataStore(store.getId(), DataStoreRole.Primary);
    }

    @Override
    public PrimaryDataStoreInfo createPool(CreateStoragePoolCmd cmd) throws ResourceInUseException, IllegalArgumentException, UnknownHostException, ResourceUnavailableException {
        String providerName = cmd.getStorageProviderName();
        DataStoreProvider storeProvider = _dataStoreProviderMgr.getDataStoreProvider(providerName);

        if (storeProvider == null) {
            storeProvider = _dataStoreProviderMgr.getDefaultPrimaryDataStoreProvider();
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
                throw new InvalidParameterValueException("Missing parameter hypervisor. Hypervisor type is required to create zone wide primary storage.");
            }
            if (hypervisorType != HypervisorType.KVM && hypervisorType != HypervisorType.VMware && hypervisorType != HypervisorType.Hyperv && hypervisorType != HypervisorType.LXC
                    && hypervisorType != HypervisorType.Any) {
                throw new InvalidParameterValueException("zone wide storage pool is not supported for hypervisor type " + hypervisor);
            }
        }

        Map<String, String> details = extractApiParamAsMap(cmd.getDetails());
        DataCenterVO zone = _dcDao.findById(cmd.getZoneId());
        if (zone == null) {
            throw new InvalidParameterValueException("unable to find zone by id " + zoneId);
        }
        // Check if zone is disabled
        Account account = CallContext.current().getCallingAccount();
        if (Grouping.AllocationState.Disabled == zone.getAllocationState() && !_accountMgr.isRootAdmin(account.getId())) {
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
            s_logger.debug("Failed to add data store: " + e.getMessage(), e);
            try {
                // clean up the db, just absorb the exception thrown in deletion with error logged, so that user can get error for adding data store
                // not deleting data store.
                if (store != null) {
                    lifeCycle.deleteDataStore(store);
                }
            } catch (Exception ex) {
                s_logger.debug("Failed to clean up storage pool: " + ex.getMessage());
            }
            throw new CloudRuntimeException("Failed to add data store: " + e.getMessage(), e);
        }

        return (PrimaryDataStoreInfo)_dataStoreMgr.getDataStore(store.getId(), DataStoreRole.Primary);
    }

    private Map<String, String> extractApiParamAsMap(Map ds) {
        Map<String, String> details = new HashMap<String, String>();
        if (ds != null) {
            Collection detailsCollection = ds.values();
            Iterator it = detailsCollection.iterator();
            while (it.hasNext()) {
                HashMap d = (HashMap)it.next();
                Iterator it2 = d.entrySet().iterator();
                while (it2.hasNext()) {
                    Map.Entry entry = (Map.Entry)it2.next();
                    details.put((String)entry.getKey(), (String)entry.getValue());
                }
            }
        }
        return details;
    }

    @ActionEvent(eventType = EventTypes.EVENT_DISABLE_PRIMARY_STORAGE, eventDescription = "disable storage pool")
    private void disablePrimaryStoragePool(StoragePoolVO primaryStorage) {
        if (!primaryStorage.getStatus().equals(StoragePoolStatus.Up)) {
            throw new InvalidParameterValueException("Primary storage with id " + primaryStorage.getId() + " cannot be disabled. Storage pool state : " + primaryStorage.getStatus().toString());
        }

        DataStoreProvider provider = _dataStoreProviderMgr.getDataStoreProvider(primaryStorage.getStorageProviderName());
        DataStoreLifeCycle dataStoreLifeCycle = provider.getDataStoreLifeCycle();
        DataStore store = _dataStoreMgr.getDataStore(primaryStorage.getId(), DataStoreRole.Primary);
        ((PrimaryDataStoreLifeCycle)dataStoreLifeCycle).disableStoragePool(store);
    }

    @ActionEvent(eventType = EventTypes.EVENT_ENABLE_PRIMARY_STORAGE, eventDescription = "enable storage pool")
    private void enablePrimaryStoragePool(StoragePoolVO primaryStorage) {
        if (!primaryStorage.getStatus().equals(StoragePoolStatus.Disabled)) {
            throw new InvalidParameterValueException("Primary storage with id " + primaryStorage.getId() + " cannot be enabled. Storage pool state : " + primaryStorage.getStatus().toString());
        }

        DataStoreProvider provider = _dataStoreProviderMgr.getDataStoreProvider(primaryStorage.getStorageProviderName());
        DataStoreLifeCycle dataStoreLifeCycle = provider.getDataStoreLifeCycle();
        DataStore store = _dataStoreMgr.getDataStore(primaryStorage.getId(), DataStoreRole.Primary);
        ((PrimaryDataStoreLifeCycle)dataStoreLifeCycle).enableStoragePool(store);
    }

    @Override
    public PrimaryDataStoreInfo updateStoragePool(UpdateStoragePoolCmd cmd) throws IllegalArgumentException {
        // Input validation
        Long id = cmd.getId();

        StoragePoolVO pool = _storagePoolDao.findById(id);
        if (pool == null) {
            throw new IllegalArgumentException("Unable to find storage pool with ID: " + id);
        }

        final List<String> storagePoolTags = cmd.getTags();
        if (storagePoolTags != null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Updating Storage Pool Tags to :" + storagePoolTags);
            }
            _storagePoolTagsDao.persist(pool.getId(), storagePoolTags);
        }

        Long updatedCapacityBytes = null;
        Long capacityBytes = cmd.getCapacityBytes();

        if (capacityBytes != null) {
            if (capacityBytes != pool.getCapacityBytes()) {
                updatedCapacityBytes = capacityBytes;
            }
        }

        Long updatedCapacityIops = null;
        Long capacityIops = cmd.getCapacityIops();

        if (capacityIops != null) {
            if (!capacityIops.equals(pool.getCapacityIops())) {
                updatedCapacityIops = capacityIops;
            }
        }

        if (updatedCapacityBytes != null || updatedCapacityIops != null) {
            StoragePoolVO storagePool = _storagePoolDao.findById(id);
            DataStoreProvider dataStoreProvider = _dataStoreProviderMgr.getDataStoreProvider(storagePool.getStorageProviderName());
            DataStoreLifeCycle dataStoreLifeCycle = dataStoreProvider.getDataStoreLifeCycle();

            if (dataStoreLifeCycle instanceof PrimaryDataStoreLifeCycle) {
                Map<String, String> details = new HashMap<String, String>();

                details.put(PrimaryDataStoreLifeCycle.CAPACITY_BYTES, updatedCapacityBytes != null ? String.valueOf(updatedCapacityBytes) : null);
                details.put(PrimaryDataStoreLifeCycle.CAPACITY_IOPS, updatedCapacityIops != null ? String.valueOf(updatedCapacityIops) : null);

                ((PrimaryDataStoreLifeCycle)dataStoreLifeCycle).updateStoragePool(storagePool, details);
            }
        }

        Boolean enabled = cmd.getEnabled();
        if (enabled != null) {
            if (enabled) {
                enablePrimaryStoragePool(pool);
            } else {
                disablePrimaryStoragePool(pool);
            }
        }

        if (updatedCapacityBytes != null) {
            _storagePoolDao.updateCapacityBytes(id, capacityBytes);
        }

        if (updatedCapacityIops != null) {
            _storagePoolDao.updateCapacityIops(id, capacityIops);
        }

        return (PrimaryDataStoreInfo)_dataStoreMgr.getDataStore(pool.getId(), DataStoreRole.Primary);
    }

    @Override
    public void removeStoragePoolFromCluster(long hostId, String iScsiName, StoragePool storagePool) {
        final Map<String, String> details = new HashMap<>();

        details.put(DeleteStoragePoolCommand.DATASTORE_NAME, iScsiName);
        details.put(DeleteStoragePoolCommand.IQN, iScsiName);
        details.put(DeleteStoragePoolCommand.STORAGE_HOST, storagePool.getHostAddress());
        details.put(DeleteStoragePoolCommand.STORAGE_PORT, String.valueOf(storagePool.getPort()));

        final DeleteStoragePoolCommand cmd = new DeleteStoragePoolCommand();

        cmd.setDetails(details);
        cmd.setRemoveDatastore(true);

        final Answer answer = _agentMgr.easySend(hostId, cmd);

        if (answer == null || !answer.getResult()) {
            String errMsg = "Error interacting with host (related to DeleteStoragePoolCommand)" + (StringUtils.isNotBlank(answer.getDetails()) ? ": " + answer.getDetails() : "");

            s_logger.error(errMsg);

            throw new CloudRuntimeException(errMsg);
        }
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

        Pair<Long, Long> vlms = _volsDao.getCountAndTotalByPool(id);
        if (forced) {
            if (vlms.first() > 0) {
                Pair<Long, Long> nonDstrdVlms = _volsDao.getNonDestroyedCountAndTotalByPool(id);
                if (nonDstrdVlms.first() > 0) {
                    throw new CloudRuntimeException("Cannot delete pool " + sPool.getName() + " as there are associated " + "non-destroyed vols for this pool");
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

        DataStoreProvider storeProvider = _dataStoreProviderMgr.getDataStoreProvider(sPool.getStorageProviderName());
        DataStoreLifeCycle lifeCycle = storeProvider.getDataStoreLifeCycle();
        DataStore store = _dataStoreMgr.getDataStore(sPool.getId(), DataStoreRole.Primary);
        return lifeCycle.deleteDataStore(store);
    }

    @Override
    public void connectHostToSharedPool(long hostId, long poolId) throws StorageUnavailableException, StorageConflictException {
        StoragePool pool = (StoragePool)_dataStoreMgr.getDataStore(poolId, DataStoreRole.Primary);
        assert (pool.isShared()) : "Now, did you actually read the name of this method?";
        s_logger.debug("Adding pool " + pool.getName() + " to  host " + hostId);

        DataStoreProvider provider = _dataStoreProviderMgr.getDataStoreProvider(pool.getStorageProviderName());
        HypervisorHostListener listener = hostListeners.get(provider.getName());
        listener.hostConnect(hostId, pool.getId());
    }

    @Override
    public BigDecimal getStorageOverProvisioningFactor(Long poolId) {
        return new BigDecimal(CapacityManager.StorageOverprovisioningFactor.valueIn(poolId));
    }

    @Override
    public void createCapacityEntry(StoragePoolVO storagePool, short capacityType, long allocated) {
        SearchCriteria<CapacityVO> capacitySC = _capacityDao.createSearchCriteria();
        capacitySC.addAnd("hostOrPoolId", SearchCriteria.Op.EQ, storagePool.getId());
        capacitySC.addAnd("dataCenterId", SearchCriteria.Op.EQ, storagePool.getDataCenterId());
        capacitySC.addAnd("capacityType", SearchCriteria.Op.EQ, capacityType);

        List<CapacityVO> capacities = _capacityDao.search(capacitySC, null);

        long totalOverProvCapacity;
        if (storagePool.getPoolType().supportsOverProvisioning()) {
            // All this is for the inaccuracy of floats for big number multiplication.
            BigDecimal overProvFactor = getStorageOverProvisioningFactor(storagePool.getId());
            totalOverProvCapacity = overProvFactor.multiply(new BigDecimal(storagePool.getCapacityBytes())).longValue();
            s_logger.debug("Found storage pool " + storagePool.getName() + " of type " + storagePool.getPoolType().toString() + " with overprovisioning factor " + overProvFactor.toString());
            s_logger.debug("Total over provisioned capacity calculated is " + overProvFactor + " * " + storagePool.getCapacityBytes());
        } else {
            s_logger.debug("Found storage pool " + storagePool.getName() + " of type " + storagePool.getPoolType().toString());
            totalOverProvCapacity = storagePool.getCapacityBytes();
        }

        s_logger.debug("Total over provisioned capacity of the pool " + storagePool.getName() + " id: " + storagePool.getId() + " is " + totalOverProvCapacity);
        CapacityState capacityState = CapacityState.Enabled;
        if (storagePool.getScope() == ScopeType.ZONE) {
            DataCenterVO dc = ApiDBUtils.findZoneById(storagePool.getDataCenterId());
            AllocationState allocationState = dc.getAllocationState();
            capacityState = (allocationState == AllocationState.Disabled) ? CapacityState.Disabled : CapacityState.Enabled;
        } else {
            if (storagePool.getClusterId() != null) {
                ClusterVO cluster = ApiDBUtils.findClusterById(storagePool.getClusterId());
                if (cluster != null) {
                    AllocationState allocationState = _configMgr.findClusterAllocationState(cluster);
                    capacityState = (allocationState == AllocationState.Disabled) ? CapacityState.Disabled : CapacityState.Enabled;
                }
            }
        }

        if (storagePool.getScope() == ScopeType.HOST) {
            List<StoragePoolHostVO> stoargePoolHostVO = _storagePoolHostDao.listByPoolId(storagePool.getId());

            if (stoargePoolHostVO != null && !stoargePoolHostVO.isEmpty()) {
                HostVO host = _hostDao.findById(stoargePoolHostVO.get(0).getHostId());

                if (host != null) {
                    capacityState = (host.getResourceState() == ResourceState.Disabled) ? CapacityState.Disabled : CapacityState.Enabled;
                }
            }
        }

        if (capacities.size() == 0) {
            CapacityVO capacity = new CapacityVO(storagePool.getId(), storagePool.getDataCenterId(), storagePool.getPodId(), storagePool.getClusterId(), allocated, totalOverProvCapacity,
                    capacityType);
            capacity.setCapacityState(capacityState);
            _capacityDao.persist(capacity);
        } else {
            CapacityVO capacity = capacities.get(0);
            if (capacity.getTotalCapacity() != totalOverProvCapacity || allocated != capacity.getUsedCapacity() || capacity.getCapacityState() != capacityState) {
                capacity.setTotalCapacity(totalOverProvCapacity);
                capacity.setUsedCapacity(allocated);
                capacity.setCapacityState(capacityState);
                _capacityDao.update(capacity.getId(), capacity);
            }
        }
        s_logger.debug("Successfully set Capacity - " + totalOverProvCapacity + " for capacity type - " + capacityType + " , DataCenterId - " + storagePool.getDataCenterId() + ", HostOrPoolId - "
                + storagePool.getId() + ", PodId " + storagePool.getPodId());
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
                    if (TemplateCleanupEnabled.value()) {
                        List<StoragePoolVO> storagePools = _storagePoolDao.listAll();
                        for (StoragePoolVO pool : storagePools) {
                            try {

                                List<VMTemplateStoragePoolVO> unusedTemplatesInPool = _tmpltMgr.getUnusedTemplatesInPool(pool);
                                s_logger.debug("Storage pool garbage collector found " + unusedTemplatesInPool.size() + " templates to clean up in storage pool: " + pool.getName());
                                for (VMTemplateStoragePoolVO templatePoolVO : unusedTemplatesInPool) {
                                    if (templatePoolVO.getDownloadState() != VMTemplateStorageResourceAssoc.Status.DOWNLOADED) {
                                        s_logger.debug("Storage pool garbage collector is skipping template with ID: " + templatePoolVO.getTemplateId() + " on pool " + templatePoolVO.getPoolId()
                                        + " because it is not completely downloaded.");
                                        continue;
                                    }

                                    if (!templatePoolVO.getMarkedForGC()) {
                                        templatePoolVO.setMarkedForGC(true);
                                        _vmTemplatePoolDao.update(templatePoolVO.getId(), templatePoolVO);
                                        s_logger.debug("Storage pool garbage collector has marked template with ID: " + templatePoolVO.getTemplateId() + " on pool " + templatePoolVO.getPoolId()
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

                    //destroy snapshots in destroying state in snapshot_store_ref
                    List<SnapshotDataStoreVO> ssSnapshots = _snapshotStoreDao.listByState(ObjectInDataStoreStateMachine.State.Destroying);
                    for (SnapshotDataStoreVO ssSnapshotVO : ssSnapshots) {
                        try {
                            _snapshotService.deleteSnapshot(snapshotFactory.getSnapshot(ssSnapshotVO.getSnapshotId(), DataStoreRole.Image));
                        } catch (Exception e) {
                            s_logger.debug("Failed to delete snapshot: " + ssSnapshotVO.getId() + " from storage");
                        }
                    }

                    cleanupSecondaryStorage(recurring);

                    List<VolumeVO> vols = _volsDao.listVolumesToBeDestroyed(new Date(System.currentTimeMillis() - ((long)StorageCleanupDelay.value() << 10)));
                    for (VolumeVO vol : vols) {
                        try {
                            // If this fails, just log a warning. It's ideal if we clean up the host-side clustered file
                            // system, but not necessary.
                            handleManagedStorage(vol);
                        } catch (Exception e) {
                            s_logger.warn("Unable to destroy host-side clustered file system " + vol.getUuid(), e);
                        }

                        try {
                            VolumeInfo volumeInfo = volFactory.getVolume(vol.getId());
                            if (volumeInfo != null) {
                                volService.expungeVolumeAsync(volumeInfo);
                            } else {
                                s_logger.debug("Volume " + vol.getUuid() + " is already destroyed");
                            }
                        } catch (Exception e) {
                            s_logger.warn("Unable to destroy volume " + vol.getUuid(), e);
                        }
                    }

                    // remove snapshots in Error state
                    List<SnapshotVO> snapshots = _snapshotDao.listAllByStatus(Snapshot.State.Error);
                    for (SnapshotVO snapshotVO : snapshots) {
                        try {
                            List<SnapshotDataStoreVO> storeRefs = _snapshotStoreDao.findBySnapshotId(snapshotVO.getId());
                            for (SnapshotDataStoreVO ref : storeRefs) {
                                _snapshotStoreDao.expunge(ref.getId());
                            }
                            _snapshotDao.expunge(snapshotVO.getId());
                        } catch (Exception e) {
                            s_logger.warn("Unable to destroy snapshot " + snapshotVO.getUuid(), e);
                        }
                    }

                    // destroy uploaded volumes in abandoned/error state
                    List<VolumeDataStoreVO> volumeDataStores = _volumeDataStoreDao.listByVolumeState(Volume.State.UploadError, Volume.State.UploadAbandoned);
                    for (VolumeDataStoreVO volumeDataStore : volumeDataStores) {
                        VolumeVO volume = _volumeDao.findById(volumeDataStore.getVolumeId());
                        if (volume == null) {
                            s_logger.warn("Uploaded volume with id " + volumeDataStore.getVolumeId() + " not found, so cannot be destroyed");
                            continue;
                        }
                        try {
                            DataStore dataStore = _dataStoreMgr.getDataStore(volumeDataStore.getDataStoreId(), DataStoreRole.Image);
                            EndPoint ep = _epSelector.select(dataStore, volumeDataStore.getExtractUrl());
                            if (ep == null) {
                                s_logger.warn("There is no secondary storage VM for image store " + dataStore.getName() + ", cannot destroy uploaded volume " + volume.getUuid());
                                continue;
                            }
                            Host host = _hostDao.findById(ep.getId());
                            if (host != null && host.getManagementServerId() != null) {
                                if (_serverId == host.getManagementServerId().longValue()) {
                                    volService.destroyVolume(volume.getId());
                                    // decrement volume resource count
                                    _resourceLimitMgr.decrementResourceCount(volume.getAccountId(), ResourceType.volume, volume.isDisplayVolume());
                                    // expunge volume from secondary if volume is on image store
                                    VolumeInfo volOnSecondary = volFactory.getVolume(volume.getId(), DataStoreRole.Image);
                                    if (volOnSecondary != null) {
                                        s_logger.info("Expunging volume " + volume.getUuid() + " uploaded using HTTP POST from secondary data store");
                                        AsyncCallFuture<VolumeApiResult> future = volService.expungeVolumeAsync(volOnSecondary);
                                        VolumeApiResult result = future.get();
                                        if (!result.isSuccess()) {
                                            s_logger.warn("Failed to expunge volume " + volume.getUuid() + " from the image store " + dataStore.getName() + " due to: " + result.getResult());
                                        }
                                    }
                                }
                            }
                        } catch (Throwable th) {
                            s_logger.warn("Unable to destroy uploaded volume " + volume.getUuid() + ". Error details: " + th.getMessage());
                        }
                    }

                    // destroy uploaded templates in abandoned/error state
                    List<TemplateDataStoreVO> templateDataStores = _templateStoreDao.listByTemplateState(VirtualMachineTemplate.State.UploadError, VirtualMachineTemplate.State.UploadAbandoned);
                    for (TemplateDataStoreVO templateDataStore : templateDataStores) {
                        VMTemplateVO template = _templateDao.findById(templateDataStore.getTemplateId());
                        if (template == null) {
                            s_logger.warn("Uploaded template with id " + templateDataStore.getTemplateId() + " not found, so cannot be destroyed");
                            continue;
                        }
                        try {
                            DataStore dataStore = _dataStoreMgr.getDataStore(templateDataStore.getDataStoreId(), DataStoreRole.Image);
                            EndPoint ep = _epSelector.select(dataStore, templateDataStore.getExtractUrl());
                            if (ep == null) {
                                s_logger.warn("There is no secondary storage VM for image store " + dataStore.getName() + ", cannot destroy uploaded template " + template.getUuid());
                                continue;
                            }
                            Host host = _hostDao.findById(ep.getId());
                            if (host != null && host.getManagementServerId() != null) {
                                if (_serverId == host.getManagementServerId().longValue()) {
                                    AsyncCallFuture<TemplateApiResult> future = _imageSrv.deleteTemplateAsync(tmplFactory.getTemplate(template.getId(), dataStore));
                                    TemplateApiResult result = future.get();
                                    if (!result.isSuccess()) {
                                        s_logger.warn("Failed to delete template " + template.getUuid() + " from the image store " + dataStore.getName() + " due to: " + result.getResult());
                                        continue;
                                    }
                                    // remove from template_zone_ref
                                    List<VMTemplateZoneVO> templateZones = _vmTemplateZoneDao.listByZoneTemplate(((ImageStoreEntity)dataStore).getDataCenterId(), template.getId());
                                    if (templateZones != null) {
                                        for (VMTemplateZoneVO templateZone : templateZones) {
                                            _vmTemplateZoneDao.remove(templateZone.getId());
                                        }
                                    }
                                    // mark all the occurrences of this template in the given store as destroyed
                                    _templateStoreDao.removeByTemplateStore(template.getId(), dataStore.getId());
                                    // find all eligible image stores for this template
                                    List<DataStore> imageStores = _tmpltMgr.getImageStoreByTemplate(template.getId(), null);
                                    if (imageStores == null || imageStores.size() == 0) {
                                        template.setState(VirtualMachineTemplate.State.Inactive);
                                        _templateDao.update(template.getId(), template);

                                        // decrement template resource count
                                        _resourceLimitMgr.decrementResourceCount(template.getAccountId(), ResourceType.template);
                                    }
                                }
                            }
                        } catch (Throwable th) {
                            s_logger.warn("Unable to destroy uploaded template " + template.getUuid() + ". Error details: " + th.getMessage());
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

    /**
     * This method only applies for managed storage.
     *
     * For XenServer and vSphere, see if we need to remove an SR or a datastore, then remove the underlying volume
     * from any applicable access control list (before other code attempts to delete the volume that supports it).
     *
     * For KVM, just tell the underlying storage plug-in to remove the volume from any applicable access control list
     * (before other code attempts to delete the volume that supports it).
     */
    private void handleManagedStorage(Volume volume) {
        Long instanceId = volume.getInstanceId();

        if (instanceId != null) {
            StoragePoolVO storagePool = _storagePoolDao.findById(volume.getPoolId());

            if (storagePool != null && storagePool.isManaged()) {
                VMInstanceVO vmInstanceVO = _vmInstanceDao.findById(instanceId);

                Long lastHostId = vmInstanceVO.getLastHostId();

                if (lastHostId != null) {
                    HostVO host = _hostDao.findById(lastHostId);
                    ClusterVO cluster = _clusterDao.findById(host.getClusterId());
                    VolumeInfo volumeInfo = volFactory.getVolume(volume.getId());

                    if (cluster.getHypervisorType() == HypervisorType.KVM) {
                        volService.revokeAccess(volumeInfo, host, volumeInfo.getDataStore());
                    } else {
                        DataTO volTO = volFactory.getVolume(volume.getId()).getTO();
                        DiskTO disk = new DiskTO(volTO, volume.getDeviceId(), volume.getPath(), volume.getVolumeType());

                        DettachCommand cmd = new DettachCommand(disk, null);

                        cmd.setManaged(true);

                        cmd.setStorageHost(storagePool.getHostAddress());
                        cmd.setStoragePort(storagePool.getPort());

                        cmd.set_iScsiName(volume.get_iScsiName());

                        Answer answer = _agentMgr.easySend(lastHostId, cmd);

                        if (answer != null && answer.getResult()) {
                            volService.revokeAccess(volumeInfo, host, volumeInfo.getDataStore());
                        } else {
                            s_logger.warn("Unable to remove host-side clustered file system for the following volume: " + volume.getUuid());
                        }
                    }
                }
            }
        }
    }

    @DB
    List<Long> findAllVolumeIdInSnapshotTable(Long storeId) {
        String sql = "SELECT volume_id from snapshots, snapshot_store_ref WHERE snapshots.id = snapshot_store_ref.snapshot_id and store_id=? GROUP BY volume_id";
        List<Long> list = new ArrayList<Long>();
        try {
            TransactionLegacy txn = TransactionLegacy.currentTxn();
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
            TransactionLegacy txn = TransactionLegacy.currentTxn();
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
            List<DataStore> imageStores = _dataStoreMgr.getImageStoresByScope(new ZoneScope(null));
            for (DataStore store : imageStores) {
                try {
                    long storeId = store.getId();
                    List<TemplateDataStoreVO> destroyedTemplateStoreVOs = _templateStoreDao.listDestroyed(storeId);
                    s_logger.debug("Secondary storage garbage collector found " + destroyedTemplateStoreVOs.size() + " templates to cleanup on template_store_ref for store: " + store.getName());
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
                    s_logger.debug("Secondary storage garbage collector found " + destroyedSnapshotStoreVOs.size() + " snapshots to cleanup on snapshot_store_ref for store: " + store.getName());
                    for (SnapshotDataStoreVO destroyedSnapshotStoreVO : destroyedSnapshotStoreVOs) {
                        // check if this snapshot has child
                        SnapshotInfo snap = snapshotFactory.getSnapshot(destroyedSnapshotStoreVO.getSnapshotId(), store);
                        if (snap.getChild() != null) {
                            s_logger.debug("Skip snapshot on store: " + destroyedSnapshotStoreVO + " , because it has child");
                            continue;
                        }

                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Deleting snapshot store DB entry: " + destroyedSnapshotStoreVO);
                        }

                        _snapshotDao.remove(destroyedSnapshotStoreVO.getSnapshotId());
                        SnapshotDataStoreVO snapshotOnPrimary = _snapshotStoreDao.findBySnapshot(destroyedSnapshotStoreVO.getSnapshotId(), DataStoreRole.Primary);
                        if (snapshotOnPrimary != null) {
                            _snapshotStoreDao.remove(snapshotOnPrimary.getId());
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
                    s_logger.debug("Secondary storage garbage collector found " + destroyedStoreVOs.size() + " volumes to cleanup on volume_store_ref for store: " + store.getName());
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
    public PrimaryDataStoreInfo preparePrimaryStorageForMaintenance(Long primaryStorageId) throws ResourceUnavailableException, InsufficientCapacityException {
        StoragePoolVO primaryStorage = null;
        primaryStorage = _storagePoolDao.findById(primaryStorageId);

        if (primaryStorage == null) {
            String msg = "Unable to obtain lock on the storage pool record in preparePrimaryStorageForMaintenance()";
            s_logger.error(msg);
            throw new InvalidParameterValueException(msg);
        }

        if (!primaryStorage.getStatus().equals(StoragePoolStatus.Up) && !primaryStorage.getStatus().equals(StoragePoolStatus.ErrorInMaintenance)) {
            throw new InvalidParameterValueException("Primary storage with id " + primaryStorageId + " is not ready for migration, as the status is:" + primaryStorage.getStatus().toString());
        }

        DataStoreProvider provider = _dataStoreProviderMgr.getDataStoreProvider(primaryStorage.getStorageProviderName());
        DataStoreLifeCycle lifeCycle = provider.getDataStoreLifeCycle();
        DataStore store = _dataStoreMgr.getDataStore(primaryStorage.getId(), DataStoreRole.Primary);
        lifeCycle.maintain(store);

        return (PrimaryDataStoreInfo)_dataStoreMgr.getDataStore(primaryStorage.getId(), DataStoreRole.Primary);
    }

    @Override
    @DB
    public PrimaryDataStoreInfo cancelPrimaryStorageForMaintenance(CancelPrimaryStorageMaintenanceCmd cmd) throws ResourceUnavailableException {
        Long primaryStorageId = cmd.getId();
        StoragePoolVO primaryStorage = null;

        primaryStorage = _storagePoolDao.findById(primaryStorageId);

        if (primaryStorage == null) {
            String msg = "Unable to obtain lock on the storage pool in cancelPrimaryStorageForMaintenance()";
            s_logger.error(msg);
            throw new InvalidParameterValueException(msg);
        }

        if (primaryStorage.getStatus().equals(StoragePoolStatus.Up) || primaryStorage.getStatus().equals(StoragePoolStatus.PrepareForMaintenance)) {
            throw new StorageUnavailableException("Primary storage with id " + primaryStorageId + " is not ready to complete migration, as the status is:" + primaryStorage.getStatus().toString(),
                    primaryStorageId);
        }

        DataStoreProvider provider = _dataStoreProviderMgr.getDataStoreProvider(primaryStorage.getStorageProviderName());
        DataStoreLifeCycle lifeCycle = provider.getDataStoreLifeCycle();
        DataStore store = _dataStoreMgr.getDataStore(primaryStorage.getId(), DataStoreRole.Primary);
        lifeCycle.cancelMaintain(store);

        return (PrimaryDataStoreInfo)_dataStoreMgr.getDataStore(primaryStorage.getId(), DataStoreRole.Primary);
    }

    protected class StorageGarbageCollector extends ManagedContextRunnable {

        public StorageGarbageCollector() {
        }

        @Override
        protected void runInContext() {
            try {
                s_logger.trace("Storage Garbage Collection Thread is running.");

                cleanupStorage(true);

            } catch (Exception e) {
                s_logger.error("Caught the following Exception", e);
            }
        }
    }

    @Override
    public void onManagementNodeJoined(List<? extends ManagementServerHost> nodeList, long selfNodeId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onManagementNodeLeft(List<? extends ManagementServerHost> nodeList, long selfNodeId) {
        for (ManagementServerHost vo : nodeList) {
            if (vo.getMsid() == _serverId) {
                s_logger.info("Cleaning up storage maintenance jobs associated with Management server: " + vo.getMsid());
                List<Long> poolIds = _storagePoolWorkDao.searchForPoolIdsForPendingWorkJobs(vo.getMsid());
                if (poolIds.size() > 0) {
                    for (Long poolId : poolIds) {
                        StoragePoolVO pool = _storagePoolDao.findById(poolId);
                        // check if pool is in an inconsistent state
                        if (pool != null && (pool.getStatus().equals(StoragePoolStatus.ErrorInMaintenance) || pool.getStatus().equals(StoragePoolStatus.PrepareForMaintenance)
                                || pool.getStatus().equals(StoragePoolStatus.CancelMaintenance))) {
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

        CapacityVO capacity = new CapacityVO(hostId, zoneId, null, null, 0, 0, Capacity.CAPACITY_TYPE_SECONDARY_STORAGE);
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

        CapacityVO capacity = new CapacityVO(poolId, zoneId, podId, clusterId, 0, 0, Capacity.CAPACITY_TYPE_STORAGE);
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
        return (PrimaryDataStoreInfo)_dataStoreMgr.getDataStore(id, DataStoreRole.Primary);
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
        sc.setParameters("type", new Object[] {StoragePoolType.Filesystem, StoragePoolType.LVM});
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
                throw new InvalidParameterValueException("uri.scheme is null " + newUrl + ", add nfs:// (or cifs://) as a prefix");
            } else if (uri.getScheme().equalsIgnoreCase("nfs")) {
                if (uri.getHost() == null || uri.getHost().equalsIgnoreCase("") || uri.getPath() == null || uri.getPath().equalsIgnoreCase("")) {
                    throw new InvalidParameterValueException("Your host and/or path is wrong.  Make sure it's of the format nfs://hostname/path");
                }
            } else if (uri.getScheme().equalsIgnoreCase("cifs")) {
                // Don't validate against a URI encoded URI.
                URI cifsUri = new URI(newUrl);
                String warnMsg = UriUtils.getCifsUriParametersProblems(cifsUri);
                if (warnMsg != null) {
                    throw new InvalidParameterValueException(warnMsg);
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
        } else if (format == ImageFormat.VHDX) {
            return HypervisorType.Hyperv;
        } else {
            return HypervisorType.None;
        }
    }

    private boolean checkUsagedSpace(StoragePool pool) {
        // Managed storage does not currently deal with accounting for physically used space (only provisioned space). Just return true if "pool" is managed.
        if (pool.isManaged()) {
            return true;
        }

        StatsCollector sc = StatsCollector.getInstance();
        double storageUsedThreshold = CapacityManager.StorageCapacityDisableThreshold.valueIn(pool.getDataCenterId());
        if (sc != null) {
            long totalSize = pool.getCapacityBytes();
            StorageStats stats = sc.getStoragePoolStats(pool.getId());
            if (stats == null) {
                stats = sc.getStorageStats(pool.getId());
            }
            if (stats != null) {
                double usedPercentage = ((double)stats.getByteUsed() / (double)totalSize);
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Checking pool " + pool.getId() + " for storage, totalSize: " + pool.getCapacityBytes() + ", usedBytes: " + stats.getByteUsed() + ", usedPct: " + usedPercentage
                            + ", disable threshold: " + storageUsedThreshold);
                }
                if (usedPercentage >= storageUsedThreshold) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Insufficient space on pool: " + pool.getId() + " since its usage percentage: " + usedPercentage + " has crossed the pool.storage.capacity.disablethreshold: "
                                + storageUsedThreshold);
                    }
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean storagePoolHasEnoughIops(List<Volume> requestedVolumes, StoragePool pool) {
        if (requestedVolumes == null || requestedVolumes.isEmpty() || pool == null) {
            return false;
        }

        // Only IOPS-guaranteed primary storage like SolidFire is using/setting IOPS.
        // This check returns true for storage that does not specify IOPS.
        if (pool.getCapacityIops() == null) {
            s_logger.info("Storage pool " + pool.getName() + " (" + pool.getId() + ") does not supply IOPS capacity, assuming enough capacity");

            return true;
        }

        StoragePoolVO storagePoolVo = _storagePoolDao.findById(pool.getId());
        long currentIops = _capacityMgr.getUsedIops(storagePoolVo);

        long requestedIops = 0;

        for (Volume requestedVolume : requestedVolumes) {
            Long minIops = requestedVolume.getMinIops();

            if (minIops != null && minIops > 0) {
                requestedIops += minIops;
            }
        }

        long futureIops = currentIops + requestedIops;

        return futureIops <= pool.getCapacityIops();
    }

    @Override
    public boolean storagePoolHasEnoughSpace(List<Volume> volumes, StoragePool pool) {
        return storagePoolHasEnoughSpace(volumes, pool, null);
    }

    @Override
    public boolean storagePoolHasEnoughSpace(List<Volume> volumes, StoragePool pool, Long clusterId) {
        if (volumes == null || volumes.isEmpty()) {
            return false;
        }

        if (!checkUsagedSpace(pool)) {
            return false;
        }

        // allocated space includes templates
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Destination pool id: " + pool.getId());
        }

        StoragePoolVO poolVO = _storagePoolDao.findById(pool.getId());
        long allocatedSizeWithTemplate = _capacityMgr.getAllocatedPoolCapacity(poolVO, null);
        long totalAskingSize = 0;

        for (Volume volume : volumes) {
            // refreshing the volume from the DB to get latest hv_ss_reserve (hypervisor snapshot reserve) field
            // I could have just assigned this to "volume", but decided to make a new variable for it so that it
            // might be clearer that this "volume" in "volumes" still might have an old value for hv_ss_reverse.
            VolumeVO volumeVO = _volumeDao.findById(volume.getId());

            if (volumeVO.getHypervisorSnapshotReserve() == null) {
                // update the volume's hv_ss_reserve (hypervisor snapshot reserve) from a disk offering (used for managed storage)
                volService.updateHypervisorSnapshotReserveForVolume(getDiskOfferingVO(volumeVO), volumeVO.getId(), getHypervisorType(volumeVO));

                // hv_ss_reserve field might have been updated; refresh from DB to make use of it in getDataObjectSizeIncludingHypervisorSnapshotReserve
                volumeVO = _volumeDao.findById(volume.getId());
            }

            // this if statement should resolve to true at most once per execution of the for loop its contained within (for a root disk that is
            // to leverage a template)
            if (volume.getTemplateId() != null) {
                VMTemplateVO tmpl = _templateDao.findByIdIncludingRemoved(volume.getTemplateId());

                if (tmpl != null && !ImageFormat.ISO.equals(tmpl.getFormat())) {
                    allocatedSizeWithTemplate = _capacityMgr.getAllocatedPoolCapacity(poolVO, tmpl);
                }
            }

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Pool ID for the volume with ID " + volumeVO.getId() + " is " + volumeVO.getPoolId());
            }

            // A ready-state volume is already allocated in a pool, so the asking size is zero for it.
            // In case the volume is moving across pools or is not ready yet, the asking size has to be computed.
            if ((volumeVO.getState() != Volume.State.Ready) || (volumeVO.getPoolId() != pool.getId())) {
                totalAskingSize += getDataObjectSizeIncludingHypervisorSnapshotReserve(volumeVO, poolVO);

                totalAskingSize += getAskingSizeForTemplateBasedOnClusterAndStoragePool(volumeVO.getTemplateId(), clusterId, poolVO);
            }
        }

        long totalOverProvCapacity;

        if (pool.getPoolType().supportsOverProvisioning()) {
            BigDecimal overProvFactor = getStorageOverProvisioningFactor(pool.getId());
            totalOverProvCapacity = overProvFactor.multiply(new BigDecimal(pool.getCapacityBytes())).longValue();

            s_logger.debug("Found storage pool " + poolVO.getName() + " of type " + pool.getPoolType().toString() + " with overprovisioning factor " + overProvFactor.toString());
            s_logger.debug("Total over provisioned capacity calculated is " + overProvFactor + " * " + pool.getCapacityBytes());
        } else {
            totalOverProvCapacity = pool.getCapacityBytes();

            s_logger.debug("Found storage pool " + poolVO.getName() + " of type " + pool.getPoolType().toString());
        }

        s_logger.debug("Total capacity of the pool " + poolVO.getName() + " with ID " + pool.getId() + " is " + totalOverProvCapacity);

        double storageAllocatedThreshold = CapacityManager.StorageAllocatedCapacityDisableThreshold.valueIn(pool.getDataCenterId());

        s_logger.debug("Checking pool: " + pool.getId() + " for volume allocation " + volumes.toString() + ", maxSize : " + totalOverProvCapacity + ", totalAllocatedSize : "
                + allocatedSizeWithTemplate + ", askingSize : " + totalAskingSize + ", allocated disable threshold: " + storageAllocatedThreshold);

        double usedPercentage = (allocatedSizeWithTemplate + totalAskingSize) / (double)(totalOverProvCapacity);

        if (usedPercentage > storageAllocatedThreshold) {
            s_logger.debug("Insufficient un-allocated capacity on: " + pool.getId() + " for volume allocation: " + volumes.toString() + " since its allocated percentage: " + usedPercentage
                    + " has crossed the allocated pool.storage.allocated.capacity.disablethreshold: " + storageAllocatedThreshold + ", skipping this pool");
            return false;
        }

        if (totalOverProvCapacity < (allocatedSizeWithTemplate + totalAskingSize)) {
            s_logger.debug("Insufficient un-allocated capacity on: " + pool.getId() + " for volume allocation: " + volumes.toString() + ", not enough storage, maxSize : " + totalOverProvCapacity
                    + ", totalAllocatedSize : " + allocatedSizeWithTemplate + ", askingSize : " + totalAskingSize);
            return false;
        }

        return true;
    }

    /**
     * Storage plug-ins for managed storage can be designed in such a way as to store a template on the primary storage once and
     * make use of it via storage-side cloning.
     *
     * This method determines how many more bytes it will need for the template (if the template is already stored on the primary storage,
     * then the answer is 0).
     */
    private long getAskingSizeForTemplateBasedOnClusterAndStoragePool(Long templateId, Long clusterId, StoragePoolVO storagePoolVO) {
        if (templateId == null || clusterId == null || storagePoolVO == null || !storagePoolVO.isManaged()) {
            return 0;
        }

        VMTemplateVO tmpl = _templateDao.findByIdIncludingRemoved(templateId);

        if (tmpl == null || ImageFormat.ISO.equals(tmpl.getFormat())) {
            return 0;
        }

        HypervisorType hypervisorType = tmpl.getHypervisorType();

        // The getSupportsResigning method is applicable for XenServer as a UUID-resigning patch may or may not be installed on those hypervisor hosts.
        if (_clusterDao.getSupportsResigning(clusterId) || HypervisorType.VMware.equals(hypervisorType) || HypervisorType.KVM.equals(hypervisorType)) {
            return getBytesRequiredForTemplate(tmpl, storagePoolVO);
        }

        return 0;
    }

    private long getDataObjectSizeIncludingHypervisorSnapshotReserve(Volume volume, StoragePool pool) {
        DataStoreProvider storeProvider = _dataStoreProviderMgr.getDataStoreProvider(pool.getStorageProviderName());
        DataStoreDriver storeDriver = storeProvider.getDataStoreDriver();

        if (storeDriver instanceof PrimaryDataStoreDriver) {
            PrimaryDataStoreDriver primaryStoreDriver = (PrimaryDataStoreDriver)storeDriver;

            VolumeInfo volumeInfo = volFactory.getVolume(volume.getId());

            return primaryStoreDriver.getDataObjectSizeIncludingHypervisorSnapshotReserve(volumeInfo, pool);
        }

        return volume.getSize();
    }

    private DiskOfferingVO getDiskOfferingVO(Volume volume) {
        Long diskOfferingId = volume.getDiskOfferingId();

        return _diskOfferingDao.findById(diskOfferingId);
    }

    private HypervisorType getHypervisorType(Volume volume) {
        Long instanceId = volume.getInstanceId();

        VMInstanceVO vmInstance = _vmInstanceDao.findById(instanceId);

        if (vmInstance != null) {
            return vmInstance.getHypervisorType();
        }

        return null;
    }

    private long getBytesRequiredForTemplate(VMTemplateVO tmpl, StoragePool pool) {
        DataStoreProvider storeProvider = _dataStoreProviderMgr.getDataStoreProvider(pool.getStorageProviderName());
        DataStoreDriver storeDriver = storeProvider.getDataStoreDriver();

        if (storeDriver instanceof PrimaryDataStoreDriver) {
            PrimaryDataStoreDriver primaryStoreDriver = (PrimaryDataStoreDriver)storeDriver;

            TemplateInfo templateInfo = tmplFactory.getReadyTemplateOnImageStore(tmpl.getId(), pool.getDataCenterId());

            return primaryStoreDriver.getBytesRequiredForTemplate(templateInfo, pool);
        }

        return tmpl.getSize();
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
    public ImageStore discoverImageStore(String name, String url, String providerName, Long zoneId, Map details) throws IllegalArgumentException, DiscoveryException, InvalidParameterValueException {
        DataStoreProvider storeProvider = _dataStoreProviderMgr.getDataStoreProvider(providerName);

        if (storeProvider == null) {
            storeProvider = _dataStoreProviderMgr.getDefaultImageDataStoreProvider();
            if (storeProvider == null) {
                throw new InvalidParameterValueException("can't find image store provider: " + providerName);
            }
            providerName = storeProvider.getName(); // ignored passed provider name and use default image store provider name
        }

        ScopeType scopeType = ScopeType.ZONE;
        if (zoneId == null) {
            scopeType = ScopeType.REGION;
        }

        if (name == null) {
            name = url;
        }

        ImageStoreVO imageStore = _imageStoreDao.findByName(name);
        if (imageStore != null) {
            throw new InvalidParameterValueException("The image store with name " + name + " already exists, try creating with another name");
        }

        // check if scope is supported by store provider
        if (!((ImageStoreProvider)storeProvider).isScopeSupported(scopeType)) {
            throw new InvalidParameterValueException("Image store provider " + providerName + " does not support scope " + scopeType);
        }

        // check if we have already image stores from other different providers,
        // we currently are not supporting image stores from different
        // providers co-existing
        List<ImageStoreVO> imageStores = _imageStoreDao.listImageStores();
        for (ImageStoreVO store : imageStores) {
            if (!store.getProviderName().equalsIgnoreCase(providerName)) {
                throw new InvalidParameterValueException("You can only add new image stores from the same provider " + store.getProviderName() + " already added");
            }
        }

        if (zoneId != null) {
            // Check if the zone exists in the system
            DataCenterVO zone = _dcDao.findById(zoneId);
            if (zone == null) {
                throw new InvalidParameterValueException("Can't find zone by id " + zoneId);
            }

            Account account = CallContext.current().getCallingAccount();
            if (Grouping.AllocationState.Disabled == zone.getAllocationState() && !_accountMgr.isRootAdmin(account.getId())) {
                PermissionDeniedException ex = new PermissionDeniedException("Cannot perform this operation, Zone with specified id is currently disabled");
                ex.addProxyObject(zone.getUuid(), "dcId");
                throw ex;
            }
        }

        Map<String, Object> params = new HashMap();
        params.put("zoneId", zoneId);
        params.put("url", url);
        params.put("name", name);
        params.put("details", details);
        params.put("scope", scopeType);
        params.put("providerName", storeProvider.getName());
        params.put("role", DataStoreRole.Image);

        DataStoreLifeCycle lifeCycle = storeProvider.getDataStoreLifeCycle();

        DataStore store;
        try {
            store = lifeCycle.initialize(params);
        } catch (Exception e) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Failed to add data store: " + e.getMessage(), e);
            }
            throw new CloudRuntimeException("Failed to add data store: " + e.getMessage(), e);
        }

        if (((ImageStoreProvider)storeProvider).needDownloadSysTemplate()) {
            // trigger system vm template download
            _imageSrv.downloadBootstrapSysTemplate(store);
        } else {
            // populate template_store_ref table
            _imageSrv.addSystemVMTemplatesToSecondary(store);
        }

        // associate builtin template with zones associated with this image store
        associateCrosszoneTemplatesToZone(zoneId);

        // duplicate cache store records to region wide storage
        if (scopeType == ScopeType.REGION) {
            duplicateCacheStoreRecordsToRegionStore(store.getId());
        }

        return (ImageStore)_dataStoreMgr.getDataStore(store.getId(), DataStoreRole.Image);
    }

    @Override
    public ImageStore migrateToObjectStore(String name, String url, String providerName, Map details) throws IllegalArgumentException, DiscoveryException, InvalidParameterValueException {
        // check if current cloud is ready to migrate, we only support cloud with only NFS secondary storages
        List<ImageStoreVO> imgStores = _imageStoreDao.listImageStores();
        List<ImageStoreVO> nfsStores = new ArrayList<ImageStoreVO>();
        if (imgStores != null && imgStores.size() > 0) {
            for (ImageStoreVO store : imgStores) {
                if (!store.getProviderName().equals(DataStoreProvider.NFS_IMAGE)) {
                    throw new InvalidParameterValueException("We only support migrate NFS secondary storage to use object store!");
                } else {
                    nfsStores.add(store);
                }
            }
        }
        // convert all NFS secondary storage to staging store
        if (nfsStores != null && nfsStores.size() > 0) {
            for (ImageStoreVO store : nfsStores) {
                long storeId = store.getId();

                _accountMgr.checkAccessAndSpecifyAuthority(CallContext.current().getCallingAccount(), store.getDataCenterId());

                DataStoreProvider provider = _dataStoreProviderMgr.getDataStoreProvider(store.getProviderName());
                DataStoreLifeCycle lifeCycle = provider.getDataStoreLifeCycle();
                DataStore secStore = _dataStoreMgr.getDataStore(storeId, DataStoreRole.Image);
                lifeCycle.migrateToObjectStore(secStore);
                // update store_role in template_store_ref and snapshot_store_ref to ImageCache
                _templateStoreDao.updateStoreRoleToCachce(storeId);
                _snapshotStoreDao.updateStoreRoleToCache(storeId);
            }
        }
        // add object store
        return discoverImageStore(name, url, providerName, null, details);
    }

    private void duplicateCacheStoreRecordsToRegionStore(long storeId) {
        _templateStoreDao.duplicateCacheRecordsOnRegionStore(storeId);
        _snapshotStoreDao.duplicateCacheRecordsOnRegionStore(storeId);
        _volumeStoreDao.duplicateCacheRecordsOnRegionStore(storeId);
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
        final long storeId = cmd.getId();
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
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                // first delete from image_store_details table, we need to do that since
                // we are not actually deleting record from main
                // image_data_store table, so delete cascade will not work
                _imageStoreDetailsDao.deleteDetails(storeId);
                _snapshotStoreDao.deletePrimaryRecordsForStore(storeId, DataStoreRole.Image);
                _volumeStoreDao.deletePrimaryRecordsForStore(storeId);
                _templateStoreDao.deletePrimaryRecordsForStore(storeId);
                _imageStoreDao.remove(storeId);
            }
        });

        return true;
    }

    @Override
    public ImageStore createSecondaryStagingStore(CreateSecondaryStagingStoreCmd cmd) {
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
        if (Grouping.AllocationState.Disabled == zone.getAllocationState() && !_accountMgr.isRootAdmin(account.getId())) {
            PermissionDeniedException ex = new PermissionDeniedException("Cannot perform this operation, Zone with specified id is currently disabled");
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
            s_logger.debug("Failed to add data store: " + e.getMessage(), e);
            throw new CloudRuntimeException("Failed to add data store: " + e.getMessage(), e);
        }

        return (ImageStore)_dataStoreMgr.getDataStore(store.getId(), DataStoreRole.ImageCache);
    }

    @Override
    public boolean deleteSecondaryStagingStore(DeleteSecondaryStagingStoreCmd cmd) {
        final long storeId = cmd.getId();
        // Verify that cache store exists
        ImageStoreVO store = _imageStoreDao.findById(storeId);
        if (store == null) {
            throw new InvalidParameterValueException("Cache store with id " + storeId + " doesn't exist");
        }
        _accountMgr.checkAccessAndSpecifyAuthority(CallContext.current().getCallingAccount(), store.getDataCenterId());

        // Verify that there are no live snapshot, template, volume on the cache
        // store that is currently referenced
        List<SnapshotDataStoreVO> snapshots = _snapshotStoreDao.listActiveOnCache(storeId);
        if (snapshots != null && snapshots.size() > 0) {
            throw new InvalidParameterValueException("Cannot delete cache store with staging snapshots currently in use!");
        }
        List<VolumeDataStoreVO> volumes = _volumeStoreDao.listActiveOnCache(storeId);
        if (volumes != null && volumes.size() > 0) {
            throw new InvalidParameterValueException("Cannot delete cache store with staging volumes currently in use!");
        }

        List<TemplateDataStoreVO> templates = _templateStoreDao.listActiveOnCache(storeId);
        if (templates != null && templates.size() > 0) {
            throw new InvalidParameterValueException("Cannot delete cache store with staging templates currently in use!");
        }

        // ready to delete
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                // first delete from image_store_details table, we need to do that since
                // we are not actually deleting record from main
                // image_data_store table, so delete cascade will not work
                _imageStoreDetailsDao.deleteDetails(storeId);
                _snapshotStoreDao.deletePrimaryRecordsForStore(storeId, DataStoreRole.ImageCache);
                _volumeStoreDao.deletePrimaryRecordsForStore(storeId);
                _templateStoreDao.deletePrimaryRecordsForStore(storeId);
                _imageStoreDao.remove(storeId);
            }
        });

        return true;
    }

    protected class DownloadURLGarbageCollector implements Runnable {

        public DownloadURLGarbageCollector() {
        }

        @Override
        public void run() {
            try {
                s_logger.trace("Download URL Garbage Collection Thread is running.");

                cleanupDownloadUrls();

            } catch (Exception e) {
                s_logger.error("Caught the following Exception", e);
            }
        }
    }

    @Override
    public void cleanupDownloadUrls() {

        // Cleanup expired volume URLs
        List<VolumeDataStoreVO> volumesOnImageStoreList = _volumeStoreDao.listVolumeDownloadUrls();
        HashSet<Long> expiredVolumeIds = new HashSet<Long>();
        HashSet<Long> activeVolumeIds = new HashSet<Long>();
        for (VolumeDataStoreVO volumeOnImageStore : volumesOnImageStoreList) {

            long volumeId = volumeOnImageStore.getVolumeId();
            try {
                long downloadUrlCurrentAgeInSecs = DateUtil.getTimeDifference(DateUtil.now(), volumeOnImageStore.getExtractUrlCreated());
                if (downloadUrlCurrentAgeInSecs < _downloadUrlExpirationInterval) {  // URL hasnt expired yet
                    activeVolumeIds.add(volumeId);
                    continue;
                }
                expiredVolumeIds.add(volumeId);
                s_logger.debug("Removing download url " + volumeOnImageStore.getExtractUrl() + " for volume id " + volumeId);

                // Remove it from image store
                ImageStoreEntity secStore = (ImageStoreEntity)_dataStoreMgr.getDataStore(volumeOnImageStore.getDataStoreId(), DataStoreRole.Image);
                secStore.deleteExtractUrl(volumeOnImageStore.getInstallPath(), volumeOnImageStore.getExtractUrl(), Upload.Type.VOLUME);

                // Now expunge it from DB since this entry was created only for download purpose
                _volumeStoreDao.expunge(volumeOnImageStore.getId());
            } catch (Throwable th) {
                s_logger.warn("Caught exception while deleting download url " + volumeOnImageStore.getExtractUrl() + " for volume id " + volumeOnImageStore.getVolumeId(), th);
            }
        }
        for (Long volumeId : expiredVolumeIds) {
            if (activeVolumeIds.contains(volumeId)) {
                continue;
            }
            Volume volume = _volumeDao.findById(volumeId);
            if (volume != null && volume.getState() == Volume.State.Expunged) {
                _volumeDao.remove(volumeId);
            }
        }

        // Cleanup expired template URLs
        List<TemplateDataStoreVO> templatesOnImageStoreList = _templateStoreDao.listTemplateDownloadUrls();
        for (TemplateDataStoreVO templateOnImageStore : templatesOnImageStoreList) {

            try {
                long downloadUrlCurrentAgeInSecs = DateUtil.getTimeDifference(DateUtil.now(), templateOnImageStore.getExtractUrlCreated());
                if (downloadUrlCurrentAgeInSecs < _downloadUrlExpirationInterval) {  // URL hasnt expired yet
                    continue;
                }

                s_logger.debug("Removing download url " + templateOnImageStore.getExtractUrl() + " for template id " + templateOnImageStore.getTemplateId());

                // Remove it from image store
                ImageStoreEntity secStore = (ImageStoreEntity)_dataStoreMgr.getDataStore(templateOnImageStore.getDataStoreId(), DataStoreRole.Image);
                secStore.deleteExtractUrl(templateOnImageStore.getInstallPath(), templateOnImageStore.getExtractUrl(), Upload.Type.TEMPLATE);

                // Now remove download details from DB.
                templateOnImageStore.setExtractUrl(null);
                templateOnImageStore.setExtractUrlCreated(null);
                _templateStoreDao.update(templateOnImageStore.getId(), templateOnImageStore);
            } catch (Throwable th) {
                s_logger.warn("caught exception while deleting download url " + templateOnImageStore.getExtractUrl() + " for template id " + templateOnImageStore.getTemplateId(), th);
            }
        }
    }

    // get bytesReadRate from service_offering, disk_offering and vm.disk.throttling.bytes_read_rate
    @Override
    public Long getDiskBytesReadRate(final ServiceOffering offering, final DiskOffering diskOffering) {
        if ((offering != null) && (offering.getBytesReadRate() != null) && (offering.getBytesReadRate() > 0)) {
            return offering.getBytesReadRate();
        } else if ((diskOffering != null) && (diskOffering.getBytesReadRate() != null) && (diskOffering.getBytesReadRate() > 0)) {
            return diskOffering.getBytesReadRate();
        } else {
            Long bytesReadRate = Long.parseLong(_configDao.getValue(Config.VmDiskThrottlingBytesReadRate.key()));
            if ((bytesReadRate > 0) && ((offering == null) || (!offering.getSystemUse()))) {
                return bytesReadRate;
            }
        }
        return 0L;
    }

    // get bytesWriteRate from service_offering, disk_offering and vm.disk.throttling.bytes_write_rate
    @Override
    public Long getDiskBytesWriteRate(final ServiceOffering offering, final DiskOffering diskOffering) {
        if ((offering != null) && (offering.getBytesWriteRate() != null) && (offering.getBytesWriteRate() > 0)) {
            return offering.getBytesWriteRate();
        } else if ((diskOffering != null) && (diskOffering.getBytesWriteRate() != null) && (diskOffering.getBytesWriteRate() > 0)) {
            return diskOffering.getBytesWriteRate();
        } else {
            Long bytesWriteRate = Long.parseLong(_configDao.getValue(Config.VmDiskThrottlingBytesWriteRate.key()));
            if ((bytesWriteRate > 0) && ((offering == null) || (!offering.getSystemUse()))) {
                return bytesWriteRate;
            }
        }
        return 0L;
    }

    // get iopsReadRate from service_offering, disk_offering and vm.disk.throttling.iops_read_rate
    @Override
    public Long getDiskIopsReadRate(final ServiceOffering offering, final DiskOffering diskOffering) {
        if ((offering != null) && (offering.getIopsReadRate() != null) && (offering.getIopsReadRate() > 0)) {
            return offering.getIopsReadRate();
        } else if ((diskOffering != null) && (diskOffering.getIopsReadRate() != null) && (diskOffering.getIopsReadRate() > 0)) {
            return diskOffering.getIopsReadRate();
        } else {
            Long iopsReadRate = Long.parseLong(_configDao.getValue(Config.VmDiskThrottlingIopsReadRate.key()));
            if ((iopsReadRate > 0) && ((offering == null) || (!offering.getSystemUse()))) {
                return iopsReadRate;
            }
        }
        return 0L;
    }

    // get iopsWriteRate from service_offering, disk_offering and vm.disk.throttling.iops_write_rate
    @Override
    public Long getDiskIopsWriteRate(final ServiceOffering offering, final DiskOffering diskOffering) {
        if ((offering != null) && (offering.getIopsWriteRate() != null) && (offering.getIopsWriteRate() > 0)) {
            return offering.getIopsWriteRate();
        } else if ((diskOffering != null) && (diskOffering.getIopsWriteRate() != null) && (diskOffering.getIopsWriteRate() > 0)) {
            return diskOffering.getIopsWriteRate();
        } else {
            Long iopsWriteRate = Long.parseLong(_configDao.getValue(Config.VmDiskThrottlingIopsWriteRate.key()));
            if ((iopsWriteRate > 0) && ((offering == null) || (!offering.getSystemUse()))) {
                return iopsWriteRate;
            }
        }
        return 0L;
    }

    @Override
    public String getConfigComponentName() {
        return StorageManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {StorageCleanupInterval, StorageCleanupDelay, StorageCleanupEnabled, TemplateCleanupEnabled};
    }

    @Override
    public void setDiskProfileThrottling(DiskProfile dskCh, final ServiceOffering offering, final DiskOffering diskOffering) {
        dskCh.setBytesReadRate(getDiskBytesReadRate(offering, diskOffering));
        dskCh.setBytesWriteRate(getDiskBytesWriteRate(offering, diskOffering));
        dskCh.setIopsReadRate(getDiskIopsReadRate(offering, diskOffering));
        dskCh.setIopsWriteRate(getDiskIopsWriteRate(offering, diskOffering));
    }

    @Override
    public DiskTO getDiskWithThrottling(final DataTO volTO, final Volume.Type volumeType, final long deviceId, final String path, final long offeringId, final long diskOfferingId) {
        DiskTO disk = null;
        if (volTO != null && volTO instanceof VolumeObjectTO) {
            VolumeObjectTO volumeTO = (VolumeObjectTO)volTO;
            ServiceOffering offering = _entityMgr.findById(ServiceOffering.class, offeringId);
            DiskOffering diskOffering = _entityMgr.findById(DiskOffering.class, diskOfferingId);
            if (volumeType == Volume.Type.ROOT) {
                setVolumeObjectTOThrottling(volumeTO, offering, diskOffering);
            } else {
                setVolumeObjectTOThrottling(volumeTO, null, diskOffering);
            }
            disk = new DiskTO(volumeTO, deviceId, path, volumeType);
        } else {
            disk = new DiskTO(volTO, deviceId, path, volumeType);
        }
        return disk;
    }

    private void setVolumeObjectTOThrottling(VolumeObjectTO volumeTO, final ServiceOffering offering, final DiskOffering diskOffering) {
        volumeTO.setBytesReadRate(getDiskBytesReadRate(offering, diskOffering));
        volumeTO.setBytesWriteRate(getDiskBytesWriteRate(offering, diskOffering));
        volumeTO.setIopsReadRate(getDiskIopsReadRate(offering, diskOffering));
        volumeTO.setIopsWriteRate(getDiskIopsWriteRate(offering, diskOffering));
    }

}
