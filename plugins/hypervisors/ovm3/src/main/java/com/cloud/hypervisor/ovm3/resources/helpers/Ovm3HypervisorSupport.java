package com.cloud.hypervisor.ovm3.resources.helpers;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckHealthAnswer;
import com.cloud.agent.api.CheckHealthCommand;
import com.cloud.agent.api.CheckVirtualMachineAnswer;
import com.cloud.agent.api.CheckVirtualMachineCommand;
import com.cloud.agent.api.FenceAnswer;
import com.cloud.agent.api.FenceCommand;
import com.cloud.agent.api.GetHostStatsAnswer;
import com.cloud.agent.api.GetHostStatsCommand;
import com.cloud.agent.api.HostStatsEntry;
import com.cloud.agent.api.HostVmStateReportEntry;
import com.cloud.agent.api.MaintainAnswer;
import com.cloud.agent.api.MaintainCommand;
import com.cloud.agent.api.PlugNicAnswer;
import com.cloud.agent.api.PlugNicCommand;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.agent.api.UnPlugNicAnswer;
import com.cloud.agent.api.UnPlugNicCommand;
import com.cloud.agent.api.to.NicTO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.ovm3.objects.CloudStackPlugin;
import com.cloud.hypervisor.ovm3.objects.Common;
import com.cloud.hypervisor.ovm3.objects.Connection;
import com.cloud.hypervisor.ovm3.objects.Linux;
import com.cloud.hypervisor.ovm3.objects.Network;
import com.cloud.hypervisor.ovm3.objects.Ovm3ResourceException;
import com.cloud.hypervisor.ovm3.objects.OvmObject;
import com.cloud.hypervisor.ovm3.objects.Pool;
import com.cloud.hypervisor.ovm3.objects.Xen;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.script.Script;
import com.cloud.utils.ssh.SSHCmdHelper;
import com.cloud.vm.VirtualMachine.PowerState;
import com.cloud.vm.VirtualMachine.State;
import com.trilead.ssh2.SCPClient;

public class Ovm3HypervisorSupport {
    private static final Logger LOGGER = Logger
            .getLogger(Ovm3HypervisorSupport.class);
    private static Connection c;
    private OvmObject ovmObject = new OvmObject();
    public Ovm3HypervisorSupport(Connection conn) {
        c = conn;
    }
    private static Map<String, PowerState> powerStateMaps;
    static {
        powerStateMaps = new HashMap<String, PowerState>();
        powerStateMaps.put("Stopping", PowerState.PowerOn);
        powerStateMaps.put("Running", PowerState.PowerOn);
        powerStateMaps.put("Stopped", PowerState.PowerOff);
        powerStateMaps.put("Error", PowerState.PowerUnknown);
        powerStateMaps.put("Suspended", PowerState.PowerOn);
        powerStateMaps.put("Paused", PowerState.PowerOn);
        /* unknown ? */
        powerStateMaps.put("Migrating", PowerState.PowerOn);
    }

