/**
 * 
 */
package com.cloud.network.configuration;

import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapcityException;
import com.cloud.network.NetworkConfiguration;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.utils.component.Adapter;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VirtualMachineProfile;

/**
 * NetworkGuru takes a network offering requested and figures
 * out what is the correct network configuration that are needed to add
 * to the account in order to support this network. 
 *
 */
public interface NetworkGuru extends Adapter {
    NetworkConfiguration design(NetworkOffering offering, DeploymentPlan plan, NetworkConfiguration userSpecified, Account owner);
    
    NetworkConfiguration implement(NetworkConfiguration config, NetworkOffering offering, DeployDestination destination);
    
    NicProfile allocate(NetworkConfiguration config, NicProfile nic, VirtualMachineProfile vm) throws InsufficientVirtualNetworkCapcityException, InsufficientAddressCapacityException;
    
//    NicProfile create(NicProfile nic, NetworkConfiguration config, VirtualMachineProfile vm, DeployDestination dest) throws InsufficientVirtualNetworkCapcityException, InsufficientAddressCapacityException;
    
    String reserve(NicProfile nic, NetworkConfiguration config, VirtualMachineProfile vm, DeployDestination dest) throws InsufficientVirtualNetworkCapcityException, InsufficientAddressCapacityException;
    
    boolean release(String uniqueId);
    
    void destroy(NetworkConfiguration config, NetworkOffering offering);
}
