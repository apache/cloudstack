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
package com.cloud.configuration;

import com.cloud.agent.AgentManager;
import com.cloud.consoleproxy.ConsoleProxyManager;
import com.cloud.ha.HighAvailabilityManager;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.router.VpcVirtualNetworkApplianceManager;
import com.cloud.network.vpc.VpcManager;
import com.cloud.server.ManagementServer;
import com.cloud.storage.StorageManager;
import com.cloud.storage.secondary.SecondaryStorageVmManager;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.template.TemplateManager;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.snapshot.VMSnapshotManager;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.engine.subsystem.api.storage.StoragePoolAllocator;
import org.apache.cloudstack.framework.config.ConfigKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @deprecated use the more dynamic ConfigKey
 */
@Deprecated
public enum Config {

    // Alert

    AlertEmailAddresses(
            "Alert",
            ManagementServer.class,
            String.class,
            "alert.email.addresses",
            null,
            "Comma separated list of email addresses used for sending alerts.",
            null),
    AlertEmailSender("Alert", ManagementServer.class, String.class, "alert.email.sender", null, "Sender of alert email (will be in the From header of the email).", null),
    AlertSMTPHost("Alert", ManagementServer.class, String.class, "alert.smtp.host", null, "SMTP hostname used for sending out email alerts.", null),
    AlertSMTPPassword(
            "Secure",
            ManagementServer.class,
            String.class,
            "alert.smtp.password",
            null,
            "Password for SMTP authentication (applies only if alert.smtp.useAuth is true).",
            null),
    AlertSMTPPort("Alert", ManagementServer.class, Integer.class, "alert.smtp.port", "465", "Port the SMTP server is listening on.", null),
    AlertSMTPConnectionTimeout("Alert", ManagementServer.class, Integer.class, "alert.smtp.connectiontimeout", "30000",
            "Socket connection timeout value in milliseconds. -1 for infinite timeout.", null),
    AlertSMTPTimeout(
            "Alert",
            ManagementServer.class,
            Integer.class,
            "alert.smtp.timeout",
            "30000",
            "Socket I/O timeout value in milliseconds. -1 for infinite timeout.",
            null),
    AlertSMTPUseAuth("Alert", ManagementServer.class, String.class, "alert.smtp.useAuth", null, "If true, use SMTP authentication when sending emails.", null),
    AlertSMTPUsername(
            "Alert",
            ManagementServer.class,
            String.class,
            "alert.smtp.username",
            null,
            "Username for SMTP authentication (applies only if alert.smtp.useAuth is true).",
            null),
    CapacityCheckPeriod("Alert", ManagementServer.class, Integer.class, "capacity.check.period", "300000", "The interval in milliseconds between capacity checks", null),
    PublicIpCapacityThreshold(
            "Alert",
            ManagementServer.class,
            Float.class,
            "zone.virtualnetwork.publicip.capacity.notificationthreshold",
            "0.75",
            "Percentage (as a value between 0 and 1) of public IP address space utilization above which alerts will be sent.",
            null),
    PrivateIpCapacityThreshold(
            "Alert",
            ManagementServer.class,
            Float.class,
            "pod.privateip.capacity.notificationthreshold",
            "0.75",
            "Percentage (as a value between 0 and 1) of private IP address space utilization above which alerts will be sent.",
            null),
    SecondaryStorageCapacityThreshold(
            "Alert",
            ManagementServer.class,
            Float.class,
            "zone.secstorage.capacity.notificationthreshold",
            "0.75",
            "Percentage (as a value between 0 and 1) of secondary storage utilization above which alerts will be sent about low storage available.",
            null),
    VlanCapacityThreshold(
            "Alert",
            ManagementServer.class,
            Float.class,
            "zone.vlan.capacity.notificationthreshold",
            "0.75",
            "Percentage (as a value between 0 and 1) of Zone Vlan utilization above which alerts will be sent about low number of Zone Vlans.",
            null),
    DirectNetworkPublicIpCapacityThreshold(
            "Alert",
            ManagementServer.class,
            Float.class,
            "zone.directnetwork.publicip.capacity.notificationthreshold",
            "0.75",
            "Percentage (as a value between 0 and 1) of Direct Network Public Ip Utilization above which alerts will be sent about low number of direct network public ips.",
            null),
    LocalStorageCapacityThreshold(
            "Alert",
            ManagementServer.class,
            Float.class,
            "cluster.localStorage.capacity.notificationthreshold",
            "0.75",
            "Percentage (as a value between 0 and 1) of local storage utilization above which alerts will be sent about low local storage available.",
            null),

    // Storage

    StorageStatsInterval(
            "Storage",
            ManagementServer.class,
            String.class,
            "storage.stats.interval",
            "60000",
            "The interval (in milliseconds) when storage stats (per host) are retrieved from agents.",
            null),
    MaxVolumeSize("Storage", ManagementServer.class, Integer.class, "storage.max.volume.size", "2000", "The maximum size for a volume (in GB).", null),
    StorageCacheReplacementLRUTimeInterval(
            "Storage",
            ManagementServer.class,
            Integer.class,
            "storage.cache.replacement.lru.interval",
            "30",
            "time interval for unused data on cache storage (in days).",
            null),
    StorageCacheReplacementEnabled(
            "Storage",
            ManagementServer.class,
            Boolean.class,
            "storage.cache.replacement.enabled",
            "true",
            "enable or disable cache storage replacement algorithm.",
            null),
    StorageCacheReplacementInterval(
            "Storage",
            ManagementServer.class,
            Integer.class,
            "storage.cache.replacement.interval",
            "86400",
            "time interval between cache replacement threads (in seconds).",
            null),
    MaxUploadVolumeSize("Storage", ManagementServer.class, Integer.class, "storage.max.volume.upload.size", "500", "The maximum size for a uploaded volume(in GB).", null),
    TotalRetries(
            "Storage",
            AgentManager.class,
            Integer.class,
            "total.retries",
            "4",
            "The number of times each command sent to a host should be retried in case of failure.",
            null),
    StoragePoolMaxWaitSeconds(
            "Storage",
            ManagementServer.class,
            Integer.class,
            "storage.pool.max.waitseconds",
            "3600",
            "Timeout (in seconds) to synchronize storage pool operations.",
            null),
    CreateVolumeFromSnapshotWait(
            "Storage",
            StorageManager.class,
            Integer.class,
            "create.volume.from.snapshot.wait",
            "10800",
            "In second, timeout for creating volume from snapshot",
            null),
    CopyVolumeWait("Storage", StorageManager.class, Integer.class, "copy.volume.wait", "10800", "In second, timeout for copy volume command", null),
    CreatePrivateTemplateFromVolumeWait(
            "Storage",
            UserVmManager.class,
            Integer.class,
            "create.private.template.from.volume.wait",
            "10800",
            "In second, timeout for CreatePrivateTemplateFromVolumeCommand",
            null),
    CreatePrivateTemplateFromSnapshotWait(
            "Storage",
            UserVmManager.class,
            Integer.class,
            "create.private.template.from.snapshot.wait",
            "10800",
            "In second, timeout for CreatePrivateTemplateFromSnapshotCommand",
            null),
    BackupSnapshotWait("Storage", StorageManager.class, Integer.class, "backup.snapshot.wait", "21600", "In second, timeout for BackupSnapshotCommand", null),
    HAStorageMigration(
            "Storage",
            ManagementServer.class,
            Boolean.class,
            "enable.ha.storage.migration",
            "true",
            "Enable/disable storage migration across primary storage during HA",
            null),

    // Network
    NetworkLBHaproxyStatsVisbility(
            "Network",
            ManagementServer.class,
            String.class,
            "network.loadbalancer.haproxy.stats.visibility",
            "global",
            "Load Balancer(haproxy) stats visibilty, the value can be one of the following six parameters : global,guest-network,link-local,disabled,all,default",
            null),
    NetworkLBHaproxyStatsUri(
            "Network",
            ManagementServer.class,
            String.class,
            "network.loadbalancer.haproxy.stats.uri",
            "/admin?stats",
            "Load Balancer(haproxy) uri.",
            null),
    NetworkLBHaproxyStatsAuth(
            "Secure",
            ManagementServer.class,
            String.class,
            "network.loadbalancer.haproxy.stats.auth",
            "admin1:AdMiN123",
            "Load Balancer(haproxy) authetication string in the format username:password",
            null),
    NetworkLBHaproxyStatsPort(
            "Network",
            ManagementServer.class,
            String.class,
            "network.loadbalancer.haproxy.stats.port",
            "8081",
            "Load Balancer(haproxy) stats port number.",
            null),
    NetworkLBHaproxyMaxConn(
            "Network",
            ManagementServer.class,
            Integer.class,
            "network.loadbalancer.haproxy.max.conn",
            "4096",
            "Load Balancer(haproxy) maximum number of concurrent connections(global max)",
            null),
    NetworkRouterRpFilter(
            "Network",
            ManagementServer.class,
            Integer.class,
            "network.disable.rpfilter",
            "true",
            "disable rp_filter on Domain Router VM public interfaces.",
            null),

