/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.cloud.network.element;

import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.offering.NetworkOffering;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.PluggableService;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import org.apache.log4j.Logger;

import javax.ejb.Local;
import java.lang.Class;
import java.util.Map;
import java.util.Set;

@Local(value = NetworkElement.class)
public class MidokuraMidonetElement extends AdapterBase implements ConnectivityProvider, PluggableService {
    private static final Logger s_logger = Logger.getLogger(MidokuraMidonetElement.class);

    @Override
    public Map<Service, Map<Capability, String>> getCapabilities() {
        // TODO: implement this.
        return null;
    }

    @Override
    public Provider getProvider() {
        // TODO: implement this.
        return null;
    }

    @Override
    public boolean implement(Network network, NetworkOffering offering, DeployDestination dest,
                             ReservationContext context)
            throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        // TODO: implement this.
        return false;
    }

    @Override
    public boolean prepare(Network network, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm,
                           DeployDestination dest, ReservationContext context)
            throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        // TODO: implement this.
        return false;
    }

    @Override
    public boolean release(Network network, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm,
                           ReservationContext context)
            throws ConcurrentOperationException, ResourceUnavailableException {
        // TODO: implement this.
        return false;
    }

    @Override
    public boolean shutdown(Network network, ReservationContext context, boolean cleanup)
            throws ConcurrentOperationException, ResourceUnavailableException {
        // TODO: implement this.
        return false;
    }

    @Override
    public boolean destroy(Network network) throws ConcurrentOperationException, ResourceUnavailableException {
        // TODO: implement this.
        return false;
    }

    @Override
    public boolean isReady(PhysicalNetworkServiceProvider provider) {
        // TODO: implement this.
        return false;
    }

    @Override
    public boolean shutdownProviderInstances(PhysicalNetworkServiceProvider provider, ReservationContext context)
            throws ConcurrentOperationException, ResourceUnavailableException {
        // TODO: implement this.
        return false;
    }

    @Override
    public boolean canEnableIndividualServices() {
        // TODO: implement this.
        return false;
    }

    @Override
    public boolean verifyServicesCombination(Set<Service> services) {
        // TODO: implement this.
        return false;
    }

    @Override
    public List<Class<?>> getCommands() {
        // TODO: implement this.
        return null;
    }
}
