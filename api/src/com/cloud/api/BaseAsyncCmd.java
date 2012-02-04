/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.api;

import com.cloud.api.response.AsyncJobResponse;
import com.cloud.async.AsyncJob;
import com.cloud.user.User;
import com.cloud.user.UserContext;

/**
 * A base command for supporting asynchronous API calls. When an API command is received, the command will be
 * serialized to the queue (currently the async_job table) and a response will be immediately returned with the
 * id of the queue object. The id can be used to query the status/progress of the command using the
 * queryAsyncJobResult API command.
 */
public abstract class BaseAsyncCmd extends BaseCmd {
    public static final String ipAddressSyncObject = "ipaddress";
    public static final String networkSyncObject = "network";

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

        response.setJobId(jobId);
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

    public AsyncJob.Type getInstanceType() {
        return AsyncJob.Type.None;
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
        UserContext ctx = UserContext.current();
        Long userId = ctx.getCallerUserId();
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
        UserContext ctx = UserContext.current();
        Long userId = ctx.getCallerUserId();
        userId = (userId == null) ? User.UID_SYSTEM : userId;
        Long startEvent = startEventId;
        if (startEvent == null) {
            startEvent = 0L;
        }
        return _mgr.saveCompletedEvent((userId == null) ? User.UID_SYSTEM : userId, getEntityOwnerId(), level, eventType, description, startEvent);
    }

}
