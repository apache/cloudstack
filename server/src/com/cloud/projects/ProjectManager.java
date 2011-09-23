package com.cloud.projects;

import com.cloud.user.Account;

public interface ProjectManager extends ProjectService {
    boolean canAccessAccount(Account caller, long accountId);
    
    boolean canAccessDomain(Account caller, long domainId);
}
