/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.cloud.agent.AgentManager;
import com.cloud.ha.HighAvailabilityManager;
import com.cloud.network.NetworkManager;
import com.cloud.server.ManagementServer;
import com.cloud.storage.StorageManager;
import com.cloud.storage.allocator.StoragePoolAllocator;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.vm.UserVmManager;

public enum Config {
	
	// Alert
	
	AlertEmailAddresses("Alert", ManagementServer.class, String.class, "alert.email.addresses", null, "Comma separated list of email addresses used for sending alerts.", null),
	AlertEmailSender("Alert", ManagementServer.class, String.class, "alert.email.sender", null, "Sender of alert email (will be in the From header of the email).", null),
	AlertSMTPHost("Alert", ManagementServer.class, String.class, "alert.smtp.host", null, "SMTP hostname used for sending out email alerts.", null),
	AlertSMTPPassword("Alert", ManagementServer.class, String.class, "alert.smtp.password", null, "Password for SMTP authentication (applies only if alert.smtp.useAuth is true).", null),
	AlertSMTPPort("Alert", ManagementServer.class, Integer.class, "alert.smtp.port", "465", "Port the SMTP server is listening on.", null),
	AlertSMTPUseAuth("Alert", ManagementServer.class, String.class, "alert.smtp.useAuth", null, "If true, use SMTP authentication when sending emails.", null),
	AlertSMTPUsername("Alert", ManagementServer.class, String.class, "alert.smtp.username", null, "Username for SMTP authentication (applies only if alert.smtp.useAuth is true).", null),
	AlertWait("Alert", AgentManager.class, Integer.class, "alert.wait", null, "Seconds to wait before alerting on a disconnected agent", null),
	
	// Storage
	
	StorageOverprovisioningFactor("Storage", StoragePoolAllocator.class, String.class, "storage.overprovisioning.factor", "2", "Used for storage overprovisioning calculation; available storage will be (actualStorageSize * storage.overprovisioning.factor)", null),
	StorageStatsInterval("Storage", ManagementServer.class, String.class, "storage.stats.interval", "60000", "The interval in milliseconds when storage stats (per host) are retrieved from agents.", null),
	MaxVolumeSize("Storage", ManagementServer.class, Integer.class, "max.volume.size.gb", "2000", "The maximum size for a volume in Gb.", null),
	TotalRetries("Storage", AgentManager.class, Integer.class, "total.retries", "4", "The number of times each command sent to a host should be retried in case of failure.", null),
	
	// Network
	
//	GuestIpNetwork("Network", AgentManager.class, String.class, "guest.ip.network", "10.1.1.1", "The network address of the guest virtual network. Virtual machines will be assigned an IP in this subnet.", "privateip"),
//	GuestNetmask("Network", AgentManager.class, String.class, "guest.netmask", "255.255.255.0", "The netmask of the guest virtual network.", "netmask"),
	MulticastThrottlingRate("Network", ManagementServer.class, Integer.class, "multicast.throttling.rate", "10", "Default multicast rate in megabits per second allowed.", null),
	NetworkThrottlingRate("Network", ManagementServer.class, Integer.class, "network.throttling.rate", "200", "Default data transfer rate in megabits per second allowed.", null),

	
	// Usage
	
