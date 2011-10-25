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
import java.util.List;

import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.Mode;
import com.cloud.network.Networks.TrafficType;


public class NetworkProfile implements Network {
    private long id;
    private long dataCenterId;
    private long ownerId;
    private long domainId;
    private String dns1;
    private String dns2;
    private URI broadcastUri;
    private State state;
    private String name;
    private Mode mode;
    private BroadcastDomainType broadcastDomainType;
    private TrafficType trafficType;
    private String gateway;
    private String cidr;
    private long networkOfferingId;
    private long related;
    private String displayText;
    private String reservationId;
    private boolean isDefault;
    private String networkDomain;
    private boolean isSecurityGroupEnabled;
    private List<String> tags;
    private Network.Type type;
    private boolean isShared;
    private Long physicalNetworkId;

    public NetworkProfile(Network network) {
        this.id = network.getId();
        this.broadcastUri = network.getBroadcastUri();
        this.dataCenterId = network.getDataCenterId();
        this.ownerId = network.getAccountId();
        this.state = network.getState();
        this.name = network.getName();
        this.mode = network.getMode();
        this.broadcastDomainType = network.getBroadcastDomainType();
        this.trafficType = network.getTrafficType();
        this.gateway = network.getGateway();
        this.cidr = network.getCidr();
        this.networkOfferingId = network.getNetworkOfferingId();
        this.related = network.getRelated();
        this.displayText = network.getDisplayText();
        this.reservationId = network.getReservationId();
        this.isDefault = network.isDefault();
        this.networkDomain = network.getNetworkDomain();
        this.domainId = network.getDomainId();
        this.isSecurityGroupEnabled = network.isSecurityGroupEnabled();
        this.type = network.getType();
        this.isShared = network.getIsShared();
        this.physicalNetworkId = network.getPhysicalNetworkId();
    }

    @Override
    public List<String> getTags() {
        return tags;
    }

    public String getDns1() {
        return dns1;
    }

    public String getDns2() {
        return dns2;
    }

    public void setDns1(String dns1) {
        this.dns1 = dns1;
    }

    public void setDns2(String dns2) {
        this.dns2 = dns2;
    }

    public void setBroadcastUri(URI broadcastUri) {
        this.broadcastUri = broadcastUri;
    }

    @Override
    public URI getBroadcastUri() {
        return broadcastUri;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public long getDataCenterId() {
        return dataCenterId;
    }

    @Override
    public long getAccountId() {
        return ownerId;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Mode getMode() {
        return mode;
    }

    @Override
    public BroadcastDomainType getBroadcastDomainType() {
        return broadcastDomainType;
    }

    @Override
    public TrafficType getTrafficType() {
        return trafficType;
    }

    @Override
    public String getGateway() {
        return gateway;
    }

    @Override
    public String getCidr() {
        return cidr;
    }

    @Override
    public long getNetworkOfferingId() {
        return networkOfferingId;
    }

    @Override
    public long getRelated() {
        return related;
    }

    @Override
    public String getDisplayText() {
        return displayText;
    }
    
    @Override
    public String getReservationId() {
        return reservationId;
    }

    @Override
    public boolean isDefault() {
        return isDefault;
    }

    @Override
    public String getNetworkDomain() {
        return networkDomain;
    }

    @Override
    public long getDomainId() {
        return domainId;
    }

    @Override
    public boolean isSecurityGroupEnabled() {
        return isSecurityGroupEnabled;
    }
    
    @Override
    public Network.Type getType(){
        return type;
    }

    @Override
    public boolean getIsShared() {
        return isShared;
    }
    
    @Override
    public Long getPhysicalNetworkId() {
        return physicalNetworkId;
    }
}
