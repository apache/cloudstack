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

import java.util.HashSet;
import java.util.Set;

import javax.ejb.Local;

import com.cloud.configuration.ResourceCount.ResourceType;
import com.cloud.configuration.ResourceCount;
import com.cloud.configuration.ResourceCountVO;
import com.cloud.configuration.ResourceLimit;
import com.cloud.domain.dao.DomainDaoImpl;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;

@Local(value={ResourceCountDao.class})
public class ResourceCountDaoImpl extends GenericDaoBase<ResourceCountVO, Long> implements ResourceCountDao {
	private SearchBuilder<ResourceCountVO> IdTypeSearch;
	private SearchBuilder<ResourceCountVO> DomainIdTypeSearch;
	
	protected final DomainDaoImpl _domainDao = ComponentLocator.inject(DomainDaoImpl.class);

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

	@Override
	public ResourceCountVO findByAccountIdAndType(long accountId, ResourceType type) {
		SearchCriteria<ResourceCountVO> sc = IdTypeSearch.create();
		sc.setParameters("accountId", accountId);
		sc.setParameters("type", type);

		return findOneIncludingRemovedBy(sc);
	}

	@Override
	public ResourceCountVO findByDomainIdAndType(long domainId, ResourceType type) {
		SearchCriteria<ResourceCountVO> sc = DomainIdTypeSearch.create();
		sc.setParameters("domainId", domainId);
		sc.setParameters("type", type);

		return findOneIncludingRemovedBy(sc);
	}

	@Override
	public long getAccountCount(long accountId, ResourceType type) {
		ResourceCountVO resourceCountVO = findByAccountIdAndType(accountId, type);
		return resourceCountVO.getCount();
	}

	@Override
	public long getDomainCount(long domainId, ResourceType type) {
		ResourceCountVO resourceCountVO = findByDomainIdAndType(domainId, type);
		return resourceCountVO.getCount();
	}

	@Override
	public void setAccountCount(long accountId, ResourceType type, long count) {
		ResourceCountVO resourceCountVO = findByAccountIdAndType(accountId, type);
		if (count != resourceCountVO.getCount()) {
			resourceCountVO.setCount(count);
			update(resourceCountVO.getId(), resourceCountVO);
		}
	}

	@Override
	public void setDomainCount(long domainId, ResourceType type, long count) {
		ResourceCountVO resourceCountVO = findByDomainIdAndType(domainId, type);
		if (count != resourceCountVO.getCount()) {
			resourceCountVO.setCount(count);
			update(resourceCountVO.getId(), resourceCountVO);
		}
	}

	@Override
	public void updateDomainCount(long domainId, ResourceType type, boolean increment, long delta) {
		delta = increment ? delta : delta * -1;

        ResourceCountVO resourceCountVO = findByDomainIdAndType(domainId, type);
		resourceCountVO.setCount(resourceCountVO.getCount() + delta);
		update(resourceCountVO.getId(), resourceCountVO);	
	}
	
	@Override
	public boolean updateById(long id, boolean increment, long delta) {
	    delta = increment ? delta : delta * -1;
	    
	    ResourceCountVO resourceCountVO = findById(id);
	    resourceCountVO.setCount(resourceCountVO.getCount() + delta);
	    return update(resourceCountVO.getId(), resourceCountVO);
	}
	
	@Override
	public Set<Long> listAllRowsToUpdateForAccount(long accountId, long domainId, ResourceType type) {
	    Set<Long> rowIds = new HashSet<Long>();
	    //Create resource count records if not exist
        //1) for account
        ResourceCountVO accountCountRecord = findByAccountIdAndType(accountId, type);
        rowIds.add(accountCountRecord.getId());
        
        //2) for domain(s)
        rowIds.addAll(listRowsToUpdateForDomain(domainId, type));
        
        return rowIds;
	}
	
	@Override
	public Set<Long> listRowsToUpdateForDomain(long domainId, ResourceType type) {
	    Set<Long> rowIds = new HashSet<Long>();
	    Set<Long> domainIdsToUpdate = _domainDao.getDomainParentIds(domainId);
        for (Long domainIdToUpdate : domainIdsToUpdate) {
            ResourceCountVO domainCountRecord = findByDomainIdAndType(domainIdToUpdate, type);
            rowIds.add(domainCountRecord.getId());
        }
        return rowIds;
	}
	
	@Override @DB
    public void createResourceCounts(long ownerId, ResourceLimit.OwnerType ownerType){
        Long accountId = null;
        Long domainId = null;
        if (ownerType == ResourceLimit.OwnerType.Account) {
            accountId = ownerId;
        } else if (ownerType == ResourceLimit.OwnerType.Domain) {
            domainId = ownerId;
        }
        
        Transaction txn = Transaction.currentTxn();
        txn.start();
        
        ResourceType[] resourceTypes = ResourceCount.ResourceType.values();
        for (ResourceType resourceType : resourceTypes) {
            ResourceCountVO resourceCountVO = new ResourceCountVO(accountId, domainId, resourceType, 0);
            persist(resourceCountVO);
        }
        
        txn.commit();
    }
}