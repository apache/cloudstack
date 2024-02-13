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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.events.Event;
import org.apache.cloudstack.mom.webhook.dao.WebhookRuleDao;
import org.apache.cloudstack.mom.webhook.vo.WebhookRuleVO;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;

public class WebhookServiceImpl extends ManagerBase implements WebhookService {
    public static final String WEBHOOK_JOB_POOL_THREAD_PREFIX = "Webhook-Job-Executor";
    private ExecutorService webhookJobExecutor;
    private CloseableHttpClient closeableHttpClient;

    @Inject
    WebhookRuleDao webhookRuleDao;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        try {
            webhookJobExecutor = Executors.newFixedThreadPool(WebhookDispatcherThreadPoolSize.value(), new NamedThreadFactory(WEBHOOK_JOB_POOL_THREAD_PREFIX));
            closeableHttpClient = HttpClients.createDefault();
        } catch (final Exception e) {
            throw new ConfigurationException("Unable to to configure WebhookServiceImpl");
        }
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
                WebhookDispatcherThreadPoolSize
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
        List<WebhookRuleVO> rules = webhookRuleDao.listAll();
        // All global rules
        // All domain level rules for current and parent domains
        // All local rules
        List<Runnable> jobs = new ArrayList<>();
        for (WebhookRuleVO rule : rules) {
            WebhookDispatchThread job = new WebhookDispatchThread(closeableHttpClient, rule, event);
            job.setDispatchRetries(WebhookDispatchRetries.valueIn(rule.getDomainId()));
            job.setDeliveryTimeout(WebhookDeliveryTimeout.valueIn(rule.getDomainId()));
            jobs.add(job);
        }
        return jobs;
    }
}
