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

import javax.ejb.Local;

import com.cloud.configuration.ResourceCount.ResourceType;
import com.cloud.configuration.ResourceCountVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value={ResourceCountDao.class})
public class ResourceCountDaoImpl extends GenericDaoBase<ResourceCountVO, Long> implements ResourceCountDao {
	private SearchBuilder<ResourceCountVO> IdTypeSearch;
	private SearchBuilder<ResourceCountVO> DomainIdTypeSearch;

	public ResourceCountDaoImpl() {
		IdTypeSearch = createSearchBuilder();
		IdTypeSearch.and("type", IdTypeSearch.entity().getType(), SearchCriteria.Op.EQ);
	    IdTypeSearch.and("accountId", IdTypeSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
	    IdTypeSearch.done();

		DomainIdTypeSearch = createSearchBuilder();
		DomainIdTypeSearch.and("type", DomainIdTypeSearch.entity().getType(), SearchCriteria.Op.EQ);
		DomainIdTypeSearch.and("domainId", DomainIdTypeSearch.entity().getDomainId(), SearchCriteria.Op.EQ);
		DomainIdTypeSearch.done();
	}

	private ResourceCountVO findByAccountIdAndType(long accountId, ResourceType type) {
		if (type == null) {
			return null;
		}

		SearchCriteria<ResourceCountVO> sc = IdTypeSearch.create();
		sc.setParameters("accountId", accountId);
		sc.setParameters("type", type);

		return findOneBy(sc);
	}

	private ResourceCountVO findByDomainIdAndType(long domainId, ResourceType type) {
		if (type == null) {
			return null;
		}

		SearchCriteria<ResourceCountVO> sc = DomainIdTypeSearch.create();
		sc.setParameters("domainId", domainId);
		sc.setParameters("type", type);

		return findOneBy(sc);
	}

	@Override
	public long getAccountCount(long accountId, ResourceType type) {
		ResourceCountVO resourceCountVO = findByAccountIdAndType(accountId, type);
		return (resourceCountVO != null) ? resourceCountVO.getCount() : 0;
	}

	@Override
	public long getDomainCount(long domainId, ResourceType type) {
		ResourceCountVO resourceCountVO = findByDomainIdAndType(domainId, type);
		return (resourceCountVO != null) ? resourceCountVO.getCount() : 0;
	}

	@Override
	public void updateAccountCount(long accountId, ResourceType type, boolean increment, long delta) {
        delta = increment ? delta : delta * -1;

        ResourceCountVO resourceCountVO = findByAccountIdAndType(accountId, type);

		if (resourceCountVO == null) {
			resourceCountVO = new ResourceCountVO(accountId, null, type, 0);
			resourceCountVO.setCount(resourceCountVO.getCount() + delta);
			persist(resourceCountVO);
		} else {
			resourceCountVO.setCount(resourceCountVO.getCount() + delta);
			update(resourceCountVO.getId(), resourceCountVO);
		}
	}

	@Override
	public void updateDomainCount(long domainId, ResourceType type, boolean increment, long delta) {
		delta = increment ? delta : delta * -1;

        ResourceCountVO resourceCountVO = findByDomainIdAndType(domainId, type);
		if (resourceCountVO == null) {
			resourceCountVO = new ResourceCountVO(null, domainId, type, 0);
			resourceCountVO.setCount(resourceCountVO.getCount() + delta);
			persist(resourceCountVO);
		} else {
			resourceCountVO.setCount(resourceCountVO.getCount() + delta);
			update(resourceCountVO.getId(), resourceCountVO);	
		}
	}
}