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

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class NetworkProtocolResponse extends BaseResponse {
    @SerializedName(ApiConstants.INDEX)
    @Param(description = "the index (ID, Value, Code, Type, Option, etc) of the protocol parameter")
    private Integer index;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "the name of the protocol parameter")
    private String name;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "the description of the protocol parameter")
    private String description;

    @SerializedName(ApiConstants.DETAILS)
    @Param(description = "the details of the protocol parameter")
    private Map details;

    public NetworkProtocolResponse(Integer index, String name, String description) {
        this.index = index;
        this.name = name;
        this.description = description;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map getDetails() {
        return details;
    }

    public void setDetails(Map details) {
        this.details = details;
    }

    public void addDetail(String key, String value) {
        if (this.details == null) {
            this.details = new LinkedHashMap();
        }
        this.details.put(key, value);
    }
}
