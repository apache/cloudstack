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
import com.cloud.domain.Domain;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.PermissionDeniedException;
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
    
    protected DomainChecker() {
        super();
    }
    
    @Override
    public boolean checkAccess(Account account, Domain domain) throws PermissionDeniedException {
        if (!account.getState().equals(Account.ACCOUNT_STATE_ENABLED)) {
            throw new PermissionDeniedException(account + " is disabled.");
        }
        
        if (!_domainDao.isChildDomain(account.getDomainId(), domain.getId())) {
            throw new PermissionDeniedException(account + " does not have permission to operate within " + domain);
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
    public boolean checkAccess(Account account, ControlledEntity entity) throws PermissionDeniedException {
        if (entity instanceof VirtualMachineTemplate) {
            
            VirtualMachineTemplate template = (VirtualMachineTemplate)entity;
            
            // validate that the template is usable by the account
            if (!template.isPublicTemplate()) {
                Account owner = _accountDao.findById(template.getAccountId());
                if (BaseCmd.isAdmin(owner.getType()) || (owner.getId() != account.getId())) {
                    return true;
                }
                
                // since the current account is not the owner of the template, check the launch permissions table to see if the
                // account can launch a VM from this template
                LaunchPermissionVO permission = _launchPermissionDao.findByTemplateAndAccount(template.getId(), account.getId());
                if (permission == null) {
                    throw new PermissionDeniedException(account + " does not have permission to launch instances from " + template);
                }
            }
            
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean checkAccess(User user, ControlledEntity entity) throws PermissionDeniedException {
        Account account = _accountDao.findById(user.getAccountId());
        return checkAccess(account, entity);
    }
}
