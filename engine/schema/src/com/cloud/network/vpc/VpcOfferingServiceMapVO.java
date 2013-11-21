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
package com.cloud.network.vpc;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.api.InternalIdentity;

import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "vpc_offering_service_map")
public class VpcOfferingServiceMapVO implements InternalIdentity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    long id;

    @Column(name = "vpc_offering_id")
    long vpcOfferingId;

    @Column(name = "service")
    String service;

    @Column(name = "provider")
    String provider;

    @Column(name = GenericDao.CREATED_COLUMN)
    Date created;

    @Override
    public long getId() {
        return id;
    }

    public long getVpcOfferingId() {
        return vpcOfferingId;
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

    public VpcOfferingServiceMapVO() {
    }

    public VpcOfferingServiceMapVO(long vpcOfferingId, Service service, Provider provider) {
        this.vpcOfferingId = vpcOfferingId;
        this.service = service.getName();
        if (provider != null) {
            this.provider = provider.getName();
        }
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("[VPC Offering Service[");
        return buf.append(vpcOfferingId).append("-").append(service).append("-").append(provider).append("]").toString();
    }
}
