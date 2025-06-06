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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.agent.lb.algorithm.IndirectAgentLBRoundRobinAlgorithm;
import org.apache.cloudstack.agent.lb.algorithm.IndirectAgentLBShuffleAlgorithm;
import org.apache.cloudstack.agent.lb.algorithm.IndirectAgentLBStaticAlgorithm;
import org.apache.cloudstack.config.ApiServiceConfiguration;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.MigrateAgentConnectionCommand;
import com.cloud.cluster.ManagementServerHostVO;
import com.cloud.cluster.dao.ManagementServerHostDao;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.resource.ResourceState;
import com.cloud.utils.component.ComponentLifecycleBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.exception.CloudRuntimeException;

public class IndirectAgentLBServiceImpl extends ComponentLifecycleBase implements IndirectAgentLB, Configurable {

    public static final ConfigKey<String> IndirectAgentLBAlgorithm = new ConfigKey<>(String.class,
    "indirect.agent.lb.algorithm", "Advanced", "static",
            "The algorithm to be applied on the provided management server list in the 'host' config that that is sent to indirect agents. Allowed values are: static, roundrobin and shuffle.",
            true, ConfigKey.Scope.Global, null, null, null, null, null, ConfigKey.Kind.Select, "static,roundrobin,shuffle");

    public static final ConfigKey<Long> IndirectAgentLBCheckInterval = new ConfigKey<>("Advanced", Long.class,
            "indirect.agent.lb.check.interval", "0",
            "The interval in seconds after which indirect agent should check and try to connect to its preferred host (the first management server from the propagated list provided in the 'host' config)." +
                    " Set 0 to disable it.",
            true, ConfigKey.Scope.Cluster);

    private static Map<String, org.apache.cloudstack.agent.lb.IndirectAgentLBAlgorithm> algorithmMap = new HashMap<>();

    @Inject
    private DataCenterDao dataCenterDao;
    @Inject
    private ClusterDao clusterDao;
    @Inject
    private HostDao hostDao;
    @Inject
    private DataCenterDao dcDao;
    @Inject
    private ManagementServerHostDao mshostDao;
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
    public List<String> getManagementServerList() {
        final String msServerAddresses = ApiServiceConfiguration.ManagementServerAddresses.value();
        if (StringUtils.isEmpty(msServerAddresses)) {
            throw new CloudRuntimeException(String.format("No management server addresses are defined in '%s' setting",
                    ApiServiceConfiguration.ManagementServerAddresses.key()));
        }

        return new ArrayList<>(Arrays.asList(msServerAddresses.replace(" ", "").split(",")));
    }

    @Override
    public List<String> getManagementServerList(final Long hostId, final Long dcId, final List<Long> orderedHostIdList) {
        return getManagementServerList(hostId, dcId, orderedHostIdList, null);
    }

    @Override
    public List<String> getManagementServerList(final Long hostId, final Long dcId, final List<Long> orderedHostIdList, String lbAlgorithm) {
        final String msServerAddresses = ApiServiceConfiguration.ManagementServerAddresses.value();
        if (StringUtils.isEmpty(msServerAddresses)) {
            throw new CloudRuntimeException(String.format("No management server addresses are defined in '%s' setting",
                    ApiServiceConfiguration.ManagementServerAddresses.key()));
        }

        final List<String> msList = Arrays.asList(msServerAddresses.replace(" ", "").split(","));
        if (msList.size() == 1) {
            return msList;
        }

        final org.apache.cloudstack.agent.lb.IndirectAgentLBAlgorithm algorithm = getAgentMSLBAlgorithm(lbAlgorithm);
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
        if (receivedMSHosts == null || receivedMSHosts.isEmpty()) {
            return false;
        }
        if (!getLBAlgorithmName().equals(lbAlgorithm)) {
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
                && !(host.getHypervisorType() == Hypervisor.HypervisorType.KVM || host.getHypervisorType() == Hypervisor.HypervisorType.LXC)) {

            if (logger.isTraceEnabled()) {
                logger.trace(String.format("hypervisor is not the right type, not adding to the host list, (host: %s, hypervisortype: %s)", host, host.getHypervisorType()));
            }
            return;
        }

        agentBasedHosts.add(host);
    }

    private List<Long> getAllAgentBasedNonRoutingHostsFromDB(final Long zoneId, final Long msId) {
        return hostDao.findHostIdsByZoneClusterResourceStateTypeAndHypervisorType(zoneId, null, msId,
                agentValidResourceStates, agentNonRoutingHostTypes, agentValidHypervisorTypes);
    }

