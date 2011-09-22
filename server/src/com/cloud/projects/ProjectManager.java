package com.cloud.projects;

public interface ProjectManager extends ProjectService {
    ProjectVO findByProjectDomainId(long projectDomainId);
    ProjectVO findByProjectAccountId(long projectAccountId);
}
