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

import java.util.List;
import java.util.Map;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class DetailOptionsResponse extends BaseResponse {
    @SerializedName(ApiConstants.DETAILS)
    @Param(description = "Map of all possible details and their possible list of values")
    private Map<String, List<String>> details;

    public DetailOptionsResponse(Map<String, List<String>> details) {
        this.details = details;
    }

    public void setDetails(Map<String, List<String>> details) {
        this.details = details;
    }

    public Map<String, List<String>> getDetails() {
        return details;
    }
}
