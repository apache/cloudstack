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
package org.apache.cloudstack.framework.jobs;

import java.util.Random;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import org.apache.cloudstack.jobs.JobInfo.Status;

import com.cloud.utils.component.AdapterBase;

public class AsyncJobTestDispatcher extends AdapterBase implements AsyncJobDispatcher {
    private static final Logger s_logger =
            Logger.getLogger(AsyncJobTestDispatcher.class);

    @Inject
    private AsyncJobManager _asyncJobMgr;

    @Inject
    private AsyncJobTestDashboard _testDashboard;

    Random _random = new Random();

    public AsyncJobTestDispatcher() {
    }

    @Override
    public void runJob(final AsyncJob job) {
        _testDashboard.increaseConcurrency();

        s_logger.info("Execute job " + job.getId() + ", current concurrency " + _testDashboard.getConcurrencyCount());

        int interval = 3000;

        try {
            Thread.sleep(interval);
        } catch (InterruptedException e) {
            s_logger.debug("[ignored] .");
        }

        _asyncJobMgr.completeAsyncJob(job.getId(), Status.SUCCEEDED, 0, null);

        _testDashboard.decreaseConcurrency();
        _testDashboard.jobCompleted();
    }
}
