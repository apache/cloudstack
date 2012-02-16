package com.cloud.projects;

import java.util.List;

import com.cloud.user.Account;

public interface ProjectManager extends ProjectService {
    boolean canAccessProjectAccount(Account caller, long accountId);

    boolean canModifyProjectAccount(Account caller, long accountId);

    boolean deleteAccountFromProject(long projectId, long accountId);

    List<Long> listPermittedProjectAccounts(long accountId);

    boolean projectInviteRequired();

    boolean allowUserToCreateProject();

    boolean deleteProject(Account caller, long callerUserId, ProjectVO project);

}
