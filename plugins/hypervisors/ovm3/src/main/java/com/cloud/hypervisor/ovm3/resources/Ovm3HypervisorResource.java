package com.cloud.hypervisor.ovm3.resources;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.commons.lang.BooleanUtils;
import org.apache.log4j.Logger;

import com.cloud.agent.IAgentControl;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.PingRoutingCommand;
import com.cloud.agent.api.RebootAnswer;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.StartAnswer;
import com.cloud.agent.api.StartCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.agent.api.StartupStorageCommand;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.resource.virtualnetwork.VirtualRoutingResource;
import com.cloud.host.Host.Type;
import com.cloud.hypervisor.ovm3.objects.CloudStackPlugin;
import com.cloud.hypervisor.ovm3.objects.Common;
import com.cloud.hypervisor.ovm3.objects.Connection;
import com.cloud.hypervisor.ovm3.objects.Network;
import com.cloud.hypervisor.ovm3.objects.Ovm3ResourceException;
import com.cloud.hypervisor.ovm3.objects.OvmObject;
import com.cloud.hypervisor.ovm3.objects.Xen;
import com.cloud.hypervisor.ovm3.resources.helpers.Ovm3VmGuestTypes;
import com.cloud.hypervisor.ovm3.resources.helpers.Ovm3VmSupport;
import com.cloud.hypervisor.ovm3.resources.helpers.Ovm3HypervisorSupport;
import com.cloud.network.Networks.TrafficType;
import com.cloud.resource.ServerResourceBase;
import com.cloud.resource.hypervisor.HypervisorResource;
import com.cloud.template.VirtualMachineTemplate.BootloaderType;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;

/* This should only contain stuff that @Override(s) */
@Local(value = HypervisorResource.class)
public class Ovm3HypervisorResource extends ServerResourceBase implements HypervisorResource {
    private static final Logger LOGGER = Logger
            .getLogger(Ovm3HypervisorResource.class);
    private Connection c;
    private String agentName;
    private String agentIp;
    private Long agentZoneId;
    private Long agentPodId;
    private String agentPoolId;
    private Long agentClusterId;
    private String agentHostname;
    private String csGuid;
    private String agentSshUserName = "root";
    private String agentSshPassword;
    private String agentOvsAgentUser = "oracle";
    private String agentOvsAgentPassword;
    private Integer agentOvsAgentPort = 8899;
    private Boolean agentOvsAgentSsl = false;
    private String agentSshKey = "id_rsa.cloud";
    private String agentOwnedByUuid = "d1a749d4295041fb99854f52ea4dea97";
    private Boolean agentIsMaster = false;
    private Boolean agentHasMaster = false;
    private Boolean agentInOvm3Pool = false;
    private Boolean agentInOvm3Cluster = false;
    private String ovm3PoolVip = "";
    private String agentPrivateNetworkName;
    private String agentPublicNetworkName;
    private String agentGuestNetworkName;
    private String agentStorageNetworkName;
    private String agentControlNetworkName = "control0";
    private String agentOvmRepoPath = "/OVS/Repositories";
    private String agentSecStoragePath = "/nfsmnt";
    private OvmObject ovmObject = new OvmObject();
    private int domRSshPort = 3922;

    private Map<String, State> vmStateMap = new HashMap<String, State>();
    private static Map<String, State> s_stateMaps;
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
    @Inject

    private VirtualRoutingResource vrResource;
    private Map<String, Network.Interface> agentInterfaces = null;
    private final Map<String, Map<String, String>> vmStats = new ConcurrentHashMap<String, Map<String, String>>();

    /*
     * TODO: Add a network map, so we know which tagged interfaces we can remove
     * and switch to ConcurrentHashMap
     */
    private Map<String, Xen.Vm> vmMap = new HashMap<String, Xen.Vm>();


    @Override
    public Type getType() {
        return Type.Routing;
    }

