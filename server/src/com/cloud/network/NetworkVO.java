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

import java.net.URI;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Transient;

import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.Mode;
import com.cloud.network.Networks.TrafficType;
import com.cloud.offering.NetworkOffering.GuestIpType;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.net.NetUtils;

/**
 * NetworkConfigurationVO contains information about a specific network.
 *
 */
@Entity
@Table(name="networks")
public class NetworkVO implements Network {
    @Id
    @TableGenerator(name="networks_sq", table="sequence", pkColumnName="name", valueColumnName="value", pkColumnValue="networks_seq", allocationSize=1)
    @Column(name="id")
    long id;
    
    @Column(name="mode")
    @Enumerated(value=EnumType.STRING)
    Mode mode;
    
    @Column(name="broadcast_domain_type")
    @Enumerated(value=EnumType.STRING)
    BroadcastDomainType broadcastDomainType;
    
    @Column(name="traffic_type")
    @Enumerated(value=EnumType.STRING)
    TrafficType trafficType;
    
    @Column(name="guest_type")
    GuestIpType guestType;
    
    @Column(name="broadcast_uri")
    URI broadcastUri; 
    
    @Column(name="gateway")
    String gateway;
    
    @Column(name="cidr")
    String cidr;
    
    @Column(name="network_offering_id")
    long networkOfferingId;
    
    @Column(name="data_center_id")
    long dataCenterId;
    
    @Column(name="related")
    long related;
    
    @Column(name="guru_name")
    String guruName;
    
    @Column(name="state")
    @Enumerated(value=EnumType.STRING)
    State state;
    
    @Column(name="dns1")
    String dns1;
    
    @Column(name="domain_id")
    long domainId;
    
    @Column(name="account_id")
    long accountId;
    
    @Column(name="set_fields")
    long setFields;
    
    @TableGenerator(name="mac_address_seq", table="op_networks", pkColumnName="id", valueColumnName="mac_address_seq", allocationSize=1)
    @Transient
    long macAddress = 1;
    
    @Column(name="guru_data")
    String guruData;
    
    @Column(name="dns2")
    String dns2;
    
    public NetworkVO() {
    }
    
    /**
     * Constructor to be used for the adapters because it only initializes what's needed.
     * @param trafficType
     * @param mode
     * @param broadcastDomainType
     * @param networkOfferingId
     * @param dataCenterId
     */
    public NetworkVO(TrafficType trafficType, GuestIpType guestType, Mode mode, BroadcastDomainType broadcastDomainType, long networkOfferingId, long dataCenterId) {
        this.trafficType = trafficType;
        this.mode = mode;
        this.broadcastDomainType = broadcastDomainType;
        this.networkOfferingId = networkOfferingId;
        this.dataCenterId = dataCenterId;
        this.state = State.Allocated;
        this.id = -1;
        this.guestType = guestType;
    }
    
    public NetworkVO(long id, Network that, long offeringId, long dataCenterId, String guruName, long domainId, long accountId, long related) {
        this(id, that.getTrafficType(), that.getGuestType(), that.getMode(), that.getBroadcastDomainType(), offeringId, dataCenterId, domainId, accountId, related);
        this.gateway = that.getGateway();
        this.dns1 = that.getDns1();
        this.dns2 = that.getDns2();
        this.cidr = that.getCidr();
        this.guruName = guruName;
        this.state = that.getState();
        if (state == null) {
            state = State.Allocated;
        }
    }

    /**
     * Constructor for the actual DAO object.
     * @param trafficType
     * @param mode
     * @param broadcastDomainType
     * @param networkOfferingId
     * @param dataCenterId
     * @param domainId
     * @param accountId
     */
    public NetworkVO(long id, TrafficType trafficType, GuestIpType guestType, Mode mode, BroadcastDomainType broadcastDomainType, long networkOfferingId, long dataCenterId, long domainId, long accountId, long related) {
        this(trafficType, guestType, mode, broadcastDomainType, networkOfferingId, dataCenterId);
        this.domainId = domainId;
        this.accountId = accountId;
        this.related = related;
        this.id = id;
    }
    
    @Override
    public State getState() {
        return state;
    }
    
    public void setState(State state) {
        this.state = state;
    }
    
    @Override
    public long getRelated() {
        return related;
    }

    @Override
    public long getId() {
        return id;
    }
    
    @Override
    public GuestIpType getGuestType() {
        return guestType;
    }

    @Override
    public Mode getMode() {
        return mode;
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
    
    public String getGuruData() {
        return guruData;
    }
    
    public void setGuruData(String guruData) {
        this.guruData = guruData;
    }
    
    public String getGuruName() {
        return guruName;
    }
    
    public void setGuruName(String guruName) {
        this.guruName = guruName;
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
    
    @Override
    public URI getBroadcastUri() {
        return broadcastUri;
    }

    public void setBroadcastUri(URI broadcastUri) {
        this.broadcastUri = broadcastUri;
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
    public String getDns1() {
        return dns1;
    }
    
    public void setDns1(String dns) {
        this.dns1 = dns;
    }
    
    @Override
    public String getDns2() {
        return dns2;
    }
    
    public void setDns2(String dns) {
        this.dns2 = dns;
    }
    
    
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof NetworkVO)) {
            return false;
        }
        NetworkVO that = (NetworkVO)obj;
        if (this.trafficType != that.trafficType) {
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
    
    public boolean isImplemented() {
        return broadcastUri != null && cidr != null && gateway != null && mode != null && broadcastDomainType != null;
    }
    
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("NtwkCfg[");
        buf.append(id).append("|").append(trafficType.toString()).append("|").append(networkOfferingId).append("]");
        return buf.toString();
    }
}
