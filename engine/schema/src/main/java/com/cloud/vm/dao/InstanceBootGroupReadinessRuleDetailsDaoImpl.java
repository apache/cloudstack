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

package com.cloud.vm.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.resourcedetail.ResourceDetailsDaoBase;
import org.apache.cloudstack.vm.bootgroup.InstanceBootGroupReadinessRuleDetailsVO;
import org.springframework.stereotype.Component;

import com.cloud.utils.crypt.DBEncryptionUtil;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class InstanceBootGroupReadinessRuleDetailsDaoImpl extends ResourceDetailsDaoBase<InstanceBootGroupReadinessRuleDetailsVO> implements InstanceBootGroupReadinessRuleDetailsDao {

    private static final String ENCRYPTED_KEY = "script";

    private final SearchBuilder<InstanceBootGroupReadinessRuleDetailsVO> ruleSearch;

    public InstanceBootGroupReadinessRuleDetailsDaoImpl() {
        super();
        ruleSearch = createSearchBuilder();
        ruleSearch.and("ruleId", ruleSearch.entity().getResourceId(), SearchCriteria.Op.EQ);
        ruleSearch.done();
    }

    @Override
    public void addDetail(long resourceId, String key, String value, boolean display) {
        String storedValue = ENCRYPTED_KEY.equals(key) ? DBEncryptionUtil.encrypt(value) : value;
        super.addDetail(new InstanceBootGroupReadinessRuleDetailsVO(resourceId, key, storedValue));
    }

    @Override
    public Map<String, String> getDetails(long ruleId) {
        SearchCriteria<InstanceBootGroupReadinessRuleDetailsVO> sc = ruleSearch.create();
        sc.setParameters("ruleId", ruleId);

        List<InstanceBootGroupReadinessRuleDetailsVO> details = listBy(sc);
        Map<String, String> detailsMap = new HashMap<>();
        for (InstanceBootGroupReadinessRuleDetailsVO detail : details) {
            String name = detail.getName();
            String value = detail.getValue();
            if (ENCRYPTED_KEY.equals(name)) {
                value = DBEncryptionUtil.decrypt(value);
            }
            detailsMap.put(name, value);
        }
        return detailsMap;
    }
}
