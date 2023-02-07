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
package com.cloud.domain.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.user.Account;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;

@Component
public class DomainDaoImpl extends GenericDaoBase<DomainVO, Long> implements DomainDao {
    private static final Logger s_logger = Logger.getLogger(DomainDaoImpl.class);

    protected SearchBuilder<DomainVO> DomainNameLikeSearch;
    protected SearchBuilder<DomainVO> ParentDomainNameLikeSearch;
    protected SearchBuilder<DomainVO> DomainPairSearch;
    protected SearchBuilder<DomainVO> ImmediateChildDomainSearch;
    protected SearchBuilder<DomainVO> FindAllChildrenSearch;
    protected GenericSearchBuilder<DomainVO, Long> FindIdsOfAllChildrenSearch;
    protected SearchBuilder<DomainVO> AllFieldsSearch;

    public DomainDaoImpl() {
        DomainNameLikeSearch = createSearchBuilder();
        DomainNameLikeSearch.and("name", DomainNameLikeSearch.entity().getName(), SearchCriteria.Op.LIKE);
        DomainNameLikeSearch.done();

        ParentDomainNameLikeSearch = createSearchBuilder();
        ParentDomainNameLikeSearch.and("name", ParentDomainNameLikeSearch.entity().getName(), SearchCriteria.Op.LIKE);
        ParentDomainNameLikeSearch.and("parent", ParentDomainNameLikeSearch.entity().getName(), SearchCriteria.Op.EQ);
        ParentDomainNameLikeSearch.done();

        DomainPairSearch = createSearchBuilder();
        DomainPairSearch.and("id", DomainPairSearch.entity().getId(), SearchCriteria.Op.IN);
        DomainPairSearch.done();

        ImmediateChildDomainSearch = createSearchBuilder();
        ImmediateChildDomainSearch.and("parent", ImmediateChildDomainSearch.entity().getParent(), SearchCriteria.Op.EQ);
        ImmediateChildDomainSearch.done();

        FindAllChildrenSearch = createSearchBuilder();
        FindAllChildrenSearch.and("path", FindAllChildrenSearch.entity().getPath(), SearchCriteria.Op.LIKE);
        FindAllChildrenSearch.and("id", FindAllChildrenSearch.entity().getId(), SearchCriteria.Op.NEQ);
        FindAllChildrenSearch.done();

        FindIdsOfAllChildrenSearch = createSearchBuilder(Long.class);
        FindIdsOfAllChildrenSearch.selectFields(FindIdsOfAllChildrenSearch.entity().getId());
        FindIdsOfAllChildrenSearch.and("path", FindIdsOfAllChildrenSearch.entity().getPath(), SearchCriteria.Op.LIKE);
        FindIdsOfAllChildrenSearch.done();

        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("name", AllFieldsSearch.entity().getName(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("state", AllFieldsSearch.entity().getState(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("owner", AllFieldsSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("path", AllFieldsSearch.entity().getPath(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("parent", AllFieldsSearch.entity().getParent(), SearchCriteria.Op.EQ);
        AllFieldsSearch.done();

    }

    private static String allocPath(DomainVO parentDomain, String name) {
        String parentPath = parentDomain.getPath();
        return parentPath + name + "/";
    }

    @Override
    public synchronized DomainVO create(DomainVO domain) {
        // make sure domain name is valid
        String domainName = domain.getName();
        if (domainName != null) {
            if (domainName.contains("/")) {
                throw new IllegalArgumentException("Domain name contains one or more invalid characters.  Please enter a name without '/' characters.");
            }
        } else {
            throw new IllegalArgumentException("Domain name is null.  Please specify a valid domain name.");
        }

        long parent = Domain.ROOT_DOMAIN;
        if (domain.getParent() != null && domain.getParent().longValue() >= Domain.ROOT_DOMAIN) {
            parent = domain.getParent().longValue();
        }

        DomainVO parentDomain = findById(parent);
        if (parentDomain == null) {
            s_logger.error("Unable to load parent domain: " + parent);
            return null;
        }

        TransactionLegacy txn = TransactionLegacy.currentTxn();
        try {
            txn.start();

            parentDomain = this.lockRow(parent, true);
            if (parentDomain == null) {
                s_logger.error("Unable to lock parent domain: " + parent);
                return null;
            }

            domain.setPath(allocPath(parentDomain, domain.getName()));
            domain.setLevel(parentDomain.getLevel() + 1);

            parentDomain.setNextChildSeq(parentDomain.getNextChildSeq() + 1); // FIXME:  remove sequence number?
            parentDomain.setChildCount(parentDomain.getChildCount() + 1);
            persist(domain);
            update(parentDomain.getId(), parentDomain);

            txn.commit();
            return domain;
        } catch (Exception e) {
            s_logger.error("Unable to create domain due to " + e.getMessage(), e);
            txn.rollback();
            return null;
        }
    }

    @Override
    @DB
    public boolean remove(Long id) {
        // check for any active users / domains assigned to the given domain id and don't remove the domain if there are any
        if (id != null && id.longValue() == Domain.ROOT_DOMAIN) {
            s_logger.error("Can not remove domain " + id + " as it is ROOT domain");
            return false;
        } else {
            if(id == null) {
                s_logger.error("Can not remove domain without id.");
                return false;
            }
        }

        DomainVO domain = findById(id);
        if (domain == null) {
            s_logger.info("Unable to remove domain as domain " + id + " no longer exists");
            return true;
        }

        if (domain.getParent() == null) {
            s_logger.error("Invalid domain " + id + ", orphan?");
            return false;
        }

        String sql = "SELECT * from account where domain_id = " + id + " and removed is null";
        String sql1 = "SELECT * from domain where parent = " + id + " and removed is null";

        boolean success = false;
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        try {
            txn.start();
            DomainVO parentDomain = super.lockRow(domain.getParent(), true);
            if (parentDomain == null) {
                s_logger.error("Unable to load parent domain: " + domain.getParent());
                return false;
            }

            PreparedStatement stmt = txn.prepareAutoCloseStatement(sql);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return false;
            }
            stmt = txn.prepareAutoCloseStatement(sql1);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return false;
            }

            parentDomain.setChildCount(parentDomain.getChildCount() - 1);
            update(parentDomain.getId(), parentDomain);
            success = super.remove(id);
            txn.commit();
        } catch (SQLException ex) {
            success = false;
            s_logger.error("error removing domain: " + id, ex);
            txn.rollback();
        }
        return success;
    }

    @Override
    public DomainVO findDomainByPath(String domainPath) {
        SearchCriteria<DomainVO> sc = createSearchCriteria();
        sc.addAnd("path", SearchCriteria.Op.EQ, domainPath);
        return findOneBy(sc);
    }

    @Override
    public DomainVO findImmediateChildForParent(Long parentId) {
        SearchCriteria<DomainVO> sc = ImmediateChildDomainSearch.create();
        sc.setParameters("parent", parentId);
        return (listBy(sc).size() > 0 ? listBy(sc).get(0) : null);//may need to revisit for multiple children case
    }

    @Override
    public List<DomainVO> findImmediateChildrenForParent(Long parentId) {
        SearchCriteria<DomainVO> sc = ImmediateChildDomainSearch.create();
        sc.setParameters("parent", parentId);
        return listBy(sc);
    }

    @Override
    public List<DomainVO> findAllChildren(String path, Long parentId) {
        SearchCriteria<DomainVO> sc = FindAllChildrenSearch.create();
        sc.setParameters("path", path + "%");
        sc.setParameters("id", parentId);
        return listBy(sc);
    }

    @Override
    public List<Long> getDomainChildrenIds(String path) {
        SearchCriteria<Long> sc = FindIdsOfAllChildrenSearch.create();
        sc.setParameters("path", path + "%");
        return customSearch(sc, null);
    }

    @Override
    public boolean isChildDomain(Long parentId, Long childId) {
        if ((parentId == null) || (childId == null)) {
            return false;
        }

        if (parentId.equals(childId)) {
            return true;
        }

        boolean result = false;
        SearchCriteria<DomainVO> sc = DomainPairSearch.create();
        sc.setParameters("id", parentId, childId);

        List<DomainVO> domainPair = listBy(sc);

        if ((domainPair != null) && (domainPair.size() == 2)) {
            DomainVO d1 = domainPair.get(0);
            DomainVO d2 = domainPair.get(1);

            if (d1.getId() == parentId) {
                result = d2.getPath().startsWith(d1.getPath());
            } else {
                result = d1.getPath().startsWith(d2.getPath());
            }
        }
        return result;
    }

    @Override
    public List<DomainVO> findInactiveDomains() {
        SearchCriteria<DomainVO> sc = AllFieldsSearch.create();
        sc.setParameters("state", Domain.State.Inactive);
        return listBy(sc);
    }

    @Override
    public Set<Long> getDomainParentIds(long domainId) {
        Set<Long> parentDomains = new HashSet<Long>();
        Domain domain = findById(domainId);

        if (domain != null) {
            parentDomains.add(domain.getId());

            while (domain.getParent() != null) {
                domain = findById(domain.getParent());
                parentDomains.add(domain.getId());
            }
        }

        return parentDomains;
    }

    @Override
    public boolean domainIdListContainsAccessibleDomain(String domainIdList, Account caller, Long domainId) {
        if (StringUtils.isEmpty(domainIdList)) {
            return false;
        }
        String[] domainIdsArray = domainIdList.split(",");
        for (String domainIdString : domainIdsArray) {
            try {
                Long dId = Long.valueOf(domainIdString.trim());
                if (!Account.Type.ADMIN.equals(caller.getType()) &&
                        isChildDomain(dId, caller.getDomainId())) {
                    return true;
                }
                if (domainId != null && isChildDomain(dId, domainId)) {
                    return true;
                }
            } catch (NumberFormatException nfe) {
                s_logger.debug(String.format("Unable to parse %s as domain ID from the list of domain IDs: %s", domainIdList.trim(), domainIdList), nfe);
            }
        }
        return false;
    }
}
