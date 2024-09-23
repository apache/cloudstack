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
package com.cloud.hypervisor.external.discoverer;

import com.cloud.agent.AgentManager;
import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.agent.manager.ExternalAgentManager;
import com.cloud.dc.ClusterDetailsVO;
import com.cloud.dc.ClusterVO;
import com.cloud.exception.ConnectionException;
import com.cloud.exception.DiscoveryException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.resource.Discoverer;
import com.cloud.resource.DiscovererBase;
import com.cloud.hypervisor.external.resource.ExternalResourceBase;
import com.cloud.resource.ResourceStateAdapter;
import com.cloud.resource.ServerResource;
import com.cloud.resource.UnableDeleteHostException;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ExternalServerDiscoverer extends DiscovererBase implements Discoverer, Listener, ResourceStateAdapter {

    private static final Logger s_logger = Logger.getLogger(ExternalServerDiscoverer.class);

    @Inject
    private AgentManager _agentMgr;

    @Inject
    ExternalAgentManager _externalAgentMgr = null;

    @Override
    public boolean processAnswers(long agentId, long seq, Answer[] answers) {
        return false;
    }

    @Override
    public boolean processCommands(long agentId, long seq, Command[] commands) {
        return false;
    }

    @Override
    public AgentControlAnswer processControlCommand(long agentId, AgentControlCommand cmd) {
        return null;
    }

    @Override
    public void processHostAdded(long hostId) {

    }

    @Override
    public void processConnect(Host host, StartupCommand cmd, boolean forRebalance) throws ConnectionException {

    }

    @Override
    public boolean processDisconnect(long agentId, Status state) {
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
        return 0;
    }

    @Override
    public boolean processTimeout(long agentId, long seq) {
        return false;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        _agentMgr.registerForHostEvents(this, true, false, true);
        _resourceMgr.registerResourceStateAdapter(this.getClass().getSimpleName(), this);
        return true;
    }

    @Override
    public Map<? extends ServerResource, Map<String, String>> find(long dcId, Long podId, Long clusterId, URI uri, String username, String password, List<String> hostTags) throws DiscoveryException {
        Map<ExternalResourceBase, Map<String, String>> resources;

        try {
            String cluster = null;
            if (clusterId == null) {
                String msg = "must specify cluster Id when adding host";
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(msg);
                }
                throw new RuntimeException(msg);
            } else {
                ClusterVO clu = _clusterDao.findById(clusterId);
                if (clu == null || (clu.getHypervisorType() != Hypervisor.HypervisorType.External)) {
                    if (s_logger.isInfoEnabled())
                        s_logger.info("invalid cluster id or cluster is not for Simulator hypervisors");
                    return null;
                }
                cluster = Long.toString(clusterId);
                if (clu.getGuid() == null) {
                    clu.setGuid(UUID.randomUUID().toString());
                }
                _clusterDao.update(clusterId, clu);
            }

            String pod;
            if (podId == null) {
                String msg = "must specify pod Id when adding host";
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(msg);
                }
                throw new RuntimeException(msg);
            } else {
                pod = Long.toString(podId);
            }

            Map<String, Object> params = new HashMap<String, Object>();
            params.put("username", username);
            params.put("password", password);
            params.put("zone", Long.toString(dcId));
            params.put("pod", pod);
            params.put("cluster", cluster);
            params.put("guid", uri.toString());

            if (_params.get(ApiConstants.EXTERNAL_PROVISIONER) == null) {
                s_logger.error("External provisioner must be defined to discover");
                return null;
            }
            params.put(ApiConstants.EXTERNAL_PROVISIONER, _params.get(ApiConstants.EXTERNAL_PROVISIONER));

            resources = createAgentResources(params);
            return resources;
        } catch (Exception ex) {
            s_logger.error("Exception when discovering external hosts: " + ex.getMessage());
        }
        return null;
    }

    @Override
    protected HashMap<String, Object> buildConfigParams(HostVO host) {
        HashMap<String, Object> params = super.buildConfigParams(host);
        if (host.getClusterId() != null) {
            ClusterDetailsVO externalProvisioner = _clusterDetailsDao.findDetail(host.getClusterId(), ApiConstants.EXTERNAL_PROVISIONER);
            params.put(ApiConstants.EXTERNAL_PROVISIONER, externalProvisioner.getValue());
        }

        return params;
    }

    private Map<ExternalResourceBase, Map<String, String>> createAgentResources(Map<String, Object> params) {
        try {
            s_logger.info("Creating External Server Resources");
            return _externalAgentMgr.createServerResources(params);
        } catch (Exception ex) {
            s_logger.warn("Caught exception at agent resource creation: " + ex.getMessage(), ex);
        }
        return null;
    }

    @Override
    public void postDiscovery(List<HostVO> hosts, long msId) throws DiscoveryException {

    }

    @Override
    public boolean matchHypervisor(String hypervisor) {
        if (hypervisor == null)
            return true;

        return getHypervisorType().toString().equalsIgnoreCase(hypervisor);
    }

    @Override
    public Hypervisor.HypervisorType getHypervisorType() {
        return Hypervisor.HypervisorType.External;
    }

    @Override
    public HostVO createHostVOForConnectedAgent(HostVO host, StartupCommand[] cmd) {
        return null;
    }

    @Override
    public HostVO createHostVOForDirectConnectAgent(HostVO host, StartupCommand[] startup, ServerResource resource, Map<String, String> details, List<String> hostTags) {
        StartupCommand firstCmd = startup[0];
        if (!(firstCmd instanceof StartupRoutingCommand)) {
            return null;
        }

        StartupRoutingCommand ssCmd = ((StartupRoutingCommand)firstCmd);
        if (ssCmd.getHypervisorType() != Hypervisor.HypervisorType.External) {
            return null;
        }

        return _resourceMgr.fillRoutingHostVO(host, ssCmd, Hypervisor.HypervisorType.External, details, hostTags);
    }

    @Override
    public DeleteHostAnswer deleteHost(HostVO host, boolean isForced, boolean isForceDeleteStorage) throws UnableDeleteHostException {
        return new DeleteHostAnswer(true);
    }
}
