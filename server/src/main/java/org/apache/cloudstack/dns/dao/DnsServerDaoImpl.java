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
import org.apache.cloudstack.dns.DnsServer;
import org.apache.cloudstack.dns.vo.DnsServerVO;
import org.springframework.stereotype.Component;

import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class DnsServerDaoImpl extends GenericDaoBase<DnsServerVO, Long> implements DnsServerDao {
    SearchBuilder<DnsServerVO> AllFieldsSearch;
    SearchBuilder<DnsServerVO> AccountUrlSearch;


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

    }

    @Override
    public DnsServer findByUrlAndAccount(String url, long accountId) {
        SearchCriteria<DnsServerVO> sc = AccountUrlSearch.create();
        sc.setParameters(ApiConstants.URL, url);
        sc.setParameters(ApiConstants.ACCOUNT_ID, accountId);
        return findOneBy(sc);
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
}
