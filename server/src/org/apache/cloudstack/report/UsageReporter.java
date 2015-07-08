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

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.net.ssl.HttpsURLConnection;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.upgrade.dao.VersionDao;
import com.cloud.upgrade.dao.VersionVO;
import com.cloud.utils.component.ComponentMethodInterceptable;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.google.common.util.concurrent.AtomicLongMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Component
public class UsageReporter extends ManagerBase implements ComponentMethodInterceptable {
    public static final Logger s_logger = Logger.getLogger(UsageReporter.class.getName());

    private String reportHost = "https://reporting.cloudstack.apache.org/report";

    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private static UsageReporter s_instance = null;

    private ScheduledExecutorService _executor = null;

    static final ConfigKey<Long> usageReportInterval = new ConfigKey<Long>("Advanced",
                                                                           Long.class,
                                                                           "usage.report.interval",
                                                                           "7",
                                                                           "Interval (days) between sending anonymous Usage Reports back to the CloudStack project",
                                                                           false);

    @Inject
    ConfigurationDao _configDao;
    @Inject
    HostDao _hostDao;
    @Inject
    ClusterDao _clusterDao;
    @Inject
    PrimaryDataStoreDao _storagePoolDao;
    @Inject
    DataCenterDao _dataCenterDao;
    @Inject
    UserVmDao _userVmDao;
    @Inject
    VMInstanceDao _vmInstance;
    @Inject
    VersionDao _versionDao;
    @Inject
    DiskOfferingDao _diskOfferingDao;

    public synchronized static UsageReporter getInstance(Map<String, String> configs) {
        if (s_instance == null) {
            UsageReporter new_instance = new UsageReporter();
            new_instance.init(configs);
            s_instance = new_instance;
        }
        return s_instance;
    }

    private UsageReporter() {
        s_instance = this;
    }

    @Override
    public boolean start() {
        init(_configDao.getConfiguration());
        return true;
    }

    private void init(Map<String, String> configs) {
        _executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("UsageReporter"));

