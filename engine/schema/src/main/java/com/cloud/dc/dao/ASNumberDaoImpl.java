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
package com.cloud.dc.dao;

import com.cloud.dc.ASNumberVO;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

import java.util.Arrays;
import java.util.List;

public class ASNumberDaoImpl extends GenericDaoBase<ASNumberVO, Long> implements ASNumberDao {

    private final SearchBuilder<ASNumberVO> asNumberSearch;

    public ASNumberDaoImpl() {
        asNumberSearch = createSearchBuilder();
        asNumberSearch.and("zoneId", asNumberSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        asNumberSearch.and("rangeId", asNumberSearch.entity().getAsNumberRangeId(), SearchCriteria.Op.EQ);
        asNumberSearch.and("isAllocated", asNumberSearch.entity().isAllocated(), SearchCriteria.Op.EQ);
        asNumberSearch.and("asNumber", asNumberSearch.entity().getAsNumber(), SearchCriteria.Op.EQ);
        asNumberSearch.and("networkId", asNumberSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
        asNumberSearch.and("vpcId", asNumberSearch.entity().getVpcId(), SearchCriteria.Op.EQ);
        asNumberSearch.and("accountId", asNumberSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        asNumberSearch.and("domainId", asNumberSearch.entity().getDomainId(), SearchCriteria.Op.EQ);
        asNumberSearch.done();
    }

    @Override
    public Pair<List<ASNumberVO>, Integer> searchAndCountByZoneOrRangeOrAllocated(Long zoneId, Long asnRangeId,
                                                                                  Integer asNumber, Long networkId, Long vpcId,
                                                                                  Boolean allocated,
                                                                                  Long accountId, Long domainId,
                                                                                  String keyword, Account caller,
                                                                                  Long startIndex, Long pageSizeVal) {
        SearchCriteria<ASNumberVO> sc = asNumberSearch.create();
        if (zoneId != null) {
            sc.setParameters("zoneId", zoneId);
        }
        if (asnRangeId != null) {
            sc.setParameters("rangeId", asnRangeId);
        }
        if (networkId != null) {
            sc.setParameters("networkId", networkId);
        }
        if (vpcId != null) {
            sc.setParameters("vpcId", vpcId);
        }
        if (allocated != null) {
            sc.setParameters("isAllocated", allocated);
        }
        if (asNumber != null) {
            sc.setParameters("asNumber", asNumber);
        }
        if (accountId != null) {
            sc.setParameters("accountId", accountId);
        }
        if (domainId != null) {
            sc.setParameters("domainId", domainId);
        }
        if (keyword != null) {
            sc.addAnd("asNumber", SearchCriteria.Op.LIKE, "%" + keyword + "%");
        }
        if (Arrays.asList(Account.Type.DOMAIN_ADMIN, Account.Type.RESOURCE_DOMAIN_ADMIN).contains(caller.getType())) {
            SearchCriteria<ASNumberVO> scc = asNumberSearch.create();
            scc.addOr("domainId", SearchCriteria.Op.NULL);
            scc.addOr("domainId", SearchCriteria.Op.EQ, caller.getDomainId());
            sc.addAnd("domainId", SearchCriteria.Op.SC, scc);
        } else if (Arrays.asList(Account.Type.NORMAL, Account.Type.PROJECT).contains(caller.getType())) {
            SearchCriteria<ASNumberVO> scc = asNumberSearch.create();
            scc.addOr("domainId", SearchCriteria.Op.NULL);
            scc.addOr("accountId", SearchCriteria.Op.EQ, caller.getAccountId());
            sc.addAnd("domainId", SearchCriteria.Op.SC, scc);
        }
        Filter searchFilter = new Filter(ASNumberVO.class, "id", true, startIndex, pageSizeVal);
        return searchAndCount(sc, searchFilter);
    }

    @Override
    public ASNumberVO findByAsNumber(Long asNumber) {
        SearchCriteria<ASNumberVO> sc = asNumberSearch.create();
        sc.setParameters("asNumber", asNumber);
        return findOneBy(sc);
    }

    @Override
    public ASNumberVO findOneByAllocationStateAndZone(long zoneId, boolean allocated) {
        SearchCriteria<ASNumberVO> sc = asNumberSearch.create();
        sc.setParameters("zoneId", zoneId);
        sc.setParameters("isAllocated", allocated);
        return findOneBy(sc);
    }

    @Override
    public List<ASNumberVO> listAllocatedByASRange(Long asRangeId) {
        SearchCriteria<ASNumberVO> sc = asNumberSearch.create();
        sc.setParameters("rangeId", asRangeId);
        sc.setParameters("isAllocated", true);
        return listBy(sc);
    }

    public ASNumberVO findByZoneAndNetworkId(long zoneId, long networkId) {
        SearchCriteria<ASNumberVO> sc = asNumberSearch.create();
        sc.setParameters("zoneId", zoneId);
        sc.setParameters("networkId", networkId);
        return findOneBy(sc);
    }

    @Override
    public ASNumberVO findByZoneAndVpcId(long zoneId, long vpcId) {
        SearchCriteria<ASNumberVO> sc = asNumberSearch.create();
        sc.setParameters("zoneId", zoneId);
        sc.setParameters("vpcId", vpcId);
        return findOneBy(sc);
    }

    @Override
    public int removeASRangeNumbers(long rangeId) {
        SearchCriteria<ASNumberVO> sc = asNumberSearch.create();
        sc.setParameters("rangeId", rangeId);
        return remove(sc);
    }
}
