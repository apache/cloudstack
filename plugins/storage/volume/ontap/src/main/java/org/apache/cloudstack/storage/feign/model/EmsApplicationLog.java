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

package org.apache.cloudstack.storage.feign.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmsApplicationLog {

    @JsonProperty("computer_name")
    private String computerName;

    @JsonProperty("event_source")
    private String eventSource;

    @JsonProperty("app_version")
    private String appVersion;

    @JsonProperty("category")
    private String category;

    @JsonProperty("severity")
    private String severity;

    @JsonProperty("autosupport_required")
    private Boolean autosupportRequired;

    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("event_description")
    private String eventDescription;

    public EmsApplicationLog() {
    }

    public String getComputerName() {
        return computerName;
    }

    public void setComputerName(String computerName) {
        this.computerName = computerName;
    }

    public String getEventSource() {
        return eventSource;
    }

    public void setEventSource(String eventSource) {
        this.eventSource = eventSource;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public Boolean getAutosupportRequired() {
        return autosupportRequired;
    }

    public void setAutosupportRequired(Boolean autosupportRequired) {
        this.autosupportRequired = autosupportRequired;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventDescription() {
        return eventDescription;
    }

    public void setEventDescription(String eventDescription) {
        this.eventDescription = eventDescription;
    }

    @Override
    public String toString() {
        return "EmsApplicationLog{" +
                "computerName='" + computerName + '\'' +
                ", eventSource='" + eventSource + '\'' +
                ", appVersion='" + appVersion + '\'' +
                ", category='" + category + '\'' +
                ", severity='" + severity + '\'' +
                ", autosupportRequired=" + autosupportRequired +
                ", eventId='" + eventId + '\'' +
                ", eventDescription='" + eventDescription + '\'' +
                '}';
    }
}
