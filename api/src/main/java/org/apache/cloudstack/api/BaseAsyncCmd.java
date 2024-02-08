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



/**
 * queryAsyncJobResult API command.
 */
public abstract class BaseAsyncCmd extends BaseCmd {

    public static final String ipAddressSyncObject = "ipaddress";
    public static final String networkSyncObject = "network";
    public static final String vpcSyncObject = "vpc";
    public static final String migrationSyncObject = "migration";
    public static final String snapshotHostSyncObject = "snapshothost";
    public static final String gslbSyncObject = "globalserverloadbalancer";

    private Object job;

    @Parameter(name = "starteventid", type = CommandType.LONG)
    private Long startEventId;

    @Parameter(name = ApiConstants.CUSTOM_JOB_ID , type = CommandType.STRING)
    private String injectedJobId;

    public String getInjectedJobId() {
        return this.injectedJobId;
    }

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

    public void setJob(Object job) {
        this.job = job;
    }

    public Long getStartEventId() {
        return startEventId;
    }

    public void setStartEventId(Long startEventId) {
        this.startEventId = startEventId;
    }

    public String getSyncObjType() {
        return null;
    }

    public Long getSyncObjId() {
        return null;
    }

    public Object getJob() {
        return job;
    }

}
