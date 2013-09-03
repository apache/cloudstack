// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.acl;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.affinity.AffinityGroup;
import org.apache.cloudstack.api.BaseCmd;
import org.springframework.stereotype.Component;

import com.cloud.dc.DataCenter;
import com.cloud.dc.DedicatedResourceVO;
import com.cloud.dc.dao.DedicatedResourceDao;
import com.cloud.domain.Domain;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.projects.ProjectManager;
import com.cloud.projects.dao.ProjectAccountDao;
import com.cloud.storage.LaunchPermissionVO;
import com.cloud.storage.dao.LaunchPermissionDao;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.component.AdapterBase;

@Component
@Local(value = SecurityChecker.class)
public class DomainChecker extends AdapterBase implements SecurityChecker {
    
    @Inject DomainDao _domainDao;
    @Inject AccountDao _accountDao;
    @Inject LaunchPermissionDao _launchPermissionDao;
    @Inject ProjectManager _projectMgr;
    @Inject ProjectAccountDao _projecAccountDao;
    @Inject NetworkModel _networkMgr;
    @Inject
    private DedicatedResourceDao _dedicatedDao;
    
    protected DomainChecker() {
        super();
    }
    
    @Override
    public boolean checkAccess(Account caller, Domain domain) throws PermissionDeniedException {
        if (caller.getState() != Account.State.enabled) {
            throw new PermissionDeniedException(caller + " is disabled.");
        }
        long domainId = domain.getId();
        
        if (caller.getType() == Account.ACCOUNT_TYPE_NORMAL) {
            if (caller.getDomainId() != domainId) {
                throw new PermissionDeniedException(caller + " does not have permission to operate within domain id=" + domain.getId());
            }
        } else if (!_domainDao.isChildDomain(caller.getDomainId(), domainId)) {
            throw new PermissionDeniedException(caller + " does not have permission to operate within domain id=" + domain.getId());
        }
        
        return true;
    }

    @Override
    public boolean checkAccess(User user, Domain domain) throws PermissionDeniedException {
        if (user.getRemoved() != null) {
            throw new PermissionDeniedException(user + " is no longer active.");
        }
        Account account = _accountDao.findById(user.getAccountId());
        return checkAccess(account, domain);
    }

    @Override
    public boolean checkAccess(Account caller, ControlledEntity entity, AccessType accessType) throws PermissionDeniedException {
        if (entity instanceof VirtualMachineTemplate) {
            
            VirtualMachineTemplate template = (VirtualMachineTemplate) entity;
            Account owner = _accountDao.findById(template.getAccountId());
            // validate that the template is usable by the account
            if (!template.isPublicTemplate()) {
                if (BaseCmd.isRootAdmin(caller.getType()) || (owner.getId() == caller.getId())) {
                    return true;
                }
                //special handling for the project case
                if (owner.getType() == Account.ACCOUNT_TYPE_PROJECT && _projectMgr.canAccessProjectAccount(caller, owner.getId())) {
                    return true;
                }
                
                // since the current account is not the owner of the template, check the launch permissions table to see if the
                // account can launch a VM from this template
                LaunchPermissionVO permission = _launchPermissionDao.findByTemplateAndAccount(template.getId(), caller.getId());
                if (permission == null) {
                    throw new PermissionDeniedException(caller + " does not have permission to launch instances from " + template);
                }
            } else {
                // Domain admin and regular user can delete/modify only templates created by them
                if (accessType != null && accessType == AccessType.ModifyEntry) {
                    if (!BaseCmd.isRootAdmin(caller.getType()) && owner.getId() != caller.getId()) {
                        // For projects check if the caller account can access the project account
                        if (owner.getType() != Account.ACCOUNT_TYPE_PROJECT || !(_projectMgr.canAccessProjectAccount(caller, owner.getId()))) {
                            throw new PermissionDeniedException("Domain Admin and regular users can modify only their own Public templates");
                        }
                    }
                }
            }
            
            return true;
        } else if (entity instanceof Network && accessType != null && accessType == AccessType.UseNetwork) {
            _networkMgr.checkNetworkPermissions(caller, (Network) entity);
        } else if (entity instanceof AffinityGroup) {
            return false;
        } else {
            if (caller.getType() == Account.ACCOUNT_TYPE_NORMAL) {
                Account account = _accountDao.findById(entity.getAccountId());
                
                if (account != null && account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
                    //only project owner can delete/modify the project
                    if (accessType != null && accessType == AccessType.ModifyProject) {
                        if (!_projectMgr.canModifyProjectAccount(caller, account.getId())) {
                            throw new PermissionDeniedException(caller + " does not have permission to operate with resource " + entity);
                        }
                    } else if (!_projectMgr.canAccessProjectAccount(caller, account.getId())) {
                        throw new PermissionDeniedException(caller + " does not have permission to operate with resource " + entity);
                    }
                } else {
                    if (caller.getId() != entity.getAccountId()) {
                        throw new PermissionDeniedException(caller + " does not have permission to operate with resource " + entity);
                    }
                }  
            }
        }
        
        return true;
    }

