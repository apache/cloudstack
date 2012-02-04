package com.cloud.projects;

public interface ProjectAccount {
    public enum Role {
        Admin, Regular
    };

    long getAccountId();

    long getProjectId();

    Role getAccountRole();

    long getProjectAccountId();
}
