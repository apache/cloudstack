package com.cloud.ovm.hypervisor;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;

import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.exception.DiscoveryException;
import com.cloud.host.HostInfo;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.ovm.object.Connection;
import com.cloud.ovm.object.OvmHost;
import com.cloud.resource.Discoverer;
import com.cloud.resource.DiscovererBase;
import com.cloud.resource.ServerResource;
import com.cloud.utils.component.Inject;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.ssh.SSHCmdHelper;

@Local(value=Discoverer.class)
public class OvmDiscoverer extends DiscovererBase implements Discoverer {
	private static final Logger s_logger = Logger.getLogger(OvmDiscoverer.class);
	
	@Inject ClusterDao _clusterDao;
	
	protected OvmDiscoverer() {
		
	}
	
	@Override
	public Map<? extends ServerResource, Map<String, String>> find(long dcId,
			Long podId, Long clusterId, URI url, String username,
			String password, List<String> hostTags) throws DiscoveryException {
		Connection conn = null;
		
        if (!url.getScheme().equals("http")) {
            String msg = "urlString is not http so we're not taking care of the discovery for this: " + url;
            s_logger.debug(msg);
            return null;
        }
        if (clusterId == null) {
            String msg = "must specify cluster Id when add host";
            s_logger.debug(msg);
            throw new CloudRuntimeException(msg);
        } 
        
		if (podId == null) {
			String msg = "must specify pod Id when add host";
			s_logger.debug(msg);
			throw new CloudRuntimeException(msg);
		}
		
		ClusterVO cluster = _clusterDao.findById(clusterId);
        if(cluster == null || (cluster.getHypervisorType() != HypervisorType.Ovm)) {
        	if(s_logger.isInfoEnabled())
        		s_logger.info("invalid cluster id or cluster is not for Ovm hypervisors"); 
    		return null;
        }
        
        String agentUsername = _params.get("agentusername");
        if (agentUsername == null) {
        	throw new CloudRuntimeException("Agent user name must be specified");
        }
        
        String agentPassword = _params.get("agentpassword");
        if (agentPassword == null) {
        	throw new CloudRuntimeException("Agent password must be specified");
        }
        
        try {
        	String hostname = url.getHost();
            InetAddress ia = InetAddress.getByName(hostname);
            String hostIp = ia.getHostAddress();
            String guid = UUID.nameUUIDFromBytes(hostIp.getBytes()).toString();

            
            ClusterVO clu = _clusterDao.findById(clusterId);
			if (clu.getGuid() == null) {
				clu.setGuid(UUID.randomUUID().toString());
				_clusterDao.update(clusterId, clu);
			}
			
			com.trilead.ssh2.Connection sshConnection = new com.trilead.ssh2.Connection(hostIp, 22);
	        sshConnection.connect(null, 60000, 60000);
			sshConnection = SSHCmdHelper.acquireAuthorizedConnection(hostIp, username, password);
			if (sshConnection == null) {
				throw new DiscoveryException(
						String.format("Cannot connect to ovm host(IP=%1$s, username=%2$s, password=%3$s, discover failed", hostIp, username, password));
			}

			if (!SSHCmdHelper.sshExecuteCmd(sshConnection, "[ -f '/etc/ovs-agent/agent.ini' ]")) {
				throw new DiscoveryException("Can not find /etc/ovs-agent/agent.ini " + hostIp);
			}
						
			Map<String, String> details = new HashMap<String, String>();
			OvmResourceBase ovmResource = new OvmResourceBase();
			details.put("ip", hostIp);
			details.put("username", username);
			details.put("password", password);
			details.put("zone", Long.toString(dcId));
			details.put("guid", guid);
			details.put("pod", Long.toString(podId));
			details.put("cluster", Long.toString(clusterId));
			details.put("agentusername", agentUsername);
			details.put("agentpassword", agentPassword);
	
			Map<String, Object> params = new HashMap<String, Object>();
			params.putAll(details);
			ovmResource.configure("Ovm Server", params);
			ovmResource.start();
			
            conn = new Connection(hostIp, "oracle", agentPassword);
			/* After resource start, we are able to execute our agent api*/
			OvmHost.Details d = OvmHost.getDetails(conn);
			details.put("agentVersion", d.agentVersion);
			details.put(HostInfo.HOST_OS_KERNEL_VERSION, d.dom0KernelVersion);
			details.put(HostInfo.HYPERVISOR_VERSION, d.hypervisorVersion);
			
			Map<OvmResourceBase, Map<String, String>> resources = new HashMap<OvmResourceBase, Map<String, String>>();
			resources.put(ovmResource, details);
			return resources;
        } catch (XmlRpcException e) {
        	s_logger.debug("XmlRpc exception, Unable to discover OVM: " + url, e);
        	return null;
		} catch (UnknownHostException e) {
			s_logger.debug("Host name resolve failed exception, Unable to discover OVM: " + url, e);
			return null;
		} catch (ConfigurationException e) {
			s_logger.debug("Configure resource failed, Unable to discover OVM: " + url, e);
			return null;
		} catch (Exception e) {
        	s_logger.debug("Unable to discover OVM: " + url, e);
        	return null;
        }
	}

	@Override
	public void postDiscovery(List<HostVO> hosts, long msId)
			throws DiscoveryException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean matchHypervisor(String hypervisor) {
		return HypervisorType.Ovm.toString().equalsIgnoreCase(hypervisor);
	}

	@Override
	public HypervisorType getHypervisorType() {
		return HypervisorType.Ovm;
	}

}
