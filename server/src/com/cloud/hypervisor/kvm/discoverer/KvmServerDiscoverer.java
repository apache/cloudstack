/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

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

import com.cloud.agent.AgentManager;
import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.ShutdownCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.DiscoveredWithErrorException;
import com.cloud.exception.DiscoveryException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.kvm.resource.KvmDummyResourceBase;
import com.cloud.network.NetworkManager;
import com.cloud.network.PhysicalNetworkSetupInfo;
import com.cloud.network.PhysicalNetworkVO;
import com.cloud.resource.Discoverer;
import com.cloud.resource.DiscovererBase;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceStateAdapter;
import com.cloud.resource.ServerResource;
import com.cloud.resource.UnableDeleteHostException;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.utils.script.Script;
import com.cloud.utils.ssh.SSHCmdHelper;
import com.trilead.ssh2.ChannelCondition;
import com.trilead.ssh2.SCPClient;
import com.trilead.ssh2.Session;

@Local(value=Discoverer.class)
public class KvmServerDiscoverer extends DiscovererBase implements Discoverer,
		Listener, ResourceStateAdapter {
	 private static final Logger s_logger = Logger.getLogger(KvmServerDiscoverer.class);
	 private String _setupAgentPath;
	 private ConfigurationDao _configDao;
	 private String _hostIp;
	 private int _waitTime = 5; /*wait for 5 minutes*/
	 private String _kvmPrivateNic;
	 private String _kvmPublicNic;
	 private String _kvmGuestNic;
	 @Inject HostDao _hostDao = null;
	 @Inject ClusterDao _clusterDao;
	 @Inject ResourceManager _resourceMgr;
	 @Inject AgentManager _agentMgr;
	 @Inject NetworkManager _networkMgr;
	 
	@Override
	public boolean processAnswers(long agentId, long seq, Answer[] answers) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean processCommands(long agentId, long seq, Command[] commands) {
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
	public void processConnect(HostVO host, StartupCommand cmd, boolean forRebalance) {
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
			String password, List<String> hostTags) throws DiscoveryException {
		
        ClusterVO cluster = _clusterDao.findById(clusterId);
        if(cluster == null || cluster.getHypervisorType() != HypervisorType.KVM) {
        	if(s_logger.isInfoEnabled())
        		s_logger.info("invalid cluster id or cluster is not for KVM hypervisors"); 
    		return null;
        }
		
		 Map<KvmDummyResourceBase, Map<String, String>> resources = new HashMap<KvmDummyResourceBase, Map<String, String>>();
		 Map<String, String> details = new HashMap<String, String>();
		if (!uri.getScheme().equals("http")) {
            String msg = "urlString is not http so we're not taking care of the discovery for this: " + uri;
            s_logger.debug(msg);
            return null;
		}
		com.trilead.ssh2.Connection sshConnection = null;
		String agentIp = null;
		try {
			
			String hostname = uri.getHost();
			InetAddress ia = InetAddress.getByName(hostname);
			agentIp = ia.getHostAddress();
			String guid = UUID.nameUUIDFromBytes(agentIp.getBytes()).toString();
			String guidWithTail = guid + "-LibvirtComputingResource";/*tail added by agent.java*/
			if (_resourceMgr.findHostByGuid(guidWithTail) != null) {
				s_logger.debug("Skipping " + agentIp + " because " + guidWithTail + " is already in the database.");
				return null;
			}       
			
			sshConnection = new com.trilead.ssh2.Connection(agentIp, 22);

			sshConnection.connect(null, 60000, 60000);
			if (!sshConnection.authenticateWithPassword(username, password)) {
				s_logger.debug("Failed to authenticate");
				throw new DiscoveredWithErrorException("Authetication error");
			}
			
			if (!SSHCmdHelper.sshExecuteCmd(sshConnection, "lsmod|grep kvm", 3)) {
				s_logger.debug("It's not a KVM enabled machine");
				return null;
			}
			
			List<PhysicalNetworkSetupInfo> networks = _networkMgr.getPhysicalNetworkInfo(dcId, HypervisorType.KVM);
			if (networks.size() < 1) {
				s_logger.debug("Can't find physical network devices on zone: " + dcId + ", use the default from kvm.{private|public|guest}.devices");
			} else {
				PhysicalNetworkSetupInfo network = networks.get(0);
				String pubNetName = network.getPublicNetworkName();
				if (pubNetName != null) {
					_kvmPublicNic = pubNetName;
				}
				String prvNetName = network.getPrivateNetworkName();
				if (prvNetName != null) {
					_kvmPrivateNic = prvNetName;
				}
				String guestNetName = network.getGuestNetworkName();
				if (guestNetName != null) {
					_kvmGuestNic = guestNetName;
				}
			}
			
			String parameters = " -m " + _hostIp + " -z " + dcId + " -p " + podId + " -c " + clusterId + " -g " + guid + " -a";
			
			if (_kvmPublicNic != null) {
				parameters += " --pubNic=" + _kvmPublicNic;
			}
			
			if (_kvmPrivateNic != null) {
				parameters += " --prvNic=" + _kvmPrivateNic;
			}
			
			if (_kvmGuestNic != null) {
			    parameters += " --guestNic=" + _kvmGuestNic;
			}
		
			SSHCmdHelper.sshExecuteCmd(sshConnection, "cloud-setup-agent " + parameters, 3);
			
			KvmDummyResourceBase kvmResource = new KvmDummyResourceBase();
			Map<String, Object> params = new HashMap<String, Object>();
						
			params.put("zone", Long.toString(dcId));
			params.put("pod", Long.toString(podId));
			params.put("cluster",  Long.toString(clusterId));
			params.put("guid", guid); 
			params.put("agentIp", agentIp);
			kvmResource.configure("kvm agent", params);
			resources.put(kvmResource, details);
			
			HostVO connectedHost = waitForHostConnect(dcId, podId, clusterId, guidWithTail);
			if (connectedHost == null)
				return null;
			
			details.put("guid", guidWithTail);
			
			 // place a place holder guid derived from cluster ID
			if (cluster.getGuid() == null) {
			    cluster.setGuid(UUID.nameUUIDFromBytes(String.valueOf(clusterId).getBytes()).toString());
			    _clusterDao.update(clusterId, cluster);
			}
			
			//save user name and password
			_hostDao.loadDetails(connectedHost);
			Map<String, String> hostDetails = connectedHost.getDetails();
			hostDetails.put("password", password);
			hostDetails.put("username", username);
			_hostDao.saveDetails(connectedHost);
			return resources;
		} catch (DiscoveredWithErrorException e){ 
			throw e;
		}catch (Exception e) {
			String msg = " can't setup agent, due to " + e.toString() + " - " + e.getMessage();
			s_logger.warn(msg);
		} finally {
			if (sshConnection != null)
				sshConnection.close();
		}
		
		return null;
	}

	private HostVO waitForHostConnect(long dcId, long podId, long clusterId, String guid) {
		for (int i = 0; i < _waitTime *2; i++) {
			List<HostVO> hosts = _resourceMgr.listAllUpAndEnabledHosts(Host.Type.Routing, clusterId, podId, dcId);
			for (HostVO host : hosts) {
				if (host.getGuid().equalsIgnoreCase(guid)) {
					return host;
				}
			}
			try {
				Thread.sleep(30000);
			} catch (InterruptedException e) {
				s_logger.debug("Failed to sleep: " + e.toString());
			}
		}
		s_logger.debug("Timeout, to wait for the host connecting to mgt svr, assuming it is failed");
		List<HostVO> hosts = _resourceMgr.findHostByGuid(dcId, guid);
		if (hosts.size() == 1) {
			return hosts.get(0);
		} else {
			return null;
		}
	}
	
	@Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
		ComponentLocator locator = ComponentLocator.getCurrentLocator();
        _configDao = locator.getDao(ConfigurationDao.class);
		_setupAgentPath = Script.findScript(getPatchPath(), "setup_agent.sh");
		_kvmPrivateNic = _configDao.getValue(Config.KvmPrivateNetwork.key());
		if (_kvmPrivateNic == null) {
		    _kvmPrivateNic = "cloudbr0";
		}
		
		_kvmPublicNic = _configDao.getValue(Config.KvmPublicNetwork.key());
		if (_kvmPublicNic == null) {
		    _kvmPublicNic = _kvmPrivateNic;
		}
		
		_kvmGuestNic = _configDao.getValue(Config.KvmGuestNetwork.key());
		if (_kvmGuestNic == null) {
		    _kvmGuestNic = _kvmPrivateNic;
		}
		
		if (_setupAgentPath == null) {
			throw new ConfigurationException("Can't find setup_agent.sh");
		}
		_hostIp = _configDao.getValue("host");
		if (_hostIp == null) {
			throw new ConfigurationException("Can't get host IP");
		}
    	_resourceMgr.registerResourceStateAdapter(this.getClass().getSimpleName(), this);
		return true;
	}
	
	protected String getPatchPath() {
        return "scripts/vm/hypervisor/kvm/";
    }

	@Override
	public void postDiscovery(List<HostVO> hosts, long msId)
			throws DiscoveryException {
		// TODO Auto-generated method stub
	}
	
	public Hypervisor.HypervisorType getHypervisorType() {
		return Hypervisor.HypervisorType.KVM;
	}
	
    @Override
	public boolean matchHypervisor(String hypervisor) {
    	// for backwards compatibility, if not supplied, always let to try it
    	if(hypervisor == null)
    		return true;
    	
    	return Hypervisor.HypervisorType.KVM.toString().equalsIgnoreCase(hypervisor);
    }

	@Override
    public HostVO createHostVOForConnectedAgent(HostVO host, StartupCommand[] cmd) {
		StartupCommand firstCmd = cmd[0];
		if (!(firstCmd instanceof StartupRoutingCommand)) {
			return null;
		}

		StartupRoutingCommand ssCmd = ((StartupRoutingCommand) firstCmd);
		if (ssCmd.getHypervisorType() != HypervisorType.KVM) {
			return null;
		}

		/* KVM requires host are the same in cluster */
		ClusterVO clusterVO = _clusterDao.findById(host.getClusterId());
		List<HostVO> hostsInCluster = _resourceMgr.listAllHostsInCluster(clusterVO.getId());
		if (!hostsInCluster.isEmpty()) {
			HostVO oneHost = hostsInCluster.get(0);
			_hostDao.loadDetails(oneHost);
			String hostOsInCluster = oneHost.getDetail("Host.OS");
			String hostOs = ssCmd.getHostDetails().get("Host.OS");
			if (!hostOsInCluster.equalsIgnoreCase(hostOs)) {
				throw new IllegalArgumentException("Can't add host: " + firstCmd.getPrivateIpAddress() + " with hostOS: " + hostOs + " into a cluster,"
				        + "in which there are " + hostOsInCluster + " hosts added");
			}
		}
		
		_hostDao.loadDetails(host);
		
		return _resourceMgr.fillRoutingHostVO(host, ssCmd, HypervisorType.KVM, host.getDetails(), null);
    }

	@Override
    public HostVO createHostVOForDirectConnectAgent(HostVO host, StartupCommand[] startup, ServerResource resource, Map<String, String> details,
            List<String> hostTags) {
	    // TODO Auto-generated method stub
	    return null;
    }

	@Override
    public DeleteHostAnswer deleteHost(HostVO host, boolean isForced, boolean isForceDeleteStorage) throws UnableDeleteHostException {
        if (host.getType() != Host.Type.Routing || host.getHypervisorType() != HypervisorType.KVM) {
            return null;
        }
        
        _resourceMgr.deleteRoutingHost(host, isForced, isForceDeleteStorage);
        try {
            ShutdownCommand cmd = new ShutdownCommand(ShutdownCommand.DeleteHost, null);
            _agentMgr.send(host.getId(), cmd);
        } catch (AgentUnavailableException e) {
            s_logger.warn("Sending ShutdownCommand failed: ", e);
        } catch (OperationTimedoutException e) {
            s_logger.warn("Sending ShutdownCommand failed: ", e);
        }
        
        return new DeleteHostAnswer(true);
    }
	
    @Override
    public boolean stop() {
    	_resourceMgr.unregisterResourceStateAdapter(this.getClass().getSimpleName());
        return super.stop();
    }
    
    
}
