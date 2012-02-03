package com.cloud.projects;

import java.util.Date;

import com.cloud.acl.ControlledEntity;

public interface ProjectInvitation extends ControlledEntity {
    public enum State {
        Pending, Completed, Expired, Declined
    }

    long getId();

    long getProjectId();

    Long getForAccountId();

    String getToken();

    String getEmail();

    Date getCreated();

    State getState();

    Long getInDomainId();

}
