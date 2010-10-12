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

import com.cloud.event.EventState;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class EventResponse extends BaseResponse {
    @SerializedName("id") @Param(description="the ID of the event")
    private Long id;

    @SerializedName("username") @Param(description="the name of the user who performed the action (can be different from the account if an admin is performing an action for a user, e.g. starting/stopping a user's virtual machine)")
    private String username;

    @SerializedName("type") @Param(description="the type of the event (see event types)")
    private String eventType;

    @SerializedName("level") @Param(description="the event level (INFO, WARN, ERROR)")
    private String level;

    @SerializedName("description") @Param(description="a brief description of the event")
    private String description;

    @SerializedName("account") @Param(description="the account name for the account that owns the object being acted on in the event (e.g. the owner of the virtual machine, ip address, or security group)")
    private String accountName;

    @SerializedName("domainid") @Param(description="the id of the account's domain")
    private Long domainId;

    @SerializedName("domain") @Param(description="the name of the account's domain")
    private String domainName;

    @SerializedName("created") @Param(description="the date the event was created")
    private Date created;

    @SerializedName("state") @Param(description="the state of the event")
    private EventState state;

    @SerializedName("parentid") @Param(description="whether the event is parented")
    private Long parentId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public EventState getState() {
        return state;
    }

    public void setState(EventState state) {
        this.state = state;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }
}
