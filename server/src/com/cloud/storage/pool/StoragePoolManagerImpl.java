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
package com.cloud.storage.pool;

import java.math.BigDecimal;
import java.net.Inet6Address;
import java.net.InetAddress;
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
import com.cloud.agent.api.storage.DeleteVolumeCommand;
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
import com.cloud.api.commands.UploadVolumeCmd;
import com.cloud.async.AsyncJobManager;
import com.cloud.capacity.Capacity;
import com.cloud.capacity.CapacityManager;
import com.cloud.capacity.CapacityState;
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
import com.cloud.org.Grouping;
import com.cloud.org.Grouping.AllocationState;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceState;
import com.cloud.server.ManagementServer;
import com.cloud.server.ResourceTag.TaggedResourceType;
import com.cloud.server.StatsCollector;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.LocalStoragePoolListener;
import com.cloud.storage.OCFS2Manager;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.StoragePoolDiscoverer;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.StoragePoolWorkVO;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeHostVO;
import com.cloud.storage.VolumeVO;
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
import com.cloud.storage.dao.VolumeHostDao;
import com.cloud.storage.download.DownloadMonitor;
import com.cloud.storage.listener.StoragePoolMonitor;
import com.cloud.storage.pool.Storage.ImageFormat;
import com.cloud.storage.pool.Storage.StoragePoolType;
import com.cloud.storage.secondary.SecondaryStorageVmManager;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.storage.snapshot.SnapshotScheduler;
import com.cloud.storage.volume.Volume;
import com.cloud.storage.volume.Volume.Event;
import com.cloud.storage.volume.Volume.Type;
import com.cloud.storage.volume.VolumeManager;
import com.cloud.tags.ResourceTagVO;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.template.TemplateManager;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.User;
import com.cloud.user.UserContext;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.EnumUtils;
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

@Local(value = { StoragePoolManager.class, StoragePoolService.class })
public class StoragePoolManagerImpl implements StoragePoolManager, Manager, ClusterManagerListener {
    private static final Logger s_logger = Logger.getLogger(StoragePoolManagerImpl.class);

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
    VolumeHostDao _volumeHostDao;
    @Inject
    protected VMInstanceDao _vmInstanceDao;
    @Inject
    protected StoragePoolDao _storagePoolDao = null;
    @Inject
    protected CapacityDao _capacityDao;
    @Inject
    protected CapacityManager _capacityMgr;
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
    @Inject
    protected DownloadMonitor _downloadMonitor;
    @Inject
    protected ResourceTagDao _resourceTagDao;
    @Inject
    protected VolumeManager _volumeManager;

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

    private long _serverId;
    private StateMachine2<Volume.State, Volume.Event, Volume> _volStateMachine;


    private double _storageUsedThreshold = 1.0d;
    private double _storageAllocatedThreshold = 1.0d;
    protected BigDecimal _storageOverprovisioningFactor = new BigDecimal(1);

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
    
    @Override
    public StoragePool findStoragePool(DataCenterVO dc, DiskProfile diskProfile, Account account, HashSet<StoragePool> poolsToAvoid) {
		StoragePoolVO pool = null;
		Set<Long> podsToAvoid = new HashSet<Long>();
		Pair<HostPodVO, Long> pod = null;
		// Determine what pod to store the volume in
		while ((pod = _resourceMgr.findPod(null, null, dc, account.getId(), podsToAvoid)) != null) {
			podsToAvoid.add(pod.first().getId());
			// Determine what storage pool to store the volume in
			while ((pool = findStoragePool(diskProfile, dc, pod.first(), null, null, poolsToAvoid)) != null) {
				poolsToAvoid.add(pool);
				return pool;
			}
		}
		return null;
    }

