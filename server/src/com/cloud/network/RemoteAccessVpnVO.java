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

package com.cloud.network;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.utils.net.Ip;

@Entity
@Table(name=("remote_access_vpn"))
public class RemoteAccessVpnVO implements RemoteAccessVpn {
    @Column(name="account_id")
    private long accountId;

    @Column(name="network_id")
    private long networkId;
    
    @Column(name="domain_id")
    private long domainId;

    @Id
    @Column(name="vpn_server_addr")
    @Enumerated(value=EnumType.ORDINAL)
    private Ip serverAddress;
    
    @Column(name="local_ip")
    private String localIp;

    @Column(name="ip_range")
    private String ipRange;

    @Column(name="ipsec_psk")
    private String ipsecPresharedKey;
    
    @Column(name="state")
    private State state;

    public RemoteAccessVpnVO() { }

    public RemoteAccessVpnVO(long accountId, long domainId, long networkId, Ip publicIp, String localIp, String ipRange,  String presharedKey) {
        this.accountId = accountId;
        this.serverAddress = publicIp;
        this.ipRange = ipRange;
        this.ipsecPresharedKey = presharedKey;
        this.localIp = localIp;
        this.domainId = domainId;
        this.networkId = networkId;
        this.state = State.Added;
    }
    
    @Override
    public State getState() {
        return state;
    }
    
    public void setState(State state) {
        this.state = state;
    }

    @Override
    public long getAccountId() {
        return accountId;
    }
    
	@Override
    public Ip getServerAddress() {
		return serverAddress;
	}

	@Override
    public String getIpRange() {
		return ipRange;
	}

    public void setIpRange(String ipRange) {
		this.ipRange = ipRange;
	}

	@Override
    public String getIpsecPresharedKey() {
		return ipsecPresharedKey;
	}

    public void setIpsecPresharedKey(String ipsecPresharedKey) {
		this.ipsecPresharedKey = ipsecPresharedKey;
	}

	@Override
    public String getLocalIp() {
		return localIp;
	}

	@Override
    public long getDomainId() {
		return domainId;
	}
	
	@Override
    public long getNetworkId() {
	    return networkId;
	}
}
