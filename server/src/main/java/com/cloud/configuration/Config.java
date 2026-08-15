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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.engine.subsystem.api.storage.StoragePoolAllocator;
import org.apache.cloudstack.framework.config.ConfigKey;

import com.cloud.agent.AgentManager;
import com.cloud.ha.HighAvailabilityManager;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.vpc.VpcManager;
import com.cloud.server.ManagementServer;
import com.cloud.storage.StorageManager;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.template.TemplateManager;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.snapshot.VMSnapshotManager;

/**
 * @deprecated use the more dynamic ConfigKey
 */
@Deprecated
public enum Config {

    // Alert

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

    // Storage

    CreatePrivateTemplateFromVolumeWait(
            "Storage",
            UserVmManager.class,
            Integer.class,
            "create.private.template.from.volume.wait",
            "10800",
            "In second, timeout for CreatePrivateTemplateFromVolumeCommand",
            null),
    HAStorageMigration(
            "Storage",
            ManagementServer.class,
            Boolean.class,
            "enable.ha.storage.migration",
            "true",
            "Enable/disable storage migration across primary storage during HA",
            null),

    // Network
    //MulticastThrottlingRate("Network", ManagementServer.class, Integer.class, "multicast.throttling.rate", "10", "Default multicast rate in megabits per second allowed.", null),
    DirectNetworkNoDefaultRoute(
            "Network",
            ManagementServer.class,
            Boolean.class,
            "direct.network.no.default.route",
            "false",
            "Direct Network Dhcp Server should not send a default route",
            "true/false"),

