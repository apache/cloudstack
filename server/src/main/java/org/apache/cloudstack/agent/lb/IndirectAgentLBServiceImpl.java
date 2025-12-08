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
package org.apache.cloudstack.agent.lb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.agent.lb.algorithm.IndirectAgentLBRoundRobinAlgorithm;
import org.apache.cloudstack.agent.lb.algorithm.IndirectAgentLBShuffleAlgorithm;
import org.apache.cloudstack.agent.lb.algorithm.IndirectAgentLBStaticAlgorithm;
import org.apache.cloudstack.config.ApiServiceConfiguration;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.commons.lang3.StringUtils;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.host.Host;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.resource.ResourceState;
import com.cloud.utils.component.ComponentLifecycleBase;
import com.cloud.utils.exception.CloudRuntimeException;

public class IndirectAgentLBServiceImpl extends ComponentLifecycleBase implements IndirectAgentLB, Configurable {

    public static final ConfigKey<String> IndirectAgentLBAlgorithm = new ConfigKey<>(String.class,
    "indirect.agent.lb.algorithm", "Advanced", "static",
            "The algorithm to be applied on the provided 'host' management server list that is sent to indirect agents. Allowed values are: static, roundrobin and shuffle.",
            true, ConfigKey.Scope.Global, null, null, null, null, null, ConfigKey.Kind.Select, "static,roundrobin,shuffle");

    public static final ConfigKey<Long> IndirectAgentLBCheckInterval = new ConfigKey<>("Advanced", Long.class,
            "indirect.agent.lb.check.interval", "0",
            "The interval in seconds after which agent should check and try to connect to its preferred host. Set 0 to disable it.",
            true, ConfigKey.Scope.Cluster);

    private static Map<String, org.apache.cloudstack.agent.lb.IndirectAgentLBAlgorithm> algorithmMap = new HashMap<>();

    @Inject
    private DataCenterDao dataCenterDao;
    @Inject
    private ClusterDao clusterDao;
    @Inject
    private HostDao hostDao;
    @Inject
    private AgentManager agentManager;

    private static final List<ResourceState> agentValidResourceStates = List.of(
            ResourceState.Enabled, ResourceState.Maintenance, ResourceState.Disabled,
            ResourceState.ErrorInMaintenance, ResourceState.PrepareForMaintenance);
    private static final List<Host.Type> agentValidHostTypes = List.of(Host.Type.Routing, Host.Type.ConsoleProxy,
            Host.Type.SecondaryStorage, Host.Type.SecondaryStorageVM);
    private static final List<Host.Type> agentNonRoutingHostTypes = List.of(Host.Type.ConsoleProxy,
            Host.Type.SecondaryStorage, Host.Type.SecondaryStorageVM);
    private static final List<Hypervisor.HypervisorType> agentValidHypervisorTypes = List.of(
            Hypervisor.HypervisorType.KVM, Hypervisor.HypervisorType.LXC);

    //////////////////////////////////////////////////////
    /////////////// Agent MSLB Methods ///////////////////
    //////////////////////////////////////////////////////

    @Override
    public List<String> getManagementServerList(final Long hostId, final Long dcId, final List<Long> orderedHostIdList) {
        final String msServerAddresses = ApiServiceConfiguration.ManagementServerAddresses.value();
        if (StringUtils.isEmpty(msServerAddresses)) {
            throw new CloudRuntimeException(String.format("No management server addresses are defined in '%s' setting",
                    ApiServiceConfiguration.ManagementServerAddresses.key()));
        }
        final List<String> msList = Arrays.asList(msServerAddresses.replace(" ", "").split(","));
        if (msList.size() == 1) {
            return msList;
        }

        final org.apache.cloudstack.agent.lb.IndirectAgentLBAlgorithm algorithm = getAgentMSLBAlgorithm();
        List<Long> hostIdList = orderedHostIdList;
        if (hostIdList == null) {
            hostIdList = algorithm.isHostListNeeded() ? getOrderedHostIdList(dcId) : new ArrayList<>();
        }

        // just in case we have a host in creating state make sure it is in the list:
        if (null != hostId && ! hostIdList.contains(hostId)) {
            logger.trace("adding requested host to host list as it does not seem to be there; {}", hostId);
            hostIdList.add(hostId);
        }
        return algorithm.sort(msList, hostIdList, hostId);
    }

    @Override
    public boolean compareManagementServerList(final Long hostId, final Long dcId, final List<String> receivedMSHosts, final String lbAlgorithm) {
        if (receivedMSHosts == null || receivedMSHosts.size() < 1) {
            return false;
        }
        if (getLBAlgorithmName() != lbAlgorithm) {
            return false;
        }
        final List<String> expectedMSList = getManagementServerList(hostId, dcId, null);
        final org.apache.cloudstack.agent.lb.IndirectAgentLBAlgorithm algorithm = getAgentMSLBAlgorithm();
        return algorithm.compare(expectedMSList, receivedMSHosts);
    }

    @Override
    public String getLBAlgorithmName() {
        return IndirectAgentLBAlgorithm.value();
    }

    @Override
    public Long getLBPreferredHostCheckInterval(final Long clusterId) {
        return IndirectAgentLBCheckInterval.valueIn(clusterId);
    }