    GuestVlanBits(
            "Network",
            ManagementServer.class,
            Integer.class,
            "guest.vlan.bits",
            "12",
            "The number of bits to reserve for the VLAN identifier in the guest subnet.",
            null),
    //MulticastThrottlingRate("Network", ManagementServer.class, Integer.class, "multicast.throttling.rate", "10", "Default multicast rate in megabits per second allowed.", null),
    DirectNetworkNoDefaultRoute(
            "Network",
            ManagementServer.class,
            Boolean.class,
            "direct.network.no.default.route",
            "false",
            "Direct Network Dhcp Server should not send a default route",
            "true/false"),
    OvsTunnelNetworkDefaultLabel(
            "Network",
            ManagementServer.class,
            String.class,
            "sdn.ovs.controller.default.label",
            "cloud-public",
            "Default network label to be used when fetching interface for GRE endpoints",
            null),
    VmNetworkThrottlingRate(
            "Network",
            ManagementServer.class,
            Integer.class,
            "vm.network.throttling.rate",
            "200",
            "Default data transfer rate in megabits per second allowed in User vm's default network.",
            null),

    SecurityGroupWorkCleanupInterval(
            "Network",
            ManagementServer.class,
            Integer.class,
            "network.securitygroups.work.cleanup.interval",
            "120",
            "Time interval (seconds) in which finished work is cleaned up from the work table",
            null),
    SecurityGroupWorkerThreads(
            "Network",
            ManagementServer.class,
            Integer.class,
            "network.securitygroups.workers.pool.size",
            "50",
            "Number of worker threads processing the security group update work queue",
            null),
    SecurityGroupWorkGlobalLockTimeout(
            "Network",
            ManagementServer.class,
            Integer.class,
            "network.securitygroups.work.lock.timeout",
            "300",
            "Lock wait timeout (seconds) while updating the security group work queue",
            null),
    SecurityGroupWorkPerAgentMaxQueueSize(
            "Network",
            ManagementServer.class,
            Integer.class,
            "network.securitygroups.work.per.agent.queue.size",
            "100",
            "The number of outstanding security group work items that can be queued to a host. If exceeded, work items will get dropped to conserve memory. Security Group Sync will take care of ensuring that the host gets updated eventually",
            null),

    SecurityGroupDefaultAdding(
            "Network",
            ManagementServer.class,
            Boolean.class,
            "network.securitygroups.defaultadding",
            "true",
            "If true, the user VM would be added to the default security group by default",
            null),

    GuestOSNeedGatewayOnNonDefaultNetwork(
            "Network",
            NetworkOrchestrationService.class,
            String.class,
            "network.dhcp.nondefaultnetwork.setgateway.guestos",
            "Windows",
            "The guest OS's name start with this fields would result in DHCP server response gateway information even when the network it's on is not default network. Names are separated by comma.",
            null),

    //VPN
    RemoteAccessVpnPskLength(
            "Network",
            AgentManager.class,
            Integer.class,
            "remote.access.vpn.psk.length",
            "24",
            "The length of the ipsec preshared key (minimum 8, maximum 256)",
            null),
    RemoteAccessVpnUserLimit(
            "Network",
            AgentManager.class,
            String.class,
            "remote.access.vpn.user.limit",
            "8",
            "The maximum number of VPN users that can be created per account",
            null),
    Site2SiteVpnConnectionPerVpnGatewayLimit(
            "Network",
            ManagementServer.class,
            Integer.class,
            "site2site.vpn.vpngateway.connection.limit",
            "4",
            "The maximum number of VPN connection per VPN gateway",
            null),
    Site2SiteVpnSubnetsPerCustomerGatewayLimit(
            "Network",
            ManagementServer.class,
            Integer.class,
            "site2site.vpn.customergateway.subnets.limit",
            "10",
            "The maximum number of subnets per customer gateway",
            null),
    MaxNumberOfSecondaryIPsPerNIC(
            "Network", ManagementServer.class, Integer.class,
            "vm.network.nic.max.secondary.ipaddresses", "256",
            "Specify the number of secondary ip addresses per nic per vm. Default value 10 is used, if not specified.", null),

    EnableServiceMonitoring(
            "Network", ManagementServer.class, Boolean.class,
            "network.router.enableserviceMonitoring", "false",
            "service monitoring in router enable/disable option, default false", null),


    // Console Proxy
    ConsoleProxyCapacityStandby(
            "Console Proxy",
            AgentManager.class,
            String.class,
            "consoleproxy.capacity.standby",
            "10",
            "The minimal number of console proxy viewer sessions that system is able to serve immediately(standby capacity)",
            null),
    ConsoleProxyCapacityScanInterval(
            "Console Proxy",
            AgentManager.class,
            String.class,
            "consoleproxy.capacityscan.interval",
            "30000",
            "The time interval(in millisecond) to scan whether or not system needs more console proxy to ensure minimal standby capacity",
            null),
    ConsoleProxyCmdPort(
            "Console Proxy",
            AgentManager.class,
            Integer.class,
            "consoleproxy.cmd.port",
            "8001",
            "Console proxy command port that is used to communicate with management server",
            null),
    ConsoleProxyRestart("Console Proxy", AgentManager.class, Boolean.class, "consoleproxy.restart", "true", "Console proxy restart flag, defaulted to true", null),
    ConsoleProxyUrlDomain("Console Proxy", AgentManager.class, String.class, "consoleproxy.url.domain", "", "Console proxy url domain", "domainName"),
    ConsoleProxySessionMax(
            "Console Proxy",
            AgentManager.class,
            Integer.class,
            "consoleproxy.session.max",
            String.valueOf(ConsoleProxyManager.DEFAULT_PROXY_CAPACITY),
            "The max number of viewer sessions console proxy is configured to serve for",
            null),
    ConsoleProxySessionTimeout(
            "Console Proxy",
            AgentManager.class,
            Integer.class,
            "consoleproxy.session.timeout",
            "300000",
            "Timeout(in milliseconds) that console proxy tries to maintain a viewer session before it times out the session for no activity",
            null),
    ConsoleProxyDisableRpFilter(
            "Console Proxy",
            AgentManager.class,
            Integer.class,
            "consoleproxy.disable.rpfilter",
            "true",
            "disable rp_filter on console proxy VM public interface",
            null),
    ConsoleProxyLaunchMax(
            "Console Proxy",
            AgentManager.class,
            Integer.class,
            "consoleproxy.launch.max",
            "10",
            "maximum number of console proxy instances per zone can be launched",
            null),
    ConsoleProxyManagementState(
            "Console Proxy",
            AgentManager.class,
            String.class,
            "consoleproxy.management.state",
            com.cloud.consoleproxy.ConsoleProxyManagementState.Auto.toString(),
            "console proxy service management state",
            null),
    ConsoleProxyManagementLastState(
            "Console Proxy",
            AgentManager.class,
            String.class,
            "consoleproxy.management.state.last",
            com.cloud.consoleproxy.ConsoleProxyManagementState.Auto.toString(),
            "last console proxy service management state",
            null),

    // Snapshots

    SnapshotPollInterval(
            "Snapshots",
            SnapshotManager.class,
            Integer.class,
            "snapshot.poll.interval",
            "300",
            "The time interval in seconds when the management server polls for snapshots to be scheduled.",
            null),
    SnapshotDeltaMax("Snapshots", SnapshotManager.class, Integer.class, "snapshot.delta.max", "16", "max delta snapshots between two full snapshots.", null),
    KVMSnapshotEnabled("Hidden", SnapshotManager.class, Boolean.class, "kvm.snapshot.enabled", "false", "whether snapshot is enabled for KVM hosts", null),

