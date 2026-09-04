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
package org.apache.cloudstack.report;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.TimeZone;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.net.URL;
import java.net.SocketTimeoutException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.UnknownHostException;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.inject.Inject;
import javax.net.ssl.HttpsURLConnection;

import org.springframework.stereotype.Component;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.utils.identity.InstallationIdentity;

import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;

import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.upgrade.dao.VersionDao;
import com.cloud.upgrade.dao.VersionVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.DiskOfferingVO;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.common.util.concurrent.AtomicLongMap;

@Component
public class UsageReporter extends ManagerBase implements Configurable {

    /**
     * The canonical endpoint of the Apache CloudStack project. Deliberately not a
     * Global Setting: telemetry is meant to give the project authoritative
     * statistics, so "telemetry enabled" has to mean the data reaches the project
     * and not some other destination. DNS provides whatever indirection the
     * receiving infrastructure needs.
     */
    protected static final String TELEMETRY_URI = "https://call-home.cloudstack.org/report";

    /**
     * The version of the report schema, sent along in every report. The collector
     * validates the received payload against the schema belonging to this version,
     * so any change to the structure of the report requires incrementing this
     * version and teaching the collector the new schema.
     */
    protected static final int SCHEMA_VERSION = 1;

    public static final ConfigKey<Integer> TelemetryInterval = new ConfigKey<>("Advanced", Integer.class,
            "telemetry.interval", "0",
            "The interval in days between telemetry reports sent to the CloudStack project. 0 is the default (disabled) and when enabled a value of 7 is recommended. Changing this setting requires a restart of the Management Server.",
            false, ConfigKey.Scope.Global);

    private String uniqueID = null;

    private ScheduledExecutorService _executor = null;

    @Inject
    private HostDao _hostDao;
    @Inject
    private ClusterDao _clusterDao;
    @Inject
    private PrimaryDataStoreDao _storagePoolDao;
    @Inject
    private DataCenterDao _dataCenterDao;
    @Inject
    private VMInstanceDao _vmInstance;
    @Inject
    private VersionDao _versionDao;
    @Inject
    private DiskOfferingDao _diskOfferingDao;

    @Override
    public boolean start() {
        init();
        return true;
    }

    @Override
    public String getConfigComponentName() {
        return UsageReporter.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {TelemetryInterval};
    }

