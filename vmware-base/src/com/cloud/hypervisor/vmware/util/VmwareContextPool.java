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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import org.apache.cloudstack.managed.context.ManagedContextTimerTask;

public class VmwareContextPool {
    private static final Logger s_logger = Logger.getLogger(VmwareContextPool.class);

    private static final long DEFAULT_CHECK_INTERVAL = 10000;
    private static final int DEFAULT_IDLE_QUEUE_LENGTH = 128;

    private List<VmwareContext> _outstandingRegistry = new ArrayList<VmwareContext>();

    private Map<String, List<VmwareContext>> _pool;
    private int _maxIdleQueueLength = DEFAULT_IDLE_QUEUE_LENGTH;
    private long _idleCheckIntervalMs = DEFAULT_CHECK_INTERVAL;

    private Timer _timer = new Timer();

    public VmwareContextPool() {
        this(DEFAULT_IDLE_QUEUE_LENGTH, DEFAULT_CHECK_INTERVAL);
    }

    public VmwareContextPool(int maxIdleQueueLength) {
        this(maxIdleQueueLength, DEFAULT_CHECK_INTERVAL);
    }

    public VmwareContextPool(int maxIdleQueueLength, long idleCheckIntervalMs) {
        _pool = new HashMap<String, List<VmwareContext>>();

        _maxIdleQueueLength = maxIdleQueueLength;
        _idleCheckIntervalMs = idleCheckIntervalMs;

        _timer.scheduleAtFixedRate(getTimerTask(), _idleCheckIntervalMs, _idleCheckIntervalMs);
    }

    public void registerOutstandingContext(VmwareContext context) {
        assert (context != null);
        synchronized (this) {
            _outstandingRegistry.add(context);
        }
    }

    public void unregisterOutstandingContext(VmwareContext context) {
        assert (context != null);
        synchronized (this) {
            _outstandingRegistry.remove(context);
        }
    }

    public VmwareContext getContext(String vCenterAddress, String vCenterUserName) {
        String poolKey = composePoolKey(vCenterAddress, vCenterUserName);
        synchronized (this) {
            List<VmwareContext> l = _pool.get(poolKey);
            if (l == null)
                return null;

            if (l.size() > 0) {
                VmwareContext context = l.remove(0);
                context.setPoolInfo(this, poolKey);

                if (s_logger.isTraceEnabled())
                    s_logger.trace("Return a VmwareContext from the idle pool: " + poolKey + ". current pool size: " + l.size() + ", outstanding count: " +
                        VmwareContext.getOutstandingContextCount());
                return context;
            }

            // TODO, we need to control the maximum number of outstanding VmwareContext object in the future
            return null;
        }
    }

    public void returnContext(VmwareContext context) {
        assert (context.getPool() == this);
        assert (context.getPoolKey() != null);
        synchronized (this) {
            List<VmwareContext> l = _pool.get(context.getPoolKey());
            if (l == null) {
                l = new ArrayList<VmwareContext>();
                _pool.put(context.getPoolKey(), l);
            }

            if (l.size() < _maxIdleQueueLength) {
                context.clearStockObjects();
                l.add(context);

                if (s_logger.isTraceEnabled())
                    s_logger.trace("Recycle VmwareContext into idle pool: " + context.getPoolKey() + ", current idle pool size: " + l.size() + ", outstanding count: " +
                        VmwareContext.getOutstandingContextCount());
            } else {
                if (s_logger.isTraceEnabled())
                    s_logger.trace("VmwareContextPool queue exceeds limits, queue size: " + l.size());
                context.close();
            }
        }
    }

    private TimerTask getTimerTask() {
        return new ManagedContextTimerTask() {
            @Override
            protected void runInContext() {
                try {
                    // doIdleCheck();

                    doKeepAlive();
                } catch (Throwable e) {
                    s_logger.error("Unexpected exception", e);
                }
            }
        };
    }

    private void getIdleCheckContexts(List<VmwareContext> l, int batchSize) {
        synchronized (this) {
            for (Map.Entry<String, List<VmwareContext>> entry : _pool.entrySet()) {
                if (entry.getValue() != null) {
                    int count = 0;
                    while (entry.getValue().size() > 0 && count < batchSize) {
                        VmwareContext context = entry.getValue().remove(0);
                        context.setPoolInfo(this, entry.getKey());
                        l.add(context);
                        count++;
                    }
                }
            }
        }
    }

    private void doIdleCheck() {
        List<VmwareContext> l = new ArrayList<VmwareContext>();
        int batchSize = (int)(_idleCheckIntervalMs / 1000);    // calculate batch size at 1 request/sec rate
        getIdleCheckContexts(l, batchSize);

        for (VmwareContext context : l) {
            try {
                context.idleCheck();

                if (s_logger.isTraceEnabled())
                    s_logger.trace("Recyle context after idle check");
                returnContext(context);
            } catch (Throwable e) {
                s_logger.warn("Exception caught during VmwareContext idle check, close and discard the context", e);
                context.close();
            }
        }
    }

    private void getKeepAliveCheckContexts(List<VmwareContext> l, int batchSize) {
        synchronized (this) {
            int size = Math.min(_outstandingRegistry.size(), batchSize);
            while (size > 0) {
                VmwareContext context = _outstandingRegistry.remove(0);
                l.add(context);

                _outstandingRegistry.add(context);
                size--;
            }
        }
    }

    private void doKeepAlive() {
        List<VmwareContext> l = new ArrayList<VmwareContext>();
        int batchSize = (int)(_idleCheckIntervalMs / 1000);    // calculate batch size at 1 request/sec rate
        getKeepAliveCheckContexts(l, batchSize);

        for (VmwareContext context : l) {
            try {
                context.idleCheck();
            } catch (Throwable e) {
                s_logger.warn("Exception caught during VmwareContext idle check, close and discard the context", e);
                context.close();
            }
        }
    }

    public static String composePoolKey(String vCenterAddress, String vCenterUserName) {
        assert (vCenterUserName != null);
        assert (vCenterAddress != null);
        return vCenterUserName + "@" + vCenterAddress;
    }
}
