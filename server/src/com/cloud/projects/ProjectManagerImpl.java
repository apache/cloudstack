package com.cloud.projects;

import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.DataCenter;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.UserContext;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.utils.component.Manager;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

@Local(value = { ProjectService.class })
public class ProjectManagerImpl implements ProjectManager, Manager{
    public static final Logger s_logger = Logger.getLogger(ProjectManagerImpl.class);
    private String _name;
    private long _maxProjects;
    
    @Inject
    private AccountDao _accountDao;
    @Inject
    private DomainDao _domainDao;
    @Inject
    private ProjectDao _projectDao;
    @Inject
    AccountManager _accountMgr;
    @Inject
    ConfigurationManager _configMgr;  
    
    
    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        _name = name;

        ComponentLocator locator = ComponentLocator.getCurrentLocator();
        ConfigurationDao configDao = locator.getDao(ConfigurationDao.class);
        Map<String, String> configs = configDao.getConfiguration(params);

        String value = configs.get(Config.DefaultMaxAccountProjects.key());
        _maxProjects = NumbersUtil.parseLong(value, 20);
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
    public Project createProject(String name, String displayText, long zoneId, String accountName, Long domainId) {
        Account caller = UserContext.current().getCaller();
        Account owner = caller;
        
        //Verify request parameters
        if ((accountName != null && domainId == null) || (domainId != null && accountName == null)) {
            throw new InvalidParameterValueException("Account name and domain id must be specified together");
        }
        
        if (accountName != null) {
            owner = _accountMgr.finalizeOwner(caller, accountName, domainId);
        }
        
        DataCenter zone = _configMgr.getZone(zoneId);
        
        if (zone == null) {
            throw new InvalidParameterValueException("Unable to find zone by id " + zoneId);
        }
        
        //TODO - do resource limit check here
        
        Project project = _projectDao.persist(new ProjectVO(name, displayText, zoneId, owner.getAccountId(), owner.getDomainId()));
        
        if (project != null) {
            UserContext.current().setEventDetails("Project id=" + project.getId());
        }
        
        return project;
    }
    
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_PROJECT_DELETE, eventDescription = "deleting project", async = true)
    public boolean deleteProject (long projectId) {
        Account caller = UserContext.current().getCaller();
        
        Project project= getProject(projectId);
        //verify input parameters
        if (project == null) {
            throw new InvalidParameterValueException("Unable to find project by id " + projectId);
        }
        
        _accountMgr.checkAccess(caller, project);
        
        //TODO - delete all project resources here
        
        return _projectDao.remove(projectId);

    }
    
    @Override
    public Project getProject (long projectId) {
        return _projectDao.findById(projectId);
    }
    
    @Override
    public List<? extends Project> listProjects(Long id, String name, String displayText, Long zoneId, String accountName, Long domainId, String keyword, Long startIndex, Long pageSize) {
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
                    Account owner = _accountMgr.getActiveAccount(accountName, domainId);
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
        
        if (zoneId != null) {
            sc.addAnd("dataCenterId", Op.EQ, zoneId);
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

}
