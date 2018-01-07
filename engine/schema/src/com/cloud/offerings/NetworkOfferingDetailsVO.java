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
import com.cloud.offering.NetworkOffering.Detail;
import org.apache.cloudstack.api.ResourceDetail;

@Entity
@Table(name = "network_offering_details")
public class NetworkOfferingDetailsVO implements ResourceDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "network_offering_id")
    private long resourceId;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "name")
    private NetworkOffering.Detail name;

    @Column(name = "value", length = 1024)
    private String value;

    public NetworkOfferingDetailsVO() {
    }

    public NetworkOfferingDetailsVO(long resourceId, Detail detailName, String value) {
        this.resourceId = resourceId;
        this.name = detailName;
        this.value = value;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public long getResourceId() {
        return resourceId;
    }

    public void setResourceId(long resourceId) {
        this.resourceId = resourceId;
    }

    public String getName() {
        return name.name();
    }

    public NetworkOffering.Detail getDetailName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean isDisplay() {
        return false;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setName(NetworkOffering.Detail name) {
        this.name = name;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
