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

package org.apache.cloudstack.hypervisor.external.discoverer;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.extension.ExtensionResourceMap;
import org.apache.cloudstack.framework.extensions.dao.ExtensionDao;
import org.apache.cloudstack.framework.extensions.dao.ExtensionResourceMapDao;
import org.apache.cloudstack.framework.extensions.manager.ExtensionsManager;
import org.apache.cloudstack.framework.extensions.vo.ExtensionResourceMapVO;
import org.apache.cloudstack.framework.extensions.vo.ExtensionVO;
import org.apache.cloudstack.hypervisor.external.resource.ExternalResource;

import com.cloud.agent.AgentManager;
import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.dc.ClusterVO;
import com.cloud.exception.ConnectionException;
import com.cloud.exception.DiscoveryException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.resource.Discoverer;
import com.cloud.resource.DiscovererBase;
import com.cloud.resource.ResourceStateAdapter;
import com.cloud.resource.ServerResource;
import com.cloud.resource.UnableDeleteHostException;
import com.cloud.utils.UuidUtils;

public class ExternalServerDiscoverer extends DiscovererBase implements Discoverer, Listener, ResourceStateAdapter {

    @Inject
    AgentManager agentManager;

    @Inject
    ExtensionDao extensionDao;

    @Inject
    ExtensionResourceMapDao extensionResourceMapDao;

