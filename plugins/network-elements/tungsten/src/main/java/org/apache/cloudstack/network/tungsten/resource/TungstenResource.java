package org.apache.cloudstack.network.tungsten.resource;

import com.cloud.agent.IAgentControl;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.MaintainAnswer;
import com.cloud.agent.api.MaintainCommand;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.host.Host;
import com.cloud.resource.ServerResource;
import com.cloud.utils.TungstenUtils;
import com.cloud.utils.component.ManagerBase;
import net.juniper.tungsten.api.ApiConnectorFactory;
import net.juniper.tungsten.api.ApiObjectBase;
import net.juniper.tungsten.api.types.FloatingIp;
import net.juniper.tungsten.api.types.FloatingIpPool;
import net.juniper.tungsten.api.types.InstanceIp;
import net.juniper.tungsten.api.types.LogicalRouter;
import net.juniper.tungsten.api.types.Project;
import net.juniper.tungsten.api.types.VirtualMachine;
import net.juniper.tungsten.api.types.VirtualMachineInterface;
import net.juniper.tungsten.api.types.VirtualNetwork;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.network.tungsten.agent.api.AddTungstenRouteCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ClearTungstenNetworkGatewayCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenFloatingIpCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenFloatingIpPoolCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenLogicalRouterCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenNetworkCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenVirtualMachineCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenFloatingIpCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenLogicalRouterCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenNetworkCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenVRouterPortCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenVmCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenVmInterfaceCommand;
import org.apache.cloudstack.network.tungsten.agent.api.GetTungstenPublicNetworkCommand;
import org.apache.cloudstack.network.tungsten.agent.api.SetTungstenNetworkGatewayCommand;
import org.apache.cloudstack.network.tungsten.agent.api.StartupTungstenCommand;
import org.apache.cloudstack.network.tungsten.agent.api.TungstenAnswer;
import org.apache.cloudstack.network.tungsten.service.TungstenApi;
import org.apache.cloudstack.network.tungsten.service.TungstenVRouterApi;
import org.apache.cloudstack.network.tungsten.vrouter.Gateway;
import org.apache.cloudstack.network.tungsten.vrouter.Port;
import org.apache.cloudstack.network.tungsten.vrouter.Subnet;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

@Component
public class TungstenResource extends ManagerBase implements ServerResource {

    private static final Logger s_logger = Logger.getLogger(TungstenResource.class);

    private String _name;
    private String _guid;
    private String _zoneId;
    private int _numRetries;
    private String _hostname;
    private String _port;

    private TungstenApi _tungstenApi;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {

        _name = (String) params.get("name");
        if (_name == null) {
            throw new ConfigurationException("Unable to find name");
        }

        _guid = (String) params.get("guid");
        if (_guid == null) {
            throw new ConfigurationException("Unable to find the guid");
        }

        _zoneId = (String) params.get("zoneId");
        if (_zoneId == null) {
            throw new ConfigurationException("Unable to find zone");
        }

        _numRetries = 2;

        _hostname = (String) params.get("hostname");
        if (_hostname == null) {
            throw new ConfigurationException("Missing tungsten hostname from params: " + params);
        }

        _port = (String) params.get("port");
        if (_port == null) {
            throw new ConfigurationException("Missing tungsten port from params: " + params);
        }

        _tungstenApi = new TungstenApi();
        _tungstenApi.setHostname(_hostname);
        _tungstenApi.setPort(_port);
        _tungstenApi.setZoneId(_zoneId);
        _tungstenApi.setApiConnector(ApiConnectorFactory.build(_hostname, Integer.parseInt(_port)));
        return true;
    }

