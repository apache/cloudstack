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
import org.apache.cloudstack.framework.events.EventBusException;

import com.cloud.utils.component.PluggableService;
import com.cloud.utils.exception.CloudRuntimeException;

public interface WebhookService extends PluggableService, Configurable {

    ConfigKey<Integer> WebhookDeliveryTimeout = new ConfigKey<>("Advanced", Integer.class,
            "webhook.delivery.timeout", "10",
            "Wait timeout (in seconds) for a webhook delivery delivery",
            true, ConfigKey.Scope.Domain);

    ConfigKey<Integer> WebhookDeliveryTries = new ConfigKey<>("Advanced", Integer.class,
            "webhook.delivery.tries", "3",
            "Number of tries to be made for a webhook delivery",
            true, ConfigKey.Scope.Domain);

    ConfigKey<Integer> WebhookDeliveryThreadPoolSize = new ConfigKey<>("Advanced", Integer.class,
            "webhook.delivery.thread.pool.size", "5",
            "Size of the thread pool for webhook deliveries",
            false, ConfigKey.Scope.Global);

    ConfigKey<Integer> WebhookDeliveriesLimit = new ConfigKey<>("Advanced", Integer.class,
            "webhook.deliveries.limit", "10",
            "Limit for the number of deliveries to keep in DB per webhook",
            true, ConfigKey.Scope.Global);

    ConfigKey<Integer> WebhookDeliveriesCleanupInitialDelay = new ConfigKey<>("Advanced", Integer.class,
            "webhook.deliveries.cleanup.initial.delay", "180",
            "Initial delay (in seconds) for webhook deliveries cleanup task",
            false, ConfigKey.Scope.Global);

    ConfigKey<Integer> WebhookDeliveriesCleanupInterval = new ConfigKey<>("Advanced", Integer.class,
            "webhook.deliveries.cleanup.interval", "3600",
            "Interval (in seconds) for cleaning up webhook deliveries",
            false, ConfigKey.Scope.Global);

    void handleEvent(Event event) throws EventBusException;
    WebhookDelivery executeWebhookDelivery(WebhookDelivery delivery, Webhook webhook, String payload)
            throws CloudRuntimeException;
}
