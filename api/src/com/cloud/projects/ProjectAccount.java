package com.cloud.projects;


public interface ProjectAccount {
    public enum Role {Owner, Regular};
    
    long getAccountId();
    
    long getProjectId();
    
    Role getAccountRole();
    
    long getProjectAccountId();
}
