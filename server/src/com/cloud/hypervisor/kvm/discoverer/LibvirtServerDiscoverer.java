// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.hypervisor.kvm.discoverer;

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
import com.cloud.dc.ClusterVO;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.DiscoveredWithErrorException;
import com.cloud.exception.DiscoveryException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.PhysicalNetworkSetupInfo;
import com.cloud.resource.Discoverer;
import com.cloud.resource.DiscovererBase;
import com.cloud.resource.ResourceStateAdapter;
import com.cloud.resource.ServerResource;
import com.cloud.resource.UnableDeleteHostException;
import com.cloud.utils.ssh.SSHCmdHelper;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.net.InetAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class LibvirtServerDiscoverer extends DiscovererBase implements Discoverer, Listener, ResourceStateAdapter {
    private static final Logger s_logger = Logger.getLogger(LibvirtServerDiscoverer.class);
    private String _hostIp;
    private final int _waitTime = 5; /* wait for 5 minutes */
    private String _kvmPrivateNic;
    private String _kvmPublicNic;
    private String _kvmGuestNic;
    @Inject
    AgentManager _agentMgr;

    @Override
    public abstract Hypervisor.HypervisorType getHypervisorType();

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
    public AgentControlAnswer processControlCommand(long agentId, AgentControlCommand cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void processConnect(Host host, StartupCommand cmd, boolean forRebalance) {
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
    public Map<? extends ServerResource, Map<String, String>>
        find(long dcId, Long podId, Long clusterId, URI uri, String username, String password, List<String> hostTags) throws DiscoveryException {

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

            List<HostVO> existingHosts = _resourceMgr.listAllHostsInOneZoneByType(Host.Type.Routing, dcId);
            if (existingHosts != null) {
                for (HostVO existingHost : existingHosts) {
                    if (existingHost.getGuid().toLowerCase().startsWith(guid.toLowerCase())) {
                        s_logger.debug("Skipping " + agentIp + " because " + guid + " is already in the database for resource " + existingHost.getGuid());
                        return null;
                    }
                }
            }

            sshConnection = new com.trilead.ssh2.Connection(agentIp, 22);

            sshConnection.connect(null, 60000, 60000);
            if (!sshConnection.authenticateWithPassword(username, password)) {
                s_logger.debug("Failed to authenticate");
                throw new DiscoveredWithErrorException("Authentication error");
            }

            if (!SSHCmdHelper.sshExecuteCmd(sshConnection, "lsmod|grep kvm", 3)) {
                s_logger.debug("It's not a KVM enabled machine");
                return null;
            }

            List<PhysicalNetworkSetupInfo> netInfos = _networkMgr.getPhysicalNetworkInfo(dcId, getHypervisorType());
            String kvmPrivateNic = null;
            String kvmPublicNic = null;
            String kvmGuestNic = null;

            for (PhysicalNetworkSetupInfo info : netInfos) {
                if (info.getPrivateNetworkName() != null) {
                    kvmPrivateNic = info.getPrivateNetworkName();
                }
                if (info.getPublicNetworkName() != null) {
                    kvmPublicNic = info.getPublicNetworkName();
                }
                if (info.getGuestNetworkName() != null) {
                    kvmGuestNic = info.getGuestNetworkName();
                }
            }

            if (kvmPrivateNic == null && kvmPublicNic == null && kvmGuestNic == null) {
                kvmPrivateNic = _kvmPrivateNic;
                kvmPublicNic = _kvmPublicNic;
                kvmGuestNic = _kvmGuestNic;
            }

            if (kvmPublicNic == null) {
                kvmPublicNic = (kvmGuestNic != null) ? kvmGuestNic : kvmPrivateNic;
            }

            if (kvmPrivateNic == null) {
                kvmPrivateNic = (kvmPublicNic != null) ? kvmPublicNic : kvmGuestNic;
            }

            if (kvmGuestNic == null) {
                kvmGuestNic = (kvmPublicNic != null) ? kvmPublicNic : kvmPrivateNic;
            }

            String parameters = " -m " + _hostIp + " -z " + dcId + " -p " + podId + " -c " + clusterId + " -g " + guid + " -a";

            parameters += " --pubNic=" + kvmPublicNic;
            parameters += " --prvNic=" + kvmPrivateNic;
            parameters += " --guestNic=" + kvmGuestNic;
            parameters += " --hypervisor=" + cluster.getHypervisorType().toString().toLowerCase();

            String setupAgentCommand = "cloudstack-setup-agent ";
            if (!username.equals("root")) {
                setupAgentCommand = "sudo cloudstack-setup-agent ";
            }
            if (!SSHCmdHelper.sshExecuteCmd(sshConnection,
                    setupAgentCommand + parameters, 3)) {
                s_logger.info("cloudstack agent setup command failed: "
                        + setupAgentCommand + parameters);
                return null;
            }

            KvmDummyResourceBase kvmResource = new KvmDummyResourceBase();
            Map<String, Object> params = new HashMap<String, Object>();

            params.put("router.aggregation.command.each.timeout", _configDao.getValue(Config.RouterAggregationCommandEachTimeout.toString()));

            params.put("zone", Long.toString(dcId));
            params.put("pod", Long.toString(podId));
            params.put("cluster", Long.toString(clusterId));
            params.put("guid", guid);
            params.put("agentIp", agentIp);
            kvmResource.configure("kvm agent", params);
            resources.put(kvmResource, details);

            HostVO connectedHost = waitForHostConnect(dcId, podId, clusterId, guid);
            if (connectedHost == null)
                return null;

            details.put("guid", connectedHost.getGuid());

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
                if (host.getGuid().toLowerCase().startsWith(guid.toLowerCase())) {
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
        // _setupAgentPath = Script.findScript(getPatchPath(),
        // "setup_agent.sh");
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
    public void postDiscovery(List<HostVO> hosts, long msId) throws DiscoveryException {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean matchHypervisor(String hypervisor) {
        // for backwards compatibility, if not supplied, always let to try it
        if (hypervisor == null)
            return true;

        return getHypervisorType().toString().equalsIgnoreCase(hypervisor);
    }

    @Override
    public HostVO createHostVOForConnectedAgent(HostVO host, StartupCommand[] cmd) {
        StartupCommand firstCmd = cmd[0];
        if (!(firstCmd instanceof StartupRoutingCommand)) {
            return null;
        }

        StartupRoutingCommand ssCmd = ((StartupRoutingCommand)firstCmd);
        if (ssCmd.getHypervisorType() != getHypervisorType()) {
            return null;
        }

        /* KVM requires host are the same in cluster */
        ClusterVO clusterVO = _clusterDao.findById(host.getClusterId());
        if (clusterVO == null) {
            s_logger.debug("cannot find cluster: " + host.getClusterId());
            throw new IllegalArgumentException("cannot add host, due to can't find cluster: " + host.getClusterId());
        }

        List<HostVO> hostsInCluster = _resourceMgr.listAllHostsInCluster(clusterVO.getId());
        if (!hostsInCluster.isEmpty()) {
            HostVO oneHost = hostsInCluster.get(0);
            _hostDao.loadDetails(oneHost);
            String hostOsInCluster = oneHost.getDetail("Host.OS");
            String hostOs = ssCmd.getHostDetails().get("Host.OS");
            if (!hostOsInCluster.equalsIgnoreCase(hostOs)) {
                throw new IllegalArgumentException("Can't add host: " + firstCmd.getPrivateIpAddress() + " with hostOS: " + hostOs + " into a cluster," +
                    "in which there are " + hostOsInCluster + " hosts added");
            }
        }

        _hostDao.loadDetails(host);

        return _resourceMgr.fillRoutingHostVO(host, ssCmd, getHypervisorType(), host.getDetails(), null);
    }

    @Override
    public HostVO createHostVOForDirectConnectAgent(HostVO host, StartupCommand[] startup, ServerResource resource, Map<String, String> details, List<String> hostTags) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DeleteHostAnswer deleteHost(HostVO host, boolean isForced, boolean isForceDeleteStorage) throws UnableDeleteHostException {
        if (host.getType() != Host.Type.Routing || (host.getHypervisorType() != HypervisorType.KVM && host.getHypervisorType() != HypervisorType.LXC)) {
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
