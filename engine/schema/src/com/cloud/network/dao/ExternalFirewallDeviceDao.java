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

import com.cloud.network.dao.ExternalFirewallDeviceVO.FirewallDeviceAllocationState;
import com.cloud.network.dao.ExternalFirewallDeviceVO.FirewallDeviceState;
import com.cloud.utils.db.GenericDao;

public interface ExternalFirewallDeviceDao extends GenericDao<ExternalFirewallDeviceVO, Long> {

    /**
     * list all the firewall devices added in to this physical network?
     * @param physicalNetworkId physical Network Id
     * @return list of ExternalFirewallDeviceVO for the devices added in to this physical network.
     */
    List<ExternalFirewallDeviceVO> listByPhysicalNetwork(long physicalNetworkId);

    /**
     * list the firewall devices added in to this physical network of certain provider type?
     * @param physicalNetworkId physical Network Id
     * @param providerName netwrok service provider name
     */
    List<ExternalFirewallDeviceVO> listByPhysicalNetworkAndProvider(long physicalNetworkId, String providerName);

    /**
     * list the firewall devices added in to this physical network by their allocation state
     * @param physicalNetworkId physical Network Id
     * @param providerName netwrok service provider name
     * @param allocationState firewall device allocation state
     * @return list of ExternalFirewallDeviceVO for the devices in the physical network with a device allocation state
     */
    List<ExternalFirewallDeviceVO> listByProviderAndDeviceAllocationState(long physicalNetworkId, String providerName, FirewallDeviceAllocationState allocationState);

    /**
     * list the load balancer devices added in to this physical network by the device status (enabled/disabled)
     * @param physicalNetworkId physical Network Id
     * @param providerName netwrok service provider name
     * @param state firewall device status
     * @return list of ExternalFirewallDeviceVO for the devices in the physical network with a device state
     */
    List<ExternalFirewallDeviceVO> listByProviderAndDeviceStaus(long physicalNetworkId, String providerName, FirewallDeviceState state);
}
