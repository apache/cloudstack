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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetAddress;
import java.net.URI;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.DomainBlockStats;
import org.libvirt.DomainInfo;
import org.libvirt.DomainInfo.DomainState;
import org.libvirt.DomainInterfaceStats;
import org.libvirt.DomainSnapshot;
import org.libvirt.LibvirtException;
import org.libvirt.NodeInfo;
import org.libvirt.StorageVol;

import com.ceph.rados.IoCTX;
import com.ceph.rados.Rados;
import com.ceph.rados.RadosException;
import com.ceph.rbd.Rbd;
import com.ceph.rbd.RbdException;
import com.ceph.rbd.RbdImage;

import org.apache.cloudstack.storage.command.StorageSubSystemCommand;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImg.PhysicalDiskFormat;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.apache.cloudstack.utils.qemu.QemuImgFile;

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
import com.cloud.agent.api.CheckOnHostCommand;
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
import com.cloud.agent.api.DeleteStoragePoolCommand;
import com.cloud.agent.api.FenceAnswer;
import com.cloud.agent.api.FenceCommand;
import com.cloud.agent.api.GetHostStatsAnswer;
import com.cloud.agent.api.GetHostStatsCommand;
import com.cloud.agent.api.GetStorageStatsAnswer;
import com.cloud.agent.api.GetStorageStatsCommand;
import com.cloud.agent.api.GetVmDiskStatsAnswer;
import com.cloud.agent.api.GetVmDiskStatsCommand;
import com.cloud.agent.api.GetVmStatsAnswer;
import com.cloud.agent.api.GetVmStatsCommand;
import com.cloud.agent.api.GetVncPortAnswer;
import com.cloud.agent.api.GetVncPortCommand;
import com.cloud.agent.api.HostStatsEntry;
import com.cloud.agent.api.HostVmStateReportEntry;
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
import com.cloud.agent.api.OvsCreateTunnelAnswer;
import com.cloud.agent.api.OvsCreateTunnelCommand;
import com.cloud.agent.api.OvsDestroyBridgeCommand;
import com.cloud.agent.api.OvsDestroyTunnelCommand;
import com.cloud.agent.api.OvsFetchInterfaceAnswer;
import com.cloud.agent.api.OvsFetchInterfaceCommand;
import com.cloud.agent.api.OvsSetupBridgeCommand;
import com.cloud.agent.api.OvsVpcPhysicalTopologyConfigCommand;
import com.cloud.agent.api.OvsVpcRoutingPolicyConfigCommand;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.PingRoutingCommand;
import com.cloud.agent.api.PingRoutingWithNwGroupsCommand;
import com.cloud.agent.api.PingTestCommand;
import com.cloud.agent.api.PlugNicAnswer;
import com.cloud.agent.api.PlugNicCommand;
import com.cloud.agent.api.PrepareForMigrationAnswer;
import com.cloud.agent.api.PrepareForMigrationCommand;
import com.cloud.agent.api.PvlanSetupCommand;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.RebootAnswer;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.RebootRouterCommand;
import com.cloud.agent.api.SecurityGroupRuleAnswer;
import com.cloud.agent.api.SecurityGroupRulesCmd;
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
import com.cloud.agent.api.VmDiskStatsEntry;
import com.cloud.agent.api.VmStatsEntry;
import com.cloud.agent.api.check.CheckSshAnswer;
import com.cloud.agent.api.check.CheckSshCommand;
import com.cloud.agent.api.proxy.CheckConsoleProxyLoadCommand;
import com.cloud.agent.api.proxy.ConsoleProxyLoadAnswer;
import com.cloud.agent.api.proxy.WatchConsoleProxyLoadCommand;
import com.cloud.agent.api.routing.IpAssocCommand;
import com.cloud.agent.api.routing.IpAssocVpcCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.routing.SetSourceNatCommand;
import com.cloud.agent.api.storage.CopyVolumeAnswer;
import com.cloud.agent.api.storage.CopyVolumeCommand;
import com.cloud.agent.api.storage.CreateAnswer;
import com.cloud.agent.api.storage.CreateCommand;
import com.cloud.agent.api.storage.CreatePrivateTemplateAnswer;
import com.cloud.agent.api.storage.DestroyCommand;
import com.cloud.agent.api.storage.PrimaryStorageDownloadAnswer;
import com.cloud.agent.api.storage.PrimaryStorageDownloadCommand;
import com.cloud.agent.api.storage.ResizeVolumeAnswer;
import com.cloud.agent.api.storage.ResizeVolumeCommand;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.IpAddressTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.agent.resource.virtualnetwork.VirtualRouterDeployer;
import com.cloud.agent.resource.virtualnetwork.VirtualRoutingResource;
import com.cloud.dc.Vlan;
import com.cloud.exception.InternalErrorException;
import com.cloud.host.Host.Type;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.kvm.resource.KVMHABase.NfsStoragePool;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.ClockDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.ConsoleDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.CpuModeDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.CpuTuneDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.DevicesDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.DiskDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.DiskDef.deviceType;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.DiskDef.diskProtocol;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.FeaturesDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.FilesystemDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.GraphicDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.GuestDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.GuestResourceDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.InputDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.InterfaceDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.InterfaceDef.guestNetType;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.SerialDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.TermPolicy;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.VideoDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.VirtioSerialDef;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.hypervisor.kvm.storage.KVMStorageProcessor;
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
import com.cloud.storage.resource.StorageSubsystemCommandHandler;
import com.cloud.storage.resource.StorageSubsystemCommandHandlerBase;
import com.cloud.storage.template.Processor;
import com.cloud.storage.template.Processor.FormatInfo;
import com.cloud.storage.template.QCOW2Processor;
import com.cloud.storage.template.TemplateLocation;
import com.cloud.storage.template.TemplateProp;
import com.cloud.utils.ExecutionResult;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.OutputInterpreter.AllLinesParser;
import com.cloud.utils.script.Script;
import com.cloud.utils.ssh.SshHelper;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.PowerState;

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
@Local(value = {ServerResource.class})
public class LibvirtComputingResource extends ServerResourceBase implements ServerResource, VirtualRouterDeployer {
    private static final Logger s_logger = Logger.getLogger(LibvirtComputingResource.class);

    private String _modifyVlanPath;
    private String _versionstringpath;
    private String _patchViaSocketPath;
    private String _createvmPath;
    private String _manageSnapshotPath;
    private String _resizeVolumePath;
    private String _createTmplPath;
    private String _heartBeatPath;
    private String _securityGroupPath;
    private String _ovsPvlanDhcpHostPath;
    private String _ovsPvlanVmPath;
    private String _routerProxyPath;
    private String _ovsTunnelPath;
    private String _setupCgroupPath;
    private String _host;
    private String _dcId;
    private String _pod;
    private String _clusterId;
    private int _migrateSpeed;
    private int _migrateDowntime;
    private int _migratePauseAfter;

    private long _hvVersion;
    private long _kernelVersion;
    private KVMHAMonitor _monitor;
    private static final String SSHKEYSPATH = "/root/.ssh";
    private static final String SSHPRVKEYPATH = SSHKEYSPATH + File.separator + "id_rsa.cloud";
    private static final String SSHPUBKEYPATH = SSHKEYSPATH + File.separator + "id_rsa.pub.cloud";
    private String _mountPoint = "/mnt";
    StorageLayer _storage;
    private KVMStoragePoolManager _storagePoolMgr;

    private VifDriver _defaultVifDriver;
    private Map<TrafficType, VifDriver> _trafficTypeVifDrivers;
    protected static final String DEFAULT_OVS_VIF_DRIVER_CLASS_NAME = "com.cloud.hypervisor.kvm.resource.OvsVifDriver";
    protected static final String DEFAULT_BRIDGE_VIF_DRIVER_CLASS_NAME = "com.cloud.hypervisor.kvm.resource.BridgeVifDriver";

    @Override
    public ExecutionResult executeInVR(String routerIp, String script, String args) {
        return executeInVR(routerIp, script, args, _timeout / 1000);
    }

    @Override
    public ExecutionResult executeInVR(String routerIp, String script, String args, int timeout) {
        final Script command = new Script(_routerProxyPath, timeout * 1000, s_logger);
        final AllLinesParser parser = new AllLinesParser();
        command.add(script);
        command.add(routerIp);
        if (args != null) {
            command.add(args);
        }
        String details = command.execute(parser);
        if (details == null) {
            details = parser.getLines();
        }
        return new ExecutionResult(command.getExitValue() == 0, details);
    }

    @Override
    public ExecutionResult createFileInVR(String routerIp, String path, String filename, String content) {
        File permKey = new File("/root/.ssh/id_rsa.cloud");
        String error = null;

        try {
            SshHelper.scpTo(routerIp, 3922, "root", permKey, null, path, content.getBytes(), filename, null);
        } catch (Exception e) {
            s_logger.warn("Fail to create file " + path + filename + " in VR " + routerIp, e);
            error = e.getMessage();
        }
        return new ExecutionResult(error == null, error);
    }

    @Override
    public ExecutionResult prepareCommand(NetworkElementCommand cmd) {
        //Update IP used to access router
        cmd.setRouterAccessIp(cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP));
        assert cmd.getRouterAccessIp() != null;

