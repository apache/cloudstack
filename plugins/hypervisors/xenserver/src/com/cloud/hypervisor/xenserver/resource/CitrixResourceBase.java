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
package com.cloud.hypervisor.xenserver.resource;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import javax.ejb.Local;
import javax.naming.ConfigurationException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.cloudstack.storage.command.StorageSubSystemCommand;
import org.apache.cloudstack.storage.to.TemplateObjectTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.cloud.agent.IAgentControl;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.AttachIsoCommand;
import com.cloud.agent.api.AttachVolumeAnswer;
import com.cloud.agent.api.AttachVolumeCommand;
import com.cloud.agent.api.CheckHealthAnswer;
import com.cloud.agent.api.CheckHealthCommand;
import com.cloud.agent.api.CheckNetworkAnswer;
import com.cloud.agent.api.CheckNetworkCommand;
import com.cloud.agent.api.CheckOnHostAnswer;
import com.cloud.agent.api.CheckOnHostCommand;
import com.cloud.agent.api.CheckVirtualMachineAnswer;
import com.cloud.agent.api.CheckVirtualMachineCommand;
import com.cloud.agent.api.CleanupNetworkRulesCmd;
import com.cloud.agent.api.ClusterVMMetaDataSyncAnswer;
import com.cloud.agent.api.ClusterVMMetaDataSyncCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.CreateStoragePoolCommand;
import com.cloud.agent.api.CreateVMSnapshotAnswer;
import com.cloud.agent.api.CreateVMSnapshotCommand;
import com.cloud.agent.api.DeleteStoragePoolCommand;
import com.cloud.agent.api.DeleteVMSnapshotAnswer;
import com.cloud.agent.api.DeleteVMSnapshotCommand;
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
import com.cloud.agent.api.MigrateAnswer;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.agent.api.ModifySshKeysCommand;
import com.cloud.agent.api.ModifyStoragePoolAnswer;
import com.cloud.agent.api.ModifyStoragePoolCommand;
import com.cloud.agent.api.NetworkRulesSystemVmCommand;
import com.cloud.agent.api.NetworkRulesVmSecondaryIpCommand;
import com.cloud.agent.api.OvsCreateGreTunnelAnswer;
import com.cloud.agent.api.OvsCreateGreTunnelCommand;
import com.cloud.agent.api.OvsCreateTunnelAnswer;
import com.cloud.agent.api.OvsCreateTunnelCommand;
import com.cloud.agent.api.OvsDeleteFlowCommand;
import com.cloud.agent.api.OvsDestroyBridgeCommand;
import com.cloud.agent.api.OvsDestroyTunnelCommand;
import com.cloud.agent.api.OvsFetchInterfaceAnswer;
import com.cloud.agent.api.OvsFetchInterfaceCommand;
import com.cloud.agent.api.OvsSetTagAndFlowAnswer;
import com.cloud.agent.api.OvsSetTagAndFlowCommand;
import com.cloud.agent.api.OvsSetupBridgeCommand;
import com.cloud.agent.api.OvsVpcPhysicalTopologyConfigCommand;
import com.cloud.agent.api.OvsVpcRoutingPolicyConfigCommand;
import com.cloud.agent.api.PerformanceMonitorAnswer;
import com.cloud.agent.api.PerformanceMonitorCommand;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.PingRoutingCommand;
import com.cloud.agent.api.PingRoutingWithNwGroupsCommand;
import com.cloud.agent.api.PingRoutingWithOvsCommand;
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
import com.cloud.agent.api.RevertToVMSnapshotAnswer;
import com.cloud.agent.api.RevertToVMSnapshotCommand;
import com.cloud.agent.api.ScaleVmAnswer;
import com.cloud.agent.api.ScaleVmCommand;
import com.cloud.agent.api.SecurityGroupRuleAnswer;
import com.cloud.agent.api.SecurityGroupRulesCmd;
import com.cloud.agent.api.SetupAnswer;
import com.cloud.agent.api.SetupCommand;
import com.cloud.agent.api.SetupGuestNetworkCommand;
import com.cloud.agent.api.StartAnswer;
import com.cloud.agent.api.StartCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.agent.api.StartupStorageCommand;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.agent.api.UnPlugNicAnswer;
import com.cloud.agent.api.UnPlugNicCommand;
import com.cloud.agent.api.UpdateHostPasswordCommand;
import com.cloud.agent.api.UpgradeSnapshotCommand;
import com.cloud.agent.api.VgpuTypesInfo;
import com.cloud.agent.api.VmStatsEntry;
import com.cloud.agent.api.check.CheckSshAnswer;
import com.cloud.agent.api.check.CheckSshCommand;
import com.cloud.agent.api.proxy.CheckConsoleProxyLoadCommand;
import com.cloud.agent.api.proxy.ConsoleProxyLoadAnswer;
import com.cloud.agent.api.proxy.WatchConsoleProxyLoadCommand;
import com.cloud.agent.api.routing.IpAssocCommand;
import com.cloud.agent.api.routing.IpAssocVpcCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.routing.SetNetworkACLCommand;
import com.cloud.agent.api.routing.SetSourceNatCommand;
import com.cloud.agent.api.storage.CreateAnswer;
import com.cloud.agent.api.storage.CreateCommand;
import com.cloud.agent.api.storage.DestroyCommand;
import com.cloud.agent.api.storage.PrimaryStorageDownloadAnswer;
import com.cloud.agent.api.storage.PrimaryStorageDownloadCommand;
import com.cloud.agent.api.storage.ResizeVolumeAnswer;
import com.cloud.agent.api.storage.ResizeVolumeCommand;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.GPUDeviceTO;
import com.cloud.agent.api.to.IpAddressTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.agent.resource.virtualnetwork.VirtualRouterDeployer;
import com.cloud.agent.resource.virtualnetwork.VirtualRoutingResource;
import com.cloud.exception.InternalErrorException;
import com.cloud.host.Host.Type;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.xenserver.resource.wrapper.CitrixRequestWrapper;
import com.cloud.network.Networks;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.IsolationType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetworkSetupInfo;
import com.cloud.resource.ServerResource;
import com.cloud.resource.hypervisor.HypervisorResource;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.resource.StorageSubsystemCommandHandler;
import com.cloud.storage.resource.StorageSubsystemCommandHandlerBase;
import com.cloud.storage.template.TemplateProp;
import com.cloud.template.VirtualMachineTemplate.BootloaderType;
import com.cloud.utils.ExecutionResult;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.StringUtils;
import com.cloud.utils.Ternary;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.ssh.SSHCmdHelper;
import com.cloud.utils.ssh.SshHelper;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.PowerState;
import com.cloud.vm.snapshot.VMSnapshot;
import com.trilead.ssh2.SCPClient;
import com.xensource.xenapi.Bond;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Console;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.HostCpu;
import com.xensource.xenapi.HostMetrics;
import com.xensource.xenapi.Network;
import com.xensource.xenapi.PBD;
import com.xensource.xenapi.PIF;
import com.xensource.xenapi.Pool;
import com.xensource.xenapi.SR;
import com.xensource.xenapi.Session;
import com.xensource.xenapi.Task;
import com.xensource.xenapi.Types;
import com.xensource.xenapi.Types.BadServerResponse;
import com.xensource.xenapi.Types.VmPowerState;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.VBD;
import com.xensource.xenapi.VDI;
import com.xensource.xenapi.VGPU;
import com.xensource.xenapi.VIF;
import com.xensource.xenapi.VLAN;
import com.xensource.xenapi.VM;
import com.xensource.xenapi.XenAPIObject;

/**
 * CitrixResourceBase encapsulates the calls to the XenServer Xapi process
 * to perform the required functionalities for CloudStack.
 *
 * ==============>  READ THIS  <==============
 * Because the XenServer objects can expire when the session expires, we cannot
 * keep any of the actual XenServer objects in this class.  The only
 * thing that is constant is the UUID of the XenServer objects but not the
 * objects themselves!  This is very important before you do any changes in
 * this code here.
 *
 */
@Local(value = ServerResource.class)
public abstract class CitrixResourceBase implements ServerResource, HypervisorResource, VirtualRouterDeployer {
    private static final Logger s_logger = Logger.getLogger(CitrixResourceBase.class);
    protected static final XenServerConnectionPool ConnPool = XenServerConnectionPool.getInstance();
    protected String _name;
    protected String _username;
    protected Queue<String> _password = new LinkedList<String>();
    protected final int _retry = 100;
    protected final int _sleep = 10000;
    protected long _dcId;
    protected String _pod;
    protected String _cluster;
    protected String _privateNetworkName;
    protected String _linkLocalPrivateNetworkName;
    protected String _publicNetworkName;
    protected String _storageNetworkName1;
    protected String _storageNetworkName2;
    protected String _guestNetworkName;
    protected int _wait;
    protected int _migratewait;
    protected String _instance; //instance name (default is usually "VM")
    static final Random Rand = new Random(System.currentTimeMillis());
    protected boolean _securityGroupEnabled;

    protected IAgentControl _agentControl;

    final int _maxWeight = 256;
    protected int _heartbeatTimeout = 120;
    protected int _heartbeatInterval = 60;
    protected final XsHost _host = new XsHost();

    // Guest and Host Performance Statistics
    protected String _consolidationFunction = "AVERAGE";
    protected int _pollingIntervalInSeconds = 60;

    //Hypervisor specific params with generic value, may need to be overridden for specific versions
    long _xsMemoryUsed = 128 * 1024 * 1024L; // xenserver hypervisor used 128 M
    double _xsVirtualizationFactor = 63.0 / 64.0;  // 1 - virtualization overhead

    //static min values for guests on xenserver
    private static final long mem_128m = 134217728L;

    protected boolean _canBridgeFirewall = false;
    protected boolean _isOvs = false;
    protected List<VIF> _tmpDom0Vif = new ArrayList<VIF>();
    protected StorageSubsystemCommandHandler storageHandler;
    protected int _maxNics = 7;

    protected VirtualRoutingResource _vrResource;

    public enum SRType {
        NFS, LVM, ISCSI, ISO, LVMOISCSI, LVMOHBA, EXT, FILE;

        String _str;

        private SRType() {
            _str = super.toString().toLowerCase();
        }

        @Override
        public String toString() {
            return _str;
        }

        public boolean equals(final String type) {
            return _str.equalsIgnoreCase(type);
        }
    }

    protected static final HashMap<VmPowerState, PowerState> s_powerStatesTable;
    static {
        s_powerStatesTable = new HashMap<VmPowerState, PowerState>();
        s_powerStatesTable.put(VmPowerState.HALTED, PowerState.PowerOff);
        s_powerStatesTable.put(VmPowerState.PAUSED, PowerState.PowerOff);
        s_powerStatesTable.put(VmPowerState.RUNNING, PowerState.PowerOn);
        s_powerStatesTable.put(VmPowerState.SUSPENDED, PowerState.PowerOff);
        s_powerStatesTable.put(VmPowerState.UNRECOGNIZED, PowerState.PowerUnknown);
    }

    public XsHost getHost() {
        return _host;
    }

    public boolean isOvs() {
        return _isOvs;
    }

    public void setIsOvs(final boolean isOvs) {
        _isOvs = isOvs;
    }

    public boolean isSecurityGroupEnabled() {
        return _securityGroupEnabled;
    }

    public void setCanBridgeFirewall(final boolean canBridgeFirewall) {
        _canBridgeFirewall = canBridgeFirewall;
    }

    public boolean canBridgeFirewall() {
        return _canBridgeFirewall;
    }

    public boolean canBridgeFirewall(final Connection conn) {
        return Boolean.valueOf(callHostPlugin(conn, "vmops", "can_bridge_firewall", "host_uuid", _host.getUuid(), "instance", _instance));
    }

    private static boolean isAlienVm(final VM vm, final Connection conn) throws XenAPIException, XmlRpcException {
        // TODO : we need a better way to tell whether or not the VM belongs to CloudStack
        final String vmName = vm.getNameLabel(conn);
        if (vmName.matches("^[ivs]-\\d+-.+")) {
            return false;
        }

        return true;
    }

    public boolean cleanupHaltedVms(final Connection conn) throws XenAPIException, XmlRpcException {
        final Host host = Host.getByUuid(conn, _host.getUuid());
        final Map<VM, VM.Record> vms = VM.getAllRecords(conn);
        boolean success = true;
        if(vms != null && !vms.isEmpty()) {
            for (final Map.Entry<VM, VM.Record> entry : vms.entrySet()) {
                final VM vm = entry.getKey();
                final VM.Record vmRec = entry.getValue();
                if (vmRec.isATemplate || vmRec.isControlDomain) {
                    continue;
                }

                if (VmPowerState.HALTED.equals(vmRec.powerState) && vmRec.affinity.equals(host) && !isAlienVm(vm, conn)) {
                    try {
                        vm.destroy(conn);
                    } catch (final Exception e) {
                        s_logger.warn("Catch Exception " + e.getClass().getName() + ": unable to destroy VM " + vmRec.nameLabel + " due to ", e);
                        success = false;
                    }
                }
            }
        }
        return success;
    }

    public boolean isRefNull(final XenAPIObject object) {
        return object == null || object.toWireString().equals("OpaqueRef:NULL") || object.toWireString().equals("<not in database>");
    }

    @Override
    public void disconnected() {
    }

    protected boolean pingdomr(final Connection conn, final String host, final String port) {
        String status;
        status = callHostPlugin(conn, "vmops", "pingdomr", "host", host, "port", port);

        if (status == null || status.isEmpty()) {
            return false;
        }

        return true;

    }

    public boolean pingXAPI() {
        final Connection conn = getConnection();
        try {
            final Host host = Host.getByUuid(conn, _host.getUuid());
            if( !host.getEnabled(conn) ) {
                s_logger.debug("Host " + _host.getIp() + " is not enabled!");
                return false;
            }
        } catch (final Exception e) {
            s_logger.debug("cannot get host enabled status, host " + _host.getIp() + " due to " + e.toString(),  e);
            return false;
        }
        try {
            callHostPlugin(conn, "echo", "main");
        } catch (final Exception e) {
            s_logger.debug("cannot ping host " + _host.getIp() + " due to " + e.toString(),  e);
            return false;
        }
        return true;
    }


    protected String logX(final XenAPIObject obj, final String msg) {
        return new StringBuilder("Host ").append(_host.getIp()).append(" ").append(obj.toWireString()).append(": ").append(msg).toString();
    }

    @Override
    public Answer executeRequest(final Command cmd) {

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        try {
            final Answer answer = wrapper.execute(cmd, this);
            return answer;
        } catch (final Exception e) {
            // Ignore it for now. Just removing the command that have already been
            // replaced by the new code.
        }

        final Class<? extends Command> clazz = cmd.getClass();

        if (cmd instanceof NetworkElementCommand) {
            return _vrResource.executeRequest((NetworkElementCommand)cmd);
        } else if (clazz == OvsCreateGreTunnelCommand.class) {
            return execute((OvsCreateGreTunnelCommand)cmd);
        } else if (clazz == OvsDeleteFlowCommand.class) {
            return execute((OvsDeleteFlowCommand)cmd);
        } else if (clazz == OvsVpcPhysicalTopologyConfigCommand.class) {
            return execute((OvsVpcPhysicalTopologyConfigCommand) cmd);
        } else if (clazz == OvsVpcRoutingPolicyConfigCommand.class) {
            return execute((OvsVpcRoutingPolicyConfigCommand) cmd);
        } else if (clazz == CleanupNetworkRulesCmd.class) {
            return execute((CleanupNetworkRulesCmd)cmd);
        } else if (clazz == NetworkRulesSystemVmCommand.class) {
            return execute((NetworkRulesSystemVmCommand)cmd);
        } else if (clazz == OvsCreateTunnelCommand.class) {
            return execute((OvsCreateTunnelCommand)cmd);
        } else if (clazz == OvsSetupBridgeCommand.class) {
            return execute((OvsSetupBridgeCommand)cmd);
        } else if (clazz == OvsDestroyBridgeCommand.class) {
            return execute((OvsDestroyBridgeCommand)cmd);
        } else if (clazz == OvsDestroyTunnelCommand.class) {
            return execute((OvsDestroyTunnelCommand)cmd);
        } else if (clazz == UpdateHostPasswordCommand.class) {
            return execute((UpdateHostPasswordCommand)cmd);
        } else if (cmd instanceof ClusterVMMetaDataSyncCommand) {
            return execute((ClusterVMMetaDataSyncCommand)cmd);
        } else if (clazz == CheckNetworkCommand.class) {
            return execute((CheckNetworkCommand)cmd);
        } else if (clazz == PlugNicCommand.class) {
            return execute((PlugNicCommand)cmd);
        } else if (clazz == UnPlugNicCommand.class) {
            return execute((UnPlugNicCommand) cmd);
        } else if (cmd instanceof StorageSubSystemCommand) {
            return storageHandler.handleStorageCommands((StorageSubSystemCommand) cmd);
        } else if (clazz == CreateVMSnapshotCommand.class) {
            return execute((CreateVMSnapshotCommand) cmd);
        } else if (clazz == DeleteVMSnapshotCommand.class) {
            return execute((DeleteVMSnapshotCommand) cmd);
        } else if (clazz == RevertToVMSnapshotCommand.class) {
            return execute((RevertToVMSnapshotCommand) cmd);
        } else if (clazz == NetworkRulesVmSecondaryIpCommand.class) {
            return execute((NetworkRulesVmSecondaryIpCommand) cmd);
        } else if (clazz == ScaleVmCommand.class) {
            return execute((ScaleVmCommand)cmd);
        } else if (clazz == PvlanSetupCommand.class) {
            return execute((PvlanSetupCommand)cmd);
        } else if (clazz == PerformanceMonitorCommand.class) {
            return execute((PerformanceMonitorCommand)cmd);
        } else {
            return Answer.createUnsupportedCommandAnswer(cmd);
        }
    }

    @Override
    public ExecutionResult executeInVR(final String routerIP, final String script, final String args, final int timeout) {
        Pair<Boolean, String> result;
        String cmdline = "/opt/cloud/bin/router_proxy.sh " + script + " " + routerIP + " " + args;
        // semicolon need to be escape for bash
        cmdline = cmdline.replaceAll(";", "\\\\;");
        try {
            s_logger.debug("Executing command in VR: " + cmdline);
            result = SshHelper.sshExecute(_host.getIp(), 22, _username, null, _password.peek(), cmdline,
                    60000, 60000, timeout * 1000);
        } catch (final Exception e) {
            return new ExecutionResult(false, e.getMessage());
        }
        return new ExecutionResult(result.first(), result.second());
    }

    @Override
    public ExecutionResult executeInVR(final String routerIP, final String script, final String args) {
        // Timeout is 120 seconds by default
        return executeInVR(routerIP, script, args, 120);
    }

    @Override
    public ExecutionResult createFileInVR(final String routerIp, final String path, final String filename, final String content) {
        final Connection conn = getConnection();
        final String hostPath = "/tmp/";

        s_logger.debug("Copying VR with ip " + routerIp +" config file into host "+ _host.getIp() );
        try {
            SshHelper.scpTo(_host.getIp(), 22, _username, null, _password.peek(), hostPath, content.getBytes(Charset.defaultCharset()), filename, null);
        } catch (final Exception e) {
            s_logger.warn("scp VR config file into host " + _host.getIp() + " failed with exception " + e.getMessage().toString());
        }

        final String rc = callHostPlugin(conn, "vmops", "createFileInDomr", "domrip", routerIp, "srcfilepath", hostPath + filename, "dstfilepath", path);
        s_logger.debug ("VR Config file " + filename + " got created in VR, ip " + routerIp + " with content \n" + content);

        return new ExecutionResult(rc.startsWith("succ#"), rc.substring(5));
    }

