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

import com.cloud.api.commands.ListRegionsCmd;
import com.cloud.api.commands.UpdateAccountCmd;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.region.dao.RegionDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.component.Inject;
import com.cloud.utils.component.Manager;
import com.cloud.utils.exception.CloudRuntimeException;

@Local(value = { RegionManager.class, RegionService.class })
public class RegionManagerImpl implements RegionManager, RegionService, Manager{
    public static final Logger s_logger = Logger.getLogger(RegionManagerImpl.class);
    
    @Inject
    private RegionDao _regionDao;
    @Inject
    private AccountDao _accountDao;
    @Inject
    private AccountManager _accountMgr;
    
    private String _name;
    private long _id = 1; //ToDo, get this from config or db.properties
    
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
			String accountName, short accountType, Long domainId, String networkDomain, Map<String, String> details, String accountUUID, String userUUID, long regionId) {
		List<RegionVO> regions =  _regionDao.listAll();
		StringBuffer params = new StringBuffer("/api?command=createAccount");
		params.append("&username="+userName);
		params.append("&password="+password);
		params.append("&firstname="+firstName);
		params.append("&lastname="+lastName);
		params.append("&email="+email);
		if(timezone != null){
			params.append("&timezone="+timezone);		
		}
		if(accountName != null){
			params.append("&account="+accountName);			
		}
		params.append("&accounttype="+accountType);
		if(domainId != null){
			params.append("&domainid="+domainId); //use UUID			
		}
		if(networkDomain != null){
			params.append("&networkdomain="+networkDomain);			
		}
		if(details != null){
			params.append("&accountdetails="+details); //ToDo change to Map
		}
		params.append("&accountid="+accountUUID);
		params.append("&userid="+userUUID);
		params.append("&regionid="+regionId);
		
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
		//Check null account
		String accountUUID = account.getUuid();
		long regionId = account.getRegionId();
		String params = "/api?command=deleteAccount&id="+accountUUID+"&ispropagate=true";
		if(getId() == regionId){
			if(_accountMgr.deleteUserAccount(accountId)){
				List<RegionVO> regions =  _regionDao.listAll();
				for (Region region : regions){
					if(region.getId() == getId()){
						continue;
					}
					String url = region.getEndPoint() + params;
					if (makeAPICall(url)) {
						s_logger.debug("Successfully deleted account :"+accountUUID+" in Region: "+region.getId());
					} else {
						s_logger.error("Error while deleted account :"+accountUUID+" in Region: "+region.getId());
					}
				}
				return true;
			} else {
				return false;
			}
		} else {
			//First delete in the Region where account is created
			params = "/api?command=deleteAccount&id="+accountUUID;
			Region region = _regionDao.findById(regionId);
			String url = region.getEndPoint() + params;
			if (makeAPICall(url)) {
				s_logger.debug("Successfully deleted account :"+accountUUID+" in Region: "+region.getId());
				return true;
			} else {
				s_logger.error("Error while deleted account :"+accountUUID+" in Region: "+region.getId());
				return false;
			}
		}
	}
	
	@Override
	public Account updateAccount(UpdateAccountCmd cmd) {
        Long accountId = cmd.getId();
        Long domainId = cmd.getDomainId();
        String accountName = cmd.getAccountName();
        String newAccountName = cmd.getNewName();
        String networkDomain = cmd.getNetworkDomain();
        Map<String, String> details = cmd.getDetails();
        
        AccountVO account = _accountDao.findById(accountId);
		//Check null account
		String accountUUID = account.getUuid();
		long regionId = account.getRegionId();
		if(getId() == regionId){
			Account updateAccount = _accountMgr.updateAccount(cmd);
			if(updateAccount != null){
				List<RegionVO> regions =  _regionDao.listAll();
				StringBuffer params = new StringBuffer("/api?command=updateAccount"+"&ispropagate=true");
				for (Region region : regions){
					if(region.getId() == getId()){
						continue;
					}
					String url = region.getEndPoint() + params;
					if (makeAPICall(url)) {
						s_logger.debug("Successfully updated account :"+accountUUID+" in Region: "+region.getId());
					} else {
						s_logger.error("Error while updated account :"+accountUUID+" in Region: "+region.getId());
					}
				}
			}
			return updateAccount;
		} else {
			//First update in the Region where account is created
			StringBuffer params = new StringBuffer("/api?command=updateAccount");
			//add params
			Region region = _regionDao.findById(regionId);
			String url = region.getEndPoint() + params;
			if (makeAPICall(url)) {
				s_logger.debug("Successfully updated account :"+accountUUID+" in source Region: "+region.getId());
				//return Account object
				return null;
			} else {
				s_logger.error("Error while updated account :"+accountUUID+" in source Region: "+region.getId());
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
	public boolean addResource() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean updateResource() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean deleteResource() {
		// TODO Auto-generated method stub
		return false;
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
	public Account lockAccount(String accountName, Long domainId, Long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Account disableAccount(String accountName, Long domainId, Long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Account enableAccount(String accountName, Long domainId, Long id) {
		// TODO Auto-generated method stub
		return null;
	}

}
