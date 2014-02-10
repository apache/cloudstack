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

package org.apache.cloudstack.framework.events;

import com.google.gson.Gson;

public class Event {

    String eventCategory;
    String eventType;
    String eventSource;
    String resourceType;
    String resourceUUID;
    String description;

    public Event(String eventSource, String eventCategory, String eventType, String resourceType, String resourceUUID) {
        this.eventCategory = eventCategory;
        this.eventType = eventType;
        this.eventSource = eventSource;
        this.resourceType = resourceType;
        this.resourceUUID = resourceUUID;
    }

    public String getEventCategory() {
        return eventCategory;
    }

    public void setEventCategory(String category) {
        eventCategory = category;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String type) {
        eventType = type;
    }

    public String getEventSource() {
        return eventSource;
    }

    void setEventSource(String source) {
        eventSource = source;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(Object message) {
        Gson gson = new Gson();
        this.description = gson.toJson(message).toString();
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public void setResourceUUID(String uuid) {
        this.resourceUUID = uuid;
    }

    public String getResourceUUID() {
        return resourceUUID;
    }
}