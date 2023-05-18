/*
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloud.agent.properties;

/**
 * Class of constant agent's properties available to configure on
 * "agent.properties".
 *<br><br>
 * Not all available agent properties are defined here, but we should work to
 * migrate them on demand to this class.
 *
 * @param <T> type of the default value.
 */
public class AgentProperties{
    private static final String DEFAULT = "default";

    /**
     * MANDATORY: The GUID to identify the agent with.<br>
     * Generated with "uuidgen".<br>
     * Data type: String.<br>
     * Default value: <code>null</code>
     */
    public static final Property<String> GUID = new Property<>("guid", null, String.class);

    /**
     * The java class which the agent loads to execute.<br>
     * Data type: String.<br>
     * Default value: <code>com.cloud.hypervisor.kvm.resource.LibvirtComputingResource</code>
     */
    public static final Property<String> RESOURCE = new Property<>("resource", "com.cloud.hypervisor.kvm.resource.LibvirtComputingResource");

    /**
     * The number of threads running in the agent.<br>
     * Data type: Integer.<br>
     * Default value: <code>5</code>
     */
    protected final Property<Integer> workers = new Property<>("workers", 5);

    /**
     * The IP address of the management server.<br>
     * Data type: String.<br>
     * Default value: <code>localhost</code>
     */
    public static final Property<String> HOST = new Property<>("host", "localhost");

    /**
     * The time interval (in seconds) after which the agent will check if the connected host is the preferred host.<br>
     * After that interval, if the agent is connected to one of the secondary/backup hosts, it will attempt to reconnect to the preferred host.<br>
     * For more information see the agent.properties file.<br>
     * Data type: Integer.<br>
     * Default value: <code>null</code>
     */
    public static final Property<Long> HOST_LB_CHECK_INTERVAL = new Property<>("host.lb.check.interval", null, Long.class);

    /**
     * The port that the management server is listening on.<br>
     * Data type: Integer.<br>
     * Default value: <code>8250</code>
     */
    public static final Property<Integer> PORT = new Property<>("port", 8250);

    /**
     * The cluster which the agent belongs to.<br>
     * Data type: String.<br>
     * Default value: <code>default</code>
     */
    public static final Property<String> CLUSTER = new Property<>("cluster", DEFAULT);

    /**
     * The pod which the agent belongs to.<br>
     * Data type: String.<br>
     * Default value: <code>default</code>
     */
    public static final Property<String> POD = new Property<>("pod", DEFAULT);

    /**
     * The zone which the agent belongs to.<br>
     * Data type: String.<br>
     * Default value: <code>default</code>
     */
    public static final Property<String> ZONE = new Property<>("zone", DEFAULT);

    /**
     * Public NIC device. If this property is commented, it will be autodetected on service startup.<br>
     * Data type: String.<br>
     * Default value: <code>cloudbr0</code>
     */
    public static final Property<String> PUBLIC_NETWORK_DEVICE = new Property<>("public.network.device", "cloudbr0");

    /**
     * Private NIC device. If this property is commented, it will be autodetected on service startup.<br>
     * Data type: String.<br>
     * Default value: <code>cloudbr1</code>
     */
    public static final Property<String> PRIVATE_NETWORK_DEVICE = new Property<>("private.network.device", "cloudbr1");

    /**
     * Guest NIC device. If this property is commented, the value of the private NIC device will be used.<br>
     * Data type: String.<br>
     * Default value: the private NIC device value.
     */
    public static final Property<String> GUEST_NETWORK_DEVICE = new Property<>("guest.network.device", null, String.class);

    /**
     * Local storage path.<br>
     * This property allows multiple values to be entered in a single String. The differente values must be separated by commas.<br>
     * Data type: String.<br>
     * Default value: <code>/var/lib/libvirt/images/</code>
     */
    public static final Property<String> LOCAL_STORAGE_PATH = new Property<>("local.storage.path", "/var/lib/libvirt/images/");

    /**
     * Directory where Qemu sockets are placed.<br>
     * These sockets are for the Qemu Guest Agent and SSVM provisioning.<br>
     * Make sure that AppArmor or SELinux allows Libvirt to write there.<br>
     * Data type: String.<br>
     * Default value: <code>/var/lib/libvirt/qemu</code>
     */
    public static final Property<String> QEMU_SOCKETS_PATH = new Property<>("qemu.sockets.path", "/var/lib/libvirt/qemu");

