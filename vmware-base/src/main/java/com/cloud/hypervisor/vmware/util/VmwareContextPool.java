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

import org.apache.commons.lang3.StringUtils;
import org.apache.cloudstack.managed.context.ManagedContextTimerTask;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.joda.time.Duration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

public class VmwareContextPool {
    protected Logger logger = LogManager.getLogger(getClass());

    private static final Duration DEFAULT_CHECK_INTERVAL = Duration.millis(10000L);
    private static final int DEFAULT_IDLE_QUEUE_LENGTH = 128;

    private final ConcurrentMap<String, Queue<VmwareContext>> _pool;
    private int _maxIdleQueueLength = DEFAULT_IDLE_QUEUE_LENGTH;
    private Duration _idleCheckInterval = DEFAULT_CHECK_INTERVAL;

    private Timer _timer = new Timer();

    public VmwareContextPool() {
        this(DEFAULT_IDLE_QUEUE_LENGTH, DEFAULT_CHECK_INTERVAL);
    }

    public VmwareContextPool(int maxIdleQueueLength, Duration idleCheckInterval) {
        _pool = new ConcurrentHashMap<String, Queue<VmwareContext>>();

        _maxIdleQueueLength = maxIdleQueueLength;
        _idleCheckInterval = idleCheckInterval;

        _timer.scheduleAtFixedRate(getTimerTask(), _idleCheckInterval.getMillis(), _idleCheckInterval.getMillis());
    }

    public VmwareContext getContext(final String vCenterAddress, final String vCenterUserName) {
        final String poolKey = composePoolKey(vCenterAddress, vCenterUserName).intern();
        if (StringUtils.isEmpty(poolKey)) {
            return null;
        }
        synchronized (poolKey) {
            final Queue<VmwareContext> ctxList = _pool.get(poolKey);
            if (ctxList != null && !ctxList.isEmpty()) {
                final VmwareContext context = ctxList.remove();
                if (context != null) {
                    context.setPoolInfo(this, poolKey);
                }
                if (logger.isTraceEnabled()) {
                    logger.trace("Return a VmwareContext from the idle pool: " + poolKey + ". current pool size: " + ctxList.size() + ", outstanding count: " +
                            VmwareContext.getOutstandingContextCount());
                }
                return context;
            }
            return null;
        }
    }

    public void registerContext(final VmwareContext context) {
        assert (context.getPool() == this);
        assert (context.getPoolKey() != null);

        final String poolKey = context.getPoolKey().intern();
        synchronized (poolKey) {
            Queue<VmwareContext> ctxQueue = _pool.get(poolKey);

            if (ctxQueue == null) {
                ctxQueue = new ConcurrentLinkedQueue<>();
                _pool.put(poolKey, ctxQueue);
            }

            if (ctxQueue.size() >= _maxIdleQueueLength) {
                final VmwareContext oldestContext = ctxQueue.remove();
                if (oldestContext != null) {
                    try {
                        oldestContext.close();
                    } catch (Throwable t) {
                        logger.error("Unexpected exception caught while trying to purge oldest VmwareContext", t);
                    }
                }
            }
            context.clearStockObjects();
            ctxQueue.add(context);

            if (logger.isTraceEnabled()) {
                logger.trace("Recycle VmwareContext into idle pool: " + context.getPoolKey() + ", current idle pool size: " + ctxQueue.size() + ", outstanding count: "
                        + VmwareContext.getOutstandingContextCount());
            }
        }
    }

    public void unregisterContext(final VmwareContext context) {
        assert (context != null);
        final String poolKey = context.getPoolKey().intern();
        final Queue<VmwareContext> ctxList = _pool.get(poolKey);
        synchronized (poolKey) {
            if (StringUtils.isNotEmpty(poolKey) && ctxList != null && ctxList.contains(context)) {
                ctxList.remove(context);
            }
        }
    }

    private TimerTask getTimerTask() {
        return new ManagedContextTimerTask() {
            @Override
            protected void runInContext() {
                try {
                    doKeepAlive();
                } catch (Throwable e) {
                    logger.error("Unexpected exception", e);
                }
            }
        };
    }

    private void doKeepAlive() {
        final List<VmwareContext> closableCtxList = new ArrayList<>();
        for (final Queue<VmwareContext> ctxQueue : _pool.values()) {
            for (Iterator<VmwareContext> iterator = ctxQueue.iterator(); iterator.hasNext();) {
                final VmwareContext context = iterator.next();
                if (context == null) {
                    iterator.remove();
                    continue;
                }
                try {
                    context.idleCheck();
                } catch (Throwable e) {
                    logger.warn("Exception caught during VmwareContext idle check, close and discard the context", e);
                    closableCtxList.add(context);
                    iterator.remove();
                }
            }
        }
        for (final VmwareContext context : closableCtxList) {
            context.close();
        }
    }

    public static String composePoolKey(final String vCenterAddress, final String vCenterUserName) {
        assert (vCenterUserName != null);
        assert (vCenterAddress != null);
        return vCenterUserName + "@" + vCenterAddress;
    }
}
