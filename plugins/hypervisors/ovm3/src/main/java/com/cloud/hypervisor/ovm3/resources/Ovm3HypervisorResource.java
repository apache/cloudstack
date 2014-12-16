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
import com.cloud.agent.api.AttachIsoCommand;
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
 * update host set resource = 'com.cloud.hypervisor.ovm3.resources.Ovm3HypervisorResource'
 * where resource = 'com.cloud.hypervisor.ovm3.hypervisor.Ovm3ResourceBase';
 */
@Local(value = HypervisorResource.class)
public class Ovm3HypervisorResource extends ServerResourceBase implements HypervisorResource {
    private final Logger LOGGER = Logger
            .getLogger(Ovm3HypervisorResource.class);
    @Inject
    private VirtualRoutingResource vrResource;

    private Connection c;
    private Ovm3StoragePool ovm3sp;
    private Ovm3StorageProcessor ovm3spr;
    private Ovm3HypervisorSupport ovm3hs;
    private Ovm3VmSupport ovm3vs;
    private Ovm3HypervisorNetwork ovm3hn;
    private Ovm3VirtualRoutingResource ovm3vrr;
    private Ovm3VirtualRoutingSupport ovm3vrs;
    private Ovm3Configuration ovm3config;
    private Ovm3VmGuestTypes ovm3gt;
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

