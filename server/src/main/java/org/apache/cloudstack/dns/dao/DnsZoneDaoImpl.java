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

package org.apache.cloudstack.dns.dao;

import java.util.List;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.dns.DnsZone;
import org.apache.cloudstack.dns.vo.DnsZoneVO;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class DnsZoneDaoImpl extends GenericDaoBase<DnsZoneVO, Long> implements DnsZoneDao {
    SearchBuilder<DnsZoneVO> AccountSearch;
    SearchBuilder<DnsZoneVO> NameServerTypeSearch;

    public DnsZoneDaoImpl() {
        super();

        AccountSearch = createSearchBuilder();
        AccountSearch.and(ApiConstants.ACCOUNT_ID, AccountSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountSearch.and(ApiConstants.STATE, AccountSearch.entity().getState(), SearchCriteria.Op.EQ);
        AccountSearch.done();

        NameServerTypeSearch = createSearchBuilder();
        NameServerTypeSearch.and(ApiConstants.NAME, NameServerTypeSearch.entity().getName(), SearchCriteria.Op.EQ);
        NameServerTypeSearch.and(ApiConstants.DNS_SERVER_ID, NameServerTypeSearch.entity().getDnsServerId(), SearchCriteria.Op.EQ);
        NameServerTypeSearch.and(ApiConstants.TYPE, NameServerTypeSearch.entity().getType(), SearchCriteria.Op.EQ);
        NameServerTypeSearch.and(ApiConstants.STATE, NameServerTypeSearch.entity().getState(), SearchCriteria.Op.EQ);
        NameServerTypeSearch.done();
    }

    @Override
    public List<DnsZoneVO> listByAccount(long accountId) {
        SearchCriteria<DnsZoneVO> sc = AccountSearch.create();
        sc.setParameters(ApiConstants.ACCOUNT_ID, accountId);
        sc.setParameters(ApiConstants.STATE, DnsZone.State.Active);
        return listBy(sc);
    }

    @Override
    public DnsZoneVO findByNameServerAndType(String name, long dnsServerId, DnsZone.ZoneType type) {
        SearchCriteria<DnsZoneVO> sc = NameServerTypeSearch.create();
        sc.setParameters(ApiConstants.NAME, name);
        sc.setParameters(ApiConstants.DNS_SERVER_ID, dnsServerId);
        sc.setParameters(ApiConstants.TYPE, type);
        sc.setParameters(ApiConstants.STATE, DnsZone.State.Active);
        return findOneBy(sc);
    }

    @Override
    public Pair<List<DnsZoneVO>, Integer> searchZones(Long id, Long accountId, List<Long> ownDnsServerIds, Long targetDnsServerId,
                                                      String keyword, Filter filter) {

        SearchBuilder<DnsZoneVO> sb = createSearchBuilder();
        sb.and(ApiConstants.STATE, sb.entity().getState(), SearchCriteria.Op.EQ);
        sb.and(ApiConstants.ID, sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and(ApiConstants.NAME, sb.entity().getName(), SearchCriteria.Op.LIKE);
        sb.and(ApiConstants.TARGET_ID, sb.entity().getDnsServerId(), SearchCriteria.Op.EQ);
        if (!CollectionUtils.isEmpty(ownDnsServerIds)) {
            sb.and().op(ApiConstants.DNS_SERVER_ID, sb.entity().getDnsServerId(), SearchCriteria.Op.IN);
            sb.or(ApiConstants.ACCOUNT_ID, sb.entity().getAccountId(), SearchCriteria.Op.EQ);
            sb.cp();
        } else {
            sb.and(ApiConstants.ACCOUNT_ID, sb.entity().getAccountId(), SearchCriteria.Op.EQ);
        }
        sb.done();

        SearchCriteria<DnsZoneVO> sc = sb.create();
        if (id != null) {
            sc.setParameters(ApiConstants.ID, id);
        }
        if (!CollectionUtils.isEmpty(ownDnsServerIds)) {
            sc.setParameters(ApiConstants.DNS_SERVER_ID, ownDnsServerIds.toArray());
        }
        if (keyword != null) {
            sc.setParameters(ApiConstants.NAME, "%" + keyword + "%");
        }
        if (accountId != null) {
            sc.setParameters(ApiConstants.ACCOUNT_ID, accountId);
        }
        if (targetDnsServerId != null) {
            sc.setParameters(ApiConstants.TARGET_ID, targetDnsServerId);
        }
        sc.setParameters(ApiConstants.STATE, DnsZone.State.Active);
        return searchAndCount(sc, filter);
    }
}
