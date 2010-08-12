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

package com.cloud.configuration.dao;

import java.util.List;

import javax.ejb.Local;

import com.cloud.configuration.ResourceCount;
import com.cloud.configuration.ResourceLimitVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value={ResourceLimitDao.class})
public class ResourceLimitDaoImpl extends GenericDaoBase<ResourceLimitVO, Long> implements ResourceLimitDao {

	SearchBuilder<ResourceLimitVO> IdTypeSearch;
	
	public ResourceLimitDaoImpl () {
		IdTypeSearch = createSearchBuilder();
		IdTypeSearch.and("type", IdTypeSearch.entity().getType(), SearchCriteria.Op.EQ);
	    IdTypeSearch.and("domainId", IdTypeSearch.entity().getDomainId(), SearchCriteria.Op.EQ);
	    IdTypeSearch.and("accountId", IdTypeSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
	    IdTypeSearch.done();
	}
	
	public ResourceLimitVO findByDomainIdAndType(Long domainId, ResourceCount.ResourceType type) {
		if (domainId == null || type == null)
			return null;
		
		SearchCriteria sc = IdTypeSearch.create();
		sc.setParameters("domainId", domainId);
		sc.setParameters("type", type);
		
		return findOneBy(sc);
	}
	
	public List<ResourceLimitVO> listByDomainId(Long domainId) {
		if (domainId == null)
			return null;
		
		SearchCriteria sc = IdTypeSearch.create();
		sc.setParameters("domainId", domainId);
		
		return listBy(sc);
	}
	
	public ResourceLimitVO findByAccountIdAndType(Long accountId, ResourceCount.ResourceType type) {
		if (accountId == null || type == null)
			return null;
	
		SearchCriteria sc = IdTypeSearch.create();
		sc.setParameters("accountId", accountId);
		sc.setParameters("type", type);
		
		return findOneBy(sc);
	}
	
	public List<ResourceLimitVO> listByAccountId(Long accountId) {
		if (accountId == null)
			return null;
	
		SearchCriteria sc = IdTypeSearch.create();
		sc.setParameters("accountId", accountId);
		
		return listBy(sc);
	}
	
	public boolean update(Long id, Long max) {
        ResourceLimitVO limit = findById(id);
        if (max != null)
        	limit.setMax(max);
        else
        	limit.setMax(new Long(-1));
        return update(id, limit);
    }
	
	public ResourceCount.ResourceType getLimitType(String type) {
		ResourceCount.ResourceType[] validTypes = ResourceCount.ResourceType.values();
		
		for (ResourceCount.ResourceType validType : validTypes) {
			if (validType.toString().equals(type)) {
				return validType;
			}
		}
		
		return null;
	}
}
