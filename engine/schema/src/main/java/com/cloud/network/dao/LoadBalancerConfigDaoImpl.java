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

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.network.lb.LoadBalancerConfig.Scope;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.TransactionLegacy;

public class LoadBalancerConfigDaoImpl extends GenericDaoBase<LoadBalancerConfigVO, Long> implements LoadBalancerConfigDao {

    final SearchBuilder<LoadBalancerConfigVO> AllFieldsSearch;

    public LoadBalancerConfigDaoImpl() {
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("id", AllFieldsSearch.entity().getId(), Op.EQ);
        AllFieldsSearch.and("ids", AllFieldsSearch.entity().getId(), Op.IN);
        AllFieldsSearch.and("uuid", AllFieldsSearch.entity().getUuid(), Op.EQ);
        AllFieldsSearch.and("scope", AllFieldsSearch.entity().getScope(), Op.EQ);
        AllFieldsSearch.and("networkId", AllFieldsSearch.entity().getNetworkId(), Op.EQ);
        AllFieldsSearch.and("vpcId", AllFieldsSearch.entity().getVpcId(), Op.EQ);
        AllFieldsSearch.and("loadBalancerId", AllFieldsSearch.entity().getLoadBalancerId(), Op.EQ);
        AllFieldsSearch.and("name", AllFieldsSearch.entity().getName(), Op.EQ);
        AllFieldsSearch.and("value", AllFieldsSearch.entity().getValue(), Op.EQ);
        AllFieldsSearch.done();
    }

    @Override
    public List<LoadBalancerConfigVO> listByNetworkId(Long networkId) {
        SearchCriteria<LoadBalancerConfigVO> sc = AllFieldsSearch.create();
        sc.setParameters("scope", Scope.Network);
        sc.setParameters("networkId", networkId);
        return listBy(sc);
    }

    @Override
    public List<LoadBalancerConfigVO> listByVpcId(Long vpcId) {
        SearchCriteria<LoadBalancerConfigVO> sc = AllFieldsSearch.create();
        sc.setParameters("scope", Scope.Vpc);
        sc.setParameters("vpcId", vpcId);
        return listBy(sc);
    }

    @Override
    public List<LoadBalancerConfigVO> listByLoadBalancerId(Long loadBalancerId) {
        SearchCriteria<LoadBalancerConfigVO> sc = AllFieldsSearch.create();
        sc.setParameters("scope", Scope.LoadBalancerRule);
        sc.setParameters("loadBalancerId", loadBalancerId);
        return listBy(sc);
    }

    @Override
    public void removeByNetworkId(Long networkId) {
        SearchCriteria<LoadBalancerConfigVO> sc = AllFieldsSearch.create();
        sc.setParameters("scope", Scope.Network);
        sc.setParameters("networkId", networkId);
        remove(sc);
    }

    @Override
    public void removeByVpcId(Long vpcId) {
        SearchCriteria<LoadBalancerConfigVO> sc = AllFieldsSearch.create();
        sc.setParameters("scope", Scope.Vpc);
        sc.setParameters("vpcId", vpcId);
        remove(sc);
    }

    @Override
    public void removeByLoadBalancerId(Long loadBalancerId) {
        SearchCriteria<LoadBalancerConfigVO> sc = AllFieldsSearch.create();
        sc.setParameters("scope", Scope.LoadBalancerRule);
        sc.setParameters("loadBalancerId", loadBalancerId);
        remove(sc);
    }

    @Override
    public LoadBalancerConfigVO findConfig(Scope scope, Long networkId, Long vpcId, Long loadBalancerId, String name) {
        SearchCriteria<LoadBalancerConfigVO> sc = AllFieldsSearch.create();
        sc.setParametersIfNotNull("scope", scope);
        sc.setParametersIfNotNull("networkId", networkId);
        sc.setParametersIfNotNull("vpcId", vpcId);
        sc.setParametersIfNotNull("loadBalancerId", loadBalancerId);
        sc.setParametersIfNotNull("name", name);
        return findOneBy(sc);
    }

    @Override
    public void addConfig(LoadBalancerConfigVO config) {
        if (config == null) {
            return;
        }
        LoadBalancerConfigVO existingConfig = findConfig(config.getScope(), config.getNetworkId(), config.getVpcId(), config.getLoadBalancerId(), config.getName());
        if (existingConfig != null) {
            remove(existingConfig.getId());
        }
        persist(config);
    }

    @Override
    public List<LoadBalancerConfigVO> saveConfigs(List<LoadBalancerConfigVO> configs) {
        if (configs.isEmpty()) {
            return new ArrayList<LoadBalancerConfigVO>();
        }
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        SearchCriteria<LoadBalancerConfigVO> sc = AllFieldsSearch.create();
        sc.setParameters("scope", configs.get(0).getScope());
        Long networkId = configs.get(0).getNetworkId();
        if (networkId != null) {
            sc.setParameters("networkId", networkId);
        }
        Long vpcId = configs.get(0).getVpcId();
        if (vpcId != null) {
            sc.setParameters("vpcId", vpcId);
        }
        Long loadBalancerId = configs.get(0).getLoadBalancerId();
        if (loadBalancerId != null) {
            sc.setParameters("loadBalancerId", loadBalancerId);
        }
        expunge(sc);

        List<Long> ids = new ArrayList<Long>();
        for (LoadBalancerConfigVO config : configs) {
            config = persist(config);
            ids.add(config.getId());
        }
        txn.commit();

        SearchCriteria<LoadBalancerConfigVO> sc2 = AllFieldsSearch.create();
        sc2.setParameters("ids", ids.toArray(new Object[ids.size()]));
        return listBy(sc2);
    }
}
