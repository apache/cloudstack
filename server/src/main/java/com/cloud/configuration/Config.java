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
    HostRetry("Advanced", AgentManager.class, Integer.class, "host.retry", "2", "Number of times to retry hosts for creating a volume", null),
    UpdateWait("Advanced", AgentManager.class, Integer.class, "update.wait", "600", "Time to wait (in seconds) before alerting on a updating agent", null),
    CheckPodCIDRs(
            "Advanced",
            ManagementServer.class,
            String.class,
            "check.pod.cidrs",
            "true",
            "If true, different pods must belong to different CIDR subnets.",
            "true,false"),
    VmTransitionWaitInterval(
            "Advanced",
            ManagementServer.class,
            Integer.class,
            "vm.tranisition.wait.interval",
            "3600",
            "Time (in seconds) to wait before taking over a VM in transition state",
            null),
    EnableEC2API("Advanced", ManagementServer.class, Boolean.class, "enable.ec2.api", "false", "enable EC2 API on CloudStack", null),
    EnableS3API("Advanced", ManagementServer.class, Boolean.class, "enable.s3.api", "false", "enable Amazon S3 API on CloudStack", null),
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
    //NetworkType("Hidden", ManagementServer.class, String.class, "network.type", "vlan", "The type of network that this deployment will use.", "vlan,direct"),

    DefaultPageSize("Advanced", ManagementServer.class, Long.class, "default.page.size", "500", "Default page size for API list* commands", null),

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

    ManagementServerVendor("Advanced", ManagementServer.class, String.class, "mgt.server.vendor", "ACS", "the vendor of management server", null),
    PublishAsynJobEvent("Advanced", ManagementServer.class, Boolean.class, "publish.async.job.events", "true", "enable or disable publishing of usage events on the event bus", null);


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
