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
package com.cloud.network.ovs.dao;


import org.springframework.stereotype.Component;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class VpcDistributedRouterSeqNoDaoImpl extends GenericDaoBase<VpcDistributedRouterSeqNoVO, Long> implements VpcDistributedRouterSeqNoDao {
    private SearchBuilder<VpcDistributedRouterSeqNoVO> VpcIdSearch;

    protected VpcDistributedRouterSeqNoDaoImpl() {
        VpcIdSearch = createSearchBuilder();
        VpcIdSearch.and("vmId", VpcIdSearch.entity().getVpcId(), SearchCriteria.Op.EQ);
        VpcIdSearch.done();
    }

    @Override
    public VpcDistributedRouterSeqNoVO findByVpcId(long vpcId) {
        SearchCriteria<VpcDistributedRouterSeqNoVO> sc = VpcIdSearch.create();
        sc.setParameters("vmId", vpcId);
        return findOneIncludingRemovedBy(sc);
    }

}
