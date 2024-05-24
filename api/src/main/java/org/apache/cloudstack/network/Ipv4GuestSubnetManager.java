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
package org.apache.cloudstack.network;

import org.apache.cloudstack.api.command.admin.network.CreateIpv4GuestSubnetCmd;
import org.apache.cloudstack.api.command.admin.network.CreateIpv4SubnetForGuestNetworkCmd;
import org.apache.cloudstack.api.command.admin.network.DedicateIpv4GuestSubnetCmd;
import org.apache.cloudstack.api.command.admin.network.DeleteIpv4GuestSubnetCmd;
import org.apache.cloudstack.api.command.admin.network.DeleteIpv4SubnetForGuestNetworkCmd;
import org.apache.cloudstack.api.command.admin.network.ListIpv4GuestSubnetsCmd;
import org.apache.cloudstack.api.command.admin.network.ListIpv4SubnetsForGuestNetworkCmd;
import org.apache.cloudstack.api.command.admin.network.ReleaseDedicatedIpv4GuestSubnetCmd;
import org.apache.cloudstack.api.command.admin.network.UpdateIpv4GuestSubnetCmd;
import org.apache.cloudstack.api.response.DataCenterIpv4SubnetResponse;
import org.apache.cloudstack.api.response.Ipv4SubnetForGuestNetworkResponse;
import org.apache.cloudstack.datacenter.DataCenterIpv4GuestSubnet;
import org.apache.cloudstack.framework.config.Configurable;

import com.cloud.network.Network;
import com.cloud.utils.component.PluggableService;

import java.util.List;

public interface Ipv4GuestSubnetManager extends PluggableService, Configurable {

    // Methods for DataCenterIpv4GuestSubnet APIs
    DataCenterIpv4GuestSubnet createDataCenterIpv4GuestSubnet(CreateIpv4GuestSubnetCmd createIpv4GuestSubnetCmd);

    DataCenterIpv4SubnetResponse createDataCenterIpv4SubnetResponse(DataCenterIpv4GuestSubnet result);

    boolean deleteDataCenterIpv4GuestSubnet(DeleteIpv4GuestSubnetCmd deleteIpv4GuestSubnetCmd);

    DataCenterIpv4GuestSubnet updateDataCenterIpv4GuestSubnet(UpdateIpv4GuestSubnetCmd updateIpv4GuestSubnetCmd);

    List<? extends DataCenterIpv4GuestSubnet> listDataCenterIpv4GuestSubnets(ListIpv4GuestSubnetsCmd listIpv4GuestSubnetsCmd);

    DataCenterIpv4GuestSubnet dedicateDataCenterIpv4GuestSubnet(DedicateIpv4GuestSubnetCmd dedicateIpv4GuestSubnetCmd);

    DataCenterIpv4GuestSubnet releaseDedicatedDataCenterIpv4GuestSubnet(ReleaseDedicatedIpv4GuestSubnetCmd releaseDedicatedIpv4GuestSubnetCmd);

    // Methods for Ipv4SubnetForGuestNetwork APIs
    Ipv4GuestSubnetNetworkMap createIpv4SubnetForGuestNetwork(CreateIpv4SubnetForGuestNetworkCmd createIpv4SubnetForGuestNetworkCmd);

    boolean deleteIpv4SubnetForGuestNetwork(DeleteIpv4SubnetForGuestNetworkCmd deleteIpv4SubnetForGuestNetworkCmd);

    List<? extends Ipv4GuestSubnetNetworkMap> listIpv4GuestSubnetsForGuestNetwork(ListIpv4SubnetsForGuestNetworkCmd listIpv4SubnetsForGuestNetworkCmd);

    Ipv4SubnetForGuestNetworkResponse createIpv4SubnetForGuestNetworkResponse(Ipv4GuestSubnetNetworkMap subnet);

    // Methods for internal calls
    void getOrCreateIpv4SubnetForGuestNetwork(Network network, String networkCidr);

    void getOrCreateIpv4SubnetForGuestNetwork(Network network, Integer networkCidrSize);

}