	CapacityCheckPeriod("Usage", ManagementServer.class, Integer.class, "capacity.check.period", "300000", "The interval in milliseconds between capacity checks", null),
	CapacitySkipCountingHours("Usage", ManagementServer.class, Integer.class, "capacity.skipcounting.hours", "24", "The interval in hours since VM has stopped to skip counting its allocated CPU/Memory capacity", null),
	StorageAllocatedCapacityThreshold("Usage", ManagementServer.class, Float.class, "storage.allocated.capacity.threshold", "0.85", "Percentage (as a value between 0 and 1) of allocated storage utilization above which alerts will be sent about low storage available.", null),
	StorageCapacityThreshold("Usage", ManagementServer.class, Float.class, "storage.capacity.threshold", "0.85", "Percentage (as a value between 0 and 1) of storage utilization above which alerts will be sent about low storage available.", null),
	CPUCapacityThreshold("Usage", ManagementServer.class, Float.class, "cpu.capacity.threshold", "0.85", "Percentage (as a value between 0 and 1) of cpu utilization above which alerts will be sent about low cpu available.", null),
	MemoryCapacityThreshold("Usage", ManagementServer.class, Float.class, "memory.capacity.threshold", "0.85", "Percentage (as a value between 0 and 1) of memory utilization above which alerts will be sent about low memory available.", null),
	PublicIpCapacityThreshold("Usage", ManagementServer.class, Float.class, "public.ip.capacity.threshold", "0.85", "Percentage (as a value between 0 and 1) of public IP address space utilization above which alerts will be sent.", null),
	PrivateIpCapacityThreshold("Usage", ManagementServer.class, Float.class, "private.ip.capacity.threshold", "0.85", "Percentage (as a value between 0 and 1) of private IP address space utilization above which alerts will be sent.", null),
	
	// Console Proxy
	ConsoleProxyCapacityStandby("Console Proxy", AgentManager.class, String.class, "consoleproxy.capacity.standby", "10", "The minimal number of console proxy viewer sessions that system is able to serve immediately(standby capacity)", null),
	ConsoleProxyCapacityScanInterval("Console Proxy", AgentManager.class, String.class, "consoleproxy.capacityscan.interval", "30000", "The time interval(in millisecond) to scan whether or not system needs more console proxy to ensure minimal standby capacity", null),
	ConsoleProxyCmdPort("Console Proxy", AgentManager.class, Integer.class, "consoleproxy.cmd.port", "8001", "Console proxy command port that is used to communicate with management server", null),
	
	// obselete
	//ConsoleProxyDomPEnable("Console Proxy", ManagementServer.class, Boolean.class, "consoleproxy.domP.enable", null, null, null),
	
	ConsoleProxyLoadscanInterval("Console Proxy", AgentManager.class, String.class, "consoleproxy.loadscan.interval", "10000", "The time interval(in milliseconds) to scan console proxy working-load info", null),
	
	// obselete
	// ConsoleProxyPort("Console Proxy", ManagementServer.class, Integer.class, "consoleproxy.port", null, null, null),
	
	ConsoleProxyRamSize("Console Proxy", AgentManager.class, Integer.class, "consoleproxy.ram.size", "1024", "RAM size (in MB) used to create new console proxy VMs", null),
	ConsoleProxySessionMax("Console Proxy", AgentManager.class, Integer.class, "consoleproxy.session.max", "50", "The max number of viewer sessions console proxy is configured to serve for", null),
	ConsoleProxySessionTimeout("Console Proxy", AgentManager.class, Integer.class, "consoleproxy.session.timeout", "300000", "Timeout(in milliseconds) that console proxy tries to maintain a viewer session before it times out the session for no activity", null),
	
	// ConsoleProxyURLPort("Console Proxy", ManagementServer.class, Integer.class, "consoleproxy.url.port", "80", "Console proxy port for AJAX viewer", null),
	
	// Snapshots
    SnapshotHourlyMax("Snapshots", SnapshotManager.class, String.class, "snapshot.max.hourly", "8", "Maximum hourly snapshots for a volume", null),
    SnapshotDailyMax("Snapshots", SnapshotManager.class, String.class, "snapshot.max.daily", "8", "Maximum dalily snapshots for a volume", null),
    SnapshotWeeklyMax("Snapshots", SnapshotManager.class, String.class, "snapshot.max.weekly", "8", "Maximum hourly snapshots for a volume", null),
    SnapshotMonthlyMax("Snapshots", SnapshotManager.class, String.class, "snapshot.max.monthly", "8", "Maximum hourly snapshots for a volume", null),
    SnapshotPollInterval("Snapshots", SnapshotManager.class, Boolean.class, "snapshot.poll.interval", "300", "The time interval in seconds when the management server polls for snapshots to be scheduled.", null),
    