    @Override
    public boolean checkAccess(User user, ControlledEntity entity) throws PermissionDeniedException {
        Account account = _accountDao.findById(user.getAccountId());
        return checkAccess(account, entity, null);
    }

	@Override
    public boolean checkAccess(Account account, DiskOffering dof) throws PermissionDeniedException {
        if (account == null || dof.getDomainId() == null) {//public offering
			return true;
        } else {
			//admin has all permissions
            if (account.getType() == Account.ACCOUNT_TYPE_ADMIN) {
				return true;
			}		
			//if account is normal user or domain admin
			//check if account's domain is a child of zone's domain (Note: This is made consistent with the list command for disk offering)
            else if (account.getType() == Account.ACCOUNT_TYPE_NORMAL || account.getType() == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN || account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) {
                if (account.getDomainId() == dof.getDomainId()) {
					return true; //disk offering and account at exact node
                } else {
                    Domain domainRecord = _domainDao.findById(account.getDomainId());
                    if (domainRecord != null) {
                        while (true) {
                            if (domainRecord.getId() == dof.getDomainId()) {
		    					//found as a child
		    					return true;
		    				}
                            if (domainRecord.getParent() != null) {
                                domainRecord = _domainDao.findById(domainRecord.getParent());
                            } else {
                                break;
                            }
		    			}
		    		}
				}
			}
		}
		//not found
		return false;
	}	

	@Override
    public boolean checkAccess(Account account, ServiceOffering so) throws PermissionDeniedException {
        if (account == null || so.getDomainId() == null) {//public offering
			return true;
        } else {
			//admin has all permissions
            if (account.getType() == Account.ACCOUNT_TYPE_ADMIN) {
				return true;
			}		
			//if account is normal user or domain admin
			//check if account's domain is a child of zone's domain (Note: This is made consistent with the list command for service offering)
            else if (account.getType() == Account.ACCOUNT_TYPE_NORMAL || account.getType() == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN || account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) {
                if (account.getDomainId() == so.getDomainId()) {
					return true; //service offering and account at exact node
                } else {
                    Domain domainRecord = _domainDao.findById(account.getDomainId());
                    if (domainRecord != null) {
                        while (true) {
                            if (domainRecord.getId() == so.getDomainId()) {
		    					//found as a child
		    					return true;
		    				}
                            if (domainRecord.getParent() != null) {
                                domainRecord = _domainDao.findById(domainRecord.getParent());
                            } else {
                                break;
                            }
		    			}
		    		}
				}
			}
		}
		//not found
		return false;
	}	
    
	@Override
	public boolean checkAccess(Account account, DataCenter zone) throws PermissionDeniedException {
        if (account == null || zone.getDomainId() == null) {//public zone
			return true;
        } else {
			//admin has all permissions
            if (account.getType() == Account.ACCOUNT_TYPE_ADMIN) {
				return true;
			}		
			//if account is normal user
			//check if account's domain is a child of zone's domain
            else if (account.getType() == Account.ACCOUNT_TYPE_NORMAL || account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
                // if zone is dedicated to an account check that the accountId
                // matches.
                DedicatedResourceVO dedicatedZone = _dedicatedDao.findByZoneId(zone.getId());
                if (dedicatedZone != null) {
                    if (dedicatedZone.getAccountId() != null) {
                        if (dedicatedZone.getAccountId() == account.getId()) {
                            return true;
                        } else {
                            return false;
                        }
                    }
                }
                if (account.getDomainId() == zone.getDomainId()) {
					return true; //zone and account at exact node
                } else {
                    Domain domainRecord = _domainDao.findById(account.getDomainId());
                    if (domainRecord != null) {
                        while (true) {
                            if (domainRecord.getId() == zone.getDomainId()) {
		    					//found as a child
		    					return true;
		    				}
                            if (domainRecord.getParent() != null) {
                                domainRecord = _domainDao.findById(domainRecord.getParent());
                            } else {
                                break;
                            }
		    			}
		    		}
				}
				//not found
				return false;
			}
			//if account is domain admin
			//check if the account's domain is either child of zone's domain, or if zone's domain is child of account's domain
            else if (account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) {
                if (account.getDomainId() == zone.getDomainId()) {
					return true; //zone and account at exact node
                } else {
                    Domain zoneDomainRecord = _domainDao.findById(zone.getDomainId());
                    Domain accountDomainRecord = _domainDao.findById(account.getDomainId());
                    if (accountDomainRecord != null) {
                        Domain localRecord = accountDomainRecord;
                        while (true) {
                            if (localRecord.getId() == zone.getDomainId()) {
		    					//found as a child
		    					return true;
		    				}
                            if (localRecord.getParent() != null) {
                                localRecord = _domainDao.findById(localRecord.getParent());
                            } else {
                                break;
                            }
		    			}
		    		}
		    		//didn't find in upper tree
                    if (zoneDomainRecord.getPath().contains(accountDomainRecord.getPath())) {
		    			return true;
		    		}
				}
				//not found
				return false;
			}
		}
		return false;
	}
}
