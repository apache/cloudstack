// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements. See the NOTICE file distributed with this
// work for additional information regarding copyright ownership. The ASF
// licenses this file to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance with the
// License. You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
// License for the specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.ai;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import com.cloud.event.ActionEvent;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.ai.engine.EngineJob;
import org.apache.cloudstack.ai.engine.EngineJobPoller;
import org.apache.cloudstack.ai.engine.EngineResponse;
import org.apache.cloudstack.ai.engine.EngineException;
import org.apache.cloudstack.ai.maintenance.AiMaintenanceListener;
import org.apache.cloudstack.ai.maintenance.MaintenanceDrain;
import org.apache.cloudstack.framework.events.EventBus;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import com.cloud.utils.component.ComponentContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.cloudstack.ai.engine.CallerContext;
import org.apache.cloudstack.ai.engine.EngineClient;
import org.apache.cloudstack.ai.api.command.ApproveAiModelCmd;
import org.apache.cloudstack.ai.api.command.AssignAiTuningProfileCmd;
import org.apache.cloudstack.ai.api.command.CancelAiBatchCmd;
import org.apache.cloudstack.ai.api.command.CheckAiHfModelCmd;
import org.apache.cloudstack.ai.api.command.CreateAiApiKeyCmd;
import org.apache.cloudstack.ai.api.command.CreateAiBatchCmd;
import org.apache.cloudstack.ai.api.command.CreateAiBudgetCmd;
import org.apache.cloudstack.ai.api.command.CreateAiGenerationProfileCmd;
import org.apache.cloudstack.ai.api.command.CreateAiGuardrailProfileCmd;
import org.apache.cloudstack.ai.api.command.CreateAiPlaygroundSessionCmd;
import org.apache.cloudstack.ai.api.command.CreateAiPrivateEndpointCmd;
import org.apache.cloudstack.ai.api.command.CreateAiServingProfileCmd;
import org.apache.cloudstack.ai.api.command.DeleteAiAdapterCmd;
import org.apache.cloudstack.ai.api.command.DeleteAiBudgetCmd;
import org.apache.cloudstack.ai.api.command.DeleteAiCertificateCmd;
import org.apache.cloudstack.ai.api.command.DeleteAiGenerationProfileCmd;
import org.apache.cloudstack.ai.api.command.DeleteAiGuardrailProfileCmd;
import org.apache.cloudstack.ai.api.command.DeleteAiModelDeploymentCmd;
import org.apache.cloudstack.ai.api.command.DeleteAiPrivateEndpointCmd;
import org.apache.cloudstack.ai.api.command.DeleteAiServingProfileCmd;
import org.apache.cloudstack.ai.api.command.DeployAiModelCmd;
import org.apache.cloudstack.ai.api.command.DiscoverAiGpuClassesCmd;
import org.apache.cloudstack.ai.api.command.DrainAiNodeCmd;
import org.apache.cloudstack.ai.api.command.GetAiBatchResultsCmd;
import org.apache.cloudstack.ai.api.command.GetAiEffectiveParametersCmd;
import org.apache.cloudstack.ai.api.command.ListAiAdaptersCmd;
import org.apache.cloudstack.ai.api.command.ListAiApiKeysCmd;
import org.apache.cloudstack.ai.api.command.ListAiBatchesCmd;
import org.apache.cloudstack.ai.api.command.ListAiBudgetsCmd;
import org.apache.cloudstack.ai.api.command.ListAiCertificatesCmd;
import org.apache.cloudstack.ai.api.command.ListAiEndpointsCmd;
import org.apache.cloudstack.ai.api.command.ListAiEvaluationsCmd;
import org.apache.cloudstack.ai.api.command.ListAiGenerationProfilesCmd;
import org.apache.cloudstack.ai.api.command.ListAiGpuCapacityCmd;
import org.apache.cloudstack.ai.api.command.ListAiGuardrailProfilesCmd;
import org.apache.cloudstack.ai.api.command.ListAiModelDeploymentsCmd;
import org.apache.cloudstack.ai.api.command.ListAiPrivateEndpointsCmd;
import org.apache.cloudstack.ai.api.command.ListAiModelsCmd;
import org.apache.cloudstack.ai.api.command.ListAiReconcilerRunsCmd;
import org.apache.cloudstack.ai.api.command.ListAiServingProfilesCmd;
import org.apache.cloudstack.ai.api.command.ListAiSetupChecksCmd;
import org.apache.cloudstack.ai.api.command.ListAiUsageCmd;
import org.apache.cloudstack.ai.api.command.MirrorAiModelCmd;
import org.apache.cloudstack.ai.api.command.PromoteAiRolloutCmd;
import org.apache.cloudstack.ai.api.command.PublishAiModelCmd;
import org.apache.cloudstack.ai.api.command.ReplaceAiCertificateCmd;
import org.apache.cloudstack.ai.api.command.RevokeAiApiKeyCmd;
import org.apache.cloudstack.ai.api.command.RollbackAiRolloutCmd;
import org.apache.cloudstack.ai.api.command.RunAiModelEvaluationCmd;
import org.apache.cloudstack.ai.api.command.RunAiSetupSelfTestCmd;
import org.apache.cloudstack.ai.api.command.SearchAiHfModelsCmd;
import org.apache.cloudstack.ai.api.command.UpdateAiBudgetCmd;
import org.apache.cloudstack.ai.api.command.UpdateAiGenerationProfileCmd;
import org.apache.cloudstack.ai.api.command.UpdateAiGuardrailProfileCmd;
import org.apache.cloudstack.ai.api.command.UpdateAiModelDeploymentCmd;
import org.apache.cloudstack.ai.api.command.UpdateAiServingProfileCmd;
import org.apache.cloudstack.ai.api.command.UploadAiAdapterCmd;
import org.apache.cloudstack.ai.api.command.UploadAiCertificateCmd;
import org.apache.cloudstack.ai.dao.AiDeploymentDao;
import org.apache.cloudstack.ai.engine.EngineTransport;
import org.apache.cloudstack.ai.engine.HttpEngineTransport;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;

