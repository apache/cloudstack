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
package com.cloud.configuration.dao;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.Local;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloud.configuration.Resource;
import com.cloud.configuration.Resource.ResourceOwnerType;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.configuration.ResourceCountVO;
import com.cloud.configuration.ResourceLimit;
import com.cloud.domain.dao.DomainDao;
import com.cloud.domain.dao.DomainDaoImpl;
import com.cloud.exception.UnsupportedServiceException;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.AccountDaoImpl;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;

@Component
@Local(value={ResourceCountDao.class})
public class ResourceCountDaoImpl extends GenericDaoBase<ResourceCountVO, Long> implements ResourceCountDao {
    private final SearchBuilder<ResourceCountVO> TypeSearch;

    private final SearchBuilder<ResourceCountVO> AccountSearch;
    private final SearchBuilder<ResourceCountVO> DomainSearch;

    @Inject protected DomainDao _domainDao;
    @Inject protected AccountDao _accountDao;

    public ResourceCountDaoImpl() {
        TypeSearch = createSearchBuilder();
        TypeSearch.and("type", TypeSearch.entity().getType(), SearchCriteria.Op.EQ);
        TypeSearch.and("accountId", TypeSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        TypeSearch.and("domainId", TypeSearch.entity().getDomainId(), SearchCriteria.Op.EQ);
        TypeSearch.done();

        AccountSearch = createSearchBuilder();
        AccountSearch.and("accountId", AccountSearch.entity().getAccountId(), SearchCriteria.Op.NNULL);
        AccountSearch.done();

        DomainSearch = createSearchBuilder();
        DomainSearch.and("domainId", DomainSearch.entity().getDomainId(), SearchCriteria.Op.NNULL);
        DomainSearch.done();
    }

    @Override 
    public ResourceCountVO findByOwnerAndType(long ownerId, ResourceOwnerType ownerType, ResourceType type) {
        SearchCriteria<ResourceCountVO> sc = TypeSearch.create();
        sc.setParameters("type", type);

        if (ownerType == ResourceOwnerType.Account) {
            sc.setParameters("accountId", ownerId);
            return findOneIncludingRemovedBy(sc);
        } else if (ownerType == ResourceOwnerType.Domain) {
            sc.setParameters("domainId", ownerId);
            return findOneIncludingRemovedBy(sc);
        } else {
            return null;
        }
    }

    @Override
    public long getResourceCount(long ownerId, ResourceOwnerType ownerType, ResourceType type) {
        ResourceCountVO vo = findByOwnerAndType(ownerId, ownerType, type);
        if (vo != null) {
            return vo.getCount();
        } else {
            return 0;
        }
    }

    @Override 
    public void setResourceCount(long ownerId, ResourceOwnerType ownerType, ResourceType type, long count) {
        ResourceCountVO resourceCountVO = findByOwnerAndType(ownerId, ownerType, type);
        if (count != resourceCountVO.getCount()) {
            resourceCountVO.setCount(count);
            update(resourceCountVO.getId(), resourceCountVO);
        }
    }

    @Override @Deprecated
    public void updateDomainCount(long domainId, ResourceType type, boolean increment, long delta) {
        delta = increment ? delta : delta * -1;

        ResourceCountVO resourceCountVO = findByOwnerAndType(domainId, ResourceOwnerType.Domain, type);
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
    public Set<Long> listRowsToUpdateForDomain(long domainId, ResourceType type) {
        Set<Long> rowIds = new HashSet<Long>();
        Set<Long> domainIdsToUpdate = _domainDao.getDomainParentIds(domainId);
        for (Long domainIdToUpdate : domainIdsToUpdate) {
            ResourceCountVO domainCountRecord = findByOwnerAndType(domainIdToUpdate, ResourceOwnerType.Domain, type);
            if (domainCountRecord != null) {
                rowIds.add(domainCountRecord.getId());
            }
        }
        return rowIds;
    }

    @Override
    public Set<Long> listAllRowsToUpdate(long ownerId, ResourceOwnerType ownerType, ResourceType type) {
        Set<Long> rowIds = new HashSet<Long>();

        if (ownerType == ResourceOwnerType.Account) {
            //get records for account
            ResourceCountVO accountCountRecord = findByOwnerAndType(ownerId, ResourceOwnerType.Account, type);
            if (accountCountRecord != null) {
                rowIds.add(accountCountRecord.getId());
            }

            //get records for account's domain and all its parent domains
            rowIds.addAll(listRowsToUpdateForDomain(_accountDao.findByIdIncludingRemoved(ownerId).getDomainId(),type));
        } else if (ownerType == ResourceOwnerType.Domain) {
            return listRowsToUpdateForDomain(ownerId, type);
        } 

        return rowIds;
    }

    @Override @DB
    public void createResourceCounts(long ownerId, ResourceLimit.ResourceOwnerType ownerType){

        Transaction txn = Transaction.currentTxn();
        txn.start();

        ResourceType[] resourceTypes = Resource.ResourceType.values();
        for (ResourceType resourceType : resourceTypes) {
            if (!resourceType.supportsOwner(ownerType)) {
                continue;
            }
            ResourceCountVO resourceCountVO = new ResourceCountVO(resourceType, 0, ownerId, ownerType);
            persist(resourceCountVO);
        }

        txn.commit();
    }

    private List<ResourceCountVO> listByDomainId(long domainId) {
        SearchCriteria<ResourceCountVO> sc = TypeSearch.create();
        sc.setParameters("domainId", domainId);

        return listBy(sc);
    }

    private List<ResourceCountVO> listByAccountId(long accountId) {
        SearchCriteria<ResourceCountVO> sc = TypeSearch.create();
        sc.setParameters("accountId", accountId);

        return listBy(sc);
    }

    @Override
    public List<ResourceCountVO> listByOwnerId(long ownerId, ResourceOwnerType ownerType) {
        if (ownerType == ResourceOwnerType.Account) {
            return listByAccountId(ownerId);
        } else if (ownerType == ResourceOwnerType.Domain) {
            return listByDomainId(ownerId);
        } else {
            return new ArrayList<ResourceCountVO>();
        }
    }

    @Override
    public List<ResourceCountVO> listResourceCountByOwnerType(ResourceOwnerType ownerType) {
        if (ownerType == ResourceOwnerType.Account) {
            return listBy(AccountSearch.create());
        } else if (ownerType == ResourceOwnerType.Domain) {
            return listBy(DomainSearch.create());
        } else {
            return new ArrayList<ResourceCountVO>();
        }
    }

    @Override
    public ResourceCountVO persist(ResourceCountVO resourceCountVO){
        ResourceOwnerType ownerType = resourceCountVO.getResourceOwnerType();
        ResourceType resourceType = resourceCountVO.getType();
        if (!resourceType.supportsOwner(ownerType)) {
            throw new UnsupportedServiceException("Resource type " + resourceType + " is not supported for owner of type " + ownerType.getName());
        }

        return super.persist(resourceCountVO);
    }
}