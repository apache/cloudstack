package com.cloud.network.ovs;

import com.cloud.agent.api.Command;

public class OvsDestroyTunnelCommand extends Command {
    long account;
    String inPortName;
    
    public OvsDestroyTunnelCommand(long account, String inPortName) {
        this.account = account;
        this.inPortName = inPortName;
    }
    
    public long getAccount() {
        return account;
    }
    
    public String getInPortName() {
        return inPortName;
    }
    
    @Override
    public boolean executeInSequence() {
        return true;
    }

}
