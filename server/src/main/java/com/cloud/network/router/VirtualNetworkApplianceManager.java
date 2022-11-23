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
package com.cloud.network.router;

import java.util.List;

import org.apache.cloudstack.framework.config.ConfigKey;

import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.RemoteAccessVpn;
import com.cloud.network.VirtualNetworkApplianceService;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.utils.component.Manager;
import com.cloud.vm.DomainRouterVO;

/**
 * NetworkManager manages the network for the different end users.
 */
public interface VirtualNetworkApplianceManager extends Manager, VirtualNetworkApplianceService {

    static final String RouterTemplateXenCK = "router.template.xenserver";
    static final String RouterTemplateKvmCK = "router.template.kvm";
    static final String RouterTemplateVmwareCK = "router.template.vmware";
    static final String RouterTemplateHyperVCK = "router.template.hyperv";
    static final String RouterTemplateLxcCK = "router.template.lxc";
    static final String RouterTemplateOvm3CK = "router.template.ovm3";
    static final String SetServiceMonitorCK = "network.router.EnableServiceMonitoring";
    static final String RouterAlertsCheckIntervalCK = "router.alerts.check.interval";
    static final String VirtualRouterServiceOfferingCK = "router.service.offering";
    static final String NetworkVirtualRouterServiceOfferingCK = "network.router.service.offering";

    static final String RouterHealthChecksConfigRefreshIntervalCK = "router.health.checks.config.refresh.interval";
    static final String RouterHealthChecksResultFetchIntervalCK = "router.health.checks.results.fetch.interval";
    static final String RouterHealthChecksFailuresToRecreateVrCK = "router.health.checks.failures.to.recreate.vr";

    static final ConfigKey<String> RouterTemplateXen = new ConfigKey<String>(String.class, RouterTemplateXenCK, "Advanced", "SystemVM Template (XenServer)",
            "Name of the default router template on Xenserver.", true, ConfigKey.Scope.Zone, null);
    static final ConfigKey<String> RouterTemplateKvm = new ConfigKey<String>(String.class, RouterTemplateKvmCK, "Advanced", "SystemVM Template (KVM)",
            "Name of the default router template on KVM.", true, ConfigKey.Scope.Zone, null);
    static final ConfigKey<String> RouterTemplateVmware = new ConfigKey<String>(String.class, RouterTemplateVmwareCK, "Advanced", "SystemVM Template (vSphere)",
            "Name of the default router template on Vmware.", true, ConfigKey.Scope.Zone, null);
    static final ConfigKey<String> RouterTemplateHyperV = new ConfigKey<String>(String.class, RouterTemplateHyperVCK, "Advanced", "SystemVM Template (HyperV)",
            "Name of the default router template on Hyperv.", true, ConfigKey.Scope.Zone, null);
    static final ConfigKey<String> RouterTemplateLxc = new ConfigKey<String>(String.class, RouterTemplateLxcCK, "Advanced", "SystemVM Template (LXC)",
            "Name of the default router template on LXC.", true, ConfigKey.Scope.Zone, null);
    static final ConfigKey<String> RouterTemplateOvm3 = new ConfigKey<String>(String.class, RouterTemplateOvm3CK, "Advanced", "SystemVM Template (Ovm3)",
            "Name of the default router template on Ovm3.", true, ConfigKey.Scope.Zone, null);

    static final ConfigKey<Boolean> SetServiceMonitor = new ConfigKey<Boolean>(Boolean.class, SetServiceMonitorCK, "Advanced", "true",
            "service monitoring in router enable/disable option, default true", true, ConfigKey.Scope.Zone, null);

    static final ConfigKey<Integer> RouterAlertsCheckInterval = new ConfigKey<Integer>(Integer.class, RouterAlertsCheckIntervalCK, "Advanced", "1800",
            "Interval (in seconds) to check for alerts in Virtual Router.", false, ConfigKey.Scope.Global, null);
    static final ConfigKey<Boolean> RouterVersionCheckEnabled = new ConfigKey<Boolean>("Advanced", Boolean.class, "router.version.check", "true",
            "If true, router minimum required version is checked before sending command", false);
    static final ConfigKey<Boolean> UseExternalDnsServers = new ConfigKey<Boolean>(Boolean.class, "use.external.dns", "Advanced", "false",
            "Bypass internal dns, use external dns1 and dns2", true, ConfigKey.Scope.Zone, null);
    static final ConfigKey<Boolean> ExposeDnsAndBootpServer = new ConfigKey<Boolean>(Boolean.class, "expose.dns.externally", "Advanced", "true",
            "open dns, dhcp and bootp on the public interface", true, ConfigKey.Scope.Zone, null);

