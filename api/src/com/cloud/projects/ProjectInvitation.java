package com.cloud.projects;

import java.util.Date;

public interface ProjectInvitation {
    public enum State {Pending, Completed, Expired, Declined}

    long getId();

    long getProjectId();

    Long getAccountId();

    String getToken();

    String getEmail();

    Date getCreated();

    State getState();

    Long getDomainId();

}
