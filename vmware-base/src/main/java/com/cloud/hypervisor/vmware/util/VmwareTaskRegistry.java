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

/**
 * Tracks the vCenter tasks a resource is waiting on, per agent request sequence, so a request can be
 * cancelled by cancelling the tasks it created.
 *
 * One instance per resource (one per host), so two hosts cannot collide on a sequence number. The
 * resource opens a request scope for the executing thread; {@link VmwareClient#waitForTask} then reports
 * every task it waits on through that scope. Registering at that single chokepoint is what gives complete
 * coverage: every task the resource ever waits on passes through it.
 */
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
        final Map<String, ActiveTask> tasks = new ConcurrentHashMap<>();
        volatile boolean cancelRequested;
    }

    private static final class RequestScope {
        final VmwareTaskRegistry registry;
        final long sequence;

        RequestScope(final VmwareTaskRegistry registry, final long sequence) {
            this.registry = registry;
            this.sequence = sequence;
        }
    }

    /**
     * Marks the current thread as executing the given request. A null sequence (a command that did not
     * arrive through the agent layer) leaves the thread unscoped, so nothing is tracked for it.
     */
    public void beginRequest(final Long sequence) {
        if (sequence == null) {
            return;
        }
        requests.putIfAbsent(sequence, new RequestState());
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

    /**
     * Called by the client when it starts waiting on a task. Returns true when the request this thread is
     * executing has already been cancelled, so the caller can cancel the newly created task straight away
     * instead of letting it run.
     */
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

    /**
     * Whether the request can be stopped. Unknown sequences are not cancellable: we have nothing to stop
     * and no way to know what the backend is doing. A request with no task in flight can be stopped by
     * interrupting its thread; one with tasks in flight only if vCenter will cancel every one of them.
     */
    public boolean isCancellable(final long sequence) {
        final RequestState state = requests.get(sequence);
        if (state == null) {
            return false;
        }
        for (final ActiveTask task : state.tasks.values()) {
            try {
                if (!task.client.isTaskCancellable(task.mor)) {
                    LOGGER.debug("vCenter task {} of request sequence {} is not cancellable", task.mor.getValue(), sequence);
                    return false;
                }
            } catch (final Exception e) {
                LOGGER.warn("Unable to check whether vCenter task {} of request sequence {} is cancellable", task.mor.getValue(), sequence, e);
                return false;
            }
        }
        return true;
    }

    /**
     * Cancels every task the request has in flight and marks the request so that any task it creates
     * afterwards is cancelled as soon as the client starts waiting on it. Returns true only if every
     * in-flight task was actually cancelled.
     */
    public boolean cancel(final long sequence) {
        final RequestState state = requests.get(sequence);
        if (state == null) {
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
