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
package com.cloud.hypervisor.xen.resource;


import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import javax.ejb.Local;
import javax.naming.ConfigurationException;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.cloudstack.storage.command.StorageSubSystemCommand;
import com.cloud.agent.api.to.*;
import com.cloud.network.rules.FirewallRule;
import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.cloud.agent.IAgentControl;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.AttachIsoCommand;
import com.cloud.agent.api.AttachVolumeAnswer;
import com.cloud.agent.api.AttachVolumeCommand;
import com.cloud.agent.api.BackupSnapshotAnswer;
import com.cloud.agent.api.BackupSnapshotCommand;
import com.cloud.agent.api.BumpUpPriorityCommand;
import com.cloud.agent.api.CheckHealthAnswer;
import com.cloud.agent.api.CheckHealthCommand;
import com.cloud.agent.api.CheckNetworkAnswer;
import com.cloud.agent.api.CheckNetworkCommand;
import com.cloud.agent.api.CheckOnHostAnswer;
import com.cloud.agent.api.CheckOnHostCommand;
import com.cloud.agent.api.CheckRouterAnswer;
import com.cloud.agent.api.CheckRouterCommand;
import com.cloud.agent.api.CheckS2SVpnConnectionsAnswer;
import com.cloud.agent.api.CheckS2SVpnConnectionsCommand;
import com.cloud.agent.api.CheckVirtualMachineAnswer;
import com.cloud.agent.api.CheckVirtualMachineCommand;
import com.cloud.agent.api.CleanupNetworkRulesCmd;
import com.cloud.agent.api.ClusterSyncAnswer;
import com.cloud.agent.api.ClusterSyncCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.CreatePrivateTemplateFromSnapshotCommand;
import com.cloud.agent.api.CreatePrivateTemplateFromVolumeCommand;
import com.cloud.agent.api.CreateStoragePoolCommand;
import com.cloud.agent.api.CreateVMSnapshotAnswer;
import com.cloud.agent.api.CreateVMSnapshotCommand;
import com.cloud.agent.api.CreateVolumeFromSnapshotAnswer;
import com.cloud.agent.api.CreateVolumeFromSnapshotCommand;
import com.cloud.agent.api.DeleteStoragePoolCommand;
import com.cloud.agent.api.DeleteVMSnapshotAnswer;
import com.cloud.agent.api.DeleteVMSnapshotCommand;
import com.cloud.agent.api.GetDomRVersionAnswer;
import com.cloud.agent.api.GetDomRVersionCmd;
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
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.PingRoutingCommand;
import com.cloud.agent.api.PingRoutingWithNwGroupsCommand;
import com.cloud.agent.api.PingRoutingWithOvsCommand;
import com.cloud.agent.api.PingTestCommand;
import com.cloud.agent.api.PlugNicAnswer;
import com.cloud.agent.api.PlugNicCommand;
import com.cloud.agent.api.PoolEjectCommand;
import com.cloud.agent.api.PrepareForMigrationAnswer;
import com.cloud.agent.api.PrepareForMigrationCommand;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.RebootAnswer;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.RebootRouterCommand;
import com.cloud.agent.api.RevertToVMSnapshotAnswer;
import com.cloud.agent.api.RevertToVMSnapshotCommand;
import com.cloud.agent.api.SecurityGroupRuleAnswer;
import com.cloud.agent.api.SecurityGroupRulesCmd;
import com.cloud.agent.api.SetupAnswer;
import com.cloud.agent.api.SetupCommand;
import com.cloud.agent.api.SetupGuestNetworkAnswer;
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
import com.cloud.agent.api.VmStatsEntry;
import com.cloud.agent.api.check.CheckSshAnswer;
import com.cloud.agent.api.check.CheckSshCommand;
import com.cloud.agent.api.proxy.CheckConsoleProxyLoadCommand;
import com.cloud.agent.api.proxy.ConsoleProxyLoadAnswer;
import com.cloud.agent.api.proxy.WatchConsoleProxyLoadCommand;
import com.cloud.agent.api.routing.DhcpEntryCommand;
import com.cloud.agent.api.routing.IpAssocAnswer;
import com.cloud.agent.api.routing.IpAssocCommand;
import com.cloud.agent.api.routing.IpAssocVpcCommand;
import com.cloud.agent.api.routing.LoadBalancerConfigCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.routing.RemoteAccessVpnCfgCommand;
import com.cloud.agent.api.routing.SavePasswordCommand;
import com.cloud.agent.api.routing.SetFirewallRulesAnswer;
import com.cloud.agent.api.routing.SetFirewallRulesCommand;
import com.cloud.agent.api.routing.SetNetworkACLAnswer;
import com.cloud.agent.api.routing.SetNetworkACLCommand;
import com.cloud.agent.api.routing.SetPortForwardingRulesAnswer;
import com.cloud.agent.api.routing.SetPortForwardingRulesCommand;
import com.cloud.agent.api.routing.SetPortForwardingRulesVpcCommand;
import com.cloud.agent.api.routing.SetSourceNatAnswer;
import com.cloud.agent.api.routing.SetSourceNatCommand;
import com.cloud.agent.api.routing.SetStaticNatRulesAnswer;
import com.cloud.agent.api.routing.SetStaticNatRulesCommand;
import com.cloud.agent.api.routing.SetStaticRouteAnswer;
import com.cloud.agent.api.routing.SetStaticRouteCommand;
import com.cloud.agent.api.routing.Site2SiteVpnCfgCommand;
import com.cloud.agent.api.routing.VmDataCommand;
import com.cloud.agent.api.routing.VpnUsersCfgCommand;
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
import com.cloud.agent.api.to.IpAddressTO;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.PortForwardingRuleTO;
import com.cloud.agent.api.to.S3TO;
import com.cloud.agent.api.to.StaticNatRuleTO;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.agent.api.to.SwiftTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.exception.InternalErrorException;
import com.cloud.host.Host.Type;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.HAProxyConfigurator;
import com.cloud.network.LoadBalancerConfigurator;
import com.cloud.network.Networks;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.IsolationType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetworkSetupInfo;
import com.cloud.network.ovs.OvsCreateGreTunnelAnswer;
import com.cloud.network.ovs.OvsCreateGreTunnelCommand;
import com.cloud.network.ovs.OvsCreateTunnelAnswer;
import com.cloud.network.ovs.OvsCreateTunnelCommand;
import com.cloud.network.ovs.OvsDeleteFlowCommand;
import com.cloud.network.ovs.OvsDestroyBridgeCommand;
import com.cloud.network.ovs.OvsDestroyTunnelCommand;
import com.cloud.network.ovs.OvsFetchInterfaceAnswer;
import com.cloud.network.ovs.OvsFetchInterfaceCommand;
import com.cloud.network.ovs.OvsSetTagAndFlowAnswer;
import com.cloud.network.ovs.OvsSetTagAndFlowCommand;
import com.cloud.network.ovs.OvsSetupBridgeCommand;
import com.cloud.resource.ServerResource;
import com.cloud.resource.hypervisor.HypervisorResource;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.template.TemplateInfo;
import com.cloud.template.VirtualMachineTemplate.BootloaderType;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.S3Utils;
import com.cloud.utils.StringUtils;
import com.cloud.utils.Ternary;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;
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
import com.xensource.xenapi.PIF.Record;
import com.xensource.xenapi.Pool;
import com.xensource.xenapi.SR;
import com.xensource.xenapi.Session;
import com.xensource.xenapi.Task;
import com.xensource.xenapi.Types;
import com.xensource.xenapi.Types.BadServerResponse;
import com.xensource.xenapi.Types.ConsoleProtocol;
import com.xensource.xenapi.Types.IpConfigurationMode;
import com.xensource.xenapi.Types.OperationNotAllowed;
import com.xensource.xenapi.Types.SrFull;
import com.xensource.xenapi.Types.VbdType;
import com.xensource.xenapi.Types.VmBadPowerState;
import com.xensource.xenapi.Types.VmPowerState;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.VBD;
import com.xensource.xenapi.VDI;
import com.xensource.xenapi.VIF;
import com.xensource.xenapi.VLAN;
import com.xensource.xenapi.VM;
import com.xensource.xenapi.VMGuestMetrics;
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
public abstract class CitrixResourceBase implements ServerResource, HypervisorResource {
    private static final Logger s_logger = Logger.getLogger(CitrixResourceBase.class);
    protected static final XenServerConnectionPool _connPool = XenServerConnectionPool.getInstance();
    protected String _name;
    protected String _username;
    protected Queue<String> _password=new LinkedList<String>();
    protected final int _retry = 100;
    protected final int _sleep = 10000;
    protected long _dcId;
    protected String _pod;
    protected String _cluster;
    protected static final XenServerPoolVms s_vms = new XenServerPoolVms();
    protected String _privateNetworkName;
    protected String _linkLocalPrivateNetworkName;
    protected String _publicNetworkName;
    protected String _storageNetworkName1;
    protected String _storageNetworkName2;
    protected String _guestNetworkName;
    protected int _wait;
    protected int _migratewait;
    protected String _instance; //instance name (default is usually "VM")
    static final Random _rand = new Random(System.currentTimeMillis());

    protected IAgentControl _agentControl;

    final int _maxWeight = 256;
    protected int _heartbeatInterval = 60;
    protected final XsHost _host = new XsHost();

    // Guest and Host Performance Statistics
    protected String _consolidationFunction = "AVERAGE";
    protected int _pollingIntervalInSeconds = 60;

    //Hypervisor specific params with generic value, may need to be overridden for specific versions
    long _xs_memory_used = 128 * 1024 * 1024L; // xen hypervisor used 128 M
    double _xs_virtualization_factor = 63.0/64.0;  // 1 - virtualization overhead

    protected boolean _canBridgeFirewall = false;
    protected boolean _isOvs = false;
    protected List<VIF> _tmpDom0Vif = new ArrayList<VIF>();
    protected XenServerStorageResource storageResource;
    protected int _maxNics = 7;

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

