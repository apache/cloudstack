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
 * NetworkGuru takes a network offering requested and figures
 * out the correct network configuration needed to add
 * to the account in order to support this network. 
 *
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
     * allocate a nic in this network.  This method implementation cannot take a long time as 
     * it is meant to allocate for the DB.
     * @param network configuration to allocate the nic in.
     * @param nic user specified 
     * @param vm virtual machine the network configuraiton will be in.
     * @return NicProfile.
     * @throws InsufficientVirtualNetworkCapcityException
     * @throws InsufficientAddressCapacityException
     */
    NicProfile allocate(Network network, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm) throws InsufficientVirtualNetworkCapcityException, InsufficientAddressCapacityException, ConcurrentOperationException;
    
    /**
     * Fully implement the network configuration as specified.
     * @param network network configuration 
     * @param offering offering that the network configuration was based on.
     * @param destination where were deploying to.
     * @return a fully implemented NetworkConfiguration.
     */
    Network implement(Network network, NetworkOffering offering, DeployDestination destination, ReservationContext context);
    
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
    
    void updateNicProfile(NicProfile profile, Network network);
    
    void destroy(Network network, NetworkOffering offering);
    
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
