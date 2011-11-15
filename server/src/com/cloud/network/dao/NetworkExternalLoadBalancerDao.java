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

import com.cloud.network.NetworkExternalLoadBalancerVO;
import com.cloud.utils.db.GenericDao;

public interface  NetworkExternalLoadBalancerDao extends GenericDao<NetworkExternalLoadBalancerVO, Long> {

    /**
     * find the network to load balancer device mapping corresponding to a network
     * @param networkId guest network Id
     * @return return NetworkExternalLoadBalancerVO for the guest network
     */
    NetworkExternalLoadBalancerVO findByNetworkId(long networkId);

    /**
     * list all network to load balancer device mappings corresponding to a load balancer device Id
     * @param lbDeviceId load balancer device Id
     * @return list of NetworkExternalLoadBalancerVO mappings corresponding to the networks mapped to the load balancer device 
     */
    List<NetworkExternalLoadBalancerVO> listByLoadBalancerDeviceId(long lbDeviceId);
}
