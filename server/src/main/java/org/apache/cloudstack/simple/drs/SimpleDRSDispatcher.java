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
package org.apache.cloudstack.simple.drs;

import javax.inject.Inject;

import com.cloud.utils.component.AdapterBase;

import org.apache.cloudstack.framework.jobs.AsyncJob;
import org.apache.cloudstack.framework.jobs.AsyncJobDispatcher;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.framework.jobs.impl.JobSerializerHelper;
import org.apache.cloudstack.jobs.JobInfo;
import org.apache.log4j.Logger;

public class SimpleDRSDispatcher extends AdapterBase implements AsyncJobDispatcher {

    public static final String SIMPLE_DRS_DISPATCHER = "SimpleDRSDispatcher";

    public static final Logger LOG = Logger.getLogger(SimpleDRSDispatcher.class);

    @Inject
    protected SimpleDRSManager drsManager;

    @Inject
    private AsyncJobManager asyncJobManager;

    @Override
    public String getName() {
        return SIMPLE_DRS_DISPATCHER;
    }

    @Override
    public void runJob(AsyncJob job) {
        LOG.info("Dispatching : " + job);
        SimpleDRSJobInfo info = (SimpleDRSJobInfo) JobSerializerHelper.fromObjectSerializedString(job.getCmdInfo());
        drsManager.balanceCluster(info);
        asyncJobManager.completeAsyncJob(job.getId(), JobInfo.Status.SUCCEEDED, 0, "Complete");
    }
}
