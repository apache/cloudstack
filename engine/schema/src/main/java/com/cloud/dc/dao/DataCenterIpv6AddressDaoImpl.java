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

import java.util.Date;
import java.util.List;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.dc.DataCenterIpv6AddressVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
@DB
public class DataCenterIpv6AddressDaoImpl extends GenericDaoBase<DataCenterIpv6AddressVO, Long> implements DataCenterIpv6AddressDao, Configurable {
    private static final Logger s_logger = Logger.getLogger(DataCenterIpv6AddressDaoImpl.class);

    private final SearchBuilder<DataCenterIpv6AddressVO> AllFieldsSearch;

    private static final ConfigKey<String> VirtualRouterPrivateIpv6Cidr = new ConfigKey<String>("Advanced",
            String.class, "virtual.router.private.ipv6.cidr", "","The private Ipv6 cidr configured on virtual routers.", true, ConfigKey.Scope.Cluster);

    public DataCenterIpv6AddressDaoImpl() {
        super();

        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("id", AllFieldsSearch.entity().getId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("zoneId", AllFieldsSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("physicalNetworkId", AllFieldsSearch.entity().getPhysicalNetworkId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("networkId", AllFieldsSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("domainId", AllFieldsSearch.entity().getDomainId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("accountId", AllFieldsSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("taken", AllFieldsSearch.entity().getTakenAt(), SearchCriteria.Op.EQ);
        AllFieldsSearch.done();
    }

    @Override
    public String getConfigComponentName() {
        return DataCenterIpv6AddressDao.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] { VirtualRouterPrivateIpv6Cidr };
    }

    @Override
    public void addIpRange(long dcId, long physicalNetworkId, String ip6Gateway, String ip6Cidr, String routerIpv6) {
        DataCenterIpv6AddressVO range = new DataCenterIpv6AddressVO(dcId, physicalNetworkId, ip6Gateway, ip6Cidr, routerIpv6);
        persist(range);
    }

    @Override
    public void removeIpv6Range(long id) {
        remove(id);
    }

    @Override
    public void dedicateIpv6Range(long id, Long domainId, Long accountId) {
        //TODO
    }

    @Override
    public void releaseIpv6Range(long id) {
        //TODO
    }

    @Override
    public boolean mark(long id, Long networkId, Long domainId, Long accountId) {
        DataCenterIpv6AddressVO range = createForUpdate(id);
        range.setNetworkId(networkId);
        range.setDomainId(domainId);
        range.setAccountId(accountId);
        range.setTakenAt(new Date());
        return update(id, range);
    }

    @Override
    public boolean unmark(long id) {
        DataCenterIpv6AddressVO range = createForUpdate(id);
        range.setNetworkId(null);
        range.setDomainId(null);
        range.setAccountId(null);
        range.setTakenAt(GenericDaoBase.DATE_TO_NULL);
        return update(id, range);
    }

    @Override
    public List<DataCenterIpv6AddressVO> listByZoneDomainAccount(long zoneId, Long networkId, Long domainId, Long accountId) {
        SearchCriteria<DataCenterIpv6AddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("zoneId", zoneId);
        if (networkId != null) {
            sc.setParameters("networkId", networkId);
        }
        if (domainId != null) {
            sc.setParameters("domainId", domainId);
        }
        if (accountId != null) {
            sc.setParameters("accountId", accountId);
        }
        return listBy(sc);
    }
}
