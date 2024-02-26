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

package org.apache.cloudstack.mom.webhook;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.framework.async.AsyncCallbackDispatcher;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.framework.async.AsyncRpcContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.events.Event;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.mom.webhook.dao.WebhookDispatchDao;
import org.apache.cloudstack.mom.webhook.dao.WebhookRuleDao;
import org.apache.cloudstack.mom.webhook.vo.WebhookDispatchVO;
import org.apache.cloudstack.mom.webhook.vo.WebhookRuleVO;
import org.apache.cloudstack.utils.identity.ManagementServerNode;
import org.apache.cloudstack.webhook.WebhookHelper;
import org.apache.commons.lang3.StringUtils;

import com.cloud.cluster.ManagementServerHostVO;
import com.cloud.cluster.dao.ManagementServerHostDao;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.EventCategory;
import com.cloud.server.ManagementService;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.exception.CloudRuntimeException;

public class WebhookServiceImpl extends ManagerBase implements WebhookService, WebhookHelper {
    public static final String WEBHOOK_JOB_POOL_THREAD_PREFIX = "Webhook-Job-Executor";
    private ExecutorService webhookJobExecutor;
    private ScheduledExecutorService webhookDispatchCleanupExecutor;

    @Inject
    WebhookRuleDao webhookRuleDao;
    @Inject
    protected WebhookDispatchDao webhookDispatchDao;
    @Inject
    ManagementServerHostDao managementServerHostDao;
    @Inject
    DomainDao domainDao;
    @Inject
    AccountManager accountManager;