        if (usageReportInterval.value() > 0) {
            _executor.scheduleWithFixedDelay(new UsageCollector(), 7, usageReportInterval.value(), TimeUnit.DAYS);
        }
    }

    private void sendReport(String reportUrl, Map<String, Object> reportMap) {
        HttpsURLConnection conn = null;
        OutputStreamWriter osw = null;

        int http_timeout = 15000;

        try {
            GsonBuilder builder = new GsonBuilder();

            AtomicGsonAdapter adapter = new AtomicGsonAdapter();
            builder.registerTypeAdapter(AtomicLongMap.class, adapter);

            Gson gson = builder.create();
            String report = gson.toJson(reportMap);

            s_logger.info("Usage Report will be send to: " + reportUrl);
            s_logger.debug("Usage Report being send: " + report);

            URL url = new URL(reportUrl);

            conn = (HttpsURLConnection) url.openConnection();
            conn.setConnectTimeout(http_timeout);
            conn.setReadTimeout(http_timeout);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");

            try {
                osw = new OutputStreamWriter(conn.getOutputStream(), com.cloud.utils.StringUtils.getPreferredCharset());
                osw.write(report);
                osw.flush();
            } catch (IOException e) {
                s_logger.warn("Failed to write to OutputStream due to a IOException: " + e.getMessage());
            } finally {
                if (osw != null) {
                    try {
                        osw.close();
                    } catch (IOException e) {
                        s_logger.debug("Failed to close output stream of HTTPS connection: " + e.getMessage());
                    }
                }
            }

            int resp_code = conn.getResponseCode();

            if (resp_code == HttpsURLConnection.HTTP_OK){
                s_logger.info("Usage Report succesfully send to: " + reportUrl);
            } else {
                s_logger.warn("Failed to send Usage Report: " + conn.getResponseMessage());
            }

        } catch (UnknownHostException e) {
            s_logger.warn("Failed to look up Usage Report host: " + e.getMessage());
        } catch (SocketTimeoutException e) {
            s_logger.warn("Sending Usage Report to " + reportUrl + " timed out: " + e.getMessage());
        } catch (MalformedURLException e) {
            s_logger.warn(reportUrl + " is a invalid URL for sending Usage Report to: "+ e.getMessage());
        } catch (ProtocolException e) {
            s_logger.warn("Sending Usage Report failed due to a invalid protocol: " + e.getMessage());
        } catch (IOException e) {
            s_logger.warn("Failed to write Usage Report due to a IOException: ", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    protected String getUniqueId() {
        VersionVO version = null;
        String unique = null;
        try {
            List<VersionVO> versions = _versionDao.getAllVersions();

            if (versions != null && !versions.isEmpty()) {
                version = versions.get(0);
                unique = DigestUtils.sha256Hex(version.getVersion() + dateFormat.format(version.getUpdated()));
            } else {
                s_logger.info("No rows found in the version table. Unable to obtain unique ID for this environment");
            }

            s_logger.debug("Usage Report Unique ID is: " + unique);

            return unique;
        } catch (Exception e) {
            s_logger.error("Failed to fetch the UniqueID: " + e.getMessage());
            return null;
        }
    }

    protected Map<String, AtomicLongMap> getHostReport() {
        Map<String, AtomicLongMap> hostMap = new HashMap<String, AtomicLongMap>();
        AtomicLongMap<Object> host_types = AtomicLongMap.create();
        AtomicLongMap<Object> host_hypervisor_type = AtomicLongMap.create();
        AtomicLongMap<Object> host_version = AtomicLongMap.create();

        List<HostVO> hosts = _hostDao.search(null, null);
        for (HostVO host : hosts) {
            host_types.getAndIncrement(host.getType());
            if (host.getHypervisorType() != null) {
                host_hypervisor_type.getAndIncrement(host.getHypervisorType());
            }

            host_version.getAndIncrement(host.getVersion());
        }

        hostMap.put("version", host_version);
        hostMap.put("hypervisor_type", host_hypervisor_type);
        hostMap.put("type", host_types);

        return hostMap;
    }

    protected Map<String, AtomicLongMap> getClusterReport() {
        Map<String, AtomicLongMap> clusterMap = new HashMap<String, AtomicLongMap>();
        AtomicLongMap<Object> cluster_hypervisor_type = AtomicLongMap.create();
        AtomicLongMap<Object> cluster_types = AtomicLongMap.create();

        List<ClusterVO> clusters = _clusterDao.search(null, null);
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

    protected Map<String, AtomicLongMap> getStoragePoolReport() {
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

    protected Map<String, AtomicLongMap> getDataCenterReport() {
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

    protected Map<String, AtomicLongMap> getInstanceReport() {

        Map<String, AtomicLongMap> instanceMap = new HashMap<String, AtomicLongMap>();
        AtomicLongMap<Object> hypervisor_type = AtomicLongMap.create();
        AtomicLongMap<Object> instance_state = AtomicLongMap.create();
        AtomicLongMap<Object> instance_type = AtomicLongMap.create();
        AtomicLongMap<Object> ha_enabled = AtomicLongMap.create();
        AtomicLongMap<Object> dynamically_scalable = AtomicLongMap.create();

        List<HostVO> hosts = _hostDao.search(null, null);
        for (HostVO host : hosts) {
            List<UserVmVO> vms = _userVmDao.listByLastHostId(host.getId());
            for (UserVmVO vm : vms) {
                VMInstanceVO vmVO = _vmInstance.findById(vm.getId());

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
        }

        instanceMap.put("hypervisor_type", hypervisor_type);
        instanceMap.put("state", instance_state);
        instanceMap.put("type", instance_type);
        instanceMap.put("ha_enabled", ha_enabled);
        instanceMap.put("dynamically_scalable", dynamically_scalable);

        return instanceMap;
    }

    protected Map<String, Object> getDiskOfferingReport() {
        Map<String, Object> diskOfferingReport = new HashMap<String, Object>();

        AtomicLongMap<Object> system_use = AtomicLongMap.create();
        AtomicLongMap<Object> provisioning_type = AtomicLongMap.create();
        AtomicLongMap<Object> use_local_storage = AtomicLongMap.create();

        List<DiskOfferingVO> private_offerings = _diskOfferingDao.findPrivateDiskOffering();
        List<DiskOfferingVO> public_offerings = _diskOfferingDao.findPublicDiskOfferings();

        List<DiskOfferingVO> offerings = new ArrayList<DiskOfferingVO>();
        offerings.addAll(private_offerings);
        offerings.addAll(public_offerings);

        long disk_size = 0;
        for (DiskOfferingVO offering : offerings) {
            provisioning_type.getAndIncrement(String.valueOf(offering.getProvisioningType()));
            system_use.getAndIncrement(String.valueOf(offering.getSystemUse()));
            use_local_storage.getAndIncrement(String.valueOf(offering.getUseLocalStorage()));
            disk_size += offering.getDiskSize();
        }

        diskOfferingReport.put("system_use", system_use);
        diskOfferingReport.put("provisioning_type", provisioning_type);
        diskOfferingReport.put("use_local_storage", use_local_storage);
        diskOfferingReport.put("avg_disk_size", disk_size / offerings.size());

        return diskOfferingReport;
    }

    protected Map<String, String> getVersionReport() {
        Map<String, String> versionMap = new HashMap<String, String>();

        List<VersionVO> versions = _versionDao.getAllVersions();
        for (VersionVO version : versions) {
            versionMap.put(version.getVersion(), dateFormat.format(version.getUpdated()));
        }

        return versionMap;
    }

    protected String getCurrentVersion() {
        return _versionDao.getCurrentVersion();
    }

    protected Map<String, Object> getReport() {
        Map<String, Object> reportMap = new HashMap<String, Object>();

        reportMap.put("id", getUniqueId());
        reportMap.put("hosts", getHostReport());
        reportMap.put("clusters", getClusterReport());
        reportMap.put("primaryStorage", getStoragePoolReport());
        reportMap.put("zones", getDataCenterReport());
        reportMap.put("instances", getInstanceReport());
        reportMap.put("diskOffering", getDiskOfferingReport());
        reportMap.put("versions", getVersionReport());
        reportMap.put("current_version", getCurrentVersion());

        return reportMap;
    }

    class UsageCollector extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            try {
                s_logger.warn("UsageReporter is running...");
                sendReport(reportHost, getReport());
            } catch (Exception e) {
                s_logger.error("Failed to compile Usage Report", e);
            }
        }
    }
}
