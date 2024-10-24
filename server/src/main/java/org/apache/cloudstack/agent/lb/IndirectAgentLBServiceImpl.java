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

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.MigrateAgentConnectionCommand;
import com.cloud.cluster.ManagementServerHostVO;
import com.cloud.cluster.dao.ManagementServerHostDao;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.resource.ResourceState;
import com.cloud.utils.component.ComponentLifecycleBase;
import com.cloud.utils.exception.CloudRuntimeException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

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
    private HostDao hostDao;
    @Inject
    private DataCenterDao dcDao;
    @Inject
    private ManagementServerHostDao mshostDao;
    @Inject
    private AgentManager agentManager;

    //////////////////////////////////////////////////////
    /////////////// Agent MSLB Methods ///////////////////
    //////////////////////////////////////////////////////

    @Override
    public List<String> getManagementServerList() {
        final String msServerAddresses = ApiServiceConfiguration.ManagementServerAddresses.value();
        if (StringUtils.isEmpty(msServerAddresses)) {
            throw new CloudRuntimeException(String.format("No management server addresses are defined in '%s' setting",
                    ApiServiceConfiguration.ManagementServerAddresses.key()));
        }

        List<String> msList = new ArrayList<>(Arrays.asList(msServerAddresses.replace(" ", "").split(",")));
        return msList;
    }

    @Override
    public List<String> getManagementServerList(final Long hostId, final Long dcId, final List<Long> orderedHostIdList) {
        return  getManagementServerList(hostId, dcId, orderedHostIdList, null);
    }

    @Override
    public List<String> getManagementServerList(final Long hostId, final Long dcId, final List<Long> orderedHostIdList, String lbAlgorithm) {
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
            if (logger.isTraceEnabled()) {
                logger.trace("adding requested host to host list as it does not seem to be there; " + hostId);
            }
            hostIdList.add(hostId);
        }

        final org.apache.cloudstack.agent.lb.IndirectAgentLBAlgorithm algorithm = getAgentMSLBAlgorithm(lbAlgorithm);
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

    private List<Host> getAllAgentBasedHosts(long msId) {
        final List<HostVO> allHosts = hostDao.listHostsByMs(msId);
        if (allHosts == null) {
            return new ArrayList<>();
        }
        final List <Host> agentBasedHosts = new ArrayList<>();
        for (final Host host : allHosts) {
            conditionallyAddHost(agentBasedHosts, host);
        }
        return agentBasedHosts;
    }

    private List<Host> getAllAgentBasedHostsInDc(long msId, long dcId) {
        final List<HostVO> allHosts = hostDao.listHostsByMsAndDc(msId, dcId);
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
            if (logger.isTraceEnabled()) {
                logger.trace("trying to add no host to a list");
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
            if (logger.isTraceEnabled()) {
                logger.trace("host ({}) is in '{}' state, not adding to the host list", host, host.getResourceState());
            }
            return;
        }

        if (host.getType() != Host.Type.Routing
                && host.getType() != Host.Type.ConsoleProxy
                && host.getType() != Host.Type.SecondaryStorage
                && host.getType() != Host.Type.SecondaryStorageVM) {
            if (logger.isTraceEnabled()) {
                logger.trace(String.format("host (%s) is of wrong type, not adding to the host list, type = %s", host, host.getType()));
            }
            return;
        }

        if (host.getHypervisorType() != null
                && ! (host.getHypervisorType() == Hypervisor.HypervisorType.KVM || host.getHypervisorType() == Hypervisor.HypervisorType.LXC)) {

            if (logger.isTraceEnabled()) {
                logger.trace(String.format("hypervisor is not the right type, not adding to the host list, (host: %s, hypervisortype: %s)", host, host.getHypervisorType()));
            }
            return;
        }

        agentBasedHosts.add(host);
    }

    @Override
    public boolean haveAgentBasedHosts(long msId) {
        return CollectionUtils.isNotEmpty(getAllAgentBasedHosts(msId));
    }

    private org.apache.cloudstack.agent.lb.IndirectAgentLBAlgorithm getAgentMSLBAlgorithm() {
        return getAgentMSLBAlgorithm(null);
    }

    private org.apache.cloudstack.agent.lb.IndirectAgentLBAlgorithm getAgentMSLBAlgorithm(String lbAlgorithm) {
        boolean algorithmNameFromConfig = false;
        if (StringUtils.isEmpty(lbAlgorithm)) {
            lbAlgorithm = getLBAlgorithmName();
            algorithmNameFromConfig = true;
        }
        if (algorithmMap.containsKey(lbAlgorithm)) {
            return algorithmMap.get(lbAlgorithm);
        }
        throw new CloudRuntimeException(String.format("Algorithm %s%s not found, valid values are: %s",
                lbAlgorithm, algorithmNameFromConfig? " configured for '" + IndirectAgentLBAlgorithm.key() + "'" : "", algorithmMap.keySet()));
    }

    @Override
    public void checkLBAlgorithmName(String lbAlgorithm) {
        if (!algorithmMap.containsKey(lbAlgorithm)) {
            throw new CloudRuntimeException(String.format("Invalid algorithm %s, valid values are: %s", lbAlgorithm, algorithmMap.keySet()));
        }
    }

    ////////////////////////////////////////////////////////////
    /////////////// Agent MSLB Configuration ///////////////////
    ////////////////////////////////////////////////////////////

    @Override
    public void propagateMSListToAgents() {
        logger.debug("Propagating management server list update to agents");
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
                logger.warn(String.format("Failed to setup management servers list to the agent of %s", host));
            }
        }
    }

    @Override
    public boolean migrateAgents(String fromMsUuid, long fromMsId, String lbAlgorithm, long timeoutDurationInMs) {
        if (timeoutDurationInMs <= 0) {
            logger.debug(String.format("Not migrating indirect agents from management server node %d (id: %s) to other nodes, invalid timeout duration", fromMsId, fromMsUuid));
            return false;
        }

        logger.debug(String.format("Migrating indirect agents from management server node %d (id: %s) to other nodes", fromMsId, fromMsUuid));
        long migrationStartTime = System.currentTimeMillis();
        if (!haveAgentBasedHosts(fromMsId)) {
            logger.info(String.format("No indirect agents available on management server node %d (id: %s), to migrate", fromMsId, fromMsUuid));
            return true;
        }

        boolean lbAlgorithmChanged = false;
        if (StringUtils.isNotBlank(lbAlgorithm) && !lbAlgorithm.equalsIgnoreCase(getLBAlgorithmName())) {
            logger.debug(String.format("Indirect agent lb algorithm changed to %s", lbAlgorithm));
            lbAlgorithmChanged = true;
        }

        final List<String> avoidMsList = mshostDao.listNonUpStateMsIPs();
        ManagementServerHostVO ms = mshostDao.findByMsid(fromMsId);
        if (ms != null && !avoidMsList.contains(ms.getServiceIP())) {
            avoidMsList.add(ms.getServiceIP());
        }

        List<DataCenterVO> dataCenterList = dcDao.listAll();
        for (DataCenterVO dc : dataCenterList) {
            Long dcId = dc.getId();
            List<Long> orderedHostIdList = getOrderedHostIdList(dcId);
            List<Host> agentBasedHostsOfMsInDc = getAllAgentBasedHostsInDc(fromMsId, dcId);
            if (CollectionUtils.isEmpty(agentBasedHostsOfMsInDc)) {
                continue;
            }
            logger.debug(String.format("Migrating %d indirect agents from management server node %d (id: %s) of zone %s", agentBasedHostsOfMsInDc.size(), fromMsId, fromMsUuid, dc.toString()));
            for (final Host host : agentBasedHostsOfMsInDc) {
                long migrationElapsedTimeInMs = System.currentTimeMillis() - migrationStartTime;
                if (migrationElapsedTimeInMs >= timeoutDurationInMs) {
                    logger.debug(String.format("Stop migrating remaining indirect agents from management server node %d (id: %s), timed out", fromMsId, fromMsUuid));
                    return false;
                }

                List<String> msList = null;
                Long lbCheckInterval = 0L;
                if (lbAlgorithmChanged) {
                    // send new MS list when there is change in lb algorithm
                    msList = getManagementServerList(host.getId(), dcId, orderedHostIdList);
                    lbCheckInterval = getLBPreferredHostCheckInterval(host.getClusterId());
                }

                final MigrateAgentConnectionCommand cmd = new MigrateAgentConnectionCommand(msList, avoidMsList, lbAlgorithm, lbCheckInterval);
                agentManager.easySend(host.getId(), cmd); //answer not received as the agent disconnects and reconnects to other ms
                updateLastManagementServer(host.getId(), fromMsId);
            }
        }

        return true;
    }

    private void updateLastManagementServer(long hostId, long msId) {
        HostVO hostVO = hostDao.findById(hostId);
        if (hostVO != null) {
            hostVO.setLastManagementServerId(msId);
            hostDao.update(hostId, hostVO);
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
