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
package org.apache.cloudstack.api.response;

import java.util.Date;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.event.Event;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = Event.class)
@SuppressWarnings("unused")
public class EventResponse extends BaseResponse implements ControlledViewEntityResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "The ID of the event")
    private String id;

    @SerializedName(ApiConstants.USERNAME)
    @Param(description = "The name of the User who performed the action (can be different from the Account if an admin is performing an action for a User, e.g. starting/stopping a User's Instance)")
    private String username;

    @SerializedName(ApiConstants.TYPE)
    @Param(description = "The type of the event (see event types)")
    private String eventType;

    @SerializedName(ApiConstants.LEVEL)
    @Param(description = "The event level (INFO, WARN, ERROR)")
    private String level;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "A brief description of the event")
    private String description;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "The Account name for the Account that owns the object being acted on in the event (e.g. the owner of the Instance, IP address, or security group)")
    private String accountName;

    @SerializedName(ApiConstants.PROJECT_ID)
    @Param(description = "The project ID of the IP address")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT)
    @Param(description = "The project name of the address")
    private String projectName;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "The ID of the Account's domain")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "The name of the Account's domain")
    private String domainName;

    @SerializedName(ApiConstants.DOMAIN_PATH)
    @Param(description = "path of the Domain the account's domain belongs to", since = "4.19.2.0")
    private String domainPath;

    @SerializedName(ApiConstants.RESOURCE_ID)
    @Param(description = "The ID of the resource", since = "4.17.0")
    private String resourceId;

    @SerializedName(ApiConstants.RESOURCE_TYPE)
    @Param(description = "The type of the resource", since = "4.17.0")
    private String resourceType;

    @SerializedName(ApiConstants.RESOURCE_NAME)
    @Param(description = "The name of the resource", since = "4.17.0")
    private String resourceName;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "The date the event was created")
    private Date created;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "The state of the event")
    private Event.State state;

    @SerializedName(ApiConstants.PARENT_ID)
    @Param(description = "Whether the event is parented")
    private String parentId;

    @SerializedName(ApiConstants.ARCHIVED)
    @Param(description = "Whether the event has been archived or not")
    private Boolean archived;

    public void setId(String id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    @Override
    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    @Override
    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    @Override
    public void setDomainPath(String domainPath) {
        this.domainPath = domainPath;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public void setState(Event.State state) {
        this.state = state;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    @Override
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    @Override
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setArchived(Boolean archived) {
        this.archived = archived;
    }
}
