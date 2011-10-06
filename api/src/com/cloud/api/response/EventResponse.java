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
package com.cloud.api.response;

import java.util.Date;

import com.cloud.api.ApiConstants;
import com.cloud.event.Event;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class EventResponse extends BaseResponse implements ControlledEntityResponse{
    @SerializedName(ApiConstants.ID) @Param(description="the ID of the event")
    private Long id;

    @SerializedName(ApiConstants.USERNAME) @Param(description="the name of the user who performed the action (can be different from the account if an admin is performing an action for a user, e.g. starting/stopping a user's virtual machine)")
    private String username;

    @SerializedName(ApiConstants.TYPE) @Param(description="the type of the event (see event types)")
    private String eventType;

    @SerializedName(ApiConstants.LEVEL) @Param(description="the event level (INFO, WARN, ERROR)")
    private String level;

    @SerializedName(ApiConstants.DESCRIPTION) @Param(description="a brief description of the event")
    private String description;

    @SerializedName(ApiConstants.ACCOUNT) @Param(description="the account name for the account that owns the object being acted on in the event (e.g. the owner of the virtual machine, ip address, or security group)")
    private String accountName;
    
    @SerializedName(ApiConstants.PROJECT_ID) @Param(description="the project id of the ipaddress")
    private Long projectId;
    
    @SerializedName(ApiConstants.PROJECT) @Param(description="the project name of the address")
    private String projectName;

    @SerializedName(ApiConstants.DOMAIN_ID) @Param(description="the id of the account's domain")
    private Long domainId;

    @SerializedName(ApiConstants.DOMAIN) @Param(description="the name of the account's domain")
    private String domainName;

    @SerializedName(ApiConstants.CREATED) @Param(description="the date the event was created")
    private Date created;

    @SerializedName(ApiConstants.STATE) @Param(description="the state of the event")
    private Event.State state;

    @SerializedName("parentid") @Param(description="whether the event is parented")
    private Long parentId;

    public void setId(Long id) {
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
    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    @Override
    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public void setState(Event.State state) {
        this.state = state;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    @Override
    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    @Override
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
}
