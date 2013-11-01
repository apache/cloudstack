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

package org.apache.cloudstack.region.gslb;

import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.springframework.stereotype.Component;

import javax.ejb.Local;
import java.util.List;

@Component
@Local(value={GlobalLoadBalancerLbRuleMapDao.class})
@DB()
public class GlobalLoadBalancerLbRuleMapDaoImpl extends GenericDaoBase<GlobalLoadBalancerLbRuleMapVO, Long> implements GlobalLoadBalancerLbRuleMapDao {

    private final SearchBuilder<GlobalLoadBalancerLbRuleMapVO> listByGslbRuleId;
    private final SearchBuilder<GlobalLoadBalancerLbRuleMapVO> listByLbGslbRuleId;

    public GlobalLoadBalancerLbRuleMapDaoImpl() {
        listByGslbRuleId = createSearchBuilder();
        listByGslbRuleId.and("gslbLoadBalancerId", listByGslbRuleId.entity().getGslbLoadBalancerId(), SearchCriteria.Op.EQ);
        listByGslbRuleId.done();

        listByLbGslbRuleId = createSearchBuilder();
        listByLbGslbRuleId.and("gslbLoadBalancerId", listByLbGslbRuleId.entity().getGslbLoadBalancerId(), SearchCriteria.Op.EQ);
        listByLbGslbRuleId.and("loadBalancerId", listByLbGslbRuleId.entity().getLoadBalancerId(), SearchCriteria.Op.EQ);
        listByLbGslbRuleId.done();
    }

    @Override
    public List<GlobalLoadBalancerLbRuleMapVO> listByGslbRuleId(long gslbRuleId) {
        SearchCriteria<GlobalLoadBalancerLbRuleMapVO> sc = listByGslbRuleId.create();
        sc.setParameters("gslbLoadBalancerId", gslbRuleId);
        return listBy(sc);
    }

    @Override
    public GlobalLoadBalancerLbRuleMapVO findByGslbRuleIdAndLbRuleId(long gslbRuleId, long lbRuleId) {
        SearchCriteria<GlobalLoadBalancerLbRuleMapVO> sc = listByLbGslbRuleId.create();
        sc.setParameters("gslbLoadBalancerId", gslbRuleId);
        sc.setParameters("loadBalancerId", lbRuleId);
        return findOneBy(sc);
    }

}
