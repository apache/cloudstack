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

package org.apache.cloudstack.ha;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.admin.ha.ConfigureHAForHostCmd;
import org.apache.cloudstack.api.command.admin.ha.DisableHAForClusterCmd;
import org.apache.cloudstack.api.command.admin.ha.DisableHAForHostCmd;
import org.apache.cloudstack.api.command.admin.ha.DisableHAForZoneCmd;
import org.apache.cloudstack.api.command.admin.ha.EnableHAForClusterCmd;
import org.apache.cloudstack.api.command.admin.ha.EnableHAForHostCmd;
import org.apache.cloudstack.api.command.admin.ha.EnableHAForZoneCmd;
import org.apache.cloudstack.api.command.admin.ha.ListHostHAProvidersCmd;
import org.apache.cloudstack.api.command.admin.ha.ListHostHAResourcesCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.ha.dao.HAConfigDao;
import org.apache.cloudstack.ha.provider.HAProvider;
import org.apache.cloudstack.ha.provider.HAProvider.HAProviderConfig;
import org.apache.cloudstack.ha.task.ActivityCheckTask;
import org.apache.cloudstack.ha.task.FenceTask;
import org.apache.cloudstack.ha.task.HealthCheckTask;
import org.apache.cloudstack.ha.task.RecoveryTask;
import org.apache.cloudstack.kernel.Partition;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.poll.BackgroundPollManager;
import org.apache.cloudstack.poll.BackgroundPollTask;
import org.apache.cloudstack.utils.identity.ManagementServerNode;
import org.apache.log4j.Logger;

