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
package com.cloud.region;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.command.admin.account.UpdateAccountCmd;
import org.apache.cloudstack.api.command.admin.domain.UpdateDomainCmd;
import org.apache.cloudstack.api.command.admin.user.DeleteUserCmd;
import org.apache.cloudstack.api.command.admin.user.UpdateUserCmd;
import org.apache.cloudstack.api.command.user.region.ListRegionsCmd;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.log4j.Logger;

import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.region.dao.RegionDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.DomainManager;
import com.cloud.user.UserAccount;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserAccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.component.Inject;
import com.cloud.utils.component.Manager;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.uuididentity.dao.IdentityDao;

@Local(value = { RegionManager.class, RegionService.class })
public class RegionManagerImpl implements RegionManager, RegionService, Manager{
    public static final Logger s_logger = Logger.getLogger(RegionManagerImpl.class);
    
    @Inject
    private RegionDao _regionDao;
    @Inject
    private AccountDao _accountDao;
    @Inject
    private AccountManager _accountMgr;
    @Inject
    private UserDao _userDao;
    @Inject
    private DomainDao _domainDao;
    @Inject
    private DomainManager _domainMgr;
    @Inject
    private UserAccountDao _userAccountDao;    
    @Inject
    private IdentityDao _identityDao;
    
    private String _name;
    private int _id; 
    
    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        _name = name;
        _id = _regionDao.getRegionId();
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
	public boolean propogateAddAccount(String userName, String password, String firstName, String lastName, String email, String timezone, 
			String accountName, short accountType, Long domainId, String networkDomain, Map<String, String> details, String accountUUID, String userUUID) {
		String command = "createAccount";
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new NameValuePair(ApiConstants.USERNAME, userName));
		params.add(new NameValuePair(ApiConstants.PASSWORD, password));
		params.add(new NameValuePair(ApiConstants.FIRSTNAME, firstName));
		params.add(new NameValuePair(ApiConstants.LASTNAME, lastName));
		params.add(new NameValuePair(ApiConstants.EMAIL, email));
		params.add(new NameValuePair(ApiConstants.TIMEZONE, timezone));
		params.add(new NameValuePair(ApiConstants.ACCOUNT, accountName));
		params.add(new NameValuePair(ApiConstants.ACCOUNT_TYPE, ""+accountType));
		//use domain UUID
		params.add(new NameValuePair(ApiConstants.DOMAIN_ID, ((domainId != null) ? domainId.toString() : "")));
		params.add(new NameValuePair(ApiConstants.NETWORK_DOMAIN, networkDomain));
		params.add(new NameValuePair(ApiConstants.ACCOUNT_DETAILS, (details != null) ? details.toString() : ""));
		params.add(new NameValuePair(ApiConstants.ACCOUNT_ID, accountUUID));
		params.add(new NameValuePair(ApiConstants.USER_ID, userUUID));
		params.add(new NameValuePair(ApiConstants.REGION_ID, ""+getId()));

