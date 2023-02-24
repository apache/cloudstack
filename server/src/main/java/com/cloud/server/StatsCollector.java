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

import static com.cloud.utils.NumbersUtil.toHumanReadableSize;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProvider;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.management.ManagementServerHost;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.utils.bytescale.ByteScaleUtils;
import org.apache.cloudstack.utils.graphite.GraphiteClient;
import org.apache.cloudstack.utils.graphite.GraphiteException;
import org.apache.cloudstack.utils.identity.ManagementServerNode;
import org.apache.cloudstack.utils.usage.UsageUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;
import org.influxdb.BatchOptions;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.Pong;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.GetStorageStatsCommand;
import com.cloud.agent.api.HostStatsEntry;
import com.cloud.agent.api.VgpuTypesInfo;
import com.cloud.agent.api.VmDiskStatsEntry;
import com.cloud.agent.api.VmNetworkStatsEntry;
import com.cloud.agent.api.VmStatsEntry;
import com.cloud.agent.api.VmStatsEntryBase;
import com.cloud.agent.api.VolumeStatsEntry;
import com.cloud.api.ApiSessionListener;
import com.cloud.capacity.CapacityManager;
import com.cloud.cluster.ClusterManager;
import com.cloud.cluster.ClusterManagerListener;
import com.cloud.cluster.ClusterServicePdu;
import com.cloud.cluster.ManagementServerHostVO;
import com.cloud.cluster.ManagementServerStatusVO;
import com.cloud.cluster.dao.ManagementServerHostDao;
import com.cloud.cluster.dao.ManagementServerStatusDao;
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
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.as.AutoScaleManager;
import com.cloud.org.Cluster;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceState;
import com.cloud.serializer.GsonHelper;
import com.cloud.storage.ImageStoreDetailsUtil;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StorageStats;
import com.cloud.storage.VolumeStats;
import com.cloud.storage.VolumeStatsVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeStatsDao;
import com.cloud.user.UserStatisticsVO;
import com.cloud.user.VmDiskStatisticsVO;
import com.cloud.user.dao.UserStatisticsDao;
import com.cloud.user.dao.VmDiskStatisticsDao;
import com.cloud.utils.LogUtils;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.ComponentMethodInterceptable;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DbProperties;
import com.cloud.utils.db.DbUtil;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.MacAddress;
import com.cloud.utils.script.Script;
import com.cloud.vm.NicVO;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VmDiskStats;
import com.cloud.vm.VmNetworkStats;
import com.cloud.vm.VmStats;
import com.cloud.vm.VmStatsVO;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.vm.dao.VmStatsDao;
import com.codahale.metrics.JvmAttributeGaugeSet;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.jvm.BufferPoolMetricSet;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.sun.management.OperatingSystemMXBean;

/**
 * Provides real time stats for various agent resources up to x seconds
 *
 * @startuml
 *
 * StatsCollector -> ClusterManager : register
 * ClusterManager -> StatsCollector : onManagementNodeJoined
 * StatsCollector -> list : add MS
 * ClusterManager -> StatsCollector : onManagementNodeJoined
 * StatsCollector -> list : add MS to send list
 * StatsCollector -> collector : update own status
 * StatsCollector -> list : get all ms ids
 * StatsCollector -> ClusterManager : update status for my (ms id) to all ms_ids
 * ClusterManager -> ClusterManager : update ms_ids on status on (ms id)
 * ClusterManager -> StatsCollector : onManagementNodeLeft
 * StatsCollector -> list : add MS
 * ClusterManager -> StatsCollector : status data updated for (ms id)
 * StatsCollector -> StatsCollector : update entry for (ms id)
 * ClusterManager -> StatsCollector : onManagementNodeLeft
 * StatsCollector -> list : add MS
 * @enduml
 */
@Component
public class StatsCollector extends ManagerBase implements ComponentMethodInterceptable, Configurable, DbStatsCollection {

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

    private static final Logger LOGGER = Logger.getLogger(StatsCollector.class);

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

    private static final int HOURLY_TIME = 60;
    private static final int DAILY_TIME = HOURLY_TIME * 24;
    private static final Long ONE_MINUTE_IN_MILLISCONDS = 60000L;

    private static final String DEFAULT_DATABASE_NAME = "cloudstack";
    private static final String INFLUXDB_HOST_MEASUREMENT = "host_stats";
    private static final String INFLUXDB_VM_MEASUREMENT = "vm_stats";

    public static final ConfigKey<Integer> MANAGEMENT_SERVER_STATUS_COLLECTION_INTERVAL = new ConfigKey<>("Advanced",
            Integer.class, "management.server.stats.interval", "60",
            "Time interval in seconds, for management servers stats collection. Set to <= 0 to disable management servers stats.", false);
    private static final ConfigKey<Integer> DATABASE_SERVER_STATUS_COLLECTION_INTERVAL = new ConfigKey<>("Advanced",
            Integer.class, "database.server.stats.interval", "60",
            "Time interval in seconds, for database servers stats collection. Set to <= 0 to disable database servers stats.", false);
    private static final ConfigKey<Integer> DATABASE_SERVER_LOAD_HISTORY_RETENTION_NUMBER = new ConfigKey<>("Advanced",
            Integer.class, "database.server.stats.retention", "3",
            "The number of queries/seconds values to retain in history. This will define for how many periods of 'database.server.stats.interval' seconds, the queries/seconds values will be kept in memory",
            true);
    private static final ConfigKey<Integer> vmDiskStatsInterval = new ConfigKey<>("Advanced", Integer.class, "vm.disk.stats.interval", "0",
            "Interval (in seconds) to report vm disk statistics. Vm disk statistics will be disabled if this is set to 0 or less than 0.", false);
    private static final ConfigKey<Integer> vmDiskStatsIntervalMin = new ConfigKey<>("Advanced", Integer.class, "vm.disk.stats.interval.min", "300",
            "Minimal interval (in seconds) to report vm disk statistics. If vm.disk.stats.interval is smaller than this, use this to report vm disk statistics.", false);
    private static final ConfigKey<Integer> vmNetworkStatsInterval = new ConfigKey<>("Advanced", Integer.class, "vm.network.stats.interval", "0",
            "Interval (in seconds) to report vm network statistics (for Shared networks). Vm network statistics will be disabled if this is set to 0 or less than 0.", false);
    private static final ConfigKey<Integer> vmNetworkStatsIntervalMin = new ConfigKey<>("Advanced", Integer.class, "vm.network.stats.interval.min", "300",
            "Minimal Interval (in seconds) to report vm network statistics (for Shared networks). If vm.network.stats.interval is smaller than this, use this to report vm network statistics.",
            false);
    private static final ConfigKey<Integer> StatsTimeout = new ConfigKey<>("Advanced", Integer.class, "stats.timeout", "60000",
            "The timeout for stats call in milli seconds.", true,
            ConfigKey.Scope.Cluster);
    private static final ConfigKey<String> statsOutputUri = new ConfigKey<>("Advanced", String.class, "stats.output.uri", "",
            "URI to send StatsCollector statistics to. The collector is defined on the URI scheme. Example: graphite://graphite-hostaddress:port or influxdb://influxdb-hostaddress/dbname. Note that the port is optional, if not added the default port for the respective collector (graphite or influxdb) will be used. Additionally, the database name '/dbname' is  also optional; default db name is 'cloudstack'. You must create and configure the database if using influxdb.",
            true);
    protected static ConfigKey<Boolean> vmStatsIncrementMetrics = new ConfigKey<>("Advanced", Boolean.class, "vm.stats.increment.metrics", "true",
            "When set to 'true', VM metrics(NetworkReadKBs, NetworkWriteKBs, DiskWriteKBs, DiskReadKBs, DiskReadIOs and DiskWriteIOs) that are collected from the hypervisor are summed before being returned."
                    + "On the other hand, when set to 'false', the VM metrics API will just display the latest metrics collected.", true);
    private static final ConfigKey<Boolean> VM_STATS_INCREMENT_METRICS_IN_MEMORY = new ConfigKey<>("Advanced", Boolean.class, "vm.stats.increment.metrics.in.memory", "true",
            "When set to 'true', VM metrics(NetworkReadKBs, NetworkWriteKBs, DiskWriteKBs, DiskReadKBs, DiskReadIOs and DiskWriteIOs) that are collected from the hypervisor are summed and stored in memory. "
            + "On the other hand, when set to 'false', the VM metrics API will just display the latest metrics collected.", true);
    protected static ConfigKey<Integer> vmStatsMaxRetentionTime = new ConfigKey<>("Advanced", Integer.class, "vm.stats.max.retention.time", "720",
            "The maximum time (in minutes) for keeping VM stats records in the database. The VM stats cleanup process will be disabled if this is set to 0 or less than 0.", true);

    protected static ConfigKey<Boolean> vmStatsCollectUserVMOnly = new ConfigKey<>("Advanced", Boolean.class, "vm.stats.user.vm.only", "false",
            "When set to 'false' stats for system VMs will be collected otherwise stats collection will be done only for user VMs", true);

    protected static ConfigKey<Boolean> vmDiskStatsRetentionEnabled = new ConfigKey<>("Advanced", Boolean.class, "vm.disk.stats.retention.enabled", "false",
            "When set to 'true' stats for VM disks will be stored in the database otherwise disk stats will not be stored", true);

    protected static ConfigKey<Integer> vmDiskStatsMaxRetentionTime = new ConfigKey<>("Advanced", Integer.class, "vm.disk.stats.max.retention.time", "720",
            "The maximum time (in minutes) for keeping VM disks stats records in the database. The VM disks stats cleanup process will be disabled if this is set to 0 or less than 0.", true);

    private static StatsCollector s_instance = null;

