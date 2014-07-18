//
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
//

package org.apache.cloudstack.storage.datastore.util;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class ListElastistorPoolResponse extends BaseResponse {
    @SerializedName("id")
    @Param(description = "the ID of the storage pool")
    private String id;

    @SerializedName("name")
    @Param(description = "the name of the storage pool")
    private String name;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "the state of the storage pool")
    private String state;

    @SerializedName(ApiConstants.SIZE)
    @Param(description = "the current available space of the pool")
    private Long currentAvailableSpace;

    @SerializedName(ApiConstants.MAX_IOPS)
    @Param(description = "available iops of the pool")
    private Long availIOPS;

    @SerializedName("controllerid")
    @Param(description = "controller of the pool")
    private String controllerid;

    @SerializedName("gateway")
    @Param(description = "default gateway of the pool")
    private String gateway;

    @Override
    public String getObjectId() {
        return this.getId();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Long getCurrentAvailableSpace() {
        return currentAvailableSpace;
    }

    public void setCurrentAvailableSpace(Long currentAvailableSpace) {
        this.currentAvailableSpace = currentAvailableSpace;
    }

    public Long getAvailIOPS() {
        return availIOPS;
    }

    public void setAvailIOPS(Long availIOPS) {
        this.availIOPS = availIOPS;
    }

    public String getControllerid() {
        return controllerid;
    }

    public void setControllerid(String controllerid) {
        this.controllerid = controllerid;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }
}
