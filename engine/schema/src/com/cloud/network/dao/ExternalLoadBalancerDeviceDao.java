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

import com.cloud.network.dao.ExternalLoadBalancerDeviceVO.LBDeviceAllocationState;
import com.cloud.network.dao.ExternalLoadBalancerDeviceVO.LBDeviceState;
import com.cloud.utils.db.GenericDao;

public interface ExternalLoadBalancerDeviceDao extends GenericDao<ExternalLoadBalancerDeviceVO, Long> {

    /**
     * list all the load balancer devices added in to this physical network?
     * @param physicalNetworkId physical Network Id
     * @return list of ExternalLoadBalancerDeviceVO for the devices in the physical network.
     */
    List<ExternalLoadBalancerDeviceVO> listByPhysicalNetwork(long physicalNetworkId);

    /**
     * list the load balancer devices added in to this physical network of certain provider type?
     * @param physicalNetworkId physical Network Id
     * @param providerName netwrok service provider name
     */
    List<ExternalLoadBalancerDeviceVO> listByPhysicalNetworkAndProvider(long physicalNetworkId, String providerName);

    /**
     * list the load balancer devices added in to this physical network by their allocation state
     * @param physicalNetworkId physical Network Id
     * @param providerName netwrok service provider name
     * @param allocationState load balancer device allocation state
     * @return list of ExternalLoadBalancerDeviceVO for the devices in the physical network with a device allocation state
     */
    List<ExternalLoadBalancerDeviceVO> listByProviderAndDeviceAllocationState(long physicalNetworkId, String providerName, LBDeviceAllocationState allocationState);

    /**
     * list the load balancer devices added in to this physical network by the device status (enabled/disabled)
     * @param physicalNetworkId physical Network Id
     * @param providerName netwrok service provider name
     * @param state load balancer device status
     * @return list of ExternalLoadBalancerDeviceVO for the devices in the physical network with a device state
     */
    List<ExternalLoadBalancerDeviceVO> listByProviderAndDeviceStaus(long physicalNetworkId, String providerName, LBDeviceState state);

    /**
     * list the load balancer devices added in to this physical network by the managed type (external/cloudstack managed)
     * @param physicalNetworkId physical Network Id
     * @param providerName netwrok service provider name
     * @param managed managed type
     * @return list of ExternalLoadBalancerDeviceVO for the devices in to this physical network of a managed type
     */
    List<ExternalLoadBalancerDeviceVO> listByProviderAndManagedType(long physicalNetworkId, String providerName, boolean managed);

    /**
     * Find the external load balancer device that is provisioned as GSLB service provider in the pyshical network
     * @param physicalNetworkId physical Network Id
     * @return ExternalLoadBalancerDeviceVO for the device acting as GSLB provider in the physical network
     */
    ExternalLoadBalancerDeviceVO findGslbServiceProvider(long physicalNetworkId, String providerName);
}
