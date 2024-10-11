//
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
//

package com.cloud.utils.crypt;

/**
 * This is a copy of ./api/src/main/java/com/cloud/agent/api/to/deployasis/OVFPropertyTO.java
 */
public class OVFPropertyTO {

    private String key;
    private String type;
    private String value;
    private String qualifiers;
    private Boolean userConfigurable;
    private String label;
    private String description;
    private Boolean password;
    private int index;
    private String category;

    public OVFPropertyTO() {
    }

    public OVFPropertyTO(String key, String type, String value, String qualifiers, boolean userConfigurable,
                       String label, String description, boolean password, int index, String category) {
        this.key = key;
        this.type = type;
        this.value = value;
        this.qualifiers = qualifiers;
        this.userConfigurable = userConfigurable;
        this.label = label;
        this.description = description;
        this.password = password;
        this.index = index;
        this.category = category;
    }

    public Long getTemplateId() {
        return null;
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

    public String getCategory() {
        return category;
    }

    public int getIndex() {
        return index;
    }
}
