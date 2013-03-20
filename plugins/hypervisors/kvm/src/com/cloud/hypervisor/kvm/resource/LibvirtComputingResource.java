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
package com.cloud.hypervisor.kvm.resource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.DomainInfo;
import org.libvirt.DomainInterfaceStats;
import org.libvirt.DomainSnapshot;
import org.libvirt.LibvirtException;
import org.libvirt.NodeInfo;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.AttachIsoCommand;
import com.cloud.agent.api.AttachVolumeAnswer;
import com.cloud.agent.api.AttachVolumeCommand;
import com.cloud.agent.api.BackupSnapshotAnswer;
import com.cloud.agent.api.BackupSnapshotCommand;
import com.cloud.agent.api.CheckHealthAnswer;
import com.cloud.agent.api.CheckHealthCommand;
import com.cloud.agent.api.CheckNetworkAnswer;
import com.cloud.agent.api.CheckNetworkCommand;
import com.cloud.agent.api.CheckStateCommand;
import com.cloud.agent.api.CheckVirtualMachineAnswer;
import com.cloud.agent.api.CheckVirtualMachineCommand;
import com.cloud.agent.api.CleanupNetworkRulesCmd;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.CreatePrivateTemplateFromSnapshotCommand;
import com.cloud.agent.api.CreatePrivateTemplateFromVolumeCommand;
import com.cloud.agent.api.CreateStoragePoolCommand;
import com.cloud.agent.api.CreateVolumeFromSnapshotAnswer;
import com.cloud.agent.api.CreateVolumeFromSnapshotCommand;
import com.cloud.agent.api.DeleteSnapshotBackupAnswer;
import com.cloud.agent.api.DeleteSnapshotBackupCommand;
import com.cloud.agent.api.DeleteSnapshotsDirCommand;
import com.cloud.agent.api.DeleteStoragePoolCommand;
import com.cloud.agent.api.FenceAnswer;
import com.cloud.agent.api.FenceCommand;
import com.cloud.agent.api.GetHostStatsAnswer;
import com.cloud.agent.api.GetHostStatsCommand;
import com.cloud.agent.api.GetStorageStatsAnswer;
import com.cloud.agent.api.GetStorageStatsCommand;
import com.cloud.agent.api.GetVmStatsAnswer;
import com.cloud.agent.api.GetVmStatsCommand;
import com.cloud.agent.api.GetVncPortAnswer;
import com.cloud.agent.api.GetVncPortCommand;
import com.cloud.agent.api.HostStatsEntry;
import com.cloud.agent.api.MaintainAnswer;
import com.cloud.agent.api.MaintainCommand;
import com.cloud.agent.api.ManageSnapshotAnswer;
import com.cloud.agent.api.ManageSnapshotCommand;
import com.cloud.agent.api.MigrateAnswer;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.agent.api.ModifySshKeysCommand;
import com.cloud.agent.api.ModifyStoragePoolAnswer;
import com.cloud.agent.api.ModifyStoragePoolCommand;
import com.cloud.agent.api.NetworkRulesSystemVmCommand;
import com.cloud.agent.api.NetworkRulesVmSecondaryIpCommand;
import com.cloud.agent.api.NetworkUsageAnswer;
import com.cloud.agent.api.NetworkUsageCommand;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.PingRoutingCommand;
import com.cloud.agent.api.PingRoutingWithNwGroupsCommand;
import com.cloud.agent.api.PingTestCommand;
import com.cloud.agent.api.PlugNicAnswer;
import com.cloud.agent.api.PlugNicCommand;
import com.cloud.agent.api.PrepareForMigrationAnswer;
import com.cloud.agent.api.PrepareForMigrationCommand;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.RebootAnswer;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.RebootRouterCommand;
import com.cloud.agent.api.SecurityGroupRuleAnswer;
import com.cloud.agent.api.SecurityGroupRulesCmd;
import com.cloud.agent.api.SetupGuestNetworkAnswer;
import com.cloud.agent.api.SetupGuestNetworkCommand;
import com.cloud.agent.api.StartAnswer;
import com.cloud.agent.api.StartCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.agent.api.StartupStorageCommand;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.UnPlugNicAnswer;
import com.cloud.agent.api.UnPlugNicCommand;
import com.cloud.agent.api.UpgradeSnapshotCommand;
import com.cloud.agent.api.VmStatsEntry;
import com.cloud.agent.api.check.CheckSshAnswer;
import com.cloud.agent.api.check.CheckSshCommand;
import com.cloud.agent.api.proxy.CheckConsoleProxyLoadCommand;
import com.cloud.agent.api.proxy.ConsoleProxyLoadAnswer;
import com.cloud.agent.api.proxy.WatchConsoleProxyLoadCommand;
import com.cloud.agent.api.routing.IpAssocAnswer;
import com.cloud.agent.api.routing.IpAssocCommand;
import com.cloud.agent.api.routing.IpAssocVpcCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.routing.SetNetworkACLAnswer;
import com.cloud.agent.api.routing.SetNetworkACLCommand;
import com.cloud.agent.api.routing.SetSourceNatAnswer;
import com.cloud.agent.api.routing.SetSourceNatCommand;
import com.cloud.agent.api.storage.CopyVolumeAnswer;
import com.cloud.agent.api.storage.CopyVolumeCommand;
import com.cloud.agent.api.storage.CreateAnswer;
import com.cloud.agent.api.storage.CreateCommand;
import com.cloud.agent.api.storage.CreatePrivateTemplateAnswer;
import com.cloud.agent.api.storage.DestroyCommand;
import com.cloud.agent.api.storage.PrimaryStorageDownloadAnswer;
import com.cloud.agent.api.storage.PrimaryStorageDownloadCommand;
import com.cloud.agent.api.storage.ResizeVolumeCommand;
import com.cloud.agent.api.storage.ResizeVolumeAnswer;
import com.cloud.agent.api.to.IpAddressTO;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.agent.resource.virtualnetwork.VirtualRoutingResource;
import com.cloud.dc.Vlan;
import com.cloud.exception.InternalErrorException;
import com.cloud.host.Host.Type;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.kvm.resource.KVMHABase.NfsStoragePool;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.ClockDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.ConsoleDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.CpuTuneDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.DevicesDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.DiskDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.DiskDef.diskProtocol;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.FeaturesDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.GraphicDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.GuestDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.GuestResourceDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.InputDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.InterfaceDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.InterfaceDef.hostNicType;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.SerialDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.VirtioSerialDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.TermPolicy;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk.PhysicalDiskFormat;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.IsolationType;
import com.cloud.network.Networks.RouterPrivateIpStrategy;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetworkSetupInfo;
import com.cloud.resource.ServerResource;
import com.cloud.resource.ServerResourceBase;
import com.cloud.storage.JavaStorageLayer;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StorageLayer;
import com.cloud.storage.Volume;
import com.cloud.storage.template.Processor;
import com.cloud.storage.template.Processor.FormatInfo;
import com.cloud.storage.template.QCOW2Processor;
import com.cloud.storage.template.TemplateInfo;
import com.cloud.storage.template.TemplateLocation;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.FileUtil;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachineName;

/**
 * LibvirtComputingResource execute requests on the computing/routing host using
 * the libvirt API
 * 
 * @config {@table || Param Name | Description | Values | Default || ||
 *         hypervisor.type | type of local hypervisor | string | kvm || ||
 *         hypervisor.uri | local hypervisor to connect to | URI |
 *         qemu:///system || || domr.arch | instruction set for domr template |
 *         string | i686 || || private.bridge.name | private bridge where the
 *         domrs have their private interface | string | vmops0 || ||
 *         public.bridge.name | public bridge where the domrs have their public
 *         interface | string | br0 || || private.network.name | name of the
 *         network where the domrs have their private interface | string |
 *         vmops-private || || private.ipaddr.start | start of the range of
 *         private ip addresses for domrs | ip address | 192.168.166.128 || ||
 *         private.ipaddr.end | end of the range of private ip addresses for
 *         domrs | ip address | start + 126 || || private.macaddr.start | start
 *         of the range of private mac addresses for domrs | mac address |
 *         00:16:3e:77:e2:a0 || || private.macaddr.end | end of the range of
 *         private mac addresses for domrs | mac address | start + 126 || ||
 *         pool | the parent of the storage pool hierarchy * }
 **/
