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

import com.cloud.utils.db.GenericDao;

import org.apache.cloudstack.network.lb.LoadBalancerConfig.Scope;

public interface LoadBalancerConfigDao extends GenericDao<LoadBalancerConfigVO, Long> {

    List<LoadBalancerConfigVO> listByNetworkId(Long networkId);

    List<LoadBalancerConfigVO> listByVpcId(Long vpcId);

    List<LoadBalancerConfigVO> listByLoadBalancerId(Long loadBalancerId);

    void removeByNetworkId(Long networkId);

    void removeByVpcId(Long vpcId);

    void removeByLoadBalancerId(Long loadBalancerId);

    LoadBalancerConfigVO findConfig(Scope scope, Long networkId, Long vpcId, Long loadBalancerId, String name);

    void addConfig(LoadBalancerConfigVO config);

    List<LoadBalancerConfigVO> saveConfigs(List<LoadBalancerConfigVO> configs);
}
