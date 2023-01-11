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
import org.apache.cloudstack.api.EntityReference;

import com.cloud.network.as.Counter;
import com.cloud.serializer.Param;

@SuppressWarnings("unused")
@EntityReference(value = Counter.class)
public class CounterResponse extends BaseResponse {
    @SerializedName("id")
    @Param(description = "the id of the Counter")
    private String id;

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
    private String zoneId;

    @SerializedName(value = ApiConstants.PROVIDER)
    @Param(description = "Provider of the counter.")
    private String provider;

    @Override
    public String getObjectId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
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

    public void setProvider(String provider) {
        this.provider = provider;
    }
}