    /**
     * ValidateParam: Validate the input for configure
     * @param name
     * @param param
     * @return param
     * @throws ConfigurationException
     */
    public String validateParam(String name, String param) throws ConfigurationException {
        if (param == null) {
            String msg = "Unable to get " + name + " param is:" + param;
            LOGGER.debug(msg);
            throw new ConfigurationException(msg);
        }
        return param;
    }
    /**
     * validatePoolAndCluster:
     * A cluster is impossible with a  pool.
     * A pool is impossible without a vip.
     */
    public static void validatePoolAndCluster() {
        if (agentInOvm3Cluster) {
            LOGGER.debug("Clustering requires a pool, setting pool to true");
            agentInOvm3Pool = true;
        }
        if (!NetUtils.isValidIp(ovm3PoolVip)) {
            LOGGER.debug("No VIP, Setting ovm3pool and ovm3cluster to false");
            agentInOvm3Pool = false;
            agentInOvm3Cluster = false;
            ovm3PoolVip = "";
        }
    }
    /**
     * getSystemVMKeyFile:
     * Figure out where the cloud  keyfile lives for access to the systemvm.
     * @param filename
     * @return keyfileURI
     */
    public File getSystemVMKeyFile(String filename) {
        URL url = this.getClass().getClassLoader()
                .getResource("scripts/vm/systemvm/" + filename);
        File keyFile = null;
        if (url != null) {
            keyFile = new File(url.getPath());
        }
        if (keyFile == null || !keyFile.exists()) {
            keyFile = new File(
                    "/usr/share/cloudstack-common/scripts/vm/systemvm/"
                            + filename);
        }
        assert keyFile != null;
        if (!keyFile.exists()) {
            LOGGER.error("Unable to locate " + filename
                    + " in your setup at " + keyFile.toString());
        }
        return keyFile;
    }
    /**
     * fillHostInfo: Startup the routing for the host.
     * @param cmd
     */
    public static void fillHostInfo(StartupRoutingCommand cmd) {
        try {
            /* get data we need from parts */
            Linux host = new Linux(c);
            if (!host.getOvmVersion().startsWith("3.2.")) {
                throw new CloudRuntimeException(
                        "OVM 3.2.X is only supported, not "
                                + host.getOvmVersion());
            }
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
            cmd.setCaps(host.getCapabilities());
            // TODO: Control ip, for now cheat ?
            cmd.setPrivateIpAddress(c.getIp());
            cmd.setStorageIpAddress(c.getIp());
            /* do we need the state report here now ? */
            // cmd.setHostVmStateReport(hostVmStateReport());

            Network net = new Network(c);
            String defaultBridge = net.getBridgeByIp(c.getIp()).getName();
            if (defaultBridge == null) {
                throw new CloudRuntimeException(
                        "Unable to obtain valid bridge with " + c.getIp());
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
            d.put("ismaster", agentIsMaster.toString());
            d.put("hasmaster", agentHasMaster.toString());
            cmd.setHostDetails(d);
            LOGGER.debug("Add an Ovm3 host " + c.getHostname() + ":"
                    + cmd.getHostDetails());
        } catch (Ovm3ResourceException e) {
            throw new CloudRuntimeException("Ovm3ResourceException: "
                    + e.getMessage(), e);
        }
    }
    /**
     * setupServer:
     * Add the cloudstack plugin and setup the agent.
     * Add the ssh keys to the host.
     * @param c
     * @throws IOException
     */
    public void setupServer(String key) throws IOException {
        /* ssh-copy-id anyone ? */
        /* version dependent patching ? */
        try {
            /*
             * Do an agent check first, so we don't have to ssh, upload over the
             * agent and restart if possible
             */
            com.trilead.ssh2.Connection sshConnection = SSHCmdHelper
                    .acquireAuthorizedConnection(c.getIp(), c.getSshUser(),
                            c.getSshPassword());
            if (sshConnection == null) {
                throw new ConfigurationException(String.format("Unable to "
                        + "connect to server(IP=%1$s, username=%2$s, "
                        + "password=%3$s", c.getIp(), c.getSshUser(),
                        c.getSshPassword()));
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
                    + c.getUseSsl() + " " + "--port=" + c.getPort());
            if (!SSHCmdHelper.sshExecuteCmd(sshConnection, prepareCmd)) {
                throw new ConfigurationException("Module insertion at "
                        + c.getHostname() + " failed");
            }
            CloudStackPlugin cSp = new CloudStackPlugin(c);
            cSp.ovsUploadSshKey(c.getSshUser(),
                    FileUtils.readFileToString(getSystemVMKeyFile(key)));
        } catch (Exception es) {
            LOGGER.error("Unexpected exception ", es);
            String msg = "Unable to install module in agent";
            throw new CloudRuntimeException(msg);
        }
    }
   /**
    * Get all the VMs
    * @return
    * @throws Ovm3ResourceException
    */
    private static Map<String, Xen.Vm> getAllVms() throws Ovm3ResourceException {
        try {
            Xen vms = new Xen(c);
            return vms.getRunningVmConfigs();
        } catch (Exception e) {
            LOGGER.debug("getting VM list from " + c.getHostname() + " failed", e);
            throw new CloudRuntimeException("Exception on getting VMs from "
                    + c.getHostname() + ":" + e.getMessage(), e);
        }
    }

    /**
     * getAllVmStates: Get the state of all the VMs
     * @return
     * @throws Ovm3ResourceException
     */
    private static Map<String, State> getAllVmStates(Map<String, State> vmStateMap) throws Ovm3ResourceException {
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
                if (vmStateMap.get(vm.getVmName()) == State.Migrating) {
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
    /**
     * syncState: Sync the state the VMs are in on the hypervisor.
     * @return
     * @throws Ovm3ResourceException
     */
    public static Map<String, State> syncState(Map<String, State> vmStateMap) throws Ovm3ResourceException {
        Map<String, State> newStates;
        Map<String, State> oldStates = null;
        final Map<String, State> changes = new HashMap<String, State>();
        try {
            newStates = getAllVmStates(vmStateMap);
        } catch (Ovm3ResourceException e) {
            LOGGER.error("Ovm3 full sync failed: ", e);
            throw e;
        }
        synchronized (vmStateMap) {
            oldStates = new HashMap<String, State>(vmStateMap.size());
            oldStates.putAll(vmStateMap);

            for (final Map.Entry<String, State> entry : newStates.entrySet()) {
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
                    LOGGER.trace("VM " + vmName + ": ovm has state " + newState
                            + " and we have state "
                            + (oldState != null ? oldState.toString() : "null"));
                }

                if (newState == State.Migrating) {
                    LOGGER.trace(vmName + " is migrating, skipping state check");
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

            for (final Map.Entry<String, State> entry : oldStates.entrySet()) {
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
                     * do something smarter here.. newstate should say stopping
                     * already
                     */
                    LOGGER.debug("Ignoring VM " + vmName
                            + " in migrating state.");
                } else {
                    /* if it's not there name it stopping */
                    State state = State.Stopping;
                    LOGGER.trace("VM " + vmName
                            + " is now missing from ovm3 server so removing it");
                    changes.put(vmName, state);
                    vmStateMap.remove(vmName);
                    vmStateMap.put(vmName, state);
                }
            }
        }
        return changes;
    }

    /**
     * convertStateToPower: Convert a state, running, starting, stopped etc to a power state.
     * @param s
     * @param powerStateMap
     * @return
     */
    public static PowerState convertStateToPower(State s) {
        final PowerState state = powerStateMaps.get(s.toString());
        return state == null ? PowerState.PowerUnknown : state;
    }

    /**
     * hostVmStateReport: Get all the VM states.
     * @return
     * @throws Ovm3ResourceException
     */
    public Map<String, HostVmStateReportEntry> hostVmStateReport()
            throws Ovm3ResourceException {
        final Map<String, HostVmStateReportEntry> vmStates = new HashMap<String, HostVmStateReportEntry>();
        for (final Map.Entry<String, State> vm : vmStateMap.entrySet()) {
            LOGGER.debug("VM " + vm.getKey() + " state: " + vm.getValue() + ":"
                    + convertStateToPower(vm.getValue()));
            vmStates.put(vm.getKey(), new HostVmStateReportEntry(
                    convertStateToPower(vm.getValue()), c.getIp()));
        }
        return vmStates;
    }
    /**
     * PlugNicAnswer: plug a network interface into a VM
     * @param cmd
     * @return
     */
    private PlugNicAnswer execute(PlugNicCommand cmd) {
        String vmName = cmd.getVmName();
        NicTO nic = cmd.getNic();
        try {
            Xen xen = new Xen(c);
            Xen.Vm vm = xen.getVmConfig(vmName);
            /* check running */
            if (vm == null) {
                return new PlugNicAnswer(cmd, false,
                        "Unable to execute PlugNicCommand due to missing VM");
            }
            // setup the NIC in the VM config.
            createVif(vm, nic);
            vm.setupVifs();

            // execute the change
            xen.configureVm(ovmObject.deDash(vm.getPrimaryPoolUuid()),
                    vm.getVmUuid());
        } catch (Ovm3ResourceException e) {
            return new PlugNicAnswer(cmd, false,
                    "Unable to execute PlugNicCommand due to " + e.toString());
        }
        return new PlugNicAnswer(cmd, true, "success");
    }

    /**
     * UnPlugNicAnswer: remove a nic from a VM
     * @param cmd
     * @return
     */
    private UnPlugNicAnswer execute(UnPlugNicCommand cmd) {
        return new UnPlugNicAnswer(cmd, true, "");
    }
    /**
     * CheckHealthAnwer: Check the health of an agent on the hypervisor.
     * @param cmd
     * @return
     */
    private CheckHealthAnswer execute(CheckHealthCommand cmd) {
        Common test = new Common(c);
        String ping = "put";
        String pong;
        try {
            pong = test.echo(ping);
        } catch (Ovm3ResourceException e) {
            LOGGER.debug(
                    "CheckHealth went wrong: " + c.getHostname() + ", "
                            + e.getMessage(), e);
            return new CheckHealthAnswer(cmd, false);
        }
        if (ping.contentEquals(pong)) {
            return new CheckHealthAnswer(cmd, true);
        }
        LOGGER.debug("CheckHealth did not receive " + ping + " but got " + pong
                + " from " + c.getHostname());
        return new CheckHealthAnswer(cmd, false);
    }
    /**
     * materCheck
     * @return
     */
    /* TODO: move the connection elsewhere.... */
    public boolean masterCheck() {
        if ("".equals(ovm3PoolVip)) {
            LOGGER.debug("No cluster vip, not checking for master");
            return false;
        }

        try {
            CloudStackPlugin cSp = new CloudStackPlugin(c);
            if (cSp.dom0HasIp(ovm3PoolVip)) {
                LOGGER.debug("Host " + agentHostname
                        + " is a master, already has vip " + ovm3PoolVip);
                agentIsMaster = true;
            } else if (cSp.ping(ovm3PoolVip)) {
                LOGGER.debug("Host " + agentHostname
                        + " has a master, someone has vip " + ovm3PoolVip);
                agentHasMaster = true;
            } else {
                LOGGER.debug("Host " + agentHostname
                        + " becomes a master, no one has vip " + ovm3PoolVip);
                agentIsMaster = true;
            }
        } catch (Ovm3ResourceException e) {
            LOGGER.debug("Host " + agentHostname + " can't reach master: "
                    + e.getMessage());
            agentHasMaster = false;
        }
        return agentIsMaster;
    }
    /* Check if the host is in ready state for CS */
    private ReadyAnswer execute(ReadyCommand cmd) {
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
        } catch (CloudRuntimeException | Ovm3ResourceException e) {
            LOGGER.debug("XML RPC Exception" + e.getMessage(), e);
            throw new CloudRuntimeException("XML RPC Exception"
                    + e.getMessage(), e);
        }

    }
    /* hHeck "the" virtual machine */
    private CheckVirtualMachineAnswer execute(
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
            return new CheckVirtualMachineAnswer(cmd,
                    convertStateToPower(vmState), vncPort);
        } catch (Ovm3ResourceException e) {
            LOGGER.debug("Check migration for " + vmName + " failed", e);
            return new CheckVirtualMachineAnswer(cmd,
                    convertStateToPower(State.Stopped), null);
        }
    }

    private MaintainAnswer execute(MaintainCommand cmd) {
        /*
         * TODO: leave cluster, leave pool, release ownership, cleanout and
         * start over ?
         */
        try {
            Network net = new Network(c);
            net.stopOvsLocalConfig(agentControlNetworkName);
        } catch (Ovm3ResourceException e) {
            LOGGER.debug("unable to disable " + agentControlNetworkName, e);
        }
        return new MaintainAnswer(cmd);
    }
    private Answer execute(GetHostStatsCommand cmd) {
        try {
            CloudStackPlugin cSp = new CloudStackPlugin(c);
            Map<String, String> stats = cSp
                    .ovsDom0Stats(agentPublicNetworkName);
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
            LOGGER.debug("Get host stats of " + cmd.getHostName() + " failed",
                    e);
            return new Answer(cmd, false, e.getMessage());
        }
    }

    /*
     * TODO: no heartbeat if no cluster, should we add a heartbeat ?
     * cloudstack/plugins
     * /hypervisors/ovm/scripts/vm/hypervisor/ovm/OvmHostModule.py contains
     * fence
     */
    private FenceAnswer execute(FenceCommand cmd) {
        try {
            Boolean res = false;
            return new FenceAnswer(cmd, res, res.toString());
        } catch (Exception e) {
            LOGGER.error("fencing of  " + cmd.getHostIp() + " failed: ", e);
            return new FenceAnswer(cmd, false, e.getMessage());
        }
    }
}
