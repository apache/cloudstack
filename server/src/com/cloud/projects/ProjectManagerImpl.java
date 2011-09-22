package com.cloud.projects;

import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.projects.Project.State;
import com.cloud.projects.dao.ProjectAccountDao;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.DomainManager;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.UserContext;
import com.cloud.utils.component.Inject;
import com.cloud.utils.component.Manager;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.Transaction;

@Local(value = { ProjectService.class, ProjectManager.class })
public class ProjectManagerImpl implements ProjectManager, Manager{
    public static final Logger s_logger = Logger.getLogger(ProjectManagerImpl.class);
    private String _name;
    
    @Inject
    private DomainDao _domainDao;
    @Inject
    private ProjectDao _projectDao;
    @Inject
    AccountManager _accountMgr;
    @Inject
    DomainManager _domainMgr;
    @Inject
    ConfigurationManager _configMgr;  
    @Inject
    ResourceLimitService _resourceLimitMgr;
    @Inject
    private ProjectAccountDao _projectAccountDao;
    
    
    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        _name = name;

        return true;
    }
    
    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public String getName() {
        return _name;
    }
    
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_PROJECT_CREATE, eventDescription = "creating project")
    @DB
    public Project createProject(String name, String displayText, String accountName, Long domainId) throws ResourceAllocationException{
        Account caller = UserContext.current().getCaller();
        Account owner = caller;
        
        //Verify request parameters
        if ((accountName != null && domainId == null) || (domainId != null && accountName == null)) {
            throw new InvalidParameterValueException("Account name and domain id must be specified together");
        }
        
        if (accountName != null) {
            owner = _accountMgr.finalizeOwner(caller, accountName, domainId);
        }
        
        //don't allow 2 projects with the same name inside the same domain
        if (_projectDao.findByNameAndDomain(name, owner.getDomainId()) != null) {
            throw new InvalidParameterValueException("Project with name " + name + " already exists in domain id=" + owner.getDomainId());
        }
        
        Domain ownerDomain = _domainDao.findById(owner.getDomainId());
        
        //do resource limit check
        _resourceLimitMgr.checkResourceLimit(owner, ResourceType.project);
        
        Transaction txn = Transaction.currentTxn();
        txn.start();
        
        //Create a domain associated with the project
        StringBuilder dmnNm = new StringBuilder("PrjDmn-");
        dmnNm.append(name).append("-").append(owner.getDomainId());
        
        Domain projectDomain = _domainMgr.createDomain(dmnNm.toString(), Domain.ROOT_DOMAIN, Account.ACCOUNT_ID_SYSTEM, null, Domain.Type.Project);
        
        //Create an account associated with the project
        StringBuilder acctNm = new StringBuilder("PrjAcct-");
        acctNm.append(name).append("-").append(owner.getDomainId());
        
        Account projectAccount = _accountMgr.createAccount(acctNm.toString(), Account.ACCOUNT_TYPE_PROJECT, projectDomain.getId(), null);
        
        Project project = _projectDao.persist(new ProjectVO(name, displayText, owner.getDomainId(), projectAccount.getId(), projectDomain.getId()));
        
        //assign owner to the project
        assignAccountToProject(project, owner.getId(), ProjectAccount.Role.Owner);
        
        if (project != null) {
            UserContext.current().setEventDetails("Project id=" + project.getId());
        }
        
        //Increment resource count for the Owner's domain
        _resourceLimitMgr.incrementResourceCount(owner.getAccountId(), ResourceType.project);
        
        txn.commit();
        
        return project;
    }
    
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_PROJECT_DELETE, eventDescription = "deleting project", async = true)
    @DB
    public boolean deleteProject (long projectId) {
        Account caller = UserContext.current().getCaller();
        
        ProjectVO project= getProject(projectId);
        //verify input parameters
        if (project == null) {
            throw new InvalidParameterValueException("Unable to find project by id " + projectId);
        }
        
        _accountMgr.checkAccess(caller, _domainDao.findById(project.getDomainId()));
        
        //mark project as inactive first, so you can't add resources to it
        Transaction txn = Transaction.currentTxn();
        txn.start();
        s_logger.debug("Marking project id=" + projectId + " with state " + State.Inactive + " as a part of project delete...");
        project.setState(State.Inactive);
        boolean updateResult = _projectDao.update(projectId, project);
        _resourceLimitMgr.decrementResourceCount(project.getProjectAccountId(), ResourceType.project);
        txn.commit();
        
        if (updateResult) {
            if (!cleanupProject(project)) {
                s_logger.warn("Failed to cleanup project's id=" + projectId + " resources, not removing the project yet");
                return false;
            } else {
                return _projectDao.remove(projectId);
            }
        } else {
            s_logger.warn("Failed to mark the project id=" + projectId + " with state " + State.Inactive);
            return false;
        }  
    }
    
    private boolean cleanupProject(Project project) {
        boolean result=true;
        
        //Unassign all users from the project
        s_logger.debug("Unassigning all accounts from project " + project + " as a part of project cleanup...");
        List<? extends ProjectAccount> projectAccounts = _projectAccountDao.listByProjectId(project.getId());
        for (ProjectAccount projectAccount : projectAccounts) {
            result = result && unassignAccountFromProject(projectAccount.getProjectId(), projectAccount.getAccountId());
        }
        
        if (result) {
            s_logger.debug("Accounts are unassign successfully from project " + project + " as a part of project cleanup...");
        }
        
        //Delete project's domain
        s_logger.debug("Deleting projects " + project + " internal domain id=" + project.getProjectDomainId() + " as a part of project cleanup...");
        result = result && _domainMgr.deleteDomain(project.getProjectDomainId(), true);
        
        return result;
    }
    
    @Override
    public boolean unassignAccountFromProject(long projectId, long accountId) {
        ProjectAccountVO projectAccount = _projectAccountDao.findByProjectIdAccountId(projectId, accountId);
        if (projectAccount == null) {
            s_logger.debug("Account id=" + accountId + " is not assigned to project id=" + projectId + " so no need to unassign");
            return true;
        }
        
        if ( _projectAccountDao.remove(projectAccount.getId())) {
            return true;
        } else {
            s_logger.warn("Failed to unassign account id=" + accountId + " from the project id=" + projectId);
            return false;
        }
    }
    
    @Override
    public ProjectVO getProject (long projectId) {
        return _projectDao.findById(projectId);
    }
    
    @Override
    public List<? extends Project> listProjects(Long id, String name, String displayText, String accountName, Long domainId, String keyword, Long startIndex, Long pageSize) {
        Account caller = UserContext.current().getCaller();
        Long accountId = null;
        String path = null;
        
        Filter searchFilter = new Filter(ProjectVO.class, "id", false, startIndex, pageSize);
        SearchBuilder<ProjectVO> sb = _projectDao.createSearchBuilder();
        
        if (_accountMgr.isAdmin(caller.getType())) {
            if (domainId != null) {
                DomainVO domain = _domainDao.findById(domainId);
                if (domain == null) {
                    throw new InvalidParameterValueException("Domain id=" + domainId + " doesn't exist in the system");
                }

                _accountMgr.checkAccess(caller, domain);

                if (accountName != null) {
                    Account owner = _accountMgr.getActiveAccountByName(accountName, domainId);
                    if (owner == null) {
                        throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain " + domainId);
                    }
                    accountId = owner.getId();
                }
            }
            
            if (caller.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) {
                DomainVO domain = _domainDao.findById(caller.getDomainId());
                path = domain.getPath();
            }
        } else {
            if (accountName != null && !accountName.equals(caller.getAccountName())) {
                throw new PermissionDeniedException("Can't list account " + accountName + " projects; unauthorized");
            }
            
            if (domainId != null && domainId.equals(caller.getDomainId())) {
                throw new PermissionDeniedException("Can't list domain id= " + domainId + " projects; unauthorized");
            }
            
            accountId = caller.getId();
        }
        
        if (path != null) {
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
            sb.join("domainSearch", domainSearch, sb.entity().getDomainId(), domainSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        }
        
        SearchCriteria<ProjectVO> sc = sb.create();
        
        if (id != null) {
            sc.addAnd("id", Op.EQ, id);
        }
        
        if (name != null) {
            sc.addAnd("name", Op.EQ, name);
        }
        
        if (displayText != null) {
            sc.addAnd("displayText", Op.EQ, displayText);
        }
        
        if (accountId != null) {
            sc.addAnd("accountId", Op.EQ, accountId);
        }
        
        if (keyword != null) {
            SearchCriteria<ProjectVO> ssc = _projectDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("displayText", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }
        
        if (path != null) {
            sc.setJoinParameters("domainSearch", "path", path);
        }
        
        return _projectDao.search(sc, searchFilter);
    }
    
    @Override
    public ProjectAccount assignAccountToProject(Project project, long accountId, ProjectAccount.Role accountRole) {
        return _projectAccountDao.persist(new ProjectAccountVO(project, accountId, accountRole));
    }
    
    @Override
    public Account getProjectOwner(long projectId) {
        long accountId = _projectAccountDao.getProjectOwner(projectId).getAccountId();
        return _accountMgr.getAccount(accountId);
    }
    
    @Override
    public ProjectVO findByProjectDomainId(long projectDomainId) {
        return _projectDao.findByProjectDomainId(projectDomainId);
    }
    
    @Override
    public ProjectVO findByProjectAccountId(long projectAccountId) {
        return _projectDao.findByProjectAccountId(projectAccountId);
    }

}