    /**
     * MANDATORY: The UUID for the local storage pool.<br>
     * This property allows multiple values to be entered in a single String. The differente values must be separated by commas.<br>
     * Data type: String.<br>
     * Default value: <code>null</code>
     */
    public static final Property<String> LOCAL_STORAGE_UUID = new Property<>("local.storage.uuid", null, String.class);

    /**
     * Location for KVM virtual router scripts.<br>
     * The path defined in this property is relative to the directory "/usr/share/cloudstack-common/".<br>
     * Data type: String.<br>
     * Default value: <code>scripts/network/domr</code>
     */
    public static final Property<String> DOMR_SCRIPTS_DIR = new Property<>("domr.scripts.dir", "scripts/network/domr");

    /**
     * The timeout (in ms) for time-consuming operations, such as create/copy a snapshot.<br>
     * Data type: Integer.<br>
     * Default value: <code>7200</code>
     */
    public static final Property<Integer> CMDS_TIMEOUT = new Property<>("cmds.timeout", 7200);

    /**
     * This parameter sets the VM migration speed (in mbps). The default value is -1,<br>
     * which means that the agent will try to guess the speed of the guest network and consume all possible bandwidth.<br>
     * When entering a value, make sure to enter it in megabits per second.<br>
     * Data type: Integer.<br>
     * Default value: <code>-1</code>
     */
    public static final Property<Integer> VM_MIGRATE_SPEED = new Property<>("vm.migrate.speed", -1);

    /**
     * Sets target downtime (in ms) at end of livemigration, the 'hiccup' for final copy.<br>
     * Higher numbers make livemigration easier, lower numbers may cause migration to never complete.<br>
     * Less than 1 means hypervisor default (20ms).<br>
     * Data type: Integer.<br>
     * Default value: <code>-1</code>
     */
    public static final Property<Integer> VM_MIGRATE_DOWNTIME = new Property<>("vm.migrate.downtime", -1);

    /**
     * Busy VMs may never finish migrating, depending on the environment. <br>
     * Therefore, if configured, this option will pause the VM after the time entered (in ms) to force the migration to finish.<br>
     * Less than 1 means disabled.<br>
     * Data type: Integer.<br>
     * Default value: <code>-1</code>
     */
    public static final Property<Integer> VM_MIGRATE_PAUSEAFTER = new Property<>("vm.migrate.pauseafter", -1);

    /**
     * Time (in seconds) to wait for VM migration to finish. Less than 1 means disabled.<br>
     * For more information see the agent.properties file.<br>
     * Data type: Integer.<br>
     * Default value: <code>-1</code>
     */
    public static final Property<Integer> VM_MIGRATE_WAIT = new Property<>("vm.migrate.wait", -1);

    /**
     * Agent Hooks is the way to override default agent behavior to extend the functionality without excessive coding for a custom deployment.<br>
     * There are 3 arguments needed for the hook to be called: the base directory (defined in agent.hooks.basedir),<br>
     * the name of the script that is located in the base directory (defined in agent.hooks.*.script)<br>
     * and the method that is going to be called on the script (defined in agent.hooks.*.method).<br>
     * This property defines the agent hooks base directory, where all hooks are located.<br>
     * For more information see the agent.properties file.<br>
     * Data type: String.<br>
     * Default value: <code>/etc/cloudstack/agent/hooks</code>
     */
    public static final Property<String> AGENT_HOOKS_BASEDIR = new Property<>("agent.hooks.basedir", "/etc/cloudstack/agent/hooks");

    /**
     * This property is used with the agent.hooks.basedir property to define the Libvirt VM XML transformer script.<br>
     * The method to be called on the script is defined in the property agent.hooks.libvirt_vm_xml_transformer.method.<br>
     * Libvirt XML transformer hook does XML-to-XML transformation.<br>
     * The provider can use this to add/remove/modify some sort of attributes in Libvirt XML domain specification.<br>
     * For more information see the agent.properties file.<br>
     * Data type: String.<br>
     * Default value: <code>libvirt-vm-xml-transformer.groovy</code>
     */
    public static final Property<String> AGENT_HOOKS_LIBVIRT_VM_XML_TRANSFORMER_SCRIPT = new Property<>("agent.hooks.libvirt_vm_xml_transformer.script", "libvirt-vm-xml-transformer.groovy");

