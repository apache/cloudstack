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

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.events.Event;

import com.cloud.utils.component.PluggableService;
import com.cloud.utils.exception.CloudRuntimeException;

public interface WebhookService extends PluggableService, Configurable {

    ConfigKey<Integer> WebhookDeliveryTimeout = new ConfigKey<>("Advanced", Integer.class,
            "webhook.delivery.timeout", "10",
            "Wait timeout (in seconds) for a webhook delivery dispatch",
            true, ConfigKey.Scope.Domain);

    ConfigKey<Integer> WebhookDispatchRetries = new ConfigKey<>("Advanced", Integer.class,
            "webhook.delivery.retries", "3",
            "Number of tries to be made for a webhook dispatch",
            true, ConfigKey.Scope.Domain);

    ConfigKey<Integer> WebhookDispatcherThreadPoolSize = new ConfigKey<>("Advanced", Integer.class,
            "webhook.dispatch.thread.pool.size", "5",
            "Size of the thread pool for webhook dispatchers",
            false, ConfigKey.Scope.Global);

    ConfigKey<Integer> WebhookDispatchHistoryLimit = new ConfigKey<>("Advanced", Integer.class,
            "webhook.dispatch.history.limit", "10",
            "Limit for number of deliveries to keep in history per webhook",
            true, ConfigKey.Scope.Global);

    ConfigKey<Integer> WebhookDispatchHistoryCleanupInterval = new ConfigKey<>("Advanced", Integer.class,
            "webhook.dispatch.history.cleanup.interval", "3600",
            "Interval (in seconds) for cleaning up webhook dispatch history",
            false, ConfigKey.Scope.Global);

    void handleEvent(Event event);
    WebhookDispatch testWebhookDispatch(WebhookRule rule, String payload) throws CloudRuntimeException;
}