	// Advanced
    JobExpireMinutes("Advanced", ManagementServer.class, String.class, "job.expire.minutes", "1440", "Time (in minutes) for async-jobs to be kept in system", null),
	
	AccountCleanupInterval("Advanced", ManagementServer.class, Integer.class, "account.cleanup.interval", "86400", "The interval in seconds between cleanup for removed accounts", null),
	AllowPublicUserTemplates("Advanced", ManagementServer.class, Integer.class, "allow.public.user.templates", "true", "If false, users will not be able to create public templates.", null),
	InstanceName("Advanced", AgentManager.class, String.class, "instance.name", "VM", "Name of the deployment instance.", null),
	ExpungeDelay("Advanced", UserVmManager.class, Integer.class, "expunge.delay", "86400", "Determines how long to wait before actually expunging destroyed vm. The default value = the default value of expunge.interval", null),
	ExpungeInterval("Advanced", UserVmManager.class, Integer.class, "expunge.interval", "86400", "The interval to wait before running the expunge thread.", null),
	ExpungeWorkers("Advanced", UserVmManager.class, Integer.class, "expunge.workers",  "1", "Number of workers performing expunge ", null),
	HostStatsInterval("Advanced", ManagementServer.class, Integer.class, "host.stats.interval", "60000", "The interval in milliseconds when host stats are retrieved from agents.", null),
	HostRetry("Advanced", AgentManager.class, Integer.class, "host.retry", "2", "Number of times to retry hosts for creating a volume", null),
	IntegrationAPIPort("Advanced", ManagementServer.class, Integer.class, "integration.api.port", "8096", "Defaul API port", null),
	InvestigateRetryInterval("Advanced", HighAvailabilityManager.class, Integer.class, "investigate.retry.interval", "60", "Time in seconds between VM pings when agent is disconnected", null),
	MigrateRetryInterval("Advanced", HighAvailabilityManager.class, Integer.class, "migrate.retry.interval", "120", "Time in seconds between migration retries", null),
	PingInterval("Advanced", AgentManager.class, Integer.class, "ping.interval", "60", "Ping interval in seconds", null),
	PingTimeout("Advanced", AgentManager.class, Float.class, "ping.timeout", "2.5", "Multiplier to ping.interval before announcing an agent has timed out", null),
	Port("Advanced", AgentManager.class, Integer.class, "port", "8250", "Port to listen on for agent connection.", null),
	RouterRamSize("Advanced", NetworkManager.class, Integer.class, "router.ram.size", "128", "Default RAM for router VM in MB.", null),
	RestartRetryInterval("Advanced", HighAvailabilityManager.class, Integer.class, "restart.retry.interval", "600", "Time in seconds between retries to restart a vm", null),
	RouterCleanupInterval("Advanced", ManagementServer.class, Integer.class, "router.cleanup.interval", "3600", "Time in seconds identifies when to stop router when there are no user vms associated with it", null),
	RouterStatsInterval("Advanced", NetworkManager.class, Integer.class, "router.stats.interval", "300", "Interval to report router statistics.", null),
	RouterTemplateId("Advanced", NetworkManager.class, Long.class, "router.template.id", "1", "Default ID for template.", null),
	StartRetry("Advanced", AgentManager.class, Integer.class, "start.retry", "2", "Number of times to retry create and start commands", null),
	StopRetryInterval("Advanced", HighAvailabilityManager.class, Integer.class, "stop.retry.interval", "600", "Time in seconds between retries to stop or destroy a vm" , null),
	StorageCleanupInterval("Advanced", StorageManager.class, Integer.class, "storage.cleanup.interval", "86400", "The interval to wait before running the storage cleanup thread.", null),
	StorageCleanupEnabled("Advanced", StorageManager.class, Boolean.class, "storage.cleanup.enabled", "true", "Enables/disables the storage cleanup thread.", null),
	UpdateWait("Advanced", AgentManager.class, Integer.class, "update.wait", "600", "Time to wait before alerting on a updating agent", null),
	Wait("Advanced", AgentManager.class, Integer.class, "wait", "1800", "Time to wait for control commands to return", null),
	Workers("Advanced", AgentManager.class, Integer.class, "workers", "5", "Number of worker threads.", null),
	MountParent("Advanced", ManagementServer.class, String.class, "mount.parent", "/var/lib/cloud/mnt", "The mount point on the Management Server for Secondary Storage.", null),
	UpgradeURL("Advanced", ManagementServer.class, String.class, "upgrade.url", "http://example.com:8080/client/agent/update.zip", "The upgrade URL is the URL of the management server that agents will connect to in order to automatically upgrade.", null),
	SystemVMUseLocalStorage("Advanced", ManagementServer.class, Boolean.class, "system.vm.use.local.storage", "false", "Indicates whether to use local storage pools or shared storage pools for system VMs.", null),
	CPUOverprovisioningFactor("Advanced", ManagementServer.class, String.class, "cpu.overprovisioning.factor", "1", "Used for CPU overprovisioning calculation; available CPU will be (actualCpuCapacity * cpu.overprovisioning.factor)", null),
	NetworkType("Advanced", ManagementServer.class, String.class, "network.type", "vnet", "The type of network that this deployment will use.", "vnet,vlan,direct"),
	LinkLocalIpNums("Advanced", ManagementServer.class, Integer.class, "linkLocalIp.nums", "10", "The number of link local ip that needed by domR(in power of 2)", null),
	HypervisorType("Advanced", ManagementServer.class, String.class, "hypervisor.type", "kvm", "The type of hypervisor that this deployment will use.", "kvm,xenserver"),
	ManagementHostIPAdr("Advanced", ManagementServer.class, String.class, "host", "localhost", "The ip address of management server", null),
	UseSecondaryStorageVm("Advanced", ManagementServer.class, Boolean.class, "secondary.storage.vm", "false", "Deploys a VM per zone to manage secondary storage if true, otherwise secondary storage is mounted on management server", null),
	EventPurgeDelay("Advanced", ManagementServer.class, Integer.class, "event.purge.delay", "0", "Events older than specified number days will be purged", null),
    UseLocalStorage("Premium", ManagementServer.class, Boolean.class, "use.local.storage", "false", "Should we use the local storage if it's available?", null),
	SecStorageVmRamSize("Advanced", AgentManager.class, Integer.class, "secstorage.vm.ram.size", null, "RAM size (in MB) used to create new secondary storage vms", null),
	MaxTemplateAndIsoSize("Advanced",  ManagementServer.class, Long.class, "max.template.iso.size", "50", "The maximum size for a downloaded template or ISO (in GB).", null),
	SecStorageAllowedInternalDownloadSites("Advanced", ManagementServer.class, String.class, "secstorage.allowed.internal.sites", null, "Comma separated list of cidrs internal to the datacenter that can host template download servers", null),
	SecStorageEncryptCopy("Advanced", ManagementServer.class, Boolean.class, "secstorage.encrypt.copy", "false", "Use SSL method used to encrypt copy traffic between zones", "true,false"),
	SecStorageSecureCopyCert("Advanced", ManagementServer.class, Boolean.class, "secstorage.ssl.cert.domain", "realhostip.com", "SSL certificate used to encrypt copy traffic between zones", "realhostip.com"),
	DirectAttachNetworkGroupsEnabled("Advanced", ManagementServer.class, Boolean.class, "direct.attach.network.groups.enabled", "false", "Ec2-style distributed firewall for direct-attach VMs", "true,false"),
	DirectAttachNetworkEnabled("Advanced", ManagementServer.class, Boolean.class, "direct.attach.network.externalIpAllocator.enabled", "false", "Direct-attach VMs using external DHCP server", "true,false"),
	DirectAttachNetworkExternalAPIURL("Advanced", ManagementServer.class, String.class, "direct.attach.network.externalIpAllocator.url", null, "Direct-attach VMs using external DHCP server (API url)", null),
	DirectAttachUntaggedVlanEnabled("Advanced", ManagementServer.class, String.class, "direct.attach.untagged.vlan.enabled", "false", "Indicate whether the system supports direct-attached untagged vlan", "true,false"),
	CheckPodCIDRs("Advanced", ManagementServer.class, String.class, "check.pod.cidrs", "true", "If true, different pods must belong to different CIDR subnets.", "true,false"),
	
