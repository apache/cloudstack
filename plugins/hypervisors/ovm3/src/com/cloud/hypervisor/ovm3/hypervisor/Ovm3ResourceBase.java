// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership. The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License. You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied. See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.hypervisor.ovm3.hypervisor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
// import java.net.URISyntaxException;
/*
 * import java.util.ArrayList;
 */
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.net.URL;

import org.apache.commons.lang.BooleanUtils;

import com.google.gson.Gson;

import org.apache.commons.codec.binary.Base64;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.cloudstack.storage.to.TemplateObjectTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;

// import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
// import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import com.cloud.agent.IAgentControl;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.AttachIsoCommand;
import com.cloud.agent.api.AttachVolumeAnswer;
import com.cloud.agent.api.AttachVolumeCommand;
import com.cloud.agent.api.CheckNetworkAnswer;
import com.cloud.agent.api.CheckNetworkCommand;
import com.cloud.agent.api.CheckVirtualMachineAnswer;
import com.cloud.agent.api.CheckVirtualMachineCommand;
// import com.cloud.agent.api.CleanupNetworkRulesCmd;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.CreatePrivateTemplateFromVolumeCommand;
import com.cloud.agent.api.CreateStoragePoolCommand;
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
import com.cloud.agent.api.GetDomRVersionAnswer;
import com.cloud.agent.api.GetDomRVersionCmd;
import com.cloud.agent.api.NetworkRulesSystemVmCommand;
import com.cloud.agent.api.routing.DhcpEntryCommand;
import com.cloud.agent.api.routing.IpAssocCommand;
import com.cloud.agent.api.routing.IpAssocVpcCommand;
import com.cloud.agent.api.routing.SavePasswordCommand;
import com.cloud.agent.api.routing.SetNetworkACLCommand;
import com.cloud.agent.api.routing.SetSourceNatCommand;
import com.cloud.agent.api.routing.VmDataCommand;
// import com.cloud.agent.api.routing.DhcpEntryAnswer;
import com.cloud.agent.api.HostStatsEntry;
import com.cloud.agent.api.HostVmStateReportEntry;
import com.cloud.agent.api.MaintainAnswer;
import com.cloud.agent.api.MaintainCommand;
import com.cloud.agent.api.MigrateAnswer;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.agent.api.ModifyStoragePoolAnswer;
import com.cloud.agent.api.ModifyStoragePoolCommand;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.PingRoutingCommand;
import com.cloud.agent.api.PingTestCommand;
import com.cloud.agent.api.PrepareForMigrationAnswer;
import com.cloud.agent.api.PrepareForMigrationCommand;
// import com.cloud.agent.api.PrepareOCFS2NodesCommand;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.RebootAnswer;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.SetupGuestNetworkCommand;
/*
 * import com.cloud.agent.api.SecurityGroupRuleAnswer;
 * import com.cloud.agent.api.SecurityGroupRulesCmd;
 */
import com.cloud.agent.api.StartAnswer;
import com.cloud.agent.api.StartCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.agent.api.StartupStorageCommand;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.VmStatsEntry;
import com.cloud.agent.api.storage.CopyVolumeAnswer;
import com.cloud.agent.api.storage.CopyVolumeCommand;
import com.cloud.agent.api.storage.CreateAnswer;
import com.cloud.agent.api.storage.CreateCommand;
import com.cloud.agent.api.storage.CreatePrivateTemplateAnswer;
import com.cloud.agent.api.storage.DestroyCommand;
import com.cloud.agent.api.storage.PrimaryStorageDownloadAnswer;
import com.cloud.agent.api.storage.PrimaryStorageDownloadCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.host.Host.Type;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetworkSetupInfo;
// import com.cloud.network.NetworkModel;
import com.cloud.resource.ServerResource;
import com.cloud.resource.hypervisor.HypervisorResource;
// import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.Volume;
import com.cloud.storage.template.TemplateProp;
import com.cloud.template.VirtualMachineTemplate.BootloaderType;
import com.cloud.utils.ExecutionResult;
// import com.cloud.utils.Pair;
// import com.cloud.utils.Ternary;
import com.cloud.utils.exception.CloudRuntimeException;
// import com.cloud.utils.script.Script;
import com.cloud.utils.ssh.SshHelper;
import com.trilead.ssh2.SCPClient;
import com.cloud.utils.ssh.SSHCmdHelper;
import com.cloud.agent.api.check.CheckSshAnswer;
import com.cloud.agent.api.check.CheckSshCommand;
import com.cloud.agent.resource.virtualnetwork.VirtualRoutingResource;
import com.cloud.agent.resource.virtualnetwork.VirtualRouterDeployer;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineName;
import com.cloud.vm.VirtualMachine.PowerState;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.hypervisor.ovm3.object.Common;
import com.cloud.hypervisor.ovm3.object.Connection;
import com.cloud.hypervisor.ovm3.object.Linux;
import com.cloud.hypervisor.ovm3.object.Network;
import com.cloud.hypervisor.ovm3.object.OvmObject;
import com.cloud.hypervisor.ovm3.object.Pool;
import com.cloud.hypervisor.ovm3.object.PoolOCFS2;
import com.cloud.hypervisor.ovm3.object.Repository;
import com.cloud.hypervisor.ovm3.object.StoragePlugin;
import com.cloud.hypervisor.ovm3.object.Xen;
import com.cloud.hypervisor.ovm3.object.CloudStackPlugin;

import org.apache.cloudstack.storage.command.CopyCommand;
import org.apache.cloudstack.storage.command.DeleteCommand;
import org.apache.cloudstack.storage.command.CopyCmdAnswer;
import org.apache.cloudstack.storage.command.CreateObjectAnswer;
import org.apache.cloudstack.storage.command.CreateObjectCommand;

import com.cloud.storage.resource.StorageProcessor;

import org.apache.cloudstack.storage.command.ForgetObjectCmd;
import org.apache.cloudstack.storage.command.IntroduceObjectCmd;
import org.apache.cloudstack.storage.command.DettachCommand;
// import org.apache.cloudstack.storage.command.DettachAnswer;
import org.apache.cloudstack.storage.command.AttachCommand;

// import org.apache.cloudstack.storage.command.AttachAnswer;
import com.cloud.storage.Storage.ImageFormat;
/* do we need this ? */
import com.cloud.utils.db.GlobalLock;
import com.cloud.resource.ResourceManager;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;
import com.cloud.utils.script.OutputInterpreter.AllLinesParser;

import org.apache.commons.io.FileUtils;

import javax.inject.Inject;