    // Advanced
    EventPurgeInterval(
            "Advanced",
            ManagementServer.class,
            Integer.class,
            "event.purge.interval",
            "86400",
            "The interval (in seconds) to wait before running the event purge thread",
            null),
    AccountCleanupInterval(
            "Advanced",
            ManagementServer.class,
            Integer.class,
            "account.cleanup.interval",
            "86400",
            "The interval (in seconds) between cleanup for removed accounts",
            null),
    InstanceName("Advanced", AgentManager.class, String.class, "instance.name", "VM", "Name of the deployment instance.", "instanceName"),
    ExpungeDelay(
            "Advanced",
            UserVmManager.class,
            Integer.class,
            "expunge.delay",
            "86400",
            "Determines how long (in seconds) to wait before actually expunging destroyed vm. The default value = the default value of expunge.interval",
            null),
    ExpungeInterval(
            "Advanced",
            UserVmManager.class,
            Integer.class,
            "expunge.interval",
            "86400",
            "The interval (in seconds) to wait before running the expunge thread.",
            null),
    ExpungeWorkers("Advanced", UserVmManager.class, Integer.class, "expunge.workers", "1", "Number of workers performing expunge ", null),
    ExtractURLCleanUpInterval(
            "Advanced",
            ManagementServer.class,
            Integer.class,
            "extract.url.cleanup.interval",
            "7200",
            "The interval (in seconds) to wait before cleaning up the extract URL's ",
            null),
    DisableExtraction(
            "Advanced",
            ManagementServer.class,
            Boolean.class,
            "disable.extraction",
            "false",
            "Flag for disabling extraction of template, isos and volumes",
            null),
    ExtractURLExpirationInterval(
            "Advanced",
            ManagementServer.class,
            Integer.class,
            "extract.url.expiration.interval",
            "14400",
            "The life of an extract URL after which it is deleted ",
            null),
    HostStatsInterval(
            "Advanced",
            ManagementServer.class,
            Integer.class,
            "host.stats.interval",
            "60000",
            "The interval (in milliseconds) when host stats are retrieved from agents.",
            null),
    HostRetry("Advanced", AgentManager.class, Integer.class, "host.retry", "2", "Number of times to retry hosts for creating a volume", null),
    RouterCpuMHz(
            "Advanced",
            NetworkOrchestrationService.class,
            Integer.class,
            "router.cpu.mhz",
            String.valueOf(VpcVirtualNetworkApplianceManager.DEFAULT_ROUTER_CPU_MHZ),
            "Default CPU speed (MHz) for router VM.",
            null),
    RouterStatsInterval(
            "Advanced",
            NetworkOrchestrationService.class,
            Integer.class,
            "router.stats.interval",
            "300",
            "Interval (in seconds) to report router statistics.",
            null),
    ExternalNetworkStatsInterval(
            "Advanced",
            NetworkOrchestrationService.class,
            Integer.class,
            "external.network.stats.interval",
            "300",
            "Interval (in seconds) to report external network statistics.",
            null),
    RouterCheckInterval(
            "Advanced",
            NetworkOrchestrationService.class,
            Integer.class,
            "router.check.interval",
            "30",
            "Interval (in seconds) to report redundant router status.",
            null),
    RouterCheckPoolSize(
            "Advanced",
            NetworkOrchestrationService.class,
            Integer.class,
            "router.check.poolsize",
            "10",
            "Numbers of threads using to check redundant router status.",
            null),
    RouterExtraPublicNics(
            "Advanced",
            NetworkOrchestrationService.class,
            Integer.class,
            "router.extra.public.nics",
            "2",
            "specify extra public nics used for virtual router(up to 5)",
            "0-5"),
    ScaleRetry("Advanced", ManagementServer.class, Integer.class, "scale.retry", "2", "Number of times to retry scaling up the vm", null),
    UpdateWait("Advanced", AgentManager.class, Integer.class, "update.wait", "600", "Time to wait (in seconds) before alerting on a updating agent", null),
    XapiWait("Advanced", AgentManager.class, Integer.class, "xapiwait", "60", "Time (in seconds) to wait for XAPI to return", null),
    MigrateWait("Advanced", AgentManager.class, Integer.class, "migratewait", "3600", "Time (in seconds) to wait for VM migrate finish", null),
    MountParent(
            "Advanced",
            ManagementServer.class,
            String.class,
            "mount.parent",
            "/var/cloudstack/mnt",
            "The mount point on the Management Server for Secondary Storage.",
            null),
    SystemVMAutoReserveCapacity(
            "Advanced",
            ManagementServer.class,
            Boolean.class,
            "system.vm.auto.reserve.capacity",
            "true",
            "Indicates whether or not to automatically reserver system VM standby capacity.",
            null),
    SystemVMDefaultHypervisor("Advanced",
            ManagementServer.class,
            String.class,
            "system.vm.default.hypervisor",
            null,
            "Hypervisor type used to create system vm, valid values are: XenServer, KVM, VMware, Hyperv, VirtualBox, Parralels, BareMetal, Ovm, LXC, Any",
            null),
    SystemVMRandomPassword(
            "Advanced",
            ManagementServer.class,
            Boolean.class,
            "system.vm.random.password",
            "false",
            "Randomize system vm password the first time management server starts",
            null),
    LinkLocalIpNums("Advanced", ManagementServer.class, Integer.class, "linkLocalIp.nums", "10", "The number of link local ip that needed by domR(in power of 2)", null),
    HypervisorList(
            "Advanced",
            ManagementServer.class,
            String.class,
            "hypervisor.list",
            HypervisorType.Hyperv + "," + HypervisorType.KVM + "," + HypervisorType.XenServer + "," + HypervisorType.VMware + "," + HypervisorType.BareMetal + "," +
                    HypervisorType.Ovm + "," + HypervisorType.LXC + "," + HypervisorType.Ovm3,
                    "The list of hypervisors that this deployment will use.",
            "hypervisorList"),
    ManagementNetwork("Advanced", ManagementServer.class, String.class, "management.network.cidr", null, "The cidr of management server network", null),
    EventPurgeDelay(
            "Advanced",
            ManagementServer.class,
            Integer.class,
            "event.purge.delay",
            "15",
            "Events older than specified number days will be purged. Set this value to 0 to never delete events",
            null),
    SecStorageVmMTUSize(
            "Advanced",
            AgentManager.class,
            Integer.class,
            "secstorage.vm.mtu.size",
            String.valueOf(SecondaryStorageVmManager.DEFAULT_SS_VM_MTUSIZE),
            "MTU size (in Byte) of storage network in secondary storage vms",
            null),
    MaxTemplateAndIsoSize(
            "Advanced",
            ManagementServer.class,
            Long.class,
            "max.template.iso.size",
            "50",
            "The maximum size for a downloaded template or ISO (in GB).",
            null),
    SecStorageAllowedInternalDownloadSites(
            "Advanced",
            ManagementServer.class,
            String.class,
            "secstorage.allowed.internal.sites",
            null,
            "Comma separated list of cidrs internal to the datacenter that can host template download servers, please note 0.0.0.0 is not a valid site",
            null),
    SecStorageEncryptCopy(
            "Advanced",
            ManagementServer.class,
            Boolean.class,
            "secstorage.encrypt.copy",
            "false",
            "Use SSL method used to encrypt copy traffic between zones",
            "true,false"),
    SecStorageSecureCopyCert(
            "Advanced",
            ManagementServer.class,
            String.class,
            "secstorage.ssl.cert.domain",
            "",
            "SSL certificate used to encrypt copy traffic between zones",
            "domainName"),
    SecStorageCapacityStandby(
            "Advanced",
            AgentManager.class,
            Integer.class,
            "secstorage.capacity.standby",
            "10",
            "The minimal number of command execution sessions that system is able to serve immediately(standby capacity)",
            null),
    SecStorageSessionMax(
            "Advanced",
            AgentManager.class,
            Integer.class,
            "secstorage.session.max",
            "50",
            "The max number of command execution sessions that a SSVM can handle",
            null),
    SecStorageCmdExecutionTimeMax(
            "Advanced",
            AgentManager.class,
            Integer.class,
            "secstorage.cmd.execution.time.max",
            "30",
            "The max command execution time in minute",
            null),
    SecStorageProxy(
            "Advanced",
            AgentManager.class,
            String.class,
            "secstorage.proxy",
            null,
            "http proxy used by ssvm, in http://username:password@proxyserver:port format",
            null),
    AlertPurgeInterval(
            "Advanced",
            ManagementServer.class,
            Integer.class,
            "alert.purge.interval",
            "86400",
            "The interval (in seconds) to wait before running the alert purge thread",
            null),
    AlertPurgeDelay(
            "Advanced",
            ManagementServer.class,
            Integer.class,
            "alert.purge.delay",
            "0",
            "Alerts older than specified number days will be purged. Set this value to 0 to never delete alerts",
            null),
    HostReservationReleasePeriod(
            "Advanced",
            ManagementServer.class,
            Integer.class,
            "host.reservation.release.period",
            "300000",
            "The interval in milliseconds between host reservation release checks",
            null),
    // LB HealthCheck Interval.
    LBHealthCheck(
            "Advanced",
            ManagementServer.class,
            String.class,
            "healthcheck.update.interval",
            "600",
            "Time Interval to fetch the LB health check states (in sec)",
            null),
    NCCCmdTimeOut(
            "Advanced",
            ManagementServer.class,
            Long.class,
            "ncc.command.timeout",
            "600000", // 10 minutes
            "Command Timeout Interval (in millisec)",
            null),
    DirectAttachNetworkEnabled(
            "Advanced",
            ManagementServer.class,
            Boolean.class,
            "direct.attach.network.externalIpAllocator.enabled",
            "false",
            "Direct-attach VMs using external DHCP server",
            "true,false"),
    DirectAttachNetworkExternalAPIURL(
            "Advanced",
            ManagementServer.class,
            String.class,
            "direct.attach.network.externalIpAllocator.url",
            null,
            "Direct-attach VMs using external DHCP server (API url)",
            null),
    CheckPodCIDRs(
            "Advanced",
            ManagementServer.class,
            String.class,
            "check.pod.cidrs",
            "true",
            "If true, different pods must belong to different CIDR subnets.",
            "true,false"),
    NetworkGcWait(
            "Advanced",
            ManagementServer.class,
            Integer.class,
            "network.gc.wait",
            "600",
            "Time (in seconds) to wait before shutting down a network that's not in used",
            null),
    NetworkGcInterval("Advanced", ManagementServer.class, Integer.class, "network.gc.interval", "600", "Seconds to wait before checking for networks to shutdown", null),
    CapacitySkipcountingHours(
            "Advanced",
            ManagementServer.class,
            Integer.class,
            "capacity.skipcounting.hours",
            "3600",
            "Time (in seconds) to wait before release VM's cpu and memory when VM in stopped state",
            null),
    VmStatsInterval(
            "Advanced",
            ManagementServer.class,
            Integer.class,
            "vm.stats.interval",
            "60000",
            "The interval (in milliseconds) when vm stats are retrieved from agents.",
            null),
    VmDiskStatsInterval("Advanced", ManagementServer.class, Integer.class, "vm.disk.stats.interval", "0", "Interval (in seconds) to report vm disk statistics.", null),
    VolumeStatsInterval("Advanced", ManagementServer.class, Integer.class, "volume.stats.interval", "60000", "Interval (in miliseconds) to report volume statistics.", null),
    VmTransitionWaitInterval(
            "Advanced",
            ManagementServer.class,
            Integer.class,
            "vm.tranisition.wait.interval",
            "3600",
            "Time (in seconds) to wait before taking over a VM in transition state",
            null),
    VmDiskThrottlingIopsReadRate(
            "Advanced",
            ManagementServer.class,
            Integer.class,
            "vm.disk.throttling.iops_read_rate",
            "0",
            "Default disk I/O read rate in requests per second allowed in User vm's disk.",
            null),
    VmDiskThrottlingIopsWriteRate(
            "Advanced",
            ManagementServer.class,
            Integer.class,
            "vm.disk.throttling.iops_write_rate",
            "0",
            "Default disk I/O writerate in requests per second allowed in User vm's disk.",
            null),
    VmDiskThrottlingBytesReadRate(
            "Advanced",
            ManagementServer.class,
            Integer.class,
            "vm.disk.throttling.bytes_read_rate",
            "0",
            "Default disk I/O read rate in bytes per second allowed in User vm's disk.",
            null),
    VmDiskThrottlingBytesWriteRate(
            "Advanced",
            ManagementServer.class,
            Integer.class,
            "vm.disk.throttling.bytes_write_rate",
            "0",
            "Default disk I/O writerate in bytes per second allowed in User vm's disk.",
            null),
    ControlCidr(
            "Advanced",
            ManagementServer.class,
            String.class,
            "control.cidr",
            "169.254.0.0/16",
            "Changes the cidr for the control network traffic.  Defaults to using link local.  Must be unique within pods",
            null),
    ControlGateway("Advanced", ManagementServer.class, String.class, "control.gateway", "169.254.0.1", "gateway for the control network traffic", null),
    HostCapacityTypeToOrderClusters(
            "Advanced",
            ManagementServer.class,
            String.class,
            "host.capacityType.to.order.clusters",
            "CPU",
            "The host capacity type (CPU or RAM) is used by deployment planner to order clusters during VM resource allocation",
            "CPU,RAM"),
    ApplyAllocationAlgorithmToPods(
            "Advanced",
            ManagementServer.class,
            Boolean.class,
            "apply.allocation.algorithm.to.pods",
            "false",
            "If true, deployment planner applies the allocation heuristics at pods first in the given datacenter during VM resource allocation",
            "true,false"),
    VmUserDispersionWeight(
            "Advanced",
            ManagementServer.class,
            Float.class,
            "vm.user.dispersion.weight",
            "1",
            "Weight for user dispersion heuristic (as a value between 0 and 1) applied to resource allocation during vm deployment. Weight for capacity heuristic will be (1 - weight of user dispersion)",
            null),
    VmAllocationAlgorithm(
            "Advanced",
            ManagementServer.class,
            String.class,
            "vm.allocation.algorithm",
            "random",
            "'random', 'firstfit', 'userdispersing', 'userconcentratedpod_random', 'userconcentratedpod_firstfit', 'firstfitleastconsumed' : Order in which hosts within a cluster will be considered for VM/volume allocation.",
            null),
    VmDeploymentPlanner(
            "Advanced",
            ManagementServer.class,
            String.class,
            "vm.deployment.planner",
            "FirstFitPlanner",
            "'FirstFitPlanner', 'UserDispersingPlanner', 'UserConcentratedPodPlanner': DeploymentPlanner heuristic that will be used for VM deployment.",
            null),
    ElasticLoadBalancerEnabled(
            "Advanced",
            ManagementServer.class,
            String.class,
            "network.loadbalancer.basiczone.elb.enabled",
            "false",
            "Whether the load balancing service is enabled for basic zones",
            "true,false"),
    ElasticLoadBalancerNetwork(
            "Advanced",
            ManagementServer.class,
            String.class,
            "network.loadbalancer.basiczone.elb.network",
            "guest",
            "Whether the elastic load balancing service public ips are taken from the public or guest network",
            "guest,public"),
    ElasticLoadBalancerVmMemory(
            "Advanced",
            ManagementServer.class,
            Integer.class,
            "network.loadbalancer.basiczone.elb.vm.ram.size",
            "128",
            "Memory in MB for the elastic load balancer vm",
            null),
    ElasticLoadBalancerVmCpuMhz(
            "Advanced",
            ManagementServer.class,
            Integer.class,
            "network.loadbalancer.basiczone.elb.vm.cpu.mhz",
            "128",
            "CPU speed for the elastic load balancer vm",
            null),
    ElasticLoadBalancerVmNumVcpu(
            "Advanced",
            ManagementServer.class,
            Integer.class,
            "network.loadbalancer.basiczone.elb.vm.vcpu.num",
            "1",
            "Number of VCPU  for the elastic load balancer vm",
            null),
    ElasticLoadBalancerVmGcInterval(
            "Advanced",
            ManagementServer.class,
            Integer.class,
            "network.loadbalancer.basiczone.elb.gc.interval.minutes",
            "30",
            "Garbage collection interval to destroy unused ELB vms in minutes. Minimum of 5",
            null),
    EnableEC2API("Advanced", ManagementServer.class, Boolean.class, "enable.ec2.api", "false", "enable EC2 API on CloudStack", null),
    EnableS3API("Advanced", ManagementServer.class, Boolean.class, "enable.s3.api", "false", "enable Amazon S3 API on CloudStack", null),
    RecreateSystemVmEnabled(
            "Advanced",
            ManagementServer.class,
            Boolean.class,
            "recreate.systemvm.enabled",
            "false",
            "If true, will recreate system vm root disk whenever starting system vm",
            "true,false"),
    SetVmInternalNameUsingDisplayName(
            "Advanced",
            ManagementServer.class,
            Boolean.class,
            "vm.instancename.flag",
            "false",
            "If set to true, will set guest VM's name as it appears on the hypervisor, to its hostname. The flag is supported for VMware hypervisor only",
            "true,false"),
    IncorrectLoginAttemptsAllowed(
            "Advanced",
            ManagementServer.class,
            Integer.class,
            "incorrect.login.attempts.allowed",
            "5",
            "Incorrect login attempts allowed before the user is disabled",
            null),
    // Ovm
    OvmPublicNetwork("Hidden", ManagementServer.class, String.class, "ovm.public.network.device", null, "Specify the public bridge on host for public network", null),
    OvmPrivateNetwork("Hidden", ManagementServer.class, String.class, "ovm.private.network.device", null, "Specify the private bridge on host for private network", null),
    OvmGuestNetwork("Hidden", ManagementServer.class, String.class, "ovm.guest.network.device", null, "Specify the private bridge on host for private network", null),

