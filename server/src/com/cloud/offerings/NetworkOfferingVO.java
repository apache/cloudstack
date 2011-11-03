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

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.network.Network;
import com.cloud.network.Networks.TrafficType;
import com.cloud.offering.NetworkOffering;
import com.cloud.utils.db.GenericDao;

@Entity
@Table(name="network_offerings")
public class NetworkOfferingVO implements NetworkOffering {
   
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    long id;
    
    @Column(name="name")
    String name;
    
    @Column(name="unique_name")
    private String uniqueName;
    
    @Column(name="display_text")
    String displayText;
    
    @Column(name="nw_rate")
    Integer rateMbps;
    
    @Column(name="mc_rate")
    Integer multicastRateMbps;
    
    @Column(name="concurrent_connections")
    Integer concurrentConnections;
    
    @Column(name="traffic_type")
    @Enumerated(value=EnumType.STRING)
    TrafficType trafficType;
    
    @Column(name="specify_vlan")
    boolean specifyVlan;
    
    @Column(name="system_only")
    boolean systemOnly;
    
    @Column(name="service_offering_id")
    Long serviceOfferingId;
    
    @Column(name="tags", length=4096)
    String tags;
   
    @Column(name="default")
    boolean isDefault;
    
    @Column(name="availability")
    @Enumerated(value=EnumType.STRING)
    Availability availability;
    
    @Column(name="state")
    @Enumerated(value=EnumType.STRING)
    State state = State.Disabled;
    
    @Column(name=GenericDao.REMOVED_COLUMN)
    Date removed;
    
    @Column(name=GenericDao.CREATED_COLUMN)
    Date created;

    @Column(name="guest_type")
    @Enumerated(value=EnumType.STRING)
    Network.GuestType guestType;
    
    @Override
    public String getDisplayText() {
        return displayText;
    }

    @Override
    public long getId() {
        return id;
    }
    
    @Override
    public TrafficType getTrafficType() {
        return trafficType;
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
    
    public Date getCreated() {
        return created;
    }
    
    @Override
    public boolean isSystemOnly() {
        return systemOnly;
    }
    
    public Date getRemoved() {
        return removed;
    }
    
    @Override
    public Integer getConcurrentConnections() {
        return concurrentConnections;
    }
    
    public String getTags() {
        return tags;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    public void setRateMbps(Integer rateMbps) {
        this.rateMbps = rateMbps;
    }

    public void setMulticastRateMbps(Integer multicastRateMbps) {
        this.multicastRateMbps = multicastRateMbps;
    }
    
    @Override
    public boolean isDefault() {
        return isDefault;
    }
    
    @Override
    public boolean getSpecifyVlan() {
        return specifyVlan;
    }
    
    @Override
    public Availability getAvailability() {
        return availability;
    }

    public void setAvailability(Availability availability) {
        this.availability = availability;
    }
    
    @Override
    public String getUniqueName() {
        return uniqueName;
    }
    
    @Override
    public void setState(State state) {
        this.state = state;
    }   

    @Override
    public State getState() {
        return state;
    }
    
    @Override
    public Network.GuestType getGuestType() {
        return guestType;
    }

    public void setServiceOfferingId(Long serviceOfferingId) {
        this.serviceOfferingId = serviceOfferingId;
    }

    @Override
    public Long getServiceOfferingId() {
        return serviceOfferingId;
    }

    public NetworkOfferingVO(String name, String displayText, TrafficType trafficType, boolean systemOnly, boolean specifyVlan, Integer rateMbps, Integer multicastRateMbps, Integer concurrentConnections, boolean isDefault, Availability availability, String tags, Network.GuestType guestType) {
        this.name = name;
        this.displayText = displayText;
        this.rateMbps = rateMbps;
        this.multicastRateMbps = multicastRateMbps;
        this.concurrentConnections = concurrentConnections;
        this.trafficType = trafficType;
        this.systemOnly = systemOnly;
        this.specifyVlan = specifyVlan;
        this.isDefault = isDefault;
        this.availability = availability;
        this.uniqueName = name;
        this.tags = tags;
        this.guestType = guestType;
    }
    
    public NetworkOfferingVO() {
    }
    
    /**
     * Network Offering for all system vms.
     * @param name
     * @param trafficType
     */
    public NetworkOfferingVO(String name, TrafficType trafficType) {
        this(name, "System Offering for " + name, trafficType, true, false, 0, 0, null, true, Availability.Required, null, null);
        this.state = State.Enabled;
    }
    
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("[Network Offering [");
        return buf.append(id).append("-").append(trafficType).append("-").append(name).append("]").toString();
    }
}
