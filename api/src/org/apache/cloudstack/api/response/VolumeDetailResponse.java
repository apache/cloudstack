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

@SuppressWarnings("unused")
public class VolumeDetailResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "ID of the volume")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "name of the volume detail")
    private String name;

    @SerializedName(ApiConstants.VALUE)
    @Param(description = "value of the volume detail")
    private String value;

    @SerializedName(ApiConstants.DISPLAY_VOLUME)
    @Param(description = "an optional field whether to the display the volume to the end user or not.")
    private Boolean displayVm;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getDisplayVm() {
        return displayVm;
    }

    public void setDisplayVm(Boolean displayVm) {
        this.displayVm = displayVm;
    }
}
