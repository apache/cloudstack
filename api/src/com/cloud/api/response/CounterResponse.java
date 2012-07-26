//       Licensed to the Apache Software Foundation (ASF) under one
//       or more contributor license agreements.  See the NOTICE file
//       distributed with this work for additional information
//       regarding copyright ownership.  The ASF licenses this file
//       to you under the Apache License, Version 2.0 (the
//       "License"); you may not use this file except in compliance
//       with the License.  You may obtain a copy of the License at
//
//         http://www.apache.org/licenses/LICENSE-2.0
//
//       Unless required by applicable law or agreed to in writing,
//       software distributed under the License is distributed on an
//       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//       KIND, either express or implied.  See the License for the
//       specific language governing permissions and limitations
//       under the License.

package com.cloud.api.response;

import com.cloud.api.ApiConstants;
import com.cloud.serializer.Param;
import com.cloud.utils.IdentityProxy;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class CounterResponse extends BaseResponse {
    @SerializedName("id")
    @Param(description = "the id of the Counter")
    private final IdentityProxy id = new IdentityProxy("counter");

    @SerializedName(value = ApiConstants.NAME)
    @Param(description = "Name of the counter.")
    private String name;

    @SerializedName(value = ApiConstants.SOURCE)
    @Param(description = "Source of the counter.")
    private String source;

    @SerializedName(value = ApiConstants.VALUE)
    @Param(description = "Value in case of snmp or other specific counters.")
    private String value;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "zone id of counter")
    private final IdentityProxy zoneId = new IdentityProxy("data_center");

    public void setId(Long id) {
        this.id.setValue(id);
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setValue(String value) {
        this.value = value;
    }
}