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

package org.apache.cloudstack.poll;

import com.cloud.utils.component.Manager;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class BackgroundPollManagerImpl extends ManagerBase implements BackgroundPollManager, Manager {

    private ScheduledExecutorService backgroundPollTaskScheduler;
    private List<BackgroundPollTask> submittedTasks = new ArrayList<>();
    private volatile boolean isConfiguredAndStarted = false;

    public long getInitialDelay() {
        return 5000L;
    }

    public long getRoundDelay() {
        return 4000L;
    }

    @Override
    public boolean start() {
        if (isConfiguredAndStarted) {
            return true;
        }
        backgroundPollTaskScheduler = Executors.newScheduledThreadPool(submittedTasks.size() + 1, new NamedThreadFactory("BackgroundTaskPollManager"));
        for (final BackgroundPollTask task : submittedTasks) {
            Long delay = task.getDelay();
            if (delay == null) {
                delay = getRoundDelay();
            }
            backgroundPollTaskScheduler.scheduleWithFixedDelay(task, getInitialDelay(), delay, TimeUnit.MILLISECONDS);
            logger.debug("Scheduled background poll task: " + task.getClass().getName());
        }
        isConfiguredAndStarted = true;
        return true;
    }

    @Override
    public boolean stop() {
        if (isConfiguredAndStarted) {
            backgroundPollTaskScheduler.shutdown();
        }
        return true;
    }

    @Override
    public void submitTask(final BackgroundPollTask task) {
        Preconditions.checkNotNull(task);
        if (isConfiguredAndStarted) {
            throw new CloudRuntimeException("Background Poll Manager cannot accept poll task as it has been configured and started.");
        }
        logger.debug("Background Poll Manager received task: " + task.getClass().getSimpleName());
        submittedTasks.add(task);
    }
}
