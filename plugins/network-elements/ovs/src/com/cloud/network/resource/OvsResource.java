package com.cloud.network.resource;

import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

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
import com.cloud.host.Host.Type;
import com.cloud.network.ovs.OvsApi;
import com.cloud.network.ovs.StartupOvsCommand;
import com.cloud.resource.ServerResource;
import com.cloud.utils.component.ManagerBase;

public class OvsResource extends ManagerBase implements ServerResource {
	private static final Logger s_logger = Logger.getLogger(OvsResource.class);

	private String _name;
	private String _guid;
	private String _zoneId;
	private int _numRetries;

	private OvsApi _ovsApi;

	protected OvsApi createOvsApi() {
		return new OvsApi();
	}

	@Override
	public boolean configure(String name, Map<String, Object> params)
			throws ConfigurationException {
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

		String ip = (String) params.get("ip");
		if (ip == null) {
			throw new ConfigurationException("Unable to find IP");
		}

		_ovsApi = createOvsApi();
		_ovsApi.setControllerAddress(ip);

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
		return Host.Type.L2Networking;
	}

	@Override
	public StartupCommand[] initialize() {
		StartupOvsCommand sc = new StartupOvsCommand();
		sc.setGuid(_guid);
		sc.setName(_name);
		sc.setDataCenter(_zoneId);
		sc.setPod("");
		sc.setPrivateIpAddress("");
		sc.setStorageIpAddress("");
		sc.setVersion("");
		return new StartupCommand[] { sc };
	}

	@Override
	public PingCommand getCurrentStatus(long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Answer executeRequest(Command cmd) {
		return executeRequest(cmd, _numRetries);
	}

	private Answer executeRequest(ReadyCommand cmd) {
		return new ReadyAnswer(cmd);
	}

	private Answer executeRequest(MaintainCommand cmd) {
		return new MaintainAnswer(cmd);
	}

	public Answer executeRequest(Command cmd, int numRetries) {
		if (cmd instanceof ReadyCommand) {
			return executeRequest((ReadyCommand) cmd);
		} else if (cmd instanceof MaintainCommand) {
			return executeRequest((MaintainCommand) cmd);
		}
		// TODO: implement services request
		// else if (cmd instanceof CreateOvsNetworkCommand) {
		// return executeRequest((CreateOvsNetworkCommand)cmd, numRetries);
		// }
		// else if (cmd instanceof DeleteOvsNetworkCommand) {
		// return executeRequest((DeleteOvsNetworkCommand) cmd, numRetries);
		// }
		// else if (cmd instanceof CreateOvsPortCommand) {
		// return executeRequest((CreateOvsPortCommand) cmd, numRetries);
		// }
		// else if (cmd instanceof DeleteOvsPortCommand) {
		// return executeRequest((DeleteOvsPortCommand) cmd, numRetries);
		// }
		// else if (cmd instanceof UpdateOvsPortCommand) {
		// return executeRequest((UpdateOvsPortCommand) cmd, numRetries);
		// }
		s_logger.debug("Received unsupported command " + cmd.toString());
		return Answer.createUnsupportedCommandAnswer(cmd);
	}

	@Override
	public void disconnected() {
	}

	private Answer retry(Command cmd, int numRetries) {
		s_logger.warn("Retrying " + cmd.getClass().getSimpleName()
				+ ". Number of retries remaining: " + numRetries);
		return executeRequest(cmd, numRetries);
	}

	private String truncate(String string, int length) {
		if (string.length() <= length) {
			return string;
		} else {
			return string.substring(0, length);
		}
	}

	@Override
	public IAgentControl getAgentControl() {
		return null;
	}

	@Override
	public void setAgentControl(IAgentControl agentControl) {
	}

}
