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
package com.cloud.hypervisor.vmware.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.utils.Pair;
import com.vmware.vim25.ManagedObjectReference;

/** Per agent request sequence, the vCenter tasks a resource is waiting on; one instance per host. VmwareClient.waitForTask is the single registration point. */
public class VmwareTaskRegistry {
    private static final Logger LOGGER = LogManager.getLogger(VmwareTaskRegistry.class);

    private static final ThreadLocal<RequestScope> CURRENT = new ThreadLocal<>();

    private final Map<Long, RequestState> requests = new ConcurrentHashMap<>();

    private static final class ActiveTask {
        final ManagedObjectReference mor;
        final VmwareClient client;

        ActiveTask(final ManagedObjectReference mor, final VmwareClient client) {
            this.mor = mor;
            this.client = client;
        }
    }

    private static final class RequestState {
        final String command;
        final Map<String, ActiveTask> tasks = new ConcurrentHashMap<>();
        volatile boolean cancelRequested;

        RequestState(final String command) {
            this.command = command;
        }
    }

    private static final class RequestScope {
        final VmwareTaskRegistry registry;
        final long sequence;

        RequestScope(final VmwareTaskRegistry registry, final long sequence) {
            this.registry = registry;
            this.sequence = sequence;
        }
    }

    /** A null sequence (not from the agent layer) leaves the thread unscoped. */
    public void beginRequest(final Long sequence, final String command) {
        if (sequence == null) {
            return;
        }
        requests.putIfAbsent(sequence, new RequestState(command));
        CURRENT.set(new RequestScope(this, sequence));
    }

    public void endRequest(final Long sequence) {
        CURRENT.remove();
        if (sequence != null) {
            requests.remove(sequence);
        }
    }

    public boolean wasCancelRequested(final Long sequence) {
        if (sequence == null) {
            return false;
        }
        final RequestState state = requests.get(sequence);
        return state != null && state.cancelRequested;
    }

    /** Returns true when the request was already cancelled, so the caller cancels the new task at once. */
    static boolean taskStarted(final ManagedObjectReference mor, final VmwareClient client) {
        final RequestScope scope = CURRENT.get();
        if (scope == null || mor == null) {
            return false;
        }
        final RequestState state = scope.registry.requests.get(scope.sequence);
        if (state == null) {
            return false;
        }
        state.tasks.put(mor.getValue(), new ActiveTask(mor, client));
        return state.cancelRequested;
    }

    static void taskFinished(final ManagedObjectReference mor) {
        final RequestScope scope = CURRENT.get();
        if (scope == null || mor == null) {
            return;
        }
        final RequestState state = scope.registry.requests.get(scope.sequence);
        if (state != null) {
            state.tasks.remove(mor.getValue());
        }
    }

    /** Null when every vCenter task in flight is cancelable. A request with none in flight is refused: what vCenter was already asked to do (a guest shutdown, say) cannot be undone by interrupting the thread. */
    public String checkTaskNotCancellable(final long sequence) {
        final RequestState state = requests.get(sequence);
        if (state == null) {
            return "request sequence " + sequence + " is not running on this host";
        }
        if (state.tasks.isEmpty()) {
            return "no vCenter task is running for " + state.command;
        }
        for (final ActiveTask task : state.tasks.values()) {
            try {
                final String reason = task.client.checkTaskIsNotCancellable(task.mor);
                if (reason != null) {
                    LOGGER.debug("Request sequence {} ({}) is not cancellable: {}", sequence, state.command, reason);
                    return reason;
                }
            } catch (final Exception e) {
                LOGGER.warn("Unable to check whether vCenter task {} of request sequence {} is cancellable", task.mor.getValue(), sequence, e);
                return "unable to check vCenter task " + task.mor.getValue() + ": " + e.getMessage();
            }
        }
        return null;
    }

    /** Cancels the in-flight tasks and marks the request so later tasks are cancelled on arrival. */
    public boolean cancel(final long sequence) {
        final RequestState state = requests.get(sequence);
        if (state == null) {
            return false;
        }
        if (state.tasks.isEmpty()) {
            LOGGER.debug("Request sequence {} ({}) has no vCenter task to cancel", sequence, state.command);
            return false;
        }
        state.cancelRequested = true;

        boolean allCancelled = true;
        for (final ActiveTask task : state.tasks.values()) {
            try {
                final Pair<Boolean, String> result = task.client.cancelTask(task.mor);
                if (result.first()) {
                    LOGGER.info("Cancelled vCenter task {} of request sequence {}", task.mor.getValue(), sequence);
                } else {
                    LOGGER.info("Could not cancel vCenter task {} of request sequence {}: {}", task.mor.getValue(), sequence, result.second());
                    allCancelled = false;
                }
            } catch (final Exception e) {
                LOGGER.warn("Failed to cancel vCenter task {} of request sequence {}", task.mor.getValue(), sequence, e);
                allCancelled = false;
            }
        }
        return allCancelled;
    }

    int activeTaskCount(final long sequence) {
        final RequestState state = requests.get(sequence);
        return state == null ? 0 : state.tasks.size();
    }
}
