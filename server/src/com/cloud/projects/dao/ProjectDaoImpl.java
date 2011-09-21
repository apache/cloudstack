package com.cloud.projects.dao;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.projects.ProjectVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;

@Local(value={ProjectDao.class})
public class ProjectDaoImpl extends GenericDaoBase<ProjectVO, Long> implements ProjectDao {
    private static final Logger s_logger = Logger.getLogger(ProjectDaoImpl.class);
    protected final SearchBuilder<ProjectVO> NameDomainSearch;
   
    protected ProjectDaoImpl() {
        NameDomainSearch = createSearchBuilder();
        NameDomainSearch.and("name", NameDomainSearch.entity().getName(), SearchCriteria.Op.EQ);
        NameDomainSearch.and("domainId", NameDomainSearch.entity().getDomainId(), SearchCriteria.Op.EQ);
        NameDomainSearch.done();
    }
    
    @Override
    public ProjectVO findByNameAndDomain(String name, long domainId) {
        SearchCriteria<ProjectVO> sc = NameDomainSearch.create();
        sc.setParameters("name", name);
        sc.setParameters("domainId", domainId);
        
        return findOneBy(sc);
    }
    
    @Override @DB
    public boolean remove(Long projectId) {
        boolean result = false;
        Transaction txn = Transaction.currentTxn();
        txn.start();
        ProjectVO projectToRemove = findById(projectId);
        projectToRemove.setName(null);
        if (!update(projectId, projectToRemove)) {
            s_logger.warn("Failed to reset name for the project id=" + projectId + " as a part of project remove");
            return false;
        } else {
            
        }
        result = super.remove(projectId);
        txn.commit();
        
        return result;
        
    }
}
