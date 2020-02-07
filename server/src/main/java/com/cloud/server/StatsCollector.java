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
import java.util.Collection;
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

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.utils.graphite.GraphiteClient;
import org.apache.cloudstack.utils.graphite.GraphiteException;
import org.apache.cloudstack.utils.usage.UsageUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.influxdb.BatchOptions;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.Pong;
import org.springframework.stereotype.Component;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.GetStorageStatsCommand;
import com.cloud.agent.api.HostStatsEntry;
import com.cloud.agent.api.PerformanceMonitorCommand;
import com.cloud.agent.api.VgpuTypesInfo;
import com.cloud.agent.api.VmDiskStatsEntry;
import com.cloud.agent.api.VmNetworkStatsEntry;
import com.cloud.agent.api.VmStatsEntry;
import com.cloud.agent.api.VolumeStatsEntry;
import com.cloud.cluster.ManagementServerHostVO;
import com.cloud.cluster.dao.ManagementServerHostDao;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.VlanDao;
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
import com.cloud.org.Cluster;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceState;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.ImageStoreDetailsUtil;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StorageStats;
import com.cloud.storage.VolumeStats;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.UserStatisticsVO;
import com.cloud.user.VmDiskStatisticsVO;
import com.cloud.user.dao.UserStatisticsDao;
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
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.MacAddress;
import com.cloud.vm.NicVO;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VmStats;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

/**
 * Provides real time stats for various agent resources up to x seconds
 *
 */
@Component
public class StatsCollector extends ManagerBase implements ComponentMethodInterceptable, Configurable {

    public static enum ExternalStatsProtocol {
        NONE("none"), GRAPHITE("graphite"), INFLUXDB("influxdb");
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

    private static final int UNDEFINED_PORT_VALUE = -1;

    /**
     * Default value for the Graphite connection port: {@value}
     */
    private static final int GRAPHITE_DEFAULT_PORT = 2003;

    /**
     * Default value for the InfluxDB connection port: {@value}
     */
    private static final int INFLUXDB_DEFAULT_PORT = 8086;

    private static final String UUID_TAG = "uuid";

    private static final String TOTAL_MEMORY_KBS_FIELD = "total_memory_kb";
    private static final String FREE_MEMORY_KBS_FIELD = "free_memory_kb";
    private static final String CPU_UTILIZATION_FIELD = "cpu_utilization";
    private static final String CPUS_FIELD = "cpus";
    private static final String CPU_SOCKETS_FIELD = "cpu_sockets";
    private static final String NETWORK_READ_KBS_FIELD = "network_read_kbs";
    private static final String NETWORK_WRITE_KBS_FIELD = "network_write_kbs";
    private static final String MEMORY_TARGET_KBS_FIELD = "memory_target_kbs";
    private static final String DISK_READ_IOPS_FIELD = "disk_read_iops";
    private static final String DISK_READ_KBS_FIELD = "disk_read_kbs";
    private static final String DISK_WRITE_IOPS_FIELD = "disk_write_iops";
    private static final String DISK_WRITE_KBS_FIELD = "disk_write_kbs";

    private static final String DEFAULT_DATABASE_NAME = "cloudstack";
    private static final String INFLUXDB_HOST_MEASUREMENT = "host_stats";
    private static final String INFLUXDB_VM_MEASUREMENT = "vm_stats";

    private static final ConfigKey<Integer> vmDiskStatsInterval = new ConfigKey<Integer>("Advanced", Integer.class, "vm.disk.stats.interval", "0",
            "Interval (in seconds) to report vm disk statistics. Vm disk statistics will be disabled if this is set to 0 or less than 0.", false);
    private static final ConfigKey<Integer> vmDiskStatsIntervalMin = new ConfigKey<Integer>("Advanced", Integer.class, "vm.disk.stats.interval.min", "300",
            "Minimal interval (in seconds) to report vm disk statistics. If vm.disk.stats.interval is smaller than this, use this to report vm disk statistics.", false);
    private static final ConfigKey<Integer> vmNetworkStatsInterval = new ConfigKey<Integer>("Advanced", Integer.class, "vm.network.stats.interval", "0",
            "Interval (in seconds) to report vm network statistics (for Shared networks). Vm network statistics will be disabled if this is set to 0 or less than 0.", false);
    private static final ConfigKey<Integer> vmNetworkStatsIntervalMin = new ConfigKey<Integer>("Advanced", Integer.class, "vm.network.stats.interval.min", "300",
            "Minimal Interval (in seconds) to report vm network statistics (for Shared networks). If vm.network.stats.interval is smaller than this, use this to report vm network statistics.",
            false);
    private static final ConfigKey<Integer> StatsTimeout = new ConfigKey<Integer>("Advanced", Integer.class, "stats.timeout", "60000",
            "The timeout for stats call in milli seconds.", true,
            ConfigKey.Scope.Cluster);
    private static final ConfigKey<String> statsOutputUri = new ConfigKey<String>("Advanced", String.class, "stats.output.uri", "",
            "URI to send StatsCollector statistics to. The collector is defined on the URI scheme. Example: graphite://graphite-hostaddress:port or influxdb://influxdb-hostaddress/dbname. Note that the port is optional, if not added the default port for the respective collector (graphite or influxdb) will be used. Additionally, the database name '/dbname' is  also optional; default db name is 'cloudstack'. You must create and configure the database if using influxdb.",
            true);

    private static StatsCollector s_instance = null;

    private ScheduledExecutorService _executor = null;
    @Inject
    private AgentManager _agentMgr;
    @Inject
    private UserVmManager _userVmMgr;
    @Inject
    private HostDao _hostDao;
    @Inject
    private ClusterDao _clusterDao;
    @Inject
    private UserVmDao _userVmDao;
    @Inject
    private VolumeDao _volsDao;
    @Inject
    private PrimaryDataStoreDao _storagePoolDao;
    @Inject
    private StorageManager _storageManager;
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
    private UserStatisticsDao _userStatsDao;
    @Inject
    private NicDao _nicDao;
    @Inject
    private VlanDao _vlanDao;
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
    @Inject
    private ImageStoreDetailsUtil imageStoreDetailsUtil;

