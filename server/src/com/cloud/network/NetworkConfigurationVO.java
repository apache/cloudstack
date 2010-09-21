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
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.network.Network.BroadcastDomainType;
import com.cloud.network.Network.Mode;
import com.cloud.network.Network.TrafficType;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.net.NetUtils;

/**
 * NetworkProfileVO contains information about a specific network.
 *
 */
@Entity
@Table(name="network_configurations")
public class NetworkConfigurationVO implements NetworkConfiguration {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    Long id;
    
    @Column(name="mode")
    @Enumerated(value=EnumType.STRING)
    Mode mode;
    
    @Column(name="broadcast_domain_type")
    @Enumerated(value=EnumType.STRING)
    BroadcastDomainType broadcastDomainType;
    
    @Column(name="traffic_type")
    @Enumerated(value=EnumType.STRING)
    TrafficType trafficType;
    
    @Column(name="vlan_id")
    Long vlanId; 
    
    @Column(name="gateway")
    String gateway;
    
    @Column(name="cidr")
    String cidr;
    
    @Column(name="network_offering_id")
    long networkOfferingId;
    
    @Column(name="data_center_id")
    long dataCenterId;
    
    @Column(name="handler_name")
    String handlerName;
    
    @Column(name="state")
    @Enumerated(value=EnumType.STRING)
    State state;
    
    public NetworkConfigurationVO() {
    }
    
    public NetworkConfigurationVO(NetworkConfiguration that, long offeringId, long dataCenterId, String handlerName) {
        this(that.getTrafficType(), that.getMode(), that.getBroadcastDomainType(), offeringId, dataCenterId);
        this.handlerName = handlerName;
        this.state = that.getState();
    }
    
    public NetworkConfigurationVO(TrafficType trafficType, Mode mode, BroadcastDomainType broadcastDomainType, long networkOfferingId, long dataCenterId) {
        this.trafficType = trafficType;
        this.mode = mode;
        this.broadcastDomainType = broadcastDomainType;
        this.networkOfferingId = networkOfferingId;
        this.dataCenterId = dataCenterId;
        this.state = State.Allocated;
    }
    
    @Override
    public State getState() {
        return state;
    }
    
    public void setState(State state) {
        this.state = state;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public Mode getMode() {
        return mode;
    }
    
    @Override
    public long getNetworkOfferingId() {
        return networkOfferingId;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    @Override
    public BroadcastDomainType getBroadcastDomainType() {
        return broadcastDomainType;
    }
    
    public String getHandlerName() {
        return handlerName;
    }
    
    public void setHandlerName(String handlerName) {
        this.handlerName = handlerName;
    }

    public void setBroadcastDomainType(BroadcastDomainType broadcastDomainType) {
        this.broadcastDomainType = broadcastDomainType;
    }

    @Override
    public TrafficType getTrafficType() {
        return trafficType;
    }

    public void setTrafficType(TrafficType trafficType) {
        this.trafficType = trafficType;
    }

    @Override
    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    @Override
    public String getCidr() {
        return cidr;
    }

    public void setCidr(String cidr) {
        this.cidr = cidr;
    }
    
    public Long getVlanId() {
        return vlanId;
    }

    public void setVlanId(Long vlanId) {
        this.vlanId = vlanId;
    }
    
    @Override
    public int hashCode() {
        return NumbersUtil.hash(id);
    }
    
    @Override
    public long getDataCenterId() {
        return dataCenterId;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof NetworkConfigurationVO)) {
            return false;
        }
        NetworkConfigurationVO that = (NetworkConfigurationVO)obj;
        if (this.trafficType != that.trafficType) {
            return false;
        }
        
        if (this.vlanId != null && that.vlanId != null && this.vlanId.longValue() != that.vlanId.longValue()) {
            return false;
        }
        
        if (this.vlanId != that.vlanId) {
            return false;
        }
        
        if ((this.cidr == null && that.cidr != null) || (this.cidr != null && that.cidr == null)) {
            return false;
        }
        
        if (this.cidr == null && that.cidr == null) {
            return true;
        }
        
        return NetUtils.isNetworkAWithinNetworkB(this.cidr, that.cidr);
    }
}