    private List<Long> getAllAgentBasedRoutingHostsFromDB(final Long zoneId, final Long clusterId, final Long msId) {
        return hostDao.findHostIdsByZoneClusterResourceStateTypeAndHypervisorType(zoneId, clusterId, msId,
                agentValidResourceStates, List.of(Host.Type.Routing), agentValidHypervisorTypes);
    }

    private List<Long> getAllAgentBasedHostsFromDB(final Long zoneId, final Long clusterId) {
        return hostDao.findHostIdsByZoneClusterResourceStateTypeAndHypervisorType(zoneId, clusterId, null,
                agentValidResourceStates, agentValidHostTypes, agentValidHypervisorTypes);
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
        ExecutorService setupMSListExecutorService = Executors.newFixedThreadPool(10, new NamedThreadFactory("SetupMSList-Worker"));
        final String lbAlgorithm = getLBAlgorithmName();
        final Long globalLbCheckInterval = getLBPreferredHostCheckInterval(null);
        List<DataCenterVO> zones = dataCenterDao.listAll();
        for (DataCenterVO zone : zones) {
            List<Long> zoneHostIds = new ArrayList<>();
            List<Long> nonRoutingHostIds = getAllAgentBasedNonRoutingHostsFromDB(zone.getId(), null);
            zoneHostIds.addAll(nonRoutingHostIds);
            Map<Long, List<Long>> clusterHostIdsMap = new HashMap<>();
            List<Long> clusterIds = clusterDao.listAllClusterIds(zone.getId());
            for (Long clusterId : clusterIds) {
                List<Long> hostIds = getAllAgentBasedRoutingHostsFromDB(zone.getId(), clusterId, null);
                clusterHostIdsMap.put(clusterId, hostIds);
                zoneHostIds.addAll(hostIds);
            }
            zoneHostIds.sort(Comparator.comparingLong(x -> x));
            final List<String> avoidMsList = mshostDao.listNonUpStateMsIPs();
            for (Long nonRoutingHostId : nonRoutingHostIds) {
                setupMSListExecutorService.submit(new SetupMSListTask(nonRoutingHostId, zone.getId(), zoneHostIds, avoidMsList, lbAlgorithm, globalLbCheckInterval));
            }
            for (Long clusterId : clusterIds) {
                final Long clusterLbCheckInterval = getLBPreferredHostCheckInterval(clusterId);
                List<Long> hostIds = clusterHostIdsMap.get(clusterId);
                for (Long hostId : hostIds) {
                    setupMSListExecutorService.submit(new SetupMSListTask(hostId, zone.getId(), zoneHostIds, avoidMsList, lbAlgorithm, clusterLbCheckInterval));
                }
            }
        }

        setupMSListExecutorService.shutdown();
        try {
            if (!setupMSListExecutorService.awaitTermination(300, TimeUnit.SECONDS)) {
                setupMSListExecutorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            setupMSListExecutorService.shutdownNow();
            logger.debug(String.format("Force shutdown setup ms list service as it did not shutdown in the desired time due to: %s", e.getMessage()));
        }
    }

    private final class SetupMSListTask extends ManagedContextRunnable {
        private Long hostId;
        private Long dcId;
        private List<Long> orderedHostIdList;
        private List<String> avoidMsList;
        private String lbAlgorithm;
        private Long lbCheckInterval;

        public SetupMSListTask(Long hostId, Long dcId, List<Long> orderedHostIdList, List<String> avoidMsList,
                               String lbAlgorithm, Long lbCheckInterval) {
            this.hostId = hostId;
            this.dcId = dcId;
            this.orderedHostIdList = orderedHostIdList;
            this.avoidMsList = avoidMsList;
            this.lbAlgorithm = lbAlgorithm;
            this.lbCheckInterval = lbCheckInterval;
        }

        @Override
        protected void runInContext() {
            final List<String> msList = getManagementServerList(hostId, dcId, orderedHostIdList);
            final SetupMSListCommand cmd = new SetupMSListCommand(msList, avoidMsList, lbAlgorithm, lbCheckInterval);
            cmd.setWait(60);
            final Answer answer = agentManager.easySend(hostId, cmd);
            if (answer == null || !answer.getResult()) {
                logger.warn(String.format("Failed to setup management servers list to the agent of ID: %d", hostId));
            }
        }
    }

    protected boolean migrateNonRoutingHostAgentsInZone(String fromMsUuid, long fromMsId, DataCenter dc,
                                                        long migrationStartTimeInMs, long timeoutDurationInMs, final List<String> avoidMsList, String lbAlgorithm,
                                                        boolean lbAlgorithmChanged, List<Long> orderedHostIdList) {
        List<Long> systemVmAgentsInDc = getAllAgentBasedNonRoutingHostsFromDB(dc.getId(), fromMsId);
        if (CollectionUtils.isEmpty(systemVmAgentsInDc)) {
            return true;
        }
        logger.debug(String.format("Migrating %d non-routing host agents from management server node %d (id: %s) of zone %s",
                systemVmAgentsInDc.size(), fromMsId, fromMsUuid, dc));
        ExecutorService migrateAgentsExecutorService = Executors.newFixedThreadPool(5, new NamedThreadFactory("MigrateNonRoutingHostAgent-Worker"));
        Long lbCheckInterval = getLBPreferredHostCheckInterval(null);
        boolean stopMigration = false;
        for (final Long hostId : systemVmAgentsInDc) {
            long migrationElapsedTimeInMs = System.currentTimeMillis() - migrationStartTimeInMs;
            if (migrationElapsedTimeInMs >= timeoutDurationInMs) {
                logger.debug(String.format("Stop migrating remaining non-routing host agents from management server node %d (id: %s), timed out", fromMsId, fromMsUuid));
                stopMigration = true;
                break;
            }

            migrateAgentsExecutorService.submit(new MigrateAgentConnectionTask(fromMsId, hostId, dc.getId(), orderedHostIdList, avoidMsList, lbCheckInterval, lbAlgorithm, lbAlgorithmChanged));
        }

        if (stopMigration) {
            migrateAgentsExecutorService.shutdownNow();
            return false;
        }

        migrateAgentsExecutorService.shutdown();
        long pendingTimeoutDurationInMs = timeoutDurationInMs - (System.currentTimeMillis() - migrationStartTimeInMs);
        try {
            if (pendingTimeoutDurationInMs <= 0 || !migrateAgentsExecutorService.awaitTermination(pendingTimeoutDurationInMs, TimeUnit.MILLISECONDS)) {
                migrateAgentsExecutorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            migrateAgentsExecutorService.shutdownNow();
            logger.debug(String.format("Force shutdown migrate non-routing agents service as it did not shutdown in the desired time due to: %s", e.getMessage()));
        }

        return true;
    }

    protected boolean migrateRoutingHostAgentsInCluster(long clusterId, String fromMsUuid, long fromMsId, DataCenter dc,
                                                        long migrationStartTimeInMs, long timeoutDurationInMs, final List<String> avoidMsList, String lbAlgorithm,
                                                        boolean lbAlgorithmChanged, List<Long> orderedHostIdList) {

        List<Long> agentBasedHostsOfMsInDcAndCluster = getAllAgentBasedRoutingHostsFromDB(dc.getId(), clusterId, fromMsId);
        if (CollectionUtils.isEmpty(agentBasedHostsOfMsInDcAndCluster)) {
            return true;
        }
        logger.debug(String.format("Migrating %d indirect routing host agents from management server node %d (id: %s) of zone %s, " +
                "cluster ID: %d", agentBasedHostsOfMsInDcAndCluster.size(), fromMsId, fromMsUuid, dc, clusterId));
        ExecutorService migrateAgentsExecutorService = Executors.newFixedThreadPool(10, new NamedThreadFactory("MigrateRoutingHostAgent-Worker"));
        Long lbCheckInterval = getLBPreferredHostCheckInterval(clusterId);
        boolean stopMigration = false;
        for (final Long hostId : agentBasedHostsOfMsInDcAndCluster) {
            long migrationElapsedTimeInMs = System.currentTimeMillis() - migrationStartTimeInMs;
            if (migrationElapsedTimeInMs >= timeoutDurationInMs) {
                logger.debug(String.format("Stop migrating remaining indirect routing host agents from management server node %d (id: %s), timed out", fromMsId, fromMsUuid));
                stopMigration = true;
                break;
            }

            migrateAgentsExecutorService.submit(new MigrateAgentConnectionTask(fromMsId, hostId, dc.getId(), orderedHostIdList, avoidMsList, lbCheckInterval, lbAlgorithm, lbAlgorithmChanged));
        }

        if (stopMigration) {
            migrateAgentsExecutorService.shutdownNow();
            return false;
        }

        migrateAgentsExecutorService.shutdown();
        long pendingTimeoutDurationInMs = timeoutDurationInMs - (System.currentTimeMillis() - migrationStartTimeInMs);
        try {
            if (pendingTimeoutDurationInMs <= 0 || !migrateAgentsExecutorService.awaitTermination(pendingTimeoutDurationInMs, TimeUnit.MILLISECONDS)) {
                migrateAgentsExecutorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            migrateAgentsExecutorService.shutdownNow();
            logger.debug(String.format("Force shutdown migrate routing agents service as it did not shutdown in the desired time due to: %s", e.getMessage()));
        }

        return true;
    }

    @Override
    public boolean migrateAgents(String fromMsUuid, long fromMsId, String lbAlgorithm, long timeoutDurationInMs) {
        if (timeoutDurationInMs <= 0) {
            logger.debug(String.format("Not migrating indirect agents from management server node %d (id: %s) to other nodes, invalid timeout duration", fromMsId, fromMsUuid));
            return false;
        }

        logger.debug(String.format("Migrating indirect agents from management server node %d (id: %s) to other nodes", fromMsId, fromMsUuid));
        long migrationStartTimeInMs = System.currentTimeMillis();
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
            if (!migrateAgentsInZone(dc, fromMsUuid, fromMsId, avoidMsList, lbAlgorithm, lbAlgorithmChanged,
                    migrationStartTimeInMs, timeoutDurationInMs)) {
                return false;
            }
        }

        return true;
    }

    private boolean migrateAgentsInZone(DataCenterVO dc, String fromMsUuid, long fromMsId, List<String> avoidMsList,
                                            String lbAlgorithm, boolean lbAlgorithmChanged, long migrationStartTimeInMs, long timeoutDurationInMs) {
        List<Long> orderedHostIdList = getOrderedHostIdList(dc.getId());
        if (!migrateNonRoutingHostAgentsInZone(fromMsUuid, fromMsId, dc, migrationStartTimeInMs,
                timeoutDurationInMs, avoidMsList, lbAlgorithm, lbAlgorithmChanged, orderedHostIdList)) {
            return false;
        }
        List<Long> clusterIds = clusterDao.listAllClusterIds(dc.getId());
        for (Long clusterId : clusterIds) {
            if (!migrateRoutingHostAgentsInCluster(clusterId, fromMsUuid, fromMsId, dc, migrationStartTimeInMs,
                    timeoutDurationInMs, avoidMsList, lbAlgorithm, lbAlgorithmChanged, orderedHostIdList)) {
                return false;
            }
        }
        return true;
    }

    private final class MigrateAgentConnectionTask extends ManagedContextRunnable {
        private long fromMsId;
        Long hostId;
        Long dcId;
        List<Long> orderedHostIdList;
        List<String> avoidMsList;
        Long lbCheckInterval;
        String lbAlgorithm;
        boolean lbAlgorithmChanged;

        public MigrateAgentConnectionTask(long fromMsId, Long hostId, Long dcId, List<Long> orderedHostIdList,
                                          List<String> avoidMsList, Long lbCheckInterval, String lbAlgorithm, boolean lbAlgorithmChanged) {
            this.fromMsId = fromMsId;
            this.hostId = hostId;
            this.orderedHostIdList = orderedHostIdList;
            this.avoidMsList = avoidMsList;
            this.lbCheckInterval = lbCheckInterval;
            this.lbAlgorithm = lbAlgorithm;
            this.lbAlgorithmChanged = lbAlgorithmChanged;
        }

        @Override
        protected void runInContext() {
            try {
                List<String> msList = null;
                if (lbAlgorithmChanged) {
                    // send new MS list when there is change in lb algorithm
                    msList = getManagementServerList(hostId, dcId, orderedHostIdList, lbAlgorithm);
                }

                final MigrateAgentConnectionCommand cmd = new MigrateAgentConnectionCommand(msList, avoidMsList, lbAlgorithm, lbCheckInterval);
                cmd.setWait(60);
                final Answer answer = agentManager.easySend(hostId, cmd); //may not receive answer when the agent disconnects immediately and try reconnecting to other ms host
                if (answer != null && !answer.getResult()) {
                    logger.warn(String.format("Error while initiating migration of agent connection for host agent ID: %d - %s", hostId, answer.getDetails()));
                }
                updateLastManagementServer(hostId, fromMsId);
            } catch (final Exception e) {
                logger.error(String.format("Error migrating agent connection for host %d", hostId), e);
            }
        }
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