		List<RegionVO> regions =  _regionDao.listAll();
		for (Region region : regions){
			if(region.getId() == getId()){
				continue;
			}
			s_logger.debug("Adding account :"+accountName+" to Region: "+region.getId());
			if (RegionsApiUtil.makeAPICall(region, command, params)) {
				s_logger.debug("Successfully added account :"+accountName+" to Region: "+region.getId());
			} else {
				s_logger.error("Error while Adding account :"+accountName+" to Region: "+region.getId());
			}
		}
		return true;
	}

	@Override
	public boolean deleteUserAccount(long accountId) {
		AccountVO account = _accountDao.findById(accountId);
		if(account == null){
			return false;
		}
		String accountUUID = account.getUuid();
		int regionId = account.getRegionId();
		
		String command = "deleteAccount";
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new NameValuePair(ApiConstants.ID, accountUUID));

		if(getId() == regionId){
			if(_accountMgr.deleteUserAccount(accountId)){
				List<RegionVO> regions =  _regionDao.listAll();
				for (Region region : regions){
					if(region.getId() == getId()){
						continue;
					}
					params.add(new NameValuePair(ApiConstants.IS_PROPAGATE, "true"));
					if (RegionsApiUtil.makeAPICall(region, command, params)) {
						s_logger.debug("Successfully deleted account :"+accountUUID+" in Region: "+region.getId());
					} else {
						s_logger.error("Error while deleting account :"+accountUUID+" in Region: "+region.getId());
					}
				}
				return true;
			} else {
				return false;
			}
		} else {
			//First delete in the Region where account is created
			Region region = _regionDao.findById(regionId);
			if (RegionsApiUtil.makeAPICall(region, command, params)) {
				s_logger.debug("Successfully deleted account :"+accountUUID+" in Region: "+region.getId());
				return true;
			} else {
				s_logger.error("Error while deleting account :"+accountUUID+" in Region: "+region.getId());
				return false;
			}
		}
	}
	
	@Override
	public Account updateAccount(UpdateAccountCmd cmd) {
        Long accountId = cmd.getId();
        Long domainId = cmd.getDomainId();
        DomainVO domain = _domainDao.findById(domainId);
        String accountName = cmd.getAccountName();
        String newAccountName = cmd.getNewName();
        String networkDomain = cmd.getNetworkDomain();
        //ToDo send details
        Map<String, String> details = cmd.getDetails();
        
        Account account = null;
        if (accountId != null) {
            account = _accountDao.findById(accountId);
        } else {
            account = _accountDao.findEnabledAccount(accountName, domainId);
        }

        // Check if account exists
        if (account == null || account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            s_logger.error("Unable to find account by accountId: " + accountId + " OR by name: " + accountName + " in domain " + domainId);
            throw new InvalidParameterValueException("Unable to find account by accountId: " + accountId + " OR by name: " + accountName + " in domain " + domainId);
        }
        
        String command = "updateAccount";
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new NameValuePair(ApiConstants.NEW_NAME, newAccountName));
		params.add(new NameValuePair(ApiConstants.ID, account.getUuid()));
		params.add(new NameValuePair(ApiConstants.ACCOUNT, accountName));
		params.add(new NameValuePair(ApiConstants.DOMAIN_ID, domain.getUuid()));
		params.add(new NameValuePair(ApiConstants.NETWORK_DOMAIN, networkDomain));
		params.add(new NameValuePair(ApiConstants.NEW_NAME, newAccountName));
		if(details != null){
			params.add(new NameValuePair(ApiConstants.ACCOUNT_DETAILS, details.toString()));
		}
		int regionId = account.getRegionId();
		if(getId() == regionId){
			Account updatedAccount = _accountMgr.updateAccount(cmd);
			if(updatedAccount != null){
				List<RegionVO> regions =  _regionDao.listAll();
				for (Region region : regions){
					if(region.getId() == getId()){
						continue;
					}
					params.add(new NameValuePair(ApiConstants.IS_PROPAGATE, "true"));
					if (RegionsApiUtil.makeAPICall(region, command, params)) {
						s_logger.debug("Successfully updated account :"+account.getUuid()+" in Region: "+region.getId());
					} else {
						s_logger.error("Error while updating account :"+account.getUuid()+" in Region: "+region.getId());
					}
				}
			}
			return updatedAccount;
		} else {
			//First update in the Region where account is created
			Region region = _regionDao.findById(regionId);
			RegionAccount updatedAccount = RegionsApiUtil.makeAccountAPICall(region, command, params);
			if (updatedAccount != null) {
				Long id = _identityDao.getIdentityId("account", updatedAccount.getUuid());
				updatedAccount.setId(id);
				Long domainID = _identityDao.getIdentityId("domain", updatedAccount.getDomainUuid());
				updatedAccount.setDomainId(domainID);
				s_logger.debug("Successfully updated account :"+account.getUuid()+" in source Region: "+region.getId());
				return updatedAccount;
			} else {
				throw new CloudRuntimeException("Error while updating account :"+account.getUuid()+" in source Region: "+region.getId());
			}
		}
	}

	@Override
	public Region addRegion(int id, String name, String endPoint, String apiKey, String secretKey) {
		if( _regionDao.findById(id) == null ){
			RegionVO region = new RegionVO(id, name, endPoint, apiKey, secretKey);
			return _regionDao.persist(region);
		} else {
			throw new InvalidParameterValueException("Region with id: "+id+" already exists");
		}
	}

	@Override
	public Region updateRegion(int id, String name, String endPoint, String apiKey, String secretKey) {
		RegionVO region = _regionDao.findById(id);
		
		if(region == null){
			throw new InvalidParameterValueException("Region with id: "+id+" does not exist");
		}
		
		if(name != null){
			region.setName(name);
		}
		
		if(endPoint != null){
			region.setEndPoint(endPoint);
		}
		
		if(apiKey != null){
			region.setApiKey(apiKey);
		}
		
		if(secretKey != null){
			region.setSecretKey(secretKey);
		}
		
		_regionDao.update(id, region);
		return _regionDao.findById(id);
	}

	@Override
	public boolean removeRegion(int id) {
		//Remove complete row, instead of soft delete
		RegionVO region = _regionDao.findById(id);
		if(region != null){
			return _regionDao.remove(id);
		} else {
			throw new InvalidParameterValueException("Failed to delete Region: " + id + ", Region not found");
		}
	}

	public int getId() {
		return _id;
	}

	public void setId(int _id) {
		this._id = _id;
	}

	@Override
	public List<RegionVO> listRegions(ListRegionsCmd cmd) {
		if(cmd.getId() != null){
			List<RegionVO> regions = new ArrayList<RegionVO>();
			regions.add(_regionDao.findById(cmd.getId()));
			return regions;
		}
		return _regionDao.listAll();
	}

	@Override
	public Account disableAccount(String accountName, Long domainId, Long accountId, Boolean lockRequested) throws ConcurrentOperationException, ResourceUnavailableException {
		Account account = null;
        if (accountId != null) {
            account = _accountDao.findById(accountId);
        } else {
            account = _accountDao.findActiveAccount(accountName, domainId);
        }

        if (account == null || account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            throw new InvalidParameterValueException("Unable to find active account by accountId: " + accountId + " OR by name: " + accountName + " in domain " + domainId);
        }
		String accountUUID = account.getUuid();
		
		String command = "disableAccount";
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new NameValuePair(ApiConstants.LOCK, lockRequested.toString()));
		params.add(new NameValuePair(ApiConstants.ID, accountUUID));
		DomainVO domain = _domainDao.findById(domainId);
		if(domain != null){		
			params.add(new NameValuePair(ApiConstants.DOMAIN_ID, domain.getUuid()));
		}
		
		int regionId = account.getRegionId();
		if(getId() == regionId){
			Account retAccount = null;
			if(lockRequested){
				retAccount = _accountMgr.lockAccount(accountName, domainId, accountId);
			} else {
				retAccount = _accountMgr.disableAccount(accountName, domainId, accountId);
			}
			if(retAccount != null){
				List<RegionVO> regions =  _regionDao.listAll();
				for (Region region : regions){
					if(region.getId() == getId()){
						continue;
					}
					params.add(new NameValuePair(ApiConstants.IS_PROPAGATE, "true"));
					if (RegionsApiUtil.makeAPICall(region, command, params)) {
						s_logger.debug("Successfully disabled account :"+accountUUID+" in Region: "+region.getId());
					} else {
						s_logger.error("Error while disabling account :"+accountUUID+" in Region: "+region.getId());
					}
				}
			}
			return retAccount;
		} else {
			//First disable account in the Region where account is created
			Region region = _regionDao.findById(regionId);
			Account retAccount = RegionsApiUtil.makeAccountAPICall(region, command, params);
			if (retAccount != null) {
				s_logger.debug("Successfully disabled account :"+accountUUID+" in source Region: "+region.getId());
				return retAccount;
			} else {
				throw new CloudRuntimeException("Error while disabling account :"+accountUUID+" in source Region: "+region.getId());
			}
		}
	}

	@Override
	public Account enableAccount(String accountName, Long domainId, Long accountId) {
        // Check if account exists
        Account account = null;
        if (accountId != null) {
            account = _accountDao.findById(accountId);
        } else {
            account = _accountDao.findActiveAccount(accountName, domainId);
        }

        if (account == null || account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            throw new InvalidParameterValueException("Unable to find account by accountId: " + accountId + " OR by name: " + accountName + " in domain " + domainId);
        }
        String accountUUID = account.getUuid();
        
        String command = "enableAccount";
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new NameValuePair(ApiConstants.ID, accountUUID));
		params.add(new NameValuePair(ApiConstants.ACCOUNT, accountName));
        DomainVO domain = _domainDao.findById(domainId);
		if(domain != null){
			params.add(new NameValuePair(ApiConstants.DOMAIN_ID, domain.getUuid()));
		}
		
		int regionId = account.getRegionId();
		if(getId() == regionId){
			Account retAccount = _accountMgr.enableAccount(accountName, domainId, accountId);
			if(retAccount != null){
				List<RegionVO> regions =  _regionDao.listAll();

				for (Region region : regions){
					if(region.getId() == getId()){
						continue;
					}
					params.add(new NameValuePair(ApiConstants.IS_PROPAGATE, "true"));
					if (RegionsApiUtil.makeAPICall(region, command, params)) {
						s_logger.debug("Successfully enabled account :"+accountUUID+" in Region: "+region.getId());
					} else {
						s_logger.error("Error while enabling account :"+accountUUID+" in Region: "+region.getId());
					}
				}
			}
			return retAccount;
		} else {
			//First disable account in the Region where account is created
			Region region = _regionDao.findById(regionId);
			Account retAccount = RegionsApiUtil.makeAccountAPICall(region, command, params);
			if (retAccount != null) {
				s_logger.debug("Successfully enabled account :"+accountUUID+" in source Region: "+region.getId());
				return retAccount;
			} else {
				throw new CloudRuntimeException("Error while enabling account :"+accountUUID+" in source Region: "+region.getId());
			}
		}
	}

	@Override
	public boolean deleteUser(DeleteUserCmd cmd) {
        long id = cmd.getId();

        UserVO user = _userDao.findById(id);

        if (user == null) {
            throw new InvalidParameterValueException("The specified user doesn't exist in the system");
        }
		
		String userUUID = user.getUuid();
		int regionId = user.getRegionId();
		
		String command = "deleteUser";
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new NameValuePair(ApiConstants.ID, userUUID));
		
		if(getId() == regionId){
			if(_accountMgr.deleteUser(cmd)){
				List<RegionVO> regions =  _regionDao.listAll();
				for (Region region : regions){
					if(region.getId() == getId()){
						continue;
					}
					params.add(new NameValuePair(ApiConstants.IS_PROPAGATE, "true"));
					if (RegionsApiUtil.makeAPICall(region, command, params)) {
						s_logger.debug("Successfully deleted user :"+userUUID+" in Region: "+region.getId());
					} else {
						s_logger.error("Error while deleting account :"+userUUID+" in Region: "+region.getId());
					}
				}
				return true;
			} else {
				return false;
			}
		} else {
			//First delete in the Region where account is created
			Region region = _regionDao.findById(regionId);
			if (RegionsApiUtil.makeAPICall(region, command, params)) {
				s_logger.debug("Successfully deleted user :"+userUUID+" in source Region: "+region.getId());
				return true;
			} else {
				s_logger.error("Error while deleting user :"+userUUID+" in source Region: "+region.getId());
				return false;
			}
		}
	}

	@Override
	public boolean deleteDomain(Long id, Boolean cleanup) {

		DomainVO domain = _domainDao.findById(id);
		if(domain == null){
			throw new InvalidParameterValueException("The specified domain doesn't exist in the system");
		}
		String domainUUID = domain.getUuid();
		
		String command = "deleteDomain";
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new NameValuePair(ApiConstants.ID, domainUUID));
		params.add(new NameValuePair(ApiConstants.CLEANUP, cleanup.toString()));
		
        int regionId = domain.getRegionId();
		if(getId() == regionId){
			if(_domainMgr.deleteDomain(id, cleanup)){
				List<RegionVO> regions =  _regionDao.listAll();
				for (Region region : regions){
					if(region.getId() == getId()){
						continue;
					}
					params.add(new NameValuePair(ApiConstants.IS_PROPAGATE, "true"));
					if (RegionsApiUtil.makeAPICall(region, command, params)) {
						s_logger.debug("Successfully deleted domain :"+domainUUID+" in Region: "+region.getId());
					} else {
						s_logger.error("Error while deleting domain :"+domainUUID+" in Region: "+region.getId());
					}
				}
				return true;
			} else {
				return false;
			}
		} else {
			//First delete in the Region where domain is created
			Region region = _regionDao.findById(regionId);
			if (RegionsApiUtil.makeAPICall(region, command, params)) {
				s_logger.debug("Successfully deleted domain :"+domainUUID+" in Region: "+region.getId());
				return true;
			} else {
				s_logger.error("Error while deleting domain :"+domainUUID+" in Region: "+region.getId());
				return false;
			}
		}
	}

	@Override
	public UserAccount updateUser(UpdateUserCmd cmd){
        long id = cmd.getId();

        UserVO user = _userDao.findById(id);

        if (user == null) {
            throw new InvalidParameterValueException("The specified user doesn't exist in the system");
        }
		String userUUID = user.getUuid();
		
		String command = "updateUser";
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new NameValuePair(ApiConstants.ID, userUUID));
		params.add(new NameValuePair(ApiConstants.API_KEY, cmd.getApiKey()));
		params.add(new NameValuePair(ApiConstants.EMAIL, cmd.getEmail()));
		params.add(new NameValuePair(ApiConstants.FIRSTNAME, cmd.getFirstname()));
		params.add(new NameValuePair(ApiConstants.LASTNAME, cmd.getLastname()));
		params.add(new NameValuePair(ApiConstants.PASSWORD, cmd.getPassword()));
		params.add(new NameValuePair(ApiConstants.SECRET_KEY, cmd.getSecretKey()));
		params.add(new NameValuePair(ApiConstants.TIMEZONE, cmd.getTimezone()));
		params.add(new NameValuePair(ApiConstants.USERNAME, cmd.getUsername()));
		
		int regionId = user.getRegionId();
		if(getId() == regionId){
			UserAccount updateUser = _accountMgr.updateUser(cmd);
			if(updateUser != null){
				List<RegionVO> regions =  _regionDao.listAll();
				for (Region region : regions){
					if(region.getId() == getId()){
						continue;
					}
					params.add(new NameValuePair(ApiConstants.IS_PROPAGATE, "true"));
					if (RegionsApiUtil.makeAPICall(region, command, params)) {
						s_logger.debug("Successfully updated user :"+userUUID+" in Region: "+region.getId());
					} else {
						s_logger.error("Error while updating user :"+userUUID+" in Region: "+region.getId());
					}
				}
			}
			return updateUser;
		} else {
			//First update in the Region where user was created
			Region region = _regionDao.findById(regionId);
			UserAccount updateUser = RegionsApiUtil.makeUserAccountAPICall(region, command, params);
			if (updateUser != null) {
				s_logger.debug("Successfully updated user :"+userUUID+" in source Region: "+region.getId());
				return updateUser;
			} else {
				throw new CloudRuntimeException("Error while updating user :"+userUUID+" in source Region: "+region.getId());
			}
		}
	}

	@Override
	public Domain updateDomain(UpdateDomainCmd cmd) {
		long id = cmd.getId();
		DomainVO domain = _domainDao.findById(id);
		if(domain == null){
			throw new InvalidParameterValueException("The specified domain doesn't exist in the system");
		}
		String domainUUID = domain.getUuid();
		
		String command = "updateDomain";
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new NameValuePair(ApiConstants.ID, domainUUID));
		params.add(new NameValuePair(ApiConstants.NAME, cmd.getDomainName()));
		params.add(new NameValuePair(ApiConstants.NETWORK_DOMAIN, cmd.getNetworkDomain()));
		
		int regionId = domain.getRegionId();
		if(getId() == regionId){
			Domain updatedDomain = _domainMgr.updateDomain(cmd);
			if(updatedDomain != null){
				List<RegionVO> regions =  _regionDao.listAll();
				for (Region region : regions){
					if(region.getId() == getId()){
						continue;
					}
					params.add(new NameValuePair(ApiConstants.IS_PROPAGATE, "true"));
					if (RegionsApiUtil.makeAPICall(region, command, params)) {
						s_logger.debug("Successfully updated updatedDomain :"+domainUUID+" in Region: "+region.getId());
					} else {
						s_logger.error("Error while updating updatedDomain :"+domainUUID+" in Region: "+region.getId());
					}
				}
			}
			return updatedDomain;
		} else {
			//First update in the Region where domain was created
			Region region = _regionDao.findById(regionId);
			RegionDomain updatedDomain = RegionsApiUtil.makeDomainAPICall(region, command, params);
			if (updatedDomain != null) {
				Long parentId = _identityDao.getIdentityId("domain", updatedDomain.getParentUuid());
				updatedDomain.setParent(parentId);
				s_logger.debug("Successfully updated user :"+domainUUID+" in source Region: "+region.getId());
				return (DomainVO)updatedDomain;
			} else {
				throw new CloudRuntimeException("Error while updating user :"+domainUUID+" in source Region: "+region.getId());
			}
		}
	}

	@Override
	public UserAccount disableUser(Long userId) {
        UserVO user = _userDao.findById(userId);
        if (user == null || user.getRemoved() != null) {
            throw new InvalidParameterValueException("Unable to find active user by id " + userId);
        }
        int regionId = user.getRegionId();
        
        String command = "disableUser";
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new NameValuePair(ApiConstants.ID, user.getUuid()));
        
		if(getId() == regionId){
			UserAccount disabledUser = _accountMgr.disableUser(userId);
			if(disabledUser != null){
				List<RegionVO> regions =  _regionDao.listAll();
				for (Region region : regions){
					if(region.getId() == getId()){
						continue;
					}
					params.add(new NameValuePair(ApiConstants.IS_PROPAGATE, "true"));
					if (RegionsApiUtil.makeAPICall(region, command, params)) {
						s_logger.debug("Successfully disabled user :"+user.getUuid()+" in Region: "+region.getId());
					} else {
						s_logger.error("Error while disabling user :"+user.getUuid()+" in Region: "+region.getId());
					}
				}
			}
			return disabledUser;
		} else {
			//First disable in the Region where user was created
			Region region = _regionDao.findById(regionId);
			UserAccount disabledUser = RegionsApiUtil.makeUserAccountAPICall(region, command, params);
			if (disabledUser != null) {
				s_logger.debug("Successfully disabled user :"+user.getUuid()+" in source Region: "+region.getId());
				return disabledUser;
			} else {
				throw new CloudRuntimeException("Error while disabling user :"+user.getUuid()+" in source Region: "+region.getId());
			}
		}
	}

	@Override
	public UserAccount enableUser(Long userId) {
        UserVO user = _userDao.findById(userId);
        if (user == null || user.getRemoved() != null) {
            throw new InvalidParameterValueException("Unable to find active user by id " + userId);
        }
        int regionId = user.getRegionId();
        
        String command = "enableUser";
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new NameValuePair(ApiConstants.ID, user.getUuid()));
        
		if(getId() == regionId){
			UserAccount enabledUser = _accountMgr.enableUser(userId);
			if(enabledUser != null){
				List<RegionVO> regions =  _regionDao.listAll();
				for (Region region : regions){
					if(region.getId() == getId()){
						continue;
					}
					params.add(new NameValuePair(ApiConstants.IS_PROPAGATE, "true"));
					if (RegionsApiUtil.makeAPICall(region, command, params)) {
						s_logger.debug("Successfully enabled user :"+user.getUuid()+" in Region: "+region.getId());
					} else {
						s_logger.error("Error while disabling user :"+user.getUuid()+" in Region: "+region.getId());
					}
				}
			}
			return enabledUser;
		} else {
			//First enable in the Region where user was created
			Region region = _regionDao.findById(regionId);
			UserAccount enabledUser = RegionsApiUtil.makeUserAccountAPICall(region, command, params);
			if (enabledUser != null) {
				s_logger.debug("Successfully enabled user :"+user.getUuid()+" in source Region: "+region.getId());
				return enabledUser;
			} else {
				throw new CloudRuntimeException("Error while enabling user :"+user.getUuid()+" in source Region: "+region.getId());
			}
		}
	}

	@Override
	public void propogateAddUser(String userName, String password,
			String firstName, String lastName, String email, String timezone,
			String accountName, String domainUUId, String userUUID) {
		
        String command = "createUser";
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new NameValuePair(ApiConstants.USERNAME, userName));
		params.add(new NameValuePair(ApiConstants.PASSWORD, password));
		params.add(new NameValuePair(ApiConstants.FIRSTNAME, firstName));
		params.add(new NameValuePair(ApiConstants.LASTNAME, lastName));
		params.add(new NameValuePair(ApiConstants.EMAIL, email));
		params.add(new NameValuePair(ApiConstants.TIMEZONE, timezone));
		params.add(new NameValuePair(ApiConstants.ACCOUNT, accountName));		
		params.add(new NameValuePair(ApiConstants.DOMAIN_ID, domainUUId));
		params.add(new NameValuePair(ApiConstants.USER_ID, userUUID));
		params.add(new NameValuePair(ApiConstants.REGION_ID, ""+getId()));
		
		List<RegionVO> regions =  _regionDao.listAll();
		for (Region region : regions){
			if(region.getId() == getId()){
				continue;
			}
			s_logger.debug("Adding account :"+accountName+" to Region: "+region.getId());
			if (RegionsApiUtil.makeAPICall(region, command, params)) {
				s_logger.debug("Successfully added user :"+userName+" to Region: "+region.getId());
			} else {
				s_logger.error("Error while Adding user :"+userName+" to Region: "+region.getId());
			}
		}
		return;		
	}
	
	@Override
	public void propogateAddDomain(String name, Long parentId, String networkDomain, String uuid) {
		
        String command = "createDomain";
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new NameValuePair(ApiConstants.NAME, name));
		if(parentId != null){
			DomainVO domain = _domainDao.findById(parentId);
			if(domain != null){
				params.add(new NameValuePair(ApiConstants.PARENT_DOMAIN_ID, domain.getUuid()));
			}
		}
		params.add(new NameValuePair(ApiConstants.NETWORK_DOMAIN, networkDomain));
		params.add(new NameValuePair(ApiConstants.DOMAIN_ID, uuid));
		params.add(new NameValuePair(ApiConstants.REGION_ID, ""+getId()));
		
		List<RegionVO> regions =  _regionDao.listAll();
		for (Region region : regions){
			if(region.getId() == getId()){
				continue;
			}
			s_logger.debug("Adding domain :"+name+" to Region: "+region.getId());
			if (RegionsApiUtil.makeAPICall(region, command, params)) {
				s_logger.debug("Successfully added domain :"+name+" to Region: "+region.getId());
			} else {
				s_logger.error("Error while Adding domain :"+name+" to Region: "+region.getId());
			}
		}
		return;		
	}
	
	@Override
    public UserAccount getUserAccount(String username, Long domainId) {
		UserAccount user = _userAccountDao.getUserAccount(username, domainId);
		if(user != null){
			return user;
		} else {
			DomainVO domain = _domainDao.findById(domainId);
			if(domain == null){
				//Lookup Domain
				s_logger.debug("Domain with Id :"+domainId+" doesn't exist");
			}
			String command = "findUser";
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new NameValuePair(ApiConstants.USERNAME, username));
			params.add(new NameValuePair(ApiConstants.DOMAIN_ID, domain.getUuid()));
			RegionUser regionuser = null;
			List<RegionVO> regions =  _regionDao.listAll();
			boolean sourceCheck = false;
			for (Region region : regions){
				if(region.getId() == getId()){
					continue;
				}
				s_logger.debug("Looking up user :"+username+" in Region: "+region.getId());
				regionuser = RegionsApiUtil.makeUserAPICall(region, command, params);
				if(regionuser != null){
					s_logger.debug("Found user :"+username+" in Region: "+region.getId());
					if(regionuser.getRegionId() != region.getId()){
						sourceCheck = true;
					}
					break;
				}
			}

			if(regionuser == null){
				s_logger.debug("User :"+username+" not found in any Region");
				return null;
			}
			
			if(sourceCheck){
				if(regionuser.getRegionId() == getId()){
					s_logger.debug("Current Region is the source Region for found user: " +username+ ". Ignoring..");
					return null;
				}
				
				s_logger.debug("Verifying user: " +username+ " in source Region: "+regionuser.getRegionId());
				
				//Verify user in source Region
				Region sourceRegion = _regionDao.findById(regionuser.getRegionId());
				if(sourceRegion != null){
					regionuser = RegionsApiUtil.makeUserAPICall(sourceRegion, command, params);				 
					if(regionuser != null && sourceRegion.getId() == regionuser.getRegionId()){
						s_logger.debug("Found User :"+username+" in Source Region: "+sourceRegion.getId()+" Add to local Region");
					} else {
						s_logger.debug("User :"+username+" not found in Source Region: "+sourceRegion.getId());
						return  null;
					}
				} else {
					s_logger.debug("Source Region :"+regionuser.getRegionId()+" not found");
					return  null;
				}
			}
			
			if(regionuser != null){
				Long accountId = _identityDao.getIdentityId("account", regionuser.getAccountuuid());
				if(accountId == null){
					//Lookup Account
				}
				regionuser.setAccountId(accountId);
				UserVO newuser = (UserVO)regionuser;
				_userDao.persist(newuser);
				return _userAccountDao.getUserAccount(username, domainId);
			}
			return null;
		}
    }
	
    @Override
    public DomainVO findDomainByPath(String domainPath) {
    	DomainVO domain = (DomainVO)_domainMgr.findDomainByPath(domainPath);
    	if(domain != null){
    		return domain;
    	} else {
    		String command = "findDomain";
    		List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new NameValuePair(ApiConstants.DOMAIN, domainPath));
			boolean sourceCheck = false;
			RegionDomain regiondomain = null;
			List<RegionVO> regions =  _regionDao.listAll();
			for (Region region : regions){
				if(region.getId() == getId()){
					continue;
				}
				s_logger.debug("Looking up domain :"+domainPath+" in Region: "+region.getId());
				regiondomain = RegionsApiUtil.makeDomainAPICall(region, command, params);
				if(regiondomain != null){
					s_logger.debug("Found domain :"+domainPath+" in Region: "+region.getId());
					if(regiondomain.getRegionId() != region.getId()){
						sourceCheck = true;
					}
					break;
				}
			}

			if(regiondomain == null){
				s_logger.debug("Domain :"+domainPath+" not found in any Region");
				return null;
			}
			
			if(sourceCheck){
				if(regiondomain.getRegionId() == getId()){
					s_logger.debug("Current Region is the source Region for found domain: " +domainPath+ ". Ignoring..");
					return null;
				}
				
				s_logger.debug("Verifying domain: " +domainPath+ " in source Region: "+regiondomain.getRegionId());
				
				//Verify user in source Region
				Region sourceRegion = _regionDao.findById(regiondomain.getRegionId());
				if(sourceRegion != null){
					DomainVO sourceDomain = RegionsApiUtil.makeDomainAPICall(sourceRegion, command, params);				 
					if(sourceDomain != null && sourceRegion.getId() == sourceDomain.getRegionId()){
						s_logger.debug("Found Domain :"+domainPath+" in Source Region: "+sourceRegion.getId()+" Add to local Region");
					} else {
						s_logger.debug("Domain :"+domainPath+" not found in Source Region: "+sourceRegion.getId());
						return  null;
					}
				} else {
					s_logger.debug("Source Region :"+regiondomain.getRegionId()+" not found");
					return  null;
				}
			}
			
			if(regiondomain != null){
				Long parentId = _identityDao.getIdentityId("domain", regiondomain.getParentUuid());
				if(parentId == null){
					//lookup ParentDomain
				}
				regiondomain.setParent(parentId);
				regiondomain.setState(Domain.State.Active);
				_domainDao.persist((DomainVO)regiondomain);
			}
			return (DomainVO)_domainMgr.findDomainByPath(domainPath);
    	}
    }
}