    // Ovm3
    Ovm3PublicNetwork("Hidden", ManagementServer.class, String.class, "ovm3.public.network.device", null, "Specify the public bridge on host for public network", null),
    Ovm3PrivateNetwork("Hidden", ManagementServer.class, String.class, "ovm3.private.network.device", null, "Specify the private bridge on host for private network", null),
    Ovm3GuestNetwork("Hidden", ManagementServer.class, String.class, "ovm3.guest.network.device", null, "Specify the guest bridge on host for guest network", null),
    Ovm3StorageNetwork("Hidden", ManagementServer.class, String.class, "ovm3.storage.network.device", null, "Specify the storage bridge on host for storage network", null),
    Ovm3HeartBeatTimeout(
            "Advanced",
            ManagementServer.class,
            Integer.class,
            "ovm3.heartbeat.timeout",
            "120",
            "timeout used for primary storage check, upon timeout a panic is triggered.",
            null),
    Ovm3HeartBeatInterval(
            "Advanced",
            ManagementServer.class,
            Integer.class,
            "ovm3.heartbeat.interval",
            "1",
            "interval used to check primary storage availability.",
            null),


    // XenServer
    XenServerPublicNetwork(
            "Hidden",
            ManagementServer.class,
            String.class,
            "xenserver.public.network.device",
            null,
            "[ONLY IF THE PUBLIC NETWORK IS ON A DEDICATED NIC]:The network name label of the physical device dedicated to the public network on a XenServer host",
            null),
    XenServerStorageNetwork1("Hidden", ManagementServer.class, String.class, "xenserver.storage.network.device1", null, "Specify when there are storage networks", null),
    XenServerStorageNetwork2("Hidden", ManagementServer.class, String.class, "xenserver.storage.network.device2", null, "Specify when there are storage networks", null),
    XenServerPrivateNetwork("Hidden", ManagementServer.class, String.class, "xenserver.private.network.device", null, "Specify when the private network name is different", null),
    NetworkGuestCidrLimit(
            "Network",
            NetworkOrchestrationService.class,
            Integer.class,
            "network.guest.cidr.limit",
            "22",
            "size limit for guest cidr; can't be less than this value",
            null),
    XenServerSetupMultipath("Advanced", ManagementServer.class, String.class, "xenserver.setup.multipath", "false", "Setup the host to do multipath", null),
    XenServerBondStorageNic("Advanced", ManagementServer.class, String.class, "xenserver.bond.storage.nics", null, "Attempt to bond the two networks if found", null),
    XenServerHeartBeatTimeout(
            "Advanced",
            ManagementServer.class,
            Integer.class,
            "xenserver.heartbeat.timeout",
            "120",
            "heartbeat timeout to use when implementing XenServer Self Fencing",
            null),
    XenServerHeartBeatInterval(
            "Advanced",
            ManagementServer.class,
            Integer.class,
            "xenserver.heartbeat.interval",
            "60",
            "heartbeat interval to use when checking before XenServer Self Fencing",
            null),
    XenServerGuestNetwork("Hidden", ManagementServer.class, String.class, "xenserver.guest.network.device", null, "Specify for guest network name label", null),
    XenServerMaxNics("Advanced", AgentManager.class, Integer.class, "xenserver.nics.max", "7", "Maximum allowed nics for Vms created on XenServer", null),
    XenServerPVdriverVersion(
            "Advanced",
            ManagementServer.class,
            String.class,
            "xenserver.pvdriver.version",
            "xenserver61",
            "default Xen PV driver version for registered template, valid value:xenserver56,xenserver61 ",
            "xenserver56,xenserver61"),
    XenServerHotFix("Advanced",
            ManagementServer.class,
            Boolean.class,
            "xenserver.hotfix.enabled",
            "false",
            "Enable/Disable XenServer hot fix",
            null),