    @Inject
    ExtensionsManager extensionsManager;

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
        agentManager.registerForHostEvents(this, true, false, true);
        _resourceMgr.registerResourceStateAdapter(this.getClass().getSimpleName(), this);
        return true;
    }

    protected String getResourceGuidFromName(String name) {
        return "External:" + UuidUtils.nameUUIDFromBytes(name.getBytes());
    }

    protected void addExtensionDataToResourceParams(ExtensionVO extension, Map<String, Object> params) {
        params.put("extensionName", extension.getName());
        params.put("extensionRelativePath", extension.getRelativePath());
        params.put("extensionState", extension.getState());
        params.put("extensionPathReady", extension.isPathReady());
    }

    @Override
    public Map<? extends ServerResource, Map<String, String>> find(long dcId, Long podId, Long clusterId, URI uri, String username, String password, List<String> hostTags) throws DiscoveryException {
        Map<ExternalResource, Map<String, String>> resources;
        String errorMessage;
        if (clusterId == null) {
            errorMessage = "Must specify cluster Id when adding host";
            logger.error(errorMessage);
            throw new DiscoveryException(errorMessage);
        }
        ClusterVO cluster = _clusterDao.findById(clusterId);
        if (cluster == null || (cluster.getHypervisorType() != Hypervisor.HypervisorType.External)) {
            errorMessage = "Invalid cluster id or cluster is not for External hypervisors";
            logger.error(errorMessage);
            throw new DiscoveryException(errorMessage);
        }
        if (podId == null) {
            errorMessage = "Must specify pod when adding host";
            logger.error(errorMessage);
            throw new DiscoveryException(errorMessage);
        }
        ExtensionResourceMapVO extensionResourceMapVO = extensionResourceMapDao.findByResourceIdAndType(clusterId,
                ExtensionResourceMap.ResourceType.Cluster);
        if (extensionResourceMapVO == null) {
            logger.error("External hypervisor {} must be registered with an extension when adding host",
                    cluster);
            throw new DiscoveryException(String.format("Cluster: %s is not registered with an extension",
                    cluster.getName()));
        }
        ExtensionVO extensionVO = extensionDao.findById(extensionResourceMapVO.getExtensionId());
        if (extensionVO == null) {
            logger.error("Extension ID: {} to which {} cluster is registered is not found",
                    extensionResourceMapVO.getExtensionId(), cluster);
            throw new DiscoveryException(String.format("Cluster: %s is registered with an inexistent extension",
                    cluster.getName()));
        }
        if (cluster.getGuid() == null) {
            cluster.setGuid(UUID.randomUUID().toString());
            _clusterDao.update(clusterId, cluster);
        }
        Map<String, Object> params = new HashMap<>();
        params.put("username", username);
        params.put("password", password);
        params.put("zone", Long.toString(dcId));
        params.put("pod", Long.toString(podId));
        params.put("cluster", Long.toString(clusterId));
        String name = uri.toString();
        params.put("guid", getResourceGuidFromName(name));
        addExtensionDataToResourceParams(extensionVO, params);
        resources = createAgentResource(name, params);
        if (resources == null) {
            throw new DiscoveryException("Failed to create external agent");
        }
        return resources;
    }

    @Override
    protected HashMap<String, Object> buildConfigParams(HostVO host) {
        HashMap<String, Object> params = super.buildConfigParams(host);
        long clusterId = Long.parseLong((String) params.get("cluster"));
        ExtensionResourceMapVO extensionResourceMapVO =
                extensionResourceMapDao.findByResourceIdAndType(clusterId,
                        ExtensionResourceMap.ResourceType.Cluster);
        if (extensionResourceMapVO == null) {
            logger.debug("Cluster ID: {} not registered with any extension", clusterId);
            return params;
        }
        ExtensionVO extensionVO = extensionDao.findById(extensionResourceMapVO.getExtensionId());
        if (extensionVO == null) {
            logger.error("Extension with ID: {} not found", extensionResourceMapVO.getExtensionId());
            return params;
        }
        addExtensionDataToResourceParams(extensionVO, params);
        return params;
    }

    private Map<ExternalResource, Map<String, String>> createAgentResource(String name, Map<String, Object> params) {
        try {
            logger.info("Creating external server resource: {}", name);
            Map<String, String> args = new HashMap<>();
            Map<ExternalResource, Map<String, String>> newResources = new HashMap<>();
            ExternalResource agentResource;
            synchronized (this) {
                agentResource = new ExternalResource();
                try {
                    agentResource.start();
                    agentResource.configure(name, params);
                    args.put("guid", (String)params.get("guid"));
                    newResources.put(agentResource, args);
                } catch (ConfigurationException e) {
                    logger.error("Error while configuring server resource {}", e.getMessage());
                }
            }
            return newResources;
        } catch (Exception ex) {
            logger.warn("Caught creating external server resources {}", name, ex);
        }
        return null;
    }

    @Override
    public void postDiscovery(List<HostVO> hosts, long msId) {
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
    public HostVO createHostVOForDirectConnectAgent(HostVO host, StartupCommand[] startup, ServerResource resource,
                    Map<String, String> details, List<String> hostTags) {
        StartupCommand firstCmd = startup[0];
        if (!(firstCmd instanceof StartupRoutingCommand)) {
            return null;
        }
        StartupRoutingCommand ssCmd = (StartupRoutingCommand)firstCmd;
        if (ssCmd.getHypervisorType() != Hypervisor.HypervisorType.External) {
            return null;
        }
        ExtensionResourceMapVO extensionResourceMapVO = extensionResourceMapDao.findByResourceIdAndType(
                host.getClusterId(), ExtensionResourceMap.ResourceType.Cluster);
        if (extensionResourceMapVO != null) {
            ExtensionVO extension = extensionDao.findById(extensionResourceMapVO.getExtensionId());
            logger.debug("Creating {} for {}", host, extension);
            extensionsManager.prepareExtensionPathAcrossServers(extension);
        } else {
            logger.debug("Creating {}. No extension registered for cluster ID: {}", host, host.getClusterId());
        }
        return _resourceMgr.fillRoutingHostVO(host, ssCmd, Hypervisor.HypervisorType.External, details, hostTags);
    }

    @Override
    public DeleteHostAnswer deleteHost(HostVO host, boolean isForced, boolean isForceDeleteStorage) throws UnableDeleteHostException {
        return new DeleteHostAnswer(true);
    }
}
