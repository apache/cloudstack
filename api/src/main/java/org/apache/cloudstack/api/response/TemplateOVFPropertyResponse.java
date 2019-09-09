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
package org.apache.cloudstack.api.response;

import com.cloud.agent.api.storage.OVFProperty;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

@EntityReference(value = OVFProperty.class)
public class TemplateOVFPropertyResponse extends BaseResponse {

    @SerializedName(ApiConstants.KEY)
    @Param(description = "the ovf property key")
    private String key;

    @SerializedName(ApiConstants.TYPE)
    @Param(description = "the ovf property type")
    private String type;

    @SerializedName(ApiConstants.VALUE)
    @Param(description = "the ovf property value")
    private String value;

    @SerializedName(ApiConstants.PASSWORD)
    @Param(description = "is the ovf property a password")
    private Boolean password;

    @SerializedName(ApiConstants.QUALIFIERS)
    @Param(description = "the ovf property qualifiers")
    private String qualifiers;

    @SerializedName(ApiConstants.USER_CONFIGURABLE)
    @Param(description = "is the ovf property user configurable")
    private Boolean userConfigurable;

    @SerializedName(ApiConstants.LABEL)
    @Param(description = "the ovf property label")
    private String label;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "the ovf property label")
    private String description;

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

    public Boolean getUserConfigurable() {
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

    public Boolean getPassword() {
        return password;
    }

    public void setPassword(Boolean password) {
        this.password = password;
    }
}