    /**
     * This property is used with the agent.hooks.basedir and agent.hooks.libvirt_vm_xml_transformer.script properties to define the Libvirt VM XML transformer method.<br>
     * Libvirt XML transformer hook does XML-to-XML transformation.<br>
     * The provider can use this to add/remove/modify some sort of attributes in Libvirt XML domain specification.<br>
     * For more information see the agent.properties file.<br>
     * Data type: String.<br>
     * Default value: <code>transform</code>
     */
    public static final Property<String> AGENT_HOOKS_LIBVIRT_VM_XML_TRANSFORMER_METHOD = new Property<>("agent.hooks.libvirt_vm_xml_transformer.method", "transform");

    /**
     * This property is used with the agent.hooks.basedir property to define the Libvirt VM on start script.<br>
     * The method to be called on the script is defined in the property agent.hooks.libvirt_vm_on_start.method.<br>
     * The hook is called right after Libvirt successfully launched the VM.<br>
     * For more information see the agent.properties file.<br>
     * Data type: String.<br>
     * Default value: <code>libvirt-vm-state-change.groovy</code>
     */
    public static final Property<String> AGENT_HOOKS_LIBVIRT_VM_ON_START_SCRIPT = new Property<>("agent.hooks.libvirt_vm_on_start.script", "libvirt-vm-state-change.groovy");

    /**
     * This property is used with the agent.hooks.basedir and agent.hooks.libvirt_vm_on_start.script properties to define the Libvirt VM on start method.<br>
     * The hook is called right after Libvirt successfully launched the VM.<br>
     * For more information see the agent.properties file.<br>
     * Data type: String.<br>
     * Default value: <code>onStart</code>
     */
    public static final Property<String> AGENT_HOOKS_LIBVIRT_VM_ON_START_METHOD = new Property<>("agent.hooks.libvirt_vm_on_start.method", "onStart");

    /**
     * This property is used with the agent.hooks.basedir property to define the Libvirt VM on stop script.<br>
     * The method to be called on the script is defined in the property agent.hooks.libvirt_vm_on_stop.method.<br>
     * The hook is called right after Libvirt successfully stopped the VM.<br>
     * For more information see the agent.properties file.<br>
     * Data type: String.<br>
     * Default value: <code>libvirt-vm-state-change.groovy</code>
     */
    public static final Property<String> AGENT_HOOKS_LIBVIRT_VM_ON_STOP_SCRIPT = new Property<>("agent.hooks.libvirt_vm_on_stop.script", "libvirt-vm-state-change.groovy");

    /**
     * This property is used with the agent.hooks.basedir and agent.hooks.libvirt_vm_on_stop.script properties to define the Libvirt VM on stop method.<br>
     * The hook is called right after libvirt successfully stopped the VM.<br>
     * For more information see the agent.properties file.<br>
     * Data type: String.<br>
     * Default value: <code>onStop</code>
     */
    public static final Property<String> AGENT_HOOKS_LIBVIRT_VM_ON_STOP_METHOD = new Property<>("agent.hooks.libvirt_vm_on_stop.method", "onStop");

    /**
     * Sets the type of bridge used on the hypervisor. This defines what commands the resource will use to setup networking.<br>
     * Possible values: native | openvswitch <br>
     * Data type: String.<br>
     * Default value: <code>native</code>
     */
    public static final Property<String> NETWORK_BRIDGE_TYPE = new Property<>("network.bridge.type", "native");

    /**
     * Sets the driver used to plug and unplug NICs from the bridges.<br>
     * A sensible default value will be selected based on the network.bridge.type but can be overridden here.<br>
     * Value for native = com.cloud.hypervisor.kvm.resource.BridgeVifDriver<br>
     * Value for openvswitch = com.cloud.hypervisor.kvm.resource.OvsVifDriver<br>
     * Value to enable direct networking in libvirt = com.cloud.hypervisor.kvm.resource.DirectVifDriver (should not be used on hosts that run system VMs)<br>
     * For more information see the agent.properties file.<br>
     * Data type: String.<br>
     * Default value: <code>null</code>
     */
    public static final Property<String> LIBVIRT_VIF_DRIVER = new Property<>("libvirt.vif.driver", null, String.class);

