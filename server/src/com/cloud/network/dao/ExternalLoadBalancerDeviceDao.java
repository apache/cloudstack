/**
 *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.network.dao;

import java.util.List;
import com.cloud.network.ExternalLoadBalancerDeviceVO;
import com.cloud.network.ExternalLoadBalancerDeviceVO.LBDeviceAllocationState;
import com.cloud.network.ExternalLoadBalancerDeviceVO.LBDeviceState;
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
     * @param provider_name netwrok service provider name
     * @return list of ExternalLoadBalancerDeviceVO for the devices in the physical network of a provider type
     */
    List<ExternalLoadBalancerDeviceVO> listByPhysicalNetworkAndProvider(long physicalNetworkId, String provider_name);

    /**
     * list the load balancer devices added in to this physical network by their allocation state
     * @param physicalNetworkId physical Network Id
     * @param provider_name netwrok service provider name
     * @param allocationState load balancer device allocation state
     * @return list of ExternalLoadBalancerDeviceVO for the devices in the physical network with a device allocation state
     */
    List<ExternalLoadBalancerDeviceVO> listByProviderAndDeviceAllocationState(long physicalNetworkId, String provider_name, LBDeviceAllocationState allocationState);

    /**
     * list the load balancer devices added in to this physical network by the device status (enabled/disabled)
     * @param physicalNetworkId physical Network Id
     * @param provider_name netwrok service provider name
     * @param state load balancer device status
     * @return list of ExternalLoadBalancerDeviceVO for the devices in the physical network with a device state
     */
    List<ExternalLoadBalancerDeviceVO> listByProviderAndDeviceStaus(long physicalNetworkId, String provider_name, LBDeviceState state);

    /**
     * list the load balancer devices added in to this physical network by the managed type (external/cloudstack managed)
     * @param physicalNetworkId physical Network Id
     * @param provider_name netwrok service provider name
     * @param managed managed type
     * @return list of ExternalLoadBalancerDeviceVO for the devices in to this physical network of a managed type
     */
    List<ExternalLoadBalancerDeviceVO> listByProviderAndManagedType(long physicalNetworkId, String provider_name, boolean managed);
}
