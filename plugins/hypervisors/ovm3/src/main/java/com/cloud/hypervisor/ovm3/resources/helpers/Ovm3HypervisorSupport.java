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

package com.cloud.hypervisor.ovm3.resources.helpers;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckHealthAnswer;
import com.cloud.agent.api.CheckHealthCommand;
import com.cloud.agent.api.CheckOnHostAnswer;
import com.cloud.agent.api.CheckOnHostCommand;
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
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.ovm3.objects.CloudstackPlugin;
import com.cloud.hypervisor.ovm3.objects.Common;
import com.cloud.hypervisor.ovm3.objects.Connection;
import com.cloud.hypervisor.ovm3.objects.Linux;
import com.cloud.hypervisor.ovm3.objects.Network;
import com.cloud.hypervisor.ovm3.objects.Ovm3ResourceException;
import com.cloud.hypervisor.ovm3.objects.Pool;
import com.cloud.hypervisor.ovm3.objects.Xen;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import com.cloud.utils.ssh.SSHCmdHelper;
import com.cloud.vm.VirtualMachine.PowerState;
import com.cloud.vm.VirtualMachine.State;
import com.trilead.ssh2.SCPClient;

public class Ovm3HypervisorSupport {
    private final Logger LOGGER = Logger.getLogger(Ovm3HypervisorSupport.class);
    private Connection c;
    private Ovm3Configuration config;

    public Ovm3HypervisorSupport(Connection conn, Ovm3Configuration ovm3config) {
        c = conn;
        config = ovm3config;
    }