    @Override
    public ExecutionResult prepareCommand(final NetworkElementCommand cmd) {
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
        } else if (cmd instanceof SetNetworkACLCommand) {
            return prepareNetworkElementCommand((SetNetworkACLCommand)cmd);
        }
        return new ExecutionResult(true, null);
    }

    @Override
    public ExecutionResult cleanupCommand(final NetworkElementCommand cmd) {
        if (cmd instanceof IpAssocCommand && !(cmd instanceof IpAssocVpcCommand)) {
            return cleanupNetworkElementCommand((IpAssocCommand)cmd);
        }
        return new ExecutionResult(true, null);
    }

    private Answer execute(final PerformanceMonitorCommand cmd) {
        final Connection conn = getConnection();
        final String perfMon = getPerfMon(conn, cmd.getParams(), cmd.getWait());
        if (perfMon == null) {
            return new PerformanceMonitorAnswer(cmd, false, perfMon);
        } else {
            return new PerformanceMonitorAnswer(cmd, true, perfMon);
        }
    }

    private String getPerfMon(final Connection conn, final Map<String, String> params,
            final int wait) {
        String result = null;
        try {
            result = callHostPluginAsync(conn, "vmopspremium", "asmonitor", 60,
                    params);
            if (result != null) {
                return result;
            }
        } catch (final Exception e) {
            s_logger.error("Can not get performance monitor for AS due to ", e);
        }
        return null;
    }

    protected String callHostPluginAsync(final Connection conn, final String plugin,
            final String cmd, final int wait, final Map<String, String> params) {
        final int timeout = wait * 1000;
        final Map<String, String> args = new HashMap<String, String>();
        Task task = null;
        try {
            for (final Map.Entry< String, String > entry  : params.entrySet()) {
                args.put(entry.getKey(), entry.getValue());
            }
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("callHostPlugin executing for command " + cmd
                        + " with " + getArgsString(args));
            }
            final Host host = Host.getByUuid(conn, _host.getUuid());
            task = host.callPluginAsync(conn, plugin, cmd, args);
            // poll every 1 seconds
            waitForTask(conn, task, 1000, timeout);
            checkForSuccess(conn, task);
            final String result = task.getResult(conn);
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("callHostPlugin Result: " + result);
            }
            return result.replace("<value>", "").replace("</value>", "")
                    .replace("\n", "");
        } catch (final Types.HandleInvalid e) {
            s_logger.warn("callHostPlugin failed for cmd: " + cmd
                    + " with args " + getArgsString(args)
                    + " due to HandleInvalid clazz:" + e.clazz + ", handle:"
                    + e.handle);
        } catch (final Exception e) {
            s_logger.warn(
                    "callHostPlugin failed for cmd: " + cmd + " with args "
                            + getArgsString(args) + " due to " + e.toString(),
                            e);
        } finally {
            if (task != null) {
                try {
                    task.destroy(conn);
                } catch (final Exception e1) {
                    s_logger.debug("unable to destroy task(" + task.toString() + ") on host(" + _host.getUuid() + ") due to " +  e1.toString());
                }
            }
        }
        return null;
    }

    protected void scaleVM(final Connection conn, final VM vm, final VirtualMachineTO vmSpec, final Host host) throws XenAPIException, XmlRpcException {

        final Long staticMemoryMax = vm.getMemoryStaticMax(conn);
        final Long staticMemoryMin = vm.getMemoryStaticMin(conn);
        final Long newDynamicMemoryMin = vmSpec.getMinRam();
        final Long newDynamicMemoryMax = vmSpec.getMaxRam();
        if (staticMemoryMin > newDynamicMemoryMin || newDynamicMemoryMax > staticMemoryMax) {
            throw new CloudRuntimeException("Cannot scale up the vm because of memory constraint violation: " + "0 <= memory-static-min(" + staticMemoryMin +
                    ") <= memory-dynamic-min(" + newDynamicMemoryMin + ") <= memory-dynamic-max(" + newDynamicMemoryMax + ") <= memory-static-max(" + staticMemoryMax + ")");
        }

        vm.setMemoryDynamicRange(conn, newDynamicMemoryMin, newDynamicMemoryMax);
        vm.setVCPUsNumberLive(conn, (long)vmSpec.getCpus());

        final Integer speed = vmSpec.getMinSpeed();
        if (speed != null) {

            int cpuWeight = _maxWeight; //cpu_weight

            // weight based allocation

            cpuWeight = (int)(speed * 0.99 / _host.getSpeed() * _maxWeight);
            if (cpuWeight > _maxWeight) {
                cpuWeight = _maxWeight;
            }

            if (vmSpec.getLimitCpuUse()) {
                long utilization = 0; // max CPU cap, default is unlimited
                utilization = (int)(vmSpec.getMaxSpeed() * 0.99 * vmSpec.getCpus() / _host.getSpeed() * 100);
                //vm.addToVCPUsParamsLive(conn, "cap", Long.toString(utilization)); currently xenserver doesnot support Xapi to add VCPUs params live.
                callHostPlugin(conn, "vmops", "add_to_VCPUs_params_live", "key", "cap", "value", Long.toString(utilization), "vmname", vmSpec.getName());
            }
            //vm.addToVCPUsParamsLive(conn, "weight", Integer.toString(cpuWeight));
            callHostPlugin(conn, "vmops", "add_to_VCPUs_params_live", "key", "weight", "value", Integer.toString(cpuWeight), "vmname", vmSpec.getName());
        }
    }

    public ScaleVmAnswer execute(final ScaleVmCommand cmd) {
        final VirtualMachineTO vmSpec = cmd.getVirtualMachine();
        final String vmName = vmSpec.getName();
        try {
            final Connection conn = getConnection();
            final Set<VM> vms = VM.getByNameLabel(conn, vmName);
            final Host host = Host.getByUuid(conn, _host.getUuid());

            // If DMC is not enable then don't execute this command.
            if (!isDmcEnabled(conn, host)) {
                throw new CloudRuntimeException("Unable to scale the vm: " + vmName + " as DMC - Dynamic memory control is not enabled for the XenServer:" + _host.getUuid() +
                        " ,check your license and hypervisor version.");
            }

            // stop vm which is running on this host or is in halted state
            final Iterator<VM> iter = vms.iterator();
            while (iter.hasNext()) {
                final VM vm = iter.next();
                final VM.Record vmr = vm.getRecord(conn);

                if (vmr.powerState == VmPowerState.HALTED ||
                        vmr.powerState == VmPowerState.RUNNING && !isRefNull(vmr.residentOn) && !vmr.residentOn.getUuid(conn).equals(_host.getUuid())) {
                    iter.remove();
                }
            }

            if (vms.size() == 0) {
                s_logger.info("No running VM " + vmName + " exists on XenServer" + _host.getUuid());
                return new ScaleVmAnswer(cmd, false, "VM does not exist");
            }

            for (final VM vm : vms) {
                vm.getRecord(conn);
                try {
                    scaleVM(conn, vm, vmSpec, host);
                } catch (final Exception e) {
                    final String msg = "Catch exception " + e.getClass().getName() + " when scaling VM:" + vmName + " due to " + e.toString();
                    s_logger.debug(msg);
                    return new ScaleVmAnswer(cmd, false, msg);
                }

            }
            final String msg = "scaling VM " + vmName + " is successful on host " + host;
            s_logger.debug(msg);
            return new ScaleVmAnswer(cmd, true, msg);

        } catch (final XenAPIException e) {
            final String msg = "Upgrade Vm " + vmName + " fail due to " + e.toString();
            s_logger.warn(msg, e);
            return new ScaleVmAnswer(cmd, false, msg);
        } catch (final XmlRpcException e) {
            final String msg = "Upgrade Vm " + vmName + " fail due to " + e.getMessage();
            s_logger.warn(msg, e);
            return new ScaleVmAnswer(cmd, false, msg);
        } catch (final Exception e) {
            final String msg = "Unable to upgrade " + vmName + " due to " + e.getMessage();
            s_logger.warn(msg, e);
            return new ScaleVmAnswer(cmd, false, msg);
        }
    }

    private Answer execute(final RevertToVMSnapshotCommand cmd) {
        final String vmName = cmd.getVmName();
        final List<VolumeObjectTO> listVolumeTo = cmd.getVolumeTOs();
        final VMSnapshot.Type vmSnapshotType = cmd.getTarget().getType();
        final Boolean snapshotMemory = vmSnapshotType == VMSnapshot.Type.DiskAndMemory;
        final Connection conn = getConnection();
        PowerState vmState = null;
        VM vm = null;
        try {

            final Set<VM> vmSnapshots = VM.getByNameLabel(conn, cmd.getTarget().getSnapshotName());
            if (vmSnapshots.size() == 0) {
                return new RevertToVMSnapshotAnswer(cmd, false, "Cannot find vmSnapshot with name: " + cmd.getTarget().getSnapshotName());
            }

            final VM vmSnapshot = vmSnapshots.iterator().next();

            // find target VM or creating a work VM
            try {
                vm = getVM(conn, vmName);
            } catch (final Exception e) {
                vm = createWorkingVM(conn, vmName, cmd.getGuestOSType(), cmd.getPlatformEmulator(), listVolumeTo);
            }

            if (vm == null) {
                return new RevertToVMSnapshotAnswer(cmd, false, "Revert to VM Snapshot Failed due to can not find vm: " + vmName);
            }

            // call plugin to execute revert
            revertToSnapshot(conn, vmSnapshot, vmName, vm.getUuid(conn), snapshotMemory, _host.getUuid());
            vm = getVM(conn, vmName);
            final Set<VBD> vbds = vm.getVBDs(conn);
            final Map<String, VDI> vdiMap = new HashMap<String, VDI>();
            // get vdi:vbdr to a map
            for (final VBD vbd : vbds) {
                final VBD.Record vbdr = vbd.getRecord(conn);
                if (vbdr.type == Types.VbdType.DISK) {
                    final VDI vdi = vbdr.VDI;
                    vdiMap.put(vbdr.userdevice, vdi);
                }
            }

            if (!snapshotMemory) {
                vm.destroy(conn);
                vmState = PowerState.PowerOff;
            } else {
                vmState = PowerState.PowerOn;
            }

            // after revert, VM's volumes path have been changed, need to report to manager
            for (final VolumeObjectTO volumeTo : listVolumeTo) {
                final Long deviceId = volumeTo.getDeviceId();
                final VDI vdi = vdiMap.get(deviceId.toString());
                volumeTo.setPath(vdi.getUuid(conn));
            }

            return new RevertToVMSnapshotAnswer(cmd, listVolumeTo, vmState);
        } catch (final Exception e) {
            s_logger.error("revert vm " + vmName + " to snapshot " + cmd.getTarget().getSnapshotName() + " failed due to " + e.getMessage());
            return new RevertToVMSnapshotAnswer(cmd, false, e.getMessage());
        }
    }

    protected String revertToSnapshot(final Connection conn, final VM vmSnapshot, final String vmName, final String oldVmUuid, final Boolean snapshotMemory, final String hostUUID) throws XenAPIException,
    XmlRpcException {

        final String results =
                callHostPluginAsync(conn, "vmopsSnapshot", "revert_memory_snapshot", 10 * 60 * 1000, "snapshotUUID", vmSnapshot.getUuid(conn), "vmName", vmName, "oldVmUuid",
                        oldVmUuid, "snapshotMemory", snapshotMemory.toString(), "hostUUID", hostUUID);
        String errMsg = null;
        if (results == null || results.isEmpty()) {
            errMsg = "revert_memory_snapshot return null";
        } else {
            if (results.equals("0")) {
                return results;
            } else {
                errMsg = "revert_memory_snapshot exception";
            }
        }
        s_logger.warn(errMsg);
        throw new CloudRuntimeException(errMsg);
    }

    protected XsLocalNetwork getNativeNetworkForTraffic(final Connection conn, final TrafficType type, final String name) throws XenAPIException, XmlRpcException {
        if (name != null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Looking for network named " + name);
            }
            return getNetworkByName(conn, name);
        }

        if (type == TrafficType.Guest) {
            return new XsLocalNetwork(this, Network.getByUuid(conn, _host.getGuestNetwork()), null, PIF.getByUuid(conn, _host.getGuestPif()), null);
        } else if (type == TrafficType.Control) {
            setupLinkLocalNetwork(conn);
            return new XsLocalNetwork(this, Network.getByUuid(conn, _host.getLinkLocalNetwork()));
        } else if (type == TrafficType.Management) {
            return new XsLocalNetwork(this, Network.getByUuid(conn, _host.getPrivateNetwork()), null, PIF.getByUuid(conn, _host.getPrivatePif()), null);
        } else if (type == TrafficType.Public) {
            return new XsLocalNetwork(this, Network.getByUuid(conn, _host.getPublicNetwork()), null, PIF.getByUuid(conn, _host.getPublicPif()), null);
        } else if (type == TrafficType.Storage) {
            /*   TrafficType.Storage is for secondary storage, while storageNetwork1 is for primary storage, we need better name here */
            return new XsLocalNetwork(this, Network.getByUuid(conn, _host.getStorageNetwork1()), null, PIF.getByUuid(conn, _host.getStoragePif1()), null);
        }

        throw new CloudRuntimeException("Unsupported network type: " + type);
    }

    public synchronized Network setupvSwitchNetwork(final Connection conn) {
        try {
            if (_host.getVswitchNetwork() == null) {
                Network vswitchNw = null;
                final Network.Record rec = new Network.Record();
                final String nwName = Networks.BroadcastScheme.VSwitch.toString();
                final Set<Network> networks = Network.getByNameLabel(conn, nwName);

                if (networks.size() == 0) {
                    rec.nameDescription = "vswitch network for " + nwName;
                    rec.nameLabel = nwName;
                    vswitchNw = Network.create(conn, rec);
                } else {
                    vswitchNw = networks.iterator().next();
                }
                _host.setVswitchNetwork(vswitchNw);
            }
            return _host.getVswitchNetwork();
        } catch (final BadServerResponse e) {
            s_logger.error("Failed to setup vswitch network", e);
        } catch (final XenAPIException e) {
            s_logger.error("Failed to setup vswitch network", e);
        } catch (final XmlRpcException e) {
            s_logger.error("Failed to setup vswitch network", e);
        }

        return null;
    }

    /**
     * This method just creates a XenServer network following the tunnel network naming convention
     */
    private synchronized Network findOrCreateTunnelNetwork(final Connection conn, final String nwName) {
        try {
            Network nw = null;
            final Network.Record rec = new Network.Record();
            final Set<Network> networks = Network.getByNameLabel(conn, nwName);

            if (networks.size() == 0) {
                rec.nameDescription = "tunnel network id# " + nwName;
                rec.nameLabel = nwName;
                //Initialize the ovs-host-setup to avoid error when doing get-param in plugin
                final Map<String, String> otherConfig = new HashMap<String, String>();
                otherConfig.put("ovs-host-setup", "");
                // Mark 'internal network' as shared so bridge gets automatically created on each host in the cluster
                // when VM with vif connected to this internal network is started
                otherConfig.put("assume_network_is_shared", "true");
                rec.otherConfig = otherConfig;
                nw = Network.create(conn, rec);
                s_logger.debug("### XenServer network for tunnels created:" + nwName);
            } else {
                nw = networks.iterator().next();
                s_logger.debug("XenServer network for tunnels found:" + nwName);
            }
            return nw;
        } catch (final Exception e) {
            s_logger.warn("createTunnelNetwork failed", e);
            return null;
        }
    }

    /**
     * This method creates a XenServer network and configures it for being used as a L2-in-L3 tunneled network
     */
    private synchronized Network configureTunnelNetwork(final Connection conn, final Long networkId, final long hostId, final String bridgeName) {
        try {
            final Network nw = findOrCreateTunnelNetwork(conn, bridgeName);
            final String nwName = bridgeName;
            //Invoke plugin to setup the bridge which will be used by this network
            final String bridge = nw.getBridge(conn);
            final Map<String, String> nwOtherConfig = nw.getOtherConfig(conn);
            final String configuredHosts = nwOtherConfig.get("ovs-host-setup");
            boolean configured = false;
            if (configuredHosts != null) {
                final String hostIdsStr[] = configuredHosts.split(",");
                for (final String hostIdStr : hostIdsStr) {
                    if (hostIdStr.equals(((Long)hostId).toString())) {
                        configured = true;
                        break;
                    }
                }
            }

            if (!configured) {
                String result;
                if (bridgeName.startsWith("OVS-DR-VPC-Bridge")) {
                    result = callHostPlugin(conn, "ovstunnel", "setup_ovs_bridge_for_distributed_routing", "bridge", bridge,
                            "key", bridgeName,
                            "xs_nw_uuid", nw.getUuid(conn),
                            "cs_host_id", ((Long)hostId).toString());
                } else {
                    result = callHostPlugin(conn, "ovstunnel", "setup_ovs_bridge", "bridge", bridge,
                            "key", bridgeName,
                            "xs_nw_uuid", nw.getUuid(conn),
                            "cs_host_id", ((Long)hostId).toString());
                }

                //Note down the fact that the ovs bridge has been setup
                final String[] res = result.split(":");
                if (res.length != 2 || !res[0].equalsIgnoreCase("SUCCESS")) {
                    //TODO: Should make this error not fatal?
                    throw new CloudRuntimeException("Unable to pre-configure OVS bridge " + bridge );
                }
            }
            return nw;
        } catch (final Exception e) {
            s_logger.warn("createandConfigureTunnelNetwork failed", e);
            return null;
        }
    }

    private synchronized void destroyTunnelNetwork(final Connection conn, final Network nw, final long hostId) {
        try {
            final String bridge = nw.getBridge(conn);
            final String result = callHostPlugin(conn, "ovstunnel", "destroy_ovs_bridge", "bridge", bridge,
                    "cs_host_id", ((Long)hostId).toString());
            final String[] res = result.split(":");
            if (res.length != 2 || !res[0].equalsIgnoreCase("SUCCESS")) {
                //TODO: Should make this error not fatal?
                //Can Concurrent VM shutdown/migration/reboot events can cause this method
                //to be executed on a bridge which has already been removed?
                throw new CloudRuntimeException("Unable to remove OVS bridge " + bridge + ":" + result);
            }
            return;
        } catch (final Exception e) {
            s_logger.warn("destroyTunnelNetwork failed:", e);
            return;
        }
    }

    public Network getNetwork(final Connection conn, final NicTO nic) throws XenAPIException, XmlRpcException {
        final String name = nic.getName();
        final XsLocalNetwork network = getNativeNetworkForTraffic(conn, nic.getType(), name);
        if (network == null) {
            s_logger.error("Network is not configured on the backend for nic " + nic.toString());
            throw new CloudRuntimeException("Network for the backend is not configured correctly for network broadcast domain: " + nic.getBroadcastUri());
        }
        final URI uri = nic.getBroadcastUri();
        final BroadcastDomainType type = nic.getBroadcastType();
        if (uri != null && uri.toString().contains("untagged")) {
            return network.getNetwork();
        } else if (uri != null && type == BroadcastDomainType.Vlan) {
            assert BroadcastDomainType.getSchemeValue(uri) == BroadcastDomainType.Vlan;
            final long vlan = Long.parseLong(BroadcastDomainType.getValue(uri));
            return enableVlanNetwork(conn, vlan, network);
        } else if (type == BroadcastDomainType.Native || type == BroadcastDomainType.LinkLocal ||
                type == BroadcastDomainType.Vsp) {
            return network.getNetwork();
        } else if (uri != null && type == BroadcastDomainType.Vswitch) {
            final String header = uri.toString().substring(Networks.BroadcastDomainType.Vswitch.scheme().length() + "://".length());
            if (header.startsWith("vlan")) {
                _isOvs = true;
                return setupvSwitchNetwork(conn);
            } else {
                return findOrCreateTunnelNetwork(conn, getOvsTunnelNetworkName(uri.getAuthority()));
            }
        } else if (type == BroadcastDomainType.Storage) {
            if (uri == null) {
                return network.getNetwork();
            } else {
                final long vlan = Long.parseLong(BroadcastDomainType.getValue(uri));
                return enableVlanNetwork(conn, vlan, network);
            }
        } else if (type == BroadcastDomainType.Lswitch) {
            // Nicira Logical Switch
            return network.getNetwork();
        } else if (uri != null && type == BroadcastDomainType.Pvlan) {
            assert BroadcastDomainType.getSchemeValue(uri) == BroadcastDomainType.Pvlan;
            // should we consider moving this NetUtils method to BroadcastDomainType?
            final long vlan = Long.parseLong(NetUtils.getPrimaryPvlanFromUri(uri));
            return enableVlanNetwork(conn, vlan, network);
        }

        throw new CloudRuntimeException("Unable to support this type of network broadcast domain: " + nic.getBroadcastUri());
    }

    private String getOvsTunnelNetworkName(final String broadcastUri) {
        if (broadcastUri.contains(".")) {
            final String[] parts = broadcastUri.split("\\.");
            return "OVS-DR-VPC-Bridge"+parts[0];
        } else {
            try {
                return "OVSTunnel" + broadcastUri;
            } catch (final Exception e) {
                return null;
            }
        }
    }

    public VIF createVif(final Connection conn, final String vmName, final VM vm, final VirtualMachineTO vmSpec, final NicTO nic) throws XmlRpcException, XenAPIException {
        assert nic.getUuid() != null : "Nic should have a uuid value";

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Creating VIF for " + vmName + " on nic " + nic);
        }
        VIF.Record vifr = new VIF.Record();
        vifr.VM = vm;
        vifr.device = Integer.toString(nic.getDeviceId());
        vifr.MAC = nic.getMac();

        // Nicira needs these IDs to find the NIC
        vifr.otherConfig = new HashMap<String, String>();
        vifr.otherConfig.put("nicira-iface-id", nic.getUuid());
        vifr.otherConfig.put("nicira-vm-id", vm.getUuid(conn));
        // Provide XAPI with the cloudstack vm and nic uids.
        vifr.otherConfig.put("cloudstack-nic-id", nic.getUuid());
        if (vmSpec != null) {
            vifr.otherConfig.put("cloudstack-vm-id", vmSpec.getUuid());
        }

        // OVS plugin looks at network UUID in the vif 'otherconfig' details to group VIF's & tunnel ports as part of tier
        // when bridge is setup for distributed routing
        vifr.otherConfig.put("cloudstack-network-id", nic.getNetworkUuid());

        // Nuage Vsp needs Virtual Router IP to be passed in the otherconfig
        // get the virtual router IP information from broadcast uri
        final URI broadcastUri = nic.getBroadcastUri();
        if (broadcastUri != null && broadcastUri.getScheme().equalsIgnoreCase(Networks.BroadcastDomainType.Vsp.scheme())) {
            final String path = broadcastUri.getPath();
            vifr.otherConfig.put("vsp-vr-ip", path.substring(1));
        }
        vifr.network = getNetwork(conn, nic);

        if (nic.getNetworkRateMbps() != null && nic.getNetworkRateMbps().intValue() != -1) {
            vifr.qosAlgorithmType = "ratelimit";
            vifr.qosAlgorithmParams = new HashMap<String, String>();
            // convert mbs to kilobyte per second
            vifr.qosAlgorithmParams.put("kbps", Integer.toString(nic.getNetworkRateMbps() * 128));
        }

        vifr.lockingMode = Types.VifLockingMode.NETWORK_DEFAULT;
        final VIF vif = VIF.create(conn, vifr);
        if (s_logger.isDebugEnabled()) {
            vifr = vif.getRecord(conn);
            if(vifr !=  null) {
                s_logger.debug("Created a vif " + vifr.uuid + " on " + nic.getDeviceId());
            }
        }

        return vif;
    }

    public void prepareISO(final Connection conn, final String vmName) throws XmlRpcException, XenAPIException {

        final Set<VM> vms = VM.getByNameLabel(conn, vmName);
        if (vms == null || vms.size() != 1) {
            throw new CloudRuntimeException("There are " + (vms == null ? "0" : vms.size()) + " VMs named " + vmName);
        }
        final VM vm = vms.iterator().next();
        final Set<VBD> vbds = vm.getVBDs(conn);
        for (final VBD vbd : vbds) {
            final VBD.Record vbdr = vbd.getRecord(conn);
            if (vbdr.type == Types.VbdType.CD && vbdr.empty == false) {
                final VDI vdi = vbdr.VDI;
                final SR sr = vdi.getSR(conn);
                final Set<PBD> pbds = sr.getPBDs(conn);
                if (pbds == null) {
                    throw new CloudRuntimeException("There is no pbd for sr " + sr);
                }
                for (final PBD pbd : pbds) {
                    final PBD.Record pbdr = pbd.getRecord(conn);
                    if (pbdr.host.getUuid(conn).equals(_host.getUuid())) {
                        return;
                    }
                }
                sr.setShared(conn, true);
                final Host host = Host.getByUuid(conn, _host.getUuid());
                final PBD.Record pbdr = pbds.iterator().next().getRecord(conn);
                pbdr.host = host;
                pbdr.uuid = "";
                final PBD pbd = PBD.create(conn, pbdr);
                pbdPlug(conn, pbd, pbd.getUuid(conn));
                break;
            }
        }
    }

    protected VDI mount(final Connection conn, final String vmName, final DiskTO volume) throws XmlRpcException, XenAPIException {
        final DataTO data = volume.getData();
        final Volume.Type type = volume.getType();
        if (type == Volume.Type.ISO) {
            final TemplateObjectTO iso = (TemplateObjectTO)data;
            final DataStoreTO store = iso.getDataStore();

            if (store == null) {
                //It's a fake iso
                return null;
            }

            //corer case, xenserver pv driver iso
            final String templateName = iso.getName();
            if (templateName.startsWith("xs-tools")) {
                try {
                    final Set<VDI> vdis = VDI.getByNameLabel(conn, templateName);
                    if (vdis.isEmpty()) {
                        throw new CloudRuntimeException("Could not find ISO with URL: " + templateName);
                    }
                    return vdis.iterator().next();
                } catch (final XenAPIException e) {
                    throw new CloudRuntimeException("Unable to get pv iso: " + templateName + " due to " + e.toString());
                } catch (final Exception e) {
                    throw new CloudRuntimeException("Unable to get pv iso: " + templateName + " due to " + e.toString());
                }
            }

            if (!(store instanceof NfsTO)) {
                throw new CloudRuntimeException("only support mount iso on nfs");
            }
            final NfsTO nfsStore = (NfsTO)store;
            final String isoPath = nfsStore.getUrl() + File.separator + iso.getPath();
            final int index = isoPath.lastIndexOf("/");

            final String mountpoint = isoPath.substring(0, index);
            URI uri;
            try {
                uri = new URI(mountpoint);
            } catch (final URISyntaxException e) {
                throw new CloudRuntimeException("Incorrect uri " + mountpoint, e);
            }
            final SR isoSr = createIsoSRbyURI(conn, uri, vmName, false);

            final String isoname = isoPath.substring(index + 1);

            final VDI isoVdi = getVDIbyLocationandSR(conn, isoname, isoSr);

            if (isoVdi == null) {
                throw new CloudRuntimeException("Unable to find ISO " + isoPath);
            }
            return isoVdi;
        } else {
            final VolumeObjectTO vol = (VolumeObjectTO)data;
            return VDI.getByUuid(conn, vol.getPath());
        }
    }

    public VBD createVbd(final Connection conn, final DiskTO volume, final String vmName, final VM vm, final BootloaderType bootLoaderType, VDI vdi) throws XmlRpcException, XenAPIException {
        final Volume.Type type = volume.getType();

        if (vdi == null) {
            vdi = mount(conn, vmName, volume);
        }

        if (vdi != null) {
            if ("detached".equals(vdi.getNameLabel(conn))) {
                vdi.setNameLabel(conn, vmName + "-DATA");
            }

            final Map<String, String> smConfig = vdi.getSmConfig(conn);
            for (final String key : smConfig.keySet()) {
                if (key.startsWith("host_")) {
                    vdi.removeFromSmConfig(conn, key);
                    break;
                }
            }
        }
        final VBD.Record vbdr = new VBD.Record();
        vbdr.VM = vm;
        if (vdi != null) {
            vbdr.VDI = vdi;
        } else {
            vbdr.empty = true;
        }
        if (type == Volume.Type.ROOT && bootLoaderType == BootloaderType.PyGrub) {
            vbdr.bootable = true;
        } else if (type == Volume.Type.ISO && bootLoaderType == BootloaderType.CD) {
            vbdr.bootable = true;
        }

        vbdr.userdevice = Long.toString(volume.getDiskSeq());
        if (volume.getType() == Volume.Type.ISO) {
            vbdr.mode = Types.VbdMode.RO;
            vbdr.type = Types.VbdType.CD;
        } else if (volume.getType() == Volume.Type.ROOT) {
            vbdr.mode = Types.VbdMode.RW;
            vbdr.type = Types.VbdType.DISK;
            vbdr.unpluggable = false;
        } else {
            vbdr.mode = Types.VbdMode.RW;
            vbdr.type = Types.VbdType.DISK;
            vbdr.unpluggable = true;
        }
        final VBD vbd = VBD.create(conn, vbdr);

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("VBD " + vbd.getUuid(conn) + " created for " + volume);
        }

        return vbd;
    }


    private long getStaticMax(final String os, final boolean b, final long dynamicMinRam, final long dynamicMaxRam){
        final long recommendedValue = CitrixHelper.getXenServerStaticMax(os, b);
        if(recommendedValue == 0){
            s_logger.warn("No recommended value found for dynamic max, setting static max and dynamic max equal");
            return dynamicMaxRam;
        }
        final long staticMax = Math.min(recommendedValue, 4l * dynamicMinRam);  // XS constraint for stability
        if (dynamicMaxRam > staticMax){ // XS contraint that dynamic max <= static max
            s_logger.warn("dynamixMax " + dynamicMaxRam + " cant be greater than static max " + staticMax + ", can lead to stability issues. Setting static max as much as dynamic max ");
            return dynamicMaxRam;
        }
        return staticMax;
    }


    private long getStaticMin(final String os, final boolean b, final long dynamicMinRam, final long dynamicMaxRam) {
        final long recommendedValue = CitrixHelper.getXenServerStaticMin(os, b);
        if (recommendedValue == 0) {
            s_logger.warn("No recommended value found for dynamic min");
            return dynamicMinRam;
        }

        if (dynamicMinRam < recommendedValue) {   // XS contraint that dynamic min > static min
            s_logger.warn("Vm is set to dynamixMin " + dynamicMinRam + " less than the recommended static min " + recommendedValue + ", could lead to stability issues");
        }
        return dynamicMinRam;
    }


    public HashMap<String, HashMap<String, VgpuTypesInfo>> getGPUGroupDetails(final Connection conn) throws XenAPIException, XmlRpcException {
        return null;
    }

    public void createVGPU(final Connection conn, final StartCommand cmd, final VM vm, final GPUDeviceTO gpuDevice) throws XenAPIException, XmlRpcException {
    }

    public VM createVmFromTemplate(final Connection conn, final VirtualMachineTO vmSpec, final Host host) throws XenAPIException, XmlRpcException {
        final String guestOsTypeName = getGuestOsType(vmSpec.getOs(), vmSpec.getPlatformEmulator(), vmSpec.getBootloader() == BootloaderType.CD);
        final Set<VM> templates = VM.getByNameLabel(conn, guestOsTypeName);
        if ( templates == null || templates.isEmpty()) {
            throw new CloudRuntimeException("Cannot find template " + guestOsTypeName + " on XenServer host");
        }
        assert templates.size() == 1 : "Should only have 1 template but found " + templates.size();
        final VM template = templates.iterator().next();

        final VM.Record vmr = template.getRecord(conn);
        vmr.affinity = host;
        vmr.otherConfig.remove("disks");
        vmr.otherConfig.remove("default_template");
        vmr.otherConfig.remove("mac_seed");
        vmr.isATemplate = false;
        vmr.nameLabel = vmSpec.getName();
        vmr.actionsAfterCrash = Types.OnCrashBehaviour.DESTROY;
        vmr.actionsAfterShutdown = Types.OnNormalExit.DESTROY;
        vmr.otherConfig.put("vm_uuid", vmSpec.getUuid());
        vmr.VCPUsMax = (long) vmSpec.getCpus(); // FIX ME: In case of dynamic scaling this VCPU max should be the minumum of
        // recommended value for that template and capacity remaining on host

        if (isDmcEnabled(conn, host) && vmSpec.isEnableDynamicallyScaleVm()) {
            //scaling is allowed
            vmr.memoryStaticMin = getStaticMin(vmSpec.getOs(), vmSpec.getBootloader() == BootloaderType.CD, vmSpec.getMinRam(), vmSpec.getMaxRam());
            vmr.memoryStaticMax = getStaticMax(vmSpec.getOs(), vmSpec.getBootloader() == BootloaderType.CD, vmSpec.getMinRam(), vmSpec.getMaxRam());
            vmr.memoryDynamicMin = vmSpec.getMinRam();
            vmr.memoryDynamicMax = vmSpec.getMaxRam();
            if (guestOsTypeName.toLowerCase().contains("windows")) {
                vmr.VCPUsMax = (long) vmSpec.getCpus();
            } else {
                if (vmSpec.getVcpuMaxLimit() != null) {
                    vmr.VCPUsMax = (long) vmSpec.getVcpuMaxLimit();
                }
            }
        } else {
            //scaling disallowed, set static memory target
            if (vmSpec.isEnableDynamicallyScaleVm() && !isDmcEnabled(conn, host)) {
                s_logger.warn("Host " + host.getHostname(conn) + " does not support dynamic scaling, so the vm " + vmSpec.getName() + " is not dynamically scalable");
            }
            vmr.memoryStaticMin = vmSpec.getMinRam();
            vmr.memoryStaticMax = vmSpec.getMaxRam();
            vmr.memoryDynamicMin = vmSpec.getMinRam();;
            vmr.memoryDynamicMax = vmSpec.getMaxRam();

            vmr.VCPUsMax = (long) vmSpec.getCpus();
        }

        vmr.VCPUsAtStartup = (long) vmSpec.getCpus();
        vmr.consoles.clear();

        final VM vm = VM.create(conn, vmr);
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Created VM " + vm.getUuid(conn) + " for " + vmSpec.getName());
        }

        final Map<String, String> vcpuParams = new HashMap<String, String>();

        final Integer speed = vmSpec.getMinSpeed();
        if (speed != null) {

            int cpuWeight = _maxWeight; // cpu_weight
            int utilization = 0; // max CPU cap, default is unlimited

            // weight based allocation, CPU weight is calculated per VCPU
            cpuWeight = (int)(speed * 0.99 / _host.getSpeed() * _maxWeight);
            if (cpuWeight > _maxWeight) {
                cpuWeight = _maxWeight;
            }

            if (vmSpec.getLimitCpuUse()) {
                // CPU cap is per VM, so need to assign cap based on the number of vcpus
                utilization = (int)(vmSpec.getMaxSpeed() * 0.99 * vmSpec.getCpus() / _host.getSpeed() * 100);
            }

            vcpuParams.put("weight", Integer.toString(cpuWeight));
            vcpuParams.put("cap", Integer.toString(utilization));

        }

        if (vcpuParams.size() > 0) {
            vm.setVCPUsParams(conn, vcpuParams);
        }

        final String bootArgs = vmSpec.getBootArgs();
        if (bootArgs != null && bootArgs.length() > 0) {
            String pvargs = vm.getPVArgs(conn);
            pvargs = pvargs + vmSpec.getBootArgs().replaceAll(" ", "%");
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("PV args are " + pvargs);
            }
            vm.setPVArgs(conn, pvargs);
        }

        if (!(guestOsTypeName.startsWith("Windows") || guestOsTypeName.startsWith("Citrix") || guestOsTypeName.startsWith("Other"))) {
            if (vmSpec.getBootloader() == BootloaderType.CD) {
                final DiskTO[] disks = vmSpec.getDisks();
                for (final DiskTO disk : disks) {
                    if (disk.getType() == Volume.Type.ISO) {
                        final TemplateObjectTO iso = (TemplateObjectTO)disk.getData();
                        final String osType = iso.getGuestOsType();
                        if (osType != null) {
                            final String isoGuestOsName = getGuestOsType(osType, vmSpec.getPlatformEmulator(), vmSpec.getBootloader() == BootloaderType.CD);
                            if (!isoGuestOsName.equals(guestOsTypeName)) {
                                vmSpec.setBootloader(BootloaderType.PyGrub);
                            }
                        }
                    }
                }
            }
            if (vmSpec.getBootloader() == BootloaderType.CD) {
                vm.setPVBootloader(conn, "eliloader");
                if (!vm.getOtherConfig(conn).containsKey("install-repository")) {
                    vm.addToOtherConfig(conn, "install-repository", "cdrom");
                }
            } else if (vmSpec.getBootloader() == BootloaderType.PyGrub) {
                vm.setPVBootloader(conn, "pygrub");
            } else {
                vm.destroy(conn);
                throw new CloudRuntimeException("Unable to handle boot loader type: " + vmSpec.getBootloader());
            }
        }
        try {
            finalizeVmMetaData(vm, conn, vmSpec);
        } catch (final Exception e) {
            throw new CloudRuntimeException("Unable to finalize VM MetaData: " + vmSpec);
        }
        return vm;
    }


    protected void finalizeVmMetaData(final VM vm, final Connection conn, final VirtualMachineTO vmSpec) throws Exception {

        final Map<String, String> details = vmSpec.getDetails();
        if (details != null) {
            final String platformstring = details.get("platform");
            if (platformstring != null && !platformstring.isEmpty()) {
                final Map<String, String> platform = StringUtils.stringToMap(platformstring);
                vm.setPlatform(conn, platform);
            } else {
                final String timeoffset = details.get("timeoffset");
                if (timeoffset != null) {
                    final Map<String, String> platform = vm.getPlatform(conn);
                    platform.put("timeoffset", timeoffset);
                    vm.setPlatform(conn, platform);
                }
                final String coresPerSocket = details.get("cpu.corespersocket");
                if (coresPerSocket != null) {
                    final Map<String, String> platform = vm.getPlatform(conn);
                    platform.put("cores-per-socket", coresPerSocket);
                    vm.setPlatform(conn, platform);
                }
            }
            if ( !BootloaderType.CD.equals(vmSpec.getBootloader())) {
                final String xenservertoolsversion = details.get("hypervisortoolsversion");
                if ((xenservertoolsversion == null || !xenservertoolsversion.equalsIgnoreCase("xenserver61")) && vmSpec.getGpuDevice() == null) {
                    final Map<String, String> platform = vm.getPlatform(conn);
                    platform.remove("device_id");
                    vm.setPlatform(conn, platform);
                }
            }
        }
    }

    public String handleVmStartFailure(final Connection conn, final String vmName, final VM vm, final String message, final Throwable th) {
        final String msg = "Unable to start " + vmName + " due to " + message;
        s_logger.warn(msg, th);

        if (vm == null) {
            return msg;
        }

        try {
            final VM.Record vmr = vm.getRecord(conn);
            final List<Network> networks = new ArrayList<Network>();
            for (final VIF vif : vmr.VIFs) {
                try {
                    final VIF.Record rec = vif.getRecord(conn);
                    if(rec != null) {
                        networks.add(rec.network);
                    } else {
                        s_logger.warn("Unable to cleanup VIF: " + vif.toWireString() + " As vif record is null");
                    }
                } catch (final Exception e) {
                    s_logger.warn("Unable to cleanup VIF", e);
                }
            }
            if (vmr.powerState == VmPowerState.RUNNING) {
                try {
                    vm.hardShutdown(conn);
                } catch (final Exception e) {
                    s_logger.warn("VM hardshutdown failed due to ", e);
                }
            }
            if (vm.getPowerState(conn) == VmPowerState.HALTED) {
                try {
                    vm.destroy(conn);
                } catch (final Exception e) {
                    s_logger.warn("VM destroy failed due to ", e);
                }
            }
            for (final VBD vbd : vmr.VBDs) {
                try {
                    vbd.unplug(conn);
                    vbd.destroy(conn);
                } catch (final Exception e) {
                    s_logger.warn("Unable to clean up VBD due to ", e);
                }
            }
            for (final VIF vif : vmr.VIFs) {
                try {
                    vif.unplug(conn);
                    vif.destroy(conn);
                } catch (final Exception e) {
                    s_logger.warn("Unable to cleanup VIF", e);
                }
            }
            for (final Network network : networks) {
                if (network.getNameLabel(conn).startsWith("VLAN")) {
                    disableVlanNetwork(conn, network);
                }
            }
        } catch (final Exception e) {
            s_logger.warn("VM getRecord failed due to ", e);
        }

        return msg;
    }

    public VBD createPatchVbd(final Connection conn, final String vmName, final VM vm) throws XmlRpcException, XenAPIException {

        if (_host.getSystemvmisouuid() == null) {
            final Set<SR> srs = SR.getByNameLabel(conn, "XenServer Tools");
            if (srs.size() != 1) {
                throw new CloudRuntimeException("There are " + srs.size() + " SRs with name XenServer Tools");
            }
            final SR sr = srs.iterator().next();
            sr.scan(conn);

            final SR.Record srr = sr.getRecord(conn);

            if (_host.getSystemvmisouuid() == null) {
                for (final VDI vdi : srr.VDIs) {
                    final VDI.Record vdir = vdi.getRecord(conn);
                    if (vdir.nameLabel.contains("systemvm.iso")) {
                        _host.setSystemvmisouuid(vdir.uuid);
                        break;
                    }
                }
            }
            if (_host.getSystemvmisouuid() == null) {
                throw new CloudRuntimeException("can not find systemvmiso");
            }
        }

        final VBD.Record cdromVBDR = new VBD.Record();
        cdromVBDR.VM = vm;
        cdromVBDR.empty = true;
        cdromVBDR.bootable = false;
        cdromVBDR.userdevice = "3";
        cdromVBDR.mode = Types.VbdMode.RO;
        cdromVBDR.type = Types.VbdType.CD;
        final VBD cdromVBD = VBD.create(conn, cdromVBDR);
        cdromVBD.insert(conn, VDI.getByUuid(conn, _host.getSystemvmisouuid()));

        return cdromVBD;
    }

    public void destroyPatchVbd(final Connection conn, final String vmName) throws XmlRpcException, XenAPIException {
        try {
            if (!vmName.startsWith("r-") && !vmName.startsWith("s-") && !vmName.startsWith("v-")) {
                return;
            }
            final Set<VM> vms = VM.getByNameLabel(conn, vmName);
            for (final VM vm : vms) {
                final Set<VBD> vbds = vm.getVBDs(conn);
                for (final VBD vbd : vbds) {
                    if (vbd.getType(conn) == Types.VbdType.CD) {
                        vbd.eject(conn);
                        vbd.destroy(conn);
                        break;
                    }
                }
            }
        } catch (final Exception e) {
            s_logger.debug("Cannot destory CD-ROM device for VM " + vmName + " due to " + e.toString(), e);
        }
    }

    protected CheckSshAnswer execute(final CheckSshCommand cmd) {
        final Connection conn = getConnection();
        final String vmName = cmd.getName();
        final String privateIp = cmd.getIp();
        final int cmdPort = cmd.getPort();

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Ping command port, " + privateIp + ":" + cmdPort);
        }

        try {
            final String result = connect(conn, cmd.getName(), privateIp, cmdPort);
            if (result != null) {
                return new CheckSshAnswer(cmd, "Can not ping System vm " + vmName + "due to:" + result);
            }
            //Do not destroy the disk here! It will stio the patching process. Please, don't!
            //destroyPatchVbd(conn, vmName);
        } catch (final Exception e) {
            return new CheckSshAnswer(cmd, e);
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Ping command port succeeded for vm " + vmName);
        }

        return new CheckSshAnswer(cmd);
    }

    public HashMap<String, String> parseDefaultOvsRuleComamnd(final String str) {
        final HashMap<String, String> cmd = new HashMap<String, String>();
        final String[] sarr = str.split("/");
        for (int i = 0; i < sarr.length; i++) {
            String c = sarr[i];
            c = c.startsWith("/") ? c.substring(1) : c;
            c = c.endsWith("/") ? c.substring(0, c.length() - 1) : c;
            final String[] p = c.split(";");
            if (p.length != 2) {
                continue;
            }
            if (p[0].equalsIgnoreCase("vlans")) {
                p[1] = p[1].replace("@", "[");
                p[1] = p[1].replace("#", "]");
            }
            cmd.put(p[0], p[1]);
        }
        return cmd;
    }

    private void cleanUpTmpDomVif(final Connection conn, final Network nw) throws XenAPIException, XmlRpcException {

        final Pair<VM, VM.Record> vm = getControlDomain(conn);
        final VM dom0 = vm.first();
        final Set<VIF> dom0Vifs = dom0.getVIFs(conn);
        for (final VIF v : dom0Vifs) {
            String vifName = "unknown";
            try {
                final VIF.Record vifr = v.getRecord(conn);
                if (v.getNetwork(conn).getUuid(conn).equals(nw.getUuid(conn))) {
                    if(vifr != null) {
                        final Map<String, String> config = vifr.otherConfig;
                        vifName = config.get("nameLabel");
                    }
                    s_logger.debug("A VIF in dom0 for the network is found - so destroy the vif");
                    v.destroy(conn);
                    s_logger.debug("Destroy temp dom0 vif" + vifName + " success");
                }
            } catch (final Exception e) {
                s_logger.warn("Destroy temp dom0 vif " + vifName + "failed", e);
            }
        }
    }

    private Answer execute(final PvlanSetupCommand cmd) {
        final Connection conn = getConnection();

        final String primaryPvlan = cmd.getPrimary();
        final String isolatedPvlan = cmd.getIsolated();
        final String op = cmd.getOp();
        final String dhcpName = cmd.getDhcpName();
        final String dhcpMac = cmd.getDhcpMac();
        final String dhcpIp = cmd.getDhcpIp();
        final String vmMac = cmd.getVmMac();
        final String networkTag = cmd.getNetworkTag();

        XsLocalNetwork nw = null;
        String nwNameLabel = null;
        try {
            nw = getNativeNetworkForTraffic(conn, TrafficType.Guest, networkTag);
            if (nw == null) {
                s_logger.error("Network is not configured on the backend for pvlan " + primaryPvlan);
                throw new CloudRuntimeException("Network for the backend is not configured correctly for pvlan primary: " + primaryPvlan);
            }
            nwNameLabel = nw.getNetwork().getNameLabel(conn);
        } catch (final XenAPIException e) {
            s_logger.warn("Fail to get network", e);
            return new Answer(cmd, false, e.toString());
        } catch (final XmlRpcException e) {
            s_logger.warn("Fail to get network", e);
            return new Answer(cmd, false, e.toString());
        }

        String result = null;
        if (cmd.getType() == PvlanSetupCommand.Type.DHCP) {
            result =
                    callHostPlugin(conn, "ovs-pvlan", "setup-pvlan-dhcp", "op", op, "nw-label", nwNameLabel, "primary-pvlan", primaryPvlan, "isolated-pvlan", isolatedPvlan,
                            "dhcp-name", dhcpName, "dhcp-ip", dhcpIp, "dhcp-mac", dhcpMac);
            if (result == null || result.isEmpty() || !Boolean.parseBoolean(result)) {
                s_logger.warn("Failed to program pvlan for dhcp server with mac " + dhcpMac);
                return new Answer(cmd, false, result);
            } else {
                s_logger.info("Programmed pvlan for dhcp server with mac " + dhcpMac);
            }
        } else if (cmd.getType() == PvlanSetupCommand.Type.VM) {
            result =
                    callHostPlugin(conn, "ovs-pvlan", "setup-pvlan-vm", "op", op, "nw-label", nwNameLabel, "primary-pvlan", primaryPvlan, "isolated-pvlan", isolatedPvlan,
                            "vm-mac", vmMac);
            if (result == null || result.isEmpty() || !Boolean.parseBoolean(result)) {
                s_logger.warn("Failed to program pvlan for vm with mac " + vmMac);
                return new Answer(cmd, false, result);
            } else {
                s_logger.info("Programmed pvlan for vm with mac " + vmMac);
            }
        }
        return new Answer(cmd, true, result);
    }

    @Override
    public StartAnswer execute(final StartCommand cmd) {
        final Connection conn = getConnection();
        final VirtualMachineTO vmSpec = cmd.getVirtualMachine();
        final String vmName = vmSpec.getName();
        VmPowerState state = VmPowerState.HALTED;
        VM vm = null;
        // if a VDI is created, record its UUID to send back to the CS MS
        final Map<String, String> iqnToPath = new HashMap<String, String>();
        try {
            final Set<VM> vms = VM.getByNameLabel(conn, vmName);
            if (vms != null) {
                for (final VM v : vms) {
                    final VM.Record vRec = v.getRecord(conn);
                    if (vRec.powerState == VmPowerState.HALTED) {
                        v.destroy(conn);
                    } else if (vRec.powerState == VmPowerState.RUNNING) {
                        final String host = vRec.residentOn.getUuid(conn);
                        final String msg = "VM " + vmName + " is runing on host " + host;
                        s_logger.debug(msg);
                        return new StartAnswer(cmd, msg, host);
                    } else {
                        final String msg = "There is already a VM having the same name " + vmName + " vm record " + vRec.toString();
                        s_logger.warn(msg);
                        return new StartAnswer(cmd, msg);
                    }
                }
            }
            s_logger.debug("1. The VM " + vmName + " is in Starting state.");

            final Host host = Host.getByUuid(conn, _host.getUuid());
            vm = createVmFromTemplate(conn, vmSpec, host);

            final GPUDeviceTO gpuDevice = vmSpec.getGpuDevice();
            if (gpuDevice != null) {
                s_logger.debug("Creating VGPU for of VGPU type: " + gpuDevice.getVgpuType() + " in GPU group "
                        + gpuDevice.getGpuGroup() + " for VM " + vmName );
                createVGPU(conn, cmd, vm, gpuDevice);
            }

            for (final DiskTO disk : vmSpec.getDisks()) {
                final VDI newVdi = prepareManagedDisk(conn, disk, vmName);

                if (newVdi != null) {
                    final String path = newVdi.getUuid(conn);

                    iqnToPath.put(disk.getDetails().get(DiskTO.IQN), path);
                }

                createVbd(conn, disk, vmName, vm, vmSpec.getBootloader(), newVdi);
            }

            if (vmSpec.getType() != VirtualMachine.Type.User) {
                createPatchVbd(conn, vmName, vm);
            }

            for (final NicTO nic : vmSpec.getNics()) {
                createVif(conn, vmName, vm, vmSpec, nic);
            }

            startVM(conn, host, vm, vmName);

            if (_isOvs) {
                // TODO(Salvatore-orlando): This code should go
                for (final NicTO nic : vmSpec.getNics()) {
                    if (nic.getBroadcastType() == Networks.BroadcastDomainType.Vswitch) {
                        final HashMap<String, String> args = parseDefaultOvsRuleComamnd(BroadcastDomainType.getValue(nic.getBroadcastUri()));
                        final OvsSetTagAndFlowCommand flowCmd =
                                new OvsSetTagAndFlowCommand(args.get("vmName"), args.get("tag"), args.get("vlans"), args.get("seqno"), Long.parseLong(args.get("vmId")));
                        final OvsSetTagAndFlowAnswer r = execute(flowCmd);
                        if (!r.getResult()) {
                            s_logger.warn("Failed to set flow for VM " + r.getVmId());
                        } else {
                            s_logger.info("Success to set flow for VM " + r.getVmId());
                        }
                    }
                }
            }

            if (_canBridgeFirewall) {
                String result = null;
                if (vmSpec.getType() != VirtualMachine.Type.User) {
                    final NicTO[] nics = vmSpec.getNics();
                    boolean secGrpEnabled = false;
                    for (final NicTO nic : nics) {
                        if (nic.isSecurityGroupEnabled() ||
                                nic.getIsolationUri() != null && nic.getIsolationUri().getScheme().equalsIgnoreCase(IsolationType.Ec2.toString())) {
                            secGrpEnabled = true;
                            break;
                        }
                    }
                    if (secGrpEnabled) {
                        result = callHostPlugin(conn, "vmops", "default_network_rules_systemvm", "vmName", vmName);
                        if (result == null || result.isEmpty() || !Boolean.parseBoolean(result)) {
                            s_logger.warn("Failed to program default network rules for " + vmName);
                        } else {
                            s_logger.info("Programmed default network rules for " + vmName);
                        }
                    }

                } else {
                    //For user vm, program the rules for each nic if the isolation uri scheme is ec2
                    final NicTO[] nics = vmSpec.getNics();
                    for (final NicTO nic : nics) {
                        if (nic.isSecurityGroupEnabled() || nic.getIsolationUri() != null &&
                                nic.getIsolationUri().getScheme().equalsIgnoreCase(IsolationType.Ec2.toString())) {
                            final List<String> nicSecIps = nic.getNicSecIps();
                            String secIpsStr;
                            final StringBuilder sb = new StringBuilder();
                            if (nicSecIps != null) {
                                for (final String ip : nicSecIps) {
                                    sb.append(ip).append(":");
                                }
                                secIpsStr = sb.toString();
                            } else {
                                secIpsStr = "0:";
                            }
                            result =
                                    callHostPlugin(conn, "vmops", "default_network_rules", "vmName", vmName, "vmIP", nic.getIp(), "vmMAC", nic.getMac(), "vmID",
                                            Long.toString(vmSpec.getId()), "secIps", secIpsStr);

                            if (result == null || result.isEmpty() || !Boolean.parseBoolean(result)) {
                                s_logger.warn("Failed to program default network rules for " + vmName + " on nic with ip:" + nic.getIp() + " mac:" + nic.getMac());
                            } else {
                                s_logger.info("Programmed default network rules for " + vmName + " on nic with ip:" + nic.getIp() + " mac:" + nic.getMac());
                            }
                        }
                    }
                }
            }

            state = VmPowerState.RUNNING;

            final StartAnswer startAnswer = new StartAnswer(cmd);

            startAnswer.setIqnToPath(iqnToPath);

            return startAnswer;
        } catch (final Exception e) {
            s_logger.warn("Catch Exception: " + e.getClass().toString() + " due to " + e.toString(), e);
            final String msg = handleVmStartFailure(conn, vmName, vm, "", e);

            final StartAnswer startAnswer = new StartAnswer(cmd, msg);

            startAnswer.setIqnToPath(iqnToPath);

            return startAnswer;
        } finally {
            if (state != VmPowerState.HALTED) {
                s_logger.debug("2. The VM " + vmName + " is in " + state + " state.");
            } else {
                s_logger.debug("The VM is in stopped state, detected problem during startup : " + vmName);
            }
        }
    }

    // the idea here is to see if the DiskTO in question is from managed storage and
    // does not yet have an SR
    // if no SR, create it and create a VDI in it
    public VDI prepareManagedDisk(final Connection conn, final DiskTO disk, final String vmName) throws Exception {
        final Map<String, String> details = disk.getDetails();

        if (details == null) {
            return null;
        }

        final boolean isManaged = new Boolean(details.get(DiskTO.MANAGED)).booleanValue();

        if (!isManaged) {
            return null;
        }

        final String iqn = details.get(DiskTO.IQN);

        final Set<SR> srNameLabels = SR.getByNameLabel(conn, iqn);

        if (srNameLabels.size() != 0) {
            return null;
        }

        final String vdiNameLabel = vmName + "-DATA";

        return prepareManagedStorage(conn, details, null, vdiNameLabel);
    }

    protected SR prepareManagedSr(final Connection conn, final Map<String, String> details) {
        final String iScsiName = details.get(DiskTO.IQN);
        final String storageHost = details.get(DiskTO.STORAGE_HOST);
        final String chapInitiatorUsername = details.get(DiskTO.CHAP_INITIATOR_USERNAME);
        final String chapInitiatorSecret = details.get(DiskTO.CHAP_INITIATOR_SECRET);
        final String mountpoint = details.get(DiskTO.MOUNT_POINT);
        final String protocoltype = details.get(DiskTO.PROTOCOL_TYPE);

        if (StoragePoolType.NetworkFilesystem.toString().equalsIgnoreCase(protocoltype)) {
            final String poolid = storageHost + ":" + mountpoint;
            final String namelable = mountpoint;
            final String volumedesc = storageHost + ":" + mountpoint;

            return getNfsSR(conn, poolid, namelable, storageHost, mountpoint, volumedesc);
        } else {
            return getIscsiSR(conn, iScsiName, storageHost, iScsiName, chapInitiatorUsername, chapInitiatorSecret, true);
        }
    }

    protected VDI prepareManagedStorage(final Connection conn, final Map<String, String> details, final String path, final String vdiNameLabel) throws Exception {
        final SR sr = prepareManagedSr(conn, details);

        VDI vdi = getVDIbyUuid(conn, path, false);
        final Long volumeSize = Long.parseLong(details.get(DiskTO.VOLUME_SIZE));

        if (vdi == null) {
            vdi = createVdi(sr, vdiNameLabel, volumeSize);
        } else {
            // if VDI is not null, it must have already been created, so check whether a resize of the volume was performed
            // if true, resize the VDI to the volume size

            s_logger.info("checking for the resize of the datadisk");

            final long vdiVirtualSize = vdi.getVirtualSize(conn);

            if (vdiVirtualSize != volumeSize) {
                s_logger.info("resizing the data disk (vdi) from vdiVirtualsize: "+ vdiVirtualSize + " to volumeSize: " + volumeSize);

                try {
                    vdi.resize(conn, volumeSize);
                } catch (final Exception e) {
                    s_logger.warn("Unable to resize volume", e);
                }
            }
        }

        return vdi;
    }

    protected Answer execute(final ModifySshKeysCommand cmd) {
        return new Answer(cmd);
    }

    public boolean doPingTest(final Connection conn, final String computingHostIp) {
        final com.trilead.ssh2.Connection sshConnection = new com.trilead.ssh2.Connection(_host.getIp(), 22);
        try {
            sshConnection.connect(null, 60000, 60000);
            if (!sshConnection.authenticateWithPassword(_username, _password.peek())) {
                throw new CloudRuntimeException("Unable to authenticate");
            }

            final String cmd = "ping -c 2 " + computingHostIp;
            if (!SSHCmdHelper.sshExecuteCmd(sshConnection, cmd)) {
                throw new CloudRuntimeException("Cannot ping host " + computingHostIp + " from host " + _host.getIp());
            }
            return true;
        } catch (final Exception e) {
            s_logger.warn("Catch exception " + e.toString(), e);
            return false;
        } finally {
            sshConnection.close();
        }
    }

    protected CheckOnHostAnswer execute(final CheckOnHostCommand cmd) {
        return new CheckOnHostAnswer(cmd, null, "Not Implmeneted");
    }

    public boolean doPingTest(final Connection conn, final String domRIp, final String vmIp) {
        final String args = "-i " + domRIp + " -p " + vmIp;
        final String result = callHostPlugin(conn, "vmops", "pingtest", "args", args);
        if (result == null || result.isEmpty()) {
            return false;
        }
        return true;
    }

    private Answer execute(final PingTestCommand cmd) {
        final Connection conn = getConnection();
        boolean result = false;
        final String computingHostIp = cmd.getComputingHostIp();

        if (computingHostIp != null) {
            result = doPingTest(conn, computingHostIp);
        } else {
            result = doPingTest(conn, cmd.getRouterIp(), cmd.getPrivateIp());
        }

        if (!result) {
            return new Answer(cmd, false, "PingTestCommand failed");
        }
        return new Answer(cmd);
    }

    protected MaintainAnswer execute(final MaintainCommand cmd) {
        final Connection conn = getConnection();
        try {

            final Host host = Host.getByUuid(conn, _host.getUuid());
            // remove all tags cloud stack
            final Host.Record hr = host.getRecord(conn);
            final Iterator<String> it = hr.tags.iterator();
            while (it.hasNext()) {
                final String tag = it.next();
                if (tag.contains("cloud")) {
                    it.remove();
                }
            }
            host.setTags(conn, hr.tags);
            return new MaintainAnswer(cmd);
        } catch (final XenAPIException e) {
            s_logger.warn("Unable to put server in maintainence mode", e);
            return new MaintainAnswer(cmd, false, e.getMessage());
        } catch (final XmlRpcException e) {
            s_logger.warn("Unable to put server in maintainence mode", e);
            return new MaintainAnswer(cmd, false, e.getMessage());
        }
    }

    public String networkUsage(final Connection conn, final String privateIpAddress, final String option, final String vif) {
        if (option.equals("get")) {
            return "0:0";
        }
        return null;
    }

    protected ExecutionResult prepareNetworkElementCommand(final IpAssocCommand cmd) {
        final Connection conn = getConnection();
        final String routerName = cmd.getAccessDetail(NetworkElementCommand.ROUTER_NAME);
        final String routerIp = cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);

        try {
            final IpAddressTO[] ips = cmd.getIpAddresses();
            for (final IpAddressTO ip : ips) {

                final VM router = getVM(conn, routerName);

                final NicTO nic = new NicTO();
                nic.setMac(ip.getVifMacAddress());
                nic.setType(ip.getTrafficType());
                if (ip.getBroadcastUri()== null) {
                    nic.setBroadcastType(BroadcastDomainType.Native);
                } else {
                    final URI uri = BroadcastDomainType.fromString(ip.getBroadcastUri());
                    nic.setBroadcastType(BroadcastDomainType.getSchemeValue(uri));
                    nic.setBroadcastUri(uri);
                }
                nic.setDeviceId(0);
                nic.setNetworkRateMbps(ip.getNetworkRate());
                nic.setName(ip.getNetworkName());

                final Network network = getNetwork(conn, nic);

                // Determine the correct VIF on DomR to associate/disassociate the
                // IP address with
                VIF correctVif = getCorrectVif(conn, router, network);

                // If we are associating an IP address and DomR doesn't have a VIF
                // for the specified vlan ID, we need to add a VIF
                // If we are disassociating the last IP address in the VLAN, we need
                // to remove a VIF
                boolean addVif = false;
                if (ip.isAdd() && correctVif == null) {
                    addVif = true;
                }

                if (addVif) {
                    // Add a new VIF to DomR
                    final String vifDeviceNum = getLowestAvailableVIFDeviceNum(conn, router);

                    if (vifDeviceNum == null) {
                        throw new InternalErrorException("There were no more available slots for a new VIF on router: " + router.getNameLabel(conn));
                    }

                    nic.setDeviceId(Integer.valueOf(vifDeviceNum));

                    correctVif = createVif(conn, routerName, router, null, nic);
                    correctVif.plug(conn);
                    // Add iptables rule for network usage
                    networkUsage(conn, routerIp, "addVif", "eth" + correctVif.getDevice(conn));
                }

                if (ip.isAdd() && correctVif == null) {
                    throw new InternalErrorException("Failed to find DomR VIF to associate/disassociate IP with.");
                }
                if (correctVif != null ) {
                    ip.setNicDevId(Integer.valueOf(correctVif.getDevice(conn)));
                    ip.setNewNic(addVif);
                }
            }
        } catch (final InternalErrorException e) {
            s_logger.error("Ip Assoc failure on applying one ip due to exception:  ", e);
            return new ExecutionResult(false, e.getMessage());
        } catch (final Exception e) {
            return new ExecutionResult(false, e.getMessage());
        }
        return new ExecutionResult(true, null);
    }

    protected ExecutionResult cleanupNetworkElementCommand(final IpAssocCommand cmd) {
        final Connection conn = getConnection();
        final String routerName = cmd.getAccessDetail(NetworkElementCommand.ROUTER_NAME);
        final String routerIp = cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);
        try {
            final IpAddressTO[] ips = cmd.getIpAddresses();
            final int ipsCount = ips.length;
            for (final IpAddressTO ip : ips) {

                final VM router = getVM(conn, routerName);

                final NicTO nic = new NicTO();
                nic.setMac(ip.getVifMacAddress());
                nic.setType(ip.getTrafficType());
                if (ip.getBroadcastUri()== null) {
                    nic.setBroadcastType(BroadcastDomainType.Native);
                } else {
                    final URI uri = BroadcastDomainType.fromString(ip.getBroadcastUri());
                    nic.setBroadcastType(BroadcastDomainType.getSchemeValue(uri));
                    nic.setBroadcastUri(uri);
                }
                nic.setDeviceId(0);
                nic.setNetworkRateMbps(ip.getNetworkRate());
                nic.setName(ip.getNetworkName());

                Network network = getNetwork(conn, nic);


                // If we are disassociating the last IP address in the VLAN, we need
                // to remove a VIF
                boolean removeVif = false;

                //there is only one ip in this public vlan and removing it, so remove the nic
                if (ipsCount == 1 && !ip.isAdd()) {
                    removeVif = true;
                }

                if (removeVif) {

                    // Determine the correct VIF on DomR to associate/disassociate the
                    // IP address with
                    final VIF correctVif = getCorrectVif(conn, router, network);
                    if (correctVif != null) {
                        network = correctVif.getNetwork(conn);

                        // Mark this vif to be removed from network usage
                        networkUsage(conn, routerIp, "deleteVif", "eth" + correctVif.getDevice(conn));

                        // Remove the VIF from DomR
                        correctVif.unplug(conn);
                        correctVif.destroy(conn);

                        // Disable the VLAN network if necessary
                        disableVlanNetwork(conn, network);
                    }
                }
            }
        } catch (final Exception e) {
            s_logger.debug("Ip Assoc failure on applying one ip due to exception:  ", e);
            return new ExecutionResult(false, e.getMessage());
        }
        return new ExecutionResult(true, null);
    }

    protected GetVncPortAnswer execute(final GetVncPortCommand cmd) {
        final Connection conn = getConnection();
        try {
            final Set<VM> vms = VM.getByNameLabel(conn, cmd.getName());
            if (vms.size() == 1) {
                String consoleurl;
                consoleurl = "consoleurl=" + getVncUrl(conn, vms.iterator().next()) + "&" + "sessionref=" + conn.getSessionReference();
                return new GetVncPortAnswer(cmd, consoleurl, -1);
            } else {
                return new GetVncPortAnswer(cmd, "There are " + vms.size() + " VMs named " + cmd.getName());
            }
        } catch (final Exception e) {
            final String msg = "Unable to get vnc port due to " + e.toString();
            s_logger.warn(msg, e);
            return new GetVncPortAnswer(cmd, msg);
        }
    }

    protected Storage.StorageResourceType getStorageResourceType() {
        return Storage.StorageResourceType.STORAGE_POOL;
    }

    protected CheckHealthAnswer execute(final CheckHealthCommand cmd) {
        final boolean result = pingXAPI();
        return new CheckHealthAnswer(cmd, result);
    }

    protected long[] getNetworkStats(final Connection conn, final String privateIP) {
        final String result = networkUsage(conn, privateIP, "get", null);
        final long[] stats = new long[2];
        if (result != null) {
            final String[] splitResult = result.split(":");
            int i = 0;
            while (i < splitResult.length - 1) {
                stats[0] += new Long(splitResult[i++]).longValue();
                stats[1] += new Long(splitResult[i++]).longValue();
            }
        }
        return stats;
    }

    /**
     * This is the method called for getting the HOST stats
     *
     * @param cmd
     * @return
     */
    protected GetHostStatsAnswer execute(final GetHostStatsCommand cmd) {
        final Connection conn = getConnection();
        try {
            final HostStatsEntry hostStats = getHostStats(conn, cmd, cmd.getHostGuid(), cmd.getHostId());
            return new GetHostStatsAnswer(cmd, hostStats);
        } catch (final Exception e) {
            final String msg = "Unable to get Host stats" + e.toString();
            s_logger.warn(msg, e);
            return new GetHostStatsAnswer(cmd, null);
        }
    }

    public HostStatsEntry getHostStats(final Connection conn, final GetHostStatsCommand cmd, final String hostGuid, final long hostId) {

        final HostStatsEntry hostStats = new HostStatsEntry(hostId, 0, 0, 0, "host", 0, 0, 0, 0);
        final Object[] rrdData = getRRDData(conn, 1); // call rrd method with 1 for host

        if (rrdData == null) {
            return null;
        }

        final Integer numRows = (Integer)rrdData[0];
        final Integer numColumns = (Integer)rrdData[1];
        final Node legend = (Node)rrdData[2];
        final Node dataNode = (Node)rrdData[3];

        final NodeList legendChildren = legend.getChildNodes();
        for (int col = 0; col < numColumns; col++) {

            if (legendChildren == null || legendChildren.item(col) == null) {
                continue;
            }

            final String columnMetadata = getXMLNodeValue(legendChildren.item(col));

            if (columnMetadata == null) {
                continue;
            }

            final String[] columnMetadataList = columnMetadata.split(":");

            if (columnMetadataList.length != 4) {
                continue;
            }

            final String type = columnMetadataList[1];
            final String param = columnMetadataList[3];

            if (type.equalsIgnoreCase("host")) {

                if (param.matches("pif_eth0_rx")) {
                    hostStats.setNetworkReadKBs(getDataAverage(dataNode, col, numRows)/1000);
                } else if (param.matches("pif_eth0_tx")) {
                    hostStats.setNetworkWriteKBs(getDataAverage(dataNode, col, numRows)/1000);
                } else if (param.contains("memory_total_kib")) {
                    hostStats.setTotalMemoryKBs(getDataAverage(dataNode, col, numRows));
                } else if (param.contains("memory_free_kib")) {
                    hostStats.setFreeMemoryKBs(getDataAverage(dataNode, col, numRows));
                } else if (param.matches("cpu_avg")) {
                    // hostStats.setNumCpus(hostStats.getNumCpus() + 1);
                    hostStats.setCpuUtilization(hostStats.getCpuUtilization() + getDataAverage(dataNode, col, numRows));
                }

                /*
                if (param.contains("loadavg")) {
                    hostStats.setAverageLoad((hostStats.getAverageLoad() + getDataAverage(dataNode, col, numRows)));
                }
                 */
            }
        }

        // add the host cpu utilization
        /*
        if (hostStats.getNumCpus() != 0) {
            hostStats.setCpuUtilization(hostStats.getCpuUtilization() / hostStats.getNumCpus());
            s_logger.debug("Host cpu utilization " + hostStats.getCpuUtilization());
        }
         */

        return hostStats;
    }

    protected GetVmStatsAnswer execute(final GetVmStatsCommand cmd) {
        final Connection conn = getConnection();
        final List<String> vmNames = cmd.getVmNames();
        final HashMap<String, VmStatsEntry> vmStatsNameMap = new HashMap<String, VmStatsEntry>();
        if (vmNames.size() == 0) {
            return new GetVmStatsAnswer(cmd, vmStatsNameMap);
        }
        try {

            // Determine the UUIDs of the requested VMs
            final List<String> vmUUIDs = new ArrayList<String>();

            for (final String vmName : vmNames) {
                final VM vm = getVM(conn, vmName);
                vmUUIDs.add(vm.getUuid(conn));
            }

            final HashMap<String, VmStatsEntry> vmStatsUUIDMap = getVmStats(conn, cmd, vmUUIDs, cmd.getHostGuid());
            if (vmStatsUUIDMap == null) {
                return new GetVmStatsAnswer(cmd, vmStatsNameMap);
            }

            for (final Map.Entry<String,VmStatsEntry>entry : vmStatsUUIDMap.entrySet()) {
                vmStatsNameMap.put(vmNames.get(vmUUIDs.indexOf(entry.getKey())), entry.getValue());
            }

            return new GetVmStatsAnswer(cmd, vmStatsNameMap);
        } catch (final XenAPIException e) {
            final String msg = "Unable to get VM stats" + e.toString();
            s_logger.warn(msg, e);
            return new GetVmStatsAnswer(cmd, vmStatsNameMap);
        } catch (final XmlRpcException e) {
            final String msg = "Unable to get VM stats" + e.getMessage();
            s_logger.warn(msg, e);
            return new GetVmStatsAnswer(cmd, vmStatsNameMap);
        }
    }

    public HashMap<String, VmStatsEntry> getVmStats(final Connection conn, final GetVmStatsCommand cmd, final List<String> vmUUIDs, final String hostGuid) {
        final HashMap<String, VmStatsEntry> vmResponseMap = new HashMap<String, VmStatsEntry>();

        for (final String vmUUID : vmUUIDs) {
            vmResponseMap.put(vmUUID, new VmStatsEntry(0, 0, 0, 0, "vm"));
        }

        final Object[] rrdData = getRRDData(conn, 2); // call rrddata with 2 for vm

        if (rrdData == null) {
            return null;
        }

        final Integer numRows = (Integer)rrdData[0];
        final Integer numColumns = (Integer)rrdData[1];
        final Node legend = (Node)rrdData[2];
        final Node dataNode = (Node)rrdData[3];

        final NodeList legendChildren = legend.getChildNodes();
        for (int col = 0; col < numColumns; col++) {

            if (legendChildren == null || legendChildren.item(col) == null) {
                continue;
            }

            final String columnMetadata = getXMLNodeValue(legendChildren.item(col));

            if (columnMetadata == null) {
                continue;
            }

            final String[] columnMetadataList = columnMetadata.split(":");

            if (columnMetadataList.length != 4) {
                continue;
            }

            final String type = columnMetadataList[1];
            final String uuid = columnMetadataList[2];
            final String param = columnMetadataList[3];

            if (type.equals("vm") && vmResponseMap.keySet().contains(uuid)) {
                final VmStatsEntry vmStatsAnswer = vmResponseMap.get(uuid);

                vmStatsAnswer.setEntityType("vm");

                if (param.contains("cpu")) {
                    vmStatsAnswer.setNumCPUs(vmStatsAnswer.getNumCPUs() + 1);
                    vmStatsAnswer.setCPUUtilization(vmStatsAnswer.getCPUUtilization() + getDataAverage(dataNode, col, numRows));
                } else if (param.matches("vif_\\d*_rx")) {
                    vmStatsAnswer.setNetworkReadKBs(vmStatsAnswer.getNetworkReadKBs() + getDataAverage(dataNode, col, numRows)/1000);
                } else if (param.matches("vif_\\d*_tx")) {
                    vmStatsAnswer.setNetworkWriteKBs(vmStatsAnswer.getNetworkWriteKBs() + getDataAverage(dataNode, col, numRows)/1000);
                } else if (param.matches("vbd_.*_read")) {
                    vmStatsAnswer.setDiskReadKBs(vmStatsAnswer.getDiskReadKBs() + getDataAverage(dataNode, col, numRows)/1000);
                } else if (param.matches("vbd_.*_write")) {
                    vmStatsAnswer.setDiskWriteKBs(vmStatsAnswer.getDiskWriteKBs() + getDataAverage(dataNode, col, numRows)/1000);
                }
            }
        }

        for (final Map.Entry<String, VmStatsEntry> entry: vmResponseMap.entrySet()) {
            final VmStatsEntry vmStatsAnswer = entry.getValue();

            if (vmStatsAnswer.getNumCPUs() != 0) {
                vmStatsAnswer.setCPUUtilization(vmStatsAnswer.getCPUUtilization() / vmStatsAnswer.getNumCPUs());
            }

            vmStatsAnswer.setCPUUtilization(vmStatsAnswer.getCPUUtilization() * 100);
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Vm cpu utilization " + vmStatsAnswer.getCPUUtilization());
            }
        }
        return vmResponseMap;
    }

    protected GetVmDiskStatsAnswer execute(final GetVmDiskStatsCommand cmd) {
        return new GetVmDiskStatsAnswer(cmd, null, null, null);
    }


    protected Document getStatsRawXML(final Connection conn, final boolean host) {
        final Date currentDate = new Date();
        String urlStr = "http://" + _host.getIp() + "/rrd_updates?";
        urlStr += "session_id=" + conn.getSessionReference();
        urlStr += "&host=" + (host ? "true" : "false");
        urlStr += "&cf=" + _consolidationFunction;
        urlStr += "&interval=" + _pollingIntervalInSeconds;
        urlStr += "&start=" + (currentDate.getTime() / 1000 - 1000 - 100);

        URL url;
        BufferedReader in = null;
        try {
            url = new URL(urlStr);
            url.openConnection();
            final URLConnection uc = url.openConnection();
            in = new BufferedReader(new InputStreamReader(uc.getInputStream()));
            final InputSource statsSource = new InputSource(in);
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(statsSource);
        } catch (final MalformedURLException e) {
            s_logger.warn("Malformed URL?  come on...." + urlStr);
            return null;
        } catch (final IOException e) {
            s_logger.warn("Problems getting stats using " + urlStr, e);
            return null;
        } catch (final SAXException e) {
            s_logger.warn("Problems getting stats using " + urlStr, e);
            return null;
        } catch (final ParserConfigurationException e) {
            s_logger.warn("Problems getting stats using " + urlStr, e);
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (final IOException e) {
                    s_logger.warn("Unable to close the buffer ", e);
                }
            }
        }
    }



    protected Object[] getRRDData(final Connection conn, final int flag) {

        /*
         * Note: 1 => called from host, hence host stats 2 => called from vm, hence vm stats
         */
        Document doc = null;

        try {
            doc = getStatsRawXML(conn, flag == 1 ? true : false);
        } catch (final Exception e1) {
            s_logger.warn("Error whilst collecting raw stats from plugin: ", e1);
            return null;
        }

        if (doc == null) {         //stats are null when the host plugin call fails (host down state)
            return null;
        }

        final NodeList firstLevelChildren = doc.getChildNodes();
        final NodeList secondLevelChildren = firstLevelChildren.item(0).getChildNodes();
        final Node metaNode = secondLevelChildren.item(0);
        final Node dataNode = secondLevelChildren.item(1);

        Integer numRows = 0;
        Integer numColumns = 0;
        Node legend = null;
        final NodeList metaNodeChildren = metaNode.getChildNodes();
        for (int i = 0; i < metaNodeChildren.getLength(); i++) {
            final Node n = metaNodeChildren.item(i);
            if (n.getNodeName().equals("rows")) {
                numRows = Integer.valueOf(getXMLNodeValue(n));
            } else if (n.getNodeName().equals("columns")) {
                numColumns = Integer.valueOf(getXMLNodeValue(n));
            } else if (n.getNodeName().equals("legend")) {
                legend = n;
            }
        }

        return new Object[] { numRows, numColumns, legend, dataNode };
    }

    protected String getXMLNodeValue(final Node n) {
        return n.getChildNodes().item(0).getNodeValue();
    }

    protected double getDataAverage(final Node dataNode, final int col, final int numRows) {
        double value = 0;
        final double dummy = 0;
        int numRowsUsed = 0;
        for (int row = 0; row < numRows; row++) {
            final Node data = dataNode.getChildNodes().item(numRows - 1 - row).getChildNodes().item(col + 1);
            final Double currentDataAsDouble = Double.valueOf(getXMLNodeValue(data));
            if (!currentDataAsDouble.equals(Double.NaN)) {
                numRowsUsed += 1;
                value += currentDataAsDouble;
            }
        }

        if (numRowsUsed == 0) {
            if (!Double.isInfinite(value) && !Double.isNaN(value)) {
                return value;
            } else {
                s_logger.warn("Found an invalid value (infinity/NaN) in getDataAverage(), numRows=0");
                return dummy;
            }
        } else {
            if (!Double.isInfinite(value / numRowsUsed) && !Double.isNaN(value / numRowsUsed)) {
                return value / numRowsUsed;
            } else {
                s_logger.warn("Found an invalid value (infinity/NaN) in getDataAverage(), numRows>0");
                return dummy;
            }
        }

    }

    private static PowerState convertToPowerState(final VmPowerState ps) {
        final PowerState powerState = s_powerStatesTable.get(ps);
        return powerState == null ?  PowerState.PowerUnknown : powerState;
    }

    protected HashMap<String, HostVmStateReportEntry> getHostVmStateReport(final Connection conn) {

        // TODO : new VM sync model does not require a cluster-scope report, we need to optimize
        // the report accordingly
        final HashMap<String, HostVmStateReportEntry> vmStates = new HashMap<String, HostVmStateReportEntry>();
        Map<VM, VM.Record> vm_map = null;
        for (int i = 0; i < 2; i++) {
            try {
                vm_map = VM.getAllRecords(conn);  //USE THIS TO GET ALL VMS FROM  A CLUSTER
                break;
            } catch (final Throwable e) {
                s_logger.warn("Unable to get vms", e);
            }
            try {
                Thread.sleep(1000);
            } catch (final InterruptedException ex) {

            }
        }

        if (vm_map == null) {
            return vmStates;
        }
        for (final VM.Record record : vm_map.values()) {
            if (record.isControlDomain || record.isASnapshot || record.isATemplate) {
                continue; // Skip DOM0
            }

            final VmPowerState ps = record.powerState;
            final Host host = record.residentOn;
            String host_uuid = null;
            if (!isRefNull(host)) {
                try {
                    host_uuid = host.getUuid(conn);
                } catch (final BadServerResponse e) {
                    s_logger.error("Failed to get host uuid for host " + host.toWireString(), e);
                } catch (final XenAPIException e) {
                    s_logger.error("Failed to get host uuid for host " + host.toWireString(), e);
                } catch (final XmlRpcException e) {
                    s_logger.error("Failed to get host uuid for host " + host.toWireString(), e);
                }

                if (host_uuid.equalsIgnoreCase(_host.getUuid())) {
                    vmStates.put(
                            record.nameLabel,
                            new HostVmStateReportEntry(convertToPowerState(ps), host_uuid)
                            );
                }
            }
        }

        return vmStates;
    }

    public PowerState getVmState(final Connection conn, final String vmName) {
        int retry = 3;
        while (retry-- > 0) {
            try {
                final Set<VM> vms = VM.getByNameLabel(conn, vmName);
                for (final VM vm : vms) {
                    return convertToPowerState(vm.getPowerState(conn));
                }
            } catch (final BadServerResponse e) {
                // There is a race condition within xenserver such that if a vm is
                // deleted and we
                // happen to ask for it, it throws this stupid response. So
                // if this happens,
                // we take a nap and try again which then avoids the race
                // condition because
                // the vm's information is now cleaned up by xenserver. The error
                // is as follows
                // com.xensource.xenapi.Types$BadServerResponse
                // [HANDLE_INVALID, VM,
                // 3dde93f9-c1df-55a7-2cde-55e1dce431ab]
                s_logger.info("Unable to get a vm PowerState due to " + e.toString() + ". We are retrying.  Count: " + retry);
                try {
                    Thread.sleep(3000);
                } catch (final InterruptedException ex) {

                }
            } catch (final XenAPIException e) {
                final String msg = "Unable to get a vm PowerState due to " + e.toString();
                s_logger.warn(msg, e);
                break;
            } catch (final XmlRpcException e) {
                final String msg = "Unable to get a vm PowerState due to " + e.getMessage();
                s_logger.warn(msg, e);
                break;
            }
        }

        return PowerState.PowerOff;
    }

    protected CheckVirtualMachineAnswer execute(final CheckVirtualMachineCommand cmd) {
        final Connection conn = getConnection();
        final String vmName = cmd.getVmName();
        final PowerState powerState = getVmState(conn, vmName);
        final Integer vncPort = null;
        if (powerState == PowerState.PowerOn) {
            s_logger.debug("3. The VM " + vmName + " is in Running state");
        }

        return new CheckVirtualMachineAnswer(cmd, powerState, vncPort);
    }

    protected PrepareForMigrationAnswer execute(final PrepareForMigrationCommand cmd) {
        final Connection conn = getConnection();

        final VirtualMachineTO vm = cmd.getVirtualMachine();
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Preparing host for migrating " + vm);
        }

        final NicTO[] nics = vm.getNics();
        try {
            prepareISO(conn, vm.getName());

            for (final NicTO nic : nics) {
                getNetwork(conn, nic);
            }
            s_logger.debug("4. The VM " + vm.getName() + " is in Migrating state");

            return new PrepareForMigrationAnswer(cmd);
        } catch (final Exception e) {
            s_logger.warn("Catch Exception " + e.getClass().getName() + " prepare for migration failed due to " + e.toString(), e);
            return new PrepareForMigrationAnswer(cmd, e);
        }
    }

    public String upgradeSnapshot(final Connection conn, final String templatePath, final String snapshotPath) {
        final String results = callHostPluginAsync(conn, "vmopspremium", "upgrade_snapshot", 2 * 60 * 60, "templatePath", templatePath, "snapshotPath", snapshotPath);

        if (results == null || results.isEmpty()) {
            final String msg = "upgrade_snapshot return null";
            s_logger.warn(msg);
            throw new CloudRuntimeException(msg);
        }
        final String[] tmp = results.split("#");
        final String status = tmp[0];
        if (status.equals("0")) {
            return results;
        } else {
            s_logger.warn(results);
            throw new CloudRuntimeException(results);
        }
    }

    String createTemplateFromSnapshot(final Connection conn, final String templatePath, final String snapshotPath, final int wait) {
        final String tmpltLocalDir = UUID.randomUUID().toString();
        final String results =
                callHostPluginAsync(conn, "vmopspremium", "create_privatetemplate_from_snapshot", wait, "templatePath", templatePath, "snapshotPath", snapshotPath,
                        "tmpltLocalDir", tmpltLocalDir);
        String errMsg = null;
        if (results == null || results.isEmpty()) {
            errMsg = "create_privatetemplate_from_snapshot return null";
        } else {
            final String[] tmp = results.split("#");
            final String status = tmp[0];
            if (status.equals("0")) {
                return results;
            } else {
                errMsg = "create_privatetemplate_from_snapshot failed due to " + tmp[1];
            }
        }
        final String source = "cloud_mount/" + tmpltLocalDir;
        killCopyProcess(conn, source);
        s_logger.warn(errMsg);
        throw new CloudRuntimeException(errMsg);
    }

    boolean killCopyProcess(final Connection conn, final String nameLabel) {
        final String results = callHostPluginAsync(conn, "vmops", "kill_copy_process", 60, "namelabel", nameLabel);
        String errMsg = null;
        if (results == null || results.equals("false")) {
            errMsg = "kill_copy_process failed";
            s_logger.warn(errMsg);
            return false;
        } else {
            return true;
        }
    }

    void destroyVDIbyNameLabel(final Connection conn, final String nameLabel) {
        try {
            final Set<VDI> vdis = VDI.getByNameLabel(conn, nameLabel);
            if (vdis.size() != 1) {
                s_logger.warn("destoryVDIbyNameLabel failed due to there are " + vdis.size() + " VDIs with name " + nameLabel);
                return;
            }
            for (final VDI vdi : vdis) {
                try {
                    vdi.destroy(conn);
                } catch (final Exception e) {
                    final String msg = "Failed to destroy VDI : " + nameLabel + "due to " + e.toString() + "\n Force deleting VDI using system 'rm' command";
                    s_logger.warn(msg);
                    try {
                        final String srUUID = vdi.getSR(conn).getUuid(conn);
                        final String vdiUUID = vdi.getUuid(conn);
                        final String vdifile = "/var/run/sr-mount/" + srUUID + "/" + vdiUUID + ".vhd";
                        final String results = callHostPluginAsync(conn, "vmopspremium", "remove_corrupt_vdi", 10, "vdifile", vdifile);
                    } catch (final Exception e2) {
                        s_logger.warn(e2);
                    }
                }
            }
        } catch (final Exception e) {
        }
    }

    public String copyVhdFromSecondaryStorage(final Connection conn, final String mountpoint, final String sruuid, final int wait) {
        final String nameLabel = "cloud-" + UUID.randomUUID().toString();
        final String results =
                callHostPluginAsync(conn, "vmopspremium", "copy_vhd_from_secondarystorage", wait, "mountpoint", mountpoint, "sruuid", sruuid, "namelabel", nameLabel);
        String errMsg = null;
        if (results == null || results.isEmpty()) {
            errMsg = "copy_vhd_from_secondarystorage return null";
        } else {
            final String[] tmp = results.split("#");
            final String status = tmp[0];
            if (status.equals("0")) {
                return tmp[1];
            } else {
                errMsg = tmp[1];
            }
        }
        final String source = mountpoint.substring(mountpoint.lastIndexOf('/') + 1);
        if (killCopyProcess(conn, source)) {
            destroyVDIbyNameLabel(conn, nameLabel);
        }
        s_logger.warn(errMsg);
        throw new CloudRuntimeException(errMsg);
    }

    public PrimaryStorageDownloadAnswer execute(final PrimaryStorageDownloadCommand cmd) {
        final String tmplturl = cmd.getUrl();
        final String poolName = cmd.getPoolUuid();
        final int wait = cmd.getWait();
        try {
            final URI uri = new URI(tmplturl);
            final String tmplpath = uri.getHost() + ":" + uri.getPath();
            final Connection conn = getConnection();
            SR poolsr = null;
            final Set<SR> srs = SR.getByNameLabel(conn, poolName);
            if (srs.size() != 1) {
                final String msg = "There are " + srs.size() + " SRs with same name: " + poolName;
                s_logger.warn(msg);
                return new PrimaryStorageDownloadAnswer(msg);
            } else {
                poolsr = srs.iterator().next();
            }
            final String pUuid = poolsr.getUuid(conn);
            final boolean isISCSI = IsISCSI(poolsr.getType(conn));
            final String uuid = copyVhdFromSecondaryStorage(conn, tmplpath, pUuid, wait);
            final VDI tmpl = getVDIbyUuid(conn, uuid);
            final VDI snapshotvdi = tmpl.snapshot(conn, new HashMap<String, String>());
            final String snapshotUuid = snapshotvdi.getUuid(conn);
            snapshotvdi.setNameLabel(conn, "Template " + cmd.getName());
            final String parentuuid = getVhdParent(conn, pUuid, snapshotUuid, isISCSI);
            final VDI parent = getVDIbyUuid(conn, parentuuid);
            final Long phySize = parent.getPhysicalUtilisation(conn);
            tmpl.destroy(conn);
            poolsr.scan(conn);
            try {
                Thread.sleep(5000);
            } catch (final Exception e) {
            }
            return new PrimaryStorageDownloadAnswer(snapshotvdi.getUuid(conn), phySize);
        } catch (final Exception e) {
            final String msg = "Catch Exception " + e.getClass().getName() + " on host:" + _host.getUuid() + " for template: " + tmplturl + " due to " + e.toString();
            s_logger.warn(msg, e);
            return new PrimaryStorageDownloadAnswer(msg);
        }
    }

    protected String removeSRSync(final Connection conn, final SR sr) {
        if (sr == null) {
            return null;
        }
        if (s_logger.isDebugEnabled()) {
            s_logger.debug(logX(sr, "Removing SR"));
        }
        long waittime = 0;
        try {
            final Set<VDI> vdis = sr.getVDIs(conn);
            for (final VDI vdi : vdis) {
                final Map<java.lang.String, Types.VdiOperations> currentOperation = vdi.getCurrentOperations(conn);
                if (currentOperation == null || currentOperation.size() == 0) {
                    continue;
                }
                if (waittime >= 1800000) {
                    final String msg = "This template is being used, try late time";
                    s_logger.warn(msg);
                    return msg;
                }
                waittime += 30000;
                try {
                    Thread.sleep(30000);
                } catch (final InterruptedException ex) {
                }
            }
            removeSR(conn, sr);
            return null;
        } catch (final XenAPIException e) {
            s_logger.warn(logX(sr, "Unable to get current opertions " + e.toString()), e);
        } catch (final XmlRpcException e) {
            s_logger.warn(logX(sr, "Unable to get current opertions " + e.getMessage()), e);
        }
        final String msg = "Remove SR failed";
        s_logger.warn(msg);
        return msg;

    }

    public void removeSR(final Connection conn, final SR sr) {
        if (sr == null) {
            return;
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug(logX(sr, "Removing SR"));
        }

        for (int i = 0; i < 2; i++) {
            try {
                final Set<VDI> vdis = sr.getVDIs(conn);
                for (final VDI vdi : vdis) {
                    vdi.forget(conn);
                }

                Set<PBD> pbds = sr.getPBDs(conn);
                for (final PBD pbd : pbds) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug(logX(pbd, "Unplugging pbd"));
                    }

                    // if (pbd.getCurrentlyAttached(conn)) {
                    pbd.unplug(conn);
                    //}

                    pbd.destroy(conn);
                }

                pbds = sr.getPBDs(conn);

                if (pbds.size() == 0) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug(logX(sr, "Forgetting"));
                    }

                    sr.forget(conn);

                    return;
                }

                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(logX(sr, "There is still one or more PBDs attached."));

                    if (s_logger.isTraceEnabled()) {
                        for (final PBD pbd : pbds) {
                            s_logger.trace(logX(pbd, " Still attached"));
                        }
                    }
                }
            } catch (final XenAPIException e) {
                s_logger.debug(logX(sr, "Catch XenAPIException: " + e.toString()));
            } catch (final XmlRpcException e) {
                s_logger.debug(logX(sr, "Catch Exception: " + e.getMessage()));
            }
        }

        s_logger.warn(logX(sr, "Unable to remove SR"));
    }

    protected MigrateAnswer execute(final MigrateCommand cmd) {
        final Connection conn = getConnection();
        final String vmName = cmd.getVmName();

        try {
            final Set<VM> vms = VM.getByNameLabel(conn, vmName);

            final String ipaddr = cmd.getDestinationIp();

            final Set<Host> hosts = Host.getAll(conn);
            Host dsthost = null;
            if(hosts != null) {
                for (final Host host : hosts) {
                    if (host.getAddress(conn).equals(ipaddr)) {
                        dsthost = host;
                        break;
                    }
                }
            }
            if (dsthost == null) {
                final String msg = "Migration failed due to unable to find host " + ipaddr + " in XenServer pool " + _host.getPool();
                s_logger.warn(msg);
                return new MigrateAnswer(cmd, false, msg, null);
            }
            for (final VM vm : vms) {
                final Set<VBD> vbds = vm.getVBDs(conn);
                for (final VBD vbd : vbds) {
                    final VBD.Record vbdRec = vbd.getRecord(conn);
                    if (vbdRec.type.equals(Types.VbdType.CD) && !vbdRec.empty) {
                        vbd.eject(conn);
                        break;
                    }
                }
                migrateVM(conn, dsthost, vm, vmName);
                vm.setAffinity(conn, dsthost);
            }
            return new MigrateAnswer(cmd, true, "migration succeeded", null);
        } catch (final Exception e) {
            s_logger.warn(e.getMessage(), e);
            return new MigrateAnswer(cmd, false, e.getMessage(), null);
        }

    }

    protected Pair<VM, VM.Record> getControlDomain(final Connection conn) throws XenAPIException, XmlRpcException {
        final Host host = Host.getByUuid(conn, _host.getUuid());
        Set<VM> vms = null;
        vms = host.getResidentVMs(conn);
        for (final VM vm : vms) {
            if (vm.getIsControlDomain(conn)) {
                return new Pair<VM, VM.Record>(vm, vm.getRecord(conn));
            }
        }

        throw new CloudRuntimeException("Com'on no control domain?  What the crap?!#@!##$@");
    }

    public void umountSnapshotDir(final Connection conn, final Long dcId) {
        try {
            callHostPlugin(conn, "vmopsSnapshot", "unmountSnapshotsDir", "dcId", dcId.toString());
        } catch (final Exception e) {
            s_logger.debug("Failed to umount snapshot dir",e);
        }
    }

    protected ReadyAnswer execute(final ReadyCommand cmd) {
        final Connection conn = getConnection();
        final Long dcId = cmd.getDataCenterId();
        // Ignore the result of the callHostPlugin. Even if unmounting the
        // snapshots dir fails, let Ready command
        // succeed.
        umountSnapshotDir(conn, dcId);

        setupLinkLocalNetwork(conn);
        // try to destroy CD-ROM device for all system VMs on this host
        try {
            final Host host = Host.getByUuid(conn, _host.getUuid());
            final Set<VM> vms = host.getResidentVMs(conn);
            for (final VM vm : vms) {
                destroyPatchVbd(conn, vm.getNameLabel(conn));
            }
        } catch (final Exception e) {
        }
        try {
            final boolean result = cleanupHaltedVms(conn);
            if (!result) {
                return new ReadyAnswer(cmd, "Unable to cleanup halted vms");
            }
        } catch (final XenAPIException e) {
            s_logger.warn("Unable to cleanup halted vms", e);
            return new ReadyAnswer(cmd, "Unable to cleanup halted vms");
        } catch (final XmlRpcException e) {
            s_logger.warn("Unable to cleanup halted vms", e);
            return new ReadyAnswer(cmd, "Unable to cleanup halted vms");
        }

        return new ReadyAnswer(cmd);
    }

    public String getVncUrl(final Connection conn, final VM vm) {
        VM.Record record;
        Console c;
        try {
            record = vm.getRecord(conn);
            final Set<Console> consoles = record.consoles;

            if (consoles.isEmpty()) {
                s_logger.warn("There are no Consoles available to the vm : " + record.nameDescription);
                return null;
            }
            final Iterator<Console> i = consoles.iterator();
            while (i.hasNext()) {
                c = i.next();
                if (c.getProtocol(conn) == Types.ConsoleProtocol.RFB) {
                    return c.getLocation(conn);
                }
            }
        } catch (final XenAPIException e) {
            final String msg = "Unable to get console url due to " + e.toString();
            s_logger.warn(msg, e);
            return null;
        } catch (final XmlRpcException e) {
            final String msg = "Unable to get console url due to " + e.getMessage();
            s_logger.warn(msg, e);
            return null;
        }
        return null;
    }

    @Override
    public RebootAnswer execute(final RebootCommand cmd) {
        final Connection conn = getConnection();
        s_logger.debug("7. The VM " + cmd.getVmName() + " is in Starting state");
        try {
            Set<VM> vms = null;
            try {
                vms = VM.getByNameLabel(conn, cmd.getVmName());
            } catch (final XenAPIException e0) {
                s_logger.debug("getByNameLabel failed " + e0.toString());
                return new RebootAnswer(cmd, "getByNameLabel failed " + e0.toString(), false);
            } catch (final Exception e0) {
                s_logger.debug("getByNameLabel failed " + e0.getMessage());
                return new RebootAnswer(cmd, "getByNameLabel failed", false);
            }
            for (final VM vm : vms) {
                try {
                    rebootVM(conn, vm, vm.getNameLabel(conn));
                } catch (final Exception e) {
                    final String msg = e.toString();
                    s_logger.warn(msg, e);
                    return new RebootAnswer(cmd, msg, false);
                }
            }
            return new RebootAnswer(cmd, "reboot succeeded", true);
        } finally {
            s_logger.debug("8. The VM " + cmd.getVmName() + " is in Running state");
        }
    }

    public Answer execute(final RebootRouterCommand cmd) {
        final Connection conn = getConnection();
        final RebootAnswer answer = execute((RebootCommand)cmd);
        if (answer.getResult()) {
            final String cnct = connect(conn, cmd.getVmName(), cmd.getPrivateIpAddress());
            networkUsage(conn, cmd.getPrivateIpAddress(), "create", null);
            if (cnct == null) {
                return answer;
            } else {
                return new Answer(cmd, false, cnct);
            }
        }
        return answer;
    }

    protected void startvmfailhandle(final Connection conn, final VM vm, final List<Ternary<SR, VDI, VolumeVO>> mounts) {
        if (vm != null) {
            try {

                if (vm.getPowerState(conn) == VmPowerState.RUNNING) {
                    try {
                        vm.hardShutdown(conn);
                    } catch (final Exception e) {
                        final String msg = "VM hardshutdown failed due to " + e.toString();
                        s_logger.warn(msg, e);
                    }
                }
                if (vm.getPowerState(conn) == VmPowerState.HALTED) {
                    try {
                        vm.destroy(conn);
                    } catch (final Exception e) {
                        final String msg = "VM destroy failed due to " + e.toString();
                        s_logger.warn(msg, e);
                    }
                }
            } catch (final Exception e) {
                final String msg = "VM getPowerState failed due to " + e.toString();
                s_logger.warn(msg, e);
            }
        }
        if (mounts != null) {
            for (final Ternary<SR, VDI, VolumeVO> mount : mounts) {
                final VDI vdi = mount.second();
                Set<VBD> vbds = null;
                try {
                    vbds = vdi.getVBDs(conn);
                } catch (final Exception e) {
                    final String msg = "VDI getVBDS failed due to " + e.toString();
                    s_logger.warn(msg, e);
                    continue;
                }
                for (final VBD vbd : vbds) {
                    try {
                        vbd.unplug(conn);
                        vbd.destroy(conn);
                    } catch (final Exception e) {
                        final String msg = "VBD destroy failed due to " + e.toString();
                        s_logger.warn(msg, e);
                    }
                }
            }
        }
    }

    /**
     * WARN: static-min <= dynamic-min <= dynamic-max <= static-max
     * @see XcpServerResource#setMemory(com.xensource.xenapi.Connection, com.xensource.xenapi.VM, long, long)
     * @param conn
     * @param vm
     * @param minMemsize
     * @param maxMemsize
     * @throws XmlRpcException
     * @throws XenAPIException
     */
    protected void setMemory(final Connection conn, final VM vm, final long minMemsize, final long maxMemsize) throws XmlRpcException, XenAPIException {
        vm.setMemoryLimits(conn, mem_128m, maxMemsize, minMemsize, maxMemsize);
    }

    /**
     * When Dynamic Memory Control (DMC) is enabled -
     * xenserver allows scaling the guest memory while the guest is running
     *
     * By default this is disallowed, override the specific xenserver resource
     * if this is enabled
     */
    protected boolean isDmcEnabled(final Connection conn, final Host host) throws XenAPIException, XmlRpcException {
        return false;
    }

    protected void waitForTask(final Connection c, final Task task, final long pollInterval, final long timeout) throws XenAPIException, XmlRpcException, TimeoutException {
        final long beginTime = System.currentTimeMillis();
        if (s_logger.isTraceEnabled()) {
            s_logger.trace("Task " + task.getNameLabel(c) + " (" + task.getUuid(c) + ") sent to " + c.getSessionReference() + " is pending completion with a " + timeout +
                    "ms timeout");
        }
        while (task.getStatus(c) == Types.TaskStatusType.PENDING) {
            try {
                if (s_logger.isTraceEnabled()) {
                    s_logger.trace("Task " + task.getNameLabel(c) + " (" + task.getUuid(c) + ") is pending, sleeping for " + pollInterval + "ms");
                }
                Thread.sleep(pollInterval);
            } catch (final InterruptedException e) {
            }
            if (System.currentTimeMillis() - beginTime > timeout) {
                final String msg = "Async " + timeout / 1000 + " seconds timeout for task " + task.toString();
                s_logger.warn(msg);
                task.cancel(c);
                task.destroy(c);
                throw new TimeoutException(msg);
            }
        }
    }

    protected void checkForSuccess(final Connection c, final Task task) throws XenAPIException, XmlRpcException {
        if (task.getStatus(c) == Types.TaskStatusType.SUCCESS) {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Task " + task.getNameLabel(c) + " (" + task.getUuid(c) + ") completed");
            }
            return;
        } else {
            final String msg = "Task failed! Task record: " + task.getRecord(c);
            s_logger.warn(msg);
            task.cancel(c);
            task.destroy(c);
            throw new Types.BadAsyncResult(msg);
        }
    }

    public void rebootVM(final Connection conn, final VM vm, final String vmName) throws Exception {
        Task task = null;
        try {
            task = vm.cleanRebootAsync(conn);
            try {
                //poll every 1 seconds , timeout after 10 minutes
                waitForTask(conn, task, 1000, 10 * 60 * 1000);
                checkForSuccess(conn, task);
            } catch (final Types.HandleInvalid e) {
                if (vm.getPowerState(conn) == VmPowerState.RUNNING) {
                    task = null;
                    return;
                }
                throw new CloudRuntimeException("Reboot VM catch HandleInvalid and VM is not in RUNNING state");
            }
        } catch (final XenAPIException e) {
            s_logger.debug("Unable to Clean Reboot VM(" + vmName + ") on host(" + _host.getUuid() + ") due to " + e.toString() + ", try hard reboot");
            try {
                vm.hardReboot(conn);
            } catch (final Exception e1) {
                final String msg = "Unable to hard Reboot VM(" + vmName + ") on host(" + _host.getUuid() + ") due to " + e.toString();
                s_logger.warn(msg, e1);
                throw new CloudRuntimeException(msg);
            }
        } finally {
            if (task != null) {
                try {
                    task.destroy(conn);
                } catch (final Exception e1) {
                    s_logger.debug("unable to destroy task(" + task.toString() + ") on host(" + _host.getUuid() + ") due to " + e1.toString());
                }
            }
        }
    }

    void forceShutdownVM(final Connection conn, final VM vm) {
        try {
            final Long domId = vm.getDomid(conn);
            callHostPlugin(conn, "vmopspremium", "forceShutdownVM", "domId", domId.toString());
            vm.powerStateReset(conn);
            vm.destroy(conn);
        } catch (final Exception e) {
            final String msg = "forceShutdown failed due to " + e.toString();
            s_logger.warn(msg, e);
            throw new CloudRuntimeException(msg);
        }
    }

    public void shutdownVM(final Connection conn, final VM vm, final String vmName) throws XmlRpcException {
        Task task = null;
        try {
            task = vm.cleanShutdownAsync(conn);
            try {
                //poll every 1 seconds , timeout after 10 minutes
                waitForTask(conn, task, 1000, 10 * 60 * 1000);
                checkForSuccess(conn, task);
            } catch (final TimeoutException e) {
                if (vm.getPowerState(conn) == VmPowerState.HALTED) {
                    task = null;
                    return;
                }
                throw new CloudRuntimeException("Shutdown VM catch HandleInvalid and VM is not in HALTED state");
            }
        } catch (final XenAPIException e) {
            s_logger.debug("Unable to cleanShutdown VM(" + vmName + ") on host(" + _host.getUuid() + ") due to " + e.toString());
            try {
                VmPowerState state = vm.getPowerState(conn);
                if (state == VmPowerState.RUNNING) {
                    try {
                        vm.hardShutdown(conn);
                    } catch (final Exception e1) {
                        s_logger.debug("Unable to hardShutdown VM(" + vmName + ") on host(" + _host.getUuid() + ") due to " + e.toString());
                        state = vm.getPowerState(conn);
                        if (state == VmPowerState.RUNNING) {
                            forceShutdownVM(conn, vm);
                        }
                        return;
                    }
                } else if (state == VmPowerState.HALTED) {
                    return;
                } else {
                    final String msg = "After cleanShutdown the VM status is " + state.toString() + ", that is not expected";
                    s_logger.warn(msg);
                    throw new CloudRuntimeException(msg);
                }
            } catch (final Exception e1) {
                final String msg = "Unable to hardShutdown VM(" + vmName + ") on host(" + _host.getUuid() + ") due to " + e.toString();
                s_logger.warn(msg, e1);
                throw new CloudRuntimeException(msg);
            }
        } finally {
            if (task != null) {
                try {
                    task.destroy(conn);
                } catch (final Exception e1) {
                    s_logger.debug("unable to destroy task(" + task.toString() + ") on host(" + _host.getUuid() + ") due to " + e1.toString());
                }
            }
        }
    }

    public void startVM(final Connection conn, final Host host, final VM vm, final String vmName) throws Exception {
        Task task = null;
        try {
            task = vm.startOnAsync(conn, host, false, true);
            try {
                //poll every 1 seconds , timeout after 10 minutes
                waitForTask(conn, task, 1000, 10 * 60 * 1000);
                checkForSuccess(conn, task);
            } catch (final Types.HandleInvalid e) {
                if (vm.getPowerState(conn) == VmPowerState.RUNNING) {
                    s_logger.debug("VM " + vmName + " is in Running status");
                    task = null;
                    return;
                }
                throw new CloudRuntimeException("Start VM " + vmName + " catch HandleInvalid and VM is not in RUNNING state");
            } catch (final TimeoutException e) {
                if (vm.getPowerState(conn) == VmPowerState.RUNNING) {
                    s_logger.debug("VM " + vmName + " is in Running status");
                    task = null;
                    return;
                }
                throw new CloudRuntimeException("Start VM " + vmName + " catch BadAsyncResult and VM is not in RUNNING state");
            }
        } catch (final XenAPIException e) {
            final String msg = "Unable to start VM(" + vmName + ") on host(" + _host.getUuid() + ") due to " + e.toString();
            s_logger.warn(msg, e);
            throw new CloudRuntimeException(msg);
        } finally {
            if (task != null) {
                try {
                    task.destroy(conn);
                } catch (final Exception e1) {
                    s_logger.debug("unable to destroy task(" + task.toString() + ") on host(" + _host.getUuid() + ") due to " + e1.toString());
                }
            }
        }
    }

    public void migrateVM(final Connection conn, final Host destHost, final VM vm, final String vmName) throws Exception {
        Task task = null;
        try {
            final Map<String, String> other = new HashMap<String, String>();
            other.put("live", "true");
            task = vm.poolMigrateAsync(conn, destHost, other);
            try {
                // poll every 1 seconds
                final long timeout = _migratewait * 1000L;
                waitForTask(conn, task, 1000, timeout);
                checkForSuccess(conn, task);
            } catch (final Types.HandleInvalid e) {
                if (vm.getResidentOn(conn).equals(destHost)) {
                    task = null;
                    return;
                }
                throw new CloudRuntimeException("migrate VM catch HandleInvalid and VM is not running on dest host");
            }
        } catch (final XenAPIException e) {
            final String msg = "Unable to migrate VM(" + vmName + ") from host(" + _host.getUuid() + ")";
            s_logger.warn(msg, e);
            throw new CloudRuntimeException(msg);
        } finally {
            if (task != null) {
                try {
                    task.destroy(conn);
                } catch (final Exception e1) {
                    s_logger.debug("unable to destroy task(" + task.toString() + ") on host(" + _host.getUuid() + ") due to " + e1.toString());
                }
            }
        }
    }

    protected VDI cloudVDIcopy(final Connection conn, final VDI vdi, final SR sr, int wait) throws Exception {
        Task task = null;
        if (wait == 0) {
            wait = 2 * 60 * 60;
        }
        try {
            task = vdi.copyAsync(conn, sr);
            // poll every 1 seconds , timeout after 2 hours
            waitForTask(conn, task, 1000, (long)wait * 1000);
            checkForSuccess(conn, task);
            final VDI dvdi = Types.toVDI(task, conn);
            return dvdi;
        } finally {
            if (task != null) {
                try {
                    task.destroy(conn);
                } catch (final Exception e) {
                    s_logger.debug("unable to destroy task(" + task.toString() + ") on host(" + _host.getUuid() + ") due to " + e.toString());
                }
            }
        }
    }

    protected String callHostPluginAsync(final Connection conn, final String plugin, final String cmd, final int wait, final String... params) {
        final int timeout = wait * 1000;
        final Map<String, String> args = new HashMap<String, String>();
        Task task = null;
        try {
            for (int i = 0; i < params.length; i += 2) {
                args.put(params[i], params[i + 1]);
            }
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("callHostPlugin executing for command " + cmd + " with " + getArgsString(args));
            }
            final Host host = Host.getByUuid(conn, _host.getUuid());
            task = host.callPluginAsync(conn, plugin, cmd, args);
            // poll every 1 seconds
            waitForTask(conn, task, 1000, timeout);
            checkForSuccess(conn, task);
            final String result = task.getResult(conn);
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("callHostPlugin Result: " + result);
            }
            return result.replace("<value>", "").replace("</value>", "").replace("\n", "");
        } catch (final Types.HandleInvalid e) {
            s_logger.warn("callHostPlugin failed for cmd: " + cmd + " with args " + getArgsString(args) + " due to HandleInvalid clazz:" + e.clazz + ", handle:" +
                    e.handle);
        } catch (final XenAPIException e) {
            s_logger.warn("callHostPlugin failed for cmd: " + cmd + " with args " + getArgsString(args) + " due to " + e.toString(), e);
        } catch (final Exception e) {
            s_logger.warn("callHostPlugin failed for cmd: " + cmd + " with args " + getArgsString(args) + " due to " + e.getMessage(), e);
        } finally {
            if (task != null) {
                try {
                    task.destroy(conn);
                } catch (final Exception e1) {
                    s_logger.debug("unable to destroy task(" + task.toString() + ") on host(" + _host.getUuid() + ") due to " + e1.toString());
                }
            }
        }
        return null;
    }

    @Override
    public StopAnswer execute(final StopCommand cmd) {
        final String vmName = cmd.getVmName();
        String platformstring = null;
        try {
            final Connection conn = getConnection();
            final Set<VM> vms = VM.getByNameLabel(conn, vmName);
            // stop vm which is running on this host or is in halted state
            final Iterator<VM> iter = vms.iterator();
            while (iter.hasNext()) {
                final VM vm = iter.next();
                final VM.Record vmr = vm.getRecord(conn);
                if (vmr.powerState != VmPowerState.RUNNING) {
                    continue;
                }
                if (isRefNull(vmr.residentOn)) {
                    continue;
                }
                if (vmr.residentOn.getUuid(conn).equals(_host.getUuid())) {
                    continue;
                }
                iter.remove();
            }

            if (vms.size() == 0) {
                return new StopAnswer(cmd, "VM does not exist", true);
            }
            for (final VM vm : vms) {
                final VM.Record vmr = vm.getRecord(conn);
                platformstring = StringUtils.mapToString(vmr.platform);
                if (vmr.isControlDomain) {
                    final String msg = "Tring to Shutdown control domain";
                    s_logger.warn(msg);
                    return new StopAnswer(cmd, msg, false);
                }

                if (vmr.powerState == VmPowerState.RUNNING && !isRefNull(vmr.residentOn) && !vmr.residentOn.getUuid(conn).equals(_host.getUuid())) {
                    final String msg = "Stop Vm " + vmName + " failed due to this vm is not running on this host: " + _host.getUuid() + " but host:" + vmr.residentOn.getUuid(conn);
                    s_logger.warn(msg);
                    return new StopAnswer(cmd, msg, platformstring, false);
                }

                if (cmd.checkBeforeCleanup() && vmr.powerState == VmPowerState.RUNNING) {
                    final String msg = "Vm " + vmName + " is running on host and checkBeforeCleanup flag is set, so bailing out";
                    s_logger.debug(msg);
                    return new StopAnswer(cmd, msg, false);
                }

                s_logger.debug("9. The VM " + vmName + " is in Stopping state");

                try {
                    if (vmr.powerState == VmPowerState.RUNNING) {
                        /* when stop a vm, set affinity to current xenserver */
                        vm.setAffinity(conn, vm.getResidentOn(conn));

                        if (_canBridgeFirewall) {
                            final String result = callHostPlugin(conn, "vmops", "destroy_network_rules_for_vm", "vmName", cmd.getVmName());
                            if (result == null || result.isEmpty() || !Boolean.parseBoolean(result)) {
                                s_logger.warn("Failed to remove  network rules for vm " + cmd.getVmName());
                            } else {
                                s_logger.info("Removed  network rules for vm " + cmd.getVmName());
                            }
                        }
                        shutdownVM(conn, vm, vmName);
                    }
                } catch (final Exception e) {
                    final String msg = "Catch exception " + e.getClass().getName() + " when stop VM:" + cmd.getVmName() + " due to " + e.toString();
                    s_logger.debug(msg);
                    return new StopAnswer(cmd, msg, platformstring, false);
                } finally {

                    try {
                        if (vm.getPowerState(conn) == VmPowerState.HALTED) {
                            Set<VGPU> vGPUs = null;
                            // Get updated GPU details
                            try {
                                vGPUs = vm.getVGPUs(conn);
                            } catch (final XenAPIException e2) {
                                s_logger.debug("VM " + vmName + " does not have GPU support.");
                            }
                            if (vGPUs != null && !vGPUs.isEmpty()) {
                                final HashMap<String, HashMap<String, VgpuTypesInfo>> groupDetails = getGPUGroupDetails(conn);
                                cmd.setGpuDevice(new GPUDeviceTO(null, null, groupDetails));
                            }

                            final Set<VIF> vifs = vm.getVIFs(conn);
                            final List<Network> networks = new ArrayList<Network>();
                            for (final VIF vif : vifs) {
                                networks.add(vif.getNetwork(conn));
                            }
                            vm.destroy(conn);
                            final SR sr = getISOSRbyVmName(conn, cmd.getVmName());
                            removeSR(conn, sr);
                            // Disable any VLAN networks that aren't used
                            // anymore
                            for (final Network network : networks) {
                                try {
                                    if (network.getNameLabel(conn).startsWith("VLAN")) {
                                        disableVlanNetwork(conn, network);
                                    }
                                } catch (final Exception e) {
                                    // network might be destroyed by other host
                                }
                            }
                            return new StopAnswer(cmd, "Stop VM " + vmName + " Succeed", platformstring, true);
                        }
                    } catch (final Exception e) {
                        final String msg = "VM destroy failed in Stop " + vmName + " Command due to " + e.getMessage();
                        s_logger.warn(msg, e);
                    } finally {
                        s_logger.debug("10. The VM " + vmName + " is in Stopped state");
                    }
                }
            }

        } catch (final Exception e) {
            final String msg = "Stop Vm " + vmName + " fail due to " + e.toString();
            s_logger.warn(msg, e);
            return new StopAnswer(cmd, msg, platformstring, false);
        }
        return new StopAnswer(cmd, "Stop VM failed", platformstring, false);
    }

    private List<VDI> getVdis(final Connection conn, final VM vm) {
        final List<VDI> vdis = new ArrayList<VDI>();
        try {
            final Set<VBD> vbds = vm.getVBDs(conn);
            for (final VBD vbd : vbds) {
                vdis.add(vbd.getVDI(conn));
            }
        } catch (final XenAPIException e) {
            final String msg = "getVdis can not get VPD due to " + e.toString();
            s_logger.warn(msg, e);
        } catch (final XmlRpcException e) {
            final String msg = "getVdis can not get VPD due to " + e.getMessage();
            s_logger.warn(msg, e);
        }
        return vdis;
    }

    public String connect(final Connection conn, final String vmName, final String ipAddress, final int port) {
        for (int i = 0; i <= _retry; i++) {
            try {
                final Set<VM> vms = VM.getByNameLabel(conn, vmName);
                if (vms.size() < 1) {
                    final String msg = "VM " + vmName + " is not running";
                    s_logger.warn(msg);
                    return msg;
                }
            } catch (final Exception e) {
                final String msg = "VM.getByNameLabel " + vmName + " failed due to " + e.toString();
                s_logger.warn(msg, e);
                return msg;
            }
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Trying to connect to " + ipAddress + " attempt " + i + " of " + _retry);
            }
            if (pingdomr(conn, ipAddress, Integer.toString(port))) {
                return null;
            }
            try {
                Thread.sleep(_sleep);
            } catch (final InterruptedException e) {
            }
        }
        final String msg = "Timeout, Unable to logon to " + ipAddress;
        s_logger.debug(msg);

        return msg;
    }

    public String connect(final Connection conn, final String vmname, final String ipAddress) {
        return connect(conn, vmname, ipAddress, 3922);
    }

    public boolean isDeviceUsed(final Connection conn, final VM vm, final Long deviceId) {
        // Figure out the disk number to attach the VM to

        String msg = null;
        try {
            final Set<String> allowedVBDDevices = vm.getAllowedVBDDevices(conn);
            if (allowedVBDDevices.contains(deviceId.toString())) {
                return false;
            }
            return true;
        } catch (final XmlRpcException e) {
            msg = "Catch XmlRpcException due to: " + e.getMessage();
            s_logger.warn(msg, e);
        } catch (final XenAPIException e) {
            msg = "Catch XenAPIException due to: " + e.toString();
            s_logger.warn(msg, e);
        }
        throw new CloudRuntimeException("When check deviceId " + msg);
    }

    public String getUnusedDeviceNum(final Connection conn, final VM vm) {
        // Figure out the disk number to attach the VM to
        try {
            final Set<String> allowedVBDDevices = vm.getAllowedVBDDevices(conn);
            if (allowedVBDDevices.size() == 0) {
                throw new CloudRuntimeException("Could not find an available slot in VM with name: " + vm.getNameLabel(conn) + " to attach a new disk.");
            }
            return allowedVBDDevices.iterator().next();
        } catch (final XmlRpcException e) {
            final String msg = "Catch XmlRpcException due to: " + e.getMessage();
            s_logger.warn(msg, e);
        } catch (final XenAPIException e) {
            final String msg = "Catch XenAPIException due to: " + e.toString();
            s_logger.warn(msg, e);
        }
        throw new CloudRuntimeException("Could not find an available slot in VM with name to attach a new disk.");
    }

    public String callHostPlugin(final Connection conn, final String plugin, final String cmd, final String... params) {
        final Map<String, String> args = new HashMap<String, String>();
        String msg;
        try {
            for (int i = 0; i < params.length; i += 2) {
                args.put(params[i], params[i + 1]);
            }

            if (s_logger.isTraceEnabled()) {
                s_logger.trace("callHostPlugin executing for command " + cmd + " with " + getArgsString(args));
            }
            final Host host = Host.getByUuid(conn, _host.getUuid());
            final String result = host.callPlugin(conn, plugin, cmd, args);
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("callHostPlugin Result: " + result);
            }
            return result.replace("\n", "");
        } catch (final XenAPIException e) {
            msg = "callHostPlugin failed for cmd: " + cmd + " with args " + getArgsString(args) + " due to " + e.toString();
            s_logger.warn(msg);
        } catch (final XmlRpcException e) {
            msg = "callHostPlugin failed for cmd: " + cmd + " with args " + getArgsString(args) + " due to " + e.getMessage();
            s_logger.debug(msg);
        }
        throw new CloudRuntimeException(msg);
    }

    protected String getArgsString(final Map<String, String> args) {
        final StringBuilder argString = new StringBuilder();
        for (final Map.Entry<String, String> arg : args.entrySet()) {
            argString.append(arg.getKey() + ": " + arg.getValue() + ", ");
        }
        return argString.toString();
    }

    public boolean setIptables(final Connection conn) {
        final String result = callHostPlugin(conn, "vmops", "setIptables");
        if (result == null || result.isEmpty()) {
            return false;
        }
        return true;
    }

    protected XsLocalNetwork getManagementNetwork(final Connection conn) throws XmlRpcException, XenAPIException {
        PIF mgmtPif = null;
        PIF.Record mgmtPifRec = null;
        final Host host = Host.getByUuid(conn, _host.getUuid());
        final Set<PIF> hostPifs = host.getPIFs(conn);
        for (final PIF pif : hostPifs) {
            final PIF.Record rec = pif.getRecord(conn);
            if (rec.management) {
                if (rec.VLAN != null && rec.VLAN != -1) {
                    final String msg =
                            new StringBuilder("Unsupported configuration.  Management network is on a VLAN.  host=").append(_host.getUuid())
                            .append("; pif=")
                            .append(rec.uuid)
                            .append("; vlan=")
                            .append(rec.VLAN)
                            .toString();
                    s_logger.warn(msg);
                    throw new CloudRuntimeException(msg);
                }
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Management network is on pif=" + rec.uuid);
                }
                mgmtPif = pif;
                mgmtPifRec = rec;
                break;
            }
        }
        if (mgmtPif == null) {
            final String msg = "Unable to find management network for " + _host.getUuid();
            s_logger.warn(msg);
            throw new CloudRuntimeException(msg);
        }
        final Bond bond = mgmtPifRec.bondSlaveOf;
        if (!isRefNull(bond)) {
            final String msg =
                    "Management interface is on slave(" + mgmtPifRec.uuid + ") of bond(" + bond.getUuid(conn) + ") on host(" + _host.getUuid() +
                    "), please move management interface to bond!";
            s_logger.warn(msg);
            throw new CloudRuntimeException(msg);
        }
        final Network nk = mgmtPifRec.network;
        final Network.Record nkRec = nk.getRecord(conn);
        return new XsLocalNetwork(this, nk, nkRec, mgmtPif, mgmtPifRec);
    }

    protected VIF getCorrectVif(final Connection conn, final VM router, final Network network) throws XmlRpcException, XenAPIException {
        final Set<VIF> routerVIFs = router.getVIFs(conn);
        for (final VIF vif : routerVIFs) {
            final Network vifNetwork = vif.getNetwork(conn);
            if (vifNetwork.getUuid(conn).equals(network.getUuid(conn))) {
                return vif;
            }
        }

        return null;
    }

    protected VIF getCorrectVif(final Connection conn, final VM router, final IpAddressTO ip) throws XmlRpcException, XenAPIException {
        final NicTO nic = new NicTO();
        nic.setType(ip.getTrafficType());
        nic.setName(ip.getNetworkName());
        if (ip.getBroadcastUri() == null) {
            nic.setBroadcastType(BroadcastDomainType.Native);
        } else {
            final URI uri = BroadcastDomainType.fromString(ip.getBroadcastUri());
            nic.setBroadcastType(BroadcastDomainType.getSchemeValue(uri));
            nic.setBroadcastUri(uri);
        }
        final Network network = getNetwork(conn, nic);
        // Determine the correct VIF on DomR to associate/disassociate the
        // IP address with
        final Set<VIF> routerVIFs = router.getVIFs(conn);
        for (final VIF vif : routerVIFs) {
            final Network vifNetwork = vif.getNetwork(conn);
            if (vifNetwork.getUuid(conn).equals(network.getUuid(conn))) {
                return vif;
            }
        }
        return null;
    }

    protected VIF getVifByMac(final Connection conn, final VM router, String mac) throws XmlRpcException, XenAPIException {
        final Set<VIF> routerVIFs = router.getVIFs(conn);
        mac = mac.trim();
        for (final VIF vif : routerVIFs) {
            final String lmac = vif.getMAC(conn);
            if (lmac.trim().equals(mac)) {
                return vif;
            }
        }
        return null;
    }

    protected String getLowestAvailableVIFDeviceNum(final Connection conn, final VM vm) {
        String vmName = "";
        try {
            vmName = vm.getNameLabel(conn);
            final List<Integer> usedDeviceNums = new ArrayList<Integer>();
            final Set<VIF> vifs = vm.getVIFs(conn);
            final Iterator<VIF> vifIter = vifs.iterator();
            while (vifIter.hasNext()) {
                final VIF vif = vifIter.next();
                try {
                    final String deviceId = vif.getDevice(conn);
                    if(vm.getIsControlDomain(conn) || vif.getCurrentlyAttached(conn)) {
                        usedDeviceNums.add(Integer.valueOf(deviceId));
                    } else {
                        s_logger.debug("Found unplugged VIF " + deviceId + " in VM " + vmName + " destroy it");
                        vif.destroy(conn);
                    }
                } catch (final NumberFormatException e) {
                    final String msg = "Obtained an invalid value for an allocated VIF device number for VM: " + vmName;
                    s_logger.debug(msg, e);
                    throw new CloudRuntimeException(msg);
                }
            }

            for (Integer i = 0; i < _maxNics; i++) {
                if (!usedDeviceNums.contains(i)) {
                    s_logger.debug("Lowest available Vif device number: " + i + " for VM: " + vmName);
                    return i.toString();
                }
            }
        } catch (final XmlRpcException e) {
            final String msg = "Caught XmlRpcException: " + e.getMessage();
            s_logger.warn(msg, e);
        } catch (final XenAPIException e) {
            final String msg = "Caught XenAPIException: " + e.toString();
            s_logger.warn(msg, e);
        }

        throw new CloudRuntimeException("Could not find available VIF slot in VM with name: " + vmName);
    }

    protected VDI mount(final Connection conn, final StoragePoolType poolType, final String volumeFolder, final String volumePath) {
        return getVDIbyUuid(conn, volumePath);
    }

    /**
     * getNetworkByName() retrieves what the server thinks is the actual
     * network used by the XenServer host.  This method should always be
     * used to talk to retrieve a network by the name.  The reason is
     * because of the problems in using the name label as the way to find
     * the Network.
     *
     * To see how we are working around these problems, take a look at
     * enableVlanNetwork().  The following description assumes you have looked
     * at the description on that method.
     *
     * In order to understand this, we have to see what type of networks are
     * within a XenServer that's under CloudStack control.
     *
     *   - Native Networks: these are networks that are untagged on the
     *     XenServer and are used to crate VLAN networks on.  These are
     *     created by the user and is assumed to be one per cluster.
     *   - VLAN Networks: these are dynamically created by CloudStack and can
     *     have problems with duplicated names.
     *   - LinkLocal Networks: these are dynamically created by CloudStack and
     *     can also have problems with duplicated names but these don't have
     *     actual PIFs.
     *
     *  In order to speed to retrieval of a network, we do the following:
     *    - We retrieve by the name.  If only one network is retrieved, we
     *      assume we retrieved the right network.
     *    - If more than one network is retrieved, we check to see which one
     *      has the pif for the local host and use that.
     *    - If a pif is not found, then we look at the tags and find the
     *      one with the lowest timestamp. (See enableVlanNetwork())
     *
     * @param conn Xapi connection
     * @param name name of the network
     * @return XsNic an object that contains network, network record, pif, and pif record.
     * @throws XenAPIException
     * @throws XmlRpcException
     *
     * @see CitrixResourceBase#enableVlanNetwork
     */
    public XsLocalNetwork getNetworkByName(final Connection conn, final String name) throws XenAPIException, XmlRpcException {
        final Set<Network> networks = Network.getByNameLabel(conn, name);
        if (networks.size() == 1) {
            return new XsLocalNetwork(this, networks.iterator().next(), null, null, null);
        }

        if (networks.size() == 0) {
            return null;
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Found more than one network with the name " + name);
        }
        Network earliestNetwork = null;
        Network.Record earliestNetworkRecord = null;
        long earliestTimestamp = Long.MAX_VALUE;
        int earliestRandom = Integer.MAX_VALUE;
        for (final Network network : networks) {
            final XsLocalNetwork nic = new XsLocalNetwork(this, network);

            if (nic.getPif(conn) != null) {
                return nic;
            }

            final Network.Record record = network.getRecord(conn);
            if (record.tags != null) {
                for (final String tag : record.tags) {
                    final Pair<Long, Integer> stamp = parseTimestamp(tag);
                    if (stamp == null) {
                        continue;
                    }

                    if (stamp.first() < earliestTimestamp || stamp.first() == earliestTimestamp && stamp.second() < earliestRandom) {
                        earliestTimestamp = stamp.first();
                        earliestRandom = stamp.second();
                        earliestNetwork = network;
                        earliestNetworkRecord = record;
                    }
                }
            }
        }

        return earliestNetwork != null ? new XsLocalNetwork(this, earliestNetwork, earliestNetworkRecord, null, null) : null;
    }

    protected String generateTimeStamp() {
        return new StringBuilder("CsCreateTime-").append(System.currentTimeMillis()).append("-").append(Rand.nextInt(Integer.MAX_VALUE)).toString();
    }

    protected Pair<Long, Integer> parseTimestamp(final String timeStampStr) {
        final String[] tokens = timeStampStr.split("-");
        if (tokens.length != 3) {
            s_logger.debug("timeStamp in network has wrong pattern: " + timeStampStr);
            return null;
        }
        if (!tokens[0].equals("CsCreateTime")) {
            s_logger.debug("timeStamp in network doesn't start with CsCreateTime: " + timeStampStr);
            return null;
        }
        return new Pair<Long, Integer>(Long.parseLong(tokens[1]), Integer.parseInt(tokens[2]));
    }

    /**
     * enableVlanNetwork creates a Network object, Vlan object, and thereby
     * a tagged PIF object in Xapi.
     *
     * In XenServer, VLAN is added by
     *   - Create a network, which is unique cluster wide.
     *   - Find the PIF that you want to create the VLAN on.
     *   - Create a VLAN using the network and the PIF.  As a result of this
     *     operation, a tagged PIF object is also created.
     *
     * Here is a list of problems with clustered Xapi implementation that
     * we are trying to circumvent.
     *   - There can be multiple Networks with the same name-label so searching
     *     using name-label is not unique.
     *   - There are no other ways to search for Networks other than listing
     *     all of them which is not efficient in our implementation because
     *     we can have over 4000 VLAN networks.
     *   - In a clustered situation, it's possible for both hosts to detect
     *     that the Network is missing and both creates it.  This causes a
     *     lot of problems as one host may be using one Network and another
     *     may be using a different network for their VMs.  This causes
     *     problems in migration because the VMs are logically attached
     *     to different networks in Xapi's database but in reality, they
     *     are attached to the same network.
     *
     * To work around these problems, we do the following.
     *
     *   - When creating the VLAN network, we name it as VLAN-UUID of the
     *     Network it is created on-VLAN Tag.  Because VLAN tags is unique with
     *     one particular network, this is a unique name-label to quickly
     *     retrieve the the VLAN network with when we need it again.
     *   - When we create the VLAN network, we add a timestamp and a random
     *     number as a tag into the network.  Then instead of creating
     *     VLAN on that network, we actually retrieve the Network again
     *     and this time uses the VLAN network with lowest timestamp or
     *     lowest random number as the VLAN network.  This allows VLAN creation
     *     to happen on multiple hosts concurrently but even if two VLAN
     *     networks were created with the same name, only one of them is used.
     *
     * One cavaet about this approach is that it relies on the timestamp to
     * be relatively accurate among different hosts.
     *
     * @param conn Xapi Connection
     * @param tag VLAN tag
     * @param network network on this host to create the VLAN on.
     * @return VLAN Network created.
     * @throws XenAPIException
     * @throws XmlRpcException
     */
    protected Network enableVlanNetwork(final Connection conn, final long tag, final XsLocalNetwork network) throws XenAPIException, XmlRpcException {
        Network vlanNetwork = null;
        final String oldName = "VLAN" + Long.toString(tag);
        final String newName = "VLAN-" + network.getNetworkRecord(conn).uuid + "-" + tag;
        XsLocalNetwork vlanNic = getNetworkByName(conn, newName);
        if (vlanNic == null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Couldn't find vlan network with the new name so trying old name: " + oldName);
            }
            vlanNic = getNetworkByName(conn, oldName);
            if (vlanNic != null) {
                s_logger.info("Renaming VLAN with old name " + oldName + " to " + newName);
                vlanNic.getNetwork().setNameLabel(conn, newName);
            }
        }
        if (vlanNic == null) { // Can't find it, then create it.
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Creating VLAN network for " + tag + " on host " + _host.getIp());
            }
            final Network.Record nwr = new Network.Record();
            nwr.nameLabel = newName;
            nwr.tags = new HashSet<String>();
            nwr.tags.add(generateTimeStamp());
            vlanNetwork = Network.create(conn, nwr);
            vlanNic = getNetworkByName(conn, newName);
            if(vlanNic == null) { //Still vlanNic is null means we could not create it for some reason and no exception capture happened.
                throw new CloudRuntimeException("Could not find/create vlan network with name: " + newName);
            }
        }

        final PIF nPif = network.getPif(conn);
        final PIF.Record nPifr = network.getPifRecord(conn);

        vlanNetwork = vlanNic.getNetwork();
        if (vlanNic.getPif(conn) != null) {
            return vlanNetwork;
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Creating VLAN " + tag + " on host " + _host.getIp() + " on device " + nPifr.device);
        }
        final VLAN vlan = VLAN.create(conn, nPif, tag, vlanNetwork);
        if (vlan != null) {
            final VLAN.Record vlanr = vlan.getRecord(conn);
            if (vlanr != null) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("VLAN is created for " + tag + ".  The uuid is " + vlanr.uuid);
                }
            }
        }
        return vlanNetwork;
    }

    public void disableVlanNetwork(final Connection conn, final Network network) {
    }

    protected SR getLocalLVMSR(final Connection conn) {
        try {
            final Map<SR, SR.Record> map = SR.getAllRecords(conn);
            if(map != null && !map.isEmpty()) {
                for (final Map.Entry<SR, SR.Record> entry : map.entrySet()) {
                    final SR.Record srRec = entry.getValue();
                    if (SRType.LVM.equals(srRec.type)) {
                        final Set<PBD> pbds = srRec.PBDs;
                        if (pbds == null) {
                            continue;
                        }
                        for (final PBD pbd : pbds) {
                            final Host host = pbd.getHost(conn);
                            if (!isRefNull(host) && host.getUuid(conn).equals(_host.getUuid())) {
                                if (!pbd.getCurrentlyAttached(conn)) {
                                    pbd.plug(conn);
                                }
                                final SR sr = entry.getKey();
                                sr.scan(conn);
                                return sr;
                            }
                        }
                    }
                }
            }
        } catch (final XenAPIException e) {
            final String msg = "Unable to get local LVMSR in host:" + _host.getUuid() + e.toString();
            s_logger.warn(msg);
        } catch (final XmlRpcException e) {
            final String msg = "Unable to get local LVMSR in host:" + _host.getUuid() + e.getCause();
            s_logger.warn(msg);
        }
        return null;
    }

    protected SR getLocalEXTSR(final Connection conn) {
        try {
            final Map<SR, SR.Record> map = SR.getAllRecords(conn);
            if(map != null && !map.isEmpty()) {
                for (final Map.Entry<SR, SR.Record> entry : map.entrySet()) {
                    final SR.Record srRec = entry.getValue();
                    if (SRType.FILE.equals(srRec.type) || SRType.EXT.equals(srRec.type)) {
                        final Set<PBD> pbds = srRec.PBDs;
                        if (pbds == null) {
                            continue;
                        }
                        for (final PBD pbd : pbds) {
                            final Host host = pbd.getHost(conn);
                            if (!isRefNull(host) && host.getUuid(conn).equals(_host.getUuid())) {
                                if (!pbd.getCurrentlyAttached(conn)) {
                                    pbd.plug(conn);
                                }
                                final SR sr = entry.getKey();
                                sr.scan(conn);
                                return sr;
                            }
                        }
                    }
                }
            }
        } catch (final XenAPIException e) {
            final String msg = "Unable to get local EXTSR in host:" + _host.getUuid() + e.toString();
            s_logger.warn(msg);
        } catch (final XmlRpcException e) {
            final String msg = "Unable to get local EXTSR in host:" + _host.getUuid() + e.getCause();
            s_logger.warn(msg);
        }
        return null;
    }

    protected StartupStorageCommand initializeLocalSR(final Connection conn) {
        final SR lvmsr = getLocalLVMSR(conn);
        if (lvmsr != null) {
            try {
                _host.setLocalSRuuid(lvmsr.getUuid(conn));

                final String lvmuuid = lvmsr.getUuid(conn);
                final long cap = lvmsr.getPhysicalSize(conn);
                if (cap > 0) {
                    final long avail = cap - lvmsr.getPhysicalUtilisation(conn);
                    lvmsr.setNameLabel(conn, lvmuuid);
                    final String name = "Cloud Stack Local LVM Storage Pool for " + _host.getUuid();
                    lvmsr.setNameDescription(conn, name);
                    final Host host = Host.getByUuid(conn, _host.getUuid());
                    final String address = host.getAddress(conn);
                    final StoragePoolInfo pInfo = new StoragePoolInfo(lvmuuid, address, SRType.LVM.toString(), SRType.LVM.toString(), StoragePoolType.LVM, cap, avail);
                    final StartupStorageCommand cmd = new StartupStorageCommand();
                    cmd.setPoolInfo(pInfo);
                    cmd.setGuid(_host.getUuid());
                    cmd.setDataCenter(Long.toString(_dcId));
                    cmd.setResourceType(Storage.StorageResourceType.STORAGE_POOL);
                    return cmd;
                }
            } catch (final XenAPIException e) {
                final String msg = "build local LVM info err in host:" + _host.getUuid() + e.toString();
                s_logger.warn(msg);
            } catch (final XmlRpcException e) {
                final String msg = "build local LVM info err in host:" + _host.getUuid() + e.getMessage();
                s_logger.warn(msg);
            }
        }

        final SR extsr = getLocalEXTSR(conn);
        if (extsr != null) {
            try {
                final String extuuid = extsr.getUuid(conn);
                _host.setLocalSRuuid(extuuid);
                final long cap = extsr.getPhysicalSize(conn);
                if (cap > 0) {
                    final long avail = cap - extsr.getPhysicalUtilisation(conn);
                    extsr.setNameLabel(conn, extuuid);
                    final String name = "Cloud Stack Local EXT Storage Pool for " + _host.getUuid();
                    extsr.setNameDescription(conn, name);
                    final Host host = Host.getByUuid(conn, _host.getUuid());
                    final String address = host.getAddress(conn);
                    final StoragePoolInfo pInfo = new StoragePoolInfo(extuuid, address, SRType.EXT.toString(), SRType.EXT.toString(), StoragePoolType.EXT, cap, avail);
                    final StartupStorageCommand cmd = new StartupStorageCommand();
                    cmd.setPoolInfo(pInfo);
                    cmd.setGuid(_host.getUuid());
                    cmd.setDataCenter(Long.toString(_dcId));
                    cmd.setResourceType(Storage.StorageResourceType.STORAGE_POOL);
                    return cmd;
                }
            } catch (final XenAPIException e) {
                final String msg = "build local EXT info err in host:" + _host.getUuid() + e.toString();
                s_logger.warn(msg);
            } catch (final XmlRpcException e) {
                final String msg = "build local EXT info err in host:" + _host.getUuid() + e.getMessage();
                s_logger.warn(msg);
            }
        }
        return null;
    }

    @Override
    public PingCommand getCurrentStatus(final long id) {
        try {
            if (!pingXAPI()) {
                Thread.sleep(1000);
                if (!pingXAPI()) {
                    s_logger.warn("can not ping xenserver " + _host.getUuid());
                    return null;
                }
            }
            final Connection conn = getConnection();
            if (!_canBridgeFirewall && !_isOvs) {
                return new PingRoutingCommand(getType(), id, getHostVmStateReport(conn));
            } else if (_isOvs) {
                final List<Pair<String, Long>> ovsStates = ovsFullSyncStates();
                return new PingRoutingWithOvsCommand(getType(), id, getHostVmStateReport(conn), ovsStates);
            } else {
                final HashMap<String, Pair<Long, Long>> nwGrpStates = syncNetworkGroups(conn, id);
                return new PingRoutingWithNwGroupsCommand(getType(), id, getHostVmStateReport(conn), nwGrpStates);
            }
        } catch (final Exception e) {
            s_logger.warn("Unable to get current status", e);
            return null;
        }
    }

    private HashMap<String, Pair<Long, Long>> syncNetworkGroups(final Connection conn, final long id) {
        final HashMap<String, Pair<Long, Long>> states = new HashMap<String, Pair<Long, Long>>();

        final String result = callHostPlugin(conn, "vmops", "get_rule_logs_for_vms", "host_uuid", _host.getUuid());
        s_logger.trace("syncNetworkGroups: id=" + id + " got: " + result);
        final String[] rulelogs = result != null ? result.split(";") : new String[0];
        for (final String rulesforvm : rulelogs) {
            final String[] log = rulesforvm.split(",");
            if (log.length != 6) {
                continue;
            }
            //output = ','.join([vmName, vmID, vmIP, domID, signature, seqno])
            try {
                states.put(log[0], new Pair<Long, Long>(Long.parseLong(log[1]), Long.parseLong(log[5])));
            } catch (final NumberFormatException nfe) {
                states.put(log[0], new Pair<Long, Long>(-1L, -1L));
            }
        }
        return states;
    }

    @Override
    public Type getType() {
        return com.cloud.host.Host.Type.Routing;
    }

    protected boolean getHostInfo(final Connection conn) throws IllegalArgumentException {
        try {
            final Host myself = Host.getByUuid(conn, _host.getUuid());
            Set<HostCpu> hcs = null;
            for (int i = 0; i < 10; i++) {
                hcs = myself.getHostCPUs(conn);
                if(hcs != null) {
                    _host.setCpus(hcs.size());
                    if (_host.getCpus() > 0) {
                        break;
                    }
                }
                Thread.sleep(5000);
            }
            if (_host.getCpus() <= 0) {
                throw new CloudRuntimeException("Cannot get the numbers of cpu from XenServer host " + _host.getIp());
            }
            final Map<String, String> cpuInfo = myself.getCpuInfo(conn);
            if (cpuInfo.get("socket_count") != null) {
                _host.setCpuSockets(Integer.parseInt(cpuInfo.get("socket_count")));
            }
            // would hcs be null we would have thrown an exception on condition (_host.getCpus() <= 0) by now
            for (final HostCpu hc : hcs) {
                _host.setSpeed(hc.getSpeed(conn).intValue());
                break;
            }
            final Host.Record hr = myself.getRecord(conn);
            _host.setProductVersion(CitrixHelper.getProductVersion(hr));

            final XsLocalNetwork privateNic = getManagementNetwork(conn);
            _privateNetworkName = privateNic.getNetworkRecord(conn).nameLabel;
            _host.setPrivatePif(privateNic.getPifRecord(conn).uuid);
            _host.setPrivateNetwork(privateNic.getNetworkRecord(conn).uuid);
            _host.setSystemvmisouuid(null);

            XsLocalNetwork guestNic = null;
            if (_guestNetworkName != null && !_guestNetworkName.equals(_privateNetworkName)) {
                guestNic = getNetworkByName(conn, _guestNetworkName);
                if (guestNic == null) {
                    s_logger.warn("Unable to find guest network " + _guestNetworkName);
                    throw new IllegalArgumentException("Unable to find guest network " + _guestNetworkName + " for host " + _host.getIp());
                }
            } else {
                guestNic = privateNic;
                _guestNetworkName = _privateNetworkName;
            }
            _host.setGuestNetwork(guestNic.getNetworkRecord(conn).uuid);
            _host.setGuestPif(guestNic.getPifRecord(conn).uuid);

            XsLocalNetwork publicNic = null;
            if (_publicNetworkName != null && !_publicNetworkName.equals(_guestNetworkName)) {
                publicNic = getNetworkByName(conn, _publicNetworkName);
                if (publicNic == null) {
                    s_logger.warn("Unable to find public network " + _publicNetworkName + " for host " + _host.getIp());
                    throw new IllegalArgumentException("Unable to find public network " + _publicNetworkName + " for host " + _host.getIp());
                }
            } else {
                publicNic = guestNic;
                _publicNetworkName = _guestNetworkName;
            }
            _host.setPublicPif(publicNic.getPifRecord(conn).uuid);
            _host.setPublicNetwork(publicNic.getNetworkRecord(conn).uuid);
            if (_storageNetworkName1 == null) {
                _storageNetworkName1 = _guestNetworkName;
            }
            XsLocalNetwork storageNic1 = null;
            storageNic1 = getNetworkByName(conn, _storageNetworkName1);
            if (storageNic1 == null) {
                s_logger.warn("Unable to find storage network " + _storageNetworkName1 + " for host " + _host.getIp());
                throw new IllegalArgumentException("Unable to find storage network " + _storageNetworkName1 + " for host " + _host.getIp());
            } else {
                _host.setStorageNetwork1(storageNic1.getNetworkRecord(conn).uuid);
                _host.setStoragePif1(storageNic1.getPifRecord(conn).uuid);
            }

            XsLocalNetwork storageNic2 = null;
            if (_storageNetworkName2 != null) {
                storageNic2 = getNetworkByName(conn, _storageNetworkName2);
                if(storageNic2 != null) {
                    _host.setStoragePif2(storageNic2.getPifRecord(conn).uuid);
                }
            }

            s_logger.info("XenServer Version is " + _host.getProductVersion() + " for host " + _host.getIp());
            s_logger.info("Private Network is " + _privateNetworkName + " for host " + _host.getIp());
            s_logger.info("Guest Network is " + _guestNetworkName + " for host " + _host.getIp());
            s_logger.info("Public Network is " + _publicNetworkName + " for host " + _host.getIp());

            return true;
        } catch (final XenAPIException e) {
            s_logger.warn("Unable to get host information for " + _host.getIp(), e);
            return false;
        } catch (final Exception e) {
            s_logger.warn("Unable to get host information for " + _host.getIp(), e);
            return false;
        }
    }

    protected void plugDom0Vif(final Connection conn, final VIF dom0Vif) throws XmlRpcException, XenAPIException {
        if (dom0Vif != null) {
            dom0Vif.plug(conn);
        }
    }

    public void setupLinkLocalNetwork(final Connection conn) {
        try {
            final Network.Record rec = new Network.Record();
            final Set<Network> networks = Network.getByNameLabel(conn, _linkLocalPrivateNetworkName);
            Network linkLocal = null;

            if (networks.size() == 0) {
                rec.nameDescription = "link local network used by system vms";
                rec.nameLabel = _linkLocalPrivateNetworkName;
                final Map<String, String> configs = new HashMap<String, String>();
                configs.put("ip_begin", NetUtils.getLinkLocalGateway());
                configs.put("ip_end", NetUtils.getLinkLocalIpEnd());
                configs.put("netmask", NetUtils.getLinkLocalNetMask());
                configs.put("vswitch-disable-in-band", "true");
                rec.otherConfig = configs;
                linkLocal = Network.create(conn, rec);
            } else {
                linkLocal = networks.iterator().next();
                if (!linkLocal.getOtherConfig(conn).containsKey("vswitch-disable-in-band")) {
                    linkLocal.addToOtherConfig(conn, "vswitch-disable-in-band", "true");
                }
            }

            /* Make sure there is a physical bridge on this network */
            VIF dom0vif = null;
            final Pair<VM, VM.Record> vm = getControlDomain(conn);
            final VM dom0 = vm.first();
            final Set<VIF> vifs = dom0.getVIFs(conn);
            if (vifs.size() != 0) {
                for (final VIF vif : vifs) {
                    final Map<String, String> otherConfig = vif.getOtherConfig(conn);
                    if (otherConfig != null) {
                        final String nameLabel = otherConfig.get("nameLabel");
                        if (nameLabel != null && nameLabel.equalsIgnoreCase("link_local_network_vif")) {
                            dom0vif = vif;
                        }
                    }
                }
            }

            /* create temp VIF0 */
            if (dom0vif == null) {
                s_logger.debug("Can't find a vif on dom0 for link local, creating a new one");
                final VIF.Record vifr = new VIF.Record();
                vifr.VM = dom0;
                vifr.device = getLowestAvailableVIFDeviceNum(conn, dom0);
                if (vifr.device == null) {
                    s_logger.debug("Failed to create link local network, no vif available");
                    return;
                }
                final Map<String, String> config = new HashMap<String, String>();
                config.put("nameLabel", "link_local_network_vif");
                vifr.otherConfig = config;
                vifr.MAC = "FE:FF:FF:FF:FF:FF";
                vifr.network = linkLocal;
                vifr.lockingMode = Types.VifLockingMode.NETWORK_DEFAULT;
                dom0vif = VIF.create(conn, vifr);
                plugDom0Vif(conn, dom0vif);
            } else {
                s_logger.debug("already have a vif on dom0 for link local network");
                if (!dom0vif.getCurrentlyAttached(conn)) {
                    plugDom0Vif(conn, dom0vif);
                }
            }

            final String brName = linkLocal.getBridge(conn);
            callHostPlugin(conn, "vmops", "setLinkLocalIP", "brName", brName);
            _host.setLinkLocalNetwork(linkLocal.getUuid(conn));

        } catch (final XenAPIException e) {
            s_logger.warn("Unable to create local link network", e);
            throw new CloudRuntimeException("Unable to create local link network due to " + e.toString(), e);
        } catch (final XmlRpcException e) {
            s_logger.warn("Unable to create local link network", e);
            throw new CloudRuntimeException("Unable to create local link network due to " + e.toString(), e);
        }
    }

    public boolean transferManagementNetwork(final Connection conn, final Host host, final PIF src, final PIF.Record spr, final PIF dest) throws XmlRpcException, XenAPIException {
        dest.reconfigureIp(conn, spr.ipConfigurationMode, spr.IP, spr.netmask, spr.gateway, spr.DNS);
        Host.managementReconfigure(conn, dest);
        String hostUuid = null;
        final int count = 0;
        while (count < 10) {
            try {
                Thread.sleep(10000);
                hostUuid = host.getUuid(conn);
                if (hostUuid != null) {
                    break;
                }
            } catch (final XmlRpcException e) {
                s_logger.debug("Waiting for host to come back: " + e.getMessage());
            } catch (final XenAPIException e) {
                s_logger.debug("Waiting for host to come back: " + e.getMessage());
            } catch (final InterruptedException e) {
                s_logger.debug("Gotta run");
                return false;
            }
        }
        if (hostUuid == null) {
            s_logger.warn("Unable to transfer the management network from " + spr.uuid);
            return false;
        }

        src.reconfigureIp(conn, Types.IpConfigurationMode.NONE, null, null, null, null);
        return true;
    }

    @Override
    public StartupCommand[] initialize() throws IllegalArgumentException {
        final Connection conn = getConnection();
        if (!getHostInfo(conn)) {
            s_logger.warn("Unable to get host information for " + _host.getIp());
            return null;
        }
        final StartupRoutingCommand cmd = new StartupRoutingCommand();
        fillHostInfo(conn, cmd);
        cmd.setHypervisorType(HypervisorType.XenServer);
        cmd.setCluster(_cluster);
        cmd.setPoolSync(false);

        try {
            final Pool pool = Pool.getByUuid(conn, _host.getPool());
            final Pool.Record poolr = pool.getRecord(conn);
            poolr.master.getRecord(conn);
        } catch (final Throwable e) {
            s_logger.warn("Check for master failed, failing the FULL Cluster sync command");
        }
        final StartupStorageCommand sscmd = initializeLocalSR(conn);
        if (sscmd != null) {
            return new StartupCommand[] {cmd, sscmd};
        }
        return new StartupCommand[] {cmd};
    }

    public void cleanupTemplateSR(final Connection conn) {
        Set<PBD> pbds = null;
        try {
            final Host host = Host.getByUuid(conn, _host.getUuid());
            pbds = host.getPBDs(conn);
        } catch (final XenAPIException e) {
            s_logger.warn("Unable to get the SRs " + e.toString(), e);
            throw new CloudRuntimeException("Unable to get SRs " + e.toString(), e);
        } catch (final Exception e) {
            throw new CloudRuntimeException("Unable to get SRs " + e.getMessage(), e);
        }
        for (final PBD pbd : pbds) {
            SR sr = null;
            SR.Record srRec = null;
            try {
                sr = pbd.getSR(conn);
                srRec = sr.getRecord(conn);
            } catch (final Exception e) {
                s_logger.warn("pbd.getSR get Exception due to ", e);
                continue;
            }
            final String type = srRec.type;
            if (srRec.shared) {
                continue;
            }
            if (SRType.NFS.equals(type) || SRType.ISO.equals(type) && srRec.nameDescription.contains("template")) {
                try {
                    pbd.unplug(conn);
                    pbd.destroy(conn);
                    sr.forget(conn);
                } catch (final Exception e) {
                    s_logger.warn("forget SR catch Exception due to ", e);
                }
            }
        }
    }

    public boolean launchHeartBeat(final Connection conn) {
        final String result = callHostPluginPremium(conn, "heartbeat",
                "host", _host.getUuid(),
                "timeout", Integer.toString(_heartbeatTimeout),
                "interval", Integer.toString(_heartbeatInterval));
        if (result == null || !result.contains("> DONE <")) {
            s_logger.warn("Unable to launch the heartbeat process on " + _host.getIp());
            return false;
        }
        return true;
    }

    protected SetupAnswer execute(final SetupCommand cmd) {
        final Connection conn = getConnection();
        try {
            final Map<Pool, Pool.Record> poolRecs = Pool.getAllRecords(conn);
            if (poolRecs.size() != 1) {
                throw new CloudRuntimeException("There are " + poolRecs.size() + " pool for host :" + _host.getUuid());
            }
            final Host master = poolRecs.values().iterator().next().master;
            setupServer(conn, master);
            final Host host = Host.getByUuid(conn, _host.getUuid());
            setupServer(conn, host);

            if (!setIptables(conn)) {
                s_logger.warn("set xenserver Iptable failed");
                return null;
            }

            if (_securityGroupEnabled) {
                _canBridgeFirewall = canBridgeFirewall(conn);
                if (!_canBridgeFirewall) {
                    final String msg = "Failed to configure brige firewall";
                    s_logger.warn(msg);
                    s_logger.warn("Check host " + _host.getIp() +" for CSP is installed or not and check network mode for bridge");
                    return new SetupAnswer(cmd, msg);
                }

            }


            final boolean r = launchHeartBeat(conn);
            if (!r) {
                return null;
            }
            cleanupTemplateSR(conn);
            try {
                if (cmd.useMultipath()) {
                    // the config value is set to true
                    host.addToOtherConfig(conn, "multipathing", "true");
                    host.addToOtherConfig(conn, "multipathhandle", "dmp");
                }

            } catch (final Types.MapDuplicateKey e) {
                s_logger.debug("multipath is already set");
            }

            if (cmd.needSetup() ) {
                final String result = callHostPlugin(conn, "vmops", "setup_iscsi", "uuid", _host.getUuid());

                if (!result.contains("> DONE <")) {
                    s_logger.warn("Unable to setup iscsi: " + result);
                    return new SetupAnswer(cmd, result);
                }

                Pair<PIF, PIF.Record> mgmtPif = null;
                final Set<PIF> hostPifs = host.getPIFs(conn);
                for (final PIF pif : hostPifs) {
                    final PIF.Record rec = pif.getRecord(conn);
                    if (rec.management) {
                        if (rec.VLAN != null && rec.VLAN != -1) {
                            final String msg =
                                    new StringBuilder("Unsupported configuration.  Management network is on a VLAN.  host=").append(_host.getUuid())
                                    .append("; pif=")
                                    .append(rec.uuid)
                                    .append("; vlan=")
                                    .append(rec.VLAN)
                                    .toString();
                            s_logger.warn(msg);
                            return new SetupAnswer(cmd, msg);
                        }
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Management network is on pif=" + rec.uuid);
                        }
                        mgmtPif = new Pair<PIF, PIF.Record>(pif, rec);
                        break;
                    }
                }

                if (mgmtPif == null) {
                    final String msg = "Unable to find management network for " + _host.getUuid();
                    s_logger.warn(msg);
                    return new SetupAnswer(cmd, msg);
                }

                final Map<Network, Network.Record> networks = Network.getAllRecords(conn);
                if(networks == null) {
                    final String msg = "Unable to setup as there are no networks in the host: " +  _host.getUuid();
                    s_logger.warn(msg);
                    return new SetupAnswer(cmd, msg);
                }
                for (final Network.Record network : networks.values()) {
                    if (network.nameLabel.equals("cloud-private")) {
                        for (final PIF pif : network.PIFs) {
                            final PIF.Record pr = pif.getRecord(conn);
                            if (_host.getUuid().equals(pr.host.getUuid(conn))) {
                                if (s_logger.isDebugEnabled()) {
                                    s_logger.debug("Found a network called cloud-private. host=" + _host.getUuid() + ";  Network=" + network.uuid + "; pif=" + pr.uuid);
                                }
                                if (pr.VLAN != null && pr.VLAN != -1) {
                                    final String msg =
                                            new StringBuilder("Unsupported configuration.  Network cloud-private is on a VLAN.  Network=").append(network.uuid)
                                            .append(" ; pif=")
                                            .append(pr.uuid)
                                            .toString();
                                    s_logger.warn(msg);
                                    return new SetupAnswer(cmd, msg);
                                }
                                if (!pr.management && pr.bondMasterOf != null && pr.bondMasterOf.size() > 0) {
                                    if (pr.bondMasterOf.size() > 1) {
                                        final String msg =
                                                new StringBuilder("Unsupported configuration.  Network cloud-private has more than one bond.  Network=").append(network.uuid)
                                                .append("; pif=")
                                                .append(pr.uuid)
                                                .toString();
                                        s_logger.warn(msg);
                                        return new SetupAnswer(cmd, msg);
                                    }
                                    final Bond bond = pr.bondMasterOf.iterator().next();
                                    final Set<PIF> slaves = bond.getSlaves(conn);
                                    for (final PIF slave : slaves) {
                                        final PIF.Record spr = slave.getRecord(conn);
                                        if (spr.management) {
                                            if (!transferManagementNetwork(conn, host, slave, spr, pif)) {
                                                final String msg =
                                                        new StringBuilder("Unable to transfer management network.  slave=" + spr.uuid + "; master=" + pr.uuid + "; host=" +
                                                                _host.getUuid()).toString();
                                                s_logger.warn(msg);
                                                return new SetupAnswer(cmd, msg);
                                            }
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return new SetupAnswer(cmd, false);

        } catch (final XmlRpcException e) {
            s_logger.warn("Unable to setup", e);
            return new SetupAnswer(cmd, e.getMessage());
        } catch (final XenAPIException e) {
            s_logger.warn("Unable to setup", e);
            return new SetupAnswer(cmd, e.getMessage());
        } catch (final Exception e) {
            s_logger.warn("Unable to setup", e);
            return new SetupAnswer(cmd, e.getMessage());
        }
    }

    /* return : if setup is needed */
    public boolean setupServer(final Connection conn, final Host host) {
        final String packageVersion = CitrixResourceBase.class.getPackage().getImplementationVersion();
        final String version = this.getClass().getName() + "-" + (packageVersion == null ? Long.toString(System.currentTimeMillis()) : packageVersion);

        try {
            /* push patches to XenServer */
            final Host.Record hr = host.getRecord(conn);

            final Iterator<String> it = hr.tags.iterator();

            while (it.hasNext()) {
                final String tag = it.next();
                if (tag.startsWith("vmops-version-")) {
                    if (tag.contains(version)) {
                        s_logger.info(logX(host, "Host " + hr.address + " is already setup."));
                        return false;
                    } else {
                        it.remove();
                    }
                }
            }

            final com.trilead.ssh2.Connection sshConnection = new com.trilead.ssh2.Connection(hr.address, 22);
            try {
                sshConnection.connect(null, 60000, 60000);
                if (!sshConnection.authenticateWithPassword(_username, _password.peek())) {
                    throw new CloudRuntimeException("Unable to authenticate");
                }

                final com.trilead.ssh2.Session session = sshConnection.openSession();

                final String cmd = "mkdir -p /opt/cloud/bin /var/log/cloud";
                if (!SSHCmdHelper.sshExecuteCmd(sshConnection, cmd)) {
                    throw new CloudRuntimeException("Cannot create directory /opt/cloud/bin on XenServer hosts");
                }

                final SCPClient scp = new SCPClient(sshConnection);

                final List<File> files = getPatchFiles();
                if (files == null || files.isEmpty()) {
                    throw new CloudRuntimeException("Can not find patch file");
                }
                for (final File file : files) {
                    final String path = file.getParentFile().getAbsolutePath() + "/";
                    final Properties props = PropertiesUtil.loadFromFile(file);

                    for (final Map.Entry<Object, Object> entry : props.entrySet()) {
                        final String k = (String)entry.getKey();
                        final String v = (String)entry.getValue();

                        assert k != null && k.length() > 0 && v != null && v.length() > 0 : "Problems with " + k + "=" + v;

                        final String[] tokens = v.split(",");
                        String f = null;
                        if (tokens.length == 3 && tokens[0].length() > 0) {
                            if (tokens[0].startsWith("/")) {
                                f = tokens[0];
                            } else if (tokens[0].startsWith("~")) {
                                final String homedir = System.getenv("HOME");
                                f = homedir + tokens[0].substring(1) + k;
                            } else {
                                f = path + tokens[0] + '/' + k;
                            }
                        } else {
                            f = path + k;
                        }
                        final String directoryPath = tokens[tokens.length - 1];

                        f = f.replace('/', File.separatorChar);

                        String permissions = "0755";
                        if (tokens.length == 3) {
                            permissions = tokens[1];
                        } else if (tokens.length == 2) {
                            permissions = tokens[0];
                        }

                        if (!new File(f).exists()) {
                            s_logger.warn("We cannot locate " + f);
                            continue;
                        }
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Copying " + f + " to " + directoryPath + " on " + hr.address + " with permission " + permissions);
                        }
                        try {
                            session.execCommand("mkdir -m 700 -p " + directoryPath);
                        } catch (final IOException e) {
                            s_logger.debug("Unable to create destination path: " + directoryPath + " on " + hr.address + " but trying anyway");
                        }
                        try {
                            scp.put(f, directoryPath, permissions);
                        } catch (final IOException e) {
                            final String msg = "Unable to copy file " + f + " to path " + directoryPath + " with permissions  " + permissions;
                            s_logger.debug(msg);
                            throw new CloudRuntimeException("Unable to setup the server: " + msg, e);
                        }
                    }
                }

            } catch (final IOException e) {
                throw new CloudRuntimeException("Unable to setup the server correctly", e);
            } finally {
                sshConnection.close();
            }
            hr.tags.add("vmops-version-" + version);
            host.setTags(conn, hr.tags);
            return true;
        } catch (final XenAPIException e) {
            final String msg = "XenServer setup failed due to " + e.toString();
            s_logger.warn(msg, e);
            throw new CloudRuntimeException("Unable to get host information " + e.toString(), e);
        } catch (final XmlRpcException e) {
            final String msg = "XenServer setup failed due to " + e.getMessage();
            s_logger.warn(msg, e);
            throw new CloudRuntimeException("Unable to get host information ", e);
        }
    }

    protected CheckNetworkAnswer execute(final CheckNetworkCommand cmd) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Checking if network name setup is done on the resource");
        }

        final List<PhysicalNetworkSetupInfo> infoList = cmd.getPhysicalNetworkInfoList();

        try {
            boolean errorout = false;
            String msg = "";
            for (final PhysicalNetworkSetupInfo info : infoList) {
                if (!isNetworkSetupByName(info.getGuestNetworkName())) {
                    msg =
                            "For Physical Network id:" + info.getPhysicalNetworkId() + ", Guest Network is not configured on the backend by name " +
                                    info.getGuestNetworkName();
                    errorout = true;
                    break;
                }
                if (!isNetworkSetupByName(info.getPrivateNetworkName())) {
                    msg =
                            "For Physical Network id:" + info.getPhysicalNetworkId() + ", Private Network is not configured on the backend by name " +
                                    info.getPrivateNetworkName();
                    errorout = true;
                    break;
                }
                if (!isNetworkSetupByName(info.getPublicNetworkName())) {
                    msg =
                            "For Physical Network id:" + info.getPhysicalNetworkId() + ", Public Network is not configured on the backend by name " +
                                    info.getPublicNetworkName();
                    errorout = true;
                    break;
                }
                /*if(!isNetworkSetupByName(info.getStorageNetworkName())){
                    msg = "For Physical Network id:"+ info.getPhysicalNetworkId() + ", Storage Network is not configured on the backend by name " + info.getStorageNetworkName();
                    errorout = true;
                    break;
                }*/
            }
            if (errorout) {
                s_logger.error(msg);
                return new CheckNetworkAnswer(cmd, false, msg);
            } else {
                return new CheckNetworkAnswer(cmd, true, "Network Setup check by names is done");
            }

        } catch (final XenAPIException e) {
            final String msg = "CheckNetworkCommand failed with XenAPIException:" + e.toString() + " host:" + _host.getUuid();
            s_logger.warn(msg, e);
            return new CheckNetworkAnswer(cmd, false, msg);
        } catch (final Exception e) {
            final String msg = "CheckNetworkCommand failed with Exception:" + e.getMessage() + " host:" + _host.getUuid();
            s_logger.warn(msg, e);
            return new CheckNetworkAnswer(cmd, false, msg);
        }
    }

    protected boolean isNetworkSetupByName(final String nameTag) throws XenAPIException, XmlRpcException {
        if (nameTag != null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Looking for network setup by name " + nameTag);
            }
            final Connection conn = getConnection();
            final XsLocalNetwork network = getNetworkByName(conn, nameTag);
            if (network == null) {
                return false;
            }
        }
        return true;
    }

    protected List<File> getPatchFiles() {
        return null;
    }

    protected SR getSRByNameLabelandHost(final Connection conn, final String name) throws BadServerResponse, XenAPIException, XmlRpcException {
        final Set<SR> srs = SR.getByNameLabel(conn, name);
        SR ressr = null;
        for (final SR sr : srs) {
            Set<PBD> pbds;
            pbds = sr.getPBDs(conn);
            for (final PBD pbd : pbds) {
                final PBD.Record pbdr = pbd.getRecord(conn);
                if (pbdr.host != null && pbdr.host.getUuid(conn).equals(_host.getUuid())) {
                    if (!pbdr.currentlyAttached) {
                        pbd.plug(conn);
                    }
                    ressr = sr;
                    break;
                }
            }
        }
        return ressr;
    }

    protected GetStorageStatsAnswer execute(final GetStorageStatsCommand cmd) {
        final Connection conn = getConnection();
        try {
            final Set<SR> srs = SR.getByNameLabel(conn, cmd.getStorageId());
            if (srs.size() != 1) {
                final String msg = "There are " + srs.size() + " storageid: " + cmd.getStorageId();
                s_logger.warn(msg);
                return new GetStorageStatsAnswer(cmd, msg);
            }
            final SR sr = srs.iterator().next();
            sr.scan(conn);
            final long capacity = sr.getPhysicalSize(conn);
            final long used = sr.getPhysicalUtilisation(conn);
            return new GetStorageStatsAnswer(cmd, capacity, used);
        } catch (final XenAPIException e) {
            final String msg = "GetStorageStats Exception:" + e.toString() + "host:" + _host.getUuid() + "storageid: " + cmd.getStorageId();
            s_logger.warn(msg);
            return new GetStorageStatsAnswer(cmd, msg);
        } catch (final XmlRpcException e) {
            final String msg = "GetStorageStats Exception:" + e.getMessage() + "host:" + _host.getUuid() + "storageid: " + cmd.getStorageId();
            s_logger.warn(msg);
            return new GetStorageStatsAnswer(cmd, msg);
        }
    }

    private void pbdPlug(final Connection conn, final PBD pbd, final String uuid) {
        try {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Plugging in PBD " + uuid + " for " + _host);
            }
            pbd.plug(conn);
        } catch (final Exception e) {
            final String msg = "PBD " + uuid + " is not attached! and PBD plug failed due to " + e.toString() + ". Please check this PBD in " + _host;
            s_logger.warn(msg, e);
            throw new CloudRuntimeException(msg);
        }
    }

    protected boolean checkSR(final Connection conn, final SR sr) {
        try {
            final SR.Record srr = sr.getRecord(conn);
            final Set<PBD> pbds = sr.getPBDs(conn);
            if (pbds.size() == 0) {
                final String msg = "There is no PBDs for this SR: " + srr.nameLabel + " on host:" + _host.getUuid();
                s_logger.warn(msg);
                return false;
            }
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Checking " + srr.nameLabel + " or SR " + srr.uuid + " on " + _host);
            }
            if (srr.shared) {
                if (SRType.NFS.equals(srr.type) ){
                    final Map<String, String> smConfig = srr.smConfig;
                    if( !smConfig.containsKey("nosubdir")) {
                        smConfig.put("nosubdir", "true");
                        sr.setSmConfig(conn,smConfig);
                    }
                }

                final Host host = Host.getByUuid(conn, _host.getUuid());
                boolean found = false;
                for (final PBD pbd : pbds) {
                    final PBD.Record pbdr = pbd.getRecord(conn);
                    if (host.equals(pbdr.host)) {
                        if (!pbdr.currentlyAttached) {
                            pbdPlug(conn, pbd, pbdr.uuid);
                        }
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    final PBD.Record pbdr = srr.PBDs.iterator().next().getRecord(conn);
                    pbdr.host = host;
                    pbdr.uuid = "";
                    final PBD pbd = PBD.create(conn, pbdr);
                    pbdPlug(conn, pbd, pbd.getUuid(conn));
                }
            } else {
                for (final PBD pbd : pbds) {
                    final PBD.Record pbdr = pbd.getRecord(conn);
                    if (!pbdr.currentlyAttached) {
                        pbdPlug(conn, pbd, pbdr.uuid);
                    }
                }
            }

        } catch (final Exception e) {
            final String msg = "checkSR failed host:" + _host + " due to " + e.toString();
            s_logger.warn(msg, e);
            return false;
        }
        return true;
    }

    protected Answer execute(final CreateStoragePoolCommand cmd) {
        final Connection conn = getConnection();
        final StorageFilerTO pool = cmd.getPool();
        try {
            if (pool.getType() == StoragePoolType.NetworkFilesystem) {
                getNfsSR(conn, Long.toString(pool.getId()), pool.getUuid(), pool.getHost(), pool.getPath(), pool.toString());
            } else if (pool.getType() == StoragePoolType.IscsiLUN) {
                getIscsiSR(conn, pool.getUuid(), pool.getHost(), pool.getPath(), null, null, false);
            } else if (pool.getType() == StoragePoolType.PreSetup) {
            } else {
                return new Answer(cmd, false, "The pool type: " + pool.getType().name() + " is not supported.");
            }
            return new Answer(cmd, true, "success");
        } catch (final Exception e) {
            final String msg =
                    "Catch Exception " + e.getClass().getName() + ", create StoragePool failed due to " + e.toString() + " on host:" + _host.getUuid() + " pool: " +
                            pool.getHost() + pool.getPath();
            s_logger.warn(msg, e);
            return new Answer(cmd, false, msg);
        }

    }

    protected String callHostPluginThroughMaster(final Connection conn, final String plugin, final String cmd, final String... params) {
        final Map<String, String> args = new HashMap<String, String>();

        try {
            final Map<Pool, Pool.Record> poolRecs = Pool.getAllRecords(conn);
            if (poolRecs.size() != 1) {
                throw new CloudRuntimeException("There are " + poolRecs.size() + " pool for host :" + _host.getUuid());
            }
            final Host master = poolRecs.values().iterator().next().master;
            for (int i = 0; i < params.length; i += 2) {
                args.put(params[i], params[i + 1]);
            }

            if (s_logger.isTraceEnabled()) {
                s_logger.trace("callHostPlugin executing for command " + cmd + " with " + getArgsString(args));
            }
            final String result = master.callPlugin(conn, plugin, cmd, args);
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("callHostPlugin Result: " + result);
            }
            return result.replace("\n", "");
        } catch (final Types.HandleInvalid e) {
            s_logger.warn("callHostPlugin failed for cmd: " + cmd + " with args " + getArgsString(args) + " due to HandleInvalid clazz:" + e.clazz + ", handle:" +
                    e.handle);
        } catch (final XenAPIException e) {
            s_logger.warn("callHostPlugin failed for cmd: " + cmd + " with args " + getArgsString(args) + " due to " + e.toString(), e);
        } catch (final XmlRpcException e) {
            s_logger.warn("callHostPlugin failed for cmd: " + cmd + " with args " + getArgsString(args) + " due to " + e.getMessage(), e);
        }
        return null;
    }

    public String callHostPluginPremium(final Connection conn, final String cmd, final String... params) {
        return callHostPlugin(conn, "vmopspremium", cmd, params);
    }

    public String setupHeartbeatSr(final Connection conn, final SR sr, final boolean force) throws XenAPIException, XmlRpcException {
        final SR.Record srRec = sr.getRecord(conn);
        final String srUuid = srRec.uuid;
        if (!srRec.shared || !SRType.LVMOHBA.equals(srRec.type) && !SRType.LVMOISCSI.equals(srRec.type) && !SRType.NFS.equals(srRec.type)) {
            return srUuid;
        }
        String result = null;
        final Host host = Host.getByUuid(conn, _host.getUuid());
        final Set<String> tags = host.getTags(conn);
        if (force || !tags.contains("cloud-heartbeat-" + srUuid)) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Setting up the heartbeat sr for host " + _host.getIp() + " and sr " + srUuid);
            }
            final Set<PBD> pbds = sr.getPBDs(conn);
            for (final PBD pbd : pbds) {
                final PBD.Record pbdr = pbd.getRecord(conn);
                if (!pbdr.currentlyAttached && pbdr.host.getUuid(conn).equals(_host.getUuid())) {
                    pbd.plug(conn);
                    break;
                }
            }
            result = callHostPluginThroughMaster(conn, "vmopspremium", "setup_heartbeat_sr", "host", _host.getUuid(), "sr", srUuid);
            if (result == null || !result.split("#")[1].equals("0")) {
                throw new CloudRuntimeException("Unable to setup heartbeat sr on SR " + srUuid + " due to " + result);
            }

            if (!tags.contains("cloud-heartbeat-" + srUuid)) {
                tags.add("cloud-heartbeat-" + srUuid);
                host.setTags(conn, tags);
            }
        }
        result = callHostPluginPremium(conn, "setup_heartbeat_file", "host", _host.getUuid(), "sr", srUuid, "add", "true");
        if (result == null || !result.split("#")[1].equals("0")) {
            throw new CloudRuntimeException("Unable to setup heartbeat file entry on SR " + srUuid + " due to " + result);
        }
        return srUuid;
    }

    protected Answer execute(final ModifyStoragePoolCommand cmd) {
        final Connection conn = getConnection();
        final StorageFilerTO pool = cmd.getPool();
        final boolean add = cmd.getAdd();
        if (add) {
            try {
                final SR sr = getStorageRepository(conn, pool.getUuid());
                setupHeartbeatSr(conn, sr, false);
                final long capacity = sr.getPhysicalSize(conn);
                final long available = capacity - sr.getPhysicalUtilisation(conn);
                if (capacity == -1) {
                    final String msg = "Pool capacity is -1! pool: " + pool.getHost() + pool.getPath();
                    s_logger.warn(msg);
                    return new Answer(cmd, false, msg);
                }
                final Map<String, TemplateProp> tInfo = new HashMap<String, TemplateProp>();
                final ModifyStoragePoolAnswer answer = new ModifyStoragePoolAnswer(cmd, capacity, available, tInfo);
                return answer;
            } catch (final XenAPIException e) {
                final String msg = "ModifyStoragePoolCommand add XenAPIException:" + e.toString() + " host:" + _host.getUuid() + " pool: " + pool.getHost() + pool.getPath();
                s_logger.warn(msg, e);
                return new Answer(cmd, false, msg);
            } catch (final Exception e) {
                final String msg = "ModifyStoragePoolCommand add XenAPIException:" + e.getMessage() + " host:" + _host.getUuid() + " pool: " + pool.getHost() + pool.getPath();
                s_logger.warn(msg, e);
                return new Answer(cmd, false, msg);
            }
        } else {
            try {
                final SR sr = getStorageRepository(conn, pool.getUuid());
                final String srUuid = sr.getUuid(conn);
                final String result = callHostPluginPremium(conn, "setup_heartbeat_file", "host", _host.getUuid(), "sr", srUuid, "add", "false");
                if (result == null || !result.split("#")[1].equals("0")) {
                    throw new CloudRuntimeException("Unable to remove heartbeat file entry for SR " + srUuid + " due to " + result);
                }
                return new Answer(cmd, true, "seccuss");
            } catch (final XenAPIException e) {
                final String msg = "ModifyStoragePoolCommand remove XenAPIException:" + e.toString() + " host:" + _host.getUuid() + " pool: " + pool.getHost() + pool.getPath();
                s_logger.warn(msg, e);
                return new Answer(cmd, false, msg);
            } catch (final Exception e) {
                final String msg = "ModifyStoragePoolCommand remove XenAPIException:" + e.getMessage() + " host:" + _host.getUuid() + " pool: " + pool.getHost() + pool.getPath();
                s_logger.warn(msg, e);
                return new Answer(cmd, false, msg);
            }
        }

    }

    private Answer execute(final OvsSetupBridgeCommand cmd) {
        final Connection conn = getConnection();
        findOrCreateTunnelNetwork(conn, cmd.getBridgeName());
        configureTunnelNetwork(conn, cmd.getNetworkId(), cmd.getHostId(), cmd.getBridgeName());
        s_logger.debug("OVS Bridge configured");
        return new Answer(cmd, true, null);
    }

    private Answer execute(final OvsDestroyBridgeCommand cmd) {
        try {
            final Connection conn = getConnection();
            final Network nw = findOrCreateTunnelNetwork(conn, cmd.getBridgeName());
            cleanUpTmpDomVif(conn, nw);
            destroyTunnelNetwork(conn, nw, cmd.getHostId());
            s_logger.debug("OVS Bridge destroyed");
            return new Answer(cmd, true, null);
        } catch (final Exception e) {
            s_logger.warn("caught execption when destroying ovs bridge", e);
            return new Answer(cmd, false, e.getMessage());
        }
    }

    private Answer execute(final OvsDestroyTunnelCommand cmd) {
        final Connection conn = getConnection();
        try {
            final Network nw = findOrCreateTunnelNetwork(conn, cmd.getBridgeName());
            if (nw == null) {
                s_logger.warn("Unable to find tunnel network for GRE key:" + cmd.getBridgeName());
                return new Answer(cmd, false, "No network found");
            }

            final String bridge = nw.getBridge(conn);
            final String result = callHostPlugin(conn, "ovstunnel", "destroy_tunnel", "bridge", bridge, "in_port", cmd.getInPortName());

            if (result.equalsIgnoreCase("SUCCESS")) {
                return new Answer(cmd, true, result);
            } else {
                return new Answer(cmd, false, result);
            }
        } catch (final Exception e) {
            s_logger.warn("caught execption when destroy ovs tunnel", e);
            return new Answer(cmd, false, e.getMessage());
        }
    }

    public Answer execute(final OvsVpcPhysicalTopologyConfigCommand cmd) {
        final Connection conn = getConnection();
        try {
            final Network nw = findOrCreateTunnelNetwork(conn, cmd.getBridgeName());
            final String bridgeName = nw.getBridge(conn);
            final long sequenceNo = cmd.getSequenceNumber();
            final String result = callHostPlugin(conn, "ovstunnel", "configure_ovs_bridge_for_network_topology", "bridge",
                    bridgeName, "config", cmd.getVpcConfigInJson(), "host-id", ((Long)cmd.getHostId()).toString(),
                    "seq-no", Long.toString(sequenceNo));
            if (result.startsWith("SUCCESS")) {
                return new Answer(cmd, true, result);
            } else {
                return new Answer(cmd, false, result);
            }
        } catch  (final Exception e) {
            s_logger.warn("caught exception while updating host with latest VPC topology", e);
            return new Answer(cmd, false, e.getMessage());
        }
    }

    public Answer execute(final OvsVpcRoutingPolicyConfigCommand cmd) {
        final Connection conn = getConnection();
        try {
            final Network nw = findOrCreateTunnelNetwork(conn, cmd.getBridgeName());
            final String bridgeName = nw.getBridge(conn);
            final long sequenceNo = cmd.getSequenceNumber();

            final String result = callHostPlugin(conn, "ovstunnel", "configure_ovs_bridge_for_routing_policies", "bridge",
                    bridgeName, "host-id", ((Long)cmd.getHostId()).toString(), "config",
                    cmd.getVpcConfigInJson(), "seq-no", Long.toString(sequenceNo));
            if (result.startsWith("SUCCESS")) {
                return new Answer(cmd, true, result);
            } else {
                return new Answer(cmd, false, result);
            }
        } catch  (final Exception e) {
            s_logger.warn("caught exception while updating host with latest routing policies", e);
            return new Answer(cmd, false, e.getMessage());
        }
    }

    private Answer execute(final UpdateHostPasswordCommand cmd) {
        _password.add(cmd.getNewPassword());
        return new Answer(cmd, true, null);
    }

    private OvsCreateTunnelAnswer execute(final OvsCreateTunnelCommand cmd) {
        final Connection conn = getConnection();
        String bridge = "unknown";
        try {
            final Network nw = findOrCreateTunnelNetwork(conn, cmd.getNetworkName());
            if (nw == null) {
                s_logger.debug("Error during bridge setup");
                return new OvsCreateTunnelAnswer(cmd, false, "Cannot create network", bridge);
            }

            configureTunnelNetwork(conn, cmd.getNetworkId(), cmd.getFrom(), cmd.getNetworkName());
            bridge = nw.getBridge(conn);
            final String result =
                    callHostPlugin(conn, "ovstunnel", "create_tunnel", "bridge", bridge, "remote_ip", cmd.getRemoteIp(),
                            "key", cmd.getKey().toString(), "from",
                            cmd.getFrom().toString(), "to", cmd.getTo().toString(), "cloudstack-network-id",
                            cmd.getNetworkUuid());
            final String[] res = result.split(":");
            if (res.length == 2 && res[0].equalsIgnoreCase("SUCCESS")) {
                return new OvsCreateTunnelAnswer(cmd, true, result, res[1], bridge);
            } else {
                return new OvsCreateTunnelAnswer(cmd, false, result, bridge);
            }
        } catch (final Exception e) {
            s_logger.debug("Error during tunnel setup");
            s_logger.warn("Caught execption when creating ovs tunnel", e);
            return new OvsCreateTunnelAnswer(cmd, false, e.getMessage(), bridge);
        }
    }

    private Answer execute(final OvsDeleteFlowCommand cmd) {
        _isOvs = true;

        final Connection conn = getConnection();
        try {
            final Network nw = setupvSwitchNetwork(conn);
            final String bridge = nw.getBridge(conn);
            final String result = callHostPlugin(conn, "ovsgre", "ovs_delete_flow", "bridge", bridge, "vmName", cmd.getVmName());

            if (result.equalsIgnoreCase("SUCCESS")) {
                return new Answer(cmd, true, "success to delete flows for " + cmd.getVmName());
            } else {
                return new Answer(cmd, false, result);
            }
        } catch (final BadServerResponse e) {
            s_logger.error("Failed to delete flow", e);
        } catch (final XenAPIException e) {
            s_logger.error("Failed to delete flow", e);
        } catch (final XmlRpcException e) {
            s_logger.error("Failed to delete flow", e);
        }
        return new Answer(cmd, false, "failed to delete flow for " + cmd.getVmName());
    }

    private List<Pair<String, Long>> ovsFullSyncStates() {
        final Connection conn = getConnection();
        final String result = callHostPlugin(conn, "ovsgre", "ovs_get_vm_log", "host_uuid", _host.getUuid());
        final String[] logs = result != null ? result.split(";") : new String[0];
        final List<Pair<String, Long>> states = new ArrayList<Pair<String, Long>>();
        for (final String log : logs) {
            final String[] info = log.split(",");
            if (info.length != 5) {
                s_logger.warn("Wrong element number in ovs log(" + log + ")");
                continue;
            }

            //','.join([bridge, vmName, vmId, seqno, tag])
            try {
                states.add(new Pair<String, Long>(info[0], Long.parseLong(info[3])));
            } catch (final NumberFormatException nfe) {
                states.add(new Pair<String, Long>(info[0], -1L));
            }
        }
        return states;
    }

    private OvsSetTagAndFlowAnswer execute(final OvsSetTagAndFlowCommand cmd) {
        _isOvs = true;

        final Connection conn = getConnection();
        try {
            final Network nw = setupvSwitchNetwork(conn);
            final String bridge = nw.getBridge(conn);

            /*If VM is domainRouter, this will try to set flow and tag on its
             * none guest network nic. don't worry, it will fail silently at host
             * plugin side
             */
            final String result =
                    callHostPlugin(conn, "ovsgre", "ovs_set_tag_and_flow", "bridge", bridge, "vmName", cmd.getVmName(), "tag", cmd.getTag(), "vlans", cmd.getVlans(),
                            "seqno", cmd.getSeqNo());
            s_logger.debug("set flow for " + cmd.getVmName() + " " + result);

            if (result.equalsIgnoreCase("SUCCESS")) {
                return new OvsSetTagAndFlowAnswer(cmd, true, result);
            } else {
                return new OvsSetTagAndFlowAnswer(cmd, false, result);
            }
        } catch (final BadServerResponse e) {
            s_logger.error("Failed to set tag and flow", e);
        } catch (final XenAPIException e) {
            s_logger.error("Failed to set tag and flow", e);
        } catch (final XmlRpcException e) {
            s_logger.error("Failed to set tag and flow", e);
        }

        return new OvsSetTagAndFlowAnswer(cmd, false, "EXCEPTION");
    }

    private OvsFetchInterfaceAnswer execute(final OvsFetchInterfaceCommand cmd) {

        String label = cmd.getLabel();
        //FIXME: this is a tricky to pass the network checking in XCP. I temporary get default label from Host.
        if (isXcp()) {
            label = getLabel();
        }
        s_logger.debug("Will look for network with name-label:" + label + " on host " + _host.getIp());
        final Connection conn = getConnection();
        try {
            final XsLocalNetwork nw = getNetworkByName(conn, label);
            if(nw == null) {
                throw new CloudRuntimeException("Unable to locate the network with name-label: " + label + " on host: " + _host.getIp());
            }
            s_logger.debug("Network object:" + nw.getNetwork().getUuid(conn));
            final PIF pif = nw.getPif(conn);
            final PIF.Record pifRec = pif.getRecord(conn);
            s_logger.debug("PIF object:" + pifRec.uuid + "(" + pifRec.device + ")");
            return new OvsFetchInterfaceAnswer(cmd, true, "Interface " + pifRec.device + " retrieved successfully", pifRec.IP, pifRec.netmask, pifRec.MAC);
        } catch (final BadServerResponse e) {
            s_logger.error("An error occurred while fetching the interface for " + label + " on host " + _host.getIp(), e);
            return new OvsFetchInterfaceAnswer(cmd, false, "EXCEPTION:" + e.getMessage());
        } catch (final XenAPIException e) {
            s_logger.error("An error occurred while fetching the interface for " + label + " on host " + _host.getIp(), e);
            return new OvsFetchInterfaceAnswer(cmd, false, "EXCEPTION:" + e.getMessage());
        } catch (final XmlRpcException e) {
            s_logger.error("An error occurred while fetching the interface for " + label + " on host " + _host.getIp(), e);
            return new OvsFetchInterfaceAnswer(cmd, false, "EXCEPTION:" + e.getMessage());
        }
    }

    private OvsCreateGreTunnelAnswer execute(final OvsCreateGreTunnelCommand cmd) {
        _isOvs = true;

        final Connection conn = getConnection();
        String bridge = "unkonwn";
        try {
            final Network nw = setupvSwitchNetwork(conn);
            bridge = nw.getBridge(conn);

            final String result =
                    callHostPlugin(conn, "ovsgre", "ovs_create_gre", "bridge", bridge, "remoteIP", cmd.getRemoteIp(), "greKey", cmd.getKey(), "from",
                            Long.toString(cmd.getFrom()), "to", Long.toString(cmd.getTo()));
            final String[] res = result.split(":");
            if (res.length != 2 || res.length == 2 && res[1].equalsIgnoreCase("[]")) {
                return new OvsCreateGreTunnelAnswer(cmd, false, result, _host.getIp(), bridge);
            } else {
                return new OvsCreateGreTunnelAnswer(cmd, true, result, _host.getIp(), bridge, Integer.parseInt(res[1]));
            }
        } catch (final BadServerResponse e) {
            s_logger.error("An error occurred while creating a GRE tunnel to " + cmd.getRemoteIp() + " on host " + _host.getIp(), e);
        } catch (final XenAPIException e) {
            s_logger.error("An error occurred while creating a GRE tunnel to " + cmd.getRemoteIp() + " on host " + _host.getIp(), e);
        } catch (final XmlRpcException e) {
            s_logger.error("An error occurred while creating a GRE tunnel to " + cmd.getRemoteIp() + " on host " + _host.getIp(), e);
        }

        return new OvsCreateGreTunnelAnswer(cmd, false, "EXCEPTION", _host.getIp(), bridge);
    }

    private Answer execute(final SecurityGroupRulesCmd cmd) {
        final Connection conn = getConnection();
        if (s_logger.isTraceEnabled()) {
            s_logger.trace("Sending network rules command to " + _host.getIp());
        }

        if (!_canBridgeFirewall) {
            s_logger.warn("Host " + _host.getIp() + " cannot do bridge firewalling");
            return new SecurityGroupRuleAnswer(cmd, false, "Host " + _host.getIp() + " cannot do bridge firewalling",
                    SecurityGroupRuleAnswer.FailureReason.CANNOT_BRIDGE_FIREWALL);
        }

        final String result =
                callHostPlugin(conn, "vmops", "network_rules", "vmName", cmd.getVmName(), "vmIP", cmd.getGuestIp(), "vmMAC", cmd.getGuestMac(), "vmID",
                        Long.toString(cmd.getVmId()), "signature", cmd.getSignature(), "seqno", Long.toString(cmd.getSeqNum()), "deflated", "true", "rules",
                        cmd.compressStringifiedRules(), "secIps", cmd.getSecIpsString());

        if (result == null || result.isEmpty() || !Boolean.parseBoolean(result)) {
            s_logger.warn("Failed to program network rules for vm " + cmd.getVmName());
            return new SecurityGroupRuleAnswer(cmd, false, "programming network rules failed");
        } else {
            s_logger.info("Programmed network rules for vm " + cmd.getVmName() + " guestIp=" + cmd.getGuestIp() + ", ingress numrules=" + cmd.getIngressRuleSet().length +
                    ", egress numrules=" + cmd.getEgressRuleSet().length);
            return new SecurityGroupRuleAnswer(cmd);
        }
    }

    protected Answer execute(final DeleteStoragePoolCommand cmd) {
        final Connection conn = getConnection();
        final StorageFilerTO poolTO = cmd.getPool();
        try {
            final SR sr = getStorageRepository(conn, poolTO.getUuid());
            removeSR(conn, sr);
            final Answer answer = new Answer(cmd, true, "success");
            return answer;
        } catch (final Exception e) {
            final String msg = "DeleteStoragePoolCommand XenAPIException:" + e.getMessage() + " host:" + _host.getUuid() + " pool: " + poolTO.getHost() + poolTO.getPath();
            s_logger.warn(msg, e);
            return new Answer(cmd, false, msg);
        }

    }

    public Connection getConnection() {
        return ConnPool.connect(_host.getUuid(), _host.getPool(), _host.getIp(), _username, _password, _wait);
    }


    protected void fillHostInfo(final Connection conn, final StartupRoutingCommand cmd) {
        final StringBuilder caps = new StringBuilder();
        try {

            final Host host = Host.getByUuid(conn, _host.getUuid());
            final Host.Record hr = host.getRecord(conn);

            Map<String, String> details = cmd.getHostDetails();
            if (details == null) {
                details = new HashMap<String, String>();
            }

            String productBrand = hr.softwareVersion.get("product_brand");
            if (productBrand == null) {
                productBrand = hr.softwareVersion.get("platform_name");
            }
            details.put("product_brand", productBrand);
            details.put("product_version", _host.getProductVersion());
            if (hr.softwareVersion.get("product_version_text_short") != null) {
                details.put("product_version_text_short", hr.softwareVersion.get("product_version_text_short"));
                cmd.setHypervisorVersion(hr.softwareVersion.get("product_version_text_short"));

                cmd.setHypervisorVersion(_host.getProductVersion());
            }
            if (_privateNetworkName != null) {
                details.put("private.network.device", _privateNetworkName);
            }

            cmd.setHostDetails(details);
            cmd.setName(hr.nameLabel);
            cmd.setGuid(_host.getUuid());
            cmd.setPool(_host.getPool());
            cmd.setDataCenter(Long.toString(_dcId));
            for (final String cap : hr.capabilities) {
                if (cap.length() > 0) {
                    caps.append(cap).append(" , ");
                }
            }
            if (caps.length() > 0) {
                caps.delete(caps.length() - 3, caps.length());
            }
            cmd.setCaps(caps.toString());

            cmd.setSpeed(_host.getSpeed());
            cmd.setCpuSockets(_host.getCpuSockets());
            cmd.setCpus(_host.getCpus());

            final HostMetrics hm = host.getMetrics(conn);

            long ram = 0;
            long dom0Ram = 0;
            ram = hm.getMemoryTotal(conn);
            final Set<VM> vms = host.getResidentVMs(conn);
            for (final VM vm : vms) {
                if (vm.getIsControlDomain(conn)) {
                    dom0Ram = vm.getMemoryStaticMax(conn);
                    break;
                }
            }

            ram = (long)((ram - dom0Ram - _xsMemoryUsed) * _xsVirtualizationFactor);
            cmd.setMemory(ram);
            cmd.setDom0MinMemory(dom0Ram);

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Total Ram: " + ram + " dom0 Ram: " + dom0Ram);
            }

            PIF pif = PIF.getByUuid(conn, _host.getPrivatePif());
            PIF.Record pifr = pif.getRecord(conn);
            if (pifr.IP != null && pifr.IP.length() > 0) {
                cmd.setPrivateIpAddress(pifr.IP);
                cmd.setPrivateMacAddress(pifr.MAC);
                cmd.setPrivateNetmask(pifr.netmask);
            } else {
                cmd.setPrivateIpAddress(_host.getIp());
                cmd.setPrivateMacAddress(pifr.MAC);
                cmd.setPrivateNetmask("255.255.255.0");
            }

            pif = PIF.getByUuid(conn, _host.getPublicPif());
            pifr = pif.getRecord(conn);
            if (pifr.IP != null && pifr.IP.length() > 0) {
                cmd.setPublicIpAddress(pifr.IP);
                cmd.setPublicMacAddress(pifr.MAC);
                cmd.setPublicNetmask(pifr.netmask);
            }

            if (_host.getStoragePif1() != null) {
                pif = PIF.getByUuid(conn, _host.getStoragePif1());
                pifr = pif.getRecord(conn);
                if (pifr.IP != null && pifr.IP.length() > 0) {
                    cmd.setStorageIpAddress(pifr.IP);
                    cmd.setStorageMacAddress(pifr.MAC);
                    cmd.setStorageNetmask(pifr.netmask);
                }
            }

            if (_host.getStoragePif2() != null) {
                pif = PIF.getByUuid(conn, _host.getStoragePif2());
                pifr = pif.getRecord(conn);
                if (pifr.IP != null && pifr.IP.length() > 0) {
                    cmd.setStorageIpAddressDeux(pifr.IP);
                    cmd.setStorageMacAddressDeux(pifr.MAC);
                    cmd.setStorageNetmaskDeux(pifr.netmask);
                }
            }

            final Map<String, String> configs = hr.otherConfig;
            cmd.setIqn(configs.get("iscsi_iqn"));

            cmd.setPod(_pod);
            cmd.setVersion(CitrixResourceBase.class.getPackage().getImplementationVersion());

        } catch (final XmlRpcException e) {
            throw new CloudRuntimeException("XML RPC Exception" + e.getMessage(), e);
        } catch (final XenAPIException e) {
            throw new CloudRuntimeException("XenAPIException" + e.toString(), e);
        }
    }

    public CitrixResourceBase() {
    }

    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        _name = name;

        try {
            _dcId = Long.parseLong((String)params.get("zone"));
        } catch (final NumberFormatException e) {
            throw new ConfigurationException("Unable to get the zone " + params.get("zone"));
        }

        _host.setUuid((String)params.get("guid"));

        _name = _host.getUuid();
        _host.setIp((String)params.get("ipaddress"));

        _username = (String)params.get("username");
        _password.add((String)params.get("password"));
        _pod = (String)params.get("pod");
        _cluster = (String)params.get("cluster");
        _privateNetworkName = (String)params.get("private.network.device");
        _publicNetworkName = (String)params.get("public.network.device");
        _guestNetworkName = (String)params.get("guest.network.device");
        _instance = (String)params.get("instance.name");
        _securityGroupEnabled = Boolean.parseBoolean((String)params.get("securitygroupenabled"));

        _linkLocalPrivateNetworkName = (String)params.get("private.linkLocal.device");
        if (_linkLocalPrivateNetworkName == null) {
            _linkLocalPrivateNetworkName = "cloud_link_local_network";
        }

        _storageNetworkName1 = (String)params.get("storage.network.device1");
        _storageNetworkName2 = (String)params.get("storage.network.device2");

        _heartbeatTimeout = NumbersUtil.parseInt((String)params.get("xenserver.heartbeat.timeout"), 120);
        _heartbeatInterval = NumbersUtil.parseInt((String)params.get("xenserver.heartbeat.interval"), 60);

        String value = (String)params.get("wait");
        _wait = NumbersUtil.parseInt(value, 600);

        value = (String)params.get("migratewait");
        _migratewait = NumbersUtil.parseInt(value, 3600);

        _maxNics = NumbersUtil.parseInt((String)params.get("xenserver.nics.max"), 7);

        if (_pod == null) {
            throw new ConfigurationException("Unable to get the pod");
        }

        if (_host.getIp() == null) {
            throw new ConfigurationException("Unable to get the host address");
        }

        if (_username == null) {
            throw new ConfigurationException("Unable to get the username");
        }

        if (_password.peek() == null) {
            throw new ConfigurationException("Unable to get the password");
        }

        if (_host.getUuid() == null) {
            throw new ConfigurationException("Unable to get the uuid");
        }

        CheckXenHostInfo();

        storageHandler = getStorageHandler();

        _vrResource = new VirtualRoutingResource(this);
        if (!_vrResource.configure(name, params)) {
            throw new ConfigurationException("Unable to configure VirtualRoutingResource");
        }
        return true;
    }

    protected StorageSubsystemCommandHandler getStorageHandler() {
        final XenServerStorageProcessor processor = new XenServerStorageProcessor(this);
        return new StorageSubsystemCommandHandlerBase(processor);
    }

    private void CheckXenHostInfo() throws ConfigurationException {
        final Connection conn = ConnPool.getConnect(_host.getIp(), _username, _password);
        if( conn == null ) {
            throw new ConfigurationException("Can not create connection to " + _host.getIp());
        }
        try {
            Host.Record hostRec = null;
            try {
                final Host host = Host.getByUuid(conn, _host.getUuid());
                hostRec = host.getRecord(conn);
                final Pool.Record poolRec = Pool.getAllRecords(conn).values().iterator().next();
                _host.setPool(poolRec.uuid);

            } catch (final Exception e) {
                throw new ConfigurationException("Can not get host information from " + _host.getIp());
            }
            if (!hostRec.address.equals(_host.getIp())) {
                final String msg = "Host " + _host.getIp() + " seems be reinstalled, please remove this host and readd";
                s_logger.error(msg);
                throw new ConfigurationException(msg);
            }
        } finally {
            try {
                Session.logout(conn);
            } catch (final Exception e) {
            }
        }
    }


    public CreateAnswer execute(final CreateCommand cmd) {
        final Connection conn = getConnection();
        final StorageFilerTO pool = cmd.getPool();
        final DiskProfile dskch = cmd.getDiskCharacteristics();
        VDI vdi = null;
        try {
            final SR poolSr = getStorageRepository(conn, pool.getUuid());
            if (cmd.getTemplateUrl() != null) {
                VDI tmpltvdi = null;

                tmpltvdi = getVDIbyUuid(conn, cmd.getTemplateUrl());
                vdi = tmpltvdi.createClone(conn, new HashMap<String, String>());
                vdi.setNameLabel(conn, dskch.getName());
            } else {
                final VDI.Record vdir = new VDI.Record();
                vdir.nameLabel = dskch.getName();
                vdir.SR = poolSr;
                vdir.type = Types.VdiType.USER;

                vdir.virtualSize = dskch.getSize();
                vdi = VDI.create(conn, vdir);
            }

            VDI.Record vdir;
            vdir = vdi.getRecord(conn);
            s_logger.debug("Succesfully created VDI for " + cmd + ".  Uuid = " + vdir.uuid);

            final VolumeTO vol =
                    new VolumeTO(cmd.getVolumeId(), dskch.getType(), pool.getType(), pool.getUuid(), vdir.nameLabel, pool.getPath(), vdir.uuid, vdir.virtualSize, null);
            return new CreateAnswer(cmd, vol);
        } catch (final Exception e) {
            s_logger.warn("Unable to create volume; Pool=" + pool + "; Disk: " + dskch, e);
            return new CreateAnswer(cmd, e);
        }
    }

    public Answer execute(final ResizeVolumeCommand cmd) {
        final Connection conn = getConnection();
        final String volid = cmd.getPath();
        final long newSize = cmd.getNewSize();

        try {
            final VDI vdi = getVDIbyUuid(conn, volid);
            vdi.resize(conn, newSize);
            return new ResizeVolumeAnswer(cmd, true, "success", newSize);
        } catch (final Exception e) {
            s_logger.warn("Unable to resize volume", e);
            final String error = "failed to resize volume:" + e;
            return new ResizeVolumeAnswer(cmd, false, error);
        }
    }

    public SR getISOSRbyVmName(final Connection conn, final String vmName) {
        try {
            final Set<SR> srs = SR.getByNameLabel(conn, vmName + "-ISO");
            if (srs.size() == 0) {
                return null;
            } else if (srs.size() == 1) {
                return srs.iterator().next();
            } else {
                final String msg = "getIsoSRbyVmName failed due to there are more than 1 SR having same Label";
                s_logger.warn(msg);
            }
        } catch (final XenAPIException e) {
            final String msg = "getIsoSRbyVmName failed due to " + e.toString();
            s_logger.warn(msg, e);
        } catch (final Exception e) {
            final String msg = "getIsoSRbyVmName failed due to " + e.getMessage();
            s_logger.warn(msg, e);
        }
        return null;
    }

    protected SR createNfsSRbyURI(final Connection conn, final URI uri, final boolean shared) {
        try {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Creating a " + (shared ? "shared SR for " : "not shared SR for ") + uri);
            }

            final Map<String, String> deviceConfig = new HashMap<String, String>();
            String path = uri.getPath();
            path = path.replace("//", "/");
            deviceConfig.put("server", uri.getHost());
            deviceConfig.put("serverpath", path);
            final String name = UUID.nameUUIDFromBytes(new String(uri.getHost() + path).getBytes()).toString();
            if (!shared) {
                final Set<SR> srs = SR.getByNameLabel(conn, name);
                for (final SR sr : srs) {
                    final SR.Record record = sr.getRecord(conn);
                    if (SRType.NFS.equals(record.type) && record.contentType.equals("user") && !record.shared) {
                        removeSRSync(conn, sr);
                    }
                }
            }

            final Host host = Host.getByUuid(conn, _host.getUuid());
            final Map<String, String> smConfig = new HashMap<String, String>();
            smConfig.put("nosubdir", "true");
            final SR sr = SR.create(conn, host, deviceConfig, new Long(0), name, uri.getHost() + uri.getPath(), SRType.NFS.toString(), "user", shared, smConfig);

            if (!checkSR(conn, sr)) {
                throw new Exception("no attached PBD");
            }
            if (s_logger.isDebugEnabled()) {
                s_logger.debug(logX(sr, "Created a SR; UUID is " + sr.getUuid(conn) + " device config is " + deviceConfig));
            }
            sr.scan(conn);
            return sr;
        } catch (final XenAPIException e) {
            final String msg = "Can not create second storage SR mountpoint: " + uri.getHost() + uri.getPath() + " due to " + e.toString();
            s_logger.warn(msg, e);
            throw new CloudRuntimeException(msg, e);
        } catch (final Exception e) {
            final String msg = "Can not create second storage SR mountpoint: " + uri.getHost() + uri.getPath() + " due to " + e.getMessage();
            s_logger.warn(msg, e);
            throw new CloudRuntimeException(msg, e);
        }
    }

    protected SR createIsoSRbyURI(final Connection conn, final URI uri, final String vmName, final boolean shared) {
        try {
            final Map<String, String> deviceConfig = new HashMap<String, String>();
            String path = uri.getPath();
            path = path.replace("//", "/");
            deviceConfig.put("location", uri.getHost() + ":" + path);
            final Host host = Host.getByUuid(conn, _host.getUuid());
            final SR sr = SR.create(conn, host, deviceConfig, new Long(0), uri.getHost() + path, "iso", "iso", "iso", shared, new HashMap<String, String>());
            sr.setNameLabel(conn, vmName + "-ISO");
            sr.setNameDescription(conn, deviceConfig.get("location"));

            sr.scan(conn);
            return sr;
        } catch (final XenAPIException e) {
            final String msg = "createIsoSRbyURI failed! mountpoint: " + uri.getHost() + uri.getPath() + " due to " + e.toString();
            s_logger.warn(msg, e);
            throw new CloudRuntimeException(msg, e);
        } catch (final Exception e) {
            final String msg = "createIsoSRbyURI failed! mountpoint: " + uri.getHost() + uri.getPath() + " due to " + e.getMessage();
            s_logger.warn(msg, e);
            throw new CloudRuntimeException(msg, e);
        }
    }

    protected VDI getVDIbyLocationandSR(final Connection conn, final String loc, final SR sr) {
        try {
            final Set<VDI> vdis = sr.getVDIs(conn);
            for (final VDI vdi : vdis) {
                if (vdi.getLocation(conn).startsWith(loc)) {
                    return vdi;
                }
            }

            final String msg = "can not getVDIbyLocationandSR " + loc;
            s_logger.warn(msg);
            return null;
        } catch (final XenAPIException e) {
            final String msg = "getVDIbyLocationandSR exception " + loc + " due to " + e.toString();
            s_logger.warn(msg, e);
            throw new CloudRuntimeException(msg, e);
        } catch (final Exception e) {
            final String msg = "getVDIbyLocationandSR exception " + loc + " due to " + e.getMessage();
            s_logger.warn(msg, e);
            throw new CloudRuntimeException(msg, e);
        }

    }

    public VDI getVDIbyUuid(final Connection conn, final String uuid) {
        return getVDIbyUuid(conn, uuid, true);
    }

    public VDI getVDIbyUuid(final Connection conn, final String uuid, final boolean throwExceptionIfNotFound) {
        try {
            return VDI.getByUuid(conn, uuid);
        } catch (final Exception e) {
            if (throwExceptionIfNotFound) {
                final String msg = "Catch Exception " + e.getClass().getName() + " :VDI getByUuid for uuid: " + uuid + " failed due to " + e.toString();

                s_logger.debug(msg);

                throw new CloudRuntimeException(msg, e);
            }

            return null;
        }
    }

    public SR getIscsiSR(final Connection conn, final String srNameLabel, final String target, String path, final String chapInitiatorUsername, final String chapInitiatorPassword,
            final boolean ignoreIntroduceException) {
        synchronized (srNameLabel.intern()) {
            final Map<String, String> deviceConfig = new HashMap<String, String>();
            try {
                if (path.endsWith("/")) {
                    path = path.substring(0, path.length() - 1);
                }

                final String tmp[] = path.split("/");
                if (tmp.length != 3) {
                    final String msg = "Wrong iscsi path " + path + " it should be /targetIQN/LUN";
                    s_logger.warn(msg);
                    throw new CloudRuntimeException(msg);
                }
                final String targetiqn = tmp[1].trim();
                final String lunid = tmp[2].trim();
                String scsiid = "";

                final Set<SR> srs = SR.getByNameLabel(conn, srNameLabel);
                for (final SR sr : srs) {
                    if (!SRType.LVMOISCSI.equals(sr.getType(conn))) {
                        continue;
                    }
                    final Set<PBD> pbds = sr.getPBDs(conn);
                    if (pbds.isEmpty()) {
                        continue;
                    }
                    final PBD pbd = pbds.iterator().next();
                    final Map<String, String> dc = pbd.getDeviceConfig(conn);
                    if (dc == null) {
                        continue;
                    }
                    if (dc.get("target") == null) {
                        continue;
                    }
                    if (dc.get("targetIQN") == null) {
                        continue;
                    }
                    if (dc.get("lunid") == null) {
                        continue;
                    }
                    if (target.equals(dc.get("target")) && targetiqn.equals(dc.get("targetIQN")) && lunid.equals(dc.get("lunid"))) {
                        throw new CloudRuntimeException("There is a SR using the same configuration target:" + dc.get("target") + ",  targetIQN:" + dc.get("targetIQN") +
                                ", lunid:" + dc.get("lunid") + " for pool " + srNameLabel + "on host:" + _host.getUuid());
                    }
                }
                deviceConfig.put("target", target);
                deviceConfig.put("targetIQN", targetiqn);

                if (StringUtils.isNotBlank(chapInitiatorUsername) && StringUtils.isNotBlank(chapInitiatorPassword)) {
                    deviceConfig.put("chapuser", chapInitiatorUsername);
                    deviceConfig.put("chappassword", chapInitiatorPassword);
                }

                final Host host = Host.getByUuid(conn, _host.getUuid());
                final Map<String, String> smConfig = new HashMap<String, String>();
                final String type = SRType.LVMOISCSI.toString();
                SR sr = null;
                try {
                    sr = SR.create(conn, host, deviceConfig, new Long(0), srNameLabel, srNameLabel, type, "user", true, smConfig);
                } catch (final XenAPIException e) {
                    final String errmsg = e.toString();
                    if (errmsg.contains("SR_BACKEND_FAILURE_107")) {
                        final String lun[] = errmsg.split("<LUN>");
                        boolean found = false;
                        for (int i = 1; i < lun.length; i++) {
                            final int blunindex = lun[i].indexOf("<LUNid>") + 7;
                            final int elunindex = lun[i].indexOf("</LUNid>");
                            String ilun = lun[i].substring(blunindex, elunindex);
                            ilun = ilun.trim();
                            if (ilun.equals(lunid)) {
                                final int bscsiindex = lun[i].indexOf("<SCSIid>") + 8;
                                final int escsiindex = lun[i].indexOf("</SCSIid>");
                                scsiid = lun[i].substring(bscsiindex, escsiindex);
                                scsiid = scsiid.trim();
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            final String msg = "can not find LUN " + lunid + " in " + errmsg;
                            s_logger.warn(msg);
                            throw new CloudRuntimeException(msg);
                        }
                    } else {
                        final String msg = "Unable to create Iscsi SR  " + deviceConfig + " due to  " + e.toString();
                        s_logger.warn(msg, e);
                        throw new CloudRuntimeException(msg, e);
                    }
                }
                deviceConfig.put("SCSIid", scsiid);

                final String result = SR.probe(conn, host, deviceConfig, type, smConfig);
                String pooluuid = null;
                if (result.indexOf("<UUID>") != -1) {
                    pooluuid = result.substring(result.indexOf("<UUID>") + 6, result.indexOf("</UUID>")).trim();
                }

                if (pooluuid == null || pooluuid.length() != 36) {
                    sr = SR.create(conn, host, deviceConfig, new Long(0), srNameLabel, srNameLabel, type, "user", true, smConfig);
                } else {
                    try {
                        sr = SR.introduce(conn, pooluuid, srNameLabel, srNameLabel, type, "user", true, smConfig);
                    } catch (final XenAPIException ex) {
                        if (ignoreIntroduceException) {
                            return sr;
                        }

                        throw ex;
                    }

                    final Set<Host> setHosts = Host.getAll(conn);
                    if(setHosts == null) {
                        final String msg = "Unable to create Iscsi SR  " + deviceConfig + " due to hosts not available.";
                        s_logger.warn(msg);
                        throw new CloudRuntimeException(msg);
                    }
                    for (final Host currentHost : setHosts) {
                        final PBD.Record rec = new PBD.Record();

                        rec.deviceConfig = deviceConfig;
                        rec.host = currentHost;
                        rec.SR = sr;

                        final PBD pbd = PBD.create(conn, rec);

                        pbd.plug(conn);
                    }
                }
                sr.scan(conn);
                return sr;
            } catch (final XenAPIException e) {
                final String msg = "Unable to create Iscsi SR  " + deviceConfig + " due to  " + e.toString();
                s_logger.warn(msg, e);
                throw new CloudRuntimeException(msg, e);
            } catch (final Exception e) {
                final String msg = "Unable to create Iscsi SR  " + deviceConfig + " due to  " + e.getMessage();
                s_logger.warn(msg, e);
                throw new CloudRuntimeException(msg, e);
            }
        }
    }

    public SR getNfsSR(final Connection conn, final String poolid, final String uuid, final String server, String serverpath, final String pooldesc) {
        final Map<String, String> deviceConfig = new HashMap<String, String>();
        try {
            serverpath = serverpath.replace("//", "/");
            final Set<SR> srs = SR.getAll(conn);
            if(srs != null && !srs.isEmpty()) {
                for (final SR sr : srs) {
                    if (!SRType.NFS.equals(sr.getType(conn))) {
                        continue;
                    }

                    final Set<PBD> pbds = sr.getPBDs(conn);
                    if (pbds.isEmpty()) {
                        continue;
                    }

                    final PBD pbd = pbds.iterator().next();

                    final Map<String, String> dc = pbd.getDeviceConfig(conn);

                    if (dc == null) {
                        continue;
                    }

                    if (dc.get("server") == null) {
                        continue;
                    }

                    if (dc.get("serverpath") == null) {
                        continue;
                    }

                    if (server.equals(dc.get("server")) && serverpath.equals(dc.get("serverpath"))) {
                        throw new CloudRuntimeException("There is a SR using the same configuration server:" + dc.get("server") + ", serverpath:" + dc.get("serverpath") +
                                " for pool " + uuid + " on host:" + _host.getUuid());
                    }

                }
            }
            deviceConfig.put("server", server);
            deviceConfig.put("serverpath", serverpath);
            final Host host = Host.getByUuid(conn, _host.getUuid());
            final Map<String, String> smConfig = new HashMap<String, String>();
            smConfig.put("nosubdir", "true");
            final SR sr = SR.create(conn, host, deviceConfig, new Long(0), uuid, poolid, SRType.NFS.toString(), "user", true, smConfig);
            sr.scan(conn);
            return sr;
        } catch (final XenAPIException e) {
            throw new CloudRuntimeException("Unable to create NFS SR " + pooldesc, e);
        } catch (final XmlRpcException e) {
            throw new CloudRuntimeException("Unable to create NFS SR " + pooldesc, e);
        }
    }

    public Answer execute(final DestroyCommand cmd) {
        final Connection conn = getConnection();
        final VolumeTO vol = cmd.getVolume();
        // Look up the VDI
        final String volumeUUID = vol.getPath();
        VDI vdi = null;
        try {
            vdi = getVDIbyUuid(conn, volumeUUID);
        } catch (final Exception e) {
            return new Answer(cmd, true, "Success");
        }
        Set<VBD> vbds = null;
        try {
            vbds = vdi.getVBDs(conn);
        } catch (final Exception e) {
            final String msg = "VDI getVBDS for " + volumeUUID + " failed due to " + e.toString();
            s_logger.warn(msg, e);
            return new Answer(cmd, false, msg);
        }
        for (final VBD vbd : vbds) {
            try {
                vbd.unplug(conn);
                vbd.destroy(conn);
            } catch (final Exception e) {
                final String msg = "VM destroy for " + volumeUUID + "  failed due to " + e.toString();
                s_logger.warn(msg, e);
                return new Answer(cmd, false, msg);
            }
        }
        try {
            final Set<VDI> snapshots = vdi.getSnapshots(conn);
            for (final VDI snapshot : snapshots) {
                snapshot.destroy(conn);
            }
            vdi.destroy(conn);
        } catch (final Exception e) {
            final String msg = "VDI destroy for " + volumeUUID + " failed due to " + e.toString();
            s_logger.warn(msg, e);
            return new Answer(cmd, false, msg);
        }

        return new Answer(cmd, true, "Success");
    }

    public VDI createVdi(final SR sr, final String vdiNameLabel, final Long volumeSize) throws Types.XenAPIException, XmlRpcException {
        final Connection conn = getConnection();

        final VDI.Record vdir = new VDI.Record();

        vdir.nameLabel = vdiNameLabel;
        vdir.SR = sr;
        vdir.type = Types.VdiType.USER;

        final long totalSrSpace = sr.getPhysicalSize(conn);
        final long unavailableSrSpace = sr.getPhysicalUtilisation(conn);
        final long availableSrSpace = totalSrSpace - unavailableSrSpace;

        if (availableSrSpace < volumeSize) {
            throw new CloudRuntimeException("Available space for SR cannot be less than " + volumeSize + ".");
        }

        vdir.virtualSize = volumeSize;

        return VDI.create(conn, vdir);
    }

    public void handleSrAndVdiDetach(final String iqn, final Connection conn) throws Exception {
        final SR sr = getStorageRepository(conn, iqn);

        removeSR(conn, sr);
    }

    protected AttachVolumeAnswer execute(final AttachVolumeCommand cmd) {
        final Connection conn = getConnection();
        final boolean attach = cmd.getAttach();
        final String vmName = cmd.getVmName();
        final String vdiNameLabel = vmName + "-DATA";
        final Long deviceId = cmd.getDeviceId();

        String errorMsg;
        if (attach) {
            errorMsg = "Failed to attach volume";
        } else {
            errorMsg = "Failed to detach volume";
        }

        try {
            VDI vdi = null;

            if (cmd.getAttach() && cmd.isManaged()) {
                final SR sr = getIscsiSR(conn, cmd.get_iScsiName(), cmd.getStorageHost(), cmd.get_iScsiName(), cmd.getChapInitiatorUsername(), cmd.getChapInitiatorPassword(), true);

                vdi = getVDIbyUuid(conn, cmd.getVolumePath(), false);

                if (vdi == null) {
                    vdi = createVdi(sr, vdiNameLabel, cmd.getVolumeSize());
                }
            } else {
                vdi = getVDIbyUuid(conn, cmd.getVolumePath());
            }

            // Look up the VM
            final VM vm = getVM(conn, vmName);
            if (attach) {
                // Figure out the disk number to attach the VM to
                String diskNumber = null;
                if (deviceId != null) {
                    if (deviceId.longValue() == 3) {
                        final String msg = "Device 3 is reserved for CD-ROM, choose other device";
                        return new AttachVolumeAnswer(cmd, msg);
                    }
                    if (isDeviceUsed(conn, vm, deviceId)) {
                        final String msg = "Device " + deviceId + " is used in VM " + vmName;
                        return new AttachVolumeAnswer(cmd, msg);
                    }
                    diskNumber = deviceId.toString();
                } else {
                    diskNumber = getUnusedDeviceNum(conn, vm);
                }
                // Create a new VBD
                final VBD.Record vbdr = new VBD.Record();
                vbdr.VM = vm;
                vbdr.VDI = vdi;
                vbdr.bootable = false;
                vbdr.userdevice = diskNumber;
                vbdr.mode = Types.VbdMode.RW;
                vbdr.type = Types.VbdType.DISK;
                vbdr.unpluggable = true;
                final VBD vbd = VBD.create(conn, vbdr);

                // Attach the VBD to the VM
                vbd.plug(conn);

                // Update the VDI's label to include the VM name
                vdi.setNameLabel(conn, vdiNameLabel);

                return new AttachVolumeAnswer(cmd, Long.parseLong(diskNumber), vdi.getUuid(conn));
            } else {
                // Look up all VBDs for this VDI
                final Set<VBD> vbds = vdi.getVBDs(conn);

                // Detach each VBD from its VM, and then destroy it
                for (final VBD vbd : vbds) {
                    final VBD.Record vbdr = vbd.getRecord(conn);

                    if (vbdr.currentlyAttached) {
                        vbd.unplug(conn);
                    }

                    vbd.destroy(conn);
                }

                // Update the VDI's label to be "detached"
                vdi.setNameLabel(conn, "detached");

                if (cmd.isManaged()) {
                    handleSrAndVdiDetach(cmd.get_iScsiName(), conn);
                }

                return new AttachVolumeAnswer(cmd);
            }
        } catch (final XenAPIException e) {
            final String msg = errorMsg + " for uuid: " + cmd.getVolumePath() + "  due to " + e.toString();
            s_logger.warn(msg, e);
            return new AttachVolumeAnswer(cmd, msg);
        } catch (final Exception e) {
            final String msg = errorMsg + " for uuid: " + cmd.getVolumePath() + "  due to " + e.getMessage();
            s_logger.warn(msg, e);
            return new AttachVolumeAnswer(cmd, msg);
        }

    }

    protected void umount(final Connection conn, final VDI vdi) {

    }

    private long getVMSnapshotChainSize(final Connection conn, final VolumeObjectTO volumeTo, final String vmName) throws BadServerResponse, XenAPIException, XmlRpcException {
        final Set<VDI> allvolumeVDIs = VDI.getByNameLabel(conn, volumeTo.getName());
        long size = 0;
        for (final VDI vdi : allvolumeVDIs) {
            try {
                if (vdi.getIsASnapshot(conn) && vdi.getSmConfig(conn).get("vhd-parent") != null) {
                    final String parentUuid = vdi.getSmConfig(conn).get("vhd-parent");
                    final VDI parentVDI = VDI.getByUuid(conn, parentUuid);
                    // add size of snapshot vdi node, usually this only contains meta data
                    size = size + vdi.getPhysicalUtilisation(conn);
                    // add size of snapshot vdi parent, this contains data
                    if (!isRefNull(parentVDI)) {
                        size = size + parentVDI.getPhysicalUtilisation(conn).longValue();
                    }
                }
            } catch (final Exception e) {
                s_logger.debug("Exception occurs when calculate snapshot capacity for volumes: due to " + e.toString());
                continue;
            }
        }
        if (volumeTo.getVolumeType() == Volume.Type.ROOT) {
            final Map<VM, VM.Record> allVMs = VM.getAllRecords(conn);
            // add size of memory snapshot vdi
            if (allVMs != null && allVMs.size() > 0) {
                for (final VM vmr : allVMs.keySet()) {
                    try {
                        final String vName = vmr.getNameLabel(conn);
                        if (vName != null && vName.contains(vmName) && vmr.getIsASnapshot(conn)) {
                            final VDI memoryVDI = vmr.getSuspendVDI(conn);
                            if (!isRefNull(memoryVDI)) {
                                size = size + memoryVDI.getPhysicalUtilisation(conn);
                                final VDI pMemoryVDI = memoryVDI.getParent(conn);
                                if (!isRefNull(pMemoryVDI)) {
                                    size = size + pMemoryVDI.getPhysicalUtilisation(conn);
                                }
                            }
                        }
                    } catch (final Exception e) {
                        s_logger.debug("Exception occurs when calculate snapshot capacity for memory: due to " + e.toString());
                        continue;
                    }
                }
            }
        }
        return size;
    }

    protected Answer execute(final CreateVMSnapshotCommand cmd) {
        final String vmName = cmd.getVmName();
        final String vmSnapshotName = cmd.getTarget().getSnapshotName();
        final List<VolumeObjectTO> listVolumeTo = cmd.getVolumeTOs();
        VmPowerState vmState = VmPowerState.HALTED;
        final String guestOSType = cmd.getGuestOSType();
        final String platformEmulator = cmd.getPlatformEmulator();

        final boolean snapshotMemory = cmd.getTarget().getType() == VMSnapshot.Type.DiskAndMemory;
        final long timeout = cmd.getWait();

        final Connection conn = getConnection();
        VM vm = null;
        VM vmSnapshot = null;
        boolean success = false;

        try {
            // check if VM snapshot already exists
            final Set<VM> vmSnapshots = VM.getByNameLabel(conn, cmd.getTarget().getSnapshotName());
            if (vmSnapshots.size() > 0) {
                return new CreateVMSnapshotAnswer(cmd, cmd.getTarget(), cmd.getVolumeTOs());
            }

            // check if there is already a task for this VM snapshot
            Task task = null;
            Set<Task> tasks = Task.getByNameLabel(conn, "Async.VM.snapshot");
            if(tasks == null) {
                tasks = new LinkedHashSet<>();
            }
            final Set<Task> tasksByName = Task.getByNameLabel(conn, "Async.VM.checkpoint");
            if(tasksByName != null) {
                tasks.addAll(tasksByName);
            }
            for (final Task taskItem : tasks) {
                if (taskItem.getOtherConfig(conn).containsKey("CS_VM_SNAPSHOT_KEY")) {
                    final String vmSnapshotTaskName = taskItem.getOtherConfig(conn).get("CS_VM_SNAPSHOT_KEY");
                    if (vmSnapshotTaskName != null && vmSnapshotTaskName.equals(cmd.getTarget().getSnapshotName())) {
                        task = taskItem;
                    }
                }
            }

            // create a new task if there is no existing task for this VM snapshot
            if (task == null) {
                try {
                    vm = getVM(conn, vmName);
                    vmState = vm.getPowerState(conn);
                } catch (final Exception e) {
                    if (!snapshotMemory) {
                        vm = createWorkingVM(conn, vmName, guestOSType, platformEmulator, listVolumeTo);
                    }
                }

                if (vm == null) {
                    return new CreateVMSnapshotAnswer(cmd, false, "Creating VM Snapshot Failed due to can not find vm: " + vmName);
                }

                // call Xenserver API
                if (!snapshotMemory) {
                    task = vm.snapshotAsync(conn, vmSnapshotName);
                } else {
                    final Set<VBD> vbds = vm.getVBDs(conn);
                    final Pool pool = Pool.getByUuid(conn, _host.getPool());
                    for (final VBD vbd : vbds) {
                        final VBD.Record vbdr = vbd.getRecord(conn);
                        if (vbdr.userdevice.equals("0")) {
                            final VDI vdi = vbdr.VDI;
                            final SR sr = vdi.getSR(conn);
                            // store memory image on the same SR with ROOT volume
                            pool.setSuspendImageSR(conn, sr);
                        }
                    }
                    task = vm.checkpointAsync(conn, vmSnapshotName);
                }
                task.addToOtherConfig(conn, "CS_VM_SNAPSHOT_KEY", vmSnapshotName);
            }

            waitForTask(conn, task, 1000, timeout * 1000);
            checkForSuccess(conn, task);
            final String result = task.getResult(conn);

            // extract VM snapshot ref from result
            final String ref = result.substring("<value>".length(), result.length() - "</value>".length());
            vmSnapshot = Types.toVM(ref);
            try {
                Thread.sleep(5000);
            } catch (final InterruptedException ex) {

            }
            // calculate used capacity for this VM snapshot
            for (final VolumeObjectTO volumeTo : cmd.getVolumeTOs()) {
                final long size = getVMSnapshotChainSize(conn, volumeTo, cmd.getVmName());
                volumeTo.setSize(size);
            }

            success = true;
            return new CreateVMSnapshotAnswer(cmd, cmd.getTarget(), cmd.getVolumeTOs());
        } catch (final Exception e) {
            String msg = "";
            if (e instanceof Types.BadAsyncResult) {
                final String licenseKeyWord = "LICENCE_RESTRICTION";
                final Types.BadAsyncResult errorResult = (Types.BadAsyncResult)e;
                if (errorResult.shortDescription != null && errorResult.shortDescription.contains(licenseKeyWord)) {
                    msg = licenseKeyWord;
                }
            } else {
                msg = e.toString();
            }
            s_logger.warn("Creating VM Snapshot " + cmd.getTarget().getSnapshotName() + " failed due to: " + msg, e);
            return new CreateVMSnapshotAnswer(cmd, false, msg);
        } finally {
            try {
                if (!success) {
                    if (vmSnapshot != null) {
                        s_logger.debug("Delete exsisting VM Snapshot " + vmSnapshotName + " after making VolumeTO failed");
                        final Set<VBD> vbds = vmSnapshot.getVBDs(conn);
                        for (final VBD vbd : vbds) {
                            final VBD.Record vbdr = vbd.getRecord(conn);
                            if (vbdr.type == Types.VbdType.DISK) {
                                final VDI vdi = vbdr.VDI;
                                vdi.destroy(conn);
                            }
                        }
                        vmSnapshot.destroy(conn);
                    }
                }
                if (vmState == VmPowerState.HALTED) {
                    if (vm != null) {
                        vm.destroy(conn);
                    }
                }
            } catch (final Exception e2) {
                s_logger.error("delete snapshot error due to " + e2.getMessage());
            }
        }
    }

    private VM createWorkingVM(final Connection conn, final String vmName, final String guestOSType, final String platformEmulator, final List<VolumeObjectTO> listVolumeTo) throws BadServerResponse,
    Types.VmBadPowerState, Types.SrFull,
    Types.OperationNotAllowed, XenAPIException, XmlRpcException {
        //below is redundant but keeping for consistency and code readabilty
        final String guestOsTypeName = platformEmulator;
        if (guestOsTypeName == null) {
            final String msg =
                    " Hypervisor " + this.getClass().getName() + " doesn't support guest OS type " + guestOSType + ". you can choose 'Other install media' to run it as HVM";
            s_logger.warn(msg);
            throw new CloudRuntimeException(msg);
        }
        final VM template = getVM(conn, guestOsTypeName);
        final VM vm = template.createClone(conn, vmName);
        vm.setIsATemplate(conn, false);
        final Map<VDI, VolumeObjectTO> vdiMap = new HashMap<VDI, VolumeObjectTO>();
        for (final VolumeObjectTO volume : listVolumeTo) {
            final String vdiUuid = volume.getPath();
            try {
                final VDI vdi = VDI.getByUuid(conn, vdiUuid);
                vdiMap.put(vdi, volume);
            } catch (final Types.UuidInvalid e) {
                s_logger.warn("Unable to find vdi by uuid: " + vdiUuid + ", skip it");
            }
        }
        for (final Map.Entry<VDI, VolumeObjectTO>entry : vdiMap.entrySet()) {
            final VDI vdi = entry.getKey();
            final VolumeObjectTO volumeTO = entry.getValue();
            final VBD.Record vbdr = new VBD.Record();
            vbdr.VM = vm;
            vbdr.VDI = vdi;
            if (volumeTO.getVolumeType() == Volume.Type.ROOT) {
                vbdr.bootable = true;
                vbdr.unpluggable = false;
            } else {
                vbdr.bootable = false;
                vbdr.unpluggable = true;
            }
            vbdr.userdevice = Long.toString(volumeTO.getDeviceId());
            vbdr.mode = Types.VbdMode.RW;
            vbdr.type = Types.VbdType.DISK;
            VBD.create(conn, vbdr);
        }
        return vm;
    }

    protected Answer execute(final DeleteVMSnapshotCommand cmd) {
        final String snapshotName = cmd.getTarget().getSnapshotName();
        final Connection conn = getConnection();

        try {
            final List<VDI> vdiList = new ArrayList<VDI>();
            final Set<VM> snapshots = VM.getByNameLabel(conn, snapshotName);
            if (snapshots.size() == 0) {
                s_logger.warn("VM snapshot with name " + snapshotName + " does not exist, assume it is already deleted");
                return new DeleteVMSnapshotAnswer(cmd, cmd.getVolumeTOs());
            }
            final VM snapshot = snapshots.iterator().next();
            final Set<VBD> vbds = snapshot.getVBDs(conn);
            for (final VBD vbd : vbds) {
                if (vbd.getType(conn) == Types.VbdType.DISK) {
                    final VDI vdi = vbd.getVDI(conn);
                    vdiList.add(vdi);
                }
            }
            if (cmd.getTarget().getType() == VMSnapshot.Type.DiskAndMemory) {
                vdiList.add(snapshot.getSuspendVDI(conn));
            }
            snapshot.destroy(conn);
            for (final VDI vdi : vdiList) {
                vdi.destroy(conn);
            }

            try {
                Thread.sleep(5000);
            } catch (final InterruptedException ex) {

            }
            // re-calculate used capacify for this VM snapshot
            for (final VolumeObjectTO volumeTo : cmd.getVolumeTOs()) {
                final long size = getVMSnapshotChainSize(conn, volumeTo, cmd.getVmName());
                volumeTo.setSize(size);
            }

            return new DeleteVMSnapshotAnswer(cmd, cmd.getVolumeTOs());
        } catch (final Exception e) {
            s_logger.warn("Catch Exception: " + e.getClass().toString() + " due to " + e.toString(), e);
            return new DeleteVMSnapshotAnswer(cmd, false, e.getMessage());
        }
    }

    protected Answer execute(final AttachIsoCommand cmd) {
        final Connection conn = getConnection();
        final boolean attach = cmd.isAttach();
        final String vmName = cmd.getVmName();
        final String isoURL = cmd.getIsoPath();

        String errorMsg;
        if (attach) {
            errorMsg = "Failed to attach ISO";
        } else {
            errorMsg = "Failed to detach ISO";
        }
        try {
            if (attach) {
                VBD isoVBD = null;

                // Find the VM
                final VM vm = getVM(conn, vmName);

                // Find the ISO VDI
                final VDI isoVDI = getIsoVDIByURL(conn, vmName, isoURL);

                // Find the VM's CD-ROM VBD
                final Set<VBD> vbds = vm.getVBDs(conn);
                for (final VBD vbd : vbds) {
                    final String userDevice = vbd.getUserdevice(conn);
                    final Types.VbdType type = vbd.getType(conn);

                    if (userDevice.equals("3") && type == Types.VbdType.CD) {
                        isoVBD = vbd;
                        break;
                    }
                }

                if (isoVBD == null) {
                    throw new CloudRuntimeException("Unable to find CD-ROM VBD for VM: " + vmName);
                } else {
                    // If an ISO is already inserted, eject it
                    if (isoVBD.getEmpty(conn) == false) {
                        isoVBD.eject(conn);
                    }

                    // Insert the new ISO
                    isoVBD.insert(conn, isoVDI);
                }

                return new Answer(cmd);
            } else {
                // Find the VM
                final VM vm = getVM(conn, vmName);
                final String vmUUID = vm.getUuid(conn);

                // Find the ISO VDI
                final VDI isoVDI = getIsoVDIByURL(conn, vmName, isoURL);

                final SR sr = isoVDI.getSR(conn);

                // Look up all VBDs for this VDI
                final Set<VBD> vbds = isoVDI.getVBDs(conn);

                // Iterate through VBDs, and if the VBD belongs the VM, eject
                // the ISO from it
                for (final VBD vbd : vbds) {
                    final VM vbdVM = vbd.getVM(conn);
                    final String vbdVmUUID = vbdVM.getUuid(conn);

                    if (vbdVmUUID.equals(vmUUID)) {
                        // If an ISO is already inserted, eject it
                        if (!vbd.getEmpty(conn)) {
                            vbd.eject(conn);
                        }

                        break;
                    }
                }

                if (!sr.getNameLabel(conn).startsWith("XenServer Tools")) {
                    removeSR(conn, sr);
                }

                return new Answer(cmd);
            }
        } catch (final XenAPIException e) {
            s_logger.warn(errorMsg + ": " + e.toString(), e);
            return new Answer(cmd, false, e.toString());
        } catch (final Exception e) {
            s_logger.warn(errorMsg + ": " + e.toString(), e);
            return new Answer(cmd, false, e.getMessage());
        }
    }

    public boolean IsISCSI(final String type) {
        return SRType.LVMOHBA.equals(type) || SRType.LVMOISCSI.equals(type) || SRType.LVM.equals(type);
    }

    protected Answer execute(final UpgradeSnapshotCommand cmd) {

        final String secondaryStorageUrl = cmd.getSecondaryStorageUrl();
        final String backedUpSnapshotUuid = cmd.getSnapshotUuid();
        final Long volumeId = cmd.getVolumeId();
        final Long accountId = cmd.getAccountId();
        final Long templateId = cmd.getTemplateId();
        final Long tmpltAcountId = cmd.getTmpltAccountId();
        final String version = cmd.getVersion();

        if (!version.equals("2.1")) {
            return new Answer(cmd, true, "success");
        }
        try {
            final Connection conn = getConnection();
            final URI uri = new URI(secondaryStorageUrl);
            final String secondaryStorageMountPath = uri.getHost() + ":" + uri.getPath();
            final String snapshotPath = secondaryStorageMountPath + "/snapshots/" + accountId + "/" + volumeId + "/" + backedUpSnapshotUuid + ".vhd";
            final String templatePath = secondaryStorageMountPath + "/template/tmpl/" + tmpltAcountId + "/" + templateId;
            upgradeSnapshot(conn, templatePath, snapshotPath);
            return new Answer(cmd, true, "success");
        } catch (final Exception e) {
            final String details = "upgrading snapshot " + backedUpSnapshotUuid + " failed due to " + e.toString();
            s_logger.error(details, e);

        }
        return new Answer(cmd, false, "failure");
    }

    private boolean destroySnapshotOnPrimaryStorageExceptThis(final Connection conn, final String volumeUuid, final String avoidSnapshotUuid) {
        try {
            final VDI volume = getVDIbyUuid(conn, volumeUuid);
            if (volume == null) {
                throw new InternalErrorException("Could not destroy snapshot on volume " + volumeUuid + " due to can not find it");
            }
            final Set<VDI> snapshots = volume.getSnapshots(conn);
            for (final VDI snapshot : snapshots) {
                try {
                    if (!snapshot.getUuid(conn).equals(avoidSnapshotUuid)) {
                        snapshot.destroy(conn);
                    }
                } catch (final Exception e) {
                    final String msg = "Destroying snapshot: " + snapshot + " on primary storage failed due to " + e.toString();
                    s_logger.warn(msg, e);
                }
            }
            s_logger.debug("Successfully destroyed snapshot on volume: " + volumeUuid + " execept this current snapshot " + avoidSnapshotUuid);
            return true;
        } catch (final XenAPIException e) {
            final String msg = "Destroying snapshot on volume: " + volumeUuid + " execept this current snapshot " + avoidSnapshotUuid + " failed due to " + e.toString();
            s_logger.error(msg, e);
        } catch (final Exception e) {
            final String msg = "Destroying snapshot on volume: " + volumeUuid + " execept this current snapshot " + avoidSnapshotUuid + " failed due to " + e.toString();
            s_logger.warn(msg, e);
        }

        return false;
    }

    public VM getVM(final Connection conn, final String vmName) {
        // Look up VMs with the specified name
        Set<VM> vms;
        try {
            vms = VM.getByNameLabel(conn, vmName);
        } catch (final XenAPIException e) {
            throw new CloudRuntimeException("Unable to get " + vmName + ": " + e.toString(), e);
        } catch (final Exception e) {
            throw new CloudRuntimeException("Unable to get " + vmName + ": " + e.getMessage(), e);
        }

        // If there are no VMs, throw an exception
        if (vms.size() == 0) {
            throw new CloudRuntimeException("VM with name: " + vmName + " does not exist.");
        }

        // If there is more than one VM, print a warning
        if (vms.size() > 1) {
            s_logger.warn("Found " + vms.size() + " VMs with name: " + vmName);
        }

        // Return the first VM in the set
        return vms.iterator().next();
    }

    public VDI getIsoVDIByURL(final Connection conn, final String vmName, final String isoURL) {
        SR isoSR = null;
        String mountpoint = null;
        if (isoURL.startsWith("xs-tools")) {
            try {
                final Set<VDI> vdis = VDI.getByNameLabel(conn, isoURL);
                if (vdis.isEmpty()) {
                    throw new CloudRuntimeException("Could not find ISO with URL: " + isoURL);
                }
                return vdis.iterator().next();

            } catch (final XenAPIException e) {
                throw new CloudRuntimeException("Unable to get pv iso: " + isoURL + " due to " + e.toString());
            } catch (final Exception e) {
                throw new CloudRuntimeException("Unable to get pv iso: " + isoURL + " due to " + e.toString());
            }
        }

        final int index = isoURL.lastIndexOf("/");
        mountpoint = isoURL.substring(0, index);

        URI uri;
        try {
            uri = new URI(mountpoint);
        } catch (final URISyntaxException e) {
            throw new CloudRuntimeException("isoURL is wrong: " + isoURL);
        }
        isoSR = getISOSRbyVmName(conn, vmName);
        if (isoSR == null) {
            isoSR = createIsoSRbyURI(conn, uri, vmName, false);
        }

        final String isoName = isoURL.substring(index + 1);

        final VDI isoVDI = getVDIbyLocationandSR(conn, isoName, isoSR);

        if (isoVDI != null) {
            return isoVDI;
        } else {
            throw new CloudRuntimeException("Could not find ISO with URL: " + isoURL);
        }
    }

    public SR getStorageRepository(final Connection conn, final String srNameLabel) {
        Set<SR> srs;
        try {
            srs = SR.getByNameLabel(conn, srNameLabel);
        } catch (final XenAPIException e) {
            throw new CloudRuntimeException("Unable to get SR " + srNameLabel + " due to " + e.toString(), e);
        } catch (final Exception e) {
            throw new CloudRuntimeException("Unable to get SR " + srNameLabel + " due to " + e.getMessage(), e);
        }

        if (srs.size() > 1) {
            throw new CloudRuntimeException("More than one storage repository was found for pool with uuid: " + srNameLabel);
        } else if (srs.size() == 1) {
            final SR sr = srs.iterator().next();
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("SR retrieved for " + srNameLabel);
            }

            if (checkSR(conn, sr)) {
                return sr;
            }
            throw new CloudRuntimeException("SR check failed for storage pool: " + srNameLabel + "on host:" + _host.getUuid());
        } else {
            throw new CloudRuntimeException("Can not see storage pool: " + srNameLabel + " from on host:" + _host.getUuid());
        }
    }

    protected Answer execute(final CheckConsoleProxyLoadCommand cmd) {
        return executeProxyLoadScan(cmd, cmd.getProxyVmId(), cmd.getProxyVmName(), cmd.getProxyManagementIp(), cmd.getProxyCmdPort());
    }

    protected Answer execute(final WatchConsoleProxyLoadCommand cmd) {
        return executeProxyLoadScan(cmd, cmd.getProxyVmId(), cmd.getProxyVmName(), cmd.getProxyManagementIp(), cmd.getProxyCmdPort());
    }

    protected Answer executeProxyLoadScan(final Command cmd, final long proxyVmId, final String proxyVmName, final String proxyManagementIp, final int cmdPort) {
        String result = null;

        final StringBuffer sb = new StringBuffer();
        sb.append("http://").append(proxyManagementIp).append(":" + cmdPort).append("/cmd/getstatus");

        boolean success = true;
        try {
            final URL url = new URL(sb.toString());
            final URLConnection conn = url.openConnection();

            // setting TIMEOUTs to avoid possible waiting until death situations
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

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

    protected boolean createSecondaryStorageFolder(final Connection conn, final String remoteMountPath, final String newFolder) {
        final String result = callHostPlugin(conn, "vmopsSnapshot", "create_secondary_storage_folder", "remoteMountPath", remoteMountPath, "newFolder", newFolder);
        return result != null;
    }

    protected boolean deleteSecondaryStorageFolder(final Connection conn, final String remoteMountPath, final String folder) {
        final String details = callHostPlugin(conn, "vmopsSnapshot", "delete_secondary_storage_folder", "remoteMountPath", remoteMountPath, "folder", folder);
        return details != null && details.equals("1");
    }

    protected boolean postCreatePrivateTemplate(final Connection conn, final String templatePath, final String tmpltFilename, final String templateName, String templateDescription,
            String checksum, final long size, final long virtualSize, final long templateId) {

        if (templateDescription == null) {
            templateDescription = "";
        }

        if (checksum == null) {
            checksum = "";
        }

        final String result =
                callHostPlugin(conn, "vmopsSnapshot", "post_create_private_template", "templatePath", templatePath, "templateFilename", tmpltFilename, "templateName",
                        templateName, "templateDescription", templateDescription, "checksum", checksum, "size", String.valueOf(size), "virtualSize", String.valueOf(virtualSize),
                        "templateId", String.valueOf(templateId));

        boolean success = false;
        if (result != null && !result.isEmpty()) {
            // Else, command threw an exception which has already been logged.

            if (result.equalsIgnoreCase("1")) {
                s_logger.debug("Successfully created template.properties file on secondary storage for " + tmpltFilename);
                success = true;
            } else {
                s_logger.warn("Could not create template.properties file on secondary storage for " + tmpltFilename + " for templateId: " + templateId);
            }
        }

        return success;
    }

    public String getVhdParent(final Connection conn, final String primaryStorageSRUuid, final String snapshotUuid, final Boolean isISCSI) {
        final String parentUuid =
                callHostPlugin(conn, "vmopsSnapshot", "getVhdParent", "primaryStorageSRUuid", primaryStorageSRUuid, "snapshotUuid", snapshotUuid, "isISCSI",
                        isISCSI.toString());

        if (parentUuid == null || parentUuid.isEmpty() || parentUuid.equalsIgnoreCase("None")) {
            s_logger.debug("Unable to get parent of VHD " + snapshotUuid + " in SR " + primaryStorageSRUuid);
            // errString is already logged.
            return null;
        }
        return parentUuid;
    }

    protected String deleteSnapshotBackup(final Connection conn, final Long dcId, final Long accountId, final Long volumeId, final String secondaryStorageMountPath, final String backupUUID) {

        // If anybody modifies the formatting below again, I'll skin them
        final String result =
                callHostPlugin(conn, "vmopsSnapshot", "deleteSnapshotBackup", "backupUUID", backupUUID, "dcId", dcId.toString(), "accountId", accountId.toString(),
                        "volumeId", volumeId.toString(), "secondaryStorageMountPath", secondaryStorageMountPath);

        return result;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        disconnected();
        return true;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public IAgentControl getAgentControl() {
        return _agentControl;
    }

    @Override
    public void setAgentControl(final IAgentControl agentControl) {
        _agentControl = agentControl;
    }

    private Answer execute(final CleanupNetworkRulesCmd cmd) {
        if (!_canBridgeFirewall) {
            return new Answer(cmd, true, null);
        }
        final Connection conn = getConnection();
        final String result = callHostPlugin(conn, "vmops", "cleanup_rules", "instance", _instance);
        final int numCleaned = Integer.parseInt(result);
        if (result == null || result.isEmpty() || numCleaned < 0) {
            s_logger.warn("Failed to cleanup rules for host " + _host.getIp());
            return new Answer(cmd, false, result);
        }
        if (numCleaned > 0) {
            s_logger.info("Cleaned up rules for " + result + " vms on host " + _host.getIp());
        }
        return new Answer(cmd, true, result);
    }

    protected String getGuestOsType(final String stdType, String platformEmulator, final boolean bootFromCD) {
        if (platformEmulator == null) {
            s_logger.debug("no guest OS type, start it as HVM guest");
            platformEmulator = "Other install media";
        }
        return platformEmulator;
    }

    private Answer execute(final NetworkRulesSystemVmCommand cmd) {
        boolean success = true;
        final Connection conn = getConnection();
        if (cmd.getType() != VirtualMachine.Type.User) {
            final String result = callHostPlugin(conn, "vmops", "default_network_rules_systemvm", "vmName", cmd.getVmName());
            if (result == null || result.isEmpty() || !Boolean.parseBoolean(result)) {
                success = false;
            }
        }

        return new Answer(cmd, success, "");
    }

    private Answer execute(final NetworkRulesVmSecondaryIpCommand cmd) {
        boolean success = true;
        final Connection conn = getConnection();

        final String result =
                callHostPlugin(conn, "vmops", "network_rules_vmSecondaryIp", "vmName", cmd.getVmName(), "vmMac", cmd.getVmMac(), "vmSecIp", cmd.getVmSecIp(), "action",
                        cmd.getAction());
        if (result == null || result.isEmpty() || !Boolean.parseBoolean(result)) {
            success = false;
        }

        return new Answer(cmd, success, "");
    }

    protected ClusterVMMetaDataSyncAnswer execute(final ClusterVMMetaDataSyncCommand cmd) {
        final Connection conn = getConnection();
        //check if this is master
        Pool pool;
        try {
            pool = Pool.getByUuid(conn, _host.getPool());
            final Pool.Record poolr = pool.getRecord(conn);
            final Host.Record hostr = poolr.master.getRecord(conn);
            if (!_host.getUuid().equals(hostr.uuid)) {
                return new ClusterVMMetaDataSyncAnswer(cmd.getClusterId(), null);
            }
        } catch (final Throwable e) {
            s_logger.warn("Check for master failed, failing the Cluster sync VMMetaData command");
            return new ClusterVMMetaDataSyncAnswer(cmd.getClusterId(), null);
        }
        final HashMap<String, String> vmMetadatum = clusterVMMetaDataSync(conn);
        return new ClusterVMMetaDataSyncAnswer(cmd.getClusterId(), vmMetadatum);
    }

    protected HashMap<String, String> clusterVMMetaDataSync(final Connection conn) {
        final HashMap<String, String> vmMetaDatum = new HashMap<String, String>();
        try {
            final Map<VM, VM.Record>  vm_map = VM.getAllRecords(conn);  //USE THIS TO GET ALL VMS FROM  A CLUSTER
            if(vm_map != null) {
                for (final VM.Record record : vm_map.values()) {
                    if (record.isControlDomain || record.isASnapshot || record.isATemplate) {
                        continue; // Skip DOM0
                    }
                    vmMetaDatum.put(record.nameLabel, StringUtils.mapToString(record.platform));
                }
            }
        } catch (final Throwable e) {
            final String msg = "Unable to get vms through host " + _host.getUuid() + " due to to " + e.toString();
            s_logger.warn(msg, e);
            throw new CloudRuntimeException(msg);
        }
        return vmMetaDatum;
    }

    /**
     * @param cmd
     * @return
     */
    private UnPlugNicAnswer execute(final UnPlugNicCommand cmd) {
        final Connection conn = getConnection();
        final String vmName = cmd.getVmName();
        try {
            final Set<VM> vms = VM.getByNameLabel(conn, vmName);
            if (vms == null || vms.isEmpty()) {
                return new UnPlugNicAnswer(cmd, false, "Can not find VM " + vmName);
            }
            final VM vm = vms.iterator().next();
            final NicTO nic = cmd.getNic();
            final String mac = nic.getMac();
            final VIF vif = getVifByMac(conn, vm, mac);
            if (vif != null) {
                vif.unplug(conn);
                final Network network = vif.getNetwork(conn);
                vif.destroy(conn);
                try {
                    if (network.getNameLabel(conn).startsWith("VLAN")) {
                        disableVlanNetwork(conn, network);
                    }
                } catch (final Exception e) {
                }
            }
            return new UnPlugNicAnswer(cmd, true, "success");
        } catch (final Exception e) {
            final String msg = " UnPlug Nic failed due to " + e.toString();
            s_logger.warn(msg, e);
            return new UnPlugNicAnswer(cmd, false, msg);
        }
    }

    /**
     * @param cmd
     * @return
     */
    private PlugNicAnswer execute(final PlugNicCommand cmd) {
        final Connection conn = getConnection();
        final String vmName = cmd.getVmName();
        try {
            final Set<VM> vms = VM.getByNameLabel(conn, vmName);
            if (vms == null || vms.isEmpty()) {
                return new PlugNicAnswer(cmd, false, "Can not find VM " + vmName);
            }
            final VM vm = vms.iterator().next();
            final NicTO nic = cmd.getNic();

            String mac = nic.getMac();
            final Set<VIF> routerVIFs = vm.getVIFs(conn);
            mac = mac.trim();

            int counter = 0;
            for (final VIF vif : routerVIFs) {
                final String lmac = vif.getMAC(conn);
                if (lmac.trim().equals(mac)) {
                    counter++;
                }
            }
            // We allow 2 routers with the same mac. It's needed for the redundant vpc routers.
            // [FIXME] Find a way to identify the type of the router or if it's redundant.
            if (counter > 2) {
                final String msg = " Plug Nic failed due to a VIF with the same mac " + nic.getMac() + " exists in more than 2 routers.";
                s_logger.error(msg);
                return new PlugNicAnswer(cmd, false, msg);
            }

            // Wilder Rodrigues - replaced this code with the code above.
            //            VIF vif = getVifByMac(conn, vm, nic.getMac());
            //            if (vif != null) {
            //                final String msg = " Plug Nic failed due to a VIF with the same mac " + nic.getMac() + " exists";
            //                s_logger.warn(msg);
            //                return new PlugNicAnswer(cmd, false, msg);
            //            }

            final String deviceId = getLowestAvailableVIFDeviceNum(conn, vm);
            nic.setDeviceId(Integer.parseInt(deviceId));
            final VIF vif = createVif(conn, vmName, vm, null, nic);
            //            vif = createVif(conn, vmName, vm, null, nic);
            vif.plug(conn);
            return new PlugNicAnswer(cmd, true, "success");
        } catch (final Exception e) {
            final String msg = " Plug Nic failed due to " + e.toString();
            s_logger.error(msg, e);
            return new PlugNicAnswer(cmd, false, msg);
        }
    }

    /**
     * @param cmd
     * @return
     */
    private ExecutionResult prepareNetworkElementCommand(final SetupGuestNetworkCommand cmd) {
        final Connection conn = getConnection();
        final NicTO nic = cmd.getNic();
        final String domrName = cmd.getAccessDetail(NetworkElementCommand.ROUTER_NAME);
        try {
            final Set<VM> vms = VM.getByNameLabel(conn, domrName);
            if (vms == null || vms.isEmpty()) {
                return new ExecutionResult(false, "Can not find VM " + domrName);
            }
            final VM vm = vms.iterator().next();
            final String mac = nic.getMac();
            VIF domrVif = null;
            for (final VIF vif : vm.getVIFs(conn)) {
                final String lmac = vif.getMAC(conn);
                if (lmac.equals(mac)) {
                    domrVif = vif;
                    //Do not break it! We have 2 routers.
                    //break;
                }
            }
            if (domrVif == null) {
                return new ExecutionResult(false, "Can not find vif with mac " + mac + " for VM " + domrName);
            }

            nic.setDeviceId(Integer.valueOf(domrVif.getDevice(conn)));
        } catch (final Exception e) {
            final String msg = "Creating guest network failed due to " + e.toString();
            s_logger.warn(msg, e);
            return new ExecutionResult(false, msg);
        }
        return new ExecutionResult(true, null);
    }

    protected ExecutionResult prepareNetworkElementCommand(final IpAssocVpcCommand cmd) {
        final Connection conn = getConnection();
        final String routerName = cmd.getAccessDetail(NetworkElementCommand.ROUTER_NAME);
        try {
            final IpAddressTO[] ips = cmd.getIpAddresses();
            for (final IpAddressTO ip : ips) {

                final VM router = getVM(conn, routerName);

                final VIF correctVif = getVifByMac(conn, router, ip.getVifMacAddress());
                setNicDevIdIfCorrectVifIsNotNull(conn, ip, correctVif);
            }
        } catch (final Exception e) {
            s_logger.error("Ip Assoc failure on applying one ip due to exception:  ", e);
            return new ExecutionResult(false, e.getMessage());
        }

        return new ExecutionResult(true, null);
    }

    protected void setNicDevIdIfCorrectVifIsNotNull(final Connection conn, final IpAddressTO ip, final VIF correctVif) throws InternalErrorException, BadServerResponse, XenAPIException,
    XmlRpcException {
        if (correctVif == null) {
            if (ip.isAdd()) {
                throw new InternalErrorException("Failed to find DomR VIF to associate IP with.");
            } else {
                s_logger.debug("VIF to deassociate IP with does not exist, return success");
            }
        } else {
            ip.setNicDevId(Integer.valueOf(correctVif.getDevice(conn)));
        }
    }

    protected ExecutionResult prepareNetworkElementCommand(final SetSourceNatCommand cmd) {
        final Connection conn = getConnection();
        final String routerName = cmd.getAccessDetail(NetworkElementCommand.ROUTER_NAME);
        final IpAddressTO pubIp = cmd.getIpAddress();
        try {
            final VM router = getVM(conn, routerName);

            final VIF correctVif = getCorrectVif(conn, router, pubIp);

            pubIp.setNicDevId(Integer.valueOf(correctVif.getDevice(conn)));

        } catch (final Exception e) {
            final String msg = "Ip SNAT failure due to " + e.toString();
            s_logger.error(msg, e);
            return new ExecutionResult(false, msg);
        }
        return new ExecutionResult(true, null);
    }

    protected ExecutionResult prepareNetworkElementCommand(final SetNetworkACLCommand cmd) {
        final Connection conn = getConnection();
        final String routerName = cmd.getAccessDetail(NetworkElementCommand.ROUTER_NAME);

        try {
            final VM router = getVM(conn, routerName);

            final NicTO nic = cmd.getNic();
            if(nic != null) {
                final VIF vif = getVifByMac(conn, router, nic.getMac());
                if(vif == null) {
                    final String msg = "Prepare SetNetworkACL failed due to VIF is null for : " + nic.getMac() +" with routername: " + routerName;
                    s_logger.error(msg);
                    return new ExecutionResult(false, msg);
                }
                nic.setDeviceId(Integer.valueOf(vif.getDevice(conn)));
            } else {
                final String msg = "Prepare SetNetworkACL failed due to nic is null for : " + routerName;
                s_logger.error(msg);
                return new ExecutionResult(false, msg);
            }
        } catch (final Exception e) {
            final String msg = "Prepare SetNetworkACL failed due to " + e.toString();
            s_logger.error(msg, e);
            return new ExecutionResult(false, msg);
        }
        return new ExecutionResult(true, null);
    }

    @Override
    public void setName(final String name) {
    }

    @Override
    public void setConfigParams(final Map<String, Object> params) {
    }

    @Override
    public Map<String, Object> getConfigParams() {
        return null;
    }

    @Override
    public int getRunLevel() {
        return 0;
    }

    @Override
    public void setRunLevel(final int level) {
    }

    public boolean isXcp() {
        final Connection conn = getConnection();
        final String result = callHostPlugin(conn, "ovstunnel", "is_xcp");
        if (result.equals("XCP")) {
            return true;
        }
        return false;
    }

    public String getLabel() {
        final Connection conn = getConnection();
        final String result = callHostPlugin(conn, "ovstunnel", "getLabel");
        return result;
    }
}
