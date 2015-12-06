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
package com.cloud.server;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.cloudstack.utils.usage.UsageUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.utils.graphite.GraphiteClient;
import org.apache.cloudstack.utils.graphite.GraphiteException;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.GetStorageStatsCommand;
import com.cloud.agent.api.HostStatsEntry;
import com.cloud.agent.api.PerformanceMonitorCommand;
import com.cloud.agent.api.VgpuTypesInfo;
import com.cloud.agent.api.VmDiskStatsEntry;
import com.cloud.agent.api.VmStatsEntry;
import com.cloud.cluster.ManagementServerHostVO;
import com.cloud.cluster.dao.ManagementServerHostDao;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.gpu.dao.HostGpuGroupsDao;
import com.cloud.host.Host;
import com.cloud.host.HostStats;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.as.AutoScaleManager;
import com.cloud.network.as.AutoScalePolicyConditionMapVO;
import com.cloud.network.as.AutoScalePolicyVO;
import com.cloud.network.as.AutoScaleVmGroupPolicyMapVO;
import com.cloud.network.as.AutoScaleVmGroupVO;
import com.cloud.network.as.AutoScaleVmGroupVmMapVO;
import com.cloud.network.as.AutoScaleVmProfileVO;
import com.cloud.network.as.Condition.Operator;
import com.cloud.network.as.ConditionVO;
import com.cloud.network.as.Counter;
import com.cloud.network.as.CounterVO;
import com.cloud.network.as.dao.AutoScalePolicyConditionMapDao;
import com.cloud.network.as.dao.AutoScalePolicyDao;
import com.cloud.network.as.dao.AutoScaleVmGroupDao;
import com.cloud.network.as.dao.AutoScaleVmGroupPolicyMapDao;
import com.cloud.network.as.dao.AutoScaleVmGroupVmMapDao;
import com.cloud.network.as.dao.AutoScaleVmProfileDao;
import com.cloud.network.as.dao.ConditionDao;
import com.cloud.network.as.dao.CounterDao;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceState;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StorageStats;
import com.cloud.storage.VolumeStats;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.VmDiskStatisticsVO;
import com.cloud.user.dao.VmDiskStatisticsDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentMethodInterceptable;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.net.MacAddress;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VmStats;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

/**
 * Provides real time stats for various agent resources up to x seconds
 *
 */
@Component
public class StatsCollector extends ManagerBase implements ComponentMethodInterceptable {

    public static enum ExternalStatsProtocol {
        NONE("none"), GRAPHITE("graphite");
        String _type;

        ExternalStatsProtocol(String type) {
            _type = type;
        }

        @Override
        public String toString() {
            return _type;
        }
    }

    public static final Logger s_logger = Logger.getLogger(StatsCollector.class.getName());

    private static StatsCollector s_instance = null;

    private ScheduledExecutorService _executor = null;
    @Inject
    private AgentManager _agentMgr;
    @Inject
    private UserVmManager _userVmMgr;
    @Inject
    private HostDao _hostDao;
    @Inject
    private UserVmDao _userVmDao;
    @Inject
    private VolumeDao _volsDao;
    @Inject
    private PrimaryDataStoreDao _storagePoolDao;
    @Inject
    private ImageStoreDao _imageStoreDao;
    @Inject
    private StorageManager _storageManager;
    @Inject
    private StoragePoolHostDao _storagePoolHostDao;
    @Inject
    private DataStoreManager _dataStoreMgr;
    @Inject
    private ResourceManager _resourceMgr;
    @Inject
    private ConfigurationDao _configDao;
    @Inject
    private EndPointSelector _epSelector;
    @Inject
    private VmDiskStatisticsDao _vmDiskStatsDao;
    @Inject
    private ManagementServerHostDao _msHostDao;
    @Inject
    private AutoScaleVmGroupDao _asGroupDao;
    @Inject
    private AutoScaleVmGroupVmMapDao _asGroupVmDao;
    @Inject
    private AutoScaleManager _asManager;
    @Inject
    private VMInstanceDao _vmInstance;
    @Inject
    private AutoScaleVmGroupPolicyMapDao _asGroupPolicyDao;
    @Inject
    private AutoScalePolicyDao _asPolicyDao;
    @Inject
    private AutoScalePolicyConditionMapDao _asConditionMapDao;
    @Inject
    private ConditionDao _asConditionDao;
    @Inject
    private CounterDao _asCounterDao;
    @Inject
    private AutoScaleVmProfileDao _asProfileDao;
    @Inject
    private ServiceOfferingDao _serviceOfferingDao;
    @Inject
    private HostGpuGroupsDao _hostGpuGroupsDao;

    private ConcurrentHashMap<Long, HostStats> _hostStats = new ConcurrentHashMap<Long, HostStats>();
    private final ConcurrentHashMap<Long, VmStats> _VmStats = new ConcurrentHashMap<Long, VmStats>();
    private final ConcurrentHashMap<Long, VolumeStats> _volumeStats = new ConcurrentHashMap<Long, VolumeStats>();
    private ConcurrentHashMap<Long, StorageStats> _storageStats = new ConcurrentHashMap<Long, StorageStats>();
    private ConcurrentHashMap<Long, StorageStats> _storagePoolStats = new ConcurrentHashMap<Long, StorageStats>();

    long hostStatsInterval = -1L;
    long hostAndVmStatsInterval = -1L;
    long storageStatsInterval = -1L;
    long volumeStatsInterval = -1L;
    long autoScaleStatsInterval = -1L;
    int vmDiskStatsInterval = 0;
    List<Long> hostIds = null;
    private double _imageStoreCapacityThreshold = 0.90;

    String externalStatsPrefix = "";
    String externalStatsHost = null;
    int externalStatsPort = -1;
    boolean externalStatsEnabled = false;
    ExternalStatsProtocol externalStatsType = ExternalStatsProtocol.NONE;

