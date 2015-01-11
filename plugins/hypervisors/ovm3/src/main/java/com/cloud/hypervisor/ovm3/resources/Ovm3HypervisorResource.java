/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http:www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/
package com.cloud.hypervisor.ovm3.resources;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.storage.command.CopyCommand;
import org.apache.cloudstack.storage.command.CreateObjectCommand;
import org.apache.cloudstack.storage.command.DeleteCommand;
import org.apache.log4j.Logger;

import com.cloud.agent.IAgentControl;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.AttachVolumeCommand;
import com.cloud.agent.api.CheckHealthCommand;
import com.cloud.agent.api.CheckNetworkCommand;
import com.cloud.agent.api.CheckVirtualMachineCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.CreatePrivateTemplateFromVolumeCommand;
import com.cloud.agent.api.CreateStoragePoolCommand;
import com.cloud.agent.api.DeleteStoragePoolCommand;
import com.cloud.agent.api.FenceCommand;
import com.cloud.agent.api.GetHostStatsCommand;
import com.cloud.agent.api.GetStorageStatsCommand;
import com.cloud.agent.api.GetVmStatsCommand;
import com.cloud.agent.api.GetVncPortCommand;
import com.cloud.agent.api.MaintainCommand;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.agent.api.ModifyStoragePoolCommand;
import com.cloud.agent.api.NetworkRulesSystemVmCommand;
import com.cloud.agent.api.NetworkUsageCommand;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.PingRoutingCommand;
import com.cloud.agent.api.PingTestCommand;
import com.cloud.agent.api.PlugNicCommand;
import com.cloud.agent.api.PrepareForMigrationCommand;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.RebootAnswer;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.StartAnswer;
import com.cloud.agent.api.StartCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.agent.api.StartupStorageCommand;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.UnPlugNicCommand;
import com.cloud.agent.api.check.CheckSshCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.storage.CopyVolumeCommand;
import com.cloud.agent.api.storage.CreateCommand;
import com.cloud.agent.api.storage.DestroyCommand;
import com.cloud.agent.api.storage.PrimaryStorageDownloadCommand;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.resource.virtualnetwork.VirtualRoutingResource;
import com.cloud.host.Host.Type;
import com.cloud.hypervisor.ovm3.objects.CloudStackPlugin;
import com.cloud.hypervisor.ovm3.objects.Common;
import com.cloud.hypervisor.ovm3.objects.Connection;
import com.cloud.hypervisor.ovm3.objects.Ovm3ResourceException;
import com.cloud.hypervisor.ovm3.objects.OvmObject;
import com.cloud.hypervisor.ovm3.objects.Xen;
import com.cloud.hypervisor.ovm3.resources.helpers.Ovm3Configuration;
import com.cloud.hypervisor.ovm3.resources.helpers.Ovm3HypervisorNetwork;
import com.cloud.hypervisor.ovm3.resources.helpers.Ovm3StoragePool;
import com.cloud.hypervisor.ovm3.resources.helpers.Ovm3VirtualRoutingSupport;
import com.cloud.hypervisor.ovm3.resources.helpers.Ovm3VmGuestTypes;
import com.cloud.hypervisor.ovm3.resources.helpers.Ovm3VmSupport;
import com.cloud.hypervisor.ovm3.resources.helpers.Ovm3HypervisorSupport;
import com.cloud.network.Networks.TrafficType;
import com.cloud.resource.ServerResourceBase;
import com.cloud.resource.hypervisor.HypervisorResource;
import com.cloud.template.VirtualMachineTemplate.BootloaderType;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;

/* This should only contain stuff that @Override(s) */
/*
 * update host set resource =
 * 'com.cloud.hypervisor.ovm3.resources.Ovm3HypervisorResource'
 * where resource = 'com.cloud.hypervisor.ovm3.hypervisor.Ovm3ResourceBase';
 */
