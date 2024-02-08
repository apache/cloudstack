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
package org.apache.cloudstack.framework.jobs.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.naming.ConfigurationException;


import org.apache.cloudstack.framework.jobs.AsyncJob;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.framework.messagebus.MessageDispatcher;
import org.apache.cloudstack.framework.messagebus.MessageHandler;
import org.apache.cloudstack.managed.context.ManagedContextTimerTask;

import com.cloud.utils.component.ManagerBase;

public class AsyncJobMonitor extends ManagerBase {

    @Inject private MessageBus _messageBus;

    private final Map<Long, ActiveTaskRecord> _activeTasks = new HashMap<Long, ActiveTaskRecord>();
    private final Timer _timer = new Timer();

    private final AtomicInteger _activePoolThreads = new AtomicInteger();
    private final AtomicInteger _activeInplaceThreads = new AtomicInteger();

    // configuration
    private long _inactivityCheckIntervalMs = 60000;
    private long _inactivityWarningThresholdMs = 90000;

    public AsyncJobMonitor() {
    }

    public long getInactivityCheckIntervalMs() {
        return _inactivityCheckIntervalMs;
    }

    public void setInactivityCheckIntervalMs(long intervalMs) {
        _inactivityCheckIntervalMs = intervalMs;
    }

    public long getInactivityWarningThresholdMs() {
        return _inactivityWarningThresholdMs;
    }

    public void setInactivityWarningThresholdMs(long thresholdMs) {
        _inactivityWarningThresholdMs = thresholdMs;
    }

    @MessageHandler(topic = AsyncJob.Topics.JOB_HEARTBEAT)
    public void onJobHeartbeatNotify(String subject, String senderAddress, Object args) {
        if (args != null && args instanceof Long) {
            synchronized (this) {
                ActiveTaskRecord record = _activeTasks.get(args);
                if (record != null) {
                    record.updateJobHeartbeatTick();
                }
            }
        }
    }

    private void heartbeat() {
        synchronized (this) {
            for (Map.Entry<Long, ActiveTaskRecord> entry : _activeTasks.entrySet()) {
                if (entry.getValue().millisSinceLastJobHeartbeat() > _inactivityWarningThresholdMs) {
                    logger.warn("Task (job-" + entry.getValue().getJobId() + ") has been pending for "
                            + entry.getValue().millisSinceLastJobHeartbeat() / 1000 + " seconds");
                }
            }
        }
    }

    @Override
    public boolean configure(String name, Map<String, Object> params)
            throws ConfigurationException {

        _messageBus.subscribe(AsyncJob.Topics.JOB_HEARTBEAT, MessageDispatcher.getDispatcher(this));
        _timer.scheduleAtFixedRate(new ManagedContextTimerTask() {
            @Override
            protected void runInContext() {
                heartbeat();
            }

        }, _inactivityCheckIntervalMs, _inactivityCheckIntervalMs);
        return true;
    }

    public void registerActiveTask(long runNumber, long jobId) {
        synchronized (this) {
            logger.info("Add job-" + jobId + " into job monitoring");

            assert (_activeTasks.get(runNumber) == null);

            long threadId = Thread.currentThread().getId();
            boolean fromPoolThread = Thread.currentThread().getName().contains(AsyncJobManager.API_JOB_POOL_THREAD_PREFIX);
            ActiveTaskRecord record = new ActiveTaskRecord(jobId, threadId, fromPoolThread);
            _activeTasks.put(runNumber, record);
            if (fromPoolThread)
                _activePoolThreads.incrementAndGet();
            else
                _activeInplaceThreads.incrementAndGet();
        }
    }

    public void unregisterActiveTask(long runNumber) {
        synchronized (this) {
            ActiveTaskRecord record = _activeTasks.get(runNumber);
            assert (record != null);
            if (record != null) {
                logger.info("Remove job-" + record.getJobId() + " from job monitoring");

                if (record.isPoolThread())
                    _activePoolThreads.decrementAndGet();
                else
                    _activeInplaceThreads.decrementAndGet();

                _activeTasks.remove(runNumber);
            }
        }
    }

    public void unregisterByJobId(long jobId) {
        synchronized (this) {
            Iterator<Map.Entry<Long, ActiveTaskRecord>> it = _activeTasks.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Long, ActiveTaskRecord> entry = it.next();
                if (entry.getValue().getJobId() == jobId) {
                    logger.info("Remove Job-" + entry.getValue().getJobId() + " from job monitoring due to job cancelling");

                    if (entry.getValue().isPoolThread())
                        _activePoolThreads.decrementAndGet();
                    else
                        _activeInplaceThreads.decrementAndGet();

                    it.remove();
                }
            }
        }
    }

    public int getActivePoolThreads() {
        return _activePoolThreads.get();
    }

    public int getActiveInplaceThread() {
        return _activeInplaceThreads.get();
    }

    private static class ActiveTaskRecord {
        long _jobId;
        long _threadId;
        boolean _fromPoolThread;
        long _jobLastHeartbeatTick;

        public ActiveTaskRecord(long jobId, long threadId, boolean fromPoolThread) {
            _threadId = threadId;
            _jobId = jobId;
            _fromPoolThread = fromPoolThread;
            _jobLastHeartbeatTick = System.currentTimeMillis();
        }

        public long getThreadId() {
            return _threadId;
        }

        public long getJobId() {
            return _jobId;
        }

        public boolean isPoolThread() {
            return _fromPoolThread;
        }

        public void updateJobHeartbeatTick() {
            _jobLastHeartbeatTick = System.currentTimeMillis();
        }

        public long millisSinceLastJobHeartbeat() {
            return System.currentTimeMillis() - _jobLastHeartbeatTick;
        }
    }
}
