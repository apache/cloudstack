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

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import java.util.HashSet;
import java.util.Set;

public class ApiResponseResponse extends BaseResponse {
    @SerializedName(ApiConstants.NAME) @Param(description="the name of the api response field")
    private String name;

    @SerializedName(ApiConstants.DESCRIPTION) @Param(description="description of the api response field")
    private String description;

    @SerializedName(ApiConstants.TYPE) @Param(description="response field type")
    private String type;

    @SerializedName(ApiConstants.RESPONSE)  @Param(description="api response fields")
    private Set<ApiResponseResponse> apiResponse;

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void addApiResponse(ApiResponseResponse childApiResponse) {
        if(this.apiResponse == null) {
            this.apiResponse = new HashSet<ApiResponseResponse>();
        }
        this.apiResponse.add(childApiResponse);
    }
}
