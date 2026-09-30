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
package com.cloud.hypervisor.kvm.resource;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.agent.api.Command;
import com.cloud.hypervisor.kvm.resource.disconnecthook.DisconnectHook;

/**
 * Tracks, per agent request sequence, the backend work the resource could stop for it.
 *
 * The command wrappers already register a {@link DisconnectHook} for every long-running libvirt job
 * they start, so that an agent disconnect aborts it. Those hooks are exactly what a cancellation needs
 * to run; this keeps them keyed by the request that created them. A request with no hook has nothing
 * that can be stopped -- the agent cannot interrupt a blocking libvirt call or a child process from
 * outside -- and is reported as not cancellable rather than cancelled in name only.
 */
public class KvmCancellableRequests {
    private static final Logger LOGGER = LogManager.getLogger(KvmCancellableRequests.class);

    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

    private final Map<Long, RequestState> requests = new ConcurrentHashMap<>();

    private static final class RequestState {
        final Command command;
        final List<DisconnectHook> hooks = new CopyOnWriteArrayList<>();
        volatile boolean cancelRequested;

        RequestState(final Command command) {
            this.command = command;
        }
    }

    /**
     * Marks the current thread as executing the given request. A sequence of zero (a command that did
     * not arrive through the agent layer) leaves the thread unscoped, so nothing is tracked for it.
     */
    public void begin(final long sequence, final Command command) {
        if (sequence <= 0) {
            return;
        }
        requests.putIfAbsent(sequence, new RequestState(command));
        CURRENT.set(sequence);
    }

    public void end(final long sequence) {
        CURRENT.remove();
        if (sequence > 0) {
            requests.remove(sequence);
        }
    }

    public boolean wasCancelRequested(final long sequence) {
        final RequestState state = requests.get(sequence);
        return state != null && state.cancelRequested;
    }

    /** Associates a hook with the request the current thread is executing, if any. */
    public void attach(final DisconnectHook hook) {
        final Long sequence = CURRENT.get();
        if (sequence == null || hook == null) {
            return;
        }
        final RequestState state = requests.get(sequence);
        if (state != null) {
            state.hooks.add(hook);
        }
    }

    public void detach(final DisconnectHook hook) {
        if (hook == null) {
            return;
        }
        final Long sequence = CURRENT.get();
        final RequestState scoped = sequence != null ? requests.get(sequence) : null;
        if (scoped != null && scoped.hooks.remove(hook)) {
            return;
        }
        for (final RequestState state : requests.values()) {
            state.hooks.remove(hook);
        }
    }

    /** A request can be stopped only while it has a hook that knows how to stop it. */
    public boolean isCancellable(final long sequence) {
        final RequestState state = requests.get(sequence);
        return state != null && !state.hooks.isEmpty();
    }

    /**
     * Runs the request's hooks, each once and bounded by its own timeout, and hands every hook that ran
     * to the caller so it can be dropped from the disconnect list -- a Thread runs only once, and the
     * disconnect path would otherwise try to start it again. Returns true only if the request was known,
     * had something to stop, and every hook ran to completion.
     */
    public boolean cancel(final long sequence, final Consumer<DisconnectHook> onHookRan) {
        final RequestState state = requests.get(sequence);
        if (state == null) {
            return false;
        }
        state.cancelRequested = true;
        if (state.hooks.isEmpty()) {
            LOGGER.debug("Request sequence {} ({}) has nothing that can be stopped", sequence, describe(state.command));
            return false;
        }

        boolean allRan = true;
        for (final DisconnectHook hook : state.hooks) {
            try {
                if (hook.getState() == Thread.State.NEW) {
                    hook.start();
                }
                hook.join(hook.getTimeoutMs());
                if (hook.isAlive()) {
                    LOGGER.warn("Cancel hook {} for request sequence {} did not finish within {} ms", hook.getName(), sequence, hook.getTimeoutMs());
                    allRan = false;
                } else {
                    LOGGER.info("Ran cancel hook {} for request sequence {} ({})", hook.getName(), sequence, describe(state.command));
                }
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                allRan = false;
            } catch (final Exception e) {
                LOGGER.warn("Cancel hook {} for request sequence {} failed", hook.getName(), sequence, e);
                allRan = false;
            } finally {
                onHookRan.accept(hook);
            }
        }
        return allRan;
    }

    int hookCount(final long sequence) {
        final RequestState state = requests.get(sequence);
        return state == null ? 0 : state.hooks.size();
    }

    private static String describe(final Command command) {
        return command == null ? "unknown command" : command.getClass().getSimpleName();
    }
}
