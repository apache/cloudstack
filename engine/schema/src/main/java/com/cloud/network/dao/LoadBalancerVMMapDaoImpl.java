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
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;

@Component
public class LoadBalancerVMMapDaoImpl extends GenericDaoBase<LoadBalancerVMMapVO, Long> implements LoadBalancerVMMapDao {

    @Override
    public void remove(long loadBalancerId) {
        SearchCriteria<LoadBalancerVMMapVO> sc = createSearchCriteria();
        sc.addAnd("loadBalancerId", SearchCriteria.Op.EQ, loadBalancerId);

        expunge(sc);
    }

    @Override
    public void remove(long loadBalancerId, List<Long> instanceIds, Boolean revoke) {
        SearchCriteria<LoadBalancerVMMapVO> sc = createSearchCriteria();
        sc.addAnd("loadBalancerId", SearchCriteria.Op.EQ, loadBalancerId);
        sc.addAnd("instanceId", SearchCriteria.Op.IN, instanceIds.toArray());
        if (revoke != null) {
            sc.addAnd("revoke", SearchCriteria.Op.EQ, revoke);
        }

        expunge(sc);
    }

    @Override
    public void remove(long loadBalancerId, long instanceId, String instanceIp, Boolean revoke) {
        SearchCriteria<LoadBalancerVMMapVO> sc = createSearchCriteria();
        sc.addAnd("loadBalancerId", SearchCriteria.Op.EQ, loadBalancerId);
        sc.addAnd("instanceId", SearchCriteria.Op.IN, instanceId);
        sc.addAnd("instanceIp", SearchCriteria.Op.EQ, instanceIp);

        if (revoke != null) {
            sc.addAnd("revoke", SearchCriteria.Op.EQ, revoke);
        }

        expunge(sc);
    }


    @Override
    public List<LoadBalancerVMMapVO> listByInstanceId(long instanceId) {
        SearchCriteria<LoadBalancerVMMapVO> sc = createSearchCriteria();
        sc.addAnd("instanceId", SearchCriteria.Op.EQ, instanceId);

        return listBy(sc);
    }

    @Override
    public List<LoadBalancerVMMapVO> listByInstanceIp(String instanceIp) {
        SearchCriteria<LoadBalancerVMMapVO> sc = createSearchCriteria();
        sc.addAnd("instanceIp", SearchCriteria.Op.EQ, instanceIp);

        return listBy(sc);
    }

    @Override
    public List<LoadBalancerVMMapVO> listByLoadBalancerId(long loadBalancerId) {
        SearchCriteria<LoadBalancerVMMapVO> sc = createSearchCriteria();
        sc.addAnd("loadBalancerId", SearchCriteria.Op.EQ, loadBalancerId);

        return listBy(sc);
    }

    @Override
    public List<LoadBalancerVMMapVO> listByLoadBalancerId(long loadBalancerId, boolean pending) {
        SearchCriteria<LoadBalancerVMMapVO> sc = createSearchCriteria();
        sc.addAnd("loadBalancerId", SearchCriteria.Op.EQ, loadBalancerId);
        sc.addAnd("revoke", SearchCriteria.Op.EQ, pending);

        return listBy(sc);
    }

    @Override
    public LoadBalancerVMMapVO findByLoadBalancerIdAndVmId(long loadBalancerId, long instanceId) {
        SearchCriteria<LoadBalancerVMMapVO> sc = createSearchCriteria();
        sc.addAnd("loadBalancerId", SearchCriteria.Op.EQ, loadBalancerId);
        sc.addAnd("instanceId", SearchCriteria.Op.EQ, instanceId);
        return findOneBy(sc);
    }


    @Override
    public LoadBalancerVMMapVO findByLoadBalancerIdAndVmIdVmIp(long loadBalancerId, long instanceId, String instanceIp) {
        SearchCriteria<LoadBalancerVMMapVO> sc = createSearchCriteria();
        sc.addAnd("loadBalancerId", SearchCriteria.Op.EQ, loadBalancerId);
        sc.addAnd("instanceId", SearchCriteria.Op.EQ, instanceId);
        sc.addAnd("instanceIp", SearchCriteria.Op.EQ, instanceIp);

        return findOneBy(sc);
    }


    @Override
    public boolean isVmAttachedToLoadBalancer(long loadBalancerId) {
        GenericSearchBuilder<LoadBalancerVMMapVO, Long> CountByAccount = createSearchBuilder(Long.class);
        CountByAccount.select(null, Func.COUNT, null);
        CountByAccount.and("loadBalancerId", CountByAccount.entity().getLoadBalancerId(), SearchCriteria.Op.EQ);

        SearchCriteria<Long> sc = CountByAccount.create();
        sc.setParameters("loadBalancerId", loadBalancerId);
        return customSearch(sc, null).get(0) > 0;
    }

    @Override
    public List<LoadBalancerVMMapVO> listByLoadBalancerIdAndVmId(long loadBalancerId, long instanceId) {
        SearchCriteria<LoadBalancerVMMapVO> sc = createSearchCriteria();
        sc.addAnd("loadBalancerId", SearchCriteria.Op.EQ, loadBalancerId);
        sc.addAnd("instanceId", SearchCriteria.Op.EQ, instanceId);
        return listBy(sc);
    }

    @Override
    public int expungeByVmList(List<Long> vmIds, Long batchSize) {
        if (CollectionUtils.isEmpty(vmIds)) {
            return 0;
        }
        SearchBuilder<LoadBalancerVMMapVO> sb = createSearchBuilder();
        sb.and("vmIds", sb.entity().getInstanceId(), SearchCriteria.Op.IN);
        SearchCriteria<LoadBalancerVMMapVO> sc = sb.create();
        sc.setParameters("vmIds", vmIds.toArray());
        return batchExpunge(sc, batchSize);
    }
}