/* TODO: Seperate these out */
public class Ovm3ResourceBase implements ServerResource, HypervisorResource,
        StorageProcessor, VirtualRouterDeployer {
    private static final Logger s_logger = Logger
            .getLogger(Ovm3ResourceBase.class);
    private Connection c;
    private Connection m;
    private String _name;
    private String _ip;
    Long _zoneId;
    Long _podId;
    Long _poolId;
    Long _clusterId;
    String _host;
    String _guid;
    String _username = "root";
    String _password;
    String _agentUserName = "oracle";
    String _agentPassword;
    Integer _agentPort = 8899;
    Boolean _agentSsl = false;
    String _ovmSshKey = "id_rsa.cloud";
    String _masterUuid = "d1a749d4295041fb99854f52ea4dea97";
    Boolean _isMaster = false;
    Boolean _hasMaster = false;
    Boolean _ovm3pool = false;
    Boolean _ovm3cluster = false;
    String _ovm3vip = "";
    protected boolean _checkHvm = false;
    /* _clusterDetailsDao.findDetail(clusterId, getVagKey(storagePoolId)); */
    String _privateNetworkName;
    String _publicNetworkName;
    String _guestNetworkName;
    String _storageNetworkName;
    String _controlNetworkName = "control0";
    String _controlNetworkIp = "169.254.0.1";
    // String _controlNetworkMask = "255.255.0.0";
    boolean _canBridgeFirewall = false;
    String _ovmRepo = "/OVS/Repositories";
    String _ovmSec = "/nfsmnt";
    OvmObject _ovmObject = new OvmObject();
    static boolean s_isHeartBeat = false;
    protected final int DefaultDomRSshPort = 3922;
    String DefaultDomRPath = "/opt/cloud/bin/";
    private final int _timeout = 600;
    private VirtualRoutingResource _virtRouterResource;
    private Map<String, Network.Interface> _interfaces = null;
    /* switch to concurrenthasmaps all over the place ? */
    private final ConcurrentHashMap<String, Map<String, String>> _vmStats = new ConcurrentHashMap<String, Map<String, String>>();

    // TODO vmsync {
    /* This is replaced by the vmList in getall VMs, and operate on that */
    // protected HashMap<String, State> _vms = new HashMap<String, State>(250);
    protected Map<String, Xen.Vm> _vms = new HashMap<String, Xen.Vm>();
    protected Map<String, State> _vmstates = new HashMap<String, State>();

    static HashMap<String, State> s_stateMaps;
    static {
        s_stateMaps = new HashMap<String, State>();
        s_stateMaps.put("Stopping", State.Stopping);
        s_stateMaps.put("Running", State.Running);
        s_stateMaps.put("Stopped", State.Stopped);
        s_stateMaps.put("Error", State.Error);
        s_stateMaps.put("Suspended", State.Running);
        s_stateMaps.put("Paused", State.Running);
        s_stateMaps.put("Migrating", State.Migrating);
    }

    static HashMap<String, PowerState> s_powerStateMaps;
    static {
        s_powerStateMaps = new HashMap<String, PowerState>();
        s_powerStateMaps.put("Stopping", PowerState.PowerOn);
        s_powerStateMaps.put("Running", PowerState.PowerOn);
        s_powerStateMaps.put("Stopped", PowerState.PowerOff);
        s_powerStateMaps.put("Error", PowerState.PowerUnknown);
        s_powerStateMaps.put("Suspended", PowerState.PowerOn);
        s_powerStateMaps.put("Paused", PowerState.PowerOn);
        /* unknown ? */
        s_powerStateMaps.put("Migrating", PowerState.PowerOn);
    }

    @Inject
    PrimaryDataStoreDao _storagePoolDao;
    @Inject
    HostDao _hostDao;
    @Inject
    ResourceManager _resourceMgr;

    GlobalLock _exclusiveOpLock = GlobalLock.getInternLock("ovm3.exclusive.op");

    /* return params we want to add, damnit */
    @Override
    public boolean configure(String name, Map<String, Object> params)
            throws ConfigurationException {
        _name = name;
        s_logger.debug("configure " + name + " with params: " + params);
        /* do we have enough ? */
        try {
            _zoneId = Long.parseLong((String) params.get("zone"));
            _podId = Long.parseLong((String) params.get("pod"));
            _clusterId = Long.parseLong((String) params.get("cluster"));
            _ovm3vip = String.valueOf(params.get("ovm3vip"));
            _ovm3pool = BooleanUtils.toBoolean((String) params.get("ovm3pool"));
            _ovm3cluster = BooleanUtils.toBoolean((String) params
                    .get("ovm3cluster"));
            _host = (String) params.get("host");
            _ip = (String) params.get("ip");
            _username = (String) params.get("username");
            _password = (String) params.get("password");
            _guid = (String) params.get("guid");
            _agentUserName = (String) params.get("agentusername");
            _agentPassword = (String) params.get("agentpassword");
            _privateNetworkName = (String) params.get("private.network.device");
            _publicNetworkName = (String) params.get("public.network.device");
            _guestNetworkName = (String) params.get("guest.network.device");
            _storageNetworkName = (String) params.get("storage.network.device");

            if (params.get("agentport") != null)
                _agentPort = Integer.parseInt((String) params.get("agentport"));

            // Add later
            // _agentSsl = (Boolean)params.get("agentssl");
        } catch (Exception e) {
            s_logger.debug("Configure " + _host + " failed", e);
            throw new ConfigurationException("Configure " + _host + " failed, "
                    + e.toString());
        }

        if (_podId == null) {
            String msg = "Unable to get the pod";
            s_logger.debug(msg);
            throw new ConfigurationException(msg);
        }

        if (_host == null) {
            String msg = "Unable to get the host";
            s_logger.debug(msg);
            throw new ConfigurationException(msg);
        }

        if (_username == null) {
            String msg = "Unable to get the username";
            s_logger.debug(msg);
            throw new ConfigurationException(msg);
        }

        if (_password == null) {
            String msg = "Unable to get the password";
            s_logger.debug(msg);
            throw new ConfigurationException(msg);
        }

        if (_guid == null) {
            String msg = "Unable to get the guid";
            s_logger.debug(msg);
            throw new ConfigurationException(msg);
        }

        if (_agentUserName == null) {
            String msg = "Unable to get the agent username";
            s_logger.debug(msg);
            throw new ConfigurationException(msg);
        }

        if (_agentPassword == null) {
            String msg = "Unable to get the agent password";
            s_logger.debug(msg);
            throw new ConfigurationException(msg);
        }

        if (_agentPort == null) {
            String msg = "Unable to get the agent port";
            s_logger.debug(msg);
            throw new ConfigurationException(msg);
        }

        /* TODO: Needs to be relocated */
        if (_ovm3vip.equals("")) {
            s_logger.debug("No VIP, Setting ovm3pool and ovm3cluster to false");
            this._ovm3pool = false;
            this._ovm3cluster = false;
            this._ovm3vip = "";
        }
        /* if we're a cluster we have to be a pool ? */
        if (_ovm3cluster) {
            this._ovm3pool = true;
        }

        /* check if we're master or not and if we can connect */
        try {
            try {
                c = new Connection(_host, _agentPort, _agentUserName,
                        _agentPassword);
            } catch (XmlRpcException ex) {
                String msg = "Unable to connect to " + _host;
                s_logger.warn(msg + ": " + ex.getMessage());
                throw new Exception(msg, ex);
            }
            this._isMaster = masterCheck();
        } catch (Exception e) {
            String msg = "Base checks failed for " + _host;
            s_logger.debug(msg, e);
            throw new ConfigurationException(msg);
        }
        /* setup ovm3 plugin */
        try {
            installOvsPlugin();
            CloudStackPlugin cSp = new CloudStackPlugin(c);
            cSp.ovsUploadSshKey(this._ovmSshKey,
                    FileUtils.readFileToString(getSystemVMKeyFile()));
        } catch (Exception e) {
            String msg = "Failed to setup server: " + _host;
            s_logger.error(msg + ": " + e.getMessage());
            throw new ConfigurationException(msg + ", " + e);
        }

        try {
            try {
                /*
                 * TODO: setup meta tags for the management interface (probably
                 * required with multiple interfaces)?
                 */
                Network net = new Network(c);
                _interfaces = net.getInterfaceList();
                s_logger.debug("all interfaces: " + _interfaces);
                if (_controlNetworkName != null
                        && !_interfaces.containsKey(_controlNetworkName)) {
                    /*
                     * TODO: find a more elegant way to do this for now we need
                     * the route del and ifconfig as loopback bridges have arp
                     * disabled by default, and zeroconf configures a route on
                     * the main bridge...
                     */
                    try {
                        net.startOvsLocalConfig(_controlNetworkName);
                    } catch (Exception e) {
                        s_logger.debug("Unable to configure"
                                + _controlNetworkName + ":" + e.getMessage());
                    }
                    /* ovs replies too "fast" so the bridge can be "busy" */
                    while (!_interfaces.containsKey(_controlNetworkName)) {
                        s_logger.debug("waiting for " + _controlNetworkName);
                        _interfaces = net.getInterfaceList();
                        Thread.sleep(1 * 1000);
                    }
                }
                /* The bridge is remembered upon reboot, but not the IP */
                net.ovsIpConfig(_controlNetworkName, "static",
                        _controlNetworkIp, "255.255.0.0");
                CloudStackPlugin cSp = new CloudStackPlugin(c);
                cSp.ovsControlInterface(_controlNetworkName, _controlNetworkIp
                        + "/16");

                // Missing netM = new Missing(c);
                /* build ovs_if_meta in Net based on the following */
                if (_privateNetworkName != null
                        && net.getBridgeByName(_privateNetworkName).getName() == null) {
                    throw new ConfigurationException(
                            "Cannot find private bridge "
                                    + _privateNetworkName
                                    + " on host "
                                    + _host
                                    + " - "
                                    + net.getBridgeByName(_privateNetworkName)
                                            .getName());
                }
                if (_publicNetworkName != null
                        && net.getBridgeByName(_publicNetworkName).getName() == null) {
                    throw new ConfigurationException(
                            "Cannot find private bridge "
                                    + _publicNetworkName
                                    + " on host "
                                    + _host
                                    + " - "
                                    + net.getBridgeByName(_publicNetworkName)
                                            .getName());
                }
                if (_guestNetworkName != null
                        && net.getBridgeByName(_guestNetworkName).getName() == null) {
                    throw new ConfigurationException(
                            "Cannot find private bridge "
                                    + _guestNetworkName
                                    + " on host "
                                    + _host
                                    + " - "
                                    + net.getBridgeByName(_guestNetworkName)
                                            .getName());
                }
                if (_storageNetworkName != null
                        && net.getBridgeByName(_storageNetworkName).getName() == null) {
                    throw new ConfigurationException(
                            "Cannot find private bridge "
                                    + _storageNetworkName
                                    + " on host "
                                    + _host
                                    + " - "
                                    + net.getBridgeByName(_storageNetworkName)
                                            .getName());
                }
            } catch (Exception e) {
                s_logger.debug("Get bridges failed on host " + _host + ", ", e);
                throw new ConfigurationException("Cannot get bridges on host "
                        + _host + ", " + e);
            }
            try {
                prepareForPool();
            } catch (Exception e) {
                throw new ConfigurationException("Failed to prepare for pool "
                        + _host + ", " + e);
            }

            /*
             * set to false so each time ModifyStoragePoolCommand will re-setup
             * heartbeat
             */
            /*
             * only required if we do cluster and then o2cb will take care of it
             * s_isHeartBeat = false;
             */

            /*
             * - uses iptables in XenServer, but not agent accessible for agent
             * *should make module*
             */
            try {
                _canBridgeFirewall = canBridgeFirewall();
            } catch (XmlRpcException e) {
                s_logger.error(
                        "Failed to detect whether the host supports security groups.",
                        e);
                _canBridgeFirewall = false;
            }

            s_logger.debug(_canBridgeFirewall ? "OVM3 host supports security groups."
                    : "OVM3 host doesn't support security groups.");
            /*
             * } catch (XmlRpcException e) { String msg =
             * "XML RPC Exception, unable to setup host: " + _host + " " + e;
             * s_logger.debug(msg); throw new ConfigurationException(msg);
             */
        } catch (Exception e) {
            String msg = "Generic Exception, failed to setup host: " + _host;
            s_logger.debug(msg + ": " + e.getMessage());
            throw new ConfigurationException(msg + ":" + e.getMessage());
        } /*
           * catch (IOException e) { s_logger.debug("Failed to setup host " +
           * _host, e); throw new
           * ConfigurationException("Unable to setup host"); }
           */
        return true;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public Type getType() {
        return Type.Routing;
    }

    protected void fillHostInfo(StartupRoutingCommand cmd) {
        try {
            /* get data we need from parts */
            Linux host = new Linux(c);
            if (!host.getOvmVersion().startsWith("3.2.")) {
                throw new CloudRuntimeException(
                        "OVM 3.2.X is only supported, not "
                                + host.getOvmVersion());
            }
            Xen vms = new Xen(c);
            vms.listVms();
            Xen.Vm dom0 = vms.getRunningVmConfig("Domain-0");

            cmd.setName(host.getHostName());
            cmd.setSpeed(host.getCpuKhz());
            cmd.setCpus(host.getTotalThreads());
            cmd.setCpuSockets(host.getCpuSockets());
            cmd.setMemory(host.getMemory().longValue());
            /*
             * TODO: Convert Bigint to long in Linux
             */
            BigInteger totalmem = BigInteger.valueOf(host.getMemory()
                    .longValue());
            BigInteger freemem = BigInteger.valueOf(host.getFreeMemory()
                    .longValue());
            cmd.setDom0MinMemory(totalmem.subtract(freemem).longValue());
            // setPoolSync and setCaps.
            cmd.setGuid(_guid);
            cmd.setDataCenter(_zoneId.toString());
            cmd.setPod(_podId.toString());
            // also set uuid for ownership, or else pooling/clustering will not
            // work
            // cmd.setOwner(host.getManagerUuid());
            cmd.setCluster(_clusterId.toString());
            cmd.setHypervisorVersion(host.getOvmVersion());
            cmd.setVersion(host.getAgentVersion());
            cmd.setHypervisorType(HypervisorType.Ovm3);
            /* is this true ? */
            cmd.setCaps(host.getCapabilities());
            // TODO: Control ip, for now cheat ?
            cmd.setPrivateIpAddress(_ip);
            cmd.setStorageIpAddress(_ip);
            cmd.setHostVmStateReport(HostVmStateReport());

            Network net = new Network(c);
            /* more detail -- fix -- and shouldn't matter */
            /* should this be the bond before the bridge ? */
            String defaultBridge = net.getBridgeByIp(_ip).getName();
            if (defaultBridge == null) {
                throw new CloudRuntimeException(
                        "Unable to obtain valid bridge with " + _ip);
            }

            /* TODO: cleanup network section, this is not right */
            if (_publicNetworkName == null) {
                _publicNetworkName = defaultBridge;
            }
            if (_privateNetworkName == null) {
                _privateNetworkName = _publicNetworkName;
            }
            if (_guestNetworkName == null) {
                _guestNetworkName = _publicNetworkName;
            }
            /*
             * if (_storageNetworkName == null) { _storageNetworkName =
             * _privateNetworkName; }
             */
            Map<String, String> d = cmd.getHostDetails();
            d.put("public.network.device", _publicNetworkName);
            if (_privateNetworkName != null)
                d.put("private.network.device", _privateNetworkName);
            if (_guestNetworkName != null)
                d.put("guest.network.device", _guestNetworkName);
            if (_storageNetworkName != null)
                d.put("storage.network.device", _storageNetworkName);
            d.put("ismaster", this._isMaster.toString());
            cmd.setHostDetails(d);
            s_logger.debug("Add an Ovm3 host " + _name + ":"
                    + cmd.getHostDetails());
        } catch (XmlRpcException e) {
            s_logger.debug("XML RPC Exception" + e.getMessage(), e);
            throw new CloudRuntimeException("XML RPC Exception"
                    + e.getMessage(), e);
        } catch (Exception e) {
            s_logger.debug("Exception " + e.getMessage(), e);
            throw new CloudRuntimeException("Exception" + e.getMessage(), e);
        }
    }

    /*
     * plugs the ovm module into the ovs-agent
     */
    protected void installOvsPlugin() throws IOException {
        /* ssh-copy-id anyone ? */
        try {
            com.trilead.ssh2.Connection sshConnection = SSHCmdHelper
                    .acquireAuthorizedConnection(_ip, _username, _password);
            if (sshConnection == null) {
                throw new ConfigurationException(String.format("Unable to "
                        + "connect to server(IP=%1$s, username=%2$s, "
                        + "password=%3$s", _ip, _username, _password));
            }
            SCPClient scp = new SCPClient(sshConnection);
            String userDataScript = "scripts/vm/hypervisor/ovm3/cloudstack.py";
            String userDataScriptPath = Script.findScript("", userDataScript);
            if (userDataScriptPath == null) {
                throw new ConfigurationException("Can not find "
                        + userDataScript);
            }
            scp.put(userDataScriptPath, "", "0755");
            String prepareCmd = String.format("./cloudstack.py " + "--ssl="
                    + this._agentSsl + " " + "--port=" + this._agentPort);
            if (!SSHCmdHelper.sshExecuteCmd(sshConnection, prepareCmd)) {
                throw new ConfigurationException("Module insertion at " + _host
                        + " failed");
            }
        } catch (Exception es) {
            s_logger.error("Unexpected exception ", es);
            String msg = "Unable to install module in agent";
            s_logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }

    /* startup */
    @Override
    public StartupCommand[] initialize() {
        s_logger.debug("Ovm3 resource intializing");
        try {
            StartupRoutingCommand srCmd = new StartupRoutingCommand();

            StartupStorageCommand ssCmd = new StartupStorageCommand();
            fillHostInfo(srCmd);
            String pool = srCmd.getPool();
            s_logger.debug("Ovm3 pool " + ssCmd + " " + srCmd);

            Map<String, State> changes = null;
            synchronized (_vmstates) {
                _vmstates.clear();
                changes = syncState();
            }
            srCmd.setStateChanges(changes);
            /* should not force HVM */
            // cmd.setCaps("hvm");
            return new StartupCommand[] { srCmd, ssCmd };
        } catch (Exception e) {
            s_logger.debug("Ovm3 resource initializes failed", e);
            return null;
        }
    }

    /* See if the agent is still up on the host */
    @Override
    public PingCommand getCurrentStatus(long id) {
        try {
            /* feels useless somehow */
            Common test = new Common(c);
            String ping = "put";
            String pong = test.echo(ping);
            if (pong.contains(ping)) {
                HashMap<String, State> newStates = syncState();
                return new PingRoutingCommand(getType(), id, newStates,
                        HostVmStateReport());
            }
        } catch (XmlRpcException e) {
            s_logger.debug("Check agent status failed", e);
            return null;
        } catch (Exception e) {
            s_logger.debug("Check agent status failed", e);
            return null;
        }
        return null;
    }

    /* Check if the host is in ready state for CS */
    protected ReadyAnswer execute(ReadyCommand cmd) {
        try {
            Linux host = new Linux(c);
            Pool pool = new Pool(c);

            /* only interesting when doing cluster */
            if (!host.getIsMaster() && _ovm3cluster) {
                if (pool.getPoolMasterVip().equalsIgnoreCase(_ip)) {
                    /* check pool state here */
                    return new ReadyAnswer(cmd);
                } else {
                    s_logger.debug("Master IP changes to "
                            + pool.getPoolMasterVip() + ", it should be " + _ip);
                    return new ReadyAnswer(cmd, "I am not the master server");
                }
            } else if (host.getIsMaster()) {
                s_logger.debug("Master, not clustered " + _host);
                return new ReadyAnswer(cmd);
            } else {
                s_logger.debug("No master, not clustered " + _host);
                return new ReadyAnswer(cmd);
            }
        } catch (XmlRpcException e) {
            s_logger.debug("XML RPC Exception" + e.getMessage(), e);
            throw new CloudRuntimeException("XML RPC Exception"
                    + e.getMessage(), e);
        } catch (Exception e) {
            s_logger.debug("Exception" + e.getMessage(), e);
            throw new CloudRuntimeException("Exception" + e.getMessage(), e);
        }

    }

    /*
     * Primary storage, will throw an error if ownership does not match! Pooling
     * is a part of this, for now
     */
    protected boolean createRepo(StorageFilerTO cmd) throws XmlRpcException {
        String basePath = this._ovmRepo;
        Repository repo = new Repository(c);
        String primUuid = repo.deDash(cmd.getUuid());
        String ovsRepo = basePath + "/" + primUuid;
        /* should add port ? */
        String mountPoint = String.format("%1$s:%2$s", cmd.getHost(),
                cmd.getPath());

        String msg;
        if (cmd.getType() == StoragePoolType.NetworkFilesystem) {
            /* base repo first */

            repo.mountRepoFs(mountPoint, ovsRepo);
            s_logger.debug("NFS repository " + mountPoint + " on " + ovsRepo
                    + " requested for " + _host);
            try {
                repo.addRepo(mountPoint, ovsRepo);
            } catch (Exception e) {
                s_logger.debug("NFS repository " + mountPoint + " on "
                        + ovsRepo + " not found creating!");
                try {
                    repo.createRepo(mountPoint, ovsRepo, primUuid,
                            "OVS Reposutory");
                } catch (Exception es) {
                    msg = "NFS repository " + mountPoint + " on " + ovsRepo
                            + " create failed!";
                    s_logger.debug(msg);
                    throw new CloudRuntimeException(
                            msg + " " + es.getMessage(), es);
                }
            }
            /* add base pooling first */
            if (this._ovm3pool) {
                try {
                    msg = "Configuring host for pool";
                    s_logger.debug(msg);
                    setupPool(cmd);
                    msg = "Configured host for pool";
                    /* add clustering after pooling */
                    if (this._ovm3cluster) {
                        msg = "Configuring host for cluster";
                        s_logger.debug(msg);
                        /* setup cluster */
                        /*
                         * From cluster.java
                         * configure_server_for_cluster(cluster conf, fs, mount,
                         * fsuuid, poolfsbaseuuid)
                         */
                        /* create_cluster(poolfsuuid,) */
                        msg = "Configuring host for cluster";
                    }
                } catch (Exception e) {
                    msg = "Unable to setup pool on " + ovsRepo;
                    s_logger.debug(msg);
                    throw new CloudRuntimeException(msg + " " + e.getMessage(),
                            e);
                }
            } else {
                msg = "no way dude I can't stand for this";
                s_logger.debug(msg);
            }
            /*
             * this is to create the .generic_fs_stamp else we're not allowed to
             * create any data\disks on this thing
             */
            try {
                URI uri = new URI(cmd.getType() + "://" + cmd.getHost() + ":"
                        + +cmd.getPort() + cmd.getPath() + "/VirtualMachines");
                this.setupNfsStorage(uri, cmd.getUuid());
            } catch (Exception e) {
                msg = "NFS mount " + mountPoint + " on " + _ovmSec + "/"
                        + cmd.getUuid() + " create failed!";
                s_logger.debug(msg);
                throw new CloudRuntimeException(msg + " " + e.getMessage(), e);
            }
        } else {
            msg = "NFS repository " + mountPoint + " on " + ovsRepo
                    + " create failed, was type " + cmd.getType();
            s_logger.debug(msg);
            return false;
        }

        try {
            /* systemvm iso is imported here */
            prepareSecondaryStorageStore(ovsRepo);
        } catch (Exception e) {
            msg = "systemvm.iso copy failed to " + ovsRepo;
            s_logger.debug(msg);
            return false;
        }
        return true;
    }

    /*  */
    public void prepareSecondaryStorageStore(String storageUrl) {
        String mountPoint = storageUrl;

        GlobalLock lock = GlobalLock.getInternLock("prepare.systemvm");
        try {
            /* double check */
            if (this._hasMaster && this._ovm3pool) {
                s_logger.debug("Skip systemvm iso copy, leave it to the master");
                return;
            }
            if (lock.lock(3600)) {
                try {
                    File srcIso = getSystemVMPatchIsoFile();
                    String destPath = mountPoint + "/ISOs/";
                    String repoPath[] = mountPoint.split(File.separator);
                    String destIso = destPath + "/"
                            + getSystemVMIsoFileNameOnDatastore();
                    String result = "";
                    try {
                        StoragePlugin sp = new StoragePlugin(c);
                        // sp.storagePluginGetFileInfo(repoPath[repoPath.length],
                        // destIso);
                        if (sp.getFileSize() > 0) {
                            s_logger.info(" System VM patch ISO file already exists: "
                                    + srcIso.getAbsolutePath().toString()
                                    + ", destination: " + destIso);
                        }
                    } catch (Exception e) {
                        /*
                         * Cloudstack already does this at boot time:
                         * s_logger.info(
                         * "Inject SSH key pairs before copying systemvm.iso into secondary storage"
                         * );
                         */
                        s_logger.info("Copy System VM patch ISO file to secondary storage. source ISO: "
                                + srcIso.getAbsolutePath()
                                + ", destination: "
                                + destIso);
                        try {
                            /*
                             * should actualy come from importIso in
                             * Storageplugin, so craft a url that can be used
                             * for that.
                             */
                            SshHelper.scpTo(this._host, 22, this._username,
                                    null, this._password, destPath, srcIso
                                            .getAbsolutePath().toString(),
                                    "0644");
                        } catch (Exception es) {
                            s_logger.error("Unexpected exception ", es);
                            String msg = "Unable to copy systemvm ISO on secondary storage. src location: "
                                    + srcIso.toString()
                                    + ", dest location: "
                                    + destIso;
                            s_logger.error(msg);
                            throw new CloudRuntimeException(msg);
                        }
                    }
                } finally {
                    lock.unlock();
                }
            }
        } finally {
            lock.releaseRef();
        }
    }

    /* stolen from vmware impl */
    public String getSystemVMIsoFileNameOnDatastore() {
        String version = this.getClass().getPackage()
                .getImplementationVersion();
        String fileName = "systemvm-" + version + ".iso";
        return fileName.replace(':', '-');
    }

    /* stolen from vmware impl */
    private File getSystemVMPatchIsoFile() {
        // locate systemvm.iso
        String svmName = getSystemVMIsoFileNameOnDatastore();
        URL url = this.getClass().getClassLoader()
                .getResource("vms/" + svmName);
        s_logger.debug(url);
        File isoFile = null;
        if (url != null) {
            isoFile = new File(url.getPath());
        }
        if (isoFile == null || !isoFile.exists()) {
            isoFile = new File("/usr/share/cloudstack-common/vms/" + svmName);
        }
        if (isoFile == null || !isoFile.exists()) {
            String svm = "systemvm/dist/systemvm.iso";
            s_logger.debug("last resort for systemvm patch iso " + svm);
            isoFile = new File(svm);
        }
        assert (isoFile != null);
        if (!isoFile.exists()) {
            s_logger.error("Unable to locate " + svmName + " in your setup at "
                    + isoFile.toString());
        }
        return isoFile;
    }

    /* get the key */
    public File getSystemVMKeyFile() {
        URL url = this.getClass().getClassLoader()
                .getResource("scripts/vm/systemvm/" + this._ovmSshKey);
        File keyFile = null;
        if (url != null) {
            keyFile = new File(url.getPath());
        }
        if (keyFile == null || !keyFile.exists()) {
            keyFile = new File(
                    "/usr/share/cloudstack-common/scripts/vm/systemvm/"
                            + _ovmSshKey);
        }
        assert (keyFile != null);
        if (!keyFile.exists()) {
            s_logger.error("Unable to locate " + _ovmSshKey
                    + " in your setup at " + keyFile.toString());
        }
        return keyFile;
    }

    /*
     * TODO: local OCFS2? or iSCSI OCFS2
     */
    protected Boolean createOCFS2Sr(StorageFilerTO pool) throws XmlRpcException {
        /*
         * Ovm3StoragePool.Details d = new Ovm3StoragePool.Details(); d.path =
         * pool.getPath(); d.type = Ovm3StoragePool.OCFS2; d.uuid =
         * pool.getUuid(); Ovm3StoragePool.create(_conn, d);
         * s_logger.debug(String.format("Created SR (mount point:%1$s)",
         * d.path));
         */
        s_logger.debug("OCFS2 Not implemented yet");
        return false;
    }

    /* TODO: heartbeats are done by the oracle cluster when clustering */
    private void setupHeartBeat(String poolUuid) {
        try {
            if (!s_isHeartBeat) {
                // Ovm3Host.setupHeartBeat(_conn, poolUuid, _ip);
                s_isHeartBeat = true;
            }
        } catch (Exception e) {
            s_logger.debug("setup heart beat for " + _host + " failed", e);
            s_isHeartBeat = false;
        }
    }

    /* Setup a storage pool and also get the size */
    protected Answer execute(ModifyStoragePoolCommand cmd) {
        StorageFilerTO pool = cmd.getPool();
        s_logger.debug("modifying pool " + pool);
        try {
            if (pool.getType() == StoragePoolType.NetworkFilesystem) {
                /* this should actually not be here */
                createRepo(pool);
            } else if (pool.getType() == StoragePoolType.OCFS2) {
                createOCFS2Sr(pool);
            } else {
                return new Answer(cmd, false, "The pool type: "
                        + pool.getType().name() + " is not supported.");
            }

            if (this._ovm3cluster) {
                // setupHeartBeat(pool.getUuid());
            }
            /* TODO: needs to be in network fs above */
            StoragePlugin store = new StoragePlugin(c);
            String propUuid = store.deDash(pool.getUuid());
            String mntUuid = pool.getUuid();
            String nfsHost = pool.getHost();
            String nfsPath = pool.getPath();
            store.setUuid(propUuid);
            store.setSsUuid(propUuid);
            store.setMntUuid(mntUuid);
            store.setFsHost(nfsHost);
            store.setFsSourcePath(nfsHost + ":" + nfsPath);
            store.storagePluginGetFileSystemInfo();

            Map<String, TemplateProp> tInfo = new HashMap<String, TemplateProp>();
            ModifyStoragePoolAnswer answer = new ModifyStoragePoolAnswer(cmd,
                    Long.parseLong(store.getTotalSize()), Long.parseLong(store
                            .getFreeSize()), tInfo);
            return answer;
        } catch (Exception e) {
            s_logger.debug("ModifyStoragePoolCommand failed", e);
            return new Answer(cmd, false, e.getMessage());
        }
    }

    /* TODO: add iSCSI */
    protected Answer execute(CreateStoragePoolCommand cmd) {
        StorageFilerTO pool = cmd.getPool();
        s_logger.debug("creating pool " + pool);
        try {
            if (pool.getType() == StoragePoolType.NetworkFilesystem) {
                createRepo(pool);
            } else if (pool.getType() == StoragePoolType.IscsiLUN) {
                return new Answer(cmd, false,
                        "iSCSI is unsupported at the moment");
                // getIscsiSR(conn, pool.getUuid(), pool.getHost(),
                // pool.getPath(), null, null, false);
            } else if (pool.getType() == StoragePoolType.OCFS2) {
                return new Answer(cmd, false,
                        "OCFS2 is unsupported at the moment");
            } else if (pool.getType() == StoragePoolType.PreSetup) {
                s_logger.warn("pre setup for pool " + pool);
            } else {
                return new Answer(cmd, false, "The pool type: "
                        + pool.getType().name() + " is not supported.");
            }
        } catch (Exception e) {
            String msg = "Catch Exception " + e.getClass().getName()
                    + ", create StoragePool failed due to " + e.toString()
                    + " on host:" + _host + " pool: " + pool.getHost()
                    + pool.getPath();
            s_logger.warn(msg, e);
            return new Answer(cmd, false, msg);
        }
        return new Answer(cmd, true, "success");
    }

    /*
     * Download some primary storage into the repository, we need the repoid to
     * do that, but also have a uuid for the disk... TODO: check disk TODO:
     * looks like we don't need this for now! (dead code)
     */
    protected PrimaryStorageDownloadAnswer execute(
            final PrimaryStorageDownloadCommand cmd) {
        try {
            Repository repo = new Repository(c);
            String tmplturl = cmd.getUrl();
            String poolName = cmd.getPoolUuid();
            String image = repo.deDash(repo.newUuid()) + ".raw";

            /* url to download from, image name, and repo to copy it to */
            repo.importVirtualDisk(tmplturl, image, poolName);

            /* TODO: return uuid and size */
            return new PrimaryStorageDownloadAnswer(image);
        } catch (Exception e) {
            s_logger.debug("PrimaryStorageDownloadCommand failed", e);
            return new PrimaryStorageDownloadAnswer(e.getMessage());
        }
    }

    /*
     * TODO: Split out in Sec to prim and prim to prim and types ? Storage mount
     * also goes in here for secstorage
     */
    protected final Answer execute(final CopyCommand cmd) {
        DataTO srcData = cmd.getSrcTO();
        DataStoreTO srcStore = srcData.getDataStore();
        DataTO destData = cmd.getDestTO();
        DataStoreTO destStore = destData.getDataStore();

        try {
            /* target and source are NFS and TEMPLATE */
            if ((srcStore instanceof NfsTO)
                    && (srcData.getObjectType() == DataObjectType.TEMPLATE)
                    && (destData.getObjectType() == DataObjectType.TEMPLATE)) {
                NfsTO srcImageStore = (NfsTO) srcStore;
                TemplateObjectTO srcTemplate = (TemplateObjectTO) srcData;
                String storeUrl = srcImageStore.getUrl();

                URI uri = new URI(storeUrl);
                String secPoolUuid = setupSecondaryStorage(storeUrl);
                String primaryPoolUuid = destData.getDataStore().getUuid();
                String destPath = this._ovmRepo + "/"
                        + _ovmObject.deDash(primaryPoolUuid) + "/"
                        + "Templates";
                String sourcePath = this._ovmSec + "/" + secPoolUuid;

                Linux host = new Linux(c);
                String destUuid = srcTemplate.getUuid();
                /*
                 * TODO: add dynamic formats (tolower), it also supports VHD and
                 * QCOW2, although Ovm3.2 does not have tapdisk2 anymore so we
                 * can forget about that.
                 */
                /* TODO: add checksumming */
                String srcFile = sourcePath + "/" + srcData.getPath();
                if (srcData.getPath().endsWith("/")) {
                    srcFile = sourcePath + "/" + srcData.getPath() + "/"
                            + destUuid + ".raw";
                }
                String destFile = destPath + "/" + destUuid + ".raw";
                s_logger.debug("CopyFrom: " + srcData.getObjectType() + ","
                        + srcFile + " to " + destData.getObjectType() + ","
                        + destFile);
                host.copyFile(srcFile, destFile);

                TemplateObjectTO newVol = new TemplateObjectTO();
                newVol.setUuid(destUuid);
                newVol.setPath(destPath);
                newVol.setFormat(ImageFormat.RAW);
                return new CopyCmdAnswer(newVol);
                /* we assume the cache for templates is local */
            } else if ((srcData.getObjectType() == DataObjectType.TEMPLATE)
                    && (destData.getObjectType() == DataObjectType.VOLUME)) {
                if (srcStore.getUrl().equals(destStore.getUrl())) {
                    TemplateObjectTO srcTemplate = (TemplateObjectTO) srcData;
                    VolumeObjectTO dstVolume = (VolumeObjectTO) destData;

                    String srcFile = srcTemplate.getPath() + "/"
                            + srcTemplate.getUuid() + ".raw";
                    String vDisksPath = srcTemplate.getPath().replace(
                            "Templates", "VirtualDisks");
                    String destFile = vDisksPath + "/" + dstVolume.getUuid()
                            + ".raw";

                    Linux host = new Linux(c);
                    s_logger.debug("CopyFrom: " + srcData.getObjectType() + ","
                            + srcFile + " to " + destData.getObjectType() + ","
                            + destFile);
                    host.copyFile(srcFile, destFile);
                    VolumeObjectTO newVol = new VolumeObjectTO();
                    newVol.setUuid(dstVolume.getUuid());
                    newVol.setPath(vDisksPath);
                    newVol.setFormat(ImageFormat.RAW);
                    return new CopyCmdAnswer(newVol);
                } else {
                    s_logger.debug("Primary to Primary doesn't match");
                }
            } else {
                String msg = "Unable to do stuff for " + srcStore.getClass()
                        + ":" + srcData.getObjectType() + " to "
                        + destStore.getClass() + ":" + destData.getObjectType();
                s_logger.debug(msg);
            }
        } catch (Exception e) {
            String msg = "Catch Exception " + e.getClass().getName()
                    + " for template due to " + e.toString();
            s_logger.warn(msg, e);
            return new CopyCmdAnswer(msg);
        }
        return new CopyCmdAnswer("not implemented yet");
    }

    protected Answer execute(DeleteCommand cmd) {
        DataTO data = cmd.getData();
        s_logger.debug("Deleting object: " + data.getObjectType());
        if (data.getObjectType() == DataObjectType.VOLUME) {
            return deleteVolume(cmd);
        } else if (data.getObjectType() == DataObjectType.SNAPSHOT) {

        } else if (data.getObjectType() == DataObjectType.TEMPLATE) {

        } else {

        }
        String msg = "Delete not implemented yet for this object";
        s_logger.debug(msg);
        return new Answer(cmd);
    }

    /* TODO: Create a Disk from a template needs cleaning */
    protected CreateAnswer execute(CreateCommand cmd) {
        StorageFilerTO primaryStorage = cmd.getPool();
        DiskProfile disk = cmd.getDiskCharacteristics();

        /* disk should have a uuid */
        String fileName = UUID.randomUUID().toString() + ".img";
        String dst = primaryStorage.getPath() + "/" + primaryStorage.getUuid()
                + "/" + fileName;

        try {
            StoragePlugin store = new StoragePlugin(c);
            store.setUuid(primaryStorage.getUuid());
            store.setName(primaryStorage.getUserInfo());
            store.setSsUuid(primaryStorage.getUserInfo());
            if (cmd.getTemplateUrl() != null) {
                s_logger.debug("CreateCommand " + cmd.getTemplateUrl() + " "
                        + dst);
                Linux host = new Linux(c);
                host.copyFile(cmd.getTemplateUrl(), dst);
            } else {
                /* this is a dup with the createVolume ? */
                s_logger.debug("CreateCommand " + dst);
                store.storagePluginCreate(primaryStorage.getUuid(),
                        primaryStorage.getHost(), dst, disk.getSize());
            }

            store.storagePluginGetFileInfo(dst);
            VolumeTO volume = new VolumeTO(cmd.getVolumeId(), disk.getType(),
                    primaryStorage.getType(), primaryStorage.getUuid(),
                    primaryStorage.getPath(), fileName, store.getFileName(),
                    store.getFileSize(), null);
            return new CreateAnswer(cmd, volume);
        } catch (Exception e) {
            s_logger.debug("CreateCommand failed", e);
            return new CreateAnswer(cmd, e.getMessage());
        }
    }

    /*
     * Add rootdisk, datadisk and iso's
     */
    protected Boolean createVbds(Xen.Vm vm, VirtualMachineTO spec) {
        for (DiskTO volume : spec.getDisks()) {
            try {
                if (volume.getType() == Volume.Type.ROOT) {
                    VolumeObjectTO vol = (VolumeObjectTO) volume.getData();
                    DataStoreTO ds = (DataStoreTO) vol.getDataStore();
                    String dsk = vol.getPath() + "/" + vol.getUuid() + ".raw";
                    vm.addRootDisk(dsk);
                    /* TODO: needs to be replaced by rootdiskuuid? */
                    vm.setPrimaryPoolUuid(ds.getUuid());
                    s_logger.debug("Adding root disk: " + dsk);
                } else if (volume.getType() == Volume.Type.ISO) {
                    DataTO isoTO = volume.getData();
                    if (isoTO.getPath() != null) {
                        TemplateObjectTO template = (TemplateObjectTO) isoTO;
                        DataStoreTO store = template.getDataStore();
                        if (!(store instanceof NfsTO)) {
                            throw new CloudRuntimeException(
                                    "unsupported protocol");
                        }
                        NfsTO nfsStore = (NfsTO) store;
                        String secPoolUuid = setupSecondaryStorage(nfsStore
                                .getUrl());
                        String isoPath = this._ovmSec + File.separator
                                + secPoolUuid + File.separator
                                + template.getPath();
                        // + template.getUuid() + ".iso";
                        vm.addIso(isoPath);
                        /* check if secondary storage is mounted */
                        s_logger.debug("Adding ISO: " + isoPath);
                    }
                } else if (volume.getType() == Volume.Type.DATADISK) {
                    vm.addDataDisk(volume.getData().getPath());
                    s_logger.debug("Adding data disk: "
                            + volume.getData().getPath());
                } else {
                    throw new CloudRuntimeException("Unknown volume type: "
                            + volume.getType());
                }
            } catch (Exception e) {
                s_logger.debug("CreateVbds failed", e);
                throw new CloudRuntimeException("Exception" + e.getMessage(), e);
            }
        }
        return true;
    }

    /* TODO: iptables/etables logic in the supporting libs ? */
    protected Boolean createVifs(Xen.Vm vm, VirtualMachineTO spec) {
        NicTO[] nics = spec.getNics();
        for (NicTO nic : nics) {
            if (nic.isSecurityGroupEnabled()) {
                if (spec.getType().equals(VirtualMachine.Type.User)) {
                    /*
                     * defaultNetworkRulesForUserVm(vmName, vmSpec.getId(),
                     * nic);
                     */
                }
            }
            try {
                if (getNetwork(nic) != null)
                    vm.addVif(nic.getDeviceId(), getNetwork(nic), nic.getMac());
            } catch (Exception e) {
                String msg = "Unable to add vif " + nic.getType() + " for "
                        + spec.getName() + " " + e.getMessage();
                s_logger.debug(msg);
                throw new CloudRuntimeException(msg);
            }
        }
        vm.setupVifs();
        return true;
    }

    /*
     * TODO: ovs calls ? depending on the type of network TODO: get the bridge
     * on a per VM base so we can throw it away?
     */
    private String createVlanBridge(String networkName, Integer vlanId)
            throws XmlRpcException {
        if (vlanId < 2 || vlanId > 4094) {
            throw new CloudRuntimeException("Vlan " + vlanId
                    + " needs to be between 1-4095");
        }
        Network net = new Network(c);
        /* figure out if our bridged vlan exists, if not then create */
        String brName = networkName + "." + vlanId.toString();
        try {
            String physInterface = net.getPhysicalByBridgeName(networkName);
            String physVlanInt = physInterface + "." + vlanId;

            if (net.getInterfaceByName(physVlanInt) == null)
                net.startOvsVlanConfig(physInterface, vlanId);

            if (net.getInterfaceByName(brName) == null)
                net.startOvsBrConfig(brName, physVlanInt);
        } catch (Exception e) {
            throw new CloudRuntimeException("Unable to create vlan "
                    + vlanId.toString() + " bridge for " + networkName
                    + e.getMessage());
        }
        return brName;
    }

    // TODO: complete all network support
    protected String getNetwork(NicTO nic) throws XmlRpcException {
        String vlanId = null;
        String bridgeName = null;
        if (nic.getBroadcastType() == BroadcastDomainType.Vlan) {
            vlanId = BroadcastDomainType.getValue(nic.getBroadcastUri());
        }

        if (nic.getType() == TrafficType.Guest) {
            if (nic.getBroadcastType() == BroadcastDomainType.Vlan
                    && !vlanId.equalsIgnoreCase("untagged")) {
                bridgeName = createVlanBridge(_guestNetworkName,
                        Integer.valueOf(vlanId));
            } else {
                bridgeName = _guestNetworkName;
            }
        } else if (nic.getType() == TrafficType.Control) {
            bridgeName = _controlNetworkName;
        } else if (nic.getType() == TrafficType.Public) {
            bridgeName = _publicNetworkName;
        } else if (nic.getType() == TrafficType.Management) {
            bridgeName = _privateNetworkName;
        } else if (nic.getType() == TrafficType.Storage) {
            /* TODO: Add storage network */
            bridgeName = _storageNetworkName;
        } else {
            throw new CloudRuntimeException("Unknown network traffic type:"
                    + nic.getType());
        }
        return bridgeName;
    }

    /* This is not create for us, but really start */
    protected boolean startVm(String repoId, String vmId)
            throws XmlRpcException {
        Xen host = new Xen(c);
        try {
            if (host.getRunningVmConfig(vmId) == null) {
                s_logger.error("Create VM " + vmId + " first on " + c.getIp());
                return false;
            } else {
                s_logger.info("VM " + vmId + " exists on " + c.getIp());
            }
            host.startVm(repoId, vmId);
        } catch (Exception e) {
            s_logger.error("Failed to start VM " + vmId + " on " + c.getIp()
                    + " " + e.getMessage());
            return false;
        }
        return true;
    }

    /*
     * TODO: OVM already cleans stuff up, just not the extra bridges which we
     * don't want right now, as we'd have to keep a state table of which vlans
     * need to stay on the host!? A map with vlanid -> list-o-hosts
     */
    private void cleanupNetwork(List<String> vifs) throws XmlRpcException {
        /* peel out vif info for vlan stuff */
        /*
         * for (String vif : vifs) { if (vif.bridge.startsWith("vlan")) {
         * Network net = new Network(c); net.ovsVlanBridgeStop(br, net, vlan);
         * // Ovm3Bridge.deleteVlanBridge(_conn, vif.bridge); } }
         */
    }

    protected void cleanup(Xen.Vm vm) {
        try {
            cleanupNetwork(vm.getVmVifs());
        } catch (XmlRpcException e) {
            s_logger.debug(
                    "Clean up network for " + vm.getVmName() + " failed", e);
        }
        String vmName = vm.getVmName();
        /* should become a single entity */
        this._vmStats.remove(vmName);
    }

    /*
     * TODO: The actual VM provisioning start to end, we can rip this from our
     * simple stuff
     */
    @Override
    public synchronized StartAnswer execute(StartCommand cmd) {
        VirtualMachineTO vmSpec = cmd.getVirtualMachine();
        String vmName = vmSpec.getName();
        State state = State.Stopped;
        Xen xen = new Xen(c);

        try {
            synchronized (this._vmstates) {
                this._vmstates.put(vmName, State.Starting);
            }
            Xen.Vm vm = xen.getVmConfig();
            /* max and min ? */
            vm.setVmCpus(vmSpec.getCpus());
            /* in mb not in bytes */
            vm.setVmMemory(vmSpec.getMinRam() / 1024 / 1024);
            vm.setVmUuid(UUID.nameUUIDFromBytes(vmSpec.getName().getBytes())
                    .toString());
            vm.setVmName(vmName);
            String domType = Ovm3Helper.getOvm3GuestType(vmSpec.getOs());
            if (domType == null || domType.equals("")) {
                domType = "default";
                s_logger.debug("VM Virt type missing setting to: " + domType);
            } else {
                s_logger.debug("VM Virt type set to " + domType + " for "
                        + vmSpec.getOs());
            }
            vm.setVmDomainType(domType);
            /* only for none user VMs? */
            vm.setVmExtra(vmSpec.getBootArgs().replace(" ", "%"));

            /* TODO: booting from CD... */
            if (vmSpec.getBootloader() == BootloaderType.CD) {
                // do something with this please...
            }
            /*
             * officially CD boot is only supported on HVM, although there is a
             * simple way around it..
             */
            /* TODO: pool uuid now comes from here should change! */
            createVbds(vm, vmSpec);

            if (vmSpec.getType() != VirtualMachine.Type.User) {
                String svmPath = _ovmRepo + "/"
                        + _ovmObject.deDash(vm.getPrimaryPoolUuid()) + "/ISOs";
                String svmIso = svmPath + "/"
                        + getSystemVMIsoFileNameOnDatastore();
                vm.addIso(svmIso);
            }
            /* TODO: OVS should go here! */
            createVifs(vm, vmSpec);

            /* vm migration requires a 0.0.0.0 bind */
            vm.setVncPassword(vmSpec.getVncPassword());
            vm.setVncAddress("0.0.0.0");
            vm.setVnc();

            /* this should be getVmRootDiskPoolId ? */
            xen.createVm(_ovmObject.deDash(vm.getPrimaryPoolUuid()),
                    vm.getVmUuid());
            xen.startVm(_ovmObject.deDash(vm.getPrimaryPoolUuid()),
                    vm.getVmUuid());
            state = State.Running;

            if (vmSpec.getType() != VirtualMachine.Type.User) {
                String controlIp = null;
                for (NicTO nic : vmSpec.getNics()) {
                    if (nic.getType() == TrafficType.Control) {
                        controlIp = nic.getIp();
                    }
                }

                try {
                    CloudStackPlugin cSp = new CloudStackPlugin(c);
                    for (int count = 0; count < 60; count++) {
                        Boolean res = cSp.domrCheckSsh(controlIp);
                        s_logger.debug("connected to " + controlIp
                                + " on attempt " + count + " result: " + res);
                        Thread.sleep(5000);
                        if (res) {
                            break;
                        }
                        /*
                         * Older xend issues in, took me a while to figure this
                         * out: ../xen/xend/XendConstants.py
                         * """Minimum time between domain restarts in seconds."
                         * "" MINIMUM_RESTART_TIME = 60 this does NOT
                         * work!!!--^^ as we respawn within 30 seconds easily.
                         * return state stopped.
                         */
                        if (_vmstates.get(vmName) == null) {
                            String msg = "VM " + vmName + " went missing on "
                                    + _host + ", returning stopped";
                            s_logger.debug(msg);
                            state = State.Stopped;
                            return new StartAnswer(cmd, msg);
                        }
                    }
                } catch (Exception x) {
                    s_logger.debug("unable to connect to " + controlIp + " "
                            + x.getMessage());
                }
            }
            /*
             * TODO: Can't remember if HA worked if we were only a pool ?
             */
            if (_ovm3pool && _ovm3cluster) {
                xen.configureVmHa(_ovmObject.deDash(vm.getPrimaryPoolUuid()),
                        vm.getVmUuid(), true);
            }
            /* should be starting no ? */
            state = State.Running;
            return new StartAnswer(cmd);
        } catch (Exception e) {
            s_logger.debug("Start vm " + vmName + " failed", e);
            state = State.Stopped;
            // cleanup(vmDetails);
            return new StartAnswer(cmd, e.getMessage());
        } finally {
            synchronized (this._vmstates) {
                // FIXME: where to come to Stopped???
                this._vmstates.put(vmName, state);
            }
        }
    }

    /* VR things */
    @Override
    public ExecutionResult executeInVR(String routerIp, String script,
            String args) {
        return executeInVR(routerIp, script, args, _timeout / 1000);
    }

    /* TODO: Double check */
    @Override
    public ExecutionResult executeInVR(String routerIp, String script,
            String args, int timeout) {
        final Script command = new Script(DefaultDomRPath, timeout * 1000,
                s_logger);
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
    public ExecutionResult createFileInVR(String routerIp, String path,
            String filename, String content) {
        File permKey = new File("/root/.ssh/id_rsa.cloud");
        String error = null;

        try {
            SshHelper.scpTo(routerIp, 3922, "root", permKey, null, path,
                    content.getBytes(), filename, null);
        } catch (Exception e) {
            s_logger.warn("Fail to create file " + path + filename + " in VR "
                    + routerIp, e);
            error = e.getMessage();
        }
        return new ExecutionResult(error == null, error);
    }

    @Override
    public ExecutionResult prepareCommand(NetworkElementCommand cmd) {
        // Update IP used to access router
        cmd.setRouterAccessIp(cmd
                .getAccessDetail(NetworkElementCommand.ROUTER_IP));
        assert cmd.getRouterAccessIp() != null;

        if (cmd instanceof IpAssocVpcCommand) {
            return prepareNetworkElementCommand((IpAssocVpcCommand) cmd);
        } else if (cmd instanceof IpAssocCommand) {
            return prepareNetworkElementCommand((IpAssocCommand) cmd);
        } else if (cmd instanceof SetupGuestNetworkCommand) {
            return prepareNetworkElementCommand((SetupGuestNetworkCommand) cmd);
        } else if (cmd instanceof SetSourceNatCommand) {
            return prepareNetworkElementCommand((SetSourceNatCommand) cmd);
        }
        return new ExecutionResult(true, null);
    }

    @Override
    public ExecutionResult cleanupCommand(NetworkElementCommand cmd) {
        if (cmd instanceof IpAssocCommand
                && !(cmd instanceof IpAssocVpcCommand)) {
            return cleanupNetworkElementCommand((IpAssocCommand) cmd);
        }
        return new ExecutionResult(true, null);
    }

    protected ExecutionResult cleanupNetworkElementCommand(IpAssocCommand cmd) {
        return null;
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

    /* TODO: fill in these so we can get rid of the VR/SVM/etc specifics */
    private ExecutionResult prepareNetworkElementCommand(
            SetupGuestNetworkCommand cmd) {
        return null;
    }

    private ExecutionResult prepareNetworkElementCommand(IpAssocVpcCommand cmd) {
        return null;
    }

    protected ExecutionResult prepareNetworkElementCommand(
            SetSourceNatCommand cmd) {
        return null;
    }

    private ExecutionResult prepareNetworkElementCommand(
            SetNetworkACLCommand cmd) {
        return null;
    }

    private ExecutionResult prepareNetworkElementCommand(IpAssocCommand cmd) {
        return null;
    }

    /* TODO: egh? */
    protected String getDefaultScriptsDir() {
        return null;
    }

    /* split out domr stuff later */
    private GetDomRVersionAnswer execute(GetDomRVersionCmd cmd) {
        String args = this.DefaultDomRPath + "get_template_version.sh";
        String ip = cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);
        try {
            CloudStackPlugin cSp = new CloudStackPlugin(c);
            CloudStackPlugin.ReturnCode result;
            result = cSp.domrExec(ip, args);
            if (!result.getRc() || result.getStdOut().isEmpty()) {
                return new GetDomRVersionAnswer(cmd, "getDomRVersionCmd failed");
            }
            String domResp = result.getStdOut();
            String[] lines = domResp.split("&");
            if (lines.length != 2) {
                return new GetDomRVersionAnswer(cmd, domResp);
            }
            return new GetDomRVersionAnswer(cmd, domResp, lines[0], lines[1]);
        } catch (Exception e) {
            return new GetDomRVersionAnswer(cmd, "getDomRVersionCmd "
                    + e.getMessage());
        }
    }

    private Answer giveDomRAns(Command cmd, String ip, String args, String resp) {
        CloudStackPlugin cSp = new CloudStackPlugin(c);
        try {
            CloudStackPlugin.ReturnCode result = cSp.domrExec(ip, args);
            if (!result.getRc()) {
                return new Answer(cmd, false, result.getStdOut());
            }
        } catch (Exception e) {
            return new Answer(cmd, false, e.getMessage());
        }
        return new Answer(cmd);
    }

    protected synchronized Answer execute(final DhcpEntryCommand cmd) {
        String args = this.DefaultDomRPath + "edithosts.sh";
        String ip = cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);
        if (cmd.getVmIpAddress() != null) {
            args += " -4 " + cmd.getVmIpAddress();
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

        return giveDomRAns(cmd, ip, args, "DhcpEntry failed");
    }

    protected Answer execute(final SavePasswordCommand cmd) {
        final String password = cmd.getPassword();
        final String ip = cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);
        final String vmIpAddress = cmd.getVmIpAddress();
        String args = this.DefaultDomRPath + "savepassword.sh ";
        args += " -v " + vmIpAddress;
        args += " -p " + password;
        return giveDomRAns(cmd, ip, args, "SavePassword failed");
    }

    protected Answer execute(final VmDataCommand cmd) {
        String ip = cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);
        Map<String, List<String[]>> data = new HashMap<String, List<String[]>>();
        data.put(cmd.getVmIpAddress(), cmd.getVmData());
        String json = new Gson().toJson(data);
        json = Base64.encodeBase64String(json.getBytes());
        String args = this.DefaultDomRPath + "vmdata.py -d " + json;
        return giveDomRAns(cmd, ip, args, "Set vm_data failed");
    }

    /*
     * we don't for now, gave an error on migration though....
     */
    private Answer execute(NetworkRulesSystemVmCommand cmd) {
        boolean success = true;
        if (_canBridgeFirewall) {
            // meh
        }
        return new Answer(cmd, success, "");
    }

    public CheckSshAnswer execute(CheckSshCommand cmd) {
        String vmName = cmd.getName();
        String privateIp = cmd.getIp();
        int cmdPort = cmd.getPort();
        int interval = cmd.getInterval();
        int retries = cmd.getRetries();

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Trying port " + privateIp + ":" + cmdPort);
        }
        try {
            CloudStackPlugin cSp = new CloudStackPlugin(c);
            if (!cSp.domrCheckPort(privateIp, cmdPort, retries, interval)) {
                s_logger.info(vmName + ":" + cmdPort + " nok");
                return new CheckSshAnswer(cmd, "unable to connect");
            }
            s_logger.info(vmName + ":" + cmdPort + " ok");
        } catch (Exception e) {
            s_logger.error("Can not reach port on System vm " + vmName
                    + " due to exception", e);
            return new CheckSshAnswer(cmd, e);
        }
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Ping command port succeeded for vm " + vmName + " "
                    + cmd);
        }
        if (VirtualMachineName.isValidRouterName(vmName)) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Execute network usage setup command on "
                        + vmName);
            }
            // TODO: check this one out...
        }
        return new CheckSshAnswer(cmd);
    }

    protected Answer execute(GetHostStatsCommand cmd) {
        try {
            CloudStackPlugin cSp = new CloudStackPlugin(c);
            Map<String, String> stats = cSp
                    .ovsDom0Stats(this._publicNetworkName);
            Double cpuUtil = Double.parseDouble(stats.get("cpu"));
            Double rxBytes = Double.parseDouble(stats.get("rx"));
            Double txBytes = Double.parseDouble(stats.get("tx"));
            Double totalMemory = Double.parseDouble(stats.get("total"));
            Double freeMemory = Double.parseDouble(stats.get("free"));
            HostStatsEntry hostStats = new HostStatsEntry(cmd.getHostId(),
                    cpuUtil, rxBytes, txBytes, "host", totalMemory, freeMemory,
                    0, 0);
            return new GetHostStatsAnswer(cmd, hostStats);
        } catch (Exception e) {
            s_logger.debug(
                    "Get host stats of " + cmd.getHostName() + " failed", e);
            return new Answer(cmd, false, e.getMessage());
        }
    }

    /* TODO: Stop the VM, this means cleanup too, should this be destroy ? */
    @Override
    public StopAnswer execute(StopCommand cmd) {
        String vmName = cmd.getVmName();
        State state = State.Error;
        synchronized (this._vmstates) {
            state = _vmstates.get(vmName);
            this._vmstates.put(vmName, State.Stopping);
        }

        try {
            Xen vms = new Xen(c);
            Xen.Vm vm = null;
            vm = vms.getRunningVmConfig(vmName);

            if (vm == null) {
                state = State.Stopping;
                s_logger.debug("Unable to get details of vm: " + vmName
                        + ", treating it as Stopping");
                return new StopAnswer(cmd, "success", true);
            }
            String repoId = _ovmObject.deDash(vm.getVmRootDiskPoolId());
            String vmId = vm.getVmUuid();

            /* can we do without the poolId ? */
            vms.stopVm(repoId, vmId);
            int tries = 30;
            while (vms.getRunningVmConfig(vmName) != null && tries > 0) {
                String msg = "Waiting for " + vmName + " to stop";
                s_logger.debug(msg);
                tries--;
                Thread.sleep(10 * 1000);
            }
            vms.deleteVm(repoId, vmId);
            /* TODO: Check cleanup */
            this.cleanup(vm);

            if (vms.getRunningVmConfig(vmName) != null) {
                String msg = "Stop " + vmName + " failed ";
                s_logger.debug(msg);
                return new StopAnswer(cmd, msg, false);
            }
            state = State.Stopped;
            return new StopAnswer(cmd, "success", true);
        } catch (Exception e) {
            /* TODO: check output of message, might be that it did get removed */
            s_logger.debug("Stop " + vmName + " failed ", e);
            return new StopAnswer(cmd, e.getMessage(), false);
        } finally {
            synchronized (this._vmstates) {
                if (state != null) {
                    this._vmstates.put(vmName, state);
                } else {
                    this._vmstates.remove(vmName);
                }
            }
        }
    }

    /* Reboot the VM and destroy should call the same method in here ? */
    @Override
    public RebootAnswer execute(RebootCommand cmd) {
        String vmName = cmd.getVmName();

        synchronized (this._vmstates) {
            this._vmstates.put(vmName, State.Starting);
        }

        try {
            Xen xen = new Xen(c);
            Xen.Vm vm = xen.getRunningVmConfig(vmName);
            /* TODO: stop, start or reboot, reboot for now ? */
            xen.rebootVm(_ovmObject.deDash(vm.getVmRootDiskPoolId()),
                    vm.getVmUuid());
            vm = xen.getRunningVmConfig(vmName);
            /* erh but this don't work, should point at cloudstackplugin */
            Integer vncPort = vm.getVncPort();
            return new RebootAnswer(cmd, null, vncPort);
        } catch (Exception e) {
            s_logger.debug("Reboot " + vmName + " failed", e);
            return new RebootAnswer(cmd, e.getMessage(), false);
        } finally {
            synchronized (this._vmstates) {
                this._vmstates.put(cmd.getVmName(), State.Running);
            }
        }
    }

    protected State convertPowerToState(PowerState ps) {
        final State state = s_stateMaps.get(ps.toString());
        return state == null ? State.Unknown : state;
    }

    private static PowerState convertStateToPower(State s) {
        final PowerState state = s_powerStateMaps.get(s.toString());
        return state == null ? PowerState.PowerUnknown : state;
    }

    /* State to power in the states from the vmstates that are known */
    protected Map<String, HostVmStateReportEntry> HostVmStateReport()
            throws XmlRpcException {
        final HashMap<String, HostVmStateReportEntry> vmStates = new HashMap<String, HostVmStateReportEntry>();
        for (final Map.Entry<String, State> vm : _vmstates.entrySet()) {
            /* TODO: Figure out how to get xentools version in here */
            s_logger.debug("VM " + vm.getKey() + " state: " + vm.getValue()
                    + ":" + convertStateToPower(vm.getValue()));
            vmStates.put(vm.getKey(), new HostVmStateReportEntry(
                    convertStateToPower(vm.getValue()), c.getIp()));
        }
        return vmStates;
    }

    /* uses the running configuration, not the vm.cfg configuration */
    protected Map<String, Xen.Vm> getAllVms() throws XmlRpcException {
        try {
            Xen vms = new Xen(c);
            return vms.getRunningVmConfigs();
        } catch (Exception e) {
            s_logger.debug("getting VM list from " + _host + " failed", e);
            throw new CloudRuntimeException("Exception on getting VMs from "
                    + _host + ":" + e.getMessage(), e);
        }
    }

    /*
     * Get all the states and set them up according to xm(1) TODO: check
     * migrating ?
     */
    protected HashMap<String, State> getAllVmStates() throws XmlRpcException {
        Map<String, Xen.Vm> vms = getAllVms();
        final HashMap<String, State> states = new HashMap<String, State>();
        for (final Map.Entry<String, Xen.Vm> entry : vms.entrySet()) {
            Xen.Vm vm = entry.getValue();
            if (vm.isControlDomain()) {
                continue;
            }
            State ns = State.Running;
            String as = vm.getVmState();
            /* missing VM is stopped */
            if (as == null) {
                ns = State.Stopped;
                continue;
            }
            /* The domain is currently running on a CPU */
            /* need a more exact match! */
            if (as.contains("r")) {
                ns = State.Running;
                /* The domain is blocked, and not running or runnable. */
            } else if (as.contains("b")) {
                ns = State.Running;
                /* The domain has been paused */
            } else if (as.contains("p")) {
                ns = State.Running;
                /* The guest has requested to be shutdown, still migrating... */
            } else if (as.contains("s")) {
                if (this._vmstates.get(vm.getVmName()) == State.Migrating)
                    ns = State.Migrating;
                ns = State.Stopped;
                /* The domain has crashed */
            } else if (as.contains("c")) {
                ns = State.Error;
                /*
                 * The domain is in process of dying (if we see this twice we
                 * have a problem ?)
                 */
            } else if (as.contains("d")) {
                ns = State.Stopping;
            } else {
                ns = State.Unknown;
            }
            s_logger.debug("state " + ns + " for " + vm.getVmName()
                    + " based on " + as);
            states.put(vm.getVmName(), ns);
        }
        return states;
    }

    /* sync the state we know of with reality */
    protected HashMap<String, State> syncState() {
        HashMap<String, State> newStates;
        HashMap<String, State> oldStates = null;

        try {
            final HashMap<String, State> changes = new HashMap<String, State>();
            newStates = getAllVmStates();
            if (newStates == null) {
                s_logger.debug("Unable to get the vm states so no state sync at this point.");
                return null;
            }

            synchronized (_vmstates) {
                oldStates = new HashMap<String, State>(_vmstates.size());
                oldStates.putAll(_vmstates);

                for (final Map.Entry<String, State> entry : newStates
                        .entrySet()) {
                    final String vmName = entry.getKey();
                    State newState = entry.getValue();
                    final State oldState = oldStates.remove(vmName);
                    s_logger.debug("state for " + vmName + ", old: " + oldState
                            + ", new: " + newState);

                    /* eurh ? */
                    if (newState == State.Stopped && oldState != State.Stopping
                            && oldState != null && oldState != State.Stopped) {
                        s_logger.debug("Getting power state....");
                        newState = State.Running;
                    }

                    if (s_logger.isTraceEnabled()) {
                        s_logger.trace("VM "
                                + vmName
                                + ": ovm has state "
                                + newState
                                + " and we have state "
                                + (oldState != null ? oldState.toString()
                                        : "null"));
                    }

                    /* TODO: is this really true ? should be right ? */
                    if (newState == State.Migrating) {
                        s_logger.debug(vmName
                                + " is migrating, skipping state check");
                        continue;
                    }

                    if (oldState == null) {
                        _vmstates.put(vmName, newState);
                        s_logger.debug("New state without old state: " + vmName);
                        changes.put(vmName, newState);
                    } else if (oldState == State.Starting) {
                        if (newState == State.Running) {
                            _vmstates.put(vmName, newState);
                        } else if (newState == State.Stopped) {
                            s_logger.debug("Ignoring vm " + vmName
                                    + " because of a lag in starting the vm.");
                        }
                    } else if (oldState == State.Migrating) {
                        if (newState == State.Running) {
                            s_logger.debug("Detected that a migrating VM is now running: "
                                    + vmName);
                            _vmstates.put(vmName, newState);
                        }
                    } else if (oldState == State.Stopping) {
                        if (newState == State.Stopped) {
                            _vmstates.put(vmName, newState);
                        } else if (newState == State.Running) {
                            s_logger.debug("Ignoring vm " + vmName
                                    + " because of a lag in stopping the vm. ");
                        }
                    } else if (oldState != newState) {
                        _vmstates.put(vmName, newState);
                        if (newState == State.Stopped) {
                            // TODO: need to state.error here ?
                        }
                        changes.put(vmName, newState);
                    }
                }

                for (final Map.Entry<String, State> entry : oldStates
                        .entrySet()) {
                    final String vmName = entry.getKey();
                    final State oldState = entry.getValue();

                    if (oldState == State.Stopping) {
                        s_logger.debug("Removing VM " + vmName
                                + " in transition state stopping.");
                        _vmstates.remove(vmName);
                    } else if (oldState == State.Starting) {
                        s_logger.debug("Removing VM " + vmName
                                + " in transition state starting.");
                        _vmstates.remove(vmName);
                    } else if (oldState == State.Stopped) {
                        s_logger.debug("Stopped VM " + vmName + " removing.");
                        _vmstates.remove(vmName);
                    } else if (oldState == State.Migrating) {
                        /*
                         * do something smarter here.. newstate should say
                         * stopping already
                         */
                        s_logger.debug("Ignoring VM " + vmName
                                + " in migrating state.");
                    } else {
                        /* if it's not there name it stopping */
                        _vmstates.remove(vmName);
                        State state = State.Stopping;
                        s_logger.debug("VM "
                                + vmName
                                + " is now missing from ovm3 server so removing it");
                        /* TODO: something about killed/halted VM's in here ? */
                        changes.put(vmName, state);
                    }
                }
            }

            return changes;
        } catch (Exception e) {
            s_logger.debug("Ovm3 full sync failed", e);
            return null;
        }
    }

    /* cleanup the storageplugin so we can use an object here */
    protected GetStorageStatsAnswer execute(final GetStorageStatsCommand cmd) {
        // cmd.getStorageId();
        s_logger.debug("Getting stats for: " + cmd.getStorageId());
        try {
            Linux host = new Linux(c);
            /* TODO: NFS only for now */
            Map<String, Linux.FileSystem> fsList = host
                    .getFileSystemList("nfs");
            Linux.FileSystem fs = fsList.get(cmd.getStorageId());
            StoragePlugin store = new StoragePlugin(c);
            /*
             * TODO: something odd here where do I get which Storage I need to
             * look at ?
             */
            String propUuid = store.deDash(cmd.getStorageId());
            String mntUuid = cmd.getStorageId();
            store.setUuid(propUuid);
            store.setSsUuid(propUuid);
            store.setMntUuid(mntUuid);
            store.setFsHost(fs.getHost());
            store.setFsSourcePath(fs.getDevice());
            store.storagePluginGetFileSystemInfo();
            long total = Long.parseLong(store.getTotalSize());
            long used = total - Long.parseLong(store.getFreeSize());
            return new GetStorageStatsAnswer(cmd, total, used);
        } catch (Exception e) {
            s_logger.debug(
                    "GetStorageStatsCommand on pool " + cmd.getStorageId()
                            + " failed", e);
            return new GetStorageStatsAnswer(cmd, e.getMessage());
        }
    }

    private VmStatsEntry getVmStat(String vmName) throws XmlRpcException {
        CloudStackPlugin cSp = new CloudStackPlugin(c);
        Map<String, String> stats = cSp.ovsDomUStats(vmName);
        Map<String, String> o_stats = this._vmStats.get(vmName);
        VmStatsEntry e = new VmStatsEntry();
        if (o_stats == null) {
            e.setNumCPUs(1);
            e.setNetworkReadKBs(0);
            e.setNetworkWriteKBs(0);
            e.setDiskReadKBs(0);
            e.setDiskWriteKBs(0);
            e.setDiskReadIOs(0);
            e.setDiskWriteIOs(0);
            e.setCPUUtilization(0);
            e.setEntityType("vm");
        } else {
            /* beware of negatives ? */
            Integer cpus = Integer.parseInt(stats.get("vcpus"));
            e.setNumCPUs(Integer.parseInt(stats.get(cpus)));
            e.setNetworkReadKBs(Double.parseDouble(stats.get("rx_bytes"))
                    - Double.parseDouble(o_stats.get("rx_bytes")));
            e.setNetworkWriteKBs(Double.parseDouble(stats.get("tx_bytes"))
                    - Double.parseDouble(o_stats.get("tx_bytes")));
            ;
            e.setDiskReadKBs(Double.parseDouble(stats.get("rd_bytes"))
                    - Double.parseDouble(o_stats.get("rd_bytes")));
            ;
            e.setDiskWriteKBs(Double.parseDouble(stats.get("rw_bytes"))
                    - Double.parseDouble(o_stats.get("rw_bytes")));
            e.setDiskReadIOs(Double.parseDouble(stats.get("rd_ops"))
                    - Double.parseDouble(o_stats.get("rd_ops")));
            e.setDiskWriteIOs(Double.parseDouble(stats.get("rw_ops"))
                    - Double.parseDouble(o_stats.get("rw_ops")));

            Double d_cpu = Double.parseDouble(stats.get("cputime"))
                    - Double.parseDouble(o_stats.get("cputime"));
            Double d_time = Double.parseDouble(stats.get("uptime"))
                    - Double.parseDouble(o_stats.get("uptime"));
            Double cpupct = d_cpu / d_time * 100 * cpus;
            e.setCPUUtilization(cpupct);
            e.setEntityType("vm");
        }
        this._vmStats.replace(vmName, stats);
        return e;
    }

    /* TODO: The main caller for vm statstics */
    protected GetVmStatsAnswer execute(GetVmStatsCommand cmd) {
        List<String> vmNames = cmd.getVmNames();
        HashMap<String, VmStatsEntry> vmStatsNameMap = new HashMap<String, VmStatsEntry>();
        for (String vmName : vmNames) {
            try {
                VmStatsEntry e = getVmStat(vmName);
                vmStatsNameMap.put(vmName, e);
            } catch (XmlRpcException e) {
                s_logger.debug("Get vm stat for " + vmName + " failed", e);
                continue;
            }
        }
        return new GetVmStatsAnswer(cmd, vmStatsNameMap);
    }

    /* TODO: Hot plugging harddisks... */
    protected AttachVolumeAnswer execute(AttachVolumeCommand cmd) {
        return new AttachVolumeAnswer(cmd, "You must stop " + cmd.getVmName()
                + " first, OVM doesn't support hotplug datadisk");
    }

    /* Destroy a volume (image) */
    public Answer execute(DestroyCommand cmd) {
        VolumeTO vol = cmd.getVolume();
        String vmName = cmd.getVmName();
        try {
            StoragePlugin store = new StoragePlugin(c);
            // TODO: check if path is correct...
            store.storagePluginDestroy(vol.getPoolUuid(), vol.getPath());
            return new Answer(cmd, true, "Success");
        } catch (Exception e) {
            s_logger.debug("Destroy volume " + vol.getName() + " failed for "
                    + vmName + " ", e);
            return new Answer(cmd, false, e.getMessage());
        }
    }

    /* Migration should make sure both HVs are the same ? */
    protected PrepareForMigrationAnswer execute(PrepareForMigrationCommand cmd) {
        VirtualMachineTO vm = cmd.getVirtualMachine();
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Preparing host for migrating " + vm.getName());
        }
        /* securitygroups */
        try {
            synchronized (_vmstates) {
                _vmstates.put(vm.getName(), State.Migrating);
            }
            return new PrepareForMigrationAnswer(cmd);
        } catch (Exception e) {
            s_logger.warn("Catch Exception " + e.getClass().getName()
                    + " prepare for migration failed due to " + e.toString(), e);
            return new PrepareForMigrationAnswer(cmd, e);
        }
    }

    /* do migrations of VMs in a simple way just inside a cluster for now */
    protected MigrateAnswer execute(final MigrateCommand cmd) {
        final String vmName = cmd.getVmName();
        String destUuid = cmd.getHostGuid();
        String destIp = cmd.getDestinationIp();
        State state = State.Error;
        /*
         * TODO: figure out non pooled migration, works from CLI but not from
         * the agent... perhaps pause the VM and then migrate it ? for now just
         * stop the VM.
         */
        String msg = "Migrating " + vmName + " to " + destIp;
        s_logger.info(msg);
        if (!this._ovm3cluster && !this._ovm3pool) {
            try {
                Xen xen = new Xen(c);
                Xen.Vm vm = xen.getRunningVmConfig(vmName);
                HostVO destHost = _resourceMgr.findHostByGuid(destUuid);
                if (destHost == null) {
                    msg = "Unable to find migration target host in DB "
                            + destUuid + " with ip " + destIp;
                    s_logger.info(msg);
                    return new MigrateAnswer(cmd, false, msg, null);
                }
                xen.stopVm(_ovmObject.deDash(vm.getVmRootDiskPoolId()),
                        vm.getVmUuid());
                msg = destHost.toString();
                state = State.Stopping;
                return new MigrateAnswer(cmd, false, msg, null);
            } catch (Exception e) {
                msg = "Unpooled VM Migrate of " + vmName + " to " + destUuid
                        + " failed due to: " + e.getMessage();
                s_logger.debug(msg, e);
                return new MigrateAnswer(cmd, false, msg, null);
            } finally {
                /* shouldn't we just reinitialize completely as a last resort ? */
                synchronized (_vmstates) {
                    _vmstates.put(vmName, state);
                }
            }
        } else {
            try {
                Xen xen = new Xen(c);
                Xen.Vm vm = xen.getRunningVmConfig(vmName);
                if (vm == null) {
                    state = State.Stopped;
                    msg = vmName + " is no running on " + _host;
                    return new MigrateAnswer(cmd, false, msg, null);
                }
                /* not a storage migration!!! */
                xen.migrateVm(_ovmObject.deDash(vm.getVmRootDiskPoolId()),
                        vm.getVmUuid(), destIp);
                state = State.Stopping;
                msg = "Migration of " + vmName + " successfull";
                return new MigrateAnswer(cmd, true, msg, null);
            } catch (Exception e) {
                msg = "Pooled VM Migrate" + ": Migration of " + vmName + " to "
                        + destIp + " failed due to " + e.getMessage();
                s_logger.debug(msg, e);
                return new MigrateAnswer(cmd, false, msg, null);
            } finally {
                /* if (state != state.Stopped) { */
                synchronized (_vmstates) {
                    _vmstates.put(vmName, state);
                }
            }
        }
    }

    /* hHeck "the" virtual machine */
    protected CheckVirtualMachineAnswer execute(
            final CheckVirtualMachineCommand cmd) {
        String vmName = cmd.getVmName();
        try {
            // if state migrating skip ?
            Xen vms = new Xen(c);
            Xen.Vm vm = vms.getRunningVmConfig(vmName);
            /* check if there is a VM */

            CloudStackPlugin plug = new CloudStackPlugin(c);
            Integer vncPort = Integer.valueOf(plug.getVncPort(vmName));
            if (vncPort == 0) {
                s_logger.warn("No VNC port for " + vmName);
            }
            /* we already have the state ftw */
            Map<String, State> states = getAllVmStates();
            State vmState = states.get(vmName);
            if (vmState == null) {
                s_logger.warn("Check state of " + vmName
                        + " return null in CheckVirtualMachineCommand");
                vmState = State.Stopped;
            }
            synchronized (_vmstates) {
                _vmstates.put(vmName, State.Running);
            }
            return new CheckVirtualMachineAnswer(cmd, vmState, vncPort);
        } catch (Exception e) {
            s_logger.debug("Check migration for " + vmName + " failed", e);
            return new CheckVirtualMachineAnswer(cmd, State.Stopped, null);
        }
    }

    /* TODO: fill this one, for maintenance */
    protected MaintainAnswer execute(MaintainCommand cmd) {
        /* leave cluster, leave pool, release ownership ? */
        try {
            Network net = new Network(c);
            Pool pool = new Pool(c);
            // pool.leaveServerPool();
            net.stopOvsLocalConfig(_controlNetworkName);
        } catch (Exception e) {
            s_logger.debug("unable to disable " + _controlNetworkName);
        }
        return new MaintainAnswer(cmd);
    }

    /*
     */
    protected GetVncPortAnswer execute(GetVncPortCommand cmd) {
        try {
            Xen host = new Xen(c);
            Xen.Vm vm = host.getRunningVmConfig(cmd.getName());
            Integer vncPort = vm.getVncPort();
            s_logger.debug("get vnc port for " + cmd.getName() + ": " + vncPort);
            return new GetVncPortAnswer(cmd, _ip, vncPort);
        } catch (Exception e) {
            s_logger.debug("get vnc port for " + cmd.getName() + " failed", e);
            return new GetVncPortAnswer(cmd, e.getMessage());
        }
    }

    protected Answer execute(PingTestCommand cmd) {
        try {
            if (cmd.getComputingHostIp() != null) {
                CloudStackPlugin cSp = new CloudStackPlugin(c);
                if (!cSp.ping(cmd.getComputingHostIp())) {
                    return new Answer(cmd, false, "ping failed");
                }
            } else {
                return new Answer(cmd, false, "why asks me to ping router???");
            }
            return new Answer(cmd, true, "success");
        } catch (Exception e) {
            s_logger.debug("Ping " + cmd.getComputingHostIp() + " failed", e);
            return new Answer(cmd, false, e.getMessage());
        }
    }

    /*
     * TODO: no heartbeat if no cluster, should we add a heartbeat ?
     * cloudstack/plugins
     * /hypervisors/ovm/scripts/vm/hypervisor/ovm/OvmHostModule.py contains
     * fence
     */
    protected FenceAnswer execute(FenceCommand cmd) {
        try {
            Boolean res = false;
            return new FenceAnswer(cmd, res, res.toString());
        } catch (Exception e) {
            s_logger.debug("fence " + cmd.getHostIp() + " failed", e);
            return new FenceAnswer(cmd, false, e.getMessage());
        }
    }

    /* TODO: nothing yet */
    protected boolean canBridgeFirewall() throws XmlRpcException {
        return false;
    }

    protected boolean prepareForPool() throws ConfigurationException {
        /* need single master uuid */
        try {
            Linux host = new Linux(c);
            Pool pool = new Pool(c);

            /* setup pool and role, needs utility to be able to do things */
            if (host.getServerRoles().contentEquals(
                    pool.getValidRoles().toString())) {
                s_logger.debug("Server role for host " + _host + " is ok");
            } else {
                try {
                    pool.setServerRoles(pool.getValidRoles());
                } catch (Exception e) {
                    s_logger.debug("Failed to set server role for host "
                            + _host, e);
                    throw new ConfigurationException(
                            "Unable to set server role for host");
                }
            }
            if (host.getMembershipState().contentEquals("Unowned")) {
                try {
                    s_logger.debug("Take ownership of host " + _host);
                    pool.takeOwnership(_masterUuid, "");
                } catch (Exception e) {
                    String msg = "Failed to take ownership of host " + _host;
                    s_logger.debug(msg, e);
                    throw new ConfigurationException(msg);
                }
            } else {
                /* TODO: check if it's part of our pool, give ok if it is */
                if (host.getManagerUuid().equals(_masterUuid)) {
                    String msg = "Host " + _host + " owned by us";
                    s_logger.debug(msg);
                    return true;
                } else {
                    String msg = "Host " + _host
                            + " already part of a pool, and not owned by us";
                    s_logger.debug(msg);
                    throw new ConfigurationException(msg);
                }
            }
        } catch (Exception es) {
            String msg = "Failed to prepare " + _host + " for pool";
            s_logger.debug(msg, es);
            throw new ConfigurationException(msg + ": " + es.getMessage());
        }
        return true;
    }

    /*
     * TODO: redo this, it's a mess now.
     */
    protected Boolean setupPool(StorageFilerTO cmd)
            throws ConfigurationException, XmlRpcException, Exception {
        String primUuid = cmd.getUuid();
        String ssUuid = _ovmObject.deDash(primUuid);
        String ovsRepo = this._ovmRepo + "/" + _ovmObject.deDash(primUuid);
        String fsType = "nfs";
        /* TODO: 16, need to get this from the cluster id actually */
        String clusterUuid = _masterUuid.substring(0, 15);
        String managerId = _masterUuid;
        String poolAlias = cmd.getHost() + ":" + cmd.getPath();
        String mountPoint = String.format("%1$s:%2$s", cmd.getHost(),
                cmd.getPath())
                + "/VirtualMachines";
        List<String> members = new ArrayList<String>();
        _isMaster = masterCheck();
        Integer poolSize = 0;
        String msg = "";

        try {
            Pool poolHost = new Pool(c);
            if (_isMaster && !poolHost.getPoolId().contentEquals(primUuid)
                    && !_hasMaster) {
                PoolOCFS2 poolFs = new PoolOCFS2(c);
                try {
                    try {
                        msg = "Create poolfs on " + _host + " for repo "
                                + primUuid;
                        s_logger.debug(msg);
                        poolFs.createPoolFs(fsType, mountPoint, clusterUuid,
                                primUuid, ssUuid, managerId, primUuid);
                    } catch (Exception e) {
                        msg = "Poolfs already exists on " + _host
                                + " for repo " + primUuid;
                        s_logger.debug(msg);
                    }
                    try {
                        poolHost.createServerPool(poolAlias, primUuid,
                                _ovm3vip, poolSize + 1, _host, _ip);
                    } catch (Exception e) {
                        msg = "Server pool with id " + clusterUuid + " on "
                                + _host;
                        s_logger.debug(msg);
                    }
                } catch (Exception e) {
                    msg = "Failed to create pool on " + _host + " for repo "
                            + primUuid;
                    s_logger.debug(msg, e);
                    throw new Exception(msg);
                }
            } else if (!_isMaster || _hasMaster) {
                Pool poolMaster = new Pool(m);
                poolSize = poolMaster.getPoolMemberIpList().size() + 1;
                members.addAll(poolMaster.getPoolMemberIpList());

                poolHost = new Pool(c);
                if (!poolHost.getPoolId().contentEquals(primUuid)) {

                    /* get all the details from the master */
                    try {
                        /* why does it call bloody M here???!?!?!! */
                        poolHost.joinServerPool(poolAlias, primUuid, _ovm3vip,
                                poolSize, _host, _ip);
                    } catch (Exception e) {
                        msg = "Failed to join server pool "
                                + poolMaster.getPoolAlias() + " on " + _host
                                + " for pool " + primUuid;
                        s_logger.debug(msg, e);
                        throw new Exception(msg);
                    }
                }
            } else {
                s_logger.debug("Pool " + primUuid + " already configured on "
                        + _host);
            }
            if (!members.contains(_ip)) {
                members.add(_ip);
            }
            for (String member : members) {
                /* easy way out for now..., should get this from hostVO */
                String url = "http://" + this._agentUserName + ":"
                        + this._agentPassword + "@" + member + ":" + _agentPort
                        + "/api/3";
                /*
                 * Common comCall = new Common(c); comCall.dispatch(url,
                 * "set_pool_member_ip_list", StringUtils.join(members, ","));
                 */
                Connection x = new Connection(member, _agentPort,
                        _agentUserName, _agentPassword);
                Pool xpool = new Pool(x);
                xpool.setPoolIps(members);
                xpool.setPoolMemberIpList();
                msg = "Added " + member + " to pool " + primUuid;
                s_logger.debug(msg);
            }
        } catch (Exception e) {
            msg = "Failed to setup server pool on " + _host;
            s_logger.debug(msg, e);
            throw new Exception(msg);
        }
        return true;
    }

    /* the storage url combination of host and path is unique */
    protected String setupSecondaryStorage(String url) throws Exception {
        URI uri = URI.create(url);
        String uuid = _ovmObject.newUuid(uri.getHost() + ":" + uri.getPath());
        s_logger.info("Secondary storage with uuid: " + uuid);
        return setupNfsStorage(uri, uuid);
    }

    protected String setupNfsStorage(URI uri, String uuid) throws Exception {
        String fsUri = "nfs";
        String fsType = "FileSys";
        String msg = "";
        String mountPoint = this._ovmSec + "/" + uuid;
        Linux host = new Linux(c);
        try {
            Map<String, Linux.FileSystem> fsList = host
                    .getFileSystemList(fsUri);
            Linux.FileSystem fs = fsList.get(uuid);
            if (fs == null || !fs.getMountPoint().equals(mountPoint)) {
                try {
                    StoragePlugin sp = new StoragePlugin(c);
                    sp.storagePluginMount(uri.getHost(), uri.getPath(), uuid,
                            mountPoint);
                    msg = "Nfs storage " + uri + " mounted on " + mountPoint;
                    return uuid;
                } catch (Exception ec) {
                    msg = "Nfs storage " + uri + " mount on " + mountPoint
                            + " FAILED " + ec.getMessage();
                    throw new Exception(msg, ec);
                }
            } else {
                msg = "NFS storage " + uri + " already mounted on "
                        + mountPoint;
                return uuid;
            }
        } finally {
            s_logger.debug(msg);
        }
    }

    /* TODO: move the connection elsewhere.... */
    protected boolean masterCheck() throws ConfigurationException, Exception {
        if (_ovm3vip.equals("")) {
            s_logger.debug("No cluster vip, not checking for master");
            return false;
        }
        try {
            /* should get the details of that host hmmz */
            m = new Connection(_ovm3vip, _agentPort, _agentUserName,
                    _agentPassword);
            Linux master = new Linux(m);
            if (master.getHostName().equals(_host)) {
                s_logger.debug("Host " + _host + " is master");
                this._isMaster = true;
            } else {
                s_logger.debug("Host " + _host + " has master "
                        + master.getHostName());
                _hasMaster = true;
            }
        } catch (Exception e) {
            this._isMaster = true;
            s_logger.debug("Unable to connect to master, we are master");
        } finally {
            if (!this._isMaster) {
                if (this._hasMaster) {
                    s_logger.debug("Host " + _host + " will become a slave "
                            + _ovm3vip);
                    return this._isMaster;
                } else {
                    this._isMaster = true;
                    s_logger.debug("Host " + _host + " will become a master "
                            + _ovm3vip);
                    return this._isMaster;
                }
            }
        }
        return this._isMaster;
    }

    /* check if a VM is running should be added */
    protected CreatePrivateTemplateAnswer execute(
            final CreatePrivateTemplateFromVolumeCommand cmd) {
        String secondaryStorageUrl = cmd.getSecondaryStorageUrl();
        String volumePath = cmd.getVolumePath();
        Long accountId = cmd.getAccountId();
        Long templateId = cmd.getTemplateId();
        int wait = cmd.getWait();
        if (wait == 0) {
            /* Defaut timeout 2 hours */
            wait = 7200;
        }

        try {
            URI uri;
            uri = new URI(secondaryStorageUrl);
            String secondaryStorageMountPath = uri.getHost() + ":"
                    + uri.getPath();
            /* missing uuid */
            String installPath = _ovmRepo + "/Templates/" + accountId + "/"
                    + templateId;
            String templateLoc = installPath;
            Linux host = new Linux(c);
            /* check if VM is running or thrown an error, or pause it :P */
            host.copyFile(volumePath, installPath);
            /* TODO: look at the original */
            return new CreatePrivateTemplateAnswer(cmd, true, installPath);
            /*
             * res.get("installPath"), Long.valueOf(res.get("virtualSize")),
             * Long.valueOf(res.get("physicalSize")),
             * res.get("templateFileName"), ImageFormat.RAW);
             */
        } catch (Exception e) {
            s_logger.debug("Create template failed", e);
            return new CreatePrivateTemplateAnswer(cmd, false, e.getMessage());
        }
    }

    /*
     * Might have some logic errors that's what debugging is for...
     */
    protected CopyVolumeAnswer execute(CopyVolumeCommand cmd) {
        String volumePath = cmd.getVolumePath();
        /* is a repository */
        String secondaryStorageURL = cmd.getSecondaryStorageURL();
        int wait = cmd.getWait();
        if (wait == 0) {
            wait = 7200;
        }

        /* TODO: we need to figure out what sec and prim really is */
        try {
            Linux host = new Linux(c);

            /* to secondary storage */
            if (cmd.toSecondaryStorage()) {
                s_logger.debug("Copy to  secondary storage " + volumePath
                        + " to " + secondaryStorageURL);
                host.copyFile(volumePath, secondaryStorageURL);
                /* from secondary storage */
            } else {
                s_logger.debug("Copy from secondary storage "
                        + secondaryStorageURL + " to " + volumePath);
                host.copyFile(secondaryStorageURL, volumePath);
            }
            /* check the truth of this */
            return new CopyVolumeAnswer(cmd, true, null, null, null);
        } catch (Exception e) {
            s_logger.debug("Copy volume failed", e);
            return new CopyVolumeAnswer(cmd, false, e.getMessage(), null, null);
        }
    }

    /*
     * TODO: this ties into if we're in a cluster or just a pool, leaving a pool
     * means just leaving the pool and getting rid of the pooled fs, different a
     * repo though
     */
    protected Answer execute(DeleteStoragePoolCommand cmd) {
        try {
            Pool pool = new Pool(c);
            pool.leaveServerPool(cmd.getPool().getUuid());
            /* also connect to the master and update the pool list ? */
        } catch (Exception e) {
            s_logger.debug(
                    "Delete storage pool on host "
                            + _host
                            + " failed, however, we leave to user for cleanup and tell managment server it succeeded",
                    e);
        }

        return new Answer(cmd);
    }

    /* TODO: check getPhysicalNetworkInfoList */
    /*
     * need to refactor this bit, networksetupbyname makes no sense, and neither
     * does the physical bit
     */
    protected CheckNetworkAnswer execute(CheckNetworkCommand cmd) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Checking if network name setup is done on " + _host);
        }

        List<PhysicalNetworkSetupInfo> infoList = cmd
                .getPhysicalNetworkInfoList();
        boolean errorout = false;
        String msg = "";
        /* here we assume all networks are set */
        for (PhysicalNetworkSetupInfo info : infoList) {
            if (info.getGuestNetworkName() == null)
                info.setGuestNetworkName(this._guestNetworkName);
            if (info.getPublicNetworkName() == null)
                info.setPublicNetworkName(this._publicNetworkName);
            if (info.getPrivateNetworkName() == null)
                info.setPrivateNetworkName(this._privateNetworkName);
            if (info.getStorageNetworkName() == null)
                info.setStorageNetworkName(this._storageNetworkName);

            if (!isNetworkSetupByName(info.getGuestNetworkName())) {
                msg = "For Physical Network id:"
                        + info.getPhysicalNetworkId()
                        + ", Guest Network is not configured on the backend by name "
                        + info.getGuestNetworkName();
                errorout = true;
                break;
            }
            if (!isNetworkSetupByName(info.getPrivateNetworkName())) {
                msg = "For Physical Network id:"
                        + info.getPhysicalNetworkId()
                        + ", Private Network is not configured on the backend by name "
                        + info.getPrivateNetworkName();
                errorout = true;
                break;
            }
            if (!isNetworkSetupByName(info.getPublicNetworkName())) {
                msg = "For Physical Network id:"
                        + info.getPhysicalNetworkId()
                        + ", Public Network is not configured on the backend by name "
                        + info.getPublicNetworkName();
                errorout = true;
                break;
            }
            /* Storage network is optional, will revert to private otherwise */
        }

        if (errorout) {
            s_logger.error(msg);
            return new CheckNetworkAnswer(cmd, false, msg);
        } else {
            return new CheckNetworkAnswer(cmd, true,
                    "Network Setup check by names is done");
        }
    }

    private boolean isNetworkSetupByName(String nameTag) {
        s_logger.debug("known networks: " + this._guestNetworkName + " "
                + this._publicNetworkName + " " + this._privateNetworkName);
        if (nameTag != null) {
            s_logger.debug("Looking for network setup by name " + nameTag);

            try {
                Network net = new Network(c);
                net.setBridgeList(_interfaces);
                if (net.getBridgeByName(nameTag) != null) {
                    s_logger.debug("Found bridge with name: " + nameTag);
                    return true;
                }
            } catch (Exception e) {
                s_logger.debug("Unxpected error looking for name: " + nameTag);
                return false;
            }
        }
        s_logger.debug("No bridge with name: " + nameTag);
        return false;
    }

    @Override
    public Answer executeRequest(Command cmd) {
        try {
            if (cmd instanceof NetworkElementCommand) {
                return _virtRouterResource
                        .executeRequest((NetworkElementCommand) cmd);
            }
        } catch (final IllegalArgumentException e) {
            return new Answer(cmd, false, e.getMessage());
        }
        Class<? extends Command> clazz = cmd.getClass();
        if (clazz == ReadyCommand.class) {
            return execute((ReadyCommand) cmd);
        } else if (clazz == CopyCommand.class) {
            return execute((CopyCommand) cmd);
        } else if (clazz == DeleteCommand.class) {
            return execute((DeleteCommand) cmd);
        } else if (clazz == CreateStoragePoolCommand.class) {
            return execute((CreateStoragePoolCommand) cmd);
        } else if (clazz == ModifyStoragePoolCommand.class) {
            return execute((ModifyStoragePoolCommand) cmd);
        } else if (clazz == PrimaryStorageDownloadCommand.class) {
            return execute((PrimaryStorageDownloadCommand) cmd);
        } else if (clazz == CreateCommand.class) {
            return execute((CreateCommand) cmd);
        } else if (clazz == GetHostStatsCommand.class) {
            return execute((GetHostStatsCommand) cmd);
        } else if (clazz == StopCommand.class) {
            return execute((StopCommand) cmd);
        } else if (clazz == RebootCommand.class) {
            return execute((RebootCommand) cmd);
        } else if (clazz == GetStorageStatsCommand.class) {
            return execute((GetStorageStatsCommand) cmd);
        } else if (clazz == GetVmStatsCommand.class) {
            return execute((GetVmStatsCommand) cmd);
        } else if (clazz == AttachVolumeCommand.class) {
            return execute((AttachVolumeCommand) cmd);
        } else if (clazz == DestroyCommand.class) {
            return execute((DestroyCommand) cmd);
        } else if (clazz == PrepareForMigrationCommand.class) {
            return execute((PrepareForMigrationCommand) cmd);
        } else if (clazz == MigrateCommand.class) {
            return execute((MigrateCommand) cmd);
        } else if (clazz == CheckVirtualMachineCommand.class) {
            return execute((CheckVirtualMachineCommand) cmd);
        } else if (clazz == MaintainCommand.class) {
            return execute((MaintainCommand) cmd);
        } else if (clazz == StartCommand.class) {
            return execute((StartCommand) cmd);
        } else if (clazz == GetVncPortCommand.class) {
            return execute((GetVncPortCommand) cmd);
        } else if (clazz == PingTestCommand.class) {
            return execute((PingTestCommand) cmd);
        } else if (clazz == FenceCommand.class) {
            return execute((FenceCommand) cmd);
        } else if (clazz == AttachIsoCommand.class) {
            return execute((AttachIsoCommand) cmd);
        } else if (clazz == DettachCommand.class) {
            return execute((DettachCommand) cmd);
        } else if (clazz == AttachCommand.class) {
            return execute((AttachCommand) cmd);
        } else if (clazz == NetworkRulesSystemVmCommand.class) {
            return execute((NetworkRulesSystemVmCommand) cmd);
            /*
             * } else if (clazz == SecurityGroupRulesCmd.class) { return
             * execute((SecurityGroupRulesCmd) cmd); } else if (clazz ==
             * CleanupNetworkRulesCmd.class) { return
             * execute((CleanupNetworkRulesCmd) cmd); } else if (clazz ==
             * PrepareOCFS2NodesCommand.class) { return
             * execute((PrepareOCFS2NodesCommand) cmd);
             */
        } else if (clazz == CreatePrivateTemplateFromVolumeCommand.class) {
            return execute((CreatePrivateTemplateFromVolumeCommand) cmd);
        } else if (clazz == CopyVolumeCommand.class) {
            return execute((CopyVolumeCommand) cmd);
        } else if (clazz == DeleteStoragePoolCommand.class) {
            return execute((DeleteStoragePoolCommand) cmd);
        } else if (clazz == CheckNetworkCommand.class) {
            return execute((CheckNetworkCommand) cmd);
        } else if (clazz == CheckSshCommand.class) {
            return execute((CheckSshCommand) cmd);
        } else if (clazz == GetDomRVersionCmd.class) {
            return execute((GetDomRVersionCmd) cmd);
        } else if (clazz == DhcpEntryCommand.class) {
            return execute((DhcpEntryCommand) cmd);
        } else if (clazz == SavePasswordCommand.class) {
            return execute((SavePasswordCommand) cmd);
        } else if (clazz == VmDataCommand.class) {
            return execute((VmDataCommand) cmd);
        } else if (clazz == CreateObjectCommand.class) {
            return execute((CreateObjectCommand) cmd);
        } else {
            return Answer.createUnsupportedCommandAnswer(cmd);
        }
    }

    @Override
    public void disconnected() {
        // TODO Auto-generated method stub

    }

    @Override
    public IAgentControl getAgentControl() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setAgentControl(IAgentControl agentControl) {
        // TODO Auto-generated method stub

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

    /* TODO: move to the StoragPprocessor */
    @Override
    public Answer copyTemplateToPrimaryStorage(CopyCommand cmd) {
        return new Answer(cmd);
    }

    @Override
    public Answer copyVolumeFromPrimaryToSecondary(CopyCommand cmd) {
        return new Answer(cmd);
    }

    @Override
    public Answer cloneVolumeFromBaseTemplate(CopyCommand cmd) {
        return new Answer(cmd);
    }

    @Override
    public Answer createTemplateFromVolume(CopyCommand cmd) {
        return new Answer(cmd);
    }

    @Override
    public Answer copyVolumeFromImageCacheToPrimary(CopyCommand cmd) {
        return new Answer(cmd);
    }

    @Override
    public Answer createTemplateFromSnapshot(CopyCommand cmd) {
        return null; // To change body of implemented methods use File |
                     // Settings | File Templates.
    }

    @Override
    public Answer backupSnapshot(CopyCommand cmd) {
        return new Answer(cmd);
    }

    /*
     * TODO: Add iso and update config of the VM is done from set_disk in
     * lib/xenxm.py (Example found in Xen.java) Iso first needs to be copied
     * over to the primary storage where the ISOs reside, or just mount
     * secondary storage, feh!
     */
    protected Answer execute(AttachIsoCommand cmd) {
        try {
            URI iso = new URI(cmd.getIsoPath());
            String isoPath = iso.getHost() + ":" + iso.getPath();
            /*
             * Ovm3Vm.detachOrAttachIso(_conn, cmd.getVmName(), isoPath,
             * cmd.isAttach());
             */
            // setupSecStorage()
            s_logger.debug(cmd.getStoreUrl() + " " + cmd.getIsoPath());
            return new Answer(cmd);
        } catch (Exception e) {
            s_logger.debug(
                    "Attach or detach ISO " + cmd.getIsoPath() + " for "
                            + cmd.getVmName() + " attach:" + cmd.isAttach()
                            + " failed", e);
            return new Answer(cmd, false, e.getMessage());
        }
    }

    @Override
    public Answer attachIso(AttachCommand cmd) {
        s_logger.debug("attachIso not implemented yet");
        return new Answer(cmd);
    }

    @Override
    public Answer dettachIso(DettachCommand cmd) {
        s_logger.debug("detachIso not implemented yet");
        return new Answer(cmd);
    }

    public String isoAttachDetach(String vmName, DiskTO disk, boolean isAttach) {
        Xen xen = new Xen(c);
        String doThis = (isAttach) ? "AttachIso" : "DettachIso";
        String msg = null;
        try {
            Xen.Vm vm = xen.getVmConfig(vmName);
            /* check running */
            if (vm == null) {
                msg = doThis + " can't find VM " + vmName;
                return msg;
            }
            TemplateObjectTO isoTO = (TemplateObjectTO) disk.getData();
            DataStoreTO store = isoTO.getDataStore();
            if (!(store instanceof NfsTO)) {
                msg = doThis + " unsupported protocol";
                return msg;
            }
            NfsTO nfsStore = (NfsTO) store;
            String secPoolUuid = setupSecondaryStorage(nfsStore.getUrl());
            String isoPath = _ovmSec + File.separator + secPoolUuid
                    + File.separator + isoTO.getPath();
            s_logger.debug(doThis + " " + isoPath);
            if (isAttach) {
                /* check if iso is not already there ? */
                vm.addIso(isoPath);
            } else {
                if (!vm.removeDisk(isoPath)) {
                    msg = doThis + " failed for " + vmName
                            + " iso was not attached " + isoPath;
                    return msg;
                }
            }
            xen.configureVm(_ovmObject.deDash(vm.getPrimaryPoolUuid()),
                    vm.getVmUuid());
            return msg;
        } catch (Exception e) {
            msg = doThis + " failed for " + vmName + " " + e.getMessage();
            return msg;
        } finally {
            if (msg == null) {
                msg = "success";
            }
            s_logger.debug(msg);
        }
    }

    public Answer execute(AttachCommand cmd) {
        String vmName = cmd.getVmName();
        DiskTO disk = cmd.getDisk();
        Boolean success = false;
        String answer = "";
        if (cmd.getDisk().getType() == Volume.Type.ISO) {
            answer = isoAttachDetach(vmName, disk, true);
        }

        if (answer == null) {
            success = true;
        }
        return new Answer(cmd, success, answer);
    }

    public Answer execute(DettachCommand cmd) {
        String vmName = cmd.getVmName();
        DiskTO disk = cmd.getDisk();
        Boolean success = false;
        String answer = "";
        if (cmd.getDisk().getType() == Volume.Type.ISO) {
            answer = isoAttachDetach(vmName, disk, false);
        }

        if (answer == null) {
            success = true;
        }
        return new Answer(cmd, success, answer);
        /*
         * s_logger.debug("dettaching object: " + cmd.getDisk().getType()); if
         * (this._vmstates.get(vmName) == State.Running) { return new
         * DettachAnswer("Dettach unsupported on running VM " + vmName);
         */
    }

    /*
     * for now we assume that the datastore is there or else we never came this
     * far
     */
    public Answer execute(CreateObjectCommand cmd) {
        // public Answer execute(CreateObjectCommand cmd) {
        DataTO data = cmd.getData();
        s_logger.debug("creating new object: " + data.getObjectType());
        if (data.getObjectType() == DataObjectType.VOLUME) {
            return createVolume(cmd);
        } else if (data.getObjectType() == DataObjectType.SNAPSHOT) {
            /*
             * if stopped yes, if runniing ... no, unless we have ocfs2 when
             * using raw partitions (file:) if using tap:aio we cloud...
             */
            /* if (_vmstates(get)) { */
        } else if (data.getObjectType() == DataObjectType.TEMPLATE) {
            /* aaalwaaays */
        } else {

        }
        return new CreateObjectAnswer("Create unsupported object type: "
                + data.getObjectType());
    }

    @Override
    public Answer createVolume(CreateObjectCommand cmd) {
        DataTO data = cmd.getData();
        VolumeObjectTO volume = (VolumeObjectTO) data;
        try {
            /*
             * public Boolean storagePluginCreate(String uuid, String ssuuid,
             * String host, String file, Integer size)
             */
            String poolUuid = data.getDataStore().getUuid();
            String storeUrl = data.getDataStore().getUrl();
            URI uri = new URI(storeUrl);
            String host = uri.getHost();
            String file = this._ovmRepo + "/" + _ovmObject.deDash(poolUuid)
                    + "/VirtualDisks/" + volume.getUuid() + ".raw";
            Long size = volume.getSize();
            StoragePlugin sp = new StoragePlugin(c);
            sp.storagePluginCreate(poolUuid, host, file, size);
            sp.storagePluginGetFileInfo(file);
            VolumeObjectTO newVol = new VolumeObjectTO();
            newVol.setName(volume.getName());
            newVol.setSize(sp.getFileSize());
            newVol.setPath(file);
            return new CreateObjectAnswer(newVol);
        } catch (Exception e) {
            s_logger.debug("create volume failed: " + e.toString());
            return new CreateObjectAnswer(e.toString());
        }
    }

    /* volumes are created on primary storage, ehm they should be ... */
    @Override
    public Answer attachVolume(AttachCommand cmd) {
        return new Answer(cmd);
    }

    /* stub */
    @Override
    public Answer dettachVolume(DettachCommand cmd) {
        return new Answer(cmd);
    }

    /* is it supported? */
    @Override
    public Answer createSnapshot(CreateObjectCommand cmd) {
        /* createsnap, but should be implemented */
        return new Answer(cmd);
    }

    @Override
    public Answer deleteVolume(DeleteCommand cmd) {
        /* storagePluginDestroy(String ssuuid, String file) */
        DataTO data = cmd.getData();
        VolumeObjectTO volume = (VolumeObjectTO) data;
        try {
            String poolUuid = data.getDataStore().getUuid();
            /* needs the file attached too please... */
            String file = volume.getPath();
            if (file.endsWith("/")) {
                file = volume.getPath() + "/" + volume.getUuid() + ".raw";
            }
            StoragePlugin sp = new StoragePlugin(c);
            sp.storagePluginDestroy(poolUuid, file);
            s_logger.debug("delete volume success: " + file);
        } catch (Exception e) {
            s_logger.debug("delete volume failed: " + e.toString());
            return new CreateObjectAnswer(e.toString());
        }
        return new Answer(cmd);
    }

    /* copies it from secondary to primary ? */
    @Override
    public Answer createVolumeFromSnapshot(CopyCommand cmd) {
        return new Answer(cmd);
    }

    @Override
    public Answer deleteSnapshot(DeleteCommand cmd) {
        /* storagePluginDestroy(String ssuuid, String file) */
        return new Answer(cmd);
    }

    @Override
    public Answer introduceObject(IntroduceObjectCmd cmd) {
        return new Answer(cmd, false, "not implememented yet");
    }

    @Override
    public Answer forgetObject(ForgetObjectCmd cmd) {
        return new Answer(cmd, false, "not implememented yet");
    }

}
