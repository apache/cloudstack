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

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.storage.command.AttachCommand;
import org.apache.cloudstack.storage.command.CopyCmdAnswer;
import org.apache.cloudstack.storage.command.CopyCommand;
import org.apache.cloudstack.storage.command.CreateObjectAnswer;
import org.apache.cloudstack.storage.command.CreateObjectCommand;
import org.apache.cloudstack.storage.command.DeleteCommand;
import org.apache.cloudstack.storage.command.DettachCommand;
import org.apache.cloudstack.storage.command.ForgetObjectCmd;
import org.apache.cloudstack.storage.command.IntroduceObjectCmd;
import org.apache.cloudstack.storage.to.TemplateObjectTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;

import com.cloud.agent.IAgentControl;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.AttachIsoCommand;
import com.cloud.agent.api.AttachVolumeAnswer;
import com.cloud.agent.api.AttachVolumeCommand;
import com.cloud.agent.api.CheckHealthAnswer;
import com.cloud.agent.api.CheckHealthCommand;
import com.cloud.agent.api.CheckNetworkAnswer;
import com.cloud.agent.api.CheckNetworkCommand;
import com.cloud.agent.api.CheckVirtualMachineAnswer;
import com.cloud.agent.api.CheckVirtualMachineCommand;
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
import com.cloud.agent.api.HostStatsEntry;
import com.cloud.agent.api.HostVmStateReportEntry;
import com.cloud.agent.api.MaintainAnswer;
import com.cloud.agent.api.MaintainCommand;
import com.cloud.agent.api.MigrateAnswer;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.agent.api.ModifyStoragePoolAnswer;
import com.cloud.agent.api.ModifyStoragePoolCommand;
import com.cloud.agent.api.NetworkRulesSystemVmCommand;
import com.cloud.agent.api.NetworkUsageAnswer;
import com.cloud.agent.api.NetworkUsageCommand;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.PingRoutingCommand;
import com.cloud.agent.api.PingTestCommand;
import com.cloud.agent.api.PrepareForMigrationAnswer;
import com.cloud.agent.api.PrepareForMigrationCommand;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.RebootAnswer;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.SetupGuestNetworkCommand;
import com.cloud.agent.api.StartAnswer;
import com.cloud.agent.api.StartCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.agent.api.StartupStorageCommand;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.VmStatsEntry;
import com.cloud.agent.api.check.CheckSshAnswer;
import com.cloud.agent.api.check.CheckSshCommand;
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
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.agent.resource.virtualnetwork.VirtualRouterDeployer;
import com.cloud.agent.resource.virtualnetwork.VirtualRoutingResource;
import com.cloud.host.Host.Type;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.ovm3.object.CloudStackPlugin;
import com.cloud.hypervisor.ovm3.object.Common;
import com.cloud.hypervisor.ovm3.object.Connection;
import com.cloud.hypervisor.ovm3.object.Linux;
import com.cloud.hypervisor.ovm3.object.Network;
import com.cloud.hypervisor.ovm3.object.Ovm3ResourceException;
import com.cloud.hypervisor.ovm3.object.OvmObject;
import com.cloud.hypervisor.ovm3.object.Pool;
import com.cloud.hypervisor.ovm3.object.PoolOCFS2;
import com.cloud.hypervisor.ovm3.object.Repository;
import com.cloud.hypervisor.ovm3.object.StoragePlugin;
import com.cloud.hypervisor.ovm3.object.Xen;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetworkSetupInfo;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ServerResource;
import com.cloud.resource.ServerResourceBase;
import com.cloud.resource.hypervisor.HypervisorResource;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.Volume;
import com.cloud.storage.resource.StorageProcessor;
import com.cloud.storage.template.TemplateProp;
import com.cloud.template.VirtualMachineTemplate.BootloaderType;
import com.cloud.utils.ExecutionResult;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.script.Script;
import com.cloud.utils.ssh.SSHCmdHelper;
import com.cloud.utils.ssh.SshHelper;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.PowerState;
import com.cloud.vm.VirtualMachine.State;
import com.trilead.ssh2.SCPClient;

