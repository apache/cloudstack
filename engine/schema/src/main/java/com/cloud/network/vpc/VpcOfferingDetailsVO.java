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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.api.ResourceDetail;

@Entity
@Table(name = "vpc_offering_details")
public class VpcOfferingDetailsVO implements ResourceDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "offering_id")
    private long resourceId;

    @Column(name = "name")
    private String name;

    @Column(name = "value")
    private String value;

    @Column(name = "display")
    private boolean display = true;

    protected VpcOfferingDetailsVO() {
    }

    public VpcOfferingDetailsVO(long vpcOfferingId, String name, String value, boolean display) {
        this.resourceId = vpcOfferingId;
        this.name = name;
        this.value = value;
        this.display = display;
    }

    @Override
    public long getResourceId() {
        return resourceId;
    }

    public void setResourceId(long vpcOfferingId) {
        this.resourceId = vpcOfferingId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public boolean isDisplay() {
        return display;
    }
}
