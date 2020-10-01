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
import com.cloud.utils.component.ManagerBase;
import net.juniper.contrail.api.ApiConnectorFactory;
import net.juniper.contrail.api.types.InstanceIp;
import net.juniper.contrail.api.types.VirtualMachine;
import net.juniper.contrail.api.types.VirtualMachineInterface;
import net.juniper.contrail.api.types.VirtualNetwork;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenInstanceIpCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenNetworkCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenVmCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenVmInterfaceCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenNetworkCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenVmCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenVmInterfaceCommand;
import org.apache.cloudstack.network.tungsten.agent.api.StartupTungstenCommand;
import org.apache.cloudstack.network.tungsten.agent.api.TungstenAnswer;
import org.apache.cloudstack.network.tungsten.service.TungstenApi;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.naming.ConfigurationException;
import java.io.IOException;
import java.util.Map;

@Component
public class TungstenResource extends ManagerBase implements ServerResource {

    private static final Logger s_logger = Logger.getLogger(TungstenResource.class);

    private String _name;
    private String _guid;
    private String _zoneId;
    private int _numRetries;
    private String _hostname;
    private String _port;
    private String _vrouter;
    private String _vrouterPort;

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

        _vrouter = (String) params.get("vrouter");
        if (_vrouter == null) {
            throw new ConfigurationException("Missing tungsten vrouter hostname from params: " + params);
        }

        _vrouterPort = (String) params.get("vrouterPort");
        if (_vrouterPort == null) {
            throw new ConfigurationException("Missing tungsten vrouter port from params: " + params);
        }

        _tungstenApi = new TungstenApi();
        _tungstenApi.setHostname(_hostname);
        _tungstenApi.setPort(_port);
        _tungstenApi.setVrouter(_vrouter);
        _tungstenApi.setVrouterPort(_vrouterPort);
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
            _tungstenApi.checkTungstenVrouterConnection();
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
        } else if (cmd instanceof CreateTungstenVmCommand) {
            return executeRequest((CreateTungstenVmCommand) cmd, numRetries);
        } else if (cmd instanceof CreateTungstenVmInterfaceCommand) {
            return executeRequest((CreateTungstenVmInterfaceCommand) cmd, numRetries);
        } else if (cmd instanceof CreateTungstenInstanceIpCommand) {
            return executeRequest((CreateTungstenInstanceIpCommand) cmd, numRetries);
        } else if (cmd instanceof DeleteTungstenVmInterfaceCommand) {
            return executeRequest((DeleteTungstenVmInterfaceCommand) cmd, numRetries);
        } else if (cmd instanceof DeleteTungstenVmCommand) {
            return executeRequest((DeleteTungstenVmCommand) cmd, numRetries);
        }
        s_logger.debug("Received unsupported command " + cmd.toString());
        return Answer.createUnsupportedCommandAnswer(cmd);
    }

    private Answer executeRequest(CreateTungstenNetworkCommand cmd, int numRetries) {
        VirtualNetwork virtualNetwork = _tungstenApi.createTungstenNetwork(cmd.getNetworkId(), cmd.getTungstenService(),
                cmd.getNetworkDao(), cmd.getDataCenterDao());
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

    private Answer executeRequest(CreateTungstenVmCommand cmd, int numRetries) {
        VirtualMachine virtualMachine = _tungstenApi.createTungstenVirtualMachine(cmd.getVmUuid(), cmd.getVmName(), cmd.getTungstenService());
        if (virtualMachine != null)
            return new TungstenAnswer(cmd, virtualMachine, true, "Tungsten virtual machine created");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(CreateTungstenVmInterfaceCommand cmd, int numRetries) {
        VirtualMachineInterface vmi = _tungstenApi.createTungstenVmInterface(cmd.getNic(), cmd.getVirtualNetworkUuid(),
                cmd.getVirtualMachineUuid(), cmd.getProjectUuid(), cmd.getTungstenService());
        if (vmi != null)
            return new TungstenAnswer(cmd, vmi, true, "Tungsten virtual machine interface created");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(CreateTungstenInstanceIpCommand cmd, int numRetries) {
        InstanceIp instanceIp = _tungstenApi.createTungstenInstanceIp(cmd.getNic(), cmd.getVirtualNetworkUuid(), cmd.getVmInterfaceUuid(), cmd.getTungstenService());
        if (instanceIp != null)
            return new TungstenAnswer(cmd, instanceIp, true, "Tungsten instance ip created");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(DeleteTungstenVmInterfaceCommand cmd, int numRetries) {
        boolean deleted = _tungstenApi.deleteTungstenVmInterface(cmd.getVmInterfaceUuid(), cmd.getTungstenService());
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
        boolean deleted = _tungstenApi.deleteTungstenVm(cmd.getVirtualMachineUuid(), cmd.getTungstenService());
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
        boolean deleted = _tungstenApi.deleteTungstenNetwork(cmd.getNetworkUuid(), cmd.getTungstenService());
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
}