/* TODO: Seperate the Resources out */
@Local(value = ServerResource.class)
public class Ovm3ResourceBase extends ServerResourceBase implements
        ServerResource, HypervisorResource, VirtualRouterDeployer,
        StorageProcessor {
    private static final Logger LOGGER = Logger
            .getLogger(Ovm3ResourceBase.class);
    private Connection c;
    private Connection m;
    private String agentName;
    private String agentIp;
    Long agentZoneId;
    Long agentPodId;
    Long agentPoolId;
    Long agentClusterId;
    String agentHostname;
    String csGuid;
    String agentSshUserName = "root";
    String agentSshPassword;
    String agentOvsAgentUser = "oracle";
    String agentOvsAgentPassword;
    Integer agentOvsAgentPort = 8899;
    Boolean agentOvsAgentSsl = false;
    String agentSshKey = "id_rsa.cloud";
    String agentOwnedByUuid = "d1a749d4295041fb99854f52ea4dea97";
    Boolean agentIsMaster = false;
    Boolean agentHasMaster = false;
    Boolean agentInOvm3Pool = false;
    Boolean agentInOvm3Cluster = false;
    String ovm3PoolVip = "";
    String agentPrivateNetworkName;
    String agentPublicNetworkName;
    String agentGuestNetworkName;
    String agentStorageNetworkName;
    String agentControlNetworkName = "control0";
    String agentOvmRepoPath = "/OVS/Repositories";
    String agentSecStoragePath = "/nfsmnt";
    OvmObject ovmObject = new OvmObject();
    int domRSshPort = 3922;
    String domRCloudPath = "/opt/cloud/bin/";
    private static final int VRTIMEOUT = 600;
    protected VirtualRoutingResource vrResource;
    private Map<String, Network.Interface> agentInterfaces = null;
    private final Map<String, Map<String, String>> vmStats = new ConcurrentHashMap<String, Map<String, String>>();

    /* TODO: Add a network map, so we know which tagged interfaces we can remove and switch to ConcurrentHashMap */
    protected Map<String, Xen.Vm> vmMap = new HashMap<String, Xen.Vm>();
    protected Map<String, State> vmStateMap = new HashMap<String, State>();

    static Map<String, State> s_stateMaps;
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

    static Map<String, PowerState> s_powerStateMaps;
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
    ResourceManager resourceMgr;

    GlobalLock exclusiveOpLock = GlobalLock.getInternLock("ovm3.exclusive.op");

    /* return params we want to add, damnit */
    @Override
    public boolean configure(String name, Map<String, Object> params)
            throws ConfigurationException {
        agentName = name;
        LOGGER.debug("configure " + name + " with params: " + params);
        agentZoneId = Long.parseLong((String) params.get("zone"));
        agentPodId = Long.parseLong((String) params.get("pod"));
        agentClusterId = Long.parseLong((String) params.get("cluster"));
        ovm3PoolVip = String.valueOf(params.get("ovm3vip"));
        agentInOvm3Pool = BooleanUtils.toBoolean((String) params
                .get("ovm3pool"));
        agentInOvm3Cluster = BooleanUtils.toBoolean((String) params
                .get("ovm3cluster"));
        agentHostname = (String) params.get("host");
        agentIp = (String) params.get("ip");
        agentSshUserName = (String) params.get("username");
        agentSshPassword = (String) params.get("password");
        csGuid = (String) params.get("guid");
        agentOvsAgentUser = (String) params.get("agentusername");
        agentOvsAgentPassword = (String) params.get("agentpassword");
        agentPrivateNetworkName = (String) params.get("private.network.device");
        agentPublicNetworkName = (String) params.get("public.network.device");
        agentGuestNetworkName = (String) params.get("guest.network.device");
        agentStorageNetworkName = (String) params.get("storage.network.device1");

        if (params.get("agentport") != null) {
            agentOvsAgentPort = Integer.parseInt((String) params
                    .get("agentport"));
        }
        /* TODO: the agentssl parameter */
        if (agentPodId == null) {
            String msg = "Unable to get the pod";
            LOGGER.debug(msg);
            throw new ConfigurationException(msg);
        }

        if (agentHostname == null) {
            String msg = "Unable to get the host";
            LOGGER.debug(msg);
            throw new ConfigurationException(msg);
        }

        if (agentSshUserName == null) {
            String msg = "Unable to get the username";
            LOGGER.debug(msg);
            throw new ConfigurationException(msg);
        }

        if (agentSshPassword == null) {
            String msg = "Unable to get the password";
            LOGGER.debug(msg);
            throw new ConfigurationException(msg);
        }

        if (csGuid == null) {
            String msg = "Unable to get the guid";
            LOGGER.debug(msg);
            throw new ConfigurationException(msg);
        }

        if (agentOvsAgentUser == null) {
            String msg = "Unable to get the agent username";
            LOGGER.debug(msg);
            throw new ConfigurationException(msg);
        }

        if (agentOvsAgentPassword == null) {
            String msg = "Unable to get the agent password";
            LOGGER.debug(msg);
            throw new ConfigurationException(msg);
        }

        if (agentOvsAgentPort == null) {
            String msg = "Unable to get the agent port";
            LOGGER.debug(msg);
            throw new ConfigurationException(msg);
        }

        if (!NetUtils.isValidIp(ovm3PoolVip)) {
            LOGGER.debug("No VIP, Setting ovm3pool and ovm3cluster to false");
            this.agentInOvm3Pool = false;
            this.agentInOvm3Cluster = false;
            this.ovm3PoolVip = "";
        }
        /* if we're a cluster we are a pool */
        if (agentInOvm3Cluster) {
            this.agentInOvm3Pool = true;
        }

        /* check if we're master or not and if we can connect */
        try {
            c = new Connection(agentHostname, agentOvsAgentPort,
                    agentOvsAgentUser, agentOvsAgentPassword);
            masterCheck();
        } catch (Exception e) {
            String msg = "Base checks failed for " + agentHostname;
            LOGGER.debug(msg, e);
            throw new ConfigurationException(msg);
        }
        /* setup ovm3 agent plugin for cloudstack, our minion */
        try {
            installOvsPlugin();
            CloudStackPlugin cSp = new CloudStackPlugin(c);
            cSp.ovsUploadSshKey(this.agentSshKey,
                    FileUtils.readFileToString(getSystemVMKeyFile()));
        } catch (Ovm3ResourceException  | IOException e) {
            String msg = "Failed to setup server: " + agentHostname + " " +
                    e.getMessage();
            throw new CloudRuntimeException(msg, e);
        }

        try {
            /*
             * TODO: setup meta tags for the management interface (probably
             * required with multiple interfaces)?
             */
            Network net = new Network(c);
            agentInterfaces = net.getInterfaceList();
            LOGGER.debug("all interfaces: " + agentInterfaces);
            if (agentControlNetworkName != null
                    && !agentInterfaces.containsKey(agentControlNetworkName)) {
                net.startOvsLocalConfig(agentControlNetworkName);
                /* ovs replies too "fast" so the bridge can be "busy" */
                int contCount = 0;
                while (!agentInterfaces.containsKey(agentControlNetworkName) && contCount < 10) {
                    LOGGER.debug("waiting for " + agentControlNetworkName);
                    agentInterfaces = net.getInterfaceList();
                    Thread.sleep(1 * 1000);
                    contCount++;
                }
                if (!agentInterfaces.containsKey(agentControlNetworkName)) {
                    throw new ConfigurationException(
                            "Unable to configure "
                                    + agentControlNetworkName
                                    + " on host "
                                    + agentHostname);
                }
            }
            /*
             * The bridge is remembered upon reboot, but not the IP or the
             * config. Zeroconf also adds the route again by default.
             */
            net.ovsIpConfig(agentControlNetworkName, "static",
                    NetUtils.getLinkLocalGateway(), NetUtils.getLinkLocalNetMask());
            CloudStackPlugin cSp = new CloudStackPlugin(c);
            cSp.ovsControlInterface(agentControlNetworkName, NetUtils.getLinkLocalCIDR(),
                    NetUtils.getLinkLocalNetMask());

            LOGGER.debug(net.getInterfaceList());
            /* build ovs_if_meta in Net based on the following */
            if (net.getBridgeByName(agentPrivateNetworkName) == null) {
                throw new ConfigurationException(
                        "Cannot find private bridge "
                                + agentPrivateNetworkName
                                + " on host "
                                + agentHostname);
            }
            if (net.getBridgeByName(agentPublicNetworkName) == null) {
                throw new ConfigurationException(
                        "Cannot find private bridge "
                                + agentPublicNetworkName
                                + " on host "
                                + agentHostname);
            }
            if (net.getBridgeByName(agentGuestNetworkName) == null) {
                throw new ConfigurationException(
                        "Cannot find private bridge "
                                + agentGuestNetworkName
                                + " on host "
                                + agentHostname);
            }
            if (net.getBridgeByName(agentStorageNetworkName) == null) {
                throw new ConfigurationException(
                        "Cannot find private bridge "
                                + agentStorageNetworkName
                                + " on host "
                                + agentHostname);
            }
        } catch (InterruptedException e) {
            LOGGER.error("interrupted?", e);
        } catch (Ovm3ResourceException  e) {
            String msg = "Bridge/Network configuration failed on " + agentHostname;
            LOGGER.error(msg, e);
            throw new ConfigurationException(msg + ", " + e.getMessage());
        }
        prepareForPool();

        vrResource = new VirtualRoutingResource(this);
        if (!vrResource.configure(name, params)) {
            throw new ConfigurationException(
                    "Unable to configure VirtualRoutingResource");
        }
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
        return agentName;
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
            /* we might need dom0 vm stats here at some point */
            cmd.setName(host.getHostName());
            cmd.setSpeed(host.getCpuKhz());
            cmd.setCpus(host.getTotalThreads());
            cmd.setCpuSockets(host.getCpuSockets());
            cmd.setMemory(host.getMemory().longValue());
            BigInteger totalmem = BigInteger.valueOf(host.getMemory()
                    .longValue());
            BigInteger freemem = BigInteger.valueOf(host.getFreeMemory()
                    .longValue());
            cmd.setDom0MinMemory(totalmem.subtract(freemem).longValue());
            // setPoolSync and setCaps.
            cmd.setGuid(csGuid);
            cmd.setDataCenter(agentZoneId.toString());
            cmd.setPod(agentPodId.toString());
            /* TODO: cmd.setOwner(host.getManagerUuid()); */
            cmd.setCluster(agentClusterId.toString());
            cmd.setHypervisorVersion(host.getOvmVersion());
            cmd.setVersion(host.getAgentVersion());
            cmd.setHypervisorType(HypervisorType.Ovm3);
            /* is this true ? */
            cmd.setCaps(host.getCapabilities());
            // TODO: Control ip, for now cheat ?
            cmd.setPrivateIpAddress(agentIp);
            cmd.setStorageIpAddress(agentIp);
            cmd.setHostVmStateReport(hostVmStateReport());

            Network net = new Network(c);
            /* more detail -- fix -- and shouldn't matter */
            /* should this be the bond before the bridge ? */
            String defaultBridge = net.getBridgeByIp(agentIp).getName();
            if (defaultBridge == null) {
                throw new CloudRuntimeException(
                        "Unable to obtain valid bridge with " + agentIp);
            }

            if (agentPublicNetworkName == null) {
                agentPublicNetworkName = defaultBridge;
            }
            if (agentPrivateNetworkName == null) {
                agentPrivateNetworkName = agentPublicNetworkName;
            }
            if (agentGuestNetworkName == null) {
                agentGuestNetworkName = agentPublicNetworkName;
            }
            if (agentStorageNetworkName == null) {
                agentStorageNetworkName = agentPrivateNetworkName;
            }
            Map<String, String> d = cmd.getHostDetails();
            d.put("public.network.device", agentPublicNetworkName);
            d.put("private.network.device", agentPrivateNetworkName);
            d.put("guest.network.device", agentGuestNetworkName);
            d.put("storage.network.device", agentStorageNetworkName);
            d.put("ismaster", this.agentIsMaster.toString());
            d.put("hasmaster", this.agentHasMaster.toString());
            cmd.setHostDetails(d);
            LOGGER.debug("Add an Ovm3 host " + agentName + ":"
                    + cmd.getHostDetails());
        } catch (Ovm3ResourceException e) {
            throw new CloudRuntimeException("Ovm3ResourceException: "
                    + e.getMessage(), e);
        }
    }

    /*
     * plugs the ovm module into the ovs-agent
     */
    protected void installOvsPlugin() throws IOException {
        /* ssh-copy-id anyone ? */
        /* version dependent patching ? */
        try {
            com.trilead.ssh2.Connection sshConnection = SSHCmdHelper
                    .acquireAuthorizedConnection(agentIp, agentSshUserName,
                            agentSshPassword);
            if (sshConnection == null) {
                throw new ConfigurationException(String.format("Unable to "
                        + "connect to server(IP=%1$s, username=%2$s, "
                        + "password=%3$s", agentIp, agentSshUserName,
                        agentSshPassword));
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
                    + this.agentOvsAgentSsl + " " + "--port="
                    + this.agentOvsAgentPort);
            if (!SSHCmdHelper.sshExecuteCmd(sshConnection, prepareCmd)) {
                throw new ConfigurationException("Module insertion at "
                        + agentHostname + " failed");
            }
        } catch (Exception es) {
            LOGGER.error("Unexpected exception ", es);
            String msg = "Unable to install module in agent";
            throw new CloudRuntimeException(msg);
        }
    }

    /* startup */
    @Override
    public StartupCommand[] initialize() {
        LOGGER.debug("Ovm3 resource intializing");
        try {
            StartupRoutingCommand srCmd = new StartupRoutingCommand();

            StartupStorageCommand ssCmd = new StartupStorageCommand();
            fillHostInfo(srCmd);
            LOGGER.debug("Ovm3 pool " + ssCmd + " " + srCmd);

            Map<String, State> changes = null;
            synchronized (vmStateMap) {
                vmStateMap.clear();
                changes = syncState();
            }
            srCmd.setStateChanges(changes);
            return new StartupCommand[] { srCmd, ssCmd };
        } catch (Exception e) {
            LOGGER.debug("Ovm3 resource initializes failed", e);
            return new StartupCommand[] {};
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
                Map <String, State> newStates = syncState();
                return new PingRoutingCommand(getType(), id, newStates,
                        hostVmStateReport());
            } else {
                LOGGER.debug("Agent did not respond correctly: " + ping
                        + " but got " + pong);
            }
        } catch (Ovm3ResourceException e) {
            LOGGER.debug("Check agent status failed", e);
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
            if (!host.getIsMaster() && agentInOvm3Cluster) {
                if (pool.getPoolMasterVip().equalsIgnoreCase(agentIp)) {
                    /* check pool state here */
                    return new ReadyAnswer(cmd);
                } else {
                    LOGGER.debug("Master IP changes to "
                            + pool.getPoolMasterVip() + ", it should be "
                            + agentIp);
                    return new ReadyAnswer(cmd, "I am not the master server");
                }
            } else if (host.getIsMaster()) {
                LOGGER.debug("Master, not clustered " + agentHostname);
                return new ReadyAnswer(cmd);
            } else {
                LOGGER.debug("No master, not clustered " + agentHostname);
                return new ReadyAnswer(cmd);
            }
        } catch (CloudRuntimeException e) {
            LOGGER.debug("XML RPC Exception" + e.getMessage(), e);
            throw new CloudRuntimeException("XML RPC Exception"
                    + e.getMessage(), e);
        }

    }

    /*
     * Primary storage, will throw an error if ownership does not match! Pooling
     * is a part of this, for now
     */
    protected boolean createRepo(StorageFilerTO cmd) throws XmlRpcException {
        String basePath = this.agentOvmRepoPath;
        Repository repo = new Repository(c);
        String primUuid = repo.deDash(cmd.getUuid());
        String ovsRepo = basePath + "/" + primUuid;
        /* should add port ? */
        String mountPoint = String.format("%1$s:%2$s", cmd.getHost(),
                cmd.getPath());
        String msg;

        if (cmd.getType() == StoragePoolType.NetworkFilesystem) {
            /* TODO: condense and move into Repository */
            Boolean repoExists = false;
            /* base repo first */
            try {
                repo.mountRepoFs(mountPoint, ovsRepo);
            } catch (Ovm3ResourceException e) {
                LOGGER.debug("Unable to mount NFS repository " + mountPoint + " on " + ovsRepo
                        + " requested for " + agentHostname, e);
            }
            try {
                repo.addRepo(mountPoint, ovsRepo);
                repoExists = true;
            } catch (Ovm3ResourceException  e) {
                LOGGER.debug("NFS repository " + mountPoint + " on "
                        + ovsRepo + " not found creating!", e);
            }
            if (!repoExists) {
                try {
                    repo.createRepo(mountPoint, ovsRepo, primUuid,
                            "OVS Reposutory");
                } catch (Ovm3ResourceException  e) {
                    msg = "NFS repository " + mountPoint + " on " + ovsRepo
                            + " create failed!";
                    LOGGER.debug(msg);
                    throw new CloudRuntimeException(
                            msg + " " + e.getMessage(), e);
                }
            }

            /* add base pooling first */
            if (this.agentInOvm3Pool) {
                try {
                    msg = "Configuring host for pool";
                    LOGGER.debug(msg);
                    setupPool(cmd);
                    msg = "Configured host for pool";
                    /* add clustering after pooling */
                    if (this.agentInOvm3Cluster) {
                        msg = "Configuring host for cluster";
                        LOGGER.debug(msg);
                        /* setup cluster */
                        /*
                         * From cluster.java
                         * configure_server_for_cluster(cluster conf, fs, mount,
                         * fsuuid, poolfsbaseuuid)
                         */
                        /* create_cluster(poolfsuuid,) */
                        msg = "Configuring host for cluster";
                    }
                } catch (Ovm3ResourceException e) {
                    msg = "Unable to setup pool on " + ovsRepo;
                    throw new CloudRuntimeException(msg + " " + e.getMessage(),
                            e);
                }
            } else {
                msg = "no way dude I can't stand for this";
                LOGGER.debug(msg);
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
                msg = "NFS mount " + mountPoint + " on " + agentSecStoragePath + "/"
                        + cmd.getUuid() + " create failed!";
                throw new CloudRuntimeException(msg + " " + e.getMessage(), e);
            }
        } else {
            msg = "NFS repository " + mountPoint + " on " + ovsRepo
                    + " create failed, was type " + cmd.getType();
            LOGGER.debug(msg);
            return false;
        }

        try {
            /* systemvm iso is imported here */
            prepareSecondaryStorageStore(ovsRepo);
        } catch (Exception e) {
            msg = "systemvm.iso copy failed to " + ovsRepo;
            LOGGER.debug(msg, e);
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
            if (this.agentHasMaster && this.agentInOvm3Pool) {
                LOGGER.debug("Skip systemvm iso copy, leave it to the master");
                return;
            }
            if (lock.lock(3600)) {
                try {
                    /*
                     * TODO: save src iso real name for reuse, so we don't
                     * depend on other happy little accidents.
                     */
                    File srcIso = getSystemVMPatchIsoFile();
                    String destPath = mountPoint + "/ISOs/";
                    try {
                        StoragePlugin sp = new StoragePlugin(c);
                        if (sp.getFileSize() > 0) {
                            LOGGER.info(" System VM patch ISO file already exists: "
                                    + srcIso.getAbsolutePath().toString()
                                    + ", destination: " + destPath);
                        }
                    } catch (Exception e) {
                        LOGGER.info("Copy System VM patch ISO file to secondary storage. source ISO: "
                                + srcIso.getAbsolutePath()
                                + ", destination: "
                                + destPath);
                        try {
                            SshHelper.scpTo(this.agentHostname, 22,
                                    this.agentSshUserName, null,
                                    this.agentSshPassword, destPath, srcIso
                                            .getAbsolutePath().toString(),
                                    "0644");
                        } catch (Exception es) {
                            LOGGER.error("Unexpected exception ", es);
                            String msg = "Unable to copy systemvm ISO on secondary storage. src location: "
                                    + srcIso.toString()
                                    + ", dest location: "
                                    + destPath;
                            LOGGER.error(msg);
                            throw new CloudRuntimeException(msg, es);
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

    /* stolen from vmware impl - but is wrong now... -sigh- */
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
        LOGGER.debug(url);
        File isoFile = null;
        if (url != null) {
            isoFile = new File(url.getPath());
        }
        if (isoFile == null || !isoFile.exists()) {
            isoFile = new File("/usr/share/cloudstack-common/vms/" + svmName);
        }
        if (isoFile == null || !isoFile.exists()) {
            String svm = "systemvm/dist/systemvm.iso";
            LOGGER.debug("last resort for systemvm patch iso " + svm);
            isoFile = new File(svm);
        }
        assert isoFile != null;
        if (!isoFile.exists()) {
            LOGGER.error("Unable to locate " + svmName + " in your setup at "
                    + isoFile.toString());
        }
        return isoFile;
    }

    /* get the key */
    public File getSystemVMKeyFile() {
        URL url = this.getClass().getClassLoader()
                .getResource("scripts/vm/systemvm/" + this.agentSshKey);
        File keyFile = null;
        if (url != null) {
            keyFile = new File(url.getPath());
        }
        if (keyFile == null || !keyFile.exists()) {
            keyFile = new File(
                    "/usr/share/cloudstack-common/scripts/vm/systemvm/"
                            + agentSshKey);
        }
        assert keyFile != null;
        if (!keyFile.exists()) {
            LOGGER.error("Unable to locate " + agentSshKey
                    + " in your setup at " + keyFile.toString());
        }
        return keyFile;
    }

    /*
     * TODO: local OCFS2? or iSCSI OCFS2
     */
    protected Boolean createOCFS2Sr(StorageFilerTO pool) throws XmlRpcException {
        LOGGER.debug("OCFS2 Not implemented yet");
        return false;
    }

    /* Setup a storage pool and also get the size */
    protected Answer execute(ModifyStoragePoolCommand cmd) {
        StorageFilerTO pool = cmd.getPool();
        LOGGER.debug("modifying pool " + pool);
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

            if (this.agentInOvm3Cluster) {
                /* TODO: What extras do we need here ? HB? */
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
            return new ModifyStoragePoolAnswer(cmd,
                    Long.parseLong(store.getTotalSize()), Long.parseLong(store
                            .getFreeSize()), tInfo);
        } catch (Exception e) {
            LOGGER.debug("ModifyStoragePoolCommand failed", e);
            return new Answer(cmd, false, e.getMessage());
        }
    }

    /* TODO: add iSCSI */
    protected Answer execute(CreateStoragePoolCommand cmd) {
        StorageFilerTO pool = cmd.getPool();
        LOGGER.debug("creating pool " + pool);
        try {
            if (pool.getType() == StoragePoolType.NetworkFilesystem) {
                createRepo(pool);
            } else if (pool.getType() == StoragePoolType.IscsiLUN) {
                return new Answer(cmd, false,
                        "iSCSI is unsupported at the moment");
                /* TODO: Implement iScsi like so:
                 * getIscsiSR(conn, pool.getUuid(), pool.getHost(),
                 * pool.getPath(), null, null, false);
                 */
            } else if (pool.getType() == StoragePoolType.OCFS2) {
                return new Answer(cmd, false,
                        "OCFS2 is unsupported at the moment");
            } else if (pool.getType() == StoragePoolType.PreSetup) {
                LOGGER.warn("pre setup for pool " + pool);
            } else {
                return new Answer(cmd, false, "The pool type: "
                        + pool.getType().name() + " is not supported.");
            }
        } catch (Exception e) {
            String msg = "Catch Exception " + e.getClass().getName()
                    + ", create StoragePool failed due to " + e.toString()
                    + " on host:" + agentHostname + " pool: " + pool.getHost()
                    + pool.getPath();
            LOGGER.warn(msg, e);
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
            LOGGER.debug("PrimaryStorageDownloadCommand failed", e);
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

                String secPoolUuid = setupSecondaryStorage(storeUrl);
                String primaryPoolUuid = destData.getDataStore().getUuid();
                String destPath = this.agentOvmRepoPath + "/"
                        + ovmObject.deDash(primaryPoolUuid) + "/"
                        + "Templates";
                String sourcePath = this.agentSecStoragePath + "/" + secPoolUuid;

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
                LOGGER.debug("CopyFrom: " + srcData.getObjectType() + ","
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
                    LOGGER.debug("CopyFrom: " + srcData.getObjectType() + ","
                            + srcFile + " to " + destData.getObjectType() + ","
                            + destFile);
                    host.copyFile(srcFile, destFile);
                    VolumeObjectTO newVol = new VolumeObjectTO();
                    newVol.setUuid(dstVolume.getUuid());
                    newVol.setPath(vDisksPath);
                    newVol.setFormat(ImageFormat.RAW);
                    return new CopyCmdAnswer(newVol);
                } else {
                    LOGGER.debug("Primary to Primary doesn't match");
                }
            } else {
                String msg = "Unable to do stuff for " + srcStore.getClass()
                        + ":" + srcData.getObjectType() + " to "
                        + destStore.getClass() + ":" + destData.getObjectType();
                LOGGER.debug(msg);
            }
        } catch (Exception e) {
            String msg = "Catch Exception " + e.getClass().getName()
                    + " for template due to " + e.toString();
            LOGGER.warn(msg, e);
            return new CopyCmdAnswer(msg);
        }
        return new CopyCmdAnswer("not implemented yet");
    }

    protected Answer execute(DeleteCommand cmd) {
        DataTO data = cmd.getData();
        LOGGER.debug("Deleting object: " + data.getObjectType());
        if (data.getObjectType() == DataObjectType.VOLUME) {
            return deleteVolume(cmd);
        } else if (data.getObjectType() == DataObjectType.SNAPSHOT) {
            LOGGER.info("Snapshot deletion is not implemented yet.");
        } else if (data.getObjectType() == DataObjectType.TEMPLATE) {
            LOGGER.info("Template deletion is not implemented yet.");
        } else {
            LOGGER.info(data.getObjectType() + " deletion is not implemented yet.");
        }
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
                LOGGER.debug("CreateCommand " + cmd.getTemplateUrl() + " "
                        + dst);
                Linux host = new Linux(c);
                host.copyFile(cmd.getTemplateUrl(), dst);
            } else {
                /* this is a dup with the createVolume ? */
                LOGGER.debug("CreateCommand " + dst);
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
            LOGGER.debug("CreateCommand failed", e);
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
                    LOGGER.debug("Adding root disk: " + dsk);
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
                        String isoPath = this.agentSecStoragePath + File.separator
                                + secPoolUuid + File.separator
                                + template.getPath();
                        vm.addIso(isoPath);
                        /* check if secondary storage is mounted */
                        LOGGER.debug("Adding ISO: " + isoPath);
                    }
                } else if (volume.getType() == Volume.Type.DATADISK) {
                    vm.addDataDisk(volume.getData().getPath());
                    LOGGER.debug("Adding data disk: "
                            + volume.getData().getPath());
                } else {
                    throw new CloudRuntimeException("Unknown volume type: "
                            + volume.getType());
                }
            } catch (Exception e) {
                LOGGER.debug("CreateVbds failed", e);
                throw new CloudRuntimeException("Exception" + e.getMessage(), e);
            }
        }
        return true;
    }

    protected Boolean createVifs(Xen.Vm vm, VirtualMachineTO spec) {
        NicTO[] nics = spec.getNics();
        for (NicTO nic : nics) {
            try {
                if (getNetwork(nic) != null) {
                    vm.addVif(nic.getDeviceId(), getNetwork(nic), nic.getMac());
                }
            } catch (Exception e) {
                String msg = "Unable to add vif " + nic.getType() + " for "
                        + spec.getName() + " " + e.getMessage();
                LOGGER.debug(msg);
                throw new CloudRuntimeException(msg);
            }
        }
        vm.setupVifs();
        return true;
    }

    /*
     * TODO: State keeping for VLANs that are to be provisioned, for
     * deprovisioning..
     */
    private String createVlanBridge(String networkName, Integer vlanId)
            throws Ovm3ResourceException {
        if (vlanId < 1 || vlanId > 4094) {
            String msg = "Incorrect vlan " + vlanId + ", needs to be between 1 and 4094";
            LOGGER.info(msg);
            throw new CloudRuntimeException(msg);
        }
        Network net = new Network(c);
        /* figure out if our bridged vlan exists, if not then create */
        String brName = networkName + "." + vlanId.toString();
        try {
            String physInterface = net.getPhysicalByBridgeName(networkName);
            if (net.getInterfaceByName(brName) == null) {
                net.startOvsVlanBridge(brName, physInterface, vlanId);
            } else {
                LOGGER.debug("Interface " + brName + " already exists");
            }
        } catch (Ovm3ResourceException e) {
            String msg = "Unable to create vlan " + vlanId.toString() + " bridge for " + networkName;
            LOGGER.info(msg);
            throw new CloudRuntimeException(msg + ":" + e.getMessage());
        }
        return brName;
    }

    // TODO: complete all network support
    protected String getNetwork(NicTO nic) throws Ovm3ResourceException {
        String vlanId = null;
        String bridgeName = null;
        if (nic.getBroadcastType() == BroadcastDomainType.Vlan) {
            vlanId = BroadcastDomainType.getValue(nic.getBroadcastUri());
        }

        if (nic.getType() == TrafficType.Guest) {
            if (nic.getBroadcastType() == BroadcastDomainType.Vlan
                    && !"untagged".equalsIgnoreCase(vlanId)) {
                bridgeName = createVlanBridge(agentGuestNetworkName,
                        Integer.valueOf(vlanId));
            } else {
                bridgeName = agentGuestNetworkName;
            }

        /* VLANs for other mgmt traffic ? */
        } else if (nic.getType() == TrafficType.Control) {
            bridgeName = agentControlNetworkName;
        } else if (nic.getType() == TrafficType.Public) {
            bridgeName = agentPublicNetworkName;
        } else if (nic.getType() == TrafficType.Management) {
            bridgeName = agentPrivateNetworkName;
        } else if (nic.getType() == TrafficType.Storage) {
            /* TODO: Add storage network */
            bridgeName = agentStorageNetworkName;
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
                LOGGER.error("Create VM " + vmId + " first on " + c.getIp());
                return false;
            } else {
                LOGGER.info("VM " + vmId + " exists on " + c.getIp());
            }
            host.startVm(repoId, vmId);
        } catch (Exception e) {
            LOGGER.error("Failed to start VM " + vmId + " on " + c.getIp()
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
    }

    protected void cleanup(Xen.Vm vm) {
        try {
            cleanupNetwork(vm.getVmVifs());
        } catch (XmlRpcException e) {
            LOGGER.info("Clean up network for " + vm.getVmName() + " failed", e);
        }
        String vmName = vm.getVmName();
        /* should become a single entity */
        this.vmStats.remove(vmName);
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
            synchronized (this.vmStateMap) {
                this.vmStateMap.put(vmName, State.Starting);
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
            if (domType == null || domType.isEmpty()) {
                domType = "default";
                LOGGER.debug("VM Virt type missing setting to: " + domType);
            } else {
                LOGGER.debug("VM Virt type set to " + domType + " for "
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
                String svmPath = agentOvmRepoPath + "/"
                        + ovmObject.deDash(vm.getPrimaryPoolUuid()) + "/ISOs";
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
            xen.createVm(ovmObject.deDash(vm.getPrimaryPoolUuid()),
                    vm.getVmUuid());
            xen.startVm(ovmObject.deDash(vm.getPrimaryPoolUuid()),
                    vm.getVmUuid());
            state = State.Running;

            if (vmSpec.getType() != VirtualMachine.Type.User) {
                String controlIp = null;
                for (NicTO nic : vmSpec.getNics()) {
                    if (nic.getType() == TrafficType.Control) {
                        controlIp = nic.getIp();
                    }
                }
                /* fix is in cloudstack.py for xend restart timer */
                for (int count = 0; count < 60; count++) {
                    Thread.sleep(5000);
                    CloudStackPlugin cSp = new CloudStackPlugin(c);
                    if (vmStateMap.get(vmName) == null) {
                        String msg = "VM " + vmName + " went missing on "
                                + agentHostname + ", returning stopped";
                        LOGGER.debug(msg);
                        state = State.Stopped;
                        return new StartAnswer(cmd, msg);
                    }
                    /* creative fix? */
                    try {
                        Boolean res = cSp.domrCheckSsh(controlIp);
                        LOGGER.debug("connected to " + controlIp
                                + " on attempt " + count + " result: " + res);
                        if (res) {
                            break;
                        }
                    } catch (Exception x) {
                        LOGGER.info("unable to connect to " + controlIp
                                + " on attempt " + count + " " + x.getMessage(), x);
                    }
                }
            }
            /*
             * TODO: Can't remember if HA worked if we were only a pool ?
             */
            if (agentInOvm3Pool && agentInOvm3Cluster) {
                xen.configureVmHa(ovmObject.deDash(vm.getPrimaryPoolUuid()),
                        vm.getVmUuid(), true);
            }
            /* should be starting no ? */
            state = State.Running;
            return new StartAnswer(cmd);
        } catch (Exception e) {
            LOGGER.debug("Start vm " + vmName + " failed", e);
            state = State.Stopped;
            /* TODO: cleanup vm details ? */
            return new StartAnswer(cmd, e.getMessage());
        } finally {
            synchronized (this.vmStateMap) {
                // TODO: check if we have to set to Stopped???
                this.vmStateMap.put(vmName, state);
            }
        }
    }

    /* VR */
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

    /*
     * TODO: get the list of bridges and see if the bridge should go or stay
     * This should be combined with the bridges/vms map, or we should switch to
     * a bridge per vm.... -cough-
     */
    protected ExecutionResult cleanupNetworkElementCommand(IpAssocCommand cmd) {
        return new ExecutionResult(true, null);
    }

    /* TODO: fill in these so we can get rid of the VR/SVM/etc specifics */
    private ExecutionResult prepareNetworkElementCommand(
            SetupGuestNetworkCommand cmd) {
        return new ExecutionResult(true, null);
    }

    private ExecutionResult prepareNetworkElementCommand(IpAssocVpcCommand cmd) {
        return new ExecutionResult(true, null);
    }

    protected ExecutionResult prepareNetworkElementCommand(
            SetSourceNatCommand cmd) {
        return new ExecutionResult(true, null);
    }

    private ExecutionResult prepareNetworkElementCommand(IpAssocCommand cmd) {
        return new ExecutionResult(true, null);
    }

    /* VR things */
    @Override
    public ExecutionResult executeInVR(String routerIp, String script,
            String args) {
        return executeInVR(routerIp, script, args, VRTIMEOUT);
    }

    @Override
    public ExecutionResult executeInVR(String routerIp, String script,
            String args, int timeout) {
        /* TODO: either here OR on cloudstack.py */
        if (!script.contains(this.domRCloudPath)) {
            script = this.domRCloudPath + "/" + script;
        }
        String cmd = script + " " + args;
        LOGGER.debug("executeInVR via " + agentName + " on " + routerIp
                + ": " + cmd);
        try {
            CloudStackPlugin cSp = new CloudStackPlugin(c);
            CloudStackPlugin.ReturnCode result;
            result = cSp.domrExec(routerIp, cmd);
            return new ExecutionResult(result.getRc(), result.getStdOut());
        } catch (Exception e) {
            LOGGER.error("executeInVR FAILED via " + agentName + " on "
                    + routerIp + ":" + cmd + ", " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public ExecutionResult createFileInVR(String routerIp, String path,
            String filename, String content) {
        String error = null;
        LOGGER.debug("createFileInVR via " + agentName + " on " + routerIp
                + ": " + path + "/" + filename + ", content: " + content);
        try {
            CloudStackPlugin cSp = new CloudStackPlugin(c);
            boolean result = cSp.ovsDomrUploadFile(routerIp, path, filename,
                    content);
            return new ExecutionResult(result, "");
        } catch (Exception e) {
            error = e.getMessage();
            LOGGER.warn("createFileInVR failed for " + path + "/" + filename
                    + " in VR " + routerIp + " via " + agentName + ": " + error, e);
        }
        return new ExecutionResult(error == null, error);
    }

    /* copy paste, why isn't this just generic in the VirtualRoutingResource ? */
    protected Answer execute(NetworkUsageCommand cmd) {
        if (cmd.isForVpc()) {
            return VPCNetworkUsage(cmd);
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Executing resource NetworkUsageCommand " + cmd);
        }
        if (cmd.getOption() != null && "create".equals(cmd.getOption())) {
            String result = networkUsage(cmd.getPrivateIP(), "create", null);
            return new NetworkUsageAnswer(cmd, result, 0L,
                    0L);
        }
        long[] stats = getNetworkStats(cmd.getPrivateIP());

       return new NetworkUsageAnswer(cmd, "", stats[0],
                stats[1]);
    }

    /* copy paste, why isn't this just generic in the VirtualRoutingResource ? */
    protected String networkUsage(final String privateIpAddress,
            final String option, final String ethName) {
        String args = null;
        if ("get".equals(option)) {
            args = "-g";
        } else if ("create".equals(option)) {
            args = "-c";
        } else if ("reset".equals(option)) {
            args = "-r";
        } else if ("addVif".equals(option)) {
            args = "-a";
            args += ethName;
        } else if ("deleteVif".equals(option)) {
            args = "-d";
            args += ethName;
        }
        ExecutionResult result = executeInVR(privateIpAddress, "netusage.sh",
                args);

        if (!result.isSuccess()) {
            return null;
        }

        return result.getDetails();
    }

    /* copy paste, why isn't this just generic in the VirtualRoutingResource ? */
    private long[] getNetworkStats(String privateIP) {
        String result = networkUsage(privateIP, "get", null);
        long[] stats = new long[2];
        if (result != null) {
            try {
                String[] splitResult = result.split(":");
                int i = 0;
                while (i < splitResult.length - 1) {
                    stats[0] += (Long.valueOf(splitResult[i++])).longValue();
                    stats[1] += (Long.valueOf(splitResult[i++])).longValue();
                }
            } catch (Exception e) {
                LOGGER.warn(
                        "Unable to parse return from script return of network usage command: "
                                + e.toString(), e);
            }
        }
        return stats;
    }

    /* copy paste, why isn't this just generic in the VirtualRoutingResource ? */
    protected NetworkUsageAnswer VPCNetworkUsage(NetworkUsageCommand cmd) {
        String privateIp = cmd.getPrivateIP();
        String option = cmd.getOption();
        String publicIp = cmd.getGatewayIP();

        String args = "-l " + publicIp + " ";
        if ("get".equals(option)) {
            args += "-g";
        } else if ("create".equals(option)) {
            args += "-c";
            String vpcCIDR = cmd.getVpcCIDR();
            args += " -v " + vpcCIDR;
        } else if ("reset".equals(option)) {
            args += "-r";
        } else if ("vpn".equals(option)) {
            args += "-n";
        } else if ("remove".equals(option)) {
            args += "-d";
        } else {
            return new NetworkUsageAnswer(cmd, "success", 0L, 0L);
        }

        ExecutionResult callResult = executeInVR(privateIp, "vpc_netusage.sh",
                args);

        if (!callResult.isSuccess()) {
            LOGGER.error("Unable to execute NetworkUsage command on DomR ("
                    + privateIp
                    + "), domR may not be ready yet. failure due to "
                    + callResult.getDetails());
        }

        if ("get".equals(option) || "vpn".equals(option)) {
            String result = callResult.getDetails();
            if (result == null || result.isEmpty()) {
                LOGGER.error(" vpc network usage get returns empty ");
            }
            long[] stats = new long[2];
            if (result != null) {
                String[] splitResult = result.split(":");
                int i = 0;
                while (i < splitResult.length - 1) {
                    stats[0] += (Long.valueOf(splitResult[i++])).longValue();
                    stats[1] += (Long.valueOf(splitResult[i++])).longValue();
                }
                return new NetworkUsageAnswer(cmd, "success", stats[0],
                        stats[1]);
            }
        }
        return new NetworkUsageAnswer(cmd, "success", 0L, 0L);
    }

    /*
     * we don't for now, gave an error on migration though....
     */
    private Answer execute(NetworkRulesSystemVmCommand cmd) {
        boolean success = true;
        return new Answer(cmd, success, "");
    }

    public CheckSshAnswer execute(CheckSshCommand cmd) {
        String vmName = cmd.getName();
        String privateIp = cmd.getIp();
        int cmdPort = cmd.getPort();
        int interval = cmd.getInterval();
        int retries = cmd.getRetries();

        try {
            CloudStackPlugin cSp = new CloudStackPlugin(c);
            if (!cSp.domrCheckPort(privateIp, cmdPort, retries, interval)) {
                String msg = "Port " + cmdPort + " not reachable for " + vmName
                        + " via " + agentName;
                LOGGER.info(msg);
                return new CheckSshAnswer(cmd, msg);
            }
        } catch (Exception e) {
            String msg = "Can not reach port " + cmdPort + " on System vm "
                    + vmName + " via " + agentName + " due to exception: " + e;
            LOGGER.error(msg);
            return new CheckSshAnswer(cmd, msg);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Ping " + cmdPort + " succeeded for vm " + vmName
                    + " via " + agentName + cmd);
        }
        return new CheckSshAnswer(cmd);
    }

    protected Answer execute(GetHostStatsCommand cmd) {
        try {
            CloudStackPlugin cSp = new CloudStackPlugin(c);
            Map<String, String> stats = cSp
                    .ovsDom0Stats(this.agentPublicNetworkName);
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
            LOGGER.debug(
                    "Get host stats of " + cmd.getHostName() + " failed", e);
            return new Answer(cmd, false, e.getMessage());
        }
    }

    /* TODO: Stop the VM, this means cleanup too, should this be destroy ? */
    @Override
    public StopAnswer execute(StopCommand cmd) {
        String vmName = cmd.getVmName();
        State state = State.Error;
        synchronized (this.vmStateMap) {
            state = vmStateMap.get(vmName);
            this.vmStateMap.put(vmName, State.Stopping);
        }

        try {
            Xen vms = new Xen(c);
            Xen.Vm vm = null;
            vm = vms.getRunningVmConfig(vmName);

            if (vm == null) {
                state = State.Stopping;
                LOGGER.debug("Unable to get details of vm: " + vmName
                        + ", treating it as Stopping");
                return new StopAnswer(cmd, "success", true);
            }
            String repoId = ovmObject.deDash(vm.getVmRootDiskPoolId());
            String vmId = vm.getVmUuid();

            /* can we do without the poolId ? */
            vms.stopVm(repoId, vmId);
            int tries = 30;
            while (vms.getRunningVmConfig(vmName) != null && tries > 0) {
                String msg = "Waiting for " + vmName + " to stop";
                LOGGER.debug(msg);
                tries--;
                Thread.sleep(10 * 1000);
            }
            vms.deleteVm(repoId, vmId);
            /* TODO: Check cleanup */
            this.cleanup(vm);

            if (vms.getRunningVmConfig(vmName) != null) {
                String msg = "Stop " + vmName + " failed ";
                LOGGER.debug(msg);
                return new StopAnswer(cmd, msg, false);
            }
            state = State.Stopped;
            return new StopAnswer(cmd, "success", true);
        } catch (Exception e) {
            /* TODO: check output of message, might be that it did get removed */
            LOGGER.debug("Stop " + vmName + " failed ", e);
            return new StopAnswer(cmd, e.getMessage(), false);
        } finally {
            synchronized (this.vmStateMap) {
                if (state != null) {
                    this.vmStateMap.put(vmName, state);
                } else {
                    this.vmStateMap.remove(vmName);
                }
            }
        }
    }

    /* Reboot the VM and destroy should call the same method in here ? */
    @Override
    public RebootAnswer execute(RebootCommand cmd) {
        String vmName = cmd.getVmName();

        synchronized (this.vmStateMap) {
            this.vmStateMap.put(vmName, State.Starting);
        }

        try {
            Xen xen = new Xen(c);
            Xen.Vm vm = xen.getRunningVmConfig(vmName);
            /* TODO: stop, start or reboot, reboot for now ? */
            xen.rebootVm(ovmObject.deDash(vm.getVmRootDiskPoolId()),
                    vm.getVmUuid());
            vm = xen.getRunningVmConfig(vmName);
            /* erh but this don't work, should point at cloudstackplugin */
            Integer vncPort = vm.getVncPort();
            return new RebootAnswer(cmd, null, vncPort);
        } catch (Exception e) {
            LOGGER.debug("Reboot " + vmName + " failed", e);
            return new RebootAnswer(cmd, e.getMessage(), false);
        } finally {
            synchronized (this.vmStateMap) {
                this.vmStateMap.put(cmd.getVmName(), State.Running);
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
    protected Map<String, HostVmStateReportEntry> hostVmStateReport()
            throws Ovm3ResourceException {
        final Map<String, HostVmStateReportEntry> vmStates = new HashMap<String, HostVmStateReportEntry>();
        for (final Map.Entry<String, State> vm : vmStateMap.entrySet()) {
            /* TODO: Figure out how to get xentools version in here */
            LOGGER.debug("VM " + vm.getKey() + " state: " + vm.getValue()
                    + ":" + convertStateToPower(vm.getValue()));
            vmStates.put(vm.getKey(), new HostVmStateReportEntry(
                    convertStateToPower(vm.getValue()), c.getIp()));
        }
        return vmStates;
    }

    /* uses the running configuration, not the vm.cfg configuration */
    protected Map<String, Xen.Vm> getAllVms() throws Ovm3ResourceException {
        try {
            Xen vms = new Xen(c);
            return vms.getRunningVmConfigs();
        } catch (Exception e) {
            LOGGER.debug("getting VM list from " + agentHostname + " failed",
                    e);
            throw new CloudRuntimeException("Exception on getting VMs from "
                    + agentHostname + ":" + e.getMessage(), e);
        }
    }

    /*
     * Get all the states and set them up according to xm(1) TODO: check
     * migrating ?
     */
    protected Map<String, State> getAllVmStates() throws Ovm3ResourceException {
        Map<String, Xen.Vm> vms = getAllVms();
        final Map<String, State> states = new HashMap<String, State>();
        for (final Map.Entry<String, Xen.Vm> entry : vms.entrySet()) {
            Xen.Vm vm = entry.getValue();
            State ns = State.Running;
            String as = vm.getVmState();
            if (vm.isControlDomain() || as == null) {
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
                /* TODO: Double check this, as the change might hurt us */
                if (this.vmStateMap.get(vm.getVmName()) == State.Migrating) {
                    ns = State.Migrating;
                } else {
                    ns = State.Stopped;
                }
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
            LOGGER.trace("state " + ns + " for " + vm.getVmName()
                    + " based on " + as);
            states.put(vm.getVmName(), ns);
        }
        return states;
    }

    /* sync the state we know of with reality */
    protected Map<String, State> syncState() throws Ovm3ResourceException {
        Map<String, State> newStates;
        Map<String, State> oldStates = null;
        final Map<String, State> changes = new HashMap<String, State>();
        try {
            newStates = getAllVmStates();
        } catch (Ovm3ResourceException e) {
            LOGGER.error("Ovm3 full sync failed: ", e);
            throw e;
        }
        synchronized (vmStateMap) {
            oldStates = new HashMap<String, State>(vmStateMap.size());
            oldStates.putAll(vmStateMap);

            for (final Map.Entry<String, State> entry : newStates
                    .entrySet()) {
                final String vmName = entry.getKey();
                State newState = entry.getValue();
                final State oldState = oldStates.remove(vmName);
                LOGGER.trace("state for " + vmName + ", old: " + oldState
                        + ", new: " + newState);

                /* eurh ? */
                if (newState == State.Stopped && oldState != State.Stopping
                        && oldState != null && oldState != State.Stopped) {
                    LOGGER.trace("Getting power state....");
                    newState = State.Running;
                }

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("VM "
                            + vmName
                            + ": ovm has state "
                            + newState
                            + " and we have state "
                            + (oldState != null ? oldState.toString()
                                    : "null"));
                }

                /* TODO: is this really true ? should be right ? */
                if (newState == State.Migrating) {
                    LOGGER.trace(vmName
                            + " is migrating, skipping state check");
                    continue;
                }

                if (oldState == null) {
                    vmStateMap.put(vmName, newState);
                    LOGGER.trace("New state without old state: " + vmName);
                    changes.put(vmName, newState);
                } else if (oldState == State.Starting) {
                    if (newState == State.Running) {
                        vmStateMap.put(vmName, newState);
                    } else if (newState == State.Stopped) {
                        LOGGER.trace("Ignoring vm " + vmName
                                + " because of a lag in starting the vm.");
                    }
                } else if (oldState == State.Migrating) {
                    if (newState == State.Running) {
                        LOGGER.trace("Detected that a migrating VM is now running: "
                                + vmName);
                        vmStateMap.put(vmName, newState);
                    }
                } else if (oldState == State.Stopping) {
                    if (newState == State.Stopped) {
                        vmStateMap.put(vmName, newState);
                    } else if (newState == State.Running) {
                        LOGGER.trace("Ignoring vm " + vmName
                                + " because of a lag in stopping the vm. ");
                    }
                } else if (oldState != newState) {
                    vmStateMap.put(vmName, newState);
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
                    LOGGER.trace("Removing VM " + vmName
                            + " in transition state stopping.");
                    vmStateMap.remove(vmName);
                } else if (oldState == State.Starting) {
                    LOGGER.trace("Removing VM " + vmName
                            + " in transition state starting.");
                    vmStateMap.remove(vmName);
                } else if (oldState == State.Stopped) {
                    LOGGER.trace("Stopped VM " + vmName + " removing.");
                    vmStateMap.remove(vmName);
                } else if (oldState == State.Migrating) {
                    /*
                     * do something smarter here.. newstate should say
                     * stopping already
                     */
                    LOGGER.debug("Ignoring VM " + vmName
                            + " in migrating state.");
                } else {
                    /* if it's not there name it stopping */
                    State state = State.Stopping;
                    LOGGER.trace("VM "
                            + vmName
                            + " is now missing from ovm3 server so removing it");
                    /* TODO: something about killed/halted VM's in here ? */
                    changes.put(vmName, state);
                    vmStateMap.remove(vmName);
                    vmStateMap.put(vmName, state);
                }
            }
        }
        return changes;
    }

    /* cleanup the storageplugin so we can use an object here */
    protected GetStorageStatsAnswer execute(final GetStorageStatsCommand cmd) {
        LOGGER.debug("Getting stats for: " + cmd.getStorageId());
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
        } catch (Ovm3ResourceException  e) {
            LOGGER.debug(
                    "GetStorageStatsCommand on pool " + cmd.getStorageId()
                            + " failed", e);
            return new GetStorageStatsAnswer(cmd, e.getMessage());
        }
    }

    private VmStatsEntry getVmStat(String vmName) {
        CloudStackPlugin cSp = new CloudStackPlugin(c);
        Map<String, String> oleStats = this.vmStats.get(vmName);
        VmStatsEntry stats = new VmStatsEntry();
        Map<String, String> newStats;
        try {
            newStats = cSp.ovsDomUStats(vmName);
        } catch (Ovm3ResourceException e) {
            LOGGER.info("Unable to retrieve stats from " + vmName, e);
            return stats;
        }
        if (oleStats == null) {
            stats.setNumCPUs(1);
            stats.setNetworkReadKBs(0);
            stats.setNetworkWriteKBs(0);
            stats.setDiskReadKBs(0);
            stats.setDiskWriteKBs(0);
            stats.setDiskReadIOs(0);
            stats.setDiskWriteIOs(0);
            stats.setCPUUtilization(0);
            stats.setEntityType("vm");
        } else {
            /* beware of negatives ? */
            Integer cpus = Integer.parseInt(newStats.get("vcpus"));
            stats.setNumCPUs(Integer.parseInt(newStats.get(cpus)));
            stats.setNetworkReadKBs(Double.parseDouble(newStats.get("rx_bytes"))
                    - Double.parseDouble(oleStats.get("rx_bytes")));
            stats.setNetworkWriteKBs(Double.parseDouble(newStats.get("tx_bytes"))
                    - Double.parseDouble(oleStats.get("tx_bytes")));
            stats.setDiskReadKBs(Double.parseDouble(newStats.get("rd_bytes"))
                    - Double.parseDouble(oleStats.get("rd_bytes")));
            stats.setDiskWriteKBs(Double.parseDouble(newStats.get("rw_bytes"))
                    - Double.parseDouble(oleStats.get("rw_bytes")));
            stats.setDiskReadIOs(Double.parseDouble(newStats.get("rd_ops"))
                    - Double.parseDouble(oleStats.get("rd_ops")));
            stats.setDiskWriteIOs(Double.parseDouble(newStats.get("rw_ops"))
                    - Double.parseDouble(oleStats.get("rw_ops")));
            Double dCpu = Double.parseDouble(newStats.get("cputime"))
                    - Double.parseDouble(oleStats.get("cputime"));
            Double dTime = Double.parseDouble(newStats.get("uptime"))
                    - Double.parseDouble(oleStats.get("uptime"));
            Double cpupct = dCpu / dTime * 100 * cpus;
            stats.setCPUUtilization(cpupct);
            stats.setEntityType("vm");
        }
        ((ConcurrentHashMap<String, Map<String, String>>) this.vmStats).replace(vmName, newStats);
        return stats;
    }

    /* TODO: The main caller for vm statstics */
    protected GetVmStatsAnswer execute(GetVmStatsCommand cmd) {
        List<String> vmNames = cmd.getVmNames();
        Map<String, VmStatsEntry> vmStatsNameMap = new HashMap<String, VmStatsEntry>();
        for (String vmName : vmNames) {
            VmStatsEntry e = getVmStat(vmName);
            vmStatsNameMap.put(vmName, e);
        }
        return new GetVmStatsAnswer(cmd, (HashMap<String, VmStatsEntry>) vmStatsNameMap);
    }

    /* TODO: Hot plugging harddisks... */
    protected AttachVolumeAnswer execute(AttachVolumeCommand cmd) {
        return new AttachVolumeAnswer(cmd, "You must stop " + cmd.getVmName()
                + " first, Ovm3 doesn't support hotplug datadisk");
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
        } catch (Ovm3ResourceException e) {
            LOGGER.debug("Destroy volume " + vol.getName() + " failed for "
                    + vmName + " ", e);
            return new Answer(cmd, false, e.getMessage());
        }
    }

    /* Migration should make sure both HVs are the same ? */
    protected PrepareForMigrationAnswer execute(PrepareForMigrationCommand cmd) {
        VirtualMachineTO vm = cmd.getVirtualMachine();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Preparing host for migrating " + vm.getName());
        }
        NicTO[] nics = vm.getNics();
        try {
            for (NicTO nic : nics) {
                getNetwork(nic);
            }
            synchronized (vmStateMap) {
                vmStateMap.put(vm.getName(), State.Migrating);
            }
            LOGGER.debug("VM " + vm.getName() + " is in Migrating state");
            return new PrepareForMigrationAnswer(cmd);
        } catch (Ovm3ResourceException e) {
            LOGGER.error("Catch Exception " + e.getClass().getName()
                    + " prepare for migration failed due to: " + e.getMessage());
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
        LOGGER.info(msg);
        if (!this.agentInOvm3Cluster && !this.agentInOvm3Pool) {
            try {
                Xen xen = new Xen(c);
                Xen.Vm vm = xen.getRunningVmConfig(vmName);
                HostVO destHost = resourceMgr.findHostByGuid(destUuid);
                if (destHost == null) {
                    msg = "Unable to find migration target host in DB "
                            + destUuid + " with ip " + destIp;
                    LOGGER.info(msg);
                    return new MigrateAnswer(cmd, false, msg, null);
                }
                xen.stopVm(ovmObject.deDash(vm.getVmRootDiskPoolId()),
                        vm.getVmUuid());
                msg = destHost.toString();
                state = State.Stopping;
                return new MigrateAnswer(cmd, false, msg, null);
            } catch (Ovm3ResourceException e) {
                msg = "Unpooled VM Migrate of " + vmName + " to " + destUuid
                        + " failed due to: " + e.getMessage();
                LOGGER.debug(msg, e);
                return new MigrateAnswer(cmd, false, msg, null);
            } finally {
                /* shouldn't we just reinitialize completely as a last resort ? */
                synchronized (vmStateMap) {
                    vmStateMap.put(vmName, state);
                }
            }
        } else {
            try {
                Xen xen = new Xen(c);
                Xen.Vm vm = xen.getRunningVmConfig(vmName);
                if (vm == null) {
                    state = State.Stopped;
                    msg = vmName + " is no running on " + agentHostname;
                    return new MigrateAnswer(cmd, false, msg, null);
                }
                /* not a storage migration!!! */
                xen.migrateVm(ovmObject.deDash(vm.getVmRootDiskPoolId()),
                        vm.getVmUuid(), destIp);
                state = State.Stopping;
                msg = "Migration of " + vmName + " successfull";
                return new MigrateAnswer(cmd, true, msg, null);
            } catch (Ovm3ResourceException e) {
                msg = "Pooled VM Migrate" + ": Migration of " + vmName + " to "
                        + destIp + " failed due to " + e.getMessage();
                LOGGER.debug(msg, e);
                return new MigrateAnswer(cmd, false, msg, null);
            } finally {
                /* TODO: should we add a stopped check here ? */
                synchronized (vmStateMap) {
                    vmStateMap.put(vmName, state);
                }
            }
        }
    }

    /* hHeck "the" virtual machine */
    protected CheckVirtualMachineAnswer execute(
            final CheckVirtualMachineCommand cmd) {
        String vmName = cmd.getVmName();
        try {
            CloudStackPlugin plug = new CloudStackPlugin(c);
            Integer vncPort = Integer.valueOf(plug.getVncPort(vmName));
            if (vncPort == 0) {
                LOGGER.warn("No VNC port for " + vmName);
            }
            /* we already have the state ftw */
            Map<String, State> states = getAllVmStates();
            State vmState = states.get(vmName);
            if (vmState == null) {
                LOGGER.warn("Check state of " + vmName
                        + " return null in CheckVirtualMachineCommand");
                vmState = State.Stopped;
            }
            synchronized (vmStateMap) {
                vmStateMap.put(vmName, State.Running);
            }
            return new CheckVirtualMachineAnswer(cmd, vmState, vncPort);
        } catch (Ovm3ResourceException e) {
            LOGGER.debug("Check migration for " + vmName + " failed", e);
            return new CheckVirtualMachineAnswer(cmd, State.Stopped, null);
        }
    }

    protected MaintainAnswer execute(MaintainCommand cmd) {
        /* TODO: leave cluster, leave pool, release ownership, cleanout and start over ? */
        try {
            Network net = new Network(c);
            net.stopOvsLocalConfig(agentControlNetworkName);
        } catch (Ovm3ResourceException e) {
            LOGGER.debug("unable to disable " + agentControlNetworkName, e);
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
            LOGGER.debug("get vnc port for " + cmd.getName() + ": " + vncPort);
            return new GetVncPortAnswer(cmd, agentIp, vncPort);
        } catch (Ovm3ResourceException e) {
            LOGGER.debug("get vnc port for " + cmd.getName() + " failed", e);
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
        } catch (Ovm3ResourceException e) {
            LOGGER.debug("Ping " + cmd.getComputingHostIp() + " failed", e);
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
            LOGGER.error("fencing of  " + cmd.getHostIp() + " failed: ", e);
            return new FenceAnswer(cmd, false, e.getMessage());
        }
    }

    /* cleanup nested try stuff */
    protected boolean prepareForPool() throws ConfigurationException {
        /* need single master uuid */
        try {
            Linux host = new Linux(c);
            Pool pool = new Pool(c);

            /* setup pool and role, needs utility to be able to do things */
            if (host.getServerRoles().contentEquals(
                    pool.getValidRoles().toString())) {
                LOGGER.debug("Server role for host " + agentHostname
                        + " is ok");
            } else {
                try {
                    pool.setServerRoles(pool.getValidRoles());
                } catch (Ovm3ResourceException e) {
                    LOGGER.debug("Failed to set server role for host "
                            + agentHostname, e);
                    throw new ConfigurationException(
                            "Unable to set server role for host");
                }
            }
            if (host.getMembershipState().contentEquals("Unowned")) {
                try {
                    LOGGER.debug("Take ownership of host " + agentHostname);
                    pool.takeOwnership(agentOwnedByUuid, "");
                } catch (Ovm3ResourceException e) {
                    String msg = "Failed to take ownership of host "
                            + agentHostname;
                    LOGGER.debug(msg, e);
                    throw new ConfigurationException(msg);
                }
            } else {
                /* TODO: check if it's part of our pool, give ok if it is */
                if (host.getManagerUuid().equals(agentOwnedByUuid)) {
                    String msg = "Host " + agentHostname + " owned by us";
                    LOGGER.debug(msg);
                    return true;
                } else {
                    String msg = "Host " + agentHostname
                            + " already part of a pool, and not owned by us";
                    LOGGER.debug(msg);
                    throw new ConfigurationException(msg);
                }
            }
        } catch (ConfigurationException es) {
            String msg = "Failed to prepare " + agentHostname + " for pool";
            LOGGER.debug(msg, es);
            throw new ConfigurationException(msg + ": " + es.getMessage());
        }
        return true;
    }

    /*
     * TODO: redo this, it's a mess now.
     */
    protected Boolean setupPool(StorageFilerTO cmd)
            throws Ovm3ResourceException {
        String primUuid = cmd.getUuid();
        String ssUuid = ovmObject.deDash(primUuid);
        String fsType = "nfs";
        /* TODO: 16, need to get this from the cluster id actually */
        String clusterUuid = agentOwnedByUuid.substring(0, 15);
        String managerId = agentOwnedByUuid;
        String poolAlias = cmd.getHost() + ":" + cmd.getPath();
        String mountPoint = String.format("%1$s:%2$s", cmd.getHost(),
                cmd.getPath())
                + "/VirtualMachines";
        List<String> members = new ArrayList<String>();
        Integer poolSize = 0;
        String msg = "";

        Pool poolHost = new Pool(c);
        PoolOCFS2 poolFs = new PoolOCFS2(c);
        agentIsMaster = masterCheck();
        if (agentIsMaster && !agentHasMaster) {
            try {
               msg = "Create poolfs on " + agentHostname
                       + " for repo " + primUuid;
               LOGGER.debug(msg);
               poolFs.createPoolFs(fsType, mountPoint, clusterUuid,
                       primUuid, ssUuid, managerId, primUuid);
            } catch (Ovm3ResourceException e) {
                throw e;
            }
            try {
                poolHost.createServerPool(poolAlias, primUuid,
                        ovm3PoolVip, poolSize + 1, agentHostname,
                        agentIp);
             } catch (Ovm3ResourceException e) {
                throw e;
             }
        } else if (!agentIsMaster || agentHasMaster) {
            try {
                poolHost.joinServerPool(poolAlias, primUuid,
                          ovm3PoolVip, poolSize, agentHostname, agentIp);
            } catch (Ovm3ResourceException e) {
                throw e;
            }
            Pool poolMaster = new Pool(m);
            // poolSize = poolMaster.getPoolMemberIpList().size() + 1;
            members.addAll(poolMaster.getPoolMemberIpList());

            if (!members.contains(agentIp)) {
                members.add(agentIp);
            }
            for (String member : members) {
                Connection x = new Connection(member, agentOvsAgentPort,
                       agentOvsAgentUser, agentOvsAgentPassword);
                Pool xpool = new Pool(x);
                xpool.setPoolIps(members);
                xpool.setPoolMemberIpList();
                msg = "Added " + member + " to pool " + primUuid;
                LOGGER.debug(msg);
            }
        } else {
            LOGGER.debug("Pool " + primUuid + " already configured on "
                    + agentHostname);
        }
        return true;
    }

    /* the storage url combination of host and path is unique */
    protected String setupSecondaryStorage(String url) throws Ovm3ResourceException {
        URI uri = URI.create(url);
        String uuid = ovmObject.newUuid(uri.getHost() + ":" + uri.getPath());
        LOGGER.info("Secondary storage with uuid: " + uuid);
        return setupNfsStorage(uri, uuid);
    }

    /* NFS only for now, matches FileSys */
    protected String setupNfsStorage(URI uri, String uuid) throws Ovm3ResourceException {
        String fsUri = "nfs";
        String msg = "";
        String mountPoint = this.agentSecStoragePath + "/" + uuid;
        Linux host = new Linux(c);

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
            } catch (Ovm3ResourceException ec) {
                msg = "Nfs storage " + uri + " mount on " + mountPoint
                        + " FAILED " + ec.getMessage();
                LOGGER.error(msg);
                throw ec;
            }
        } else {
           msg = "NFS storage " + uri + " already mounted on "
                   + mountPoint;
            return uuid;
        }
    }

    /* TODO: move the connection elsewhere.... */
    protected boolean masterCheck() throws Ovm3ResourceException {
        if ("".equals(ovm3PoolVip)) {
            LOGGER.debug("No cluster vip, not checking for master");
            return false;
        }

        /* should get the details of that host hmmz */
        m = new Connection(ovm3PoolVip, agentOvsAgentPort,
                agentOvsAgentUser, agentOvsAgentPassword);
        Linux master = new Linux(m);
        if (master.getHostName().equals(agentHostname)) {
            LOGGER.debug("Host " + agentHostname + " is master");
            this.agentIsMaster = true;
        } else {
            LOGGER.debug("Host " + agentHostname + " has master "
                    + master.getHostName());
            agentHasMaster = true;
        }
        if (!this.agentIsMaster) {
            if (this.agentHasMaster) {
                LOGGER.debug("Host " + agentHostname
                        + " will become a slave " + ovm3PoolVip);
                return this.agentIsMaster;
            } else {
                this.agentIsMaster = true;
                LOGGER.debug("Host " + agentHostname
                        + " will become a master " + ovm3PoolVip);
                return this.agentIsMaster;
            }
        }
        return this.agentIsMaster;
    }

    /* check if a VM is running should be added */
    protected CreatePrivateTemplateAnswer execute(
            final CreatePrivateTemplateFromVolumeCommand cmd) {
        String volumePath = cmd.getVolumePath();
        Long accountId = cmd.getAccountId();
        Long templateId = cmd.getTemplateId();
        int wait = cmd.getWait();
        if (wait == 0) {
            /* Defaut timeout 2 hours */
            wait = 7200;
        }

        try {
            /* missing uuid */
            String installPath = agentOvmRepoPath + "/Templates/" + accountId + "/"
                    + templateId;
            Linux host = new Linux(c);
            /* check if VM is running or thrown an error, or pause it :P */
            host.copyFile(volumePath, installPath);
            /* TODO: look at the original */
            return new CreatePrivateTemplateAnswer(cmd, true, installPath);
        } catch (Exception e) {
            LOGGER.debug("Create template failed", e);
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
                LOGGER.debug("Copy to  secondary storage " + volumePath
                        + " to " + secondaryStorageURL);
                host.copyFile(volumePath, secondaryStorageURL);
                /* from secondary storage */
            } else {
                LOGGER.debug("Copy from secondary storage "
                        + secondaryStorageURL + " to " + volumePath);
                host.copyFile(secondaryStorageURL, volumePath);
            }
            /* check the truth of this */
            return new CopyVolumeAnswer(cmd, true, null, null, null);
        } catch (Ovm3ResourceException e) {
            LOGGER.debug("Copy volume failed", e);
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
        } catch (Ovm3ResourceException  e) {
            LOGGER.debug(
                    "Delete storage pool on host "
                            + agentHostname
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
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Checking if network name setup is done on "
                    + agentHostname);
        }

        List<PhysicalNetworkSetupInfo> infoList = cmd
                .getPhysicalNetworkInfoList();
        /* here we assume all networks are set */
        for (PhysicalNetworkSetupInfo info : infoList) {
            if (info.getGuestNetworkName() == null) {
                info.setGuestNetworkName(this.agentGuestNetworkName);
            }
            if (info.getPublicNetworkName() == null) {
                info.setPublicNetworkName(this.agentPublicNetworkName);
            }
            if (info.getPrivateNetworkName() == null) {
                info.setPrivateNetworkName(this.agentPrivateNetworkName);
            }
            if (info.getStorageNetworkName() == null) {
                info.setStorageNetworkName(this.agentStorageNetworkName);
            }

            if (!isNetworkSetupByName(info.getGuestNetworkName())) {
                String msg = "For Physical Network id:"
                        + info.getPhysicalNetworkId()
                        + ", Guest Network is not configured on the backend by name "
                        + info.getGuestNetworkName();
                LOGGER.error(msg);
                return new CheckNetworkAnswer(cmd, false, msg);
            }
            if (!isNetworkSetupByName(info.getPrivateNetworkName())) {
                String msg = "For Physical Network id:"
                        + info.getPhysicalNetworkId()
                        + ", Private Network is not configured on the backend by name "
                        + info.getPrivateNetworkName();
                LOGGER.error(msg);
                return new CheckNetworkAnswer(cmd, false, msg);
            }
            if (!isNetworkSetupByName(info.getPublicNetworkName())) {
                String msg = "For Physical Network id:"
                        + info.getPhysicalNetworkId()
                        + ", Public Network is not configured on the backend by name "
                        + info.getPublicNetworkName();
                LOGGER.error(msg);
                return new CheckNetworkAnswer(cmd, false, msg);
            }
            /* Storage network is optional, will revert to private otherwise */
        }

        return new CheckNetworkAnswer(cmd, true, "Network Setup check by names is done");

    }

    private boolean isNetworkSetupByName(String nameTag) {
        LOGGER.debug("known networks: " + this.agentGuestNetworkName + " "
                + this.agentPublicNetworkName + " " + this.agentPrivateNetworkName + " "
                + this.agentStorageNetworkName);
        if (nameTag != null) {
            LOGGER.debug("Looking for network setup by name " + nameTag);

            try {
                Network net = new Network(c);
                net.setBridgeList(agentInterfaces);
                if (net.getBridgeByName(nameTag) != null) {
                    LOGGER.debug("Found bridge with name: " + nameTag);
                    return true;
                }
            } catch (Ovm3ResourceException  e) {
                LOGGER.debug("Unxpected error looking for name: " + nameTag, e);
                return false;
            }
        }
        LOGGER.debug("No bridge with name: " + nameTag);
        return false;
    }

    protected CheckHealthAnswer execute(CheckHealthCommand cmd) {
        Common test = new Common(c);
        String ping = "put";
        String pong;
        try {
            pong = test.echo(ping);
        } catch (Ovm3ResourceException e) {
            LOGGER.debug("CheckHealth went wrong: " + agentName + ", "
                    + e.getMessage(), e);
            return new CheckHealthAnswer(cmd, false);
        }
        if (ping.contentEquals(pong)) {
            return new CheckHealthAnswer(cmd, true);
        }
        LOGGER.debug("CheckHealth did not receive " + ping + " but got "
                + pong + " from " + agentName);
        return new CheckHealthAnswer(cmd, false);
    }

    @Override
    public Answer executeRequest(Command cmd) {
        Class<? extends Command> clazz = cmd.getClass();
        if (cmd instanceof NetworkElementCommand) {
            return vrResource.executeRequest((NetworkElementCommand) cmd);
        } else if (clazz == CheckHealthCommand.class) {
            return execute((CheckHealthCommand) cmd);
        } else if (clazz == NetworkUsageCommand.class) {
            return execute((NetworkUsageCommand) cmd);
        } else if (clazz == ReadyCommand.class) {
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
        /* To change body of implemented methods use File | Settings | File Templates. */
        return null;
    }

    @Override
    public Answer backupSnapshot(CopyCommand cmd) {
        return new Answer(cmd);
    }

    /*
     * This can go I think.
     */
    protected Answer execute(AttachIsoCommand cmd) {
        try {
            LOGGER.debug(cmd.getStoreUrl() + " " + cmd.getIsoPath());
            return new Answer(cmd);
        } catch (Exception e) {
            LOGGER.debug(
                    "Attach or detach ISO " + cmd.getIsoPath() + " for "
                            + cmd.getVmName() + " attach:" + cmd.isAttach()
                            + " failed", e);
            return new Answer(cmd, false, e.getMessage());
        }
    }

    @Override
    public Answer attachIso(AttachCommand cmd) {
        LOGGER.debug("attachIso not implemented yet");
        return new Answer(cmd);
    }

    @Override
    public Answer dettachIso(DettachCommand cmd) {
        LOGGER.debug("detachIso not implemented yet");
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
                msg = doThis + " only NFS is supported at the moment.";
                return msg;
            }
            NfsTO nfsStore = (NfsTO) store;
            String secPoolUuid = setupSecondaryStorage(nfsStore.getUrl());
            String isoPath = agentSecStoragePath + File.separator + secPoolUuid
                    + File.separator + isoTO.getPath();
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
            xen.configureVm(ovmObject.deDash(vm.getPrimaryPoolUuid()),
                    vm.getVmUuid());
            return msg;
        } catch (Ovm3ResourceException e) {
            msg = doThis + " failed for " + vmName + " " + e.getMessage();
            LOGGER.info(msg, e);
            return msg;
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
    }

    /*
     * for now we assume that the datastore is there or else we never came this
     * far
     */
    public Answer execute(CreateObjectCommand cmd) {
        DataTO data = cmd.getData();
        if (data.getObjectType() == DataObjectType.VOLUME) {
            return createVolume(cmd);
        } else if (data.getObjectType() == DataObjectType.SNAPSHOT) {
            /*
             * if stopped yes, if runniing ... no, unless we have ocfs2 when
             * using raw partitions (file:) if using tap:aio we cloud...
             */
            LOGGER.debug("Snapshot object creation not supported.");
        } else if (data.getObjectType() == DataObjectType.TEMPLATE) {
            LOGGER.debug("Template object creation not supported.");
        }
        return new CreateObjectAnswer(data.getObjectType() + " object creation not supported");
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
            String file = this.agentOvmRepoPath + "/" + ovmObject.deDash(poolUuid)
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
        } catch (Ovm3ResourceException  | URISyntaxException e) {
            LOGGER.info("Volume creation failed: " + e.toString(), e);
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
            String path = volume.getPath();
            if (!path.contains(volume.getUuid())) {
                path = volume.getPath() + "/" + volume.getUuid() + ".raw";
            }
            StoragePlugin sp = new StoragePlugin(c);
            sp.storagePluginDestroy(poolUuid, path);
            LOGGER.debug("Volume deletion success: " + path);
        } catch (Ovm3ResourceException  e) {
            LOGGER.info("Volume deletion failed: " + e.toString(), e);
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

    /* we don't need this as we use the agent */
    @Override
    protected String getDefaultScriptsDir() {
        return null;
    }

}
