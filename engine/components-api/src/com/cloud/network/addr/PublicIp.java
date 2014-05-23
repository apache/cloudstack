// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.network.addr;

import java.util.Date;

import com.cloud.dc.VlanVO;
import com.cloud.network.IpAddress;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.utils.net.Ip;
import com.cloud.utils.net.NetUtils;

/**
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

    public static PublicIp createFromAddrAndVlan(IPAddressVO addr, VlanVO vlan) {
        return new PublicIp(addr, vlan, NetUtils.createSequenceBasedMacAddress(addr.getMacAddress()));
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
    public String getUuid() {
        return _addr.getUuid();
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

    @Override
    public boolean getSystem() {
        return _addr.getSystem();
    }

    @Override
    public Long getVpcId() {
       return _addr.getVpcId();
    }

    @Override
    public String getIp6Gateway() {
        return _vlan.getIp6Gateway();
    }

    @Override
    public String getIp6Cidr() {
        return _vlan.getIp6Cidr();
    }

    @Override
    public String getIp6Range() {
        return _vlan.getIp6Range();
    }

    @Override
    public String getVmIp() {
        return _addr.getVmIp();
    }

    @Override
    public boolean isPortable() {
        return _addr.isPortable();
    }

    public void setPortable(boolean portable) {
        _addr.setPortable(portable);
    }

    public Long getIpMacAddress() {
        return  _addr.getMacAddress();
    }

    @Override
    public boolean isDisplay() {
        return _addr.isDisplay();
    }

    @Override
    public Date getRemoved() {
        return _addr.getRemoved();
    }

    @Override
    public Date getCreated() {
        return _addr.getCreated();
    }

    @Override
    public Class<?> getEntityType() {
        return IpAddress.class;
    }
}