        if (cmd instanceof IpAssocVpcCommand) {
            return prepareNetworkElementCommand((IpAssocVpcCommand)cmd);
        } else if (cmd instanceof IpAssocCommand) {
            return prepareNetworkElementCommand((IpAssocCommand)cmd);
        } else if (cmd instanceof SetupGuestNetworkCommand) {
            return prepareNetworkElementCommand((SetupGuestNetworkCommand)cmd);
        } else if (cmd instanceof SetSourceNatCommand) {
            return prepareNetworkElementCommand((SetSourceNatCommand)cmd);
        }
        return new ExecutionResult(true, null);
    }

    @Override
    public ExecutionResult cleanupCommand(NetworkElementCommand cmd) {
        if (cmd instanceof IpAssocCommand && !(cmd instanceof IpAssocVpcCommand)) {
            return cleanupNetworkElementCommand((IpAssocCommand)cmd);
        }
        return new ExecutionResult(true, null);
    }

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

    protected static final MessageFormat SnapshotXML = new MessageFormat("   <domainsnapshot>" + "       <name>{0}</name>" + "          <domain>"
            + "            <uuid>{1}</uuid>" + "        </domain>" + "    </domainsnapshot>");

    protected HypervisorType _hypervisorType;
    protected String _hypervisorURI;
    protected long _hypervisorLibvirtVersion;
    protected long _hypervisorQemuVersion;
    protected String _hypervisorPath;
    protected String _networkDirectSourceMode;
    protected String _networkDirectDevice;
    protected String _sysvmISOPath;
    protected String _privNwName;
    protected String _privBridgeName;
    protected String _linkLocalBridgeName;
    protected String _publicBridgeName;
    protected String _guestBridgeName;
    protected String _privateIp;
    protected String _pool;
    protected String _localGateway;
    private boolean _canBridgeFirewall;
    protected String _localStoragePath;
    protected String _localStorageUUID;
    protected boolean _noMemBalloon = false;
    protected String _guestCpuMode;
    protected String _guestCpuModel;
    protected boolean _noKvmClock;
    protected String _videoHw;
    protected int _videoRam;
    protected Pair<Integer,Integer> hostOsVersion;
    private final Map <String, String> _pifs = new HashMap<String, String>();
    private final Map<String, VmStats> _vmStats = new ConcurrentHashMap<String, VmStats>();

    protected boolean _disconnected = true;
    protected int _timeout;
    protected int _cmdsTimeout;
    protected int _stopTimeout;

    protected static final HashMap<DomainState, PowerState> s_powerStatesTable;
    static {
        s_powerStatesTable = new HashMap<DomainState, PowerState>();
        s_powerStatesTable.put(DomainState.VIR_DOMAIN_SHUTOFF, PowerState.PowerOff);
        s_powerStatesTable.put(DomainState.VIR_DOMAIN_PAUSED, PowerState.PowerOn);
        s_powerStatesTable.put(DomainState.VIR_DOMAIN_RUNNING, PowerState.PowerOn);
        s_powerStatesTable.put(DomainState.VIR_DOMAIN_BLOCKED, PowerState.PowerOn);
        s_powerStatesTable.put(DomainState.VIR_DOMAIN_NOSTATE, PowerState.PowerUnknown);
        s_powerStatesTable.put(DomainState.VIR_DOMAIN_SHUTDOWN, PowerState.PowerOff);
    }

    protected List<String> _vmsKilled = new ArrayList<String>();

    private VirtualRoutingResource _virtRouterResource;

    private String _pingTestPath;

    private int _dom0MinMem;

    protected enum BridgeType {
        NATIVE, OPENVSWITCH
    }

    protected BridgeType _bridgeType;

    protected StorageSubsystemCommandHandler storageHandler;

    private String getEndIpFromStartIp(String startIp, int numIps) {
        String[] tokens = startIp.split("[.]");
        assert (tokens.length == 4);
        int lastbyte = Integer.parseInt(tokens[3]);
        lastbyte = lastbyte + numIps;
        tokens[3] = Integer.toString(lastbyte);
        StringBuilder end = new StringBuilder(15);
        end.append(tokens[0]).append(".").append(tokens[1]).append(".").append(tokens[2]).append(".").append(tokens[3]);
        return end.toString();
    }

    private Map<String, Object> getDeveloperProperties() throws ConfigurationException {

        final File file = PropertiesUtil.findConfigFile("developer.properties");
        if (file == null) {
            throw new ConfigurationException("Unable to find developer.properties.");
        }

        s_logger.info("developer.properties found at " + file.getAbsolutePath());
        try {
            Properties properties = PropertiesUtil.loadFromFile(file);

            String startMac = (String)properties.get("private.macaddr.start");
            if (startMac == null) {
                throw new ConfigurationException("Developers must specify start mac for private ip range");
            }

            String startIp = (String)properties.get("private.ipaddr.start");
            if (startIp == null) {
                throw new ConfigurationException("Developers must specify start ip for private ip range");
            }
            final Map<String, Object> params = PropertiesUtil.toMap(properties);

            String endIp = (String)properties.get("private.ipaddr.end");
            if (endIp == null) {
                endIp = getEndIpFromStartIp(startIp, 16);
                params.put("private.ipaddr.end", endIp);
            }
            return params;
        } catch (final FileNotFoundException ex) {
            throw new CloudRuntimeException("Cannot find the file: " + file.getAbsolutePath(), ex);
        } catch (final IOException ex) {
            throw new CloudRuntimeException("IOException in reading " + file.getAbsolutePath(), ex);
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
        return "scripts/network/domr";
    }

    protected String getNetworkDirectSourceMode() {
        return _networkDirectSourceMode;
    }

    protected String getNetworkDirectDevice() {
        return _networkDirectDevice;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        boolean success = super.configure(name, params);
        if (!success) {
            return false;
        }

        _storage = new JavaStorageLayer();
        _storage.configure("StorageLayer", params);

        String domrScriptsDir = (String)params.get("domr.scripts.dir");
        if (domrScriptsDir == null) {
            domrScriptsDir = getDefaultDomrScriptsDir();
        }

        String kvmScriptsDir = (String)params.get("kvm.scripts.dir");
        if (kvmScriptsDir == null) {
            kvmScriptsDir = getDefaultKvmScriptsDir();
        }

        String networkScriptsDir = (String)params.get("network.scripts.dir");
        if (networkScriptsDir == null) {
            networkScriptsDir = getDefaultNetworkScriptsDir();
        }

        String storageScriptsDir = (String)params.get("storage.scripts.dir");
        if (storageScriptsDir == null) {
            storageScriptsDir = getDefaultStorageScriptsDir();
        }

        String bridgeType = (String)params.get("network.bridge.type");
        if (bridgeType == null) {
            _bridgeType = BridgeType.NATIVE;
        } else {
            _bridgeType = BridgeType.valueOf(bridgeType.toUpperCase());
        }

        params.put("domr.scripts.dir", domrScriptsDir);

        _virtRouterResource = new VirtualRoutingResource(this);
        success = _virtRouterResource.configure(name, params);

        if (!success) {
            return false;
        }

        _host = (String)params.get("host");
        if (_host == null) {
            _host = "localhost";
        }

        _dcId = (String)params.get("zone");
        if (_dcId == null) {
            _dcId = "default";
        }

        _pod = (String)params.get("pod");
        if (_pod == null) {
            _pod = "default";
        }

        _clusterId = (String)params.get("cluster");

        _modifyVlanPath = Script.findScript(networkScriptsDir, "modifyvlan.sh");
        if (_modifyVlanPath == null) {
            throw new ConfigurationException("Unable to find modifyvlan.sh");
        }

        _versionstringpath = Script.findScript(kvmScriptsDir, "versions.sh");
        if (_versionstringpath == null) {
            throw new ConfigurationException("Unable to find versions.sh");
        }

        _patchViaSocketPath = Script.findScript(kvmScriptsDir + "/patch/", "patchviasocket.pl");
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

        _manageSnapshotPath = Script.findScript(storageScriptsDir, "managesnapshot.sh");
        if (_manageSnapshotPath == null) {
            throw new ConfigurationException("Unable to find the managesnapshot.sh");
        }

        _resizeVolumePath = Script.findScript(storageScriptsDir, "resizevolume.sh");
        if (_resizeVolumePath == null) {
            throw new ConfigurationException("Unable to find the resizevolume.sh");
        }

        _createTmplPath = Script.findScript(storageScriptsDir, "createtmplt.sh");
        if (_createTmplPath == null) {
            throw new ConfigurationException("Unable to find the createtmplt.sh");
        }

        _securityGroupPath = Script.findScript(networkScriptsDir, "security_group.py");
        if (_securityGroupPath == null) {
            throw new ConfigurationException("Unable to find the security_group.py");
        }

        _ovsTunnelPath = Script.findScript(networkScriptsDir, "ovstunnel.py");
        if (_ovsTunnelPath == null) {
            throw new ConfigurationException("Unable to find the ovstunnel.py");
        }

        _routerProxyPath = Script.findScript("scripts/network/domr/", "router_proxy.sh");
        if (_routerProxyPath == null) {
            throw new ConfigurationException("Unable to find the router_proxy.sh");
        }

        _ovsPvlanDhcpHostPath = Script.findScript(networkScriptsDir, "ovs-pvlan-dhcp-host.sh");
        if (_ovsPvlanDhcpHostPath == null) {
            throw new ConfigurationException("Unable to find the ovs-pvlan-dhcp-host.sh");
        }

        _ovsPvlanVmPath = Script.findScript(networkScriptsDir, "ovs-pvlan-vm.sh");
        if (_ovsPvlanVmPath == null) {
            throw new ConfigurationException("Unable to find the ovs-pvlan-vm.sh");
        }

        String value = (String)params.get("developer");
        boolean isDeveloper = Boolean.parseBoolean(value);

        if (isDeveloper) {
            params.putAll(getDeveloperProperties());
        }

        _pool = (String)params.get("pool");
        if (_pool == null) {
            _pool = "/root";
        }

        String instance = (String)params.get("instance");

        _hypervisorType = HypervisorType.getType((String)params.get("hypervisor.type"));
        if (_hypervisorType == HypervisorType.None) {
            _hypervisorType = HypervisorType.KVM;
        }

        //Verify that cpu,cpuacct cgroups are not co-mounted
        if(HypervisorType.LXC.equals(getHypervisorType())){
            _setupCgroupPath = Script.findScript(kvmScriptsDir, "setup-cgroups.sh");
            if (_setupCgroupPath == null) {
                throw new ConfigurationException("Unable to find the setup-cgroups.sh");
            }
            if(!checkCgroups()){
                throw new ConfigurationException("cpu,cpuacct cgroups are co-mounted");
            }
        }

        _hypervisorURI = (String)params.get("hypervisor.uri");
        if (_hypervisorURI == null) {
            _hypervisorURI = LibvirtConnection.getHypervisorURI(_hypervisorType.toString());
        }

        _networkDirectSourceMode = (String)params.get("network.direct.source.mode");
        _networkDirectDevice = (String)params.get("network.direct.device");

        String startMac = (String)params.get("private.macaddr.start");
        if (startMac == null) {
            startMac = "00:16:3e:77:e2:a0";
        }

        String startIp = (String)params.get("private.ipaddr.start");
        if (startIp == null) {
            startIp = "192.168.166.128";
        }

        _pingTestPath = Script.findScript(kvmScriptsDir, "pingtest.sh");
        if (_pingTestPath == null) {
            throw new ConfigurationException("Unable to find the pingtest.sh");
        }

        _linkLocalBridgeName = (String)params.get("private.bridge.name");
        if (_linkLocalBridgeName == null) {
            if (isDeveloper) {
                _linkLocalBridgeName = "cloud-" + instance + "-0";
            } else {
                _linkLocalBridgeName = "cloud0";
            }
        }

        _publicBridgeName = (String)params.get("public.network.device");
        if (_publicBridgeName == null) {
            _publicBridgeName = "cloudbr0";
        }

        _privBridgeName = (String)params.get("private.network.device");
        if (_privBridgeName == null) {
            _privBridgeName = "cloudbr1";
        }

        _guestBridgeName = (String)params.get("guest.network.device");
        if (_guestBridgeName == null) {
            _guestBridgeName = _privBridgeName;
        }

        _privNwName = (String)params.get("private.network.name");
        if (_privNwName == null) {
            if (isDeveloper) {
                _privNwName = "cloud-" + instance + "-private";
            } else {
                _privNwName = "cloud-private";
            }
        }

        _localStoragePath = (String)params.get("local.storage.path");
        if (_localStoragePath == null) {
            _localStoragePath = "/var/lib/libvirt/images/";
        }

        File storagePath = new File(_localStoragePath);
        _localStoragePath = storagePath.getAbsolutePath();

        _localStorageUUID = (String)params.get("local.storage.uuid");
        if (_localStorageUUID == null) {
            _localStorageUUID = UUID.randomUUID().toString();
        }

        value = (String)params.get("scripts.timeout");
        _timeout = NumbersUtil.parseInt(value, 30 * 60) * 1000;

        value = (String)params.get("stop.script.timeout");
        _stopTimeout = NumbersUtil.parseInt(value, 120) * 1000;

        value = (String)params.get("cmds.timeout");
        _cmdsTimeout = NumbersUtil.parseInt(value, 7200) * 1000;

        value = (String) params.get("vm.memballoon.disable");
        if (Boolean.parseBoolean(value)) {
            _noMemBalloon = true;
        }

        _videoHw = (String) params.get("vm.video.hardware");
        value = (String) params.get("vm.video.ram");
        _videoRam = NumbersUtil.parseInt(value, 0);

        value = (String)params.get("host.reserved.mem.mb");
        _dom0MinMem = NumbersUtil.parseInt(value, 0) * 1024 * 1024;

        value = (String) params.get("kvmclock.disable");
        if (Boolean.parseBoolean(value)) {
            _noKvmClock = true;
        } else if(HypervisorType.LXC.equals(_hypervisorType) && (value == null)){
            //Disable kvmclock by default for LXC
            _noKvmClock = true;
        }

        LibvirtConnection.initialize(_hypervisorURI);
        Connect conn = null;
        try {
            conn = LibvirtConnection.getConnection();

            if (_bridgeType == BridgeType.OPENVSWITCH) {
                if (conn.getLibVirVersion() < (10 * 1000 + 0)) {
                    throw new ConfigurationException("LibVirt version 0.10.0 required for openvswitch support, but version " + conn.getLibVirVersion() + " detected");
                }
            }
        } catch (LibvirtException e) {
            throw new CloudRuntimeException(e.getMessage());
        }

        if (HypervisorType.KVM == _hypervisorType) {
            /* Does node support HVM guest? If not, exit */
            if (!IsHVMEnabled(conn)) {
                throw new ConfigurationException("NO HVM support on this machine, please make sure: " + "1. VT/SVM is supported by your CPU, or is enabled in BIOS. "
                        + "2. kvm modules are loaded (kvm, kvm_amd|kvm_intel)");
            }
        }

        _hypervisorPath = getHypervisorPath(conn);
        try {
            _hvVersion = conn.getVersion();
            _hvVersion = (_hvVersion % 1000000) / 1000;
            _hypervisorLibvirtVersion = conn.getLibVirVersion();
            _hypervisorQemuVersion = conn.getVersion();
        } catch (LibvirtException e) {
            s_logger.trace("Ignoring libvirt error.", e);
        }

        _guestCpuMode = (String)params.get("guest.cpu.mode");
        if (_guestCpuMode != null) {
            _guestCpuModel = (String)params.get("guest.cpu.model");

            if (_hypervisorLibvirtVersion < (9 * 1000 + 10)) {
                s_logger.warn("LibVirt version 0.9.10 required for guest cpu mode, but version " + prettyVersion(_hypervisorLibvirtVersion) +
                        " detected, so it will be disabled");
                _guestCpuMode = "";
                _guestCpuModel = "";
            }
            params.put("guest.cpu.mode", _guestCpuMode);
            params.put("guest.cpu.model", _guestCpuModel);
        }

        String[] info = NetUtils.getNetworkParams(_privateNic);

        _monitor = new KVMHAMonitor(null, info[0], _heartBeatPath);
        Thread ha = new Thread(_monitor);
        ha.start();

        _storagePoolMgr = new KVMStoragePoolManager(_storage, _monitor);

        _sysvmISOPath = (String)params.get("systemvm.iso.path");
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
        s_logger.debug("Found pif: " + _pifs.get("private") + " on " + _privBridgeName + ", pif: " + _pifs.get("public") + " on " + _publicBridgeName);

        _canBridgeFirewall = can_bridge_firewall(_pifs.get("public"));

        _localGateway = Script.runSimpleBashScript("ip route |grep default|awk '{print $3}'");
        if (_localGateway == null) {
            s_logger.debug("Failed to found the local gateway");
        }

        _mountPoint = (String)params.get("mount.path");
        if (_mountPoint == null) {
            _mountPoint = "/mnt";
        }

        value = (String) params.get("vm.migrate.downtime");
        _migrateDowntime = NumbersUtil.parseInt(value, -1);

        value = (String) params.get("vm.migrate.pauseafter");
        _migratePauseAfter = NumbersUtil.parseInt(value, -1);

        value = (String)params.get("vm.migrate.speed");
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
        params.put("libvirtVersion", _hypervisorLibvirtVersion);

        configureVifDrivers(params);

        KVMStorageProcessor storageProcessor = new KVMStorageProcessor(_storagePoolMgr, this);
        storageProcessor.configure(name, params);
        storageHandler = new StorageSubsystemCommandHandlerBase(storageProcessor);

        String unameKernelVersion = Script.runSimpleBashScript("uname -r");
        String[] kernelVersions = unameKernelVersion.split("[\\.\\-]");
        _kernelVersion = Integer.parseInt(kernelVersions[0]) * 1000 * 1000 + (long)Integer.parseInt(kernelVersions[1]) * 1000 + Integer.parseInt(kernelVersions[2]);

        /* Disable this, the code using this is pretty bad and non portable
         * getOsVersion();
         */
        return true;
    }

    protected void configureVifDrivers(Map<String, Object> params) throws ConfigurationException {
        final String LIBVIRT_VIF_DRIVER = "libvirt.vif.driver";

        _trafficTypeVifDrivers = new HashMap<TrafficType, VifDriver>();

        // Load the default vif driver
        String defaultVifDriverName = (String)params.get(LIBVIRT_VIF_DRIVER);
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
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String k = entry.getKey();
            String vifDriverPrefix = LIBVIRT_VIF_DRIVER + ".";

            if (k.startsWith(vifDriverPrefix)) {
                // Get trafficType
                String trafficTypeSuffix = k.substring(vifDriverPrefix.length());

                // Does this suffix match a real traffic type?
                TrafficType trafficType = TrafficType.getTrafficType(trafficTypeSuffix);
                if (!trafficType.equals(TrafficType.None)) {
                    // Get vif driver class name
                    String vifDriverClassName = (String)entry.getValue();
                    // if value is null, ignore
                    if (vifDriverClassName != null) {
                        // add traffic type to vif driver mapping to Map
                        _trafficTypeVifDrivers.put(trafficType, getVifDriverClass(vifDriverClassName, params));
                    }
                }
            }
        }
    }

    protected VifDriver getVifDriverClass(String vifDriverClassName, Map<String, Object> params) throws ConfigurationException {
        VifDriver vifDriver;

        try {
            Class<?> clazz = Class.forName(vifDriverClassName);
            vifDriver = (VifDriver)clazz.newInstance();
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

    protected VifDriver getVifDriver(TrafficType trafficType) {
        VifDriver vifDriver = _trafficTypeVifDrivers.get(trafficType);

        if (vifDriver == null) {
            vifDriver = _defaultVifDriver;
        }

        return vifDriver;
    }

    protected List<VifDriver> getAllVifDrivers() {
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
            if (_publicBridgeName != null && bridge.equals(_publicBridgeName)) {
                _pifs.put("public", pif);
            }
            if (_guestBridgeName != null && bridge.equals(_guestBridgeName)) {
                _pifs.put("private", pif);
            }
            _pifs.put(bridge, pif);
        }

        // guest(private) creates bridges on a pif, if private bridge not found try pif direct
        // This addresses the unnecessary requirement of someone to create an unused bridge just for traffic label
        if (_pifs.get("private") == null) {
            s_logger.debug("guest(private) traffic label '" + _guestBridgeName + "' not found as bridge, looking for physical interface");
            File dev = new File("/sys/class/net/" + _guestBridgeName);
            if (dev.exists()) {
                s_logger.debug("guest(private) traffic label '" + _guestBridgeName + "' found as a physical device");
                _pifs.put("private", _guestBridgeName);
            }
        }

        // public creates bridges on a pif, if private bridge not found try pif direct
        // This addresses the unnecessary requirement of someone to create an unused bridge just for traffic label
        if (_pifs.get("public") == null) {
            s_logger.debug("public traffic label '" + _publicBridgeName+ "' not found as bridge, looking for physical interface");
            File dev = new File("/sys/class/net/" + _publicBridgeName);
            if (dev.exists()) {
                s_logger.debug("public traffic label '" + _publicBridgeName + "' found as a physical device");
                _pifs.put("public", _publicBridgeName);
            }
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
            pif = Script.runSimpleBashScript("grep ^Device\\: /proc/net/vlan/" + pif + " | awk {'print $2'}");
        }

        return pif;
    }

    private String matchPifFileInDirectory(String bridgeName) {
        File brif = new File("/sys/devices/virtual/net/" + bridgeName + "/brif");

        if (!brif.isDirectory()) {
            File pif = new File("/sys/class/net/" + bridgeName);
            if (pif.isDirectory()) {
                // if bridgeName already refers to a pif, return it as-is
                return bridgeName;
            }
            s_logger.debug("failing to get physical interface from bridge " + bridgeName + ", does " + brif.getAbsolutePath() + "exist?");
            return "";
        }

        File[] interfaces = brif.listFiles();

        for (int i = 0; i < interfaces.length; i++) {
            String fname = interfaces[i].getName();
            s_logger.debug("matchPifFileInDirectory: file name '" + fname + "'");
            if (fname.startsWith("eth") || fname.startsWith("bond") || fname.startsWith("vlan") || fname.startsWith("vx") || fname.startsWith("em") ||
                    fname.matches("^p\\d+p\\d+.*")) {
                return fname;
            }
        }

        s_logger.debug("failing to get physical interface from bridge " + bridgeName + ", did not find an eth*, bond*, vlan*, em*, or p*p* in " + brif.getAbsolutePath());
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
        return "0".equals(command.execute(null));
    }

    private boolean passCmdLine(String vmName, String cmdLine) throws InternalErrorException {
        final Script command = new Script(_patchViaSocketPath, 5 * 1000, s_logger);
        String result;
        command.add("-n", vmName);
        command.add("-p", cmdLine.replaceAll(" ", "%"));
        result = command.execute();
        if (result != null) {
            s_logger.debug("passcmd failed:" + result);
            return false;
        }
        return true;
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

    protected String startVM(Connect conn, String vmName, String domainXML) throws LibvirtException, InternalErrorException {
        try {
            /*
                We create a transient domain here. When this method gets
                called we receive a full XML specification of the guest,
                so no need to define it persistent.

                This also makes sure we never have any old "garbage" defined
                in libvirt which might haunt us.
             */

            // check for existing inactive vm definition and remove it
            // this can sometimes happen during crashes, etc
            Domain dm = null;
            try {
                dm = conn.domainLookupByName(vmName);
                if (dm != null && dm.isPersistent() == 1) {
                    // this is safe because it doesn't stop running VMs
                    dm.undefine();
                }
            } catch (LibvirtException e) {
                // this is what we want, no domain found
            } finally {
                if (dm != null) {
                    dm.free();
                }
            }

            conn.domainCreateXML(domainXML, 0);
        } catch (final LibvirtException e) {
            throw e;
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
                return execute((StopCommand)cmd);
            } else if (cmd instanceof GetVmStatsCommand) {
                return execute((GetVmStatsCommand)cmd);
            } else if (cmd instanceof GetVmDiskStatsCommand) {
                return execute((GetVmDiskStatsCommand)cmd);
            } else if (cmd instanceof RebootRouterCommand) {
                return execute((RebootRouterCommand)cmd);
            } else if (cmd instanceof RebootCommand) {
                return execute((RebootCommand)cmd);
            } else if (cmd instanceof GetHostStatsCommand) {
                return execute((GetHostStatsCommand)cmd);
            } else if (cmd instanceof CheckStateCommand) {
                return executeRequest(cmd);
            } else if (cmd instanceof CheckHealthCommand) {
                return execute((CheckHealthCommand)cmd);
            } else if (cmd instanceof PrepareForMigrationCommand) {
                return execute((PrepareForMigrationCommand)cmd);
            } else if (cmd instanceof MigrateCommand) {
                return execute((MigrateCommand)cmd);
            } else if (cmd instanceof PingTestCommand) {
                return execute((PingTestCommand)cmd);
            } else if (cmd instanceof CheckVirtualMachineCommand) {
                return execute((CheckVirtualMachineCommand)cmd);
            } else if (cmd instanceof ReadyCommand) {
                return execute((ReadyCommand)cmd);
            } else if (cmd instanceof AttachIsoCommand) {
                return execute((AttachIsoCommand)cmd);
            } else if (cmd instanceof AttachVolumeCommand) {
                return execute((AttachVolumeCommand)cmd);
            } else if (cmd instanceof CheckConsoleProxyLoadCommand) {
                return execute((CheckConsoleProxyLoadCommand)cmd);
            } else if (cmd instanceof WatchConsoleProxyLoadCommand) {
                return execute((WatchConsoleProxyLoadCommand)cmd);
            } else if (cmd instanceof GetVncPortCommand) {
                return execute((GetVncPortCommand)cmd);
            } else if (cmd instanceof ModifySshKeysCommand) {
                return execute((ModifySshKeysCommand)cmd);
            } else if (cmd instanceof MaintainCommand) {
                return execute((MaintainCommand)cmd);
            } else if (cmd instanceof CreateCommand) {
                return execute((CreateCommand)cmd);
            } else if (cmd instanceof DestroyCommand) {
                return execute((DestroyCommand)cmd);
            } else if (cmd instanceof PrimaryStorageDownloadCommand) {
                return execute((PrimaryStorageDownloadCommand)cmd);
            } else if (cmd instanceof CreatePrivateTemplateFromVolumeCommand) {
                return execute((CreatePrivateTemplateFromVolumeCommand)cmd);
            } else if (cmd instanceof GetStorageStatsCommand) {
                return execute((GetStorageStatsCommand)cmd);
            } else if (cmd instanceof ManageSnapshotCommand) {
                return execute((ManageSnapshotCommand)cmd);
            } else if (cmd instanceof BackupSnapshotCommand) {
                return execute((BackupSnapshotCommand)cmd);
            } else if (cmd instanceof CreateVolumeFromSnapshotCommand) {
                return execute((CreateVolumeFromSnapshotCommand)cmd);
            } else if (cmd instanceof CreatePrivateTemplateFromSnapshotCommand) {
                return execute((CreatePrivateTemplateFromSnapshotCommand)cmd);
            } else if (cmd instanceof UpgradeSnapshotCommand) {
                return execute((UpgradeSnapshotCommand)cmd);
            } else if (cmd instanceof CreateStoragePoolCommand) {
                return execute((CreateStoragePoolCommand)cmd);
            } else if (cmd instanceof ModifyStoragePoolCommand) {
                return execute((ModifyStoragePoolCommand)cmd);
            } else if (cmd instanceof SecurityGroupRulesCmd) {
                return execute((SecurityGroupRulesCmd)cmd);
            } else if (cmd instanceof DeleteStoragePoolCommand) {
                return execute((DeleteStoragePoolCommand)cmd);
            } else if (cmd instanceof FenceCommand) {
                return execute((FenceCommand)cmd);
            } else if (cmd instanceof StartCommand) {
                return execute((StartCommand)cmd);
            } else if (cmd instanceof PlugNicCommand) {
                return execute((PlugNicCommand)cmd);
            } else if (cmd instanceof UnPlugNicCommand) {
                return execute((UnPlugNicCommand)cmd);
            } else if (cmd instanceof NetworkElementCommand) {
                return _virtRouterResource.executeRequest((NetworkElementCommand)cmd);
            } else if (cmd instanceof CheckSshCommand) {
                return execute((CheckSshCommand)cmd);
            } else if (cmd instanceof NetworkUsageCommand) {
                return execute((NetworkUsageCommand)cmd);
            } else if (cmd instanceof NetworkRulesSystemVmCommand) {
                return execute((NetworkRulesSystemVmCommand)cmd);
            } else if (cmd instanceof CleanupNetworkRulesCmd) {
                return execute((CleanupNetworkRulesCmd)cmd);
            } else if (cmd instanceof CopyVolumeCommand) {
                return execute((CopyVolumeCommand)cmd);
            } else if (cmd instanceof ResizeVolumeCommand) {
                return execute((ResizeVolumeCommand)cmd);
            } else if (cmd instanceof CheckNetworkCommand) {
                return execute((CheckNetworkCommand)cmd);
            } else if (cmd instanceof NetworkRulesVmSecondaryIpCommand) {
                return execute((NetworkRulesVmSecondaryIpCommand)cmd);
            } else if (cmd instanceof StorageSubSystemCommand) {
                return storageHandler.handleStorageCommands((StorageSubSystemCommand)cmd);
            } else if (cmd instanceof PvlanSetupCommand) {
                return execute((PvlanSetupCommand)cmd);
            } else if (cmd instanceof CheckOnHostCommand) {
                return execute((CheckOnHostCommand)cmd);
            } else if (cmd instanceof OvsFetchInterfaceCommand) {
                return execute((OvsFetchInterfaceCommand)cmd);
            } else if (cmd instanceof OvsSetupBridgeCommand) {
                return execute((OvsSetupBridgeCommand)cmd);
            } else if (cmd instanceof OvsDestroyBridgeCommand) {
                return execute((OvsDestroyBridgeCommand)cmd);
            } else if (cmd instanceof OvsCreateTunnelCommand) {
                return execute((OvsCreateTunnelCommand)cmd);
            } else if (cmd instanceof OvsDestroyTunnelCommand) {
                return execute((OvsDestroyTunnelCommand)cmd);
            } else if (cmd instanceof OvsVpcPhysicalTopologyConfigCommand) {
                return execute((OvsVpcPhysicalTopologyConfigCommand) cmd);
            } else if (cmd instanceof OvsVpcRoutingPolicyConfigCommand) {
                return execute((OvsVpcRoutingPolicyConfigCommand) cmd);
            } else {
                s_logger.warn("Unsupported command ");
                return Answer.createUnsupportedCommandAnswer(cmd);
            }
        } catch (final IllegalArgumentException e) {
            return new Answer(cmd, false, e.getMessage());
        }
    }

    private OvsFetchInterfaceAnswer execute(OvsFetchInterfaceCommand cmd) {
        String label = cmd.getLabel();
        s_logger.debug("Will look for network with name-label:" + label);
        try {
            String ipadd = Script.runSimpleBashScript("ifconfig " + label + " | grep 'inet addr:' | cut -d: -f2 | awk '{ print $1}'");
            String mask = Script.runSimpleBashScript("ifconfig " + label + " | grep 'inet addr:' | cut -d: -f4");
            String mac = Script.runSimpleBashScript("ifconfig " + label + " | grep HWaddr | awk -F \" \" '{print $5}'");
            return new OvsFetchInterfaceAnswer(cmd, true, "Interface " + label
                    + " retrieved successfully", ipadd, mask, mac);

        } catch (Exception e) {
            s_logger.warn("Caught execption when fetching interface", e);
            return new OvsFetchInterfaceAnswer(cmd, false, "EXCEPTION:"
                    + e.getMessage());
        }

    }

    private Answer execute(OvsSetupBridgeCommand cmd) {
        findOrCreateTunnelNetwork(cmd.getBridgeName());
        configureTunnelNetwork(cmd.getNetworkId(), cmd.getHostId(),
                cmd.getBridgeName());
        s_logger.debug("OVS Bridge configured");
        return new Answer(cmd, true, null);
    }

    private Answer execute(OvsDestroyBridgeCommand cmd) {
        destroyTunnelNetwork(cmd.getBridgeName());
        s_logger.debug("OVS Bridge destroyed");
        return new Answer(cmd, true, null);
    }

    public Answer execute(OvsVpcPhysicalTopologyConfigCommand cmd) {

        String bridge = cmd.getBridgeName();
        try {
            Script command = new Script(_ovsTunnelPath, _timeout, s_logger);
            command.add("configure_ovs_bridge_for_network_topology");
            command.add("--bridge", bridge);
            command.add("--config", cmd.getVpcConfigInJson());

            String result = command.execute();
            if (result.equalsIgnoreCase("SUCCESS")) {
                return new Answer(cmd, true, result);
            } else {
                return new Answer(cmd, false, result);
            }
        } catch  (Exception e) {
            s_logger.warn("caught exception while updating host with latest routing polcies", e);
            return new Answer(cmd, false, e.getMessage());
        }
    }

    public Answer execute(OvsVpcRoutingPolicyConfigCommand cmd) {

        try {
            Script command = new Script(_ovsTunnelPath, _timeout, s_logger);
            command.add("configure_ovs_bridge_for_routing_policies");
            command.add("--bridge", cmd.getBridgeName());
            command.add("--config", cmd.getVpcConfigInJson());

            String result = command.execute();
            if (result.equalsIgnoreCase("SUCCESS")) {
                return new Answer(cmd, true, result);
            } else {
                return new Answer(cmd, false, result);
            }
        } catch  (Exception e) {
            s_logger.warn("caught exception while updating host with latest VPC topology", e);
            return new Answer(cmd, false, e.getMessage());
        }
    }

    private synchronized void destroyTunnelNetwork(String bridge) {
        try {
            findOrCreateTunnelNetwork(bridge);
            Script cmd = new Script(_ovsTunnelPath, _timeout, s_logger);
            cmd.add("destroy_ovs_bridge");
            cmd.add("--bridge", bridge);
            String result = cmd.execute();
            if (result != null) {
                // TODO: Should make this error not fatal?
                // Can Concurrent VM shutdown/migration/reboot events can cause
                // this method
                // to be executed on a bridge which has already been removed?
                throw new CloudRuntimeException("Unable to remove OVS bridge " + bridge);
            }
            return;
        } catch (Exception e) {
            s_logger.warn("destroyTunnelNetwork failed:", e);
            return;
        }
    }

    private synchronized boolean findOrCreateTunnelNetwork(String nwName) {
        try {
            if (checkNetwork(nwName)) {
                return true;
            }
            // if not found, create a new one
            Map<String, String> otherConfig = new HashMap<String, String>();
            otherConfig.put("ovs-host-setup", "");
            Script.runSimpleBashScript("ovs-vsctl -- --may-exist add-br "
                    + nwName + " -- set bridge " + nwName
                    + " other_config:ovs-host-setup='-1'");
            s_logger.debug("### KVM network for tunnels created:" + nwName);
        } catch (Exception e) {
            s_logger.warn("createTunnelNetwork failed", e);
        }
        return true;
    }

    private synchronized boolean configureTunnelNetwork(long networkId,
            long hostId, String nwName) {
        try {
            findOrCreateTunnelNetwork(nwName);
            String configuredHosts = Script
                    .runSimpleBashScript("ovs-vsctl get bridge " + nwName
                            + " other_config:ovs-host-setup");
            boolean configured = false;
            if (configuredHosts != null) {
                String hostIdsStr[] = configuredHosts.split(",");
                for (String hostIdStr : hostIdsStr) {
                    if (hostIdStr.equals(((Long)hostId).toString())) {
                        configured = true;
                        break;
                    }
                }
            }
            if (!configured) {
                Script cmd = new Script(_ovsTunnelPath, _timeout, s_logger);
                cmd.add("setup_ovs_bridge");
                cmd.add("--key", nwName);
                cmd.add("--cs_host_id", ((Long)hostId).toString());
                cmd.add("--bridge", nwName);
                String result = cmd.execute();
                if (result != null) {
                    throw new CloudRuntimeException(
                            "Unable to pre-configure OVS bridge " + nwName
                            + " for network ID:" + networkId);
                }
            }
        } catch (Exception e) {
            s_logger.warn("createandConfigureTunnelNetwork failed", e);
            return false;
        }
        return true;
    }

    private OvsCreateTunnelAnswer execute(OvsCreateTunnelCommand cmd) {
        String bridge = cmd.getNetworkName();
        try {
            if (!findOrCreateTunnelNetwork(bridge)) {
                s_logger.debug("Error during bridge setup");
                return new OvsCreateTunnelAnswer(cmd, false,
                        "Cannot create network", bridge);
            }

            configureTunnelNetwork(cmd.getNetworkId(), cmd.getFrom(),
                    cmd.getNetworkName());
            Script command = new Script(_ovsTunnelPath, _timeout, s_logger);
            command.add("create_tunnel");
            command.add("--bridge", bridge);
            command.add("--remote_ip", cmd.getRemoteIp());
            command.add("--key", cmd.getKey().toString());
            command.add("--src_host", cmd.getFrom().toString());
            command.add("--dst_host", cmd.getTo().toString());

            String result = command.execute();
            if (result != null) {
                return new OvsCreateTunnelAnswer(cmd, true, result, null,
                        bridge);
            } else {
                return new OvsCreateTunnelAnswer(cmd, false, result, bridge);
            }
        } catch (Exception e) {
            s_logger.debug("Error during tunnel setup");
            s_logger.warn("Caught execption when creating ovs tunnel", e);
            return new OvsCreateTunnelAnswer(cmd, false, e.getMessage(), bridge);
        }
    }

    private Answer execute(OvsDestroyTunnelCommand cmd) {
        try {
            if (!findOrCreateTunnelNetwork(cmd.getBridgeName())) {
                s_logger.warn("Unable to find tunnel network for GRE key:"
                        + cmd.getBridgeName());
                return new Answer(cmd, false, "No network found");
            }

            Script command = new Script(_ovsTunnelPath, _timeout, s_logger);
            command.add("destroy_tunnel");
            command.add("--bridge", cmd.getBridgeName());
            command.add("--iface_name", cmd.getInPortName());
            String result = command.execute();
            if (result == null) {
                return new Answer(cmd, true, result);
            } else {
                return new Answer(cmd, false, result);
            }
        } catch (Exception e) {
            s_logger.warn("caught execption when destroy ovs tunnel", e);
            return new Answer(cmd, false, e.getMessage());
        }
    }

    private CheckNetworkAnswer execute(CheckNetworkCommand cmd) {
        List<PhysicalNetworkSetupInfo> phyNics = cmd.getPhysicalNetworkInfoList();
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
        /**
             This method is only used for copying files from Primary Storage TO Secondary Storage

             It COULD also do it the other way around, but the code in the ManagementServerImpl shows
             that it always sets copyToSecondary to true

         */
        boolean copyToSecondary = cmd.toSecondaryStorage();
        String volumePath = cmd.getVolumePath();
        StorageFilerTO pool = cmd.getPool();
        String secondaryStorageUrl = cmd.getSecondaryStorageURL();
        KVMStoragePool secondaryStoragePool = null;
        KVMStoragePool primaryPool = null;
        try {
            try {
                primaryPool = _storagePoolMgr.getStoragePool(pool.getType(), pool.getUuid());
            } catch (CloudRuntimeException e) {
                if (e.getMessage().contains("not found")) {
                    primaryPool =
                            _storagePoolMgr.createStoragePool(cmd.getPool().getUuid(), cmd.getPool().getHost(), cmd.getPool().getPort(), cmd.getPool().getPath(),
                                    cmd.getPool().getUserInfo(), cmd.getPool().getType());
                } else {
                    return new CopyVolumeAnswer(cmd, false, e.getMessage(), null, null);
                }
            }

            String volumeName = UUID.randomUUID().toString();

            if (copyToSecondary) {
                String destVolumeName = volumeName + ".qcow2";
                KVMPhysicalDisk volume = primaryPool.getPhysicalDisk(cmd.getVolumePath());
                String volumeDestPath = "/volumes/" + cmd.getVolumeId() + File.separator;
                secondaryStoragePool = _storagePoolMgr.getStoragePoolByURI(secondaryStorageUrl);
                secondaryStoragePool.createFolder(volumeDestPath);
                _storagePoolMgr.deleteStoragePool(secondaryStoragePool.getType(), secondaryStoragePool.getUuid());
                secondaryStoragePool = _storagePoolMgr.getStoragePoolByURI(secondaryStorageUrl + volumeDestPath);
                _storagePoolMgr.copyPhysicalDisk(volume, destVolumeName, secondaryStoragePool, 0);
                return new CopyVolumeAnswer(cmd, true, null, null, volumeName);
            } else {
                volumePath = "/volumes/" + cmd.getVolumeId() + File.separator;
                secondaryStoragePool = _storagePoolMgr.getStoragePoolByURI(secondaryStorageUrl + volumePath);
                KVMPhysicalDisk volume = secondaryStoragePool.getPhysicalDisk(cmd.getVolumePath() + ".qcow2");
                _storagePoolMgr.copyPhysicalDisk(volume, volumeName, primaryPool, 0);
                return new CopyVolumeAnswer(cmd, true, null, null, volumeName);
            }
        } catch (CloudRuntimeException e) {
            return new CopyVolumeAnswer(cmd, false, e.toString(), null, null);
        } finally {
            if (secondaryStoragePool != null) {
                _storagePoolMgr.deleteStoragePool(secondaryStoragePool.getType(), secondaryStoragePool.getUuid());
            }
        }
    }

    protected Answer execute(DeleteStoragePoolCommand cmd) {
        try {
            _storagePoolMgr.deleteStoragePool(cmd.getPool().getType(), cmd.getPool().getUuid());
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

    protected Answer execute(CheckOnHostCommand cmd) {
        ExecutorService executors = Executors.newSingleThreadExecutor();
        List<NfsStoragePool> pools = _monitor.getStoragePools();
        KVMHAChecker ha = new KVMHAChecker(pools, cmd.getHost().getPrivateNetwork().getIp());
        Future<Boolean> future = executors.submit(ha);
        try {
            Boolean result = future.get();
            if (result) {
                return new Answer(cmd, false, "Heart is still beating...");
            } else {
                return new Answer(cmd);
            }
        } catch (InterruptedException e) {
            return new Answer(cmd, false, "can't get status of host:");
        } catch (ExecutionException e) {
            return new Answer(cmd, false, "can't get status of host:");
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
            primaryPool = _storagePoolMgr.getStoragePool(pool.getType(), pool.getUuid());
            disksize = dskch.getSize();

            if (cmd.getTemplateUrl() != null) {
                if (primaryPool.getType() == StoragePoolType.CLVM) {
                    vol = templateToPrimaryDownload(cmd.getTemplateUrl(), primaryPool, dskch.getPath());
                } else {
                    BaseVol = primaryPool.getPhysicalDisk(cmd.getTemplateUrl());
                    vol = _storagePoolMgr.createDiskFromTemplate(BaseVol,
                            dskch.getPath(), dskch.getProvisioningType(), primaryPool, 0);
                }
                if (vol == null) {
                    return new Answer(cmd, false, " Can't create storage volume on storage pool");
                }
            } else {
                vol = primaryPool.createPhysicalDisk(dskch.getPath(), dskch.getProvisioningType(), dskch.getSize());
            }
            VolumeTO volume =
                    new VolumeTO(cmd.getVolumeId(), dskch.getType(), pool.getType(), pool.getUuid(), pool.getPath(), vol.getName(), vol.getName(), disksize, null);
            volume.setBytesReadRate(dskch.getBytesReadRate());
            volume.setBytesWriteRate(dskch.getBytesWriteRate());
            volume.setIopsReadRate(dskch.getIopsReadRate());
            volume.setIopsWriteRate(dskch.getIopsWriteRate());
            volume.setCacheMode(dskch.getCacheMode());
            return new CreateAnswer(cmd, volume);
        } catch (CloudRuntimeException e) {
            s_logger.debug("Failed to create volume: " + e.toString());
            return new CreateAnswer(cmd, e);
        }
    }

    // this is much like PrimaryStorageDownloadCommand, but keeping it separate
    protected KVMPhysicalDisk templateToPrimaryDownload(String templateUrl, KVMStoragePool primaryPool, String volUuid) {
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

            KVMPhysicalDisk primaryVol = _storagePoolMgr.copyPhysicalDisk(templateVol, volUuid, primaryPool, 0);
            return primaryVol;
        } catch (CloudRuntimeException e) {
            s_logger.error("Failed to download template to primary storage", e);
            return null;
        } finally {
            if (secondaryPool != null) {
                _storagePoolMgr.deleteStoragePool(secondaryPool.getType(), secondaryPool.getUuid());
            }
        }
    }

    private String getResizeScriptType(KVMStoragePool pool, KVMPhysicalDisk vol) {
        StoragePoolType poolType = pool.getType();
        PhysicalDiskFormat volFormat = vol.getFormat();

        if (pool.getType() == StoragePoolType.CLVM && volFormat == PhysicalDiskFormat.RAW) {
            return "CLVM";
        } else if ((poolType == StoragePoolType.NetworkFilesystem
                || poolType == StoragePoolType.SharedMountPoint
                || poolType == StoragePoolType.Filesystem
                || poolType == StoragePoolType.Gluster)
                && volFormat == PhysicalDiskFormat.QCOW2 ) {
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

        if ( currentSize == newSize) {
            // nothing to do
            s_logger.info("No need to resize volume: current size " + currentSize + " is same as new size " + newSize);
            return new ResizeVolumeAnswer(cmd, true, "success", currentSize);
        }

        try {
            KVMStoragePool pool = _storagePoolMgr.getStoragePool(spool.getType(), spool.getUuid());
            KVMPhysicalDisk vol = pool.getPhysicalDisk(volid);
            String path = vol.getPath();
            String type = getResizeScriptType(pool, vol);

            if (pool.getType() != StoragePoolType.RBD) {
                if (type == null) {
                    return new ResizeVolumeAnswer(cmd, false, "Unsupported volume format: pool type '" + pool.getType() + "' and volume format '" + vol.getFormat() + "'");
                } else if (type.equals("QCOW2") && shrinkOk) {
                    return new ResizeVolumeAnswer(cmd, false, "Unable to shrink volumes of type " + type);
                }
            } else {
                s_logger.debug("Volume " + path + " is on a RBD storage pool. No need to query for additional information.");
            }

            s_logger.debug("Resizing volume: " + path + "," + currentSize + "," + newSize + "," + type + "," + vmInstanceName + "," + shrinkOk);

            /* libvirt doesn't support resizing (C)LVM devices, so we have to do that via a Bash script */
            if (pool.getType() != StoragePoolType.CLVM) {
                s_logger.debug("Volume " + path +  " can be resized by libvirt. Asking libvirt to resize the volume.");
                try {
                    Connect conn = LibvirtConnection.getConnection();
                    StorageVol v = conn.storageVolLookupByPath(path);
                    int flags = 0;

                    if (conn.getLibVirVersion() > 1001000 && vol.getFormat() == PhysicalDiskFormat.RAW && pool.getType() != StoragePoolType.RBD) {
                        flags = 1;
                    }
                    if (shrinkOk) {
                        flags = 4;
                    }

                    v.resize(newSize, flags);
                } catch (LibvirtException e) {
                    return new ResizeVolumeAnswer(cmd, false, e.toString());
                }
            } else {
                s_logger.debug("Volume " + path + " is of the type LVM and can not be resized using libvirt. Invoking resize script.");
                final Script resizecmd = new Script(_resizeVolumePath, _cmdsTimeout, s_logger);
                resizecmd.add("-s", String.valueOf(newSize));
                resizecmd.add("-c", String.valueOf(currentSize));
                resizecmd.add("-p", path);
                resizecmd.add("-t", type);
                resizecmd.add("-r", String.valueOf(shrinkOk));
                resizecmd.add("-v", vmInstanceName);
                String result = resizecmd.execute();

                if (result != null) {
                    return new ResizeVolumeAnswer(cmd, false, result);
                }
            }

            /* fetch new size as seen from libvirt, don't want to assume anything */
            pool = _storagePoolMgr.getStoragePool(spool.getType(), spool.getUuid());
            long finalSize = pool.getPhysicalDisk(volid).getVirtualSize();
            s_logger.debug("after resize, size reports as " + finalSize + ", requested " + newSize);
            return new ResizeVolumeAnswer(cmd, true, "success", finalSize);
        } catch (CloudRuntimeException e) {
            String error = "Failed to resize volume: " + e.getMessage();
            s_logger.debug(error);
            return new ResizeVolumeAnswer(cmd, false, error);
        }

    }

    public Answer execute(DestroyCommand cmd) {
        VolumeTO vol = cmd.getVolume();
        try {
            KVMStoragePool pool = _storagePoolMgr.getStoragePool(vol.getPoolType(), vol.getPoolUuid());
            pool.deletePhysicalDisk(vol.getPath(), null);
            return new Answer(cmd, true, "Success");
        } catch (CloudRuntimeException e) {
            s_logger.debug("Failed to delete volume: " + e.toString());
            return new Answer(cmd, false, e.toString());
        }
    }

    private String getBroadcastUriFromBridge(String brName) {
        String pif = matchPifFileInDirectory(brName);
        Pattern pattern = Pattern.compile("(\\D+)(\\d+)(\\D*)(\\d*)");
        Matcher matcher = pattern.matcher(pif);
        s_logger.debug("getting broadcast uri for pif " + pif + " and bridge " + brName);
        if(matcher.find()) {
            if (brName.startsWith("brvx")){
                return BroadcastDomainType.Vxlan.toUri(matcher.group(2)).toString();
            }
            else{
                if (!matcher.group(4).isEmpty()) {
                    return BroadcastDomainType.Vlan.toUri(matcher.group(4)).toString();
                } else {
                    //untagged or not matching (eth|bond)#.#
                    s_logger.debug("failed to get vNet id from bridge " + brName
                            + "attached to physical interface" + pif + ", perhaps untagged interface");
                    return "";
                }
            }
        } else {
            s_logger.debug("failed to get vNet id from bridge " + brName + "attached to physical interface" + pif);
            return "";
        }
    }

    private Answer execute(PvlanSetupCommand cmd) {
        String primaryPvlan = cmd.getPrimary();
        String isolatedPvlan = cmd.getIsolated();
        String op = cmd.getOp();
        String dhcpName = cmd.getDhcpName();
        String dhcpMac = cmd.getDhcpMac();
        String dhcpIp = cmd.getDhcpIp();
        String vmMac = cmd.getVmMac();
        boolean add = true;

        String opr = "-A";
        if (op.equals("delete")) {
            opr = "-D";
            add = false;
        }

        String result = null;
        Connect conn;
        try {
            if (cmd.getType() == PvlanSetupCommand.Type.DHCP) {
                Script script = new Script(_ovsPvlanDhcpHostPath, _timeout, s_logger);
                if (add) {
                    conn = LibvirtConnection.getConnectionByVmName(dhcpName);
                    List<InterfaceDef> ifaces = getInterfaces(conn, dhcpName);
                    InterfaceDef guestNic = ifaces.get(0);
                    script.add(opr, "-b", _guestBridgeName, "-p", primaryPvlan, "-i", isolatedPvlan, "-n", dhcpName, "-d", dhcpIp, "-m", dhcpMac, "-I",
                            guestNic.getDevName());
                } else {
                    script.add(opr, "-b", _guestBridgeName, "-p", primaryPvlan, "-i", isolatedPvlan, "-n", dhcpName, "-d", dhcpIp, "-m", dhcpMac);
                }
                result = script.execute();
                if (result != null) {
                    s_logger.warn("Failed to program pvlan for dhcp server with mac " + dhcpMac);
                    return new Answer(cmd, false, result);
                } else {
                    s_logger.info("Programmed pvlan for dhcp server with mac " + dhcpMac);
                }
            } else if (cmd.getType() == PvlanSetupCommand.Type.VM) {
                Script script = new Script(_ovsPvlanVmPath, _timeout, s_logger);
                script.add(opr, "-b", _guestBridgeName, "-p", primaryPvlan, "-i", isolatedPvlan, "-v", vmMac);
                result = script.execute();
                if (result != null) {
                    s_logger.warn("Failed to program pvlan for vm with mac " + vmMac);
                    return new Answer(cmd, false, result);
                } else {
                    s_logger.info("Programmed pvlan for vm with mac " + vmMac);
                }
            }
        } catch (LibvirtException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return new Answer(cmd, true, result);
    }

    private void VifHotPlug(Connect conn, String vmName, String broadcastUri, String macAddr) throws InternalErrorException, LibvirtException {
        NicTO nicTO = new NicTO();
        nicTO.setMac(macAddr);
        nicTO.setType(TrafficType.Public);
        if (broadcastUri == null) {
            nicTO.setBroadcastType(BroadcastDomainType.Native);
        } else {
            URI uri = BroadcastDomainType.fromString(broadcastUri);
            nicTO.setBroadcastType(BroadcastDomainType.getSchemeValue(uri));
            nicTO.setBroadcastUri(uri);
        }

        Domain vm = getDomain(conn, vmName);
        vm.attachDevice(getVifDriver(nicTO.getType()).plug(nicTO, "Other PV", "").toString());
    }


    private void vifHotUnPlug (Connect conn, String vmName, String macAddr) throws InternalErrorException, LibvirtException {

        Domain vm = null;
        vm = getDomain(conn, vmName);
        List<InterfaceDef> pluggedNics = getInterfaces(conn, vmName);
        for (InterfaceDef pluggedNic : pluggedNics) {
            if (pluggedNic.getMacAddress().equalsIgnoreCase(macAddr)) {
                vm.detachDevice(pluggedNic.toString());
                // We don't know which "traffic type" is associated with
                // each interface at this point, so inform all vif drivers
                for (VifDriver vifDriver : getAllVifDrivers()) {
                    vifDriver.unplug(pluggedNic);
                }
            }
        }
    }

    private PlugNicAnswer execute(PlugNicCommand cmd) {
        NicTO nic = cmd.getNic();
        String vmName = cmd.getVmName();
        Domain vm = null;
        try {
            Connect conn = LibvirtConnection.getConnectionByVmName(vmName);
            vm = getDomain(conn, vmName);
            List<InterfaceDef> pluggedNics = getInterfaces(conn, vmName);
            Integer nicnum = 0;
            for (InterfaceDef pluggedNic : pluggedNics) {
                if (pluggedNic.getMacAddress().equalsIgnoreCase(nic.getMac())) {
                    s_logger.debug("found existing nic for mac " + pluggedNic.getMacAddress() + " at index " + nicnum);
                    return new PlugNicAnswer(cmd, true, "success");
                }
                nicnum++;
            }
            vm.attachDevice(getVifDriver(nic.getType()).plug(nic, "Other PV", "").toString());
            return new PlugNicAnswer(cmd, true, "success");
        } catch (LibvirtException e) {
            String msg = " Plug Nic failed due to " + e.toString();
            s_logger.warn(msg, e);
            return new PlugNicAnswer(cmd, false, msg);
        } catch (InternalErrorException e) {
            String msg = " Plug Nic failed due to " + e.toString();
            s_logger.warn(msg, e);
            return new PlugNicAnswer(cmd, false, msg);
        } finally {
            if (vm != null) {
                try {
                    vm.free();
                } catch (LibvirtException l) {
                    s_logger.trace("Ignoring libvirt error.", l);
                }
            }
        }
    }

    private UnPlugNicAnswer execute(UnPlugNicCommand cmd) {
        Connect conn;
        NicTO nic = cmd.getNic();
        String vmName = cmd.getVmName();
        Domain vm = null;
        try {
            conn = LibvirtConnection.getConnectionByVmName(vmName);
            vm = getDomain(conn, vmName);
            List<InterfaceDef> pluggedNics = getInterfaces(conn, vmName);
            for (InterfaceDef pluggedNic : pluggedNics) {
                if (pluggedNic.getMacAddress().equalsIgnoreCase(nic.getMac())) {
                    vm.detachDevice(pluggedNic.toString());
                    // We don't know which "traffic type" is associated with
                    // each interface at this point, so inform all vif drivers
                    for (VifDriver vifDriver : getAllVifDrivers()) {
                        vifDriver.unplug(pluggedNic);
                    }
                    return new UnPlugNicAnswer(cmd, true, "success");
                }
            }
            return new UnPlugNicAnswer(cmd, true, "success");
        } catch (LibvirtException e) {
            String msg = " Unplug Nic failed due to " + e.toString();
            s_logger.warn(msg, e);
            return new UnPlugNicAnswer(cmd, false, msg);
        } finally {
            if (vm != null) {
                try {
                    vm.free();
                } catch (LibvirtException l) {
                    s_logger.trace("Ignoring libvirt error.", l);
                }
            }
        }
    }

    private ExecutionResult prepareNetworkElementCommand(SetupGuestNetworkCommand cmd) {
        Connect conn;
        NicTO nic = cmd.getNic();
        String routerName = cmd.getAccessDetail(NetworkElementCommand.ROUTER_NAME);

        try {
            conn = LibvirtConnection.getConnectionByVmName(routerName);
            List<InterfaceDef> pluggedNics = getInterfaces(conn, routerName);
            InterfaceDef routerNic = null;

            for (InterfaceDef pluggedNic : pluggedNics) {
                if (pluggedNic.getMacAddress().equalsIgnoreCase(nic.getMac())) {
                    routerNic = pluggedNic;
                    break;
                }
            }

            if (routerNic == null) {
                return new ExecutionResult(false, "Can not find nic with mac " + nic.getMac() + " for VM " + routerName);
            }

            return new ExecutionResult(true, null);
        } catch (LibvirtException e) {
            String msg = "Creating guest network failed due to " + e.toString();
            s_logger.warn(msg, e);
            return new ExecutionResult(false, msg);
        }
    }

    protected ExecutionResult prepareNetworkElementCommand(SetSourceNatCommand cmd) {
        Connect conn;
        String routerName = cmd.getAccessDetail(NetworkElementCommand.ROUTER_NAME);
        cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);
        IpAddressTO pubIP = cmd.getIpAddress();

        try {
            conn = LibvirtConnection.getConnectionByVmName(routerName);
            Integer devNum = 0;
            String pubVlan = pubIP.getBroadcastUri();
            List<InterfaceDef> pluggedNics = getInterfaces(conn, routerName);

            for (InterfaceDef pluggedNic : pluggedNics) {
                String pluggedVlanBr = pluggedNic.getBrName();
                String pluggedVlanId = getBroadcastUriFromBridge(pluggedVlanBr);
                if (pubVlan.equalsIgnoreCase(Vlan.UNTAGGED) && pluggedVlanBr.equalsIgnoreCase(_publicBridgeName)) {
                    break;
                } else if (pluggedVlanBr.equalsIgnoreCase(_linkLocalBridgeName)) {
                    /*skip over, no physical bridge device exists*/
                } else if (pluggedVlanId == null) {
                    /*this should only be true in the case of link local bridge*/
                    return new ExecutionResult(false, "unable to find the vlan id for bridge " + pluggedVlanBr + " when attempting to set up" + pubVlan +
                            " on router " + routerName);
                } else if (pluggedVlanId.equals(pubVlan)) {
                    break;
                }
                devNum++;
            }

            pubIP.setNicDevId(devNum);

            return new ExecutionResult(true, "success");
        } catch (LibvirtException e) {
            String msg = "Ip SNAT failure due to " + e.toString();
            s_logger.error(msg, e);
            return new ExecutionResult(false, msg);
        }
    }

    protected ExecutionResult prepareNetworkElementCommand(IpAssocVpcCommand cmd) {
        Connect conn;
        String routerName = cmd.getAccessDetail(NetworkElementCommand.ROUTER_NAME);

        try {
            conn = LibvirtConnection.getConnectionByVmName(routerName);
            IpAddressTO[] ips = cmd.getIpAddresses();
            Integer devNum = 0;
            Map<String, Integer> broadcastUriToNicNum = new HashMap<String, Integer>();
            List<InterfaceDef> pluggedNics = getInterfaces(conn, routerName);

            for (InterfaceDef pluggedNic : pluggedNics) {
                String pluggedVlan = pluggedNic.getBrName();
                if (pluggedVlan.equalsIgnoreCase(_linkLocalBridgeName)) {
                    broadcastUriToNicNum.put("LinkLocal", devNum);
                } else if (pluggedVlan.equalsIgnoreCase(_publicBridgeName) || pluggedVlan.equalsIgnoreCase(_privBridgeName) ||
                        pluggedVlan.equalsIgnoreCase(_guestBridgeName)) {
                    broadcastUriToNicNum.put(BroadcastDomainType.Vlan.toUri(Vlan.UNTAGGED).toString(), devNum);
                } else {
                    broadcastUriToNicNum.put(getBroadcastUriFromBridge(pluggedVlan), devNum);
                }
                devNum++;
            }

            for (IpAddressTO ip : ips) {
                ip.setNicDevId(broadcastUriToNicNum.get(ip.getBroadcastUri()));
            }

            return new ExecutionResult(true, null);
        } catch (LibvirtException e) {
            s_logger.error("Ip Assoc failure on applying one ip due to exception:  ", e);
            return new ExecutionResult(false, e.getMessage());
        }
    }

    public ExecutionResult prepareNetworkElementCommand(IpAssocCommand cmd) {
        String routerName = cmd.getAccessDetail(NetworkElementCommand.ROUTER_NAME);
        String routerIp = cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);
        Connect conn;
        try {
            conn = LibvirtConnection.getConnectionByVmName(routerName);
            List<InterfaceDef> nics = getInterfaces(conn, routerName);
            Map<String, Integer> broadcastUriAllocatedToVM = new HashMap<String, Integer>();
            Integer nicPos = 0;
            for (InterfaceDef nic : nics) {
                if (nic.getBrName().equalsIgnoreCase(_linkLocalBridgeName)) {
                    broadcastUriAllocatedToVM.put("LinkLocal", nicPos);
                } else {
                    if (nic.getBrName().equalsIgnoreCase(_publicBridgeName) || nic.getBrName().equalsIgnoreCase(_privBridgeName) ||
                            nic.getBrName().equalsIgnoreCase(_guestBridgeName)) {
                        broadcastUriAllocatedToVM.put(BroadcastDomainType.Vlan.toUri(Vlan.UNTAGGED).toString(), nicPos);
                    } else {
                        String broadcastUri = getBroadcastUriFromBridge(nic.getBrName());
                        broadcastUriAllocatedToVM.put(broadcastUri, nicPos);
                    }
                }
                nicPos++;
            }
            IpAddressTO[] ips = cmd.getIpAddresses();
            int nicNum = 0;
            for (IpAddressTO ip : ips) {
                boolean newNic = false;
                if (!broadcastUriAllocatedToVM.containsKey(ip.getBroadcastUri())) {
                    /* plug a vif into router */
                    VifHotPlug(conn, routerName, ip.getBroadcastUri(), ip.getVifMacAddress());
                    broadcastUriAllocatedToVM.put(ip.getBroadcastUri(), nicPos++);
                    newNic = true;
                }
                nicNum = broadcastUriAllocatedToVM.get(ip.getBroadcastUri());
                networkUsage(routerIp, "addVif", "eth" + nicNum);

                ip.setNicDevId(nicNum);
                ip.setNewNic(newNic);
            }
            return new ExecutionResult(true, null);
        } catch (LibvirtException e) {
            s_logger.error("ipassoccmd failed", e);
            return new ExecutionResult(false, e.getMessage());
        } catch (InternalErrorException e) {
            s_logger.error("ipassoccmd failed", e);
            return new ExecutionResult(false, e.getMessage());
        }
    }

    protected ExecutionResult cleanupNetworkElementCommand(IpAssocCommand cmd) {

        String routerName = cmd.getAccessDetail(NetworkElementCommand.ROUTER_NAME);
        String routerIp = cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);
        Connect conn;


        try{
            conn = LibvirtConnection.getConnectionByVmName(routerName);
            List<InterfaceDef> nics = getInterfaces(conn, routerName);
            Map<String, Integer> broadcastUriAllocatedToVM = new HashMap<String, Integer>();

            Integer nicPos = 0;
            for (InterfaceDef nic : nics) {
                if (nic.getBrName().equalsIgnoreCase(_linkLocalBridgeName)) {
                    broadcastUriAllocatedToVM.put("LinkLocal", nicPos);
                } else {
                    if (nic.getBrName().equalsIgnoreCase(_publicBridgeName) || nic.getBrName().equalsIgnoreCase(_privBridgeName) ||
                            nic.getBrName().equalsIgnoreCase(_guestBridgeName)) {
                        broadcastUriAllocatedToVM.put(BroadcastDomainType.Vlan.toUri(Vlan.UNTAGGED).toString(), nicPos);
                    } else {
                        String broadcastUri = getBroadcastUriFromBridge(nic.getBrName());
                        broadcastUriAllocatedToVM.put(broadcastUri, nicPos);
                    }
                }
                nicPos++;
            }

            IpAddressTO[] ips = cmd.getIpAddresses();
            int numOfIps = ips.length;
            int nicNum = 0;
            for (IpAddressTO ip : ips) {

                if (!broadcastUriAllocatedToVM.containsKey(ip.getBroadcastUri())) {
                    /* plug a vif into router */
                    VifHotPlug(conn, routerName, ip.getBroadcastUri(), ip.getVifMacAddress());
                    broadcastUriAllocatedToVM.put(ip.getBroadcastUri(), nicPos++);
                }
                nicNum = broadcastUriAllocatedToVM.get(ip.getBroadcastUri());

                if (numOfIps == 1 && !ip.isAdd()) {
                    vifHotUnPlug(conn, routerName, ip.getVifMacAddress());
                    networkUsage(routerIp, "deleteVif", "eth" + nicNum);
                }
            }

        } catch (LibvirtException e) {
            s_logger.error("ipassoccmd failed", e);
            return new ExecutionResult(false, e.getMessage());
        } catch (InternalErrorException e) {
            s_logger.error("ipassoccmd failed", e);
            return new ExecutionResult(false, e.getMessage());
        }

        return new ExecutionResult(true, null);
    }

    protected ManageSnapshotAnswer execute(final ManageSnapshotCommand cmd) {
        String snapshotName = cmd.getSnapshotName();
        String snapshotPath = cmd.getSnapshotPath();
        String vmName = cmd.getVmName();
        try {
            Connect conn = LibvirtConnection.getConnectionByVmName(vmName);
            DomainState state = null;
            Domain vm = null;
            if (vmName != null) {
                try {
                    vm = getDomain(conn, cmd.getVmName());
                    state = vm.getInfo().state;
                } catch (LibvirtException e) {
                    s_logger.trace("Ignoring libvirt error.", e);
                }
            }

            KVMStoragePool primaryPool = _storagePoolMgr.getStoragePool(cmd.getPool().getType(), cmd.getPool().getUuid());

            KVMPhysicalDisk disk = primaryPool.getPhysicalDisk(cmd.getVolumePath());
            if (state == DomainState.VIR_DOMAIN_RUNNING && !primaryPool.isExternalSnapshot()) {
                String vmUuid = vm.getUUIDString();
                Object[] args = new Object[] {snapshotName, vmUuid};
                String snapshot = SnapshotXML.format(args);
                s_logger.debug(snapshot);
                if (cmd.getCommandSwitch().equalsIgnoreCase(ManageSnapshotCommand.CREATE_SNAPSHOT)) {
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
                if (state == DomainState.VIR_DOMAIN_PAUSED) {
                    vm.resume();
                }
            } else {
                /**
                 * For RBD we can't use libvirt to do our snapshotting or any Bash scripts.
                 * libvirt also wants to store the memory contents of the Virtual Machine,
                 * but that's not possible with RBD since there is no way to store the memory
                 * contents in RBD.
                 *
                 * So we rely on the Java bindings for RBD to create our snapshot
                 *
                 * This snapshot might not be 100% consistent due to writes still being in the
                 * memory of the Virtual Machine, but if the VM runs a kernel which supports
                 * barriers properly (>2.6.32) this won't be any different then pulling the power
                 * cord out of a running machine.
                 */
                if (primaryPool.getType() == StoragePoolType.RBD) {
                    try {
                        Rados r = new Rados(primaryPool.getAuthUserName());
                        r.confSet("mon_host", primaryPool.getSourceHost() + ":" + primaryPool.getSourcePort());
                        r.confSet("key", primaryPool.getAuthSecret());
                        r.confSet("client_mount_timeout", "30");
                        r.connect();
                        s_logger.debug("Succesfully connected to Ceph cluster at " + r.confGet("mon_host"));

                        IoCTX io = r.ioCtxCreate(primaryPool.getSourceDir());
                        Rbd rbd = new Rbd(io);
                        RbdImage image = rbd.open(disk.getName());

                        if (cmd.getCommandSwitch().equalsIgnoreCase(ManageSnapshotCommand.CREATE_SNAPSHOT)) {
                            s_logger.debug("Attempting to create RBD snapshot " + disk.getName() + "@" + snapshotName);
                            image.snapCreate(snapshotName);
                        } else {
                            s_logger.debug("Attempting to remove RBD snapshot " + disk.getName() + "@" + snapshotName);
                            image.snapRemove(snapshotName);
                        }

                        rbd.close(image);
                        r.ioCtxDestroy(io);
                    } catch (Exception e) {
                        s_logger.error("A RBD snapshot operation on " + disk.getName() + " failed. The error was: " + e.getMessage());
                    }
                } else {
                    /* VM is not running, create a snapshot by ourself */
                    final Script command = new Script(_manageSnapshotPath, _cmdsTimeout, s_logger);
                    if (cmd.getCommandSwitch().equalsIgnoreCase(ManageSnapshotCommand.CREATE_SNAPSHOT)) {
                        command.add("-c", disk.getPath());
                    } else {
                        command.add("-d", snapshotPath);
                    }

                    command.add("-n", snapshotName);
                    String result = command.execute();
                    if (result != null) {
                        s_logger.debug("Failed to manage snapshot: " + result);
                        return new ManageSnapshotAnswer(cmd, false, "Failed to manage snapshot: " + result);
                    }
                }
            }
            return new ManageSnapshotAnswer(cmd, cmd.getSnapshotId(), disk.getPath() + File.separator + snapshotName, true, null);
        } catch (LibvirtException e) {
            s_logger.debug("Failed to manage snapshot: " + e.toString());
            return new ManageSnapshotAnswer(cmd, false, "Failed to manage snapshot: " + e.toString());
        }

    }

    protected BackupSnapshotAnswer execute(final BackupSnapshotCommand cmd) {
        Long dcId = cmd.getDataCenterId();
        Long accountId = cmd.getAccountId();
        Long volumeId = cmd.getVolumeId();
        String secondaryStoragePoolUrl = cmd.getSecondaryStorageUrl();
        String snapshotName = cmd.getSnapshotName();
        String snapshotDestPath = null;
        String snapshotRelPath = null;
        String vmName = cmd.getVmName();
        KVMStoragePool secondaryStoragePool = null;
        try {
            Connect conn = LibvirtConnection.getConnectionByVmName(vmName);

            secondaryStoragePool = _storagePoolMgr.getStoragePoolByURI(secondaryStoragePoolUrl);

            String ssPmountPath = secondaryStoragePool.getLocalPath();
            snapshotRelPath = File.separator + "snapshots" + File.separator + dcId + File.separator + accountId + File.separator + volumeId;

            snapshotDestPath = ssPmountPath + File.separator + "snapshots" + File.separator + dcId + File.separator + accountId + File.separator + volumeId;
            KVMStoragePool primaryPool = _storagePoolMgr.getStoragePool(cmd.getPool().getType(), cmd.getPrimaryStoragePoolNameLabel());
            KVMPhysicalDisk snapshotDisk = primaryPool.getPhysicalDisk(cmd.getVolumePath());

            /**
             * RBD snapshots can't be copied using qemu-img, so we have to use
             * the Java bindings for librbd here.
             *
             * These bindings will read the snapshot and write the contents to
             * the secondary storage directly
             *
             * It will stop doing so if the amount of time spend is longer then
             * cmds.timeout
             */
            if (primaryPool.getType() == StoragePoolType.RBD) {
                try {
                    Rados r = new Rados(primaryPool.getAuthUserName());
                    r.confSet("mon_host", primaryPool.getSourceHost() + ":" + primaryPool.getSourcePort());
                    r.confSet("key", primaryPool.getAuthSecret());
                    r.confSet("client_mount_timeout", "30");
                    r.connect();
                    s_logger.debug("Succesfully connected to Ceph cluster at " + r.confGet("mon_host"));

                    IoCTX io = r.ioCtxCreate(primaryPool.getSourceDir());
                    Rbd rbd = new Rbd(io);
                    RbdImage image = rbd.open(snapshotDisk.getName(), snapshotName);
                    File fh = new File(snapshotDestPath);
                    try(BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(fh));) {
                        int chunkSize = 4194304;
                        long offset = 0;
                        s_logger.debug("Backuping up RBD snapshot " + snapshotName + " to  " + snapshotDestPath);
                        while (true) {
                            byte[] buf = new byte[chunkSize];
                            int bytes = image.read(offset, buf, chunkSize);
                            if (bytes <= 0) {
                                break;
                            }
                            bos.write(buf, 0, bytes);
                            offset += bytes;
                        }
                        s_logger.debug("Completed backing up RBD snapshot " + snapshotName + " to  " + snapshotDestPath + ". Bytes written: " + offset);
                    }catch(IOException ex)
                    {
                        s_logger.error("BackupSnapshotAnswer:Exception:"+ ex.getMessage());
                    }
                    r.ioCtxDestroy(io);
                } catch (RadosException e) {
                    s_logger.error("A RADOS operation failed. The error was: " + e.getMessage());
                    return new BackupSnapshotAnswer(cmd, false, e.toString(), null, true);
                } catch (RbdException e) {
                    s_logger.error("A RBD operation on " + snapshotDisk.getName() + " failed. The error was: " + e.getMessage());
                    return new BackupSnapshotAnswer(cmd, false, e.toString(), null, true);
                }
            } else {
                Script command = new Script(_manageSnapshotPath, _cmdsTimeout, s_logger);
                command.add("-b", snapshotDisk.getPath());
                command.add("-n", snapshotName);
                command.add("-p", snapshotDestPath);
                command.add("-t", snapshotName);
                String result = command.execute();
                if (result != null) {
                    s_logger.debug("Failed to backup snaptshot: " + result);
                    return new BackupSnapshotAnswer(cmd, false, result, null, true);
                }
            }
            /* Delete the snapshot on primary */

            DomainState state = null;
            Domain vm = null;
            if (vmName != null) {
                try {
                    vm = getDomain(conn, cmd.getVmName());
                    state = vm.getInfo().state;
                } catch (LibvirtException e) {
                    s_logger.trace("Ignoring libvirt error.", e);
                }
            }

            KVMStoragePool primaryStorage = _storagePoolMgr.getStoragePool(cmd.getPool().getType(), cmd.getPool().getUuid());
            if (state == DomainState.VIR_DOMAIN_RUNNING && !primaryStorage.isExternalSnapshot()) {
                String vmUuid = vm.getUUIDString();
                Object[] args = new Object[] {snapshotName, vmUuid};
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
                if (state == DomainState.VIR_DOMAIN_PAUSED) {
                    vm.resume();
                }
            } else {
                Script command = new Script(_manageSnapshotPath, _cmdsTimeout, s_logger);
                command.add("-d", snapshotDisk.getPath());
                command.add("-n", snapshotName);
                String result = command.execute();
                if (result != null) {
                    s_logger.debug("Failed to backup snapshot: " + result);
                    return new BackupSnapshotAnswer(cmd, false, "Failed to backup snapshot: " + result, null, true);
                }
            }
        } catch (LibvirtException e) {
            return new BackupSnapshotAnswer(cmd, false, e.toString(), null, true);
        } catch (CloudRuntimeException e) {
            return new BackupSnapshotAnswer(cmd, false, e.toString(), null, true);
        } finally {
            if (secondaryStoragePool != null) {
                _storagePoolMgr.deleteStoragePool(secondaryStoragePool.getType(), secondaryStoragePool.getUuid());
            }
        }
        return new BackupSnapshotAnswer(cmd, true, null, snapshotRelPath + File.separator + snapshotName, true);
    }

    protected CreateVolumeFromSnapshotAnswer execute(final CreateVolumeFromSnapshotCommand cmd) {
        try {

            String snapshotPath = cmd.getSnapshotUuid();
            int index = snapshotPath.lastIndexOf("/");
            snapshotPath = snapshotPath.substring(0, index);
            KVMStoragePool secondaryPool = _storagePoolMgr.getStoragePoolByURI(cmd.getSecondaryStorageUrl() + snapshotPath);
            KVMPhysicalDisk snapshot = secondaryPool.getPhysicalDisk(cmd.getSnapshotName());

            String primaryUuid = cmd.getPrimaryStoragePoolNameLabel();
            KVMStoragePool primaryPool = _storagePoolMgr.getStoragePool(cmd.getPool().getType(), primaryUuid);
            String volUuid = UUID.randomUUID().toString();
            KVMPhysicalDisk disk = _storagePoolMgr.copyPhysicalDisk(snapshot, volUuid, primaryPool, 0);
            return new CreateVolumeFromSnapshotAnswer(cmd, true, "", disk.getName());
        } catch (CloudRuntimeException e) {
            return new CreateVolumeFromSnapshotAnswer(cmd, false, e.toString(), null);
        }
    }

    protected Answer execute(final UpgradeSnapshotCommand cmd) {

        return new Answer(cmd, true, "success");
    }

    protected CreatePrivateTemplateAnswer execute(final CreatePrivateTemplateFromSnapshotCommand cmd) {
        String templateFolder = cmd.getAccountId() + File.separator + cmd.getNewTemplateId();
        String templateInstallFolder = "template/tmpl/" + templateFolder;
        String tmplName = UUID.randomUUID().toString();
        String tmplFileName = tmplName + ".qcow2";
        KVMStoragePool secondaryPool = null;
        KVMStoragePool snapshotPool = null;
        try {
            String snapshotPath = cmd.getSnapshotUuid();
            int index = snapshotPath.lastIndexOf("/");
            snapshotPath = snapshotPath.substring(0, index);
            snapshotPool = _storagePoolMgr.getStoragePoolByURI(cmd.getSecondaryStorageUrl() + snapshotPath);
            KVMPhysicalDisk snapshot = snapshotPool.getPhysicalDisk(cmd.getSnapshotName());

            secondaryPool = _storagePoolMgr.getStoragePoolByURI(cmd.getSecondaryStorageUrl());

            String templatePath = secondaryPool.getLocalPath() + File.separator + templateInstallFolder;

            _storage.mkdirs(templatePath);

            String tmplPath = templateInstallFolder + File.separator + tmplFileName;
            Script command = new Script(_createTmplPath, _cmdsTimeout, s_logger);
            command.add("-t", templatePath);
            command.add("-n", tmplFileName);
            command.add("-f", snapshot.getPath());
            command.execute();

            Map<String, Object> params = new HashMap<String, Object>();
            params.put(StorageLayer.InstanceConfigKey, _storage);
            Processor qcow2Processor = new QCOW2Processor();
            qcow2Processor.configure("QCOW2 Processor", params);
            FormatInfo info = qcow2Processor.process(templatePath, null, tmplName);

            TemplateLocation loc = new TemplateLocation(_storage, templatePath);
            loc.create(1, true, tmplName);
            loc.addFormat(info);
            loc.save();

            return new CreatePrivateTemplateAnswer(cmd, true, "", tmplPath, info.virtualSize, info.size, tmplName, info.format);
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
                _storagePoolMgr.deleteStoragePool(secondaryPool.getType(), secondaryPool.getUuid());
            }
            if (snapshotPool != null) {
                _storagePoolMgr.deleteStoragePool(snapshotPool.getType(), snapshotPool.getUuid());
            }
        }
    }

    protected GetStorageStatsAnswer execute(final GetStorageStatsCommand cmd) {
        try {
            KVMStoragePool sp = _storagePoolMgr.getStoragePool(cmd.getPooltype(), cmd.getStorageId(), true);
            return new GetStorageStatsAnswer(cmd, sp.getCapacity(), sp.getUsed());
        } catch (CloudRuntimeException e) {
            return new GetStorageStatsAnswer(cmd, e.toString());
        }
    }

    protected CreatePrivateTemplateAnswer execute(CreatePrivateTemplateFromVolumeCommand cmd) {
        String secondaryStorageURL = cmd.getSecondaryStorageUrl();

        KVMStoragePool secondaryStorage = null;
        KVMStoragePool primary = null;
        try {
            String templateFolder = cmd.getAccountId() + File.separator + cmd.getTemplateId() + File.separator;
            String templateInstallFolder = "/template/tmpl/" + templateFolder;

            secondaryStorage = _storagePoolMgr.getStoragePoolByURI(secondaryStorageURL);

            try {
                primary = _storagePoolMgr.getStoragePool(cmd.getPool().getType(), cmd.getPrimaryStoragePoolNameLabel());
            } catch (CloudRuntimeException e) {
                if (e.getMessage().contains("not found")) {
                    primary =
                            _storagePoolMgr.createStoragePool(cmd.getPool().getUuid(), cmd.getPool().getHost(), cmd.getPool().getPort(), cmd.getPool().getPath(),
                                    cmd.getPool().getUserInfo(), cmd.getPool().getType());
                } else {
                    return new CreatePrivateTemplateAnswer(cmd, false, e.getMessage());
                }
            }

            KVMPhysicalDisk disk = primary.getPhysicalDisk(cmd.getVolumePath());
            String tmpltPath = secondaryStorage.getLocalPath() + File.separator + templateInstallFolder;
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

                QemuImgFile srcFile =
                        new QemuImgFile(KVMPhysicalDisk.RBDStringBuilder(primary.getSourceHost(), primary.getSourcePort(), primary.getAuthUserName(),
                                primary.getAuthSecret(), disk.getPath()));
                srcFile.setFormat(PhysicalDiskFormat.RAW);

                QemuImgFile destFile = new QemuImgFile(tmpltPath + "/" + cmd.getUniqueName() + ".qcow2");
                destFile.setFormat(PhysicalDiskFormat.QCOW2);

                QemuImg q = new QemuImg(0);
                try {
                    q.convert(srcFile, destFile);
                } catch (QemuImgException e) {
                    s_logger.error("Failed to create new template while converting " + srcFile.getFileName() + " to " + destFile.getFileName() + " the error was: " +
                            e.getMessage());
                }

                File templateProp = new File(tmpltPath + "/template.properties");
                if (!templateProp.exists()) {
                    templateProp.createNewFile();
                }

                String templateContent = "filename=" + cmd.getUniqueName() + ".qcow2" + System.getProperty("line.separator");

                DateFormat dateFormat = new SimpleDateFormat("MM_dd_yyyy");
                Date date = new Date();
                templateContent += "snapshot.name=" + dateFormat.format(date) + System.getProperty("line.separator");

                try(FileOutputStream templFo = new FileOutputStream(templateProp);) {
                    templFo.write(templateContent.getBytes());
                    templFo.flush();
                }catch(IOException ex)
                {
                    s_logger.error("CreatePrivateTemplateAnswer:Exception:"+ex.getMessage());
                }

            }

            Map<String, Object> params = new HashMap<String, Object>();
            params.put(StorageLayer.InstanceConfigKey, _storage);
            Processor qcow2Processor = new QCOW2Processor();

            qcow2Processor.configure("QCOW2 Processor", params);

            FormatInfo info = qcow2Processor.process(tmpltPath, null, cmd.getUniqueName());

            TemplateLocation loc = new TemplateLocation(_storage, tmpltPath);
            loc.create(1, true, cmd.getUniqueName());
            loc.addFormat(info);
            loc.save();

            return new CreatePrivateTemplateAnswer(cmd, true, null, templateInstallFolder + cmd.getUniqueName() + ".qcow2", info.virtualSize, info.size,
                    cmd.getUniqueName(), ImageFormat.QCOW2);
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
                _storagePoolMgr.deleteStoragePool(secondaryStorage.getType(), secondaryStorage.getUuid());
            }
        }
    }

    protected PrimaryStorageDownloadAnswer execute(final PrimaryStorageDownloadCommand cmd) {
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
                    return new PrimaryStorageDownloadAnswer("Failed to get volumes from pool: " + secondaryPool.getUuid());
                }
                for (KVMPhysicalDisk disk : disks) {
                    if (disk.getName().endsWith("qcow2")) {
                        tmplVol = disk;
                        break;
                    }
                }
                if (tmplVol == null) {
                    return new PrimaryStorageDownloadAnswer("Failed to get template from pool: " + secondaryPool.getUuid());
                }
            } else {
                tmplVol = secondaryPool.getPhysicalDisk(tmpltname);
            }

            /* Copy volume to primary storage */
            KVMStoragePool primaryPool = _storagePoolMgr.getStoragePool(cmd.getPool().getType(), cmd.getPoolUuid());

            KVMPhysicalDisk primaryVol = _storagePoolMgr.copyPhysicalDisk(tmplVol, UUID.randomUUID().toString(), primaryPool, 0);

            return new PrimaryStorageDownloadAnswer(primaryVol.getName(), primaryVol.getSize());
        } catch (CloudRuntimeException e) {
            return new PrimaryStorageDownloadAnswer(e.toString());
        } finally {
            if (secondaryPool != null) {
                _storagePoolMgr.deleteStoragePool(secondaryPool.getType(), secondaryPool.getUuid());
            }
        }
    }

    protected Answer execute(CreateStoragePoolCommand cmd) {
        return new Answer(cmd, true, "success");
    }

    protected Answer execute(ModifyStoragePoolCommand cmd) {
        KVMStoragePool storagepool =
                _storagePoolMgr.createStoragePool(cmd.getPool().getUuid(), cmd.getPool().getHost(), cmd.getPool().getPort(), cmd.getPool().getPath(), cmd.getPool()
                        .getUserInfo(), cmd.getPool().getType());
        if (storagepool == null) {
            return new Answer(cmd, false, " Failed to create storage pool");
        }

        Map<String, TemplateProp> tInfo = new HashMap<String, TemplateProp>();
        ModifyStoragePoolAnswer answer = new ModifyStoragePoolAnswer(cmd, storagepool.getCapacity(), storagepool.getAvailable(), tInfo);

        return answer;
    }

    private Answer execute(SecurityGroupRulesCmd cmd) {
        String vif = null;
        String brname = null;
        try {
            Connect conn = LibvirtConnection.getConnectionByVmName(cmd.getVmName());
            List<InterfaceDef> nics = getInterfaces(conn, cmd.getVmName());
            vif = nics.get(0).getDevName();
            brname = nics.get(0).getBrName();
        } catch (LibvirtException e) {
            return new SecurityGroupRuleAnswer(cmd, false, e.toString());
        }

        boolean result =
                add_network_rules(cmd.getVmName(), Long.toString(cmd.getVmId()), cmd.getGuestIp(), cmd.getSignature(), Long.toString(cmd.getSeqNum()), cmd.getGuestMac(),
                        cmd.stringifyRules(), vif, brname, cmd.getSecIpsString());

        if (!result) {
            s_logger.warn("Failed to program network rules for vm " + cmd.getVmName());
            return new SecurityGroupRuleAnswer(cmd, false, "programming network rules failed");
        } else {
            s_logger.debug("Programmed network rules for vm " + cmd.getVmName() + " guestIp=" + cmd.getGuestIp() + ",ingress numrules=" + cmd.getIngressRuleSet().length +
                    ",egress numrules=" + cmd.getEgressRuleSet().length);
            return new SecurityGroupRuleAnswer(cmd);
        }
    }

    private Answer execute(CleanupNetworkRulesCmd cmd) {
        boolean result = cleanup_rules();
        return new Answer(cmd, result, "");
    }

    protected GetVncPortAnswer execute(GetVncPortCommand cmd) {
        try {
            Connect conn = LibvirtConnection.getConnectionByVmName(cmd.getName());
            Integer vncPort = getVncPort(conn, cmd.getName());
            return new GetVncPortAnswer(cmd, _privateIp, 5900 + vncPort);
        } catch (LibvirtException e) {
            return new GetVncPortAnswer(cmd, e.toString());
        }
    }

    protected Answer execute(final CheckConsoleProxyLoadCommand cmd) {
        return executeProxyLoadScan(cmd, cmd.getProxyVmId(), cmd.getProxyVmName(), cmd.getProxyManagementIp(), cmd.getProxyCmdPort());
    }

    protected Answer execute(final WatchConsoleProxyLoadCommand cmd) {
        return executeProxyLoadScan(cmd, cmd.getProxyVmId(), cmd.getProxyVmName(), cmd.getProxyManagementIp(), cmd.getProxyCmdPort());
    }

    protected MaintainAnswer execute(MaintainCommand cmd) {
        return new MaintainAnswer(cmd);
    }

    private Answer executeProxyLoadScan(final Command cmd, final long proxyVmId, final String proxyVmName, final String proxyManagementIp, final int cmdPort) {
        String result = null;

        final StringBuffer sb = new StringBuffer();
        sb.append("http://").append(proxyManagementIp).append(":" + cmdPort).append("/cmd/getstatus");

        boolean success = true;
        try {
            final URL url = new URL(sb.toString());
            final URLConnection conn = url.openConnection();

            final InputStream is = conn.getInputStream();
            final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
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
                    s_logger.warn("Exception when closing , console proxy address : " + proxyManagementIp);
                    success = false;
                }
            }
        } catch (final IOException e) {
            s_logger.warn("Unable to open console proxy command port url, console proxy address : " + proxyManagementIp);
            success = false;
        }

        return new ConsoleProxyLoadAnswer(cmd, proxyVmId, proxyVmName, success, result);
    }

    private Answer execute(AttachIsoCommand cmd) {
        try {
            Connect conn = LibvirtConnection.getConnectionByVmName(cmd.getVmName());
            attachOrDetachISO(conn, cmd.getVmName(), cmd.getIsoPath(), cmd.isAttach());
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
            Connect conn = LibvirtConnection.getConnectionByVmName(cmd.getVmName());
            KVMStoragePool primary = _storagePoolMgr.getStoragePool(cmd.getPooltype(), cmd.getPoolUuid());
            KVMPhysicalDisk disk = primary.getPhysicalDisk(cmd.getVolumePath());
            attachOrDetachDisk(conn, cmd.getAttach(), cmd.getVmName(), disk,
                    cmd.getDeviceId().intValue(), cmd.getBytesReadRate(), cmd.getBytesWriteRate(), cmd.getIopsReadRate(), cmd.getIopsWriteRate(),
                    cmd.getCacheMode());
        } catch (LibvirtException e) {
            return new AttachVolumeAnswer(cmd, e.toString());
        } catch (InternalErrorException e) {
            return new AttachVolumeAnswer(cmd, e.toString());
        }

        return new AttachVolumeAnswer(cmd, cmd.getDeviceId(), cmd.getVolumePath());
    }

    private Answer execute(ReadyCommand cmd) {
        return new ReadyAnswer(cmd);
    }

    protected PowerState convertToPowerState(DomainState ps) {
        final PowerState state = s_powerStatesTable.get(ps);
        return state == null ? PowerState.PowerUnknown : state;
    }

    protected PowerState getVmState(Connect conn, final String vmName) {
        int retry = 3;
        Domain vms = null;
        while (retry-- > 0) {
            try {
                vms = conn.domainLookupByName(vmName);
                PowerState s = convertToPowerState(vms.getInfo().state);
                return s;
            } catch (final LibvirtException e) {
                s_logger.warn("Can't get vm state " + vmName + e.getMessage() + "retry:" + retry);
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
        return PowerState.PowerOff;
    }

    private Answer execute(CheckVirtualMachineCommand cmd) {
        try {
            Connect conn = LibvirtConnection.getConnectionByVmName(cmd.getVmName());
            final PowerState state = getVmState(conn, cmd.getVmName());
            Integer vncPort = null;
            if (state == PowerState.PowerOn) {
                vncPort = getVncPort(conn, cmd.getVmName());
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

    private Answer execute(MigrateCommand cmd) {
        String vmName = cmd.getVmName();

        String result = null;

        List<InterfaceDef> ifaces = null;
        List<DiskDef> disks = null;

        Domain dm = null;
        Connect dconn = null;
        Domain destDomain = null;
        Connect conn = null;
        String xmlDesc = null;
        try {
            conn = LibvirtConnection.getConnectionByVmName(vmName);
            ifaces = getInterfaces(conn, vmName);
            disks = getDisks(conn, vmName);
            dm = conn.domainLookupByName(vmName);
            /*
                We replace the private IP address with the address of the destination host.
                This is because the VNC listens on the private IP address of the hypervisor,
                but that address is ofcourse different on the target host.

                MigrateCommand.getDestinationIp() returns the private IP address of the target
                hypervisor. So it's safe to use.

                The Domain.migrate method from libvirt supports passing a different XML
                description for the instance to be used on the target host.

                This is supported by libvirt-java from version 0.50.0
             */
            xmlDesc = dm.getXMLDesc(0).replace(_privateIp, cmd.getDestinationIp());

            dconn = new Connect("qemu+tcp://" + cmd.getDestinationIp() + "/system");

            //run migration in thread so we can monitor it
            s_logger.info("Live migration of instance " + vmName + " initiated");
            ExecutorService executor = Executors.newFixedThreadPool(1);
            Callable<Domain> worker = new MigrateKVMAsync(dm, dconn, xmlDesc, vmName, cmd.getDestinationIp());
            Future<Domain> migrateThread = executor.submit(worker);
            executor.shutdown();
            long sleeptime = 0;
            while (!executor.isTerminated()) {
                Thread.sleep(100);
                sleeptime += 100;
                if (sleeptime == 1000) { // wait 1s before attempting to set downtime on migration, since I don't know of a VIR_DOMAIN_MIGRATING state
                    if (_migrateDowntime > 0 ) {
                        try {
                            int setDowntime = dm.migrateSetMaxDowntime(_migrateDowntime);
                            if (setDowntime == 0 ) {
                                s_logger.debug("Set max downtime for migration of " + vmName + " to " + String.valueOf(_migrateDowntime) + "ms");
                            }
                        } catch (LibvirtException e) {
                            s_logger.debug("Failed to set max downtime for migration, perhaps migration completed? Error: " + e.getMessage());
                        }
                    }
                }
                if ((sleeptime % 1000) == 0) {
                    s_logger.info("Waiting for migration of " + vmName + " to complete, waited " + sleeptime + "ms");
                }

                // pause vm if we meet the vm.migrate.pauseafter threshold and not already paused
                if (_migratePauseAfter > 0 && sleeptime > _migratePauseAfter && dm.getInfo().state == DomainState.VIR_DOMAIN_RUNNING ) {
                    s_logger.info("Pausing VM " + vmName + " due to property vm.migrate.pauseafter setting to " + _migratePauseAfter+ "ms to complete migration");
                    try {
                        dm.suspend();
                    } catch (LibvirtException e) {
                        // pause could be racy if it attempts to pause right when vm is finished, simply warn
                        s_logger.info("Failed to pause vm " + vmName + " : " + e.getMessage());
                    }
                }
            }
            s_logger.info("Migration thread for " + vmName + " is done");

            destDomain = migrateThread.get(10, TimeUnit.SECONDS);

            if (destDomain != null) {
                for (DiskDef disk : disks) {
                    cleanupDisk(disk);
                }
            }
        } catch (LibvirtException e) {
            s_logger.debug("Can't migrate domain: " + e.getMessage());
            result = e.getMessage();
        } catch (InterruptedException e) {
            s_logger.debug("Interrupted while migrating domain: " + e.getMessage());
            result = e.getMessage();
        } catch (ExecutionException e) {
            s_logger.debug("Failed to execute while migrating domain: " + e.getMessage());
            result = e.getMessage();
        } catch (TimeoutException e) {
            s_logger.debug("Timed out while migrating domain: " + e.getMessage());
            result = e.getMessage();
        } finally {
            try {
                if (dm != null) {
                    if (dm.isPersistent() == 1) {
                        dm.undefine();
                    }
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
        } else {
            destroy_network_rules_for_vm(conn, vmName);
            for (InterfaceDef iface : ifaces) {
                // We don't know which "traffic type" is associated with
                // each interface at this point, so inform all vif drivers
                for (VifDriver vifDriver : getAllVifDrivers()) {
                    vifDriver.unplug(iface);
                }
            }
        }

        return new MigrateAnswer(cmd, result == null, result, null);
    }

    private class MigrateKVMAsync implements Callable<Domain> {
        Domain dm = null;
        Connect dconn = null;
        String dxml = "";
        String vmName = "";
        String destIp = "";

        MigrateKVMAsync(Domain dm, Connect dconn, String dxml, String vmName, String destIp) {
            this.dm = dm;
            this.dconn = dconn;
            this.dxml = dxml;
            this.vmName = vmName;
            this.destIp = destIp;
        }

        @Override
        public Domain call() throws LibvirtException {
            // set compression flag for migration if libvirt version supports it
            if (dconn.getLibVirVersion() < 1003000) {
                return dm.migrate(dconn, (1 << 0), dxml, vmName, "tcp:" + destIp, _migrateSpeed);
            } else {
                return dm.migrate(dconn, (1 << 0)|(1 << 11), dxml, vmName, "tcp:" + destIp, _migrateSpeed);
            }
        }
    }

    private synchronized Answer execute(PrepareForMigrationCommand cmd) {

        VirtualMachineTO vm = cmd.getVirtualMachine();
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Preparing host for migrating " + vm);
        }

        NicTO[] nics = vm.getNics();

        boolean skipDisconnect = false;

        try {
            Connect conn = LibvirtConnection.getConnectionByVmName(vm.getName());
            for (NicTO nic : nics) {
                getVifDriver(nic.getType()).plug(nic, null, "");
            }

            /* setup disks, e.g for iso */
            DiskTO[] volumes = vm.getDisks();
            for (DiskTO volume : volumes) {
                if (volume.getType() == Volume.Type.ISO) {
                    getVolumePath(conn, volume);
                }
            }

            if (!_storagePoolMgr.connectPhysicalDisksViaVmSpec(vm)) {
                skipDisconnect = true;
                return new PrepareForMigrationAnswer(cmd, "failed to connect physical disks to host");
            }

            skipDisconnect = true;

            return new PrepareForMigrationAnswer(cmd);
        } catch (LibvirtException e) {
            return new PrepareForMigrationAnswer(cmd, e.toString());
        } catch (InternalErrorException e) {
            return new PrepareForMigrationAnswer(cmd, e.toString());
        } catch (URISyntaxException e) {
            return new PrepareForMigrationAnswer(cmd, e.toString());
        } finally {
            if (!skipDisconnect) {
                _storagePoolMgr.disconnectPhysicalDisksViaVmSpec(vm);
            }
        }
    }

    private Answer execute(CheckHealthCommand cmd) {
        return new CheckHealthAnswer(cmd, true);
    }

    private Answer execute(GetHostStatsCommand cmd) {
        final Script cpuScript = new Script("/bin/bash", s_logger);
        cpuScript.add("-c");
        cpuScript.add("idle=$(top -b -n 1| awk -F, '/^[%]*[Cc]pu/{$0=$4; gsub(/[^0-9.,]+/,\"\"); print }'); echo $idle");

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
        memScript.add("freeMem=$(free|grep cache:|awk '{print $4}');echo $freeMem");
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

        HostStatsEntry hostStats = new HostStatsEntry(cmd.getHostId(), cpuUtil, nicStats.first() / 1024, nicStats.second() / 1024, "host", totMem, freeMem, 0, 0);
        return new GetHostStatsAnswer(cmd, hostStats);
    }

    protected String networkUsage(final String privateIpAddress, final String option, final String vif) {
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
                stats[0] += Long.parseLong(splitResult[i++]);
                stats[1] += Long.parseLong(splitResult[i++]);
            }
        }
        return stats;
    }

    protected String VPCNetworkUsage(final String privateIpAddress, final String publicIp, final String option, final String vpcCIDR) {
        Script getUsage = new Script(_routerProxyPath, s_logger);
        getUsage.add("vpc_netusage.sh");
        getUsage.add(privateIpAddress);
        getUsage.add("-l", publicIp);

        if (option.equals("get")) {
            getUsage.add("-g");
        } else if (option.equals("create")) {
            getUsage.add("-c");
            getUsage.add("-v", vpcCIDR);
        } else if (option.equals("reset")) {
            getUsage.add("-r");
        } else if (option.equals("vpn")) {
            getUsage.add("-n");
        } else if (option.equals("remove")) {
            getUsage.add("-d");
        }

        final OutputInterpreter.OneLineParser usageParser = new OutputInterpreter.OneLineParser();
        String result = getUsage.execute(usageParser);
        if (result != null) {
            s_logger.debug("Failed to execute VPCNetworkUsage:" + result);
            return null;
        }
        return usageParser.getLine();
    }

    protected long[] getVPCNetworkStats(String privateIP, String publicIp, String option) {
        String result = VPCNetworkUsage(privateIP, publicIp, option, null);
        long[] stats = new long[2];
        if (result != null) {
            String[] splitResult = result.split(":");
            int i = 0;
            while (i < splitResult.length - 1) {
                stats[0] += Long.parseLong(splitResult[i++]);
                stats[1] += Long.parseLong(splitResult[i++]);
            }
        }
        return stats;
    }

    private Answer execute(NetworkUsageCommand cmd) {
        if (cmd.isForVpc()) {
            if (cmd.getOption() != null && cmd.getOption().equals("create")) {
                String result = VPCNetworkUsage(cmd.getPrivateIP(), cmd.getGatewayIP(), "create", cmd.getVpcCIDR());
                NetworkUsageAnswer answer = new NetworkUsageAnswer(cmd, result, 0L, 0L);
                return answer;
            } else if (cmd.getOption() != null && (cmd.getOption().equals("get") || cmd.getOption().equals("vpn"))) {
                long[] stats = getVPCNetworkStats(cmd.getPrivateIP(), cmd.getGatewayIP(), cmd.getOption());
                NetworkUsageAnswer answer = new NetworkUsageAnswer(cmd, "", stats[0], stats[1]);
                return answer;
            } else {
                String result = VPCNetworkUsage(cmd.getPrivateIP(), cmd.getGatewayIP(), cmd.getOption(), cmd.getVpcCIDR());
                NetworkUsageAnswer answer = new NetworkUsageAnswer(cmd, result, 0L, 0L);
                return answer;
            }
        } else {
            if (cmd.getOption() != null && cmd.getOption().equals("create")) {
                String result = networkUsage(cmd.getPrivateIP(), "create", null);
                NetworkUsageAnswer answer = new NetworkUsageAnswer(cmd, result, 0L, 0L);
                return answer;
            }
            long[] stats = getNetworkStats(cmd.getPrivateIP());
            NetworkUsageAnswer answer = new NetworkUsageAnswer(cmd, "", stats[0], stats[1]);
            return answer;
        }
    }

    private Answer execute(RebootCommand cmd) {

        try {
            Connect conn = LibvirtConnection.getConnectionByVmName(cmd.getVmName());
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
        }
    }

    protected Answer execute(RebootRouterCommand cmd) {
        RebootAnswer answer = (RebootAnswer)execute((RebootCommand)cmd);
        if (_virtRouterResource.connect(cmd.getPrivateIpAddress())) {
            networkUsage(cmd.getPrivateIpAddress(), "create", null);
            return answer;
        } else {
            return new Answer(cmd, false, "Failed to connect to virtual router " + cmd.getVmName());
        }
    }

    protected GetVmDiskStatsAnswer execute(GetVmDiskStatsCommand cmd) {
        List<String> vmNames = cmd.getVmNames();
        try {
            HashMap<String, List<VmDiskStatsEntry>> vmDiskStatsNameMap = new HashMap<String, List<VmDiskStatsEntry>>();
            Connect conn = LibvirtConnection.getConnection();
            for (String vmName : vmNames) {
                List<VmDiskStatsEntry> statEntry = getVmDiskStat(conn, vmName);
                if (statEntry == null) {
                    continue;
                }

                vmDiskStatsNameMap.put(vmName, statEntry);
            }
            return new GetVmDiskStatsAnswer(cmd, "", cmd.getHostName(), vmDiskStatsNameMap);
        } catch (LibvirtException e) {
            s_logger.debug("Can't get vm disk stats: " + e.toString());
            return new GetVmDiskStatsAnswer(cmd, null, null, null);
        }
    }

    protected GetVmStatsAnswer execute(GetVmStatsCommand cmd) {
        List<String> vmNames = cmd.getVmNames();
        try {
            HashMap<String, VmStatsEntry> vmStatsNameMap = new HashMap<String, VmStatsEntry>();
            for (String vmName : vmNames) {
                Connect conn = LibvirtConnection.getConnectionByVmName(vmName);
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

        if (cmd.checkBeforeCleanup()) {
            try {
                Connect conn = LibvirtConnection.getConnectionByVmName(vmName);
                Domain vm = conn.domainLookupByName(cmd.getVmName());
                if (vm != null && vm.getInfo().state == DomainState.VIR_DOMAIN_RUNNING) {
                    return new StopAnswer(cmd, "vm is still running on host", false);
                }
            } catch (Exception e) {
                s_logger.debug("Failed to get vm status in case of checkboforecleanup is true", e);
            }
        }

        try {
            Connect conn = LibvirtConnection.getConnectionByVmName(vmName);

            List<DiskDef> disks = getDisks(conn, vmName);
            List<InterfaceDef> ifaces = getInterfaces(conn, vmName);

            destroy_network_rules_for_vm(conn, vmName);
            String result = stopVM(conn, vmName);
            if (result == null) {
                for (DiskDef disk : disks) {
                    cleanupDisk(disk);
                }
                for (InterfaceDef iface : ifaces) {
                    // We don't know which "traffic type" is associated with
                    // each interface at this point, so inform all vif drivers
                    for (VifDriver vifDriver : getAllVifDrivers()) {
                        vifDriver.unplug(iface);
                    }
                }
            }

            return new StopAnswer(cmd, result, true);
        } catch (LibvirtException e) {
            return new StopAnswer(cmd, e.getMessage(), false);
        }
    }

    protected Answer execute(ModifySshKeysCommand cmd) {
        File sshKeysDir = new File(SSHKEYSPATH);
        String result = null;
        if (!sshKeysDir.exists()) {
            // Change permissions for the 700
            Script script = new Script("mkdir", _timeout, s_logger);
            script.add("-m", "700");
            script.add(SSHKEYSPATH);
            script.execute();

            if (!sshKeysDir.exists()) {
                s_logger.debug("failed to create directory " + SSHKEYSPATH);
            }
        }

        File pubKeyFile = new File(SSHPUBKEYPATH);
        if (!pubKeyFile.exists()) {
            try {
                pubKeyFile.createNewFile();
            } catch (IOException e) {
                result = "Failed to create file: " + e.toString();
                s_logger.debug(result);
            }
        }

        if (pubKeyFile.exists()) {
            try (FileOutputStream pubkStream = new FileOutputStream(pubKeyFile)) {
                pubkStream.write(cmd.getPubKey().getBytes());
            } catch (FileNotFoundException e) {
                result = "File" + SSHPUBKEYPATH + "is not found:"
                        + e.toString();
                s_logger.debug(result);
            } catch (IOException e) {
                result = "Write file " + SSHPUBKEYPATH + ":" + e.toString();
                s_logger.debug(result);
            }
        }

        File prvKeyFile = new File(SSHPRVKEYPATH);
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
            try (FileOutputStream prvKStream = new FileOutputStream(prvKeyFile);){
                if ( prvKStream != null) {
                    prvKStream.write(prvKey.getBytes());
                }
            } catch (FileNotFoundException e) {
                result = "File" + SSHPRVKEYPATH + "is not found:" + e.toString();
                s_logger.debug(result);
            } catch (IOException e) {
                result = "Write file " + SSHPRVKEYPATH + ":" + e.toString();
                s_logger.debug(result);
            }
            Script script = new Script("chmod", _timeout, s_logger);
            script.add("600", SSHPRVKEYPATH);
            script.execute();
        }

        if (result != null) {
            return new Answer(cmd, false, result);
        } else {
            return new Answer(cmd, true, null);
        }
    }

    protected void handleVmStartFailure(Connect conn, String vmName, LibvirtVMDef vm) {
        if (vm != null && vm.getDevices() != null) {
            cleanupVMNetworks(conn, vm.getDevices().getInterfaces());
        }
    }

    protected String getUuid(String uuid) {
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
        } else {
            try {
                UUID uuid2 = UUID.fromString(uuid);
                String uuid3 = uuid2.toString();
                if (!uuid3.equals(uuid)) {
                    uuid = UUID.randomUUID().toString();
                }
            } catch (IllegalArgumentException e) {
                uuid = UUID.randomUUID().toString();
            }
        }
        return uuid;
    }

    private void getOsVersion() {
        String version = Script.runSimpleBashScript("cat /etc/redhat-release | awk '{print $7}'");
        if (version != null) {
            String[] versions = version.split("\\.");
            if (versions.length == 2) {
                String major = versions[0];
                String minor = versions[1];
                try {
                    Integer m = Integer.parseInt(major);
                    Integer min = Integer.parseInt(minor);
                    hostOsVersion = new Pair<>(m, min);
                } catch(NumberFormatException e) {

                }
            }
        }
    }

    protected LibvirtVMDef createVMFromSpec(VirtualMachineTO vmTO) {
        LibvirtVMDef vm = new LibvirtVMDef();
        vm.setDomainName(vmTO.getName());
        String uuid = vmTO.getUuid();
        uuid = getUuid(uuid);
        vm.setDomUUID(uuid);
        vm.setDomDescription(vmTO.getOs());
        vm.setPlatformEmulator(vmTO.getPlatformEmulator());

        GuestDef guest = new GuestDef();

        if (HypervisorType.LXC == _hypervisorType && VirtualMachine.Type.User == vmTO.getType()) {
            // LXC domain is only valid for user VMs. Use KVM for system VMs.
            guest.setGuestType(GuestDef.guestType.LXC);
            vm.setHvsType(HypervisorType.LXC.toString().toLowerCase());
        } else {
            guest.setGuestType(GuestDef.guestType.KVM);
            vm.setHvsType(HypervisorType.KVM.toString().toLowerCase());
            vm.setLibvirtVersion(_hypervisorLibvirtVersion);
            vm.setQemuVersion(_hypervisorQemuVersion);
        }
        guest.setGuestArch(vmTO.getArch());
        guest.setMachineType("pc");
        guest.setBootOrder(GuestDef.bootOrder.CDROM);
        guest.setBootOrder(GuestDef.bootOrder.HARDISK);

        vm.addComp(guest);

        GuestResourceDef grd = new GuestResourceDef();

        if (vmTO.getMinRam() != vmTO.getMaxRam() && !_noMemBalloon) {
            grd.setMemBalloning(true);
            grd.setCurrentMem(vmTO.getMinRam() / 1024);
            grd.setMemorySize(vmTO.getMaxRam() / 1024);
        } else {
            grd.setMemorySize(vmTO.getMaxRam() / 1024);
        }
        int vcpus = vmTO.getCpus();
        grd.setVcpuNum(vcpus);
        vm.addComp(grd);

        CpuModeDef cmd = new CpuModeDef();
        cmd.setMode(_guestCpuMode);
        cmd.setModel(_guestCpuModel);
        // multi cores per socket, for larger core configs
        if (vcpus % 6 == 0) {
            int sockets = vcpus / 6;
            cmd.setTopology(6, sockets);
        } else if (vcpus % 4 == 0) {
            int sockets = vcpus / 4;
            cmd.setTopology(4, sockets);
        }
        vm.addComp(cmd);

        if (_hypervisorLibvirtVersion >= 9000) {
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
        }

        FeaturesDef features = new FeaturesDef();
        features.addFeatures("pae");
        features.addFeatures("apic");
        features.addFeatures("acpi");
        //for rhel 6.5 and above, hyperv enlightment feature is added
        /*
         * if (vmTO.getOs().contains("Windows Server 2008") && hostOsVersion != null && ((hostOsVersion.first() == 6 && hostOsVersion.second() >= 5) || (hostOsVersion.first() >= 7))) {
         *    LibvirtVMDef.HyperVEnlightenmentFeatureDef hyv = new LibvirtVMDef.HyperVEnlightenmentFeatureDef();
         *    hyv.setRelaxed(true);
         *    features.addHyperVFeature(hyv);
         * }
         */
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
        } else if (vmTO.getType() != VirtualMachine.Type.User || isGuestPVEnabled(vmTO.getOs())) {
            clock.setTimer("kvmclock", null, null, _noKvmClock);
        }

        vm.addComp(clock);

        DevicesDef devices = new DevicesDef();
        devices.setEmulatorPath(_hypervisorPath);
        devices.setGuestType(guest.getGuestType());

        SerialDef serial = new SerialDef("pty", null, (short)0);
        devices.addDevice(serial);

        if (vmTO.getType() != VirtualMachine.Type.User) {
            VirtioSerialDef vserial = new VirtioSerialDef(vmTO.getName(), null);
            devices.addDevice(vserial);
        }

        VideoDef videoCard = new VideoDef(_videoHw, _videoRam);
        devices.addDevice(videoCard);

        ConsoleDef console = new ConsoleDef("pty", null, null, (short)0);
        devices.addDevice(console);

        //add the VNC port passwd here, get the passwd from the vmInstance.
        String passwd = vmTO.getVncPassword();
        GraphicDef grap = new GraphicDef("vnc", (short)0, true, vmTO.getVncAddr(), passwd, null);
        devices.addDevice(grap);

        InputDef input = new InputDef("tablet", "usb");
        devices.addDevice(input);

        vm.addComp(devices);

        return vm;
    }

    protected void createVifs(VirtualMachineTO vmSpec, LibvirtVMDef vm) throws InternalErrorException, LibvirtException {
        NicTO[] nics = vmSpec.getNics();
        Map <String, String> params = vmSpec.getDetails();
        String nicAdapter = "";
        if (params != null && params.get("nicAdapter") != null && !params.get("nicAdapter").isEmpty()) {
            nicAdapter = params.get("nicAdapter");
        }
        for (int i = 0; i < nics.length; i++) {
            for (NicTO nic : vmSpec.getNics()) {
                if (nic.getDeviceId() == i) {
                    createVif(vm, nic, nicAdapter);
                }
            }
        }
    }

    protected StartAnswer execute(StartCommand cmd) {
        VirtualMachineTO vmSpec = cmd.getVirtualMachine();
        vmSpec.setVncAddr(cmd.getHostIp());
        String vmName = vmSpec.getName();
        LibvirtVMDef vm = null;

        DomainState  state = DomainState.VIR_DOMAIN_SHUTOFF;
        Connect conn = null;
        try {
            NicTO[] nics = vmSpec.getNics();

            for (NicTO nic : nics) {
                if (vmSpec.getType() != VirtualMachine.Type.User) {
                    nic.setPxeDisable(true);
                }
            }

            vm = createVMFromSpec(vmSpec);

            conn = LibvirtConnection.getConnectionByType(vm.getHvsType());

            createVbd(conn, vmSpec, vmName, vm);

            if (!_storagePoolMgr.connectPhysicalDisksViaVmSpec(vmSpec)) {
                return new StartAnswer(cmd, "Failed to connect physical disks to host");
            }

            createVifs(vmSpec, vm);

            s_logger.debug("starting " + vmName + ": " + vm.toString());
            startVM(conn, vmName, vm.toString());

            for (NicTO nic : nics) {
                if (nic.isSecurityGroupEnabled() || (nic.getIsolationUri() != null && nic.getIsolationUri().getScheme().equalsIgnoreCase(IsolationType.Ec2.toString()))) {
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
                //wait and try passCmdLine for 5 minutes at most for CLOUDSTACK-2823
                String controlIp = null;
                for (NicTO nic : nics) {
                    if (nic.getType() == TrafficType.Control) {
                        controlIp = nic.getIp();
                        break;
                    }
                }
                for (int count = 0; count < 30; count++) {
                    passCmdLine(vmName, vmSpec.getBootArgs());
                    //check router is up?
                    boolean result = _virtRouterResource.connect(controlIp, 1, 5000);
                    if (result) {
                        break;
                    }
                }
            }

            state = DomainState.VIR_DOMAIN_RUNNING;
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
            if (state != DomainState.VIR_DOMAIN_RUNNING) {
                _storagePoolMgr.disconnectPhysicalDisksViaVmSpec(vmSpec);
            }
        }
    }

    private String getVolumePath(Connect conn, DiskTO volume) throws LibvirtException, URISyntaxException {
        DataTO data = volume.getData();
        DataStoreTO store = data.getDataStore();

        if (volume.getType() == Volume.Type.ISO && data.getPath() != null) {
            NfsTO nfsStore = (NfsTO)store;
            String isoPath = nfsStore.getUrl() + File.separator + data.getPath();
            int index = isoPath.lastIndexOf("/");
            String path = isoPath.substring(0, index);
            String name = isoPath.substring(index + 1);
            KVMStoragePool secondaryPool = _storagePoolMgr.getStoragePoolByURI(path);
            KVMPhysicalDisk isoVol = secondaryPool.getPhysicalDisk(name);
            return isoVol.getPath();
        } else {
            return data.getPath();
        }
    }

    protected void createVbd(Connect conn, VirtualMachineTO vmSpec, String vmName, LibvirtVMDef vm) throws InternalErrorException, LibvirtException, URISyntaxException {
        List<DiskTO> disks = Arrays.asList(vmSpec.getDisks());
        Collections.sort(disks, new Comparator<DiskTO>() {
            @Override
            public int compare(DiskTO arg0, DiskTO arg1) {
                return arg0.getDiskSeq() > arg1.getDiskSeq() ? 1 : -1;
            }
        });

        for (DiskTO volume : disks) {
            KVMPhysicalDisk physicalDisk = null;
            KVMStoragePool pool = null;
            DataTO data = volume.getData();
            if (volume.getType() == Volume.Type.ISO && data.getPath() != null) {
                NfsTO nfsStore = (NfsTO)data.getDataStore();
                String volPath = nfsStore.getUrl() + File.separator + data.getPath();
                int index = volPath.lastIndexOf("/");
                String volDir = volPath.substring(0, index);
                String volName = volPath.substring(index + 1);
                KVMStoragePool secondaryStorage = _storagePoolMgr.getStoragePoolByURI(volDir);
                physicalDisk = secondaryStorage.getPhysicalDisk(volName);
            } else if (volume.getType() != Volume.Type.ISO) {
                PrimaryDataStoreTO store = (PrimaryDataStoreTO)data.getDataStore();
                physicalDisk = _storagePoolMgr.getPhysicalDisk(store.getPoolType(), store.getUuid(), data.getPath());
                pool = physicalDisk.getPool();
            }

            String volPath = null;
            if (physicalDisk != null) {
                volPath = physicalDisk.getPath();
            }

            // if params contains a rootDiskController key, use its value (this is what other HVs are doing)
            DiskDef.diskBus diskBusType = null;
            Map <String, String> params = vmSpec.getDetails();
            if (params != null && params.get("rootDiskController") != null && !params.get("rootDiskController").isEmpty()) {
                String rootDiskController = params.get("rootDiskController");
                s_logger.debug("Passed custom disk bus " + rootDiskController);
                for (DiskDef.diskBus bus : DiskDef.diskBus.values()) {
                    if (bus.toString().equalsIgnoreCase(rootDiskController)) {
                        s_logger.debug("Found matching enum for disk bus " + rootDiskController);
                        diskBusType = bus;
                        break;
                    }
                }
            }

            if (diskBusType == null) {
                diskBusType = getGuestDiskModel(vmSpec.getPlatformEmulator());
            }
            DiskDef disk = new DiskDef();
            if (volume.getType() == Volume.Type.ISO) {
                if (volPath == null) {
                    /* Add iso as placeholder */
                    disk.defISODisk(null);
                } else {
                    disk.defISODisk(volPath);
                }
            } else {
                int devId = volume.getDiskSeq().intValue();

                if (pool.getType() == StoragePoolType.RBD) {
                    /*
                            For RBD pools we use the secret mechanism in libvirt.
                            We store the secret under the UUID of the pool, that's why
                            we pass the pool's UUID as the authSecret
                     */
                    disk.defNetworkBasedDisk(physicalDisk.getPath().replace("rbd:", ""), pool.getSourceHost(), pool.getSourcePort(), pool.getAuthUserName(),
                            pool.getUuid(), devId, diskBusType, diskProtocol.RBD, DiskDef.diskFmtType.RAW);
                } else if (pool.getType() == StoragePoolType.Gluster) {
                    String mountpoint = pool.getLocalPath();
                    String path = physicalDisk.getPath();
                    String glusterVolume = pool.getSourceDir().replace("/", "");
                    disk.defNetworkBasedDisk(glusterVolume + path.replace(mountpoint, ""), pool.getSourceHost(), pool.getSourcePort(), null,
                            null, devId, diskBusType, diskProtocol.GLUSTER, DiskDef.diskFmtType.QCOW2);
                } else if (pool.getType() == StoragePoolType.CLVM || physicalDisk.getFormat() == PhysicalDiskFormat.RAW) {
                    disk.defBlockBasedDisk(physicalDisk.getPath(), devId, diskBusType);
                } else {
                    if (volume.getType() == Volume.Type.DATADISK) {
                        disk.defFileBasedDisk(physicalDisk.getPath(), devId, DiskDef.diskBus.VIRTIO, DiskDef.diskFmtType.QCOW2);
                    } else {
                        disk.defFileBasedDisk(physicalDisk.getPath(), devId, diskBusType, DiskDef.diskFmtType.QCOW2);
                    }

                }

            }

            if (data instanceof VolumeObjectTO) {
                VolumeObjectTO volumeObjectTO = (VolumeObjectTO)data;
                if ((volumeObjectTO.getBytesReadRate() != null) && (volumeObjectTO.getBytesReadRate() > 0))
                    disk.setBytesReadRate(volumeObjectTO.getBytesReadRate());
                if ((volumeObjectTO.getBytesWriteRate() != null) && (volumeObjectTO.getBytesWriteRate() > 0))
                    disk.setBytesWriteRate(volumeObjectTO.getBytesWriteRate());
                if ((volumeObjectTO.getIopsReadRate() != null) && (volumeObjectTO.getIopsReadRate() > 0))
                    disk.setIopsReadRate(volumeObjectTO.getIopsReadRate());
                if ((volumeObjectTO.getIopsWriteRate() != null) && (volumeObjectTO.getIopsWriteRate() > 0))
                    disk.setIopsWriteRate(volumeObjectTO.getIopsWriteRate());
                if (volumeObjectTO.getCacheMode() != null)
                    disk.setCacheMode(DiskDef.diskCacheMode.valueOf(volumeObjectTO.getCacheMode().toString().toUpperCase()));
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

        // For LXC, find and add the root filesystem, rbd data disks
        if (HypervisorType.LXC.toString().toLowerCase().equals(vm.getHvsType())) {
            for (DiskTO volume : disks) {
                DataTO data = volume.getData();
                PrimaryDataStoreTO store = (PrimaryDataStoreTO)data.getDataStore();
                if (volume.getType() == Volume.Type.ROOT) {
                    KVMPhysicalDisk physicalDisk = _storagePoolMgr.getPhysicalDisk(store.getPoolType(), store.getUuid(), data.getPath());
                    FilesystemDef rootFs = new FilesystemDef(physicalDisk.getPath(), "/");
                    vm.getDevices().addDevice(rootFs);
                } else if (volume.getType() == Volume.Type.DATADISK) {
                    KVMPhysicalDisk physicalDisk = _storagePoolMgr.getPhysicalDisk(store.getPoolType(), store.getUuid(), data.getPath());
                    KVMStoragePool pool = physicalDisk.getPool();
                    if(StoragePoolType.RBD.equals(pool.getType())) {
                        int devId = volume.getDiskSeq().intValue();
                        String device = mapRbdDevice(physicalDisk);
                        if (device != null) {
                            s_logger.debug("RBD device on host is: " + device);
                            DiskDef diskdef = new DiskDef();
                            diskdef.defBlockBasedDisk(device, devId, DiskDef.diskBus.VIRTIO);
                            diskdef.setQemuDriver(false);
                            vm.getDevices().addDevice(diskdef);
                        } else {
                            throw new InternalErrorException("Error while mapping RBD device on host");
                        }
                    }
                }
            }
        }

    }

    private void createVif(LibvirtVMDef vm, NicTO nic, String nicAdapter) throws InternalErrorException, LibvirtException {
        vm.getDevices().addDevice(getVifDriver(nic.getType()).plug(nic, vm.getPlatformEmulator().toString(), nicAdapter).toString());
    }

    protected CheckSshAnswer execute(CheckSshCommand cmd) {
        String vmName = cmd.getName();
        String privateIp = cmd.getIp();
        int cmdPort = cmd.getPort();

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Ping command port, " + privateIp + ":" + cmdPort);
        }

        if (!_virtRouterResource.connect(privateIp, cmdPort)) {
            return new CheckSshAnswer(cmd, "Can not ping System vm " + vmName + " because of a connection failure");
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Ping command port succeeded for vm " + vmName);
        }

        return new CheckSshAnswer(cmd);
    }

    public boolean cleanupDisk(DiskDef disk) {
        String path = disk.getDiskPath();

        if (path == null) {
            s_logger.debug("Unable to clean up disk with null path (perhaps empty cdrom drive):" + disk);
            return false;
        }

        if (path.endsWith("systemvm.iso")) {
            // don't need to clean up system vm ISO as it's stored in local
            return true;
        }

        return _storagePoolMgr.disconnectPhysicalDiskByPath(path);
    }

    protected KVMStoragePoolManager getPoolManager() {
        return _storagePoolMgr;
    }

    protected synchronized String attachOrDetachISO(Connect conn, String vmName, String isoPath, boolean isAttach) throws LibvirtException, URISyntaxException,
    InternalErrorException {
        String isoXml = null;
        if (isoPath != null && isAttach) {
            int index = isoPath.lastIndexOf("/");
            String path = isoPath.substring(0, index);
            String name = isoPath.substring(index + 1);
            KVMStoragePool secondaryPool = _storagePoolMgr.getStoragePoolByURI(path);
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
                    cleanupDisk(disk);
                }
            }

        }
        return result;
    }

    protected synchronized String attachOrDetachDisk(Connect conn,
            boolean attach, String vmName, KVMPhysicalDisk attachingDisk,
            int devId, Long bytesReadRate, Long bytesWriteRate, Long iopsReadRate, Long iopsWriteRate, String cacheMode) throws LibvirtException, InternalErrorException {
        List<DiskDef> disks = null;
        Domain dm = null;
        DiskDef diskdef = null;
        KVMStoragePool attachingPool = attachingDisk.getPool();
        try {
            if (!attach) {
                dm = conn.domainLookupByName(vmName);
                LibvirtDomainXMLParser parser = new LibvirtDomainXMLParser();
                String xml = dm.getXMLDesc(0);
                parser.parseDomainXML(xml);
                disks = parser.getDisks();

                for (DiskDef disk : disks) {
                    String file = disk.getDiskPath();
                    if (file != null && file.equalsIgnoreCase(attachingDisk.getPath())) {
                        diskdef = disk;
                        break;
                    }
                }
                if (diskdef == null) {
                    throw new InternalErrorException("disk: " + attachingDisk.getPath() + " is not attached before");
                }
            } else {
                diskdef = new DiskDef();
                if (attachingPool.getType() == StoragePoolType.RBD) {
                    diskdef.defNetworkBasedDisk(attachingDisk.getPath(), attachingPool.getSourceHost(), attachingPool.getSourcePort(), attachingPool.getAuthUserName(),
                            attachingPool.getUuid(), devId, DiskDef.diskBus.VIRTIO, diskProtocol.RBD, DiskDef.diskFmtType.RAW);
                } else if (attachingPool.getType() == StoragePoolType.Gluster) {
                    diskdef.defNetworkBasedDisk(attachingDisk.getPath(), attachingPool.getSourceHost(), attachingPool.getSourcePort(), null,
                            null, devId, DiskDef.diskBus.VIRTIO, diskProtocol.GLUSTER, DiskDef.diskFmtType.QCOW2);
                } else if (attachingDisk.getFormat() == PhysicalDiskFormat.QCOW2) {
                    diskdef.defFileBasedDisk(attachingDisk.getPath(), devId, DiskDef.diskBus.VIRTIO, DiskDef.diskFmtType.QCOW2);
                } else if (attachingDisk.getFormat() == PhysicalDiskFormat.RAW) {
                    diskdef.defBlockBasedDisk(attachingDisk.getPath(), devId, DiskDef.diskBus.VIRTIO);
                }
                if ((bytesReadRate != null) && (bytesReadRate > 0))
                    diskdef.setBytesReadRate(bytesReadRate);
                if ((bytesWriteRate != null) && (bytesWriteRate > 0))
                    diskdef.setBytesWriteRate(bytesWriteRate);
                if ((iopsReadRate != null) && (iopsReadRate > 0))
                    diskdef.setIopsReadRate(iopsReadRate);
                if ((iopsWriteRate != null) && (iopsWriteRate > 0))
                    diskdef.setIopsWriteRate(iopsWriteRate);

                if (cacheMode != null) {
                    diskdef.setCacheMode(DiskDef.diskCacheMode.valueOf(cacheMode.toUpperCase()));
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

    protected synchronized String attachOrDetachDevice(Connect conn, boolean attach, String vmName, String xml) throws LibvirtException, InternalErrorException {
        Domain dm = null;
        try {
            dm = conn.domainLookupByName(vmName);
            if (attach) {
                s_logger.debug("Attaching device: " + xml);
                dm.attachDevice(xml);
            } else {
                s_logger.debug("Detaching device: " + xml);
                dm.detachDevice(xml);
            }
        } catch (LibvirtException e) {
            if (attach) {
                s_logger.warn("Failed to attach device to " + vmName + ": " + e.getMessage());
            } else {
                s_logger.warn("Failed to detach device from " + vmName + ": " + e.getMessage());
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

        if (!_canBridgeFirewall) {
            return new PingRoutingCommand(com.cloud.host.Host.Type.Routing, id, this.getHostVmStateReport());
        } else {
            HashMap<String, Pair<Long, Long>> nwGrpStates = syncNetworkGroups(id);
            return new PingRoutingWithNwGroupsCommand(getType(), id, this.getHostVmStateReport(), nwGrpStates);
        }
    }

    @Override
    public Type getType() {
        return Type.Routing;
    }

    private Map<String, String> getVersionStrings() {
        final Script command = new Script(_versionstringpath, _timeout, s_logger);
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

        final List<Object> info = getHostInfo();

        final StartupRoutingCommand cmd =
                new StartupRoutingCommand((Integer)info.get(0), (Long)info.get(1), (Long)info.get(2), (Long)info.get(4), (String)info.get(3), _hypervisorType,
                        RouterPrivateIpStrategy.HostLocal);
        cmd.setCpuSockets((Integer)info.get(5));
        fillNetworkInformation(cmd);
        _privateIp = cmd.getPrivateIpAddress();
        cmd.getHostDetails().putAll(getVersionStrings());
        cmd.setPool(_pool);
        cmd.setCluster(_clusterId);
        cmd.setGatewayIpAddress(_localGateway);
        cmd.setIqn(getIqn());

        StartupStorageCommand sscmd = null;
        try {

            KVMStoragePool localStoragePool = _storagePoolMgr.createStoragePool(_localStorageUUID, "localhost", -1, _localStoragePath, "", StoragePoolType.Filesystem);
            com.cloud.agent.api.StoragePoolInfo pi =
                    new com.cloud.agent.api.StoragePoolInfo(localStoragePool.getUuid(), cmd.getPrivateIpAddress(), _localStoragePath, _localStoragePath,
                            StoragePoolType.Filesystem, localStoragePool.getCapacity(), localStoragePool.getAvailable());

            sscmd = new StartupStorageCommand();
            sscmd.setPoolInfo(pi);
            sscmd.setGuid(pi.getUuid());
            sscmd.setDataCenter(_dcId);
            sscmd.setResourceType(Storage.StorageResourceType.STORAGE_POOL);
        } catch (CloudRuntimeException e) {
            s_logger.debug("Unable to initialize local storage pool: " + e);
        }

        if (sscmd != null) {
            return new StartupCommand[] {cmd, sscmd};
        } else {
            return new StartupCommand[] {cmd};
        }
    }

    private String getIqn() {
        try {
            final String textToFind = "InitiatorName=";

            Script iScsiAdmCmd = new Script(true, "grep", 0, s_logger);

            iScsiAdmCmd.add(textToFind);
            iScsiAdmCmd.add("/etc/iscsi/initiatorname.iscsi");

            OutputInterpreter.OneLineParser parser = new OutputInterpreter.OneLineParser();

            String result = iScsiAdmCmd.execute(parser);

            if (result != null) {
                return null;
            }

            String textFound = parser.getLine().trim();

            return textFound.substring(textToFind.length());
        }
        catch (Exception ex) {
            return null;
        }
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

    private HashMap<String, HostVmStateReportEntry> getHostVmStateReport() {
        final HashMap<String, HostVmStateReportEntry> vmStates = new HashMap<String, HostVmStateReportEntry>();
        Connect conn = null;

        if (_hypervisorType == HypervisorType.LXC) {
            try {
                conn = LibvirtConnection.getConnectionByType(HypervisorType.LXC.toString());
                vmStates.putAll(getHostVmStateReport(conn));
                conn = LibvirtConnection.getConnectionByType(HypervisorType.KVM.toString());
                vmStates.putAll(getHostVmStateReport(conn));
            } catch (LibvirtException e) {
                s_logger.debug("Failed to get connection: " + e.getMessage());
            }
        }

        if (_hypervisorType == HypervisorType.KVM) {
            try {
                conn = LibvirtConnection.getConnectionByType(HypervisorType.KVM.toString());
                vmStates.putAll(getHostVmStateReport(conn));
            } catch (LibvirtException e) {
                s_logger.debug("Failed to get connection: " + e.getMessage());
            }
        }

        return vmStates;
    }

    private HashMap<String, HostVmStateReportEntry> getHostVmStateReport(Connect conn) {
        final HashMap<String, HostVmStateReportEntry> vmStates = new HashMap<String, HostVmStateReportEntry>();

        String[] vms = null;
        int[] ids = null;

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

                DomainState ps = dm.getInfo().state;

                final PowerState state = convertToPowerState(ps);

                s_logger.trace("VM " + dm.getName() + ": powerstate = " + ps + "; vm state=" + state.toString());
                String vmName = dm.getName();

                // TODO : for XS/KVM (host-based resource), we require to remove
                // VM completely from host, for some reason, KVM seems to still keep
                // Stopped VM around, to work-around that, reporting only powered-on VM
                //
                if (state == PowerState.PowerOn)
                    vmStates.put(vmName, new HostVmStateReportEntry(state, conn.getHostName()));
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

                dm = conn.domainLookupByName(vms[i]);

                DomainState ps = dm.getInfo().state;
                final PowerState state = convertToPowerState(ps);
                String vmName = dm.getName();
                s_logger.trace("VM " + vmName + ": powerstate = " + ps + "; vm state=" + state.toString());

                // TODO : for XS/KVM (host-based resource), we require to remove
                // VM completely from host, for some reason, KVM seems to still keep
                // Stopped VM around, to work-around that, reporting only powered-on VM
                //
                if (state == PowerState.PowerOn)
                    vmStates.put(vmName, new HostVmStateReportEntry(state, conn.getHostName()));
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
        int cpuSockets = 0;
        String cap = null;
        try {
            final Connect conn = LibvirtConnection.getConnection();
            final NodeInfo hosts = conn.nodeInfo();
            speed = getCpuSpeed(hosts);

            cpuSockets = hosts.sockets;
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

        info.add((int)cpus);
        info.add(speed);
        info.add(ram);
        info.add(cap);
        long dom0ram = Math.min(ram / 10, 768 * 1024 * 1024L);// save a maximum
        // of 10% of
        // system ram or
        // 768M
        dom0ram = Math.max(dom0ram, _dom0MinMem);
        info.add(dom0ram);
        info.add(cpuSockets);
        s_logger.debug("cpus=" + cpus + ", speed=" + speed + ", ram=" + ram + ", dom0ram=" + dom0ram + ", cpu sockets=" + cpuSockets);

        return info;
    }

    protected static long getCpuSpeed(final NodeInfo nodeInfo) {
        try (final Reader reader = new FileReader(
                "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq")) {
            return Long.parseLong(IOUtils.toString(reader).trim()) / 1000;
        } catch (IOException | NumberFormatException e) {
            s_logger.warn("Could not read cpuinfo_max_freq");
            return nodeInfo.mhz;
        }
    }

    protected String rebootVM(Connect conn, String vmName) {
        Domain dm = null;
        String msg = null;
        try {
            dm = conn.domainLookupByName(vmName);
            String vmDef = dm.getXMLDesc(0);
            LibvirtDomainXMLParser parser = new LibvirtDomainXMLParser();
            parser.parseDomainXML(vmDef);
            for (InterfaceDef nic : parser.getInterfaces()) {
                if ((nic.getNetType() == guestNetType.BRIDGE) && (nic.getBrName().startsWith("cloudVirBr"))) {
                    try {
                        int vnetId = Integer.parseInt(nic.getBrName().replaceFirst("cloudVirBr", ""));
                        String pifName = getPif(_guestBridgeName);
                        String newBrName = "br" + pifName + "-" + vnetId;
                        vmDef = vmDef.replaceAll("'" + nic.getBrName() + "'", "'" + newBrName + "'");
                        s_logger.debug("VM bridge name is changed from " + nic.getBrName() + " to " + newBrName);
                    } catch (NumberFormatException e) {
                        continue;
                    }
                }
            }
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
        DomainState state = null;
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
                    dm = conn.domainLookupByName(vmName);
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

            if (state != DomainState.VIR_DOMAIN_SHUTOFF) {
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
            dm = conn.domainLookupByName(vmName);
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
                    while (dm.isActive() == 1 && (retry >= 0)) {
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
                    s_logger.warn("Timed out waiting for domain " + vmName + " to shutdown gracefully");
                    return Script.ERR_TIMEOUT;
                } else {
                    if (persist == 1) {
                        dm.undefine();
                    }
                }
            }
        } catch (LibvirtException e) {
            if (e.getMessage().contains("Domain not found")) {
                s_logger.debug("VM " + vmName + " doesn't exist, no need to stop it");
                return null;
            }
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

    protected Integer getVncPort(Connect conn, String vmName) throws LibvirtException {
        LibvirtDomainXMLParser parser = new LibvirtDomainXMLParser();
        Domain dm = null;
        try {
            dm = conn.domainLookupByName(vmName);
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
            dm = conn.domainLookupByName(vmName);
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

    boolean isGuestPVEnabled(String guestOSName) {
        if (guestOSName == null) {
            return false;
        }
        if (guestOSName.startsWith("Ubuntu") || guestOSName.startsWith("Fedora 13") || guestOSName.startsWith("Fedora 12") || guestOSName.startsWith("Fedora 11") ||
                guestOSName.startsWith("Fedora 10") || guestOSName.startsWith("Fedora 9") || guestOSName.startsWith("CentOS 5.3") || guestOSName.startsWith("CentOS 5.4") ||
                guestOSName.startsWith("CentOS 5.5") || guestOSName.startsWith("CentOS") || guestOSName.startsWith("Fedora") ||
                guestOSName.startsWith("Red Hat Enterprise Linux 5.3") || guestOSName.startsWith("Red Hat Enterprise Linux 5.4") ||
                guestOSName.startsWith("Red Hat Enterprise Linux 5.5") || guestOSName.startsWith("Red Hat Enterprise Linux 6") || guestOSName.startsWith("Debian GNU/Linux") ||
                guestOSName.startsWith("FreeBSD 10") || guestOSName.startsWith("Oracle Enterprise Linux") || guestOSName.startsWith("Other PV")) {
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

    private DiskDef.diskBus getGuestDiskModel(String platformEmulator) {
        if (isGuestPVEnabled(platformEmulator)) {
            return DiskDef.diskBus.VIRTIO;
        } else {
            return DiskDef.diskBus.IDE;
        }
    }

    private void cleanupVMNetworks(Connect conn, List<InterfaceDef> nics) {
        if (nics != null) {
            for (InterfaceDef nic : nics) {
                for (VifDriver vifDriver : getAllVifDrivers()) {
                    vifDriver.unplug(nic);
                }
            }
        }
    }

    public Domain getDomain(Connect conn, String vmName) throws LibvirtException {
        return conn.domainLookupByName(vmName);
    }

    protected List<InterfaceDef> getInterfaces(Connect conn, String vmName) {
        LibvirtDomainXMLParser parser = new LibvirtDomainXMLParser();
        Domain dm = null;
        try {
            dm = conn.domainLookupByName(vmName);
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

    public List<DiskDef> getDisks(Connect conn, String vmName) {
        LibvirtDomainXMLParser parser = new LibvirtDomainXMLParser();
        Domain dm = null;
        try {
            dm = conn.domainLookupByName(vmName);
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

    private List<VmDiskStatsEntry> getVmDiskStat(Connect conn, String vmName) throws LibvirtException {
        Domain dm = null;
        try {
            dm = getDomain(conn, vmName);

            List<VmDiskStatsEntry> stats = new ArrayList<VmDiskStatsEntry>();

            List<DiskDef> disks = getDisks(conn, vmName);

            for (DiskDef disk : disks) {
                if (disk.getDeviceType() != deviceType.DISK)
                    break;
                DomainBlockStats blockStats = dm.blockStats(disk.getDiskLabel());
                String path = disk.getDiskPath(); // for example, path = /mnt/pool_uuid/disk_path/
                String diskPath = null;
                if (path != null) {
                    String[] token = path.split("/");
                    if (token.length > 3) {
                        diskPath = token[3];
                        VmDiskStatsEntry stat = new VmDiskStatsEntry(vmName, diskPath, blockStats.wr_req, blockStats.rd_req, blockStats.wr_bytes, blockStats.rd_bytes);
                        stats.add(stat);
                    }
                }
            }

            return stats;
        } finally {
            if (dm != null) {
                dm.free();
            }
        }
    }

    private class VmStats {
        long _usedTime;
        long _tx;
        long _rx;
        long _ioRead;
        long _ioWrote;
        long _bytesRead;
        long _bytesWrote;
        Calendar _timestamp;
    }

    VmStatsEntry getVmStat(Connect conn, String vmName) throws LibvirtException {
        Domain dm = null;
        try {
            dm = getDomain(conn, vmName);
            DomainInfo info = dm.getInfo();

            VmStatsEntry stats = new VmStatsEntry();
            stats.setNumCPUs(info.nrVirtCpu);
            stats.setEntityType("vm");

            /* get cpu utilization */
            VmStats oldStats = null;

            Calendar now = Calendar.getInstance();

            oldStats = _vmStats.get(vmName);

            long elapsedTime = 0;
            if (oldStats != null) {
                elapsedTime = now.getTimeInMillis() - oldStats._timestamp.getTimeInMillis();
                double utilization = (info.cpuTime - oldStats._usedTime) / ((double)elapsedTime * 1000000);

                NodeInfo node = conn.nodeInfo();
                utilization = utilization / node.cpus;
                if (utilization > 0) {
                    stats.setCPUUtilization(utilization * 100);
                }
            }

            /* get network stats */

            List<InterfaceDef> vifs = getInterfaces(conn, vmName);
            long rx = 0;
            long tx = 0;
            for (InterfaceDef vif : vifs) {
                DomainInterfaceStats ifStats = dm.interfaceStats(vif.getDevName());
                rx += ifStats.rx_bytes;
                tx += ifStats.tx_bytes;
            }

            if (oldStats != null) {
                double deltarx = rx - oldStats._rx;
                if (deltarx > 0)
                    stats.setNetworkReadKBs(deltarx / 1024);
                double deltatx = tx - oldStats._tx;
                if (deltatx > 0)
                    stats.setNetworkWriteKBs(deltatx / 1024);
            }

            /* get disk stats */
            List<DiskDef> disks = getDisks(conn, vmName);
            long io_rd = 0;
            long io_wr = 0;
            long bytes_rd = 0;
            long bytes_wr = 0;
            for (DiskDef disk : disks) {
                DomainBlockStats blockStats = dm.blockStats(disk.getDiskLabel());
                io_rd += blockStats.rd_req;
                io_wr += blockStats.wr_req;
                bytes_rd += blockStats.rd_bytes;
                bytes_wr += blockStats.wr_bytes;
            }

            if (oldStats != null) {
                long deltaiord = io_rd - oldStats._ioRead;
                if (deltaiord > 0)
                    stats.setDiskReadIOs(deltaiord);
                long deltaiowr = io_wr - oldStats._ioWrote;
                if (deltaiowr > 0)
                    stats.setDiskWriteIOs(deltaiowr);
                double deltabytesrd = bytes_rd - oldStats._bytesRead;
                if (deltabytesrd > 0)
                    stats.setDiskReadKBs(deltabytesrd / 1024);
                double deltabyteswr = bytes_wr - oldStats._bytesWrote;
                if (deltabyteswr > 0)
                    stats.setDiskWriteKBs(deltabyteswr / 1024);
            }

            /* save to Hashmap */
            VmStats newStat = new VmStats();
            newStat._usedTime = info.cpuTime;
            newStat._rx = rx;
            newStat._tx = tx;
            newStat._ioRead = io_rd;
            newStat._ioWrote = io_wr;
            newStat._bytesRead = bytes_rd;
            newStat._bytesWrote = bytes_wr;
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
        if (!_canBridgeFirewall) {
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

    protected boolean default_network_rules(Connect conn, String vmName, NicTO nic, Long vmId, String secIpStr) {
        if (!_canBridgeFirewall) {
            return false;
        }

        List<InterfaceDef> intfs = getInterfaces(conn, vmName);
        if (intfs.size() == 0 || intfs.size() < nic.getDeviceId()) {
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

    protected boolean post_default_network_rules(Connect conn, String vmName, NicTO nic, Long vmId, InetAddress dhcpServerIp, String hostIp, String hostMacAddr) {
        if (!_canBridgeFirewall) {
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

    protected boolean default_network_rules_for_systemvm(Connect conn, String vmName) {
        if (!_canBridgeFirewall) {
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

    private boolean add_network_rules(String vmName, String vmId, String guestIP, String sig, String seq, String mac, String rules, String vif, String brname,
            String secIps) {
        if (!_canBridgeFirewall) {
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
        if (newRules != null && !newRules.isEmpty()) {
            cmd.add("--rules", newRules);
        }
        String result = cmd.execute();
        if (result != null) {
            return false;
        }
        return true;
    }

    private boolean network_rules_vmSecondaryIp(Connect conn, String vmName, String secIp, String action) {

        if (!_canBridgeFirewall) {
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
        if (!_canBridgeFirewall) {
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
                states.put(log[0], new Pair<Long, Long>(Long.parseLong(log[1]), Long.parseLong(log[5])));
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

    static Pair<Double, Double> getNicStats(String nicName) {
        return new Pair<Double, Double>(readDouble(nicName, "rx_bytes"), readDouble(nicName, "tx_bytes"));
    }

    static double readDouble(String nicName, String fileName) {
        final String path = "/sys/class/net/" + nicName + "/statistics/" + fileName;
        try {
            return Double.parseDouble(FileUtils.readFileToString(new File(path)));
        } catch (IOException ioe) {
            s_logger.warn("Failed to read the " + fileName + " for " + nicName + " from " + path, ioe);
            return 0.0;
        }
    }

    private Answer execute(NetworkRulesSystemVmCommand cmd) {
        boolean success = false;
        Connect conn;
        try {
            conn = LibvirtConnection.getConnectionByVmName(cmd.getVmName());
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
            conn = LibvirtConnection.getConnectionByVmName(cmd.getVmName());
            success = network_rules_vmSecondaryIp(conn, cmd.getVmName(), cmd.getVmSecIp(), cmd.getAction());
        } catch (LibvirtException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return new Answer(cmd, success, "");
    }

    private String prettyVersion(long version) {
        long major = version / 1000000;
        long minor = version % 1000000 / 1000;
        long release = version % 1000000 % 1000;
        return major + "." + minor + "." + release;
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

    public HypervisorType getHypervisorType(){
        return _hypervisorType;
    }

    private boolean checkCgroups(){
        final Script command = new Script(_setupCgroupPath, 5 * 1000, s_logger);
        String result;
        result = command.execute();
        if (result != null) {
            s_logger.debug("cgroup check failed:" + result);
            return false;
        }
        return true;
    }

    public String mapRbdDevice(KVMPhysicalDisk disk){
        KVMStoragePool pool = disk.getPool();
        //Check if rbd image is already mapped
        String[] splitPoolImage = disk.getPath().split("/");
        String device = Script.runSimpleBashScript("rbd showmapped | grep \""+splitPoolImage[0]+"[ ]*"+splitPoolImage[1]+"\" | grep -o \"[^ ]*[ ]*$\"");
        if(device == null) {
            //If not mapped, map and return mapped device
            Script.runSimpleBashScript("rbd map " + disk.getPath() + " --id " + pool.getAuthUserName());
            device = Script.runSimpleBashScript("rbd showmapped | grep \""+splitPoolImage[0]+"[ ]*"+splitPoolImage[1]+"\" | grep -o \"[^ ]*[ ]*$\"");
        }
        return device;
    }
}
