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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.dns.DnsProviderType;
import org.apache.cloudstack.dns.DnsServer;
import org.apache.cloudstack.dns.vo.DnsServerDetailVO;
import org.apache.cloudstack.dns.vo.DnsServerVO;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;

@Component
public class DnsServerDaoImpl extends GenericDaoBase<DnsServerVO, Long> implements DnsServerDao {
    @Inject
    DnsServerDetailsDao dnsServerDetailsDao;

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
    public DnsServerVO findById(Long dnsServerId) {
        DnsServerVO dnsServer = super.findById(dnsServerId);
        loadDetails(dnsServer);
        return dnsServer;
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
    public Pair<List<DnsServerVO>, Integer> searchDnsServer(Long dnsServerId, Long accountId, Set<Long> domainIds, DnsProviderType providerType,
                                                            String keyword, Filter filter) {

        SearchBuilder<DnsServerVO> sb = createSearchBuilder();
        sb.and(ApiConstants.ID, sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and(ApiConstants.NAME, sb.entity().getName(), SearchCriteria.Op.LIKE);

        sb.and().op(ApiConstants.ACCOUNT_ID, sb.entity().getAccountId(), SearchCriteria.Op.EQ);
        if (!CollectionUtils.isEmpty(domainIds)) {
            sb.or().op(ApiConstants.IS_PUBLIC, sb.entity().getPublicServer(), SearchCriteria.Op.EQ);
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

    @Override
    public DnsServerVO persist(DnsServerVO dnsServer) {
        return Transaction.execute((TransactionCallback<DnsServerVO>) status -> {
            DnsServerVO dnsServerDb = super.persist(dnsServer);
            saveDetails(dnsServer);
            loadDetails(dnsServerDb);
            return dnsServerDb;
        });
    }

    @Override
    public boolean update(Long id, DnsServerVO dnsServer) {
        return Transaction.execute((TransactionCallback<Boolean>) status -> {
            boolean result = super.update(id, dnsServer);
            if (result) {
                saveDetails(dnsServer);
            }
            return result;
        });
    }

    @Override
    public boolean remove(Long dnsServerId) {
        return Transaction.execute((TransactionCallback<Boolean>) status -> {
            boolean result = super.remove(dnsServerId);
            if (result) {
                dnsServerDetailsDao.removeDetails(dnsServerId);
            }
            return result;
        });
    }

    @Override
    public void loadDetails(DnsServer dnsServer) {
        Map<String, String> details = dnsServerDetailsDao.listDetailsKeyPairs(dnsServer.getId());
        dnsServer.setDetails(details);
    }

    @Override
    public void saveDetails(DnsServer dnsServer) {
        Map<String, String> detailsStr = dnsServer.getDetails();
        if (detailsStr == null) {
            return;
        }
        List<DnsServerDetailVO> details = new ArrayList<>();
        for (String key : detailsStr.keySet()) {
            DnsServerDetailVO detail = new DnsServerDetailVO(dnsServer.getId(), key, detailsStr.get(key), true);
            details.add(detail);
        }
        dnsServerDetailsDao.saveDetails(details);
    }
}
