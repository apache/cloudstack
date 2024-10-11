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
package com.cloud.storage;

import org.apache.cloudstack.api.InternalIdentity;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "vnf_template_nics")
public class VnfTemplateNicVO implements InternalIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "template_id")
    private long templateId;

    @Column(name = "device_id")
    private long deviceId;

    @Column(name = "device_name")
    private String deviceName;

    @Column(name = "required")
    private boolean required = true;

    @Column(name = "management")
    private boolean management = true;

    @Column(name = "description")
    private String description;

    public VnfTemplateNicVO() {
    }

    public VnfTemplateNicVO(long templateId, long deviceId, String deviceName, boolean required, boolean management, String description) {
        this.templateId = templateId;
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.required = required;
        this.management = management;
        this.description = description;
    }

    @Override
    public String toString() {
        return String.format("Template %s", ReflectionToStringBuilderUtils.reflectOnlySelectedFields(this, "id", "templateId", "deviceId", "required"));
    }

    @Override
    public long getId() {
        return id;
    }

    public long getTemplateId() {
        return templateId;
    }

    public long getDeviceId() {
        return deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isManagement() {
        return management;
    }

    public String getDescription() {
        return description;
    }
}