@Local(value = { ServerResource.class })
public class LibvirtComputingResource extends ServerResourceBase implements
ServerResource {
    private static final Logger s_logger = Logger
            .getLogger(LibvirtComputingResource.class);

    private String _modifyVlanPath;
    private String _versionstringpath;
    private String _patchViaSocketPath;
    private String _createvmPath;
    private String _manageSnapshotPath;
    private String _resizeVolumePath;
    private String _createTmplPath;
    private String _heartBeatPath;
    private String _securityGroupPath;
    private String _routerProxyPath;
    private String _host;
    private String _dcId;
    private String _pod;
    private String _clusterId;
    private int _migrateSpeed;

    private long _hvVersion;
    private KVMHAMonitor _monitor;
    private final String _SSHKEYSPATH = "/root/.ssh";
    private final String _SSHPRVKEYPATH = _SSHKEYSPATH + File.separator
            + "id_rsa.cloud";
    private final String _SSHPUBKEYPATH = _SSHKEYSPATH + File.separator
            + "id_rsa.pub.cloud";
    private String _mountPoint = "/mnt";
    StorageLayer _storage;
    private KVMStoragePoolManager _storagePoolMgr;

    private VifDriver _defaultVifDriver;
    private Map<TrafficType, VifDriver> _trafficTypeVifDrivers;
    protected static final String DEFAULT_OVS_VIF_DRIVER_CLASS_NAME = "com.cloud.hypervisor.kvm.resource.OvsVifDriver";
    protected static final String DEFAULT_BRIDGE_VIF_DRIVER_CLASS_NAME = "com.cloud.hypervisor.kvm.resource.BridgeVifDriver";

    private static final class KeyValueInterpreter extends OutputInterpreter {
        private final Map<String, String> map = new HashMap<String, String>();

        @Override
        public String interpret(BufferedReader reader) throws IOException {
            String line = null;
            int numLines = 0;
            while ((line = reader.readLine()) != null) {
                String[] toks = line.trim().split("=");
                if (toks.length < 2) {
                    s_logger.warn("Failed to parse Script output: " + line);
                } else {
                    map.put(toks[0].trim(), toks[1].trim());
                }
                numLines++;
            }
            if (numLines == 0) {
                s_logger.warn("KeyValueInterpreter: no output lines?");
            }
            return null;
        }

        public Map<String, String> getKeyValues() {
            return map;
        }
    }

    @Override
    protected String getDefaultScriptsDir() {
        return null;
    }

    protected static MessageFormat SnapshotXML = new MessageFormat(
            "   <domainsnapshot>" + "       <name>{0}</name>" + "          <domain>"
                    + "            <uuid>{1}</uuid>" + "        </domain>"
                    + "    </domainsnapshot>");

    protected String _hypervisorType;
    protected String _hypervisorURI;
    protected String _hypervisorPath;
    protected String _sysvmISOPath;
    protected String _privNwName;
    protected String _privBridgeName;
    protected String _linkLocalBridgeName;
    protected String _publicBridgeName;
    protected String _guestBridgeName;
    protected String _privateIp;
    protected String _pool;
    protected String _localGateway;
    private boolean _can_bridge_firewall;
    protected String _localStoragePath;
    protected String _localStorageUUID;
    private final Map <String, String> _pifs = new HashMap<String, String>();
    private final Map<String, Map<String, String>> hostNetInfo = new HashMap<String, Map<String, String>>();
    private final Map<String, vmStats> _vmStats = new ConcurrentHashMap<String, vmStats>();

    protected boolean _disconnected = true;
    protected int _timeout;
    protected int _cmdsTimeout;
    protected int _stopTimeout;
    protected static HashMap<DomainInfo.DomainState, State> s_statesTable;
    static {
        s_statesTable = new HashMap<DomainInfo.DomainState, State>();
        s_statesTable.put(DomainInfo.DomainState.VIR_DOMAIN_SHUTOFF,
                State.Stopped);
        s_statesTable.put(DomainInfo.DomainState.VIR_DOMAIN_PAUSED,
                State.Running);
        s_statesTable.put(DomainInfo.DomainState.VIR_DOMAIN_RUNNING,
                State.Running);
        s_statesTable.put(DomainInfo.DomainState.VIR_DOMAIN_BLOCKED,
                State.Running);
        s_statesTable.put(DomainInfo.DomainState.VIR_DOMAIN_NOSTATE,
                State.Unknown);
        s_statesTable.put(DomainInfo.DomainState.VIR_DOMAIN_SHUTDOWN,
                State.Stopping);
    }

    protected HashMap<String, State> _vms = new HashMap<String, State>(20);
    protected List<String> _vmsKilled = new ArrayList<String>();

    private VirtualRoutingResource _virtRouterResource;

    private String _pingTestPath;

    private int _dom0MinMem;

    protected enum BridgeType {
        NATIVE, OPENVSWITCH
    }

    protected BridgeType _bridgeType;

    private String getEndIpFromStartIp(String startIp, int numIps) {
        String[] tokens = startIp.split("[.]");
        assert (tokens.length == 4);
        int lastbyte = Integer.parseInt(tokens[3]);
        lastbyte = lastbyte + numIps;
        tokens[3] = Integer.toString(lastbyte);
        StringBuilder end = new StringBuilder(15);
        end.append(tokens[0]).append(".").append(tokens[1]).append(".")
        .append(tokens[2]).append(".").append(tokens[3]);
        return end.toString();
    }

    private Map<String, Object> getDeveloperProperties()
            throws ConfigurationException {
        final File file = PropertiesUtil.findConfigFile("developer.properties");
        if (file == null) {
            throw new ConfigurationException(
                    "Unable to find developer.properties.");
        }

        s_logger.info("developer.properties found at " + file.getAbsolutePath());
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(file));

            String startMac = (String) properties.get("private.macaddr.start");
            if (startMac == null) {
                throw new ConfigurationException(
                        "Developers must specify start mac for private ip range");
            }

            String startIp = (String) properties.get("private.ipaddr.start");
            if (startIp == null) {
                throw new ConfigurationException(
                        "Developers must specify start ip for private ip range");
            }
            final Map<String, Object> params = PropertiesUtil.toMap(properties);

            String endIp = (String) properties.get("private.ipaddr.end");
            if (endIp == null) {
                endIp = getEndIpFromStartIp(startIp, 16);
                params.put("private.ipaddr.end", endIp);
            }
            return params;
        } catch (final FileNotFoundException ex) {
            throw new CloudRuntimeException("Cannot find the file: "
                    + file.getAbsolutePath(), ex);
        } catch (final IOException ex) {
            throw new CloudRuntimeException("IOException in reading "
                    + file.getAbsolutePath(), ex);
        }
    }

    protected String getDefaultNetworkScriptsDir() {
        return "scripts/vm/network/vnet";
    }

    protected String getDefaultStorageScriptsDir() {
        return "scripts/storage/qcow2";
    }

    protected String getDefaultKvmScriptsDir() {
        return "scripts/vm/hypervisor/kvm";
    }

    protected String getDefaultDomrScriptsDir() {
        return "scripts/network/domr/kvm";
    }

    @Override
    public boolean configure(String name, Map<String, Object> params)
            throws ConfigurationException {
        boolean success = super.configure(name, params);
        if (!success) {
            return false;
        }

        _storage = new JavaStorageLayer();
        _storage.configure("StorageLayer", params);

        String domrScriptsDir = (String) params.get("domr.scripts.dir");
        if (domrScriptsDir == null) {
            domrScriptsDir = getDefaultDomrScriptsDir();
        }

        String kvmScriptsDir = (String) params.get("kvm.scripts.dir");
        if (kvmScriptsDir == null) {
            kvmScriptsDir = getDefaultKvmScriptsDir();
        }

        String networkScriptsDir = (String) params.get("network.scripts.dir");
        if (networkScriptsDir == null) {
            networkScriptsDir = getDefaultNetworkScriptsDir();
        }

        String storageScriptsDir = (String) params.get("storage.scripts.dir");
        if (storageScriptsDir == null) {
            storageScriptsDir = getDefaultStorageScriptsDir();
        }

        String bridgeType = (String) params.get("network.bridge.type");
        if (bridgeType == null) {
            _bridgeType = BridgeType.NATIVE;
        }
        else {
            _bridgeType = BridgeType.valueOf(bridgeType.toUpperCase());
        }

        params.put("domr.scripts.dir", domrScriptsDir);

        _virtRouterResource = new VirtualRoutingResource();
        success = _virtRouterResource.configure(name, params);

        if (!success) {
            return false;
        }

        _host = (String) params.get("host");
        if (_host == null) {
            _host = "localhost";
        }

        _dcId = (String) params.get("zone");
        if (_dcId == null) {
            _dcId = "default";
        }

        _pod = (String) params.get("pod");
        if (_pod == null) {
            _pod = "default";
        }

        _clusterId = (String) params.get("cluster");

        _modifyVlanPath = Script.findScript(networkScriptsDir, "modifyvlan.sh");
        if (_modifyVlanPath == null) {
            throw new ConfigurationException("Unable to find modifyvlan.sh");
        }

        _versionstringpath = Script.findScript(kvmScriptsDir, "versions.sh");
        if (_versionstringpath == null) {
            throw new ConfigurationException("Unable to find versions.sh");
        }

        _patchViaSocketPath = Script.findScript(kvmScriptsDir + "/patch/",
                "patchviasocket.pl");
        if (_patchViaSocketPath == null) {
            throw new ConfigurationException("Unable to find patchviasocket.pl");
        }

        _heartBeatPath = Script.findScript(kvmScriptsDir, "kvmheartbeat.sh");
        if (_heartBeatPath == null) {
            throw new ConfigurationException("Unable to find kvmheartbeat.sh");
        }

        _createvmPath = Script.findScript(storageScriptsDir, "createvm.sh");
        if (_createvmPath == null) {
            throw new ConfigurationException("Unable to find the createvm.sh");
        }

        _manageSnapshotPath = Script.findScript(storageScriptsDir,
                "managesnapshot.sh");
        if (_manageSnapshotPath == null) {
            throw new ConfigurationException(
                    "Unable to find the managesnapshot.sh");
        }

        _resizeVolumePath = Script.findScript(storageScriptsDir, "resizevolume.sh");
        if (_resizeVolumePath == null) {
            throw new ConfigurationException(
                    "Unable to find the resizevolume.sh");
        }

        _createTmplPath = Script
                .findScript(storageScriptsDir, "createtmplt.sh");
        if (_createTmplPath == null) {
            throw new ConfigurationException(
                    "Unable to find the createtmplt.sh");
        }

        _securityGroupPath = Script.findScript(networkScriptsDir,
                "security_group.py");
        if (_securityGroupPath == null) {
            throw new ConfigurationException(
                    "Unable to find the security_group.py");
        }

        _routerProxyPath = Script.findScript("scripts/network/domr/",
                "router_proxy.sh");
        if (_routerProxyPath == null) {
            throw new ConfigurationException(
                    "Unable to find the router_proxy.sh");
        }

        String value = (String) params.get("developer");
        boolean isDeveloper = Boolean.parseBoolean(value);

        if (isDeveloper) {
            params.putAll(getDeveloperProperties());
        }

        _pool = (String) params.get("pool");
        if (_pool == null) {
            _pool = "/root";
        }

        String instance = (String) params.get("instance");

        _hypervisorType = (String) params.get("hypervisor.type");
        if (_hypervisorType == null) {
            _hypervisorType = "kvm";
        }

        _hypervisorURI = (String) params.get("hypervisor.uri");
        if (_hypervisorURI == null) {
            _hypervisorURI = "qemu:///system";
        }
        String startMac = (String) params.get("private.macaddr.start");
        if (startMac == null) {
            startMac = "00:16:3e:77:e2:a0";
        }

        String startIp = (String) params.get("private.ipaddr.start");
        if (startIp == null) {
            startIp = "192.168.166.128";
        }

        _pingTestPath = Script.findScript(kvmScriptsDir, "pingtest.sh");
        if (_pingTestPath == null) {
            throw new ConfigurationException("Unable to find the pingtest.sh");
        }

        _linkLocalBridgeName = (String) params.get("private.bridge.name");
        if (_linkLocalBridgeName == null) {
            if (isDeveloper) {
                _linkLocalBridgeName = "cloud-" + instance + "-0";
            } else {
                _linkLocalBridgeName = "cloud0";
            }
        }

        _publicBridgeName = (String) params.get("public.network.device");
        if (_publicBridgeName == null) {
            _publicBridgeName = "cloudbr0";
        }

        _privBridgeName = (String) params.get("private.network.device");
        if (_privBridgeName == null) {
            _privBridgeName = "cloudbr1";
        }

        _guestBridgeName = (String) params.get("guest.network.device");
        if (_guestBridgeName == null) {
            _guestBridgeName = _privBridgeName;
        }

        _privNwName = (String) params.get("private.network.name");
        if (_privNwName == null) {
            if (isDeveloper) {
                _privNwName = "cloud-" + instance + "-private";
            } else {
                _privNwName = "cloud-private";
            }
        }

        _localStoragePath = (String) params.get("local.storage.path");
        if (_localStoragePath == null) {
            _localStoragePath = "/var/lib/libvirt/images/";
        }

        _localStorageUUID = (String) params.get("local.storage.uuid");
        if (_localStorageUUID == null) {
            throw new ConfigurationException("local.storage.uuid is not set! Please set this to a valid UUID");
        }

        value = (String) params.get("scripts.timeout");
        _timeout = NumbersUtil.parseInt(value, 30 * 60) * 1000;

        value = (String) params.get("stop.script.timeout");
        _stopTimeout = NumbersUtil.parseInt(value, 120) * 1000;

        value = (String) params.get("cmds.timeout");
        _cmdsTimeout = NumbersUtil.parseInt(value, 7200) * 1000;

        value = (String) params.get("host.reserved.mem.mb");
        _dom0MinMem = NumbersUtil.parseInt(value, 0) * 1024 * 1024;

        LibvirtConnection.initialize(_hypervisorURI);
        Connect conn = null;
        try {
            conn = LibvirtConnection.getConnection();

            if (_bridgeType == BridgeType.OPENVSWITCH) {
                if (conn.getLibVirVersion() < (9 * 1000 + 11)) {
                    throw new ConfigurationException("LibVirt version 0.9.11 required for openvswitch support, but version "
                            + conn.getLibVirVersion() + " detected");
                }
            }
        } catch (LibvirtException e) {
            throw new CloudRuntimeException(e.getMessage());
        }

        /* Does node support HVM guest? If not, exit */
        if (!IsHVMEnabled(conn)) {
            throw new ConfigurationException(
                    "NO HVM support on this machine, please make sure: "
                            + "1. VT/SVM is supported by your CPU, or is enabled in BIOS. "
                            + "2. kvm modules are loaded (kvm, kvm_amd|kvm_intel)");
        }

        _hypervisorPath = getHypervisorPath(conn);
        try {
            _hvVersion = conn.getVersion();
            _hvVersion = (_hvVersion % 1000000) / 1000;
        } catch (LibvirtException e) {
            s_logger.trace("Ignoring libvirt error.", e);
        }

        String[] info = NetUtils.getNetworkParams(_privateNic);

        _monitor = new KVMHAMonitor(null, info[0], _heartBeatPath);
        Thread ha = new Thread(_monitor);
        ha.start();

        _storagePoolMgr = new KVMStoragePoolManager(_storage, _monitor);

        _sysvmISOPath = (String) params.get("systemvm.iso.path");
        if (_sysvmISOPath == null) {
            String[] isoPaths = {"/usr/share/cloudstack-common/vms/systemvm.iso"};
            for (String isoPath : isoPaths) {
                if (_storage.exists(isoPath)) {
                    _sysvmISOPath = isoPath;
                    break;
                }
            }
            if (_sysvmISOPath == null) {
                s_logger.debug("Can't find system vm ISO");
            }
        }

        switch (_bridgeType) {
        case OPENVSWITCH:
            getOvsPifs();
            break;
        case NATIVE:
        default:
        getPifs();
            break;
        }

        if (_pifs.get("private") == null) {
            s_logger.debug("Failed to get private nic name");
            throw new ConfigurationException("Failed to get private nic name");
        }

        if (_pifs.get("public") == null) {
            s_logger.debug("Failed to get public nic name");
            throw new ConfigurationException("Failed to get public nic name");
        }
        s_logger.debug("Found pif: " + _pifs.get("private") + " on " + _privBridgeName
                + ", pif: " + _pifs.get("public") + " on " + _publicBridgeName);


        _can_bridge_firewall = can_bridge_firewall(_pifs.get("public"));

        _localGateway = Script
                .runSimpleBashScript("ip route |grep default|awk '{print $3}'");
        if (_localGateway == null) {
            s_logger.debug("Failed to found the local gateway");
        }

        _mountPoint = (String) params.get("mount.path");
        if (_mountPoint == null) {
            _mountPoint = "/mnt";
        }

        value = (String) params.get("vm.migrate.speed");
        _migrateSpeed = NumbersUtil.parseInt(value, -1);
        if (_migrateSpeed == -1) {
            //get guest network device speed
            _migrateSpeed = 0;
            String speed = Script.runSimpleBashScript("ethtool " + _pifs.get("public") + " |grep Speed | cut -d \\  -f 2");
            if (speed != null) {
                String[] tokens = speed.split("M");
                if (tokens.length == 2) {
                    try {
                        _migrateSpeed = Integer.parseInt(tokens[0]);
                    } catch (NumberFormatException e) {
                        s_logger.trace("Ignoring migrateSpeed extraction error.", e);
                    }
                    s_logger.debug("device " + _pifs.get("public") + " has speed: " + String.valueOf(_migrateSpeed));
                }
            }
            params.put("vm.migrate.speed", String.valueOf(_migrateSpeed));
        }

        Map<String, String> bridges = new HashMap<String, String>();
        bridges.put("linklocal", _linkLocalBridgeName);
        bridges.put("public", _publicBridgeName);
        bridges.put("private", _privBridgeName);
        bridges.put("guest", _guestBridgeName);

        params.put("libvirt.host.bridges", bridges);
        params.put("libvirt.host.pifs", _pifs);

        params.put("libvirt.computing.resource", this);

        configureVifDrivers(params);

        return true;
    }

    protected void configureVifDrivers(Map<String, Object> params)
            throws ConfigurationException {
        final String LIBVIRT_VIF_DRIVER = "libvirt.vif.driver";

        _trafficTypeVifDrivers = new HashMap<TrafficType, VifDriver>();

        // Load the default vif driver
        String defaultVifDriverName = (String) params.get(LIBVIRT_VIF_DRIVER);
        if (defaultVifDriverName == null) {
            if (_bridgeType == BridgeType.OPENVSWITCH) {
                s_logger.info("No libvirt.vif.driver specified. Defaults to OvsVifDriver.");
                defaultVifDriverName = DEFAULT_OVS_VIF_DRIVER_CLASS_NAME;
            } else {
                s_logger.info("No libvirt.vif.driver specified. Defaults to BridgeVifDriver.");
                defaultVifDriverName = DEFAULT_BRIDGE_VIF_DRIVER_CLASS_NAME;
            }
        }
        _defaultVifDriver = getVifDriverClass(defaultVifDriverName, params);

        // Load any per-traffic-type vif drivers
        for (Map.Entry<String, Object> entry : params.entrySet())
        {
            String k = entry.getKey();
            String vifDriverPrefix = LIBVIRT_VIF_DRIVER + ".";

            if(k.startsWith(vifDriverPrefix)){
                // Get trafficType
                String trafficTypeSuffix = k.substring(vifDriverPrefix.length());

                // Does this suffix match a real traffic type?
                TrafficType trafficType = TrafficType.getTrafficType(trafficTypeSuffix);
                if(!trafficType.equals(TrafficType.None)){
                    // Get vif driver class name
                    String vifDriverClassName = (String) entry.getValue();
                    // if value is null, ignore
                    if(vifDriverClassName != null){
                        // add traffic type to vif driver mapping to Map
                        _trafficTypeVifDrivers.put(trafficType,
                                getVifDriverClass(vifDriverClassName, params));
                    }
                }
            }
        }
    }

    protected VifDriver getVifDriverClass(String vifDriverClassName, Map<String, Object> params)
            throws ConfigurationException {
        VifDriver vifDriver;

        try {
            Class<?> clazz = Class.forName(vifDriverClassName);
            vifDriver = (VifDriver) clazz.newInstance();
            vifDriver.configure(params);
        } catch (ClassNotFoundException e) {
            throw new ConfigurationException("Unable to find class for libvirt.vif.driver " + e);
        } catch (InstantiationException e) {
            throw new ConfigurationException("Unable to instantiate class for libvirt.vif.driver " + e);
        } catch (IllegalAccessException e) {
            throw new ConfigurationException("Unable to instantiate class for libvirt.vif.driver " + e);
        }
        return vifDriver;
    }

    protected VifDriver getVifDriver(TrafficType trafficType){
        VifDriver vifDriver = _trafficTypeVifDrivers.get(trafficType);

        if(vifDriver == null){
            vifDriver = _defaultVifDriver;
        }

        return vifDriver;
    }

    protected List<VifDriver> getAllVifDrivers(){
        Set<VifDriver> vifDrivers = new HashSet<VifDriver>();

        vifDrivers.add(_defaultVifDriver);
        vifDrivers.addAll(_trafficTypeVifDrivers.values());

        ArrayList<VifDriver> vifDriverList = new ArrayList<VifDriver>(vifDrivers);

        return vifDriverList;
    }

    private void getPifs() {
        File dir = new File("/sys/devices/virtual/net");
        File[] netdevs = dir.listFiles();
        List<String> bridges = new ArrayList<String>();
        for (int i = 0; i < netdevs.length; i++) {
            File isbridge = new File(netdevs[i].getAbsolutePath() + "/bridge");
            String netdevName = netdevs[i].getName();
            s_logger.debug("looking in file " + netdevs[i].getAbsolutePath() + "/bridge");
            if (isbridge.exists()) {
                s_logger.debug("Found bridge " + netdevName);
                bridges.add(netdevName);
            }
        }

        for (String bridge : bridges) {
            s_logger.debug("looking for pif for bridge " + bridge);
            String pif = getPif(bridge);
            if(_publicBridgeName != null && bridge.equals(_publicBridgeName)){
                _pifs.put("public", pif);
            }
            if (_guestBridgeName != null && bridge.equals(_guestBridgeName)) {
                _pifs.put("private", pif);
            }
            _pifs.put(bridge, pif);
        }
        s_logger.debug("done looking for pifs, no more bridges");
    }

    private void getOvsPifs() {
        String cmdout = Script.runSimpleBashScript("ovs-vsctl list-br | sed '{:q;N;s/\\n/%/g;t q}'");
        s_logger.debug("cmdout was " + cmdout);
        List<String> bridges = Arrays.asList(cmdout.split("%"));
        for (String bridge : bridges) {
            s_logger.debug("looking for pif for bridge " + bridge);
            // String pif = getOvsPif(bridge);
            // Not really interested in the pif name at this point for ovs
            // bridges
            String pif = bridge;
            if (_publicBridgeName != null && bridge.equals(_publicBridgeName)) {
                _pifs.put("public", pif);
            }
            if (_guestBridgeName != null && bridge.equals(_guestBridgeName)) {
                _pifs.put("private", pif);
            }
            _pifs.put(bridge, pif);
        }
        s_logger.debug("done looking for pifs, no more bridges");
    }

    private String getPif(String bridge) {
        String pif = matchPifFileInDirectory(bridge);
        File vlanfile = new File("/proc/net/vlan/" + pif);

        if (vlanfile.isFile()) {
                pif = Script.runSimpleBashScript("grep ^Device\\: /proc/net/vlan/"
                                                  + pif + " | awk {'print $2'}");
        }

        return pif;
    }

    private String getOvsPif(String bridge) {
        String pif = Script.runSimpleBashScript("ovs-vsctl list-ports " + bridge);
        return pif;
    }
    
    private String matchPifFileInDirectory(String bridgeName){
        File f = new File("/sys/devices/virtual/net/" + bridgeName + "/brif");

        if (! f.isDirectory()){
            s_logger.debug("failing to get physical interface from bridge"
                           + bridgeName + ", does " + f.getAbsolutePath() 
                           + "exist?");
            return "";
        }

        File[] interfaces = f.listFiles();

        for (int i = 0; i < interfaces.length; i++) {
            String fname = interfaces[i].getName();
            s_logger.debug("matchPifFileInDirectory: file name '"+fname+"'");
            if (fname.startsWith("eth") || fname.startsWith("bond")
                || fname.startsWith("vlan") || fname.startsWith("em")) {
                return fname;
            }
        }

        s_logger.debug("failing to get physical interface from bridge"
                        + bridgeName + ", did not find an eth*, bond*, or vlan* in "
                        + f.getAbsolutePath());
        return "";
    }


    private boolean checkNetwork(String networkName) {
        if (networkName == null) {
            return true;
        }

        if (_bridgeType == BridgeType.OPENVSWITCH) {
            return checkOvsNetwork(networkName);
        } else {
            return checkBridgeNetwork(networkName);
        }
    }

    private boolean checkBridgeNetwork(String networkName) {
        if (networkName == null) {
            return true;
        }

        String name = matchPifFileInDirectory(networkName);

        if (name == null || name.isEmpty()) {
            return false;
        } else {
            return true;
        }
    }

    private boolean checkOvsNetwork(String networkName) {
        s_logger.debug("Checking if network " + networkName + " exists as openvswitch bridge");
        if (networkName == null) {
            return true;
        }

        Script command = new Script("/bin/sh", _timeout);
        command.add("-c");
        command.add("ovs-vsctl br-exists " + networkName);
        String result = command.execute(null);
        if ("Ok".equals(result)) {
            return true;
        } else {
            return false;
        }
    }

    private String getVnetId(String vnetId) {
        return vnetId;
    }

    private void passCmdLine(String vmName, String cmdLine) 
            throws InternalErrorException {
        final Script command = new Script(_patchViaSocketPath, _timeout, s_logger);
        String result;
        command.add("-n",vmName);
        command.add("-p", cmdLine.replaceAll(" ", "%"));
        result = command.execute();
        if (result != null) {
            throw new InternalErrorException(result);
        }
    }

    boolean isDirectAttachedNetwork(String type) {
        if ("untagged".equalsIgnoreCase(type)) {
            return true;
        } else {
            try {
                Long.valueOf(type);
            } catch (NumberFormatException e) {
                return true;
            }
            return false;
        }
    }

    protected String startVM(Connect conn, String vmName, String domainXML)
            throws LibvirtException, InternalErrorException {
        try {
            /*
                We create a transient domain here. When this method gets
                called we receive a full XML specification of the guest,
                so no need to define it persistent.

                This also makes sure we never have any old "garbage" defined
                in libvirt which might haunt us.
            */
            conn.domainCreateXML(domainXML, 0);
        } catch (final LibvirtException e) {
            s_logger.warn("Failed to start domain " + vmName + ": "
                    + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public boolean stop() {
        try {
            Connect conn = LibvirtConnection.getConnection();
            conn.close();
        } catch (LibvirtException e) {
            s_logger.trace("Ignoring libvirt error.", e);
        }

        return true;
    }

    @Override
    public Answer executeRequest(Command cmd) {

        try {
            if (cmd instanceof StopCommand) {
                return execute((StopCommand) cmd);
            } else if (cmd instanceof GetVmStatsCommand) {
                return execute((GetVmStatsCommand) cmd);
            } else if (cmd instanceof RebootRouterCommand) {
                return execute((RebootRouterCommand) cmd);
            } else if (cmd instanceof RebootCommand) {
                return execute((RebootCommand) cmd);
            } else if (cmd instanceof GetHostStatsCommand) {
                return execute((GetHostStatsCommand) cmd);
            } else if (cmd instanceof CheckStateCommand) {
                return executeRequest(cmd);
            } else if (cmd instanceof CheckHealthCommand) {
                return execute((CheckHealthCommand) cmd);
            } else if (cmd instanceof PrepareForMigrationCommand) {
                return execute((PrepareForMigrationCommand) cmd);
            } else if (cmd instanceof MigrateCommand) {
                return execute((MigrateCommand) cmd);
            } else if (cmd instanceof PingTestCommand) {
                return execute((PingTestCommand) cmd);
            } else if (cmd instanceof CheckVirtualMachineCommand) {
                return execute((CheckVirtualMachineCommand) cmd);
            } else if (cmd instanceof ReadyCommand) {
                return execute((ReadyCommand) cmd);
            } else if (cmd instanceof AttachIsoCommand) {
                return execute((AttachIsoCommand) cmd);
            } else if (cmd instanceof AttachVolumeCommand) {
                return execute((AttachVolumeCommand) cmd);
            } else if (cmd instanceof StopCommand) {
                return execute((StopCommand) cmd);
            } else if (cmd instanceof CheckConsoleProxyLoadCommand) {
                return execute((CheckConsoleProxyLoadCommand) cmd);
            } else if (cmd instanceof WatchConsoleProxyLoadCommand) {
                return execute((WatchConsoleProxyLoadCommand) cmd);
            } else if (cmd instanceof GetVncPortCommand) {
                return execute((GetVncPortCommand) cmd);
            } else if (cmd instanceof ModifySshKeysCommand) {
                return execute((ModifySshKeysCommand) cmd);
            } else if (cmd instanceof MaintainCommand) {
                return execute((MaintainCommand) cmd);
            } else if (cmd instanceof CreateCommand) {
                return execute((CreateCommand) cmd);
            } else if (cmd instanceof DestroyCommand) {
                return execute((DestroyCommand) cmd);
            } else if (cmd instanceof PrimaryStorageDownloadCommand) {
                return execute((PrimaryStorageDownloadCommand) cmd);
            } else if (cmd instanceof CreatePrivateTemplateFromVolumeCommand) {
                return execute((CreatePrivateTemplateFromVolumeCommand) cmd);
            } else if (cmd instanceof GetStorageStatsCommand) {
                return execute((GetStorageStatsCommand) cmd);
            } else if (cmd instanceof ManageSnapshotCommand) {
                return execute((ManageSnapshotCommand) cmd);
            } else if (cmd instanceof BackupSnapshotCommand) {
                return execute((BackupSnapshotCommand) cmd);
            } else if (cmd instanceof CreateVolumeFromSnapshotCommand) {
                return execute((CreateVolumeFromSnapshotCommand) cmd);
            } else if (cmd instanceof CreatePrivateTemplateFromSnapshotCommand) {
                return execute((CreatePrivateTemplateFromSnapshotCommand) cmd);
            } else if (cmd instanceof UpgradeSnapshotCommand) {
                return execute((UpgradeSnapshotCommand) cmd);
            } else if (cmd instanceof CreateStoragePoolCommand) {
                return execute((CreateStoragePoolCommand) cmd);
            } else if (cmd instanceof ModifyStoragePoolCommand) {
                return execute((ModifyStoragePoolCommand) cmd);
            } else if (cmd instanceof SecurityGroupRulesCmd) {
                return execute((SecurityGroupRulesCmd) cmd);
            } else if (cmd instanceof DeleteStoragePoolCommand) {
                return execute((DeleteStoragePoolCommand) cmd);
            } else if (cmd instanceof FenceCommand) {
                return execute((FenceCommand) cmd);
            } else if (cmd instanceof StartCommand) {
                return execute((StartCommand) cmd);
            } else if (cmd instanceof PlugNicCommand) {
                return execute((PlugNicCommand) cmd);
            } else if (cmd instanceof UnPlugNicCommand) {
                return execute((UnPlugNicCommand) cmd);
            } else if (cmd instanceof SetupGuestNetworkCommand) {
                return execute((SetupGuestNetworkCommand) cmd);
            } else if (cmd instanceof SetNetworkACLCommand) {
                return execute((SetNetworkACLCommand) cmd);
            } else if (cmd instanceof SetSourceNatCommand) {
                return execute((SetSourceNatCommand) cmd);
            } else if (cmd instanceof IpAssocVpcCommand) {
                return execute((IpAssocVpcCommand) cmd);
            } else if (cmd instanceof IpAssocCommand) {
                return execute((IpAssocCommand) cmd);
            } else if (cmd instanceof NetworkElementCommand) {
                return _virtRouterResource.executeRequest(cmd);
            } else if (cmd instanceof CheckSshCommand) {
                return execute((CheckSshCommand) cmd);
            } else if (cmd instanceof NetworkUsageCommand) {
                return execute((NetworkUsageCommand) cmd);
            } else if (cmd instanceof NetworkRulesSystemVmCommand) {
                return execute((NetworkRulesSystemVmCommand) cmd);
            } else if (cmd instanceof CleanupNetworkRulesCmd) {
                return execute((CleanupNetworkRulesCmd) cmd);
            } else if (cmd instanceof CopyVolumeCommand) {
                return execute((CopyVolumeCommand) cmd);
            } else if (cmd instanceof ResizeVolumeCommand) {
                return execute((ResizeVolumeCommand) cmd);
            } else if (cmd instanceof CheckNetworkCommand) {
                return execute((CheckNetworkCommand) cmd);
            } else if (cmd instanceof NetworkRulesVmSecondaryIpCommand) {
                return execute((NetworkRulesVmSecondaryIpCommand) cmd);
            } else {
                s_logger.warn("Unsupported command ");
                return Answer.createUnsupportedCommandAnswer(cmd);
            }
        } catch (final IllegalArgumentException e) {
            return new Answer(cmd, false, e.getMessage());
        }
    }

    private CheckNetworkAnswer execute(CheckNetworkCommand cmd) {
        List<PhysicalNetworkSetupInfo> phyNics = cmd
                .getPhysicalNetworkInfoList();
        String errMsg = null;
        for (PhysicalNetworkSetupInfo nic : phyNics) {
            if (!checkNetwork(nic.getGuestNetworkName())) {
                errMsg = "Can not find network: " + nic.getGuestNetworkName();
                break;
            } else if (!checkNetwork(nic.getPrivateNetworkName())) {
                errMsg = "Can not find network: " + nic.getPrivateNetworkName();
                break;
            } else if (!checkNetwork(nic.getPublicNetworkName())) {
                errMsg = "Can not find network: " + nic.getPublicNetworkName();
                break;
            }
        }

        if (errMsg != null) {
            return new CheckNetworkAnswer(cmd, false, errMsg);
        } else {
            return new CheckNetworkAnswer(cmd, true, null);
        }
    }

    private CopyVolumeAnswer execute(CopyVolumeCommand cmd) {
        boolean copyToSecondary = cmd.toSecondaryStorage();
        String volumePath = cmd.getVolumePath();
        StorageFilerTO pool = cmd.getPool();
        String secondaryStorageUrl = cmd.getSecondaryStorageURL();
        KVMStoragePool secondaryStoragePool = null;
        try {
            KVMStoragePool primaryPool = _storagePoolMgr.getStoragePool(
                    pool.getType(),
                    pool.getUuid());
            String volumeName = UUID.randomUUID().toString();

            if (copyToSecondary) {
                String destVolumeName = volumeName + ".qcow2";
                KVMPhysicalDisk volume = primaryPool.getPhysicalDisk(cmd
                        .getVolumePath());
                String volumeDestPath = "/volumes/" + cmd.getVolumeId()
                        + File.separator;
                secondaryStoragePool = _storagePoolMgr.getStoragePoolByURI(
                        secondaryStorageUrl);
                secondaryStoragePool.createFolder(volumeDestPath);
                secondaryStoragePool.delete();
                secondaryStoragePool = _storagePoolMgr.getStoragePoolByURI(
                        secondaryStorageUrl
                        + volumeDestPath);
                _storagePoolMgr.copyPhysicalDisk(volume,
                        destVolumeName,secondaryStoragePool);
                return new CopyVolumeAnswer(cmd, true, null, null, volumeName);
            } else {
                volumePath = "/volumes/" + cmd.getVolumeId() + File.separator;
                secondaryStoragePool = _storagePoolMgr.getStoragePoolByURI(
                        secondaryStorageUrl
                        + volumePath);
                KVMPhysicalDisk volume = secondaryStoragePool
                        .getPhysicalDisk(cmd.getVolumePath() + ".qcow2");
                _storagePoolMgr.copyPhysicalDisk(volume, volumeName,
                        primaryPool);
                return new CopyVolumeAnswer(cmd, true, null, null, volumeName);
            }
        } catch (CloudRuntimeException e) {
            return new CopyVolumeAnswer(cmd, false, e.toString(), null, null);
        } finally {
            if (secondaryStoragePool != null) {
                secondaryStoragePool.delete();
            }
        }
    }

    protected Answer execute(DeleteStoragePoolCommand cmd) {
        try {
            _storagePoolMgr.deleteStoragePool(cmd.getPool().getType(),
                    cmd.getPool().getUuid());
            return new Answer(cmd);
        } catch (CloudRuntimeException e) {
            return new Answer(cmd, false, e.toString());
        }
    }

    protected FenceAnswer execute(FenceCommand cmd) {
        ExecutorService executors = Executors.newSingleThreadExecutor();
        List<NfsStoragePool> pools = _monitor.getStoragePools();
        KVMHAChecker ha = new KVMHAChecker(pools, cmd.getHostIp());
        Future<Boolean> future = executors.submit(ha);
        try {
            Boolean result = future.get();
            if (result) {
                return new FenceAnswer(cmd, false, "Heart is still beating...");
            } else {
                return new FenceAnswer(cmd);
            }
        } catch (InterruptedException e) {
            s_logger.warn("Unable to fence", e);
            return new FenceAnswer(cmd, false, e.getMessage());
        } catch (ExecutionException e) {
            s_logger.warn("Unable to fence", e);
            return new FenceAnswer(cmd, false, e.getMessage());
        }

    }

    protected Storage.StorageResourceType getStorageResourceType() {
        return Storage.StorageResourceType.STORAGE_POOL;
    }

    protected Answer execute(CreateCommand cmd) {
        StorageFilerTO pool = cmd.getPool();
        DiskProfile dskch = cmd.getDiskCharacteristics();
        KVMPhysicalDisk BaseVol = null;
        KVMStoragePool primaryPool = null;
        KVMPhysicalDisk vol = null;
        long disksize;
        try {
            primaryPool = _storagePoolMgr.getStoragePool(pool.getType(),
                    pool.getUuid());
            disksize = dskch.getSize();

            if (cmd.getTemplateUrl() != null) {
                if(primaryPool.getType() == StoragePoolType.CLVM) { 
                    vol = templateToPrimaryDownload(cmd.getTemplateUrl(),primaryPool);
                } else {
                    BaseVol = primaryPool.getPhysicalDisk(cmd.getTemplateUrl());
                    vol = _storagePoolMgr.createDiskFromTemplate(BaseVol, UUID
                            .randomUUID().toString(), primaryPool);
                }
                if (vol == null) {
                    return new Answer(cmd, false,
                            " Can't create storage volume on storage pool");
                }
            } else {
                vol = primaryPool.createPhysicalDisk(UUID.randomUUID()
                        .toString(), dskch.getSize());
            }
            VolumeTO volume = new VolumeTO(cmd.getVolumeId(), dskch.getType(),
                    pool.getType(), pool.getUuid(), pool.getPath(),
                    vol.getName(), vol.getName(), disksize, null);
            return new CreateAnswer(cmd, volume);
        } catch (CloudRuntimeException e) {
            s_logger.debug("Failed to create volume: " + e.toString());
            return new CreateAnswer(cmd, e);
        }
    }

    // this is much like PrimaryStorageDownloadCommand, but keeping it separate
    protected KVMPhysicalDisk templateToPrimaryDownload(String templateUrl, KVMStoragePool primaryPool) {
        int index = templateUrl.lastIndexOf("/");
        String mountpoint = templateUrl.substring(0, index);
        String templateName = null;
        if (index < templateUrl.length() - 1) {
            templateName = templateUrl.substring(index + 1);
        }

        KVMPhysicalDisk templateVol = null;
        KVMStoragePool secondaryPool = null;
        try {
            secondaryPool = _storagePoolMgr.getStoragePoolByURI(mountpoint);
            /* Get template vol */
            if (templateName == null) {
                secondaryPool.refresh();
                List<KVMPhysicalDisk> disks = secondaryPool.listPhysicalDisks();
                if (disks == null || disks.isEmpty()) {
                    s_logger.error("Failed to get volumes from pool: " + secondaryPool.getUuid());
                    return null;
                }
                for (KVMPhysicalDisk disk : disks) {
                    if (disk.getName().endsWith("qcow2")) {
                        templateVol = disk;
                        break;
                    }
                }
                if (templateVol == null) {
                    s_logger.error("Failed to get template from pool: " + secondaryPool.getUuid());
                    return null;
                }
            } else {
                templateVol = secondaryPool.getPhysicalDisk(templateName);
            }

            /* Copy volume to primary storage */

            KVMPhysicalDisk primaryVol = _storagePoolMgr.copyPhysicalDisk(templateVol, UUID.randomUUID().toString(), primaryPool);
            return primaryVol;
        } catch (CloudRuntimeException e) {
            s_logger.error("Failed to download template to primary storage",e);
            return null;
        } finally {
            if (secondaryPool != null) {
                secondaryPool.delete();
            }
        }
    }

    private String getResizeScriptType (KVMStoragePool pool, KVMPhysicalDisk vol) {
        StoragePoolType poolType = pool.getType();
        PhysicalDiskFormat volFormat = vol.getFormat();
         
        if(pool.getType() == StoragePoolType.CLVM && volFormat == KVMPhysicalDisk.PhysicalDiskFormat.RAW) {
            return "CLVM";
        } else if ((poolType == StoragePoolType.NetworkFilesystem
                  || poolType == StoragePoolType.SharedMountPoint
                  || poolType == StoragePoolType.Filesystem)
                  && volFormat == KVMPhysicalDisk.PhysicalDiskFormat.QCOW2 ) {
            return "QCOW2";
        }
        return null;
    }

    /* uses a local script now, eventually support for virStorageVolResize() will maybe work on 
       qcow2 and lvm and we can do this in libvirt calls */
    public Answer execute(ResizeVolumeCommand cmd) {
        String volid = cmd.getPath();
        long newSize = cmd.getNewSize();
        long currentSize = cmd.getCurrentSize();
        String vmInstanceName = cmd.getInstanceName();
        boolean shrinkOk = cmd.getShrinkOk();
        StorageFilerTO spool = cmd.getPool();

        try {
            KVMStoragePool pool = _storagePoolMgr.getStoragePool(spool.getType(), spool.getUuid());
            KVMPhysicalDisk vol = pool.getPhysicalDisk(volid);
            String path = vol.getPath();
            String type = getResizeScriptType(pool, vol);

            if (type == null) {
                return new ResizeVolumeAnswer(cmd, false, "Unsupported volume format: pool type '" 
                                + pool.getType() + "' and volume format '" + vol.getFormat() + "'");
            } else if (type.equals("QCOW2") && shrinkOk) {
                return new ResizeVolumeAnswer(cmd, false, "Unable to shrink volumes of type " + type);
            }

            s_logger.debug("got to the stage where we execute the volume resize, params:" 
                           + path + "," + currentSize + "," + newSize + "," + type + "," + vmInstanceName + "," + shrinkOk);
            final Script resizecmd = new Script(_resizeVolumePath,
                        _cmdsTimeout, s_logger); 
            resizecmd.add("-s",String.valueOf(newSize));
            resizecmd.add("-c",String.valueOf(currentSize));
            resizecmd.add("-p",path);
            resizecmd.add("-t",type);
            resizecmd.add("-r",String.valueOf(shrinkOk));
            resizecmd.add("-v",vmInstanceName);
            String result = resizecmd.execute();

            if (result == null) {

                /* fetch new size as seen from libvirt, don't want to assume anything */
                pool = _storagePoolMgr.getStoragePool(spool.getType(), spool.getUuid());
                long finalSize = pool.getPhysicalDisk(volid).getVirtualSize();
                s_logger.debug("after resize, size reports as " + finalSize + ", requested " + newSize);
                return new ResizeVolumeAnswer(cmd, true, "success", finalSize);
            }

            return new ResizeVolumeAnswer(cmd, false, result);
        } catch (CloudRuntimeException e) {
            String error = "failed to resize volume: " + e;
            s_logger.debug(error);
            return new ResizeVolumeAnswer(cmd, false, error);
        }
        
    } 

    public Answer execute(DestroyCommand cmd) {
        VolumeTO vol = cmd.getVolume();

        try {
            KVMStoragePool pool = _storagePoolMgr.getStoragePool(
                    vol.getPoolType(),
                    vol.getPoolUuid());
            pool.deletePhysicalDisk(vol.getPath());
            String vmName = cmd.getVmName();
            String poolPath = pool.getLocalPath();
            return new Answer(cmd, true, "Success");
        } catch (CloudRuntimeException e) {
            s_logger.debug("Failed to delete volume: " + e.toString());
            return new Answer(cmd, false, e.toString());
        }
    }

    private String getVlanIdFromBridge(String brName) {
        String pif= matchPifFileInDirectory(brName);
        String[] pifparts = pif.split("\\.");

        if(pifparts.length == 2) {
            return pifparts[1];
        } else {
            s_logger.debug("failed to get vlan id from bridge " + brName 
                           + "attached to physical interface" + pif);
            return "";
        }
    }

    private void VifHotPlug(Connect conn, String vmName, String vlanId,
            String macAddr) throws InternalErrorException, LibvirtException {
        NicTO nicTO = new NicTO();
        nicTO.setMac(macAddr);
        nicTO.setType(TrafficType.Public);
        if (vlanId == null) {
            nicTO.setBroadcastType(BroadcastDomainType.Native);
        } else {
            nicTO.setBroadcastType(BroadcastDomainType.Vlan);
            nicTO.setBroadcastUri(BroadcastDomainType.Vlan.toUri(vlanId));
        }

        Domain vm = getDomain(conn, vmName);
        vm.attachDevice(getVifDriver(nicTO.getType()).plug(nicTO, "Other PV (32-bit)").toString());
    }

    private PlugNicAnswer execute(PlugNicCommand cmd) {
        NicTO nic = cmd.getNic();
        String vmName = cmd.getVmName();
        try {
            Connect conn = LibvirtConnection.getConnection();
            Domain vm = getDomain(conn, vmName);
            List<InterfaceDef> pluggedNics = getInterfaces(conn, vmName);
            Integer nicnum = 0;
            for (InterfaceDef pluggedNic : pluggedNics) {
                if (pluggedNic.getMacAddress().equalsIgnoreCase(nic.getMac())) {
                    s_logger.debug("found existing nic for mac "+ pluggedNic.getMacAddress() + " at index "+nicnum);
                    return new PlugNicAnswer(cmd, true, "success");
                }
                nicnum++;
            }
            vm.attachDevice(getVifDriver(nic.getType()).plug(nic, "Other PV (32-bit)").toString());
            return new PlugNicAnswer(cmd, true, "success");
        } catch (LibvirtException e) {
            String msg = " Plug Nic failed due to " + e.toString();
            s_logger.warn(msg, e);
            return new PlugNicAnswer(cmd, false, msg);
        } catch (InternalErrorException e) {
            String msg = " Plug Nic failed due to " + e.toString();
            s_logger.warn(msg, e);
            return new PlugNicAnswer(cmd, false, msg);
        }
    }

    private UnPlugNicAnswer execute(UnPlugNicCommand cmd) {
        Connect conn;
        NicTO nic = cmd.getNic();
        String vmName = cmd.getInstanceName();
        try {
            conn = LibvirtConnection.getConnection();
            Domain vm = getDomain(conn, vmName);
            List<InterfaceDef> pluggedNics = getInterfaces(conn, vmName);
            for (InterfaceDef pluggedNic : pluggedNics) {
                if (pluggedNic.getMacAddress().equalsIgnoreCase(nic.getMac())) {
                    vm.detachDevice(pluggedNic.toString());
                    return new UnPlugNicAnswer(cmd, true, "success");
                }
            }
            return new UnPlugNicAnswer(cmd, true, "success");
        } catch (LibvirtException e) {
            String msg = " Unplug Nic failed due to " + e.toString();
            s_logger.warn(msg, e);
            return new UnPlugNicAnswer(cmd, false, msg);
        }
    }

    private SetupGuestNetworkAnswer execute(SetupGuestNetworkCommand cmd) {
        Connect conn;
        NicTO nic = cmd.getNic();
        String routerIP = cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);
        String routerGIP = cmd.getAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP);
        String routerName = cmd.getAccessDetail(NetworkElementCommand.ROUTER_NAME);
        String gateway = cmd.getAccessDetail(NetworkElementCommand.GUEST_NETWORK_GATEWAY);
        String cidr = Long.toString(NetUtils.getCidrSize(nic.getNetmask()));;
        String domainName = cmd.getNetworkDomain();
        String dns = cmd.getDefaultDns1();

        if (dns == null || dns.isEmpty()) {
            dns = cmd.getDefaultDns2();
        } else {
            String dns2= cmd.getDefaultDns2();
            if ( dns2 != null && !dns2.isEmpty()) {
                dns += "," + dns2;
            }
        }

        try {
            conn = LibvirtConnection.getConnection();
            Domain vm = getDomain(conn, routerName);
            List<InterfaceDef> pluggedNics = getInterfaces(conn, routerName);
            InterfaceDef routerNic = null;

            for (InterfaceDef pluggedNic : pluggedNics) {
                if (pluggedNic.getMacAddress().equalsIgnoreCase(nic.getMac())) {
                    routerNic = pluggedNic;
                    break;
                }
            }

            if ( routerNic == null ) {
                return new SetupGuestNetworkAnswer(cmd, false, "Can not find nic with mac " + nic.getMac() + " for VM " + routerName);
            }

            String args = "vpc_guestnw.sh " + routerIP + " -C";
            String dev = "eth" + nic.getDeviceId();
            String netmask = NetUtils.getSubNet(routerGIP, nic.getNetmask());
            String result = _virtRouterResource.assignGuestNetwork(dev, routerIP,
                    routerGIP, gateway, cidr, netmask, dns, domainName );

            if (result != null) {
                return new SetupGuestNetworkAnswer(cmd, false, "Creating guest network failed due to " + result);
            }
            return new SetupGuestNetworkAnswer(cmd, true, "success");
        } catch (LibvirtException e) {
            String msg = "Creating guest network failed due to " + e.toString();
            s_logger.warn(msg, e);
            return new SetupGuestNetworkAnswer(cmd, false, msg);
        }
    }

    private SetNetworkACLAnswer execute(SetNetworkACLCommand cmd) {
        String[] results = new String[cmd.getRules().length];
        String callResult;
        Connect conn;
        String routerName = cmd.getAccessDetail(NetworkElementCommand.ROUTER_NAME);
        String routerIp = cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);

        try {
            conn = LibvirtConnection.getConnection();
            Domain vm = getDomain(conn, routerName);
            String [][] rules = cmd.generateFwRules();
            String[] aclRules = rules[0];
            NicTO nic = cmd.getNic();
            String dev = "eth" + nic.getDeviceId();
            String netmask = Long.toString(NetUtils.getCidrSize(nic.getNetmask()));
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < aclRules.length; i++) {
                sb.append(aclRules[i]).append(',');
            }

            String rule =  sb.toString();
            String result = _virtRouterResource.assignNetworkACL(routerIp,
                    dev, nic.getIp(), netmask, rule);

            if (result != null) {
                for (int i=0; i < results.length; i++) {
                    results[i] = "Failed";
                }
                return new SetNetworkACLAnswer(cmd, false, results);
            }

            return new SetNetworkACLAnswer(cmd, true, results);
        } catch (LibvirtException e) {
            String msg = "SetNetworkACL failed due to " + e.toString();
            s_logger.error(msg, e);
            return new SetNetworkACLAnswer(cmd, false, results);
        }
    }

    protected SetSourceNatAnswer execute(SetSourceNatCommand cmd) {
        Connect conn;
        String routerName = cmd.getAccessDetail(NetworkElementCommand.ROUTER_NAME);
        String routerIP = cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);
        IpAddressTO pubIP = cmd.getIpAddress();

        try {
            conn = LibvirtConnection.getConnection();
            Domain vm = getDomain(conn, routerName);
            Integer devNum = 0;
            String pubVlan = pubIP.getVlanId();
            List<InterfaceDef> pluggedNics = getInterfaces(conn, routerName);

            for (InterfaceDef pluggedNic : pluggedNics) {
                String pluggedVlanBr = pluggedNic.getBrName();
                String pluggedVlanId = getVlanIdFromBridge(pluggedVlanBr);
                if (pubVlan.equalsIgnoreCase(Vlan.UNTAGGED) 
                        && pluggedVlanBr.equalsIgnoreCase(_publicBridgeName)) {
                    break;
                } else if (pluggedVlanBr.equalsIgnoreCase(_linkLocalBridgeName)){
                    /*skip over, no physical bridge device exists*/
                } else if (pluggedVlanId == null) {
                    /*this should only be true in the case of link local bridge*/
                    return new SetSourceNatAnswer(cmd, false, "unable to find the vlan id for bridge "+pluggedVlanBr+
                            " when attempting to set up" + pubVlan + " on router " + routerName);
                } else if (pluggedVlanId.equals(pubVlan)) {
                    break;
                }
                devNum++;
            }

            String dev = "eth" + devNum;
            String result = _virtRouterResource.assignSourceNat(routerIP, pubIP.getPublicIp(), dev);

            if (result != null) {
                return new SetSourceNatAnswer(cmd, false, "KVM plugin \"vpc_snat\" failed:"+result);
            }
            return new SetSourceNatAnswer(cmd, true, "success");
        } catch (LibvirtException e) {
            String msg = "Ip SNAT failure due to " + e.toString();
            s_logger.error(msg, e);
            return new SetSourceNatAnswer(cmd, false, msg);
        }
    }

    protected IpAssocAnswer execute(IpAssocVpcCommand cmd) {
        Connect conn;
        String[] results = new String[cmd.getIpAddresses().length];
        int i = 0;
        String routerName = cmd.getAccessDetail(NetworkElementCommand.ROUTER_NAME);
        String routerIP = cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);

        try {
            conn = LibvirtConnection.getConnection();
            IpAddressTO[] ips = cmd.getIpAddresses();
            Domain vm = getDomain(conn, routerName);
            Integer devNum = 0;
            Map<String, Integer> vlanToNicNum = new HashMap<String, Integer>();
            List<InterfaceDef> pluggedNics = getInterfaces(conn, routerName);

            for (InterfaceDef pluggedNic : pluggedNics) {
                String pluggedVlan = pluggedNic.getBrName();
                if (pluggedVlan.equalsIgnoreCase(_linkLocalBridgeName)) {
                    vlanToNicNum.put("LinkLocal",devNum); 
                } else if (pluggedVlan.equalsIgnoreCase(_publicBridgeName)
                        || pluggedVlan.equalsIgnoreCase(_privBridgeName)
                        || pluggedVlan.equalsIgnoreCase(_guestBridgeName)) {
                    vlanToNicNum.put(Vlan.UNTAGGED,devNum);
                } else {
                    vlanToNicNum.put(getVlanIdFromBridge(pluggedVlan),devNum);
                }
                devNum++;
            }

            for (IpAddressTO ip : ips) {
                String nicName = "eth" + vlanToNicNum.get(ip.getVlanId());
                String netmask = Long.toString(NetUtils.getCidrSize(ip.getVlanNetmask()));
                String subnet = NetUtils.getSubNet(ip.getPublicIp(), ip.getVlanNetmask());
                _virtRouterResource.assignVpcIpToRouter(routerIP, ip.isAdd(), ip.getPublicIp(),
                        nicName, ip.getVlanGateway(), netmask, subnet);
                results[i++] = ip.getPublicIp() + " - success";
            }

        } catch (LibvirtException e) {
            s_logger.error("Ip Assoc failure on applying one ip due to exception:  ", e);
            results[i++] = IpAssocAnswer.errorResult;
        } catch (InternalErrorException e) {
            s_logger.error("Ip Assoc failure on applying one ip due to exception:  ", e);
            results[i++] = IpAssocAnswer.errorResult;
        }

        return new IpAssocAnswer(cmd, results);
    }

    public Answer execute(IpAssocCommand cmd) {
        String routerName = cmd
                .getAccessDetail(NetworkElementCommand.ROUTER_NAME);
        String routerIp = cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);
        String[] results = new String[cmd.getIpAddresses().length];
        Connect conn;
        try {
            conn = LibvirtConnection.getConnection();
            List<InterfaceDef> nics = getInterfaces(conn, routerName);
            Map<String, Integer> vlanAllocatedToVM = new HashMap<String, Integer>();
            Integer nicPos = 0;
            for (InterfaceDef nic : nics) {
                if (nic.getBrName().equalsIgnoreCase(_linkLocalBridgeName)) {
                    vlanAllocatedToVM.put("LinkLocal", nicPos);
                } else {
                    if (nic.getBrName().equalsIgnoreCase(_publicBridgeName) 
                            || nic.getBrName().equalsIgnoreCase(_privBridgeName) 
                            || nic.getBrName().equalsIgnoreCase(_guestBridgeName)) {
                        vlanAllocatedToVM.put(Vlan.UNTAGGED, nicPos);
                    } else {
                        String vlanId = getVlanIdFromBridge(nic.getBrName());
                        vlanAllocatedToVM.put(vlanId, nicPos);
                    }
                }
                nicPos++;
            }
            IpAddressTO[] ips = cmd.getIpAddresses();
            int i = 0;
            String result = null;
            int nicNum = 0;
            for (IpAddressTO ip : ips) {
                if (!vlanAllocatedToVM.containsKey(ip.getVlanId())) {
                    /* plug a vif into router */
                    VifHotPlug(conn, routerName, ip.getVlanId(),
                            ip.getVifMacAddress());
                    vlanAllocatedToVM.put(ip.getVlanId(), nicPos++);
                }
                nicNum = vlanAllocatedToVM.get(ip.getVlanId());
                networkUsage(routerIp, "addVif", "eth" + nicNum);
                result = _virtRouterResource.assignPublicIpAddress(routerName,
                        routerIp, ip.getPublicIp(), ip.isAdd(), ip.isFirstIP(),
                        ip.isSourceNat(), ip.getVlanId(), ip.getVlanGateway(),
                        ip.getVlanNetmask(), ip.getVifMacAddress(), nicNum);

                if (result != null) {
                    results[i++] = IpAssocAnswer.errorResult;
                } else {
                    results[i++] = ip.getPublicIp() + " - success";
                    ;
                }
            }
            return new IpAssocAnswer(cmd, results);
        } catch (LibvirtException e) {
            return new IpAssocAnswer(cmd, results);
        } catch (InternalErrorException e) {
            return new IpAssocAnswer(cmd, results);
        }
    }

    protected ManageSnapshotAnswer execute(final ManageSnapshotCommand cmd) {
        String snapshotName = cmd.getSnapshotName();
        String snapshotPath = cmd.getSnapshotPath();
        String vmName = cmd.getVmName();
        try {
            Connect conn = LibvirtConnection.getConnection();
            DomainInfo.DomainState state = null;
            Domain vm = null;
            if (vmName != null) {
                try {
                    vm = getDomain(conn, cmd.getVmName());
                    state = vm.getInfo().state;
                } catch (LibvirtException e) {
                    s_logger.trace("Ignoring libvirt error.", e);
                }
            }

            KVMStoragePool primaryPool = _storagePoolMgr.getStoragePool(
                    cmd.getPool().getType(),
                    cmd.getPool().getUuid());

            if (primaryPool.getType() == StoragePoolType.RBD) {
                s_logger.debug("Snapshots are not supported on RBD volumes");
                return new ManageSnapshotAnswer(cmd, false,
                        "Snapshots are not supported on RBD volumes");
            }

            KVMPhysicalDisk disk = primaryPool.getPhysicalDisk(cmd
                    .getVolumePath());
            if (state == DomainInfo.DomainState.VIR_DOMAIN_RUNNING
                    && !primaryPool.isExternalSnapshot()) {
                String vmUuid = vm.getUUIDString();
                Object[] args = new Object[] { snapshotName, vmUuid };
                String snapshot = SnapshotXML.format(args);
                s_logger.debug(snapshot);
                if (cmd.getCommandSwitch().equalsIgnoreCase(
                        ManageSnapshotCommand.CREATE_SNAPSHOT)) {
                    vm.snapshotCreateXML(snapshot);
                } else {
                    DomainSnapshot snap = vm.snapshotLookupByName(snapshotName);
                    snap.delete(0);
                }

                /*
                 * libvirt on RHEL6 doesn't handle resume event emitted from
                 * qemu
                 */
                vm = getDomain(conn, cmd.getVmName());
                state = vm.getInfo().state;
                if (state == DomainInfo.DomainState.VIR_DOMAIN_PAUSED) {
                    vm.resume();
                }
            } else {

                /* VM is not running, create a snapshot by ourself */
                final Script command = new Script(_manageSnapshotPath,
                        _cmdsTimeout, s_logger);
                if (cmd.getCommandSwitch().equalsIgnoreCase(
                        ManageSnapshotCommand.CREATE_SNAPSHOT)) {
                    command.add("-c", disk.getPath());
                } else {
                    command.add("-d", snapshotPath);
                }

                command.add("-n", snapshotName);
                String result = command.execute();
                if (result != null) {
                    s_logger.debug("Failed to manage snapshot: " + result);
                    return new ManageSnapshotAnswer(cmd, false,
                            "Failed to manage snapshot: " + result);
                }
            }
            return new ManageSnapshotAnswer(cmd, cmd.getSnapshotId(),
                    disk.getPath() + File.separator + snapshotName, true, null);
        } catch (LibvirtException e) {
            s_logger.debug("Failed to manage snapshot: " + e.toString());
            return new ManageSnapshotAnswer(cmd, false,
                    "Failed to manage snapshot: " + e.toString());
        }

    }

    protected BackupSnapshotAnswer execute(final BackupSnapshotCommand cmd) {
        Long dcId = cmd.getDataCenterId();
        Long accountId = cmd.getAccountId();
        Long volumeId = cmd.getVolumeId();
        String secondaryStoragePoolUrl = cmd.getSecondaryStorageUrl();
        String snapshotName = cmd.getSnapshotName();
        String snapshotPath = cmd.getVolumePath();
        String snapshotDestPath = null;
        String snapshotRelPath = null;
        String vmName = cmd.getVmName();
        KVMStoragePool secondaryStoragePool = null;
        try {
            Connect conn = LibvirtConnection.getConnection();

            secondaryStoragePool = _storagePoolMgr.getStoragePoolByURI(
                    secondaryStoragePoolUrl);

            String ssPmountPath = secondaryStoragePool.getLocalPath();
            snapshotRelPath = File.separator + "snapshots" + File.separator
                    + dcId + File.separator + accountId + File.separator
                    + volumeId;

            snapshotDestPath = ssPmountPath + File.separator + "snapshots"
                    + File.separator + dcId + File.separator + accountId
                    + File.separator + volumeId;
            KVMStoragePool primaryPool = _storagePoolMgr.getStoragePool(
                    cmd.getPool().getType(),
                    cmd.getPrimaryStoragePoolNameLabel());
            KVMPhysicalDisk snapshotDisk = primaryPool.getPhysicalDisk(cmd
                    .getVolumePath());
            Script command = new Script(_manageSnapshotPath, _cmdsTimeout,
                    s_logger);
            command.add("-b", snapshotDisk.getPath());
            command.add("-n", snapshotName);
            command.add("-p", snapshotDestPath);
            command.add("-t", snapshotName);
            String result = command.execute();
            if (result != null) {
                s_logger.debug("Failed to backup snaptshot: " + result);
                return new BackupSnapshotAnswer(cmd, false, result, null, true);
            }
            /* Delete the snapshot on primary */

            DomainInfo.DomainState state = null;
            Domain vm = null;
            if (vmName != null) {
                try {
                    vm = getDomain(conn, cmd.getVmName());
                    state = vm.getInfo().state;
                } catch (LibvirtException e) {
                    s_logger.trace("Ignoring libvirt error.", e);
                }
            }

            KVMStoragePool primaryStorage = _storagePoolMgr.getStoragePool(
                    cmd.getPool().getType(),
                    cmd.getPool().getUuid());
            if (state == DomainInfo.DomainState.VIR_DOMAIN_RUNNING
                    && !primaryStorage.isExternalSnapshot()) {
                String vmUuid = vm.getUUIDString();
                Object[] args = new Object[] { snapshotName, vmUuid };
                String snapshot = SnapshotXML.format(args);
                s_logger.debug(snapshot);
                DomainSnapshot snap = vm.snapshotLookupByName(snapshotName);
                snap.delete(0);

                /*
                 * libvirt on RHEL6 doesn't handle resume event emitted from
                 * qemu
                 */
                vm = getDomain(conn, cmd.getVmName());
                state = vm.getInfo().state;
                if (state == DomainInfo.DomainState.VIR_DOMAIN_PAUSED) {
                    vm.resume();
                }
            } else {
                command = new Script(_manageSnapshotPath, _cmdsTimeout,
                        s_logger);
                command.add("-d", snapshotDisk.getPath());
                command.add("-n", snapshotName);
                result = command.execute();
                if (result != null) {
                    s_logger.debug("Failed to backup snapshot: " + result);
                    return new BackupSnapshotAnswer(cmd, false,
                            "Failed to backup snapshot: " + result, null, true);
                }
            }
        } catch (LibvirtException e) {
            return new BackupSnapshotAnswer(cmd, false, e.toString(), null,
                    true);
        } catch (CloudRuntimeException e) {
            return new BackupSnapshotAnswer(cmd, false, e.toString(), null,
                    true);
        } finally {
            if (secondaryStoragePool != null) {
                secondaryStoragePool.delete();
            }
        }
        return new BackupSnapshotAnswer(cmd, true, null, snapshotRelPath
                + File.separator + snapshotName, true);
    }

    protected DeleteSnapshotBackupAnswer execute(
            final DeleteSnapshotBackupCommand cmd) {
        Long dcId = cmd.getDataCenterId();
        Long accountId = cmd.getAccountId();
        Long volumeId = cmd.getVolumeId();
        KVMStoragePool secondaryStoragePool = null;
        try {
            secondaryStoragePool = _storagePoolMgr.getStoragePoolByURI(cmd
                    .getSecondaryStorageUrl());

            String ssPmountPath = secondaryStoragePool.getLocalPath();
            String snapshotDestPath = ssPmountPath + File.separator
                    + "snapshots" + File.separator + dcId + File.separator
                    + accountId + File.separator + volumeId;

            final Script command = new Script(_manageSnapshotPath,
                    _cmdsTimeout, s_logger);
            command.add("-d", snapshotDestPath);
            command.add("-n", cmd.getSnapshotName());

            command.execute();
        } catch (CloudRuntimeException e) {
            return new DeleteSnapshotBackupAnswer(cmd, false, e.toString());
        } finally {
            if (secondaryStoragePool != null) {
                secondaryStoragePool.delete();
            }
        }
        return new DeleteSnapshotBackupAnswer(cmd, true, null);
    }

    protected Answer execute(DeleteSnapshotsDirCommand cmd) {
        Long dcId = cmd.getDcId();
        Long accountId = cmd.getAccountId();
        Long volumeId = cmd.getVolumeId();
        KVMStoragePool secondaryStoragePool = null;
        try {
            secondaryStoragePool = _storagePoolMgr.getStoragePoolByURI(cmd
                    .getSecondaryStorageUrl());

            String ssPmountPath = secondaryStoragePool.getLocalPath();
            String snapshotDestPath = ssPmountPath + File.separator
                    + "snapshots" + File.separator + dcId + File.separator
                    + accountId + File.separator + volumeId;

            final Script command = new Script(_manageSnapshotPath,
                    _cmdsTimeout, s_logger);
            command.add("-d", snapshotDestPath);
            command.add("-f");
            command.execute();
        } catch (CloudRuntimeException e) {
            return new Answer(cmd, false, e.toString());
        } finally {
            if (secondaryStoragePool != null) {
                secondaryStoragePool.delete();
            }

        }
        return new Answer(cmd, true, null);
    }

    protected CreateVolumeFromSnapshotAnswer execute(
            final CreateVolumeFromSnapshotCommand cmd) {
        try {

            String snapshotPath = cmd.getSnapshotUuid();
            int index = snapshotPath.lastIndexOf("/");
            snapshotPath = snapshotPath.substring(0, index);
            KVMStoragePool secondaryPool = _storagePoolMgr.getStoragePoolByURI(
                    cmd.getSecondaryStorageUrl()
                    + snapshotPath);
            KVMPhysicalDisk snapshot = secondaryPool.getPhysicalDisk(cmd
                    .getSnapshotName());

            String primaryUuid = cmd.getPrimaryStoragePoolNameLabel();
            KVMStoragePool primaryPool = _storagePoolMgr
                    .getStoragePool(cmd.getPool().getType(),
                            primaryUuid);
            String volUuid = UUID.randomUUID().toString();
            KVMPhysicalDisk disk = _storagePoolMgr.copyPhysicalDisk(snapshot,
                    volUuid, primaryPool);
            return new CreateVolumeFromSnapshotAnswer(cmd, true, "",
                    disk.getName());
        } catch (CloudRuntimeException e) {
            return new CreateVolumeFromSnapshotAnswer(cmd, false, e.toString(),
                    null);
        }
    }

    protected Answer execute(final UpgradeSnapshotCommand cmd) {

        return new Answer(cmd, true, "success");
    }

    protected CreatePrivateTemplateAnswer execute(
            final CreatePrivateTemplateFromSnapshotCommand cmd) {
        String templateFolder = cmd.getAccountId() + File.separator
                + cmd.getNewTemplateId();
        String templateInstallFolder = "template/tmpl/" + templateFolder;
        String tmplName = UUID.randomUUID().toString();
        String tmplFileName = tmplName + ".qcow2";
        KVMStoragePool secondaryPool = null;
        KVMStoragePool snapshotPool = null;
        try {
            String snapshotPath = cmd.getSnapshotUuid();
            int index = snapshotPath.lastIndexOf("/");
            snapshotPath = snapshotPath.substring(0, index);
            snapshotPool = _storagePoolMgr.getStoragePoolByURI(cmd
                    .getSecondaryStorageUrl() + snapshotPath);
            KVMPhysicalDisk snapshot = snapshotPool.getPhysicalDisk(cmd
                    .getSnapshotName());

            secondaryPool = _storagePoolMgr.getStoragePoolByURI(
                    cmd.getSecondaryStorageUrl());

            String templatePath = secondaryPool.getLocalPath() + File.separator
                    + templateInstallFolder;

            _storage.mkdirs(templatePath);

            String tmplPath = templateInstallFolder + File.separator
                    + tmplFileName;
            Script command = new Script(_createTmplPath, _cmdsTimeout, s_logger);
            command.add("-t", templatePath);
            command.add("-n", tmplFileName);
            command.add("-f", snapshot.getPath());
            command.execute();

            Map<String, Object> params = new HashMap<String, Object>();
            params.put(StorageLayer.InstanceConfigKey, _storage);
            Processor qcow2Processor = new QCOW2Processor();
            qcow2Processor.configure("QCOW2 Processor", params);
            FormatInfo info = qcow2Processor.process(templatePath, null,
                    tmplName);

            TemplateLocation loc = new TemplateLocation(_storage, templatePath);
            loc.create(1, true, tmplName);
            loc.addFormat(info);
            loc.save();

            return new CreatePrivateTemplateAnswer(cmd, true, "", tmplPath,
                    info.virtualSize, info.size, tmplName, info.format);
        } catch (ConfigurationException e) {
            return new CreatePrivateTemplateAnswer(cmd, false, e.getMessage());
        } catch (InternalErrorException e) {
            return new CreatePrivateTemplateAnswer(cmd, false, e.getMessage());
        } catch (IOException e) {
            return new CreatePrivateTemplateAnswer(cmd, false, e.getMessage());
        } catch (CloudRuntimeException e) {
            return new CreatePrivateTemplateAnswer(cmd, false, e.getMessage());
        } finally {
            if (secondaryPool != null) {
                secondaryPool.delete();
            }
            if (snapshotPool != null) {
                snapshotPool.delete();
            }
        }
    }

    protected GetStorageStatsAnswer execute(final GetStorageStatsCommand cmd) {
        try {
            KVMStoragePool sp = _storagePoolMgr.getStoragePool(
                    cmd.getPooltype(),
                    cmd.getStorageId());
            return new GetStorageStatsAnswer(cmd, sp.getCapacity(),
                    sp.getUsed());
        } catch (CloudRuntimeException e) {
            return new GetStorageStatsAnswer(cmd, e.toString());
        }
    }

    protected CreatePrivateTemplateAnswer execute(
            CreatePrivateTemplateFromVolumeCommand cmd) {
        String secondaryStorageURL = cmd.getSecondaryStorageUrl();

        KVMStoragePool secondaryStorage = null;
        try {
            Connect conn = LibvirtConnection.getConnection();
            String templateFolder = cmd.getAccountId() + File.separator
                    + cmd.getTemplateId() + File.separator;
            String templateInstallFolder = "/template/tmpl/" + templateFolder;

            secondaryStorage = _storagePoolMgr.getStoragePoolByURI(
                    secondaryStorageURL);

            KVMStoragePool primary = _storagePoolMgr.getStoragePool(
                    cmd.getPool().getType(),
                    cmd.getPrimaryStoragePoolNameLabel());
            KVMPhysicalDisk disk = primary.getPhysicalDisk(cmd.getVolumePath());
            String tmpltPath = secondaryStorage.getLocalPath() + File.separator
                    + templateInstallFolder;
            _storage.mkdirs(tmpltPath);

            if (primary.getType() != StoragePoolType.RBD) {
                Script command = new Script(_createTmplPath, _cmdsTimeout, s_logger);
                command.add("-f", disk.getPath());
                command.add("-t", tmpltPath);
                command.add("-n", cmd.getUniqueName() + ".qcow2");

                String result = command.execute();

                if (result != null) {
                    s_logger.debug("failed to create template: " + result);
                    return new CreatePrivateTemplateAnswer(cmd, false, result);
                }
            } else {
                s_logger.debug("Converting RBD disk " + disk.getPath() + " into template " + cmd.getUniqueName());
                Script.runSimpleBashScript("qemu-img convert"
                        + " -f raw -O qcow2 "
                        + KVMPhysicalDisk.RBDStringBuilder(primary.getSourceHost(),
                                primary.getSourcePort(),
                                primary.getAuthUserName(),
                                primary.getAuthSecret(),
                                disk.getPath())
                                + " " + tmpltPath + "/" + cmd.getUniqueName() + ".qcow2");
                File templateProp = new File(tmpltPath + "/template.properties");
                if (!templateProp.exists()) {
                    templateProp.createNewFile();
                }

                String templateContent = "filename=" + cmd.getUniqueName() + ".qcow2" + System.getProperty("line.separator");

                DateFormat dateFormat = new SimpleDateFormat("MM_dd_yyyy");
                Date date = new Date();
                templateContent += "snapshot.name=" + dateFormat.format(date) + System.getProperty("line.separator");

                FileOutputStream templFo = new FileOutputStream(templateProp);
                templFo.write(templateContent.getBytes());
                templFo.flush();
                templFo.close();
            }

            Map<String, Object> params = new HashMap<String, Object>();
            params.put(StorageLayer.InstanceConfigKey, _storage);
            Processor qcow2Processor = new QCOW2Processor();

            qcow2Processor.configure("QCOW2 Processor", params);

            FormatInfo info = qcow2Processor.process(tmpltPath, null,
                    cmd.getUniqueName());

            TemplateLocation loc = new TemplateLocation(_storage, tmpltPath);
            loc.create(1, true, cmd.getUniqueName());
            loc.addFormat(info);
            loc.save();

            return new CreatePrivateTemplateAnswer(cmd, true, null,
                    templateInstallFolder + cmd.getUniqueName() + ".qcow2",
                    info.virtualSize, info.size, cmd.getUniqueName(),
                    ImageFormat.QCOW2);
        } catch (LibvirtException e) {
            s_logger.debug("Failed to get secondary storage pool: "
                    + e.toString());
            return new CreatePrivateTemplateAnswer(cmd, false, e.toString());
        } catch (InternalErrorException e) {
            return new CreatePrivateTemplateAnswer(cmd, false, e.toString());
        } catch (IOException e) {
            return new CreatePrivateTemplateAnswer(cmd, false, e.toString());
        } catch (ConfigurationException e) {
            return new CreatePrivateTemplateAnswer(cmd, false, e.toString());
        } catch (CloudRuntimeException e) {
            return new CreatePrivateTemplateAnswer(cmd, false, e.toString());
        } finally {
            if (secondaryStorage != null) {
                secondaryStorage.delete();
            }
        }
    }

    protected PrimaryStorageDownloadAnswer execute(
            final PrimaryStorageDownloadCommand cmd) {
        String tmplturl = cmd.getUrl();
        int index = tmplturl.lastIndexOf("/");
        String mountpoint = tmplturl.substring(0, index);
        String tmpltname = null;
        if (index < tmplturl.length() - 1) {
            tmpltname = tmplturl.substring(index + 1);
        }

        KVMPhysicalDisk tmplVol = null;
        KVMStoragePool secondaryPool = null;
        try {
            secondaryPool = _storagePoolMgr.getStoragePoolByURI(mountpoint);

            /* Get template vol */
            if (tmpltname == null) {
                secondaryPool.refresh();
                List<KVMPhysicalDisk> disks = secondaryPool.listPhysicalDisks();
                if (disks == null || disks.isEmpty()) {
                    return new PrimaryStorageDownloadAnswer(
                            "Failed to get volumes from pool: "
                                    + secondaryPool.getUuid());
                }
                for (KVMPhysicalDisk disk : disks) {
                    if (disk.getName().endsWith("qcow2")) {
                        tmplVol = disk;
                        break;
                    }
                }
                if (tmplVol == null) {
                    return new PrimaryStorageDownloadAnswer(
                            "Failed to get template from pool: "
                                    + secondaryPool.getUuid());
                }
            } else {
                tmplVol = secondaryPool.getPhysicalDisk(tmpltname);
            }

            /* Copy volume to primary storage */
            KVMStoragePool primaryPool = _storagePoolMgr.getStoragePool(
                    cmd.getPool().getType(),
                    cmd.getPoolUuid());

            KVMPhysicalDisk primaryVol = _storagePoolMgr.copyPhysicalDisk(
                    tmplVol, UUID.randomUUID().toString(), primaryPool);

            return new PrimaryStorageDownloadAnswer(primaryVol.getName(),
                    primaryVol.getSize());
        } catch (CloudRuntimeException e) {
            return new PrimaryStorageDownloadAnswer(e.toString());
        } finally {
            if (secondaryPool != null) {
                secondaryPool.delete();
            }
        }
    }

    protected Answer execute(CreateStoragePoolCommand cmd) {
        return new Answer(cmd, true, "success");
    }

    protected Answer execute(ModifyStoragePoolCommand cmd) {
        String poolType = cmd.getPool().getType().toString();
        KVMStoragePool storagepool = _storagePoolMgr.createStoragePool(cmd
                .getPool().getUuid(), cmd.getPool().getHost(),
                cmd.getPool().getPort(), cmd.getPool().getPath(),
                cmd.getPool().getUserInfo(), cmd.getPool().getType());
        if (storagepool == null) {
            return new Answer(cmd, false, " Failed to create storage pool");
        }

        Map<String, TemplateInfo> tInfo = new HashMap<String, TemplateInfo>();
        ModifyStoragePoolAnswer answer = new ModifyStoragePoolAnswer(cmd,
                storagepool.getCapacity(), storagepool.getUsed(), tInfo);

        return answer;
    }

    private Answer execute(SecurityGroupRulesCmd cmd) {
        String vif = null;
        String brname = null;
        try {
            Connect conn = LibvirtConnection.getConnection();
            List<InterfaceDef> nics = getInterfaces(conn, cmd.getVmName());
            vif = nics.get(0).getDevName();
            brname = nics.get(0).getBrName();
        } catch (LibvirtException e) {
            return new SecurityGroupRuleAnswer(cmd, false, e.toString());
        }

        boolean result = add_network_rules(cmd.getVmName(),
                Long.toString(cmd.getVmId()), cmd.getGuestIp(),
                cmd.getSignature(), Long.toString(cmd.getSeqNum()),
                cmd.getGuestMac(), cmd.stringifyRules(), vif, brname, cmd.getSecIpsString());

        if (!result) {
            s_logger.warn("Failed to program network rules for vm "
                    + cmd.getVmName());
            return new SecurityGroupRuleAnswer(cmd, false,
                    "programming network rules failed");
        } else {
            s_logger.debug("Programmed network rules for vm " + cmd.getVmName()
                    + " guestIp=" + cmd.getGuestIp() + ",ingress numrules="
                    + cmd.getIngressRuleSet().length + ",egress numrules="
                    + cmd.getEgressRuleSet().length);
            return new SecurityGroupRuleAnswer(cmd);
        }
    }

    private Answer execute(CleanupNetworkRulesCmd cmd) {
        boolean result = cleanup_rules();
        return new Answer(cmd, result, "");
    }

    protected GetVncPortAnswer execute(GetVncPortCommand cmd) {
        try {
            Connect conn = LibvirtConnection.getConnection();
            Integer vncPort = getVncPort(conn, cmd.getName());
            return new GetVncPortAnswer(cmd, _privateIp, 5900 + vncPort);
        } catch (LibvirtException e) {
            return new GetVncPortAnswer(cmd, e.toString());
        }
    }

    protected Answer execute(final CheckConsoleProxyLoadCommand cmd) {
        return executeProxyLoadScan(cmd, cmd.getProxyVmId(),
                cmd.getProxyVmName(), cmd.getProxyManagementIp(),
                cmd.getProxyCmdPort());
    }

    protected Answer execute(final WatchConsoleProxyLoadCommand cmd) {
        return executeProxyLoadScan(cmd, cmd.getProxyVmId(),
                cmd.getProxyVmName(), cmd.getProxyManagementIp(),
                cmd.getProxyCmdPort());
    }

    protected MaintainAnswer execute(MaintainCommand cmd) {
        return new MaintainAnswer(cmd);
    }

    private Answer executeProxyLoadScan(final Command cmd,
            final long proxyVmId, final String proxyVmName,
            final String proxyManagementIp, final int cmdPort) {
        String result = null;

        final StringBuffer sb = new StringBuffer();
        sb.append("http://").append(proxyManagementIp).append(":" + cmdPort)
        .append("/cmd/getstatus");

        boolean success = true;
        try {
            final URL url = new URL(sb.toString());
            final URLConnection conn = url.openConnection();

            final InputStream is = conn.getInputStream();
            final BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is));
            final StringBuilder sb2 = new StringBuilder();
            String line = null;
            try {
                while ((line = reader.readLine()) != null) {
                    sb2.append(line + "\n");
                }
                result = sb2.toString();
            } catch (final IOException e) {
                success = false;
            } finally {
                try {
                    is.close();
                } catch (final IOException e) {
                    s_logger.warn("Exception when closing , console proxy address : "
                            + proxyManagementIp);
                    success = false;
                }
            }
        } catch (final IOException e) {
            s_logger.warn("Unable to open console proxy command port url, console proxy address : "
                    + proxyManagementIp);
            success = false;
        }

        return new ConsoleProxyLoadAnswer(cmd, proxyVmId, proxyVmName, success,
                result);
    }

    private Answer execute(AttachIsoCommand cmd) {
        try {
            Connect conn = LibvirtConnection.getConnection();
            attachOrDetachISO(conn, cmd.getVmName(), cmd.getIsoPath(),
                    cmd.isAttach());
        } catch (LibvirtException e) {
            return new Answer(cmd, false, e.toString());
        } catch (URISyntaxException e) {
            return new Answer(cmd, false, e.toString());
        } catch (InternalErrorException e) {
            return new Answer(cmd, false, e.toString());
        }

        return new Answer(cmd);
    }

    private AttachVolumeAnswer execute(AttachVolumeCommand cmd) {
        try {
            Connect conn = LibvirtConnection.getConnection();
            KVMStoragePool primary = _storagePoolMgr.getStoragePool(
                    cmd.getPooltype(),
                    cmd.getPoolUuid());
            KVMPhysicalDisk disk = primary.getPhysicalDisk(cmd.getVolumePath());
            attachOrDetachDisk(conn, cmd.getAttach(), cmd.getVmName(), disk,
                    cmd.getDeviceId().intValue());
        } catch (LibvirtException e) {
            return new AttachVolumeAnswer(cmd, e.toString());
        } catch (InternalErrorException e) {
            return new AttachVolumeAnswer(cmd, e.toString());
        }

        return new AttachVolumeAnswer(cmd, cmd.getDeviceId());
    }

    private Answer execute(ReadyCommand cmd) {
        return new ReadyAnswer(cmd);
    }

    protected State convertToState(DomainInfo.DomainState ps) {
        final State state = s_statesTable.get(ps);
        return state == null ? State.Unknown : state;
    }

    protected State getVmState(Connect conn, final String vmName) {
        int retry = 3;
        Domain vms = null;
        while (retry-- > 0) {
            try {
                vms = conn.domainLookupByUUID(UUID.nameUUIDFromBytes(vmName
                        .getBytes()));
                State s = convertToState(vms.getInfo().state);
                return s;
            } catch (final LibvirtException e) {
                s_logger.warn("Can't get vm state " + vmName + e.getMessage()
                        + "retry:" + retry);
            } finally {
                try {
                    if (vms != null) {
                        vms.free();
                    }
                } catch (final LibvirtException l) {
                    s_logger.trace("Ignoring libvirt error.", l);
                }
            }
        }
        return State.Stopped;
    }

    private Answer execute(CheckVirtualMachineCommand cmd) {
        try {
            Connect conn = LibvirtConnection.getConnection();
            final State state = getVmState(conn, cmd.getVmName());
            Integer vncPort = null;
            if (state == State.Running) {
                vncPort = getVncPort(conn, cmd.getVmName());

                synchronized (_vms) {
                    _vms.put(cmd.getVmName(), State.Running);
                }
            }

            return new CheckVirtualMachineAnswer(cmd, state, vncPort);
        } catch (LibvirtException e) {
            return new CheckVirtualMachineAnswer(cmd, e.getMessage());
        }
    }

    private Answer execute(PingTestCommand cmd) {
        String result = null;
        final String computingHostIp = cmd.getComputingHostIp(); // TODO, split
        // the
        // command
        // into 2
        // types

        if (computingHostIp != null) {
            result = doPingTest(computingHostIp);
        } else if (cmd.getRouterIp() != null && cmd.getPrivateIp() != null) {
            result = doPingTest(cmd.getRouterIp(), cmd.getPrivateIp());
        } else {
            return new Answer(cmd, false, "routerip and private ip is null");
        }

        if (result != null) {
            return new Answer(cmd, false, result);
        }
        return new Answer(cmd);
    }

    private String doPingTest(final String computingHostIp) {
        final Script command = new Script(_pingTestPath, 10000, s_logger);
        command.add("-h", computingHostIp);
        return command.execute();
    }

    private String doPingTest(final String domRIp, final String vmIp) {
        final Script command = new Script(_pingTestPath, 10000, s_logger);
        command.add("-i", domRIp);
        command.add("-p", vmIp);
        return command.execute();
    }

    private synchronized Answer execute(MigrateCommand cmd) {
        String vmName = cmd.getVmName();

        State state = null;
        String result = null;
        synchronized (_vms) {
            state = _vms.get(vmName);
            _vms.put(vmName, State.Stopping);
        }

        List<InterfaceDef> ifaces = null;

        Domain dm = null;
        Connect dconn = null;
        Domain destDomain = null;
        Connect conn = null;
        try {
            conn = LibvirtConnection.getConnection();
            ifaces = getInterfaces(conn, vmName);
            dm = conn.domainLookupByUUID(UUID.nameUUIDFromBytes(vmName
                    .getBytes()));
            dconn = new Connect("qemu+tcp://" + cmd.getDestinationIp()
                    + "/system");
            /*
             * Hard code lm flags: VIR_MIGRATE_LIVE(1<<0) and
             * VIR_MIGRATE_PERSIST_DEST(1<<3)
             */
            destDomain = dm.migrate(dconn, (1 << 0) | (1 << 3), vmName, "tcp:"
                    + cmd.getDestinationIp(), _migrateSpeed);
        } catch (LibvirtException e) {
            s_logger.debug("Can't migrate domain: " + e.getMessage());
            result = e.getMessage();
        } finally {
            try {
                if (dm != null) {
                    dm.free();
                }
                if (dconn != null) {
                    dconn.close();
                }
                if (destDomain != null) {
                    destDomain.free();
                }
            } catch (final LibvirtException e) {
                s_logger.trace("Ignoring libvirt error.", e);
            }
        }

        if (result != null) {
            synchronized (_vms) {
                _vms.put(vmName, state);
            }
        } else {
            destroy_network_rules_for_vm(conn, vmName);
            for (InterfaceDef iface : ifaces) {
                // We don't know which "traffic type" is associated with
                // each interface at this point, so inform all vif drivers
                for(VifDriver vifDriver : getAllVifDrivers()){
                    vifDriver.unplug(iface);
                }
            }
            cleanupVM(conn, vmName,
                    getVnetId(VirtualMachineName.getVnet(vmName)));
        }

        return new MigrateAnswer(cmd, result == null, result, null);
    }

    private synchronized Answer execute(PrepareForMigrationCommand cmd) {

        VirtualMachineTO vm = cmd.getVirtualMachine();
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Preparing host for migrating " + vm);
        }

        NicTO[] nics = vm.getNics();
        try {
            Connect conn = LibvirtConnection.getConnection();
            for (NicTO nic : nics) {
                getVifDriver(nic.getType()).plug(nic, null);
            }

            /* setup disks, e.g for iso */
            VolumeTO[] volumes = vm.getDisks();
            for (VolumeTO volume : volumes) {
                if (volume.getType() == Volume.Type.ISO) {
                    getVolumePath(conn, volume);
                }
            }

            synchronized (_vms) {
                _vms.put(vm.getName(), State.Migrating);
            }

            return new PrepareForMigrationAnswer(cmd);
        } catch (LibvirtException e) {
            return new PrepareForMigrationAnswer(cmd, e.toString());
        } catch (InternalErrorException e) {
            return new PrepareForMigrationAnswer(cmd, e.toString());
        } catch (URISyntaxException e) {
            return new PrepareForMigrationAnswer(cmd, e.toString());
        }
    }

    private Answer execute(CheckHealthCommand cmd) {
        return new CheckHealthAnswer(cmd, true);
    }

    private Answer execute(GetHostStatsCommand cmd) {
        final Script cpuScript = new Script("/bin/bash", s_logger);
        cpuScript.add("-c");
        cpuScript
        .add("idle=$(top -b -n 1|grep Cpu\\(s\\):|cut -d% -f4|cut -d, -f2);echo $idle");

        final OutputInterpreter.OneLineParser parser = new OutputInterpreter.OneLineParser();
        String result = cpuScript.execute(parser);
        if (result != null) {
            s_logger.debug("Unable to get the host CPU state: " + result);
            return new Answer(cmd, false, result);
        }
        double cpuUtil = (100.0D - Double.parseDouble(parser.getLine()));

        long freeMem = 0;
        final Script memScript = new Script("/bin/bash", s_logger);
        memScript.add("-c");
        memScript
        .add("freeMem=$(free|grep cache:|awk '{print $4}');echo $freeMem");
        final OutputInterpreter.OneLineParser Memparser = new OutputInterpreter.OneLineParser();
        result = memScript.execute(Memparser);
        if (result != null) {
            s_logger.debug("Unable to get the host Mem state: " + result);
            return new Answer(cmd, false, result);
        }
        freeMem = Long.parseLong(Memparser.getLine());

        Script totalMem = new Script("/bin/bash", s_logger);
        totalMem.add("-c");
        totalMem.add("free|grep Mem:|awk '{print $2}'");
        final OutputInterpreter.OneLineParser totMemparser = new OutputInterpreter.OneLineParser();
        result = totalMem.execute(totMemparser);
        if (result != null) {
            s_logger.debug("Unable to get the host Mem state: " + result);
            return new Answer(cmd, false, result);
        }
        long totMem = Long.parseLong(totMemparser.getLine());

        Pair<Double, Double> nicStats = getNicStats(_publicBridgeName);

        HostStatsEntry hostStats = new HostStatsEntry(cmd.getHostId(), cpuUtil,
                nicStats.first() / 1000, nicStats.second() / 1000, "host",
                totMem, freeMem, 0, 0);
        return new GetHostStatsAnswer(cmd, hostStats);
    }

    protected String networkUsage(final String privateIpAddress,
            final String option, final String vif) {
        Script getUsage = new Script(_routerProxyPath, s_logger);
        getUsage.add("netusage.sh");
        getUsage.add(privateIpAddress);
        if (option.equals("get")) {
            getUsage.add("-g");
        } else if (option.equals("create")) {
            getUsage.add("-c");
        } else if (option.equals("reset")) {
            getUsage.add("-r");
        } else if (option.equals("addVif")) {
            getUsage.add("-a", vif);
        } else if (option.equals("deleteVif")) {
            getUsage.add("-d", vif);
        }


        final OutputInterpreter.OneLineParser usageParser = new OutputInterpreter.OneLineParser();
        String result = getUsage.execute(usageParser);
        if (result != null) {
            s_logger.debug("Failed to execute networkUsage:" + result);
            return null;
        }
        return usageParser.getLine();
    }

    protected long[] getNetworkStats(String privateIP) {
        String result = networkUsage(privateIP, "get", null);
        long[] stats = new long[2];
        if (result != null) {
            String[] splitResult = result.split(":");
            int i = 0;
            while (i < splitResult.length - 1) {
                stats[0] += (new Long(splitResult[i++])).longValue();
                stats[1] += (new Long(splitResult[i++])).longValue();
            }
        }
        return stats;
    }

    private Answer execute(NetworkUsageCommand cmd) {
        if (cmd.getOption() != null && cmd.getOption().equals("create")) {
            String result = networkUsage(cmd.getPrivateIP(), "create", null);
            NetworkUsageAnswer answer = new NetworkUsageAnswer(cmd, result, 0L,
                    0L);
            return answer;
        }
        long[] stats = getNetworkStats(cmd.getPrivateIP());
        NetworkUsageAnswer answer = new NetworkUsageAnswer(cmd, "", stats[0],
                stats[1]);
        return answer;
    }

    private Answer execute(RebootCommand cmd) {

        synchronized (_vms) {
            _vms.put(cmd.getVmName(), State.Starting);
        }

        try {
            Connect conn = LibvirtConnection.getConnection();
            final String result = rebootVM(conn, cmd.getVmName());
            if (result == null) {
                Integer vncPort = null;
                try {
                    vncPort = getVncPort(conn, cmd.getVmName());
                } catch (LibvirtException e) {
                    s_logger.trace("Ignoring libvirt error.", e);
                }
                get_rule_logs_for_vms();
                return new RebootAnswer(cmd, null, vncPort);
            } else {
                return new RebootAnswer(cmd, result, false);
            }
        } catch (LibvirtException e) {
            return new RebootAnswer(cmd, e.getMessage(), false);
        } finally {
            synchronized (_vms) {
                _vms.put(cmd.getVmName(), State.Running);
            }
        }
    }

    protected Answer execute(RebootRouterCommand cmd) {
        RebootAnswer answer = (RebootAnswer) execute((RebootCommand) cmd);
        String result = _virtRouterResource.connect(cmd.getPrivateIpAddress());
        if (result == null) {
            networkUsage(cmd.getPrivateIpAddress(), "create", null);
            return answer;
        } else {
            return new Answer(cmd, false, result);
        }
    }

    protected GetVmStatsAnswer execute(GetVmStatsCommand cmd) {
        List<String> vmNames = cmd.getVmNames();
        try {
            HashMap<String, VmStatsEntry> vmStatsNameMap = new HashMap<String, VmStatsEntry>();
            Connect conn = LibvirtConnection.getConnection();
            for (String vmName : vmNames) {
                VmStatsEntry statEntry = getVmStat(conn, vmName);
                if (statEntry == null) {
                    continue;
                }

                vmStatsNameMap.put(vmName, statEntry);
            }
            return new GetVmStatsAnswer(cmd, vmStatsNameMap);
        } catch (LibvirtException e) {
            s_logger.debug("Can't get vm stats: " + e.toString());
            return new GetVmStatsAnswer(cmd, null);
        }
    }

    protected Answer execute(StopCommand cmd) {
        final String vmName = cmd.getVmName();

        State state = null;
        synchronized (_vms) {
            state = _vms.get(vmName);
            _vms.put(vmName, State.Stopping);
        }
        try {
            Connect conn = LibvirtConnection.getConnection();

            List<DiskDef> disks = getDisks(conn, vmName);
            List<InterfaceDef> ifaces = getInterfaces(conn, vmName);

            destroy_network_rules_for_vm(conn, vmName);
            String result = stopVM(conn, vmName);
            if (result == null) {
                for (DiskDef disk : disks) {
                    if (disk.getDeviceType() == DiskDef.deviceType.CDROM
                            && disk.getDiskPath() != null) {
                        cleanupDisk(conn, disk);
                    }
                }
                for (InterfaceDef iface: ifaces) {
                    // We don't know which "traffic type" is associated with
                    // each interface at this point, so inform all vif drivers
                    for(VifDriver vifDriver : getAllVifDrivers()){
                        vifDriver.unplug(iface);
                    }
                }
            }

            final String result2 = cleanupVnet(conn, cmd.getVnet());

            if (result != null && result2 != null) {
                result = result2 + result;
            }
            state = State.Stopped;
            return new StopAnswer(cmd, result, 0, true);
        } catch (LibvirtException e) {
            return new StopAnswer(cmd, e.getMessage(), false);
        } finally {
            synchronized (_vms) {
                if (state != null) {
                    _vms.put(vmName, state);
                } else {
                    _vms.remove(vmName);
                }
            }
        }
    }

    protected Answer execute(ModifySshKeysCommand cmd) {
        File sshKeysDir = new File(_SSHKEYSPATH);
        String result = null;
        if (!sshKeysDir.exists()) {
            // Change permissions for the 700
            Script script = new Script("mkdir", _timeout, s_logger);
            script.add("-m","700");
            script.add(_SSHKEYSPATH);
            script.execute();

            if(!sshKeysDir.exists()) {
                s_logger.debug("failed to create directory " + _SSHKEYSPATH);
            }
        }

        File pubKeyFile = new File(_SSHPUBKEYPATH);
        if (!pubKeyFile.exists()) {
            try {
                pubKeyFile.createNewFile();
            } catch (IOException e) {
                result = "Failed to create file: " + e.toString();
                s_logger.debug(result);
            }
        }

        if (pubKeyFile.exists()) {
            String pubKey = cmd.getPubKey();
            try {
                FileOutputStream pubkStream = new FileOutputStream(pubKeyFile);
                pubkStream.write(pubKey.getBytes());
                pubkStream.close();
            } catch (FileNotFoundException e) {
                result = "File" + _SSHPUBKEYPATH + "is not found:"
                        + e.toString();
                s_logger.debug(result);
            } catch (IOException e) {
                result = "Write file " + _SSHPUBKEYPATH + ":" + e.toString();
                s_logger.debug(result);
            }
        }

        File prvKeyFile = new File(_SSHPRVKEYPATH);
        if (!prvKeyFile.exists()) {
            try {
                prvKeyFile.createNewFile();
            } catch (IOException e) {
                result = "Failed to create file: " + e.toString();
                s_logger.debug(result);
            }
        }

        if (prvKeyFile.exists()) {
            String prvKey = cmd.getPrvKey();
            try {
                FileOutputStream prvKStream = new FileOutputStream(prvKeyFile);
                prvKStream.write(prvKey.getBytes());
                prvKStream.close();
            } catch (FileNotFoundException e) {
                result = "File" + _SSHPRVKEYPATH + "is not found:"
                        + e.toString();
                s_logger.debug(result);
            } catch (IOException e) {
                result = "Write file " + _SSHPRVKEYPATH + ":" + e.toString();
                s_logger.debug(result);
            }

            Script script = new Script("chmod", _timeout, s_logger);
            script.add("600", _SSHPRVKEYPATH);
            script.execute();
        }

        if (result != null) {
            return new Answer(cmd, false, result);
        } else {
            return new Answer(cmd, true, null);
        }
    }

    protected void handleVmStartFailure(Connect conn, String vmName,
            LibvirtVMDef vm) {
        if (vm != null && vm.getDevices() != null) {
            cleanupVMNetworks(conn, vm.getDevices().getInterfaces());
        }
    }

    protected LibvirtVMDef createVMFromSpec(VirtualMachineTO vmTO) {
        LibvirtVMDef vm = new LibvirtVMDef();
        vm.setHvsType(_hypervisorType);
        vm.setDomainName(vmTO.getName());
        vm.setDomUUID(UUID.nameUUIDFromBytes(vmTO.getName().getBytes())
                .toString());
        vm.setDomDescription(vmTO.getOs());

        GuestDef guest = new GuestDef();
        guest.setGuestType(GuestDef.guestType.KVM);
        guest.setGuestArch(vmTO.getArch());
        guest.setMachineType("pc");
        guest.setBootOrder(GuestDef.bootOrder.CDROM);
        guest.setBootOrder(GuestDef.bootOrder.HARDISK);

        vm.addComp(guest);

        GuestResourceDef grd = new GuestResourceDef();

        if (vmTO.getMinRam() != vmTO.getMaxRam()){
            grd.setMemBalloning(true);
            grd.setCurrentMem((int)vmTO.getMinRam()/1024);
            grd.setMemorySize((int)vmTO.getMaxRam()/1024);
        }
        else{
            grd.setMemorySize(vmTO.getMaxRam() / 1024);
        }
        grd.setVcpuNum(vmTO.getCpus());
        vm.addComp(grd);

        CpuTuneDef ctd = new CpuTuneDef();
        /**
            A 4.0.X/4.1.X management server doesn't send the correct JSON
            command for getMinSpeed, it only sends a 'speed' field.

            So if getMinSpeed() returns null we fall back to getSpeed().

            This way a >4.1 agent can work communicate a <=4.1 management server

            This change is due to the overcommit feature in 4.2
        */
        if (vmTO.getMinSpeed() != null) {
            ctd.setShares(vmTO.getCpus() * vmTO.getMinSpeed());
        } else {
            ctd.setShares(vmTO.getCpus() * vmTO.getSpeed());
        }
        vm.addComp(ctd);

        FeaturesDef features = new FeaturesDef();
        features.addFeatures("pae");
        features.addFeatures("apic");
        features.addFeatures("acpi");
        vm.addComp(features);

        TermPolicy term = new TermPolicy();
        term.setCrashPolicy("destroy");
        term.setPowerOffPolicy("destroy");
        term.setRebootPolicy("restart");
        vm.addComp(term);

        ClockDef clock = new ClockDef();
        if (vmTO.getOs().startsWith("Windows")) {
            clock.setClockOffset(ClockDef.ClockOffset.LOCALTIME);
            clock.setTimer("rtc", "catchup", null);
        }

        vm.addComp(clock);

        DevicesDef devices = new DevicesDef();
        devices.setEmulatorPath(_hypervisorPath);

        SerialDef serial = new SerialDef("pty", null, (short) 0);
        devices.addDevice(serial);

        if (vmTO.getType() != VirtualMachine.Type.User) {
            VirtioSerialDef vserial = new VirtioSerialDef(vmTO.getName(), null);
            devices.addDevice(vserial);
        }

        ConsoleDef console = new ConsoleDef("pty", null, null, (short) 0);
        devices.addDevice(console);

        GraphicDef grap = new GraphicDef("vnc", (short) 0, true, vmTO.getVncAddr(), null,
                null);
        devices.addDevice(grap);

        InputDef input = new InputDef("tablet", "usb");
        devices.addDevice(input);

        vm.addComp(devices);

        return vm;
    }

    protected void createVifs(VirtualMachineTO vmSpec,
            LibvirtVMDef vm) throws InternalErrorException, LibvirtException {
        NicTO[] nics = vmSpec.getNics();
        for (int i = 0; i < nics.length; i++) {
            for (NicTO nic : vmSpec.getNics()) {
                if (nic.getDeviceId() == i) {
                    createVif(vm, nic);
                }
            }
        }
    }

    protected synchronized StartAnswer execute(StartCommand cmd) {
        VirtualMachineTO vmSpec = cmd.getVirtualMachine();
        vmSpec.setVncAddr(cmd.getHostIp());
        String vmName = vmSpec.getName();
        LibvirtVMDef vm = null;

        State state = State.Stopped;
        Connect conn = null;
        try {
            conn = LibvirtConnection.getConnection();
            synchronized (_vms) {
                _vms.put(vmName, State.Starting);
            }

            vm = createVMFromSpec(vmSpec);

            createVbd(conn, vmSpec, vmName, vm);

            createVifs(vmSpec, vm);

            s_logger.debug("starting " + vmName + ": " + vm.toString());
            startVM(conn, vmName, vm.toString());

            NicTO[] nics = vmSpec.getNics();
            for (NicTO nic : nics) {
                if (nic.isSecurityGroupEnabled() || ( nic.getIsolationUri() != null
                         && nic.getIsolationUri().getScheme().equalsIgnoreCase(IsolationType.Ec2.toString()))) {
                    if (vmSpec.getType() != VirtualMachine.Type.User) {
                        default_network_rules_for_systemvm(conn, vmName);
                        break;
                    } else {
                        List<String> nicSecIps = nic.getNicSecIps();
                        String secIpsStr;
                        StringBuilder sb = new StringBuilder();
                        if (nicSecIps != null) {
                            for (String ip : nicSecIps) {
                                sb.append(ip).append(":");
                            }
                            secIpsStr = sb.toString();
                        } else {
                            secIpsStr = "0:";
                        }
                        default_network_rules(conn, vmName, nic, vmSpec.getId(), secIpsStr);
                    }
                }
            }

            // pass cmdline info to system vms
            if (vmSpec.getType() != VirtualMachine.Type.User) {
                passCmdLine(vmName, vmSpec.getBootArgs() );
            }

            state = State.Running;
            return new StartAnswer(cmd);
        } catch (LibvirtException e) {
            s_logger.warn("LibvirtException ", e);
            if (conn != null) {
                handleVmStartFailure(conn, vmName, vm);
            }
            return new StartAnswer(cmd, e.getMessage());
        } catch (InternalErrorException e) {
            s_logger.warn("InternalErrorException ", e);
            if (conn != null) {
                handleVmStartFailure(conn, vmName, vm);
            }
            return new StartAnswer(cmd, e.getMessage());
        } catch (URISyntaxException e) {
            s_logger.warn("URISyntaxException ", e);
            if (conn != null) {
                handleVmStartFailure(conn, vmName, vm);
            }
            return new StartAnswer(cmd, e.getMessage());
        } finally {
            synchronized (_vms) {
                if (state != State.Stopped) {
                    _vms.put(vmName, state);
                } else {
                    _vms.remove(vmName);
                }
            }
        }
    }

    private String getVolumePath(Connect conn, VolumeTO volume)
            throws LibvirtException, URISyntaxException {
        if (volume.getType() == Volume.Type.ISO && volume.getPath() != null) {
            String isoPath = volume.getPath();
            int index = isoPath.lastIndexOf("/");
            String path = isoPath.substring(0, index);
            String name = isoPath.substring(index + 1);
            KVMStoragePool secondaryPool = _storagePoolMgr.getStoragePoolByURI(
                    path);
            KVMPhysicalDisk isoVol = secondaryPool.getPhysicalDisk(name);
            return isoVol.getPath();
        } else {
            return volume.getPath();
        }
    }

    protected void createVbd(Connect conn, VirtualMachineTO vmSpec,
            String vmName, LibvirtVMDef vm) throws InternalErrorException,
            LibvirtException, URISyntaxException {
        List<VolumeTO> disks = Arrays.asList(vmSpec.getDisks());
        Collections.sort(disks, new Comparator<VolumeTO>() {
            @Override
            public int compare(VolumeTO arg0, VolumeTO arg1) {
                return arg0.getDeviceId() > arg1.getDeviceId() ? 1 : -1;
            }
        });

        for (VolumeTO volume : disks) {
            KVMPhysicalDisk physicalDisk = null;
            KVMStoragePool pool = null;
            if (volume.getType() == Volume.Type.ISO && volume.getPath() != null) {
                String volPath = volume.getPath();
                int index = volPath.lastIndexOf("/");
                String volDir = volPath.substring(0, index);
                String volName = volPath.substring(index + 1);
                KVMStoragePool secondaryStorage = _storagePoolMgr.
                        getStoragePoolByURI(volDir);
                physicalDisk = secondaryStorage.getPhysicalDisk(volName);
            } else if (volume.getType() != Volume.Type.ISO) {
                pool = _storagePoolMgr.getStoragePool(
                        volume.getPoolType(),
                        volume.getPoolUuid());
                physicalDisk = pool.getPhysicalDisk(volume.getPath());
            }

            String volPath = null;
            if (physicalDisk != null) {
                volPath = physicalDisk.getPath();
            }

            DiskDef.diskBus diskBusType = getGuestDiskModel(vmSpec.getOs());
            DiskDef disk = new DiskDef();
            if (volume.getType() == Volume.Type.ISO) {
                if (volPath == null) {
                    /* Add iso as placeholder */
                    disk.defISODisk(null);
                } else {
                    disk.defISODisk(volPath);
                }
            } else {
                int devId = (int) volume.getDeviceId();

                if (pool.getType() == StoragePoolType.RBD) {
                    /*
                            For RBD pools we use the secret mechanism in libvirt.
                            We store the secret under the UUID of the pool, that's why
                            we pass the pool's UUID as the authSecret
                     */
                    disk.defNetworkBasedDisk(physicalDisk.getPath().replace("rbd:", ""), pool.getSourceHost(), pool.getSourcePort(),
                            pool.getAuthUserName(), pool.getUuid(),
                            devId, diskBusType, diskProtocol.RBD);
                } else if (pool.getType() == StoragePoolType.CLVM) {
                    disk.defBlockBasedDisk(physicalDisk.getPath(), devId,
                            diskBusType);
                } else {
                    if (volume.getType() == Volume.Type.DATADISK) {
                        disk.defFileBasedDisk(physicalDisk.getPath(), devId,
                                DiskDef.diskBus.VIRTIO,
                                DiskDef.diskFmtType.QCOW2);
                    } else {
                        disk.defFileBasedDisk(physicalDisk.getPath(), devId, diskBusType, DiskDef.diskFmtType.QCOW2);
                    }

                }

            }

            vm.getDevices().addDevice(disk);
        }

        if (vmSpec.getType() != VirtualMachine.Type.User) {
            if (_sysvmISOPath != null) {
                DiskDef iso = new DiskDef();
                iso.defISODisk(_sysvmISOPath);
                vm.getDevices().addDevice(iso);
            }
        }
    }

    private VolumeTO getVolume(VirtualMachineTO vmSpec, Volume.Type type) {
        VolumeTO volumes[] = vmSpec.getDisks();
        for (VolumeTO volume : volumes) {
            if (volume.getType() == type) {
                return volume;
            }
        }
        return null;
    }

    private void createVif(LibvirtVMDef vm, NicTO nic)
            throws InternalErrorException, LibvirtException {
        vm.getDevices().addDevice(
                getVifDriver(nic.getType()).plug(nic, vm.getGuestOSType()).toString());
    }

    protected CheckSshAnswer execute(CheckSshCommand cmd) {
        String vmName = cmd.getName();
        String privateIp = cmd.getIp();
        int cmdPort = cmd.getPort();

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Ping command port, " + privateIp + ":" + cmdPort);
        }

        String result = _virtRouterResource.connect(privateIp, cmdPort);
        if (result != null) {
            return new CheckSshAnswer(cmd, "Can not ping System vm "
                    + vmName + "due to:" + result);
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Ping command port succeeded for vm " + vmName);
        }

        return new CheckSshAnswer(cmd);
    }

    private boolean cleanupDisk(Connect conn, DiskDef disk) {
        // need to umount secondary storage
        String path = disk.getDiskPath();
        String poolUuid = null;
        if (path != null) {
            String[] token = path.split("/");
            if (token.length > 3) {
                poolUuid = token[2];
            }
        }

        if (poolUuid == null) {
            return true;
        }

        try {
            // we use libvirt as storage adaptor since we passed a libvirt
            // connection to cleanupDisk. We pass a storage type that maps
            // to libvirt adaptor.
            KVMStoragePool pool = _storagePoolMgr.getStoragePool(
                                      StoragePoolType.Filesystem, poolUuid);
            if (pool != null) {
                pool.delete();
            }
            return true;
        } catch (CloudRuntimeException e) {
            return false;
        }
    }

    protected synchronized String attachOrDetachISO(Connect conn,
            String vmName, String isoPath, boolean isAttach)
                    throws LibvirtException, URISyntaxException, InternalErrorException {
        String isoXml = null;
        if (isoPath != null && isAttach) {
            int index = isoPath.lastIndexOf("/");
            String path = isoPath.substring(0, index);
            String name = isoPath.substring(index + 1);
            KVMStoragePool secondaryPool = _storagePoolMgr.getStoragePoolByURI(
                    path);
            KVMPhysicalDisk isoVol = secondaryPool.getPhysicalDisk(name);
            isoPath = isoVol.getPath();

            DiskDef iso = new DiskDef();
            iso.defISODisk(isoPath);
            isoXml = iso.toString();
        } else {
            DiskDef iso = new DiskDef();
            iso.defISODisk(null);
            isoXml = iso.toString();
        }

        List<DiskDef> disks = getDisks(conn, vmName);
        String result = attachOrDetachDevice(conn, true, vmName, isoXml);
        if (result == null && !isAttach) {
            for (DiskDef disk : disks) {
                if (disk.getDeviceType() == DiskDef.deviceType.CDROM) {
                    cleanupDisk(conn, disk);
                }
            }

        }
        return result;
    }

    protected synchronized String attachOrDetachDisk(Connect conn,
            boolean attach, String vmName, KVMPhysicalDisk attachingDisk,
            int devId) throws LibvirtException, InternalErrorException {
        List<DiskDef> disks = null;
        Domain dm = null;
        DiskDef diskdef = null;
        try {
            if (!attach) {
                dm = conn.domainLookupByUUID(UUID.nameUUIDFromBytes(vmName
                        .getBytes()));
                LibvirtDomainXMLParser parser = new LibvirtDomainXMLParser();
                String xml = dm.getXMLDesc(0);
                parser.parseDomainXML(xml);
                disks = parser.getDisks();

                for (DiskDef disk : disks) {
                    String file = disk.getDiskPath();
                    if (file != null
                            && file.equalsIgnoreCase(attachingDisk.getPath())) {
                        diskdef = disk;
                        break;
                    }
                }
                if (diskdef == null) {
                    throw new InternalErrorException("disk: "
                            + attachingDisk.getPath()
                            + " is not attached before");
                }
            } else {
                diskdef = new DiskDef();
                if (attachingDisk.getFormat() == PhysicalDiskFormat.QCOW2) {
                    diskdef.defFileBasedDisk(attachingDisk.getPath(), devId,
                            DiskDef.diskBus.VIRTIO, DiskDef.diskFmtType.QCOW2);
                } else if (attachingDisk.getFormat() == PhysicalDiskFormat.RAW) {
                    diskdef.defBlockBasedDisk(attachingDisk.getPath(), devId,
                            DiskDef.diskBus.VIRTIO);
                }
            }

            String xml = diskdef.toString();
            return attachOrDetachDevice(conn, attach, vmName, xml);
        } finally {
            if (dm != null) {
                dm.free();
            }
        }
    }

    protected synchronized String attachOrDetachDevice(Connect conn,
            boolean attach, String vmName, String xml) throws LibvirtException,
            InternalErrorException {
        Domain dm = null;
        try {
            dm = conn.domainLookupByUUID(UUID.nameUUIDFromBytes((vmName
                    .getBytes())));

            if (attach) {
                s_logger.debug("Attaching device: " + xml);
                dm.attachDevice(xml);
            } else {
                s_logger.debug("Detaching device: " + xml);
                dm.detachDevice(xml);
            }
        } catch (LibvirtException e) {
            if (attach) {
                s_logger.warn("Failed to attach device to " + vmName + ": "
                        + e.getMessage());
            } else {
                s_logger.warn("Failed to detach device from " + vmName + ": "
                        + e.getMessage());
            }
            throw e;
        } finally {
            if (dm != null) {
                try {
                    dm.free();
                } catch (LibvirtException l) {
                    s_logger.trace("Ignoring libvirt error.", l);
                }
            }
        }

        return null;
    }

    @Override
    public PingCommand getCurrentStatus(long id) {
        final HashMap<String, State> newStates = sync();

        if (!_can_bridge_firewall) {
            return new PingRoutingCommand(com.cloud.host.Host.Type.Routing, id,
                    newStates);
        } else {
            HashMap<String, Pair<Long, Long>> nwGrpStates = syncNetworkGroups(id);
            return new PingRoutingWithNwGroupsCommand(getType(), id, newStates,
                    nwGrpStates);
        }
    }

    @Override
    public Type getType() {
        return Type.Routing;
    }

    private Map<String, String> getVersionStrings() {
        final Script command = new Script(_versionstringpath, _timeout,
                s_logger);
        KeyValueInterpreter kvi = new KeyValueInterpreter();
        String result = command.execute(kvi);
        if (result == null) {
            return kvi.getKeyValues();
        } else {
            return new HashMap<String, String>(1);
        }
    }

    @Override
    public StartupCommand[] initialize() {
        Map<String, State> changes = null;

        synchronized (_vms) {
            _vms.clear();
            changes = sync();
        }

        final List<Object> info = getHostInfo();

        final StartupRoutingCommand cmd = new StartupRoutingCommand(
                (Integer) info.get(0), (Long) info.get(1), (Long) info.get(2),
                (Long) info.get(4), (String) info.get(3), HypervisorType.KVM,
                RouterPrivateIpStrategy.HostLocal);
        cmd.setStateChanges(changes);
        fillNetworkInformation(cmd);
        _privateIp = cmd.getPrivateIpAddress();
        cmd.getHostDetails().putAll(getVersionStrings());
        cmd.setPool(_pool);
        cmd.setCluster(_clusterId);
        cmd.setGatewayIpAddress(_localGateway);

        StartupStorageCommand sscmd = null;
        try {

            KVMStoragePool localStoragePool = _storagePoolMgr
                    .createStoragePool(_localStorageUUID, "localhost", -1,
                            _localStoragePath, "", StoragePoolType.Filesystem);
            com.cloud.agent.api.StoragePoolInfo pi = new com.cloud.agent.api.StoragePoolInfo(
                    localStoragePool.getUuid(), cmd.getPrivateIpAddress(),
                    _localStoragePath, _localStoragePath,
                    StoragePoolType.Filesystem, localStoragePool.getCapacity(),
                    localStoragePool.getUsed());

            sscmd = new StartupStorageCommand();
            sscmd.setPoolInfo(pi);
            sscmd.setGuid(pi.getUuid());
            sscmd.setDataCenter(_dcId);
            sscmd.setResourceType(Storage.StorageResourceType.STORAGE_POOL);
        } catch (CloudRuntimeException e) {
            s_logger.debug("Unable to initialize local storage pool: " + e);
        }

        if (sscmd != null) {
            return new StartupCommand[] { cmd, sscmd };
        } else {
            return new StartupCommand[] { cmd };
        }
    }

    protected HashMap<String, State> sync() {
        HashMap<String, State> newStates;
        HashMap<String, State> oldStates = null;

        final HashMap<String, State> changes = new HashMap<String, State>();

        synchronized (_vms) {
            newStates = getAllVms();
            if (newStates == null) {
                s_logger.debug("Unable to get the vm states so no state sync at this point.");
                return changes;
            }

            oldStates = new HashMap<String, State>(_vms.size());
            oldStates.putAll(_vms);

            for (final Map.Entry<String, State> entry : newStates.entrySet()) {
                final String vm = entry.getKey();

                State newState = entry.getValue();
                final State oldState = oldStates.remove(vm);

                if (newState == State.Stopped && oldState != State.Stopping
                        && oldState != null && oldState != State.Stopped) {
                    newState = getRealPowerState(vm);
                }

                if (s_logger.isTraceEnabled()) {
                    s_logger.trace("VM " + vm + ": libvirt has state "
                            + newState + " and we have state "
                            + (oldState != null ? oldState.toString() : "null"));
                }

                if (vm.startsWith("migrating")) {
                    s_logger.debug("Migration detected.  Skipping");
                    continue;
                }
                if (oldState == null) {
                    _vms.put(vm, newState);
                    s_logger.debug("Detecting a new state but couldn't find a old state so adding it to the changes: "
                            + vm);
                    changes.put(vm, newState);
                } else if (oldState == State.Starting) {
                    if (newState == State.Running) {
                        _vms.put(vm, newState);
                    } else if (newState == State.Stopped) {
                        s_logger.debug("Ignoring vm " + vm
                                + " because of a lag in starting the vm.");
                    }
                } else if (oldState == State.Migrating) {
                    if (newState == State.Running) {
                        s_logger.debug("Detected that an migrating VM is now running: "
                                + vm);
                        _vms.put(vm, newState);
                    }
                } else if (oldState == State.Stopping) {
                    if (newState == State.Stopped) {
                        _vms.put(vm, newState);
                    } else if (newState == State.Running) {
                        s_logger.debug("Ignoring vm " + vm
                                + " because of a lag in stopping the vm. ");
                    }
                } else if (oldState != newState) {
                    _vms.put(vm, newState);
                    if (newState == State.Stopped) {
                        if (_vmsKilled.remove(vm)) {
                            s_logger.debug("VM " + vm
                                    + " has been killed for storage. ");
                            newState = State.Error;
                        }
                    }
                    changes.put(vm, newState);
                }
            }

            for (final Map.Entry<String, State> entry : oldStates.entrySet()) {
                final String vm = entry.getKey();
                final State oldState = entry.getValue();

                if (s_logger.isTraceEnabled()) {
                    s_logger.trace("VM "
                            + vm
                            + " is now missing from libvirt so reporting stopped");
                }

                if (oldState == State.Stopping) {
                    s_logger.debug("Ignoring VM " + vm
                            + " in transition state stopping.");
                    _vms.remove(vm);
                } else if (oldState == State.Starting) {
                    s_logger.debug("Ignoring VM " + vm
                            + " in transition state starting.");
                } else if (oldState == State.Stopped) {
                    _vms.remove(vm);
                } else if (oldState == State.Migrating) {
                    s_logger.debug("Ignoring VM " + vm + " in migrating state.");
                } else {
                    _vms.remove(vm);
                    State state = State.Stopped;
                    if (_vmsKilled.remove(entry.getKey())) {
                        s_logger.debug("VM " + vm
                                + " has been killed by storage monitor");
                        state = State.Error;
                    }
                    changes.put(entry.getKey(), state);
                }
            }
        }

        return changes;
    }

    protected State getRealPowerState(String vm) {
        int i = 0;
        s_logger.trace("Checking on the HALTED State");
        Domain dm = null;
        for (; i < 5; i++) {
            try {
                Connect conn = LibvirtConnection.getConnection();
                dm = conn.domainLookupByUUID(UUID.nameUUIDFromBytes(vm
                        .getBytes()));
                DomainInfo.DomainState vps = dm.getInfo().state;
                if (vps != null
                        && vps != DomainInfo.DomainState.VIR_DOMAIN_SHUTOFF
                        && vps != DomainInfo.DomainState.VIR_DOMAIN_NOSTATE) {
                    return convertToState(vps);
                }
            } catch (final LibvirtException e) {
                s_logger.trace("Ignoring libvirt error.", e);
            } finally {
                try {
                    if (dm != null) {
                        dm.free();
                    }
                } catch (final LibvirtException l) {
                    s_logger.trace("Ignoring libvirt error.", l);
                }
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                s_logger.trace("Ignoring InterruptedException.", e);
            }
        }
        return State.Stopped;
    }

    protected List<String> getAllVmNames(Connect conn) {
        ArrayList<String> la = new ArrayList<String>();
        try {
            final String names[] = conn.listDefinedDomains();
            for (int i = 0; i < names.length; i++) {
                la.add(names[i]);
            }
        } catch (final LibvirtException e) {
            s_logger.warn("Failed to list Defined domains", e);
        }

        int[] ids = null;
        try {
            ids = conn.listDomains();
        } catch (final LibvirtException e) {
            s_logger.warn("Failed to list domains", e);
            return la;
        }

        Domain dm = null;
        for (int i = 0; i < ids.length; i++) {
            try {
                dm = conn.domainLookupByID(ids[i]);
                la.add(dm.getName());
            } catch (final LibvirtException e) {
                s_logger.warn("Unable to get vms", e);
            } finally {
                try {
                    if (dm != null) {
                        dm.free();
                    }
                } catch (final LibvirtException e) {
                    s_logger.trace("Ignoring libvirt error.", e);
                }
            }
        }

        return la;
    }

    private HashMap<String, State> getAllVms() {
        final HashMap<String, State> vmStates = new HashMap<String, State>();

        String[] vms = null;
        int[] ids = null;
        Connect conn = null;
        try {
            conn = LibvirtConnection.getConnection();
        } catch (LibvirtException e) {
            s_logger.debug("Failed to get connection: " + e.getMessage());
            return vmStates;
        }

        try {
            ids = conn.listDomains();
        } catch (final LibvirtException e) {
            s_logger.warn("Unable to listDomains", e);
            return null;
        }
        try {
            vms = conn.listDefinedDomains();
        } catch (final LibvirtException e) {
            s_logger.warn("Unable to listDomains", e);
            return null;
        }

        Domain dm = null;
        for (int i = 0; i < ids.length; i++) {
            try {
                dm = conn.domainLookupByID(ids[i]);

                DomainInfo.DomainState ps = dm.getInfo().state;

                final State state = convertToState(ps);

                s_logger.trace("VM " + dm.getName() + ": powerstate = " + ps
                        + "; vm state=" + state.toString());
                String vmName = dm.getName();
                vmStates.put(vmName, state);
            } catch (final LibvirtException e) {
                s_logger.warn("Unable to get vms", e);
            } finally {
                try {
                    if (dm != null) {
                        dm.free();
                    }
                } catch (LibvirtException e) {
                    s_logger.trace("Ignoring libvirt error.", e);
                }
            }
        }

        for (int i = 0; i < vms.length; i++) {
            try {

                dm = conn.domainLookupByUUID(UUID.nameUUIDFromBytes(vms[i]
                        .getBytes()));

                DomainInfo.DomainState ps = dm.getInfo().state;
                final State state = convertToState(ps);
                String vmName = dm.getName();
                s_logger.trace("VM " + vmName + ": powerstate = " + ps
                        + "; vm state=" + state.toString());

                vmStates.put(vmName, state);
            } catch (final LibvirtException e) {
                s_logger.warn("Unable to get vms", e);
            } finally {
                try {
                    if (dm != null) {
                        dm.free();
                    }
                } catch (LibvirtException e) {
                    s_logger.trace("Ignoring libvirt error.", e);
                }
            }
        }

        return vmStates;
    }

    protected List<Object> getHostInfo() {
        final ArrayList<Object> info = new ArrayList<Object>();
        long speed = 0;
        long cpus = 0;
        long ram = 0;
        String cap = null;
        try {
            Connect conn = LibvirtConnection.getConnection();
            final NodeInfo hosts = conn.nodeInfo();
            boolean result = false;
            try {
                BufferedReader in = new BufferedReader(
                        new FileReader(
                                "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq"));
                speed = Long.parseLong(in.readLine()) / 1000;
                result = true;
            } catch (FileNotFoundException e) {

            } catch (IOException e) {

            } catch (NumberFormatException e) {

            }

            if (!result) {
                speed = hosts.mhz;
            }

            cpus = hosts.cpus;
            ram = hosts.memory * 1024L;
            LibvirtCapXMLParser parser = new LibvirtCapXMLParser();
            parser.parseCapabilitiesXML(conn.getCapabilities());
            ArrayList<String> oss = parser.getGuestOsType();
            for (String s : oss) {
                /*
                 * Even host supports guest os type more than hvm, we only
                 * report hvm to management server
                 */
                if (s.equalsIgnoreCase("hvm")) {
                    cap = "hvm";
                }
            }
        } catch (LibvirtException e) {
            s_logger.trace("Ignoring libvirt error.", e);
        }

        if (isSnapshotSupported()) {
            cap = cap + ",snapshot";
        }

        info.add((int) cpus);
        info.add(speed);
        info.add(ram);
        info.add(cap);
        long dom0ram = Math.min(ram / 10, 768 * 1024 * 1024L);// save a maximum
        // of 10% of
        // system ram or
        // 768M
        dom0ram = Math.max(dom0ram, _dom0MinMem);
        info.add(dom0ram);
        s_logger.debug("cpus=" + cpus + ", speed=" + speed + ", ram=" + ram
                + ", dom0ram=" + dom0ram);

        return info;
    }

    protected void cleanupVM(Connect conn, final String vmName,
            final String vnet) {
        s_logger.debug("Trying to cleanup the vnet: " + vnet);
        if (vnet != null) {
            cleanupVnet(conn, vnet);
        }

        _vmStats.remove(vmName);
    }

    protected String rebootVM(Connect conn, String vmName) {
        Domain dm = null;
        String msg = null;
        try {
            dm = conn.domainLookupByUUID(UUID.nameUUIDFromBytes(vmName
                    .getBytes()));
            String vmDef = dm.getXMLDesc(0);
            s_logger.debug(vmDef);
            msg = stopVM(conn, vmName);
            msg = startVM(conn, vmName, vmDef);
            return null;
        } catch (LibvirtException e) {
            s_logger.warn("Failed to create vm", e);
            msg = e.getMessage();
        } catch (InternalErrorException e) {
            s_logger.warn("Failed to create vm", e);
            msg = e.getMessage();
        } finally {
            try {
                if (dm != null) {
                    dm.free();
                }
            } catch (LibvirtException e) {
                s_logger.trace("Ignoring libvirt error.", e);
            }
        }

        return msg;
    }

    protected String stopVM(Connect conn, String vmName) {
        DomainInfo.DomainState state = null;
        Domain dm = null;

        s_logger.debug("Try to stop the vm at first");
        String ret = stopVM(conn, vmName, false);
        if (ret == Script.ERR_TIMEOUT) {
            ret = stopVM(conn, vmName, true);
        } else if (ret != null) {
            /*
             * There is a race condition between libvirt and qemu: libvirt
             * listens on qemu's monitor fd. If qemu is shutdown, while libvirt
             * is reading on the fd, then libvirt will report an error.
             */
            /* Retry 3 times, to make sure we can get the vm's status */
            for (int i = 0; i < 3; i++) {
                try {
                    dm = conn.domainLookupByUUID(UUID.nameUUIDFromBytes(vmName
                            .getBytes()));
                    state = dm.getInfo().state;
                    break;
                } catch (LibvirtException e) {
                    s_logger.debug("Failed to get vm status:" + e.getMessage());
                } finally {
                    try {
                        if (dm != null) {
                            dm.free();
                        }
                    } catch (LibvirtException l) {
                        s_logger.trace("Ignoring libvirt error.", l);
                    }
                }
            }

            if (state == null) {
                s_logger.debug("Can't get vm's status, assume it's dead already");
                return null;
            }

            if (state != DomainInfo.DomainState.VIR_DOMAIN_SHUTOFF) {
                s_logger.debug("Try to destroy the vm");
                ret = stopVM(conn, vmName, true);
                if (ret != null) {
                    return ret;
                }
            }
        }

        return null;
    }

    protected String stopVM(Connect conn, String vmName, boolean force) {
        Domain dm = null;
        try {
            dm = conn.domainLookupByUUID(UUID.nameUUIDFromBytes(vmName
                    .getBytes()));
            int persist = dm.isPersistent();
            if (force) {
                if (dm.isActive() == 1) {
                    dm.destroy();
                    if (persist == 1) {
                        dm.undefine();
                    }
                }
            } else {
                if (dm.isActive() == 0) {
                    return null;
                }
                dm.shutdown();
                int retry = _stopTimeout / 2000;
                /* Wait for the domain gets into shutoff state. When it does
                   the dm object will no longer work, so we need to catch it. */
                try {
                    while ( dm.isActive() == 1 && (retry >= 0)) {
                        Thread.sleep(2000);
                        retry--;
                    }
                } catch (LibvirtException e) {
                    String error = e.toString();
                    if (error.contains("Domain not found")) {
                        s_logger.debug("successfully shut down vm " + vmName);
                    } else {
                        s_logger.debug("Error in waiting for vm shutdown:" + error);
                    }
                }
                if (retry < 0) {
                    s_logger.warn("Timed out waiting for domain " + vmName
                            + " to shutdown gracefully");
                    return Script.ERR_TIMEOUT;
                } else {
                    if (persist == 1) {
                        dm.undefine();
                    }
                }
            }
        } catch (LibvirtException e) {
            s_logger.debug("Failed to stop VM :" + vmName + " :", e);
            return e.getMessage();
        } catch (InterruptedException ie) {
            s_logger.debug("Interrupted sleep");
            return ie.getMessage();
        } finally {
            try {
                if (dm != null) {
                    dm.free();
                }
            } catch (LibvirtException e) {
                s_logger.trace("Ignoring libvirt error.", e);
            }
        }

        return null;
    }

    public synchronized String cleanupVnet(Connect conn, final String vnetId) {
        // VNC proxy VMs do not have vnet
        if (vnetId == null || vnetId.isEmpty()
                || isDirectAttachedNetwork(vnetId)) {
            return null;
        }

        final List<String> names = getAllVmNames(conn);

        if (!names.isEmpty()) {
            for (final String name : names) {
                if (VirtualMachineName.getVnet(name).equals(vnetId)) {
                    return null; // Can't remove the vnet yet.
                }
            }
        }

        final Script command = new Script(_modifyVlanPath, _timeout, s_logger);
        command.add("-o", "delete");
        command.add("-v", vnetId);
        return command.execute();
    }

    protected Integer getVncPort(Connect conn, String vmName)
            throws LibvirtException {
        LibvirtDomainXMLParser parser = new LibvirtDomainXMLParser();
        Domain dm = null;
        try {
            dm = conn.domainLookupByUUID(UUID.nameUUIDFromBytes(vmName
                    .getBytes()));
            String xmlDesc = dm.getXMLDesc(0);
            parser.parseDomainXML(xmlDesc);
            return parser.getVncPort();
        } finally {
            try {
                if (dm != null) {
                    dm.free();
                }
            } catch (LibvirtException l) {
                s_logger.trace("Ignoring libvirt error.", l);
            }
        }
    }

    private boolean IsHVMEnabled(Connect conn) {
        LibvirtCapXMLParser parser = new LibvirtCapXMLParser();
        try {
            parser.parseCapabilitiesXML(conn.getCapabilities());
            ArrayList<String> osTypes = parser.getGuestOsType();
            for (String o : osTypes) {
                if (o.equalsIgnoreCase("hvm")) {
                    return true;
                }
            }
        } catch (LibvirtException e) {
            s_logger.trace("Ignoring libvirt error.", e);
        }
        return false;
    }

    private String getHypervisorPath(Connect conn) {
        LibvirtCapXMLParser parser = new LibvirtCapXMLParser();
        try {
            parser.parseCapabilitiesXML(conn.getCapabilities());
        } catch (LibvirtException e) {
            s_logger.debug(e.getMessage());
        }
        return parser.getEmulator();
    }

    private String getGuestType(Connect conn, String vmName) {
        LibvirtDomainXMLParser parser = new LibvirtDomainXMLParser();
        Domain dm = null;
        try {
            dm = conn.domainLookupByUUID(UUID.nameUUIDFromBytes(vmName
                    .getBytes()));
            String xmlDesc = dm.getXMLDesc(0);
            parser.parseDomainXML(xmlDesc);
            return parser.getDescription();
        } catch (LibvirtException e) {
            s_logger.trace("Ignoring libvirt error.", e);
            return null;
        } finally {
            try {
                if (dm != null) {
                    dm.free();
                }
            } catch (LibvirtException l) {
                s_logger.trace("Ignoring libvirt error.", l);
            }
        }
    }

    boolean isGuestPVEnabled(String guestOS) {
        if (guestOS == null) {
            return false;
        }
        String guestOSName = KVMGuestOsMapper.getGuestOsName(guestOS);
        if (guestOS.startsWith("Ubuntu")
                || guestOSName.startsWith("Fedora 13")
                || guestOSName.startsWith("Fedora 12")
                || guestOSName.startsWith("Fedora 11")
                || guestOSName.startsWith("Fedora 10")
                || guestOSName.startsWith("Fedora 9")
                || guestOSName.startsWith("CentOS 5.3")
                || guestOSName.startsWith("CentOS 5.4")
                || guestOSName.startsWith("CentOS 5.5")
                || guestOS.startsWith("CentOS")
                || guestOS.startsWith("Fedora")
                || guestOSName.startsWith("Red Hat Enterprise Linux 5.3")
                || guestOSName.startsWith("Red Hat Enterprise Linux 5.4")
                || guestOSName.startsWith("Red Hat Enterprise Linux 5.5")
                || guestOSName.startsWith("Red Hat Enterprise Linux 6")
                || guestOS.startsWith("Debian GNU/Linux")
                || guestOSName.startsWith("Other PV")) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isCentosHost() {
        if (_hvVersion <= 9) {
            return true;
        } else {
            return false;
        }
    }

    private InterfaceDef.nicModel getGuestNicModel(String guestOSType) {
        if (isGuestPVEnabled(guestOSType)) {
            return InterfaceDef.nicModel.VIRTIO;
        } else {
            return InterfaceDef.nicModel.E1000;
        }
    }

    private DiskDef.diskBus getGuestDiskModel(String guestOSType) {
        if (isGuestPVEnabled(guestOSType)) {
            return DiskDef.diskBus.VIRTIO;
        } else {
            return DiskDef.diskBus.IDE;
        }
    }

    private String getVnetIdFromBrName(String vnetBrName) {
        if (vnetBrName.contains("cloudVirBr")) {
            return vnetBrName.replaceAll("cloudVirBr", "");
        } else {
            Pattern r = Pattern.compile("-(\\d+)$");
            Matcher m = r.matcher(vnetBrName);
            if(m.group(1) != null || !m.group(1).isEmpty()) {
                return m.group(1);
            } else {
                s_logger.debug("unable to get a vlan ID from name " + vnetBrName);
                return "";
            }
        }
    }

    private void cleanupVMNetworks(Connect conn, List<InterfaceDef> nics) {
        for (InterfaceDef nic : nics) {
            if (nic.getHostNetType() == hostNicType.VNET) {
                cleanupVnet(conn, getVnetIdFromBrName(nic.getBrName()));
            }
        }
    }

    private Domain getDomain(Connect conn, String vmName)
            throws LibvirtException {
        return conn
                .domainLookupByUUID(UUID.nameUUIDFromBytes(vmName.getBytes()));
    }

    protected List<InterfaceDef> getInterfaces(Connect conn, String vmName) {
        LibvirtDomainXMLParser parser = new LibvirtDomainXMLParser();
        Domain dm = null;
        try {
            dm = conn.domainLookupByUUID(UUID.nameUUIDFromBytes(vmName
                    .getBytes()));
            parser.parseDomainXML(dm.getXMLDesc(0));
            return parser.getInterfaces();

        } catch (LibvirtException e) {
            s_logger.debug("Failed to get dom xml: " + e.toString());
            return new ArrayList<InterfaceDef>();
        } finally {
            try {
                if (dm != null) {
                    dm.free();
                }
            } catch (LibvirtException e) {
                s_logger.trace("Ignoring libvirt error.", e);
            }
        }
    }

    protected List<DiskDef> getDisks(Connect conn, String vmName) {
        LibvirtDomainXMLParser parser = new LibvirtDomainXMLParser();
        Domain dm = null;
        try {
            dm = conn.domainLookupByUUID(UUID.nameUUIDFromBytes(vmName
                    .getBytes()));
            parser.parseDomainXML(dm.getXMLDesc(0));
            return parser.getDisks();

        } catch (LibvirtException e) {
            s_logger.debug("Failed to get dom xml: " + e.toString());
            return new ArrayList<DiskDef>();
        } finally {
            try {
                if (dm != null) {
                    dm.free();
                }
            } catch (LibvirtException e) {
                s_logger.trace("Ignoring libvirt error.", e);
            }
        }
    }

    private String executeBashScript(String script) {
        Script command = new Script("/bin/bash", _timeout, s_logger);
        command.add("-c");
        command.add(script);
        return command.execute();
    }

    private String executeBashScript(String script, OutputInterpreter parser) {
        Script command = new Script("/bin/bash", _timeout, s_logger);
        command.add("-c");
        command.add(script);
        return command.execute(parser);
    }

    private void deletExitingLinkLocalRoutTable(String linkLocalBr) {
        Script command = new Script("/bin/bash", _timeout);
        command.add("-c");
        command.add("ip route | grep " + NetUtils.getLinkLocalCIDR());
        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        String result = command.execute(parser);
        boolean foundLinkLocalBr = false;
        if (result == null && parser.getLines() != null) {
            String[] lines = parser.getLines().split("\\n");
            for (String line : lines) {
                String[] tokens = line.split(" ");
                if (!tokens[2].equalsIgnoreCase(linkLocalBr)) {
                    Script.runSimpleBashScript("ip route del "
                            + NetUtils.getLinkLocalCIDR());
                } else {
                    foundLinkLocalBr = true;
                }
            }
        }
        if (!foundLinkLocalBr) {
            Script.runSimpleBashScript("ip route add "
                    + NetUtils.getLinkLocalCIDR() + " dev " + linkLocalBr
                    + " src " + NetUtils.getLinkLocalGateway());
        }
    }

    private class vmStats {
        long _usedTime;
        long _tx;
        long _rx;
        Calendar _timestamp;
    }

    private VmStatsEntry getVmStat(Connect conn, String vmName)
            throws LibvirtException {
        Domain dm = null;
        try {
            dm = getDomain(conn, vmName);
            DomainInfo info = dm.getInfo();

            VmStatsEntry stats = new VmStatsEntry();
            stats.setNumCPUs(info.nrVirtCpu);
            stats.setEntityType("vm");

            /* get cpu utilization */
            vmStats oldStats = null;

            Calendar now = Calendar.getInstance();

            oldStats = _vmStats.get(vmName);

            long elapsedTime = 0;
            if (oldStats != null) {
                elapsedTime = now.getTimeInMillis()
                        - oldStats._timestamp.getTimeInMillis();
                double utilization = (info.cpuTime - oldStats._usedTime)
                        / ((double) elapsedTime * 1000000);

                NodeInfo node = conn.nodeInfo();
                utilization = utilization / node.cpus;
                if(utilization > 0){
                    stats.setCPUUtilization(utilization * 100);
                }
            }

            /* get network stats */

            List<InterfaceDef> vifs = getInterfaces(conn, vmName);
            long rx = 0;
            long tx = 0;
            for (InterfaceDef vif : vifs) {
                DomainInterfaceStats ifStats = dm.interfaceStats(vif
                        .getDevName());
                rx += ifStats.rx_bytes;
                tx += ifStats.tx_bytes;
            }

            if (oldStats != null) {
                long deltarx = rx - oldStats._rx;
                if (deltarx > 0)
                    stats.setNetworkReadKBs(deltarx / 1000);
                long deltatx = tx - oldStats._tx;
                if (deltatx > 0)
                    stats.setNetworkWriteKBs(deltatx / 1000);
            }

            vmStats newStat = new vmStats();
            newStat._usedTime = info.cpuTime;
            newStat._rx = rx;
            newStat._tx = tx;
            newStat._timestamp = now;
            _vmStats.put(vmName, newStat);
            return stats;
        } finally {
            if (dm != null) {
                dm.free();
            }
        }
    }

    private boolean can_bridge_firewall(String prvNic) {
        Script cmd = new Script(_securityGroupPath, _timeout, s_logger);
        cmd.add("can_bridge_firewall");
        cmd.add(prvNic);
        String result = cmd.execute();
        if (result != null) {
            return false;
        }
        return true;
    }

    protected boolean destroy_network_rules_for_vm(Connect conn, String vmName) {
        if (!_can_bridge_firewall) {
            return false;
        }
        String vif = null;
        List<InterfaceDef> intfs = getInterfaces(conn, vmName);
        if (intfs.size() > 0) {
            InterfaceDef intf = intfs.get(0);
            vif = intf.getDevName();
        }
        Script cmd = new Script(_securityGroupPath, _timeout, s_logger);
        cmd.add("destroy_network_rules_for_vm");
        cmd.add("--vmname", vmName);
        if (vif != null) {
            cmd.add("--vif", vif);
        }
        String result = cmd.execute();
        if (result != null) {
            return false;
        }
        return true;
    }

    protected boolean default_network_rules(Connect conn, String vmName,
            NicTO nic, Long vmId, String secIpStr) {
        if (!_can_bridge_firewall) {
            return false;
        }

        List<InterfaceDef> intfs = getInterfaces(conn, vmName);
        if (intfs.size() < nic.getDeviceId()) {
            return false;
        }

        InterfaceDef intf = intfs.get(nic.getDeviceId());
        String brname = intf.getBrName();
        String vif = intf.getDevName();

        Script cmd = new Script(_securityGroupPath, _timeout, s_logger);
        cmd.add("default_network_rules");
        cmd.add("--vmname", vmName);
        cmd.add("--vmid", vmId.toString());
        if (nic.getIp() != null) {
            cmd.add("--vmip", nic.getIp());
        }
        cmd.add("--vmmac", nic.getMac());
        cmd.add("--vif", vif);
        cmd.add("--brname", brname);
        cmd.add("--nicsecips", secIpStr);
        String result = cmd.execute();
        if (result != null) {
            return false;
        }
        return true;
    }

    protected boolean post_default_network_rules(Connect conn, String vmName,
            NicTO nic, Long vmId, InetAddress dhcpServerIp, String hostIp,
            String hostMacAddr) {
        if (!_can_bridge_firewall) {
            return false;
        }

        List<InterfaceDef> intfs = getInterfaces(conn, vmName);
        if (intfs.size() < nic.getDeviceId()) {
            return false;
        }

        InterfaceDef intf = intfs.get(nic.getDeviceId());
        String brname = intf.getBrName();
        String vif = intf.getDevName();

        Script cmd = new Script(_securityGroupPath, _timeout, s_logger);
        cmd.add("post_default_network_rules");
        cmd.add("--vmname", vmName);
        cmd.add("--vmid", vmId.toString());
        cmd.add("--vmip", nic.getIp());
        cmd.add("--vmmac", nic.getMac());
        cmd.add("--vif", vif);
        cmd.add("--brname", brname);
        if (dhcpServerIp != null)
            cmd.add("--dhcpSvr", dhcpServerIp.getHostAddress());

        cmd.add("--hostIp", hostIp);
        cmd.add("--hostMacAddr", hostMacAddr);
        String result = cmd.execute();
        if (result != null) {
            return false;
        }
        return true;
    }

    protected boolean default_network_rules_for_systemvm(Connect conn,
            String vmName) {
        if (!_can_bridge_firewall) {
            return false;
        }

        Script cmd = new Script(_securityGroupPath, _timeout, s_logger);
        cmd.add("default_network_rules_systemvm");
        cmd.add("--vmname", vmName);
        cmd.add("--localbrname", _linkLocalBridgeName);
        String result = cmd.execute();
        if (result != null) {
            return false;
        }
        return true;
    }

    private boolean add_network_rules(String vmName, String vmId,
            String guestIP, String sig, String seq, String mac, String rules,
            String vif, String brname, String secIps) {
        if (!_can_bridge_firewall) {
            return false;
        }

        String newRules = rules.replace(" ", ";");
        Script cmd = new Script(_securityGroupPath, _timeout, s_logger);
        cmd.add("add_network_rules");
        cmd.add("--vmname", vmName);
        cmd.add("--vmid", vmId);
        cmd.add("--vmip", guestIP);
        cmd.add("--sig", sig);
        cmd.add("--seq", seq);
        cmd.add("--vmmac", mac);
        cmd.add("--vif", vif);
        cmd.add("--brname", brname);
        cmd.add("--nicsecips", secIps);
        if (rules != null) {
            cmd.add("--rules", newRules);
        }
        String result = cmd.execute();
        if (result != null) {
            return false;
        }
        return true;
    }

    private boolean network_rules_vmSecondaryIp (Connect conn, String vmName, String secIp, String action) {

        if (!_can_bridge_firewall) {
            return false;
        }

        Script cmd = new Script(_securityGroupPath, _timeout, s_logger);
        cmd.add("network_rules_vmSecondaryIp");
        cmd.add("--vmname", vmName);
        cmd.add("--nicsecips", secIp);
        cmd.add("--action", action);

        String result = cmd.execute();
        if (result != null) {
            return false;
        }
        return true;
    }

    private boolean cleanup_rules() {
        if (!_can_bridge_firewall) {
            return false;
        }
        Script cmd = new Script(_securityGroupPath, _timeout, s_logger);
        cmd.add("cleanup_rules");
        String result = cmd.execute();
        if (result != null) {
            return false;
        }
        return true;
    }

    private String get_rule_logs_for_vms() {
        Script cmd = new Script(_securityGroupPath, _timeout, s_logger);
        cmd.add("get_rule_logs_for_vms");
        OutputInterpreter.OneLineParser parser = new OutputInterpreter.OneLineParser();
        String result = cmd.execute(parser);
        if (result == null) {
            return parser.getLine();
        }
        return null;
    }

    private HashMap<String, Pair<Long, Long>> syncNetworkGroups(long id) {
        HashMap<String, Pair<Long, Long>> states = new HashMap<String, Pair<Long, Long>>();

        String result = get_rule_logs_for_vms();
        s_logger.trace("syncNetworkGroups: id=" + id + " got: " + result);
        String[] rulelogs = result != null ? result.split(";") : new String[0];
        for (String rulesforvm : rulelogs) {
            String[] log = rulesforvm.split(",");
            if (log.length != 6) {
                continue;
            }
            try {
                states.put(log[0], new Pair<Long, Long>(Long.parseLong(log[1]),
                        Long.parseLong(log[5])));
            } catch (NumberFormatException nfe) {
                states.put(log[0], new Pair<Long, Long>(-1L, -1L));
            }
        }
        return states;
    }

    /* online snapshot supported by enhanced qemu-kvm */
    private boolean isSnapshotSupported() {
        String result = executeBashScript("qemu-img --help|grep convert");
        if (result != null) {
            return false;
        } else {
            return true;
        }
    }

    private Pair<Double, Double> getNicStats(String nicName) {
        double rx = 0.0;
        String rxFile = "/sys/class/net/" + nicName + "/statistics/rx_bytes";
        String rxContent = FileUtil.readFileAsString(rxFile);
        if (rxContent == null) {
            s_logger.warn("Failed to read the rx_bytes for " + nicName + " from " + rxFile);
        }
        rx = Double.parseDouble(rxContent);

        double tx = 0.0;
        String txFile = "/sys/class/net/" + nicName + "/statistics/tx_bytes";
        String txContent = FileUtil.readFileAsString(txFile);
        if (txContent == null) {
            s_logger.warn("Failed to read the tx_bytes for " + nicName + " from " + txFile);
        }
        tx = Double.parseDouble(txContent);

        return new Pair<Double, Double>(rx, tx);
    }

    private Answer execute(NetworkRulesSystemVmCommand cmd) {
        boolean success = false;
        Connect conn;
        try {
            conn = LibvirtConnection.getConnection();
            success = default_network_rules_for_systemvm(conn, cmd.getVmName());
        } catch (LibvirtException e) {
            s_logger.trace("Ignoring libvirt error.", e);
        }

        return new Answer(cmd, success, "");
    }

    private Answer execute(NetworkRulesVmSecondaryIpCommand cmd) {
        boolean success = false;
        Connect conn;
        try {
            conn = LibvirtConnection.getConnection();
            success = network_rules_vmSecondaryIp(conn, cmd.getVmName(), cmd.getVmSecIp(), cmd.getAction());
        } catch (LibvirtException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return new Answer(cmd, success, "");
    }

	@Override
	public void setName(String name) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setConfigParams(Map<String, Object> params) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Map<String, Object> getConfigParams() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getRunLevel() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setRunLevel(int level) {
		// TODO Auto-generated method stub
		
	}

}
