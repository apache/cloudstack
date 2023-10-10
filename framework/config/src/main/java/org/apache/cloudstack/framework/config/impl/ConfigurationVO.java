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
package org.apache.cloudstack.framework.config.impl;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.cloudstack.config.Configuration;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.commons.lang3.StringUtils;

import com.cloud.utils.crypt.DBEncryptionUtil;

@Entity
@Table(name = "configuration")
public class ConfigurationVO implements Configuration {
    @Column(name = "instance")
    private String instance;

    @Column(name = "component")
    private String component;

    @Id
    @Column(name = "name")
    private String name;

    @Column(name = "value", length = 8191)
    private String value;

    @Column(name = "default_value", length = 8191)
    private String defaultValue;

    @Column(name = "description", length = 1024)
    private String description;

    @Column(name = "category")
    private String category;

    @Column(name = "is_dynamic")
    private boolean dynamic;

    @Column(name = "scope")
    private String scope;

    @Column(name = "updated")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date updated;

    @Column(name = "group_id")
    private Long groupId = 1L;

    @Column(name = "subgroup_id")
    private Long subGroupId = 1L;

    @Column(name = "parent")
    private String parent;

    @Column(name = "display_text", length = 255)
    private String displayText;

    @Column(name = "kind")
    private String kind;

    @Column(name = "options")
    private String options;

    protected ConfigurationVO() {
    }

    public ConfigurationVO(String category, String instance, String component, String name, String value, String description) {
        this(category, instance, component, name, value, description, null, null);
    }

    public ConfigurationVO(String category, String instance, String component, String name, String value, String description, String displayText, String parentConfigName) {
        this(category, instance, component, name, value, description, displayText, parentConfigName, null, null);
    }

    public ConfigurationVO(String category, String instance, String component, String name, String value, String description, String displayText, String parentConfigName, Long groupId, Long subGroupId) {
        this.category = category;
        this.instance = instance;
        this.component = component;
        this.name = name;
        this.description = description;
        this.parent = parentConfigName;
        setValue(value);
        setDisplayText(displayText);
        setGroupId(groupId);
        setSubGroupId(subGroupId);
    }

    public ConfigurationVO(String component, ConfigKey<?> key) {
        this(key.category(), "DEFAULT", component, key.key(), key.defaultValue(), key.description(), key.displayText(), key.parent());
        defaultValue = key.defaultValue();
        dynamic = key.isDynamic();
        scope = key.scope() != null ? key.scope().toString() : null;
    }

    @Override
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public String getInstance() {
        return instance;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    @Override
    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
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
        if(isEncrypted()) {
            return DBEncryptionUtil.decrypt(value);
        } else {
            return value;
        }
    }

    public void setValue(String value) {
        if(isEncrypted()) {
            this.value = DBEncryptionUtil.encrypt(value);
        } else {
            this.value = value;
        }
    }

    @Override
    public boolean isEncrypted() {
        return StringUtils.equalsAny(getCategory(), "Hidden", "Secure");
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getScope() {
        return scope;
    }

    @Override
    public boolean isDynamic() {
        return dynamic;
    }

    public void setDynamic(boolean dynamic) {
        this.dynamic = dynamic;
    }

    @Override
    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    @Override
    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    @Override
    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        if (groupId != null) {
            this.groupId = groupId;
        } else {
            this.groupId = 1L;
        }
    }

    @Override
    public Long getSubGroupId() {
        return subGroupId;
    }

    public void setSubGroupId(Long subGroupId) {
        if (subGroupId != null) {
            this.subGroupId = subGroupId;
        } else {
            this.subGroupId = 1L;
        }
    }

    @Override
    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    @Override
    public String getDisplayText() {
        if (StringUtils.isNotBlank(displayText)) {
            return displayText;
        }

        String displayText = "";
        String name = getName();
        if (StringUtils.isNotBlank(name)) {
            name = name.replace(".", " ");
            displayText = name.substring(0, 1).toUpperCase() + name.substring(1);
        }
        return displayText;
    }

    public void setDisplayText(String displayText) {
        if (StringUtils.isBlank(displayText)) {
            String name = getName();
            if (StringUtils.isNotBlank(name)) {
                name = name.replace(".", " ");
                displayText = name.substring(0, 1).toUpperCase() + name.substring(1);
            }
        }

        this.displayText = displayText;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getOptions() {
        return options;
    }

    public void setOptions(String options) {
        this.options = options;
    }

}