import com.cloud.cluster.ClusterManagerListener;
import org.apache.cloudstack.management.ManagementServerHost;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterDetailsVO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterDetailVO;
import com.cloud.dc.dao.DataCenterDetailsDao;
import com.cloud.domain.Domain;
import com.cloud.event.ActionEvent;
import com.cloud.event.ActionEventUtils;
import com.cloud.event.EventTypes;
import com.cloud.ha.Investigator;
import com.cloud.host.Host;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.org.Cluster;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateListener;
import com.cloud.utils.fsm.StateMachine2;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public final class HAManagerImpl extends ManagerBase implements HAManager, ClusterManagerListener, PluggableService, Configurable, StateListener<HAConfig.HAState, HAConfig.Event, HAConfig> {
    public static final Logger LOG = Logger.getLogger(HAManagerImpl.class);

    @Inject
    private HAConfigDao haConfigDao;

    @Inject
    private HostDao hostDao;

    @Inject
    private ClusterDetailsDao clusterDetailsDao;

    @Inject
    private DataCenterDetailsDao dataCenterDetailsDao;

    @Inject
    private BackgroundPollManager pollManager;

    private List<HAProvider<HAResource>> haProviders;
    private Map<String, HAProvider<HAResource>> haProviderMap = new HashMap<>();

    private static ExecutorService healthCheckExecutor;
    private static ExecutorService activityCheckExecutor;
    private static ExecutorService recoveryExecutor;
    private static ExecutorService fenceExecutor;

    private static final String HA_ENABLED_DETAIL = "resourceHAEnabled";

    //////////////////////////////////////////////////////
    //////////////// HA Manager methods //////////////////
    //////////////////////////////////////////////////////

    public Map<String, HAResourceCounter> haCounterMap = new ConcurrentHashMap<>();

    public HAProvider<HAResource> getHAProvider(final String name) {
        return haProviderMap.get(name);
    }

    private String resourceCounterKey(final Long resourceId, final HAResource.ResourceType resourceType) {
        return resourceId.toString() + resourceType.toString();
    }

    public synchronized HAResourceCounter getHACounter(final Long resourceId, final HAResource.ResourceType resourceType) {
        final String key = resourceCounterKey(resourceId, resourceType);
        if (!haCounterMap.containsKey(key)) {
            haCounterMap.put(key, new HAResourceCounter());
        }
        return haCounterMap.get(key);
    }

    public synchronized void purgeHACounter(final Long resourceId, final HAResource.ResourceType resourceType) {
        final String key = resourceCounterKey(resourceId, resourceType);
        if (haCounterMap.containsKey(key)) {
            haCounterMap.remove(key);
        }
    }

    public boolean transitionHAState(final HAConfig.Event event, final HAConfig haConfig) {
        if (event == null || haConfig == null) {
            return false;
        }
        final HAConfig.HAState currentHAState = haConfig.getState();
        try {
            final HAConfig.HAState nextState = HAConfig.HAState.getStateMachine().getNextState(currentHAState, event);
            boolean result = HAConfig.HAState.getStateMachine().transitTo(haConfig, event, null, haConfigDao);
            if (result) {
                final String message = String.format("Transitioned host HA state from:%s to:%s due to event:%s for the host id:%d",
                        currentHAState, nextState, event, haConfig.getResourceId());
                LOG.debug(message);

                if (nextState == HAConfig.HAState.Recovering || nextState == HAConfig.HAState.Fencing || nextState == HAConfig.HAState.Fenced) {
                    ActionEventUtils.onActionEvent(CallContext.current().getCallingUserId(), CallContext.current().getCallingAccountId(),
                            Domain.ROOT_DOMAIN, EventTypes.EVENT_HA_STATE_TRANSITION, message);
                }
            }
            return result;
        } catch (NoTransitionException e) {
            LOG.warn(String.format("Unable to find next HA state for current HA state=[%s] for event=[%s] for host=[%s].", currentHAState, event, haConfig.getResourceId()), e);
        }
        return false;
    }

    private boolean transitionResourceStateToDisabled(final Partition partition) {
        List<? extends HAResource> resources;
        if (partition.partitionType() == Partition.PartitionType.Cluster) {
            resources = hostDao.findByClusterId(partition.getId());
        } else if (partition.partitionType() == Partition.PartitionType.Zone) {
            resources = hostDao.findByDataCenterId(partition.getId());
        } else {
            return true;
        }

        boolean result = true;
        for (final HAResource resource: resources) {
            result = result && transitionHAState(HAConfig.Event.Disabled,
                    haConfigDao.findHAResource(resource.getId(), resource.resourceType()));
        }
        return result;
    }

    private boolean checkHAOwnership(final HAConfig haConfig) {
        // Skip for resources not owned by this mgmt server
        return !(haConfig.getManagementServerId() != null
                && haConfig.getManagementServerId() != ManagementServerNode.getManagementServerId());
    }

    private HAResource validateAndFindHAResource(final HAConfig haConfig) {
        HAResource resource = null;
        if (haConfig == null) {
            return null;
        }
        if (haConfig.getResourceType() == HAResource.ResourceType.Host) {
            final Host host = hostDao.findById(haConfig.getResourceId());
            if (host != null && host.getRemoved() != null) {
                return null;
            }
            resource = host;
            if (haConfig.getState() == null || (resource == null && haConfig.getState() != HAConfig.HAState.Disabled)) {
                disableHA(haConfig.getResourceId(), haConfig.getResourceType());
                return null;
            }
        }
        if (!haConfig.isEnabled() || !isHAEnabledForZone(resource) || !isHAEnabledForCluster(resource)) {
            if (haConfig.getState() != HAConfig.HAState.Disabled) {
                if (transitionHAState(HAConfig.Event.Disabled, haConfig) ) {
                    purgeHACounter(haConfig.getResourceId(), haConfig.getResourceType());
                }
            }
            return null;
        } else if (haConfig.getState() == HAConfig.HAState.Disabled) {
            transitionHAState(HAConfig.Event.Enabled, haConfig);
        }
        return resource;
    }

    private HAProvider<HAResource> validateAndFindHAProvider(final HAConfig haConfig, final HAResource resource) {
        if (haConfig == null) {
            return null;
        }
        final HAProvider<HAResource> haProvider = haProviderMap.get(haConfig.getHaProvider());
        if (haProvider != null && !haProvider.isEligible(resource)) {
            if (haConfig.getState() != HAConfig.HAState.Ineligible) {
                transitionHAState(HAConfig.Event.Ineligible, haConfig);
            }
            return null;
        } else if (haConfig.getState() == HAConfig.HAState.Ineligible) {
            transitionHAState(HAConfig.Event.Eligible, haConfig);
        }
        return haProvider;
    }

    public boolean isHAEnabledForZone(final HAResource resource) {
        if (resource == null || resource.getDataCenterId() < 1L) {
            return true;
        }
        final DataCenterDetailVO zoneDetails = dataCenterDetailsDao.findDetail(resource.getDataCenterId(), HA_ENABLED_DETAIL);
        return zoneDetails == null || Strings.isNullOrEmpty(zoneDetails.getValue()) || Boolean.valueOf(zoneDetails.getValue());
    }

    private boolean isHAEnabledForCluster(final HAResource resource) {
        if (resource == null || resource.getClusterId() == null) {
            return true;
        }
        final ClusterDetailsVO clusterDetails = clusterDetailsDao.findDetail(resource.getClusterId(), HA_ENABLED_DETAIL);
        return clusterDetails == null || Strings.isNullOrEmpty(clusterDetails.getValue()) || Boolean.valueOf(clusterDetails.getValue());
    }

    private boolean isHAEligibleForResource(final HAResource resource) {
        if (resource == null || resource.getId() < 1L) {
            return false;
        }
        HAResource.ResourceType resourceType = null;
        if (resource instanceof Host) {
            resourceType = HAResource.ResourceType.Host;
        }
        if (resourceType == null) {
            return false;
        }
        final HAConfig haConfig = haConfigDao.findHAResource(resource.getId(), resourceType);
        return haConfig != null && haConfig.isEnabled()
                && haConfig.getState() != HAConfig.HAState.Disabled
                && haConfig.getState() != HAConfig.HAState.Ineligible;
    }

    public boolean isHAEligible(final HAResource resource) {
        return resource != null && isHAEnabledForZone(resource)
                && isHAEnabledForCluster(resource)
                && isHAEligibleForResource(resource);
    }

    public void validateHAProviderConfigForResource(final Long resourceId, final HAResource.ResourceType resourceType, final HAProvider<HAResource> haProvider) {
        if (HAResource.ResourceType.Host.equals(resourceType)) {
            final Host host = hostDao.findById(resourceId);

            if (host == null) {
                throw new ServerApiException(ApiErrorCode.PARAM_ERROR, String.format("Resource [%s] not found.", resourceId));
            }

            if (host.getHypervisorType() == null) {
                throw new ServerApiException(ApiErrorCode.PARAM_ERROR, String.format("No hypervisor type provided on resource [%s].", resourceId));
            }

            if (haProvider.resourceSubType() == null) {
                throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "No hypervisor type provided on haprovider.");
            }

            if (!host.getHypervisorType().toString().equals(haProvider.resourceSubType().toString())) {
                throw new ServerApiException(ApiErrorCode.PARAM_ERROR, String.format("Incompatible haprovider provided [%s] for the resource [%s] of hypervisor type: [%s].", haProvider.resourceSubType().toString(), host.getId(),host.getHypervisorType()));
            }
        }
    }

    ////////////////////////////////////////////////////////////////////
    //////////////// HA Investigator wrapper for Old HA ////////////////
    ////////////////////////////////////////////////////////////////////

    public Boolean isVMAliveOnHost(final Host host) throws Investigator.UnknownVM {
        final HAConfig haConfig = haConfigDao.findHAResource(host.getId(), HAResource.ResourceType.Host);
        if (haConfig != null) {
            if (haConfig.getState() == HAConfig.HAState.Fenced) {
                LOG.debug(String.format("HA: Host [%s] is fenced.", host.getId()));
                return false;
            }
            LOG.debug(String.format("HA: Host [%s] is alive.", host.getId()));
            return true;
        }
        throw new Investigator.UnknownVM();
    }

    public Status getHostStatus(final Host host) {
        final HAConfig haConfig = haConfigDao.findHAResource(host.getId(), HAResource.ResourceType.Host);
        if (haConfig != null) {
            if (haConfig.getState() == HAConfig.HAState.Fenced) {
                LOG.debug(String.format("HA: Agent [%s] is available/suspect/checking Up.", host.getId()));
                return Status.Down;
            } else if (haConfig.getState() == HAConfig.HAState.Degraded || haConfig.getState() == HAConfig.HAState.Recovering || haConfig.getState() == HAConfig.HAState.Fencing) {
                LOG.debug(String.format("HA: Agent [%s] is disconnected. State: %s, %s.", host.getId(), haConfig.getState(), haConfig.getState().getDescription()));
                return Status.Disconnected;
            }
            return Status.Up;
        }
        return Status.Unknown;
    }

    //////////////////////////////////////////////////////
    //////////////// HA API handlers /////////////////////
    //////////////////////////////////////////////////////

    private boolean configureHA(final Long resourceId, final HAResource.ResourceType resourceType, final Boolean enable, final String haProvider) {
        return Transaction.execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction(TransactionStatus status) {
                HAConfigVO haConfig = (HAConfigVO) haConfigDao.findHAResource(resourceId, resourceType);
                if (haConfig == null) {
                    haConfig = new HAConfigVO();
                    if (haProvider != null) {
                        haConfig.setHaProvider(haProvider);
                    }
                    if (enable != null) {
                        haConfig.setEnabled(enable);
                        haConfig.setManagementServerId(ManagementServerNode.getManagementServerId());
                    }
                    haConfig.setResourceId(resourceId);
                    haConfig.setResourceType(resourceType);
                    if (Strings.isNullOrEmpty(haConfig.getHaProvider())) {
                        throw new ServerApiException(ApiErrorCode.PARAM_ERROR, String.format("HAProvider is not provided for the resource [%s], failing configuration.", resourceId));
                    }
                    if (haConfigDao.persist(haConfig) != null) {
                        return true;
                    }
                } else {
                    if (enable != null) {
                        haConfig.setEnabled(enable);
                    }
                    if (haProvider != null) {
                        haConfig.setHaProvider(haProvider);
                    }
                    if (Strings.isNullOrEmpty(haConfig.getHaProvider())) {
                        throw new ServerApiException(ApiErrorCode.PARAM_ERROR, String.format("HAProvider is not provided for the resource [%s], failing configuration.", resourceId));
                    }
                    return haConfigDao.update(haConfig.getId(), haConfig);
                }
                return false;
            }
        });
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_HA_RESOURCE_CONFIGURE, eventDescription = "configuring HA for resource")
    public boolean configureHA(final Long resourceId, final HAResource.ResourceType resourceType, final String haProvider) {
        Preconditions.checkArgument(resourceId != null && resourceId > 0L);
        Preconditions.checkArgument(resourceType != null);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(haProvider));

        if (!haProviderMap.containsKey(haProvider.toLowerCase())) {
            throw new CloudRuntimeException(String.format("Given HA provider [%s] does not exist.", haProvider));
        }
        validateHAProviderConfigForResource(resourceId, resourceType, haProviderMap.get(haProvider.toLowerCase()));
        return configureHA(resourceId, resourceType, null, haProvider.toLowerCase());
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_HA_RESOURCE_ENABLE, eventDescription = "enabling HA for resource")
    public boolean enableHA(final Long resourceId, final HAResource.ResourceType resourceType) {
        Preconditions.checkArgument(resourceId != null && resourceId > 0L);
        Preconditions.checkArgument(resourceType != null);
        return configureHA(resourceId, resourceType, true, null);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_HA_RESOURCE_DISABLE, eventDescription = "disabling HA for resource")
    public boolean disableHA(final Long resourceId, final HAResource.ResourceType resourceType) {
        Preconditions.checkArgument(resourceId != null && resourceId > 0L);
        Preconditions.checkArgument(resourceType != null);
        boolean result = configureHA(resourceId, resourceType, false, null);
        if (result) {
            transitionHAState(HAConfig.Event.Disabled, haConfigDao.findHAResource(resourceId, resourceType));
            purgeHACounter(resourceId, resourceType);
        }
        return result;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_HA_RESOURCE_ENABLE, eventDescription = "enabling HA for a cluster")
    public boolean enableHA(final Cluster cluster) {
        clusterDetailsDao.persist(cluster.getId(), HA_ENABLED_DETAIL, String.valueOf(true));
        return true;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_HA_RESOURCE_DISABLE, eventDescription = "disabling HA for a cluster")
    public boolean disableHA(final Cluster cluster) {
        clusterDetailsDao.persist(cluster.getId(), HA_ENABLED_DETAIL, String.valueOf(false));
        return transitionResourceStateToDisabled(cluster);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_HA_RESOURCE_ENABLE, eventDescription = "enabling HA for a zone")
    public boolean enableHA(final DataCenter zone) {
        dataCenterDetailsDao.persist(zone.getId(), HA_ENABLED_DETAIL, String.valueOf(true));
        return true;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_HA_RESOURCE_DISABLE, eventDescription = "disabling HA for a zone")
    public boolean disableHA(final DataCenter zone) {
        dataCenterDetailsDao.persist(zone.getId(), HA_ENABLED_DETAIL, String.valueOf(false));
        return transitionResourceStateToDisabled(zone);
    }

    @Override
    public List<HAConfig> listHAResources(final Long resourceId, final HAResource.ResourceType resourceType) {
        return haConfigDao.listHAResource(resourceId, resourceType);
    }

    @Override
    public List<String> listHAProviders(final HAResource.ResourceType resourceType, final HAResource.ResourceSubType entityType) {
        final List<String> haProviderNames = new ArrayList<>();
        for (final HAProvider<HAResource> haProvider : haProviders) {
            if (haProvider.resourceType().equals(resourceType) && haProvider.resourceSubType().equals(entityType)) {
                haProviderNames.add(haProvider.getClass().getSimpleName());
            }
        }
        return haProviderNames;
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<>();
        cmdList.add(ConfigureHAForHostCmd.class);
        cmdList.add(EnableHAForHostCmd.class);
        cmdList.add(EnableHAForClusterCmd.class);
        cmdList.add(EnableHAForZoneCmd.class);
        cmdList.add(DisableHAForHostCmd.class);
        cmdList.add(DisableHAForClusterCmd.class);
        cmdList.add(DisableHAForZoneCmd.class);
        cmdList.add(ListHostHAResourcesCmd.class);
        cmdList.add(ListHostHAProvidersCmd.class);
        return cmdList;
    }

    //////////////////////////////////////////////////////
    //////////////// Event Listeners /////////////////////
    //////////////////////////////////////////////////////

    @Override
    public void onManagementNodeJoined(List<? extends ManagementServerHost> nodeList, long selfNodeId) {
    }

    @Override
    public void onManagementNodeLeft(List<? extends ManagementServerHost> nodeList, long selfNodeId) {
    }

    @Override
    public void onManagementNodeIsolated() {
    }

    private boolean processHAStateChange(final HAConfig haConfig, final HAConfig.HAState newState, final boolean status) {
        if (!status || !checkHAOwnership(haConfig)) {
            return false;
        }

        final HAResource resource = validateAndFindHAResource(haConfig);
        if (resource == null) {
            return false;
        }

        final HAProvider<HAResource> haProvider = validateAndFindHAProvider(haConfig, resource);
        if (haProvider == null) {
            return false;
        }

        final HAResourceCounter counter = getHACounter(haConfig.getResourceId(), haConfig.getResourceType());

        // Perform activity checks
        if (newState == HAConfig.HAState.Checking) {
            final ActivityCheckTask job = ComponentContext.inject(new ActivityCheckTask(resource, haProvider, haConfig,
                    HAProviderConfig.ActivityCheckTimeout, activityCheckExecutor, counter.getSuspectTimeStamp()));
            activityCheckExecutor.submit(job);
        }

        // Attempt recovery
        if (newState == HAConfig.HAState.Recovering) {
            if (counter.getRecoveryCounter() >= (Long) (haProvider.getConfigValue(HAProviderConfig.MaxRecoveryAttempts, resource))) {
                return false;
            }
            final RecoveryTask task = ComponentContext.inject(new RecoveryTask(resource, haProvider, haConfig,
                    HAProviderConfig.RecoveryTimeout, recoveryExecutor));
            final Future<Boolean> recoveryFuture = recoveryExecutor.submit(task);
            counter.setRecoveryFuture(recoveryFuture);
        }

        // Fencing
        if (newState == HAConfig.HAState.Fencing) {
            final FenceTask task = ComponentContext.inject(new FenceTask(resource, haProvider, haConfig,
                    HAProviderConfig.FenceTimeout, fenceExecutor));
            final Future<Boolean> fenceFuture = fenceExecutor.submit(task);
            counter.setFenceFuture(fenceFuture);
        }
        return true;
    }

    @Override
    public boolean preStateTransitionEvent(final HAConfig.HAState oldState, final HAConfig.Event event, final HAConfig.HAState newState, final HAConfig haConfig, final boolean status, final Object opaque) {
        if (oldState != newState || newState == HAConfig.HAState.Suspect || newState == HAConfig.HAState.Checking) {
            return false;
        }

        LOG.debug(String.format("HA state pre-transition:: new state=[%s], old state=[%s], for resource id=[%s], status=[%s], ha config state=[%s]." , newState, oldState, haConfig.getResourceId(), status, haConfig.getState()));

        if (status && haConfig.getState() != newState) {
            LOG.warn(String.format("HA state pre-transition:: HA state is not equal to transition state, HA state=[%s], new state=[%s].", haConfig.getState(), newState));
        }
        return processHAStateChange(haConfig, newState, status);
    }

    @Override
    public boolean postStateTransitionEvent(final StateMachine2.Transition<HAConfig.HAState, HAConfig.Event> transition, final HAConfig haConfig, final boolean status, final Object opaque) {
        LOG.debug(String.format("HA state post-transition:: new state=[%s], old state=[%s], for resource id=[%s], status=[%s], ha config state=[%s].", transition.getToState(), transition.getCurrentState(),  haConfig.getResourceId(), status, haConfig.getState()));

        if (status && haConfig.getState() != transition.getToState()) {
            LOG.warn(String.format("HA state post-transition:: HA state is not equal to transition state, HA state=[%s], new state=[%s].", haConfig.getState(), transition.getToState()));
        }
        return processHAStateChange(haConfig, transition.getToState(), status);
    }

    ///////////////////////////////////////////////////
    //////////////// Manager Init /////////////////////
    ///////////////////////////////////////////////////

    @Override
    public boolean start() {
        haProviderMap.clear();
        for (final HAProvider<HAResource> haProvider : haProviders) {
            haProviderMap.put(haProvider.getClass().getSimpleName().toLowerCase(), haProvider);
        }
        return true;
    }

    @Override
    public boolean stop() {
        haConfigDao.expireServerOwnership(ManagementServerNode.getManagementServerId());
        return true;
    }

    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        // Health Check
        final int healthCheckWorkers = MaxConcurrentHealthCheckOperations.value();
        final int healthCheckQueueSize = MaxPendingHealthCheckOperations.value();
        healthCheckExecutor = new ThreadPoolExecutor(healthCheckWorkers, healthCheckWorkers,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(healthCheckQueueSize, true), new ThreadPoolExecutor.CallerRunsPolicy());

        // Activity Check
        final int activityCheckWorkers = MaxConcurrentActivityCheckOperations.value();
        final int activityCheckQueueSize = MaxPendingActivityCheckOperations.value();
        activityCheckExecutor = new ThreadPoolExecutor(activityCheckWorkers, activityCheckWorkers,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(activityCheckQueueSize, true), new ThreadPoolExecutor.CallerRunsPolicy());

        // Recovery
        final int recoveryOperationWorkers = MaxConcurrentRecoveryOperations.value();
        final int recoveryOperationQueueSize = MaxPendingRecoveryOperations.value();
        recoveryExecutor = new ThreadPoolExecutor(recoveryOperationWorkers, recoveryOperationWorkers,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(recoveryOperationQueueSize, true), new ThreadPoolExecutor.CallerRunsPolicy());

        // Fence
        final int fenceOperationWorkers = MaxConcurrentFenceOperations.value();
        final int fenceOperationQueueSize = MaxPendingFenceOperations.value();
        fenceExecutor = new ThreadPoolExecutor(fenceOperationWorkers, fenceOperationWorkers,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(fenceOperationQueueSize, true), new ThreadPoolExecutor.CallerRunsPolicy());

        pollManager.submitTask(new HAManagerBgPollTask());
        HAConfig.HAState.getStateMachine().registerListener(this);

        LOG.debug("HA manager has been configured.");
        return true;
    }

    public void setHaProviders(List<HAProvider<HAResource>> haProviders) {
        this.haProviders = haProviders;
    }

    @Override
    public String getConfigComponentName() {
        return HAManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {
                MaxConcurrentHealthCheckOperations,
                MaxPendingHealthCheckOperations,
                MaxConcurrentActivityCheckOperations,
                MaxPendingActivityCheckOperations,
                MaxConcurrentRecoveryOperations,
                MaxPendingRecoveryOperations,
                MaxConcurrentFenceOperations,
                MaxPendingFenceOperations
        };
    }

    /////////////////////////////////////////////////
    //////////////// Poll Tasks /////////////////////
    /////////////////////////////////////////////////

    private final class HAManagerBgPollTask extends ManagedContextRunnable implements BackgroundPollTask {
        @Override
        protected void runInContext() {
            HAConfig currentHaConfig = null;

            try {
                LOG.debug("HA health check task is running...");

                final List<HAConfig> haConfigList = new ArrayList<HAConfig>(haConfigDao.listAll());
                for (final HAConfig haConfig : haConfigList) {
                    currentHaConfig = haConfig;

                    if (haConfig == null) {
                        continue;
                    }

                    if (!checkHAOwnership(haConfig)) {
                        continue;
                    }

                    final HAResource resource = validateAndFindHAResource(haConfig);
                    if (resource == null) {
                        continue;
                    }

                    final HAProvider<HAResource> haProvider = validateAndFindHAProvider(haConfig, resource);
                    if (haProvider == null) {
                        continue;
                    }

                    switch (haConfig.getState()) {
                        case Available:
                        case Suspect:
                        case Degraded:
                        case Fenced:
                            final HealthCheckTask task = ComponentContext.inject(new HealthCheckTask(resource, haProvider, haConfig,
                                    HAProviderConfig.HealthCheckTimeout, healthCheckExecutor));
                            healthCheckExecutor.submit(task);
                            break;
                    default:
                        break;
                    }

                    final HAResourceCounter counter = getHACounter(haConfig.getResourceId(), haConfig.getResourceType());

                    if (haConfig.getState() == HAConfig.HAState.Suspect) {
                        if (counter.canPerformActivityCheck((Long)(haProvider.getConfigValue(HAProviderConfig.MaxActivityCheckInterval, resource)))) {
                            transitionHAState(HAConfig.Event.PerformActivityCheck, haConfig);
                        }
                    }

                    if (haConfig.getState() == HAConfig.HAState.Degraded) {
                        if (counter.canRecheckActivity((Long)(haProvider.getConfigValue(HAProviderConfig.MaxDegradedWaitTimeout, resource)))) {
                            transitionHAState(HAConfig.Event.PeriodicRecheckResourceActivity, haConfig);
                        }
                    }

                    if (haConfig.getState() == HAConfig.HAState.Recovering) {
                        if (counter.getRecoveryCounter() >= (Long) (haProvider.getConfigValue(HAProviderConfig.MaxRecoveryAttempts, resource))) {
                            transitionHAState(HAConfig.Event.RecoveryOperationThresholdExceeded, haConfig);
                        } else {
                            transitionHAState(HAConfig.Event.RetryRecovery, haConfig);
                        }
                    }

                    if (haConfig.getState() == HAConfig.HAState.Recovered) {
                        counter.markRecoveryStarted();
                        if (counter.canExitRecovery((Long)(haProvider.getConfigValue(HAProviderConfig.RecoveryWaitTimeout, resource)))) {
                            if (transitionHAState(HAConfig.Event.RecoveryWaitPeriodTimeout, haConfig)) {
                                counter.markRecoveryCompleted();
                            }
                        }
                    }

                    if (haConfig.getState() == HAConfig.HAState.Fencing && counter.canAttemptFencing()) {
                        transitionHAState(HAConfig.Event.RetryFencing, haConfig);
                    }
                }
            } catch (Throwable t) {
                if (currentHaConfig != null) {
                    LOG.error(String.format("Error trying to perform health checks in HA manager [%s].", currentHaConfig.getHaProvider()), t);
                } else {
                    LOG.error("Error trying to perform health checks in HA manager.", t);
                }
            }
        }

        @Override
        public Long getDelay() {
            return null;
        }
    }
}