    @Override
    public StartupCommand[] initialize() {
        LOGGER.debug("Ovm3 resource intializing");
        try {
            StartupRoutingCommand srCmd = new StartupRoutingCommand();
            StartupStorageCommand ssCmd = new StartupStorageCommand();
            Ovm3HypervisorSupport.fillHostInfo(srCmd);
            LOGGER.debug("Ovm3 pool " + ssCmd + " " + srCmd);

            synchronized (vmStateMap) {
                vmStateMap.clear();
                Ovm3HypervisorSupport.syncState(this.vmStateMap);
            }
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
                Ovm3HypervisorSupport.syncState(this.vmStateMap);
                return new PingRoutingCommand(getType(), id,
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

    @Override
    public Answer executeRequest(Command cmd) {
        Class<? extends Command> clazz = cmd.getClass();
        if (cmd instanceof NetworkElementCommand) {
            return vrResource.executeRequest((NetworkElementCommand) cmd);
        } else if (clazz == CheckHealthCommand.class) {
            return execute((CheckHealthCommand) cmd);
        } else if (clazz == NetworkUsageCommand.class) {
            return execute((NetworkUsageCommand) cmd);
        } else if (clazz == PlugNicCommand.class) {
            return execute((PlugNicCommand) cmd);
        } else if (clazz == UnPlugNicCommand.class) {
            return execute((UnPlugNicCommand) cmd);
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
    public String getName() {
        return agentName;
    }

    @Override
    public void setName(String name) {
        this.agentName = name;
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

    @Override
    public boolean configure(String name, Map<String, Object> params)
            throws ConfigurationException {
        String agentName = name;
        LOGGER.debug("configure " + name + " with params: " + params);
        agentZoneId = Long.parseLong((String) params.get("zone"));
        agentPodId = Long.parseLong(validateParam("PodId", (String) params.get("pod")));
        agentClusterId = Long.parseLong((String) params.get("cluster"));
        ovm3PoolVip = String.valueOf(params.get("ovm3vip"));
        agentInOvm3Pool = BooleanUtils.toBoolean((String) params
                .get("ovm3pool"));
        agentInOvm3Cluster = BooleanUtils.toBoolean((String) params
                .get("ovm3cluster"));
        agentHostname = validateParam("Hostname", (String) params.get("host"));
        agentIp = (String) params.get("ip");
        agentSshUserName = validateParam("Username", (String) params.get("username"));
        agentSshPassword = validateParam("Password", (String) params.get("password"));
        csGuid = validateParam("Cloudstack GUID", (String) params.get("guid"));
        agentOvsAgentUser = validateParam("OVS Username", (String) params.get("agentusername"));
        agentOvsAgentPassword = validateParam("OVS Password", (String) params.get("agentpassword"));
        agentPrivateNetworkName = (String) params.get("private.network.device");
        agentPublicNetworkName = (String) params.get("public.network.device");
        agentGuestNetworkName = (String) params.get("guest.network.device");
        agentStorageNetworkName = (String) params
                .get("storage.network.device1");

        if (params.get("agentport") != null) {
            agentOvsAgentPort = Integer.parseInt((String) params
                    .get("agentport"));
        }
        /* TODO: the agentssl parameter */
        Ovm3HypervisorSupport.validatePoolAndCluster();

        /* check if we're master or not and if we can connect */
        try {
            c = new Connection(agentHostname, agentOvsAgentPort,
                    agentOvsAgentUser, agentOvsAgentPassword);
            masterCheck();
        } catch (Exception e) {
            throw new CloudRuntimeException("Base checks failed for "
                    + agentHostname, e);
        }
        /* setup ovm3 agent plugin for cloudstack, our minion */


        try {
            /*
             * TODO: setup meta tags for the management interface (probably
             * required with multiple interfaces)?
             */
            Network net = new Network(c);
            agentInterfaces = net.getInterfaceList();
            if (agentControlNetworkName != null
                    && !agentInterfaces.containsKey(agentControlNetworkName)) {
                net.startOvsLocalConfig(agentControlNetworkName);
                /* ovs replies too "fast" so the bridge can be "busy" */
                int contCount = 0;
                while (!agentInterfaces.containsKey(agentControlNetworkName)) {
                    LOGGER.debug("waiting for " + agentControlNetworkName);
                    agentInterfaces = net.getInterfaceList();
                    Thread.sleep(1 * 1000);
                    if (contCount > 9) {
                        throw new ConfigurationException("Unable to configure "
                                + agentControlNetworkName + " on host "
                                + agentHostname);
                    }
                    contCount++;
                }
            }
            /*
             * The bridge is remembered upon reboot, but not the IP or the
             * config. Zeroconf also adds the route again by default.
             */
            net.ovsIpConfig(agentControlNetworkName, "static",
                    NetUtils.getLinkLocalGateway(),
                    NetUtils.getLinkLocalNetMask());
            CloudStackPlugin cSp = new CloudStackPlugin(c);
            cSp.ovsControlInterface(agentControlNetworkName,
                    NetUtils.getLinkLocalCIDR());

        } catch (InterruptedException e) {
            LOGGER.error("interrupted?", e);
        } catch (Ovm3ResourceException e) {
            String msg = "Basic configuration failed on " + agentHostname;
            LOGGER.error(msg, e);
            throw new ConfigurationException(msg + ", " + e.getMessage());
        }
        prepareForPool();

        vrResource = new Ovm3VirtualRoutingResource(this);
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
    public synchronized StartAnswer execute(StartCommand cmd) {
        VirtualMachineTO vmSpec = cmd.getVirtualMachine();
        String vmName = vmSpec.getName();
        State state = State.Stopped;
        Xen xen = new Xen(c);

        try {
            synchronized (vmStateMap) {
                vmStateMap.put(vmName, State.Starting);
            }
            Xen.Vm vm = xen.getVmConfig();
            /* max and min ? */
            vm.setVmCpus(vmSpec.getCpus());
            /* in mb not in bytes */
            vm.setVmMemory(vmSpec.getMinRam() / 1024 / 1024);
            vm.setVmUuid(UUID.nameUUIDFromBytes(vmSpec.getName().getBytes())
                    .toString());
            vm.setVmName(vmName);
            String domType = Ovm3VmGuestTypes.getOvm3GuestType(vmSpec.getOs());
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
                        + getSystemVMPatchIsoFile().getName();
                vm.addIso(svmIso);
            }
            /* TODO: OVS should go here! */
            createVifs(vm, vmSpec);
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
            return new StartAnswer(cmd, e.getMessage());
        } finally {
            synchronized (vmStateMap) {
                vmStateMap.put(vmName, state);
            }
        }
    }

    /* TODO: Stop the VM, this means cleanup too, should this be destroy ? */
    @Override
    public StopAnswer execute(StopCommand cmd) {
        String vmName = cmd.getVmName();
        State state = State.Error;
        synchronized (vmStateMap) {
            state = vmStateMap.get(vmName);
            vmStateMap.put(vmName, State.Stopping);
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
            cleanup(vm);

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
            synchronized (vmStateMap) {
                if (state != null) {
                    vmStateMap.put(vmName, state);
                } else {
                    vmStateMap.remove(vmName);
                }
            }
        }
    }

    @Override
    public RebootAnswer execute(RebootCommand cmd) {
        String vmName = cmd.getVmName();

        synchronized (vmStateMap) {
            vmStateMap.put(vmName, State.Starting);
        }

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
            synchronized (vmStateMap) {
                vmStateMap.put(cmd.getVmName(), State.Running);
            }
        }
    }

    @Override
    protected String getDefaultScriptsDir() {
        // TODO Auto-generated method stub
        return null;
    }

}
