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
package org.apache.cloudstack.network.element;

import com.cloud.deploy.DeployDestination;
import com.cloud.network.Network;
import com.cloud.network.PhysicalNetwork;
import com.cloud.offering.NetworkOffering;
import com.cloud.utils.component.Manager;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;

public interface SspManager extends Manager {
    /**
     * Checks Ssp is activated or not
     * @param physicalNetwork
     * @return true if physicalNetworkProvider is configured
     */
    public boolean canHandle(PhysicalNetwork physicalNetwork);

    /**
     * Tell ssp to create a network
     * @param network
     * @param offering
     * @param dest
     * @param context
     * @return
     */
    public boolean createNetwork(Network network, NetworkOffering offering, DeployDestination dest, ReservationContext context);

    /**
     * Tell ssp to delete a network
     * @param network
     * @return
     */
    public boolean deleteNetwork(Network network);

    /**
     * Create a nic entry in ssp
     * @param network
     * @param nic
     * @param dest
     * @param context
     * @return true on success
     */
    public boolean createNicEnv(Network network, NicProfile nic, DeployDestination dest, ReservationContext context);

    /**
     * Delete a nic entry from ssp
     * @param network
     * @param nic
     * @param context
     * @return
     */
    public boolean deleteNicEnv(Network network, NicProfile nic, ReservationContext context);
}
