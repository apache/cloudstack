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
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
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
import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.resource.ResourceState;
import com.cloud.utils.component.ComponentLifecycleBase;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.commons.lang3.StringUtils;

public class IndirectAgentLBServiceImpl extends ComponentLifecycleBase implements IndirectAgentLB, Configurable {
    public static final Logger LOG = Logger.getLogger(IndirectAgentLBServiceImpl.class);

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
    private HostDao hostDao;
    @Inject
    private AgentManager agentManager;

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

        List<Long> hostIdList = orderedHostIdList;
        if (hostIdList == null) {
            hostIdList = getOrderedHostIdList(dcId);
        }

        // just in case we have a host in creating state make sure it is in the list:
        if (null != hostId && ! hostIdList.contains(hostId)) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("adding requested host to host list as it does not seem to be there; " + hostId);
            }
            hostIdList.add(hostId);
        }

        final org.apache.cloudstack.agent.lb.IndirectAgentLBAlgorithm algorithm = getAgentMSLBAlgorithm();
        final List<String> msList = Arrays.asList(msServerAddresses.replace(" ", "").split(","));
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
        final List<Long> hostIdList = new ArrayList<>();
        for (final Host host : getAllAgentBasedHosts()) {
            if (host.getDataCenterId() == dcId) {
                hostIdList.add(host.getId());
            }
        }
        Collections.sort(hostIdList, new Comparator<Long>() {
            @Override
            public int compare(Long x, Long y) {
                return Long.compare(x,y);
            }
        });
        return hostIdList;
    }

    private List<Host> getAllAgentBasedHosts() {
        final List<HostVO> allHosts = hostDao.listAll();
        if (allHosts == null) {
            return new ArrayList<>();
        }
        final List <Host> agentBasedHosts = new ArrayList<>();
        for (final Host host : allHosts) {
            conditionallyAddHost(agentBasedHosts, host);
        }
        return agentBasedHosts;
    }

    private void conditionallyAddHost(List<Host> agentBasedHosts, Host host) {
        if (host == null) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("trying to add no host to a list");
            }
            return;
        }

        EnumSet<ResourceState> allowedStates = EnumSet.of(
                ResourceState.Enabled,
                ResourceState.Maintenance,
                ResourceState.Disabled,
                ResourceState.ErrorInMaintenance,
                ResourceState.PrepareForMaintenance);
        // so the remaining EnumSet<ResourceState> disallowedStates = EnumSet.complementOf(allowedStates)
        // would be {ResourceState.Creating, ResourceState.Error};
        if (!allowedStates.contains(host.getResourceState())) {
            if (LOG.isTraceEnabled()) {
                LOG.trace(String.format("host is in '%s' state, not adding to the host list, (id = %s)", host.getResourceState(), host.getUuid()));
            }
            return;
        }

        if (host.getType() != Host.Type.Routing
                && host.getType() != Host.Type.ConsoleProxy
                && host.getType() != Host.Type.SecondaryStorage
                && host.getType() != Host.Type.SecondaryStorageVM) {
            if (LOG.isTraceEnabled()) {
                LOG.trace(String.format("host is of wrong type, not adding to the host list, (id = %s, type = %s)", host.getUuid(), host.getType()));
            }
            return;
        }

        if (host.getHypervisorType() != null
                && ! (host.getHypervisorType() == Hypervisor.HypervisorType.KVM || host.getHypervisorType() == Hypervisor.HypervisorType.LXC)) {

            if (LOG.isTraceEnabled()) {
                LOG.trace(String.format("hypervisor is not the right type, not adding to the host list, (id = %s, hypervisortype = %s)", host.getUuid(), host.getHypervisorType()));
            }
            return;
        }

        agentBasedHosts.add(host);
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
        LOG.debug("Propagating management server list update to agents");
        final String lbAlgorithm = getLBAlgorithmName();
        final Map<Long, List<Long>> dcOrderedHostsMap = new HashMap<>();
        for (final Host host : getAllAgentBasedHosts()) {
            final Long dcId = host.getDataCenterId();
            if (!dcOrderedHostsMap.containsKey(dcId)) {
                dcOrderedHostsMap.put(dcId, getOrderedHostIdList(dcId));
            }
            final List<String> msList = getManagementServerList(host.getId(), host.getDataCenterId(), dcOrderedHostsMap.get(dcId));
            final Long lbCheckInterval = getLBPreferredHostCheckInterval(host.getClusterId());
            final SetupMSListCommand cmd = new SetupMSListCommand(msList, lbAlgorithm, lbCheckInterval);
            final Answer answer = agentManager.easySend(host.getId(), cmd);
            if (answer == null || !answer.getResult()) {
                LOG.warn(String.format("Failed to setup management servers list to the agent of %s", host));
            }
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
