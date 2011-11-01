/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
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

/**
 * 
 */
package com.cloud.network.guru;

import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapcityException;
import com.cloud.network.Network;
import com.cloud.network.NetworkProfile;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.utils.component.Adapter;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

/**
 * NetworkGuru has the following functionalities
 *   - Issues the ip address for the network that it is responsible for.
 *   - Designs a virtual network depending on the network offering.
 *   - Implements the virtual network when a virtual machine requires the network to be started.
 *   
 * NetworkManager is responsible for figuring which NetworkGuru to use when 
 * networks are created and nics are designed.
 */
public interface NetworkGuru extends Adapter {
    /**
     * Design a network configuration given the information.
     * @param offering network offering that contains the information.
     * @param plan where is this network configuration will be deployed.
     * @param userSpecified user specified parameters for this network configuration.
     * @param owner owner of this network configuration.
     * @return NetworkConfiguration
     */
    Network design(NetworkOffering offering, DeploymentPlan plan, Network userSpecified, Account owner);

    /**
     * allocate a nic in this network. This method implementation cannot take a long time as it is meant to allocate for
     * the DB.
     * 
     * @param network
     *            configuration to allocate the nic in.
     * @param nic
     *            user specified
     * @param vm
     *            virtual machine the network configuration will be in.
     * @return NicProfile.
     * @throws InsufficientVirtualNetworkCapcityException
     * @throws InsufficientAddressCapacityException
     */
    NicProfile allocate(Network network, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm) throws InsufficientVirtualNetworkCapcityException, InsufficientAddressCapacityException, ConcurrentOperationException;

    /**
     * Fully implement the network configuration as specified.
     * 
     * @param network
     *            network configuration
     * @param offering
     *            offering that the network configuration was based on.
     * @param destination
     *            where were deploying to.
     * @return a fully implemented NetworkConfiguration.
     * @throws InsufficientVirtualNetworkCapcityException
     */
    Network implement(Network network, NetworkOffering offering, DeployDestination destination, ReservationContext context) throws InsufficientVirtualNetworkCapcityException;

    /**
     * reserve a nic for this VM in this network.
     * @param nic
     * @param network
     * @param vm
     * @param dest
     * @return
     * @throws InsufficientVirtualNetworkCapcityException
     * @throws InsufficientAddressCapacityException
     * @throws ConcurrentOperationException 
     */
    void reserve(NicProfile nic, Network network, VirtualMachineProfile<? extends VirtualMachine> vm, DeployDestination dest, ReservationContext context) throws InsufficientVirtualNetworkCapcityException, InsufficientAddressCapacityException, ConcurrentOperationException;

    boolean release(NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm, String reservationId);

    void deallocate(Network network, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm);

    /**
     * @deprecated This method should not be here in the first place. What does this really mean? Is it always persisted
     *             in the nic? When is it persisted in the nic? When is it called? No Idea.
     * @param profile
     * @param network
     */
    @Deprecated
    void updateNicProfile(NicProfile profile, Network network);

    void shutdown(NetworkProfile network, NetworkOffering offering);

    /**
     * Throw away the design.
     * @param network
     * @param offering
     * @param owner
     * @return
     */
    boolean trash(Network network, NetworkOffering offering, Account owner);

    void updateNetworkProfile(NetworkProfile networkProfile);

}
