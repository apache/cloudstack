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

import javax.ejb.Local;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
@Local(value={Site2SiteCustomerGatewayDao.class})
public class Site2SiteCustomerGatewayDaoImpl extends GenericDaoBase<Site2SiteCustomerGatewayVO, Long> implements Site2SiteCustomerGatewayDao {
    private static final Logger s_logger = Logger.getLogger(Site2SiteCustomerGatewayDaoImpl.class);
    
    private final SearchBuilder<Site2SiteCustomerGatewayVO> AllFieldsSearch;

    protected Site2SiteCustomerGatewayDaoImpl() {
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("gatewayIp", AllFieldsSearch.entity().getGatewayIp(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("name", AllFieldsSearch.entity().getName(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("accountId", AllFieldsSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.done();
    }
    
    @Override
    public Site2SiteCustomerGatewayVO findByGatewayIpAndAccountId(String ip, long accountId) {
        SearchCriteria<Site2SiteCustomerGatewayVO> sc = AllFieldsSearch.create();
        sc.setParameters("gatewayIp", ip);
        sc.setParameters("accountId", accountId);
        return findOneBy(sc);
    }

    @Override
    public Site2SiteCustomerGatewayVO findByNameAndAccountId(String name, long accountId) {
        SearchCriteria<Site2SiteCustomerGatewayVO> sc = AllFieldsSearch.create();
        sc.setParameters("name", name);
        sc.setParameters("accountId", accountId);
        return findOneBy(sc);
    }

    @Override
    public List<Site2SiteCustomerGatewayVO> listByAccountId(long accountId) {
        SearchCriteria<Site2SiteCustomerGatewayVO> sc = AllFieldsSearch.create();
        sc.setParameters("accountId", accountId);
        return listBy(sc, null);
    }
}