    // VMware
    VmwareUseNexusVSwitch(
            "Network",
            ManagementServer.class,
            Boolean.class,
            "vmware.use.nexus.vswitch",
            "false",
            "Enable/Disable Cisco Nexus 1000v vSwitch in VMware environment",
            null),
    VmwareUseDVSwitch(
            "Network",
            ManagementServer.class,
            Boolean.class,
            "vmware.use.dvswitch",
            "false",
            "Enable/Disable Nexus/Vmware dvSwitch in VMware environment",
            null),
    VmwareCreateFullClone(
            "Advanced",
            ManagementServer.class,
            Boolean.class,
            "vmware.create.full.clone",
            "true",
            "If set to true, creates guest VMs as full clones on ESX",
            null),
    VmwareServiceConsole(
            "Advanced",
            ManagementServer.class,
            String.class,
            "vmware.service.console",
            "Service Console",
            "Specify the service console network name(for ESX hosts)",
            null),
    VmwareManagementPortGroup(
            "Advanced",
            ManagementServer.class,
            String.class,
            "vmware.management.portgroup",
            "Management Network",
            "Specify the management network name(for ESXi hosts)",
            null),
    VmwareAdditionalVncPortRangeStart(
            "Advanced",
            ManagementServer.class,
            Integer.class,
            "vmware.additional.vnc.portrange.start",
            "50000",
            "Start port number of additional VNC port range",
            null),
    VmwareAdditionalVncPortRangeSize(
            "Advanced",
            ManagementServer.class,
            Integer.class,
            "vmware.additional.vnc.portrange.size",
            "1000",
            "Start port number of additional VNC port range",
            null),
    //VmwareGuestNicDeviceType("Advanced", ManagementServer.class, String.class, "vmware.guest.nic.device.type", "E1000", "Ethernet card type used in guest VM, valid values are E1000, PCNet32, Vmxnet2, Vmxnet3", null),
    VmwareRootDiskControllerType(
            "Advanced",
            ManagementServer.class,
            String.class,
            "vmware.root.disk.controller",
            "ide",
            "Specify the default disk controller for root volumes, valid values are scsi, ide, osdefault. Please check documentation for more details on each of these values.",
            null),
    VmwareSystemVmNicDeviceType(
            "Advanced",
            ManagementServer.class,
            String.class,
            "vmware.systemvm.nic.device.type",
            "E1000",
            "Specify the default network device type for system VMs, valid values are E1000, PCNet32, Vmxnet2, Vmxnet3",
            null),
    VmwareRecycleHungWorker(
            "Advanced",
            ManagementServer.class,
            Boolean.class,
            "vmware.recycle.hung.wokervm",
            "false",
            "Specify whether or not to recycle hung worker VMs",
            null),
    VmwareHungWorkerTimeout("Advanced", ManagementServer.class, Long.class, "vmware.hung.wokervm.timeout", "7200", "Worker VM timeout in seconds", null),
    VmwareVcenterSessionTimeout("Advanced", ManagementServer.class, Long.class, "vmware.vcenter.session.timeout", "1200", "VMware client timeout in seconds", null),

    // KVM
    KvmPublicNetwork("Hidden", ManagementServer.class, String.class, "kvm.public.network.device", null, "Specify the public bridge on host for public network", null),
    KvmPrivateNetwork("Hidden", ManagementServer.class, String.class, "kvm.private.network.device", null, "Specify the private bridge on host for private network", null),
    KvmGuestNetwork("Hidden", ManagementServer.class, String.class, "kvm.guest.network.device", null, "Specify the private bridge on host for private network", null),

    // Hyperv
    HypervPublicNetwork(
            "Hidden",
            ManagementServer.class,
            String.class,
            "hyperv.public.network.device",
            null,
            "Specify the public virtual switch on host for public network",
            null),
    HypervPrivateNetwork(
            "Hidden",
            ManagementServer.class,
            String.class,
            "hyperv.private.network.device",
            null,
            "Specify the virtual switch on host for private network",
            null),
    HypervGuestNetwork(
            "Hidden",
            ManagementServer.class,
            String.class,
            "hyperv.guest.network.device",
            null,
            "Specify the virtual switch on host for private network",
            null),

    // Usage
    UsageExecutionTimezone("Usage", ManagementServer.class, String.class, "usage.execution.timezone", null, "The timezone to use for usage job execution time", null),
    UsageStatsJobAggregationRange(
            "Usage",
            ManagementServer.class,
            Integer.class,
            "usage.stats.job.aggregation.range",
            "1440",
            "The range of time for aggregating the user statistics specified in minutes (e.g. 1440 for daily, 60 for hourly.",
            null),
    UsageStatsJobExecTime(
            "Usage",
            ManagementServer.class,
            String.class,
            "usage.stats.job.exec.time",
            "00:15",
            "The time at which the usage statistics aggregation job will run as an HH24:MM time, e.g. 00:30 to run at 12:30am.",
            null),
    EnableUsageServer("Usage", ManagementServer.class, Boolean.class, "enable.usage.server", "true", "Flag for enabling usage", null),
    DirectNetworkStatsInterval(
            "Usage",
            ManagementServer.class,
            Integer.class,
            "direct.network.stats.interval",
            "86400",
            "Interval (in seconds) to collect stats from Traffic Monitor",
            null),
    UsageSanityCheckInterval(
            "Usage",
            ManagementServer.class,
            Integer.class,
            "usage.sanity.check.interval",
            null,
            "Interval (in days) to check sanity of usage data. To disable set it to 0 or negative.",
            null),
    UsageAggregationTimezone("Usage", ManagementServer.class, String.class, "usage.aggregation.timezone", "GMT", "The timezone to use for usage stats aggregation", null),
    TrafficSentinelIncludeZones(
            "Usage",
            ManagementServer.class,
            Integer.class,
            "traffic.sentinel.include.zones",
            "EXTERNAL",
            "Traffic going into specified list of zones is metered. For metering all traffic leave this parameter empty",
            null),
    TrafficSentinelExcludeZones(
            "Usage",
            ManagementServer.class,
            Integer.class,
            "traffic.sentinel.exclude.zones",
            "",
            "Traffic going into specified list of zones is not metered.",
            null),

