package com.cloud.hypervisor.kvm.discoverer;

import java.net.InetAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StartupCommand;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.exception.DiscoveryException;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.kvm.resource.KvmDummyResourceBase;
import com.cloud.hypervisor.xen.resource.CitrixResourceBase;
import com.cloud.resource.Discoverer;
import com.cloud.resource.DiscovererBase;
import com.cloud.resource.ServerResource;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.utils.script.Script;
import com.trilead.ssh2.SCPClient;
import com.trilead.ssh2.Session;

@Local(value=Discoverer.class)
public class KvmServerDiscoverer extends DiscovererBase implements Discoverer,
		Listener {
	 private static final Logger s_logger = Logger.getLogger(KvmServerDiscoverer.class);
	 private String _setupAgentPath;
	 private ConfigurationDao _configDao;
	 private String _hostIp;
	 private int _waitTime = 10;
	 @Inject HostDao _hostDao = null;
	 
	@Override
	public boolean processAnswer(long agentId, long seq, Answer[] answers) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean processCommand(long agentId, long seq, Command[] commands) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public AgentControlAnswer processControlCommand(long agentId,
			AgentControlCommand cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean processConnect(HostVO host, StartupCommand cmd) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean processDisconnect(long agentId, Status state) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isRecurring() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int getTimeout() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean processTimeout(long agentId, long seq) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Map<? extends ServerResource, Map<String, String>> find(long dcId,
			Long podId, Long clusterId, URI uri, String username,
			String password) throws DiscoveryException {
		 Map<KvmDummyResourceBase, Map<String, String>> resources = new HashMap<KvmDummyResourceBase, Map<String, String>>();
		 Map<String, String> details = new HashMap<String, String>();
		if (!uri.getScheme().equals("http")) {
            String msg = "urlString is not http so we're not taking care of the discovery for this: " + uri;
            s_logger.debug(msg);
            return null;
		}
		com.trilead.ssh2.Connection sshConnection = null;
		Session sshSession = null;
		String agentIp = null;
		try {
			
			String hostname = uri.getHost();
			InetAddress ia = InetAddress.getByName(hostname);
			agentIp = ia.getHostAddress();
			String guid = UUID.nameUUIDFromBytes(agentIp.getBytes()).toString();
			sshConnection = new com.trilead.ssh2.Connection(agentIp, 22);

			sshConnection.connect(null, 60000, 60000);
			if (!sshConnection.authenticateWithPassword(username, password)) {
				throw new Exception("Unable to authenticate");
			}
			SCPClient scp = new SCPClient(sshConnection);
			scp.put(_setupAgentPath, "/usr/bin", "0755");
			sshSession = sshConnection.openSession();
			/*running setup script in background, because we will restart agent network, that may cause connection lost*/
			s_logger.debug("/usr/bin/setup_agent.sh " + " -h " + _hostIp + " -z " + dcId + " -p " + podId + " -u " + guid);
			sshSession.execCommand("/usr/bin/setup_agent.sh " + " -h " + _hostIp + " -z " + dcId + " -p " + podId + " -u " + guid + " 1>&2" );
			
			KvmDummyResourceBase kvmResource = new KvmDummyResourceBase();
			Map<String, Object> params = new HashMap<String, Object>();
			
			params.put("zone", Long.toString(dcId));
			params.put("pod", Long.toString(podId));
			params.put("guid", guid);
			params.put("agentIp", agentIp);
			kvmResource.configure("kvm agent", params);
			kvmResource.setRemoteAgent(true);
			resources.put(kvmResource, details);
			return resources;
		} catch (Exception e) {
			String msg = " can't setup agent, due to " + e.toString() + " - " + e.getMessage();
			s_logger.warn(msg);
		} finally {
			if (sshSession != null) 
				sshSession.close();
			
			if (sshConnection != null)
				sshConnection.close();
		}
		
		return null;
	}

	@Override
	public void postDiscovery(List<HostVO> hosts, long msId) throws DiscoveryException {
		/*Wait for agent coming back*/
		if (hosts.isEmpty()) {
			return;
		}
		HostVO host = hosts.get(0);
		for (int i = 0 ; i < _waitTime; i++) {
			
			if (host.getStatus() != Status.Up) {
				try {
					Thread.sleep(60000);
				} catch (InterruptedException e) {
					s_logger.debug("Failed to sleep: " + e.toString());
				}
			} else {
				return;
			}
		}

		/*Timeout, throw warning msg to user*/
		throw new DiscoveryException("Agent " + host.getId() + ":" + host.getPublicIpAddress() + " does not come back, It may connect to server later, if not, please check the agent log");
	}
	
	@Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
		ComponentLocator locator = ComponentLocator.getCurrentLocator();
        _configDao = locator.getDao(ConfigurationDao.class);
		_setupAgentPath = Script.findScript(getPatchPath(), "setup_agent.sh");
		
		if (_setupAgentPath == null) {
			throw new ConfigurationException("Can't find setup_agent.sh");
		}
		_hostIp = _configDao.getValue("host");
		if (_hostIp == null) {
			throw new ConfigurationException("Can't get host IP");
		}
		return true;
	}
	
	protected String getPatchPath() {
        return "scripts/vm/hypervisor/kvm/";
    }
}
