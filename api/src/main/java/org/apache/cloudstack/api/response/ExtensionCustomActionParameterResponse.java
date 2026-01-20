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

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class ExtensionCustomActionParameterResponse extends BaseResponse {

    @SerializedName(ApiConstants.NAME)
    @Param(description = "Name of the parameter")
    private String name;

    @SerializedName(ApiConstants.TYPE)
    @Param(description = "Type of the parameter")
    private String type;

    @SerializedName(ApiConstants.VALIDATION_FORMAT)
    @Param(description = "Validation format for value of the parameter. Available for specific types")
    private String validationFormat;

    @SerializedName(ApiConstants.VALUE_OPTIONS)
    @Param(description = "Comma-separated list of options for value of the parameter")
    private List<Object> valueOptions;

    @SerializedName(ApiConstants.REQUIRED)
    @Param(description = "Whether the parameter is required or not")
    private Boolean required;

    public ExtensionCustomActionParameterResponse(String name, String type, String validationFormat, List<Object> valueOptions,
                boolean required) {
        this.name = name;
        this.type = type;
        this.validationFormat = validationFormat;
        this.valueOptions = valueOptions;
        this.required = required;
    }
}
