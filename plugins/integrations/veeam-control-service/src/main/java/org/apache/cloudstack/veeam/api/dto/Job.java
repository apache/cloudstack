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

package org.apache.cloudstack.veeam.api.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Job {
    private String autoCleared;
    private String external;
    private Long lastUpdated;
    private Long startTime;
    private Long endTime;
    private String status;
    private Ref owner;
    private Actions actions;
    private String description;
    private List<Link> link;
    private String href;
    private String id;

    // getters and setters
    public String getAutoCleared() { return autoCleared; }
    public void setAutoCleared(String autoCleared) { this.autoCleared = autoCleared; }

    public String getExternal() { return external; }
    public void setExternal(String external) { this.external = external; }

    public Long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Long lastUpdated) { this.lastUpdated = lastUpdated; }

    public Long getStartTime() { return startTime; }
    public void setStartTime(Long startTime) { this.startTime = startTime; }

    public Long getEndTime() { return endTime; }
    public void setEndTime(Long endTime) { this.endTime = endTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Ref getOwner() { return owner; }
    public void setOwner(Ref owner) { this.owner = owner; }

    public Actions getActions() { return actions; }
    public void setActions(Actions actions) { this.actions = actions; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<Link> getLink() { return link; }
    public void setLink(List<Link> link) { this.link = link; }

    public String getHref() { return href; }
    public void setHref(String href) { this.href = href; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}
