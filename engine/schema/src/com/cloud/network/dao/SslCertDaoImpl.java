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

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value = {SslCertDao.class})
public class SslCertDaoImpl extends GenericDaoBase<SslCertVO, Long> implements SslCertDao {

    private final SearchBuilder<SslCertVO> listByAccountId;

    public SslCertDaoImpl() {
        listByAccountId = createSearchBuilder();
        listByAccountId.and("accountId", listByAccountId.entity().getAccountId(), SearchCriteria.Op.EQ);
        listByAccountId.done();
   }

    @Override
    public List<SslCertVO> listByAccountId(Long accountId) {
        SearchCriteria<SslCertVO> sc = listByAccountId.create();
        sc.setParameters("accountId", accountId);
        return listBy(sc);
    }

}
