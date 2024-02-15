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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.framework.async.AsyncCallbackDispatcher;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.events.Event;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.mom.webhook.dao.WebhookDispatchDao;
import org.apache.cloudstack.mom.webhook.dao.WebhookRuleDao;
import org.apache.cloudstack.mom.webhook.vo.WebhookDispatchVO;
import org.apache.cloudstack.mom.webhook.vo.WebhookRuleVO;
import org.apache.cloudstack.utils.identity.ManagementServerNode;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;

import com.cloud.cluster.ManagementServerHostVO;
import com.cloud.cluster.dao.ManagementServerHostDao;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.EventCategory;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.GlobalLock;

public class WebhookServiceImpl extends ManagerBase implements WebhookService {
    public static final Logger LOGGER = Logger.getLogger(WebhookApiServiceImpl.class.getName());
    public static final String WEBHOOK_JOB_POOL_THREAD_PREFIX = "Webhook-Job-Executor";
    private ExecutorService webhookJobExecutor;
    private ScheduledExecutorService webhookDispatchCleanupExecutor;
    private CloseableHttpClient closeableHttpClient;

    @Inject
    WebhookRuleDao webhookRuleDao;
    @Inject
    protected WebhookDispatchDao webhookDispatchDao;
    @Inject
    ManagementServerHostDao managementServerHostDao;
    @Inject
    DomainDao domainDao;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        try {
            webhookJobExecutor = Executors.newFixedThreadPool(WebhookDispatcherThreadPoolSize.value(), new NamedThreadFactory(WEBHOOK_JOB_POOL_THREAD_PREFIX));
            webhookDispatchCleanupExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("Webhook-Dispatch-Cleanup-Worker"));
            closeableHttpClient = HttpClients.createDefault();
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
    public void handleEvent(Event event) {
        List<Runnable> jobs = getDispatchJobs(event);
        for(Runnable job : jobs) {
            webhookJobExecutor.submit(job);
        }
    }

    @Override
    public List<Class<?>> getCommands() {
        return new ArrayList<>();
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
        List<WebhookRuleVO> rules = webhookRuleDao.listByEnabledRulesForDispatch(event.getResourceAccountId(), domainIds);
        Map<Long, Pair<Integer, Integer>> domainConfigs = new HashMap<>();
        for (WebhookRuleVO rule : rules) {
            if (!domainConfigs.containsKey(rule.getDomainId())) {
                domainConfigs.put(rule.getDomainId(), new Pair<>(WebhookDispatchRetries.valueIn(rule.getDomainId()),
                        WebhookDeliveryTimeout.valueIn(rule.getDomainId())));
            }
            Pair<Integer, Integer> configs = domainConfigs.get(rule.getDomainId());
            WebhookDispatchThread.WebhookDispatchContext<WebhookDispatchThread.WebhookDispatchResult> context =
                    new WebhookDispatchThread.WebhookDispatchContext<>(null, event.getEventId(), rule.getId());
            AsyncCallbackDispatcher<WebhookServiceImpl, WebhookDispatchThread.WebhookDispatchResult> caller =
                    AsyncCallbackDispatcher.create(this);
            caller.setCallback(caller.getTarget().dispatchCompleteCallback(null, null))
                    .setContext(context);
            WebhookDispatchThread job = new WebhookDispatchThread(closeableHttpClient, rule, event, caller);
            job = ComponentContext.inject(job);
            job.setDispatchRetries(configs.first());
            job.setDeliveryTimeout(configs.second());
            jobs.add(job);
        }
        return jobs;
    }

    protected Void dispatchCompleteCallback(
            AsyncCallbackDispatcher<WebhookServiceImpl,WebhookDispatchThread.WebhookDispatchResult> callback,
            WebhookDispatchThread.WebhookDispatchContext<WebhookRule> context) {
        WebhookDispatchThread.WebhookDispatchResult result = callback.getResult();;
        WebhookDispatchVO dispatchVO = new WebhookDispatchVO(context.getEventId(), context.getRuleId(),
                ManagementServerNode.getManagementServerId(), result.getPayload(), result.isSuccess(),
                result.getResult(),  result.getStarTime(), result.getEndTime());
        webhookDispatchDao.persist(dispatchVO);
        return null;
    }


    public class WebhookDispatchCleanupWorker extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            GlobalLock gcLock = GlobalLock.getInternLock("WebhookDispatchHistoryCleanup");
            try {
                if (gcLock.lock(3)) {
                    try {
                        ManagementServerHostVO msHost = managementServerHostDao.findOneByLongestRuntime();
                        if (msHost == null || (msHost.getMsid() != ManagementServerNode.getManagementServerId())) {
                            LOGGER.trace("Skipping the webhook dispatch cleanup task on this management server");
                            return;
                        }
                        long limit = WebhookDispatchHistoryLimit.value();
                        webhookDispatchDao.removeOlderDispatches(limit);
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
