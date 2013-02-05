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
package org.apache.cloudstack.region;

import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.command.admin.account.DeleteAccountCmd;
import org.apache.cloudstack.api.command.admin.account.DisableAccountCmd;
import org.apache.cloudstack.api.command.admin.account.EnableAccountCmd;
import org.apache.cloudstack.api.command.admin.account.UpdateAccountCmd;
import org.apache.cloudstack.api.command.admin.domain.DeleteDomainCmd;
import org.apache.cloudstack.api.command.admin.domain.UpdateDomainCmd;
import org.apache.cloudstack.api.command.admin.user.DeleteUserCmd;
import org.apache.cloudstack.api.command.admin.user.DisableUserCmd;
import org.apache.cloudstack.api.command.admin.user.EnableUserCmd;
import org.apache.cloudstack.api.command.admin.user.UpdateUserCmd;
import org.apache.cloudstack.api.command.user.region.ListRegionsCmd;
import org.apache.cloudstack.region.dao.RegionDao;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.domain.Domain;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.DomainManager;
import com.cloud.user.UserAccount;
import com.cloud.user.UserContext;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.component.Manager;
import com.cloud.utils.component.ManagerBase;

@Component
@Local(value = { RegionService.class })
public class RegionServiceImpl extends ManagerBase implements RegionService, Manager {
    public static final Logger s_logger = Logger.getLogger(RegionServiceImpl.class);
    
    @Inject
    private RegionDao _regionDao;
    @Inject
    private AccountDao _accountDao;
    @Inject
    private UserDao _userDao;
    @Inject
    private DomainDao _domainDao;    
    @Inject
    private RegionManager _regionMgr;
    @Inject
    private AccountManager _accountMgr;
    @Inject
    private DomainManager _domainMgr;
    
    private String _name;
    
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

    /**
     * {@inheritDoc}
     */    
	@Override
	public Region addRegion(int id, String name, String endPoint, String apiKey, String secretKey) {
		//Check for valid Name
		//Check valid end_point url
		return _regionMgr.addRegion(id, name, endPoint, apiKey, secretKey);
	}

    /**
     * {@inheritDoc}
     */ 
	@Override
	public Region updateRegion(int id, String name, String endPoint, String apiKey, String secretKey) {
		//Check for valid Name
		//Check valid end_point url		
		return _regionMgr.updateRegion(id, name, endPoint, apiKey, secretKey);
	}

    /**
     * {@inheritDoc}
     */ 
	@Override
	public boolean removeRegion(int id) {
		return _regionMgr.removeRegion(id);
	}

    /**
     * {@inheritDoc}
     */ 
	@Override
	public List<? extends Region> listRegions(ListRegionsCmd cmd) {
		return _regionMgr.listRegions(cmd.getId(), cmd.getName());
	}

    /**
     * {@inheritDoc}
     */ 
	@Override
	public boolean deleteUserAccount(DeleteAccountCmd cmd) {
        boolean result = false;
        if(checkIsPropagate(cmd.getIsPropagate())){
        	result = _accountMgr.deleteUserAccount(cmd.getId());
        } else {
        	result = _regionMgr.deleteUserAccount(cmd.getId());
        }
		return result;
	}
	
    /**
     * {@inheritDoc}
     */ 	
	@Override
	public Account updateAccount(UpdateAccountCmd cmd) {
    	Account result = null;
    	if(checkIsPropagate(cmd.getIsPropagate())){
    		result = _accountMgr.updateAccount(cmd);
        } else {
        	result = _regionMgr.updateAccount(cmd);
        }
		
		return result;
	}

    /**
     * {@inheritDoc}
     */ 
	@Override
	public Account disableAccount(DisableAccountCmd cmd) throws ConcurrentOperationException, ResourceUnavailableException {
    	Account result = null;
    	if(checkIsPropagate(cmd.getIsPropagate())){
    		if(cmd.getLockRequested())
    			result = _accountMgr.lockAccount(cmd.getAccountName(), cmd.getDomainId(), cmd.getId());
    		else
    			result = _accountMgr.disableAccount(cmd.getAccountName(), cmd.getDomainId(), cmd.getId());
    	} else {
    		result = _regionMgr.disableAccount(cmd.getAccountName(), cmd.getDomainId(), cmd.getId(), cmd.getLockRequested());
    	}
		return result;
	}

