/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cloudstack.storage.feign.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class VolumeQosPolicy {
    @JsonProperty("max_throughput_iops")
    @SerializedName("max_throughput_iops")
    private Integer maxThroughputIops = null;

    @JsonProperty("max_throughput_mbps")
    @SerializedName("max_throughput_mbps")
    private Integer maxThroughputMbps = null;

    @JsonProperty("min_throughput_iops")
    @SerializedName("min_throughput_iops")
    private Integer minThroughputIops = null;

    @JsonProperty("name")
    @SerializedName("name")
    private String name = null;

    @JsonProperty("uuid")
    @SerializedName("uuid")
    private String uuid = null;

    public Integer getMaxThroughputIops() {
        return maxThroughputIops;
    }

    public void setMaxThroughputIops(Integer maxThroughputIops) {
        this.maxThroughputIops = maxThroughputIops;
    }

    public Integer getMaxThroughputMbps() {
        return maxThroughputMbps;
    }

    public void setMaxThroughputMbps(Integer maxThroughputMbps) {
        this.maxThroughputMbps = maxThroughputMbps;
    }

    public Integer getMinThroughputIops() {
        return minThroughputIops;
    }

    public void setMinThroughputIops(Integer minThroughputIops) {
        this.minThroughputIops = minThroughputIops;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
