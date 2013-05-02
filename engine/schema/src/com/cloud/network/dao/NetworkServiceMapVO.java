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
package com.cloud.network.dao;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.network.Network;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.utils.db.GenericDao;
import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name="ntwk_service_map")
public class NetworkServiceMapVO implements InternalIdentity {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    long id;
    
    @Column(name="network_id")
    long networkId;
    
    @Column(name="service")
    String service;
    
    @Column(name="provider")
    String provider;
    
    @Column(name=GenericDao.CREATED_COLUMN)
    Date created;

    public long getId() {
        return id;
    }

    public long getNetworkId() {
        return networkId;
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
    
    public NetworkServiceMapVO() {
    }
    
    public NetworkServiceMapVO(long networkId, Service service, Provider provider) {
        this.networkId = networkId;
        this.service = service.getName();
        this.provider = provider.getName();
    }
    
    public String toString() {
        StringBuilder buf = new StringBuilder("[Network Service[");
        return buf.append(networkId).append("-").append(service).append("-").append(provider).append("]").toString();
    }
}