    /**
     * Setting to enable direct networking in libvirt.<br>
     * Should not be used on hosts that run system VMs.<br>
     * For more information see the agent.properties file.<br>
     * Possible values: private | bridge | vepa <br>
     * Data type: String.<br>
     * Default value: <code>null</code>
     */
    public static final Property<String> NETWORK_DIRECT_SOURCE_MODE = new Property<>("network.direct.source.mode", null, String.class);

    /**
     * Setting to enable direct networking in libvirt.<br>
     * Should not be used on hosts that run system VMs.<br>
     * For more information see the agent.properties file.<br>
     * Data type: String.<br>
     * Default value: <code>null</code>
     */
    public static final Property<String> NETWORK_DIRECT_DEVICE = new Property<>("network.direct.device", null, String.class);

    /**
     * Sets DPDK Support on OpenVSwitch.<br>
     * Data type: Boolean.<br>
     * Default value: <code>false</code>
     */
    public static final Property<Boolean> OPENVSWITCH_DPDK_ENABLED = new Property<>("openvswitch.dpdk.enabled", false);

    /**
     * Sets DPDK Support on OpenVSwitch (path).<br>
     * Data type: String.<br>
     * Default value: <code>null</code>
     */
    public static final Property<String> OPENVSWITCH_DPDK_OVS_PATH = new Property<>("openvswitch.dpdk.ovs.path", null, String.class);

    public static final Property<String> HEALTH_CHECK_SCRIPT_PATH =
            new Property<>("agent.health.check.script.path", null, String.class);

    /**
     * Sets the hypervisor type.<br>
     * Possible values: kvm | lxc <br>
     * Data type: String.<br>
     * Default value: <code>kvm</code>
     */
    public static final Property<String> HYPERVISOR_TYPE = new Property<>("hypervisor.type", "kvm");

    /**
     * Specifies a directory on the host local storage for temporary storing direct download templates.<br>
     * Data type: String.<br>
     * Default value: <code>/var/lib/libvirt/images</code>
     */
    public static final Property<String> DIRECT_DOWNLOAD_TEMPORARY_DOWNLOAD_LOCATION = new Property<>("direct.download.temporary.download.location",
            "/var/lib/libvirt/images");

    /**
     * Specifies a directory on the host local storage for creating and hosting the config drives.<br>
     * Data type: String.<br>
     * Default value: <code>/var/cache/cloud</code>
     */
    public static final Property<String> HOST_CACHE_LOCATION = new Property<>("host.cache.location", "/var/cache/cloud");

    /**
     * Sets the rolling maintenance hook scripts directory.<br>
     * Data type: String.<br>
     * Default value: <code>null</code>
     */
    public static final Property<String> ROLLING_MAINTENANCE_HOOKS_DIR = new Property<>("rolling.maintenance.hooks.dir", null, String.class);

    /**
     * Disables the rolling maintenance service execution.<br>
     * Data type: Boolean.<br>
     * Default value: <code>false</code>
     */
    public static final Property<Boolean> ROLLING_MAINTENANCE_SERVICE_EXECUTOR_DISABLED = new Property<>("rolling.maintenance.service.executor.disabled", false);

    /**
     * Sets the hypervisor URI.<br>
     * Value for KVM: qemu:///system<br>
     * Value for LXC: lxc:///<br>
     * Data type: String.<br>
     * Default value: <code>null</code>
     */
    public static final Property<String> HYPERVISOR_URI = new Property<>("hypervisor.uri", null, String.class);

    /**
     * Setting to enable the CPU model to KVM guest globally.<br>
     * Possible values: custom | host-model | host-passthrough <br>
     * For more information on each value see the agent.properties file.<br>
     * Data type: String.<br>
     * Default value: <code>null</code>
     */
    public static final Property<String> GUEST_CPU_MODE = new Property<>("guest.cpu.mode", null, String.class);

    /**
     * Custom CPU model. This param is only valid if property guest.cpu.mode=custom.<br>
     * For more information see the agent.properties file.<br>
     * Data type: String.<br>
     * Default value: <code>null</code>
     */
    public static final Property<String> GUEST_CPU_MODEL = new Property<>("guest.cpu.model", null, String.class);