    @Override
    public Host.Type getType() {
        return Host.Type.L2Networking;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public StartupCommand[] initialize() {
        StartupTungstenCommand sc = new StartupTungstenCommand();
        sc.setGuid(_guid);
        sc.setName(_name);
        sc.setDataCenter(_zoneId);
        sc.setPod("");
        sc.setPrivateIpAddress("");
        sc.setStorageIpAddress("");
        sc.setVersion("");
        return new StartupCommand[]{sc};
    }

    @Override
    public PingCommand getCurrentStatus(long id) {
        try {
            _tungstenApi.checkTungstenProviderConnection();
        } catch (ServerApiException e) {
            s_logger.error("Check tungsten provider connection failed", e);
            return null;
        }
        return new PingCommand(Host.Type.L2Networking, id);
    }

    @Override
    public Answer executeRequest(Command cmd) {
        return executeRequest(cmd, _numRetries);
    }

    public Answer executeRequest(Command cmd, int numRetries) {
        if (cmd instanceof ReadyCommand) {
            return executeRequest((ReadyCommand) cmd);
        } else if (cmd instanceof MaintainCommand) {
            return executeRequest((MaintainCommand) cmd);
        } else if (cmd instanceof CreateTungstenNetworkCommand) {
            return executeRequest((CreateTungstenNetworkCommand) cmd, numRetries);
        } else if (cmd instanceof DeleteTungstenNetworkCommand) {
            return executeRequest((DeleteTungstenNetworkCommand) cmd, numRetries);
        } else if (cmd instanceof DeleteTungstenVmInterfaceCommand) {
            return executeRequest((DeleteTungstenVmInterfaceCommand) cmd, numRetries);
        } else if (cmd instanceof DeleteTungstenVmCommand) {
            return executeRequest((DeleteTungstenVmCommand) cmd, numRetries);
        } else if (cmd instanceof CreateTungstenLogicalRouterCommand) {
            return executeRequest((CreateTungstenLogicalRouterCommand) cmd, numRetries);
        } else if (cmd instanceof CreateTungstenVirtualMachineCommand) {
            return executeRequest((CreateTungstenVirtualMachineCommand) cmd, numRetries);
        } else if (cmd instanceof SetTungstenNetworkGatewayCommand) {
            return executeRequest((SetTungstenNetworkGatewayCommand) cmd, numRetries);
        } else if (cmd instanceof DeleteTungstenLogicalRouterCommand) {
            return executeRequest((DeleteTungstenLogicalRouterCommand) cmd, numRetries);
        } else if (cmd instanceof DeleteTungstenVRouterPortCommand) {
            return executeRequest((DeleteTungstenVRouterPortCommand) cmd, numRetries);
        } else if (cmd instanceof AddTungstenRouteCommand) {
            return executeRequest((AddTungstenRouteCommand) cmd, numRetries);
        } else if (cmd instanceof GetTungstenPublicNetworkCommand) {
            return executeRequest((GetTungstenPublicNetworkCommand) cmd, numRetries);
        } else if (cmd instanceof ClearTungstenNetworkGatewayCommand) {
            return executeRequest((ClearTungstenNetworkGatewayCommand) cmd, numRetries);
        } else if (cmd instanceof CreateTungstenFloatingIpPoolCommand) {
            return executeRequest((CreateTungstenFloatingIpPoolCommand) cmd, numRetries);
        } else if (cmd instanceof CreateTungstenFloatingIpCommand) {
            return executeRequest((CreateTungstenFloatingIpCommand) cmd, numRetries);
        } else if (cmd instanceof DeleteTungstenFloatingIpCommand) {
            return executeRequest((DeleteTungstenFloatingIpCommand) cmd, numRetries);
        }
        s_logger.debug("Received unsupported command " + cmd.toString());
        return Answer.createUnsupportedCommandAnswer(cmd);
    }

    private Answer executeRequest(CreateTungstenNetworkCommand cmd, int numRetries) {
        Project project = (Project) _tungstenApi.getTungstenNetworkProject(cmd.getParent());
        if (project == null) {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }

        VirtualNetwork virtualNetwork = _tungstenApi.createTungstenNetwork(cmd.getUuid(), cmd.getName(),
            project.getUuid(), cmd.isRouterExternal(), cmd.isShared(), cmd.getIpPrefix(), cmd.getIpPrefixLen(),
            cmd.getGateway(), cmd.isDhcpEnable(), cmd.getDnsServers(), cmd.getAllocationStart(), cmd.getAllocationEnd(),
            cmd.isIpFromStart());

        if (virtualNetwork != null)
            return new TungstenAnswer(cmd, virtualNetwork, true, "Tungsten network created");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(DeleteTungstenVmInterfaceCommand cmd, int numRetries) {
        Project project = (Project) _tungstenApi.getTungstenNetworkProject(cmd.getProjectUuid());
        VirtualMachineInterface vmi = (VirtualMachineInterface) _tungstenApi.getTungstenObjectByName(
            VirtualMachineInterface.class, project.getQualifiedName(), cmd.getName());
        boolean deleted = _tungstenApi.deleteTungstenVmInterface(vmi);
        if (deleted)
            return new TungstenAnswer(cmd, true, "Tungsten virtual machine interface deleted");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(DeleteTungstenVmCommand cmd, int numRetries) {
        VirtualMachine virtualMachine = (VirtualMachine) _tungstenApi.getTungstenObject(VirtualMachine.class,
            cmd.getVirtualMachineUuid());
        boolean deleted = _tungstenApi.deleteTungstenVm(virtualMachine);
        if (deleted)
            return new TungstenAnswer(cmd, true, "Tungsten virtual machine deleted");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(DeleteTungstenNetworkCommand cmd, int numRetries) {
        VirtualNetwork virtualNetwork = (VirtualNetwork) _tungstenApi.getTungstenObject(VirtualNetwork.class,
            cmd.getNetworkUuid());
        if (virtualNetwork != null) {
            boolean deleted = _tungstenApi.deleteTungstenNetwork(virtualNetwork);
            if (deleted)
                return new TungstenAnswer(cmd, true, "Tungsten network deleted");
            else {
                if (numRetries > 0) {
                    return retry(cmd, --numRetries);
                } else {
                    return new TungstenAnswer(cmd, new IOException());
                }
            }
        }
        return new TungstenAnswer(cmd, true, "Tungsten network is not exist");
    }

    private Answer executeRequest(CreateTungstenLogicalRouterCommand cmd, int numRetries) {
        Project project = (Project) _tungstenApi.getTungstenNetworkProject(cmd.getParentUuid());
        LogicalRouter logicalRouter = (LogicalRouter) _tungstenApi.createTungstenLogicalRouter(cmd.getName(),
            project.getUuid(), cmd.getPubNetworkUuid());
        if (logicalRouter != null)
            return new TungstenAnswer(cmd, logicalRouter, true, "Tungsten logical router created");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(CreateTungstenVirtualMachineCommand cmd, int numRetries) {
        Project project = (Project) _tungstenApi.getTungstenNetworkProject(cmd.getProjectUuid());
        if (project == null) {
            return new TungstenAnswer(cmd, new IOException());
        }

        VirtualNetwork virtualNetwork = (VirtualNetwork) _tungstenApi.getTungstenObject(VirtualNetwork.class,
            cmd.getVnUuid());
        if (virtualNetwork == null) {
            return new TungstenAnswer(cmd, new IOException());
        }

        VirtualMachine virtualMachine = (VirtualMachine) _tungstenApi.getTungstenObject(VirtualMachine.class,
            cmd.getVmUuid());
        if (virtualMachine == null) {
            virtualMachine = _tungstenApi.createTungstenVirtualMachine(cmd.getVmUuid(), cmd.getVmName());
        }
        if (virtualMachine == null) {
            return new TungstenAnswer(cmd, new IOException());
        }

        String vmiName = TungstenUtils.getVmiName(cmd.getTrafficType(), cmd.getVmType(), cmd.getVmName(),
            cmd.getNicId());
        VirtualMachineInterface virtualMachineInterface =
            (VirtualMachineInterface) _tungstenApi.getTungstenObjectByName(
            VirtualMachineInterface.class, project.getQualifiedName(), vmiName);
        if (virtualMachineInterface == null) {
            virtualMachineInterface = _tungstenApi.createTungstenVmInterface(cmd.getNicUuid(), vmiName, cmd.getMac(),
                cmd.getVnUuid(), cmd.getVmUuid(), project.getUuid());
        }
        if (virtualMachineInterface == null) {
            return new TungstenAnswer(cmd, new IOException());
        }

        String iiName = TungstenUtils.getInstanceIpName(cmd.getTrafficType(), cmd.getVmType(), cmd.getVmName(),
            cmd.getNicId());
        InstanceIp instanceIp = (InstanceIp) _tungstenApi.getTungstenObjectByName(InstanceIp.class, null, iiName);
        if (instanceIp == null) {
            instanceIp = _tungstenApi.createTungstenInstanceIp(iiName, cmd.getIp(), cmd.getVnUuid(),
                virtualMachineInterface.getUuid());
        }
        if (instanceIp == null) {
            return new TungstenAnswer(cmd, new IOException());
        }

        Port port = new Port();
        port.setId(virtualMachineInterface.getUuid());
        port.setVnId(virtualNetwork.getUuid());
        port.setDisplayName(virtualMachine.getName());
        port.setVmProjectId(project.getUuid());
        port.setMacAddress(cmd.getMac());
        port.setIpAddress(cmd.getIp());
        port.setInstanceId(virtualMachine.getUuid());
        port.setTapInterfaceName(TungstenUtils.getTapName(cmd.getMac()));
        boolean addPortResult = TungstenVRouterApi.addTungstenVrouterPort(cmd.getHost(), port);
        if (!addPortResult) {
            return new TungstenAnswer(cmd, new IOException());
        }

        return new TungstenAnswer(cmd, virtualMachine, true, null);
    }

    private Answer executeRequest(SetTungstenNetworkGatewayCommand cmd, int numRetries) {
        Project project = (Project) _tungstenApi.getTungstenNetworkProject(cmd.getProjectUuid());
        if (project == null) {
            return new TungstenAnswer(cmd, new IOException());
        }

        LogicalRouter logicalRouter = (LogicalRouter) _tungstenApi.getTungstenObjectByName(LogicalRouter.class,
            project.getQualifiedName(), cmd.getRouterName());
        if (logicalRouter == null) {
            return new TungstenAnswer(cmd, new IOException());
        }

        VirtualMachineInterface virtualMachineInterface =
            (VirtualMachineInterface) _tungstenApi.getTungstenObjectByName(
            VirtualMachineInterface.class, project.getQualifiedName(),
            TungstenUtils.getNetworkGatewayVmiName(cmd.getVnId()));
        if (virtualMachineInterface == null) {
            virtualMachineInterface = (VirtualMachineInterface) _tungstenApi.createTungstenGatewayVmi(
                TungstenUtils.getNetworkGatewayVmiName(cmd.getVnId()), project.getUuid(), cmd.getVnUuid());
        }
        if (virtualMachineInterface == null) {
            return new TungstenAnswer(cmd, new IOException());
        }

        InstanceIp instanceIp = (InstanceIp) _tungstenApi.getTungstenObjectByName(InstanceIp.class, null,
            TungstenUtils.getNetworkGatewayIiName(cmd.getVnId()));
        if (instanceIp == null) {
            instanceIp = _tungstenApi.createTungstenInstanceIp(TungstenUtils.getNetworkGatewayIiName(cmd.getVnId()),
                cmd.getVnGatewayIp(), cmd.getVnUuid(), virtualMachineInterface.getUuid());
        }
        if (instanceIp == null) {
            return new TungstenAnswer(cmd, new IOException());
        }

        logicalRouter.addVirtualMachineInterface(virtualMachineInterface);
        boolean updateLRResult = _tungstenApi.updateTungstenObject(logicalRouter);
        if (!updateLRResult) {
            return new TungstenAnswer(cmd, new IOException());
        }

        return new TungstenAnswer(cmd, virtualMachineInterface, true, null);
    }

    private Answer executeRequest(DeleteTungstenLogicalRouterCommand cmd, int numRetries) {
        Project project = (Project) _tungstenApi.getTungstenNetworkProject(cmd.getProjectUuid());
        if (project == null) {
            return new TungstenAnswer(cmd, new IOException());
        }

        LogicalRouter logicalRouter = (LogicalRouter) _tungstenApi.getTungstenObjectByName(LogicalRouter.class,
            project.getQualifiedName(), TungstenUtils.getLogicalRouterName(cmd.getId()));
        if (logicalRouter != null) {
            boolean deteleResult = _tungstenApi.deleteTungstenLogicalRouter(logicalRouter);
            if (!deteleResult) {
                return new TungstenAnswer(cmd, new IOException());
            }
        }

        return new TungstenAnswer(cmd, true, null);
    }

    private Answer executeRequest(AddTungstenRouteCommand cmd, int numRetries) {
        List<Gateway> gateways = new ArrayList<>();
        Gateway gateway = new Gateway(cmd.getInf(), cmd.getVrf(), getListSubnet(cmd.getSubnetList()),
            getListSubnet(cmd.getRouteList()));
        gateways.add(gateway);
        boolean result = TungstenVRouterApi.addTungstenRoute(cmd.getHost(), gateways);
        return new TungstenAnswer(cmd, result, null);
    }

    private Answer executeRequest(GetTungstenPublicNetworkCommand cmd, int numRetries) {
        Project project = (Project) _tungstenApi.getTungstenNetworkProject(cmd.getParent());
        if (project == null) {
            return new TungstenAnswer(cmd, new IOException());
        }

        ApiObjectBase apiObjectBase = _tungstenApi.getTungstenObjectByName(VirtualNetwork.class,
            project.getQualifiedName(), cmd.getName());
        return new TungstenAnswer(cmd, apiObjectBase, true, null);
    }

    private Answer executeRequest(ClearTungstenNetworkGatewayCommand cmd, int numRetries) {
        Project project = (Project) _tungstenApi.getTungstenNetworkProject(cmd.getProjectUuid());
        if (project == null) {
            return new TungstenAnswer(cmd, new IOException());
        }

        LogicalRouter logicalRouter = (LogicalRouter) _tungstenApi.getTungstenObjectByName(LogicalRouter.class,
            project.getQualifiedName(), cmd.getRouterName());
        if (logicalRouter == null) {
            return new TungstenAnswer(cmd, new IOException());
        }

        VirtualMachineInterface virtualMachineInterface =
            (VirtualMachineInterface) _tungstenApi.getTungstenObjectByName(
            VirtualMachineInterface.class, project.getQualifiedName(),
            TungstenUtils.getNetworkGatewayVmiName(cmd.getVnId()));

        if (virtualMachineInterface != null) {
            logicalRouter.removeVirtualMachineInterface(virtualMachineInterface);

            boolean updateLRResult = _tungstenApi.updateTungstenObject(logicalRouter);
            if (!updateLRResult) {
                return new TungstenAnswer(cmd, new IOException());
            }

            boolean deleteVmi = _tungstenApi.deleteTungstenVmInterface(virtualMachineInterface);
            if (!deleteVmi) {
                return new TungstenAnswer(cmd, new IOException());
            }
        }

        return new TungstenAnswer(cmd, true, null);
    }

    private Answer executeRequest(CreateTungstenFloatingIpPoolCommand cmd, int numRetries) {
        ApiObjectBase apiObjectBase = _tungstenApi.createTungstenFloatingIpPool(cmd.getNetworkUuid(), cmd.getFipName());
        if (apiObjectBase != null) {
            return new TungstenAnswer(cmd, apiObjectBase, true, null);
        } else {
            return new TungstenAnswer(cmd, new IOException());
        }
    }

    private Answer executeRequest(CreateTungstenFloatingIpCommand cmd, int numRetries) {
        Project project = (Project) _tungstenApi.getTungstenNetworkProject(cmd.getProjectUuid());
        if (project == null) {
            return new TungstenAnswer(cmd, new IOException());
        }

        ApiObjectBase apiObjectBase = _tungstenApi.createTungstenFloatingIp(project.getUuid(), cmd.getNetworkUuid(),
            cmd.getVmiUuid(), cmd.getFipName(), cmd.getName(), cmd.getPublicIp(), cmd.getPrivateIp());
        if (apiObjectBase != null) {
            return new TungstenAnswer(cmd, apiObjectBase, true, null);
        } else {
            return new TungstenAnswer(cmd, new IOException());
        }
    }

    private Answer executeRequest(DeleteTungstenVRouterPortCommand cmd, int numRetries) {
        boolean deletePort = TungstenVRouterApi.deleteTungstenVrouterPort(cmd.getHost(), cmd.getPortId());
        return new TungstenAnswer(cmd, deletePort, null);
    }

    private Answer executeRequest(DeleteTungstenFloatingIpCommand cmd, int numRetries) {
        Project project = (Project) _tungstenApi.getTungstenNetworkProject(cmd.getProjectUuid());
        if (project == null) {
            return new TungstenAnswer(cmd, new IOException());
        }
        VirtualNetwork virtualNetwork = (VirtualNetwork) _tungstenApi.getTungstenObject(VirtualNetwork.class,
            cmd.getVnUuid());
        FloatingIpPool floatingIpPool = (FloatingIpPool) _tungstenApi.getTungstenObjectByName(FloatingIpPool.class,
            virtualNetwork.getQualifiedName(), cmd.getFipName());
        FloatingIp floatingIp = (FloatingIp) _tungstenApi.getTungstenObjectByName(FloatingIp.class,
            floatingIpPool.getQualifiedName(), cmd.getName());
        boolean deleteFip = _tungstenApi.deleteTungstenFloatingIp(floatingIp);
        return new TungstenAnswer(cmd, deleteFip, null);
    }

    private Answer retry(Command cmd, int numRetries) {
        s_logger.warn("Retrying " + cmd.getClass().getSimpleName() + ". Number of retries remaining: " + numRetries);
        return executeRequest(cmd, numRetries);
    }

    @Override
    public void disconnected() {

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
    public IAgentControl getAgentControl() {
        return null;
    }

    @Override
    public void setAgentControl(IAgentControl agentControl) {
    }

    private Answer executeRequest(ReadyCommand cmd) {
        return new ReadyAnswer(cmd);
    }

    private Answer executeRequest(MaintainCommand cmd) {
        return new MaintainAnswer(cmd);
    }

    private List<Subnet> getListSubnet(List<String> list) {
        List<Subnet> subnets = new ArrayList<>();
        for (String str : list) {
            String[] pair = StringUtils.split(str, "/");
            Subnet subnet = new Subnet(pair[0], Integer.parseInt(pair[1]));
            subnets.add(subnet);
        }
        return subnets;
    }
}
