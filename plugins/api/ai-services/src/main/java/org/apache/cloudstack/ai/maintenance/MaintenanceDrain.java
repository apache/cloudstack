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

import java.util.List;

import org.apache.cloudstack.ai.engine.CallerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Drains GPU nodes off a host before it enters maintenance (SPEC.md section 7).
 *
 * <p>This exists because passthrough GPUs cannot live-migrate (ADR-0003).
 * CloudStack's own maintenance moves VMs off a host; these VMs cannot be
 * moved, so they have to be destroyed and replaced elsewhere first.
 * Without that, maintenance stalls on VMs nothing can migrate.
 *
 * <p>The decision is the engine's, not this class's: whether there is
 * capacity elsewhere and whether a force is needed lives in the engine's
 * {@code lifecycle} module, and is not reimplemented here. All this does
 * is ask, once per node, and report what came back.
 *
 * <p><b>On when this runs.</b> CloudStack 4.23 has no synchronous
 * pre-maintenance extension point: {@code ResourceStateAdapter} covers
 * host create and delete only, the agent {@code Listener} covers connect,
 * disconnect and removal, and {@code ResourceState}'s state machine is the
 * older {@code StateMachine}, which takes no listeners. The only hook is
 * the event bus, which is not enabled in a default install. So
 * {@link AiMaintenanceListener} wires this where it can and says plainly
 * at startup when it cannot.
 */
public class MaintenanceDrain {

    private static final Logger LOG = LogManager.getLogger(MaintenanceDrain.class);

    /** What the engine said about one node. */
    public enum Outcome {
        /** The node is gone and its replicas are elsewhere. */
        DRAINED,
        /** Capacity elsewhere is short; an admin must confirm a forced drain. */
        NEEDS_FORCE,
        /** The drain did not finish inside the configured timeout. */
        TIMED_OUT,
    }

    /** Finds the GPU node VMs running on a host. */
    public interface NodeLookup {
        List<String> nodesOnHost(long hostId);
    }

    /** Asks the engine to drain one node. */
    public interface Drainer {
        Outcome drain(String nodeId, CallerContext caller, int timeoutSeconds);
    }

    /** Tells the operator when maintenance needs a decision from them. */
    public interface Alerts {
        void needsForcedDrain(long hostId, String nodeId, String reason);
    }

    private final NodeLookup lookup;
    private final Drainer drainer;
    private final Alerts alerts;
    private final int timeoutSeconds;

    public MaintenanceDrain(NodeLookup lookup, Drainer drainer, Alerts alerts, int timeoutSeconds) {
        this.lookup = lookup;
        this.drainer = drainer;
        this.alerts = alerts;
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * Drain every GPU node on this host.
     *
     * @return true when maintenance may proceed; false when something needs
     *         an operator's decision first.
     */
    public boolean beforeMaintenance(long hostId, CallerContext caller) {
        List<String> nodes = lookup.nodesOnHost(hostId);
        if (nodes.isEmpty()) {
            return true;
        }

        LOG.info("Draining {} GPU node(s) from host {} before maintenance", nodes.size(), hostId);

        // Every node is attempted even after one fails. Stopping early
        // would leave the rest serving on a host that is going down
        // regardless, and the operator wants one list of problems.
        boolean mayProceed = true;
        for (String nodeId : nodes) {
            Outcome outcome = drainer.drain(nodeId, caller, timeoutSeconds);
            if (outcome == Outcome.DRAINED) {
                continue;
            }

            mayProceed = false;
            String reason = outcome == Outcome.NEEDS_FORCE
                    ? "not enough GPU capacity elsewhere for this node's replicas"
                    : "the drain did not finish within " + timeoutSeconds + " seconds";
            LOG.warn("Host {} cannot enter maintenance yet: {} ({})", hostId, nodeId, reason);
            alerts.needsForcedDrain(hostId, nodeId, reason);
        }
        return mayProceed;
    }
}