    /**
     * This param will set the CPU architecture for the domain to override what the management server would send.<br>
     * In case of arm64 (aarch64), this will change the machine type to 'virt' and add a SCSI and a USB controller in the domain XML.<br>
     * Possible values: x86_64 | aarch64 <br>
     * Data type: String.<br>
     * Default value: <code>null</code> (will set use the architecture of the VM's OS).
     */
    public static final Property<String> GUEST_CPU_ARCH = new Property<>("guest.cpu.arch", null, String.class);

    /**
     * This param will require CPU features on the CPU section.<br>
     * The features listed in this property must be separated by a blank space (see example below).<br>
     * Possible values: vmx vme <br>
     * Data type: String.<br>
     * Default value: <code>null</code>
     */
    public static final Property<String> GUEST_CPU_FEATURES = new Property<>("guest.cpu.features", null, String.class);

    /**
     * Disables memory ballooning on VM guests for overcommit.<br>
     * By default overcommit feature enables balloon and sets currentMemory to a minimum value.<br>
     * Data type: Boolean.<br>
     * Default value: <code>false</code>
     */
    public static final Property<Boolean> VM_MEMBALLOON_DISABLE = new Property<>("vm.memballoon.disable", false);

    /**
     * Set to true to check disk activity on VM's disks before starting a VM.<br>
     * This only applies to QCOW2 files, and ensures that there is no other running instance accessing the file before starting.<br>
     * It works by checking the modified time against the current time, so care must be taken to ensure that the cluster's time is synchronized, otherwise VMs may fail to start.<br>
     * Data type: Boolean.<br>
     * Default value: <code>false</code>
     */
    public static final Property<Boolean> VM_DISKACTIVITY_CHECKENABLED = new Property<>("vm.diskactivity.checkenabled", false);

    /**
     * Timeout (in seconds) for giving up on waiting for VM's disk files to become inactive.<br>
     * Hitting this timeout will result in failure to start VM.<br>
     * Value must be greater than 0 (zero), otherwise the default value will be used.<br>
     * Data type: Integer.<br>
     * Default value: <code>120</code>
     */
    public static final Property<Integer> VM_DISKACTIVITY_CHECKTIMEOUT_S = new Property<>("vm.diskactivity.checktimeout_s", 120);

    /**
     * The length of time (in ms) that the disk needs to be inactive in order to pass the check.<br>
     * This means current time minus time of disk file needs to be greater than this number.<br>
     * It also has the side effect of setting the minimum threshold between a stop and start of a given VM.<br>
     * Value must be greater than 0 (zero), otherwise the default value will be used.<br>
     * Data type: Long.<br>
     * Default value: <code>30000L</code>
     */
    public static final Property<Long> VM_DISKACTIVITY_INACTIVETIME_MS = new Property<>("vm.diskactivity.inactivetime_ms", 30000L);

    /**
     * Some newer linux kernels are incapable of reliably migrating VMs with KVMclock.<br>
     * This is a workaround for the bug, admin can set this to true per-host.<br>
     * Data type: Boolean.<br>
     * Default value: <code>false</code>
     */
    public static final Property<Boolean> KVMCLOCK_DISABLE = new Property<>("kvmclock.disable", false);

    /**
     * This enables the VirtIO Random Number Generator device for guests. <br>
     * Data type: Boolean.<br>
     * Default value: <code>false</code>
     */
    public static final Property<Boolean> VM_RNG_ENABLE = new Property<>("vm.rng.enable", false);

    /**
     * The model of VirtIO Random Number Generator (RNG) to present to the Guest.<br>
     * Currently only 'random' is supported.<br>
     * Data type: String.<br>
     * Default value: <code>random</code>
     */
    public static final Property<String> VM_RNG_MODEL = new Property<>("vm.rng.model", "random");

    /**
     * Local Random Number Device Generator to use for VirtIO RNG for Guests.<br>
     * This is usually /dev/random, but it might be different per platform.<br>
     * Data type: String.<br>
     * Default value: <code>/dev/random</code>
     */
    public static final Property<String> VM_RNG_PATH = new Property<>("vm.rng.path", "/dev/random");

    /**
     * The amount of bytes the Guest may request/obtain from the RNG in the period specified in the property vm.rng.rate.period.<br>
     * Data type: Integer.<br>
     * Default value: <code>2048</code>
     */
    public static final Property<Integer> VM_RNG_RATE_BYTES = new Property<>("vm.rng.rate.bytes", 2048);

