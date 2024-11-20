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
import org.apache.cloudstack.framework.events.EventBusException;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.mom.webhook.dao.WebhookDao;
import org.apache.cloudstack.mom.webhook.dao.WebhookDeliveryDao;
import org.apache.cloudstack.mom.webhook.vo.WebhookDeliveryVO;
import org.apache.cloudstack.mom.webhook.vo.WebhookVO;
import org.apache.cloudstack.utils.identity.ManagementServerNode;
import org.apache.cloudstack.webhook.WebhookHelper;
import org.apache.commons.lang3.StringUtils;

import com.cloud.api.query.vo.EventJoinVO;
import com.cloud.cluster.ManagementServerHostVO;
import com.cloud.cluster.dao.ManagementServerHostDao;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.EventCategory;
import com.cloud.event.dao.EventJoinDao;
import com.cloud.server.ManagementService;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.exception.CloudRuntimeException;

public class WebhookServiceImpl extends ManagerBase implements WebhookService, WebhookHelper {
    public static final String WEBHOOK_JOB_POOL_THREAD_PREFIX = "Webhook-Job-Executor";
    private ExecutorService webhookJobExecutor;
    private ScheduledExecutorService webhookDeliveriesCleanupExecutor;

    @Inject
    EventJoinDao eventJoinDao;
    @Inject
    WebhookDao webhookDao;
    @Inject
    protected WebhookDeliveryDao webhookDeliveryDao;
    @Inject
    ManagementServerHostDao managementServerHostDao;
    @Inject
    DomainDao domainDao;
    @Inject
    AccountManager accountManager;

