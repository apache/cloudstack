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

import com.cloud.configuration.Config;
import com.cloud.dc.ClusterVO;
import com.cloud.exception.DiscoveredWithErrorException;
import com.cloud.exception.DiscoveryException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.network.PhysicalNetworkSetupInfo;
import com.cloud.resource.Discoverer;
import com.cloud.resource.ServerResource;
import com.cloud.utils.ssh.SSHCmdHelper;

@Local(value = Discoverer.class)
public class DockerServerDiscoverer extends LibvirtServerDiscoverer {
    private static final Logger s_logger = Logger.getLogger(DockerServerDiscoverer.class);
    private String _hostIp;
    private final int _waitTime = 5; /* wait for 5 minutes */
    private String _dockerPrivateNic;
    private String _dockerPublicNic;
    private String _dockerGuestNic;

    @Override
    public Hypervisor.HypervisorType getHypervisorType() {
        return Hypervisor.HypervisorType.Docker;
    }
    
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _dockerPrivateNic = _configDao.getValue(Config.KvmPrivateNetwork.key());
        if (_dockerPrivateNic == null) {
        	_dockerPrivateNic = "cloudbr0";
        }

        _dockerPublicNic = _configDao.getValue(Config.KvmPublicNetwork.key());
        if (_dockerPublicNic == null) {
        	_dockerPublicNic = _dockerPrivateNic;
        }

        _dockerGuestNic = _configDao.getValue(Config.KvmGuestNetwork.key());
        if (_dockerGuestNic == null) {
            _dockerGuestNic = _dockerPrivateNic;
        }

        _hostIp = _configDao.getValue("host");
        if (_hostIp == null) {
            throw new ConfigurationException("Can't get host IP");
        }
        _resourceMgr.registerResourceStateAdapter(this.getClass().getSimpleName(), this);
        return true;
    }
    
    @Override
    public Map<? extends ServerResource, Map<String, String>> find(long dcId, Long podId, Long clusterId, URI uri,
            String username, String password, List<String> hostTags) throws DiscoveryException {
        ClusterVO cluster = _clusterDao.findById(clusterId);
        if (cluster == null || cluster.getHypervisorType() != getHypervisorType()) {
            if (s_logger.isInfoEnabled())
                s_logger.info("invalid cluster id or cluster is not for " + getHypervisorType() + " hypervisors");
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
            String guidWithTail = guid + "-LibvirtComputingResource";/*
                                                                      * tail
                                                                      * added by
                                                                      * agent
                                                                      * .java
                                                                      */
            if (_resourceMgr.findHostByGuid(guidWithTail) != null) {
                s_logger.debug("Skipping " + agentIp + " because " + guidWithTail + " is already in the database.");
                return null;
            }

            sshConnection = new com.trilead.ssh2.Connection(agentIp, 22);
            sshConnection.connect(null, 60000, 60000);
            if (!sshConnection.authenticateWithPassword(username, password)) {
                s_logger.debug("Failed to authenticate");
                throw new DiscoveredWithErrorException("Authentication error");
            }
            
            List<PhysicalNetworkSetupInfo> netInfos = _networkMgr.getPhysicalNetworkInfo(dcId, getHypervisorType());
            String dockerPrivateNic = null;
            String dockerPublicNic = null;
            String dockerGuestNic = null;
            
            for (PhysicalNetworkSetupInfo info : netInfos) {
                if (info.getPrivateNetworkName() != null) {
                	dockerPrivateNic = info.getPrivateNetworkName();
                }
                if (info.getPublicNetworkName() != null) {
                	dockerPublicNic = info.getPublicNetworkName();
                }
                if (info.getGuestNetworkName() != null) {
                	dockerGuestNic = info.getGuestNetworkName();
                }
            }
            
            if (dockerPrivateNic == null && dockerPublicNic == null && dockerGuestNic == null) {
                dockerPrivateNic = _dockerPrivateNic;
                dockerPublicNic = _dockerPublicNic;
                dockerGuestNic = _dockerGuestNic;
            }

            if (dockerPublicNic == null) {
                dockerPublicNic = (dockerGuestNic != null) ? dockerGuestNic : dockerPrivateNic;
            }

            if (dockerPrivateNic == null) {
                dockerPrivateNic = (dockerPublicNic != null) ? dockerPublicNic : dockerGuestNic;
            }

            if (dockerGuestNic == null) {
                dockerGuestNic = (dockerPublicNic != null) ? dockerPublicNic : dockerPrivateNic;
            }

            String parameters = " -m " + _hostIp + " -z " + dcId + " -p " + podId + " -c " + clusterId + " -g " + guid
                    + " -a";

            parameters += " --pubNic=" + dockerPublicNic;
            parameters += " --prvNic=" + dockerPrivateNic;
            parameters += " --guestNic=" + dockerGuestNic;
            
            SSHCmdHelper.sshExecuteCmd(sshConnection, "cloudstack-setup-agent " + parameters, 3);
            
            KvmDummyResourceBase kvmResource = new KvmDummyResourceBase();
            Map<String, Object> params = new HashMap<String, Object>();

            params.put("zone", Long.toString(dcId));
            params.put("pod", Long.toString(podId));
            params.put("cluster", Long.toString(clusterId));
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

            // save user name and password
            _hostDao.loadDetails(connectedHost);
            Map<String, String> hostDetails = connectedHost.getDetails();
            hostDetails.put("password", password);
            hostDetails.put("username", username);
            _hostDao.saveDetails(connectedHost);
            return resources;
        } catch (DiscoveredWithErrorException e) {
            throw e;
        } catch (Exception e) {
            String msg = " can't setup agent, due to " + e.toString() + " - " + e.getMessage();
            s_logger.warn(msg);
        } finally {
            if (sshConnection != null)
                sshConnection.close();
        }

    	return null;
    }
    
    private HostVO waitForHostConnect(long dcId, long podId, long clusterId, String guid) {
        for (int i = 0; i < _waitTime * 2; i++) {
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
}