    static final ConfigKey<String> VirtualRouterServiceOffering = new ConfigKey<String>(String.class, VirtualRouterServiceOfferingCK, "Advanced", "",
            "Uuid of the service offering used by virtual routers; if NULL - system offering will be used", true, ConfigKey.Scope.Account, null);
    static final ConfigKey<String> NetworkVirtualRouterServiceOffering = new ConfigKey<String>(String.class, NetworkVirtualRouterServiceOfferingCK, "Advanced", "",
            "Uuid of the service offering used by virtual routers of this network; if NULL - account/system offering will be used", true, ConfigKey.Scope.Network, null);

    // Health checks
    static final ConfigKey<Boolean> RouterHealthChecksEnabled = new ConfigKey<Boolean>(Boolean.class, "router.health.checks.enabled", "Advanced", "true",
            "If true, router health checks are allowed to be executed and read. If false, all scheduled checks and API calls for on demand checks are disabled.",
            true, ConfigKey.Scope.Global, null);
    static final ConfigKey<Integer> RouterHealthChecksBasicInterval = new ConfigKey<Integer>(Integer.class, "router.health.checks.basic.interval", "Advanced", "3",
            "Interval in minutes at which basic router health checks are performed. If set to 0, no tests are scheduled.",
            true, ConfigKey.Scope.Global, null, RouterHealthChecksEnabled.key());
    static final ConfigKey<Integer> RouterHealthChecksAdvancedInterval = new ConfigKey<Integer>(Integer.class, "router.health.checks.advanced.interval", "Advanced", "10",
            "Interval in minutes at which advanced router health checks are performed. If set to 0, no tests are scheduled.",
            true, ConfigKey.Scope.Global, null, RouterHealthChecksEnabled.key());
    static final ConfigKey<Integer> RouterHealthChecksConfigRefreshInterval = new ConfigKey<Integer>(Integer.class, RouterHealthChecksConfigRefreshIntervalCK, "Advanced", "10",
            "Interval in minutes at which router health checks config - such as scheduling intervals, excluded checks, etc is updated on virtual routers by the management server. This value should" +
                    " be sufficiently high (like 2x) from the router.health.checks.basic.interval and router.health.checks.advanced.interval so that there is time between new results generation and results generation for passed data.",
            false, ConfigKey.Scope.Global, null, RouterHealthChecksEnabled.key());
    static final ConfigKey<Integer> RouterHealthChecksResultFetchInterval = new ConfigKey<Integer>(Integer.class, RouterHealthChecksResultFetchIntervalCK, "Advanced", "10",
            "Interval in minutes at which router health checks results are fetched by management server. On each result fetch, management server evaluates need to recreate VR as per configuration of " + RouterHealthChecksFailuresToRecreateVrCK +
                    "This value should be sufficiently high (like 2x) from the router.health.checks.basic.interval and router.health.checks.advanced.interval so that there is time between new results generation and fetch.",
            false, ConfigKey.Scope.Global, null, RouterHealthChecksEnabled.key());
    static final ConfigKey<String> RouterHealthChecksFailuresToRecreateVr = new ConfigKey<String>(String.class, RouterHealthChecksFailuresToRecreateVrCK, "Advanced", "",
            "Health checks failures defined by this config are the checks that should cause router recreation. If empty the recreate is not attempted for any health check failure. Possible values are comma separated script names " +
                    "from systemvm’s /root/health_scripts/ (namely - cpu_usage_check.py, dhcp_check.py, disk_space_check.py, dns_check.py, gateways_check.py, haproxy_check.py, iptables_check.py, memory_usage_check.py, router_version_check.py), connectivity.test, filesystem.writable.test " +
                    " or services (namely - loadbalancing.service, webserver.service, dhcp.service) ",
            true, ConfigKey.Scope.Zone, null, null, RouterHealthChecksEnabled.key(), null, null, ConfigKey.Kind.CSV, null);
    static final ConfigKey<String> RouterHealthChecksToExclude = new ConfigKey<String>(String.class, "router.health.checks.to.exclude", "Advanced", "",
            "Health checks that should be excluded when executing scheduled checks on the router. This can be a comma separated list of script names placed in the '/root/health_checks/' folder. Currently the following scripts are " +
                    "placed in default systemvm template -  cpu_usage_check.py, disk_space_check.py, gateways_check.py, iptables_check.py, router_version_check.py, dhcp_check.py, dns_check.py, haproxy_check.py, memory_usage_check.py.",
            true, ConfigKey.Scope.Zone, null, null, RouterHealthChecksEnabled.key(), null, null, ConfigKey.Kind.CSV, null);
    static final ConfigKey<Double> RouterHealthChecksFreeDiskSpaceThreshold = new ConfigKey<Double>(Double.class, "router.health.checks.free.disk.space.threshold",
            "Advanced", "100", "Free disk space threshold (in MB) on VR below which the check is considered a failure.",
            true, ConfigKey.Scope.Zone, null, RouterHealthChecksEnabled.key());
    static final ConfigKey<Double> RouterHealthChecksMaxCpuUsageThreshold = new ConfigKey<Double>(Double.class, "router.health.checks.max.cpu.usage.threshold",
            "Advanced", "100", " Max CPU Usage threshold as % above which check is considered a failure.",
            true, ConfigKey.Scope.Zone, null, RouterHealthChecksEnabled.key());
    static final ConfigKey<Double> RouterHealthChecksMaxMemoryUsageThreshold = new ConfigKey<Double>(Double.class, "router.health.checks.max.memory.usage.threshold",
            "Advanced", "100", "Max Memory Usage threshold as % above which check is considered a failure.",
            true, ConfigKey.Scope.Zone, null, RouterHealthChecksEnabled.key());
    ConfigKey<String> RouterLogrotateFrequency = new ConfigKey<>(String.class, "router.logrotate.frequency", "Advanced", "*:00:00",
            "Sets the frequency of the logrotate service on the virtual router. The default value is *:00:00 (hourly) and follows the last block of " +
                    "OnCalendar standard [Hour:Minute:Second]. e.g, *:*:00 is for every minute and */12:00:00 is for every 12 hours. See Systemd Timers for more options. " +
                    "Furthermore, the file's minimum size is hardcoded as 10MiB, meaning that the service of logrotate will run, but not rotate the log files if it does not " +
                    "reach the minimum size.",
            true, ConfigKey.Scope.Zone, null);