    // Hidden
    UseSecondaryStorageVm(
            "Hidden",
            ManagementServer.class,
            Boolean.class,
            "secondary.storage.vm",
            "false",
            "Deploys a VM per zone to manage secondary storage if true, otherwise secondary storage is mounted on management server",
            null),
    CreatePoolsInPod(
            "Hidden",
            ManagementServer.class,
            Boolean.class,
            "xenserver.create.pools.in.pod",
            "false",
            "Should we automatically add XenServers into pools that are inside a Pod",
            null),
    CloudIdentifier("Hidden", ManagementServer.class, String.class, "cloud.identifier", null, "A unique identifier for the cloud.", null),
    SSOKey("Secure", ManagementServer.class, String.class, "security.singlesignon.key", null, "A Single Sign-On key used for logging into the cloud", null),
    SSOAuthTolerance(
            "Advanced",
            ManagementServer.class,
            Long.class,
            "security.singlesignon.tolerance.millis",
            "300000",
            "The allowable clock difference in milliseconds between when an SSO login request is made and when it is received.",
            null),
    //NetworkType("Hidden", ManagementServer.class, String.class, "network.type", "vlan", "The type of network that this deployment will use.", "vlan,direct"),
    RouterRamSize("Hidden", NetworkOrchestrationService.class, Integer.class, "router.ram.size", "256", "Default RAM for router VM (in MB).", null),

    DefaultPageSize("Advanced", ManagementServer.class, Long.class, "default.page.size", "500", "Default page size for API list* commands", null),

    TaskCleanupRetryInterval(
            "Advanced",
            ManagementServer.class,
            Integer.class,
            "task.cleanup.retry.interval",
            "600",
            "Time (in seconds) to wait before retrying cleanup of tasks if the cleanup failed previously.  0 means to never retry.",
            "Seconds"),

    // Account Default Limits
    DefaultMaxAccountUserVms(
            "Account Defaults",
            ManagementServer.class,
            Long.class,
            "max.account.user.vms",
            "20",
            "The default maximum number of user VMs that can be deployed for an account",
            null),
    DefaultMaxAccountPublicIPs(
            "Account Defaults",
            ManagementServer.class,
            Long.class,
            "max.account.public.ips",
            "20",
            "The default maximum number of public IPs that can be consumed by an account",
            null),
    DefaultMaxAccountTemplates(
            "Account Defaults",
            ManagementServer.class,
            Long.class,
            "max.account.templates",
            "20",
            "The default maximum number of templates that can be deployed for an account",
            null),
    DefaultMaxAccountSnapshots(
            "Account Defaults",
            ManagementServer.class,
            Long.class,
            "max.account.snapshots",
            "20",
            "The default maximum number of snapshots that can be created for an account",
            null),
    DefaultMaxAccountVolumes(
            "Account Defaults",
            ManagementServer.class,
            Long.class,
            "max.account.volumes",
            "20",
            "The default maximum number of volumes that can be created for an account",
            null),
    DefaultMaxAccountNetworks(
            "Account Defaults",
            ManagementServer.class,
            Long.class,
            "max.account.networks",
            "20",
            "The default maximum number of networks that can be created for an account",
            null),
    DefaultMaxAccountVpcs(
            "Account Defaults",
            ManagementServer.class,
            Long.class,
            "max.account.vpcs",
            "20",
            "The default maximum number of vpcs that can be created for an account",
            null),
    DefaultMaxAccountCpus(
            "Account Defaults",
            ManagementServer.class,
            Long.class,
            "max.account.cpus",
            "40",
            "The default maximum number of cpu cores that can be used for an account",
            null),
    DefaultMaxAccountMemory(
            "Account Defaults",
            ManagementServer.class,
            Long.class,
            "max.account.memory",
            "40960",
            "The default maximum memory (in MB) that can be used for an account",
            null),
    DefaultMaxAccountPrimaryStorage(
            "Account Defaults",
            ManagementServer.class,
            Long.class,
            "max.account.primary.storage",
            "200",
            "The default maximum primary storage space (in GiB) that can be used for an account",
            null),
    DefaultMaxAccountSecondaryStorage(
            "Account Defaults",
            ManagementServer.class,
            Long.class,
            "max.account.secondary.storage",
            "400",
            "The default maximum secondary storage space (in GiB) that can be used for an account",
            null),

    //disabling lb as cluster sync does not work with distributed cluster
    SubDomainNetworkAccess(
            "Advanced",
            NetworkOrchestrationService.class,
            Boolean.class,
            "allow.subdomain.network.access",
            "true",
            "Allow subdomains to use networks dedicated to their parent domain(s)",
            null),
    DnsBasicZoneUpdates(
            "Advanced",
            NetworkOrchestrationService.class,
            String.class,
            "network.dns.basiczone.updates",
            "all",
            "This parameter can take 2 values: all (default) and pod. It defines if DHCP/DNS requests have to be send to all dhcp servers in cloudstack, or only to the one in the same pod",
            "all,pod"),

    ClusterMessageTimeOutSeconds(
            "Advanced",
            ManagementServer.class,
            Integer.class,
            "cluster.message.timeout.seconds",
            "300",
            "Time (in seconds) to wait before a inter-management server message post times out.",
            null),
    AgentLoadThreshold(
            "Advanced",
            ManagementServer.class,
            Float.class,
            "agent.load.threshold",
            "0.7",
            "Percentage (as a value between 0 and 1) of connected agents after which agent load balancing will start happening",
            null),

    DefaultMaxDomainUserVms("Domain Defaults", ManagementServer.class, Long.class, "max.domain.user.vms", "40", "The default maximum number of user VMs that can be deployed for a domain", null),
    DefaultMaxDomainPublicIPs("Domain Defaults", ManagementServer.class, Long.class, "max.domain.public.ips", "40", "The default maximum number of public IPs that can be consumed by a domain", null),
    DefaultMaxDomainTemplates("Domain Defaults", ManagementServer.class, Long.class, "max.domain.templates", "40", "The default maximum number of templates that can be deployed for a domain", null),
    DefaultMaxDomainSnapshots("Domain Defaults", ManagementServer.class, Long.class, "max.domain.snapshots", "40", "The default maximum number of snapshots that can be created for a domain", null),
    DefaultMaxDomainVolumes("Domain Defaults", ManagementServer.class, Long.class, "max.domain.volumes", "40", "The default maximum number of volumes that can be created for a domain", null),
    DefaultMaxDomainNetworks("Domain Defaults", ManagementServer.class, Long.class, "max.domain.networks", "40", "The default maximum number of networks that can be created for a domain", null),
    DefaultMaxDomainVpcs("Domain Defaults", ManagementServer.class, Long.class, "max.domain.vpcs", "40", "The default maximum number of vpcs that can be created for a domain", null),
    DefaultMaxDomainCpus("Domain Defaults", ManagementServer.class, Long.class, "max.domain.cpus", "80", "The default maximum number of cpu cores that can be used for a domain", null),
    DefaultMaxDomainMemory("Domain Defaults", ManagementServer.class, Long.class, "max.domain.memory", "81920", "The default maximum memory (in MB) that can be used for a domain", null),
    DefaultMaxDomainPrimaryStorage("Domain Defaults", ManagementServer.class, Long.class, "max.domain.primary.storage", "400", "The default maximum primary storage space (in GiB) that can be used for a domain", null),
    DefaultMaxDomainSecondaryStorage("Domain Defaults", ManagementServer.class, Long.class, "max.domain.secondary.storage", "800", "The default maximum secondary storage space (in GiB) that can be used for a domain", null),

    DefaultMaxProjectUserVms(
            "Project Defaults",
            ManagementServer.class,
            Long.class,
            "max.project.user.vms",
            "20",
            "The default maximum number of user VMs that can be deployed for a project",
            null),
    DefaultMaxProjectPublicIPs(
            "Project Defaults",
            ManagementServer.class,
            Long.class,
            "max.project.public.ips",
            "20",
            "The default maximum number of public IPs that can be consumed by a project",
            null),
    DefaultMaxProjectTemplates(
            "Project Defaults",
            ManagementServer.class,
            Long.class,
            "max.project.templates",
            "20",
            "The default maximum number of templates that can be deployed for a project",
            null),
    DefaultMaxProjectSnapshots(
            "Project Defaults",
            ManagementServer.class,
            Long.class,
            "max.project.snapshots",
            "20",
            "The default maximum number of snapshots that can be created for a project",
            null),
    DefaultMaxProjectVolumes(
            "Project Defaults",
            ManagementServer.class,
            Long.class,
            "max.project.volumes",
            "20",
            "The default maximum number of volumes that can be created for a project",
            null),
    DefaultMaxProjectNetworks(
            "Project Defaults",
            ManagementServer.class,
            Long.class,
            "max.project.networks",
            "20",
            "The default maximum number of networks that can be created for a project",
            null),
    DefaultMaxProjectVpcs(
            "Project Defaults",
            ManagementServer.class,
            Long.class,
            "max.project.vpcs",
            "20",
            "The default maximum number of vpcs that can be created for a project",
            null),
    DefaultMaxProjectCpus(
            "Project Defaults",
            ManagementServer.class,
            Long.class,
            "max.project.cpus",
            "40",
            "The default maximum number of cpu cores that can be used for a project",
            null),
    DefaultMaxProjectMemory(
            "Project Defaults",
            ManagementServer.class,
            Long.class,
            "max.project.memory",
            "40960",
            "The default maximum memory (in MB) that can be used for a project",
            null),
    DefaultMaxProjectPrimaryStorage(
            "Project Defaults",
            ManagementServer.class,
            Long.class,
            "max.project.primary.storage",
            "200",
            "The default maximum primary storage space (in GiB) that can be used for an project",
            null),
    DefaultMaxProjectSecondaryStorage(
            "Project Defaults",
            ManagementServer.class,
            Long.class,
            "max.project.secondary.storage",
            "400",
            "The default maximum secondary storage space (in GiB) that can be used for an project",
            null),