    private void init() {
        if (_executor != null) {
            _executor.shutdown();
        }

        int interval = TelemetryInterval.value();
        if (interval > 0) {
            _executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("UsageReporter"));
            _executor.scheduleWithFixedDelay(new UsageCollector(), interval, interval, TimeUnit.DAYS);
        }
    }

    private void sendReport(String reportUri, String uniqueID, Map<String, Object> reportMap) {

        GsonBuilder builder = new GsonBuilder();

        AtomicGsonAdapter adapter = new AtomicGsonAdapter();
        builder.registerTypeAdapter(AtomicLongMap.class, adapter);

        Gson gson = builder.create();
        String report = gson.toJson(reportMap);

        int http_timeout = 15000;

        HttpsURLConnection conn = null;
        try {
            URL url = new URL(reportUri + "/" + uniqueID);
            if (!"https".equalsIgnoreCase(url.getProtocol())) {
                logger.warn("Usage Reports can only be sent over HTTPS, " + reportUri + " is not a valid URI");
                return;
            }

            logger.info("Usage Report will be send to: " + reportUri);
            logger.debug("REPORT: " + report);

            conn = (HttpsURLConnection) url.openConnection();
            conn.setConnectTimeout(http_timeout);
            conn.setReadTimeout(http_timeout);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");

            try (OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8)) {
                osw.write(report);
            }

            int resp_code = conn.getResponseCode();

            // Consume and close the response stream to allow connection reuse
            InputStream responseStream = (resp_code >= 200 && resp_code < 300)
                    ? conn.getInputStream() : conn.getErrorStream();
            if (responseStream != null) {
                responseStream.skip(Long.MAX_VALUE);
                responseStream.close();
            }

            if (resp_code == HttpsURLConnection.HTTP_OK) {
                logger.info("Usage Report successfully sent to: " + reportUri);
            } else {
                logger.warn("Failed to send Usage Report: " + conn.getResponseMessage());
            }

        } catch (UnknownHostException e) {
            logger.warn("Failed to look up Usage Report host: " + e.getMessage());
        } catch (SocketTimeoutException e) {
            logger.warn("Sending Usage Report to " + reportUri + " timed out: " + e.getMessage());
        } catch (MalformedURLException e) {
            logger.warn(reportUri + " is a invalid URL for sending Usage Report to: " + e.getMessage());
        } catch (ProtocolException e) {
            logger.warn("Sending Usage Report failed due to a invalid protocol: " + e.getMessage());
        } catch (IOException e) {
            logger.warn("Failed to write Usage Report due to a IOException: " + e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * The identity of this installation, derived from the version the database was
     * created with and when. Every Management Server sharing the database derives
     * the same value, so no identity has to be generated or stored.
     */
    protected String getUniqueId() {
        final VersionVO initialVersion = _versionDao.getInitialVersion();
        if (initialVersion == null) {
            logger.debug("No rows found in the version table. Unable to obtain unique ID for this environment");
            return null;
        }

        final String unique = InstallationIdentity.generate(initialVersion.getVersion(), initialVersion.getUpdated());

        logger.debug("Usage Report Unique ID is: " + unique);

        return unique;
    }

    private Map<String, AtomicLongMap> getHostReport() {
        Map<String, AtomicLongMap> hostMap = new HashMap<String, AtomicLongMap>();
        AtomicLongMap<Object> host_types = AtomicLongMap.create();
        AtomicLongMap<Object> host_hypervisor_type = AtomicLongMap.create();
        AtomicLongMap<Object> host_version = AtomicLongMap.create();

        SearchCriteria<HostVO> host_sc = _hostDao.createSearchCriteria();
        List<HostVO> hosts = _hostDao.search(host_sc, null);
        for (HostVO host : hosts) {
            host_types.getAndIncrement(host.getType());
            if (host.getHypervisorType() != null) {
                host_hypervisor_type.getAndIncrement(host.getHypervisorType());
            }

            if (host.getVersion() != null) {
                host_version.getAndIncrement(host.getVersion());
            }
        }

        hostMap.put("version", host_version);
        hostMap.put("hypervisor_type", host_hypervisor_type);
        hostMap.put("type", host_types);

        return hostMap;
    }

    private Map<String, AtomicLongMap> getClusterReport() {
        Map<String, AtomicLongMap> clusterMap = new HashMap<String, AtomicLongMap>();
        AtomicLongMap<Object> cluster_hypervisor_type = AtomicLongMap.create();
        AtomicLongMap<Object> cluster_types = AtomicLongMap.create();

        SearchCriteria<ClusterVO> cluster_sc = _clusterDao.createSearchCriteria();
        List<ClusterVO> clusters = _clusterDao.search(cluster_sc, null);
        for (ClusterVO cluster : clusters) {
            if (cluster.getClusterType() != null) {
                cluster_types.getAndIncrement(cluster.getClusterType());
            }

            if (cluster.getHypervisorType() != null) {
                cluster_hypervisor_type.getAndIncrement(cluster.getHypervisorType());
            }
        }

        clusterMap.put("hypervisor_type", cluster_hypervisor_type);
        clusterMap.put("type", cluster_types);

        return clusterMap;
    }

    private Map<String, AtomicLongMap> getStoragePoolReport() {
        Map<String, AtomicLongMap> storagePoolMap = new HashMap<String, AtomicLongMap>();
        AtomicLongMap<Object> storage_pool_types = AtomicLongMap.create();
        AtomicLongMap<Object> storage_pool_provider = AtomicLongMap.create();
        AtomicLongMap<Object> storage_pool_scope = AtomicLongMap.create();

        List<StoragePoolVO> storagePools = _storagePoolDao.listAll();
        for (StoragePoolVO pool : storagePools) {
            if (pool.getPoolType() != null) {
                storage_pool_types.getAndIncrement(pool.getPoolType());
            }

            if (pool.getStorageProviderName() != null) {
                storage_pool_provider.getAndIncrement(pool.getStorageProviderName());
            }

            if (pool.getScope() != null) {
                storage_pool_scope.getAndIncrement(pool.getScope());
            }
        }

        storagePoolMap.put("type", storage_pool_types);
        storagePoolMap.put("provider", storage_pool_provider);
        storagePoolMap.put("scope", storage_pool_scope);

        return storagePoolMap;
    }

    private Map<String, AtomicLongMap> getDataCenterReport() {
        Map<String, AtomicLongMap> datacenterMap = new HashMap<String, AtomicLongMap>();
        AtomicLongMap<Object> network_type = AtomicLongMap.create();
        AtomicLongMap<Object> dns_provider = AtomicLongMap.create();
        AtomicLongMap<Object> dhcp_provider = AtomicLongMap.create();
        AtomicLongMap<Object> lb_provider = AtomicLongMap.create();
        AtomicLongMap<Object> firewall_provider = AtomicLongMap.create();
        AtomicLongMap<Object> gateway_provider = AtomicLongMap.create();
        AtomicLongMap<Object> userdata_provider = AtomicLongMap.create();
        AtomicLongMap<Object> vpn_provider = AtomicLongMap.create();

        List<DataCenterVO> datacenters = _dataCenterDao.listAllZones();
        for (DataCenterVO datacenter : datacenters) {
            if (datacenter.getNetworkType() != null) {
                network_type.getAndIncrement(datacenter.getNetworkType());
            }

            if (datacenter.getDnsProvider() != null) {
                dns_provider.getAndIncrement(datacenter.getDnsProvider());
            }

            if (datacenter.getDhcpProvider() != null) {
                dhcp_provider.getAndIncrement(datacenter.getDhcpProvider());
            }

            if (datacenter.getLoadBalancerProvider() != null) {
                lb_provider.getAndIncrement(datacenter.getLoadBalancerProvider());
            }

            if (datacenter.getFirewallProvider() != null) {
                firewall_provider.getAndIncrement(datacenter.getFirewallProvider());
            }

            if (datacenter.getGatewayProvider() != null) {
                gateway_provider.getAndIncrement(datacenter.getGatewayProvider());
            }

            if (datacenter.getUserDataProvider() != null) {
                userdata_provider.getAndIncrement(datacenter.getUserDataProvider());
            }

            if (datacenter.getVpnProvider() != null) {
                vpn_provider.getAndIncrement(datacenter.getVpnProvider());
            }
        }

        datacenterMap.put("network_type", network_type);
        datacenterMap.put("dns_provider", dns_provider);
        datacenterMap.put("dhcp_provider", dhcp_provider);
        datacenterMap.put("lb_provider", lb_provider);
        datacenterMap.put("firewall_provider", firewall_provider);
        datacenterMap.put("gateway_provider", gateway_provider);
        datacenterMap.put("userdata_provider", userdata_provider);
        datacenterMap.put("vpn_provider", vpn_provider);

        return datacenterMap;
    }

    /**
     * Rows of removed Instances are kept in the database, so both current and
     * lifetime statistics can be reported. They mean different things: "current"
     * describes the Instances the cloud holds right now, in any state including
     * Destroyed ones which have not been expunged yet, while "lifetime" also
     * counts the removed rows and therefore describes every Instance which ever
     * existed in this cloud.
     */
    private Map<String, Object> getInstanceReport() {

        Map<String, AtomicLongMap> current = new HashMap<String, AtomicLongMap>();
        AtomicLongMap<Object> hypervisor_type = AtomicLongMap.create();
        AtomicLongMap<Object> instance_state = AtomicLongMap.create();
        AtomicLongMap<Object> instance_type = AtomicLongMap.create();
        AtomicLongMap<Object> ha_enabled = AtomicLongMap.create();
        AtomicLongMap<Object> dynamically_scalable = AtomicLongMap.create();

        Map<String, Object> lifetime = new HashMap<String, Object>();
        AtomicLongMap<Object> lifetime_hypervisor_type = AtomicLongMap.create();
        AtomicLongMap<Object> lifetime_type = AtomicLongMap.create();

        long total = 0;
        long removed = 0;

        SearchCriteria<VMInstanceVO> vm_sc = _vmInstance.createSearchCriteria();
        List<VMInstanceVO> vms = _vmInstance.searchIncludingRemoved(vm_sc, null, null, false);
        for (VMInstanceVO vmVO : vms) {
            total++;

            if (vmVO.getHypervisorType() != null) {
                lifetime_hypervisor_type.getAndIncrement(vmVO.getHypervisorType());
            }

            if (vmVO.getType() != null) {
                lifetime_type.getAndIncrement(vmVO.getType());
            }

            if (vmVO.getRemoved() != null) {
                removed++;
                continue;
            }

            if (vmVO.getHypervisorType() != null) {
                hypervisor_type.getAndIncrement(vmVO.getHypervisorType());
            }

            if (vmVO.getState() != null) {
                instance_state.getAndIncrement(vmVO.getState());
            }

            if (vmVO.getType() != null) {
                instance_type.getAndIncrement(vmVO.getType());
            }

            ha_enabled.getAndIncrement(vmVO.isHaEnabled());
            dynamically_scalable.getAndIncrement(vmVO.isDynamicallyScalable());
        }

        current.put("hypervisor_type", hypervisor_type);
        current.put("state", instance_state);
        current.put("type", instance_type);
        current.put("ha_enabled", ha_enabled);
        current.put("dynamically_scalable", dynamically_scalable);

        lifetime.put("total", total);
        lifetime.put("removed", removed);
        lifetime.put("hypervisor_type", lifetime_hypervisor_type);
        lifetime.put("type", lifetime_type);

        Map<String, Object> instanceMap = new HashMap<String, Object>();
        instanceMap.put("current", current);
        instanceMap.put("lifetime", lifetime);

        return instanceMap;
    }

    private Map<String, Object> getDiskOfferingReport() {
        Map<String, Object> diskOfferingReport = new HashMap<String, Object>();

        AtomicLongMap<Object> compute_only = AtomicLongMap.create();
        AtomicLongMap<Object> provisioning_type = AtomicLongMap.create();
        AtomicLongMap<Object> use_local_storage = AtomicLongMap.create();

        List<DiskOfferingVO> offerings = _diskOfferingDao.listAll();

        long disk_size = 0;
        for (DiskOfferingVO offering : offerings) {
            provisioning_type.getAndIncrement(offering.getProvisioningType());
            compute_only.getAndIncrement(offering.isComputeOnly());
            use_local_storage.getAndIncrement(offering.isUseLocalStorage());
            disk_size += offering.getDiskSize();
        }

        diskOfferingReport.put("compute_only", compute_only);
        diskOfferingReport.put("provisioning_type", provisioning_type);
        diskOfferingReport.put("use_local_storage", use_local_storage);
        diskOfferingReport.put("avg_disk_size", offerings.isEmpty() ? 0 : disk_size / offerings.size());

        return diskOfferingReport;
    }

    private Map<String, String> getVersionReport() {
        Map<String, String> versionMap = new HashMap<String, String>();

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        List<VersionVO> versions = _versionDao.getAllVersions();
        for (VersionVO version : versions) {
            versionMap.put(version.getVersion(), dateFormat.format(version.getUpdated()));
        }

        return versionMap;
    }

    private String getCurrentVersion() {
        return _versionDao.getCurrentVersion();
    }

    class UsageCollector extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            try {
                logger.info("UsageReporter is running...");

                if (uniqueID == null) {
                    uniqueID = getUniqueId();
                }

                if (uniqueID == null) {
                    logger.warn("Unable to determine the unique ID of this environment. Not sending Usage Report");
                    return;
                }

                Map<String, Object> reportMap = new HashMap<String, Object>();

                reportMap.put("schema_version", SCHEMA_VERSION);
                reportMap.put("hosts", getHostReport());
                reportMap.put("clusters", getClusterReport());
                reportMap.put("primaryStorage", getStoragePoolReport());
                reportMap.put("zones", getDataCenterReport());
                reportMap.put("instances", getInstanceReport());
                reportMap.put("diskOffering", getDiskOfferingReport());
                reportMap.put("versions", getVersionReport());
                reportMap.put("current_version", getCurrentVersion());

                sendReport(TELEMETRY_URI, uniqueID, reportMap);

            } catch (Exception e) {
                logger.warn("Failed to compile Usage Report: " + e.getMessage());
            }
        }
    }
}
