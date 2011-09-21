package com.cloud.projects.dao;

import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.projects.ProjectAccount;
import com.cloud.projects.ProjectAccountVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value={ProjectAccountDao.class})
public class ProjectAccountDaoImpl extends GenericDaoBase<ProjectAccountVO, Long> implements ProjectAccountDao {
    private static final Logger s_logger = Logger.getLogger(ProjectAccountDaoImpl.class);
    
    protected final SearchBuilder<ProjectAccountVO> AllFieldsSearch;
    
    
    protected ProjectAccountDaoImpl() {
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("role", AllFieldsSearch.entity().getAccountRole(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("projectId", AllFieldsSearch.entity().getProjectId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("accountId", AllFieldsSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.done();
    }
    
    @Override
    public ProjectAccountVO getProjectOwner(long projectId) {
        SearchCriteria<ProjectAccountVO> sc = AllFieldsSearch.create();
        sc.setParameters("role", ProjectAccount.Role.Owner);
        sc.setParameters("projectId", projectId);
        
        return findOneBy(sc);
    }
    
    @Override
    public List<ProjectAccountVO> listByProjectId(long projectId) {
        SearchCriteria<ProjectAccountVO> sc = AllFieldsSearch.create();
        sc.setParameters("projectId", projectId);
        
        return listBy(sc);
    }
    
    @Override
    public ProjectAccountVO findByProjectIdAccountId(long projectId, long accountId) {
        SearchCriteria<ProjectAccountVO> sc = AllFieldsSearch.create();
        sc.setParameters("projectId", projectId);
        sc.setParameters("accountId", accountId);
        
        return findOneBy(sc);
    }
    
}
