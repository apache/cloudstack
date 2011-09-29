package com.cloud.projects;

import java.util.List;

import com.cloud.user.Account;

public interface ProjectManager extends ProjectService {
    boolean canAccessAccount(Account caller, long accountId);
    
    boolean canAccessDomain(Account caller, long domainId);

    boolean canModifyProjectAccount(Account caller, long accountId);

    boolean canModifyProjectDomain(Account caller, long domainId);

    boolean deleteAccountFromProject(long projectId, long accountId);
    
    List<Long> listPermittedProjectAccounts(long accountId);
}