    private ScheduledExecutorService _diskStatsUpdateExecutor;
    private int _usageAggregationRange = 1440;
    private String _usageTimeZone = "GMT";
    private final long mgmtSrvrId = MacAddress.getMacAddress().toLong();
    private static final int ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION = 5;    // 5 seconds
    private boolean _dailyOrHourly = false;

    //private final GlobalLock m_capacityCheckLock = GlobalLock.getInternLock("capacity.check");

    public static StatsCollector getInstance() {
        return s_instance;
    }

    public static StatsCollector getInstance(Map<String, String> configs) {
        s_instance.init(configs);
        return s_instance;
    }

    public StatsCollector() {
        s_instance = this;
    }

    @Override
    public boolean start() {
        init(_configDao.getConfiguration());
        return true;
    }

    private void init(Map<String, String> configs) {
        _executor = Executors.newScheduledThreadPool(4, new NamedThreadFactory("StatsCollector"));

        hostStatsInterval = NumbersUtil.parseLong(configs.get("host.stats.interval"), 60000L);
        hostAndVmStatsInterval = NumbersUtil.parseLong(configs.get("vm.stats.interval"), 60000L);
        storageStatsInterval = NumbersUtil.parseLong(configs.get("storage.stats.interval"), 60000L);
        volumeStatsInterval = NumbersUtil.parseLong(configs.get("volume.stats.interval"), -1L);
        autoScaleStatsInterval = NumbersUtil.parseLong(configs.get("autoscale.stats.interval"), 60000L);
        vmDiskStatsInterval = NumbersUtil.parseInt(configs.get("vm.disk.stats.interval"), 0);

        /* URI to send statistics to. Currently only Graphite is supported */
        String externalStatsUri = configs.get("stats.output.uri");
        if (externalStatsUri != null && !externalStatsUri.equals("")) {
            try {
                URI uri = new URI(externalStatsUri);
                String scheme = uri.getScheme();

                try {
                    externalStatsType = ExternalStatsProtocol.valueOf(scheme.toUpperCase());
                } catch (IllegalArgumentException e) {
                    s_logger.info(scheme + " is not a valid protocol for external statistics. No statistics will be send.");
                }

                externalStatsHost = uri.getHost();
                externalStatsPort = uri.getPort();
                externalStatsPrefix = uri.getPath().substring(1);

                /* Append a dot (.) to the prefix if it is set */
                if (externalStatsPrefix != null && !externalStatsPrefix.equals("")) {
                    externalStatsPrefix += ".";
                } else {
                    externalStatsPrefix = "";
                }

                externalStatsEnabled = true;
            } catch (URISyntaxException e) {
                s_logger.debug("Failed to parse external statistics URI: " + e.getMessage());
            }
        }

        if (hostStatsInterval > 0) {
            _executor.scheduleWithFixedDelay(new HostCollector(), 15000L, hostStatsInterval, TimeUnit.MILLISECONDS);
        }

        if (hostAndVmStatsInterval > 0) {
            _executor.scheduleWithFixedDelay(new VmStatsCollector(), 15000L, hostAndVmStatsInterval, TimeUnit.MILLISECONDS);
        }

        if (storageStatsInterval > 0) {
            _executor.scheduleWithFixedDelay(new StorageCollector(), 15000L, storageStatsInterval, TimeUnit.MILLISECONDS);
        }

        if (autoScaleStatsInterval > 0) {
            _executor.scheduleWithFixedDelay(new AutoScaleMonitor(), 15000L, autoScaleStatsInterval, TimeUnit.MILLISECONDS);
        }

        if (vmDiskStatsInterval > 0) {
            if (vmDiskStatsInterval < 300)
                vmDiskStatsInterval = 300;
            _executor.scheduleAtFixedRate(new VmDiskStatsTask(), vmDiskStatsInterval, vmDiskStatsInterval, TimeUnit.SECONDS);
        }

        //Schedule disk stats update task
        _diskStatsUpdateExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("DiskStatsUpdater"));
        String aggregationRange = configs.get("usage.stats.job.aggregation.range");
        _usageAggregationRange = NumbersUtil.parseInt(aggregationRange, 1440);
        _usageTimeZone = configs.get("usage.aggregation.timezone");
        if (_usageTimeZone == null) {
            _usageTimeZone = "GMT";
        }
        TimeZone usageTimezone = TimeZone.getTimeZone(_usageTimeZone);
        Calendar cal = Calendar.getInstance(usageTimezone);
        cal.setTime(new Date());
        long endDate = 0;
        int HOURLY_TIME = 60;
        final int DAILY_TIME = 60 * 24;
        if (_usageAggregationRange == DAILY_TIME) {
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            cal.roll(Calendar.DAY_OF_YEAR, true);
            cal.add(Calendar.MILLISECOND, -1);
            endDate = cal.getTime().getTime();
            _dailyOrHourly = true;
        } else if (_usageAggregationRange == HOURLY_TIME) {
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            cal.roll(Calendar.HOUR_OF_DAY, true);
            cal.add(Calendar.MILLISECOND, -1);
            endDate = cal.getTime().getTime();
            _dailyOrHourly = true;
        } else {
            endDate = cal.getTime().getTime();
            _dailyOrHourly = false;
        }
        if (_usageAggregationRange < UsageUtils.USAGE_AGGREGATION_RANGE_MIN) {
            s_logger.warn("Usage stats job aggregation range is to small, using the minimum value of " + UsageUtils.USAGE_AGGREGATION_RANGE_MIN);
            _usageAggregationRange = UsageUtils.USAGE_AGGREGATION_RANGE_MIN;
        }
        _diskStatsUpdateExecutor.scheduleAtFixedRate(new VmDiskStatsUpdaterTask(), (endDate - System.currentTimeMillis()), (_usageAggregationRange * 60 * 1000),
                TimeUnit.MILLISECONDS);

    }

    class HostCollector extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            try {
                s_logger.debug("HostStatsCollector is running...");

                SearchCriteria<HostVO> sc = _hostDao.createSearchCriteria();
                sc.addAnd("status", SearchCriteria.Op.EQ, Status.Up.toString());
                sc.addAnd("resourceState", SearchCriteria.Op.NIN, ResourceState.Maintenance, ResourceState.PrepareForMaintenance, ResourceState.ErrorInMaintenance);
                sc.addAnd("type", SearchCriteria.Op.NEQ, Host.Type.Storage.toString());
                sc.addAnd("type", SearchCriteria.Op.NEQ, Host.Type.ConsoleProxy.toString());
                sc.addAnd("type", SearchCriteria.Op.NEQ, Host.Type.SecondaryStorage.toString());
                sc.addAnd("type", SearchCriteria.Op.NEQ, Host.Type.LocalSecondaryStorage.toString());
                sc.addAnd("type", SearchCriteria.Op.NEQ, Host.Type.TrafficMonitor.toString());
                sc.addAnd("type", SearchCriteria.Op.NEQ, Host.Type.SecondaryStorageVM.toString());
                sc.addAnd("type", SearchCriteria.Op.NEQ, Host.Type.ExternalFirewall.toString());
                sc.addAnd("type", SearchCriteria.Op.NEQ, Host.Type.ExternalLoadBalancer.toString());
                sc.addAnd("type", SearchCriteria.Op.NEQ, Host.Type.L2Networking.toString());
                sc.addAnd("type", SearchCriteria.Op.NEQ, Host.Type.BaremetalDhcp.toString());
                sc.addAnd("type", SearchCriteria.Op.NEQ, Host.Type.BaremetalPxe.toString());
                ConcurrentHashMap<Long, HostStats> hostStats = new ConcurrentHashMap<Long, HostStats>();
                List<HostVO> hosts = _hostDao.search(sc, null);
                for (HostVO host : hosts) {
                    HostStatsEntry stats = (HostStatsEntry)_resourceMgr.getHostStatistics(host.getId());
                    if (stats != null) {
                        hostStats.put(host.getId(), stats);
                    } else {
                        s_logger.warn("Received invalid host stats for host: " + host.getId());
                    }
                }
                _hostStats = hostStats;
                // Get a subset of hosts with GPU support from the list of "hosts"
                List<HostVO> gpuEnabledHosts = new ArrayList<HostVO>();
                if (hostIds != null) {
                    for (HostVO host : hosts) {
                        if (hostIds.contains(host.getId())) {
                            gpuEnabledHosts.add(host);
                        }
                    }
                } else {
                    // Check for all the hosts managed by CloudStack.
                    gpuEnabledHosts = hosts;
                }
                for (HostVO host : gpuEnabledHosts) {
                    HashMap<String, HashMap<String, VgpuTypesInfo>> groupDetails = _resourceMgr.getGPUStatistics(host);
                    if (groupDetails != null) {
                        _resourceMgr.updateGPUDetails(host.getId(), groupDetails);
                    }
                }
                hostIds = _hostGpuGroupsDao.listHostIds();
            } catch (Throwable t) {
                s_logger.error("Error trying to retrieve host stats", t);
            }
        }
    }

    class VmStatsCollector extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            try {
                s_logger.debug("VmStatsCollector is running...");

                SearchCriteria<HostVO> sc = _hostDao.createSearchCriteria();
                sc.addAnd("status", SearchCriteria.Op.EQ, Status.Up.toString());
                sc.addAnd("resourceState", SearchCriteria.Op.NIN, ResourceState.Maintenance, ResourceState.PrepareForMaintenance, ResourceState.ErrorInMaintenance);
                sc.addAnd("type", SearchCriteria.Op.NEQ, Host.Type.Storage.toString());
                sc.addAnd("type", SearchCriteria.Op.NEQ, Host.Type.ConsoleProxy.toString());
                sc.addAnd("type", SearchCriteria.Op.NEQ, Host.Type.SecondaryStorage.toString());
                sc.addAnd("type", SearchCriteria.Op.NEQ, Host.Type.LocalSecondaryStorage.toString());
                sc.addAnd("type", SearchCriteria.Op.NEQ, Host.Type.TrafficMonitor.toString());
                sc.addAnd("type", SearchCriteria.Op.NEQ, Host.Type.SecondaryStorageVM.toString());
                List<HostVO> hosts = _hostDao.search(sc, null);

                /* HashMap for metrics to be send to Graphite */
                HashMap metrics = new HashMap<String, Integer>();

                for (HostVO host : hosts) {
                    List<UserVmVO> vms = _userVmDao.listRunningByHostId(host.getId());
                    List<Long> vmIds = new ArrayList<Long>();

                    for (UserVmVO vm : vms) {
                        vmIds.add(vm.getId());
                    }

                    try {
                        HashMap<Long, VmStatsEntry> vmStatsById = _userVmMgr.getVirtualMachineStatistics(host.getId(), host.getName(), vmIds);

                        if (vmStatsById != null) {
                            VmStatsEntry statsInMemory = null;

                            Set<Long> vmIdSet = vmStatsById.keySet();
                            for (Long vmId : vmIdSet) {
                                VmStatsEntry statsForCurrentIteration = vmStatsById.get(vmId);
                                statsInMemory = (VmStatsEntry)_VmStats.get(vmId);

                                if (statsInMemory == null) {
                                    //no stats exist for this vm, directly persist
                                    _VmStats.put(vmId, statsForCurrentIteration);
                                } else {
                                    //update each field
                                    statsInMemory.setCPUUtilization(statsForCurrentIteration.getCPUUtilization());
                                    statsInMemory.setNumCPUs(statsForCurrentIteration.getNumCPUs());
                                    statsInMemory.setNetworkReadKBs(statsInMemory.getNetworkReadKBs() + statsForCurrentIteration.getNetworkReadKBs());
                                    statsInMemory.setNetworkWriteKBs(statsInMemory.getNetworkWriteKBs() + statsForCurrentIteration.getNetworkWriteKBs());
                                    statsInMemory.setDiskWriteKBs(statsInMemory.getDiskWriteKBs() + statsForCurrentIteration.getDiskWriteKBs());
                                    statsInMemory.setDiskReadIOs(statsInMemory.getDiskReadIOs() + statsForCurrentIteration.getDiskReadIOs());
                                    statsInMemory.setDiskWriteIOs(statsInMemory.getDiskWriteIOs() + statsForCurrentIteration.getDiskWriteIOs());
                                    statsInMemory.setDiskReadKBs(statsInMemory.getDiskReadKBs() + statsForCurrentIteration.getDiskReadKBs());

                                    _VmStats.put(vmId, statsInMemory);
                                }

                                /**
                                 * Add statistics to HashMap only when they should be send to a external stats collector
                                 * Performance wise it seems best to only append to the HashMap when needed
                                 */
                                if (externalStatsEnabled) {
                                    VMInstanceVO vmVO = _vmInstance.findById(vmId);
                                    String vmName = vmVO.getUuid();

                                    metrics.put(externalStatsPrefix + "cloudstack.stats.instances." + vmName + ".cpu.num", statsForCurrentIteration.getNumCPUs());
                                    metrics.put(externalStatsPrefix + "cloudstack.stats.instances." + vmName + ".cpu.utilization", statsForCurrentIteration.getCPUUtilization());
                                    metrics.put(externalStatsPrefix + "cloudstack.stats.instances." + vmName + ".network.read_kbs", statsForCurrentIteration.getNetworkReadKBs());
                                    metrics.put(externalStatsPrefix + "cloudstack.stats.instances." + vmName + ".network.write_kbs", statsForCurrentIteration.getNetworkWriteKBs());
                                    metrics.put(externalStatsPrefix + "cloudstack.stats.instances." + vmName + ".disk.write_kbs", statsForCurrentIteration.getDiskWriteKBs());
                                    metrics.put(externalStatsPrefix + "cloudstack.stats.instances." + vmName + ".disk.read_kbs", statsForCurrentIteration.getDiskReadKBs());
                                    metrics.put(externalStatsPrefix + "cloudstack.stats.instances." + vmName + ".disk.write_iops", statsForCurrentIteration.getDiskWriteIOs());
                                    metrics.put(externalStatsPrefix + "cloudstack.stats.instances." + vmName + ".disk.read_iops", statsForCurrentIteration.getDiskReadIOs());
                                }

                            }

                            /**
                             * Send the metrics to a external stats collector
                             * We send it on a per-host basis to prevent that we flood the host
                             * Currently only Graphite is supported
                             */
                            if (!metrics.isEmpty()) {
                                if (externalStatsType != null && externalStatsType == ExternalStatsProtocol.GRAPHITE) {

                                    if (externalStatsPort == -1) {
                                        externalStatsPort = 2003;
                                    }

                                    s_logger.debug("Sending VmStats of host " + host.getId() + " to Graphite host " + externalStatsHost + ":" + externalStatsPort);

                                    try {
                                        GraphiteClient g = new GraphiteClient(externalStatsHost, externalStatsPort);
                                        g.sendMetrics(metrics);
                                    } catch (GraphiteException e) {
                                        s_logger.debug("Failed sending VmStats to Graphite host " + externalStatsHost + ":" + externalStatsPort + ": " + e.getMessage());
                                    }

                                    metrics.clear();
                                }
                            }
                        }

                    } catch (Exception e) {
                        s_logger.debug("Failed to get VM stats for host with ID: " + host.getId());
                        continue;
                    }
                }

            } catch (Throwable t) {
                s_logger.error("Error trying to retrieve VM stats", t);
            }
        }
    }

    public VmStats getVmStats(long id) {
        return _VmStats.get(id);
    }

    class VmDiskStatsUpdaterTask extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            GlobalLock scanLock = GlobalLock.getInternLock("vm.disk.stats");
            try {
                if (scanLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION)) {
                    //Check for ownership
                    //msHost in UP state with min id should run the job
                    ManagementServerHostVO msHost = _msHostDao.findOneInUpState(new Filter(ManagementServerHostVO.class, "id", true, 0L, 1L));
                    if (msHost == null || (msHost.getMsid() != mgmtSrvrId)) {
                        s_logger.debug("Skipping aggregate disk stats update");
                        scanLock.unlock();
                        return;
                    }
                    try {
                        Transaction.execute(new TransactionCallbackNoReturn() {
                            @Override
                            public void doInTransactionWithoutResult(TransactionStatus status) {
                                //get all stats with delta > 0
                                List<VmDiskStatisticsVO> updatedVmNetStats = _vmDiskStatsDao.listUpdatedStats();
                                for (VmDiskStatisticsVO stat : updatedVmNetStats) {
                                    if (_dailyOrHourly) {
                                        //update agg bytes
                                        stat.setAggBytesRead(stat.getCurrentBytesRead() + stat.getNetBytesRead());
                                        stat.setAggBytesWrite(stat.getCurrentBytesWrite() + stat.getNetBytesWrite());
                                        stat.setAggIORead(stat.getCurrentIORead() + stat.getNetIORead());
                                        stat.setAggIOWrite(stat.getCurrentIOWrite() + stat.getNetIOWrite());
                                        _vmDiskStatsDao.update(stat.getId(), stat);
                                    }
                                }
                                s_logger.debug("Successfully updated aggregate vm disk stats");
                            }
                        });
                    } catch (Exception e) {
                        s_logger.debug("Failed to update aggregate disk stats", e);
                    } finally {
                        scanLock.unlock();
                    }
                }
            } catch (Exception e) {
                s_logger.debug("Exception while trying to acquire disk stats lock", e);
            } finally {
                scanLock.releaseRef();
            }
        }
    }

    class VmDiskStatsTask extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            // collect the vm disk statistics(total) from hypervisor. added by weizhou, 2013.03.
            try {
                Transaction.execute(new TransactionCallbackNoReturn() {
                    @Override
                    public void doInTransactionWithoutResult(TransactionStatus status) {
                        SearchCriteria<HostVO> sc = _hostDao.createSearchCriteria();
                        sc.addAnd("status", SearchCriteria.Op.EQ, Status.Up.toString());
                        sc.addAnd("resourceState", SearchCriteria.Op.NIN, ResourceState.Maintenance, ResourceState.PrepareForMaintenance,
                                ResourceState.ErrorInMaintenance);
                        sc.addAnd("type", SearchCriteria.Op.EQ, Host.Type.Routing.toString());
                        sc.addAnd("hypervisorType", SearchCriteria.Op.EQ, HypervisorType.KVM); // support KVM only util 2013.06.25
                        List<HostVO> hosts = _hostDao.search(sc, null);

                        for (HostVO host : hosts) {
                            List<UserVmVO> vms = _userVmDao.listRunningByHostId(host.getId());
                            List<Long> vmIds = new ArrayList<Long>();

                            for (UserVmVO vm : vms) {
                                if (vm.getType() == VirtualMachine.Type.User) // user vm
                                    vmIds.add(vm.getId());
                            }

                            HashMap<Long, List<VmDiskStatsEntry>> vmDiskStatsById = _userVmMgr.getVmDiskStatistics(host.getId(), host.getName(), vmIds);
                            if (vmDiskStatsById == null)
                                continue;

                            Set<Long> vmIdSet = vmDiskStatsById.keySet();
                            for (Long vmId : vmIdSet) {
                                List<VmDiskStatsEntry> vmDiskStats = vmDiskStatsById.get(vmId);
                                if (vmDiskStats == null)
                                    continue;
                                UserVmVO userVm = _userVmDao.findById(vmId);
                                for (VmDiskStatsEntry vmDiskStat : vmDiskStats) {
                                    SearchCriteria<VolumeVO> sc_volume = _volsDao.createSearchCriteria();
                                    sc_volume.addAnd("path", SearchCriteria.Op.EQ, vmDiskStat.getPath());
                                    List<VolumeVO> volumes = _volsDao.search(sc_volume, null);
                                    if ((volumes == null) || (volumes.size() == 0))
                                        break;
                                    VolumeVO volume = volumes.get(0);
                                    VmDiskStatisticsVO previousVmDiskStats =
                                            _vmDiskStatsDao.findBy(userVm.getAccountId(), userVm.getDataCenterId(), vmId, volume.getId());
                                    VmDiskStatisticsVO vmDiskStat_lock = _vmDiskStatsDao.lock(userVm.getAccountId(), userVm.getDataCenterId(), vmId, volume.getId());

                                    if ((vmDiskStat.getBytesRead() == 0) && (vmDiskStat.getBytesWrite() == 0) && (vmDiskStat.getIORead() == 0) &&
                                            (vmDiskStat.getIOWrite() == 0)) {
                                        s_logger.debug("IO/bytes read and write are all 0. Not updating vm_disk_statistics");
                                        continue;
                                    }

                                    if (vmDiskStat_lock == null) {
                                        s_logger.warn("unable to find vm disk stats from host for account: " + userVm.getAccountId() + " with vmId: " + userVm.getId() +
                                                " and volumeId:" + volume.getId());
                                        continue;
                                    }

                                    if (previousVmDiskStats != null &&
                                            ((previousVmDiskStats.getCurrentBytesRead() != vmDiskStat_lock.getCurrentBytesRead()) ||
                                                    (previousVmDiskStats.getCurrentBytesWrite() != vmDiskStat_lock.getCurrentBytesWrite()) ||
                                                    (previousVmDiskStats.getCurrentIORead() != vmDiskStat_lock.getCurrentIORead()) || (previousVmDiskStats.getCurrentIOWrite() != vmDiskStat_lock.getCurrentIOWrite()))) {
                                        s_logger.debug("vm disk stats changed from the time GetVmDiskStatsCommand was sent. " + "Ignoring current answer. Host: " +
                                                host.getName() + " . VM: " + vmDiskStat.getVmName() + " Read(Bytes): " + vmDiskStat.getBytesRead() + " write(Bytes): " +
                                                vmDiskStat.getBytesWrite() + " Read(IO): " + vmDiskStat.getIORead() + " write(IO): " + vmDiskStat.getIOWrite());
                                        continue;
                                    }

                                    if (vmDiskStat_lock.getCurrentBytesRead() > vmDiskStat.getBytesRead()) {
                                        if (s_logger.isDebugEnabled()) {
                                            s_logger.debug("Read # of bytes that's less than the last one.  " +
                                                    "Assuming something went wrong and persisting it. Host: " + host.getName() + " . VM: " + vmDiskStat.getVmName() +
                                                    " Reported: " + vmDiskStat.getBytesRead() + " Stored: " + vmDiskStat_lock.getCurrentBytesRead());
                                        }
                                        vmDiskStat_lock.setNetBytesRead(vmDiskStat_lock.getNetBytesRead() + vmDiskStat_lock.getCurrentBytesRead());
                                    }
                                    vmDiskStat_lock.setCurrentBytesRead(vmDiskStat.getBytesRead());
                                    if (vmDiskStat_lock.getCurrentBytesWrite() > vmDiskStat.getBytesWrite()) {
                                        if (s_logger.isDebugEnabled()) {
                                            s_logger.debug("Write # of bytes that's less than the last one.  " +
                                                    "Assuming something went wrong and persisting it. Host: " + host.getName() + " . VM: " + vmDiskStat.getVmName() +
                                                    " Reported: " + vmDiskStat.getBytesWrite() + " Stored: " + vmDiskStat_lock.getCurrentBytesWrite());
                                        }
                                        vmDiskStat_lock.setNetBytesWrite(vmDiskStat_lock.getNetBytesWrite() + vmDiskStat_lock.getCurrentBytesWrite());
                                    }
                                    vmDiskStat_lock.setCurrentBytesWrite(vmDiskStat.getBytesWrite());
                                    if (vmDiskStat_lock.getCurrentIORead() > vmDiskStat.getIORead()) {
                                        if (s_logger.isDebugEnabled()) {
                                            s_logger.debug("Read # of IO that's less than the last one.  " + "Assuming something went wrong and persisting it. Host: " +
                                                    host.getName() + " . VM: " + vmDiskStat.getVmName() + " Reported: " + vmDiskStat.getIORead() + " Stored: " +
                                                    vmDiskStat_lock.getCurrentIORead());
                                        }
                                        vmDiskStat_lock.setNetIORead(vmDiskStat_lock.getNetIORead() + vmDiskStat_lock.getCurrentIORead());
                                    }
                                    vmDiskStat_lock.setCurrentIORead(vmDiskStat.getIORead());
                                    if (vmDiskStat_lock.getCurrentIOWrite() > vmDiskStat.getIOWrite()) {
                                        if (s_logger.isDebugEnabled()) {
                                            s_logger.debug("Write # of IO that's less than the last one.  " + "Assuming something went wrong and persisting it. Host: " +
                                                    host.getName() + " . VM: " + vmDiskStat.getVmName() + " Reported: " + vmDiskStat.getIOWrite() + " Stored: " +
                                                    vmDiskStat_lock.getCurrentIOWrite());
                                        }
                                        vmDiskStat_lock.setNetIOWrite(vmDiskStat_lock.getNetIOWrite() + vmDiskStat_lock.getCurrentIOWrite());
                                    }
                                    vmDiskStat_lock.setCurrentIOWrite(vmDiskStat.getIOWrite());

                                    if (!_dailyOrHourly) {
                                        //update agg bytes
                                        vmDiskStat_lock.setAggBytesWrite(vmDiskStat_lock.getNetBytesWrite() + vmDiskStat_lock.getCurrentBytesWrite());
                                        vmDiskStat_lock.setAggBytesRead(vmDiskStat_lock.getNetBytesRead() + vmDiskStat_lock.getCurrentBytesRead());
                                        vmDiskStat_lock.setAggIOWrite(vmDiskStat_lock.getNetIOWrite() + vmDiskStat_lock.getCurrentIOWrite());
                                        vmDiskStat_lock.setAggIORead(vmDiskStat_lock.getNetIORead() + vmDiskStat_lock.getCurrentIORead());
                                    }

                                    _vmDiskStatsDao.update(vmDiskStat_lock.getId(), vmDiskStat_lock);
                                }
                            }
                        }
                    }
                });
            } catch (Exception e) {
                s_logger.warn("Error while collecting vm disk stats from hosts", e);
            }
        }
    }

    class StorageCollector extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            try {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("StorageCollector is running...");
                }

                List<DataStore> stores = _dataStoreMgr.listImageStores();
                ConcurrentHashMap<Long, StorageStats> storageStats = new ConcurrentHashMap<Long, StorageStats>();
                for (DataStore store : stores) {
                    if (store.getUri() == null) {
                        continue;
                    }

                    GetStorageStatsCommand command = new GetStorageStatsCommand(store.getTO());
                    EndPoint ssAhost = _epSelector.select(store);
                    if (ssAhost == null) {
                        s_logger.debug("There is no secondary storage VM for secondary storage host " + store.getName());
                        continue;
                    }
                    long storeId = store.getId();
                    Answer answer = ssAhost.sendMessage(command);
                    if (answer != null && answer.getResult()) {
                        storageStats.put(storeId, (StorageStats)answer);
                        s_logger.trace("HostId: " + storeId + " Used: " + ((StorageStats)answer).getByteUsed() + " Total Available: " +
                                ((StorageStats)answer).getCapacityBytes());
                    }
                }
                _storageStats = storageStats;
                ConcurrentHashMap<Long, StorageStats> storagePoolStats = new ConcurrentHashMap<Long, StorageStats>();

                List<StoragePoolVO> storagePools = _storagePoolDao.listAll();
                for (StoragePoolVO pool : storagePools) {
                    // check if the pool has enabled hosts
                    List<Long> hostIds = _storageManager.getUpHostsInPool(pool.getId());
                    if (hostIds == null || hostIds.isEmpty())
                        continue;
                    GetStorageStatsCommand command = new GetStorageStatsCommand(pool.getUuid(), pool.getPoolType(), pool.getPath());
                    long poolId = pool.getId();
                    try {
                        Answer answer = _storageManager.sendToPool(pool, command);
                        if (answer != null && answer.getResult()) {
                            storagePoolStats.put(pool.getId(), (StorageStats)answer);

                            // Seems like we have dynamically updated the pool size since the prev. size and the current do not match
                            if (_storagePoolStats.get(poolId) != null && _storagePoolStats.get(poolId).getCapacityBytes() != ((StorageStats)answer).getCapacityBytes()) {
                                pool.setCapacityBytes(((StorageStats)answer).getCapacityBytes());
                                _storagePoolDao.update(pool.getId(), pool);
                            }
                        }
                    } catch (StorageUnavailableException e) {
                        s_logger.info("Unable to reach " + pool, e);
                    } catch (Exception e) {
                        s_logger.warn("Unable to get stats for " + pool, e);
                    }
                }
                _storagePoolStats = storagePoolStats;
            } catch (Throwable t) {
                s_logger.error("Error trying to retrieve storage stats", t);
            }
        }
    }

    class AutoScaleMonitor extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            try {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("AutoScaling Monitor is running...");
                }
                // list all AS VMGroups
                List<AutoScaleVmGroupVO> asGroups = _asGroupDao.listAll();
                for (AutoScaleVmGroupVO asGroup : asGroups) {
                    // check group state
                    if ((asGroup.getState().equals("enabled")) && (is_native(asGroup.getId()))) {
                        // check minimum vm of group
                        Integer currentVM = _asGroupVmDao.countByGroup(asGroup.getId());
                        if (currentVM < asGroup.getMinMembers()) {
                            _asManager.doScaleUp(asGroup.getId(), asGroup.getMinMembers() - currentVM);
                            continue;
                        }

                        //check interval
                        long now = (new Date()).getTime();
                        if (asGroup.getLastInterval() != null)
                            if ((now - asGroup.getLastInterval().getTime()) < asGroup
                                    .getInterval()) {
                                continue;
                            }

                        // update last_interval
                        asGroup.setLastInterval(new Date());
                        _asGroupDao.persist(asGroup);

                        // collect RRDs data for this group
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("[AutoScale] Collecting RRDs data...");
                        }
                        Map<String, String> params = new HashMap<String, String>();
                        List<AutoScaleVmGroupVmMapVO> asGroupVmVOs = _asGroupVmDao.listByGroup(asGroup.getId());
                        params.put("total_vm", String.valueOf(asGroupVmVOs.size()));
                        for (int i = 0; i < asGroupVmVOs.size(); i++) {
                            long vmId = asGroupVmVOs.get(i).getInstanceId();
                            VMInstanceVO vmVO = _vmInstance.findById(vmId);
                            //xe vm-list | grep vmname -B 1 | head -n 1 | awk -F':' '{print $2}'
                            params.put("vmname" + String.valueOf(i + 1), vmVO.getInstanceName());
                            params.put("vmid" + String.valueOf(i + 1), String.valueOf(vmVO.getId()));

                        }
                        // get random hostid because all vms are in a cluster
                        long vmId = asGroupVmVOs.get(0).getInstanceId();
                        VMInstanceVO vmVO = _vmInstance.findById(vmId);
                        Long receiveHost = vmVO.getHostId();

                        // setup parameters phase: duration and counter
                        // list pair [counter, duration]
                        List<Pair<String, Integer>> lstPair = getPairofCounternameAndDuration(asGroup.getId());
                        int total_counter = 0;
                        String[] lstCounter = new String[lstPair.size()];
                        for (int i = 0; i < lstPair.size(); i++) {
                            Pair<String, Integer> pair = lstPair.get(i);
                            String strCounterNames = pair.first();
                            Integer duration = pair.second();

                            lstCounter[i] = strCounterNames.split(",")[0];
                            total_counter++;
                            params.put("duration" + String.valueOf(total_counter), duration.toString());
                            params.put("counter" + String.valueOf(total_counter), lstCounter[i]);
                            params.put("con" + String.valueOf(total_counter), strCounterNames.split(",")[1]);
                        }
                        params.put("total_counter", String.valueOf(total_counter));

                        PerformanceMonitorCommand perfMon = new PerformanceMonitorCommand(params, 20);

                        try {
                            Answer answer = _agentMgr.send(receiveHost, perfMon);
                            if (answer == null || !answer.getResult()) {
                                s_logger.debug("Failed to send data to node !");
                            } else {
                                String result = answer.getDetails();
                                s_logger.debug("[AutoScale] RRDs collection answer: " + result);
                                HashMap<Long, Double> avgCounter = new HashMap<Long, Double>();

                                // extract data
                                String[] counterElements = result.split(",");
                                if ((counterElements != null) && (counterElements.length > 0)) {
                                    for (String string : counterElements) {
                                        try {
                                            String[] counterVals = string.split(":");
                                            String[] counter_vm = counterVals[0].split("\\.");

                                            Long counterId = Long.parseLong(counter_vm[1]);
                                            Long conditionId = Long.parseLong(params.get("con" + counter_vm[1]));
                                            Double coVal = Double.parseDouble(counterVals[1]);

                                            // Summary of all counter by counterId key
                                            if (avgCounter.get(counterId) == null) {
                                                /* initialize if data is not set */
                                                avgCounter.put(counterId, new Double(0));
                                            }

                                            String counterName = getCounternamebyCondition(conditionId.longValue());
                                            if (Counter.Source.memory.toString().equals(counterName)) {
                                                // calculate memory in percent
                                                Long profileId = asGroup.getProfileId();
                                                AutoScaleVmProfileVO profileVo = _asProfileDao.findById(profileId);
                                                ServiceOfferingVO serviceOff = _serviceOfferingDao.findById(profileVo.getServiceOfferingId());
                                                int maxRAM = serviceOff.getRamSize();

                                                // get current RAM percent
                                                coVal = coVal / maxRAM;
                                            } else {
                                                // cpu
                                                coVal = coVal * 100;
                                            }

                                            // update data entry
                                            avgCounter.put(counterId, avgCounter.get(counterId) + coVal);

                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }

                                    String scaleAction = getAutoscaleAction(avgCounter, asGroup.getId(), currentVM, params);
                                    if (scaleAction != null) {
                                        s_logger.debug("[AutoScale] Doing scale action: " + scaleAction + " for group " + asGroup.getId());
                                        if (scaleAction.equals("scaleup")) {
                                            _asManager.doScaleUp(asGroup.getId(), 1);
                                        } else {
                                            _asManager.doScaleDown(asGroup.getId());
                                        }
                                    }
                                }
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                }

            } catch (Throwable t) {
                s_logger.error("Error trying to monitor autoscaling", t);
            }

        }

        private boolean is_native(long groupId) {
            List<AutoScaleVmGroupPolicyMapVO> vos = _asGroupPolicyDao.listByVmGroupId(groupId);
            for (AutoScaleVmGroupPolicyMapVO vo : vos) {
                List<AutoScalePolicyConditionMapVO> ConditionPolicies = _asConditionMapDao.findByPolicyId(vo.getPolicyId());
                for (AutoScalePolicyConditionMapVO ConditionPolicy : ConditionPolicies) {
                    ConditionVO condition = _asConditionDao.findById(ConditionPolicy.getConditionId());
                    CounterVO counter = _asCounterDao.findById(condition.getCounterid());
                    if (counter.getSource() == Counter.Source.cpu || counter.getSource() == Counter.Source.memory)
                        return true;
                }
            }
            return false;
        }

        private String getAutoscaleAction(HashMap<Long, Double> avgCounter, long groupId, long currentVM, Map<String, String> params) {

            List<AutoScaleVmGroupPolicyMapVO> listMap = _asGroupPolicyDao.listByVmGroupId(groupId);
            if ((listMap == null) || (listMap.size() == 0))
                return null;
            for (AutoScaleVmGroupPolicyMapVO asVmgPmap : listMap) {
                AutoScalePolicyVO policyVO = _asPolicyDao.findById(asVmgPmap.getPolicyId());
                if (policyVO != null) {
                    Integer quitetime = policyVO.getQuietTime();
                    Date quitetimeDate = policyVO.getLastQuiteTime();
                    long last_quitetime = 0L;
                    if (quitetimeDate != null) {
                        last_quitetime = policyVO.getLastQuiteTime().getTime();
                    }
                    long current_time = (new Date()).getTime();

                    // check quite time for this policy
                    if ((current_time - last_quitetime) >= (long)quitetime) {

                        // list all condition of this policy
                        boolean bValid = true;
                        List<ConditionVO> lstConditions = getConditionsbyPolicyId(policyVO.getId());
                        if ((lstConditions != null) && (lstConditions.size() > 0)) {
                            // check whole conditions of this policy
                            for (ConditionVO conditionVO : lstConditions) {
                                long thresholdValue = conditionVO.getThreshold();
                                Double thresholdPercent = (double)thresholdValue / 100;
                                CounterVO counterVO = _asCounterDao.findById(conditionVO.getCounterid());
//Double sum = avgCounter.get(conditionVO.getCounterid());
                                long counter_count = 1;
                                do {
                                    String counter_param = params.get("counter" + String.valueOf(counter_count));
                                    Counter.Source counter_source = counterVO.getSource();
                                    if (counter_param.equals(counter_source.toString()))
                                        break;
                                    counter_count++;
                                } while (1 == 1);

                                Double sum = avgCounter.get(counter_count);
                                Double avg = sum / currentVM;
                                Operator op = conditionVO.getRelationalOperator();
                                boolean bConditionCheck = ((op == com.cloud.network.as.Condition.Operator.EQ) && (thresholdPercent.equals(avg)))
                                        || ((op == com.cloud.network.as.Condition.Operator.GE) && (avg.doubleValue() >= thresholdPercent.doubleValue()))
                                        || ((op == com.cloud.network.as.Condition.Operator.GT) && (avg.doubleValue() > thresholdPercent.doubleValue()))
                                        || ((op == com.cloud.network.as.Condition.Operator.LE) && (avg.doubleValue() <= thresholdPercent.doubleValue()))
                                        || ((op == com.cloud.network.as.Condition.Operator.LT) && (avg.doubleValue() < thresholdPercent.doubleValue()));

                                if (!bConditionCheck) {
                                    bValid = false;
                                    break;
                                }
                            }
                            if (bValid) {
                                return policyVO.getAction();
                            }
                        }
                    }
                }
            }
            return null;
        }

        private List<ConditionVO> getConditionsbyPolicyId(long policyId) {
            List<AutoScalePolicyConditionMapVO> conditionMap = _asConditionMapDao.findByPolicyId(policyId);
            if ((conditionMap == null) || (conditionMap.size() == 0))
                return null;

            List<ConditionVO> lstResult = new ArrayList<ConditionVO>();
            for (AutoScalePolicyConditionMapVO asPCmap : conditionMap) {
                lstResult.add(_asConditionDao.findById(asPCmap.getConditionId()));
            }

            return lstResult;
        }

        public List<Pair<String, Integer>> getPairofCounternameAndDuration(
                long groupId) {
            AutoScaleVmGroupVO groupVo = _asGroupDao.findById(groupId);
            if (groupVo == null)
                return null;
            List<Pair<String, Integer>> result = new ArrayList<Pair<String, Integer>>();
            //list policy map
            List<AutoScaleVmGroupPolicyMapVO> groupPolicymap = _asGroupPolicyDao.listByVmGroupId(groupVo.getId());
            if (groupPolicymap == null)
                return null;
            for (AutoScaleVmGroupPolicyMapVO gpMap : groupPolicymap) {
                //get duration
                AutoScalePolicyVO policyVo = _asPolicyDao.findById(gpMap.getPolicyId());
                Integer duration = policyVo.getDuration();
                //get collection of counter name

                StringBuffer buff = new StringBuffer();
                List<AutoScalePolicyConditionMapVO> lstPCmap = _asConditionMapDao.findByPolicyId(policyVo.getId());
                for (AutoScalePolicyConditionMapVO pcMap : lstPCmap) {
                    String counterName = getCounternamebyCondition(pcMap.getConditionId());
                    buff.append(counterName);
                    buff.append(",");
                    buff.append(pcMap.getConditionId());
                }
                // add to result
                Pair<String, Integer> pair = new Pair<String, Integer>(buff.toString(), duration);
                result.add(pair);
            }

            return result;
        }

        public String getCounternamebyCondition(long conditionId) {

            ConditionVO condition = _asConditionDao.findById(conditionId);
            if (condition == null)
                return "";

            long counterId = condition.getCounterid();
            CounterVO counter = _asCounterDao.findById(counterId);
            if (counter == null)
                return "";

            return counter.getSource().toString();
        }
    }

    public boolean imageStoreHasEnoughCapacity(DataStore imageStore) {
        StorageStats imageStoreStats = _storageStats.get(imageStore.getId());
        if (imageStoreStats != null && (imageStoreStats.getByteUsed()/(imageStoreStats.getCapacityBytes()*1.0)) <= _imageStoreCapacityThreshold) {
            return true;
        }
        return false;
    }

    public StorageStats getStorageStats(long id) {
        return _storageStats.get(id);
    }

    public HostStats getHostStats(long hostId) {
        return _hostStats.get(hostId);
    }

    public StorageStats getStoragePoolStats(long id) {
        return _storagePoolStats.get(id);
    }
}
