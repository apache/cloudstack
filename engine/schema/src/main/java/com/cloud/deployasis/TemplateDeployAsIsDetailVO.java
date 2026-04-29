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
package com.cloud.deployasis;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

import org.apache.cloudstack.api.ResourceDetail;

@Entity
@Table(name = "template_deploy_as_is_details")
public class TemplateDeployAsIsDetailVO implements ResourceDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "template_id")
    private long resourceId;

    @Column(name = "name")
    private String name;

    @Lob
    @Column(name = "value", length = 65535)
    private String value;

    public TemplateDeployAsIsDetailVO() {
    }

    public TemplateDeployAsIsDetailVO(long templateId, String name, String value) {
        this.resourceId = templateId;
        this.name = name;
        this.value = value;
    }

    @Override
    public long getId() {
        return id;
    }

    public long getResourceId() {
        return resourceId;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean isDisplay() {
        return true;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setResourceId(long resourceId) {
        this.resourceId = resourceId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