@Local(value = HypervisorResource.class)
public class Ovm3HypervisorResource extends ServerResourceBase implements
        HypervisorResource {
    private final Logger LOGGER = Logger
            .getLogger(Ovm3HypervisorResource.class);
    @Inject
    private VirtualRoutingResource vrResource;

    private Connection c;
    private Ovm3StoragePool storagepool;
    private Ovm3StorageProcessor storageprocessor;
    private Ovm3HypervisorSupport hypervisorsupport;
    private Ovm3VmSupport vmsupport;
    private Ovm3HypervisorNetwork hypervisornetwork;
    private Ovm3VirtualRoutingResource virtualroutingresource;
    private Ovm3VirtualRoutingSupport virtualroutingsupport;
    private Ovm3Configuration configuration;
    private Ovm3VmGuestTypes guesttypes;
    private OvmObject ovmObject = new OvmObject();

    /*
     * TODO: Add a network map, so we know which tagged interfaces we can remove
     * and switch to ConcurrentHashMap
     */
    private Map<String, Xen.Vm> vmMap = new HashMap<String, Xen.Vm>();

    @Override
    public Type getType() {
        return Type.Routing;
    }

    /*
     * configure is called before this, does setup of the connection and
     * gets the params.
     *
     * @see com.cloud.resource.ServerResource#initialize()
     */
    @Override
    public StartupCommand[] initialize() {
        LOGGER.debug("Ovm3 resource intializing");
        try {
            StartupRoutingCommand srCmd = new StartupRoutingCommand();
            StartupStorageCommand ssCmd = new StartupStorageCommand();

            /* here stuff gets completed, but where should state live ? */
            hypervisorsupport.fillHostInfo(srCmd);
            hypervisorsupport.vmStateMapClear();
            if (configuration.getIsTest() == false) {
                hypervisorsupport.setupServer(configuration
                        .getAgentSshKeyFileName());
            }
            LOGGER.debug("Ovm3 pool " + ssCmd + " " + srCmd);
            // srCmd.setStateChanges(changes);
            return new StartupCommand[] { srCmd, ssCmd };
        } catch (Exception e) {
            LOGGER.debug("Ovm3 resource initializes failed", e);
            return new StartupCommand[] {};
        }
    }

    @Override
    public PingCommand getCurrentStatus(long id) {
        try {
            /* feels useless somehow */
            Common test = new Common(c);
            String ping = "put";
            String pong = test.echo(ping);
            if (pong.contains(ping)) {
                hypervisorsupport.syncState();
                CloudStackPlugin cSp = new CloudStackPlugin(c);
                if (!cSp.dom0CheckStorageHealth(configuration .getAgentScriptsDir(),
                        configuration.getAgentCheckStorageScript(),
                        configuration.getAgentStorageCheckTimeout(),
                        configuration.getAgentStorageCheckInterval())
                        && !cSp.dom0StorageCheckStarted()) {
                    LOGGER.error("Storage health check not running on "
                            + configuration.getAgentHostname());
                } else if (cSp.dom0StorageCheckStarted()) {
                    LOGGER.error("Storage health check started on "
                            + configuration.getAgentHostname());
                } else {
                    LOGGER.debug("Storage health check running on "
                            + configuration.getAgentHostname());
                }
                return new PingRoutingCommand(getType(), id,
                        hypervisorsupport.hostVmStateReport());
            } else {
                LOGGER.debug("Agent did not respond correctly: " + ping
                        + " but got " + pong);
            }

        } catch (Ovm3ResourceException | NullPointerException e) {
            LOGGER.debug("Check agent status failed", e);
            return null;
        }
        return null;
    }

    /* order sometime, or just get rid of... */
    @Override
    public Answer executeRequest(Command cmd) {
        Class<? extends Command> clazz = cmd.getClass();
        if (cmd instanceof NetworkElementCommand) {
            return vrResource.executeRequest((NetworkElementCommand) cmd);
        } else if (clazz == CheckHealthCommand.class) {
            return hypervisorsupport.execute((CheckHealthCommand) cmd);
        } else if (clazz == NetworkUsageCommand.class) {
            return virtualroutingsupport.execute((NetworkUsageCommand) cmd);
        } else if (clazz == PlugNicCommand.class) {
            return vmsupport.execute((PlugNicCommand) cmd);
        } else if (clazz == UnPlugNicCommand.class) {
            return vmsupport.execute((UnPlugNicCommand) cmd);
        } else if (clazz == ReadyCommand.class) {
            return hypervisorsupport.execute((ReadyCommand) cmd);
        } else if (clazz == CopyCommand.class) {
            return storageprocessor.execute((CopyCommand) cmd);
        } else if (clazz == DeleteCommand.class) {
            return storageprocessor.execute((DeleteCommand) cmd);
        } else if (clazz == CreateStoragePoolCommand.class) {
            return storagepool.execute((CreateStoragePoolCommand) cmd);
        } else if (clazz == ModifyStoragePoolCommand.class) {
            return storagepool.execute((ModifyStoragePoolCommand) cmd);
        } else if (clazz == PrimaryStorageDownloadCommand.class) {
            return storagepool.execute((PrimaryStorageDownloadCommand) cmd);
        } else if (clazz == CreateCommand.class) {
            return storageprocessor.execute((CreateCommand) cmd);
        } else if (clazz == GetHostStatsCommand.class) {
            return hypervisorsupport.execute((GetHostStatsCommand) cmd);
        } else if (clazz == StopCommand.class) {
            return execute((StopCommand) cmd);
        } else if (clazz == RebootCommand.class) {
            return execute((RebootCommand) cmd);
        } else if (clazz == GetStorageStatsCommand.class) {
            return storagepool.execute((GetStorageStatsCommand) cmd);
        } else if (clazz == GetVmStatsCommand.class) {
            return vmsupport.execute((GetVmStatsCommand) cmd);
        } else if (clazz == AttachVolumeCommand.class) {
            return vmsupport.execute((AttachVolumeCommand) cmd);
        } else if (clazz == DestroyCommand.class) {
            return storageprocessor.execute((DestroyCommand) cmd);
        } else if (clazz == PrepareForMigrationCommand.class) {
            return vmsupport.execute((PrepareForMigrationCommand) cmd);
        } else if (clazz == MigrateCommand.class) {
            return vmsupport.execute((MigrateCommand) cmd);
        } else if (clazz == CheckVirtualMachineCommand.class) {
            return hypervisorsupport.execute((CheckVirtualMachineCommand) cmd);
        } else if (clazz == MaintainCommand.class) {
            return hypervisorsupport.execute((MaintainCommand) cmd);
        } else if (clazz == GetVncPortCommand.class) {
            return vmsupport.execute((GetVncPortCommand) cmd);
        } else if (clazz == PingTestCommand.class) {
            return hypervisornetwork.execute((PingTestCommand) cmd);
        } else if (clazz == FenceCommand.class) {
            return hypervisorsupport.execute((FenceCommand) cmd);
            /*
             * } else if (clazz == AttachIsoCommand.class) {
             *
             * return ovm3spr.execute((attachIso) cmd);
             * } else if (clazz == DettachCommand.class) {
             * return ovm3spr.execute((DettachCommand) cmd);
             * } else if (clazz == AttachCommand.class) {
             * return ovm3spr.execute((AttachCommand) cmd);
             */
        } else if (clazz == NetworkRulesSystemVmCommand.class) {
            return virtualroutingsupport
                    .execute((NetworkRulesSystemVmCommand) cmd);
        } else if (clazz == CreatePrivateTemplateFromVolumeCommand.class) {
            return storageprocessor
                    .execute((CreatePrivateTemplateFromVolumeCommand) cmd);
        } else if (clazz == CopyVolumeCommand.class) {
            return storageprocessor.execute((CopyVolumeCommand) cmd);
        } else if (clazz == DeleteStoragePoolCommand.class) {
            return storagepool.execute((DeleteStoragePoolCommand) cmd);
        } else if (clazz == CheckNetworkCommand.class) {
            return hypervisornetwork.execute((CheckNetworkCommand) cmd);
        } else if (clazz == CheckSshCommand.class) {
            return virtualroutingsupport.execute((CheckSshCommand) cmd);
        } else if (clazz == CreateObjectCommand.class) {
            return storageprocessor.execute((CreateObjectCommand) cmd);
        } else if (clazz == StartCommand.class) {
            return execute((StartCommand) cmd);
        }
        LOGGER.debug("Can't find class for executeRequest, is your direct call missing?");
        return Answer.createUnsupportedCommandAnswer(cmd);
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
    public String getName() {
        return configuration.getAgentName();
    }

    @Override
    public void setName(String name) {
        configuration.setAgentName(name);
    }

    @Override
    public void setConfigParams(Map<String, Object> params) {
        configuration.setRawParams(params);
    }

    @Override
    public Map<String, Object> getConfigParams() {
        return configuration.getRawParams();
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

    /* TODO: fix this: Configure is the first thing called, later fillHostinfo */
    @Override
    public boolean configure(String name, Map<String, Object> params)
            throws ConfigurationException {
        LOGGER.debug("configure " + name + " with params: " + params);
        /* check if we're master or not and if we can connect */
        try {
            configuration = new Ovm3Configuration(params);
            if (configuration.getIsTest() == false) {
                c = new Connection(configuration.getAgentIp(),
                        configuration.getAgentOvsAgentPort(),
                        configuration.getAgentOvsAgentUser(),
                        configuration.getAgentOvsAgentPassword());
                c.setHostName(configuration.getAgentHostname());
            }

            hypervisorsupport = new Ovm3HypervisorSupport(c, configuration);
            hypervisorsupport.masterCheck();
        } catch (Exception e) {
            throw new CloudRuntimeException("Base checks failed for "
                    + configuration.getAgentHostname(), e);
        }
        hypervisornetwork = new Ovm3HypervisorNetwork(c, configuration);
        hypervisornetwork.configureNetworking();
        virtualroutingresource = new Ovm3VirtualRoutingResource(c);
        vmsupport = new Ovm3VmSupport(c, configuration, hypervisorsupport,
                storagepool, hypervisornetwork);
        vrResource = new VirtualRoutingResource(virtualroutingresource);
        if (!vrResource.configure(name, params)) {
            throw new ConfigurationException(
                    "Unable to configure VirtualRoutingResource");
        }
        storagepool = new Ovm3StoragePool(c, configuration);
        storagepool.prepareForPool();
        storageprocessor = new Ovm3StorageProcessor(c, configuration,
                storagepool);
        guesttypes = new Ovm3VmGuestTypes();
        virtualroutingsupport = new Ovm3VirtualRoutingSupport(c, configuration,
                virtualroutingresource);
        this.setConfigParams(params);
        return true;
    }

    public void setConnection(Connection con) {
        LOGGER.debug("override connection: " + con.getIp());
        c = con;
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
    public synchronized StartAnswer execute(StartCommand cmd) {
        VirtualMachineTO vmSpec = cmd.getVirtualMachine();
        String vmName = vmSpec.getName();
        State state = State.Stopped;
        Xen xen = new Xen(c);

        try {
            hypervisorsupport.setVmStateStarting(vmName);
            Xen.Vm vm = xen.getVmConfig();
            /* max and min ? */
            vm.setVmCpus(vmSpec.getCpus());
            /* in mb not in bytes */
            vm.setVmMemory(vmSpec.getMinRam() / 1024 / 1024);
            vm.setVmUuid(UUID.nameUUIDFromBytes(vmSpec.getName().getBytes())
                    .toString());
            vm.setVmName(vmName);

            String domType = guesttypes.getOvm3GuestType(vmSpec.getOs());
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
            vmsupport.createVbds(vm, vmSpec);

            if (vmSpec.getType() != VirtualMachine.Type.User) {
                String svmPath = configuration.getAgentOvmRepoPath() + "/"
                        + ovmObject.deDash(vm.getPrimaryPoolUuid()) + "/ISOs";
                String svmIso = svmPath + "/"
                        + storagepool.getSystemVMPatchIsoFile().getName();
                vm.addIso(svmIso);
            }
            /* TODO: OVS should go here! */
            vmsupport.createVifs(vm, vmSpec);
            vm.setupVifs();

            /* vm migration requires a 0.0.0.0 bind */
            vm.setVnc("0.0.0.0", vmSpec.getVncPassword());

            /* this should be getVmRootDiskPoolId ? */
            // System.out.println(vm.getPrimaryPoolUuid());
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
                    CloudStackPlugin cSp = new CloudStackPlugin(c);
                    if (hypervisorsupport.getVmState(vmName) == null) {
                        String msg = "VM " + vmName + " went missing on "
                                + configuration.getAgentHostname()
                                + ", returning stopped";
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
                        LOGGER.info(
                                "unable to connect to " + controlIp
                                        + " on attempt " + count + " "
                                        + x.getMessage(), x);
                    }
                    Thread.sleep(5000);
                }
            }
            /*
             * TODO: Can't remember if HA worked if we were only a pool ?
             */
            if (configuration.getAgentInOvm3Pool()
                    && configuration.getAgentInOvm3Cluster()) {
                xen.configureVmHa(ovmObject.deDash(vm.getPrimaryPoolUuid()),
                        vm.getVmUuid(), true);
            }
            /* should be starting no ? */
            state = State.Running;
            return new StartAnswer(cmd);
        } catch (Exception e) {
            LOGGER.debug("Start vm " + vmName + " failed", e);
            state = State.Stopped;
            return new StartAnswer(cmd, e.getMessage());
        } finally {
            hypervisorsupport.setVmState(vmName, state);
        }
    }

    /* TODO: Stop the VM, this means cleanup too, should this be destroy ? */
    @Override
    public StopAnswer execute(StopCommand cmd) {
        String vmName = cmd.getVmName();
        State state = State.Error;
        hypervisorsupport.setVmState(vmName, State.Stopping);

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
            vmsupport.cleanup(vm);

            if (vms.getRunningVmConfig(vmName) != null) {
                String msg = "Stop " + vmName + " failed ";
                LOGGER.debug(msg);
                return new StopAnswer(cmd, msg, false);
            }
            state = State.Stopped;
            return new StopAnswer(cmd, "success", true);
        } catch (Exception e) {
            LOGGER.debug("Stop " + vmName + " failed ", e);
            return new StopAnswer(cmd, e.getMessage(), false);
        } finally {
            if (state != null) {
                hypervisorsupport.setVmState(vmName, state);
            } else {
                hypervisorsupport.revmoveVmState(vmName);
            }
        }
    }

    @Override
    public RebootAnswer execute(RebootCommand cmd) {
        String vmName = cmd.getVmName();
        hypervisorsupport.setVmStateStarting(vmName);

        try {
            Xen xen = new Xen(c);
            Xen.Vm vm = xen.getRunningVmConfig(vmName);
            if (vm == null) {
                return new RebootAnswer(cmd, vmName + " not present", false);
            }
            xen.rebootVm(ovmObject.deDash(vm.getVmRootDiskPoolId()),
                    vm.getVmUuid());
            vm = xen.getRunningVmConfig(vmName);
            Integer vncPort = vm.getVncPort();
            return new RebootAnswer(cmd, null, vncPort);
        } catch (Exception e) {
            LOGGER.debug("Reboot " + vmName + " failed", e);
            return new RebootAnswer(cmd, e.getMessage(), false);
        } finally {
            hypervisorsupport.setVmState(vmName, State.Running);
        }
    }

    @Override
    protected String getDefaultScriptsDir() {
        // TODO Auto-generated method stub
        return null;
    }
}
