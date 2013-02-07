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

import java.util.Date;
import java.util.List;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.network.vpc.VpcOffering;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value=VpcOffering.class)
@SuppressWarnings("unused")
public class VpcOfferingResponse extends BaseResponse {
    @SerializedName("id") @Param(description="the id of the vpc offering")
    private String id;

    @SerializedName(ApiConstants.NAME) @Param(description="the name of the vpc offering")
    private String name;

    @SerializedName(ApiConstants.DISPLAY_TEXT) @Param(description="an alternate display text of the vpc offering.")
    private String displayText;

    @SerializedName(ApiConstants.CREATED) @Param(description="the date this vpc offering was created")
    private Date created;

    @SerializedName(ApiConstants.IS_DEFAULT) @Param(description="true if vpc offering is default, false otherwise")
    private Boolean isDefault;

    @SerializedName(ApiConstants.STATE) @Param(description="state of the vpc offering. Can be Disabled/Enabled")
    private String state;

    @SerializedName(ApiConstants.SERVICE) @Param(description="the list of supported services", responseObject = ServiceResponse.class)
    private List<ServiceResponse> services;


    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public void setServices(List<ServiceResponse> services) {
        this.services = services;
    }

    public void setState(String state) {
        this.state = state;
    }
}
