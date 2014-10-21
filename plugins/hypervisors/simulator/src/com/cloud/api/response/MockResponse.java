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
package com.cloud.api.response;

import com.cloud.serializer.Param;
import com.cloud.simulator.MockConfigurationVO;
import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;


@EntityReference(value = MockConfigurationVO.class)
public class MockResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID) @Param(description="the mock ID")
    private Long id;

    @SerializedName(ApiConstants.ZONE_ID) @Param(description="the Zone ID scope of the mock")
    private Long zoneId;

    @SerializedName(ApiConstants.POD_ID) @Param(description="the Pod ID scope of the mock")
    private Long podId;

    @SerializedName(ApiConstants.CLUSTER_ID) @Param(description="the Cluster ID scope of the mock")
    private Long clusterId;

    @SerializedName(ApiConstants.HOST_ID) @Param(description="the Host ID scope of the mock")
    private Long hostId;

    @SerializedName(ApiConstants.NAME) @Param(description="the agent command to be mocked")
    private String name;

    @SerializedName(ApiConstants.COUNT) @Param(description="number of times mock is executed, if not specified then mock remains active till cleaned up")
    private Integer count;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }

    public Long getPodId() {
        return podId;
    }

    public void setPodId(Long podId) {
        this.podId = podId;
    }

    public Long getClusterId() {
        return clusterId;
    }

    public void setClusterId(Long clusterId) {
        this.clusterId = clusterId;
    }

    public Long getHostId() {
        return hostId;
    }

    public void setHostId(Long hostId) {
        this.hostId = hostId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

}
