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
package com.cloud.vm;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.jobs.AsyncJobExecutionContext;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;

import com.cloud.utils.Pair;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.GlobalLock;

//
// TODO: simple load scanner, to minimize code changes required in console proxy manager and SSVM, we still leave most of work at handler
//
public class SystemVmLoadScanner<T> {
    public enum AfterScanAction {
        nop, expand, shrink
    }

    private static final Logger s_logger = Logger.getLogger(SystemVmLoadScanner.class);

    private static final int ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION = 3;   // 3 seconds

    private final SystemVmLoadScanHandler<T> _scanHandler;
    private final ScheduledExecutorService _capacityScanScheduler;
    private final GlobalLock _capacityScanLock;

    public SystemVmLoadScanner(SystemVmLoadScanHandler<T> scanHandler) {
        _scanHandler = scanHandler;
        _capacityScanScheduler = Executors.newScheduledThreadPool(1, new NamedThreadFactory(scanHandler.getScanHandlerName()));
        _capacityScanLock = GlobalLock.getInternLock(scanHandler.getScanHandlerName() + ".scan.lock");
    }

    public void initScan(long startupDelayMs, long scanIntervalMs) {
        _capacityScanScheduler.scheduleAtFixedRate(getCapacityScanTask(), startupDelayMs, scanIntervalMs, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        _capacityScanScheduler.shutdownNow();

        try {
            _capacityScanScheduler.awaitTermination(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            s_logger.debug("[ignored] interupted while stopping systemvm load scanner.");
        }

        _capacityScanLock.releaseRef();
    }

    private Runnable getCapacityScanTask() {
        return new ManagedContextRunnable() {

            @Override
            protected void runInContext() {
                try {
                    CallContext callContext = CallContext.current();
                    assert (callContext != null);

                    AsyncJobExecutionContext.registerPseudoExecutionContext(
                        callContext.getCallingAccountId(), callContext.getCallingUserId());

                    reallyRun();

                    AsyncJobExecutionContext.unregister();
                } catch (Throwable e) {
                    s_logger.warn("Unexpected exception " + e.getMessage(), e);
                }
            }

            private void reallyRun() {
                loadScan();
            }
        };
    }

    private void loadScan() {
        if (!_scanHandler.canScan()) {
            return;
        }

        if (!_capacityScanLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION)) {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Capacity scan lock is used by others, skip and wait for my turn");
            }
            return;
        }

        try {
            _scanHandler.onScanStart();

            T[] pools = _scanHandler.getScannablePools();
            for (T p : pools) {
                if (_scanHandler.isPoolReadyForScan(p)) {
                    Pair<AfterScanAction, Object> actionInfo = _scanHandler.scanPool(p);

                    switch (actionInfo.first()) {
                        case nop:
                            break;

                        case expand:
                            _scanHandler.expandPool(p, actionInfo.second());
                            break;

                        case shrink:
                            _scanHandler.shrinkPool(p, actionInfo.second());
                            break;
                    }
                }
            }

            _scanHandler.onScanEnd();

        } finally {
            _capacityScanLock.unlock();
        }
    }
}
