/**
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.projects;

import java.sql.Date;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.acl.SecurityChecker.AccessType;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.projects.Project.State;
import com.cloud.projects.ProjectAccount.Role;
import com.cloud.projects.dao.ProjectAccountDao;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.projects.dao.ProjectInvitationDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.DomainManager;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.UserContext;
import com.cloud.user.dao.AccountDao;
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
    @Inject
    private AccountDao _accountDao;
    @Inject
    private ConfigurationDao _configDao;
    @Inject
    private ProjectInvitationDao _projectInvitationDao;
    
    protected boolean _invitationRequired = false;
    protected long _invitationTimeOut = 86400;
    
    
    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        _name = name;
        
        Map<String, String> configs = _configDao.getConfiguration(params);
        _invitationRequired = Boolean.valueOf(configs.get(Config.ProjectInviteRequired.key()));
        _invitationTimeOut = Long.valueOf(configs.get(Config.ProjectInvitationExpirationTime.key()));

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
            owner = _accountMgr.finalizeOwner(caller, accountName, domainId, null);
        }
        
        //don't allow 2 projects with the same name inside the same domain
        if (_projectDao.findByNameAndDomain(name, owner.getDomainId()) != null) {
            throw new InvalidParameterValueException("Project with name " + name + " already exists in domain id=" + owner.getDomainId());
        }
        
        //do resource limit check
        _resourceLimitMgr.checkResourceLimit(owner, ResourceType.project);
        
        Transaction txn = Transaction.currentTxn();
        txn.start();
        
        //Create an account associated with the project
        StringBuilder acctNm = new StringBuilder("PrjAcct-");
        acctNm.append(name).append("-").append(owner.getDomainId());
        
        Account projectAccount = _accountMgr.createAccount(acctNm.toString(), Account.ACCOUNT_TYPE_PROJECT, domainId, null);
        
        Project project = _projectDao.persist(new ProjectVO(name, displayText, owner.getDomainId(), projectAccount.getId()));
        
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
        
        _accountMgr.checkAccess(caller, _domainDao.findById(project.getDomainId()), AccessType.ModifyProject);
        
        //mark project as inactive first, so you can't add resources to it
        Transaction txn = Transaction.currentTxn();
        txn.start();
        s_logger.debug("Marking project id=" + projectId + " with state " + State.Inactive + " as a part of project delete...");
        project.setState(State.Inactive);
        boolean updateResult = _projectDao.update(projectId, project);
        _resourceLimitMgr.decrementResourceCount(project.getProjectAccountId(), ResourceType.project);
        txn.commit();
        
        if (updateResult) {
            if (!cleanupProject(project, null, null)) {
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
    
    private boolean cleanupProject(Project project, AccountVO caller, Long callerUserId) {
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
        
        //Delete project's account
        AccountVO account = _accountDao.findById(project.getProjectAccountId());
        s_logger.debug("Deleting projects " + project + " internal account id=" + account.getId() + " as a part of project cleanup...");
        
        result = result && _accountMgr.deleteAccount(account, callerUserId, caller);
        
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

                _accountMgr.checkAccess(caller, domain, null);

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
    @ActionEvent(eventType = EventTypes.EVENT_PROJECT_ACCOUNT_ADD, eventDescription = "adding account to project")
    public ProjectAccount assignAccountToProject(Project project, long accountId, ProjectAccount.Role accountRole) {
        return _projectAccountDao.persist(new ProjectAccountVO(project, accountId, accountRole));
    }
    
    @Override
    public boolean deleteAccountFromProject(long projectId, long accountId) {
        ProjectAccountVO projectAccount = _projectAccountDao.findByProjectIdAccountId(projectId, accountId);
        return _projectAccountDao.remove(projectAccount.getId());
    }
    
    @Override
    public Account getProjectOwner(long projectId) {
        long accountId = _projectAccountDao.getProjectOwner(projectId).getAccountId();
        return _accountMgr.getAccount(accountId);
    }
    
    @Override
    public ProjectVO findByProjectAccountId(long projectAccountId) {
        return _projectDao.findByProjectAccountId(projectAccountId);
    }
    
    @Override
    public Project findByNameAndDomainId(String name, long domainId) {
        return _projectDao.findByNameAndDomain(name, domainId);
    }
    
    @Override
    public boolean canAccessProjectAccount(Account caller, long accountId) {
        //ROOT admin always can access the project
        if (caller.getType() == Account.ACCOUNT_TYPE_ADMIN) {
            return true;
        } else if (caller.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) {
            Account owner = _accountMgr.getAccount(accountId);
            _accountMgr.checkAccess(caller, _domainDao.findById(owner.getDomainId()), null);
            return true;
        }
        
        return _projectAccountDao.canAccessProjectAccount(caller.getId(), accountId);
    }
    
    public boolean canModifyProjectAccount(Account caller, long accountId) {
        //ROOT admin always can access the project
        if (caller.getType() == Account.ACCOUNT_TYPE_ADMIN) {
            return true;
        } else if (caller.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) {
            Account owner = _accountMgr.getAccount(accountId);
            _accountMgr.checkAccess(caller, _domainDao.findById(owner.getDomainId()), null);
            return true;
        }
        return _projectAccountDao.canModifyProjectAccount(caller.getId(), accountId);
    }
    
    @Override @DB
    @ActionEvent(eventType = EventTypes.EVENT_PROJECT_UPDATE, eventDescription = "updating project")
    public Project updateProject(long projectId, String displayText, String newOwnerName) {
        Account caller = UserContext.current().getCaller();
        
        //check that the project exists
        ProjectVO project = getProject(projectId);
        
        if (project == null) {
            throw new InvalidParameterValueException("Unable to find the project id=" + projectId);
        }
       
        //verify permissions
        _accountMgr.checkAccess(caller, _domainDao.findById(project.getDomainId()), AccessType.ModifyProject);
        
        Transaction txn = Transaction.currentTxn();
        txn.start();
        if (displayText != null) {
            project.setDisplayText(displayText);
            _projectDao.update(projectId, project);
        }
        
        if (newOwnerName != null) {
            //check that the new owner exists
            Account futureOwnerAccount = _accountMgr.getActiveAccountByName(newOwnerName, project.getDomainId());
            if (futureOwnerAccount == null) {
                throw new InvalidParameterValueException("Unable to find account name=" + newOwnerName + " in domain id=" + project.getDomainId());
            }
            Account currentOwnerAccount = getProjectOwner(projectId);
            if (currentOwnerAccount.getId() != futureOwnerAccount.getId()) {
                ProjectAccountVO futureOwner = _projectAccountDao.findByProjectIdAccountId(projectId, futureOwnerAccount.getAccountId());
                if (futureOwner == null) {
                    throw new InvalidParameterValueException("Account " + newOwnerName + " doesn't belong to the project. Add it to the project first and then change the project's ownership");
                }
                
                //unset the role for the old owner
                ProjectAccountVO currentOwner = _projectAccountDao.findByProjectIdAccountId(projectId, currentOwnerAccount.getId());
                currentOwner.setAccountRole(Role.Regular);
                _projectAccountDao.update(currentOwner.getId(), currentOwner);
                
                //set new owner
                futureOwner.setAccountRole(Role.Owner);
                _projectAccountDao.update(futureOwner.getId(), futureOwner);
                
            } else {
                s_logger.trace("Future owner " + newOwnerName + "is already the owner of the project id=" + projectId);
            }
        }
        
        txn.commit();
        
        return _projectDao.findById(projectId);
        
    }
    
    @Override
    public boolean addAccountToProject(long projectId, String accountName) {
        Account caller = UserContext.current().getCaller();
        
        //check that the project exists
        Project project = getProject(projectId);
        
        if (project == null) {
            throw new InvalidParameterValueException("Unable to find the project id=" + projectId);
        }
       
        //check that account-to-add exists
        Account account = _accountMgr.getActiveAccountByName(accountName, project.getDomainId());
        if (account == null) {
            throw new InvalidParameterValueException("Unable to find account name=" + accountName + " in domain id=" + project.getDomainId());
        }
        
        //verify permissions
        _accountMgr.checkAccess(caller, _domainDao.findById(project.getDomainId()), AccessType.ModifyProject);
        
        //Check if the account already added to the project
        ProjectAccount projectAccount =  _projectAccountDao.findByProjectIdAccountId(projectId, account.getId());
        if (projectAccount != null) {
            s_logger.debug("Account " + accountName + " already added to the project id=" + projectId);
            return true;
        }
        
        if (_invitationRequired) {
            //TODO - token based registration
            if (generateInvitation(projectId, account.getId()) != null) {
                return true;
            } else {
                s_logger.warn("Failed to generate invitation for account " + accountName + " to project id=" + projectId);
                return false;
            }
            
        } else {
            if (assignAccountToProject(project, account.getId(), ProjectAccount.Role.Regular) != null) {
                return true;
            } else {
                s_logger.warn("Failed to add account " + accountName + " to project id=" + projectId);
                return false;
            }
        }
    }
    
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_PROJECT_ACCOUNT_REMOVE, eventDescription = "removing account from project")
    public boolean deleteAccountFromProject(long projectId, String accountName) {
        Account caller = UserContext.current().getCaller();
        
        //check that the project exists
        Project project = getProject(projectId);
        
        if (project == null) {
            throw new InvalidParameterValueException("Unable to find the project id=" + projectId);
        }
       
        //check that account-to-remove exists
        Account account = _accountMgr.getActiveAccountByName(accountName, project.getDomainId());
        if (account == null) {
            throw new InvalidParameterValueException("Unable to find account name=" + accountName + " in domain id=" + project.getDomainId());
        }
        
        //verify permissions
        _accountMgr.checkAccess(caller, _domainDao.findById(project.getDomainId()), AccessType.ModifyProject);
        
        //Check if the account exists in the project
        ProjectAccount projectAccount =  _projectAccountDao.findByProjectIdAccountId(projectId, account.getId());
        if (projectAccount == null) {
            throw new InvalidParameterValueException("Account " + accountName + " is not assigned to the project id=" + projectId);
        }
        
        //can't remove the owner of the project
        if (projectAccount.getAccountRole() == Role.Owner) {
            throw new InvalidParameterValueException("Unable to delete account " + accountName + " from the project id=" + projectId + " as the account is the owner of the project");
        }
        
        return deleteAccountFromProject(projectId, account.getId());
    }
    
    
    @Override
    public List<? extends ProjectAccount> listProjectAccounts(long projectId, String accountName, String role, Long startIndex, Long pageSizeVal) {
        Account caller = UserContext.current().getCaller();
        
        //check that the project exists
        Project project = getProject(projectId);
        
        if (project == null) {
            throw new InvalidParameterValueException("Unable to find the project id=" + projectId);
        }
        
        //verify permissions
        _accountMgr.checkAccess(caller, _domainDao.findById(project.getDomainId()), null);
        
        Filter searchFilter = new Filter(ProjectAccountVO.class, "id", false, startIndex, pageSizeVal);
        SearchBuilder<ProjectAccountVO> sb = _projectAccountDao.createSearchBuilder();
        sb.and("accountRole", sb.entity().getAccountRole(), Op.EQ);
        
        SearchBuilder<AccountVO> accountSearch;
        if (accountName != null) {
            accountSearch = _accountDao.createSearchBuilder();
            accountSearch.and("accountName", accountSearch.entity().getAccountName(), SearchCriteria.Op.EQ);
            sb.join("accountSearch", accountSearch, sb.entity().getAccountId(), accountSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        }
        
        SearchCriteria<ProjectAccountVO> sc = sb.create();
        
        if (role != null) {
            sc.setParameters("accountRole", role);
        }
        
        if (accountName != null) {
            sc.setJoinParameters("accountSearch", "accountName", accountName);
        }
        
        return _projectAccountDao.search(sc, searchFilter);
    }
    
    public ProjectInvitation generateInvitation(long projectId, Long accountId) {
        //verify if the invitation was already generated
        ProjectInvitationVO invite = _projectInvitationDao.findPendingByAccountIdProjectId(accountId, projectId);
        
        if (invite != null) {
            if (_projectInvitationDao.isActive(invite.getId(), _invitationTimeOut)) {
                throw new InvalidParameterValueException("There is already a pending invitation for account id=" + accountId + " to the project id=" + projectId);
            } else {
                if (invite.getState() == ProjectInvitation.State.Pending) {
                    expireInvitation(invite);
                }
            }
        }
        
        return _projectInvitationDao.persist(new ProjectInvitationVO(projectId, accountId, _accountMgr.getAccount(accountId).getDomainId(), null, null));
    }
    
    private boolean expireInvitation(ProjectInvitationVO invite) {
        s_logger.debug("Expiring invitation id=" + invite.getId());
        invite.setState(ProjectInvitation.State.Expired);
        return _projectInvitationDao.update(invite.getId(), invite);
    }
    
    @Override
    public List<? extends ProjectInvitation> listProjectInvitations(Long projectId, String accountName, Long domainId, String state, boolean activeOnly, Long startIndex, Long pageSizeVal) {
        Account caller = UserContext.current().getCaller();
        Long accountId = null;
        String domainPath = null;
        
        if (caller.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) {
            if (domainId == null) {
                domainPath = _domainMgr.getDomain(caller.getDomainId()).getPath();
            } 
        } else if (caller.getType() == Account.ACCOUNT_TYPE_NORMAL){
            // regular user is constraint to only his account
            accountId = caller.getId();
        }
        
        if (domainId != null) {
            Domain domain = _domainDao.findById(domainId);
            if (domain == null) {
                throw new InvalidParameterValueException("Domain id=" + domainId + " doesn't exist");
            }
            _accountMgr.checkAccess(caller, domain, null);

            if (accountName != null) {
                Account account = _accountDao.findActiveAccount(accountName, domainId);
                if (account == null) {
                    throw new InvalidParameterValueException("Unable to find account by name " + accountName + " in domain " + domainId);
                }

                _accountMgr.checkAccess(caller, null, account);
                accountId = account.getId();
            }
        }
        
        Filter searchFilter = new Filter(ProjectInvitationVO.class, "id", true, startIndex, pageSizeVal);
        
        SearchBuilder<ProjectInvitationVO> sb = _projectInvitationDao.createSearchBuilder();
        sb.and("accountId", sb.entity().getAccountId(), SearchCriteria.Op.EQ);
        sb.and("domainId", sb.entity().getDomainId(), SearchCriteria.Op.EQ);
        sb.and("projectId", sb.entity().getProjectId(), SearchCriteria.Op.EQ);
        sb.and("state", sb.entity().getState(), SearchCriteria.Op.EQ);
        sb.and("created", sb.entity().getCreated(), SearchCriteria.Op.GT);

        if (domainPath != null) {
            // do a domain LIKE match for the admin case if isRecursive is true
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
            sb.join("domainSearch", domainSearch, sb.entity().getDomainId(), domainSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        }
        
        SearchCriteria<ProjectInvitationVO> sc = sb.create();
        
        if (domainPath != null) {
            sc.setJoinParameters("domainSearch", "path", domainPath);
        }
        
        if (accountId != null) {
            sc.setParameters("accountId", accountId);
        }
        
        if (projectId != null){
            sc.setParameters("projectId", projectId);
        }
        
        if (state != null) {
            sc.setParameters("state", state);
        }
        
        if (activeOnly) {
            sc.setParameters("state", ProjectInvitation.State.Pending);
            sc.setParameters("created", new Date((System.currentTimeMillis() >> 10) - _invitationTimeOut));
        }
        
        return _projectInvitationDao.search(sc, searchFilter);
    }
    
    @Override @DB
    public boolean joinProject(long projectId, String accountName) {
        Account caller = UserContext.current().getCaller();
        Long accountId = null;
        boolean result = true;
        
        //check that the project exists
        Project project = getProject(projectId);
        
        if (project == null) {
            throw new InvalidParameterValueException("Unable to find the project id=" + projectId);
        }
        
        if (accountName != null) {
            //check that account-to-remove exists
            Account account = _accountMgr.getActiveAccountByName(accountName, project.getDomainId());
            if (account == null) {
                throw new InvalidParameterValueException("Unable to find account name=" + accountName + " in domain id=" + project.getDomainId());
            }
            
            //verify permissions
            _accountMgr.checkAccess(caller, _domainDao.findById(project.getDomainId()), AccessType.ModifyProject);
            accountId = account.getId();
        } else {
            accountId = caller.getId();
        }
        
        //check that invitation exists
        ProjectInvitationVO invite = _projectInvitationDao.findPendingByAccountIdProjectId(accountId, projectId);
        if (invite != null) {
            if (!_projectInvitationDao.isActive(invite.getId(), _invitationTimeOut)) {
                expireInvitation(invite);
                throw new InvalidParameterValueException("Invitation is expired for account id=" + accountName + " to the project id=" + projectId);
            } else {
                Transaction txn = Transaction.currentTxn();
                txn.start();
               //complete invitation
               s_logger.debug("Marking invitation " + invite + " with state " + ProjectInvitation.State.Completed);
               invite.setState(ProjectInvitation.State.Completed);
               result = _projectInvitationDao.update(invite.getId(), invite);
               
               if (result) {
                   //check if account already exists for the project (was added before invitation got accepted)
                   ProjectAccount projectAccount =  _projectAccountDao.findByProjectIdAccountId(projectId, accountId);
                   if (projectAccount != null) {
                       s_logger.debug("Account " + accountName + " already added to the project id=" + projectId);
                   } else {
                       assignAccountToProject(project, accountId, ProjectAccount.Role.Regular); 
                   }
               } else {
                   s_logger.warn("Failed to update project invitation " + invite + " with state " + ProjectInvitation.State.Completed);
               }
              
               txn.commit();
            }
        } else {
            throw new InvalidParameterValueException("Unable to find invitation for account id=" + accountName + " to the project id=" + projectId);
        }
        
        return result;
    }
    
    @Override
    public List<Long> listPermittedProjectAccounts(long accountId) {
        return _projectAccountDao.listPermittedAccountIds(accountId);
    }
}
