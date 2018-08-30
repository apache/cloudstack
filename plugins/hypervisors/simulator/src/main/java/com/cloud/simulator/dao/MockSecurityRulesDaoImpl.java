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
package com.cloud.simulator.dao;

import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.springframework.stereotype.Component;

import com.cloud.simulator.MockSecurityRulesVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class MockSecurityRulesDaoImpl extends GenericDaoBase<MockSecurityRulesVO, Long> implements MockSecurityRulesDao {
    protected SearchBuilder<MockSecurityRulesVO> vmIdSearch;
    protected SearchBuilder<MockSecurityRulesVO> hostSearch;

    @Override
    public MockSecurityRulesVO findByVmId(Long vmId) {
        SearchCriteria<MockSecurityRulesVO> sc = vmIdSearch.create();
        sc.setParameters("vmId", vmId);
        return findOneBy(sc);
    }

    @Override
    public List<MockSecurityRulesVO> findByHost(String hostGuid) {
        SearchCriteria<MockSecurityRulesVO> sc = hostSearch.create();
        sc.setParameters("host", hostGuid);
        return listBy(sc);
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        vmIdSearch = createSearchBuilder();
        vmIdSearch.and("vmId", vmIdSearch.entity().getVmId(), SearchCriteria.Op.EQ);
        vmIdSearch.done();

        hostSearch = createSearchBuilder();
        hostSearch.and("host", hostSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        hostSearch.done();

        return true;
    }

}
