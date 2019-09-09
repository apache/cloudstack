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

import com.cloud.agent.api.storage.OVFProperty;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "template_ovf_properties")
public class TemplateOVFPropertyVO implements OVFProperty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "key")
    private String key;

    @Column(name = "type")
    private String type;

    @Column(name = "value")
    private String value;

    @Column(name = "qualifiers")
    private String qualifiers;

    @Column(name = "password")
    private Boolean password;

    @Column(name = "user_configurable")
    private Boolean userConfigurable;

    @Column(name = "label")
    private String label;

    @Column(name = "description")
    private String description;

    public TemplateOVFPropertyVO() {
    }

    public TemplateOVFPropertyVO(Long templateId, String key, String type, String value, String qualifiers,
                                 Boolean userConfigurable, String label, String description, Boolean password) {
        this.templateId = templateId;
        this.key = key;
        this.type = type;
        this.value = value;
        this.qualifiers = qualifiers;
        this.userConfigurable = userConfigurable;
        this.label = label;
        this.description = description;
        this.password = password;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getQualifiers() {
        return qualifiers;
    }

    public void setQualifiers(String qualifiers) {
        this.qualifiers = qualifiers;
    }

    @Override
    public Boolean isUserConfigurable() {
        return userConfigurable;
    }

    public void setUserConfigurable(Boolean userConfigurable) {
        this.userConfigurable = userConfigurable;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean isPassword() {
        return password;
    }

    public void setPassword(Boolean password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return String.format("PROP - templateId=%s> key=%s value=%s type=%s qual=%s conf=%s label=%s desc=%s password=%s",
                templateId, key, value, type, qualifiers, userConfigurable, label, description, password);
    }
}