    private static Gson gson = new Gson();

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
    protected UserVmDao _userVmDao;
    @Inject
    protected VmStatsDao vmStatsDao;
    @Inject
    private VolumeDao _volsDao;
    @Inject
    protected VolumeStatsDao volumeStatsDao;
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
    private UserStatisticsDao _userStatsDao;
    @Inject
    private NicDao _nicDao;
    @Inject
    private VlanDao _vlanDao;
    @Inject
    private AutoScaleManager _asManager;
    @Inject
    private VMInstanceDao _vmInstance;
    @Inject
    private HostGpuGroupsDao _hostGpuGroupsDao;
    @Inject
    private ImageStoreDetailsUtil imageStoreDetailsUtil;
    @Inject
    private ManagementServerHostDao managementServerHostDao;
    // stats collector is now a clustered agent
    @Inject
    private ClusterManager clusterManager;
    @Inject
    private ManagementServerStatusDao managementServerStatusDao;
    @Inject
    VirtualMachineManager virtualMachineManager;

    private final ConcurrentHashMap<String, ManagementServerHostStats> managementServerHostStats = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> dbStats = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, HostStats> _hostStats = new ConcurrentHashMap<>();
    protected ConcurrentHashMap<Long, VmStats> _VmStats = new ConcurrentHashMap<>();
    private final Map<String, VolumeStats> _volumeStats = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Long, StorageStats> _storageStats = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Long, StorageStats> _storagePoolStats = new ConcurrentHashMap<>();

    private static final long DEFAULT_INITIAL_DELAY = 15000L;

