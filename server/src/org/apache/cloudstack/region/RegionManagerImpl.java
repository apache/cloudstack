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

import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.DomainManager;
import com.cloud.user.UserAccount;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.component.Manager;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.uuididentity.dao.IdentityDao;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.command.admin.account.UpdateAccountCmd;
import org.apache.cloudstack.api.command.admin.domain.UpdateDomainCmd;
import org.apache.cloudstack.api.command.admin.user.DeleteUserCmd;
import org.apache.cloudstack.api.command.admin.user.UpdateUserCmd;
import org.apache.cloudstack.region.dao.RegionDao;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Local(value = { RegionManager.class })
public class RegionManagerImpl extends ManagerBase implements RegionManager, Manager{
    public static final Logger s_logger = Logger.getLogger(RegionManagerImpl.class);

    @Inject
    RegionDao _regionDao;
    @Inject
    AccountDao _accountDao;
    @Inject
    private AccountManager _accountMgr;
    @Inject
    private UserDao _userDao;
    @Inject
    private DomainDao _domainDao;
    @Inject
    private DomainManager _domainMgr;
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

    public int getId() {
        return _id;
    }

    /**
     * {@inheritDoc}
     */ 
    @Override
    public Region addRegion(int id, String name, String endPoint, String apiKey, String secretKey) {
        //Region Id should be unique
        if( _regionDao.findById(id) != null ){
            throw new InvalidParameterValueException("Region with id: "+id+" already exists");
        }
        //Region Name should be unique
        if( _regionDao.findByName(name) != null ){
            throw new InvalidParameterValueException("Region with name: "+name+" already exists");
        }
        RegionVO region = new RegionVO(id, name, endPoint, apiKey, secretKey);
        return _regionDao.persist(region);
    }

    /**
     * {@inheritDoc}
     */ 
    @Override
    public Region updateRegion(int id, String name, String endPoint, String apiKey, String secretKey) {
        RegionVO region = _regionDao.findById(id);

        if(region == null){
            throw new InvalidParameterValueException("Region with id: "+id+" does not exist");
        }

        //Ensure region name is unique
        if(name != null){
            RegionVO region1 = _regionDao.findByName(name);
            if(region1 != null && id != region1.getId()){
                throw new InvalidParameterValueException("Region with name: "+name+" already exists");
            }
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

    /**
     * {@inheritDoc}
     */ 
    @Override
    public boolean removeRegion(int id) {
        RegionVO region = _regionDao.findById(id);
        if(region == null){
            throw new InvalidParameterValueException("Failed to delete Region: " + id + ", Region not found");
        }
        return _regionDao.remove(id);
    }

    /**
     * {@inheritDoc}
     */ 
    @Override
    public List<RegionVO> listRegions(Integer id, String name) {
        List<RegionVO> regions = new ArrayList<RegionVO>();
        if(id != null){
            RegionVO region = _regionDao.findById(id);
            if(region != null){
                regions.add(region);
            }
            return regions;
        }
        if(name != null){
            RegionVO region = _regionDao.findByName(name);
            if(region != null){
                regions.add(region);
            }
            return regions;
        }
        return _regionDao.listAll();
    }

    /**
     * {@inheritDoc}
     */ 
    @Override
    public boolean deleteUserAccount(long accountId) {
        AccountVO account = _accountDao.findById(accountId);
        if(account == null){
            throw new InvalidParameterValueException("The specified account does not exist in the system");
        }
        String accountUUID = account.getUuid();
        int regionId = account.getRegionId();

        String command = "deleteAccount";
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new NameValuePair(ApiConstants.ID, accountUUID));

        if(getId() == regionId){
            return _accountMgr.deleteUserAccount(accountId);
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

    /**
     * {@inheritDoc}
     */ 
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
            return _accountMgr.updateAccount(cmd);
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

    /**
     * {@inheritDoc}
     */ 
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

    /**
     * {@inheritDoc}
     */ 
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
            return _accountMgr.enableAccount(accountName, domainId, accountId);
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

    /**
     * {@inheritDoc}
     */
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
            return _accountMgr.deleteUser(cmd);
        } else {
            //First delete in the Region where user is created
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

    /**
     * {@inheritDoc}
     */
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
            return _domainMgr.updateDomain(cmd);
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

    /**
     * {@inheritDoc}
     */ 
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
        if(cleanup != null){
            params.add(new NameValuePair(ApiConstants.CLEANUP, cleanup.toString()));
        }

        int regionId = domain.getRegionId();
        if(getId() == regionId){
            return _domainMgr.deleteDomain(id, cleanup);
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

    /**
     * {@inheritDoc}
     */
    @Override
    public UserAccount updateUser(UpdateUserCmd cmd) {
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
            return _accountMgr.updateUser(cmd);
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

    /**
     * {@inheritDoc}
     */
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
            return _accountMgr.disableUser(userId);
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

    /**
     * {@inheritDoc}
     */
    @Override
    public UserAccount enableUser(long userId) {
        UserVO user = _userDao.findById(userId);
        if (user == null || user.getRemoved() != null) {
            throw new InvalidParameterValueException("Unable to find active user by id " + userId);
        }

        int regionId = user.getRegionId();

        String command = "enableUser";
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new NameValuePair(ApiConstants.ID, user.getUuid()));

        if(getId() == regionId){
            return _accountMgr.enableUser(userId);
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

}
