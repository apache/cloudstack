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
