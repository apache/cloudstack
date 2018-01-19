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

import javax.inject.Inject;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

public class LoadBalancerCertMapDaoImpl extends GenericDaoBase<LoadBalancerCertMapVO, Long> implements LoadBalancerCertMapDao {

    private final SearchBuilder<LoadBalancerCertMapVO> listByCertId;
    private final SearchBuilder<LoadBalancerCertMapVO> findByLbRuleId;

    @Inject
    SslCertDao _sslCertDao;

    public LoadBalancerCertMapDaoImpl() {

        listByCertId = createSearchBuilder();
        listByCertId.and("certificateId", listByCertId.entity().getCertId(), SearchCriteria.Op.EQ);
        listByCertId.done();

        findByLbRuleId = createSearchBuilder();
        findByLbRuleId.and("loadBalancerId", findByLbRuleId.entity().getLbId(), SearchCriteria.Op.EQ);
        findByLbRuleId.done();

    }

    @Override
    public List<LoadBalancerCertMapVO> listByCertId(Long certId) {
        SearchCriteria<LoadBalancerCertMapVO> sc = listByCertId.create();
        sc.setParameters("certificateId", certId);
        return listBy(sc);
    }

    @Override
    public LoadBalancerCertMapVO findByLbRuleId(Long lbId) {
        SearchCriteria<LoadBalancerCertMapVO> sc = findByLbRuleId.create();
        sc.setParameters("loadBalancerId", lbId);
        return findOneBy(sc);
    }

    @Override
    public List<LoadBalancerCertMapVO> listByAccountId(Long accountId) {

        SearchBuilder<LoadBalancerCertMapVO> listByAccountId;
        SearchBuilder<SslCertVO> certsForAccount;

        listByAccountId = createSearchBuilder();
        certsForAccount = _sslCertDao.createSearchBuilder();
        certsForAccount.and("accountId", certsForAccount.entity().getAccountId(), SearchCriteria.Op.EQ);
        listByAccountId.join("certsForAccount", certsForAccount, certsForAccount.entity().getId(), listByAccountId.entity().getLbId(), JoinBuilder.JoinType.INNER);
        certsForAccount.done();
        listByAccountId.done();

        SearchCriteria<LoadBalancerCertMapVO> sc = listByAccountId.create();
        sc.setParameters("accountId", accountId);
        return listBy(sc);
    }
}
