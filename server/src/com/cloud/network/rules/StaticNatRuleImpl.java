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

package com.cloud.network.rules;

import java.util.List;

import com.cloud.network.rules.FirewallRule.FirewallRuleType;


public class StaticNatRuleImpl implements StaticNatRule{
    long id;
    String xid;
    String protocol;
    int portStart;
    int portEnd;
    State state;
    long accountId;
    long domainId;
    long networkId;
    long sourceIpAddressId;
    String destIpAddress;

    public StaticNatRuleImpl(FirewallRuleVO rule, String dstIp) {  
        this.id = rule.getId();
        this.xid = rule.getXid();
        this.protocol = rule.getProtocol();
        this.portStart = rule.getSourcePortStart();
        this.portEnd = rule.getSourcePortEnd();
        this.state = rule.getState();
        this.accountId = rule.getAccountId();
        this.domainId = rule.getDomainId();
        this.networkId = rule.getNetworkId();
        this.sourceIpAddressId = rule.getSourceIpAddressId();
        this.destIpAddress = dstIp;
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public Integer getSourcePortEnd() {
        return portEnd;
    }
    
    @Override
    public Purpose getPurpose() {
        return Purpose.StaticNat;
    }

    @Override
    public State getState() {
        return state;
    }
    
    @Override
    public long getAccountId() {
        return accountId;
    }
    
    @Override
    public long getDomainId() {
        return domainId;
    }
    
    @Override
    public long getNetworkId() {
        return networkId;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public Integer getSourcePortStart() {
        return portStart;
    }

    @Override
    public long getSourceIpAddressId() {
        return sourceIpAddressId;
    }

    @Override
    public String getDestIpAddress() {
        return destIpAddress;
    }

    @Override
    public String getXid() {
        return xid;
    }
    
    @Override
    public Integer getIcmpCode() {
        return null;
    }
    
    @Override
    public Integer getIcmpType() {
        return null;
    }

    @Override
    public List<String> getSourceCidrList() {
        return null;
    }

    @Override
    public Long getRelated() {
        return null;
    }

	@Override
	public FirewallRuleType getType() {
		return FirewallRuleType.User;
	}

}
