package com.cloud.projects.dao;

import java.util.List;

import com.cloud.projects.ProjectAccountVO;
import com.cloud.utils.db.GenericDao;

public interface ProjectAccountDao extends GenericDao<ProjectAccountVO, Long>{
    ProjectAccountVO getProjectOwner(long projectId);
    List<ProjectAccountVO> listByProjectId(long projectId);
    ProjectAccountVO findByProjectIdAccountId(long projectId, long accountId);
    
    boolean canAccessAccount(long accountId, long projectAccountId);
    
    boolean canAccessDomain(long accountId, long projectDomainId);
}
