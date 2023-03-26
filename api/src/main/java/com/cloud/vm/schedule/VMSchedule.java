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
package com.cloud.vm.schedule;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;
import java.util.Date;

public interface VMSchedule extends InternalIdentity, Identity {
    public enum State {
        DISABLED, ENABLED
    }

    public enum Action {
        start, stop, reboot, stopForced
    }

    public String getUuid();

    public String getDescription();

    public String getAction();

    public VMSchedule.State getState();

    public void setState(State state);

    public String getTimezone();

    public String getTag();

    public Long getVmId();

    Date getScheduledTimestamp();

    String getScheduleType();

    String getSchedule();

    long getId();
}