import com.cloud.domain.Domain;
import com.cloud.domain.dao.DomainDao;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.utils.component.ManagerBase;

/**
 * Registers the plugin's commands and configuration (SPEC.md section 9).
 *
 * The command list is generated from the engine's command map
 * (aisvc/api/commands.py) rather than maintained here by hand, so a
 * command cannot exist on one side and be missing from the other.
 */
public class AiServicesManagerImpl extends ManagerBase
        implements AiServicesService, Configurable {

    @Inject
    private DomainDao domainDao;

    @Inject
    private AiDeploymentDao deploymentDao;

    private static final Logger LOG = LogManager.getLogger(AiServicesManagerImpl.class);

    /** GPU node VMs are named aisvc-<pool>-<ordinal> (CLAUDE.md). */
    static final String NODE_NAME_PREFIX = "aisvc-";

    @Inject
    private VMInstanceDao vmInstanceDao;

    @Inject
    private HostDao hostDao;

    private AiMaintenanceListener maintenanceListener;

    private EngineClient engineClient;
    private EngineTransport transport;

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> commands = new ArrayList<>();
        // Every v1 command from SPEC.md section 9. The list is kept in
        // step with the engine's routes by aisvc/api/commands.py and the
        // tests on both sides.
        commands.add(ApproveAiModelCmd.class);
        commands.add(AssignAiTuningProfileCmd.class);
        commands.add(CheckAiHfModelCmd.class);
        commands.add(CreateAiApiKeyCmd.class);
        commands.add(CreateAiBudgetCmd.class);
        commands.add(CreateAiGenerationProfileCmd.class);
        commands.add(CreateAiPlaygroundSessionCmd.class);
        commands.add(CreateAiServingProfileCmd.class);
        commands.add(DeleteAiBudgetCmd.class);
        commands.add(DeleteAiGenerationProfileCmd.class);
        commands.add(DeleteAiModelDeploymentCmd.class);
        commands.add(DeleteAiServingProfileCmd.class);
        commands.add(DeployAiModelCmd.class);
        commands.add(DiscoverAiGpuClassesCmd.class);
        commands.add(DrainAiNodeCmd.class);
        commands.add(GetAiEffectiveParametersCmd.class);
        commands.add(ListAiApiKeysCmd.class);
        commands.add(ListAiBudgetsCmd.class);
        commands.add(ListAiEndpointsCmd.class);
        commands.add(ListAiGenerationProfilesCmd.class);
        commands.add(ListAiGpuCapacityCmd.class);
        commands.add(ListAiModelDeploymentsCmd.class);
        commands.add(ListAiModelsCmd.class);
        commands.add(ListAiReconcilerRunsCmd.class);
        commands.add(ListAiServingProfilesCmd.class);
        commands.add(ListAiSetupChecksCmd.class);
        commands.add(ListAiUsageCmd.class);
        commands.add(MirrorAiModelCmd.class);
        commands.add(PromoteAiRolloutCmd.class);
        commands.add(PublishAiModelCmd.class);
        commands.add(RevokeAiApiKeyCmd.class);
        commands.add(RollbackAiRolloutCmd.class);
        commands.add(RunAiSetupSelfTestCmd.class);
        commands.add(SearchAiHfModelsCmd.class);
        commands.add(UpdateAiBudgetCmd.class);
        commands.add(UpdateAiGenerationProfileCmd.class);
        commands.add(UpdateAiModelDeploymentCmd.class);
        commands.add(UpdateAiServingProfileCmd.class);
        if (v2Enabled()) {
            commands.addAll(v2Commands());
        }
        return commands;
    }

    /**
     * Whether to register the Tier 2 command surface.
     *
     * <p>Its own method so the split can be tested without a
     * ConfigDepot; in production this is the global setting, which
     * reads false when nothing has set it.
     */
    boolean v2Enabled() {
        return AiServicesConfig.V2Enabled.value();
    }

    /**
     * The Tier 2 commands (SPEC.md section 8).
     *
     * <p>Generated like the rest, but held back: the engine marks them
     * {@code Release.V2}, and a GA install should not advertise an API
     * for features it does not run. An operator turns them on with
     * {@code ai.services.v2.enabled}.
     */
    private static List<Class<?>> v2Commands() {
        List<Class<?>> commands = new ArrayList<>();
        commands.add(CancelAiBatchCmd.class);
        commands.add(CreateAiBatchCmd.class);
        commands.add(CreateAiGuardrailProfileCmd.class);
        commands.add(CreateAiPrivateEndpointCmd.class);
        commands.add(DeleteAiAdapterCmd.class);
        commands.add(DeleteAiCertificateCmd.class);
        commands.add(DeleteAiGuardrailProfileCmd.class);
        commands.add(DeleteAiPrivateEndpointCmd.class);
        commands.add(GetAiBatchResultsCmd.class);
        commands.add(ListAiAdaptersCmd.class);
        commands.add(ListAiBatchesCmd.class);
        commands.add(ListAiCertificatesCmd.class);
        commands.add(ListAiEvaluationsCmd.class);
        commands.add(ListAiGuardrailProfilesCmd.class);
        commands.add(ListAiPrivateEndpointsCmd.class);
        commands.add(ReplaceAiCertificateCmd.class);
        commands.add(RunAiModelEvaluationCmd.class);
        commands.add(UpdateAiGuardrailProfileCmd.class);
        commands.add(UploadAiAdapterCmd.class);
        commands.add(UploadAiCertificateCmd.class);
        return commands;
    }

    @Override
    public String getConfigComponentName() {
        return AiServicesManagerImpl.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return AiServicesConfig.all();
    }

    @Override
    public boolean isEnabledInZone(long zoneId) {
        return Boolean.TRUE.equals(AiServicesConfig.Enabled.valueIn(zoneId));
    }

    @Override
    public synchronized EngineClient getEngineClient() {
        if (engineClient == null) {
            List<String> urls = EngineClient.parseUrls(AiServicesConfig.EngineUrls.value());
            engineClient = new EngineClient(urls, resolveTransport());
        }
        return engineClient;
    }

    private EngineTransport resolveTransport() {
        if (transport != null) {
            return transport;
        }
        return new HttpEngineTransport(
                AiServicesConfig.EngineTimeout.value(),
                AiServicesConfig.EngineClientCert.value(),
                AiServicesConfig.EngineClientKey.value(),
                AiServicesConfig.EngineTrustStore.value(),
                AiServicesConfig.EngineTrustStorePassword.value());
    }

    /** Injection point for tests; production uses the mTLS transport. */
    public synchronized void setTransport(EngineTransport transport) {
        this.transport = transport;
        this.engineClient = null;
    }

    /**
     * Build the caller context from the CloudStack session.
     *
     * Never from request parameters: the engine trusts this assertion and
     * filters every response by it, so a caller able to influence it could
     * read another tenant's data.
     */
    @Override
    public CallerContext currentCaller() {
        CallContext context = CallContext.current();
        if (context == null) {
            throw new IllegalStateException("no CloudStack call context on this thread");
        }
        Account account = context.getCallingAccount();
        User user = context.getCallingUser();
        return new CallerContext(
                String.valueOf(user.getUuid()),
                account.getAccountName(),
                domainPathOf(account.getDomainId()),
                roleNameOf(account));
    }

    /**
     * The caller's full domain path, for example /Customers/Acme.
     *
     * The engine decides domain-admin visibility by prefix-matching this
     * against a resource's domain, so a wrong or partial path either
     * hides a domain's own resources or exposes a sibling's. Resolved
     * from the domain DAO rather than assembled from an id.
     */
    String domainPathOf(long domainId) {
        Domain domain = domainDao == null ? null : domainDao.findById(domainId);
        if (domain == null || domain.getPath() == null || domain.getPath().isEmpty()) {
            throw new IllegalStateException(
                    "cannot resolve the path of domain " + domainId
                            + "; refusing to call the engine with an unknown domain");
        }
        String path = domain.getPath();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        // CloudStack stores paths with a trailing slash; the engine
        // compares them as prefixes, where a trailing slash would make
        // /Customers/Acme/ fail to match /Customers/Acme.
        return path.length() > 1 && path.endsWith("/")
                ? path.substring(0, path.length() - 1)
                : path;
    }

    private String roleNameOf(Account account) {
        switch (account.getType()) {
            case ADMIN:
                return "Admin";
            case DOMAIN_ADMIN:
            case RESOURCE_DOMAIN_ADMIN:
                return "DomainAdmin";
            default:
                return "User";
        }
    }

    /**
     * Remove an account's deployments (SPEC.md section 9).
     *
     * Only the ownership rows: the engine owns the running workloads and
     * removes them when it next reconciles, because the spec they were
     * deployed from no longer lists them. Deleting here and there would
     * race.
     *
     * NOTE: CloudStack 4.23 exposes no plugin-facing account-cleanup
     * interface — AccountCleanupTask is internal to AccountManagerImpl —
     * so this is not yet wired to account deletion. See PROGRESS.md.
     */
    @Override
    public int cleanupAccount(long accountId) {
        if (deploymentDao == null) {
            return 0;
        }
        return deploymentDao.removeByAccount(accountId);
    }

    /**
     * Run a command's work so CloudStack records how it ended.
     *
     * <p>Two things have to be true for an event to appear, and both
     * were learned from a server that recorded none.
     *
     * <p>First, this method has to be on a bean. The advisor in
     * {@code spring-core-system-context-inheritable.xml} is
     * {@code execution(* *(..)) && @annotation(ActionEvent)}, so it
     * wraps beans and nothing else; a command carrying the same
     * annotation is never proxied and never intercepted. The manager
     * is a bean, so a call through the injected {@code AiServicesService}
     * interface goes through the proxy and the interceptor sees it.
     *
     * <p>Second, the event type has to reach the interceptor through
     * the {@link CallContext}. {@code interceptComplete} returns early
     * when the annotation's {@code eventType} is empty, and the
     * fallback it would otherwise use, {@code ctx.getEventType()}, is
     * never populated — nothing in CloudStack's API path calls
     * {@code setEventType}. So the context is stamped here, inside the
     * proxied call, before the work runs: {@code interceptComplete} and
     * {@code interceptException} both read it afterwards and record
     * Completed or Failed against the command's own type.
     *
     * <p>{@code async = false} is deliberate. The interceptor's
     * {@code interceptStart} publishes a Started event only when the
     * annotation says async, and for an async command {@code ApiServer}
     * has already written the Scheduled event before the job began;
     * asking for a second one would put two openers against one close.
     */
    @Override
    @ActionEvent(eventType = "", eventDescription = "", async = false)
    public void recordEvent(String eventType, String eventDescription, Runnable work) {
        CallContext ctx = CallContext.current();
        if (ctx != null) {
            ctx.setEventType(eventType);
            ctx.setEventDescription(eventDescription);
        }
        work.run();
    }

    /**
     * Which GPU node VMs are running on a host.
     *
     * <p>Asked of CloudStack rather than the engine: CloudStack is the
     * IaaS authority and already knows what runs where, and the naming
     * scheme {@code aisvc-<pool>-<ordinal>} is what identifies ours
     * (CLAUDE.md). The engine would have to be told the same thing to
     * answer it, so there is nothing to gain by routing through it.
     */
    List<String> gpuNodesOnHost(long hostId) {
        if (vmInstanceDao == null) {
            return new ArrayList<>();
        }
        List<String> nodes = new ArrayList<>();
        for (VMInstanceVO vm : vmInstanceDao.listUpByHostId(hostId)) {
            if (vm.getInstanceName() != null && vm.getInstanceName().startsWith(NODE_NAME_PREFIX)) {
                nodes.add(vm.getInstanceName());
            }
        }
        return nodes;
    }

    /**
     * Start listening for host maintenance, where the install allows it.
     *
     * <p>Returns normally either way: a missing event bus is a
     * limitation to report, not a reason to refuse to start. See
     * {@link AiMaintenanceListener} for why this hook and no better one.
     */
    @Override
    public boolean start() {
        // CloudStack has no way for a plugin JAR to join its schema
        // upgrade chain, so the one table this plugin owns is created
        // here. See AiServicesSchema for why, and why a failure is
        // logged rather than fatal.
        applySchema();

        maintenanceListener = new AiMaintenanceListener(
                new MaintenanceDrain(
                        this::gpuNodesOnHost,
                        this::drainNodeForMaintenance,
                        this::alertForcedDrainNeeded,
                        AiServicesConfig.MaintenanceDrainTimeout.value()),
                this::hostIdForUuid);
        maintenanceListener.subscribe(resolveEventBus());
        return true;
    }

    /**
     * Create the plugin's table if it is not there.
     *
     * <p>Its own method so startup can be tested without a database,
     * the same seam {@link #v2Enabled()} uses.
     */
    boolean applySchema() {
        return AiServicesSchema.applyToCloudDb();
    }

    /**
     * The configured event bus, or null when there is none.
     *
     * <p>No event bus plugin is enabled in a default CloudStack install,
     * so its absence is the normal case and not an error. CloudStack's own
     * code resolves it this way for the same reason.
     *
     * <p>Package-private so startup can be tested without a Spring
     * context, the same seam {@link #v2Enabled()} and
     * {@link #applySchema()} use. Note that under {@code -ea}
     * CloudStack's {@code ComponentContext.getComponent} asserts rather
     * than throwing one of the exceptions caught here.
     */
    EventBus resolveEventBus() {
        try {
            return ComponentContext.getComponent(EventBus.class);
        } catch (NoSuchBeanDefinitionException | IllegalStateException absent) {
            return null;
        }
    }

    private Long hostIdForUuid(String uuid) {
        if (hostDao == null || uuid == null) {
            return null;
        }
        HostVO host = hostDao.findByUuid(uuid);
        return host == null ? null : host.getId();
    }

    private MaintenanceDrain.Outcome drainNodeForMaintenance(
            String nodeId, CallerContext caller, int timeoutSeconds) {
        try {
            EngineResponse accepted = getEngineClient()
                    .post("/v1/nodes/" + nodeId + "/drain", caller, "");
            if (!accepted.isAccepted()) {
                return MaintenanceDrain.Outcome.TIMED_OUT;
            }
            EngineJob started = EngineJob.parse(accepted.getBody());
            EngineJobPoller poller = new EngineJobPoller(
                    getEngineClient(), timeoutSeconds * 1000L);
            EngineJob job = poller.await(started.getJobId(), caller);
            // A failed drain job is the engine saying there is not enough
            // capacity elsewhere, which is the operator's call to make.
            return job.isSuccess()
                    ? MaintenanceDrain.Outcome.DRAINED
                    : MaintenanceDrain.Outcome.NEEDS_FORCE;
        } catch (EngineException e) {
            LOG.warn("Draining {} failed before maintenance: {}", nodeId, e.getMessage());
            return MaintenanceDrain.Outcome.TIMED_OUT;
        }
    }

    private void alertForcedDrainNeeded(long hostId, String nodeId, String reason) {
        // Maintenance has no user session behind it, so this is logged
        // rather than raised through CallContext, which would be null here.
        LOG.warn("AI Services: host {} needs a forced drain of {} ({}). Event {}.",
                hostId, nodeId, reason, AiServicesEvents.NODE_DRAIN);
    }

    /** Test seam for the DAO, which is otherwise Spring-injected. */
    void setDeploymentDao(AiDeploymentDao dao) {
        this.deploymentDao = dao;
    }

    /** Test seam for the domain DAO. */
    void setDomainDao(DomainDao dao) {
        this.domainDao = dao;
    }
}
