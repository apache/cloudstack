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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.cloud.configuration.Resource;
import com.cloud.configuration.Resource.ResourceOwnerType;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.configuration.ResourceCountVO;
import com.cloud.configuration.ResourceLimit;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.user.AccountVO;
import com.cloud.user.ResourceLimitService;
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
    private final SearchBuilder<ResourceCountVO> TypeNullTagSearch;
    private final SearchBuilder<ResourceCountVO> NonMatchingTagsSearch;
    private final SearchBuilder<ResourceCountVO> AccountSearch;
    private final SearchBuilder<ResourceCountVO> DomainSearch;
    private final SearchBuilder<ResourceCountVO> IdsSearch;

    @Inject
    private DomainDao _domainDao;
    @Inject
    private AccountDao _accountDao;

    protected static final String INCREMENT_COUNT_BY_IDS_SQL = "UPDATE `cloud`.`resource_count` SET `count` = `count` + ? WHERE `id` IN (?)";
    protected static final String DECREMENT_COUNT_BY_IDS_SQL = "UPDATE `cloud`.`resource_count` SET `count` = `count` - ? WHERE `id` IN (?)";

    public ResourceCountDaoImpl() {
        TypeSearch = createSearchBuilder();
        TypeSearch.and("type", TypeSearch.entity().getType(), SearchCriteria.Op.EQ);
        TypeSearch.and("accountId", TypeSearch.entity().getAccountId(), SearchCriteria.Op.IN);
        TypeSearch.and("domainId", TypeSearch.entity().getDomainId(), SearchCriteria.Op.IN);
        TypeSearch.and("tag", TypeSearch.entity().getTag(), SearchCriteria.Op.EQ);
        TypeSearch.done();

        TypeNullTagSearch = createSearchBuilder();
        TypeNullTagSearch.and("type", TypeNullTagSearch.entity().getType(), SearchCriteria.Op.EQ);
        TypeNullTagSearch.and("accountId", TypeNullTagSearch.entity().getAccountId(), SearchCriteria.Op.IN);
        TypeNullTagSearch.and("domainId", TypeNullTagSearch.entity().getDomainId(), SearchCriteria.Op.IN);
        TypeNullTagSearch.and("tag", TypeNullTagSearch.entity().getTag(), SearchCriteria.Op.NULL);
        TypeNullTagSearch.done();

        NonMatchingTagsSearch = createSearchBuilder();
        NonMatchingTagsSearch.and("accountId", NonMatchingTagsSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        NonMatchingTagsSearch.and("domainId", NonMatchingTagsSearch.entity().getDomainId(), SearchCriteria.Op.EQ);
        NonMatchingTagsSearch.and("types", NonMatchingTagsSearch.entity().getType(), SearchCriteria.Op.IN);
        NonMatchingTagsSearch.and("tagNotNull", NonMatchingTagsSearch.entity().getTag(), SearchCriteria.Op.NNULL);
        NonMatchingTagsSearch.and("tags", NonMatchingTagsSearch.entity().getTag(), SearchCriteria.Op.NIN);
        NonMatchingTagsSearch.done();

        AccountSearch = createSearchBuilder();
        DomainSearch = createSearchBuilder();

        IdsSearch = createSearchBuilder();
        IdsSearch.and("id", IdsSearch.entity().getId(), SearchCriteria.Op.IN);
        IdsSearch.done();
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
    public ResourceCountVO findByOwnerAndTypeAndTag(long ownerId, ResourceOwnerType ownerType, ResourceType type, String tag) {
        List<ResourceCountVO> resourceCounts = findByOwnersAndTypeAndTag(List.of(ownerId), ownerType, type, tag);
        if (CollectionUtils.isNotEmpty(resourceCounts)) {
            return resourceCounts.get(0);
        } else {
            return null;
        }
    }

    @Override
    public List<ResourceCountVO> findByOwnersAndTypeAndTag(List<Long> ownerIdList, ResourceOwnerType ownerType, ResourceType type, String tag) {
        if (CollectionUtils.isEmpty(ownerIdList)) {
            return new ArrayList<>();
        }
        SearchCriteria<ResourceCountVO> sc = tag != null ? TypeSearch.create() : TypeNullTagSearch.create();
        sc.setParameters("type", type);
        if (tag != null) {
            sc.setParameters("tag", tag);
        }

        if (ownerType == ResourceOwnerType.Account) {
            sc.setParameters("accountId", ownerIdList.toArray());
            return listIncludingRemovedBy(sc);
        } else if (ownerType == ResourceOwnerType.Domain) {
            sc.setParameters("domainId", ownerIdList.toArray());
            return listIncludingRemovedBy(sc);
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public long getResourceCount(long ownerId, ResourceOwnerType ownerType, ResourceType type, String tag) {
        ResourceCountVO vo = findByOwnerAndTypeAndTag(ownerId, ownerType, type, tag);
        if (vo != null) {
            return vo.getCount();
        } else {
            return 0;
        }
    }

    @Override
    public void setResourceCount(long ownerId, ResourceOwnerType ownerType, ResourceType type, String tag, long count) {
        ResourceCountVO resourceCountVO = findByOwnerAndTypeAndTag(ownerId, ownerType, type, tag);
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
    public boolean updateCountByDeltaForIds(List<Long> ids, boolean increment, long delta) {
        if (CollectionUtils.isEmpty(ids)) {
            return false;
        }
        String updateSql = increment ? INCREMENT_COUNT_BY_IDS_SQL : DECREMENT_COUNT_BY_IDS_SQL;

        String poolIdsInStr = ids.stream().map(String::valueOf).collect(Collectors.joining(",", "(", ")"));
        String sql = updateSql.replace("(?)", poolIdsInStr);

        final TransactionLegacy txn = TransactionLegacy.currentTxn();
        try(PreparedStatement pstmt = txn.prepareStatement(sql);) {
            pstmt.setLong(1, delta);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    public Set<Long> listRowsToUpdateForDomain(long domainId, ResourceType type, String tag) {
        Set<Long> rowIds = new HashSet<Long>();
        Set<Long> domainIdsToUpdate = _domainDao.getDomainParentIds(domainId);
        for (Long domainIdToUpdate : domainIdsToUpdate) {
            ResourceCountVO domainCountRecord = findByOwnerAndTypeAndTag(domainIdToUpdate, ResourceOwnerType.Domain, type, tag);
            if (domainCountRecord != null) {
                rowIds.add(domainCountRecord.getId());
            } else {
                if (StringUtils.isNotEmpty(tag)) {
                    ResourceCountVO resourceCountVO = createTaggedResourceCount(domainIdToUpdate, ResourceOwnerType.Domain, type, tag);
                    rowIds.add(resourceCountVO.getId());
                }
            }
        }
        return rowIds;
    }

    @Override
    public Set<Long> listAllRowsToUpdate(long ownerId, ResourceOwnerType ownerType, ResourceType type, String tag) {
        Set<Long> rowIds = new HashSet<Long>();

        if (ownerType == ResourceOwnerType.Account) {
            //get records for account
            ResourceCountVO accountCountRecord = findByOwnerAndTypeAndTag(ownerId, ResourceOwnerType.Account, type, tag);
            if (accountCountRecord != null) {
                rowIds.add(accountCountRecord.getId());
            } else {
                if (StringUtils.isNotEmpty(tag)) {
                    ResourceCountVO resourceCountVO = createTaggedResourceCount(ownerId, ownerType, type, tag);
                    rowIds.add(resourceCountVO.getId());
                }
            }

            //get records for account's domain and all its parent domains
            rowIds.addAll(listRowsToUpdateForDomain(_accountDao.findByIdIncludingRemoved(ownerId).getDomainId(), type, tag));
        } else if (ownerType == ResourceOwnerType.Domain) {
            rowIds = listRowsToUpdateForDomain(ownerId, type, tag);
        }

        return rowIds;
    }

    protected ResourceCountVO createTaggedResourceCount(long ownerId, ResourceLimit.ResourceOwnerType ownerType, ResourceType resourceType, String tag) {
        ResourceCountVO taggedResourceCountVO = new ResourceCountVO(resourceType, 0, ownerId, ownerType, tag);
        return persist(taggedResourceCountVO);
    }

    protected void createTaggedResourceCounts(long ownerId, ResourceLimit.ResourceOwnerType ownerType, ResourceType resourceType, List<String> tags) {
        for (String tag : tags) {
            createTaggedResourceCount(ownerId, ownerType, resourceType, tag);
        }
    }

    @Override
    @DB
    public void createResourceCounts(long ownerId, ResourceLimit.ResourceOwnerType ownerType) {

        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();

        ResourceType[] resourceTypes = Resource.ResourceType.values();
        List<String> hostTags = new ArrayList<>();
        if (StringUtils.isNotEmpty(ResourceLimitService.ResourceLimitHostTags.value())) {
            hostTags = Arrays.asList(ResourceLimitService.ResourceLimitHostTags.value().split(","));
        }
        List<String> storageTags = new ArrayList<>();
        if (StringUtils.isNotEmpty(ResourceLimitService.ResourceLimitStorageTags.value())) {
            storageTags = Arrays.asList(ResourceLimitService.ResourceLimitStorageTags.value().split(","));
        }
        for (ResourceType resourceType : resourceTypes) {
            ResourceCountVO resourceCountVO = new ResourceCountVO(resourceType, 0, ownerId, ownerType);
            persist(resourceCountVO);
            if (ResourceLimitService.HostTagsSupportingTypes.contains(resourceType)) {
                createTaggedResourceCounts(ownerId, ownerType, resourceType, hostTags);
            }
            if (ResourceLimitService.StorageTagsSupportingTypes.contains(resourceType)) {
                createTaggedResourceCounts(ownerId, ownerType, resourceType, storageTags);
            }
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

    @Override
    public void removeResourceCountsForNonMatchingTags(Long ownerId, ResourceOwnerType ownerType, List<ResourceType> types, List<String> tags) {
        SearchCriteria<ResourceCountVO> sc = NonMatchingTagsSearch.create();
        if (ObjectUtils.allNotNull(ownerId, ownerType)) {
            if (ResourceOwnerType.Account.equals(ownerType)) {
                sc.setParameters("accountId", ownerId);
            } else {
                sc.setParameters("domainId", ownerId);
            }
        }
        if (CollectionUtils.isNotEmpty(types)) {
            sc.setParameters("types", types.stream().map(ResourceType::getName).toArray());
        }
        if (CollectionUtils.isNotEmpty(tags)) {
            sc.setParameters("tags", tags.toArray());
        }
        remove(sc);
    }

    @Override
    public List<ResourceCountVO> lockRows(Set<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return new ArrayList<>();
        }
        SearchCriteria<ResourceCountVO> sc = IdsSearch.create();
        sc.setParameters("id", ids.toArray());
        return lockRows(sc, null, true);
    }
}