    private ConcurrentHashMap<Long, HostStats> _hostStats = new ConcurrentHashMap<Long, HostStats>();
    private final ConcurrentHashMap<Long, VmStats> _VmStats = new ConcurrentHashMap<Long, VmStats>();
    private final Map<String, VolumeStats> _volumeStats = new ConcurrentHashMap<String, VolumeStats>();
    private ConcurrentHashMap<Long, StorageStats> _storageStats = new ConcurrentHashMap<Long, StorageStats>();
    private ConcurrentHashMap<Long, StorageStats> _storagePoolStats = new ConcurrentHashMap<Long, StorageStats>();

    private long hostStatsInterval = -1L;
    private long hostAndVmStatsInterval = -1L;
    private long storageStatsInterval = -1L;
    private long volumeStatsInterval = -1L;
    private long autoScaleStatsInterval = -1L;

    private double _imageStoreCapacityThreshold = 0.90;

    private String externalStatsPrefix = "";
    String externalStatsHost = null;
    int externalStatsPort = -1;
    private String externalStatsScheme;
    ExternalStatsProtocol externalStatsType = ExternalStatsProtocol.NONE;
    private String databaseName = DEFAULT_DATABASE_NAME;

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

    protected void init(Map<String, String> configs) {
        _executor = Executors.newScheduledThreadPool(6, new NamedThreadFactory("StatsCollector"));

        hostStatsInterval = NumbersUtil.parseLong(configs.get("host.stats.interval"), 60000L);
        hostAndVmStatsInterval = NumbersUtil.parseLong(configs.get("vm.stats.interval"), 60000L);
        storageStatsInterval = NumbersUtil.parseLong(configs.get("storage.stats.interval"), 60000L);
        volumeStatsInterval = NumbersUtil.parseLong(configs.get("volume.stats.interval"), 600000L);
        autoScaleStatsInterval = NumbersUtil.parseLong(configs.get("autoscale.stats.interval"), 60000L);

        String statsUri = statsOutputUri.value();
        if (StringUtils.isNotBlank(statsUri)) {
            try {
                URI uri = new URI(statsUri);
                externalStatsScheme = uri.getScheme();

                try {
                    externalStatsType = ExternalStatsProtocol.valueOf(externalStatsScheme.toUpperCase());
                } catch (IllegalArgumentException e) {
                    s_logger.error(externalStatsScheme + " is not a valid protocol for external statistics. No statistics will be send.");
                }

                if (StringUtils.isNotEmpty(uri.getHost())) {
                    externalStatsHost = uri.getHost();
                }

                externalStatsPort = retrieveExternalStatsPortFromUri(uri);

                databaseName = configureDatabaseName(uri);

                if (!StringUtils.isEmpty(uri.getPath())) {
                    externalStatsPrefix = uri.getPath().substring(1);
                }

                /* Append a dot (.) to the prefix if it is set */
                if (!StringUtils.isEmpty(externalStatsPrefix)) {
                    externalStatsPrefix += ".";
                } else {
                    externalStatsPrefix = "";
                }

            } catch (URISyntaxException e) {
                s_logger.error("Failed to parse external statistics URI: ", e);
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

        if (vmDiskStatsInterval.value() > 0) {
            if (vmDiskStatsInterval.value() < vmDiskStatsIntervalMin.value()) {
                s_logger.debug("vm.disk.stats.interval - " + vmDiskStatsInterval.value() + " is smaller than vm.disk.stats.interval.min - " + vmDiskStatsIntervalMin.value()
                        + ", so use vm.disk.stats.interval.min");
                _executor.scheduleAtFixedRate(new VmDiskStatsTask(), vmDiskStatsIntervalMin.value(), vmDiskStatsIntervalMin.value(), TimeUnit.SECONDS);
            } else {
                _executor.scheduleAtFixedRate(new VmDiskStatsTask(), vmDiskStatsInterval.value(), vmDiskStatsInterval.value(), TimeUnit.SECONDS);
            }
        } else {
            s_logger.debug("vm.disk.stats.interval - " + vmDiskStatsInterval.value() + " is 0 or less than 0, so not scheduling the vm disk stats thread");
        }

        if (vmNetworkStatsInterval.value() > 0) {
            if (vmNetworkStatsInterval.value() < vmNetworkStatsIntervalMin.value()) {
                s_logger.debug("vm.network.stats.interval - " + vmNetworkStatsInterval.value() + " is smaller than vm.network.stats.interval.min - "
                        + vmNetworkStatsIntervalMin.value() + ", so use vm.network.stats.interval.min");
                _executor.scheduleAtFixedRate(new VmNetworkStatsTask(), vmNetworkStatsIntervalMin.value(), vmNetworkStatsIntervalMin.value(), TimeUnit.SECONDS);
            } else {
                _executor.scheduleAtFixedRate(new VmNetworkStatsTask(), vmNetworkStatsInterval.value(), vmNetworkStatsInterval.value(), TimeUnit.SECONDS);
            }
        } else {
            s_logger.debug("vm.network.stats.interval - " + vmNetworkStatsInterval.value() + " is 0 or less than 0, so not scheduling the vm network stats thread");
        }

        if (volumeStatsInterval > 0) {
            _executor.scheduleAtFixedRate(new VolumeStatsTask(), 15000L, volumeStatsInterval, TimeUnit.MILLISECONDS);
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

    /**
     * Configures the database name according to the URI path. For instance, if the URI is as influxdb://address:port/dbname, the database name will be 'dbname'.
     */
    protected String configureDatabaseName(URI uri) {
        String dbname = StringUtils.removeStart(uri.getPath(), "/");
        if (StringUtils.isBlank(dbname)) {
            return DEFAULT_DATABASE_NAME;
        } else {
            return dbname;
        }
    }

    /**
     * Configures the port to be used when connecting with the stats collector service.
     * Default values are 8086 for influx DB and 2003 for GraphiteDB.
     * Throws URISyntaxException in case of non configured port and external StatsType
     */
    protected int retrieveExternalStatsPortFromUri(URI uri) throws URISyntaxException {
        int port = uri.getPort();
        if (externalStatsType != ExternalStatsProtocol.NONE) {
            if (port != UNDEFINED_PORT_VALUE) {
                return port;
            }
            if (externalStatsType == ExternalStatsProtocol.GRAPHITE) {
                return GRAPHITE_DEFAULT_PORT;
            }
            if (externalStatsType == ExternalStatsProtocol.INFLUXDB) {
                return INFLUXDB_DEFAULT_PORT;
            }
        }
        throw new URISyntaxException(uri.toString(), String.format(
                "Cannot define a port for the Stats Collector host %s://%s:%s or URI scheme is incorrect. The configured URI in stats.output.uri is not supported. Please configure as the following examples: graphite://graphite-hostaddress:port, or influxdb://influxdb-hostaddress:port. Note that the port is optional, if not added the default port for the respective collector (graphite or influxdb) will be used.",
                externalStatsPrefix, externalStatsHost, externalStatsPort));
    }

    class HostCollector extends AbstractStatsCollector {
        @Override
        protected void runInContext() {
            try {
                s_logger.debug("HostStatsCollector is running...");

                SearchCriteria<HostVO> sc = createSearchCriteriaForHostTypeRoutingStateUpAndNotInMaintenance();

                Map<Object, Object> metrics = new HashMap<>();
                List<HostVO> hosts = _hostDao.search(sc, null);

                for (HostVO host : hosts) {
                    HostStatsEntry hostStatsEntry = (HostStatsEntry)_resourceMgr.getHostStatistics(host.getId());
                    if (hostStatsEntry != null) {
                        hostStatsEntry.setHostVo(host);
                        metrics.put(hostStatsEntry.getHostId(), hostStatsEntry);
                        _hostStats.put(host.getId(), hostStatsEntry);
                    } else {
                        s_logger.warn("The Host stats is null for host: " + host.getId());
                    }
                }

                if (externalStatsType == ExternalStatsProtocol.INFLUXDB) {
                    sendMetricsToInfluxdb(metrics);
                }

                updateGpuEnabledHostsDetails(hosts);
            } catch (Throwable t) {
                s_logger.error("Error trying to retrieve host stats", t);
            }
        }

        /**
         * Updates GPU details on hosts supporting GPU.
         */
        private void updateGpuEnabledHostsDetails(List<HostVO> hosts) {
            List<HostVO> gpuEnabledHosts = new ArrayList<HostVO>();
            List<Long> hostIds = _hostGpuGroupsDao.listHostIds();
            if (CollectionUtils.isEmpty(hostIds)) {
                return;
            }
            for (HostVO host : hosts) {
                if (hostIds.contains(host.getId())) {
                    gpuEnabledHosts.add(host);
                }
            }
            for (HostVO host : gpuEnabledHosts) {
                HashMap<String, HashMap<String, VgpuTypesInfo>> groupDetails = _resourceMgr.getGPUStatistics(host);
                if (MapUtils.isEmpty(groupDetails)) {
                    _resourceMgr.updateGPUDetails(host.getId(), groupDetails);
                }
            }
        }

        @Override
        protected Point creteInfluxDbPoint(Object metricsObject) {
            return createInfluxDbPointForHostMetrics(metricsObject);
        }
    }

    class VmStatsCollector extends AbstractStatsCollector {
        @Override
        protected void runInContext() {
            try {
                s_logger.trace("VmStatsCollector is running...");

                SearchCriteria<HostVO> sc = createSearchCriteriaForHostTypeRoutingStateUpAndNotInMaintenance();
                List<HostVO> hosts = _hostDao.search(sc, null);

                Map<Object, Object> metrics = new HashMap<>();

                for (HostVO host : hosts) {
                    List<UserVmVO> vms = _userVmDao.listRunningByHostId(host.getId());
                    List<Long> vmIds = new ArrayList<Long>();

                    for (UserVmVO vm : vms) {
                        vmIds.add(vm.getId());
                    }

                    try {
                        Map<Long, VmStatsEntry> vmStatsById = _userVmMgr.getVirtualMachineStatistics(host.getId(), host.getName(), vmIds);

                        if (vmStatsById != null) {
                            Set<Long> vmIdSet = vmStatsById.keySet();
                            for (Long vmId : vmIdSet) {
                                VmStatsEntry statsForCurrentIteration = vmStatsById.get(vmId);
                                statsForCurrentIteration.setVmId(vmId);
                                UserVmVO userVmVo = _userVmDao.findById(vmId);
                                statsForCurrentIteration.setUserVmVO(userVmVo);

                                storeVirtualMachineStatsInMemory(statsForCurrentIteration);

                                if (externalStatsType == ExternalStatsProtocol.GRAPHITE) {
                                    prepareVmMetricsForGraphite(metrics, statsForCurrentIteration);
                                } else {
                                    metrics.put(statsForCurrentIteration.getVmId(), statsForCurrentIteration);
                                }
                            }

                            if (!metrics.isEmpty()) {
                                if (externalStatsType == ExternalStatsProtocol.GRAPHITE) {
                                    sendVmMetricsToGraphiteHost(metrics, host);
                                } else if (externalStatsType == ExternalStatsProtocol.INFLUXDB) {
                                    sendMetricsToInfluxdb(metrics);
                                }
                            }

                            metrics.clear();
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

        @Override
        protected Point creteInfluxDbPoint(Object metricsObject) {
            return createInfluxDbPointForVmMetrics(metricsObject);
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
            //Check for ownership
            //msHost in UP state with min id should run the job
            ManagementServerHostVO msHost = _msHostDao.findOneInUpState(new Filter(ManagementServerHostVO.class, "id", true, 0L, 1L));
            if (msHost == null || (msHost.getMsid() != mgmtSrvrId)) {
                s_logger.debug("Skipping collect vm disk stats from hosts");
                return;
            }
            // collect the vm disk statistics(total) from hypervisor. added by weizhou, 2013.03.
            s_logger.trace("Running VM disk stats ...");
            try {
                Transaction.execute(new TransactionCallbackNoReturn() {
                    @Override
                    public void doInTransactionWithoutResult(TransactionStatus status) {
                        s_logger.debug("VmDiskStatsTask is running...");

                        SearchCriteria<HostVO> sc = createSearchCriteriaForHostTypeRoutingStateUpAndNotInMaintenance();
                        sc.addAnd("hypervisorType", SearchCriteria.Op.IN, HypervisorType.KVM, HypervisorType.VMware);
                        List<HostVO> hosts = _hostDao.search(sc, null);

                        for (HostVO host : hosts) {
                            List<UserVmVO> vms = _userVmDao.listRunningByHostId(host.getId());
                            List<Long> vmIds = new ArrayList<Long>();

                            for (UserVmVO  vm : vms) {
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

                                    if (CollectionUtils.isEmpty(volumes))
                                        break;

                                    VolumeVO volume = volumes.get(0);
                                    VmDiskStatisticsVO previousVmDiskStats = _vmDiskStatsDao.findBy(userVm.getAccountId(), userVm.getDataCenterId(), vmId, volume.getId());
                                    VmDiskStatisticsVO vmDiskStat_lock = _vmDiskStatsDao.lock(userVm.getAccountId(), userVm.getDataCenterId(), vmId, volume.getId());

                                    if (areAllDiskStatsZero(vmDiskStat)) {
                                        s_logger.debug("IO/bytes read and write are all 0. Not updating vm_disk_statistics");
                                        continue;
                                    }

                                    if (vmDiskStat_lock == null) {
                                        s_logger.warn("unable to find vm disk stats from host for account: " + userVm.getAccountId() + " with vmId: " + userVm.getId()
                                                + " and volumeId:" + volume.getId());
                                        continue;
                                    }

                                    if (isCurrentVmDiskStatsDifferentFromPrevious(previousVmDiskStats, vmDiskStat_lock)) {
                                        s_logger.debug("vm disk stats changed from the time GetVmDiskStatsCommand was sent. " + "Ignoring current answer. Host: " + host.getName()
                                                + " . VM: " + vmDiskStat.getVmName() + " Read(Bytes): " + vmDiskStat.getBytesRead() + " write(Bytes): " + vmDiskStat.getBytesWrite()
                                                + " Read(IO): " + vmDiskStat.getIORead() + " write(IO): " + vmDiskStat.getIOWrite());
                                        continue;
                                    }

                                    if (vmDiskStat_lock.getCurrentBytesRead() > vmDiskStat.getBytesRead()) {
                                        if (s_logger.isDebugEnabled()) {
                                            s_logger.debug("Read # of bytes that's less than the last one.  " + "Assuming something went wrong and persisting it. Host: "
                                                    + host.getName() + " . VM: " + vmDiskStat.getVmName() + " Reported: " + vmDiskStat.getBytesRead() + " Stored: "
                                                    + vmDiskStat_lock.getCurrentBytesRead());
                                        }
                                        vmDiskStat_lock.setNetBytesRead(vmDiskStat_lock.getNetBytesRead() + vmDiskStat_lock.getCurrentBytesRead());
                                    }
                                    vmDiskStat_lock.setCurrentBytesRead(vmDiskStat.getBytesRead());
                                    if (vmDiskStat_lock.getCurrentBytesWrite() > vmDiskStat.getBytesWrite()) {
                                        if (s_logger.isDebugEnabled()) {
                                            s_logger.debug("Write # of bytes that's less than the last one.  " + "Assuming something went wrong and persisting it. Host: "
                                                    + host.getName() + " . VM: " + vmDiskStat.getVmName() + " Reported: " + vmDiskStat.getBytesWrite() + " Stored: "
                                                    + vmDiskStat_lock.getCurrentBytesWrite());
                                        }
                                        vmDiskStat_lock.setNetBytesWrite(vmDiskStat_lock.getNetBytesWrite() + vmDiskStat_lock.getCurrentBytesWrite());
                                    }
                                    vmDiskStat_lock.setCurrentBytesWrite(vmDiskStat.getBytesWrite());
                                    if (vmDiskStat_lock.getCurrentIORead() > vmDiskStat.getIORead()) {
                                        if (s_logger.isDebugEnabled()) {
                                            s_logger.debug("Read # of IO that's less than the last one.  " + "Assuming something went wrong and persisting it. Host: "
                                                    + host.getName() + " . VM: " + vmDiskStat.getVmName() + " Reported: " + vmDiskStat.getIORead() + " Stored: "
                                                    + vmDiskStat_lock.getCurrentIORead());
                                        }
                                        vmDiskStat_lock.setNetIORead(vmDiskStat_lock.getNetIORead() + vmDiskStat_lock.getCurrentIORead());
                                    }
                                    vmDiskStat_lock.setCurrentIORead(vmDiskStat.getIORead());
                                    if (vmDiskStat_lock.getCurrentIOWrite() > vmDiskStat.getIOWrite()) {
                                        if (s_logger.isDebugEnabled()) {
                                            s_logger.debug("Write # of IO that's less than the last one.  " + "Assuming something went wrong and persisting it. Host: "
                                                    + host.getName() + " . VM: " + vmDiskStat.getVmName() + " Reported: " + vmDiskStat.getIOWrite() + " Stored: "
                                                    + vmDiskStat_lock.getCurrentIOWrite());
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

    class VmNetworkStatsTask extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            //Check for ownership
            //msHost in UP state with min id should run the job
            ManagementServerHostVO msHost = _msHostDao.findOneInUpState(new Filter(ManagementServerHostVO.class, "id", true, 0L, 1L));
            if (msHost == null || (msHost.getMsid() != mgmtSrvrId)) {
                s_logger.debug("Skipping collect vm network stats from hosts");
                return;
            }
            // collect the vm network statistics(total) from hypervisor
            try {
                Transaction.execute(new TransactionCallbackNoReturn() {
                    @Override
                    public void doInTransactionWithoutResult(TransactionStatus status) {
                        s_logger.debug("VmNetworkStatsTask is running...");

                        SearchCriteria<HostVO> sc = createSearchCriteriaForHostTypeRoutingStateUpAndNotInMaintenance();
                        List<HostVO> hosts = _hostDao.search(sc, null);

                        for (HostVO host : hosts) {
                            List<UserVmVO> vms = _userVmDao.listRunningByHostId(host.getId());
                            List<Long> vmIds = new ArrayList<Long>();

                            for (UserVmVO vm : vms) {
                                if (vm.getType() == VirtualMachine.Type.User) // user vm
                                    vmIds.add(vm.getId());
                            }

                            HashMap<Long, List<VmNetworkStatsEntry>> vmNetworkStatsById = _userVmMgr.getVmNetworkStatistics(host.getId(), host.getName(), vmIds);
                            if (vmNetworkStatsById == null)
                                continue;

                            Set<Long> vmIdSet = vmNetworkStatsById.keySet();
                            for (Long vmId : vmIdSet) {
                                List<VmNetworkStatsEntry> vmNetworkStats = vmNetworkStatsById.get(vmId);
                                if (vmNetworkStats == null)
                                    continue;
                                UserVmVO userVm = _userVmDao.findById(vmId);
                                if (userVm == null) {
                                    s_logger.debug("Cannot find uservm with id: " + vmId + " , continue");
                                    continue;
                                }
                                s_logger.debug("Now we are updating the user_statistics table for VM: " + userVm.getInstanceName()
                                        + " after collecting vm network statistics from host: " + host.getName());
                                for (VmNetworkStatsEntry vmNetworkStat : vmNetworkStats) {
                                    SearchCriteria<NicVO> sc_nic = _nicDao.createSearchCriteria();
                                    sc_nic.addAnd("macAddress", SearchCriteria.Op.EQ, vmNetworkStat.getMacAddress());
                                    NicVO nic = _nicDao.search(sc_nic, null).get(0);
                                    List<VlanVO> vlan = _vlanDao.listVlansByNetworkId(nic.getNetworkId());
                                    if (vlan == null || vlan.size() == 0 || vlan.get(0).getVlanType() != VlanType.DirectAttached)
                                        continue; // only get network statistics for DirectAttached network (shared networks in Basic zone and Advanced zone with/without SG)
                                    UserStatisticsVO previousvmNetworkStats = _userStatsDao.findBy(userVm.getAccountId(), userVm.getDataCenterId(), nic.getNetworkId(),
                                            nic.getIPv4Address(), vmId, "UserVm");
                                    if (previousvmNetworkStats == null) {
                                        previousvmNetworkStats = new UserStatisticsVO(userVm.getAccountId(), userVm.getDataCenterId(), nic.getIPv4Address(), vmId, "UserVm",
                                                nic.getNetworkId());
                                        _userStatsDao.persist(previousvmNetworkStats);
                                    }
                                    UserStatisticsVO vmNetworkStat_lock = _userStatsDao.lock(userVm.getAccountId(), userVm.getDataCenterId(), nic.getNetworkId(),
                                            nic.getIPv4Address(), vmId, "UserVm");

                                    if ((vmNetworkStat.getBytesSent() == 0) && (vmNetworkStat.getBytesReceived() == 0)) {
                                        s_logger.debug("bytes sent and received are all 0. Not updating user_statistics");
                                        continue;
                                    }

                                    if (vmNetworkStat_lock == null) {
                                        s_logger.warn("unable to find vm network stats from host for account: " + userVm.getAccountId() + " with vmId: " + userVm.getId()
                                                + " and nicId:" + nic.getId());
                                        continue;
                                    }

                                    if (previousvmNetworkStats != null && ((previousvmNetworkStats.getCurrentBytesSent() != vmNetworkStat_lock.getCurrentBytesSent())
                                            || (previousvmNetworkStats.getCurrentBytesReceived() != vmNetworkStat_lock.getCurrentBytesReceived()))) {
                                        s_logger.debug("vm network stats changed from the time GetNmNetworkStatsCommand was sent. " + "Ignoring current answer. Host: "
                                                + host.getName() + " . VM: " + vmNetworkStat.getVmName() + " Sent(Bytes): " + vmNetworkStat.getBytesSent() + " Received(Bytes): "
                                                + vmNetworkStat.getBytesReceived());
                                        continue;
                                    }

                                    if (vmNetworkStat_lock.getCurrentBytesSent() > vmNetworkStat.getBytesSent()) {
                                        if (s_logger.isDebugEnabled()) {
                                            s_logger.debug("Sent # of bytes that's less than the last one.  " + "Assuming something went wrong and persisting it. Host: "
                                                    + host.getName() + " . VM: " + vmNetworkStat.getVmName() + " Reported: " + vmNetworkStat.getBytesSent() + " Stored: "
                                                    + vmNetworkStat_lock.getCurrentBytesSent());
                                        }
                                        vmNetworkStat_lock.setNetBytesSent(vmNetworkStat_lock.getNetBytesSent() + vmNetworkStat_lock.getCurrentBytesSent());
                                    }
                                    vmNetworkStat_lock.setCurrentBytesSent(vmNetworkStat.getBytesSent());

                                    if (vmNetworkStat_lock.getCurrentBytesReceived() > vmNetworkStat.getBytesReceived()) {
                                        if (s_logger.isDebugEnabled()) {
                                            s_logger.debug("Received # of bytes that's less than the last one.  " + "Assuming something went wrong and persisting it. Host: "
                                                    + host.getName() + " . VM: " + vmNetworkStat.getVmName() + " Reported: " + vmNetworkStat.getBytesReceived() + " Stored: "
                                                    + vmNetworkStat_lock.getCurrentBytesReceived());
                                        }
                                        vmNetworkStat_lock.setNetBytesReceived(vmNetworkStat_lock.getNetBytesReceived() + vmNetworkStat_lock.getCurrentBytesReceived());
                                    }
                                    vmNetworkStat_lock.setCurrentBytesReceived(vmNetworkStat.getBytesReceived());

                                    if (!_dailyOrHourly) {
                                        //update agg bytes
                                        vmNetworkStat_lock.setAggBytesReceived(vmNetworkStat_lock.getNetBytesReceived() + vmNetworkStat_lock.getCurrentBytesReceived());
                                        vmNetworkStat_lock.setAggBytesSent(vmNetworkStat_lock.getNetBytesSent() + vmNetworkStat_lock.getCurrentBytesSent());
                                    }

                                    _userStatsDao.update(vmNetworkStat_lock.getId(), vmNetworkStat_lock);
                                }
                            }
                        }
                    }
                });
            } catch (Exception e) {
                s_logger.warn("Error while collecting vm network stats from hosts", e);
            }
        }
    }

    class VolumeStatsTask extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            try {
                List<StoragePoolVO> pools = _storagePoolDao.listAll();

                for (StoragePoolVO pool : pools) {
                    List<VolumeVO> volumes = _volsDao.findByPoolId(pool.getId(), null);
                    List<String> volumeLocators = new ArrayList<String>();
                    for (VolumeVO volume : volumes) {
                        if (volume.getFormat() == ImageFormat.QCOW2) {
                            if (pool.isManaged()) {
                                volumeLocators.add(volume.getPath());
                            } else {
                                volumeLocators.add(volume.getUuid());
                            }
                        } else if (volume.getFormat() == ImageFormat.VHD) {
                            volumeLocators.add(volume.getPath());
                        } else if (volume.getFormat() == ImageFormat.OVA) {
                            volumeLocators.add(volume.getChainInfo());
                        } else {
                            s_logger.warn("Volume stats not implemented for this format type " + volume.getFormat());
                            break;
                        }
                    }
                    try {
                        Map<String, VolumeStatsEntry> volumeStatsByUuid;
                        if (pool.getScope() == ScopeType.ZONE) {
                            volumeStatsByUuid = new HashMap<>();
                            for (final Cluster cluster : _clusterDao.listByZoneId(pool.getDataCenterId())) {
                                final Map<String, VolumeStatsEntry> volumeStatsForCluster = _userVmMgr.getVolumeStatistics(cluster.getId(), pool.getUuid(), pool.getPoolType(),
                                        volumeLocators, StatsTimeout.value());
                                if (volumeStatsForCluster != null) {
                                    volumeStatsByUuid.putAll(volumeStatsForCluster);
                                }
                            }
                        } else {
                            volumeStatsByUuid = _userVmMgr.getVolumeStatistics(pool.getClusterId(), pool.getUuid(), pool.getPoolType(), volumeLocators, StatsTimeout.value());
                        }
                        if (volumeStatsByUuid != null) {
                            for (final Map.Entry<String, VolumeStatsEntry> entry : volumeStatsByUuid.entrySet()) {
                                if (entry == null || entry.getKey() == null || entry.getValue() == null) {
                                    continue;
                                }
                                _volumeStats.put(entry.getKey(), entry.getValue());
                            }
                        }
                    } catch (Exception e) {
                        s_logger.warn("Failed to get volume stats for cluster with ID: " + pool.getClusterId(), e);
                        continue;
                    }
                }
            } catch (Throwable t) {
                s_logger.error("Error trying to retrieve volume stats", t);
            }
        }
    }

    public VolumeStats getVolumeStats(String volumeLocator) {
        if (volumeLocator != null && _volumeStats.containsKey(volumeLocator)) {
            return _volumeStats.get(volumeLocator);
        }
        return null;
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

                    Integer nfsVersion = imageStoreDetailsUtil.getNfsVersion(store.getId());
                    GetStorageStatsCommand command = new GetStorageStatsCommand(store.getTO(), nfsVersion);
                    EndPoint ssAhost = _epSelector.select(store);
                    if (ssAhost == null) {
                        s_logger.debug("There is no secondary storage VM for secondary storage host " + store.getName());
                        continue;
                    }
                    long storeId = store.getId();
                    Answer answer = ssAhost.sendMessage(command);
                    if (answer != null && answer.getResult()) {
                        storageStats.put(storeId, (StorageStats)answer);
                        s_logger.trace("HostId: " + storeId + " Used: " + ((StorageStats)answer).getByteUsed() + " Total Available: " + ((StorageStats)answer).getCapacityBytes());
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
                            if ((now - asGroup.getLastInterval().getTime()) < asGroup.getInterval()) {
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

        public List<Pair<String, Integer>> getPairofCounternameAndDuration(long groupId) {
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

    /**
     * This class allows to writing metrics in InfluxDB for the table that matches the Collector extending it.
     * Thus, VmStatsCollector and HostCollector can use same method to write on different measures (host_stats or vm_stats table).
     */
    abstract class AbstractStatsCollector extends ManagedContextRunnable {
        /**
         * Sends metrics to influxdb host. This method supports both VM and Host metrics
         */
        protected void sendMetricsToInfluxdb(Map<Object, Object> metrics) {
            InfluxDB influxDbConnection = createInfluxDbConnection();

            Pong response = influxDbConnection.ping();
            if (response.getVersion().equalsIgnoreCase("unknown")) {
                throw new CloudRuntimeException(String.format("Cannot ping influxdb host %s:%s.", externalStatsHost, externalStatsPort));
            }

            Collection<Object> metricsObjects = metrics.values();
            List<Point> points = new ArrayList<>();

            s_logger.debug(String.format("Sending stats to %s host %s:%s", externalStatsType, externalStatsHost, externalStatsPort));

            for (Object metricsObject : metricsObjects) {
                Point vmPoint = creteInfluxDbPoint(metricsObject);
                points.add(vmPoint);
            }
            writeBatches(influxDbConnection, databaseName, points);
        }

        /**
         * Creates a InfluxDB point for the given stats collector (VmStatsCollector, or HostCollector).
         */
        protected abstract Point creteInfluxDbPoint(Object metricsObject);
    }

    public boolean imageStoreHasEnoughCapacity(DataStore imageStore) {
        if (!_storageStats.keySet().contains(imageStore.getId())) { // Stats not available for this store yet, can be a new store. Better to assume it has enough capacity?
            return true;
        }
        StorageStats imageStoreStats = _storageStats.get(imageStore.getId());
        if (imageStoreStats != null && (imageStoreStats.getByteUsed() / (imageStoreStats.getCapacityBytes() * 1.0)) <= _imageStoreCapacityThreshold) {
            return true;
        }
        return false;
    }

    public long imageStoreCurrentFreeCapacity(DataStore imageStore) {
        StorageStats imageStoreStats = _storageStats.get(imageStore.getId());
        return imageStoreStats != null ? Math.max(0, imageStoreStats.getCapacityBytes() - imageStoreStats.getByteUsed()) : 0;
    }

    /**
     * Calculates secondary storage disk capacity against a configurable threshold instead of the hardcoded default 95 % value
     * @param imageStore secondary storage
     * @param storeCapThreshold the threshold capacity for computing if secondary storage has enough space to accommodate the @this object
     * @return
     */
    public boolean imageStoreHasEnoughCapacity(DataStore imageStore, Double storeCapThreshold) {
        StorageStats imageStoreStats = _storageStats.get(imageStore.getId());
        if (imageStoreStats != null && (imageStoreStats.getByteUsed() / (imageStoreStats.getCapacityBytes() * 1.0)) <= storeCapThreshold) {
            return true;
        }
        return false;
    }

    /**
     * Sends VMs metrics to the configured graphite host.
     */
    protected void sendVmMetricsToGraphiteHost(Map<Object, Object> metrics, HostVO host) {
        s_logger.debug(String.format("Sending VmStats of host %s to %s host %s:%s", host.getId(), externalStatsType, externalStatsHost, externalStatsPort));
        try {
            GraphiteClient g = new GraphiteClient(externalStatsHost, externalStatsPort);
            g.sendMetrics(metrics);
        } catch (GraphiteException e) {
            s_logger.debug("Failed sending VmStats to Graphite host " + externalStatsHost + ":" + externalStatsPort + ": " + e.getMessage());
        }
    }

    /**
     * Prepares metrics for Graphite.
     * @note this method must only be executed in case the configured stats collector is a Graphite host;
     * otherwise, it will compromise the map of metrics used by another type of collector (e.g. InfluxDB).
     */
    private void prepareVmMetricsForGraphite(Map<Object, Object> metrics, VmStatsEntry statsForCurrentIteration) {
        VMInstanceVO vmVO = _vmInstance.findById(statsForCurrentIteration.getVmId());
        String vmName = vmVO.getUuid();

        metrics.put(externalStatsPrefix + "cloudstack.stats.instances." + vmName + ".cpu.num", statsForCurrentIteration.getNumCPUs());
        metrics.put(externalStatsPrefix + "cloudstack.stats.instances." + vmName + ".cpu.utilization", statsForCurrentIteration.getCPUUtilization());
        metrics.put(externalStatsPrefix + "cloudstack.stats.instances." + vmName + ".network.read_kbs", statsForCurrentIteration.getNetworkReadKBs());
        metrics.put(externalStatsPrefix + "cloudstack.stats.instances." + vmName + ".network.write_kbs", statsForCurrentIteration.getNetworkWriteKBs());
        metrics.put(externalStatsPrefix + "cloudstack.stats.instances." + vmName + ".disk.write_kbs", statsForCurrentIteration.getDiskWriteKBs());
        metrics.put(externalStatsPrefix + "cloudstack.stats.instances." + vmName + ".disk.read_kbs", statsForCurrentIteration.getDiskReadKBs());
        metrics.put(externalStatsPrefix + "cloudstack.stats.instances." + vmName + ".disk.write_iops", statsForCurrentIteration.getDiskWriteIOs());
        metrics.put(externalStatsPrefix + "cloudstack.stats.instances." + vmName + ".disk.read_iops", statsForCurrentIteration.getDiskReadIOs());
        metrics.put(externalStatsPrefix + "cloudstack.stats.instances." + vmName + ".memory.total_kbs", statsForCurrentIteration.getMemoryKBs());
        metrics.put(externalStatsPrefix + "cloudstack.stats.instances." + vmName + ".memory.internalfree_kbs", statsForCurrentIteration.getIntFreeMemoryKBs());
        metrics.put(externalStatsPrefix + "cloudstack.stats.instances." + vmName + ".memory.target_kbs", statsForCurrentIteration.getTargetMemoryKBs());
    }

    /**
     * Stores virtual machine stats in memory (map of {@link VmStatsEntry}).
     */
    private void storeVirtualMachineStatsInMemory(VmStatsEntry statsForCurrentIteration) {
        VmStatsEntry statsInMemory = (VmStatsEntry)_VmStats.get(statsForCurrentIteration.getVmId());

        if (statsInMemory == null) {
            //no stats exist for this vm, directly persist
            _VmStats.put(statsForCurrentIteration.getVmId(), statsForCurrentIteration);
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
            statsInMemory.setMemoryKBs(statsForCurrentIteration.getMemoryKBs());
            statsInMemory.setIntFreeMemoryKBs(statsForCurrentIteration.getIntFreeMemoryKBs());
            statsInMemory.setTargetMemoryKBs(statsForCurrentIteration.getTargetMemoryKBs());

            _VmStats.put(statsForCurrentIteration.getVmId(), statsInMemory);
        }
    }

    /**
     * Sends host metrics to a configured InfluxDB host. The metrics respects the following specification.</br>
     * <b>Tags:</b>vm_id, uuid, instance_name, data_center_id, host_id</br>
     * <b>Fields:</b>memory_total_kb, memory_internal_free_kbs, memory_target_kbs, cpu_utilization, cpus, network_write_kb, disk_read_iops, disk_read_kbs, disk_write_iops, disk_write_kbs
     */
    protected Point createInfluxDbPointForHostMetrics(Object metricsObject) {
        HostStatsEntry hostStatsEntry = (HostStatsEntry)metricsObject;

        Map<String, String> tagsToAdd = new HashMap<>();
        tagsToAdd.put(UUID_TAG, hostStatsEntry.getHostVo().getUuid());

        Map<String, Object> fieldsToAdd = new HashMap<>();
        fieldsToAdd.put(TOTAL_MEMORY_KBS_FIELD, hostStatsEntry.getTotalMemoryKBs());
        fieldsToAdd.put(FREE_MEMORY_KBS_FIELD, hostStatsEntry.getFreeMemoryKBs());
        fieldsToAdd.put(CPU_UTILIZATION_FIELD, hostStatsEntry.getCpuUtilization());
        fieldsToAdd.put(CPUS_FIELD, hostStatsEntry.getHostVo().getCpus());
        fieldsToAdd.put(CPU_SOCKETS_FIELD, hostStatsEntry.getHostVo().getCpuSockets());
        fieldsToAdd.put(NETWORK_READ_KBS_FIELD, hostStatsEntry.getNetworkReadKBs());
        fieldsToAdd.put(NETWORK_WRITE_KBS_FIELD, hostStatsEntry.getNetworkWriteKBs());

        return Point.measurement(INFLUXDB_HOST_MEASUREMENT).tag(tagsToAdd).time(System.currentTimeMillis(), TimeUnit.MILLISECONDS).fields(fieldsToAdd).build();
    }

    /**
     * Sends VMs metrics to a configured InfluxDB host. The metrics respects the following specification.</br>
     * <b>Tags:</b>vm_id, uuid, instance_name, data_center_id, host_id</br>
     * <b>Fields:</b>memory_total_kb, memory_internal_free_kbs, memory_target_kbs, cpu_utilization, cpus, network_write_kb, disk_read_iops, disk_read_kbs, disk_write_iops, disk_write_kbs
     */
    protected Point createInfluxDbPointForVmMetrics(Object metricsObject) {
        VmStatsEntry vmStatsEntry = (VmStatsEntry)metricsObject;
        UserVmVO userVmVO = vmStatsEntry.getUserVmVO();

        Map<String, String> tagsToAdd = new HashMap<>();
        tagsToAdd.put(UUID_TAG, userVmVO.getUuid());

        Map<String, Object> fieldsToAdd = new HashMap<>();
        fieldsToAdd.put(TOTAL_MEMORY_KBS_FIELD, vmStatsEntry.getMemoryKBs());
        fieldsToAdd.put(FREE_MEMORY_KBS_FIELD, vmStatsEntry.getIntFreeMemoryKBs());
        fieldsToAdd.put(MEMORY_TARGET_KBS_FIELD, vmStatsEntry.getTargetMemoryKBs());
        fieldsToAdd.put(CPU_UTILIZATION_FIELD, vmStatsEntry.getCPUUtilization());
        fieldsToAdd.put(CPUS_FIELD, vmStatsEntry.getNumCPUs());
        fieldsToAdd.put(NETWORK_READ_KBS_FIELD, vmStatsEntry.getNetworkReadKBs());
        fieldsToAdd.put(NETWORK_WRITE_KBS_FIELD, vmStatsEntry.getNetworkWriteKBs());
        fieldsToAdd.put(DISK_READ_IOPS_FIELD, vmStatsEntry.getDiskReadIOs());
        fieldsToAdd.put(DISK_READ_KBS_FIELD, vmStatsEntry.getDiskReadKBs());
        fieldsToAdd.put(DISK_WRITE_IOPS_FIELD, vmStatsEntry.getDiskWriteIOs());
        fieldsToAdd.put(DISK_WRITE_KBS_FIELD, vmStatsEntry.getDiskWriteKBs());

        return Point.measurement(INFLUXDB_VM_MEASUREMENT).tag(tagsToAdd).time(System.currentTimeMillis(), TimeUnit.MILLISECONDS).fields(fieldsToAdd).build();
    }

    /**
     * Creates connection to InfluxDB. If the database does not exist, it throws a CloudRuntimeException. </br>
     * @note the user can configure the database name on parameter 'stats.output.influxdb.database.name'; such database must be yet created and configured by the user.
     * The Default name for the database is 'cloudstack_stats'.
     */
    protected InfluxDB createInfluxDbConnection() {
        String influxDbQueryUrl = String.format("http://%s:%s/", externalStatsHost, externalStatsPort);
        InfluxDB influxDbConnection = InfluxDBFactory.connect(influxDbQueryUrl);

        if (!influxDbConnection.databaseExists(databaseName)) {
            throw new CloudRuntimeException(String.format("Database with name %s does not exist in influxdb host %s:%s", databaseName, externalStatsHost, externalStatsPort));
        }

        return influxDbConnection;
    }

    /**
     * Writes batches of InfluxDB database points into a given database.
     */
    protected void writeBatches(InfluxDB influxDbConnection, String dbName, List<Point> points) {
        BatchPoints batchPoints = BatchPoints.database(dbName).build();
        influxDbConnection.enableBatch(BatchOptions.DEFAULTS);

        for (Point point : points) {
            batchPoints.point(point);
        }

        influxDbConnection.write(batchPoints);
    }

    /**
     * Returns true if at least one of the current disk stats is different from the previous.</br>
     * The considered disk stats are the following: bytes read, bytes write, IO read,  and IO write.
     */
    protected boolean isCurrentVmDiskStatsDifferentFromPrevious(VmDiskStatisticsVO previousVmDiskStats, VmDiskStatisticsVO currentVmDiskStats) {
        if (previousVmDiskStats != null) {
            boolean bytesReadDifferentFromPrevious = previousVmDiskStats.getCurrentBytesRead() != currentVmDiskStats.getCurrentBytesRead();
            boolean bytesWriteDifferentFromPrevious = previousVmDiskStats.getCurrentBytesWrite() != currentVmDiskStats.getCurrentBytesWrite();
            boolean ioReadDifferentFromPrevious = previousVmDiskStats.getCurrentIORead() != currentVmDiskStats.getCurrentIORead();
            boolean ioWriteDifferentFromPrevious = previousVmDiskStats.getCurrentIOWrite() != currentVmDiskStats.getCurrentIOWrite();
            return bytesReadDifferentFromPrevious || bytesWriteDifferentFromPrevious || ioReadDifferentFromPrevious || ioWriteDifferentFromPrevious;
        }
        if (currentVmDiskStats == null) {
            return false;
        }
        return true;
    }

    /**
     * Returns true if all the VmDiskStatsEntry are Zeros (Bytes read, Bytes write, IO read, and IO write must be all equals to zero)
     */
    protected boolean areAllDiskStatsZero(VmDiskStatsEntry vmDiskStat) {
        return (vmDiskStat.getBytesRead() == 0) && (vmDiskStat.getBytesWrite() == 0) && (vmDiskStat.getIORead() == 0) && (vmDiskStat.getIOWrite() == 0);
    }

    /**
     * Creates a HostVO SearchCriteria where:
     * <ul>
     *  <li>"status" is Up;</li>
     *  <li>"resourceState" is not in Maintenance, PrepareForMaintenance, or ErrorInMaintenance; and</li>
     *  <li>"type" is Routing.</li>
     * </ul>
     */
    private SearchCriteria<HostVO> createSearchCriteriaForHostTypeRoutingStateUpAndNotInMaintenance() {
        SearchCriteria<HostVO> sc = _hostDao.createSearchCriteria();
        sc.addAnd("status", SearchCriteria.Op.EQ, Status.Up.toString());
        sc.addAnd("resourceState", SearchCriteria.Op.NIN,
                ResourceState.Maintenance,
                ResourceState.PrepareForMaintenance,
                ResourceState.ErrorInPrepareForMaintenance,
                ResourceState.ErrorInMaintenance);
        sc.addAnd("type", SearchCriteria.Op.EQ, Host.Type.Routing.toString());
        return sc;
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

    @Override
    public String getConfigComponentName() {
        return StatsCollector.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {vmDiskStatsInterval, vmDiskStatsIntervalMin, vmNetworkStatsInterval, vmNetworkStatsIntervalMin, StatsTimeout, statsOutputUri};
    }

    public double getImageStoreCapacityThreshold() {
        return _imageStoreCapacityThreshold;
    }
}
