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
package org.apache.cloudstack.ai.maintenance;

import java.util.UUID;
import java.util.function.Function;

import org.apache.cloudstack.ai.engine.CallerContext;
import org.apache.cloudstack.framework.events.Event;
import org.apache.cloudstack.framework.events.EventBus;
import org.apache.cloudstack.framework.events.EventBusException;
import org.apache.cloudstack.framework.events.EventSubscriber;
import org.apache.cloudstack.framework.events.EventTopic;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Hears about host maintenance and drains GPU nodes first (SPEC.md section 7).
 *
 * <p><b>This is the best hook CloudStack 4.23 offers, and it is not a good
 * one.</b> There is no synchronous pre-maintenance extension point:
 * {@code ResourceStateAdapter} covers host creation and deletion,
 * the agent {@code Listener} covers connect, disconnect and removal, and
 * {@code ResourceState} is driven by the older {@code StateMachine}, which
 * accepts no listeners. The event bus is what is left, and it is
 * asynchronous — the host has already begun its transition by the time
 * this runs — and no event bus plugin is enabled in a default install.
 *
 * <p>So this subscribes where it can, and where it cannot it says so
 * loudly at startup rather than leaving an operator believing that drains
 * happen on their own. Either way {@code drainAiNode} remains the
 * dependable path, and the UI's maintenance screen should say so.
 */
public class AiMaintenanceListener implements EventSubscriber {

    private static final Logger LOG = LogManager.getLogger(AiMaintenanceListener.class);

    /** CloudStack's {@code EventTypes.EVENT_MAINTENANCE_PREPARE}. */
    public static final String MAINTENANCE_EVENT = "MAINT.PREPARE";

    /** The resource type CloudStack tags host maintenance events with. */
    public static final String HOST_CATEGORY = "Host";

    private final MaintenanceDrain drain;
    private final Function<String, Long> resolveHostId;
    private UUID subscriptionId;
    private String inactiveReason = "";

    public AiMaintenanceListener(MaintenanceDrain drain, Function<String, Long> resolveHostId) {
        this.drain = drain;
        this.resolveHostId = resolveHostId;
    }

    /** The one topic this listens on: host maintenance, nothing else. */
    public static EventTopic topic() {
        return new EventTopic("ActionEvent", MAINTENANCE_EVENT, HOST_CATEGORY, null, null);
    }

    /**
     * Subscribe to the bus, if there is one.
     *
     * @return true when automatic draining is live; false when it is not,
     *         in which case {@link #inactiveReason()} says why and what to
     *         do instead.
     */
    public boolean subscribe(EventBus bus) {
        if (bus == null) {
            inactiveReason = "no event bus is configured, so host maintenance will not "
                    + "drain GPU nodes automatically; enable an event bus plugin, or run "
                    + "drainAiNode on each GPU node before putting its host into maintenance";
            LOG.warn("AI Services: {}", inactiveReason);
            return false;
        }

        try {
            subscriptionId = UUID.randomUUID();
            bus.subscribe(topic(), this);
            LOG.info("AI Services: listening for host maintenance to drain GPU nodes");
            return true;
        } catch (EventBusException e) {
            inactiveReason = "could not subscribe to the event bus (" + e.getMessage()
                    + "), so host maintenance will not drain GPU nodes automatically; "
                    + "run drainAiNode on each GPU node before putting its host into maintenance";
            LOG.warn("AI Services: {}", inactiveReason);
            subscriptionId = null;
            return false;
        }
    }

    public boolean isActive() {
        return subscriptionId != null;
    }

    public String inactiveReason() {
        return inactiveReason;
    }

    @Override
    public void onEvent(Event event) {
        if (event == null
                || !MAINTENANCE_EVENT.equals(event.getEventType())
                || !HOST_CATEGORY.equals(event.getResourceType())) {
            return;
        }

        try {
            Long hostId = resolveHostId.apply(event.getResourceUUID());
            if (hostId == null) {
                LOG.debug("Ignoring maintenance for unknown host {}", event.getResourceUUID());
                return;
            }
            drain.beforeMaintenance(hostId, CallerContext.system());
        } catch (RuntimeException e) {
            // Never throw back at the bus: an exception here can
            // unsubscribe this listener or stall other subscribers, and
            // then nothing drains for the rest of the uptime.
            LOG.error("Failed to drain GPU nodes for host {} before maintenance",
                    event.getResourceUUID(), e);
        }
    }
}