    protected WebhookDeliveryThread getDeliveryJob(Event event, Webhook webhook, Pair<Integer, Integer> configs) {
        WebhookDeliveryThread.WebhookDeliveryContext<WebhookDeliveryThread.WebhookDeliveryResult> context =
                new WebhookDeliveryThread.WebhookDeliveryContext<>(null, event.getEventId(), webhook.getId());
        AsyncCallbackDispatcher<WebhookServiceImpl, WebhookDeliveryThread.WebhookDeliveryResult> caller =
                AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().deliveryCompleteCallback(null, null))
                .setContext(context);
        WebhookDeliveryThread job = new WebhookDeliveryThread(webhook, event, caller);
        job = ComponentContext.inject(job);
        job.setDeliveryTries(configs.first());
        job.setDeliveryTimeout(configs.second());
        return job;
    }

    protected List<Runnable> getDeliveryJobs(Event event) throws EventBusException {
        List<Runnable> jobs = new ArrayList<>();
        if (!EventCategory.ACTION_EVENT.getName().equals(event.getEventCategory())) {
            return jobs;
        }
        if (event.getResourceAccountId() == null) {
            logger.warn("Skipping delivering event [ID: {}, description: {}] to any webhook as account ID is missing",
                    event.getEventId(), event.getDescription());
            throw new EventBusException(String.format("Account missing for the event ID: %s", event.getEventUuid()));
        }
        List<Long> domainIds = new ArrayList<>();
        if (event.getResourceDomainId() != null) {
            domainIds.add(event.getResourceDomainId());
            domainIds.addAll(domainDao.getDomainParentIds(event.getResourceDomainId()));
        }
        List<WebhookVO> webhooks =
                webhookDao.listByEnabledForDelivery(event.getResourceAccountId(), domainIds);
        Map<Long, Pair<Integer, Integer>> domainConfigs = new HashMap<>();
        for (WebhookVO webhook : webhooks) {
            if (!domainConfigs.containsKey(webhook.getDomainId())) {
                domainConfigs.put(webhook.getDomainId(),
                        new Pair<>(WebhookDeliveryTries.valueIn(webhook.getDomainId()),
                        WebhookDeliveryTimeout.valueIn(webhook.getDomainId())));
            }
            Pair<Integer, Integer> configs = domainConfigs.get(webhook.getDomainId());
            WebhookDeliveryThread job = getDeliveryJob(event, webhook, configs);
            jobs.add(job);
        }
        return jobs;
    }

    protected Runnable getManualDeliveryJob(WebhookDelivery existingDelivery, Webhook webhook, String payload,
                AsyncCallFuture<WebhookDeliveryThread.WebhookDeliveryResult> future) {
        if (StringUtils.isBlank(payload)) {
            payload = "{ \"CloudStack\": \"works!\" }";
        }
        long eventId = Webhook.ID_DUMMY;
        String eventType = WebhookDelivery.TEST_EVENT_TYPE;
        String eventUuid = UUID.randomUUID().toString();
        String description = payload;
        String resourceAccountUuid = null;
        if (existingDelivery != null) {
            EventJoinVO eventJoinVO = eventJoinDao.findById(existingDelivery.getEventId());
            eventId = eventJoinVO.getId();
            eventType = eventJoinVO.getType();
            eventUuid = eventJoinVO.getUuid();
            description = existingDelivery.getPayload();
            resourceAccountUuid = eventJoinVO.getAccountUuid();
        } else {
            Account account = accountManager.getAccount(webhook.getAccountId());
            resourceAccountUuid = account.getUuid();
        }
        Event event = new Event(ManagementService.Name, EventCategory.ACTION_EVENT.toString(),
                eventType, null, null);
        event.setEventId(eventId);
        event.setEventUuid(eventUuid);
        event.setDescription(description);
        event.setResourceAccountUuid(resourceAccountUuid);
        ManualDeliveryContext<WebhookDeliveryThread.WebhookDeliveryResult> context =
                new ManualDeliveryContext<>(null, webhook, future);
        AsyncCallbackDispatcher<WebhookServiceImpl, WebhookDeliveryThread.WebhookDeliveryResult> caller =
                AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().manualDeliveryCompleteCallback(null, null))
                .setContext(context);
        WebhookDeliveryThread job = new WebhookDeliveryThread(webhook, event, caller);
        job.setDeliveryTries(WebhookDeliveryTries.valueIn(webhook.getDomainId()));
        job.setDeliveryTimeout(WebhookDeliveryTimeout.valueIn(webhook.getDomainId()));
        return job;
    }

    protected Void deliveryCompleteCallback(
            AsyncCallbackDispatcher<WebhookServiceImpl, WebhookDeliveryThread.WebhookDeliveryResult> callback,
            WebhookDeliveryThread.WebhookDeliveryContext<Webhook> context) {
        WebhookDeliveryThread.WebhookDeliveryResult result = callback.getResult();
        WebhookDeliveryVO deliveryVO = new WebhookDeliveryVO(context.getEventId(), context.getRuleId(),
                ManagementServerNode.getManagementServerId(), result.getHeaders(), result.getPayload(),
                result.isSuccess(), result.getResult(), result.getStarTime(), result.getEndTime());
        webhookDeliveryDao.persist(deliveryVO);
        return null;
    }

    protected Void manualDeliveryCompleteCallback(
            AsyncCallbackDispatcher<WebhookServiceImpl, WebhookDeliveryThread.WebhookDeliveryResult> callback,
            ManualDeliveryContext<WebhookDeliveryThread.WebhookDeliveryResult> context) {
        WebhookDeliveryThread.WebhookDeliveryResult result = callback.getResult();
        context.future.complete(result);
        return null;
    }

    protected long cleanupOldWebhookDeliveries(long deliveriesLimit) {
        Filter filter = new Filter(WebhookVO.class, "id", true, 0L, 50L);
        Pair<List<WebhookVO>, Integer> webhooksAndCount =
                webhookDao.searchAndCount(webhookDao.createSearchCriteria(), filter);
        List<WebhookVO> webhooks = webhooksAndCount.first();
        long count = webhooksAndCount.second();
        long processed = 0;
        do {
            for (WebhookVO webhook : webhooks) {
                webhookDeliveryDao.removeOlderDeliveries(webhook.getId(), deliveriesLimit);
                processed++;
            }
            if (processed < count) {
                filter.setOffset(processed);
                webhooks = webhookDao.listAll(filter);
            }
        } while (processed < count);
        return processed;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        try {
            webhookJobExecutor = Executors.newFixedThreadPool(WebhookDeliveryThreadPoolSize.value(),
                    new NamedThreadFactory(WEBHOOK_JOB_POOL_THREAD_PREFIX));
            webhookDeliveriesCleanupExecutor = Executors.newScheduledThreadPool(1,
                    new NamedThreadFactory("Webhook-Deliveries-Cleanup-Worker"));
        } catch (final Exception e) {
            throw new ConfigurationException("Unable to to configure WebhookServiceImpl");
        }
        return true;
    }

    @Override
    public boolean start() {
        long webhookDeliveriesCleanupInitialDelay = WebhookDeliveriesCleanupInitialDelay.value();
        long webhookDeliveriesCleanupInterval = WebhookDeliveriesCleanupInterval.value();
        logger.debug("Scheduling webhook deliveries cleanup task with initial delay={}s and interval={}s",
                webhookDeliveriesCleanupInitialDelay, webhookDeliveriesCleanupInterval);
        webhookDeliveriesCleanupExecutor.scheduleWithFixedDelay(new WebhookDeliveryCleanupWorker(),
                webhookDeliveriesCleanupInitialDelay, webhookDeliveriesCleanupInterval, TimeUnit.SECONDS);
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
                WebhookDeliveryTries,
                WebhookDeliveryThreadPoolSize,
                WebhookDeliveriesLimit,
                WebhookDeliveriesCleanupInitialDelay,
                WebhookDeliveriesCleanupInterval
        };
    }

    @Override
    public void deleteWebhooksForAccount(long accountId) {
        webhookDao.deleteByAccount(accountId);
    }

    @Override
    public List<? extends ControlledEntity> listWebhooksByAccount(long accountId) {
        return webhookDao.listByAccount(accountId);
    }

    @Override
    public void handleEvent(Event event) throws EventBusException {
        List<Runnable> jobs = getDeliveryJobs(event);
        for(Runnable job : jobs) {
            webhookJobExecutor.submit(job);
        }
    }

    @Override
    public WebhookDelivery executeWebhookDelivery(WebhookDelivery delivery, Webhook webhook, String payload)
            throws CloudRuntimeException {
        AsyncCallFuture<WebhookDeliveryThread.WebhookDeliveryResult> future = new AsyncCallFuture<>();
        Runnable job = getManualDeliveryJob(delivery, webhook, payload, future);
        webhookJobExecutor.submit(job);
        WebhookDeliveryThread.WebhookDeliveryResult result = null;
        WebhookDeliveryVO webhookDeliveryVO;
        try {
            result = future.get();
            if (delivery != null) {
                webhookDeliveryVO = new WebhookDeliveryVO(delivery.getEventId(), delivery.getWebhookId(),
                        ManagementServerNode.getManagementServerId(), result.getHeaders(), result.getPayload(),
                        result.isSuccess(), result.getResult(), result.getStarTime(), result.getEndTime());
                webhookDeliveryVO = webhookDeliveryDao.persist(webhookDeliveryVO);
            } else {
                webhookDeliveryVO = new WebhookDeliveryVO(ManagementServerNode.getManagementServerId(),
                        result.getHeaders(), result.getPayload(), result.isSuccess(), result.getResult(),
                        result.getStarTime(), result.getEndTime());
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.error(String.format("Failed to execute test webhook delivery due to: %s", e.getMessage()), e);
            throw new CloudRuntimeException("Failed to execute test webhook delivery");
        }
        return webhookDeliveryVO;
    }

    @Override
    public List<Class<?>> getCommands() {
        return new ArrayList<>();
    }

    static public class ManualDeliveryContext<T> extends AsyncRpcContext<T> {
        final Webhook webhook;
        final AsyncCallFuture<WebhookDeliveryThread.WebhookDeliveryResult> future;

        public ManualDeliveryContext(AsyncCompletionCallback<T> callback, Webhook webhook,
                 AsyncCallFuture<WebhookDeliveryThread.WebhookDeliveryResult> future) {
            super(callback);
            this.webhook = webhook;
            this.future = future;
        }

    }

    public class WebhookDeliveryCleanupWorker extends ManagedContextRunnable {

        protected void runCleanupForLongestRunningManagementServer() {
            try {
                ManagementServerHostVO msHost = managementServerHostDao.findOneByLongestRuntime();
                if (msHost == null || (msHost.getMsid() != ManagementServerNode.getManagementServerId())) {
                    logger.debug("Skipping the webhook delivery cleanup task on this management server");
                    return;
                }
                long deliveriesLimit = WebhookDeliveriesLimit.value();
                logger.debug("Clearing old deliveries for webhooks with limit={} using management server {}",
                        deliveriesLimit, msHost.getMsid());
                long processed = cleanupOldWebhookDeliveries(deliveriesLimit);
                logger.debug("Cleared old deliveries with limit={} for {} webhooks", deliveriesLimit, processed);
            } catch (Exception e) {
                logger.warn("Cleanup task failed to cleanup old webhook deliveries", e);
            }
        }

        @Override
        protected void runInContext() {
            GlobalLock gcLock = GlobalLock.getInternLock("WebhookDeliveriesCleanup");
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