    GuestOSNeedGatewayOnNonDefaultNetwork(
            "Network",
            NetworkOrchestrationService.class,
            String.class,
            "network.dhcp.nondefaultnetwork.setgateway.guestos",
            "Windows",
            "The guest OS's name start with this fields would result in DHCP server response gateway information even when the network it's on is not default network. Names are separated by comma.",
            null,
            ConfigKey.Kind.CSV,
            null),

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
    HostStatsInterval(
            "Advanced",
            ManagementServer.class,
            Integer.class,
            "host.stats.interval",
            "60000",
            "The interval (in milliseconds) when host stats are retrieved from agents.",
            null),
    HostRetry("Advanced", AgentManager.class, Integer.class, "host.retry", "2", "Number of times to retry hosts for creating a volume", null),
    ScaleRetry("Advanced", ManagementServer.class, Integer.class, "scale.retry", "2", "Number of times to retry scaling up the vm", null),
    UpdateWait("Advanced", AgentManager.class, Integer.class, "update.wait", "600", "Time to wait (in seconds) before alerting on a updating agent", null),
    LinkLocalIpNums("Advanced", ManagementServer.class, Integer.class, "linkLocalIp.nums", "10", "The number of link local ip that needed by domR(in power of 2)", null),
    HypervisorList(
            "Advanced",
            ManagementServer.class,
            String.class,
            "hypervisor.list",
            HypervisorType.KVM + "," + HypervisorType.VMware + "," + HypervisorType.XenServer + "," + HypervisorType.Hyperv + "," +
                    HypervisorType.BareMetal + "," + HypervisorType.Ovm + "," + HypervisorType.LXC + "," + HypervisorType.Ovm3 + "," + HypervisorType.External,
                    "The list of hypervisors that this deployment will use.",
            "hypervisorList",
            ConfigKey.Kind.CSV,
            null),
    ManagementNetwork("Advanced", ManagementServer.class, String.class, "management.network.cidr", null, "The cidr of management server network", null),
    EventPurgeDelay(
            "Advanced",
            ManagementServer.class,
            Integer.class,
            "event.purge.delay",
            "15",
            "Events older than specified number days will be purged. Set this value to 0 to never delete events",
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
    CheckPodCIDRs(
            "Advanced",
            ManagementServer.class,
            String.class,
            "check.pod.cidrs",
            "true",
            "If true, different pods must belong to different CIDR subnets.",
            "true,false"),
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
    VolumeStatsInterval("Advanced", ManagementServer.class, Integer.class, "volume.stats.interval", "60000", "Interval (in milliseconds) to report volume statistics.", null),
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
            "The host capacity type (CPU, RAM, COMBINED) is used by deployment planner to order clusters during VM resource allocation",
            "CPU,RAM,COMBINED"),

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
    VmDeploymentPlanner(
            "Advanced",
            ManagementServer.class,
            String.class,
            "vm.deployment.planner",
            "FirstFitPlanner",
            "'FirstFitPlanner', 'UserDispersingPlanner', 'UserConcentratedPodPlanner': DeploymentPlanner heuristic that will be used for VM deployment.",
            null,
            ConfigKey.Kind.Select,
            "FirstFitPlanner,UserDispersingPlanner,UserConcentratedPodPlanner"),
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
            "512",
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
            "Incorrect login attempts allowed before the user is disabled (when value > 0). If value <=0 users are not disabled after failed login attempts",
            null),
    // Ovm3
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
    XenServerPVdriverVersion(
            "Advanced",
            ManagementServer.class,
            String.class,
            "xenserver.pvdriver.version",
            "xenserver61",
            "default Xen PV driver version for registered template, valid value:xenserver56,xenserver61 ",
            "xenserver56,xenserver61",
            ConfigKey.Kind.Select,
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
            null,
            ConfigKey.Kind.Select,
            "scsi,ide,osdefault"),
    VmwareSystemVmNicDeviceType(
            "Advanced",
            ManagementServer.class,
            String.class,
            "vmware.systemvm.nic.device.type",
            "E1000",
            "Specify the default network device type for system VMs, valid values are E1000, PCNet32, Vmxnet2, Vmxnet3",
            null,
            ConfigKey.Kind.Select,
            "E1000,PCNet32,Vmxnet2,Vmxnet3"),
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

    // Hidden
    CreatePoolsInPod(
            "Hidden",
            ManagementServer.class,
            Boolean.class,
            "xenserver.create.pools.in.pod",
            "false",
            "Should we automatically add XenServers into pools that are inside a Pod",
            null),
    SSOAuthTolerance(
            "Advanced",
            ManagementServer.class,
            Long.class,
            "security.singlesignon.tolerance.millis",
            "300000",
            "The allowable clock difference in milliseconds between when an SSO login request is made and when it is received.",
            null),
    //NetworkType("Hidden", ManagementServer.class, String.class, "network.type", "vlan", "The type of network that this deployment will use.", "vlan,direct"),

    DefaultPageSize("Advanced", ManagementServer.class, Long.class, "default.page.size", "500", "Default page size for API list* commands", null),

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
    HaTag("Advanced", ManagementServer.class, String.class, "ha.tag", null, "HA tag defining that the host marked with this tag can be used for HA purposes only", null),
    ImplicitHostTags(
            "Advanced",
            ManagementServer.class,
            String.class,
            "implicit.host.tags",
            "GPU",
            "Tag hosts at the time of host discovery based on the host properties/capabilities",
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
            "option specified in -I option of impitool. candidates are: open/bmc/lipmi/lan/lanplus/free/imb, see ipmitool man page for details. default value 'default' means using default option of ipmitool",
            null),

    BaremetalIpmiRetryTimes("Advanced",
            ManagementServer.class,
            String.class,
            "baremetal.ipmi.fail.retry",
            "5",
            "ipmi interface will be temporary out of order after power operations(e.g. cycle, on), it leads following commands fail immediately. The value specifies retry times before accounting it as real failure",
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
    UCSSyncBladeInterval(
            "Advanced",
            ManagementServer.class,
            Integer.class,
            "ucs.sync.blade.interval",
            "3600",
            "the interval cloudstack sync with UCS manager for available blades in case user remove blades from chassis without notifying CloudStack",
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
    StatsOutPutGraphiteHost("Advanced", ManagementServer.class, String.class, "stats.output.uri", "", "URI to additionally send StatsCollector statistics to", null);


    private final String _category;
    private final Class<?> _componentClass;
    private final Class<?> _type;
    private final String _name;
    private final String _defaultValue;
    private final String _description;
    private final String _range;
    private final int _scope; // Parameter can be at different levels (Zone/cluster/pool/account), by default every parameter is at global
    private final ConfigKey.Kind _kind;
    private final String _options;

    private static final HashMap<Integer, List<Config>> s_scopeLevelConfigsMap = new HashMap<>();
    static {
        s_scopeLevelConfigsMap.put(ConfigKey.Scope.Zone.getBitValue(), new ArrayList<>());
        s_scopeLevelConfigsMap.put(ConfigKey.Scope.Cluster.getBitValue(), new ArrayList<>());
        s_scopeLevelConfigsMap.put(ConfigKey.Scope.StoragePool.getBitValue(), new ArrayList<>());
        s_scopeLevelConfigsMap.put(ConfigKey.Scope.Account.getBitValue(), new ArrayList<>());
        s_scopeLevelConfigsMap.put(ConfigKey.Scope.Global.getBitValue(), new ArrayList<>());

        for (Config c : Config.values()) {
            //Creating group of parameters per each level (zone/cluster/pool/account)
            List<ConfigKey.Scope> scopes = ConfigKey.Scope.decode(c.getScope());
            for (ConfigKey.Scope scope : scopes) {
                List<Config> currentConfigs = s_scopeLevelConfigsMap.get(scope.getBitValue());
                currentConfigs.add(c);
                s_scopeLevelConfigsMap.put(scope.getBitValue(), currentConfigs);
            }
        }
    }

    private static final HashMap<String, List<Config>> Configs = new HashMap<>();
    static {
        // Add categories
        Configs.put("Account Defaults", new ArrayList<>());
        Configs.put("Advanced", new ArrayList<>());
        Configs.put("Alert", new ArrayList<>());
        Configs.put("Console Proxy", new ArrayList<>());
        Configs.put("Developer", new ArrayList<>());
        Configs.put("Domain Defaults", new ArrayList<>());
        Configs.put("Hidden", new ArrayList<>());
        Configs.put("Network", new ArrayList<>());
        Configs.put("Secure", new ArrayList<>());
        Configs.put("Snapshots", new ArrayList<>());
        Configs.put("Storage", new ArrayList<>());
        Configs.put("Usage", new ArrayList<>());
        Configs.put("Project Defaults", new ArrayList<>());

        // Add values into HashMap
        for (Config c : Config.values()) {
            String category = c.getCategory();
            List<Config> currentConfigs = Configs.get(category);
            currentConfigs.add(c);
            Configs.put(category, currentConfigs);
        }
    }

    Config(String category, Class<?> componentClass, Class<?> type, String name, String defaultValue, String description, String range) {
        this(category, componentClass, type, name, defaultValue, description, range, null, null);
    }

    Config(String category, Class<?> componentClass, Class<?> type, String name, String defaultValue, String description, String range, ConfigKey.Kind kind, String options) {
        _category = category;
        _componentClass = componentClass;
        _type = type;
        _name = name;
        _defaultValue = defaultValue;
        _description = description;
        _range = range;
        _scope = ConfigKey.Scope.Global.getBitValue();
        _kind = kind;
        _options = options;
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

    public int getScope() {
        return _scope;
    }

    public String getKind() {
        if (_kind == null) {
                return null;
        }
        return _kind.toString();
    }

    public String getOptions() {
        return _options;
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
        List<String> categories = new ArrayList<>();
        for (Object key : keys) {
            categories.add((String)key);
        }
        return categories;
    }
}
