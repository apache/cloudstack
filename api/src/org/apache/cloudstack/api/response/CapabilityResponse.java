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

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;

public class CapabilityResponse extends BaseResponse {

    @SerializedName(ApiConstants.NAME)
    @Param(description = "the capability name")
    private String name;

    @SerializedName(ApiConstants.VALUE)
    @Param(description = "the capability value")
    private String value;

    @SerializedName(ApiConstants.CAN_CHOOSE_SERVICE_CAPABILITY)
    @Param(description = "can this service capability value can be choosable while creatine network offerings")
    private boolean canChoose;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean getCanChoose() {
        return canChoose;
    }

    public void setCanChoose(boolean choosable) {
        this.canChoose = choosable;
    }
}