    private long hostStatsInterval = -1L;
    private long vmStatsInterval = -1L;
    private long storageStatsInterval = -1L;
    private long volumeStatsInterval = -1L;
    private long autoScaleStatsInterval = -1L;

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
    protected long managementServerNodeId = ManagementServerNode.getManagementServerId();
    protected long msId = managementServerNodeId;
    final static MetricRegistry METRIC_REGISTRY = new MetricRegistry();

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
        registerAll("gc", new GarbageCollectorMetricSet(), METRIC_REGISTRY);
        registerAll("buffers", new BufferPoolMetricSet(ManagementFactory.getPlatformMBeanServer()), METRIC_REGISTRY);
        registerAll("memory", new MemoryUsageGaugeSet(), METRIC_REGISTRY);
        registerAll("threads", new ThreadStatesGaugeSet(), METRIC_REGISTRY);
        registerAll("jvm", new JvmAttributeGaugeSet(), METRIC_REGISTRY);
        return true;
    }
    @Override
    public boolean stop() {
        _executor.shutdown();
        return true;
    }

    private void registerAll(String prefix, MetricSet metricSet, MetricRegistry registry) {
        String registryTemplate = new String(prefix + "%s");
        for (Map.Entry<String, Metric> entry : metricSet.getMetrics().entrySet()) {
            String registryName = String.format(registryTemplate, entry.getKey());
            if (entry.getValue() instanceof MetricSet) {
                registerAll(registryName, (MetricSet) entry.getValue(), registry);
            } else {
                registry.register(registryName, entry.getValue());
            }
        }
    }

    protected void init(Map<String, String> configs) {
        _executor = Executors.newScheduledThreadPool(6, new NamedThreadFactory("StatsCollector"));

        hostStatsInterval = NumbersUtil.parseLong(configs.get("host.stats.interval"), ONE_MINUTE_IN_MILLISCONDS);
        vmStatsInterval = NumbersUtil.parseLong(configs.get("vm.stats.interval"), ONE_MINUTE_IN_MILLISCONDS);
        storageStatsInterval = NumbersUtil.parseLong(configs.get("storage.stats.interval"), ONE_MINUTE_IN_MILLISCONDS);
        volumeStatsInterval = NumbersUtil.parseLong(configs.get("volume.stats.interval"), ONE_MINUTE_IN_MILLISCONDS);
        autoScaleStatsInterval = AutoScaleManager.AutoScaleStatsInterval.value();
        ManagementServerStatusAdministrator managementServerStatusAdministrator = new ManagementServerStatusAdministrator();
        clusterManager.registerStatusAdministrator(managementServerStatusAdministrator);
        clusterManager.registerListener(managementServerStatusAdministrator);

        gson = GsonHelper.getGson();

        String statsUri = statsOutputUri.value();
        if (StringUtils.isNotBlank(statsUri)) {
            try {
                URI uri = new URI(statsUri);
                externalStatsScheme = uri.getScheme();

                try {
                    externalStatsType = ExternalStatsProtocol.valueOf(externalStatsScheme.toUpperCase());
                } catch (IllegalArgumentException e) {
                    LOGGER.error(externalStatsScheme + " is not a valid protocol for external statistics. No statistics will be send.");
                }

                if (StringUtils.isNotEmpty(uri.getHost())) {
                    externalStatsHost = uri.getHost();
                }

                externalStatsPort = retrieveExternalStatsPortFromUri(uri);

                databaseName = configureDatabaseName(uri);

                if (StringUtils.isNotEmpty(uri.getPath())) {
                    externalStatsPrefix = uri.getPath().substring(1);
                }

                /* Append a dot (.) to the prefix if it is set */
                if (StringUtils.isNotEmpty(externalStatsPrefix)) {
                    externalStatsPrefix += ".";
                } else {
                    externalStatsPrefix = "";
                }

            } catch (URISyntaxException e) {
                LOGGER.error("Failed to parse external statistics URI: ", e);
            }
        }

        if (hostStatsInterval > 0) {
            _executor.scheduleWithFixedDelay(new HostCollector(), DEFAULT_INITIAL_DELAY, hostStatsInterval, TimeUnit.MILLISECONDS);
        }

        if (vmStatsInterval > 0) {
            _executor.scheduleWithFixedDelay(new VmStatsCollector(), DEFAULT_INITIAL_DELAY, vmStatsInterval, TimeUnit.MILLISECONDS);
        } else {
            LOGGER.info("Skipping collect VM stats. The global parameter vm.stats.interval is set to 0 or less than 0.");
        }

        _executor.scheduleWithFixedDelay(new VmStatsCleaner(), DEFAULT_INITIAL_DELAY, 60000L, TimeUnit.MILLISECONDS);

        _executor.scheduleWithFixedDelay(new VolumeStatsCleaner(), DEFAULT_INITIAL_DELAY, 60000L, TimeUnit.MILLISECONDS);

        scheduleCollection(MANAGEMENT_SERVER_STATUS_COLLECTION_INTERVAL, new ManagementServerCollector(), 1L);
        scheduleCollection(DATABASE_SERVER_STATUS_COLLECTION_INTERVAL, new DbCollector(), 0L);

        if (storageStatsInterval > 0) {
            _executor.scheduleWithFixedDelay(new StorageCollector(), DEFAULT_INITIAL_DELAY, storageStatsInterval, TimeUnit.MILLISECONDS);
        }

        if (autoScaleStatsInterval > 0) {
            _executor.scheduleWithFixedDelay(new AutoScaleMonitor(), DEFAULT_INITIAL_DELAY, autoScaleStatsInterval * 1000L, TimeUnit.MILLISECONDS);
        }

        if (vmDiskStatsInterval.value() > 0) {
            if (vmDiskStatsInterval.value() < vmDiskStatsIntervalMin.value()) {
                LOGGER.debug("vm.disk.stats.interval - " + vmDiskStatsInterval.value() + " is smaller than vm.disk.stats.interval.min - " + vmDiskStatsIntervalMin.value()
                        + ", so use vm.disk.stats.interval.min");
                _executor.scheduleAtFixedRate(new VmDiskStatsTask(), vmDiskStatsIntervalMin.value(), vmDiskStatsIntervalMin.value(), TimeUnit.SECONDS);
            } else {
                _executor.scheduleAtFixedRate(new VmDiskStatsTask(), vmDiskStatsInterval.value(), vmDiskStatsInterval.value(), TimeUnit.SECONDS);
            }
        } else {
            LOGGER.debug("vm.disk.stats.interval - " + vmDiskStatsInterval.value() + " is 0 or less than 0, so not scheduling the vm disk stats thread");
        }

        if (vmNetworkStatsInterval.value() > 0) {
            if (vmNetworkStatsInterval.value() < vmNetworkStatsIntervalMin.value()) {
                LOGGER.debug("vm.network.stats.interval - " + vmNetworkStatsInterval.value() + " is smaller than vm.network.stats.interval.min - "
                        + vmNetworkStatsIntervalMin.value() + ", so use vm.network.stats.interval.min");
                _executor.scheduleAtFixedRate(new VmNetworkStatsTask(), vmNetworkStatsIntervalMin.value(), vmNetworkStatsIntervalMin.value(), TimeUnit.SECONDS);
            } else {
                _executor.scheduleAtFixedRate(new VmNetworkStatsTask(), vmNetworkStatsInterval.value(), vmNetworkStatsInterval.value(), TimeUnit.SECONDS);
            }
        } else {
            LOGGER.debug("vm.network.stats.interval - " + vmNetworkStatsInterval.value() + " is 0 or less than 0, so not scheduling the vm network stats thread");
        }

        if (volumeStatsInterval > 0) {
            _executor.scheduleAtFixedRate(new VolumeStatsTask(), DEFAULT_INITIAL_DELAY, volumeStatsInterval, TimeUnit.MILLISECONDS);
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
            LOGGER.warn("Usage stats job aggregation range is to small, using the minimum value of " + UsageUtils.USAGE_AGGREGATION_RANGE_MIN);
            _usageAggregationRange = UsageUtils.USAGE_AGGREGATION_RANGE_MIN;
        }

        long period = _usageAggregationRange * ONE_MINUTE_IN_MILLISCONDS;
        _diskStatsUpdateExecutor.scheduleAtFixedRate(new VmDiskStatsUpdaterTask(), (endDate - System.currentTimeMillis()), period, TimeUnit.MILLISECONDS);

        ManagementServerHostVO mgmtServerVo = managementServerHostDao.findByMsid(managementServerNodeId);
        if (mgmtServerVo != null) {
            msId = mgmtServerVo.getId();
        } else {
            LOGGER.warn(String.format("Cannot find management server with msid [%s]. "
                    + "Therefore, VM stats will be recorded with the management server MAC address converted as a long in the mgmt_server_id column.", managementServerNodeId));
        }
    }

    private void scheduleCollection(ConfigKey<Integer> statusCollectionInterval, AbstractStatsCollector collector, long delay) {
        if (statusCollectionInterval.value() > 0) {
            _executor.scheduleAtFixedRate(collector,
                    delay,
                    statusCollectionInterval.value(),
                    TimeUnit.SECONDS);
        } else {
                LOGGER.debug(String.format("%s - %d is 0 or less, so not scheduling the status collector thread",
                        statusCollectionInterval.key(), statusCollectionInterval.value()));
        }
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

    protected Map<Long, VMInstanceVO> getVmMapForStatsForHost(Host host) {
        List<VMInstanceVO> vms = _vmInstance.listByHostAndState(host.getId(), VirtualMachine.State.Running);
        boolean collectUserVMStatsOnly = Boolean.TRUE.equals(vmStatsCollectUserVMOnly.value());
        Map<Long, VMInstanceVO> vmMap = new HashMap<>();
        for (VMInstanceVO vm : vms) {
            if (collectUserVMStatsOnly && !VirtualMachine.Type.User.equals(vm.getType())) {
                continue;
            }
            vmMap.put(vm.getId(), vm);
        }
        return vmMap;
    }

    class HostCollector extends AbstractStatsCollector {
        @Override
        protected void runInContext() {
            try {
                LOGGER.debug("HostStatsCollector is running...");

                SearchCriteria<HostVO> sc = createSearchCriteriaForHostTypeRoutingStateUpAndNotInMaintenance();

                Map<Object, Object> metrics = new HashMap<>();
                List<HostVO> hosts = _hostDao.search(sc, null);

                for (HostVO host : hosts) {
                    HostStatsEntry hostStatsEntry = (HostStatsEntry) _resourceMgr.getHostStatistics(host.getId());
                    if (hostStatsEntry != null) {
                        hostStatsEntry.setHostVo(host);
                        metrics.put(hostStatsEntry.getHostId(), hostStatsEntry);
                        _hostStats.put(host.getId(), hostStatsEntry);
                    } else {
                        LOGGER.warn("The Host stats is null for host: " + host.getId());
                    }
                }

                if (externalStatsType == ExternalStatsProtocol.INFLUXDB) {
                    sendMetricsToInfluxdb(metrics);
                }

                updateGpuEnabledHostsDetails(hosts);
            } catch (Throwable t) {
                LOGGER.error("Error trying to retrieve host stats", t);
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
                if (!MapUtils.isEmpty(groupDetails)) {
                    _resourceMgr.updateGPUDetails(host.getId(), groupDetails);
                }
            }
        }

        @Override
        protected Point createInfluxDbPoint(Object metricsObject) {
            return createInfluxDbPointForHostMetrics(metricsObject);
        }
    }

     class DbCollector extends AbstractStatsCollector {
         List<Double> loadHistory = new ArrayList<>();
         DbCollector() {
             dbStats.put(loadAvarages, loadHistory);
         }
         @Override
         protected void runInContext() {
             LOGGER.debug(String.format("%s is running...", this.getClass().getSimpleName()));

             try {
                 long lastUptime = (dbStats.containsKey(uptime) ? (Long) dbStats.get(uptime) : 0);
                 long lastQueries = (dbStats.containsKey(queries) ? (Long) dbStats.get(queries) : 0);
                 getDynamicDataFromDB();
                 long interval = (Long) dbStats.get(uptime) - lastUptime;
                 long activity = (Long) dbStats.get(queries) - lastQueries;
                 loadHistory.add(0, Double.valueOf(activity / interval));
                 int maxsize = DATABASE_SERVER_LOAD_HISTORY_RETENTION_NUMBER.value();
                 while (loadHistory.size() > maxsize) {
                     loadHistory.remove(maxsize - 1);
                 }
             } catch (Throwable e) {
                 // pokemon catch to make sure the thread stays running
                 LOGGER.error("db statistics collection failed due to " + e.getLocalizedMessage());
                 if (LOGGER.isDebugEnabled()) {
                     LOGGER.debug("db statistics collection failed.", e);
                 }
             }
         }

         private void getDynamicDataFromDB() {
             Map<String, String> stats = DbUtil.getDbInfo("STATUS", queries, uptime);
             dbStats.put(collectionTime, new Date());
             dbStats.put(queries, (Long.valueOf(stats.get(queries))));
             dbStats.put(uptime, (Long.valueOf(stats.get(uptime))));
         }


         @Override
         protected Point createInfluxDbPoint(Object metricsObject) {
             return null;
         }
     }

    class ManagementServerCollector extends AbstractStatsCollector {
        @Override
        protected void runInContext() {
            LOGGER.debug(String.format("%s is running...", this.getClass().getSimpleName()));
            long msid = ManagementServerNode.getManagementServerId();
            ManagementServerHostVO mshost = null;
            ManagementServerHostStatsEntry hostStatsEntry = null;
            try {
                mshost = managementServerHostDao.findByMsid(msid);
                // get local data
                hostStatsEntry = getDataFrom(mshost);
                managementServerHostStats.put(mshost.getUuid(), hostStatsEntry);
                // send to other hosts
                clusterManager.publishStatus(gson.toJson(hostStatsEntry));
            } catch (Throwable t) {
                // pokemon catch to make sure the thread stays running
                LOGGER.error("Error trying to retrieve management server host statistics", t);
            }
            try {
                // send to DB
                storeStatus(hostStatsEntry, mshost);
            } catch (Throwable t) {
                // pokemon catch to make sure the thread stays running
                LOGGER.error("Error trying to store  management server host statistics", t);
            }
        }

        private void storeStatus(ManagementServerHostStatsEntry hostStatsEntry, ManagementServerHostVO mshost) {
            if (hostStatsEntry == null || mshost == null) {
                return;
            }
            ManagementServerStatusVO msStats = managementServerStatusDao.findByMsId(hostStatsEntry.getManagementServerHostUuid());
            if (msStats == null) {
                LOGGER.info(String.format("creating new status info record for host %s - %s",
                        mshost.getName(),
                        hostStatsEntry.getManagementServerHostUuid()));
                msStats = new ManagementServerStatusVO();
                msStats.setMsId(hostStatsEntry.getManagementServerHostUuid());
            }
            msStats.setOsDistribution(hostStatsEntry.getOsDistribution()); // for now just the bunch details come later
            msStats.setJavaName(hostStatsEntry.getJvmVendor());
            msStats.setJavaVersion(hostStatsEntry.getJvmVersion());
            Date startTime = new Date(hostStatsEntry.getJvmStartTime());
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(String.format("reporting starttime %s", startTime));
            }
            msStats.setLastJvmStart(startTime);
            msStats.setLastSystemBoot(hostStatsEntry.getSystemBootTime());
            msStats.setUpdated(new Date());
            managementServerStatusDao.persist(msStats);
        }

        @NotNull
        private ManagementServerHostStatsEntry getDataFrom(ManagementServerHostVO mshost) {
            ManagementServerHostStatsEntry newEntry = new ManagementServerHostStatsEntry();
            LOGGER.trace("Metrics collection start...");
            newEntry.setManagementServerHostId(mshost.getId());
            newEntry.setManagementServerHostUuid(mshost.getUuid());
            newEntry.setDbLocal(isDbLocal());
            newEntry.setUsageLocal(isUsageLocal());
            retrieveSession(newEntry);
            getJvmDimensions(newEntry);
            LOGGER.trace("Metrics collection extra...");
            getRuntimeData(newEntry);
            getMemoryData(newEntry);
            // newEntry must now include a pid!
            getProcFileSystemData(newEntry);
            // proc memory data has precedence over mbean memory data
            getCpuData(newEntry);
            getFileSystemData(newEntry);
            getDataBaseStatistics(newEntry, mshost.getMsid());
            gatherAllMetrics(newEntry);
            LOGGER.trace("Metrics collection end!");
            return newEntry;
        }

        private void retrieveSession(ManagementServerHostStatsEntry newEntry) {
            long sessions = ApiSessionListener.getSessionCount();
            newEntry.setSessions(sessions);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(String.format("Sessions found in Api %d vs context %d", sessions,ApiSessionListener.getNumberOfSessions()));
            } else {
                LOGGER.debug("Sessions active: " + sessions);
            }
        }

        private void getDataBaseStatistics(ManagementServerHostStatsEntry newEntry, long msid) {
            int count = _hostDao.countByMs(msid);
            newEntry.setAgentCount(count);
        }

        private void getMemoryData(@NotNull ManagementServerHostStatsEntry newEntry) {
            MemoryMXBean mxBean = ManagementFactory.getMemoryMXBean();
            newEntry.setTotalInit(mxBean.getHeapMemoryUsage().getInit() + mxBean.getNonHeapMemoryUsage().getInit());
            newEntry.setTotalUsed(mxBean.getHeapMemoryUsage().getUsed() + mxBean.getNonHeapMemoryUsage().getUsed());
            newEntry.setMaxJvmMemoryBytes(mxBean.getHeapMemoryUsage().getMax() + mxBean.getNonHeapMemoryUsage().getMax());
            newEntry.setTotalCommitted(mxBean.getHeapMemoryUsage().getCommitted() + mxBean.getNonHeapMemoryUsage().getCommitted());
        }

        private void getCpuData(@NotNull ManagementServerHostStatsEntry newEntry) {
            java.lang.management.OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
            newEntry.setAvailableProcessors(bean.getAvailableProcessors());
            newEntry.setLoadAverage(bean.getSystemLoadAverage());
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(String.format(
                        "Metrics processors - %d , loadavg - %f ",
                        newEntry.getAvailableProcessors(),
                        newEntry.getLoadAverage()));
            }
            if (bean instanceof OperatingSystemMXBean) {
                OperatingSystemMXBean mxBean = (OperatingSystemMXBean) bean;
                // if we got these from /proc, skip the bean
                if (newEntry.getSystemMemoryTotal() == 0) {
                    newEntry.setSystemMemoryTotal(mxBean.getTotalPhysicalMemorySize());
                }
                if (newEntry.getSystemMemoryFree() == 0) {
                    newEntry.setSystemMemoryFree(mxBean.getFreePhysicalMemorySize());
                }
                if (newEntry.getSystemMemoryUsed() <= 0) {
                    newEntry.setSystemMemoryUsed(mxBean.getCommittedVirtualMemorySize());
                }
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(String.format("data from 'OperatingSystemMXBean': total mem: %d, free mem: %d, used mem: %d",
                            newEntry.getSystemMemoryTotal(),
                            newEntry.getSystemMemoryFree(),
                            newEntry.getSystemMemoryUsed()));
                }
            }
        }

        private void getRuntimeData(@NotNull ManagementServerHostStatsEntry newEntry) {
            final RuntimeMXBean mxBean = ManagementFactory.getRuntimeMXBean();
            newEntry.setJvmUptime(mxBean.getUptime());
            newEntry.setJvmStartTime(mxBean.getStartTime());
            newEntry.setProcessId(mxBean.getPid());
            newEntry.setJvmName(mxBean.getName());
            newEntry.setJvmVendor(mxBean.getVmVendor());
            newEntry.setJvmVersion(mxBean.getVmVersion());
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(String.format(
                        "Metrics uptime - %d , starttime - %d",
                        newEntry.getJvmUptime(),
                        newEntry.getJvmStartTime()));
            }
        }

        private void getJvmDimensions(@NotNull ManagementServerHostStatsEntry newEntry) {
            Runtime runtime = Runtime.getRuntime();
            newEntry.setTotalJvmMemoryBytes(runtime.totalMemory());
            newEntry.setFreeJvmMemoryBytes(runtime.freeMemory());
            newEntry.setMaxJvmMemoryBytes(runtime.maxMemory());
            //long maxMem = runtime.maxMemory();
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(String.format(
                        "Metrics proc - %d , maxMem - %d , totalMemory - %d , freeMemory - %f ",
                        newEntry.getAvailableProcessors(),
                        newEntry.getMaxJvmMemoryBytes(),
                        newEntry.getTotalJvmMemoryBytes(),
                        newEntry.getFreeJvmMemoryBytes()));
            }
        }

        /**
         * As for data from outside the JVM, we only rely on /proc/ contained data.
         *
         * @param newEntry item to add the information to
         */
        private void getProcFileSystemData(@NotNull ManagementServerHostStatsEntry newEntry) {
            // this should be taken from ("cat /proc/version"), not sure how standard this /etc entry is
            String OS = Script.runSimpleBashScript("cat /etc/os-release | grep PRETTY_NAME | cut -f2 -d '=' | tr -d '\"'");
            newEntry.setOsDistribution(OS);
            String kernel = Script.runSimpleBashScript("uname -r");
            newEntry.setKernelVersion(kernel);
            // if we got these from the bean, skip
            if (newEntry.getSystemMemoryTotal() == 0) {
                String mem = Script.runSimpleBashScript("cat /proc/meminfo | grep MemTotal | cut -f 2 -d ':' | tr -d 'a-zA-z '").trim();
                newEntry.setSystemMemoryTotal(Long.parseLong(mem) * ByteScaleUtils.KiB);
                LOGGER.info(String.format("system memory from /proc: %d", newEntry.getSystemMemoryTotal()));
            }
            if (newEntry.getSystemMemoryFree() == 0) {
                String free = Script.runSimpleBashScript("cat /proc/meminfo | grep MemFree | cut -f 2 -d ':' | tr -d 'a-zA-z '").trim();
                newEntry.setSystemMemoryFree(Long.parseLong(free) * ByteScaleUtils.KiB);
                LOGGER.info(String.format("free memory from /proc: %d", newEntry.getSystemMemoryFree()));
            }
            if (newEntry.getSystemMemoryUsed() <= 0) {
                String used = Script.runSimpleBashScript(String.format("ps -o rss= %d", newEntry.getPid()));
                newEntry.setSystemMemoryUsed(Long.parseLong(used));
                LOGGER.info(String.format("used memory from /proc: %d", newEntry.getSystemMemoryUsed()));
            }
            try {
                String bootTime = Script.runSimpleBashScript("uptime -s");
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.ENGLISH);
                Date date = formatter.parse(bootTime);
                newEntry.setSystemBootTime(date);
            } catch (ParseException e) {
                LOGGER.error("can not retrieve system uptime");
            }
            String maxuse = Script.runSimpleBashScript(String.format("ps -o vsz= %d", newEntry.getPid()));
            newEntry.setSystemMemoryVirtualSize(Long.parseLong(maxuse) * 1024);

            newEntry.setSystemTotalCpuCycles(getSystemCpuCyclesTotal());
            newEntry.setSystemLoadAverages(getCpuLoads());

            newEntry.setSystemCyclesUsage(getSystemCpuUsage());
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(
                        String.format("cpu\ncapacities: %f\n     loads: %s ; %s ; %s\n     stats: %d ; %d ; %d",
                                newEntry.getSystemTotalCpuCycles(),
                                newEntry.getSystemLoadAverages()[0], newEntry.getSystemLoadAverages()[1], newEntry.getSystemLoadAverages()[2],
                                newEntry.getSystemCyclesUsage()[0], newEntry.getSystemCyclesUsage()[1], newEntry.getSystemCyclesUsage()[2]
                        )
                );
            }
        }

        @NotNull
        private double[] getCpuLoads() {
            String[] cpuloadString = Script.runSimpleBashScript("cat /proc/loadavg").split(" ");
            double[] cpuloads = {Double.parseDouble(cpuloadString[0]), Double.parseDouble(cpuloadString[1]), Double.parseDouble(cpuloadString[2])};
            return cpuloads;
        }

        private long [] getSystemCpuUsage() {
            String[] cpustats = Script.runSimpleBashScript("cat /proc/stat | grep \"cpu \" | tr -d \"cpu\"").trim().split(" ");
            long [] cycleUsage = {Long.parseLong(cpustats[0]) + Long.parseLong(cpustats[1]), Long.parseLong(cpustats[2]), Long.parseLong(cpustats[3])};
            return cycleUsage;
        }

        private double getSystemCpuCyclesTotal() {
            String cpucaps = Script.runSimpleBashScript("cat /proc/cpuinfo | grep \"cpu MHz\" | grep \"cpu MHz\" | cut -f 2 -d : | tr -d ' '| tr '\\n' \" \"");
            double totalcpucap = 0;
            for (String cpucap : cpucaps.split(" ")) {
                totalcpucap += Double.parseDouble(cpucap);
            }
            return totalcpucap;
        }

        private void getFileSystemData(@NotNull ManagementServerHostStatsEntry newEntry) {
            Set<String> logFileNames = LogUtils.getLogFileNames();
            StringBuilder logInfoBuilder = new StringBuilder();
            for (String fileName : logFileNames) {
                String du = Script.runSimpleBashScript(String.format("du -sh %s | cut -f '1'", fileName));
                String df = Script.runSimpleBashScript(String.format("df -h %s | grep -v Filesystem | awk '{print \"on disk \" $1 \" mounted on \" $6 \" (\" $5 \" full)\"}'", fileName));
                logInfoBuilder.append(fileName).append(" using: ").append(du).append('\n').append(df);
            }
            newEntry.setLogInfo(logInfoBuilder.toString());
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("log stats:\n" + newEntry.getLogInfo());
            }
        }

        private void gatherAllMetrics(ManagementServerHostStatsEntry metricsEntry) {
            Map<String, Object> metricDetails = new HashMap<>();
            for (String metricName : METRIC_REGISTRY.getGauges().keySet()) {
                Object value = getMetric(metricName);
                metricDetails.put(metricName, value);
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(String.format("Metrics collection '%s'=%s", metricName, value));
                }
                // gather what we need from this list
                extractDetailToField(metricsEntry, metricName, value);
            }
        }

        /**
         * store a value in the local fields of newEntry
         *
         * @param metricsEntry the stats info we need to communicate
         * @param metricName the detail to extract
         * @param value ;)
         */
        private void extractDetailToField(ManagementServerHostStatsEntry metricsEntry, String metricName, Object value) {
            switch (metricName) {
                case "memoryheap.used":
                    metricsEntry.setHeapMemoryUsed((Long) value);
                    break;
                case "memoryheap.max":
                    metricsEntry.setHeapMemoryTotal((Long) value);
                    break;
                case "threadsblocked.count":
                    metricsEntry.setThreadsBlockedCount((Integer) value);
                    break;
                case "threadscount":
                    metricsEntry.setThreadsTotalCount((Integer) value);
                    break;
                case "threadsdaemon.count":
                    metricsEntry.setThreadsDaemonCount((Integer) value);
                    break;
                case "threadsrunnable.count":
                    metricsEntry.setThreadsRunnableCount((Integer) value);
                    break;
                case "threadsterminated.count":
                    metricsEntry.setThreadsTerminatedCount((Integer) value);
                    break;
                case "threadswaiting.count":
                    metricsEntry.setThreadsWaitingCount((Integer) value);
                    break;
                case "threadsdeadlocks":
                case "threadsnew.count":
                case "threadstimed_waiting.count":
                default:
                    LOGGER.debug(String.format("not storing detail %s, %s", metricName, value));
                    /*
                     * 'buffers.direct.capacity'=8192 type=Long
                     * 'buffers.direct.count'=1 type=Long
                     * 'buffers.direct.used'=8192 type=Long
                     * 'buffers.mapped.capacity'=0 type=Long
                     * 'buffers.mapped.count'=0 type=Long
                     * 'buffers.mapped.used'=0 type=Long
                     * 'gc.G1-Old-Generation.count'=0 type=Long
                     * 'gc.G1-Old-Generation.time'=0 type=Long
                     * 'gc.G1-Young-Generation.count'=36 type=Long
                     * 'gc.G1-Young-Generation.time'=678 type=Long
                     * 'jvm.name'=532601@matah type=String
                     * 'jvm.uptime'=272482 type=Long
                     * 'jvm.vendor'=Red Hat, Inc. OpenJDK 64-Bit Server VM 11.0.12+7 (11) type=String
                     * 'memory.heap.committed'=1200619520 type=Long
                     * 'memory.heap.init'=522190848 type=Long
                     *+ 'memoryheap.max'=4294967296 type=Long
                     * 'memory.heap.usage'=0.06405723094940186 type=Double
                     *+ 'memoryheap.used'=275123712 type=Long
                     * 'memory.non-heap.committed'=217051136 type=Long
                     * 'memory.non-heap.init'=7667712 type=Long
                     * 'memory.non-heap.max'=-1 type=Long
                     * 'memory.non-heap.usage'=-2.11503936E8 type=Double
                     * 'memory.non-heap.used'=211503936 type=Long
                     * 'memory.pools.CodeHeap-'non-nmethods'.usage'=0.3137061403508772 type=Double
                     * 'memory.pools.CodeHeap-'non-profiled-nmethods'.usage'=0.16057488836310319 type=Double
                     * 'memory.pools.CodeHeap-'profiled-nmethods'.usage'=0.3391885643349885 type=Double
                     * 'memory.pools.Compressed-Class-Space.usage'=0.012650594115257263 type=Double
                     * 'memory.pools.G1-Eden-Space.usage'=0.005822416302765648 type=Double
                     * 'memory.pools.G1-Old-Gen.usage'=0.054535746574401855 type=Double
                     * 'memory.pools.G1-Survivor-Space.usage'=1.0 type=Double
                     * 'memory.pools.Metaspace.usage'=0.9765298966718151 type=Double
                     * 'memory.total.committed'=1417670656 type=Long
                     * 'memory.total.init'=529858560 type=Long
                     * 'memory.total.max'=4294967295 type=Long
                     * 'memory.total.used'=486627648 type=Long
                     *+ 'threadsblocked.count'=1 type=Integer
                     *+ 'threadscount'=439 type=Integer
                     *+ 'threadsdaemon.count'=12 type=Integer
                     * 'threadsdeadlocks'=[] type=EmptySet
                     * 'threads.new.count'=0 type=Integer
                     *+ 'threadsrunnable.count'=5 type=Integer
                     *+ 'threadsterminated.count'=0 type=Integer
                     * 'threads.timed_waiting.count'=52 type=Integer
                     *+ 'threadswaiting.count'=381 type=Integer
                     */
                    break;
            }
        }

        private Object getMetric(String metricName) {
            return METRIC_REGISTRY.getGauges().get(metricName).getValue();
        }

        @Override
        protected Point createInfluxDbPoint(Object metricsObject) {
            return null;
        }

    }

    /**
     * @return true if there is a usage server installed locally.
     */
    protected boolean isUsageLocal() {
        boolean local = false;
        String usageInstall = Script.runSimpleBashScript("systemctl status cloudstack-usage | grep \"  Loaded:\"");
        LOGGER.debug(String.format("usage install: %s", usageInstall));

        if (StringUtils.isNotBlank(usageInstall)) {
            local = usageInstall.contains("enabled");
        }
        return local;
    }

    /**
     * @return true if the DB endpoint is local to this server
     */
    protected boolean isDbLocal() {
        Properties p = getDbProperties();
        String configeredHost = p.getProperty("db.cloud.host");
        String localHost = p.getProperty("cluster.node.IP");
        // see if these resolve to the same
        if ("localhost".equals(configeredHost)) return true;
        if ("127.0.0.1".equals(configeredHost)) return true;
        if ("::1".equals(configeredHost)) return true;
        if (StringUtils.isNotBlank(configeredHost) && StringUtils.isNotBlank(localHost) && configeredHost.equals(localHost)) return true;
        return false;
    }

    protected Properties getDbProperties() {
        return DbProperties.getDbProperties();
    }

    protected class ManagementServerStatusAdministrator implements ClusterManager.StatusAdministrator, ClusterManagerListener {
        @Override
        public String newStatus(ClusterServicePdu pdu) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("StatusUpdate from %s, json: %s", pdu.getSourcePeer(), pdu.getJsonPackage()));
            }

            ManagementServerHostStatsEntry hostStatsEntry = null;
            try {
                hostStatsEntry = gson.fromJson(pdu.getJsonPackage(),new TypeToken<ManagementServerHostStatsEntry>(){}.getType());
                managementServerHostStats.put(hostStatsEntry.getManagementServerHostUuid(), hostStatsEntry);
            } catch (JsonParseException e) {
                LOGGER.error("Exception in decoding of other MS hosts status from : " + pdu.getSourcePeer());
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Exception in decoding of other MS hosts status: ", e);
                }
            }
            return null;
        }

        @Override
        public void onManagementNodeJoined(List<? extends ManagementServerHost> nodeList, long selfNodeId) {
            // do nothing, but wait for the status to come through
        }

        @Override
        public void onManagementNodeLeft(List<? extends ManagementServerHost> nodeList, long selfNodeId) {
            // remove the status for those ones
            for (ManagementServerHost node : nodeList) {
                LOGGER.info(String.format("node %s (%s) at %s (%od) is reported to have left the cluster, invalidating status.",node.getName(), node.getUuid(), node.getServiceIP(), node.getMsid()));
                managementServerHostStats.remove(node.getUuid());
            }
        }

        @Override
        public void onManagementNodeIsolated() {
            LOGGER.error(String.format("This management server is reported to be isolated (msid %d", mgmtSrvrId));
            // not sure if anything should be done now.
        }
    }

    class VmStatsCollector extends AbstractStatsCollector {
        @Override
        protected void runInContext() {
            try {
                LOGGER.trace("VmStatsCollector is running...");

                SearchCriteria<HostVO> sc = createSearchCriteriaForHostTypeRoutingStateUpAndNotInMaintenance();
                List<HostVO> hosts = _hostDao.search(sc, null);

                Map<Object, Object> metrics = new HashMap<>();

                for (HostVO host : hosts) {
                    Date timestamp = new Date();
                    Map<Long, VMInstanceVO> vmMap = getVmMapForStatsForHost(host);
                    try {
                        Map<Long, ? extends VmStats> vmStatsById = virtualMachineManager.getVirtualMachineStatistics(host.getId(), host.getName(), vmMap);

                        if (vmStatsById != null) {
                            Set<Long> vmIdSet = vmStatsById.keySet();
                            for (Long vmId : vmIdSet) {
                                VmStatsEntry statsForCurrentIteration = (VmStatsEntry)vmStatsById.get(vmId);
                                statsForCurrentIteration.setVmId(vmId);
                                VMInstanceVO vm = vmMap.get(vmId);
                                statsForCurrentIteration.setVmUuid(vm.getUuid());

                                persistVirtualMachineStats(statsForCurrentIteration, timestamp);

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
                        LOGGER.debug("Failed to get VM stats for host with ID: " + host.getId());
                        continue;
                    }
                }

            } catch (Throwable t) {
                LOGGER.error("Error trying to retrieve VM stats", t);
            }
        }

        @Override
        protected Point createInfluxDbPoint(Object metricsObject) {
            return createInfluxDbPointForVmMetrics(metricsObject);
        }
    }

    /**
     * <p>Previously, the VM stats cleanup process was triggered during the data collection process.
     * So, when data collection was disabled, the cleaning process was also disabled.</p>
     *
     * <p>With the introduction of persistence of VM stats, as well as the provision of historical data,
     * we created this class to allow that both the collection process and the data cleaning process
     * can be enabled/disabled independently.</p>
     */
    class VmStatsCleaner extends ManagedContextRunnable{
        protected void runInContext() {
            cleanUpVirtualMachineStats();
        }
    }

    class VolumeStatsCleaner extends ManagedContextRunnable{
        protected void runInContext() {
            cleanUpVolumeStats();
        }
    }

    /**
     * Gets the latest or the accumulation of the stats collected from a given VM.
     *
     * @param vmId the specific VM.
     * @param accumulate whether or not the stats data should be accumulated.
     * @return the latest or the accumulation of the stats for the specified VM.
     */
    public VmStats getVmStats(long vmId, Boolean accumulate) {
        List<VmStatsVO> vmStatsVOList = vmStatsDao.findByVmIdOrderByTimestampDesc(vmId);

        if (CollectionUtils.isEmpty(vmStatsVOList)) {
            return null;
        }

        if (accumulate != null) {
            return getLatestOrAccumulatedVmMetricsStats(vmStatsVOList, accumulate.booleanValue());
        }
        return getLatestOrAccumulatedVmMetricsStats(vmStatsVOList, BooleanUtils.toBoolean(vmStatsIncrementMetrics.value()));
    }

    /**
     * Gets the latest or the accumulation of a list of VM stats.<br>
     * It extracts the stats data from the VmStatsVO.
     *
     * @param vmStatsVOList the list of VM stats.
     * @param accumulate {@code true} if the data should be accumulated, {@code false} otherwise.
     * @return the {@link VmStatsEntry} containing the latest or the accumulated stats.
     */
    protected VmStatsEntry getLatestOrAccumulatedVmMetricsStats (List<VmStatsVO> vmStatsVOList, boolean accumulate) {
        if (accumulate) {
            return accumulateVmMetricsStats(vmStatsVOList);
        }
        return gson.fromJson(vmStatsVOList.get(0).getVmStatsData(), VmStatsEntry.class);
    }

    /**
     * Accumulates (I/O) stats for a given VM.
     *
     * @param vmStatsVOList the list of stats for a given VM.
     * @return the {@link VmStatsEntry} containing the accumulated (I/O) stats.
     */
    protected VmStatsEntry accumulateVmMetricsStats(List<VmStatsVO> vmStatsVOList) {
        VmStatsEntry latestVmStats = gson.fromJson(vmStatsVOList.remove(0).getVmStatsData(), VmStatsEntry.class);

        VmStatsEntry vmStatsEntry = new VmStatsEntry();
        vmStatsEntry.setEntityType(latestVmStats.getEntityType());
        vmStatsEntry.setVmId(latestVmStats.getVmId());
        vmStatsEntry.setCPUUtilization(latestVmStats.getCPUUtilization());
        vmStatsEntry.setNumCPUs(latestVmStats.getNumCPUs());
        vmStatsEntry.setMemoryKBs(latestVmStats.getMemoryKBs());
        vmStatsEntry.setIntFreeMemoryKBs(latestVmStats.getIntFreeMemoryKBs());
        vmStatsEntry.setTargetMemoryKBs(latestVmStats.getTargetMemoryKBs());
        vmStatsEntry.setNetworkReadKBs(latestVmStats.getNetworkReadKBs());
        vmStatsEntry.setNetworkWriteKBs(latestVmStats.getNetworkWriteKBs());
        vmStatsEntry.setDiskWriteKBs(latestVmStats.getDiskWriteKBs());
        vmStatsEntry.setDiskReadIOs(latestVmStats.getDiskReadIOs());
        vmStatsEntry.setDiskWriteIOs(latestVmStats.getDiskWriteIOs());
        vmStatsEntry.setDiskReadKBs(latestVmStats.getDiskReadKBs());

        for (VmStatsVO vmStatsVO : vmStatsVOList) {
            VmStatsEntry currentVmStatsEntry = gson.fromJson(vmStatsVO.getVmStatsData(), VmStatsEntry.class);

            vmStatsEntry.setNetworkReadKBs(vmStatsEntry.getNetworkReadKBs() + currentVmStatsEntry.getNetworkReadKBs());
            vmStatsEntry.setNetworkWriteKBs(vmStatsEntry.getNetworkWriteKBs() + currentVmStatsEntry.getNetworkWriteKBs());
            vmStatsEntry.setDiskReadKBs(vmStatsEntry.getDiskReadKBs() + currentVmStatsEntry.getDiskReadKBs());
            vmStatsEntry.setDiskWriteKBs(vmStatsEntry.getDiskWriteKBs() + currentVmStatsEntry.getDiskWriteKBs());
            vmStatsEntry.setDiskReadIOs(vmStatsEntry.getDiskReadIOs() + currentVmStatsEntry.getDiskReadIOs());
            vmStatsEntry.setDiskWriteIOs(vmStatsEntry.getDiskWriteIOs() + currentVmStatsEntry.getDiskWriteIOs());
        }
        return vmStatsEntry;
    }

    class VmDiskStatsUpdaterTask extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            GlobalLock scanLock = GlobalLock.getInternLock("vm.disk.stats");
            try {
                if (scanLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION)) {
                    //Check for ownership
                    //msHost in UP state with min id should run the job
                    ManagementServerHostVO msHost = managementServerHostDao.findOneInUpState(new Filter(ManagementServerHostVO.class, "id", true, 0L, 1L));
                    if (msHost == null || (msHost.getMsid() != mgmtSrvrId)) {
                        LOGGER.debug("Skipping aggregate disk stats update");
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
                                LOGGER.debug("Successfully updated aggregate vm disk stats");
                            }
                        });
                    } catch (Exception e) {
                        LOGGER.debug("Failed to update aggregate disk stats", e);
                    } finally {
                        scanLock.unlock();
                    }
                }
            } catch (Exception e) {
                LOGGER.debug("Exception while trying to acquire disk stats lock", e);
            } finally {
                scanLock.releaseRef();
            }
        }
    }

    private void logLessLatestStatDiscrepancy(String prefix, String hostName, String vmName, long reported, long stored, boolean toHumanReadable) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("%s that's less than the last one.  Assuming something went wrong and persisting it. Host: %s . VM: %s Reported: %s Stored: %s",
                    prefix, hostName, vmName, toHumanReadable ? toHumanReadableSize(reported) : reported, toHumanReadable ? toHumanReadableSize(stored) : stored));
        }
    }

    class VmDiskStatsTask extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            //Check for ownership
            //msHost in UP state with min id should run the job
            ManagementServerHostVO msHost = managementServerHostDao.findOneInUpState(new Filter(ManagementServerHostVO.class, "id", true, 0L, 1L));
            boolean persistVolumeStats = vmDiskStatsRetentionEnabled.value();
            if (msHost == null || (msHost.getMsid() != mgmtSrvrId)) {
                LOGGER.debug("Skipping collect vm disk stats from hosts");
                return;
            }
            // collect the vm disk statistics(total) from hypervisor. added by weizhou, 2013.03.
            LOGGER.debug("VmDiskStatsTask is running...");

            SearchCriteria<HostVO> sc = createSearchCriteriaForHostTypeRoutingStateUpAndNotInMaintenance();
            sc.addAnd("hypervisorType", SearchCriteria.Op.IN, HypervisorType.KVM, HypervisorType.VMware);
            List<HostVO> hosts = _hostDao.search(sc, null);

            for (HostVO host : hosts) {
                Date timestamp = new Date();
                try {
                    Transaction.execute(new TransactionCallbackNoReturn() {
                        @Override
                        public void doInTransactionWithoutResult(TransactionStatus status) {
                            Map<Long, VMInstanceVO> vmMap = getVmMapForStatsForHost(host);
                            HashMap<Long, List<? extends VmDiskStats>> vmDiskStatsById = virtualMachineManager.getVmDiskStatistics(host.getId(), host.getName(), vmMap);
                            if (vmDiskStatsById == null)
                                return;

                            Set<Long> vmIdSet = vmDiskStatsById.keySet();
                            for (Long vmId : vmIdSet) {
                                List<? extends VmDiskStats> vmDiskStats = vmDiskStatsById.get(vmId);
                                if (vmDiskStats == null)
                                    continue;
                                VMInstanceVO vm = vmMap.get(vmId);
                                for (VmDiskStats vmDiskStat : vmDiskStats) {
                                    VmDiskStatsEntry vmDiskStatEntry = (VmDiskStatsEntry)vmDiskStat;
                                    SearchCriteria<VolumeVO> sc_volume = _volsDao.createSearchCriteria();
                                    sc_volume.addAnd("path", SearchCriteria.Op.EQ, vmDiskStatEntry.getPath());
                                    List<VolumeVO> volumes = _volsDao.search(sc_volume, null);

                                    if (CollectionUtils.isEmpty(volumes))
                                        break;

                                    VolumeVO volume = volumes.get(0);
                                    VmDiskStatisticsVO previousVmDiskStats = _vmDiskStatsDao.findBy(vm.getAccountId(), vm.getDataCenterId(), vmId, volume.getId());
                                    VmDiskStatisticsVO vmDiskStat_lock = _vmDiskStatsDao.lock(vm.getAccountId(), vm.getDataCenterId(), vmId, volume.getId());

                                    if (persistVolumeStats) {
                                        persistVolumeStats(volume.getId(), vmDiskStatEntry, vm.getHypervisorType(), timestamp);
                                    }

                                    if (areAllDiskStatsZero(vmDiskStatEntry)) {
                                        LOGGER.debug("IO/bytes read and write are all 0. Not updating vm_disk_statistics");
                                        continue;
                                    }

                                    if (vmDiskStat_lock == null) {
                                        LOGGER.warn("unable to find vm disk stats from host for account: " + vm.getAccountId() + " with vmId: " + vm.getId()
                                                + " and volumeId:" + volume.getId());
                                        continue;
                                    }

                                    if (isCurrentVmDiskStatsDifferentFromPrevious(previousVmDiskStats, vmDiskStat_lock)) {
                                        LOGGER.debug("vm disk stats changed from the time GetVmDiskStatsCommand was sent. " + "Ignoring current answer. Host: " + host.getName()
                                                + " . VM: " + vmDiskStatEntry.getVmName() + " Read(Bytes): " + toHumanReadableSize(vmDiskStatEntry.getBytesRead()) + " write(Bytes): " + toHumanReadableSize(vmDiskStatEntry.getBytesWrite())
                                                + " Read(IO): " + toHumanReadableSize(vmDiskStatEntry.getIORead()) + " write(IO): " + toHumanReadableSize(vmDiskStatEntry.getIOWrite()));
                                        continue;
                                    }

                                    if (vmDiskStat_lock.getCurrentBytesRead() > vmDiskStatEntry.getBytesRead()) {
                                        logLessLatestStatDiscrepancy("Read # of bytes", host.getName(), vmDiskStatEntry.getVmName(), vmDiskStatEntry.getBytesRead(), vmDiskStat_lock.getCurrentBytesRead(), true);
                                        vmDiskStat_lock.setNetBytesRead(vmDiskStat_lock.getNetBytesRead() + vmDiskStat_lock.getCurrentBytesRead());
                                    }
                                    vmDiskStat_lock.setCurrentBytesRead(vmDiskStatEntry.getBytesRead());
                                    if (vmDiskStat_lock.getCurrentBytesWrite() > vmDiskStatEntry.getBytesWrite()) {
                                        logLessLatestStatDiscrepancy("Write # of bytes", host.getName(), vmDiskStatEntry.getVmName(), vmDiskStatEntry.getBytesWrite(), vmDiskStat_lock.getCurrentBytesWrite(), true);
                                        vmDiskStat_lock.setNetBytesWrite(vmDiskStat_lock.getNetBytesWrite() + vmDiskStat_lock.getCurrentBytesWrite());
                                    }
                                    vmDiskStat_lock.setCurrentBytesWrite(vmDiskStatEntry.getBytesWrite());
                                    if (vmDiskStat_lock.getCurrentIORead() > vmDiskStatEntry.getIORead()) {
                                        logLessLatestStatDiscrepancy("Read # of IO", host.getName(), vmDiskStatEntry.getVmName(), vmDiskStatEntry.getIORead(), vmDiskStat_lock.getCurrentIORead(), false);
                                        vmDiskStat_lock.setNetIORead(vmDiskStat_lock.getNetIORead() + vmDiskStat_lock.getCurrentIORead());
                                    }
                                    vmDiskStat_lock.setCurrentIORead(vmDiskStatEntry.getIORead());
                                    if (vmDiskStat_lock.getCurrentIOWrite() > vmDiskStatEntry.getIOWrite()) {
                                        logLessLatestStatDiscrepancy("Write # of IO", host.getName(), vmDiskStatEntry.getVmName(), vmDiskStatEntry.getIOWrite(), vmDiskStat_lock.getCurrentIOWrite(), false);
                                        vmDiskStat_lock.setNetIOWrite(vmDiskStat_lock.getNetIOWrite() + vmDiskStat_lock.getCurrentIOWrite());
                                    }
                                    vmDiskStat_lock.setCurrentIOWrite(vmDiskStatEntry.getIOWrite());

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
                    });
                } catch (Exception e) {
                    LOGGER.warn(String.format("Error while collecting vm disk stats from host %s : ", host.getName()), e);
                }
            }
        }
    }

    class VmNetworkStatsTask extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            //Check for ownership
            //msHost in UP state with min id should run the job
            ManagementServerHostVO msHost = managementServerHostDao.findOneInUpState(new Filter(ManagementServerHostVO.class, "id", true, 0L, 1L));
            if (msHost == null || (msHost.getMsid() != mgmtSrvrId)) {
                LOGGER.debug("Skipping collect vm network stats from hosts");
                return;
            }
            // collect the vm network statistics(total) from hypervisor
            LOGGER.debug("VmNetworkStatsTask is running...");

            SearchCriteria<HostVO> sc = createSearchCriteriaForHostTypeRoutingStateUpAndNotInMaintenance();
            List<HostVO> hosts = _hostDao.search(sc, null);

            for (HostVO host : hosts) {
                try {
                    Transaction.execute(new TransactionCallbackNoReturn() {
                        @Override
                        public void doInTransactionWithoutResult(TransactionStatus status) {
                            Map<Long, VMInstanceVO> vmMap = getVmMapForStatsForHost(host);
                            HashMap<Long, List<? extends VmNetworkStats>> vmNetworkStatsById = virtualMachineManager.getVmNetworkStatistics(host.getId(), host.getName(), vmMap);
                            if (vmNetworkStatsById == null)
                                return;

                            Set<Long> vmIdSet = vmNetworkStatsById.keySet();
                            for (Long vmId : vmIdSet) {
                                List<? extends VmNetworkStats> vmNetworkStats = vmNetworkStatsById.get(vmId);
                                if (CollectionUtils.isEmpty(vmNetworkStats))
                                    continue;
                                UserVmVO userVm = _userVmDao.findById(vmId);
                                if (userVm == null) {
                                    LOGGER.debug("Cannot find uservm with id: " + vmId + " , continue");
                                    continue;
                                }
                                LOGGER.debug("Now we are updating the user_statistics table for VM: " + userVm.getInstanceName()
                                        + " after collecting vm network statistics from host: " + host.getName());
                                for (VmNetworkStats vmNetworkStat : vmNetworkStats) {
                                    VmNetworkStatsEntry vmNetworkStatEntry = (VmNetworkStatsEntry)vmNetworkStat;
                                    SearchCriteria<NicVO> sc_nic = _nicDao.createSearchCriteria();
                                    sc_nic.addAnd("macAddress", SearchCriteria.Op.EQ, vmNetworkStatEntry.getMacAddress());
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

                                    if ((vmNetworkStatEntry.getBytesSent() == 0) && (vmNetworkStatEntry.getBytesReceived() == 0)) {
                                        LOGGER.debug("bytes sent and received are all 0. Not updating user_statistics");
                                        continue;
                                    }

                                    if (vmNetworkStat_lock == null) {
                                        LOGGER.warn("unable to find vm network stats from host for account: " + userVm.getAccountId() + " with vmId: " + userVm.getId()
                                                + " and nicId:" + nic.getId());
                                        continue;
                                    }

                                    if (previousvmNetworkStats != null && ((previousvmNetworkStats.getCurrentBytesSent() != vmNetworkStat_lock.getCurrentBytesSent())
                                            || (previousvmNetworkStats.getCurrentBytesReceived() != vmNetworkStat_lock.getCurrentBytesReceived()))) {
                                        LOGGER.debug("vm network stats changed from the time GetNmNetworkStatsCommand was sent. " + "Ignoring current answer. Host: "
                                                + host.getName() + " . VM: " + vmNetworkStatEntry.getVmName() + " Sent(Bytes): " + vmNetworkStatEntry.getBytesSent() + " Received(Bytes): "
                                                + vmNetworkStatEntry.getBytesReceived());
                                        continue;
                                    }

                                    if (vmNetworkStat_lock.getCurrentBytesSent() > vmNetworkStatEntry.getBytesSent()) {
                                        logLessLatestStatDiscrepancy("Sent # of bytes", host.getName(), vmNetworkStatEntry.getVmName(), vmNetworkStatEntry.getBytesSent(), vmNetworkStat_lock.getCurrentBytesSent(), true);
                                        vmNetworkStat_lock.setNetBytesSent(vmNetworkStat_lock.getNetBytesSent() + vmNetworkStat_lock.getCurrentBytesSent());
                                    }
                                    vmNetworkStat_lock.setCurrentBytesSent(vmNetworkStatEntry.getBytesSent());

                                    if (vmNetworkStat_lock.getCurrentBytesReceived() > vmNetworkStatEntry.getBytesReceived()) {
                                        logLessLatestStatDiscrepancy("Received # of bytes", host.getName(), vmNetworkStatEntry.getVmName(), vmNetworkStatEntry.getBytesReceived(), vmNetworkStat_lock.getCurrentBytesReceived(), true);
                                        vmNetworkStat_lock.setNetBytesReceived(vmNetworkStat_lock.getNetBytesReceived() + vmNetworkStat_lock.getCurrentBytesReceived());
                                    }
                                    vmNetworkStat_lock.setCurrentBytesReceived(vmNetworkStatEntry.getBytesReceived());

                                    if (!_dailyOrHourly) {
                                        //update agg bytes
                                        vmNetworkStat_lock.setAggBytesReceived(vmNetworkStat_lock.getNetBytesReceived() + vmNetworkStat_lock.getCurrentBytesReceived());
                                        vmNetworkStat_lock.setAggBytesSent(vmNetworkStat_lock.getNetBytesSent() + vmNetworkStat_lock.getCurrentBytesSent());
                                    }

                                    _userStatsDao.update(vmNetworkStat_lock.getId(), vmNetworkStat_lock);
                                }
                            }
                        }
                    });
                } catch (Exception e) {
                    LOGGER.warn(String.format("Error while collecting vm network stats from host %s : ", host.getName()), e);
                }
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
                    for (VolumeVO volume : volumes) {
                        if (volume.getFormat() != ImageFormat.QCOW2 && volume.getFormat() != ImageFormat.VHD && volume.getFormat() != ImageFormat.OVA && (volume.getFormat() != ImageFormat.RAW || pool.getPoolType() != Storage.StoragePoolType.PowerFlex)) {
                            LOGGER.warn("Volume stats not implemented for this format type " + volume.getFormat());
                            break;
                        }
                    }
                    try {
                        Map<String, VolumeStatsEntry> volumeStatsByUuid;
                        if (pool.getScope() == ScopeType.ZONE) {
                            volumeStatsByUuid = new HashMap<>();
                            for (final Cluster cluster : _clusterDao.listClustersByDcId(pool.getDataCenterId())) {
                                final Map<String, VolumeStatsEntry> volumeStatsForCluster = _userVmMgr.getVolumeStatistics(cluster.getId(), pool.getUuid(), pool.getPoolType(), StatsTimeout.value());
                                if (volumeStatsForCluster != null) {
                                    volumeStatsByUuid.putAll(volumeStatsForCluster);
                                }
                            }
                        } else {
                            volumeStatsByUuid = _userVmMgr.getVolumeStatistics(pool.getClusterId(), pool.getUuid(), pool.getPoolType(), StatsTimeout.value());
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
                        LOGGER.warn("Failed to get volume stats for cluster with ID: " + pool.getClusterId(), e);
                        continue;
                    }
                }
            } catch (Throwable t) {
                LOGGER.error("Error trying to retrieve volume stats", t);
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
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("StorageCollector is running...");
                }

                List<DataStore> stores = _dataStoreMgr.listImageStores();
                ConcurrentHashMap<Long, StorageStats> storageStats = new ConcurrentHashMap<Long, StorageStats>();
                for (DataStore store : stores) {
                    if (store.getUri() == null) {
                        continue;
                    }

                    String nfsVersion = imageStoreDetailsUtil.getNfsVersion(store.getId());
                    GetStorageStatsCommand command = new GetStorageStatsCommand(store.getTO(), nfsVersion);
                    EndPoint ssAhost = _epSelector.select(store);
                    if (ssAhost == null) {
                        LOGGER.debug("There is no secondary storage VM for secondary storage host " + store.getName());
                        continue;
                    }
                    long storeId = store.getId();
                    Answer answer = ssAhost.sendMessage(command);
                    if (answer != null && answer.getResult()) {
                        storageStats.put(storeId, (StorageStats)answer);
                        LOGGER.trace("HostId: " + storeId + " Used: " + toHumanReadableSize(((StorageStats)answer).getByteUsed()) + " Total Available: " + toHumanReadableSize(((StorageStats)answer).getCapacityBytes()));
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

                            boolean poolNeedsUpdating = false;
                            // Seems like we have dynamically updated the pool size since the prev. size and the current do not match
                            if (_storagePoolStats.get(poolId) != null && _storagePoolStats.get(poolId).getCapacityBytes() != ((StorageStats)answer).getCapacityBytes()) {
                                if (((StorageStats)answer).getCapacityBytes() > 0) {
                                    pool.setCapacityBytes(((StorageStats)answer).getCapacityBytes());
                                    poolNeedsUpdating = true;
                                } else {
                                    LOGGER.warn("Not setting capacity bytes, received " + ((StorageStats)answer).getCapacityBytes()  + " capacity for pool ID " + poolId);
                                }
                            }
                            if (pool.getUsedBytes() != ((StorageStats)answer).getByteUsed() && (pool.getStorageProviderName().equalsIgnoreCase(DataStoreProvider.DEFAULT_PRIMARY) || _storageManager.canPoolProvideStorageStats(pool))) {
                                pool.setUsedBytes(((StorageStats) answer).getByteUsed());
                                poolNeedsUpdating = true;
                            }
                            if (poolNeedsUpdating) {
                                pool.setUpdateTime(new Date());
                                _storagePoolDao.update(pool.getId(), pool);
                            }
                        }
                    } catch (StorageUnavailableException e) {
                        LOGGER.info("Unable to reach " + pool, e);
                    } catch (Exception e) {
                        LOGGER.warn("Unable to get stats for " + pool, e);
                    }
                }
                _storagePoolStats = storagePoolStats;
            } catch (Throwable t) {
                LOGGER.error("Error trying to retrieve storage stats", t);
            }
        }
    }

    class AutoScaleMonitor extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            try {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("AutoScaling Monitor is running...");
                }
                //msHost in UP state with min id should run the job
                ManagementServerHostVO msHost = managementServerHostDao.findOneInUpState(new Filter(ManagementServerHostVO.class, "id", true, 0L, 1L));
                if (msHost == null || (msHost.getMsid() != mgmtSrvrId)) {
                    LOGGER.debug("Skipping AutoScaling Monitor");
                    return;
                }

                _asManager.checkAllAutoScaleVmGroups();

            } catch (Throwable t) {
                LOGGER.error("Error trying to monitor autoscaling", t);
            }

        }

    }

    /**
     * This class allows to writing metrics in InfluxDB for the table that matches the Collector extending it.
     * Thus, VmStatsCollector and HostCollector can use same method to write on different measures (host_stats or vm_stats table).
     */
    abstract class  AbstractStatsCollector extends ManagedContextRunnable {
        /**
         * Sends metrics to influxdb host. This method supports both VM and Host metrics
         */
        protected void sendMetricsToInfluxdb(Map<Object, Object> metrics) {
            InfluxDB influxDbConnection = createInfluxDbConnection();

            try {
                Pong response = influxDbConnection.ping();
                if (response.getVersion().equalsIgnoreCase("unknown")) {
                    throw new CloudRuntimeException(String.format("Cannot ping influxdb host %s:%s.", externalStatsHost, externalStatsPort));
                }

                Collection<Object> metricsObjects = metrics.values();
                List<Point> points = new ArrayList<>();

                LOGGER.debug(String.format("Sending stats to %s host %s:%s", externalStatsType, externalStatsHost, externalStatsPort));

                for (Object metricsObject : metricsObjects) {
                    Point vmPoint = createInfluxDbPoint(metricsObject);
                    points.add(vmPoint);
                }
                writeBatches(influxDbConnection, databaseName, points);
            } finally {
                influxDbConnection.close();
            }
        }

        /**
         * Creates a InfluxDB point for the given stats collector (VmStatsCollector, or HostCollector).
         */
        protected abstract Point createInfluxDbPoint(Object metricsObject);
    }

    public boolean imageStoreHasEnoughCapacity(DataStore imageStore) {
        if (!_storageStats.keySet().contains(imageStore.getId())) { // Stats not available for this store yet, can be a new store. Better to assume it has enough capacity?
            return true;
        }

        long imageStoreId = imageStore.getId();
        StorageStats imageStoreStats = _storageStats.get(imageStoreId);

        if (imageStoreStats == null) {
            LOGGER.debug(String.format("Stats for image store [%s] not found.", imageStoreId));
            return false;
        }

        double totalCapacity = imageStoreStats.getCapacityBytes();
        double usedCapacity = imageStoreStats.getByteUsed();
        double threshold = getImageStoreCapacityThreshold();
        String readableTotalCapacity = FileUtils.byteCountToDisplaySize((long) totalCapacity);
        String readableUsedCapacity = FileUtils.byteCountToDisplaySize((long) usedCapacity);

        LOGGER.debug(String.format("Verifying image storage [%s]. Capacity: total=[%s], used=[%s], threshold=[%s%%].", imageStoreId, readableTotalCapacity, readableUsedCapacity, threshold * 100));

        if (usedCapacity / totalCapacity <= threshold) {
            return true;
        }

        LOGGER.warn(String.format("Image storage [%s] has not enough capacity. Capacity: total=[%s], used=[%s], threshold=[%s%%].", imageStoreId, readableTotalCapacity, readableUsedCapacity, threshold * 100));
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
        LOGGER.debug(String.format("Sending VmStats of host %s to %s host %s:%s", host.getId(), externalStatsType, externalStatsHost, externalStatsPort));
        try {
            GraphiteClient g = new GraphiteClient(externalStatsHost, externalStatsPort);
            g.sendMetrics(metrics);
        } catch (GraphiteException e) {
            LOGGER.debug("Failed sending VmStats to Graphite host " + externalStatsHost + ":" + externalStatsPort + ": " + e.getMessage());
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
     * Persists VM stats in the database.
     * @param statsForCurrentIteration the metrics stats data to persist.
     * @param timestamp the time that will be stamped.
     */
    protected void persistVirtualMachineStats(VmStatsEntry statsForCurrentIteration, Date timestamp) {
        VmStatsEntryBase vmStats = new VmStatsEntryBase(statsForCurrentIteration.getVmId(), statsForCurrentIteration.getMemoryKBs(), statsForCurrentIteration.getIntFreeMemoryKBs(),
                statsForCurrentIteration.getTargetMemoryKBs(), statsForCurrentIteration.getCPUUtilization(), statsForCurrentIteration.getNetworkReadKBs(),
                statsForCurrentIteration.getNetworkWriteKBs(), statsForCurrentIteration.getNumCPUs(), statsForCurrentIteration.getDiskReadKBs(),
                statsForCurrentIteration.getDiskWriteKBs(), statsForCurrentIteration.getDiskReadIOs(), statsForCurrentIteration.getDiskWriteIOs(),
                statsForCurrentIteration.getEntityType());
        VmStatsVO vmStatsVO = new VmStatsVO(statsForCurrentIteration.getVmId(), msId, timestamp, gson.toJson(vmStats));
        LOGGER.trace(String.format("Recording VM stats: [%s].", vmStatsVO.toString()));
        vmStatsDao.persist(vmStatsVO);
    }

    private String getVmDiskStatsEntryAsString(VmDiskStatsEntry statsForCurrentIteration, Hypervisor.HypervisorType hypervisorType) {
        VmDiskStatsEntry entry;
        if (Hypervisor.HypervisorType.KVM.equals(hypervisorType)) {
            entry = new VmDiskStatsEntry(statsForCurrentIteration.getVmName(),
                    statsForCurrentIteration.getPath(),
                    statsForCurrentIteration.getDeltaIoWrite(),
                    statsForCurrentIteration.getDeltaIoRead(),
                    statsForCurrentIteration.getDeltaBytesWrite(),
                    statsForCurrentIteration.getDeltaBytesRead());
        } else {
            entry = statsForCurrentIteration;
        }
        JsonElement element = gson.toJsonTree(entry);
        JsonObject obj = element.getAsJsonObject();
        for (String key : Arrays.asList("deltaIoRead", "deltaIoWrite", "deltaBytesWrite", "deltaBytesRead")) {
            obj.remove(key);
        }
        return obj.toString();
    }

    /**
     * Persists VM disk stats in the database.
     * @param statsForCurrentIteration the metrics stats data to persist.
     * @param timestamp the time that will be stamped.
     */
    protected void persistVolumeStats(long volumeId, VmDiskStatsEntry statsForCurrentIteration, Hypervisor.HypervisorType hypervisorType, Date timestamp) {
        VolumeStatsVO volumeStatsVO = new VolumeStatsVO(volumeId, msId, timestamp, getVmDiskStatsEntryAsString(statsForCurrentIteration, hypervisorType));
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Recording volume stats: [%s].", volumeStatsVO));
        }
        volumeStatsDao.persist(volumeStatsVO);
    }

    /**
     * Removes the oldest VM stats records according to the global
     * parameter {@code vm.stats.max.retention.time}.
     */
    protected void cleanUpVirtualMachineStats() {
        Integer maxRetentionTime = vmStatsMaxRetentionTime.value();
        if (maxRetentionTime <= 0) {
            LOGGER.debug(String.format("Skipping VM stats cleanup. The [%s] parameter [%s] is set to 0 or less than 0.",
                    vmStatsMaxRetentionTime.scope(), vmStatsMaxRetentionTime.toString()));
            return;
        }
        LOGGER.trace("Removing older VM stats records.");
        Date now = new Date();
        Date limit = DateUtils.addMinutes(now, -maxRetentionTime);
        vmStatsDao.removeAllByTimestampLessThan(limit);
    }

    /**
     * Removes the oldest Volume stats records according to the global
     * parameter {@code vm.disk.stats.max.retention.time}.
     */
    protected void cleanUpVolumeStats() {
        Integer maxRetentionTime = vmDiskStatsMaxRetentionTime.value();
        if (maxRetentionTime <= 0) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Skipping Volume stats cleanup. The [%s] parameter [%s] is set to 0 or less than 0.",
                        vmDiskStatsMaxRetentionTime.scope(), vmDiskStatsMaxRetentionTime.toString()));
            }
            return;
        }
        LOGGER.trace("Removing older Volume stats records.");
        Date now = new Date();
        Date limit = DateUtils.addMinutes(now, -maxRetentionTime);
        volumeStatsDao.removeAllByTimestampLessThan(limit);
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

        Map<String, String> tagsToAdd = new HashMap<>();
        tagsToAdd.put(UUID_TAG, vmStatsEntry.getVmUuid());

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
        if(!influxDbConnection.isBatchEnabled()){
            influxDbConnection.enableBatch(BatchOptions.DEFAULTS);
        }

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

    public Map<String, Object> getDbStats() {
        return dbStats;
    }


    public ManagementServerHostStats getManagementServerHostStats(String managementServerUuid) {
        return managementServerHostStats.get(managementServerUuid);
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
        return new ConfigKey<?>[] {vmDiskStatsInterval, vmDiskStatsIntervalMin, vmNetworkStatsInterval, vmNetworkStatsIntervalMin, StatsTimeout, statsOutputUri,
            vmStatsIncrementMetrics, vmStatsMaxRetentionTime, vmStatsCollectUserVMOnly, vmDiskStatsRetentionEnabled, vmDiskStatsMaxRetentionTime,
                VM_STATS_INCREMENT_METRICS_IN_MEMORY,
                MANAGEMENT_SERVER_STATUS_COLLECTION_INTERVAL,
                DATABASE_SERVER_STATUS_COLLECTION_INTERVAL,
                DATABASE_SERVER_LOAD_HISTORY_RETENTION_NUMBER};
    }

    public double getImageStoreCapacityThreshold() {
        return CapacityManager.SecondaryStorageCapacityThreshold.value();
    }
}
