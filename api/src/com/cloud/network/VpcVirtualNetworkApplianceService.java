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
package com.cloud.network;

import java.util.Map;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.router.VirtualRouter;
import com.cloud.vm.VirtualMachineProfile;

public interface VpcVirtualNetworkApplianceService extends VirtualNetworkApplianceService {

    /**
     * @param router
     * @param network
     * @param isRedundant
     * @param params TODO
     * @return
     * @throws ConcurrentOperationException
     * @throws ResourceUnavailableException
     * @throws InsufficientCapacityException
     */
    boolean addVpcRouterToGuestNetwork(VirtualRouter router, Network network, Map<VirtualMachineProfile.Param, Object> params)
            throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException;

    /**
     * @param router
     * @param network
     * @param isRedundant
     * @return
     * @throws ConcurrentOperationException
     * @throws ResourceUnavailableException
     */
    boolean removeVpcRouterFromGuestNetwork(VirtualRouter router, Network network) throws ConcurrentOperationException, ResourceUnavailableException;

}
