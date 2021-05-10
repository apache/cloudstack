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

import java.util.List;

import org.apache.cloudstack.api.command.admin.router.UpgradeRouterCmd;
import org.apache.cloudstack.api.command.admin.router.UpgradeRouterTemplateCmd;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.router.VirtualRouter;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.vm.Nic;

public interface VirtualNetworkApplianceService {
    /**
     * Starts domain router
     *
     * @param cmd the command specifying router's id
     * @return DomainRouter object
     */
    VirtualRouter startRouter(long routerId, boolean reprogramNetwork) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException;

    /**
     * Reboots domain router
     *
     * @param cmd
     *            the command specifying router's id
     * @return router if successful
     */
    VirtualRouter rebootRouter(long routerId, boolean reprogramNetwork, boolean forced) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException;

    VirtualRouter upgradeRouter(UpgradeRouterCmd cmd);

    /**
     * Stops domain router
     *
     * @param id of the router
     * @param forced just do it. caller knows best.
     * @return router if successful, null otherwise
     * @throws ResourceUnavailableException
     * @throws ConcurrentOperationException
     */
    VirtualRouter stopRouter(long routerId, boolean forced) throws ResourceUnavailableException, ConcurrentOperationException;

    VirtualRouter startRouter(long id) throws ResourceUnavailableException, InsufficientCapacityException, ConcurrentOperationException;

    VirtualRouter destroyRouter(long routerId, Account caller, Long callerUserId) throws ResourceUnavailableException, ConcurrentOperationException;

    VirtualRouter findRouter(long routerId);

    List<Long> upgradeRouterTemplate(UpgradeRouterTemplateCmd cmd);

    /**
     * Updates router with latest health checkdata, runs health checks and persists health checks on virtual router if feasible.
     * Throws relevant exception if feature is disabled or failures occur.
     *
     * @param routerId id of the router
     * @return
     */
    Pair<Boolean, String> performRouterHealthChecks(long routerId);

    <T extends VirtualRouter> void collectNetworkStatistics(T router, Nic nic);
}