    /* configure is called before this, does setup of the connection and
     * gets the params.
     * @see com.cloud.resource.ServerResource#initialize()
     */
    @Override
    public StartupCommand[] initialize() {
        LOGGER.debug("Ovm3 resource intializing");
        try {
            StartupRoutingCommand srCmd = new StartupRoutingCommand();
            StartupStorageCommand ssCmd = new StartupStorageCommand();

            /* here stuff gets completed, but where should state live ? */
            ovm3hs.fillHostInfo(srCmd);
            ovm3hs.vmStateMapClear();
            ovm3vs = new Ovm3VmSupport(c, ovm3config, ovm3hs, ovm3sp, ovm3hn);
            ovm3vrs = new Ovm3VirtualRoutingSupport(c, ovm3config, ovm3vrr);
            ovm3spr = new Ovm3StorageProcessor(c, ovm3config, ovm3sp);
            ovm3gt = new Ovm3VmGuestTypes();
            ovm3hs.setupServer(ovm3config.getAgentSshKey());
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
                ovm3hs.syncState();
                return new PingRoutingCommand(getType(), id,
                        ovm3hs.hostVmStateReport());
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

    @Override
    public Answer executeRequest(Command cmd) {
        Class<? extends Command> clazz = cmd.getClass();
        if (cmd instanceof NetworkElementCommand) {
            return vrResource.executeRequest((NetworkElementCommand) cmd);
        } else if (clazz == CheckHealthCommand.class) {
            return ovm3hs.execute((CheckHealthCommand) cmd);
        } else if (clazz == NetworkUsageCommand.class) {
            return ovm3vrs.execute((NetworkUsageCommand) cmd);
        } else if (clazz == PlugNicCommand.class) {
            return ovm3vs.execute((PlugNicCommand) cmd);
        } else if (clazz == UnPlugNicCommand.class) {
            return ovm3vs.execute((UnPlugNicCommand) cmd);
        } else if (clazz == ReadyCommand.class) {
            return ovm3hs.execute((ReadyCommand) cmd);
        } else if (clazz == CopyCommand.class) {
            return ovm3spr.execute((CopyCommand) cmd);
        } else if (clazz == DeleteCommand.class) {
            return ovm3spr.execute((DeleteCommand) cmd);
        } else if (clazz == CreateStoragePoolCommand.class) {
            return ovm3sp.execute((CreateStoragePoolCommand) cmd);
        } else if (clazz == ModifyStoragePoolCommand.class) {
            return ovm3sp.execute((ModifyStoragePoolCommand) cmd);
        } else if (clazz == PrimaryStorageDownloadCommand.class) {
            return ovm3sp.execute((PrimaryStorageDownloadCommand) cmd);
        } else if (clazz == CreateCommand.class) {
            return ovm3spr.execute((CreateCommand) cmd);
        } else if (clazz == GetHostStatsCommand.class) {
            return ovm3hs.execute((GetHostStatsCommand) cmd);
        } else if (clazz == StopCommand.class) {
            return execute((StopCommand) cmd);
        } else if (clazz == RebootCommand.class) {
            return execute((RebootCommand) cmd);
        } else if (clazz == GetStorageStatsCommand.class) {
            return ovm3sp.execute((GetStorageStatsCommand) cmd);
        } else if (clazz == GetVmStatsCommand.class) {
            return ovm3vs.execute((GetVmStatsCommand) cmd);
        } else if (clazz == AttachVolumeCommand.class) {
            return ovm3vs.execute((AttachVolumeCommand) cmd);
        } else if (clazz == DestroyCommand.class) {
            return ovm3spr.execute((DestroyCommand) cmd);
        } else if (clazz == PrepareForMigrationCommand.class) {
            return ovm3vs.execute((PrepareForMigrationCommand) cmd);
        } else if (clazz == MigrateCommand.class) {
            return ovm3vs.execute((MigrateCommand) cmd);
        } else if (clazz == CheckVirtualMachineCommand.class) {
            return ovm3hs.execute((CheckVirtualMachineCommand) cmd);
        /* } else if (clazz == MaintainCommand.class) {
            return ovm3hs.execute((MaintainCommand) cmd);
            */
        } else if (clazz == StartCommand.class) {
            return execute((StartCommand) cmd);
        } else if (clazz == GetVncPortCommand.class) {
            return ovm3vs.execute((GetVncPortCommand) cmd);
        } else if (clazz == PingTestCommand.class) {
            return ovm3hn.execute((PingTestCommand) cmd);
        } else if (clazz == FenceCommand.class) {
            return ovm3hs.execute((FenceCommand) cmd);
        } else if (clazz == AttachIsoCommand.class) {
            /*
            return ovm3spr.execute((attachIso) cmd);
        } else if (clazz == DettachCommand.class) {
            return ovm3spr.execute((DettachCommand) cmd);
        } else if (clazz == AttachCommand.class) {
            return ovm3spr.execute((AttachCommand) cmd);
            */
        } else if (clazz == NetworkRulesSystemVmCommand.class) {
            return ovm3vrs.execute((NetworkRulesSystemVmCommand) cmd);
        } else if (clazz == CreatePrivateTemplateFromVolumeCommand.class) {
            return ovm3spr.execute((CreatePrivateTemplateFromVolumeCommand) cmd);
        } else if (clazz == CopyVolumeCommand.class) {
            return ovm3spr.execute((CopyVolumeCommand) cmd);
        } else if (clazz == DeleteStoragePoolCommand.class) {
            return ovm3sp.execute((DeleteStoragePoolCommand) cmd);
        } else if (clazz == CheckNetworkCommand.class) {
            return ovm3hn.execute((CheckNetworkCommand) cmd);
        } else if (clazz == CheckSshCommand.class) {
            return ovm3vrs.execute((CheckSshCommand) cmd);
        } else if (clazz == CreateObjectCommand.class) {
            return ovm3spr.execute((CreateObjectCommand) cmd);
        }
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
        return ovm3config.getAgentName();
    }

    @Override
    public void setName(String name) {
        ovm3config.setAgentName(name);
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

    /* TODO: fix this: Configure is the first thing called, later fillHostinfo */
    @Override
    public boolean configure(String name, Map<String, Object> params)
            throws ConfigurationException {
        LOGGER.debug("configure " + name + " with params: " + params);
        ovm3config = new Ovm3Configuration(params);
        /* check if we're master or not and if we can connect */
        try {
            c = new Connection(ovm3config.getAgentIp(),
                    ovm3config.getAgentOvsAgentPort(),
                    ovm3config.getAgentOvsAgentUser(),
                    ovm3config.getAgentOvsAgentPassword());
            c.setHostName(ovm3config.getAgentHostname());
            ovm3hs = new Ovm3HypervisorSupport(c, ovm3config);
            ovm3hs.masterCheck();
        } catch (Exception e) {
            throw new CloudRuntimeException("Base checks failed for "
                    + ovm3config.getAgentHostname(), e);
        }
        ovm3hn = new Ovm3HypervisorNetwork(c, ovm3config);
        ovm3hn.configureNetworking();
        ovm3vrr = new Ovm3VirtualRoutingResource(c);
        vrResource = new VirtualRoutingResource(ovm3vrr);
        if (!vrResource.configure(name, params)) {
            throw new ConfigurationException(
                    "Unable to configure VirtualRoutingResource");
        }
        ovm3sp = new Ovm3StoragePool(c, ovm3config);
        ovm3sp.prepareForPool();
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
    public synchronized StartAnswer execute(StartCommand cmd) {
        VirtualMachineTO vmSpec = cmd.getVirtualMachine();
        String vmName = vmSpec.getName();
        State state = State.Stopped;
        Xen xen = new Xen(c);

        try {
            ovm3hs.setVmStateStarting(vmName);
            Xen.Vm vm = xen.getVmConfig();
            /* max and min ? */
            vm.setVmCpus(vmSpec.getCpus());
            /* in mb not in bytes */
            vm.setVmMemory(vmSpec.getMinRam() / 1024 / 1024);
            vm.setVmUuid(UUID.nameUUIDFromBytes(vmSpec.getName().getBytes())
                    .toString());
            vm.setVmName(vmName);

            String domType = ovm3gt.getOvm3GuestType(vmSpec.getOs());
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
            ovm3vs.createVbds(vm, vmSpec);

            if (vmSpec.getType() != VirtualMachine.Type.User) {
                String svmPath = ovm3config.getAgentOvmRepoPath() + "/"
                        + ovmObject.deDash(vm.getPrimaryPoolUuid()) + "/ISOs";
                String svmIso = svmPath + "/"
                        + ovm3sp.getSystemVMPatchIsoFile().getName();
                vm.addIso(svmIso);
            }
            /* TODO: OVS should go here! */
            ovm3vs.createVifs(vm, vmSpec);
            vm.setupVifs();

            /* vm migration requires a 0.0.0.0 bind */
            vm.setVnc("0.0.0.0", vmSpec.getVncPassword());

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
                    if (ovm3hs.getVmState(vmName) == null) {
                        String msg = "VM " + vmName + " went missing on "
                                + ovm3config.getAgentHostname() + ", returning stopped";
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
                }
            }
            /*
             * TODO: Can't remember if HA worked if we were only a pool ?
             */
            if (ovm3config.getAgentInOvm3Pool() && ovm3config.getAgentInOvm3Cluster()) {
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
            ovm3hs.setVmState(vmName, state);
        }
    }

    /* TODO: Stop the VM, this means cleanup too, should this be destroy ? */
    @Override
    public StopAnswer execute(StopCommand cmd) {
        String vmName = cmd.getVmName();
        State state = State.Error;
        ovm3hs.setVmState(vmName, State.Stopping);

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
            ovm3vs.cleanup(vm);

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
                ovm3hs.setVmState(vmName, state);
            } else {
                ovm3hs.revmoveVmState(vmName);
            }
        }
    }

    @Override
    public RebootAnswer execute(RebootCommand cmd) {
        String vmName = cmd.getVmName();
        ovm3hs.setVmStateStarting(vmName);

        try {
            Xen xen = new Xen(c);
            Xen.Vm vm = xen.getRunningVmConfig(vmName);
            xen.rebootVm(ovmObject.deDash(vm.getVmRootDiskPoolId()),
                    vm.getVmUuid());
            vm = xen.getRunningVmConfig(vmName);
            Integer vncPort = vm.getVncPort();
            return new RebootAnswer(cmd, null, vncPort);
        } catch (Exception e) {
            LOGGER.debug("Reboot " + vmName + " failed", e);
            return new RebootAnswer(cmd, e.getMessage(), false);
        } finally {
            ovm3hs.setVmState(vmName, State.Running);
        }
    }

    @Override
    protected String getDefaultScriptsDir() {
        // TODO Auto-generated method stub
        return null;
    }

}
