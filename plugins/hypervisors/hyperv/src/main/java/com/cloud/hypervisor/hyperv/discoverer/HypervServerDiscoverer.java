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
package com.cloud.hypervisor.hyperv.discoverer;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import javax.inject.Inject;
import javax.naming.ConfigurationException;


import com.cloud.agent.AgentManager;
import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.SetupAnswer;
import com.cloud.agent.api.SetupCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.alert.AlertManager;
import com.cloud.configuration.Config;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConnectionException;
import com.cloud.exception.DiscoveryException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.Host;
import com.cloud.host.HostEnvironment;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.hyperv.resource.HypervDirectConnectResource;
import com.cloud.resource.Discoverer;
import com.cloud.resource.DiscovererBase;
import com.cloud.resource.ResourceStateAdapter;
import com.cloud.resource.ServerResource;
import com.cloud.resource.UnableDeleteHostException;
import com.cloud.storage.StorageLayer;

/**
 * Methods to discover and managem a Hyper-V agent. Prepares a
 * HypervDirectConnectResource corresponding to the agent on a Hyper-V
 * hypervisor and manages its lifecycle.
 */
public class HypervServerDiscoverer extends DiscovererBase implements Discoverer, Listener, ResourceStateAdapter {
    Random _rand = new Random(System.currentTimeMillis());

    Map<String, String> _storageMounts = new HashMap<String, String>();
    StorageLayer _storage;

    @Inject
    private HostPodDao _podDao;


    // TODO: AgentManager and AlertManager not being used to transmit info,
    // may want to reconsider.
    @Inject
    private AgentManager _agentMgr;
    @Inject
    private AlertManager _alertMgr;

    // Listener interface methods

    @Override
    public final boolean processAnswers(final long agentId, final long seq, final Answer[] answers) {
        return false;
    }

    @Override
    public final boolean processCommands(final long agentId, final long seq, final Command[] commands) {
        return false;
    }

    @Override
    public final AgentControlAnswer processControlCommand(final long agentId, final AgentControlCommand cmd) {
        return null;
    }

    @Override
    public void processHostAdded(long hostId) {
    }

    @Override
    public final void processConnect(final Host agent, final StartupCommand cmd, final boolean forRebalance) throws ConnectionException {
        // Limit the commands we can process
        if (!(cmd instanceof StartupRoutingCommand)) {
            return;
        }

        StartupRoutingCommand startup = (StartupRoutingCommand)cmd;

        // assert
        if (startup.getHypervisorType() != HypervisorType.Hyperv) {
            logger.debug("Not Hyper-V hypervisor, so moving on.");
            return;
        }

        long agentId = agent.getId();
        HostVO host = _hostDao.findById(agentId);

        // Our Hyper-V machines are not participating in pools, and the pool id
        // we provide them is not persisted.
        // This means the pool id can vary.
        ClusterVO cluster = _clusterDao.findById(host.getClusterId());
        if (cluster.getGuid() == null) {
            cluster.setGuid(startup.getPool());
            _clusterDao.update(cluster.getId(), cluster);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Setting up host " + agentId);
        }

        HostEnvironment env = new HostEnvironment();
        SetupCommand setup = new SetupCommand(env);
        if (!host.isSetup()) {
            setup.setNeedSetup(true);
        }

        try {
            SetupAnswer answer = (SetupAnswer)_agentMgr.send(agentId, setup);
            if (answer != null && answer.getResult()) {
                host.setSetup(true);
                // TODO: clean up magic numbers below
                host.setLastPinged((System.currentTimeMillis() >> 10) - 5 * 60);
                _hostDao.update(host.getId(), host);
                if (answer.needReconnect()) {
                    throw new ConnectionException(false, "Reinitialize agent after setup.");
                }
                return;
            } else {
                String reason = answer.getDetails();
                if (reason == null) {
                    reason = " details were null";
                }
                logger.warn("Unable to setup agent " + agentId + " due to " + reason);
            }
            // Error handling borrowed from XcpServerDiscoverer, may need to be
            // updated.
        } catch (AgentUnavailableException e) {
            logger.warn("Unable to setup agent " + agentId + " because it became unavailable.", e);
        } catch (OperationTimedoutException e) {
            logger.warn("Unable to setup agent " + agentId + " because it timed out", e);
        }
        throw new ConnectionException(true, "Reinitialize agent after setup.");
    }