	// XenServer
    VmAllocationAlgorithm("Advanced", ManagementServer.class, String.class, "vm.allocation.algorithm", "random", "If 'random', hosts within a pod will be randomly considered for VM/volume allocation. If 'firstfit', they will be considered on a first-fit basis.", null),
    XenPublicNetwork("Network", ManagementServer.class, String.class, "xen.public.network.device", null, "[ONLY IF THE PUBLIC NETWORK IS ON A DEDICATED NIC]:The network name label of the physical device dedicated to the public network on a XenServer host", null),
    XenStorageNetwork1("Network", ManagementServer.class, String.class, "xen.storage.network.device1", "cloud-stor1", "Specify when there are storage networks", null),
    XenStorageNetwork2("Network", ManagementServer.class, String.class, "xen.storage.network.device2", "cloud-stor2", "Specify when there are storage networks", null),
    XenPrivateNetwork("Network", ManagementServer.class, String.class, "xen.private.network.device", null, "Specify when the private network name is different", null),
    XenMinVersion("Advanced", ManagementServer.class, String.class, "xen.min.version", "3.3.1", "Minimum Xen version", null),
    XenProductMinVersion("Advanced", ManagementServer.class, String.class, "xen.min.product.version", "0.1.1", "Minimum XenServer version", null),
	XenXapiMinVersion("Advanced", ManagementServer.class, String.class, "xen.min.xapi.version", "1.3", "Minimum Xapi Tool Stack version", null),
    XenMaxVersion("Advanced", ManagementServer.class, String.class, "xen.max.version", "3.4.2", "Maximum Xen version", null),
    XenProductMaxVersion("Advanced", ManagementServer.class, String.class, "xen.max.product.version", "5.6.0", "Maximum XenServer version", null),
    XenXapiMaxVersion("Advanced", ManagementServer.class, String.class, "xen.max.xapi.version", "1.3", "Maximum Xapi Tool Stack version", null),
    XenSetupMultipath("Advanced", ManagementServer.class, String.class, "xen.setup.multipath", "false", "Setup the host to do multipath", null),
    XenBondStorageNic("Advanced", ManagementServer.class, String.class, "xen.bond.storage.nics", null, "Attempt to bond the two networks if found", null),
    XenHeartBeatInterval("Advanced", ManagementServer.class, Integer.class, "xen.heartbeat.interval", "60", "heartbeat to use when implementing XenServer Self Fencing", "any # of seconds"),
    XenPreallocatedLunSizeRange("Advanced", ManagementServer.class, Float.class, "xen.preallocated.lun.size.range", ".05", "percentage to add to disk size when allocating", null),
    XenGuestNetwork("Advanced", ManagementServer.class, String.class, "xen.guest.network.device", null, "Specify when the guest network does not go over the private network", null),
    
