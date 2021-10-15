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

import com.cloud.network.rules.LoadBalancerContainer.Scheme;
import com.cloud.utils.db.GenericDao;

public interface LoadBalancerDao extends GenericDao<LoadBalancerVO, Long> {

    List<LoadBalancerVO> listByIpAddress(long ipAddressId);

    List<LoadBalancerVO> listByNetworkIdAndScheme(long networkId, Scheme scheme);

    List<LoadBalancerVO> listByVpcIdAndScheme(long vpcId, Scheme scheme);

    List<LoadBalancerVO> listByNetworkIdOrVpcIdAndScheme(long networkId, Long vpcId, Scheme scheme);

    List<LoadBalancerVO> listInTransitionStateByNetworkIdAndScheme(long networkId, Scheme scheme);

    boolean isLoadBalancerRulesMappedToVmGuestIp(long instanceId, String instanceIp, long networkId);

}
