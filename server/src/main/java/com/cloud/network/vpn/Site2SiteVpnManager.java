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
package com.cloud.network.vpn;

import java.util.List;
import java.util.Set;

import org.apache.cloudstack.framework.config.ConfigKey;

import com.cloud.network.Site2SiteCustomerGateway;
import com.cloud.network.dao.Site2SiteVpnConnectionVO;
import com.cloud.vm.DomainRouterVO;

public interface Site2SiteVpnManager extends Site2SiteVpnService {

    ConfigKey<Integer> Site2SiteVpnConnectionPerVpnGatewayLimit = new ConfigKey<>("Network", Integer.class,
            "site2site.vpn.vpngateway.connection.limit", "4",
            "The maximum number of VPN connection per VPN gateway", true);

    ConfigKey<Integer> Site2SiteVpnSubnetsPerCustomerGatewayLimit = new ConfigKey<>("Network", Integer.class,
            "site2site.vpn.customergateway.subnets.limit", "10",
            "The maximum number of subnets per customer gateway", true);

    Set<String> getExcludedVpnGatewayParameters(Site2SiteCustomerGateway customerGw);

    Set<String> getObsoleteVpnGatewayParameters(Site2SiteCustomerGateway customerGw);

    boolean cleanupVpnConnectionByVpc(long vpcId);

    boolean cleanupVpnGatewayByVpc(long vpcId);

    void markDisconnectVpnConnByVpc(long vpcId);

    List<Site2SiteVpnConnectionVO> getConnectionsForRouter(DomainRouterVO router);

    boolean deleteCustomerGatewayByAccount(long accountId);

    void reconnectDisconnectedVpnByVpc(Long vpcId);
}