    public static final int DEFAULT_ROUTER_VM_RAMSIZE = 256;            // 256M
    public static final int DEFAULT_ROUTER_CPU_MHZ = 500;                // 500 MHz
    public static final boolean USE_POD_VLAN = false;
    public static final int DEFAULT_PRIORITY = 100;
    public static final int DEFAULT_DELTA = 2;

    /**
    /*
     * Send ssh public/private key pair to specified host
     * @param hostId
     * @param pubKey
     * @param prvKey
     *
     * NOT USED IN THE VIRTUAL NET APPLIANCE
     *
     */
    //boolean sendSshKeysToHost(Long hostId, String pubKey, String prvKey):

    boolean startRemoteAccessVpn(Network network, RemoteAccessVpn vpn, List<? extends VirtualRouter> routers) throws ResourceUnavailableException;

    boolean deleteRemoteAccessVpn(Network network, RemoteAccessVpn vpn, List<? extends VirtualRouter> routers) throws ResourceUnavailableException;

    List<VirtualRouter> getRoutersForNetwork(long networkId);

    VirtualRouter stop(VirtualRouter router, boolean forced, User callingUser, Account callingAccount) throws ConcurrentOperationException, ResourceUnavailableException;

    String getDnsBasicZoneUpdate();

    boolean removeDhcpSupportForSubnet(Network network, List<DomainRouterVO> routers) throws ResourceUnavailableException;

    public boolean prepareAggregatedExecution(Network network, List<DomainRouterVO> routers) throws AgentUnavailableException, ResourceUnavailableException;

    public boolean completeAggregatedExecution(Network network, List<DomainRouterVO> routers) throws AgentUnavailableException, ResourceUnavailableException;
}