    /**
     * The number of milliseconds in which the guest is allowed to obtain the bytes specified in vm.rng.rate.bytes.<br>
     * Data type: Integer.<br>
     * Default value: <code>1000</code>
     */
    public static final Property<Integer> VM_RNG_RATE_PERIOD = new Property<>("vm.rng.rate.period", 1000);

    /**
     * Timeout value for aggregation commands to be sent to the virtual router (in seconds).<br>
     * Data type: Long.<br>
     * Default value: <code>null</code>
     */
    public static final Property<Long> ROUTER_AGGREGATION_COMMAND_EACH_TIMEOUT = new Property<>("router.aggregation.command.each.timeout", null, Long.class);

    /**
     * Allows to virtually increase the amount of RAM (in MB) available on the host.<br>
     * This property can be useful if the host uses Zswap, KSM features and other memory compressing technologies.<br>
     * For example: if the host has 2GB of RAM and this property is set to 2048, the amount of RAM of the host will be read as 4GB.<br>
     * Data type: Integer.<br>
     * Default value: <code>0</code>
     */
    public static final Property<Integer> HOST_OVERCOMMIT_MEM_MB = new Property<>("host.overcommit.mem.mb", 0);

    /**
     * How much host memory (in MB) to reserve for non-allocation.<br>
     * A useful parameter if a node uses some other software that requires memory, or in case that OOM Killer kicks in.<br>
     * If this parameter is used, property host.overcommit.mem.mb must be set to 0.<br>
     * Data type: Integer.<br>
     * Default value: <code>1024</code>
     */
    public static final Property<Integer> HOST_RESERVED_MEM_MB = new Property<>("host.reserved.mem.mb", 1024);

    /**
     * The model of Watchdog timer to present to the Guest.<br>
     * For all models refer to the libvirt documentation.<br>
     * Data type: String.<br>
     * Default value: <code>i6300esb</code>
     */
    public static final Property<String> VM_WATCHDOG_MODEL = new Property<>("vm.watchdog.model", "i6300esb");

    /**
     * Action to take when the Guest/Instance is no longer notifying the Watchdog timer.<br>
     * Possible values: none | reset | poweroff <br>
     * Data type: String.<br>
     * Default value: <code>none</code>
     */
    public static final Property<String> VM_WATCHDOG_ACTION = new Property<>("vm.watchdog.action", "none");

    /**
     * Automatically clean up iSCSI sessions not attached to any VM.<br>
     * Should be enabled for users using managed storage (for example solidfire).<br>
     * Should be disabled for users with unmanaged iSCSI connections on their hosts.<br>
     * Data type: Boolean.<br>
     * Default value: <code>false</code>
     */
    public static final Property<Boolean> ISCSI_SESSION_CLEANUP_ENABLED = new Property<>("iscsi.session.cleanup.enabled", false);

    /**
     * Heartbeat update timeout (in ms).<br>
     * Depending on the use case, this timeout might need increasing/decreasing.<br>
     * Data type: Integer.<br>
     * Default value: <code>60000</code>
     */
    public static final Property<Integer> HEARTBEAT_UPDATE_TIMEOUT = new Property<>("heartbeat.update.timeout", 60000);

    /**
     * The timeout (in seconds) to retrieve the target's domain ID when migrating a VM with KVM. <br>
     * Data type: Integer. <br>
     * Default value: <code>10</code>
     */
    public static final Property<Integer> VM_MIGRATE_DOMAIN_RETRIEVE_TIMEOUT = new Property<>("vm.migrate.domain.retrieve.timeout", 10);

    /**
     * This parameter specifies if the host must be rebooted when something goes wrong with the heartbeat.<br>
     * Data type: Boolean.<br>
     * Default value: <code>true</code>
     */
    public static final Property<Boolean> REBOOT_HOST_AND_ALERT_MANAGEMENT_ON_HEARTBEAT_TIMEOUT
        = new Property<>("reboot.host.and.alert.management.on.heartbeat.timeout", true);

