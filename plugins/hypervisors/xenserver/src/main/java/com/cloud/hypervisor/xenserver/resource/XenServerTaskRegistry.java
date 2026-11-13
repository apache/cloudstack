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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

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

    /**
     * Power operations cannot be taken back once issued: XAPI lets the task be cancelled, but the guest has already been told to shut
     * down or reboot, and a hard_shutdown cancelled half way leaves the VM paused. The resource would then fall back to the hard
     * variant, which is the opposite of what a cancel means.
     */
    static final Set<String> UNCANCELLABLE_TASKS = new HashSet<>(Arrays.asList(
            "Async.VM.clean_shutdown", "Async.VM.hard_shutdown", "Async.VM.clean_reboot", "Async.VM.hard_reboot",
            "Async.VM.start", "Async.VM.start_on", "Async.VM.suspend", "Async.VM.resume", "Async.VM.resume_on", "Async.VM.pause", "Async.VM.unpause"));

    private static final ThreadLocal<RequestScope> CURRENT = new ThreadLocal<>();

    private final Map<Long, RequestState> requests = new ConcurrentHashMap<>();

    private static final class ActiveTask {
        final Task task;
        final Connection connection;
        final String name;

        ActiveTask(final Task task, final Connection connection, final String name) {
            this.task = task;
            this.connection = connection;
            this.name = name;
        }
    }

    private static final class RequestState {
        final String command;
        final Map<String, ActiveTask> tasks = new ConcurrentHashMap<>();
        // the status the waiting thread saw last, kept because it destroys the task object right after and the canceller may still be polling it
        final Map<String, Types.TaskStatusType> finished = new ConcurrentHashMap<>();
        volatile boolean cancelling;
        volatile boolean cancelRequested;

        RequestState(final String command) {
            this.command = command;
        }
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

    /** Returns true when the request is being cancelled right now, so the caller cancels the new task at once; a task started after the cancel is the request's undo and runs. */
    static boolean taskStarted(final Task task, final Connection connection) {
        return taskStarted(task, connection, null);
    }

    static boolean taskStarted(final Task task, final Connection connection, final String name) {
        final RequestScope scope = CURRENT.get();
        if (scope == null || task == null) {
            return false;
        }
        final RequestState state = scope.registry.requests.get(scope.sequence);
        if (state == null) {
            return false;
        }
        if (state.cancelling) {
            return true;
        }
        state.tasks.put(refOf(task), new ActiveTask(task, connection, name));
        return false;
    }

    static void taskFinished(final Task task) {
        taskFinished(task, null);
    }

    static void taskFinished(final Task task, final Types.TaskStatusType finalStatus) {
        final RequestScope scope = CURRENT.get();
        if (scope == null || task == null) {
            return;
        }
        final RequestState state = scope.registry.requests.get(scope.sequence);
        if (state != null) {
            final String ref = refOf(task);
            if (finalStatus != null) {
                state.finished.put(ref, finalStatus);
            }
            state.tasks.remove(ref);
        }
    }

    /** Null when every XenAPI task in flight is still pending (XenAPI has no cancelable flag). A request with none in flight is refused: a synchronous call already made cannot be undone by interrupting the thread. */
    public String checkTaskNotCancellable(final long sequence) {
        final RequestState state = requests.get(sequence);
        if (state == null) {
            return "request sequence " + sequence + " is not running on this host";
        }
        if (state.tasks.isEmpty()) {
            return "no XenAPI task is running for " + state.command;
        }
        for (final ActiveTask active : state.tasks.values()) {
            final String refusal = refusalForUncancellable(active);
            if (refusal != null) {
                return refusal;
            }
            try {
                final Types.TaskStatusType status = active.task.getStatus(active.connection);
                if (status != Types.TaskStatusType.PENDING) {
                    LOGGER.debug("XenAPI task {} of request sequence {} is already {}, not cancellable", refOf(active.task), sequence, status);
                    return "XenAPI task " + refOf(active.task) + " is already " + status;
                }
            } catch (final Exception e) {
                final Types.TaskStatusType finished = state.finished.get(refOf(active.task));
                if (finished != null) {
                    return "XenAPI task " + refOf(active.task) + " is already " + finished;
                }
                LOGGER.info("Unable to check the status of XenAPI task {} of request sequence {}: {}", refOf(active.task), sequence, e.getMessage());
                return "unable to check XenAPI task " + refOf(active.task) + ": " + e.getMessage();
            }
        }
        return null;
    }

    private static String refusalForUncancellable(final ActiveTask active) {
        if (active.name != null && UNCANCELLABLE_TASKS.contains(active.name)) {
            return "XenAPI task " + active.name + " cannot be stopped once issued: the guest has already been told";
        }
        return null;
    }

    /** Cancels the tasks in flight. One that starts meanwhile is cancelled on arrival; what the request starts afterwards is its undo and runs. */
    public boolean cancel(final long sequence) {
        final RequestState state = requests.get(sequence);
        if (state == null) {
            return false;
        }
        if (state.tasks.isEmpty()) {
            LOGGER.debug("Request sequence {} ({}) has no XenAPI task to cancel", sequence, state.command);
            return false;
        }
        for (final ActiveTask active : state.tasks.values()) {
            final String refusal = refusalForUncancellable(active);
            if (refusal != null) {
                LOGGER.info("Not cancelling request sequence {} ({}): {}", sequence, state.command, refusal);
                return false;
            }
        }
        state.cancelling = true;
        boolean allCancelled = true;
        boolean anyCancelled = false;
        try {
            for (final ActiveTask active : state.tasks.values()) {
                final Pair<Boolean, String> result = cancelTask(active.task, active.connection, state.finished::get);
                if (result.first()) {
                    anyCancelled = true;
                    LOGGER.info("Cancelled XenAPI task {} of request sequence {}", refOf(active.task), sequence);
                } else {
                    LOGGER.info("Could not cancel XenAPI task {} of request sequence {}: {}", refOf(active.task), sequence, result.second());
                    allCancelled = false;
                }
            }
        } finally {
            if (anyCancelled) {
                state.cancelRequested = true;
            }
            state.cancelling = false;
        }
        return allCancelled;
    }

    /** cancelAsync, then poll to a terminal state: the synchronous cancel would hold the caller on a XenServer round trip. */
    static Pair<Boolean, String> cancelTask(final Task task, final Connection connection) {
        return cancelTask(task, connection, ref -> null);
    }

    /**
     * The thread waiting on the task destroys the task object as soon as it sees it finish, so a status poll here can meet an invalid
     * handle. The final status that thread recorded then says whether the task ended CANCELLED or completed first.
     */
    static Pair<Boolean, String> cancelTask(final Task task, final Connection connection, final Function<String, Types.TaskStatusType> finishedStatus) {
        final String ref = refOf(task);
        boolean cancelIssued = false;
        try {
            final Types.TaskStatusType status = task.getStatus(connection);
            if (status != Types.TaskStatusType.PENDING && status != Types.TaskStatusType.CANCELLING) {
                return new Pair<>(false, "task " + ref + " is already " + status);
            }
            if (status == Types.TaskStatusType.PENDING) {
                task.cancelAsync(connection);
            }
            cancelIssued = true;

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
            final Types.TaskStatusType finished = finishedStatus.apply(ref);
            if (finished != null) {
                LOGGER.debug("XenAPI task {} is gone, it ended {}", ref, finished);
                return finished == Types.TaskStatusType.CANCELLED && cancelIssued
                        ? new Pair<>(true, "task " + ref + " cancelled")
                        : new Pair<>(false, "task " + ref + " is already " + finished);
            }
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