    @Override
    public final boolean processDisconnect(final long agentId, final Status state) {
        return false;
    }

    @Override
    public void processHostAboutToBeRemoved(long hostId) {
    }

    @Override
    public void processHostRemoved(long hostId, long clusterId) {
    }

    @Override
    public final boolean isRecurring() {
        return false;
    }

    @Override
    public final int getTimeout() {
        return 0;
    }

    @Override
    public final boolean processTimeout(final long agentId, final long seq) {
        return false;
    }

    // End Listener implementation

    // Returns server component used by server manager to operate the plugin.
    // Server component is a ServerResource. If a connected agent is used, the
    // ServerResource is
    // ignored in favour of another created in response to
    @Override
    public final Map<? extends ServerResource, Map<String, String>> find(final long dcId, final Long podId, final Long clusterId, final URI uri, final String username,
        final String password, final List<String> hostTags) throws DiscoveryException {

        if (logger.isInfoEnabled()) {
            logger.info("Discover host. dc(zone): " + dcId + ", pod: " + podId + ", cluster: " + clusterId + ", uri host: " + uri.getHost());
        }

        // Assertions
        if (podId == null) {
            if (logger.isInfoEnabled()) {
                logger.info("No pod is assigned, skipping the discovery in" + " Hyperv discoverer");
            }
            return null;
        }
        ClusterVO cluster = _clusterDao.findById(clusterId); // ClusterVO exists
        // in the
        // database
        if (cluster == null) {
            if (logger.isInfoEnabled()) {
                logger.info("No cluster in database for cluster id " + clusterId);
            }
            return null;
        }
        if (cluster.getHypervisorType() != HypervisorType.Hyperv) {
            if (logger.isInfoEnabled()) {
                logger.info("Cluster " + clusterId + "is not for Hyperv hypervisors");
            }
            return null;
        }
        if (!uri.getScheme().equals("http")) {
            String msg = "urlString is not http so we're not taking care of" + " the discovery for this: " + uri;
            logger.debug(msg);
            return null;
        }

        try {
            String hostname = uri.getHost();
            InetAddress ia = InetAddress.getByName(hostname);
            String agentIp = ia.getHostAddress();
            String uuidSeed = agentIp;
            String guidWithTail = calcServerResourceGuid(uuidSeed) + "-HypervResource";

            if (_resourceMgr.findHostByGuid(guidWithTail) != null) {
                logger.debug("Skipping " + agentIp + " because " + guidWithTail + " is already in the database.");
                return null;
            }

            logger.info("Creating" + HypervDirectConnectResource.class.getName() + " HypervDirectConnectResource for zone/pod/cluster " + dcId + "/" + podId + "/" +
                clusterId);

            // Some Hypervisors organise themselves in pools.
            // The startup command tells us what pool they are using.
            // In the meantime, we have to place a GUID corresponding to the
            // pool in the database
            // This GUID may change.
            if (cluster.getGuid() == null) {
                cluster.setGuid(UUID.nameUUIDFromBytes(String.valueOf(clusterId).getBytes(Charset.forName("UTF-8"))).toString());
                _clusterDao.update(clusterId, cluster);
            }

            // Settings required by all server resources managing a hypervisor
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("zone", Long.toString(dcId));
            params.put("pod", Long.toString(podId));
            params.put("cluster", Long.toString(clusterId));
            params.put("guid", guidWithTail);
            params.put("ipaddress", agentIp);

            // Hyper-V specific settings
            Map<String, String> details = new HashMap<String, String>();
            details.put("url", uri.getHost());
            details.put("username", username);
            details.put("password", password);
            details.put("cluster.guid", cluster.getGuid());

            params.putAll(details);

            params.put("router.aggregation.command.each.timeout", _configDao.getValue(Config.RouterAggregationCommandEachTimeout.toString()));

            HypervDirectConnectResource resource = new HypervDirectConnectResource();
            resource.configure(agentIp, params);

            // Assert
            // TODO: test by using bogus URL and bogus virtual path in URL
            ReadyCommand ping = new ReadyCommand();
            Answer pingAns = resource.executeRequest(ping);
            if (pingAns == null || !pingAns.getResult()) {
                String errMsg = "Agent not running, or no route to agent on at " + uri;
                logger.debug(errMsg);
                throw new DiscoveryException(errMsg);
            }

            Map<HypervDirectConnectResource, Map<String, String>> resources = new HashMap<HypervDirectConnectResource, Map<String, String>>();
            resources.put(resource, details);

            // TODO: does the resource have to create a connection?
            return resources;
        } catch (ConfigurationException e) {
            _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_HOST, dcId, podId, "Unable to add " + uri.getHost(), "Error is " + e.getMessage());
            logger.warn("Unable to instantiate " + uri.getHost(), e);
        } catch (UnknownHostException e) {
            _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_HOST, dcId, podId, "Unable to add " + uri.getHost(), "Error is " + e.getMessage());

