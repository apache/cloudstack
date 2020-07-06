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

import com.cloud.dc.VsphereStoragePolicyVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VsphereStoragePolicyDaoImpl extends GenericDaoBase<VsphereStoragePolicyVO, Long> implements VsphereStoragePolicyDao {

    protected static final Logger LOGGER = Logger.getLogger(VsphereStoragePolicyDaoImpl.class);

    private final SearchBuilder<VsphereStoragePolicyVO> zoneSearch;
    private final SearchBuilder<VsphereStoragePolicyVO> policySearch;

    public VsphereStoragePolicyDaoImpl() {
        super();

        zoneSearch = createSearchBuilder();
        zoneSearch.and("zoneId", zoneSearch.entity().getZoneId(), SearchCriteria.Op.EQ);
        zoneSearch.done();

        policySearch = createSearchBuilder();
        policySearch.and("zoneId", policySearch.entity().getZoneId(), SearchCriteria.Op.EQ);
        policySearch.and("policyId", policySearch.entity().getPolicyId(), SearchCriteria.Op.EQ);
        policySearch.done();
    }

    @Override
    public VsphereStoragePolicyVO findByPolicyId(Long zoneId, String policyId) {
        SearchCriteria<VsphereStoragePolicyVO> sc = policySearch.create();
        sc.setParameters("zoneId", zoneId);
        sc.setParameters("policyId", policyId);
        return findOneBy(sc);
    }

    @Override
    public List<VsphereStoragePolicyVO> findByZoneId(Long zoneId) {
        SearchCriteria<VsphereStoragePolicyVO> sc = zoneSearch.create();
        sc.setParameters("zoneId", zoneId);

        return listBy(sc);
    }
}