    /* statemap bounces */
    private Map<String, PowerState> powerStateMaps;
    {
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
    private Map<String, State> vmStateMap = new HashMap<String, State>();
    private Map<String, State> stateMaps;
    {
        stateMaps = new HashMap<String, State>();
        stateMaps.put("Stopping", State.Stopping);
        stateMaps.put("Running", State.Running);
        stateMaps.put("Stopped", State.Stopped);
        stateMaps.put("Error", State.Error);
        stateMaps.put("Suspended", State.Running);
        stateMaps.put("Paused", State.Running);
        stateMaps.put("Migrating", State.Migrating);
    }

    /**
     * removeVmState: get rid of the state of a VM
     *
     * @param vmName
     */
    public void revmoveVmState(String vmName) {
        vmStateMap.remove(vmName);
    }

    /**
     * vmStateMapClear: clear out the statemap and repopulate it.
     *
     * @throws Ovm3ResourceException
     */
    public void vmStateMapClear() throws Ovm3ResourceException {
        synchronized (vmStateMap) {
            vmStateMap.clear();
            syncState(this.vmStateMap);
        }
    }

    /**
     * vmStateStarting: set the state of a vm to starting
     *
     * @param vmName
     */
    public void setVmStateStarting(String vmName) {
        setVmState(vmName, State.Starting);
    }

    /**
     * getVmState: get the state for a vm
     *
     * @param vmName
     * @return
     */
    public State getVmState(String vmName) {
        return vmStateMap.get(vmName);
    }

    /**
     * vmStateChange: set the state of a vm to state
     *
     * @param vmName
     * @param state
     */
    public void setVmState(String vmName, State state) {
        synchronized (vmStateMap) {
            vmStateMap.put(vmName, state);
        }
    }

    /**
     * getSystemVMKeyFile:
     * Figure out where the cloud keyfile lives for access to the systemvm.
     *
     * @param filename
     * @return keyfileURI
     */
    public File getSystemVMKeyFile(String filename) {
        String keyPath = Script.findScript("", "scripts/vm/systemvm/"
                + filename);
        File keyFile = null;
        if (keyPath != null) {
            LOGGER.debug("found SshKey " + keyPath);
            keyFile = new File(keyPath);
        }
        if (keyFile == null || !keyFile.exists()) {
            String key = "client/target/generated-webapp/WEB-INF/classes/scripts/vm/systemvm/"
                    + filename;
            LOGGER.warn("findScript failed, going for generated " + key);
            keyFile = new File(key);
        }
        if (keyFile == null || !keyFile.exists()) {
            String key = "/usr/share/cloudstack-common/scripts/vm/systemvm/"
                    + filename;
            LOGGER.warn("generated key retrieval failed " + key);
            keyFile = new File(key);
        }
        return keyFile;
    }

    /**
     * fillHostInfo: Startup the routing for the host.
     *
     * @param cmd
     */
    public void fillHostInfo(StartupRoutingCommand cmd) {
        try {
            /* get data we need from parts */
            Linux host = new Linux(c);
            if (!host.getOvmVersion().startsWith("3.2.") && !host.getOvmVersion().startsWith("3.3.")) {
                LOGGER.error("Hypervisor not supported: " + host.getOvmVersion());
                throw new CloudRuntimeException(
                        "OVM 3.2. or 3.3. are only supported, not "
                                + host.getOvmVersion());
            } else {
                LOGGER.debug("Hypervisor version: " + host.getOvmVersion());
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
            cmd.setGuid(config.getCsHostGuid());
            cmd.setDataCenter(config.getAgentZoneId().toString());
            cmd.setPod(config.getAgentPodId().toString());
            /* TODO: cmd.setOwner(host.getManagerUuid()); */
            cmd.setCluster(config.getAgentClusterId().toString());
            cmd.setHypervisorVersion(host.getOvmVersion());
            cmd.setVersion(host.getAgentVersion());
            cmd.setHypervisorType(HypervisorType.Ovm3);
            cmd.setCaps(host.getCapabilities());
            cmd.setPrivateIpAddress(c.getIp());
            cmd.setStorageIpAddress(c.getIp());
            Network net = new Network(c);
            String defaultBridge = net.getBridgeByIp(c.getIp()).getName();
            if (defaultBridge == null) {
                throw new CloudRuntimeException(
                        "Unable to obtain valid bridge with " + c.getIp());
            }

            if (config.getAgentPublicNetworkName() == null) {
                config.setAgentPublicNetworkName(defaultBridge);
            }
            if (config.getAgentPrivateNetworkName() == null) {
                config.setAgentPrivateNetworkName(config
                        .getAgentPublicNetworkName());
            }
            if (config.getAgentGuestNetworkName() == null) {
                config.setAgentGuestNetworkName(config
                        .getAgentPublicNetworkName());
            }
            if (config.getAgentStorageNetworkName() == null) {
                config.setAgentStorageNetworkName(config
                        .getAgentPrivateNetworkName());
            }
            Map<String, String> d = cmd.getHostDetails();
            d.put("public.network.device", config.getAgentPublicNetworkName());
            d.put("private.network.device", config.getAgentPrivateNetworkName());
            d.put("guest.network.device", config.getAgentGuestNetworkName());
            d.put("storage.network.device", config.getAgentStorageNetworkName());
            d.put("isprimary", config.getAgentIsPrimary().toString());
            d.put("hasprimary", config.getAgentHasPrimary().toString());
            cmd.setHostDetails(d);
            LOGGER.debug("Add an Ovm3 host " + config.getAgentHostname() + ":"
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
     *
     * @param c
     * @throws IOException
     */
    public Boolean setupServer(String key) throws IOException {
        LOGGER.debug("Setup all bits on agent: " + config.getAgentHostname());
        /* version dependent patching ? */
        try {
            com.trilead.ssh2.Connection sshConnection = SSHCmdHelper
                    .acquireAuthorizedConnection(config.getAgentIp(),
                            config.getAgentSshUserName(),
                            config.getAgentSshPassword());
            if (sshConnection == null) {
                throw new ConfigurationException(String.format("Unable to "
                        + "connect to server(IP=%1$s, username=%2$s, "
                        + "password=%3$s", config.getAgentIp(),
                        config.getAgentSshUserName(),
                        config.getAgentSshPassword()));
            }
            SCPClient scp = new SCPClient(sshConnection);
            String userDataScriptDir = "scripts/vm/hypervisor/ovm3/";
            String userDataScriptPath = Script.findScript("", userDataScriptDir);
            if (userDataScriptPath == null) {
                throw new ConfigurationException("Can not find "
                        + userDataScriptDir);
            }
            String mkdir = "mkdir -p " + config.getAgentScriptsDir();
            if (!SSHCmdHelper.sshExecuteCmd(sshConnection, mkdir)) {
                throw new ConfigurationException("Failed " + mkdir + " on "
                        + config.getAgentHostname());
            }
            for (String script : config.getAgentScripts()) {
                script = userDataScriptPath + "/" + script;
                scp.put(script, config.getAgentScriptsDir(), "0755");
            }
            String prepareCmd = String.format(config.getAgentScriptsDir() + "/"
                    + config.getAgentScript() + " --ssl=" + c.getUseSsl() + " "
                    + "--port=" + c.getPort());
            if (!SSHCmdHelper.sshExecuteCmd(sshConnection, prepareCmd)) {
                throw new ConfigurationException("Failed to insert module on "
                        + config.getAgentHostname());
            } else {
                /* because of OVM 3.3.1 (might be 2000) */
                Thread.sleep(5000);
            }
            CloudstackPlugin cSp = new CloudstackPlugin(c);
            cSp.ovsUploadSshKey(config.getAgentSshKeyFileName(),
                    FileUtils.readFileToString(getSystemVMKeyFile(key)));
            cSp.dom0CheckStorageHealthCheck(config.getAgentScriptsDir(),
                    config.getAgentCheckStorageScript(),
                    config.getCsHostGuid(),
                    config.getAgentStorageCheckTimeout(),
                    config.getAgentStorageCheckInterval());
        } catch (Exception es) {
            LOGGER.error("Unexpected exception ", es);
            String msg = "Unable to install module in agent";
            throw new CloudRuntimeException(msg);
        }
        return true;
    }

    /**
     * Get all the VMs
     *
     * @return
     * @throws Ovm3ResourceException
     */
    private Map<String, Xen.Vm> getAllVms() throws Ovm3ResourceException {
        try {
            Xen vms = new Xen(c);
            return vms.getRunningVmConfigs();
        } catch (Exception e) {
            LOGGER.debug("getting VM list from " + config.getAgentHostname()
                    + " failed", e);
            throw new CloudRuntimeException("Exception on getting VMs from "
                    + config.getAgentHostname() + ":" + e.getMessage(), e);
        }
    }

    /**
     * getAllVmStates: Get the state of all the VMs
     *
     * @return
     * @throws Ovm3ResourceException
     */
    private Map<String, State> getAllVmStates(Map<String, State> vmStateMap)
            throws Ovm3ResourceException {
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
     *
     * @return
     * @throws Ovm3ResourceException
     */
    public Map<String, State> syncState() throws Ovm3ResourceException {
        return syncState(this.vmStateMap);
    }

    private Map<String, State> syncState(Map<String, State> vmStateMap)
            throws Ovm3ResourceException {
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
                    LOGGER.debug("New state without old state: " + vmName);
                    changes.put(vmName, newState);
                } else if (oldState == State.Starting) {
                    if (newState == State.Running) {
                        vmStateMap.put(vmName, newState);
                    } else if (newState == State.Stopped) {
                        LOGGER.debug("Ignoring vm " + vmName
                                + " because of a lag in starting the vm.");
                    }
                } else if (oldState == State.Migrating) {
                    if (newState == State.Running) {
                        LOGGER.debug("Detected that a migrating VM is now running: "
                                + vmName);
                        vmStateMap.put(vmName, newState);
                    }
                } else if (oldState == State.Stopping) {
                    if (newState == State.Stopped) {
                        vmStateMap.put(vmName, newState);
                    } else if (newState == State.Running) {
                        LOGGER.debug("Ignoring vm " + vmName
                                + " because of a lag in stopping the vm. ");
                        /* should kill it hard perhaps ? */
                    }
                } else if (oldState != newState) {
                    vmStateMap.put(vmName, newState);
                    if (newState == State.Stopped) {
                        // For now leave it be.
                    }
                    changes.put(vmName, newState);
                }
            }

            for (final Map.Entry<String, State> entry : oldStates.entrySet()) {
                final String vmName = entry.getKey();
                final State oldState = entry.getValue();

                if (oldState == State.Stopping) {
                    LOGGER.debug("Removing VM " + vmName
                            + " in transition state stopping.");
                    vmStateMap.remove(vmName);
                } else if (oldState == State.Starting) {
                    LOGGER.debug("Removing VM " + vmName
                            + " in transition state starting.");
                    vmStateMap.remove(vmName);
                } else if (oldState == State.Stopped) {
                    LOGGER.debug("Stopped VM " + vmName + " removing.");
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
                    LOGGER.debug("VM " + vmName
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
     * convertStateToPower: Convert a state, running, starting, stopped etc to a
     * power state.
     *
     * @param s
     * @param powerStateMap
     * @return
     */
    private PowerState convertStateToPower(State s) {
        final PowerState state = powerStateMaps.get(s.toString());
        return state == null ? PowerState.PowerUnknown : state;
    }

    /**
     * hostVmStateReport: Get all the VM states.
     *
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
     * CheckHealthAnwer: Check the health of an agent on the hypervisor.
     * TODO: should elaborate here with checks...
     *
     * @param cmd
     * @return
     */
    public CheckHealthAnswer execute(CheckHealthCommand cmd) {
        Common test = new Common(c);
        String ping = "put";
        String pong;
        try {
            pong = test.echo(ping);
        } catch (Ovm3ResourceException e) {
            LOGGER.debug("CheckHealth went wrong: " + config.getAgentHostname()
                    + ", " + e.getMessage(), e);
            return new CheckHealthAnswer(cmd, false);
        }
        if (ping.contentEquals(pong)) {
            return new CheckHealthAnswer(cmd, true);
        }
        LOGGER.debug("CheckHealth did not receive " + ping + " but got " + pong
                + " from " + config.getAgentHostname());
        return new CheckHealthAnswer(cmd, false);
    }

    /**
     * primaryCheck
     *
     * @return
     */
    public boolean primaryCheck() {
        if ("".equals(config.getOvm3PoolVip())) {
            LOGGER.debug("No cluster vip, not checking for primary");
            return false;
        }

        try {
            CloudstackPlugin cSp = new CloudstackPlugin(c);
            if (cSp.dom0HasIp(config.getOvm3PoolVip())) {
                LOGGER.debug(config.getAgentHostname()
                        + " is a primary, already has vip "
                        + config.getOvm3PoolVip());
                config.setAgentIsPrimary(true);
            } else if (cSp.ping(config.getOvm3PoolVip())) {
                LOGGER.debug(config.getAgentHostname()
                        + " has a primary, someone has vip "
                        + config.getOvm3PoolVip());
                config.setAgentHasPrimary(true);
            } else {
                LOGGER.debug(config.getAgentHostname()
                        + " becomes a primary, no one has vip "
                        + config.getOvm3PoolVip());
                config.setAgentIsPrimary(true);
            }
        } catch (Ovm3ResourceException e) {
            LOGGER.debug(config.getAgentHostname()
                    + " can't reach primary: " + e.getMessage());
            config.setAgentHasPrimary(false);
        }
        return config.getAgentIsPrimary();
    }

    /* Check if the host is in ready state for CS */
    public ReadyAnswer execute(ReadyCommand cmd) {
        try {
            Linux host = new Linux(c);
            Pool pool = new Pool(c);

            /* only interesting when doing cluster */
            if (!host.getIsPrimary() && config.getAgentInOvm3Cluster()) {
                if (pool.getPoolPrimaryVip().equalsIgnoreCase(c.getIp())) {
                    /* check pool state here */
                    return new ReadyAnswer(cmd);
                } else {
                    LOGGER.debug("Primary IP changes to "
                            + pool.getPoolPrimaryVip() + ", it should be "
                            + c.getIp());
                    return new ReadyAnswer(cmd, "I am not the primary server");
                }
            } else if (host.getIsPrimary()) {
                LOGGER.debug("Primary, not clustered "
                        + config.getAgentHostname());
                return new ReadyAnswer(cmd);
            } else {
                LOGGER.debug("No primary, not clustered "
                        + config.getAgentHostname());
                return new ReadyAnswer(cmd);
            }
        } catch (CloudRuntimeException | Ovm3ResourceException e) {
            LOGGER.debug("XML RPC Exception" + e.getMessage(), e);
            throw new CloudRuntimeException("XML RPC Exception"
                    + e.getMessage(), e);
        }

    }

    /* check "the" virtual machine */
    public CheckVirtualMachineAnswer execute(
            final CheckVirtualMachineCommand cmd) {
        LOGGER.debug("CheckVirtualMachineCommand: " + cmd.getVmName());
        String vmName = cmd.getVmName();
        try {
            CloudstackPlugin plug = new CloudstackPlugin(c);
            Integer vncPort = Integer.valueOf(plug.getVncPort(vmName));
            if (vncPort == 0) {
                LOGGER.warn("No VNC port for " + vmName);
            }
            /* we already have the state ftw */
            Map<String, State> states = getAllVmStates(vmStateMap);
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

    /*
     * TODO: leave cluster, leave pool, release ownership, cleanout and
     * start over ?
     * For now leave it as we're not clustering in OVM terms.
     */
    public MaintainAnswer execute(MaintainCommand cmd) {
        LOGGER.debug("MaintainCommand");
        /*
         * try {
         * Network net = new Network(c);
         * net.stopOvsLocalConfig(config.getAgentControlNetworkName());
         * } catch (Ovm3ResourceException e) {
         * LOGGER.debug("unable to disable " +
         * config.getAgentControlNetworkName(), e);
         * }
         */
        return new MaintainAnswer(cmd);
    }

    public Answer execute(GetHostStatsCommand cmd) {
        try {
            CloudstackPlugin cSp = new CloudstackPlugin(c);
            Map<String, String> stats = cSp.ovsDom0Stats(config
                    .getAgentPublicNetworkName());
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
            LOGGER.debug("Unable to get host stats for: " + cmd.getHostName(),
                    e);
            return new Answer(cmd, false, e.getMessage());
        }
    }

    /*
     * We rely on storage health with CheckOnHostCommand....
     */
    public FenceAnswer execute(FenceCommand cmd) {
        LOGGER.debug("FenceCommand");
        try {
            Boolean res = false;
            return new FenceAnswer(cmd, res, res.toString());
        } catch (Exception e) {
            LOGGER.error("Unable to fence" + cmd.getHostIp(), e);
            return new FenceAnswer(cmd, false, e.getMessage());
        }
    }

    public CheckOnHostAnswer execute(CheckOnHostCommand cmd) {
        LOGGER.debug("CheckOnHostCommand");
        CloudstackPlugin csp = new CloudstackPlugin(c);
        try {
            Boolean alive = csp.dom0CheckStorageHealth(config.getAgentScriptsDir(),
                    config.getAgentCheckStorageScript(),
                    cmd.getHost().getGuid(),
                    config.getAgentStorageCheckTimeout());
            String msg = "";
            if (alive == null) {
                    msg = "storage check failed for " + cmd.getHost().getGuid();
            } else if (alive) {
                    msg = "storage check ok for " + cmd.getHost().getGuid();
            } else {
                    msg = "storage dead for " + cmd.getHost().getGuid();
            }
            LOGGER.debug(msg);
            return new CheckOnHostAnswer(cmd, alive, msg);
        } catch (Ovm3ResourceException e) {
            return new CheckOnHostAnswer(cmd, false, "Error while checking storage for " +cmd.getHost().getGuid() +": " + e.getMessage());
        }
    }
}