    ProjectInviteRequired(
            "Project Defaults",
            ManagementServer.class,
            Boolean.class,
            "project.invite.required",
            "false",
            "If invitation confirmation is required when add account to project. Default value is false",
            null),
    ProjectInvitationExpirationTime(
            "Project Defaults",
            ManagementServer.class,
            Long.class,
            "project.invite.timeout",
            "86400",
            "Invitation expiration time (in seconds). Default is 1 day - 86400 seconds",
            null),
    AllowUserToCreateProject(
            "Project Defaults",
            ManagementServer.class,
            Long.class,
            "allow.user.create.projects",
            "true",
            "If regular user can create a project; true by default",
            null),

    ProjectEmailSender(
            "Project Defaults",
            ManagementServer.class,
            String.class,
            "project.email.sender",
            null,
            "Sender of project invitation email (will be in the From header of the email)",
            null),
    ProjectSMTPHost(
            "Project Defaults",
            ManagementServer.class,
            String.class,
            "project.smtp.host",
            null,
            "SMTP hostname used for sending out email project invitations",
            null),
    ProjectSMTPPassword(
            "Secure",
            ManagementServer.class,
            String.class,
            "project.smtp.password",
            null,
            "Password for SMTP authentication (applies only if project.smtp.useAuth is true)",
            null),
    ProjectSMTPPort("Project Defaults", ManagementServer.class, Integer.class, "project.smtp.port", "465", "Port the SMTP server is listening on", null),
    ProjectSMTPUseAuth(
            "Project Defaults",
            ManagementServer.class,
            String.class,
            "project.smtp.useAuth",
            null,
            "If true, use SMTP authentication when sending emails",
            null),
    ProjectSMTPUsername(
            "Project Defaults",
            ManagementServer.class,
            String.class,
            "project.smtp.username",
            null,
            "Username for SMTP authentication (applies only if project.smtp.useAuth is true)",
            null),

    DefaultExternalLoadBalancerCapacity(
            "Advanced",
            ManagementServer.class,
            String.class,
            "external.lb.default.capacity",
            "50",
            "default number of networks permitted per external load balancer device",
            null),
    DefaultExternalFirewallCapacity(
            "Advanced",
            ManagementServer.class,
            String.class,
            "external.firewall.default.capacity",
            "50",
            "default number of networks permitted per external load firewall device",
            null),
    EIPWithMultipleNetScalersEnabled(
            "Advanced",
            ManagementServer.class,
            Boolean.class,
            "eip.use.multiple.netscalers",
            "false",
            "Should be set to true, if there will be multiple NetScaler devices providing EIP service in a zone",
            null),
    ConsoleProxyServiceOffering(
            "Advanced",
            ManagementServer.class,
            String.class,
            "consoleproxy.service.offering",
            null,
            "Uuid of the service offering used by console proxy; if NULL - system offering will be used",
            null),
    SecondaryStorageServiceOffering(
            "Advanced",
            ManagementServer.class,
            String.class,
            "secstorage.service.offering",
            null,
            "Uuid of the service offering used by secondary storage; if NULL - system offering will be used",
            null),
    HaTag("Advanced", ManagementServer.class, String.class, "ha.tag", null, "HA tag defining that the host marked with this tag can be used for HA purposes only", null),
    ImplicitHostTags(
            "Advanced",
            ManagementServer.class,
            String.class,
            "implicit.host.tags",
            "GPU",
            "Tag hosts at the time of host disovery based on the host properties/capabilities",
            null),
    VpcCleanupInterval(
            "Advanced",
            ManagementServer.class,
            Integer.class,
            "vpc.cleanup.interval",
            "3600",
            "The interval (in seconds) between cleanup for Inactive VPCs",
            null),
    VpcMaxNetworks("Advanced", ManagementServer.class, Integer.class, "vpc.max.networks", "3", "Maximum number of networks per vpc", null),
    DetailBatchQuerySize("Advanced", ManagementServer.class, Integer.class, "detail.batch.query.size", "2000", "Default entity detail batch query size for listing", null),
    NetworkIPv6SearchRetryMax(
            "Network",
            ManagementServer.class,
            Integer.class,
            "network.ipv6.search.retry.max",
            "10000",
            "The maximum number of retrying times to search for an available IPv6 address in the table",
            null),

    BaremetalInternalStorageServer(
            "Advanced",
            ManagementServer.class,
            String.class,
            "baremetal.internal.storage.server.ip",
            null,
            "the ip address of server that stores kickstart file, kernel, initrd, ISO for advanced networking baremetal provisioning",
            null),
    BaremetalProvisionDoneNotificationEnabled(
            "Advanced",
            ManagementServer.class,
            Boolean.class,
            "baremetal.provision.done.notification.enabled",
            "true",
            "whether to enable baremetal provison done notification",
            null),
    BaremetalProvisionDoneNotificationTimeout(
            "Advanced",
            ManagementServer.class,
            Integer.class,
            "baremetal.provision.done.notification.timeout",
            "1800",
            "the max time to wait before treating a baremetal provision as failure if no provision done notification is not received, in secs",
            null),
    BaremetalProvisionDoneNotificationPort(
            "Advanced",
            ManagementServer.class,
            Integer.class,
            "baremetal.provision.done.notification.port",
            "8080",
            "the port that listens baremetal provision done notification. Should be the same to port management server listening on for now. Please change it to management server port if it's not default 8080",
            null),
    ExternalBaremetalSystemUrl(
            "Advanced",
            ManagementServer.class,
            String.class,
            "external.baremetal.system.url",
            null,
            "url of external baremetal system that CloudStack will talk to",
            null),
    ExternalBaremetalResourceClassName(
            "Advanced",
            ManagementServer.class,
            String.class,
            "external.baremetal.resource.classname",
            null,
            "class name for handling external baremetal resource",
            null),
    EnableBaremetalSecurityGroupAgentEcho(
            "Advanced",
            ManagementServer.class,
            Boolean.class,
            "enable.baremetal.securitygroup.agent.echo",
            "false",
            "After starting provision process, periodcially echo security agent installed in the template. Treat provisioning as success only if echo successfully",
            null),
    IntervalToEchoBaremetalSecurityGroupAgent(
            "Advanced",
            ManagementServer.class,
            Integer.class,
            "interval.baremetal.securitygroup.agent.echo",
            "10",
            "Interval to echo baremetal security group agent, in seconds",
            null),
    TimeoutToEchoBaremetalSecurityGroupAgent(
            "Advanced",
            ManagementServer.class,
            Integer.class,
            "timeout.baremetal.securitygroup.agent.echo",
            "3600",
            "Timeout to echo baremetal security group agent, in seconds, the provisioning process will be treated as a failure",
            null),

    BaremetalIpmiLanInterface(
            "Advanced",
            ManagementServer.class,
            String.class,
            "baremetal.ipmi.lan.interface",
            "default",
            "option specified in -I option of impitool. candidates are: open/bmc/lipmi/lan/lanplus/free/imb, see ipmitool man page for details. default valule 'default' means using default option of ipmitool",
            null),

    BaremetalIpmiRetryTimes("Advanced",
            ManagementServer.class,
            String.class,
            "baremetal.ipmi.fail.retry",
            "5",
            "ipmi interface will be temporary out of order after power opertions(e.g. cycle, on), it leads following commands fail immediately. The value specifies retry times before accounting it as real failure",
            null),

    ApiLimitEnabled("Advanced", ManagementServer.class, Boolean.class, "api.throttling.enabled", "false", "Enable/disable Api rate limit", null),
    ApiLimitInterval("Advanced", ManagementServer.class, Integer.class, "api.throttling.interval", "1", "Time interval (in seconds) to reset API count", null),
    ApiLimitMax("Advanced", ManagementServer.class, Integer.class, "api.throttling.max", "25", "Max allowed number of APIs within fixed interval", null),
    ApiLimitCacheSize("Advanced", ManagementServer.class, Integer.class, "api.throttling.cachesize", "50000", "Account based API count cache size", null),

