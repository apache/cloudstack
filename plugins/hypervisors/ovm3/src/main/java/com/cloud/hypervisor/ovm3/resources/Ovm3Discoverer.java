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

package com.cloud.hypervisor.ovm3.resources;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.configuration.Config;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.exception.DiscoveryException;
import com.cloud.host.Host;
import com.cloud.host.HostInfo;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.ovm3.objects.Connection;
import com.cloud.hypervisor.ovm3.objects.Linux;
import com.cloud.hypervisor.ovm3.objects.Ovm3ResourceException;
import com.cloud.resource.Discoverer;
import com.cloud.resource.DiscovererBase;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceStateAdapter;
import com.cloud.resource.ServerResource;
import com.cloud.resource.UnableDeleteHostException;
import com.cloud.utils.db.QueryBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.ssh.SSHCmdHelper;

public class Ovm3Discoverer extends DiscovererBase implements Discoverer,
        Listener, ResourceStateAdapter {
    private static final Logger LOGGER = Logger.getLogger(Ovm3Discoverer.class);
    protected String publicNetworkDevice;
    protected String privateNetworkDevice;
    protected String guestNetworkDevice;
    protected String storageNetworkDevice;

    @Inject
    ClusterDao clusterDao;
    @Inject
    ClusterDetailsDao clusterDetailsDao;
    @Inject
    ResourceManager resourceMgr;
    @Inject
    AgentManager agentMgr;
    @Inject
    HostDao hostDao = null;

    protected Ovm3Discoverer() {
    }

    @Override
    public boolean configure(String name, Map<String, Object> params)
            throws ConfigurationException {
        boolean success = super.configure(name, params);
        if (!success) {
            return false;
        }

        /* these are in Config.java */
        publicNetworkDevice = _params.get(Config.Ovm3PublicNetwork.key());
        privateNetworkDevice = _params.get(Config.Ovm3PrivateNetwork.key());
        guestNetworkDevice = _params.get(Config.Ovm3GuestNetwork.key());
        storageNetworkDevice = _params.get(Config.Ovm3StorageNetwork.key());
        resourceMgr.registerResourceStateAdapter(this.getClass()
                .getSimpleName(), this);
        return true;
    }

    @Override
    public boolean stop() {
        resourceMgr.unregisterResourceStateAdapter(this.getClass()
                .getSimpleName());
        return super.stop();
    }

    private boolean checkIfExisted(String guid) {
        QueryBuilder<HostVO> sc = QueryBuilder.create(HostVO.class);
        sc.and(sc.entity().getGuid(), SearchCriteria.Op.EQ, guid);
        sc.and(sc.entity().getHypervisorType(), SearchCriteria.Op.EQ,
                HypervisorType.Ovm3);
        List<HostVO> hosts = sc.list();
        return !hosts.isEmpty();
    }

    private boolean CheckUrl(URI url) throws DiscoveryException {
        if ("http".equals(url.getScheme()) || "https".equals(url.getScheme())) {
            String msg = "Discovering " + url + ": " + _params;
            LOGGER.debug(msg);
        } else {
            String msg = "urlString is not http(s) so we're not taking care of the discovery for this: "
                    + url;
            LOGGER.info(msg);
            throw new DiscoveryException(msg);
        }
        return true;
    }

    @Override
    public Map<? extends ServerResource, Map<String, String>> find(long dcId,
            Long podId, Long clusterId, URI url, String username,
            String password, List<String> hostTags) throws DiscoveryException {
        Connection c = null;

        CheckUrl(url);
        if (clusterId == null) {
            String msg = "must specify cluster Id when add host";
            LOGGER.info(msg);
            throw new DiscoveryException(msg);
        }

        if (podId == null) {
            String msg = "must specify pod Id when add host";
            LOGGER.info(msg);
            throw new DiscoveryException(msg);
        }

        ClusterVO cluster = clusterDao.findById(clusterId);
        if (cluster == null
                || (cluster.getHypervisorType() != HypervisorType.Ovm3)) {
            String msg = "invalid cluster id or cluster is not for Ovm3 hypervisors";
            LOGGER.info(msg);
            throw new DiscoveryException(msg);
        } else {
            LOGGER.debug("cluster: " + cluster);
        }

        String agentUsername = _params.get("agentusername");
        if (agentUsername == null) {
            String msg = "Agent user name must be specified";
            LOGGER.info(msg);
            throw new DiscoveryException(msg);
        }

        String agentPassword = _params.get("agentpassword");
        if (agentPassword == null) {
            String msg = "Agent password must be specified";
            LOGGER.info(msg);
            throw new DiscoveryException(msg);
        }

        String agentPort = _params.get("agentport");
        if (agentPort == null) {
            String msg = "Agent port must be specified";
            LOGGER.info(msg);
            throw new DiscoveryException(msg);
        }

        try {
            String hostname = url.getHost();

            InetAddress ia = InetAddress.getByName(hostname);
            String hostIp = ia.getHostAddress();
            String guid = UUID.nameUUIDFromBytes(hostIp.getBytes("UTF8"))
                    .toString();

            if (checkIfExisted(guid)) {
                String msg = "The host " + hostIp + " has been added before";
                LOGGER.info(msg);
                throw new DiscoveryException(msg);
            }

            LOGGER.debug("Ovm3 discover is going to disover host having guid "
                    + guid);

            ClusterVO clu = clusterDao.findById(clusterId);
            if (clu.getGuid() == null) {
                clu.setGuid(UUID.randomUUID().toString());
            }
            clusterDao.update(clusterId, clu);
            Map<String, String> clusterDetails = clusterDetailsDao
                    .findDetails(clusterId);
            String ovm3vip = (clusterDetails.get("ovm3vip") == null) ? ""
                    : clusterDetails.get("ovm3vip");
            String ovm3pool = (clusterDetails.get("ovm3pool") == null) ? "false"
                    : clusterDetails.get("ovm3pool");
            String ovm3cluster = (clusterDetails.get("ovm3cluster") == null) ? "false"
                    : clusterDetails.get("ovm3cluster");

            /* should perhaps only make this connect to the agent port ? */
            com.trilead.ssh2.Connection sshConnection = new com.trilead.ssh2.Connection(
                    hostIp, 22);
            sshConnection.connect(null, 60000, 60000);
            sshConnection = SSHCmdHelper.acquireAuthorizedConnection(hostIp,
                    username, password);
            if (sshConnection == null) {
                String msg = "Cannot Ssh to Ovm3 host(IP=" + hostIp
                        + ", username=" + username
                        + ", password=*******), discovery failed";
                LOGGER.warn(msg);
                throw new DiscoveryException(msg);
            }

            Map<String, String> details = new HashMap<String, String>();
            Ovm3HypervisorResource ovmResource = new Ovm3HypervisorResource();
            details.put("ip", hostIp);
            details.put("host", hostname);
            details.put("username", username);
            details.put("password", password);
            details.put("zone", Long.toString(dcId));
            details.put("guid", guid);
            details.put("pod", Long.toString(podId));
            details.put("cluster", Long.toString(clusterId));
            details.put("agentusername", agentUsername);
            details.put("agentpassword", agentPassword);
            details.put("agentport", agentPort.toString());
            details.put("ovm3vip", ovm3vip);
            details.put("ovm3pool", ovm3pool);
            details.put("ovm3cluster", ovm3cluster);

            if (publicNetworkDevice != null) {
                details.put("public.network.device", publicNetworkDevice);
            }
            if (privateNetworkDevice != null) {
                details.put("private.network.device", privateNetworkDevice);
            }
            if (guestNetworkDevice != null) {
                details.put("guest.network.device", guestNetworkDevice);
            }
            if (storageNetworkDevice != null) {
                details.put("storage.network.device", storageNetworkDevice);
            }

            Map<String, Object> params = new HashMap<String, Object>();
            params.putAll(details);

            ovmResource.configure(hostname, params);
            ovmResource.start();

            c = new Connection(hostIp, Integer.parseInt(agentPort),
                    agentUsername, agentPassword);

            /* After resource start, we are able to execute our agent api */
            Linux host = new Linux(c);
            details.put("agentVersion", host.getAgentVersion());
            details.put(HostInfo.HOST_OS_KERNEL_VERSION,
                    host.getHostKernelRelease());
            details.put(HostInfo.HOST_OS, host.getHostOs());
            details.put(HostInfo.HOST_OS_VERSION, host.getHostOsVersion());
            details.put(HostInfo.HYPERVISOR_VERSION,
                    host.getHypervisorVersion());

            Map<Ovm3HypervisorResource, Map<String, String>> resources = new HashMap<Ovm3HypervisorResource, Map<String, String>>();
            resources.put(ovmResource, details);
            return resources;
        } catch (UnknownHostException e) {
            LOGGER.error(
                    "Host name resolve failed exception, Unable to discover Ovm3 host: "
                            + url.getHost(), e);
            return null;
        } catch (ConfigurationException e) {
            LOGGER.error(
                    "Configure resource failed, Unable to discover Ovm3 host: "
                            + url.getHost(), e);
            return null;
        } catch (IOException | Ovm3ResourceException e) {
            LOGGER.error("Unable to discover Ovm3 host: " + url.getHost(), e);
            return null;
        }
    }

    @Override
    public void postDiscovery(List<HostVO> hosts, long msId)
            throws CloudRuntimeException {
        LOGGER.debug("postDiscovery: " + hosts);
    }

    @Override
    public boolean matchHypervisor(String hypervisor) {
        return HypervisorType.Ovm3.toString().equalsIgnoreCase(hypervisor);
    }

    @Override
    public HypervisorType getHypervisorType() {
        return HypervisorType.Ovm3;
    }

    @Override
    public HostVO createHostVOForConnectedAgent(HostVO host,
            StartupCommand[] cmd) {
        LOGGER.debug("createHostVOForConnectedAgent: " + host);
        return null;
    }

    @Override
    public boolean processAnswers(long agentId, long seq, Answer[] answers) {
        LOGGER.debug("processAnswers: " + agentId);
        return false;
    }

    @Override
    public boolean processCommands(long agentId, long seq, Command[] commands) {
        LOGGER.debug("processCommands: " + agentId);
        return false;
    }

    @Override
    public AgentControlAnswer processControlCommand(long agentId,
            AgentControlCommand cmd) {
        LOGGER.debug("processControlCommand: " + agentId);
        return null;
    }

    @Override
    public void processHostAdded(long hostId) {
    }

    /* for reconnecting */
    @Override
    public void processConnect(Host host, StartupCommand cmd,
            boolean forRebalance) {
        LOGGER.debug("processConnect");
    }

    @Override
    public boolean processDisconnect(long agentId, Status state) {
        LOGGER.debug("processDisconnect");
        return false;
    }

    @Override
    public void processHostAboutToBeRemoved(long hostId) {
    }

    @Override
    public void processHostRemoved(long hostId, long clusterId) {
    }

    @Override
    public boolean isRecurring() {
        return false;
    }

    @Override
    public int getTimeout() {
        LOGGER.debug("getTimeout");
        return 0;
    }

    @Override
    public boolean processTimeout(long agentId, long seq) {
        LOGGER.debug("processTimeout: " + agentId);
        return false;
    }

    @Override
    public HostVO createHostVOForDirectConnectAgent(HostVO host,
            StartupCommand[] startup, ServerResource resource,
            Map<String, String> details, List<String> hostTags) {
        LOGGER.debug("createHostVOForDirectConnectAgent: " + host);
        StartupCommand firstCmd = startup[0];
        if (!(firstCmd instanceof StartupRoutingCommand)) {
            return null;
        }

        StartupRoutingCommand ssCmd = (StartupRoutingCommand) firstCmd;
        if (ssCmd.getHypervisorType() != HypervisorType.Ovm3) {
            return null;
        }

        return resourceMgr.fillRoutingHostVO(host, ssCmd, HypervisorType.Ovm3,
                details, hostTags);
    }

    @Override
    public DeleteHostAnswer deleteHost(HostVO host, boolean isForced,
            boolean isForceDeleteStorage) throws UnableDeleteHostException {
        LOGGER.debug("deleteHost: " + host);
        if (host.getType() != com.cloud.host.Host.Type.Routing
                || host.getHypervisorType() != HypervisorType.Ovm3) {
            return null;
        }

        resourceMgr.deleteRoutingHost(host, isForced, isForceDeleteStorage);
        return new DeleteHostAnswer(true);
    }

}
