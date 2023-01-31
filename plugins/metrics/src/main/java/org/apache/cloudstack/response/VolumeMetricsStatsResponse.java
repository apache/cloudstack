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

package org.apache.cloudstack.response;

import java.util.List;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.response.StatsResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class VolumeMetricsStatsResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "the ID of the volume")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "the name of the volume")
    private String name;

    @SerializedName("stats")
    @Param(description = "the list of VM stats")
    private List<StatsResponse> stats;

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStats(List<StatsResponse> stats) {
        this.stats = stats;
    }

}
