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

import java.util.List;

import com.cloud.dc.DataCenterIpv6AddressVO;
import com.cloud.utils.db.GenericDao;

public interface DataCenterIpv6AddressDao extends GenericDao<DataCenterIpv6AddressVO, Long> {

    DataCenterIpv6AddressVO addIpRange(long dcId, long physicalNetworkId, String ip6Gateway, String ip6Cidr, String routerIpv6, String routerIpv6Gateway, String routerIpv6Vlan);

    boolean removeIpv6Range(long id);

    boolean dedicateIpv6Range(long id, Long domainId, Long accountId);

    boolean releaseIpv6Range(long id);

    boolean updateIpRange(long id, String ip6Gateway, String ip6Cidr, String routerIpv6, String routerIpv6Gateway, String routerIpv6Vlan);

    DataCenterIpv6AddressVO takeIpv6Range(long zoneId, boolean isRouterIpv6Null);

    boolean mark(long id, Long networkId, Long domainId, Long accountId);

    boolean mark(long zoneId, String ip6Gateway, String ip6Cidr, long networkId, long domainId, long accountId);

    boolean unmark(long id);

    boolean unmark(long networkId, long domainId, long accountId);

    List<DataCenterIpv6AddressVO> listByZoneDomainAccount(long zoneId, Long networkId, Long domainId, Long accountId);

    String getRouterIpv6ByNetwork(Long networkId);

    String getRouterIpv6GatewayByNetwork(Long networkId);

    String getRouterIpv6VlanByNetwork(Long networkId);
}
