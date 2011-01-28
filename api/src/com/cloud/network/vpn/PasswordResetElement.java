package com.cloud.network.vpn;

import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

public interface PasswordResetElement {
    
    boolean savePassword(Network network, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm) throws ResourceUnavailableException;
}
