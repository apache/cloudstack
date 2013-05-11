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
package com.cloud.network.dao;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.JoinBuilder.JoinType;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
@Local(value={Site2SiteVpnConnectionDao.class})
public class Site2SiteVpnConnectionDaoImpl extends GenericDaoBase<Site2SiteVpnConnectionVO, Long> implements Site2SiteVpnConnectionDao {
    private static final Logger s_logger = Logger.getLogger(Site2SiteVpnConnectionDaoImpl.class);

    @Inject protected IPAddressDao _addrDao;
    @Inject protected Site2SiteVpnGatewayDao _vpnGatewayDao;

    private SearchBuilder<Site2SiteVpnConnectionVO> AllFieldsSearch;
    private SearchBuilder<Site2SiteVpnConnectionVO> VpcSearch;
    private SearchBuilder<Site2SiteVpnGatewayVO> VpnGatewaySearch;

    public Site2SiteVpnConnectionDaoImpl() {
    }

    @PostConstruct
    protected void init() {
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("customerGatewayId", AllFieldsSearch.entity().getCustomerGatewayId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("vpnGatewayId", AllFieldsSearch.entity().getVpnGatewayId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.done();

        VpcSearch = createSearchBuilder();
        VpnGatewaySearch = _vpnGatewayDao.createSearchBuilder();
        VpnGatewaySearch.and("vpcId", VpnGatewaySearch.entity().getVpcId(), SearchCriteria.Op.EQ);
        VpcSearch.join("vpnGatewaySearch", VpnGatewaySearch, VpnGatewaySearch.entity().getId(), VpcSearch.entity().getVpnGatewayId(), JoinType.INNER);
        VpcSearch.done();
    }

    @Override
    public List<Site2SiteVpnConnectionVO> listByCustomerGatewayId(long id) {
        SearchCriteria<Site2SiteVpnConnectionVO> sc = AllFieldsSearch.create();
        sc.setParameters("customerGatewayId", id);
        return listBy(sc);
    }

    @Override
    public List<Site2SiteVpnConnectionVO> listByVpnGatewayId(long id) {
        SearchCriteria<Site2SiteVpnConnectionVO> sc = AllFieldsSearch.create();
        sc.setParameters("vpnGatewayId", id);
        return listBy(sc);
    }

    @Override
    public List<Site2SiteVpnConnectionVO> listByVpcId(long vpcId) {
        SearchCriteria<Site2SiteVpnConnectionVO> sc = VpcSearch.create();
        sc.setJoinParameters("vpnGatewaySearch", "vpcId", vpcId);
        return listBy(sc);
    }

    @Override
    public Site2SiteVpnConnectionVO findByVpnGatewayIdAndCustomerGatewayId(long vpnId, long customerId) {
        SearchCriteria<Site2SiteVpnConnectionVO> sc = AllFieldsSearch.create();
        sc.setParameters("vpnGatewayId", vpnId);
        sc.setParameters("customerGatewayId", customerId);
        return findOneBy(sc);
    }

    @Override
    public Site2SiteVpnConnectionVO findByCustomerGatewayId(long customerId) {
        SearchCriteria<Site2SiteVpnConnectionVO> sc = AllFieldsSearch.create();
        sc.setParameters("customerGatewayId", customerId);
        return findOneBy(sc);
    }
}
