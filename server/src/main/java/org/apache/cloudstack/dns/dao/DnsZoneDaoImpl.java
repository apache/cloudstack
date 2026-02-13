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
import org.springframework.stereotype.Component;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class DnsZoneDaoImpl extends GenericDaoBase<DnsZoneVO, Long> implements DnsZoneDao {
    static final String DNS_SERVER_ID = "dnsServerId";
    SearchBuilder<DnsZoneVO> ServerSearch;
    SearchBuilder<DnsZoneVO> AccountSearch;
    SearchBuilder<DnsZoneVO> NameServerTypeSearch;

    public DnsZoneDaoImpl() {
        super();
        ServerSearch = createSearchBuilder();
        ServerSearch.and(DNS_SERVER_ID, ServerSearch.entity().getDnsServerId(), SearchCriteria.Op.EQ);
        ServerSearch.done();

        AccountSearch = createSearchBuilder();
        AccountSearch.and(ApiConstants.ACCOUNT_ID, AccountSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountSearch.done();

        NameServerTypeSearch = createSearchBuilder();
        NameServerTypeSearch.and(ApiConstants.NAME, NameServerTypeSearch.entity().getName(), SearchCriteria.Op.EQ);
        NameServerTypeSearch.and(DNS_SERVER_ID, NameServerTypeSearch.entity().getDnsServerId(), SearchCriteria.Op.EQ);
        NameServerTypeSearch.and(ApiConstants.TYPE, NameServerTypeSearch.entity().getType(), SearchCriteria.Op.EQ);
        NameServerTypeSearch.done();
    }

    @Override
    public List<DnsZoneVO> listByServerId(long serverId) {
        SearchCriteria<DnsZoneVO> sc = ServerSearch.create();
        sc.setParameters(DNS_SERVER_ID, serverId);
        return listBy(sc);
    }

    @Override
    public List<DnsZoneVO> listByAccount(long accountId) {
        SearchCriteria<DnsZoneVO> sc = AccountSearch.create();
        sc.setParameters(ApiConstants.ACCOUNT_ID, accountId);
        return listBy(sc);
    }

    @Override
    public DnsZoneVO findByNameServerAndType(String name, long dnsServerId, DnsZone.ZoneType type) {
        SearchCriteria<DnsZoneVO> sc = NameServerTypeSearch.create();
        sc.setParameters(ApiConstants.NAME, name);
        sc.setParameters(DNS_SERVER_ID, dnsServerId);
        sc.setParameters(ApiConstants.TYPE, type);
        return findOneBy(sc);
    }
}
