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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.commands.DeleteUserCmd;
import com.cloud.api.commands.ListRegionsCmd;
import com.cloud.api.commands.UpdateAccountCmd;
import com.cloud.api.commands.UpdateDomainCmd;
import com.cloud.api.commands.UpdateUserCmd;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.region.dao.RegionDao;
import com.cloud.server.ManagementServer;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.DomainManager;
import com.cloud.user.UserAccount;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.component.Inject;
import com.cloud.utils.component.Manager;

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
    private ManagementServer _mgmtSrvr;
    @Inject
    private DomainManager _domainMgr;
    
    
    private String _name;
    private long _id = 1; //ToDo, get this from config or db.properties
    
    //ToDo use API constants
    //prepare API params in advance
    
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
	public boolean propogateAddAccount(String userName, String password, String firstName, String lastName, String email, String timezone, 
			String accountName, short accountType, Long domainId, String networkDomain, Map<String, String> details, String accountUUID, String userUUID) {
		List<RegionVO> regions =  _regionDao.listAll();
		StringBuffer params = new StringBuffer("/api?command=createAccount");
		params.append("&"+ApiConstants.USERNAME+"="+userName);
		params.append("&"+ApiConstants.PASSWORD+"="+password);
		params.append("&"+ApiConstants.FIRSTNAME+"="+firstName);
		params.append("&"+ApiConstants.LASTNAME+"="+lastName);
		params.append("&"+ApiConstants.EMAIL+"="+email);
		if(timezone != null){
			params.append("&"+ApiConstants.TIMEZONE+"="+timezone);		
		}
		if(accountName != null){
			params.append("&"+ApiConstants.ACCOUNT+"="+accountName);			
		}
		params.append("&"+ApiConstants.ACCOUNT_TYPE+"="+accountType);
		if(domainId != null){
			params.append("&"+ApiConstants.DOMAIN_ID+"="+domainId); //use UUID			
		}
		if(networkDomain != null){
			params.append("&"+ApiConstants.NETWORK_DOMAIN+"="+networkDomain);			
		}
		if(details != null){
			params.append("&"+ApiConstants.ACCOUNT_DETAILS+"="+details); //ToDo change to Map
		}
		params.append("&"+ApiConstants.ACCOUNT_ID+"="+accountUUID);
		params.append("&"+ApiConstants.USER_ID+"="+userUUID);
		params.append("&"+ApiConstants.REGION_ID+"="+getId());
		
		for (Region region : regions){
			if(region.getId() == getId()){
				continue;
			}
			s_logger.debug("Adding account :"+accountName+" to Region: "+region.getId());
			String url = region.getEndPoint() + params;
			if (makeAPICall(url)) {
				s_logger.debug("Successfully added account :"+accountName+" to Region: "+region.getId());
			} else {
				s_logger.error("Error while Adding account :"+accountName+" to Region: "+region.getId());
				//Send Account delete to all Regions where account is added successfully
				break;
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
		long regionId = account.getRegionId();
		String params = "/api?command=deleteAccount&"+ApiConstants.ID+"="+accountUUID;
		if(getId() == regionId){
			if(_accountMgr.deleteUserAccount(accountId)){
				List<RegionVO> regions =  _regionDao.listAll();
				for (Region region : regions){
					if(region.getId() == getId()){
						continue;
					}
					String url = region.getEndPoint() + params+"&"+ApiConstants.IS_PROPAGATE+"=true";
					if (makeAPICall(url)) {
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
			String url = region.getEndPoint() + params;
			if (makeAPICall(url)) {
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
        
        StringBuffer params = new StringBuffer("/api?command=updateAccount");
        params.append("&"+ApiConstants.NEW_NAME+"="+newAccountName);
        if(account != null){
        	params.append("&"+ApiConstants.ID+"="+account.getUuid());
        }
        if(accountName != null){
        	params.append("&"+ApiConstants.ACCOUNT+"="+accountName);
        }
        if(domain != null){
        	params.append("&"+ApiConstants.DOMAIN_ID+"="+domain.getUuid());
        }
        if(networkDomain != null){
        	params.append("&"+ApiConstants.NETWORK_DOMAIN+"="+networkDomain);
        }
        if(details != null){
        	params.append("&"+ApiConstants.ACCOUNT_DETAILS+"="+details);
        }
        
		long regionId = account.getRegionId();
		if(getId() == regionId){
			Account updateAccount = _accountMgr.updateAccount(cmd);
			if(updateAccount != null){
				List<RegionVO> regions =  _regionDao.listAll();
				for (Region region : regions){
					if(region.getId() == getId()){
						continue;
					}
					String url = region.getEndPoint() + params+"&"+ApiConstants.IS_PROPAGATE+"=true";
					if (makeAPICall(url)) {
						s_logger.debug("Successfully updated account :"+account.getUuid()+" in Region: "+region.getId());
					} else {
						s_logger.error("Error while updating account :"+account.getUuid()+" in Region: "+region.getId());
					}
				}
			}
			return updateAccount;
		} else {
			//First update in the Region where account is created
			Region region = _regionDao.findById(regionId);
			String url = region.getEndPoint() + params;
			if (makeAPICall(url)) {
				s_logger.debug("Successfully updated account :"+account.getUuid()+" in source Region: "+region.getId());
				//return Account object
				return null;
			} else {
				s_logger.error("Error while updating account :"+account.getUuid()+" in source Region: "+region.getId());
				//throw exception;
				return null;
			}
		}
	}

	private boolean makeAPICall(String url){
		try {

			HttpClient client = new HttpClient();
			HttpMethod method = new GetMethod(url);
			if( client.executeMethod(method) == 200){
				return true;
			} else {
				return false;
			}
		} catch (HttpException e) {
			s_logger.error(e.getMessage());
			return false;
		} catch (IOException e) {
			s_logger.error(e.getMessage());
			return false;
		}
		
	}
	
	@Override
	public Region addRegion(long id, String name, String endPoint) {
		RegionVO region = new RegionVO(id, name, endPoint);
		return _regionDao.persist(region);
	}

	@Override
	public Region updateRegion(long id, String name, String endPoint) {
		RegionVO region = _regionDao.findById(id);
		if(name != null){
			region.setName(name);
		}
		
		if(endPoint != null){
			region.setEndPoint(endPoint);
		}
		
		_regionDao.update(id, region);
		return _regionDao.findById(id);
	}

	@Override
	public boolean removeRegion(long id) {
		RegionVO region = _regionDao.findById(id);
		if(region != null){
			return _regionDao.remove(id);
		} else {
			throw new InvalidParameterValueException("Failed to delete Region: " + id + ", Region not found");
		}
	}

	public long getId() {
		return _id;
	}

	public void setId(long _id) {
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
		StringBuffer params = new StringBuffer("/api?command=disableAccount"+"&"+ApiConstants.LOCK+"="+lockRequested);
		params.append("&"+ApiConstants.ID+"="+accountUUID);
		if(accountName != null){
			params.append("&"+ApiConstants.ACCOUNT+"="+accountName);
		}
        DomainVO domain = _domainDao.findById(domainId);
		if(domain != null){
			params.append("&"+ApiConstants.DOMAIN_ID+"="+domain.getUuid());
		}
		long regionId = account.getRegionId();
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
					String url = region.getEndPoint() + params +"&"+ApiConstants.IS_PROPAGATE+"=true";
					if (makeAPICall(url)) {
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
			String url = region.getEndPoint() + params;
			if (makeAPICall(url)) {
				s_logger.debug("Successfully disabled account :"+accountUUID+" in source Region: "+region.getId());
				//return Account object
				return null;
			} else {
				s_logger.error("Error while disabling account :"+accountUUID+" in source Region: "+region.getId());
				//throw exception;
				return null;
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
		StringBuffer params = new StringBuffer("/api?command=enableAccount");
		params.append("&"+ApiConstants.ID+"="+accountUUID);
		if(accountName != null){
			params.append("&"+ApiConstants.ACCOUNT+"="+accountName);
		}
        DomainVO domain = _domainDao.findById(domainId);
		if(domain != null){
			params.append("&"+ApiConstants.DOMAIN_ID+"="+domain.getUuid());
		}

		long regionId = account.getRegionId();
		if(getId() == regionId){
			Account retAccount = _accountMgr.enableAccount(accountName, domainId, accountId);
			if(retAccount != null){
				List<RegionVO> regions =  _regionDao.listAll();

				for (Region region : regions){
					if(region.getId() == getId()){
						continue;
					}
					String url = region.getEndPoint() + params +"&"+ApiConstants.IS_PROPAGATE+"=true";
					if (makeAPICall(url)) {
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
			String url = region.getEndPoint() + params;
			if (makeAPICall(url)) {
				s_logger.debug("Successfully enabled account :"+accountUUID+" in source Region: "+region.getId());
				//return Account object
				return null;
			} else {
				s_logger.error("Error while enabling account :"+accountUUID+" in source Region: "+region.getId());
				//throw exception;
				return null;
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
		long regionId = user.getRegionId();
		String params = "/api?command=deleteUser&id="+userUUID;
		if(getId() == regionId){
			if(_accountMgr.deleteUser(cmd)){
				List<RegionVO> regions =  _regionDao.listAll();
				for (Region region : regions){
					if(region.getId() == getId()){
						continue;
					}
					String url = region.getEndPoint() + params +"&"+ApiConstants.IS_PROPAGATE+"=true";
					if (makeAPICall(url)) {
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
			String url = region.getEndPoint() + params;
			if (makeAPICall(url)) {
				s_logger.debug("Successfully deleted user :"+userUUID+" in Region: "+region.getId());
				return true;
			} else {
				s_logger.error("Error while deleting user :"+userUUID+" in Region: "+region.getId());
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
        StringBuffer params = new StringBuffer("/api?command=deleteDomain");
        params.append("&"+ApiConstants.ID+"="+domainUUID);
        params.append("&"+ApiConstants.CLEANUP+"="+cleanup);
        long regionId = domain.getRegionId();
		if(getId() == regionId){
			if(_domainMgr.deleteDomain(id, cleanup)){
				List<RegionVO> regions =  _regionDao.listAll();
				for (Region region : regions){
					if(region.getId() == getId()){
						continue;
					}
					String url = region.getEndPoint() + params +"&"+ApiConstants.IS_PROPAGATE+"=true";
					if (makeAPICall(url)) {
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
			String url = region.getEndPoint() + params;
			if (makeAPICall(url)) {
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
        StringBuffer params = new StringBuffer("/api?command=updateUser");
        params.append("&"+ApiConstants.ID+"="+userUUID);
        if(cmd.getApiKey() != null){
        	params.append("&"+ApiConstants.API_KEY+"="+cmd.getApiKey());    
        }
        if(cmd.getEmail() != null){
        	params.append("&"+ApiConstants.EMAIL+"="+cmd.getEmail());    
        }
        if(cmd.getFirstname() != null){
        	params.append("&"+ApiConstants.FIRSTNAME+"="+cmd.getFirstname());    
        }        
        if(cmd.getLastname() != null){
        	params.append("&"+ApiConstants.LASTNAME+"="+cmd.getLastname());    
        }        
        if(cmd.getPassword() != null){
        	params.append("&"+ApiConstants.PASSWORD+"="+cmd.getPassword());    
        }
        if(cmd.getSecretKey() != null){
        	params.append("&"+ApiConstants.SECRET_KEY+"="+cmd.getSecretKey());    
        }          
        if(cmd.getTimezone() != null){
        	params.append("&"+ApiConstants.TIMEZONE+"="+cmd.getTimezone());    
        } 
        if(cmd.getUsername() != null){
        	params.append("&"+ApiConstants.USERNAME+"="+cmd.getUsername());    
        }         

		long regionId = user.getRegionId();
		if(getId() == regionId){
			UserAccount updateUser = _accountMgr.updateUser(cmd);
			if(updateUser != null){
				List<RegionVO> regions =  _regionDao.listAll();
				for (Region region : regions){
					if(region.getId() == getId()){
						continue;
					}
					String url = region.getEndPoint() + params +"&"+ApiConstants.IS_PROPAGATE+"=true";
					if (makeAPICall(url)) {
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
			String url = region.getEndPoint() + params;
			if (makeAPICall(url)) {
				s_logger.debug("Successfully updated user :"+userUUID+" in source Region: "+region.getId());
				//return object
				return null;
			} else {
				s_logger.error("Error while updating user :"+userUUID+" in source Region: "+region.getId());
				//throw exception;
				return null;
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
        StringBuffer params = new StringBuffer("/api?command=updateDomain");
        params.append("&"+ApiConstants.ID+"="+domainUUID);
        if(cmd.getDomainName() != null){
        	params.append("&"+ApiConstants.NAME+"="+cmd.getDomainName());    
        }
        if(cmd.getNetworkDomain() != null){
        	params.append("&"+ApiConstants.NETWORK_DOMAIN+"="+cmd.getNetworkDomain());    
        }

		long regionId = domain.getRegionId();
		if(getId() == regionId){
			Domain updatedDomain = _mgmtSrvr.updateDomain(cmd);
			if(updatedDomain != null){
				List<RegionVO> regions =  _regionDao.listAll();
				for (Region region : regions){
					if(region.getId() == getId()){
						continue;
					}
					String url = region.getEndPoint() + params +"&"+ApiConstants.IS_PROPAGATE+"=true";
					if (makeAPICall(url)) {
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
			String url = region.getEndPoint() + params;
			if (makeAPICall(url)) {
				s_logger.debug("Successfully updated user :"+domainUUID+" in source Region: "+region.getId());
				//return object
				return null;
			} else {
				s_logger.error("Error while updating user :"+domainUUID+" in source Region: "+region.getId());
				//throw exception;
				return null;
			}
		}
	}

	@Override
	public UserAccount disableUser(Long userId) {
        UserVO user = _userDao.findById(userId);
        if (user == null || user.getRemoved() != null) {
            throw new InvalidParameterValueException("Unable to find active user by id " + userId);
        }
        long regionId = user.getRegionId();
        StringBuffer params = new StringBuffer("/api?command=disableUser&id="+user.getUuid());
		if(getId() == regionId){
			UserAccount disabledUser = _accountMgr.disableUser(userId);
			if(disabledUser != null){
				List<RegionVO> regions =  _regionDao.listAll();
				for (Region region : regions){
					if(region.getId() == getId()){
						continue;
					}
					String url = region.getEndPoint() + params +"&"+ApiConstants.IS_PROPAGATE+"=true";
					if (makeAPICall(url)) {
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
			String url = region.getEndPoint() + params;
			if (makeAPICall(url)) {
				s_logger.debug("Successfully disabled user :"+user.getUuid()+" in source Region: "+region.getId());
				//return object
				return null;
			} else {
				s_logger.error("Error while disabling user :"+user.getUuid()+" in source Region: "+region.getId());
				//throw exception;
				return null;
			}
		}
	}

	@Override
	public UserAccount enableUser(Long userId) {
        UserVO user = _userDao.findById(userId);
        if (user == null || user.getRemoved() != null) {
            throw new InvalidParameterValueException("Unable to find active user by id " + userId);
        }
        long regionId = user.getRegionId();
        StringBuffer params = new StringBuffer("/api?command=enableUser&id="+user.getUuid());
		if(getId() == regionId){
			UserAccount enabledUser = _accountMgr.enableUser(userId);
			if(enabledUser != null){
				List<RegionVO> regions =  _regionDao.listAll();
				for (Region region : regions){
					if(region.getId() == getId()){
						continue;
					}
					String url = region.getEndPoint() + params +"&"+ApiConstants.IS_PROPAGATE+"=true";
					if (makeAPICall(url)) {
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
			String url = region.getEndPoint() + params;
			if (makeAPICall(url)) {
				s_logger.debug("Successfully enabled user :"+user.getUuid()+" in source Region: "+region.getId());
				//return object
				return null;
			} else {
				s_logger.error("Error while enabling user :"+user.getUuid()+" in source Region: "+region.getId());
				//throw exception;
				return null;
			}
		}
	}

	@Override
	public void propogateAddUser(String userName, String password,
			String firstName, String lastName, String email, String timezone,
			String accountName, String domainUUId, String userUUID) {
		
		StringBuffer params = new StringBuffer("/api?command=createUser");
		params.append("&"+ApiConstants.USERNAME+"="+userName);
		params.append("&"+ApiConstants.PASSWORD+"="+password);
		params.append("&"+ApiConstants.FIRSTNAME+"="+firstName);
		params.append("&"+ApiConstants.LASTNAME+"="+lastName);
		params.append("&"+ApiConstants.EMAIL+"="+email);
		if(timezone != null){
			params.append("&"+ApiConstants.TIMEZONE+"="+timezone);		
		}
		if(accountName != null){
			params.append("&"+ApiConstants.ACCOUNT+"="+accountName);	
		}
		if(domainUUId != null){
			params.append("&"+ApiConstants.DOMAIN_ID+"="+domainUUId); //use UUID			
		}
		params.append("&"+ApiConstants.USER_ID+"="+userUUID);
		params.append("&"+ApiConstants.REGION_ID+"="+getId());
		
		List<RegionVO> regions =  _regionDao.listAll();
		for (Region region : regions){
			if(region.getId() == getId()){
				continue;
			}
			s_logger.debug("Adding account :"+accountName+" to Region: "+region.getId());
			String url = region.getEndPoint() + params;
			if (makeAPICall(url)) {
				s_logger.debug("Successfully added user :"+userName+" to Region: "+region.getId());
			} else {
				s_logger.error("Error while Adding user :"+userName+" to Region: "+region.getId());
				//Send User delete to all Regions where account is added successfully
				break;
			}
		}
		return;		
	}
	
	@Override
	public void propogateAddDomain(String name, Long parentId, String networkDomain, String uuid) {
		StringBuffer params = new StringBuffer("/api?command=createDomain");
		params.append("&ApiConstants.NAME="+name);
		String parentUUID = null;
		if(parentId != null){
			DomainVO domain = _domainDao.findById(parentId);
			if(domain != null){
				parentUUID = domain.getUuid();
				params.append("&ApiConstants.PARENT_DOMAIN_ID="+parentUUID);
			}
		}
		if(networkDomain != null){
			params.append("&ApiConstants.NETWORK_DOMAIN="+networkDomain);
		}
		params.append("&ApiConstants.DOMAIN_ID="+uuid);
		params.append("&ApiConstants.REGION_ID="+getId());
		
		List<RegionVO> regions =  _regionDao.listAll();
		for (Region region : regions){
			if(region.getId() == getId()){
				continue;
			}
			s_logger.debug("Adding domain :"+name+" to Region: "+region.getId());
			String url = region.getEndPoint() + params;
			if (makeAPICall(url)) {
				s_logger.debug("Successfully added domain :"+name+" to Region: "+region.getId());
			} else {
				s_logger.error("Error while Adding domain :"+name+" to Region: "+region.getId());
				//Send User delete to all Regions where account is added successfully
				break;
			}
		}
		return;		
		
	}
	
}
