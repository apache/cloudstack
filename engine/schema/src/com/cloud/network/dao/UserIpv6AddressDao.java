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

import com.cloud.network.UserIpv6AddressVO;
import com.cloud.utils.db.GenericDao;

public interface UserIpv6AddressDao extends GenericDao<UserIpv6AddressVO, Long> {
    List<UserIpv6AddressVO> listByAccount(long accountId);

    List<UserIpv6AddressVO> listByVlanId(long vlanId);

    List<UserIpv6AddressVO> listByDcId(long dcId);

    List<UserIpv6AddressVO> listByNetwork(long networkId);

    public UserIpv6AddressVO findByNetworkIdAndIp(long networkId, String ipAddress);

    List<UserIpv6AddressVO> listByPhysicalNetworkId(long physicalNetworkId);

    long countExistedIpsInNetwork(long networkId);

    long countExistedIpsInVlan(long vlanId);
}