    List<Long> getOrderedHostIdList(final Long dcId) {
        final List<Long> hostIdList = getAllAgentBasedHostsFromDB(dcId, null);
        hostIdList.sort(Comparator.comparingLong(x -> x));
        return hostIdList;
    }

    private List<Long> getAllAgentBasedNonRoutingHostsFromDB(final Long zoneId) {
        return hostDao.findHostIdsByZoneClusterResourceStateTypeAndHypervisorType(zoneId, null,
                agentValidResourceStates, agentNonRoutingHostTypes, agentValidHypervisorTypes);
    }

    private List<Long> getAllAgentBasedRoutingHostsFromDB(final Long zoneId, final Long clusterId) {
        return hostDao.findHostIdsByZoneClusterResourceStateTypeAndHypervisorType(zoneId, clusterId,
                agentValidResourceStates, List.of(Host.Type.Routing), agentValidHypervisorTypes);
    }

    private List<Long> getAllAgentBasedHostsFromDB(final Long zoneId, final Long clusterId) {
        return hostDao.findHostIdsByZoneClusterResourceStateTypeAndHypervisorType(zoneId, clusterId,
                agentValidResourceStates, agentValidHostTypes, agentValidHypervisorTypes);
    }

    private org.apache.cloudstack.agent.lb.IndirectAgentLBAlgorithm getAgentMSLBAlgorithm() {
        final String algorithm = getLBAlgorithmName();
        if (algorithmMap.containsKey(algorithm)) {
            return algorithmMap.get(algorithm);
        }
        throw new CloudRuntimeException(String.format("Algorithm configured for '%s' not found, valid values are: %s",
                IndirectAgentLBAlgorithm.key(), algorithmMap.keySet()));
    }

    ////////////////////////////////////////////////////////////
    /////////////// Agent MSLB Configuration ///////////////////
    ////////////////////////////////////////////////////////////

    @Override
    public void propagateMSListToAgents() {
        logger.debug("Propagating management server list update to agents");
        final String lbAlgorithm = getLBAlgorithmName();
        final Long globalLbCheckInterval = getLBPreferredHostCheckInterval(null);
        List<DataCenterVO> zones = dataCenterDao.listAll();
        for (DataCenterVO zone : zones) {
            List<Long> zoneHostIds = new ArrayList<>();
            List<Long> nonRoutingHostIds = getAllAgentBasedNonRoutingHostsFromDB(zone.getId());
            zoneHostIds.addAll(nonRoutingHostIds);
            Map<Long, List<Long>> clusterHostIdsMap = new HashMap<>();
            List<Long> clusterIds = clusterDao.listAllClusterIds(zone.getId());
            for (Long clusterId : clusterIds) {
                List<Long> hostIds = getAllAgentBasedRoutingHostsFromDB(zone.getId(), clusterId);
                clusterHostIdsMap.put(clusterId, hostIds);
                zoneHostIds.addAll(hostIds);
            }
            zoneHostIds.sort(Comparator.comparingLong(x -> x));
            for (Long nonRoutingHostId : nonRoutingHostIds) {
                setupMSList(nonRoutingHostId, zone.getId(), zoneHostIds, lbAlgorithm, globalLbCheckInterval);
            }
            for (Long clusterId : clusterIds) {
                final Long clusterLbCheckInterval = getLBPreferredHostCheckInterval(clusterId);
                List<Long> hostIds = clusterHostIdsMap.get(clusterId);
                for (Long hostId : hostIds) {
                    setupMSList(hostId, zone.getId(), zoneHostIds, lbAlgorithm, clusterLbCheckInterval);
                }
            }
        }
    }

    private void setupMSList(final Long hostId, final Long dcId, final List<Long> orderedHostIdList, final String lbAlgorithm, final Long lbCheckInterval) {
        final List<String> msList = getManagementServerList(hostId, dcId, orderedHostIdList);
        final SetupMSListCommand cmd = new SetupMSListCommand(msList, lbAlgorithm, lbCheckInterval);
        final Answer answer = agentManager.easySend(hostId, cmd);
        if (answer == null || !answer.getResult()) {
            logger.warn(String.format("Failed to setup management servers list to the agent of ID: %d", hostId));
        }
    }

    private void configureAlgorithmMap() {
        final List<org.apache.cloudstack.agent.lb.IndirectAgentLBAlgorithm> algorithms = new ArrayList<>();
        algorithms.add(new IndirectAgentLBStaticAlgorithm());
        algorithms.add(new IndirectAgentLBRoundRobinAlgorithm());
        algorithms.add(new IndirectAgentLBShuffleAlgorithm());
        algorithmMap.clear();
        for (org.apache.cloudstack.agent.lb.IndirectAgentLBAlgorithm algorithm : algorithms) {
            algorithmMap.put(algorithm.getName(), algorithm);
        }
    }

    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        configureAlgorithmMap();
        return true;
    }

    @Override
    public String getConfigComponentName() {
        return IndirectAgentLBServiceImpl.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {
                IndirectAgentLBAlgorithm,
                IndirectAgentLBCheckInterval
        };
    }
}
