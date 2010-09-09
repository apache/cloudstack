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
package com.cloud.vm;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.network.Network.Mode;
import com.cloud.network.Network.TrafficType;

@Entity
@Table(name="nics")
public class NicVO implements Nic {
    protected NicVO() {
    }
    
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    long id;
    
    @Column(name="instance_id")
    long instanceId;
    
    @Column(name="type")
    @Enumerated(value=EnumType.STRING)
    TrafficType trafficType;
    
    @Column(name="ip4_address")
    String ip4Address;
    
    @Column(name="mac_address")
    String macAddress;
    
    @Column(name="netmask")
    String netMask;
    
    @Column(name="mode")
    @Enumerated(value=EnumType.STRING)
    Mode mode;
    
    @Column(name="network_profile_id")
    long networkProfileId;
    
    @Column(name="String")
    String vlan;
    
    @Column(name="state")
    @Enumerated(value=EnumType.STRING)
    State state;
    
    @Column(name="name")
    String conciergeName;
    
    @Column(name="reservation_id")
    String reservationId;
    
    @Column(name="device_id")
    int deviceId;

    public NicVO(String conciergeName, long instanceId, long profileId) {
        this.conciergeName = conciergeName;
        this.instanceId = instanceId;
        this.networkProfileId = profileId;
        this.state = State.Allocated;
    }
    
    @Override
    public String getIp4Address() {
        return ip4Address;
    }
    
    public void setIp4Address(String address) {
        ip4Address = address;
    }

    @Override
    public String getMacAddress() {
        return macAddress;
    }
    
    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    @Override
    public State getState() {
        return state;
    }
    
    public void setState(State state) {
        this.state = state;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public long getInstanceId() {
        return instanceId;
    }

    @Override
    public long getNetworkProfileId() {
        return networkProfileId;
    }
    
    @Override
    public long getDeviceId() {
        return deviceId;
    }
    
    public String getConciergeName() {
        return conciergeName;
    }
    
    public String getReservationId() {
        return reservationId;
    }
    
    public void setReservationId(String id) {
        this.reservationId = id;
    }
    
    public void setConciergeName(String conciergeName) {
        this.conciergeName = conciergeName;
    }
    
    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

    @Override
    public Mode getMode() {
        return mode;
    }
    
    public void setMode(Mode mode) {
        this.mode = mode;
    }
}
