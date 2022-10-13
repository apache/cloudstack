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


import org.springframework.stereotype.Component;

import com.cloud.network.Network.Service;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

@Component
@DB()
public class PhysicalNetworkServiceProviderDaoImpl extends GenericDaoBase<PhysicalNetworkServiceProviderVO, Long> implements PhysicalNetworkServiceProviderDao {
    final SearchBuilder<PhysicalNetworkServiceProviderVO> physicalNetworkSearch;
    final SearchBuilder<PhysicalNetworkServiceProviderVO> physicalNetworkServiceProviderSearch;
    final SearchBuilder<PhysicalNetworkServiceProviderVO> AllFieldsSearch;

    protected PhysicalNetworkServiceProviderDaoImpl() {
        super();
        physicalNetworkSearch = createSearchBuilder();
        physicalNetworkSearch.and("physicalNetworkId", physicalNetworkSearch.entity().getPhysicalNetworkId(), Op.EQ);
        physicalNetworkSearch.done();

        physicalNetworkServiceProviderSearch = createSearchBuilder();
        physicalNetworkServiceProviderSearch.and("physicalNetworkId", physicalNetworkServiceProviderSearch.entity().getPhysicalNetworkId(), Op.EQ);
        physicalNetworkServiceProviderSearch.and("serviceProvderType", physicalNetworkServiceProviderSearch.entity().getProviderName(), Op.EQ);
        physicalNetworkServiceProviderSearch.done();

        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("physicalNetworkId", AllFieldsSearch.entity().getPhysicalNetworkId(), Op.EQ);
        AllFieldsSearch.and("serviceProvderType", AllFieldsSearch.entity().getProviderName(), Op.EQ);
        AllFieldsSearch.and("state", AllFieldsSearch.entity().getState(), Op.EQ);
        AllFieldsSearch.and("vpnService", AllFieldsSearch.entity().isVpnServiceProvided(), Op.EQ);
        AllFieldsSearch.and("dhcpService", AllFieldsSearch.entity().isDhcpServiceProvided(), Op.EQ);
        AllFieldsSearch.and("dnsService", AllFieldsSearch.entity().isDnsServiceProvided(), Op.EQ);
        AllFieldsSearch.and("gatewayService", AllFieldsSearch.entity().isGatewayServiceProvided(), Op.EQ);
        AllFieldsSearch.and("firewallService", AllFieldsSearch.entity().isFirewallServiceProvided(), Op.EQ);
        AllFieldsSearch.and("sourceNatService", AllFieldsSearch.entity().isSourcenatServiceProvided(), Op.EQ);
        AllFieldsSearch.and("lbService", AllFieldsSearch.entity().isLbServiceProvided(), Op.EQ);
        AllFieldsSearch.and("staticNatService", AllFieldsSearch.entity().isStaticnatServiceProvided(), Op.EQ);
        AllFieldsSearch.and("pfService", AllFieldsSearch.entity().isPortForwardingServiceProvided(), Op.EQ);
        AllFieldsSearch.and("userDataService", AllFieldsSearch.entity().isUserdataServiceProvided(), Op.EQ);
        AllFieldsSearch.and("securityGroupService", AllFieldsSearch.entity().isSecuritygroupServiceProvided(), Op.EQ);
        AllFieldsSearch.done();
    }

    @Override
    public List<PhysicalNetworkServiceProviderVO> listBy(long physicalNetworkId) {
        SearchCriteria<PhysicalNetworkServiceProviderVO> sc = physicalNetworkSearch.create();
        sc.setParameters("physicalNetworkId", physicalNetworkId);
        return search(sc, null);
    }

    @Override
    public PhysicalNetworkServiceProviderVO findByServiceProvider(long physicalNetworkId, String providerType) {
        SearchCriteria<PhysicalNetworkServiceProviderVO> sc = physicalNetworkServiceProviderSearch.create();
        sc.setParameters("physicalNetworkId", physicalNetworkId);
        sc.setParameters("serviceProvderType", providerType);
        return findOneBy(sc);
    }

    @Override
    public void deleteProviders(long physicalNetworkId) {
        SearchCriteria<PhysicalNetworkServiceProviderVO> sc = physicalNetworkSearch.create();
        sc.setParameters("physicalNetworkId", physicalNetworkId);
        remove(sc);
    }

    @Override
    public boolean isServiceProviderEnabled(long physicalNetworkId, String providerType, String serviceType) {
        SearchCriteria<PhysicalNetworkServiceProviderVO> sc = AllFieldsSearch.create();
        sc.setParameters("physicalNetworkId", physicalNetworkId);
        sc.setParameters("serviceProvderType", providerType);
        sc.setParameters("state", PhysicalNetworkServiceProvider.State.Enabled.toString());

        if (serviceType.equalsIgnoreCase(Service.Dhcp.getName())) {
            sc.setParameters("dhcpService", true);
        } else if (serviceType.equalsIgnoreCase(Service.Dns.getName())) {
            sc.setParameters("dnsService", true);
        } else if (serviceType.equalsIgnoreCase(Service.Firewall.getName())) {
            sc.setParameters("firewallService", true);
        } else if (serviceType.equalsIgnoreCase(Service.Gateway.getName())) {
            sc.setParameters("gatewayService", true);
        } else if (serviceType.equalsIgnoreCase(Service.Lb.getName())) {
            sc.setParameters("lbService", true);
        } else if (serviceType.equalsIgnoreCase(Service.PortForwarding.getName())) {
            sc.setParameters("pfService", true);
        } else if (serviceType.equalsIgnoreCase(Service.SecurityGroup.getName())) {
            sc.setParameters("securityGroupService", true);
        } else if (serviceType.equalsIgnoreCase(Service.SourceNat.getName())) {
            sc.setParameters("sourceNatService", true);
        } else if (serviceType.equalsIgnoreCase(Service.StaticNat.getName())) {
            sc.setParameters("staticNatService", true);
        } else if (serviceType.equalsIgnoreCase(Service.UserData.getName())) {
            sc.setParameters("userDataService", true);
        } else if (serviceType.equalsIgnoreCase(Service.Vpn.getName())) {
            sc.setParameters("vpnService", true);
        }

        PhysicalNetworkServiceProviderVO map = findOneBy(sc);

        if (map != null) {
            return true;
        } else {
            return false;
        }
    }

}
