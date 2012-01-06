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
package com.cloud.network.addr;

import java.util.Date;

import com.cloud.dc.VlanVO;
import com.cloud.network.IPAddressVO;
import com.cloud.network.PublicIpAddress;
import com.cloud.utils.net.Ip;
import com.cloud.utils.net.NetUtils;

/**
 * PublicIp is a combo object of IPAddressVO and VLAN information.
 */
public class PublicIp implements PublicIpAddress {
    IPAddressVO _addr;
    VlanVO _vlan;
    String macAddress;

    public PublicIp(IPAddressVO addr, VlanVO vlan, long macAddress) {
        _addr = addr;
        _vlan = vlan;
        this.macAddress = NetUtils.long2Mac(macAddress);
    }
    
    @Override
    public Ip getAddress() {
        return _addr.getAddress();
    }
    
    @Override
    public String getNetmask() {
        return _vlan.getVlanNetmask();
    }
    
    @Override
    public String getGateway() {
        return _vlan.getVlanGateway();
    }
    
    @Override
    public String getVlanTag() {
        return _vlan.getVlanTag();
    }
    
    @Override
    public long getDataCenterId() {
        return _addr.getDataCenterId();
    }
    
    @Override
    public boolean readyToUse() {
        return _addr.getAllocatedTime() != null && _addr.getState() == State.Allocated;
    }
    
    @Override
    public boolean isSourceNat() {
        return _addr.isSourceNat();
    }
    
    @Override
    public boolean isOneToOneNat() {
        return _addr.isOneToOneNat();
    }
    
    @Override
    public Long getAssociatedWithVmId() {
        return _addr.getAssociatedWithVmId();
    }
    
    @Override
    public Date getAllocatedTime() {
        return _addr.getAllocatedTime();
    }

    @Override
    public long getAccountId() {
        return _addr.getAccountId();
    }

    @Override
    public long getDomainId() {
        return _addr.getDomainId();
    }

    @Override
    public long getVlanId() {
        return _vlan.getId();
    }

    @Override
    public State getState() {
        return _addr.getState();
    }
    
    public IPAddressVO ip() {
        return _addr;
    }
    
    public VlanVO vlan() {
        return _vlan;
    }
    
    @Override
    public String getMacAddress() {
        return macAddress;
    }
    
    @Override
    public Long getAssociatedWithNetworkId() {
        return _addr.getAssociatedWithNetworkId();
    }
    
    @Override
    public Long getNetworkId() {
        return _vlan.getNetworkId();
    }

    @Override
    public String getVlanGateway() {
        return _vlan.getVlanGateway();
    }

    @Override
    public String getVlanNetmask() {
        return _vlan.getVlanNetmask();
    }

    @Override
    public String getIpRange() {
        return _vlan.getIpRange();
    }

    @Override
    public VlanType getVlanType() {
        return _vlan.getVlanType();
    }
    
    @Override
    public long getId() {
        return _addr.getId();
    }
    
    @Override
    public String toString() {
        return _addr.getAddress().toString();
    }

    @Override
    public Long getPhysicalNetworkId() {
        return _vlan.getPhysicalNetworkId();
    }

	@Override
	public void setState(State state) {
		_addr.setState(state);
	}

	@Override
	public Long getAllocatedToAccountId() {
		return _addr.getAllocatedToAccountId();
	}

	@Override
	public Long getAllocatedInDomainId() {
		return _addr.getAllocatedInDomainId();
	}
}
