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
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.DomainManager;
import com.cloud.user.UserAccount;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserAccountDao;
import com.cloud.utils.component.Manager;
import com.cloud.utils.component.ManagerBase;
import org.apache.cloudstack.api.command.admin.account.UpdateAccountCmd;
import org.apache.cloudstack.api.command.admin.domain.UpdateDomainCmd;
import org.apache.cloudstack.api.command.admin.user.DeleteUserCmd;
import org.apache.cloudstack.api.command.admin.user.UpdateUserCmd;
import org.apache.cloudstack.region.dao.RegionDao;
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
    private DomainManager _domainMgr;

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
    public Region addRegion(int id, String name, String endPoint) {
        //Region Id should be unique
        if( _regionDao.findById(id) != null ){
            throw new InvalidParameterValueException("Region with id: "+id+" already exists");
        }
        //Region Name should be unique
        if( _regionDao.findByName(name) != null ){
            throw new InvalidParameterValueException("Region with name: "+name+" already exists");
        }
        RegionVO region = new RegionVO(id, name, endPoint);
        return _regionDao.persist(region);
    }

    /**
     * {@inheritDoc}
     */ 
    @Override
    public Region updateRegion(int id, String name, String endPoint) {
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
        return _accountMgr.deleteUserAccount(accountId);
    }

    /**
     * {@inheritDoc}
     */ 
    @Override
    public Account updateAccount(UpdateAccountCmd cmd) {
        return _accountMgr.updateAccount(cmd);
    }

    /**
     * {@inheritDoc}
     */ 
    @Override
    public Account disableAccount(String accountName, Long domainId, Long accountId, Boolean lockRequested) throws ConcurrentOperationException, ResourceUnavailableException {
        Account account = null;
        if(lockRequested){
            account = _accountMgr.lockAccount(accountName, domainId, accountId);
        } else {
            account = _accountMgr.disableAccount(accountName, domainId, accountId);
        }
        return account;
    }

    /**
     * {@inheritDoc}
     */ 
    @Override
    public Account enableAccount(String accountName, Long domainId, Long accountId) {
        return _accountMgr.enableAccount(accountName, domainId, accountId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean deleteUser(DeleteUserCmd cmd) {
        return _accountMgr.deleteUser(cmd);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Domain updateDomain(UpdateDomainCmd cmd) {
        return _domainMgr.updateDomain(cmd);
    }

    /**
     * {@inheritDoc}
     */ 
    @Override
    public boolean deleteDomain(Long id, Boolean cleanup) {
        return _domainMgr.deleteDomain(id, cleanup);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserAccount updateUser(UpdateUserCmd cmd) {
        return _accountMgr.updateUser(cmd);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserAccount disableUser(Long userId) {
        return _accountMgr.disableUser(userId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserAccount enableUser(long userId) {
        return _accountMgr.enableUser(userId);
    }

}
