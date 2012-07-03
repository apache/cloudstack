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

import org.apache.log4j.Logger;

import com.cloud.api.commands.ListRegionsCmd;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.region.dao.RegionDao;
import com.cloud.utils.component.Inject;
import com.cloud.utils.component.Manager;

@Local(value = { RegionManager.class, RegionService.class })
public class RegionManagerImpl implements RegionManager, RegionService, Manager{
    public static final Logger s_logger = Logger.getLogger(RegionManagerImpl.class);
    
    @Inject
    private RegionDao _regionDao;
    
    private String _name;
    private long _id = 1; //ToDo, get this from config
    
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
	public boolean propogateAddResource() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean propogateUpdateResource() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean propogateDeleteResource() {
		// TODO Auto-generated method stub
		return false;
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
		
		return region;
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
    
}
