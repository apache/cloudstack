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

import com.cloud.network.Networks.TrafficType;
import com.cloud.offering.NetworkOffering;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.utils.db.GenericDao;

@Entity
@Table(name="network_offerings")
public class NetworkOfferingVO implements NetworkOffering {
    public final static String SystemVmPublicNetwork = "System-Public-Network";
    public final static String SystemVmControlNetwork = "System-Control-Network";
    public final static String SystemVmManagementNetwork = "System-Management-Network";
    public final static String SystemVmStorageNetwork = "System-Storage-Network";
    
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    long id;
    
    @Column(name="name")
    String name;
    
    @Column(name="display_text")
    String displayText;
    
    @Column(name="nw_rate")
    Integer rateMbps;
    
    @Column(name="mc_rate")
    Integer multicastRateMbps;
    
    @Column(name="concurrent_connections")
    Integer concurrentConnections;
    
    @Column(name="type")
    @Enumerated(value=EnumType.STRING)
    GuestIpType guestIpType;
    
    @Column(name="traffic_type")
    @Enumerated(value=EnumType.STRING)
    TrafficType trafficType;
    
    @Column(name="specify_vlan")
    boolean specifyVlan;
    
    @Column(name="system_only")
    boolean systemOnly;
    
    @Column(name="service_offering_id")
    Long serviceOfferingId;
    
    @Column(name="tags")
    String tags;
    
    @Column(name="shared")
    boolean isShared;
    
    @Column(name="default")
    boolean isDefault;
    
    @Column(name=GenericDao.REMOVED_COLUMN)
    Date removed;
    
    @Column(name=GenericDao.CREATED_COLUMN)
    Date created;

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
    
    public void setTags(String tags) {
        this.tags = tags;
    }
    
    public NetworkOfferingVO() {
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

    public void setConcurrentConnections(Integer concurrentConnections) {
        this.concurrentConnections = concurrentConnections;
    }

    public void setGuestIpType(GuestIpType guestIpType) {
        this.guestIpType = guestIpType;
    }

    public void setTrafficType(TrafficType trafficType) {
        this.trafficType = trafficType;
    }

    public void setSystemOnly(boolean systemOnly) {
        this.systemOnly = systemOnly;
    }


    public void setRemoved(Date removed) {
        this.removed = removed;
    }
    
    public Long getServiceOfferingId() {
        return serviceOfferingId;
    }
    
    public void setServiceOfferingId(long serviceOfferingId) {
        this.serviceOfferingId = serviceOfferingId;
    }
    
    @Override
    public boolean isShared() {
        return isShared;
    }

    public void setShared(boolean isShared) {
        this.isShared = isShared;
    }
    
    @Override
    public boolean isDefault() {
        return isDefault;
    }
    
    @Override
    public boolean getSpecifyVlan() {
        return specifyVlan;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public NetworkOfferingVO(String name, String displayText, TrafficType trafficType, GuestIpType type, boolean systemOnly, boolean specifyVlan, Integer rateMbps, Integer multicastRateMbps, Integer concurrentConnections, boolean isShared, boolean isDefault) {
        this.name = name;
        this.displayText = displayText;
        this.guestIpType = type;
        this.rateMbps = rateMbps;
        this.multicastRateMbps = multicastRateMbps;
        this.concurrentConnections = concurrentConnections;
        this.trafficType = trafficType;
        this.systemOnly = systemOnly;
        this.specifyVlan = specifyVlan;
        this.isDefault = isDefault;
        this.isShared = isShared;
    }
    
    public NetworkOfferingVO(ServiceOfferingVO offering) {
        this("Network Offering for " + offering.getName(), "Network Offering for " + offering.getDisplayText(), TrafficType.Guest, offering.getGuestIpType(), false, false, offering.getRateMbps(), offering.getMulticastRateMbps(), null, false, false);
        this.serviceOfferingId = offering.getId();
    }
    

    /**
     * Network Offering for all system vms.
     * @param name
     * @param trafficType
     * @param type
     */
    public NetworkOfferingVO(String name, TrafficType trafficType, GuestIpType type) {
        this(name, "System Offering for " + name, trafficType, type, true, false, null, null, null, false, false);
    }
    
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("[Network Offering [");
        return buf.append(id).append("-").append(trafficType).append("-").append(name).append("-").append(guestIpType).append("]").toString();
    }
}
