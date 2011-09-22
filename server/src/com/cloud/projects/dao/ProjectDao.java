package com.cloud.projects.dao;

import com.cloud.projects.ProjectVO;
import com.cloud.utils.db.GenericDao;

public interface ProjectDao extends GenericDao<ProjectVO, Long>{

    ProjectVO findByNameAndDomain(String name, long domainId);

    Long countProjectsForDomain(long domainId);
    
    ProjectVO findByProjectDomainId(long projectDomainId);
    
    ProjectVO findByProjectAccountId(long projectAccountId);

}
