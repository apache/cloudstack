/**
 * 
 */
package com.cloud.agent.api;

import com.cloud.agent.api.to.VirtualMachineTO;

public class Start2Command extends Command {
    VirtualMachineTO vm;
    
    public VirtualMachineTO getVirtualMachine() {
        return vm;
    }
    
    @Override
    public boolean executeInSequence() {
        return true;
    }
    
    protected Start2Command() {
    }
    
    public Start2Command(VirtualMachineTO vm) {
        this.vm = vm;
    }
}