            logger.warn("Unable to instantiate " + uri.getHost(), e);
        } catch (Exception e) {
            String msg = " can't setup agent, due to " + e.toString() + " - " + e.getMessage();
            logger.warn(msg);
        }
        return null;
    }

    /**
     * Encapsulate GUID calculation in public method to allow access to test
     * programs. Works by converting a string to a GUID using
     * UUID.nameUUIDFromBytes
     *
     * @param uuidSeed
     *            string to use to generate GUID
     *
     * @return GUID in form of a string.
     */
    public static String calcServerResourceGuid(final String uuidSeed) {
        String guid = UUID.nameUUIDFromBytes(uuidSeed.getBytes(Charset.forName("UTF-8"))).toString();
        return guid;
    }

    // Adapter implementation: (facilitates plug in loading)
    // Required because Discoverer extends Adapter
    // Overrides Adapter.configure to always return true
    // Inherit Adapter.getName
    // Inherit Adapter.stop
    // Inherit Adapter.start
    @Override
    public final boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        // TODO: allow timeout on we HTTPRequests to be configured
        _agentMgr.registerForHostEvents(this, true, false, true);
        _resourceMgr.registerResourceStateAdapter(this.getClass().getSimpleName(), this);
        return true;
    }

    // end of Adapter

    @Override
    public void postDiscovery(final List<HostVO> hosts, final long msId) throws DiscoveryException {
    }

    @Override
    public final Hypervisor.HypervisorType getHypervisorType() {
        return Hypervisor.HypervisorType.Hyperv;
    }

    // TODO: verify that it is okay to return true on null hypervisor
    @Override
    public final boolean matchHypervisor(final String hypervisor) {
        if (hypervisor == null) {
            return true;
        }
        return Hypervisor.HypervisorType.Hyperv.toString().equalsIgnoreCase(hypervisor);
    }

    // end of Discoverer

    // ResourceStateAdapter
    @Override
    public final HostVO createHostVOForConnectedAgent(final HostVO host, final StartupCommand[] cmd) {
        return null;
    }

    // TODO: add test for method
    @Override
    public final HostVO createHostVOForDirectConnectAgent(final HostVO host, final StartupCommand[] startup, final ServerResource resource,
        final Map<String, String> details, final List<String> hostTags) {
        StartupCommand firstCmd = startup[0];
        if (!(firstCmd instanceof StartupRoutingCommand)) {
            return null;
        }

        StartupRoutingCommand ssCmd = ((StartupRoutingCommand)firstCmd);
        if (ssCmd.getHypervisorType() != HypervisorType.Hyperv) {
            return null;
        }

        logger.info("Host: " + host.getName() + " connected with hypervisor type: " + HypervisorType.Hyperv + ". Checking CIDR...");

        HostPodVO pod = _podDao.findById(host.getPodId());
        DataCenterVO dc = _dcDao.findById(host.getDataCenterId());

        _resourceMgr.checkCIDR(pod, dc, ssCmd.getPrivateIpAddress(), ssCmd.getPrivateNetmask());

        return _resourceMgr.fillRoutingHostVO(host, ssCmd, HypervisorType.Hyperv, details, hostTags);
    }

    // TODO: add test for method
    @Override
    public final DeleteHostAnswer deleteHost(final HostVO host, final boolean isForced, final boolean isForceDeleteStorage) throws UnableDeleteHostException {
        // assert
        if (host.getType() != Host.Type.Routing || host.getHypervisorType() != HypervisorType.Hyperv) {
            return null;
        }
        _resourceMgr.deleteRoutingHost(host, isForced, isForceDeleteStorage);
        return new DeleteHostAnswer(true);
    }

    @Override
    public final boolean stop() {
        _resourceMgr.unregisterResourceStateAdapter(this.getClass().getSimpleName());
        return super.stop();
    }
    // end of ResourceStateAdapter

}
