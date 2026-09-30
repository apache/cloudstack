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

/** Per agent request sequence, the XenAPI tasks a resource is waiting on; one instance per host. waitForTask is the single registration point. */
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

    /** A null sequence (not from the agent layer) leaves the thread unscoped. */
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

    /** Returns true when the request was already cancelled, so the caller cancels the new task at once. */
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

    /** Unknown sequences are not cancellable; XenAPI has no cancelable flag, so tasks must still be pending. */
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

    /** Cancels the in-flight tasks and marks the request so later tasks are cancelled on arrival. */
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

    /** cancelAsync, then poll to a terminal state: the synchronous cancel would hold the caller on a XenServer round trip. */
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

    // no opaque ref before a round trip (or on a test double): fall back to identity
    private static String refOf(final Task task) {
        final String ref = task.toWireString();
        return ref != null ? ref : "task@" + Integer.toHexString(System.identityHashCode(task));
    }

    int activeTaskCount(final long sequence) {
        final RequestState state = requests.get(sequence);
        return state == null ? 0 : state.tasks.size();
    }
}