    /**
     * Enables manually setting CPU's topology on KVM's VM. <br>
     * Data type: Boolean.<br>
     * Default value: <code>true</code>
     */
    public static final Property<Boolean> ENABLE_MANUALLY_SETTING_CPU_TOPOLOGY_ON_KVM_VM = new Property<>("enable.manually.setting.cpu.topology.on.kvm.vm", true);

    /**
     * Manually sets the host CPU speed (in MHz), in cases where CPU scaling support detects the value is wrong. <br>
     * Data type: Integer.<br>
     * Default value: <code>0</code>
     */
    public static final Property<Integer> HOST_CPU_MANUAL_SPEED_MHZ = new Property<>("host.cpu.manual.speed.mhz", 0);

    /**
     * Defines the location for Hypervisor scripts.<br>
     * The path defined in this property is relative.<br>
     * To locate the script, ACS first tries to concatenate the property path with "/usr/share/cloudstack-agent/lib/".<br>
     * If it fails, it will test each folder of the path, decreasing one by one, until it reaches root.<br>
     * If the script is not found, ACS will repeat the same steps concatenating the property path with "/usr/share/cloudstack-common/".<br>
     * The path defined in this property is relative to the directory "/usr/share/cloudstack-common/".<br>
     * Data type: String.<br>
     * Default value: <code>scripts/vm/hypervisor</code>
     */
    public static final Property<String> HYPERVISOR_SCRIPTS_DIR = new Property<>("hypervisor.scripts.dir", "scripts/vm/hypervisor");

    /**
     * Defines the location for KVM scripts.<br>
     * The path defined in this property is relative.<br>
     * To locate the script, ACS first tries to concatenate the property path with "/usr/share/cloudstack-agent/lib/".<br>
     * If it fails, it will test each folder of the path, decreasing one by one, until it reaches root.<br>
     * If the script is not found, ACS will repeat the same steps concatenating the property path with "/usr/share/cloudstack-common/".<br>
     * The path defined in this property is relative to the directory "/usr/share/cloudstack-common/".<br>
     * Data type: String.<br>
     * Default value: <code>scripts/vm/hypervisor/kvm</code>
     */
    public static final Property<String> KVM_SCRIPTS_DIR = new Property<>("kvm.scripts.dir", "scripts/vm/hypervisor/kvm");

    /**
     * Specifies start MAC address for private IP range.<br>
     * Data type: String.<br>
     * Default value: <code>00:16:3e:77:e2:a0</code>
     */
    public static final Property<String> PRIVATE_MACADDR_START = new Property<>("private.macaddr.start", "00:16:3e:77:e2:a0");

    /**
     * Specifies start IP for private IP range. <br>
     * Data type: String.<br>
     * Default value: <code>192.168.166.128</code>
     */
    public static final Property<String> PRIVATE_IPADDR_START = new Property<>("private.ipaddr.start", "192.168.166.128");

    /**
     * Defines Local Bridge Name.<br>
     * Data type: String.<br>
     * Default value: <code>null</code>
     */
    public static final Property<String> PRIVATE_BRIDGE_NAME = new Property<>("private.bridge.name", null, String.class);

    /**
     * Defines private network name.<br>
     * Data type: String.<br>
     * Default value: <code>null</code>
     */
    public static final Property<String> PRIVATE_NETWORK_NAME = new Property<>("private.network.name", null, String.class);

    /**
     * Defines the location for network scripts.<br>
     * The path defined in this property is relative.<br>
     * To locate the script, ACS first tries to concatenate the property path with "/usr/share/cloudstack-agent/lib/".<br>
     * If it fails, it will test each folder of the path, decreasing one by one, until it reaches root.<br>
     * If the script is not found, ACS will repeat the same steps concatenating the property path with "/usr/share/cloudstack-common/".<br>
     * The path defined in this property is relative to the directory "/usr/share/cloudstack-common/".<br>
     * Data type: String.<br>
     * Default value: <code>scripts/vm/network/vnet</code>
     */
    public static final Property<String> NETWORK_SCRIPTS_DIR = new Property<>("network.scripts.dir", "scripts/vm/network/vnet");