        public boolean equals(String type) {
            return _str.equalsIgnoreCase(type);
        }
    }

    protected static HashMap<Types.VmPowerState, State> s_statesTable;
    static {
        s_statesTable = new HashMap<Types.VmPowerState, State>();
        s_statesTable.put(Types.VmPowerState.HALTED, State.Stopped);
        s_statesTable.put(Types.VmPowerState.PAUSED, State.Running);
        s_statesTable.put(Types.VmPowerState.RUNNING, State.Running);
        s_statesTable.put(Types.VmPowerState.SUSPENDED, State.Running);
        s_statesTable.put(Types.VmPowerState.UNRECOGNIZED, State.Unknown);
    }

    public XsHost getHost() {
        return this._host;
    }

    protected boolean cleanupHaltedVms(Connection conn) throws XenAPIException, XmlRpcException {
        Host host = Host.getByUuid(conn, _host.uuid);
        Map<VM, VM.Record> vms = VM.getAllRecords(conn);
        boolean success = true;
        for (Map.Entry<VM, VM.Record> entry : vms.entrySet()) {
            VM vm = entry.getKey();
            VM.Record vmRec = entry.getValue();
            if ( vmRec.isATemplate || vmRec.isControlDomain ) {
                continue;
            }

            if (VmPowerState.HALTED.equals(vmRec.powerState) && vmRec.affinity.equals(host)) {
                try {
                    vm.destroy(conn);
                } catch (Exception e) {
                    s_logger.warn("Catch Exception " + e.getClass().getName() + ": unable to destroy VM " + vmRec.nameLabel + " due to ", e);
                    success = false;
                }
            }
        }
        return success;
    }

    protected boolean isRefNull(XenAPIObject object) {
        return (object == null || object.toWireString().equals("OpaqueRef:NULL") || object.toWireString().equals("<not in database>"));
    }

    @Override
    public void disconnected() {
    }

    protected boolean pingdomr(Connection conn, String host, String port) {
        String status;
        status = callHostPlugin(conn, "vmops", "pingdomr", "host", host, "port", port);

        if (status == null || status.isEmpty()) {
            return false;
        }

        return true;

    }

    protected boolean pingXenServer() {
        Session slaveSession = null;
        Connection slaveConn = null;
        try {
            URL slaveUrl = null;
            slaveUrl = _connPool.getURL(_host.ip);
            slaveConn = new Connection(slaveUrl, 10);
            slaveSession = _connPool.slaveLocalLoginWithPassword(slaveConn, _username, _password);
            return true;
        } catch (Exception e) {
        } finally {
            if( slaveSession != null ){
                try{
                    Session.localLogout(slaveConn);
                } catch (Exception e) {
                }
                slaveConn.dispose();
            }
        }
        return false;
    }

    protected String logX(XenAPIObject obj, String msg) {
        return new StringBuilder("Host ").append(_host.ip).append(" ").append(obj.toWireString()).append(": ").append(msg).toString();
    }


    @Override
    public Answer executeRequest(Command cmd) {
        Class<? extends Command> clazz = cmd.getClass();
        if (clazz == CreateCommand.class) {
            return execute((CreateCommand) cmd);
        } else if (clazz == SetPortForwardingRulesCommand.class) {
            return execute((SetPortForwardingRulesCommand) cmd);
        } else if (clazz == SetStaticNatRulesCommand.class) {
            return execute((SetStaticNatRulesCommand) cmd);
        }  else if (clazz == LoadBalancerConfigCommand.class) {
            return execute((LoadBalancerConfigCommand) cmd);
        } else if (clazz == IpAssocCommand.class) {
            return execute((IpAssocCommand) cmd);
        } else if (clazz == CheckConsoleProxyLoadCommand.class) {
            return execute((CheckConsoleProxyLoadCommand) cmd);
        } else if (clazz == WatchConsoleProxyLoadCommand.class) {
            return execute((WatchConsoleProxyLoadCommand) cmd);
        } else if (clazz == SavePasswordCommand.class) {
            return execute((SavePasswordCommand) cmd);
        } else if (clazz == DhcpEntryCommand.class) {
            return execute((DhcpEntryCommand) cmd);
        } else if (clazz == VmDataCommand.class) {
            return execute((VmDataCommand) cmd);
        } else if (clazz == ReadyCommand.class) {
            return execute((ReadyCommand) cmd);
        } else if (clazz == GetHostStatsCommand.class) {
            return execute((GetHostStatsCommand) cmd);
        } else if (clazz == GetVmStatsCommand.class) {
            return execute((GetVmStatsCommand) cmd);
        } else if (clazz == CheckHealthCommand.class) {
            return execute((CheckHealthCommand) cmd);
        } else if (clazz == StopCommand.class) {
            return execute((StopCommand) cmd);
        } else if (clazz == RebootRouterCommand.class) {
            return execute((RebootRouterCommand) cmd);
        } else if (clazz == RebootCommand.class) {
            return execute((RebootCommand) cmd);
        } else if (clazz == CheckVirtualMachineCommand.class) {
            return execute((CheckVirtualMachineCommand) cmd);
        } else if (clazz == PrepareForMigrationCommand.class) {
            return execute((PrepareForMigrationCommand) cmd);
        } else if (clazz == MigrateCommand.class) {
            return execute((MigrateCommand) cmd);
        } else if (clazz == DestroyCommand.class) {
            return execute((DestroyCommand) cmd);
        } else if (clazz == CreateStoragePoolCommand.class) {
            return execute((CreateStoragePoolCommand) cmd);
        } else if (clazz == ModifyStoragePoolCommand.class) {
            return execute((ModifyStoragePoolCommand) cmd);
        } else if (clazz == DeleteStoragePoolCommand.class) {
            return execute((DeleteStoragePoolCommand) cmd);
        } else if (clazz == CopyVolumeCommand.class) {
            return execute((CopyVolumeCommand) cmd);
        } else if (clazz == ResizeVolumeCommand.class) {
            return execute((ResizeVolumeCommand) cmd);
        } else if (clazz == AttachVolumeCommand.class) {
            return execute((AttachVolumeCommand) cmd);
        } else if (clazz == AttachIsoCommand.class) {
            return execute((AttachIsoCommand) cmd);
        } else if (clazz == ManageSnapshotCommand.class) {
            return execute((ManageSnapshotCommand) cmd);
        } else if (clazz == BackupSnapshotCommand.class) {
            return execute((BackupSnapshotCommand) cmd);
        } else if (clazz == CreateVolumeFromSnapshotCommand.class) {
            return execute((CreateVolumeFromSnapshotCommand) cmd);
        } else if (clazz == CreatePrivateTemplateFromVolumeCommand.class) {
            return execute((CreatePrivateTemplateFromVolumeCommand) cmd);
        } else if (clazz == CreatePrivateTemplateFromSnapshotCommand.class) {
            return execute((CreatePrivateTemplateFromSnapshotCommand) cmd);
        } else if (clazz == UpgradeSnapshotCommand.class) {
            return execute((UpgradeSnapshotCommand) cmd);
        } else if (clazz == GetStorageStatsCommand.class) {
            return execute((GetStorageStatsCommand) cmd);
        } else if (clazz == PrimaryStorageDownloadCommand.class) {
            return execute((PrimaryStorageDownloadCommand) cmd);
        } else if (clazz == GetVncPortCommand.class) {
            return execute((GetVncPortCommand) cmd);
        } else if (clazz == SetupCommand.class) {
            return execute((SetupCommand) cmd);
        } else if (clazz == MaintainCommand.class) {
            return execute((MaintainCommand) cmd);
        } else if (clazz == PingTestCommand.class) {
            return execute((PingTestCommand) cmd);
        } else if (clazz == CheckOnHostCommand.class) {
            return execute((CheckOnHostCommand) cmd);
        } else if (clazz == ModifySshKeysCommand.class) {
            return execute((ModifySshKeysCommand) cmd);
        } else if (clazz == PoolEjectCommand.class) {
            return execute((PoolEjectCommand) cmd);
        } else if (clazz == StartCommand.class) {
            return execute((StartCommand)cmd);
        } else if (clazz == RemoteAccessVpnCfgCommand.class) {
            return execute((RemoteAccessVpnCfgCommand)cmd);
        } else if (clazz == VpnUsersCfgCommand.class) {
            return execute((VpnUsersCfgCommand)cmd);
        } else if (clazz == CheckSshCommand.class) {
            return execute((CheckSshCommand)cmd);
        } else if (clazz == SecurityGroupRulesCmd.class) {
            return execute((SecurityGroupRulesCmd) cmd);
        } else if (clazz == OvsFetchInterfaceCommand.class) {
            return execute((OvsFetchInterfaceCommand)cmd);
        } else if (clazz == OvsCreateGreTunnelCommand.class) {
            return execute((OvsCreateGreTunnelCommand)cmd);
        } else if (clazz == OvsSetTagAndFlowCommand.class) {
            return execute((OvsSetTagAndFlowCommand)cmd);
        } else if (clazz == OvsDeleteFlowCommand.class) {
            return execute((OvsDeleteFlowCommand)cmd);
        } else if (clazz == CleanupNetworkRulesCmd.class){
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
        } else if (cmd instanceof CheckRouterCommand) {
            return execute((CheckRouterCommand)cmd);
        } else if (cmd instanceof SetFirewallRulesCommand) {
            return execute((SetFirewallRulesCommand)cmd);
        } else if (cmd instanceof BumpUpPriorityCommand) {
            return execute((BumpUpPriorityCommand)cmd);
        } else if (cmd instanceof ClusterSyncCommand) {
            return execute((ClusterSyncCommand)cmd);
        } else if (cmd instanceof GetDomRVersionCmd) {
            return execute((GetDomRVersionCmd)cmd);
        } else if (clazz == CheckNetworkCommand.class) {
            return execute((CheckNetworkCommand) cmd);
        } else if (clazz == SetupGuestNetworkCommand.class) {
            return execute((SetupGuestNetworkCommand) cmd);
        } else if (clazz == PlugNicCommand.class) {
            return execute((PlugNicCommand) cmd);
        } else if (clazz == UnPlugNicCommand.class) {
            return execute((UnPlugNicCommand) cmd);
        } else if (clazz == IpAssocVpcCommand.class) {
            return execute((IpAssocVpcCommand) cmd);
        } else if (clazz == SetSourceNatCommand.class) {
            return execute((SetSourceNatCommand) cmd);
        } else if (clazz == SetNetworkACLCommand.class) {
            return execute((SetNetworkACLCommand) cmd);
        } else if (clazz == SetPortForwardingRulesVpcCommand.class) {
            return execute((SetPortForwardingRulesVpcCommand) cmd);
        } else if (clazz == SetStaticRouteCommand.class) {
            return execute((SetStaticRouteCommand) cmd);
        } else if (clazz == Site2SiteVpnCfgCommand.class) {
            return execute((Site2SiteVpnCfgCommand) cmd);
        } else if (clazz == CheckS2SVpnConnectionsCommand.class) {
            return execute((CheckS2SVpnConnectionsCommand) cmd);
        } else if (cmd instanceof StorageSubSystemCommand) {
            return this.storageResource.handleStorageCommands((StorageSubSystemCommand)cmd);
        } else if (clazz == CreateVMSnapshotCommand.class) {
            return execute((CreateVMSnapshotCommand)cmd);
        } else if (clazz == DeleteVMSnapshotCommand.class) {
            return execute((DeleteVMSnapshotCommand)cmd);
        } else if (clazz == RevertToVMSnapshotCommand.class) {
            return execute((RevertToVMSnapshotCommand)cmd);
        } else if (clazz == NetworkRulesVmSecondaryIpCommand.class) {
            return execute((NetworkRulesVmSecondaryIpCommand)cmd);
        } else {
            return Answer.createUnsupportedCommandAnswer(cmd);
        }
    }



    private Answer execute(RevertToVMSnapshotCommand cmd) {
        String vmName = cmd.getVmName();
        List<VolumeTO> listVolumeTo = cmd.getVolumeTOs();
        VMSnapshot.Type vmSnapshotType = cmd.getTarget().getType();
        Boolean snapshotMemory = vmSnapshotType == VMSnapshot.Type.DiskAndMemory;
        Connection conn = getConnection();
        VirtualMachine.State vmState = null;
        VM vm = null;
        try {

            // remove vm from s_vms, for delta sync
            s_vms.remove(_cluster, _name, vmName);

            Set<VM> vmSnapshots = VM.getByNameLabel(conn, cmd.getTarget().getSnapshotName());
            if(vmSnapshots.size() == 0)
                return new RevertToVMSnapshotAnswer(cmd, false, "Cannot find vmSnapshot with name: " + cmd.getTarget().getSnapshotName());
            
            VM vmSnapshot = vmSnapshots.iterator().next();
            
            // find target VM or creating a work VM
            try {
                vm = getVM(conn, vmName);
            } catch (Exception e) {
                vm = createWorkingVM(conn, vmName, cmd.getGuestOSType(), listVolumeTo);
            }

            if (vm == null) {
                return new RevertToVMSnapshotAnswer(cmd, false,
                        "Revert to VM Snapshot Failed due to can not find vm: " + vmName);
            }
            
            // call plugin to execute revert
            revertToSnapshot(conn, vmSnapshot, vmName, vm.getUuid(conn), snapshotMemory, _host.uuid);
            vm = getVM(conn, vmName);
            Set<VBD> vbds = vm.getVBDs(conn);
            Map<String, VDI> vdiMap = new HashMap<String, VDI>();
            // get vdi:vbdr to a map
            for (VBD vbd : vbds) {
                VBD.Record vbdr = vbd.getRecord(conn);
                if (vbdr.type == Types.VbdType.DISK) {
                    VDI vdi = vbdr.VDI;
                    vdiMap.put(vbdr.userdevice, vdi);
                }
            }

            if (!snapshotMemory) {
                vm.destroy(conn);
                vmState = VirtualMachine.State.Stopped;
            } else {
                s_vms.put(_cluster, _name, vmName, State.Running);
                vmState = VirtualMachine.State.Running;
            }

            // after revert, VM's volumes path have been changed, need to report to manager
            for (VolumeTO volumeTo : listVolumeTo) {
                Long deviceId = volumeTo.getDeviceId();
                VDI vdi = vdiMap.get(deviceId.toString());
                volumeTo.setPath(vdi.getUuid(conn));
            }

            return new RevertToVMSnapshotAnswer(cmd, listVolumeTo,vmState);
        } catch (Exception e) {
            s_logger.error("revert vm " + vmName
                    + " to snapshot " + cmd.getTarget().getSnapshotName() + " failed due to " + e.getMessage());
            return new RevertToVMSnapshotAnswer(cmd, false, e.getMessage());
        } 
    }

    private String revertToSnapshot(Connection conn, VM vmSnapshot,
            String vmName, String oldVmUuid, Boolean snapshotMemory, String hostUUID)
            throws XenAPIException, XmlRpcException {
 
        String results = callHostPluginAsync(conn, "vmopsSnapshot",
                "revert_memory_snapshot", 10 * 60 * 1000, "snapshotUUID",
                vmSnapshot.getUuid(conn), "vmName", vmName, "oldVmUuid",
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

    protected XsLocalNetwork getNativeNetworkForTraffic(Connection conn, TrafficType type, String name) throws XenAPIException, XmlRpcException {
        if (name != null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Looking for network named " + name);
            }
            return getNetworkByName(conn, name);
        }

        if (type == TrafficType.Guest) {
            return new XsLocalNetwork(Network.getByUuid(conn, _host.guestNetwork), null, PIF.getByUuid(conn, _host.guestPif), null);
        } else if (type == TrafficType.Control) {
            setupLinkLocalNetwork(conn);
            return new XsLocalNetwork(Network.getByUuid(conn, _host.linkLocalNetwork));
        } else if (type == TrafficType.Management) {
            return new XsLocalNetwork(Network.getByUuid(conn, _host.privateNetwork), null, PIF.getByUuid(conn, _host.privatePif), null);
        } else if (type == TrafficType.Public) {
            return new XsLocalNetwork(Network.getByUuid(conn, _host.publicNetwork), null, PIF.getByUuid(conn, _host.publicPif), null);
        } else if (type == TrafficType.Storage) {
            /*   TrafficType.Storage is for secondary storage, while storageNetwork1 is for primary storage, we need better name here */
            return new XsLocalNetwork(Network.getByUuid(conn, _host.storageNetwork1), null, PIF.getByUuid(conn, _host.storagePif1), null);
        }


        throw new CloudRuntimeException("Unsupported network type: " + type);
    }

    /**
     * This is a tricky to create network in xenserver.
     * if you create a network then create bridge by brctl or openvswitch yourself,
     * then you will get an expection that is "REQUIRED_NETWROK" when you start a
     * vm with this network. The soultion is, create a vif of dom0 and plug it in
     * network, xenserver will create the bridge on behalf of you
     * @throws XmlRpcException
     * @throws XenAPIException
     */
    private void enableXenServerNetwork(Connection conn, Network nw,
            String vifNameLabel, String networkDesc) throws XenAPIException, XmlRpcException {
        /* Make sure there is a physical bridge on this network */
        VIF dom0vif = null;
        Pair<VM, VM.Record> vm = getControlDomain(conn);
        VM dom0 = vm.first();
        // Create a VIF unless there's not already another VIF
        Set<VIF> dom0Vifs = dom0.getVIFs(conn);
        for (VIF vif:dom0Vifs) {
            vif.getRecord(conn);
            if (vif.getNetwork(conn).getUuid(conn) == nw.getUuid(conn)) {
                dom0vif = vif;
                s_logger.debug("A VIF for dom0 has already been found - No need to create one");
            }
        }
        if (dom0vif == null) {
            s_logger.debug("Create a vif on dom0 for " + networkDesc);
            VIF.Record vifr = new VIF.Record();
            vifr.VM = dom0;
            vifr.device = getLowestAvailableVIFDeviceNum(conn, dom0);
            if (vifr.device == null) {
                s_logger.debug("Failed to create " + networkDesc + ", no vif available");
                return;
            }
            Map<String, String> config = new HashMap<String, String>();
            config.put("nameLabel", vifNameLabel);
            vifr.otherConfig = config;
            vifr.MAC = "FE:FF:FF:FF:FF:FF";
            vifr.network = nw;

            dom0vif = VIF.create(conn, vifr);
        }
        // At this stage we surely have a VIF
        dom0vif.plug(conn);
        dom0vif.unplug(conn);
        synchronized(_tmpDom0Vif) {
            _tmpDom0Vif.add(dom0vif);
        }

    }

    private synchronized Network setupvSwitchNetwork(Connection conn) {
        try {
            if (_host.vswitchNetwork == null) {
                Network vswitchNw = null;
                Network.Record rec = new Network.Record();
                String nwName = Networks.BroadcastScheme.VSwitch.toString();
                Set<Network> networks = Network.getByNameLabel(conn, nwName);

                if (networks.size() == 0) {
                    rec.nameDescription = "vswitch network for " + nwName;
                    rec.nameLabel = nwName;
                    vswitchNw = Network.create(conn, rec);
                } else {
                    vswitchNw = networks.iterator().next();
                }

                enableXenServerNetwork(conn, vswitchNw, "vswitch", "vswicth network");
                _host.vswitchNetwork = vswitchNw;
            }
            return _host.vswitchNetwork;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * This method just creates a XenServer network following the tunnel network naming convention
     */
    private synchronized Network findOrCreateTunnelNetwork(Connection conn, long key) {
        try {
            String nwName = "OVSTunnel" + key;
            Network nw = null;
            Network.Record rec = new Network.Record();
            Set<Network> networks = Network.getByNameLabel(conn, nwName);

            if (networks.size() == 0) {
                rec.nameDescription = "tunnel network id# " + key;
                rec.nameLabel = nwName;
                //Initialize the ovs-host-setup to avoid error when doing get-param in plugin
                Map<String,String> otherConfig = new HashMap<String,String>();
                otherConfig.put("ovs-host-setup", "");
                rec.otherConfig = otherConfig;
                nw = Network.create(conn, rec);
                // Plug dom0 vif only when creating network
                enableXenServerNetwork(conn, nw, nwName, "tunnel network for account " + key);
                s_logger.debug("### Xen Server network for tunnels created:" + nwName);                
            } else {
                nw = networks.iterator().next();
                s_logger.debug("Xen Server network for tunnels found:" + nwName);                
            }
            return nw;
        } catch (Exception e) {
            s_logger.warn("createTunnelNetwork failed", e);
            return null;
        }
    }

    /**
     * This method creates a XenServer network and configures it for being used as a L2-in-L3 tunneled network
     */
    private synchronized Network configureTunnelNetwork(Connection conn, long networkId, long hostId, int key) {
        try {
            Network nw = findOrCreateTunnelNetwork(conn, key);
            String nwName = "OVSTunnel" + key;
            //Invoke plugin to setup the bridge which will be used by this network
            String bridge = nw.getBridge(conn);
            Map<String,String> nwOtherConfig = nw.getOtherConfig(conn);
            String configuredHosts = nwOtherConfig.get("ovs-host-setup");
            boolean configured = false;
            if (configuredHosts!=null) {
                String hostIdsStr[] = configuredHosts.split(",");
                for (String hostIdStr:hostIdsStr) {
                    if (hostIdStr.equals(((Long)hostId).toString())) {
                        configured = true;
                        break;
                    }
                }
            }
            if (!configured) {
                // Plug dom0 vif only if not done before for network and host
                enableXenServerNetwork(conn, nw, nwName, "tunnel network for account " + key);
                String result = callHostPlugin(conn, "ovstunnel", "setup_ovs_bridge", "bridge", bridge,
                        "key", String.valueOf(key),
                        "xs_nw_uuid", nw.getUuid(conn),
                        "cs_host_id", ((Long)hostId).toString());
                //Note down the fact that the ovs bridge has been setup
                String[] res = result.split(":");
                if (res.length != 2 || !res[0].equalsIgnoreCase("SUCCESS")) {
                    //TODO: Should make this error not fatal?
                    throw new CloudRuntimeException("Unable to pre-configure OVS bridge " + bridge + " for network ID:" + networkId +
                            " - " + res);
                }
            }
            return nw;
        } catch (Exception e) {
            s_logger.warn("createandConfigureTunnelNetwork failed", e);
            return null;
        }
    }

    private synchronized void destroyTunnelNetwork(Connection conn, int key) {
        try {
            Network nw = findOrCreateTunnelNetwork(conn, key);
            String bridge = nw.getBridge(conn);
            String result = callHostPlugin(conn, "ovstunnel", "destroy_ovs_bridge", "bridge", bridge);
            String[] res = result.split(":");
            if (res.length != 2 || !res[0].equalsIgnoreCase("SUCCESS")) {
                //TODO: Should make this error not fatal?
                //Can Concurrent VM shutdown/migration/reboot events can cause this method
                //to be executed on a bridge which has already been removed?
                throw new CloudRuntimeException("Unable to remove OVS bridge " + bridge + ":" + res);
            }
            return;
        } catch (Exception e) {
            s_logger.warn("destroyTunnelNetwork failed:", e);
            return;
        }
    }

    protected Network getNetwork(Connection conn, NicTO nic) throws XenAPIException, XmlRpcException {
        String name = nic.getName();
        XsLocalNetwork network = getNativeNetworkForTraffic(conn, nic.getType(), name);
        if (network == null) {
            s_logger.error("Network is not configured on the backend for nic " + nic.toString());
            throw new CloudRuntimeException("Network for the backend is not configured correctly for network broadcast domain: " + nic.getBroadcastUri());
        }
        if (nic.getBroadcastUri() != null && nic.getBroadcastUri().toString().contains("untagged")) {
            return network.getNetwork();
        } else if (nic.getBroadcastType() == BroadcastDomainType.Vlan) {
            URI broadcastUri = nic.getBroadcastUri();
            assert broadcastUri.getScheme().equals(BroadcastDomainType.Vlan.scheme());
            long vlan = Long.parseLong(broadcastUri.getHost());
            return enableVlanNetwork(conn, vlan, network);
        } else if (nic.getBroadcastType() == BroadcastDomainType.Native || nic.getBroadcastType() == BroadcastDomainType.LinkLocal) {
            return network.getNetwork();
        } else if (nic.getBroadcastType() == BroadcastDomainType.Vswitch) {
            String broadcastUri = nic.getBroadcastUri().toString();
            String header = broadcastUri.substring(Networks.BroadcastDomainType.Vswitch.scheme().length() + "://".length());
            if (header.startsWith("vlan")) {
                _isOvs = true;
                return setupvSwitchNetwork(conn);
            } else {
                long vnetId = Long.parseLong(nic.getBroadcastUri().getHost());
                return findOrCreateTunnelNetwork(conn, vnetId);
            }
        } else if (nic.getBroadcastType() == BroadcastDomainType.Storage) {
            URI broadcastUri = nic.getBroadcastUri();
            if (broadcastUri == null) {
                return network.getNetwork();
            } else {
                long vlan = Long.parseLong(broadcastUri.getHost());
                return enableVlanNetwork(conn, vlan, network);
            }
        } else if (nic.getBroadcastType() == BroadcastDomainType.Lswitch) {
            // Nicira Logical Switch
            return network.getNetwork();
        }

        throw new CloudRuntimeException("Unable to support this type of network broadcast domain: " + nic.getBroadcastUri());
    }

    protected VIF createVif(Connection conn, String vmName, VM vm, NicTO nic) throws XmlRpcException, XenAPIException {
        assert(nic.getUuid() != null) : "Nic should have a uuid value";

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

        vifr.network = getNetwork(conn, nic);

        if (nic.getNetworkRateMbps() != null && nic.getNetworkRateMbps().intValue() != -1) {
            vifr.qosAlgorithmType = "ratelimit";
            vifr.qosAlgorithmParams = new HashMap<String, String>();
            // convert mbs to kilobyte per second
            vifr.qosAlgorithmParams.put("kbps", Integer.toString(nic.getNetworkRateMbps() * 128));
        }

        VIF vif = VIF.create(conn, vifr);
        if (s_logger.isDebugEnabled()) {
            vifr = vif.getRecord(conn);
            s_logger.debug("Created a vif " + vifr.uuid + " on " + nic.getDeviceId());
        }

        return vif;
    }

    protected void prepareISO(Connection conn, String vmName) throws XmlRpcException, XenAPIException {

        Set<VM> vms = VM.getByNameLabel(conn, vmName);
        if( vms == null || vms.size() != 1) {
            throw new CloudRuntimeException("There are " + ((vms == null) ? "0" : vms.size()) + " VMs named " + vmName);
        }
        VM vm = vms.iterator().next();
        Set<VBD> vbds = vm.getVBDs(conn);
        for ( VBD vbd : vbds ) {
            VBD.Record vbdr = vbd.getRecord(conn);
            if ( vbdr.type == Types.VbdType.CD && vbdr.empty == false ) {
                VDI vdi = vbdr.VDI;
                SR sr = vdi.getSR(conn);
                Set<PBD> pbds = sr.getPBDs(conn);
                if( pbds == null ) {
                    throw new CloudRuntimeException("There is no pbd for sr " + sr);
                }
                for ( PBD pbd : pbds ) {
                    PBD.Record pbdr = pbd.getRecord(conn);
                    if ( pbdr.host.getUuid(conn).equals(_host.uuid)) {
                        return;
                    }
                }
                sr.setShared(conn, true);
                Host host = Host.getByUuid(conn, _host.uuid);
                PBD.Record pbdr = pbds.iterator().next().getRecord(conn);
                pbdr.host = host;
                pbdr.uuid = "";
                PBD pbd = PBD.create(conn, pbdr);
                pbdPlug(conn, pbd, pbd.getUuid(conn));
                break;
            }
        }
    }

    protected VDI mount(Connection conn, String vmName, VolumeTO volume) throws XmlRpcException, XenAPIException {
        if (volume.getType() == Volume.Type.ISO) {
            String isopath = volume.getPath();
            if (isopath == null) {
                return null;
            }
            if (isopath.startsWith("xs-tools")) {
                try {
                    Set<VDI> vdis = VDI.getByNameLabel(conn, isopath);
                    if (vdis.isEmpty()) {
                        throw new CloudRuntimeException("Could not find ISO with URL: " + isopath);
                    }
                    return vdis.iterator().next();

                } catch (XenAPIException e) {
                    throw new CloudRuntimeException("Unable to get pv iso: " + isopath + " due to " + e.toString());
                } catch (Exception e) {
                    throw new CloudRuntimeException("Unable to get pv iso: " + isopath + " due to " + e.toString());
                }
            }


            int index = isopath.lastIndexOf("/");

            String mountpoint = isopath.substring(0, index);
            URI uri;
            try {
                uri = new URI(mountpoint);
            } catch (URISyntaxException e) {
                throw new CloudRuntimeException("Incorrect uri " + mountpoint, e);
            }
            SR isoSr = createIsoSRbyURI(conn, uri, vmName, false);

            String isoname = isopath.substring(index + 1);

            VDI isoVdi = getVDIbyLocationandSR(conn, isoname, isoSr);

            if (isoVdi == null) {
                throw new CloudRuntimeException("Unable to find ISO " + volume.getPath());
            }
            return isoVdi;
        } else {
            return VDI.getByUuid(conn, volume.getPath());
        }
    }

    protected VBD createVbd(Connection conn, VolumeTO volume, String vmName, VM vm, BootloaderType bootLoaderType) throws XmlRpcException, XenAPIException {
        Volume.Type type = volume.getType();

        VDI vdi = mount(conn, vmName, volume);

        VBD.Record vbdr = new VBD.Record();
        vbdr.VM = vm;
        if (vdi != null) {
            vbdr.VDI = vdi;
        } else {
            vbdr.empty = true;
        }
        if (type == Volume.Type.ROOT && bootLoaderType == BootloaderType.PyGrub) {
            vbdr.bootable = true;
        }else if(type == Volume.Type.ISO && bootLoaderType == BootloaderType.CD) {
            vbdr.bootable = true;
        }

        vbdr.userdevice = Long.toString(volume.getDeviceId());
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
        VBD vbd = VBD.create(conn, vbdr);

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("VBD " + vbd.getUuid(conn) + " created for " + volume);
        }

        return vbd;
    }

    protected VM createVmFromTemplate(Connection conn, VirtualMachineTO vmSpec, Host host) throws XenAPIException, XmlRpcException {
        String guestOsTypeName = getGuestOsType(vmSpec.getOs(), vmSpec.getBootloader() == BootloaderType.CD);
        if ( guestOsTypeName == null ) {
            String msg =  " Hypervisor " + this.getClass().getName() + " doesn't support guest OS type " + vmSpec.getOs()
                    + ". you can choose 'Other install media' to run it as HVM";
            s_logger.warn(msg);
            throw new CloudRuntimeException(msg);
        }
        Set<VM> templates = VM.getByNameLabel(conn, guestOsTypeName);
        assert templates.size() == 1 : "Should only have 1 template but found " + templates.size();
        VM template = templates.iterator().next();

        VM vm = template.createClone(conn, vmSpec.getName());
        VM.Record vmr = vm.getRecord(conn);
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Created VM " + vmr.uuid + " for " + vmSpec.getName());
        }

        for (Console console : vmr.consoles) {
            console.destroy(conn);
        }

        vm.setIsATemplate(conn, false);
        vm.setAffinity(conn, host);
        vm.removeFromOtherConfig(conn, "disks");
        vm.setNameLabel(conn, vmSpec.getName());
        setMemory(conn, vm, vmSpec.getMinRam(),vmSpec.getMaxRam());
        vm.setVCPUsMax(conn, (long)vmSpec.getCpus());
        vm.setVCPUsAtStartup(conn, (long)vmSpec.getCpus());

        Map<String, String> vcpuParams = new HashMap<String, String>();

        Integer speed = vmSpec.getMinSpeed();
        if (speed != null) {

            int cpuWeight = _maxWeight; //cpu_weight
            long utilization = 0; // max CPU cap, default is unlimited

            // weight based allocation
            cpuWeight = (int)((speed*0.99) / _host.speed * _maxWeight);
            if (cpuWeight > _maxWeight) {
                cpuWeight = _maxWeight;
            }

            if (vmSpec.getLimitCpuUse()) {
                utilization = ((long)speed * 100 * vmSpec.getCpus()) / _host.speed ;
            }

            vcpuParams.put("weight", Integer.toString(cpuWeight));
            vcpuParams.put("cap", Long.toString(utilization));
        }

        if (vcpuParams.size() > 0) {
            vm.setVCPUsParams(conn, vcpuParams);
        }

        vm.setActionsAfterCrash(conn, Types.OnCrashBehaviour.DESTROY);
        vm.setActionsAfterShutdown(conn, Types.OnNormalExit.DESTROY);

        String bootArgs = vmSpec.getBootArgs();
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
                VolumeTO [] disks = vmSpec.getDisks();
                for (VolumeTO disk : disks) {
                    if (disk.getType() == Volume.Type.ISO && disk.getOsType() != null) {
                        String isoGuestOsName = getGuestOsType(disk.getOsType(), vmSpec.getBootloader() == BootloaderType.CD);
                        if (!isoGuestOsName.equals(guestOsTypeName)) {
                            vmSpec.setBootloader(BootloaderType.PyGrub);
                        }
                    }
                }
            }
            if (vmSpec.getBootloader() == BootloaderType.CD) {
                vm.setPVBootloader(conn, "eliloader");
                Map<String, String> otherConfig = vm.getOtherConfig(conn);
                if ( ! vm.getOtherConfig(conn).containsKey("install-repository") ) {
                    otherConfig.put( "install-repository", "cdrom");
                }
                vm.setOtherConfig(conn, otherConfig);
            } else if (vmSpec.getBootloader() == BootloaderType.PyGrub ){
                vm.setPVBootloader(conn, "pygrub");
            } else {
                vm.destroy(conn);
                throw new CloudRuntimeException("Unable to handle boot loader type: " + vmSpec.getBootloader());
            }
        }
        return vm;
    }

    protected String handleVmStartFailure(Connection conn, String vmName, VM vm, String message, Throwable th) {
        String msg = "Unable to start " + vmName + " due to " + message;
        s_logger.warn(msg, th);

        if (vm == null) {
            return msg;
        }

        try {
            VM.Record vmr = vm.getRecord(conn);
            List<Network> networks = new ArrayList<Network>();
            for (VIF vif : vmr.VIFs) {
                try {
                    VIF.Record rec = vif.getRecord(conn);
                    networks.add(rec.network);
                } catch (Exception e) {
                    s_logger.warn("Unable to cleanup VIF", e);
                }
            }
            if (vmr.powerState == VmPowerState.RUNNING) {
                try {
                    vm.hardShutdown(conn);
                } catch (Exception e) {
                    s_logger.warn("VM hardshutdown failed due to ", e);
                }
            }
            if (vm.getPowerState(conn) == VmPowerState.HALTED) {
                try {
                    vm.destroy(conn);
                } catch (Exception e) {
                    s_logger.warn("VM destroy failed due to ", e);
                }
            }
            for (VBD vbd : vmr.VBDs) {
                try {
                    vbd.unplug(conn);
                    vbd.destroy(conn);
                } catch (Exception e) {
                    s_logger.warn("Unable to clean up VBD due to ", e);
                }
            }
            for (VIF vif : vmr.VIFs) {
                try {
                    vif.unplug(conn);
                    vif.destroy(conn);
                } catch (Exception e) {
                    s_logger.warn("Unable to cleanup VIF", e);
                }
            }
            for (Network network : networks) {
                if (network.getNameLabel(conn).startsWith("VLAN")) {
                    disableVlanNetwork(conn, network);
                }
            }                    
        } catch (Exception e) {
            s_logger.warn("VM getRecord failed due to ", e);
        }

        return msg;
    }

    protected VBD createPatchVbd(Connection conn, String vmName, VM vm) throws XmlRpcException, XenAPIException {

        if(  _host.systemvmisouuid == null ) {
            Set<SR> srs = SR.getByNameLabel(conn, "XenServer Tools");
            if( srs.size() != 1 ) {
                throw new CloudRuntimeException("There are " + srs.size() + " SRs with name XenServer Tools");
            }
            SR sr = srs.iterator().next();
            sr.scan(conn);

            SR.Record srr = sr.getRecord(conn);

            if(  _host.systemvmisouuid == null ) {
                for( VDI vdi : srr.VDIs ) {
                    VDI.Record vdir = vdi.getRecord(conn);
                    if(vdir.nameLabel.contains("systemvm.iso")){
                        _host.systemvmisouuid = vdir.uuid;
                        break;
                    }
                }
            }
            if(  _host.systemvmisouuid == null ) {
                throw new CloudRuntimeException("can not find systemvmiso");
            }
        }

        VBD.Record cdromVBDR = new VBD.Record();
        cdromVBDR.VM = vm;
        cdromVBDR.empty = true;
        cdromVBDR.bootable = false;
        cdromVBDR.userdevice = "3";
        cdromVBDR.mode = Types.VbdMode.RO;
        cdromVBDR.type = Types.VbdType.CD;
        VBD cdromVBD = VBD.create(conn, cdromVBDR);
        cdromVBD.insert(conn, VDI.getByUuid(conn, _host.systemvmisouuid));

        return cdromVBD;
    }

    protected void destroyPatchVbd(Connection conn, String vmName) throws XmlRpcException, XenAPIException {
        try {
            if( !vmName.startsWith("r-") && !vmName.startsWith("s-") && !vmName.startsWith("v-") ) {
                return;
            }
            Set<VM> vms = VM.getByNameLabel(conn, vmName);
            for ( VM vm : vms ) {
                Set<VBD> vbds = vm.getVBDs(conn);
                for( VBD vbd : vbds ) {
                    if (vbd.getType(conn) == Types.VbdType.CD ) {
                        vbd.eject(conn);
                        vbd.destroy(conn);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            s_logger.debug("Cannot destory CD-ROM device for VM " + vmName + " due to " + e.toString(), e);
        }
    }

    protected CheckSshAnswer execute(CheckSshCommand cmd) {
        Connection conn = getConnection();
        String vmName = cmd.getName();
        String privateIp = cmd.getIp();
        int cmdPort = cmd.getPort();

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Ping command port, " + privateIp + ":" + cmdPort);
        }

        try {
            String result = connect(conn, cmd.getName(), privateIp, cmdPort);
            if (result != null) {
                return new CheckSshAnswer(cmd, "Can not ping System vm " + vmName + "due to:" + result);
            }
            destroyPatchVbd(conn, vmName);
        } catch (Exception e) {
            return new CheckSshAnswer(cmd, e);
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Ping command port succeeded for vm " + vmName);
        }

        return new CheckSshAnswer(cmd);
    }

    private HashMap<String, String> parseDefaultOvsRuleComamnd(String str) {
        HashMap<String, String> cmd = new HashMap<String, String>();
        String[] sarr = str.split("/");
        for (int i = 0; i < sarr.length; i++) {
            String c = sarr[i];
            c = c.startsWith("/") ? c.substring(1) : c;
            c = c.endsWith("/") ? c.substring(0, c.length() - 1) : c;
            String[] p = c.split(";");
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

    private void cleanUpTmpDomVif(Connection conn) {
        List<VIF> vifs;
        synchronized(_tmpDom0Vif) {
            vifs = _tmpDom0Vif;
            _tmpDom0Vif = new ArrayList<VIF>();
        }

        for (VIF v : vifs) {
            String vifName = "unkown";
            try {
                VIF.Record vifr = v.getRecord(conn);
                Map<String, String> config = vifr.otherConfig;
                vifName = config.get("nameLabel");
                v.destroy(conn);
                s_logger.debug("Destroy temp dom0 vif" + vifName + " success");
            } catch (Exception e) {
                s_logger.warn("Destroy temp dom0 vif " + vifName + "failed", e);
            }
        }
    }

    @Override
    public StartAnswer execute(StartCommand cmd) {
        Connection conn = getConnection();
        VirtualMachineTO vmSpec = cmd.getVirtualMachine();
        String vmName = vmSpec.getName();
        State state = State.Stopped;
        VM vm = null;
        try {
            Set<VM> vms = VM.getByNameLabel(conn, vmName);
            if ( vms != null ) {
                for ( VM v : vms ) {
                    VM.Record vRec = v.getRecord(conn);
                    if ( vRec.powerState == VmPowerState.HALTED ) {
                        v.destroy(conn);
                    } else if ( vRec.powerState == VmPowerState.RUNNING ) {
                        String host = vRec.residentOn.getUuid(conn);
                        String msg = "VM " + vmName + " is runing on host " + host;
                        s_logger.debug(msg);
                        return new StartAnswer(cmd, msg, host);
                    } else {
                        String msg = "There is already a VM having the same name " + vmName + " vm record " +  vRec.toString();
                        s_logger.warn(msg);
                        return new StartAnswer(cmd, msg);
                    }
                }
            }
            synchronized (_cluster.intern()) {
                s_vms.put(_cluster, _name, vmName, State.Starting);
            }
            s_logger.debug("1. The VM " + vmName + " is in Starting state.");

            Host host = Host.getByUuid(conn, _host.uuid);
            vm = createVmFromTemplate(conn, vmSpec, host);

            for (VolumeTO disk : vmSpec.getDisks()) {
                createVbd(conn, disk, vmName, vm, vmSpec.getBootloader());
            }

            if (vmSpec.getType() != VirtualMachine.Type.User) {
                createPatchVbd(conn, vmName, vm);
            }

            for (NicTO nic : vmSpec.getNics()) {
                createVif(conn, vmName, vm, nic);
            }

            startVM(conn, host, vm, vmName);

            if (_isOvs) {
                // TODO(Salvatore-orlando): This code should go
                for (NicTO nic : vmSpec.getNics()) {
                    if (nic.getBroadcastType() == Networks.BroadcastDomainType.Vswitch) {
                        HashMap<String, String> args = parseDefaultOvsRuleComamnd(nic.getBroadcastUri().toString().substring(Networks.BroadcastDomainType.Vswitch.scheme().length() + "://".length()));
                        OvsSetTagAndFlowCommand flowCmd = new OvsSetTagAndFlowCommand(args.get("vmName"), args.get("tag"), args.get("vlans"),
                                args.get("seqno"), Long.parseLong(args.get("vmId")));
                        OvsSetTagAndFlowAnswer r = execute(flowCmd);
                        if (!r.getResult()) {
                            s_logger.warn("Failed to set flow for VM " + r.getVmId());
                        } else {
                            s_logger.info("Success to set flow for VM " + r.getVmId());
                        }
                    }
                }
            }
            cleanUpTmpDomVif(conn);

            if (_canBridgeFirewall) {
                String result = null;
                if (vmSpec.getType() != VirtualMachine.Type.User) {
                    NicTO[] nics = vmSpec.getNics();
                    boolean secGrpEnabled = false;
                    for (NicTO nic : nics) {
                        if (nic.isSecurityGroupEnabled() || (nic.getIsolationUri() != null
                                && nic.getIsolationUri().getScheme().equalsIgnoreCase(IsolationType.Ec2.toString()))) {
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
                    NicTO[] nics = vmSpec.getNics();
                    for (NicTO nic : nics) {
                        if ( nic.isSecurityGroupEnabled() || nic.getIsolationUri() != null
                                && nic.getIsolationUri().getScheme().equalsIgnoreCase(IsolationType.Ec2.toString())) {
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
                            result = callHostPlugin(conn, "vmops", "default_network_rules", "vmName", vmName, "vmIP", nic.getIp(), "vmMAC", nic.getMac(), "vmID", Long.toString(vmSpec.getId()), "secIps", secIpsStr);

                            if (result == null || result.isEmpty() || !Boolean.parseBoolean(result)) {
                                s_logger.warn("Failed to program default network rules for " + vmName+" on nic with ip:"+nic.getIp()+" mac:"+nic.getMac());
                            } else {
                                s_logger.info("Programmed default network rules for " + vmName+" on nic with ip:"+nic.getIp()+" mac:"+nic.getMac());
                            }
                        }
                    }
                }
            }

            state = State.Running;
            return new StartAnswer(cmd);
        } catch (Exception e) {
            s_logger.warn("Catch Exception: " + e.getClass().toString() + " due to " + e.toString(), e);
            String msg = handleVmStartFailure(conn, vmName, vm, "", e);
            return new StartAnswer(cmd, msg);
        } finally {
            synchronized (_cluster.intern()) {
                if (state != State.Stopped) { 
                    s_vms.put(_cluster, _name, vmName, state);
                    s_logger.debug("2. The VM " + vmName + " is in " + state + " state.");
                } else {
                    s_vms.remove(_cluster, _name, vmName);
                    s_logger.debug("The VM is in stopped state, detected problem during startup : " + vmName);
                }
            }
        }
    }

    protected Answer execute(ModifySshKeysCommand cmd) {
        return new Answer(cmd);
    }

    private boolean doPingTest(Connection conn, final String computingHostIp) {
        String args = "-h " + computingHostIp;
        String result = callHostPlugin(conn, "vmops", "pingtest", "args", args);
        if (result == null || result.isEmpty()) {
            return false;
        }
        return true;
    }

    protected CheckOnHostAnswer execute(CheckOnHostCommand cmd) {
        return new CheckOnHostAnswer(cmd, null, "Not Implmeneted");
    }

    private boolean doPingTest(Connection conn, final String domRIp, final String vmIp) {
        String args = "-i " + domRIp + " -p " + vmIp;
        String result = callHostPlugin(conn, "vmops", "pingtest", "args", args);
        if (result == null || result.isEmpty()) {
            return false;
        }
        return true;
    }

    private Answer execute(PingTestCommand cmd) {
        Connection conn = getConnection();
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

    private CheckS2SVpnConnectionsAnswer execute(CheckS2SVpnConnectionsCommand cmd) {
        Connection conn = getConnection();
        String args = "checkbatchs2svpn.sh " + cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);
        for (String ip : cmd.getVpnIps()) {
            args += " " + ip;
        }
        String result = callHostPlugin(conn, "vmops", "routerProxy", "args", args);
        if (result == null || result.isEmpty()) {
            return new CheckS2SVpnConnectionsAnswer(cmd, false, "CheckS2SVpnConneciontsCommand failed");
        }
        return new CheckS2SVpnConnectionsAnswer(cmd, true, result);
    }

    private CheckRouterAnswer execute(CheckRouterCommand cmd) {
        Connection conn = getConnection();
        String args = "checkrouter.sh " + cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);
        String result = callHostPlugin(conn, "vmops", "routerProxy", "args", args);
        if (result == null || result.isEmpty()) {
            return new CheckRouterAnswer(cmd, "CheckRouterCommand failed");
        }
        return new CheckRouterAnswer(cmd, result, true);
    }

    private GetDomRVersionAnswer execute(GetDomRVersionCmd cmd) {
        Connection conn = getConnection();
        String args = "get_template_version.sh " + cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);
        String result = callHostPlugin(conn, "vmops", "routerProxy", "args", args);
        if (result == null || result.isEmpty()) {
            return new GetDomRVersionAnswer(cmd, "getDomRVersionCmd failed");
        }
        String[] lines = result.split("&");
        if (lines.length != 2) {
            return new GetDomRVersionAnswer(cmd, result);
        }
        return new GetDomRVersionAnswer(cmd, result, lines[0], lines[1]);
    }

    private Answer execute(BumpUpPriorityCommand cmd) {
        Connection conn = getConnection();
        String args = cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);
        String result = callHostPlugin(conn, "vmops", "bumpUpPriority", "args", args);
        if (result == null || result.isEmpty()) {
            return new Answer(cmd, false, "BumpUpPriorityCommand failed");
        }
        return new Answer(cmd, true, result);
    }

    protected MaintainAnswer execute(MaintainCommand cmd) {
        Connection conn = getConnection();
        try {
            Pool pool = Pool.getByUuid(conn, _host.pool);
            Pool.Record poolr = pool.getRecord(conn);

            Host.Record hostr = poolr.master.getRecord(conn);
            if (!_host.uuid.equals(hostr.uuid)) {
                s_logger.debug("Not the master node so just return ok: " + _host.ip);
                return new MaintainAnswer(cmd);
            }
            Map<Host, Host.Record> hostMap = Host.getAllRecords(conn);
            if (hostMap.size() == 1) {
                s_logger.debug("There is the last host in pool " + poolr.uuid );
                return new MaintainAnswer(cmd);
            }
            Host newMaster = null;
            Host.Record newMasterRecord = null;
            for (Map.Entry<Host, Host.Record> entry : hostMap.entrySet()) {
                if (!_host.uuid.equals(entry.getValue().uuid)) {
                    newMaster = entry.getKey();
                    newMasterRecord = entry.getValue();
                    s_logger.debug("New master for the XenPool is " + newMasterRecord.uuid + " : " + newMasterRecord.address);
                    try {
                        _connPool.switchMaster(_host.ip, _host.pool, conn, newMaster, _username, _password, _wait);
                        return new MaintainAnswer(cmd, "New Master is " + newMasterRecord.address);
                    } catch (XenAPIException e) {
                        s_logger.warn("Unable to switch the new master to " + newMasterRecord.uuid + ": " + newMasterRecord.address + " Trying again...");
                    } catch (XmlRpcException e) {
                        s_logger.warn("Unable to switch the new master to " + newMasterRecord.uuid + ": " + newMasterRecord.address + " Trying again...");
                    }
                }
            }
            return new MaintainAnswer(cmd, false, "Unable to find an appropriate host to set as the new master");
        } catch (XenAPIException e) {
            s_logger.warn("Unable to put server in maintainence mode", e);
            return new MaintainAnswer(cmd, false, e.getMessage());
        } catch (XmlRpcException e) {
            s_logger.warn("Unable to put server in maintainence mode", e);
            return new MaintainAnswer(cmd, false, e.getMessage());
        }
    }

    protected SetPortForwardingRulesAnswer execute(SetPortForwardingRulesCommand cmd) {
        Connection conn = getConnection();

        String routerIp = cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);
        String[] results = new String[cmd.getRules().length];
        int i = 0;

        boolean endResult = true;
        for (PortForwardingRuleTO rule : cmd.getRules()) {
            StringBuilder args = new StringBuilder();
            args.append(routerIp);
            args.append(rule.revoked() ? " -D " : " -A ");
            args.append(" -P ").append(rule.getProtocol().toLowerCase());
            args.append(" -l ").append(rule.getSrcIp());
            args.append(" -p ").append(rule.getStringSrcPortRange());
            args.append(" -r ").append(rule.getDstIp());
            args.append(" -d ").append(rule.getStringDstPortRange());

            String result = callHostPlugin(conn, "vmops", "setFirewallRule", "args", args.toString());

            if (result == null || result.isEmpty()) {
                results[i++] = "Failed";
                endResult = false;
            } else {
                results[i++] = null;
            }
        }

        return new SetPortForwardingRulesAnswer(cmd, results, endResult);
    }

    protected SetStaticNatRulesAnswer SetVPCStaticNatRules(SetStaticNatRulesCommand cmd) {
        Connection conn = getConnection();
        String routerIp = cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);
        //String args = routerIp;
        String[] results = new String[cmd.getRules().length];
        int i = 0;
        boolean endResult = true;
        for (StaticNatRuleTO rule : cmd.getRules()) {
            String args = "vpc_staticnat.sh " + routerIp;
            args += rule.revoked() ? " -D" : " -A";
            args += " -l " + rule.getSrcIp();
            args += " -r " + rule.getDstIp();
            String result = callHostPlugin(conn, "vmops", "routerProxy", "args", args.toString());

            if (result == null || result.isEmpty()) {
                results[i++] = "Failed";
                endResult = false;
            } else {
                results[i++] = null;
            }
        }
        return new SetStaticNatRulesAnswer(cmd, results, endResult);
    }

    protected SetStaticNatRulesAnswer execute(SetStaticNatRulesCommand cmd) {
        if ( cmd.getVpcId() != null ) {
            return SetVPCStaticNatRules(cmd);
        }
        Connection conn = getConnection();

        String routerIp = cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);
        //String args = routerIp;
        String[] results = new String[cmd.getRules().length];
        int i = 0;
        boolean endResult = true;
        for (StaticNatRuleTO rule : cmd.getRules()) {
            //1:1 NAT needs instanceip;publicip;domrip;op
            StringBuilder args = new StringBuilder();
            args.append(routerIp);
            args.append(rule.revoked() ? " -D " : " -A ");
            args.append(" -l ").append(rule.getSrcIp());
            args.append(" -r ").append(rule.getDstIp());

            if (rule.getProtocol() != null) {
                args.append(" -P ").append(rule.getProtocol().toLowerCase());
            }

            args.append(" -d ").append(rule.getStringSrcPortRange());
            args.append(" -G ");

            String result = callHostPlugin(conn, "vmops", "setFirewallRule", "args", args.toString());

            if (result == null || result.isEmpty()) {
                results[i++] = "Failed";
                endResult = false;
            } else {
                results[i++] = null;
            }
        }

        return new SetStaticNatRulesAnswer(cmd, results, endResult);
    }

    protected Answer VPCLoadBalancerConfig(final LoadBalancerConfigCommand cmd) {
        Connection conn = getConnection();
        String routerIp = cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);

        if (routerIp == null) {
            return new Answer(cmd);
        }

        LoadBalancerConfigurator cfgtr = new HAProxyConfigurator();
        String[] config = cfgtr.generateConfiguration(cmd);
        String tmpCfgFileContents = "";
        for (int i = 0; i < config.length; i++) {
            tmpCfgFileContents += config[i];
            tmpCfgFileContents += "\n";
        }
        String tmpCfgFilePath = "/etc/haproxy/haproxy.cfg.new";
        String result = callHostPlugin(conn, "vmops", "createFileInDomr", "domrip", routerIp, "filepath", tmpCfgFilePath, "filecontents", tmpCfgFileContents);

        if (result == null || result.isEmpty()) {
            return new Answer(cmd, false, "LoadBalancerConfigCommand failed to create HA proxy cfg file.");
        }

        String[][] rules = cfgtr.generateFwRules(cmd);

        String[] addRules = rules[LoadBalancerConfigurator.ADD];
        String[] removeRules = rules[LoadBalancerConfigurator.REMOVE];
        String[] statRules = rules[LoadBalancerConfigurator.STATS];

        String args = "vpc_loadbalancer.sh " + routerIp;
        String ip = cmd.getNic().getIp();
        args += " -i " + ip;
        StringBuilder sb = new StringBuilder();
        if (addRules.length > 0) {
            for (int i = 0; i < addRules.length; i++) {
                sb.append(addRules[i]).append(',');
            }

            args += " -a " + sb.toString();
        }

        sb = new StringBuilder();
        if (removeRules.length > 0) {
            for (int i = 0; i < removeRules.length; i++) {
                sb.append(removeRules[i]).append(',');
            }

            args += " -d " + sb.toString();
        }

        sb = new StringBuilder();
        if (statRules.length > 0) {
            for (int i = 0; i < statRules.length; i++) {
                sb.append(statRules[i]).append(',');
            }

            args += " -s " + sb.toString();
        }

        result = callHostPlugin(conn, "vmops", "routerProxy", "args", args);

        if (result == null || result.isEmpty()) {
            return new Answer(cmd, false, "LoadBalancerConfigCommand failed");
        }
        return new Answer(cmd);
    }

    protected Answer execute(final LoadBalancerConfigCommand cmd) {
        if ( cmd.getVpcId() != null ) {
            return VPCLoadBalancerConfig(cmd);
        }
        Connection conn = getConnection();
        String routerIp = cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);

        if (routerIp == null) {
            return new Answer(cmd);
        }

        LoadBalancerConfigurator cfgtr = new HAProxyConfigurator();
        String[] config = cfgtr.generateConfiguration(cmd);

        String[][] rules = cfgtr.generateFwRules(cmd);
        String tmpCfgFilePath = "/tmp/" + routerIp.replace('.', '_') + ".cfg";
        String tmpCfgFileContents = "";
        for (int i = 0; i < config.length; i++) {
            tmpCfgFileContents += config[i];
            tmpCfgFileContents += "\n";
        }

        String result = callHostPlugin(conn, "vmops", "createFile", "filepath", tmpCfgFilePath, "filecontents", tmpCfgFileContents);

        if (result == null || result.isEmpty()) {
            return new Answer(cmd, false, "LoadBalancerConfigCommand failed to create HA proxy cfg file.");
        }

        String[] addRules = rules[LoadBalancerConfigurator.ADD];
        String[] removeRules = rules[LoadBalancerConfigurator.REMOVE];
        String[] statRules = rules[LoadBalancerConfigurator.STATS];

        String args = "";
        args += "-i " + routerIp;
        args += " -f " + tmpCfgFilePath;

        StringBuilder sb = new StringBuilder();
        if (addRules.length > 0) {
            for (int i = 0; i < addRules.length; i++) {
                sb.append(addRules[i]).append(',');
            }

            args += " -a " + sb.toString();
        }

        sb = new StringBuilder();
        if (removeRules.length > 0) {
            for (int i = 0; i < removeRules.length; i++) {
                sb.append(removeRules[i]).append(',');
            }

            args += " -d " + sb.toString();
        }

        sb = new StringBuilder();
        if (statRules.length > 0) {
            for (int i = 0; i < statRules.length; i++) {
                sb.append(statRules[i]).append(',');
            }

            args += " -s " + sb.toString();
        }

        result = callHostPlugin(conn, "vmops", "setLoadBalancerRule", "args", args);

        if (result == null || result.isEmpty()) {
            return new Answer(cmd, false, "LoadBalancerConfigCommand failed");
        }

        callHostPlugin(conn, "vmops", "deleteFile", "filepath", tmpCfgFilePath);

        return new Answer(cmd);
    }

    protected synchronized Answer execute(final DhcpEntryCommand cmd) {
        Connection conn = getConnection();
        String args = "-r " + cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);
        if (cmd.getVmIpAddress() != null) {
        args += " -v " + cmd.getVmIpAddress();
        }
        args += " -m " + cmd.getVmMac();
        args += " -n " + cmd.getVmName();
        if (cmd.getDefaultRouter() != null) {
            args += " -d " + cmd.getDefaultRouter();
        }
        if (cmd.getStaticRoutes() != null) {
            args += " -s " + cmd.getStaticRoutes();
        }

        if (cmd.getDefaultDns() != null) {
            args += " -N " + cmd.getDefaultDns();
        }

        if (cmd.getVmIp6Address() != null) {
        	args += " -6 " + cmd.getVmIp6Address();
        	args += " -u " + cmd.getDuid();
        }
        
        if (!cmd.isDefault()) {
        	args += " -z";
        }
        
        String result = callHostPlugin(conn, "vmops", "saveDhcpEntry", "args", args);
        if (result == null || result.isEmpty()) {
            return new Answer(cmd, false, "DhcpEntry failed");
        }
        return new Answer(cmd);
    }

    protected synchronized Answer execute(final RemoteAccessVpnCfgCommand cmd) {
        Connection conn = getConnection();
        String args = "vpn_l2tp.sh " + cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);
        if (cmd.isCreate()) {
            args += " -r " + cmd.getIpRange();
            args += " -p " + cmd.getPresharedKey();
            args += " -s " + cmd.getVpnServerIp();
            args += " -l " + cmd.getLocalIp();
            args += " -c ";

        } else {
            args += " -d ";
            args += " -s " + cmd.getVpnServerIp();
        }
        String result = callHostPlugin(conn, "vmops", "routerProxy", "args", args);
        if (result == null || result.isEmpty()) {
            return new Answer(cmd, false, "Configure VPN failed");
        }
        return new Answer(cmd);
    }

    protected synchronized Answer execute(final VpnUsersCfgCommand cmd) {
        Connection conn = getConnection();
        for (VpnUsersCfgCommand.UsernamePassword userpwd: cmd.getUserpwds()) {
            String args = "vpn_l2tp.sh " + cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);
            if (!userpwd.isAdd()) {
                args += " -U " + userpwd.getUsername();
            } else {
                args += " -u " + userpwd.getUsernamePassword();
            }
            String result = callHostPlugin(conn, "vmops", "routerProxy", "args", args);
            if (result == null || result.isEmpty()) {
                return new Answer(cmd, false, "Configure VPN user failed for user " + userpwd.getUsername());
            }
        }

        return new Answer(cmd);
    }

    protected Answer execute(final VmDataCommand cmd) {
        Connection conn = getConnection();
        String routerPrivateIpAddress = cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);
        String vmIpAddress = cmd.getVmIpAddress();
        List<String[]> vmData = cmd.getVmData();
        String[] vmDataArgs = new String[vmData.size() * 2 + 4];
        vmDataArgs[0] = "routerIP";
        vmDataArgs[1] = routerPrivateIpAddress;
        vmDataArgs[2] = "vmIP";
        vmDataArgs[3] = vmIpAddress;
        int i = 4;
        for (String[] vmDataEntry : vmData) {
            String folder = vmDataEntry[0];
            String file = vmDataEntry[1];
            String contents = (vmDataEntry[2] != null) ? vmDataEntry[2] : "none";

            vmDataArgs[i] = folder + "," + file;
            vmDataArgs[i + 1] = contents;
            i += 2;
        }

        String result = callHostPlugin(conn, "vmops", "vm_data", vmDataArgs);

        if (result == null || result.isEmpty()) {
            return new Answer(cmd, false, "vm_data failed");
        } else {
            return new Answer(cmd);
        }

    }

    protected Answer execute(final SavePasswordCommand cmd) {
        Connection conn = getConnection();
        final String password = cmd.getPassword();
        final String routerPrivateIPAddress = cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);
        final String vmName = cmd.getVmName();
        final String vmIpAddress = cmd.getVmIpAddress();
        final String local = vmName;

        // Run save_password_to_domr.sh
        String args = "-r " + routerPrivateIPAddress;
        args += " -v " + vmIpAddress;
        args += " -p " + password;
        args += " " + local;
        String result = callHostPlugin(conn, "vmops", "savePassword", "args", args);

        if (result == null || result.isEmpty()) {
            return new Answer(cmd, false, "savePassword failed");
        }
        return new Answer(cmd);
    }


    protected void assignPublicIpAddress(Connection conn, String vmName, String privateIpAddress, String publicIpAddress, boolean add, boolean firstIP,
            boolean sourceNat, String vlanId, String vlanGateway, String vlanNetmask, String vifMacAddress, Integer networkRate, TrafficType trafficType, String name) throws InternalErrorException {

        try {
            VM router = getVM(conn, vmName);

            NicTO nic = new NicTO();
            nic.setMac(vifMacAddress);
            nic.setType(trafficType);
            if (vlanId == null) {
                nic.setBroadcastType(BroadcastDomainType.Native);
            } else {
                nic.setBroadcastType(BroadcastDomainType.Vlan);
                nic.setBroadcastUri(BroadcastDomainType.Vlan.toUri(vlanId));
            }
            nic.setDeviceId(0);
            nic.setNetworkRateMbps(networkRate);
            nic.setName(name);

            Network network = getNetwork(conn, nic);

            // Determine the correct VIF on DomR to associate/disassociate the
            // IP address with
            VIF correctVif = getCorrectVif(conn, router, network);

            // If we are associating an IP address and DomR doesn't have a VIF
            // for the specified vlan ID, we need to add a VIF
            // If we are disassociating the last IP address in the VLAN, we need
            // to remove a VIF
            boolean addVif = false;
            boolean removeVif = false;
            if (add && correctVif == null) {
                addVif = true;
            }

            if (addVif) {
                // Add a new VIF to DomR
                String vifDeviceNum = getLowestAvailableVIFDeviceNum(conn, router);

                if (vifDeviceNum == null) {
                    throw new InternalErrorException("There were no more available slots for a new VIF on router: " + router.getNameLabel(conn));
                }

                nic.setDeviceId(Integer.parseInt(vifDeviceNum));

                correctVif = createVif(conn, vmName, router, nic);
                correctVif.plug(conn);
                // Add iptables rule for network usage
                networkUsage(conn, privateIpAddress, "addVif", "eth" + correctVif.getDevice(conn));
            }

            if (correctVif == null) {
                throw new InternalErrorException("Failed to find DomR VIF to associate/disassociate IP with.");
            }

            String args = "ipassoc.sh " + privateIpAddress;

            if (add) {
                args += " -A ";
            } else {
                args += " -D ";
            }

            if (sourceNat) {
                args += " -s";
            }
            if (firstIP) {
                args += " -f";
            }

            String cidrSize = Long.toString(NetUtils.getCidrSize(vlanNetmask));
            args += " -l ";
            args += publicIpAddress + "/" + cidrSize;

            args += " -c ";
            args += "eth" + correctVif.getDevice(conn);

            args += " -g ";
            args += vlanGateway;


            String result = callHostPlugin(conn, "vmops", "routerProxy", "args", args);
            if (result == null || result.isEmpty()) {
                throw new InternalErrorException("Xen plugin \"ipassoc\" failed.");
            }

            if (removeVif) {
                network = correctVif.getNetwork(conn);

                // Mark this vif to be removed from network usage
                networkUsage(conn, privateIpAddress, "deleteVif", "eth" + correctVif.getDevice(conn));

                // Remove the VIF from DomR
                correctVif.unplug(conn);
                correctVif.destroy(conn);

                // Disable the VLAN network if necessary
                disableVlanNetwork(conn, network);
            }

        } catch (XenAPIException e) {
            String msg = "Unable to assign public IP address due to " + e.toString();
            s_logger.warn(msg, e);
            throw new InternalErrorException(msg);
        } catch (final XmlRpcException e) {
            String msg = "Unable to assign public IP address due to " + e.getMessage();
            s_logger.warn(msg, e);
            throw new InternalErrorException(msg);
        }
    }

    protected void assignVPCPublicIpAddress(Connection conn, String vmName, String routerIp, IpAddressTO ip) throws Exception {

        try {
            VM router = getVM(conn, vmName);

            VIF correctVif = getVifByMac(conn, router, ip.getVifMacAddress());
            if (correctVif == null) {
                if (ip.isAdd()) {
                    throw new InternalErrorException("Failed to find DomR VIF to associate IP with.");
                } else {
                    s_logger.debug("VIF to deassociate IP with does not exist, return success");
                    return;
                }
            }           

            String args = "vpc_ipassoc.sh " + routerIp;

            if (ip.isAdd()) {
                args += " -A ";
            } else {
                args += " -D ";
            }

            args += " -l ";
            args += ip.getPublicIp();

            args += " -c ";
            args += "eth" + correctVif.getDevice(conn);

            args += " -g ";
            args += ip.getVlanGateway();

            args += " -m ";
            args += Long.toString(NetUtils.getCidrSize(ip.getVlanNetmask()));


            args += " -n ";
            args += NetUtils.getSubNet(ip.getPublicIp(), ip.getVlanNetmask());

            String result = callHostPlugin(conn, "vmops", "routerProxy", "args", args);
            if (result == null || result.isEmpty()) {
                throw new InternalErrorException("Xen plugin \"vpc_ipassoc\" failed.");
            }
        } catch (Exception e) {
            String msg = "Unable to assign public IP address due to " + e.toString();
            s_logger.warn(msg, e);
            throw new Exception(msg);
        }
    }

    protected String networkUsage(Connection conn, final String privateIpAddress, final String option, final String vif) {

        if (option.equals("get")) {
            return "0:0";
        }
        return null;
    }

    protected Answer execute(IpAssocCommand cmd) {
        Connection conn = getConnection();
        String[] results = new String[cmd.getIpAddresses().length];
        int i = 0;
        String routerName = cmd.getAccessDetail(NetworkElementCommand.ROUTER_NAME);
        String routerIp = cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);
        try {
            IpAddressTO[] ips = cmd.getIpAddresses();
            for (IpAddressTO ip : ips) {

                assignPublicIpAddress(conn, routerName, routerIp, ip.getPublicIp(), ip.isAdd(), ip.isFirstIP(), ip.isSourceNat(), ip.getVlanId(),
                        ip.getVlanGateway(), ip.getVlanNetmask(), ip.getVifMacAddress(), ip.getNetworkRate(), ip.getTrafficType(), ip.getNetworkName());
                results[i++] = ip.getPublicIp() + " - success";
            }
        } catch (InternalErrorException e) {
            s_logger.error(
                    "Ip Assoc failure on applying one ip due to exception:  ", e);
            results[i++] = IpAssocAnswer.errorResult;
        }

        return new IpAssocAnswer(cmd, results);
    }

    protected GetVncPortAnswer execute(GetVncPortCommand cmd) {
        Connection conn = getConnection();
        try {
            Set<VM> vms = VM.getByNameLabel(conn, cmd.getName());
            if(vms.size() == 1) {
                int vncport = getVncPort(conn, vms.iterator().next());
                String consoleurl;
                consoleurl = "consoleurl=" +getVncUrl(conn, vms.iterator().next()) + "&" +"sessionref="+ conn.getSessionReference();
                return new GetVncPortAnswer(cmd, consoleurl, vncport);
            } else {
                return new GetVncPortAnswer(cmd, "There are " + vms.size() + " VMs named " + cmd.getName());
            }
        } catch (Exception e) {
            String msg = "Unable to get vnc port due to " + e.toString();
            s_logger.warn(msg, e);
            return new GetVncPortAnswer(cmd, msg);
        }
    }

    protected Storage.StorageResourceType getStorageResourceType() {
        return Storage.StorageResourceType.STORAGE_POOL;
    }

    protected CheckHealthAnswer execute(CheckHealthCommand cmd) {
        boolean result = pingXenServer();
        return new CheckHealthAnswer(cmd, result);
    }


    protected long[] getNetworkStats(Connection conn, String privateIP) {
        String result = networkUsage(conn, privateIP, "get", null);
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

    /**
     * This is the method called for getting the HOST stats
     * 
     * @param cmd
     * @return
     */
    protected GetHostStatsAnswer execute(GetHostStatsCommand cmd) {
        Connection conn = getConnection();
        try {
            HostStatsEntry hostStats = getHostStats(conn, cmd, cmd.getHostGuid(), cmd.getHostId());
            return new GetHostStatsAnswer(cmd, hostStats);
        } catch (Exception e) {
            String msg = "Unable to get Host stats" + e.toString();
            s_logger.warn(msg, e);
            return new GetHostStatsAnswer(cmd, null);
        }
    }

    protected HostStatsEntry getHostStats(Connection conn, GetHostStatsCommand cmd, String hostGuid, long hostId) {

        HostStatsEntry hostStats = new HostStatsEntry(hostId, 0, 0, 0, "host", 0, 0, 0, 0);
        Object[] rrdData = getRRDData(conn, 1); // call rrd method with 1 for host

        if (rrdData == null) {
            return null;
        }

        Integer numRows = (Integer) rrdData[0];
        Integer numColumns = (Integer) rrdData[1];
        Node legend = (Node) rrdData[2];
        Node dataNode = (Node) rrdData[3];

        NodeList legendChildren = legend.getChildNodes();
        for (int col = 0; col < numColumns; col++) {

            if (legendChildren == null || legendChildren.item(col) == null) {
                continue;
            }

            String columnMetadata = getXMLNodeValue(legendChildren.item(col));

            if (columnMetadata == null) {
                continue;
            }

            String[] columnMetadataList = columnMetadata.split(":");

            if (columnMetadataList.length != 4) {
                continue;
            }

            String type = columnMetadataList[1];
            String param = columnMetadataList[3];

            if (type.equalsIgnoreCase("host")) {

                if (param.contains("pif_eth0_rx")) {
                    hostStats.setNetworkReadKBs(getDataAverage(dataNode, col, numRows));
                }

                if (param.contains("pif_eth0_tx")) {
                    hostStats.setNetworkWriteKBs(getDataAverage(dataNode, col, numRows));
                }

                if (param.contains("memory_total_kib")) {
                    hostStats.setTotalMemoryKBs(getDataAverage(dataNode, col, numRows));
                }

                if (param.contains("memory_free_kib")) {
                    hostStats.setFreeMemoryKBs(getDataAverage(dataNode, col, numRows));
                }

                if (param.contains("cpu")) {
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

    protected GetVmStatsAnswer execute( GetVmStatsCommand cmd) {
        Connection conn = getConnection();
        List<String> vmNames = cmd.getVmNames();
        HashMap<String, VmStatsEntry> vmStatsNameMap = new HashMap<String, VmStatsEntry>();
        if( vmNames.size() == 0 ) {
            return new GetVmStatsAnswer(cmd, vmStatsNameMap);
        }
        try {

            // Determine the UUIDs of the requested VMs
            List<String> vmUUIDs = new ArrayList<String>();

            for (String vmName : vmNames) {
                VM vm = getVM(conn, vmName);
                vmUUIDs.add(vm.getUuid(conn));
            }

            HashMap<String, VmStatsEntry> vmStatsUUIDMap = getVmStats(conn, cmd, vmUUIDs, cmd.getHostGuid());
            if( vmStatsUUIDMap == null ) {
                return new GetVmStatsAnswer(cmd, vmStatsNameMap);
            }

            for (String vmUUID : vmStatsUUIDMap.keySet()) {
                vmStatsNameMap.put(vmNames.get(vmUUIDs.indexOf(vmUUID)), vmStatsUUIDMap.get(vmUUID));
            }

            return new GetVmStatsAnswer(cmd, vmStatsNameMap);
        } catch (XenAPIException e) {
            String msg = "Unable to get VM stats" + e.toString();
            s_logger.warn(msg, e);
            return new GetVmStatsAnswer(cmd, vmStatsNameMap);
        } catch (XmlRpcException e) {
            String msg = "Unable to get VM stats" + e.getMessage();
            s_logger.warn(msg, e);
            return new GetVmStatsAnswer(cmd, vmStatsNameMap);
        }
    }

    protected HashMap<String, VmStatsEntry> getVmStats(Connection conn, GetVmStatsCommand cmd, List<String> vmUUIDs, String hostGuid) {
        HashMap<String, VmStatsEntry> vmResponseMap = new HashMap<String, VmStatsEntry>();

        for (String vmUUID : vmUUIDs) {
            vmResponseMap.put(vmUUID, new VmStatsEntry(0, 0, 0, 0, "vm"));
        }

        Object[] rrdData = getRRDData(conn, 2); // call rrddata with 2 for vm

        if (rrdData == null) {
            return null;
        }

        Integer numRows = (Integer) rrdData[0];
        Integer numColumns = (Integer) rrdData[1];
        Node legend = (Node) rrdData[2];
        Node dataNode = (Node) rrdData[3];

        NodeList legendChildren = legend.getChildNodes();
        for (int col = 0; col < numColumns; col++) {

            if (legendChildren == null || legendChildren.item(col) == null) {
                continue;
            }

            String columnMetadata = getXMLNodeValue(legendChildren.item(col));

            if (columnMetadata == null) {
                continue;
            }

            String[] columnMetadataList = columnMetadata.split(":");

            if (columnMetadataList.length != 4) {
                continue;
            }

            String type = columnMetadataList[1];
            String uuid = columnMetadataList[2];
            String param = columnMetadataList[3];

            if (type.equals("vm") && vmResponseMap.keySet().contains(uuid)) {
                VmStatsEntry vmStatsAnswer = vmResponseMap.get(uuid);

                vmStatsAnswer.setEntityType("vm");

                if (param.contains("cpu")) {
                    vmStatsAnswer.setNumCPUs(vmStatsAnswer.getNumCPUs() + 1);
                    vmStatsAnswer.setCPUUtilization(((vmStatsAnswer.getCPUUtilization() + getDataAverage(dataNode, col, numRows))));
                } else if (param.matches("vif_\\d_rx")) {
                    vmStatsAnswer.setNetworkReadKBs(vmStatsAnswer.getNetworkReadKBs() + (getDataAverage(dataNode, col, numRows)/(8*2)));
                } else if (param.matches("vif_\\d_tx")) {
                    vmStatsAnswer.setNetworkWriteKBs(vmStatsAnswer.getNetworkWriteKBs() + (getDataAverage(dataNode, col, numRows)/(8*2)));
                }
            }

        }

        for (String vmUUID : vmResponseMap.keySet()) {
            VmStatsEntry vmStatsAnswer = vmResponseMap.get(vmUUID);

            if (vmStatsAnswer.getNumCPUs() != 0) {
                vmStatsAnswer.setCPUUtilization(vmStatsAnswer.getCPUUtilization() / vmStatsAnswer.getNumCPUs());
            }

            vmStatsAnswer.setCPUUtilization(vmStatsAnswer.getCPUUtilization()*100);
            if(s_logger.isDebugEnabled()) {
                s_logger.debug("Vm cpu utilization " + vmStatsAnswer.getCPUUtilization());
            }
        }

        return vmResponseMap;
    }

    protected Object[] getRRDData(Connection conn, int flag) {

        /*
         * Note: 1 => called from host, hence host stats 2 => called from vm, hence vm stats
         */
        String stats = "";

        try {
            if (flag == 1) {
                stats = getHostStatsRawXML(conn);
            }
            if (flag == 2) {
                stats = getVmStatsRawXML(conn);
            }
        } catch (Exception e1) {
            s_logger.warn("Error whilst collecting raw stats from plugin: ", e1);
            return null;
        }

        // s_logger.debug("The raw xml stream is:"+stats);
        // s_logger.debug("Length of raw xml is:"+stats.length());

        //stats are null when the host plugin call fails (host down state)
        if(stats == null) {
            return null;
        }

        StringReader statsReader = new StringReader(stats);
        InputSource statsSource = new InputSource(statsReader);

        Document doc = null;
        try {
            doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(statsSource);
        } catch (Exception e) {
            s_logger.warn("Exception caught whilst processing the document via document factory:", e);
            return null;
        }

        if(doc==null){
            s_logger.warn("Null document found after tryinh to parse the stats source");
            return null;
        }

        NodeList firstLevelChildren = doc.getChildNodes();
        NodeList secondLevelChildren = (firstLevelChildren.item(0)).getChildNodes();
        Node metaNode = secondLevelChildren.item(0);
        Node dataNode = secondLevelChildren.item(1);

        Integer numRows = 0;
        Integer numColumns = 0;
        Node legend = null;
        NodeList metaNodeChildren = metaNode.getChildNodes();
        for (int i = 0; i < metaNodeChildren.getLength(); i++) {
            Node n = metaNodeChildren.item(i);
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

    protected String getXMLNodeValue(Node n) {
        return n.getChildNodes().item(0).getNodeValue();
    }

    protected double getDataAverage(Node dataNode, int col, int numRows) {
        double value = 0;
        double dummy = 0;
        int numRowsUsed = 0;
        for (int row = 0; row < numRows; row++) {
            Node data = dataNode.getChildNodes().item(numRows - 1 - row).getChildNodes().item(col + 1);
            Double currentDataAsDouble = Double.valueOf(getXMLNodeValue(data));
            if (!currentDataAsDouble.equals(Double.NaN)) {
                numRowsUsed += 1;
                value += currentDataAsDouble;
            }
        }

        if(numRowsUsed == 0)
        {
            if((!Double.isInfinite(value))&&(!Double.isNaN(value)))
            {
                return value;
            }
            else
            {
                s_logger.warn("Found an invalid value (infinity/NaN) in getDataAverage(), numRows=0");
                return dummy;
            }
        }
        else
        {
            if((!Double.isInfinite(value/numRowsUsed))&&(!Double.isNaN(value/numRowsUsed)))
            {
                return (value/numRowsUsed);
            }
            else
            {
                s_logger.warn("Found an invalid value (infinity/NaN) in getDataAverage(), numRows>0");
                return dummy;
            }
        }

    }

    protected String getHostStatsRawXML(Connection conn) {
        Date currentDate = new Date();
        String startTime = String.valueOf(currentDate.getTime() / 1000 - 1000);

        return callHostPlugin(conn, "vmops", "gethostvmstats", "collectHostStats", String.valueOf("true"), "consolidationFunction", _consolidationFunction, "interval", String
                .valueOf(_pollingIntervalInSeconds), "startTime", startTime);
    }

    protected String getVmStatsRawXML(Connection conn) {
        Date currentDate = new Date();
        String startTime = String.valueOf(currentDate.getTime() / 1000 - 1000);

        return callHostPlugin(conn, "vmops", "gethostvmstats", "collectHostStats", String.valueOf("false"), "consolidationFunction", _consolidationFunction, "interval", String
                .valueOf(_pollingIntervalInSeconds), "startTime", startTime);
    }

    protected State convertToState(Types.VmPowerState ps) {
        final State state = s_statesTable.get(ps);
        return state == null ? State.Unknown : state;
    }

    protected HashMap<String, Pair<String, State>> getAllVms(Connection conn) {
        final HashMap<String, Pair<String, State>> vmStates = new HashMap<String, Pair<String, State>>();
        Map<VM, VM.Record>  vm_map = null;
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
            return null;
        }
        for (VM.Record record: vm_map.values()) {
            if (record.isControlDomain || record.isASnapshot || record.isATemplate) {
                continue; // Skip DOM0
            }

            VmPowerState ps = record.powerState;
            final State state = convertToState(ps);
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("VM " + record.nameLabel + ": powerstate = " + ps + "; vm state=" + state.toString());
            }
            Host host = record.residentOn;
            String host_uuid = null;
            if( ! isRefNull(host) ) {
                try {
                    host_uuid = host.getUuid(conn);
                } catch (BadServerResponse e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (XenAPIException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (XmlRpcException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                vmStates.put(record.nameLabel, new Pair<String, State>(host_uuid, state));
            }
        }

        return vmStates;
    }

    protected State getVmState(Connection conn, final String vmName) {
        int retry = 3;
        while (retry-- > 0) {
            try {
                Set<VM> vms = VM.getByNameLabel(conn, vmName);
                for (final VM vm : vms) {
                    return convertToState(vm.getPowerState(conn));
                }
            } catch (final BadServerResponse e) {
                // There is a race condition within xen such that if a vm is
                // deleted and we
                // happen to ask for it, it throws this stupid response. So
                // if this happens,
                // we take a nap and try again which then avoids the race
                // condition because
                // the vm's information is now cleaned up by xen. The error
                // is as follows
                // com.xensource.xenapi.Types$BadServerResponse
                // [HANDLE_INVALID, VM,
                // 3dde93f9-c1df-55a7-2cde-55e1dce431ab]
                s_logger.info("Unable to get a vm PowerState due to " + e.toString() + ". We are retrying.  Count: " + retry);
                try {
                    Thread.sleep(3000);
                } catch (final InterruptedException ex) {

                }
            } catch (XenAPIException e) {
                String msg = "Unable to get a vm PowerState due to " + e.toString();
                s_logger.warn(msg, e);
                break;
            } catch (final XmlRpcException e) {
                String msg = "Unable to get a vm PowerState due to " + e.getMessage();
                s_logger.warn(msg, e);
                break;
            }
        }

        return State.Stopped;
    }

    protected CheckVirtualMachineAnswer execute(final CheckVirtualMachineCommand cmd) {
        Connection conn = getConnection();
        final String vmName = cmd.getVmName();
        final State state = getVmState(conn, vmName);
        Integer vncPort = null;
        if (state == State.Running) {
            synchronized (_cluster.intern()) {
                s_vms.put(_cluster, _name, vmName, State.Running);
            }
            s_logger.debug("3. The VM " + vmName + " is in Running state");
        }

        return new CheckVirtualMachineAnswer(cmd, state, vncPort);
    }

    protected PrepareForMigrationAnswer execute(PrepareForMigrationCommand cmd) {
        Connection conn = getConnection();

        VirtualMachineTO vm = cmd.getVirtualMachine();
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Preparing host for migrating " + vm);
        }

        NicTO[] nics = vm.getNics();
        try {
            prepareISO(conn, vm.getName());

            for (NicTO nic : nics) {
                getNetwork(conn, nic);
            }
            synchronized (_cluster.intern()) {
                s_vms.put(_cluster, _name, vm.getName(), State.Migrating);
            }
            s_logger.debug("4. The VM " +  vm.getName() + " is in Migrating state");

            return new PrepareForMigrationAnswer(cmd);
        } catch (Exception e) {
            s_logger.warn("Catch Exception " + e.getClass().getName() + " prepare for migration failed due to " + e.toString(), e);
            return new PrepareForMigrationAnswer(cmd, e);
        }
    }

    private String copy_vhd_to_secondarystorage(Connection conn, String mountpoint, String vdiuuid, String sruuid, int wait) {
        String results = callHostPluginAsync(conn, "vmopspremium", "copy_vhd_to_secondarystorage",
                wait, "mountpoint", mountpoint, "vdiuuid", vdiuuid, "sruuid", sruuid);
        String errMsg = null;
        if (results == null || results.isEmpty()) {
            errMsg = "copy_vhd_to_secondarystorage return null";
        } else {
            String[] tmp = results.split("#");
            String status = tmp[0];
            if (status.equals("0")) {
                return tmp[1];
            } else {
                errMsg = tmp[1];
            }
        }
        String source = vdiuuid + ".vhd";
        killCopyProcess(conn, source);
        s_logger.warn(errMsg);
        throw new CloudRuntimeException(errMsg);
    }

    String upgradeSnapshot(Connection conn, String templatePath, String snapshotPath) {
        String results = callHostPluginAsync(conn, "vmopspremium", "upgrade_snapshot",
                2 * 60 * 60, "templatePath", templatePath, "snapshotPath", snapshotPath);

        if (results == null || results.isEmpty()) {
            String msg = "upgrade_snapshot return null";
            s_logger.warn(msg);
            throw new CloudRuntimeException(msg);
        }
        String[] tmp = results.split("#");
        String status = tmp[0];
        if (status.equals("0")) {
            return results;
        } else {
            s_logger.warn(results);
            throw new CloudRuntimeException(results);
        }
    }

    String createTemplateFromSnapshot(Connection conn, String templatePath, String snapshotPath, int wait) {
        String tmpltLocalDir = UUID.randomUUID().toString();
        String results = callHostPluginAsync(conn, "vmopspremium", "create_privatetemplate_from_snapshot",
                wait, "templatePath", templatePath, "snapshotPath", snapshotPath, "tmpltLocalDir", tmpltLocalDir);
        String errMsg = null;
        if (results == null || results.isEmpty()) {
            errMsg = "create_privatetemplate_from_snapshot return null";
        } else {
            String[] tmp = results.split("#");
            String status = tmp[0];
            if (status.equals("0")) {
                return results;
            } else {
                errMsg = "create_privatetemplate_from_snapshot failed due to " + tmp[1];
            }
        }
        String source = "cloud_mount/" + tmpltLocalDir;
        killCopyProcess(conn, source);
        s_logger.warn(errMsg);
        throw new CloudRuntimeException(errMsg);
    }

    boolean killCopyProcess(Connection conn, String nameLabel) {
        String results = callHostPluginAsync(conn, "vmops", "kill_copy_process",
                60, "namelabel", nameLabel);
        String errMsg = null;
        if (results == null || results.equals("false")) {
            errMsg = "kill_copy_process failed";
            s_logger.warn(errMsg);
            return false;
        } else {
            return true;
        }
    }

    void destroyVDIbyNameLabel(Connection conn, String nameLabel) {
        try {
            Set<VDI> vdis = VDI.getByNameLabel(conn, nameLabel);
            if ( vdis.size() != 1 ) {
                s_logger.warn("destoryVDIbyNameLabel failed due to there are " + vdis.size() + " VDIs with name " + nameLabel);
                return;
            }
            for (VDI vdi : vdis) {
                try {
                    vdi.destroy(conn);
                } catch (Exception e) {
                }
            }
        } catch (Exception e){
        }
    }

    String copy_vhd_from_secondarystorage(Connection conn, String mountpoint, String sruuid, int wait) {
        String nameLabel = "cloud-" + UUID.randomUUID().toString();
        String results = callHostPluginAsync(conn, "vmopspremium", "copy_vhd_from_secondarystorage",
                wait, "mountpoint", mountpoint, "sruuid", sruuid, "namelabel", nameLabel);
        String errMsg = null;
        if (results == null || results.isEmpty()) {
            errMsg = "copy_vhd_from_secondarystorage return null";
        } else {
            String[] tmp = results.split("#");
            String status = tmp[0];
            if (status.equals("0")) {
                return tmp[1];
            } else {
                errMsg = tmp[1];
            }
        }
        String source = mountpoint.substring(mountpoint.lastIndexOf('/') + 1);
        if( killCopyProcess(conn, source) ) {
            destroyVDIbyNameLabel(conn, nameLabel);
        }
        s_logger.warn(errMsg);
        throw new CloudRuntimeException(errMsg);
    }

    public PrimaryStorageDownloadAnswer execute(final PrimaryStorageDownloadCommand cmd) {
        String tmplturl = cmd.getUrl();
        String poolName = cmd.getPoolUuid();
        int wait = cmd.getWait();
        try {
            URI uri = new URI(tmplturl);
            String tmplpath = uri.getHost() + ":" + uri.getPath();
            Connection conn = getConnection();
            SR poolsr = null;
            Set<SR> srs = SR.getByNameLabel(conn, poolName);
            if (srs.size() != 1) {
                String msg = "There are " + srs.size() + " SRs with same name: " + poolName;
                s_logger.warn(msg);
                return new PrimaryStorageDownloadAnswer(msg);
            } else {
                poolsr = srs.iterator().next();
            }
            String pUuid = poolsr.getUuid(conn);
            boolean isISCSI = IsISCSI(poolsr.getType(conn));
            String uuid = copy_vhd_from_secondarystorage(conn, tmplpath, pUuid, wait);
            VDI tmpl = getVDIbyUuid(conn, uuid);
            VDI snapshotvdi = tmpl.snapshot(conn, new HashMap<String, String>());
            String snapshotUuid = snapshotvdi.getUuid(conn);
            snapshotvdi.setNameLabel(conn, "Template " + cmd.getName());
            String parentuuid = getVhdParent(conn, pUuid, snapshotUuid, isISCSI);
            VDI parent = getVDIbyUuid(conn, parentuuid);
            Long phySize = parent.getPhysicalUtilisation(conn);
            tmpl.destroy(conn);
            poolsr.scan(conn);
            try{
                Thread.sleep(5000);
            } catch (Exception e) {
            }
            return new PrimaryStorageDownloadAnswer(snapshotvdi.getUuid(conn), phySize);
        } catch (Exception e) {
            String msg = "Catch Exception " + e.getClass().getName() + " on host:" + _host.uuid + " for template: "
                    + tmplturl + " due to " + e.toString();
            s_logger.warn(msg, e);
            return new PrimaryStorageDownloadAnswer(msg);
        }
    }

    protected String removeSRSync(Connection conn, SR sr) {
        if (sr == null) {
            return null;
        }
        if (s_logger.isDebugEnabled()) {
            s_logger.debug(logX(sr, "Removing SR"));
        }
        long waittime = 0;
        try {
            Set<VDI> vdis = sr.getVDIs(conn);
            for (VDI vdi : vdis) {
                Map<java.lang.String, Types.VdiOperations> currentOperation = vdi.getCurrentOperations(conn);
                if (currentOperation == null || currentOperation.size() == 0) {
                    continue;
                }
                if (waittime >= 1800000) {
                    String msg = "This template is being used, try late time";
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
        } catch (XenAPIException e) {
            s_logger.warn(logX(sr, "Unable to get current opertions " + e.toString()), e);
        } catch (XmlRpcException e) {
            s_logger.warn(logX(sr, "Unable to get current opertions " + e.getMessage()), e);
        }
        String msg = "Remove SR failed";
        s_logger.warn(msg);
        return msg;

    }

    protected void removeSR(Connection conn, SR sr) {
        if (sr == null) {
            return;
        }
        if (s_logger.isDebugEnabled()) {
            s_logger.debug(logX(sr, "Removing SR"));
        }

        for (int i = 0; i < 2; i++) {
            try {
                Set<VDI> vdis = sr.getVDIs(conn);
                for (VDI vdi : vdis) {
                    vdi.forget(conn);
                }
                Set<PBD> pbds = sr.getPBDs(conn);
                for (PBD pbd : pbds) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug(logX(pbd, "Unplugging pbd"));
                    }
                    if (pbd.getCurrentlyAttached(conn)) {
                        pbd.unplug(conn);
                    }
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
                    s_logger.debug(logX(sr, "There are still pbd attached"));
                    if (s_logger.isTraceEnabled()) {
                        for (PBD pbd : pbds) {
                            s_logger.trace(logX(pbd, " Still attached"));
                        }
                    }
                }
            } catch (XenAPIException e) {
                s_logger.debug(logX(sr, "Catch XenAPIException: " + e.toString()));
            } catch (XmlRpcException e) {
                s_logger.debug(logX(sr, "Catch Exception: " + e.getMessage()));
            }
        }
        s_logger.warn(logX(sr, "Unable to remove SR"));
    }

    protected MigrateAnswer execute(final MigrateCommand cmd) {
        Connection conn = getConnection();
        final String vmName = cmd.getVmName();
        State state = null;

        state = s_vms.getState(_cluster, vmName);

        synchronized (_cluster.intern()) {
            s_vms.put(_cluster, _name, vmName, State.Stopping);
        }
        s_logger.debug("5. The VM " + vmName + " is in Stopping state");
        try {
            Set<VM> vms = VM.getByNameLabel(conn, vmName);

            String ipaddr = cmd.getDestinationIp();

            Set<Host> hosts = Host.getAll(conn);
            Host dsthost = null;
            for (Host host : hosts) {
                if (host.getAddress(conn).equals(ipaddr)) {
                    dsthost = host;
                    break;
                }
            }
            if ( dsthost == null ) {
                String msg = "Migration failed due to unable to find host " + ipaddr + " in XenServer pool " + _host.pool;
                s_logger.warn(msg);
                return new MigrateAnswer(cmd, false, msg, null);
            }
            for (VM vm : vms) {
                Set<VBD> vbds = vm.getVBDs(conn);
                for( VBD vbd : vbds) {
                    VBD.Record vbdRec = vbd.getRecord(conn);
                    if( vbdRec.type.equals(Types.VbdType.CD.toString()) && !vbdRec.empty ) {
                        vbd.eject(conn);
                        break;
                    }
                }
                migrateVM(conn, dsthost, vm, vmName);
                vm.setAffinity(conn, dsthost);
                state = State.Stopping;
            }
            return new MigrateAnswer(cmd, true, "migration succeeded", null);
        } catch (Exception e) {
            String msg = "Catch Exception " + e.getClass().getName() + ": Migration failed due to " + e.toString();
            s_logger.warn(msg, e);
            return new MigrateAnswer(cmd, false, msg, null);
        } finally {
            synchronized (_cluster.intern()) {
                s_vms.put(_cluster, _name, vmName, state);
            }
            s_logger.debug("6. The VM " + vmName + " is in " + state + " state");
        }

    }

    protected State getRealPowerState(Connection conn, String label) {
        int i = 0;
        s_logger.trace("Checking on the HALTED State");
        for (; i < 20; i++) {
            try {
                Set<VM> vms = VM.getByNameLabel(conn, label);
                if (vms == null || vms.size() == 0) {
                    continue;
                }

                VM vm = vms.iterator().next();

                VmPowerState vps = vm.getPowerState(conn);
                if (vps != null && vps != VmPowerState.HALTED && vps != VmPowerState.UNRECOGNIZED) {
                    return convertToState(vps);
                }
            } catch (XenAPIException e) {
                String msg = "Unable to get real power state due to " + e.toString();
                s_logger.warn(msg, e);
            } catch (XmlRpcException e) {
                String msg = "Unable to get real power state due to " + e.getMessage();
                s_logger.warn(msg, e);
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
        return State.Stopped;
    }

    protected Pair<VM, VM.Record> getControlDomain(Connection conn) throws XenAPIException, XmlRpcException {
        Host host = Host.getByUuid(conn, _host.uuid);
        Set<VM> vms = null;
        vms = host.getResidentVMs(conn);
        for (VM vm : vms) {
            if (vm.getIsControlDomain(conn)) {
                return new Pair<VM, VM.Record>(vm, vm.getRecord(conn));
            }
        }

        throw new CloudRuntimeException("Com'on no control domain?  What the crap?!#@!##$@");
    }


    protected ReadyAnswer execute(ReadyCommand cmd) {
        Connection conn = getConnection();
        Long dcId = cmd.getDataCenterId();
        // Ignore the result of the callHostPlugin. Even if unmounting the
        // snapshots dir fails, let Ready command
        // succeed.
        callHostPlugin(conn, "vmopsSnapshot", "unmountSnapshotsDir", "dcId", dcId.toString());

        setupLinkLocalNetwork(conn);
        // try to destroy CD-ROM device for all system VMs on this host
        try {
            Host host = Host.getByUuid(conn, _host.uuid);
            Set<VM> vms = host.getResidentVMs(conn);
            for ( VM vm : vms ) {
                destroyPatchVbd(conn, vm.getNameLabel(conn));
            }
        } catch (Exception e) {
        }
        try {
            boolean result = cleanupHaltedVms(conn);
            if (!result) {
                return new ReadyAnswer(cmd, "Unable to cleanup halted vms");
            }
        } catch (XenAPIException e) {
            s_logger.warn("Unable to cleanup halted vms", e);
            return new ReadyAnswer(cmd, "Unable to cleanup halted vms");
        } catch (XmlRpcException e) {
            s_logger.warn("Unable to cleanup halted vms", e);
            return new ReadyAnswer(cmd, "Unable to cleanup halted vms");
        }

        return new ReadyAnswer(cmd);
    }

    //
    // using synchronized on VM name in the caller does not prevent multiple
    // commands being sent against
    // the same VM, there will be a race condition here in finally clause and
    // the main block if
    // there are multiple requests going on
    //
    // Therefore, a lazy solution is to add a synchronized guard here
    protected int getVncPort(Connection conn, VM vm) {
        VM.Record record;
        try {
            record = vm.getRecord(conn);
            Set<Console> consoles = record.consoles;
            if (consoles.isEmpty()) {
                s_logger.warn("There are no Consoles available to the vm : " + record.nameDescription);
                return -1;
            }
            Iterator<Console> i = consoles.iterator();
        } catch (XenAPIException e) {
            String msg = "Unable to get vnc-port due to " + e.toString();
            s_logger.warn(msg, e);
            return -1;
        } catch (XmlRpcException e) {
            String msg = "Unable to get vnc-port due to " + e.getMessage();
            s_logger.warn(msg, e);
            return -1;
        }
        String hvm = "true";
        if (record.HVMBootPolicy.isEmpty()) {
            hvm = "false";
        }

        String vncport = callHostPlugin(conn, "vmops", "getvncport", "domID", record.domid.toString(), "hvm", hvm, "version", _host.product_version);
        if (vncport == null || vncport.isEmpty()) {
            return -1;
        }

        vncport = vncport.replace("\n", "");
        return NumbersUtil.parseInt(vncport, -1);
    }

    protected String getVncUrl(Connection conn, VM vm) {
        VM.Record record;
        Console c;
        try {
            record = vm.getRecord(conn);
            Set<Console> consoles = record.consoles;

            if (consoles.isEmpty()) {
                s_logger.warn("There are no Consoles available to the vm : " + record.nameDescription);
                return null;
            }
            Iterator<Console> i = consoles.iterator();
            while(i.hasNext()) {
                c = i.next();
                if(c.getProtocol(conn) == ConsoleProtocol.RFB)
                    return c.getLocation(conn);
            }
        } catch (XenAPIException e) {
            String msg = "Unable to get console url due to " + e.toString();
            s_logger.warn(msg, e);
            return null;
        } catch (XmlRpcException e) {
            String msg = "Unable to get console url due to " + e.getMessage();
            s_logger.warn(msg, e);
            return null;
        }
        return null;
    }

    @Override
    public RebootAnswer execute(RebootCommand cmd) {
        Connection conn = getConnection();
        synchronized (_cluster.intern()) {
            s_vms.put(_cluster, _name, cmd.getVmName(), State.Starting);
        }
        s_logger.debug("7. The VM " + cmd.getVmName() + " is in Starting state");
        try {
            Set<VM> vms = null;
            try {
                vms = VM.getByNameLabel(conn, cmd.getVmName());
            } catch (XenAPIException e0) {
                s_logger.debug("getByNameLabel failed " + e0.toString());
                return new RebootAnswer(cmd, "getByNameLabel failed " + e0.toString(), false);
            } catch (Exception e0) {
                s_logger.debug("getByNameLabel failed " + e0.getMessage());
                return new RebootAnswer(cmd, "getByNameLabel failed", false);
            }
            for (VM vm : vms) {
                try {
                    rebootVM(conn, vm, vm.getNameLabel(conn));
                } catch (Exception e) {
                    String msg = e.toString();
                    s_logger.warn(msg, e);
                    return new RebootAnswer(cmd, msg, false);
                }
            }
            return new RebootAnswer(cmd, "reboot succeeded", true);
        } finally {
            synchronized (_cluster.intern()) {
                s_vms.put(_cluster, _name, cmd.getVmName(), State.Running);
            }
            s_logger.debug("8. The VM " + cmd.getVmName() + " is in Running state");
        }
    }

    protected Answer execute(RebootRouterCommand cmd) {
        Connection conn = getConnection();
        RebootAnswer answer = execute((RebootCommand) cmd);
        if (answer.getResult()) {
            String cnct = connect(conn, cmd.getVmName(), cmd.getPrivateIpAddress());
            networkUsage(conn, cmd.getPrivateIpAddress(), "create", null);
            if (cnct == null) {
                return answer;
            } else {
                return new Answer(cmd, false, cnct);
            }
        }
        return answer;
    }



    protected void startvmfailhandle(Connection conn, VM vm, List<Ternary<SR, VDI, VolumeVO>> mounts) {
        if (vm != null) {
            try {

                if (vm.getPowerState(conn) == VmPowerState.RUNNING) {
                    try {
                        vm.hardShutdown(conn);
                    } catch (Exception e) {
                        String msg = "VM hardshutdown failed due to " + e.toString();
                        s_logger.warn(msg, e);
                    }
                }
                if (vm.getPowerState(conn) == VmPowerState.HALTED) {
                    try {
                        vm.destroy(conn);
                    } catch (Exception e) {
                        String msg = "VM destroy failed due to " + e.toString();
                        s_logger.warn(msg, e);
                    }
                }
            } catch (Exception e) {
                String msg = "VM getPowerState failed due to " + e.toString();
                s_logger.warn(msg, e);
            }
        }
        if (mounts != null) {
            for (Ternary<SR, VDI, VolumeVO> mount : mounts) {
                VDI vdi = mount.second();
                Set<VBD> vbds = null;
                try {
                    vbds = vdi.getVBDs(conn);
                } catch (Exception e) {
                    String msg = "VDI getVBDS failed due to " + e.toString();
                    s_logger.warn(msg, e);
                    continue;
                }
                for (VBD vbd : vbds) {
                    try {
                        vbd.unplug(conn);
                        vbd.destroy(conn);
                    } catch (Exception e) {
                        String msg = "VBD destroy failed due to " + e.toString();
                        s_logger.warn(msg, e);
                    }
                }
            }
        }
    }

    protected void setMemory(Connection conn, VM vm, long minMemsize, long maxMemsize) throws XmlRpcException, XenAPIException {
        vm.setMemoryLimits(conn, maxMemsize, maxMemsize, minMemsize, maxMemsize);
    }

    private void waitForTask(Connection c, Task task, long pollInterval, long timeout) throws XenAPIException, XmlRpcException {
        long beginTime = System.currentTimeMillis();
        while (task.getStatus(c) == Types.TaskStatusType.PENDING) {
            try {
                Thread.sleep(pollInterval);
            } catch (InterruptedException e) {
            }
            if( System.currentTimeMillis() - beginTime > timeout){
                String msg = "Async " + timeout/1000 + " seconds timeout for task " + task.toString();
                s_logger.warn(msg);
                task.cancel(c);
                throw new Types.BadAsyncResult(msg);
            }
        }
    }

    private void checkForSuccess(Connection c, Task task) throws XenAPIException, XmlRpcException {
        if (task.getStatus(c) == Types.TaskStatusType.SUCCESS) {
            return;
        } else {
            String msg = "Task failed! Task record: " + task.getRecord(c);
            s_logger.warn(msg);
            task.cancel(c);
            throw new Types.BadAsyncResult(msg);
        }
    }

    void rebootVM(Connection conn, VM vm, String vmName) throws XmlRpcException {
        Task task = null;
        try {
            task = vm.cleanRebootAsync(conn);
            try {
                //poll every 1 seconds , timeout after 10 minutes
                waitForTask(conn, task, 1000, 10 * 60 * 1000);
                checkForSuccess(conn, task);
            } catch (Types.HandleInvalid e) {
                if (vm.getPowerState(conn) == Types.VmPowerState.RUNNING) {
                    task = null;
                    return;
                }
                throw new CloudRuntimeException("Reboot VM catch HandleInvalid and VM is not in RUNNING state");
            }
        } catch (XenAPIException e) {
            s_logger.debug("Unable to Clean Reboot VM(" + vmName + ") on host(" + _host.uuid +") due to " + e.toString() + ", try hard reboot");
            try {
                vm.hardReboot(conn);
            } catch (Exception e1) {
                String msg = "Unable to hard Reboot VM(" + vmName + ") on host(" + _host.uuid +") due to " + e.toString();
                s_logger.warn(msg, e1);
                throw new CloudRuntimeException(msg);
            }
        }finally {
            if( task != null) {
                try {
                    task.destroy(conn);
                } catch (Exception e1) {
                    s_logger.debug("unable to destroy task(" + task.toString() + ") on host(" + _host.uuid +") due to " + e1.toString());
                }
            }
        }
    }

    void forceShutdownVM(Connection conn, VM vm) {
        try {
            Long domId = vm.getDomid(conn);
            callHostPlugin(conn, "vmopspremium", "forceShutdownVM", "domId", domId.toString());
            vm.powerStateReset(conn);
            vm.destroy(conn);
        } catch (Exception e) {
            String msg = "forceShutdown failed due to " + e.toString();
            s_logger.warn(msg, e);
            throw new CloudRuntimeException(msg);
        }
    }

    void shutdownVM(Connection conn, VM vm, String vmName) throws XmlRpcException {
        Task task = null;
        try {
            task = vm.cleanShutdownAsync(conn);
            try {
                //poll every 1 seconds , timeout after 10 minutes
                waitForTask(conn, task, 1000, 10 * 60 * 1000);
                checkForSuccess(conn, task);
            } catch (Types.HandleInvalid e) {
                if (vm.getPowerState(conn) == Types.VmPowerState.HALTED) {
                    task = null;
                    return;
                }
                throw new CloudRuntimeException("Shutdown VM catch HandleInvalid and VM is not in HALTED state");
            }
        } catch (XenAPIException e) {
            s_logger.debug("Unable to cleanShutdown VM(" + vmName + ") on host(" + _host.uuid +") due to " + e.toString());
            try {
                Types.VmPowerState state = vm.getPowerState(conn);
                if (state == Types.VmPowerState.RUNNING ) {
                    try {
                        vm.hardShutdown(conn);
                    } catch (Exception e1) {
                        s_logger.debug("Unable to hardShutdown VM(" + vmName + ") on host(" + _host.uuid +") due to " + e.toString());
                        state = vm.getPowerState(conn);
                        if (state == Types.VmPowerState.RUNNING ) {
                            forceShutdownVM(conn, vm);
                        }
                        return;
                    }
                } else if (state == Types.VmPowerState.HALTED ) {
                    return;
                } else {
                    String msg = "After cleanShutdown the VM status is " + state.toString() + ", that is not expected";
                    s_logger.warn(msg);
                    throw new CloudRuntimeException(msg);
                }
            } catch (Exception e1) {
                String msg = "Unable to hardShutdown VM(" + vmName + ") on host(" + _host.uuid +") due to " + e.toString();
                s_logger.warn(msg, e1);
                throw new CloudRuntimeException(msg);
            }
        }finally {
            if( task != null) {
                try {
                    task.destroy(conn);
                } catch (Exception e1) {
                    s_logger.debug("unable to destroy task(" + task.toString() + ") on host(" + _host.uuid +") due to " + e1.toString());
                }
            }
        }
    }

    void startVM(Connection conn, Host host, VM vm, String vmName) throws XmlRpcException {
        Task task = null;
        try {
            task = vm.startOnAsync(conn, host, false, true);
            try {
                //poll every 1 seconds , timeout after 10 minutes
                waitForTask(conn, task, 1000, 10 * 60 * 1000);
                checkForSuccess(conn, task);
            } catch (Types.HandleInvalid e) {
                if (vm.getPowerState(conn) == Types.VmPowerState.RUNNING) {
                    task = null;
                    return;
                }
                throw new CloudRuntimeException("Shutdown VM catch HandleInvalid and VM is not in RUNNING state");
            }
        } catch (XenAPIException e) {
            String msg = "Unable to start VM(" + vmName + ") on host(" + _host.uuid +") due to " + e.toString();
            s_logger.warn(msg, e);
            throw new CloudRuntimeException(msg);
        }finally {
            if( task != null) {
                try {
                    task.destroy(conn);
                } catch (Exception e1) {
                    s_logger.debug("unable to destroy task(" + task.toString() + ") on host(" + _host.uuid +") due to " + e1.toString());
                }
            }
        }
    }

    private
    void migrateVM(Connection conn, Host destHost, VM vm, String vmName) throws XmlRpcException {
        Task task = null;
        try {
            Map<String, String> other = new HashMap<String, String>();
            other.put("live", "true");
            task = vm.poolMigrateAsync(conn, destHost, other);
            try {
                // poll every 1 seconds 
                long timeout = (_migratewait) * 1000L;
                waitForTask(conn, task, 1000, timeout);
                checkForSuccess(conn, task);
            } catch (Types.HandleInvalid e) {
                if (vm.getResidentOn(conn).equals(destHost)) {
                    task = null;
                    return;
                }
                throw new CloudRuntimeException("migrate VM catch HandleInvalid and VM is not running on dest host");
            }
        } catch (XenAPIException e) {
            String msg = "Unable to migrate VM(" + vmName + ") from host(" + _host.uuid +") due to " + e.toString();
            s_logger.warn(msg, e);
            throw new CloudRuntimeException(msg);
        }finally {
            if( task != null) {
                try {
                    task.destroy(conn);
                } catch (Exception e1) {
                    s_logger.debug("unable to destroy task(" + task.toString() + ") on host(" + _host.uuid +") due to " + e1.toString());
                }
            }
        }
    }

    protected VDI cloudVDIcopy(Connection conn, VDI vdi, SR sr, int wait) throws XenAPIException, XmlRpcException {
        Task task = null;
        if ( wait == 0 ) {
            wait = 2 * 60 * 60;
        }
        try {
            task = vdi.copyAsync(conn, sr);
            // poll every 1 seconds , timeout after 2 hours
            waitForTask(conn, task, 1000, wait * 1000);
            checkForSuccess(conn, task);
            VDI dvdi = Types.toVDI(task, conn);
            return dvdi;
        } finally {
            if (task != null) {
                try {
                    task.destroy(conn);
                } catch (Exception e1) {
                    s_logger.warn("unable to destroy task(" + task.toString() + ") on host(" + _host.uuid + ") due to ", e1);
                }
            }
        }
    }

    boolean swiftDownload(Connection conn, SwiftTO swift, String container, String rfilename, String dir, String lfilename, Boolean remote) {
        String result = null;
        try {
            result = callHostPluginAsync(conn, "swiftxen", "swift", 60 * 60,
                    "op", "download", "url", swift.getUrl(), "account", swift.getAccount(),
                    "username", swift.getUserName(), "key", swift.getKey(), "rfilename", rfilename,
                    "dir", dir, "lfilename", lfilename, "remote", remote.toString());
            if( result != null && result.equals("true")) {
                return true;
            }
        } catch (Exception e) {
            s_logger.warn("swift download failed due to ", e);
        }
        return false;
    }

    boolean swiftUpload(Connection conn, SwiftTO swift, String container, String ldir, String lfilename, Boolean isISCSI, int wait) {
        String result = null;
        try {
            result = callHostPluginAsync(conn, "swiftxen", "swift", wait,
                    "op", "upload", "url", swift.getUrl(), "account", swift.getAccount(),
                    "username", swift.getUserName(), "key", swift.getKey(), "container", container,
                    "ldir", ldir, "lfilename", lfilename, "isISCSI", isISCSI.toString());
            if( result != null && result.equals("true")) {
                return true;
            }
        } catch (Exception e) {
            s_logger.warn("swift upload failed due to " + e.toString(), e);
        }
        return false;
    }

    boolean swiftDelete(Connection conn, SwiftTO swift, String rfilename) {
        String result = null;
        try {
            result = callHostPlugin(conn, "swiftxen", "swift",
                    "op", "delete", "url", swift.getUrl(), "account", swift.getAccount(),
                    "username", swift.getUserName(), "key", swift.getKey(), "rfilename", rfilename);
            if( result != null && result.equals("true")) {
                return true;
            }
        } catch (Exception e) {
            s_logger.warn("swift download failed due to ", e);
        }
        return false;
    }

    private void swiftBackupSnapshot(Connection conn, SwiftTO swift, String srUuid, String snapshotUuid, String container, Boolean isISCSI, int wait)  {
        String lfilename;
        String ldir;
        if ( isISCSI ) {
            ldir = "/dev/VG_XenStorage-" + srUuid;
            lfilename = "VHD-" + snapshotUuid;
        } else {
            ldir = "/var/run/sr-mount/" + srUuid;
            lfilename = snapshotUuid + ".vhd";
        }
        swiftUpload(conn, swift, container, ldir, lfilename, isISCSI, wait);
    }


    protected String backupSnapshot(Connection conn, String primaryStorageSRUuid, Long dcId, Long accountId,
            Long volumeId, String secondaryStorageMountPath, String snapshotUuid, String prevBackupUuid, Boolean isISCSI, int wait, Long secHostId) {
        String backupSnapshotUuid = null;

        if (prevBackupUuid == null) {
            prevBackupUuid = "";
        }

        // Each argument is put in a separate line for readability.
        // Using more lines does not harm the environment.
        String backupUuid = UUID.randomUUID().toString();
        String results = callHostPluginAsync(conn, "vmopsSnapshot", "backupSnapshot", wait,
                "primaryStorageSRUuid", primaryStorageSRUuid, "dcId", dcId.toString(), "accountId", accountId.toString(),
                "volumeId", volumeId.toString(), "secondaryStorageMountPath", secondaryStorageMountPath,
                "snapshotUuid", snapshotUuid, "prevBackupUuid", prevBackupUuid, "backupUuid", backupUuid, "isISCSI", isISCSI.toString(), "secHostId", secHostId.toString());
        String errMsg = null;
        if (results == null || results.isEmpty()) {
            errMsg = "Could not copy backupUuid: " + backupSnapshotUuid + " of volumeId: " + volumeId
                    + " from primary storage " + primaryStorageSRUuid + " to secondary storage "
                    + secondaryStorageMountPath + " due to null";
        } else {

            String[] tmp = results.split("#");
            String status = tmp[0];
            backupSnapshotUuid = tmp[1];
            // status == "1" if and only if backupSnapshotUuid != null
            // So we don't rely on status value but return backupSnapshotUuid as an
            // indicator of success.
            if (status != null && status.equalsIgnoreCase("1") && backupSnapshotUuid != null) {
                s_logger.debug("Successfully copied backupUuid: " + backupSnapshotUuid + " of volumeId: " + volumeId
                        + " to secondary storage");
                return backupSnapshotUuid;
            } else {
                errMsg = "Could not copy backupUuid: " + backupSnapshotUuid + " of volumeId: " + volumeId
                        + " from primary storage " + primaryStorageSRUuid + " to secondary storage "
                        + secondaryStorageMountPath + " due to " + tmp[1];
            }
        }
        String source = backupUuid + ".vhd";
        killCopyProcess(conn, source);
        s_logger.warn(errMsg);
        return null;

    }

    protected String callHostPluginAsync(Connection conn, String plugin, String cmd, int wait, String... params) {
        int timeout = wait * 1000;
        Map<String, String> args = new HashMap<String, String>();
        Task task = null;
        try {
            for (int i = 0; i < params.length; i += 2) {
                args.put(params[i], params[i + 1]);
            }
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("callHostPlugin executing for command " + cmd + " with " + getArgsString(args));
            }
            Host host = Host.getByUuid(conn, _host.uuid);
            task = host.callPluginAsync(conn, plugin, cmd, args);
            // poll every 1 seconds
            waitForTask(conn, task, 1000, timeout);
            checkForSuccess(conn, task);
            String result = task.getResult(conn);
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("callHostPlugin Result: " + result);
            }
            return result.replace("<value>", "").replace("</value>", "").replace("\n", "");
        } catch (Types.HandleInvalid e) {
            s_logger.warn("callHostPlugin failed for cmd: " + cmd + " with args " + getArgsString(args)
                    + " due to HandleInvalid clazz:" + e.clazz + ", handle:" + e.handle);
        } catch (XenAPIException e) {
            s_logger.warn("callHostPlugin failed for cmd: " + cmd + " with args " + getArgsString(args) + " due to "
                    + e.toString(), e);
        } catch (XmlRpcException e) {
            s_logger.warn("callHostPlugin failed for cmd: " + cmd + " with args " + getArgsString(args) + " due to "
                    + e.getMessage(), e);
        } finally {
            if (task != null) {
                try {
                    task.destroy(conn);
                } catch (Exception e1) {
                    s_logger.warn("unable to destroy task(" + task.toString() + ") on host(" + _host.uuid + ") due to ", e1);
                }
            }
        }
        return null;
    }

    @Override
    public StopAnswer execute(StopCommand cmd) {
        String vmName = cmd.getVmName();
        try {
            Connection conn = getConnection();
            Set<VM> vms = VM.getByNameLabel(conn, vmName);
            // stop vm which is running on this host or is in halted state
            Iterator<VM> iter = vms.iterator();
            while ( iter.hasNext() ) {
                VM vm = iter.next();
                VM.Record vmr = vm.getRecord(conn);
                if (vmr.powerState != VmPowerState.RUNNING) {
                    continue;
                }
                if (isRefNull(vmr.residentOn)) {
                    continue;
                }
                if (vmr.residentOn.getUuid(conn).equals(_host.uuid)) {
                    continue;
                }
                iter.remove();
            }

            if (vms.size() == 0) {
                synchronized (_cluster.intern()) {
                    s_logger.info("VM does not exist on XenServer" + _host.uuid);
                    s_vms.remove(_cluster, _name, vmName);
                }
                return new StopAnswer(cmd, "VM does not exist", 0 , true);
            }
            for (VM vm : vms) {
                VM.Record vmr = vm.getRecord(conn);

                if (vmr.isControlDomain) {
                    String msg = "Tring to Shutdown control domain";
                    s_logger.warn(msg);
                    return new StopAnswer(cmd, msg, false);
                }

                if (vmr.powerState == VmPowerState.RUNNING && !isRefNull(vmr.residentOn) && !vmr.residentOn.getUuid(conn).equals(_host.uuid)) {
                    String msg = "Stop Vm " + vmName + " failed due to this vm is not running on this host: " + _host.uuid + " but host:" + vmr.residentOn.getUuid(conn);
                    s_logger.warn(msg);
                    return new StopAnswer(cmd, msg, false);
                }

                State state = s_vms.getState(_cluster, vmName);

                synchronized (_cluster.intern()) {
                    s_vms.put(_cluster, _name, vmName, State.Stopping);
                }
                s_logger.debug("9. The VM " + vmName + " is in Stopping state");

                try {
                    if (vmr.powerState == VmPowerState.RUNNING) {
                        /* when stop a vm, set affinity to current xenserver */
                        vm.setAffinity(conn, vm.getResidentOn(conn));

                        if (_canBridgeFirewall) {
                            String result = callHostPlugin(conn, "vmops", "destroy_network_rules_for_vm", "vmName", cmd
                                    .getVmName());
                            if (result == null || result.isEmpty() || !Boolean.parseBoolean(result)) {
                                s_logger.warn("Failed to remove  network rules for vm " + cmd.getVmName());
                            } else {
                                s_logger.info("Removed  network rules for vm " + cmd.getVmName());
                            }
                        }
                        shutdownVM(conn, vm, vmName);
                    }
                } catch (Exception e) {
                    String msg = "Catch exception " + e.getClass().getName() + " when stop VM:" + cmd.getVmName() + " due to " + e.toString();
                    s_logger.debug(msg);
                    return new StopAnswer(cmd, msg, false);
                } finally {

                    try {
                        if (vm.getPowerState(conn) == VmPowerState.HALTED) {
                            Set<VIF> vifs = vm.getVIFs(conn);
                            List<Network> networks = new ArrayList<Network>();
                            for (VIF vif : vifs) {
                                networks.add(vif.getNetwork(conn));
                            }
                            List<VDI> vdis = getVdis(conn, vm);
                            vm.destroy(conn);
                            for( VDI vdi : vdis ){
                                umount(conn, vdi);
                            }
                            state = State.Stopped;
                            SR sr = getISOSRbyVmName(conn, cmd.getVmName());
                            removeSR(conn, sr);
                            // Disable any VLAN networks that aren't used
                            // anymore
                            for (Network network : networks) {
                                try {
                                    if (network.getNameLabel(conn).startsWith("VLAN")) {
                                        disableVlanNetwork(conn, network);
                                    }
                                } catch (Exception e) {
                                    // network might be destroyed by other host
                                }
                            }
                            return new StopAnswer(cmd, "Stop VM " + vmName + " Succeed", 0, true);
                        }
                    } catch (XenAPIException e) {
                        String msg = "VM destroy failed in Stop " + vmName + " Command due to " + e.toString();
                        s_logger.warn(msg, e);
                    } catch (Exception e) {
                        String msg = "VM destroy failed in Stop " + vmName + " Command due to " + e.getMessage();
                        s_logger.warn(msg, e);
                    } finally {
                        synchronized (_cluster.intern()) {
                            s_vms.put(_cluster, _name, vmName, state);
                        }
                        s_logger.debug("10. The VM " + vmName + " is in " + state + " state");
                    }
                }
            }

        } catch (XenAPIException e) {
            String msg = "Stop Vm " + vmName + " fail due to " + e.toString();
            s_logger.warn(msg, e);
            return new StopAnswer(cmd, msg, false);
        } catch (XmlRpcException e) {
            String msg = "Stop Vm " + vmName + " fail due to " + e.getMessage();
            s_logger.warn(msg, e);
            return new StopAnswer(cmd, msg, false);
        } catch (Exception e) {
            s_logger.warn("Unable to stop " + vmName + " due to ",  e);
            return new StopAnswer(cmd, e);
        }
        return new StopAnswer(cmd, "Stop VM failed", false);
    }

    private List<VDI> getVdis(Connection conn, VM vm) {
        List<VDI> vdis = new ArrayList<VDI>();
        try {
            Set<VBD> vbds =vm.getVBDs(conn);
            for( VBD vbd : vbds ) {
                vdis.add(vbd.getVDI(conn));
            }
        } catch (XenAPIException e) {
            String msg = "getVdis can not get VPD due to " + e.toString();
            s_logger.warn(msg, e);
        } catch (XmlRpcException e) {
            String msg = "getVdis can not get VPD due to " + e.getMessage();
            s_logger.warn(msg, e);
        }
        return vdis;
    }

    protected String connect(Connection conn, final String vmName, final String ipAddress, final int port) {
        for (int i = 0; i <= _retry; i++) {
            try {
                Set<VM> vms = VM.getByNameLabel(conn, vmName);
                if (vms.size() < 1) {
                    String msg = "VM " + vmName + " is not running";
                    s_logger.warn(msg);
                    return msg;
                }
            } catch (Exception e) {
                String msg = "VM.getByNameLabel " + vmName + " failed due to " + e.toString();
                s_logger.warn(msg, e);
                return msg;
            }
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Trying to connect to " + ipAddress);
            }
            if (pingdomr(conn, ipAddress, Integer.toString(port))) {
                return null;
            }
            try {
                Thread.sleep(_sleep);
            } catch (final InterruptedException e) {
            }
        }
        String msg = "Timeout, Unable to logon to " + ipAddress;
        s_logger.debug(msg);

        return msg;
    }

    protected String connect(Connection conn, final String vmname, final String ipAddress) {
        return connect(conn, vmname, ipAddress, 3922);
    }

    protected boolean isDeviceUsed(Connection conn, VM vm, Long deviceId) {
        // Figure out the disk number to attach the VM to

        String msg = null;
        try {
            Set<String> allowedVBDDevices = vm.getAllowedVBDDevices(conn);
            if (allowedVBDDevices.contains(deviceId.toString())) {
                return false;
            }
            return true;
        } catch (XmlRpcException e) {
            msg = "Catch XmlRpcException due to: " + e.getMessage();
            s_logger.warn(msg, e);
        } catch (XenAPIException e) {
            msg = "Catch XenAPIException due to: " + e.toString();
            s_logger.warn(msg, e);
        }
        throw new CloudRuntimeException("When check deviceId " + msg);
    }


    protected String getUnusedDeviceNum(Connection conn, VM vm) {
        // Figure out the disk number to attach the VM to
        try {
            Set<String> allowedVBDDevices = vm.getAllowedVBDDevices(conn);
            if (allowedVBDDevices.size() == 0) {
                throw new CloudRuntimeException("Could not find an available slot in VM with name: " + vm.getNameLabel(conn) + " to attach a new disk.");
            }
            return allowedVBDDevices.iterator().next();
        } catch (XmlRpcException e) {
            String msg = "Catch XmlRpcException due to: " + e.getMessage();
            s_logger.warn(msg, e);
        } catch (XenAPIException e) {
            String msg = "Catch XenAPIException due to: " + e.toString();
            s_logger.warn(msg, e);
        }
        throw new CloudRuntimeException("Could not find an available slot in VM with name to attach a new disk.");
    }

    protected String callHostPlugin(Connection conn, String plugin, String cmd, String... params) {
        Map<String, String> args = new HashMap<String, String>();
        String msg;
        try {
            for (int i = 0; i < params.length; i += 2) {
                args.put(params[i], params[i + 1]);
            }

            if (s_logger.isTraceEnabled()) {
                s_logger.trace("callHostPlugin executing for command " + cmd + " with " + getArgsString(args));
            }
            Host host = Host.getByUuid(conn, _host.uuid);
            String result = host.callPlugin(conn, plugin, cmd, args);
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("callHostPlugin Result: " + result);
            }
            return result.replace("\n", "");
        } catch (XenAPIException e) {
            msg = "callHostPlugin failed for cmd: " + cmd + " with args " + getArgsString(args) + " due to " + e.toString();
            s_logger.warn(msg);
        } catch (XmlRpcException e) {
            msg = "callHostPlugin failed for cmd: " + cmd + " with args " + getArgsString(args) + " due to " + e.getMessage();
            s_logger.debug(msg);
        }
        throw new CloudRuntimeException(msg);
    }

    protected String getArgsString(Map<String, String> args) {
        StringBuilder argString = new StringBuilder();
        for (Map.Entry<String, String> arg : args.entrySet()) {
            argString.append(arg.getKey() + ": " + arg.getValue() + ", ");
        }
        return argString.toString();
    }

    protected boolean setIptables(Connection conn) {
        String result = callHostPlugin(conn, "vmops", "setIptables");
        if (result == null || result.isEmpty()) {
            return false;
        }
        return true;
    }

    protected XsLocalNetwork getManagementNetwork(Connection conn) throws XmlRpcException, XenAPIException {
        PIF mgmtPif = null;
        PIF.Record mgmtPifRec = null;
        Host host = Host.getByUuid(conn, _host.uuid);
        Set<PIF> hostPifs = host.getPIFs(conn);
        for (PIF pif : hostPifs) {
            PIF.Record rec = pif.getRecord(conn);
            if (rec.management) {
                if (rec.VLAN != null && rec.VLAN != -1) {
                    String msg = new StringBuilder("Unsupported configuration.  Management network is on a VLAN.  host=").append(_host.uuid).append("; pif=").append(rec.uuid)
                            .append("; vlan=").append(rec.VLAN).toString();
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
            String msg = "Unable to find management network for " + _host.uuid;
            s_logger.warn(msg);
            throw new CloudRuntimeException(msg);
        }
        Bond bond = mgmtPifRec.bondSlaveOf;
        if ( !isRefNull(bond) ) {
            String msg = "Management interface is on slave(" +mgmtPifRec.uuid + ") of bond("
                    + bond.getUuid(conn) + ") on host(" +_host.uuid + "), please move management interface to bond!";
            s_logger.warn(msg);
            throw new CloudRuntimeException(msg);
        }
        Network nk =  mgmtPifRec.network;
        Network.Record nkRec = nk.getRecord(conn);
        return new XsLocalNetwork(nk, nkRec, mgmtPif, mgmtPifRec);
    }


    protected VIF getCorrectVif(Connection conn, VM router, Network network) throws XmlRpcException, XenAPIException {
        Set<VIF> routerVIFs = router.getVIFs(conn);
        for (VIF vif : routerVIFs) {
            Network vifNetwork = vif.getNetwork(conn);
            if (vifNetwork.getUuid(conn).equals(network.getUuid(conn))) {
                return vif;
            }
        }

        return null;
    }

    protected VIF getCorrectVif(Connection conn, VM router, IpAddressTO ip) throws XmlRpcException, XenAPIException {
        NicTO nic = new NicTO();
        nic.setType(ip.getTrafficType());
        nic.setName(ip.getNetworkName());
        if (ip.getVlanId() == null) {
            nic.setBroadcastType(BroadcastDomainType.Native);
        } else {
            nic.setBroadcastType(BroadcastDomainType.Vlan);
            nic.setBroadcastUri(BroadcastDomainType.Vlan.toUri(ip.getVlanId()));
        }
        Network network = getNetwork(conn, nic);
        // Determine the correct VIF on DomR to associate/disassociate the
        // IP address with
        Set<VIF> routerVIFs = router.getVIFs(conn);
        for (VIF vif : routerVIFs) {
            Network vifNetwork = vif.getNetwork(conn);
            if (vifNetwork.getUuid(conn).equals(network.getUuid(conn))) {
                return vif;
            }
        }
        return null;
    }

    protected VIF getVifByMac(Connection conn, VM router, String mac) throws XmlRpcException, XenAPIException {
        Set<VIF> routerVIFs = router.getVIFs(conn);
        mac = mac.trim();
        for (VIF vif : routerVIFs) {
            String lmac = vif.getMAC(conn);
            if (lmac.trim().equals(mac)) {
                return vif;
            }
        }
        return null;
    }

    protected String getLowestAvailableVIFDeviceNum(Connection conn, VM vm) {
        String vmName = "";
        try {
            vmName = vm.getNameLabel(conn);
            List<Integer> usedDeviceNums = new ArrayList<Integer>();
            Set<VIF> vifs = vm.getVIFs(conn);
            Iterator<VIF> vifIter = vifs.iterator();
            while(vifIter.hasNext()){
                VIF vif = vifIter.next();
                try{
                    usedDeviceNums.add(Integer.valueOf(vif.getDevice(conn)));
                } catch (NumberFormatException e) {
                    String msg = "Obtained an invalid value for an allocated VIF device number for VM: " + vmName;
                    s_logger.debug(msg, e);
                    throw new CloudRuntimeException(msg);
                }
            }

            for(Integer i=0; i< _maxNics; i++){
                if(!usedDeviceNums.contains(i)){
                    s_logger.debug("Lowest available Vif device number: "+i+" for VM: " + vmName);
                    return i.toString();
                }
            }
        } catch (XmlRpcException e) {
            String msg = "Caught XmlRpcException: " + e.getMessage();
            s_logger.warn(msg, e);
        } catch (XenAPIException e) {
            String msg = "Caught XenAPIException: " + e.toString();
            s_logger.warn(msg, e);
        }

        throw new CloudRuntimeException("Could not find available VIF slot in VM with name: " + vmName);
    }

    protected VDI mount(Connection conn, StoragePoolType pooltype, String volumeFolder, String volumePath) {
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
     * @see enableVlanNetwork
     */
    protected XsLocalNetwork getNetworkByName(Connection conn, String name) throws XenAPIException, XmlRpcException {
        Set<Network> networks = Network.getByNameLabel(conn, name);
        if (networks.size() == 1) {
            return new XsLocalNetwork(networks.iterator().next(), null, null, null);
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
        for (Network network : networks) {
            XsLocalNetwork nic = new XsLocalNetwork(network);

            if (nic.getPif(conn) != null) {
                return nic;
            }

            Network.Record record = network.getRecord(conn);
            if (record.tags != null) {
                for (String tag : record.tags) {
                    Pair<Long, Integer> stamp = parseTimestamp(tag);
                    if (stamp == null) {
                        continue;
                    }

                    if (stamp.first() < earliestTimestamp || (stamp.first() == earliestTimestamp && stamp.second() < earliestRandom)) {
                        earliestTimestamp = stamp.first();
                        earliestRandom = stamp.second();
                        earliestNetwork = network;
                        earliestNetworkRecord = record;
                    }
                }
            }
        }

        return earliestNetwork != null ? new XsLocalNetwork(earliestNetwork, earliestNetworkRecord, null, null) : null;
    }

    protected String generateTimeStamp() {
        return new StringBuilder("CsCreateTime-").append(System.currentTimeMillis()).append("-").append(_rand.nextInt()).toString();
    }

    protected Pair<Long, Integer> parseTimestamp(String timeStampStr) {
        String[] tokens = timeStampStr.split("-");
        assert(tokens.length == 3) : "It's our timestamp but it doesn't fit our format: " + timeStampStr;
        if (!tokens[0].equals("CsCreateTime-")) {
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
    protected Network enableVlanNetwork(Connection conn, long tag, XsLocalNetwork network) throws XenAPIException, XmlRpcException {
        Network vlanNetwork = null;
        String oldName = "VLAN" + Long.toString(tag);
        String newName = "VLAN-" + network.getNetworkRecord(conn).uuid + "-" + tag;
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
                s_logger.debug("Creating VLAN network for " + tag + " on host " + _host.ip);
            }
            Network.Record nwr = new Network.Record();
            nwr.nameLabel = newName;
            nwr.tags = new HashSet<String>();
            nwr.tags.add(generateTimeStamp());
            vlanNetwork = Network.create(conn, nwr);
            vlanNic = getNetworkByName(conn, newName);
        }

        PIF nPif = network.getPif(conn);
        PIF.Record nPifr = network.getPifRecord(conn);

        vlanNetwork = vlanNic.getNetwork();
        if (vlanNic.getPif(conn) != null) {
            return vlanNetwork;
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Creating VLAN " + tag + " on host " + _host.ip + " on device " + nPifr.device);
        }
        VLAN vlan = VLAN.create(conn, nPif, tag, vlanNetwork);
        VLAN.Record vlanr = vlan.getRecord(conn);
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("VLAN is created for " + tag + ".  The uuid is " + vlanr.uuid);
        }

        return vlanNetwork;
    }

    protected void disableVlanNetwork(Connection conn, Network network) {
    }

    protected SR getLocalLVMSR(Connection conn) {
        try {
            Map<SR, SR.Record> map = SR.getAllRecords(conn);
            for (Map.Entry<SR, SR.Record> entry : map.entrySet()) {
                SR.Record srRec = entry.getValue();
                if (SRType.LVM.equals(srRec.type)) {
                    Set<PBD> pbds = srRec.PBDs;
                    if (pbds == null) {
                        continue;
                    }
                    for (PBD pbd : pbds) {
                        Host host = pbd.getHost(conn);
                        if (!isRefNull(host) && host.getUuid(conn).equals(_host.uuid)) {
                            if (!pbd.getCurrentlyAttached(conn)) {
                                pbd.plug(conn);
                            }
                            SR sr = entry.getKey();
                            sr.scan(conn);
                            return sr;
                        }
                    }
                }
            }
        } catch (XenAPIException e) {
            String msg = "Unable to get local LVMSR in host:" + _host.uuid + e.toString();
            s_logger.warn(msg);
        } catch (XmlRpcException e) {
            String msg = "Unable to get local LVMSR in host:" + _host.uuid + e.getCause();
            s_logger.warn(msg);
        }
        return null;
    }

    protected SR getLocalEXTSR(Connection conn) {
        try {
            Map<SR, SR.Record> map = SR.getAllRecords(conn);
            for (Map.Entry<SR, SR.Record> entry : map.entrySet()) {
                SR.Record srRec = entry.getValue();
                if (SRType.FILE.equals(srRec.type) || SRType.EXT.equals(srRec.type)) {
                    Set<PBD> pbds = srRec.PBDs;
                    if (pbds == null) {
                        continue;
                    }
                    for (PBD pbd : pbds) {
                        Host host = pbd.getHost(conn);
                        if (!isRefNull(host) && host.getUuid(conn).equals(_host.uuid)) {
                            if (!pbd.getCurrentlyAttached(conn)) {
                                pbd.plug(conn);
                            }
                            SR sr = entry.getKey();
                            sr.scan(conn);
                            return sr;
                        }
                    }
                }
            }
        } catch (XenAPIException e) {
            String msg = "Unable to get local EXTSR in host:" + _host.uuid + e.toString();
            s_logger.warn(msg);
        } catch (XmlRpcException e) {
            String msg = "Unable to get local EXTSR in host:" + _host.uuid + e.getCause();
            s_logger.warn(msg);
        }
        return null;
    }

    protected StartupStorageCommand initializeLocalSR(Connection conn) {
        SR lvmsr = getLocalLVMSR(conn);
        if (lvmsr != null) {
            try {
                _host.localSRuuid = lvmsr.getUuid(conn);

                String lvmuuid = lvmsr.getUuid(conn);
                long cap = lvmsr.getPhysicalSize(conn);
                if (cap > 0) {
                    long avail = cap - lvmsr.getPhysicalUtilisation(conn);
                    lvmsr.setNameLabel(conn, lvmuuid);
                    String name = "Cloud Stack Local LVM Storage Pool for " + _host.uuid;
                    lvmsr.setNameDescription(conn, name);
                    Host host = Host.getByUuid(conn, _host.uuid);
                    String address = host.getAddress(conn);
                    StoragePoolInfo pInfo = new StoragePoolInfo(lvmuuid, address, SRType.LVM.toString(), SRType.LVM.toString(), StoragePoolType.LVM, cap, avail);
                    StartupStorageCommand cmd = new StartupStorageCommand();
                    cmd.setPoolInfo(pInfo);
                    cmd.setGuid(_host.uuid);
                    cmd.setDataCenter(Long.toString(_dcId));
                    cmd.setResourceType(Storage.StorageResourceType.STORAGE_POOL);
                    return cmd;
                }
            } catch (XenAPIException e) {
                String msg = "build local LVM info err in host:" + _host.uuid + e.toString();
                s_logger.warn(msg);
            } catch (XmlRpcException e) {
                String msg = "build local LVM info err in host:" + _host.uuid + e.getMessage();
                s_logger.warn(msg);
            }
        }

        SR extsr = getLocalEXTSR(conn);
        if (extsr != null) {
            try {
                String extuuid = extsr.getUuid(conn);
                _host.localSRuuid = extuuid;
                long cap = extsr.getPhysicalSize(conn);
                if (cap > 0) {
                    long avail = cap - extsr.getPhysicalUtilisation(conn);
                    extsr.setNameLabel(conn, extuuid);
                    String name = "Cloud Stack Local EXT Storage Pool for " + _host.uuid;
                    extsr.setNameDescription(conn, name);
                    Host host = Host.getByUuid(conn, _host.uuid);
                    String address = host.getAddress(conn);
                    StoragePoolInfo pInfo = new StoragePoolInfo(extuuid, address, SRType.EXT.toString(), SRType.EXT.toString(), StoragePoolType.EXT, cap, avail);
                    StartupStorageCommand cmd = new StartupStorageCommand();
                    cmd.setPoolInfo(pInfo);
                    cmd.setGuid(_host.uuid);
                    cmd.setDataCenter(Long.toString(_dcId));
                    cmd.setResourceType(Storage.StorageResourceType.STORAGE_POOL);
                    return cmd;
                }
            } catch (XenAPIException e) {
                String msg = "build local EXT info err in host:" + _host.uuid + e.toString();
                s_logger.warn(msg);
            } catch (XmlRpcException e) {
                String msg = "build local EXT info err in host:" + _host.uuid + e.getMessage();
                s_logger.warn(msg);
            }
        }
        return null;
    }

    @Override
    public PingCommand getCurrentStatus(long id) {
        try {
            if (!pingXenServer()) {
                Thread.sleep(1000);
                if (!pingXenServer()) {
                    s_logger.warn(" can not ping xenserver " + _host.uuid);
                    return null;
                }
            }
            Connection conn = getConnection();
            if (!_canBridgeFirewall && !_isOvs) {
                return new PingRoutingCommand(getType(), id, null);
            } else if (_isOvs) {
                List<Pair<String, Long>>ovsStates = ovsFullSyncStates();
                return new PingRoutingWithOvsCommand(getType(), id, null, ovsStates);
            }else {
                HashMap<String, Pair<Long, Long>> nwGrpStates = syncNetworkGroups(conn, id);
                return new PingRoutingWithNwGroupsCommand(getType(), id, null, nwGrpStates);
            }
        } catch (Exception e) {
            s_logger.warn("Unable to get current status", e);
            return null;
        }
    }

    private HashMap<String, Pair<Long,Long>> syncNetworkGroups(Connection conn, long id) {
        HashMap<String, Pair<Long,Long>> states = new HashMap<String, Pair<Long,Long>>();

        String result = callHostPlugin(conn, "vmops", "get_rule_logs_for_vms", "host_uuid", _host.uuid);
        s_logger.trace("syncNetworkGroups: id=" + id + " got: " + result);
        String [] rulelogs = result != null ?result.split(";"): new String [0];
        for (String rulesforvm: rulelogs){
            String [] log = rulesforvm.split(",");
            if (log.length != 6) {
                continue;
            }
            //output = ','.join([vmName, vmID, vmIP, domID, signature, seqno])
            try {
                states.put(log[0], new Pair<Long,Long>(Long.parseLong(log[1]), Long.parseLong(log[5])));
            } catch (NumberFormatException nfe) {
                states.put(log[0], new Pair<Long,Long>(-1L, -1L));
            }
        }
        return states;
    }

    @Override
    public Type getType() {
        return com.cloud.host.Host.Type.Routing;
    }

    protected boolean getHostInfo(Connection conn) throws IllegalArgumentException{
        try {
            Host myself = Host.getByUuid(conn, _host.uuid);
            Set<HostCpu> hcs = null;
            for (int i = 0; i < 10; i++) {
                hcs = myself.getHostCPUs(conn);
                _host.cpus = hcs.size();
                if (_host.cpus > 0) {
                    break;
                }
                Thread.sleep(5000);
            }
            if (_host.cpus <= 0) {
                throw new CloudRuntimeException("Cannot get the numbers of cpu from XenServer host " + _host.ip);
            }

            for (final HostCpu hc : hcs) {
                _host.speed = hc.getSpeed(conn).intValue();
                break;
            }
            Host.Record hr = myself.getRecord(conn);
            _host.product_version = hr.softwareVersion.get("product_version");
            if (_host.product_version == null) {
                _host.product_version = hr.softwareVersion.get("platform_version");
            } else {
                _host.product_version = _host.product_version.trim();
            }

            XsLocalNetwork privateNic = getManagementNetwork(conn);
            _privateNetworkName = privateNic.getNetworkRecord(conn).nameLabel;
            _host.privatePif = privateNic.getPifRecord(conn).uuid;
            _host.privateNetwork = privateNic.getNetworkRecord(conn).uuid;           
            _host.systemvmisouuid = null;

            XsLocalNetwork guestNic = null;
            if (_guestNetworkName != null && !_guestNetworkName.equals(_privateNetworkName)) {
                guestNic = getNetworkByName(conn, _guestNetworkName);
                if (guestNic == null) {
                    s_logger.warn("Unable to find guest network " + _guestNetworkName);
                    throw new IllegalArgumentException("Unable to find guest network " + _guestNetworkName + " for host " + _host.ip);
                }
            } else {
                guestNic = privateNic;
                _guestNetworkName = _privateNetworkName;
            }
            _host.guestNetwork = guestNic.getNetworkRecord(conn).uuid;
            _host.guestPif = guestNic.getPifRecord(conn).uuid;

            XsLocalNetwork publicNic = null;
            if (_publicNetworkName != null && !_publicNetworkName.equals(_guestNetworkName)) {
                publicNic = getNetworkByName(conn, _publicNetworkName);
                if (publicNic == null) {
                    s_logger.warn("Unable to find public network " + _publicNetworkName + " for host " + _host.ip);
                    throw new IllegalArgumentException("Unable to find public network " + _publicNetworkName + " for host " + _host.ip);
                }
            } else {
                publicNic = guestNic;
                _publicNetworkName = _guestNetworkName;
            }
            _host.publicPif = publicNic.getPifRecord(conn).uuid;
            _host.publicNetwork = publicNic.getNetworkRecord(conn).uuid;
            if (_storageNetworkName1 == null ) {
                _storageNetworkName1 = _guestNetworkName;
            }
            XsLocalNetwork storageNic1 = null;
            storageNic1 = getNetworkByName(conn, _storageNetworkName1);
            if (storageNic1 == null) {
                s_logger.warn("Unable to find storage network " + _storageNetworkName1 + " for host " + _host.ip);
                throw new IllegalArgumentException("Unable to find storage network " + _storageNetworkName1 + " for host " + _host.ip);
            } else {
                _host.storageNetwork1 = storageNic1.getNetworkRecord(conn).uuid;
                _host.storagePif1 = storageNic1.getPifRecord(conn).uuid;
            }

            XsLocalNetwork storageNic2 = null;
            if (_storageNetworkName2 != null) {
                storageNic2 = getNetworkByName(conn, _storageNetworkName2);
                _host.storageNetwork2 = storageNic2.getNetworkRecord(conn).uuid;
                _host.storagePif2 = storageNic2.getPifRecord(conn).uuid;
            }

            s_logger.info("Private Network is " + _privateNetworkName + " for host " + _host.ip);
            s_logger.info("Guest Network is " + _guestNetworkName + " for host " + _host.ip);
            s_logger.info("Public Network is " + _publicNetworkName + " for host " + _host.ip);

            return true;
        } catch (XenAPIException e) {
            s_logger.warn("Unable to get host information for " + _host.ip, e);
            return false;
        } catch (Exception e) {
            s_logger.warn("Unable to get host information for " + _host.ip, e);
            return false;
        }
    }

    private void setupLinkLocalNetwork(Connection conn) {
        try {
            Network.Record rec = new Network.Record();
            Set<Network> networks = Network.getByNameLabel(conn, _linkLocalPrivateNetworkName);
            Network linkLocal = null;

            if (networks.size() == 0) {
                rec.nameDescription = "link local network used by system vms";
                rec.nameLabel = _linkLocalPrivateNetworkName;
                Map<String, String> configs = new HashMap<String, String>();
                configs.put("ip_begin", NetUtils.getLinkLocalGateway());
                configs.put("ip_end", NetUtils.getLinkLocalIpEnd());
                configs.put("netmask", NetUtils.getLinkLocalNetMask());
                rec.otherConfig = configs;
                linkLocal = Network.create(conn, rec);

            } else {
                linkLocal = networks.iterator().next();
            }

            /* Make sure there is a physical bridge on this network */
            VIF dom0vif = null;
            Pair<VM, VM.Record> vm = getControlDomain(conn);
            VM dom0 = vm.first();
            Set<VIF> vifs = dom0.getVIFs(conn);
            if (vifs.size() != 0) {
                for (VIF vif : vifs) {
                    Map<String, String> otherConfig = vif.getOtherConfig(conn);
                    if (otherConfig != null) {
                        String nameLabel = otherConfig.get("nameLabel");
                        if ((nameLabel != null) && nameLabel.equalsIgnoreCase("link_local_network_vif")) {
                            dom0vif = vif;
                        }
                    }
                }
            }

            /* create temp VIF0 */
            if (dom0vif == null) {
                s_logger.debug("Can't find a vif on dom0 for link local, creating a new one");
                VIF.Record vifr = new VIF.Record();
                vifr.VM = dom0;
                vifr.device = getLowestAvailableVIFDeviceNum(conn, dom0);
                if (vifr.device == null) {
                    s_logger.debug("Failed to create link local network, no vif available");
                    return;
                }
                Map<String, String> config = new HashMap<String, String>();
                config.put("nameLabel", "link_local_network_vif");
                vifr.otherConfig = config;
                vifr.MAC = "FE:FF:FF:FF:FF:FF";
                vifr.network = linkLocal;
                dom0vif = VIF.create(conn, vifr);
                dom0vif.plug(conn);
            } else {
                s_logger.debug("already have a vif on dom0 for link local network");
                if (!dom0vif.getCurrentlyAttached(conn)) {
                    dom0vif.plug(conn);
                }
            }

            String brName = linkLocal.getBridge(conn);
            callHostPlugin(conn, "vmops", "setLinkLocalIP", "brName", brName);
            _host.linkLocalNetwork = linkLocal.getUuid(conn);

        } catch (XenAPIException e) {
            s_logger.warn("Unable to create local link network", e);
        } catch (XmlRpcException e) {
            s_logger.warn("Unable to create local link network", e);
        }
    }

    protected boolean transferManagementNetwork(Connection conn, Host host, PIF src, PIF.Record spr, PIF dest) throws XmlRpcException, XenAPIException {
        dest.reconfigureIp(conn, spr.ipConfigurationMode, spr.IP, spr.netmask, spr.gateway, spr.DNS);
        Host.managementReconfigure(conn, dest);
        String hostUuid = null;
        int count = 0;
        while (count < 10) {
            try {
                Thread.sleep(10000);
                hostUuid = host.getUuid(conn);
                if (hostUuid != null) {
                    break;
                }
            } catch (XmlRpcException e) {
                s_logger.debug("Waiting for host to come back: " + e.getMessage());
            } catch (XenAPIException e) {
                s_logger.debug("Waiting for host to come back: " + e.getMessage());
            } catch (InterruptedException e) {
                s_logger.debug("Gotta run");
                return false;
            }
        }
        if (hostUuid == null) {
            s_logger.warn("Unable to transfer the management network from " + spr.uuid);
            return false;
        }

        src.reconfigureIp(conn, IpConfigurationMode.NONE, null, null, null, null);
        return true;
    }

    @Override
    public StartupCommand[] initialize() throws IllegalArgumentException{
        Connection conn = getConnection();
        if (!getHostInfo(conn)) {
            s_logger.warn("Unable to get host information for " + _host.ip);
            return null;
        }
        StartupRoutingCommand cmd = new StartupRoutingCommand();
        fillHostInfo(conn, cmd);
        cmd.setHypervisorType(HypervisorType.XenServer);
        cmd.setCluster(_cluster);
        cmd.setPoolSync(false);

        Pool pool;
        try {
            pool = Pool.getByUuid(conn, _host.pool);
            Pool.Record poolr = pool.getRecord(conn);

            Host.Record hostr = poolr.master.getRecord(conn);
            if (_host.uuid.equals(hostr.uuid)) {
                HashMap<String, Pair<String, State>> allStates=fullClusterSync(conn);
                cmd.setClusterVMStateChanges(allStates);
            }
        } catch (Throwable e) {
            s_logger.warn("Check for master failed, failing the FULL Cluster sync command");
        } 

        StartupStorageCommand sscmd = initializeLocalSR(conn);
        if (sscmd != null) {
            return new StartupCommand[] { cmd, sscmd };
        }
        return new StartupCommand[] { cmd };
    }

    private void cleanupTemplateSR(Connection conn) {
        Set<PBD> pbds = null;
        try {
            Host host = Host.getByUuid(conn, _host.uuid);
            pbds = host.getPBDs(conn);
        } catch (XenAPIException e) {
            s_logger.warn("Unable to get the SRs " + e.toString(), e);
            throw new CloudRuntimeException("Unable to get SRs " + e.toString(), e);
        } catch (Exception e) {
            throw new CloudRuntimeException("Unable to get SRs " + e.getMessage(), e);
        }
        for (PBD pbd : pbds) {
            SR sr = null;
            SR.Record srRec = null;
            try {
                sr = pbd.getSR(conn);
                srRec = sr.getRecord(conn);
            } catch (Exception e) {
                s_logger.warn("pbd.getSR get Exception due to ", e);
                continue;
            }
            String type = srRec.type;
            if (srRec.shared) {
                continue;
            }
            if (SRType.NFS.equals(type) || (SRType.ISO.equals(type) && srRec.nameDescription.contains("template"))) {
                try {
                    pbd.unplug(conn);
                    pbd.destroy(conn);
                    sr.forget(conn);
                } catch (Exception e) {
                    s_logger.warn("forget SR catch Exception due to ", e);
                }
            }
        }
    }

    protected SetupAnswer execute(SetupCommand cmd) {
        Connection conn = getConnection();
        setupServer(conn);
        try {
            if (!setIptables(conn)) {
                s_logger.warn("set xenserver Iptable failed");
                return null;
            }
            _canBridgeFirewall = can_bridge_firewall(conn);

            String result = callHostPluginPremium(conn, "heartbeat", "host", _host.uuid, "interval", Integer
                    .toString(_heartbeatInterval));
            if (result == null || !result.contains("> DONE <")) {
                s_logger.warn("Unable to launch the heartbeat process on " + _host.ip);
                return null;
            }
            cleanupTemplateSR(conn);
            Host host = Host.getByUuid(conn, _host.uuid);
            try {
                if (cmd.useMultipath()) {
                    // the config value is set to true
                    host.addToOtherConfig(conn, "multipathing", "true");
                    host.addToOtherConfig(conn, "multipathhandle", "dmp");
                }

            } catch (Types.MapDuplicateKey e) {
                s_logger.debug("multipath is already set");
            }
            if (cmd.needSetup() ) {
                result = callHostPlugin(conn, "vmops", "setup_iscsi", "uuid", _host.uuid);
                if (!result.contains("> DONE <")) {
                    s_logger.warn("Unable to setup iscsi: " + result);
                    return new SetupAnswer(cmd, result);
                }

                Pair<PIF, PIF.Record> mgmtPif = null;
                Set<PIF> hostPifs = host.getPIFs(conn);
                for (PIF pif : hostPifs) {
                    PIF.Record rec = pif.getRecord(conn);
                    if (rec.management) {
                        if (rec.VLAN != null && rec.VLAN != -1) {
                            String msg = new StringBuilder(
                                    "Unsupported configuration.  Management network is on a VLAN.  host=").append(
                                            _host.uuid).append("; pif=").append(rec.uuid).append("; vlan=").append(rec.VLAN)
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
                    String msg = "Unable to find management network for " + _host.uuid;
                    s_logger.warn(msg);
                    return new SetupAnswer(cmd, msg);
                }

                Map<Network, Network.Record> networks = Network.getAllRecords(conn);
                for (Network.Record network : networks.values()) {
                    if (network.nameLabel.equals("cloud-private")) {
                        for (PIF pif : network.PIFs) {
                            PIF.Record pr = pif.getRecord(conn);
                            if (_host.uuid.equals(pr.host.getUuid(conn))) {
                                if (s_logger.isDebugEnabled()) {
                                    s_logger.debug("Found a network called cloud-private. host=" + _host.uuid
                                            + ";  Network=" + network.uuid + "; pif=" + pr.uuid);
                                }
                                if (pr.VLAN != null && pr.VLAN != -1) {
                                    String msg = new StringBuilder(
                                            "Unsupported configuration.  Network cloud-private is on a VLAN.  Network=")
                                    .append(network.uuid).append(" ; pif=").append(pr.uuid).toString();
                                    s_logger.warn(msg);
                                    return new SetupAnswer(cmd, msg);
                                }
                                if (!pr.management && pr.bondMasterOf != null && pr.bondMasterOf.size() > 0) {
                                    if (pr.bondMasterOf.size() > 1) {
                                        String msg = new StringBuilder(
                                                "Unsupported configuration.  Network cloud-private has more than one bond.  Network=")
                                        .append(network.uuid).append("; pif=").append(pr.uuid).toString();
                                        s_logger.warn(msg);
                                        return new SetupAnswer(cmd, msg);
                                    }
                                    Bond bond = pr.bondMasterOf.iterator().next();
                                    Set<PIF> slaves = bond.getSlaves(conn);
                                    for (PIF slave : slaves) {
                                        PIF.Record spr = slave.getRecord(conn);
                                        if (spr.management) {
                                            if (!transferManagementNetwork(conn, host, slave, spr, pif)) {
                                                String msg = new StringBuilder(
                                                        "Unable to transfer management network.  slave=" + spr.uuid
                                                        + "; master=" + pr.uuid + "; host=" + _host.uuid)
                                                .toString();
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

        } catch (XmlRpcException e) {
            s_logger.warn("Unable to setup", e);
            return new SetupAnswer(cmd, e.getMessage());
        } catch (XenAPIException e) {
            s_logger.warn("Unable to setup", e);
            return new SetupAnswer(cmd, e.getMessage());
        } catch (Exception e) {
            s_logger.warn("Unable to setup", e);
            return new SetupAnswer(cmd, e.getMessage());
        }
    }

    /* return : if setup is needed */
    protected boolean setupServer(Connection conn) {
        String packageVersion = CitrixResourceBase.class.getPackage().getImplementationVersion();
        String version = this.getClass().getName() + "-" + ( packageVersion == null ? Long.toString(System.currentTimeMillis()) : packageVersion );

        try {
            Host host = Host.getByUuid(conn, _host.uuid);
            /* enable host in case it is disabled somehow */
            host.enable(conn);
            /* push patches to XenServer */
            Host.Record hr = host.getRecord(conn);

            Iterator<String> it = hr.tags.iterator();

            while (it.hasNext()) {
                String tag = it.next();
                if (tag.startsWith("vmops-version-")) {
                    if (tag.contains(version)) {
                        s_logger.info(logX(host, "Host " + hr.address + " is already setup."));
                        return false;
                    } else {
                        it.remove();
                    }
                }
            }

            com.trilead.ssh2.Connection sshConnection = new com.trilead.ssh2.Connection(hr.address, 22);
            try {
                sshConnection.connect(null, 60000, 60000);
                if (!sshConnection.authenticateWithPassword(_username, _password.peek())) {
                    throw new CloudRuntimeException("Unable to authenticate");
                }

                com.trilead.ssh2.Session session = sshConnection.openSession();
                SCPClient scp = new SCPClient(sshConnection);

                List<File> files = getPatchFiles();
                if( files == null || files.isEmpty() ) {
                    throw new CloudRuntimeException("Can not find patch file");
                }
                for( File file :files) {
                    String path = file.getParentFile().getAbsolutePath() + "/";
                    Properties props = new Properties();
                    props.load(new FileInputStream(file));

                    for (Map.Entry<Object, Object> entry : props.entrySet()) {
                        String k = (String) entry.getKey();
                        String v = (String) entry.getValue();

                        assert (k != null && k.length() > 0 && v != null && v.length() > 0) : "Problems with " + k + "=" + v;

                        String[] tokens = v.split(",");
                        String f = null;
                        if (tokens.length == 3 && tokens[0].length() > 0) {
                            if (tokens[0].startsWith("/")) {
                                f = tokens[0];
                            } else if (tokens[0].startsWith("~")) {
                                String homedir = System.getenv("HOME");
                                f = homedir + tokens[0].substring(1) + k;
                            } else {
                                f = path + tokens[0] + '/' + k;
                            }
                        } else {
                            f = path + k;
                        }
                        String d = tokens[tokens.length - 1];
                        f = f.replace('/', File.separatorChar);

                        String p = "0755";
                        if (tokens.length == 3) {
                            p = tokens[1];
                        } else if (tokens.length == 2) {
                            p = tokens[0];
                        }

                        if (!new File(f).exists()) {
                            s_logger.warn("We cannot locate " + f);
                            continue;
                        }
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Copying " + f + " to " + d + " on " + hr.address + " with permission " + p);
                        }
                        try {
                            session.execCommand("mkdir -m 700 -p " + d);
                        } catch (IOException e) {
                            s_logger.debug("Unable to create destination path: " + d + " on " + hr.address + " but trying anyway");

                        }
                        scp.put(f, d, p);

                    }
                }

            } catch (IOException e) {
                throw new CloudRuntimeException("Unable to setup the server correctly", e);
            } finally {
                sshConnection.close();
            }
            hr.tags.add("vmops-version-" + version);
            host.setTags(conn, hr.tags);
            return true;
        } catch (XenAPIException e) {
            String msg = "Xen setup failed due to " + e.toString();
            s_logger.warn(msg, e);
            throw new CloudRuntimeException("Unable to get host information " + e.toString(), e);
        } catch (XmlRpcException e) {
            String msg = "Xen setup failed due to " + e.getMessage();
            s_logger.warn(msg, e);
            throw new CloudRuntimeException("Unable to get host information ", e);
        }
    }

    protected CheckNetworkAnswer execute(CheckNetworkCommand cmd) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Checking if network name setup is done on the resource");
        }

        List<PhysicalNetworkSetupInfo> infoList = cmd.getPhysicalNetworkInfoList();

        try{
            boolean errorout = false;
            String msg = "";
            for(PhysicalNetworkSetupInfo info : infoList){
                if(!isNetworkSetupByName(info.getGuestNetworkName())){
                    msg = "For Physical Network id:"+ info.getPhysicalNetworkId() + ", Guest Network is not configured on the backend by name " + info.getGuestNetworkName();
                    errorout = true;
                    break;
                }
                if(!isNetworkSetupByName(info.getPrivateNetworkName())){
                    msg = "For Physical Network id:"+ info.getPhysicalNetworkId() + ", Private Network is not configured on the backend by name " + info.getPrivateNetworkName();
                    errorout = true;
                    break;               
                }
                if(!isNetworkSetupByName(info.getPublicNetworkName())){
                    msg = "For Physical Network id:"+ info.getPhysicalNetworkId() + ", Public Network is not configured on the backend by name " + info.getPublicNetworkName();
                    errorout = true;
                    break;
                }
                /*if(!isNetworkSetupByName(info.getStorageNetworkName())){
                    msg = "For Physical Network id:"+ info.getPhysicalNetworkId() + ", Storage Network is not configured on the backend by name " + info.getStorageNetworkName();
                    errorout = true;
                    break;
                }*/
            }
            if(errorout){
                s_logger.error(msg);
                return new CheckNetworkAnswer(cmd, false, msg);
            }else{
                return new CheckNetworkAnswer(cmd, true , "Network Setup check by names is done");
            }

        }catch (XenAPIException e) {
            String msg = "CheckNetworkCommand failed with XenAPIException:" + e.toString() + " host:" + _host.uuid;
            s_logger.warn(msg, e);
            return new CheckNetworkAnswer(cmd, false, msg);
        }catch (Exception e) {
            String msg = "CheckNetworkCommand failed with Exception:" + e.getMessage() + " host:" + _host.uuid;
            s_logger.warn(msg, e);
            return new CheckNetworkAnswer(cmd, false, msg);
        }
    }

    protected boolean isNetworkSetupByName(String nameTag) throws XenAPIException, XmlRpcException{
        if (nameTag != null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Looking for network setup by name " + nameTag);
            }
            Connection conn = getConnection();
            XsLocalNetwork network = getNetworkByName(conn, nameTag);
            if (network == null) {
                return false;
            }
        }
        return true;
    }    

    protected List<File> getPatchFiles() {
        return null;
    }

    protected SR getSRByNameLabelandHost(Connection conn, String name) throws BadServerResponse, XenAPIException, XmlRpcException {
        Set<SR> srs = SR.getByNameLabel(conn, name);
        SR ressr = null;
        for (SR sr : srs) {
            Set<PBD> pbds;
            pbds = sr.getPBDs(conn);
            for (PBD pbd : pbds) {
                PBD.Record pbdr = pbd.getRecord(conn);
                if (pbdr.host != null && pbdr.host.getUuid(conn).equals(_host.uuid)) {
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
        Connection conn = getConnection();
        try {
            Set<SR> srs = SR.getByNameLabel(conn, cmd.getStorageId());
            if (srs.size() != 1) {
                String msg = "There are " + srs.size() + " storageid: " + cmd.getStorageId();
                s_logger.warn(msg);
                return new GetStorageStatsAnswer(cmd, msg);
            }
            SR sr = srs.iterator().next();
            sr.scan(conn);
            long capacity = sr.getPhysicalSize(conn);
            long used = sr.getPhysicalUtilisation(conn);
            return new GetStorageStatsAnswer(cmd, capacity, used);
        } catch (XenAPIException e) {
            String msg = "GetStorageStats Exception:" + e.toString() + "host:" + _host.uuid + "storageid: " + cmd.getStorageId();
            s_logger.warn(msg);
            return new GetStorageStatsAnswer(cmd, msg);
        } catch (XmlRpcException e) {
            String msg = "GetStorageStats Exception:" + e.getMessage() + "host:" + _host.uuid + "storageid: " + cmd.getStorageId();
            s_logger.warn(msg);
            return new GetStorageStatsAnswer(cmd, msg);
        }
    }


    private void pbdPlug(Connection conn, PBD pbd, String uuid) {
        try {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Plugging in PBD " + uuid + " for " + _host);
            }
            pbd.plug(conn);
        } catch (Exception e) {
            String msg = "PBD " + uuid + " is not attached! and PBD plug failed due to "
                    + e.toString() + ". Please check this PBD in " + _host;
            s_logger.warn(msg, e);
            throw new CloudRuntimeException(msg);
        }
    }

    protected boolean checkSR(Connection conn, SR sr) {
        try {
            SR.Record srr = sr.getRecord(conn);
            Set<PBD> pbds = sr.getPBDs(conn);
            if (pbds.size() == 0) {
                String msg = "There is no PBDs for this SR: " + srr.nameLabel + " on host:" + _host.uuid;
                s_logger.warn(msg);
                return false;
            }
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Checking " + srr.nameLabel + " or SR " + srr.uuid + " on " + _host);
            }
            if (srr.shared) {
                Host host = Host.getByUuid(conn, _host.uuid);

                boolean found = false;
                for (PBD pbd : pbds) {
                    PBD.Record pbdr = pbd.getRecord(conn);
                    if (host.equals(pbdr.host)) {
                        if (!pbdr.currentlyAttached) {
                            pbdPlug(conn, pbd, pbdr.uuid);
                        }
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    PBD.Record pbdr = srr.PBDs.iterator().next().getRecord(conn);
                    pbdr.host = host;
                    pbdr.uuid = "";
                    PBD pbd = PBD.create(conn, pbdr);
                    pbdPlug(conn, pbd, pbd.getUuid(conn));
                }
            } else {
                for (PBD pbd : pbds) {
                    PBD.Record pbdr = pbd.getRecord(conn);
                    if (!pbdr.currentlyAttached) {
                        pbdPlug(conn, pbd, pbdr.uuid);
                    }
                }
            }

        } catch (Exception e) {
            String msg = "checkSR failed host:" + _host + " due to " + e.toString();
            s_logger.warn(msg, e);
            return false;
        }
        return true;
    }

    protected Answer execute(CreateStoragePoolCommand cmd) {
        Connection conn = getConnection();
        StorageFilerTO pool = cmd.getPool();
        try {
            if (pool.getType() == StoragePoolType.NetworkFilesystem) {
                getNfsSR(conn, pool);
            } else if (pool.getType() == StoragePoolType.IscsiLUN) {
                getIscsiSR(conn, pool);
            } else if (pool.getType() == StoragePoolType.PreSetup) {
            } else {
                return new Answer(cmd, false, "The pool type: " + pool.getType().name() + " is not supported.");
            }
            return new Answer(cmd, true, "success");
        } catch (Exception e) {
            String msg = "Catch Exception " + e.getClass().getName() + ", create StoragePool failed due to " + e.toString() + " on host:" + _host.uuid + " pool: " + pool.getHost() + pool.getPath();
            s_logger.warn(msg, e);
            return new Answer(cmd, false, msg);
        }

    }

    protected String callHostPluginThroughMaster(Connection conn, String plugin, String cmd, String... params) {
        Map<String, String> args = new HashMap<String, String>();

        try {
            Map<Pool, Pool.Record> poolRecs = Pool.getAllRecords(conn);
            if (poolRecs.size() != 1) {
                throw new CloudRuntimeException("There are " + poolRecs.size() + " pool for host :" + _host.uuid);
            }
            Host master = poolRecs.values().iterator().next().master;
            for (int i = 0; i < params.length; i += 2) {
                args.put(params[i], params[i + 1]);
            }

            if (s_logger.isTraceEnabled()) {
                s_logger.trace("callHostPlugin executing for command " + cmd + " with " + getArgsString(args));
            }
            String result = master.callPlugin(conn, plugin, cmd, args);
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("callHostPlugin Result: " + result);
            }
            return result.replace("\n", "");
        } catch (Types.HandleInvalid e) {
            s_logger.warn("callHostPlugin failed for cmd: " + cmd + " with args " + getArgsString(args)
                    + " due to HandleInvalid clazz:" + e.clazz + ", handle:" + e.handle);
        } catch (XenAPIException e) {
            s_logger.warn("callHostPlugin failed for cmd: " + cmd + " with args " + getArgsString(args) + " due to "
                    + e.toString(), e);
        } catch (XmlRpcException e) {
            s_logger.warn("callHostPlugin failed for cmd: " + cmd + " with args " + getArgsString(args) + " due to "
                    + e.getMessage(), e);
        }
        return null;
    }


    protected String callHostPluginPremium(Connection conn, String cmd, String... params) {
        return callHostPlugin(conn, "vmopspremium", cmd, params);
    }

    protected String setupHeartbeatSr(Connection conn, SR sr, boolean force) throws XenAPIException, XmlRpcException {
        SR.Record srRec = sr.getRecord(conn);
        String srUuid = srRec.uuid;
        if (!srRec.shared || (!SRType.LVMOHBA.equals(srRec.type) && !SRType.LVMOISCSI.equals(srRec.type) && !SRType.NFS.equals(srRec.type) )) {
            return srUuid;
        }
        String result = null;
        Host host = Host.getByUuid(conn, _host.uuid);
        Set<String> tags = host.getTags(conn);
        if (force || !tags.contains("cloud-heartbeat-" + srUuid)) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Setting up the heartbeat sr for host " + _host.ip + " and sr " + srUuid);
            }
            Set<PBD> pbds = sr.getPBDs(conn);
            for (PBD pbd : pbds) {
                PBD.Record pbdr = pbd.getRecord(conn);
                if (!pbdr.currentlyAttached && pbdr.host.getUuid(conn).equals(_host.uuid)) {
                    pbd.plug(conn);
                    break;
                }
            }
            result = callHostPluginThroughMaster(conn, "vmopspremium", "setup_heartbeat_sr", "host", _host.uuid,
                    "sr", srUuid);
            if (result == null || !result.split("#")[1].equals("0")) {
                throw new CloudRuntimeException("Unable to setup heartbeat sr on SR " + srUuid + " due to " + result);
            }

            if (!tags.contains("cloud-heartbeat-" + srUuid)) {
                tags.add("cloud-heartbeat-" + srUuid);
                host.setTags(conn, tags);
            }
        }
        result = callHostPluginPremium(conn, "setup_heartbeat_file", "host", _host.uuid, "sr", srUuid, "add", "true");
        if (result == null || !result.split("#")[1].equals("0")) {
            throw new CloudRuntimeException("Unable to setup heartbeat file entry on SR " + srUuid + " due to "
                    + result);
        }
        return srUuid;
    }

    protected Answer execute(ModifyStoragePoolCommand cmd) {
        Connection conn = getConnection();
        StorageFilerTO pool = cmd.getPool();
        boolean add = cmd.getAdd();
        if( add ) {
            try {
                SR sr = getStorageRepository(conn, pool.getUuid());
                setupHeartbeatSr(conn, sr, false);
                long capacity = sr.getPhysicalSize(conn);
                long available = capacity - sr.getPhysicalUtilisation(conn);
                if (capacity == -1) {
                    String msg = "Pool capacity is -1! pool: " + pool.getHost() + pool.getPath();
                    s_logger.warn(msg);
                    return new Answer(cmd, false, msg);
                }
                Map<String, TemplateInfo> tInfo = new HashMap<String, TemplateInfo>();
                ModifyStoragePoolAnswer answer = new ModifyStoragePoolAnswer(cmd, capacity, available, tInfo);
                return answer;
            } catch (XenAPIException e) {
                String msg = "ModifyStoragePoolCommand add XenAPIException:" + e.toString() + " host:" + _host.uuid + " pool: " + pool.getHost() + pool.getPath();
                s_logger.warn(msg, e);
                return new Answer(cmd, false, msg);
            } catch (Exception e) {
                String msg = "ModifyStoragePoolCommand add XenAPIException:" + e.getMessage() + " host:" + _host.uuid + " pool: " + pool.getHost() + pool.getPath();
                s_logger.warn(msg, e);
                return new Answer(cmd, false, msg);
            }
        } else {
            try {
                SR sr = getStorageRepository(conn, pool.getUuid());
                String srUuid = sr.getUuid(conn);
                String result = callHostPluginPremium(conn, "setup_heartbeat_file", "host", _host.uuid, "sr", srUuid, "add", "false");
                if (result == null || !result.split("#")[1].equals("0")) {
                    throw new CloudRuntimeException("Unable to remove heartbeat file entry for SR " + srUuid + " due to "
                            + result);
                }
                return new Answer(cmd, true , "seccuss");
            } catch (XenAPIException e) {
                String msg = "ModifyStoragePoolCommand remove XenAPIException:" + e.toString() + " host:" + _host.uuid + " pool: " + pool.getHost() + pool.getPath();
                s_logger.warn(msg, e);
                return new Answer(cmd, false, msg);
            } catch (Exception e) {
                String msg = "ModifyStoragePoolCommand remove XenAPIException:" + e.getMessage() + " host:" + _host.uuid + " pool: " + pool.getHost() + pool.getPath();
                s_logger.warn(msg, e);
                return new Answer(cmd, false, msg);
            }
        }

    }

    protected boolean can_bridge_firewall(Connection conn) {
        return Boolean.valueOf(callHostPlugin(conn, "vmops", "can_bridge_firewall", "host_uuid", _host.uuid, "instance", _instance));
    }


    private Answer execute(OvsSetupBridgeCommand cmd) {
        Connection conn = getConnection();
        findOrCreateTunnelNetwork(conn, cmd.getKey());
        configureTunnelNetwork(conn, cmd.getNetworkId(), cmd.getHostId(), cmd.getKey());
        s_logger.debug("OVS Bridge configured");
        return new Answer(cmd, true, null);
    }

    private Answer execute(OvsDestroyBridgeCommand cmd) {
        Connection conn = getConnection();
        destroyTunnelNetwork(conn, cmd.getKey());
        s_logger.debug("OVS Bridge destroyed");
        return new Answer(cmd, true, null);
    }

    private Answer execute(OvsDestroyTunnelCommand cmd) {
        Connection conn = getConnection();
        try {
            Network nw = findOrCreateTunnelNetwork(conn, cmd.getNetworkId());
            if (nw == null) {
                s_logger.warn("Unable to find tunnel network for GRE key:" + cmd.getKey());
                return new Answer(cmd, false, "No network found");
            }

            String bridge = nw.getBridge(conn);
            String result = callHostPlugin(conn, "ovstunnel", "destroy_tunnel", "bridge", bridge, "in_port", cmd.getInPortName());
            if (result.equalsIgnoreCase("SUCCESS")) {
                return new Answer(cmd, true, result);
            } else {
                return new Answer(cmd, false, result);
            }
        } catch (Exception e) {
            s_logger.warn("caught execption when destroy ovs tunnel", e);
            return new Answer(cmd, false, e.getMessage());
        }
    }


    private Answer execute(UpdateHostPasswordCommand cmd) {
        _password.add(cmd.getNewPassword());
        return new Answer(cmd, true, null);
    }

    private OvsCreateTunnelAnswer execute(OvsCreateTunnelCommand cmd) {
        Connection conn = getConnection();
        String bridge = "unknown";
        try {
            Network nw = findOrCreateTunnelNetwork(conn, cmd.getKey());
            if (nw == null) {
                s_logger.debug("Error during bridge setup");
                return new OvsCreateTunnelAnswer(cmd, false, "Cannot create network", bridge);
            }

            configureTunnelNetwork(conn, cmd.getNetworkId(), cmd.getFrom(), cmd.getKey());            
            bridge = nw.getBridge(conn);
            String result = callHostPlugin(conn, "ovstunnel", "create_tunnel", "bridge", bridge, "remote_ip", cmd.getRemoteIp(), 
                    "key", cmd.getKey().toString(), "from", cmd.getFrom().toString(), "to", cmd.getTo().toString());
            String[] res = result.split(":");
            if (res.length == 2 && res[0].equalsIgnoreCase("SUCCESS")) {
                return new OvsCreateTunnelAnswer(cmd, true, result, res[1], bridge);
            } else {
                return new OvsCreateTunnelAnswer(cmd, false, result, bridge);
            }
        } catch (Exception e) {
            s_logger.debug("Error during tunnel setup");
            s_logger.warn("Caught execption when creating ovs tunnel", e);
            return new OvsCreateTunnelAnswer(cmd, false, e.getMessage(), bridge);
        }
    }

    private Answer execute(OvsDeleteFlowCommand cmd) {
        _isOvs = true;

        Connection conn = getConnection();
        try {
            Network nw = setupvSwitchNetwork(conn);
            String bridge = nw.getBridge(conn);
            String result = callHostPlugin(conn, "ovsgre", "ovs_delete_flow", "bridge", bridge,
                    "vmName", cmd.getVmName());

            if (result.equalsIgnoreCase("SUCCESS")) {
                return new Answer(cmd, true, "success to delete flows for " + cmd.getVmName());
            } else {
                return new Answer(cmd, false, result);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Answer(cmd, false, "failed to delete flow for " + cmd.getVmName());
    }

    private List<Pair<String, Long>> ovsFullSyncStates() {
        Connection conn = getConnection();
        try {
            String result = callHostPlugin(conn, "ovsgre", "ovs_get_vm_log", "host_uuid", _host.uuid);
            String [] logs = result != null ?result.split(";"): new String [0];
            List<Pair<String, Long>> states = new ArrayList<Pair<String, Long>>();
            for (String log: logs){
                String [] info = log.split(",");
                if (info.length != 5) {
                    s_logger.warn("Wrong element number in ovs log(" + log +")");
                    continue;
                }

                //','.join([bridge, vmName, vmId, seqno, tag])
                try {
                    states.add(new Pair<String,Long>(info[0], Long.parseLong(info[3])));
                } catch (NumberFormatException nfe) {
                    states.add(new Pair<String,Long>(info[0], -1L));
                }
            }

            return states;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private OvsSetTagAndFlowAnswer execute(OvsSetTagAndFlowCommand cmd) {
        _isOvs = true;

        Connection conn = getConnection();
        try {
            Network nw = setupvSwitchNetwork(conn);
            String bridge = nw.getBridge(conn);

            /*If VM is domainRouter, this will try to set flow and tag on its
             * none guest network nic. don't worry, it will fail silently at host
             * plugin side
             */
            String result = callHostPlugin(conn, "ovsgre", "ovs_set_tag_and_flow", "bridge", bridge,
                    "vmName", cmd.getVmName(), "tag", cmd.getTag(),
                    "vlans", cmd.getVlans(), "seqno", cmd.getSeqNo());
            s_logger.debug("set flow for " + cmd.getVmName() + " " + result);

            if (result.equalsIgnoreCase("SUCCESS")) {
                return new OvsSetTagAndFlowAnswer(cmd, true, result);
            } else {
                return new OvsSetTagAndFlowAnswer(cmd, false, result);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new OvsSetTagAndFlowAnswer(cmd, false, "EXCEPTION");
    }


    private OvsFetchInterfaceAnswer execute(OvsFetchInterfaceCommand cmd) {

        String label = cmd.getLabel();
        s_logger.debug("Will look for network with name-label:" + label + " on host " + _host.ip);
        Connection conn = getConnection();
        try {
            XsLocalNetwork nw = this.getNetworkByName(conn, label);
            s_logger.debug("Network object:" + nw.getNetwork().getUuid(conn));
            PIF pif = nw.getPif(conn);
            Record pifRec = pif.getRecord(conn);
            s_logger.debug("PIF object:" + pifRec.uuid + "(" + pifRec.device + ")");
            return new OvsFetchInterfaceAnswer(cmd, true, "Interface " + pifRec.device + " retrieved successfully", 
                    pifRec.IP, pifRec.netmask, pifRec.MAC);
        } catch (Exception e) {
            e.printStackTrace();
            s_logger.error("An error occurred while fetching the interface for " +
                    label + " on host " + _host.ip + ":" + e.toString() + 
                    "(" + e.getClass() + ")");
            return new OvsFetchInterfaceAnswer(cmd, false, "EXCEPTION:" + e.getMessage());
        }
    }


    private OvsCreateGreTunnelAnswer execute(OvsCreateGreTunnelCommand cmd) {
        _isOvs = true;

        Connection conn = getConnection();
        String bridge = "unkonwn";
        try {
            Network nw = setupvSwitchNetwork(conn);
            bridge = nw.getBridge(conn);

            String result = callHostPlugin(conn, "ovsgre", "ovs_create_gre", "bridge", bridge,
                    "remoteIP", cmd.getRemoteIp(), "greKey", cmd.getKey(), "from",
                    Long.toString(cmd.getFrom()), "to", Long.toString(cmd.getTo()));
            String[] res = result.split(":");
            if (res.length != 2 || (res.length == 2 && res[1].equalsIgnoreCase("[]"))) {
                return new OvsCreateGreTunnelAnswer(cmd, false, result,
                        _host.ip, bridge);
            } else {
                return new OvsCreateGreTunnelAnswer(cmd, true, result, _host.ip, bridge, Integer.parseInt(res[1]));
            }
        } catch (Exception e) {
            e.printStackTrace();
            s_logger.error("An error occurred while creating a GRE tunnel to " +
                    cmd.getRemoteIp() + " on host " + _host.ip + ":" + e.getMessage() + 
                    "(" + e.getClass() + ")");

        }

        return new OvsCreateGreTunnelAnswer(cmd, false, "EXCEPTION", _host.ip, bridge);
    }

    private Answer execute(SecurityGroupRulesCmd cmd) {
        Connection conn = getConnection();
        if (s_logger.isTraceEnabled()) {
            s_logger.trace("Sending network rules command to " + _host.ip);
        }

        if (!_canBridgeFirewall) {
            s_logger.warn("Host " + _host.ip + " cannot do bridge firewalling");
            return new SecurityGroupRuleAnswer(cmd, false, 
                    "Host " + _host.ip + " cannot do bridge firewalling",
                    SecurityGroupRuleAnswer.FailureReason.CANNOT_BRIDGE_FIREWALL);
        }

        String result = callHostPlugin(conn, "vmops", "network_rules",
                "vmName", cmd.getVmName(),
                "vmIP", cmd.getGuestIp(),
                "vmMAC", cmd.getGuestMac(),
                "vmID", Long.toString(cmd.getVmId()),
                "signature", cmd.getSignature(),
                "seqno", Long.toString(cmd.getSeqNum()),
                "deflated", "true",
                "rules", cmd.compressStringifiedRules(),
                "secIps", cmd.getSecIpsString());

        if (result == null || result.isEmpty() || !Boolean.parseBoolean(result)) {
            s_logger.warn("Failed to program network rules for vm " + cmd.getVmName());
            return new SecurityGroupRuleAnswer(cmd, false, "programming network rules failed");
        } else {
            s_logger.info("Programmed network rules for vm " + cmd.getVmName() + " guestIp=" + cmd.getGuestIp() + ", ingress numrules=" + cmd.getIngressRuleSet().length + ", egress numrules=" + cmd.getEgressRuleSet().length);
            return new SecurityGroupRuleAnswer(cmd);
        }
    }

    protected Answer execute(DeleteStoragePoolCommand cmd) {
        Connection conn = getConnection();
        StorageFilerTO poolTO = cmd.getPool();
        try {
            SR sr = getStorageRepository(conn, poolTO.getUuid());
            removeSR(conn, sr);
            Answer answer = new Answer(cmd, true, "success");
            return answer;
        } catch (Exception e) {
            String msg = "DeleteStoragePoolCommand XenAPIException:" + e.getMessage() + " host:" + _host.uuid + " pool: " + poolTO.getHost() + poolTO.getPath();
            s_logger.warn(msg, e);
            return new Answer(cmd, false, msg);
        }

    }

    public Connection getConnection() {
        return _connPool.connect(_host.uuid, _host.pool, _host.ip, _username, _password, _wait);
    }

    protected void fillHostInfo(Connection conn, StartupRoutingCommand cmd) {
        final StringBuilder caps = new StringBuilder();
        try {

            Host host = Host.getByUuid(conn, _host.uuid);
            Host.Record hr = host.getRecord(conn);

            Map<String, String> details = cmd.getHostDetails();
            if (details == null) {
                details = new HashMap<String, String>();
            }

            String productBrand = hr.softwareVersion.get("product_brand");
            if (productBrand == null) {
                productBrand = hr.softwareVersion.get("platform_name");
            }
            details.put("product_brand", productBrand);
            details.put("product_version", _host.product_version);

            if( hr.softwareVersion.get("product_version_text_short") != null ) {
                details.put("product_version_text_short", hr.softwareVersion.get("product_version_text_short"));
                cmd.setHypervisorVersion(hr.softwareVersion.get("product_version_text_short"));                

                cmd.setHypervisorVersion(_host.product_version);
            }
            if (_privateNetworkName != null) {
                details.put("private.network.device", _privateNetworkName);
            }

            details.put("can_bridge_firewall", Boolean.toString(_canBridgeFirewall));
            cmd.setHostDetails(details);
            cmd.setName(hr.nameLabel);
            cmd.setGuid(_host.uuid);
            cmd.setPool(_host.pool);
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

            cmd.setSpeed(_host.speed);
            cmd.setCpus(_host.cpus);

            HostMetrics hm = host.getMetrics(conn);

            long ram = 0;
            long dom0Ram = 0;
            ram = hm.getMemoryTotal(conn);
            Set<VM> vms = host.getResidentVMs(conn);
            for (VM vm : vms) {
                if (vm.getIsControlDomain(conn)) {
                    dom0Ram = vm.getMemoryDynamicMax(conn);
                    break;
                }
            }

            ram = (long) ((ram - dom0Ram - _xs_memory_used) * _xs_virtualization_factor);
            cmd.setMemory(ram);
            cmd.setDom0MinMemory(dom0Ram);

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Total Ram: " + ram + " dom0 Ram: " + dom0Ram);
            }

            PIF pif = PIF.getByUuid(conn, _host.privatePif);
            PIF.Record pifr = pif.getRecord(conn);
            if (pifr.IP != null && pifr.IP.length() > 0) {
                cmd.setPrivateIpAddress(pifr.IP);
                cmd.setPrivateMacAddress(pifr.MAC);
                cmd.setPrivateNetmask(pifr.netmask);
            } else {
                cmd.setPrivateIpAddress(_host.ip);
                cmd.setPrivateMacAddress(pifr.MAC);
                cmd.setPrivateNetmask("255.255.255.0");
            }

            pif = PIF.getByUuid(conn, _host.publicPif);
            pifr = pif.getRecord(conn);
            if (pifr.IP != null && pifr.IP.length() > 0) {
                cmd.setPublicIpAddress(pifr.IP);
                cmd.setPublicMacAddress(pifr.MAC);
                cmd.setPublicNetmask(pifr.netmask);
            }

            if (_host.storagePif1 != null) {
                pif = PIF.getByUuid(conn, _host.storagePif1);
                pifr = pif.getRecord(conn);
                if (pifr.IP != null && pifr.IP.length() > 0) {
                    cmd.setStorageIpAddress(pifr.IP);
                    cmd.setStorageMacAddress(pifr.MAC);
                    cmd.setStorageNetmask(pifr.netmask);
                }
            }

            if (_host.storagePif2 != null) {
                pif = PIF.getByUuid(conn, _host.storagePif2);
                pifr = pif.getRecord(conn);
                if (pifr.IP != null && pifr.IP.length() > 0) {
                    cmd.setStorageIpAddressDeux(pifr.IP);
                    cmd.setStorageMacAddressDeux(pifr.MAC);
                    cmd.setStorageNetmaskDeux(pifr.netmask);
                }
            }

            Map<String, String> configs = hr.otherConfig;
            cmd.setIqn(configs.get("iscsi_iqn"));

            cmd.setPod(_pod);
            cmd.setVersion(CitrixResourceBase.class.getPackage().getImplementationVersion());

        } catch (final XmlRpcException e) {
            throw new CloudRuntimeException("XML RPC Exception" + e.getMessage(), e);
        } catch (XenAPIException e) {
            throw new CloudRuntimeException("XenAPIException" + e.toString(), e);
        }
    }

    public CitrixResourceBase() {
    }


    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _name = name;

        try {
            _dcId = Long.parseLong((String) params.get("zone"));
        } catch (NumberFormatException e) {
            throw new ConfigurationException("Unable to get the zone " + params.get("zone"));
        }

        _host.uuid = (String) params.get("guid");

        _name = _host.uuid;
        _host.ip = (String) params.get("ipaddress");

        _username = (String) params.get("username");
        _password.add((String) params.get("password"));
        _pod = (String) params.get("pod");
        _cluster = (String)params.get("cluster");
        _privateNetworkName = (String) params.get("private.network.device");
        _publicNetworkName = (String) params.get("public.network.device");
        _guestNetworkName = (String)params.get("guest.network.device");
        _instance = (String) params.get("instance.name");

        _linkLocalPrivateNetworkName = (String) params.get("private.linkLocal.device");
        if (_linkLocalPrivateNetworkName == null) {
            _linkLocalPrivateNetworkName = "cloud_link_local_network";
        }

        _storageNetworkName1 = (String) params.get("storage.network.device1");
        _storageNetworkName2 = (String) params.get("storage.network.device2");

        _heartbeatInterval = NumbersUtil.parseInt((String) params.get("xen.heartbeat.interval"), 60);

        String value = (String) params.get("wait");
        _wait = NumbersUtil.parseInt(value, 600);

        value = (String) params.get("migratewait");
        _migratewait = NumbersUtil.parseInt(value, 3600);

        _maxNics = NumbersUtil.parseInt((String) params.get("xen.nics.max"), 7);

        if (_pod == null) {
            throw new ConfigurationException("Unable to get the pod");
        }

        if (_host.ip == null) {
            throw new ConfigurationException("Unable to get the host address");
        }

        if (_username == null) {
            throw new ConfigurationException("Unable to get the username");
        }

        if (_password == null) {
            throw new ConfigurationException("Unable to get the password");
        }

        if (_host.uuid == null) {
            throw new ConfigurationException("Unable to get the uuid");
        }

        CheckXenHostInfo();

        this.storageResource = getStorageResource();
        return true;

    }

    protected XenServerStorageResource getStorageResource() {
        return new XenServerStorageResource(this);
    }

    private void CheckXenHostInfo() throws ConfigurationException {
        Connection conn = _connPool.slaveConnect(_host.ip, _username, _password);
        if( conn == null ) {
            throw new ConfigurationException("Can not create slave connection to " + _host.ip);
        }
        try {
            Host.Record hostRec = null;
            try {
                Host host = Host.getByUuid(conn, _host.uuid);
                hostRec = host.getRecord(conn);
                Pool.Record poolRec = Pool.getAllRecords(conn).values().iterator().next();
                _host.pool = poolRec.uuid;

            } catch (Exception e) {
                throw new ConfigurationException("Can not get host information from " + _host.ip);
            }
            if( !hostRec.address.equals(_host.ip) ) {
                String msg = "Host " + _host.ip + " seems be reinstalled, please remove this host and readd";
                s_logger.error(msg);
                throw new ConfigurationException(msg);
            }
        } finally {
            try {
                Session.localLogout(conn);
            } catch (Exception e) {
            }
        }
    }

    void destroyVDI(Connection conn, VDI vdi) {
        try {
            vdi.destroy(conn);
        } catch (Exception e) {
            String msg = "destroy VDI failed due to " + e.toString();
            s_logger.warn(msg);
        }
    }

    public CreateAnswer execute(CreateCommand cmd) {
        Connection conn = getConnection();
        StorageFilerTO pool = cmd.getPool();
        DiskProfile dskch = cmd.getDiskCharacteristics();
        VDI vdi = null;
        try {
            SR poolSr = getStorageRepository(conn, pool.getUuid());
            if (cmd.getTemplateUrl() != null) {
                VDI tmpltvdi = null;

                tmpltvdi = getVDIbyUuid(conn, cmd.getTemplateUrl());
                vdi = tmpltvdi.createClone(conn, new HashMap<String, String>());
                vdi.setNameLabel(conn, dskch.getName());
            } else {
                VDI.Record vdir = new VDI.Record();
                vdir.nameLabel = dskch.getName();
                vdir.SR = poolSr;
                vdir.type = Types.VdiType.USER;

                vdir.virtualSize = dskch.getSize();
                vdi = VDI.create(conn, vdir);
            }

            VDI.Record vdir;
            vdir = vdi.getRecord(conn);
            s_logger.debug("Succesfully created VDI for " + cmd + ".  Uuid = " + vdir.uuid);

            VolumeTO vol = new VolumeTO(cmd.getVolumeId(), dskch.getType(), pool.getType(), pool.getUuid(), vdir.nameLabel,
                    pool.getPath(), vdir.uuid, vdir.virtualSize, null);
            return new CreateAnswer(cmd, vol);
        } catch (Exception e) {
            s_logger.warn("Unable to create volume; Pool=" + pool + "; Disk: " + dskch, e);
            return new CreateAnswer(cmd, e);
        }
    }

    public Answer execute(ResizeVolumeCommand cmd) {
        Connection conn = getConnection();
        StorageFilerTO pool = cmd.getPool();
        String volid = cmd.getPath();
        long newSize = cmd.getNewSize();

        try {
            VDI vdi = getVDIbyUuid(conn, volid);
            vdi.resize(conn, newSize);
            return new ResizeVolumeAnswer(cmd, true, "success", newSize);
        } catch (Exception e) {
            s_logger.warn("Unable to resize volume",e);
            String error = "failed to resize volume:"  +e;
            return new ResizeVolumeAnswer(cmd, false, error );
        }
    }

    protected SR getISOSRbyVmName(Connection conn, String vmName) {
        try {
            Set<SR> srs = SR.getByNameLabel(conn, vmName + "-ISO");
            if (srs.size() == 0) {
                return null;
            } else if (srs.size() == 1) {
                return srs.iterator().next();
            } else {
                String msg = "getIsoSRbyVmName failed due to there are more than 1 SR having same Label";
                s_logger.warn(msg);
            }
        } catch (XenAPIException e) {
            String msg = "getIsoSRbyVmName failed due to " + e.toString();
            s_logger.warn(msg, e);
        } catch (Exception e) {
            String msg = "getIsoSRbyVmName failed due to " + e.getMessage();
            s_logger.warn(msg, e);
        }
        return null;
    }

    protected SR createNfsSRbyURI(Connection conn, URI uri, boolean shared) {
        try {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Creating a " + (shared ? "shared SR for " : "not shared SR for ") + uri);
            }

            Map<String, String> deviceConfig = new HashMap<String, String>();
            String path = uri.getPath();
            path = path.replace("//", "/");
            deviceConfig.put("server", uri.getHost());
            deviceConfig.put("serverpath", path);
            String name = UUID.nameUUIDFromBytes(new String(uri.getHost() + path).getBytes()).toString();
            if (!shared) {
                Set<SR> srs = SR.getByNameLabel(conn, name);
                for (SR sr : srs) {
                    SR.Record record = sr.getRecord(conn);
                    if (SRType.NFS.equals(record.type) && record.contentType.equals("user") && !record.shared) {
                        removeSRSync(conn, sr);
                    }
                }
            }

            Host host = Host.getByUuid(conn, _host.uuid);

            SR sr = SR.create(conn, host, deviceConfig, new Long(0), name, uri.getHost() + uri.getPath(), SRType.NFS.toString(), "user", shared, new HashMap<String, String>());
            if( !checkSR(conn, sr) ) {
                throw new Exception("no attached PBD");
            }
            if (s_logger.isDebugEnabled()) {
                s_logger.debug(logX(sr, "Created a SR; UUID is " + sr.getUuid(conn) + " device config is " + deviceConfig));
            }
            sr.scan(conn);
            return sr;
        } catch (XenAPIException e) {
            String msg = "Can not create second storage SR mountpoint: " + uri.getHost() + uri.getPath() + " due to " + e.toString();
            s_logger.warn(msg, e);
            throw new CloudRuntimeException(msg, e);
        } catch (Exception e) {
            String msg = "Can not create second storage SR mountpoint: " + uri.getHost() + uri.getPath() + " due to " + e.getMessage();
            s_logger.warn(msg, e);
            throw new CloudRuntimeException(msg, e);
        }
    }

    protected SR createIsoSRbyURI(Connection conn, URI uri, String vmName, boolean shared) {
        try {
            Map<String, String> deviceConfig = new HashMap<String, String>();
            String path = uri.getPath();
            path = path.replace("//", "/");
            deviceConfig.put("location", uri.getHost() + ":" + path);
            Host host = Host.getByUuid(conn, _host.uuid);
            SR sr = SR.create(conn, host, deviceConfig, new Long(0), uri.getHost() + path, "iso", "iso", "iso", shared, new HashMap<String, String>());
            sr.setNameLabel(conn, vmName + "-ISO");
            sr.setNameDescription(conn, deviceConfig.get("location"));

            sr.scan(conn);
            return sr;
        } catch (XenAPIException e) {
            String msg = "createIsoSRbyURI failed! mountpoint: " + uri.getHost() + uri.getPath() + " due to " + e.toString();
            s_logger.warn(msg, e);
            throw new CloudRuntimeException(msg, e);
        } catch (Exception e) {
            String msg = "createIsoSRbyURI failed! mountpoint: " + uri.getHost() + uri.getPath() + " due to " + e.getMessage();
            s_logger.warn(msg, e);
            throw new CloudRuntimeException(msg, e);
        }
    }

    protected VDI getVDIbyLocationandSR(Connection conn, String loc, SR sr) {
        try {
            Set<VDI> vdis = sr.getVDIs(conn);
            for (VDI vdi : vdis) {
                if (vdi.getLocation(conn).startsWith(loc)) {
                    return vdi;
                }
            }

            String msg = "can not getVDIbyLocationandSR " + loc;
            s_logger.warn(msg);
            return null;
        } catch (XenAPIException e) {
            String msg = "getVDIbyLocationandSR exception " + loc + " due to " + e.toString();
            s_logger.warn(msg, e);
            throw new CloudRuntimeException(msg, e);
        } catch (Exception e) {
            String msg = "getVDIbyLocationandSR exception " + loc + " due to " + e.getMessage();
            s_logger.warn(msg, e);
            throw new CloudRuntimeException(msg, e);
        }

    }

    protected VDI getVDIbyUuid(Connection conn, String uuid) {
        try {
            return VDI.getByUuid(conn, uuid);
        } catch (Exception e) {
            String msg = "Catch Exception " + e.getClass().getName() + " :VDI getByUuid for uuid: " + uuid + " failed due to " + e.toString();
            s_logger.debug(msg);
            throw new CloudRuntimeException(msg, e);
        }
    }

    protected SR getIscsiSR(Connection conn, StorageFilerTO pool) {
        synchronized (pool.getUuid().intern()) {
            Map<String, String> deviceConfig = new HashMap<String, String>();
            try {
                String target = pool.getHost();
                String path = pool.getPath();
                if (path.endsWith("/")) {
                    path = path.substring(0, path.length() - 1);
                }

                String tmp[] = path.split("/");
                if (tmp.length != 3) {
                    String msg = "Wrong iscsi path " + pool.getPath() + " it should be /targetIQN/LUN";
                    s_logger.warn(msg);
                    throw new CloudRuntimeException(msg);
                }
                String targetiqn = tmp[1].trim();
                String lunid = tmp[2].trim();
                String scsiid = "";

                Set<SR> srs = SR.getByNameLabel(conn, pool.getUuid());
                for (SR sr : srs) {
                    if (!SRType.LVMOISCSI.equals(sr.getType(conn))) {
                        continue;
                    }
                    Set<PBD> pbds = sr.getPBDs(conn);
                    if (pbds.isEmpty()) {
                        continue;
                    }
                    PBD pbd = pbds.iterator().next();
                    Map<String, String> dc = pbd.getDeviceConfig(conn);
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
                        throw new CloudRuntimeException("There is a SR using the same configuration target:" + dc.get("target") +  ",  targetIQN:"
                                + dc.get("targetIQN")  + ", lunid:" + dc.get("lunid") + " for pool " + pool.getUuid() + "on host:" + _host.uuid);
                    }
                }
                deviceConfig.put("target", target);
                deviceConfig.put("targetIQN", targetiqn);

                Host host = Host.getByUuid(conn, _host.uuid);
                Map<String, String> smConfig = new HashMap<String, String>();
                String type = SRType.LVMOISCSI.toString();
                String poolId = Long.toString(pool.getId());
                SR sr = null;
                try {
                    sr = SR.create(conn, host, deviceConfig, new Long(0), pool.getUuid(), poolId, type, "user", true,
                            smConfig);
                } catch (XenAPIException e) {
                    String errmsg = e.toString();
                    if (errmsg.contains("SR_BACKEND_FAILURE_107")) {
                        String lun[] = errmsg.split("<LUN>");
                        boolean found = false;
                        for (int i = 1; i < lun.length; i++) {
                            int blunindex = lun[i].indexOf("<LUNid>") + 7;
                            int elunindex = lun[i].indexOf("</LUNid>");
                            String ilun = lun[i].substring(blunindex, elunindex);
                            ilun = ilun.trim();
                            if (ilun.equals(lunid)) {
                                int bscsiindex = lun[i].indexOf("<SCSIid>") + 8;
                                int escsiindex = lun[i].indexOf("</SCSIid>");
                                scsiid = lun[i].substring(bscsiindex, escsiindex);
                                scsiid = scsiid.trim();
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            String msg = "can not find LUN " + lunid + " in " + errmsg;
                            s_logger.warn(msg);
                            throw new CloudRuntimeException(msg);
                        }
                    } else {
                        String msg = "Unable to create Iscsi SR  " + deviceConfig + " due to  " + e.toString();
                        s_logger.warn(msg, e);
                        throw new CloudRuntimeException(msg, e);
                    }
                }
                deviceConfig.put("SCSIid", scsiid);

                String result = SR.probe(conn, host, deviceConfig, type , smConfig);
                String pooluuid = null;
                if( result.indexOf("<UUID>") != -1) {
                    pooluuid = result.substring(result.indexOf("<UUID>") + 6, result.indexOf("</UUID>")).trim();
                }
                if( pooluuid == null || pooluuid.length() != 36) {
                    sr = SR.create(conn, host, deviceConfig, new Long(0), pool.getUuid(), poolId, type, "user", true,
                            smConfig);
                } else {
                    sr = SR.introduce(conn, pooluuid, pool.getUuid(), poolId,
                            type, "user", true, smConfig);
                    Pool.Record pRec = XenServerConnectionPool.getPoolRecord(conn);
                    PBD.Record rec = new PBD.Record();
                    rec.deviceConfig = deviceConfig;
                    rec.host = pRec.master;
                    rec.SR = sr;
                    PBD pbd = PBD.create(conn, rec);
                    pbd.plug(conn);
                }
                sr.scan(conn);
                return sr;
            } catch (XenAPIException e) {
                String msg = "Unable to create Iscsi SR  " + deviceConfig + " due to  " + e.toString();
                s_logger.warn(msg, e);
                throw new CloudRuntimeException(msg, e);
            } catch (Exception e) {
                String msg = "Unable to create Iscsi SR  " + deviceConfig + " due to  " + e.getMessage();
                s_logger.warn(msg, e);
                throw new CloudRuntimeException(msg, e);
            }
        }
    }

    protected SR getNfsSR(Connection conn, StorageFilerTO pool) {
        Map<String, String> deviceConfig = new HashMap<String, String>();
        try {
            String server = pool.getHost();
            String serverpath = pool.getPath();
            serverpath = serverpath.replace("//", "/");
            Set<SR> srs = SR.getAll(conn);
            for (SR sr : srs) {
                if (!SRType.NFS.equals(sr.getType(conn))) {
                    continue;
                }

                Set<PBD> pbds = sr.getPBDs(conn);
                if (pbds.isEmpty()) {
                    continue;
                }

                PBD pbd = pbds.iterator().next();

                Map<String, String> dc = pbd.getDeviceConfig(conn);

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
                    throw new CloudRuntimeException("There is a SR using the same configuration server:" + dc.get("server") + ", serverpath:"
                            + dc.get("serverpath") + " for pool " + pool.getUuid() + "on host:" + _host.uuid);
                }

            }
            deviceConfig.put("server", server);
            deviceConfig.put("serverpath", serverpath);
            Host host = Host.getByUuid(conn, _host.uuid);
            SR sr = SR.create(conn, host, deviceConfig, new Long(0), pool.getUuid(), Long.toString(pool.getId()), SRType.NFS.toString(), "user", true,
                    new HashMap<String, String>());
            sr.scan(conn);
            return sr;
        } catch (XenAPIException e) {
            throw new CloudRuntimeException("Unable to create NFS SR " + pool.toString(), e);
        } catch (XmlRpcException e) {
            throw new CloudRuntimeException("Unable to create NFS SR " + pool.toString(), e);
        }
    }

    public Answer execute(DestroyCommand cmd) {
        Connection conn = getConnection();
        VolumeTO vol = cmd.getVolume();
        // Look up the VDI
        String volumeUUID = vol.getPath();
        VDI vdi = null;
        try {
            vdi = getVDIbyUuid(conn, volumeUUID);
        } catch (Exception e) {
            return new Answer(cmd, true, "Success");
        }
        Set<VBD> vbds = null;
        try {
            vbds = vdi.getVBDs(conn);
        } catch (Exception e) {
            String msg = "VDI getVBDS for " + volumeUUID + " failed due to " + e.toString();
            s_logger.warn(msg, e);
            return new Answer(cmd, false, msg);
        }
        for (VBD vbd : vbds) {
            try {
                vbd.unplug(conn);
                vbd.destroy(conn);
            } catch (Exception e) {
                String msg = "VM destroy for " + volumeUUID + "  failed due to " + e.toString();
                s_logger.warn(msg, e);
                return new Answer(cmd, false, msg);
            }
        }
        try {
            Set<VDI> snapshots = vdi.getSnapshots(conn);
            for( VDI snapshot: snapshots ) {
                snapshot.destroy(conn);
            }
            vdi.destroy(conn);
        } catch (Exception e) {
            String msg = "VDI destroy for " + volumeUUID + " failed due to " + e.toString();
            s_logger.warn(msg, e);
            return new Answer(cmd, false, msg);
        }

        return new Answer(cmd, true, "Success");
    }

    public CopyVolumeAnswer execute(final CopyVolumeCommand cmd) {
        Connection conn = getConnection();
        String volumeUUID = cmd.getVolumePath();
        StorageFilerTO poolTO = cmd.getPool();
        String secondaryStorageURL = cmd.getSecondaryStorageURL();
        boolean toSecondaryStorage = cmd.toSecondaryStorage();
        int wait = cmd.getWait();
        try {
            URI uri = new URI(secondaryStorageURL);
            String remoteVolumesMountPath = uri.getHost() + ":" + uri.getPath() + "/volumes/";
            String volumeFolder = String.valueOf(cmd.getVolumeId()) + "/";
            String mountpoint = remoteVolumesMountPath + volumeFolder;
            SR primaryStoragePool = getStorageRepository(conn, poolTO.getUuid());
            String srUuid = primaryStoragePool.getUuid(conn);
            if (toSecondaryStorage) {
                // Create the volume folder
                if (!createSecondaryStorageFolder(conn, remoteVolumesMountPath, volumeFolder)) {
                    throw new InternalErrorException("Failed to create the volume folder.");
                }
                SR secondaryStorage = null;
                try {
                    // Create a SR for the volume UUID folder
                    secondaryStorage = createNfsSRbyURI(conn, new URI(secondaryStorageURL + "/volumes/" + volumeFolder), false);
                    // Look up the volume on the source primary storage pool
                    VDI srcVolume = getVDIbyUuid(conn, volumeUUID);
                    // Copy the volume to secondary storage
                    VDI destVolume = cloudVDIcopy(conn, srcVolume, secondaryStorage, wait);
                    String destVolumeUUID = destVolume.getUuid(conn);
                    return new CopyVolumeAnswer(cmd, true, null, null, destVolumeUUID);
                } finally {
                    removeSR(conn, secondaryStorage);
                }
            } else {
                try {
                    String volumePath = mountpoint + "/" + volumeUUID + ".vhd";
                    String uuid = copy_vhd_from_secondarystorage(conn, volumePath, srUuid, wait );
                    return new CopyVolumeAnswer(cmd, true, null, srUuid, uuid);
                } finally {
                    deleteSecondaryStorageFolder(conn, remoteVolumesMountPath, volumeFolder);
                }
            }
        } catch (Exception e) {
            String msg = "Catch Exception " + e.getClass().getName() + " due to " + e.toString();
            s_logger.warn(msg, e);
            return new CopyVolumeAnswer(cmd, false, msg, null, null);
        }
    }


    protected AttachVolumeAnswer execute(final AttachVolumeCommand cmd) {
        Connection conn = getConnection();
        boolean attach = cmd.getAttach();
        String vmName = cmd.getVmName();
        Long deviceId = cmd.getDeviceId();

        String errorMsg;
        if (attach) {
            errorMsg = "Failed to attach volume";
        } else {
            errorMsg = "Failed to detach volume";
        }

        try {
            // Look up the VDI
            VDI vdi = mount(conn, cmd.getPooltype(), cmd.getVolumeFolder(),cmd.getVolumePath());
            // Look up the VM
            VM vm = getVM(conn, vmName);
            /* For HVM guest, if no pv driver installed, no attach/detach */
            boolean isHVM;
            if (vm.getPVBootloader(conn).equalsIgnoreCase("")) {
                isHVM = true;
            } else {
                isHVM = false;
            }
            VMGuestMetrics vgm = vm.getGuestMetrics(conn);
            boolean pvDrvInstalled = false;
            if (!isRefNull(vgm) && vgm.getPVDriversUpToDate(conn)) {
                pvDrvInstalled = true;
            }
            if (isHVM && !pvDrvInstalled) {
                s_logger.warn(errorMsg + ": You attempted an operation on a VM which requires PV drivers to be installed but the drivers were not detected");
                return new AttachVolumeAnswer(cmd, "You attempted an operation that requires PV drivers to be installed on the VM. Please install them by inserting xen-pv-drv.iso.");
            }
            if (attach) {
                // Figure out the disk number to attach the VM to
                String diskNumber = null;
                if( deviceId != null ) {
                    if( deviceId.longValue() == 3 ) {
                        String msg = "Device 3 is reserved for CD-ROM, choose other device";
                        return new AttachVolumeAnswer(cmd,msg);
                    }
                    if(isDeviceUsed(conn, vm, deviceId)) {
                        String msg = "Device " + deviceId + " is used in VM " + vmName;
                        return new AttachVolumeAnswer(cmd,msg);
                    }
                    diskNumber = deviceId.toString();
                } else {
                    diskNumber = getUnusedDeviceNum(conn, vm);
                }
                // Create a new VBD
                VBD.Record vbdr = new VBD.Record();
                vbdr.VM = vm;
                vbdr.VDI = vdi;
                vbdr.bootable = false;
                vbdr.userdevice = diskNumber;
                vbdr.mode = Types.VbdMode.RW;
                vbdr.type = Types.VbdType.DISK;
                vbdr.unpluggable = true;
                VBD vbd = VBD.create(conn, vbdr);

                // Attach the VBD to the VM
                vbd.plug(conn);

                // Update the VDI's label to include the VM name
                vdi.setNameLabel(conn, vmName + "-DATA");

                return new AttachVolumeAnswer(cmd, Long.parseLong(diskNumber));
            } else {
                // Look up all VBDs for this VDI
                Set<VBD> vbds = vdi.getVBDs(conn);

                // Detach each VBD from its VM, and then destroy it
                for (VBD vbd : vbds) {
                    VBD.Record vbdr = vbd.getRecord(conn);

                    if (vbdr.currentlyAttached) {
                        vbd.unplug(conn);
                    }

                    vbd.destroy(conn);
                }

                // Update the VDI's label to be "detached"
                vdi.setNameLabel(conn, "detached");

                umount(conn, vdi);

                return new AttachVolumeAnswer(cmd);
            }
        } catch (XenAPIException e) {
            String msg = errorMsg + " for uuid: " + cmd.getVolumePath() + "  due to " + e.toString();
            s_logger.warn(msg, e);
            return new AttachVolumeAnswer(cmd, msg);
        } catch (Exception e) {
            String msg = errorMsg + " for uuid: " + cmd.getVolumePath() + "  due to "  + e.getMessage();
            s_logger.warn(msg, e);
            return new AttachVolumeAnswer(cmd, msg);
        }

    }

    protected void umount(Connection conn, VDI vdi) {

    }

    protected Answer execute(final CreateVMSnapshotCommand cmd) {
        String vmName = cmd.getVmName();
        String vmSnapshotName = cmd.getTarget().getSnapshotName();
        List<VolumeTO> listVolumeTo = cmd.getVolumeTOs();
        VirtualMachine.State vmState = cmd.getVmState();
        String guestOSType = cmd.getGuestOSType();

        boolean snapshotMemory = cmd.getTarget().getType() == VMSnapshot.Type.DiskAndMemory;
        long timeout = 600;

        Connection conn = getConnection();
        VM vm = null;
        VM vmSnapshot = null;
        boolean success = false;

        try {
            // check if VM snapshot already exists
            Set<VM> vmSnapshots = VM.getByNameLabel(conn, cmd.getTarget().getSnapshotName());
            if(vmSnapshots.size() > 0)
                return new CreateVMSnapshotAnswer(cmd, cmd.getTarget(), cmd.getVolumeTOs());
            
            // check if there is already a task for this VM snapshot
            Task task = null;
            Set<Task> tasks = Task.getByNameLabel(conn, "Async.VM.snapshot");
            tasks.addAll(Task.getByNameLabel(conn, "Async.VM.checkpoint"));
            for (Task taskItem : tasks) {
                if(taskItem.getOtherConfig(conn).containsKey("CS_VM_SNAPSHOT_KEY")){
                    String vmSnapshotTaskName = taskItem.getOtherConfig(conn).get("CS_VM_SNAPSHOT_KEY");
                    if(vmSnapshotTaskName != null && vmSnapshotTaskName.equals(cmd.getTarget().getSnapshotName())){
                        task = taskItem;
                    }
                }
            }
            
            // create a new task if there is no existing task for this VM snapshot
            if(task == null){
                try {
                    vm = getVM(conn, vmName);
                } catch (Exception e) {
                    if (!snapshotMemory) {
                        vm = createWorkingVM(conn, vmName, guestOSType, listVolumeTo);
                    }
                }
    
                if (vm == null) {
                    return new CreateVMSnapshotAnswer(cmd, false,
                            "Creating VM Snapshot Failed due to can not find vm: "
                                    + vmName);
                }
                
                // call Xenserver API
                if (!snapshotMemory) {
                    task = vm.snapshotAsync(conn, vmSnapshotName);
                } else {
                    Set<VBD> vbds = vm.getVBDs(conn);
                    Pool pool = Pool.getByUuid(conn, _host.pool);
                    for (VBD vbd: vbds){
                        VBD.Record vbdr = vbd.getRecord(conn);
                        if (vbdr.userdevice.equals("0")){
                            VDI vdi = vbdr.VDI;
                            SR sr = vdi.getSR(conn);
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
            String result = task.getResult(conn);
            
            // extract VM snapshot ref from result 
            String ref = result.substring("<value>".length(), result.length() - "</value>".length());
            vmSnapshot = Types.toVM(ref);
            
            success = true;
            return new CreateVMSnapshotAnswer(cmd, cmd.getTarget(), cmd.getVolumeTOs());
        } catch (Exception e) {
            String msg = e.getMessage();
            s_logger.error("Creating VM Snapshot " + cmd.getTarget().getSnapshotName() + " failed due to: " + msg);
            return new CreateVMSnapshotAnswer(cmd, false, msg);
        } finally {
            try {
                if (!success) {
                    if (vmSnapshot != null) {
                        s_logger.debug("Delete exsisting VM Snapshot "
                                + vmSnapshotName
                                + " after making VolumeTO failed");
                        Set<VBD> vbds = vmSnapshot.getVBDs(conn);
                        for (VBD vbd : vbds) {
                            VBD.Record vbdr = vbd.getRecord(conn);
                            if (vbdr.type == VbdType.DISK) {
                                VDI vdi = vbdr.VDI;
                                vdi.destroy(conn);
                            }
                        }
                        vmSnapshot.destroy(conn);
                    }
                }
                if (vmState == VirtualMachine.State.Stopped) {
                    if (vm != null) {
                        vm.destroy(conn);
                    }
                }
            } catch (Exception e2) {
                s_logger.error("delete snapshot error due to "
                        + e2.getMessage());
            }
        }
    }
    
    private VM createWorkingVM(Connection conn, String vmName,
            String guestOSType, List<VolumeTO> listVolumeTo)
            throws BadServerResponse, VmBadPowerState, SrFull,
            OperationNotAllowed, XenAPIException, XmlRpcException {
        String guestOsTypeName = getGuestOsType(guestOSType, false);
        if (guestOsTypeName == null) {
            String msg = " Hypervisor " + this.getClass().getName()
                    + " doesn't support guest OS type " + guestOSType
                    + ". you can choose 'Other install media' to run it as HVM";
            s_logger.warn(msg);
            throw new CloudRuntimeException(msg);
        }
        VM template = getVM(conn, guestOsTypeName);
        VM vm = template.createClone(conn, vmName);
        vm.setIsATemplate(conn, false);
        Map<VDI, VolumeTO> vdiMap = new HashMap<VDI, VolumeTO>();
        for (VolumeTO volume : listVolumeTo) {
            String vdiUuid = volume.getPath();
            try {
                VDI vdi = VDI.getByUuid(conn, vdiUuid);
                vdiMap.put(vdi, volume);
            } catch (Types.UuidInvalid e) {
                s_logger.warn("Unable to find vdi by uuid: " + vdiUuid
                        + ", skip it");
            }
        }
        for (VDI vdi : vdiMap.keySet()) {
            VolumeTO volumeTO = vdiMap.get(vdi);
            VBD.Record vbdr = new VBD.Record();
            vbdr.VM = vm;
            vbdr.VDI = vdi;
            if (volumeTO.getType() == Volume.Type.ROOT) {
                vbdr.bootable = true;
                vbdr.unpluggable = false;
            } else {
                vbdr.bootable = false;
                vbdr.unpluggable = true;
            }
            vbdr.userdevice = new Long(volumeTO.getDeviceId()).toString();
            vbdr.mode = Types.VbdMode.RW;
            vbdr.type = Types.VbdType.DISK;
            VBD.create(conn, vbdr);
        }
        return vm;
    }

    protected Answer execute(final DeleteVMSnapshotCommand cmd) {
        String snapshotName = cmd.getTarget().getSnapshotName();
        Connection conn = getConnection();
                
        try {
            List<VDI> vdiList = new ArrayList<VDI>();
            Set<VM> snapshots = VM.getByNameLabel(conn, snapshotName);
            if(snapshots.size() == 0){
                s_logger.warn("VM snapshot with name " + snapshotName + " does not exist, assume it is already deleted");
                return new DeleteVMSnapshotAnswer(cmd, cmd.getVolumeTOs());
            }
            VM snapshot = snapshots.iterator().next();
            Set<VBD> vbds = snapshot.getVBDs(conn);
            for (VBD vbd : vbds) {
                if (vbd.getType(conn) == VbdType.DISK) {
                    VDI vdi = vbd.getVDI(conn);
                    vdiList.add(vdi);
                }
            }
            if(cmd.getTarget().getType() == VMSnapshot.Type.DiskAndMemory)
                vdiList.add(snapshot.getSuspendVDI(conn));
            snapshot.destroy(conn);
            for (VDI vdi : vdiList) {
                vdi.destroy(conn);
            }
            return new DeleteVMSnapshotAnswer(cmd, cmd.getVolumeTOs());
        } catch (Exception e) {
            s_logger.warn("Catch Exception: " + e.getClass().toString()
                    + " due to " + e.toString(), e);
            return new DeleteVMSnapshotAnswer(cmd, false, e.getMessage());
        }
    }
    
    protected Answer execute(final AttachIsoCommand cmd) {
        Connection conn = getConnection();
        boolean attach = cmd.isAttach();
        String vmName = cmd.getVmName();
        String isoURL = cmd.getIsoPath();

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
                VM vm = getVM(conn, vmName);

                // Find the ISO VDI
                VDI isoVDI = getIsoVDIByURL(conn, vmName, isoURL);

                // Find the VM's CD-ROM VBD
                Set<VBD> vbds = vm.getVBDs(conn);
                for (VBD vbd : vbds) {
                    String userDevice = vbd.getUserdevice(conn);
                    Types.VbdType type = vbd.getType(conn);

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
                VM vm = getVM(conn, vmName);
                String vmUUID = vm.getUuid(conn);

                // Find the ISO VDI
                VDI isoVDI = getIsoVDIByURL(conn, vmName, isoURL);

                SR sr = isoVDI.getSR(conn);

                // Look up all VBDs for this VDI
                Set<VBD> vbds = isoVDI.getVBDs(conn);

                // Iterate through VBDs, and if the VBD belongs the VM, eject
                // the ISO from it
                for (VBD vbd : vbds) {
                    VM vbdVM = vbd.getVM(conn);
                    String vbdVmUUID = vbdVM.getUuid(conn);

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
        } catch (XenAPIException e) {
            s_logger.warn(errorMsg + ": " + e.toString(), e);
            return new Answer(cmd, false, e.toString());
        } catch (Exception e) {
            s_logger.warn(errorMsg + ": " + e.toString(), e);
            return new Answer(cmd, false, e.getMessage());
        }
    }

    boolean IsISCSI(String type) {
        return SRType.LVMOHBA.equals(type) || SRType.LVMOISCSI.equals(type) || SRType.LVM.equals(type) ;
    }

    protected ManageSnapshotAnswer execute(final ManageSnapshotCommand cmd) {
        Connection conn = getConnection();
        long snapshotId = cmd.getSnapshotId();
        String snapshotName = cmd.getSnapshotName();

        // By default assume failure
        boolean success = false;
        String cmdSwitch = cmd.getCommandSwitch();
        String snapshotOp = "Unsupported snapshot command." + cmdSwitch;
        if (cmdSwitch.equals(ManageSnapshotCommand.CREATE_SNAPSHOT)) {
            snapshotOp = "create";
        } else if (cmdSwitch.equals(ManageSnapshotCommand.DESTROY_SNAPSHOT)) {
            snapshotOp = "destroy";
        }
        String details = "ManageSnapshotCommand operation: " + snapshotOp + " Failed for snapshotId: " + snapshotId;
        String snapshotUUID = null;

        try {
            if (cmdSwitch.equals(ManageSnapshotCommand.CREATE_SNAPSHOT)) {
                // Look up the volume
                String volumeUUID = cmd.getVolumePath();
                VDI volume = VDI.getByUuid(conn, volumeUUID);

                // Create a snapshot
                VDI snapshot = volume.snapshot(conn, new HashMap<String, String>());

                if (snapshotName != null) {
                    snapshot.setNameLabel(conn, snapshotName);
                }
                // Determine the UUID of the snapshot

                snapshotUUID = snapshot.getUuid(conn);
                String preSnapshotUUID = cmd.getSnapshotPath();
                //check if it is a empty snapshot
                if( preSnapshotUUID != null) {
                    SR sr = volume.getSR(conn);
                    String srUUID = sr.getUuid(conn);
                    String type = sr.getType(conn);
                    Boolean isISCSI = IsISCSI(type);
                    String snapshotParentUUID = getVhdParent(conn, srUUID, snapshotUUID, isISCSI);

                    String preSnapshotParentUUID = getVhdParent(conn, srUUID, preSnapshotUUID, isISCSI);
                    if( snapshotParentUUID != null && snapshotParentUUID.equals(preSnapshotParentUUID)) {
                        // this is empty snapshot, remove it
                        snapshot.destroy(conn);
                        snapshotUUID = preSnapshotUUID;
                    }

                }

                success = true;
                details = null;
            } else if (cmd.getCommandSwitch().equals(ManageSnapshotCommand.DESTROY_SNAPSHOT)) {
                // Look up the snapshot
                snapshotUUID = cmd.getSnapshotPath();
                VDI snapshot = getVDIbyUuid(conn, snapshotUUID);

                snapshot.destroy(conn);
                snapshotUUID = null;
                success = true;
                details = null;
            }
        } catch (XenAPIException e) {
            details += ", reason: " + e.toString();
            s_logger.warn(details, e);
        } catch (Exception e) {
            details += ", reason: " + e.toString();
            s_logger.warn(details, e);
        }

        return new ManageSnapshotAnswer(cmd, snapshotId, snapshotUUID, success, details);
    }

    protected CreatePrivateTemplateAnswer execute(final CreatePrivateTemplateFromVolumeCommand cmd) {
        Connection conn = getConnection();
        String secondaryStoragePoolURL = cmd.getSecondaryStorageUrl();
        String volumeUUID = cmd.getVolumePath();
        Long accountId = cmd.getAccountId();
        String userSpecifiedName = cmd.getTemplateName();
        Long templateId = cmd.getTemplateId();
        int wait = cmd.getWait();
        String details = null;
        SR tmpltSR = null;
        boolean result = false;
        String secondaryStorageMountPath = null;
        String installPath = null;
        try {
            URI uri = new URI(secondaryStoragePoolURL);
            secondaryStorageMountPath = uri.getHost() + ":" + uri.getPath();
            installPath = "template/tmpl/" + accountId + "/" + templateId;
            if( !createSecondaryStorageFolder(conn, secondaryStorageMountPath, installPath)) {
                details = " Filed to create folder " + installPath + " in secondary storage";
                s_logger.warn(details);
                return new CreatePrivateTemplateAnswer(cmd, false, details);
            }

            VDI volume = getVDIbyUuid(conn, volumeUUID);
            // create template SR
            URI tmpltURI = new URI(secondaryStoragePoolURL + "/" + installPath);
            tmpltSR = createNfsSRbyURI(conn, tmpltURI, false);

            // copy volume to template SR
            VDI tmpltVDI = cloudVDIcopy(conn, volume, tmpltSR, wait);
            // scan makes XenServer pick up VDI physicalSize
            tmpltSR.scan(conn);
            if (userSpecifiedName != null) {
                tmpltVDI.setNameLabel(conn, userSpecifiedName);
            }

            String tmpltUUID = tmpltVDI.getUuid(conn);
            String tmpltFilename = tmpltUUID + ".vhd";
            long virtualSize = tmpltVDI.getVirtualSize(conn);
            long physicalSize = tmpltVDI.getPhysicalUtilisation(conn);
            // create the template.properties file
            String templatePath = secondaryStorageMountPath + "/" + installPath;
            result = postCreatePrivateTemplate(conn, templatePath, tmpltFilename, tmpltUUID, userSpecifiedName, null, physicalSize, virtualSize, templateId);
            if (!result) {
                throw new CloudRuntimeException("Could not create the template.properties file on secondary storage dir: " + tmpltURI);
            }
            installPath = installPath + "/" + tmpltFilename;
            removeSR(conn, tmpltSR);
            tmpltSR = null;
            return new CreatePrivateTemplateAnswer(cmd, true, null, installPath, virtualSize, physicalSize, tmpltUUID, ImageFormat.VHD);
        } catch (Exception e) {
            if (tmpltSR != null) {
                removeSR(conn, tmpltSR);
            }
            if ( secondaryStorageMountPath != null) {
                deleteSecondaryStorageFolder(conn, secondaryStorageMountPath, installPath);
            }
            details = "Creating template from volume " + volumeUUID + " failed due to " + e.toString();
            s_logger.error(details, e);
        }
        return new CreatePrivateTemplateAnswer(cmd, result, details);
    }

    protected Answer execute(final UpgradeSnapshotCommand cmd) {

        String secondaryStorageUrl = cmd.getSecondaryStorageUrl();
        String backedUpSnapshotUuid = cmd.getSnapshotUuid();
        Long volumeId = cmd.getVolumeId();
        Long accountId = cmd.getAccountId();
        Long templateId = cmd.getTemplateId();
        Long tmpltAcountId = cmd.getTmpltAccountId();
        String version = cmd.getVersion();

        if ( !version.equals("2.1") ) {
            return new Answer(cmd, true, "success");
        }
        try {
            Connection conn = getConnection();
            URI uri = new URI(secondaryStorageUrl);
            String secondaryStorageMountPath = uri.getHost() + ":" + uri.getPath();
            String snapshotPath = secondaryStorageMountPath + "/snapshots/" + accountId + "/" + volumeId + "/" + backedUpSnapshotUuid + ".vhd";
            String templatePath = secondaryStorageMountPath + "/template/tmpl/" + tmpltAcountId + "/" + templateId;
            upgradeSnapshot(conn, templatePath, snapshotPath);
            return new Answer(cmd, true, "success");
        } catch (Exception e) {
            String details = "upgrading snapshot " + backedUpSnapshotUuid + " failed due to " + e.toString();
            s_logger.error(details, e);

        }
        return new Answer(cmd, false, "failure");
    }

    protected CreatePrivateTemplateAnswer execute(final CreatePrivateTemplateFromSnapshotCommand cmd) {
        Connection conn = getConnection();
        Long accountId = cmd.getAccountId();
        Long volumeId = cmd.getVolumeId();
        String secondaryStorageUrl = cmd.getSecondaryStorageUrl();
        String backedUpSnapshotUuid = cmd.getSnapshotUuid();
        Long newTemplateId = cmd.getNewTemplateId();
        String userSpecifiedName = cmd.getTemplateName();
        int wait = cmd.getWait();
        // By default, assume failure
        String details = null;
        boolean result = false;
        String secondaryStorageMountPath = null;
        String installPath = null;
        try {
            URI uri = new URI(secondaryStorageUrl);
            secondaryStorageMountPath = uri.getHost() + ":" + uri.getPath();
            installPath = "template/tmpl/" + accountId + "/" + newTemplateId;
            if( !createSecondaryStorageFolder(conn, secondaryStorageMountPath, installPath)) {
                details = " Filed to create folder " + installPath + " in secondary storage";
                s_logger.warn(details);
                return new CreatePrivateTemplateAnswer(cmd, false, details);
            }
            String templatePath = secondaryStorageMountPath + "/" + installPath;
            // create snapshot SR
            String filename = backedUpSnapshotUuid;
            if ( !filename.startsWith("VHD-") && !filename.endsWith(".vhd")) {
                filename = backedUpSnapshotUuid + ".vhd";
            }
            String snapshotPath = secondaryStorageMountPath + "/snapshots/" + accountId + "/" + volumeId + "/" + filename;
            String results = createTemplateFromSnapshot(conn, templatePath, snapshotPath, wait);
            String[] tmp = results.split("#");
            String tmpltUuid = tmp[1];
            long physicalSize = Long.parseLong(tmp[2]);
            long virtualSize = Long.parseLong(tmp[3]) * 1024 * 1024;
            String tmpltFilename = tmpltUuid + ".vhd";

            // create the template.properties file
            result = postCreatePrivateTemplate(conn, templatePath, tmpltFilename, tmpltUuid, userSpecifiedName, null, physicalSize, virtualSize, newTemplateId);
            if (!result) {
                throw new CloudRuntimeException("Could not create the template.properties file on secondary storage dir: " + templatePath);
            }
            installPath = installPath + "/" + tmpltFilename;
            return new CreatePrivateTemplateAnswer(cmd, true, null, installPath, virtualSize, physicalSize, tmpltUuid, ImageFormat.VHD);
        } catch (Exception e) {
            if (secondaryStorageMountPath != null) {
                deleteSecondaryStorageFolder(conn, secondaryStorageMountPath, installPath);
            }
            details = "Creating template from snapshot " + backedUpSnapshotUuid + " failed due to " + e.toString();
            s_logger.error(details, e);
        }
        return new CreatePrivateTemplateAnswer(cmd, result, details);
    }

    private boolean destroySnapshotOnPrimaryStorageExceptThis(Connection conn, String volumeUuid, String avoidSnapshotUuid){
        try {
            VDI volume = getVDIbyUuid(conn, volumeUuid);
            if (volume == null) {
                throw new InternalErrorException("Could not destroy snapshot on volume " + volumeUuid + " due to can not find it");
            }
            Set<VDI> snapshots = volume.getSnapshots(conn);
            for( VDI snapshot : snapshots ) {
                try {
                    if(! snapshot.getUuid(conn).equals(avoidSnapshotUuid)) {
                        snapshot.destroy(conn);
                    }
                } catch (Exception e) {
                    String msg = "Destroying snapshot: " + snapshot+ " on primary storage failed due to " + e.toString();
                    s_logger.warn(msg, e);
                }
            }
            s_logger.debug("Successfully destroyed snapshot on volume: " + volumeUuid + " execept this current snapshot "+ avoidSnapshotUuid );
            return true;
        } catch (XenAPIException e) {
            String msg = "Destroying snapshot on volume: " + volumeUuid + " execept this current snapshot "+ avoidSnapshotUuid + " failed due to " + e.toString();
            s_logger.error(msg, e);
        } catch (Exception e) {
            String msg = "Destroying snapshot on volume: " + volumeUuid + " execept this current snapshot "+ avoidSnapshotUuid + " failed due to " + e.toString();
            s_logger.warn(msg, e);
        }

        return false;
    }

    protected BackupSnapshotAnswer execute(final BackupSnapshotCommand cmd) {
        Connection conn = getConnection();
        String primaryStorageNameLabel = cmd.getPrimaryStoragePoolNameLabel();
        Long dcId = cmd.getDataCenterId();
        Long accountId = cmd.getAccountId();
        Long volumeId = cmd.getVolumeId();
        String secondaryStorageUrl = cmd.getSecondaryStorageUrl();
        String snapshotUuid = cmd.getSnapshotUuid(); // not null: Precondition.
        String prevBackupUuid = cmd.getPrevBackupUuid();
        String prevSnapshotUuid = cmd.getPrevSnapshotUuid();
        int wait = cmd.getWait();
        Long secHostId = cmd.getSecHostId();
        // By default assume failure
        String details = null;
        boolean success = false;
        String snapshotBackupUuid = null;
        boolean fullbackup = true;
        try {
            SR primaryStorageSR = getSRByNameLabelandHost(conn, primaryStorageNameLabel);
            if (primaryStorageSR == null) {
                throw new InternalErrorException("Could not backup snapshot because the primary Storage SR could not be created from the name label: " + primaryStorageNameLabel);
            }
            String psUuid = primaryStorageSR.getUuid(conn);
            Boolean isISCSI = IsISCSI(primaryStorageSR.getType(conn));
            URI uri = new URI(secondaryStorageUrl);
            String secondaryStorageMountPath = uri.getHost() + ":" + uri.getPath();
            VDI snapshotVdi = getVDIbyUuid(conn, snapshotUuid);
            String snapshotPaUuid = null;
            if ( prevBackupUuid != null ) {
                try {
                    snapshotPaUuid = getVhdParent(conn, psUuid, snapshotUuid, isISCSI);
                    if( snapshotPaUuid != null ) {
                        String snashotPaPaPaUuid = getVhdParent(conn, psUuid, snapshotPaUuid, isISCSI);
                        String prevSnashotPaUuid = getVhdParent(conn, psUuid, prevSnapshotUuid, isISCSI);
                        if (snashotPaPaPaUuid != null && prevSnashotPaUuid!= null && prevSnashotPaUuid.equals(snashotPaPaPaUuid)) {
                            fullbackup = false;
                        }
                    }
                } catch (Exception e) {
                }
            }

            if (fullbackup) {
                // the first snapshot is always a full snapshot
                String folder = "snapshots/" + accountId + "/" + volumeId;
                if( !createSecondaryStorageFolder(conn, secondaryStorageMountPath, folder)) {
                    details = " Filed to create folder " + folder + " in secondary storage";
                    s_logger.warn(details);
                    return new BackupSnapshotAnswer(cmd, false, details, null, false);
                }
                String snapshotMountpoint = secondaryStorageUrl + "/" + folder;
                SR snapshotSr = null;
                try {
                    snapshotSr = createNfsSRbyURI(conn, new URI(snapshotMountpoint), false);
                    VDI backedVdi = cloudVDIcopy(conn, snapshotVdi, snapshotSr, wait);
                    snapshotBackupUuid = backedVdi.getUuid(conn);
                    if( cmd.getSwift() != null ) {
                        try {
                            swiftBackupSnapshot(conn, cmd.getSwift(), snapshotSr.getUuid(conn), snapshotBackupUuid, "S-" + volumeId.toString(), false, wait);
                            snapshotBackupUuid = snapshotBackupUuid + ".vhd";
                        } finally {
                            deleteSnapshotBackup(conn, dcId, accountId, volumeId, secondaryStorageMountPath, snapshotBackupUuid);
                        }
                    } else if (cmd.getS3() != null) {
                        try {
                            backupSnapshotToS3(conn, cmd.getS3(), snapshotSr.getUuid(conn), snapshotBackupUuid, isISCSI, wait);
                            snapshotBackupUuid = snapshotBackupUuid + ".vhd";
                        } finally {
                            deleteSnapshotBackup(conn, dcId, accountId, volumeId, secondaryStorageMountPath, snapshotBackupUuid);
                        }
                    }                    
                    success = true;
                } finally {
                    if( snapshotSr != null) {
                        removeSR(conn, snapshotSr);
                    }
                }
            } else {
                String primaryStorageSRUuid = primaryStorageSR.getUuid(conn);
                if( cmd.getSwift() != null ) {
                    swiftBackupSnapshot(conn, cmd.getSwift(), primaryStorageSRUuid, snapshotPaUuid, "S-" + volumeId.toString(), isISCSI, wait);
                    if ( isISCSI ) {
                        snapshotBackupUuid = "VHD-" + snapshotPaUuid;
                    } else {
                        snapshotBackupUuid = snapshotPaUuid + ".vhd";
                    }
                    success = true;
                } else if (cmd.getS3() != null) {
                    backupSnapshotToS3(conn, cmd.getS3(), primaryStorageSRUuid, snapshotPaUuid, isISCSI, wait);
                } else {
                    snapshotBackupUuid = backupSnapshot(conn, primaryStorageSRUuid, dcId, accountId, volumeId, secondaryStorageMountPath, snapshotUuid, prevBackupUuid, isISCSI, wait, secHostId);
                    success = (snapshotBackupUuid != null);
                }
            }
            String volumeUuid = cmd.getVolumePath();
            destroySnapshotOnPrimaryStorageExceptThis(conn, volumeUuid, snapshotUuid);
            if (success) {
                details = "Successfully backedUp the snapshotUuid: " + snapshotUuid + " to secondary storage.";

            }
        } catch (XenAPIException e) {
            details = "BackupSnapshot Failed due to " + e.toString();
            s_logger.warn(details, e);
        } catch (Exception e) {
            details = "BackupSnapshot Failed due to " + e.getMessage();
            s_logger.warn(details, e);
        }

        return new BackupSnapshotAnswer(cmd, success, details, snapshotBackupUuid, fullbackup);
    }

    private static List<String> serializeProperties(final Object object,
            final Class<?> propertySet) {

        assert object != null;
        assert propertySet != null;
        assert propertySet.isAssignableFrom(object.getClass());

        try {

            final BeanInfo beanInfo = Introspector.getBeanInfo(propertySet);
            final PropertyDescriptor[] descriptors = beanInfo
                    .getPropertyDescriptors();

            final List<String> serializedProperties = new ArrayList<String>();
            for (final PropertyDescriptor descriptor : descriptors) {

                serializedProperties.add(descriptor.getName());
                final Object value = descriptor.getReadMethod().invoke(object);
                serializedProperties.add(value != null ? value.toString()
                        : "null");

            }

            return Collections.unmodifiableList(serializedProperties);

        } catch (IntrospectionException e) {
            s_logger.warn(
                    "Ignored IntrospectionException when serializing class "
                            + object.getClass().getCanonicalName(), e);
        } catch (IllegalArgumentException e) {
            s_logger.warn(
                    "Ignored IllegalArgumentException when serializing class "
                            + object.getClass().getCanonicalName(), e);
        } catch (IllegalAccessException e) {
            s_logger.warn(
                    "Ignored IllegalAccessException when serializing class "
                            + object.getClass().getCanonicalName(), e);
        } catch (InvocationTargetException e) {
            s_logger.warn(
                    "Ignored InvocationTargetException when serializing class "
                            + object.getClass().getCanonicalName(), e);
        }

        return Collections.emptyList();

    }

    private boolean backupSnapshotToS3(final Connection connection,
            final S3TO s3, final String srUuid, final String snapshotUuid,
            final Boolean iSCSIFlag, final int wait) {

        final String filename = iSCSIFlag ? "VHD-" + snapshotUuid
                : snapshotUuid + ".vhd";
        final String dir = (iSCSIFlag ? "/dev/VG_XenStorage-"
                : "/var/run/sr-mount/") + srUuid;
        final String key = StringUtils.join("/", "snapshots", snapshotUuid);

        try {

            final List<String> parameters = new ArrayList<String>(
                    serializeProperties(s3, S3Utils.ClientOptions.class));
            parameters.addAll(Arrays.asList("operation", "put", "directory",
                    dir, "filename", filename, "iSCSIFlag",
                    iSCSIFlag.toString(), "key", key));
            final String result = callHostPluginAsync(connection, "s3xen",
                    "s3", wait,
                    parameters.toArray(new String[parameters.size()]));

            if (result != null && result.equals("true")) {
                return true;
            }

        } catch (Exception e) {
            s_logger.error(String.format(
                    "S3 upload failed of snapshot %1$s due to %2$s.",
                    snapshotUuid, e.toString()), e);
        }

        return false;

    }

    protected CreateVolumeFromSnapshotAnswer execute(final CreateVolumeFromSnapshotCommand cmd) {
        Connection conn = getConnection();
        String primaryStorageNameLabel = cmd.getPrimaryStoragePoolNameLabel();
        Long accountId = cmd.getAccountId();
        Long volumeId = cmd.getVolumeId();
        String secondaryStorageUrl = cmd.getSecondaryStorageUrl();
        String backedUpSnapshotUuid = cmd.getSnapshotUuid();
        int wait = cmd.getWait();
        boolean result = false;
        // Generic error message.
        String details = null;
        String volumeUUID = null;
        SR snapshotSR = null;

        if (secondaryStorageUrl == null) {
            details += " because the URL passed: " + secondaryStorageUrl + " is invalid.";
            return new CreateVolumeFromSnapshotAnswer(cmd, result, details, volumeUUID);
        }
        try {
            SR primaryStorageSR = getSRByNameLabelandHost(conn, primaryStorageNameLabel);
            if (primaryStorageSR == null) {
                throw new InternalErrorException("Could not create volume from snapshot because the primary Storage SR could not be created from the name label: "
                        + primaryStorageNameLabel);
            }
            // Get the absolute path of the snapshot on the secondary storage.
            URI snapshotURI = new URI(secondaryStorageUrl + "/snapshots/" + accountId + "/" + volumeId);
            String filename = backedUpSnapshotUuid;
            if ( !filename.startsWith("VHD-") && !filename.endsWith(".vhd")) {
                filename = backedUpSnapshotUuid + ".vhd";
            }
            String snapshotPath = snapshotURI.getHost() + ":" + snapshotURI.getPath() + "/" + filename;
            String srUuid = primaryStorageSR.getUuid(conn);
            volumeUUID = copy_vhd_from_secondarystorage(conn, snapshotPath, srUuid, wait);
            result = true;
        } catch (XenAPIException e) {
            details += " due to " + e.toString();
            s_logger.warn(details, e);
        } catch (Exception e) {
            details += " due to " + e.getMessage();
            s_logger.warn(details, e);
        } finally {
            // In all cases, if the temporary SR was created, forget it.
            if (snapshotSR != null) {
                removeSR(conn, snapshotSR);
            }
        }
        if (!result) {
            // Is this logged at a higher level?
            s_logger.error(details);
        }

        // In all cases return something.
        return new CreateVolumeFromSnapshotAnswer(cmd, result, details, volumeUUID);
    }


    protected VM getVM(Connection conn, String vmName) {
        // Look up VMs with the specified name
        Set<VM> vms;
        try {
            vms = VM.getByNameLabel(conn, vmName);
        } catch (XenAPIException e) {
            throw new CloudRuntimeException("Unable to get " + vmName + ": " + e.toString(), e);
        } catch (Exception e) {
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

    protected VDI getIsoVDIByURL(Connection conn, String vmName, String isoURL) {
        SR isoSR = null;
        String mountpoint = null;
        if (isoURL.startsWith("xs-tools")) {
            try {
                Set<VDI> vdis = VDI.getByNameLabel(conn, isoURL);
                if (vdis.isEmpty()) {
                    throw new CloudRuntimeException("Could not find ISO with URL: " + isoURL);
                }
                return vdis.iterator().next();

            } catch (XenAPIException e) {
                throw new CloudRuntimeException("Unable to get pv iso: " + isoURL + " due to " + e.toString());
            } catch (Exception e) {
                throw new CloudRuntimeException("Unable to get pv iso: " + isoURL + " due to " + e.toString());
            }
        }

        int index = isoURL.lastIndexOf("/");
        mountpoint = isoURL.substring(0, index);

        URI uri;
        try {
            uri = new URI(mountpoint);
        } catch (URISyntaxException e) {
            throw new CloudRuntimeException("isoURL is wrong: " + isoURL);
        }
        isoSR = getISOSRbyVmName(conn, vmName);
        if (isoSR == null) {
            isoSR = createIsoSRbyURI(conn, uri, vmName, false);
        }

        String isoName = isoURL.substring(index + 1);

        VDI isoVDI = getVDIbyLocationandSR(conn, isoName, isoSR);

        if (isoVDI != null) {
            return isoVDI;
        } else {
            throw new CloudRuntimeException("Could not find ISO with URL: " + isoURL);
        }
    }

    protected SR getStorageRepository(Connection conn, String uuid) {
        Set<SR> srs;
        try {
            srs = SR.getByNameLabel(conn, uuid);
        } catch (XenAPIException e) {
            throw new CloudRuntimeException("Unable to get SR " + uuid + " due to " + e.toString(), e);
        } catch (Exception e) {
            throw new CloudRuntimeException("Unable to get SR " + uuid + " due to " + e.getMessage(), e);
        }

        if (srs.size() > 1) {
            throw new CloudRuntimeException("More than one storage repository was found for pool with uuid: " + uuid);
        } else if (srs.size() == 1) {
            SR sr = srs.iterator().next();
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("SR retrieved for " + uuid);
            }

            if (checkSR(conn, sr)) {
                return sr;
            }
            throw new CloudRuntimeException("SR check failed for storage pool: " + uuid + "on host:" + _host.uuid);
        } else {
            throw new CloudRuntimeException("Can not see storage pool: " + uuid + " from on host:" + _host.uuid);
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


    protected boolean createSecondaryStorageFolder(Connection conn, String remoteMountPath, String newFolder) {
        String result = callHostPlugin(conn, "vmopsSnapshot", "create_secondary_storage_folder", "remoteMountPath", remoteMountPath, "newFolder", newFolder);
        return (result != null);
    }

    protected boolean deleteSecondaryStorageFolder(Connection conn, String remoteMountPath, String folder) {
        String details = callHostPlugin(conn, "vmopsSnapshot", "delete_secondary_storage_folder", "remoteMountPath", remoteMountPath, "folder", folder);
        return (details != null && details.equals("1"));
    }

    protected boolean postCreatePrivateTemplate(Connection conn, String templatePath, String tmpltFilename, String templateName, String templateDescription, String checksum, long size, long virtualSize, long templateId) {

        if (templateDescription == null) {
            templateDescription = "";
        }

        if (checksum == null) {
            checksum = "";
        }

        String result = callHostPlugin(conn, "vmopsSnapshot", "post_create_private_template", "templatePath", templatePath, "templateFilename", tmpltFilename, "templateName", templateName, "templateDescription", templateDescription,
                "checksum", checksum, "size", String.valueOf(size), "virtualSize", String.valueOf(virtualSize), "templateId", String.valueOf(templateId));

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

    protected String getVhdParent(Connection conn, String primaryStorageSRUuid, String snapshotUuid, Boolean isISCSI) {
        String parentUuid = callHostPlugin(conn, "vmopsSnapshot", "getVhdParent", "primaryStorageSRUuid", primaryStorageSRUuid,
                "snapshotUuid", snapshotUuid, "isISCSI", isISCSI.toString());

        if (parentUuid == null || parentUuid.isEmpty() || parentUuid.equalsIgnoreCase("None")) {
            s_logger.debug("Unable to get parent of VHD " + snapshotUuid + " in SR " + primaryStorageSRUuid);
            // errString is already logged.
            return null;
        }
        return parentUuid;
    }

    protected boolean destroySnapshotOnPrimaryStorage(Connection conn, String snapshotUuid) {
        // Precondition snapshotUuid != null
        try {
            VDI snapshot = getVDIbyUuid(conn, snapshotUuid);
            if (snapshot == null) {
                throw new InternalErrorException("Could not destroy snapshot " + snapshotUuid + " because the snapshot VDI was null");
            }
            snapshot.destroy(conn);
            s_logger.debug("Successfully destroyed snapshotUuid: " + snapshotUuid + " on primary storage");
            return true;
        } catch (XenAPIException e) {
            String msg = "Destroy snapshotUuid: " + snapshotUuid + " on primary storage failed due to " + e.toString();
            s_logger.error(msg, e);
        } catch (Exception e) {
            String msg = "Destroy snapshotUuid: " + snapshotUuid + " on primary storage failed due to " + e.getMessage();
            s_logger.warn(msg, e);
        }

        return false;
    }

    protected String deleteSnapshotBackup(Connection conn, Long dcId, Long accountId, Long volumeId, String secondaryStorageMountPath, String backupUUID) {

        // If anybody modifies the formatting below again, I'll skin them
        String result = callHostPlugin(conn, "vmopsSnapshot", "deleteSnapshotBackup", "backupUUID", backupUUID, "dcId", dcId.toString(), "accountId", accountId.toString(),
                "volumeId", volumeId.toString(), "secondaryStorageMountPath", secondaryStorageMountPath);

        return result;
    }

    protected boolean deleteSnapshotsDir(Connection conn, Long dcId, Long accountId, Long volumeId, String secondaryStorageMountPath) {
        return deleteSecondaryStorageFolder(conn, secondaryStorageMountPath, "snapshots" + "/" + accountId.toString() + "/" + volumeId.toString());             
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
    public void setAgentControl(IAgentControl agentControl) {
        _agentControl = agentControl;
    }

    protected Answer execute(PoolEjectCommand cmd) {
        Connection conn = getConnection();
        String hostuuid = cmd.getHostuuid();
        try {
            Map<Host, Host.Record> hostrs = Host.getAllRecords(conn);
            boolean found = false;
            for( Host.Record hr : hostrs.values() ) {
                if( hr.uuid.equals(hostuuid)) {
                    found = true;
                }
            }
            if( ! found) {
                s_logger.debug("host " + hostuuid + " has already been ejected from pool " + _host.pool);
                return new Answer(cmd);
            }

            Pool pool = Pool.getAll(conn).iterator().next();
            Pool.Record poolr = pool.getRecord(conn);

            Host.Record masterRec = poolr.master.getRecord(conn);
            if (hostuuid.equals(masterRec.uuid)) {
                s_logger.debug("This is last host to eject, so don't need to eject: " + hostuuid);
                return new Answer(cmd);
            }

            Host host = Host.getByUuid(conn, hostuuid);
            // remove all tags cloud stack add before eject
            Host.Record hr = host.getRecord(conn);
            Iterator<String> it = hr.tags.iterator();
            while (it.hasNext()) {
                String tag = it.next();
                if (tag.startsWith("vmops-version-")) {
                    it.remove();
                }
            }
            // eject from pool
            try {
                Pool.eject(conn, host);
                try {
                    Thread.sleep(10 * 1000);
                } catch (InterruptedException e) {
                }
            } catch (XenAPIException e) {
                String msg = "Unable to eject host " + _host.uuid + " due to " + e.toString();
                s_logger.warn(msg);
                host.destroy(conn);
            }
            return new Answer(cmd);
        } catch (XenAPIException e) {
            String msg = "XenAPIException Unable to destroy host " + _host.uuid + " in xenserver database due to " + e.toString();
            s_logger.warn(msg, e);
            return new Answer(cmd, false, msg);
        } catch (Exception e) {
            String msg = "Exception Unable to destroy host " + _host.uuid + " in xenserver database due to " + e.getMessage();
            s_logger.warn(msg, e);
            return new Answer(cmd, false, msg);
        }
    }

    private Answer execute(CleanupNetworkRulesCmd cmd) {
        if (!_canBridgeFirewall) {
            return new Answer(cmd, true, null);
        }
        Connection conn = getConnection();
        String result = callHostPlugin(conn, "vmops","cleanup_rules", "instance", _instance);
        int numCleaned = Integer.parseInt(result);
        if (result == null || result.isEmpty() || (numCleaned < 0)) {
            s_logger.warn("Failed to cleanup rules for host " + _host.ip);
            return new Answer(cmd, false, result);
        }
        if (numCleaned > 0) {
            s_logger.info("Cleaned up rules for " + result + " vms on host " + _host.ip);
        }
        return new Answer(cmd, true, result);
    }



    /**
     * XsNic represents a network and the host's specific PIF.
     */
    protected class XsLocalNetwork {
        private final Network _n;
        private Network.Record _nr;
        private PIF _p;
        private PIF.Record _pr;

        public XsLocalNetwork(Network n) {
            this(n, null, null, null);
        }

        public XsLocalNetwork(Network n, Network.Record nr, PIF p, PIF.Record pr) {
            _n = n;
            _nr = nr;
            _p = p;
            _pr = pr;
        }

        public Network getNetwork() {
            return _n;
        }

        public Network.Record getNetworkRecord(Connection conn) throws XenAPIException, XmlRpcException {
            if (_nr == null) {
                _nr = _n.getRecord(conn);
            }

            return _nr;
        }

        public PIF getPif(Connection conn) throws XenAPIException, XmlRpcException {
            if (_p == null) {
                Network.Record nr = getNetworkRecord(conn);
                for (PIF pif : nr.PIFs) {
                    PIF.Record pr = pif.getRecord(conn);
                    if (_host.uuid.equals(pr.host.getUuid(conn))) {
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Found a network called " + nr.nameLabel + " on host=" + _host.ip + ";  Network=" + nr.uuid + "; pif=" + pr.uuid);
                        }
                        _p = pif;
                        _pr = pr;
                        break;
                    }
                }
            }
            return _p;
        }

        public PIF.Record getPifRecord(Connection conn) throws XenAPIException, XmlRpcException {
            if (_pr == null) {
                PIF p = getPif(conn);
                if (_pr == null) {
                    _pr = p.getRecord(conn);
                }
            }
            return _pr;
        }
    }

    // A list of UUIDs that are gathered from the XenServer when
    // the resource first connects to XenServer. These UUIDs do
    // not change over time.
    protected class XsHost {
        public String systemvmisouuid;
        public String uuid;
        public String ip;
        public String publicNetwork;
        public String privateNetwork;
        public String linkLocalNetwork;
        public Network vswitchNetwork;
        public String storageNetwork1;
        public String storageNetwork2;
        public String guestNetwork;
        public String guestPif;
        public String publicPif;
        public String privatePif;
        public String storagePif1;
        public String storagePif2;
        public String pool;
        public int speed;
        public int cpus;
        public String product_version;
        public String localSRuuid;


        @Override
        public String toString() {
            return new StringBuilder("XS[").append(uuid).append("-").append(ip).append("]").toString();
        }
    }

    /*Override by subclass*/
    protected String getGuestOsType(String stdType, boolean bootFromCD) {
        return stdType;
    }

    private Answer execute(NetworkRulesSystemVmCommand cmd) {
        boolean success = true;
        Connection conn = getConnection();
        if (cmd.getType() != VirtualMachine.Type.User) {
            String result = callHostPlugin(conn, "vmops", "default_network_rules_systemvm", "vmName", cmd.getVmName());
            if (result == null || result.isEmpty() || !Boolean.parseBoolean(result)) {
                success = false;
            }
        }

        return new Answer(cmd, success, "");
    }

    private Answer execute(NetworkRulesVmSecondaryIpCommand cmd) {
        boolean success = true;
        Connection conn = getConnection();

        String result = callHostPlugin(conn, "vmops", "network_rules_vmSecondaryIp", "vmName", cmd.getVmName(), "vmMac", cmd.getVmMac(), "vmSecIp", cmd.getVmSecIp(), "action",
                cmd.getAction());
        if (result == null || result.isEmpty() || !Boolean.parseBoolean(result)) {
            success = false;
        }

        return new Answer(cmd, success, "");
    }

    protected SetFirewallRulesAnswer execute(SetFirewallRulesCommand cmd) {
        String[] results = new String[cmd.getRules().length];
        String callResult;
        Connection conn = getConnection();
        String routerIp = cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);
        FirewallRuleTO[] allrules = cmd.getRules();
        FirewallRule.TrafficType trafficType = allrules[0].getTrafficType();
        if (routerIp == null) {
            return new SetFirewallRulesAnswer(cmd, false, results);
        }

        String[][] rules = cmd.generateFwRules();
        String args = "";
        args += routerIp + " -F";
        if (trafficType == FirewallRule.TrafficType.Egress){
            args+= " -E";
        }
        StringBuilder sb = new StringBuilder();
        String[] fwRules = rules[0];
        if (fwRules.length > 0) {
            for (int i = 0; i < fwRules.length; i++) {
                sb.append(fwRules[i]).append(',');
            }
            args += " -a " + sb.toString();
        }

        callResult = callHostPlugin(conn, "vmops", "setFirewallRule", "args", args);

        if (callResult == null || callResult.isEmpty()) {
            //FIXME - in the future we have to process each rule separately; now we temporarily set every rule to be false if single rule fails
            for (int i=0; i < results.length; i++) {
                results[i] = "Failed";
            }
            return new SetFirewallRulesAnswer(cmd, false, results);
        }
        return new SetFirewallRulesAnswer(cmd, true, results);
    }

    protected Answer execute(final ClusterSyncCommand cmd) {
        Connection conn = getConnection();
        //check if this is master
        Pool pool;
        try {
            pool = Pool.getByUuid(conn, _host.pool);
            Pool.Record poolr = pool.getRecord(conn);

            Host.Record hostr = poolr.master.getRecord(conn);
            if (!_host.uuid.equals(hostr.uuid)) {
                return new Answer(cmd);
            }
        } catch (Throwable e) {
            s_logger.warn("Check for master failed, failing the Cluster sync command");
            return  new Answer(cmd);
        } 
        HashMap<String, Pair<String, State>> newStates = deltaClusterSync(conn);
        return new ClusterSyncAnswer(cmd.getClusterId(), newStates);
    }


    protected HashMap<String, Pair<String, State>> fullClusterSync(Connection conn) {
        synchronized (_cluster.intern()) {
            s_vms.clear(_cluster);
        }
        try {
            Map<VM, VM.Record>  vm_map = VM.getAllRecords(conn);  //USE THIS TO GET ALL VMS FROM  A CLUSTER
            for (VM.Record record: vm_map.values()) {
                if (record.isControlDomain || record.isASnapshot || record.isATemplate) {
                    continue; // Skip DOM0
                }
                String vm_name = record.nameLabel;
                VmPowerState ps = record.powerState;
                final State state = convertToState(ps);
                Host host = record.residentOn;
                String host_uuid = null;
                if( ! isRefNull(host) ) {
                    host_uuid = host.getUuid(conn);
                    synchronized (_cluster.intern()) {
                        s_vms.put(_cluster, host_uuid, vm_name, state);
                    }
                }
                if (s_logger.isTraceEnabled()) {
                    s_logger.trace("VM " + vm_name + ": powerstate = " + ps + "; vm state=" + state.toString());
                } 
            }
        } catch (final Throwable e) {
            String msg = "Unable to get vms through host " + _host.uuid + " due to to " + e.toString();      
            s_logger.warn(msg, e);
            throw new CloudRuntimeException(msg);
        }
        return s_vms.getClusterVmState(_cluster);
    }


    protected HashMap<String, Pair<String, State>> deltaClusterSync(Connection conn) {
        final HashMap<String, Pair<String, State>> changes = new HashMap<String, Pair<String, State>>();


        synchronized (_cluster.intern()) {
            HashMap<String, Pair<String, State>> newStates = getAllVms(conn);
            if (newStates == null) {
                s_logger.warn("Unable to get the vm states so no state sync at this point.");
                return null;
            }
            HashMap<String, Pair<String, State>>  oldStates = new HashMap<String, Pair<String, State>>(s_vms.size(_cluster));
            oldStates.putAll(s_vms.getClusterVmState(_cluster));

            for (final Map.Entry<String, Pair<String, State>> entry : newStates.entrySet()) {
                final String vm = entry.getKey();

                State newState = entry.getValue().second();
                String host_uuid = entry.getValue().first();
                final Pair<String, State> oldState = oldStates.remove(vm);

                //check if host is changed
                if (host_uuid != null && oldState != null){
                    if (!host_uuid.equals(oldState.first()) && newState != State.Stopped && newState != State.Stopping){
                        s_logger.warn("Detecting a change in host for " + vm);
                        changes.put(vm, new Pair<String, State>(host_uuid, newState));

                        s_logger.debug("11. The VM " + vm + " is in " + newState + " state");
                        s_vms.put(_cluster, host_uuid, vm, newState);
                        continue;
                    }
                }

                if (newState == State.Stopped  && oldState != null && oldState.second() != State.Stopping && oldState.second() != State.Stopped) {
                    newState = getRealPowerState(conn, vm);
                }

                if (s_logger.isTraceEnabled()) {
                    s_logger.trace("VM " + vm + ": xen has state " + newState + " and we have state " + (oldState != null ? oldState.toString() : "null"));
                }

                if (vm.startsWith("migrating")) {
                    s_logger.warn("Migrating from xen detected.  Skipping");
                    continue;
                }
                if (oldState == null) {
                    s_vms.put(_cluster, host_uuid, vm, newState);
                    s_logger.warn("Detecting a new state but couldn't find a old state so adding it to the changes: " + vm);
                    changes.put(vm, new Pair<String, State>(host_uuid, newState));
                } else if (oldState.second() == State.Starting) {
                    if (newState == State.Running) { 
                        s_logger.debug("12. The VM " + vm + " is in " + State.Running + " state");
                        s_vms.put(_cluster, host_uuid, vm, newState);
                    } else if (newState == State.Stopped) {
                        s_logger.warn("Ignoring vm " + vm + " because of a lag in starting the vm.");
                    }
                } else if (oldState.second() == State.Migrating) {
                    if (newState == State.Running) {
                        s_logger.debug("Detected that an migrating VM is now running: " + vm);
                        s_vms.put(_cluster, host_uuid, vm, newState);
                    }
                } else if (oldState.second() == State.Stopping) {
                    if (newState == State.Stopped) {
                        s_logger.debug("13. The VM " + vm + " is in " + State.Stopped + " state");
                        s_vms.put(_cluster, host_uuid, vm, newState);
                    } else if (newState == State.Running) {
                        s_logger.warn("Ignoring vm " + vm + " because of a lag in stopping the vm. ");
                    }
                } else if (oldState.second() != newState) {
                    s_logger.debug("14. The VM " + vm + " is in " + newState + " state was " + oldState.second());
                    s_vms.put(_cluster, host_uuid, vm, newState);
                    if (newState == State.Stopped) {
                        /*
                         * if (s_vmsKilled.remove(vm)) { s_logger.debug("VM " + vm + " has been killed for storage. ");
                         * newState = State.Error; }
                         */
                    }
                    changes.put(vm, new Pair<String, State>(host_uuid, newState));
                }
            }

            for (final Map.Entry<String, Pair<String, State>> entry : oldStates.entrySet()) {
                final String vm = entry.getKey();
                final State oldState = entry.getValue().second();
                String host_uuid = entry.getValue().first();

                if (s_logger.isTraceEnabled()) {
                    s_logger.trace("VM " + vm + " is now missing from xen so reporting stopped");
                }

                if (oldState == State.Stopping) {
                    s_logger.warn("Ignoring VM " + vm + " in transition state stopping.");
                    s_vms.remove(_cluster, host_uuid, vm);
                } else if (oldState == State.Starting) {
                    s_logger.warn("Ignoring VM " + vm + " in transition state starting.");
                } else if (oldState == State.Stopped) {
                    s_logger.debug("VM missing " + vm + " old state stopped so removing.");
                    s_vms.remove(_cluster, host_uuid, vm);
                } else if (oldState == State.Migrating) {
                    s_logger.warn("Ignoring VM " + vm + " in migrating state.");
                } else {
                    State newState = State.Stopped;
                    s_logger.warn("The VM is now missing marking it as Stopped " + vm);
                    changes.put(vm, new Pair<String, State>(host_uuid, newState));
                }
            }
        }
        return changes;
    }

    /**
     * @param cmd
     * @return
     */
    private UnPlugNicAnswer execute(UnPlugNicCommand cmd) {
        Connection conn = getConnection();
        String vmName = cmd.getInstanceName();
        try {
            Set<VM> vms = VM.getByNameLabel(conn, vmName);
            if ( vms == null || vms.isEmpty() ) {
                return new UnPlugNicAnswer(cmd, false, "Can not find VM " + vmName);
            }
            VM vm = vms.iterator().next();
            NicTO nic = cmd.getNic();
            String mac = nic.getMac();
            VIF vif = getVifByMac(conn, vm, mac);
            if ( vif != null ) {
                vif.unplug(conn);
                Network network = vif.getNetwork(conn);
                vif.destroy(conn);
                try {
                    if (network.getNameLabel(conn).startsWith("VLAN")) {
                        disableVlanNetwork(conn, network);
                    }
                }  catch (Exception e) {
                }
            }
            return new UnPlugNicAnswer(cmd, true, "success");
        } catch (Exception e) {
            String msg = " UnPlug Nic failed due to " + e.toString();
            s_logger.warn(msg, e);
            return new UnPlugNicAnswer(cmd, false, msg);
        }
    }

    /**
     * @param cmd
     * @return
     */
    private PlugNicAnswer execute(PlugNicCommand cmd) {
        Connection conn = getConnection();
        String vmName = cmd.getVmName();
        try {
            Set<VM> vms = VM.getByNameLabel(conn, vmName);
            if ( vms == null || vms.isEmpty() ) {
                return new PlugNicAnswer(cmd, false, "Can not find VM " + vmName);
            }
            VM vm = vms.iterator().next();
            NicTO nic = cmd.getNic();
            VIF vif = getVifByMac(conn, vm, nic.getMac());
            if ( vif != null ) {
                String msg = " Plug Nic failed due to a VIF with the same mac " + nic.getMac() + " exists";
                s_logger.warn(msg);
                return new PlugNicAnswer(cmd, false, msg);
            }
            String deviceId = getLowestAvailableVIFDeviceNum(conn, vm);
            nic.setDeviceId(Integer.parseInt(deviceId));
            vif = createVif(conn, vmName, vm, nic);
            vif.plug(conn);
            return new PlugNicAnswer(cmd, true, "success");
        } catch (Exception e) {
            String msg = " Plug Nic failed due to " + e.toString();
            s_logger.warn(msg, e);
            return new PlugNicAnswer(cmd, false, msg);
        }
    }

    /**
     * @param cmd
     * @return
     */
    private SetupGuestNetworkAnswer execute(SetupGuestNetworkCommand cmd) {
        Connection conn = getConnection();
        NicTO nic = cmd.getNic();
        String domrIP = cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);
        String domrGIP = cmd.getAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP);
        String domrName = cmd.getAccessDetail(NetworkElementCommand.ROUTER_NAME);
        String gw = cmd.getAccessDetail(NetworkElementCommand.GUEST_NETWORK_GATEWAY);
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
            Set<VM> vms = VM.getByNameLabel(conn, domrName);
            if ( vms == null || vms.isEmpty() ) {
                return new SetupGuestNetworkAnswer(cmd, false, "Can not find VM " + domrName);
            }
            VM vm = vms.iterator().next();
            String mac = nic.getMac();
            VIF domrVif = null;
            for ( VIF vif : vm.getVIFs(conn)) {
                String lmac = vif.getMAC(conn);
                if ( lmac.equals(mac) ) {
                    domrVif = vif;
                    break;
                }
            }
            if ( domrVif == null ) {
                return new SetupGuestNetworkAnswer(cmd, false, "Can not find vif with mac " + mac + " for VM " + domrName);
            }

            String args = "vpc_guestnw.sh " + domrIP + (cmd.isAdd()?" -C":" -D");
            String dev = "eth" + domrVif.getDevice(conn);
            args += " -d " + dev;
            args += " -i " + domrGIP;
            args += " -g " + gw;
            args += " -m " + cidr;
            args += " -n " + NetUtils.getSubNet(domrGIP, nic.getNetmask());
            if ( dns != null && !dns.isEmpty() ) {
                args += " -s " + dns;
            }
            if ( domainName != null && !domainName.isEmpty() ) {
                args += " -e " + domainName;
            }
            String result = callHostPlugin(conn, "vmops", "routerProxy", "args", args);
            if (result == null || result.isEmpty()) {
                return new SetupGuestNetworkAnswer(cmd, false, "creating guest network failed due to " + ((result == null)? "null":result));
            }
            return new SetupGuestNetworkAnswer(cmd, true, "success");
        } catch (Exception e) {
            String msg = "Creating guest network failed due to " + e.toString();
            s_logger.warn(msg, e);
            return new SetupGuestNetworkAnswer(cmd, false, msg);
        }
    }

    protected IpAssocAnswer execute(IpAssocVpcCommand cmd) {
        Connection conn = getConnection();
        String[] results = new String[cmd.getIpAddresses().length];
        int i = 0;
        String routerName = cmd.getAccessDetail(NetworkElementCommand.ROUTER_NAME);
        String routerIp = cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);
        try {
            IpAddressTO[] ips = cmd.getIpAddresses();
            for (IpAddressTO ip : ips) {

                assignVPCPublicIpAddress(conn, routerName, routerIp, ip);
                results[i++] = ip.getPublicIp() + " - success";
            }
        } catch (Exception e) {
            s_logger.error("Ip Assoc failure on applying one ip due to exception:  ", e);
            results[i++] = IpAssocAnswer.errorResult;
        }

        return new IpAssocAnswer(cmd, results);
    }

    protected Answer execute(Site2SiteVpnCfgCommand cmd) {
        Connection conn = getConnection();
        String args = "ipsectunnel.sh " + cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);
        if (cmd.isCreate()) {
            args += " -A";
            args += " -l ";
            args += cmd.getLocalPublicIp();
            args += " -n ";
            args += cmd.getLocalGuestCidr();
            args += " -g ";
            args += cmd.getLocalPublicGateway();
            args += " -r ";
            args += cmd.getPeerGatewayIp();
            args += " -N ";
            args += cmd.getPeerGuestCidrList();
            args += " -e ";
            args += "\"" + cmd.getEspPolicy() + "\"";
            args += " -i ";
            args += "\"" + cmd.getIkePolicy() + "\"";
            args += " -t ";
            args += Long.toString(cmd.getIkeLifetime());
            args += " -T ";
            args += Long.toString(cmd.getEspLifetime());
            args += " -s ";
            args += "\"" + cmd.getIpsecPsk() + "\"";
            args += " -d ";
            if (cmd.getDpd()) {
                args += "1";
            } else {
                args += "0";
            }
        } else {
            args += " -D";
            args += " -r ";
            args += cmd.getPeerGatewayIp();
            args += " -n ";
            args += cmd.getLocalGuestCidr();
            args += " -N ";
            args += cmd.getPeerGuestCidrList();
        }
        String result = callHostPlugin(conn, "vmops", "routerProxy", "args", args);
        if (result == null || result.isEmpty()) {
            return new Answer(cmd, false, "Configure site to site VPN failed! ");
        }
        return new Answer(cmd);
    }

    protected SetSourceNatAnswer execute(SetSourceNatCommand cmd) {
        Connection conn = getConnection();
        String routerName = cmd.getAccessDetail(NetworkElementCommand.ROUTER_NAME);
        String routerIp = cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);
        IpAddressTO pubIp = cmd.getIpAddress();
        try {
            VM router = getVM(conn, routerName);

            VIF correctVif = getCorrectVif(conn, router, pubIp);

            String args = "vpc_snat.sh " + routerIp;

            args += " -A ";
            args += " -l ";
            args += pubIp.getPublicIp();

            args += " -c ";
            args += "eth" + correctVif.getDevice(conn);

            String result = callHostPlugin(conn, "vmops", "routerProxy", "args", args);
            if (result == null || result.isEmpty()) {
                throw new InternalErrorException("Xen plugin \"vpc_snat\" failed.");
            }
            return new SetSourceNatAnswer(cmd, true, "success");
        } catch (Exception e) {
            String msg = "Ip SNAT failure due to " + e.toString();
            s_logger.error(msg, e);
            return new SetSourceNatAnswer(cmd, false, msg);
        }
    }

    private SetNetworkACLAnswer execute(SetNetworkACLCommand cmd) {
        String[] results = new String[cmd.getRules().length];
        String callResult;
        Connection conn = getConnection();
        String routerName = cmd.getAccessDetail(NetworkElementCommand.ROUTER_NAME);
        String routerIp = cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);
        try {
            VM router = getVM(conn, routerName);
            String [][] rules = cmd.generateFwRules();
            StringBuilder sb = new StringBuilder();
            String[] aclRules = rules[0];

            for (int i = 0; i < aclRules.length; i++) {
                sb.append(aclRules[i]).append(',');
            }

            NicTO nic = cmd.getNic();
            VIF vif = getVifByMac(conn, router, nic.getMac());
            String args = "vpc_acl.sh " + routerIp;
            args += " -d " + "eth" + vif.getDevice(conn);
            args += " -i " + nic.getIp();
            args += " -m " + Long.toString(NetUtils.getCidrSize(nic.getNetmask()));
            args += " -a " + sb.toString();
            callResult = callHostPlugin(conn, "vmops", "routerProxy", "args", args);
            if (callResult == null || callResult.isEmpty()) {
                //FIXME - in the future we have to process each rule separately; now we temporarily set every rule to be false if single rule fails
                for (int i=0; i < results.length; i++) {
                    results[i] = "Failed";
                }
                return new SetNetworkACLAnswer(cmd, false, results);
            }
            return new SetNetworkACLAnswer(cmd, true, results);
        } catch (Exception e) {
            String msg = "SetNetworkACL failed due to " + e.toString();
            s_logger.error(msg, e);
            return new SetNetworkACLAnswer(cmd, false, results);
        }
    }

    protected SetPortForwardingRulesAnswer execute(SetPortForwardingRulesVpcCommand cmd) {
        Connection conn = getConnection();

        String routerIp = cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);
        String[] results = new String[cmd.getRules().length];
        int i = 0;

        boolean endResult = true;
        for (PortForwardingRuleTO rule : cmd.getRules()) {
            String args ="vpc_portforwarding.sh " + routerIp;
            args += rule.revoked() ? " -D" : " -A";
            args += " -P " + rule.getProtocol().toLowerCase();
            args += " -l " + rule.getSrcIp();
            args += " -p " + rule.getStringSrcPortRange();
            args += " -r " + rule.getDstIp();
            args += " -d " + rule.getStringDstPortRange().replace(":", "-");

            String result = callHostPlugin(conn, "vmops", "routerProxy", "args", args.toString());

            if (result == null || result.isEmpty()) {
                results[i++] = "Failed";
                endResult = false;
            } else {
                results[i++] = null;
            }
        }
        return new SetPortForwardingRulesAnswer(cmd, results, endResult);
    }


    private SetStaticRouteAnswer execute(SetStaticRouteCommand cmd) {
        String callResult;
        Connection conn = getConnection();
        String routerIp = cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);
        try {
            String[] results = new String[cmd.getStaticRoutes().length];
            String [][] rules = cmd.generateSRouteRules();
            StringBuilder sb = new StringBuilder();
            String[] srRules = rules[0];
            for (int i = 0; i < srRules.length; i++) {
                sb.append(srRules[i]).append(',');
            }
            String args = "vpc_staticroute.sh " + routerIp;
            args += " -a " + sb.toString();
            callResult = callHostPlugin(conn, "vmops", "routerProxy", "args", args);
            if (callResult == null || callResult.isEmpty()) {
                //FIXME - in the future we have to process each rule separately; now we temporarily set every rule to be false if single rule fails
                for (int i=0; i < results.length; i++) {
                    results[i] = "Failed";
                }
                return new SetStaticRouteAnswer(cmd, false, results);
            }
            return new SetStaticRouteAnswer(cmd, true, results);
        } catch (Exception e) {
            String msg = "SetStaticRoute failed due to " + e.toString();
            s_logger.error(msg, e);
            return new SetStaticRouteAnswer(cmd, false, null);
        }
    }

	@Override
	public void setName(String name) {
	}

	@Override
	public void setConfigParams(Map<String, Object> params) {
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
	public void setRunLevel(int level) {
	}
}
