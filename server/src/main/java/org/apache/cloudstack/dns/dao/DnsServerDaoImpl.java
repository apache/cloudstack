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
import java.util.Set;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.dns.DnsProviderType;
import org.apache.cloudstack.dns.DnsServer;
import org.apache.cloudstack.dns.vo.DnsServerVO;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class DnsServerDaoImpl extends GenericDaoBase<DnsServerVO, Long> implements DnsServerDao {
    SearchBuilder<DnsServerVO> AllFieldsSearch;
    SearchBuilder<DnsServerVO> AccountUrlSearch;
    GenericSearchBuilder<DnsServerVO, Long> DnsServerIdsByAccountSearch;


    public DnsServerDaoImpl() {
        super();

        AccountUrlSearch = createSearchBuilder();
        AccountUrlSearch.and(ApiConstants.URL, AccountUrlSearch.entity().getUrl(), SearchCriteria.Op.EQ);
        AccountUrlSearch.and(ApiConstants.ACCOUNT_ID, AccountUrlSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountUrlSearch.done();

        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and(ApiConstants.ID, AllFieldsSearch.entity().getId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and(ApiConstants.NAME, AllFieldsSearch.entity().getName(), SearchCriteria.Op.LIKE);
        AllFieldsSearch.and(ApiConstants.PROVIDER_TYPE, AllFieldsSearch.entity().getProviderType(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and(ApiConstants.ACCOUNT_ID, AllFieldsSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.done();

        DnsServerIdsByAccountSearch = createSearchBuilder(Long.class);
        DnsServerIdsByAccountSearch.selectFields(DnsServerIdsByAccountSearch.entity().getId());
        DnsServerIdsByAccountSearch.and(ApiConstants.ACCOUNT_ID, DnsServerIdsByAccountSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        DnsServerIdsByAccountSearch.and(ApiConstants.STATE, DnsServerIdsByAccountSearch.entity().getState(), SearchCriteria.Op.EQ);
        DnsServerIdsByAccountSearch.done();

    }

    @Override
    public DnsServer findByUrlAndAccount(String url, long accountId) {
        SearchCriteria<DnsServerVO> sc = AccountUrlSearch.create();
        sc.setParameters(ApiConstants.URL, url);
        sc.setParameters(ApiConstants.ACCOUNT_ID, accountId);
        return findOneBy(sc);
    }

    @Override
    public List<Long> listDnsServerIdsByAccountId(Long accountId) {
        SearchCriteria<Long> sc = DnsServerIdsByAccountSearch.create();
        if (accountId != null) {
            sc.setParameters(ApiConstants.ACCOUNT_ID, accountId);
        }
        sc.setParameters(ApiConstants.STATE, DnsServer.State.Enabled);
        return customSearch(sc, null);
    }

    @Override
    public Pair<List<DnsServerVO>, Integer> searchDnsServers(Long id, String keyword, String provider, Long accountId, Filter filter) {
        SearchCriteria<DnsServerVO> sc = AllFieldsSearch.create();
        if (id != null) {
            sc.setParameters(ApiConstants.ID, id);
        }
        if (keyword != null) {
            sc.setParameters(ApiConstants.NAME, "%" + keyword + "%");
        }
        if (provider != null) {
            sc.setParameters(ApiConstants.PROVIDER_TYPE, provider);
        }
        if (accountId != null) {
            sc.setParameters(ApiConstants.ACCOUNT_ID, accountId);
        }
        return searchAndCount(sc, filter);
    }

    @Override
    public Pair<List<DnsServerVO>, Integer> searchDnsServer(Long dnsServerId, Long accountId, Set<Long> domainIds, DnsProviderType providerType,
                                                            String keyword, Filter filter) {

        SearchBuilder<DnsServerVO> sb = createSearchBuilder();
        sb.and(ApiConstants.ID, sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and(ApiConstants.NAME, sb.entity().getName(), SearchCriteria.Op.LIKE);

        sb.and().op(ApiConstants.ACCOUNT_ID, sb.entity().getAccountId(), SearchCriteria.Op.EQ);
        if (!CollectionUtils.isEmpty(domainIds)) {
            sb.or().op(ApiConstants.IS_PUBLIC, sb.entity().isPublicServer(), SearchCriteria.Op.EQ);
            sb.and(ApiConstants.DOMAIN_IDS, sb.entity().getDomainId(), SearchCriteria.Op.IN);
            sb.cp();
        }
        sb.cp();
        sb.and(ApiConstants.PROVIDER_TYPE, sb.entity().getProviderType(), SearchCriteria.Op.EQ);
        sb.and(ApiConstants.STATE, sb.entity().getState(), SearchCriteria.Op.EQ);
        sb.done();

        SearchCriteria<DnsServerVO> sc = sb.create();
        if (dnsServerId != null) {
            sc.setParameters(ApiConstants.ID, dnsServerId);
        }
        if (accountId != null) {
            sc.setParameters(ApiConstants.ACCOUNT_ID, accountId);
        }
        if (!CollectionUtils.isEmpty(domainIds)) {
            sc.setParameters(ApiConstants.IS_PUBLIC, true);
            sc.setParameters(ApiConstants.DOMAIN_IDS, domainIds.toArray());
        }
        if (providerType != null) {
            sc.setParameters(ApiConstants.PROVIDER_TYPE, providerType);
        }
        if (keyword != null) {
            sc.setParameters(ApiConstants.NAME, "%" + keyword + "%");
        }
        sc.setParameters(ApiConstants.STATE, DnsServer.State.Enabled);
        return searchAndCount(sc, filter);
    }
}