	// Premium
	
	UsageExecutionTimezone("Premium", ManagementServer.class, String.class, "usage.execution.timezone", null, "The timezone to use for usage job execution time", null),
	UsageStatsJobAggregationRange("Premium", ManagementServer.class, Integer.class, "usage.stats.job.aggregation.range", "1440", "The range of time for aggregating the user statistics specified in minutes (e.g. 1440 for daily, 60 for hourly.", null),
	UsageStatsJobExecTime("Premium", ManagementServer.class, String.class, "usage.stats.job.exec.time", "00:15", "The time at which the usage statistics aggregation job will run as an HH24:MM time, e.g. 00:30 to run at 12:30am.", null),
    EnableUsageServer("Premium", ManagementServer.class, Boolean.class, "enable.usage.server", "true", "Flag for enabling usage", null),
    
	// Hidden
	
    CreatePoolsInPod("Hidden", ManagementServer.class, Boolean.class, "xen.create.pools.in.pod", "false", "Should we automatically add XenServers into pools that are inside a Pod", null),
    CloudIdentifier("Hidden", ManagementServer.class, String.class, "cloud.identifier", null, "A unique identifier for the cloud.", null),
    SSOKey("Hidden", ManagementServer.class, String.class, "security.singlesignon.key", null, "A Single Sign-On key used for logging into the cloud", null),
    SSOAuthTolerance("Advanced", ManagementServer.class, Long.class, "security.singlesignon.tolerance.millis", "300000", "The allowable clock difference in milliseconds between when an SSO login request is made and when it is received.", null),
    HashKey("Hidden", ManagementServer.class, String.class, "security.hash.key", null, "for generic key-ed hash", null);

