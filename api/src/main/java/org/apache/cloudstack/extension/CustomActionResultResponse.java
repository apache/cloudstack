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

package org.apache.cloudstack.extension;

import java.util.Map;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class CustomActionResultResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "ID of the action")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "Name of the action")
    private String name;

    @SerializedName(ApiConstants.SUCCESS)
    @Param(description = "Whether custom action succeed or not")
    private Boolean success;

    @SerializedName(ApiConstants.RESULT)
    @Param(description = "Result of the action execution")
    private Map<String, String> result;

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setResult(Map<String, String> result) {
        this.result = result;
    }
}
