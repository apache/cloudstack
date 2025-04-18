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

package com.cloud.hypervisor.external.provisioner.vo;

import org.apache.cloudstack.api.ResourceDetail;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "extension_details")
public class ExternalOrchestratorDetailVO implements ResourceDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "extension_id", nullable = false)
    private long resourceId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "value", nullable = false, length = 255)
    private String value;

    @Column(name = "display")
    private boolean display = true;

    public ExternalOrchestratorDetailVO() {
    }

    public ExternalOrchestratorDetailVO(long resourceId, String name, String value) {
        this.resourceId = resourceId;
        this.name = name;
        this.value = value;
    }

    public ExternalOrchestratorDetailVO(long id, String name, String value, boolean display) {
        this.resourceId = id;
        this.name = name;
        this.value = value;
        this.display = display;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public boolean isDisplay() {
        return display;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public long getResourceId() {
        return resourceId;
    }
}
