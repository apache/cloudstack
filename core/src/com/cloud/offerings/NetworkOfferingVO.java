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
package com.cloud.offerings;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.offering.NetworkOffering;

@Entity
@Table(name="network_offerings")
public class NetworkOfferingVO implements NetworkOffering {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    long id;
    
    @Column(name="name")
    String name;
    
    @Column(name="display_text")
    String displayText;
    
    @Column(name="rate")
    Integer rateMbps;
    
    @Column(name="multicast_rate")
    Integer multicastRateMbps;
    
    @Column(name="concurrent_connections")
    Integer concurrentConnections;
    
    @Column(name="type")
    @Enumerated(value=EnumType.STRING)
    GuestIpType guestIpType;

    @Override
    public String getDisplayText() {
        return displayText;
    }

    @Override
    public GuestIpType getGuestIpType() {
        return guestIpType;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public Integer getMulticastRateMbps() {
        return multicastRateMbps;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Integer getRateMbps() {
        return rateMbps;
    }
    
    public NetworkOfferingVO() {
    }
    
    public NetworkOfferingVO(String name, String displayText, GuestIpType type, Integer rateMbps, Integer multicastRateMbps, Integer concurrentConnections) {
        this.name = name;
        this.displayText = displayText;
        this.guestIpType = type;
        this.rateMbps = rateMbps;
        this.multicastRateMbps = multicastRateMbps;
        this.concurrentConnections = concurrentConnections;
    }

    @Override
    public Integer getConcurrentConnections() {
        return concurrentConnections;
    }

}
