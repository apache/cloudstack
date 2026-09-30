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
package com.cloud.hypervisor.xenserver.resource;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.utils.Pair;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Task;
import com.xensource.xenapi.Types;

/**
 * Tracks the XenAPI tasks a resource is waiting on, per agent request sequence, so a request can be
 * cancelled by cancelling the tasks it created.
 *
 * One instance per resource (one per host). The resource opens a request scope for the executing
 * thread; {@link CitrixResourceBase#waitForTask} then reports every task it waits on through that
 * scope. Registering at that single chokepoint is what gives complete coverage: the storage processor
 * and the command wrappers all wait on their tasks through the resource.
 */
public class XenServerTaskRegistry {
    private static final Logger LOGGER = LogManager.getLogger(XenServerTaskRegistry.class);

    private static final long CANCEL_TASK_WAIT_MS = 30000;
    private static final long CANCEL_TASK_POLL_MS = 500;

    private static final ThreadLocal<RequestScope> CURRENT = new ThreadLocal<>();

    private final Map<Long, RequestState> requests = new ConcurrentHashMap<>();

    private static final class ActiveTask {
        final Task task;
        final Connection connection;

        ActiveTask(final Task task, final Connection connection) {
            this.task = task;
            this.connection = connection;
        }
    }

    private static final class RequestState {
        final Map<String, ActiveTask> tasks = new ConcurrentHashMap<>();
        volatile boolean cancelRequested;
    }

    private static final class RequestScope {
        final XenServerTaskRegistry registry;
        final long sequence;

        RequestScope(final XenServerTaskRegistry registry, final long sequence) {
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
     * Called by the resource when it starts waiting on a task. Returns true when the request this thread
     * is executing has already been cancelled, so the caller can cancel the newly created task straight
     * away instead of letting it run.
     */
    static boolean taskStarted(final Task task, final Connection connection) {
        final RequestScope scope = CURRENT.get();
        if (scope == null || task == null) {
            return false;
        }
        final RequestState state = scope.registry.requests.get(scope.sequence);
        if (state == null) {
            return false;
        }
        state.tasks.put(refOf(task), new ActiveTask(task, connection));
        return state.cancelRequested;
    }

    static void taskFinished(final Task task) {
        final RequestScope scope = CURRENT.get();
        if (scope == null || task == null) {
            return;
        }
        final RequestState state = scope.registry.requests.get(scope.sequence);
        if (state != null) {
            state.tasks.remove(refOf(task));
        }
    }

    /**
     * Whether the request can be stopped. Unknown sequences are not cancellable: we have nothing to stop
     * and no way to know what the backend is doing. A request with no task in flight can be stopped by
     * interrupting its thread; one with tasks in flight only if every one of them is still pending --
     * XenAPI has no cancelable flag, a task that is no longer pending simply cannot be cancelled.
     */
    public boolean isCancellable(final long sequence) {
        final RequestState state = requests.get(sequence);
        if (state == null) {
            return false;
        }
        for (final ActiveTask active : state.tasks.values()) {
            try {
                if (active.task.getStatus(active.connection) != Types.TaskStatusType.PENDING) {
                    LOGGER.debug("XenAPI task {} of request sequence {} is no longer pending, not cancellable", refOf(active.task), sequence);
                    return false;
                }
            } catch (final Exception e) {
                LOGGER.warn("Unable to check the status of XenAPI task {} of request sequence {}", refOf(active.task), sequence, e);
                return false;
            }
        }
        return true;
    }

    /**
     * Cancels every task the request has in flight and marks the request so that any task it creates
     * afterwards is cancelled as soon as the resource starts waiting on it. Returns true only if every
     * in-flight task was actually cancelled.
     */
    public boolean cancel(final long sequence) {
        final RequestState state = requests.get(sequence);
        if (state == null) {
            return false;
        }
        state.cancelRequested = true;

        boolean allCancelled = true;
        for (final ActiveTask active : state.tasks.values()) {
            final Pair<Boolean, String> result = cancelTask(active.task, active.connection);
            if (result.first()) {
                LOGGER.info("Cancelled XenAPI task {} of request sequence {}", refOf(active.task), sequence);
            } else {
                LOGGER.info("Could not cancel XenAPI task {} of request sequence {}: {}", refOf(active.task), sequence, result.second());
                allCancelled = false;
            }
        }
        return allCancelled;
    }

    /**
     * Asks XenServer to cancel the task and waits, bounded, for it to reach a terminal state.
     *
     * Uses the asynchronous cancel: the synchronous form would hold the cancelling thread on a XenServer
     * round trip, and this is called from the agent layer while it holds nothing but the request's
     * bookkeeping. Returns whether the task ended cancelled, with the reason when it did not.
     */
    static Pair<Boolean, String> cancelTask(final Task task, final Connection connection) {
        final String ref = refOf(task);
        try {
            final Types.TaskStatusType status = task.getStatus(connection);
            if (status != Types.TaskStatusType.PENDING && status != Types.TaskStatusType.CANCELLING) {
                return new Pair<>(false, "task " + ref + " is already " + status);
            }
            if (status == Types.TaskStatusType.PENDING) {
                task.cancelAsync(connection);
            }

            final long deadline = System.currentTimeMillis() + CANCEL_TASK_WAIT_MS;
            while (System.currentTimeMillis() < deadline) {
                final Types.TaskStatusType now = task.getStatus(connection);
                if (now == Types.TaskStatusType.CANCELLED) {
                    return new Pair<>(true, "task " + ref + " cancelled");
                }
                if (now == Types.TaskStatusType.SUCCESS) {
                    return new Pair<>(false, "task " + ref + " completed before it could be cancelled");
                }
                if (now == Types.TaskStatusType.FAILURE) {
                    return new Pair<>(false, "task " + ref + " failed before it could be cancelled");
                }
                Thread.sleep(CANCEL_TASK_POLL_MS);
            }
            return new Pair<>(false, "task " + ref + " did not stop within " + CANCEL_TASK_WAIT_MS / 1000 + " seconds of the cancel request");
        } catch (final Types.OperationNotAllowed e) {
            return new Pair<>(false, "XenServer does not allow cancelling task " + ref);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Pair<>(false, "interrupted while waiting for task " + ref + " to cancel");
        } catch (final Exception e) {
            LOGGER.warn("Failed to cancel XenAPI task {}", ref, e);
            return new Pair<>(false, "failed to cancel task " + ref + ": " + e.getMessage());
        }
    }

    // A task's opaque ref is the natural key, but a task that has not been round-tripped to the
    // server (or a test double) has none; fall back to identity so the map never sees a null key.
    private static String refOf(final Task task) {
        final String ref = task.toWireString();
        return ref != null ? ref : "task@" + Integer.toHexString(System.identityHashCode(task));
    }

    int activeTaskCount(final long sequence) {
        final RequestState state = requests.get(sequence);
        return state == null ? 0 : state.tasks.size();
    }
}
