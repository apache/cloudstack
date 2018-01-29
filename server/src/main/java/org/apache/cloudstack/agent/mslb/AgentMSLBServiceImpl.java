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
package org.apache.cloudstack.agent.mslb;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.event.EventTypes;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.resource.ResourceListener;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceState;
import com.cloud.resource.ServerResource;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.agent.mslb.algorithm.AgentMSLBRoundRobinAlgorithm;
import org.apache.cloudstack.agent.mslb.algorithm.AgentMSLBShuffleAlgorithm;
import org.apache.cloudstack.agent.mslb.algorithm.AgentMSLBStaticAlgorithm;
import org.apache.cloudstack.config.ApiServiceConfiguration;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.log4j.Logger;

import com.cloud.utils.component.ComponentLifecycleBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.common.base.Strings;

public class AgentMSLBServiceImpl extends ComponentLifecycleBase implements AgentMSLB, Configurable, ResourceListener {
    public static final Logger LOG = Logger.getLogger(AgentMSLBServiceImpl.class);

    public static final ConfigKey<String> ConnectedAgentLBAlgorithm = new ConfigKey<>("Advanced", String.class,
            "connected.agent.mslb.algorithm", "static",
            "The algorithm to applied on the provided 'host' management server list that is sent to indirect agents. Allowed values are: static, roundrobin and shuffle.",
            true, ConfigKey.Scope.Global);

    private static Map<String, AgentMSLBAlgorithm> algorithmMap = new HashMap<>();

    @Inject
    ResourceManager resourceManager;
    @Inject
    MessageBus messageBus;
    @Inject
    AgentManager agentManager;

    /**
     * Return a map of (zoneid, list of host ids)
     * @return map
     */
    protected Map<Long, List<Long>> getHostsPerZone() {
        List<HostVO> allHosts = resourceManager.listAllHostsInAllZonesByType(Host.Type.Routing);
        if (allHosts == null) {
            return null;
        }
        return allHosts.stream()
                .collect(
                        Collectors.groupingBy(
                               HostVO::getDataCenterId,
                               Collectors.mapping(HostVO::getId, Collectors.toList()
                        )));
    }

    /**
     * Propagate management servers lists to agents
     */
    private void propagateListToAgents() {
        LOG.debug("Propagating management lists to agents");
        Map<Long, List<Long>> hostsPerZone = getHostsPerZone();
        for (Long zoneId : hostsPerZone.keySet()) {
            List<Long> hostIds = hostsPerZone.get(zoneId);
            for (Long hostId : hostIds) {
                List<String> msList = getManagementServerList(hostId, zoneId);
                SetupManagementServersListCommand cmd = new SetupManagementServersListCommand(msList);
                Answer answer = agentManager.easySend(hostId, cmd);
                if (answer == null || !answer.getResult()) {
                    LOG.warn("Error sending management servers list to agent" + hostId);
                }
            }
        }
    }

    private void initMessageBusListener() {
        messageBus.subscribe(EventTypes.EVENT_CONFIGURATION_VALUE_EDIT, (senderAddress, subject, args) -> {
            String globalSettingUpdated = (String) args;
            if (globalSettingUpdated.equals(ApiServiceConfiguration.ManagementServerAddresses.key()) ||
                    globalSettingUpdated.equals(ConnectedAgentLBAlgorithm.key())) {
                propagateListToAgents();
            }
        });
    }

    @Override
    public List<String> getManagementServerList(Long hostId, Long dcId) {
        final String msServerAddresses = ApiServiceConfiguration.ManagementServerAddresses.value();
        if (Strings.isNullOrEmpty(msServerAddresses)) {
            throw new CloudRuntimeException(String.format("No management server addresses are defined in '%s' setting",
                    ApiServiceConfiguration.ManagementServerAddresses.key()));
        }

        List<Long> orderedHostIds = getOrderedRunningHostIds(dcId);
        List<String> msList = Arrays.asList(msServerAddresses.replace(" ", "").split(","));
        AgentMSLBAlgorithm algorithm = getAgentMSLBAlgorithm();
        return algorithm.getMSList(msList, orderedHostIds, hostId);
    }

    @Override
    public boolean isManagementServerListUpToDate(Long hostId, Long dcId, List<String> receivedMgmtHosts) {
        List<String> managementServerList = getManagementServerList(hostId, dcId);
        AgentMSLBAlgorithm algorithm = getAgentMSLBAlgorithm();
        return algorithm.isMSListEqual(managementServerList, receivedMgmtHosts);
    }

    protected List<Long> getOrderedRunningHostIds(Long dcId) {
        List<HostVO> hosts = resourceManager.listAllHostsInOneZoneByType(Host.Type.Routing, dcId);
        if (hosts != null) {
            return hosts.stream()
                    .filter(x -> !x.getHypervisorType().equals(Hypervisor.HypervisorType.VMware) &&
                                    !x.getHypervisorType().equals(Hypervisor.HypervisorType.XenServer) &&
                                    !x.getHypervisorType().equals(Hypervisor.HypervisorType.Hyperv) &&
                                    x.getRemoved() == null && x.getResourceState().equals(ResourceState.Enabled))
                    .map(x -> x.getId())
                    .sorted((x,y) -> Long.compare(x, y))
                    .collect(Collectors.toList());
        }
        return null;
    }

    private AgentMSLBAlgorithm getAgentMSLBAlgorithm() {
        final String algorithm = ConnectedAgentLBAlgorithm.value();
        if (algorithmMap.containsKey(algorithm)) {
            return algorithmMap.get(algorithm);
        }
        throw new CloudRuntimeException(String.format("Algorithm configured for '%s' not found, valid values are: %s",
                ConnectedAgentLBAlgorithm.key(), algorithmMap.keySet()));
    }

    private void initAlgorithmMap() {
        final List<AgentMSLBAlgorithm> algorithms = new ArrayList<>();
        algorithms.add(new AgentMSLBStaticAlgorithm());
        algorithms.add(new AgentMSLBRoundRobinAlgorithm());
        algorithms.add(new AgentMSLBShuffleAlgorithm());
        algorithmMap.clear();
        for (AgentMSLBAlgorithm algorithm : algorithms) {
            algorithmMap.put(algorithm.getName(), algorithm);
        }
    }

    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        initAlgorithmMap();
        initMessageBusListener();
        return true;
    }

    @Override
    public boolean start() {
        resourceManager.registerResourceEvent(ResourceListener.EVENT_DISCOVER_AFTER, this);
        return true;
    }

    @Override
    public boolean stop() {
        resourceManager.unregisterResourceEvent(this);
        return true;
    }

    @Override
    public String getConfigComponentName() {
        return AgentMSLBServiceImpl.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {
                ConnectedAgentLBAlgorithm
        };
    }

    @Override
    public void processDiscoverEventBefore(Long dcid, Long podId, Long clusterId, URI uri, String username, String password, List<String> hostTags) {
    }

    @Override
    public void processDiscoverEventAfter(Map<? extends ServerResource, Map<String, String>> resources) {
        propagateListToAgents();
    }

    @Override
    public void processDeleteHostEventBefore(Host host) {
    }

    @Override
    public void processDeletHostEventAfter(Host host) {
    }

    @Override
    public void processCancelMaintenaceEventBefore(Long hostId) {
    }

    @Override
    public void processCancelMaintenaceEventAfter(Long hostId) {
    }

    @Override
    public void processPrepareMaintenaceEventBefore(Long hostId) {
    }

    @Override
    public void processPrepareMaintenaceEventAfter(Long hostId) {
    }
}