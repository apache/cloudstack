package com.cloud.network.ovs;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;

public class OvsCreateTunnelAnswer extends Answer {
    Long from;
    Long to;
    long account;
    String inPortName;
    
    //for debug info
    String fromIp;
    String toIp;
    String key;
    String bridge;
    
    public OvsCreateTunnelAnswer(Command cmd, boolean success, String details, String bridge) {
        super(cmd, success, details);
        OvsCreateTunnelCommand c = (OvsCreateTunnelCommand)cmd;
        from = c.getFrom();
        to = c.getTo();
        account = c.getAccount();
        inPortName = "[]";
        fromIp = c.getFromIp();
        toIp = c.getRemoteIp();
        key = c.getKey();
        this.bridge = bridge;
    }
    
    public OvsCreateTunnelAnswer(Command cmd, boolean success, String details, String inPortName, String bridge) {
        this(cmd, success, details, bridge);
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
    
    public String getFromIp() {
        return fromIp;
    }
    
    public String getToIp() {
        return toIp;
    }
    
    public String getKey() {
        return key;
    }
    
    public String getBridge() {
        return bridge;
    }
}