    @Override
    public StoragePoolVO findStoragePool(DiskProfile dskCh, final DataCenterVO dc, HostPodVO pod, Long clusterId, VMInstanceVO vm, final Set<StoragePool> avoid) {

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

        String storageUsedThreshold = configDao.getValue(Config.StorageCapacityDisableThreshold.key());
        if (storageUsedThreshold != null) {
            _storageUsedThreshold = Double.parseDouble(storageUsedThreshold);
        }

        String storageAllocatedThreshold = configDao.getValue(Config.StorageAllocatedCapacityDisableThreshold.key());
        if (storageAllocatedThreshold != null) {
            _storageAllocatedThreshold = Double.parseDouble(storageAllocatedThreshold);
        }

        String globalStorageOverprovisioningFactor = configs.get("storage.overprovisioning.factor");
        _storageOverprovisioningFactor = new BigDecimal(NumbersUtil.parseFloat(globalStorageOverprovisioningFactor, 2.0f));

        s_logger.info("Storage cleanup enabled: " + _storageCleanupEnabled + ", interval: " + _storageCleanupInterval + ", template cleanup enabled: " + _templateCleanupEnabled);

        String workers = configs.get("expunge.workers");
        int wrks = NumbersUtil.parseInt(workers, 10);
        _executor = Executors.newScheduledThreadPool(wrks, new NamedThreadFactory("StorageManager-Scavenger"));

        boolean localStorage = Boolean.parseBoolean(configs.get(Config.UseLocalStorage.key()));
        if (localStorage) {
            _agentMgr.registerForHostEvents(ComponentLocator.inject(LocalStoragePoolListener.class), true, false, false);
        }





   



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
            }  else if (uri.getScheme().equalsIgnoreCase("rbd")) {
                String uriPath = uri.getPath();
                if (uriPath == null) {
                    throw new InvalidParameterValueException("host or path is null, should be rbd://hostname/pool");
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
        String userInfo = uri.getUserInfo();
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
        } else if (scheme.equalsIgnoreCase("rbd")) {
            if (port == -1) {
                port = 6789;
            }
            pool = new StoragePoolVO(StoragePoolType.RBD, storageHost, port, hostPath.replaceFirst("/", ""), userInfo);
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
    public boolean deletePool(DeletePoolCmd cmd) {
        Long id = cmd.getId();
        boolean deleteFlag = false;
        boolean forced = cmd.isForced();

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

        Pair<Long, Long> vlms = _volsDao.getCountAndTotalByPool(id);
        if (forced) {
            if (vlms.first() > 0) {
                Pair<Long, Long> nonDstrdVlms = _volsDao.getNonDestroyedCountAndTotalByPool(id);
                if (nonDstrdVlms.first() > 0) {
                    throw new CloudRuntimeException("Cannot delete pool " + sPool.getName() + " as there are associated " +
                            "non-destroyed vols for this pool");
                }
                //force expunge non-destroyed volumes
                List<VolumeVO> vols = _volsDao.listVolumesToBeDestroyed();
                for (VolumeVO vol : vols) {
                	_volumeManager.expungeVolume(vol, true);
                }
            }
        } else {
            // Check if the pool has associated volumes in the volumes table
            // If it does , then you cannot delete the pool
            if (vlms.first() > 0) {
                throw new CloudRuntimeException("Cannot delete pool " + sPool.getName() + " as there are associated vols" +
                		" for this pool");
            }
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
                    DeleteStoragePoolCommand deleteCmd = new DeleteStoragePoolCommand(sPool);
                    final Answer answer = _agentMgr.easySend(host.getHostId(), deleteCmd);

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
                && pool.getPoolType() != StoragePoolType.PreSetup && pool.getPoolType() != StoragePoolType.OCFS2 && pool.getPoolType() != StoragePoolType.RBD && pool.getPoolType() != StoragePoolType.CLVM) {
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
            totalOverProvCapacity = _overProvisioningFactor.multiply(new BigDecimal(storagePool.getCapacityBytes())).longValue();// All this for the inaccuracy of floats for big number multiplication.
        } else {
            totalOverProvCapacity = storagePool.getCapacityBytes();
        }

        if (capacities.size() == 0) {
            CapacityVO capacity = new CapacityVO(storagePool.getId(), storagePool.getDataCenterId(), storagePool.getPodId(), storagePool.getClusterId(), allocated, totalOverProvCapacity, capacityType);
            CapacityState capacityState = _configMgr.findClusterAllocationState(ApiDBUtils.findClusterById(storagePool.getClusterId())) == AllocationState.Disabled ?
            				CapacityState.Disabled : CapacityState.Enabled;
            capacity.setCapacityState(capacityState);
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
                            _volumeManager.expungeVolume(vol, false);
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
            
            //CleanUp volumes on Secondary Storage.
            for (HostVO secondaryStorageHost : secondaryStorageHosts) {
                try {
                    long hostId = secondaryStorageHost.getId();
                    List<VolumeHostVO> destroyedVolumeHostVOs = _volumeHostDao.listDestroyed(hostId);
                    s_logger.debug("Secondary storage garbage collector found " + destroyedVolumeHostVOs.size() + " templates to cleanup on secondary storage host: "
                            + secondaryStorageHost.getName());
                    for (VolumeHostVO destroyedVolumeHostVO : destroyedVolumeHostVOs) {
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Deleting volume host: " + destroyedVolumeHostVO);
                        }

                        String installPath = destroyedVolumeHostVO.getInstallPath();

                        if (installPath != null) {
                            Answer answer = _agentMgr.sendToSecStorage(secondaryStorageHost, new DeleteVolumeCommand(secondaryStorageHost.getStorageUrl(), destroyedVolumeHostVO.getInstallPath()));

                            if (answer == null || !answer.getResult()) {
                                s_logger.debug("Failed to delete " + destroyedVolumeHostVO + " due to " + ((answer == null) ? "answer is null" : answer.getDetails()));
                            } else {
                                _volumeHostDao.remove(destroyedVolumeHostVO.getId());
                                s_logger.debug("Deleted volume at: " + destroyedVolumeHostVO.getInstallPath());
                            }
                        } else {
                            _volumeHostDao.remove(destroyedVolumeHostVO.getId());
                        }
                    }
                
                }catch (Exception e2) {
                    s_logger.warn("problem cleaning up volumes in secondary storage " + secondaryStorageHost, e2);
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
    public StoragePoolVO prepareStorageForMaintenance(Long primaryStorageId) throws ResourceUnavailableException, InsufficientCapacityException {
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
    public StoragePoolVO cancelStorageForMaintenance(CancelPrimaryStorageMaintenanceCmd cmd) throws ResourceUnavailableException {
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




    @Override
    public void release(VirtualMachineProfile<? extends VMInstanceVO> profile) {
        // add code here
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
    public String getSupportedImageFormatForCluster(Long clusterId) {
        ClusterVO cluster = ApiDBUtils.findClusterById(clusterId);

        if (cluster.getHypervisorType() == HypervisorType.XenServer) {
            return "vhd";
        } else if (cluster.getHypervisorType() == HypervisorType.KVM) {
            return "qcow2";
        } else if (cluster.getHypervisorType() == HypervisorType.VMware) {
            return "ova";
        } else if (cluster.getHypervisorType() == HypervisorType.Ovm) {
            return "raw";
        } else {
            return null;
        }
    }
    
    @Override
    public HypervisorType getHypervisorTypeFromFormat(ImageFormat format) {
        
    	if(format == null) {
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

    private boolean checkUsagedSpace(StoragePool pool){
        StatsCollector sc = StatsCollector.getInstance();
        if (sc != null) {
            long totalSize = pool.getCapacityBytes();
            StorageStats stats = sc.getStoragePoolStats(pool.getId());
            if(stats == null){
                stats = sc.getStorageStats(pool.getId());
            }
            if (stats != null) {
                double usedPercentage = ((double)stats.getByteUsed() / (double)totalSize);
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Checking pool " + pool.getId() + " for storage, totalSize: " + pool.getCapacityBytes() + ", usedBytes: " + stats.getByteUsed() + ", usedPct: " + usedPercentage + ", disable threshold: " + _storageUsedThreshold);
                }
                if (usedPercentage >= _storageUsedThreshold) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Insufficient space on pool: " + pool.getId() + " since its usage percentage: " +usedPercentage + " has crossed the pool.storage.capacity.disablethreshold: " + _storageUsedThreshold);
                    }
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean storagePoolHasEnoughSpace(List<Volume> volumes, StoragePool pool) {
        if(volumes == null || volumes.isEmpty())
            return false;

        if(!checkUsagedSpace(pool))
            return false;

        // allocated space includes template of specified volume
        StoragePoolVO poolVO = _storagePoolDao.findById(pool.getId());
        long allocatedSizeWithtemplate = _capacityMgr.getAllocatedPoolCapacity(poolVO, null);
        long totalAskingSize = 0;
        for (Volume volume : volumes) {
            if(volume.getTemplateId()!=null){
                VMTemplateVO tmpl = _templateDao.findById(volume.getTemplateId());
                if (tmpl.getFormat() != ImageFormat.ISO){
                    allocatedSizeWithtemplate = _capacityMgr.getAllocatedPoolCapacity(poolVO, tmpl);
                }
            }
            if(volume.getState() != Volume.State.Ready)
                totalAskingSize = totalAskingSize + volume.getSize();
        }

        long totalOverProvCapacity;
        if (pool.getPoolType() == StoragePoolType.NetworkFilesystem) {
            totalOverProvCapacity = _storageOverprovisioningFactor.multiply(new BigDecimal(pool.getCapacityBytes())).longValue();// All this for the inaccuracy of floats for big number multiplication.
        }else {
            totalOverProvCapacity = pool.getCapacityBytes();
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Checking pool: " + pool.getId() + " for volume allocation " + volumes.toString() + ", maxSize : " + totalOverProvCapacity + ", totalAllocatedSize : " + allocatedSizeWithtemplate + ", askingSize : " + totalAskingSize + ", allocated disable threshold: " + _storageAllocatedThreshold);
        }

        double usedPercentage = (allocatedSizeWithtemplate + totalAskingSize) / (double)(totalOverProvCapacity);
        if (usedPercentage > _storageAllocatedThreshold){
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Insufficient un-allocated capacity on: " + pool.getId() + " for volume allocation: " + volumes.toString() + " since its allocated percentage: " +usedPercentage + " has crossed the allocated pool.storage.allocated.capacity.disablethreshold: " + _storageAllocatedThreshold + ", skipping this pool");
            }
            return false;
        }

        if (totalOverProvCapacity < (allocatedSizeWithtemplate + totalAskingSize)) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Insufficient un-allocated capacity on: " + pool.getId() + " for volume allocation: " + volumes.toString() + ", not enough storage, maxSize : " + totalOverProvCapacity + ", totalAllocatedSize : " + allocatedSizeWithtemplate + ", askingSize : " + totalAskingSize);
            }
            return false;
        }
        return true;
    }
    
    @Override
    public StoragePool getStoragePoolById(Long id) {
    	StoragePool pool = _storagePoolDao.findById(id);
    	return pool;
    }
}
