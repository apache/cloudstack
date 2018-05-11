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

package org.apache.cloudstack.br.dao;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.br.BRPolicyVO;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

@Component
public class BRPoliciesDaoImpl extends GenericDaoBase<BRPolicyVO, Long> implements BRPoliciesDao {

    protected SearchBuilder<BRPolicyVO> backupPoliciesSearch;

    public BRPoliciesDaoImpl() {
    }

    @PostConstruct
    protected void init() {
        backupPoliciesSearch = createSearchBuilder();
        backupPoliciesSearch.and("provider", backupPoliciesSearch.entity().getProviderId(), SearchCriteria.Op.EQ);
        backupPoliciesSearch.done();
    }

    @Override
    public List<BRPolicyVO> listByProvider(long providerId) {
        SearchCriteria<BRPolicyVO> sc = backupPoliciesSearch.create();
        sc.setParameters("provider", providerId);
        return listBy(sc);
    }

    @Override
    public void removeByProvider(long providerId) {
        SearchCriteria<BRPolicyVO> sc = backupPoliciesSearch.create();
        sc.setParameters("provider", providerId);
        expunge(sc);
    }


}
