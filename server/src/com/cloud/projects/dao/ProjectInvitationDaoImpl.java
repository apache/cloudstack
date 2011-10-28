package com.cloud.projects.dao;

import java.sql.Date;
import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.projects.ProjectInvitation.State;
import com.cloud.projects.ProjectInvitationVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value={ProjectInvitationDao.class})
public class ProjectInvitationDaoImpl extends GenericDaoBase<ProjectInvitationVO, Long> implements ProjectInvitationDao {
    private static final Logger s_logger = Logger.getLogger(ProjectInvitationDaoImpl.class);
    protected final SearchBuilder<ProjectInvitationVO> AllFieldsSearch;
    protected final SearchBuilder<ProjectInvitationVO> InactiveSearch;
    
    protected ProjectInvitationDaoImpl() {
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("accountId", AllFieldsSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("projectId", AllFieldsSearch.entity().getProjectId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("created", AllFieldsSearch.entity().getCreated(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("projectAccountId", AllFieldsSearch.entity().getState(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("state", AllFieldsSearch.entity().getState(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("email", AllFieldsSearch.entity().getEmail(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("token", AllFieldsSearch.entity().getToken(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("id", AllFieldsSearch.entity().getId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.done();
        
        InactiveSearch = createSearchBuilder();
        InactiveSearch.and("id", InactiveSearch.entity().getId(), SearchCriteria.Op.EQ);
        InactiveSearch.and("accountId", InactiveSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        InactiveSearch.and("projectId", InactiveSearch.entity().getProjectId(), SearchCriteria.Op.EQ);
        InactiveSearch.and("created", InactiveSearch.entity().getCreated(), SearchCriteria.Op.LTEQ);
        InactiveSearch.and("state", InactiveSearch.entity().getState(), SearchCriteria.Op.EQ);
        InactiveSearch.done();
    }
    
    
    @Override
    public ProjectInvitationVO findPendingByAccountIdProjectId(long accountId, long projectId) {
        SearchCriteria<ProjectInvitationVO> sc = AllFieldsSearch.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("projectId", projectId);
        sc.setParameters("state", State.Pending);
        
        return findOneBy(sc);
    }
    
    @Override
    public List<ProjectInvitationVO> listExpiredInvitations() {
        SearchCriteria<ProjectInvitationVO> sc = AllFieldsSearch.create();
        sc.setParameters("state", State.Expired);
        
        return listBy(sc);
    }
    
    @Override
    public boolean expirePendingInvitations(long timeout) {
        boolean success = true;
        
        SearchCriteria<ProjectInvitationVO> sc = InactiveSearch.create();
        sc.setParameters("created", new Date((System.currentTimeMillis() >> 10) - timeout));
        sc.setParameters("state", State.Pending);
        
        List<ProjectInvitationVO> invitationsToExpire = listBy(sc);
        for (ProjectInvitationVO invitationToExpire : invitationsToExpire) {
            invitationToExpire.setState(State.Expired);
            if (!update(invitationToExpire.getId(), invitationToExpire)) {
                s_logger.warn("Fail to expire invitation " + invitationToExpire.toString());
                success = false;
            }
        }
        
        return success;
    }
    
    @Override
    public boolean isActive(long id, long timeout) {
        SearchCriteria<ProjectInvitationVO> sc = InactiveSearch.create();
        
        sc.setParameters("id", id);
        
        if (findOneBy(sc) == null) {
            s_logger.warn("Unable to find project invitation by id " + id);
            return false;
        }
        
        sc.setParameters("created", new Date((System.currentTimeMillis() >> 10) - timeout));
        
        if (findOneBy(sc) == null) {
            return true;
        } else {
            return false;
        }
    }
    
    @Override
    public ProjectInvitationVO findPendingByEmailAndProjectId(String email, long projectId) {
        SearchCriteria<ProjectInvitationVO> sc = AllFieldsSearch.create();
        sc.setParameters("email", email);
        sc.setParameters("projectId", projectId);
        sc.setParameters("state", State.Pending);
        
        return findOneBy(sc);
    }
    
    @Override
    public ProjectInvitationVO findPendingByTokenAndProjectId(String token, long projectId) {
        SearchCriteria<ProjectInvitationVO> sc = AllFieldsSearch.create();
        sc.setParameters("token", token);
        sc.setParameters("projectId", projectId);
        sc.setParameters("state", State.Pending);
        
        return findOneBy(sc);
    }
    
    @Override
    public ProjectInvitationVO findPendingById(long id) {
        SearchCriteria<ProjectInvitationVO> sc = AllFieldsSearch.create();
        sc.setParameters("id", id);
        sc.setParameters("state", State.Pending);
        
        return findOneBy(sc);
    }
    
    @Override
    public void cleanupInvitations(long projectId) {
        SearchCriteria<ProjectInvitationVO> sc = AllFieldsSearch.create();
        sc.setParameters("projectId", projectId);
        
        int numberRemoved = remove(sc);
        s_logger.debug("Removed " + numberRemoved + " invitations for project id=" + projectId);
    }
}
