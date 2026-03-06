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

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.dns.vo.DnsZoneNetworkMapVO;
import org.springframework.stereotype.Component;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class DnsZoneNetworkMapDaoImpl extends GenericDaoBase<DnsZoneNetworkMapVO, Long> implements DnsZoneNetworkMapDao {
    private final SearchBuilder<DnsZoneNetworkMapVO> ZoneNetworkSearch;
    private final SearchBuilder<DnsZoneNetworkMapVO> NetworkSearch;

    public DnsZoneNetworkMapDaoImpl() {
        super();
        ZoneNetworkSearch = createSearchBuilder();
        ZoneNetworkSearch.and(ApiConstants.DNS_ZONE_ID, ZoneNetworkSearch.entity().getDnsZoneId(), SearchCriteria.Op.EQ);
        ZoneNetworkSearch.and(ApiConstants.NETWORK_ID, ZoneNetworkSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
        ZoneNetworkSearch.done();

        NetworkSearch = createSearchBuilder();
        NetworkSearch.and(ApiConstants.NETWORK_ID, NetworkSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
        NetworkSearch.done();
    }

    @Override
    public DnsZoneNetworkMapVO findByZoneAndNetwork(long dnsZoneId, long networkId) {
        SearchCriteria<DnsZoneNetworkMapVO> sc = ZoneNetworkSearch.create();
        sc.setParameters(ApiConstants.DNS_ZONE_ID, dnsZoneId);
        sc.setParameters(ApiConstants.NETWORK_ID, networkId);

        return findOneBy(sc);
    }

    @Override
    public DnsZoneNetworkMapVO findByNetworkId(long networkId) {
        SearchCriteria<DnsZoneNetworkMapVO> sc = NetworkSearch.create();
        sc.setParameters(ApiConstants.NETWORK_ID, networkId);
        return findOneBy(sc);
    }
}
