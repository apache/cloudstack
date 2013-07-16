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
package org.apache.cloudstack.api;

import org.apache.cloudstack.api.response.AsyncJobResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.async.AsyncJob;
import com.cloud.user.User;

/**
 * queryAsyncJobResult API command.
 */
public abstract class BaseAsyncCmd extends BaseCmd {

    public static final String ipAddressSyncObject = "ipaddress";
    public static final String networkSyncObject = "network";
    public static final String vpcSyncObject = "vpc";
    public static final String snapshotHostSyncObject = "snapshothost";
    public static final String gslbSyncObject = "globalserverloadbalacner";

    private AsyncJob job;

    @Parameter(name = "starteventid", type = CommandType.LONG)
    private Long startEventId;

    /**
     * For proper tracking of async commands through the system, events must be generated when the command is
     * scheduled, started, and completed. Commands should specify the type of event so that when the scheduled,
     * started, and completed events are saved to the events table, they have the proper type information.
     *
     * @return a string representing the type of event, e.g. VM.START, VOLUME.CREATE.
     */
    public abstract String getEventType();

    /**
     * For proper tracking of async commands through the system, events must be generated when the command is
     * scheduled, started, and completed. Commands should specify a description for these events so that when
     * the scheduled, started, and completed events are saved to the events table, they have a meaningful description.
     *
     * @return a string representing a description of the event
     */
    public abstract String getEventDescription();

    public ResponseObject getResponse(long jobId) {
        AsyncJobResponse response = new AsyncJobResponse();

        AsyncJob job = _entityMgr.findById(AsyncJob.class, jobId);
        response.setJobId(job.getUuid());
        response.setResponseName(getCommandName());
        return response;
    }

    public void setJob(AsyncJob job) {
        this.job = job;
    }

    public Long getStartEventId() {
        return startEventId;
    }

    public void setStartEventId(Long startEventId) {
        this.startEventId = startEventId;
    }

    /**
     * Async commands that want to be tracked as part of the listXXX commands need to
     * provide implementations of the two following methods, getInstanceId() and getInstanceType()
     *
     * getObjectId() should return the id of the object the async command is executing on
     * getObjectType() should return a type from the AsyncJob.Type enumeration
     */
    public Long getInstanceId() {
        return null;
    }

    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.None;
    }

    public String getSyncObjType() {
        return null;
    }

    public Long getSyncObjId() {
        return null;
    }

    public AsyncJob getJob() {
        return job;
    }

    protected long saveStartedEvent() {
        return saveStartedEvent(getEventType(), "Executing job for " + getEventDescription(), getStartEventId());
    }

    protected long saveStartedEvent(String eventType, String description, Long startEventId) {
        CallContext ctx = CallContext.current();
        Long userId = ctx.getCallingUserId();
        userId = (userId == null) ? User.UID_SYSTEM : userId;
        Long startEvent = startEventId;
        if (startEvent == null) {
            startEvent = 0L;
        }
        return _mgr.saveStartedEvent((userId == null) ? User.UID_SYSTEM : userId, getEntityOwnerId(), eventType, description, startEvent);
    }

    protected long saveCompletedEvent(String level, String description) {
        return saveCompletedEvent(level, getEventType(), description, getStartEventId());
    }

    protected long saveCompletedEvent(String level, String eventType, String description, Long startEventId) {
        CallContext ctx = CallContext.current();
        Long userId = ctx.getCallingUserId();
        userId = (userId == null) ? User.UID_SYSTEM : userId;
        Long startEvent = startEventId;
        if (startEvent == null) {
            startEvent = 0L;
        }
        return _mgr.saveCompletedEvent((userId == null) ? User.UID_SYSTEM : userId, getEntityOwnerId(), level, eventType, description, startEvent);
    }

}
