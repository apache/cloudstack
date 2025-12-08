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

import java.util.Date;

import org.apache.cloudstack.framework.jobs.impl.SyncQueueItem;
import org.apache.cloudstack.jobs.JobInfo;

public interface AsyncJob extends JobInfo {

    public enum JournalType {
        SUCCESS, FAILURE
    };

    public static interface Topics {
        public static final String JOB_HEARTBEAT = "job.heartbeat";
        public static final String JOB_STATE = "job.state";
        public static final String JOB_EVENT_PUBLISH = "job.eventpublish";
    }

    public static interface Constants {

        // Although we may have detailed masks for each individual wakeup event, i.e.
        // periodical timer, matched topic from message bus, it seems that we don't
        // need to distinguish them to such level. Therefore, only one wakeup signal
        // is defined
        public static final int SIGNAL_MASK_WAKEUP = 1;

        public static final String MS_ID = "MS_ID";

        public static final String SYNC_LOCK_NAME = "SyncLock";
    }

    @Override
    String getType();

    @Override
    String getDispatcher();

    @Override
    int getPendingSignals();

    @Override
    long getUserId();

    @Override
    long getAccountId();

    @Override
    String getCmd();

    @Override
    int getCmdVersion();

    @Override
    String getCmdInfo();

    @Override
    Status getStatus();

    @Override
    int getProcessStatus();

    @Override
    int getResultCode();

    @Override
    String getResult();

    @Override
    Long getInitMsid();

    void setInitMsid(Long msid);

    @Override
    Long getExecutingMsid();

    void setExecutingMsid(Long msid);

    @Override
    Long getCompleteMsid();

    void setCompleteMsid(Long msid);

    @Override
    Date getCreated();

    @Override
    Date getLastUpdated();

    @Override
    Date getLastPolled();

    @Override
    String getInstanceType();

    @Override
    Long getInstanceId();

    String getShortUuid();

    SyncQueueItem getSyncSource();

    void setSyncSource(SyncQueueItem item);

    String getRelated();
}
