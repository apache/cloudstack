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
package com.cloud.uuididentity;

import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import com.cloud.api.IdentityMapper;
import com.cloud.api.IdentityService;
import com.cloud.utils.component.Inject;
import com.cloud.utils.component.Manager;
import com.cloud.uuididentity.dao.IdentityDao;

@Local(value = { IdentityService.class })
public class IdentityServiceImpl implements Manager, IdentityService {
    private String _name;
	
	@Inject	private IdentityDao _identityDao;
	
	public Long getIdentityId(IdentityMapper mapper, String identityString) {
		return _identityDao.getIdentityId(mapper, identityString);
	}
	
    public Long getIdentityId(String tableName, String identityString) {
		return _identityDao.getIdentityId(tableName, identityString);
    }
	
	public String getIdentityUuid(String tableName, String identityString) {
		return _identityDao.getIdentityUuid(tableName, identityString);
	}

	@Override
	public boolean configure(String name, Map<String, Object> params)
			throws ConfigurationException {
		_name = name;
		
		return true;
	}

	@Override
	public String getName() {
		return _name;
	}

	@Override
	public boolean start() {
		return true;
	}

	@Override
	public boolean stop() {
		return true;
	}
}