    /**
     * {@inheritDoc}
     */ 	
	@Override
	public Account enableAccount(EnableAccountCmd cmd) {
    	Account result = null;
    	if(checkIsPropagate(cmd.getIsPropagate())){
    		result = _accountMgr.enableAccount(cmd.getAccountName(), cmd.getDomainId(), cmd.getId());
    	} else {
    		result = _regionMgr.enableAccount(cmd.getAccountName(), cmd.getDomainId(), cmd.getId());
    	}
		return result;
	}

    /**
     * {@inheritDoc}
     */ 
	@Override
	public boolean deleteUser(DeleteUserCmd cmd) {
        boolean result = false;
        if(checkIsPropagate(cmd.getIsPropagate())){
        	result = _accountMgr.deleteUser(cmd);
        } else {
        	result = _regionMgr.deleteUser(cmd);
        }		
		return result;
	}
	
    /**
     * {@inheritDoc}
     */ 	
	@Override
	public Domain updateDomain(UpdateDomainCmd cmd) {
        Domain domain = null;
        if(checkIsPropagate(cmd.getIsPropagate())){
        	domain = _domainMgr.updateDomain(cmd);
        } else {
        	domain = _regionMgr.updateDomain(cmd);
        }		
		return domain;
	}	

    /**
     * {@inheritDoc}
     */ 
	@Override
	public boolean deleteDomain(DeleteDomainCmd cmd) {
		boolean result = false;
		if(checkIsPropagate(cmd.isPropagate())){
			result = _domainMgr.deleteDomain(cmd.getId(), cmd.getCleanup());
		} else {
			result = _regionMgr.deleteDomain(cmd.getId(), cmd.getCleanup());
		}
		return result;
	}

    /**
     * {@inheritDoc}
     */ 	
	@Override
	public UserAccount updateUser(UpdateUserCmd cmd){
        UserAccount user = null;
        if(checkIsPropagate(cmd.getIsPropagate())){
        	user = _accountMgr.updateUser(cmd);
        } else {
        	user = _regionMgr.updateUser(cmd);
        }		
		return user;
	}

    /**
     * {@inheritDoc}
     */ 
	@Override
	public UserAccount disableUser(DisableUserCmd cmd) {
        UserAccount user = null;
        if(checkIsPropagate(cmd.getIsPropagate())){
    		user = _accountMgr.disableUser(cmd.getId());
        } else {
        	user = _regionMgr.disableUser(cmd.getId());
        }
		return user;
	}

    /**
     * {@inheritDoc}
     */ 
	@Override
	public UserAccount enableUser(EnableUserCmd cmd) {
		UserAccount user = null;
		if(checkIsPropagate(cmd.getIsPropagate())){
			user = _accountMgr.enableUser(cmd.getId());
		} else {
			user = _regionMgr.enableUser(cmd.getId());
		}		
		return user;
	}
	
    private boolean isRootAdmin(short accountType) {
        return (accountType == Account.ACCOUNT_TYPE_ADMIN);
    }
    
    /**
     * Check isPopagate flag, Only ROOT Admin can use this param
     * @param isPopagate
     * @return
     */
    private boolean checkIsPropagate(Boolean isPopagate){
    	if(isPopagate == null || !isPopagate){
    		return false;
    	}
		// Only Admin can use isPopagate flag
    	UserContext ctx = UserContext.current();
    	Account caller = ctx.getCaller();
    	if(!isRootAdmin(caller.getType())){
    		throw new PermissionDeniedException("isPropagate param cannot be used by non ROOT Admin");
    	}          	
    	return true;
    }

}
