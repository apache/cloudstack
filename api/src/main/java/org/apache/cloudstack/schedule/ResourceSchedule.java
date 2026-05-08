/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.schedule;

import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

import java.time.ZoneId;
import java.util.Date;

public interface ResourceSchedule extends Identity, InternalIdentity {

    /**
     * Common contract for scheduler actions.  Each provider defines its own enum
     * implementing this interface so the generic machinery can call {@link #name()}
     * and {@link #getEventType()} without knowing the concrete type.
     */
    interface Action {
        /** Enum constant name (START, STOP, SCALE_UP, …). */
        String name();

        /** CloudStack event type string used for audit / quota attribution. */
        String getEventType();
    }

    ApiCommandResourceType getResourceType();

    long getResourceId();

    String getDescription();

    String getSchedule();

    String getTimeZone();

    /** Returns the raw action name stored in the DB (e.g. "START", "SCALE_UP"). */
    String getActionName();

    boolean getEnabled();

    Date getStartDate();

    Date getEndDate();

    ZoneId getTimeZoneId();

    Date getCreated();
}
