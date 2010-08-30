package com.cloud.api.response;

import java.util.Date;

import com.cloud.api.ResponseObject;
import com.cloud.event.EventState;
import com.cloud.serializer.Param;

public class EventResponse implements ResponseObject {
    @Param(name="id")
    private Long id;

    @Param(name="username")
    private String username;

    @Param(name="type")
    private String eventType;

    @Param(name="level")
    private String level;

    @Param(name="description")
    private String description;

    @Param(name="account")
    private String accountName;

    @Param(name="domainid")
    private Long domainId;

    @Param(name="domain")
    private String domainName;

    @Param(name="created")
    private Date created;

    @Param(name="state")
    private EventState state;

    @Param(name="parentid")
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
