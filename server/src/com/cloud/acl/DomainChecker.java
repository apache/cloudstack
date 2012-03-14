/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
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
package com.cloud.acl;

import javax.ejb.Local;

import com.cloud.api.BaseCmd;
import com.cloud.dc.DataCenter;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.network.Network;
import com.cloud.network.NetworkManager;
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
import com.cloud.utils.component.Inject;

@Local(value=SecurityChecker.class)
public class DomainChecker extends AdapterBase implements SecurityChecker {
    
    @Inject DomainDao _domainDao;
    @Inject AccountDao _accountDao;
    @Inject LaunchPermissionDao _launchPermissionDao;
    @Inject ProjectManager _projectMgr;
    @Inject ProjectAccountDao _projecAccountDao;
    @Inject NetworkManager _networkMgr;
    
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
            
            VirtualMachineTemplate template = (VirtualMachineTemplate)entity;
            Account owner = _accountDao.findById(template.getAccountId());
            // validate that the template is usable by the account
            if (!template.isPublicTemplate()) {
                if (BaseCmd.isRootAdmin(caller.getType()) || (owner.getId() == caller.getId())) {
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
                        throw new PermissionDeniedException("Domain Admin and regular users can modify only their own Public templates");
                    }
                }
            }
            
            return true;
        } else if (entity instanceof Network && accessType != null && accessType == AccessType.UseNetwork) {
        	_networkMgr.checkNetworkPermissions(caller, (Network)entity);
        } else {
            if (caller.getType() == Account.ACCOUNT_TYPE_NORMAL) {
                Account account = _accountDao.findById(entity.getAccountId());
                
                if (account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
                    //only project owner can delete/modify the project
                    if (accessType != null && accessType == AccessType.ModifyProject) {
                        if (!_projectMgr.canModifyProjectAccount(caller, account.getId())) {
                            throw new PermissionDeniedException(caller + " does not have permission to operate with resource " + entity);
                        }
                    } else if (!_projectMgr.canAccessProjectAccount(caller, account.getId())){
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
	public boolean checkAccess(Account account, DiskOffering dof) throws PermissionDeniedException 
	{
		if(account == null || dof.getDomainId() == null)
		{//public offering
			return true;
		}
		else
		{
			//admin has all permissions
			if(account.getType() == Account.ACCOUNT_TYPE_ADMIN)
			{
				return true;
			}		
			//if account is normal user or domain admin
			//check if account's domain is a child of zone's domain (Note: This is made consistent with the list command for disk offering)
			else if(account.getType() == Account.ACCOUNT_TYPE_NORMAL || account.getType() == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN || account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN)
			{
				if(account.getDomainId() == dof.getDomainId())
				{
					return true; //disk offering and account at exact node
				}
				else
				{
		    		DomainVO domainRecord = _domainDao.findById(account.getDomainId());
		    		if(domainRecord != null)
		    		{
		    			while(true)
		    			{
		    				if(domainRecord.getId() == dof.getDomainId())
		    				{
		    					//found as a child
		    					return true;
		    				}
		    				if(domainRecord.getParent() != null) {
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
	public boolean checkAccess(Account account, ServiceOffering so) throws PermissionDeniedException 
	{
		if(account == null || so.getDomainId() == null)
		{//public offering
			return true;
		}
		else
		{
			//admin has all permissions
			if(account.getType() == Account.ACCOUNT_TYPE_ADMIN)
			{
				return true;
			}		
			//if account is normal user or domain admin
			//check if account's domain is a child of zone's domain (Note: This is made consistent with the list command for service offering)
			else if(account.getType() == Account.ACCOUNT_TYPE_NORMAL || account.getType() == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN || account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN)
			{
				if(account.getDomainId() == so.getDomainId())
				{
					return true; //service offering and account at exact node
				}
				else
				{
		    		DomainVO domainRecord = _domainDao.findById(account.getDomainId());
		    		if(domainRecord != null)
		    		{
		    			while(true)
		    			{
		    				if(domainRecord.getId() == so.getDomainId())
		    				{
		    					//found as a child
		    					return true;
		    				}
		    				if(domainRecord.getParent() != null) {
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
		if(account == null || zone.getDomainId() == null){//public zone
			return true;
		}else{
			//admin has all permissions
			if(account.getType() == Account.ACCOUNT_TYPE_ADMIN){
				return true;
			}		
			//if account is normal user
			//check if account's domain is a child of zone's domain
			else if(account.getType() == Account.ACCOUNT_TYPE_NORMAL || account.getType() == Account.ACCOUNT_TYPE_PROJECT){
				if(account.getDomainId() == zone.getDomainId()){
					return true; //zone and account at exact node
				}else{
		    		DomainVO domainRecord = _domainDao.findById(account.getDomainId());
		    		if(domainRecord != null)
		    		{
		    			while(true){
		    				if(domainRecord.getId() == zone.getDomainId()){
		    					//found as a child
		    					return true;
		    				}
		    				if(domainRecord.getParent() != null) {
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
			else if(account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN){
				if(account.getDomainId() == zone.getDomainId()){
					return true; //zone and account at exact node
				}else{
					DomainVO zoneDomainRecord = _domainDao.findById(zone.getDomainId());
		    		DomainVO accountDomainRecord = _domainDao.findById(account.getDomainId());
		    		if(accountDomainRecord != null)
		    		{
		    			DomainVO localRecord = accountDomainRecord;
		    			while(true){
		    				if(localRecord.getId() == zone.getDomainId()){
		    					//found as a child
		    					return true;
		    				}
		    				if(localRecord.getParent() != null) {
                                localRecord = _domainDao.findById(localRecord.getParent());
                            } else {
                                break;
                            }
		    			}
		    		}
		    		//didn't find in upper tree
		    		if(zoneDomainRecord.getPath().contains(accountDomainRecord.getPath())){
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
