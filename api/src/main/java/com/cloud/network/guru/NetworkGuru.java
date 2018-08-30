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
package com.cloud.network.guru;

import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapacityException;
import com.cloud.network.Network;
import com.cloud.network.NetworkProfile;
import com.cloud.network.Networks.TrafficType;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.utils.component.Adapter;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachineProfile;

/**
 *   - Designs a virtual network depending on the network offering.
 *   - Implements the virtual network when a virtual machine requires the network to be started.
 *
 * There can be multiple NetworkGurus in a CloudStack system.  Each NetworkGuru
 * resources when VMs are gone.
 *
 * A Network goes through the following life cycles through the NetworkGuru.
 *   - When a guest network is created, NetworkGuru is asked to "design" the network.
 *     This means the NetworkGuru checks the parameters such as cidr, gateway,
 *     vlan, etc and returns a network that can work with those paremeters.
 *     Note that at this point the network is only a virtual network.  It has
 *     not been substantiated with resources, such as vlan, to make the network
 *     functional in the physical environment.  At this stage, the network is in
 *     Allocated state.
 *
 *   - When the first virtual machine is about to be started and requires network
 *     services, the guest network needs to have resources to make it usable
 *     within the physical environment.  At this time, the NetworkGuru is
 *     called with the implement() method to acquire those resources.
 *
 *   - For every virtual machine starting in the network, the NetworkGuru is
 *     asked via the reserve() method to make sure everything the virtual
 *     machine needs to be functional in the network is reserved.
 *
 *   - For every virtual machine being stopped in the network, the NetworkGuru
 *     is informed via the release() method to make sure resources occupied
 *     by the virtual machine is released.
 *
 *   - If all virtual machines within the network have been stopped, the guest
 *     network is garbage collected.  When a guest network is garbage collected
 *     the NetworkGuru is informed via the shutdown() method to release any
 *     resources it allocated to that network.
 *
 *   - When a guest network is being deleted, the NetworkGuru is informed via
 *     the trash() method.
 *
 */
public interface NetworkGuru extends Adapter {
    /**
     * Cloud stack requires the NetworkGuru to design a guest network given
     * the software packages  Once a NetworkGuru returns the designed network,
     * that NetworkGuru is forever associated with the guest network.  It is
     * very important for the NetworkGuru implementation to be very specific
     * about the network it is responsible for designing.  Things that can
     * be used to make determination can be isolation methods, services
     * provided on the guest network and the service provider that's on the
     * guest network.
     *
     * If a network is already fully substantiated with the necessary resources
     * during this design phase, then the state should be set to Setup.  If
     * the resources are not allocated at this point, the state should be set
     * to Allocated.
     *
     * @param offering network offering that contains the package of services
     *                 the end user intends to use on that network.
     * @param plan where is this network being deployed.
     * @param userSpecified user specified parameters for this network.
     * @param owner owner of this network.
     * @return Network
     */
    Network design(NetworkOffering offering, DeploymentPlan plan, Network userSpecified, Account owner);

    /**
     * For guest networks that are in Allocated state after the design stage,
     * resources are allocated when the guest network is actually being used
     * by a virtual machine.  implement() is called to acquire those resources.
     *
     * @param network network to be implemented.
     * @param offering network offering that the network was created with.
     * @param destination where the network is being deployed in.
     * @return a fully implemented Network.
     * @throws InsufficientVirtualNetworkCapacityException  if there's not
     * enough resources to make the guest network usable in the physical
     * environment.  At this time, the admin generally must be involved to
     * allocate more resources before any more guest network can be implemented.
     */
    Network implement(Network network, NetworkOffering offering, DeployDestination destination, ReservationContext context)
        throws InsufficientVirtualNetworkCapacityException;

    /**
     * Once a guest network has been designed, virtual machines can be
     * created.  allocated() is called for the NetworkGuru to design a nic
     * that will make the virtual machine work within the guest network.
     *
     * @param network guest network that the virtual machine will be deployed in.
     * @param nic nic information that the end user wants to set.  The
     *            NetworkGuru should check this information with the guest
     *            network settings to make sure everything will work.
     * @param vm virtual machine that is about to be deployed.
     * @return NicProfile nic with all of the information
     * @throws InsufficientVirtualNetworkCapacityException if there's
     *         insufficient capacity within the guest network.
     * @throws InsufficientAddressCapacityException if there are not addresses
     *         to be assigned.
     */
    NicProfile allocate(Network network, NicProfile nic, VirtualMachineProfile vm) throws InsufficientVirtualNetworkCapacityException,
        InsufficientAddressCapacityException, ConcurrentOperationException;

    /**
     * Once a guest network is implemented, then the virtual machine must
     * be allocated its resources in order for it to participate within the
     * guest network.  reserve() is called for the NetworkGuru to make sure
     * that works.
     *
     * @param nic nic that the vm is using to access the guest network.
     * @param network guest network the vm is in.
     * @param vm vm
     * @param dest destination the vm is deployed to
     * @param context Reservation context from which to get the owner, caller, and reservation id
     * @throws InsufficientVirtualNetworkCapacityException if there's not enough
     *         resources.
     * @throws InsufficientAddressCapacityException if there's not enough ip
     *         addresses.
     * @throws ConcurrentOperationException if there are multiple operations
     *         happening on this guest network or vm.
     */
    void reserve(NicProfile nic, Network network, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context)
        throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException, ConcurrentOperationException;

    /**
     * When a virtual machine is stopped, the NetworkGuru is informed via the
     * release() method to release any resources.
     *
     * @param nic nic that the vm is using to access the guest network.
     * @param vm virtual machine
     * @param reservationId reservation id passed to it in the ReservationContext
     * @return true if release is successful or false if unsuccessful.
     */
    boolean release(NicProfile nic, VirtualMachineProfile vm, String reservationId);

    /**
     * When a virtual machine is destroyed, the NetworkGuru is informed via
     * the deallocate() method to make sure any resources that are allocated
     * are released.
     *
     * @param network guest network that the vm was running in.
     * @param nic nic that the vm was using to access the guest network.
     * @param vm virtual machine being destroyed.
     */
    void deallocate(Network network, NicProfile nic, VirtualMachineProfile vm);

    /**
     * @deprecated This method should not be here in the first place. What does this really mean? Is it always persisted
     *             in the nic? When is it persisted in the nic? When is it called? No Idea.
     * @param profile
     * @param network
     */
    @Deprecated
    void updateNicProfile(NicProfile profile, Network network);

    /**
     * When no virtual machines are running in the network, the network is
     * shutdown and all physical resources are released.  The NetworkGuru is
     * informed via the shutdown method().
     *
     * @param network guest network being shut down
     * @param offering network offering the guest network was created with.
     */
    void shutdown(NetworkProfile network, NetworkOffering offering);

    /**
     * When a guest network is destroyed, the NetworkGuru is informed via the
     * trash() method to recover any resources.
     *
     * @param network guest network being destroyed.
     * @param offering network offering the guest network was created with.
     * @return true if trash was successful; false if not.
     */
    boolean trash(Network network, NetworkOffering offering);

    void updateNetworkProfile(NetworkProfile networkProfile);

    TrafficType[] getSupportedTrafficType();

    boolean isMyTrafficType(TrafficType type);

}
