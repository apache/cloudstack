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

import java.net.URI;

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

    ConfigKey<String> WebhookDeliveryBlocklist = new ConfigKey<>("Advanced", String.class,
            "webhook.delivery.blocklist",
            "0.0.0.0/8,10.0.0.0/8,100.64.0.0/10,127.0.0.0/8,169.254.0.0/16,172.16.0.0/12,"
                    + "192.0.0.0/24,192.0.2.0/24,192.88.99.0/24,192.168.0.0/16,198.18.0.0/15,"
                    + "198.51.100.0/24,203.0.113.0/24,224.0.0.0/4,240.0.0.0/4,"
                    + "::1/128,::/128,::ffff:0:0/96,64:ff9b::/96,64:ff9b:1::/48,100::/64,"
                    + "2001::/32,2001:db8::/32,2002::/16,fc00::/7,fe80::/10,ff00::/8",
            "Comma-separated list of IPv4/IPv6 CIDR ranges where webhook deliveries are prohibited "
                    + "from accessing. Validation is performed against the resolved destination IP "
                    + "addresses.",
            true, ConfigKey.Scope.Domain);

    ConfigKey<Boolean> WebhookDeliveryBlockLocalAddresses = new ConfigKey<>("Hidden", Boolean.class,
            "webhook.delivery.block.local.addresses", "true",
            "Whether webhook deliveries are prohibited from accessing IP addresses assigned to "
                    + "the local management server. Validation is performed against resolved "
                    + "destination IP addresses.",
            true, ConfigKey.Scope.Global);

    ConfigKey<Boolean> WebhookDeliveryAllowRedirects = new ConfigKey<>("Advanced", Boolean.class,
            "webhook.delivery.allow.redirects", "false",
            "Whether webhook deliveries are allowed to follow HTTP redirects. If enabled, "
                    + "each redirect target is validated against the destination blocklist and "
                    + "local management server address restrictions, when enabled.",
            true, ConfigKey.Scope.Domain);

    ConfigKey<Boolean> WebhookDeliveryAllowHttp = new ConfigKey<>("Advanced", Boolean.class,
            "webhook.delivery.allow.http", "false",
            "Whether unencrypted HTTP URLs are allowed as webhook destinations. When false, "
                    + "only HTTPS URLs are permitted.",
            true, ConfigKey.Scope.Domain);

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
    WebhookDelivery executeWebhookDelivery(WebhookDelivery delivery, Webhook webhook, String payload, URI uri)
            throws CloudRuntimeException;
    void invalidateWebhooksCache();
    void invalidateWebhookFiltersCache(long webhookId);
}
