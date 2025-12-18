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

import com.cloud.network.vo.PublicIpQuarantineVO;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

@Component
public class PublicIpQuarantineDaoImpl extends GenericDaoBase<PublicIpQuarantineVO, Long> implements PublicIpQuarantineDao {
    private SearchBuilder<PublicIpQuarantineVO> publicIpAddressByIdSearch;

    private SearchBuilder<IPAddressVO> ipAddressSearchBuilder;

    @Inject
    IPAddressDao ipAddressDao;

    @PostConstruct
    public void init() {
        publicIpAddressByIdSearch = createSearchBuilder();
        publicIpAddressByIdSearch.and("publicIpAddressId", publicIpAddressByIdSearch.entity().getPublicIpAddressId(), SearchCriteria.Op.EQ);

        ipAddressSearchBuilder = ipAddressDao.createSearchBuilder();
        ipAddressSearchBuilder.and("ipAddress", ipAddressSearchBuilder.entity().getAddress(), SearchCriteria.Op.EQ);
        ipAddressSearchBuilder.and("removed", ipAddressSearchBuilder.entity().getRemoved(), SearchCriteria.Op.NULL);
        publicIpAddressByIdSearch.join("quarantineJoin", ipAddressSearchBuilder, ipAddressSearchBuilder.entity().getId(),
                publicIpAddressByIdSearch.entity().getPublicIpAddressId(), JoinBuilder.JoinType.INNER);

        ipAddressSearchBuilder.done();
        publicIpAddressByIdSearch.done();
    }

    @Override
    public PublicIpQuarantineVO findByPublicIpAddressId(long publicIpAddressId) {
        SearchCriteria<PublicIpQuarantineVO> sc = publicIpAddressByIdSearch.create();
        sc.setParameters("publicIpAddressId", publicIpAddressId);
        final Filter filter = new Filter(PublicIpQuarantineVO.class, "created", false);

        return findOneBy(sc, filter);
    }

    @Override
    public PublicIpQuarantineVO findByIpAddress(String publicIpAddress) {
        SearchCriteria<PublicIpQuarantineVO> sc = publicIpAddressByIdSearch.create();
        sc.setJoinParameters("quarantineJoin", "ipAddress", publicIpAddress);
        final Filter filter = new Filter(PublicIpQuarantineVO.class, "created", false);

        return findOneBy(sc, filter);
    }
}