    // object store
    S3EnableRRS("Advanced", ManagementServer.class, Boolean.class, "s3.rrs.enabled", "false", "enable s3 reduced redundancy storage", null),
    S3MaxSingleUploadSize(
            "Advanced",
            ManagementServer.class,
            Integer.class,
            "s3.singleupload.max.size",
            "5",
            "The maximum size limit for S3 single part upload API(in GB). If it is set to 0, then it means always use multi-part upload to upload object to S3. "
                    + "If it is set to -1, then it means always use single-part upload to upload object to S3. ",
                    null),

    // VMSnapshots
    VMSnapshotMax("Advanced", VMSnapshotManager.class, Integer.class, "vmsnapshot.max", "10", "Maximum vm snapshots for a vm", null),
    VMSnapshotCreateWait("Advanced", VMSnapshotManager.class, Integer.class, "vmsnapshot.create.wait", "1800", "In second, timeout for create vm snapshot", null),

    CloudDnsName("Advanced", ManagementServer.class, String.class, "cloud.dns.name", null, "DNS name of the cloud for the GSLB service", null),
    InternalLbVmServiceOfferingId(
            "Advanced",
            ManagementServer.class,
            String.class,
            "internallbvm.service.offering",
            null,
            "Uuid of the service offering used by internal lb vm; if NULL - default system internal lb offering will be used",
            null),
    ExecuteInSequenceNetworkElementCommands(
            "Advanced",
            NetworkOrchestrationService.class,
            Boolean.class,
            "execute.in.sequence.network.element.commands",
            "false",
            "If set to true, DhcpEntryCommand, SavePasswordCommand, VmDataCommand will be synchronized on the agent side."
                    + " If set to false, these commands become asynchronous. Default value is false.",
                    null),

    UCSSyncBladeInterval(
            "Advanced",
            ManagementServer.class,
            Integer.class,
            "ucs.sync.blade.interval",
            "3600",
            "the interval cloudstack sync with UCS manager for available blades in case user remove blades from chassis without notifying CloudStack",
            null),

    RedundantRouterVrrpInterval(
            "Advanced",
            NetworkOrchestrationService.class,
            Integer.class,
            "router.redundant.vrrp.interval",
            "1",
            "seconds between VRRP broadcast. It would 3 times broadcast fail to trigger fail-over mechanism of redundant router",
            null),

    RouterAggregationCommandEachTimeout(
            "Advanced",
            NetworkOrchestrationService.class,
            Integer.class,
            "router.aggregation.command.each.timeout",
            "600",
            "timeout in seconds for each Virtual Router command being aggregated. The final aggregation command timeout would be determined by this timeout * commands counts ",
            null),

    ManagementServerVendor("Advanced", ManagementServer.class, String.class, "mgt.server.vendor", "ACS", "the vendor of management server", null),
    PublishActionEvent("Advanced", ManagementServer.class, Boolean.class, "publish.action.events", "true", "enable or disable publishing of action events on the event bus", null),
    PublishAlertEvent("Advanced", ManagementServer.class, Boolean.class, "publish.alert.events", "true", "enable or disable publishing of alert events on the event bus", null),
    PublishResourceStateEvent("Advanced", ManagementServer.class, Boolean.class, "publish.resource.state.events", "true", "enable or disable publishing of alert events on the event bus", null),
    PublishUsageEvent("Advanced", ManagementServer.class, Boolean.class, "publish.usage.events", "true", "enable or disable publishing of usage events on the event bus", null),
    PublishAsynJobEvent("Advanced", ManagementServer.class, Boolean.class, "publish.async.job.events", "true", "enable or disable publishing of usage events on the event bus", null),

    // StatsCollector
    StatsOutPutGraphiteHost("Advanced", ManagementServer.class, String.class, "stats.output.uri", "", "URI to additionally send StatsCollector statistics to", null),

    SSVMPSK("Hidden", ManagementServer.class, String.class, "upload.post.secret.key", "", "PSK with SSVM", null);

    private final String _category;
    private final Class<?> _componentClass;
    private final Class<?> _type;
    private final String _name;
    private final String _defaultValue;
    private final String _description;
    private final String _range;
    private final String _scope; // Parameter can be at different levels (Zone/cluster/pool/account), by default every parameter is at global

    private static final HashMap<String, List<Config>> s_scopeLevelConfigsMap = new HashMap<String, List<Config>>();
    static {
        s_scopeLevelConfigsMap.put(ConfigKey.Scope.Zone.toString(), new ArrayList<Config>());
        s_scopeLevelConfigsMap.put(ConfigKey.Scope.Cluster.toString(), new ArrayList<Config>());
        s_scopeLevelConfigsMap.put(ConfigKey.Scope.StoragePool.toString(), new ArrayList<Config>());
        s_scopeLevelConfigsMap.put(ConfigKey.Scope.Account.toString(), new ArrayList<Config>());
        s_scopeLevelConfigsMap.put(ConfigKey.Scope.Global.toString(), new ArrayList<Config>());

        for (Config c : Config.values()) {
            //Creating group of parameters per each level (zone/cluster/pool/account)
            StringTokenizer tokens = new StringTokenizer(c.getScope(), ",");
            while (tokens.hasMoreTokens()) {
                String scope = tokens.nextToken().trim();
                List<Config> currentConfigs = s_scopeLevelConfigsMap.get(scope);
                currentConfigs.add(c);
                s_scopeLevelConfigsMap.put(scope, currentConfigs);
            }
        }
    }

    private static final HashMap<String, List<Config>> Configs = new HashMap<String, List<Config>>();
    static {
        // Add categories
        Configs.put("Alert", new ArrayList<Config>());
        Configs.put("Storage", new ArrayList<Config>());
        Configs.put("Snapshots", new ArrayList<Config>());
        Configs.put("Network", new ArrayList<Config>());
        Configs.put("Usage", new ArrayList<Config>());
        Configs.put("Console Proxy", new ArrayList<Config>());
        Configs.put("Advanced", new ArrayList<Config>());
        Configs.put("Usage", new ArrayList<Config>());
        Configs.put("Developer", new ArrayList<Config>());
        Configs.put("Hidden", new ArrayList<Config>());
        Configs.put("Account Defaults", new ArrayList<Config>());
        Configs.put("Domain Defaults", new ArrayList<Config>());
        Configs.put("Project Defaults", new ArrayList<Config>());
        Configs.put("Secure", new ArrayList<Config>());

        // Add values into HashMap
        for (Config c : Config.values()) {
            String category = c.getCategory();
            List<Config> currentConfigs = Configs.get(category);
            currentConfigs.add(c);
            Configs.put(category, currentConfigs);
        }
    }

    private Config(String category, Class<?> componentClass, Class<?> type, String name, String defaultValue, String description, String range) {
        _category = category;
        _componentClass = componentClass;
        _type = type;
        _name = name;
        _defaultValue = defaultValue;
        _description = description;
        _range = range;
        _scope = ConfigKey.Scope.Global.toString();
    }

    public String getCategory() {
        return _category;
    }

    public String key() {
        return _name;
    }

    public String getDescription() {
        return _description;
    }

    public String getDefaultValue() {
        return _defaultValue;
    }

    public Class<?> getType() {
        return _type;
    }

    public String getScope() {
        return _scope;
    }

    public String getComponent() {
        if (_componentClass == ManagementServer.class) {
            return "management-server";
        } else if (_componentClass == AgentManager.class) {
            return "AgentManager";
        } else if (_componentClass == UserVmManager.class) {
            return "UserVmManager";
        } else if (_componentClass == HighAvailabilityManager.class) {
            return "HighAvailabilityManager";
        } else if (_componentClass == StoragePoolAllocator.class) {
            return "StorageAllocator";
        } else if (_componentClass == NetworkOrchestrationService.class) {
            return "NetworkManager";
        } else if (_componentClass == StorageManager.class) {
            return "StorageManager";
        } else if (_componentClass == TemplateManager.class) {
            return "TemplateManager";
        } else if (_componentClass == VpcManager.class) {
            return "VpcManager";
        } else if (_componentClass == SnapshotManager.class) {
            return "SnapshotManager";
        } else if (_componentClass == VMSnapshotManager.class) {
            return "VMSnapshotManager";
        } else {
            return "none";
        }
    }

    public String getRange() {
        return _range;
    }

    @Override
    public String toString() {
        return _name;
    }

    public static List<Config> getConfigs(String category) {
        return Configs.get(category);
    }

    public static Config getConfig(String name) {
        List<String> categories = getCategories();
        for (String category : categories) {
            List<Config> currentList = getConfigs(category);
            for (Config c : currentList) {
                if (c.key().equals(name)) {
                    return c;
                }
            }
        }

        return null;
    }

    public static List<String> getCategories() {
        Object[] keys = Configs.keySet().toArray();
        List<String> categories = new ArrayList<String>();
        for (Object key : keys) {
            categories.add((String)key);
        }
        return categories;
    }
}
