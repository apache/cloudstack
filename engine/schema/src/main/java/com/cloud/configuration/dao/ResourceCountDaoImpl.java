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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloud.configuration.Resource;
import com.cloud.configuration.Resource.ResourceOwnerType;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.configuration.ResourceCountVO;
import com.cloud.configuration.ResourceLimit;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.UnsupportedServiceException;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;

@Component
public class ResourceCountDaoImpl extends GenericDaoBase<ResourceCountVO, Long> implements ResourceCountDao {
    private final SearchBuilder<ResourceCountVO> TypeSearch;

    private final SearchBuilder<ResourceCountVO> AccountSearch;
    private final SearchBuilder<ResourceCountVO> DomainSearch;

    @Inject
    private DomainDao _domainDao;
    @Inject
    private AccountDao _accountDao;

    public ResourceCountDaoImpl() {
        TypeSearch = createSearchBuilder();
        TypeSearch.and("type", TypeSearch.entity().getType(), SearchCriteria.Op.EQ);
        TypeSearch.and("accountId", TypeSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        TypeSearch.and("domainId", TypeSearch.entity().getDomainId(), SearchCriteria.Op.EQ);
        TypeSearch.done();

        AccountSearch = createSearchBuilder();
        DomainSearch = createSearchBuilder();
    }

    @PostConstruct
    protected void configure() {
        AccountSearch.and("accountId", AccountSearch.entity().getAccountId(), SearchCriteria.Op.NNULL);
        SearchBuilder<AccountVO> joinAccount = _accountDao.createSearchBuilder();
        joinAccount.and("notremoved", joinAccount.entity().getRemoved(), SearchCriteria.Op.NULL);
        AccountSearch.join("account", joinAccount, AccountSearch.entity().getAccountId(), joinAccount.entity().getId(), JoinBuilder.JoinType.INNER);
        AccountSearch.done();

        DomainSearch.and("domainId", DomainSearch.entity().getDomainId(), SearchCriteria.Op.NNULL);
        SearchBuilder<DomainVO> joinDomain = _domainDao.createSearchBuilder();
        joinDomain.and("notremoved", joinDomain.entity().getRemoved(), SearchCriteria.Op.NULL);
        DomainSearch.join("domain", joinDomain, DomainSearch.entity().getDomainId(), joinDomain.entity().getId(), JoinBuilder.JoinType.INNER);
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
        if (resourceCountVO != null && count != resourceCountVO.getCount()) {
            resourceCountVO.setCount(count);
            update(resourceCountVO.getId(), resourceCountVO);
        }
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
            rowIds.addAll(listRowsToUpdateForDomain(_accountDao.findByIdIncludingRemoved(ownerId).getDomainId(), type));
        } else if (ownerType == ResourceOwnerType.Domain) {
            return listRowsToUpdateForDomain(ownerId, type);
        }

        return rowIds;
    }

    @Override
    @DB
    public void createResourceCounts(long ownerId, ResourceLimit.ResourceOwnerType ownerType) {

        TransactionLegacy txn = TransactionLegacy.currentTxn();
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
    public ResourceCountVO persist(ResourceCountVO resourceCountVO) {
        ResourceOwnerType ownerType = resourceCountVO.getResourceOwnerType();
        ResourceType resourceType = resourceCountVO.getType();
        if (!resourceType.supportsOwner(ownerType)) {
            throw new UnsupportedServiceException("Resource type " + resourceType + " is not supported for owner of type " + ownerType.getName());
        }

        return super.persist(resourceCountVO);
    }

    @Override
    public long removeEntriesByOwner(long ownerId, ResourceOwnerType ownerType) {
        SearchCriteria<ResourceCountVO> sc = TypeSearch.create();

        if (ownerType == ResourceOwnerType.Account) {
            sc.setParameters("accountId", ownerId);
            return remove(sc);
        } else if (ownerType == ResourceOwnerType.Domain) {
            sc.setParameters("domainId", ownerId);
            return remove(sc);
        }
        return 0;
    }

    private String baseSqlCountComputingResourceAllocatedToAccount = "Select "
            + " SUM((CASE "
            + "        WHEN so.%s is not null THEN so.%s "
            + "        ELSE CONVERT(vmd.value, UNSIGNED INTEGER) "
            + "    END)) as total "
            + " from vm_instance vm "
            + " join service_offering so on so.id = vm.service_offering_id "
            + " left join user_vm_details vmd on vmd.vm_id = vm.id and vmd.name = '%s' "
            + " where vm.type = 'User' and state not in ('Destroyed', 'Error', 'Expunging') and display_vm = true and account_id = ? ";

    @Override
    public long countCpuNumberAllocatedToAccount(long accountId) {
        String sqlCountCpuNumberAllocatedToAccount = String.format(baseSqlCountComputingResourceAllocatedToAccount, ResourceType.cpu, ResourceType.cpu, "cpuNumber");
        return executeSqlCountComputingResourcesForAccount(accountId, sqlCountCpuNumberAllocatedToAccount);
    }

    @Override
    public long countMemoryAllocatedToAccount(long accountId) {
        String serviceOfferingRamSizeField = "ram_size";
        String sqlCountCpuNumberAllocatedToAccount = String.format(baseSqlCountComputingResourceAllocatedToAccount, serviceOfferingRamSizeField, serviceOfferingRamSizeField, "memory");
        return executeSqlCountComputingResourcesForAccount(accountId, sqlCountCpuNumberAllocatedToAccount);
    }

    private long executeSqlCountComputingResourcesForAccount(long accountId, String sqlCountComputingResourcesAllocatedToAccount) {
        TransactionLegacy tx = TransactionLegacy.currentTxn();
        try {
            PreparedStatement pstmt = tx.prepareAutoCloseStatement(sqlCountComputingResourcesAllocatedToAccount);
            pstmt.setLong(1, accountId);

            ResultSet rs = pstmt.executeQuery();
            if (!rs.next()) {
                return 0L;
            }
            return rs.getLong("total");
        } catch (SQLException e) {
            throw new CloudRuntimeException(e);
        }
    }

}
