/**
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
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
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.utils.db.GenericDao;

@Entity
@Table(name="ntwk_offering_service_map")
public class NetworkOfferingServiceMapVO {
   
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    long id;
    
    @Column(name="network_offering_id")
    long networkOfferingId;
    
    @Column(name="service")
    String service;
    
    @Column(name="provider")
    String provider;
    
    @Column(name=GenericDao.CREATED_COLUMN)
    Date created;

    public long getId() {
        return id;
    }

    public long getNetworkOfferingId() {
        return networkOfferingId;
    }

    public String getService() {
        return service;
    }

    public String getProvider() {
        return provider;
    }

    public Date getCreated() {
        return created;
    }
    
    public NetworkOfferingServiceMapVO() {
    }
    
    public NetworkOfferingServiceMapVO(long networkOfferingId, Service service, Provider provider) {
        this.networkOfferingId = networkOfferingId;
        this.service = service.getName();
        if (provider != null) {
            this.provider = provider.getName();
        }
    }
    
    public String toString() {
        StringBuilder buf = new StringBuilder("[Network Offering Service[");
        return buf.append(networkOfferingId).append("-").append(service).append("-").append(provider).append("]").toString();
    }
}