	private final String _category;
	private final Class<?> _componentClass;
	private final Class<?> _type;
    private final String _name;
    private final String _defaultValue;
    private final String _description;
    private final String _range;

    private static final HashMap<String, List<Config>> _configs = new HashMap<String, List<Config>>();
    static {
    	// Add categories
    	_configs.put("Alert", new ArrayList<Config>());
    	_configs.put("Storage", new ArrayList<Config>());
    	_configs.put("Snapshots", new ArrayList<Config>());
    	_configs.put("Network", new ArrayList<Config>());
    	_configs.put("Usage", new ArrayList<Config>());
    	_configs.put("Console Proxy", new ArrayList<Config>());
    	_configs.put("Advanced", new ArrayList<Config>());
    	_configs.put("Premium", new ArrayList<Config>());
    	_configs.put("Developer", new ArrayList<Config>());
    	_configs.put("Hidden", new ArrayList<Config>());
    	
    	// Add values into HashMap
        for (Config c : Config.values()) {
        	String category = c.getCategory();
        	List<Config> currentConfigs = _configs.get(category);
        	currentConfigs.add(c);
        	_configs.put(category, currentConfigs);
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

    public Class<?> getComponentClass() {
        return _componentClass;
    }
    
    public String getComponent() {
    	if (_componentClass == ManagementServer.class)
    		return "management-server";
    	else if (_componentClass == AgentManager.class)
    		return "AgentManager";
    	else if (_componentClass == UserVmManager.class)
    		return "UserVmManager";
    	else if (_componentClass == HighAvailabilityManager.class)
    		return "HighAvailabilityManager";
    	else if (_componentClass == StoragePoolAllocator.class)
    		return "StorageAllocator";
    	else
    		return "none";
    }

    public String getRange() {
        return _range;
    }
    
    @Override
	public String toString() {
        return _name;
    }
    
    public static List<Config> getConfigs(String category) {
    	return _configs.get(category);
    }
    
    public static Config getConfig(String name) {
    	List<String> categories = getCategories();
    	for (String category : categories) {
    		List<Config> currentList = getConfigs(category);
    		for (Config c : currentList) {
    			if (c.key().equals(name))
    				return c;
    		}
    	}
    	
    	return null;
    }
    
    public static List<String> getCategories() {
    	Object[] keys = _configs.keySet().toArray();
    	List<String> categories = new ArrayList<String>();
    	for (Object key : keys) {
    		categories.add((String) key);
    	}
    	return categories;
    }
}