    protected WebhookDispatchThread getDispatchJob(Event event, WebhookRule rule, Pair<Integer, Integer> configs) {
        WebhookDispatchThread.WebhookDispatchContext<WebhookDispatchThread.WebhookDispatchResult> context =
                new WebhookDispatchThread.WebhookDispatchContext<>(null, event.getEventId(), rule.getId());
        AsyncCallbackDispatcher<WebhookServiceImpl, WebhookDispatchThread.WebhookDispatchResult> caller =
                AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().dispatchCompleteCallback(null, null))
                .setContext(context);
        WebhookDispatchThread job = new WebhookDispatchThread(rule, event, caller);
        job = ComponentContext.inject(job);
        job.setDispatchRetries(configs.first());
        job.setDeliveryTimeout(configs.second());
        return job;
    }

    protected List<Runnable> getDispatchJobs(Event event) {
        List<Runnable> jobs = new ArrayList<>();
        if (!EventCategory.ACTION_EVENT.getName().equals(event.getEventCategory()) ||
                event.getResourceAccountId() == null) {
            return jobs;
        }
        List<Long> domainIds = new ArrayList<>();
        if (event.getResourceDomainId() != null) {
            domainIds.add(event.getResourceDomainId());
            domainIds.addAll(domainDao.getDomainParentIds(event.getResourceDomainId()));
        }
        List<WebhookRuleVO> rules =
                webhookRuleDao.listByEnabledRulesForDispatch(event.getResourceAccountId(), domainIds);
        Map<Long, Pair<Integer, Integer>> domainConfigs = new HashMap<>();
        for (WebhookRuleVO rule : rules) {
            if (!domainConfigs.containsKey(rule.getDomainId())) {
                domainConfigs.put(rule.getDomainId(), new Pair<>(WebhookDispatchRetries.valueIn(rule.getDomainId()),
                        WebhookDeliveryTimeout.valueIn(rule.getDomainId())));
            }
            Pair<Integer, Integer> configs = domainConfigs.get(rule.getDomainId());
            WebhookDispatchThread job = getDispatchJob(event, rule, configs);
            jobs.add(job);
        }
        return jobs;
    }

    protected Runnable getTestDispatchJobs(WebhookRule rule, String payload,
               AsyncCallFuture<WebhookDispatchThread.WebhookDispatchResult> future) {
        if (StringUtils.isBlank(payload)) {
            payload = "{ \"CloudStack\": \"works!\" }";
        }
        Event event = new Event(ManagementService.Name, EventCategory.ACTION_EVENT.toString(),
                "TEST.WEBHOOK", null, null);
        event.setEventId(WebhookRule.ID_DUMMY_RULE);
        event.setEventUuid(UUID.randomUUID().toString());
        event.setDescription(payload);
        Account account = accountManager.getAccount(rule.getAccountId());
        event.setResourceAccountId(account.getId());
        event.setResourceAccountUuid(account.getUuid());
        event.setResourceDomainId(account.getDomainId());
        Pair<Integer, Integer> configs = new Pair<>(WebhookDispatchRetries.valueIn(rule.getDomainId()),
                WebhookDeliveryTimeout.valueIn(rule.getDomainId()));
//        WebhookDispatchThread job = getDispatchJob(event, rule, configs);
        TestDispatchContext<WebhookDispatchThread.WebhookDispatchResult> context =
                new TestDispatchContext<>(null, rule, future);
        AsyncCallbackDispatcher<WebhookServiceImpl, WebhookDispatchThread.WebhookDispatchResult> caller =
                AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().testDispatchCompleteCallback(null, null))
                .setContext(context);
        WebhookDispatchThread job = new WebhookDispatchThread(rule, event, caller);
        return job;
    }

    protected Void dispatchCompleteCallback(
            AsyncCallbackDispatcher<WebhookServiceImpl, WebhookDispatchThread.WebhookDispatchResult> callback,
            WebhookDispatchThread.WebhookDispatchContext<WebhookRule> context) {
        WebhookDispatchThread.WebhookDispatchResult result = callback.getResult();
        WebhookDispatchVO dispatchVO = new WebhookDispatchVO(context.getEventId(), context.getRuleId(),
                ManagementServerNode.getManagementServerId(), result.getPayload(), result.isSuccess(),
                result.getResult(), result.getStarTime(), result.getEndTime());
        webhookDispatchDao.persist(dispatchVO);
        return null;
    }

    protected Void testDispatchCompleteCallback(
            AsyncCallbackDispatcher<WebhookServiceImpl, WebhookDispatchThread.WebhookDispatchResult> callback,
            TestDispatchContext<WebhookDispatchThread.WebhookDispatchResult> context) {
        WebhookDispatchThread.WebhookDispatchResult result = callback.getResult();
        context.future.complete(result);
        return null;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        try {
            webhookJobExecutor = Executors.newFixedThreadPool(WebhookDispatcherThreadPoolSize.value(),
                    new NamedThreadFactory(WEBHOOK_JOB_POOL_THREAD_PREFIX));
            webhookDispatchCleanupExecutor = Executors.newScheduledThreadPool(1,
                    new NamedThreadFactory("Webhook-Dispatch-Cleanup-Worker"));
        } catch (final Exception e) {
            throw new ConfigurationException("Unable to to configure WebhookServiceImpl");
        }
        return true;
    }

    @Override
    public boolean start() {
        long webhookDispatchCleanupInterval = WebhookDispatchHistoryCleanupInterval.value();
        webhookDispatchCleanupExecutor.scheduleWithFixedDelay(new WebhookDispatchCleanupWorker(),
                (5 * 60), webhookDispatchCleanupInterval, TimeUnit.SECONDS);
        return true;
    }

    @Override
    public boolean stop() {
        webhookJobExecutor.shutdown();
        return true;
    }

    @Override
    public String getConfigComponentName() {
        return WebhookService.class.getName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[]{
                WebhookDeliveryTimeout,
                WebhookDispatchRetries,
                WebhookDispatcherThreadPoolSize,
                WebhookDispatchHistoryLimit,
                WebhookDispatchHistoryCleanupInterval
        };
    }

    @Override
    public void deleteRulesForAccount(long accountId) {
        webhookRuleDao.deleteByAccount(accountId);
    }

    @Override
    public List<? extends ControlledEntity> listByAccount(long accountId) {
        return webhookRuleDao.listByAccount(accountId);
    }

    @Override
    public void handleEvent(Event event) {
        List<Runnable> jobs = getDispatchJobs(event);
        for(Runnable job : jobs) {
            webhookJobExecutor.submit(job);
        }
    }

    @Override
    public WebhookDispatch testWebhookDispatch(WebhookRule rule, String payload) throws CloudRuntimeException {
        AsyncCallFuture<WebhookDispatchThread.WebhookDispatchResult> future = new AsyncCallFuture<>();
        Runnable job = getTestDispatchJobs(rule, payload, future);
        webhookJobExecutor.submit(job);
        WebhookDispatchThread.WebhookDispatchResult result = null;
        WebhookDispatchVO webhookDispatchVO;
        try {
            result = future.get();
            webhookDispatchVO = new WebhookDispatchVO(ManagementServerNode.getManagementServerId(),
                    result.getPayload(), result.isSuccess(), result.getResult(),
                    result.getStarTime(), result.getEndTime());
        } catch (InterruptedException | ExecutionException e) {
            logger.error(String.format("Failed to execute test webhook dispatch due to: %s", e.getMessage()), e);
            throw new CloudRuntimeException("Failed to execute test webhook dispatch");
        }
        return webhookDispatchVO;
    }

    @Override
    public List<Class<?>> getCommands() {
        return new ArrayList<>();
    }

    static public class TestDispatchContext<T> extends AsyncRpcContext<T> {
        final WebhookRule webhookRule;
        final AsyncCallFuture<WebhookDispatchThread.WebhookDispatchResult> future;

        public TestDispatchContext(AsyncCompletionCallback<T> callback, WebhookRule rule,
               AsyncCallFuture<WebhookDispatchThread.WebhookDispatchResult> future) {
            super(callback);
            this.webhookRule = rule;
            this.future = future;
        }

    }

    public class WebhookDispatchCleanupWorker extends ManagedContextRunnable {

        protected void runCleanupForLongestRunningManagementServer() {
            ManagementServerHostVO msHost = managementServerHostDao.findOneByLongestRuntime();
            if (msHost == null || (msHost.getMsid() != ManagementServerNode.getManagementServerId())) {
                logger.trace("Skipping the webhook dispatch cleanup task on this management server");
                return;
            }
            long limit = WebhookDispatchHistoryLimit.value();
            List<WebhookRuleVO> webhooks = webhookRuleDao.listAll();
            for (WebhookRuleVO webhook : webhooks) {
                webhookDispatchDao.removeOlderDispatches(webhook.getId(), limit);
            }
        }

        @Override
        protected void runInContext() {
            GlobalLock gcLock = GlobalLock.getInternLock("WebhookDispatchHistoryCleanup");
            try {
                if (gcLock.lock(3)) {
                    try {
                        runCleanupForLongestRunningManagementServer();
                    } finally {
                        gcLock.unlock();
                    }
                }
            } finally {
                gcLock.releaseRef();
            }
        }
    }
}