    /**
     * Defines the location for storage scripts.<br>
     * The path defined in this property is relative.<br>
     * To locate the script, ACS first tries to concatenate the property path with "/usr/share/cloudstack-agent/lib/".<br>
     * If it fails, it will test each folder of the path, decreasing one by one, until it reaches root.<br>
     * If the script is not found, ACS will repeat the same steps concatenating the property path with "/usr/share/cloudstack-common/".<br>
     * The path defined in this property is relative to the directory "/usr/share/cloudstack-common/".<br>
     * Data type: String.<br>
     * Default value: <code>scripts/storage/qcow2</code>
     */
    public static final Property<String> STORAGE_SCRIPTS_DIR = new Property<>("storage.scripts.dir", "scripts/storage/qcow2");

    /**
     * Time (in seconds) to wait for the VM to shutdown gracefully.<br>
     * If the time is exceeded shutdown will be forced.<br>
     * Data type: Integer.<br>
     * Default value: <code>120</code>
     */
    public static final Property<Integer> STOP_SCRIPT_TIMEOUT = new Property<>("stop.script.timeout", 120);

    /**
     * Definition of VMs video model type.<br>
     * Data type: String.<br>
     * Default value: <code>null</code>
     */
    public static final Property<String> VM_VIDEO_HARDWARE = new Property<>("vm.video.hardware", null, String.class);

    /**
     * Definition of VMs video, specifies the amount of RAM in kibibytes (blocks of 1024 bytes).<br>
     * Data type: Integer.<br>
     * Default value: <code>0</code>
     */
    public static final Property<Integer> VM_VIDEO_RAM = new Property<>("vm.video.ram", 0);

    /**
     * System VM ISO path.<br>
     * Data type: String.<br>
     * Default value: <code>null</code>
     */
    public static final Property<String> SYSTEMVM_ISO_PATH = new Property<>("systemvm.iso.path", null, String.class);

    /**
     * If set to "true", allows override of the properties: private.macaddr.start, private.ipaddr.start, private.ipaddr.end.<br>
     * Data type: Boolean.<br>
     * Default value: <code>false</code>
     */
    public static final Property<Boolean> DEVELOPER = new Property<>("developer", false);

    /**
     * Can only be used if developer = true. This property is used to define the local bridge name and private network name.<br>
     * Data type: String.<br>
     * Default value: <code>null</code>
     */
    public static final Property<String> INSTANCE = new Property<>("instance", null, String.class);

    /**
     * Shows the path to the base directory in which NFS servers are going to be mounted.<br>
     * Data type: String.<br>
     * Default value: <code>/mnt</code>
     */
    public static final Property<String> MOUNT_PATH = new Property<>("mount.path", "/mnt");

    /**
     * Port listened by the console proxy.
     * Data type: Integer.<br>
     * Default value: <code>443</code>
     */
    public static final Property<Integer> CONSOLEPROXY_HTTPLISTENPORT = new Property<>("consoleproxy.httpListenPort", 443);

    /**
     * Data type: Integer.<br>
     * Default value: <code>5</code>
     */
    public static final Property<Integer> PING_RETRIES = new Property<>("ping.retries", 5);

    /**
     * Returns {@link AgentProperties#workers}.
     */
    public Property<Integer> getWorkers() {
        return workers;
    }

    /**
     * The time interval (in seconds) at which the balloon driver will get memory stats updates. This is equivalent to Libvirt's <code>--period</code> parameter when using the dommemstat command.
     * Data type: Integer.<br>
     * Default value: <code>0</code>
     */
    public static final Property<Integer> VM_MEMBALLOON_STATS_PERIOD = new Property<>("vm.memballoon.stats.period", 0);

    /**
     * The number of iothreads
     * Data type: Integer.<br>
     * Default value: <code>1</code>
     */
    public static final Property<Integer> IOTHREADS = new Property<>("iothreads", 1);

    public static class Property <T>{
        private String name;
        private T defaultValue;
        private Class<T> typeClass;

        Property(String name, T value) {
            init(name, value);
        }

        Property(String name, T defaultValue, Class<T> typeClass) {
            this.typeClass = typeClass;
            init(name, defaultValue);
        }

        private void init(String name, T defaultValue) {
            this.name = name;
            this.defaultValue = defaultValue;

            if (defaultValue != null) {
                this.typeClass = (Class<T>)defaultValue.getClass();
            }
        }

        public String getName() {
            return name;
        }

        public T getDefaultValue() {
            return defaultValue;
        }

        public Class<T> getTypeClass() {
            return typeClass;
        }
    }
}
