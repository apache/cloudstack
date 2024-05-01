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

import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class InlineLoadBalancerNicMapDaoImpl extends GenericDaoBase<InlineLoadBalancerNicMapVO, Long> implements InlineLoadBalancerNicMapDao {

    @Override
    public InlineLoadBalancerNicMapVO findByPublicIpAddress(String publicIpAddress) {
        SearchCriteria<InlineLoadBalancerNicMapVO> sc = createSearchCriteria();
        sc.addAnd("publicIpAddress", SearchCriteria.Op.EQ, publicIpAddress);

        return findOneBy(sc);
    }

    @Override
    public InlineLoadBalancerNicMapVO findByNicId(long nicId) {
        SearchCriteria<InlineLoadBalancerNicMapVO> sc = createSearchCriteria();
        sc.addAnd("nicId", SearchCriteria.Op.EQ, nicId);

        return findOneBy(sc);
    }

    @Override
    public int expungeByNicList(List<Long> nicIds, Long batchSize) {
        if (CollectionUtils.isEmpty(nicIds)) {
            return 0;
        }
        SearchBuilder<InlineLoadBalancerNicMapVO> sb = createSearchBuilder();
        sb.and("nicIds", sb.entity().getNicId(), SearchCriteria.Op.IN);
        SearchCriteria<InlineLoadBalancerNicMapVO> sc = sb.create();
        sc.setParameters("nicIds", nicIds.toArray());
        return batchExpunge(sc, batchSize);
    }
}
