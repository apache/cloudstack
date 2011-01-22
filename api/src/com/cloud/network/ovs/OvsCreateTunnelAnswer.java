package com.cloud.network.ovs;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;

public class OvsCreateTunnelAnswer extends Answer {
    Long from;
    Long to;
    long account;
    String inPortName;
    
    public OvsCreateTunnelAnswer(Command cmd, boolean success, String details) {
        super(cmd, success, details);
        OvsCreateTunnelCommand c = (OvsCreateTunnelCommand)cmd;
        from = c.getFrom();
        to = c.getTo();
        account = c.getAccount();
        inPortName = "[]";
    }
    
    public OvsCreateTunnelAnswer(Command cmd, boolean success, String details, String inPortName) {
        this(cmd, success, details);
        this.inPortName = inPortName;
    }
    
    
    public Long getFrom() {
        return from;
    }
    
    public Long getTo() {
        return to;
    }
    
    public long getAccount() {
        return account;
    }
    
    public String getInPortName() {
        return inPortName;
    }
}
